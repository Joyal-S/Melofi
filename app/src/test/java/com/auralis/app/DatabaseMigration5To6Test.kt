package com.auralis.app

import androidx.sqlite.db.SupportSQLiteDatabase
import com.auralis.app.data.local.db.AuralisDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Proxy

/**
 * Unit tests verifying Room database migration definition from Version 5 to Version 6.
 * Ensures creation of the artwork_overrides table and unique index for universal artwork customization.
 */
class DatabaseMigration5To6Test {

    @Test
    fun migration_5_6_versionsAreCorrect() {
        val migration = AuralisDatabase.MIGRATION_5_6
        assertEquals(5, migration.startVersion)
        assertEquals(6, migration.endVersion)
    }

    @Test
    fun migration_5_6_executesCorrectSql() {
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

        AuralisDatabase.MIGRATION_5_6.migrate(dummyDb)

        assertTrue(
            "Should create artwork_overrides table",
            executedSqlList.any { it.contains("CREATE TABLE IF NOT EXISTS `artwork_overrides`") }
        )
        assertTrue(
            "Should create unique index on artwork_overrides (entityType, entityKey)",
            executedSqlList.any { it.contains("CREATE UNIQUE INDEX IF NOT EXISTS `index_artwork_overrides_entityType_entityKey`") }
        )
    }
}
