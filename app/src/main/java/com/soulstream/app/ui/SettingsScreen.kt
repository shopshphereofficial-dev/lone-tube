package com.soulstream.app.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.SystemUpdateAlt
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.soulstream.app.SoulStreamApp
import com.soulstream.app.data.History
import com.soulstream.app.data.Prefs
import com.soulstream.app.engine.AppInfo
import com.soulstream.app.engine.Diag
import com.soulstream.app.engine.Engine
import com.soulstream.app.engine.Session
import com.soulstream.app.ui.components.GhostButton
import com.soulstream.app.ui.components.GlowButton
import com.soulstream.app.ui.components.NeonCard
import com.soulstream.app.ui.components.PopIn
import com.soulstream.app.ui.components.SectionLabel
import com.soulstream.app.ui.theme.Accents
import com.soulstream.app.ui.theme.Hairline
import com.soulstream.app.ui.theme.Muted
import com.soulstream.app.ui.theme.NeonLime
import com.soulstream.app.ui.theme.NeonRed
import com.soulstream.app.ui.theme.OnDark
import com.soulstream.app.ui.theme.Surface2
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SettingsScreen(
    accentIndex: Int,
    onAccentChange: (Int) -> Unit,
    onOpenVault: () -> Unit,
    onOpenTools: () -> Unit
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val accentA = MaterialTheme.colorScheme.primary
    val accentB = MaterialTheme.colorScheme.secondary

    var defaultQ by remember { mutableStateOf(Prefs.defaultQuality(ctx)) }
    var askQ by remember { mutableStateOf(Prefs.askQuality(ctx)) }
    var turbo by remember { mutableStateOf(Prefs.turbo(ctx)) }
    var adBlock by remember { mutableStateOf(Prefs.adBlock(ctx)) }
    var keepLogin by remember { mutableStateOf(Prefs.keepLogin(ctx)) }
    var autoClip by remember { mutableStateOf(Prefs.autoClipboard(ctx)) }
    var updating by remember { mutableStateOf(false) }
    var updateMsg by remember { mutableStateOf<String?>(null) }
    var engineReady by remember { mutableStateOf(SoulStreamApp.engineReady) }
    var engineErr by remember { mutableStateOf(SoulStreamApp.engineError) }
    var dlErr by remember { mutableStateOf(Diag.lastDownloadError(ctx)) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp)
    ) {
        Spacer(Modifier.height(16.dp))
        Text(
            "CONTROL ROOM",
            style = MaterialTheme.typography.headlineMedium.copy(
                brush = Brush.horizontalGradient(listOf(accentA, accentB))
            )
        )
        Spacer(Modifier.height(14.dp))

        PopIn {
            NeonCard(Modifier.fillMaxWidth(), accent = if (engineReady) accentA else NeonRed) {
                SectionLabel("Engine health", if (engineReady) accentA else NeonRed)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (engineReady) "ENGINE READY" else "ENGINE NOT READY",
                        color = if (engineReady) NeonLime else NeonRed,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    SoulStreamApp.engineNote
                        ?: if (engineReady) "yt-dlp + ffmpeg + aria2c loaded"
                        else "Downloads cannot run until this is fixed",
                    color = Muted,
                    style = MaterialTheme.typography.bodySmall
                )
                if (engineErr != null) {
                    Spacer(Modifier.height(6.dp))
                    Text("error: $engineErr", color = NeonRed, style = MaterialTheme.typography.bodySmall)
                }
                if (dlErr != null) {
                    Spacer(Modifier.height(6.dp))
                    Text("last download error:", color = Muted, style = MaterialTheme.typography.bodySmall)
                    Text(dlErr ?: "", color = NeonRed, style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GlowButton(
                        text = if (updating) "FIXING..." else "REPAIR",
                        modifier = Modifier.weight(1f),
                        icon = Icons.Rounded.SystemUpdateAlt,
                        enabled = !updating,
                        accentA = accentA,
                        accentB = accentB
                    ) {
                        updating = true
                        updateMsg = null
                        scope.launch {
                            val res = withContext(Dispatchers.IO) {
                                try {
                                    SoulStreamApp.initEngine(ctx, force = true)
                                    if (SoulStreamApp.engineReady) {
                                        "Engine ready" + (SoulStreamApp.engineNote?.let { " - $it" } ?: "")
                                    } else {
                                        "Failed: " + (SoulStreamApp.engineError ?: "unknown")
                                    }
                                } catch (e: Throwable) {
                                    "Failed: ${e.message}"
                                }
                            }
                            engineReady = SoulStreamApp.engineReady
                            engineErr = SoulStreamApp.engineError
                            updateMsg = res
                            updating = false
                        }
                    }
                    GhostButton(
                        text = "Refresh",
                        modifier = Modifier.weight(1f),
                        icon = Icons.Rounded.Refresh,
                        accent = accentB
                    ) {
                        engineReady = SoulStreamApp.engineReady
                        engineErr = SoulStreamApp.engineError
                        dlErr = Diag.lastDownloadError(ctx)
                    }
                }
                updateMsg?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = accentA, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        PopIn(delayMillis = 30) {
            NeonCard(Modifier.fillMaxWidth(), accent = accentA) {
                SectionLabel("Download tools", accentA)
                GhostButton(
                    text = "WhatsApp status saver",
                    modifier = Modifier.fillMaxWidth(),
                    icon = Icons.Rounded.Refresh,
                    accent = accentA
                ) { onOpenVault() }
                Spacer(Modifier.height(8.dp))
                GhostButton(
                    text = "Phone cleaner (junk, large files, WhatsApp)",
                    modifier = Modifier.fillMaxWidth(),
                    icon = Icons.Rounded.DeleteSweep,
                    accent = accentB
                ) { onOpenTools() }
            }
        }

        Spacer(Modifier.height(12.dp))

        PopIn(delayMillis = 60) {
            NeonCard(Modifier.fillMaxWidth(), accent = accentB) {
                SectionLabel("Diagnostics report", accentB)
                Text(
                    "If something fails, copy this and send it - it lists the exact reason.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Muted
                )
                Spacer(Modifier.height(10.dp))
                GlowButton(
                    text = "COPY REPORT",
                    modifier = Modifier.fillMaxWidth(),
                    icon = Icons.Rounded.SystemUpdateAlt,
                    accentA = accentA,
                    accentB = accentB
                ) {
                    val txt = Diag.snapshot(ctx)
                    try {
                        val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cm.setPrimaryClip(ClipData.newPlainText("SoulStream diagnostics", txt))
                        Toast.makeText(ctx, "Report copied - paste it in the chat", Toast.LENGTH_LONG).show()
                    } catch (e: Throwable) {
                        Toast.makeText(ctx, "Could not copy", Toast.LENGTH_SHORT).show()
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GhostButton(
                        text = "Share",
                        modifier = Modifier.weight(1f),
                        accent = accentA
                    ) {
                        try {
                            val i = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "SoulStream diagnostics")
                                putExtra(Intent.EXTRA_TEXT, Diag.snapshot(ctx))
                            }
                            ctx.startActivity(Intent.createChooser(i, "Send diagnostics"))
                        } catch (e: Throwable) {
                            Toast.makeText(ctx, "Could not share", Toast.LENGTH_SHORT).show()
                        }
                    }
                    GhostButton(
                        text = "Clear log",
                        modifier = Modifier.weight(1f),
                        icon = Icons.Rounded.DeleteSweep,
                        accent = accentB
                    ) {
                        Diag.clearAll(ctx)
                        dlErr = null
                        Toast.makeText(ctx, "Diagnostics cleared", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        PopIn(delayMillis = 60) {
            NeonCard(Modifier.fillMaxWidth(), accent = accentA) {
                SectionLabel("Neon skin", accentA)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Accents.ALL.forEachIndexed { i, palette ->
                        AccentSwatch(
                            selected = i == accentIndex,
                            a = palette.primary,
                            b = palette.secondary
                        ) { onAccentChange(i) }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    Accents.get(accentIndex).name,
                    color = Muted,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        PopIn(delayMillis = 90) {
            NeonCard(Modifier.fillMaxWidth(), accent = accentA) {
                SectionLabel("Download power", accentA)
                Text("Default quality", style = MaterialTheme.typography.titleMedium, color = OnDark)
                Spacer(Modifier.height(8.dp))
                Engine.QUALITIES.chunked(2).forEach { pair ->
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        pair.forEach { q ->
                            QualityChip(
                                text = q.title,
                                selected = q.index == defaultQ,
                                accent = accentA,
                                modifier = Modifier.weight(1f)
                            ) {
                                defaultQ = q.index
                                Prefs.setDefaultQuality(ctx, q.index)
                            }
                        }
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(8.dp))
                }
                Spacer(Modifier.height(4.dp))
                ToggleRow(
                    title = "Ask me every time",
                    subtitle = "Show the power-up picker before each download",
                    checked = askQ,
                    accent = accentA
                ) {
                    askQ = it
                    Prefs.setAskQuality(ctx, it)
                }
                ToggleRow(
                    title = "Turbo downloads",
                    subtitle = "aria2c engine, 16 parallel connections",
                    checked = turbo,
                    accent = accentB
                ) {
                    turbo = it
                    Prefs.setTurbo(ctx, it)
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        PopIn(delayMillis = 120) {
            NeonCard(Modifier.fillMaxWidth(), accent = accentB) {
                SectionLabel("Browser", accentB)
                ToggleRow(
                    title = "Block ads",
                    subtitle = "Kills ad networks and hides ad slots while browsing",
                    checked = adBlock,
                    accent = accentB
                ) {
                    adBlock = it
                    Prefs.setAdBlock(ctx, it)
                }
                ToggleRow(
                    title = "Stay logged in",
                    subtitle = "Google, Instagram, Facebook and other logins survive restarts",
                    checked = keepLogin,
                    accent = accentB
                ) {
                    keepLogin = it
                    Prefs.setKeepLogin(ctx, it)
                }
                ToggleRow(
                    title = "Detect clipboard links",
                    subtitle = "Offer the link you just copied on the home screen",
                    checked = autoClip,
                    accent = accentB
                ) {
                    autoClip = it
                    Prefs.setAutoClipboard(ctx, it)
                }
                Spacer(Modifier.height(6.dp))
                GhostButton(
                    text = "Reset browser (clear logins + cache)",
                    modifier = Modifier.fillMaxWidth(),
                    icon = Icons.Rounded.Refresh,
                    accent = accentB
                ) {
                    Engine.resetBrowserData(ctx)
                    Toast.makeText(ctx, "Browser data cleared - reopen the browser", Toast.LENGTH_LONG).show()
                }
                Spacer(Modifier.height(8.dp))
                GhostButton(
                    text = "Log out everywhere",
                    modifier = Modifier.fillMaxWidth(),
                    icon = Icons.Rounded.Logout,
                    accent = MaterialTheme.colorScheme.error
                ) {
                    Session.logoutEverywhere()
                    Toast.makeText(ctx, "All logins cleared", Toast.LENGTH_SHORT).show()
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        PopIn(delayMillis = 150) {
            NeonCard(Modifier.fillMaxWidth(), accent = accentA) {
                SectionLabel("Engine update", accentA)
                Text(
                    "yt-dlp refreshes itself daily. Force an update if a site stops working.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Muted
                )
                Spacer(Modifier.height(10.dp))
                GlowButton(
                    text = if (updating) "UPDATING..." else "UPDATE ENGINE",
                    modifier = Modifier.fillMaxWidth(),
                    icon = Icons.Rounded.SystemUpdateAlt,
                    enabled = !updating,
                    accentA = accentA,
                    accentB = accentB
                ) {
                    updating = true
                    updateMsg = null
                    scope.launch {
                        val res = withContext(Dispatchers.IO) {
                            try {
                                YoutubeDL.getInstance()
                                    .updateYoutubeDL(ctx, YoutubeDL.UpdateChannel.STABLE)
                                Prefs.setEngineUpdatedAt(ctx, System.currentTimeMillis())
                                "Engine updated"
                            } catch (e: Throwable) {
                                "Update failed: ${e.message}"
                            }
                        }
                        updateMsg = res
                        engineReady = SoulStreamApp.engineReady
                        updating = false
                    }
                }
                updateMsg?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = accentA, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        PopIn(delayMillis = 180) {
            NeonCard(Modifier.fillMaxWidth(), accent = accentA) {
                SectionLabel("Storage", accentA)
                Text(
                    "Clears the finished list only - your files stay in Downloads",
                    style = MaterialTheme.typography.bodySmall,
                    color = Muted
                )
                Spacer(Modifier.height(10.dp))
                GhostButton(
                    text = "Clear library list",
                    modifier = Modifier.fillMaxWidth(),
                    icon = Icons.Rounded.DeleteSweep,
                    accent = accentA
                ) {
                    History.clear(ctx)
                    Toast.makeText(ctx, "Library list cleared", Toast.LENGTH_SHORT).show()
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        PopIn(delayMillis = 210) {
            NeonCard(Modifier.fillMaxWidth(), accent = accentB) {
                SectionLabel("About", accentB)
                Text(
                    "SoulStream v" + AppInfo.VERSION,
                    style = MaterialTheme.typography.titleMedium,
                    color = OnDark
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "yt-dlp + ffmpeg + aria2c engine. Personal use of your own content only.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Muted
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        PopIn(delayMillis = 240) {
            NeonCard(Modifier.fillMaxWidth(), accent = accentA) {
                SectionLabel("More", accentA)
                GhostButton(
                    text = "Share SoulStream",
                    modifier = Modifier.fillMaxWidth(),
                    icon = Icons.Rounded.Share,
                    accent = accentA
                ) {
                    try {
                        val i = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "SoulStream - media downloader: " +
                                    "https://github.com/shopshphereofficial-dev/soulstream"
                            )
                        }
                        ctx.startActivity(Intent.createChooser(i, "Share SoulStream"))
                    } catch (e: Throwable) {
                        Toast.makeText(ctx, "Could not share", Toast.LENGTH_SHORT).show()
                    }
                }
                Spacer(Modifier.height(8.dp))
                GhostButton(
                    text = "Send feedback",
                    modifier = Modifier.fillMaxWidth(),
                    icon = Icons.Rounded.Share,
                    accent = accentB
                ) {
                    try {
                        val i = Intent(Intent.ACTION_SENDTO).apply {
                            data = android.net.Uri.parse("mailto:")
                            putExtra(Intent.EXTRA_SUBJECT, "SoulStream feedback")
                            putExtra(Intent.EXTRA_TEXT, Diag.snapshot(ctx))
                        }
                        ctx.startActivity(i)
                    } catch (e: Throwable) {
                        Toast.makeText(ctx, "No mail app found", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    accent: Color,
    onChange: (Boolean) -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = OnDark)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Muted)
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF03060E),
                checkedTrackColor = accent
            )
        )
    }
}

@Composable
private fun AccentSwatch(
    selected: Boolean,
    a: Color,
    b: Color,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.12f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 700f),
        label = "swatch"
    )
    Box(
        Modifier
            .size(46.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(a, b)))
            .border(
                if (selected) 2.dp else 1.dp,
                if (selected) Color.White.copy(alpha = 0.85f) else Hairline,
                CircleShape
            )
            .clickable(onClick = onClick)
    )
}

@Composable
private fun QualityChip(
    text: String,
    selected: Boolean,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 800f),
        label = "qc"
    )
    Box(
        modifier
            .scale(scale)
            .clip(RoundedCornerShape(13.dp))
            .background(if (selected) accent.copy(alpha = 0.22f) else Surface2.copy(alpha = 0.5f))
            .border(1.dp, if (selected) accent else Hairline, RoundedCornerShape(13.dp))
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 11.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            color = if (selected) OnDark else Muted,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}
