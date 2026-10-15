package com.escape.security

import android.content.Intent
import android.content.ComponentName
import android.os.Build
import android.app.UiAutomation
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Configurator
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Non-destructive UI tests. Run after emulator onboarding; never enroll or enter credentials here. */
@RunWith(AndroidJUnit4::class)
class EmulatorUiTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val device = run {
        Configurator.getInstance().setUiAutomationFlags(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)
        UiDevice.getInstance(instrumentation)
    }

    private fun emulatorOnly() {
        assumeTrue("Only run this fixture on an emulator", Build.FINGERPRINT.contains("generic") ||
            Build.MODEL.contains("sdk_gphone") || Build.HARDWARE == "ranchu")
    }

    private fun openEscape() {
        device.pressHome()
        assertTrue(device.wait(Until.hasObject(By.pkg(requireNotNull(device.launcherPackageName))), 10_000))
        device.waitForIdle(1000)
        val intent = requireNotNull(instrumentation.targetContext.packageManager.getLaunchIntentForPackage("com.escape.security"))
        instrumentation.targetContext.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    @Test fun settingsRequireIdentityBeforeExposingSelections() {
        emulatorOnly()
        openEscape()
        assertTrue("Escape must show its lock screen or the native auth UI", device.wait(Until.hasObject(
            By.textContains("Unlock Escape Security")), 10_000))
        assertFalse("Protected app switches must not be shown before authentication", device.hasObject(
            By.text("Protected apps")))
        device.pressBack()
    }

    @Test fun verificationActivityIsNotExported() {
        emulatorOnly()
        val activity = instrumentation.targetContext.packageManager.getActivityInfo(
            ComponentName("com.escape.security", "com.escape.security.VerificationActivity"), 0)
        assertFalse("Challenge activity must reject external callers", activity.exported)
    }

    @Test fun appHasNoContentOrGestureAccessibilityCapability() {
        emulatorOnly()
        val manager = instrumentation.targetContext.getSystemService(android.view.accessibility.AccessibilityManager::class.java)
        val service = manager.installedAccessibilityServiceList.first {
            it.resolveInfo.serviceInfo.name == "com.escape.security.ProtectionService"
        }
        val forbidden = android.accessibilityservice.AccessibilityServiceInfo.CAPABILITY_CAN_RETRIEVE_WINDOW_CONTENT or
            android.accessibilityservice.AccessibilityServiceInfo.CAPABILITY_CAN_PERFORM_GESTURES or
            android.accessibilityservice.AccessibilityServiceInfo.CAPABILITY_CAN_TAKE_SCREENSHOT
        assertEquals(0, service.capabilities and forbidden)
        assertEquals(android.view.accessibility.AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED, service.eventTypes)
    }
}
