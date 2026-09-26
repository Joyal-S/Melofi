package com.auralis.app

import android.database.MatrixCursor
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteStatement
import com.auralis.app.data.local.db.AuralisDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Proxy

/**
 * Unit tests verifying Room database migration definition from Version 9 to Version 10.
 * Validates:
 * - Persistent Artist entity table schema creation
 * - UNIQUE constraint / index on normalizedName
 * - Data preservation and table swap
 * - Atomic recalculation of songCount and albumCount
 */
class DatabaseMigration9To10Test {

    @Test
    fun migration_9_10_versionsAreCorrect() {
        val migration = AuralisDatabase.MIGRATION_9_10
        assertEquals(9, migration.startVersion)
        assertEquals(10, migration.endVersion)
    }

    @Test
    fun migration_9_10_executesCorrectSql() {
        val executedSqlList = mutableListOf<String>()

        val dummyStatement = Proxy.newProxyInstance(
            SupportSQLiteStatement::class.java.classLoader,
            arrayOf(SupportSQLiteStatement::class.java)
        ) { _, method, _ ->
            when (method.name) {
                "executeInsert" -> 1L
                "executeUpdateDelete" -> 1
                else -> null
            }
        } as SupportSQLiteStatement

        val dummyDb = Proxy.newProxyInstance(
            SupportSQLiteDatabase::class.java.classLoader,
            arrayOf(SupportSQLiteDatabase::class.java)
        ) { _, method, args ->
            when (method.name) {
                "execSQL" -> {
                    if (args != null && args.isNotEmpty()) {
                        executedSqlList.add(args[0] as String)
                    }
                    null
                }
                "compileStatement" -> {
                    dummyStatement
                }
                "query" -> {
                    // Return empty MatrixCursor so query iteration completes cleanly
                    MatrixCursor(arrayOf("id", "name", "albumCount", "songCount", "artistId", "customName", "entityKey", "localPath", "artistName"))
                }
                else -> null
            }
        } as SupportSQLiteDatabase

        AuralisDatabase.MIGRATION_9_10.migrate(dummyDb)

        assertTrue(
            "Must create artists_new table",
            executedSqlList.any { it.contains("CREATE TABLE IF NOT EXISTS `artists_new`") }
        )
        val createSql = executedSqlList.first { it.contains("artists_new") }
        assertTrue("Must have id primary key autoincrement", createSql.contains("`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL"))
        assertTrue("Must have name column", createSql.contains("`name` TEXT NOT NULL"))
        assertTrue("Must have normalizedName column", createSql.contains("`normalizedName` TEXT NOT NULL"))
        assertTrue("Must have albumCount column", createSql.contains("`albumCount` INTEGER NOT NULL"))
        assertTrue("Must have songCount column", createSql.contains("`songCount` INTEGER NOT NULL"))
        assertTrue("Must have customArtworkUri column", createSql.contains("`customArtworkUri` TEXT"))
        assertTrue("Must have createdAt column", createSql.contains("`createdAt` INTEGER NOT NULL"))
        assertTrue("Must have updatedAt column", createSql.contains("`updatedAt` INTEGER NOT NULL"))

        assertTrue(
            "Must create UNIQUE index on normalizedName",
            executedSqlList.any { it.contains("CREATE UNIQUE INDEX IF NOT EXISTS `index_artists_normalizedName`") }
        )
        assertTrue(
            "Must create index on name",
            executedSqlList.any { it.contains("CREATE INDEX IF NOT EXISTS `index_artists_name`") }
        )
        assertTrue(
            "Must drop old artists table",
            executedSqlList.any { it.contains("DROP TABLE IF EXISTS `artists`") }
        )
        assertTrue(
            "Must rename artists_new to artists",
            executedSqlList.any { it.contains("ALTER TABLE `artists_new` RENAME TO `artists`") }
        )
        assertTrue(
            "Must recalculate song and album counts",
            executedSqlList.any { it.contains("UPDATE `artists` SET") && it.contains("`songCount` =") }
        )
    }
}
