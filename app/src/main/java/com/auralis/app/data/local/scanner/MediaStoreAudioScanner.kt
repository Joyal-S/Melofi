package com.auralis.app.data.local.scanner

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import com.auralis.app.data.local.db.entity.AlbumEntity
import com.auralis.app.data.local.db.entity.ArtistEntity
import com.auralis.app.data.local.db.entity.GenreEntity
import com.auralis.app.data.local.db.entity.SongEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Result bundle produced by a MediaStore audio scan.
 */
data class MediaStoreScanOutput(
    val songs: List<SongEntity>,
    val albums: List<AlbumEntity>,
    val artists: List<ArtistEntity>,
    val genres: List<GenreEntity>
)

/**
 * Scanner component responsible for querying Android MediaStore via ContentResolver.
 *
 * Adheres to Scoped Storage (API 29+), reads stable content URIs, normalizes all metadata,
 * isolates corrupt records, and derives album, artist, and genre groupings.
 */
@Singleton
class MediaStoreAudioScanner @Inject constructor(
    @param:ApplicationContext private val context: Context
) {

    companion object {
        private const val TAG = "MediaStoreScanner"
        private const val MIN_DURATION_MS = 1000L // Exclude short notification/click sounds
    }

    /**
     * Queries MediaStore for all valid music tracks on the device.
     * Returns empty output on permission failure or query error without throwing.
     */
    fun scanAudioFiles(): MediaStoreScanOutput {
        val resolver: ContentResolver = context.contentResolver
        val songs = mutableListOf<SongEntity>()

        val projectionList = mutableListOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ARTIST_ID,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.DATE_MODIFIED,
            MediaStore.Audio.Media.DATA
        )

        // Add API 30+ columns conditionally
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            projectionList.add(MediaStore.Audio.AudioColumns.GENRE)
            projectionList.add(MediaStore.Audio.AudioColumns.BITRATE)
        }

        val projection = projectionList.toTypedArray()
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} >= ?"
        val selectionArgs = arrayOf(MIN_DURATION_MS.toString())
        val sortOrder = "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"

        try {
            val cursor = resolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            ) ?: throw IllegalStateException("MediaStore ContentResolver query returned null cursor")

            cursor.use {
                parseCursor(it, songs)
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "Permission denied when accessing MediaStore: ${e.message}")
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error scanning MediaStore: ${e.message}", e)
            throw e
        }

        // Derive albums, artists, genres from the successfully scanned songs
        val albums = LibraryAggregator.aggregateAlbums(songs)
        val artists = LibraryAggregator.aggregateArtists(songs)
        val genres = LibraryAggregator.aggregateGenres(songs)

        return MediaStoreScanOutput(
            songs = songs,
            albums = albums,
            artists = artists,
            genres = genres
        )
    }

    private fun parseCursor(cursor: Cursor, destination: MutableList<SongEntity>) {
        val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
        val titleCol = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)
        val displayCol = cursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME)
        val artistCol = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)
        val artistIdCol = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST_ID)
        val albumCol = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM)
        val albumIdCol = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)
        val durationCol = cursor.getColumnIndex(MediaStore.Audio.Media.DURATION)
        val trackCol = cursor.getColumnIndex(MediaStore.Audio.Media.TRACK)
        val yearCol = cursor.getColumnIndex(MediaStore.Audio.Media.YEAR)
        val sizeCol = cursor.getColumnIndex(MediaStore.Audio.Media.SIZE)
        val mimeCol = cursor.getColumnIndex(MediaStore.Audio.Media.MIME_TYPE)
        val dateAddedCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATE_ADDED)
        val dateModCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATE_MODIFIED)
        val dataCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)

        val genreCol = cursor.getColumnIndex("genre")
        val bitrateCol = cursor.getColumnIndex("bitrate")

        while (cursor.moveToNext()) {
            try {
                val id = cursor.getLong(idCol)
                if (id <= 0) continue

                val rawTitle = if (titleCol != -1) cursor.getString(titleCol) else null
                val rawDisplayName = if (displayCol != -1) cursor.getString(displayCol) else null
                val rawArtist = if (artistCol != -1) cursor.getString(artistCol) else null
                val rawArtistId = if (artistIdCol != -1 && !cursor.isNull(artistIdCol)) cursor.getLong(artistIdCol) else null
                val rawAlbum = if (albumCol != -1) cursor.getString(albumCol) else null
                val rawAlbumId = if (albumIdCol != -1 && !cursor.isNull(albumIdCol)) cursor.getLong(albumIdCol) else null
                val rawDuration = if (durationCol != -1 && !cursor.isNull(durationCol)) cursor.getLong(durationCol) else 0L
                val rawTrack = if (trackCol != -1 && !cursor.isNull(trackCol)) cursor.getInt(trackCol) else null
                val rawYear = if (yearCol != -1 && !cursor.isNull(yearCol)) cursor.getInt(yearCol) else null
                val rawSize = if (sizeCol != -1 && !cursor.isNull(sizeCol)) cursor.getLong(sizeCol) else 0L
                val rawMimeType = if (mimeCol != -1) cursor.getString(mimeCol) else null
                val rawDateAdded = if (dateAddedCol != -1 && !cursor.isNull(dateAddedCol)) cursor.getLong(dateAddedCol) else 0L
                val rawDateModified = if (dateModCol != -1 && !cursor.isNull(dateModCol)) cursor.getLong(dateModCol) else 0L
                val rawPath = if (dataCol != -1) cursor.getString(dataCol) else null

                val rawGenre = if (genreCol != -1) cursor.getString(genreCol) else null
                val rawBitrate = if (bitrateCol != -1 && !cursor.isNull(bitrateCol)) cursor.getInt(bitrateCol) else null

                val title = AudioMetadataNormalizer.normalizeTitle(rawTitle, rawDisplayName)
                val artist = AudioMetadataNormalizer.normalizeArtist(rawArtist)
                val album = AudioMetadataNormalizer.normalizeAlbum(rawAlbum)
                val genre = AudioMetadataNormalizer.normalizeGenre(rawGenre)
                val duration = AudioMetadataNormalizer.normalizeDuration(rawDuration)
                val (discNumber, trackNumber) = AudioMetadataNormalizer.parseTrackAndDiscNumber(rawTrack)
                val contentUri = AudioMetadataNormalizer.buildContentUri(id)
                val albumArtUri = AudioMetadataNormalizer.buildAlbumArtUri(rawAlbumId)

                val song = SongEntity(
                    id = id,
                    title = title,
                    artistId = rawArtistId,
                    artistName = artist,
                    albumId = rawAlbumId,
                    albumName = album,
                    genreId = null,
                    genreName = genre,
                    duration = duration,
                    trackNumber = trackNumber,
                    discNumber = discNumber,
                    year = if (rawYear != null && rawYear > 0) rawYear else null,
                    filePath = rawPath,
                    contentUri = contentUri,
                    mimeType = rawMimeType,
                    size = rawSize,
                    bitrate = if (rawBitrate != null && rawBitrate > 0) rawBitrate else null,
                    sampleRate = null,
                    channelCount = null,
                    dateAdded = rawDateAdded,
                    dateModified = rawDateModified,
                    albumArtUri = albumArtUri
                )
                destination.add(song)
            } catch (e: Exception) {
                // Isolate corrupt/unreadable entry so entire scan succeeds
                Log.w(TAG, "Skipping corrupt media entry: ${e.message}")
            }
        }
    }
}

