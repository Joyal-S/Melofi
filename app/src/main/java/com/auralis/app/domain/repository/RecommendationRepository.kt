package com.auralis.app.domain.repository

import com.auralis.app.domain.model.RecommendationSection
import com.auralis.app.domain.model.RecommendationSectionType
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for obtaining explainable, on-device music recommendations.
 * 100% offline — operates exclusively on local listening data and device audio files.
 */
interface RecommendationRepository {
    fun getRecommendationSections(): Flow<List<RecommendationSection>>
    fun getSectionDetail(type: RecommendationSectionType): Flow<RecommendationSection?>
    suspend fun refreshRecommendations()
}
