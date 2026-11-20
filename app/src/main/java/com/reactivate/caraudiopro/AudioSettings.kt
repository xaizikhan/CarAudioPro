package com.reactivate.caraudiopro

import android.content.Context

/**
 * Everything the app remembers: survives app kill and car reboot.
 * Default curve is the "Daily" clear-sound preset: all cut, no boost, so the
 * engine never has to pull input gain down and nothing clips.
 */
class AudioSettings(
    var enabled: Boolean = true,
    var bands: FloatArray = DEFAULT_BANDS.copyOf(),
    var subLevel: Float = 0f,
    var subFreq: Int = 80,
    var theme: String = "dark",
    var autoHeadroom: Boolean = true,
    var preset: Int = 0,
    var loudness: Boolean = false,
    var balance: Int = 0,
    var highPass: Int = 0,
) {
    fun save(ctx: Context) {
        sp(ctx).edit()
            .putBoolean("enabled", enabled)
            .putString("bands", bands.joinToString(","))
            .putFloat("subLevel", subLevel)
            .putInt("subFreq", subFreq)
            .putString("theme", theme)
            .putBoolean("autoHeadroom", autoHeadroom)
            .putInt("preset", preset)
            .putBoolean("loudness", loudness)
            .putInt("balance", balance)
            .putInt("highPass", highPass)
            .apply()
    }

    companion object {
        private fun sp(ctx: Context) = ctx.getSharedPreferences("car_audio_pro", Context.MODE_PRIVATE)

        val DEFAULT_BANDS = floatArrayOf(
            -2f, -2f, -3f, -3.5f, -4f, -3.5f, -2.5f, -2f, -1f, -0.5f, 0f, 0f, -0.5f, -0.5f, -1f
        )

        /** Preset curves. Index matches the PRESETS order in MainActivity. */
        val PRESETS: List<Pair<String, Pair<FloatArray, Float>>> = listOf(
            "Daily" to (DEFAULT_BANDS.copyOf() to 0f),
            "Bass" to (floatArrayOf(0f, 0f, -1f, -2f, -4f, -6f, -7f, -7f, -6f, -6f, -7f, -8f, -7f, -6f, -6f) to 0f),
            "Vocal" to (floatArrayOf(-5f, -4f, -3f, -3f, -2f, -1f, 0f, 0f, -0.5f, -1.5f, -3f, -4f, -3f, -2.5f, -3f) to -3f),
            "Night" to (floatArrayOf(-1f, -1f, -1.5f, -2f, -2.5f, -3f, -3f, -2.5f, -2f, -2f, -3f, -4f, -4f, -4.5f, -5f) to -2f)
        )

        private fun dec(s: String?): FloatArray? =
            s?.split(",")?.mapNotNull { it.toFloatOrNull() }?.takeIf { it.size == 15 }?.toFloatArray()

        fun load(ctx: Context): AudioSettings {
            val p = sp(ctx)
            return AudioSettings(
                enabled = p.getBoolean("enabled", true),
                bands = dec(p.getString("bands", null)) ?: DEFAULT_BANDS.copyOf(),
                subLevel = p.getFloat("subLevel", 0f),
                subFreq = p.getInt("subFreq", 80),
                theme = p.getString("theme", "dark") ?: "dark",
                autoHeadroom = p.getBoolean("autoHeadroom", true),
                preset = p.getInt("preset", 0),
                loudness = p.getBoolean("loudness", false),
                balance = p.getInt("balance", 0),
                highPass = p.getInt("highPass", 0),
            )
        }
    }
}
