package com.iptv.player

import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.iptv.player.data.Channel
import com.iptv.player.network.ChannelRepository
import com.iptv.player.updater.OtaUpdater
import com.iptv.player.updater.VersionInfo
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var player: ExoPlayer
    private lateinit var playerView: PlayerView
    private lateinit var layoutChannelDrawer: LinearLayout
    private lateinit var rvChannels: RecyclerView
    private lateinit var cardUpdate: CardView
    private lateinit var tvUpdateText: TextView
    private lateinit var btnDoUpdate: Button

    private lateinit var channelRepo: ChannelRepository
    private lateinit var updater: OtaUpdater

    // URLs can be hosted on your GitHub repository
    private val remoteM3uUrl = "https://raw.githubusercontent.com/your-username/iptv-config/main/playlist.m3u"
    private val versionJsonUrl = "https://raw.githubusercontent.com/your-username/iptv-config/main/version.json"

    private var channelList: List<Channel> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        playerView = findViewById(R.id.playerView)
        layoutChannelDrawer = findViewById(R.id.layoutChannelDrawer)
        rvChannels = findViewById(R.id.rvChannels)
        cardUpdate = findViewById(R.id.cardUpdate)
        tvUpdateText = findViewById(R.id.tvUpdateText)
        btnDoUpdate = findViewById(R.id.btnDoUpdate)

        channelRepo = ChannelRepository(this)
        updater = OtaUpdater(this)

        initPlayer()
        loadChannels()
        checkForUpdates()
    }

    private fun initPlayer() {
        player = ExoPlayer.Builder(this).build()
        playerView.player = player
        player.playWhenReady = true
    }

    private fun loadChannels() {
        lifecycleScope.launch {
            channelList = channelRepo.getChannels(remoteM3uUrl)
            if (channelList.isNotEmpty()) {
                playChannel(channelList[0])
            }
        }
    }

    private fun playChannel(channel: Channel) {
        val mediaItem = MediaItem.fromUri(channel.streamUrl)
        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()
    }

    private fun checkForUpdates() {
        lifecycleScope.launch {
            val updateInfo = updater.checkUpdate(versionJsonUrl)
            if (updateInfo != null) {
                showUpdateNotification(updateInfo)
            }
        }
    }

    private fun showUpdateNotification(info: VersionInfo) {
        tvUpdateText.text = "Có bản cập nhật v${info.versionName}!"
        cardUpdate.visibility = View.VISIBLE
        btnDoUpdate.setOnClickListener {
            btnDoUpdate.isEnabled = false
            btnDoUpdate.text = "Đang tải..."
            lifecycleScope.launch {
                updater.downloadAndInstallApk(info) { progress ->
                    btnDoUpdate.text = "Tải $progress%"
                }
            }
        }
    }

    // Remote control navigation for Android TV
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                // Toggle channel list
                layoutChannelDrawer.visibility = if (layoutChannelDrawer.visibility == View.VISIBLE) {
                    View.GONE
                } else {
                    View.VISIBLE
                }
                return true
            }
            KeyEvent.KEYCODE_BACK -> {
                if (layoutChannelDrawer.visibility == View.VISIBLE) {
                    layoutChannelDrawer.visibility = View.GONE
                    return true
                }
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onDestroy() {
        super.onDestroy()
        player.release()
    }
}

