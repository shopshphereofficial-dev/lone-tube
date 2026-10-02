package com.soulstream.app

import android.net.Uri
import android.os.Bundle
import android.widget.VideoView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import com.soulstream.app.data.Prefs
import com.soulstream.app.ui.theme.OnDark
import com.soulstream.app.ui.theme.SoulTheme

/**
 * Full-screen player.
 *
 * The VideoView is an interop view, so only the small control overlays paint on
 * top of it - no full-screen Compose background.
 */
class PlayActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val uri = intent?.getStringExtra("uri")
        val title = intent?.getStringExtra("title") ?: "Playing"
        if (uri.isNullOrBlank()) {
            finish()
            return
        }

        setContent {
            SoulTheme(accentIndex = Prefs.accent(this)) {
                val accentA = MaterialTheme.colorScheme.primary
                val accentB = MaterialTheme.colorScheme.secondary
                var isPlaying by remember { mutableStateOf(true) }
                var videoView by remember { mutableStateOf<VideoView?>(null) }
                var controlsVisible by remember { mutableStateOf(true) }

                Box(
                    Modifier
                        .fillMaxSize()
                        .clickable { controlsVisible = !controlsVisible }
                ) {
                    AndroidView(
                        factory = { c ->
                            VideoView(c).apply {
                                setBackgroundColor(android.graphics.Color.BLACK)
                                setVideoURI(Uri.parse(uri))
                                setOnPreparedListener {
                                    it.isLooping = true
                                    start()
                                    isPlaying = true
                                }
                                setOnErrorListener { _, _, _ -> true }
                                videoView = this
                            }
                        },
                        modifier = Modifier.fillMaxSize(),
                        update = { }
                    )

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
                                IconButton(onClick = { finish() }) {
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
                                BigPlayButton(isPlaying, accentA, accentB) {
                                    val vv = videoView
                                    if (vv != null) {
                                        if (vv.isPlaying) {
                                            vv.pause()
                                            isPlaying = false
                                        } else {
                                            vv.start()
                                            isPlaying = true
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
