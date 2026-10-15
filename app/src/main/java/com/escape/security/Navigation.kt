package com.escape.security

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.widget.Toast

fun Context.openSettings(intent: Intent) {
    runCatching { startActivity(intent) }.onFailure {
        Toast.makeText(this, "This settings page is unavailable. Open your phone's Settings app manually.", Toast.LENGTH_LONG).show()
    }
}

fun Activity.returnHome(message: String? = null) {
    if (message != null) Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    runCatching { startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    finishAndRemoveTask()
}
