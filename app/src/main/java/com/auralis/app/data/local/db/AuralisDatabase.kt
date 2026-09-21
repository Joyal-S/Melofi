package com.auralis.app.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.auralis.app.data.local.db.dao.AlbumDao
import com.auralis.app.data.local.db.dao.ArtistDao
import com.auralis.app.data.local.db.dao.FavoriteDao
import com.auralis.app.data.local.db.dao.GenreDao
import com.auralis.app.data.local.db.dao.HistoryDao
import com.auralis.app.data.local.db.dao.PlaylistDao
import com.auralis.app.data.local.db.dao.SongDao
import com.auralis.app.data.local.db.entity.AlbumEntity
import com.auralis.app.data.local.db.entity.ArtistEntity
import com.auralis.app.data.local.db.entity.FavoriteEntity
import com.auralis.app.data.local.db.entity.GenreEntity
import com.auralis.app.data.local.db.entity.PlaybackHistoryEntity
import com.auralis.app.data.local.db.entity.PlaylistEntity
import com.auralis.app.data.local.db.entity.PlaylistSongCrossRef
import com.auralis.app.data.local.db.entity.SongEntity

/**
 * Main Room database for Auralis offline audio library and personal organization.
 */
@Database(
    entities = [
        SongEntity::class,
        AlbumEntity::class,
        ArtistEntity::class,
        GenreEntity::class,
        PlaylistEntity::class,
        FavoriteEntity::class,
        PlaylistSongCrossRef::class,
        PlaybackHistoryEntity::class,
        com.auralis.app.data.local.db.entity.SearchHistoryEntity::class,
    ],
    version = 4,
    exportSchema = true
)
abstract class AuralisDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
    abstract fun albumDao(): AlbumDao
    abstract fun artistDao(): ArtistDao
    abstract fun genreDao(): GenreDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun historyDao(): HistoryDao
    abstract fun searchHistoryDao(): com.auralis.app.data.local.db.dao.SearchHistoryDao

    companion object {
        const val DATABASE_NAME = "auralis_database.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Create playlists table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `playlists` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_playlists_name` ON `playlists` (`name`)")

                // 2. Create favorites table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `favorites` (
                        `songId` INTEGER NOT NULL,
                        `addedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`songId`),
                        FOREIGN KEY(`songId`) REFERENCES `songs`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_favorites_songId` ON `favorites` (`songId`)")

                // 3. Create playlist_songs table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `playlist_songs` (
                        `playlistId` INTEGER NOT NULL,
                        `songId` INTEGER NOT NULL,
                        `position` INTEGER NOT NULL,
                        `addedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`playlistId`, `songId`),
                        FOREIGN KEY(`playlistId`) REFERENCES `playlists`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`songId`) REFERENCES `songs`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_playlist_songs_playlistId` ON `playlist_songs` (`playlistId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_playlist_songs_songId` ON `playlist_songs` (`songId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_playlist_songs_playlistId_position` ON `playlist_songs` (`playlistId`, `position`)")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create playback_history table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `playback_history` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `songId` INTEGER NOT NULL,
                        `playedAt` INTEGER NOT NULL,
                        `durationMs` INTEGER NOT NULL,
                        `completed` INTEGER NOT NULL,
                        FOREIGN KEY(`songId`) REFERENCES `songs`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_playback_history_songId` ON `playback_history` (`songId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_playback_history_playedAt` ON `playback_history` (`playedAt`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_playback_history_songId_playedAt` ON `playback_history` (`songId`, `playedAt`)")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create search_history table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `search_history` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `query` TEXT NOT NULL,
                        `searchedAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_search_history_query` ON `search_history` (`query`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_search_history_searchedAt` ON `search_history` (`searchedAt`)")
            }
        }
    }
}
