package com.escape.security

import android.app.KeyguardManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class VerificationActivity : FragmentActivity() {
    private val session: AuthSession by viewModels()
    private lateinit var prompt: BiometricPrompt
    private lateinit var challenge: LockEngine.Challenge
    private var exiting = false
    private val handler = Handler(Looper.getMainLooper())
    companion object { var lastShownChallenge: String? = null; private set }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        challenge = LockEngine.Challenge(intent.getStringExtra("challenge") ?: "", intent.getStringExtra("target") ?: "")
        prompt = systemPrompt(this, session, {
            session.successWaiting = true
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) complete()
        }, { deny(it) })
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = deny("Verification cancelled.")
        })
        setContent { EscapeTheme { VerificationScreen("Checking your identity", "Android verification opens automatically. Access is allowed only after success. This is best-effort app protection.") { deny("Verification cancelled.") } } }
        lifecycleScope.launch {
            RuntimeState.revision.collect {
                if (!exiting && RuntimeState.engine.pending != challenge) deny("This verification session has ended.")
            }
        }
    }

    override fun onResume() {
        super.onResume()
        lastShownChallenge = challenge.id
        val settings = LocalSettings(this)
        if (RuntimeState.engine.pending != challenge || !RuntimeState.connected.value ||
            !settings.detectionConsent || challenge.target !in settings.protectedPackages ||
            getSystemService(KeyguardManager::class.java).isKeyguardLocked) {
            deny("Protection session unavailable. Reopen the app after checking Escape Security.")
            return
        }
        if (session.successWaiting) { complete(); return }
        if (!session.started) {
            session.started = true
            session.startedAt = SystemClock.elapsedRealtime()
            if (!Authentication.available(this, settings.credentialFallback)) {
                deny("No allowed authenticator is ready. Check Escape Security and your phone's security settings.")
            } else {
                runCatching { prompt.authenticate(promptInfo(settings.credentialFallback, "Unlock protected app")) }
                    .onFailure { deny("Android could not start verification.") }
            }
        }
        handler.removeCallbacksAndMessages(null)
        handler.postDelayed({ deny("Verification timed out. Reopen the app to try again.") },
            (90_000 - (SystemClock.elapsedRealtime() - session.startedAt)).coerceAtLeast(0))
    }

    override fun onPostResume() { super.onPostResume(); if (session.successWaiting) complete() }

    private fun complete() {
        if (exiting) return
        if (SystemClock.elapsedRealtime() - session.startedAt >= 90_000 ||
            !getSystemService(PowerManager::class.java).isInteractive ||
            getSystemService(KeyguardManager::class.java).isKeyguardLocked ||
            !RuntimeState.connected.value || !LocalSettings(this).detectionConsent ||
            challenge.target !in LocalSettings(this).protectedPackages) {
            deny("Protection changed during verification."); return
        }
        // A stale callback, including one from before process recreation, cannot grant access.
        if (!RuntimeState.engine.succeed(challenge)) { deny("Verification expired."); return }
        exiting = true
        RuntimeState.changed()
        // The protected task is underneath. Do not launch its main activity or replay a deep link.
        finishAndRemoveTask()
    }

    private fun deny(message: String) {
        if (exiting) return
        exiting = true
        session.terminal = true
        prompt.cancelAuthentication()
        RuntimeState.engine.deny(challenge)
        RuntimeState.changed()
        returnHome(message)
    }

    override fun onDestroy() { handler.removeCallbacksAndMessages(null); super.onDestroy() }
}
