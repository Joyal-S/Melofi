package com.auralis.app

import androidx.sqlite.db.SupportSQLiteDatabase
import com.auralis.app.data.local.db.AuralisDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Proxy

/**
 * Unit tests verifying Room database migration definition from Version 3 to Version 4.
 */
class DatabaseMigration3To4Test {

    @Test
    fun migration_3_4_versionsAreCorrect() {
        val migration = AuralisDatabase.MIGRATION_3_4
        assertEquals(3, migration.startVersion)
        assertEquals(4, migration.endVersion)
    }

    @Test
    fun previousMigrations_areStillIntact() {
        val migration12 = AuralisDatabase.MIGRATION_1_2
        assertEquals(1, migration12.startVersion)
        assertEquals(2, migration12.endVersion)

        val migration23 = AuralisDatabase.MIGRATION_2_3
        assertEquals(2, migration23.startVersion)
        assertEquals(3, migration23.endVersion)
    }

    @Test
    fun migration_3_4_executesCorrectSql() {
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

        AuralisDatabase.MIGRATION_3_4.migrate(dummyDb)

        // Verify table creation
        assertTrue(
            "Should create search_history table",
            executedSqlList.any { it.contains("CREATE TABLE IF NOT EXISTS `search_history`") }
        )
        // Verify unique index on query
        assertTrue(
            "Should create unique index on query",
            executedSqlList.any { it.contains("CREATE UNIQUE INDEX IF NOT EXISTS `index_search_history_query`") }
        )
        // Verify index on searchedAt
        assertTrue(
            "Should create index on searchedAt",
            executedSqlList.any { it.contains("CREATE INDEX IF NOT EXISTS `index_search_history_searchedAt`") }
        )
    }

    @Test
    fun databaseName_isConsistent() {
        assertEquals("auralis_database.db", AuralisDatabase.DATABASE_NAME)
    }
}
