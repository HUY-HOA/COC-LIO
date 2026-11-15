package com.huyhoa.coclio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ClashSearchNormalizerTest {
    @Test
    fun removesDecorativeSymbolsAndVietnameseDiacritics() {
        assertEquals("HUYHOA", normalizeClashSearchText("HUY⭐⭐⭐HÒA"))
        assertEquals("LIOONE", normalizeClashSearchText("LIO⭐⭐⭐ONE"))
    }

    @Test
    fun buildsSearchVariantsAroundDecorativeSymbols() {
        val terms = clashSearchTerms("HUY⭐⭐⭐HÒA")
        assertTrue(terms.any { it.equals("HUY", ignoreCase = true) })
        assertTrue(terms.any { it.equals("HÒA", ignoreCase = true) })
        assertTrue(terms.any { it.equals("HUYHOA", ignoreCase = true) })
    }

    @Test
    fun matchesNamesWithSymbolsDiacriticsAndDifferentSpacing() {
        assertTrue(clashNameMatchesSearch("HUY⭐⭐⭐HÒA", "HUY⭐⭐⭐HÒA"))
        assertTrue(clashNameMatchesSearch("HUY⭐⭐⭐HÒA", "huy hoa"))
        assertTrue(clashNameMatchesSearch("LIO⭐⭐⭐ONE", "LIO ONE"))
        assertTrue(clashNameMatchesSearch("LIO ⭐⭐⭐ ONE", "LIO⭐⭐⭐ONE"))
    }

    @Test
    fun rejectsNamesMissingOneOfTheSearchParts() {
        assertFalse(clashNameMatchesSearch("LIO⭐TWO", "LIO⭐⭐⭐ONE"))
    }

    @Test
    fun stripsZeroWidthAndVariationCharacters() {
        assertEquals("LIOONE", normalizeClashSearchText("LIO\u200B⭐\uFE0FONE"))
    }
}
