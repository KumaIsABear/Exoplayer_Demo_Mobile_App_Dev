package com.example.exoplayerdemo

import android.app.PictureInPictureParams
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

class PlayerActivity : AppCompatActivity() {

    private var player: ExoPlayer? = null
    private lateinit var playerView: PlayerView

    private var videoUrl: String? = null
    private var playWhenReady = true
    private var mediaItemIndex = 0
    private var playbackPosition = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)

        playerView = findViewById(R.id.playerView)
        videoUrl = intent.getStringExtra(EXTRA_VIDEO_URL)
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
        // No-op for SDK >= 24, handled in onStop
    }

    override fun onStop() {
        super.onStop()
        if (!isInPictureInPictureMode) {
            releasePlayer()
        }
    }

    private fun initializePlayer() {
        if (player == null) {
            player = ExoPlayer.Builder(this).build()
            playerView.player = player
        }

        videoUrl?.let {
            val mediaItem = MediaItem.fromUri(it)
            player?.setMediaItem(mediaItem)
            player?.playWhenReady = playWhenReady
            player?.seekTo(mediaItemIndex, playbackPosition)
            player?.prepare()
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

    // Called when the user presses the home button or otherwise leaves the activity
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val aspectRatio = Rational(16, 9)
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(aspectRatio)
                .build()
            enterPictureInPictureMode(params)
        }
    }

    // Manage UI during PiP transitions
    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        if (isInPictureInPictureMode) {
            // Hide UI controls when entering PiP mode
            playerView.useController = false
        } else {
            // Show UI controls when exiting PiP mode
            playerView.useController = true
        }
    }

    companion object {
        const val EXTRA_VIDEO_URL = "extra_video_url"
    }
}
