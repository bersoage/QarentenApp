package app.qarentenapp.core

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

/**
 * Vigila las apps en cuarentena: si una se activó sola (por una actualización del sistema
 * o de Play Store, o tras un reseteo), avisa o la vuelve a congelar según los ajustes.
 */
object UpdateWatcher {
    private const val PERIODIC = "qapp-watch"
    private const val ONCE = "qapp-watch-once"

    fun schedule(context: Context) {
        runCatching {
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC, ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<WatchWorker>(1, TimeUnit.HOURS).build()
            )
        }
    }

    fun runSoon(context: Context) {
        runCatching {
            WorkManager.getInstance(context).enqueueUniqueWork(
                ONCE, ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<WatchWorker>().setInitialDelay(20, TimeUnit.SECONDS).build()
            )
        }
    }
}

class WatchWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        Repo.init(applicationContext)
        val s = Repo.config.value.settings
        if (!s.monitorUpdates || Repo.config.value.quarantined.isEmpty()) return Result.success()
        Repo.refresh()
        val found = Repo.requarantineCandidates()
        if (found.isEmpty()) return Result.success()
        if (s.autoRequarantine) Repo.setFrozen(found, true, Origin.SYSTEM, silent = true)
        else Notifier.updatesFound(applicationContext, found)
        return Result.success()
    }
}

/** Al prender el celular o al actualizar Q'App: revisar la cuarentena. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED, "android.intent.action.QUICKBOOT_POWERON" -> {
                UpdateWatcher.schedule(context)
                UpdateWatcher.runSoon(context)
            }
        }
    }
}
