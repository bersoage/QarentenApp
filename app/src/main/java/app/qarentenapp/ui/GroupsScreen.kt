package app.qarentenapp.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.qarentenapp.core.Mode
import app.qarentenapp.core.Shortcuts
import app.qarentenapp.data.AppEntry
import app.qarentenapp.data.Group
import java.text.DateFormat
import java.util.Date

@Composable
fun buildBrand() = buildAnnotatedString {
    withStyle(SpanStyle(fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)) { append("Qarenten") }
    withStyle(SpanStyle(fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)) { append("App") }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GroupsScreen(vm: MainViewModel, open: (String) -> Unit, created: (String) -> Unit, openApp: (String) -> Unit) {
    val config by vm.config.collectAsState()
    val apps by vm.apps.collectAsState()
    val mode by vm.mode.collectAsState()
    val map = remember(apps) { apps.associateBy { it.pkg } }
    var creating by rememberSaveable { mutableStateOf(false) }
    var searching by rememberSaveable { mutableStateOf(false) }
    var filter by rememberSaveable { mutableStateOf("") }
    val groups = config.groups.filter { filter.isBlank() || it.name.contains(filter.trim(), ignoreCase = true) }
    val favorites = config.favorites.filter { it in map }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Column(Modifier.padding(horizontal = 8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.weight(1f)) { BrandMark() }
                        IconButton(onClick = { searching = !searching; if (!searching) filter = "" }) {
                            Icon(if (searching) Icons.Outlined.Close else Icons.Outlined.Search, "Filtrar grupos")
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text("Grupos", fontSize = 32.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    StatusChips(vm, mode, config.settings.lastBackupAt, config.settings.autoBackupTree != null)
                    if (searching) OutlinedTextField(
                        filter, { filter = it }, singleLine = true, placeholder = { Text("Filtrar grupos") },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    )
                }
            }
            if (favorites.isNotEmpty() && filter.isBlank()) {
                item {
                    Column {
                        Text("Favoritas", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 8.dp, bottom = 8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(horizontal = 8.dp)) {
                            items(favorites, key = { it }) { p ->
                                val e = map[p]!!
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.width(64.dp).combinedClickable(
                                        onClick = { vm.toggle(p) }, onLongClick = { openApp(p) },
                                    ),
                                ) {
                                    AppIcon(vm, p, 48.dp, frozen = e.frozen)
                                    Text(e.label, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }
            }
            if (config.groups.isEmpty()) {
                item {
                    Text(
                        "Todavía no tenés grupos. Creá uno con el botón de abajo y sumale las apps que querés congelar juntas.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(8.dp),
                    )
                }
            }
            items(groups, key = { it.id }) { g ->
                GroupCard(vm, g, map, onOpen = { open(g.id) })
            }
        }
        ExtendedFloatingActionButton(
            onClick = { creating = true },
            icon = { Icon(Icons.Filled.Add, null) },
            text = { Text("Nuevo grupo") },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        )
    }

    if (creating) GroupDialog(
        title = "Nuevo grupo", initialName = "", initialIcon = "folder", initialColor = config.groups.size % GroupColorPairs.size,
        onDismiss = { creating = false },
        onSave = { name, icon, color -> creating = false; created(vm.createGroup(name, icon, color)) },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StatusChips(vm: MainViewModel, mode: Mode?, lastBackup: Long, auto: Boolean) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        val (label, color) = when (mode) {
            null -> "Detectando…" to MaterialTheme.colorScheme.outline
            Mode.ROOT -> "Root activo" to androidx.compose.ui.graphics.Color(0xFF2E7D3A)
            Mode.SHIZUKU -> "Shizuku activo" to androidx.compose.ui.graphics.Color(0xFF2E7D3A)
            Mode.NONE -> "Sin root ni Shizuku" to MaterialTheme.colorScheme.error
        }
        AssistChip(
            onClick = { vm.redetect() },
            label = { Text(label) },
            leadingIcon = { Surface(Modifier.size(8.dp), shape = CircleShape, color = color) {} },
        )
        val text = if (lastBackup == 0L) "Sin backup" else "Backup: " + DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(lastBackup))
        AssistChip(
            onClick = {},
            label = { Text(text) },
            leadingIcon = {
                Icon(if (auto || lastBackup > 0) Icons.Outlined.CloudDone else Icons.Outlined.CloudOff, null, Modifier.size(AssistChipDefaults.IconSize))
            },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GroupCard(vm: MainViewModel, g: Group, map: Map<String, AppEntry>, onOpen: () -> Unit) {
    val ctx = LocalContext.current
    val installed = g.packages.mapNotNull { map[it] }
    val frozen = installed.count { it.frozen }
    val pending = g.packages.size - installed.size
    val allFrozen = installed.isNotEmpty() && installed.filterNot { it.protected }.all { it.frozen }
    val (bg, fg) = groupColors(g.color)
    var menu by remember { mutableStateOf(false) }

    Box {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            modifier = Modifier.fillMaxWidth().combinedClickable(onClick = onOpen, onLongClick = { menu = true }),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(16.dp), color = bg, modifier = Modifier.size(48.dp)) {
                        Box(contentAlignment = Alignment.Center) { Icon(groupIcon(g.icon), null, tint = fg) }
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(g.name, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                        val sub = when {
                            installed.isEmpty() && pending == 0 -> "Sin apps"
                            frozen == 0 -> "Todas activas"
                            else -> "$frozen de ${installed.size} congeladas"
                        } + if (pending > 0) " · $pending pendientes" else ""
                        Text(sub, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = allFrozen,
                        onCheckedChange = { vm.setGroupFrozen(g, it) },
                        enabled = installed.isNotEmpty(),
                        thumbContent = if (allFrozen) { { Icon(Icons.Filled.AcUnit, null, Modifier.size(16.dp)) } } else null,
                    )
                }
                if (g.packages.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        g.packages.take(8).forEach { p ->
                            val e = map[p]
                            AppIcon(vm, p, 30.dp, frozen = e?.frozen == true, badge = false, pending = e == null)
                        }
                        if (g.packages.size > 8) Text("+${g.packages.size - 8}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            DropdownMenuItem(text = { Text(if (allFrozen) "Activar todo" else "Congelar todo") },
                onClick = { menu = false; vm.setGroupFrozen(g, !allFrozen) })
            if (Shortcuts.canPin(ctx)) DropdownMenuItem(text = { Text("Agregar a pantalla de inicio") },
                onClick = { menu = false; Shortcuts.pinGroup(ctx, g) })
            DropdownMenuItem(text = { Text("Subir") }, onClick = { menu = false; vm.moveGroup(g.id, -1) })
            DropdownMenuItem(text = { Text("Bajar") }, onClick = { menu = false; vm.moveGroup(g.id, 1) })
        }
    }
}

/** Diálogo para crear o editar un grupo: nombre, ícono y color. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GroupDialog(
    title: String,
    initialName: String,
    initialIcon: String,
    initialColor: Int,
    onDismiss: () -> Unit,
    onSave: (String, String, Int) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(initialName) }
    var icon by rememberSaveable { mutableStateOf(initialIcon) }
    var color by rememberSaveable { mutableStateOf(initialColor) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it.take(40) },
                    label = { Text("Nombre") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                )
                Text("Ícono", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    GroupIcons.forEach { (key, vec) ->
                        FilledIconToggleButton(checked = icon == key, onCheckedChange = { icon = key }) {
                            Icon(vec, contentDescription = key)
                        }
                    }
                }
                Text("Color", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    GroupColorPairs.forEachIndexed { i, (bg, fg) ->
                        Surface(
                            onClick = { color = i }, shape = CircleShape, color = bg, modifier = Modifier.size(36.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (color == i) Icon(Icons.Filled.Check, "Elegido", tint = fg, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(enabled = name.isNotBlank(), onClick = { onSave(name, icon, color) }) { Text("Guardar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}
