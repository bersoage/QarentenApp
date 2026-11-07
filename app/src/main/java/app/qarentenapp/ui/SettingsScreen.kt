package app.qarentenapp.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.qarentenapp.BuildConfig
import app.qarentenapp.core.Freezer
import app.qarentenapp.core.Mode
import app.qarentenapp.core.ModePref
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(vm: MainViewModel) {
    val config by vm.config.collectAsState()
    val mode by vm.mode.collectAsState()
    val s = config.settings
    val ctx = LocalContext.current
    var warnSystem by rememberSaveable { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) vm.exportBackup(uri)
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.importBackup(uri)
    }
    val treeLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) vm.setAutoBackupTree(uri)
    }

    Column(Modifier.verticalScroll(rememberScrollState()).padding(bottom = 32.dp)) {
        Text("Ajustes", fontSize = 28.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(24.dp, 24.dp, 24.dp, 8.dp))

        // ---------- Modo ----------
        Section("Cómo congela")
        val status = when (mode) {
            null -> "Detectando…"
            Mode.ROOT -> "Usando root"
            Mode.SHIZUKU -> "Usando Shizuku"
            Mode.NONE -> "Sin root ni Shizuku: no se puede congelar"
        }
        Text(status, color = if (mode == Mode.NONE) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp))
        ModePref.entries.forEach { p ->
            val label = when (p) {
                ModePref.AUTO -> "Automático (root si hay, si no Shizuku)"
                ModePref.ROOT -> "Solo root"
                ModePref.SHIZUKU -> "Solo Shizuku"
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().clickable { vm.updateSettings { it.copy(modePref = p.name) } }.padding(horizontal = 12.dp),
            ) {
                RadioButton(selected = s.modePref == p.name, onClick = { vm.updateSettings { it.copy(modePref = p.name) } })
                Text(label)
            }
        }
        if (mode == Mode.NONE) {
            Row(Modifier.padding(horizontal = 24.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (Freezer.shizukuRunning()) {
                    Button(onClick = { vm.requestShizuku() }) { Text("Dar permiso a Shizuku") }
                } else {
                    Button(onClick = {
                        val i = ctx.packageManager.getLaunchIntentForPackage("moe.shizuku.privileged.api")
                            ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=moe.shizuku.privileged.api"))
                        runCatching { ctx.startActivity(i) }
                    }) { Text("Abrir Shizuku") }
                }
                OutlinedButton(onClick = { vm.redetect() }) { Text("Reintentar") }
            }
            Text("Shizuku no está activo." , fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp))
        }

        // ---------- Backup ----------
        Section("Backup de tu configuración")
        Text(
            if (s.lastBackupAt == 0L) "Todavía no hiciste ningún backup."
            else "Último backup: " + DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(s.lastBackupAt)),
            fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 24.dp),
        )
        Row(Modifier.padding(horizontal = 24.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                val stamp = SimpleDateFormat("yyyy-MM-dd_HHmm", Locale.US).format(Date())
                exportLauncher.launch("qarentenapp-backup_$stamp.json")
            }) { Text("Exportar") }
            OutlinedButton(onClick = { importLauncher.launch(arrayOf("application/json", "application/octet-stream", "*/*")) }) { Text("Importar") }
        }
        ToggleRow(
            "Backup automático en una carpeta",
            if (s.autoBackupTree == null) "Elegí una carpeta (puede ser Google Drive). Se actualiza cada vez que cambiás algo."
            else "Activo. Se guarda qarentenapp-backup.json en la carpeta elegida.",
            checked = s.autoBackupTree != null,
        ) { on -> if (on) treeLauncher.launch(null) else vm.setAutoBackupTree(null) }
        Text(
            "Además, Android guarda tu configuración en tu cuenta de Google y la devuelve sola si reinstalás la app.",
            fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 24.dp),
        )

        // ---------- Notificación ----------
        Section("Notificación")
        ToggleRow("Apps activas", "Muestra las apps que descongelaste, para volver a congelarlas de un toque.", s.notifyActive) { on ->
            vm.updateSettings { it.copy(notifyActive = on) }
        }

        // ---------- Avanzado ----------
        Section("Avanzado")
        ToggleRow("Mostrar apps del sistema", "Permite congelar apps del sistema. Las esenciales quedan protegidas.", s.showSystem) { on ->
            if (on && !s.showSystemWarned) warnSystem = true
            else vm.updateSettings { it.copy(showSystem = on) }
        }
        ToggleRow("Mostrar nombre del paquete", "Muestra el identificador técnico de cada app.", s.showPackageName) { on ->
            vm.updateSettings { it.copy(showPackageName = on) }
        }

        Section("Acerca de")
        Text("QarentenApp ${BuildConfig.VERSION_NAME}", modifier = Modifier.padding(horizontal = 24.dp))
    }

    if (warnSystem) AlertDialog(
        onDismissRequest = { warnSystem = false },
        title = { Text("Apps del sistema") },
        text = { Text("Congelar la app del sistema equivocada puede hacer que algo del celular deje de funcionar. Q'App protege las esenciales, pero usá esta opción solo si sabés qué estás congelando.") },
        confirmButton = {
            TextButton(onClick = {
                warnSystem = false
                vm.updateSettings { it.copy(showSystem = true, showSystemWarned = true) }
            }) { Text("Entiendo, mostrar") }
        },
        dismissButton = { TextButton(onClick = { warnSystem = false }) { Text("Cancelar") } },
    )
}

@Composable
private fun Section(title: String) {
    HorizontalDivider(Modifier.padding(top = 16.dp))
    Text(title, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 24.dp, top = 16.dp, bottom = 4.dp))
}

@Composable
private fun ToggleRow(title: String, sub: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clickable { onChange(!checked) }.padding(horizontal = 24.dp, vertical = 10.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title)
            Text(sub, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
