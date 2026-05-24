package com.neo.locallm.theme

import android.annotation.SuppressLint
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val NeoDarkColorScheme = darkColorScheme(
    primary = Color(0xFFFF5A7A),
    onPrimary = Color(0xFF35000A),
    primaryContainer = Color(0xFF7A102A),
    onPrimaryContainer = Color(0xFFFFD9E1),
    inversePrimary = Color(0xFFB71D3B),
    secondary = Color(0xFF93E7C6),
    onSecondary = Color(0xFF003827),
    secondaryContainer = Color(0xFF00513A),
    onSecondaryContainer = Color(0xFFB2F5DB),
    tertiary = Color(0xFFFFD36E),
    onTertiary = Color(0xFF3F2D00),
    tertiaryContainer = Color(0xFF5C4300),
    onTertiaryContainer = Color(0xFFFFE4A8),
    error = Red80,
    onError = Red20,
    errorContainer = Red30,
    onErrorContainer = Red90,
    background = Color(0xFF111014),
    onBackground = Color(0xFFF5EEF2),
    surface = Color(0xFF17151B),
    onSurface = Color(0xFFF5EEF2),
    inverseSurface = Color(0xFFE8DDE3),
    inverseOnSurface = Color(0xFF242128),
    surfaceVariant = Color(0xFF4C3F45),
    onSurfaceVariant = Color(0xFFD7C1C8),
    outline = Color(0xFFA38C94),
    surfaceContainer = Color(0xFF211D24),
    surfaceContainerHighest = Color(0xFF302931)
)

private val NeoLightColorScheme = lightColorScheme(
    primary = Color(0xFFB71D3B),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFD9E1),
    onPrimaryContainer = Color(0xFF40000D),
    inversePrimary = Color(0xFFFFB1C1),
    secondary = Color(0xFF006C4E),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFB2F5DB),
    onSecondaryContainer = Color(0xFF002116),
    tertiary = Color(0xFF765A00),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFE4A8),
    onTertiaryContainer = Color(0xFF251A00),
    error = Red40,
    onError = Color.White,
    errorContainer = Red90,
    onErrorContainer = Red10,
    background = Color(0xFFFFF8FA),
    onBackground = Color(0xFF23191D),
    surface = Color(0xFFFFF8FA),
    onSurface = Color(0xFF23191D),
    inverseSurface = Color(0xFF392E33),
    inverseOnSurface = Color(0xFFFFECF1),
    surfaceVariant = Color(0xFFF3DDE4),
    onSurfaceVariant = Color(0xFF514348),
    outline = Color(0xFF837178),
    surfaceContainer = Color(0xFFFFEDF2),
    surfaceContainerHighest = Color(0xFFF3DDE4)
)

@SuppressLint("NewApi")
@Composable
fun PlaygroundTheme(
    isDarkTheme: Boolean = isSystemInDarkTheme(),
    isDynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val dynamicColor = isDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val myColorScheme = when {
        dynamicColor && isDarkTheme -> {
            dynamicDarkColorScheme(LocalContext.current)
        }
        dynamicColor && !isDarkTheme -> {
            dynamicLightColorScheme(LocalContext.current)
        }
        isDarkTheme -> NeoDarkColorScheme
        else -> NeoLightColorScheme
    }

    MaterialTheme(
        colorScheme = myColorScheme,
        typography = PlaygroundTypography,
        content = content
    )
}
