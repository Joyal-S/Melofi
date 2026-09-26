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
import com.auralis.app.data.local.db.dao.ArtworkOverrideDao
import com.auralis.app.data.local.db.dao.ExcludedFolderDao
import com.auralis.app.data.local.db.dao.HiddenAudioDao
import com.auralis.app.data.local.db.dao.SongDefaultCoverDao
import com.auralis.app.data.local.db.entity.ArtworkOverrideEntity
import com.auralis.app.data.local.db.entity.ExcludedFolderEntity
import com.auralis.app.data.local.db.entity.HiddenAudioEntity
import com.auralis.app.data.local.db.entity.PlaylistSongCrossRef
import com.auralis.app.data.local.db.entity.SongDefaultCoverEntity
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
        ArtworkOverrideEntity::class,
        SongDefaultCoverEntity::class,
        HiddenAudioEntity::class,
        ExcludedFolderEntity::class,
        com.auralis.app.data.local.db.entity.ArtistProfileOverrideEntity::class,
    ],
    version = 10,
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
    abstract fun artworkOverrideDao(): ArtworkOverrideDao
    abstract fun songDefaultCoverDao(): SongDefaultCoverDao
    abstract fun hiddenAudioDao(): HiddenAudioDao
    abstract fun excludedFolderDao(): ExcludedFolderDao
    abstract fun artistProfileOverrideDao(): com.auralis.app.data.local.db.dao.ArtistProfileOverrideDao

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

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `playlists` ADD COLUMN `customCoverUri` TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE `playlists` ADD COLUMN `description` TEXT DEFAULT NULL")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `artwork_overrides` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `entityType` TEXT NOT NULL,
                        `entityKey` TEXT NOT NULL,
                        `localPath` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_artwork_overrides_entityType_entityKey` ON `artwork_overrides` (`entityType`, `entityKey`)"
                )
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `song_default_covers` (
                        `songId` INTEGER PRIMARY KEY NOT NULL,
                        `coverFileName` TEXT NOT NULL,
                        `assignedAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Create hidden_audio table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `hidden_audio` (
                        `id` INTEGER PRIMARY KEY NOT NULL,
                        `contentUri` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `artistName` TEXT NOT NULL,
                        `albumName` TEXT NOT NULL,
                        `duration` INTEGER NOT NULL,
                        `filePath` TEXT,
                        `albumArtUri` TEXT,
                        `hiddenAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )

                // 2. Create excluded_folders table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `excluded_folders` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `folderName` TEXT NOT NULL,
                        `folderPath` TEXT NOT NULL,
                        `isEnabled` INTEGER NOT NULL DEFAULT 1,
                        `isDefault` INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )

                // 3. Seed default excluded folders
                val defaultFolders = listOf(
                    "Recordings",
                    "Notifications",
                    "Ringtones",
                    "Alarms",
                    "Podcasts"
                )
                for (folder in defaultFolders) {
                    db.execSQL(
                        "INSERT OR IGNORE INTO `excluded_folders` (`folderName`, `folderPath`, `isEnabled`, `isDefault`) VALUES ('$folder', '$folder', 1, 1)"
                    )
                }
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `artist_profile_overrides` (
                        `artistId` INTEGER PRIMARY KEY NOT NULL,
                        `originalName` TEXT NOT NULL,
                        `customName` TEXT NOT NULL,
                        `updatedAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Create artists_new table with persistent identity and unique normalizedName index
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `artists_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `normalizedName` TEXT NOT NULL,
                        `albumCount` INTEGER NOT NULL,
                        `songCount` INTEGER NOT NULL,
                        `customArtworkUri` TEXT,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_artists_normalizedName` ON `artists_new` (`normalizedName`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_artists_name` ON `artists_new` (`name`)")

                // 2. Fetch artist profile overrides
                val profileOverrides = mutableMapOf<Long, String>()
                try {
                    val overrideCursor = db.query("SELECT artistId, customName FROM artist_profile_overrides")
                    while (overrideCursor.moveToNext()) {
                        val aId = overrideCursor.getLong(0)
                        val cName = overrideCursor.getString(1)
                        if (!cName.isNullOrBlank()) {
                            profileOverrides[aId] = cName
                        }
                    }
                    overrideCursor.close()
                } catch (ignored: Exception) {}

                // 3. Fetch custom artwork overrides
                val artworkOverrides = mutableMapOf<String, String>()
                try {
                    val artCursor = db.query("SELECT entityKey, localPath FROM artwork_overrides WHERE entityType = 'ARTIST'")
                    while (artCursor.moveToNext()) {
                        val key = artCursor.getString(0)
                        val path = artCursor.getString(1)
                        if (!key.isNullOrBlank() && !path.isNullOrBlank()) {
                            artworkOverrides[key] = path
                        }
                    }
                    artCursor.close()
                } catch (ignored: Exception) {}

                // 4. Map old artist records to new canonical artist IDs
                val oldIdToCanonicalId = mutableMapOf<Long, Long>()
                val normalizedToCanonicalId = mutableMapOf<String, Long>()

                try {
                    val artistCursor = db.query("SELECT id, name FROM artists")
                    while (artistCursor.moveToNext()) {
                        val oldId = artistCursor.getLong(0)
                        val rawName = artistCursor.getString(1)
                        val effectiveName = profileOverrides[oldId] ?: rawName
                        val clean = com.auralis.app.core.util.ArtistNameNormalizer.cleanDisplayName(effectiveName)
                        val norm = com.auralis.app.core.util.ArtistNameNormalizer.normalize(effectiveName)
                        val artUri = artworkOverrides[oldId.toString()]

                        if (norm.isNotEmpty()) {
                            if (normalizedToCanonicalId.containsKey(norm)) {
                                oldIdToCanonicalId[oldId] = normalizedToCanonicalId[norm]!!
                            } else {
                                val now = System.currentTimeMillis()
                                val stmt = db.compileStatement(
                                    "INSERT INTO `artists_new` (`name`, `normalizedName`, `albumCount`, `songCount`, `customArtworkUri`, `createdAt`, `updatedAt`) VALUES (?, ?, ?, ?, ?, ?, ?)"
                                )
                                stmt.bindString(1, clean)
                                stmt.bindString(2, norm)
                                stmt.bindLong(3, 0L)
                                stmt.bindLong(4, 0L)
                                if (artUri != null) stmt.bindString(5, artUri) else stmt.bindNull(5)
                                stmt.bindLong(6, now)
                                stmt.bindLong(7, now)
                                val newId = stmt.executeInsert()
                                stmt.close()

                                normalizedToCanonicalId[norm] = newId
                                oldIdToCanonicalId[oldId] = newId
                            }
                        }
                    }
                    artistCursor.close()
                } catch (ignored: Exception) {}

                // 5. Inspect existing songs to associate with canonical artists or create missing ones
                try {
                    val songCursor = db.query("SELECT id, artistId, artistName FROM songs")
                    while (songCursor.moveToNext()) {
                        val songId = songCursor.getLong(0)
                        val rawArtistId = if (songCursor.isNull(1)) null else songCursor.getLong(1)
                        val rawArtistName = songCursor.getString(2)

                        var targetArtistId: Long? = null
                        if (rawArtistId != null && oldIdToCanonicalId.containsKey(rawArtistId)) {
                            targetArtistId = oldIdToCanonicalId[rawArtistId]
                        }

                        if (targetArtistId == null && !rawArtistName.isNullOrBlank()) {
                            val norm = com.auralis.app.core.util.ArtistNameNormalizer.normalize(rawArtistName)
                            if (norm.isNotEmpty()) {
                                if (normalizedToCanonicalId.containsKey(norm)) {
                                    targetArtistId = normalizedToCanonicalId[norm]
                                } else {
                                    val clean = com.auralis.app.core.util.ArtistNameNormalizer.cleanDisplayName(rawArtistName)
                                    val now = System.currentTimeMillis()
                                    val stmt = db.compileStatement(
                                        "INSERT INTO `artists_new` (`name`, `normalizedName`, `albumCount`, `songCount`, `customArtworkUri`, `createdAt`, `updatedAt`) VALUES (?, ?, ?, ?, ?, ?, ?)"
                                    )
                                    stmt.bindString(1, clean)
                                    stmt.bindString(2, norm)
                                    stmt.bindLong(3, 0L)
                                    stmt.bindLong(4, 0L)
                                    stmt.bindNull(5)
                                    stmt.bindLong(6, now)
                                    stmt.bindLong(7, now)
                                    val newId = stmt.executeInsert()
                                    stmt.close()

                                    normalizedToCanonicalId[norm] = newId
                                    targetArtistId = newId
                                }
                            }
                        }

                        if (targetArtistId != null) {
                            val updateStmt = db.compileStatement("UPDATE `songs` SET `artistId` = ? WHERE `id` = ?")
                            updateStmt.bindLong(1, targetArtistId)
                            updateStmt.bindLong(2, songId)
                            updateStmt.executeUpdateDelete()
                            updateStmt.close()
                        }
                    }
                    songCursor.close()
                } catch (ignored: Exception) {}

                // 6. Swap table: drop old artists table and rename artists_new to artists
                db.execSQL("DROP TABLE IF EXISTS `artists`")
                db.execSQL("ALTER TABLE `artists_new` RENAME TO `artists`")

                // 7. Recalculate songCount and albumCount for all artists
                db.execSQL(
                    """
                    UPDATE `artists` SET
                        `songCount` = (SELECT COUNT(*) FROM `songs` WHERE `songs`.`artistId` = `artists`.`id`),
                        `albumCount` = (SELECT COUNT(DISTINCT `albumId`) FROM `songs` WHERE `songs`.`artistId` = `artists`.`id` AND `albumId` IS NOT NULL)
                    """.trimIndent()
                )
            }
        }
    }
}
