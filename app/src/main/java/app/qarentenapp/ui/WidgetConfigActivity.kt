package app.qarentenapp.ui

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import app.qarentenapp.core.Repo
import app.qarentenapp.core.Widgets
import app.qarentenapp.data.WidgetConf
import kotlinx.coroutines.launch

/** Configuración de un widget: grupo o app, y su apariencia y comportamiento. */
class WidgetConfigActivity : ComponentActivity() {

    private var widgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Repo.init(this)
        widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        setResult(Activity.RESULT_CANCELED, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) { finish(); return }
        lifecycleScope.launch { if (Repo.apps.value.isEmpty()) Repo.refresh() }
        val existing = Repo.config.value.widgets[widgetId.toString()]
        setContent { QTheme { Screen(existing) } }
    }

    private fun save(conf: WidgetConf) {
        Repo.update(backup = false) { it.copy(widgets = it.widgets + (widgetId.toString() to conf)) }
        Widgets.update(this, AppWidgetManager.getInstance(this), widgetId)
        setResult(Activity.RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))
        finish()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun Screen(existing: WidgetConf?) {
        val vm: MainViewModel = viewModel()
        val config by Repo.config.collectAsState()
        val apps by Repo.apps.collectAsState()
        // Tipo fijo según el widget elegido: App, Grupo o Ventana de grupo
        val fixed = remember { Widgets.fixedKind(this@WidgetConfigActivity, widgetId) }
        var kind by remember { mutableStateOf(when (fixed) { "app" -> "app"; "group", "groupwin" -> "group"; else -> existing?.kind ?: "group" }) }
        var target by remember { mutableStateOf(existing?.target) }
        var direct by remember { mutableStateOf(existing?.directLaunch ?: false) }
        var black by remember { mutableStateOf(existing?.blackBackground ?: false) }
        var hideCaption by remember { mutableStateOf(existing?.hideCaption ?: false) }
        var hideStatus by remember { mutableStateOf(existing?.hideStatus ?: false) }
        var quick by remember { mutableStateOf(when (fixed) { "group" -> true; "groupwin" -> false; else -> existing?.quickToggle ?: false }) }
        var query by remember { mutableStateOf("") }

        Surface(Modifier.fillMaxSize()) {
            Column(Modifier.systemBarsPadding()) {
                Text(
                    when (fixed) {
                        "app" -> "Widget de app"
                        "group" -> "Widget de grupo"
                        "groupwin" -> "Widget de ventana de grupo"
                        else -> "Widget de Q'App"
                    },
                    fontSize = 24.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 20.dp, top = 20.dp, end = 20.dp),
                )
                Text(
                    when (fixed) {
                        "app" -> "Elegí la app. Un toque la congela o la descongela."
                        "group" -> "Elegí el grupo. Un toque congela o descongela todas sus apps."
                        "groupwin" -> "Elegí el grupo. Un toque abre su ventana con la lista de apps."
                        else -> "Elegí un grupo o una app."
                    },
                    fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 12.dp),
                )
                if (fixed == null) SingleChoiceSegmentedButtonRow(Modifier.padding(horizontal = 20.dp).fillMaxWidth()) {
                    SegmentedButton(selected = kind == "group", onClick = { kind = "group"; target = null },
                        shape = SegmentedButtonDefaults.itemShape(0, 2)) { Text("Grupo") }
                    SegmentedButton(selected = kind == "app", onClick = { kind = "app"; target = null },
                        shape = SegmentedButtonDefaults.itemShape(1, 2)) { Text("App") }
                }
                if (kind == "app") OutlinedTextField(
                    query, { query = it }, placeholder = { Text("Buscar app") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                )
                LazyColumn(Modifier.weight(1f)) {
                    if (kind == "group") {
                        if (config.groups.isEmpty()) item {
                            Text("Todavía no tenés grupos. Creá uno en Q'App.", modifier = Modifier.padding(20.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        items(config.groups, key = { it.id }) { g ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth().clickable { target = g.id }.padding(horizontal = 12.dp, vertical = 6.dp),
                            ) {
                                RadioButton(selected = target == g.id, onClick = { target = g.id })
                                GroupGlyph(g.icon, 24.dp, MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(12.dp))
                                Text(g.name)
                            }
                        }
                    } else {
                        val q = query.trim().lowercase()
                        val list = apps.filter { it.hasLauncher && !it.protected && (q.isEmpty() || it.label.lowercase().contains(q)) }
                        items(list, key = { it.pkg }) { a ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth().clickable { target = a.pkg }.padding(horizontal = 12.dp, vertical = 4.dp),
                            ) {
                                RadioButton(selected = target == a.pkg, onClick = { target = a.pkg })
                                AppIcon(vm, a.pkg, 36.dp, frozen = a.frozen)
                                Spacer(Modifier.width(12.dp))
                                Text(a.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
                HorizontalDivider()
                Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    if (kind == "group" && fixed == null) Toggle("Congelar o activar de un toque",
                        "Si está apagado, el widget abre la ventana del grupo con sus apps", quick) { quick = it }
                    if (kind == "app") Toggle("Lanzamiento directo", "Abre la app de un toque (se descongela sola) en vez de congelarla", direct) { direct = it }
                    Toggle("Fondo negro", "Muestra el widget con fondo negro", black) { black = it }
                    Toggle("Ocultar nombre", "Oculta el nombre del grupo o la app", hideCaption) { hideCaption = it }
                    Toggle("Ocultar estado", "Oculta el indicador de congelado", hideStatus) { hideStatus = it }
                }
                Button(
                    onClick = { target?.let { save(WidgetConf(kind, it, direct && kind == "app", black, hideCaption, hideStatus, quick && kind == "group")) } },
                    enabled = target != null,
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                ) { Text(if (existing == null) "Agregar widget" else "Guardar") }
            }
        }
    }

    @Composable
    private fun Toggle(title: String, sub: String, checked: Boolean, onChange: (Boolean) -> Unit) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { onChange(!checked) }.padding(vertical = 4.dp)) {
            Column(Modifier.weight(1f)) {
                Text(title)
                Text(sub, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked, onChange, Modifier.size(width = 52.dp, height = 32.dp))
        }
    }
}
