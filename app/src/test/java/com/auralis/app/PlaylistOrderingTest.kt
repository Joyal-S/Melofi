package com.auralis.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for playlist deterministic ordering and position compaction logic.
 */
class PlaylistOrderingTest {

    data class PlaylistItem(val songId: Long, var position: Int)

    private fun addSong(items: MutableList<PlaylistItem>, songId: Long) {
        val maxPos = items.maxOfOrNull { it.position }
        val nextPos = if (maxPos != null) maxPos + 1 else 0
        items.add(PlaylistItem(songId, nextPos))
    }

    private fun removeSong(items: MutableList<PlaylistItem>, songId: Long) {
        val removed = items.firstOrNull { it.songId == songId } ?: return
        items.remove(removed)
        // Compact positions
        for (item in items) {
            if (item.position > removed.position) {
                item.position -= 1
            }
        }
    }

    private fun reorder(items: MutableList<PlaylistItem>, orderedSongIds: List<Long>) {
        for ((index, id) in orderedSongIds.withIndex()) {
            items.firstOrNull { it.songId == id }?.position = index
        }
        items.sortBy { it.position }
    }

    @Test
    fun addSong_allocatesSequentialPositions() {
        val items = mutableListOf<PlaylistItem>()
        addSong(items, 101L)
        addSong(items, 102L)
        addSong(items, 103L)

        assertEquals(3, items.size)
        assertEquals(0, items[0].position)
        assertEquals(1, items[1].position)
        assertEquals(2, items[2].position)
    }

    @Test
    fun removeSong_middleItem_compactsRemainingPositions() {
        val items = mutableListOf<PlaylistItem>()
        addSong(items, 101L) // 0
        addSong(items, 102L) // 1
        addSong(items, 103L) // 2

        removeSong(items, 102L)

        assertEquals(2, items.size)
        assertEquals(101L, items[0].songId)
        assertEquals(0, items[0].position)
        assertEquals(103L, items[1].songId)
        assertEquals(1, items[1].position)
    }

    @Test
    fun removeSong_firstItem_compactsRemainingPositions() {
        val items = mutableListOf<PlaylistItem>()
        addSong(items, 101L) // 0
        addSong(items, 102L) // 1

        removeSong(items, 101L)

        assertEquals(1, items.size)
        assertEquals(102L, items[0].songId)
        assertEquals(0, items[0].position)
    }

    @Test
    fun removeSong_lastItem_noCompactionNeeded() {
        val items = mutableListOf<PlaylistItem>()
        addSong(items, 101L) // 0
        addSong(items, 102L) // 1

        removeSong(items, 102L)

        assertEquals(1, items.size)
        assertEquals(101L, items[0].songId)
        assertEquals(0, items[0].position)
    }

    @Test
    fun reorder_updatesPositionsCorrectly() {
        val items = mutableListOf<PlaylistItem>()
        addSong(items, 1L) // 0
        addSong(items, 2L) // 1
        addSong(items, 3L) // 2

        // Reverse order: 3, 2, 1
        reorder(items, listOf(3L, 2L, 1L))

        assertEquals(3L, items[0].songId)
        assertEquals(0, items[0].position)
        assertEquals(2L, items[1].songId)
        assertEquals(1, items[1].position)
        assertEquals(1L, items[2].songId)
        assertEquals(2, items[2].position)
    }
}
