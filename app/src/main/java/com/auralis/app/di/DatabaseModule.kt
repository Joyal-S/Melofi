package com.auralis.app.di

import android.content.Context
import androidx.room.Room
import com.auralis.app.data.local.db.AuralisDatabase
import com.auralis.app.data.local.db.dao.AlbumDao
import com.auralis.app.data.local.db.dao.ArtistDao
import com.auralis.app.data.local.db.dao.GenreDao
import com.auralis.app.data.local.db.dao.SongDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): AuralisDatabase {
        return Room.databaseBuilder(
            context,
            AuralisDatabase::class.java,
            AuralisDatabase.DATABASE_NAME
        ).build()
    }

    @Provides
    fun provideSongDao(database: AuralisDatabase): SongDao {
        return database.songDao()
    }

    @Provides
    fun provideAlbumDao(database: AuralisDatabase): AlbumDao {
        return database.albumDao()
    }

    @Provides
    fun provideArtistDao(database: AuralisDatabase): ArtistDao {
        return database.artistDao()
    }

    @Provides
    fun provideGenreDao(database: AuralisDatabase): GenreDao {
        return database.genreDao()
    }
}
