package com.reactivate.caraudiopro

import android.content.Context
import android.media.AudioManager
import android.media.audiofx.AudioEffect
import android.media.audiofx.DynamicsProcessing
import android.media.audiofx.PresetReverb
import android.os.Build
import android.util.Log
import kotlin.math.max

/**
 * The real audio engine (DynamicsProcessing):
 *   Input gain (auto headroom, per channel for balance)
 *     -> 15-band EQ (+ optional loudness lift)
 *     -> Post-EQ: sub level below crossover, optional speaker high-pass
 *     -> Limiter
 * Attaches to session 0 = Android's whole output (YouTube, local music, every app).
 */
object AudioEngine {
    const val TAG = "CarAudioPro"
    private const val PRIORITY = 1000
    private const val BANDS = 15
    private const val POST_BANDS = 3
    private const val HIGH_PASS_CUT = -12f

    /** Set after the first attempt: false means this device ignores per-channel gain. */
    @Volatile var balanceWorks: Boolean = true

    val CENTERS = floatArrayOf(25f, 40f, 63f, 100f, 160f, 250f, 400f, 630f,
        1000f, 1600f, 2500f, 4000f, 6300f, 10000f, 16000f)

    private val effects = LinkedHashMap<Int, DynamicsProcessing>()
    private val reverbs = LinkedHashMap<Int, PresetReverb>()

    /**
     * Ambience uses Android's PresetReverb, a separate effect from the EQ
     * engine above. DynamicsProcessing has no reverb of its own, and
     * PresetReverb only ships these fixed rooms — no custom decay or mix.
     */
    val AMBIENCE_IDS = listOf("off", "room", "hall", "large", "plate", "echo")
    val AMBIENCE_LABELS = mapOf(
        "off" to "Off", "room" to "Room", "hall" to "Hall",
        "large" to "Concert", "plate" to "Plate", "echo" to "Echo"
    )
    val AMBIENCE_NOTES = mapOf(
        "off" to "Dry, no reverb",
        "room" to "A small, close space",
        "hall" to "Medium hall — gentle",
        "large" to "Large hall — widest",
        "plate" to "Classic studio plate",
        "echo" to "Longest tail"
    )
    private fun reverbPresetFor(id: String): Short = when (id) {
        "room" -> PresetReverb.PRESET_SMALLROOM
        "hall" -> PresetReverb.PRESET_MEDIUMHALL
        "large" -> PresetReverb.PRESET_LARGEHALL
        "plate" -> PresetReverb.PRESET_PLATE
        "echo" -> PresetReverb.PRESET_LARGEHALL   // PresetReverb has no distinct "echo"; this is its longest tail
        else -> PresetReverb.PRESET_NONE
    }
    @Volatile var reverbWorks: Boolean = true
    private val owners = HashMap<Int, String>()
    private val errors = ArrayDeque<String>()
    @Volatile var current: AudioSettings? = null
    @Volatile var status: String = "Engine start ho raha hai…"

    @Synchronized
    fun init(ctx: Context) {
        if (current == null) current = AudioSettings.load(ctx)
        attach(0, "ALL APPS")
        current?.let { apply(it) }
    }

    @Synchronized
    fun onSessionOpen(session: Int, pkg: String) {
        if (globalOk()) return
        attach(session, pkg)
        current?.let { apply(it) }
    }

    @Synchronized
    fun onSessionClose(session: Int) {
        if (session == 0) return
        effects.remove(session)?.let { runCatching { it.release() } }
        owners.remove(session)
        updateStatus()
    }

    @Synchronized
    fun apply(s: AudioSettings) {
        current = s
        val bands = withLoudness(s)
        val pre = DynamicsProcessing.Eq(true, true, BANDS)
        for (i in 0 until BANDS) pre.getBand(i).apply {
            isEnabled = true
            cutoffFrequency = edge(i)
            gain = bands[i]
        }
        // Post-EQ band 0 = sub level (below crossover).
        // Band 1 = speaker high-pass: cuts everything below the chosen frequency.
        // Band 2 = the rest of the spectrum, left flat.
        val post = DynamicsProcessing.Eq(true, true, POST_BANDS)
        if (s.highPass > 0) {
            // Everything under highPass gets cut hard, so the speakers stop
            // trying to reproduce bass the subwoofer already handles.
            post.getBand(0).apply {
                isEnabled = true
                cutoffFrequency = s.highPass.toFloat()
                gain = HIGH_PASS_CUT + s.subLevel
            }
            post.getBand(1).apply {
                isEnabled = true
                cutoffFrequency = maxOf(s.subFreq, s.highPass).toFloat()
                gain = if (s.subFreq > s.highPass) s.subLevel else 0f
            }
        } else {
            post.getBand(0).apply { isEnabled = true; cutoffFrequency = s.subFreq.toFloat(); gain = s.subLevel }
            post.getBand(1).apply { isEnabled = true; cutoffFrequency = s.subFreq.toFloat() + 1f; gain = 0f }
        }
        post.getBand(2).apply { isEnabled = true; cutoffFrequency = 20000f; gain = 0f }

        val lim = DynamicsProcessing.Limiter(true, true, 0, 1f, 60f, 10f, -1f, 0f)
        val inGain = headroom(s)

        effects.forEach { (sess, dp) ->
            try {
                dp.setInputGainAllChannelsTo(inGain)
                dp.setPreEqAllChannelsTo(pre)
                dp.setPostEqAllChannelsTo(post)
                dp.setLimiterAllChannelsTo(lim)
                applyBalance(dp, s, inGain)
                dp.enabled = s.enabled
            } catch (e: Throwable) {
                err("apply session $sess: ${e.javaClass.simpleName}: ${e.message}")
            }
        }

        reverbs.forEach { (sess, pr) ->
            try {
                pr.enabled = s.ambience != "off"
                pr.preset = reverbPresetFor(s.ambience)
            } catch (e: Throwable) {
                reverbWorks = false
                err("reverb session $sess: ${e.javaClass.simpleName}: ${e.message}")
            }
        }
        updateStatus()
    }

    /**
     * Balance: channel 0 = left, channel 1 = right. We only ever attenuate the
     * quieter side, never boost the louder one, so balance costs no headroom.
     * Some devices refuse per-channel input gain; we note it and move on.
     */
    private fun applyBalance(dp: DynamicsProcessing, s: AudioSettings, inGain: Float) {
        if (s.balance == 0) return
        val cut = -1.5f * kotlin.math.abs(s.balance)
        val left = if (s.balance > 0) inGain + cut else inGain
        val right = if (s.balance < 0) inGain + cut else inGain
        try {
            dp.setInputGainbyChannel(0, left)
            dp.setInputGainbyChannel(1, right)
            balanceWorks = true
        } catch (e: Throwable) {
            balanceWorks = false
            err("balance not supported: ${e.javaClass.simpleName}")
        }
    }

    /**
     * Loudness: a gentle smile applied on top of the user's curve, only when
     * they switch it on. The app cannot read the head unit's volume knob, so
     * this is a fixed lift rather than a volume-tracking one.
     */
    fun withLoudness(s: AudioSettings): FloatArray {
        if (!s.loudness) return s.bands
        val lift = floatArrayOf(4f, 4f, 3.5f, 3f, 2f, 1f, 0f, 0f, 0f, 0.5f, 1f, 1.5f, 2f, 2.5f, 3f)
        return FloatArray(BANDS) { (s.bands[it] + lift[it]).coerceIn(-12f, 12f) }
    }

    private fun edge(i: Int): Float =
        if (i == BANDS - 1) 20000f else Math.sqrt((CENTERS[i] * CENTERS[i + 1]).toDouble()).toFloat()

    fun headroom(s: AudioSettings): Float {
        if (!s.autoHeadroom) return 0f
        val bands = withLoudness(s)
        var mx = 0f
        for (i in 0 until BANDS) {
            val g = bands[i] + if (CENTERS[i] <= s.subFreq) s.subLevel else 0f
            mx = max(mx, g)
        }
        return -(mx * 0.5f).coerceIn(0f, 9f)
    }

    @Synchronized
    fun release() {
        effects.values.forEach { runCatching { it.release() } }
        reverbs.values.forEach { runCatching { it.release() } }
        effects.clear(); reverbs.clear(); owners.clear()
        status = "Engine stopped"
    }

    private fun attach(session: Int, owner: String) {
        if (effects.containsKey(session)) return
        try {
            val cfg = DynamicsProcessing.Config.Builder(
                DynamicsProcessing.VARIANT_FAVOR_FREQUENCY_RESOLUTION,
                2, true, BANDS, false, 1, true, 2, true
            ).setPreferredFrameDuration(10f).build()
            val dp = DynamicsProcessing(PRIORITY, session, cfg)
            dp.setControlStatusListener { _, has ->
                if (has) current?.let { apply(it) } else updateStatus()
            }
            effects[session] = dp
            owners[session] = owner
        } catch (e: Throwable) {
            err("attach session $session: ${e.javaClass.simpleName}: ${e.message}")
        }
        try {
            reverbs[session] = PresetReverb(PRIORITY, session).apply { enabled = false }
        } catch (e: Throwable) {
            reverbWorks = false
            err("attach reverb session $session: ${e.javaClass.simpleName}: ${e.message}")
        }
        updateStatus()
    }

    private fun ctrl(dp: DynamicsProcessing) = runCatching { dp.hasControl() }.getOrDefault(false)
    private fun globalOk() = effects[0]?.let { ctrl(it) } ?: false

    private fun err(m: String) {
        Log.e(TAG, m); errors.addLast(m); while (errors.size > 5) errors.removeFirst()
    }

    private fun updateStatus() {
        val g = effects[0]
        val on = current?.enabled != false
        val extra = effects.size - (if (g != null) 1 else 0)
        status = when {
            g == null -> "ENGINE ERROR: global effect nahi bana"
            !ctrl(g) -> "WARNING: control kisi aur app ke paas hai (stock DSP?)"
            else -> "ENGINE OK · All apps · " + if (on) "ACTIVE" else "BYPASS"
        } + if (extra > 0) " · +$extra app session" else ""
    }

    fun diagnostics(ctx: Context): String = buildString {
        append("Device: ${Build.MANUFACTURER} ${Build.MODEL} (${Build.HARDWARE})\n")
        append("Android ${Build.VERSION.RELEASE} / API ${Build.VERSION.SDK_INT}\n\n")
        append("Status: $status\n\nEngine sessions:\n")
        synchronized(AudioEngine) {
            effects.forEach { (s, dp) ->
                val en = runCatching { dp.enabled }.getOrDefault(false)
                append(" • session $s [${owners[s]}] control=${ctrl(dp)} enabled=$en\n")
            }
            errors.forEach { append(" ! $it\n") }
        }
        append("Auto headroom: ${"%.1f".format(current?.let { headroom(it) } ?: 0f)} dB\n")
        append("Per-channel balance supported: $balanceWorks\n")
        append("Ambience (PresetReverb) supported: $reverbWorks\n")
        current?.let { append("Ambience: ${AMBIENCE_LABELS[it.ambience] ?: it.ambience}\n") }
        current?.let { append("Loudness: ${it.loudness} · High-pass: ${if (it.highPass > 0) "${it.highPass} Hz" else "off"} · Balance: ${it.balance}\n") }
        append("\nOutput devices:\n")
        ctx.getSystemService(AudioManager::class.java)
            .getDevices(AudioManager.GET_DEVICES_OUTPUTS).forEach {
                append(" • type=${it.type} '${it.productName}' ch=${it.channelCounts.joinToString()}\n")
            }
        append("\nAudio effects on device:\n")
        runCatching { AudioEffect.queryEffects()?.forEach { append(" • ${it.name} (${it.implementor})\n") } }
    }
}
