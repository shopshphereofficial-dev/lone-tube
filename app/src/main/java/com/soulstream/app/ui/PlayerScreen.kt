package com.soulstream.app.ui

import android.net.Uri
import android.widget.VideoView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.soulstream.app.data.History
import com.soulstream.app.ui.components.PopIn
import com.soulstream.app.ui.theme.Hairline
import com.soulstream.app.ui.theme.Muted
import com.soulstream.app.ui.theme.OnDark
import com.soulstream.app.ui.theme.Surface1

/** Play what you downloaded - no need to leave the app. */
@Composable
fun PlayerScreen() {
    val ctx = LocalContext.current
    val accentA = MaterialTheme.colorScheme.primary
    val accentB = MaterialTheme.colorScheme.secondary
    var all by remember { mutableStateOf(History.list(ctx)) }
    var playing by remember { mutableStateOf<History.Item?>(null) }

    LaunchedEffect(Unit) { all = History.list(ctx) }

    val media = all.filter { it.mime.startsWith("video") || it.mime.startsWith("audio") }

    val current = playing
    if (current != null) {
        FullPlayer(current, accentA, accentB) { playing = null }
        return
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp)
    ) {
        Spacer(Modifier.height(16.dp))
        Text(
            "PLAY ROOM",
            style = MaterialTheme.typography.headlineMedium.copy(
                brush = Brush.horizontalGradient(listOf(accentA, accentB))
            )
        )
        Text(
            "${media.size} playable files",
            style = MaterialTheme.typography.bodySmall,
            color = Muted
        )
        Spacer(Modifier.height(14.dp))

        if (media.isEmpty()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(bottom = 60.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier
                            .size(74.dp)
                            .clip(CircleShape)
                            .background(accentA.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = accentA, modifier = Modifier.size(34.dp))
                    }
                    Spacer(Modifier.height(14.dp))
                    Text("Nothing to play yet", color = OnDark, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Download a video or MP3 and it shows up here",
                        color = Muted,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(media, key = { it.uri }) { item ->
                    PlayerTile(item, accentA, accentB) { playing = item }
                }
            }
        }
    }
}

@Composable
private fun PlayerTile(
    item: History.Item,
    accentA: Color,
    accentB: Color,
    onPlay: () -> Unit
) {
    val isAudio = item.mime.startsWith("audio")
    val t = rememberInfiniteTransition(label = "tile")
    val pulse by t.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    PopIn {
        Column(
            Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(Surface1.copy(alpha = 0.85f))
                .border(1.dp, Hairline, RoundedCornerShape(16.dp))
                .clickable(onClick = onPlay)
                .padding(10.dp)
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.5f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Brush.linearGradient(listOf(accentA.copy(alpha = 0.22f), accentB.copy(alpha = 0.18f)))),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier
                        .size(46.dp)
                        .scale(pulse)
                        .clip(CircleShape)
                        .background(Color(0xFF03060E).copy(alpha = 0.55f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isAudio) Icons.Rounded.MusicNote else Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        tint = if (isAudio) accentB else accentA,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                item.name,
                color = OnDark,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun FullPlayer(
    item: History.Item,
    accentA: Color,
    accentB: Color,
    onBack: () -> Unit
) {
    var isPlaying by remember { mutableStateOf(true) }
    var videoView by remember { mutableStateOf<VideoView?>(null) }
    var controlsVisible by remember { mutableStateOf(true) }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable { controlsVisible = !controlsVisible }
    ) {
        AndroidView(
            factory = { c ->
                VideoView(c).apply {
                    setVideoURI(Uri.parse(item.uri))
                    setOnPreparedListener {
                        it.isLooping = true
                        start()
                        isPlaying = true
                    }
                    videoView = this
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { }
        )

        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(tween(200)) + slideInVertically(tween(240)) { -it / 8 },
            exit = fadeOut(tween(180))
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Back", tint = OnDark)
                    }
                    Text(
                        item.name,
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
