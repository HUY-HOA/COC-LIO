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
    fun decoratedVietnamesePlayerNameMatchesEveryMeaningfulWord() {
        val query = "HUY⭐⭐⭐HÒA"
        val candidates = lookupSearchCandidates(query)

        assertTrue(candidates.contains("HUY HOA"))
        assertTrue(candidates.contains("HUYHOA"))
        assertEquals("huy hoa", lookupSearchNameKey(query))
        assertEquals(1000, lookupSearchMatchScore("HUY⭐⭐⭐HÒA", query))
        assertTrue(lookupSearchMatchScore("HUY HÒA COC", query) > 0)
        assertEquals(0, lookupSearchMatchScore("HUY GAMER", query))
        assertEquals(0, lookupSearchMatchScore("HÒA GAMER", query))
    }

    @Test
    fun exactNameSearchResultsAreRankedAheadOfPartialMatches() {
        val exact = lookupEntry(JSONObject("""{"name":"HUY⭐⭐⭐HÒA","tag":"#P2Q8Y"}"""), false)
        val unrelated = lookupEntry(JSONObject("""{"name":"HUY GAMER","tag":"#P2Q8Y"}"""), false)
        val matching = rankLookupSearchEntries(listOf(unrelated, exact), "HUY⭐⭐⭐HÒA")

        assertEquals(listOf(exact), matching)
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

    @Test
    fun playerDoesNotReceiveClanWarWinStreak() {
        val player = JSONObject(
            """{"name":"Player","tag":"#P2Q8Y","war_win_streak":12}"""
        )

        val entry = lookupEntry(player, isClan = false)

        assertEquals("", entry.streak)
    }

    @Test
    fun clanMemberObjectUsesCountNotObjectPropertyCount() {
        val clan = JSONObject(
            """{"name":"Clan","tag":"#P2Q8Y","members":{"count":38,"items":[]}}"""
        )

        val entry = lookupEntry(clan, isClan = true)

        assertEquals("38", entry.members)
    }

    @Test
    fun displayFormattingUsesUnitsForPercentAndDuration() {
        assertEquals("99%", lookupDisplayValue("destructionPercentage", 99))
        assertEquals("75%", lookupDisplayValue("tripleRate", 0.75))
        assertEquals("1 giờ", lookupDisplayValue("attackSeconds", 3600))
    }

    @Test
    fun largeCountsAreGroupedForVietnameseDisplay() {
        val formatted = formatLookupCount("1234567")

        assertTrue(formatted != "1234567")
        assertTrue(formatted.contains("1"))
    }

    @Test
    fun statIconsUseTheCorrectSemanticAsset() {
        assertEquals(R.drawable.ic_coc_members, lookupGameIconResource("memberCount"))
        assertEquals(R.drawable.ic_coc_player, lookupGameIconResource("expLevel"))
        assertEquals(R.drawable.ic_coc_level, lookupGameIconResource("level"))
        assertEquals(R.drawable.ic_coc_hero, lookupGameIconResource("heroLevel"))
        assertEquals(R.drawable.ic_coc_spell, lookupGameIconResource("spellLevel"))
        assertEquals(R.drawable.ic_coc_swords, lookupGameIconResource("troops"))
        assertEquals(R.drawable.ic_coc_defense, lookupGameIconResource("defenses"))
        assertEquals(R.drawable.ic_coc_ore_glowy, lookupGameIconResource("glowyOre"))
        assertEquals(R.drawable.ic_coc_percent, lookupGameIconResource("destructionPercentage"))
    }

    @Test
    fun wrappedPlayerResultStaysPlayerAndNeverUsesClanCrest() {
        val wrapped = JSONObject(
            """{"player":{"name":"Player","tag":"#P2Q8Y","townHallLevel":18,"expLevel":245,"badge_url":"https://example.com/clan.png","clan":{"name":"Clan","badge":"https://example.com/crest.png"}}}"""
        )

        val entry = lookupEntry(wrapped, isClan = false)

        assertEquals("Player", entry.name)
        assertEquals("#P2Q8Y", entry.tag)
        assertEquals(false, entry.isClan)
        assertEquals("18", entry.townHall)
        assertEquals("245", entry.playerLevel)
        assertEquals("Clan", entry.clanName)
        assertEquals("", entry.badgeUrl)
        assertEquals("https://example.com/crest.png", entry.clanBadgeUrl)
    }

    @Test
    fun wrappedClanResultStaysClanAndDoesNotShowPlayerStats() {
        val wrapped = JSONObject(
            """{"clan":{"name":"Clan","tag":"#P2Q8Y","clanLevel":20,"memberCount":48,"badge_url":"https://example.com/crest.png","townHallLevel":18}}"""
        )

        val entry = lookupEntry(wrapped, isClan = true)

        assertEquals("Clan", entry.name)
        assertEquals(true, entry.isClan)
        assertEquals("20", entry.level)
        assertEquals("48", entry.members)
        assertEquals("", entry.townHall)
        assertEquals("https://example.com/crest.png", entry.badgeUrl)
    }


    @Test
    fun clanTagAndClanLevelUseDifferentSemanticIcons() {
        assertEquals(R.drawable.ic_coc_tag, lookupGameIconResource("clanTag"))
        assertEquals(R.drawable.ic_coc_clan, lookupGameIconResource("clanLevel"))
    }

    @Test
    fun timeAndRateFieldsUseTheirMatchingIcons() {
        assertEquals(R.drawable.ic_coc_time, lookupGameIconResource("lastActive"))
        assertEquals(R.drawable.ic_coc_time, lookupGameIconResource("joinedAt"))
        assertEquals(R.drawable.ic_coc_percent, lookupGameIconResource("averageOffense"))
        assertEquals(R.drawable.ic_coc_percent, lookupGameIconResource("tripleRate"))
    }

    @Test
    fun fractionalPercentagesAreNormalizedToPercentUnits() {
        assertEquals("98,5%", lookupDisplayValue("destructionPercentage", 0.985))
        assertEquals("98,5%", lookupDisplayValue("tripleRate", 0.985))
        assertEquals("99%", lookupDisplayValue("destructionPercentage", 99))
    }

    @Test
    fun unavailableSentinelIsNotShownAsNegativeOne() {
        assertEquals("Chưa có dữ liệu", lookupDisplayValue("trophies", -1))
    }

    @Test
    fun decimalCountersAreLocalizedAndAverageDurationsHaveUnits() {
        assertEquals("1.234,5", lookupDisplayValue("attackRate", 1234.5))
        assertEquals("1 giờ", lookupDisplayValue("averageDuration", 3600.0))
    }

}
