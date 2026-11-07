package app.qarentenapp.data

import android.app.backup.BackupManager
import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import kotlinx.serialization.json.Json
import java.io.File

/** Guarda y lee la configuración (config.json) y los archivos de backup. */
class Store(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }
    private val file = File(context.filesDir, "config.json")

    fun load(): Config = runCatching {
        if (file.exists()) json.decodeFromString(Config.serializer(), file.readText()) else Config()
    }.getOrDefault(Config())

    fun save(config: Config) {
        val tmp = File(context.filesDir, "config.json.tmp")
        tmp.writeText(json.encodeToString(Config.serializer(), config))
        tmp.renameTo(file)
        // Avisa al backup automático de Android que hay cambios
        runCatching { BackupManager(context).dataChanged() }
    }

    /** Lo que se exporta: sin el estado efímero de "activas". */
    fun toBackupText(config: Config, now: Long): String =
        json.encodeToString(
            Config.serializer(),
            config.copy(
                active = emptySet(), recent = emptyList(), widgets = emptyMap(), savedAt = now,
                // La contraseña y la carpeta de backup son de este celular: no viajan en el archivo
                settings = config.settings.copy(
                    protectionEnabled = false, passwordHash = null, passwordSalt = null, autoBackupTree = null,
                ),
            ),
        )

    fun parseBackup(text: String): Config? = runCatching {
        val c = json.decodeFromString(Config.serializer(), text)
        if (c.format != "qarentenapp-backup") null else c
    }.getOrNull()

    fun exportTo(uri: Uri, text: String): Boolean = runCatching {
        context.contentResolver.openOutputStream(uri, "wt")!!.use { it.write(text.toByteArray()) }
        true
    }.getOrDefault(false)

    fun importFrom(uri: Uri): Config? = runCatching {
        context.contentResolver.openInputStream(uri)!!.use { parseBackup(it.readBytes().decodeToString()) }
    }.getOrNull()

    /** Backup automático en la carpeta que eligió el usuario. */
    fun writeToTree(treeUri: String, text: String): Boolean = runCatching {
        val tree = DocumentFile.fromTreeUri(context, Uri.parse(treeUri)) ?: return false
        val name = "qarentenapp-backup.json"
        val target = tree.findFile(name) ?: tree.createFile("application/json", name) ?: return false
        exportTo(target.uri, text)
    }.getOrDefault(false)
}
