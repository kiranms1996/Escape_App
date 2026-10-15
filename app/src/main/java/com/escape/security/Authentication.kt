package com.escape.security

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf

object Authentication {
    fun mask(fallback: Boolean) = BIOMETRIC_STRONG or (if (fallback) DEVICE_CREDENTIAL else 0)
    fun status(context: Context, mask: Int): Int = BiometricManager.from(context).canAuthenticate(mask)
    fun available(context: Context, fallback: Boolean) = status(context, mask(fallback)) == BiometricManager.BIOMETRIC_SUCCESS
    fun describe(code: Int): String = when (code) {
        BiometricManager.BIOMETRIC_SUCCESS -> "Ready"
        BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> "Nothing enrolled"
        BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> "No supported hardware"
        BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> "Temporarily unavailable"
        BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> "Security update required"
        BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED -> "Unsupported combination"
        else -> "Unavailable (code $code)"
    }
    fun report(context: Context): String = "Strong biometrics: ${describe(status(context, BIOMETRIC_STRONG))}\n" +
        "Class 2 or stronger: ${describe(status(context, BIOMETRIC_WEAK))}\n" +
        "Device credential: ${describe(status(context, DEVICE_CREDENTIAL))}\n" +
        "These checks do not identify face or fingerprint. Escape accepts Class 3 biometrics. Android chooses the modality; face cannot be forced. Screen-unlock face recognition may be unavailable here."
}

class AuthSession : ViewModel() {
    var authorized by mutableStateOf(false)
    var started = false
    var startedAt = 0L
    var terminal = false
    var successWaiting = false
    fun fresh() { started = false; startedAt = 0L; terminal = false; successWaiting = false; authorized = false }
}

/** Recreate on each Activity creation to reconnect AndroidX's retained biometric fragment. */
fun systemPrompt(activity: FragmentActivity, session: AuthSession, onSuccess: () -> Unit,
                 onDenied: (String) -> Unit): BiometricPrompt = BiometricPrompt(activity,
    ContextCompat.getMainExecutor(activity), object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
            if (session.terminal) return
            session.terminal = true
            onSuccess()
        }
        override fun onAuthenticationFailed() {
            if (session.terminal) return
            session.terminal = true
            onDenied("Identity was not verified. Reopen the app to try again.")
        }
        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
            if (session.terminal) return
            session.terminal = true
            onDenied("Verification ended: $errString")
        }
    })

fun promptInfo(fallback: Boolean, title: String): BiometricPrompt.PromptInfo {
    val builder = BiometricPrompt.PromptInfo.Builder().setTitle(title)
        .setSubtitle("Escape Security · identity verification")
        .setDescription(if (fallback) "Use a strong biometric or your phone PIN, pattern, or password. Anyone who knows it can authenticate."
            else "Use a supported strong biometric. Android chooses which sensor is available.")
        .setAllowedAuthenticators(Authentication.mask(fallback))
        .setConfirmationRequired(true)
    if (!fallback) builder.setNegativeButtonText("Cancel")
    return builder.build()
}
