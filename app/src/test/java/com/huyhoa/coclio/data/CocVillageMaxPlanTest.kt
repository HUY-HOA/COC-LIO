package com.huyhoa.coclio.data

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CocVillageMaxPlanTest {
    @Test
    fun multipliesBuildingUpgradeCostsByTheNumberOfCopies() {
        val entry = testEntry(
            id = 1001,
            category = "defense",
            levels = listOf(
                testLevel(1),
                testLevel(2, 100L, "Gold", 86_400L),
                testLevel(3, 200L, "Gold", 172_800L)
            )
        )
        val summary = calculate(
            item = testItem(dataId = 1001, tab = "buildings", level = 1, count = 3),
            entry = entry
        )

        assertEquals(900L, summary.resources["Gold"])
        assertEquals(777_600L, summary.durationSeconds)
        assertEquals(3, summary.ownedCopies)
        assertEquals(3, summary.lines.single().quantity)
    }

    @Test
    fun doesNotMultiplyResearchCostsByTheNumberOfTroopsInTheVillageExport() {
        val entry = testEntry(
            id = 1002,
            category = "troop",
            levels = listOf(
                testLevel(1),
                testLevel(2, 100L, "Elixir", 86_400L),
                testLevel(3, 200L, "Elixir", 172_800L)
            )
        )
        val summary = calculate(
            item = testItem(dataId = 1002, tab = "army", level = 1, count = 40),
            entry = entry
        )

        assertEquals(300L, summary.resources["Elixir"])
        assertEquals(259_200L, summary.durationSeconds)
        assertEquals(1, summary.ownedCopies)
        assertEquals(1, summary.lines.single().quantity)
    }

    @Test
    fun subtractsOneAlreadyPaidUpgradeAndAddsItsRemainingTimer() {
        val entry = testEntry(
            id = 1003,
            category = "defense",
            levels = listOf(
                testLevel(1),
                testLevel(2, 100L, "Gold", 86_400L),
                testLevel(3, 200L, "Gold", 172_800L)
            )
        )
        val summary = calculate(
            item = testItem(
                dataId = 1003,
                tab = "buildings",
                level = 1,
                count = 2,
                timerSeconds = 86_400L
            ),
            entry = entry,
            snapshotTimestamp = 100L,
            nowSeconds = 130L
        )

        assertEquals(500L, summary.resources["Gold"])
        assertEquals(518_370L, summary.durationSeconds)
        assertTrue(summary.lines.single().upgradeInProgress)
    }

    @Test
    fun reportsMissingUpgradeLevelsAndDoesNotCountUnconfirmedItems() {
        val entry = testEntry(
            id = 1004,
            category = "troop",
            levels = listOf(testLevel(1), testLevel(2, 100L, "Elixir", 86_400L)),
            maxLevel = 3
        )
        val unconfirmedEntry = testEntry(
            id = 1005,
            category = "troop",
            levels = listOf(testLevel(1), testLevel(2, 50L, "Elixir", 43_200L))
        )
        val owned = testItem(dataId = 1004, tab = "army", level = 1, count = 1)
        val unconfirmed = testItem(dataId = 1005, tab = "army", level = 1, count = 0)
        val summary = calculate(
            items = listOf(owned, unconfirmed),
            entry = entry,
            additionalEntries = listOf(unconfirmedEntry)
        )

        assertEquals(listOf(3), summary.lines.single().missingLevels)
        assertEquals(1, summary.incompleteItems)
        assertEquals(1, summary.unconfirmedItems)
    }

    private fun calculate(
        item: VillageItem? = null,
        items: List<VillageItem> = listOfNotNull(item),
        entry: CocUpgradeEntry,
        additionalEntries: List<CocUpgradeEntry> = emptyList(),
        snapshotTimestamp: Long = 100L,
        nowSeconds: Long = 100L
    ): CocVillageMaxPlanSummary {
        val snapshot = VillageSnapshot(
            tag = "#TEST",
            timestamp = snapshotTimestamp,
            items = items,
            extras = emptyMap(),
            rawJson = "{}"
        )
        return CocVillageMaxPlanCalculator.calculate(
            snapshot = snapshot,
            base = "home",
            catalog = (listOf(entry) + additionalEntries).associateBy { it.dataId },
            nowSeconds = nowSeconds
        )
    }

    private fun testItem(
        dataId: Int,
        tab: String,
        level: Int,
        count: Int,
        timerSeconds: Long? = null
    ) = VillageItem(
        sectionKey = if (tab == "buildings") "buildings" else "units",
        sectionLabel = if (tab == "buildings") "Công trình" else "Quân đội",
        tab = tab,
        base = "home",
        dataId = dataId,
        level = level,
        count = count,
        timerSeconds = timerSeconds,
        helperCooldownSeconds = null,
        gearUp = false,
        weaponLevel = null,
        details = emptyList()
    )

    private fun testEntry(
        id: Int,
        category: String,
        levels: List<CocUpgradeLevel>,
        maxLevel: Int = 3
    ) = CocUpgradeEntry(
        dataId = id,
        id = "test-$id",
        name = "Test $id",
        description = "",
        base = "home",
        category = category,
        rarity = "",
        hero = "",
        maxLevel = maxLevel,
        properties = JSONObject(),
        levels = levels
    )

    private fun testLevel(
        level: Int,
        cost: Long = 0L,
        resource: String = "",
        durationSeconds: Long = 0L
    ): CocUpgradeLevel {
        val raw = JSONObject().put("level", level)
        if (cost > 0L) {
            raw.put("upgradeCost", cost).put("upgradeCostResource", resource)
        }
        if (durationSeconds > 0L) {
            raw.put(
                "upgradeTime",
                JSONObject()
                    .put("days", durationSeconds / 86_400L)
                    .put("hours", durationSeconds % 86_400L / 3_600L)
                    .put("minutes", durationSeconds % 3_600L / 60L)
                    .put("seconds", durationSeconds % 60L)
            )
        }
        return CocUpgradeLevel(level, raw)
    }
}
