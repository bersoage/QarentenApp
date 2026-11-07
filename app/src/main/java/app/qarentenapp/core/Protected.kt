package app.qarentenapp.core

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings

/**
 * Apps que nunca se pueden congelar, para no dejar el celular inservible.
 */
object Protected {

    private val fixed = setOf(
        "android",
        "com.android.systemui",
        "com.android.settings",
        "com.android.phone",
        "com.android.shell",
        "com.android.packageinstaller",
        "com.google.android.packageinstaller",
        "com.android.permissioncontroller",
        "com.google.android.permissioncontroller",
        "com.google.android.gms",
        "com.google.android.gsf",
        "com.android.providers.settings",
        "com.android.providers.telephony",
        "com.android.inputdevices",
        "com.miui.home",
        "com.miui.securitycenter",
        "com.lbe.security.miui",
        "com.topjohnwu.magisk",
        "moe.shizuku.privileged.api",
    )

    fun set(context: Context): Set<String> {
        val pm = context.packageManager
        val extra = mutableSetOf(context.packageName)
        // Launcher actual
        runCatching {
            val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
            pm.resolveActivity(home, PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo?.packageName
                ?.let { extra += it }
        }
        // Teclado actual
        runCatching {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
                ?.substringBefore('/')?.let { extra += it }
        }
        return fixed + extra
    }
}
