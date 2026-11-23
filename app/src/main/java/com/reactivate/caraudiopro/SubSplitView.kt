package com.reactivate.caraudiopro

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.graphics.Typeface
import android.view.View
import kotlin.math.log10

/** Speakers + subwoofer split: shows where the crossover divides deep bass (sub) from the rest (speakers). */
class SubSplitView(context: Context) : View(context) {

    var subLevel: Float = 0f
        set(v) { field = v; invalidate() }
    var crossover: Int = 80
        set(v) { field = v; invalidate() }
    var accentColor: Int = Palette.ACCENT
        set(v) { field = v; invalidate() }
    var theme: ThemeColors = ThemeColors.DARK
        set(v) { field = v; invalidate() }

    private val d = resources.displayMetrics.density
    private fun dp(v: Float) = v * d
    private val freqs = listOf(20 to "20", 50 to "50", 100 to "100", 200 to "200", 500 to "500",
        1000 to "1k", 2000 to "2k", 5000 to "5k", 10000 to "10k", 20000 to "20k")

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeWidth = dp(1f) }
    private val zeroPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeWidth = dp(1f) }
    private val spkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = dp(2.4f); alpha = 200
    }
    private val subAreaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val subLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = dp(3.2f); color = Palette.SUB
        strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
    }
    private val xoPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = dp(1.4f); pathEffect = DashPathEffect(floatArrayOf(dp(4f), dp(5f)), 0f); alpha = 160
    }
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = Typeface.DEFAULT_BOLD; textAlign = Paint.Align.CENTER }
    private val tagText = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = Typeface.DEFAULT_BOLD; textAlign = Paint.Align.LEFT }

    private fun padTop() = dp(14f)
    private fun padBottom() = dp(28f)
    private fun padL() = dp(10f)
    private fun padR() = dp(10f)

    private fun fx(f: Float): Float {
        val plotW = width - padL() - padR()
        return padL() + (log10(f) - log10(20f)) / 3f * plotW
    }

    override fun onMeasure(wSpec: Int, hSpec: Int) {
        val d = resources.displayMetrics.density
        setMeasuredDimension(
            resolveSize((320 * d).toInt(), wSpec),
            resolveSize((170 * d).toInt(), hSpec)
        )
    }

    override fun onDraw(canvas: Canvas) {
        if (width == 0 || height == 0) return
        val plotH = height - padTop() - padBottom()
        val floorY = padTop() + plotH
        val zeroY = padTop() + plotH / 2f
        val subY = zeroY - subLevel / 12f * (plotH / 2f)

        gridPaint.color = theme.gridLine
        freqs.forEach { (f, _) -> canvas.drawLine(fx(f.toFloat()), padTop(), fx(f.toFloat()), floorY, gridPaint) }
        zeroPaint.color = theme.zeroLine
        canvas.drawLine(padL(), zeroY, width - padR(), zeroY, zeroPaint)

        val xo = fx(crossover.toFloat())
        val xEnd = fx(crossover * 3f)
        val xStart = fx(crossover / 3f)

        val subLine = Path().apply {
            moveTo(fx(20f), subY); lineTo(xo, subY)
            cubicTo((xo + xEnd) / 2f, subY, (xo + xEnd) / 2f, floorY, xEnd, floorY)
        }
        val subArea = Path(subLine).apply { lineTo(fx(20f), floorY); close() }
        val spkPath = Path().apply {
            moveTo(xStart, floorY)
            cubicTo((xStart + xo) / 2f, floorY, (xStart + xo) / 2f, zeroY, xo, zeroY)
            lineTo(fx(20000f), zeroY)
        }

        subAreaPaint.shader = LinearGradient(0f, padTop(), 0f, floorY,
            Color.argb(170, 255, 138, 61), Color.argb(5, 255, 138, 61), Shader.TileMode.CLAMP)
        canvas.drawPath(subArea, subAreaPaint)
        spkPaint.color = accentColor
        canvas.drawPath(spkPath, spkPaint)
        canvas.drawPath(subLine, subLinePaint)

        xoPaint.color = theme.text
        canvas.drawLine(xo, padTop(), xo, floorY, xoPaint)
        tagText.textSize = dp(11f); tagText.color = theme.knobText
        canvas.drawText("CROSSOVER $crossover Hz", xo + dp(6f), padTop() + dp(10f), tagText)

        text.textAlign = Paint.Align.CENTER; text.textSize = dp(11f); text.color = theme.freqLabel
        freqs.forEach { (f, label) -> canvas.drawText(label, fx(f.toFloat()), floorY + dp(18f), text) }
    }
}
