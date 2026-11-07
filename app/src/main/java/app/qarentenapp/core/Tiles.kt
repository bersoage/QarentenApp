package app.qarentenapp.core

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.TileService
import app.qarentenapp.ui.PopupActivity

/** Mosaicos del panel rápido (el que se baja desde arriba). */
abstract class PopupTile(private val mode: String) : TileService() {
    @SuppressLint("StartActivityAndCollapseDeprecated")
    override fun onClick() {
        super.onClick()
        val i = Intent(this, PopupActivity::class.java)
            .putExtra(PopupActivity.EXTRA_MODE, mode)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        if (Build.VERSION.SDK_INT >= 34) {
            startActivityAndCollapse(
                PendingIntent.getActivity(this, mode.hashCode(), i, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            )
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(i)
        }
    }
}

class DrawerTileService : PopupTile(PopupActivity.MODE_DRAWER)
class QuickTileService : PopupTile(PopupActivity.MODE_QUICK)
