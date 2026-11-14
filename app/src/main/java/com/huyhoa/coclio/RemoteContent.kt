package com.huyhoa.coclio

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class RemoteContentAnnouncement(
    val title: String,
    val message: String,
    val severity: String,
    val imageUrl: String,
    val actionLabel: String,
    val actionId: String
)

data class RemoteCatalogEntry(
    val name: String,
    val category: String,
    val description: String,
    val maxLevel: Int?,
    val imageUrl: String
)

data class RemoteGuide(
    val id: String,
    val title: String,
    val body: String,
    val imageUrl: String
)

data class RemoteContentLink(
    val id: String,
    val label: String,
    val url: String
)

data class RemoteContentState(
    val version: Int = 0,
    val maintenanceEnabled: Boolean = false,
    val maintenanceTitle: String = "",
    val maintenanceMessage: String = "",
    val announcement: RemoteContentAnnouncement? = null,
    val catalog: Map<Int, RemoteCatalogEntry> = emptyMap(),
    val guides: List<RemoteGuide> = emptyList(),
    val links: List<RemoteContentLink> = emptyList(),
    val notificationTemplates: Map<String, Map<String, String>> = emptyMap()
)

object RemoteContentManager {
    private const val COLLECTION = "app_remote_content"
    private const val DOCUMENT = "current"
    private const val PREFS = "coc_lio_remote_content"

    private val mutableContent = MutableStateFlow(RemoteContentState())
    val content: StateFlow<RemoteContentState> = mutableContent.asStateFlow()
    private var listenerRegistration: ListenerRegistration? = null
    @Volatile private var accessAllowed = false

    fun setAccessAllowed(context: Context, allowed: Boolean) {
        accessAllowed = allowed
        if (allowed) startRealtimeUpdates(context) else stopRealtimeUpdates()
    }

    fun onHostStart(context: Context) {
        if (accessAllowed) startRealtimeUpdates(context)
    }

    fun startRealtimeUpdates(context: Context) {
        if (!accessAllowed || FirebaseAuth.getInstance().currentUser == null || listenerRegistration != null) return

        listenerRegistration = FirebaseFirestore.getInstance()
            .collection(COLLECTION)
            .document(DOCUMENT)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val parsed = if (snapshot?.exists() == true) {
                    parseRemoteContent(snapshot.data.orEmpty())
                } else {
                    RemoteContentState()
                }
                publish(context.applicationContext, parsed)
            }
    }

    fun stopRealtimeUpdates() {
        listenerRegistration?.remove()
        listenerRegistration = null
    }

    private fun publish(context: Context, state: RemoteContentState) {
        val preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val editor = preferences.edit()
            .putInt("version", state.version)

        val language = AppLanguageSettings.resolve(
            AppLanguageSettings.load(context),
            context.resources.configuration.locales[0].language
        )
        val languageKey = if (language == AppUiLanguage.ENGLISH) "en" else "vi"
        val templates = state.notificationTemplates[languageKey].orEmpty()
        listOf("title", "text", "body").forEach { key ->
            val value = templates[key]
            if (value.isNullOrBlank()) editor.remove("notification_$key")
            else editor.putString("notification_$key", value)
        }
        editor.apply()
        mutableContent.value = state
    }
}

internal fun parseRemoteContent(data: Map<String, Any>): RemoteContentState {
    val version = (data["version"] as? Number)?.toInt()?.coerceAtLeast(0) ?: 0
    if (data["enabled"] == false) return RemoteContentState(version = version)

    val maintenance = data["maintenance"] as? Map<*, *>
    val announcementData = data["announcement"] as? Map<*, *>
    val announcement = announcementData?.takeIf { it["enabled"] != false }?.let { value ->
        val title = (value["title"] as? String).orEmpty().take(120)
        val message = (value["message"] as? String).orEmpty().take(800)
        if (title.isBlank() && message.isBlank()) null else {
            val actionId = (value["actionId"] as? String).orEmpty()
                .takeIf { it in setOf("import_json", "open_game", "open_settings", "open_community", "open_history", "open_lookup") }
                .orEmpty()
            RemoteContentAnnouncement(
                title = title,
                message = message,
                severity = (value["severity"] as? String)
                    ?.takeIf { it in setOf("info", "success", "warning", "error") } ?: "info",
                imageUrl = safeRemoteHttpsUrl(value["imageUrl"]),
                actionLabel = (value["actionLabel"] as? String).orEmpty().take(48),
                actionId = actionId
            )
        }
    }

    val catalog = (data["catalog"] as? List<*>)?.take(500)
        ?.mapNotNull { raw ->
            val item = raw as? Map<*, *> ?: return@mapNotNull null
            val id = (item["dataId"] as? Number)?.toInt()
                ?.takeIf { it in 1..2_000_000 } ?: return@mapNotNull null
            val name = (item["name"] as? String).orEmpty().trim().take(80)
            if (name.isBlank()) return@mapNotNull null
            val category = (item["category"] as? String)
                ?.takeIf { it in setOf("defense", "resources", "traps", "other", "troops", "heroes", "spells", "pets", "equipment") }
                .orEmpty()
            val maxLevel = (item["maxLevel"] as? Number)?.toInt()
                ?.takeIf { it in 1..100 }
            id to RemoteCatalogEntry(
                name = name,
                category = category,
                description = (item["description"] as? String).orEmpty().take(240),
                maxLevel = maxLevel,
                imageUrl = safeRemoteHttpsUrl(item["imageUrl"])
            )
        }
        ?.toMap()
        .orEmpty()

    val guides = (data["guides"] as? List<*>)?.take(20)
        ?.mapNotNull { raw ->
            val item = raw as? Map<*, *> ?: return@mapNotNull null
            val id = (item["id"] as? String).orEmpty().take(40)
            val title = (item["title"] as? String).orEmpty().trim().take(100)
            val body = (item["body"] as? String).orEmpty().trim().take(1200)
            if (id.isBlank() || title.isBlank() || body.isBlank() || item["enabled"] == false) {
                return@mapNotNull null
            }
            RemoteGuide(id, title, body, safeRemoteHttpsUrl(item["imageUrl"]))
        }
        .orEmpty()

    val links = (data["links"] as? List<*>)?.take(20)
        ?.mapNotNull { raw ->
            val item = raw as? Map<*, *> ?: return@mapNotNull null
            val id = (item["id"] as? String).orEmpty().take(40)
            val label = (item["label"] as? String).orEmpty().trim().take(60)
            val url = safeRemoteHttpsUrl(item["url"])
            if (id.isBlank() || label.isBlank() || url.isBlank() || item["enabled"] == false) null
            else RemoteContentLink(id, label, url)
        }
        .orEmpty()

    val templatesRaw = data["notificationTemplates"] as? Map<*, *>
    val templates = listOf("vi", "en").mapNotNull { language ->
        val languageData = templatesRaw?.get(language) as? Map<*, *>
            ?: return@mapNotNull null
        val values = listOf("title", "text", "body").mapNotNull { key ->
            val value = (languageData[key] as? String)?.trim()
                ?.takeIf { it.isNotBlank() && it.length <= 1800 }
                ?: return@mapNotNull null
            key to value
        }.toMap()
        language to values
    }.toMap()

    return RemoteContentState(
        version = version,
        maintenanceEnabled = maintenance?.get("enabled") == true,
        maintenanceTitle = (maintenance?.get("title") as? String).orEmpty().take(120),
        maintenanceMessage = (maintenance?.get("message") as? String).orEmpty().take(500),
        announcement = announcement,
        catalog = catalog,
        guides = guides,
        links = links,
        notificationTemplates = templates
    )
}

private fun safeRemoteHttpsUrl(value: Any?): String =
    (value as? String)?.trim()
        ?.takeIf { it.length <= 2000 && it.startsWith("https://", ignoreCase = true) }
        .orEmpty()
