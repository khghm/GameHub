package com.gamehub.host.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ===== Main Colors =====
val Primary = Color(0xFF6366F1)
val PrimaryLight = Color(0xFF818CF8)
val PrimaryDark = Color(0xFF4F46E5)
val Secondary = Color(0xFF7C3AED)
val SecondaryLight = Color(0xFFA78BFA)
val Tertiary = Color(0xFFEC4899)
val TertiaryLight = Color(0xFFF472B6)
val Background = Color(0xFF0F172A)
val BackgroundGradient = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF0F172A),
        Color(0xFF111827),
        Color(0xFF1E293B)
    )
)
val Surface = Color(0xFF1E293B)
val SurfaceLight = Color(0xFF334155)
val SurfaceVariant = Color(0xFF475569)
val OnPrimary = Color.White
val OnSecondary = Color.White
val OnBackground = Color.White
val OnSurface = Color.White
val OnSurfaceVariant = Color(0xFF94A3B8)
val Success = Color(0xFF22C55E)
val Warning = Color(0xFFF59E0B)
val Error = Color(0xFFEF4444)

// ===== Light/Dark Colors (we'll use only dark for this app) =====
private val DarkColorScheme = darkColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryDark,
    secondary = Secondary,
    onSecondary = OnSecondary,
    tertiary = Tertiary,
    background = Background,
    surface = Surface,
    surfaceVariant = SurfaceVariant,
    onBackground = OnBackground,
    onSurface = OnSurface,
    onSurfaceVariant = OnSurfaceVariant,
    error = Error,
    onError = Color.White
)

// ===== Typography =====
private val AppTypography = Typography(
    displayLarge = TextStyle(
        fontSize = 32.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 40.sp
    ),
    displayMedium = TextStyle(
        fontSize = 28.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 36.sp
    ),
    displaySmall = TextStyle(
        fontSize = 24.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 32.sp
    ),
    headlineLarge = TextStyle(
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 30.sp
    ),
    headlineMedium = TextStyle(
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 28.sp
    ),
    headlineSmall = TextStyle(
        fontSize = 18.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 26.sp
    ),
    titleLarge = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 24.sp
    ),
    titleMedium = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 22.sp
    ),
    titleSmall = TextStyle(
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 20.sp
    ),
    bodyLarge = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 22.sp
    ),
    bodySmall = TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 20.sp
    ),
    labelLarge = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 20.sp
    ),
    labelMedium = TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 18.sp
    ),
    labelSmall = TextStyle(
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 16.sp
    )
)

// ===== Theme =====
@Composable
fun GameHubTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = AppTypography,
        shapes = Shapes(
            extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
            small = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
            medium = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
            large = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
            extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(28.dp)
        ),
        content = content
    )
}
