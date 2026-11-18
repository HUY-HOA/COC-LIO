package com.huyhoa.coclio.data

import org.json.JSONArray
import org.json.JSONObject

data class SectionSpec(
    val key: String,
    val label: String,
    val tab: String,
    val base: String
)

private data class ExtraSectionSpec(
    val key: String,
    val label: String,
    val base: String
)

data class VillageExtraItem(
    val sectionKey: String,
    val sectionLabel: String,
    val base: String,
    val dataId: Int?,
    val level: Int?,
    val count: Int,
    val value: String? = null
) {
    val name: String
        get() = dataId?.let { CocNames.name(it, sectionKey) } ?: value ?: sectionLabel
}

data class VillageItem(
    val sectionKey: String,
    val sectionLabel: String,
    val tab: String,
    val base: String,
    val dataId: Int,
    val level: Int?,
    val count: Int,
    val timerSeconds: Long?,
    val helperCooldownSeconds: Long?,
    val gearUp: Boolean,
    val weaponLevel: Int?,
    val details: List<String>,
    val properties: Map<String, String> = emptyMap()
) {
    val name: String get() = CocNames.name(dataId, sectionKey)
    val isRunning: Boolean get() = timerSeconds != null && timerSeconds > 0L
    val isUpgradeRunning: Boolean get() = isRunning && !gearUp
    val isGearUpRunning: Boolean get() = isRunning && gearUp
    val isExpired: Boolean get() = timerSeconds != null && timerSeconds <= 0L
    val identity: String get() = "$sectionKey:$dataId:${level ?: 0}"
    val idleCount: Int get() = if (timerSeconds != null && timerSeconds > 0L) 0 else count

    fun remainingSecondsAt(snapshotTimestamp: Long, nowSeconds: Long): Long {
        val seconds = timerSeconds?.coerceAtLeast(0L) ?: return 0L
        val elapsed = (nowSeconds - snapshotTimestamp).coerceAtLeast(0L)
        return (seconds - elapsed).coerceAtLeast(0L)
    }

    fun progressRatioAt(snapshotTimestamp: Long, nowSeconds: Long): Float {
        val total = timerSeconds?.takeIf { it > 0L } ?: return 0f
        val elapsed = (nowSeconds - snapshotTimestamp).coerceIn(0L, total)
        return elapsed.toDouble().div(total.toDouble()).toFloat().coerceIn(0f, 1f)
    }

    fun progressPercentAt(snapshotTimestamp: Long, nowSeconds: Long): Int =
        (progressRatioAt(snapshotTimestamp, nowSeconds) * 100f).toInt().coerceIn(0, 100)

    fun isTimerActive(snapshotTimestamp: Long, nowSeconds: Long): Boolean =
        remainingSecondsAt(snapshotTimestamp, nowSeconds) > 0L
}

data class VillageSnapshot(
    val tag: String,
    val timestamp: Long,
    val items: List<VillageItem>,
    val extras: Map<String, Int>,
    val rawJson: String,
    val villageName: String? = null,
    val extraItems: List<VillageExtraItem> = emptyList(),
    val extraFields: Map<String, String> = emptyMap()
) {
    val homeItems: List<VillageItem> get() = items.filter { it.base == "home" }
    val builderItems: List<VillageItem> get() = items.filter { it.base == "builder" }
    val upgrades: List<VillageItem> get() = items.filter { it.isUpgradeRunning }
    val gearUps: List<VillageItem> get() = items.filter { it.isGearUpRunning }

    fun activeUpgradesAt(nowSeconds: Long): List<VillageItem> =
        (upgrades + gearUps).filter { it.isTimerActive(timestamp, nowSeconds) }

    fun expiredUpgradesAt(nowSeconds: Long): List<VillageItem> =
        (upgrades + gearUps).filterNot { it.isTimerActive(timestamp, nowSeconds) }

    val townHallLevel: Int? get() = items.firstOrNull {
        it.sectionKey == "buildings" && it.dataId == 1000001 && it.level != null
    }?.level
    val builderHallLevel: Int? get() = items.firstOrNull {
        it.sectionKey == "buildings2" && it.dataId == 1000034 && it.level != null
    }?.level
    val homeBuilderHutCount: Int get() = items
        .filter { it.sectionKey == "buildings" && it.dataId == 1000015 }
        .sumOf { it.count }

    val hasBobBuilder: Boolean get() = items.any {
        it.sectionKey == "buildings" && it.dataId == 1000064 && (it.level ?: 0) > 0
    }

    val homeBuilderTotal: Int? get() =
        (homeBuilderHutCount + if (hasBobBuilder) 1 else 0).takeIf { homeBuilderHutCount > 0 }

    fun homeBuildersBusyAt(nowSeconds: Long): Int = items.count { item ->
        item.base == "home" &&
            item.sectionKey in setOf("buildings", "traps", "heroes", "pets") &&
            !item.gearUp &&
            item.isTimerActive(timestamp, nowSeconds)
    }
}

data class ChangeEvent(
    val title: String,
    val detail: String,
    val completed: Boolean = false
)

object VillageParser {
    private val sections = listOf(
        SectionSpec("buildings", "Công trình", "buildings", "home"),
        SectionSpec("traps", "Bẫy", "buildings", "home"),
        SectionSpec("units", "Quân đội", "army", "home"),
        SectionSpec("siege_machines", "Máy công thành", "army", "home"),
        SectionSpec("heroes", "Anh hùng", "army", "home"),
        SectionSpec("spells", "Thần chú", "army", "home"),
        SectionSpec("pets", "Linh thú", "army", "home"),
        SectionSpec("guardians", "Hộ vệ", "army", "home"),
        SectionSpec("equipment", "Trang bị anh hùng", "army", "home"),
        SectionSpec("squads", "Đội hình Squad", "army", "home"),
        SectionSpec("helpers", "Trợ thủ", "other", "home"),
        SectionSpec("buildings2", "Công trình căn cứ thợ xây", "buildings", "builder"),
        SectionSpec("traps2", "Bẫy căn cứ thợ xây", "buildings", "builder"),
        SectionSpec("units2", "Quân đội căn cứ thợ xây", "army", "builder"),
        SectionSpec("heroes2", "Anh hùng căn cứ thợ xây", "army", "builder")
    )

    fun parse(raw: String): VillageSnapshot {
        require(raw.isNotBlank()) { "JSON đang trống." }
        val root = try {
            JSONObject(raw)
        } catch (_: Exception) {
            throw IllegalArgumentException("Không đọc được JSON. Hãy kiểm tra nội dung đã sao chép đầy đủ.")
        }

        val tag = root.optString("tag", "").trim()
        if (tag.isBlank() || !tag.startsWith("#")) {
            throw IllegalArgumentException("JSON không có mã người chơi hợp lệ.")
        }

        val rawTimestamp = root.optLong("timestamp", 0L)
        val timestamp = if (rawTimestamp >= 100_000_000_000L) rawTimestamp / 1000L else rawTimestamp
        if (timestamp <= 0L) {
            throw IllegalArgumentException("JSON không có timestamp hợp lệ.")
        }

        val villageName = listOf(
            "villageName",
            "village_name",
            "playerName",
            "player_name",
            "name"
        ).firstNotNullOfOrNull { key ->
            root.optString(key, "")
                .trim()
                .takeIf { it.isNotBlank() && it != "null" && it != tag }
        } ?: listOf("player", "profile", "village").firstNotNullOfOrNull { key ->
            val nested = root.optJSONObject(key) ?: return@firstNotNullOfOrNull null
            listOf("name", "playerName", "player_name", "villageName", "village_name")
                .firstNotNullOfOrNull { nestedKey ->
                    nested.optString(nestedKey, "")
                        .trim()
                        .takeIf { it.isNotBlank() && it != "null" && it != tag }
                }
        }

        val items = mutableListOf<VillageItem>()
        for (spec in sections) {
            val array = root.optJSONArray(spec.key) ?: continue
            for (index in 0 until array.length()) {
                val obj = array.optJSONObject(index) ?: continue
                val dataId = obj.optInt("data", -1)
                if (dataId < 0) continue
                val level = if (obj.has("lvl") && !obj.isNull("lvl")) obj.optInt("lvl") else null
                val timer = if (obj.has("timer") && !obj.isNull("timer")) obj.optLong("timer") else null
                val helperCooldown = if (obj.has("helper_cooldown") && !obj.isNull("helper_cooldown")) obj.optLong("helper_cooldown") else null
                val count = if (obj.has("cnt") && !obj.isNull("cnt")) {
                    obj.optInt("cnt", 1).coerceAtLeast(0)
                } else {
                    1
                }
                val gearUp = obj.optInt("gear_up", 0) > 0
                val weapon = if (obj.has("weapon")) obj.optInt("weapon") else null
                val details = buildList {
                    val types = obj.optJSONArray("types")
                    if (types != null) {
                        for (typeIndex in 0 until types.length()) {
                            val type = types.optJSONObject(typeIndex) ?: continue
                            val typeId = type.optInt("data", -1)
                            val typeName = CocNames.name(typeId, "module")
                            val modules = type.optJSONArray("modules")
                            val moduleText = if (modules == null) "" else {
                                (0 until modules.length()).mapNotNull { moduleIndex ->
                                    val module = modules.optJSONObject(moduleIndex) ?: return@mapNotNull null
                                    val moduleId = module.optInt("data", -1)
                                    val moduleLevel = module.optInt("lvl", 0)
                                    if (moduleId < 0) null else "${CocNames.name(moduleId, "module")} cấp $moduleLevel"
                                }.joinToString(", ")
                            }
                            add(if (moduleText.isBlank()) typeName else "$typeName: $moduleText")
                        }
                    }
                }
                val properties = linkedMapOf<String, String>()
                val itemKeys = obj.keys()
                while (itemKeys.hasNext()) {
                    val key = itemKeys.next()
                    if (key !in setOf("data", "lvl", "timer", "helper_cooldown", "cnt", "gear_up", "weapon", "types")) {
                        val value = obj.opt(key)
                        if (value != null && value !== JSONObject.NULL) {
                            properties[key] = value.toString()
                        }
                    }
                }
                items += VillageItem(
                    sectionKey = spec.key,
                    sectionLabel = spec.label,
                    tab = spec.tab,
                    base = spec.base,
                    dataId = dataId,
                    level = level,
                    count = count,
                    timerSeconds = timer,
                    helperCooldownSeconds = helperCooldown,
                    gearUp = gearUp,
                    weaponLevel = weapon,
                    details = details,
                    properties = properties
                )
            }
        }

        val extras = linkedMapOf<String, Int>()
        val extraItems = mutableListOf<VillageExtraItem>()
        val extraSpecs = listOf(
            ExtraSectionSpec("decos", "Đồ trang trí", "home"),
            ExtraSectionSpec("obstacles", "Chướng ngại vật", "home"),
            ExtraSectionSpec("house_parts", "Bộ phận nhà", "home"),
            ExtraSectionSpec("skins", "Trang phục tướng", "home"),
            ExtraSectionSpec("sceneries", "Phong cảnh", "home"),
            ExtraSectionSpec("decos2", "Đồ trang trí", "builder"),
            ExtraSectionSpec("obstacles2", "Chướng ngại vật", "builder"),
            ExtraSectionSpec("skins2", "Trang phục", "builder"),
            ExtraSectionSpec("sceneries2", "Phong cảnh", "builder")
        )
        extraSpecs.forEach { spec ->
            val array = root.optJSONArray(spec.key) ?: return@forEach
            var amount = 0
            for (index in 0 until array.length()) {
                val entry = array.opt(index)
                val obj = entry as? JSONObject
                val dataId = when {
                    obj != null -> obj.optInt("data", -1).takeIf { it >= 0 }
                    entry is Number -> entry.toInt().takeIf { it >= 0 }
                    entry is String -> entry.toIntOrNull()?.takeIf { it >= 0 }
                    else -> null
                }
                val level = obj?.takeIf { it.has("lvl") && !it.isNull("lvl") }?.optInt("lvl")
                val count = obj?.takeIf { it.has("cnt") && !it.isNull("cnt") }
                    ?.optInt("cnt", 1)?.coerceAtLeast(1) ?: 1
                val value = if (dataId == null && entry != null && entry !== JSONObject.NULL) entry.toString() else null
                if (dataId == null && value.isNullOrBlank()) continue
                extraItems += VillageExtraItem(
                    sectionKey = spec.key,
                    sectionLabel = spec.label,
                    base = spec.base,
                    dataId = dataId,
                    level = level,
                    count = count,
                    value = value
                )
                amount += count
            }
            extras[spec.key] = amount
        }

        val boosts = root.optJSONObject("boosts")
        if (boosts != null && boosts.length() > 0) {
            val boostKeys = boosts.keys()
            while (boostKeys.hasNext()) {
                val key = boostKeys.next()
                val value = boosts.opt(key)
                val label = when (key) {
                    "lab_boost" -> "Tăng tốc phòng thí nghiệm"
                    "builder_boost" -> "Tăng tốc thợ xây"
                    "clock_tower_boost" -> "Tăng tốc tháp đồng hồ"
                    else -> "Tăng tốc " + key.replace('_', ' ')
                }
                extraItems += VillageExtraItem(
                    sectionKey = "boosts",
                    sectionLabel = "Tăng tốc",
                    base = if (key.startsWith("builder") || key.startsWith("clock_tower")) "builder" else "home",
                    dataId = null,
                    level = null,
                    count = 1,
                    value = "$label: ${value ?: "—"}"
                )
            }
            extras["boosts"] = boosts.length()
        }

        val knownTopLevel = sections.map { it.key }.toSet() +
            extraSpecs.map { it.key }.toSet() +
            setOf(
                "tag", "timestamp", "villageName", "village_name", "playerName",
                "player_name", "name", "player", "profile", "village", "boosts"
            )
        val extraFields = linkedMapOf<String, String>()
        val rootKeys = root.keys()
        while (rootKeys.hasNext()) {
            val key = rootKeys.next()
            if (key !in knownTopLevel) {
                val value = root.opt(key)
                if (value != null && value !== JSONObject.NULL) {
                    extraFields[key] = value.toString()
                }
            }
        }

        return VillageSnapshot(
            tag = tag,
            timestamp = timestamp,
            items = items,
            extras = extras,
            rawJson = raw,
            villageName = villageName,
            extraItems = extraItems,
            extraFields = extraFields
        )
    }

    fun compare(previous: VillageSnapshot?, current: VillageSnapshot): List<ChangeEvent> {
        if (previous == null || previous.tag != current.tag) return emptyList()

        val result = mutableListOf<ChangeEvent>()
        val previousRunning = previous.items.filter { it.isUpgradeRunning }
            .groupingBy { it.identity }.eachCount()
        val currentRunning = current.items.filter { it.isUpgradeRunning }
            .groupingBy { it.identity }.eachCount()

        current.items.filter { it.isUpgradeRunning }.forEach { item ->
            val oldCount = previousRunning[item.identity] ?: 0
            val nowCount = currentRunning[item.identity] ?: 0
            if (nowCount > oldCount) {
                result += ChangeEvent(
                    title = "Bắt đầu nâng cấp",
                    detail = item.name + (item.level?.let { " • cấp $it" } ?: "")
                )
            }
        }

        previous.items.filter { it.isUpgradeRunning }.forEach { oldItem ->
            val oldCount = previousRunning[oldItem.identity] ?: 0
            val nowCount = currentRunning[oldItem.identity] ?: 0
            if (oldCount <= nowCount) return@forEach

            val nextLevel = (oldItem.level ?: 0) + 1
            val oldNextCount = idleCount(previous, oldItem, nextLevel)
            val newNextCount = idleCount(current, oldItem, nextLevel)
            val completed = newNextCount > oldNextCount

            result += ChangeEvent(
                title = if (completed) "Đã hoàn thành nâng cấp" else "Nâng cấp không còn trong bộ đếm",
                detail = if (completed) {
                    oldItem.name + " • cấp " + (oldItem.level ?: "?") + " → " + nextLevel
                } else {
                    oldItem.name + " • cần nhập lại để xác nhận cấp độ mới"
                },
                completed = completed
            )
        }

        val previousIdle = countByLevel(previous)
        val currentIdle = countByLevel(current)
        currentIdle.forEach { (key, count) ->
            val oldCount = previousIdle[key] ?: 0
            if (count > oldCount) {
                val sample = current.items.firstOrNull {
                    !it.isRunning && levelKey(it) == key
                } ?: return@forEach
                val itemGroupHadLowerLevel = previous.items.any {
                    it.sectionKey == sample.sectionKey &&
                        it.dataId == sample.dataId &&
                        (it.level ?: Int.MAX_VALUE) < (sample.level ?: Int.MIN_VALUE)
                }
                if (oldCount == 0 && itemGroupHadLowerLevel) {
                    val alreadyReported = result.any {
                        it.completed && it.detail.contains(sample.name)
                    }
                    if (!alreadyReported) {
                        result += ChangeEvent(
                            title = "Ghi nhận cấp độ mới",
                            detail = sample.name + " • cấp " + sample.level + " • +" + (count - oldCount)
                        )
                    }
                }
            }
        }

        return result.distinctBy { it.title + it.detail }.take(30)
    }

    private fun idleCount(snapshot: VillageSnapshot, reference: VillageItem, level: Int): Int {
        return snapshot.items.filter {
            it.sectionKey == reference.sectionKey &&
                it.dataId == reference.dataId &&
                it.level == level &&
                !it.isRunning
        }.sumOf { it.count }
    }

    private fun levelKey(item: VillageItem): String {
        return item.sectionKey + ":" + item.dataId + ":" + (item.level ?: 0)
    }

    private fun countByLevel(snapshot: VillageSnapshot): Map<String, Int> {
        return snapshot.items.filter { it.level != null && !it.isRunning }
            .groupBy { levelKey(it) }
            .mapValues { (_, entries) -> entries.sumOf { it.count } }
    }

    fun categoryItems(snapshot: VillageSnapshot, tab: String): List<VillageItem> {
        return when (tab) {
            "buildings" -> snapshot.items.filter { it.tab == "buildings" }
            "army" -> snapshot.items.filter { it.tab == "army" }
            else -> snapshot.items
        }
    }

    fun categoryLabel(tab: String): String = when (tab) {
        "buildings" -> "Công trình & bẫy"
        "army" -> "Quân đội & nghiên cứu"
        else -> "Tất cả"
    }
}

object CocNames {
    private val names = mapOf(
        1000000 to "Trại quân", 1000001 to "Nhà chính", 1000002 to "Mỏ tinh chất",
        1000003 to "Kho tinh chất", 1000004 to "Mỏ vàng", 1000005 to "Kho vàng",
        1000006 to "Doanh trại", 1000007 to "Phòng thí nghiệm", 1000008 to "Đại bác",
        1000009 to "Tháp cung", 1000010 to "Tường", 1000011 to "Tháp pháp sư",
        1000012 to "Phòng không", 1000013 to "Cối", 1000014 to "Thành bang hội",
        1000015 to "Nhà thợ xây", 1000019 to "Tesla ẩn", 1000020 to "Nhà máy Thần chú",
        1000021 to "Nỏ X", 1000023 to "Mỏ hắc tiên", 1000024 to "Kho hắc tiên",
        1000026 to "Nhà lính hắc tiên", 1000027 to "Tháp địa ngục", 1000028 to "Quạt gió",
        1000029 to "Nhà máy Thần chú Hắc ám", 1000031 to "Pháo đại bàng", 1000032 to "Tháp bom",
        1000059 to "Xưởng công thành", 1000064 to "Nhà B.O.B", 1000067 to "Tháp pháo bắn chùm",
        1000068 to "Nhà thú cưng", 1000070 to "Lò rèn", 1000071 to "Sảnh anh hùng",
        1000072 to "Tháp Thần chú", 1000077 to "Đại bác Monolith", 1000079 to "Tháp đa nòng",
        1000084 to "Tháp cung đa mục tiêu", 1000085 to "Đại bác dội", 1000086 to "Tháp phản công",
        1000089 to "Tháp phun lửa", 1000093 to "Nhà trợ thủ", 1000097 to "Trạm chế tạo",
        1000102 to "Tháp pháp sư siêu cấp",
        1000033 to "Tường căn cứ thợ xây", 1000034 to "Nhà chính căn cứ thợ xây",
        1000035 to "Mỏ tinh chất căn cứ thợ xây", 1000036 to "Kho tinh chất căn cứ thợ xây",
        1000037 to "Mỏ vàng căn cứ thợ xây", 1000038 to "Kho vàng căn cứ thợ xây",
        1000039 to "Tháp đồng hồ", 1000040 to "Doanh trại căn cứ thợ xây",
        1000041 to "Đại bác đôi", 1000042 to "Trại quân căn cứ thợ xây",
        1000043 to "Tesla ẩn căn cứ thợ xây", 1000044 to "Đại bác căn cứ thợ xây",
        1000045 to "Cối đa nòng", 1000046 to "Phòng thí nghiệm sao",
        1000047 to "Nhà B.O.T.O", 1000048 to "Tháp cung căn cứ thợ xây",
        1000049 to "Trại tiếp viện", 1000050 to "Pháo bông",
        1000051 to "Trạm canh gác", 1000052 to "Tesla khổng lồ",
        1000053 to "Bệ máy chiến đấu", 1000054 to "Bom bay căn cứ thợ xây",
        1000055 to "Máy nghiền", 1000056 to "Lò nung",
        1000057 to "Đại bác khổng lồ", 1000058 to "Mỏ ngọc",
        1000063 to "Máy phóng dung nham", 1000065 to "Trung tâm điều khiển B.O.B",
        1000078 to "Tiền đồn O.T.T.O", 1000080 to "Bệ trực thăng chiến đấu",
        1000081 to "Nỏ X căn cứ thợ xây", 1000082 to "Nhà hồi máu",
        12000000 to "Bom", 12000001 to "Bẫy lò xo", 12000002 to "Bom khổng lồ",
        12000005 to "Bom bay", 12000006 to "Mìn tìm nhiệt", 12000008 to "Bẫy bộ xương",
        12000010 to "Bẫy lò xo căn cứ thợ xây", 12000011 to "Bẫy đẩy căn cứ thợ xây",
        12000013 to "Mìn căn cứ thợ xây", 12000014 to "Mìn khổng lồ căn cứ thợ xây",
        12000016 to "Bẫy lốc xoáy", 12000020 to "Bom Giga",
        28000000 to "Vua man di", 28000001 to "Nữ hoàng cung thủ",
        28000002 to "Đại hộ vệ", 28000003 to "Cỗ máy chiến đấu",
        28000004 to "Nhà vô địch Hoàng gia", 28000005 to "Trực thăng chiến đấu",
        28000006 to "Hoàng tử tiểu quỷ", 28000007 to "Công tước Rồng",
        4000000 to "Chiến binh man di", 4000001 to "Cung thủ", 4000002 to "Yêu tinh",
        4000003 to "Khổng lồ", 4000004 to "Phá tường", 4000005 to "Bóng bay",
        4000006 to "Pháp sư", 4000007 to "Nữ tu hồi máu", 4000008 to "Rồng",
        4000009 to "P.E.K.K.A", 4000010 to "Tiểu quỷ", 4000011 to "Kỵ sĩ heo",
        4000012 to "Nữ chiến binh", 4000013 to "Người đá", 4000015 to "Phù thủy",
        4000017 to "Chó dung nham", 4000022 to "Kẻ ném đá", 4000023 to "Rồng con",
        4000024 to "Thợ mỏ", 4000051 to "Máy phá tường",
        4000052 to "Khinh khí cầu chiến đấu", 4000053 to "Người tuyết Yeti",
        4000058 to "Người đá băng", 4000059 to "Rồng điện", 4000062 to "Máy nghiền đá",
        4000065 to "Kỵ sĩ rồng", 4000082 to "Thợ săn anh hùng",
        4000087 to "Máy phóng khúc gỗ", 4000091 to "Máy phun lửa",
        4000092 to "Máy khoan chiến đấu", 4000095 to "Titan điện",
        4000097 to "Người canh giữ tập sự", 4000109 to "Phù thủy tàn tích",
        4000110 to "Kỵ sĩ rễ cây", 4000123 to "Tu sĩ rừng", 4000132 to "Kẻ ném",
        4000135 to "Bệ phóng quân", 4000150 to "Lò lửa",
        4000177 to "Khổng lồ thiên thạch", 4000188 to "Xe bay",
        4000031 to "Chiến binh man di cuồng nộ", 4000032 to "Cung thủ lén lút",
        4000033 to "Tiểu quỷ Beta", 4000034 to "Khổng lồ đấm bốc",
        4000035 to "Kẻ ném bom", 4000036 to "P.E.K.K.A quyền năng",
        4000037 to "Xe pháo", 4000038 to "Tàu thả bom",
        4000041 to "Rồng con", 4000042 to "Phù thủy đêm",
        4000070 to "Kỵ sĩ heo bay", 4000075 to "Doanh trại công thành",
        4000106 to "Pháp sư điện lửa",
        26000000 to "Thần chú Sấm sét", 26000001 to "Thần chú Hồi máu",
        26000002 to "Thần chú Thịnh nộ", 26000003 to "Thần chú Bật nhảy",
        26000005 to "Thần chú Đóng băng", 26000009 to "Thần chú Độc dược",
        26000010 to "Thần chú Động đất", 26000011 to "Thần chú Tăng tốc",
        26000016 to "Thần chú Nhân bản", 26000017 to "Thần chú Bộ xương",
        26000028 to "Thần chú Dơi", 26000035 to "Thần chú Tàng hình",
        26000053 to "Thần chú Thu quân", 26000070 to "Thần chú Siêu tăng trưởng",
        26000098 to "Thần chú Hồi sinh", 26000109 to "Thần chú Khiên chắn băng",
        26000120 to "Thần chú Vật tổ", 26000123 to "Thần chú Giận dữ",
        26000153 to "Thần chú Anh hùng",
        73000000 to "L.A.S.S.I", 73000001 to "Yak dũng mãnh", 73000002 to "Cú điện",
        73000003 to "Kỳ lân", 73000004 to "Phượng hoàng",
        73000007 to "Thằn lằn độc", 73000008 to "Diggy",
        73000009 to "Frosty băng giá", 73000010 to "Cáo linh",
        73000011 to "Thạch dẻo giận dữ", 73000016 to "Hắt hơi", 73000017 to "Quạ tham ăn",
        107000008 to "Logger", 107000000 to "Longshot", 107000001 to "Smasher",
        90000000 to "Búp bê man di", 90000001 to "Lọ cuồng nộ",
        90000002 to "Búp bê cung thủ", 90000003 to "Lọ tàng hình",
        90000004 to "Sách bất tử", 90000005 to "Ngọc sinh mệnh",
        90000006 to "Khiên truy đuổi", 90000007 to "Ngọc Hoàng gia",
        90000008 to "Giày động đất", 90000009 to "Búp bê Kỵ sĩ heo",
        90000010 to "Găng tay khổng lồ", 90000011 to "Râu ma cà rồng",
        90000012 to "Lọ tăng tốc", 90000013 to "Giáo tên lửa",
        90000014 to "Bóng gai", 90000015 to "Mũi tên băng",
        90000016 to "Nanh điện", 90000017 to "Mũi tên khổng lồ",
        90000019 to "Ngọn đuốc anh hùng", 90000020 to "Búp bê nữ tu hồi máu",
        90000022 to "Cầu lửa", 90000024 to "Ngọc cuồng nộ",
        90000032 to "Vòng tay rắn", 90000034 to "Sách hồi máu",
        90000035 to "Vương miện bóng tối", 90000039 to "Gương phép",
        90000040 to "Giày điện", 90000041 to "Búp bê Lavaloon",
        90000042 to "Búp bê tay sai", 90000043 to "Quả cầu bóng tối",
        90000044 to "Quần kim loại", 90000047 to "Sắt quý tộc",
        90000048 to "Hình nhân hành động", 90000049 to "Trượng thiên thạch",
        90000050 to "Mảnh băng", 90000051 to "Ngựa gậy",
        90000052 to "Trái tim lửa", 90000053 to "Ba lô tên lửa",
        90000056 to "Súng gây choáng", 90000057 to "Máy thổi lửa",
        90000059 to "Mũi tên Monolith", 90000060 to "Bộ bài trả thù",
        90000061 to "Mặt dây chuyền cổng không gian",
        93000000 to "Thợ xây tập sự", 93000001 to "Trợ lý phòng thí nghiệm",
        93000002 to "Người đào quặng",
        103000008 to "Lò nướng", 103000009 to "Bom bay",
        103000010 to "Máy phóng dung nham", 103000011 to "Nến nóng",
        103000012 to "Thợ săn anh hùng", 103000013 to "Máy bắn bánh",
        102000033 to "Máu Nến nóng", 102000034 to "Sát thương Nến nóng",
        102000035 to "Tốc độ Nến nóng", 102000036 to "Máu Thợ săn anh hùng",
        102000037 to "Sát thương Thợ săn anh hùng",
        102000038 to "Tầm bắn Thợ săn anh hùng",
        102000039 to "Máu Máy bắn bánh", 102000040 to "Sát thương Máy bắn bánh",
        102000041 to "Hiệu ứng Máy bắn bánh",
        4000195 to "Đội hình Man di & Cung thủ",
        4000196 to "Đội hình Khổng lồ & Pháp sư",
        4000197 to "Đội hình Phá tường",
        4000198 to "Đội hình Tiểu quỷ",
        4000199 to "Đội hình Bóng bay",
        4000200 to "Đội hình Người đá & Phù thủy",
        4000201 to "Đội hình Kỵ sĩ heo",
        4000202 to "Đội hình Thợ mỏ",
        4000203 to "Đội hình Hồi máu",
        4000204 to "Đội hình P.E.K.K.A & Titan điện",
        4000205 to "Đội hình Rồng",
        4000206 to "Đội hình Rồng con"
    )

    fun name(id: Int, section: String): String {
        if (id < 0) return "Không xác định"
        return names[id] ?: when {
            section == "skins" || section == "skins2" -> "Trang phục #$id"
            section == "sceneries" || section == "sceneries2" -> "Phong cảnh #$id"
            section == "decos" || section == "decos2" -> "Đồ trang trí #$id"
            section == "obstacles" || section == "obstacles2" -> "Chướng ngại vật #$id"
            section == "house_parts" -> "Bộ phận nhà #$id"
            section == "spells" -> "Thần chú #$id"
            section.endsWith("2") -> "Mục căn cứ thợ xây #$id"
            else -> "Mục #$id"
        }
    }
}
