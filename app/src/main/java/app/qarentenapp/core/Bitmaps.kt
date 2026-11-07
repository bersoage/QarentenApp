package app.qarentenapp.core

import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat
import app.qarentenapp.R
import app.qarentenapp.data.Group
import app.qarentenapp.ui.iconDef
import kotlin.random.Random

/** Imágenes para widgets, accesos directos y notificaciones. */
object Bitmaps {

    /** Colores de tema de los grupos (fondo claro, primer plano) para la interfaz. */
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

    /** Color base (saturado) de cada tema, para los íconos con carámbanos. */
    val groupBase: List<Int> = listOf(
        0xFF3478D4.toInt(), 0xFFB4531F.toInt(), 0xFF6A4FB3.toInt(), 0xFF12705C.toInt(),
        0xFFB08214.toInt(), 0xFFAA346B.toInt(), 0xFF467832.toInt(), 0xFF465569.toInt(),
    )

    private val greyPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
        colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) })
        alpha = 150
    }
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    private var iconFont: Typeface? = null
    private fun font(): Typeface? {
        if (iconFont == null) iconFont = runCatching { ResourcesCompat.getFont(Repo.ctx, R.font.material_icons) }.getOrNull()
        return iconFont
    }

    fun app(pkg: String, frozen: Boolean, size: Int = 144): Bitmap {
        val src = Repo.source.loadIconBitmap(pkg, size) ?: Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        if (!frozen) return src
        val out = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        Canvas(out).drawBitmap(src, 0f, 0f, greyPaint)
        return out
    }

    private fun mix(c: Int, other: Int, t: Float): Int {
        fun ch(a: Int, b: Int) = (a + (b - a) * t).toInt().coerceIn(0, 255)
        return Color.rgb(ch(Color.red(c), Color.red(other)), ch(Color.green(c), Color.green(other)), ch(Color.blue(c), Color.blue(other)))
    }

    /**
     * Ícono de grupo con carámbanos: fondo del color del grupo, el ícono del rubro en blanco
     * y carámbanos largos si está congelado o cortitos (derretidos) si está activo.
     * Con [adaptive] se dibuja a sangre para íconos adaptativos (el contenido queda en la zona segura).
     */
    fun group(g: Group, frozen: Boolean, size: Int = 192, adaptive: Boolean = false): Bitmap {
        val out = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(out)
        val s = size.toFloat()
        val base = groupBase[g.color.coerceIn(0, groupBase.lastIndex)]
        val inset = if (adaptive) s * 0.17f else 0f
        val n = s - 2 * inset          // lado del área útil
        fun X(f: Float) = inset + f * n
        fun Y(f: Float) = inset + f * n

        // Forma y fondo con degradé
        val shape = Path()
        if (adaptive) shape.addRect(0f, 0f, s, s, Path.Direction.CW)
        else shape.addRoundRect(RectF(0f, 0f, s, s), s * 0.22f, s * 0.22f, Path.Direction.CW)
        c.save()
        c.clipPath(shape)
        val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(0f, 0f, 0f, s, mix(base, Color.WHITE, 0.32f), mix(base, Color.BLACK, 0.28f), Shader.TileMode.CLAMP)
        }
        c.drawRect(0f, 0f, s, s, bg)

        // Ícono del rubro (o de una app)
        if (g.icon.startsWith("app:")) {
            val pkg = g.icon.removePrefix("app:")
            Repo.source.loadIconBitmap(pkg, (n * 0.46f).toInt().coerceAtLeast(48))?.let { b ->
                val w = n * 0.46f
                val r = RectF(X(0.5f) - w / 2, Y(0.64f) - w / 2, X(0.5f) + w / 2, Y(0.64f) + w / 2)
                c.drawBitmap(b, null, r, paint)
            }
        } else {
            val tf = font()
            if (tf != null) {
                val gp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    typeface = tf; color = Color.WHITE; textSize = n * 0.40f; textAlign = Paint.Align.CENTER
                    setShadowLayer(n * 0.02f, 0f, n * 0.012f, 0x6E001946)
                }
                val fm = gp.fontMetrics
                c.drawText(iconDef(g.icon).glyph, X(0.5f), Y(0.66f) - (fm.ascent + fm.descent) / 2, gp)
            }
        }

        // Nieve y carámbanos
        val rnd = Random(g.id.hashCode())
        val ice = Path()
        val capH = if (frozen) 0.20f else 0.15f
        ice.moveTo(0f, 0f); ice.lineTo(s, 0f); ice.lineTo(s, Y(capH))
        var x = 1f
        while (x > 0f) {
            x -= 0.04f
            ice.lineTo(X(x.coerceAtLeast(0f)) - if (x <= 0f) inset else 0f, Y(capH + rnd.nextFloat() * 0.04f - 0.015f))
        }
        ice.lineTo(0f, Y(capH))
        ice.close()
        val count = 9
        val drops = Path()
        for (i in 0 until count) {
            val cx = 0.08f + i * 0.84f / (count - 1) + (rnd.nextFloat() - 0.5f) * 0.03f
            val len = if (frozen) listOf(0.10f, 0.14f, 0.19f, 0.24f, 0.28f, 0.16f, 0.12f).random(rnd)
            else listOf(0.03f, 0.05f, 0.07f, 0.04f).random(rnd)
            val w = (0.05f + rnd.nextFloat() * 0.025f) * (if (frozen) 1f else 0.8f)
            val top = capH - 0.01f
            val p = Path()
            p.moveTo(X(cx - w / 2), Y(top)); p.lineTo(X(cx + w / 2), Y(top))
            val steps = 12
            for (k in 1..steps) {
                val t = k / steps.toFloat(); val ww = w / 2 * Math.pow((1 - t).toDouble(), 1.25).toFloat() + 0.004f
                p.lineTo(X(cx + ww), Y(top + len * t))
            }
            for (k in steps downTo 1) {
                val t = k / steps.toFloat(); val ww = w / 2 * Math.pow((1 - t).toDouble(), 1.25).toFloat() + 0.004f
                p.lineTo(X(cx - ww), Y(top + len * t))
            }
            p.close()
            ice.addPath(p)
            ice.addOval(RectF(X(cx - 0.009f), Y(top + len - 0.009f), X(cx + 0.009f), Y(top + len + 0.013f)), Path.Direction.CW)
            if (frozen && rnd.nextFloat() < 0.35f) {
                val gy = top + len + 0.03f + rnd.nextFloat() * 0.04f
                drops.addOval(RectF(X(cx - 0.009f), Y(gy - 0.011f), X(cx + 0.009f), Y(gy + 0.012f)), Path.Direction.CW)
            }
        }
        val shadow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0x590A285A; maskFilter = BlurMaskFilter(n * 0.012f, BlurMaskFilter.Blur.NORMAL)
        }
        c.save(); c.translate(0f, n * 0.012f); c.drawPath(ice, shadow); c.restore()
        c.drawPath(ice, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xEBECF6FF.toInt() })
        c.drawPath(drops, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xC8ECF6FF.toInt() })
        // brillo en la nieve
        c.save(); c.clipPath(ice)
        c.drawRect(0f, 0f, s, Y(capH * 0.45f), Paint().apply { color = 0x5AFFFFFF })
        c.restore()
        c.restore()

        // bisel
        if (!adaptive) c.drawRoundRect(
            RectF(s * 0.006f, s * 0.006f, s * 0.994f, s * 0.994f), s * 0.22f, s * 0.22f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = s * 0.012f; color = 0x46FFFFFF },
        )
        return out
    }
}
