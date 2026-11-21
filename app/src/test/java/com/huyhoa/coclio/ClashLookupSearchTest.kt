package com.huyhoa.coclio

import org.json.JSONObject
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
    @Test
    fun playerUsesLeagueBadgeButNeverClanCrest() {
        val player = JSONObject(
            """{
                "name":"Player",
                "tag":"#P2Q8Y",
                "badge_url":"https://example.com/clan-crest.png",
                "leagueTier":{"name":"Champion","iconUrls":{"medium":"https://example.com/league.png"}}
            }"""
        )

        val entry = lookupEntry(player, isClan = false)

        assertEquals("", entry.badgeUrl)
        assertEquals("https://example.com/league.png", entry.leagueIconUrl)
    }

    @Test
    fun clanUsesClanCrestAndNeverLeagueBadge() {
        val clan = JSONObject(
            """{
                "name":"Clan",
                "tag":"#P2Q8Y",
                "badge_url":"https://example.com/clan-crest.png",
                "leagueTier":{"name":"Champion","iconUrls":{"medium":"https://example.com/league.png"}},
                "memberCount":25,
                "clanLevel":18
            }"""
        )

        val entry = lookupEntry(clan, isClan = true)

        assertEquals("https://example.com/clan-crest.png", entry.badgeUrl)
        assertEquals("", entry.leagueIconUrl)
        assertEquals("25", entry.members)
        assertEquals("18", entry.level)
    }

}
