package com.auralis.app.domain.model

import java.util.Locale
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min

/**
 * Pure, deterministic, local recommendation engine.
 *
 * Implements weighted scoring, recency decay, play frequency normalization,
 * playlist co-occurrence, diversity enforcement, and explainable reason deduction.
 *
 * 100% on-device — no neural networks, no embeddings, no vector databases, no remote queries.
 */
object RecommendationEngine {

    /**
     * Normalizes genre names for consistent grouping (lowercased, trimmed).
     */
    fun normalizeGenre(genre: String?): String {
        return genre?.trim()?.lowercase(Locale.ROOT) ?: ""
    }

    /**
     * Evaluates a comprehensive relevance score for a single candidate [Song].
     */
    fun scoreCandidate(
        song: Song,
        profile: RecommendationProfile,
        currentTimeMillis: Long = System.currentTimeMillis()
    ): RecommendationItem {
        val breakdown = mutableMapOf<String, Float>()

        // 1. Artist Affinity (0.0 to 30.0)
        val maxArtistPlays = profile.artistPlayCounts.values.maxOrNull() ?: 1
        val artistPlays = profile.artistPlayCounts[song.artistName] ?: 0
        val artistPlayRatio = if (maxArtistPlays > 0) artistPlays.toFloat() / maxArtistPlays else 0f
        var artistScore = artistPlayRatio * 20.0f
        if (song.artistName in profile.favoriteArtistNames) {
            artistScore += 10.0f
        }
        artistScore = min(30.0f, artistScore)
        breakdown["artistAffinity"] = artistScore

        // 2. Genre Affinity (0.0 to 20.0)
        val normGenre = normalizeGenre(song.genreName)
        val maxGenrePlays = profile.genrePlayCounts.values.maxOrNull() ?: 1
        val genrePlays = profile.genrePlayCounts[normGenre] ?: 0
        val genrePlayRatio = if (maxGenrePlays > 0) genrePlays.toFloat() / maxGenrePlays else 0f
        val genreScore = min(20.0f, genrePlayRatio * 20.0f)
        breakdown["genreAffinity"] = genreScore

        // 3. Play Frequency with Logarithmic Bounded Normalization (0.0 to 15.0)
        val songPlays = profile.songPlayCounts[song.id] ?: 0
        val frequencyScore = min(15.0f, (5.0 * ln(1.0 + songPlays)).toFloat())
        breakdown["playFrequency"] = frequencyScore

        // 4. Recency with Deterministic Piecewise Decay (0.0 to 15.0)
        val lastPlayed = profile.recentSongTimestamps[song.id]
        val recencyScore = if (lastPlayed != null && lastPlayed > 0) {
            val deltaHours = max(0L, currentTimeMillis - lastPlayed) / (1000L * 3600L)
            when {
                deltaHours < 24 -> 15.0f
                deltaHours < 72 -> 12.0f
                deltaHours < 168 -> 9.0f
                deltaHours < 336 -> 6.0f
                deltaHours < 720 -> 3.0f
                else -> 0.0f
            }
        } else {
            0.0f
        }
        breakdown["recency"] = recencyScore

        // 5. Completion Signal (0.0 to 10.0)
        val completionRatio = profile.songCompletionRatios[song.id] ?: 0f
        val completionScore = completionRatio * 10.0f
        breakdown["completion"] = completionScore

        // 6. Favorites Boost (0.0 to 15.0)
        var favScore = 0.0f
        if (song.id in profile.favoriteSongIds) {
            favScore += 15.0f
        } else if (song.artistName in profile.favoriteArtistNames) {
            favScore += 8.0f
        }
        favScore = min(15.0f, favScore)
        breakdown["favorites"] = favScore

        // 7. Playlist Co-occurrence (0.0 to 10.0)
        val coOccurringSongs = profile.playlistSongPairs[song.id] ?: emptySet()
        val favOrTopCoOccurring = coOccurringSongs.count { it in profile.favoriteSongIds || (profile.songPlayCounts[it] ?: 0) >= 3 }
        val playlistScore = min(10.0f, favOrTopCoOccurring * 5.0f)
        breakdown["playlistCoOccurrence"] = playlistScore

        // 8. Discovery / Unplayed Boost (0.0 to 15.0)
        var discoveryScore = 0.0f
        if (song.id in profile.unplayedSongIds) {
            discoveryScore += 5.0f
            if (artistPlays > 0 || genrePlays > 0 || song.artistName in profile.favoriteArtistNames) {
                discoveryScore += 10.0f
            }
        }
        breakdown["discovery"] = discoveryScore

        // 9. Forgotten Favorite Boost (0.0 to 20.0)
        val forgottenScore = if (song.id in profile.forgottenFavoriteIds) 20.0f else 0.0f
        breakdown["forgottenFavorite"] = forgottenScore

        // 10. Repetition Penalty (-10.0 to 0.0)
        // If played in the last 6 hours, apply a repetition penalty so recommendations don't repeat the active queue
        val repetitionPenalty = if (lastPlayed != null && (currentTimeMillis - lastPlayed) < 6L * 3600L * 1000L) {
            -10.0f
        } else {
            0.0f
        }
        breakdown["repetitionPenalty"] = repetitionPenalty

        // 11. Audio Quality (Secondary nudge: +2.0 for Lossless)
        val isLossless = AudioFormatClassifier.isLossless(song.mimeType, song.contentUri)
        val qualityNudge = if (isLossless) 2.0f else 0.0f
        breakdown["audioQuality"] = qualityNudge

        // Compute Final Total
        val rawTotal = artistScore + genreScore + frequencyScore + recencyScore +
                completionScore + favScore + playlistScore + discoveryScore +
                forgottenScore + repetitionPenalty + qualityNudge
        val finalScore = max(0.0f, rawTotal)

        // Deduce Primary Explainable Reason
        val reason = deduceReason(song, breakdown, profile)

        return RecommendationItem(
            song = song,
            score = finalScore,
            reason = reason,
            scoreBreakdown = breakdown
        )
    }

    /**
     * Transparently derives the primary explainable reason corresponding to the highest weighted signal.
     */
    fun deduceReason(
        song: Song,
        breakdown: Map<String, Float>,
        profile: RecommendationProfile
    ): RecommendationReason {
        val forgotten = breakdown["forgottenFavorite"] ?: 0f
        val discovery = breakdown["discovery"] ?: 0f
        val artist = breakdown["artistAffinity"] ?: 0f
        val genre = breakdown["genreAffinity"] ?: 0f
        val fav = breakdown["favorites"] ?: 0f
        val playlist = breakdown["playlistCoOccurrence"] ?: 0f
        val freq = breakdown["playFrequency"] ?: 0f
        val recency = breakdown["recency"] ?: 0f

        return when {
            forgotten >= 15.0f -> RecommendationReason.ForgottenFavorite
            fav >= 15.0f -> RecommendationReason.FavoriteTrack
            artist >= 15.0f -> RecommendationReason.BecauseYouListenTo(song.artistName)
            genre >= 12.0f && !song.genreName.isNullOrBlank() -> RecommendationReason.BasedOnGenre(song.genreName)
            discovery >= 10.0f -> RecommendationReason.DiscoverNew
            fav >= 8.0f -> RecommendationReason.SimilarToFavorites
            playlist >= 5.0f -> RecommendationReason.PlaylistCoOccurrence
            freq >= 10.0f -> RecommendationReason.FrequentlyPlayed
            recency >= 9.0f -> RecommendationReason.RecentlyEnjoyed
            else -> RecommendationReason.ExploreLibrary
        }
    }

    /**
     * Enforces artist and album diversity on a candidate list to prevent mono-artist clusters.
     */
    fun enforceDiversity(
        items: List<RecommendationItem>,
        maxPerArtist: Int = 2,
        maxPerAlbum: Int = 2,
        targetCount: Int = 15
    ): List<RecommendationItem> {
        val result = mutableListOf<RecommendationItem>()
        val artistCounts = mutableMapOf<String, Int>()
        val albumCounts = mutableMapOf<String, Int>()

        for (item in items) {
            val artist = item.song.artistName.lowercase(Locale.ROOT)
            val album = item.song.albumName.lowercase(Locale.ROOT)

            val currentArtistCount = artistCounts[artist] ?: 0
            val currentAlbumCount = albumCounts[album] ?: 0

            if (currentArtistCount < maxPerArtist && currentAlbumCount < maxPerAlbum) {
                result.add(item)
                artistCounts[artist] = currentArtistCount + 1
                albumCounts[album] = currentAlbumCount + 1
            }

            if (result.size >= targetCount) break
        }

        return result
    }

    /**
     * Generates categorized recommendation sections from a library of songs and a user taste profile.
     */
    fun generateSections(
        allSongs: List<Song>,
        profile: RecommendationProfile,
        currentTimeMillis: Long = System.currentTimeMillis()
    ): List<RecommendationSection> {
        if (allSongs.isEmpty()) return emptyList()

        // 1. Cold Start Check: Less than 3 plays and 0 favorites
        val isColdStart = profile.totalPlays < 3 && profile.favoriteSongIds.isEmpty()

        if (isColdStart) {
            return generateColdStartSections(allSongs)
        }

        // Score all available songs deterministically
        val scoredItems = allSongs
            .map { scoreCandidate(it, profile, currentTimeMillis) }
            .sortedWith(
                compareByDescending<RecommendationItem> { it.score }
                    .thenBy { it.song.title.lowercase(Locale.ROOT) }
            )

        val sections = mutableListOf<RecommendationSection>()

        // 1. "Made For You" — Top general recommendations with diversity
        val madeForYou = enforceDiversity(scoredItems, maxPerArtist = 2, maxPerAlbum = 2, targetCount = 15)
        if (madeForYou.isNotEmpty()) {
            sections.add(
                RecommendationSection(
                    type = RecommendationSectionType.MADE_FOR_YOU,
                    title = "Made For You",
                    subtitle = "Personalized tracks based on your listening habits",
                    items = madeForYou
                )
            )
        }

        // 2. "Because You Listen To {TopArtist}"
        val topArtistEntry = profile.artistPlayCounts.entries.maxByOrNull { it.value }
        if (topArtistEntry != null && topArtistEntry.value >= 2) {
            val topArtistName = topArtistEntry.key
            val artistTracks = scoredItems.filter {
                it.song.artistName.equals(topArtistName, ignoreCase = true)
            }.take(10)

            if (artistTracks.isNotEmpty()) {
                sections.add(
                    RecommendationSection(
                        type = RecommendationSectionType.BECAUSE_YOU_LISTEN_TO,
                        title = "Because You Listen To $topArtistName",
                        subtitle = "More from your most played artist",
                        items = artistTracks
                    )
                )
            }
        }

        // 3. "Discover Something New" (Never played or low played matching taste)
        val unplayedCandidates = scoredItems.filter { it.song.id in profile.unplayedSongIds }.take(12)
        if (unplayedCandidates.isNotEmpty()) {
            sections.add(
                RecommendationSection(
                    type = RecommendationSectionType.DISCOVER_NEW,
                    title = "Discover Something New",
                    subtitle = "Unplayed tracks from your local collection matching your taste",
                    items = unplayedCandidates
                )
            )
        }

        // 4. "Forgotten Favorites"
        val forgottenItems = scoredItems.filter { it.song.id in profile.forgottenFavoriteIds }.take(10)
        if (forgottenItems.isNotEmpty()) {
            sections.add(
                RecommendationSection(
                    type = RecommendationSectionType.FORGOTTEN_FAVORITES,
                    title = "Forgotten Favorites",
                    subtitle = "Favorited tracks you haven't listened to in over 30 days",
                    items = forgottenItems
                )
            )
        }

        // 5. "From Your Playlists" (Co-occurrences)
        val playlistCandidates = scoredItems.filter { (it.scoreBreakdown["playlistCoOccurrence"] ?: 0f) > 0f }
        if (playlistCandidates.isNotEmpty()) {
            val distinctPlaylistItems = enforceDiversity(playlistCandidates, maxPerArtist = 3, targetCount = 10)
            sections.add(
                RecommendationSection(
                    type = RecommendationSectionType.FROM_PLAYLISTS,
                    title = "From Your Playlists",
                    subtitle = "Songs frequently grouped in your custom playlists",
                    items = distinctPlaylistItems
                )
            )
        }

        // 6. "Recently Enjoyed" (High completion recent tracks)
        val recentCandidates = scoredItems.filter {
            (it.scoreBreakdown["recency"] ?: 0f) >= 9.0f && (it.scoreBreakdown["completion"] ?: 0f) >= 5.0f
        }.take(10)
        if (recentCandidates.isNotEmpty()) {
            sections.add(
                RecommendationSection(
                    type = RecommendationSectionType.RECENTLY_ENJOYED,
                    title = "Recently Enjoyed",
                    subtitle = "Completed tracks from your recent sessions",
                    items = recentCandidates
                )
            )
        }

        return sections
    }

    /**
     * Fallback strategy when user has no listening history or favorites.
     * Uses honest, educational local discovery without synthetic personalization.
     */
    fun generateColdStartSections(allSongs: List<Song>): List<RecommendationSection> {
        val sections = mutableListOf<RecommendationSection>()

        // 1. Explore Your Library (Recently added & high quality)
        val recentlyAdded = allSongs
            .sortedByDescending { it.dateAdded }
            .take(15)
            .map { song ->
                RecommendationItem(
                    song = song,
                    score = 1.0f,
                    reason = RecommendationReason.ExploreLibrary
                )
            }

        if (recentlyAdded.isNotEmpty()) {
            sections.add(
                RecommendationSection(
                    type = RecommendationSectionType.EXPLORE_COLLECTION,
                    title = "Explore Your Library",
                    subtitle = "Recommendations adapt as you listen to music and favorite tracks",
                    items = recentlyAdded
                )
            )
        }

        // 2. High-Fidelity & Lossless discovery
        val losslessTracks = allSongs
            .filter { AudioFormatClassifier.isLossless(it.mimeType, it.contentUri) }
            .take(10)
            .map { song ->
                RecommendationItem(
                    song = song,
                    score = 1.0f,
                    reason = RecommendationReason.ExploreLibrary
                )
            }

        if (losslessTracks.isNotEmpty()) {
            sections.add(
                RecommendationSection(
                    type = RecommendationSectionType.DISCOVER_NEW,
                    title = "High-Quality Collection",
                    subtitle = "Lossless and high-bitrate audio from your device",
                    items = losslessTracks
                )
            )
        }

        return sections
    }
}
