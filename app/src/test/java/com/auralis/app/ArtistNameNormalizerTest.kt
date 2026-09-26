package com.auralis.app

import com.auralis.app.core.util.ArtistNameNormalizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests verifying artist name normalization and equivalence rules according to Melofi specification:
 * - Leading/trailing whitespace trimmed
 * - Repeated whitespace collapsed
 * - Case differences resolved to identical identity ("Susu", "susu", " SUSU ")
 * - Distinct artists kept distinct ("Susu" vs "Susi")
 * - Meaningful punctuation, accents, and symbols preserved
 */
class ArtistNameNormalizerTest {

    @Test
    fun normalize_handlesCaseDifferences() {
        val n1 = ArtistNameNormalizer.normalize("Susu")
        val n2 = ArtistNameNormalizer.normalize("susu")
        val n3 = ArtistNameNormalizer.normalize("SUSU")

        assertEquals("susu", n1)
        assertEquals("susu", n2)
        assertEquals("susu", n3)
        assertEquals(n1, n2)
        assertEquals(n2, n3)
    }

    @Test
    fun normalize_handlesWhitespaceVariations() {
        val n1 = ArtistNameNormalizer.normalize("  Susu  ")
        val n2 = ArtistNameNormalizer.normalize("\tSUSU\n")
        val n3 = ArtistNameNormalizer.normalize("Su   su")

        assertEquals("susu", n1)
        assertEquals("susu", n2)
        assertEquals("su su", n3)
    }

    @Test
    fun normalize_distinguishesDifferentArtists() {
        val susu = ArtistNameNormalizer.normalize("Susu")
        val susi = ArtistNameNormalizer.normalize("Susi")

        assertEquals("susu", susu)
        assertEquals("susi", susi)
        assertFalse("Susu and Susi must be distinct artist identities", susu == susi)
    }

    @Test
    fun areEquivalent_identifiesEquivalenceCorrectly() {
        assertTrue(ArtistNameNormalizer.areEquivalent("Susu", "susu"))
        assertTrue(ArtistNameNormalizer.areEquivalent(" Susu ", "SUSU"))
        assertTrue(ArtistNameNormalizer.areEquivalent("Ananya", "  ananya  "))
        assertFalse(ArtistNameNormalizer.areEquivalent("Susu", "Susi"))
        assertFalse(ArtistNameNormalizer.areEquivalent("Artist A", "Artist B"))
    }

    @Test
    fun cleanDisplayName_preservesCasingAndAccents() {
        val clean1 = ArtistNameNormalizer.cleanDisplayName("  Beyoncé  ")
        val clean2 = ArtistNameNormalizer.cleanDisplayName("AC / DC")
        val clean3 = ArtistNameNormalizer.cleanDisplayName("  Su    su  ")

        assertEquals("Beyoncé", clean1)
        assertEquals("AC / DC", clean2)
        assertEquals("Su su", clean3)
    }

    @Test
    fun cleanDisplayName_handlesNullAndBlank() {
        assertEquals("Unknown Artist", ArtistNameNormalizer.cleanDisplayName(null))
        assertEquals("Unknown Artist", ArtistNameNormalizer.cleanDisplayName(""))
        assertEquals("Unknown Artist", ArtistNameNormalizer.cleanDisplayName("   "))
    }
}
