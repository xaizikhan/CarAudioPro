package com.reactivate.caraudiopro

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.view.View

enum class IconKind { BASS, MID, TREBLE }

/**
 * The little round icon beside Bass / Mid / Treble. Drawn rather than shipped as
 * a drawable so it inherits the theme colour and scales with density.
 */
class BandIconView(ctx: Context, private val kind: IconKind) : View(ctx) {
    var iconColor: Int = Color.WHITE
        set(v) { field = v; invalidate() }

    private val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    override fun onDraw(c: Canvas) {
        val d = resources.displayMetrics.density
        p.color = iconColor
        p.strokeWidth = 1.8f * d
        // Icons are authored on a 24x24 grid, then scaled into the view.
        val s = minOf(width, height) * 0.52f / 24f
        val ox = width / 2f - 12f * s
        val oy = height / 2f - 12f * s
        fun x(v: Float) = ox + v * s
        fun y(v: Float) = oy + v * s

        when (kind) {
            IconKind.BASS -> {
                // speaker cone: an arc over two driver circles
                val arc = Path().apply {
                    moveTo(x(4f), y(14.5f))
                    cubicTo(x(4f), y(9.8f), x(7.6f), y(6f), x(12f), y(6f))
                    cubicTo(x(16.4f), y(6f), x(20f), y(9.8f), x(20f), y(14.5f))
                }
                c.drawPath(arc, p)
                c.drawCircle(x(7f), y(16f), 2.4f * s, p)
                c.drawCircle(x(17f), y(16f), 2.4f * s, p)
            }
            IconKind.MID -> {
                // a waveform peak
                val w = Path().apply {
                    moveTo(x(2f), y(12f)); lineTo(x(5f), y(12f))
                    lineTo(x(7.5f), y(5f)); lineTo(x(11.5f), y(19f))
                    lineTo(x(14.5f), y(10f)); lineTo(x(16.5f), y(14f))
                    lineTo(x(22f), y(14f))
                }
                c.drawPath(w, p)
            }
            IconKind.TREBLE -> {
                // a double music note
                c.drawCircle(x(7f), y(17.5f), 2.6f * s, p)
                c.drawCircle(x(17.5f), y(15f), 2.6f * s, p)
                val stem = Path().apply {
                    moveTo(x(9.6f), y(17.5f)); lineTo(x(9.6f), y(7.2f))
                    lineTo(x(20.1f), y(5f)); lineTo(x(20.1f), y(15f))
                }
                c.drawPath(stem, p)
            }
        }
    }
}

/** The L/R track on the Balance card: a centre mark plus a metallic dot. */
class BalanceTrackView(ctx: Context) : View(ctx) {
    var value: Int = 0
        set(v) { field = v; invalidate() }
    var trackColor: Int = Color.DKGRAY
        set(v) { field = v; invalidate() }
    var markColor: Int = Color.GRAY
        set(v) { field = v; invalidate() }

    private val p = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun onDraw(c: Canvas) {
        val d = resources.displayMetrics.density
        val cy = height / 2f
        val barH = 10f * d

        p.style = Paint.Style.FILL
        p.color = trackColor
        c.drawRoundRect(RectF(0f, cy - barH / 2, width.toFloat(), cy + barH / 2), barH / 2, barH / 2, p)

        // centre tick
        p.color = markColor
        val tw = 2f * d
        c.drawRect(width / 2f - tw / 2, cy - barH, width / 2f + tw / 2, cy + barH, p)

        // the dot
        val frac = (value + 6) / 12f
        val r = 11f * d
        val cx = r + frac * (width - 2 * r)
        p.color = Color.parseColor("#8C939F")
        c.drawCircle(cx, cy, r, p)
        p.color = Color.parseColor("#F4F6F9")
        c.drawCircle(cx, cy, r - 1.2f * d, p)
    }
}

/** The headroom meter: a plain filled bar. */
class BarView(ctx: Context) : View(ctx) {
    var fraction: Float = 0f
        set(v) { field = v.coerceIn(0f, 1f); invalidate() }
    var barColor: Int = Color.GREEN
        set(v) { field = v; invalidate() }
    var trackColor: Int = Color.DKGRAY
        set(v) { field = v; invalidate() }

    private val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

    override fun onDraw(c: Canvas) {
        val r = height / 2f
        p.color = trackColor
        c.drawRoundRect(RectF(0f, 0f, width.toFloat(), height.toFloat()), r, r, p)
        if (fraction > 0f) {
            p.color = barColor
            c.drawRoundRect(RectF(0f, 0f, width * fraction, height.toFloat()), r, r, p)
        }
    }
}
