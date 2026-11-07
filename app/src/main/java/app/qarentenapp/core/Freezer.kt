package app.qarentenapp.core

import android.content.pm.PackageManager
import android.os.Process
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import java.util.concurrent.TimeUnit

/** Cómo congela la app: con root, con Shizuku, o ninguno disponible. */
enum class Mode { ROOT, SHIZUKU, NONE }

/** Preferencia del usuario sobre el modo. */
enum class ModePref { AUTO, ROOT, SHIZUKU }

data class ShellResult(val ok: Boolean, val output: String)

/**
 * Motor de congelado. Usa los comandos del propio Android:
 *  - congelar:     pm disable-user --user N <paquete>
 *  - descongelar:  pm enable --user N <paquete>
 */
object Freezer {

    private val userId: Int get() = Process.myUid() / 100000

    fun freezeCmd(pkg: String) = "pm disable-user --user $userId $pkg"
    fun unfreezeCmd(pkg: String) = "pm enable --user $userId $pkg"

    // ---------- Detección ----------

    suspend fun hasRoot(): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val p = Runtime.getRuntime().exec(arrayOf("su", "-c", "id"))
            val finished = p.waitFor(8, TimeUnit.SECONDS)
            val out = p.inputStream.bufferedReader().readText()
            finished && p.exitValue() == 0 && out.contains("uid=0")
        }.getOrDefault(false)
    }

    fun shizukuRunning(): Boolean = runCatching { Shizuku.pingBinder() }.getOrDefault(false)

    fun shizukuGranted(): Boolean = runCatching {
        Shizuku.pingBinder() && !Shizuku.isPreV11() &&
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    }.getOrDefault(false)

    fun requestShizukuPermission(code: Int = 7) {
        runCatching { if (Shizuku.pingBinder()) Shizuku.requestPermission(code) }
    }

    suspend fun detect(pref: ModePref): Mode = when (pref) {
        ModePref.ROOT -> if (hasRoot()) Mode.ROOT else Mode.NONE
        ModePref.SHIZUKU -> if (shizukuGranted()) Mode.SHIZUKU else Mode.NONE
        ModePref.AUTO -> when {
            hasRoot() -> Mode.ROOT
            shizukuGranted() -> Mode.SHIZUKU
            else -> Mode.NONE
        }
    }

    // ---------- Ejecución ----------

    suspend fun run(mode: Mode, commands: List<String>): ShellResult = withContext(Dispatchers.IO) {
        if (commands.isEmpty()) return@withContext ShellResult(true, "")
        val script = commands.joinToString("; ")
        when (mode) {
            Mode.ROOT -> exec(arrayOf("su", "-c", script))
            Mode.SHIZUKU -> execShizuku(arrayOf("sh", "-c", script))
            Mode.NONE -> ShellResult(false, "Sin root ni Shizuku")
        }
    }

    private fun exec(cmd: Array<String>): ShellResult = runCatching {
        val p = Runtime.getRuntime().exec(cmd)
        val out = p.inputStream.bufferedReader().readText() + p.errorStream.bufferedReader().readText()
        p.waitFor(60, TimeUnit.SECONDS)
        ShellResult(p.exitValue() == 0, out)
    }.getOrElse { ShellResult(false, it.message ?: "error") }

    /** Shizuku.newProcess es privado desde la API 13: se llama por reflexión. */
    private fun execShizuku(cmd: Array<String>): ShellResult = runCatching {
        val m = Shizuku::class.java.getDeclaredMethod(
            "newProcess", Array<String>::class.java, Array<String>::class.java, String::class.java
        )
        m.isAccessible = true
        val p = m.invoke(null, cmd, null, null) as java.lang.Process
        val out = p.inputStream.bufferedReader().readText() + p.errorStream.bufferedReader().readText()
        p.waitFor()
        ShellResult(p.exitValue() == 0, out)
    }.getOrElse { ShellResult(false, it.message ?: "error") }
}
