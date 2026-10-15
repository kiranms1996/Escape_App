package com.escape.security

import android.app.Application
import android.os.Build
import android.os.StrictMode
import kotlinx.coroutines.flow.MutableStateFlow

class EscapeApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        RuntimeState.engine = LockEngine(packageName)
        if (BuildConfig.DEBUG && Build.VERSION.SDK_INT >= 36) {
            StrictMode.setVmPolicy(StrictMode.VmPolicy.Builder()
                .detectBlockedBackgroundActivityLaunch().penaltyLog().build())
        }
    }
}

object RuntimeState {
    lateinit var engine: LockEngine
    val revision = MutableStateFlow(0L)
    val connected = MutableStateFlow(false)
    val launchIssue = MutableStateFlow<String?>(null)
    fun changed() { revision.value += 1 }
    fun reset() { engine.reset(); changed() }
}
