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
import androidx.compose.material3.FilterChipDefaults
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
    USER("Usuario"), UNGROUPED("Desagrupadas"), FROZEN("Congeladas"), ACTIVE("Activas"), SYSTEM("Sistema")
}

/** Minúsculas y sin tildes, para buscar sin importar cómo se escribió. */
fun normSearch(t: String): String =
    java.text.Normalizer.normalize(t, java.text.Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "").lowercase()

/** Nombres de los grupos que contienen la app (menos [except]). */
fun groupNamesOf(groups: List<app.qarentenapp.data.Group>, pkg: String, except: String? = null): List<String> =
    groups.filter { pkg in it.packages && it.id != except }.map { it.name }

/** "Grupos: Shoping, Redes +2" (muestra hasta 3 nombres). */
fun groupsText(names: List<String>): String? {
    if (names.isEmpty()) return null
    val shown = names.take(3).joinToString(", ")
    return "Grupos: $shown" + if (names.size > 3) " +${names.size - 3}" else ""
}

@Composable
private fun rememberFiltered(vm: MainViewModel, query: String, filters: Set<Filter>, perms: Set<String>): List<AppEntry> {
    val apps by vm.apps.collectAsState()
    val config by vm.config.collectAsState()
    val permMap by vm.perms.collectAsState()
    val s = config.settings
    val grouped = remember(config.groups) { config.groups.flatMap { it.packages }.toSet() }
    return remember(apps, query, filters, s.showServices, grouped, perms, permMap, config.groups) {
        val q = normSearch(query.trim())
        // Apps que están en un grupo cuyo nombre coincide con lo buscado
        val inMatchingGroup = if (q.isEmpty()) emptySet() else config.groups.filter { normSearch(it.name).contains(q) }.flatMap { it.packages }.toSet()
        val wanted = PermCategory.entries.filter { it.name in perms }
        // Cada botón se prende y se apaga solo. Dentro de una misma fila se suman; entre filas se combinan.
        val user = Filter.USER in filters; val system = Filter.SYSTEM in filters
        val frozen = Filter.FROZEN in filters; val active = Filter.ACTIVE in filters
        val ungrouped = Filter.UNGROUPED in filters
        apps.filter { a ->
            val sysVisible = a.isSystem && (a.hasLauncher || s.showServices)
            val typeOk = when {
                user == system -> !a.isSystem || sysVisible
                user -> !a.isSystem
                else -> sysVisible
            }
            val stateOk = when {
                frozen == active -> true
                frozen -> a.frozen
                else -> !a.frozen
            }
            typeOk && stateOk && (!ungrouped || a.pkg !in grouped) &&
                (q.isEmpty() || normSearch(a.label).contains(q) || a.pkg.contains(q) || a.pkg in inMatchingGroup) &&
                (wanted.isEmpty() || permMap?.get(a.pkg)?.let { p -> wanted.any { c -> p.any { it in c.perms } } } == true)
        }
    }
}

/** Resumen de la combinación elegida: "Usuario · activas · sin grupo". */
private fun summary(filters: Set<Filter>): String {
    val parts = mutableListOf<String>()
    val user = Filter.USER in filters; val system = Filter.SYSTEM in filters
    parts += when { user && !system -> "Usuario"; system && !user -> "Sistema"; else -> "Usuario y sistema" }
    val frozen = Filter.FROZEN in filters; val active = Filter.ACTIVE in filters
    if (frozen != active) parts += if (frozen) "congeladas" else "activas"
    if (Filter.UNGROUPED in filters) parts += "sin grupo"
    return parts.joinToString(" · ")
}

@Composable
private fun SearchAndFilters(
    vm: MainViewModel,
    query: String, onQuery: (String) -> Unit,
    filters: Set<Filter>, onFilters: (Set<Filter>) -> Unit,
    permCount: Int, onPerms: () -> Unit,
    count: Int,
) {
    val config by vm.config.collectAsState()
    var warn by remember { mutableStateOf(false) }
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
            Filter.entries.forEach { f ->
                if (f == Filter.SYSTEM) {
                    val red = androidx.compose.ui.graphics.Color(0xFFB3261E)
                    FilterChip(
                        selected = f in filters,
                        onClick = {
                            if (f !in filters && !config.settings.systemFilterWarned) warn = true
                            else onFilters(if (f in filters) filters - f else filters + f)
                        },
                        label = { Text(f.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            labelColor = androidx.compose.ui.graphics.Color(0xFFFF8C82),
                            selectedContainerColor = red,
                            selectedLabelColor = androidx.compose.ui.graphics.Color.White,
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true, selected = f in filters,
                            borderColor = red, selectedBorderColor = red, borderWidth = 1.5.dp,
                        ),
                    )
                } else FilterChip(
                    selected = f in filters,
                    onClick = { onFilters(if (f in filters) filters - f else filters + f) },
                    label = { Text(f.label) },
                )
            }
        }
        Text(
            summary(filters) + (if (permCount > 0) " · $permCount ${if (permCount == 1) "permiso" else "permisos"}" else "") + " — $count",
            fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    if (warn) {
        var dontShow by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { warn = false },
            title = { Text("⚠️ Apps del sistema") },
            text = {
                Column {
                    Text("Estas apps son parte de Android y del celular. Congelar la equivocada puede hacer que algo deje de funcionar: notificaciones, cámara, llamadas, el teclado o hasta el arranque del celular.")
                    Text("Las esenciales están protegidas y no se pueden congelar, pero con el resto tené cuidado: congelá solo las que sepas para qué sirven.",
                        modifier = Modifier.padding(top = 8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp).clickable { dontShow = !dontShow }) {
                        Checkbox(checked = dontShow, onCheckedChange = { dontShow = it })
                        Text("No volver a mostrar este aviso", fontSize = 14.sp)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    warn = false
                    if (dontShow) vm.updateSettings { it.copy(systemFilterWarned = true) }
                    onFilters(filters + Filter.SYSTEM)
                }) { Text("Entiendo, mostrar") }
            },
            dismissButton = { TextButton(onClick = { warn = false }) { Text("Cancelar") } },
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
    var filters by rememberSaveable { mutableStateOf(setOf(Filter.USER)) }
    var perms by rememberSaveable { mutableStateOf(setOf<String>()) }
    var permsDialog by rememberSaveable { mutableStateOf(false) }
    val config by vm.config.collectAsState()
    val list = rememberFiltered(vm, query, filters, perms)

    Column {
        Text("Todas las apps", fontSize = 28.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 24.dp, top = 24.dp, bottom = 12.dp))
        SearchAndFilters(vm, query, { query = it }, filters, { filters = it }, perms.size, count = list.size,
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
                        AppLabel(a, config.settings.showPackageName, a.pkg in config.favorites, Modifier.weight(1f), groupNamesOf(config.groups, a.pkg))
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
fun AppLabel(a: AppEntry, showPkg: Boolean, favorite: Boolean, modifier: Modifier, groups: List<String> = emptyList()) {
    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(a.label, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f, fill = false))
            if (favorite) Icon(Icons.Filled.Star, "Favorita", tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 4.dp).size(14.dp))
        }
        val parts = buildList {
            if (a.protected) add("Protegida")
            else if (a.isSystem) add(if (a.hasLauncher) "⚠️ App del sistema · cuidado" else "⚠️ Servicio del sistema · cuidado")
            if (a.frozen) add("Congelada") else if (groups.isNotEmpty() && !a.isSystem) add("Activa")
            groupsText(groups)?.let { add(it) }
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
    var filters by rememberSaveable { mutableStateOf(setOf(Filter.USER)) }
    var perms by rememberSaveable { mutableStateOf(setOf<String>()) }
    var permsDialog by rememberSaveable { mutableStateOf(false) }
    val list = rememberFiltered(vm, query, filters, perms)

    Column {
        TopAppBar(
            title = { Text("Apps de «${g.name}»", maxLines = 1, overflow = TextOverflow.Ellipsis) },
            navigationIcon = { IconButton(onClick = done) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Cancelar") } },
            actions = {
                TextButton(onClick = { vm.setGroupApps(groupId, selected); done() }) { Text("Listo (${selected.size})") }
            },
        )
        SearchAndFilters(vm, query, { query = it }, filters, { filters = it }, perms.size, count = list.size,
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
                    AppLabel(a, config.settings.showPackageName, a.pkg in config.favorites, Modifier.weight(1f), groupNamesOf(config.groups, a.pkg))
                    Checkbox(checked = checked, onCheckedChange = { selected = if (it) selected + a.pkg else selected - a.pkg })
                }
            }
        }
    }
    if (permsDialog) PermsDialog(perms, onDismiss = { permsDialog = false }) { perms = it; permsDialog = false }
}
