package com.auralis.app

import androidx.sqlite.db.SupportSQLiteDatabase
import com.auralis.app.data.local.db.AuralisDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Proxy

/**
 * Unit tests verifying Room database migration definition from Version 8 to Version 9.
 * Validates the creation of artist_profile_overrides table for user-customized artist display names.
 */
class DatabaseMigration8To9Test {

    @Test
    fun migration_8_9_versionsAreCorrect() {
        val migration = AuralisDatabase.MIGRATION_8_9
        assertEquals(8, migration.startVersion)
        assertEquals(9, migration.endVersion)
    }

    @Test
    fun migration_8_9_executesCorrectSql() {
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

        AuralisDatabase.MIGRATION_8_9.migrate(dummyDb)

        assertTrue(
            "Should create artist_profile_overrides table",
            executedSqlList.any { it.contains("CREATE TABLE IF NOT EXISTS `artist_profile_overrides`") }
        )
        val createSql = executedSqlList.first { it.contains("artist_profile_overrides") }
        assertTrue("Must include artistId column", createSql.contains("`artistId` INTEGER PRIMARY KEY NOT NULL"))
        assertTrue("Must include originalName column", createSql.contains("`originalName` TEXT NOT NULL"))
        assertTrue("Must include customName column", createSql.contains("`customName` TEXT NOT NULL"))
        assertTrue("Must include updatedAt column", createSql.contains("`updatedAt` INTEGER NOT NULL"))
    }
}
