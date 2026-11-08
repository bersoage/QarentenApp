package app.qarentenapp.core

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import app.qarentenapp.R
import app.qarentenapp.ui.ActionActivity

/** Widget chico (1×1): un grupo o una app, con su estado. Base de los tres tipos de widget. */
open class QWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        Repo.init(context)
        ids.forEach { Widgets.update(context, manager, it) }
    }

    override fun onDeleted(context: Context, ids: IntArray) {
        Repo.init(context)
        Repo.update(backup = false) { c -> c.copy(widgets = c.widgets - ids.map { it.toString() }.toSet()) }
    }
}

/** «Q'App · App»: congela o descongela una app (o la abre, con lanzamiento directo). */
class AppWidget : QWidgetProvider()
/** «Q'App · Grupo»: congela o descongela el grupo entero de un toque. */
class GroupWidget : QWidgetProvider()
/** «Q'App · Ventana de grupo»: abre la ventana flotante del grupo, como el acceso directo. */
class GroupWindowWidget : QWidgetProvider()

object Widgets {

    private val providers = listOf(
        QWidgetProvider::class.java, AppWidget::class.java, GroupWidget::class.java, GroupWindowWidget::class.java,
    )

    /** Tipo fijo según el widget elegido en la lista del celular: "app", "group", "groupwin" o null (widget de la beta vieja). */
    fun fixedKind(context: Context, id: Int): String? {
        val cls = AppWidgetManager.getInstance(context).getAppWidgetInfo(id)?.provider?.className ?: return null
        return when (cls) {
            AppWidget::class.java.name -> "app"
            GroupWidget::class.java.name -> "group"
            GroupWindowWidget::class.java.name -> "groupwin"
            else -> null
        }
    }

    fun updateAll(context: Context) {
        val m = AppWidgetManager.getInstance(context)
        providers.forEach { cls ->
            m.getAppWidgetIds(ComponentName(context, cls)).forEach { update(context, m, it) }
        }
    }

    fun update(context: Context, manager: AppWidgetManager, id: Int) {
        val conf = Repo.config.value.widgets[id.toString()]
        val rv = RemoteViews(context.packageName, R.layout.widget_small)
        if (conf == null) {
            rv.setImageViewResource(R.id.w_icon, R.drawable.logo_candado)
            rv.setTextViewText(R.id.w_label, "Configurar")
            rv.setViewVisibility(R.id.w_status, View.GONE)
            manager.updateAppWidget(id, rv)
            return
        }
        val map = Repo.appMap()
        val (label, frozen, bitmap) = when (conf.kind) {
            "group" -> {
                val g = Repo.config.value.groups.firstOrNull { it.id == conf.target }
                if (g == null) Triple("Grupo borrado", false, null)
                else {
                    val f = Repo.groupFrozen(g)
                    Triple(g.name, f, runCatching { Bitmaps.group(g, f, 192) }.getOrNull())
                }
            }
            else -> {
                val f = map[conf.target]?.frozen ?: Repo.source.isFrozen(conf.target)
                Triple(Repo.labelOf(conf.target), f, runCatching { Bitmaps.app(conf.target, f) }.getOrNull())
            }
        }
        if (bitmap != null) rv.setImageViewBitmap(R.id.w_icon, bitmap) else rv.setImageViewResource(R.id.w_icon, R.drawable.logo_candado)
        rv.setTextViewText(R.id.w_label, label)
        rv.setViewVisibility(R.id.w_label, if (conf.hideCaption) View.GONE else View.VISIBLE)
        rv.setViewVisibility(R.id.w_status, if (!conf.hideStatus && frozen) View.VISIBLE else View.GONE)
        // El ícono de grupo ya trae su propio fondo con carámbanos
        rv.setInt(R.id.w_root, "setBackgroundResource", when {
            conf.blackBackground -> R.drawable.widget_bg_black
            conf.kind == "group" -> android.R.color.transparent
            else -> R.drawable.widget_bg_light
        })
        if (conf.kind == "group") rv.setViewVisibility(R.id.w_status, android.view.View.GONE)
        // Sobre el fondo de pantalla (grupo sin fondo) el nombre va en blanco, como en el launcher
        rv.setTextColor(R.id.w_label, if (conf.blackBackground || conf.kind == "group") 0xFFFFFFFF.toInt() else 0xFF14202C.toInt())

        val tap = Intent(context, ActionActivity::class.java)
            .setAction(ActionActivity.ACTION_WIDGET)
            .putExtra(ActionActivity.EXTRA_ID, id.toString())
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
        rv.setOnClickPendingIntent(
            R.id.w_root,
            PendingIntent.getActivity(context, id, tap, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        )
        manager.updateAppWidget(id, rv)
    }
}
