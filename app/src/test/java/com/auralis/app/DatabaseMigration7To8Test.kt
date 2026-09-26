package com.auralis.app

import androidx.sqlite.db.SupportSQLiteDatabase
import com.auralis.app.data.local.db.AuralisDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Proxy

/**
 * Unit tests verifying Room database migration definition from Version 7 to Version 8.
 * Ensures creation of hidden_audio and excluded_folders tables, and default seed exclusions.
 */
class DatabaseMigration7To8Test {

    @Test
    fun migration_7_8_versionsAreCorrect() {
        val migration = AuralisDatabase.MIGRATION_7_8
        assertEquals(7, migration.startVersion)
        assertEquals(8, migration.endVersion)
    }

    @Test
    fun migration_7_8_executesCorrectSql() {
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

        AuralisDatabase.MIGRATION_7_8.migrate(dummyDb)

        assertTrue(
            "Should create hidden_audio table",
            executedSqlList.any { it.contains("CREATE TABLE IF NOT EXISTS `hidden_audio`") }
        )
        assertTrue(
            "Should create excluded_folders table",
            executedSqlList.any { it.contains("CREATE TABLE IF NOT EXISTS `excluded_folders`") }
        )
        assertTrue(
            "Should seed default folders",
            executedSqlList.any { it.contains("INSERT OR IGNORE INTO `excluded_folders`") && it.contains("Recordings") }
        )
        assertTrue(
            "Should seed Notifications default folder",
            executedSqlList.any { it.contains("Notifications") }
        )
        assertTrue(
            "Should seed Ringtones default folder",
            executedSqlList.any { it.contains("Ringtones") }
        )
        assertTrue(
            "Should seed Alarms default folder",
            executedSqlList.any { it.contains("Alarms") }
        )
        assertTrue(
            "Should seed Podcasts default folder",
            executedSqlList.any { it.contains("Podcasts") }
        )
    }
}
