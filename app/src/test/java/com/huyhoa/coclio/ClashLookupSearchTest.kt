package com.huyhoa.coclio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ClashLookupSearchTest {
    @Test
    fun playerAndClanTagsWorkWithOrWithoutHash() {
        assertEquals(listOf("#P2Q8Y", "P2Q8Y"), lookupSearchCandidates("#P2Q8Y"))
        assertEquals(listOf("#P2Q8Y", "P2Q8Y"), lookupSearchCandidates("P2Q8Y"))
    }

    @Test
    fun letterOInCopiedTagIsNormalizedToZero() {
        assertEquals("#P0Q8Y", lookupSearchCandidates("#POQ8Y").first())
    }

    @Test
    fun multiWordNameRetriesIndividualWords() {
        val candidates = lookupSearchCandidates("Royal Clan")

        assertEquals("Royal Clan", candidates.first())
        assertTrue(candidates.contains("Royal"))
        assertTrue(candidates.contains("Clan"))
    }

    @Test
    fun decorativeNameGetsNormalizedFallback() {
        val candidates = lookupSearchCandidates("𝑹𝒐𝒚𝒂𝒍 ⚔️ Clan")

        assertEquals("𝑹𝒐𝒚𝒂𝒍 ⚔️ Clan", candidates.first())
        assertTrue(candidates.contains("Royal Clan"))
        assertTrue(candidates.contains("Royal"))
        assertTrue(candidates.contains("Clan"))
    }
}
