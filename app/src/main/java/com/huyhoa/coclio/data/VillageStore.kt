package com.huyhoa.coclio.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object VillageStore {
    private const val PREFS = "coc_lio_local"
    private const val KEY_HISTORY = "village_snapshots"
    private const val MAX_SNAPSHOTS = 60

    private fun historyKey(profile: String): String =
        if (profile == "local") KEY_HISTORY
        else KEY_HISTORY + "_" + profile.hashCode().toUInt().toString(16)

    private fun namesKey(profile: String): String =
        "village_names_" + profile.hashCode().toUInt().toString(16)

    private fun readNames(context: Context, profile: String): JSONObject {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(namesKey(profile), "{}") ?: "{}"
        return runCatching { JSONObject(raw) }.getOrElse { JSONObject() }
    }

    fun loadAll(context: Context, profile: String = "local"): List<VillageSnapshot> {
        val preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val rawHistory = preferences.getString(historyKey(profile), "[]") ?: "[]"
        val array = runCatching { JSONArray(rawHistory) }.getOrElse { JSONArray() }
        val names = readNames(context, profile)
        val snapshots = mutableListOf<VillageSnapshot>()

        for (index in 0 until array.length()) {
            val entry = array.optJSONObject(index) ?: continue
            val raw = entry.optString("raw", "")
            if (raw.isBlank()) continue
            runCatching { VillageParser.parse(raw) }.getOrNull()?.let { parsed ->
                val savedName = names.optString(parsed.tag, "").trim()
                snapshots += parsed.copy(
                    villageName = savedName.takeIf { it.isNotBlank() } ?: parsed.villageName
                )
            }
        }
        return snapshots.sortedByDescending { it.timestamp }
    }

    fun save(
        context: Context,
        snapshot: VillageSnapshot,
        profile: String = "local",
        villageNameOverride: String? = null
    ) {
        val preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val key = historyKey(profile)
        val nameOverride = villageNameOverride?.trim().orEmpty()
        if (nameOverride.isNotBlank()) {
            val names = readNames(context, profile)
            names.put(snapshot.tag, nameOverride)
            preferences.edit().putString(namesKey(profile), names.toString()).apply()
        }

        val existing = loadAll(context, profile)
            .filterNot { it.tag == snapshot.tag && it.timestamp == snapshot.timestamp }
        val all = (listOf(snapshot) + existing)
            .sortedByDescending { it.timestamp }
            .take(MAX_SNAPSHOTS)

        val array = JSONArray()
        all.forEach { item ->
            array.put(
                JSONObject()
                    .put("tag", item.tag)
                    .put("timestamp", item.timestamp)
                    .put("raw", item.rawJson)
            )
        }

        preferences.edit().putString(key, array.toString()).apply()
    }
}
