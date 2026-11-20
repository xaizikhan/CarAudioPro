# Owais Car Audio Pro — v2.0

## Build
Push to GitHub; the Actions workflow `Build APK` produces the debug APK as the
artifact `CarAudioPro-apk`. Or locally: `gradle assembleDebug`.

## First thing to check on the SANSUI unit
Open the app and tap the round **i** button in the header. The Diagnostics
dialog must say:

    Status: ENGINE OK · All apps · ACTIVE

If it says **WARNING: control kisi aur app ke paas hai**, the stock DSP is
holding the effect and nothing in this app will change the sound. Everything
else is pointless until that line reads OK.

The dialog also reports:
  * `Per-channel balance supported: true/false` — if false, the Balance card on
    the SETUP tab does nothing on this device and should be removed.
  * The current loudness / high-pass / balance values.

## What's in this version
* **Greyish (gunmetal) theme**, dark and light, with gradients and soft shadows.
* **Four tabs**: CUSTOM, EQUALIZER, SUBWOOFER, SETUP.
* **Presets** (Daily / Bass / Vocal / Night) on the CUSTOM tab. All four are
  cut-only curves — no band goes above 0 dB — so the engine never has to pull
  the input gain down and nothing clips. Expect them to sound quieter than
  before; raise the head unit volume to compensate.
* **SETUP tab**: Loudness, Balance, Speaker High-Pass, Headroom meter.
* **Weight-based layout** — fits 1024x600 and 1280x800 without clipping.
* Everything auto-saves on change; there is no save button.

## Notes on the new audio features
* **Speaker High-Pass** adds a post-EQ band that cuts below the chosen
  frequency. Use 80 Hz with the sub crossover at 80 Hz.
* **Loudness** is a fixed bass+treble lift, not volume-tracking: the app cannot
  read the head unit's volume knob.
* **Balance** attenuates the quieter side only, so it costs no headroom. It
  needs per-channel input gain, which may not work on every device — check the
  diagnostics line above.

## Still open
* Boot after a full battery disconnect: the app may stay in the "stopped" state
  until opened once by hand. `LOCKED_BOOT_COMPLETED` and `directBootAware` are
  already set. To confirm:
  `adb shell dumpsys package com.reactivate.caraudiopro | grep -i stopped`
