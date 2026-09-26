package com.auralis.app

import androidx.sqlite.db.SupportSQLiteDatabase
import com.auralis.app.data.local.db.AuralisDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Proxy

/**
 * Unit test verifying Room database migration definition from Version 10 to Version 11.
 * Validates:
 * - Creation of imported_folders table
 * - Unique index on treeUri
 */
class DatabaseMigration10To11Test {

    @Test
    fun migration_10_11_versionsAreCorrect() {
        val migration = AuralisDatabase.MIGRATION_10_11
        assertEquals(10, migration.startVersion)
        assertEquals(11, migration.endVersion)
    }

    @Test
    fun migration_10_11_executesCorrectSql() {
        val executedSqlList = mutableListOf<String>()

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
                else -> null
            }
        } as SupportSQLiteDatabase

        AuralisDatabase.MIGRATION_10_11.migrate(dummyDb)

        assertTrue(
            "Must create imported_folders table",
            executedSqlList.any { it.contains("CREATE TABLE IF NOT EXISTS `imported_folders`") }
        )
        val createSql = executedSqlList.first { it.contains("imported_folders") }
        assertTrue("Must have id primary key autoincrement", createSql.contains("`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL"))
        assertTrue("Must have treeUri column", createSql.contains("`treeUri` TEXT NOT NULL"))
        assertTrue("Must have displayName column", createSql.contains("`displayName` TEXT NOT NULL"))
        assertTrue("Must have dateAdded column", createSql.contains("`dateAdded` INTEGER NOT NULL"))
        assertTrue("Must have isPermissionValid column", createSql.contains("`isPermissionValid` INTEGER NOT NULL"))
        assertTrue("Must have songCount column", createSql.contains("`songCount` INTEGER NOT NULL"))

        assertTrue(
            "Must create UNIQUE index on treeUri",
            executedSqlList.any { it.contains("CREATE UNIQUE INDEX IF NOT EXISTS `index_imported_folders_treeUri`") }
        )
    }
}
