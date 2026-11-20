package com.reactivate.caraudiopro

import android.graphics.Color

/** Dark and light palettes — greyish gunmetal theme with gradients and soft shadows. */
data class ThemeColors(
    val pageBgTop: Int, val pageBgBottom: Int,
    val panelBgTop: Int, val panelBgBottom: Int, val panelBorder: Int,
    val text: Int, val mutedText: Int, val headText: Int,
    val knobBgTop: Int, val knobBgBottom: Int, val knobBorder: Int, val knobText: Int,
    val track: Int, val gridLine: Int, val zeroLine: Int, val freqLabel: Int,
    val dotHole: Int, val shadow: Int, val pillBgTop: Int, val pillBgBottom: Int,
    val tabOnTop: Int, val tabOnBottom: Int, val tabOnText: Int
) {
    companion object {
        val DARK = ThemeColors(
            pageBgTop = Color.parseColor("#1A1D23"), pageBgBottom = Color.parseColor("#121419"),
            panelBgTop = Color.parseColor("#23272F"), panelBgBottom = Color.parseColor("#1B1F26"),
            panelBorder = Color.parseColor("#333842"),
            text = Color.parseColor("#F0F2F5"), mutedText = Color.parseColor("#9AA2AE"),
            headText = Color.parseColor("#E6EAF0"),
            knobBgTop = Color.parseColor("#363C46"), knobBgBottom = Color.parseColor("#272C34"),
            knobBorder = Color.parseColor("#454C58"), knobText = Color.parseColor("#E6EAF0"),
            track = Color.parseColor("#3A4049"), gridLine = Color.parseColor("#2C313A"),
            zeroLine = Color.parseColor("#4C535E"), freqLabel = Color.parseColor("#8891A0"),
            dotHole = Color.parseColor("#1A1D23"), shadow = Color.parseColor("#73000000"),
            pillBgTop = Color.parseColor("#1E222A"), pillBgBottom = Color.parseColor("#171A20"),
            tabOnTop = Color.parseColor("#4A5160"), tabOnBottom = Color.parseColor("#2B3038"),
            tabOnText = Color.parseColor("#FFFFFF")
        )
        val LIGHT = ThemeColors(
            pageBgTop = Color.parseColor("#EEF0F4"), pageBgBottom = Color.parseColor("#DFE3E9"),
            panelBgTop = Color.parseColor("#FFFFFF"), panelBgBottom = Color.parseColor("#F1F3F7"),
            panelBorder = Color.parseColor("#D5DAE3"),
            text = Color.parseColor("#1A1D23"), mutedText = Color.parseColor("#687082"),
            headText = Color.parseColor("#2B313C"),
            knobBgTop = Color.parseColor("#FFFFFF"), knobBgBottom = Color.parseColor("#E3E7ED"),
            knobBorder = Color.parseColor("#C6CCD7"), knobText = Color.parseColor("#1A1D23"),
            track = Color.parseColor("#CCD2DC"), gridLine = Color.parseColor("#E0E4EA"),
            zeroLine = Color.parseColor("#AAB2BF"), freqLabel = Color.parseColor("#687082"),
            dotHole = Color.parseColor("#FFFFFF"), shadow = Color.parseColor("#1A1A202D"),
            pillBgTop = Color.parseColor("#E4E7ED"), pillBgBottom = Color.parseColor("#D7DBE3"),
            tabOnTop = Color.parseColor("#FFFFFF"), tabOnBottom = Color.parseColor("#EBEEF3"),
            tabOnText = Color.parseColor("#15181E")
        )
    }
}

object Palette {
    val ACCENT = Color.parseColor("#2B6FE0")
    val ACCENT_DEEP = Color.parseColor("#1D4FA8")
    val SUB = Color.parseColor("#FF8A3D")
    val SUB_DEEP = Color.parseColor("#C25E16")
    val LIVE_GREEN = Color.parseColor("#3DDC84")
    val OK = Color.parseColor("#86C99A")
    val WARN = Color.parseColor("#E8B57A")
    val DANGER = Color.parseColor("#FF6B6B")
}
