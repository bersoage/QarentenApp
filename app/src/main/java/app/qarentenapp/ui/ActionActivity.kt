package app.qarentenapp.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import app.qarentenapp.core.GroupLock
import app.qarentenapp.core.Origin
import app.qarentenapp.core.Repo
import app.qarentenapp.core.Security
import kotlinx.coroutines.launch

/**
 * Trampolín sin interfaz para accesos directos y widgets.
 * Si hay contraseña activada, la pide antes de actuar.
 */
class ActionActivity : FragmentActivity() {

    companion object {
        const val ACTION_TOGGLE_GROUP = "app.qarentenapp.TOGGLE_GROUP"
        const val ACTION_TOGGLE_APP = "app.qarentenapp.TOGGLE_APP"
        const val ACTION_LAUNCH = "app.qarentenapp.LAUNCH"
        const val ACTION_WIDGET = "app.qarentenapp.WIDGET"
        const val EXTRA_ID = "id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Repo.init(this)
        val s = Repo.config.value.settings
        // Si la acción toca un grupo protegido, la huella o contraseña del grupo reemplaza a la contraseña general
        val groupAuth = groupAuthTitle(intent)
        when {
            groupAuth != null -> setContent { QTheme { AuthGate(auth) } }.also {
                auth.ask(groupAuth.first, groupAuth.second, onDenied = { finish() }) { perform(intent) }
            }
            Security.appLocked(s) -> setContent { QTheme { AuthGate(auth) } }.also {
                auth.ask("Desbloquear Q'App", onDenied = { finish() }) { perform(intent) }
            }
            else -> perform(intent)
        }
    }

    private val auth = AuthState()

    /** Título del pedido si esta acción necesita desbloquear un grupo protegido; null si no. */
    private fun groupAuthTitle(intent: Intent): Pair<String, String?>? {
        val id = intent.getStringExtra(EXTRA_ID) ?: return null
        val groups = Repo.config.value.groups
        fun forGroup(gid: String) = groups.firstOrNull { it.id == gid }?.takeIf { GroupLock.needsForGroup(it) }
            ?.let { "Desbloquear «${it.name}»" to GroupLock.methodOf(it) }
        fun forApp(pkg: String) = if (GroupLock.needsForApps(listOf(pkg))) "Desbloquear ${Repo.labelOf(pkg)}" to GroupLock.methodForApps(listOf(pkg)) else null
        return when (intent.action) {
            ACTION_TOGGLE_GROUP -> forGroup(id)
            ACTION_TOGGLE_APP -> forApp(id)
            ACTION_WIDGET -> {
                val conf = Repo.config.value.widgets[id] ?: return null
                when {
                    conf.kind == "group" && conf.quickToggle -> forGroup(conf.target)
                    conf.kind == "group" -> null            // la ventana del grupo pide su propio ingreso
                    conf.directLaunch -> null
                    else -> forApp(conf.target)
                }
            }
            else -> null
        }
    }

    private fun perform(intent: Intent) {
        val id = intent.getStringExtra(EXTRA_ID)
        if (id == null) { finish(); return }
        lifecycleScope.launch {
            when (intent.action) {
                ACTION_TOGGLE_GROUP -> toggleGroup(id, Origin.SHORTCUT)
                ACTION_TOGGLE_APP -> toggleApp(id, Origin.SHORTCUT)
                ACTION_LAUNCH -> Repo.launch(id, Origin.SHORTCUT)
                ACTION_WIDGET -> widget(id)
            }
            if (!waiting) finish()
        }
    }

    private suspend fun widget(widgetId: String) {
        val conf = Repo.config.value.widgets[widgetId]
        if (conf == null) {
            startActivity(
                Intent(this, WidgetConfigActivity::class.java)
                    .putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId.toInt())
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            return
        }
        when {
            conf.kind == "group" && conf.quickToggle -> toggleGroup(conf.target, Origin.WIDGET)
            conf.kind == "group" -> startActivity(
                Intent(this, PopupActivity::class.java)
                    .putExtra(PopupActivity.EXTRA_MODE, PopupActivity.MODE_GROUP)
                    .putExtra(PopupActivity.EXTRA_ID, conf.target)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            conf.directLaunch -> Repo.launch(conf.target, Origin.WIDGET)
            else -> toggleApp(conf.target, Origin.WIDGET)
        }
    }

    /** Esperando que el usuario confirme el aviso de apps del sistema. */
    private var waiting = false

    /** Si hay apps del sistema para congelar, muestra el aviso y sigue solo si confirma. */
    private fun needsConfirm(pkgs: List<String>, frozen: Boolean, then: suspend () -> Unit): Boolean {
        val sys = Repo.systemToConfirm(pkgs, frozen)
        if (sys.isEmpty()) return false
        waiting = true
        var confirmed = false
        val req = SystemConfirm(pkgs, sys) { confirmed = true; lifecycleScope.launch { then(); finish() } }
        setContent { QTheme { SystemConfirmDialog(req, onDismiss = { if (!confirmed) finish() }) } }
        return true
    }

    private suspend fun toggleGroup(id: String, origin: Origin, confirmed: Boolean = false) {
        if (Repo.apps.value.isEmpty()) Repo.refresh()
        val g = Repo.config.value.groups.firstOrNull { it.id == id } ?: return toast("Ese grupo ya no existe")
        val name = g.name
        if (!confirmed && needsConfirm(g.packages, !Repo.groupFrozen(g)) { toggleGroup(id, origin, true) }) return
        when (Repo.toggleGroup(id, origin)) {
            true -> toast("«$name» congelado")
            false -> toast("«$name» activo")
            null -> {}
        }
    }

    private suspend fun toggleApp(pkg: String, origin: Origin, confirmed: Boolean = false) {
        if (Repo.apps.value.isEmpty()) Repo.refresh()
        val e = Repo.appMap()[pkg]
        if (!confirmed && e != null && needsConfirm(listOf(pkg), !e.frozen) { toggleApp(pkg, origin, true) }) return
        when (Repo.toggle(pkg, origin)) {
            true -> toast("${Repo.labelOf(pkg)} congelada")
            false -> toast("${Repo.labelOf(pkg)} activa")
            null -> toast("Esa app no está instalada")
        }
    }

    private fun toast(msg: String) = Toast.makeText(applicationContext, msg, Toast.LENGTH_SHORT).show()
}
