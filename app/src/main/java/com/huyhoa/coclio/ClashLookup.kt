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

private data class LookupRequest(
    val title: String,
    val path: String,
    val staticPayload: JSONObject? = null,
    val includeFields: Set<String>? = null
)
private data class LookupPanel(val title: String, val payload: JSONObject?, val error: String? = null)

private data class LookupFilters(
    val clanTags: String = "",
    val leagueIds: String = "",
    val townhallLevels: String = "",
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

    fun playerSearchPath(query: String, cursor: String?, filters: LookupFilters): String {
        val params = mutableListOf("query=${encode(query)}", "limit=20")
        cursor?.takeIf { it.isNotBlank() }?.let { params += "cursor=${encode(it)}" }
        fun addList(name: String, raw: String) {
            raw.split(",", ";", " ").map(String::trim).filter(String::isNotBlank)
                .forEach { params += "${name}=${encode(it)}" }
        }
        addList("clanTags", filters.clanTags)
        addList("leagueIds", filters.leagueIds)
        addList("townhallLevels", filters.townhallLevels)
        return "/v2/player/search?${params.joinToString("&")}"
    }

    fun clanCachedPath(rawTag: String): String {
        val input = rawTag.trim().uppercase()
        val normalized = if (input.startsWith("#")) input else "#$input"
        require(Regex("^#[PYLQGRJCUV0289]{3,15}$").matches(normalized)) {
            "Bang hội hiện cần tra cứu bằng Clan TAG hợp lệ, ví dụ #2PR8V8."
        }
        return "/v2/clan/${encode(normalized)}/cached"
    }

    private fun tag(entry: ClashLookupEntry): String = encode(entry.tag.trim().uppercase())

    fun leaderboardPath(category: String, value: String, season: String, teamSize: String): String = when (category) {
        "townhall" -> "/v2/leaderboard/townhalls/${encode(value)}?limit=100"
        "league" -> "/v2/leaderboard/league/${encode(value)}?limit=100"
        "war_triples" -> "/v2/leaderboard/war-triples/${encode(value)}?limit=100"
        "clan_donations" -> "/v2/leaderboard/${encode(value)}/clan/donations?limit=100"
        "clan_war_wins" -> "/v2/leaderboard/${encode(value)}/clan/war-wins?limit=100"
        "clan_capital_gold" -> "/v2/leaderboard/${encode(value)}/clan/capital-gold?limit=100"
        "clan_cwl" -> {
            require(season.isNotBlank() && teamSize.toIntOrNull() != null) { "CWL cần mùa giải và quy mô đội (15 hoặc 30)." }
            "/v2/leaderboard/cwl/${encode(value)}?season=${encode(season)}&team_size=${encode(teamSize)}"
        }
        else -> "/v2/leaderboard/clan/win-streak?limit=100"
    }

    fun analyticsPath(category: String, primary: String, secondary: String, date: String, from: String, to: String,
        leaderboardType: String, mode: String, cohort: String): String {
        val params = mutableListOf<String>()
        fun add(key: String, value: String) { value.trim().takeIf(String::isNotBlank)?.let { params += "$key=${encode(it)}" } }
        fun withQuery(path: String): String = if (params.isEmpty()) path else "$path?${params.joinToString("&")}"
        return when (category) {
            "global_counts" -> "/v2/counts"
            "historical_leaderboard" -> {
                require(primary.isNotBlank() && date.isNotBlank()) { "Cần nhập ID khu vực và ngày (YYYY-MM-DD)." }
                "/v2/leaderboard/history/${encode(leaderboardType)}/${encode(primary)}/${encode(date)}"
            }
            "trophy_buckets" -> {
                require(primary.isNotBlank()) { "Cần nhập ID giải đấu." }
                "/v2/leaderboard/${encode(primary)}/trophy-buckets"
            }
            "legend_season" -> {
                require(primary.isNotBlank()) { "Cần nhập mùa Legend, ví dụ YYYY-MM." }
                add("limit", secondary.ifBlank { "100" })
                withQuery("/v2/legends/history/${encode(primary)}")
            }
            "ranked_group" -> {
                require(primary.isNotBlank() && secondary.isNotBlank()) { "Cần nhập Season ID và League Group ID." }
                "/v2/ranked/${encode(primary)}/groups/${encode(secondary)}"
            }
            "league_tier_statistics" -> {
                require(primary.isNotBlank() && secondary.isNotBlank()) { "Cần nhập Season ID và League Tier ID." }
                "/v2/stats/league/tournaments/${encode(primary)}/tiers/${encode(secondary)}"
            }
            "league_hit_rates" -> {
                add("time[after]", from); add("time[before]", to); add("mode", mode)
                if (mode == "ranked") {
                    add("leagueTierId", primary)
                    add("townHallLevel", secondary)
                }
                withQuery("/v2/stats/league/hit-rates")
            }
            "legend_days" -> {
                add("time[after]", from); add("time[before]", to); add("cohort", cohort)
                withQuery("/v2/stats/legend/days")
            }
            "legend_performance" -> {
                add("time[after]", from); add("time[before]", to); add("cohort", cohort)
                add("compare", "true"); add("armyTypes", "true")
                withQuery("/v2/stats/legend/performance")
            }
            else -> throw IllegalArgumentException("Loại thống kê chưa được hỗ trợ.")
        }
    }

    fun detailRequests(entry: ClashLookupEntry, section: String): List<LookupRequest> {
        val tag = tag(entry)
        fun req(title: String, route: String, includeFields: Set<String>? = null) =
            LookupRequest(title, route, includeFields = includeFields)
        fun data(title: String, payload: JSONObject) = LookupRequest(title, "", payload)
        fun player(route: String, query: String = "") = "/v2/player/$tag/$route" + query
        fun clan(route: String, query: String = "") = "/v2/clan/$tag/$route" + query
        val limit = "?limit=50"

        return if (!entry.isClan) {
            when (section) {
                "TỔNG QUAN" -> listOf(
                    data("Hồ sơ người chơi và Nhà chính", entry.raw)
                )
                "QUÂN ĐỘI" -> listOf(
                    req("Lịch sử nâng cấp quân lính", player("history/changes", "?type=troop_level&limit=50")),
                    req("Lịch sử kích hoạt Super Troop", player("history/changes", "?type=super_troop_boost&limit=50")),
                    req("Lịch sử anh hùng", player("history/changes", "?type=hero_level&limit=50")),
                    req("Lịch sử phép", player("history/changes", "?type=spell_level&limit=50")),
                    req("Lịch sử thú cưng", player("history/changes", "?type=pet_level&limit=50")),
                    req("Lịch sử trang bị anh hùng", player("history/changes", "?type=equipment_level&limit=50")),
                    req("Lịch sử nâng Nhà chính", player("history/changes", "?type=townhall_level&limit=50"))
                )
                "THÀNH TÍCH" -> listOf(
                    req("Kỷ lục cúp Nhà chính", player("history/changes", "?type=best_trophies&limit=50")),
                    req("Kỷ lục cúp Nhà thợ xây", player("history/changes", "?type=best_builder_base_trophies&limit=50")),
                    req("Lịch sử cấp kinh nghiệm", player("history/changes", "?type=exp_level&limit=50")),
                    req("Lịch sử ưu tiên hội chiến", player("history/changes", "?type=war_preference&limit=50")),
                    req("Lịch sử đổi tên", player("history/changes", "?type=name&limit=50")),
                    req("Lịch sử giải đấu", player("history/changes", "?type=league_tier&limit=50")),
                    req("Lịch sử sao hội chiến", player("history/changes", "?type=war_stars&limit=50")),
                    req("Lịch sử đóng góp Thủ phủ", player("history/changes", "?type=clan_capital_contributions&limit=50"))
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
                    req("Hồ sơ bang hội và danh sách thành viên", clan("cached"))
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
                    req("Tổng quyên góp trong tháng", clan("donations/summary"))
                )
                "THỦ PHỦ" -> listOf(
                    req("Xếp hạng điểm Thủ phủ", clan("rankings"), setOf("clanCapital", "capitalGold")),
                    req("Cấp giải và tổng Vàng Thủ phủ", clan("cached"), setOf("capitalGoldTotal", "capitalLeague"))
                )
                "XẾP HẠNG" -> listOf(
                    req("Xếp hạng hiện tại", clan("rankings")),
                    req("Lịch sử xếp hạng theo ngày", clan("history/leaderboards")),
                    req("Tổng kết xếp hạng Điểm Nhà chính", clan("history/leaderboards/summary", "?type=clan_home_points")),
                    req("Tổng kết xếp hạng Điểm Nhà thợ xây", clan("history/leaderboards/summary", "?type=clan_builder_base_points")),
                    req("Tổng kết xếp hạng Điểm Thủ phủ", clan("history/leaderboards/summary", "?type=clan_capital_points"))
                )
                "HUYỀN THOẠI" -> listOf(
                    req("Lịch sử người chơi Legend trong bang", clan("history/legends", limit)),
                    req("Tổng kết Legend và thành tích nổi bật", clan("history/legends/summary", "?top=20"))
                )
                "KỶ LỤC" -> listOf(
                    req("Điểm clan và chuỗi thắng kỷ lục", clan("records"))
                )
                "LỊCH SỬ" -> listOf(
                    req("Lịch sử thay đổi cấp và mô tả bang", clan("history/changes", limit))
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
            if (isClan) json.lookupText("clanTag").ifBlank { "Bang hội chưa rõ tên" } else "Người chơi chưa rõ tên"
        },
        tag = json.lookupText("tag").ifBlank { json.lookupText("playerTag").ifBlank { json.lookupText("clanTag") } },
        isClan = isClan,
        townHall = number("townHallLevel", "townhallLevel", "townhall_level"),
        clanName = clan?.lookupText("name").orEmpty(),
        league = league?.lookupText("name").orEmpty(),
        trophies = number("trophies", "leagueTrophies"),
        members = number("members", "memberCount"),
        level = number("clanLevel", "level"),
        rank = number("rank", "globalRank", "groupRank"),
        streak = number("war_win_streak", "warWinStreak", "streak"),
        metric = number("donations", "war_wins", "capital_gold_total", "stars"),
        metricLabel = when {
            json.has("donations") -> "Quyên góp"
            json.has("war_wins") -> "Thắng hội chiến"
            json.has("capital_gold_total") -> "Vàng Thủ phủ"
            json.has("stars") -> "Sao CWL"
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
        "clanTag" to "TAG bang hội",
        "playerTag" to "TAG người chơi",
        "leagueGroupId" to "Mã nhóm giải",
        "seasonId" to "Mã mùa giải",
        "leagueTierId" to "Mã bậc giải",
        "cwlLeagueId" to "Mã giải CWL",
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
    var rankingSeason by rememberSaveable { mutableStateOf("") }
    var rankingTeamSize by rememberSaveable { mutableStateOf("15") }
    var analyticsCategory by rememberSaveable { mutableStateOf("global_counts") }
    var analyticsPrimary by rememberSaveable { mutableStateOf("") }
    var analyticsSecondary by rememberSaveable { mutableStateOf("") }
    var analyticsDate by rememberSaveable { mutableStateOf("") }
    var analyticsFrom by rememberSaveable { mutableStateOf("") }
    var analyticsTo by rememberSaveable { mutableStateOf("") }
    var analyticsLeaderboardType by rememberSaveable { mutableStateOf("player_home_trophies") }
    var analyticsMode by rememberSaveable { mutableStateOf("ranked") }
    var analyticsCohort by rememberSaveable { mutableStateOf("legend_i") }
    var analyticsResult by remember { mutableStateOf<JSONObject?>(null) }
    var analyticsResultTitle by remember { mutableStateOf("DỮ LIỆU THỐNG KÊ") }
    var extendedSeasonId by rememberSaveable { mutableStateOf("") }
    var extendedLegendDay by rememberSaveable { mutableStateOf("") }
    var extendedOtherPlayerTag by rememberSaveable { mutableStateOf("") }
    var extendedWarEndTime by rememberSaveable { mutableStateOf("") }
    var query by rememberSaveable { mutableStateOf("") }
    var showAdvancedFilters by rememberSaveable { mutableStateOf(false) }
    var clanTags by rememberSaveable { mutableStateOf("") }
    var leagueIds by rememberSaveable { mutableStateOf("") }
    var townhallLevels by rememberSaveable { mutableStateOf("") }
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
        townhallLevels = townhallLevels    )

    fun runSearch(loadMore: Boolean = false) {
        val term = query.trim()
        val isLeaderboard = searchMode == "leaderboard"
        val isAnalytics = searchMode == "analytics"
        if (!isLeaderboard && !isAnalytics && term.length < 2) {
            searchError = "Nhập ít nhất 2 ký tự tên hoặc TAG/ID cần tìm."
            return
        }
        if (isLeaderboard && rankingCategory != "clan_win_streak" && rankingValue.toIntOrNull() == null) {
            searchError = "Nhập cấp Nhà chính, ID khu vực hoặc ID giải đấu hợp lệ."
            return
        }
        if (isSearching) return
        val oldCursor = if (loadMore) nextCursor else null
        val selectedFilters = filters
        val isClanLeaderboard = isLeaderboard && rankingCategory.startsWith("clan_")
        scope.launch {
            isSearching = true
            searchError = null
            if (!loadMore) {
                results = emptyList()
                nextCursor = null
                analyticsResult = null
            }
            try {
                if (isAnalytics) {
                    val path = ClashKingLookupApi.analyticsPath(analyticsCategory, analyticsPrimary, analyticsSecondary, analyticsDate,
                        analyticsFrom, analyticsTo, analyticsLeaderboardType, analyticsMode, analyticsCohort)
                    analyticsResult = ClashKingLookupApi.get(path)
                    analyticsResultTitle = when (analyticsCategory) {
                        "global_counts" -> "THỐNG KÊ TỔNG QUAN CLASHKING"
                        "historical_leaderboard" -> "BẢNG XẾP HẠNG LỊCH SỬ"
                        "trophy_buckets" -> "PHÂN BỐ CÚP THEO GIẢI ĐẤU"
                        "legend_season" -> "DỮ LIỆU HUYỀN THOẠI THEO MÙA"
                        "ranked_group" -> "NHÓM XẾP HẠNG RANKED"
                        "league_tier_statistics" -> "THỐNG KÊ HẠNG GIẢI RANKED"
                        "league_hit_rates" -> "TỶ LỆ SAO RANKED / LEGEND"
                        "legend_days" -> "THỐNG KÊ CHI TIẾT LEGEND"
                        else -> "PHÂN TÍCH HIỆU SUẤT LEGEND"
                    }
                    results = emptyList()
                    nextCursor = null
                } else {
                    if (isLeaderboard) {
                        val response = ClashKingLookupApi.get(ClashKingLookupApi.leaderboardPath(rankingCategory, rankingValue, rankingSeason, rankingTeamSize))
                        results = lookupItems(response, isClanLeaderboard)
                        nextCursor = null
                    } else if (searchMode == "clan") {
                        val clan = ClashKingLookupApi.get(ClashKingLookupApi.clanCachedPath(term))
                        if (clan.lookupText("tag").isBlank()) throw IOException("Không tìm thấy bang hội theo TAG này hoặc ClashKing chưa lưu hồ sơ.")
                        results = listOf(lookupEntry(clan, true))
                        nextCursor = null
                    } else {
                        val response = ClashKingLookupApi.get(ClashKingLookupApi.playerSearchPath(term, oldCursor, selectedFilters))
                        results = lookupItems(response, false)
                        nextCursor = response.optJSONObject("pagination")?.lookupText("nextCursor")?.takeIf { it.isNotBlank() }
                    }
                    if (results.isEmpty()) searchError = "Không tìm thấy kết quả. Hãy kiểm tra tên, TAG/ID hoặc bộ lọc."
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
        if (selectedSection == "DỮ LIỆU MỞ RỘNG") {
            detailPanels = emptyList()
            detailError = null
            detailLoading = false
            return@LaunchedEffect
        }
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
                    val payload = ClashKingLookupApi.get(request.path)
                    val visiblePayload = request.includeFields?.let { fields ->
                        JSONObject().apply { fields.forEach { key -> if (payload.has(key)) put(key, payload.opt(key)) } }
                    } ?: payload
                    LookupPanel(request.title, visiblePayload)
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
            listOf("TỔNG QUAN", "THÀNH VIÊN", "HỘI CHIẾN", "CWL", "QUYÊN GÓP", "THỦ PHỦ", "XẾP HẠNG", "HUYỀN THOẠI", "KỶ LỤC", "LỊCH SỬ", "DỮ LIỆU MỞ RỘNG")
        } else {
            listOf("TỔNG QUAN", "QUÂN ĐỘI", "THÀNH TÍCH", "HOẠT ĐỘNG", "TRẬN ĐẤU", "HỘI CHIẾN", "CWL", "HUYỀN THOẠI", "XẾP HẠNG", "BANG HỘI", "THỜI GIAN", "DỮ LIỆU MỞ RỘNG")
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

            if (selectedSection == "DỮ LIỆU MỞ RỘNG") {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)),
                    border = BorderStroke(1.dp, LookupGold.copy(alpha = 0.35f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(Modifier.fillMaxWidth().padding(13.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        Text("ENDPOINT CÔNG KHAI CẦN THAM SỐ", color = LookupGold, fontSize = 10.sp, fontWeight = FontWeight.Black)
                        if (!entry.isClan) {
                            LookupFilterField("Season ID Ranked (để tải nhóm và battlelog)", extendedSeasonId, { extendedSeasonId = it }, "Season ID / YYYY-MM")
                            LookupFilterField("Ngày Legend (YYYY-MM-DD)", extendedLegendDay, { extendedLegendDay = it }, "2026-11-20")
                            LookupFilterField("TAG người chơi khác (để so lịch sử bang)", extendedOtherPlayerTag, { extendedOtherPlayerTag = it }, "#ABC123")
                            Text("Nhập một hoặc nhiều trường. Season ID tải nhóm Ranked và battlelog; ngày tải battlelog Legend; TAG khác tải thời gian hai người cùng ở bang.", fontSize = 9.sp, color = LookupMuted, lineHeight = 13.sp)
                        } else {
                            LookupFilterField("Thời điểm kết thúc hội chiến (ISO 8601)", extendedWarEndTime, { extendedWarEndTime = it }, "2026-11-20T00:00:00.000Z")
                            Text("Dùng thời điểm endTime của một cuộc hội chiến đã lưu để tải hồ sơ đầy đủ của cuộc chiến đó.", fontSize = 9.sp, color = LookupMuted, lineHeight = 13.sp)
                        }
                        Button(
                            onClick = {
                                val playerTag = URLEncoder.encode(entry.tag.trim().uppercase(), "UTF-8")
                                val clanTag = URLEncoder.encode(entry.tag.trim().uppercase(), "UTF-8")
                                val requests = mutableListOf<LookupRequest>()
                                if (!entry.isClan) {
                                    val season = extendedSeasonId.trim()
                                    val day = extendedLegendDay.trim()
                                    val otherTag = extendedOtherPlayerTag.trim().uppercase()
                                    if (season.isNotBlank()) {
                                        val encodedSeason = URLEncoder.encode(season, "UTF-8")
                                        requests += LookupRequest("Nhóm Ranked của mùa $season", "/v2/player/$playerTag/ranked/$encodedSeason/group")
                                        requests += LookupRequest("Battlelog Ranked của mùa $season", "/v2/player/$playerTag/ranked/$encodedSeason/battlelog")
                                    }
                                    if (day.isNotBlank()) {
                                        val encodedDay = URLEncoder.encode(day, "UTF-8")
                                        requests += LookupRequest("Battlelog Legend ngày $day", "/v2/player/$playerTag/legend/$encodedDay/battlelog")
                                    }
                                    if (otherTag.isNotBlank()) {
                                        val encodedOtherTag = URLEncoder.encode(otherTag, "UTF-8")
                                        requests += LookupRequest("Lịch sử cùng bang với $otherTag", "/v2/player/$playerTag/join-leave/shared?tag=$encodedOtherTag")
                                    }
                                } else if (extendedWarEndTime.isNotBlank()) {
                                    val encodedEndTime = URLEncoder.encode(extendedWarEndTime.trim(), "UTF-8")
                                    requests += LookupRequest("Chi tiết hội chiến kết thúc $extendedWarEndTime", "/v2/war/$clanTag/previous/$encodedEndTime")
                                }
                                if (requests.isEmpty()) {
                                    detailError = if (entry.isClan) "Nhập endTime của hội chiến trước khi tải." else "Nhập ít nhất một tham số để tải dữ liệu."
                                } else {
                                    scope.launch {
                                        detailLoading = true
                                        detailError = null
                                        detailPanels = requests.map { request ->
                                            async {
                                                runCatching { LookupPanel(request.title, ClashKingLookupApi.get(request.path)) }
                                                    .getOrElse { error -> LookupPanel(request.title, null, error.message ?: "Không thể tải dữ liệu.") }
                                            }
                                        }.awaitAll()
                                        val failures = detailPanels.filter { it.payload == null }
                                        detailError = failures.takeIf { it.isNotEmpty() }?.joinToString("\n") { "• ${it.title}: ${it.error}" }
                                        detailLoading = false
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !detailLoading,
                            colors = ButtonDefaults.buttonColors(containerColor = LookupGold, contentColor = Color(0xFF1A1300))
                        ) {
                            Text(if (detailLoading) "ĐANG TẢI..." else "TẢI DỮ LIỆU MỞ RỘNG", fontSize = 10.sp, fontWeight = FontWeight.Black)
                        }
                    }
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
                "Dữ liệu công khai từ ClashKing API. Các API riêng tư yêu cầu tài khoản đã xác minh hoặc quyền bot (last-activity, member-activity, clan warlog) không được gọi ở chế độ công khai. Hoạt động và lịch sử chỉ phản ánh những gì API đã ghi nhận.",
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
                            analyticsResult = null
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
                            analyticsResult = null
                            searchError = null
                        },
                        label = { Text("BANG HỘI", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                        leadingIcon = { Icon(Icons.Outlined.Groups, null, modifier = Modifier.size(16.dp)) }
                    )
                    FilterChip(
                        selected = searchMode == "leaderboard",
                        onClick = { searchMode = "leaderboard"; results = emptyList(); nextCursor = null; analyticsResult = null; searchError = null },
                        label = { Text("XẾP HẠNG", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                    )
                    FilterChip(
                        selected = searchMode == "analytics",
                        onClick = { searchMode = "analytics"; results = emptyList(); nextCursor = null; analyticsResult = null; searchError = null },
                        label = { Text("THỐNG KÊ MỞ RỘNG", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                    )
                }

                if (searchMode != "leaderboard" && searchMode != "analytics") OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(if (searchMode == "player") "Tên người chơi hoặc #TAG" else "Clan TAG (bắt buộc)") },
                    placeholder = { Text(if (searchMode == "player") "Nhập tên hoặc #ABC123" else "Nhập #2PR8V8") },
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
                        FilterChip(selected = rankingCategory == "clan_cwl", onClick = { rankingCategory = "clan_cwl"; rankingValue = "48000000" }, label = { Text("CLAN CWL", fontSize = 9.sp) })
                        FilterChip(selected = rankingCategory == "war_triples", onClick = { rankingCategory = "war_triples"; rankingValue = "19" }, label = { Text("CHUỖI 3 SAO", fontSize = 9.sp) })
                    }
                    if (rankingCategory != "clan_win_streak") {
                        val isClanLeaderboard = rankingCategory.startsWith("clan_")
                        val label = when {
                            rankingCategory == "townhall" || rankingCategory == "war_triples" -> "Cấp Nhà chính"
                            rankingCategory == "league" -> "ID giải đấu"
                            rankingCategory == "clan_cwl" -> "ID giải CWL"
                            else -> "ID quốc gia / khu vực"
                        }
                        LookupFilterField(
                            label,
                            rankingValue,
                            { rankingValue = it },
                            when {
                                rankingCategory == "townhall" || rankingCategory == "war_triples" -> "19"
                                rankingCategory == "league" -> "29000000"
                                rankingCategory == "clan_cwl" -> "48000000"
                                else -> "32000006"
                            }
                        )
                        if (rankingCategory == "clan_cwl") {
                            LookupFilterField("Mùa CWL (YYYY-MM)", rankingSeason, { rankingSeason = it }, "2026-11")
                            LookupFilterField("Quy mô đội (15 hoặc 30)", rankingTeamSize, { rankingTeamSize = it }, "15")
                        }
                        Text(
                            when {
                                rankingCategory == "clan_cwl" -> "Bảng xếp hạng CWL cần ID giải, mùa giải và quy mô đội."
                                rankingCategory == "war_triples" -> "Bảng chuỗi 3 sao theo cấp Nhà chính."
                                isClanLeaderboard -> "Các bảng xếp hạng clan dùng ID khu vực; 32000006 là khu vực International."
                                else -> "Nhập đúng ID Nhà chính hoặc ID giải đấu theo dữ liệu ClashKing."
                            },
                            color = LookupMuted,
                            fontSize = 9.sp,
                            lineHeight = 13.sp
                        )
                    } else {
                        Text("Bảng xếp hạng clan theo chuỗi thắng hiện tại.", color = LookupMuted, fontSize = 9.sp)
                    }
                }

                if (searchMode == "analytics") {
                    Text("API THỐNG KÊ CÔNG KHAI", color = LookupGold, fontSize = 10.sp, fontWeight = FontWeight.Black)
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("global_counts" to "TỔNG QUAN", "historical_leaderboard" to "BXH LỊCH SỬ",
                            "trophy_buckets" to "PHÂN BỐ CÚP", "legend_season" to "MÙA LEGEND", "ranked_group" to "NHÓM RANKED",
                            "league_tier_statistics" to "THỐNG KÊ HẠNG", "league_hit_rates" to "TỶ LỆ SAO",
                            "legend_days" to "LEGEND THEO NGÀY", "legend_performance" to "HIỆU SUẤT LEGEND").forEach { (key, label) ->
                            FilterChip(selected = analyticsCategory == key,
                                onClick = { analyticsCategory = key; analyticsResult = null; searchError = null },
                                label = { Text(label, fontSize = 8.sp) })
                        }
                    }
                    when (analyticsCategory) {
                        "historical_leaderboard" -> {
                            Text("Loại bảng xếp hạng", color = LookupMuted, fontSize = 9.sp)
                            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                listOf("player_home_trophies" to "NHÀ CHÍNH", "player_builder_base_trophies" to "NHÀ THỢ XÂY",
                                    "clan_home_points" to "ĐIỂM CLAN", "clan_builder_base_points" to "ĐIỂM XÂY",
                                    "clan_capital_points" to "ĐIỂM THỦ PHỦ").forEach { (key, label) ->
                                    FilterChip(selected = analyticsLeaderboardType == key, onClick = { analyticsLeaderboardType = key },
                                        label = { Text(label, fontSize = 8.sp) })
                                }
                            }
                            LookupFilterField("ID khu vực", analyticsPrimary, { analyticsPrimary = it }, "32000006")
                            LookupFilterField("Ngày snapshot (YYYY-MM-DD)", analyticsDate, { analyticsDate = it }, "2026-11-20")
                        }
                        "trophy_buckets" -> LookupFilterField("ID giải đấu", analyticsPrimary, { analyticsPrimary = it }, "29000000")
                        "legend_season" -> {
                            LookupFilterField("Mùa Legend (YYYY-MM)", analyticsPrimary, { analyticsPrimary = it }, "2026-10")
                            LookupFilterField("Số kết quả tối đa", analyticsSecondary, { analyticsSecondary = it }, "100")
                        }
                        "ranked_group" -> {
                            LookupFilterField("Season ID", analyticsPrimary, { analyticsPrimary = it }, "Season ID")
                            LookupFilterField("League Group ID", analyticsSecondary, { analyticsSecondary = it }, "League Group ID")
                        }
                        "league_tier_statistics" -> {
                            LookupFilterField("Season ID", analyticsPrimary, { analyticsPrimary = it }, "Season ID")
                            LookupFilterField("League Tier ID", analyticsSecondary, { analyticsSecondary = it }, "29000000")
                        }
                        "league_hit_rates" -> {
                            LookupFilterField("Từ ngày", analyticsFrom, { analyticsFrom = it }, "2026-11-01")
                            LookupFilterField("Đến ngày", analyticsTo, { analyticsTo = it }, "2026-11-20")
                            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                FilterChip(selected = analyticsMode == "ranked", onClick = { analyticsMode = "ranked" }, label = { Text("RANKED", fontSize = 8.sp) })
                                FilterChip(selected = analyticsMode == "legend", onClick = { analyticsMode = "legend" }, label = { Text("LEGEND", fontSize = 8.sp) })
                            }
                            LookupFilterField("League Tier ID (tùy chọn)", analyticsPrimary, { analyticsPrimary = it }, "29000000")
                            LookupFilterField("Cấp Nhà chính (tùy chọn)", analyticsSecondary, { analyticsSecondary = it }, "19")
                        }
                        "legend_days", "legend_performance" -> {
                            LookupFilterField("Từ ngày (YYYY-MM-DD)", analyticsFrom, { analyticsFrom = it }, "2026-11-01")
                            LookupFilterField("Đến ngày (YYYY-MM-DD)", analyticsTo, { analyticsTo = it }, "2026-11-20")
                            Text("Nhóm dữ liệu", color = LookupMuted, fontSize = 9.sp)
                            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                val cohorts = if (analyticsCategory == "legend_days") listOf("legend_i", "top_1000", "top_200") else listOf("observed", "legend_i", "top_1000", "top_200")
                                cohorts.forEach { cohort ->
                                    FilterChip(selected = analyticsCohort == cohort, onClick = { analyticsCohort = cohort },
                                        label = { Text(cohort.uppercase().replace('_', ' '), fontSize = 8.sp) })
                                }
                            }
                        }
                    }
                    Text("Ngày có thể để trống nếu API hỗ trợ mặc định. ID và Season ID phải lấy từ dữ liệu ClashKing.", color = LookupMuted, fontSize = 9.sp, lineHeight = 13.sp)
                }

                if (searchMode == "player") Button(
                    onClick = { showAdvancedFilters = !showAdvancedFilters },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = LookupCyan
                    )
                ) {
                    Text(if (showAdvancedFilters) "ẨN BỘ LỌC NÂNG CAO" else "BỘ LỌC NÂNG CAO", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }

                if (showAdvancedFilters && searchMode == "player") {
                    LookupFilterField("TAG bang hội (phân cách bằng dấu phẩy)", clanTags, { clanTags = it }, "#ABC123")
                    LookupFilterField("ID giải đấu (phân cách bằng dấu phẩy)", leagueIds, { leagueIds = it }, "29000000")
                    LookupFilterField("Cấp Nhà chính (phân cách bằng dấu phẩy)", townhallLevels, { townhallLevels = it }, "17,18,19")
                    Text(
                        "Bộ lọc phải dùng TAG và ID đúng theo dữ liệu ClashKing. Để trống mục nào thì không áp dụng mục đó.",
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
                        Text(when (searchMode) { "leaderboard" -> "XEM XẾP HẠNG"; "analytics" -> "TẢI THỐNG KÊ"; else -> "TÌM KIẾM" },
                            fontWeight = FontWeight.Black, letterSpacing = 0.5.sp)
                    }
                }
            }
        }

        if (searchError != null) LookupMessageCard(searchError ?: "", isError = true)
        analyticsResult?.let { LookupDataPanel(title = analyticsResultTitle, payload = it) }
        if (searchMode == "clan" && searchError == null) {
            Text("Tra cứu bang hội bằng Clan TAG. API công khai hiện chưa có chức năng tìm bang hội theo tên.", color = LookupMuted, fontSize = 9.sp, lineHeight = 13.sp)
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
        } else if (searchError == null && !isSearching && analyticsResult == null) {
            LookupMessageCard(
                when (searchMode) {
                    "player" -> "Tìm người chơi bằng tên hoặc TAG. Hồ sơ hỗ trợ hoạt động, lịch sử quân đội, trận đấu, hội chiến, CWL, Legend, Ranked và xếp hạng."
                    "clan" -> "Tìm bang hội bằng tên hoặc Clan TAG. Hồ sơ hỗ trợ thành viên, hội chiến, CWL, quyên góp, Thủ phủ, xếp hạng, kỷ lục và lịch sử."
                    "leaderboard" -> "Tra cứu bảng xếp hạng theo Nhà chính, giải đấu, chuỗi 3 sao và các hạng mục bang hội."
                    else -> "Truy cập thống kê công khai: bảng xếp hạng lịch sử, nhóm Ranked, phân bố cúp, Legend và dữ liệu tổng quan."
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
                        entry.clanName.takeIf { it.isNotBlank() },
                        entry.streak.takeIf { it.isNotBlank() }?.let { "$it chuỗi 3 sao" }
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
