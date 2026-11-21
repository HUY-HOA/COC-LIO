package com.huyhoa.coclio

import com.huyhoa.coclio.data.VillageExtraItem
import com.huyhoa.coclio.data.VillageItem
import java.text.Normalizer
import java.util.Locale

/**
 * Pure filtering/sorting logic for the local JSON explorer. Search intentionally
 * ignores Vietnamese diacritics so "nha chinh" can find "Nhà chính".
 */
internal fun filterJsonExplorerItems(
    items: List<VillageItem>,
    selectedBase: String,
    selectedStatus: String,
    query: String,
    sortOrder: String,
    snapshotTimestamp: Long,
    nowSeconds: Long
): List<VillageItem> {
    val normalizedQuery = normalizeJsonExplorerText(query)
    return items.asSequence()
        .filter { selectedBase == "all" || it.base == selectedBase }
        .filter { item ->
            val active = item.isTimerActive(snapshotTimestamp, nowSeconds)
            when (selectedStatus) {
                "running" -> active
                "upgrading" -> active && !item.gearUp
                "gear" -> active && item.gearUp
                "expired" -> item.timerSeconds != null && !active
                "idle" -> item.timerSeconds == null
                "extras" -> false
                else -> true
            }
        }
        .filter { item ->
            if (normalizedQuery.isBlank()) return@filter true
            val searchable = buildString {
                append(item.name).append(' ')
                append(item.sectionLabel).append(' ')
                append(item.sectionKey).append(' ')
                append(item.dataId).append(' ')
                append(item.level ?: "").append(' ')
                append(item.count).append(' ')
                append(item.timerSeconds ?: "").append(' ')
                append(item.helperCooldownSeconds ?: "").append(' ')
                append(item.properties.entries.joinToString(" ") { "${it.key} ${it.value}" }).append(' ')
                append(item.details.joinToString(" "))
            }
            normalizeJsonExplorerText(searchable).contains(normalizedQuery)
        }
        .sortedWith(
            when (sortOrder) {
                "level" -> compareByDescending<VillageItem> { it.level ?: -1 }
                    .thenBy { normalizeJsonExplorerText(it.name) }
                "timer" -> compareBy<VillageItem> {
                    it.remainingSecondsAt(snapshotTimestamp, nowSeconds)
                        .takeIf { remaining -> remaining > 0L } ?: Long.MAX_VALUE
                }.thenBy { normalizeJsonExplorerText(it.name) }
                else -> compareBy<VillageItem> { normalizeJsonExplorerText(it.name) }
            }
        )
        .toList()
}

internal fun VillageExtraItem.matchesExplorerQuery(query: String): Boolean {
    val normalizedQuery = normalizeJsonExplorerText(query)
    if (normalizedQuery.isBlank()) return true
    val searchable = listOf(
        name,
        sectionLabel,
        sectionKey,
        dataId?.toString().orEmpty(),
        level?.toString().orEmpty(),
        count.toString(),
        value.orEmpty()
    ).joinToString(" ")
    return normalizeJsonExplorerText(searchable).contains(normalizedQuery)
}

internal fun normalizeJsonExplorerText(value: String): String =
    Normalizer.normalize(
        value.lowercase(Locale.ROOT).replace('đ', 'd'),
        Normalizer.Form.NFD
    ).replace(Regex("\\p{M}+"), "")
        .replace(Regex("[^a-z0-9]+"), " ")
        .trim()
