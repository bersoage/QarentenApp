package app.qarentenapp.core

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.RectF
import app.qarentenapp.data.Group

/** Imágenes para widgets, accesos directos y notificaciones. */
object Bitmaps {

    /** Colores de tema de los grupos (fondo, primer plano). */
    val groupColors: List<Pair<Int, Int>> = listOf(
        0xFFD6E4F5.toInt() to 0xFF1F4268.toInt(),
        0xFFF6DFD3.toInt() to 0xFF6B2E12.toInt(),
        0xFFE3DDF3.toInt() to 0xFF3A2A6B.toInt(),
        0xFFD3EDE6.toInt() to 0xFF12463A.toInt(),
        0xFFF7E3A8.toInt() to 0xFF5A4300.toInt(),
        0xFFF4D6E2.toInt() to 0xFF6A1F45.toInt(),
        0xFFDCE6D2.toInt() to 0xFF2E4A1A.toInt(),
        0xFFDDE2E8.toInt() to 0xFF2A3646.toInt(),
    )

    private val greyPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
        colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) })
        alpha = 150
    }
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    fun app(pkg: String, frozen: Boolean, size: Int = 144): Bitmap {
        val src = Repo.source.loadIconBitmap(pkg, size) ?: Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        if (!frozen) return src
        val out = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        Canvas(out).drawBitmap(src, 0f, 0f, greyPaint)
        return out
    }

    /** Ícono de grupo: fondo con el color del grupo y hasta 4 íconos de sus apps. */
    fun group(g: Group, frozen: Boolean, size: Int = 192): Bitmap {
        val out = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(out)
        val (bg, _) = groupColors[g.color.coerceIn(0, groupColors.lastIndex)]
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = if (frozen) 0xFFDDE2E8.toInt() else bg }
        c.drawRoundRect(RectF(0f, 0f, size.toFloat(), size.toFloat()), size * 0.28f, size * 0.28f, bgPaint)
        val pkgs = g.packages.filter { Repo.source.isInstalled(it) }.take(4)
        if (pkgs.isEmpty()) return out
        val pad = size * 0.12f
        val gap = size * 0.06f
        val cell = (size - 2 * pad - gap) / 2f
        val positions = if (pkgs.size == 1) listOf(RectF(size * 0.22f, size * 0.22f, size * 0.78f, size * 0.78f))
        else (0 until 4).map { i ->
            val x = pad + (i % 2) * (cell + gap); val y = pad + (i / 2) * (cell + gap)
            RectF(x, y, x + cell, y + cell)
        }
        pkgs.forEachIndexed { i, p ->
            val bmp = Repo.source.loadIconBitmap(p, cell.toInt().coerceAtLeast(48)) ?: return@forEachIndexed
            c.drawBitmap(bmp, null, positions[i], if (frozen) greyPaint else paint)
        }
        return out
    }
}
