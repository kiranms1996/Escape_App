package com.escape.security

import android.app.UiAutomation
import android.content.Intent
import android.os.Build
import android.os.SystemClock
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

/** Opt-in destructive-to-test-session flow. Only artificial emulator credentials are permitted. */
@RunWith(AndroidJUnit4::class)
class ProtectedAppFlowTest {
    @Test fun nativeCredentialFlowCancelsUnlocksAndRelocks() {
        assumeTrue(Build.FINGERPRINT.contains("generic") || Build.MODEL.contains("sdk_gphone") || Build.HARDWARE == "ranchu")
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val arguments = InstrumentationRegistry.getArguments()
        assumeTrue("Opt in with -e liveFlow true on a configured emulator", arguments.getString("liveFlow") == "true")
        val pin = requireNotNull(arguments.getString("testPin")) { "Pass the artificial emulator PIN as testPin" }
        val context = instrumentation.targetContext
        val target = "com.google.android.deskclock"
        Configurator.getInstance().setUiAutomationFlags(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)
        val device = UiDevice.getInstance(instrumentation)
        val home = requireNotNull(device.launcherPackageName)

        fun launch(pkg: String) {
            val intent = context.packageManager.getLaunchIntentForPackage(pkg) ?: error("No launcher for $pkg")
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
        fun waitText(text: String) {
            assertTrue("Expected visible '$text'", device.wait(Until.hasObject(By.textContains(text)), 12_000))
        }
        fun credential() {
            device.wait(Until.findObject(By.text("Use PIN")), 1500)?.click()
            val field = requireNotNull(device.wait(Until.findObject(By.res("com.android.systemui", "lockPassword")), 12_000)) {
                "Expected Android's native PIN field"
            }
            field.text = pin
            device.pressEnter()
        }
        fun homeVisible() {
            assertTrue("Expected Home after denial", device.wait(Until.hasObject(By.pkg(home)), 12_000))
        }

        // A separate test APK/process avoids force-stopping Escape's bound service at test startup.
        device.pressHome()
        SystemClock.sleep(1000)

        launch(target)
        waitText("Unlock protected app")
        println("LIVE PASS: selected app automatically opens native verification")
        device.pressBack()
        homeVisible()
        SystemClock.sleep(2500)
        assertFalse("Cancellation must not cause a prompt loop", device.hasObject(By.text("Unlock protected app")))
        println("LIVE PASS: cancellation returns Home without a prompt loop")

        launch(target)
        waitText("Unlock protected app")
        credential()
        assertTrue("Success must reveal the original Clock task", device.wait(Until.hasObject(By.pkg(target)), 12_000))
        SystemClock.sleep(2500)
        assertFalse("Remaining in the selected app must not re-prompt", device.hasObject(By.text("Unlock protected app")))
        println("LIVE PASS: correct native credential resumes Clock without repeated authentication")

        device.pressHome()
        homeVisible()
        device.waitForIdle(1000)
        launch(target)
        waitText("Unlock protected app")
        device.pressBack()
        homeVisible()
        println("LIVE PASS: leaving and reopening relocks the selected app")

        launch("com.escape.security")
        waitText("Unlock Escape Security")
        assertFalse("Selections must remain hidden", device.hasObject(By.text("Protected apps")))
        credential()
        waitText("Protected apps")
        requireNotNull(device.findObject(By.text("Status"))).click()
        waitText("Best-effort detection ready")
        println("LIVE PASS: Escape settings require authentication and status reports ready")
    }
}
