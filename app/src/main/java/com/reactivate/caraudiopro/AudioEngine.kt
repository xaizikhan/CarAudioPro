package com.reactivate.caraudiopro

import android.content.Context
import android.media.AudioManager
import android.media.audiofx.AudioEffect
import android.media.audiofx.DynamicsProcessing
import android.os.Build
import android.util.Log
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Asal audio engine (DynamicsProcessing):
 *   Input gain (auto headroom) -> Pre-EQ: 15-band graphic EQ
 *   -> Post-EQ: SUB LEVEL (crossover se neeche ki frequencies) -> Limiter (clipping se bachao)
 * Session 0 = poore Android ka output (YouTube, local music, sab apps).
 */
object AudioEngine {
    const val TAG = "CarAudioPro"
    private const val PRIORITY = 1000
    private const val BANDS = 15

    val CENTERS = floatArrayOf(25f, 40f, 63f, 100f, 160f, 250f, 400f, 630f,
        1000f, 1600f, 2500f, 4000f, 6300f, 10000f, 16000f)
    // DynamicsProcessing bands "upper edge" se define hote hain: do centers ke beech geometric midpoint
    private val EDGES = FloatArray(BANDS) { i ->
        if (i == BANDS - 1) 20000f else sqrt(CENTERS[i] * CENTERS[i + 1])
    }

    private val effects = LinkedHashMap<Int, DynamicsProcessing>()
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

    /** Sirf tab per-app session lagate hain jab global kaam na kar raha ho (double processing se bachao). */
    @Synchronized
    fun onSessionOpen(session: Int, pkg: String) {
        if (globalOk()) { Log.i(TAG, "global OK, skip session $session ($pkg)"); return }
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
        val pre = DynamicsProcessing.Eq(true, true, BANDS)
        for (i in 0 until BANDS) pre.getBand(i).apply {
            isEnabled = true; cutoffFrequency = EDGES[i]; gain = s.bands[i]
        }
        val post = DynamicsProcessing.Eq(true, true, 2)
        post.getBand(0).apply { isEnabled = true; cutoffFrequency = s.subFreq.toFloat(); gain = s.subLevel }
        post.getBand(1).apply { isEnabled = true; cutoffFrequency = 20000f; gain = 0f }
        // attack 1ms, release 60ms, ratio 10:1, threshold -1 dB, post gain 0
        val lim = DynamicsProcessing.Limiter(true, true, 0, 1f, 60f, 10f, -1f, 0f)
        val inGain = headroom(s)

        effects.forEach { (sess, dp) ->
            try {
                dp.setInputGainAllChannelsTo(inGain)
                dp.setPreEqAllChannelsTo(pre)
                dp.setPostEqAllChannelsTo(post)
                dp.setLimiterAllChannelsTo(lim)
                dp.enabled = s.enabled
            } catch (e: Throwable) {
                err("apply session $sess: ${e.javaClass.simpleName}: ${e.message}")
            }
        }
        updateStatus()
    }

    /** Boost jitna zyada, input utna kam: limiter ke saath milkar clipping rokta hai. */
    fun headroom(s: AudioSettings): Float {
        if (!s.autoHeadroom) return 0f
        var mx = 0f
        for (i in 0 until BANDS) {
            val g = s.bands[i] + if (CENTERS[i] <= s.subFreq) s.subLevel else 0f
            mx = max(mx, g)
        }
        return -(mx * 0.5f).coerceIn(0f, 9f)
    }

    @Synchronized
    fun release() {
        effects.values.forEach { runCatching { it.release() } }
        effects.clear(); owners.clear()
        status = "Engine stopped"
    }

    private fun attach(session: Int, owner: String) {
        if (effects.containsKey(session)) return
        try {
            val cfg = DynamicsProcessing.Config.Builder(
                DynamicsProcessing.VARIANT_FAVOR_FREQUENCY_RESOLUTION,
                2,              // stereo
                true, BANDS,    // pre-EQ (graphic EQ)
                false, 1,       // multiband compressor (off)
                true, 2,        // post-EQ (sub level)
                true            // limiter
            ).setPreferredFrameDuration(10f).build()
            val dp = DynamicsProcessing(PRIORITY, session, cfg)
            dp.setControlStatusListener { _, has ->
                Log.i(TAG, "session $session control=$has")
                if (has) current?.let { apply(it) } else updateStatus()
            }
            effects[session] = dp
            owners[session] = owner
            Log.i(TAG, "attached session $session ($owner)")
        } catch (e: Throwable) {
            err("attach session $session: ${e.javaClass.simpleName}: ${e.message}")
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
            g == null -> "ENGINE ERROR: global effect nahi bana (INFO dabaein)"
            !ctrl(g) -> "WARNING: control kisi aur app ke paas hai (stock DSP?)"
            else -> "ENGINE OK · All apps · " + if (on) "ACTIVE" else "BYPASS"
        } + if (extra > 0) " · +$extra app session" else ""
        Log.d(TAG, status)
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
        append("\nOutput devices:\n")
        ctx.getSystemService(AudioManager::class.java)
            .getDevices(AudioManager.GET_DEVICES_OUTPUTS).forEach {
                append(" • type=${it.type} '${it.productName}' ch=${it.channelCounts.joinToString()}\n")
            }
        append("\nAudio effects on device:\n")
        runCatching { AudioEffect.queryEffects()?.forEach { append(" • ${it.name} (${it.implementor})\n") } }
    }
}
