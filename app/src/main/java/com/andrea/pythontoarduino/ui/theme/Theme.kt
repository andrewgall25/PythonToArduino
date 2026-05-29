package com.andrea.pythontoarduino.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.isSystemInDarkTheme
import com.andrea.pythontoarduino.R

// Font families matching HTML (Manrope + JetBrains Mono)
val Manrope = FontFamily(
    Font(R.font.base_neue, FontWeight.Normal),
    Font(R.font.base_neue, FontWeight.SemiBold),
    Font(R.font.base_neue, FontWeight.Bold),
    Font(R.font.base_neue, FontWeight.ExtraBold)
)

val JetBrainsMono = FontFamily(
    Font(R.font.jetbrainsmono_regular, FontWeight.Normal),
    Font(R.font.jetbrainsmono_regular, FontWeight.Medium),
    Font(R.font.jetbrainsmono_regular, FontWeight.Bold)
)

// Design System: Glow-Integrated IDE (Stitch MCP)
// Matches the HTML color schemes exactly
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF7BDB80),
    onPrimary = Color(0xFF00390E),
    primaryContainer = Color(0xFF238636),
    onPrimaryContainer = Color(0xFFF9FFF3),
    secondary = Color(0xFFB5C4FF),
    onSecondary = Color(0xFF112B6E),
    secondaryContainer = Color(0xFF2F4588),
    onSecondaryContainer = Color(0xFFA1B6FF),
    tertiary = Color(0xFFEFC04B),
    onTertiary = Color(0xFF3F2E00),
    tertiaryContainer = Color(0xFF937000),
    onTertiaryContainer = Color(0xFFFFFDFF),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF10141A),
    onBackground = Color(0xFFDFE2EB),
    surface = Color(0xFF10141A),
    onSurface = Color(0xFFDFE2EB),
    surfaceVariant = Color(0xFF31353C),
    onSurfaceVariant = Color(0xFFBECABA),
    outline = Color(0xFF899485),
    outlineVariant = Color(0xFF3F4A3D),
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF7BDB80),
    onPrimary = Color(0xFF00390E),
    primaryContainer = Color(0xFF238636),
    onPrimaryContainer = Color(0xFFF9FFF3),
    secondary = Color(0xFFB5C4FF),
    onSecondary = Color(0xFF112B6E),
    secondaryContainer = Color(0xFF2F4588),
    onSecondaryContainer = Color(0xFFA1B6FF),
    tertiary = Color(0xFFEFC04B),
    onTertiary = Color(0xFF3F2E00),
    tertiaryContainer = Color(0xFF937000),
    onTertiaryContainer = Color(0xFFFFFDFF),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF10141A),
    onBackground = Color(0xFFDFE2EB),
    surface = Color(0xFF10141A),
    onSurface = Color(0xFFDFE2EB),
    surfaceVariant = Color(0xFF31353C),
    onSurfaceVariant = Color(0xFFBECABA),
    outline = Color(0xFF899485),
    outlineVariant = Color(0xFF3F4A3D),
)

// Custom colors from HTML design (used outside Material theme)
object HtmlColors {
    val BgPrimary = Color(0xFF0d1117)
    val BgCard = Color(0xFF161b22)
    val BorderColor = Color(0xFF30363d)
    val BorderSubtle = Color(0xFF21262d)
    val TextPrimary = Color(0xFFe6edf3)
    val TextSecondary = Color(0xFF8b949e)
    val TextDimmed = Color(0xFFC4CAD0)
    val AccentGreen = Color(0xFF238636)
    val StopRed = Color(0xFFef5350)
    val FlashOrange = Color(0xFFffa726)
    val SyntaxGreen = Color(0xFF6a9955)
    val SyntaxBlue = Color(0xFFa5d6ff)
    val SyntaxRed = Color(0xFFff7b72)
    val SyntaxOrange = Color(0xFFce9178)
    val SyntaxPurple = Color(0xFFd2a8ff)
    val RunBlue = Color(0xFF86bbd8)
    val OceanBlue = Color(0xFF002b44)
    val SurfaceContainer = Color(0xFF1c2026)
    val SurfaceContainerLow = Color(0xFF181c22)
    val SurfaceContainerLowest = Color(0xFF0a0e14)
    val SurfaceContainerHigh = Color(0xFF262a31)
    val SurfaceContainerHighest = Color(0xFF31353c)
    // Material-mapped colors for direct access outside MaterialTheme
    val Primary = Color(0xFF7BDB80)
    val OnPrimary = Color(0xFF00390E)
    val PrimaryContainer = Color(0xFF238636)
    val OnPrimaryContainer = Color(0xFFF9FFF3)
    val Tertiary = Color(0xFFEFC04B)
    val OnSurfaceVariant = Color(0xFFBECABA)
    val Background = Color(0xFF10141A)
    val OnSurface = Color(0xFFDFE2EB)
}

@Composable
fun PythonToArduinoTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colors,
        typography = Typography(),
        content = content
    )
}
