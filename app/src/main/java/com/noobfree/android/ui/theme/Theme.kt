package com.noobfree.android.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF4DD0E1),
    onPrimary = Color(0xFF00363D),
    primaryContainer = Color(0xFF004F58),
    onPrimaryContainer = Color(0xFF97F0FF),
    secondary = Color(0xFF80CBC4),
    onSecondary = Color(0xFF003732),
    secondaryContainer = Color(0xFF1E3538),
    onSecondaryContainer = Color(0xFFCCE8E4),
    tertiary = Color(0xFFA5D6A7),
    background = Color(0xFF0B1117),
    surface = Color(0xFF111922),
    surfaceVariant = Color(0xFF192430),
    onBackground = Color(0xFFE4ECF2),
    onSurface = Color(0xFFE4ECF2),
    onSurfaceVariant = Color(0xFFA5B4C2),
    outline = Color(0xFF3A4B5C),
    error = Color(0xFFFF8A80),
    errorContainer = Color(0xFF5C1916),
    onErrorContainer = Color(0xFFFFDAD6)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF006874),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF97F0FF),
    onPrimaryContainer = Color(0xFF001F24),
    secondary = Color(0xFF2B6761),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD5ECE8),
    onSecondaryContainer = Color(0xFF06201D),
    background = Color(0xFFF4F7FA),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFE6EDF3),
    onBackground = Color(0xFF111922),
    onSurface = Color(0xFF111922),
    onSurfaceVariant = Color(0xFF435363),
    outline = Color(0xFF738394)
)

val NoobFreeTypography = Typography(
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        letterSpacing = 0.8.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp
    )
)

@Composable
fun NoobFreeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = NoobFreeTypography,
        content = content
    )
}
