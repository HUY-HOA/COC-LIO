package com.huyhoa.coclio

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.tasks.Task
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.languageid.LanguageIdentificationOptions
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object CommunityAutoTranslationPreferences {
    private const val PREFS = "coc_lio_community_translation"
    private const val KEY_ENABLED = "enabled"

    fun isEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_ENABLED, false)

    fun setEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ENABLED, enabled)
            .apply()
    }
}

private object CommunityTranslationEngine {
    private val mutex = Mutex()
    private val languageIdentifier = LanguageIdentification.getClient(
        LanguageIdentificationOptions.Builder()
            .setConfidenceThreshold(0.34f)
            .build()
    )
    private val translators = LinkedHashMap<String, Translator>(4, 0.75f, true)
    private val cache = LinkedHashMap<String, String>(128, 0.75f, true)

    suspend fun translate(sourceText: String, targetTag: String): String? = mutex.withLock {
        if (sourceText.isBlank() || sourceText.length > 5000) return@withLock null
        val cacheKey = "$targetTag\u0000$sourceText"
        cache[cacheKey]?.let { return@withLock it }

        val detectedTag = runCatching {
            languageIdentifier.identifyLanguage(sourceText).awaitTranslationResult()
        }.getOrNull()?.substringBefore('-')?.lowercase() ?: return@withLock null
        if (detectedTag == "und" || detectedTag == targetTag) return@withLock null

        val sourceLanguage = TranslateLanguage.fromLanguageTag(detectedTag) ?: return@withLock null
        val targetLanguage = TranslateLanguage.fromLanguageTag(targetTag) ?: return@withLock null
        if (sourceLanguage == targetLanguage) return@withLock null

        val pair = "$sourceLanguage-$targetLanguage"
        val translator = translators[pair] ?: Translation.getClient(
            TranslatorOptions.Builder()
                .setSourceLanguage(sourceLanguage)
                .setTargetLanguage(targetLanguage)
                .build()
        ).also { created ->
            translators[pair] = created
            if (translators.size > 4) {
                val oldestPair = translators.entries.first().key
                if (oldestPair != pair) {
                    runCatching { translators.remove(oldestPair)?.close() }
                }
            }
        }
        runCatching {
            translator.downloadModelIfNeeded(DownloadConditions.Builder().build()).awaitTranslationResult()
            translator.translate(sourceText).awaitTranslationResult().trim()
        }.getOrNull()?.takeIf { it.isNotBlank() && it != sourceText }?.also { translated ->
            cache[cacheKey] = translated
            if (cache.size > 200) cache.remove(cache.entries.first().key)
        }
    }
}

suspend fun translateCommunityTextForApp(context: Context, sourceText: String): String? {
    if (!CommunityAutoTranslationPreferences.isEnabled(context)) return null
    val systemLanguage = context.resources.configuration.locales[0].language
    val targetTag = when (AppLanguageSettings.resolve(AppLanguageSettings.load(context), systemLanguage)) {
        AppUiLanguage.VIETNAMESE -> "vi"
        AppUiLanguage.ENGLISH -> "en"
    }
    return CommunityTranslationEngine.translate(sourceText, targetTag)
}

private suspend fun <T> Task<T>.awaitTranslationResult(): T =
    suspendCancellableCoroutine { continuation ->
        addOnCompleteListener { task ->
            if (!continuation.isActive) return@addOnCompleteListener
            if (task.isSuccessful) {
                continuation.resume(task.result)
            } else {
                continuation.resumeWithException(
                    task.exception ?: IllegalStateException("Không thể hoàn tất bản dịch.")
                )
            }
        }
    }

@Composable
fun CommunityAutoTranslatedText(
    sourceText: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface,
    fontSize: TextUnit = 12.sp,
    maxLines: Int = Int.MAX_VALUE,
    enabled: Boolean = false
) {
    val targetTag = when (LocalAppLanguage.current) {
        AppUiLanguage.VIETNAMESE -> "vi"
        AppUiLanguage.ENGLISH -> "en"
    }
    var translatedText by remember(sourceText, targetTag) { mutableStateOf<String?>(null) }
    var translating by remember(sourceText, targetTag) { mutableStateOf(false) }

    LaunchedEffect(sourceText, targetTag, enabled) {
        translatedText = null
        translating = enabled && sourceText.isNotBlank() && sourceText.length <= 5000
        if (enabled && translating) {
            translatedText = CommunityTranslationEngine.translate(sourceText, targetTag)
            translating = false
        }
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        if (enabled && !translatedText.isNullOrBlank()) {
            Text(
                translatedText.orEmpty(),
                color = color,
                fontSize = fontSize,
                maxLines = maxLines
            )
            Text(
                "Bản dịch tự động",
                color = MaterialTheme.colorScheme.tertiary,
                fontSize = 8.sp
            )
            Text(
                sourceText,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = (fontSize.value * 0.85f).sp,
                maxLines = maxLines
            )
        } else {
            Text(
                sourceText,
                color = color,
                fontSize = fontSize,
                maxLines = maxLines
            )
            if (translating) {
                Text(
                    "Đang dịch tự động…",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 8.sp
                )
            }
        }
    }
}