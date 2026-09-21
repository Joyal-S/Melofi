package com.auralis.app.data.local.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing a meaningful playback session in the local listening history.
 *
 * Foreign-keyed to [SongEntity] with CASCADE delete to guarantee that when a song
 * is removed from the device library, its listening history is cleanly pruned without
 * leaving orphaned rows or corrupted aggregation statistics.
 */
@Entity(
    tableName = "playback_history",
    foreignKeys = [
        ForeignKey(
            entity = SongEntity::class,
            parentColumns = ["id"],
            childColumns = ["songId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("songId"),
        Index("playedAt"),
        Index(value = ["songId", "playedAt"])
    ]
)
data class PlaybackHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val songId: Long,
    val playedAt: Long = System.currentTimeMillis(),
    val durationMs: Long,
    val completed: Boolean = false
)
