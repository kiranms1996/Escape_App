package com.escape.security

import android.app.Activity
import android.app.UiAutomation
import android.os.Build
import android.os.Bundle
import androidx.test.runner.AndroidJUnitRunner
import androidx.test.uiautomator.Configurator
import androidx.test.uiautomator.UiDevice
import java.io.ByteArrayOutputStream

/** Host-only screen inspector in a separate harness process. Never packaged in Escape Security. */
class UiDumpRunner : AndroidJUnitRunner() {
    private var inspectUi = false
    override fun onCreate(arguments: Bundle) {
        inspectUi = arguments.getString("inspectUi") == "true"
        super.onCreate(arguments)
    }
    override fun onStart() {
        if (!inspectUi) { super.onStart(); return }
        if (!(Build.FINGERPRINT.contains("generic") || Build.MODEL.contains("sdk_gphone") || Build.HARDWARE == "ranchu")) {
            finish(Activity.RESULT_CANCELED, Bundle().apply { putString("stream", "Emulator required") })
            return
        }
        try {
            Configurator.getInstance().setUiAutomationFlags(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)
            val buffer = ByteArrayOutputStream()
            UiDevice.getInstance(this).dumpWindowHierarchy(buffer)
            finish(Activity.RESULT_OK, Bundle().apply {
                putString("stream", "ESCAPE_UI_XML_BEGIN${buffer.toString(Charsets.UTF_8.name())}ESCAPE_UI_XML_END")
            })
        } catch (error: Exception) {
            finish(Activity.RESULT_CANCELED, Bundle().apply { putString("stream", "Inspection failed: ${error.message}") })
        }
    }
}
