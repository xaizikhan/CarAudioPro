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
import android.widget.TextView

class MainActivity : Activity() {
    private val accent = 0xFF00B4FF.toInt()
    private val subColor = 0xFFFF6A00.toInt()

    private lateinit var st: AudioSettings
    private val eqSliders = mutableListOf<EqSlider>()
    private lateinit var subSlider: EqSlider
    private lateinit var statusTv: TextView
    private lateinit var powerBtn: TextView
    private val profileBtns = mutableMapOf<Profile, TextView>()
    private val xoverBtns = mutableMapOf<Int, TextView>()
    private val handler = Handler(Looper.getMainLooper())
    private val ticker = object : Runnable {
        override fun run() { statusTv.text = AudioEngine.status; handler.postDelayed(this, 1000) }
    }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        st = AudioSettings.load(this)
        CarAudioService.start(this)
        setContentView(buildUi())
        refreshAll()
        handler.post(ticker)
    }

    override fun onDestroy() { handler.removeCallbacks(ticker); super.onDestroy() }

    /** Har change: save + foran asal audio engine par apply. */
    private fun commit() { st.save(this); AudioEngine.apply(st) }

    private fun buildUi(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.BLACK)
            setPadding(dp(14), dp(8), dp(14), dp(10))
        }

        // ---- Top bar ----
        val top = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        top.addView(label("CAR AUDIO PRO", 20f, Color.WHITE, true).apply { letterSpacing = 0.12f })
        top.addView(label("  DSP", 20f, accent, true))
        statusTv = label("", 12f, 0xFF9A9A9A.toInt(), false).apply {
            gravity = Gravity.END; setPadding(dp(12), 0, dp(12), 0); maxLines = 2
        }
        top.addView(statusTv, LinearLayout.LayoutParams(0, -2, 1f))
        top.addView(chip("INFO") { showDiagnostics() }, LinearLayout.LayoutParams(dp(80), dp(42)).apply { rightMargin = dp(8) })
        powerBtn = chip("ON") { st.enabled = !st.enabled; commit(); refreshAll() }
        top.addView(powerBtn, LinearLayout.LayoutParams(dp(100), dp(42)))
        root.addView(top)

        // ---- EQ + Subwoofer ----
        val mid = LinearLayout(this)
        val eqPanel = LinearLayout(this).apply { background = panelBg(); setPadding(dp(6), dp(8), dp(6), dp(4)) }
        AudioEngine.CENTERS.forEachIndexed { i, hz ->
            val s = EqSlider(this, fmtHz(hz), -12f, 12f, accent)
            s.onChange = { v -> st.bands[i] = v; toCustom(); commit() }
            eqPanel.addView(s, LinearLayout.LayoutParams(0, -1, 1f))
            eqSliders += s
        }
        mid.addView(eqPanel, LinearLayout.LayoutParams(0, -1, 1f))

        val subPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL
            background = panelBg(); setPadding(dp(8), dp(8), dp(8), dp(8))
        }
        subPanel.addView(label("SUBWOOFER", 14f, subColor, true))
        subSlider = EqSlider(this, "SUB LEVEL", -12f, 12f, subColor)
        subSlider.onChange = { v -> st.subLevel = v; toCustom(); commit() }
        subPanel.addView(subSlider, LinearLayout.LayoutParams(-1, 0, 1f))
        subPanel.addView(label("CROSSOVER", 11f, 0xFF9A9A9A.toInt(), false))
        listOf(listOf(60, 80), listOf(100, 120)).forEach { pair ->
            val row = LinearLayout(this)
            pair.forEach { f ->
                val c = chip("$f Hz") { st.subFreq = f; commit(); refreshAll() }
                xoverBtns[f] = c
                row.addView(c, LinearLayout.LayoutParams(0, dp(40), 1f).apply { setMargins(dp(3), dp(3), dp(3), dp(3)) })
            }
            subPanel.addView(row, LinearLayout.LayoutParams(-1, -2))
        }
        mid.addView(subPanel, LinearLayout.LayoutParams(dp(180), -1).apply { leftMargin = dp(10) })
        root.addView(mid, LinearLayout.LayoutParams(-1, 0, 1f).apply { topMargin = dp(8); bottomMargin = dp(8) })

        // ---- Profiles ----
        val prow = LinearLayout(this)
        Profile.values().forEach { p ->
            val c = chip(p.title) { selectProfile(p) }
            profileBtns[p] = c
            prow.addView(c, LinearLayout.LayoutParams(0, dp(52), 1f).apply { setMargins(dp(4), 0, dp(4), 0) })
        }
        root.addView(prow)
        return root
    }

    private fun toCustom() {
        st.profile = Profile.CUSTOM
        st.customBands = st.bands.copyOf()
        st.customSub = st.subLevel
        highlightProfiles()
    }

    private fun selectProfile(p: Profile) {
        st.profile = p
        if (p == Profile.CUSTOM) { st.bands = st.customBands.copyOf(); st.subLevel = st.customSub }
        else { st.bands = p.bands.copyOf(); st.subLevel = p.sub }
        commit(); refreshAll()
    }

    private fun refreshAll() {
        eqSliders.forEachIndexed { i, s -> s.value = st.bands[i] }
        subSlider.value = st.subLevel
        powerBtn.text = if (st.enabled) "ON" else "BYPASS"
        setChip(powerBtn, st.enabled, accent)
        xoverBtns.forEach { (f, c) -> setChip(c, f == st.subFreq, subColor) }
        highlightProfiles()
    }

    private fun highlightProfiles() = profileBtns.forEach { (p, c) -> setChip(c, p == st.profile, accent) }

    private fun showDiagnostics() {
        AlertDialog.Builder(this).setTitle("Diagnostics")
            .setMessage(AudioEngine.diagnostics(this)).setPositiveButton("OK", null).show()
    }

    // ---- UI helpers ----
    private fun chip(text: String, onClick: () -> Unit) = TextView(this).apply {
        this.text = text; gravity = Gravity.CENTER; textSize = 13f
        setTypeface(Typeface.DEFAULT_BOLD); setOnClickListener { onClick() }
        setChip(this, false, accent)
    }

    private fun setChip(v: TextView, active: Boolean, color: Int) {
        v.background = GradientDrawable().apply {
            cornerRadius = dp(10).toFloat()
            setColor(if (active) color else 0xFF161616.toInt())
            setStroke(dp(1), if (active) color else 0xFF333333.toInt())
        }
        v.setTextColor(if (active) Color.BLACK else Color.WHITE)
    }

    private fun panelBg() = GradientDrawable().apply {
        cornerRadius = dp(14).toFloat(); setColor(0xFF0D0D0D.toInt()); setStroke(dp(1), 0xFF1E1E1E.toInt())
    }

    private fun label(t: String, size: Float, color: Int, bold: Boolean) = TextView(this).apply {
        text = t; textSize = size; setTextColor(color); if (bold) setTypeface(Typeface.DEFAULT_BOLD)
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun fmtHz(hz: Float): String =
        if (hz >= 1000) { val k = hz / 1000f; if (k % 1f == 0f) "${k.toInt()}k" else "${k}k" }
        else hz.toInt().toString()
}
