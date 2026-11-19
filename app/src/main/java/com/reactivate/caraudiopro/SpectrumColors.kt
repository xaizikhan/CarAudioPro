package com.reactivate.caraudiopro

import android.graphics.Color

/** Same spectrum used in the approved design: bass=red, mids=green, treble=purple. */
object SpectrumColors {
    private val stops = floatArrayOf(0f, 0.2f, 0.4f, 0.6f, 0.8f, 1f)
    private val colors = intArrayOf(
        Color.rgb(255, 59, 92),   // red
        Color.rgb(255, 138, 61),  // orange
        Color.rgb(255, 210, 61),  // yellow
        Color.rgb(62, 217, 122),  // green
        Color.rgb(34, 211, 238),  // cyan
        Color.rgb(176, 124, 255)  // purple
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
