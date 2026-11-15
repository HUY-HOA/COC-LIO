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
    val chunks = Regex("[\\p{L}\\p{N}]+")
        .findAll(trimmed)
        .map { it.value }
        .toList()

    return buildList {
        add(trimmed)
        add(normalizeClashSearchText(trimmed))
        chunks.forEach {
            add(it)
            add(normalizeClashSearchText(it))
        }
    }
        .map(String::trim)
        .filter { it.length >= 2 }
        .distinctBy { it.lowercase(Locale.ROOT) }
        .take(6)
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
