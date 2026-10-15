# Android 16 verification and design decisions

Reviewed official Android documentation on 15 October 2026 before implementation. These sources describe platform behavior, not measured Vivo firmware behavior.

| Question | Finding and prototype decision |
| --- | --- |
| Can an ordinary app veto launches of arbitrary installed apps? | There is no general public pre-launch interception API for a personal app locker. This implementation reacts to a window event. Target-app content may appear first; protection is advisory and bypassable. |
| Can it show verification from the background? | Android restricts background activity launches. Current guidance describes exceptions, including permitted service bindings, and separate PendingIntent opt-ins. The prototype attempts a direct explicit start from the user-enabled system-bound service. It uses neither a PendingIntent trampoline nor a foreground-service workaround. A two-second visibility watchdog marks an apparent blocked launch as degraded; it cannot prove interception reliability. Debug builds enable Android 16 blocked-launch StrictMode logging. |
| Why not usage access? | UsageStatsManager exposes queries requiring special usage access; polling introduces latency and does not provide pre-launch interception or general background-launch privileges. No usage access is requested. |
| Why accessibility? | It offers event callbacks instead of polling. Only window-state events and package names are processed. Events across packages are needed to revoke access when leaving. No content, source nodes, text, class names, screenshots, key events, or gestures are requested or read. User enablement and in-app consent are both required. This use is not an accessibility tool. |
| Can face be forced? | BiometricPrompt accepts authenticator strengths, not a face-only requirement. Runtime strong, weak, and credential readiness are reported separately. This prototype accepts Class 3 only, optionally OR'd with DEVICE_CREDENTIAL. It never claims the reported success proves a face sensor exists. A Class 2-only face sensor is excluded; no custom camera substitute exists. |
| What does fallback mean? | DEVICE_CREDENTIAL accepts the phone PIN, pattern, or password. Anyone knowing it can authenticate. No app-specific PIN is created. The exact allowed mask is checked immediately before starting the prompt. Disabling fallback is refused unless strong biometrics are ready. |
| What Android 16 changes matter? | Review both all-app and target-36 changes. Background activity restrictions still apply; target-36 edge-to-edge is handled with Compose insets. No background jobs, FGS, hidden camera, notifications, or boot-start work are introduced. Large-screen/multi-window bypass remains outside this locker's guarantee. |

Sources:

- [Android activity security and background activity launches](https://developer.android.com/guide/components/activities/secure-bal): restrictions, exceptions, PendingIntent rules, Android 16 StrictMode diagnostics. A successful `startActivity` call does not by itself prove the activity was displayed.
- [Android 16 all-app behavior changes](https://developer.android.com/about/versions/16/behavior-changes-all) and [target-36 behavior changes](https://developer.android.com/about/versions/16/behavior-changes-16).
- [Biometric authentication](https://developer.android.com/identity/sign-in/biometric-auth): strength masks, canAuthenticate, credential fallback, and incompatibility of a negative button with DEVICE_CREDENTIAL.
- [Accessibility service configuration](https://developer.android.com/guide/topics/ui/accessibility/views/service) and [AccessibilityService reference](https://developer.android.com/reference/android/accessibilityservice/AccessibilityService): event filtering, explicit enablement, and content-retrieval capability.
- [UsageStatsManager](https://developer.android.com/reference/android/app/usage/UsageStatsManager): permission-gated event queries.
- [Package visibility declarations](https://developer.android.com/training/package-visibility/declaring): scoped launcher/home intent queries rather than QUERY_ALL_PACKAGES. App list is profile-scoped, not a cross-profile inventory.
- [AGP 8.13 compatibility](https://developer.android.com/build/releases/agp-8-13-0-release-notes): pinned build tool baseline.
- [Play AccessibilityService API policy](https://support.google.com/googleplay/android-developer/answer/10964491) and [prominent disclosure and consent](https://support.google.com/googleplay/android-developer/answer/11150561): a separate publication review is required. Do not mislabel this app as a disability-focused accessibility tool. Personal sideloading does not establish Play eligibility.

## Lifecycle and state

The service and activities share one application process. Only preferences persist. Pending challenges and granted package are in memory; a non-exported verification activity must present the active challenge before it can grant a session. Process recreation invalidates old challenges. AndroidX retains an in-flight BiometricPrompt across configuration recreation; a ViewModel avoids issuing a second prompt. The callbacks are reattached by the new Activity. These lifecycle assumptions need physical testing with credential fallback on the target firmware.

The first biometric mismatch is treated as denial, even though Android normally permits additional attempts in the same prompt. The app cancels that prompt and returns Home. OS credential entry errors are managed within Android's credential UI until it reports success/cancellation/error. Terminal errors and cancellation never grant access. Duplicate rejected-package events are suppressed until a different package is observed. Screen-off, service unbind/interruption, and settings changes clear volatile state.

While a prompt is pending, events from non-launchable system packages are ignored so the credential UI does not cancel itself. A different launchable app or Home invalidates the attempt. After success, any other package event conservatively revokes the grant, including system UI; this can cause extra authentication around keyboards/dialogs on some firmware. The prototype favors relocking over long-lived exceptions. No target-app task is relaunched: finishing verification reveals the existing task, preserving its deep link. Missing/late events, split-screen, PiP, external displays, and OEM task behavior may defeat this assumption; test explicitly.

## Permission and threat inventory

App manifest requests USE_BIOMETRIC only. BIND_ACCESSIBILITY_SERVICE is a permission the service **requires of its binder**, rather than an ordinary user-granted app permission. AndroidX may merge library permissions; inspect the actual merged manifest/APK when validating. No camera, microphone, internet, storage, usage access, overlay, device admin, root, or private API is used. Backups and device transfer of settings are excluded. Debug signing, OS compromise, ADB access, and a person with the phone credential are outside the threat boundary.

The app cannot make other applications secure or tamper-proof. Prefer their own authentication, phone screen lock, and notification privacy settings for sensitive data. No claim of guaranteed prevention is made even when Status says detection is ready.
