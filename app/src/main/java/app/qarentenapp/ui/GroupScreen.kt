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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.ui.graphics.Color
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
    // Grupo protegido: una sola autenticación al entrar; adentro nada vuelve a pedirse
    var allowed by androidx.compose.runtime.remember(id) { mutableStateOf(!app.qarentenapp.core.GroupLock.needsEntry(g)) }
    if (!allowed) {
        androidx.compose.runtime.LaunchedEffect(id) {
            vm.auth.ask("Desbloquear «${g.name}»", onDenied = back) { app.qarentenapp.core.GroupLock.enter(id); allowed = true }
        }
        androidx.compose.foundation.layout.Box(Modifier.fillMaxWidth())
        return
    }
    androidx.compose.runtime.DisposableEffect(id) { onDispose { app.qarentenapp.core.GroupLock.leave(id) } }
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
        LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
            // Encabezado: ícono con carámbanos y estado
            item {
                Row(Modifier.padding(horizontal = 20.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    GroupTile(g, allFrozen, 64.dp)
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (installed.isEmpty()) "Sin apps instaladas" else "$frozenCount de ${installed.size} congeladas",
                            fontSize = 20.sp, fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            "${installed.size - frozenCount} activas" + (if (pending.isNotEmpty()) " · ${pending.size} pendientes" else "") +
                                when (g.lockMode) { 1 -> " · 🔒 protegido"; 2 -> " · 🔒 «todas» protegido"; else -> "" },
                            fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            // Congelar todas (azul) / Descongelar todas (rojo)
            item {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = { vm.setGroupFrozen(g, true) },
                        enabled = installed.any { !it.frozen && !it.protected },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2F5E8E), contentColor = Color.White),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 12.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        Glyph("\ueb3b", 18.dp, Color.White, outlined = true)
                        Spacer(Modifier.width(6.dp))
                        Text("Congelar todas", fontSize = 14.sp, maxLines = 1)
                    }
                    Button(
                        onClick = { vm.setGroupFrozen(g, false) },
                        enabled = installed.any { it.frozen },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB3261E), contentColor = Color.White),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 12.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        Glyph("\uef55", 18.dp, Color.White, outlined = true)
                        Spacer(Modifier.width(6.dp))
                        Text("Descongelar todas", fontSize = 14.sp, maxLines = 1)
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }

            if (g.packages.isEmpty()) item {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(24.dp)) {
                    Text("Este grupo está vacío.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TextButton(onClick = pick) { Text("Elegir apps") }
                }
            }

            items(installed.sortedBy { it.label.lowercase() }, key = { it.pkg }) { e ->
                AppRow(
                    vm, e,
                    onOpen = { vm.launch(e.pkg) },
                    onToggle = { vm.toggle(e.pkg, g.id) },
                    menu = listOf(
                        "Abrir" to { vm.launch(e.pkg); Unit },
                        (if (e.frozen) "Descongelar" else "Congelar") to { vm.toggle(e.pkg, g.id); Unit },
                        "Ficha de la app" to { openApp(e.pkg) },
                        (if (e.pkg in config.favorites) "Quitar de favoritas" else "Agregar a favoritas") to { vm.toggleFavorite(e.pkg) },
                        "Acceso directo para abrirla" to { app.qarentenapp.core.Shortcuts.pinApp(ctx, e.pkg); Unit },
                        "Quitar del grupo" to { vm.removeFromGroup(g.id, e.pkg) },
                    ),
                )
            }

            if (pending.isNotEmpty()) {
                item {
                    Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 20.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Pendientes · ${pending.size}", fontWeight = FontWeight.SemiBold)
                            Text("Venían en tu backup y no están instaladas. Tocá una para buscarla en Play Store.",
                                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        OutlinedButton(onClick = { vm.removePending(g.id) }) { Text("Quitar") }
                    }
                }
                items(pending, key = { "p:$it" }) { p ->
                    PendingRow(vm, p, onTap = { vm.openStore(p) }, onRemove = { vm.removeFromGroup(g.id, p) })
                }
            }

            item {
                Text(
                    "Tocá una app para abrirla · Tocá ❄️ o 🔥 para congelarla o descongelarla · Mantené presionado para más opciones",
                    fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(24.dp),
                )
            }
        }
    }

    if (editing) GroupDialog("Editar grupo", g.name, g.icon, g.color, onDismiss = { editing = false }, groupApps = g.packages, vm = vm, initialLock = g.lockMode) { n, i, c, l ->
        editing = false; vm.updateGroup(g.id, n, i, c, l)
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

/** Fila de app: tocar la abre, ❄️/🔥 la congela o descongela, mantener presionado muestra opciones. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppRow(
    vm: MainViewModel,
    e: app.qarentenapp.data.AppEntry,
    onOpen: () -> Unit,
    onToggle: () -> Unit,
    menu: List<Pair<String, () -> Unit>>,
) {
    var open by rememberSaveable(e.pkg) { mutableStateOf(false) }
    val system = e.isSystem && !e.protected
    Box {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
                    .combinedClickable(enabled = true, onClick = { if (e.hasLauncher) onOpen() }, onLongClick = { open = true })
                    .padding(start = 16.dp, top = 10.dp, bottom = 10.dp),
            ) {
                AppIcon(vm, e.pkg, 46.dp, frozen = e.frozen, badge = false)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text((if (system) "⚠️ " else "") + e.label, fontSize = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        when {
                            e.protected -> "Protegida"
                            system && e.frozen -> "Congelada · app del sistema"
                            system -> "Activa · app del sistema, cuidado"
                            e.frozen -> "Congelada"
                            else -> "Activa"
                        },
                        fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            TextButton(onClick = onToggle, enabled = !e.protected, modifier = Modifier.padding(end = 6.dp).size(60.dp)) {
                if (e.protected) Icon(Icons.Outlined.Lock, "Protegida", tint = MaterialTheme.colorScheme.outline)
                else Text(if (e.frozen) "❄️" else "🔥", fontSize = 28.sp)
            }
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            menu.forEach { (text, action) -> DropdownMenuItem(text = { Text(text) }, onClick = { open = false; action() }) }
        }
        HorizontalDivider(Modifier.align(Alignment.BottomStart).padding(start = 76.dp), color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PendingRow(vm: MainViewModel, pkg: String, onTap: () -> Unit, onRemove: () -> Unit) {
    var open by rememberSaveable(pkg) { mutableStateOf(false) }
    Box {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().combinedClickable(onClick = onTap, onLongClick = { open = true })
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            AppIcon(vm, pkg, 46.dp, frozen = false, pending = true)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(pkg.substringAfterLast('.'), fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("Pendiente de instalar · $pkg", fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(text = { Text("Buscar en Play Store") }, onClick = { open = false; onTap() })
            DropdownMenuItem(text = { Text("Quitar del grupo") }, onClick = { open = false; onRemove() })
        }
    }
}
