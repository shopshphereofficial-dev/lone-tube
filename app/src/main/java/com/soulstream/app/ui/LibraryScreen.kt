package com.soulstream.app.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import com.soulstream.app.PlayActivity
import com.soulstream.app.data.History
import com.soulstream.app.ui.components.GhostButton
import com.soulstream.app.ui.components.PopIn
import com.soulstream.app.ui.theme.Hairline
import com.soulstream.app.ui.theme.Muted
import com.soulstream.app.ui.theme.OnDark
import com.soulstream.app.ui.theme.Surface1
import com.soulstream.app.ui.theme.Surface2
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LibraryScreen() {
    val ctx = LocalContext.current
    val accentA = MaterialTheme.colorScheme.primary
    val accentB = MaterialTheme.colorScheme.secondary
    var all by remember { mutableStateOf(History.list(ctx)) }
    var query by remember { mutableStateOf("") }
    var appeared by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        all = History.list(ctx)
        appeared = true
    }

    val list = if (query.isBlank()) all
    else all.filter { it.name.lowercase().contains(query.lowercase()) }

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp)
    ) {
        Spacer(Modifier.height(16.dp))
        Text(
            "LIBRARY",
            style = MaterialTheme.typography.headlineMedium.copy(
                brush = Brush.horizontalGradient(listOf(accentA, accentB))
            )
        )
        Text(
            "${all.size} files in your Downloads folder",
            style = MaterialTheme.typography.bodySmall,
            color = Muted
        )
        Spacer(Modifier.height(12.dp))

        if (all.isEmpty()) {
            EmptyLibrary(accentA)
        } else {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                placeholder = { Text("Search your files", color = Muted) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accentA,
                    unfocusedBorderColor = Hairline,
                    focusedContainerColor = Surface2.copy(alpha = 0.4f),
                    unfocusedContainerColor = Surface2.copy(alpha = 0.4f),
                    focusedTextColor = OnDark,
                    unfocusedTextColor = OnDark,
                    cursorColor = accentA
                )
            )
            Spacer(Modifier.height(12.dp))
            AnimatedVisibility(
                visible = appeared,
                enter = fadeIn(tween(380)) + slideInVertically(tween(430)) { it / 16 }
            ) {
                LazyColumn(Modifier.fillMaxSize()) {
                    items(list, key = { it.uri }) { item ->
                        LibraryRow(
                            item = item,
                            accentA = accentA,
                            accentB = accentB,
                            onOpen = { open(ctx, item) },
                            onShare = { share(ctx, item) },
                            onDelete = {
                                delete(ctx, item)
                                all = History.list(ctx)
                            }
                        )
                        Spacer(Modifier.height(10.dp))
                    }
                    item {
                        Spacer(Modifier.height(6.dp))
                        GhostButton(
                            text = "Clear list",
                            modifier = Modifier.fillMaxWidth(),
                            icon = Icons.Rounded.DeleteSweep,
                            accent = accentA
                        ) {
                            History.clear(ctx)
                            all = History.list(ctx)
                        }
                        Spacer(Modifier.height(22.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyLibrary(accent: Color) {
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
                    .background(accent.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.FolderOpen, contentDescription = null, tint = accent, modifier = Modifier.size(34.dp))
            }
            Spacer(Modifier.height(14.dp))
            Text("Nothing captured yet", color = OnDark, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                "Everything you download lands here",
                color = Muted,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun LibraryRow(
    item: History.Item,
    accentA: Color,
    accentB: Color,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.98f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 800f),
        label = "row"
    )
    val isAudio = item.mime.startsWith("audio")
    val tint = if (isAudio) accentB else accentA

    PopIn {
        Row(
            Modifier
                .fillMaxWidth()
                .scale(scale)
                .clip(RoundedCornerShape(18.dp))
                .background(Surface1.copy(alpha = 0.85f))
                .border(1.dp, Hairline, RoundedCornerShape(18.dp))
                .clickable(interactionSource = interaction, indication = null, onClick = onOpen)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PlayBadge(tint, isAudio, onOpen)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    item.name,
                    color = OnDark,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    humanSize(item.size) + " - " + dateOf(item.time),
                    color = Muted,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            IconButton(onClick = onShare) {
                Icon(Icons.Rounded.Share, contentDescription = "Share", tint = Muted, modifier = Modifier.size(20.dp))
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Rounded.DeleteSweep, contentDescription = "Delete", tint = Muted, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun PlayBadge(tint: Color, isAudio: Boolean, onOpen: () -> Unit) {
    val t = rememberInfiniteTransition(label = "play")
    val pulse by t.animateFloat(
        initialValue = 1f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(1300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    Box(
        Modifier
            .size(44.dp)
            .scale(pulse)
            .clip(RoundedCornerShape(14.dp))
            .background(tint.copy(alpha = 0.18f))
            .clickable(onClick = onOpen),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isAudio) Icons.Rounded.MusicNote else Icons.Rounded.PlayArrow,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(22.dp)
        )
    }
}

private fun open(ctx: Context, item: History.Item) {
    val playable = item.mime.startsWith("video") || item.mime.startsWith("audio")
    try {
        if (playable) {
            ctx.startActivity(
                Intent(ctx, PlayActivity::class.java)
                    .putExtra("uri", item.uri)
                    .putExtra("title", item.name)
            )
        } else {
            ctx.startActivity(
                Intent(Intent.ACTION_VIEW)
                    .setDataAndType(Uri.parse(item.uri), item.mime)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    } catch (e: Exception) {
        Toast.makeText(ctx, "No app can open this file", Toast.LENGTH_SHORT).show()
    }
}

private fun share(ctx: Context, item: History.Item) {
    try {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = item.mime
            putExtra(Intent.EXTRA_STREAM, Uri.parse(item.uri))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        ctx.startActivity(Intent.createChooser(intent, "Share"))
    } catch (e: Exception) {
        Toast.makeText(ctx, "Could not share", Toast.LENGTH_SHORT).show()
    }
}

private fun delete(ctx: Context, item: History.Item) {
    try {
        ctx.contentResolver.delete(Uri.parse(item.uri), null, null)
    } catch (e: Exception) {
        // file may already be gone
    }
    History.remove(ctx, item.uri)
    Toast.makeText(ctx, "Deleted", Toast.LENGTH_SHORT).show()
}

private fun humanSize(bytes: Long): String {
    if (bytes <= 0) return "-"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1 -> String.format(Locale.US, "%.2f GB", gb)
        mb >= 1 -> String.format(Locale.US, "%.1f MB", mb)
        else -> String.format(Locale.US, "%.0f KB", kb)
    }
}

private fun dateOf(ts: Long): String =
    SimpleDateFormat("dd MMM, hh:mm a", Locale.ENGLISH).format(Date(ts))
