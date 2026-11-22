package com.turel.fx

import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

class MainActivity : ComponentActivity() {
    private var player: ExoPlayer? = null
    private lateinit var playerView: PlayerView
    private lateinit var status: TextView

    private val pickVideo = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            try {
                contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: Exception) { }
            player?.setMediaItem(MediaItem.fromUri(uri))
            player?.prepare()
            player?.play()
            status.text = "Video loaded • Ready to edit"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(12, 12, 16)
        window.navigationBarColor = android.graphics.Color.rgb(12, 12, 16)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 28, 20, 20)
            setBackgroundColor(android.graphics.Color.rgb(12, 12, 16))
        }
        val title = TextView(this).apply {
            text = "TUREL FX"
            textSize = 27f
            setTextColor(android.graphics.Color.WHITE)
            gravity = Gravity.CENTER
            letterSpacing = 0.12f
        }
        val subtitle = TextView(this).apply {
            text = "CREATE  •  EDIT  •  INSPIRE"
            textSize = 11f
            setTextColor(android.graphics.Color.rgb(160, 160, 175))
            gravity = Gravity.CENTER
        }
        playerView = PlayerView(this).apply {
            useController = true
            setBackgroundColor(android.graphics.Color.BLACK)
        }
        status = TextView(this).apply {
            text = "Import a video to start your project"
            textSize = 14f
            setTextColor(android.graphics.Color.LTGRAY)
            gravity = Gravity.CENTER
            setPadding(8, 18, 8, 18)
        }
        val importButton = Button(this).apply {
            text = "＋  IMPORT VIDEO"
            setOnClickListener { pickVideo.launch(arrayOf("video/*")) }
        }
        val coming = TextView(this).apply {
            text = "EDITOR TOOLS\nTrim  •  Music  •  Text  •  Filters  •  Export"
            textSize = 14f
            setTextColor(android.graphics.Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(8, 28, 8, 8)
        }
        root.addView(title, LinearLayout.LayoutParams(-1, ViewGroup.LayoutParams.WRAP_CONTENT))
        root.addView(subtitle, LinearLayout.LayoutParams(-1, ViewGroup.LayoutParams.WRAP_CONTENT))
        root.addView(playerView, LinearLayout.LayoutParams(-1, 0, 1f).apply { topMargin = 24; bottomMargin = 8 })
        root.addView(status)
        root.addView(importButton, LinearLayout.LayoutParams(-1, ViewGroup.LayoutParams.WRAP_CONTENT))
        root.addView(coming)
        setContentView(root)
        player = ExoPlayer.Builder(this).build()
        playerView.player = player
    }

    override fun onStop() {
        super.onStop()
        player?.pause()
    }

    override fun onDestroy() {
        playerView.player = null
        player?.release()
        player = null
        super.onDestroy()
    }
}
