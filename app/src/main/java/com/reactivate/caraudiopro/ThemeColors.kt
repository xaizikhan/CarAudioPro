package com.reactivate.caraudiopro

import android.graphics.Color

/** Dark and light palettes, matching the approved design (no shadows anywhere). */
data class ThemeColors(
    val pageBg: Int,
    val panelBgTop: Int, val panelBgBottom: Int, val panelBorder: Int,
    val text: Int, val mutedText: Int, val headText: Int,
    val knobBgTop: Int, val knobBgBottom: Int, val knobBorder: Int, val knobText: Int,
    val track: Int, val gridLine: Int, val zeroLine: Int, val freqLabel: Int,
    val dotHole: Int
) {
    companion object {
        val DARK = ThemeColors(
            pageBg = Color.parseColor("#07080A"),
            panelBgTop = Color.parseColor("#0F1116"), panelBgBottom = Color.parseColor("#0A0B0E"),
            panelBorder = Color.parseColor("#1E2229"),
            text = Color.parseColor("#F2F4F7"), mutedText = Color.parseColor("#A8B0BA"), headText = Color.parseColor("#C3C9D1"),
            knobBgTop = Color.parseColor("#1C2027"), knobBgBottom = Color.parseColor("#161922"),
            knobBorder = Color.parseColor("#2E333B"), knobText = Color.parseColor("#F2F4F7"),
            track = Color.parseColor("#1B1E24"), gridLine = Color.parseColor("#1A1E25"), zeroLine = Color.parseColor("#525964"),
            freqLabel = Color.parseColor("#A8B0BA"), dotHole = Color.parseColor("#05060A")
        )
        val LIGHT = ThemeColors(
            pageBg = Color.parseColor("#F2F3F6"),
            panelBgTop = Color.parseColor("#FFFFFF"), panelBgBottom = Color.parseColor("#F5F6F9"),
            panelBorder = Color.parseColor("#E3E6EB"),
            text = Color.parseColor("#15171B"), mutedText = Color.parseColor("#5B6470"), headText = Color.parseColor("#2B2F36"),
            knobBgTop = Color.parseColor("#FFFFFF"), knobBgBottom = Color.parseColor("#ECEEF2"),
            knobBorder = Color.parseColor("#D6DAE1"), knobText = Color.parseColor("#15171B"),
            track = Color.parseColor("#E2E5EA"), gridLine = Color.parseColor("#E3E6EB"), zeroLine = Color.parseColor("#B9C0C9"),
            freqLabel = Color.parseColor("#5B6470"), dotHole = Color.parseColor("#FFFFFF")
        )
    }
}

object Palette {
    val ACCENT = Color.parseColor("#22B8FF")
    val BASS = Color.rgb(255, 76, 106)
    val MID = Color.rgb(62, 217, 122)
    val TREBLE = Color.rgb(34, 211, 238)
    val SUB = Color.parseColor("#FF8A3D")
    val LIVE_GREEN = Color.parseColor("#3DDC84")
}
