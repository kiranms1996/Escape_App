# Escape Security

Native Kotlin / Jetpack Compose / Material 3 personal app-locker prototype. Designed for physical testing on a Vivo X300 Pro running Android 16 / OriginOS. Android 11 (API 30) minimum; compile and target API 36. No root or private APIs.

**Security boundary:** this app detects window package changes after they happen. It cannot suspend another app's launch, guarantee zero content exposure, or prevent bypass through force-stop, uninstall, disabled permissions, safe mode, missing events, notifications, widgets, previews, or multi-window. Do not use it as the only protection for sensitive data.

## What is included

- Three-step onboarding with limitations, authenticator checks, credential warning, and package-detection consent.
- Searchable launchable-app list with accessible switches; default home, Android Settings, and Escape itself excluded from selection. Escape settings are always gated after onboarding.
- Explicitly enabled accessibility detection; only `TYPE_WINDOW_STATE_CHANGED` and `packageName` are read. No content retrieval, gestures, global actions, overlay, usage access, camera, internet, device-admin, foreground service, or boot receiver.
- Automatic AndroidX BiometricPrompt accepting Class 3 biometrics, optionally phone PIN/pattern/password (enabled by default and disclosed). The platform chooses modality. Class 2-only face is excluded; screen-unlock face does not prove third-party face support.
- In-memory unlock for the current selected package; leaving it, screen-off, connection loss, or process death revokes the grant. Errors, first biometric mismatch, cancellation, and 90-second timeout return home. Duplicate rejected-app events do not open repeated prompts.
- Status dashboard, dark/light system theme, large text-friendly scrolling controls, and local settings with backup disabled.

## Windows laptop setup (Android Studio)

1. Install Android Studio from [the official Android site](https://developer.android.com/studio). Use a version supporting AGP 8.13.2 or newer; retain the project's pinned versions on first import.
2. In Android Studio's SDK Manager, install **Android SDK Platform 36**, **Android SDK Build-Tools 35.0.0**, **Android SDK Platform-Tools**, and **Android SDK Command-line Tools (latest)**. Accept the displayed SDK licenses.
3. Open this directory (the one containing `settings.gradle.kts`) as a project. Let Gradle sync download dependencies. Under Settings → Build, Execution, Deployment → Build Tools → Gradle, select a JDK 17 installation for Gradle JDK. If absent, use the JDK selector's Download JDK option, select version 17 and a vendor such as Eclipse Temurin, and note its installation directory. A newer compatible Studio JDK may work, but JDK 17 is the documented reproducible choice. No NDK is needed.
4. Android Studio normally creates `local.properties` with the SDK path. Do not commit it. If using PowerShell, set the environment as below. The standard SDK location is an example: use the actual path from SDK Manager.

```powershell
Set-Location 'C:\Users\kiran\Documents\Projects\Escape_App'
$env:JAVA_HOME = 'C:\path\to\jdk-17'
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
$env:Path = "$env:JAVA_HOME\bin;$env:ANDROID_HOME\platform-tools;$env:Path"
java -version
.\gradlew.bat --version
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug --console=plain
```

The wrapper, including its JAR, is included. Versions: Gradle 8.13, AGP 8.13.2, Kotlin + Compose compiler 2.2.21, Compose BOM 2025.10.01. First build needs internet. The app itself has no internet permission.

On this development laptop, the successful build used portable tools now present in the ignored `.tools` folder. To rebuild using those tools without editing paths, run `powershell -ExecutionPolicy Bypass -File .\scripts\Build.ps1`. That script also works with `JAVA_HOME` and `ANDROID_HOME` on another laptop. The source ZIP excludes the downloaded JDK/SDK/caches; Android Studio can install equivalent tools there.

Optional command-line SDK installation after installing the official command-line tools:

```powershell
& "$env:ANDROID_HOME\cmdline-tools\latest\bin\sdkmanager.bat" --licenses
& "$env:ANDROID_HOME\cmdline-tools\latest\bin\sdkmanager.bat" 'platforms;android-36' 'build-tools;35.0.0' 'platform-tools'
```

macOS/Linux: set `JAVA_HOME` to JDK 17, set `ANDROID_HOME`, run `chmod +x gradlew`, then `./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug`.

## Build and install the debug APK

Enable developer options and USB debugging using the instructions shown by your Vivo firmware or [vivo Support](https://www.vivo.com/in/support); menu names vary. Connect a data-capable USB cable, unlock the phone, and approve its computer authorization prompt. Install the Vivo/Android USB driver if Windows does not enumerate it. Use current SDK platform-tools rather than an older `adb` already on PATH.

```powershell
& "$env:ANDROID_HOME\platform-tools\adb.exe" devices -l
.\gradlew.bat :app:assembleDebug
& "$env:ANDROID_HOME\platform-tools\adb.exe" install -r '.\app\build\outputs\apk\debug\app-debug.apk'
& "$env:ANDROID_HOME\platform-tools\adb.exe" shell am start -n com.escape.security/.MainActivity
```

If multiple devices are connected, insert `-s YOUR_SERIAL` immediately after `adb.exe`. The APK is debug-signed for testing. An update requires the same signing key; a signature mismatch requires uninstalling the old build, which loses settings and protection.

Complete onboarding, then authenticate into Escape. Select a harmless test app. In **Status → Review and enable detection**, read the disclosure and open Android Accessibility settings; explicitly enable **Escape Security · app detection**. Return to Escape and reauthenticate. Confirm consent, enabled toggle, connected service, and ready authentication. Exit to Home and open the selected app. Verification should begin automatically if this firmware permits the activity launch. There is no extra verify button.

Android may restrict accessibility for a sideloaded build. Read the system warning and consult [Android's restricted-settings guidance](https://support.google.com/android/answer/12623953); do not assume every firmware exposes the same control. The app cannot grant this access itself.

## Verification and device limits

Read [actual validation results](docs/VALIDATION.md), [physical-device checklist](docs/DEVICE_TESTING.md), [Android 16 research and design](docs/ANDROID_16_RESEARCH.md), and [OriginOS troubleshooting](docs/ORIGINOS.md).

Android 16 emulator testing is now complete for the recorded scenarios: see [live results](docs/LIVE_RESULTS.md) and [emulator setup/repeatable tests](docs/EMULATOR_TESTING.md). The local emulator is `Escape_API_36`, serial `emulator-5556`, with artificial PIN **2468** and simulated fingerprint ID **1**. Run `powershell -ExecutionPolicy Bypass -File .\scripts\Start-Emulator.ps1` to reopen it after closing it. The `live-tests` module is a separate emulator-only test harness, not part of Escape Security's APK.

Unit tests cover grant scope, leave/re-entry, cancellation suppression, stale callbacks, screen-off/permission reset, process-recreation state, and authenticator masks. They do **not** prove device interception, biometric sensor behavior, credential lifecycle, or OriginOS background survival. Those need the physical checklist. Build output and test/lint reports are under `app/build/`.

If all allowed authenticators become unavailable, Escape's settings stay locked. Restore enrollment/security from Android Settings, wait out system biometric lockout, or use device credentials if enabled. Clearing Escape data or uninstalling is a destructive recovery that removes protection. Android Settings and Home stay unprotected to avoid locking you out of the device.

No selected apps, authentication results, or window events are logged by app code. Debug Android diagnostics can contain OS task/package information; review before sharing logs. Only selections, onboarding, consent, and fallback preference persist. The prototype does not encrypt target apps or modify their tasks.

## Project layout

`app/src/main/java/com/escape/security/`: Compose UI, local settings, authenticator wrapper, foreground service, verification activity, and pure Kotlin lock engine. `app/src/main/res/`: service configuration, backup rules, app icon and theme. `app/src/test/`: JVM unit tests. `docs/`: research, setup details, threats, and physical verification.

`live-tests/`: separate emulator-only application/instrumentation harness, including an accessibility-preserving screen inspector and four live tests. It is not a dependency of the production app and is not included in Escape's APK.

## Distribution

Personal sideloading is the intended scope. Before proposing Play Store publication, review the current AccessibilityService API policy, complete required declaration/review and prominent disclosure/consent, provide a privacy policy and accurate Data safety answers, verify package-visibility use and target-API requirements, and create a proper release-signing/update plan. This app is **not** an accessibility tool and declares `isAccessibilityTool=false`; acceptance is not guaranteed. See the policy links in the research document.
