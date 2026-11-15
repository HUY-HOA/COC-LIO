package com.huyhoa.coclio

import java.text.Normalizer
import java.util.Locale

internal fun normalizeClashSearchText(value: String): String {
    val decomposed = Normalizer.normalize(value, Normalizer.Form.NFD)
    return buildString(decomposed.length) {
        for (character in decomposed) {
            when (Character.getType(character)) {
                Character.NON_SPACING_MARK.toInt(),
                Character.COMBINING_SPACING_MARK.toInt(),
                Character.ENCLOSING_MARK.toInt(),
                Character.FORMAT.toInt() -> Unit
                else -> if (character.isLetterOrDigit()) {
                    append(character.toString().uppercase(Locale.ROOT))
                }
            }
        }
    }
}

internal fun clashSearchTerms(value: String): List<String> {
    val trimmed = value.trim()
    val rawChunks = Regex("[\\p{L}\\p{N}]+")
        .findAll(trimmed)
        .map { it.value.trim() }
        .filter { normalizeClashSearchText(it).length >= 2 }
        .toList()
    val normalizedChunks = rawChunks
        .map(::normalizeClashSearchText)
        .distinct()
    val normalized = normalizeClashSearchText(trimmed)

    return buildList {
        if (normalized.length >= 2) add(normalized)
        addAll(normalizedChunks)
        if (normalizedChunks.size > 1) add(normalizedChunks.joinToString(""))
        addAll(rawChunks)
        add(trimmed)
    }
        .map(String::trim)
        .filter { normalizeClashSearchText(it).length >= 2 }
        .distinctBy { it.uppercase(Locale.ROOT) }
        .take(8)
}

internal fun clashNameMatchesSearch(name: String, query: String): Boolean {
    val normalizedName = normalizeClashSearchText(name)
    val normalizedQuery = normalizeClashSearchText(query)
    if (normalizedName.isBlank() || normalizedQuery.isBlank()) return false
    if (normalizedName.contains(normalizedQuery)) return true

    val queryParts = Regex("[\\p{L}\\p{N}]+")
        .findAll(query)
        .map { normalizeClashSearchText(it.value) }
        .filter { it.length >= 2 }
        .distinct()
        .toList()

    return queryParts.size > 1 && queryParts.all(normalizedName::contains)
}
