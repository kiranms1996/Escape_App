# Actual Android 16 live results

Executed on 15 October 2026 (Asia/Calcutta). This report supersedes the original statement that only JVM/build testing was available. **No Vivo was connected or tested.**

## Installed environment

- Official Android Emulator **37.2.12**, WHPX accelerator confirmed installed and usable. No Windows security/virtualization setting was changed.
- Official Google APIs Android **16 / API 36**, x86_64 image revision **7**, build `BE2A.250530.026.F3` (`sdk_gphone64_x86_64`). The 1,895,447,397-byte image archive was verified against Google's published SHA-1 `c6bf44bdcd885bb902b4ba752d111a073ad7a817` before installation. Duplicate download/extraction copies were removed afterward; installed SDK/AVD retained.
- AVD **Escape_API_36**, Pixel 5 profile, serial **emulator-5556**, local data `.tools/avd`. Two cores, requested 2048 MB RAM (emulator increased to 2560 MB), software GPU, no host cameras or audio.
- Only artificial PIN **2468** and simulated fingerprint ID **1** were enrolled. Unenrolled ID **99** was used for rejection. No Google account, personal credential, face image, or real biometric was supplied.
- Final installed Escape APK SHA-256: `4963b1dbec6bb8668bd4433447b68e242ffa867871903f63322d97c39450d7fe`.

## Passed live checks

| Check | Observed result |
| --- | --- |
| Fresh onboarding without authenticators | Reports nothing enrolled; Finish setup disabled. |
| Credential readiness after test PIN setup | Device credential becomes Ready; automatic native PIN prompt appears after setup. |
| App picker | Search filters Clock; package switch selects it; selection survives leaving, process restart during early tooling experiments, and APK replacement. |
| Explicit service enablement | App disclosure and Android's confirmation UI used. Dashboard shows enabled and connected when observed without suppressing the service. |
| Automatic selected-app verification | Launching Clock opens Escape verification and the native system prompt without another verify button. |
| Cancellation | Returns Home; no immediate repeated-prompt loop. |
| Native credential success | Correct test PIN reveals the existing Clock task; staying there does not repeatedly prompt. Also passed after fingerprint enrollment using the system's Use PIN option. |
| Relock after leaving | After observing Home, reopening Clock requires a new prompt. |
| Own settings | Reopening Escape after observed Home requires verification before selections appear. |
| Fingerprint rejection | Simulated unenrolled ID 99 returns Home; still Home after a two-second loop check. |
| Fingerprint success | Enrolled synthetic ID 1 resumes Clock. Callback/animation takes longer than a fixed 600 ms check on this emulator; eventual UI/activity inspection confirmed success. |
| Screen off/wake | Power state entered Dozing; after waking/unlocking, protected Clock required fresh app verification. |
| Runtime strong-biometric report | Strong and Class 2-or-stronger become Ready after enrollment; no face modality claim is made. |
| Strong-only policy | Disabling fallback removes the native Use PIN option; default fallback restored afterward. |
| Consent withdrawal | Consent becomes not granted; Clock remains foreground without a gate. Consent restored through the disclosure afterward. |
| Dark mode | Switching Android to night mode recreates the UI successfully; dark dashboard and readable system-bar icons visually inspected. Light-mode system-bar contrast was fixed during this session. |
| Framework security configuration | Installed challenge activity non-exported; service event type is window-state change only; no retrieve-content, gesture, or screenshot capabilities. |

## Repeatable instrumentation result

Final separate-process harness run:

```text
Time: 31.671
OK (4 tests)
```

Tests: `verificationActivityIsNotExported`, `appHasNoContentOrGestureAccessibilityCapability`, `settingsRequireIdentityBeforeExposingSelections`, and `nativeCredentialFlowCancelsUnlocksAndRelocks`. The fourth test covers multiple live assertions, including cancellation, successful PIN authentication with fingerprint enrolled, relock after observed Home, own-settings authentication, and ready status. See [raw final instrumentation output](LIVE_INSTRUMENTATION.txt).

The 18 JVM tests also passed. Final production APK assembly and lint passed after the system-bar fix: **0 lint errors, 13 existing warnings**. One intermediate lint run rejected an Activity cast; the implementation was corrected to `LocalActivity`, with no lint suppression.

## Failed/invalid early attempts and limits

- Standalone `uiautomator dump` suppresses ordinary accessibility services. Its dashboard snapshots showed disconnected/disabled while the OS otherwise had the service enabled. Those snapshots were invalid for protection testing. The final inspector explicitly uses `FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES` in a separate test process.
- Instrumenting Escape's own process force-stopped its service and Android marked the binding crashed. The final harness targets its own empty application process instead. The service was restored through normal Android Settings. APK replacement also required rechecking/re-enabling the binding on this emulator.
- A test that sent Home and immediately relaunched Clock without waiting for Home failed the relock assertion. It did not establish a genuine observed foreground transition. Corrected tests wait for Home to appear and settle, then relaunch. **Rapid transitions, missing events, and zero content exposure remain unproven and potentially bypassable.** No interception guarantee is inferred from the passing cases.
- Fixed-duration 600 ms fingerprint-success observation was too early; subsequent inspection confirmed Clock resumed. Passing checks use UI waits rather than claiming a latency bound.

Not established: first-frame exposure duration, physical face/fingerprint eligibility, OriginOS power/startup survival, prolonged idle/overnight behavior, multi-window/PiP, cloned/work profiles, safe mode, tamper resistance, or protection after force-stop/uninstall/permission loss. Unexpected process death while authenticating was not accepted as a live pass; volatile-state behavior is covered by JVM tests. Continue with the [Vivo checklist](DEVICE_TESTING.md).
