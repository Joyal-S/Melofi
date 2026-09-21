package com.auralis.app

import com.auralis.app.domain.model.ScanResult
import com.auralis.app.domain.model.Song
import com.auralis.app.domain.model.SongSortOrder
import com.auralis.app.domain.repository.SongRepository
import com.auralis.app.domain.usecase.GetSongsUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for [GetSongsUseCase].
 * Verifies repository delegation and sort order propagation.
 */
class GetSongsUseCaseTest {

    private class FakeSongRepository : SongRepository {
        var requestedSortOrder: SongSortOrder? = null

        override fun getSongs(sortOrder: SongSortOrder): Flow<List<Song>> {
            requestedSortOrder = sortOrder
            return flowOf(
                listOf(
                    Song(
                        id = 1L,
                        title = "Sample $sortOrder",
                        artistId = null,
                        artistName = "Artist",
                        albumId = null,
                        albumName = "Album",
                        genreId = null,
                        genreName = null,
                        duration = 200000L,
                        trackNumber = 1,
                        discNumber = null,
                        year = 2024,
                        contentUri = "content://media/external/audio/media/1",
                        mimeType = "audio/mp3",
                        size = 5000000L,
                        bitrate = 320,
                        sampleRate = 44100,
                        channelCount = 2,
                        dateAdded = 1000L,
                        dateModified = 1000L,
                        albumArtUri = null
                    )
                )
            )
        }

        override fun getSongById(id: Long): Flow<Song?> = flowOf(null)

        override suspend fun scanLibrary(): Result<ScanResult> =
            Result.success(ScanResult(1, 1, 0, 0))

        override fun getSongDetails(id: Long): Flow<com.auralis.app.domain.model.SongDetails?> = flowOf(null)

        override suspend fun updateSongMetadata(
            songId: Long,
            title: String,
            artist: String,
            album: String,
            albumArtist: String?,
            genre: String?,
            composer: String?,
            year: Int?,
            trackNumber: Int?,
            discNumber: Int?
        ): Result<Unit> = Result.success(Unit)

        override suspend fun findDuplicates(): List<com.auralis.app.domain.model.DuplicateGroup> = emptyList()

        override suspend fun deleteSong(songId: Long): Result<Unit> = Result.success(Unit)
    }

    @Test
    fun getSongsUseCase_passesSortOrderCorrectly() = runBlocking {
        val fakeRepo = FakeSongRepository()
        val useCase = GetSongsUseCase(fakeRepo)

        val songsTitle = useCase(SongSortOrder.TITLE).first()
        assertEquals(SongSortOrder.TITLE, fakeRepo.requestedSortOrder)
        assertEquals("Sample TITLE", songsTitle.first().title)

        val songsArtist = useCase(SongSortOrder.ARTIST).first()
        assertEquals(SongSortOrder.ARTIST, fakeRepo.requestedSortOrder)
        assertEquals("Sample ARTIST", songsArtist.first().title)

        val songsAlbum = useCase(SongSortOrder.ALBUM).first()
        assertEquals(SongSortOrder.ALBUM, fakeRepo.requestedSortOrder)
        assertEquals("Sample ALBUM", songsAlbum.first().title)

        val songsDate = useCase(SongSortOrder.DATE_ADDED).first()
        assertEquals(SongSortOrder.DATE_ADDED, fakeRepo.requestedSortOrder)
        assertEquals("Sample DATE_ADDED", songsDate.first().title)
    }
}
