package com.escape.security

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.PowerManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle

class MainActivity : FragmentActivity() {
    private val session: AuthSession by viewModels()
    private lateinit var settings: LocalSettings
    private lateinit var prompt: BiometricPrompt
    private var resumed by mutableStateOf(false)
    private var onboarded by mutableStateOf(false)
    private val handler = Handler(Looper.getMainLooper())
    private val timeout = Runnable {
        if (!session.authorized && session.started) {
            session.terminal = true
            prompt.cancelAuthentication()
            returnHome("Verification timed out. Reopen Escape to try again.")
        }
    }
    private val screenOff = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            RuntimeState.reset()
            session.authorized = false
            session.terminal = true
            prompt.cancelAuthentication()
            returnHome()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        settings = LocalSettings(this)
        onboarded = settings.onboarded
        prompt = systemPrompt(this, session, {
            session.successWaiting = true
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) complete()
        }, {
            session.authorized = false
            prompt.cancelAuthentication()
            returnHome(it)
        })
        ContextCompat.registerReceiver(this, screenOff, IntentFilter(Intent.ACTION_SCREEN_OFF),
            ContextCompat.RECEIVER_NOT_EXPORTED)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { session.terminal = true; prompt.cancelAuthentication(); returnHome() }
        })
        setContent {
            EscapeTheme {
                if (!onboarded) OnboardingScreen(settings) {
                    settings.onboarded = true
                    onboarded = true
                    session.fresh()
                    authenticate()
                } else if (session.authorized && resumed) DashboardScreen(settings)
                else VerificationScreen("Unlock Escape Security", "Your app selections and settings require identity verification. The system prompt starts automatically.") {
                    session.terminal = true; prompt.cancelAuthentication(); returnHome()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        resumed = true
        // Entering Escape's own settings revokes any previously granted protected-app session.
        RuntimeState.reset()
        if (onboarded && !session.authorized) {
            if (session.successWaiting) complete() else authenticate()
            if (session.started && !session.authorized) {
                handler.removeCallbacks(timeout)
                handler.postDelayed(timeout, (90_000 - (SystemClock.elapsedRealtime() - session.startedAt)).coerceAtLeast(0))
            }
        }
    }

    override fun onPostResume() { super.onPostResume(); if (session.successWaiting) complete() }

    private fun authenticate() {
        if (session.started || isFinishing) return
        session.started = true
        session.startedAt = SystemClock.elapsedRealtime()
        handler.removeCallbacks(timeout)
        handler.postDelayed(timeout, 90_000)
        if (!Authentication.available(this, settings.credentialFallback)) {
            // No auth bypass into settings. System security settings remain independently accessible.
            returnHome("No allowed authenticator is ready. Enroll/unlock a strong biometric or restore your device credential in phone Settings.")
            return
        }
        runCatching { prompt.authenticate(promptInfo(settings.credentialFallback, "Unlock Escape Security")) }
            .onFailure { returnHome("Android could not start verification.") }
    }

    private fun complete() {
        if (SystemClock.elapsedRealtime() - session.startedAt >= 90_000 ||
            !getSystemService(PowerManager::class.java).isInteractive ||
            getSystemService(KeyguardManager::class.java).isKeyguardLocked) {
            session.authorized = false
            returnHome("Unlock your phone and try again.")
            return
        }
        session.successWaiting = false
        session.authorized = true
        handler.removeCallbacks(timeout)
    }

    override fun onPause() { resumed = false; super.onPause() }
    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations && session.authorized) session.fresh()
    }
    override fun onDestroy() { handler.removeCallbacksAndMessages(null); unregisterReceiver(screenOff); super.onDestroy() }
}
