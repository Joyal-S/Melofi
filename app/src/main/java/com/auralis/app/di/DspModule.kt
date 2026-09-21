package com.auralis.app.di

import com.auralis.app.playback.dsp.MelofyDspAudioProcessor
import com.auralis.app.playback.dsp.MelofyDspEngine
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DspModule {

    @Provides
    @Singleton
    fun provideMelofyDspEngine(): MelofyDspEngine {
        return MelofyDspEngine()
    }

    @Provides
    @Singleton
    fun provideMelofyDspAudioProcessor(engine: MelofyDspEngine): MelofyDspAudioProcessor {
        return MelofyDspAudioProcessor(engine)
    }
}
