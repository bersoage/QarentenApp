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
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import app.qarentenapp.MainActivity
import app.qarentenapp.R
import app.qarentenapp.data.Store
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Notificación de "apps activas": las que descongelaste desde Q'App, para volver a congelarlas de un toque. */
object Notifier {
    private const val CHANNEL = "activas"
    private const val ID = 1001
    const val ACTION_FREEZE_ALL = "app.qarentenapp.FREEZE_ALL"
    const val ACTION_FREEZE_ONE = "app.qarentenapp.FREEZE_ONE"
    const val EXTRA_PKG = "pkg"

    fun update(context: Context, active: Set<String>, labelOf: (String) -> String, enabled: Boolean) {
        val nm = NotificationManagerCompat.from(context)
        if (!enabled || active.isEmpty()) { nm.cancel(ID); return }
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        if (Build.VERSION.SDK_INT >= 26) {
            val ch = NotificationChannel(CHANNEL, "Apps activas", NotificationManager.IMPORTANCE_LOW)
            ch.description = "Apps que descongelaste y podés volver a congelar"
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
        }

        val names = active.map(labelOf).sorted()
        val open = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val b = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notif)
            .setContentTitle(if (names.size == 1) "1 app activa" else "${names.size} apps activas")
            .setContentText(names.joinToString(", "))
            .setStyle(NotificationCompat.BigTextStyle().bigText(names.joinToString("\n")))
            .setContentIntent(open)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(0, "Congelar todas", pending(context, ACTION_FREEZE_ALL, null, 1))

        // Hasta 2 botones individuales (Android muestra 3 como máximo)
        active.sortedBy(labelOf).take(2).forEachIndexed { i, pkg ->
            b.addAction(0, "Congelar ${labelOf(pkg)}", pending(context, ACTION_FREEZE_ONE, pkg, 10 + i))
        }
        runCatching { nm.notify(ID, b.build()) }
    }

    private fun pending(context: Context, action: String, pkg: String?, code: Int): PendingIntent {
        val i = Intent(context, NotificationActionReceiver::class.java).setAction(action)
        if (pkg != null) i.putExtra(EXTRA_PKG, pkg)
        return PendingIntent.getBroadcast(context, code, i, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }
}

class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val result = goAsync()
        val app = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val store = Store(app)
                val cfg = store.load()
                val targets = when (intent.action) {
                    Notifier.ACTION_FREEZE_ALL -> cfg.active
                    Notifier.ACTION_FREEZE_ONE -> setOfNotNull(intent.getStringExtra(Notifier.EXTRA_PKG))
                    else -> emptySet()
                }
                val mode = Freezer.detect(enumValueOf(cfg.settings.modePref))
                val res = Freezer.run(mode, targets.map(Freezer::freezeCmd))
                if (res.ok) {
                    val next = cfg.copy(active = cfg.active - targets, quarantined = cfg.quarantined + targets)
                    store.save(next)
                    val pm = app.packageManager
                    Notifier.update(app, next.active, { p ->
                        runCatching { pm.getApplicationInfo(p, PackageManager.MATCH_DISABLED_COMPONENTS).loadLabel(pm).toString() }
                            .getOrDefault(p)
                    }, next.settings.notifyActive)
                }
            } finally {
                result.finish()
            }
        }
    }
}
