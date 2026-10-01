package com.lonetube.app.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Videocam
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lonetube.app.data.History
import com.lonetube.app.ui.components.SoftButton
import com.lonetube.app.ui.theme.Cyan
import com.lonetube.app.ui.theme.Hairline
import com.lonetube.app.ui.theme.Muted
import com.lonetube.app.ui.theme.OnDark
import com.lonetube.app.ui.theme.Pink
import com.lonetube.app.ui.theme.Surface1
import com.lonetube.app.ui.theme.Violet
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LibraryScreen() {
    val ctx = LocalContext.current
    var list by remember { mutableStateOf(History.list(ctx)) }
    var appeared by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        list = History.list(ctx)
        appeared = true
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp)
    ) {
        Spacer(Modifier.height(18.dp))
        Text("Library", style = MaterialTheme.typography.headlineMedium, color = OnDark)
        Text(
            "${list.size} download${if (list.size == 1) "" else "s"} - saved in your Downloads folder",
            style = MaterialTheme.typography.bodySmall,
            color = Muted
        )
        Spacer(Modifier.height(16.dp))

        if (list.isEmpty()) {
            EmptyLibrary()
        } else {
            AnimatedVisibility(
                visible = appeared,
                enter = fadeIn(tween(400)) + slideInVertically(tween(450)) { it / 14 }
            ) {
                LazyColumn(Modifier.fillMaxSize()) {
                    items(list, key = { it.uri }) { item ->
                        LibraryRow(
                            item = item,
                            onOpen = { open(ctx, item) },
                            onDelete = {
                                delete(ctx, item)
                                list = History.list(ctx)
                            }
                        )
                        Spacer(Modifier.height(10.dp))
                    }
                    item {
                        Spacer(Modifier.height(6.dp))
                        SoftButton(
                            text = "Clear list",
                            modifier = Modifier.fillMaxWidth(),
                            icon = Icons.Rounded.DeleteSweep
                        ) {
                            History.clear(ctx)
                            list = History.list(ctx)
                        }
                        Spacer(Modifier.height(22.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyLibrary() {
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
                    .background(Violet.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.FolderOpen,
                    contentDescription = null,
                    tint = Violet,
                    modifier = Modifier.size(34.dp)
                )
            }
            Spacer(Modifier.height(14.dp))
            Text("Nothing here yet", color = OnDark, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                "Downloads you start will show up here",
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
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.98f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 800f),
        label = "row"
    )
    val isAudio = item.mime.startsWith("audio")
    Row(
        Modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(18.dp))
            .background(Surface1.copy(alpha = 0.7f))
            .border(1.dp, Hairline, RoundedCornerShape(18.dp))
            .clickable(interactionSource = interaction, indication = null, onClick = onOpen)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(if (isAudio) Pink.copy(alpha = 0.16f) else Cyan.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isAudio) Icons.Rounded.MusicNote else Icons.Rounded.Videocam,
                contentDescription = null,
                tint = if (isAudio) Pink else Cyan,
                modifier = Modifier.size(20.dp)
            )
        }
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
        IconButton(onClick = onOpen) {
            Icon(Icons.Rounded.PlayCircle, contentDescription = "Open", tint = Cyan)
        }
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Rounded.DeleteSweep,
                contentDescription = "Delete",
                tint = Muted,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

private fun open(ctx: Context, item: History.Item) {
    try {
        ctx.startActivity(
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(Uri.parse(item.uri), item.mime)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    } catch (e: Exception) {
        Toast.makeText(ctx, "No app can open this file", Toast.LENGTH_SHORT).show()
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
