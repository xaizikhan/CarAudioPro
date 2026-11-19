# Car Audio Pro – Final Build

This is the real Android app (Kotlin), matching the design we finalised:
Easy / Equalizer / Subwoofer tabs, a colourful spectrum "mountain" EQ curve,
dark/light theme toggle, no shadows, and a live Custom curve (no presets —
whatever you set is what plays, and it's exactly what saves and survives
an app kill or car restart).

## Install
1. Android Studio mein `CarAudioPro` folder kholein, Gradle sync hone dein.
2. Build > Build APK(s), phir head unit par install karein (USB/ADB ya pen drive).
   Ya GitHub Actions se APK download karein (neeche dekhein).
3. Pehli dafa app kholne par Notification permission ALLOW karein (Android 13).

## GitHub se APK banane ka tareeqa (SDK ke baghair)
1. Is poore folder ko ek naye GitHub repo mein upload karein (`.github/workflows/build-apk.yml` samet).
2. Repo ke **Actions** tab mein jayein, "Build APK" workflow chalayein.
3. 3-5 minute baad us run ke **Artifacts** mein `CarAudioPro-apk` milega — isay download karke tablet/head unit par install karein.

## Test se pehle zaroori
- SANSUI ka stock DSP/EQ FLAT kar dein, aur stock loudness/bass boost OFF.
- Volume mid-level par rakhein.
- INFO button dabakar status dekhein. `ENGINE OK · All apps · ACTIVE` aana chahiye.

## Tests
| # | Test | Kaise | PASS |
|---|------|-------|------|
| 1 | YouTube | Koi gaana chalayein, Bass/Treble knob chalayein | Wazeh farq, bina app khole bhi awaaz par asar rahe |
| 2 | Local music | Stock music player mein yahi test | Wazeh farq |
| 3 | Har band | EQUALIZER tab mein kisi dot par tap karein, +/- se adjust karein | Sirf usi frequency ka asar |
| 4 | Subwoofer | SUBWOOFER tab mein Sub Level aur crossover badlein | Sub ka bass badle, split graph bhi update ho |
| 5 | Theme | Header ke sun/moon button se light/dark switch karein | Poori app ka look badal jaye |
| 6 | Save | App kill karein, dobara kholein | Wahi bands/sub/theme wapas aayein |
| 7 | Restart | Car band/start karein, app na kholein | Sound par asar wapas khud aa jaye |

## Amplifier (JL Audio XD400/4) – ek dafa set karein
- CH3/CH4 (sub): amp ka low-pass ON, app ke crossover ke qareeb (jaise 80 Hz).
- CH1/CH2 (speakers): high-pass ON, isi frequency ke qareeb.
- Bridged sub load minimum 4 ohm.

## Logs
`adb logcat -s CarAudioPro`
