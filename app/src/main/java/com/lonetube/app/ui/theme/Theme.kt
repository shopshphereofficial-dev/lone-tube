package com.lonetube.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ---- LoneTube palette: deep space violet with neon cyan ----
val Violet = Color(0xFF8B5CF6)
val VioletDeep = Color(0xFF6D28D9)
val Cyan = Color(0xFF22D3EE)
val Pink = Color(0xFFF472B6)
val Lime = Color(0xFFA3E635)
val Amber = Color(0xFFFBBF24)

val BgTop = Color(0xFF0B0716)
val BgBottom = Color(0xFF150A2E)
val Surface1 = Color(0xFF1B1132)
val Surface2 = Color(0xFF241640)
val OnDark = Color(0xFFF1EDFF)
val Muted = Color(0xFFA79FC7)
val Hairline = Color(0x26FFFFFF)

val LoneScheme = darkColorScheme(
    primary = Violet,
    onPrimary = Color.White,
    primaryContainer = VioletDeep,
    onPrimaryContainer = Color.White,
    secondary = Cyan,
    onSecondary = Color(0xFF04212B),
    tertiary = Pink,
    background = BgTop,
    onBackground = OnDark,
    surface = Surface1,
    onSurface = OnDark,
    surfaceVariant = Surface2,
    onSurfaceVariant = Muted,
    outline = Hairline,
    error = Color(0xFFFF6B81),
    onError = Color(0xFF2B0A11)
)

val LoneTypography = Typography(
    headlineMedium = TextStyle(
        fontSize = 30.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.6).sp
    ),
    titleLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
    bodyMedium = TextStyle(fontSize = 14.sp),
    bodySmall = TextStyle(fontSize = 12.sp),
    labelSmall = TextStyle(
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.8.sp
    )
)

@Composable
fun LoneTubeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LoneScheme,
        typography = LoneTypography,
        content = content
    )
}
