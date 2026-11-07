package app.qarentenapp.core

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import app.qarentenapp.R
import app.qarentenapp.data.Group
import app.qarentenapp.ui.ActionActivity
import app.qarentenapp.ui.PopupActivity

/**
 * Accesos directos en la pantalla de inicio.
 * Se crean desde Q'App (fijar) o desde la lista "Accesos directos" del launcher y de otras apps.
 */
object Shortcuts {

    fun canPin(context: Context) = ShortcutManagerCompat.isRequestPinShortcutSupported(context)

    private fun action(context: Context, action: String, id: String) =
        Intent(context, ActionActivity::class.java).setAction(action).putExtra(ActionActivity.EXTRA_ID, id)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)

    private fun groupPopup(context: Context, id: String) =
        Intent(context, PopupActivity::class.java).setAction(Intent.ACTION_VIEW)
            .putExtra(PopupActivity.EXTRA_MODE, PopupActivity.MODE_GROUP).putExtra(PopupActivity.EXTRA_ID, id)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)

    private fun popup(context: Context, mode: String) =
        Intent(context, PopupActivity::class.java).setAction(Intent.ACTION_VIEW).putExtra(PopupActivity.EXTRA_MODE, mode)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)

    // ---------- Definiciones ----------

    /** Abre la ventana del grupo: sus apps con candado, y activar o congelar todo. */
    fun groupInfo(context: Context, g: Group, id: String = "group_${g.id}"): ShortcutInfoCompat =
        ShortcutInfoCompat.Builder(context, id)
            .setShortLabel(g.name)
            .setLongLabel("Grupo «${g.name}»")
            .setIcon(IconCompat.createWithAdaptiveBitmap(padded(Bitmaps.group(g, false))))
            .setIntent(groupPopup(context, g.id))
            .build()

    /** Congela o activa el grupo entero de un toque, sin abrir nada. */
    fun groupToggleInfo(context: Context, g: Group): ShortcutInfoCompat =
        ShortcutInfoCompat.Builder(context, "grouptoggle_${g.id}")
            .setShortLabel(g.name)
            .setLongLabel("Congelar o activar «${g.name}»")
            .setIcon(IconCompat.createWithAdaptiveBitmap(padded(Bitmaps.group(g, false))))
            .setIntent(action(context, ActionActivity.ACTION_TOGGLE_GROUP, g.id))
            .build()

    /** Descongela la app (si hace falta) y la abre. */
    fun appInfo(context: Context, pkg: String): ShortcutInfoCompat =
        ShortcutInfoCompat.Builder(context, "app_$pkg")
            .setShortLabel(Repo.labelOf(pkg))
            .setIcon(IconCompat.createWithBitmap(Bitmaps.app(pkg, false)))
            .setIntent(action(context, ActionActivity.ACTION_LAUNCH, pkg))
            .build()

    fun drawerInfo(context: Context, id: String = "drawer"): ShortcutInfoCompat =
        ShortcutInfoCompat.Builder(context, id)
            .setShortLabel("Cuarentena")
            .setLongLabel("Cajón de cuarentena")
            .setIcon(IconCompat.createWithResource(context, R.mipmap.ic_launcher))
            .setIntent(popup(context, PopupActivity.MODE_DRAWER))
            .build()

    fun quickInfo(context: Context, id: String = "quick"): ShortcutInfoCompat =
        ShortcutInfoCompat.Builder(context, id)
            .setShortLabel("Menú rápido")
            .setIcon(IconCompat.createWithResource(context, R.mipmap.ic_launcher))
            .setIntent(popup(context, PopupActivity.MODE_QUICK))
            .build()

    // ---------- Fijar desde Q'App ----------

    fun pinGroup(context: Context, g: Group) = ShortcutManagerCompat.requestPinShortcut(context, groupInfo(context, g), null)
    fun pinApp(context: Context, pkg: String) = ShortcutManagerCompat.requestPinShortcut(context, appInfo(context, pkg), null)
    fun pinDrawer(context: Context) = ShortcutManagerCompat.requestPinShortcut(context, drawerInfo(context), null)

    /** Resultado para la lista clásica "Accesos directos" (launcher, Tasker, etc.). */
    fun result(context: Context, info: ShortcutInfoCompat): Intent =
        ShortcutManagerCompat.createShortcutResultIntent(context, info)

    /** Accesos al mantener presionado el ícono de Q'App. */
    fun updateDynamic(context: Context) {
        runCatching {
            val list = mutableListOf(drawerInfo(context, "dyn_drawer"), quickInfo(context, "dyn_quick"))
            Repo.config.value.groups.take(2).forEach { g -> list += groupInfo(context, g, "dyn_group_${g.id}") }
            ShortcutManagerCompat.setDynamicShortcuts(context, list)
        }
    }

    /** Los íconos adaptativos recortan los bordes: se agrega margen. */
    private fun padded(src: android.graphics.Bitmap): android.graphics.Bitmap {
        val size = (src.width * 1.5f).toInt()
        val out = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
        val c = android.graphics.Canvas(out)
        c.drawColor(0xFF001A5C.toInt())
        val off = (size - src.width) / 2f
        c.drawBitmap(src, off, off, null)
        return out
    }
}
