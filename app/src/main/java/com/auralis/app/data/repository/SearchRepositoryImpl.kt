package com.auralis.app.data.repository

import com.auralis.app.data.local.db.dao.AlbumDao
import com.auralis.app.data.local.db.dao.ArtistDao
import com.auralis.app.data.local.db.dao.GenreDao
import com.auralis.app.data.local.db.dao.SearchHistoryDao
import com.auralis.app.data.local.db.dao.SongDao
import com.auralis.app.data.mapper.LibraryMapper
import com.auralis.app.data.mapper.SongMapper
import com.auralis.app.di.IoDispatcher
import com.auralis.app.domain.model.SearchResultGroup
import com.auralis.app.domain.model.SearchRankingEngine
import com.auralis.app.domain.model.SearchSuggestion
import com.auralis.app.domain.model.SearchSuggestionType
import com.auralis.app.domain.repository.SearchRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SearchRepositoryImpl @Inject constructor(
    private val searchHistoryDao: SearchHistoryDao,
    private val songDao: SongDao,
    private val albumDao: AlbumDao,
    private val artistDao: ArtistDao,
    private val genreDao: GenreDao,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : SearchRepository {

    override fun getRecentSearches(limit: Int): Flow<List<String>> {
        return searchHistoryDao.getRecentSearches(limit).map { list ->
            list.map { it.query }
        }
    }

    override suspend fun recordSearch(query: String) = withContext(ioDispatcher) {
        searchHistoryDao.recordSearch(query)
    }

    override suspend fun deleteSearch(query: String) = withContext(ioDispatcher) {
        searchHistoryDao.deleteSearchByQuery(query)
    }

    override suspend fun clearSearchHistory() = withContext(ioDispatcher) {
        searchHistoryDao.clearSearchHistory()
    }

    override suspend fun searchCategorized(query: String): SearchResultGroup = withContext(ioDispatcher) {
        val clean = query.trim()
        if (clean.isEmpty()) return@withContext SearchResultGroup(query = "")

        val songs = songDao.getAllSongsList().map { SongMapper.toDomain(it) }
        val albums = albumDao.getAllAlbums().first().map { LibraryMapper.toDomain(it) }
        val artists = artistDao.getAllArtists().first().map { LibraryMapper.toDomain(it) }
        val genres = genreDao.getAllGenres().first().map { LibraryMapper.toDomain(it) }

        val rankedSongs = SearchRankingEngine.rankSongs(songs, clean)
        val rankedArtists = SearchRankingEngine.rankArtists(artists, clean)
        val rankedAlbums = SearchRankingEngine.rankAlbums(albums, clean)
        val rankedGenres = SearchRankingEngine.rankGenres(genres, clean)

        SearchResultGroup(
            query = clean,
            songs = rankedSongs,
            artists = rankedArtists,
            albums = rankedAlbums,
            genres = rankedGenres
        )
    }

    override suspend fun getSearchSuggestions(query: String): List<SearchSuggestion> = withContext(ioDispatcher) {
        val clean = query.trim()
        val suggestions = mutableListOf<SearchSuggestion>()

        val recentHistory = searchHistoryDao.getRecentSearches(20).first()

        if (clean.isEmpty()) {
            // Return top recent searches when query is empty
            recentHistory.take(10).forEach { item ->
                suggestions.add(
                    SearchSuggestion(
                        text = item.query,
                        type = SearchSuggestionType.HISTORY,
                        subtitle = "Recent search"
                    )
                )
            }
            return@withContext suggestions
        }

        val qNorm = SearchRankingEngine.normalize(clean)

        // 1. History matches
        recentHistory
            .filter { SearchRankingEngine.normalize(it.query).contains(qNorm) }
            .take(3)
            .forEach { item ->
                suggestions.add(
                    SearchSuggestion(
                        text = item.query,
                        type = SearchSuggestionType.HISTORY,
                        subtitle = "Recent search"
                    )
                )
            }

        // 2. Artist matches
        val artists = artistDao.getAllArtists().first()
        artists
            .filter { SearchRankingEngine.normalize(it.name).contains(qNorm) }
            .take(3)
            .forEach { artist ->
                suggestions.add(
                    SearchSuggestion(
                        text = artist.name,
                        type = SearchSuggestionType.ARTIST,
                        subtitle = "Artist • ${artist.songCount} songs"
                    )
                )
            }

        // 3. Album matches
        val albums = albumDao.getAllAlbums().first()
        albums
            .filter { SearchRankingEngine.normalize(it.name).contains(qNorm) }
            .take(3)
            .forEach { album ->
                suggestions.add(
                    SearchSuggestion(
                        text = album.name,
                        type = SearchSuggestionType.ALBUM,
                        subtitle = "Album • ${album.artistName ?: "Unknown Artist"}"
                    )
                )
            }

        // 4. Song matches
        val songs = songDao.getAllSongsList()
        songs
            .filter { SearchRankingEngine.normalize(it.title).contains(qNorm) }
            .take(5)
            .forEach { song ->
                suggestions.add(
                    SearchSuggestion(
                        text = song.title,
                        type = SearchSuggestionType.SONG,
                        subtitle = "${song.artistName} • ${song.albumName}"
                    )
                )
            }

        // 5. Genre matches
        val genres = genreDao.getAllGenres().first()
        genres
            .filter { SearchRankingEngine.normalize(it.name).contains(qNorm) }
            .take(2)
            .forEach { genre ->
                suggestions.add(
                    SearchSuggestion(
                        text = genre.name,
                        type = SearchSuggestionType.GENRE,
                        subtitle = "Genre • ${genre.songCount} songs"
                    )
                )
            }

        suggestions.distinctBy { it.text.lowercase(Locale.ROOT) }
    }
}
