package com.soulstream.app.ui

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soulstream.app.PlayActivity
import com.soulstream.app.data.StatusItem
import com.soulstream.app.data.Statuses
import com.soulstream.app.ui.components.GhostButton
import com.soulstream.app.ui.components.GlowButton
import com.soulstream.app.ui.components.NeonCard
import com.soulstream.app.ui.components.PopIn
import com.soulstream.app.ui.components.SectionLabel
import com.soulstream.app.ui.theme.Hairline
import com.soulstream.app.ui.theme.Muted
import com.soulstream.app.ui.theme.OnDark
import com.soulstream.app.ui.theme.Surface1
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun StatusScreen() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val accentA = MaterialTheme.colorScheme.primary
    val accentB = MaterialTheme.colorScheme.secondary

    var hasAccess by remember { mutableStateOf(Statuses.hasAccess(ctx)) }
    var items by remember { mutableStateOf<List<StatusItem>>(emptyList()) }
    var preview by remember { mutableStateOf<StatusItem?>(null) }
    var scanning by remember { mutableStateOf(false) }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { hasAccess = Statuses.hasAccess(ctx) }

    fun rescan() {
        scope.launch {
            scanning = true
            val access = Statuses.hasAccess(ctx)
            hasAccess = access
            items = if (access) withContext(Dispatchers.IO) { Statuses.scan(ctx) } else emptyList()
            scanning = false
        }
    }

    LaunchedEffect(Unit) { rescan() }

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp)
    ) {
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    "STATUS VAULT",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        brush = Brush.horizontalGradient(listOf(accentA, accentB))
                    )
                )
                Text(
                    "${items.size} saved by WhatsApp - grab them before they expire",
                    style = MaterialTheme.typography.bodySmall,
                    color = Muted
                )
            }
            IconButton(onClick = { rescan() }) {
                Icon(Icons.Rounded.Refresh, contentDescription = "Refresh", tint = accentA)
            }
        }
        Spacer(Modifier.height(10.dp))

        when {
            !hasAccess -> {
                PopIn {
                    NeonCard(Modifier.fillMaxWidth(), accent = accentA) {
                        SectionLabel("One-time unlock", accentA)
                        Text("Allow file access", style = MaterialTheme.typography.titleMedium, color = OnDark)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "WhatsApp hides statuses in a private folder. Android needs \"All files access\" " +
                                "once so SoulStream can read them. Nothing leaves your phone.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Muted
                        )
                        Spacer(Modifier.height(12.dp))
                        GlowButton(
                            text = "Unlock statuses",
                            modifier = Modifier.fillMaxWidth(),
                            icon = Icons.Rounded.Folder,
                            accentA = accentA,
                            accentB = accentB
                        ) {
                            if (Build.VERSION.SDK_INT >= 30) {
                                try {
                                    ctx.startActivity(
                                        Intent(
                                            Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                                            Uri.parse("package:" + ctx.packageName)
                                        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    )
                                } catch (e: Exception) {
                                    ctx.startActivity(
                                        Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    )
                                }
                            } else {
                                permLauncher.launch(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                            }
                        }
                    }
                }
            }

            items.isEmpty() -> EmptyStatuses(accentA)

            else -> {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GlowButton(
                        text = if (scanning) "Scanning..." else "Save all",
                        modifier = Modifier.weight(1f),
                        icon = Icons.Rounded.Save,
                        enabled = !scanning,
                        accentA = accentA,
                        accentB = accentB
                    ) {
                        scope.launch {
                            val msg = withContext(Dispatchers.IO) { Statuses.saveAll(ctx, items) }
                            Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()
                        }
                    }
                    GhostButton(
                        text = "Refresh",
                        modifier = Modifier.weight(1f),
                        icon = Icons.Rounded.Refresh,
                        accent = accentB
                    ) { rescan() }
                }
                Spacer(Modifier.height(12.dp))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(items, key = { it.file.absolutePath }) { item ->
                        StatusTile(item, accentA) {
                            if (item.isVideo) {
                                playFile(ctx, item.file.absolutePath, item.file.name)
                            } else {
                                preview = item
                            }
                        }
                    }
                }
            }
        }
    }

    preview?.let { item ->
        StatusPreview(item, accentA, accentB) { preview = null }
    }
}

@Composable
private fun EmptyStatuses(accent: Color) {
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
                Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = accent, modifier = Modifier.size(34.dp))
            }
            Spacer(Modifier.height(14.dp))
            Text("No statuses yet", color = OnDark, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                "Open WhatsApp, view a status, then hit refresh here",
                color = Muted,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun StatusTile(item: StatusItem, accent: Color, onClick: () -> Unit) {
    val ctx = LocalContext.current
    val bmp = remember(item.file.absolutePath) { loadThumb(ctx, item) }
    Box(
        Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(14.dp))
            .background(Surface1)
            .border(1.dp, Hairline, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
    ) {
        if (bmp != null) {
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(if (item.isVideo) "\u25B6" else "IMG", fontSize = 16.sp, color = Muted)
            }
        }
        if (item.isVideo) {
            Box(
                Modifier
                    .align(Alignment.Center)
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.45f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun StatusPreview(
    item: StatusItem,
    accentA: Color,
    accentB: Color,
    onClose: () -> Unit
) {
    val ctx = LocalContext.current
    val bmp = remember(item.file.absolutePath) { loadThumb(ctx, item, full = true) }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.92f))
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    item.file.name,
                    color = OnDark,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onClose) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close", tint = OnDark)
                }
            }
            Spacer(Modifier.height(8.dp))

            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                if (bmp != null) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                GlowButton(
                    text = "Save",
                    modifier = Modifier.weight(1f),
                    icon = Icons.Rounded.Download,
                    accentA = accentA,
                    accentB = accentB
                ) {
                    Toast.makeText(ctx, Statuses.saveToGallery(ctx, item), Toast.LENGTH_SHORT).show()
                }
                GhostButton(
                    text = "Share",
                    modifier = Modifier.weight(1f),
                    icon = Icons.Rounded.Share,
                    accent = accentB
                ) {
                    val uri = Statuses.shareUri(ctx, item)
                    if (uri == null) {
                        Toast.makeText(ctx, "Could not share", Toast.LENGTH_SHORT).show()
                    } else {
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = if (item.isVideo) "video/*" else "image/*"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        ctx.startActivity(Intent.createChooser(intent, "Share status"))
                    }
                }
            }
        }
    }
}

private fun playFile(ctx: android.content.Context, path: String, title: String) {
    try {
        ctx.startActivity(
            Intent(ctx, PlayActivity::class.java)
                .putExtra("uri", Uri.fromFile(java.io.File(path)).toString())
                .putExtra("title", title)
        )
    } catch (e: Exception) {
        Toast.makeText(ctx, "Could not play this file", Toast.LENGTH_SHORT).show()
    }
}

private fun loadThumb(ctx: android.content.Context, item: StatusItem, full: Boolean = false): Bitmap? {
    return try {
        if (item.isVideo) {
            val mmr = MediaMetadataRetriever()
            mmr.setDataSource(item.file.absolutePath)
            val frame = mmr.getFrameAtTime(1_000_000L)
            mmr.release()
            frame
        } else {
            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(item.file.absolutePath, opts)
            var sample = 1
            val target = if (full) 1400 else 400
            while (opts.outWidth / (sample * 2) >= target && opts.outHeight / (sample * 2) >= target) {
                sample *= 2
            }
            BitmapFactory.decodeFile(
                item.file.absolutePath,
                BitmapFactory.Options().apply { inSampleSize = sample }
            )
        }
    } catch (e: Exception) {
        null
    }
}
