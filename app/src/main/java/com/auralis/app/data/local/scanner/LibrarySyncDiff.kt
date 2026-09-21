package com.auralis.app.data.local.scanner

import com.auralis.app.data.local.db.entity.SongEntity

/**
 * Result of computing the difference between MediaStore scanned songs and current Room database contents.
 */
data class SyncDiffResult(
    val toInsert: List<SongEntity>,
    val toUpdate: List<SongEntity>,
    val toDeleteIds: List<Long>,
    val unchangedCount: Int
)

/**
 * Pure Kotlin differential calculation engine for library synchronization.
 *
 * Prevents duplicate insertions on repeated scans, identifies modified files,
 * and detects deleted/moved files without wiping the database.
 */
object LibrarySyncDiff {

    /**
     * Calculates the differential update between existing database records and scanned records.
     *
     * @param existingMap Map of [id -> dateModified] currently in the database.
     * @param scannedSongs List of freshly scanned [SongEntity] items from MediaStore.
     */
    fun computeDiff(
        existingMap: Map<Long, Long>,
        scannedSongs: List<SongEntity>
    ): SyncDiffResult {
        val toInsert = mutableListOf<SongEntity>()
        val toUpdate = mutableListOf<SongEntity>()
        val scannedIds = HashSet<Long>(scannedSongs.size)
        var unchangedCount = 0

        for (scanned in scannedSongs) {
            val id = scanned.id
            scannedIds.add(id)
            val existingDateModified = existingMap[id]

            if (existingDateModified == null) {
                // New song not in database
                toInsert.add(scanned)
            } else if (existingDateModified != scanned.dateModified) {
                // Modified file metadata
                toUpdate.add(scanned)
            } else {
                // Unchanged file
                unchangedCount++
            }
        }

        // Stale entries: in database but no longer in MediaStore
        val toDeleteIds = existingMap.keys.filter { it !in scannedIds }

        return SyncDiffResult(
            toInsert = toInsert,
            toUpdate = toUpdate,
            toDeleteIds = toDeleteIds,
            unchangedCount = unchangedCount
        )
    }
}
