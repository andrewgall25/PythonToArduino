package com.andrea.pythontoarduino.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.isSystemInDarkTheme
import com.google.accompanist.systemuicontroller.rememberSystemUiController

// Definisci la palette colori chiara
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFFE91E63),
    onPrimary = Color.White,
    background = Color(0xFFFFFFFF),
    onBackground = Color.Black,
    surface = Color(0xFFFFFFFF),
    onSurface = Color.Black,
    // aggiungi altri colori se ti servono
)

// Definisci la palette colori scura
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFE91E63),
    onPrimary = Color.Black,
    background = Color(0xFF121921),   // il tuo sfondo nero
    onBackground = Color.White,       // testo bianco su sfondo nero
    surface = Color(0xFF002B44),
    onSurface = Color.White,
    // aggiungi altri colori se ti servono
)

@Composable
fun PythonToArduinoTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColorScheme else LightColorScheme

    val systemUiController = rememberSystemUiController()

    SideEffect {
        // 🔥 Forza sempre navigation bar nera
        systemUiController.setNavigationBarColor(
            color = Color.Black,
            darkIcons = false // false = icone bianche
        )
    }

    MaterialTheme(
        colorScheme = colors,
        typography = Typography(),
        content = content
    )
}
