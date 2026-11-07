package app.qarentenapp.ui

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.qarentenapp.core.Freezer
import app.qarentenapp.core.Mode
import app.qarentenapp.core.Repo
import app.qarentenapp.core.Security
import app.qarentenapp.data.AppDetails
import app.qarentenapp.data.AppSettings
import app.qarentenapp.data.Config
import app.qarentenapp.data.Group
import app.qarentenapp.data.IconCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val ctx get() = getApplication<Application>()
    val icons = IconCache()

    val config = Repo.config
    val apps = Repo.apps
    val mode = Repo.mode
    val busy = Repo.busy
    val messages = Repo.messages

    private val _loaded = MutableStateFlow(false)
    val loaded = _loaded.asStateFlow()

    /** Bloqueado por contraseña. */
    private val _locked = MutableStateFlow(Repo.config.value.settings.protectionEnabled)
    val locked = _locked.asStateFlow()

    /** Backup recién importado, esperando confirmación. */
    private val _pendingImport = MutableStateFlow<Config?>(null)
    val pendingImport = _pendingImport.asStateFlow()

    /** Apps que deberían estar en cuarentena y están activas (tras reseteo o actualización). */
    private val _requarantine = MutableStateFlow<List<String>>(emptyList())
    val requarantine = _requarantine.asStateFlow()

    /** Permisos por app, cargados al usar el filtro por permisos. */
    private val _perms = MutableStateFlow<Map<String, Set<String>>?>(null)
    val perms = _perms.asStateFlow()

    init {
        viewModelScope.launch {
            Repo.detectMode()
            Repo.refresh()
            checkRequarantine()
            _loaded.value = true
        }
    }

    fun onResume() {
        if (!_loaded.value) return
        viewModelScope.launch {
            if (Repo.mode.value == Mode.NONE) Repo.detectMode()
            Repo.refresh()
            checkRequarantine()
        }
    }

    fun onStop() {
        if (Repo.config.value.settings.protectionEnabled) _locked.value = true
    }

    private fun checkRequarantine() {
        val c = Repo.requarantineCandidates()
        if (c.isNotEmpty() && Repo.config.value.settings.autoRequarantine) {
            viewModelScope.launch { Repo.setFrozen(c, true, silent = true) }
            _requarantine.value = emptyList()
        } else _requarantine.value = c
    }

    fun confirmRequarantine(yes: Boolean) {
        val list = _requarantine.value
        _requarantine.value = emptyList()
        if (yes) viewModelScope.launch { Repo.setFrozen(list, true) }
        else Repo.update(backup = true) { it.copy(quarantined = it.quarantined - list.toSet()) }
    }

    fun labelOf(pkg: String) = Repo.labelOf(pkg)

    fun redetect() = viewModelScope.launch { Repo.detectMode() }
    fun requestShizuku() = Freezer.requestShizukuPermission()
    fun refreshAsync() = viewModelScope.launch { Repo.refresh() }

    // ---------------- Íconos ----------------

    fun icon(pkg: String): ImageBitmap? = if (icons.has(pkg)) icons.get(pkg) else null

    suspend fun loadIcon(pkg: String): ImageBitmap? = withContext(Dispatchers.IO) {
        if (!icons.has(pkg)) icons.put(pkg, Repo.source.loadIconBitmap(pkg)?.asImageBitmap())
        icons.get(pkg)
    }

    // ---------------- Congelar ----------------

    fun toggle(pkg: String) = viewModelScope.launch { Repo.toggle(pkg) }
    fun setFrozen(pkgs: List<String>, frozen: Boolean) = viewModelScope.launch { Repo.setFrozen(pkgs, frozen) }
    fun setGroupFrozen(g: Group, frozen: Boolean) = viewModelScope.launch { Repo.setFrozen(g.packages, frozen) }
    fun launch(pkg: String) = viewModelScope.launch { Repo.launch(pkg) }
    fun openStore(pkg: String) = Repo.openStore(pkg)

    fun details(pkg: String): AppDetails? = Repo.source.details(pkg)

    fun loadPermissions() {
        if (_perms.value != null) return
        viewModelScope.launch {
            _perms.value = withContext(Dispatchers.IO) { Repo.source.permissionsOf(Repo.apps.value.map { it.pkg }) }
        }
    }

    // ---------------- Favoritos ----------------

    fun toggleFavorite(pkg: String) = Repo.update { c ->
        c.copy(favorites = if (pkg in c.favorites) c.favorites - pkg else c.favorites + pkg)
    }

    fun setFavorites(list: List<String>) = Repo.update { it.copy(favorites = list.distinct()) }

    // ---------------- Grupos ----------------

    fun createGroup(name: String, icon: String, color: Int): String {
        val g = Group(id = UUID.randomUUID().toString(), name = name.trim(), icon = icon, color = color)
        Repo.update { it.copy(groups = it.groups + g) }
        return g.id
    }

    fun updateGroup(id: String, name: String, icon: String, color: Int) =
        editGroup(id) { it.copy(name = name.trim(), icon = icon, color = color) }

    fun setGroupApps(id: String, pkgs: List<String>) = editGroup(id) { it.copy(packages = pkgs.distinct()) }
    fun addToGroup(id: String, pkg: String) = editGroup(id) { it.copy(packages = (it.packages + pkg).distinct()) }
    fun removeFromGroup(id: String, pkg: String) = editGroup(id) { it.copy(packages = it.packages - pkg) }

    /** Quita las apps pendientes (no instaladas). Con [groupId] null, de todos los grupos. */
    fun removePending(groupId: String?) {
        val installed = Repo.appMap().keys
        Repo.update { c ->
            c.copy(groups = c.groups.map { g ->
                if (groupId == null || g.id == groupId) g.copy(packages = g.packages.filter { it in installed }) else g
            })
        }
    }

    fun deleteGroup(id: String) = Repo.update { c -> c.copy(groups = c.groups.filterNot { it.id == id }) }

    fun moveGroup(id: String, delta: Int) = Repo.update { c ->
        val list = c.groups.toMutableList()
        val i = list.indexOfFirst { it.id == id }
        val j = i + delta
        if (i < 0 || j !in list.indices) c else { val g = list.removeAt(i); list.add(j, g); c.copy(groups = list) }
    }

    private fun editGroup(id: String, f: (Group) -> Group) =
        Repo.update { c -> c.copy(groups = c.groups.map { if (it.id == id) f(it) else it }) }

    // ---------------- Ajustes ----------------

    fun updateSettings(f: (AppSettings) -> AppSettings) {
        val prev = Repo.config.value.settings.modePref
        Repo.update { it.copy(settings = f(it.settings)) }
        if (Repo.config.value.settings.modePref != prev) redetect()
    }

    // ---------------- Contraseña ----------------

    fun unlock(password: String): Boolean {
        val s = Repo.config.value.settings
        val ok = Security.check(password, s.passwordSalt, s.passwordHash)
        if (ok) _locked.value = false
        return ok
    }

    fun setPassword(password: String) {
        val salt = Security.newSalt()
        updateSettings { it.copy(passwordSalt = salt, passwordHash = Security.hash(password, salt), protectionEnabled = true) }
        Repo.say("Contraseña guardada")
    }

    fun disableProtection() = updateSettings { it.copy(protectionEnabled = false, passwordHash = null, passwordSalt = null) }

    // ---------------- Backup ----------------

    fun exportBackup(uri: Uri) = viewModelScope.launch {
        val now = System.currentTimeMillis()
        val ok = withContext(Dispatchers.IO) { Repo.store.exportTo(uri, Repo.store.toBackupText(Repo.config.value, now)) }
        if (ok) { Repo.markBackup(now); Repo.say("Backup guardado") } else Repo.say("No se pudo guardar el backup")
    }

    fun importBackup(uri: Uri) = viewModelScope.launch {
        val c = withContext(Dispatchers.IO) { Repo.store.importFrom(uri) }
        if (c == null) Repo.say("Ese archivo no es un backup de QarentenApp") else _pendingImport.value = c
    }

    fun confirmImport(refreeze: Boolean) {
        val imported = _pendingImport.value ?: return
        _pendingImport.value = null
        val current = Repo.config.value
        val next = imported.copy(
            active = emptySet(),
            recent = emptyList(),
            widgets = current.widgets,
            settings = imported.settings.copy(
                autoBackupTree = current.settings.autoBackupTree,
                lastBackupAt = current.settings.lastBackupAt,
                // la contraseña no viaja en el backup
                protectionEnabled = current.settings.protectionEnabled,
                passwordHash = current.settings.passwordHash,
                passwordSalt = current.settings.passwordSalt,
            ),
        )
        Repo.commit(next)
        viewModelScope.launch {
            Repo.detectMode()
            if (refreeze) Repo.setFrozen(next.quarantined, true, silent = true)
            val pending = next.groups.flatMap { it.packages }.distinct().count { it !in Repo.appMap() }
            Repo.say(if (pending > 0) "Backup restaurado · $pending apps pendientes de instalar" else "Backup restaurado")
        }
    }

    fun cancelImport() { _pendingImport.value = null }

    fun setAutoBackupTree(uri: Uri?) {
        if (uri != null) runCatching {
            ctx.contentResolver.takePersistableUriPermission(
                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        }
        updateSettings { it.copy(autoBackupTree = uri?.toString()) }
    }

    // ---------------- Reporte de error ----------------

    fun errorReport(): String {
        val s = Repo.config.value
        val lines = synchronized(Repo.log) { Repo.log.toList() }
        return buildString {
            appendLine("QarentenApp ${app.qarentenapp.BuildConfig.VERSION_NAME}")
            appendLine("Android ${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})")
            appendLine("Equipo: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
            appendLine("Modo: ${Repo.mode.value} (preferencia ${s.settings.modePref})")
            appendLine("Grupos: ${s.groups.size} · En cuarentena: ${s.quarantined.size} · Activas: ${s.active.size}")
            appendLine()
            appendLine("Últimos comandos:")
            lines.forEach { appendLine(it) }
        }
    }
}
