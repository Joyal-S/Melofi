package com.auralis.app

import com.auralis.app.data.local.db.entity.SongEntity
import com.auralis.app.data.local.scanner.LibrarySyncDiff
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [LibrarySyncDiff].
 * Verifies duplicate prevention, detection of added/updated/deleted tracks,
 * and resilience against rescans with identical data.
 */
class LibrarySyncDiffTest {

    private fun createSampleSong(id: Long, dateModified: Long): SongEntity {
        return SongEntity(
            id = id,
            title = "Track $id",
            artistId = 100L,
            artistName = "Artist",
            albumId = 200L,
            albumName = "Album",
            genreId = null,
            genreName = null,
            duration = 180000L,
            trackNumber = id.toInt(),
            discNumber = null,
            year = 2024,
            filePath = "/storage/emulated/0/Music/track_$id.mp3",
            contentUri = "content://media/external/audio/media/$id",
            mimeType = "audio/mpeg",
            size = 5000000L,
            bitrate = 320,
            sampleRate = 44100,
            channelCount = 2,
            dateAdded = 1700000000L,
            dateModified = dateModified,
            albumArtUri = "content://media/external/audio/albumart/200"
        )
    }

    @Test
    fun computeDiff_firstScan_allSongsInserted() {
        val existingMap = emptyMap<Long, Long>()
        val scanned = listOf(
            createSampleSong(id = 1L, dateModified = 1000L),
            createSampleSong(id = 2L, dateModified = 1000L)
        )

        val diff = LibrarySyncDiff.computeDiff(existingMap, scanned)

        assertEquals(2, diff.toInsert.size)
        assertTrue(diff.toUpdate.isEmpty())
        assertTrue(diff.toDeleteIds.isEmpty())
        assertEquals(0, diff.unchangedCount)
    }

    @Test
    fun computeDiff_secondScanNoChanges_zeroDuplicatesAndAllUnchanged() {
        // Database already has songs 1 and 2 with dateModified = 1000L
        val existingMap = mapOf(1L to 1000L, 2L to 1000L)
        val scanned = listOf(
            createSampleSong(id = 1L, dateModified = 1000L),
            createSampleSong(id = 2L, dateModified = 1000L)
        )

        val diff = LibrarySyncDiff.computeDiff(existingMap, scanned)

        // Rescan must NOT insert duplicates
        assertTrue("Expected 0 inserts on rescan", diff.toInsert.isEmpty())
        assertTrue("Expected 0 updates on unchanged files", diff.toUpdate.isEmpty())
        assertTrue("Expected 0 deletions", diff.toDeleteIds.isEmpty())
        assertEquals(2, diff.unchangedCount)
    }

    @Test
    fun computeDiff_newSongAdded_onlyNewSongInserted() {
        val existingMap = mapOf(1L to 1000L)
        val scanned = listOf(
            createSampleSong(id = 1L, dateModified = 1000L),
            createSampleSong(id = 2L, dateModified = 1200L)
        )

        val diff = LibrarySyncDiff.computeDiff(existingMap, scanned)

        assertEquals(1, diff.toInsert.size)
        assertEquals(2L, diff.toInsert.first().id)
        assertTrue(diff.toUpdate.isEmpty())
        assertTrue(diff.toDeleteIds.isEmpty())
        assertEquals(1, diff.unchangedCount)
    }

    @Test
    fun computeDiff_songDeletedFromDevice_detectedForDeletion() {
        // Song 2 was deleted from device storage
        val existingMap = mapOf(1L to 1000L, 2L to 1000L)
        val scanned = listOf(
            createSampleSong(id = 1L, dateModified = 1000L)
        )

        val diff = LibrarySyncDiff.computeDiff(existingMap, scanned)

        assertTrue(diff.toInsert.isEmpty())
        assertTrue(diff.toUpdate.isEmpty())
        assertEquals(listOf(2L), diff.toDeleteIds)
        assertEquals(1, diff.unchangedCount)
    }

    @Test
    fun computeDiff_songMetadataModified_detectedForUpdate() {
        // Song 1 was tagged or modified on device (newer dateModified)
        val existingMap = mapOf(1L to 1000L)
        val scanned = listOf(
            createSampleSong(id = 1L, dateModified = 1500L)
        )

        val diff = LibrarySyncDiff.computeDiff(existingMap, scanned)

        assertTrue(diff.toInsert.isEmpty())
        assertEquals(1, diff.toUpdate.size)
        assertEquals(1L, diff.toUpdate.first().id)
        assertEquals(1500L, diff.toUpdate.first().dateModified)
        assertTrue(diff.toDeleteIds.isEmpty())
        assertEquals(0, diff.unchangedCount)
    }

    @Test
    fun computeDiff_duplicatePhysicalFiles_bothIndexedIndependentlyByUniqueId() {
        // Two physical files with identical audio tags (e.g. copies in different folders)
        // MediaStore assigns them distinct IDs (1L and 2L)
        val existingMap = emptyMap<Long, Long>()
        val scanned = listOf(
            createSampleSong(id = 1L, dateModified = 1000L).copy(title = "Same Title", artistName = "Same Artist"),
            createSampleSong(id = 2L, dateModified = 1000L).copy(title = "Same Title", artistName = "Same Artist")
        )

        val diff = LibrarySyncDiff.computeDiff(existingMap, scanned)

        assertEquals(2, diff.toInsert.size)
        assertEquals(1L, diff.toInsert[0].id)
        assertEquals(2L, diff.toInsert[1].id)
        assertEquals("content://media/external/audio/media/1", diff.toInsert[0].contentUri)
        assertEquals("content://media/external/audio/media/2", diff.toInsert[1].contentUri)
    }

    @Test
    fun computeDiff_renamedFile_detectedAsUpdatePreservingId() {
        // Renamed file on same volume retains MediaStore ID, bumps dateModified
        val existingMap = mapOf(1L to 1000L)
        val scanned = listOf(
            createSampleSong(id = 1L, dateModified = 1100L).copy(title = "Renamed Track Title")
        )

        val diff = LibrarySyncDiff.computeDiff(existingMap, scanned)

        assertTrue(diff.toInsert.isEmpty())
        assertEquals(1, diff.toUpdate.size)
        assertEquals(1L, diff.toUpdate.first().id)
        assertEquals("Renamed Track Title", diff.toUpdate.first().title)
        assertTrue(diff.toDeleteIds.isEmpty())
    }

    @Test
    fun computeDiff_replacedPhysicalFile_detectedAsUpdateByDateModified() {
        // In-place file replacement updates dateModified and size
        val existingMap = mapOf(1L to 1000L)
        val scanned = listOf(
            createSampleSong(id = 1L, dateModified = 2000L).copy(size = 8000000L, bitrate = 320)
        )

        val diff = LibrarySyncDiff.computeDiff(existingMap, scanned)

        assertTrue(diff.toInsert.isEmpty())
        assertEquals(1, diff.toUpdate.size)
        assertEquals(1L, diff.toUpdate.first().id)
        assertEquals(8000000L, diff.toUpdate.first().size)
    }

    @Test
    fun computeDiff_emptyScanFromClearedStorage_deletesAllSongs() {
        val existingMap = mapOf(1L to 1000L, 2L to 1000L, 3L to 1000L)
        val scanned = emptyList<SongEntity>()

        val diff = LibrarySyncDiff.computeDiff(existingMap, scanned)

        assertTrue(diff.toInsert.isEmpty())
        assertTrue(diff.toUpdate.isEmpty())
        assertEquals(3, diff.toDeleteIds.size)
        assertEquals(0, diff.unchangedCount)
    }
}
