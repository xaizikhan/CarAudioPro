package com.reactivate.caraudiopro

import android.graphics.Color

/** Soft pastel spectrum used by the sound curve: warm on the left, cool on the right. */
object SpectrumColors {
    private val stops = floatArrayOf(0f, 0.3f, 0.55f, 0.8f, 1f)
    private val colors = intArrayOf(
        Color.rgb(240, 138, 138), // soft red
        Color.rgb(232, 181, 122), // soft amber
        Color.rgb(134, 201, 154), // soft green
        Color.rgb(127, 179, 222), // soft blue
        Color.rgb(175, 155, 219)  // soft violet
    )

    fun colorsArray() = colors
    fun stopsArray() = stops

    fun at(t: Float): Int {
        val tt = t.coerceIn(0f, 1f)
        for (i in 0 until stops.size - 1) {
            if (tt <= stops[i + 1]) {
                val f = (tt - stops[i]) / (stops[i + 1] - stops[i])
                return Color.rgb(
                    (Color.red(colors[i]) + (Color.red(colors[i + 1]) - Color.red(colors[i])) * f).toInt(),
                    (Color.green(colors[i]) + (Color.green(colors[i + 1]) - Color.green(colors[i])) * f).toInt(),
                    (Color.blue(colors[i]) + (Color.blue(colors[i + 1]) - Color.blue(colors[i])) * f).toInt()
                )
            }
        }
        return colors.last()
    }
}
