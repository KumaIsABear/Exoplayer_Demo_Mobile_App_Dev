package com.example.exoplayerdemo

import android.app.PictureInPictureParams
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Rational
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.ui.PlayerView

class PlayerActivity : AppCompatActivity() {

    private var player: ExoPlayer? = null
    private lateinit var playerView: PlayerView
    private lateinit var btnBack: ImageButton
    private lateinit var textViewStudentInfoPlayer: TextView

    private var videoUrl: String? = null
    private var playWhenReady = true
    private var mediaItemIndex = 0
    private var playbackPosition = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)

        playerView = findViewById(R.id.playerView)
        btnBack = findViewById(R.id.btn_back)
        textViewStudentInfoPlayer = findViewById(R.id.textViewStudentInfoPlayer)

        btnBack.setOnClickListener {
            finish()
        }

        videoUrl = intent.data?.toString()
            ?: intent.getStringExtra(EXTRA_VIDEO_URL)
                    ?: intent.getStringExtra("VIDEO_URL")
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val newUrl = intent.data?.toString()
            ?: intent.getStringExtra(EXTRA_VIDEO_URL)
            ?: intent.getStringExtra("VIDEO_URL")
        if (newUrl != null) {
            videoUrl = newUrl
            playbackPosition = 0L
            mediaItemIndex = 0
            playWhenReady = true
            initializePlayer()
        }
    }

    override fun onStart() {
        super.onStart()
        initializePlayer()
    }

    override fun onResume() {
        super.onResume()
        if (player == null) {
            initializePlayer()
        }
    }

    override fun onPause() {
        super.onPause()
        // Giữ nguyên luồng phát khi vào chế độ PiP
    }

    override fun onStop() {
        super.onStop()
        if (!isInPictureInPictureMode) {
            releasePlayer()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        releasePlayer()
    }

    @OptIn(UnstableApi::class)
    private fun initializePlayer() {
        if (player == null) {
            // Loại bỏ bộ giải mã phần cứng ảo bị lỗi (goldfish/ranchu) trên máy ảo
            // và ưu tiên bộ giải mã phần mềm chuẩn của Google (c2.android.* / OMX.google.*)
            val softwarePreferredSelector = MediaCodecSelector { mimeType, requiresSecure, requiresTunneling ->
                val defaultList = MediaCodecSelector.DEFAULT.getDecoderInfos(
                    mimeType,
                    requiresSecure,
                    requiresTunneling
                )
                val filtered = defaultList.filter { info ->
                    !info.name.contains("goldfish", ignoreCase = true) &&
                            !info.name.contains("ranchu", ignoreCase = true)
                }.sortedByDescending { info ->
                    info.name.startsWith("c2.android.", ignoreCase = true) ||
                            info.name.startsWith("OMX.google.", ignoreCase = true)
                }
                if (filtered.isNotEmpty()) filtered.toMutableList() else defaultList
            }

            val renderersFactory = DefaultRenderersFactory(this)
                .setEnableDecoderFallback(true)
                .setMediaCodecSelector(softwarePreferredSelector)

            player = ExoPlayer.Builder(this, renderersFactory).build().also { exoPlayer ->
                playerView.player = exoPlayer
                exoPlayer.addListener(object : Player.Listener {
                    override fun onPlayerError(error: PlaybackException) {
                        Toast.makeText(
                            this@PlayerActivity,
                            "Lỗi giải mã: ${error.errorCodeName}. Hãy chọn file video_demo_pip.mp4!",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                })
            }
        }

        videoUrl?.let {
            val mediaItem = MediaItem.fromUri(it)
            player?.setMediaItem(mediaItem)
            player?.seekTo(mediaItemIndex, playbackPosition)
            player?.playWhenReady = true
            player?.prepare()
            player?.play()
        }
    }

    private fun releasePlayer() {
        player?.let {
            playbackPosition = it.currentPosition
            mediaItemIndex = it.currentMediaItemIndex
            playWhenReady = it.playWhenReady
            it.release()
        }
        player = null
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val aspectRatio = Rational(16, 9)
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(aspectRatio)
                .build()
            enterPictureInPictureMode(params)
        } else {
            @Suppress("DEPRECATION")
            enterPictureInPictureMode()
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        if (isInPictureInPictureMode) {
            playerView.useController = false
            btnBack.visibility = View.GONE
            textViewStudentInfoPlayer.visibility = View.GONE
        } else {
            playerView.useController = true
            btnBack.visibility = View.VISIBLE
            textViewStudentInfoPlayer.visibility = View.VISIBLE
        }
    }

    companion object {
        const val EXTRA_VIDEO_URL = "extra_video_url"
    }
}