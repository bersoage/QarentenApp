package app.qarentenapp.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.PlaylistAdd
import androidx.compose.material.icons.outlined.AddToHomeScreen
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Column as Col

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupScreen(vm: MainViewModel, id: String, back: () -> Unit, pick: () -> Unit, openApp: (String) -> Unit) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val config by vm.config.collectAsState()
    val apps by vm.apps.collectAsState()
    val g = config.groups.firstOrNull { it.id == id }
    if (g == null) { androidx.compose.runtime.LaunchedEffect(Unit) { back() }; return }
    val map = apps.associateBy { it.pkg }
    val installed = g.packages.mapNotNull { map[it] }
    val pending = g.packages.filter { it !in map }
    val frozenCount = installed.count { it.frozen }
    val allFrozen = installed.isNotEmpty() && installed.filterNot { it.protected }.all { it.frozen }

    var editing by rememberSaveable { mutableStateOf(false) }
    var deleting by rememberSaveable { mutableStateOf(false) }

    Col {
        TopAppBar(
            title = { Text(g.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            navigationIcon = { IconButton(onClick = back) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver") } },
            actions = {
                IconButton(onClick = pick) { Icon(Icons.Outlined.PlaylistAdd, "Elegir apps") }
                if (app.qarentenapp.core.Shortcuts.canPin(ctx)) IconButton(onClick = { app.qarentenapp.core.Shortcuts.pinGroup(ctx, g) }) {
                    Icon(Icons.Outlined.AddToHomeScreen, "Agregar a pantalla de inicio")
                }
                IconButton(onClick = { editing = true }) { Icon(Icons.Outlined.Edit, "Editar grupo") }
                IconButton(onClick = { deleting = true }) { Icon(Icons.Outlined.Delete, "Borrar grupo") }
            },
        )
        LazyVerticalGrid(
            columns = GridCells.Adaptive(80.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                    Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                        GroupTile(g, allFrozen, 60.dp)
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                if (installed.isEmpty()) "Sin apps instaladas" else "$frozenCount de ${installed.size} congeladas",
                                fontSize = 19.sp, fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                            Text(
                                "${installed.size - frozenCount} activas" + if (pending.isNotEmpty()) " · ${pending.size} pendientes" else "",
                                fontSize = 13.sp, color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                        Button(onClick = { vm.setGroupFrozen(g, !allFrozen) }, enabled = installed.isNotEmpty()) {
                            Text(if (allFrozen) "Activar todo" else "Congelar todo")
                        }
                    }
                }
            }

            if (g.packages.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(24.dp)) {
                        Text("Este grupo está vacío.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        TextButton(onClick = pick) { Text("Elegir apps") }
                    }
                }
            }

            if (installed.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text("Apps del grupo", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp, start = 8.dp))
                }
                items(installed, key = { it.pkg }) { e ->
                    AppTile(vm, e.pkg, (if (e.isSystem && !e.protected) "⚠️ " else "") + e.label, e.frozen, pending = false,
                        onTap = { vm.toggle(e.pkg) },
                        menu = listOf(
                            "Abrir" to { vm.launch(e.pkg); Unit },
                            (if (e.frozen) "Activar" else "Congelar") to { vm.toggle(e.pkg); Unit },
                            "Ficha de la app" to { openApp(e.pkg) },
                            (if (e.pkg in config.favorites) "Quitar de favoritas" else "Agregar a favoritas") to { vm.toggleFavorite(e.pkg) },
                            "Acceso directo para abrirla" to { app.qarentenapp.core.Shortcuts.pinApp(ctx, e.pkg); Unit },
                            "Quitar del grupo" to { vm.removeFromGroup(g.id, e.pkg) },
                        ))
                }
            }

            if (pending.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.padding(top = 8.dp),
                    ) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Pendientes · ${pending.size}", fontWeight = FontWeight.SemiBold)
                                Text("Venían en tu backup y no están instaladas. Tocá una para buscarla en Play Store.",
                                    fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Spacer(Modifier.width(8.dp))
                            OutlinedButton(onClick = { vm.removePending(g.id) }) { Text("Quitar") }
                        }
                    }
                }
                items(pending, key = { "p:$it" }) { p ->
                    AppTile(vm, p, p.substringAfterLast('.'), frozen = false, pending = true,
                        onTap = { vm.openStore(p) },
                        menu = listOf(
                            "Buscar en Play Store" to { vm.openStore(p) },
                            "Quitar del grupo" to { vm.removeFromGroup(g.id, p) },
                        ))
                }
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    "Tocá una app para congelarla o activarla · Mantené presionado para más opciones",
                    fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                )
            }
        }
    }

    if (editing) GroupDialog("Editar grupo", g.name, g.icon, g.color, onDismiss = { editing = false }, groupApps = g.packages) { n, i, c ->
        editing = false; vm.updateGroup(g.id, n, i, c)
    }
    if (deleting) AlertDialog(
        onDismissRequest = { deleting = false },
        title = { Text("¿Borrar «${g.name}»?") },
        text = { Text("Se borra el grupo. Las apps quedan como están (congeladas o activas).") },
        confirmButton = { TextButton(onClick = { deleting = false; vm.deleteGroup(g.id); back() }) { Text("Borrar") } },
        dismissButton = { TextButton(onClick = { deleting = false }) { Text("Cancelar") } },
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppTile(
    vm: MainViewModel,
    pkg: String,
    label: String,
    frozen: Boolean,
    pending: Boolean,
    onTap: () -> Unit,
    menu: List<Pair<String, () -> Unit>>,
) {
    var open by rememberSaveable(pkg) { mutableStateOf(false) }
    Box {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .combinedClickable(onClick = onTap, onLongClick = { open = true })
                .padding(4.dp),
        ) {
            AppIcon(vm, pkg, 60.dp, frozen = frozen, pending = pending)
            Spacer(Modifier.size(6.dp))
            Text(label, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            menu.forEach { (text, action) ->
                DropdownMenuItem(text = { Text(text) }, onClick = { open = false; action() })
            }
        }
    }
}
