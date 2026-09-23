package com.auralis.app.data.repository

import com.auralis.app.data.local.db.dao.FavoriteDao
import com.auralis.app.data.local.db.dao.HistoryDao
import com.auralis.app.data.local.db.dao.PlaylistDao
import com.auralis.app.data.local.db.dao.SongDao
import com.auralis.app.data.mapper.SongMapper
import com.auralis.app.di.IoDispatcher
import com.auralis.app.domain.model.RecommendationEngine
import com.auralis.app.domain.model.RecommendationProfile
import com.auralis.app.domain.model.RecommendationSection
import com.auralis.app.domain.model.RecommendationSectionType
import com.auralis.app.domain.repository.RecommendationRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Production implementation of [RecommendationRepository].
 * Derives temporary taste profiles and evaluates recommendation sections on-demand.
 * 100% on-device, offline, and private.
 */
@Singleton
class RecommendationRepositoryImpl @Inject constructor(
    private val songDao: SongDao,
    private val historyDao: HistoryDao,
    private val favoriteDao: FavoriteDao,
    private val playlistDao: PlaylistDao,
    private val localArtworkRepository: LocalArtworkRepository,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : RecommendationRepository {

    private val _refreshTrigger = MutableStateFlow(0L)

    private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)

    private val sharedSectionsFlow: Flow<List<RecommendationSection>> = combine(
        songDao.getSongsSortedByTitle(),
        favoriteDao.getFavoriteSongIds(),
        historyDao.getRecentHistoryEntries(limit = 1000),
        _refreshTrigger
    ) { songEntities, favIds, historyEntries, _ ->
        val domainSongs = songEntities.map { entity ->
            val domain = SongMapper.toDomain(entity)
            val art = localArtworkRepository.resolveSongArtworkFast(domain)
            domain.copy(albumArtUri = art)
        }
        if (domainSongs.isEmpty()) {
            return@combine emptyList()
        }

        val songsById = domainSongs.associateBy { it.id }

        // 1. Build Song Play Counts and Completion Ratios
        val songPlayCounts = mutableMapOf<Long, Int>()
        val songCompletedCounts = mutableMapOf<Long, Int>()
        val recentTimestamps = mutableMapOf<Long, Long>()

        for (entry in historyEntries) {
            songPlayCounts[entry.songId] = (songPlayCounts[entry.songId] ?: 0) + 1
            if (entry.completed) {
                songCompletedCounts[entry.songId] = (songCompletedCounts[entry.songId] ?: 0) + 1
            }
            val currentMax = recentTimestamps[entry.songId] ?: 0L
            if (entry.playedAt > currentMax) {
                recentTimestamps[entry.songId] = entry.playedAt
            }
        }

        val completionRatios = songPlayCounts.mapValues { (songId, total) ->
            val completed = songCompletedCounts[songId] ?: 0
            if (total > 0) completed.toFloat() / total.toFloat() else 0f
        }

        // 2. Build Artist and Genre Play Counts
        val artistPlayCounts = mutableMapOf<String, Int>()
        val genrePlayCounts = mutableMapOf<String, Int>()

        for (entry in historyEntries) {
            val song = songsById[entry.songId] ?: continue
            artistPlayCounts[song.artistName] = (artistPlayCounts[song.artistName] ?: 0) + 1
            val normGenre = RecommendationEngine.normalizeGenre(song.genreName)
            if (normGenre.isNotEmpty()) {
                genrePlayCounts[normGenre] = (genrePlayCounts[normGenre] ?: 0) + 1
            }
        }

        // 3. Favorites and Favorite Artists
        val favSet = favIds.toSet()
        val favoriteArtistNames = domainSongs
            .filter { it.id in favSet }
            .map { it.artistName }
            .toSet()

        // 4. Forgotten Favorites (favorited, not played in >30 days)
        val thirtyDaysAgo = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000L
        val forgottenFavIds = favSet.filter { id ->
            val lastPlayed = recentTimestamps[id]
            lastPlayed == null || lastPlayed < thirtyDaysAgo
        }.toSet()

        // 5. Unplayed Songs
        val unplayedIds = domainSongs
            .filter { (songPlayCounts[it.id] ?: 0) == 0 }
            .map { it.id }
            .toSet()

        // 6. Playlist Co-occurrence Graph
        val playlistCrossRefs = runCatching { playlistDao.getAllPlaylistSongCrossRefs() }.getOrDefault(emptyList())
        val playlistToSongs = playlistCrossRefs.groupBy({ it.playlistId }, { it.songId })
        val songPairs = mutableMapOf<Long, MutableSet<Long>>()
        for ((_, memberIds) in playlistToSongs) {
            val memberSet = memberIds.toSet()
            for (id in memberSet) {
                songPairs.getOrPut(id) { mutableSetOf() }.addAll(memberSet - id)
            }
        }

        val profile = RecommendationProfile(
            artistPlayCounts = artistPlayCounts,
            genrePlayCounts = genrePlayCounts,
            songPlayCounts = songPlayCounts,
            songCompletionRatios = completionRatios,
            recentSongTimestamps = recentTimestamps,
            favoriteSongIds = favSet,
            favoriteArtistNames = favoriteArtistNames,
            forgottenFavoriteIds = forgottenFavIds,
            playlistSongPairs = songPairs,
            unplayedSongIds = unplayedIds,
            totalPlays = historyEntries.size
        )

        RecommendationEngine.generateSections(
            allSongs = domainSongs,
            profile = profile,
            currentTimeMillis = System.currentTimeMillis()
        )
    }
        .flowOn(ioDispatcher)
        .shareIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
            replay = 1
        )

    override fun getRecommendationSections(): Flow<List<RecommendationSection>> {
        return sharedSectionsFlow
    }

    override fun getSectionDetail(type: RecommendationSectionType): Flow<RecommendationSection?> {
        return sharedSectionsFlow.map { sections ->
            sections.find { it.type == type }
        }
    }

    override suspend fun refreshRecommendations() = withContext(ioDispatcher) {
        _refreshTrigger.value = System.currentTimeMillis()
    }
}
