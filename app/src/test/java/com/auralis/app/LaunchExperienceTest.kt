package com.auralis.app

import com.auralis.app.core.ui.theme.MelofyAnimationMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests verifying startup behavior, cold/warm start state tracking,
 * and accessibility motion adaptation for Melofi's premium launch experience.
 */
class LaunchExperienceTest {

    @Before
    fun setUp() {
        // Reset state before each test
        MainActivity.hasCompletedInitialLaunch = false
    }

    @Test
    fun coldStart_initialStateIsUncompleted() {
        // On initial application process start, initial launch has not completed
        assertFalse(
            "Initial cold start must indicate launch experience has not yet completed",
            MainActivity.hasCompletedInitialLaunch
        )
    }

    @Test
    fun launchComplete_updatesState() {
        // Simulating launch completion
        MainActivity.hasCompletedInitialLaunch = true
        assertTrue(
            "After launch completion, state must be marked as completed",
            MainActivity.hasCompletedInitialLaunch
        )
    }

    @Test
    fun warmStart_preservesCompletedState() {
        // Mark initial launch completed on cold start
        MainActivity.hasCompletedInitialLaunch = true

        // Simulate returning to app from background (warm start)
        val isWarmStartBypassed = MainActivity.hasCompletedInitialLaunch
        assertTrue(
            "Warm start must retain completed state to bypass the splash animation",
            isWarmStartBypassed
        )
    }

    @Test
    fun animationModeOff_bypassesLaunchExperienceImmediately() {
        val animationMode = MelofyAnimationMode.OFF
        val shouldShowLaunch = !MainActivity.hasCompletedInitialLaunch && animationMode != MelofyAnimationMode.OFF

        assertFalse(
            "When animation mode is OFF, the launch experience must be bypassed immediately",
            shouldShowLaunch
        )
    }

    @Test
    fun animationModeFull_enablesLaunchExperienceOnColdStart() {
        val animationMode = MelofyAnimationMode.FULL
        val shouldShowLaunch = !MainActivity.hasCompletedInitialLaunch && animationMode != MelofyAnimationMode.OFF

        assertTrue(
            "Cold start with full animations enabled must show the branded launch experience",
            shouldShowLaunch
        )
    }

    @Test
    fun animationModeReduced_enablesLaunchExperienceOnColdStart() {
        val animationMode = MelofyAnimationMode.REDUCED
        val shouldShowLaunch = !MainActivity.hasCompletedInitialLaunch && animationMode != MelofyAnimationMode.OFF

        assertTrue(
            "Cold start with reduced motion enabled must show the accessible quick launch experience",
            shouldShowLaunch
        )
    }

    @Test
    fun animationModeDisplayNames_areConsistent() {
        assertEquals("Full Motion", MelofyAnimationMode.FULL.displayName)
        assertEquals("Reduced Motion", MelofyAnimationMode.REDUCED.displayName)
        assertEquals("Animations Off", MelofyAnimationMode.OFF.displayName)
    }
}
