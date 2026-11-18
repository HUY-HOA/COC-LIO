package com.huyhoa.coclio.data

/**
 * One owned village item (or a group of identical buildings) and its remaining
 * upgrade plan from the imported level to the current game maximum.
 */
data class CocVillageMaxPlanLine(
    val dataId: Int,
    val name: String,
    val groupLabel: String,
    val currentLevel: Int,
    val targetLevel: Int,
    val quantity: Int,
    val resources: Map<String, Long>,
    val durationSeconds: Long,
    val missingLevels: List<Int>,
    val assumedCurrentLevel: Boolean,
    val upgradeInProgress: Boolean
)

data class CocVillageMaxPlanSummary(
    val resources: Map<String, Long>,
    /** Cumulative builder/laboratory time, not a calendar-time schedule. */
    val durationSeconds: Long,
    val lines: List<CocVillageMaxPlanLine>,
    val ownedCopies: Int,
    val maxedCopies: Int,
    val unconfirmedItems: Int,
    val itemsWithoutCatalogData: Int,
    val incompleteItems: Int
)

/**
 * Calculates a village-wide "current level -> game MAX" plan from a ClashKing
 * village export and the bundled game catalog.
 *
 * Building/trap/wall counts represent multiple identical structures and are
 * multiplied. Troop counts represent troop quantity, not separate research
 * upgrades, so army items are counted once.
 */
object CocVillageMaxPlanCalculator {
    fun calculate(
        snapshot: VillageSnapshot,
        base: String,
        catalog: Map<Int, CocUpgradeEntry>,
        nowSeconds: Long
    ): CocVillageMaxPlanSummary {
        val totals = linkedMapOf<String, Long>()
        val lines = mutableListOf<CocVillageMaxPlanLine>()
        var ownedCopies = 0
        var maxedCopies = 0
        var unconfirmedItems = 0
        var itemsWithoutCatalogData = 0
        var incompleteItems = 0

        snapshot.items.asSequence()
            .filter { it.base == base }
            .forEach { item ->
                val quantity = if (item.tab == "buildings") item.count.coerceAtLeast(1) else 1
                val entry = catalog[item.dataId]
                if (entry == null) {
                    if (item.count > 0 || (item.timerSeconds ?: 0L) > 0L) {
                        itemsWithoutCatalogData += 1
                    }
                    return@forEach
                }

                val ownershipConfirmed = if (entry.category == "hero-equipment") {
                    item.hasConfirmedEquipmentOwnership
                } else {
                    item.count > 0 || (item.timerSeconds ?: 0L) > 0L
                }
                if (!ownershipConfirmed || item.properties["catalogOnly"] == "true") {
                    unconfirmedItems += 1
                    return@forEach
                }
                if (entry.levels.isEmpty()) {
                    incompleteItems += 1
                    return@forEach
                }

                val firstLevel = entry.firstAvailableLevel
                val targetLevel = CocUpgradeCatalog.currentMaxLevel(entry).coerceAtLeast(firstLevel)
                val hasLevel = item.level != null
                val currentLevel = (item.level ?: firstLevel).coerceIn(firstLevel, targetLevel)
                ownedCopies += quantity

                val missing = CocUpgradeCatalog.missingLevels(entry, currentLevel, targetLevel)
                if (missing.isNotEmpty()) incompleteItems += 1

                val regularTotals = CocUpgradeCatalog.upgradeTotals(entry, currentLevel, targetLevel)
                val lineResources = regularTotals.resources.mapValuesTo(linkedMapOf()) {
                    safeMultiply(it.value, quantity.toLong())
                }
                var lineDuration = safeMultiply(regularTotals.durationSeconds, quantity.toLong())

                val recordedTimer = item.timerSeconds?.takeIf { it > 0L }
                val upgradeInProgress = item.isUpgradeRunning
                if (recordedTimer != null) {
                    if (upgradeInProgress) {
                        // The next level's cost has already been paid for one structure.
                        val nextLevel = entry.level(currentLevel + 1)
                        if (nextLevel != null) {
                            val cost = nextLevel.cost()
                            val resource = nextLevel.resource()
                            if (cost > 0L && resource.isNotBlank()) {
                                addAmount(lineResources, resource, -cost)
                            }
                            nextLevel.oreCosts().forEach { (resourceName, amount) ->
                                addAmount(lineResources, resourceName, -amount)
                            }
                            lineDuration = (lineDuration - nextLevel.durationSeconds()).coerceAtLeast(0L)
                        }
                    }
                    // A gear-up timer is added as work in progress, but does not
                    // consume the next normal upgrade level.
                    lineDuration = safeAdd(
                        lineDuration,
                        item.remainingSecondsAt(snapshot.timestamp, nowSeconds)
                    )
                }

                if (currentLevel >= targetLevel && lineDuration == 0L && lineResources.isEmpty()) {
                    maxedCopies += quantity
                    return@forEach
                }

                val groupLabel = if (item.tab == "buildings") {
                    "Công trình, phòng thủ và bẫy"
                } else {
                    "Quân lính, thần chú và anh hùng"
                }
                val line = CocVillageMaxPlanLine(
                    dataId = item.dataId,
                    name = entry.localizedName.ifBlank { item.name },
                    groupLabel = groupLabel,
                    currentLevel = currentLevel,
                    targetLevel = targetLevel,
                    quantity = quantity,
                    resources = lineResources.filterValues { it > 0L },
                    durationSeconds = lineDuration,
                    missingLevels = missing,
                    assumedCurrentLevel = !hasLevel,
                    upgradeInProgress = upgradeInProgress
                )
                lines += line
                line.resources.forEach { (resource, amount) ->
                    addAmount(totals, resource, amount)
                }
            }

        return CocVillageMaxPlanSummary(
            resources = totals,
            durationSeconds = lines.fold(0L) { sum, line -> safeAdd(sum, line.durationSeconds) },
            lines = lines.sortedWith(compareBy<CocVillageMaxPlanLine> { it.groupLabel }.thenBy { it.name }),
            ownedCopies = ownedCopies,
            maxedCopies = maxedCopies,
            unconfirmedItems = unconfirmedItems,
            itemsWithoutCatalogData = itemsWithoutCatalogData,
            incompleteItems = incompleteItems
        )
    }

    private fun addAmount(target: MutableMap<String, Long>, key: String, amount: Long) {
        if (key.isBlank() || amount == 0L) return
        val next = safeAdd(target[key] ?: 0L, amount)
        if (next > 0L) target[key] = next else target.remove(key)
    }

    private fun safeMultiply(value: Long, multiplier: Long): Long {
        if (value <= 0L || multiplier <= 0L) return 0L
        return if (value > Long.MAX_VALUE / multiplier) Long.MAX_VALUE else value * multiplier
    }

    private fun safeAdd(first: Long, second: Long): Long {
        if (second > 0L && first > Long.MAX_VALUE - second) return Long.MAX_VALUE
        if (second < 0L && first < Long.MIN_VALUE - second) return Long.MIN_VALUE
        return first + second
    }
}
