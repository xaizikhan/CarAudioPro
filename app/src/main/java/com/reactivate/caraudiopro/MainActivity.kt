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
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView

class MainActivity : Activity() {

    private lateinit var st: AudioSettings
    private lateinit var root: LinearLayout
    private var liveDot: View? = null
    private var curveView: SoundCurveView? = null
    private var eqValueTv: TextView? = null
    private var eqFreqTv: TextView? = null
    private var tab = "easy"           // easy | eq | sub | setup
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

    private val FREQ_LABELS = listOf("25","40","63","100","160","250","400","630","1k","1.6k","2.5k","4k","6.3k","10k","16k")
    private val W_BASS = floatArrayOf(1f,1f,1f,0.8f,0.5f,0.2f,0f,0f,0f,0f,0f,0f,0f,0f,0f)
    private val W_MID = floatArrayOf(0f,0f,0f,0f,0.3f,0.7f,1f,1f,1f,0.7f,0.3f,0f,0f,0f,0f)
    private val W_TREBLE = floatArrayOf(0f,0f,0f,0f,0f,0f,0f,0f,0f,0.2f,0.5f,0.8f,1f,1f,1f)

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

    /**
     * Rebuilds the screen from current state. Everything below the header is
     * weighted, not fixed in dp, so the same layout fits a 1024x600 head unit
     * and a 1280x800 tablet without clipping.
     */
    private fun render() {
        val t = theme
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = vertGradient(t.pageBgTop, t.pageBgBottom, 0)
            setPadding(dp(16), dp(10), dp(16), dp(10))
        }
        setContentView(root)

        root.addView(buildHeader(t))
        root.addView(spacer(10))

        if (tab == "easy" || tab == "eq") {
            root.addView(buildGraphPanel(t), LinearLayout.LayoutParams(-1, 0, 1f))
            root.addView(spacer(10))
        }

        when (tab) {
            "easy" -> {
                root.addView(buildPresetRow(t))
                root.addView(spacer(10))
                root.addView(buildEasyTab(t))
            }
            "eq" -> root.addView(buildEqTab(t))
            "sub" -> root.addView(buildSubTab(t), LinearLayout.LayoutParams(-1, 0, 1f))
            "setup" -> root.addView(buildSetupTab(t), LinearLayout.LayoutParams(-1, 0, 1f))
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

        val title = LinearLayout(this).apply { minimumWidth = dp(220); gravity = Gravity.CENTER_VERTICAL }
        title.addView(label("OWAIS CAR AUDIO PRO", 15f, t.text, true))
        title.addView(label(" DSP", 15f, t.mutedText, false))
        row.addView(title)

        val pill = LinearLayout(this).apply {
            background = pillGradient(t.pillBgTop, t.pillBgBottom, t.panelBorder, dp(22))
            setPadding(dp(4), dp(4), dp(4), dp(4))
        }
        listOf("easy" to "CUSTOM", "eq" to "EQUALIZER", "sub" to "SUBWOOFER", "setup" to "SETUP").forEach { (id, lbl) ->
            pill.addView(tabChip(lbl, tab == id, t) { tab = id; render() },
                LinearLayout.LayoutParams(0, dp(40), 1f).apply { setMargins(dp(2), 0, dp(2), 0) })
        }
        row.addView(pill, LinearLayout.LayoutParams(0, -2, 1f).apply { leftMargin = dp(14); rightMargin = dp(14) })

        val live = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        val dot = View(this).apply {
            background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(Palette.LIVE_GREEN) }
            alpha = if (liveOn) 1f else 0.35f
        }
        liveDot = dot
        live.addView(dot, LinearLayout.LayoutParams(dp(8), dp(8)).apply { rightMargin = dp(6) })
        live.addView(label("LIVE", 11f, t.mutedText, false))
        row.addView(live, LinearLayout.LayoutParams(-2, -2).apply { rightMargin = dp(10) })

        row.addView(roundIconButton(t, "i", t.knobText) { showDiagnostics() },
            LinearLayout.LayoutParams(dp(42), dp(42)).apply { rightMargin = dp(8) })

        row.addView(roundIconButton(t, if (st.theme == "light") "\u263E" else "\u2600", t.knobText) {
            st.theme = if (st.theme == "light") "dark" else "light"; st.save(this@MainActivity); render()
        }, LinearLayout.LayoutParams(dp(42), dp(42)).apply { rightMargin = dp(8) })

        val powerOn = st.enabled
        row.addView(powerButton(t, powerOn) { st.enabled = !st.enabled; commit(); render() },
            LinearLayout.LayoutParams(dp(42), dp(42)))

        return row
    }

    private fun roundIconButton(t: ThemeColors, txt: String, color: Int, onClick: () -> Unit) = TextView(this).apply {
        text = txt; textSize = 16f; gravity = Gravity.CENTER
        setTypeface(null, Typeface.BOLD)
        setTextColor(color)
        background = circleGradient(t.knobBgTop, t.knobBgBottom, t.knobBorder)
        elevation = dpf(3f)
        setOnClickListener { onClick() }
    }

    private fun powerButton(t: ThemeColors, on: Boolean, onClick: () -> Unit) = TextView(this).apply {
        text = "\u23FB"; textSize = 17f; gravity = Gravity.CENTER
        setTextColor(if (on) Palette.ACCENT else t.knobText)
        background = circleGradient(t.knobBgTop, t.knobBgBottom, if (on) Palette.ACCENT else t.knobBorder)
        elevation = dpf(3f)
        setOnClickListener { onClick() }
    }

    // ---------- sound curve panel (CUSTOM + EQUALIZER) ----------
    private fun buildGraphPanel(t: ThemeColors): View {
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = panelBg(t)
            elevation = dpf(4f)
            setPadding(dp(14), dp(12), dp(14), dp(10))
        }
        panel.addView(label(if (tab == "eq") "15-Band Equalizer" else "Sound Curve", 16f, t.headText, true))
        panel.addView(spacer(6))

        val curve = SoundCurveView(this).apply {
            bands = st.bands
            interactive = tab == "eq"
            selected = selBand
            theme = t
            onBandTap = { i -> selBand = i; tab = "eq"; render() }
            onBandDrag = { i, v ->
                val rv = (Math.round(v * 2f) / 2f).coerceIn(-12f, 12f)
                st.bands[i] = rv
                selBand = i
                st.preset = -1
                AudioEngine.apply(st)
                curveView?.let { sv -> sv.bands = st.bands; sv.selected = i }
                eqValueTv?.text = fmtDb(rv)
                eqFreqTv?.let { tv -> tv.text = FREQ_LABELS[i] + " Hz"; tv.setTextColor(SpectrumColors.at(i / 14f)) }
            }
            onBandDragEnd = { st.save(this@MainActivity) }
        }
        curveView = curve
        panel.addView(curve, LinearLayout.LayoutParams(-1, 0, 1f))
        return panel
    }

    // ---------- preset row (CUSTOM tab) ----------
    private fun buildPresetRow(t: ThemeColors): View {
        val row = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        row.addView(label("PRESETS", 11f, t.mutedText, false),
            LinearLayout.LayoutParams(dp(64), -2))
        AudioSettings.PRESETS.forEachIndexed { i, (name, data) ->
            row.addView(pillChip(name, st.preset == i, Palette.ACCENT, Palette.ACCENT_DEEP, t) {
                st.bands = data.first.copyOf()
                st.subLevel = data.second
                st.preset = i
                commit(); render()
            }, LinearLayout.LayoutParams(0, dp(46), 1f).apply { setMargins(dp(4), 0, dp(4), 0) })
        }
        return row
    }

    // ---------- CUSTOM tab: Bass / Mid / Treble ----------
    private fun buildEasyTab(t: ThemeColors): View {
        val row = LinearLayout(this)

        fun avg(w: FloatArray): Float {
            var num = 0f; var den = 0f
            for (i in 0 until 15) { num += st.bands[i] * w[i]; den += w[i] }
            return if (den > 0f) num / den else 0f
        }
        // Live while dragging: engine + numbers update instantly, no full rebuild per tick.
        fun shiftLive(w: FloatArray, delta: Float, valueTv: TextView) {
            for (i in 0 until 15) if (w[i] > 0f) st.bands[i] = (st.bands[i] + delta * w[i]).coerceIn(-12f, 12f)
            st.preset = -1
            AudioEngine.apply(st)
            valueTv.text = fmtDb(avg(w))
            curveView?.let { it.bands = st.bands }
        }
        fun shiftStep(w: FloatArray, delta: Float) {
            for (i in 0 until 15) if (w[i] > 0f) st.bands[i] = (st.bands[i] + delta * w[i]).coerceIn(-12f, 12f)
            st.preset = -1
            commit(); render()
        }

        val bassValue = TextView(this)
        row.addView(knobCard(t, "Bass", "Deep & Powerful", IconKind.BASS, avg(W_BASS), bassValue,
            minus = { shiftStep(W_BASS, -1f) }, plus = { shiftStep(W_BASS, 1f) },
            onSlide = { v -> shiftLive(W_BASS, v - avg(W_BASS), bassValue) },
            onSlideEnd = { st.save(this) }), LinearLayout.LayoutParams(0, -2, 1f).apply { rightMargin = dp(8) })

        val midValue = TextView(this)
        row.addView(knobCard(t, "Mid", "Clear & Balanced", IconKind.MID, avg(W_MID), midValue,
            minus = { shiftStep(W_MID, -1f) }, plus = { shiftStep(W_MID, 1f) },
            onSlide = { v -> shiftLive(W_MID, v - avg(W_MID), midValue) },
            onSlideEnd = { st.save(this) }), LinearLayout.LayoutParams(0, -2, 1f).apply { leftMargin = dp(4); rightMargin = dp(4) })

        val trebleValue = TextView(this)
        row.addView(knobCard(t, "Treble", "Crisp & Detailed", IconKind.TREBLE, avg(W_TREBLE), trebleValue,
            minus = { shiftStep(W_TREBLE, -1f) }, plus = { shiftStep(W_TREBLE, 1f) },
            onSlide = { v -> shiftLive(W_TREBLE, v - avg(W_TREBLE), trebleValue) },
            onSlideEnd = { st.save(this) }), LinearLayout.LayoutParams(0, -2, 1f).apply { leftMargin = dp(8) })
        return row
    }

    private fun knobCard(t: ThemeColors, title: String, hint: String, icon: IconKind, value: Float, valueTv: TextView,
                         minus: () -> Unit, plus: () -> Unit, onSlide: (Float) -> Unit, onSlideEnd: () -> Unit): View {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = panelBg(t)
            elevation = dpf(4f)
            setPadding(dp(16), dp(14), dp(16), dp(14))
        }

        val head = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        head.addView(BandIconView(this, icon).apply {
            iconColor = t.knobText
            background = circleGradient(t.knobBgTop, t.knobBgBottom, t.knobBorder)
            elevation = dpf(3f)
        }, LinearLayout.LayoutParams(dp(42), dp(42)).apply { rightMargin = dp(12) })

        val texts = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        texts.addView(label(title, 15f, t.text, true))
        texts.addView(label(hint, 11f, t.mutedText, false))
        head.addView(texts, LinearLayout.LayoutParams(0, -2, 1f))

        valueTv.apply { text = fmtDb(value); textSize = 22f; setTextColor(t.text); setTypeface(null, Typeface.BOLD) }
        head.addView(valueTv)
        card.addView(head)
        card.addView(spacer(10))

        val sliderRow = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        sliderRow.addView(knobButton(t, "\u2212") { minus() }, LinearLayout.LayoutParams(dp(48), dp(48)))
        val seek = SeekBar(this).apply {
            max = 48
            progress = ((value + 12f) * 2f).toInt().coerceIn(0, 48)
            styleSlimSeekBar(this, t.track, Palette.ACCENT)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar, p: Int, fromUser: Boolean) {
                    if (fromUser) onSlide(p / 2f - 12f)
                }
                override fun onStartTrackingTouch(sb: SeekBar) {}
                override fun onStopTrackingTouch(sb: SeekBar) { onSlideEnd() }
            })
        }
        sliderRow.addView(seek, LinearLayout.LayoutParams(0, -2, 1f).apply { leftMargin = dp(10); rightMargin = dp(10) })
        sliderRow.addView(knobButton(t, "+") { plus() }, LinearLayout.LayoutParams(dp(48), dp(48)))
        card.addView(sliderRow)
        return card
    }

    private fun knobButton(t: ThemeColors, txt: String, onClick: () -> Unit) = TextView(this).apply {
        text = txt; textSize = 20f; setTypeface(null, Typeface.BOLD); gravity = Gravity.CENTER
        setTextColor(t.knobText)
        background = circleGradient(t.knobBgTop, t.knobBgBottom, t.knobBorder)
        elevation = dpf(3f)
        setOnClickListener { onClick() }
    }

    // ---------- EQUALIZER tab ----------
    private fun buildEqTab(t: ThemeColors): View {
        val row = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            background = panelBg(t)
            elevation = dpf(4f)
            setPadding(dp(14), dp(12), dp(14), dp(12))
        }
        val bandColor = SpectrumColors.at(selBand / 14f)

        row.addView(knobButton(t, "\u2039") { selBand = (selBand - 1).coerceAtLeast(0); render() },
            LinearLayout.LayoutParams(dp(48), dp(48)))
        val mid = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL; minimumWidth = dp(110)
        }
        mid.addView(label("BAND", 10f, t.mutedText, false))
        val freqTv = label(FREQ_LABELS[selBand] + " Hz", 20f, bandColor, true)
        eqFreqTv = freqTv
        mid.addView(freqTv)
        row.addView(mid, LinearLayout.LayoutParams(-2, -2).apply { leftMargin = dp(8); rightMargin = dp(8) })
        row.addView(knobButton(t, "\u203A") { selBand = (selBand + 1).coerceAtMost(14); render() },
            LinearLayout.LayoutParams(dp(48), dp(48)))

        row.addView(View(this).apply { setBackgroundColor(t.panelBorder) },
            LinearLayout.LayoutParams(dp(1), dp(36)).apply { leftMargin = dp(12); rightMargin = dp(12) })

        row.addView(knobButton(t, "\u2212") {
            st.bands[selBand] = (st.bands[selBand] - 0.5f).coerceIn(-12f, 12f); st.preset = -1; commit(); render()
        }, LinearLayout.LayoutParams(dp(48), dp(48)))
        val valueTv = label(fmtDb(st.bands[selBand]), 24f, t.text, true).apply { gravity = Gravity.CENTER }
        eqValueTv = valueTv
        row.addView(valueTv, LinearLayout.LayoutParams(dp(110), -2))
        row.addView(knobButton(t, "+") {
            st.bands[selBand] = (st.bands[selBand] + 0.5f).coerceIn(-12f, 12f); st.preset = -1; commit(); render()
        }, LinearLayout.LayoutParams(dp(48), dp(48)))

        row.addView(View(this), LinearLayout.LayoutParams(0, -2, 1f))
        row.addView(pillChip("FLAT", false, Palette.ACCENT, Palette.ACCENT_DEEP, t) {
            for (i in 0 until 15) st.bands[i] = 0f
            st.preset = -1
            commit(); render()
        }, LinearLayout.LayoutParams(dp(96), dp(48)))
        return row
    }

    // ---------- SUBWOOFER tab ----------
    private fun buildSubTab(t: ThemeColors): View {
        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        val splitPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = panelBg(t)
            elevation = dpf(4f)
            setPadding(dp(14), dp(12), dp(14), dp(8))
        }
        val head = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        head.addView(label("Speakers + Subwoofer Split", 16f, t.headText, true), LinearLayout.LayoutParams(0, -2, 1f))
        val legend = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        legend.addView(label("\u25A0", 11f, Palette.SUB, true))
        legend.addView(label(" Subwoofer   ", 11f, t.mutedText, false))
        legend.addView(label("\u25A0", 11f, Palette.ACCENT, true))
        legend.addView(label(" Speakers", 11f, t.mutedText, false))
        head.addView(legend)
        splitPanel.addView(head)
        splitPanel.addView(spacer(4))
        val split = SubSplitView(this).apply {
            subLevel = st.subLevel; crossover = st.subFreq; accentColor = Palette.ACCENT; theme = t
        }
        splitPanel.addView(split, LinearLayout.LayoutParams(-1, 0, 1f))
        col.addView(splitPanel, LinearLayout.LayoutParams(-1, 0, 1f))
        col.addView(spacer(10))

        val bottomRow = LinearLayout(this)

        val subCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = panelBg(t)
            elevation = dpf(4f)
            setPadding(dp(16), dp(14), dp(16), dp(14))
        }
        val subHead = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        subHead.addView(label("SUB LEVEL", 15f, Palette.SUB, true), LinearLayout.LayoutParams(0, -2, 1f))
        val subValueTv = label(fmtDb(st.subLevel), 22f, t.text, true)
        subHead.addView(subValueTv)
        subCard.addView(subHead)
        subCard.addView(spacer(10))

        fun liveSub(v: Float) {
            st.subLevel = v.coerceIn(-12f, 12f)
            AudioEngine.apply(st)
            subValueTv.text = fmtDb(st.subLevel)
            split.subLevel = st.subLevel
        }
        fun stepSub(delta: Float) { st.subLevel = (st.subLevel + delta).coerceIn(-12f, 12f); commit(); render() }

        val subSlideRow = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        subSlideRow.addView(knobButton(t, "\u2212") { stepSub(-1f) }, LinearLayout.LayoutParams(dp(48), dp(48)))
        val subSeek = SeekBar(this).apply {
            max = 48; progress = ((st.subLevel + 12f) * 2f).toInt().coerceIn(0, 48)
            styleSlimSeekBar(this, t.track, Palette.SUB)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar, p: Int, fromUser: Boolean) {
                    if (fromUser) liveSub(p / 2f - 12f)
                }
                override fun onStartTrackingTouch(sb: SeekBar) {}
                override fun onStopTrackingTouch(sb: SeekBar) { st.save(this@MainActivity) }
            })
        }
        subSlideRow.addView(subSeek, LinearLayout.LayoutParams(0, -2, 1f).apply { leftMargin = dp(10); rightMargin = dp(10) })
        subSlideRow.addView(knobButton(t, "+") { stepSub(1f) }, LinearLayout.LayoutParams(dp(48), dp(48)))
        subCard.addView(subSlideRow)
        bottomRow.addView(subCard, LinearLayout.LayoutParams(0, -1, 1f).apply { rightMargin = dp(8) })

        val xoCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = panelBg(t)
            elevation = dpf(4f)
            setPadding(dp(16), dp(14), dp(16), dp(14))
        }
        xoCard.addView(label("CROSSOVER", 15f, t.text, true))
        xoCard.addView(spacer(10))
        val xoGrid = LinearLayout(this)
        listOf(60, 80, 100, 120).forEach { f ->
            xoGrid.addView(pillChip("$f Hz", st.subFreq == f, Palette.SUB, Palette.SUB_DEEP, t) {
                st.subFreq = f; commit(); render()
            }, LinearLayout.LayoutParams(0, dp(46), 1f).apply { setMargins(dp(3), 0, dp(3), 0) })
        }
        xoCard.addView(xoGrid)
        xoCard.addView(spacer(8))
        xoCard.addView(label("Set the amp (CH3/CH4) LPF near this frequency too.", 11f, t.mutedText, false))
        bottomRow.addView(xoCard, LinearLayout.LayoutParams(0, -1, 1f).apply { leftMargin = dp(8) })

        col.addView(bottomRow, LinearLayout.LayoutParams(-1, -2))
        return col
    }

    // ---------- SETUP tab ----------
    private fun buildSetupTab(t: ThemeColors): View {
        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        // --- row 1: loudness + balance ---
        val row1 = LinearLayout(this)

        val loudCard = card(t)
        val loudRow = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        val loudTexts = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        loudTexts.addView(label("Loudness", 16f, t.text, true))
        loudTexts.addView(label("Gentle bass and treble lift at low volume", 11f, t.mutedText, false))
        loudRow.addView(loudTexts, LinearLayout.LayoutParams(0, -2, 1f))
        loudRow.addView(toggleSwitch(t, st.loudness) {
            st.loudness = !st.loudness; commit(); render()
        }, LinearLayout.LayoutParams(dp(60), dp(32)))
        loudCard.addView(loudRow)
        row1.addView(loudCard, LinearLayout.LayoutParams(0, -1, 1f).apply { rightMargin = dp(8) })

        val balCard = card(t)
        val balHead = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        balHead.addView(label("Balance", 16f, t.text, true), LinearLayout.LayoutParams(0, -2, 1f))
        val balWord = if (st.balance == 0) "Both sides equal"
            else if (st.balance < 0) "Louder on the left" else "Louder on the right"
        balHead.addView(label(balWord, 11f, t.mutedText, false))
        balCard.addView(balHead)
        balCard.addView(spacer(10))

        val balRow = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        balRow.addView(sideTag(t, "L", "Driver", st.balance < 0), LinearLayout.LayoutParams(dp(56), -2).apply { rightMargin = dp(10) })

        val balMid = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val balBtns = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        balBtns.addView(knobButton(t, "\u2212") {
            st.balance = (st.balance - 1).coerceAtLeast(-6); commit(); render()
        }, LinearLayout.LayoutParams(dp(40), dp(40)))
        balBtns.addView(BalanceTrackView(this).apply {
            value = st.balance; trackColor = t.track; markColor = t.panelBorder
        }, LinearLayout.LayoutParams(0, dp(24), 1f).apply { leftMargin = dp(8); rightMargin = dp(8) })
        balBtns.addView(knobButton(t, "+") {
            st.balance = (st.balance + 1).coerceAtMost(6); commit(); render()
        }, LinearLayout.LayoutParams(dp(40), dp(40)))
        balMid.addView(balBtns)
        balMid.addView(spacer(6))
        balMid.addView(pillChip("CENTER", false, Palette.ACCENT, Palette.ACCENT_DEEP, t) {
            st.balance = 0; commit(); render()
        }.apply { alpha = if (st.balance == 0) 0.45f else 1f }, LinearLayout.LayoutParams(-1, dp(36)))
        balRow.addView(balMid, LinearLayout.LayoutParams(0, -2, 1f))

        balRow.addView(sideTag(t, "R", "Passenger", st.balance > 0), LinearLayout.LayoutParams(dp(56), -2).apply { leftMargin = dp(10) })
        balCard.addView(balRow)
        row1.addView(balCard, LinearLayout.LayoutParams(0, -1, 1f).apply { leftMargin = dp(8) })

        col.addView(row1, LinearLayout.LayoutParams(-1, 0, 1f))
        col.addView(spacer(12))

        // --- row 2: high-pass + headroom ---
        val row2 = LinearLayout(this)

        val hpCard = card(t)
        hpCard.addView(label("Speaker High-Pass", 16f, t.text, true))
        hpCard.addView(label("Remove low frequencies from the speakers \u2014 less distortion, let the sub do the work",
            11f, t.mutedText, false))
        hpCard.addView(spacer(10))
        val hpGrid = LinearLayout(this)
        listOf(0, 60, 80, 100).forEach { f ->
            hpGrid.addView(pillChip(if (f == 0) "Off" else "$f Hz", st.highPass == f, Palette.ACCENT, Palette.ACCENT_DEEP, t) {
                st.highPass = f; commit(); render()
            }, LinearLayout.LayoutParams(0, dp(46), 1f).apply { setMargins(dp(3), 0, dp(3), 0) })
        }
        hpCard.addView(hpGrid)
        row2.addView(hpCard, LinearLayout.LayoutParams(0, -1, 1f).apply { rightMargin = dp(8) })

        val hrCard = card(t)
        val headroom = AudioEngine.headroom(st)
        val maxBoost = maxBoost()
        val hrColor = if (maxBoost >= 9f) Palette.DANGER else if (maxBoost >= 5f) Palette.WARN else Palette.OK
        val hrHead = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        hrHead.addView(label("Headroom", 16f, t.text, true), LinearLayout.LayoutParams(0, -2, 1f))
        hrHead.addView(label("%.1f dB".format(headroom), 20f, hrColor, true))
        hrCard.addView(hrHead)
        hrCard.addView(spacer(10))
        hrCard.addView(BarView(this).apply {
            fraction = (maxBoost / 12f).coerceIn(0f, 1f)
            barColor = hrColor
            trackColor = t.track
        }, LinearLayout.LayoutParams(-1, dp(10)))
        hrCard.addView(spacer(8))
        val hrNote = if (maxBoost >= 9f) "Very high boost \u2014 keep the volume down or it may clip."
            else if (maxBoost >= 5f) "Fine, but watch out at high volume."
            else "Safe \u2014 no risk of clipping."
        hrCard.addView(label(hrNote, 11f, t.mutedText, false))
        row2.addView(hrCard, LinearLayout.LayoutParams(0, -1, 1f).apply { leftMargin = dp(8) })

        col.addView(row2, LinearLayout.LayoutParams(-1, 0, 1f))
        return col
    }

    private fun maxBoost(): Float {
        val bands = AudioEngine.withLoudness(st)
        var mx = 0f
        for (i in 0 until 15) {
            val g = bands[i] + if (AudioEngine.CENTERS[i] <= st.subFreq) st.subLevel else 0f
            if (g > mx) mx = g
        }
        return mx
    }

    private fun card(t: ThemeColors) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        background = panelBg(t)
        elevation = dpf(4f)
        setPadding(dp(18), dp(16), dp(18), dp(16))
        gravity = Gravity.CENTER_VERTICAL
    }

    private fun sideTag(t: ThemeColors, letter: String, who: String, active: Boolean): View {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, dp(8))
            background = if (active) pillGradient(Palette.ACCENT, Palette.ACCENT_DEEP, Palette.ACCENT, dp(12))
                else GradientDrawable().apply { cornerRadius = dpf(12f); setColor(Color.TRANSPARENT) }
        }
        box.addView(label(letter, 19f, if (active) Color.WHITE else t.mutedText, true).apply { gravity = Gravity.CENTER })
        box.addView(label(who, 10f, if (active) Color.WHITE else t.mutedText, false).apply { gravity = Gravity.CENTER })
        return box
    }

    private fun toggleSwitch(t: ThemeColors, on: Boolean, onClick: () -> Unit): View {
        val wrap = LinearLayout(this).apply {
            gravity = if (on) Gravity.END or Gravity.CENTER_VERTICAL else Gravity.START or Gravity.CENTER_VERTICAL
            setPadding(dp(3), dp(3), dp(3), dp(3))
            background = pillGradient(
                if (on) Palette.ACCENT else t.knobBgTop,
                if (on) Palette.ACCENT_DEEP else t.knobBgBottom,
                if (on) Palette.ACCENT else t.knobBorder, dp(16))
            setOnClickListener { onClick() }
        }
        wrap.addView(View(this).apply {
            background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(Color.WHITE) }
        }, LinearLayout.LayoutParams(dp(26), dp(26)))
        return wrap
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

    /** One pill recipe for every pill button in the app: presets, crossover, high-pass, FLAT. */
    private fun pillChip(text: String, active: Boolean, onTop: Int, onBottom: Int, t: ThemeColors,
                         onClick: () -> Unit) = TextView(this).apply {
        this.text = text; textSize = 13f; setTypeface(null, Typeface.BOLD); gravity = Gravity.CENTER
        setTextColor(if (active) Color.WHITE else t.knobText)
        background = if (active) pillGradient(onTop, onBottom, onTop, dp(12))
            else pillGradient(t.knobBgTop, t.knobBgBottom, t.knobBorder, dp(12))
        elevation = dpf(3f)
        setOnClickListener { onClick() }
    }

    private fun tabChip(text: String, active: Boolean, t: ThemeColors, onClick: () -> Unit) = TextView(this).apply {
        this.text = text; textSize = 12f; setTypeface(null, Typeface.BOLD); gravity = Gravity.CENTER
        setTextColor(if (active) t.tabOnText else t.mutedText)
        background = if (active) pillGradient(t.tabOnTop, t.tabOnBottom, t.tabOnTop, dp(20))
            else GradientDrawable().apply { cornerRadius = dpf(20f); setColor(Color.TRANSPARENT) }
        if (active) elevation = dpf(3f)
        setOnClickListener { onClick() }
    }

    private fun panelBg(t: ThemeColors) = pillGradient(t.panelBgTop, t.panelBgBottom, t.panelBorder, dp(18))

    private fun vertGradient(top: Int, bottom: Int, radius: Int) = GradientDrawable(
        GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(top, bottom)
    ).apply { cornerRadius = radius.toFloat() }

    private fun pillGradient(top: Int, bottom: Int, stroke: Int, radius: Int) = GradientDrawable(
        GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(top, bottom)
    ).apply {
        cornerRadius = radius.toFloat()
        setStroke(dp(1), stroke)
    }

    private fun circleGradient(top: Int, bottom: Int, stroke: Int) = GradientDrawable(
        GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(top, bottom)
    ).apply {
        shape = GradientDrawable.OVAL
        setStroke(dp(1), stroke)
    }

    /** A slim, fixed-look track + round thumb — independent of the car unit's own Android skin. */
    private fun styleSlimSeekBar(seek: SeekBar, trackColor: Int, progressColor: Int) {
        val barH = dp(6); val totalH = dp(40); val inset = (totalH - barH) / 2
        val bg = GradientDrawable().apply { cornerRadius = barH / 2f; setColor(trackColor) }
        val fg = GradientDrawable().apply { cornerRadius = barH / 2f; setColor(progressColor) }
        val clip = android.graphics.drawable.ClipDrawable(fg, Gravity.START, android.graphics.drawable.ClipDrawable.HORIZONTAL)
        val layer = android.graphics.drawable.LayerDrawable(arrayOf(bg, clip))
        layer.setId(0, android.R.id.background)
        layer.setId(1, android.R.id.progress)
        layer.setLayerInset(0, 0, inset, 0, inset)
        layer.setLayerInset(1, 0, inset, 0, inset)
        seek.progressDrawable = layer
        seek.thumb = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            colors = intArrayOf(Color.parseColor("#FBFCFD"), Color.parseColor("#BFC5D0"))
            orientation = GradientDrawable.Orientation.TOP_BOTTOM
            setStroke(dp(1), Color.parseColor("#8C939F"))
            setSize(dp(24), dp(24))
        }
        seek.minimumHeight = totalH
    }
}
