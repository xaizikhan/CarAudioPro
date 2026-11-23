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
    private var headroomBar: BarView? = null
    private var headroomValueTv: TextView? = null
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

    /** Keeps the headroom meter in sync on every drag tick, without rebuilding the screen. */
    private fun updateHeadroomLive() {
        val mb = maxBoost()
        val color = if (mb >= 9f) Palette.DANGER else if (mb >= 5f) Palette.WARN else Palette.OK
        headroomBar?.apply { fraction = (mb / 12f).coerceIn(0f, 1f); barColor = color }
        headroomValueTv?.apply {
            text = "%.1f dB".format(AudioEngine.headroom(st))
            setTextColor(color)
        }
    }

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
        root.addView(spacer(8))
        root.addView(buildHeadroomMeter(t))
        root.addView(spacer(8))

        if (tab == "easy" || tab == "eq") {
            root.addView(buildGraphPanel(t), LinearLayout.LayoutParams(-1, graphPanelHeight()))
            root.addView(spacer(10))
        }

        when (tab) {
            "easy" -> {
                root.addView(buildPresetRow(t))
                root.addView(spacer(10))
                root.addView(buildEasyTab(t))
            }
            "eq" -> root.addView(buildEqTab(t))
            "sub" -> root.addView(buildSubTab(t), LinearLayout.LayoutParams(-1, -1))
            "setup" -> root.addView(buildSetupTab(t), LinearLayout.LayoutParams(-1, -1))
        }
    }

    /**
     * Height for the curve panel, measured from the screen rather than fixed in
     * dp: the rows under it are wrap-content, so the graph takes what is left
     * over, with a floor so it can never measure to nothing.
     */
    private fun graphPanelHeight(): Int {
        val screenH = resources.displayMetrics.heightPixels
        val below = if (tab == "eq") dp(90) else dp(230)   // eq row, or presets + cards
        val chrome = dp(108)                                // header + headroom meter + spacers
        return (screenH - below - chrome).coerceAtLeast(dp(140))
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
            background = flatPillGradient(t.pillBgTop, t.pillBgBottom, dp(22))
            setPadding(dp(5), dp(5), dp(5), dp(5))
        }
        listOf(
            Triple("easy", "CUSTOM", IconKind.TAB_EQ),
            Triple("eq", "EQUALIZER", IconKind.TAB_BAND),
            Triple("sub", "SUBWOOFER", IconKind.TAB_SUB),
            Triple("setup", "SETUP", IconKind.TAB_CFG)
        ).forEach { (id, lbl, icon) ->
            pill.addView(tabChip(lbl, icon, tab == id, t) { tab = id; render() },
                LinearLayout.LayoutParams(0, dp(44), 1f).apply { setMargins(dp(3), 0, dp(3), 0) })
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
        row.addView(live, LinearLayout.LayoutParams(-2, -2).apply { rightMargin = dp(10) })


        row.addView(roundIconButton(t, if (st.theme == "light") IconKind.MOON else IconKind.SUN, t.knobText) {
            st.theme = if (st.theme == "light") "dark" else "light"; st.save(this@MainActivity); render()
        }, LinearLayout.LayoutParams(dp(40), dp(40)).apply { rightMargin = dp(8) })

        val powerOn = st.enabled
        row.addView(powerButton(t, powerOn) { st.enabled = !st.enabled; commit(); render() },
            LinearLayout.LayoutParams(dp(40), dp(40)))

        return row
    }

    /**
     * Headroom as a slim meter under the header: it changes with every band the
     * user moves, and it explains why a heavily boosted curve plays quieter.
     */
    private fun buildHeadroomMeter(t: ThemeColors): View {
        val mb = maxBoost()
        val color = if (mb >= 9f) Palette.DANGER else if (mb >= 5f) Palette.WARN else Palette.OK
        val row = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            setOnClickListener { showHeadroomInfo() }
        }
        row.addView(label("HEADROOM", 10f, t.mutedText, false),
            LinearLayout.LayoutParams(dp(68), -2))
        val bar = BarView(this).apply {
            fraction = (mb / 12f).coerceIn(0f, 1f)
            barColor = color
            trackColor = t.track
        }
        headroomBar = bar
        row.addView(bar, LinearLayout.LayoutParams(0, dp(6), 1f).apply { rightMargin = dp(12) })
        val valueTv = label("%.1f dB".format(AudioEngine.headroom(st)), 12f, color, true)
        headroomValueTv = valueTv
        row.addView(valueTv, LinearLayout.LayoutParams(dp(56), -2).apply { rightMargin = dp(12) })
        row.addView(label(
            if (mb >= 9f) "Very high boost - keep the volume down"
            else if (mb >= 5f) "Fine, but watch out at high volume"
            else "Safe - no risk of clipping",
            11f, t.mutedText, false))
        return row
    }

    private fun showHeadroomInfo() {
        val mb = maxBoost()
        val msg = buildString {
            append("Headroom: %.1f dB\n\n".format(AudioEngine.headroom(st)))
            append("Boosting a band makes the signal bigger. Past the digital ceiling the ")
            append("waveform gets clipped, which sounds like crackle and is hard on tweeters.\n\n")
            append("So the app turns the input down first, by half of your biggest boost. ")
            append("That is why a heavily boosted curve plays quieter — raise the head unit ")
            append("volume to make it up.\n\n")
            append("Biggest boost right now: %.1f dB\n".format(mb))
            append(if (mb >= 9f) "Very high — keep the volume down or it may clip."
                else if (mb >= 5f) "Fine, but watch out at high volume."
                else "Safe — no risk of clipping.")
            append("\n\nTip: to shape the sound without losing any headroom, cut the bands ")
            append("you want less of instead of boosting the ones you want more of.")
        }
        AlertDialog.Builder(this).setTitle("Headroom").setMessage(msg)
            .setPositiveButton("OK", null).show()
    }

    private fun roundIconButton(t: ThemeColors, kind: IconKind, color: Int, onClick: () -> Unit) =
        BandIconView(this, kind).apply {
            iconColor = color
            scale = 0.5f
            background = flatCircleGradient(t.knobBgTop, t.knobBgBottom)
            elevation = dpf(3f)
            isClickable = true
            setOnClickListener { onClick() }
        }

    private fun powerButton(t: ThemeColors, on: Boolean, onClick: () -> Unit) =
        BandIconView(this, IconKind.POWER).apply {
            iconColor = if (on) Palette.ACCENT else t.knobText
            scale = 0.5f
            background = if (on) circleGradient(t.knobBgTop, t.knobBgBottom, Palette.ACCENT)
                else flatCircleGradient(t.knobBgTop, t.knobBgBottom)
            elevation = dpf(3f)
            isClickable = true
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
                st.dirty = true
                AudioEngine.apply(st)
                curveView?.let { sv -> sv.bands = st.bands; sv.selected = i }
                eqValueTv?.text = fmtDb(rv)
                eqFreqTv?.let { tv -> tv.text = FREQ_LABELS[i] + " Hz"; tv.setTextColor(SpectrumColors.at(i / 14f)) }
                updateHeadroomLive()
            }
            onBandDragEnd = { st.save(this@MainActivity); render() }
        }
        curveView = curve
        panel.addView(curve, LinearLayout.LayoutParams(-1, 0, 1f))
        return panel
    }

    // ---------- preset row (CUSTOM tab) ----------
    /**
     * A slot shows the user's own saved version when there is one, otherwise the
     * factory curve. Editing marks the current slot dirty; nothing is written to
     * that slot until SAVE. RESET discards the saved version and goes back to
     * the factory curve for the slot currently selected.
     */
    private fun buildPresetRow(t: ThemeColors): View {
        val row = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        row.addView(label("PRESETS", 11f, t.mutedText, false),
            LinearLayout.LayoutParams(dp(64), -2))

        AudioSettings.PRESETS.forEachIndexed { i, (name, _) ->
            val active = st.preset == i
            val edited = st.savedSlots[i] != null
            val unsaved = active && st.dirty
            val chip = TextView(this).apply {
                text = name; textSize = 13f; setTypeface(null, Typeface.BOLD); gravity = Gravity.CENTER
                setTextColor(if (active) Color.WHITE else t.knobText)
                background = if (active) pillGradient(Palette.ACCENT, Palette.ACCENT_DEEP, Palette.ACCENT, dp(12))
                    else flatPillGradient(t.knobBgTop, t.knobBgBottom, dp(12))
                elevation = dpf(3f)
                setOnClickListener {
                    val (bands, sub) = AudioSettings.slotCurve(st, i)
                    st.bands = bands.copyOf(); st.subLevel = sub; st.preset = i; st.dirty = false
                    commit(); render()
                }
            }
            val wrap = android.widget.FrameLayout(this)
            wrap.addView(chip, android.widget.FrameLayout.LayoutParams(-1, -1))
            if (unsaved || edited) {
                wrap.addView(View(this).apply {
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(if (unsaved) Palette.WARN else if (active) Color.WHITE else Palette.ACCENT)
                    }
                }, android.widget.FrameLayout.LayoutParams(dp(7), dp(7)).apply {
                    gravity = Gravity.TOP or Gravity.END; topMargin = dp(9); rightMargin = dp(10)
                })
            }
            row.addView(wrap, LinearLayout.LayoutParams(0, dp(46), 1f).apply { setMargins(dp(4), 0, dp(4), 0) })
        }

        row.addView(View(this).apply { setBackgroundColor(t.panelBorder) },
            LinearLayout.LayoutParams(dp(1), dp(28)).apply { leftMargin = dp(6); rightMargin = dp(8) })

        row.addView(TextView(this).apply {
            text = if (st.dirty) "SAVE •" else "SAVE"
            textSize = 11f; setTypeface(null, Typeface.BOLD); gravity = Gravity.CENTER
            setTextColor(if (st.dirty) Color.parseColor("#0E1A12") else t.knobText)
            background = if (st.dirty) pillGradient(Color.parseColor("#86C99A"), Color.parseColor("#4E8C62"), Color.parseColor("#86C99A"), dp(12))
                else flatPillGradient(t.knobBgTop, t.knobBgBottom, dp(12)).apply { alpha = 140 }
            elevation = dpf(3f)
            setOnClickListener {
                if (!st.dirty) return@setOnClickListener
                st.savedSlots[st.preset] = SavedSlot(st.bands.copyOf(), st.subLevel)
                st.dirty = false
                commit(); render()
            }
        }, LinearLayout.LayoutParams(dp(78), dp(46)).apply { rightMargin = dp(6) })

        row.addView(pillChip("RESET", false, Palette.ACCENT, Palette.ACCENT_DEEP, t) {
            st.savedSlots[st.preset] = null
            val (bands, sub) = AudioSettings.PRESETS[st.preset].second
            st.bands = bands.copyOf(); st.subLevel = sub; st.dirty = false
            commit(); render()
        }, LinearLayout.LayoutParams(dp(78), dp(46)))

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
            st.dirty = true
            AudioEngine.apply(st)
            valueTv.text = fmtDb(avg(w))
            curveView?.let { it.bands = st.bands }
            updateHeadroomLive()
        }
        fun shiftStep(w: FloatArray, delta: Float) {
            for (i in 0 until 15) if (w[i] > 0f) st.bands[i] = (st.bands[i] + delta * w[i]).coerceIn(-12f, 12f)
            st.dirty = true
            commit(); render()
        }

        val bassValue = TextView(this)
        row.addView(knobCard(t, "Bass", "Deep & Powerful", IconKind.BASS, avg(W_BASS), bassValue,
            minus = { shiftStep(W_BASS, -1f) }, plus = { shiftStep(W_BASS, 1f) },
            onSlide = { v -> shiftLive(W_BASS, v - avg(W_BASS), bassValue) },
            onSlideEnd = { st.save(this); render() }), LinearLayout.LayoutParams(0, -2, 1f).apply { rightMargin = dp(8) })

        val midValue = TextView(this)
        row.addView(knobCard(t, "Mid", "Clear & Balanced", IconKind.MID, avg(W_MID), midValue,
            minus = { shiftStep(W_MID, -1f) }, plus = { shiftStep(W_MID, 1f) },
            onSlide = { v -> shiftLive(W_MID, v - avg(W_MID), midValue) },
            onSlideEnd = { st.save(this); render() }), LinearLayout.LayoutParams(0, -2, 1f).apply { leftMargin = dp(4); rightMargin = dp(4) })

        val trebleValue = TextView(this)
        row.addView(knobCard(t, "Treble", "Crisp & Detailed", IconKind.TREBLE, avg(W_TREBLE), trebleValue,
            minus = { shiftStep(W_TREBLE, -1f) }, plus = { shiftStep(W_TREBLE, 1f) },
            onSlide = { v -> shiftLive(W_TREBLE, v - avg(W_TREBLE), trebleValue) },
            onSlideEnd = { st.save(this); render() }), LinearLayout.LayoutParams(0, -2, 1f).apply { leftMargin = dp(8) })
        return row
    }

    private fun knobCard(t: ThemeColors, title: String, hint: String, icon: IconKind, value: Float, valueTv: TextView,
                         minus: () -> Unit, plus: () -> Unit, onSlide: (Float) -> Unit, onSlideEnd: () -> Unit): View {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = panelBg(t)
            elevation = dpf(4f)
            setPadding(dp(18), dp(16), dp(18), dp(16))
        }

        val head = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        head.addView(BandIconView(this, icon).apply {
            iconColor = t.knobText
            background = flatCircleGradient(t.knobBgTop, t.knobBgBottom)
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
        sliderRow.addView(iconKnob(t, IconKind.MINUS) { minus() }, LinearLayout.LayoutParams(dp(48), dp(48)))
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
        sliderRow.addView(seek, LinearLayout.LayoutParams(0, dp(44), 1f).apply { leftMargin = dp(10); rightMargin = dp(10) })
        sliderRow.addView(iconKnob(t, IconKind.PLUS) { plus() }, LinearLayout.LayoutParams(dp(48), dp(48)))
        card.addView(sliderRow)
        return card
    }

    /**
     * Round buttons draw their own icon. Head-unit firmware often ships a font
     * with no glyph for characters like U+23FB, which showed up as a blank.
     */
    private fun iconKnob(t: ThemeColors, kind: IconKind, onClick: () -> Unit) =
        BandIconView(this, kind).apply {
            iconColor = t.knobText
            scale = 0.46f
            background = flatCircleGradient(t.knobBgTop, t.knobBgBottom)
            elevation = dpf(3f)
            isClickable = true
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

        row.addView(iconKnob(t, IconKind.CHEVRON_LEFT) { selBand = (selBand - 1).coerceAtLeast(0); render() },
            LinearLayout.LayoutParams(dp(48), dp(48)))
        val mid = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL; minimumWidth = dp(110)
        }
        mid.addView(label("BAND", 10f, t.mutedText, false))
        val freqTv = label(FREQ_LABELS[selBand] + " Hz", 20f, bandColor, true)
        eqFreqTv = freqTv
        mid.addView(freqTv)
        row.addView(mid, LinearLayout.LayoutParams(-2, -2).apply { leftMargin = dp(8); rightMargin = dp(8) })
        row.addView(iconKnob(t, IconKind.CHEVRON_RIGHT) { selBand = (selBand + 1).coerceAtMost(14); render() },
            LinearLayout.LayoutParams(dp(48), dp(48)))

        row.addView(View(this).apply { setBackgroundColor(t.panelBorder) },
            LinearLayout.LayoutParams(dp(1), dp(36)).apply { leftMargin = dp(12); rightMargin = dp(12) })

        row.addView(iconKnob(t, IconKind.MINUS) {
            st.bands[selBand] = (st.bands[selBand] - 0.5f).coerceIn(-12f, 12f); st.dirty = true; commit(); render()
        }, LinearLayout.LayoutParams(dp(48), dp(48)))
        val valueTv = label(fmtDb(st.bands[selBand]), 24f, t.text, true).apply { gravity = Gravity.CENTER }
        eqValueTv = valueTv
        row.addView(valueTv, LinearLayout.LayoutParams(dp(110), -2))
        row.addView(iconKnob(t, IconKind.PLUS) {
            st.bands[selBand] = (st.bands[selBand] + 0.5f).coerceIn(-12f, 12f); st.dirty = true; commit(); render()
        }, LinearLayout.LayoutParams(dp(48), dp(48)))

        row.addView(View(this), LinearLayout.LayoutParams(0, -2, 1f))
        row.addView(pillChip("FLAT", false, Palette.ACCENT, Palette.ACCENT_DEEP, t) {
            for (i in 0 until 15) st.bands[i] = 0f
            st.dirty = true
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
        legend.addView(swatch(Palette.SUB), LinearLayout.LayoutParams(dp(10), dp(10)).apply { rightMargin = dp(5) })
        legend.addView(label("Subwoofer", 11f, t.mutedText, false))
        legend.addView(swatch(Palette.ACCENT), LinearLayout.LayoutParams(dp(10), dp(10)).apply { leftMargin = dp(12); rightMargin = dp(5) })
        legend.addView(label("Speakers", 11f, t.mutedText, false))
        head.addView(legend)
        splitPanel.addView(head)
        splitPanel.addView(spacer(4))
        val split = SubSplitView(this).apply {
            subLevel = st.subLevel; crossover = st.subFreq; accentColor = Palette.ACCENT; theme = t
        }
        split.minimumHeight = dp(140)
        splitPanel.addView(split, LinearLayout.LayoutParams(-1, 0, 1f))
        col.addView(splitPanel, LinearLayout.LayoutParams(-1, 0, 1f))
        col.addView(spacer(10))

        val bottomRow = LinearLayout(this)

        val subCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = panelBg(t)
            elevation = dpf(4f)
            setPadding(dp(18), dp(16), dp(18), dp(16))
        }
        val subHead = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        subHead.addView(label("SUB LEVEL", 15f, Palette.SUB, true), LinearLayout.LayoutParams(0, -2, 1f))
        val subValueTv = label(fmtDb(st.subLevel), 22f, t.text, true)
        subHead.addView(subValueTv)
        subCard.addView(subHead)
        subCard.addView(spacer(10))

        fun liveSub(v: Float) {
            st.subLevel = v.coerceIn(-12f, 12f)
            st.dirty = true
            AudioEngine.apply(st)
            subValueTv.text = fmtDb(st.subLevel)
            split.subLevel = st.subLevel
            updateHeadroomLive()
        }
        fun stepSub(delta: Float) { st.subLevel = (st.subLevel + delta).coerceIn(-12f, 12f); st.dirty = true; commit(); render() }

        val subSlideRow = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        subSlideRow.addView(iconKnob(t, IconKind.MINUS) { stepSub(-1f) }, LinearLayout.LayoutParams(dp(48), dp(48)))
        val subSeek = SeekBar(this).apply {
            max = 48; progress = ((st.subLevel + 12f) * 2f).toInt().coerceIn(0, 48)
            styleSlimSeekBar(this, t.track, Palette.SUB)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar, p: Int, fromUser: Boolean) {
                    if (fromUser) liveSub(p / 2f - 12f)
                }
                override fun onStartTrackingTouch(sb: SeekBar) {}
                override fun onStopTrackingTouch(sb: SeekBar) { st.save(this@MainActivity); render() }
            })
        }
        subSlideRow.addView(subSeek, LinearLayout.LayoutParams(0, dp(44), 1f).apply { leftMargin = dp(10); rightMargin = dp(10) })
        subSlideRow.addView(iconKnob(t, IconKind.PLUS) { stepSub(1f) }, LinearLayout.LayoutParams(dp(48), dp(48)))
        subCard.addView(subSlideRow)
        bottomRow.addView(subCard, LinearLayout.LayoutParams(0, -1, 1f).apply { rightMargin = dp(8) })

        val xoCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = panelBg(t)
            elevation = dpf(4f)
            setPadding(dp(18), dp(16), dp(18), dp(16))
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

        // --- engine status: the one card that says whether any of this reaches the speakers ---
        val ok = AudioEngine.status.startsWith("ENGINE OK")
        val statusColor = if (!st.enabled) Palette.WARN else if (ok) Palette.OK else Palette.DANGER
        // A left accent strip carries the status colour; the card itself stays flat
        // (no outline), matching the rest of the app once borders were removed.
        val statusCard = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            background = panelBg(t)
            elevation = dpf(4f)
            setPadding(dp(18), dp(14), dp(20), dp(14))
            setOnClickListener { showDiagnostics() }
        }
        statusCard.addView(View(this).apply {
            background = GradientDrawable().apply { cornerRadius = dpf(2f); setColor(statusColor) }
        }, LinearLayout.LayoutParams(dp(4), -1).apply { rightMargin = dp(14) })
        statusCard.addView(View(this).apply {
            background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(statusColor) }
        }, LinearLayout.LayoutParams(dp(12), dp(12)).apply { rightMargin = dp(14) })
        val statusTexts = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        statusTexts.addView(label(
            if (!st.enabled) "Bypassed" else if (ok) "Engine OK" else "Engine problem",
            16f, statusColor, true))
        statusTexts.addView(label(AudioEngine.status, 11f, t.mutedText, false))
        statusCard.addView(statusTexts, LinearLayout.LayoutParams(0, -2, 1f))
        statusCard.addView(label("Tap for diagnostics", 11f, t.mutedText, false))
        col.addView(statusCard)
        col.addView(spacer(12))

        // --- ambience on its own, full-width row ---
        val row1 = LinearLayout(this)

        val ambCard = card(t)
        val ambHead = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        ambHead.addView(label("Ambience", 16f, t.text, true), LinearLayout.LayoutParams(0, -2, 1f))
        ambHead.addView(label(AudioEngine.AMBIENCE_NOTES[st.ambience] ?: "", 11f, t.mutedText, false))
        ambCard.addView(ambHead)
        ambCard.addView(spacer(10))
        AudioEngine.AMBIENCE_IDS.chunked(3).forEach { chunk ->
            val row = LinearLayout(this)
            chunk.forEach { id ->
                row.addView(pillChip(AudioEngine.AMBIENCE_LABELS[id] ?: id, st.ambience == id,
                    Color.parseColor("#AF9BDB"), Color.parseColor("#6E5CA8"), t) {
                    st.ambience = id; commit(); render()
                }, LinearLayout.LayoutParams(0, dp(44), 1f).apply { setMargins(dp(3), 0, dp(3), 0) })
            }
            ambCard.addView(row)
            ambCard.addView(spacer(8))
        }
        ambCard.addView(spacer(8))
        ambCard.addView(label(
            "A car cabin already has its own reflections, so these often blur vocals and bass. Worth a listen, but Off suits most music.",
            11f, t.mutedText, false).apply { setLineSpacing(dpf(3f), 1f) })
        row1.addView(ambCard, LinearLayout.LayoutParams(-1, -1))
        col.addView(row1, LinearLayout.LayoutParams(-1, 0, 1f))
        col.addView(spacer(12))

        // --- loudness / balance / high-pass, back on their own row below ambience ---
        val row2 = LinearLayout(this)

        val loudCard = card(t)
        val loudRow = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        val loudTexts = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        loudTexts.addView(label("Loudness", 14f, t.text, true))
        loudTexts.addView(label("Lift at low volume", 11f, t.mutedText, false))
        loudRow.addView(loudTexts, LinearLayout.LayoutParams(0, -2, 1f))
        loudRow.addView(toggleSwitch(t, st.loudness) {
            st.loudness = !st.loudness; commit(); render()
        }, LinearLayout.LayoutParams(dp(56), dp(30)))
        loudCard.addView(loudRow)
        row2.addView(loudCard, LinearLayout.LayoutParams(0, -1, 0.85f).apply { rightMargin = dp(7) })

        val balCard = card(t)
        val balHead = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        balHead.addView(label("Balance", 14f, t.text, true), LinearLayout.LayoutParams(0, -2, 1f))
        val balWord = if (st.balance == 0) "Both sides equal"
            else if (st.balance < 0) "Louder on the left" else "Louder on the right"
        val balHeadWord = label(balWord, 11f, t.mutedText, false)
        balHead.addView(balHeadWord)
        balCard.addView(balHead)
        balCard.addView(spacer(8))

        val balRow = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        // Both tags are created before the track, so the drag handler below
        // can reach both update functions.
        val (leftTagView, updateLeftTag) = sideTag(t, "L", "Driver", st.balance < 0)
        val (rightTagView, updateRightTag) = sideTag(t, "R", "Passenger", st.balance > 0)
        val balTrack = BalanceTrackView(this).apply {
            value = st.balance; trackColor = t.track; markColor = t.panelBorder
            onValueChangeEnd = { st.save(this@MainActivity); render() }
        }
        val centerBtn = pillChip("CENTER", false, Palette.ACCENT, Palette.ACCENT_DEEP, t) {
            st.balance = 0; commit(); render()
        }.apply { alpha = if (st.balance == 0) 0.45f else 1f }
        balTrack.onValueChange = { v ->
            st.balance = v
            AudioEngine.apply(st)
            balHeadWord.text = if (v == 0) "Both sides equal"
                else if (v < 0) "Louder on the left" else "Louder on the right"
            updateLeftTag(v < 0)
            updateRightTag(v > 0)
            centerBtn.alpha = if (v == 0) 0.45f else 1f
        }
        balRow.addView(leftTagView, LinearLayout.LayoutParams(dp(50), -2).apply { rightMargin = dp(8) })
        balRow.addView(balTrack, LinearLayout.LayoutParams(0, dp(40), 1f))
        balRow.addView(rightTagView, LinearLayout.LayoutParams(dp(50), -2).apply { leftMargin = dp(8) })
        balRow.addView(centerBtn, LinearLayout.LayoutParams(dp(74), dp(36)).apply { leftMargin = dp(8) })
        balCard.addView(balRow)
        row2.addView(balCard, LinearLayout.LayoutParams(0, -1, 1.3f).apply { leftMargin = dp(7); rightMargin = dp(7) })

        val hpCard = card(t)
        val hpHead = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        hpHead.addView(label("Speaker High-Pass", 14f, t.text, true), LinearLayout.LayoutParams(0, -2, 1f))
        hpHead.addView(label("keeps bass off the doors", 11f, t.mutedText, false))
        hpCard.addView(hpHead)
        hpCard.addView(spacer(8))
        val hpGrid = LinearLayout(this)
        listOf(0, 60, 80, 100).forEach { f ->
            hpGrid.addView(pillChip(if (f == 0) "Off" else "$f Hz", st.highPass == f, Palette.ACCENT, Palette.ACCENT_DEEP, t) {
                st.highPass = f; commit(); render()
            }, LinearLayout.LayoutParams(0, dp(42), 1f).apply { setMargins(dp(3), 0, dp(3), 0) })
        }
        hpCard.addView(hpGrid)
        row2.addView(hpCard, LinearLayout.LayoutParams(0, -1, 1.1f).apply { leftMargin = dp(7) })

        col.addView(row2, LinearLayout.LayoutParams(-1, -2))
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
        setPadding(dp(20), dp(18), dp(20), dp(18))
        gravity = Gravity.CENTER_VERTICAL
    }

    /**
     * Returns the L/R tag view plus a function to flip its highlight on the fly —
     * used so the Balance track can light up the right side while dragging,
     * not only after the finger lifts.
     */
    private fun sideTag(t: ThemeColors, letter: String, who: String, active: Boolean): Pair<View, (Boolean) -> Unit> {
        val letterTv = label(letter, 19f, if (active) Color.WHITE else t.mutedText, true).apply { gravity = Gravity.CENTER }
        val whoTv = label(who, 10f, if (active) Color.WHITE else t.mutedText, false).apply { gravity = Gravity.CENTER }
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, dp(8))
            background = if (active) pillGradient(Palette.ACCENT, Palette.ACCENT_DEEP, Palette.ACCENT, dp(12))
                else GradientDrawable().apply { cornerRadius = dpf(12f); setColor(Color.TRANSPARENT) }
        }
        box.addView(letterTv)
        box.addView(whoTv)
        val update: (Boolean) -> Unit = { on ->
            box.background = if (on) pillGradient(Palette.ACCENT, Palette.ACCENT_DEEP, Palette.ACCENT, dp(12))
                else GradientDrawable().apply { cornerRadius = dpf(12f); setColor(Color.TRANSPARENT) }
            val col = if (on) Color.WHITE else t.mutedText
            letterTv.setTextColor(col); whoTv.setTextColor(col)
        }
        return box to update
    }

    private fun toggleSwitch(t: ThemeColors, on: Boolean, onClick: () -> Unit): View {
        val wrap = LinearLayout(this).apply {
            gravity = if (on) Gravity.END or Gravity.CENTER_VERTICAL else Gravity.START or Gravity.CENTER_VERTICAL
            setPadding(dp(3), dp(3), dp(3), dp(3))
            background = if (on) pillGradient(Palette.ACCENT, Palette.ACCENT_DEEP, Palette.ACCENT, dp(16))
                else flatPillGradient(t.knobBgTop, t.knobBgBottom, dp(16))
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
            else flatPillGradient(t.knobBgTop, t.knobBgBottom, dp(12))
        elevation = dpf(3f)
        setOnClickListener { onClick() }
    }

    /** A tab chip: icon + label, highlighted with a solid fill and a soft glow when active. */
    private fun tabChip(text: String, icon: IconKind, active: Boolean, t: ThemeColors, onClick: () -> Unit): View {
        val chip = LinearLayout(this).apply {
            gravity = Gravity.CENTER
            background = if (active) flatPillGradient(t.tabOnTop, t.tabOnBottom, dp(20))
                else GradientDrawable().apply { cornerRadius = dpf(20f); setColor(Color.TRANSPARENT) }
            elevation = if (active) dpf(4f) else 0f
            setOnClickListener { onClick() }
        }
        chip.addView(BandIconView(this, icon).apply {
            iconColor = if (active) Palette.ACCENT else t.mutedText
            scale = 0.68f
        }, LinearLayout.LayoutParams(dp(18), dp(18)).apply { rightMargin = dp(7) })
        val lbl = text
        chip.addView(TextView(this).apply {
            this.text = lbl
            textSize = 12f; setTypeface(null, Typeface.BOLD)
            setTextColor(if (active) t.tabOnText else t.mutedText)
        })
        return chip
    }

    private fun swatch(color: Int) = View(this).apply {
        background = GradientDrawable().apply { cornerRadius = dpf(2f); setColor(color) }
    }

    private fun panelBg(t: ThemeColors) = flatPillGradient(t.panelBgTop, t.panelBgBottom, dp(18))

    /** Same gradient fill as pillGradient, but with no stroke — for neutral surfaces. */
    private fun flatPillGradient(top: Int, bottom: Int, radius: Int) = GradientDrawable(
        GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(top, bottom)
    ).apply { cornerRadius = radius.toFloat() }

    private fun flatCircleGradient(top: Int, bottom: Int) = GradientDrawable(
        GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(top, bottom)
    ).apply { shape = GradientDrawable.OVAL }

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
            setColor(Color.parseColor("#F4F6F9"))
            setStroke(dp(2), Color.parseColor("#8C939F"))
            setSize(dp(24), dp(24))
        }
        seek.minimumHeight = totalH
        seek.minimumWidth = dp(80)
        seek.thumbOffset = 0
    }
}
