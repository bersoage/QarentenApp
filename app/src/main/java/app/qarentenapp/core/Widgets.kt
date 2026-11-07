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

/** Widget chico (1×1): un grupo o una app, con su estado. */
class QWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        Repo.init(context)
        ids.forEach { Widgets.update(context, manager, it) }
    }

    override fun onDeleted(context: Context, ids: IntArray) {
        Repo.init(context)
        Repo.update(backup = false) { c -> c.copy(widgets = c.widgets - ids.map { it.toString() }.toSet()) }
    }
}

object Widgets {

    fun updateAll(context: Context) {
        val m = AppWidgetManager.getInstance(context)
        val ids = m.getAppWidgetIds(ComponentName(context, QWidgetProvider::class.java))
        ids.forEach { update(context, m, it) }
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
                    Triple(g.name, f, runCatching { Bitmaps.group(g, f) }.getOrNull())
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
        rv.setInt(R.id.w_root, "setBackgroundResource",
            if (conf.blackBackground) R.drawable.widget_bg_black else R.drawable.widget_bg_light)
        rv.setTextColor(R.id.w_label, if (conf.blackBackground) 0xFFFFFFFF.toInt() else 0xFF14202C.toInt())

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
