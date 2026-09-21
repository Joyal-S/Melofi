package com.auralis.app.di

import com.auralis.app.playback.queue.PlaybackQueue
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PlaybackModule {

    @Provides
    @Singleton
    fun providePlaybackQueue(): PlaybackQueue = PlaybackQueue()
}
