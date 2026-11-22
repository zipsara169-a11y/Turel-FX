package com.turel.fx

import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.TransformationException
import androidx.media3.transformer.Transformer
import java.io.File
import java.util.Locale

class MainActivity : ComponentActivity() {
    private val bg = Color.rgb(10, 12, 20)
    private val panel = Color.rgb(22, 26, 39)
    private val panel2 = Color.rgb(30, 35, 51)
    private val purple = Color.rgb(145, 105, 255)
    private val cyan = Color.rgb(68, 215, 225)
    private val white = Color.rgb(245, 246, 252)
    private val muted = Color.rgb(145, 153, 174)

    private var player: ExoPlayer? = null
    private var playerView: PlayerView? = null
    private var selectedUri: Uri? = null
    private var durationMs = 0L
    private var trimStartMs = 0L
    private var trimEndMs = 0L
    private lateinit var videoStatus: TextView
    private lateinit var timeLabel: TextView
    private lateinit var startLabel: TextView
    private lateinit var endLabel: TextView
    private lateinit var startSeek: SeekBar
    private lateinit var endSeek: SeekBar
    private lateinit var playButton: Button
    private lateinit var exportButton: Button
    private var exporting = false

    private val pickVideo = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            selectedUri = uri
            try {
                contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: Exception) { }
            player?.setMediaItem(MediaItem.fromUri(uri))
            player?.prepare()
            player?.playWhenReady = true
            videoStatus.text = "VIDEO LOADED  •  READY TO EDIT"
            playButton.text = "Ⅱ  PAUSE"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        window.decorView.systemUiVisibility = 0

        player = ExoPlayer.Builder(this).build()
        player?.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) {
                    durationMs = (player?.duration ?: 0L).coerceAtLeast(0L)
                    if (durationMs > 0) {
                        trimStartMs = 0L
                        trimEndMs = durationMs
                        startSeek.max = durationMs.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
                        endSeek.max = startSeek.max
                        endSeek.progress = endSeek.max
                        updateTrimLabels()
                        timeLabel.text = "00:00  /  ${formatTime(durationMs)}"
                    }
                }
            }
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (::playButton.isInitialized) playButton.text = if (isPlaying) "Ⅱ  PAUSE" else "▶  PLAY"
            }
        })

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bg)
        }
        val scroll = ScrollView(this).apply { isFillViewport = true; clipToPadding = false }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(18), dp(20), dp(30))
        }
        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        val top = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val brand = TextView(this).apply {
            text = "TUREL"
            textSize = 22f
            letterSpacing = .12f
            setTextColor(white)
            typeface = android.graphics.Typeface.create("sans-serif-black", 1)
        }
        val fx = TextView(this).apply {
            text = " FX"
            textSize = 22f
            letterSpacing = .08f
            setTextColor(purple)
            typeface = android.graphics.Typeface.create("sans-serif-black", 1)
        }
        val brandGroup = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; addView(brand); addView(fx) }
        top.addView(brandGroup, LinearLayout.LayoutParams(0, -2, 1f))
        top.addView(pill("PRO EDITOR", cyan), LinearLayout.LayoutParams(-2, dp(32)))
        content.addView(top)
        content.addView(space(22))

        content.addView(label("YOUR CREATIVE STUDIO", purple, 10f, true))
        content.addView(space(6))
        content.addView(title("Make every frame\nfeel cinematic."))
        content.addView(space(7))
        content.addView(label("A powerful mobile editing workspace.", muted, 13f))
        content.addView(space(20))

        val previewCard = card()
        previewCard.orientation = LinearLayout.VERTICAL
        previewCard.setPadding(dp(10), dp(10), dp(10), dp(12))
        val previewHeader = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        previewHeader.addView(label("PREVIEW MONITOR", white, 11f, true), LinearLayout.LayoutParams(0, -2, 1f))
        previewHeader.addView(label("16:9  •  ORIGINAL", muted, 9f, true))
        previewCard.addView(previewHeader)
        val frame = FrameLayout(this).apply {
            setBackgroundColor(Color.BLACK)
        }
        val pv = PlayerView(this).apply {
            useController = false
            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
            player = this@MainActivity.player
            setShutterBackgroundColor(Color.BLACK)
        }
        playerView = pv
        frame.addView(pv, FrameLayout.LayoutParams(-1, -1))
        val emptyHint = TextView(this).apply {
            text = "＋\n\nImport a video to begin"
            gravity = Gravity.CENTER
            textSize = 14f
            setTextColor(Color.rgb(180, 185, 205))
            setOnClickListener { pickVideo.launch(arrayOf("video/*")) }
        }
        frame.addView(emptyHint, FrameLayout.LayoutParams(-1, -1))
        videoStatus = label("NO PROJECT OPEN  •  IMPORT A CLIP TO START", muted, 9f, true)
        previewCard.addView(space(10))
        previewCard.addView(frame, LinearLayout.LayoutParams(-1, dp(205)))
        previewCard.addView(space(10))
        previewCard.addView(videoStatus)
        content.addView(previewCard)
        content.addView(space(14))

        val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val import = actionButton("＋  IMPORT", purple)
        import.setOnClickListener { pickVideo.launch(arrayOf("video/*")) }
        actions.addView(import, LinearLayout.LayoutParams(0, dp(48), 1f))
        actions.addView(spaceHorizontal(10))
        playButton = actionButton("▶  PLAY", cyan)
        playButton.setOnClickListener {
            val p = player
            if (p == null || selectedUri == null) {
                Toast.makeText(this, "Import a video first", Toast.LENGTH_SHORT).show()
            } else if (p.isPlaying) p.pause() else p.play()
        }
        actions.addView(playButton, LinearLayout.LayoutParams(0, dp(48), 1f))
        content.addView(actions)
        content.addView(space(22))

        content.addView(sectionHeading("EDIT TIMELINE", "01  /  CLIP"))
        content.addView(space(10))
        val timeline = card()
        timeline.orientation = LinearLayout.VERTICAL
        timeline.setPadding(dp(14), dp(14), dp(14), dp(14))
        val timeRow = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        timeLabel = label("00:00  /  00:00", white, 11f, true)
        timeRow.addView(timeLabel, LinearLayout.LayoutParams(0, -2, 1f))
        timeRow.addView(label("TRIM RANGE", purple, 9f, true))
        timeline.addView(timeRow)
        timeline.addView(space(14))
        timeline.addView(label("START POINT", muted, 9f, true))
        startSeek = SeekBar(this).apply {
            progressTintList = android.content.res.ColorStateList.valueOf(purple)
            thumbTintList = android.content.res.ColorStateList.valueOf(purple)
            max = 1000
            setOnSeekBarChangeListener(simpleSeek { value ->
                trimStartMs = value.toLong()
                if (trimStartMs >= trimEndMs) trimStartMs = (trimEndMs - 500).coerceAtLeast(0)
                updateTrimLabels()
            })
        }
        timeline.addView(startSeek)
        startLabel = label("00:00", white, 11f, true)
        timeline.addView(startLabel)
        timeline.addView(space(10))
        timeline.addView(label("END POINT", muted, 9f, true))
        endSeek = SeekBar(this).apply {
            progressTintList = android.content.res.ColorStateList.valueOf(cyan)
            thumbTintList = android.content.res.ColorStateList.valueOf(cyan)
            max = 1000
            progress = 1000
            setOnSeekBarChangeListener(simpleSeek { value ->
                trimEndMs = value.toLong()
                if (trimEndMs <= trimStartMs) trimEndMs = (trimStartMs + 500).coerceAtMost(durationMs)
                updateTrimLabels()
            })
        }
        timeline.addView(endSeek)
        endLabel = label("00:00", white, 11f, true)
        timeline.addView(endLabel)
        timeline.addView(space(12))
        val markers = LinearLayout(this).apply { gravity = Gravity.CENTER }
        val markStart = smallButton("SET START")
        markStart.setOnClickListener {
            val pos = player?.currentPosition ?: 0L
            trimStartMs = pos.coerceAtMost((trimEndMs - 500).coerceAtLeast(0))
            syncSeekBars()
            updateTrimLabels()
        }
        val markEnd = smallButton("SET END")
        markEnd.setOnClickListener {
            val pos = player?.currentPosition ?: durationMs
            trimEndMs = pos.coerceAtLeast(trimStartMs + 500).coerceAtMost(durationMs)
            syncSeekBars()
            updateTrimLabels()
        }
        markers.addView(markStart, LinearLayout.LayoutParams(0, dp(38), 1f))
        markers.addView(spaceHorizontal(8))
        markers.addView(markEnd, LinearLayout.LayoutParams(0, dp(38), 1f))
        timeline.addView(markers)
        content.addView(timeline)
        content.addView(space(20))

        content.addView(sectionHeading("QUICK CONTROLS", "02  /  PLAYBACK"))
        content.addView(space(10))
        val speeds = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        listOf("0.5×", "1.0×", "1.5×", "2.0×").forEach { speed ->
            val b = smallButton(speed)
            b.setOnClickListener {
                val rate = speed.removeSuffix("×").toFloat()
                player?.setPlaybackParameters(PlaybackParameters(rate))
                Toast.makeText(this, "Playback speed: $speed", Toast.LENGTH_SHORT).show()
            }
            speeds.addView(b, LinearLayout.LayoutParams(0, dp(42), 1f))
            if (speed != "2.0×") speeds.addView(spaceHorizontal(7))
        }
        content.addView(speeds)
        content.addView(space(20))

        content.addView(sectionHeading("EXPORT PROJECT", "03  /  OUTPUT"))
        content.addView(space(10))
        val exportCard = card()
        exportCard.orientation = LinearLayout.VERTICAL
        exportCard.setPadding(dp(14), dp(14), dp(14), dp(14))
        exportCard.addView(label("Export your trimmed clip as an MP4 video.", white, 13f))
        exportCard.addView(space(5))
        exportCard.addView(label("The source resolution and frame rate are preserved.", muted, 11f))
        exportCard.addView(space(12))
        exportButton = actionButton("⇧  EXPORT VIDEO", purple)
        exportButton.setOnClickListener { exportTrimmedVideo() }
        exportCard.addView(exportButton, LinearLayout.LayoutParams(-1, dp(48)))
        content.addView(exportCard)
        content.addView(space(18))
        content.addView(label("MORE CREATIVE TOOLS", white, 11f, true))
        content.addView(space(8))
        val toolGrid = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val row1 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        listOf("TEXT & TITLES", "MUSIC & AUDIO", "FILTERS", "TRANSITIONS").forEach { name ->
            val t = toolTile(name)
            row1.addView(t, LinearLayout.LayoutParams(0, dp(58), 1f))
            if (name != "TRANSITIONS") row1.addView(spaceHorizontal(7))
        }
        toolGrid.addView(row1)
        content.addView(toolGrid)
        content.addView(space(16))
        content.addView(label("TUREL FX  •  CREATIVE TOOLS, BUILT FOR MOBILE", muted, 9f, true).apply { gravity = Gravity.CENTER })
        setContentView(root)
    }

    private fun simpleSeek(onValue: (Int) -> Unit) = object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
            if (fromUser) onValue(progress)
        }
        override fun onStartTrackingTouch(seekBar: SeekBar?) {}
        override fun onStopTrackingTouch(seekBar: SeekBar?) {}
    }

    private fun syncSeekBars() {
        if (durationMs <= 0) return
        startSeek.progress = trimStartMs.toInt().coerceIn(0, startSeek.max)
        endSeek.progress = trimEndMs.toInt().coerceIn(0, endSeek.max)
    }

    private fun updateTrimLabels() {
        if (!::startLabel.isInitialized) return
        startLabel.text = "START  •  ${formatTime(trimStartMs)}"
        endLabel.text = "END  •  ${formatTime(trimEndMs)}"
        timeLabel.text = "${formatTime(player?.currentPosition ?: 0L)}  /  ${formatTime(durationMs)}"
    }

    private fun exportTrimmedVideo() {
        val uri = selectedUri
        if (uri == null || durationMs <= 0L) {
            Toast.makeText(this, "Import a video before exporting", Toast.LENGTH_SHORT).show()
            return
        }
        if (trimEndMs <= trimStartMs) {
            Toast.makeText(this, "Choose a valid trim range", Toast.LENGTH_SHORT).show()
            return
        }
        if (exporting) return
        exporting = true
        exportButton.isEnabled = false
        exportButton.text = "EXPORTING..."
        val outputDir = getExternalFilesDir(android.os.Environment.DIRECTORY_MOVIES) ?: cacheDir
        val output = File(outputDir, "TUREL_FX_${System.currentTimeMillis()}.mp4")
        val clippedItem = MediaItem.Builder()
            .setUri(uri)
            .setClippingConfiguration(
                MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs(trimStartMs)
                    .setEndPositionMs(trimEndMs)
                    .build()
            )
            .build()
        val edited = EditedMediaItem.Builder(clippedItem).build()
        val transformer = Transformer.Builder(this)
            .addListener(object : Transformer.Listener {
                override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                    exporting = false
                    exportButton.isEnabled = true
                    exportButton.text = "⇧  EXPORT VIDEO"
                    Toast.makeText(this@MainActivity, "Export complete: ${output.name}", Toast.LENGTH_LONG).show()
                }
                override fun onError(composition: Composition, exportResult: ExportResult, exception: TransformationException) {
                    exporting = false
                    exportButton.isEnabled = true
                    exportButton.text = "⇧  EXPORT VIDEO"
                    output.delete()
                    Toast.makeText(this@MainActivity, "Export failed: ${exception.message}", Toast.LENGTH_LONG).show()
                }
            })
            .build()
        Toast.makeText(this, "Export started. Please keep the app open.", Toast.LENGTH_SHORT).show()
        transformer.start(edited, output.absolutePath)
    }

    private fun formatTime(ms: Long): String {
        val seconds = (ms.coerceAtLeast(0L) / 1000L)
        return String.format(Locale.US, "%02d:%02d", seconds / 60, seconds % 60)
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private fun space(height: Int) = View(this).apply { layoutParams = LinearLayout.LayoutParams(1, dp(height)) }
    private fun spaceHorizontal(width: Int) = View(this).apply { layoutParams = LinearLayout.LayoutParams(dp(width), 1) }

    private fun label(textValue: String, color: Int, size: Float, bold: Boolean = false) = TextView(this).apply {
        text = textValue
        textSize = size
        setTextColor(color)
        if (bold) typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.BOLD)
        letterSpacing = if (bold && size < 12f) .08f else 0f
    }

    private fun title(value: String) = TextView(this).apply {
        text = value
        textSize = 30f
        setTextColor(white)
        typeface = android.graphics.Typeface.create("sans-serif-black", android.graphics.Typeface.NORMAL)
        setLineSpacing(dp(1).toFloat(), 1f)
    }

    private fun card() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(12), dp(12), dp(12), dp(12))
        background = rounded(panel, 18)
    }

    private fun pill(textValue: String, color: Int) = TextView(this).apply {
        text = textValue
        textSize = 9f
        letterSpacing = .1f
        gravity = Gravity.CENTER
        setTextColor(color)
        setPadding(dp(12), 0, dp(12), 0)
        background = rounded(panel2, 30)
    }

    private fun actionButton(textValue: String, color: Int) = Button(this).apply {
        text = textValue
        textSize = 12f
        letterSpacing = .04f
        setTextColor(Color.WHITE)
        isAllCaps = false
        background = rounded(color, 14)
        stateListAnimator = null
    }

    private fun smallButton(textValue: String) = Button(this).apply {
        text = textValue
        textSize = 10f
        isAllCaps = false
        setTextColor(white)
        background = rounded(panel2, 10)
        stateListAnimator = null
    }

    private fun toolTile(value: String) = TextView(this).apply {
        text = value
        textSize = 9f
        gravity = Gravity.CENTER
        setTextColor(white)
        typeface = android.graphics.Typeface.DEFAULT_BOLD
        background = rounded(panel, 12)
    }

    private fun sectionHeading(left: String, right: String) = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        addView(label(left, white, 12f, true), LinearLayout.LayoutParams(0, -2, 1f))
        addView(label(right, purple, 9f, true))
    }

    private fun rounded(color: Int, radius: Int): android.graphics.drawable.GradientDrawable =
        android.graphics.drawable.GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
        }

    override fun onStop() {
        super.onStop()
        player?.pause()
    }

    override fun onDestroy() {
        playerView?.player = null
        player?.release()
        player = null
        super.onDestroy()
    }
}
