package com.auralis.app

import com.auralis.app.data.local.db.entity.SearchHistoryEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests verifying search history management, deduplication, and capacity pruning.
 */
class SearchHistoryTest {

    private class FakeSearchHistoryTracker(private val maxEntries: Int = 20) {
        private val history = mutableListOf<SearchHistoryEntity>()
        private var nextId = 1L

        fun record(query: String, timestamp: Long = System.currentTimeMillis()) {
            val clean = query.trim()
            if (clean.isEmpty()) return

            val existingIndex = history.indexOfFirst { it.query.equals(clean, ignoreCase = true) }
            if (existingIndex >= 0) {
                val existing = history.removeAt(existingIndex)
                history.add(0, existing.copy(searchedAt = timestamp))
            } else {
                history.add(0, SearchHistoryEntity(id = nextId++, query = clean, searchedAt = timestamp))
            }

            // Prune to maxEntries
            while (history.size > maxEntries) {
                history.removeAt(history.lastIndex)
            }
        }

        fun getRecent(limit: Int = 20): List<String> {
            return history.take(limit).map { it.query }
        }

        fun delete(query: String) {
            history.removeAll { it.query.equals(query, ignoreCase = true) }
        }

        fun clear() {
            history.clear()
        }
    }

    @Test
    fun recordSearch_emptyOrBlank_isIgnored() {
        val tracker = FakeSearchHistoryTracker()
        tracker.record("")
        tracker.record("   ")
        assertTrue(tracker.getRecent().isEmpty())
    }

    @Test
    fun recordSearch_addsToRecentListInOrder() {
        val tracker = FakeSearchHistoryTracker()
        tracker.record("Beatles")
        tracker.record("Queen")
        tracker.record("Led Zeppelin")

        val recent = tracker.getRecent()
        assertEquals(listOf("Led Zeppelin", "Queen", "Beatles"), recent)
    }

    @Test
    fun recordSearch_deduplicatesAndUpdatesRecency() {
        val tracker = FakeSearchHistoryTracker()
        tracker.record("Queen")
        tracker.record("Beatles")
        tracker.record("Queen") // Repeat

        val recent = tracker.getRecent()
        assertEquals(2, recent.size)
        assertEquals("Queen", recent[0])
        assertEquals("Beatles", recent[1])
    }

    @Test
    fun recordSearch_prunesAtCapacityLimitOf20() {
        val tracker = FakeSearchHistoryTracker(maxEntries = 20)
        for (i in 1..25) {
            tracker.record("Query $i")
        }

        val recent = tracker.getRecent(50)
        assertEquals(20, recent.size)
        assertEquals("Query 25", recent.first())
        assertEquals("Query 6", recent.last())
    }

    @Test
    fun deleteAndClear_removesQueries() {
        val tracker = FakeSearchHistoryTracker()
        tracker.record("Queen")
        tracker.record("Beatles")

        tracker.delete("Queen")
        assertEquals(listOf("Beatles"), tracker.getRecent())

        tracker.clear()
        assertTrue(tracker.getRecent().isEmpty())
    }
}
