package com.huyhoa.coclio

import org.junit.Assert.assertEquals
import org.junit.Test

class RemoteContentParserTest {
    @Test
    fun preservesClashOfClansIdsAboveTwoMillion() {
        val state = parseRemoteContent(
            mapOf(
                "version" to 3,
                "enabled" to true,
                "catalog" to listOf(
                    mapOf(
                        "dataId" to 90_000_048L,
                        "name" to "Action Figure",
                        "category" to "equipment"
                    ),
                    mapOf(
                        "dataId" to 73_000_017L,
                        "name" to "Example Game Item",
                        "category" to "troops"
                    ),
                    mapOf(
                        "dataId" to 2_147_483_648L,
                        "name" to "Out of Int Range",
                        "category" to "other"
                    )
                )
            )
        )

        assertEquals(setOf(90_000_048, 73_000_017), state.catalog.keys)
        assertEquals("Action Figure", state.catalog[90_000_048]?.name)
        assertEquals(2, state.catalog.size)
    }
}
