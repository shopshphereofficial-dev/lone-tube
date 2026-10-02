package com.lonetube.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.lonetube.app.ui.components.AnimatedGradientBackground
import com.lonetube.app.ui.theme.Cyan
import com.lonetube.app.ui.theme.Muted
import com.lonetube.app.ui.theme.OnDark
import com.lonetube.app.ui.theme.Surface1
import com.lonetube.app.ui.theme.Violet

private data class NavTab(val label: String, val icon: ImageVector)

private val TABS = listOf(
    NavTab("Home", Icons.Rounded.Home),
    NavTab("Statuses", Icons.Rounded.Star),
    NavTab("Library", Icons.Rounded.Folder),
    NavTab("Settings", Icons.Rounded.Settings)
)

@Composable
fun AppRoot(
    onOpenBrowser: (String) -> Unit,
    onStartDownload: (String, Int) -> Unit
) {
    var tab by remember { mutableStateOf(0) }

    AnimatedGradientBackground(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .systemBarsPadding()
        ) {
            AnimatedContent(
                targetState = tab,
                transitionSpec = {
                    (fadeIn(tween(320)) + slideInVertically(tween(380)) { it / 16 }) togetherWith
                        fadeOut(tween(150))
                },
                label = "tab",
                modifier = Modifier.weight(1f)
            ) { t ->
                when (t) {
                    0 -> HomeScreen(onOpenBrowser = onOpenBrowser, onStartDownload = onStartDownload)
                    1 -> StatusScreen()
                    2 -> LibraryScreen()
                    else -> SettingsScreen()
                }
            }
            BottomBar(current = tab) { tab = it }
        }
    }
}

@Composable
private fun BottomBar(current: Int, onSelect: (Int) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Surface1.copy(alpha = 0.92f))
            .padding(horizontal = 6.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TABS.forEachIndexed { index, t ->
            BarItem(
                label = t.label,
                icon = t.icon,
                selected = index == current
            ) { onSelect(index) }
        }
    }
}

@Composable
private fun BarItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.08f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 600f),
        label = "bar"
    )
    Column(
        Modifier
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) Violet.copy(alpha = 0.18f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) Cyan else Muted,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.height(3.dp))
        Text(
            label,
            color = if (selected) OnDark else Muted,
            style = MaterialTheme.typography.labelSmall
        )
    }
}
