package com.huyhoa.coclio

import android.content.Context
import org.json.JSONObject

internal data class SupplementalCatalogEntry(
    val name: String,
    val category: String,
    val description: String,
    val maxLevel: Int?,
    val imageUrl: String,
    val aliases: List<String>,
    val requirements: List<String>,
    val details: List<String>,
    val upgradeDurationSeconds: Long?,
    val upgradeCost: String,
    val rarity: String,
    val abilityType: String,
    val levelDetails: Map<Int, List<String>>
)

internal object LocalSupplementalCatalog {
    private const val ASSET_PATH = "coc_upgrade/supplemental_catalog.json"

    fun load(context: Context): Map<Int, SupplementalCatalogEntry> = runCatching {
        val json = context.applicationContext.assets.open(ASSET_PATH).bufferedReader().use { it.readText() }
        parse(json)
    }.getOrDefault(emptyMap())

    internal fun parse(json: String): Map<Int, SupplementalCatalogEntry> = runCatching {
        val rows = JSONObject(json).optJSONArray("catalog") ?: return@runCatching emptyMap()
        buildMap {
            for (index in 0 until rows.length()) {
                val item = rows.optJSONObject(index) ?: continue
                val rawId = item.opt("dataId") as? Number ?: continue
                val rawIdDouble = rawId.toDouble()
                val id = rawId.toLong()
                if (!rawIdDouble.isFinite() || rawIdDouble != id.toDouble() || id !in 1L..Int.MAX_VALUE.toLong()) continue
                val name = item.optString("name").trim().take(80)
                if (name.isBlank()) continue
                val category = item.optString("category").takeIf {
                    it in setOf("defense", "resources", "traps", "other", "troops", "heroes", "spells", "pets", "guardians", "equipment")
                }.orEmpty()
                val maxLevel = item.opt("maxLevel").let { value ->
                    (value as? Number)?.toInt()?.takeIf { it in 1..200 }
                }
                val levelDetailsJson = item.optJSONObject("levelDetails")
                val levelDetails = buildMap {
                    levelDetailsJson?.keys()?.forEach { rawLevel ->
                        val level = rawLevel.toIntOrNull()?.takeIf { it in 1..100 } ?: return@forEach
                        val values = levelDetailsJson.optJSONArray(rawLevel).toStringList(12, 120)
                        if (values.isNotEmpty()) put(level, values)
                    }
                }
                put(
                    id.toInt(),
                    SupplementalCatalogEntry(
                        name = name,
                        category = category,
                        description = item.optString("description").take(240),
                        maxLevel = maxLevel,
                        imageUrl = item.optString("imageUrl").takeIf(::isSafeHttps).orEmpty(),
                        aliases = item.optJSONArray("aliases").toStringList(10, 64),
                        requirements = item.optJSONArray("requirements").toStringList(4, 100),
                        details = item.optJSONArray("details").toStringList(6, 100),
                        upgradeDurationSeconds = item.opt("upgradeDurationSeconds").let { value ->
                            (value as? Number)?.toLong()?.takeIf { it in 1L..31_536_000L }
                        },
                        upgradeCost = item.optString("upgradeCost").trim().take(80),
                        rarity = item.optString("rarity").lowercase().takeIf {
                            it in setOf("common", "rare", "epic", "legendary")
                        }.orEmpty(),
                        abilityType = item.optString("abilityType").lowercase().takeIf {
                            it in setOf("active", "passive")
                        }.orEmpty(),
                        levelDetails = levelDetails
                    )
                )
            }
        }
    }.getOrDefault(emptyMap())

    private fun isSafeHttps(value: String): Boolean =
        value.length <= 2000 && value.startsWith("https://", ignoreCase = true)
}

private fun org.json.JSONArray?.toStringList(maxItems: Int, maxLength: Int): List<String> =
    this?.let { array ->
        (0 until minOf(array.length(), maxItems)).mapNotNull { index ->
            array.optString(index).trim().takeIf { it.isNotBlank() }?.take(maxLength)
        }
    }.orEmpty()
