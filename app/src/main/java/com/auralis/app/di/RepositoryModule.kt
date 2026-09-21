package com.auralis.app.di

import com.auralis.app.data.repository.FavoriteRepositoryImpl
import com.auralis.app.data.repository.HistoryRepositoryImpl
import com.auralis.app.data.repository.PlaylistRepositoryImpl
import com.auralis.app.data.repository.SmartPlaylistRepositoryImpl
import com.auralis.app.data.repository.SongRepositoryImpl
import com.auralis.app.domain.repository.FavoriteRepository
import com.auralis.app.domain.repository.HistoryRepository
import com.auralis.app.domain.repository.PlaylistRepository
import com.auralis.app.domain.repository.SmartPlaylistRepository
import com.auralis.app.domain.repository.SongRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindSongRepository(
        songRepositoryImpl: SongRepositoryImpl
    ): SongRepository

    @Binds
    @Singleton
    abstract fun bindFavoriteRepository(
        favoriteRepositoryImpl: FavoriteRepositoryImpl
    ): FavoriteRepository

    @Binds
    @Singleton
    abstract fun bindPlaylistRepository(
        playlistRepositoryImpl: PlaylistRepositoryImpl
    ): PlaylistRepository

    @Binds
    @Singleton
    abstract fun bindHistoryRepository(
        historyRepositoryImpl: HistoryRepositoryImpl
    ): HistoryRepository

    @Binds
    @Singleton
    abstract fun bindSmartPlaylistRepository(
        smartPlaylistRepositoryImpl: SmartPlaylistRepositoryImpl
    ): SmartPlaylistRepository

    @Binds
    @Singleton
    abstract fun bindSearchRepository(
        searchRepositoryImpl: com.auralis.app.data.repository.SearchRepositoryImpl
    ): com.auralis.app.domain.repository.SearchRepository

    @Binds
    @Singleton
    abstract fun bindLibraryBrowserRepository(
        libraryBrowserRepositoryImpl: com.auralis.app.data.repository.LibraryBrowserRepositoryImpl
    ): com.auralis.app.domain.repository.LibraryBrowserRepository

    @Binds
    @Singleton
    abstract fun bindRecommendationRepository(
        recommendationRepositoryImpl: com.auralis.app.data.repository.RecommendationRepositoryImpl
    ): com.auralis.app.domain.repository.RecommendationRepository
}
