package com.escape.security

import java.util.UUID

/** Main-thread state machine. No timestamps, app history, or successful sessions are persisted. */
class LockEngine(private val ownPackage: String) {
    data class Challenge(val id: String, val target: String)
    private val processNonce = UUID.randomUUID().toString()
    private var sequence = 0L
    var pending: Challenge? = null
        private set
    var granted: String? = null
        private set
    private var denied: String? = null

    fun foreground(pkg: String, protected: Set<String>, transientDuringPrompt: Boolean = false): Challenge? {
        if (pkg == ownPackage) return null
        if (pending != null && transientDuringPrompt) return null
        if (pending != null && pending?.target != pkg) {
            // Let the existing verification activity cancel and return home before another attempt.
            pending = null
            granted = null
            return null
        }
        if (pkg != denied) denied = null
        if (pkg != granted) granted = null
        if (pending?.target == pkg) return null
        pending = null
        if (pkg !in protected || pkg == granted || pkg == denied) return null
        return Challenge("$processNonce:${++sequence}", pkg).also { pending = it }
    }

    fun succeed(challenge: Challenge): Boolean {
        if (pending != challenge) return false
        granted = challenge.target
        pending = null
        denied = null
        return true
    }

    fun deny(challenge: Challenge) {
        if (pending != challenge) return
        granted = null
        pending = null
        // Duplicate events from the rejected app must not repeatedly summon a new prompt.
        denied = challenge.target
    }

    fun reset() { pending = null; granted = null; denied = null }
}
