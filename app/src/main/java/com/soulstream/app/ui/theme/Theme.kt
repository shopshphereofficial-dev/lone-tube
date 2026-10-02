package com.soulstream.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ---- base: deep-space black with a violet bloom ----
val BgTop = Color(0xFF04060F)
val BgBottom = Color(0xFF0B0620)
val Surface1 = Color(0xFF0E1330)
val Surface2 = Color(0xFF151B3D)
val OnDark = Color(0xFFEAF6FF)
val Muted = Color(0xFF8A93C4)
val Hairline = Color(0x2EFFFFFF)

// ---- neon accents ----
val NeonCyan = Color(0xFF22D3EE)
val NeonViolet = Color(0xFFA855F7)
val NeonPink = Color(0xFFF472B6)
val NeonLime = Color(0xFFA3E635)
val NeonAmber = Color(0xFFFBBF24)
val NeonRed = Color(0xFFFF4D6D)

data class AccentPalette(
    val name: String,
    val primary: Color,
    val secondary: Color,
    val tertiary: Color
)

object Accents {
    val ALL = listOf(
        AccentPalette("Neon Cyan", NeonCyan, NeonViolet, NeonPink),
        AccentPalette("Toxic Lime", NeonLime, NeonCyan, NeonViolet),
        AccentPalette("Hot Magenta", NeonPink, NeonViolet, NeonCyan),
        AccentPalette("Plasma Amber", NeonAmber, NeonPink, NeonViolet)
    )

    fun get(index: Int): AccentPalette = ALL.getOrElse(index) { ALL[0] }
}

fun schemeFor(accentIndex: Int) = Accents.get(accentIndex).let { a ->
    darkColorScheme(
        primary = a.primary,
        onPrimary = Color(0xFF03060E),
        primaryContainer = a.secondary,
        onPrimaryContainer = Color(0xFF03060E),
        secondary = a.secondary,
        onSecondary = Color(0xFF03060E),
        tertiary = a.tertiary,
        onTertiary = Color(0xFF03060E),
        background = BgTop,
        onBackground = OnDark,
        surface = Surface1,
        onSurface = OnDark,
        surfaceVariant = Surface2,
        onSurfaceVariant = Muted,
        outline = Hairline,
        error = NeonRed,
        onError = Color(0xFF1A0208)
    )
}

val SoulTypography = Typography(
    headlineMedium = TextStyle(
        fontSize = 30.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = (-0.4).sp
    ),
    titleLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.ExtraBold),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold),
    bodyMedium = TextStyle(fontSize = 14.sp),
    bodySmall = TextStyle(fontSize = 12.sp),
    labelSmall = TextStyle(
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.6.sp
    )
)

@Composable
fun SoulTheme(accentIndex: Int = 0, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = schemeFor(accentIndex),
        typography = SoulTypography,
        content = content
    )
}
