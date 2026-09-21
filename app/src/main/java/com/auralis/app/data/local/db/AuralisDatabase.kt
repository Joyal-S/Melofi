package com.auralis.app.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.auralis.app.data.local.db.dao.AlbumDao
import com.auralis.app.data.local.db.dao.ArtistDao
import com.auralis.app.data.local.db.dao.GenreDao
import com.auralis.app.data.local.db.dao.SongDao
import com.auralis.app.data.local.db.entity.AlbumEntity
import com.auralis.app.data.local.db.entity.ArtistEntity
import com.auralis.app.data.local.db.entity.GenreEntity
import com.auralis.app.data.local.db.entity.SongEntity

/**
 * Main Room database for Auralis offline audio library.
 */
@Database(
    entities = [
        SongEntity::class,
        AlbumEntity::class,
        ArtistEntity::class,
        GenreEntity::class,
    ],
    version = 1,
    exportSchema = true
)
abstract class AuralisDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
    abstract fun albumDao(): AlbumDao
    abstract fun artistDao(): ArtistDao
    abstract fun genreDao(): GenreDao

    companion object {
        const val DATABASE_NAME = "auralis_database.db"
    }
}
