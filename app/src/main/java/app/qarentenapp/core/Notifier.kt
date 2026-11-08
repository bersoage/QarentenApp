package app.qarentenapp.core

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import app.qarentenapp.MainActivity
import app.qarentenapp.R
import app.qarentenapp.ui.ActionActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Notificación de apps activas (las que descongelaste desde Q'App), en grilla o lista,
 * y avisos de apps en cuarentena que se activaron solas (por una actualización).
 */
object Notifier {
    private const val CH_ACTIVE = "activas"
    private const val CH_UPDATES = "actualizaciones"
    private const val ID_ACTIVE = 1001
    private const val ID_UPDATES = 1002

    const val ACTION_FREEZE_ALL = "app.qarentenapp.FREEZE_ALL"
    const val ACTION_TOGGLE = "app.qarentenapp.N_TOGGLE"
    const val ACTION_REQUARANTINE = "app.qarentenapp.REQUARANTINE"
    const val ACTION_IGNORE_UPDATES = "app.qarentenapp.IGNORE_UPDATES"
    const val EXTRA_PKG = "pkg"
    const val EXTRA_PKGS = "pkgs"

    private fun allowed(context: Context) = Build.VERSION.SDK_INT < 33 ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun channels(context: Context) {
        if (Build.VERSION.SDK_INT < 26) return
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CH_ACTIVE, "Apps activas", NotificationManager.IMPORTANCE_LOW).apply {
            description = "Apps que descongelaste y podés volver a congelar"
        })
        nm.createNotificationChannel(NotificationChannel(CH_UPDATES, "Apps actualizadas", NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = "Apps en cuarentena que se activaron por una actualización"
        })
    }

    fun update(context: Context) {
        val c = Repo.config.value
        val s = c.settings
        val nm = NotificationManagerCompat.from(context)
        val map = Repo.appMap()
        val active = c.active.filter { it in map }.sortedBy { Repo.labelOf(it) }
        val shown = if (s.notifActiveOnly) active
        else (active + c.recent.filter { it in map && it !in active }).take(12)

        if (!s.notifyActive || active.isEmpty() || !allowed(context)) { nm.cancel(ID_ACTIVE); return }
        channels(context)

        val style = when (s.notifStyle) {
            "GRID" -> "GRID"
            "LIST" -> "LIST"
            else -> if (shown.size <= 3) "LIST" else "GRID"
        }
        val content = if (style == "GRID") grid(context, shown, s.notifSingleClick) else list(context, shown.take(3))

        val open = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val names = active.map { Repo.labelOf(it) }
        val b = NotificationCompat.Builder(context, CH_ACTIVE)
            .setSmallIcon(R.drawable.ic_notif)
            .setContentTitle(if (active.size == 1) "1 app activa" else "${active.size} apps activas")
            .setContentText(names.joinToString(", "))
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomBigContentView(content)
            .setContentIntent(open)
            .setOngoing(s.notifOngoing)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .addAction(0, "Congelar todas", broadcast(context, ACTION_FREEZE_ALL, null, 1))
        runCatching { nm.notify(ID_ACTIVE, b.build()) }
    }

    private fun grid(context: Context, pkgs: List<String>, singleClick: Boolean): RemoteViews {
        val rv = RemoteViews(context.packageName, R.layout.notif_grid)
        rv.removeAllViews(R.id.n_row1)
        rv.removeAllViews(R.id.n_row2)
        val map = Repo.appMap()
        pkgs.take(12).forEachIndexed { i, p ->
            val frozen = map[p]?.frozen == true
            val cell = RemoteViews(context.packageName, R.layout.notif_cell)
            runCatching { cell.setImageViewBitmap(R.id.c_icon, Bitmaps.app(p, frozen, 96)) }
            cell.setViewVisibility(R.id.c_status, if (frozen) View.VISIBLE else View.GONE)
            // Con "un toque": congela o activa al instante. Si no, abre la app.
            val pi = if (singleClick) broadcast(context, ACTION_TOGGLE, p, 100 + i) else launch(context, p, 100 + i)
            cell.setOnClickPendingIntent(R.id.c_root, pi)
            rv.addView(if (i < 6) R.id.n_row1 else R.id.n_row2, cell)
        }
        // Completar la fila para que los íconos no se estiren
        val rows = if (pkgs.size > 6) 2 else 1
        repeat(rows * 6 - pkgs.size.coerceAtMost(12)) {
            val empty = RemoteViews(context.packageName, R.layout.notif_cell)
            empty.setViewVisibility(R.id.c_status, View.GONE)
            rv.addView(if (pkgs.size + it < 6) R.id.n_row1 else R.id.n_row2, empty)
        }
        return rv
    }

    private fun list(context: Context, pkgs: List<String>): RemoteViews {
        val rv = RemoteViews(context.packageName, R.layout.notif_list)
        rv.removeAllViews(R.id.n_list)
        val map = Repo.appMap()
        pkgs.forEachIndexed { i, p ->
            val frozen = map[p]?.frozen == true
            val row = RemoteViews(context.packageName, R.layout.notif_row)
            runCatching { row.setImageViewBitmap(R.id.r_icon, Bitmaps.app(p, frozen, 96)) }
            row.setTextViewText(R.id.r_label, Repo.labelOf(p))
            row.setTextViewText(R.id.r_action, if (frozen) "🔥 Descongelar" else "❄️ Congelar")
            row.setOnClickPendingIntent(R.id.r_action, broadcast(context, ACTION_TOGGLE, p, 200 + i))
            row.setOnClickPendingIntent(R.id.r_root, launch(context, p, 300 + i))
            rv.addView(R.id.n_list, row)
        }
        return rv
    }

    /** Aviso: apps en cuarentena que aparecieron activas (actualización o reseteo). */
    fun updatesFound(context: Context, pkgs: List<String>) {
        if (pkgs.isEmpty() || !allowed(context)) return
        channels(context)
        val names = pkgs.map { Repo.labelOf(it) }
        val text = if (pkgs.size == 1) "${names[0]} estaba en cuarentena pero se actualizó y quedó activa."
        else "${pkgs.size} apps estaban en cuarentena y quedaron activas: ${names.joinToString(", ")}"
        val open = PendingIntent.getActivity(context, 2, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val n = NotificationCompat.Builder(context, CH_UPDATES)
            .setSmallIcon(R.drawable.ic_notif)
            .setContentTitle("¿Volver a poner en cuarentena?")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(open)
            .setAutoCancel(true)
            .addAction(0, "Volver a congelar", broadcastMany(context, ACTION_REQUARANTINE, pkgs, 3))
            .addAction(0, "Dejar activas", broadcastMany(context, ACTION_IGNORE_UPDATES, pkgs, 4))
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(ID_UPDATES, n) }
    }

    fun cancelUpdates(context: Context) = NotificationManagerCompat.from(context).cancel(ID_UPDATES)

    private fun broadcast(context: Context, action: String, pkg: String?, code: Int): PendingIntent {
        val i = Intent(context, NotificationActionReceiver::class.java).setAction(action)
        if (pkg != null) i.putExtra(EXTRA_PKG, pkg)
        return PendingIntent.getBroadcast(context, code, i, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun broadcastMany(context: Context, action: String, pkgs: List<String>, code: Int): PendingIntent {
        val i = Intent(context, NotificationActionReceiver::class.java).setAction(action)
            .putExtra(EXTRA_PKGS, pkgs.toTypedArray())
        return PendingIntent.getBroadcast(context, code, i, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun launch(context: Context, pkg: String, code: Int): PendingIntent {
        val i = Intent(context, ActionActivity::class.java).setAction(ActionActivity.ACTION_LAUNCH)
            .putExtra(ActionActivity.EXTRA_ID, pkg)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
        return PendingIntent.getActivity(context, code, i, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }
}

class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Repo.init(context)
        val result = goAsync()
        Repo.scope.launch(Dispatchers.IO) {
            try {
                if (Repo.apps.value.isEmpty()) Repo.refresh()
                when (intent.action) {
                    Notifier.ACTION_FREEZE_ALL -> Repo.setFrozen(Repo.config.value.active, true, Origin.NOTIFICATION)
                    Notifier.ACTION_TOGGLE -> intent.getStringExtra(Notifier.EXTRA_PKG)?.let { Repo.toggle(it, Origin.NOTIFICATION) }
                    Notifier.ACTION_REQUARANTINE -> {
                        Notifier.cancelUpdates(context)
                        intent.getStringArrayExtra(Notifier.EXTRA_PKGS)?.let { Repo.setFrozen(it.toList(), true, Origin.SYSTEM) }
                    }
                    Notifier.ACTION_IGNORE_UPDATES -> {
                        Notifier.cancelUpdates(context)
                        val list = intent.getStringArrayExtra(Notifier.EXTRA_PKGS)?.toSet() ?: emptySet()
                        Repo.update { it.copy(quarantined = it.quarantined - list) }
                    }
                }
            } finally {
                result.finish()
            }
        }
    }
}
