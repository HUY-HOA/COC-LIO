package com.huyhoa.coclio.data

import org.json.JSONArray
import org.json.JSONObject

data class SectionSpec(
    val key: String,
    val label: String,
    val tab: String,
    val base: String
)

data class VillageItem(
    val sectionKey: String,
    val sectionLabel: String,
    val tab: String,
    val base: String,
    val dataId: Int,
    val level: Int?,
    val count: Int,
    val timerSeconds: Long?,
    val gearUp: Boolean,
    val weaponLevel: Int?,
    val details: List<String>
) {
    val name: String get() = CocNames.name(dataId, sectionKey)
    val isRunning: Boolean get() = timerSeconds != null && timerSeconds > 0L
    val isExpired: Boolean get() = timerSeconds != null && timerSeconds <= 0L
    val identity: String get() = "$sectionKey:$dataId:${level ?: 0}"
    val idleCount: Int get() = if (timerSeconds != null && timerSeconds > 0L) 0 else count
}

data class VillageSnapshot(
    val tag: String,
    val timestamp: Long,
    val items: List<VillageItem>,
    val extras: Map<String, Int>,
    val rawJson: String
) {
    val homeItems: List<VillageItem> get() = items.filter { it.base == "home" }
    val builderItems: List<VillageItem> get() = items.filter { it.base == "builder" }
    val upgrades: List<VillageItem> get() = items.filter { it.isRunning }
    val townHallLevel: Int? get() = items.firstOrNull {
        it.sectionKey == "buildings" && it.dataId == 1000001 && it.level != null
    }?.level
    val builderHallLevel: Int? get() = items.firstOrNull {
        it.sectionKey == "buildings2" && it.dataId == 1000034 && it.level != null
    }?.level
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
        SectionSpec("spells", "Phép thuật", "army", "home"),
        SectionSpec("pets", "Thú cưng", "army", "home"),
        SectionSpec("equipment", "Trang bị anh hùng", "army", "home"),
        SectionSpec("helpers", "Trợ thủ", "other", "home"),
        SectionSpec("buildings2", "Công trình Builder Base", "buildings", "builder"),
        SectionSpec("traps2", "Bẫy Builder Base", "buildings", "builder"),
        SectionSpec("units2", "Quân đội Builder Base", "army", "builder"),
        SectionSpec("heroes2", "Anh hùng Builder Base", "army", "builder")
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
            throw IllegalArgumentException("JSON không có Player Tag hợp lệ.")
        }

        val timestamp = root.optLong("timestamp", 0L)
        if (timestamp <= 0L) {
            throw IllegalArgumentException("JSON không có timestamp hợp lệ.")
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
                val count = if (obj.has("cnt") && !obj.isNull("cnt")) {
                    obj.optInt("cnt", 1).coerceAtLeast(1)
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
                items += VillageItem(
                    sectionKey = spec.key,
                    sectionLabel = spec.label,
                    tab = spec.tab,
                    base = spec.base,
                    dataId = dataId,
                    level = level,
                    count = count,
                    timerSeconds = timer,
                    gearUp = gearUp,
                    weaponLevel = weapon,
                    details = details
                )
            }
        }

        val extras = linkedMapOf<String, Int>()
        listOf(
            "decos" to "Trang trí Nhà chính",
            "obstacles" to "Chướng ngại Nhà chính",
            "decos2" to "Trang trí Builder Base",
            "obstacles2" to "Chướng ngại Builder Base",
            "house_parts" to "Bộ phận nhà",
            "skins" to "Trang phục",
            "sceneries" to "Cảnh quan",
            "skins2" to "Trang phục Builder Base",
            "sceneries2" to "Cảnh quan Builder Base"
        ).forEach { (key, label) ->
            val array = root.optJSONArray(key)
            if (array != null) {
                val amount = (0 until array.length()).sumOf { index ->
                    val value = array.opt(index)
                    if (value is JSONObject) value.optInt("cnt", 1) else 1
                }
                extras[label] = amount
            }
        }
        val boosts = root.optJSONObject("boosts")
        if (boosts != null && boosts.length() > 0) {
            extras["Thông tin tăng tốc"] = boosts.length()
        }

        return VillageSnapshot(
            tag = tag,
            timestamp = timestamp,
            items = items,
            extras = extras,
            rawJson = raw
        )
    }

    fun compare(previous: VillageSnapshot?, current: VillageSnapshot): List<ChangeEvent> {
        if (previous == null || previous.tag != current.tag) return emptyList()

        val result = mutableListOf<ChangeEvent>()
        val previousRunning = previous.items.filter { it.isRunning }
            .groupingBy { it.identity }.eachCount()
        val currentRunning = current.items.filter { it.isRunning }
            .groupingBy { it.identity }.eachCount()

        current.items.filter { it.isRunning }.forEach { item ->
            val oldCount = previousRunning[item.identity] ?: 0
            val nowCount = currentRunning[item.identity] ?: 0
            if (nowCount > oldCount) {
                result += ChangeEvent(
                    title = "Bắt đầu nâng cấp",
                    detail = item.name + (item.level?.let { " • cấp $it" } ?: "")
                )
            }
        }

        previous.items.filter { it.isRunning }.forEach { oldItem ->
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
        1000000 to "Trại lính", 1000001 to "Nhà chính", 1000002 to "Mỏ Elixir",
        1000003 to "Kho Elixir", 1000004 to "Mỏ vàng", 1000005 to "Kho vàng",
        1000006 to "Nhà lính", 1000007 to "Phòng thí nghiệm", 1000008 to "Đại bác",
        1000009 to "Tháp cung", 1000010 to "Tường", 1000011 to "Tháp pháp sư",
        1000012 to "Phòng không", 1000013 to "Cối", 1000014 to "Thành Clan",
        1000015 to "Nhà thợ xây", 1000019 to "Tesla ẩn", 1000020 to "Nhà phép",
        1000021 to "Nỏ X", 1000023 to "Mỏ hắc tiên", 1000024 to "Kho hắc tiên",
        1000026 to "Nhà lính hắc tiên", 1000027 to "Tháp địa ngục", 1000028 to "Quạt gió",
        1000029 to "Nhà phép hắc tiên", 1000031 to "Pháo đại bàng", 1000032 to "Tháp bom",
        1000059 to "Xưởng công thành", 1000064 to "Nhà B.O.B", 1000067 to "Tháp pháo Scattershot",
        1000068 to "Nhà thú cưng", 1000070 to "Lò rèn", 1000071 to "Sảnh anh hùng",
        1000072 to "Tháp phép", 1000077 to "Đại bác Monolith", 1000079 to "Tháp đa nòng",
        1000084 to "Tháp cung đa mục tiêu", 1000085 to "Đại bác dội", 1000086 to "Tháp phản công",
        1000089 to "Firespitter", 1000093 to "Nhà trợ thủ", 1000097 to "Trạm chế tạo",
        1000102 to "Tháp pháp sư siêu cấp",
        1000033 to "Tường Builder Base", 1000034 to "Nhà thợ xây chính",
        1000035 to "Mỏ Elixir Builder", 1000036 to "Kho Elixir Builder",
        1000037 to "Mỏ vàng Builder", 1000038 to "Kho vàng Builder",
        1000039 to "Tháp đồng hồ", 1000040 to "Nhà lính Builder",
        1000041 to "Đại bác đôi", 1000042 to "Trại lính Builder",
        1000043 to "Tesla ẩn Builder", 1000044 to "Đại bác Builder",
        1000045 to "Cối đa nòng", 1000046 to "Phòng thí nghiệm sao",
        1000047 to "Nhà B.O.T.O", 1000048 to "Tháp cung Builder",
        1000049 to "Trại tiếp viện", 1000050 to "Pháo bông",
        1000051 to "Trạm canh gác", 1000052 to "Tesla khổng lồ",
        1000053 to "Bệ máy chiến đấu", 1000054 to "Bom bay Builder",
        1000055 to "Máy nghiền", 1000056 to "Lò nung",
        1000057 to "Đại bác khổng lồ", 1000058 to "Mỏ ngọc",
        1000063 to "Máy phóng dung nham", 1000065 to "B.O.B Control",
        1000078 to "Tiền đồn O.T.T.O", 1000080 to "Bệ trực thăng chiến đấu",
        1000081 to "Nỏ X Builder", 1000082 to "Nhà hồi máu",
        12000000 to "Bom", 12000001 to "Bẫy lò xo", 12000002 to "Bom khổng lồ",
        12000005 to "Bom bay", 12000006 to "Mìn tìm nhiệt", 12000008 to "Bẫy bộ xương",
        12000010 to "Bẫy lò xo Builder", 12000011 to "Bẫy đẩy Builder",
        12000013 to "Mìn Builder", 12000014 to "Mìn khổng lồ Builder",
        12000016 to "Bẫy lốc xoáy", 12000020 to "Bom Giga",
        28000000 to "Vua Barbarian", 28000001 to "Nữ hoàng cung thủ",
        28000002 to "Warden vĩ đại", 28000003 to "Cỗ máy chiến đấu",
        28000004 to "Nhà vô địch Hoàng gia", 28000005 to "Trực thăng chiến đấu",
        28000006 to "Hoàng tử Minion", 28000007 to "Dragon Duke",
        4000000 to "Barbarian", 4000001 to "Cung thủ", 4000002 to "Goblin",
        4000003 to "Khổng lồ", 4000004 to "Phá tường", 4000005 to "Bóng bay",
        4000006 to "Pháp sư", 4000007 to "Healer", 4000008 to "Rồng",
        4000009 to "P.E.K.K.A", 4000010 to "Minion", 4000011 to "Kỵ sĩ heo",
        4000012 to "Valkyrie", 4000013 to "Golem", 4000015 to "Phù thủy",
        4000017 to "Chó dung nham", 4000022 to "Bowler", 4000023 to "Rồng con",
        4000024 to "Thợ mỏ", 4000051 to "Máy phá tường",
        4000052 to "Khinh khí cầu chiến đấu", 4000053 to "Yeti",
        4000058 to "Golem băng", 4000059 to "Rồng điện", 4000062 to "Máy nghiền đá",
        4000065 to "Kỵ sĩ rồng", 4000082 to "Thợ săn anh hùng",
        4000087 to "Máy phóng khúc gỗ", 4000091 to "Máy phun lửa",
        4000092 to "Máy khoan chiến đấu", 4000095 to "Titan điện",
        4000097 to "Warden tập sự", 4000109 to "Phù thủy tàn tích",
        4000110 to "Kỵ sĩ rễ cây", 4000123 to "Druid", 4000132 to "Thrower",
        4000135 to "Bệ phóng quân", 4000150 to "Lò lửa",
        4000031 to "Barbarian cuồng nộ", 4000032 to "Cung thủ lén lút",
        4000033 to "Minion Beta", 4000034 to "Khổng lồ Boxer",
        4000035 to "Bomber", 4000036 to "P.E.K.K.A quyền năng",
        4000037 to "Xe pháo", 4000038 to "Tàu thả bom",
        4000041 to "Rồng con", 4000042 to "Phù thủy đêm",
        4000070 to "Kỵ sĩ heo bay", 4000106 to "Pháp sư điện lửa",
        26000000 to "Phép sét", 26000001 to "Phép hồi máu",
        26000002 to "Phép cuồng nộ", 26000003 to "Phép nhảy",
        26000005 to "Phép đóng băng", 26000009 to "Phép độc",
        26000010 to "Phép động đất", 26000011 to "Phép tăng tốc",
        26000016 to "Phép nhân bản", 26000017 to "Phép bộ xương",
        26000028 to "Phép dơi", 26000035 to "Phép tàng hình",
        26000053 to "Phép gọi về", 26000070 to "Phép mọc um tùm",
        26000098 to "Phép hồi sinh", 26000109 to "Phép băng",
        26000120 to "Phép vật tổ", 26000123 to "Phép cuồng nộ",
        73000000 to "L.A.S.S.I", 73000001 to "Mighty Yak", 73000002 to "Cú điện",
        73000003 to "Kỳ lân", 73000004 to "Phượng hoàng",
        73000007 to "Thằn lằn độc", 73000008 to "Diggy",
        73000009 to "Frosty", 73000010 to "Spirit Fox",
        73000011 to "Angry Jelly", 73000016 to "Sneezy",
        90000000 to "Búp bê Barbarian", 90000001 to "Lọ cuồng nộ",
        90000002 to "Búp bê cung thủ", 90000003 to "Lọ tàng hình",
        90000004 to "Sách bất tử", 90000005 to "Ngọc cuồng nộ",
        90000006 to "Khiên truy đuổi", 90000007 to "Ngọc Hoàng gia",
        90000008 to "Giày động đất", 90000009 to "Ngọc sinh mệnh",
        90000010 to "Găng tay khổng lồ", 90000011 to "Râu ma cà rồng",
        90000012 to "Búp bê Healer", 90000013 to "Mũi tên băng",
        90000014 to "Cầu lửa", 90000015 to "Gương phép",
        90000016 to "Bóng gai", 90000017 to "Giáo tên lửa",
        93000000 to "Thợ xây tập sự", 93000001 to "Trợ lý phòng thí nghiệm",
        93000002 to "Nhà giả kim",
        103000008 to "Lò nướng Builder", 103000009 to "Bom bay Builder",
        103000010 to "Máy phóng dung nham", 103000011 to "Nến nóng",
        103000012 to "Thợ săn anh hùng", 103000013 to "Máy bắn bánh",
        102000033 to "Máu Nến nóng", 102000034 to "Sát thương Nến nóng",
        102000035 to "Tốc độ Nến nóng", 102000036 to "Máu Thợ săn anh hùng",
        102000037 to "Sát thương Thợ săn anh hùng",
        102000038 to "Tầm bắn Thợ săn anh hùng",
        102000039 to "Máu Máy bắn bánh", 102000040 to "Sát thương Máy bắn bánh",
        102000041 to "Hiệu ứng Máy bắn bánh"
    )

    fun name(id: Int, section: String): String {
        if (id < 0) return "Không xác định"
        return names[id] ?: when {
            section.endsWith("2") -> "Mục Builder Base #$id"
            else -> "Mục #$id"
        }
    }
}
