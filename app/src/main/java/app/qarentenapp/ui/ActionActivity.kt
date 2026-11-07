package app.qarentenapp.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import app.qarentenapp.core.Origin
import app.qarentenapp.core.Repo
import kotlinx.coroutines.launch

/**
 * Trampolín sin interfaz para accesos directos y widgets.
 * Si hay contraseña activada, la pide antes de actuar.
 */
class ActionActivity : ComponentActivity() {

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
        if (s.protectionEnabled && s.passwordHash != null) {
            setContent { QTheme { PasswordDialog(onCancel = { finish() }, onOk = { perform(intent) }) } }
        } else perform(intent)
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
            finish()
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
            conf.kind == "group" && conf.directLaunch -> startActivity(
                Intent(this, PopupActivity::class.java)
                    .putExtra(PopupActivity.EXTRA_MODE, PopupActivity.MODE_GROUP)
                    .putExtra(PopupActivity.EXTRA_ID, conf.target)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            conf.kind == "group" -> toggleGroup(conf.target, Origin.WIDGET)
            conf.directLaunch -> Repo.launch(conf.target, Origin.WIDGET)
            else -> toggleApp(conf.target, Origin.WIDGET)
        }
    }

    private suspend fun toggleGroup(id: String, origin: Origin) {
        val name = Repo.config.value.groups.firstOrNull { it.id == id }?.name ?: return toast("Ese grupo ya no existe")
        when (Repo.toggleGroup(id, origin)) {
            true -> toast("«$name» congelado")
            false -> toast("«$name» activo")
            null -> {}
        }
    }

    private suspend fun toggleApp(pkg: String, origin: Origin) {
        when (Repo.toggle(pkg, origin)) {
            true -> toast("${Repo.labelOf(pkg)} congelada")
            false -> toast("${Repo.labelOf(pkg)} activa")
            null -> toast("Esa app no está instalada")
        }
    }

    private fun toast(msg: String) = Toast.makeText(applicationContext, msg, Toast.LENGTH_SHORT).show()
}
