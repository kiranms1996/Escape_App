# Actual validation results

Date: 15 October 2026 (Asia/Calcutta). Build-time results and follow-up emulator testing are distinguished below. These are not Vivo acceptance results.

## Completed in this environment

- Downloaded portable Eclipse Temurin JDK **17.0.20.1+1** and official Android command-line tools into ignored `.tools/` because no JDK/SDK was preinstalled. Installed SDK Platform 36, Build-Tools 35.0.0, and platform-tools. Gradle used 8.13 / AGP 8.13.2 / Kotlin 2.2.21.
- Original baseline command: `gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:lintDebug --console=plain` with JAVA_HOME and ANDROID_HOME set to those tools.
- Original baseline result: **BUILD SUCCESSFUL**, 24 seconds; 55 actionable tasks, 27 executed and 28 up-to-date. The first dependency-download/build pass also succeeded (7m 27s). After live visual review, the system-bar contrast fix was compiled and linted successfully in a follow-up build (2m 5s).
- **18 JVM tests passed:** 16 LockEngine tests and 2 Authentication tests, zero failures/errors.
- **Android lint: 0 errors, 13 warnings.** Warnings: newer Gradle/library versions available (6), convenience KTX replacements (6), and `isAccessibilityTool` only applying on API 31+ (1). Target API is 36; API 30 ignores that explicit false flag. Dependencies are intentionally pinned; warnings were reviewed and not hidden.
- `apksigner verify --verbose`: APK verifies, one signer, APK Signature Scheme v2. This is a debug-signed APK, not a production release.
- `apkanalyzer manifest permissions` on the final APK returned exactly `android.permission.USE_BIOMETRIC` and `com.escape.security.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`. The latter is AndroidX's signature-only receiver guard. No internet, camera, usage access, overlay, notification, storage, root, or administrator permission exists in the APK.
- Inspected merged manifest. Verification activity is not exported; system service requires BIND_ACCESSIBILITY_SERVICE. Legacy USE_FINGERPRINT was explicitly removed because minimum API is 30. Unused preview tooling was removed.
- Compared wrapper JAR SHA-256 with Gradle's published checksum: `81a82aaea5abcc8ff68b3dfcb58b3c3c429378efd98e7433460610fecd7ae45f`. The Gradle distribution download is checksum-pinned.
- `git diff --check` passed. Sources, resources, and Android manifest compiled successfully.

## Debug artifact

- Build output: `app/build/outputs/apk/debug/app-debug.apk`
- Delivery copy: `deliverables/EscapeSecurity-debug.apk`
- Size: **30,193,795 bytes**
- SHA-256: `4963b1dbec6bb8668bd4433447b68e242ffa867871903f63322d97c39450d7fe`
- Source archive: `deliverables/EscapeSecurity-project.zip` (source, wrapper, resources, tests, and docs; excludes machine tools, caches, build directories, signing keys, and local SDK paths).

Local reports after a build: `app/build/reports/tests/testDebugUnitTest/index.html`, `app/build/test-results/testDebugUnitTest/`, and `app/build/reports/lint-results-debug.html`.

## Follow-up live emulator testing

An Android 16/API 36 emulator was subsequently installed and booted. **All 4 final live instrumentation tests passed**, plus manual fingerprint success/rejection, screen-off/wake relock, consent withdrawal, strong-only fallback policy, and dark-mode checks. See [actual live results](LIVE_RESULTS.md), [raw instrumentation output](LIVE_INSTRUMENTATION.txt), and [repeatable setup](EMULATOR_TESTING.md). Tests run from a separate harness process so they do not stop Escape's service. The final APK's debug signature and minimal permissions were rechecked after the contrast fix.

No physical phone was connected. OriginOS background experiments, real sensor eligibility, first-frame exposure, split-screen/PiP, cloned/work apps, long-term idle, and physical-device process/permission changes still require the [physical checklist](DEVICE_TESTING.md). No measured exposure or uptime bound is claimed. Exact OriginOS settings paths for the user's firmware remain unverified, as documented in [OriginOS guidance](ORIGINOS.md). The live report retains early harness failures and the unproven rapid-switch case instead of counting them as passes.
