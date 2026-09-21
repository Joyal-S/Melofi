package com.auralis.app

import com.auralis.app.widget.MelofyWidgetProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests verifying widget actions, intent contract, and state mapping.
 */
class WidgetStateTest {

    @Test
    fun widgetActionConstants_areProperlyNamespaced() {
        assertEquals("com.auralis.app.ACTION_WIDGET_PLAY_PAUSE", MelofyWidgetProvider.ACTION_WIDGET_PLAY_PAUSE)
        assertEquals("com.auralis.app.ACTION_WIDGET_NEXT", MelofyWidgetProvider.ACTION_WIDGET_NEXT)
        assertEquals("com.auralis.app.ACTION_WIDGET_PREVIOUS", MelofyWidgetProvider.ACTION_WIDGET_PREVIOUS)
    }

    @Test
    fun widgetActions_areUnique() {
        val actions = listOf(
            MelofyWidgetProvider.ACTION_WIDGET_PLAY_PAUSE,
            MelofyWidgetProvider.ACTION_WIDGET_NEXT,
            MelofyWidgetProvider.ACTION_WIDGET_PREVIOUS
        )
        assertEquals(actions.size, actions.toSet().size)
    }

    @Test
    fun widgetActions_followStandardNamingConvention() {
        assertTrue(MelofyWidgetProvider.ACTION_WIDGET_PLAY_PAUSE.startsWith("com.auralis.app."))
        assertTrue(MelofyWidgetProvider.ACTION_WIDGET_NEXT.startsWith("com.auralis.app."))
        assertTrue(MelofyWidgetProvider.ACTION_WIDGET_PREVIOUS.startsWith("com.auralis.app."))
    }
}
