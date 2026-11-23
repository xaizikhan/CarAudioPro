# Owais Car Audio Pro — v4.5

## Build
Push to GitHub; the Actions workflow `Build APK` produces the debug APK as the
artifact `CarAudioPro-apk`. Or locally: `gradle assembleDebug`.

## First thing to check on the SANSUI unit
Open the SETUP tab. The status card at the top must read **Engine OK**. Tap it
for the full diagnostics dialog, which must say:

    Status: ENGINE OK · All apps · ACTIVE

If it says **WARNING: control kisi aur app ke paas hai**, the stock DSP is
holding the effect and nothing in this app — EQ, sub, ambience, any of it —
will change the sound. Everything else is pointless until that line reads OK.

The dialog also reports:
  * `Per-channel balance supported: true/false` — if false, the Balance card
    does nothing on this device.
  * `Ambience (PresetReverb) supported: true/false` — if false, the Ambience
    card does nothing either; the two use different effect APIs and can fail
    independently.

## App icon
A launcher icon is included (adaptive icon for API 26+, flat PNGs for older
devices): a dark gunmetal background with blue equalizer bars, matching the
in-app theme. Files are under app/src/main/res/mipmap-*.

## v4.5 — reverted the v4.4 SETUP merge
v4.4 put Ambience, Loudness, Balance and High-Pass in one equal-height row.
It looked uneven: Ambience has two rows of pills plus a hint line, the other
three have one short row each, so stretching them to the same height left
Ambience full and the other three mostly empty around a single control.
Back to the v4.3 layout — Ambience full-width on its own, the other three
below in a compact row — which was correct to begin with.

## v4.4 changes
* **SETUP layout merged into one row**: Ambience, Loudness, Balance and
  Speaker High-Pass now sit side by side as four cards, instead of Ambience
  alone on top with an empty strip above the other three. No functionality
  changed, just the empty space closed up.

## v4.3 changes
* **Balance**: dragging the L/R track now highlights the Driver/Passenger
  side and dims/brightens the CENTER button live, the same way the slider
  fixes in v4.2 made headroom update live. Same root cause — a live-drag
  path that didn't touch the views built at render time.
* **Loudness and Speaker High-Pass checked**: both are tap controls (toggle,
  chips), not drags, so every tap already triggers a full `render()`. No
  change needed there; confirmed working as intended.

## v4.2 changes — bug fixes
* **Headroom and SAVE now update from the slider, not just the +/- buttons.**
  The bug: releasing a slider only called `st.save()`, never `render()`, so
  the headroom meter and the preset SAVE indicator stayed frozen until you
  tapped a +/- button (which does call `render()`). Fixed on the Bass/Mid/
  Treble sliders, the Sub Level slider, and dragging the EQUALIZER graph.
* **Headroom now updates live, on every drag tick, from any of those
  controls** — not just on release — via a direct reference to the meter
  instead of a full screen rebuild, so it stays smooth while dragging.
* **Channel Test removed** from the SETUP tab — wasn't useful day to day.
  Ambience now takes the full width where it sat. `ToneGenerator.kt` deleted.

## v4.1 changes
* Tab chips in the header now carry an icon (equalizer bars / waveform /
  speaker box / gear) alongside the label, and the active tab is more clearly
  highlighted.
* Removed the plain grey outline that sat around every card, panel and
  button. Surfaces are now distinguished purely by their gradient fill and
  shadow; the few places that still show a thin coloured line (the active
  preset/crossover/high-pass chip, the power button when on) use that
  control's own accent colour, not a neutral grey — so it reads as a glow
  rather than a box around the element.
* A little more breathing room: wider gaps between header items, between
  cards, and inside card padding.

## What's in this version
* **Greyish (gunmetal) theme**, dark and light, with gradients and soft shadows.
* **Four tabs**: CUSTOM, EQUALIZER, SUBWOOFER, SETUP.
* **Headroom meter** under the header, on every tab, green to amber to red.
* **Presets with per-slot save**: Daily / Bass / Vocal / Night each remember
  your own edited version once you tap SAVE, independently of one another.
  RESET discards your saved version and goes back to the factory curve for
  whichever slot is selected. A dot on a preset chip shows amber for unsaved
  edits, and the accent colour once saved.
* **SETUP tab**: engine status card, channel test (safe, self-stopping),
  ambience (PresetReverb rooms), loudness, balance, speaker high-pass.
* **Weight-based layout** — fits 1024x600 and 1280x800 without clipping.
* All header and button icons are **drawn, not font glyphs** — head-unit fonts
  often have no glyph for characters like U+23FB and left a blank button.
* Everything auto-saves on change; there is no save button except for presets,
  which are save-on-purpose so a quick tweak does not overwrite your tuning.

## Channel test
A soft 440 Hz tone on one side at a fixed low level, stopping by itself after
a few seconds. Use it to confirm nothing is wired backwards.

Pink noise and bass sweeps are deliberately **not** included: continuous high
frequencies cook tweeters, and sub-30 Hz sweeps can bottom out a subwoofer
before anything sounds wrong. They are needed once, if ever; a familiar track
at moderate volume does the same job for setting amp gain.

## Ambience
Android's PresetReverb — Off, Room, Hall, Concert, Plate, Echo (Echo reuses
the largest hall preset; PresetReverb has no true echo/delay). This is a
second, independent effect from the EQ, so it can work even if EQ does not,
or vice versa. A car cabin already has its own reflections, so these often
blur vocals and bass — worth a listen, but Off suits most music.

## Notes on the other new audio features
* **Speaker High-Pass** adds a post-EQ band that cuts below the chosen
  frequency. Use 80 Hz with the sub crossover at 80 Hz.
* **Loudness** is a fixed bass+treble lift, not volume-tracking: the app
  cannot read the head unit's volume knob.
* **Balance** attenuates the quieter side only, so it costs no headroom. It
  needs per-channel input gain, which may not work on every device — check
  the diagnostics line above.

## Still open
* Boot after a full battery disconnect: the app may stay in the "stopped"
  state until opened once by hand. `LOCKED_BOOT_COMPLETED` and
  `directBootAware` are already set. To confirm:
  `adb shell dumpsys package com.reactivate.caraudiopro | grep -i stopped`
