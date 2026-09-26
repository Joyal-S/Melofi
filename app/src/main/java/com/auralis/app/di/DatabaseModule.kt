package com.auralis.app.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.auralis.app.data.local.db.AuralisDatabase
import com.auralis.app.data.local.db.dao.AlbumDao
import com.auralis.app.data.local.db.dao.ArtistDao
import com.auralis.app.data.local.db.dao.FavoriteDao
import com.auralis.app.data.local.db.dao.GenreDao
import com.auralis.app.data.local.db.dao.HistoryDao
import com.auralis.app.data.local.db.dao.PlaylistDao
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
        )
        .addMigrations(
            AuralisDatabase.MIGRATION_1_2,
            AuralisDatabase.MIGRATION_2_3,
            AuralisDatabase.MIGRATION_3_4,
            AuralisDatabase.MIGRATION_4_5,
            AuralisDatabase.MIGRATION_5_6,
            AuralisDatabase.MIGRATION_6_7,
            AuralisDatabase.MIGRATION_7_8,
            AuralisDatabase.MIGRATION_8_9,
            AuralisDatabase.MIGRATION_9_10
        )
        .addCallback(object : RoomDatabase.Callback() {
            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                db.setForeignKeyConstraintsEnabled(true)
            }
        })
        .build()
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

    @Provides
    fun providePlaylistDao(database: AuralisDatabase): PlaylistDao {
        return database.playlistDao()
    }

    @Provides
    fun provideFavoriteDao(database: AuralisDatabase): FavoriteDao {
        return database.favoriteDao()
    }

    @Provides
    fun provideHistoryDao(database: AuralisDatabase): HistoryDao {
        return database.historyDao()
    }

    @Provides
    fun provideSearchHistoryDao(database: AuralisDatabase): com.auralis.app.data.local.db.dao.SearchHistoryDao {
        return database.searchHistoryDao()
    }

    @Provides
    fun provideArtworkOverrideDao(database: AuralisDatabase): com.auralis.app.data.local.db.dao.ArtworkOverrideDao {
        return database.artworkOverrideDao()
    }

    @Provides
    fun provideSongDefaultCoverDao(database: AuralisDatabase): com.auralis.app.data.local.db.dao.SongDefaultCoverDao {
        return database.songDefaultCoverDao()
    }

    @Provides
    fun provideHiddenAudioDao(database: AuralisDatabase): com.auralis.app.data.local.db.dao.HiddenAudioDao {
        return database.hiddenAudioDao()
    }

    @Provides
    fun provideExcludedFolderDao(database: AuralisDatabase): com.auralis.app.data.local.db.dao.ExcludedFolderDao {
        return database.excludedFolderDao()
    }

    @Provides
    fun provideArtistProfileOverrideDao(database: AuralisDatabase): com.auralis.app.data.local.db.dao.ArtistProfileOverrideDao {
        return database.artistProfileOverrideDao()
    }
}
