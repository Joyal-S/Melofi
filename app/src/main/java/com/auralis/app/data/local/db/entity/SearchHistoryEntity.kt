package com.auralis.app.data.local.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for storing local search history.
 * Queries are indexed uniquely to prevent duplicates, and timestamp-indexed for fast recency ordering.
 * 100% offline — never sent to any remote server or analytics service.
 */
@Entity(
    tableName = "search_history",
    indices = [
        Index(value = ["query"], unique = true),
        Index("searchedAt")
    ]
)
data class SearchHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val query: String,
    val searchedAt: Long
)
