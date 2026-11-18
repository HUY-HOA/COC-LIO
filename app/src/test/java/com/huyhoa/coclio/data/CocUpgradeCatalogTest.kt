package com.huyhoa.coclio.data

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CocUpgradeCatalogTest {
    @Test
    fun sumsUpgradeResourcesOresAndDurationsAcrossLevels() {
        val entry = CocUpgradeEntry(
            dataId = 4000008,
            id = "dragon",
            name = "Dragon",
            description = "",
            base = "home",
            category = "troop",
            rarity = "",
            hero = "",
            maxLevel = 3,
            properties = JSONObject(),
            levels = listOf(
                CocUpgradeLevel(1, JSONObject("""{"level":1}""")),
                CocUpgradeLevel(
                    2,
                    JSONObject(
                        """{
                            "level":2,
                            "upgradeCost":250000,
                            "upgradeCostResource":"Dark Elixir",
                            "upgradeShinyOre":20,
                            "upgradeGlowingOre":3,
                            "upgradeStarryOre":1,
                            "upgradeTime":{"days":1,"hours":2,"minutes":0,"seconds":0}
                        }"""
                    )
                ),
                CocUpgradeLevel(
                    3,
                    JSONObject(
                        """{
                            "level":3,
                            "researchCost":500000,
                            "researchCostResource":"Elixir",
                            "researchTime":{"days":0,"hours":2,"minutes":30,"seconds":0}
                        }"""
                    )
                )
            )
        )

        val totals = CocUpgradeCatalog.upgradeTotals(entry, 1, 3)

        assertEquals(250000L, totals.resources["Dark Elixir"])
        assertEquals(500000L, totals.resources["Elixir"])
        assertEquals(20L, totals.resources["Quặng sáng"])
        assertEquals(3L, totals.resources["Quặng phát sáng"])
        assertEquals(1L, totals.resources["Quặng sao"])
        assertEquals(102600L, totals.durationSeconds)
    }

    @Test
    fun doesNotCountTheCostOrFullDurationOfAnUpgradeAlreadyInProgress() {
        val entry = CocUpgradeEntry(
            dataId = 4000008,
            id = "dragon",
            name = "Dragon",
            description = "",
            base = "home",
            category = "troop",
            rarity = "",
            hero = "",
            maxLevel = 3,
            properties = JSONObject(),
            levels = listOf(
                CocUpgradeLevel(
                    2,
                    JSONObject(
                        """{
                            "level":2,
                            "upgradeCost":250000,
                            "upgradeCostResource":"Dark Elixir",
                            "upgradeTime":{"days":1,"hours":0,"minutes":0,"seconds":0}
                        }"""
                    )
                ),
                CocUpgradeLevel(
                    3,
                    JSONObject(
                        """{
                            "level":3,
                            "upgradeCost":400000,
                            "upgradeCostResource":"Dark Elixir",
                            "upgradeTime":{"days":2,"hours":0,"minutes":0,"seconds":0}
                        }"""
                    )
                )
            )
        )

        val totals = CocUpgradeCatalog.upgradeTotals(
            entry = entry,
            currentLevel = 1,
            targetLevel = 3,
            skipFirstInProgress = true
        )

        assertEquals(400000L, totals.resources["Dark Elixir"])
        assertEquals(172800L, totals.durationSeconds)
    }
    @Test
    fun keepsSuperchargeTiersSeparateAndCalculatesTheirCostsAndTime() {
        val entry = CocUpgradeEntry(
            dataId = 1000077,
            id = "monolith",
            name = "Monolith",
            description = "",
            base = "home",
            category = "defense",
            rarity = "",
            hero = "",
            maxLevel = 5,
            properties = JSONObject(),
            levels = listOf(
                CocUpgradeLevel(4, JSONObject("""{"level":4,"buildCost":300000,"buildCostResource":"Dark Elixir","buildTime":{"days":11}}""")),
                CocUpgradeLevel(5, JSONObject("""{"level":5,"buildCost":470000,"buildCostResource":"Dark Elixir","buildTime":{"days":15}}"""))
            ),
            superchargeLevels = listOf(
                CocUpgradeLevel(1, JSONObject("""{"level":1,"supercharge":true,"buildCost":220000,"buildCostResource":"Dark Elixir","buildTime":{"days":5}}""")),
                CocUpgradeLevel(2, JSONObject("""{"level":2,"supercharge":true,"buildCost":200000,"buildCostResource":"Dark Elixir","buildTime":{"days":7}}"""))
            )
        )

        assertEquals(2, entry.superchargeLevels.size)
        assertTrue(entry.superchargeLevels.all { it.isSupercharge })
        assertEquals(false, entry.levels.any { it.isSupercharge })

        val regular = CocUpgradeCatalog.upgradeTotals(entry, 3, 5)
        val supercharge = CocUpgradeCatalog.superchargeTotals(entry, 0, 2)

        assertEquals(770000L, regular.resources["Dark Elixir"])
        assertEquals(2246400L, regular.durationSeconds)
        assertEquals(420000L, supercharge.resources["Dark Elixir"])
        assertEquals(1036800L, supercharge.durationSeconds)
    }

    @Test
    fun findsCatalogEntriesByNumericIdAndAccentInsensitiveVietnameseName() {
        val entry = CocUpgradeEntry(
            dataId = 28000006,
            id = "minion-prince",
            name = "Minion Prince",
            description = "",
            base = "home",
            category = "hero",
            rarity = "",
            hero = "",
            maxLevel = 95,
            properties = JSONObject(),
            levels = emptyList(),
            aliases = listOf("Prince")
        )

        assertEquals(28000006, CocUpgradeCatalog.search(listOf(entry), "28000006").single().dataId)
        assertEquals(28000006, CocUpgradeCatalog.search(listOf(entry), "hoang tu tieu quy").single().dataId)
        assertEquals(28000006, CocUpgradeCatalog.search(listOf(entry), "Prince").single().dataId)
    }

    @Test
    fun filtersLookupByLocalizedHeroAndEquipmentRarity() {
        val equipment = CocUpgradeEntry(
            dataId = 90000010,
            id = "giant-gauntlet",
            name = "Giant Gauntlet",
            description = "",
            base = "home",
            category = "hero-equipment",
            rarity = "epic",
            hero = "barbarian-king",
            maxLevel = 27,
            properties = JSONObject(),
            levels = emptyList()
        )

        assertEquals(
            90000010,
            CocUpgradeCatalog.search(
                entries = listOf(equipment),
                query = "vua man di",
                categoryFilter = "hero-equipment",
                rarityFilter = "epic",
                heroFilter = "barbarian-king"
            ).single().dataId
        )
        assertTrue(
            CocUpgradeCatalog.search(
                entries = listOf(equipment),
                query = "",
                categoryFilter = "hero-equipment",
                rarityFilter = "common"
            ).isEmpty()
        )
    }

    @Test
    fun unlocksOnlySuperchargeTiersAllowedByTownHall() {
        val entry = CocUpgradeEntry(
            dataId = 1000077,
            id = "monolith",
            name = "Monolith",
            description = "",
            base = "home",
            category = "defense",
            rarity = "",
            hero = "",
            maxLevel = 5,
            properties = JSONObject(),
            levels = listOf(CocUpgradeLevel(5, JSONObject("""{"level":5,"townHallRequired":17}"""))),
            superchargeLevels = listOf(
                CocUpgradeLevel(1, JSONObject("""{"level":1,"supercharge":true,"townHallRequired":18}""")),
                CocUpgradeLevel(2, JSONObject("""{"level":2,"supercharge":true,"townHallRequired":19}"""))
            )
        )

        assertNull(CocUpgradeCatalog.superchargeMaxAtHall(entry, 17))
        assertEquals(1, CocUpgradeCatalog.superchargeMaxAtHall(entry, 18))
        assertEquals(2, CocUpgradeCatalog.superchargeMaxAtHall(entry, 19))
    }


    @Test
    fun appliesCurrentTownHallNineteenMaxLevelsWithoutInventingMissingCosts() {
        val king = CocUpgradeEntry(
            dataId = 28000000,
            id = "barbarian-king",
            name = "Barbarian King",
            description = "",
            base = "home",
            category = "hero",
            rarity = "",
            hero = "",
            maxLevel = 110,
            properties = JSONObject(),
            levels = (1..110).map { level ->
                CocUpgradeLevel(level, JSONObject().put("level", level))
            }
        )

        assertEquals(115, CocUpgradeCatalog.currentMaxLevel(king))
        assertEquals((111..115).toList(), CocUpgradeCatalog.missingLevels(king, 110, 115))
        assertEquals(110, CocUpgradeCatalog.maxLevelAtHall(king, 18, mapOf(king.dataId to king)))
        assertEquals(115, CocUpgradeCatalog.maxLevelAtHall(king, 19, mapOf(king.dataId to king)))
    }

    @Test
    fun updatesAngryJellyMaximumAtTownHallEighteen() {
        val pet = CocUpgradeEntry(
            dataId = 73000011,
            id = "angry-jelly",
            name = "Angry Jelly",
            description = "",
            base = "home",
            category = "pet",
            rarity = "",
            hero = "",
            maxLevel = 10,
            properties = JSONObject(),
            levels = (1..10).map { level ->
                CocUpgradeLevel(level, JSONObject().put("level", level))
            }
        )

        assertEquals(15, CocUpgradeCatalog.currentMaxLevel(pet))
        assertEquals(10, CocUpgradeCatalog.maxLevelAtHall(pet, 17, mapOf(pet.dataId to pet)))
        assertEquals(15, CocUpgradeCatalog.maxLevelAtHall(pet, 18, mapOf(pet.dataId to pet)))
    }

    @Test
    fun reportsMissingUpgradeLevelsInsteadOfTreatingPartialTotalsAsComplete() {
        val entry = CocUpgradeEntry(
            dataId = 4000008,
            id = "dragon",
            name = "Dragon",
            description = "",
            base = "home",
            category = "troop",
            rarity = "",
            hero = "",
            maxLevel = 6,
            properties = JSONObject(),
            levels = listOf(
                CocUpgradeLevel(1, JSONObject("""{"level":1}""")),
                CocUpgradeLevel(2, JSONObject("""{"level":2,"researchCost":100,"researchCostResource":"Elixir"}""")),
                CocUpgradeLevel(4, JSONObject("""{"level":4,"researchCost":300,"researchCostResource":"Elixir"}""")),
                CocUpgradeLevel(6, JSONObject("""{"level":6,"researchCost":500,"researchCostResource":"Elixir"}"""))
            )
        )

        assertEquals(listOf(3, 5), CocUpgradeCatalog.missingLevels(entry, 1, 6))
        assertEquals(listOf(5), CocUpgradeCatalog.missingLevels(entry, 4, 6))
        assertTrue(CocUpgradeCatalog.missingLevels(entry, 6, 6).isEmpty())
        assertEquals(900L, CocUpgradeCatalog.upgradeTotals(entry, 1, 6).resources["Elixir"])
    }
}
