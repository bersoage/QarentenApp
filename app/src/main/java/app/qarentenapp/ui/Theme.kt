package app.qarentenapp.ui

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

val Navy = Color(0xFF001A5C)
val Ice = Color(0xFF3FD0F5)

private val Light = lightColorScheme(
    primary = Color(0xFF2F5E8E),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6E4F5),
    onPrimaryContainer = Color(0xFF1F4268),
    secondary = Color(0xFF0B6FA8),
    background = Color(0xFFF3F6FA),
    surface = Color(0xFFF3F6FA),
    surfaceContainer = Color(0xFFE9EEF4),
    surfaceContainerLow = Color(0xFFFFFFFF),
)

private val Dark = darkColorScheme(
    primary = Color(0xFF9CC6F2),
    onPrimary = Color(0xFF00315C),
    primaryContainer = Color(0xFF1F4268),
    onPrimaryContainer = Color(0xFFD6E4F5),
    secondary = Ice,
    background = Color(0xFF0E141B),
    surface = Color(0xFF0E141B),
)

@Composable
fun QTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val ctx = LocalContext.current
    val scheme = when {
        Build.VERSION.SDK_INT >= 31 -> if (dark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        dark -> Dark
        else -> Light
    }
    MaterialTheme(colorScheme = scheme, content = content)
}
