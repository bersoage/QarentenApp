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

/** Accesos directos en la pantalla de inicio. */
object Shortcuts {

    fun canPin(context: Context) = ShortcutManagerCompat.isRequestPinShortcutSupported(context)

    private fun action(context: Context, action: String, id: String) =
        Intent(context, ActionActivity::class.java).setAction(action).putExtra(ActionActivity.EXTRA_ID, id)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)

    /** Ícono que abre la ventana del grupo: sus apps con candado, y activar o congelar todo. */
    fun pinGroup(context: Context, g: Group): Boolean {
        val info = ShortcutInfoCompat.Builder(context, "group_${g.id}")
            .setShortLabel(g.name)
            .setLongLabel("Grupo «${g.name}»")
            .setIcon(IconCompat.createWithAdaptiveBitmap(padded(Bitmaps.group(g, false))))
            .setIntent(groupPopup(context, g.id))
            .build()
        return ShortcutManagerCompat.requestPinShortcut(context, info, null)
    }

    /** Ícono que descongela la app (si hace falta) y la abre. */
    fun pinApp(context: Context, pkg: String): Boolean {
        val info = ShortcutInfoCompat.Builder(context, "app_$pkg")
            .setShortLabel(Repo.labelOf(pkg))
            .setIcon(IconCompat.createWithBitmap(Bitmaps.app(pkg, false)))
            .setIntent(action(context, ActionActivity.ACTION_LAUNCH, pkg))
            .build()
        return ShortcutManagerCompat.requestPinShortcut(context, info, null)
    }

    fun pinDrawer(context: Context): Boolean {
        val info = ShortcutInfoCompat.Builder(context, "drawer")
            .setShortLabel("Cuarentena")
            .setLongLabel("Cajón de cuarentena")
            .setIcon(IconCompat.createWithResource(context, R.mipmap.ic_launcher))
            .setIntent(popup(context, PopupActivity.MODE_DRAWER))
            .build()
        return ShortcutManagerCompat.requestPinShortcut(context, info, null)
    }

    private fun groupPopup(context: Context, id: String) =
        Intent(context, PopupActivity::class.java).setAction(Intent.ACTION_VIEW)
            .putExtra(PopupActivity.EXTRA_MODE, PopupActivity.MODE_GROUP).putExtra(PopupActivity.EXTRA_ID, id)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)

    private fun popup(context: Context, mode: String) =
        Intent(context, PopupActivity::class.java).setAction(Intent.ACTION_VIEW).putExtra(PopupActivity.EXTRA_MODE, mode)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)

    /** Accesos al mantener presionado el ícono de Q'App. */
    fun updateDynamic(context: Context) {
        runCatching {
            val list = mutableListOf(
                ShortcutInfoCompat.Builder(context, "dyn_drawer").setShortLabel("Cajón de cuarentena")
                    .setIcon(IconCompat.createWithResource(context, R.mipmap.ic_launcher))
                    .setIntent(popup(context, PopupActivity.MODE_DRAWER)).build(),
                ShortcutInfoCompat.Builder(context, "dyn_quick").setShortLabel("Menú rápido")
                    .setIcon(IconCompat.createWithResource(context, R.mipmap.ic_launcher))
                    .setIntent(popup(context, PopupActivity.MODE_QUICK)).build(),
            )
            Repo.config.value.groups.take(2).forEach { g ->
                list += ShortcutInfoCompat.Builder(context, "dyn_group_${g.id}").setShortLabel(g.name)
                    .setIcon(IconCompat.createWithAdaptiveBitmap(padded(Bitmaps.group(g, false))))
                    .setIntent(groupPopup(context, g.id)).build()
            }
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
