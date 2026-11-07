package app.qarentenapp.data

import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.serialization.Serializable

@Serializable
data class Group(
    val id: String,
    val name: String,
    val icon: String = "folder",
    val packages: List<String> = emptyList(),
)

@Serializable
data class AppSettings(
    val modePref: String = "AUTO",
    val showSystem: Boolean = false,
    val showSystemWarned: Boolean = false,
    val showPackageName: Boolean = false,
    val notifyActive: Boolean = true,
    /** Carpeta elegida para el backup automático (URI de SAF), o null. */
    val autoBackupTree: String? = null,
    val lastBackupAt: Long = 0L,
)

/** Todo lo que el usuario configuró. Es también el formato del archivo de backup. */
@Serializable
data class Config(
    val format: String = "qarentenapp-backup",
    val version: Int = 1,
    val savedAt: Long = 0L,
    val groups: List<Group> = emptyList(),
    /** Apps que el usuario puso en cuarentena (para re-congelarlas tras un formateo). */
    val quarantined: Set<String> = emptySet(),
    /** Apps descongeladas desde Q'App (para la notificación de "activas"). */
    val active: Set<String> = emptySet(),
    val settings: AppSettings = AppSettings(),
)

/** Una app instalada, tal como la ve la interfaz. */
data class AppEntry(
    val pkg: String,
    val label: String,
    val isSystem: Boolean,
    val frozen: Boolean,
    val protected: Boolean,
)

class IconCache {
    private val map = HashMap<String, ImageBitmap?>()
    fun get(pkg: String) = map[pkg]
    fun has(pkg: String) = map.containsKey(pkg)
    fun put(pkg: String, b: ImageBitmap?) { map[pkg] = b }
}
