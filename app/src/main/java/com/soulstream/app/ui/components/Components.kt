package com.soulstream.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.soulstream.app.ui.theme.BgBottom
import com.soulstream.app.ui.theme.BgTop
import com.soulstream.app.ui.theme.Hairline
import com.soulstream.app.ui.theme.Muted
import com.soulstream.app.ui.theme.OnDark
import com.soulstream.app.ui.theme.Surface1
import com.soulstream.app.ui.theme.Surface2

/**
 * The gamer backdrop: deep gradient, drifting neon blooms, a slow perspective
 * grid and a scanline sweep.
 */
@Composable
fun NeonBackground(
    modifier: Modifier = Modifier,
    accentA: Color,
    accentB: Color,
    content: @Composable BoxScope.() -> Unit
) {
    val t = rememberInfiniteTransition(label = "bg")
    val shift by t.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(11000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shift"
    )
    val grid by t.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "grid"
    )
    val scan by t.animateFloat(
        initialValue = -0.2f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(5200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scan"
    )

    Box(modifier.background(Brush.verticalGradient(listOf(BgTop, BgBottom)))) {
        Canvas(Modifier.fillMaxSize()) {
            // neon blooms
            val r1 = size.minDimension * 0.85f
            val c1 = Offset(size.width * (0.15f + shift * 0.5f), size.height * (0.08f + shift * 0.12f))
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(accentA.copy(alpha = 0.30f), Color.Transparent),
                    center = c1,
                    radius = r1
                ),
                radius = r1,
                center = c1
            )
            val r2 = size.minDimension * 0.65f
            val c2 = Offset(size.width * (0.95f - shift * 0.35f), size.height * (0.9f - shift * 0.1f))
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(accentB.copy(alpha = 0.22f), Color.Transparent),
                    center = c2,
                    radius = r2
                ),
                radius = r2,
                center = c2
            )

            // perspective grid
            val step = 64f
            val offset = (grid * step)
            var x = -step + offset
            while (x < size.width + step) {
                drawLine(
                    color = accentA.copy(alpha = 0.05f),
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = 1.5f
                )
                x += step
            }
            var y = -step + offset
            while (y < size.height + step) {
                drawLine(
                    color = accentA.copy(alpha = 0.05f),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1.5f
                )
                y += step
            }

            // scanline sweep
            val sy = size.height * scan
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        accentB.copy(alpha = 0.10f),
                        Color.Transparent
                    ),
                    startY = sy - 90f,
                    endY = sy + 90f
                ),
                topLeft = Offset(0f, sy - 90f),
                size = Size(size.width, 180f)
            )
        }
        content()
    }
}

/** Angular neon card with a gradient hairline and a soft inner glow. */
@Composable
fun NeonCard(
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.primary,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        Surface1.copy(alpha = 0.94f),
                        Surface2.copy(alpha = 0.82f)
                    )
                )
            )
            .border(
                1.dp,
                Brush.verticalGradient(
                    listOf(accent.copy(alpha = 0.55f), Hairline)
                ),
                RoundedCornerShape(20.dp)
            )
            .padding(16.dp),
        content = content
    )
}

/** Primary action: gradient fill, press feedback, and a light sweep. */
@Composable
fun GlowButton(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    accentA: Color = MaterialTheme.colorScheme.primary,
    accentB: Color = MaterialTheme.colorScheme.secondary,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.955f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 750f),
        label = "press"
    )
    val sweep = rememberInfiniteTransition(label = "sweep")
    val sweepX by sweep.animateFloat(
        initialValue = -0.4f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweepX"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (enabled) Brush.horizontalGradient(listOf(accentA, accentB))
                else Brush.horizontalGradient(listOf(Surface2, Surface2))
            )
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        // light sweep across the button
        if (enabled) {
            Canvas(Modifier.fillMaxSize()) {
                val w = size.width
                val x = w * sweepX
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = 0.22f),
                            Color.Transparent
                        ),
                        startX = x - w * 0.18f,
                        endX = x + w * 0.18f
                    )
                )
            }
        }
        Row(
            Modifier.padding(horizontal = 20.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (enabled) Color(0xFF03060E) else Muted,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = text,
                color = if (enabled) Color(0xFF03060E) else Muted,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

/** Secondary action: dark glass with a neon rim. */
@Composable
fun GhostButton(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    accent: Color = MaterialTheme.colorScheme.primary,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 750f),
        label = "ghost"
    )
    Box(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .background(Surface2.copy(alpha = 0.6f))
            .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(7.dp))
            }
            Text(text, color = OnDark, style = MaterialTheme.typography.titleMedium)
        }
    }
}

/** Breathing status dot. */
@Composable
fun PulseDot(color: Color, size: Dp = 9.dp) {
    val t = rememberInfiniteTransition(label = "pulse")
    val s by t.animateFloat(
        initialValue = 0.7f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(850, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "s"
    )
    val a by t.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(850, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "a"
    )
    Box(
        Modifier
            .size(size)
            .scale(s)
            .clip(CircleShape)
            .background(color.copy(alpha = a))
    )
}

/** Neon progress ring with a glow halo. */
@Composable
fun NeonRing(
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 46.dp,
    strokeWidth: Dp = 5.dp,
    accentA: Color = MaterialTheme.colorScheme.primary,
    accentB: Color = MaterialTheme.colorScheme.secondary
) {
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(420),
        label = "ring"
    )
    val glow = rememberInfiniteTransition(label = "glow")
    val glowAlpha by glow.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowA"
    )
    Canvas(modifier.size(size)) {
        val sw = strokeWidth.toPx()
        val d = size.toPx() - sw * 2
        val topLeft = Offset(sw, sw)
        val arcSize = Size(d, d)
        // halo
        drawArc(
            brush = Brush.sweepGradient(listOf(accentA, accentB)),
            startAngle = -90f,
            sweepAngle = 360f * animated,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = sw * 2.4f, cap = StrokeCap.Round),
            alpha = glowAlpha * 0.35f
        )
        // track
        drawArc(
            color = Surface2,
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = sw, cap = StrokeCap.Round)
        )
        // progress
        drawArc(
            brush = Brush.sweepGradient(listOf(accentA, accentB)),
            startAngle = -90f,
            sweepAngle = 360f * animated,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = sw, cap = StrokeCap.Round)
        )
    }
}

/** Animated number that counts up/down smoothly. */
@Composable
fun AnimatedCounter(
    value: Int,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    suffix: String = "%"
) {
    val animated by animateIntAsState(
        targetValue = value,
        animationSpec = tween(500),
        label = "counter"
    )
    Text(
        text = "$animated$suffix",
        color = color,
        style = MaterialTheme.typography.labelSmall,
        modifier = modifier
    )
}

@Composable
fun SectionLabel(text: String, accent: Color = MaterialTheme.colorScheme.primary, modifier: Modifier = Modifier) {
    Row(
        modifier.padding(start = 2.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(3.dp, 12.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(accent)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = Muted
        )
    }
}

/** Wrap anything in a springy pop-in entrance. */
@Composable
fun PopIn(
    modifier: Modifier = Modifier,
    delayMillis: Int = 0,
    content: @Composable () -> Unit
) {
    val a = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        a.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 420, delayMillis = delayMillis, easing = FastOutSlowInEasing)
        )
    }
    Box(modifier.scale(0.92f + 0.08f * a.value)) {
        content()
    }
}
