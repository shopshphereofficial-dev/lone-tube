package com.lonetube.app.ui

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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import android.widget.VideoView
import com.lonetube.app.data.StatusItem
import com.lonetube.app.data.Statuses
import com.lonetube.app.ui.components.GlassCard
import com.lonetube.app.ui.components.GradientButton
import com.lonetube.app.ui.components.SoftButton
import com.lonetube.app.ui.theme.Cyan
import com.lonetube.app.ui.theme.Hairline
import com.lonetube.app.ui.theme.Muted
import com.lonetube.app.ui.theme.OnDark
import com.lonetube.app.ui.theme.Surface1
import com.lonetube.app.ui.theme.Violet

@Composable
fun StatusScreen() {
    val ctx = LocalContext.current
    var hasAccess by remember { mutableStateOf(Statuses.hasAccess(ctx)) }
    var items by remember { mutableStateOf<List<StatusItem>>(emptyList()) }
    var preview by remember { mutableStateOf<StatusItem?>(null) }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { hasAccess = Statuses.hasAccess(ctx) }

    fun rescan() {
        hasAccess = Statuses.hasAccess(ctx)
        items = if (hasAccess) Statuses.scan(ctx) else emptyList()
    }

    LaunchedEffect(Unit) { rescan() }

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp)
    ) {
        Spacer(Modifier.height(18.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("WhatsApp statuses", style = MaterialTheme.typography.headlineMedium, color = OnDark)
                Text(
                    "${items.size} found - save or share them before they vanish",
                    style = MaterialTheme.typography.bodySmall,
                    color = Muted
                )
            }
            IconButton(onClick = { rescan() }) {
                Icon(Icons.Rounded.Refresh, contentDescription = "Refresh", tint = Cyan)
            }
        }
        Spacer(Modifier.height(12.dp))

        if (!hasAccess) {
            AccessCard(
                onGrant = {
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
            )
        } else if (items.isEmpty()) {
            EmptyStatuses()
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(items, key = { it.file.absolutePath }) { item ->
                    StatusTile(item) { preview = item }
                }
            }
        }
    }

    preview?.let { item ->
        StatusPreview(
            item = item,
            onClose = { preview = null }
        )
    }
}

@Composable
private fun AccessCard(onGrant: () -> Unit) {
    GlassCard(Modifier.fillMaxWidth()) {
        Text("Allow file access", style = MaterialTheme.typography.titleMedium, color = OnDark)
        Spacer(Modifier.height(4.dp))
        Text(
            "WhatsApp keeps its statuses in a private folder. Android needs you to allow " +
                "LoneTube \"All files access\" once so it can read them. Nothing is uploaded anywhere.",
            style = MaterialTheme.typography.bodySmall,
            color = Muted
        )
        Spacer(Modifier.height(12.dp))
        GradientButton(
            text = "Open settings",
            modifier = Modifier.fillMaxWidth(),
            icon = Icons.Rounded.Folder
        ) { onGrant() }
    }
}

@Composable
private fun EmptyStatuses() {
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
                    Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    tint = Violet,
                    modifier = Modifier.size(34.dp)
                )
            }
            Spacer(Modifier.height(14.dp))
            Text("No statuses right now", color = OnDark, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                "Open WhatsApp and view a status, then tap refresh here",
                color = Muted,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun StatusTile(item: StatusItem, onClick: () -> Unit) {
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
                Text(if (item.isVideo) "▶" else "🖼", fontSize = 24.sp, color = Muted)
            }
        }
        if (item.isVideo) {
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.55f)),
                contentAlignment = Alignment.Center
            ) {
                Text("▶", fontSize = 10.sp, color = Color.White)
            }
        }
    }
}

@Composable
private fun StatusPreview(item: StatusItem, onClose: () -> Unit) {
    val ctx = LocalContext.current
    val showVideo = item.isVideo
    val bmp = remember(item.file.absolutePath) { loadThumb(ctx, item, full = true) }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.9f))
            .clickable { onClose() }
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
                if (showVideo) {
                    AndroidView(
                        factory = { c ->
                            VideoView(c).apply {
                                setVideoURI(Uri.fromFile(item.file))
                                setOnPreparedListener { it.isLooping = true; start() }
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (bmp != null) {
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
                GradientButton(
                    text = "Save",
                    modifier = Modifier.weight(1f),
                    icon = Icons.Rounded.Download
                ) {
                    val msg = Statuses.saveToGallery(ctx, item)
                    Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()
                }
                SoftButton(
                    text = "Share",
                    modifier = Modifier.weight(1f),
                    icon = Icons.Rounded.Share
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
