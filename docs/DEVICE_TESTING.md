# Physical Vivo acceptance checklist

**Not executed on a Vivo by the development environment.** Mark actual results on your phone. Record Android/OriginOS firmware, region, test date, and app version. Use an innocuous app first, with fake test content. Any missed interception means you must not assume protection.

| Scenario | Expected result / measurement | Result |
| --- | --- | --- |
| First run | Three onboarding steps disclose limitations, data use, authenticators, and optional detection consent. No service enables itself. | Untested |
| No enrollment | Finish is unavailable until an allowed authenticator is ready; Android enrollment link works. | Untested |
| Consent declined | App works for setup; detection remains inactive, dashboard says attention needed. | Untested |
| Search/select | Label and package search, switches, persistence on reopen; Home/Settings excluded. Test uninstall/reinstall and remove stale selections. | Untested |
| Service enablement | Read disclosure, enable Escape in Accessibility, reauthenticate on return. Toggle and connection are reported separately. | Untested |
| Supported authenticators | Compare strong/weak/credential status with actual enrolled hardware. No face-only claims. Verify system-selected fingerprint and credential flows independently. | Untested |
| Protected launch | Home → selected app → prompt appears automatically, without verify button. Measure visible content before prompt with a second camera. Zero exposure is not promised. | Untested |
| Biometric success | Existing app task/deep link resumes. Same-app activity transitions should not repeatedly prompt. | Untested |
| Biometric mismatch | First mismatch cancels verification, returns Home, no immediate prompt loop. Reopen to retry. | Untested |
| Credential fallback | Accept correct PIN/pattern/password; warning understood. Android handles incorrect credential entries. Test cancellation at credential UI. | Untested |
| Fallback disabled | Strong biometrics only. Disable switch refuses when none ready. Restore via phone settings if biometrics later become unavailable. | Untested |
| Back/cancel | Back, negative button, touch outside if Android permits: Home, no grant, no automatic retries. | Untested |
| Lockout/repeated failures | Reopen and fail repeatedly. Android lockout respected; no in-app bypass or loops. Credential option only when allowed. | Untested |
| Leaving app | Home, Recents, another app, and another selected app revoke prior grant. Return requires fresh verification. | Untested |
| Switch during prompt | Home or another launchable app invalidates the pending challenge and returns Home; stale biometric callback cannot grant. | Untested |
| Screen-off | Power button during unlocked selected app AND during prompt. On unlock, access needs fresh verification when foreground event arrives. Record missed resume events as a detection failure. | Untested |
| Own settings | Reopen Escape: automatic authentication before selections/preferences. Home/app switch and return requires fresh authentication. No selection snapshot in Recents for Escape. | Untested |
| Rotation/configuration | Rotate prompt, credential UI, dashboard. No second prompt while AndroidX retains the original; no settings exposure before success. Large font/display scaling/TalkBack controls remain usable. | Untested |
| Process death | Background Escape, use `adb shell am kill com.escape.security` (OS may retain bound services); enable Don't keep activities or use controlled process termination. Restore gate/settings: no persisted grant; stale gate returns Home. Record whether process actually died. | Untested |
| Permission loss | Disable service manually, withdraw consent, and interrupt connection. Grants disappear; dashboard reflects degradation. There is no app interception once disabled. | Untested |
| Blocked activity launch | Inspect dashboard and Logcat for Android BAL/StrictMode messages; no endless retries. A watchdog warning is evidence of degradation, not successful enforcement. | Untested |
| Deep links | Notification tap, shared link, Recents, shortcut, widget, external intent to selected app: verify existing task is resumed after success; measure leaks. | Untested |
| Keyboards/system windows | Type into selected app, open permission dialogs, notification shade and quick settings. Conservative relock may cause extra prompts; record usability failures. | Untested |
| Multi-window/PiP | Expect no reliable content isolation. Document bypasses, including secondary displays, work profile, and cloned apps. | Untested |
| OEM background controls | Immediate, 30-minute idle, overnight, battery saver, recent-app clear, reboot. Record service toggle, connection, and prompt success each time. | Untested |
| Force-stop/uninstall/safe mode | Demonstrate bypass without claiming resistance. Return to normal mode/reinstall and explicitly restore configuration as needed. | Untested |
| Privacy/permissions | APK has no camera/internet/usage/overlay/admin permission; only selections/consent/preferences persist. Airplane mode does not affect authentication. No usage history UI or uploads. | Untested |

Useful PowerShell commands (replace paths/serials as appropriate):

```powershell
$adbPath = "$env:ANDROID_HOME\platform-tools\adb.exe"
& $adbPath devices -l
& $adbPath shell getprop ro.build.display.id
& $adbPath shell getprop ro.build.version.sdk
& $adbPath shell dumpsys accessibility
& $adbPath logcat -s ActivityTaskManager StrictMode AndroidRuntime
```

Review outputs locally; OS diagnostics may expose package names. Do not collect or upload a usage history as part of routine testing. Test consent withdrawal with a previously granted app and both success/error credential callbacks. A successful build and JVM tests do not certify these phone behaviors.
