package com.auralis.app

import com.auralis.app.domain.util.PlaylistValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [PlaylistValidator].
 * Verifies validation rules: blank/empty names, whitespace trimming,
 * maximum character limits, and duplicate name prevention.
 */
class PlaylistValidatorTest {

    @Test
    fun validate_validName_returnsValidWithTrimmedName() {
        val result = PlaylistValidator.validate("  Road Trip Classics  ")
        assertTrue(result is PlaylistValidator.ValidationResult.Valid)
        assertEquals("Road Trip Classics", (result as PlaylistValidator.ValidationResult.Valid).sanitizedName)
    }

    @Test
    fun validate_emptyName_returnsInvalid() {
        val result = PlaylistValidator.validate("")
        assertTrue(result is PlaylistValidator.ValidationResult.Invalid)
        assertEquals("Playlist name cannot be empty", (result as PlaylistValidator.ValidationResult.Invalid).reason)
    }

    @Test
    fun validate_whitespaceOnlyName_returnsInvalid() {
        val result = PlaylistValidator.validate("    \t\n  ")
        assertTrue(result is PlaylistValidator.ValidationResult.Invalid)
        assertEquals("Playlist name cannot be empty", (result as PlaylistValidator.ValidationResult.Invalid).reason)
    }

    @Test
    fun validate_nameExceedingMaxChars_returnsInvalid() {
        val longName = "A".repeat(101)
        val result = PlaylistValidator.validate(longName)
        assertTrue(result is PlaylistValidator.ValidationResult.Invalid)
        assertEquals("Playlist name cannot exceed 100 characters", (result as PlaylistValidator.ValidationResult.Invalid).reason)
    }

    @Test
    fun validate_nameExactlyMaxChars_returnsValid() {
        val maxName = "A".repeat(100)
        val result = PlaylistValidator.validate(maxName)
        assertTrue(result is PlaylistValidator.ValidationResult.Valid)
        assertEquals(100, (result as PlaylistValidator.ValidationResult.Valid).sanitizedName.length)
    }

    @Test
    fun validate_duplicateName_caseInsensitive_returnsInvalid() {
        val existing = listOf("Chill Beats", "Workout 2024")
        val result = PlaylistValidator.validate("chill beats", existing)
        assertTrue(result is PlaylistValidator.ValidationResult.Invalid)
        assertEquals("A playlist named \"chill beats\" already exists", (result as PlaylistValidator.ValidationResult.Invalid).reason)
    }

    @Test
    fun validate_duplicateNameWithExtraSpaces_returnsInvalid() {
        val existing = listOf("Workout 2024")
        val result = PlaylistValidator.validate("  Workout 2024  ", existing)
        assertTrue(result is PlaylistValidator.ValidationResult.Invalid)
        assertEquals("A playlist named \"Workout 2024\" already exists", (result as PlaylistValidator.ValidationResult.Invalid).reason)
    }

    @Test
    fun validate_uniqueName_againstExisting_returnsValid() {
        val existing = listOf("Chill Beats", "Workout 2024")
        val result = PlaylistValidator.validate("Study Session", existing)
        assertTrue(result is PlaylistValidator.ValidationResult.Valid)
        assertEquals("Study Session", (result as PlaylistValidator.ValidationResult.Valid).sanitizedName)
    }
}
