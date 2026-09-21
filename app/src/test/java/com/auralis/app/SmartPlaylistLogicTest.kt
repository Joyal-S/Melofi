package com.auralis.app

import com.auralis.app.domain.model.SmartPlaylistType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests verifying smart playlist definitions, types, and lookup logic.
 */
class SmartPlaylistLogicTest {

    @Test
    fun smartPlaylistTypes_areComprehensive() {
        val types = SmartPlaylistType.entries
        assertEquals("Should have exactly 6 smart playlist types", 6, types.size)

        assertTrue(types.contains(SmartPlaylistType.RECENTLY_PLAYED))
        assertTrue(types.contains(SmartPlaylistType.MOST_PLAYED))
        assertTrue(types.contains(SmartPlaylistType.NEVER_PLAYED))
        assertTrue(types.contains(SmartPlaylistType.RECENTLY_ADDED))
        assertTrue(types.contains(SmartPlaylistType.FAVORITES))
        assertTrue(types.contains(SmartPlaylistType.FORGOTTEN_FAVORITES))
    }

    @Test
    fun smartPlaylistTypes_haveNonEmptyMetadata() {
        for (type in SmartPlaylistType.entries) {
            assertTrue("ID must not be blank for ${type.name}", type.id.isNotBlank())
            assertTrue("Title must not be blank for ${type.name}", type.title.isNotBlank())
            assertTrue("Description must not be blank for ${type.name}", type.description.isNotBlank())
            assertTrue("Icon resource must be valid for ${type.name}", type.iconRes > 0)
        }
    }

    @Test
    fun smartPlaylistTypes_idsAreUnique() {
        val ids = SmartPlaylistType.entries.map { it.id }
        assertEquals("All smart playlist IDs must be unique", ids.size, ids.toSet().size)
    }

    @Test
    fun fromId_validIds_returnCorrectType() {
        assertEquals(SmartPlaylistType.RECENTLY_PLAYED, SmartPlaylistType.fromId("recently_played"))
        assertEquals(SmartPlaylistType.MOST_PLAYED, SmartPlaylistType.fromId("most_played"))
        assertEquals(SmartPlaylistType.NEVER_PLAYED, SmartPlaylistType.fromId("never_played"))
        assertEquals(SmartPlaylistType.RECENTLY_ADDED, SmartPlaylistType.fromId("recently_added"))
        assertEquals(SmartPlaylistType.FAVORITES, SmartPlaylistType.fromId("smart_favorites"))
        assertEquals(SmartPlaylistType.FORGOTTEN_FAVORITES, SmartPlaylistType.fromId("forgotten_favorites"))
    }

    @Test
    fun fromId_invalidId_returnsNull() {
        assertNull(SmartPlaylistType.fromId("unknown_type"))
        assertNull(SmartPlaylistType.fromId(""))
    }
}
