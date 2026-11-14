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
        snapshots.distinctBy { it.tag to it.timestamp }
            .sortedByDescending { it.timestamp }
            .take(MAX_SNAPSHOTS)
            .forEach { item ->
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

    fun permanentlyDelete(context: Context, recordId: String, profile: String = "local"): Boolean {
        val raw = preferences(context).getString(deletedKey(profile), "[]") ?: "[]"
        val array = runCatching { JSONArray(raw) }.getOrElse { JSONArray() }
        val updated = JSONArray()
        var removed = false
        for (index in 0 until array.length()) {
            val entry = array.optJSONObject(index) ?: continue
            val id = entry.optString("id", "").takeIf { it.isNotBlank() }
                ?: entry.optString("tag", "") + ":" + entry.optLong("deleted_at", 0L) + ":" + index
            if (id == recordId) removed = true else updated.put(entry)
        }
        if (removed) preferences(context).edit().putString(deletedKey(profile), updated.toString()).apply()
        return removed
    }

    fun restoreDeleted(
        context: Context,
        recordId: String,
        profile: String = "local"
    ): Boolean {
        val record = loadDeleted(context, profile).firstOrNull { it.id == recordId } ?: return false
        if (record.snapshots.isEmpty()) return false

        val existing = loadAll(context, profile)
        writeHistory(context, existing + record.snapshots, profile)
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


data class ReceivedSharedJsonRecord(
    val id: String,
    val senderUid: String,
    val senderName: String,
    val tag: String,
    val villageName: String,
    val timestamp: Long,
    val receivedAt: Long,
    val rawJson: String
)

data class DeletedReceivedSharedJsonRecord(
    val record: ReceivedSharedJsonRecord,
    val deletedAt: Long
)

object ReceivedSharedJsonStore {
    private const val PREFS = "coc_lio_local"
    private const val MAX_RECEIVED = 20
    private const val MAX_DELETED = 30

    private fun receivedKey(profile: String) =
        "received_shared_json_" + profile.hashCode().toUInt().toString(16)

    private fun deletedKey(profile: String) =
        "deleted_shared_json_" + profile.hashCode().toUInt().toString(16)

    private fun preferences(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun encode(record: ReceivedSharedJsonRecord) = JSONObject()
        .put("id", record.id)
        .put("sender_uid", record.senderUid)
        .put("sender_name", record.senderName)
        .put("tag", record.tag)
        .put("village_name", record.villageName)
        .put("timestamp", record.timestamp)
        .put("received_at", record.receivedAt)
        .put("raw", record.rawJson)

    private fun decode(value: JSONObject): ReceivedSharedJsonRecord? {
        val id = value.optString("id", "")
        val raw = value.optString("raw", "")
        if (id.isBlank() || raw.isBlank()) return null
        return ReceivedSharedJsonRecord(
            id = id,
            senderUid = value.optString("sender_uid", ""),
            senderName = value.optString("sender_name", "Thành viên"),
            tag = value.optString("tag", ""),
            villageName = value.optString("village_name", ""),
            timestamp = value.optLong("timestamp", 0L),
            receivedAt = value.optLong("received_at", 0L),
            rawJson = raw
        )
    }

    fun loadReceived(context: Context, profile: String = "local"): List<ReceivedSharedJsonRecord> {
        val raw = preferences(context).getString(receivedKey(profile), "[]") ?: "[]"
        val array = runCatching { JSONArray(raw) }.getOrElse { JSONArray() }
        return (0 until array.length()).mapNotNull { decode(array.optJSONObject(it) ?: return@mapNotNull null) }
            .sortedByDescending { it.receivedAt }
    }

    fun saveReceived(
        context: Context,
        receiverUid: String,
        senderUid: String,
        senderName: String,
        tag: String,
        villageName: String,
        timestamp: Long,
        jsonId: String,
        rawJson: String,
        profile: String = "local"
    ): Boolean {
        if (receiverUid.isBlank() || senderUid.isBlank() || receiverUid == senderUid ||
            jsonId.isBlank() || rawJson.isBlank()
        ) return false
        if (rawJson.toByteArray(Charsets.UTF_8).size > 700_000) return false
        val existing = loadReceived(context, profile).filterNot { it.id == jsonId }
        val record = ReceivedSharedJsonRecord(
            id = jsonId,
            senderUid = senderUid,
            senderName = senderName.ifBlank { "Thành viên" }.take(80),
            tag = tag.take(20),
            villageName = villageName.take(100),
            timestamp = timestamp,
            receivedAt = System.currentTimeMillis() / 1000L,
            rawJson = rawJson
        )
        val array = JSONArray()
        (listOf(record) + existing).take(MAX_RECEIVED).forEach { array.put(encode(it)) }
        preferences(context).edit().putString(receivedKey(profile), array.toString()).apply()
        return true
    }

    fun loadDeleted(context: Context, profile: String = "local"): List<DeletedReceivedSharedJsonRecord> {
        val raw = preferences(context).getString(deletedKey(profile), "[]") ?: "[]"
        val array = runCatching { JSONArray(raw) }.getOrElse { JSONArray() }
        return (0 until array.length()).mapNotNull { index ->
            val value = array.optJSONObject(index) ?: return@mapNotNull null
            val record = decode(value) ?: return@mapNotNull null
            DeletedReceivedSharedJsonRecord(record, value.optLong("deleted_at", 0L))
        }.sortedByDescending { it.deletedAt }
    }

    fun deleteReceived(context: Context, jsonId: String, profile: String = "local"): Boolean {
        if (jsonId.isBlank()) return false
        val existing = loadReceived(context, profile)
        if (existing.none { it.id == jsonId }) return false
        val updated = JSONArray()
        existing.filterNot { it.id == jsonId }.forEach { updated.put(encode(it)) }
        preferences(context).edit().putString(receivedKey(profile), updated.toString()).apply()
        return true
    }

    fun moveToDeleted(context: Context, jsonId: String, profile: String = "local"): Boolean {
        val record = loadReceived(context, profile).firstOrNull { it.id == jsonId } ?: return false
        val active = JSONArray()
        loadReceived(context, profile).filterNot { it.id == jsonId }.forEach { active.put(encode(it)) }
        val deleted = JSONArray()
        loadDeleted(context, profile).forEach {
            deleted.put(encode(it.record).put("deleted_at", it.deletedAt))
        }
        deleted.put(encode(record).put("deleted_at", System.currentTimeMillis() / 1000L))
        val trimmed = JSONArray()
        val start = (deleted.length() - MAX_DELETED).coerceAtLeast(0)
        for (index in start until deleted.length()) trimmed.put(deleted.get(index))
        preferences(context).edit()
            .putString(receivedKey(profile), active.toString())
            .putString(deletedKey(profile), trimmed.toString())
            .apply()
        return true
    }

    fun restore(context: Context, jsonId: String, profile: String = "local"): Boolean {
        val deletedRecord = loadDeleted(context, profile).firstOrNull { it.record.id == jsonId } ?: return false
        val received = JSONArray()
        loadReceived(context, profile).filterNot { it.id == jsonId }.forEach { received.put(encode(it)) }
        received.put(encode(deletedRecord.record))
        val deleted = JSONArray()
        loadDeleted(context, profile).filterNot { it.record.id == jsonId }.forEach {
            deleted.put(encode(it.record).put("deleted_at", it.deletedAt))
        }
        preferences(context).edit()
            .putString(receivedKey(profile), received.toString())
            .putString(deletedKey(profile), deleted.toString())
            .apply()
        return true
    }

    fun permanentlyDelete(context: Context, jsonId: String, profile: String = "local"): Boolean {
        val before = loadDeleted(context, profile)
        if (before.none { it.record.id == jsonId }) return false
        val deleted = JSONArray()
        before.filterNot { it.record.id == jsonId }.forEach {
            deleted.put(encode(it.record).put("deleted_at", it.deletedAt))
        }
        preferences(context).edit().putString(deletedKey(profile), deleted.toString()).apply()
        return true
    }
}
