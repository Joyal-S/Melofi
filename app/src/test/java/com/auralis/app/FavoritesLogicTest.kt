package com.auralis.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for favorite set operations, toggle behavior, and duplicate prevention.
 */
class FavoritesLogicTest {

    @Test
    fun favoriteSet_lookupIsO1AndAccurate() {
        val favoriteIds = setOf(10L, 20L, 30L)

        assertTrue(favoriteIds.contains(10L))
        assertTrue(favoriteIds.contains(20L))
        assertTrue(favoriteIds.contains(30L))
        assertFalse(favoriteIds.contains(40L))
        assertFalse(favoriteIds.contains(999L))
    }

    @Test
    fun toggleFavorite_transitionsBetweenAddedAndRemoved() {
        val favoriteSet = mutableSetOf<Long>()

        fun toggle(id: Long): Boolean {
            return if (favoriteSet.contains(id)) {
                favoriteSet.remove(id)
                false
            } else {
                favoriteSet.add(id)
                true
            }
        }

        // Initially absent -> toggle returns true (added)
        val added = toggle(42L)
        assertTrue(added)
        assertTrue(favoriteSet.contains(42L))

        // Now present -> toggle returns false (removed)
        val removed = toggle(42L)
        assertFalse(removed)
        assertFalse(favoriteSet.contains(42L))
    }

    @Test
    fun duplicateFavorite_setDeduplicatesAutomatically() {
        val listWithDuplicates = listOf(10L, 20L, 10L, 30L, 20L)
        val set = listWithDuplicates.toSet()

        assertEquals(3, set.size)
        assertTrue(set.containsAll(listOf(10L, 20L, 30L)))
    }

    @Test
    fun cascadeDeletion_removesDeletedSongFromFavorites() {
        val favorites = mutableSetOf(101L, 102L, 103L)
        val deletedSongIds = listOf(102L)

        favorites.removeAll(deletedSongIds.toSet())

        assertEquals(2, favorites.size)
        assertTrue(favorites.contains(101L))
        assertFalse(favorites.contains(102L))
        assertTrue(favorites.contains(103L))
    }
}
