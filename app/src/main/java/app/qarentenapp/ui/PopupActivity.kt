package app.qarentenapp.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import app.qarentenapp.core.Origin
import app.qarentenapp.core.Repo
import kotlinx.coroutines.launch

/**
 * Ventanas flotantes:
 *  - Cajón de cuarentena: las apps congeladas, para abrirlas de un toque.
 *  - Menú rápido: favoritas, activas y grupos, para congelar o activar sin abrir la app.
 *  - Grupo: las apps de un grupo, para abrirlas (widget con "lanzamiento directo").
 */
class PopupActivity : ComponentActivity() {

    companion object {
        const val EXTRA_MODE = "mode"
        const val EXTRA_ID = "id"
        const val MODE_DRAWER = "drawer"
        const val MODE_QUICK = "quick"
        const val MODE_GROUP = "group"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Repo.init(this)
        val mode = intent.getStringExtra(EXTRA_MODE) ?: MODE_DRAWER
        val groupId = intent.getStringExtra(EXTRA_ID)
        val s = Repo.config.value.settings
        lifecycleScope.launch { if (Repo.apps.value.isEmpty()) Repo.refresh() }
        setContent {
            QTheme {
                var unlocked by remember { mutableStateOf(!(s.protectionEnabled && s.passwordHash != null)) }
                if (!unlocked) PasswordDialog(onCancel = { finish() }, onOk = { unlocked = true })
                else PopupContent(mode, groupId, fullscreen = mode == MODE_DRAWER && s.drawerFullscreen, close = { finish() })
            }
        }
    }

    @OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
    @Composable
    private fun PopupContent(mode: String, groupId: String?, fullscreen: Boolean, close: () -> Unit) {
        val vm: MainViewModel = viewModel()
        val apps by Repo.apps.collectAsState()
        val config by Repo.config.collectAsState()
        val map = apps.associateBy { it.pkg }

        val title: String
        val list: List<String>
        when (mode) {
            MODE_GROUP -> {
                val g = config.groups.firstOrNull { it.id == groupId }
                title = g?.name ?: "Grupo"
                list = g?.packages?.filter { it in map } ?: emptyList()
            }
            MODE_QUICK -> {
                title = "Menú rápido"
                list = (config.favorites + config.active).distinct().filter { it in map }
            }
            else -> {
                title = "Cajón de cuarentena"
                list = apps.filter { it.frozen && it.hasLauncher }.map { it.pkg }
            }
        }

        Box(
            Modifier.fillMaxSize().clickable(onClick = close).systemBarsPadding().padding(if (fullscreen) 0.dp else 20.dp),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                shape = RoundedCornerShape(if (fullscreen) 0.dp else 28.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (fullscreen) Modifier.fillMaxSize() else Modifier.heightIn(max = 560.dp))
                    .clickable(enabled = false) {},
            ) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(76.dp),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(title, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                            IconButton(onClick = close) { Icon(Icons.Filled.Close, "Cerrar") }
                        }
                    }
                    if (mode == MODE_QUICK && config.groups.isNotEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                config.groups.forEach { g ->
                                    val frozen = remember(apps, g) { Repo.groupFrozen(g) }
                                    FilterChip(
                                        selected = frozen,
                                        onClick = { lifecycleScope.launch { Repo.toggleGroup(g.id, Origin.WIDGET) } },
                                        label = { Text(g.name) },
                                        leadingIcon = { Icon(if (frozen) Icons.Filled.AcUnit else groupIcon(g.icon), null, Modifier.size(18.dp)) },
                                    )
                                }
                            }
                        }
                    }
                    if (list.isEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Text(
                                when (mode) {
                                    MODE_QUICK -> "Marcá apps como favoritas para verlas acá."
                                    MODE_GROUP -> "Este grupo no tiene apps instaladas."
                                    else -> "No hay apps en cuarentena."
                                },
                                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(8.dp),
                            )
                        }
                    }
                    items(list, key = { it }) { p ->
                        val e = map[p] ?: return@items
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.combinedClickable(
                                onClick = {
                                    lifecycleScope.launch {
                                        if (mode == MODE_QUICK) Repo.toggle(p, Origin.WIDGET)
                                        else { Repo.launch(p, Origin.WIDGET); close() }
                                    }
                                },
                                onLongClick = {
                                    lifecycleScope.launch {
                                        if (mode == MODE_QUICK) { Repo.launch(p, Origin.WIDGET); close() }
                                        else Repo.toggle(p, Origin.WIDGET)
                                    }
                                },
                            ).padding(4.dp),
                        ) {
                            AppIcon(vm, p, 56.dp, frozen = e.frozen)
                            Spacer(Modifier.size(4.dp))
                            Text(e.label, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            if (mode == MODE_QUICK) "Tocá para congelar o activar · Mantené presionado para abrir"
                            else "Tocá para abrir · Mantené presionado para congelar o activar",
                            fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
