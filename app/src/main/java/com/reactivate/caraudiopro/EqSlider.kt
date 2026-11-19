package com.reactivate.caraudiopro

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.view.MotionEvent
import android.view.View
import kotlin.math.roundToInt

/** Bara, touch-friendly vertical slider. 0.5 dB steps. Double-tap = 0 dB. */
@SuppressLint("ViewConstructor")
class EqSlider(
    ctx: Context,
    private val label: String,
    private val minV: Float,
    private val maxV: Float,
    accent: Int,
) : View(ctx) {

    var onChange: ((Float) -> Unit)? = null
    var value = 0f
        set(v) { field = (v.coerceIn(minV, maxV) * 2).roundToInt() / 2f; invalidate() }

    private val d = resources.displayMetrics.density
    private val track = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF262626.toInt(); strokeWidth = 6 * d; strokeCap = Paint.Cap.ROUND }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accent; strokeWidth = 6 * d; strokeCap = Paint.Cap.ROUND }
    private val zero = Paint().apply { color = 0xFF555555.toInt(); strokeWidth = d }
    private val thumb = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    private val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accent; alpha = 80 }
    private val lblP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFAAAAAA.toInt(); textSize = 11 * d; textAlign = Paint.Align.CENTER }
    private val valP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textSize = 12 * d; textAlign = Paint.Align.CENTER; typeface = Typeface.DEFAULT_BOLD }
    private var lastDown = 0L

    private val top get() = 26 * d
    private val bottom get() = height - 22 * d
    private fun yFor(v: Float) = top + (maxV - v) / (maxV - minV) * (bottom - top)
    private fun vFor(y: Float) = maxV - (y - top) / (bottom - top) * (maxV - minV)

    override fun onDraw(c: Canvas) {
        val cx = width / 2f
        val z = if (minV < 0) 0f else minV
        c.drawLine(cx, yFor(maxV), cx, yFor(minV), track)
        c.drawLine(cx - 12 * d, yFor(z), cx + 12 * d, yFor(z), zero)
        c.drawLine(cx, yFor(z), cx, yFor(value), fill)
        val ty = yFor(value)
        c.drawCircle(cx, ty, 14 * d, glow)
        c.drawCircle(cx, ty, 8 * d, thumb)
        c.drawText((if (value > 0) "+" else "") + "%.1f".format(value), cx, 15 * d, valP)
        c.drawText(label, cx, height - 5 * d, lblP)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                val now = System.currentTimeMillis()
                if (now - lastDown < 300) { set(0f); lastDown = 0; return true }
                lastDown = now
                set(vFor(e.y))
            }
            MotionEvent.ACTION_MOVE -> set(vFor(e.y))
        }
        return true
    }

    private fun set(v: Float) {
        val old = value
        value = v
        if (value != old) onChange?.invoke(value)
    }
}
