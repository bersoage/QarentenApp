package app.qarentenapp.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Download
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import app.qarentenapp.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val MaterialIconsFont = FontFamily(Font(R.font.material_icons))
val MaterialIconsOutlinedFont = FontFamily(Font(R.font.material_icons_outlined))

/** Glifo de la fuente de íconos (por código). */
@Composable
fun Glyph(glyph: String, size: Dp, tint: Color, outlined: Boolean = false, modifier: Modifier = Modifier) {
    Text(
        glyph,
        fontFamily = if (outlined) MaterialIconsOutlinedFont else MaterialIconsFont,
        fontSize = with(LocalDensity.current) { size.toSp() },
        lineHeight = with(LocalDensity.current) { size.toSp() },
        color = tint,
        modifier = modifier,
    )
}

/**
 * Ícono de un grupo: un glifo del catálogo o, si la clave es "app:<paquete>", el ícono de esa app.
 */
@Composable
fun GroupGlyph(key: String, size: Dp, tint: Color) {
    if (key.startsWith("app:")) {
        val pkg = key.removePrefix("app:")
        var bmp by remember(pkg) { mutableStateOf<ImageBitmap?>(null) }
        LaunchedEffect(pkg) {
            bmp = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                app.qarentenapp.core.Repo.source.loadIconBitmap(pkg, 128)?.asImageBitmap()
            }
        }
        val b = bmp
        if (b != null) Image(b, null, Modifier.size(size).clip(RoundedCornerShape(size * 0.25f)))
        else Box(Modifier.size(size))
    } else Glyph(iconDef(key).glyph, size, tint)
}

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

/** Ícono de grupo con carámbanos (el mismo de los accesos directos y widgets). */
@Composable
fun GroupTile(g: app.qarentenapp.data.Group, frozen: Boolean, size: Dp) {
    val px = with(LocalDensity.current) { size.roundToPx() }
    var bmp by remember(g, frozen, px) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(g, frozen, px) {
        bmp = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
            runCatching { app.qarentenapp.core.Bitmaps.group(g, frozen, px.coerceAtLeast(48)).asImageBitmap() }.getOrNull()
        }
    }
    val b = bmp
    if (b != null) Image(b, contentDescription = null, modifier = Modifier.size(size))
    else Box(Modifier.size(size).clip(RoundedCornerShape(size * 0.22f)).background(groupColors(g.color).first))
}

/** Pedido de confirmación para congelar apps del sistema. */
data class SystemConfirm(val pkgs: List<String>, val system: List<String>, val run: () -> Unit)

/** Aviso ⚠️ antes de congelar apps del sistema. */
@Composable
fun SystemConfirmDialog(req: SystemConfirm, onDismiss: () -> Unit) {
    var dontAsk by remember { mutableStateOf(false) }
    val names = req.system.map { app.qarentenapp.core.Repo.labelOf(it) }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("⚠️ Ojo: " + if (names.size == 1) "es una app del sistema" else "hay ${names.size} apps del sistema") },
        text = {
            androidx.compose.foundation.layout.Column {
                Text(
                    (if (names.size == 1) "«${names[0]}»" else names.joinToString(", ") { "«$it»" }) +
                        ". Congelar apps del sistema puede hacer que algo del celular deje de funcionar (notificaciones, cámara, llamadas…). ¿Congelar igual?"
                )
                androidx.compose.foundation.layout.Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 8.dp).clickable { dontAsk = !dontAsk },
                ) {
                    androidx.compose.material3.Checkbox(checked = dontAsk, onCheckedChange = { dontAsk = it })
                    Text(if (names.size == 1) "No volver a preguntar por esta app" else "No volver a preguntar por estas apps", fontSize = 14.sp)
                }
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = {
                if (dontAsk) app.qarentenapp.core.Repo.ackSystem(req.system)
                req.run(); onDismiss()
            }) { Text("Congelar igual") }
        },
        dismissButton = { androidx.compose.material3.TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}
