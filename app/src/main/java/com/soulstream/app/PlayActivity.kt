package com.soulstream.app

import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.VideoView
import androidx.activity.ComponentActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.soulstream.app.data.Prefs
import com.soulstream.app.ui.theme.OnDark
import com.soulstream.app.ui.theme.SoulTheme

/**
 * Full-screen player.
 *
 * Same architecture as the browser: a plain VideoView inside a FrameLayout with
 * a Compose overlay on top - no interop views, so the video always renders.
 */
class PlayActivity : ComponentActivity() {

    private var playing by mutableStateOf(true)
    private var controlsVisible by mutableStateOf(true)
    private var videoView: VideoView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val uri = intent?.getStringExtra("uri")
        val title = intent?.getStringExtra("title") ?: "Playing"
        if (uri.isNullOrBlank()) {
            finish()
            return
        }

        val player = VideoView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(android.graphics.Color.BLACK)
            setVideoURI(Uri.parse(uri))
            setOnPreparedListener {
                it.isLooping = true
                start()
                playing = true
            }
            setOnErrorListener { _, _, _ -> true }
            videoView = this
        }

        val root = FrameLayout(this)
        root.addView(player)

        val overlay = ComposeView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setContent {
                SoulTheme(accentIndex = Prefs.accent(this@PlayActivity)) {
                    PlayerOverlay(title) { finish() }
                }
            }
        }
        root.addView(overlay)
        setContentView(root)
    }

    @Composable
    private fun PlayerOverlay(title: String, onClose: () -> Unit) {
        val accentA = MaterialTheme.colorScheme.primary
        val accentB = MaterialTheme.colorScheme.secondary

        Box(
            Modifier
                .fillMaxSize()
                .clickable { controlsVisible = !controlsVisible }
        ) {
            AnimatedVisibility(
                visible = controlsVisible,
                enter = fadeIn(tween(200)),
                exit = fadeOut(tween(180))
            ) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .systemBarsPadding()
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onClose) {
                            Icon(Icons.Rounded.ArrowBack, contentDescription = "Back", tint = OnDark)
                        }
                        Text(
                            title,
                            color = OnDark,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BigPlayButton(playing, accentA, accentB) {
                            val vv = videoView
                            if (vv != null) {
                                if (vv.isPlaying) {
                                    vv.pause()
                                    playing = false
                                } else {
                                    vv.start()
                                    playing = true
                                }
                            }
                        }
                    }
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun BigPlayButton(isPlaying: Boolean, accentA: Color, accentB: Color, onClick: () -> Unit) {
    val scale by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 1.12f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 500f),
        label = "bigplay"
    )
    Box(
        Modifier
            .size(78.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(accentA, accentB)))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
            contentDescription = null,
            tint = Color(0xFF03060E),
            modifier = Modifier.size(40.dp)
        )
    }
}
