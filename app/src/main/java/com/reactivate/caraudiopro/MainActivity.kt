package com.reactivate.caraudiopro

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView

class MainActivity : Activity() {

    private lateinit var st: AudioSettings
    private lateinit var root: LinearLayout
    private var liveDot: View? = null
    private var tab = "easy"           // easy | eq | sub
    private var selBand = 2
    private val handler = Handler(Looper.getMainLooper())
    private var liveOn = true
    private val liveTicker = object : Runnable {
        override fun run() {
            liveOn = !liveOn
            liveDot?.alpha = if (liveOn) 1f else 0.35f
            handler.postDelayed(this, 800)
        }
    }

    private val theme get() = if (st.theme == "light") ThemeColors.LIGHT else ThemeColors.DARK
    private val d get() = resources.displayMetrics.density
    private fun dp(v: Int) = (v * d).toInt()
    private fun dpf(v: Float) = v * d

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        st = AudioSettings.load(this)
        CarAudioService.start(this)
        render()
        handler.post(liveTicker)
    }

    override fun onDestroy() { handler.removeCallbacks(liveTicker); super.onDestroy() }

    private fun commit() { st.save(this); AudioEngine.apply(st) }

    // ---------- top-level render: rebuilds the screen from current state, like the approved design ----------
    private fun render() {
        val t = theme
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(t.pageBg)
            setPadding(dp(18), dp(10), dp(18), dp(10))
        }
        setContentView(root)

        root.addView(buildHeader(t))
        root.addView(spacer(10))

        if (tab != "sub") {
            root.addView(buildGraphPanel(t))
            root.addView(spacer(12))
        }

        when (tab) {
            "easy" -> root.addView(buildEasyTab(t))
            "eq" -> root.addView(buildEqTab(t))
            "sub" -> root.addView(buildSubTab(t))
        }
    }

    private fun spacer(h: Int) = View(this).apply { layoutParams = LinearLayout.LayoutParams(-1, dp(h)) }

    private fun showDiagnostics() {
        AlertDialog.Builder(this).setTitle("Diagnostics")
            .setMessage(AudioEngine.diagnostics(this)).setPositiveButton("OK", null).show()
    }

    // ---------- header ----------
    private fun buildHeader(t: ThemeColors): View {
        val row = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }

        val title = LinearLayout(this).apply { minimumWidth = dp(220) }
        title.addView(label("CAR AUDIO PRO", 18f, t.text, true))
        title.addView(label(" DSP", 18f, Palette.ACCENT, true))
        row.addView(title)

        val pill = LinearLayout(this).apply {
            background = roundedRect(dp(14), t.panelBgTop, t.panelBorder, dp(1))
            setPadding(dp(4), dp(4), dp(4), dp(4))
        }
        listOf("easy" to "EASY", "eq" to "EQUALIZER", "sub" to "SUBWOOFER").forEach { (id, lbl) ->
            val active = tab == id
            pill.addView(tabChip(lbl, active, t) { tab = id; render() },
                LinearLayout.LayoutParams(0, dp(40), 1f).apply { setMargins(dp(2), 0, dp(2), 0) })
        }
        row.addView(pill, LinearLayout.LayoutParams(0, -2, 1f).apply { leftMargin = dp(16); rightMargin = dp(16) })

        val live = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        val dot = View(this).apply {
            background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(Palette.LIVE_GREEN) }
            alpha = if (liveOn) 1f else 0.35f
        }
        liveDot = dot
        live.addView(dot, LinearLayout.LayoutParams(dp(8), dp(8)).apply { rightMargin = dp(6) })
        live.addView(label("LIVE", 11f, t.mutedText, false))
        row.addView(live, LinearLayout.LayoutParams(-2, -2).apply { rightMargin = dp(12) })

        row.addView(chip("INFO", null, t.knobText, t) { showDiagnostics() },
            LinearLayout.LayoutParams(dp(68), dp(42)).apply { rightMargin = dp(10) })

        row.addView(iconButton(t) { st.theme = if (st.theme == "light") "dark" else "light"; st.save(this@MainActivity); render() },
            LinearLayout.LayoutParams(dp(42), dp(42)).apply { rightMargin = dp(10) })

        val powerOn = st.enabled
        row.addView(chip(if (powerOn) "ON" else "BYPASS", if (powerOn) Palette.ACCENT else null, if (powerOn) Color.BLACK else t.knobText, t) {
            st.enabled = !st.enabled; commit(); render()
        }, LinearLayout.LayoutParams(dp(104), dp(42)))

        return row
    }

    private fun iconButton(t: ThemeColors, onClick: () -> Unit): TextView = TextView(this).apply {
        text = if (st.theme == "light") "☾" else "☀"
        textSize = 18f; gravity = Gravity.CENTER
        setTextColor(t.knobText)
        background = roundedRect(dp(14), t.knobBgTop, t.knobBorder, dp(1))
        setOnClickListener { onClick() }
    }

    // ---------- sound curve panel (shared by Easy + Equalizer tabs) ----------
    private fun buildGraphPanel(t: ThemeColors): View {
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = roundedRect(dp(18), t.panelBgTop, t.panelBorder, dp(1))
            setPadding(dp(14), dp(12), dp(14), dp(10))
        }
        val headRow = LinearLayout(this)
        headRow.addView(label(if (tab == "eq") "15-BAND EQUALIZER · dot par tap karein" else "SOUND CURVE", 13f, t.headText, true),
            LinearLayout.LayoutParams(0, -2, 1f))
        headRow.addView(label("CUSTOM", 12f, Palette.ACCENT, true))
        panel.addView(headRow)
        panel.addView(spacer(6))

        val curve = SoundCurveView(this).apply {
            bands = st.bands
            interactive = tab == "eq"
            selected = selBand
            theme = t
            onBandTap = { i -> selBand = i; tab = "eq"; render() }
        }
        panel.addView(curve, LinearLayout.LayoutParams(-1, dp(if (tab == "eq") 300 else 150)))
        return panel
    }

    // ---------- EASY tab: Bass / Mid / Treble / Subwoofer, each a live knob on the custom curve ----------
    private fun buildEasyTab(t: ThemeColors): View {
        val row = LinearLayout(this)
        val wB = floatArrayOf(1f,1f,1f,0.8f,0.5f,0.2f,0f,0f,0f,0f,0f,0f,0f,0f,0f)
        val wM = floatArrayOf(0f,0f,0f,0f,0.3f,0.7f,1f,1f,1f,0.7f,0.3f,0f,0f,0f,0f)
        val wT = floatArrayOf(0f,0f,0f,0f,0f,0f,0f,0f,0f,0.2f,0.5f,0.8f,1f,1f,1f)

        fun avg(w: FloatArray): Float {
            var num = 0f; var den = 0f
            for (i in 0 until 15) { num += st.bands[i] * w[i]; den += w[i] }
            return if (den > 0f) num / den else 0f
        }
        fun shift(w: FloatArray, delta: Float) {
            for (i in 0 until 15) if (w[i] > 0f) st.bands[i] = (st.bands[i] + delta * w[i]).coerceIn(-12f, 12f)
            commit(); render()
        }

        row.addView(knobCard(t, "BASS", Palette.BASS, "Drums aur kick ka punch", avg(wB),
            minus = { shift(wB, -1f) }, plus = { shift(wB, 1f) },
            onSlide = { v -> shift(wB, v - avg(wB)) }), LinearLayout.LayoutParams(0, -1, 1f).apply { rightMargin = dp(8) })
        row.addView(knobCard(t, "MID", Palette.MID, "Vocals aur instruments", avg(wM),
            minus = { shift(wM, -1f) }, plus = { shift(wM, 1f) },
            onSlide = { v -> shift(wM, v - avg(wM)) }), LinearLayout.LayoutParams(0, -1, 1f).apply { leftMargin = dp(4); rightMargin = dp(4) })
        row.addView(knobCard(t, "TREBLE", Palette.TREBLE, "Clarity aur detail", avg(wT),
            minus = { shift(wT, -1f) }, plus = { shift(wT, 1f) },
            onSlide = { v -> shift(wT, v - avg(wT)) }), LinearLayout.LayoutParams(0, -1, 1f).apply { leftMargin = dp(4); rightMargin = dp(4) })
        row.addView(knobCard(t, "SUBWOOFER", Palette.SUB, "Sirf gehra bass", st.subLevel,
            minus = { st.subLevel = (st.subLevel - 1f).coerceIn(-12f, 12f); commit(); render() },
            plus = { st.subLevel = (st.subLevel + 1f).coerceIn(-12f, 12f); commit(); render() },
            onSlide = { v -> st.subLevel = v.coerceIn(-12f, 12f); commit(); render() }), LinearLayout.LayoutParams(0, -1, 1f).apply { leftMargin = dp(8) })
        return row
    }

    private fun knobCard(t: ThemeColors, title: String, color: Int, hint: String, value: Float,
                          minus: () -> Unit, plus: () -> Unit, onSlide: (Float) -> Unit): View {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = roundedRect(dp(16), t.panelBgTop, t.panelBorder, dp(1))
            setPadding(dp(14), dp(12), dp(14), dp(12))
        }
        val head = LinearLayout(this)
        head.addView(label(title, 15f, color, true), LinearLayout.LayoutParams(0, -2, 1f))
        head.addView(label(fmtDb(value), 24f, t.text, true))
        card.addView(head)
        card.addView(label(hint, 11f, t.mutedText, false))
        card.addView(spacer(6))

        val sliderRow = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        sliderRow.addView(knobButton(t, "−") { minus() }, LinearLayout.LayoutParams(dp(42), dp(42)))
        val seek = SeekBar(this).apply {
            max = 48
            progress = ((value + 12f) * 2f).toInt().coerceIn(0, 48)
            progressTintList = android.content.res.ColorStateList.valueOf(color)
            thumbTintList = android.content.res.ColorStateList.valueOf(Color.WHITE)
            progressBackgroundTintList = android.content.res.ColorStateList.valueOf(t.track)
            progressBackgroundTintMode = android.graphics.PorterDuff.Mode.SRC_IN
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar, p: Int, fromUser: Boolean) {
                    if (fromUser) onSlide(p / 2f - 12f)
                }
                override fun onStartTrackingTouch(sb: SeekBar) {}
                override fun onStopTrackingTouch(sb: SeekBar) {}
            })
        }
        sliderRow.addView(seek, LinearLayout.LayoutParams(0, -2, 1f).apply { leftMargin = dp(8); rightMargin = dp(8) })
        sliderRow.addView(knobButton(t, "+") { plus() }, LinearLayout.LayoutParams(dp(42), dp(42)))
        card.addView(sliderRow)
        return card
    }

    private fun knobButton(t: ThemeColors, txt: String, onClick: () -> Unit) = TextView(this).apply {
        text = txt; textSize = 20f; gravity = Gravity.CENTER; setTextColor(t.knobText)
        background = roundedRect(dp(12), t.knobBgTop, t.knobBorder, dp(1))
        setOnClickListener { onClick() }
    }

    // ---------- EQUALIZER tab: select a band from the curve, nudge it precisely ----------
    private fun buildEqTab(t: ThemeColors): View {
        val row = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            background = roundedRect(dp(16), t.panelBgTop, t.panelBorder, dp(1))
            setPadding(dp(12), dp(10), dp(12), dp(10))
        }
        val labels = listOf("25","40","63","100","160","250","400","630","1k","1.6k","2.5k","4k","6.3k","10k","16k")
        val bandColor = SpectrumColors.at(selBand / 14f)

        row.addView(knobButton(t, "‹") { selBand = (selBand - 1).coerceAtLeast(0); render() }, LinearLayout.LayoutParams(dp(48), dp(48)))
        val mid = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL; minimumWidth = dp(120) }
        mid.addView(label("BAND", 10f, t.mutedText, false))
        mid.addView(label(labels[selBand] + " Hz", 22f, bandColor, true))
        row.addView(mid, LinearLayout.LayoutParams(-2, -2).apply { leftMargin = dp(10); rightMargin = dp(10) })
        row.addView(knobButton(t, "›") { selBand = (selBand + 1).coerceAtMost(14); render() }, LinearLayout.LayoutParams(dp(48), dp(48)))

        row.addView(View(this).apply { setBackgroundColor(t.panelBorder) }, LinearLayout.LayoutParams(dp(1), dp(36)).apply { leftMargin = dp(10); rightMargin = dp(10) })

        row.addView(knobButton(t, "−") {
            st.bands[selBand] = (st.bands[selBand] - 0.5f).coerceIn(-12f, 12f); commit(); render()
        }, LinearLayout.LayoutParams(dp(56), dp(48)))
        row.addView(label(fmtDb(st.bands[selBand]), 26f, t.text, true), LinearLayout.LayoutParams(dp(110), -2).apply { gravity = Gravity.CENTER })
        row.addView(knobButton(t, "+") {
            st.bands[selBand] = (st.bands[selBand] + 0.5f).coerceIn(-12f, 12f); commit(); render()
        }, LinearLayout.LayoutParams(dp(56), dp(48)))

        row.addView(View(this), LinearLayout.LayoutParams(0, -2, 1f))
        row.addView(flatButton(t) {
            for (i in 0 until 15) st.bands[i] = 0f; commit(); render()
        })
        return row
    }

    private fun flatButton(t: ThemeColors, onClick: () -> Unit) = TextView(this).apply {
        text = "FLAT"; textSize = 13f; setTypeface(null, Typeface.BOLD); gravity = Gravity.CENTER
        setTextColor(t.knobText); setPadding(dp(20), 0, dp(20), 0)
        background = roundedRect(dp(14), t.knobBgTop, t.knobBorder, dp(1))
        setOnClickListener { onClick() }
    }

    // ---------- SUBWOOFER tab: speakers/sub split graph + sub level + crossover ----------
    private fun buildSubTab(t: ThemeColors): View {
        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        val splitPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = roundedRect(dp(18), t.panelBgTop, t.panelBorder, dp(1))
            setPadding(dp(14), dp(10), dp(14), dp(8))
        }
        val head = LinearLayout(this)
        head.addView(label("SPEAKERS + SUBWOOFER SPLIT", 13f, t.headText, true), LinearLayout.LayoutParams(0, -2, 1f))
        val legend = TextView(this).apply {
            textSize = 11f; setTextColor(t.mutedText)
            text = "■ Subwoofer   ■ Speakers"
        }
        head.addView(legend)
        splitPanel.addView(head)
        splitPanel.addView(spacer(4))
        val split = SubSplitView(this).apply {
            subLevel = st.subLevel; crossover = st.subFreq; accentColor = Palette.ACCENT; theme = t
        }
        splitPanel.addView(split, LinearLayout.LayoutParams(-1, dp(180)))
        col.addView(splitPanel)
        col.addView(spacer(12))

        val bottomRow = LinearLayout(this)
        val subCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = roundedRect(dp(16), t.panelBgTop, t.panelBorder, dp(1))
            setPadding(dp(14), dp(12), dp(14), dp(12))
        }
        val subHead = LinearLayout(this)
        subHead.addView(label("SUB LEVEL", 16f, Palette.SUB, true), LinearLayout.LayoutParams(0, -2, 1f))
        subHead.addView(label(fmtDb(st.subLevel), 26f, t.text, true))
        subCard.addView(subHead)
        subCard.addView(spacer(6))
        val subSlideRow = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        subSlideRow.addView(knobButton(t, "−") { st.subLevel = (st.subLevel - 1f).coerceIn(-12f, 12f); commit(); render() }, LinearLayout.LayoutParams(dp(46), dp(46)))
        val subSeek = SeekBar(this).apply {
            max = 48; progress = ((st.subLevel + 12f) * 2f).toInt().coerceIn(0, 48)
            progressTintList = android.content.res.ColorStateList.valueOf(Palette.SUB)
            thumbTintList = android.content.res.ColorStateList.valueOf(Color.WHITE)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar, p: Int, fromUser: Boolean) {
                    if (fromUser) { st.subLevel = (p / 2f - 12f).coerceIn(-12f, 12f); commit(); render() }
                }
                override fun onStartTrackingTouch(sb: SeekBar) {}
                override fun onStopTrackingTouch(sb: SeekBar) {}
            })
        }
        subSlideRow.addView(subSeek, LinearLayout.LayoutParams(0, -2, 1f).apply { leftMargin = dp(8); rightMargin = dp(8) })
        subSlideRow.addView(knobButton(t, "+") { st.subLevel = (st.subLevel + 1f).coerceIn(-12f, 12f); commit(); render() }, LinearLayout.LayoutParams(dp(46), dp(46)))
        subCard.addView(subSlideRow)
        bottomRow.addView(subCard, LinearLayout.LayoutParams(0, -1, 1f).apply { rightMargin = dp(8) })

        val xoCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = roundedRect(dp(16), t.panelBgTop, t.panelBorder, dp(1))
            setPadding(dp(14), dp(12), dp(14), dp(12))
        }
        xoCard.addView(label("CROSSOVER", 16f, t.text, true))
        xoCard.addView(spacer(8))
        val xoGrid = LinearLayout(this)
        listOf(60, 80, 100, 120).forEach { f ->
            val active = st.subFreq == f
            xoGrid.addView(chip("$f Hz", if (active) Palette.SUB else null, if (active) Color.BLACK else t.knobText, t) {
                st.subFreq = f; commit(); render()
            }, LinearLayout.LayoutParams(0, dp(46), 1f).apply { setMargins(dp(3), 0, dp(3), 0) })
        }
        xoCard.addView(xoGrid)
        xoCard.addView(spacer(8))
        xoCard.addView(label("Amp (CH3/CH4) ka LPF bhi isi frequency ke qareeb rakhein.", 11f, t.mutedText, false))
        bottomRow.addView(xoCard, LinearLayout.LayoutParams(0, -1, 1f).apply { leftMargin = dp(8) })

        col.addView(bottomRow, LinearLayout.LayoutParams(-1, 0, 1f))
        return col
    }

    // ---------- small helpers ----------
    private fun fmtDb(v: Float): String {
        val r = Math.round(v * 2f) / 2f
        val sign = if (r > 0f) "+" else ""
        return sign + (if (r == r.toLong().toFloat()) r.toLong().toString() else "%.1f".format(r)) + " dB"
    }

    private fun label(t: String, size: Float, color: Int, bold: Boolean) = TextView(this).apply {
        text = t; textSize = size; setTextColor(color); if (bold) setTypeface(null, Typeface.BOLD)
    }

    private fun chip(text: String, bg: Int?, textColor: Int, onClick: () -> Unit): View =
        chip(text, bg, textColor, theme, onClick)

    private fun chip(text: String, bg: Int?, textColor: Int, t: ThemeColors, onClick: () -> Unit) = TextView(this).apply {
        this.text = text; textSize = 13f; setTypeface(null, Typeface.BOLD); gravity = Gravity.CENTER
        setTextColor(textColor)
        background = if (bg != null) roundedRect(dp(10), bg, bg, 0)
        else roundedRect(dp(10), t.knobBgTop, t.knobBorder, dp(1))
        setOnClickListener { onClick() }
    }

    private fun tabChip(text: String, active: Boolean, t: ThemeColors, onClick: () -> Unit) = TextView(this).apply {
        this.text = text; textSize = 13f; setTypeface(null, Typeface.BOLD); gravity = Gravity.CENTER
        setTextColor(if (active) Color.BLACK else t.mutedText)
        background = if (active) roundedRect(dp(10), Palette.ACCENT, Palette.ACCENT, 0)
        else GradientDrawable().apply { cornerRadius = dpf(10f); setColor(Color.TRANSPARENT) }
        setOnClickListener { onClick() }
    }

    private fun roundedRect(radius: Int, fill: Int, stroke: Int, strokeW: Int): GradientDrawable =
        GradientDrawable().apply {
            cornerRadius = radius.toFloat()
            setColor(fill)
            if (strokeW > 0) setStroke(strokeW, stroke)
        }
}
