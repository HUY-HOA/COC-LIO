package com.huyhoa.coclio.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object VillageStore {
    private const val PREFS = "coc_lio_local"
    private const val KEY_HISTORY = "village_snapshots"
    private const val MAX_SNAPSHOTS = 60

    fun loadAll(context: Context): List<VillageSnapshot> {
        val rawHistory = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_HISTORY, "[]") ?: "[]"
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

    fun save(context: Context, snapshot: VillageSnapshot) {
        val existing = loadAll(context)
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
            .putString(KEY_HISTORY, array.toString())
            .apply()
    }
}
