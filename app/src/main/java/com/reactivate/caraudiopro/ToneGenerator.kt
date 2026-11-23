package com.reactivate.caraudiopro

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import kotlin.concurrent.thread
import kotlin.math.PI
import kotlin.math.sin

/**
 * Plays a channel-identification tone on one side only, so the user can confirm
 * which speaker is which and that nothing is wired backwards.
 *
 * Deliberately limited: a soft 440 Hz sine at a fixed, low amplitude, stopping
 * on its own after a few seconds. No pink noise and no bass sweep — those can
 * cook a tweeter or bottom out a sub, and they are only ever needed once.
 */
object ToneGenerator {
    private const val RATE = 44100
    private const val FREQ = 440.0
    private const val AMP = 0.18f          // well under full scale on purpose
    private const val SECONDS = 6
    private const val FADE = 0.04f         // fade in/out, so there is no click

    @Volatile private var track: AudioTrack? = null
    @Volatile var playing: String? = null
        private set

    /** side is "left" or "right". Calling again switches sides. */
    @Synchronized
    fun play(side: String, onFinished: () -> Unit) {
        stop()
        playing = side
        val frames = RATE * SECONDS
        val buf = ShortArray(frames * 2)
        val fadeFrames = (RATE * FADE).toInt()
        for (i in 0 until frames) {
            var a = AMP
            if (i < fadeFrames) a *= i / fadeFrames.toFloat()
            if (i > frames - fadeFrames) a *= (frames - i) / fadeFrames.toFloat()
            val v = (sin(2.0 * PI * FREQ * i / RATE) * a * Short.MAX_VALUE).toInt().toShort()
            buf[i * 2] = if (side == "left") v else 0
            buf[i * 2 + 1] = if (side == "right") v else 0
        }

        val t = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                    .build()
            )
            .setBufferSizeInBytes(buf.size * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        t.write(buf, 0, buf.size)
        track = t
        t.play()

        // stop by itself; the user should not have to remember to
        thread(isDaemon = true) {
            Thread.sleep(SECONDS * 1000L)
            synchronized(ToneGenerator) {
                if (track === t) { stop(); onFinished() }
            }
        }
    }

    @Synchronized
    fun stop() {
        track?.let { runCatching { it.stop() }; runCatching { it.release() } }
        track = null
        playing = null
    }
}
