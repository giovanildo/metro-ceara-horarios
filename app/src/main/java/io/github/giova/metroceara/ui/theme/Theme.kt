package io.github.giova.metroceara.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.toColorInt
import io.github.giova.metroceara.data.Regras

private val MetroRed = Color(0xFFED1C24)

/** O app está no tema escuro (o do celular ou o escolhido nas configurações)? */
private val LocalDarkTheme = staticCompositionLocalOf { false }

private val LightColors = lightColorScheme(
    primary = MetroRed,
    onPrimary = Color.White,
    secondary = Color(0xFF8C3A3D),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFF8A85),
    onPrimary = Color(0xFF5F1013),
    secondary = Color(0xFFE7B4B2),
)

@Composable
fun MetroCearaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    wallpaperColors: Boolean = true,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colors = when {
        // Cor dinâmica a partir do papel de parede, quando o sistema oferece e
        // a pessoa não desligou nas configurações.
        wallpaperColors && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)

        darkTheme -> DarkColors
        else -> LightColors
    }
    CompositionLocalProvider(LocalDarkTheme provides darkTheme) {
        MaterialTheme(colorScheme = colors, content = content)
    }
}

/**
 * Converte a cor da linha (`"ce0f2e"`) em uma [Color]. No tema escuro, o azul e o
 * verde do mapa oficial ficam apagados e trocam pelo tom de regras.json.
 */
@Composable
fun routeColor(hex: String): Color {
    val shown = if (LocalDarkTheme.current) Regras.current.darkColor(hex) else hex
    return runCatching { Color("#$shown".toColorInt()) }.getOrDefault(MetroRed)
}
