package com.lonetube.app.ui.components

import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import com.lonetube.app.engine.Engine
import com.lonetube.app.engine.QualityOption
import com.lonetube.app.ui.theme.Cyan
import com.lonetube.app.ui.theme.Muted
import com.lonetube.app.ui.theme.OnDark
import com.lonetube.app.ui.theme.Pink
import com.lonetube.app.ui.theme.Surface1
import com.lonetube.app.ui.theme.Surface2
import com.lonetube.app.ui.theme.Violet

/** The quality picker, shared by the home screen and the share dialog. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QualitySheet(
    onDismiss: () -> Unit,
    onPick: (Int) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Surface1,
        dragHandle = { BottomSheetDefaults.DragHandle(color = Muted) }
    ) {
        Column(
            Modifier
                .padding(horizontal = 16.dp)
                .padding(bottom = 28.dp)
        ) {
            Text("Choose quality", style = MaterialTheme.typography.titleLarge, color = OnDark)
            Spacer(Modifier.height(4.dp))
            Text(
                "Video keeps full audio - MP3 saves sound only",
                style = MaterialTheme.typography.bodySmall,
                color = Muted
            )
            Spacer(Modifier.height(14.dp))
            Engine.QUALITIES.forEach { q ->
                QualityRow(q) { onPick(q.index) }
            }
        }
    }
}

@Composable
fun QualityRow(q: QualityOption, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 800f),
        label = "q"
    )
    Row(
        Modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .background(Surface2.copy(alpha = 0.6f))
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(if (q.isAudio) Pink.copy(alpha = 0.18f) else Violet.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (q.isAudio) Icons.Rounded.MusicNote else Icons.Rounded.Movie,
                contentDescription = null,
                tint = if (q.isAudio) Pink else Violet,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(q.title, color = OnDark, style = MaterialTheme.typography.titleMedium)
            Text(q.subtitle, color = Muted, style = MaterialTheme.typography.bodySmall)
        }
        Icon(
            Icons.Rounded.Download,
            contentDescription = null,
            tint = Cyan,
            modifier = Modifier.size(20.dp)
        )
    }
    Spacer(Modifier.height(8.dp))
}
