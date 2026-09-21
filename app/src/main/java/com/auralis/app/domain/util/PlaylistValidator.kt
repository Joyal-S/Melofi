package com.auralis.app.domain.util

/**
 * Pure Kotlin validator for playlist names.
 */
object PlaylistValidator {

    const val MAX_NAME_LENGTH = 100

    sealed class ValidationResult {
        data class Valid(val sanitizedName: String) : ValidationResult()
        data class Invalid(val reason: String) : ValidationResult()
    }

    /**
     * Validates and sanitizes a proposed playlist name.
     *
     * - Trims leading and trailing whitespace.
     * - Rejects blank or empty names.
     * - Rejects names exceeding [MAX_NAME_LENGTH] characters.
     * - Rejects duplicate names (case-insensitive) if [existingNames] are provided.
     */
    fun validate(name: String, existingNames: List<String> = emptyList()): ValidationResult {
        val trimmed = name.trim()

        if (trimmed.isEmpty()) {
            return ValidationResult.Invalid("Playlist name cannot be empty")
        }

        if (trimmed.length > MAX_NAME_LENGTH) {
            return ValidationResult.Invalid("Playlist name cannot exceed $MAX_NAME_LENGTH characters")
        }

        val duplicateExists = existingNames.any { it.equals(trimmed, ignoreCase = true) }
        if (duplicateExists) {
            return ValidationResult.Invalid("A playlist named \"$trimmed\" already exists")
        }

        return ValidationResult.Valid(trimmed)
    }
}
