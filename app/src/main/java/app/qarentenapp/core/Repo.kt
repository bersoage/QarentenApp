package app.qarentenapp.core

import android.content.Context
import android.content.Intent
import android.net.Uri
import app.qarentenapp.data.AppEntry
import app.qarentenapp.data.AppsSource
import app.qarentenapp.data.Config
import app.qarentenapp.data.Group
import app.qarentenapp.data.Store
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** De dónde vino un cambio de estado. */
enum class Origin { APP, WIDGET, NOTIFICATION, SHORTCUT, SYSTEM }

/**
 * Núcleo compartido: lo usan las pantallas, los widgets, los accesos directos,
 * la notificación y las tareas en segundo plano.
 */
object Repo {
    lateinit var ctx: Context
        private set
    lateinit var store: Store
        private set
    lateinit var source: AppsSource
        private set

    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val lock = Mutex()
    private var initialized = false

    private val _config = MutableStateFlow(Config())
    val config = _config.asStateFlow()

    private val _apps = MutableStateFlow<List<AppEntry>>(emptyList())
    val apps = _apps.asStateFlow()

    /** null mientras detecta. */
    private val _mode = MutableStateFlow<Mode?>(null)
    val mode = _mode.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy = _busy.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val messages = _messages.asSharedFlow()

    /** Registro de los últimos comandos (para el reporte de error). */
    val log = ArrayDeque<String>()

    @Synchronized
    fun init(context: Context) {
        if (initialized) return
        ctx = context.applicationContext
        store = Store(ctx)
        source = AppsSource(ctx)
        _config.value = store.load()
        initialized = true
    }

    fun say(msg: String) { _messages.tryEmit(msg) }

    fun appMap(): Map<String, AppEntry> = _apps.value.associateBy { it.pkg }

    fun labelOf(pkg: String): String = appMap()[pkg]?.label ?: source.label(pkg)

    // ---------------- Modo ----------------

    suspend fun detectMode(): Mode {
        _mode.value = null
        val m = Freezer.detect(enumValueOf(_config.value.settings.modePref))
        _mode.value = m
        return m
    }

    suspend fun ensureMode(): Mode {
        val m = _mode.value
        return if (m == null || m == Mode.NONE) detectMode() else m
    }

    // ---------------- Apps ----------------

    suspend fun refresh() {
        _apps.value = withContext(Dispatchers.IO) { source.list() }
        val frozenNow = _apps.value.filter { it.frozen }.map { it.pkg }.toSet()
        val c = _config.value
        val active = c.active - frozenNow
        if (active != c.active) commit(c.copy(active = active), backup = false) else sideEffects(c)
    }

    /**
     * Congela o descongela. Devuelve cuántas apps cambiaron.
     */
    suspend fun setFrozen(pkgs: Collection<String>, frozen: Boolean, origin: Origin = Origin.APP, silent: Boolean = false): Int =
        lock.withLock {
            val mode = ensureMode()
            if (mode == Mode.NONE) {
                if (!silent) say("No hay root ni Shizuku activos")
                return@withLock 0
            }
            if (_apps.value.isEmpty()) _apps.value = withContext(Dispatchers.IO) { source.list() }
            val map = appMap()
            val targets = pkgs.distinct().filter { p ->
                val e = map[p] ?: return@filter false
                !e.protected && e.frozen != frozen
            }
            if (targets.isEmpty()) {
                if (!silent && pkgs.any { map[it]?.protected == true }) say("Esa app está protegida y no se puede congelar")
                return@withLock 0
            }
            _busy.value = true
            val cmds = targets.map { if (frozen) Freezer.freezeCmd(it) else Freezer.unfreezeCmd(it) }.toMutableList()
            if (_config.value.settings.forceRestartLauncher) source.currentLauncher()?.let { cmds += "am force-stop $it" }
            val res = Freezer.run(mode, cmds)
            addLog((if (frozen) "congelar " else "activar ") + targets.joinToString(","), res)
            _apps.value = withContext(Dispatchers.IO) { source.list() }
            val nowMap = appMap()
            val done = targets.filter { nowMap[it]?.frozen == frozen }
            val c = _config.value
            var next = if (frozen) c.copy(quarantined = c.quarantined + done, active = c.active - done.toSet())
            else c.copy(quarantined = c.quarantined - done.toSet(), active = c.active + done)
            if (origin == Origin.WIDGET || origin == Origin.NOTIFICATION || origin == Origin.SHORTCUT) {
                next = next.copy(recent = (done + next.recent).distinct().take(12))
            }
            commit(next)
            _busy.value = false
            if (done.size < targets.size && !silent) {
                val failed = targets.size - done.size
                say(if (failed == 1) "No se pudo cambiar 1 app" else "No se pudieron cambiar $failed apps")
            }
            done.size
        }

    suspend fun toggle(pkg: String, origin: Origin = Origin.APP): Boolean? {
        if (_apps.value.isEmpty()) refresh()
        val e = appMap()[pkg] ?: return null
        setFrozen(listOf(pkg), !e.frozen, origin)
        return appMap()[pkg]?.frozen
    }

    /**
     * Apps del sistema que hay que confirmar antes de congelar
     * (activas, no protegidas y sin "no volver a preguntar").
     */
    fun systemToConfirm(pkgs: Collection<String>, frozen: Boolean): List<String> {
        if (!frozen) return emptyList()
        val map = appMap()
        val acked = _config.value.systemAcked
        return pkgs.distinct().filter { p -> map[p]?.let { it.isSystem && !it.protected && !it.frozen } == true && p !in acked }
    }

    fun ackSystem(pkgs: Collection<String>) = update { it.copy(systemAcked = it.systemAcked + pkgs) }

    fun groupFrozen(g: Group): Boolean {
        val map = appMap()
        val installed = g.packages.mapNotNull { map[it] }.filterNot { it.protected }
        return installed.isNotEmpty() && installed.all { it.frozen }
    }

    /** Congela el grupo si no está todo congelado; si lo está, lo activa. Devuelve el estado nuevo. */
    suspend fun toggleGroup(groupId: String, origin: Origin = Origin.APP): Boolean? {
        if (_apps.value.isEmpty()) refresh()
        val g = _config.value.groups.firstOrNull { it.id == groupId } ?: return null
        val target = !groupFrozen(g)
        setFrozen(g.packages, target, origin)
        return groupFrozen(g)
    }

    /** Descongela (si hace falta) y abre la app. */
    suspend fun launch(pkg: String, origin: Origin = Origin.APP): Boolean {
        if (_apps.value.isEmpty()) refresh()
        val e = appMap()[pkg] ?: return false
        if (e.frozen) setFrozen(listOf(pkg), false, origin)
        val intent = source.launchIntent(pkg) ?: run { say("Esa app no se puede abrir directamente"); return false }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return withContext(Dispatchers.Main) { runCatching { ctx.startActivity(intent) }.isSuccess }
    }

    fun openStore(pkg: String) {
        val market = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$pkg")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { ctx.startActivity(market) }.onFailure {
            runCatching {
                ctx.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$pkg"))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
        }
    }

    /**
     * Apps que deberían estar en cuarentena pero están activas sin que Q'App las haya activado
     * (después de un reseteo, una restauración o una actualización).
     */
    fun requarantineCandidates(): List<String> {
        val map = appMap()
        val c = _config.value
        return c.quarantined.filter { p -> map[p]?.let { !it.frozen && !it.protected } == true && p !in c.active }
    }

    // ---------------- Persistencia ----------------

    fun update(backup: Boolean = true, f: (Config) -> Config) = commit(f(_config.value), backup)

    fun commit(next: Config, backup: Boolean = true) {
        _config.value = next
        store.save(next)
        sideEffects(next)
        val tree = next.settings.autoBackupTree
        if (backup && tree != null) scope.launch(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            if (store.writeToTree(tree, store.toBackupText(next, now))) markBackup(now)
        }
    }

    fun markBackup(now: Long) {
        val c = _config.value
        _config.value = c.copy(settings = c.settings.copy(lastBackupAt = now))
        store.save(_config.value)
    }

    private fun sideEffects(c: Config) {
        scope.launch(Dispatchers.IO) {
            runCatching { Notifier.update(ctx) }
            runCatching { Widgets.updateAll(ctx) }
            runCatching { Shortcuts.updateDynamic(ctx) }
        }
    }

    private fun addLog(what: String, r: ShellResult) {
        synchronized(log) {
            log.addLast("${java.text.SimpleDateFormat("dd/MM HH:mm:ss", java.util.Locale.US).format(java.util.Date())} " +
                "[${_mode.value}] $what → ${if (r.ok) "ok" else "ERROR"} ${r.output.take(300).trim()}")
            while (log.size > 50) log.removeFirst()
        }
    }
}
