package com.escape.security

import android.content.Context

class LocalSettings(context: Context) {
    private val prefs = context.getSharedPreferences("escape_settings", Context.MODE_PRIVATE)
    var onboarded: Boolean
        get() = prefs.getBoolean("onboarded", false)
        set(value) { prefs.edit().putBoolean("onboarded", value).apply() }
    var detectionConsent: Boolean
        get() = prefs.getBoolean("detection_consent", false)
        set(value) { prefs.edit().putBoolean("detection_consent", value).apply() }
    var credentialFallback: Boolean
        get() = prefs.getBoolean("credential_fallback", true)
        set(value) { prefs.edit().putBoolean("credential_fallback", value).apply() }
    var protectedPackages: Set<String>
        get() = prefs.getStringSet("protected_packages", emptySet())!!.toSet()
        set(value) { prefs.edit().putStringSet("protected_packages", value.toSet()).apply() }
}
