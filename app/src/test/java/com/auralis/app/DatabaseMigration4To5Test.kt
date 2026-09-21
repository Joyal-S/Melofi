package com.auralis.app

import androidx.sqlite.db.SupportSQLiteDatabase
import com.auralis.app.data.local.db.AuralisDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Proxy

/**
 * Unit tests verifying Room database migration definition from Version 4 to Version 5.
 */
class DatabaseMigration4To5Test {

    @Test
    fun migration_4_5_versionsAreCorrect() {
        val migration = AuralisDatabase.MIGRATION_4_5
        assertEquals(4, migration.startVersion)
        assertEquals(5, migration.endVersion)
    }

    @Test
    fun migration_4_5_executesCorrectSql() {
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

        AuralisDatabase.MIGRATION_4_5.migrate(dummyDb)

        assertTrue(
            "Should alter playlists table to add customCoverUri",
            executedSqlList.any { it.contains("ALTER TABLE `playlists` ADD COLUMN `customCoverUri`") }
        )
        assertTrue(
            "Should alter playlists table to add description",
            executedSqlList.any { it.contains("ALTER TABLE `playlists` ADD COLUMN `description`") }
        )
    }
}
