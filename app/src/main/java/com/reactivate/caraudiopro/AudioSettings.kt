package com.reactivate.caraudiopro

import android.content.Context

/** Saari settings. App band ho, kill ho ya car restart ho, yahi se wapas load hoti hain. */
class AudioSettings(
    var enabled: Boolean = true,
    var profile: Profile = Profile.CLEAR,
    var bands: FloatArray = Profile.CLEAR.bands.copyOf(),
    var subLevel: Float = Profile.CLEAR.sub,
    var subFreq: Int = 80,
    var customBands: FloatArray = FloatArray(15),
    var customSub: Float = 0f,
    var autoHeadroom: Boolean = true,
) {
    fun save(ctx: Context) {
        sp(ctx).edit()
            .putBoolean("enabled", enabled)
            .putString("profile", profile.name)
            .putString("bands", bands.joinToString(","))
            .putFloat("subLevel", subLevel)
            .putInt("subFreq", subFreq)
            .putString("customBands", customBands.joinToString(","))
            .putFloat("customSub", customSub)
            .putBoolean("autoHeadroom", autoHeadroom)
            .apply()
    }

    companion object {
        private fun sp(ctx: Context) = ctx.getSharedPreferences("car_audio_pro", Context.MODE_PRIVATE)

        private fun dec(s: String?, def: FloatArray): FloatArray =
            s?.split(",")?.mapNotNull { it.toFloatOrNull() }?.takeIf { it.size == 15 }?.toFloatArray()
                ?: def.copyOf()

        fun load(ctx: Context): AudioSettings {
            val p = sp(ctx)
            val prof = runCatching { Profile.valueOf(p.getString("profile", "CLEAR")!!) }.getOrDefault(Profile.CLEAR)
            return AudioSettings(
                enabled = p.getBoolean("enabled", true),
                profile = prof,
                bands = dec(p.getString("bands", null), Profile.CLEAR.bands),
                subLevel = p.getFloat("subLevel", Profile.CLEAR.sub),
                subFreq = p.getInt("subFreq", 80),
                customBands = dec(p.getString("customBands", null), FloatArray(15)),
                customSub = p.getFloat("customSub", 0f),
                autoHeadroom = p.getBoolean("autoHeadroom", true),
            )
        }
    }
}
