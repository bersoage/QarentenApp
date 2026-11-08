package app.qarentenapp.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import app.qarentenapp.core.Repo
import app.qarentenapp.data.AppSettings

val Navy = Color(0xFF001A5C)
val Ice = Color(0xFF3FD0F5)

/** Colores de acento: nombre, tono para fondo claro, tono para fondo oscuro. */
val Accents: List<Triple<String, Color, Color>> = listOf(
    Triple("Azul hielo", Color(0xFF2F5E8E), Color(0xFF8CC0F2)),
    Triple("Celeste", Color(0xFF0B6FA8), Color(0xFF7FD3F5)),
    Triple("Verde", Color(0xFF2E7D3A), Color(0xFF8FD39A)),
    Triple("Violeta", Color(0xFF6A4FB3), Color(0xFFC3B1F5)),
    Triple("Rosa", Color(0xFFA3346B), Color(0xFFF5A8CB)),
    Triple("Naranja", Color(0xFFB4531F), Color(0xFFF7B48A)),
    Triple("Rojo", Color(0xFFB3261E), Color(0xFFFFB4AB)),
    Triple("Dorado", Color(0xFF8A6A12), Color(0xFFE8C66A)),
)

/** Fondos: tonos de superficie de cada estilo. */
private data class Bg(
    val background: Color, val lowest: Color, val low: Color, val container: Color, val high: Color, val highest: Color,
    val text: Color, val sub: Color, val outline: Color, val outlineVariant: Color,
)

private val Backgrounds = mapOf(
    "LIGHT" to Bg(Color(0xFFF0F6FC), Color.White, Color.White, Color(0xFFE6EEF7), Color(0xFFE1EBF6), Color(0xFFD8E3F0),
        Color(0xFF14202C), Color(0xFF56626F), Color(0xFF8A97A6), Color(0xFFC5CED8)),
    "NAVY" to Bg(Color(0xFF0C1830), Color(0xFF0A1428), Color(0xFF162646), Color(0xFF10203E), Color(0xFF1C2E52), Color(0xFF26395F),
        Color(0xFFE8F0FA), Color(0xFF96AAC8), Color(0xFF5E739A), Color(0xFF32466A)),
    "CHARCOAL" to Bg(Color(0xFF1A1A19), Color(0xFF151515), Color(0xFF262625), Color(0xFF20201F), Color(0xFF2C2C2A), Color(0xFF363634),
        Color(0xFFECECEA), Color(0xFFA0A09C), Color(0xFF6A6865), Color(0xFF3D3B39)),
    "OLED" to Bg(Color.Black, Color.Black, Color(0xFF121212), Color(0xFF0A0A0A), Color(0xFF1A1A1A), Color(0xFF242424),
        Color(0xFFEDEDED), Color(0xFF9E9E9E), Color(0xFF5A5A5A), Color(0xFF2A2A2A)),
)

/** El fondo elegido (con compatibilidad con la beta 6). */
fun effectiveBackground(s: AppSettings): String = s.background.ifEmpty {
    when (s.themeMode) { "LIGHT" -> "LIGHT"; "DARK" -> "NAVY"; else -> "AUTO" }
}

private fun scheme(style: String, accent: Int): ColorScheme {
    val bg = Backgrounds[style] ?: Backgrounds.getValue("NAVY")
    val (_, aLight, aDark) = Accents[accent.coerceIn(0, Accents.lastIndex)]
    return if (style == "LIGHT") lightColorScheme(
        primary = aLight, onPrimary = Color.White,
        primaryContainer = lerp(aLight, Color.White, 0.82f), onPrimaryContainer = lerp(aLight, Color.Black, 0.4f),
        secondary = aLight, onSecondary = Color.White,
        secondaryContainer = lerp(aLight, Color.White, 0.86f), onSecondaryContainer = lerp(aLight, Color.Black, 0.45f),
        background = bg.background, onBackground = bg.text, surface = bg.background, onSurface = bg.text,
        surfaceVariant = bg.highest, onSurfaceVariant = bg.sub,
        surfaceContainerLowest = bg.lowest, surfaceContainerLow = bg.low, surfaceContainer = bg.container,
        surfaceContainerHigh = bg.high, surfaceContainerHighest = bg.highest,
        outline = bg.outline, outlineVariant = bg.outlineVariant, error = Color(0xFFB3261E),
    ) else darkColorScheme(
        primary = aDark, onPrimary = lerp(aLight, Color.Black, 0.6f),
        primaryContainer = lerp(aLight, bg.background, 0.35f), onPrimaryContainer = lerp(aDark, Color.White, 0.6f),
        secondary = aDark, onSecondary = lerp(aLight, Color.Black, 0.6f),
        secondaryContainer = lerp(aLight, bg.background, 0.5f), onSecondaryContainer = lerp(aDark, Color.White, 0.6f),
        background = bg.background, onBackground = bg.text, surface = bg.background, onSurface = bg.text,
        surfaceVariant = bg.highest, onSurfaceVariant = bg.sub,
        surfaceContainerLowest = bg.lowest, surfaceContainerLow = bg.low, surfaceContainer = bg.container,
        surfaceContainerHigh = bg.high, surfaceContainerHighest = bg.highest,
        outline = bg.outline, outlineVariant = bg.outlineVariant, error = Color(0xFFFFB4AB),
    )
}

/** Colores propios de Q'App: fondo (claro, azul noche, gris carbón, negro OLED o según el celular) y acento. */
@Composable
fun QTheme(content: @Composable () -> Unit) {
    val s = Repo.config.collectAsState().value.settings
    val style = when (val b = effectiveBackground(s)) {
        "AUTO" -> if (isSystemInDarkTheme()) s.darkStyle else "LIGHT"
        else -> b
    }
    MaterialTheme(colorScheme = scheme(style, s.accent), content = content)
}
