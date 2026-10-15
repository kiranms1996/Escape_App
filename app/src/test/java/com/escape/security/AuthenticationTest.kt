package com.escape.security

import androidx.biometric.BiometricManager.Authenticators.*
import org.junit.Assert.*
import org.junit.Test

class AuthenticationTest {
    @Test fun credentialFallbackIsExplicitInMask() {
        assertEquals(BIOMETRIC_STRONG or DEVICE_CREDENTIAL, Authentication.mask(true))
        assertEquals(BIOMETRIC_STRONG, Authentication.mask(false))
    }
    @Test fun unknownAuthenticatorStatusIsNeverReportedReady() {
        assertNotEquals("Ready", Authentication.describe(-12345))
    }
}
