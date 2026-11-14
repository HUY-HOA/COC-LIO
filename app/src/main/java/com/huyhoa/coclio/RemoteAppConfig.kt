package com.huyhoa.coclio

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

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
    private const val CONFIG_KEY = "coclio_app_config_json"
    private const val DEFAULT_CONFIG_JSON = """
{
  "schemaVersion": 1,
  "version": 1,
  "enabled": true,
  "theme": {
    "darkPrimary": "#36F4FF",
    "darkSecondary": "#FF4DDA",
    "darkTertiary": "#FFD166",
    "darkBackground": "#000000",
    "darkSurface": "#0D152B",
    "darkSurfaceVariant": "#14203B",
    "darkOnSurfaceVariant": "#9EAFCE",
    "lightPrimary": "#006A78",
    "lightSecondary": "#8B3B85",
    "lightTertiary": "#8A5A00",
    "lightBackground": "#FFFFFF",
    "lightSurface": "#FFFFFF",
    "lightSurfaceVariant": "#E8EEF6",
    "lightOnBackground": "#172235",
    "lightOnSurface": "#172235",
    "lightOnSurfaceVariant": "#526176"
  },
  "led": {
    "speedMultiplier": 1.0
  },
  "translations": {
    "vi": {},
    "en": {}
  }
}
"""

    private val mutableConfig = MutableStateFlow(RemoteAppConfig())
    val config: StateFlow<RemoteAppConfig> = mutableConfig.asStateFlow()
    private var initialized = false

    fun initialize() {
        if (initialized) return
        initialized = true

        val remoteConfig = FirebaseRemoteConfig.getInstance()
        val settings = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(60L)
            .build()

        remoteConfig.setConfigSettingsAsync(settings)
        remoteConfig.setDefaultsAsync(mapOf(CONFIG_KEY to DEFAULT_CONFIG_JSON))
            .addOnCompleteListener {
                apply(remoteConfig.getString(CONFIG_KEY))
                refresh()
            }
    }

    fun refresh() {
        val remoteConfig = FirebaseRemoteConfig.getInstance()
        remoteConfig.fetchAndActivate().addOnCompleteListener {
            apply(remoteConfig.getString(CONFIG_KEY))
        }
    }

    private fun apply(raw: String) {
        runCatching { parse(raw) }
            .onSuccess { mutableConfig.value = it }
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
