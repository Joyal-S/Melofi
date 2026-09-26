package com.auralis.app.domain.repository

import com.auralis.app.domain.model.RecommendationItem
import com.auralis.app.domain.model.RecommendationSection
import com.auralis.app.domain.model.RecommendationSectionType
import com.auralis.app.domain.model.Song
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for obtaining explainable, on-device music recommendations.
 * 100% offline — operates exclusively on local listening data and device audio files.
 */
interface RecommendationRepository {
    fun getRecommendationSections(): Flow<List<RecommendationSection>>
    fun getSectionDetail(type: RecommendationSectionType): Flow<RecommendationSection?>
    suspend fun refreshRecommendations()

    /**
     * Intelligently selects the single best next-track recommendation candidate using
     * local listening history signals, cold-start exploration, anti-repetition rules,
     * and artist diversity constraints.
     */
    suspend fun getNextTrackRecommendation(
        currentSong: Song?,
        excludedSongIds: Set<Long> = emptySet()
    ): RecommendationItem?
}
