package com.reactivate.caraudiopro

import android.content.Context

/** One user-saved version of a preset slot — null means "use the factory curve". */
data class SavedSlot(val bands: FloatArray, val sub: Float)

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
    /** True once the current curve has been edited since the preset was picked or last saved. */
    var dirty: Boolean = false,
    /** One slot per preset; null = that preset still plays its factory curve. */
    var savedSlots: Array<SavedSlot?> = arrayOfNulls(PRESETS.size),
    var loudness: Boolean = false,
    var balance: Int = 0,
    var highPass: Int = 0,
    var ambience: String = "off",
) {
    fun save(ctx: Context) {
        val e = sp(ctx).edit()
            .putBoolean("enabled", enabled)
            .putString("bands", bands.joinToString(","))
            .putFloat("subLevel", subLevel)
            .putInt("subFreq", subFreq)
            .putString("theme", theme)
            .putBoolean("autoHeadroom", autoHeadroom)
            .putInt("preset", preset)
            .putBoolean("dirty", dirty)
            .putBoolean("loudness", loudness)
            .putInt("balance", balance)
            .putInt("highPass", highPass)
            .putString("ambience", ambience)
        savedSlots.forEachIndexed { i, slot ->
            if (slot == null) {
                e.remove("slot${i}_bands").remove("slot${i}_sub")
            } else {
                e.putString("slot${i}_bands", slot.bands.joinToString(","))
                e.putFloat("slot${i}_sub", slot.sub)
            }
        }
        e.apply()
    }

    companion object {
        private fun sp(ctx: Context) = ctx.getSharedPreferences("car_audio_pro", Context.MODE_PRIVATE)

        val DEFAULT_BANDS = floatArrayOf(
            -2f, -2f, -3f, -3.5f, -4f, -3.5f, -2.5f, -2f, -1f, -0.5f, 0f, 0f, -0.5f, -0.5f, -1f
        )

        /** Factory preset curves. Index matches the PRESETS order shown in the UI. */
        val PRESETS: List<Pair<String, Pair<FloatArray, Float>>> = listOf(
            "Daily" to (DEFAULT_BANDS.copyOf() to 0f),
            "Bass" to (floatArrayOf(0f, 0f, -1f, -2f, -4f, -6f, -7f, -7f, -6f, -6f, -7f, -8f, -7f, -6f, -6f) to 0f),
            "Vocal" to (floatArrayOf(-5f, -4f, -3f, -3f, -2f, -1f, 0f, 0f, -0.5f, -1.5f, -3f, -4f, -3f, -2.5f, -3f) to -3f),
            "Night" to (floatArrayOf(-1f, -1f, -1.5f, -2f, -2.5f, -3f, -3f, -2.5f, -2f, -2f, -3f, -4f, -4f, -4.5f, -5f) to -2f)
        )

        /** What a preset slot plays right now: the user's saved version, or the factory curve. */
        fun slotCurve(st: AudioSettings, index: Int): Pair<FloatArray, Float> {
            val saved = st.savedSlots.getOrNull(index)
            return if (saved != null) saved.bands to saved.sub else PRESETS[index].second
        }

        private fun dec(s: String?): FloatArray? =
            s?.split(",")?.mapNotNull { it.toFloatOrNull() }?.takeIf { it.size == 15 }?.toFloatArray()

        fun load(ctx: Context): AudioSettings {
            val p = sp(ctx)
            val slots = Array(PRESETS.size) { i ->
                val b = dec(p.getString("slot${i}_bands", null))
                if (b != null) SavedSlot(b, p.getFloat("slot${i}_sub", 0f)) else null
            }
            return AudioSettings(
                enabled = p.getBoolean("enabled", true),
                bands = dec(p.getString("bands", null)) ?: DEFAULT_BANDS.copyOf(),
                subLevel = p.getFloat("subLevel", 0f),
                subFreq = p.getInt("subFreq", 80),
                theme = p.getString("theme", "dark") ?: "dark",
                autoHeadroom = p.getBoolean("autoHeadroom", true),
                preset = p.getInt("preset", 0),
                dirty = p.getBoolean("dirty", false),
                savedSlots = slots,
                loudness = p.getBoolean("loudness", false),
                balance = p.getInt("balance", 0),
                highPass = p.getInt("highPass", 0),
                ambience = p.getString("ambience", "off") ?: "off",
            )
        }
    }
}
