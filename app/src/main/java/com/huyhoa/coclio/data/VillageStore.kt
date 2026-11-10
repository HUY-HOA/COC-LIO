package com.huyhoa.coclio.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class DeletedVillageRecord(
    val id: String,
    val tag: String,
    val villageName: String?,
    val deletedAt: Long,
    val snapshots: List<VillageSnapshot>,
    val wasPinned: Boolean
)

object VillageStore {
    private const val PREFS = "coc_lio_local"
    private const val KEY_HISTORY = "village_snapshots"
    private const val MAX_SNAPSHOTS = 60
    private const val MAX_DELETED_RECORDS = 20

    private fun historyKey(profile: String): String =
        if (profile == "local") KEY_HISTORY
        else KEY_HISTORY + "_" + profile.hashCode().toUInt().toString(16)

    private fun namesKey(profile: String): String =
        "village_names_" + profile.hashCode().toUInt().toString(16)

    private fun pinsKey(profile: String): String =
        "village_pins_" + profile.hashCode().toUInt().toString(16)

    private fun deletedKey(profile: String): String =
        "village_deleted_" + profile.hashCode().toUInt().toString(16)

    private fun preferences(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun readNames(context: Context, profile: String): JSONObject {
        val raw = preferences(context).getString(namesKey(profile), "{}") ?: "{}"
        return runCatching { JSONObject(raw) }.getOrElse { JSONObject() }
    }

    fun pinnedTags(context: Context, profile: String = "local"): List<String> {
        val raw = preferences(context).getString(pinsKey(profile), "[]") ?: "[]"
        val array = runCatching { JSONArray(raw) }.getOrElse { JSONArray() }
        return (0 until array.length()).mapNotNull {
            array.optString(it).takeIf(String::isNotBlank)
        }
    }

    fun setPinned(context: Context, tag: String, pinned: Boolean, profile: String = "local") {
        val tags = pinnedTags(context, profile).toMutableList()
        if (pinned) {
            tags.remove(tag)
            tags.add(tag)
        } else {
            tags.removeAll { it == tag }
        }
        val array = JSONArray()
        tags.forEach(array::put)
        preferences(context).edit().putString(pinsKey(profile), array.toString()).apply()
    }

    fun loadAll(context: Context, profile: String = "local"): List<VillageSnapshot> {
        val prefs = preferences(context)
        val rawHistory = prefs.getString(historyKey(profile), "[]") ?: "[]"
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

        val pinOrder = pinnedTags(context, profile).withIndex().associate { it.value to it.index }
        return snapshots.sortedWith(
            compareBy<VillageSnapshot> { pinOrder[it.tag] ?: Int.MAX_VALUE }
                .thenByDescending { it.timestamp }
        )
    }

    private fun writeHistory(context: Context, snapshots: List<VillageSnapshot>, profile: String) {
        val array = JSONArray()
        snapshots.sortedByDescending { it.timestamp }.take(MAX_SNAPSHOTS).forEach { item ->
            array.put(
                JSONObject()
                    .put("tag", item.tag)
                    .put("timestamp", item.timestamp)
                    .put("raw", item.rawJson)
            )
        }
        preferences(context).edit().putString(historyKey(profile), array.toString()).apply()
    }

    fun save(
        context: Context,
        snapshot: VillageSnapshot,
        profile: String = "local",
        villageNameOverride: String? = null
    ) {
        val prefs = preferences(context)
        val nameOverride = villageNameOverride?.trim().orEmpty()
        if (nameOverride.isNotBlank()) {
            val names = readNames(context, profile)
            names.put(snapshot.tag, nameOverride)
            prefs.edit().putString(namesKey(profile), names.toString()).apply()
        }

        val existing = loadAll(context, profile)
            .filterNot { it.tag == snapshot.tag && it.timestamp == snapshot.timestamp }
        writeHistory(context, listOf(snapshot) + existing, profile)
    }

    fun deleteVillage(context: Context, tag: String, profile: String = "local"): Boolean {
        val all = loadAll(context, profile)
        val removed = all.filter { it.tag == tag }
        if (removed.isEmpty()) return false

        val wasPinned = pinnedTags(context, profile).contains(tag)
        val names = readNames(context, profile)
        val savedName = names.optString(tag, "").trim()
            .takeIf { it.isNotBlank() } ?: removed.firstOrNull()?.villageName
        val snapshots = JSONArray()
        removed.forEach { snapshot ->
            snapshots.put(JSONObject().put("timestamp", snapshot.timestamp).put("raw", snapshot.rawJson))
        }

        val raw = preferences(context).getString(deletedKey(profile), "[]") ?: "[]"
        val stored = runCatching { JSONArray(raw) }.getOrElse { JSONArray() }
        val updated = JSONArray()
        for (index in 0 until stored.length()) updated.put(stored.get(index))
        val deletedAt = System.currentTimeMillis() / 1000L
        updated.put(
            JSONObject()
                .put("id", UUID.randomUUID().toString())
                .put("tag", tag)
                .put("name", savedName ?: "")
                .put("deleted_at", deletedAt)
                .put("was_pinned", wasPinned)
                .put("snapshots", snapshots)
        )

        val trimmed = JSONArray()
        val start = (updated.length() - MAX_DELETED_RECORDS).coerceAtLeast(0)
        for (index in start until updated.length()) trimmed.put(updated.get(index))
        preferences(context).edit().putString(deletedKey(profile), trimmed.toString()).apply()

        writeHistory(context, all.filterNot { it.tag == tag }, profile)
        if (wasPinned) setPinned(context, tag, false, profile)
        return true
    }

    fun loadDeleted(context: Context, profile: String = "local"): List<DeletedVillageRecord> {
        val raw = preferences(context).getString(deletedKey(profile), "[]") ?: "[]"
        val array = runCatching { JSONArray(raw) }.getOrElse { JSONArray() }
        val names = readNames(context, profile)
        val result = mutableListOf<DeletedVillageRecord>()

        for (index in 0 until array.length()) {
            val entry = array.optJSONObject(index) ?: continue
            val tag = entry.optString("tag", "")
            if (tag.isBlank()) continue
            val snapshotsArray = entry.optJSONArray("snapshots") ?: JSONArray()
            val snapshots = mutableListOf<VillageSnapshot>()
            for (snapshotIndex in 0 until snapshotsArray.length()) {
                val rawSnapshot = snapshotsArray.optJSONObject(snapshotIndex)?.optString("raw", "").orEmpty()
                if (rawSnapshot.isBlank()) continue
                runCatching { VillageParser.parse(rawSnapshot) }.getOrNull()?.let { parsed ->
                    val entryName = entry.optString("name", "").trim()
                    val savedName = names.optString(tag, "").trim()
                    snapshots += parsed.copy(
                        villageName = entryName.takeIf { it.isNotBlank() }
                            ?: savedName.takeIf { it.isNotBlank() }
                            ?: parsed.villageName
                    )
                }
            }
            val deletedAt = entry.optLong("deleted_at", 0L)
            result += DeletedVillageRecord(
                id = entry.optString("id", "").takeIf { it.isNotBlank() } ?: "$tag:$deletedAt:$index",
                tag = tag,
                villageName = entry.optString("name", "").trim().takeIf { it.isNotBlank() }
                    ?: snapshots.firstOrNull()?.villageName,
                deletedAt = deletedAt,
                snapshots = snapshots.sortedByDescending { it.timestamp },
                wasPinned = entry.optBoolean("was_pinned", false)
            )
        }
        return result.sortedByDescending { it.deletedAt }
    }

    fun restoreDeleted(
        context: Context,
        recordId: String,
        profile: String = "local"
    ): Boolean {
        val record = loadDeleted(context, profile).firstOrNull { it.id == recordId } ?: return false
        if (record.snapshots.isEmpty()) return false

        val existing = loadAll(context, profile)
        writeHistory(context, record.snapshots + existing, profile)
        if (record.wasPinned) setPinned(context, record.tag, true, profile)

        val raw = preferences(context).getString(deletedKey(profile), "[]") ?: "[]"
        val array = runCatching { JSONArray(raw) }.getOrElse { JSONArray() }
        val updated = JSONArray()
        for (index in 0 until array.length()) {
            val entry = array.optJSONObject(index) ?: continue
            val id = entry.optString("id", "").takeIf { it.isNotBlank() }
                ?: entry.optString("tag", "") + ":" + entry.optLong("deleted_at", 0L) + ":" + index
            if (id != recordId) updated.put(entry)
        }
        preferences(context).edit().putString(deletedKey(profile), updated.toString()).apply()
        return true
    }
}
