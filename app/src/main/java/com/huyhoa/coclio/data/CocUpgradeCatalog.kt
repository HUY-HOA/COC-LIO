package com.huyhoa.coclio.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.Normalizer
import java.util.Locale

data class CocUpgradeLevel(
    val level: Int,
    val raw: JSONObject
) {
    val isSupercharge: Boolean get() = raw.optBoolean("supercharge")

    fun long(vararg keys: String): Long? = keys.firstNotNullOfOrNull { key ->
        if (raw.has(key) && !raw.isNull(key)) raw.optLong(key).takeIf { it >= 0L } else null
    }

    fun int(vararg keys: String): Int? = keys.firstNotNullOfOrNull { key ->
        if (raw.has(key) && !raw.isNull(key)) raw.optInt(key).takeIf { it >= 0 } else null
    }

    fun resource(): String = listOf(
        "upgradeCostResource",
        "researchCostResource",
        "buildCostResource"
    ).firstNotNullOfOrNull { key ->
        raw.optString(key).takeIf { it.isNotBlank() && it != "null" }
    }.orEmpty()

    fun cost(): Long = long("upgradeCost", "researchCost", "buildCost") ?: 0L

    fun durationSeconds(): Long {
        val time = sequenceOf("upgradeTime", "researchTime", "buildTime")
            .mapNotNull { raw.optJSONObject(it) }
            .firstOrNull() ?: return 0L
        return time.optLong("days") * 86400L +
            time.optLong("hours") * 3600L +
            time.optLong("minutes") * 60L +
            time.optLong("seconds")
    }

    fun oreCosts(): Map<String, Long> = buildMap {
        put("Quặng sáng", long("upgradeShinyOre") ?: 0L)
        put("Quặng phát sáng", long("upgradeGlowingOre") ?: 0L)
        put("Quặng sao", long("upgradeStarryOre") ?: 0L)
    }.filterValues { it > 0L }

    fun displayStats(): List<Pair<String, String>> {
        val ignored = setOf(
            "level", "townHallRequired", "builderHallLevelRequired",
            "heroHallLevelRequired", "laboratoryRequired", "petHouseLevelRequired",
            "blacksmithLevelRequired", "workshopLevelRequired", "starLabRequired",
            "upgradeCost", "upgradeCostResource", "researchCost", "researchCostResource",
            "buildCost", "buildCostResource", "upgradeTime", "researchTime", "buildTime",
            "upgradeShinyOre", "upgradeGlowingOre", "upgradeStarryOre", "images", "supercharge"
        )
        val result = mutableListOf<Pair<String, String>>()
        raw.keys().forEach { key ->
            if (key in ignored) return@forEach
            val value = raw.opt(key) ?: return@forEach
            when (value) {
                is JSONObject -> value.keys().forEach { nested ->
                    val nestedValue = value.opt(nested)
                    if (nestedValue != null && nestedValue != JSONObject.NULL) {
                        result += "${key}_${nested}" to nestedValue.toString()
                    }
                }
                is JSONArray -> result += key to value.toString()
                JSONObject.NULL -> Unit
                else -> result += key to value.toString()
            }
        }
        return result
    }
}

data class CocUpgradeEntry(
    val dataId: Int,
    val id: String,
    val name: String,
    val description: String,
    val base: String,
    val category: String,
    val rarity: String,
    val hero: String,
    val maxLevel: Int,
    val properties: JSONObject,
    val levels: List<CocUpgradeLevel>,
    val aliases: List<String> = emptyList(),
    val superchargeLevels: List<CocUpgradeLevel> = emptyList()
) {
    val sectionKey: String
        get() = when (category) {
            "hero-equipment" -> "equipment"
            "siege-machine" -> "siege_machines"
            "troop" -> if (base == "builder") "units2" else "units"
            "hero" -> if (base == "builder") "heroes2" else "heroes"
            "spell" -> "spells"
            "pet" -> "pets"
            "guardian" -> "guardians"
            "trap" -> if (base == "builder") "traps2" else "traps"
            else -> if (base == "builder") "buildings2" else "buildings"
        }

    val localizedName: String get() = CocNames.name(dataId, sectionKey)

    fun level(level: Int): CocUpgradeLevel? = levels.firstOrNull { it.level == level }
}

data class CocUpgradeTotals(
    val resources: Map<String, Long>,
    val durationSeconds: Long
)

data class CocUpgradeRequirement(
    val label: String,
    val level: Int
)

object CocUpgradeCatalog {
    private val facilityIds = mapOf(
        "heroHallLevelRequired" to "hero-hall",
        "laboratoryRequired" to "laboratory",
        "petHouseLevelRequired" to "pet-house",
        "blacksmithLevelRequired" to "blacksmith",
        "workshopLevelRequired" to "workshop",
        "barrackLevelRequired" to "barracks",
        "darkBarrackLevelRequired" to "dark-barracks",
        "spellFactoryLevelRequired" to "spell-factory",
        "darkSpellFactoryLevelRequired" to "dark-spell-factory",
        "starLabRequired" to "star-laboratory",
        "starLaboratoryRequired" to "star-laboratory",
        "builderBarracksLevelRequired" to "builder-barracks"
    )

    @Volatile
    private var cached: Map<Int, CocUpgradeEntry>? = null

    @Volatile
    private var cachedWarnings: List<String> = emptyList()

    fun warnings(context: Context): List<String> {
        all(context)
        return cachedWarnings
    }

    fun all(context: Context): Map<Int, CocUpgradeEntry> {
        cached?.let { return it }
        return synchronized(this) {
            cached ?: load(context.applicationContext).also { cached = it }
        }
    }

    fun get(context: Context, dataId: Int): CocUpgradeEntry? = all(context)[dataId]

    private fun load(context: Context): Map<Int, CocUpgradeEntry> {
        val result = linkedMapOf<Int, CocUpgradeEntry>()
        val warnings = mutableListOf<String>()
        val names = context.assets.list("coc_upgrade").orEmpty()
            .filter { it.matches(Regex("part-\\d{2}\\.json")) }
            .sorted()
        names.forEach { fileName ->
            runCatching {
                val text = context.assets.open("coc_upgrade/$fileName")
                    .bufferedReader(Charsets.UTF_8).use { it.readText() }
                val items = JSONObject(text).optJSONArray("items") ?: JSONArray()
                for (index in 0 until items.length()) {
                    val raw = items.optJSONObject(index) ?: continue
                    val id = raw.optInt("dataId", -1)
                    if (id < 1) {
                        warnings += "$fileName: bản ghi thứ ${index + 1} thiếu dataId hợp lệ"
                        continue
                    }
                    if (result.containsKey(id)) {
                        warnings += "$fileName: dataId $id bị trùng; giữ bản ghi đầu tiên"
                        continue
                    }
                    val levelsJson = raw.optJSONArray("levels") ?: JSONArray()
                    val parsedLevels = (0 until levelsJson.length()).mapNotNull { levelIndex ->
                        val levelRaw = levelsJson.optJSONObject(levelIndex) ?: return@mapNotNull null
                        val level = levelRaw.optInt("level", -1)
                        if (level < 1) null else CocUpgradeLevel(level, levelRaw)
                    }
                    val levels = parsedLevels.filterNot { it.isSupercharge }.sortedBy { it.level }
                    val superchargeLevels = parsedLevels.filter { it.isSupercharge }.sortedBy { it.level }
                    result[id] = CocUpgradeEntry(
                        dataId = id,
                        id = raw.optString("id"),
                        name = raw.optString("name"),
                        description = raw.optString("description"),
                        base = raw.optString("base"),
                        category = raw.optString("category"),
                        rarity = raw.optString("rarity").lowercase(),
                        hero = raw.optString("hero"),
                        maxLevel = raw.optInt("maxLevel", levels.maxOfOrNull { it.level } ?: 0),
                        properties = raw.optJSONObject("properties") ?: JSONObject(),
                        levels = levels,
                        aliases = raw.optJSONArray("aliases")?.let { aliases ->
                            (0 until aliases.length()).mapNotNull { aliasIndex ->
                                aliases.optString(aliasIndex).takeIf { it.isNotBlank() }
                            }
                        }.orEmpty(),
                        superchargeLevels = superchargeLevels
                    )
                }
            }.onFailure { error ->
                warnings += "$fileName: ${error.message ?: "JSON không hợp lệ"}"
            }
        }
        if (names.isEmpty()) warnings += "Không tìm thấy tệp part-XX.json trong assets/coc_upgrade"
        cachedWarnings = warnings
        return result
    }

    fun search(
        entries: Collection<CocUpgradeEntry>,
        query: String,
        baseFilter: String = "all",
        categoryFilter: String = "all",
        limit: Int = 30,
        rarityFilter: String = "all",
        heroFilter: String = "all"
    ): List<CocUpgradeEntry> {
        val rawQuery = query.trim()
        val normalizedQuery = normalizeSearchText(rawQuery.removePrefix("#"))
        val idQuery = rawQuery.removePrefix("#").filter(Char::isDigit)
        return entries.asSequence()
            .filter { baseFilter == "all" || it.base == baseFilter }
            .filter { categoryFilter == "all" || it.category == categoryFilter }
            .filter { rarityFilter == "all" || it.rarity.equals(rarityFilter, ignoreCase = true) }
            .filter { heroFilter == "all" || it.hero == heroFilter }
            .filter { entry ->
                if (rawQuery.isBlank()) return@filter true
                val searchable = normalizeSearchText(
                    listOf(entry.dataId.toString(), entry.id, entry.name, entry.localizedName,
                        entry.description, entry.hero, localizedHeroName(entry.hero), entry.category, entry.base, entry.rarity)
                        .plus(entry.aliases).joinToString(" ")
                )
                val numericMatch = idQuery.isNotBlank() && entry.dataId.toString().contains(idQuery)
                val textMatch = normalizedQuery.isNotBlank() && normalizedQuery.split(' ')
                    .all { token -> searchable.contains(token) }
                numericMatch || textMatch
            }
            .sortedWith(
                compareBy<CocUpgradeEntry> {
                    when {
                        idQuery.isNotBlank() && it.dataId.toString() == idQuery -> 0
                        it.id.equals(rawQuery, ignoreCase = true) -> 1
                        normalizeSearchText(it.name) == normalizedQuery -> 2
                        normalizeSearchText(it.localizedName) == normalizedQuery -> 3
                        it.dataId.toString().startsWith(idQuery) && idQuery.isNotBlank() -> 4
                        else -> 5
                    }
                }.thenBy { it.localizedName.lowercase(Locale.ROOT) }
            )
            .take(limit.coerceIn(1, 100))
            .toList()
    }

    fun localizedHeroName(heroId: String): String = when (heroId.lowercase(Locale.ROOT)) {
        "barbarian-king" -> "Vua man di"
        "archer-queen" -> "Nữ hoàng cung thủ"
        "grand-warden" -> "Đại hộ vệ"
        "royal-champion" -> "Nhà vô địch Hoàng gia"
        "minion-prince" -> "Hoàng tử tiểu quỷ"
        "dragon-duke" -> "Công tước Rồng"
        else -> heroId.replace('-', ' ').replaceFirstChar { it.uppercase(Locale.ROOT) }
    }

    private fun normalizeSearchText(value: String): String =
        Normalizer.normalize(value.lowercase(Locale.ROOT).replace('đ', 'd'), Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .replace(Regex("[^a-z0-9]+"), " ")
            .trim()

    fun maxLevelAtHall(
        entry: CocUpgradeEntry,
        hallLevel: Int?,
        entries: Map<Int, CocUpgradeEntry>
    ): Int? {
        if (hallLevel == null || hallLevel < 1) return null
        if (entry.category == "hero-equipment") {
            val smith = entries.values.firstOrNull { it.id == "blacksmith" } ?: return null
            val availableSmithLevel = smith.levels
                .filter { hallRequirement(smith, it, entries)?.let { required -> required <= hallLevel } == true }
                .maxOfOrNull { it.level } ?: return null
            val smithData = smith.level(availableSmithLevel)?.raw ?: return null
            val caps = smithData.optJSONObject("maxEquipmentLevel") ?: return null
            return caps.optInt(entry.rarity, -1).takeIf { it > 0 }?.coerceAtMost(entry.maxLevel)
        }
        val highest = entry.levels.filter { level ->
            hallRequirement(entry, level, entries)?.let { required -> required <= hallLevel } == true
        }.maxOfOrNull { it.level }
        return highest?.coerceAtMost(entry.maxLevel)
    }

    fun maxEquipmentAtBlacksmithLevel(
        entry: CocUpgradeEntry,
        blacksmithLevel: Int?,
        entries: Map<Int, CocUpgradeEntry>
    ): Int? {
        if (entry.category != "hero-equipment" || blacksmithLevel == null) return null
        val smith = entries.values.firstOrNull { it.id == "blacksmith" } ?: return null
        val caps = smith.level(blacksmithLevel)?.raw?.optJSONObject("maxEquipmentLevel") ?: return null
        return caps.optInt(entry.rarity, -1).takeIf { it > 0 }?.coerceAtMost(entry.maxLevel)
    }

    fun nextRequirements(
        entry: CocUpgradeEntry,
        level: Int,
        entries: Map<Int, CocUpgradeEntry>
    ): List<CocUpgradeRequirement> {
        val raw = entry.level(level)?.raw ?: return emptyList()
        val result = mutableListOf<CocUpgradeRequirement>()
        val hallKey = if (entry.base == "builder") "builderHallLevelRequired" else "townHallRequired"
        val hallFallbackKey = if (entry.base == "builder") "builderHallRequired" else "townHall"
        raw.optInt(hallKey, raw.optInt(hallFallbackKey, -1)).takeIf { it > 0 }?.let {
            result += CocUpgradeRequirement(if (entry.base == "builder") "Nhà thợ xây" else "Nhà chính", it)
        }
        facilityIds.forEach { (key, facilityId) ->
            val required = raw.optInt(key, -1)
            if (required > 0) {
                val label = when (facilityId) {
                    "hero-hall" -> "Nhà Anh hùng"
                    "laboratory" -> "Phòng thí nghiệm"
                    "pet-house" -> "Nhà thú cưng"
                    "blacksmith" -> "Lò rèn"
                    "workshop" -> "Xưởng chế tạo"
                    "barracks" -> "Doanh trại"
                    "dark-barracks" -> "Doanh trại hắc ám"
                    "spell-factory" -> "Nhà máy Thần chú"
                    "dark-spell-factory" -> "Nhà máy Thần chú hắc ám"
                    "star-laboratory" -> "Phòng thí nghiệm sao"
                    "builder-barracks" -> "Doanh trại thợ xây"
                    else -> facilityId
                }
                result += CocUpgradeRequirement(label, required)
            }
        }
        return result.distinctBy { it.label to it.level }
    }

    private fun hallRequirement(
        entry: CocUpgradeEntry,
        level: CocUpgradeLevel,
        entries: Map<Int, CocUpgradeEntry>
    ): Int? {
        val hallKey = if (entry.base == "builder") "builderHallLevelRequired" else "townHallRequired"
        val hallFallbackKey = if (entry.base == "builder") "builderHallRequired" else "townHall"
        val direct = level.int(hallKey, hallFallbackKey)
        val requirements = mutableListOf<Int>()
        direct?.takeIf { it > 0 }?.let(requirements::add)
        facilityIds.forEach { (key, facilityId) ->
            val facilityLevel = level.int(key) ?: return@forEach
            val facility = entries.values.firstOrNull { it.id == facilityId } ?: return@forEach
            val facilityData = facility.level(facilityLevel)?.raw ?: return@forEach
            val requiredHall = if (entry.base == "builder") {
                facilityData.optInt("builderHallLevelRequired", facilityData.optInt("builderHallRequired", -1))
            } else {
                facilityData.optInt("townHallRequired", -1)
            }
            if (requiredHall > 0) requirements += requiredHall
        }
        requirements.maxOrNull()?.let { return it }
        val previous = entry.levels.filter { it.level < level.level }.maxByOrNull { it.level }
        return previous?.let { hallRequirement(entry, it, entries) }
            ?: if (level.level == entry.levels.firstOrNull()?.level && entry.levels.size == 1) 1 else null
    }

    fun upgradeTotals(
        entry: CocUpgradeEntry,
        currentLevel: Int,
        targetLevel: Int,
        skipFirstInProgress: Boolean = false
    ): CocUpgradeTotals {
        val resources = linkedMapOf<String, Long>()
        var duration = 0L
        entry.levels.filter { it.level in (currentLevel + 1)..targetLevel }.forEach { level ->
            val inProgress = skipFirstInProgress && level.level == currentLevel + 1
            if (inProgress) return@forEach
            val cost = level.cost()
            val resource = level.resource()
            if (cost > 0L && resource.isNotBlank()) resources[resource] = (resources[resource] ?: 0L) + cost
            level.oreCosts().forEach { (name, amount) ->
                resources[name] = (resources[name] ?: 0L) + amount
            }
            duration += level.durationSeconds()
        }
        return CocUpgradeTotals(resources, duration)
    }

    fun superchargeMaxAtHall(entry: CocUpgradeEntry, hallLevel: Int?): Int? {
        if (hallLevel == null || hallLevel < 1 || entry.superchargeLevels.isEmpty()) return null
        val hallKey = if (entry.base == "builder") "builderHallLevelRequired" else "townHallRequired"
        val fallbackKey = if (entry.base == "builder") "builderHallRequired" else "townHall"
        return entry.superchargeLevels
            .filter { level ->
                val required = level.int(hallKey, fallbackKey)
                required != null && required <= hallLevel
            }
            .maxOfOrNull { it.level }
    }

    fun superchargeTotals(
        entry: CocUpgradeEntry,
        currentTier: Int,
        targetTier: Int
    ): CocUpgradeTotals {
        val resources = linkedMapOf<String, Long>()
        var duration = 0L
        entry.superchargeLevels
            .filter { it.level > currentTier && it.level <= targetTier }
            .forEach { level ->
                val cost = level.cost()
                val resource = level.resource()
                if (cost > 0L && resource.isNotBlank()) {
                    resources[resource] = (resources[resource] ?: 0L) + cost
                }
                level.oreCosts().forEach { (name, amount) ->
                    resources[name] = (resources[name] ?: 0L) + amount
                }
                duration += level.durationSeconds()
            }
        return CocUpgradeTotals(resources, duration)
    }

    fun levelTime(level: CocUpgradeLevel?): Long = level?.durationSeconds() ?: 0L
}
