# Car Audio Pro – PoC v2 Test Guide

## Install
1. Android Studio mein `CarAudioPro` folder kholein, Gradle sync hone dein.
2. Build > Build APK(s), phir head unit par install karein (USB/ADB ya pen drive).
3. Pehli dafa app kholne par Notification permission ALLOW karein (Android 13).

## Test se pehle zaroori
- SANSUI ka stock DSP/EQ FLAT kar dein, aur stock loudness/bass boost OFF.
- Head unit ka volume mid-level par rakhein (taake clipping na ho).
- Top par status line dekhein. `ENGINE OK · All apps · ACTIVE` aana chahiye.
  Agar `WARNING` ya `ERROR` aaye to INFO dabakar screenshot bhejein.

## Tests
| # | Test | Kaise | PASS |
|---|------|-------|------|
| 1 | YouTube | Song chalayein, POWER BASS chunein, phir ON/BYPASS dabate rahein | Wazeh farq |
| 2 | Local music | Stock music player mein yahi test | Wazeh farq |
| 3 | Har band | 63 Hz ko +12 aur -12 karein, phir 4k ko | Har band ka alag asar |
| 4 | Subwoofer | SUB LEVEL -12 se +12, crossover 80 Hz | Sub ka bass badle, vocals na badlein |
| 5 | Clipping | POWER BASS + SUB +12, volume barhayein | Bass loud ho, phatay/distort na ho |
| 6 | Save | App ko recents se kill karein, dobara kholein | Wahi settings |
| 7 | Restart | Car band karke start karein, app na kholein | Sound par asar wapas ho |

## Amplifier (JL Audio XD400/4) – ek dafa set karein
- CH3/CH4 (sub): amp ka low-pass ON, app ke crossover ke qareeb (jaise 80 Hz).
- CH1/CH2 (speakers): high-pass ON, isi frequency ke qareeb.
- Gain knobs: distortion ke baghair set karein (zyada gain se EQ ka farq bhi kam lagta hai).
- Exact settings ke liye amp ka manual confirm karein. Bridged sub load minimum 4 ohm.

## Logs
`adb logcat -s CarAudioPro`
