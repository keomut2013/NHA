package com.iptv.player.network

import android.content.Context
import com.iptv.player.data.Channel
import com.iptv.player.data.M3uParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

class ChannelRepository(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val cacheFile = File(context.filesDir, "cached_playlist.m3u")

    suspend fun getChannels(remoteUrl: String, forceRefresh: Boolean = false): List<Channel> =
        withContext(Dispatchers.IO) {
            // 1. Try remote fetch if network is available
            if (forceRefresh || !cacheFile.exists() || isCacheExpired()) {
                try {
                    val request = Request.Builder().url(remoteUrl).build()
                    val response = client.newCall(request).execute()
                    if (response.isSuccessful) {
                        val body = response.body?.string() ?: ""
                        if (body.contains("#EXTM3U")) {
                            // Save to local cache
                            cacheFile.writeText(body)
                            return@withContext M3uParser.parse(body)
                        }
                    }
                } catch (e: Exception) {
                    // Fallback to cache if network fails
                    e.printStackTrace()
                }
            }

            // 2. Load from local cache
            if (cacheFile.exists()) {
                val cachedContent = cacheFile.readText()
                return@withContext M3uParser.parse(cachedContent)
            }

            // 3. Fallback default channels
            return@withContext listOf(
                Channel(
                    name = "VTV3 HD (Mặc Định)",
                    streamUrl = "http://42.1.99.192:8080/secureVhost/vtv3.flv?token=vtv_secret_20260706",
                    group = "VTV"
                )
            )
        }

    private fun isCacheExpired(): Boolean {
        // Cache expires after 2 hours (TTL)
        val twoHoursMillis = 2 * 60 * 60 * 1000
        return System.currentTimeMillis() - cacheFile.lastModified() > twoHoursMillis
    }
}

