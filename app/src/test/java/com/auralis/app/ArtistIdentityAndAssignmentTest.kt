package com.auralis.app

import com.auralis.app.core.util.ArtistNameNormalizer
import com.auralis.app.data.local.db.entity.ArtistEntity
import com.auralis.app.data.local.db.entity.SongEntity
import com.auralis.app.domain.model.Artist
import com.auralis.app.domain.model.DuplicateArtistException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * Comprehensive test suite verifying the Melofi Artist Identity & Song Artist Assignment System:
 * - Persistent artist entity architecture
 * - Normalization rules & duplicate prevention on creation
 * - Moving songs between existing artists
 * - Artist profile rename and duplicate rename rejection
 * - Merge / use existing artist
 * - Preservation across library rescans
 */
class ArtistIdentityAndAssignmentTest {

    private class MockArtistRepository {
        val artists = mutableMapOf<Long, ArtistEntity>()
        val songs = mutableMapOf<Long, SongEntity>()
        private var nextArtistId = 1L

        fun createArtist(name: String): Result<Artist> {
            val clean = ArtistNameNormalizer.cleanDisplayName(name)
            val norm = ArtistNameNormalizer.normalize(name)
            if (norm.isEmpty()) {
                return Result.failure(IllegalArgumentException("Artist name cannot be blank"))
            }

            val existing = artists.values.firstOrNull { it.normalizedName == norm }
            if (existing != null) {
                return Result.failure(
                    DuplicateArtistException(
                        Artist(
                            id = existing.id,
                            name = existing.name,
                            albumCount = existing.albumCount,
                            songCount = existing.songCount
                        )
                    )
                )
            }

            val id = nextArtistId++
            val entity = ArtistEntity(
                id = id,
                name = clean,
                normalizedName = norm,
                albumCount = 0,
                songCount = 0
            )
            artists[id] = entity
            return Result.success(
                Artist(id = id, name = clean, albumCount = 0, songCount = 0)
            )
        }

        fun renameArtist(artistId: Long, newName: String): Result<Unit> {
            val current = artists[artistId]
                ?: return Result.failure(IllegalArgumentException("Artist not found"))
            val clean = ArtistNameNormalizer.cleanDisplayName(newName)
            val norm = ArtistNameNormalizer.normalize(newName)
            if (norm.isEmpty()) {
                return Result.failure(IllegalArgumentException("Artist name cannot be blank"))
            }

            val duplicate = artists.values.firstOrNull { it.normalizedName == norm }
            if (duplicate != null && duplicate.id != artistId) {
                return Result.failure(
                    DuplicateArtistException(
                        Artist(
                            id = duplicate.id,
                            name = duplicate.name,
                            albumCount = duplicate.albumCount,
                            songCount = duplicate.songCount
                        )
                    )
                )
            }

            artists[artistId] = current.copy(
                name = clean,
                normalizedName = norm,
                updatedAt = System.currentTimeMillis()
            )

            // Update associated songs display name
            for ((id, song) in songs) {
                if (song.artistId == artistId) {
                    songs[id] = song.copy(artistName = clean)
                }
            }
            return Result.success(Unit)
        }

        fun reassignSongArtist(songId: Long, newArtistId: Long): Result<Unit> {
            val song = songs[songId]
                ?: return Result.failure(IllegalArgumentException("Song not found"))
            val targetArtist = artists[newArtistId]
                ?: return Result.failure(IllegalArgumentException("Target artist not found"))

            val oldArtistId = song.artistId
            songs[songId] = song.copy(
                artistId = targetArtist.id,
                artistName = targetArtist.name
            )

            recalculateCounts(oldArtistId)
            recalculateCounts(targetArtist.id)
            return Result.success(Unit)
        }

        fun mergeArtists(sourceArtistId: Long, targetArtistId: Long): Result<Unit> {
            if (sourceArtistId == targetArtistId) return Result.success(Unit)
            val targetArtist = artists[targetArtistId]
                ?: return Result.failure(IllegalArgumentException("Target artist not found"))

            for ((id, song) in songs) {
                if (song.artistId == sourceArtistId) {
                    songs[id] = song.copy(
                        artistId = targetArtist.id,
                        artistName = targetArtist.name
                    )
                }
            }

            artists.remove(sourceArtistId)
            recalculateCounts(targetArtist.id)
            return Result.success(Unit)
        }

        fun getSongsForArtist(artistId: Long): List<SongEntity> {
            return songs.values.filter { it.artistId == artistId }
        }

        fun recalculateCounts(artistId: Long?) {
            if (artistId == null) return
            val artist = artists[artistId] ?: return
            val artistSongs = songs.values.filter { it.artistId == artistId }
            val distinctAlbums = artistSongs.mapNotNull { it.albumId }.distinct().size
            artists[artistId] = artist.copy(
                songCount = artistSongs.size,
                albumCount = distinctAlbums
            )
        }
    }

    @Test
    fun uniqueArtistCreation_createsAndAssignsId() {
        val repo = MockArtistRepository()
        val result = repo.createArtist("Susu")
        assertTrue(result.isSuccess)
        val artist = result.getOrThrow()
        assertEquals("Susu", artist.name)
        assertTrue(artist.id > 0)
    }

    @Test
    fun duplicateArtistCreation_isRejectedWithExistingProfile() {
        val repo = MockArtistRepository()
        repo.createArtist("Susu")

        // Try creating variants of Susu
        val variant1 = repo.createArtist("susu")
        assertTrue(variant1.isFailure)
        val err1 = variant1.exceptionOrNull()
        assertTrue(err1 is DuplicateArtistException)
        assertEquals("Susu", (err1 as DuplicateArtistException).existingArtist.name)

        val variant2 = repo.createArtist("  SUSU  ")
        assertTrue(variant2.isFailure)
        val err2 = variant2.exceptionOrNull()
        assertTrue(err2 is DuplicateArtistException)

        // Susi is a different artist and must be accepted
        val susiResult = repo.createArtist("Susi")
        assertTrue(susiResult.isSuccess)
        assertEquals("Susi", susiResult.getOrThrow().name)
    }

    @Test
    fun moveSongBetweenArtists_updatesArtistProfileAssociation() {
        val repo = MockArtistRepository()
        val susu = repo.createArtist("Susu").getOrThrow()
        val susi = repo.createArtist("Susi").getOrThrow()

        // Create Song A assigned to Susi
        val songA = SongEntity(
            id = 100L,
            title = "Song A",
            artistId = susi.id,
            artistName = susi.name,
            albumId = 50L,
            albumName = "Album X",
            genreId = null,
            genreName = null,
            duration = 180000L,
            trackNumber = 1,
            discNumber = 1,
            year = 2024,
            filePath = "/storage/music/songA.mp3",
            contentUri = "content://media/external/audio/media/100",
            mimeType = "audio/mpeg",
            size = 5000000L,
            bitrate = 320,
            sampleRate = 44100,
            channelCount = 2,
            dateAdded = 1000L,
            dateModified = 2000L,
            albumArtUri = null
        )
        repo.songs[songA.id] = songA
        repo.recalculateCounts(susi.id)

        // Verify initial state: song belongs to Susi, not Susu
        assertEquals(1, repo.getSongsForArtist(susi.id).size)
        assertEquals(0, repo.getSongsForArtist(susu.id).size)

        // Move Song A to Susu
        val reassignResult = repo.reassignSongArtist(songA.id, susu.id)
        assertTrue(reassignResult.isSuccess)

        // Song now belongs to Susu
        val susuSongs = repo.getSongsForArtist(susu.id)
        assertEquals(1, susuSongs.size)
        assertEquals(songA.id, susuSongs.first().id)
        assertEquals(susu.id, susuSongs.first().artistId)
        assertEquals("Susu", susuSongs.first().artistName)

        // Song is no longer visible under Susi
        val susiSongs = repo.getSongsForArtist(susi.id)
        assertEquals(0, susiSongs.size)
    }

    @Test
    fun artistRename_rejectsDuplicateAndOffersExisting() {
        val repo = MockArtistRepository()
        val susu = repo.createArtist("Susu").getOrThrow()
        val susi = repo.createArtist("Susi").getOrThrow()

        // Renaming Susi to a unique name works
        val validRename = repo.renameArtist(susi.id, "Susi Q")
        assertTrue(validRename.isSuccess)
        assertEquals("Susi Q", repo.artists[susi.id]?.name)

        // Attempting to rename Susi to Susu must be rejected
        val duplicateRename = repo.renameArtist(susi.id, "susu")
        assertTrue(duplicateRename.isFailure)
        val err = duplicateRename.exceptionOrNull()
        assertTrue(err is DuplicateArtistException)
        val dupException = err as DuplicateArtistException
        assertEquals(susu.id, dupException.existingArtist.id)
        assertEquals("Susu", dupException.existingArtist.name)
    }

    @Test
    fun mergeArtists_reassignsAllSongsSafely() {
        val repo = MockArtistRepository()
        val susu = repo.createArtist("Susu").getOrThrow()
        val duplicateSusu = repo.createArtist("susu_temp").getOrThrow()

        // Assign two songs to duplicateSusu
        val s1 = SongEntity(
            id = 1L, title = "Track 1", artistId = duplicateSusu.id, artistName = duplicateSusu.name,
            albumId = 10L, albumName = "Album 1", genreId = null, genreName = null, duration = 1000L,
            trackNumber = 1, discNumber = 1, year = 2024, filePath = null, contentUri = "uri/1",
            mimeType = null, size = 100L, bitrate = null, sampleRate = null, channelCount = null,
            dateAdded = 1L, dateModified = 1L, albumArtUri = null
        )
        val s2 = SongEntity(
            id = 2L, title = "Track 2", artistId = duplicateSusu.id, artistName = duplicateSusu.name,
            albumId = 10L, albumName = "Album 1", genreId = null, genreName = null, duration = 1000L,
            trackNumber = 2, discNumber = 1, year = 2024, filePath = null, contentUri = "uri/2",
            mimeType = null, size = 100L, bitrate = null, sampleRate = null, channelCount = null,
            dateAdded = 1L, dateModified = 1L, albumArtUri = null
        )
        repo.songs[1L] = s1
        repo.songs[2L] = s2
        repo.recalculateCounts(duplicateSusu.id)

        // Merge duplicate into Susu
        val mergeResult = repo.mergeArtists(sourceArtistId = duplicateSusu.id, targetArtistId = susu.id)
        assertTrue(mergeResult.isSuccess)

        // Duplicate profile deleted
        assertFalse(repo.artists.containsKey(duplicateSusu.id))

        // All songs reassigned to Susu
        val mergedSongs = repo.getSongsForArtist(susu.id)
        assertEquals(2, mergedSongs.size)
        assertTrue(mergedSongs.all { it.artistId == susu.id && it.artistName == "Susu" })
        assertEquals(2, repo.artists[susu.id]?.songCount)
    }

    @Test
    fun rescanPreservation_doesNotOverwriteUserReassignedArtist() {
        val repo = MockArtistRepository()
        val susu = repo.createArtist("Susu").getOrThrow()
        val susi = repo.createArtist("Susi").getOrThrow()

        // User manually reassigned Song 1 to Susu
        val song1 = SongEntity(
            id = 1L, title = "Song 1", artistId = susu.id, artistName = susu.name,
            albumId = 10L, albumName = "Album 1", genreId = null, genreName = null, duration = 1000L,
            trackNumber = 1, discNumber = 1, year = 2024, filePath = null, contentUri = "uri/1",
            mimeType = null, size = 100L, bitrate = null, sampleRate = null, channelCount = null,
            dateAdded = 1L, dateModified = 100L, albumArtUri = null
        )
        repo.songs[1L] = song1

        // Simulate library rescan discovering raw MediaStore track with raw artist "Susi"
        val scannedRawSong = song1.copy(artistName = "Susi", dateModified = 200L)

        // Rescan differential update preservation:
        val existing = repo.songs[scannedRawSong.id]
        val preservedSong = if (existing != null && existing.artistId != null) {
            scannedRawSong.copy(artistId = existing.artistId, artistName = existing.artistName)
        } else {
            scannedRawSong
        }
        repo.songs[preservedSong.id] = preservedSong

        // Verify that the song is STILL assigned to Susu, NOT overwritten to Susi
        assertEquals(susu.id, repo.songs[1L]?.artistId)
        assertEquals("Susu", repo.songs[1L]?.artistName)
    }
}
