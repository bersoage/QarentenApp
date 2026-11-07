package app.qarentenapp.ui

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.qarentenapp.core.Mode
import app.qarentenapp.data.Group
import java.text.DateFormat
import java.util.Date

@Composable
fun buildBrand() = buildAnnotatedString {
    withStyle(SpanStyle(fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)) { append("Qarenten") }
    withStyle(SpanStyle(fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)) { append("App") }
}

@Composable
fun GroupsScreen(vm: MainViewModel, open: (String) -> Unit) {
    val config by vm.config.collectAsState()
    val apps by vm.apps.collectAsState()
    val mode by vm.mode.collectAsState()
    val map = remember(apps) { apps.associateBy { it.pkg } }
    var creating by rememberSaveable { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Column(Modifier.padding(horizontal = 8.dp)) {
                    BrandMark()
                    Spacer(Modifier.height(4.dp))
                    Text("Grupos", fontSize = 32.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    StatusChips(vm, mode, config.settings.lastBackupAt, config.settings.autoBackupTree != null)
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
            items(config.groups, key = { it.id }) { g ->
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
        title = "Nuevo grupo", initialName = "", initialIcon = "folder",
        onDismiss = { creating = false },
        onSave = { name, icon -> creating = false; open(vm.createGroup(name, icon)) },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StatusChips(vm: MainViewModel, mode: Mode?, lastBackup: Long, auto: Boolean) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        val (label, color) = when (mode) {
            null -> "Detectando…" to MaterialTheme.colorScheme.outline
            Mode.ROOT -> "Root activo" to Color(0xFF2E7D3A)
            Mode.SHIZUKU -> "Shizuku activo" to Color(0xFF2E7D3A)
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

@Composable
private fun GroupCard(vm: MainViewModel, g: Group, map: Map<String, app.qarentenapp.data.AppEntry>, onOpen: () -> Unit) {
    val installed = g.packages.mapNotNull { map[it] }
    val frozen = installed.count { it.frozen }
    val pending = g.packages.size - installed.size
    val allFrozen = installed.isNotEmpty() && installed.filterNot { it.protected }.all { it.frozen }

    Card(
        onClick = onOpen,
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(48.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(groupIcon(g.icon), null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
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
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    g.packages.take(8).forEach { p ->
                        val e = map[p]
                        AppIcon(vm, p, 30.dp, frozen = e?.frozen == true, badge = false, pending = e == null)
                    }
                    if (g.packages.size > 8) {
                        Text("+${g.packages.size - 8}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.align(Alignment.CenterVertically))
                    }
                }
            }
        }
    }
}

/** Diálogo para crear o editar un grupo: nombre e ícono. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GroupDialog(
    title: String,
    initialName: String,
    initialIcon: String,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(initialName) }
    var icon by rememberSaveable { mutableStateOf(initialIcon) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
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
            }
        },
        confirmButton = { TextButton(enabled = name.isNotBlank(), onClick = { onSave(name, icon) }) { Text("Guardar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}
