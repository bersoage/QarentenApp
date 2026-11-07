package app.qarentenapp.data

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import app.qarentenapp.core.Protected

/** Lee las apps instaladas y su estado (congelada o no). */
class AppsSource(private val context: Context) {

    private val pm: PackageManager get() = context.packageManager

    fun list(): List<AppEntry> {
        val protected = Protected.set(context)
        @Suppress("DEPRECATION")
        val apps = pm.getInstalledApplications(PackageManager.MATCH_DISABLED_COMPONENTS)
        return apps.map { ai ->
            AppEntry(
                pkg = ai.packageName,
                label = runCatching { ai.loadLabel(pm).toString() }.getOrDefault(ai.packageName),
                isSystem = (ai.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                frozen = isFrozen(ai),
                protected = ai.packageName in protected,
            )
        }.sortedBy { it.label.lowercase() }
    }

    fun isInstalled(pkg: String): Boolean = runCatching {
        pm.getApplicationInfo(pkg, PackageManager.MATCH_DISABLED_COMPONENTS); true
    }.getOrDefault(false)

    private fun isFrozen(ai: ApplicationInfo): Boolean {
        if (!ai.enabled) return true
        return runCatching {
            val s = pm.getApplicationEnabledSetting(ai.packageName)
            s == PackageManager.COMPONENT_ENABLED_STATE_DISABLED ||
                s == PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER
        }.getOrDefault(false)
    }

    fun loadIcon(pkg: String, sizePx: Int = 112): ImageBitmap? = runCatching {
        val d = pm.getApplicationIcon(pkg)
        val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        d.setBounds(0, 0, sizePx, sizePx)
        d.draw(c)
        bmp.asImageBitmap()
    }.getOrNull()

    fun launchIntent(pkg: String) = pm.getLaunchIntentForPackage(pkg)
}
