package com.soulstream.app.ui

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
import com.soulstream.app.data.History
import com.soulstream.app.data.Prefs
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
import com.soulstream.app.ui.theme.OnDark
import com.soulstream.app.ui.theme.Surface2
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SettingsScreen(accentIndex: Int, onAccentChange: (Int) -> Unit) {
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

        PopIn(delayMillis = 40) {
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

        PopIn(delayMillis = 80) {
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

        PopIn(delayMillis = 120) {
            NeonCard(Modifier.fillMaxWidth(), accent = accentA) {
                SectionLabel("Engine", accentA)
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
                                "Engine updated"
                            } catch (e: Exception) {
                                "Update failed: ${e.message}"
                            }
                        }
                        updateMsg = res
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

        PopIn(delayMillis = 160) {
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

        PopIn(delayMillis = 200) {
            NeonCard(Modifier.fillMaxWidth(), accent = accentB) {
                SectionLabel("About", accentB)
                Text("SoulStream v2.0", style = MaterialTheme.typography.titleMedium, color = OnDark)
                Spacer(Modifier.height(4.dp))
                Text(
                    "yt-dlp + ffmpeg + aria2c engine. Personal use of your own content only.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Muted
                )
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
