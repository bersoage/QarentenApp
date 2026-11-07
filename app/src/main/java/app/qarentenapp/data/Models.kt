package app.qarentenapp.data

import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.serialization.Serializable

@Serializable
data class Group(
    val id: String,
    val name: String,
    val icon: String = "folder",
    /** Tema (color) del grupo: índice en la paleta. */
    val color: Int = 0,
    val packages: List<String> = emptyList(),
)

@Serializable
data class WidgetConf(
    /** "group" o "app" */
    val kind: String,
    /** id del grupo o paquete de la app */
    val target: String,
    val directLaunch: Boolean = false,
    val blackBackground: Boolean = false,
    val hideCaption: Boolean = false,
    val hideStatus: Boolean = false,
    /** Solo grupos: congelar o activar de un toque en vez de abrir la ventana del grupo. */
    val quickToggle: Boolean = false,
)

@Serializable
data class AppSettings(
    val modePref: String = "AUTO",
    // Filtros
    val showSystem: Boolean = false,
    val showSystemWarned: Boolean = false,
    val showServices: Boolean = false,
    val showPackageName: Boolean = false,
    // Apariencia
    val iconTheme: String = "ORIGINAL",
    /** AUTO (según el celular), LIGHT (claro hielo) o DARK (azul noche). */
    val themeMode: String = "AUTO",
    /** Ya se mostró el cartel de bienvenida. */
    val welcomeShown: Boolean = false,
    // Notificación
    val notifyActive: Boolean = true,
    val notifStyle: String = "AUTO",
    val notifOngoing: Boolean = true,
    val notifSingleClick: Boolean = false,
    val notifActiveOnly: Boolean = true,
    // Cajón de cuarentena
    val drawerEnabled: Boolean = true,
    val drawerFullscreen: Boolean = false,
    // Seguridad
    val protectionEnabled: Boolean = false,
    val passwordHash: String? = null,
    val passwordSalt: String? = null,
    // Actualizaciones
    val monitorUpdates: Boolean = true,
    val autoRequarantine: Boolean = false,
    // Avanzado
    val forceRestartLauncher: Boolean = false,
    // Backup
    val autoBackupTree: String? = null,
    val lastBackupAt: Long = 0L,
)

/** Todo lo que el usuario configuró. Es también el formato del archivo de backup. */
@Serializable
data class Config(
    val format: String = "qarentenapp-backup",
    val version: Int = 2,
    val savedAt: Long = 0L,
    val groups: List<Group> = emptyList(),
    val favorites: List<String> = emptyList(),
    /** Apps que el usuario puso en cuarentena (para re-congelarlas tras un formateo o una actualización). */
    val quarantined: Set<String> = emptySet(),
    /** Apps descongeladas desde Q'App (para la notificación de "activas"). */
    val active: Set<String> = emptySet(),
    /** Últimas apps cambiadas desde la notificación o un widget (para relanzarlas). */
    val recent: List<String> = emptyList(),
    /** Apps del sistema que el usuario pidió no volver a advertir al congelarlas. */
    val systemAcked: Set<String> = emptySet(),
    /** Configuración de cada widget, por id de widget (no se exporta). */
    val widgets: Map<String, WidgetConf> = emptyMap(),
    val settings: AppSettings = AppSettings(),
)

/** Una app instalada, tal como la ve la interfaz. */
data class AppEntry(
    val pkg: String,
    val label: String,
    val isSystem: Boolean,
    val hasLauncher: Boolean,
    val frozen: Boolean,
    val protected: Boolean,
)

class IconCache {
    private val map = HashMap<String, ImageBitmap?>()
    @Synchronized fun get(pkg: String) = map[pkg]
    @Synchronized fun has(pkg: String) = map.containsKey(pkg)
    @Synchronized fun put(pkg: String, b: ImageBitmap?) { map[pkg] = b }
}
