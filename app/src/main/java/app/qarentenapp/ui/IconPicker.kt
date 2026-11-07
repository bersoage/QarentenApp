package app.qarentenapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.qarentenapp.core.Repo

/**
 * Selector de íconos para grupos: catálogo por categorías con buscador,
 * o el ícono de una app (ideal para marcas: Google, Adobe, Nikon…).
 */
@Composable
fun IconPickerDialog(current: String, groupApps: List<String>, onDismiss: () -> Unit, onPick: (String) -> Unit) {
    var query by remember { mutableStateOf("") }
    var allApps by remember { mutableStateOf(false) }
    val apps by Repo.apps.collectAsState()
    val icons = searchIcons(query)
    val appChoices = remember(apps, groupApps, allApps, query) {
        val q = query.trim().lowercase()
        val base = if (allApps) apps.filter { it.hasLauncher } else apps.filter { it.pkg in groupApps }
        base.filter { q.isEmpty() || it.label.lowercase().contains(q) }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(8.dp)) {
                    IconButton(onClick = onDismiss) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver") }
                    OutlinedTextField(
                        query, { query = it }, singleLine = true,
                        placeholder = { Text("Buscar ícono (ej.: banco, música, gym)") },
                        leadingIcon = { Icon(Icons.Outlined.Search, null) },
                        modifier = Modifier.weight(1f).padding(end = 8.dp),
                    )
                }
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(72.dp),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // Ícono de una app
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Ícono de una app", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                            TextButton(onClick = { allApps = !allApps }) { Text(if (allApps) "Solo las del grupo" else "Ver todas") }
                        }
                    }
                    if (appChoices.isEmpty()) item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            if (allApps) "No hay apps que coincidan." else "Este grupo todavía no tiene apps. Tocá «Ver todas».",
                            fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    items(appChoices.take(if (allApps) 400 else 40), key = { "a:" + it.pkg }) { a ->
                        val key = "app:" + a.pkg
                        Cell(selected = current == key, label = a.label, onClick = { onPick(key) }) {
                            GroupGlyph(key, 40.dp, MaterialTheme.colorScheme.primary)
                        }
                    }
                    // Catálogo
                    icons.groupBy { it.category }.forEach { (cat, list) ->
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Text(cat, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 12.dp))
                        }
                        items(list, key = { it.key }) { d ->
                            Cell(selected = current == d.key, label = d.label, onClick = { onPick(d.key) }) {
                                Glyph(d.glyph, 34.dp, MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                        }
                    }
                    if (icons.isEmpty()) item(span = { GridItemSpan(maxLineSpan) }) {
                        Text("No hay íconos con ese nombre.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun Cell(selected: Boolean, label: String, onClick: () -> Unit, content: @Composable () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick).padding(4.dp),
    ) {
        Box(
            Modifier.size(56.dp).clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.primaryContainer)
                .then(if (selected) Modifier.border(3.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp)) else Modifier),
            contentAlignment = Alignment.Center,
        ) { content() }
        Text(label, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth())
    }
}
