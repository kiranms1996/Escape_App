package com.escape.security

import org.junit.Assert.*
import org.junit.Test

class LockEngineTest {
    private val selected = setOf("bank", "mail")
    private fun engine() = LockEngine("escape")

    @Test fun unselectedAppsDoNotPrompt() {
        assertNull(engine().foreground("browser", selected))
    }
    @Test fun selectedAppPromptsExactlyOnceForDuplicateEvents() {
        val e = engine()
        val c = e.foreground("bank", selected)!!
        repeat(100) { assertNull(e.foreground("bank", selected)) }
        assertEquals(c, e.pending)
        assertNull(e.granted)
    }
    @Test fun ownVerificationWindowDoesNotCancelPendingChallenge() {
        val e = engine(); val c = e.foreground("bank", selected)
        assertNull(e.foreground("escape", selected))
        assertEquals(c, e.pending)
    }
    @Test fun successfulAuthenticationGrantsOnlyThatApp() {
        val e = engine(); val c = e.foreground("bank", selected)!!
        assertTrue(e.succeed(c))
        assertEquals("bank", e.granted)
        assertNull(e.foreground("bank", selected))
        assertNotNull(e.foreground("mail", selected))
        assertNull(e.granted)
    }
    @Test fun leavingAppRelocksIt() {
        val e = engine(); e.succeed(e.foreground("bank", selected)!!)
        e.foreground("home", selected)
        assertNull(e.granted)
        assertNotNull(e.foreground("bank", selected))
    }
    @Test fun cancellationSuppressesDuplicateEventsUntilAppIsLeft() {
        val e = engine(); e.deny(e.foreground("bank", selected)!!)
        repeat(100) { assertNull(e.foreground("bank", selected)) }
        e.foreground("home", selected)
        assertNotNull(e.foreground("bank", selected))
    }
    @Test fun lateSuccessAfterDenialCannotUnlock() {
        val e = engine(); val c = e.foreground("bank", selected)!!
        e.deny(c)
        assertFalse(e.succeed(c))
        assertNull(e.granted)
    }
    @Test fun movingHomeDuringPromptInvalidatesCallback() {
        val e = engine(); val c = e.foreground("bank", selected)!!
        e.foreground("home", selected)
        assertFalse(e.succeed(c))
        assertNull(e.pending)
    }
    @Test fun switchingSelectedAppDuringPromptCancelsWithoutStartingCompetingPrompt() {
        val e = engine(); val c = e.foreground("bank", selected)!!
        assertNull(e.foreground("mail", selected))
        assertNull(e.pending)
        assertFalse(e.succeed(c))
    }
    @Test fun systemCredentialWindowCanBeIgnoredOnlyWhilePromptPending() {
        val e = engine(); val c = e.foreground("bank", selected)!!
        e.foreground("credentialui", selected, transientDuringPrompt = true)
        assertEquals(c, e.pending)
        e.succeed(c)
        e.foreground("systemui", selected, transientDuringPrompt = true)
        assertNull(e.granted)
    }
    @Test fun screenOffOrPermissionLossResetRejectsPendingSuccess() {
        val e = engine(); val c = e.foreground("bank", selected)!!
        e.reset()
        assertFalse(e.succeed(c))
        assertNull(e.pending)
        assertNull(e.granted)
    }
    @Test fun resetRevokesExistingGrant() {
        val e = engine(); e.succeed(e.foreground("bank", selected)!!)
        e.reset()
        assertNotNull(e.foreground("bank", selected))
    }
    @Test fun processRecreationHasNoSuccessfulSession() {
        val previous = engine(); val c = previous.foreground("bank", selected)!!
        previous.succeed(c)
        val recreated = engine()
        assertNull(recreated.granted)
        assertFalse(recreated.succeed(c))
        assertNotNull(recreated.foreground("bank", selected))
        assertFalse(recreated.succeed(c))
    }
    @Test fun previousAttemptCannotCompleteNewAttempt() {
        val e = engine(); val old = e.foreground("bank", selected)!!
        e.deny(old); e.foreground("home", selected)
        val current = e.foreground("bank", selected)!!
        assertNotEquals(old.id, current.id)
        assertFalse(e.succeed(old))
        assertTrue(e.succeed(current))
    }
    @Test fun replayOfSuccessfulCallbackCannotGrantTwice() {
        val e = engine(); val c = e.foreground("bank", selected)!!
        assertTrue(e.succeed(c))
        assertFalse(e.succeed(c))
    }
    @Test fun removingSelectionPreventsNewChallenge() {
        val e = engine(); e.foreground("bank", selected); e.reset()
        assertNull(e.foreground("bank", emptySet()))
    }
}
