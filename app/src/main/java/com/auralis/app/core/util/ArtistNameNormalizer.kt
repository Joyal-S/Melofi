package com.auralis.app.core.util

import java.util.Locale

/**
 * Utility for normalizing artist names to enforce unique artist identities across Melofi.
 *
 * Normalization rules:
 * - Trims leading and trailing whitespace
 * - Collapses consecutive whitespace characters (spaces, tabs, newlines) into a single standard space
 * - Converts all characters to lowercase using invariant [Locale.ROOT]
 * - Preserves meaningful characters (accents, punctuation, numbers, casing in clean display names)
 *
 * Examples:
 * - "Susu", "susu", " SUSU ", "  Su   su  " normalize to "susu" / "su su"
 * - "Susu" and "Susi" normalize to "susu" and "susi" (distinct identities)
 */
object ArtistNameNormalizer {

    private val MULTI_SPACE_REGEX = Regex("\\s+")

    /**
     * Produces a normalized key used for unique constraints, indexing, and equivalence checks.
     */
    fun normalize(name: String?): String {
        if (name.isNullOrBlank()) return ""
        return name
            .trim()
            .replace(MULTI_SPACE_REGEX, " ")
            .lowercase(Locale.ROOT)
    }

    /**
     * Produces a sanitized, clean display name by trimming leading/trailing whitespace
     * and collapsing internal multi-spaces, while preserving original casing and accents.
     */
    fun cleanDisplayName(name: String?): String {
        if (name.isNullOrBlank()) return "Unknown Artist"
        val cleaned = name.trim().replace(MULTI_SPACE_REGEX, " ")
        return cleaned.ifEmpty { "Unknown Artist" }
    }

    /**
     * Checks if two artist name strings represent the same artist identity.
     */
    fun areEquivalent(name1: String?, name2: String?): Boolean {
        val norm1 = normalize(name1)
        val norm2 = normalize(name2)
        if (norm1.isEmpty() && norm2.isEmpty()) return true
        return norm1 == norm2
    }
}
