package com.mikeyphw.relatorio.android.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val LightColors = lightColorScheme(
    primary = Color(0xFF174E73),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCEAF2),
    onPrimaryContainer = Color(0xFF0D344E),
    secondary = Color(0xFF2F6B58),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD8EEE5),
    onSecondaryContainer = Color(0xFF133D31),
    background = Color(0xFFF7FAFC),
    onBackground = Color(0xFF17212B),
    surface = Color.White,
    onSurface = Color(0xFF17212B),
    surfaceVariant = Color(0xFFE8EEF2),
    onSurfaceVariant = Color(0xFF465762),
    outline = Color(0xFF748691),
    error = Color(0xFF9B1C1C),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9ACCF0),
    onPrimary = Color(0xFF00344F),
    primaryContainer = Color(0xFF164B6D),
    onPrimaryContainer = Color(0xFFD1E9FA),
    secondary = Color(0xFF9FD8BF),
    onSecondary = Color(0xFF07382B),
    secondaryContainer = Color(0xFF225043),
    onSecondaryContainer = Color(0xFFBCEED7),
    background = Color(0xFF11171B),
    onBackground = Color(0xFFE3E8EC),
    surface = Color(0xFF171E22),
    onSurface = Color(0xFFE3E8EC),
    surfaceVariant = Color(0xFF293238),
    onSurfaceVariant = Color(0xFFC1CBD1),
    outline = Color(0xFF8B9BA5),
)

private val LargeTypography = Typography(
    headlineLarge = TextStyle(fontSize = 32.sp, lineHeight = 39.sp, fontWeight = FontWeight.SemiBold),
    headlineMedium = TextStyle(fontSize = 28.sp, lineHeight = 35.sp, fontWeight = FontWeight.SemiBold),
    headlineSmall = TextStyle(fontSize = 24.sp, lineHeight = 31.sp, fontWeight = FontWeight.SemiBold),
    titleLarge = TextStyle(fontSize = 22.sp, lineHeight = 29.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 19.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold),
    titleSmall = TextStyle(fontSize = 17.sp, lineHeight = 23.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 18.sp, lineHeight = 27.sp),
    bodyMedium = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    bodySmall = TextStyle(fontSize = 14.sp, lineHeight = 21.sp),
    labelLarge = TextStyle(fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold),
    labelMedium = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.Medium),
    labelSmall = TextStyle(fontSize = 14.sp, lineHeight = 19.sp, fontWeight = FontWeight.Medium),
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun RelatorioTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        typography = LargeTypography,
        shapes = AppShapes,
        content = content,
    )
}
