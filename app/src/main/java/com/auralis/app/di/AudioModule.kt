package com.auralis.app.di

import com.auralis.app.playback.audiofx.SleepTimerManager
import com.auralis.app.playback.client.PlaybackController
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AudioModule {

    @Provides
    @Singleton
    fun provideSleepTimerManager(playbackController: PlaybackController): SleepTimerManager {
        return SleepTimerManager(playbackController)
    }
}
