package app.qarentenapp.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.TravelExplore
import androidx.compose.material.icons.outlined.Work
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Íconos disponibles para los grupos. */
val GroupIcons: List<Pair<String, ImageVector>> = listOf(
    "folder" to Icons.Outlined.Folder,
    "shopping" to Icons.Outlined.ShoppingBag,
    "bank" to Icons.Outlined.AccountBalance,
    "camera" to Icons.Outlined.PhotoCamera,
    "movie" to Icons.Outlined.Movie,
    "music" to Icons.Outlined.MusicNote,
    "games" to Icons.Outlined.SportsEsports,
    "work" to Icons.Outlined.Work,
    "chat" to Icons.Outlined.Chat,
    "school" to Icons.Outlined.School,
    "travel" to Icons.Outlined.TravelExplore,
    "star" to Icons.Outlined.Star,
)

fun groupIcon(key: String): ImageVector = GroupIcons.firstOrNull { it.first == key }?.second ?: Icons.Outlined.Folder

private val grey = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
private val mono = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })

/** Temas de íconos. */
val IconThemes = listOf(
    "ORIGINAL" to "Original",
    "ROUND" to "Redondos",
    "SQUARE" to "Cuadrados",
    "MONO" to "Monocromo",
)

/** Colores de tema de los grupos (fondo, primer plano). */
val GroupColorPairs: List<Pair<Color, Color>> = app.qarentenapp.core.Bitmaps.groupColors.map { Color(it.first) to Color(it.second) }

fun groupColors(index: Int) = GroupColorPairs[index.coerceIn(0, GroupColorPairs.lastIndex)]

/** Ícono de una app; en gris y con copo de nieve si está congelada. */
@Composable
fun AppIcon(
    vm: MainViewModel,
    pkg: String,
    size: Dp,
    frozen: Boolean,
    badge: Boolean = true,
    pending: Boolean = false,
) {
    var bmp by remember(pkg) { mutableStateOf<ImageBitmap?>(vm.icon(pkg)) }
    LaunchedEffect(pkg) { if (bmp == null && !pending) bmp = vm.loadIcon(pkg) }
    val theme = vm.config.collectAsState().value.settings.iconTheme
    val shape: Shape = when (theme) {
        "ROUND" -> CircleShape
        "SQUARE" -> RoundedCornerShape(size * 0.12f)
        else -> RoundedCornerShape(size * 0.28f)
    }
    Box(Modifier.size(size)) {
        val b = bmp
        if (b != null && !pending) {
            Image(
                bitmap = b,
                contentDescription = null,
                colorFilter = if (frozen) grey else if (theme == "MONO") mono else null,
                modifier = Modifier.size(size).clip(if (theme == "ORIGINAL") RectangleShape else shape)
                    .alpha(if (frozen) 0.55f else 1f),
            )
        } else {
            Box(
                Modifier
                    .size(size)
                    .clip(shape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    .then(if (pending) Modifier.border(2.dp, MaterialTheme.colorScheme.outline, shape) else Modifier),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    pkg.substringAfterLast('.').take(1).uppercase(),
                    fontSize = (size.value * 0.36f).sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }
        if (badge && (frozen || pending)) {
            val bs = (size * 0.38f).coerceAtLeast(16.dp)
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .size(bs)
                    .clip(CircleShape)
                    .background(if (pending) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary)
                    .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (pending) Icons.Filled.Download else Icons.Filled.AcUnit,
                    contentDescription = if (pending) "Pendiente" else "Congelada",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(bs * 0.62f),
                )
            }
        }
    }
}
