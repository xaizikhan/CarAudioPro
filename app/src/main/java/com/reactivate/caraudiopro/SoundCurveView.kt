package com.reactivate.caraudiopro

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Shader
import android.graphics.Typeface
import android.view.MotionEvent
import android.view.View
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * The colourful "mountain" EQ curve shown on Easy and Equalizer tabs.
 * Bass->red, mids->green, treble->purple, same spectrum as the approved design.
 * Tap a dot (only when [interactive]) to select that band for fine editing.
 */
class SoundCurveView(context: Context) : View(context) {

    var bands: FloatArray = FloatArray(15)
        set(v) { field = v; invalidate() }
    var selected: Int = 2
        set(v) { field = v; invalidate() }
    var interactive: Boolean = false
        set(v) { field = v; invalidate() }
    var theme: ThemeColors = ThemeColors.DARK
        set(v) { field = v; invalidate() }
    var onBandTap: ((Int) -> Unit)? = null

    private val d = resources.displayMetrics.density
    private fun dp(v: Float) = v * d
    private val labels = listOf("25", "40", "63", "100", "160", "250", "400", "630", "1k", "1.6k", "2.5k", "4k", "6.3k", "10k", "16k")
    private val n = labels.size

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeWidth = dp(1f) }
    private val zeroPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = dp(1.5f); pathEffect = DashPathEffect(floatArrayOf(dp(3f), dp(6f)), 0f)
    }
    private val areaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val fadePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
    }
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = dp(3.2f); strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
    }
    private val dotFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val dotStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = Typeface.DEFAULT_BOLD; textAlign = Paint.Align.CENTER }

    private fun padTop() = dp(16f)
    private fun padBottom() = dp(30f)
    private fun padL() = dp(32f)
    private fun padR() = dp(14f)

    private fun xAt(i: Int): Float {
        val plotW = width - padL() - padR()
        return padL() + i * plotW / (n - 1)
    }
    private fun yAt(g: Float): Float {
        val plotH = height - padTop() - padBottom()
        return padTop() + plotH / 2f - g / 12f * (plotH / 2f)
    }

    override fun onDraw(canvas: Canvas) {
        if (width == 0 || height == 0) return
        val plotH = height - padTop() - padBottom()
        val plotW = width - padL() - padR()

        gridPaint.color = theme.gridLine
        for (i in 0 until n) canvas.drawLine(xAt(i), padTop(), xAt(i), padTop() + plotH, gridPaint)
        for (dbv in intArrayOf(12, 6, -6, -12)) canvas.drawLine(padL(), yAt(dbv.toFloat()), padL() + plotW, yAt(dbv.toFloat()), gridPaint)
        zeroPaint.color = theme.zeroLine
        canvas.drawLine(padL(), yAt(0f), padL() + plotW, yAt(0f), zeroPaint)

        val xs = FloatArray(n) { xAt(it) }
        val ys = FloatArray(n) { yAt(bands.getOrElse(it) { 0f }) }
        val path = Path().apply { moveTo(xs[0], ys[0]) }
        for (i in 0 until n - 1) {
            val p0x = xs[max(0, i - 1)]; val p0y = ys[max(0, i - 1)]
            val p1x = xs[i]; val p1y = ys[i]
            val p2x = xs[i + 1]; val p2y = ys[i + 1]
            val p3x = xs[min(n - 1, i + 2)]; val p3y = ys[min(n - 1, i + 2)]
            val c1x = p1x + (p2x - p0x) / 6f; val c1y = p1y + (p2y - p0y) / 6f
            val c2x = p2x - (p3x - p1x) / 6f; val c2y = p2y - (p3y - p1y) / 6f
            path.cubicTo(c1x, c1y, c2x, c2y, p2x, p2y)
        }
        val areaPath = Path(path).apply { lineTo(xs[n - 1], padTop() + plotH); lineTo(xs[0], padTop() + plotH); close() }

        val hueShader = LinearGradient(
            padL(), 0f, padL() + plotW, 0f,
            SpectrumColors.colorsArray(), SpectrumColors.stopsArray(), Shader.TileMode.CLAMP
        )

        val layer = canvas.saveLayer(padL(), padTop(), padL() + plotW, padTop() + plotH, null)
        areaPaint.shader = hueShader
        canvas.drawPath(areaPath, areaPaint)
        fadePaint.shader = LinearGradient(
            0f, padTop(), 0f, padTop() + plotH,
            Color.argb(170, 255, 255, 255), Color.argb(0, 255, 255, 255), Shader.TileMode.CLAMP
        )
        canvas.drawRect(padL(), padTop(), padL() + plotW, padTop() + plotH, fadePaint)
        canvas.restoreToCount(layer)

        linePaint.shader = hueShader
        canvas.drawPath(path, linePaint)

        text.textAlign = Paint.Align.LEFT
        text.textSize = dp(10f)
        for (dbv in intArrayOf(12, 6, 0, -6, -12)) {
            text.color = theme.freqLabel
            canvas.drawText((if (dbv > 0) "+$dbv" else "$dbv"), dp(2f), yAt(dbv.toFloat()) - dp(3f), text)
        }
        text.textAlign = Paint.Align.CENTER

        for (i in 0 until n) {
            val bc = SpectrumColors.at(i / (n - 1f))
            val isSel = interactive && selected == i
            val r = if (isSel) dp(12f) else dp(6f)
            dotFill.color = if (isSel) Color.WHITE else theme.dotHole
            canvas.drawCircle(xs[i], ys[i], r, dotFill)
            dotStroke.color = bc; dotStroke.strokeWidth = dp(2.4f)
            canvas.drawCircle(xs[i], ys[i], r, dotStroke)

            text.textSize = dp(11f)
            text.color = if (isSel) bc else theme.freqLabel
            canvas.drawText(labels[i], xs[i], padTop() + plotH + dp(20f), text)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!interactive) return false
        if (event.action == MotionEvent.ACTION_DOWN) {
            val plotW = width - padL() - padR()
            val idx = ((event.x - padL()) / (plotW / (n - 1))).roundToInt().coerceIn(0, n - 1)
            onBandTap?.invoke(idx)
            return true
        }
        return false
    }
}
