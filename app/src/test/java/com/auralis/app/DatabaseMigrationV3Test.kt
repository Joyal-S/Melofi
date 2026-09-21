package com.auralis.app

import androidx.sqlite.db.SupportSQLiteDatabase
import com.auralis.app.data.local.db.AuralisDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Proxy

/**
 * Unit tests verifying Room database migration definition from Version 2 to Version 3.
 */
class DatabaseMigrationV3Test {

    @Test
    fun migration_2_3_versionsAreCorrect() {
        val migration = AuralisDatabase.MIGRATION_2_3
        assertEquals(2, migration.startVersion)
        assertEquals(3, migration.endVersion)
    }

    @Test
    fun migration_1_2_versionsAreStillIntact() {
        val migration = AuralisDatabase.MIGRATION_1_2
        assertEquals(1, migration.startVersion)
        assertEquals(2, migration.endVersion)
    }

    @Test
    fun migration_2_3_executesCorrectSql() {
        val executedSqlList = mutableListOf<String>()

        val dummyDb = Proxy.newProxyInstance(
            SupportSQLiteDatabase::class.java.classLoader,
            arrayOf(SupportSQLiteDatabase::class.java)
        ) { _, method, args ->
            if (method.name == "execSQL" && args != null && args.isNotEmpty()) {
                executedSqlList.add(args[0] as String)
            }
            null
        } as SupportSQLiteDatabase

        AuralisDatabase.MIGRATION_2_3.migrate(dummyDb)

        // Verify table creation
        assertTrue(
            "Should create playback_history table",
            executedSqlList.any { it.contains("CREATE TABLE IF NOT EXISTS `playback_history`") }
        )
        // Verify foreign key cascade
        assertTrue(
            "Should have foreign key with CASCADE delete",
            executedSqlList.any { it.contains("FOREIGN KEY(`songId`) REFERENCES `songs`(`id`)") && it.contains("ON DELETE CASCADE") }
        )
        // Verify indices
        assertTrue(
            "Should create index on songId",
            executedSqlList.any { it.contains("index_playback_history_songId") }
        )
        assertTrue(
            "Should create index on playedAt",
            executedSqlList.any { it.contains("index_playback_history_playedAt") }
        )
        assertTrue(
            "Should create compound index on songId and playedAt",
            executedSqlList.any { it.contains("index_playback_history_songId_playedAt") }
        )
    }

    @Test
    fun databaseName_isConsistent() {
        assertEquals("auralis_database.db", AuralisDatabase.DATABASE_NAME)
    }
}
