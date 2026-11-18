package com.huyhoa.coclio.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VillageParserTest {
    @Test
    fun parsesHomeBuilderGearUpModulesAndHelperCooldown() {
        val json = """
            {
              "tag":"#TEST123",
              "timestamp":1794320907,
              "buildings":[
                {"data":1000001,"lvl":17},
                {"data":1000000,"lvl":12,"cnt":2},
                {"data":1000008,"lvl":14,"gear_up":1},
                {"data":1000009,"lvl":21,"gear_up":1,"timer":200},
                {"data":1000097,"types":[
                  {"data":103000011,"modules":[{"data":102000033,"lvl":1}]}
                ]}
              ],
              "traps":[{"data":12000005,"lvl":6,"timer":100}],
              "helpers":[{"data":93000002,"lvl":2,"helper_cooldown":300}],
              "buildings2":[{"data":1000034,"lvl":10}],
              "equipment":[{"data":90000061,"lvl":1}]
            }
        """.trimIndent()

        val snapshot = VillageParser.parse(json)

        assertEquals("#TEST123", snapshot.tag)
        assertEquals(17, snapshot.townHallLevel)
        assertEquals(10, snapshot.builderHallLevel)
        assertEquals(1, snapshot.upgrades.size)
        assertEquals(1, snapshot.gearUps.size)
        assertEquals(300L, snapshot.items.first { it.sectionKey == "helpers" }.helperCooldownSeconds)
        assertEquals("Trại quân", CocNames.name(1000000, "buildings"))
        assertEquals("Mặt dây chuyền cổng không gian", CocNames.name(90000061, "equipment"))
        assertTrue(snapshot.items.first { it.dataId == 1000097 }.details.any { it.contains("Máu Nến nóng cấp 1") })
    }

    @Test
    fun preservesZeroCountEquipmentAsNotOwned() {
        val snapshot = VillageParser.parse(
            """{"tag":"#ZERO_COUNT","timestamp":1794320000,
                "equipment":[
                    {"data":90000061,"lvl":1,"cnt":0},
                    {"data":90000060,"lvl":2,"cnt":1},
                    {"data":90000059,"lvl":3}
                ]}"""
        )

        assertEquals(0, snapshot.items.first { it.dataId == 90000061 }.count)
        assertEquals(1, snapshot.items.first { it.dataId == 90000060 }.count)
        assertEquals(1, snapshot.items.first { it.dataId == 90000059 }.count)
    }

    @Test
    fun detectsUpgradeStartAndCompletionAcrossImports() {
        val before = VillageParser.parse(
            """{"tag":"#TEST123","timestamp":1794320000,"buildings":[{"data":1000008,"lvl":5,"cnt":1}]}"""
        )
        val running = VillageParser.parse(
            """{"tag":"#TEST123","timestamp":1794320100,"buildings":[{"data":1000008,"lvl":5,"timer":3600}]}"""
        )
        val completed = VillageParser.parse(
            """{"tag":"#TEST123","timestamp":1794324000,"buildings":[{"data":1000008,"lvl":6,"cnt":1}]}"""
        )

        val startedEvents = VillageParser.compare(before, running)
        val completedEvents = VillageParser.compare(running, completed)

        assertTrue(startedEvents.any { it.title == "Bắt đầu nâng cấp" && it.detail.contains("Đại bác") })
        assertTrue(completedEvents.any { it.title == "Đã hoàn thành nâng cấp" && it.completed })
    }

    @Test
    fun doesNotConfirmCompletionWhenUpgradeDisappearsWithoutNextLevel() {
        val before = VillageParser.parse(
            """{"tag":"#NO_FALSE_COMPLETE","timestamp":1794320000,"buildings":[{"data":1000008,"lvl":5,"timer":3600}]}"""
        )
        val after = VillageParser.parse(
            """{"tag":"#NO_FALSE_COMPLETE","timestamp":1794324000,"buildings":[{"data":1000008,"lvl":5}]}"""
        )

        val events = VillageParser.compare(before, after)

        assertTrue(events.any {
            it.title == "Nâng cấp không còn trong bộ đếm" && !it.completed
        })
        assertTrue(events.none { it.title == "Đã hoàn thành nâng cấp" && it.completed })
    }

    @Test
    fun gearUpTimerDoesNotBecomeNormalLevelUpgrade() {
        val snapshot = VillageParser.parse(
            """{"tag":"#TEST123","timestamp":1794320000,"buildings":[{"data":1000008,"lvl":14,"gear_up":1,"timer":7200}]}"""
        )

        assertEquals(0, snapshot.upgrades.size)
        assertEquals(1, snapshot.gearUps.size)
    }

    @Test
    fun normalizesMillisecondTimestampButKeepsGameTimerInSeconds() {
        val snapshot = VillageParser.parse(
            """{"tag":"#MILLISECONDS","timestamp":1794320000000,"buildings":[{"data":1000008,"lvl":5,"timer":3600}]}"""
        )

        assertEquals(1794320000L, snapshot.timestamp)
        assertEquals(3600L, snapshot.upgrades.single().timerSeconds)
    }

    @Test
    fun calculatesHomeBuilderCountAndFreeBuildersFromHutsAndActiveWork() {
        val snapshot = VillageParser.parse(
            """{"tag":"#BUILDERS","timestamp":1794320000,"buildings":[
                {"data":1000015,"lvl":5,"cnt":5},
                {"data":1000064,"lvl":1},
                {"data":1000008,"lvl":5,"timer":3600},
                {"data":1000009,"lvl":5,"timer":7200},
                {"data":1000001,"lvl":17}
            ]}"""
        )

        assertEquals(6, snapshot.homeBuilderTotal)
        assertEquals(2, snapshot.homeBuildersBusyAt(1794320100L))
        assertEquals(4, snapshot.homeBuilderTotal!! - snapshot.homeBuildersBusyAt(1794320100L))
        assertEquals(1, snapshot.homeBuildersBusyAt(1794325000L))
        assertEquals(0, snapshot.homeBuildersBusyAt(1794328000L))
    }


    @Test
    fun subtractsElapsedTimeSinceJsonWasExported() {
        val snapshot = VillageParser.parse(
            """{"tag":"#TIME_OFFSET","timestamp":1794320000,"buildings":[
                {"data":1000008,"lvl":5,"timer":7200}
            ]}"""
        )
        val item = snapshot.upgrades.single()

        assertEquals(7200L, item.remainingSecondsAt(snapshot.timestamp, snapshot.timestamp))
        assertEquals(7200L, item.remainingSecondsAt(snapshot.timestamp, snapshot.timestamp - 600L))
        assertEquals(5400L, item.remainingSecondsAt(snapshot.timestamp, snapshot.timestamp + 1800L))
        assertEquals(0L, item.remainingSecondsAt(snapshot.timestamp, snapshot.timestamp + 7200L))
        assertEquals(0L, item.remainingSecondsAt(snapshot.timestamp, snapshot.timestamp + 9000L))
    }

    @Test
    fun separatesActiveAndExpiredUpgradeTimersAtCurrentTime() {
        val snapshot = VillageParser.parse(
            """{"tag":"#ACTIVE","timestamp":1794320000,"buildings":[
                {"data":1000008,"lvl":5,"timer":3600}
            ]}"""
        )

        assertEquals(1, snapshot.activeUpgradesAt(1794323599L).size)
        assertEquals(0, snapshot.expiredUpgradesAt(1794323599L).size)
        assertEquals(0, snapshot.activeUpgradesAt(1794323600L).size)
        assertEquals(1, snapshot.expiredUpgradesAt(1794323600L).size)
    }

    @Test
    fun upgradeProgressMovesFromZeroToOneHundredAsCountdownExpires() {
        val snapshot = VillageParser.parse(
            """{"tag":"#PROGRESS","timestamp":1794320000,"buildings":[
                {"data":1000008,"lvl":5,"timer":7200}
            ]}"""
        )
        val item = snapshot.upgrades.single()

        assertEquals(0, item.progressPercentAt(snapshot.timestamp, snapshot.timestamp))
        assertEquals(0.25f, item.progressRatioAt(snapshot.timestamp, snapshot.timestamp + 1800L), 0.0001f)
        assertEquals(25, item.progressPercentAt(snapshot.timestamp, snapshot.timestamp + 1800L))
        assertEquals(50, item.progressPercentAt(snapshot.timestamp, snapshot.timestamp + 3600L))
        assertEquals(75, item.progressPercentAt(snapshot.timestamp, snapshot.timestamp + 5400L))
        assertEquals(100, item.progressPercentAt(snapshot.timestamp, snapshot.timestamp + 7200L))
        assertEquals(100, item.progressPercentAt(snapshot.timestamp, snapshot.timestamp + 9000L))
        assertEquals(0L, item.remainingSecondsAt(snapshot.timestamp, snapshot.timestamp + 7200L))
    }

    @Test
    fun petUpgradeCountsAsBusyHomeBuilder() {
        val snapshot = VillageParser.parse(
            """{"tag":"#PETS","timestamp":1794320000,"buildings":[
                {"data":1000015,"lvl":5,"cnt":5}
            ],"pets":[
                {"data":73000000,"lvl":5,"timer":3600}
            ]}"""
        )

        assertEquals(5, snapshot.homeBuilderTotal)
        assertEquals(1, snapshot.homeBuildersBusyAt(1794320100L))
        assertEquals(4, snapshot.homeBuilderTotal!! - snapshot.homeBuildersBusyAt(1794320100L))
    }

    @Test
    fun readsVillageNameWhenJsonProvidesIt() {
        val direct = VillageParser.parse(
            """{"tag":"#TEST123","timestamp":1794320000,"playerName":"Làng Chính"}"""
        )
        val nested = VillageParser.parse(
            """{"tag":"#TEST123","timestamp":1794320000,"player":{"name":"Làng Phụ"}}"""
        )
        val exportOnly = VillageParser.parse(
            """{"tag":"#TEST123","timestamp":1794320000}"""
        )

        assertEquals("Làng Chính", direct.villageName)
        assertEquals("Làng Phụ", nested.villageName)
        assertEquals(null, exportOnly.villageName)
    }


    @Test
    fun parsesCosmeticsBoostsAndUnknownJsonFields() {
        val snapshot = VillageParser.parse(
            """{"tag":"#EXTRAS","timestamp":1794320000,
                "buildings":[{"data":1000001,"lvl":16,"helper_recurrent":1}],
                "skins":[{"data":52000120,"cnt":1}],
                "sceneries":[{"data":51000001}],
                "decos2":[{"data":53000002,"cnt":2}],
                "skins2":[52000003],
                "boosts":{"lab_boost":3600,"builder_boost":1800},
                "custom_flag":"kept"}"""
        )

        assertTrue(snapshot.extraItems.any {
            it.sectionKey == "skins" && it.base == "home" && it.dataId == 52000120
        })
        assertTrue(snapshot.extraItems.any {
            it.sectionKey == "sceneries" && it.base == "home" && it.dataId == 51000001
        })
        assertTrue(snapshot.extraItems.any {
            it.sectionKey == "decos2" && it.base == "builder" && it.dataId == 53000002 && it.count == 2
        })
        assertTrue(snapshot.extraItems.any {
            it.sectionKey == "skins2" && it.base == "builder" && it.dataId == 52000003
        })
        assertTrue(snapshot.extraItems.any { it.value?.contains("Tăng tốc phòng thí nghiệm") == true })
        assertTrue(snapshot.extraItems.any { it.value?.contains("Tăng tốc thợ xây") == true })
        assertEquals("kept", snapshot.extraFields["custom_flag"])
        assertEquals("1", snapshot.items.single().properties["helper_recurrent"])
        assertEquals(16, snapshot.townHallLevel)
    }

    @Test
    fun mapsSiegeBarracksAndKeepsUnknownCosmeticIdsVisible() {
        assertEquals("Doanh trại công thành", CocNames.name(4000075, "siege_machines"))
        assertEquals("Trang phục #99999999", CocNames.name(99999999, "skins"))
        assertEquals("Phong cảnh #99999998", CocNames.name(99999998, "sceneries"))
    }

}
