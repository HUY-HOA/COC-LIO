package com.huyhoa.coclio

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

private const val CLASHKING_API = "https://api.clashk.ing"
private val LookupCyan = Color(0xFF36F4FF)
private val LookupGold = Color(0xFFFFD166)
private val LookupMuted = Color(0xFF9EAFCE)

private data class ClashLookupEntry(
    val name: String,
    val tag: String,
    val isClan: Boolean,
    val townHall: String = "",
    val clanName: String = "",
    val league: String = "",
    val trophies: String = "",
    val members: String = "",
    val level: String = "",
    val rank: String = "",
    val streak: String = "",
    val metric: String = "",
    val metricLabel: String = "",
    val badgeUrl: String = "",
    val raw: JSONObject
)

private data class LookupRequest(val title: String, val path: String, val staticPayload: JSONObject? = null)
private data class LookupPanel(val title: String, val payload: JSONObject?, val error: String? = null)

private data class LookupFilters(
    val clanTags: String = "",
    val leagueIds: String = "",
    val townhallLevels: String = "",
    val locationIds: String = "",
    val warLeagueIds: String = "",
    val clanMinLevel: String = "",
    val clanMaxLevel: String = "",
    val minMembers: String = "",
    val maxMembers: String = ""
)

private object ClashKingLookupApi {
    suspend fun get(path: String): JSONObject = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            connection = (URL(CLASHKING_API + path).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10_000
                readTimeout = 20_000
                instanceFollowRedirects = true
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "COC-LIO-Android")
            }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (status !in 200..299) {
                val detail = runCatching { JSONObject(body).optString("message") }.getOrNull().orEmpty()
                throw IOException(
                    if (detail.isNotBlank()) "HTTP $status • $detail"
                    else "Dịch vụ tra cứu trả về mã HTTP $status."
                )
            }
            if (body.isBlank()) throw IOException("Dịch vụ không trả về dữ liệu.")
            when (val value = JSONTokener(body).nextValue()) {
                is JSONObject -> value
                is JSONArray -> JSONObject().put("items", value)
                JSONObject.NULL, null -> JSONObject().put("message", "Không có dữ liệu được ghi nhận.")
                else -> JSONObject().put("value", value)
            }
        } finally {
            connection?.disconnect()
        }
    }

    private fun encode(value: String): String = URLEncoder.encode(value.trim(), "UTF-8")

    fun searchPath(
        isClan: Boolean,
        query: String,
        cursor: String?,
        filters: LookupFilters
    ): String {
        val params = mutableListOf("query=${encode(query)}", "limit=20")
        cursor?.takeIf { it.isNotBlank() }?.let { params += "cursor=${encode(it)}" }

        fun addList(name: String, raw: String) {
            raw.split(",", ";", " ").map(String::trim).filter(String::isNotBlank)
                .forEach { params += "${name}=${encode(it)}" }
        }
        fun addRange(name: String, min: String, max: String) {
            min.trim().takeIf(String::isNotBlank)?.let { params += "${name}[min]=${encode(it)}" }
            max.trim().takeIf(String::isNotBlank)?.let { params += "${name}[max]=${encode(it)}" }
        }

        if (isClan) {
            addList("locationIds", filters.locationIds)
            addList("warLeagueIds", filters.warLeagueIds)
            addRange("clanLevel", filters.clanMinLevel, filters.clanMaxLevel)
            addRange("members", filters.minMembers, filters.maxMembers)
        } else {
            addList("clanTags", filters.clanTags)
            addList("leagueIds", filters.leagueIds)
            addList("townhallLevels", filters.townhallLevels)
        }
        val route = if (isClan) "/v2/clan/search" else "/v2/player/search"
        return "$route?${params.joinToString("&")}"
    }

    private fun tag(entry: ClashLookupEntry): String = encode(entry.tag.trim().uppercase())

    fun leaderboardPath(category: String, value: String): String = when (category) {
        "townhall" -> "/v2/leaderboard/townhalls/${encode(value)}?limit=100"
        "league" -> "/v2/leaderboard/league/${encode(value)}?limit=100"
        "clan_donations" -> "/v2/leaderboard/${encode(value)}/clan/donations?limit=100"
        "clan_war_wins" -> "/v2/leaderboard/${encode(value)}/clan/war-wins?limit=100"
        "clan_capital_gold" -> "/v2/leaderboard/${encode(value)}/clan/capital-gold?limit=100"
        else -> "/v2/leaderboard/clan/win-streak?limit=100"
    }

    fun detailRequests(entry: ClashLookupEntry, section: String): List<LookupRequest> {
        val tag = tag(entry)
        fun req(title: String, route: String) = LookupRequest(title, route)
        fun data(title: String, payload: JSONObject) = LookupRequest(title, "", payload)
        fun player(route: String, query: String = "") = "/v2/player/$tag/$route" + query
        fun clan(route: String, query: String = "") = "/v2/clan/$tag/$route" + query
        val limit = "?limit=50"

        return if (!entry.isClan) {
            when (section) {
                "TỔNG QUAN" -> listOf(
                    data("Hồ sơ người chơi và Nhà chính", entry.raw),
                    req("Xếp hạng hiện tại", player("rankings"))
                )
                "QUÂN ĐỘI" -> listOf(
                    req("Lịch sử nâng cấp quân lính", player("history/changes", "?type=troop_level&limit=50")),
                    req("Lịch sử anh hùng", player("history/changes", "?type=hero_level&limit=50")),
                    req("Lịch sử phép", player("history/changes", "?type=spell_level&limit=50")),
                    req("Lịch sử thú cưng", player("history/changes", "?type=pet_level&limit=50")),
                    req("Lịch sử trang bị anh hùng", player("history/changes", "?type=equipment_level&limit=50"))
                )
                "HOẠT ĐỘNG" -> listOf(
                    req("Hoạt động 30 ngày", player("activity-summary")),
                    req("Tổng tài nguyên cướp được", player("farming")),
                    req("Lịch sử quyên góp", player("history/stats", "?type=donated&limit=50")),
                    req("Lịch sử nhận quân", player("history/stats", "?type=received&limit=50")),
                    req("Điểm Clan Games", player("history/stats", "?type=clan_games&limit=50")),
                    req("Vàng Thủ phủ đã đóng góp", player("history/stats", "?type=capital_gold_donated&limit=50"))
                )
                "TRẬN ĐẤU" -> listOf(
                    req("Lịch sử tấn công và phòng thủ", player("battlelog/history"))
                )
                "HỘI CHIẾN" -> listOf(
                    req("Thống kê hội chiến", player("war/stats", limit)),
                    req("Chi tiết đòn đánh và phòng thủ", player("war/attacks", limit))
                )
                "CWL" -> listOf(
                    req("Lịch sử Clan War League", player("cwl/history", "?limit=30"))
                )
                "HUYỀN THOẠI" -> listOf(
                    req("Các mùa Legend đã ghi nhận", player("legend-history")),
                    req("Biểu đồ cúp theo ngày", player("legend/series")),
                    req("Thống kê mùa Legend hiện tại", player("legend/season")),
                    req("So sánh hiệu suất Legend", player("legend/comparisons"))
                )
                "XẾP HẠNG" -> listOf(
                    req("Xếp hạng toàn cầu và địa phương", player("rankings")),
                    req("Lịch sử xếp hạng Nhà chính", player("leaderboard-history/player_home_trophies")),
                    req("Lịch sử xếp hạng Nhà thợ xây", player("leaderboard-history/player_builder_base_trophies")),
                    req("Lịch sử Ranked", player("ranked/history"))
                )
                "BANG HỘI" -> listOf(
                    req("Lịch sử tham gia bang hội", player("join-leave", limit)),
                    req("Tổng thời gian ở từng bang hội", player("join-leave/totals"))
                )
                "THỜI GIAN" -> listOf(
                    req("Các mốc hội chiến, CWL và Thủ phủ", player("timers"))
                )
                else -> emptyList()
            }
        } else {
            when (section) {
                "TỔNG QUAN" -> listOf(
                    req("Hồ sơ bang hội và danh sách thành viên", clan("cached")),
                    req("Xếp hạng và điểm bang hội", clan("rankings"))
                )
                "THÀNH VIÊN" -> listOf(
                    req("Thành viên hiện tại và ngày tham gia được ghi nhận", "/v2/clan/$tag/member-joins"),
                    req("Lịch sử thành viên vào và rời bang", clan("join-leave", limit))
                )
                "HỘI CHIẾN" -> listOf(
                    req("Các cuộc hội chiến đã lưu", clan("wars", limit)),
                    req("Trạng thái cuộc hội chiến hiện tại", "/v2/war/$tag/basic")
                )
                "CWL" -> listOf(
                    req("Các mùa CWL", "/v2/cwl/$tag/seasons?limit=30"),
                    req("Lịch sử thứ hạng CWL", "/v2/cwl/$tag/ranking-history"),
                    req("Nhóm CWL hiện tại", "/v2/cwl/$tag/group")
                )
                "QUYÊN GÓP" -> listOf(
                    req("Tổng quyên góp trong tháng", clan("donations/summary")),
                    req("Thứ hạng quyên góp và nhận quân", clan("rankings"))
                )
                "THỦ PHỦ" -> listOf(
                    req("Cấp Thủ phủ, giải đấu và tổng Vàng Thủ phủ", clan("cached")),
                    req("Thứ hạng Thủ phủ và đóng góp", clan("rankings"))
                )
                "XẾP HẠNG" -> listOf(
                    req("Xếp hạng hiện tại", clan("rankings")),
                    req("Lịch sử xếp hạng theo ngày", clan("history/leaderboards")),
                    req("Tổng kết các mùa trong bảng xếp hạng", clan("history/leaderboards/summary", "?type=clan_home_points"))
                )
                "HUYỀN THOẠI" -> listOf(
                    req("Lịch sử người chơi Legend trong bang", clan("history/legends", limit)),
                    req("Tổng kết Legend và thành tích nổi bật", clan("history/legends/summary", "?top=20"))
                )
                "KỶ LỤC" -> listOf(
                    req("Điểm clan và chuỗi thắng kỷ lục", clan("records"))
                )
                "LỊCH SỬ" -> listOf(
                    req("Lịch sử thay đổi cấp và mô tả bang", clan("history/changes", limit)),
                    req("Lịch sử thành viên", clan("join-leave", limit))
                )
                else -> emptyList()
            }
        }
    }
}

private fun JSONObject.lookupText(key: String): String {
    val value = opt(key)
    return when {
        value == null || value == JSONObject.NULL -> ""
        value is JSONObject || value is JSONArray -> ""
        else -> value.toString().takeIf { it.isNotBlank() && it != "null" }.orEmpty()
    }
}

private fun lookupEntry(json: JSONObject, isClan: Boolean): ClashLookupEntry {
    val clan = json.optJSONObject("clan")
    val league = json.optJSONObject("leagueTier") ?: json.optJSONObject("league")
    val badgeUrls = json.optJSONObject("badgeUrls") ?: clan?.optJSONObject("badgeUrls")
    val badge = json.lookupText("badge")
        .ifBlank { clan?.lookupText("badge").orEmpty() }
        .ifBlank { badgeUrls?.lookupText("medium").orEmpty() }
        .ifBlank { badgeUrls?.lookupText("large").orEmpty() }

    fun number(vararg keys: String): String {
        for (key in keys) {
            val value = json.lookupText(key)
            if (value.isNotBlank() && value != "-1") return value
        }
        return ""
    }

    return ClashLookupEntry(
        name = json.lookupText("name").ifBlank {
            if (isClan) "Bang hội chưa rõ tên" else "Người chơi chưa rõ tên"
        },
        tag = json.lookupText("tag").ifBlank { json.lookupText("playerTag") },
        isClan = isClan,
        townHall = number("townHallLevel", "townhallLevel", "townhall_level"),
        clanName = clan?.lookupText("name").orEmpty(),
        league = league?.lookupText("name").orEmpty(),
        trophies = number("trophies", "leagueTrophies"),
        members = number("members", "memberCount"),
        level = number("clanLevel", "level"),
        rank = number("rank"),
        streak = number("war_win_streak", "warWinStreak"),
        metric = number("donations", "war_wins", "capital_gold_total"),
        metricLabel = when {
            json.has("donations") -> "Quyên góp"
            json.has("war_wins") -> "Thắng hội chiến"
            json.has("capital_gold_total") -> "Vàng Thủ phủ"
            else -> ""
        },
        badgeUrl = json.lookupText("badge_url").ifBlank { badge },
        raw = json
    )
}

private fun lookupItems(response: JSONObject, isClan: Boolean): List<ClashLookupEntry> {
    val items = response.optJSONArray("items")
        ?: response.optJSONArray("results")
        ?: response.optJSONArray("data")
        ?: JSONArray()
    return buildList {
        for (index in 0 until items.length()) {
            val item = items.optJSONObject(index) ?: continue
            add(lookupEntry(item, isClan))
        }
    }
}

private fun humanizeLookupKey(key: String): String {
    val known = mapOf(
        "tag" to "TAG / ID",
        "name" to "Tên",
        "townHallLevel" to "Nhà chính",
        "townhallLevel" to "Nhà chính",
        "builderHallLevel" to "Nhà thợ xây",
        "leagueTier" to "Giải đấu",
        "league" to "Giải đấu",
        "trophies" to "Cúp",
        "leagueTrophies" to "Cúp giải đấu",
        "clanLevel" to "Cấp bang hội",
        "memberCount" to "Thành viên",
        "members" to "Thành viên",
        "warWins" to "Thắng hội chiến",
        "warLosses" to "Thua hội chiến",
        "warTies" to "Hòa hội chiến",
        "warWinStreak" to "Chuỗi thắng",
        "donated" to "Đã quyên góp",
        "received" to "Đã nhận",
        "troopsDonated" to "Quân đã quyên góp",
        "troopsReceived" to "Quân đã nhận",
        "capitalGoldTotal" to "Tổng Vàng Thủ phủ",
        "capitalPoints" to "Điểm Thủ phủ",
        "capitalLeague" to "Giải Thủ phủ",
        "warLeague" to "Giải hội chiến",
        "attackWins" to "Thắng tấn công",
        "defenseWins" to "Thắng phòng thủ",
        "stars" to "Sao",
        "destructionPercentage" to "Phá hủy",
        "globalRank" to "Xếp hạng toàn cầu",
        "localRank" to "Xếp hạng địa phương",
        "time" to "Thời gian",
        "type" to "Loại sự kiện",
        "delta" to "Thay đổi",
        "lastActive" to "Hoạt động được ghi nhận gần nhất",
        "lastObservedAt" to "Lần cuối được ghi nhận",
        "joinedAt" to "Ngày tham gia được ghi nhận",
        "battleTime" to "Thời gian trận đấu",
        "attackTrophies" to "Cúp tấn công",
        "defenseTrophies" to "Cúp phòng thủ",
        "totalLoot" to "Tổng tài nguyên",
        "townhall_level" to "Nhà chính",
        "previous_value" to "Cấp trước",
        "current_value" to "Cấp hiện tại",
        "eventTime" to "Thời gian sự kiện",
        "statType" to "Loại thống kê",
        "preparationStartTime" to "Bắt đầu chuẩn bị",
        "startTime" to "Bắt đầu hội chiến",
        "endTime" to "Kết thúc",
        "teamSize" to "Quy mô đội",
        "rank" to "Thứ hạng",
        "season" to "Mùa giải",
        "attackTriples" to "Số trận 3 sao",
        "defenseTriples" to "Phòng thủ 3 sao",
        "averageOffense" to "Phá hủy tấn công trung bình",
        "averageDefense" to "Phá hủy phòng thủ trung bình",
        "clanPoints" to "Điểm bang hội",
        "builderBasePoints" to "Điểm Nhà thợ xây",
        "capitalGold" to "Vàng Thủ phủ",
        "clanCapital" to "Thủ phủ bang hội"
    )
    return known[key] ?: key
        .replace(Regex("([a-z])([A-Z])"), "$1 $2")
        .replace('_', ' ')
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
}

private fun lookupValue(value: Any?): String = when (value) {
    null, JSONObject.NULL -> "—"
    is JSONObject -> ""
    is JSONArray -> "${value.length()} mục"
    is Boolean -> if (value) "Có" else "Không"
    else -> value.toString().take(500)
}

@Composable
fun ClashLookupScreen() {
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    var searchMode by rememberSaveable { mutableStateOf("player") }
    var rankingCategory by rememberSaveable { mutableStateOf("townhall") }
    var rankingValue by rememberSaveable { mutableStateOf("19") }
    var query by rememberSaveable { mutableStateOf("") }
    var showAdvancedFilters by rememberSaveable { mutableStateOf(false) }
    var clanTags by rememberSaveable { mutableStateOf("") }
    var leagueIds by rememberSaveable { mutableStateOf("") }
    var townhallLevels by rememberSaveable { mutableStateOf("") }
    var locationIds by rememberSaveable { mutableStateOf("") }
    var warLeagueIds by rememberSaveable { mutableStateOf("") }
    var clanMinLevel by rememberSaveable { mutableStateOf("") }
    var clanMaxLevel by rememberSaveable { mutableStateOf("") }
    var minMembers by rememberSaveable { mutableStateOf("") }
    var maxMembers by rememberSaveable { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf<String?>(null) }
    var results by remember { mutableStateOf(emptyList<ClashLookupEntry>()) }
    var nextCursor by remember { mutableStateOf<String?>(null) }
    var selectedEntry by remember { mutableStateOf<ClashLookupEntry?>(null) }
    var selectedSection by rememberSaveable { mutableStateOf("TỔNG QUAN") }
    var detailPanels by remember { mutableStateOf(emptyList<LookupPanel>()) }
    var detailLoading by remember { mutableStateOf(false) }
    var detailError by remember { mutableStateOf<String?>(null) }

    val filters = LookupFilters(
        clanTags = clanTags,
        leagueIds = leagueIds,
        townhallLevels = townhallLevels,
        locationIds = locationIds,
        warLeagueIds = warLeagueIds,
        clanMinLevel = clanMinLevel,
        clanMaxLevel = clanMaxLevel,
        minMembers = minMembers,
        maxMembers = maxMembers
    )

    fun runSearch(loadMore: Boolean = false) {
        val term = query.trim()
        val isLeaderboard = searchMode == "leaderboard"
        if (!isLeaderboard && term.length < 2) {
            searchError = "Nhập ít nhất 2 ký tự tên hoặc TAG/ID cần tìm."
            return
        }
        if (isLeaderboard && rankingCategory != "clan_win_streak" && rankingValue.toIntOrNull() == null) {
            searchError = "Nhập cấp Nhà chính hoặc ID giải đấu hợp lệ."
            return
        }
        if (isSearching) return
        val oldCursor = if (loadMore) nextCursor else null
        val selectedFilters = filters
        val isClan = searchMode == "clan" || (isLeaderboard && rankingCategory == "clan_win_streak")
        scope.launch {
            isSearching = true
            searchError = null
            if (!loadMore) {
                results = emptyList()
                nextCursor = null
            }
            try {
                val response = if (isLeaderboard) {
                    ClashKingLookupApi.get(ClashKingLookupApi.leaderboardPath(rankingCategory, rankingValue))
                } else {
                    ClashKingLookupApi.get(
                        ClashKingLookupApi.searchPath(isClan, term, oldCursor, selectedFilters)
                    )
                }
                val found = lookupItems(response, isClan)
                results = if (loadMore) results + found else found
                nextCursor = if (isLeaderboard) null else response.optJSONObject("pagination")
                    ?.lookupText("nextCursor")
                    ?.takeIf { it.isNotBlank() }
                if (results.isEmpty()) {
                    searchError = "Không tìm thấy kết quả. Hãy kiểm tra tên, TAG/ID hoặc bộ lọc."
                }
            } catch (error: Exception) {
                searchError = error.message ?: "Không thể kết nối dịch vụ tra cứu."
            } finally {
                isSearching = false
            }
        }
    }

    val activeEntry = selectedEntry
    LaunchedEffect(activeEntry?.tag, activeEntry?.isClan, selectedSection) {
        val entry = activeEntry ?: return@LaunchedEffect
        val requests = ClashKingLookupApi.detailRequests(entry, selectedSection)
        if (requests.isEmpty()) {
            detailPanels = emptyList()
            detailError = "Mục dữ liệu này chưa được API công khai hỗ trợ."
            detailLoading = false
            return@LaunchedEffect
        }
        detailLoading = true
        detailError = null
        detailPanels = emptyList()
        val loaded = requests.map { request ->
            async {
                if (request.staticPayload != null) {
                    LookupPanel(request.title, request.staticPayload)
                } else runCatching {
                    LookupPanel(request.title, ClashKingLookupApi.get(request.path))
                }.getOrElse { error ->
                    LookupPanel(request.title, null, error.message ?: "Không thể tải dữ liệu.")
                }
            }
        }.awaitAll()
        detailPanels = loaded
        val failures = loaded.filter { it.payload == null }
        detailError = when {
            failures.size == loaded.size -> failures.joinToString("\n") { "• ${it.title}: ${it.error}" }
            failures.isNotEmpty() -> failures.joinToString("\n") { "• ${it.title}: ${it.error}" }
            else -> null
        }
        detailLoading = false
    }

    val entry = selectedEntry
    if (entry != null) {
        val sections = if (entry.isClan) {
            listOf("TỔNG QUAN", "THÀNH VIÊN", "HỘI CHIẾN", "CWL", "QUYÊN GÓP", "THỦ PHỦ", "XẾP HẠNG", "HUYỀN THOẠI", "KỶ LỤC", "LỊCH SỬ")
        } else {
            listOf("TỔNG QUAN", "QUÂN ĐỘI", "HOẠT ĐỘNG", "TRẬN ĐẤU", "HỘI CHIẾN", "CWL", "HUYỀN THOẠI", "XẾP HẠNG", "BANG HỘI", "THỜI GIAN")
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = {
                        selectedEntry = null
                        detailPanels = emptyList()
                        detailError = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Outlined.ArrowBack, contentDescription = "Quay lại", modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("KẾT QUẢ TÌM KIẾM", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = { clipboard.setText(AnnotatedString(entry.tag)) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = LookupCyan
                    ),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Outlined.ContentCopy, contentDescription = "Sao chép TAG", modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("SAO CHÉP ID", fontSize = 9.sp)
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)),
                border = BorderStroke(1.dp, LookupCyan.copy(alpha = 0.45f)),
                shape = RoundedCornerShape(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(15.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (entry.badgeUrl.isNotBlank()) {
                        AsyncImage(
                            model = entry.badgeUrl,
                            contentDescription = "Huy hiệu bang hội",
                            modifier = Modifier.size(62.dp)
                        )
                    } else {
                        Box(
                            modifier = Modifier.size(62.dp).clip(RoundedCornerShape(16.dp))
                                .background(LookupCyan.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                if (entry.isClan) Icons.Outlined.Groups else Icons.Outlined.Person,
                                contentDescription = null,
                                tint = LookupCyan,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(13.dp))
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(entry.name, fontSize = 18.sp, fontWeight = FontWeight.Black, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(entry.tag.ifBlank { "Chưa có TAG" }, fontSize = 12.sp, color = LookupCyan, fontWeight = FontWeight.Bold)
                        Text(
                            if (entry.isClan) "HỒ SƠ BANG HỘI" else "HỒ SƠ NGƯỜI CHƠI",
                            fontSize = 9.sp,
                            color = LookupMuted,
                            letterSpacing = 0.8.sp
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                sections.forEach { section ->
                    FilterChip(
                        selected = selectedSection == section,
                        onClick = { selectedSection = section },
                        label = { Text(section, fontSize = 9.sp, maxLines = 1) }
                    )
                }
            }

            if (detailLoading) {
                Box(Modifier.fillMaxWidth().padding(28.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = LookupCyan)
                }
            }
            if (!detailError.isNullOrBlank() && detailPanels.any { it.payload != null }) {
                LookupMessageCard("Một số nguồn dữ liệu chưa trả về kết quả:\n${detailError}", isError = false)
            }
            if (detailError != null && detailPanels.none { it.payload != null } && !detailLoading) {
                LookupMessageCard(detailError ?: "Không tải được dữ liệu", isError = true)
            }
            detailPanels.forEach { panel ->
                if (panel.payload != null) {
                    LookupDataPanel(title = panel.title, payload = panel.payload)
                } else if (!detailLoading) {
                    LookupMessageCard("${panel.title}: ${panel.error ?: "Chưa có dữ liệu"}")
                }
            }
            Text(
                "Dữ liệu công khai từ ClashKing API. Hoạt động, lịch sử và thống kê chỉ phản ánh những dữ liệu API đã ghi nhận; không phải lúc nào cũng có đủ.",
                color = LookupMuted,
                fontSize = 9.sp,
                lineHeight = 13.sp
            )
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text("TRA CỨU CLASH", fontSize = 19.sp, fontWeight = FontWeight.Black, color = LookupCyan, letterSpacing = 1.sp)
            Text("Tra cứu người chơi, bang hội và hồ sơ thống kê Clash of Clans", fontSize = 10.sp, color = LookupMuted)
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)),
            border = BorderStroke(1.dp, LookupCyan.copy(alpha = 0.35f)),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(11.dp)
            ) {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = searchMode == "player",
                        onClick = {
                            searchMode = "player"
                            results = emptyList()
                            searchError = null
                        },
                        label = { Text("NGƯỜI CHƠI", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                        leadingIcon = { Icon(Icons.Outlined.Person, null, modifier = Modifier.size(16.dp)) }
                    )
                    FilterChip(
                        selected = searchMode == "clan",
                        onClick = {
                            searchMode = "clan"
                            results = emptyList()
                            searchError = null
                        },
                        label = { Text("BANG HỘI", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                        leadingIcon = { Icon(Icons.Outlined.Groups, null, modifier = Modifier.size(16.dp)) }
                    )
                    FilterChip(
                        selected = searchMode == "leaderboard",
                        onClick = {
                            searchMode = "leaderboard"
                            results = emptyList()
                            nextCursor = null
                            searchError = null
                        },
                        label = { Text("XẾP HẠNG", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                    )
                }

                if (searchMode != "leaderboard") OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(if (searchMode == "player") "Tên người chơi hoặc #TAG" else "Tên bang hội hoặc #TAG") },
                    placeholder = { Text(if (searchMode == "player") "Nhập tên hoặc #ABC123" else "Nhập tên clan hoặc #XYZ789") },
                    leadingIcon = { Icon(Icons.Outlined.Search, null, tint = LookupCyan) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LookupCyan,
                        focusedLabelColor = LookupCyan,
                        cursorColor = LookupCyan
                    )
                )

                if (searchMode == "leaderboard") {
                    Text("CHỌN BẢNG XẾP HẠNG", color = LookupGold, fontSize = 10.sp, fontWeight = FontWeight.Black)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = rankingCategory == "townhall",
                            onClick = { rankingCategory = "townhall"; rankingValue = "19" },
                            label = { Text("NHÀ CHÍNH", fontSize = 9.sp) }
                        )
                        FilterChip(
                            selected = rankingCategory == "league",
                            onClick = { rankingCategory = "league"; rankingValue = "29000000" },
                            label = { Text("GIẢI ĐẤU", fontSize = 9.sp) }
                        )
                        FilterChip(
                            selected = rankingCategory == "clan_donations",
                            onClick = { rankingCategory = "clan_donations"; rankingValue = "32000006" },
                            label = { Text("CLAN QUYÊN GÓP", fontSize = 9.sp) }
                        )
                        FilterChip(
                            selected = rankingCategory == "clan_war_wins",
                            onClick = { rankingCategory = "clan_war_wins"; rankingValue = "32000006" },
                            label = { Text("CLAN THẮNG HỘI CHIẾN", fontSize = 9.sp) }
                        )
                        FilterChip(
                            selected = rankingCategory == "clan_capital_gold",
                            onClick = { rankingCategory = "clan_capital_gold"; rankingValue = "32000006" },
                            label = { Text("VÀNG THỦ PHỦ", fontSize = 9.sp) }
                        )
                        FilterChip(
                            selected = rankingCategory == "clan_win_streak",
                            onClick = { rankingCategory = "clan_win_streak" },
                            label = { Text("CLAN CHUỖI THẮNG", fontSize = 9.sp) }
                        )
                    }
                    if (rankingCategory != "clan_win_streak") {
                        val isClanLeaderboard = rankingCategory.startsWith("clan_")
                        val label = when {
                            rankingCategory == "townhall" -> "Cấp Nhà chính"
                            rankingCategory == "league" -> "ID giải đấu"
                            else -> "ID quốc gia / khu vực"
                        }
                        LookupFilterField(
                            label,
                            rankingValue,
                            { rankingValue = it },
                            when {
                                rankingCategory == "townhall" -> "19"
                                rankingCategory == "league" -> "29000000"
                                else -> "32000006"
                            }
                        )
                        Text(
                            if (isClanLeaderboard) "Các bảng xếp hạng clan dùng ID khu vực; 32000006 là khu vực International."
                            else "Nhập đúng ID Nhà chính hoặc ID giải đấu theo dữ liệu ClashKing.",
                            color = LookupMuted,
                            fontSize = 9.sp,
                            lineHeight = 13.sp
                        )
                    } else {
                        Text("Bảng xếp hạng clan theo chuỗi thắng hiện tại.", color = LookupMuted, fontSize = 9.sp)
                    }
                }

                if (searchMode != "leaderboard") Button(
                    onClick = { showAdvancedFilters = !showAdvancedFilters },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = LookupCyan
                    )
                ) {
                    Text(if (showAdvancedFilters) "ẨN BỘ LỌC NÂNG CAO" else "BỘ LỌC NÂNG CAO", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }

                if (showAdvancedFilters && searchMode != "leaderboard") {
                    if (searchMode == "player") {
                        LookupFilterField("TAG bang hội (phân cách bằng dấu phẩy)", clanTags, { clanTags = it }, "#ABC123")
                        LookupFilterField("ID giải đấu (phân cách bằng dấu phẩy)", leagueIds, { leagueIds = it }, "29000000")
                        LookupFilterField("Cấp Nhà chính (phân cách bằng dấu phẩy)", townhallLevels, { townhallLevels = it }, "17,18,19")
                    } else {
                        LookupFilterField("ID quốc gia / khu vực", locationIds, { locationIds = it }, "32000006")
                        LookupFilterField("ID giải hội chiến", warLeagueIds, { warLeagueIds = it }, "48000000")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Column(Modifier.weight(1f)) {
                                LookupFilterField("Cấp clan từ", clanMinLevel, { clanMinLevel = it }, "1")
                            }
                            Column(Modifier.weight(1f)) {
                                LookupFilterField("Đến cấp", clanMaxLevel, { clanMaxLevel = it }, "30")
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Column(Modifier.weight(1f)) {
                                LookupFilterField("Ít nhất thành viên", minMembers, { minMembers = it }, "1")
                            }
                            Column(Modifier.weight(1f)) {
                                LookupFilterField("Nhiều nhất", maxMembers, { maxMembers = it }, "50")
                            }
                        }
                    }
                    Text(
                        "ID bộ lọc phải là ID dữ liệu game do ClashKing API sử dụng. Để trống bộ lọc không cần dùng.",
                        fontSize = 9.sp,
                        color = LookupMuted,
                        lineHeight = 13.sp
                    )
                }

                Button(
                    onClick = { runSearch(false) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSearching,
                    colors = ButtonDefaults.buttonColors(containerColor = LookupCyan, contentColor = Color(0xFF06111B)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isSearching) {
                        CircularProgressIndicator(modifier = Modifier.size(17.dp), strokeWidth = 2.dp, color = Color(0xFF06111B))
                        Spacer(Modifier.width(8.dp))
                        Text("ĐANG TÌM...", fontWeight = FontWeight.Black)
                    } else {
                        Icon(Icons.Outlined.Search, null, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(7.dp))
                        Text(if (searchMode == "leaderboard") "XEM XẾP HẠNG" else "TÌM KIẾM", fontWeight = FontWeight.Black, letterSpacing = 0.5.sp)
                    }
                }
            }
        }

        if (searchError != null) {
            LookupMessageCard(searchError ?: "", isError = true)
        }

        if (results.isNotEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("KẾT QUẢ", fontSize = 11.sp, color = LookupGold, fontWeight = FontWeight.Black)
                Spacer(Modifier.weight(1f))
                Text("${results.size} kết quả", fontSize = 9.sp, color = LookupMuted)
            }
            results.forEach { result ->
                LookupResultCard(
                    entry = result,
                    onClick = {
                        selectedEntry = result
                        selectedSection = "TỔNG QUAN"
                        detailPanels = emptyList()
                        detailError = null
                    },
                    onCopy = { clipboard.setText(AnnotatedString(result.tag)) }
                )
            }
            if (!nextCursor.isNullOrBlank()) {
                Button(
                    onClick = { runSearch(true) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSearching,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = LookupCyan
                    )
                ) {
                    Text(if (isSearching) "ĐANG TẢI..." else "TẢI THÊM KẾT QUẢ", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        } else if (searchError == null && !isSearching) {
            LookupMessageCard(
                when (searchMode) {
                    "player" -> "Tìm người chơi bằng tên hoặc TAG. Chọn kết quả để mở hồ sơ, hoạt động, quân đội, hội chiến, CWL, Legend, xếp hạng và lịch sử."
                    "clan" -> "Tìm bang hội bằng tên hoặc Clan TAG. Hồ sơ có thành viên, hội chiến, CWL, quyên góp, Thủ phủ, xếp hạng, kỷ lục và lịch sử."
                    else -> "Tra cứu bảng xếp hạng theo Nhà chính, giải đấu hoặc chuỗi thắng của clan."
                }
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            Icon(Icons.Outlined.Security, contentDescription = null, tint = LookupMuted, modifier = Modifier.size(14.dp))
            Text(
                "Tra cứu dữ liệu game công khai • Không yêu cầu mật khẩu tài khoản Clash of Clans",
                fontSize = 9.sp,
                lineHeight = 13.sp,
                color = LookupMuted
            )
        }
        Text(
            "Nguồn dữ liệu: ClashKing API • Creator Code: ClashKing. COC-LIO là công cụ cộng đồng độc lập, không phải ứng dụng chính thức của Supercell.",
            fontSize = 9.sp,
            color = LookupMuted,
            lineHeight = 13.sp
        )
    }
}

@Composable
private fun LookupFilterField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        label = { Text(label, fontSize = 9.sp) },
        placeholder = { Text(placeholder, fontSize = 10.sp) },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = LookupCyan,
            focusedLabelColor = LookupCyan,
            cursorColor = LookupCyan
        )
    )
}

@Composable
private fun LookupResultCard(
    entry: ClashLookupEntry,
    onClick: () -> Unit,
    onCopy: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.93f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(15.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (entry.badgeUrl.isNotBlank()) {
                AsyncImage(model = entry.badgeUrl, contentDescription = "Huy hiệu clan", modifier = Modifier.size(48.dp))
            } else {
                Box(
                    modifier = Modifier.size(48.dp).clip(RoundedCornerShape(13.dp))
                        .background(LookupCyan.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (entry.isClan) entry.level.ifBlank { "CLAN" } else "TH ${entry.townHall.ifBlank { "?" }}",
                        color = LookupCyan,
                        fontSize = if (entry.isClan) 10.sp else 9.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(entry.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(entry.tag.ifBlank { "Chưa có TAG" }, fontSize = 10.sp, color = LookupCyan, fontWeight = FontWeight.Bold)
                val subtitle = if (entry.isClan) {
                    listOfNotNull(
                        entry.rank.takeIf { it.isNotBlank() }?.let { "Hạng #$it" },
                        entry.level.takeIf { it.isNotBlank() }?.let { "Cấp $it" },
                        entry.members.takeIf { it.isNotBlank() }?.let { "$it thành viên" },
                        entry.streak.takeIf { it.isNotBlank() }?.let { "Chuỗi thắng $it" },
                        entry.metric.takeIf { it.isNotBlank() }?.let { "${entry.metricLabel}: $it" }
                    ).joinToString(" • ")
                } else {
                    listOfNotNull(
                        entry.rank.takeIf { it.isNotBlank() }?.let { "Hạng #$it" },
                        entry.townHall.takeIf { it.isNotBlank() }?.let { "Nhà chính $it" },
                        entry.league.takeIf { it.isNotBlank() },
                        entry.clanName.takeIf { it.isNotBlank() }
                    ).joinToString(" • ")
                }
                Text(subtitle.ifBlank { if (entry.isClan) "Bang hội" else "Người chơi" }, fontSize = 9.sp, color = LookupMuted, maxLines = 2)
            }
            Button(
                onClick = onCopy,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = LookupCyan
                ),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 7.dp)
            ) {
                Icon(Icons.Outlined.ContentCopy, contentDescription = "Sao chép TAG", modifier = Modifier.size(15.dp))
            }
        }
    }
}

@Composable
private fun LookupDataPanel(title: String, payload: JSONObject) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(13.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Text(
                title.uppercase(),
                fontSize = 10.sp,
                color = LookupCyan,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.7.sp
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
            LookupJsonObject(payload, depth = 0)
        }
    }
}

@Composable
private fun LookupJsonObject(payload: JSONObject, depth: Int) {
    if (depth > 5) {
        Text("Dữ liệu lồng sâu hơn không được mở rộng ở cấp này.", fontSize = 9.sp, color = LookupMuted)
        return
    }
    val keys = remember(payload) { payload.keys().asSequence().toList() }
    keys.forEach { key ->
        val value = payload.opt(key)
        when (value) {
            is JSONObject -> {
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(
                        humanizeLookupKey(key),
                        color = if (depth == 0) LookupGold else LookupCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.36f)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(Modifier.fillMaxWidth().padding(9.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            LookupJsonObject(value, depth + 1)
                        }
                    }
                }
            }
            is JSONArray -> LookupJsonArray(humanizeLookupKey(key), value, depth + 1)
            else -> {
                val display = lookupValue(value)
                if (display.isNotBlank()) LookupDetailRow(humanizeLookupKey(key), display)
            }
        }
    }
}

@Composable
private fun LookupJsonArray(label: String, values: JSONArray, depth: Int) {
    var visibleCount by remember(values) { mutableIntStateOf(20) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = if (depth <= 1) LookupGold else LookupCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Text("${values.length()} mục", color = LookupMuted, fontSize = 9.sp)
        }
        if (values.length() == 0) {
            Text("Không có dữ liệu được ghi nhận.", color = LookupMuted, fontSize = 9.sp)
        } else {
            val end = minOf(values.length(), visibleCount)
            for (index in 0 until end) {
                when (val value = values.opt(index)) {
                    is JSONObject -> {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(
                                Modifier.fillMaxWidth().padding(9.dp),
                                verticalArrangement = Arrangement.spacedBy(7.dp)
                            ) {
                                Text("#${index + 1}", fontSize = 9.sp, color = LookupGold, fontWeight = FontWeight.Bold)
                                LookupJsonObject(value, depth + 1)
                            }
                        }
                    }
                    is JSONArray -> LookupJsonArray("#${index + 1}", value, depth + 1)
                    else -> LookupDetailRow("#${index + 1}", lookupValue(value))
                }
            }
            if (end < values.length()) {
                Button(
                    onClick = { visibleCount = (visibleCount + 20).coerceAtMost(values.length()) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = LookupCyan
                    )
                ) {
                    Text("HIỂN THỊ TIẾP ${minOf(20, values.length() - end)} / ${values.length() - end}", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun LookupDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(label, modifier = Modifier.weight(0.9f), color = LookupMuted, fontSize = 10.sp, lineHeight = 14.sp)
        Text(
            value,
            modifier = Modifier.weight(1.1f),
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 10.sp,
            lineHeight = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun LookupMessageCard(message: String, isError: Boolean = false) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f)),
        border = BorderStroke(1.dp, (if (isError) Color(0xFFFF6D85) else LookupCyan).copy(alpha = 0.25f)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Text(
            message,
            modifier = Modifier.fillMaxWidth().padding(13.dp),
            fontSize = 10.sp,
            lineHeight = 15.sp,
            color = if (isError) Color(0xFFFF9CAB) else LookupMuted
        )
    }
}
