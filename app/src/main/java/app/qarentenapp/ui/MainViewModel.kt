package app.qarentenapp.ui

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.qarentenapp.core.Freezer
import app.qarentenapp.core.Mode
import app.qarentenapp.core.ModePref
import app.qarentenapp.core.Notifier
import app.qarentenapp.data.AppEntry
import app.qarentenapp.data.AppSettings
import app.qarentenapp.data.AppsSource
import app.qarentenapp.data.Config
import app.qarentenapp.data.Group
import app.qarentenapp.data.IconCache
import app.qarentenapp.data.Store
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.UUID

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val ctx get() = getApplication<Application>()
    private val store = Store(app)
    private val source = AppsSource(app)
    val icons = IconCache()
    private val lock = Mutex()

    private val _config = MutableStateFlow(store.load())
    val config = _config.asStateFlow()

    private val _apps = MutableStateFlow<List<AppEntry>>(emptyList())
    val apps = _apps.asStateFlow()

    /** null mientras detecta. */
    private val _mode = MutableStateFlow<Mode?>(null)
    val mode = _mode.asStateFlow()

    private val _loaded = MutableStateFlow(false)
    val loaded = _loaded.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy = _busy.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val messages = _messages.asSharedFlow()

    /** Backup recién importado, esperando que el usuario confirme si re-congela. */
    private val _pendingImport = MutableStateFlow<Config?>(null)
    val pendingImport = _pendingImport.asStateFlow()

    init {
        viewModelScope.launch {
            detectMode()
            refresh()
            reconcile()
            _loaded.value = true
        }
    }

    // ---------------- Estado ----------------

    fun appMap(): Map<String, AppEntry> = _apps.value.associateBy { it.pkg }

    fun labelOf(pkg: String): String = appMap()[pkg]?.label ?: pkg

    fun onResume() {
        if (!_loaded.value) return
        viewModelScope.launch {
            if (_mode.value == Mode.NONE || _mode.value == null) detectMode()
            refresh()
            reconcile()
        }
    }

    suspend fun detectMode() {
        _mode.value = null
        _mode.value = Freezer.detect(enumValueOf(_config.value.settings.modePref))
    }

    fun redetect() = viewModelScope.launch { detectMode() }

    fun requestShizuku() {
        Freezer.requestShizukuPermission()
    }

    suspend fun refresh() {
        _apps.value = withContext(Dispatchers.IO) { source.list() }
        // Las que el usuario congeló por fuera de la app también cuentan como "activas" solo si las activó Q'App
        val frozenNow = _apps.value.filter { it.frozen }.map { it.pkg }.toSet()
        val c = _config.value
        val active = c.active - frozenNow
        if (active != c.active) commit(c.copy(active = active), backup = false)
        else notifyActive()
    }

    fun refreshAsync() = viewModelScope.launch { refresh() }

    fun icon(pkg: String) = if (icons.has(pkg)) icons.get(pkg) else null

    suspend fun loadIcon(pkg: String) = withContext(Dispatchers.IO) {
        if (!icons.has(pkg)) icons.put(pkg, source.loadIcon(pkg))
        icons.get(pkg)
    }

    /**
     * Apps de un backup que antes no estaban instaladas y ahora sí: si estaban en cuarentena, se congelan.
     */
    private suspend fun reconcile() {
        val mode = _mode.value ?: return
        if (mode == Mode.NONE) return
        val map = appMap()
        val toFreeze = _config.value.quarantined.filter { p -> map[p]?.let { !it.frozen && !it.protected } == true }
            .filter { it !in _config.value.active }
        if (toFreeze.isNotEmpty()) setFrozen(toFreeze, true, silent = true)
    }

    // ---------------- Congelar ----------------

    fun toggle(pkg: String) {
        val e = appMap()[pkg] ?: return
        viewModelScope.launch { setFrozen(listOf(pkg), !e.frozen) }
    }

    fun setGroupFrozen(group: Group, frozen: Boolean) {
        viewModelScope.launch { setFrozen(group.packages, frozen) }
    }

    fun freezeAllActive() {
        viewModelScope.launch { setFrozen(_config.value.active.toList(), true) }
    }

    private suspend fun setFrozen(pkgs: List<String>, frozen: Boolean, silent: Boolean = false) = lock.withLock {
        val mode = _mode.value
        if (mode == null || mode == Mode.NONE) {
            _messages.tryEmit("No hay root ni Shizuku activos")
            return@withLock
        }
        val map = appMap()
        val targets = pkgs.distinct().filter { p ->
            val e = map[p] ?: return@filter false
            if (e.protected) return@filter false
            e.frozen != frozen
        }
        val skippedProtected = pkgs.count { map[it]?.protected == true }
        if (targets.isEmpty()) {
            if (skippedProtected > 0 && !silent) _messages.tryEmit("Esa app está protegida y no se puede congelar")
            return@withLock
        }
        _busy.value = true
        val cmds = targets.map { if (frozen) Freezer.freezeCmd(it) else Freezer.unfreezeCmd(it) }
        val res = Freezer.run(mode, cmds)
        refreshInternal()
        val nowMap = appMap()
        val done = targets.filter { nowMap[it]?.frozen == frozen }
        val c = _config.value
        val next = if (frozen) c.copy(quarantined = c.quarantined + done, active = c.active - done.toSet())
        else c.copy(quarantined = c.quarantined - done.toSet(), active = c.active + done)
        commit(next)
        _busy.value = false
        if (done.size < targets.size && !silent) {
            val failed = targets.size - done.size
            _messages.tryEmit(
                if (failed == 1) "No se pudo cambiar 1 app" + (if (!res.ok) ": ${res.output.take(80)}" else "")
                else "No se pudieron cambiar $failed apps"
            )
        }
    }

    private suspend fun refreshInternal() {
        _apps.value = withContext(Dispatchers.IO) { source.list() }
    }

    fun launch(pkg: String) {
        viewModelScope.launch {
            val e = appMap()[pkg] ?: return@launch
            if (e.frozen) setFrozen(listOf(pkg), false)
            val intent = source.launchIntent(pkg)
            if (intent == null) {
                _messages.tryEmit("Esa app no se puede abrir directamente")
                return@launch
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            runCatching { ctx.startActivity(intent) }
        }
    }

    fun openStore(pkg: String) {
        val i = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$pkg")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { ctx.startActivity(i) }.onFailure {
            runCatching {
                ctx.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$pkg"))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
        }
    }

    // ---------------- Grupos ----------------

    fun createGroup(name: String, icon: String): String {
        val g = Group(id = UUID.randomUUID().toString(), name = name.trim(), icon = icon)
        commit(_config.value.let { it.copy(groups = it.groups + g) })
        return g.id
    }

    fun updateGroup(id: String, name: String, icon: String) = editGroup(id) { it.copy(name = name.trim(), icon = icon) }

    fun setGroupApps(id: String, pkgs: List<String>) = editGroup(id) { it.copy(packages = pkgs.distinct()) }

    fun removeFromGroup(id: String, pkg: String) = editGroup(id) { it.copy(packages = it.packages - pkg) }

    /** Quita las apps pendientes (no instaladas). Con [groupId] null, de todos los grupos. */
    fun removePending(groupId: String?) {
        val installed = appMap().keys
        val c = _config.value
        commit(c.copy(groups = c.groups.map { g ->
            if (groupId == null || g.id == groupId) g.copy(packages = g.packages.filter { it in installed }) else g
        }))
    }

    fun deleteGroup(id: String) = commit(_config.value.let { c -> c.copy(groups = c.groups.filterNot { it.id == id }) })

    fun moveGroup(id: String, delta: Int) {
        val list = _config.value.groups.toMutableList()
        val i = list.indexOfFirst { it.id == id }
        val j = i + delta
        if (i < 0 || j !in list.indices) return
        val g = list.removeAt(i); list.add(j, g)
        commit(_config.value.copy(groups = list))
    }

    private fun editGroup(id: String, f: (Group) -> Group) {
        val c = _config.value
        commit(c.copy(groups = c.groups.map { if (it.id == id) f(it) else it }))
    }

    // ---------------- Ajustes ----------------

    fun updateSettings(f: (AppSettings) -> AppSettings) {
        val c = _config.value
        val prevPref = c.settings.modePref
        commit(c.copy(settings = f(c.settings)), backup = true)
        if (_config.value.settings.modePref != prevPref) redetect()
        notifyActive()
    }

    // ---------------- Backup ----------------

    fun exportBackup(uri: Uri) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val ok = withContext(Dispatchers.IO) { store.exportTo(uri, store.toBackupText(_config.value, now)) }
            if (ok) {
                markBackup(now)
                _messages.tryEmit("Backup guardado")
            } else _messages.tryEmit("No se pudo guardar el backup")
        }
    }

    fun importBackup(uri: Uri) {
        viewModelScope.launch {
            val c = withContext(Dispatchers.IO) { store.importFrom(uri) }
            if (c == null) _messages.tryEmit("Ese archivo no es un backup de QarentenApp")
            else _pendingImport.value = c
        }
    }

    /** Aplica el backup importado. Con [refreeze], congela las apps que estaban en cuarentena. */
    fun confirmImport(refreeze: Boolean) {
        val imported = _pendingImport.value ?: return
        _pendingImport.value = null
        val current = _config.value
        val next = imported.copy(
            active = emptySet(),
            // la carpeta de backup automático es de este celular, no del archivo
            settings = imported.settings.copy(
                autoBackupTree = current.settings.autoBackupTree,
                lastBackupAt = current.settings.lastBackupAt,
            ),
        )
        commit(next)
        viewModelScope.launch {
            detectMode()
            if (refreeze) setFrozen(next.quarantined.toList(), true, silent = true)
            val pending = next.groups.flatMap { it.packages }.distinct().count { it !in appMap() }
            _messages.tryEmit(
                if (pending > 0) "Backup restaurado · $pending apps pendientes de instalar" else "Backup restaurado"
            )
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

    private fun markBackup(now: Long) {
        val c = _config.value
        _config.value = c.copy(settings = c.settings.copy(lastBackupAt = now))
        store.save(_config.value)
    }

    // ---------------- Persistencia ----------------

    private fun commit(next: Config, backup: Boolean = true) {
        _config.value = next
        store.save(next)
        notifyActive()
        val tree = next.settings.autoBackupTree
        if (backup && tree != null) {
            viewModelScope.launch {
                val now = System.currentTimeMillis()
                val ok = withContext(Dispatchers.IO) { store.writeToTree(tree, store.toBackupText(next, now)) }
                if (ok) markBackup(now)
            }
        }
    }

    private fun notifyActive() {
        val c = _config.value
        Notifier.update(ctx, c.active, ::labelOf, c.settings.notifyActive)
    }
}
