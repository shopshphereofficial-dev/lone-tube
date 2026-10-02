package com.soulstream.app.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Warning
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soulstream.app.engine.Tools
import com.soulstream.app.ui.components.GhostButton
import com.soulstream.app.ui.components.GlowButton
import com.soulstream.app.ui.components.NeonCard
import com.soulstream.app.ui.components.PopIn
import com.soulstream.app.ui.components.SectionLabel
import com.soulstream.app.ui.theme.Hairline
import com.soulstream.app.ui.theme.Muted
import com.soulstream.app.ui.theme.NeonLime
import com.soulstream.app.ui.theme.OnDark
import com.soulstream.app.ui.theme.Surface2
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** SnapTube-style toolbox: cleaner utilities that really work on the filesystem. */
@Composable
fun ToolsScreen() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val accentA = MaterialTheme.colorScheme.primary
    val accentB = MaterialTheme.colorScheme.secondary

    var hasAccess by remember { mutableStateOf(Tools.hasAccess(ctx)) }
    var busy by remember { mutableStateOf(false) }

    var junkBytes by remember { mutableStateOf(0L) }
    var junkItems by remember { mutableStateOf(0) }
    var trash by remember { mutableStateOf<List<File>>(emptyList()) }
    var bigFiles by remember { mutableStateOf<List<File>>(emptyList()) }
    var bigPhotos by remember { mutableStateOf<List<File>>(emptyList()) }
    var waFolders by remember { mutableStateOf<List<Tools.Folder>>(emptyList()) }
    var apps by remember { mutableStateOf<List<Tools.AppEntry>>(emptyList()) }

    var showBig by remember { mutableStateOf(false) }
    var showPhotos by remember { mutableStateOf(false) }
    var showApps by remember { mutableStateOf(false) }
    var showWa by remember { mutableStateOf(false) }

    var battery by remember { mutableStateOf("") }
    var storage by remember { mutableStateOf("") }
    var ram by remember { mutableStateOf("") }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { hasAccess = Tools.hasAccess(ctx) }

    fun rescan() {
        scope.launch {
            busy = true
            val access = Tools.hasAccess(ctx)
            hasAccess = access
            battery = Tools.batteryLine(ctx)
            storage = Tools.storageLine()
            ram = Tools.ramLine(ctx)
            if (access) {
                val j = withContext(Dispatchers.IO) { Tools.scanJunk(ctx) }
                junkBytes = j.bytes
                junkItems = j.items
                trash = withContext(Dispatchers.IO) { Tools.scanTrash(ctx) }
                bigFiles = withContext(Dispatchers.IO) { Tools.scanLarge(ctx) }
                bigPhotos = withContext(Dispatchers.IO) { Tools.scanLargePhotos(ctx) }
                waFolders = withContext(Dispatchers.IO) { Tools.whatsappFolders() }
            }
            busy = false
        }
    }

    LaunchedEffect(Unit) { rescan() }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp)
    ) {
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    "TOOLBOX",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        brush = Brush.horizontalGradient(listOf(accentA, accentB))
                    )
                )
                Text(
                    if (busy) "Scanning your phone..." else "Clean, free up space, manage apps",
                    style = MaterialTheme.typography.bodySmall,
                    color = Muted
                )
            }
            IconButton(onClick = { rescan() }) {
                Icon(Icons.Rounded.Refresh, contentDescription = "Refresh", tint = accentA)
            }
        }
        Spacer(Modifier.height(12.dp))

        if (!hasAccess) {
            PopIn {
                NeonCard(Modifier.fillMaxWidth(), accent = accentA) {
                    SectionLabel("One-time unlock", accentA)
                    Text("Allow file access", style = MaterialTheme.typography.titleMedium, color = OnDark)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "The toolbox reads real folders (Downloads, WhatsApp, DCIM). Android needs " +
                            "\"All files access\" once. Nothing leaves your phone.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Muted
                    )
                    Spacer(Modifier.height(12.dp))
                    GlowButton(
                        text = "Unlock toolbox",
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
            Spacer(Modifier.height(12.dp))
        }

        PopIn {
            NeonCard(Modifier.fillMaxWidth(), accent = accentB) {
                SectionLabel("Phone status", accentB)
                Text(storage, color = OnDark, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(2.dp))
                Text(ram, color = Muted, style = MaterialTheme.typography.bodySmall)
                Text(battery, color = Muted, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GhostButton(
                        text = "Boost",
                        modifier = Modifier.weight(1f),
                        icon = Icons.Rounded.Bolt,
                        accent = accentA
                    ) {
                        scope.launch {
                            val msg = withContext(Dispatchers.IO) { Tools.cleanJunk(ctx) }
                            Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()
                            rescan()
                        }
                    }
                    GhostButton(
                        text = "Battery saver",
                        modifier = Modifier.weight(1f),
                        icon = Icons.Rounded.Bolt,
                        accent = accentB
                    ) { Tools.openBatterySettings(ctx) }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        PopIn(delayMillis = 40) {
            NeonCard(Modifier.fillMaxWidth(), accent = accentA) {
                SectionLabel("Junk clean", accentA)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (junkBytes > 0) Tools.human(junkBytes) else "All clean",
                            color = if (junkBytes > 0) NeonLime else OnDark,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            "$junkItems junk items found",
                            color = Muted,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    GlowButton(
                        text = "CLEAN",
                        icon = Icons.Rounded.DeleteSweep,
                        enabled = hasAccess && !busy,
                        accentA = accentA,
                        accentB = accentB
                    ) {
                        scope.launch {
                            val msg = withContext(Dispatchers.IO) { Tools.cleanJunk(ctx) }
                            Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()
                            rescan()
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        PopIn(delayMillis = 80) {
            NeonCard(Modifier.fillMaxWidth(), accent = accentB) {
                SectionLabel("WhatsApp clean", accentB)
                val total = waFolders.sumOf { it.bytes }
                Text(Tools.human(total), color = OnDark, style = MaterialTheme.typography.titleLarge)
                Text(
                    "WhatsApp media taking space",
                    color = Muted,
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GhostButton(
                        text = if (showWa) "Hide" else "Details",
                        modifier = Modifier.weight(1f),
                        accent = accentB
                    ) { showWa = !showWa }
                    GhostButton(
                        text = "Clean trashed",
                        modifier = Modifier.weight(1f),
                        icon = Icons.Rounded.DeleteSweep,
                        accent = accentB
                    ) {
                        scope.launch {
                            val msg = withContext(Dispatchers.IO) {
                                Tools.deleteFiles(Tools.scanTrash(ctx))
                            }
                            Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()
                            rescan()
                        }
                    }
                }
                AnimatedVisibility(visible = showWa) {
                    Column(Modifier.padding(top = 10.dp)) {
                        waFolders.take(10).forEach { f ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    f.name,
                                    color = OnDark,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(Tools.human(f.bytes), color = accentB, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        PopIn(delayMillis = 120) {
            NeonCard(Modifier.fillMaxWidth(), accent = accentA) {
                SectionLabel("Trash clean", accentA)
                Text(
                    "${trash.size} deleted files still on disk",
                    color = OnDark,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    Tools.human(trash.sumOf { it.length() }),
                    color = Muted,
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.height(10.dp))
                GlowButton(
                    text = "EMPTY TRASH",
                    modifier = Modifier.fillMaxWidth(),
                    icon = Icons.Rounded.DeleteSweep,
                    enabled = hasAccess && trash.isNotEmpty() && !busy,
                    accentA = accentA,
                    accentB = accentB
                ) {
                    scope.launch {
                        val msg = withContext(Dispatchers.IO) { Tools.deleteFiles(trash) }
                        Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()
                        rescan()
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        PopIn(delayMillis = 160) {
            NeonCard(Modifier.fillMaxWidth(), accent = accentB) {
                SectionLabel("Large files", accentB)
                Text(
                    "${bigFiles.size} files over 20 MB",
                    color = OnDark,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    Tools.human(bigFiles.sumOf { it.length() }),
                    color = Muted,
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GhostButton(
                        text = if (showBig) "Hide" else "Show list",
                        modifier = Modifier.weight(1f),
                        accent = accentB
                    ) { showBig = !showBig }
                    GhostButton(
                        text = "Delete shown",
                        modifier = Modifier.weight(1f),
                        icon = Icons.Rounded.DeleteSweep,
                        accent = accentB
                    ) {
                        scope.launch {
                            val msg = withContext(Dispatchers.IO) { Tools.deleteFiles(bigFiles) }
                            Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()
                            rescan()
                        }
                    }
                }
                AnimatedVisibility(visible = showBig) {
                    FileList(bigFiles.take(12), accentB) { f ->
                        scope.launch {
                            val msg = withContext(Dispatchers.IO) { Tools.deleteFiles(listOf(f)) }
                            Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()
                            rescan()
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        PopIn(delayMillis = 200) {
            NeonCard(Modifier.fillMaxWidth(), accent = accentA) {
                SectionLabel("Photos clean", accentA)
                Text(
                    "${bigPhotos.size} heavy photos (3 MB+)",
                    color = OnDark,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GhostButton(
                        text = if (showPhotos) "Hide" else "Show list",
                        modifier = Modifier.weight(1f),
                        accent = accentA
                    ) { showPhotos = !showPhotos }
                }
                AnimatedVisibility(visible = showPhotos) {
                    FileList(bigPhotos.take(12), accentA) { f ->
                        scope.launch {
                            val msg = withContext(Dispatchers.IO) { Tools.deleteFiles(listOf(f)) }
                            Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()
                            rescan()
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        PopIn(delayMillis = 240) {
            NeonCard(Modifier.fillMaxWidth(), accent = accentB) {
                SectionLabel("App uninstaller", accentB)
                Text(
                    if (apps.isEmpty()) "Tap to load your apps" else "${apps.size} installed apps",
                    color = OnDark,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(10.dp))
                GhostButton(
                    text = if (showApps) "Hide list" else "Show list",
                    modifier = Modifier.fillMaxWidth(),
                    accent = accentB
                ) {
                    showApps = !showApps
                    if (showApps && apps.isEmpty()) {
                        scope.launch {
                            apps = withContext(Dispatchers.IO) { Tools.installedApps(ctx) }
                        }
                    }
                }
                AnimatedVisibility(visible = showApps) {
                    Column(Modifier.padding(top = 8.dp)) {
                        apps.take(60).forEach { a ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { Tools.uninstall(ctx, a.pkg) }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    a.label,
                                    color = OnDark,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    "UNINSTALL",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        PopIn(delayMillis = 280) {
            NeonCard(Modifier.fillMaxWidth(), accent = accentA) {
                SectionLabel("Tip", accentA)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Warning, contentDescription = null, tint = accentA, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Cleaning only removes junk, trashed files and what you pick. Your downloads stay safe.",
                        color = Muted,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun FileList(files: List<File>, accent: Color, onDelete: (File) -> Unit) {
    Column(Modifier.padding(top = 10.dp)) {
        files.forEach { f ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        f.name,
                        color = OnDark,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        Tools.human(f.length()),
                        color = accent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Box(
                    Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(Surface2.copy(alpha = 0.6f))
                        .border(1.dp, Hairline, CircleShape)
                        .clickable { onDelete(f) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.DeleteSweep,
                        contentDescription = "Delete",
                        tint = accent,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
