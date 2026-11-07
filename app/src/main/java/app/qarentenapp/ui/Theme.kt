package app.qarentenapp.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.graphics.Color
import app.qarentenapp.core.Repo

val Navy = Color(0xFF001A5C)
val Ice = Color(0xFF3FD0F5)

/** Claro "hielo": fondo blanco azulado, tarjetas blancas, detalles en azul. */
private val IceLight = lightColorScheme(
    primary = Color(0xFF2F5E8E),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6E4F5),
    onPrimaryContainer = Color(0xFF1F4268),
    secondary = Color(0xFF0B6FA8),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD3ECF7),
    onSecondaryContainer = Color(0xFF0A3D5C),
    tertiary = Color(0xFF12705C),
    background = Color(0xFFF0F6FC),
    onBackground = Color(0xFF14202C),
    surface = Color(0xFFF0F6FC),
    onSurface = Color(0xFF14202C),
    surfaceVariant = Color(0xFFDDE6F0),
    onSurfaceVariant = Color(0xFF56626F),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainer = Color(0xFFE6EEF7),
    surfaceContainerHigh = Color(0xFFE1EBF6),
    surfaceContainerHighest = Color(0xFFD8E3F0),
    outline = Color(0xFF8A97A6),
    outlineVariant = Color(0xFFC5CED8),
    error = Color(0xFFB3261E),
)

/** Oscuro azul noche: azul marino profundo en vez de negro. */
private val NightDark = darkColorScheme(
    primary = Color(0xFF8CC0F2),
    onPrimary = Color(0xFF002E57),
    primaryContainer = Color(0xFF284A78),
    onPrimaryContainer = Color(0xFFD6E4F5),
    secondary = Ice,
    onSecondary = Color(0xFF00344A),
    secondaryContainer = Color(0xFF1C4A66),
    onSecondaryContainer = Color(0xFFCDEFFB),
    tertiary = Color(0xFF7FD6BE),
    background = Color(0xFF0C1830),
    onBackground = Color(0xFFE8F0FA),
    surface = Color(0xFF0C1830),
    onSurface = Color(0xFFE8F0FA),
    surfaceVariant = Color(0xFF243656),
    onSurfaceVariant = Color(0xFF96AAC8),
    surfaceContainerLowest = Color(0xFF0A1428),
    surfaceContainerLow = Color(0xFF162646),
    surfaceContainer = Color(0xFF10203E),
    surfaceContainerHigh = Color(0xFF1C2E52),
    surfaceContainerHighest = Color(0xFF26395F),
    outline = Color(0xFF5E739A),
    outlineVariant = Color(0xFF32466A),
    error = Color(0xFFFFB4AB),
)

/** Colores propios de Q'App (no se copian los del sistema). Modo: automático, claro u oscuro. */
@Composable
fun QTheme(content: @Composable () -> Unit) {
    val mode = Repo.config.collectAsState().value.settings.themeMode
    val dark = when (mode) {
        "LIGHT" -> false
        "DARK" -> true
        else -> isSystemInDarkTheme()
    }
    MaterialTheme(colorScheme = if (dark) NightDark else IceLight, content = content)
}
