package app.qarentenapp.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Search
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.qarentenapp.data.AppEntry

private enum class Filter(val label: String) { ALL("Todas"), USER("Usuario"), SYSTEM("Sistema"), FROZEN("Congeladas") }

@Composable
private fun rememberFiltered(vm: MainViewModel, query: String, filter: Filter): List<AppEntry> {
    val apps by vm.apps.collectAsState()
    val config by vm.config.collectAsState()
    val showSystem = config.settings.showSystem
    return remember(apps, query, filter, showSystem) {
        val q = query.trim().lowercase()
        apps.filter { a ->
            (showSystem || !a.isSystem || a.frozen) &&
                when (filter) {
                    Filter.ALL -> true
                    Filter.USER -> !a.isSystem
                    Filter.SYSTEM -> a.isSystem
                    Filter.FROZEN -> a.frozen
                } &&
                (q.isEmpty() || a.label.lowercase().contains(q) || a.pkg.contains(q))
        }
    }
}

@Composable
private fun SearchAndFilters(query: String, onQuery: (String) -> Unit, filter: Filter, onFilter: (Filter) -> Unit, showSystem: Boolean) {
    Column(Modifier.padding(horizontal = 16.dp)) {
        OutlinedTextField(
            value = query, onValueChange = onQuery, singleLine = true,
            leadingIcon = { Icon(Icons.Outlined.Search, null) },
            placeholder = { Text("Buscar app") },
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
            Filter.entries.filter { showSystem || it != Filter.SYSTEM }.forEach { f ->
                FilterChip(selected = filter == f, onClick = { onFilter(f) }, label = { Text(f.label) })
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppsScreen(vm: MainViewModel) {
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf(Filter.ALL) }
    val config by vm.config.collectAsState()
    val list = rememberFiltered(vm, query, filter)

    Column {
        Text("Todas las apps", fontSize = 28.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 24.dp, top = 24.dp, bottom = 12.dp))
        SearchAndFilters(query, { query = it }, filter, { filter = it }, config.settings.showSystem)
        LazyColumn {
            items(list, key = { it.pkg }) { a ->
                var menu by remember(a.pkg) { mutableStateOf(false) }
                Box {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(onClick = { vm.toggle(a.pkg) }, onLongClick = { menu = true })
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    ) {
                        AppIcon(vm, a.pkg, 44.dp, frozen = a.frozen)
                        Spacer(Modifier.width(14.dp))
                        AppLabel(a, config.settings.showPackageName, Modifier.weight(1f))
                        if (a.protected) Icon(Icons.Outlined.Lock, "Protegida", tint = MaterialTheme.colorScheme.outline)
                        else Switch(
                            checked = a.frozen, onCheckedChange = { vm.toggle(a.pkg) },
                            thumbContent = if (a.frozen) { { Icon(Icons.Filled.AcUnit, null, Modifier.padding(2.dp)) } } else null,
                        )
                    }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(text = { Text("Abrir") }, onClick = { menu = false; vm.launch(a.pkg) })
                        config.groups.forEach { g ->
                            val inGroup = a.pkg in g.packages
                            DropdownMenuItem(
                                text = { Text(if (inGroup) "Quitar de «${g.name}»" else "Agregar a «${g.name}»") },
                                onClick = {
                                    menu = false
                                    if (inGroup) vm.removeFromGroup(g.id, a.pkg) else vm.setGroupApps(g.id, g.packages + a.pkg)
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppLabel(a: AppEntry, showPkg: Boolean, modifier: Modifier) {
    Column(modifier) {
        Text(a.label, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium)
        val parts = buildList {
            if (a.isSystem) add("Sistema")
            if (a.protected) add("Protegida")
            if (a.frozen) add("Congelada")
            if (showPkg) add(a.pkg)
        }
        if (parts.isNotEmpty()) Text(
            parts.joinToString(" · "), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
            color = if (a.isSystem) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Elegir qué apps forman parte de un grupo. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PickAppsScreen(vm: MainViewModel, groupId: String, done: () -> Unit) {
    val config by vm.config.collectAsState()
    val g = config.groups.firstOrNull { it.id == groupId }
    if (g == null) { androidx.compose.runtime.LaunchedEffect(Unit) { done() }; return }
    var selected by rememberSaveable { mutableStateOf(g.packages.toList()) }
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf(Filter.ALL) }
    val list = rememberFiltered(vm, query, filter)

    Column {
        TopAppBar(
            title = { Text("Apps de «${g.name}»", maxLines = 1, overflow = TextOverflow.Ellipsis) },
            navigationIcon = { IconButton(onClick = done) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Cancelar") } },
            actions = {
                TextButton(onClick = { vm.setGroupApps(groupId, selected); done() }) { Text("Listo (${selected.size})") }
            },
        )
        SearchAndFilters(query, { query = it }, filter, { filter = it }, config.settings.showSystem)
        LazyColumn {
            items(list, key = { it.pkg }) { a ->
                val checked = a.pkg in selected
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickableCompat { selected = if (checked) selected - a.pkg else selected + a.pkg }
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                ) {
                    AppIcon(vm, a.pkg, 40.dp, frozen = a.frozen)
                    Spacer(Modifier.width(14.dp))
                    AppLabel(a, config.settings.showPackageName, Modifier.weight(1f))
                    Checkbox(checked = checked, onCheckedChange = { selected = if (it) selected + a.pkg else selected - a.pkg })
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
private fun Modifier.combinedClickableCompat(onClick: () -> Unit) = this.combinedClickable(onClick = onClick)
