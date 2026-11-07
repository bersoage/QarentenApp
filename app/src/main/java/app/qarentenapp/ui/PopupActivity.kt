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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
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

    /** Pedido de confirmación de apps del sistema dentro de las ventanas flotantes. */
    private val confirmState = mutableStateOf<SystemConfirm?>(null)

    private fun guarded(pkgs: List<String>, frozen: Boolean, origin: Origin) {
        val run = { lifecycleScope.launch { Repo.setFrozen(pkgs, frozen, origin) }; Unit }
        val sys = Repo.systemToConfirm(pkgs, frozen)
        if (sys.isEmpty()) run() else confirmState.value = SystemConfirm(pkgs, sys, run)
    }

    /**
     * Ventana de un grupo (acceso directo y widget de grupo), en gris carbón:
     * «Congelar todas» (azul) y «Descongelar todas» (rojo), y la lista de apps con ❄️ o 🔥.
     * Tocar la app la abre (se descongela sola); tocar ❄️/🔥 la congela o la activa.
     */
    @Composable
    private fun GroupWindow(vm: MainViewModel, groupId: String?, close: () -> Unit) {
        val head = Color(0xFF151515); val line = Color(0xFF3D3B39); val body = Color(0xFF20201F)
        val txt = Color(0xFFECECEA); val sub = Color(0xFFA0A09C)
        val apps by Repo.apps.collectAsState()
        val config by Repo.config.collectAsState()
        val busy by Repo.busy.collectAsState()
        val confirm by confirmState
        val map = apps.associateBy { it.pkg }
        val g = config.groups.firstOrNull { it.id == groupId }
        val rows = g?.packages?.mapNotNull { map[it] }?.sortedBy { it.label.lowercase() } ?: emptyList()
        val pending = g?.packages?.count { it !in map } ?: 0

        confirm?.let { SystemConfirmDialog(it, onDismiss = { confirmState.value = null }) }

        Box(
            Modifier.fillMaxSize().clickable(onClick = close).systemBarsPadding().padding(20.dp),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = body,
                modifier = Modifier.fillMaxWidth().heightIn(max = 660.dp).clickable(enabled = false) {},
            ) {
                Column {
                    Row(
                        Modifier.fillMaxWidth().background(head).padding(start = 20.dp, end = 8.dp, top = 14.dp, bottom = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (g != null) GroupGlyph(g.icon, 28.dp, Color(0xFF78AAE6))
                        Spacer(Modifier.width(12.dp))
                        Text(g?.name ?: "Grupo borrado", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = txt,
                            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        IconButton(onClick = close) { Icon(Icons.Filled.Close, "Cerrar", tint = sub) }
                    }
                    HorizontalDivider(color = line)
                    if (g != null) Row(
                        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Button(
                            onClick = { guarded(g.packages, true, Origin.SHORTCUT) },
                            enabled = !busy && rows.any { !it.frozen && !it.protected },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2F5E8E), contentColor = Color.White),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                            modifier = Modifier.weight(1f),
                        ) {
                            Glyph("\ueb3b", 18.dp, Color.White, outlined = true)
                            Spacer(Modifier.width(6.dp))
                            Text("Congelar todas", fontSize = 14.sp, maxLines = 1)
                        }
                        Button(
                            onClick = { guarded(g.packages, false, Origin.SHORTCUT) },
                            enabled = !busy && rows.any { it.frozen },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB3261E), contentColor = Color.White),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                            modifier = Modifier.weight(1f),
                        ) {
                            Glyph("\uef55", 18.dp, Color.White, outlined = true)
                            Spacer(Modifier.width(6.dp))
                            Text("Descongelar todas", fontSize = 14.sp, maxLines = 1)
                        }
                    }
                    HorizontalDivider(color = line)
                    if (rows.isEmpty()) Text(
                        if (g == null) "Este grupo ya no existe." else "Este grupo no tiene apps instaladas.",
                        color = sub, modifier = Modifier.padding(20.dp),
                    )
                    LazyColumn(Modifier.weight(1f, fill = false)) {
                        items(rows, key = { it.pkg }) { e ->
                            val system = e.isSystem && !e.protected
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable(enabled = e.hasLauncher) {
                                            lifecycleScope.launch { if (Repo.launch(e.pkg, Origin.SHORTCUT)) close() }
                                        }
                                        .padding(start = 16.dp, top = 10.dp, bottom = 10.dp),
                                ) {
                                    AppIcon(vm, e.pkg, 46.dp, frozen = e.frozen, badge = false)
                                    Spacer(Modifier.width(14.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text((if (system) "⚠️ " else "") + e.label, fontSize = 17.sp, color = txt,
                                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(
                                            when {
                                                e.protected -> "Protegida"
                                                system && e.frozen -> "Congelada · app del sistema"
                                                system -> "Activa · app del sistema, cuidado"
                                                e.frozen -> "Congelada"
                                                else -> "Activa"
                                            },
                                            fontSize = 12.sp, color = sub,
                                        )
                                    }
                                }
                                TextButton(
                                    onClick = { guarded(listOf(e.pkg), !e.frozen, Origin.SHORTCUT) },
                                    enabled = !e.protected && !busy,
                                    modifier = Modifier.padding(end = 6.dp).size(60.dp),
                                ) {
                                    if (e.protected) Icon(Icons.Filled.Lock, "Protegida", tint = sub)
                                    else Text(if (e.frozen) "❄️" else "🔥", fontSize = 28.sp)
                                }
                            }
                            HorizontalDivider(Modifier.padding(start = 76.dp), color = line)
                        }
                    }
                    if (pending > 0) Text(
                        "$pending ${if (pending == 1) "app pendiente" else "apps pendientes"} de instalar",
                        fontSize = 12.sp, color = sub, modifier = Modifier.padding(16.dp),
                    )
                }
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

        if (mode == MODE_GROUP) {
            GroupWindow(vm, groupId, close)
            return
        }
        val pendingConfirm by confirmState
        pendingConfirm?.let { SystemConfirmDialog(it, onDismiss = { confirmState.value = null }) }

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
                                        onClick = { guarded(g.packages, !Repo.groupFrozen(g), Origin.WIDGET) },
                                        label = { Text(g.name) },
                                        leadingIcon = { if (frozen) Icon(Icons.Filled.AcUnit, null, Modifier.size(18.dp)) else GroupGlyph(g.icon, 18.dp, MaterialTheme.colorScheme.onSurfaceVariant) },
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
                                        if (mode == MODE_QUICK) guarded(listOf(p), !e.frozen, Origin.WIDGET)
                                        else { Repo.launch(p, Origin.WIDGET); close() }
                                    }
                                },
                                onLongClick = {
                                    lifecycleScope.launch {
                                        if (mode == MODE_QUICK) { Repo.launch(p, Origin.WIDGET); close() }
                                        else guarded(listOf(p), !e.frozen, Origin.WIDGET)
                                    }
                                },
                            ).padding(4.dp),
                        ) {
                            AppIcon(vm, p, 56.dp, frozen = e.frozen)
                            Spacer(Modifier.size(4.dp))
                            Text((if (e.isSystem && !e.protected) "⚠️ " else "") + e.label, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
