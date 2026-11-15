package com.huyhoa.coclio.data

import org.json.JSONObject
import org.junit.Assert.assertEquals
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
}
