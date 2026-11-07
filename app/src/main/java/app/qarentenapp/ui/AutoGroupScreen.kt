package app.qarentenapp.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.qarentenapp.core.AutoGroup
import app.qarentenapp.core.Proposal
import app.qarentenapp.data.Group

/** «Auto agrupar»: propuesta de grupos por rubro para revisar antes de crear nada. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoGroupScreen(vm: MainViewModel, done: () -> Unit) {
    val apps by vm.apps.collectAsState()
    val config by vm.config.collectAsState()
    val proposals = remember(apps) {
        AutoGroup.propose(apps, config.groups.flatMap { it.packages }.toSet())
    }
    // Estado editable: nombre, grupo incluido y apps tildadas
    val names = remember(proposals) { mutableStateListOf(*proposals.map { it.name }.toTypedArray()) }
    val included = remember(proposals) { mutableStateListOf(*proposals.map { true }.toTypedArray()) }
    val unchecked = remember(proposals) { mutableStateMapOf<String, Boolean>() }

    val chosen = proposals.indices.filter { included[it] }.map { i ->
        val p = proposals[i]
        Proposal(names[i].trim().ifEmpty { p.name }, p.icon, p.color, p.packages.filter { unchecked[it] != true })
    }.filter { it.packages.isNotEmpty() }

    Column {
        TopAppBar(
            title = { Text("Auto agrupar") },
            navigationIcon = { IconButton(onClick = done) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Cancelar") } },
        )
        LazyColumn(
            Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    if (proposals.isEmpty()) "No encontré apps para agrupar: puede que ya estén todas en grupos o que no las reconozca. Podés armar los grupos a mano."
                    else "Esto es una propuesta. Destildá lo que no quieras y cambiá los nombres si querés. No se congela nada.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            items(proposals.indices.toList()) { i ->
                val p = proposals[i]
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = included[i], onCheckedChange = { included[i] = it })
                            GroupTile(Group("auto-$i", names[i], p.icon, p.color), frozen = true, size = 44.dp)
                            Spacer(Modifier.width(10.dp))
                            OutlinedTextField(
                                names[i], { names[i] = it.take(40) }, singleLine = true,
                                modifier = Modifier.weight(1f), enabled = included[i],
                            )
                        }
                        if (included[i]) p.packages.mapNotNull { pkg -> apps.firstOrNull { it.pkg == pkg } }.forEach { e ->
                            val pkg = e.pkg
                            val on = unchecked[pkg] != true
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth().clickable { unchecked[pkg] = on }.padding(start = 8.dp, top = 2.dp, bottom = 2.dp),
                            ) {
                                Checkbox(checked = on, onCheckedChange = { unchecked[pkg] = !it })
                                AppIcon(vm, pkg, 32.dp, frozen = e.frozen, badge = false)
                                Spacer(Modifier.width(10.dp))
                                Text(e.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
            item {
                Text(
                    "Las apps que no reconozco quedan afuera: las podés sumar a mano. Si ya tenés un grupo con el mismo nombre, se agregan ahí.",
                    fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Button(
            onClick = { vm.applyAutoGroups(chosen); done() },
            enabled = chosen.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        ) {
            Text(if (chosen.size == 1) "Crear 1 grupo" else "Crear ${chosen.size} grupos", fontWeight = FontWeight.SemiBold)
        }
    }
}
