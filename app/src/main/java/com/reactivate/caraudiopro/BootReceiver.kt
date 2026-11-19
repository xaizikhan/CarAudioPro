package com.reactivate.caraudiopro

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Car start hone par last settings khud lag jati hain. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) = CarAudioService.start(c)
}
