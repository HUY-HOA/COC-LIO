package com.huyhoa.coclio

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.URL
import javax.net.ssl.HttpsURLConnection

data class RemoteAppConfig(
    val enabled: Boolean = true,
    val version: Int = 0,
    val darkPrimary: Int = 0xFF36F4FF.toInt(),
    val darkSecondary: Int = 0xFFFF4DDA.toInt(),
    val darkTertiary: Int = 0xFFFFD166.toInt(),
    val darkBackground: Int = 0xFF000000.toInt(),
    val darkSurface: Int = 0xFF0D152B.toInt(),
    val darkSurfaceVariant: Int = 0xFF14203B.toInt(),
    val darkOnSurfaceVariant: Int = 0xFF9EAFCE.toInt(),
    val lightPrimary: Int = 0xFF006A78.toInt(),
    val lightSecondary: Int = 0xFF8B3B85.toInt(),
    val lightTertiary: Int = 0xFF8A5A00.toInt(),
    val lightBackground: Int = 0xFFFFFFFF.toInt(),
    val lightSurface: Int = 0xFFFFFFFF.toInt(),
    val lightSurfaceVariant: Int = 0xFFE8EEF6.toInt(),
    val lightOnBackground: Int = 0xFF172235.toInt(),
    val lightOnSurface: Int = 0xFF172235.toInt(),
    val lightOnSurfaceVariant: Int = 0xFF526176.toInt(),
    val ledSpeedMultiplier: Float = 1f,
    val translations: Map<String, Map<String, String>> = emptyMap()
) {
    fun translation(source: String, language: AppUiLanguage): String? {
        if (!enabled) return null
        val key = if (language == AppUiLanguage.ENGLISH) "en" else "vi"
        return translations[key]?.get(source)
    }
}

object RemoteAppConfigManager {
    private const val PREFERENCES = "coc_lio_remote_config"
    private const val KEY_CONFIG = "config_json"
    private const val CONFIG_URL = "https://raw.githubusercontent.com/HUY-HOA-1/COC-LIO/main/remote-config/app.json"
    private const val MAX_CONFIG_BYTES = 262144

    private val mutableConfig = MutableStateFlow(RemoteAppConfig())
    val config: StateFlow<RemoteAppConfig> = mutableConfig.asStateFlow()

    fun loadCached(context: Context) {
        val cached = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
            .getString(KEY_CONFIG, null)
            ?: return
        runCatching { parse(cached) }
            .onSuccess { mutableConfig.value = it }
    }

    suspend fun refresh(context: Context) {
        val downloaded = runCatching {
            withContext(Dispatchers.IO) { download() }
        }.getOrNull() ?: return

        val parsed = runCatching { parse(downloaded) }.getOrNull() ?: return
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_CONFIG, downloaded)
            .apply()
        mutableConfig.value = parsed
    }

    private fun download(): String {
        val url = URL("$CONFIG_URL?cacheBust=${System.currentTimeMillis()}")
        val connection = (url.openConnection() as HttpsURLConnection).apply {
            connectTimeout = 5000
            readTimeout = 5000
            requestMethod = "GET"
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Cache-Control", "no-cache")
            setRequestProperty("Pragma", "no-cache")
            setRequestProperty("User-Agent", "COC-LIO-Android")
        }

        try {
            if (connection.responseCode != HttpsURLConnection.HTTP_OK) {
                throw IOException("Remote configuration unavailable.")
            }
            val output = ByteArrayOutputStream()
            connection.inputStream.use { input ->
                val buffer = ByteArray(8192)
                var total = 0
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    total += count
                    if (total > MAX_CONFIG_BYTES) {
                        throw IOException("Remote configuration is too large.")
                    }
                    output.write(buffer, 0, count)
                }
            }
            return output.toString(Charsets.UTF_8.name())
        } finally {
            connection.disconnect()
        }
    }

    private fun parse(raw: String): RemoteAppConfig {
        val root = JSONObject(raw)
        require(root.optInt("schemaVersion", 0) == 1)

        val defaults = RemoteAppConfig()
        if (!root.optBoolean("enabled", true)) {
            return defaults.copy(enabled = false, version = root.optInt("version", 0))
        }

        val theme = root.optJSONObject("theme") ?: JSONObject()
        val led = root.optJSONObject("led") ?: JSONObject()
        val translationsJson = root.optJSONObject("translations")
        val translations = linkedMapOf<String, Map<String, String>>()

        for (language in listOf("vi", "en")) {
            val languageJson = translationsJson?.optJSONObject(language) ?: continue
            val entries = linkedMapOf<String, String>()
            val keys = languageJson.keys()
            while (keys.hasNext() && entries.size < 1500) {
                val key = keys.next()
                val value = languageJson.optString(key, "")
                if (key.isNotBlank() && key.length <= 2048 && value.length <= 2048) {
                    entries[key] = value
                }
            }
            translations[language] = entries
        }

        val speed = led.optDouble("speedMultiplier", 1.0).toFloat()
            .takeIf { it.isFinite() }
            ?.coerceIn(0.5f, 3f)
            ?: 1f

        return RemoteAppConfig(
            enabled = true,
            version = root.optInt("version", 0),
            darkPrimary = color(theme, "darkPrimary", defaults.darkPrimary),
            darkSecondary = color(theme, "darkSecondary", defaults.darkSecondary),
            darkTertiary = color(theme, "darkTertiary", defaults.darkTertiary),
            darkBackground = color(theme, "darkBackground", defaults.darkBackground),
            darkSurface = color(theme, "darkSurface", defaults.darkSurface),
            darkSurfaceVariant = color(theme, "darkSurfaceVariant", defaults.darkSurfaceVariant),
            darkOnSurfaceVariant = color(theme, "darkOnSurfaceVariant", defaults.darkOnSurfaceVariant),
            lightPrimary = color(theme, "lightPrimary", defaults.lightPrimary),
            lightSecondary = color(theme, "lightSecondary", defaults.lightSecondary),
            lightTertiary = color(theme, "lightTertiary", defaults.lightTertiary),
            lightBackground = color(theme, "lightBackground", defaults.lightBackground),
            lightSurface = color(theme, "lightSurface", defaults.lightSurface),
            lightSurfaceVariant = color(theme, "lightSurfaceVariant", defaults.lightSurfaceVariant),
            lightOnBackground = color(theme, "lightOnBackground", defaults.lightOnBackground),
            lightOnSurface = color(theme, "lightOnSurface", defaults.lightOnSurface),
            lightOnSurfaceVariant = color(theme, "lightOnSurfaceVariant", defaults.lightOnSurfaceVariant),
            ledSpeedMultiplier = speed,
            translations = translations
        )
    }

    private fun color(json: JSONObject, key: String, fallback: Int): Int {
        val raw = json.optString(key, "").removePrefix("#").uppercase()
        val argb = when (raw.length) {
            6 -> "FF$raw"
            8 -> raw
            else -> return fallback
        }
        if (!argb.matches(Regex("[0-9A-F]{8}"))) return fallback
        return runCatching { argb.toLong(16).toInt() }.getOrDefault(fallback)
    }
}
