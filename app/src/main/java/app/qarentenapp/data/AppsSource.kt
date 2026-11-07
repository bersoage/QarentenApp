package app.qarentenapp.data

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build
import app.qarentenapp.core.Protected

data class AppDetails(
    val pkg: String,
    val label: String,
    val version: String,
    val versionCode: Long,
    val firstInstall: Long,
    val lastUpdate: Long,
    val partition: String,
    val isSystem: Boolean,
    val permissions: List<String>,
)

/** Lee las apps instaladas y su estado (congelada o no). */
class AppsSource(private val context: Context) {

    private val pm: PackageManager get() = context.packageManager

    @Suppress("DEPRECATION")
    fun list(): List<AppEntry> {
        val protected = Protected.set(context)
        val launchable = runCatching {
            val i = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            pm.queryIntentActivities(i, PackageManager.MATCH_DISABLED_COMPONENTS).map { it.activityInfo.packageName }.toSet()
        }.getOrDefault(emptySet())
        val apps = pm.getInstalledApplications(PackageManager.MATCH_DISABLED_COMPONENTS)
        return apps.map { ai ->
            AppEntry(
                pkg = ai.packageName,
                label = runCatching { ai.loadLabel(pm).toString() }.getOrDefault(ai.packageName),
                isSystem = (ai.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                hasLauncher = ai.packageName in launchable,
                frozen = isFrozen(ai),
                protected = ai.packageName in protected,
            )
        }.sortedBy { it.label.lowercase() }
    }

    fun isInstalled(pkg: String): Boolean = runCatching {
        pm.getApplicationInfo(pkg, PackageManager.MATCH_DISABLED_COMPONENTS); true
    }.getOrDefault(false)

    fun isFrozen(pkg: String): Boolean = runCatching {
        isFrozen(pm.getApplicationInfo(pkg, PackageManager.MATCH_DISABLED_COMPONENTS))
    }.getOrDefault(false)

    private fun isFrozen(ai: ApplicationInfo): Boolean {
        if (!ai.enabled) return true
        return runCatching {
            val s = pm.getApplicationEnabledSetting(ai.packageName)
            s == PackageManager.COMPONENT_ENABLED_STATE_DISABLED ||
                s == PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER
        }.getOrDefault(false)
    }

    fun label(pkg: String): String = runCatching {
        pm.getApplicationInfo(pkg, PackageManager.MATCH_DISABLED_COMPONENTS).loadLabel(pm).toString()
    }.getOrDefault(pkg)

    @Suppress("DEPRECATION")
    private fun packageInfo(pkg: String, flags: Int): PackageInfo? = runCatching {
        pm.getPackageInfo(pkg, flags or PackageManager.MATCH_DISABLED_COMPONENTS)
    }.getOrNull()

    fun lastUpdate(pkg: String): Long = packageInfo(pkg, 0)?.lastUpdateTime ?: 0L

    fun details(pkg: String): AppDetails? {
        val pi = packageInfo(pkg, PackageManager.GET_PERMISSIONS) ?: return null
        val ai = pi.applicationInfo ?: return null
        val src = ai.sourceDir ?: ""
        val partition = when {
            src.startsWith("/data") -> "Usuario (/data)"
            src.startsWith("/system_ext") -> "Sistema (/system_ext)"
            src.startsWith("/product") -> "Sistema (/product)"
            src.startsWith("/vendor") -> "Fabricante (/vendor)"
            src.startsWith("/system") -> "Sistema (/system)"
            else -> src.substringBeforeLast('/').ifEmpty { "—" }
        }
        @Suppress("DEPRECATION")
        val code = if (Build.VERSION.SDK_INT >= 28) pi.longVersionCode else pi.versionCode.toLong()
        return AppDetails(
            pkg = pkg,
            label = runCatching { ai.loadLabel(pm).toString() }.getOrDefault(pkg),
            version = pi.versionName ?: "—",
            versionCode = code,
            firstInstall = pi.firstInstallTime,
            lastUpdate = pi.lastUpdateTime,
            partition = partition,
            isSystem = (ai.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
            permissions = pi.requestedPermissions?.toList() ?: emptyList(),
        )
    }

    /** Permisos pedidos por cada app, para el filtro por permisos. */
    fun permissionsOf(pkgs: Collection<String>): Map<String, Set<String>> =
        pkgs.associateWith { p -> packageInfo(p, PackageManager.GET_PERMISSIONS)?.requestedPermissions?.toSet() ?: emptySet() }

    fun loadIconBitmap(pkg: String, sizePx: Int = 112): Bitmap? = runCatching {
        val d = pm.getApplicationIcon(pkg)
        val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        d.setBounds(0, 0, sizePx, sizePx)
        d.draw(c)
        bmp
    }.getOrNull()

    fun launchIntent(pkg: String) = pm.getLaunchIntentForPackage(pkg)

    fun currentLauncher(): String? = runCatching {
        val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        pm.resolveActivity(home, PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo?.packageName
    }.getOrNull()
}
