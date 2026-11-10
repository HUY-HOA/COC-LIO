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

}
