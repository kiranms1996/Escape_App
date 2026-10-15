package com.escape.security

import android.accessibilityservice.AccessibilityService
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.view.accessibility.AccessibilityEvent
import androidx.core.content.ContextCompat

class ProtectionService : AccessibilityService() {
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var settings: LocalSettings
    private var knownLaunchable = emptySet<String>()
    private var exclusions = emptySet<String>()
    private val screenOff = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) { RuntimeState.reset() }
    }

    override fun onServiceConnected() {
        settings = LocalSettings(this)
        knownLaunchable = launchableApps(this).map { it.packageName }.toSet()
        exclusions = excludedPackages(this)
        RuntimeState.reset()
        RuntimeState.connected.value = true
        ContextCompat.registerReceiver(this, screenOff, IntentFilter(Intent.ACTION_SCREEN_OFF),
            ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!::settings.isInitialized || event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        // Deliberately do not access event.text, event.source, className, rootInActiveWindow, or windows.
        if (!settings.onboarded || !settings.detectionConsent) { RuntimeState.reset(); return }
        val pkg = event.packageName?.toString() ?: return
        if (!getSystemService(PowerManager::class.java).isInteractive ||
            getSystemService(KeyguardManager::class.java).isKeyguardLocked) {
            RuntimeState.reset()
            return
        }
        val protected = settings.protectedPackages - exclusions
        // Only while a prompt is pending, ignore non-launchable system/credential windows.
        // A real switch to home or another launchable app invalidates that prompt.
        val challenge = RuntimeState.engine.foreground(pkg, protected,
            transientDuringPrompt = pkg !in knownLaunchable && pkg !in exclusions && pkg !in protected)
        RuntimeState.changed()
        if (challenge != null) {
            RuntimeState.launchIssue.value = null
            try {
                startActivity(Intent(this, VerificationActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    .putExtra("challenge", challenge.id).putExtra("target", challenge.target))
                // startActivity can be silently blocked. No retries or forced UI interactions.
                handler.postDelayed({
                    if (RuntimeState.engine.pending == challenge && VerificationActivity.lastShownChallenge != challenge.id) {
                        RuntimeState.engine.deny(challenge)
                        RuntimeState.launchIssue.value = "Verification did not become visible. Protection is degraded; check background restrictions and test again."
                        RuntimeState.changed()
                    }
                }, 2000)
            } catch (_: RuntimeException) {
                RuntimeState.engine.deny(challenge)
                RuntimeState.launchIssue.value = "Android refused to open verification. Protection is degraded."
                RuntimeState.changed()
            }
        }
    }

    override fun onInterrupt() { RuntimeState.reset() }
    override fun onUnbind(intent: Intent?): Boolean {
        RuntimeState.connected.value = false
        RuntimeState.reset()
        return super.onUnbind(intent)
    }
    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        runCatching { unregisterReceiver(screenOff) }
        RuntimeState.connected.value = false
        RuntimeState.reset()
        super.onDestroy()
    }
}
