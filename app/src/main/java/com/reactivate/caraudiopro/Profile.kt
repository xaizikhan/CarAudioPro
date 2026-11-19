package com.reactivate.caraudiopro

private fun f(vararg v: Number) = FloatArray(v.size) { v[it].toFloat() }

/**
 * Bands: 25, 40, 63, 100, 160, 250, 400, 630, 1k, 1.6k, 2.5k, 4k, 6.3k, 10k, 16k (dB)
 * NOTE: yeh starting points hain. Asal calibration measurement mic (REW) se hogi.
 */
enum class Profile(val title: String, val bands: FloatArray, val sub: Float) {
    CLEAR("CLEAR SOUND", f(2, 2, 1.5, 0.5, 0, -1, -1.5, -0.5, 0, 1, 2, 2.5, 2, 2, 1), 2f),
    POWER_BASS("POWER BASS", f(5, 6, 6, 4, 1.5, -0.5, -1.5, -1, 0, 0.5, 1.5, 2, 2, 1.5, 0.5), 6f),
    VOCAL("VOCAL", f(-1, -1, -0.5, 0, 0, -0.5, 0.5, 1.5, 2.5, 3, 3, 2, 1, 0, -0.5), 0f),
    PIONEER("PIONEER STYLE", f(3, 4, 3.5, 2, 0.5, -1, -1, 0, 0, 0.5, 1.5, 2.5, 3, 3, 2), 3f),
    FLAT("FLAT", FloatArray(15), 0f),
    CUSTOM("CUSTOM", FloatArray(15), 0f);
}
