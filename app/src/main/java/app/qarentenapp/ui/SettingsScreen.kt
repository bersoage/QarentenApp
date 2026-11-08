package app.qarentenapp.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
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
import app.qarentenapp.core.Shortcuts
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(vm: MainViewModel) {
    val config by vm.config.collectAsState()
    val mode by vm.mode.collectAsState()
    val s = config.settings
    val ctx = LocalContext.current
    var warnSystem by rememberSaveable { mutableStateOf(false) }
    var warnServices by rememberSaveable { mutableStateOf(false) }
    var setPassword by rememberSaveable { mutableStateOf(false) }
    var setGroupPw by rememberSaveable { mutableStateOf(false) }
    var report by rememberSaveable { mutableStateOf(false) }

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
            Radio(label, s.modePref == p.name) { vm.updateSettings { it.copy(modePref = p.name) } }
        }
        if (mode == Mode.NONE) {
            Text("Shizuku no está activo.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp))
            Row(Modifier.padding(horizontal = 24.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (Freezer.shizukuRunning()) {
                    Button(onClick = { vm.requestShizuku() }) { Text("Dar permiso a Shizuku") }
                } else {
                    Button(onClick = {
                        val i = ctx.packageManager.getLaunchIntentForPackage("moe.shizuku.privileged.api")
                            ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=moe.shizuku.privileged.api"))
                        runCatching { ctx.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                    }) { Text("Abrir Shizuku") }
                }
                OutlinedButton(onClick = { vm.redetect() }) { Text("Reintentar") }
            }
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
        Note("Además, Android guarda tu configuración en tu cuenta de Google y la devuelve sola si reinstalás la app.")
        TextButton(onClick = { vm.removePending(null) }, modifier = Modifier.padding(horizontal = 12.dp)) {
            Text("Quitar todas las apps pendientes de los grupos")
        }

        // ---------- Apariencia ----------
        Section("Apariencia")
        Text("Fondo", modifier = Modifier.padding(horizontal = 24.dp))
        val bg = effectiveBackground(s)
        FlowRow(Modifier.padding(horizontal = 24.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("AUTO" to "Según el celular", "LIGHT" to "Claro hielo", "NAVY" to "Azul noche", "CHARCOAL" to "Gris carbón", "OLED" to "Negro OLED")
                .forEach { (key, label) ->
                    FilterChip(selected = bg == key, onClick = { vm.updateSettings { it.copy(background = key) } }, label = { Text(label) })
                }
        }
        if (bg == "AUTO") {
            Text("De noche, usar:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 24.dp))
            FlowRow(Modifier.padding(horizontal = 24.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("NAVY" to "Azul noche", "CHARCOAL" to "Gris carbón", "OLED" to "Negro OLED").forEach { (key, label) ->
                    FilterChip(selected = s.darkStyle == key, onClick = { vm.updateSettings { it.copy(darkStyle = key) } }, label = { Text(label) })
                }
            }
        }
        Text("Color de acento", modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 8.dp))
        FlowRow(Modifier.padding(horizontal = 24.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Accents.forEachIndexed { i, (name, light, dark) ->
                androidx.compose.material3.Surface(
                    onClick = { vm.updateSettings { it.copy(accent = i) } },
                    shape = androidx.compose.foundation.shape.CircleShape,
                    color = if (androidx.compose.foundation.isSystemInDarkTheme()) dark else light,
                    border = if (s.accent == i) androidx.compose.foundation.BorderStroke(3.dp, MaterialTheme.colorScheme.onSurface) else null,
                    modifier = Modifier.size(40.dp),
                ) {}
            }
        }
        Text(Accents[s.accent.coerceIn(0, Accents.lastIndex)].first, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 24.dp))
        Text("Tema de íconos", modifier = Modifier.padding(horizontal = 24.dp))
        FlowRow(Modifier.padding(horizontal = 24.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconThemes.forEach { (key, label) ->
                FilterChip(selected = s.iconTheme == key, onClick = { vm.updateSettings { it.copy(iconTheme = key) } }, label = { Text(label) })
            }
        }

        // ---------- Cajón de cuarentena ----------
        Section("Cajón de cuarentena")
        Note("Muestra tus apps congeladas para abrirlas de un toque (se descongelan solas). Se abre desde un acceso directo, el mosaico del panel rápido o manteniendo presionado el ícono de Q'App.")
        ToggleRow("Pantalla completa", "Si está apagado, se abre como ventana flotante", s.drawerFullscreen) { on ->
            vm.updateSettings { it.copy(drawerFullscreen = on) }
        }
        if (Shortcuts.canPin(ctx)) Row(Modifier.padding(horizontal = 24.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { Shortcuts.pinDrawer(ctx) }) { Text("Acceso directo") }
            OutlinedButton(onClick = {
                ctx.startActivity(Intent(ctx, PopupActivity::class.java).putExtra(PopupActivity.EXTRA_MODE, PopupActivity.MODE_DRAWER))
            }) { Text("Probar ahora") }
        }

        // ---------- Notificación ----------
        Section("Notificación")
        ToggleRow("Activar notificación", "Muestra las apps que activaste, para volver a congelarlas de un toque", s.notifyActive) { on ->
            vm.updateSettings { it.copy(notifyActive = on) }
        }
        if (s.notifyActive) {
            Text("Estilo", modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp))
            Radio("Automático (lista si son pocas, grilla si son muchas)", s.notifStyle == "AUTO") { vm.updateSettings { it.copy(notifStyle = "AUTO") } }
            Radio("Grilla (hasta 12 íconos)", s.notifStyle == "GRID") { vm.updateSettings { it.copy(notifStyle = "GRID") } }
            Radio("Lista (hasta 3 apps con detalle)", s.notifStyle == "LIST") { vm.updateSettings { it.copy(notifStyle = "LIST") } }
            ToggleRow("Permanente", "No se puede descartar deslizando", s.notifOngoing) { on -> vm.updateSettings { it.copy(notifOngoing = on) } }
            ToggleRow("Un toque (solo grilla)", "Tocar un ícono lo congela al instante, en vez de abrir la app", s.notifSingleClick) { on ->
                vm.updateSettings { it.copy(notifSingleClick = on) }
            }
            ToggleRow("Solo apps activas", "Si está apagado, también muestra las últimas que congelaste para reabrirlas", s.notifActiveOnly) { on ->
                vm.updateSettings { it.copy(notifActiveOnly = on) }
            }
        }

        // ---------- Widgets y accesos ----------
        Section("Widgets y accesos directos")
        Note("Widget: mantené presionada la pantalla de inicio → Widgets → Q'App. Podés elegir un grupo o una app y su apariencia.\n\nAcceso directo de grupo: mantené presionado un grupo → «Agregar a pantalla de inicio».\n\nPanel rápido: editá los mosaicos del panel y sumá «Cajón de cuarentena» y «Menú rápido».")
        Row(Modifier.padding(horizontal = 24.dp, vertical = 4.dp)) {
            OutlinedButton(onClick = {
                ctx.startActivity(Intent(ctx, PopupActivity::class.java).putExtra(PopupActivity.EXTRA_MODE, PopupActivity.MODE_QUICK))
            }) { Text("Abrir menú rápido") }
        }

        // ---------- Seguridad ----------
        Section("Seguridad")
        ToggleRow("Bloquear Q'App al abrirla", "Pide el método de «Protección de grupos» para abrir Q'App y para usar widgets y accesos directos", s.protectionEnabled) { on ->
            if (on && Biometrics.effective(null) == "PASSWORD" && s.passwordHash == null) setPassword = true
            else vm.setProtection(on)
        }

        // ---------- Protección de grupos ----------
        Section("Protección de grupos")
        Note("Elegí en cada grupo (✏️ Editar) si queda protegido. Se autentica una sola vez al entrar; adentro no vuelve a pedirse.")
        Text("Cómo desbloquear", modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp))
        val m = Biometrics.effective(null)
        Radio("Huella o bloqueo del celular (patrón, PIN)", m == "DEVICE") { vm.updateSettings { it.copy(lockMethod = "DEVICE") } }
        Radio("Solo huella", m == "BIOMETRIC") { vm.updateSettings { it.copy(lockMethod = "BIOMETRIC") } }
        Radio("Contraseña de Q'App", m == "PASSWORD") { vm.updateSettings { it.copy(lockMethod = "PASSWORD") } }
        Text("Cuánto dura el desbloqueo", modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp))
        listOf(0 to "Hasta que salgas del grupo", 1 to "1 minuto", 5 to "5 minutos", 15 to "15 minutos").forEach { (m, l) ->
            Radio(l, s.unlockMinutes == m) { vm.updateSettings { it.copy(unlockMinutes = m) } }
        }
        TextButton(onClick = { setGroupPw = true }, modifier = Modifier.padding(horizontal = 12.dp)) {
            Text(if (s.passwordHash == null) "Crear contraseña de Q'App" else "Cambiar contraseña de Q'App")
        }

        // ---------- Actualizaciones ----------
        Section("Actualizaciones y arranque")
        ToggleRow("Vigilar actualizaciones", "Avisa si una app en cuarentena se activó sola por una actualización o un reseteo", s.monitorUpdates) { on ->
            vm.updateSettings { it.copy(monitorUpdates = on) }
        }
        ToggleRow("Volver a congelar sola", "Pone de nuevo en cuarentena esas apps sin preguntar", s.autoRequarantine) { on ->
            vm.updateSettings { it.copy(autoRequarantine = on) }
        }
        ToggleRow("Volver a congelar al reiniciar", "Al prender el celular, vuelve a congelar las apps que descongelaste desde Q'App. Con Shizuku, lo hace apenas Shizuku esté activo.", s.refreezeOnBoot) { on ->
            vm.updateSettings { it.copy(refreezeOnBoot = on) }
        }

        // ---------- Avanzado ----------
        Section("Avanzado")
        ToggleRow("Incluir servicios del sistema", "En el filtro «Sistema», muestra también los componentes que no tienen ícono", s.showServices) { on ->
            if (on) warnServices = true else vm.updateSettings { it.copy(showServices = false) }
        }
        ToggleRow("Mostrar nombre del paquete", "Muestra el identificador técnico de cada app", s.showPackageName) { on ->
            vm.updateSettings { it.copy(showPackageName = on) }
        }
        ToggleRow("Reiniciar el launcher", "Solo si las apps congeladas siguen apareciendo en tu pantalla de inicio", s.forceRestartLauncher) { on ->
            vm.updateSettings { it.copy(forceRestartLauncher = on) }
        }

        // ---------- Acerca de ----------
        Section("Acerca de")
        Column(Modifier.padding(horizontal = 24.dp, vertical = 4.dp)) {
            Text("QarentenApp", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text("Versión ${BuildConfig.VERSION_NAME}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Creada por Bernardo Soage", modifier = Modifier.padding(top = 6.dp))
            Text("Corrientes, Argentina · 2026", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        TextButton(onClick = { report = true }, modifier = Modifier.padding(horizontal = 12.dp)) { Text("Enviar reporte de error") }
    }

    if (warnSystem) AlertDialog(
        onDismissRequest = { warnSystem = false },
        title = { Text("Apps del sistema") },
        text = { Text("Congelar la app del sistema equivocada puede hacer que algo del celular deje de funcionar. Q'App protege las esenciales, pero usá esta opción solo si sabés qué estás congelando.") },
        confirmButton = {
            TextButton(onClick = { warnSystem = false; vm.updateSettings { it.copy(showSystem = true, showSystemWarned = true) } }) { Text("Entiendo, mostrar") }
        },
        dismissButton = { TextButton(onClick = { warnSystem = false }) { Text("Cancelar") } },
    )
    if (warnServices) AlertDialog(
        onDismissRequest = { warnServices = false },
        title = { Text("Servicios del sistema") },
        text = { Text("¡Cuidado! Si congelás el servicio equivocado, el celular puede dejar de funcionar bien. Activalo solo si sabés exactamente qué hace cada servicio.") },
        confirmButton = {
            TextButton(onClick = { warnServices = false; vm.updateSettings { it.copy(showServices = true) } }) { Text("Entiendo, mostrar") }
        },
        dismissButton = { TextButton(onClick = { warnServices = false }) { Text("Cancelar") } },
    )
    if (setGroupPw) SetPasswordDialog(onDismiss = { setGroupPw = false }) { p -> setGroupPw = false; vm.setGroupPassword(p) }
    if (setPassword) SetPasswordDialog(onDismiss = { setPassword = false }) { p -> setPassword = false; vm.setPassword(p) }
    if (report) {
        val text = vm.errorReport()
        AlertDialog(
            onDismissRequest = { report = false },
            title = { Text("Reporte de error") },
            text = { Text("Se va a compartir información técnica: modelo del celular, versión de Android, modo y los últimos comandos que ejecutó Q'App (con los nombres de paquete de las apps). Elegí con qué app enviarlo.") },
            confirmButton = {
                TextButton(onClick = {
                    report = false
                    val send = Intent(Intent.ACTION_SEND).setType("text/plain")
                        .putExtra(Intent.EXTRA_SUBJECT, "Reporte de error QarentenApp ${BuildConfig.VERSION_NAME}")
                        .putExtra(Intent.EXTRA_TEXT, text)
                    runCatching { ctx.startActivity(Intent.createChooser(send, "Enviar reporte").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                }) { Text("Compartir") }
            },
            dismissButton = { TextButton(onClick = { report = false }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun Section(title: String) {
    HorizontalDivider(Modifier.padding(top = 16.dp))
    Text(title, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 24.dp, top = 16.dp, bottom = 4.dp))
}

@Composable
private fun Note(text: String) {
    Text(text, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp))
}

@Composable
private fun Radio(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 12.dp),
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(label)
    }
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
