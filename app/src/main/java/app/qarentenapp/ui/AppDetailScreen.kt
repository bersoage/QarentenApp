package app.qarentenapp.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.qarentenapp.core.PermCategory
import app.qarentenapp.core.Shortcuts
import java.text.DateFormat
import java.util.Date

/** Ficha de una app: versión, fechas, partición, permisos y acciones. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AppDetailScreen(vm: MainViewModel, pkg: String, back: () -> Unit) {
    val ctx = LocalContext.current
    val apps by vm.apps.collectAsState()
    val config by vm.config.collectAsState()
    val e = apps.firstOrNull { it.pkg == pkg }
    val d = remember(pkg, e?.frozen) { vm.details(pkg) }
    var showAllPerms by rememberSaveable { mutableStateOf(false) }
    val fav = pkg in config.favorites
    val df = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)

    Column {
        TopAppBar(
            title = { Text(e?.label ?: pkg) },
            navigationIcon = { IconButton(onClick = back) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver") } },
            actions = {
                IconButton(onClick = { vm.toggleFavorite(pkg) }) {
                    Icon(if (fav) Icons.Filled.Star else Icons.Outlined.StarBorder, if (fav) "Quitar de favoritas" else "Agregar a favoritas")
                }
            },
        )
        if (e == null || d == null) {
            Text("Esta app no está instalada.", modifier = Modifier.padding(24.dp))
        } else Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppIcon(vm, pkg, 72.dp, frozen = e.frozen)
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(e.label, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        when {
                            e.protected -> "Protegida · no se puede congelar"
                            e.frozen -> "Congelada"
                            else -> "Activa"
                        },
                        color = if (e.frozen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Row(Modifier.padding(vertical = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!e.protected) Button(onClick = { vm.toggle(pkg) }) { Text(if (e.frozen) "Activar" else "Congelar") }
                if (e.hasLauncher) OutlinedButton(onClick = { vm.launch(pkg) }) { Text("Abrir") }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = {
                    runCatching {
                        ctx.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$pkg"))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    }
                }, label = { Text("Info de Android") })
                AssistChip(onClick = { vm.openStore(pkg) }, label = { Text("Play Store") })
                if (Shortcuts.canPin(ctx) && e.hasLauncher) AssistChip(onClick = { Shortcuts.pinApp(ctx, pkg) }, label = { Text("Acceso directo") })
            }

            HorizontalDivider(Modifier.padding(vertical = 16.dp))
            Info("Paquete", d.pkg)
            Info("Versión", "${d.version} (${d.versionCode})")
            Info("Instalada", df.format(Date(d.firstInstall)))
            Info("Última actualización", df.format(Date(d.lastUpdate)))
            Info("Partición", d.partition)
            Info("Tipo", if (d.isSystem) (if (e.hasLauncher) "App del sistema" else "Servicio del sistema") else "App de usuario")
            val groups = config.groups.filter { pkg in it.packages }.joinToString(", ") { it.name }
            Info("Grupos", groups.ifEmpty { "Ninguno" })

            HorizontalDivider(Modifier.padding(vertical = 16.dp))
            Text("Permisos", fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
            val cats = PermCategory.of(d.permissions)
            if (cats.isEmpty()) Text("No pide permisos relevantes.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            cats.forEach { c ->
                Text("• ${c.label}", modifier = Modifier.padding(top = 6.dp))
                Text(
                    d.permissions.filter { it in c.perms }.joinToString(", ") { it.substringAfterLast('.') },
                    fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 12.dp),
                )
            }
            if (d.permissions.isNotEmpty()) {
                OutlinedButton(onClick = { showAllPerms = !showAllPerms }, modifier = Modifier.padding(top = 12.dp)) {
                    Text(if (showAllPerms) "Ocultar lista completa" else "Ver todos (${d.permissions.size})")
                }
                if (showAllPerms) d.permissions.sorted().forEach {
                    Text(it, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.padding(24.dp))
        }
    }
}

@Composable
private fun Info(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(150.dp))
        Text(value, modifier = Modifier.weight(1f))
    }
}
