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
        .map { normalizeClashSearchText(it.value) }
        .filter { it.length >= 2 }
        .distinct()

    val normalized = normalizeClashSearchText(trimmed)
    return buildList {
        if (normalized.length >= 2) add(normalized)
        addAll(chunks)
        if (chunks.size > 1) add(chunks.joinToString(""))
        Regex("[\\p{L}\\p{N}]+")
            .findAll(trimmed)
            .map { it.value.trim() }
            .filter { it.length >= 2 }
            .forEach(::add)
        add(trimmed)
    }
        .map(String::trim)
        .filter { it.length >= 2 }
        .distinctBy { normalizeClashSearchText(it) }
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
