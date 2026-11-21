package com.huyhoa.coclio

import com.huyhoa.coclio.data.VillageItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class JsonDataExplorerLogicTest {
    private fun item(
        dataId: Int = 1000008,
        base: String = "home",
        sectionKey: String = "buildings",
        sectionLabel: String = "Công trình",
        level: Int? = 3,
        timerSeconds: Long? = null,
        gearUp: Boolean = false
    ) = VillageItem(
        sectionKey = sectionKey,
        sectionLabel = sectionLabel,
        tab = if (sectionKey.startsWith("buildings") || sectionKey.startsWith("traps")) "buildings" else "army",
        base = base,
        dataId = dataId,
        level = level,
        count = 1,
        timerSeconds = timerSeconds,
        helperCooldownSeconds = null,
        gearUp = gearUp,
        weaponLevel = null,
        details = emptyList()
    )

    @Test
    fun searchIgnoresVietnameseDiacriticsAndFindsDataIds() {
        val items = listOf(
            item(dataId = 1000001, sectionLabel = "Công trình"),
            item(dataId = 1000008, sectionLabel = "Phòng thủ")
        )

        assertEquals(
            listOf(1000001),
            filterJsonExplorerItems(items, "all", "all", "cong trinh", "name", 100L, 100L)
                .map { it.dataId }
        )
        assertEquals(
            listOf(1000008),
            filterJsonExplorerItems(items, "all", "all", "1000008", "name", 100L, 100L)
                .map { it.dataId }
        )
    }

    @Test
    fun statusFiltersSeparateNormalUpgradesGearUpsExpiredTimersAndIdleItems() {
        val items = listOf(
            item(dataId = 1, timerSeconds = 1_000L),
            item(dataId = 2, timerSeconds = 1_000L, gearUp = true),
            item(dataId = 3, timerSeconds = 20L),
            item(dataId = 4, timerSeconds = null)
        )

        assertEquals(
            listOf(1),
            filterJsonExplorerItems(items, "all", "upgrading", "", "name", 100L, 150L).map { it.dataId }
        )
        assertEquals(
            listOf(1, 2),
            filterJsonExplorerItems(items, "all", "running", "", "name", 100L, 150L)
                .map { it.dataId }.sorted()
        )
        assertEquals(
            listOf(2),
            filterJsonExplorerItems(items, "all", "gear", "", "name", 100L, 150L).map { it.dataId }
        )
        assertEquals(
            listOf(3),
            filterJsonExplorerItems(items, "all", "expired", "", "name", 100L, 150L).map { it.dataId }
        )
        assertEquals(
            listOf(4),
            filterJsonExplorerItems(items, "all", "idle", "", "name", 100L, 150L).map { it.dataId }
        )
    }

    @Test
    fun baseFilterAndLevelSortAreAppliedBeforeRendering() {
        val items = listOf(
            item(dataId = 1, level = 2),
            item(dataId = 2, base = "builder", sectionKey = "buildings2", level = 6),
            item(dataId = 3, level = 9)
        )

        val result = filterJsonExplorerItems(items, "home", "all", "", "level", 100L, 100L)

        assertEquals(listOf(3, 1), result.map { it.dataId })
        assertTrue(result.all { it.base == "home" })
    }

    @Test
    fun timerSortShowsSoonestFinishingUpgradeFirst() {
        val items = listOf(
            item(dataId = 1, timerSeconds = 900L),
            item(dataId = 2, timerSeconds = 200L),
            item(dataId = 3)
        )

        assertEquals(
            listOf(2, 1, 3),
            filterJsonExplorerItems(items, "all", "all", "", "timer", 100L, 150L).map { it.dataId }
        )
    }
}
