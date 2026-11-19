package com.reactivate.caraudiopro

import android.content.Context

/**
 * Everything the app remembers: survives app kill and car reboot.
 * "Meetha" starting curve: warm bass (100-160Hz), clear vocals, less 2.5k-4k harshness, smoother 10k-16k.
 */
class AudioSettings(
    var enabled: Boolean = true,
    var bands: FloatArray = floatArrayOf(1.5f, 2f, 2f, 2f, 1.5f, 0.5f, 0f, 0f, 0.5f, 1f, -2f, -2.5f, -1f, -1.5f, -1f),
    var subLevel: Float = 3f,
    var subFreq: Int = 80,
    var theme: String = "dark",
    var autoHeadroom: Boolean = true,
) {
    fun save(ctx: Context) {
        sp(ctx).edit()
            .putBoolean("enabled", enabled)
            .putString("bands", bands.joinToString(","))
            .putFloat("subLevel", subLevel)
            .putInt("subFreq", subFreq)
            .putString("theme", theme)
            .putBoolean("autoHeadroom", autoHeadroom)
            .apply()
    }

    companion object {
        private fun sp(ctx: Context) = ctx.getSharedPreferences("car_audio_pro", Context.MODE_PRIVATE)
        private val DEFAULT_BANDS = floatArrayOf(1.5f, 2f, 2f, 2f, 1.5f, 0.5f, 0f, 0f, 0.5f, 1f, -2f, -2.5f, -1f, -1.5f, -1f)

        private fun dec(s: String?): FloatArray? =
            s?.split(",")?.mapNotNull { it.toFloatOrNull() }?.takeIf { it.size == 15 }?.toFloatArray()

        fun load(ctx: Context): AudioSettings {
            val p = sp(ctx)
            return AudioSettings(
                enabled = p.getBoolean("enabled", true),
                bands = dec(p.getString("bands", null)) ?: DEFAULT_BANDS.copyOf(),
                subLevel = p.getFloat("subLevel", 3f),
                subFreq = p.getInt("subFreq", 80),
                theme = p.getString("theme", "dark") ?: "dark",
                autoHeadroom = p.getBoolean("autoHeadroom", true),
            )
        }
    }
}
