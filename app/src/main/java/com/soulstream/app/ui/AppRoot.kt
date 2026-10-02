package com.soulstream.app.ui

import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
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
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Warning
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.soulstream.app.data.Prefs
import com.soulstream.app.ui.components.NeonBackground
import com.soulstream.app.ui.components.PopIn
import com.soulstream.app.ui.theme.Muted
import com.soulstream.app.ui.theme.OnDark
import com.soulstream.app.ui.theme.Surface1

private data class NavTab(val label: String, val icon: ImageVector)

private val TABS = listOf(
    NavTab("Home", Icons.Rounded.Home),
    NavTab("Vault", Icons.Rounded.Star),
    NavTab("Library", Icons.Rounded.Folder),
    NavTab("Play", Icons.Rounded.PlayArrow),
    NavTab("Settings", Icons.Rounded.Settings)
)

@Composable
fun AppRoot(
    accentIndex: Int,
    onAccentChange: (Int) -> Unit,
    onOpenBrowser: (String) -> Unit,
    onStartDownload: (String, Int) -> Unit
) {
    val ctx = LocalContext.current
    var tab by remember { mutableStateOf(0) }
    var crash by remember { mutableStateOf(Prefs.lastCrash(ctx)) }

    val accentA = MaterialTheme.colorScheme.primary
    val accentB = MaterialTheme.colorScheme.secondary

    NeonBackground(
        modifier = Modifier.fillMaxSize(),
        accentA = accentA,
        accentB = accentB
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .systemBarsPadding()
        ) {
            crash?.let { msg ->
                CrashBanner(msg, accentA) {
                    Prefs.clearLastCrash(ctx)
                    crash = null
                }
            }

            AnimatedContent(
                targetState = tab,
                transitionSpec = {
                    (fadeIn(tween(300)) + slideInVertically(tween(360)) { it / 14 } + scaleIn(
                        initialScale = 0.97f,
                        animationSpec = tween(320, easing = FastOutSlowInEasing)
                    )) togetherWith fadeOut(tween(140))
                },
                label = "tab",
                modifier = Modifier.weight(1f)
            ) { t ->
                when (t) {
                    0 -> HomeScreen(onOpenBrowser = onOpenBrowser, onStartDownload = onStartDownload)
                    1 -> StatusScreen()
                    2 -> LibraryScreen()
                    3 -> PlayerScreen()
                    else -> SettingsScreen(accentIndex = accentIndex, onAccentChange = onAccentChange)
                }
            }

            BottomBar(current = tab, accentA = accentA, accentB = accentB) { tab = it }
        }
    }
}

@Composable
private fun CrashBanner(msg: String, accent: Color, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    PopIn {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(accent.copy(alpha = 0.12f))
                .border(1.dp, accent.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.Warning, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("Last session hiccup", color = OnDark, style = MaterialTheme.typography.bodySmall)
                Text(
                    msg,
                    color = Muted,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = {
                try {
                    val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    cm.setPrimaryClip(android.content.ClipData.newPlainText("SoulStream error", msg))
                    Toast.makeText(ctx, "Copied", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    // ignore
                }
            }) {
                Text("copy", color = accent, style = MaterialTheme.typography.labelSmall)
            }
            IconButton(onClick = onDismiss) {
                Icon(Icons.Rounded.Close, contentDescription = "Dismiss", tint = Muted, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun BottomBar(
    current: Int,
    accentA: Color,
    accentB: Color,
    onSelect: (Int) -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Surface1.copy(alpha = 0.95f))
            .border(1.dp, accentA.copy(alpha = 0.18f), RoundedCornerShape(0.dp))
            .padding(horizontal = 4.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TABS.forEachIndexed { index, t ->
            BarItem(
                label = t.label,
                icon = t.icon,
                selected = index == current,
                accentA = accentA,
                accentB = accentB
            ) { onSelect(index) }
        }
    }
}

@Composable
private fun BarItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    accentA: Color,
    accentB: Color,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.1f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 620f),
        label = "bar"
    )
    val t = rememberInfiniteTransition(label = "barGlow")
    val glow by t.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "barGlowA"
    )

    Column(
        Modifier
            .scale(scale)
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (selected) accentA.copy(alpha = 0.16f) else Color.Transparent
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (selected) {
                Box(
                    Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(accentA.copy(alpha = glow * 0.35f), Color.Transparent)
                            )
                        )
                )
            }
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (selected) accentA else Muted,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(Modifier.height(2.dp))
        Text(
            label,
            color = if (selected) OnDark else Muted,
            style = MaterialTheme.typography.labelSmall
        )
    }
}
