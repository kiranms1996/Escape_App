# Android 16 emulator: setup and live testing

This is an isolated Android test device, not a Vivo/OriginOS image. Emulator fingerprint events are synthetic sensor events, not a real identity check. Live results are recorded separately in `LIVE_RESULTS.md` after execution.

## Install and start (PowerShell)

The development laptop keeps the official Android SDK under `.tools/android-sdk` and JDK under `.tools/jdk`. These large downloads and AVD data are ignored and excluded from the source ZIP. To install equivalent packages with an existing SDK:

```powershell
& "$env:ANDROID_HOME\cmdline-tools\latest\bin\sdkmanager.bat" 'emulator' 'system-images;android-36;google_apis;x86_64'
& "$env:ANDROID_HOME\emulator\emulator.exe" -accel-check
powershell -ExecutionPolicy Bypass -File .\scripts\Start-Emulator.ps1
```

The script creates `Escape_API_36` (Pixel 5 profile, API 36 Google APIs x86_64), with local AVD data in `.tools/avd`, on ADB serial `emulator-5556`. It requests 2 GB of guest RAM; this emulator raised that to its 2560 MB minimum. It uses two cores, software graphics, no host cameras/audio, and no snapshots. Android user data persists. The tested laptop supports WHPX; no Windows security changes were made. Stop a project Gradle daemon with `gradlew.bat --stop` if RAM is tight. Android Studio can reuse `.tools/android-sdk` as its SDK location to avoid downloading another copy.

```powershell
$adbPath = '.\.tools\android-sdk\platform-tools\adb.exe'
& $adbPath -s emulator-5556 wait-for-device
& $adbPath -s emulator-5556 shell getprop sys.boot_completed
& $adbPath -s emulator-5556 install -r '.\app\build\outputs\apk\debug\app-debug.apk'
& $adbPath -s emulator-5556 shell am start -n com.escape.security/.MainActivity
```

`sys.boot_completed` must return `1`; `wait-for-device` alone does not mean Android finished booting. To close only this emulator, close its window or run `adb -s emulator-5556 emu kill`.

## Test scope

Use only artificial credentials on this new virtual device. Do not enter any personal Google account, phone PIN, face images, or other personal data. Follow the normal Android UI for app consent and accessibility enablement. The configured test PIN is **2468**, and simulated fingerprint ID **1** is enrolled. These are public test fixtures, not personal credentials.

The host `scripts/AndroidUi.ps1` helper inspects only emulator test UI and rejects physical-device serials. While Escape's accessibility service is enabled, it uses `UiDumpRunner` in the separate `live-tests` harness with `FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES`. Standalone `uiautomator dump` suppresses ordinary accessibility services and invalidates protection tests. Never instrument Escape's own process for these live flows: test startup can force-stop its bound service and mark it crashed. The helper/test code is not in the shipped Escape APK.

Manual live checks: onboarding, runtime authenticator reporting, app search/selection, explicitly enabled accessibility, automatic protected-app prompts, cancellation, biometric success/failure, credential fallback, re-entry relock, screen-off, settings authentication, consent withdrawal, process recreation, and dark mode. Record failures and fixes; do not infer these outcomes from a successful build.

Fingerprint testing uses the official emulator console after enrolling a test fingerprint in Android Settings:

```powershell
& $adbPath -s emulator-5556 emu finger touch 1
& $adbPath -s emulator-5556 emu finger remove
```

Use a different unenrolled ID to test rejection. See [Android's fingerprint simulation guidance](https://developer.android.com/studio/run/emulator-console#fingerprint-simulation).

## Repeatable instrumentation checks

After onboarding, explicitly select Clock, grant detection consent, enable Escape's service, and allow credential fallback. Configure only the artificial PIN on this emulator. Then build/install the separate harness and run the suite:

```powershell
.\gradlew.bat :app:assembleDebug :live-tests:assembleDebug :live-tests:assembleDebugAndroidTest
& $adbPath -s emulator-5556 install -r '.\app\build\outputs\apk\debug\app-debug.apk'
& $adbPath -s emulator-5556 install -r '.\live-tests\build\outputs\apk\debug\live-tests-debug.apk'
& $adbPath -s emulator-5556 install -r '.\live-tests\build\outputs\apk\androidTest\debug\live-tests-debug-androidTest.apk'
# Recheck/re-enable the service through Android Accessibility after replacing the Escape APK.
& $adbPath -s emulator-5556 shell am instrument -w -r -e class com.escape.security.EmulatorUiTest,com.escape.security.ProtectedAppFlowTest -e liveFlow true -e testPin 2468 com.escape.security.harness.test/com.escape.security.UiDumpRunner
```

Four tests check settings authentication, the non-exported challenge activity, the absence of content/gesture/screenshot capabilities, and a full automatic-prompt → cancel → PIN success → observed Home → relock → settings flow. The full flow enters the supplied artificial PIN only when explicitly opted in with `liveFlow=true`; it skips otherwise. All tests restrict themselves to emulator builds. The fixture expects the Google APIs Clock package and completed onboarding. A skipped test is not a passed flow. Manual fingerprint and screen-off checks are recorded separately.

To inspect a live screen without stopping protection after installing the harness:

```powershell
. .\scripts\AndroidUi.ps1
Show-AndroidUi
```

After replacing the Escape APK, Android may keep its service enabled but mark the old binding crashed. The live session encountered this; toggle Escape off/on manually through Android Accessibility, then verify **enabled + connected**. An APK installation result alone does not establish active protection.

The real Vivo still requires the physical-device checklist. This emulator cannot establish OriginOS background survival, a physical face/fingerprint sensor's app eligibility, or zero content exposure.
