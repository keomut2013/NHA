package com.iptv.player.updater

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

data class VersionInfo(
    val versionCode: Int,
    val versionName: String,
    val apkUrl: String,
    val sha256: String,
    val changeLog: String
)

class OtaUpdater(private val context: Context) {

    private val client = OkHttpClient()
    private val gson = Gson()

    suspend fun checkUpdate(versionJsonUrl: String): VersionInfo? = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder().url(versionJsonUrl).build()
            val res = client.newCall(req).execute()
            if (res.isSuccessful) {
                val json = res.body?.string() ?: return@withContext null
                val versionInfo = gson.fromJson(json, VersionInfo::class.java)

                val currentVersion = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    context.packageManager.getPackageInfo(
                        context.packageName,
                        android.content.pm.PackageManager.PackageInfoFlags.of(0)
                    ).longVersionCode.toInt()
                } else {
                    @Suppress("DEPRECATION")
                    context.packageManager.getPackageInfo(context.packageName, 0).versionCode
                }

                if (versionInfo.versionCode > currentVersion) {
                    return@withContext versionInfo
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext null
    }

    suspend fun downloadAndInstallApk(versionInfo: VersionInfo, onProgress: (Int) -> Unit = {}) =
        withContext(Dispatchers.IO) {
            val updateDir = File(context.getExternalFilesDir(null), "updates").apply { mkdirs() }
            val apkFile = File(updateDir, "update_${versionInfo.versionCode}.apk")

            try {
                // 1. Download
                val req = Request.Builder().url(versionInfo.apkUrl).build()
                val res = client.newCall(req).execute()
                val body = res.body ?: throw Exception("Empty body")
                val totalBytes = body.contentLength()

                body.byteStream().use { input ->
                    FileOutputStream(apkFile).use { output ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        var downloaded: Long = 0
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            downloaded += bytesRead
                            if (totalBytes > 0) {
                                val progress = ((downloaded * 100) / totalBytes).toInt()
                                onProgress(progress)
                            }
                        }
                    }
                }

                // 2. Verify SHA-256 checksum (Codex Audit compliance)
                if (versionInfo.sha256.isNotBlank()) {
                    val computedHash = calculateSha256(apkFile)
                    if (!computedHash.equals(versionInfo.sha256, ignoreCase = true)) {
                        apkFile.delete()
                        throw SecurityException("SHA-256 hash mismatch! File may be corrupted or tampered.")
                    }
                }

                // 3. Trigger Installation Intent via FileProvider
                installApk(apkFile)

            } catch (e: Exception) {
                e.printStackTrace()
                apkFile.delete()
            }
        }

    private fun installApk(file: File) {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.Action.VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    private fun calculateSha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}

