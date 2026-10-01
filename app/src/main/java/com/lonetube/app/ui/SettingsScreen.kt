package com.lonetube.app.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteSweep
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lonetube.app.data.History
import com.lonetube.app.data.Prefs
import com.lonetube.app.engine.Engine
import com.lonetube.app.ui.components.GlassCard
import com.lonetube.app.ui.components.GradientButton
import com.lonetube.app.ui.components.SoftButton
import com.lonetube.app.ui.theme.Cyan
import com.lonetube.app.ui.theme.Hairline
import com.lonetube.app.ui.theme.Muted
import com.lonetube.app.ui.theme.OnDark
import com.lonetube.app.ui.theme.Surface2
import com.lonetube.app.ui.theme.Violet
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SettingsScreen() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var defaultQ by remember { mutableStateOf(Prefs.defaultQuality(ctx)) }
    var askQ by remember { mutableStateOf(Prefs.askQuality(ctx)) }
    var updating by remember { mutableStateOf(false) }
    var updateMsg by remember { mutableStateOf<String?>(null) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp)
    ) {
        Spacer(Modifier.height(18.dp))
        Text("Settings", style = MaterialTheme.typography.headlineMedium, color = OnDark)
        Spacer(Modifier.height(16.dp))

        GlassCard(Modifier.fillMaxWidth()) {
            Text("Default quality", style = MaterialTheme.typography.titleMedium, color = OnDark)
            Text(
                "Used when ask-me-every-time is off",
                style = MaterialTheme.typography.bodySmall,
                color = Muted
            )
            Spacer(Modifier.height(12.dp))
            Engine.QUALITIES.chunked(2).forEach { pair ->
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    pair.forEach { q ->
                        QualityChoiceChip(
                            text = q.title,
                            selected = q.index == defaultQ,
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
        }

        Spacer(Modifier.height(12.dp))

        GlassCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Ask me every time", style = MaterialTheme.typography.titleMedium, color = OnDark)
                    Text(
                        "Show the quality picker before each download",
                        style = MaterialTheme.typography.bodySmall,
                        color = Muted
                    )
                }
                Switch(
                    checked = askQ,
                    onCheckedChange = {
                        askQ = it
                        Prefs.setAskQuality(ctx, it)
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Violet
                    )
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        GlassCard(Modifier.fillMaxWidth()) {
            Text("Engine", style = MaterialTheme.typography.titleMedium, color = OnDark)
            Text(
                "yt-dlp refreshes itself daily. Tap to force an update if a site stops working.",
                style = MaterialTheme.typography.bodySmall,
                color = Muted
            )
            Spacer(Modifier.height(12.dp))
            GradientButton(
                text = if (updating) "Updating..." else "Update engine now",
                modifier = Modifier.fillMaxWidth(),
                icon = Icons.Rounded.SystemUpdateAlt,
                enabled = !updating
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
                Text(it, color = Cyan, style = MaterialTheme.typography.bodySmall)
            }
        }

        Spacer(Modifier.height(12.dp))

        GlassCard(Modifier.fillMaxWidth()) {
            Text("Library", style = MaterialTheme.typography.titleMedium, color = OnDark)
            Text(
                "Clear the finished-downloads list (your files stay in Downloads)",
                style = MaterialTheme.typography.bodySmall,
                color = Muted
            )
            Spacer(Modifier.height(12.dp))
            SoftButton(
                text = "Clear library list",
                modifier = Modifier.fillMaxWidth(),
                icon = Icons.Rounded.DeleteSweep
            ) { History.clear(ctx) }
        }

        Spacer(Modifier.height(12.dp))

        GlassCard(Modifier.fillMaxWidth()) {
            Text("About LoneTube", style = MaterialTheme.typography.titleMedium, color = OnDark)
            Text(
                "v1.0 - Powered by yt-dlp, ffmpeg and aria2c.\nFor personal use of your own content only.",
                style = MaterialTheme.typography.bodySmall,
                color = Muted
            )
        }

        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun QualityChoiceChip(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 800f),
        label = "qc"
    )
    Row(
        modifier
            .scale(scale)
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) Violet.copy(alpha = 0.25f) else Surface2.copy(alpha = 0.5f))
            .border(
                1.dp,
                if (selected) Cyan else Hairline,
                RoundedCornerShape(14.dp)
            )
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text,
            color = if (selected) OnDark else Muted,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}
