package app.qarentenapp.ui

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import app.qarentenapp.core.Repo
import app.qarentenapp.core.Shortcuts
import kotlinx.coroutines.launch

/**
 * Accesos directos clásicos de Android (ACTION_CREATE_SHORTCUT).
 * Aparecen en la lista "Accesos directos" del launcher y los pueden usar otras apps
 * (launchers, Tasker, automatizaciones). Hay tres entradas, una por tipo:
 *  - Grupo de Q'App, App de Q'App, Cajón de cuarentena.
 */
open class CreateShortcutActivity : ComponentActivity() {

    open val kind: String = "group"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Repo.init(this)
        setResult(Activity.RESULT_CANCELED)
        if (kind == "drawer") { done(Shortcuts.drawerInfo(this)); return }
        lifecycleScope.launch { if (Repo.apps.value.isEmpty()) Repo.refresh() }
        setContent { QTheme { if (kind == "app") AppPicker() else GroupPicker() } }
    }

    private fun done(info: ShortcutInfoCompat) {
        setResult(Activity.RESULT_OK, Shortcuts.result(this, info))
        finish()
    }

    @Composable
    private fun GroupPicker() {
        val config by Repo.config.collectAsState()
        var toggle by remember { mutableStateOf(false) }
        Surface(Modifier.fillMaxSize()) {
            Column(Modifier.systemBarsPadding()) {
                Text("Acceso directo de grupo", fontSize = 24.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(20.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable { toggle = !toggle }.padding(horizontal = 20.dp, vertical = 4.dp),
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Congelar o activar de un toque")
                        Text(
                            if (toggle) "El acceso cambia el estado del grupo entero sin abrir nada"
                            else "El acceso abre la ventana del grupo con sus apps y candados",
                            fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = toggle, onCheckedChange = { toggle = it })
                }
                if (config.groups.isEmpty()) Text(
                    "Todavía no tenés grupos. Creá uno en Q'App.", modifier = Modifier.padding(20.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                LazyColumn {
                    items(config.groups, key = { it.id }) { g ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().clickable {
                                done(if (toggle) Shortcuts.groupToggleInfo(this@CreateShortcutActivity, g)
                                else Shortcuts.groupInfo(this@CreateShortcutActivity, g))
                            }.padding(horizontal = 20.dp, vertical = 14.dp),
                        ) {
                            Icon(groupIcon(g.icon), null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(16.dp))
                            Text(g.name, fontSize = 17.sp)
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun AppPicker() {
        val vm: MainViewModel = viewModel()
        val apps by Repo.apps.collectAsState()
        var query by remember { mutableStateOf("") }
        val q = query.trim().lowercase()
        val list = apps.filter { it.hasLauncher && (q.isEmpty() || it.label.lowercase().contains(q)) }
            .sortedWith(compareByDescending<app.qarentenapp.data.AppEntry> { it.frozen }.thenBy { it.label.lowercase() })
        Surface(Modifier.fillMaxSize()) {
            Column(Modifier.systemBarsPadding()) {
                Text("Acceso directo de app", fontSize = 24.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 20.dp, top = 20.dp))
                Text("Abre la app y la descongela si hace falta. Primero aparecen las congeladas.",
                    fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
                OutlinedTextField(query, { query = it }, placeholder = { Text("Buscar app") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp))
                LazyColumn {
                    items(list, key = { it.pkg }) { a ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().clickable { done(Shortcuts.appInfo(this@CreateShortcutActivity, a.pkg)) }
                                .padding(horizontal = 20.dp, vertical = 8.dp),
                        ) {
                            AppIcon(vm, a.pkg, 40.dp, frozen = a.frozen)
                            Spacer(Modifier.width(14.dp))
                            Text(a.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}

class CreateGroupShortcut : CreateShortcutActivity() { override val kind = "group" }
class CreateAppShortcut : CreateShortcutActivity() { override val kind = "app" }
class CreateDrawerShortcut : CreateShortcutActivity() { override val kind = "drawer" }
