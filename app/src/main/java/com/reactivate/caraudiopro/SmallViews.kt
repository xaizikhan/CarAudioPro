package com.reactivate.caraudiopro

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.view.View

enum class IconKind {
    BASS, MID, TREBLE,
    POWER, SUN, MOON, INFO,
    PLUS, MINUS, CHEVRON_LEFT, CHEVRON_RIGHT,
    TAB_EQ, TAB_BAND, TAB_SUB, TAB_CFG
}

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

    /** How much of the view the glyph fills. Round buttons want a smaller glyph. */
    var scale: Float = 0.52f
        set(v) { field = v; invalidate() }

    override fun onMeasure(wSpec: Int, hSpec: Int) {
        val want = (42 * resources.displayMetrics.density).toInt()
        setMeasuredDimension(resolveSize(want, wSpec), resolveSize(want, hSpec))
    }

    override fun onDraw(c: Canvas) {
        val d = resources.displayMetrics.density
        p.color = iconColor
        p.strokeWidth = 1.8f * d
        // Icons are authored on a 24x24 grid, then scaled into the view.
        val s = minOf(width, height) * scale / 24f
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
            IconKind.POWER -> {
                p.strokeWidth = 2.2f * d
                c.drawLine(x(12f), y(2.5f), x(12f), y(11f), p)
                val ring = android.graphics.RectF(x(4f), y(4f), x(20f), y(20f))
                c.drawArc(ring, -55f, 290f, false, p)
            }
            IconKind.SUN -> {
                p.strokeWidth = 2f * d
                c.drawCircle(x(12f), y(12f), 5f * s, p)
                val rays = listOf(
                    listOf(12f, 1f, 12f, 4f), listOf(12f, 20f, 12f, 23f),
                    listOf(1f, 12f, 4f, 12f), listOf(20f, 12f, 23f, 12f),
                    listOf(4.2f, 4.2f, 6.3f, 6.3f), listOf(17.7f, 17.7f, 19.8f, 19.8f),
                    listOf(4.2f, 19.8f, 6.3f, 17.7f), listOf(17.7f, 6.3f, 19.8f, 4.2f)
                )
                rays.forEach { c.drawLine(x(it[0]), y(it[1]), x(it[2]), y(it[3]), p) }
            }
            IconKind.MOON -> {
                p.strokeWidth = 2f * d
                val moon = Path().apply {
                    moveTo(x(20f), y(14.5f))
                    cubicTo(x(19f), y(18.8f), x(15.2f), y(22f), x(10.6f), y(22f))
                    cubicTo(x(5.3f), y(22f), x(1f), y(17.7f), x(1f), y(12.4f))
                    cubicTo(x(1f), y(7.8f), x(4.2f), y(4f), x(8.5f), y(3f))
                    cubicTo(x(7.5f), y(5f), x(7.2f), y(7.4f), x(8f), y(9.6f))
                    cubicTo(x(9.2f), y(13f), x(12.4f), y(15f), x(16f), y(15f))
                    cubicTo(x(17.4f), y(15f), x(18.8f), y(14.8f), x(20f), y(14.5f))
                    close()
                }
                c.drawPath(moon, p)
            }
            IconKind.INFO -> {
                p.strokeWidth = 2f * d
                c.drawCircle(x(12f), y(12f), 9f * s, p)
                p.style = Paint.Style.FILL
                c.drawCircle(x(12f), y(7.8f), 1.2f * s, p)
                p.style = Paint.Style.STROKE
                c.drawLine(x(12f), y(11f), x(12f), y(16.5f), p)
            }
            IconKind.PLUS -> {
                p.strokeWidth = 2.8f * d
                c.drawLine(x(12f), y(5f), x(12f), y(19f), p)
                c.drawLine(x(5f), y(12f), x(19f), y(12f), p)
            }
            IconKind.MINUS -> {
                p.strokeWidth = 2.8f * d
                c.drawLine(x(5f), y(12f), x(19f), y(12f), p)
            }
            IconKind.CHEVRON_LEFT -> {
                p.strokeWidth = 2.6f * d
                val ch = Path().apply { moveTo(x(15f), y(5f)); lineTo(x(8f), y(12f)); lineTo(x(15f), y(19f)) }
                c.drawPath(ch, p)
            }
            IconKind.CHEVRON_RIGHT -> {
                p.strokeWidth = 2.6f * d
                val ch = Path().apply { moveTo(x(9f), y(5f)); lineTo(x(16f), y(12f)); lineTo(x(9f), y(19f)) }
                c.drawPath(ch, p)
            }
            IconKind.TAB_EQ -> {
                // equalizer bars — used on the CUSTOM tab chip
                p.strokeWidth = 2f * d
                listOf(4f to 11f, 8f to 7f, 12f to 4f, 16f to 8f, 20f to 11f).forEach { (bx, by) ->
                    c.drawLine(x(bx), y(by), x(bx), y(by + 2f), p)
                }
            }
            IconKind.TAB_BAND -> {
                // sine wave — used on the EQUALIZER tab chip
                p.strokeWidth = 2f * d
                val wave = Path().apply {
                    moveTo(x(3f), y(12f))
                    cubicTo(x(5f), y(6f), x(7f), y(6f), x(9f), y(12f))
                    cubicTo(x(11f), y(18f), x(13f), y(18f), x(15f), y(12f))
                    cubicTo(x(17f), y(6f), x(19f), y(6f), x(21f), y(12f))
                }
                c.drawPath(wave, p)
            }
            IconKind.TAB_SUB -> {
                // a speaker-box trapezoid — used on the SUBWOOFER tab chip
                p.strokeWidth = 2f * d
                val box = Path().apply {
                    moveTo(x(4f), y(17f))
                    cubicTo(x(4f), y(13.2f), x(7.2f), y(10f), x(11f), y(10f))
                    lineTo(x(12f), y(10f))
                    cubicTo(x(15.8f), y(10f), x(19f), y(13.2f), x(19f), y(17f))
                    lineTo(x(19f), y(19f))
                    lineTo(x(4f), y(19f))
                    close()
                }
                c.drawPath(box, p)
            }
            IconKind.TAB_CFG -> {
                // a gear — used on the SETUP tab chip
                p.strokeWidth = 2f * d
                c.drawCircle(x(12f), y(12f), 3f * s, p)
                val ticks = listOf(
                    listOf(12f, 3f, 12f, 5f), listOf(12f, 19f, 12f, 21f),
                    listOf(4.2f, 4.2f, 5.6f, 5.6f), listOf(18.4f, 18.4f, 19.8f, 19.8f),
                    listOf(3f, 12f, 5f, 12f), listOf(19f, 12f, 21f, 12f),
                    listOf(4.2f, 19.8f, 5.6f, 18.4f), listOf(18.4f, 5.6f, 19.8f, 4.2f)
                )
                ticks.forEach { c.drawLine(x(it[0]), y(it[1]), x(it[2]), y(it[3]), p) }
            }
        }
    }
}

/** The L/R track on the Balance card: a centre mark plus a metallic dot. */
class BalanceTrackView(ctx: Context) : View(ctx) {
    var value: Int = 0
        set(v) { field = v.coerceIn(-6, 6); invalidate() }
    var trackColor: Int = Color.DKGRAY
        set(v) { field = v; invalidate() }
    var markColor: Int = Color.GRAY
        set(v) { field = v; invalidate() }

    /** Fired continuously while dragging, so the engine can follow the finger. */
    var onValueChange: ((Int) -> Unit)? = null
    /** Fired once on release, for saving. */
    var onValueChangeEnd: (() -> Unit)? = null

    private val p = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun onMeasure(wSpec: Int, hSpec: Int) {
        val d = resources.displayMetrics.density
        setMeasuredDimension(
            resolveSize((160 * d).toInt(), wSpec),
            resolveSize((44 * d).toInt(), hSpec)
        )
    }

    override fun onTouchEvent(e: android.view.MotionEvent): Boolean {
        val d = resources.displayMetrics.density
        val r = 11f * d
        val usable = (width - 2 * r).coerceAtLeast(1f)
        when (e.actionMasked) {
            android.view.MotionEvent.ACTION_DOWN,
            android.view.MotionEvent.ACTION_MOVE -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                val frac = ((e.x - r) / usable).coerceIn(0f, 1f)
                val v = Math.round(frac * 12f) - 6
                if (v != value) {
                    value = v
                    onValueChange?.invoke(v)
                }
                return true
            }
            android.view.MotionEvent.ACTION_UP,
            android.view.MotionEvent.ACTION_CANCEL -> {
                parent?.requestDisallowInterceptTouchEvent(false)
                onValueChangeEnd?.invoke()
                return true
            }
        }
        return super.onTouchEvent(e)
    }

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

    override fun onMeasure(wSpec: Int, hSpec: Int) {
        val d = resources.displayMetrics.density
        setMeasuredDimension(
            resolveSize((160 * d).toInt(), wSpec),
            resolveSize((10 * d).toInt(), hSpec)
        )
    }

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
