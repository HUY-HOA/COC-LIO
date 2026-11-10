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

    fun loadAll(context: Context, profile: String = "local"): List<VillageSnapshot> {
        val rawHistory = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(historyKey(profile), "[]") ?: "[]"
        val array = try {
            JSONArray(rawHistory)
        } catch (_: Exception) {
            JSONArray()
        }

        val snapshots = mutableListOf<VillageSnapshot>()
        for (index in 0 until array.length()) {
            val entry = array.optJSONObject(index) ?: continue
            val raw = entry.optString("raw", "")
            if (raw.isBlank()) continue
            try {
                snapshots += VillageParser.parse(raw)
            } catch (_: Exception) {
            }
        }
        return snapshots.sortedByDescending { it.timestamp }
    }

    fun save(context: Context, snapshot: VillageSnapshot, profile: String = "local") {
        val key = historyKey(profile)
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

        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(key, array.toString())
            .apply()
    }
}
