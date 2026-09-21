package com.auralis.app

import com.auralis.app.data.local.db.AuralisDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * Unit tests verifying Room database migration definition from Version 1 to Version 2.
 */
class DatabaseMigrationSqlTest {

    @Test
    fun migration_1_2_versionsAreCorrect() {
        val migration = AuralisDatabase.MIGRATION_1_2
        assertEquals(1, migration.startVersion)
        assertEquals(2, migration.endVersion)
    }

    @Test
    fun databaseName_isConsistent() {
        assertEquals("auralis_database.db", AuralisDatabase.DATABASE_NAME)
    }
}
