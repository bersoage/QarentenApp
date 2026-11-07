package app.qarentenapp.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.qarentenapp.core.PermCategory
import app.qarentenapp.core.Shortcuts
import app.qarentenapp.data.AppEntry

private enum class Filter(val label: String) {
    ALL("Todas"), USER("Usuario"), SYSTEM("Sistema"), FROZEN("Congeladas"), ACTIVE("Activas"), FAVORITES("Favoritas")
}

@Composable
private fun rememberFiltered(vm: MainViewModel, query: String, filter: Filter, perms: Set<String>): List<AppEntry> {
    val apps by vm.apps.collectAsState()
    val config by vm.config.collectAsState()
    val permMap by vm.perms.collectAsState()
    val s = config.settings
    val favorites = config.favorites
    return remember(apps, query, filter, s.showSystem, s.showServices, favorites, perms, permMap) {
        val q = query.trim().lowercase()
        val wanted = PermCategory.entries.filter { it.name in perms }
        apps.filter { a ->
            val visible = when {
                !a.isSystem -> true
                a.frozen -> true
                !s.showSystem -> false
                !a.hasLauncher -> s.showServices
                else -> true
            }
            visible &&
                when (filter) {
                    Filter.ALL -> true
                    Filter.USER -> !a.isSystem
                    Filter.SYSTEM -> a.isSystem
                    Filter.FROZEN -> a.frozen
                    Filter.ACTIVE -> !a.frozen
                    Filter.FAVORITES -> a.pkg in favorites
                } &&
                (q.isEmpty() || a.label.lowercase().contains(q) || a.pkg.contains(q)) &&
                (wanted.isEmpty() || permMap?.get(a.pkg)?.let { p -> wanted.any { c -> p.any { it in c.perms } } } == true)
        }
    }
}

@Composable
private fun SearchAndFilters(
    query: String, onQuery: (String) -> Unit,
    filter: Filter, onFilter: (Filter) -> Unit,
    showSystem: Boolean, permCount: Int, onPerms: () -> Unit,
) {
    Column(Modifier.padding(horizontal = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = query, onValueChange = onQuery, singleLine = true,
                leadingIcon = { Icon(Icons.Outlined.Search, null) },
                placeholder = { Text("Buscar app") },
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onPerms) {
                Icon(Icons.Outlined.FilterList, "Filtrar por permisos",
                    tint = if (permCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(vertical = 8.dp).horizontalScroll(rememberScrollState()),
        ) {
            Filter.entries.filter { showSystem || it != Filter.SYSTEM }.forEach { f ->
                FilterChip(selected = filter == f, onClick = { onFilter(f) }, label = { Text(f.label) })
            }
        }
        if (permCount > 0) Text(
            "Filtrando por $permCount ${if (permCount == 1) "permiso" else "permisos"}",
            fontSize = 12.sp, color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun PermsDialog(selected: Set<String>, onDismiss: () -> Unit, onApply: (Set<String>) -> Unit) {
    var sel by remember { mutableStateOf(selected) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Filtrar por permisos") },
        text = {
            LazyColumn {
                item { Text("Mostrar solo las apps que usan:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                items(PermCategory.entries) { c ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clickable { sel = if (c.name in sel) sel - c.name else sel + c.name },
                    ) {
                        Checkbox(checked = c.name in sel, onCheckedChange = { sel = if (it) sel + c.name else sel - c.name })
                        Text(c.label)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onApply(sel) }) { Text("Aplicar") } },
        dismissButton = { TextButton(onClick = { onApply(emptySet()) }) { Text("Quitar filtro") } },
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppsScreen(vm: MainViewModel, openApp: (String) -> Unit) {
    val ctx = LocalContext.current
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf(Filter.ALL) }
    var perms by rememberSaveable { mutableStateOf(setOf<String>()) }
    var permsDialog by rememberSaveable { mutableStateOf(false) }
    val config by vm.config.collectAsState()
    val list = rememberFiltered(vm, query, filter, perms)

    Column {
        Text("Todas las apps", fontSize = 28.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 24.dp, top = 24.dp, bottom = 12.dp))
        SearchAndFilters(query, { query = it }, filter, { filter = it }, config.settings.showSystem, perms.size,
            onPerms = { vm.loadPermissions(); permsDialog = true })
        LazyColumn {
            items(list, key = { it.pkg }) { a ->
                var menu by remember(a.pkg) { mutableStateOf(false) }
                Box {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(onClick = { openApp(a.pkg) }, onLongClick = { menu = true })
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    ) {
                        AppIcon(vm, a.pkg, 44.dp, frozen = a.frozen)
                        Spacer(Modifier.width(14.dp))
                        AppLabel(a, config.settings.showPackageName, a.pkg in config.favorites, Modifier.weight(1f))
                        if (a.protected) Icon(Icons.Outlined.Lock, "Protegida", tint = MaterialTheme.colorScheme.outline)
                        else Switch(
                            checked = a.frozen, onCheckedChange = { vm.toggle(a.pkg) },
                            thumbContent = if (a.frozen) { { Icon(Icons.Filled.AcUnit, null, Modifier.padding(2.dp)) } } else null,
                        )
                    }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(text = { Text("Abrir") }, onClick = { menu = false; vm.launch(a.pkg) })
                        DropdownMenuItem(text = { Text("Ficha de la app") }, onClick = { menu = false; openApp(a.pkg) })
                        DropdownMenuItem(
                            text = { Text(if (a.pkg in config.favorites) "Quitar de favoritas" else "Agregar a favoritas") },
                            onClick = { menu = false; vm.toggleFavorite(a.pkg) })
                        if (Shortcuts.canPin(ctx) && a.hasLauncher) DropdownMenuItem(
                            text = { Text("Acceso directo para abrirla") },
                            onClick = { menu = false; Shortcuts.pinApp(ctx, a.pkg) })
                        config.groups.forEach { g ->
                            val inGroup = a.pkg in g.packages
                            DropdownMenuItem(
                                text = { Text(if (inGroup) "Quitar de «${g.name}»" else "Agregar a «${g.name}»") },
                                onClick = {
                                    menu = false
                                    if (inGroup) vm.removeFromGroup(g.id, a.pkg) else vm.addToGroup(g.id, a.pkg)
                                },
                            )
                        }
                    }
                }
            }
        }
    }
    if (permsDialog) PermsDialog(perms, onDismiss = { permsDialog = false }) { perms = it; permsDialog = false }
}

@Composable
fun AppLabel(a: AppEntry, showPkg: Boolean, favorite: Boolean, modifier: Modifier) {
    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(a.label, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f, fill = false))
            if (favorite) Icon(Icons.Filled.Star, "Favorita", tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 4.dp).size(14.dp))
        }
        val parts = buildList {
            if (a.protected) add("Protegida")
            else if (a.isSystem) add(if (a.hasLauncher) "⚠️ App del sistema · cuidado" else "⚠️ Servicio del sistema · cuidado")
            if (a.frozen) add("Congelada")
            if (showPkg) add(a.pkg)
        }
        if (parts.isNotEmpty()) Text(
            parts.joinToString(" · "), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
            color = if (a.isSystem) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Elegir qué apps forman parte de un grupo (también sirve como asistente al crear uno). */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PickAppsScreen(vm: MainViewModel, groupId: String, done: () -> Unit) {
    val config by vm.config.collectAsState()
    val g = config.groups.firstOrNull { it.id == groupId }
    if (g == null) { androidx.compose.runtime.LaunchedEffect(Unit) { done() }; return }
    var selected by rememberSaveable { mutableStateOf(g.packages.toList()) }
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf(Filter.ALL) }
    var perms by rememberSaveable { mutableStateOf(setOf<String>()) }
    var permsDialog by rememberSaveable { mutableStateOf(false) }
    val list = rememberFiltered(vm, query, filter, perms)

    Column {
        TopAppBar(
            title = { Text("Apps de «${g.name}»", maxLines = 1, overflow = TextOverflow.Ellipsis) },
            navigationIcon = { IconButton(onClick = done) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Cancelar") } },
            actions = {
                TextButton(onClick = { vm.setGroupApps(groupId, selected); done() }) { Text("Listo (${selected.size})") }
            },
        )
        SearchAndFilters(query, { query = it }, filter, { filter = it }, config.settings.showSystem, perms.size,
            onPerms = { vm.loadPermissions(); permsDialog = true })
        LazyColumn {
            items(list, key = { it.pkg }) { a ->
                val checked = a.pkg in selected
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selected = if (checked) selected - a.pkg else selected + a.pkg }
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                ) {
                    AppIcon(vm, a.pkg, 40.dp, frozen = a.frozen)
                    Spacer(Modifier.width(14.dp))
                    AppLabel(a, config.settings.showPackageName, a.pkg in config.favorites, Modifier.weight(1f))
                    Checkbox(checked = checked, onCheckedChange = { selected = if (it) selected + a.pkg else selected - a.pkg })
                }
            }
        }
    }
    if (permsDialog) PermsDialog(perms, onDismiss = { permsDialog = false }) { perms = it; permsDialog = false }
}
