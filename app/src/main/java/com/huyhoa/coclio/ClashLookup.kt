package com.huyhoa.coclio

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.outlined.CompareArrows
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.huyhoa.coclio.data.CocAssets
import com.huyhoa.coclio.data.VillageItem
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
import java.text.Normalizer
import java.text.NumberFormat
import java.time.LocalDate
import java.util.Locale

private const val CLASHKING_API = "https://api.clashk.ing"
private val LookupCyan = Color(0xFF36F4FF)
private val LookupGold = Color(0xFFFFD166)
private val LookupMuted = Color(0xFF9EAFCE)

internal data class ClashLookupEntry(
    val name: String,
    val tag: String,
    val isClan: Boolean,
    val townHall: String = "",
    val builderHall: String = "",
    val playerLevel: String = "",
    val builderTrophies: String = "",
    val clanName: String = "",
    val league: String = "",
    val trophies: String = "",
    val members: String = "",
    val level: String = "",
    val rank: String = "",
    val streak: String = "",
    val metric: String = "",
    val metricLabel: String = "",
    // Clan crest and player league badge are different assets and must never be interchanged.
    val badgeUrl: String = "",
    val leagueIconUrl: String = "",
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

private fun normalizeLookupTag(raw: String, label: String): String {
    val input = raw.trim().uppercase().replace('O', '0')
    val normalized = if (input.startsWith("#")) input else "#$input"
    require(Regex("^#[PYLQGRJCUV0289]{3,15}$").matches(normalized)) {
        "$label không hợp lệ. Hãy nhập TAG dạng #ABC123."
    }
    return normalized
}

internal fun lookupSearchCandidates(raw: String): List<String> {
    val input = raw.trim()
    if (input.isBlank()) return emptyList()

    val candidates = mutableListOf<String>()
    val tagPattern = Regex("^[PYLQGRJCUV0289]{3,15}$")
    val normalizedInput = input.uppercase().replace('O', '0')
    val embeddedTag = Regex("#[PYLQGRJCUV0289]{3,15}")
        .find(normalizedInput)
        ?.value
    val standaloneTag = normalizedInput.removePrefix("#").let { value ->
        value.takeIf { tagPattern.matches(it) }
    }
    val tagInput = embeddedTag ?: standaloneTag
    if (tagInput != null) {
        val exactTag = runCatching { normalizeLookupTag(tagInput, "TAG tìm kiếm") }.getOrNull()
        if (exactTag != null) {
            candidates += exactTag
            candidates += exactTag.removePrefix("#")
        }
        if (!input.equals(exactTag, ignoreCase = true)) candidates += input
        return candidates.distinct()
    }

    candidates += input
    val compatible = Normalizer.normalize(input, Normalizer.Form.NFKD)
        .replace(Regex("[\\p{M}\\p{Cf}\\p{So}]"), " ")
        .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()
    if (compatible.length >= 2 && compatible != input) candidates += compatible

    // ClashKing matches all words in one query. Retry meaningful name parts
    // individually so a partial or decorated multi-word name can still match.
    compatible.split(' ')
        .asSequence()
        .filter { it.length >= 2 }
        .distinct()
        .sortedByDescending { it.length }
        .take(4)
        .forEach { candidates += it }

    return candidates.distinct()
}

private fun validateLookupWarEndTime(raw: String): String {
    val value = raw.trim()
    require(Regex("^\\d{8}T\\d{6}\\.\\d{3}Z$").matches(value)) {
        "Thời điểm kết thúc phải đúng dạng YYYYMMDDTHHmmss.SSSZ."
    }
    return value
}

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
                throw IOException(httpErrorMessage(status, detail))
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
        fun values(raw: String) = raw.split(",", ";", " ").map(String::trim).filter(String::isNotBlank)
        fun addList(name: String, raw: String, maximumCount: Int, validate: (String) -> String) {
            val entries = values(raw).map(validate).distinct()
            require(entries.size <= maximumCount) { "$name chỉ nhận tối đa $maximumCount giá trị." }
            entries.forEach { params += "${name}=${encode(it)}" }
        }
        addList("clanTags", filters.clanTags, 100) { normalizeLookupTag(it, "TAG bang hội") }
        addList("leagueIds", filters.leagueIds, 5) {
            val value = it.toLongOrNull()
            require(value != null && value in 1L..9_007_199_254_740_991L) { "Mã giải đấu phải là số nguyên dương hợp lệ." }
            value.toString()
        }
        addList("townhallLevels", filters.townhallLevels, 100) {
            val value = it.toIntOrNull()
            require(value != null && value in 1..100) { "Cấp Nhà chính phải là số nguyên từ 1 đến 100." }
            value.toString()
        }
        return "/v2/player/search?${params.joinToString("&")}"
    }

    fun clanSearchPath(query: String, cursor: String?): String {
        val params = mutableListOf("query=${encode(query)}", "limit=20")
        cursor?.takeIf { it.isNotBlank() }?.let { params += "cursor=${encode(it)}" }
        return "/v2/clan/search?${params.joinToString("&")}"
    }

    fun cachedClanPath(tag: String): String =
        "/v2/clan/${encode(normalizeLookupTag(tag, "TAG bang hội"))}/cached"

    private fun tag(entry: ClashLookupEntry): String = encode(entry.tag.trim().uppercase())

    fun leaderboardPath(category: String, value: String, season: String, teamSize: String): String = when (category) {
        "townhall" -> "/v2/leaderboard/townhalls/${encode(value)}?limit=100"
        "league" -> "/v2/leaderboard/league/${encode(value)}?limit=100"
        "war_triples" -> "/v2/leaderboard/war-triples/${encode(value)}?limit=100"
        "clan_donations" -> "/v2/leaderboard/${encode(value)}/clan/donations?limit=100"
        "clan_war_wins" -> "/v2/leaderboard/${encode(value)}/clan/war-wins?limit=100"
        "clan_capital_gold" -> "/v2/leaderboard/${encode(value)}/clan/capital-gold?limit=100"
        "clan_cwl" -> {
            require(value.toIntOrNull()?.let { it > 0 } == true) { "Nhập mã bậc giải CWL hợp lệ." }
            require(season.matches(Regex("\\d{4}-(0[1-9]|1[0-2])"))) { "Mùa CWL phải có dạng YYYY-MM." }
            require(teamSize == "15" || teamSize == "30") { "Quy mô đội CWL chỉ nhận 15 hoặc 30." }
            "/v2/leaderboard/cwl/${encode(value)}?season=${encode(season)}&team_size=${encode(teamSize)}"
        }
        else -> "/v2/leaderboard/clan/win-streak?limit=100"
    }

    fun analyticsPath(category: String, primary: String, secondary: String, date: String, from: String, to: String,
        leaderboardType: String, mode: String, cohort: String, interval: String, sort: String, limit: String,
        rankLimit: String, equalTownHalls: String, groupKey: String, variantKey: String): String {
        val params = mutableListOf<String>()
        fun add(key: String, value: String) { value.trim().takeIf(String::isNotBlank)?.let { params += "$key=${encode(it)}" } }
        fun withQuery(path: String): String = if (params.isEmpty()) path else "$path?${params.joinToString("&")}"
        fun validateDay(raw: String, label: String, required: Boolean = false) {
            val value = raw.trim()
            if (value.isBlank()) {
                require(!required) { "Cần nhập $label theo dạng YYYY-MM-DD." }
                return
            }
            require(runCatching { LocalDate.parse(value) }.isSuccess) {
                "$label không hợp lệ. Hãy dùng dạng YYYY-MM-DD."
            }
        }
        fun validateRange(start: String, end: String, maximumDays: Int? = null) {
            validateDay(start, "ngày bắt đầu")
            validateDay(end, "ngày kết thúc")
            if (start.isNotBlank() && end.isNotBlank()) {
                require(start.trim() <= end.trim()) { "Ngày bắt đầu không được sau ngày kết thúc." }
                if (maximumDays != null) {
                    val firstDay = LocalDate.parse(start.trim()).toEpochDay()
                    val lastDay = LocalDate.parse(end.trim()).toEpochDay()
                    require(lastDay - firstDay + 1 <= maximumDays) {
                        "Khoảng ngày không được vượt quá $maximumDays ngày theo giới hạn của API."
                    }
                }
            }
        }
        fun validatePositive(raw: String, label: String, maximum: Int = Int.MAX_VALUE) {
            val value = raw.trim().toIntOrNull()
            require(value != null && value in 1..maximum) { "$label phải là số nguyên từ 1 đến $maximum." }
        }
        fun validateSeason(raw: String, label: String) {
            require(Regex("\\d{4}-(0[1-9]|1[0-2])").matches(raw.trim())) {
                "$label phải có dạng YYYY-MM."
            }
        }
        fun validateRankLimit(raw: String) {
            if (raw.isBlank()) return
            require(raw.trim() == "200" || raw.trim() == "1000") {
                "Giới hạn thứ hạng đội hình chỉ nhận 200 hoặc 1000; để trống để xem toàn bộ."
            }
        }
        return when (category) {
            "global_counts" -> "/v2/counts"
            "counts_townhalls" -> "/v2/counts/players/town-halls"
            "counts_league_tiers" -> "/v2/counts/players/league-tiers"
            "counts_clan_locations" -> "/v2/counts/clans/locations"
            "counts_cwl_leagues" -> "/v2/counts/clans/cwl-leagues"
            "counts_capital_leagues" -> "/v2/counts/clans/capital-leagues"
            "counts_clan_members" -> "/v2/counts/clans/member-bins"
            "stats_ranked" -> {
                validatePositive(primary, "Cấp Nhà chính", 20)
                validatePositive(secondary, "Mã bậc giải xếp hạng")
                validateRange(from, to, 90)
                add("townHallLevel", primary)
                add("leagueTierId", secondary)
                add("startDate", from)
                add("endDate", to)
                withQuery("/v2/stats/ranked")
            }
            "stats_war" -> {
                validateRange(from, to)
                if (primary.isNotBlank()) validatePositive(primary, "Nhà chính người tấn công", 20)
                if (secondary.isNotBlank()) validatePositive(secondary, "Nhà chính đối thủ", 20)
                add("startDate", from)
                add("endDate", to)
                add("townHallLevel", primary)
                add("opponentTownHallLevel", secondary)
                if (equalTownHalls == "true" || equalTownHalls == "false") add("equalTownHalls", equalTownHalls)
                withQuery("/v2/stats/war")
            }
            "stats_cwl" -> {
                if (primary.isNotBlank()) validateSeason(primary, "Mùa CWL")
                add("season", primary)
                withQuery("/v2/stats/cwl")
            }
            "wars_hit_rates" -> {
                validateRange(from, to)
                if (primary.isNotBlank()) validatePositive(primary, "Cấp Nhà chính", 20)
                add("time[after]", from)
                add("time[before]", to)
                add("interval", interval)
                add("townHall", primary)
                withQuery("/v2/stats/wars/hitrates")
            }
            "wars_summary" -> {
                validateRange(from, to)
                if (primary.isNotBlank()) validatePositive(primary, "Quy mô hội chiến", 100)
                add("time[after]", from)
                add("time[before]", to)
                add("interval", interval)
                add("warSize", primary)
                withQuery("/v2/stats/wars/summary")
            }
            "army_setups" -> {
                validateRange(from, to, 90)
                validatePositive(limit.ifBlank { "100" }, "Giới hạn đội hình", 200)
                validateRankLimit(rankLimit)
                add("time[after]", from)
                add("time[before]", to)
                add("sort", sort)
                add("limit", limit.ifBlank { "100" })
                add("rankLimit", rankLimit)
                add("leagueTierId", "105000036")
                withQuery("/v2/stats/army-setups")
            }
            "army_setup_timeline" -> {
                require(groupKey.isNotBlank()) { "Nhập mã nhóm đội hình lấy từ kết quả đội hình Huyền thoại." }
                validateRange(from, to, 365)
                validateRankLimit(rankLimit)
                add("time[after]", from)
                add("time[before]", to)
                add("groupKey", groupKey)
                add("sort", sort)
                add("rankLimit", rankLimit)
                add("leagueTierId", "105000036")
                withQuery("/v2/stats/army-setups/timeline")
            }
            "army_setup_rank_history" -> {
                require(groupKey.isNotBlank()) { "Nhập mã nhóm đội hình lấy từ kết quả đội hình Huyền thoại." }
                validateRange(from, to, 365)
                validateRankLimit(rankLimit)
                add("time[after]", from)
                add("time[before]", to)
                add("groupKey", groupKey)
                add("variantKey", variantKey)
                add("sort", sort)
                add("rankLimit", rankLimit)
                add("leagueTierId", "105000036")
                withQuery("/v2/stats/army-setups/rank-history")
            }
            "legend_rank_curve" -> {
                validateDay(date, "ngày xem xếp hạng Huyền thoại", required = true)
                add("day", date)
                withQuery("/v2/legends/rank-curve")
            }
            "legend_trophy_buckets" -> {
                if (date.isBlank()) "/v2/legends/trophy-buckets"
                else {
                    validateDay(date, "ngày thống kê Huyền thoại")
                    "/v2/legends/trophy-buckets/${encode(date)}"
                }
            }
            "historical_leaderboard" -> {
                require(primary.trim() == "global" || primary.trim().toIntOrNull()?.let { it > 0 } == true) {
                    "Mã khu vực phải là số dương hoặc nhập global để xem bảng toàn cầu."
                }
                validateDay(date, "ngày snapshot", required = true)
                "/v2/leaderboard/history/${encode(leaderboardType)}/${encode(primary.trim())}/${encode(date)}"
            }
            "trophy_buckets" -> {
                validatePositive(primary, "Mã giải đấu")
                "/v2/leaderboard/${encode(primary)}/trophy-buckets"
            }
            "legend_season" -> {
                validateSeason(primary, "Mùa Huyền thoại")
                validatePositive(secondary.ifBlank { "100" }, "Số kết quả tối đa", 200)
                add("limit", secondary.ifBlank { "100" })
                withQuery("/v2/legends/history/${encode(primary)}")
            }
            "ranked_group" -> {
                validatePositive(primary, "Mã mùa giải xếp hạng (Ranked)")
                val groupTag = normalizeLookupTag(secondary, "TAG nhóm xếp hạng")
                "/v2/ranked/${encode(primary)}/groups/${encode(groupTag)}"
            }
            "league_tier_statistics" -> {
                validatePositive(primary, "Mã mùa giải xếp hạng (Ranked)")
                validatePositive(secondary, "Mã bậc giải")
                "/v2/stats/league/tournaments/${encode(primary)}/tiers/${encode(secondary)}"
            }
            "league_hit_rates" -> {
                validateRange(from, to, 90)
                require(mode == "ranked" || mode == "legend") { "Chế độ phải là Xếp hạng (Ranked) hoặc Huyền thoại (Legend)." }
                add("time[after]", from); add("time[before]", to); add("mode", mode)
                if (mode == "ranked") {
                    if (primary.isNotBlank()) validatePositive(primary, "Mã bậc giải xếp hạng")
                    if (secondary.isNotBlank()) validatePositive(secondary, "Cấp Nhà chính", 20)
                    add("leagueTierId", primary)
                    add("townHallLevel", secondary)
                }
                withQuery("/v2/stats/league/hit-rates")
            }
            "legend_days" -> {
                validateRange(from, to, 90)
                add("time[after]", from); add("time[before]", to); add("cohort", cohort)
                withQuery("/v2/stats/legend/days")
            }
            "legend_performance" -> {
                validateRange(from, to, 90)
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
        val hasCachedClanProfile = entry.raw.has("description") && entry.raw.has("members")
        fun cachedClanFields(vararg fields: String) = JSONObject().apply {
            fields.forEach { key -> if (entry.raw.has(key)) put(key, entry.raw.opt(key)) }
        }

        return if (!entry.isClan) {
            when (section) {
                "TỔNG QUAN" -> listOf(
                    data("Hồ sơ người chơi và Nhà chính", entry.raw)
                )
                "QUÂN ĐỘI" -> listOf(
                    req("Lịch sử nâng cấp quân lính", player("history/changes", "?type=troop_level&limit=50")),
                    req("Lịch sử kích hoạt Quân siêu cấp", player("history/changes", "?type=super_troop_boost&limit=50")),
                    req("Lịch sử anh hùng", player("history/changes", "?type=hero_level&limit=50")),
                    req("Lịch sử nâng cấp thần chú", player("history/changes", "?type=spell_level&limit=50")),
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
                    req("Lịch sử thay đổi giải đấu", player("history/changes", "?type=league_tier&limit=50")),
                    req("Lịch sử sao hội chiến", player("history/changes", "?type=war_stars&limit=50")),
                    req("Lịch sử đóng góp Thủ phủ", player("history/changes", "?type=clan_capital_contributions&limit=50"))
                )
                "HOẠT ĐỘNG" -> listOf(
                    req("Hoạt động 30 ngày", player("activity-summary")),
                    req("Tổng tài nguyên cướp được trong tháng", player("farming")),
                    req("Lịch sử quyên góp", player("history/stats", "?type=donated&limit=50")),
                    req("Lịch sử nhận quân", player("history/stats", "?type=received&limit=50")),
                    req("Điểm Trò chơi bang hội", player("history/stats", "?type=clan_games&limit=50")),
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
                    req("Lịch sử giải đấu bang hội (CWL)", player("cwl/history", "?limit=30"))
                )
                "HUYỀN THOẠI" -> listOf(
                    req("Các mùa Huyền thoại đã ghi nhận", player("legend-history")),
                    req("Biểu đồ cúp theo ngày", player("legend/series")),
                    req("Thống kê mùa Huyền thoại hiện tại", player("legend/season")),
                    req("So sánh hiệu suất Huyền thoại", player("legend/comparisons"))
                )
                "XẾP HẠNG" -> listOf(
                    req("Xếp hạng toàn cầu và địa phương", player("rankings")),
                    req("Lịch sử xếp hạng Nhà chính", player("leaderboard-history/player_home_trophies")),
                    req("Lịch sử xếp hạng Nhà thợ xây", player("leaderboard-history/player_builder_base_trophies")),
                    req("Lịch sử xếp hạng (Ranked)", player("ranked/history"))
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
                    if (hasCachedClanProfile) data("Hồ sơ bang hội và danh sách thành viên", entry.raw)
                    else req("Hồ sơ bang hội và danh sách thành viên", clan("cached"))
                )
                "THÀNH VIÊN" -> listOf(
                    req("Thành viên hiện tại và ngày tham gia được ghi nhận", "/v2/clan/$tag/member-joins"),
                    req("Lịch sử thành viên vào và rời bang", clan("join-leave", limit))
                )
                "HỘI CHIẾN" -> listOf(
                    req("Các cuộc hội chiến đã lưu", clan("wars", limit)),
                    req("Mốc hội chiến gần nhất được ghi nhận", "/v2/war/$tag/basic")
                )
                "CWL" -> listOf(
                    req("Các mùa CWL", "/v2/cwl/$tag/seasons?limit=30"),
                    req("Lịch sử thứ hạng CWL", "/v2/cwl/$tag/ranking-history"),
                    req("Nhóm CWL mới nhất được lưu", "/v2/cwl/$tag/group")
                )
                "QUYÊN GÓP" -> listOf(
                    req("Tổng quyên góp trong tháng", clan("donations/summary"))
                )
                "THỦ PHỦ" -> listOf(
                    if (hasCachedClanProfile) data("Cấp giải và tổng Vàng Thủ phủ", cachedClanFields("capitalGoldTotal", "capitalLeague"))
                    else req("Cấp giải và tổng Vàng Thủ phủ", clan("cached"), setOf("capitalGoldTotal", "capitalLeague"))
                )
                "XẾP HẠNG" -> listOf(
                    req("Xếp hạng hiện tại", clan("rankings")),
                    req("Lịch sử thứ hạng Điểm Nhà chính", clan("history/leaderboards", "?type=clan_home_points&limit=50")),
                    req("Lịch sử thứ hạng Điểm Nhà thợ xây", clan("history/leaderboards", "?type=clan_builder_base_points&limit=50")),
                    req("Lịch sử thứ hạng Điểm Thủ phủ", clan("history/leaderboards", "?type=clan_capital_points&limit=50")),
                    req("Tổng kết theo mùa Điểm Nhà chính", clan("history/leaderboards/summary", "?type=clan_home_points")),
                    req("Tổng kết theo mùa Điểm Nhà thợ xây", clan("history/leaderboards/summary", "?type=clan_builder_base_points")),
                    req("Tổng kết theo mùa Điểm Thủ phủ", clan("history/leaderboards/summary", "?type=clan_capital_points"))
                )
                "HUYỀN THOẠI" -> listOf(
                    req("Lịch sử người chơi Huyền thoại trong bang", clan("history/legends", limit)),
                    req("Tổng kết Huyền thoại và thành tích nổi bật", clan("history/legends/summary", "?top=20"))
                )
                "KỶ LỤC" -> listOf(
                    req("Điểm bang hội và chuỗi thắng kỷ lục", clan("records"))
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

internal fun lookupEntry(json: JSONObject, isClan: Boolean): ClashLookupEntry {
    // ClashKing may wrap a result in { player: {...} } or { clan: {...} }.
    // Always unwrap the entity selected by the search mode.
    val entity = json.optJSONObject(if (isClan) "clan" else "player") ?: json
    val clan = entity.optJSONObject("clan")
    val league = entity.optJSONObject("leagueTier") ?: entity.optJSONObject("league")
    val badgeUrls = entity.optJSONObject("badgeUrls") ?: clan?.optJSONObject("badgeUrls")
    val badge = entity.lookupText("badge")
        .ifBlank { clan?.lookupText("badge").orEmpty() }
        .ifBlank { badgeUrls?.lookupText("medium").orEmpty() }
        .ifBlank { badgeUrls?.lookupText("large").orEmpty() }

    fun number(vararg keys: String): String {
        for (key in keys) {
            val value = entity.lookupText(key)
            if (value.isNotBlank() && value != "-1") return value
        }
        return ""
    }

    val tag = entity.lookupText("tag").ifBlank { entity.lookupText(if (isClan) "clanTag" else "playerTag") }
    return ClashLookupEntry(
        name = entity.lookupText("name").ifBlank {
            if (isClan) tag.takeIf { it.isNotBlank() }?.let { "Bang hội $it" } ?: "Bang hội chưa rõ tên"
            else "Người chơi chưa rõ tên"
        },
        tag = tag,
        isClan = isClan,
        townHall = if (isClan) "" else number("townHallLevel", "townhallLevel", "townhall_level"),
        builderHall = if (isClan) "" else number("builderHallLevel", "builderHall"),
        playerLevel = if (isClan) "" else number("expLevel", "experienceLevel", "playerLevel"),
        builderTrophies = if (isClan) "" else number("builderBaseTrophies", "builderTrophies"),
        clanName = if (isClan) "" else clan?.lookupText("name").orEmpty(),
        league = if (isClan) "" else league?.lookupText("name").orEmpty(),
        trophies = if (isClan) "" else number("trophies", "leagueTrophies"),
        members = if (isClan) number("memberCount").ifBlank {
            when (val memberValue = entity.opt("members")) {
                is JSONArray -> memberValue.length().takeIf { it > 0 }?.toString().orEmpty()
                is JSONObject -> memberValue.lookupText("count").ifBlank { memberValue.lookupText("total") }
                    .ifBlank { memberValue.lookupText("memberCount") }
                    .ifBlank { memberValue.optJSONArray("items")?.length()?.takeIf { it > 0 }?.toString().orEmpty() }
                else -> number("members")
            }
        } else "",
        level = if (isClan) number("clanLevel", "level") else "",
        rank = number("rank", "globalRank", "groupRank"),
        streak = if (isClan) number("war_win_streak", "warWinStreak", "streak") else "",
        metric = number("donations", "war_wins", "capital_gold_total", "stars"),
        metricLabel = when {
            entity.has("donations") -> "Quyên góp"
            entity.has("war_wins") -> "Thắng hội chiến"
            entity.has("capital_gold_total") -> "Vàng Thủ phủ"
            entity.has("stars") -> "Sao CWL"
            else -> ""
        },
        badgeUrl = if (isClan) entity.lookupText("badge_url").ifBlank { badge } else "",
        leagueIconUrl = if (!isClan) league?.optJSONObject("iconUrls")?.let { icons ->
            icons.lookupText("medium").ifBlank { icons.lookupText("large") }.ifBlank { icons.lookupText("small") }
        }.orEmpty() else "",
        raw = entity
    )
}

private fun lookupEntryKey(entry: ClashLookupEntry): String =
    "${if (entry.isClan) "clan" else "player"}:${entry.tag.trim().uppercase()}"

private fun deduplicateLookupEntries(entries: List<ClashLookupEntry>): List<ClashLookupEntry> =
    entries.filter { it.tag.isNotBlank() }.distinctBy(::lookupEntryKey)

private fun lookupItems(response: JSONObject, isClan: Boolean): List<ClashLookupEntry> {
    val items = response.optJSONArray("items")
        ?: response.optJSONArray("results")
        ?: response.optJSONArray("data")
        ?: JSONArray()
    return deduplicateLookupEntries(buildList {
        for (index in 0 until items.length()) {
            val item = items.optJSONObject(index) ?: continue
            add(lookupEntry(item, isClan))
        }
    })
}



private const val LOOKUP_LOCAL_PREFS = "clash_lookup_local_v1"
private const val LOOKUP_HISTORY_KEY = "search_history"
private const val LOOKUP_FAVORITES_KEY = "saved_entries"
private const val LOOKUP_HISTORY_LIMIT = 20
private const val LOOKUP_FAVORITES_LIMIT = 40

private data class LookupHistoryItem(
    val mode: String,
    val query: String,
    val searchedAt: Long
)

private fun lookupHistoryIdentity(mode: String, query: String): String {
    val trimmed = query.trim()
    val normalizedTag = lookupSearchCandidates(trimmed).firstOrNull()
        ?.takeIf { it.startsWith("#") && runCatching { normalizeLookupTag(it, "TAG") }.isSuccess }
    val normalizedName = Normalizer.normalize(trimmed, Normalizer.Form.NFKD)
        .replace(Regex("[\\p{M}\\p{Cf}\\p{So}]"), " ")
        .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()
    return "$mode:${(normalizedTag ?: normalizedName).lowercase()}"
}

private fun lookupLocalPreferences(context: Context) =
    context.getSharedPreferences(LOOKUP_LOCAL_PREFS, Context.MODE_PRIVATE)

private fun loadLookupHistory(context: Context): List<LookupHistoryItem> = runCatching {
    val raw = lookupLocalPreferences(context).getString(LOOKUP_HISTORY_KEY, "[]").orEmpty()
    val values = JSONArray(raw)
    buildList {
        for (index in 0 until values.length()) {
            val item = values.optJSONObject(index) ?: continue
            val mode = item.optString("mode")
            val query = item.optString("query").trim()
            if (mode !in setOf("player", "clan") || query.isBlank()) continue
            add(LookupHistoryItem(mode, query, item.optLong("searchedAt")))
        }
    }.distinctBy { lookupHistoryIdentity(it.mode, it.query) }
        .sortedByDescending { it.searchedAt }
        .take(LOOKUP_HISTORY_LIMIT)
}.getOrElse { emptyList() }

private fun recordLookupHistory(context: Context, mode: String, query: String): List<LookupHistoryItem> {
    val cleanQuery = query.trim()
    if (mode !in setOf("player", "clan") || cleanQuery.isBlank()) return loadLookupHistory(context)
    val updated = (
        listOf(LookupHistoryItem(mode, cleanQuery, System.currentTimeMillis())) +
            loadLookupHistory(context).filterNot {
                lookupHistoryIdentity(it.mode, it.query) == lookupHistoryIdentity(mode, cleanQuery)
            }
        ).take(LOOKUP_HISTORY_LIMIT)
    val values = JSONArray()
    updated.forEach { item ->
        values.put(
            JSONObject()
                .put("mode", item.mode)
                .put("query", item.query)
                .put("searchedAt", item.searchedAt)
        )
    }
    lookupLocalPreferences(context).edit().putString(LOOKUP_HISTORY_KEY, values.toString()).apply()
    return updated
}

private fun clearLookupHistory(context: Context) {
    lookupLocalPreferences(context).edit().remove(LOOKUP_HISTORY_KEY).apply()
}

private fun loadLookupFavorites(context: Context): List<ClashLookupEntry> = runCatching {
    val raw = lookupLocalPreferences(context).getString(LOOKUP_FAVORITES_KEY, "[]").orEmpty()
    val values = JSONArray(raw)
    buildList {
        for (index in 0 until values.length()) {
            val item = values.optJSONObject(index) ?: continue
            val savedRaw = item.optJSONObject("raw") ?: continue
            val entry = lookupEntry(savedRaw, item.optBoolean("isClan"))
            if (entry.tag.isNotBlank()) add(entry)
        }
    }.distinctBy { "${it.isClan}:${it.tag.trim().uppercase()}" }
        .take(LOOKUP_FAVORITES_LIMIT)
}.getOrElse { emptyList() }

private fun toggleLookupFavorite(context: Context, entry: ClashLookupEntry): List<ClashLookupEntry> {
    val key = "${entry.isClan}:${entry.tag.trim().uppercase()}"
    val current = loadLookupFavorites(context)
    val alreadySaved = current.any {
        "${it.isClan}:${it.tag.trim().uppercase()}" == key
    }
    val updated = if (alreadySaved) {
        current.filterNot { "${it.isClan}:${it.tag.trim().uppercase()}" == key }
    } else {
        (listOf(entry) + current).take(LOOKUP_FAVORITES_LIMIT)
    }
    val values = JSONArray()
    updated.forEach { saved ->
        values.put(
            JSONObject()
                .put("isClan", saved.isClan)
                .put("raw", JSONObject(saved.raw.toString()))
        )
    }
    lookupLocalPreferences(context).edit().putString(LOOKUP_FAVORITES_KEY, values.toString()).apply()
    return updated
}

private fun shareLookupEntry(context: Context, entry: ClashLookupEntry) {
    val details = if (entry.isClan) {
        listOfNotNull(
            entry.level.takeIf(String::isNotBlank)?.let { "Cấp bang hội: $it" },
            entry.members.takeIf(String::isNotBlank)?.let { "Thành viên: $it" },
            entry.rank.takeIf(String::isNotBlank)?.let { "Thứ hạng: $it" },
            entry.streak.takeIf(String::isNotBlank)?.let { "Chuỗi thắng: $it" }
        )
    } else {
        listOfNotNull(
            entry.townHall.takeIf(String::isNotBlank)?.let { "Nhà chính: $it" },
            entry.trophies.takeIf(String::isNotBlank)?.let { "Cúp: $it" },
            entry.league.takeIf(String::isNotBlank)?.let { "Giải đấu: $it" },
            entry.clanName.takeIf(String::isNotBlank)?.let { "Bang hội: $it" },
            entry.rank.takeIf(String::isNotBlank)?.let { "Thứ hạng: $it" }
        )
    }
    val message = buildString {
        append(if (entry.isClan) "BANG HỘI: " else "NGƯỜI CHƠI: ")
        append(entry.name)
        append("\nTAG: ")
        append(entry.tag)
        if (details.isNotEmpty()) {
            append("\n")
            append(details.joinToString("\n"))
        }
        append("\n\nTra cứu qua COC-LIO • Nguồn dữ liệu: ClashKing API")
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Hồ sơ Clash of Clans - ${entry.name}")
        putExtra(Intent.EXTRA_TEXT, message)
    }
    runCatching { context.startActivity(Intent.createChooser(intent, "Chia sẻ hồ sơ COC-LIO")) }
}



private fun lookupAssetName(value: String): String =
    Normalizer.normalize(value.trim(), Normalizer.Form.NFKD)
        .replace(Regex("[\\p{M}\\p{Cf}]"), "")
        .lowercase()
        .replace(Regex("[^\\p{L}\\p{N}]"), "")

private fun lookupCatalogCategory(parentKey: String): String? = when (parentKey.lowercase()) {
    "troops", "supertroops", "units", "siege", "siegemachines" -> "troops"
    "heroes" -> "heroes"
    "spells" -> "spells"
    "pets" -> "pets"
    "equipment", "heroequipment" -> "equipment"
    "defenses", "defence", "defense" -> "defense"
    "traps" -> "traps"
    "buildings" -> "other"
    else -> null
}

private fun lookupCatalogEntry(
    payload: JSONObject,
    catalog: Map<Int, SupplementalCatalogEntry>,
    parentKey: String
): SupplementalCatalogEntry? {
    val idKeys = listOf("dataId", "id", "unitId", "troopId", "heroId", "spellId", "petId", "equipmentId", "buildingId", "itemId")
    val id = idKeys.firstNotNullOfOrNull { key ->
        (payload.opt(key) as? Number)?.toInt() ?: payload.optString(key).toIntOrNull()
    }
    if (id != null) catalog[id]?.let { return it }

    val name = listOf("name", "unitName", "troopName", "heroName", "spellName", "petName", "equipmentName", "buildingName")
        .firstNotNullOfOrNull { key -> payload.lookupText(key).takeIf(String::isNotBlank) }
        ?: return null
    val normalizedName = lookupAssetName(name)
    val preferredCategory = lookupCatalogCategory(parentKey)
    val matching = catalog.values.asSequence().let { entries ->
        if (preferredCategory == null) entries else entries.filter { it.category == preferredCategory }
    }
    return matching.firstOrNull { entry ->
        lookupAssetName(entry.name) == normalizedName ||
            entry.aliases.any { lookupAssetName(it) == normalizedName }
    } ?: if (preferredCategory != null) {
        catalog.values.firstOrNull { entry ->
            lookupAssetName(entry.name) == normalizedName ||
                entry.aliases.any { lookupAssetName(it) == normalizedName }
        }
    } else null
}

private fun lookupImageUrl(
    payload: JSONObject,
    catalog: Map<Int, SupplementalCatalogEntry>,
    parentKey: String
): String {
    lookupCatalogEntry(payload, catalog, parentKey)?.imageUrl
        ?.takeIf { it.startsWith("https://", ignoreCase = true) }
        ?.let { return it }

    val directKeys = listOf("imageUrl", "image_url", "image", "badge_url", "badge", "iconUrl", "icon_url", "icon", "avatarUrl", "avatar_url")
    directKeys.firstNotNullOfOrNull { key ->
        payload.lookupText(key).takeIf { it.startsWith("https://", ignoreCase = true) }
    }?.let { return it }

    for (key in listOf("iconUrls", "badgeUrls", "icons", "images")) {
        val nested = payload.optJSONObject(key) ?: continue
        listOf("medium", "large", "small", "tiny").firstNotNullOfOrNull { size ->
            nested.lookupText(size).takeIf { it.startsWith("https://", ignoreCase = true) }
        }?.let { return it }
    }
    return ""
}

private fun lookupBuildingImageUrl(key: String, level: String): String {
    val normalized = key.lowercase().replace(Regex("[_\\s-]"), "")
    val levelNumber = level.toIntOrNull()?.takeIf { it > 0 } ?: return ""

    fun image(sectionKey: String, sectionLabel: String, base: String, dataId: Int): String {
        val item = VillageItem(
            sectionKey = sectionKey,
            sectionLabel = sectionLabel,
            tab = "buildings",
            base = base,
            dataId = dataId,
            level = levelNumber,
            count = 1,
            timerSeconds = null,
            helperCooldownSeconds = null,
            gearUp = false,
            weaponLevel = null,
            details = emptyList()
        )
        return CocAssets.imageUrl(item).orEmpty()
    }

    return when (normalized) {
        "townhalllevel", "townhall" -> image("buildings", "Nhà chính", "home", 1000001)
        "builderhalllevel", "builderhall" -> image("buildings2", "Nhà thợ xây", "builder", 1000034)
        else -> ""
    }
}

internal fun lookupGameIconResource(key: String): Int? {
    val normalized = key.lowercase().replace(Regex("[_\\s-]"), "")
    return when (normalized) {
        "townhalllevel", "townhall" -> R.drawable.ic_coc_buildings
        "builderhalllevel", "builderhall", "builderbase", "builderbasevillage" -> R.drawable.ic_coc_home
        "explevel", "experiencelevel", "playerlevel", "rank", "globalrank", "localrank", "grouprank",
        "trophies", "leaguetrophies", "builderbasetrophies", "buildertrophies", "stars", "attacktrophies", "defensetrophies" -> R.drawable.ic_coc_star
        "clanlevel", "clan", "clantag", "clancapital" -> R.drawable.ic_coc_clan
        "membercount", "members" -> R.drawable.ic_coc_members
        "leaguetier", "league", "warleague", "capitalleague", "cwlleagueid", "leaguetierid", "leaguegroupid" -> R.drawable.ic_coc_star
        "warwins", "warlosses", "warties", "warwinstreak", "attackwins", "defensewins",
        "attacktriples", "defensetriples", "attacks", "wins", "losses", "ties" -> R.drawable.ic_coc_swords
        "destructionpercentage", "triplerate", "perfectdayrate", "share" -> R.drawable.ic_coc_percent
        "donated", "received", "troopsdonated", "troopsreceived", "troops", "supertroops", "siege", "siegemachines" -> R.drawable.ic_coc_guardian
        "capitalgoldtotal", "capitalgold", "capitalpoints" -> R.drawable.ic_coc_capital_gold
        "gold", "goldloot", "goldstorage", "goldmine" -> R.drawable.ic_coc_gold
        "elixir", "elixirloot", "elixirstorage" -> R.drawable.ic_coc_elixir
        "darkelixir", "darkelixirloot" -> R.drawable.ic_coc_dark_elixir
        "shinyore" -> R.drawable.ic_coc_ore_shiny
        "glowyore" -> R.drawable.ic_coc_ore_glowy
        "starryore" -> R.drawable.ic_coc_ore_starry
        "hero", "heroes" -> R.drawable.ic_coc_hero
        "spell", "spells" -> R.drawable.ic_coc_spell
        "pet", "pets" -> R.drawable.ic_coc_pet
        "equipment", "heroequipment" -> R.drawable.ic_coc_equipment
        "defense", "defenses", "defence" -> R.drawable.ic_coc_defense
        "trap", "traps" -> R.drawable.ic_coc_trap
        "wall", "walls" -> R.drawable.ic_coc_wall
        "tag", "playertag" -> R.drawable.ic_coc_tag
        "time", "duration", "battletime", "starttime", "endtime", "updatedat" -> R.drawable.ic_coc_time
        "homevillage" -> R.drawable.ic_coc_buildings
        else -> null
    }
}

internal fun formatLookupCount(value: String): String {
    val number = value.trim().toLongOrNull() ?: return value
    return NumberFormat.getIntegerInstance(Locale("vi", "VN")).format(number)
}

internal fun lookupDisplayValue(key: String, value: Any?): String {
    val raw = lookupValue(value)
    if (raw.isBlank() || raw == "—") return raw
    val normalized = key.lowercase().replace("_", "").replace("-", "")

    val percentageKeys = setOf(
        "destructionpercentage", "destruction", "averagedestruction",
        "averageoffense", "averagedefense"
    )
    if (normalized in percentageKeys) {
        val number = raw.toDoubleOrNull() ?: return raw
        val formatted = NumberFormat.getNumberInstance(Locale("vi", "VN")).apply {
            maximumFractionDigits = 2
            minimumFractionDigits = 0
        }.format(number)
        return "$formatted%"
    }

    if (normalized in setOf("triplerate", "perfectdayrate", "share")) {
        val number = raw.toDoubleOrNull() ?: return raw
        val percent = if (number in 0.0..1.0) number * 100.0 else number
        val formatted = NumberFormat.getNumberInstance(Locale("vi", "VN")).apply {
            maximumFractionDigits = 2
            minimumFractionDigits = 0
        }.format(percent)
        return "$formatted%"
    }

    if (normalized.endsWith("seconds") || normalized == "duration" || normalized == "averageduration") {
        raw.toLongOrNull()?.let { seconds ->
            val safeSeconds = seconds.coerceAtLeast(0L)
            val days = safeSeconds / 86_400
            val hours = (safeSeconds % 86_400) / 3_600
            val minutes = (safeSeconds % 3_600) / 60
            val remainder = safeSeconds % 60
            return when {
                days > 0 -> "$days ngày" + if (hours > 0) " $hours giờ" else ""
                hours > 0 -> "$hours giờ" + if (minutes > 0) " $minutes phút" else ""
                minutes > 0 -> "$minutes phút" + if (remainder > 0) " $remainder giây" else ""
                else -> "$remainder giây"
            }
        }
    }

    val identifierKeys = setOf(
        "id", "dataid", "unitid", "troopid", "heroid", "spellid", "petid",
        "equipmentid", "buildingid", "itemid", "seasonid", "leaguetierid",
        "leaguegroupid", "cwlleagueid", "leagueid", "locationid", "warid",
        "timestamp", "battletime", "tag", "playertag", "clantag", "wartag"
    )
    if (normalized !in identifierKeys) raw.toLongOrNull()?.let { return formatLookupCount(raw) }
    return raw
}

private data class LookupMiniStat(
    val label: String,
    val value: String,
    val iconRes: Int,
    val imageUrl: String = ""
)

private fun lookupMiniStats(entry: ClashLookupEntry): List<LookupMiniStat> {
    fun stat(label: String, value: String, icon: Int, image: String = "") =
        value.takeIf(String::isNotBlank)?.let { LookupMiniStat(label, formatLookupCount(it), icon, image) }
    return if (entry.isClan) {
        listOfNotNull(
            stat("CẤP BANG", entry.level, R.drawable.ic_coc_clan),
            stat("THÀNH VIÊN", entry.members, R.drawable.ic_coc_members),
            stat("XẾP HẠNG", entry.rank, R.drawable.ic_coc_star),
            stat("CHUỖI THẮNG", entry.streak, R.drawable.ic_coc_swords)
        )
    } else {
        listOfNotNull(
            stat("CẤP NGƯỜI CHƠI", entry.playerLevel, R.drawable.ic_coc_star),
            stat("NHÀ CHÍNH", entry.townHall, R.drawable.ic_coc_buildings, lookupBuildingImageUrl("townHallLevel", entry.townHall)),
            stat("NHÀ THỢ XÂY", entry.builderHall, R.drawable.ic_coc_home, lookupBuildingImageUrl("builderHallLevel", entry.builderHall)),
            stat("CÚP", entry.trophies, R.drawable.ic_coc_star),
            stat("CÚP NHÀ THỢ XÂY", entry.builderTrophies, R.drawable.ic_coc_home),
            stat("GIẢI ĐẤU", entry.league, R.drawable.ic_coc_star, entry.leagueIconUrl)
        )
    }
}

@Composable
private fun LookupEntityAvatar(entry: ClashLookupEntry, size: Dp) {
    val imageUrl = if (entry.isClan) {
        entry.badgeUrl
    } else {
        lookupBuildingImageUrl("townHallLevel", entry.townHall)
    }
    Box(
        modifier = Modifier
            .size(size)
            .clip(if (entry.isClan) RoundedCornerShape(14.dp) else androidx.compose.foundation.shape.CircleShape)
            .background((if (entry.isClan) LookupGold else LookupCyan).copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
    ) {
        when {
            imageUrl.startsWith("https://", ignoreCase = true) -> AsyncImage(
                model = imageUrl,
                contentDescription = if (entry.isClan) "Huy hiệu bang hội" else "Nhà chính cấp ${entry.townHall}",
                modifier = Modifier.fillMaxSize().padding(4.dp),
                contentScale = ContentScale.Fit
            )
            entry.isClan -> CocGameIcon(R.drawable.ic_coc_clan, Modifier.size(size * 0.58f), "Bang hội")
            else -> CocGameIcon(R.drawable.ic_coc_player, Modifier.size(size * 0.62f), "Người chơi")
        }
    }
}

@Composable
private fun LookupGameFieldIcon(
    key: String,
    imageUrl: String = "",
    value: String = "",
    modifier: Modifier = Modifier.size(18.dp)
) {
    val gameImage = imageUrl.takeIf { it.startsWith("https://", ignoreCase = true) }
        ?: lookupBuildingImageUrl(key, value).takeIf(String::isNotBlank)
    if (gameImage != null) {
        AsyncImage(model = gameImage, contentDescription = humanizeLookupKey(key), modifier = modifier, contentScale = ContentScale.Fit)
    } else {
        lookupGameIconResource(key)?.let { icon ->
            CocGameIcon(icon, modifier, humanizeLookupKey(key))
        }
    }
}

@Composable
private fun LookupMiniStatChip(stat: LookupMiniStat) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f))
            .padding(horizontal = 7.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (stat.imageUrl.startsWith("https://", ignoreCase = true)) {
            AsyncImage(model = stat.imageUrl, contentDescription = null, modifier = Modifier.size(15.dp), contentScale = ContentScale.Fit)
        } else {
            CocGameIcon(stat.iconRes, Modifier.size(15.dp), stat.label)
        }
        Text("${stat.label}: ${stat.value}", fontSize = 8.5.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun LookupEntryQuickStats(entry: ClashLookupEntry) {
    val stats = lookupMiniStats(entry)
    if (stats.isNotEmpty()) {
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            stats.forEach { LookupMiniStatChip(it) }
        }
    }
}

private fun humanizeLookupKey(key: String): String {
    val known = mapOf(
        "tag" to "TAG / ID",
        "name" to "Tên",
        "clanTag" to "TAG bang hội",
        "playerTag" to "TAG người chơi",
        "leagueGroupId" to "TAG nhóm xếp hạng",
        "seasonId" to "Mã mùa giải",
        "leagueTierId" to "Mã bậc giải",
        "cwlLeagueId" to "Mã giải CWL",
        "townHallLevel" to "Nhà chính",
        "townhallLevel" to "Nhà chính",
        "builderHallLevel" to "Nhà thợ xây",
        "builderHall" to "Nhà thợ xây",
        "expLevel" to "Cấp người chơi",
        "experienceLevel" to "Cấp người chơi",
        "playerLevel" to "Cấp người chơi",
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
        "destructionPercentage" to "Tỷ lệ phá hủy",
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
        "averageOffense" to "Tỷ lệ phá hủy tấn công trung bình",
        "averageDefense" to "Tỷ lệ phá hủy phòng thủ trung bình",
        "clanPoints" to "Điểm bang hội",
        "builderBasePoints" to "Điểm Nhà thợ xây",
        "capitalGold" to "Vàng Thủ phủ",
        "clanCapital" to "Thủ phủ bang hội",
        "homeVillage" to "Làng chính",
        "builderBase" to "Nhà thợ xây",
        "builderBaseTrophies" to "Cúp Nhà thợ xây",
        "builderTrophies" to "Cúp Nhà thợ xây",
        "warSize" to "Quy mô đội",
        "warsFinished" to "Số hội chiến đã kết thúc",
        "totalClansInGroup" to "Tổng số bang hội trong nhóm",
        "groupRank" to "Hạng trong nhóm",
        "updatedAt" to "Cập nhật lúc",
        "destruction" to "Tỷ lệ phá hủy",
        "wins" to "Thắng",
        "losses" to "Thua",
        "ties" to "Hòa",
        "attacks" to "Số lượt tấn công",
        "defenses" to "Số lượt phòng thủ",
        "averageDuration" to "Thời lượng trung bình",
        "averageDestruction" to "Phá hủy trung bình",
        "currentDay" to "Ngày hiện tại",
        "eligiblePlayerDays" to "Số ngày người chơi đủ điều kiện",
        "observedPlayerDays" to "Số ngày đã ghi nhận",
        "completePlayerDays" to "Số ngày đủ dữ liệu",
        "partialPlayerDays" to "Số ngày thiếu một phần dữ liệu",
        "missingPlayerDays" to "Số ngày chưa có dữ liệu",
        "invalidPlayerDays" to "Số ngày dữ liệu không hợp lệ",
        "observedBattles" to "Số trận đã ghi nhận",
        "observedTriples" to "Số trận 3 sao đã ghi nhận",
        "perfectPlayerDays" to "Số ngày đạt hoàn hảo",
        "tripleRate" to "Tỷ lệ 3 sao",
        "perfectDayRate" to "Tỷ lệ ngày hoàn hảo",
        "meanTrophies" to "Cúp trung bình",
        "cohort" to "Nhóm thống kê",
        "mode" to "Chế độ",
        "items" to "Danh sách dữ liệu",
        "count" to "Số lượng",
        "limit" to "Giới hạn",
        "available" to "Số bản ghi hiện có",
        "uniquePlayers" to "Số người chơi khác nhau",
        "clan" to "Bang hội",
        "opponent" to "Đối thủ",
        "player" to "Người chơi",
        "attackerTag" to "TAG người tấn công",
        "attackerName" to "Tên người tấn công",
        "defenderTag" to "TAG người phòng thủ",
        "defenderName" to "Tên người phòng thủ",
        "attackerTownhall" to "Nhà chính người tấn công",
        "defenderTownhall" to "Nhà chính người phòng thủ",
        "mapPosition" to "Vị trí bản đồ",
        "attackOrder" to "Thứ tự tấn công",
        "duration" to "Thời lượng",
        "location" to "Khu vực",
        "description" to "Mô tả",
        "publicWarLog" to "Nhật ký hội chiến công khai",
        "builder_base_points" to "Điểm Nhà thợ xây",
        "builder_base_trophies" to "Cúp Nhà thợ xây",
        "clan_home_points" to "Điểm làng chính",
        "clan_builder_base_points" to "Điểm Nhà thợ xây",
        "clan_capital_points" to "Điểm Thủ phủ",
        "league_id" to "Mã bậc giải",
        "location_id" to "Mã khu vực",
        "war_size" to "Quy mô đội",
        "clan_tag" to "TAG bang hội",
        "last_attack_time" to "Thời điểm tấn công gần nhất",
        "streak" to "Chuỗi trận",
        "end_time" to "Thời điểm kết thúc",
        "team_size" to "Quy mô đội",
        "players_in_war" to "Người chơi đang hội chiến",
        "clans_in_war" to "Bang hội đang hội chiến",
        "total_join_leaves" to "Tổng lượt vào và rời bang",
        "players_in_legends" to "Người chơi ở giải Huyền thoại",
        "player_count" to "Tổng người chơi đã ghi nhận",
        "clan_count" to "Tổng bang hội đã ghi nhận",
        "wars_stored" to "Số hội chiến đã lưu",
        "badgeUrls" to "Đường dẫn huy hiệu",
        "badgeToken" to "Mã huy hiệu",
        "state" to "Trạng thái",
        "rounds" to "Các vòng đấu",
        "warTags" to "Mã hội chiến",
        "starCounts" to "Số trận theo sao",
        "zero" to "Không sao",
        "one" to "Một sao",
        "two" to "Hai sao",
        "three" to "Ba sao",
        "day" to "Ngày",
        "players" to "Người chơi",
        "heroes" to "Anh hùng",
        "pets" to "Thú cưng",
        "equipment" to "Trang bị",
        "petAssignments" to "Ghép thú cưng với anh hùng",
        "troops" to "Quân lính",
        "spells" to "Thần chú",
        "sieges" to "Máy công thành",
        "equipmentPairs" to "Cặp trang bị",
        "petCombos" to "Tổ hợp thú cưng",
        "attackTypes" to "Loại đội hình tấn công",
        "air" to "Không quân",
        "ground" to "Mặt đất",
        "mixed" to "Hỗn hợp",
        "unknown" to "Chưa phân loại",
        "offense" to "Tấn công",
        "battles" to "Trận đấu",
        "triples" to "Trận 3 sao",
        "buckets" to "Các khoảng thống kê",
        "minimum" to "Giá trị nhỏ nhất",
        "maximum" to "Giá trị lớn nhất",
        "playerDays" to "Số ngày người chơi",
        "share" to "Tỷ lệ",
        "points" to "Điểm",
        "warTag" to "TAG hội chiến",
        "warStartTime" to "Thời điểm bắt đầu hội chiến",
        "attacksPerMember" to "Số lượt đánh mỗi thành viên",
        "battleModifier" to "Điều kiện hội chiến",
        "battleType" to "Loại trận đấu",
        "gameMode" to "Chế độ trò chơi",
        "result" to "Kết quả",
        "previousRank" to "Thứ hạng trước",
        "previousDayRank" to "Thứ hạng ngày trước",
        "dailyRank" to "Thứ hạng trong ngày",
        "rankChange" to "Thay đổi thứ hạng",
        "clanName" to "Tên bang hội",
        "opponentTag" to "TAG đối thủ",
        "opponentName" to "Tên đối thủ",
        "attacksPerDay" to "Lượt đánh mỗi ngày",
        "from" to "Từ thời điểm",
        "until" to "Đến thời điểm",
        "asOf" to "Dữ liệu tính đến",
        "observations" to "Số lần ghi nhận",
        "leagueId" to "Mã giải đấu",
        "totalStars" to "Tổng số sao",
        "totalDestruction" to "Tổng tỷ lệ phá hủy",
        "attackSeconds" to "Thời lượng tấn công (giây)",
        "sessionSeconds" to "Thời lượng phiên chơi (giây)",
        "attack_count" to "Số lượt tấn công"
    )
    return known[key] ?: key
        .replace(Regex("([a-z])([A-Z])"), "$1 $2")
        .replace('_', ' ')
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
}

private fun httpErrorMessage(status: Int, detail: String): String {
    val cause = detail.lowercase()
    return when {
        status == 400 && cause.contains("cursor") -> "Trang kết quả đã hết hạn hoặc không còn khớp với bộ lọc. Hãy tìm kiếm lại từ đầu."
        status == 400 -> "Tham số tra cứu chưa hợp lệ. Hãy kiểm tra TAG, mã giải, mùa giải và khoảng ngày."
        status == 401 -> "Dịch vụ yêu cầu xác thực tài khoản ClashKing cho dữ liệu này."
        status == 403 -> "Tài khoản hiện không có quyền xem dữ liệu này."
        status == 404 -> "Không tìm thấy dữ liệu phù hợp hoặc ClashKing chưa lưu hồ sơ."
        status == 429 -> "Bạn gửi yêu cầu quá nhanh. Hãy thử lại sau."
        status in 500..599 -> "Dịch vụ ClashKing đang gặp sự cố tạm thời. Hãy thử lại sau. (HTTP $status)"
        else -> "Dịch vụ tra cứu trả về lỗi HTTP $status."
    }
}

private fun lookupErrorMessage(error: Throwable): String {
    val message = error.message.orEmpty()
    val normalized = message.lowercase()
    return when {
        error is IllegalArgumentException && message.isNotBlank() -> message
        message.startsWith("Không") || message.startsWith("Nhập") || message.startsWith("Trang") ||
            message.startsWith("Bạn") || message.startsWith("Dịch vụ") -> message
        normalized.contains("sockettimeoutexception") || normalized.contains("timed out") ->
            "Kết nối đến ClashKing quá thời gian chờ. Hãy thử lại."
        normalized.contains("unknownhostexception") || normalized.contains("unable to resolve host") ||
            normalized.contains("failed to connect") ->
            "Không thể kết nối máy chủ ClashKing. Hãy kiểm tra mạng rồi thử lại."
        else -> "Không thể tải dữ liệu từ ClashKing. Hãy kiểm tra kết nối rồi thử lại."
    }
}

private fun lookupValue(value: Any?): String = when (value) {
    null, JSONObject.NULL -> "—"
    is JSONObject -> ""
    is JSONArray -> "${value.length()} mục"
    is Boolean -> if (value) "Có" else "Không"
    else -> value.toString().take(2000)
}

@Composable
private fun LookupSearchTargetCard(
    title: String,
    description: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(13.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(
                if (selected) LookupCyan.copy(alpha = 0.14f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.34f)
            )
            .border(
                BorderStroke(
                    if (selected) 1.5.dp else 1.dp,
                    if (selected) LookupCyan else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)
                ),
                shape
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) LookupCyan else LookupMuted,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(8.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    title,
                    color = if (selected) LookupCyan else MaterialTheme.colorScheme.onSurface,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black
                )
                Text(description, color = LookupMuted, fontSize = 8.sp, lineHeight = 11.sp)
            }
        }
    }
}

@Composable
fun ClashLookupScreen() {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var lookupCatalog by remember { mutableStateOf<Map<Int, SupplementalCatalogEntry>>(emptyMap()) }
    LaunchedEffect(context) {
        lookupCatalog = withContext(Dispatchers.IO) { LocalSupplementalCatalog.load(context) }
    }
    var recentSearches by remember(context) { mutableStateOf(loadLookupHistory(context)) }
    var favoriteEntries by remember(context) { mutableStateOf(loadLookupFavorites(context)) }
    var comparedEntryKeys by remember { mutableStateOf(emptyList<String>()) }
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
    var analyticsInterval by rememberSaveable { mutableStateOf("week") }
    var analyticsSort by rememberSaveable { mutableStateOf("usage") }
    var analyticsLimit by rememberSaveable { mutableStateOf("100") }
    var analyticsRankLimit by rememberSaveable { mutableStateOf("") }
    var analyticsGroupKey by rememberSaveable { mutableStateOf("") }
    var analyticsVariantKey by rememberSaveable { mutableStateOf("") }
    var analyticsEqualTownHalls by rememberSaveable { mutableStateOf("all") }
    var analyticsResult by remember { mutableStateOf<JSONObject?>(null) }
    var analyticsResultTitle by remember { mutableStateOf("DỮ LIỆU THỐNG KÊ") }
    var extendedSeasonId by rememberSaveable { mutableStateOf("") }
    var extendedLegendDay by rememberSaveable { mutableStateOf("") }
    var extendedOtherPlayerTag by rememberSaveable { mutableStateOf("") }
    var extendedWarEndTime by rememberSaveable { mutableStateOf("") }
    var query by rememberSaveable { mutableStateOf("") }
    var effectiveSearchQuery by rememberSaveable { mutableStateOf("") }
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

    fun toggleFavorite(entry: ClashLookupEntry) {
        favoriteEntries = toggleLookupFavorite(context, entry)
    }

    fun toggleCompare(entry: ClashLookupEntry) {
        val key = lookupEntryKey(entry)
        comparedEntryKeys = if (key in comparedEntryKeys) {
            comparedEntryKeys.filterNot { it == key }
        } else if (comparedEntryKeys.size < 2) {
            comparedEntryKeys + key
        } else {
            comparedEntryKeys.drop(1) + key
        }
    }

    fun resetSearchPage() {
        comparedEntryKeys = emptyList()
        results = emptyList()
        nextCursor = null
        effectiveSearchQuery = ""
        searchError = null
    }

    fun clearAnalyticsResult() {
        analyticsResult = null
        searchError = null
    }

    fun selectRankingCategory(category: String) {
        rankingCategory = category
        rankingValue = when (category) {
            "townhall", "war_triples" -> "19"
            "league" -> "29000000"
            "clan_cwl", "clan_win_streak" -> ""
            else -> "32000006"
        }
        resetSearchPage()
    }

    fun selectAnalyticsCategory(category: String) {
        analyticsCategory = category
        analyticsPrimary = ""
        analyticsSecondary = ""
        analyticsDate = ""
        analyticsFrom = ""
        analyticsTo = ""
        analyticsGroupKey = ""
        analyticsVariantKey = ""
        clearAnalyticsResult()
    }

    fun clearExtendedResults() {
        detailPanels = emptyList()
        detailError = null
        detailLoading = false
    }

    fun currentExtendedKey(entry: ClashLookupEntry): String = listOf(
        entry.tag, entry.isClan.toString(), extendedSeasonId, extendedLegendDay,
        extendedOtherPlayerTag, extendedWarEndTime
    ).joinToString("\u0000")

    fun currentSearchKey(): String = listOf(
        searchMode, query.trim(), clanTags, leagueIds, townhallLevels,
        rankingCategory, rankingValue, rankingSeason, rankingTeamSize,
        analyticsCategory, analyticsPrimary, analyticsSecondary, analyticsDate, analyticsFrom, analyticsTo,
        analyticsLeaderboardType, analyticsMode, analyticsCohort, analyticsInterval, analyticsSort,
        analyticsLimit, analyticsRankLimit, analyticsEqualTownHalls, analyticsGroupKey, analyticsVariantKey
    ).joinToString("\u0000")

    val filters = LookupFilters(
        clanTags = clanTags,
        leagueIds = leagueIds,
        townhallLevels = townhallLevels
    )

    fun runSearch(loadMore: Boolean = false) {
        val term = query.trim()
        val isLeaderboard = searchMode == "leaderboard"
        val isAnalytics = searchMode == "analytics"
        if (!isLeaderboard && !isAnalytics && term.length < 2) {
            searchError = "Nhập ít nhất 2 ký tự tên hoặc TAG/ID cần tìm."
            return
        }
        if (!isLeaderboard && !isAnalytics && term.length > 100) {
            searchError = "Từ khóa tìm kiếm không được dài quá 100 ký tự."
            return
        }
        if (isLeaderboard && rankingCategory != "clan_win_streak") {
            val supportsGlobal = rankingCategory == "clan_donations" || rankingCategory == "clan_war_wins"
            val isValidGlobal = supportsGlobal && rankingValue.trim().equals("global", ignoreCase = true)
            val rankingId = rankingValue.toIntOrNull()
            if (!isValidGlobal && (rankingId == null || rankingId <= 0)) {
                searchError = if (rankingCategory == "clan_cwl") "Nhập mã bậc giải CWL hợp lệ."
                    else "Nhập cấp Nhà chính, mã khu vực hoặc mã giải đấu hợp lệ."
                return
            }
        }
        if (isSearching) return
        if (!loadMore) recentSearches = recordLookupHistory(context, searchMode, term)
        val oldCursor = if (loadMore) nextCursor else null
        // Never apply saved advanced filters while their controls are hidden.
        val selectedFilters = if (showAdvancedFilters && searchMode == "player") filters else LookupFilters()
        val searchCandidates = if (loadMore) {
            listOf(effectiveSearchQuery.ifBlank { term })
        } else {
            lookupSearchCandidates(term)
        }
        val isClanLeaderboard = isLeaderboard && rankingCategory.startsWith("clan_")
        val requestKey = currentSearchKey()
        val requestAnalyticsCategory = analyticsCategory
        scope.launch {
            isSearching = true
            searchError = null
            if (!loadMore) {
                comparedEntryKeys = emptyList()
                results = emptyList()
                nextCursor = null
                analyticsResult = null
            }
            try {
                if (isAnalytics) {
                    val path = ClashKingLookupApi.analyticsPath(
                        analyticsCategory, analyticsPrimary, analyticsSecondary, analyticsDate, analyticsFrom, analyticsTo,
                        analyticsLeaderboardType, analyticsMode, analyticsCohort, analyticsInterval, analyticsSort,
                        analyticsLimit, analyticsRankLimit, analyticsEqualTownHalls, analyticsGroupKey, analyticsVariantKey
                    )
                    val response = ClashKingLookupApi.get(path)
                    if (currentSearchKey() == requestKey) {
                        analyticsResult = response
                        analyticsResultTitle = when (requestAnalyticsCategory) {
                        "global_counts" -> "THỐNG KÊ TỔNG QUAN CLASHKING"
                        "counts_townhalls" -> "PHÂN BỐ NHÀ CHÍNH"
                        "counts_league_tiers" -> "PHÂN BỐ GIẢI NGƯỜI CHƠI"
                        "counts_clan_locations" -> "PHÂN BỐ KHU VỰC BANG HỘI"
                        "counts_cwl_leagues" -> "PHÂN BỐ GIẢI CWL"
                        "counts_capital_leagues" -> "PHÂN BỐ GIẢI THỦ PHỦ"
                        "counts_clan_members" -> "PHÂN BỐ QUY MÔ BANG HỘI"
                        "stats_ranked" -> "THỐNG KÊ TẤN CÔNG XẾP HẠNG"
                        "stats_war" -> "THỐNG KÊ HỘI CHIẾN THƯỜNG"
                        "stats_cwl" -> "THỐNG KÊ THAM GIA CWL"
                        "wars_hit_rates" -> "TỶ LỆ SAO HỘI CHIẾN"
                        "wars_summary" -> "TỔNG KẾT HỘI CHIẾN"
                        "army_setups" -> "ĐỘI HÌNH HUYỀN THOẠI PHỔ BIẾN"
                        "army_setup_timeline" -> "LỊCH SỬ ĐỘI HÌNH HUYỀN THOẠI"
                        "army_setup_rank_history" -> "LỊCH SỬ XẾP HẠNG ĐỘI HÌNH"
                        "legend_rank_curve" -> "ĐƯỜNG CONG XẾP HẠNG HUYỀN THOẠI"
                        "legend_trophy_buckets" -> "PHÂN BỐ CÚP HUYỀN THOẠI"
                        "historical_leaderboard" -> "BẢNG XẾP HẠNG LỊCH SỬ"
                        "trophy_buckets" -> "PHÂN BỐ CÚP THEO GIẢI ĐẤU"
                        "legend_season" -> "DỮ LIỆU HUYỀN THOẠI THEO MÙA"
                        "ranked_group" -> "NHÓM XẾP HẠNG"
                        "league_tier_statistics" -> "THỐNG KÊ HẠNG GIẢI XẾP HẠNG"
                        "league_hit_rates" -> "TỶ LỆ SAO XẾP HẠNG / HUYỀN THOẠI"
                        "legend_days" -> "THỐNG KÊ CHI TIẾT HUYỀN THOẠI"
                        else -> "PHÂN TÍCH HIỆU SUẤT HUYỀN THOẠI"
                        }
                        results = emptyList()
                        nextCursor = null
                    }
                } else {
                    if (isLeaderboard) {
                        val response = ClashKingLookupApi.get(ClashKingLookupApi.leaderboardPath(rankingCategory, rankingValue, rankingSeason, rankingTeamSize))
                        if (currentSearchKey() == requestKey) {
                            results = lookupItems(response, isClanLeaderboard)
                            nextCursor = null
                        }
                    } else {
                        val isClanSearch = searchMode == "clan"
                        var found = emptyList<ClashLookupEntry>()
                        var foundCursor: String? = null
                        var usedQuery = searchCandidates.firstOrNull().orEmpty()
                        for (candidate in searchCandidates) {
                            val response = if (isClanSearch) {
                                ClashKingLookupApi.get(ClashKingLookupApi.clanSearchPath(candidate, oldCursor))
                            } else {
                                ClashKingLookupApi.get(ClashKingLookupApi.playerSearchPath(candidate, oldCursor, selectedFilters))
                            }
                            found = lookupItems(response, isClanSearch)
                            usedQuery = candidate
                            if (found.isNotEmpty() || loadMore || candidate == searchCandidates.lastOrNull()) {
                                foundCursor = response.optJSONObject("pagination")
                                    ?.lookupText("nextCursor")
                                    ?.takeIf { it.isNotBlank() }
                                break
                            }
                        }
                        if (found.isEmpty() && isClanSearch && !loadMore) {
                            val directTag = runCatching { normalizeLookupTag(term, "TAG bang hội") }.getOrNull()
                            if (directTag != null) {
                                val cached = ClashKingLookupApi.get(ClashKingLookupApi.cachedClanPath(directTag))
                                val profile = cached.optJSONObject("clan") ?: cached
                                if (profile.lookupText("tag").isNotBlank()) {
                                    found = listOf(lookupEntry(profile, isClan = true))
                                    foundCursor = null
                                }
                            }
                        }
                        if (currentSearchKey() == requestKey) {
                            results = if (loadMore) deduplicateLookupEntries(results + found) else deduplicateLookupEntries(found)
                            nextCursor = foundCursor
                            effectiveSearchQuery = usedQuery
                        }
                    }
                    if (currentSearchKey() == requestKey && results.isEmpty()) searchError = "Không tìm thấy kết quả. Kiểm tra ID/TAG, tên và chế độ Người chơi/Bang hội. Bộ lọc nâng cao chỉ có hiệu lực khi đang mở."
                }
            } catch (error: Exception) {
                if (currentSearchKey() == requestKey) searchError = lookupErrorMessage(error)
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
        val loaded = mutableListOf<LookupPanel>()
        requests.chunked(3).forEach { batch ->
            val batchPanels = batch.map { request ->
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
                        LookupPanel(request.title, null, lookupErrorMessage(error))
                    }
                }
            }.awaitAll()
            loaded.addAll(batchPanels)
            detailPanels = loaded.toList()
        }
        val completed = loaded.toList()
        val failures = completed.filter { it.payload == null }
        detailError = failures.takeIf { it.isNotEmpty() }?.joinToString("\n") { "• ${it.title}: ${it.error}" }
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
                        clearExtendedResults()
                        selectedEntry = null
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
                IconButton(onClick = { toggleFavorite(entry) }) {
                    Icon(
                        if (favoriteEntries.any { it.tag.equals(entry.tag, true) && it.isClan == entry.isClan }) Icons.Outlined.Star
                        else Icons.Outlined.StarBorder,
                        contentDescription = if (favoriteEntries.any { it.tag.equals(entry.tag, true) && it.isClan == entry.isClan }) "Bỏ ghim hồ sơ" else "Ghim hồ sơ",
                        tint = LookupGold
                    )
                }
                IconButton(onClick = { shareLookupEntry(context, entry) }) {
                    Icon(Icons.Outlined.Share, contentDescription = "Chia sẻ hồ sơ", tint = LookupCyan)
                }
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
                    Text("SAO CHÉP TAG", fontSize = 9.sp)
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
                    LookupEntityAvatar(entry, 62.dp)
                    Spacer(Modifier.width(13.dp))
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(entry.name, fontSize = 18.sp, fontWeight = FontWeight.Black, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(
                            "${if (entry.isClan) "TAG BANG HỘI" else "TAG NGƯỜI CHƠI"} · ${entry.tag.ifBlank { "Chưa có TAG" }}",
                            fontSize = 10.sp,
                            color = LookupCyan,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            if (entry.isClan) "HỒ SƠ BANG HỘI" else "HỒ SƠ NGƯỜI CHƠI",
                            fontSize = 9.sp,
                            color = LookupMuted,
                            letterSpacing = 0.8.sp
                        )
                    }
                }
            }

            LookupEntryQuickStats(entry)

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                sections.forEach { section ->
                    FilterChip(
                        selected = selectedSection == section,
                        onClick = { clearExtendedResults(); selectedSection = section },
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
                        Text("API CÔNG KHAI CẦN THAM SỐ", color = LookupGold, fontSize = 10.sp, fontWeight = FontWeight.Black)
                        if (!entry.isClan) {
                            LookupFilterField("Mã mùa giải xếp hạng (để tải nhóm và nhật ký trận)", extendedSeasonId, { extendedSeasonId = it; clearExtendedResults() }, "Mã mùa giải")
                            LookupFilterField("Ngày Huyền thoại (YYYY-MM-DD)", extendedLegendDay, { extendedLegendDay = it; clearExtendedResults() }, "YYYY-MM-DD")
                            LookupFilterField("TAG người chơi khác (để so lịch sử bang)", extendedOtherPlayerTag, { extendedOtherPlayerTag = it; clearExtendedResults() }, "#ABC123")
                            Text("Có thể nhập một hoặc nhiều mục: mã mùa xếp hạng tải nhóm và nhật ký trận; ngày tải nhật ký Huyền thoại; TAG khác dùng để so sánh thời gian hai người cùng ở bang.", fontSize = 9.sp, color = LookupMuted, lineHeight = 13.sp)
                        } else {
                            LookupFilterField("Thời điểm kết thúc hội chiến (YYYYMMDDTHHmmss.SSSZ)", extendedWarEndTime, { extendedWarEndTime = it; clearExtendedResults() }, "YYYYMMDDTHHmmss.SSSZ")
                            Text("Sao chép nguyên endTime từ dữ liệu API của cuộc hội chiến đã lưu; không tự đổi sang định dạng ISO thông thường.", fontSize = 9.sp, color = LookupMuted, lineHeight = 13.sp)
                        }
                        Button(
                            onClick = {
                                try {
                                    val playerTag = URLEncoder.encode(entry.tag.trim().uppercase(), "UTF-8")
                                    val clanTag = URLEncoder.encode(entry.tag.trim().uppercase(), "UTF-8")
                                    val requests = mutableListOf<LookupRequest>()
                                    if (!entry.isClan) {
                                        val season = extendedSeasonId.trim()
                                        val day = extendedLegendDay.trim()
                                        val otherRawTag = extendedOtherPlayerTag.trim()
                                        val otherTag = if (otherRawTag.isBlank()) "" else normalizeLookupTag(otherRawTag, "TAG người chơi khác")
                                        if (season.isNotBlank()) {
                                            require(season.toLongOrNull()?.let { it > 0L } == true) { "Mã mùa giải xếp hạng (Ranked) phải là số dương." }
                                            val encodedSeason = URLEncoder.encode(season, "UTF-8")
                                            requests += LookupRequest("Nhóm xếp hạng của mùa $season", "/v2/player/$playerTag/ranked/$encodedSeason/group")
                                            requests += LookupRequest("Nhật ký trận xếp hạng của mùa $season", "/v2/player/$playerTag/ranked/$encodedSeason/battlelog")
                                        }
                                        if (day.isNotBlank()) {
                                            require(runCatching { LocalDate.parse(day) }.isSuccess) { "Ngày Legend không hợp lệ. Hãy dùng dạng YYYY-MM-DD." }
                                            val encodedDay = URLEncoder.encode(day, "UTF-8")
                                            requests += LookupRequest("Nhật ký trận Huyền thoại ngày $day", "/v2/player/$playerTag/legend/$encodedDay/battlelog")
                                        }
                                        if (otherTag.isNotBlank()) {
                                            val encodedOtherTag = URLEncoder.encode(otherTag, "UTF-8")
                                            requests += LookupRequest("Lịch sử cùng bang với $otherTag", "/v2/player/$playerTag/join-leave/shared?tag=$encodedOtherTag")
                                        }
                                    } else if (extendedWarEndTime.isNotBlank()) {
                                        val validEndTime = validateLookupWarEndTime(extendedWarEndTime)
                                        val encodedEndTime = URLEncoder.encode(validEndTime, "UTF-8")
                                        requests += LookupRequest("Chi tiết hội chiến kết thúc $validEndTime", "/v2/war/$clanTag/previous/$encodedEndTime")
                                    }
                                    if (requests.isEmpty()) {
                                        detailPanels = emptyList()
                                        detailError = if (entry.isClan) "Nhập thời điểm kết thúc hội chiến trước khi tải." else "Nhập ít nhất một tham số để tải dữ liệu."
                                    } else {
                                        val requestKey = currentExtendedKey(entry)
                                        detailPanels = emptyList()
                                        scope.launch {
                                            detailLoading = true
                                            detailError = null
                                            val loaded = mutableListOf<LookupPanel>()
                                            requests.chunked(3).forEach { batch ->
                                                val batchPanels = batch.map { request ->
                                                    async {
                                                        runCatching { LookupPanel(request.title, ClashKingLookupApi.get(request.path)) }
                                                            .getOrElse { error -> LookupPanel(request.title, null, lookupErrorMessage(error)) }
                                                    }
                                                }.awaitAll()
                                                loaded.addAll(batchPanels)
                                                if (selectedEntry?.tag == entry.tag && selectedEntry?.isClan == entry.isClan &&
                                                    selectedSection == "DỮ LIỆU MỞ RỘNG" && currentExtendedKey(entry) == requestKey) {
                                                    detailPanels = loaded.toList()
                                                }
                                            }
                                            if (selectedEntry?.tag == entry.tag && selectedEntry?.isClan == entry.isClan &&
                                                selectedSection == "DỮ LIỆU MỞ RỘNG" && currentExtendedKey(entry) == requestKey) {
                                                val failures = loaded.filter { it.payload == null }
                                                detailError = failures.takeIf { it.isNotEmpty() }?.joinToString("\n") { "• ${it.title}: ${it.error}" }
                                                detailLoading = false
                                            }
                                        }
                                    }
                                } catch (error: Exception) {
                                    detailPanels = emptyList()
                                    detailError = lookupErrorMessage(error)
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
                    LookupDataPanel(title = panel.title, payload = panel.payload, catalog = lookupCatalog)
                } else if (!detailLoading) {
                    LookupMessageCard("${panel.title}: ${panel.error ?: "Chưa có dữ liệu"}")
                }
            }
            Text(
                "Dữ liệu công khai từ ClashKing API. Một số dữ liệu riêng tư cần tài khoản ClashKing đã xác minh hoặc quyền bot phù hợp nên không được gọi ở chế độ tra cứu công khai. Hoạt động và lịch sử chỉ phản ánh những gì API đã ghi nhận.",
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
                Text("CHỌN ĐỐI TƯỢNG TRA CỨU", color = LookupGold, fontSize = 10.sp, fontWeight = FontWeight.Black)
                Text("Chọn đúng loại trước khi tìm. Kết quả Người chơi chỉ hiển thị hồ sơ game thủ; kết quả Bang hội chỉ hiển thị huy hiệu clan.", color = LookupMuted, fontSize = 9.sp, lineHeight = 13.sp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LookupSearchTargetCard(
                        title = "NGƯỜI CHƠI",
                        description = "Tìm game thủ theo tên hoặc TAG",
                        icon = Icons.Outlined.Person,
                        selected = searchMode == "player",
                        onClick = {
                            searchMode = "player"
                            resetSearchPage()
                            analyticsResult = null
                        },
                        modifier = Modifier.weight(1f)
                    )
                    LookupSearchTargetCard(
                        title = "BANG HỘI",
                        description = "Tìm clan theo tên hoặc TAG",
                        icon = Icons.Outlined.Groups,
                        selected = searchMode == "clan",
                        onClick = {
                            searchMode = "clan"
                            resetSearchPage()
                            analyticsResult = null
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    FilterChip(
                        selected = searchMode == "leaderboard",
                        onClick = { searchMode = "leaderboard"; resetSearchPage(); analyticsResult = null },
                        label = { Text("XẾP HẠNG", fontSize = 9.sp, fontWeight = FontWeight.Bold) }
                    )
                    FilterChip(
                        selected = searchMode == "analytics",
                        onClick = { searchMode = "analytics"; resetSearchPage(); analyticsResult = null },
                        label = { Text("THỐNG KÊ MỞ RỘNG", fontSize = 9.sp, fontWeight = FontWeight.Bold) }
                    )
                }

                if (searchMode != "leaderboard" && searchMode != "analytics") OutlinedTextField(
                    value = query,
                    onValueChange = {
                        if (query != it) {
                            query = it
                            resetSearchPage()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(if (searchMode == "player") "TÌM NGƯỜI CHƠI · tên hoặc TAG" else "TÌM BANG HỘI · tên hoặc TAG") },
                    placeholder = {
                        Text(
                            if (searchMode == "player") "Ví dụ: #ABC123 hoặc tên người chơi"
                            else "Ví dụ: #ABC123 hoặc tên bang hội"
                        )
                    },
                    leadingIcon = { Icon(Icons.Outlined.Search, null, tint = LookupCyan) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LookupCyan,
                        focusedLabelColor = LookupCyan,
                        cursorColor = LookupCyan
                    )
                )

                if (searchMode == "leaderboard") {
                    Text("CHỌN BẢNG XẾP HẠNG", color = LookupGold, fontSize = 10.sp, fontWeight = FontWeight.Black)
                    LookupChoiceGroup(
                        "NGƯỜI CHƠI",
                        rankingCategory,
                        listOf("townhall" to "NHÀ CHÍNH", "league" to "GIẢI ĐẤU", "war_triples" to "CHUỖI 3 SAO"),
                        ::selectRankingCategory
                    )
                    LookupChoiceGroup(
                        "BANG HỘI",
                        rankingCategory,
                        listOf(
                            "clan_donations" to "QUYÊN GÓP",
                            "clan_war_wins" to "THẮNG HỘI CHIẾN",
                            "clan_capital_gold" to "VÀNG THỦ PHỦ",
                            "clan_win_streak" to "CHUỖI THẮNG",
                            "clan_cwl" to "CWL"
                        ),
                        ::selectRankingCategory
                    )
                    if (rankingCategory != "clan_win_streak") {
                        val isClanLeaderboard = rankingCategory.startsWith("clan_")
                        val label = when {
                            rankingCategory == "townhall" || rankingCategory == "war_triples" -> "Cấp Nhà chính"
                            rankingCategory == "league" -> "Mã giải đấu"
                            rankingCategory == "clan_cwl" -> "Mã bậc giải CWL"
                            rankingCategory == "clan_donations" || rankingCategory == "clan_war_wins" -> "Mã khu vực hoặc global"
                            else -> "Mã quốc gia / khu vực"
                        }
                        LookupFilterField(
                            label,
                            rankingValue,
                            { rankingValue = it; resetSearchPage() },
                            when {
                                rankingCategory == "townhall" || rankingCategory == "war_triples" -> "19"
                                rankingCategory == "league" -> "29000000"
                                rankingCategory == "clan_cwl" -> "Nhập mã bậc giải CWL"
                                rankingCategory == "clan_donations" || rankingCategory == "clan_war_wins" -> "32000006 hoặc global"
                                else -> "32000006"
                            }
                        )
                        if (rankingCategory == "clan_cwl") {
                            LookupFilterField("Mùa CWL (YYYY-MM)", rankingSeason, { rankingSeason = it; resetSearchPage() }, "YYYY-MM")
                            LookupFilterField("Quy mô đội (15 hoặc 30)", rankingTeamSize, { rankingTeamSize = it; resetSearchPage() }, "15")
                        }
                        Text(
                            when {
                                rankingCategory == "clan_cwl" -> "Bảng xếp hạng CWL cần mã bậc giải, mùa giải và quy mô đội."
                                rankingCategory == "war_triples" -> "Bảng chuỗi 3 sao theo cấp Nhà chính."
                                rankingCategory == "clan_donations" || rankingCategory == "clan_war_wins" ->
                                    "32000006 là khu vực Quốc tế; nhập global để xem bảng xếp hạng toàn cầu."
                                isClanLeaderboard -> "Bảng Vàng Thủ phủ cần mã khu vực; 32000006 là khu vực Quốc tế."
                                else -> "Nhập đúng cấp Nhà chính hoặc mã giải đấu theo dữ liệu ClashKing."
                            },
                            color = LookupMuted,
                            fontSize = 9.sp,
                            lineHeight = 13.sp
                        )
                    } else {
                        Text("Bảng xếp hạng bang hội theo chuỗi thắng hiện tại.", color = LookupMuted, fontSize = 9.sp)
                    }
                }

                if (searchMode == "analytics") {
                    Text("API THỐNG KÊ CÔNG KHAI", color = LookupGold, fontSize = 10.sp, fontWeight = FontWeight.Black)
                    LookupChoiceGroup(
                        "TỔNG QUAN & PHÂN BỐ",
                        analyticsCategory,
                        listOf(
                            "global_counts" to "TỔNG QUAN",
                            "counts_townhalls" to "NHÀ CHÍNH",
                            "counts_league_tiers" to "GIẢI NGƯỜI CHƠI",
                            "counts_clan_locations" to "KHU VỰC BANG",
                            "counts_cwl_leagues" to "GIẢI CWL",
                            "counts_capital_leagues" to "GIẢI THỦ PHỦ",
                            "counts_clan_members" to "QUY MÔ BANG"
                        ),
                        ::selectAnalyticsCategory
                    )
                    LookupChoiceGroup(
                        "HỘI CHIẾN & XẾP HẠNG",
                        analyticsCategory,
                        listOf(
                            "stats_ranked" to "XẾP HẠNG",
                            "stats_war" to "HỘI CHIẾN",
                            "stats_cwl" to "CWL",
                            "wars_hit_rates" to "TỶ LỆ SAO",
                            "wars_summary" to "TỔNG KẾT"
                        ),
                        ::selectAnalyticsCategory
                    )
                    LookupChoiceGroup(
                        "ĐỘI HÌNH",
                        analyticsCategory,
                        listOf(
                            "army_setups" to "ĐỘI HÌNH PHỔ BIẾN",
                            "army_setup_timeline" to "LỊCH SỬ ĐỘI HÌNH",
                            "army_setup_rank_history" to "HẠNG ĐỘI HÌNH"
                        ),
                        ::selectAnalyticsCategory
                    )
                    LookupChoiceGroup(
                        "HUYỀN THOẠI & DỮ LIỆU LỊCH SỬ",
                        analyticsCategory,
                        listOf(
                            "legend_rank_curve" to "XẾP HẠNG HUYỀN THOẠI",
                            "legend_trophy_buckets" to "PHÂN BỐ CÚP HUYỀN THOẠI",
                            "historical_leaderboard" to "BXH LỊCH SỬ",
                            "trophy_buckets" to "PHÂN BỐ CÚP",
                            "legend_season" to "MÙA HUYỀN THOẠI",
                            "ranked_group" to "NHÓM XẾP HẠNG",
                            "league_tier_statistics" to "THỐNG KÊ HẠNG",
                            "league_hit_rates" to "TỶ LỆ SAO",
                            "legend_days" to "HUYỀN THOẠI THEO NGÀY",
                            "legend_performance" to "HIỆU SUẤT HUYỀN THOẠI"
                        ),
                        ::selectAnalyticsCategory
                    )
                    when (analyticsCategory) {
                        "stats_ranked" -> {
                            LookupFilterField("Cấp Nhà chính", analyticsPrimary, { analyticsPrimary = it; clearAnalyticsResult() }, "19")
                            LookupFilterField("Mã bậc giải xếp hạng", analyticsSecondary, { analyticsSecondary = it; clearAnalyticsResult() }, "105000036")
                            LookupFilterField("Từ ngày (YYYY-MM-DD)", analyticsFrom, { analyticsFrom = it; clearAnalyticsResult() }, "YYYY-MM-DD")
                            LookupFilterField("Đến ngày (YYYY-MM-DD)", analyticsTo, { analyticsTo = it; clearAnalyticsResult() }, "YYYY-MM-DD")
                        }
                        "stats_war" -> {
                            LookupFilterField("Nhà chính người tấn công (tùy chọn)", analyticsPrimary, { analyticsPrimary = it; clearAnalyticsResult() }, "19")
                            LookupFilterField("Nhà chính đối thủ (tùy chọn)", analyticsSecondary, { analyticsSecondary = it; clearAnalyticsResult() }, "19")
                            LookupFilterField("Từ ngày (YYYY-MM-DD)", analyticsFrom, { analyticsFrom = it; clearAnalyticsResult() }, "YYYY-MM-DD")
                            LookupFilterField("Đến ngày (YYYY-MM-DD)", analyticsTo, { analyticsTo = it; clearAnalyticsResult() }, "YYYY-MM-DD")
                            Text("Lọc Nhà chính bằng cách nhập cấp; có thể bỏ trống để xem tổng hợp.", fontSize = 9.sp, color = LookupMuted)
                            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                FilterChip(selected = analyticsEqualTownHalls == "all", onClick = { analyticsEqualTownHalls = "all"; clearAnalyticsResult() }, label = { Text("MỌI CẶP", fontSize = 8.sp) })
                                FilterChip(selected = analyticsEqualTownHalls == "true", onClick = { analyticsEqualTownHalls = "true"; clearAnalyticsResult() }, label = { Text("CÙNG NHÀ CHÍNH", fontSize = 8.sp) })
                                FilterChip(selected = analyticsEqualTownHalls == "false", onClick = { analyticsEqualTownHalls = "false"; clearAnalyticsResult() }, label = { Text("KHÁC NHÀ CHÍNH", fontSize = 8.sp) })
                            }
                        }
                        "stats_cwl" -> LookupFilterField("Mùa CWL (tùy chọn, YYYY-MM)", analyticsPrimary, { analyticsPrimary = it; clearAnalyticsResult() }, "YYYY-MM")
                        "wars_hit_rates" -> {
                            LookupFilterField("Nhà chính (tùy chọn)", analyticsPrimary, { analyticsPrimary = it; clearAnalyticsResult() }, "19")
                            LookupFilterField("Từ ngày (YYYY-MM-DD)", analyticsFrom, { analyticsFrom = it; clearAnalyticsResult() }, "YYYY-MM-DD")
                            LookupFilterField("Đến ngày (YYYY-MM-DD)", analyticsTo, { analyticsTo = it; clearAnalyticsResult() }, "YYYY-MM-DD")
                            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf("day" to "NGÀY", "week" to "TUẦN", "month" to "THÁNG").forEach { (value, label) ->
                                    FilterChip(selected = analyticsInterval == value, onClick = { analyticsInterval = value; clearAnalyticsResult() }, label = { Text(label, fontSize = 8.sp) })
                                }
                            }
                        }
                        "wars_summary" -> {
                            LookupFilterField("Quy mô hội chiến (tùy chọn)", analyticsPrimary, { analyticsPrimary = it; clearAnalyticsResult() }, "15")
                            LookupFilterField("Từ ngày (YYYY-MM-DD)", analyticsFrom, { analyticsFrom = it; clearAnalyticsResult() }, "YYYY-MM-DD")
                            LookupFilterField("Đến ngày (YYYY-MM-DD)", analyticsTo, { analyticsTo = it; clearAnalyticsResult() }, "YYYY-MM-DD")
                            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf("day" to "NGÀY", "week" to "TUẦN", "month" to "THÁNG").forEach { (value, label) ->
                                    FilterChip(selected = analyticsInterval == value, onClick = { analyticsInterval = value; clearAnalyticsResult() }, label = { Text(label, fontSize = 8.sp) })
                                }
                            }
                        }
                        "army_setups", "army_setup_timeline", "army_setup_rank_history" -> {
                            if (analyticsCategory != "army_setups") {
                                LookupFilterField("Mã nhóm đội hình", analyticsGroupKey, { analyticsGroupKey = it; clearAnalyticsResult() }, "Lấy từ kết quả Đội hình Huyền thoại")
                                if (analyticsCategory == "army_setup_rank_history") {
                                    LookupFilterField("Mã biến thể (bỏ trống để xem đội hình chính)", analyticsVariantKey, { analyticsVariantKey = it; clearAnalyticsResult() }, "Có thể để trống")
                                }
                            }
                            LookupFilterField("Từ ngày (YYYY-MM-DD)", analyticsFrom, { analyticsFrom = it; clearAnalyticsResult() }, "YYYY-MM-DD")
                            LookupFilterField("Đến ngày (YYYY-MM-DD)", analyticsTo, { analyticsTo = it; clearAnalyticsResult() }, "YYYY-MM-DD")
                            if (analyticsCategory == "army_setups") {
                                LookupFilterField("Giới hạn số đội hình (tối đa 200)", analyticsLimit, { analyticsLimit = it; clearAnalyticsResult() }, "100")
                            }
                            LookupFilterField("Giới hạn thứ hạng (200 hoặc 1000; để trống xem toàn bộ)", analyticsRankLimit, { analyticsRankLimit = it; clearAnalyticsResult() }, "200 hoặc 1000")
                            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                FilterChip(selected = analyticsSort == "usage", onClick = { analyticsSort = "usage"; clearAnalyticsResult() }, label = { Text("PHỔ BIẾN", fontSize = 8.sp) })
                                FilterChip(selected = analyticsSort == "tripleRate", onClick = { analyticsSort = "tripleRate"; clearAnalyticsResult() }, label = { Text("TỶ LỆ 3 SAO", fontSize = 8.sp) })
                            }
                            Text("Dữ liệu đội hình hiện chỉ có cho Huyền thoại I. Giới hạn thứ hạng chỉ nhận 200 hoặc 1000; bỏ trống để xem toàn bộ.", color = LookupMuted, fontSize = 9.sp)
                        }
                        "legend_rank_curve" -> LookupFilterField("Ngày (YYYY-MM-DD)", analyticsDate, { analyticsDate = it; clearAnalyticsResult() }, "YYYY-MM-DD")
                        "legend_trophy_buckets" -> LookupFilterField("Ngày lịch sử (để trống xem hiện tại)", analyticsDate, { analyticsDate = it; clearAnalyticsResult() }, "YYYY-MM-DD")
                        "historical_leaderboard" -> {
                            Text("Loại bảng xếp hạng", color = LookupMuted, fontSize = 9.sp)
                            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                listOf("player_home_trophies" to "NHÀ CHÍNH", "player_builder_base_trophies" to "NHÀ THỢ XÂY",
                                    "clan_home_points" to "ĐIỂM BANG HỘI", "clan_builder_base_points" to "ĐIỂM XÂY",
                                    "clan_capital_points" to "ĐIỂM THỦ PHỦ").forEach { (key, label) ->
                                    FilterChip(selected = analyticsLeaderboardType == key, onClick = { analyticsLeaderboardType = key; clearAnalyticsResult() },
                                        label = { Text(label, fontSize = 8.sp) })
                                }
                            }
                            LookupFilterField("Mã khu vực", analyticsPrimary, { analyticsPrimary = it; clearAnalyticsResult() }, "32000006")
                            LookupFilterField("Ngày snapshot (YYYY-MM-DD)", analyticsDate, { analyticsDate = it; clearAnalyticsResult() }, "YYYY-MM-DD")
                        }
                        "trophy_buckets" -> LookupFilterField("Mã giải đấu", analyticsPrimary, { analyticsPrimary = it; clearAnalyticsResult() }, "29000000")
                        "legend_season" -> {
                            LookupFilterField("Mùa Huyền thoại (YYYY-MM)", analyticsPrimary, { analyticsPrimary = it; clearAnalyticsResult() }, "YYYY-MM")
                            LookupFilterField("Số kết quả tối đa", analyticsSecondary, { analyticsSecondary = it; clearAnalyticsResult() }, "100")
                        }
                        "ranked_group" -> {
                            LookupFilterField("Mã mùa giải", analyticsPrimary, { analyticsPrimary = it; clearAnalyticsResult() }, "Mã mùa giải")
                            LookupFilterField("TAG nhóm xếp hạng", analyticsSecondary, { analyticsSecondary = it; clearAnalyticsResult() }, "#ABC123")
                        }
                        "league_tier_statistics" -> {
                            LookupFilterField("Mã mùa giải", analyticsPrimary, { analyticsPrimary = it; clearAnalyticsResult() }, "Mã mùa giải")
                            LookupFilterField("Mã bậc giải", analyticsSecondary, { analyticsSecondary = it; clearAnalyticsResult() }, "105000036")
                        }
                        "league_hit_rates" -> {
                            LookupFilterField("Từ ngày", analyticsFrom, { analyticsFrom = it; clearAnalyticsResult() }, "YYYY-MM-DD")
                            LookupFilterField("Đến ngày", analyticsTo, { analyticsTo = it; clearAnalyticsResult() }, "YYYY-MM-DD")
                            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                FilterChip(selected = analyticsMode == "ranked", onClick = { analyticsMode = "ranked"; clearAnalyticsResult() }, label = { Text("XẾP HẠNG", fontSize = 8.sp) })
                                FilterChip(selected = analyticsMode == "legend", onClick = { analyticsMode = "legend"; clearAnalyticsResult() }, label = { Text("HUYỀN THOẠI", fontSize = 8.sp) })
                            }
                            if (analyticsMode == "ranked") {
                                LookupFilterField("Mã bậc giải (tùy chọn)", analyticsPrimary, { analyticsPrimary = it; clearAnalyticsResult() }, "105000036")
                                LookupFilterField("Cấp Nhà chính (tùy chọn)", analyticsSecondary, { analyticsSecondary = it; clearAnalyticsResult() }, "19")
                            } else {
                                Text("Thống kê Huyền thoại không lọc theo bậc giải hoặc cấp Nhà chính.", fontSize = 9.sp, color = LookupMuted)
                            }
                        }
                        "legend_days", "legend_performance" -> {
                            LookupFilterField("Từ ngày (YYYY-MM-DD)", analyticsFrom, { analyticsFrom = it; clearAnalyticsResult() }, "YYYY-MM-DD")
                            LookupFilterField("Đến ngày (YYYY-MM-DD)", analyticsTo, { analyticsTo = it; clearAnalyticsResult() }, "YYYY-MM-DD")
                            Text("Nhóm dữ liệu", color = LookupMuted, fontSize = 9.sp)
                            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                val cohorts = if (analyticsCategory == "legend_days") listOf("legend_i", "top_1000", "top_200") else listOf("observed", "legend_i", "top_1000", "top_200")
                                cohorts.forEach { cohort ->
                                    FilterChip(selected = analyticsCohort == cohort, onClick = { analyticsCohort = cohort; clearAnalyticsResult() },
                                        label = { Text(cohort.uppercase().replace('_', ' '), fontSize = 8.sp) })
                                }
                            }
                        }
                    }
                    Text("Ngày có thể để trống nếu API hỗ trợ mặc định. Mã định danh và mã mùa giải phải lấy từ dữ liệu ClashKing.", color = LookupMuted, fontSize = 9.sp, lineHeight = 13.sp)
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
                    LookupFilterField("TAG bang hội (phân cách bằng dấu phẩy)", clanTags, { clanTags = it; resetSearchPage() }, "#ABC123")
                    LookupFilterField("Mã giải đấu (phân cách bằng dấu phẩy)", leagueIds, { leagueIds = it; resetSearchPage() }, "29000000")
                    LookupFilterField("Cấp Nhà chính (phân cách bằng dấu phẩy)", townhallLevels, { townhallLevels = it; resetSearchPage() }, "17,18,19")
                    Text(
                        "Tối đa 100 TAG bang hội, 5 mã giải và 100 cấp Nhà chính. Nhập nhiều giá trị cách nhau bằng dấu phẩy; bỏ trống mục nào thì không lọc mục đó.",
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
                        Text(
                            when (searchMode) {
                                "leaderboard" -> "XEM XẾP HẠNG"
                                "analytics" -> "TẢI THỐNG KÊ"
                                "player" -> "TÌM NGƯỜI CHƠI"
                                else -> "TÌM BANG HỘI"
                            },
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
        }

        val playerHistory = recentSearches.filter { it.mode == "player" }
        val clanHistory = recentSearches.filter { it.mode == "clan" }
        val playerFavorites = favoriteEntries.filterNot { it.isClan }
        val clanFavorites = favoriteEntries.filter { it.isClan }

        if (recentSearches.isNotEmpty() || favoriteEntries.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(11.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (recentSearches.isNotEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.History, null, tint = LookupCyan, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("LỊCH SỬ TRA CỨU", color = LookupCyan, fontSize = 10.sp, fontWeight = FontWeight.Black)
                            Spacer(Modifier.weight(1f))
                            Text("${recentSearches.size}/${LOOKUP_HISTORY_LIMIT}", color = LookupMuted, fontSize = 9.sp)
                            IconButton(
                                onClick = {
                                    clearLookupHistory(context)
                                    recentSearches = emptyList()
                                },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(Icons.Outlined.DeleteOutline, contentDescription = "Xóa lịch sử", tint = LookupMuted, modifier = Modifier.size(17.dp))
                            }
                        }
                        if (playerHistory.isNotEmpty()) {
                            Text("NGƯỜI CHƠI · ${playerHistory.size}", color = LookupMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            Row(
                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                playerHistory.forEach { item ->
                                    FilterChip(
                                        selected = false,
                                        onClick = {
                                            searchMode = "player"
                                            query = item.query
                                            resetSearchPage()
                                            analyticsResult = null
                                            searchError = null
                                        },
                                        label = { Text(item.query, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                                    )
                                }
                            }
                        }
                        if (clanHistory.isNotEmpty()) {
                            Text("BANG HỘI · ${clanHistory.size}", color = LookupMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            Row(
                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                clanHistory.forEach { item ->
                                    FilterChip(
                                        selected = false,
                                        onClick = {
                                            searchMode = "clan"
                                            query = item.query
                                            resetSearchPage()
                                            analyticsResult = null
                                            searchError = null
                                        },
                                        label = { Text(item.query, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                                    )
                                }
                            }
                        }
                    }

                    if (favoriteEntries.isNotEmpty()) {
                        if (recentSearches.isNotEmpty()) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Star, null, tint = LookupGold, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("HỒ SƠ ĐÃ GHIM", color = LookupGold, fontSize = 10.sp, fontWeight = FontWeight.Black)
                            Spacer(Modifier.weight(1f))
                            Text("${favoriteEntries.size}/${LOOKUP_FAVORITES_LIMIT}", color = LookupMuted, fontSize = 9.sp)
                        }
                        if (playerFavorites.isNotEmpty()) {
                            Text("NGƯỜI CHƠI · ${playerFavorites.size}", color = LookupMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            Row(
                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                playerFavorites.forEach { saved ->
                                    FilterChip(
                                        selected = false,
                                        onClick = {
                                            clearExtendedResults()
                                            selectedEntry = saved
                                            selectedSection = "TỔNG QUAN"
                                        },
                                        label = { Text("${saved.name} · ${saved.tag}", fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                        leadingIcon = { Icon(Icons.Outlined.Star, null, tint = LookupGold, modifier = Modifier.size(13.dp)) }
                                    )
                                }
                            }
                        }
                        if (clanFavorites.isNotEmpty()) {
                            Text("BANG HỘI · ${clanFavorites.size}", color = LookupMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            Row(
                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                clanFavorites.forEach { saved ->
                                    FilterChip(
                                        selected = false,
                                        onClick = {
                                            clearExtendedResults()
                                            selectedEntry = saved
                                            selectedSection = "TỔNG QUAN"
                                        },
                                        label = { Text("${saved.name} · ${saved.tag}", fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                        leadingIcon = { Icon(Icons.Outlined.Star, null, tint = LookupGold, modifier = Modifier.size(13.dp)) }
                                    )
                                }
                            }
                        }
                    }
                    Text(
                        "Lịch sử và hồ sơ đã ghim được nhóm riêng theo người chơi/bang hội và chỉ lưu cục bộ trên thiết bị này.",
                        color = LookupMuted,
                        fontSize = 8.sp
                    )
                }
            }
        }

        if (searchError != null) LookupMessageCard(searchError ?: "", isError = true)
        analyticsResult?.let { LookupDataPanel(title = analyticsResultTitle, payload = it, catalog = lookupCatalog) }
        if (searchMode == "clan" && searchError == null) {
            Text("Có thể tìm bang hội bằng tên, TAG hoặc tên có biểu tượng/ký tự trang trí. Ứng dụng tự thử dạng chữ chuẩn hóa khi cần.", color = LookupMuted, fontSize = 9.sp, lineHeight = 13.sp)
        }

        val comparisonEntries = comparedEntryKeys.mapNotNull { key -> results.firstOrNull { lookupEntryKey(it) == key } }
        if (comparedEntryKeys.isNotEmpty()) {
            if (comparisonEntries.size == 2) {
                LookupComparisonCard(comparisonEntries)
            } else {
                LookupMessageCard("Đã chọn ${comparisonEntries.size}/2 hồ sơ để so sánh. Chọn thêm một kết quả.")
            }
        }

        if (results.isNotEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    when (searchMode) {
                        "player" -> "KẾT QUẢ NGƯỜI CHƠI"
                        "clan" -> "KẾT QUẢ BANG HỘI"
                        "leaderboard" -> "KẾT QUẢ XẾP HẠNG"
                        else -> "KẾT QUẢ THỐNG KÊ"
                    },
                    fontSize = 11.sp,
                    color = LookupGold,
                    fontWeight = FontWeight.Black
                )
                Spacer(Modifier.weight(1f))
                Text("${results.size} kết quả", fontSize = 9.sp, color = LookupMuted)
            }
            results.forEach { result ->
                LookupResultCard(
                    entry = result,
                    onClick = {
                        clearExtendedResults()
                        selectedEntry = result
                        selectedSection = "TỔNG QUAN"
                    },
                    onCopy = { clipboard.setText(AnnotatedString(result.tag)) },
                    isFavorite = favoriteEntries.any { it.tag.equals(result.tag, true) && it.isClan == result.isClan },
                    onFavorite = { toggleFavorite(result) },
                    isCompared = lookupEntryKey(result) in comparedEntryKeys,
                    onCompare = { toggleCompare(result) }
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
                    "player" -> "Nhập ID/TAG (ví dụ #ABC123) hoặc tên người chơi. Có thể tìm một phần tên; ứng dụng sẽ thử cả tên đã chuẩn hóa."
                    "clan" -> "Nhập ID/TAG (ví dụ #ABC123) hoặc tên bang hội. Nếu tìm theo TAG, ứng dụng còn đối chiếu hồ sơ bang đã lưu."
                    "leaderboard" -> "Tra cứu bảng xếp hạng theo Nhà chính, giải đấu, chuỗi 3 sao và các hạng mục bang hội."
                    else -> "Truy cập thống kê công khai: bảng xếp hạng lịch sử, nhóm xếp hạng, phân bố cúp Huyền thoại và dữ liệu tổng quan."
                }
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            Icon(Icons.Outlined.Security, contentDescription = null, tint = LookupMuted, modifier = Modifier.size(14.dp))
            Text(
                "API công khai không cần khóa API hoặc mật khẩu Clash of Clans. API bảo vệ cần phiên ClashKing đã xác minh hoặc quyền bot; chế độ này không truy cập các API đó.",
                fontSize = 9.sp,
                lineHeight = 13.sp,
                color = LookupMuted
            )
        }
        Text(
            "Nguồn dữ liệu: ClashKing API • Mã nhà sáng tạo: ClashKing. COC-LIO là công cụ cộng đồng độc lập, không phải ứng dụng chính thức của Supercell.",
            fontSize = 9.sp,
            color = LookupMuted,
            lineHeight = 13.sp
        )
    }
}


private fun lookupComparisonIconKey(label: String): String = when (label.lowercase()) {
    "cấp người chơi" -> "expLevel"
    "nhà chính" -> "townHallLevel"
    "nhà thợ xây" -> "builderHallLevel"
    "cúp", "cúp giải đấu" -> "trophies"
    "cúp nhà thợ xây" -> "builderBaseTrophies"
    "giải đấu" -> "league"
    "bang hội" -> "clan"
    "cấp bang" -> "clanLevel"
    "thành viên" -> "members"
    "thứ hạng" -> "rank"
    "chuỗi thắng", "chuỗi 3 sao" -> "warWinStreak"
    else -> ""
}

@Composable
private fun LookupComparisonCard(entries: List<ClashLookupEntry>) {
    if (entries.size != 2) return
    val first = entries[0]
    val second = entries[1]
    val rows = if (first.isClan) {
        listOf(
            Triple("Cấp bang", first.level, second.level),
            Triple("Thành viên", first.members, second.members),
            Triple("Thứ hạng", first.rank, second.rank),
            Triple("Chuỗi thắng", first.streak, second.streak),
            Triple(first.metricLabel.ifBlank { "Chỉ số" }, first.metric, second.metric)
        )
    } else {
        listOf(
            Triple("Cấp người chơi", first.playerLevel, second.playerLevel),
            Triple("Nhà chính", first.townHall, second.townHall),
            Triple("Nhà thợ xây", first.builderHall, second.builderHall),
            Triple("Cúp", first.trophies, second.trophies),
            Triple("Cúp Nhà thợ xây", first.builderTrophies, second.builderTrophies),
            Triple("Giải đấu", first.league, second.league),
            Triple("Bang hội", first.clanName, second.clanName),
            Triple("Thứ hạng", first.rank, second.rank),
        )
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f)),
        border = BorderStroke(1.dp, LookupCyan.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                if (first.isClan) "SO SÁNH BANG HỘI" else "SO SÁNH NGƯỜI CHƠI",
                color = LookupCyan,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(first.name, modifier = Modifier.weight(1f), color = LookupGold, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text("VS", color = LookupMuted, fontSize = 9.sp, fontWeight = FontWeight.Black)
                Text(second.name, modifier = Modifier.weight(1f), color = LookupGold, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Text("${first.tag}     •     ${second.tag}", color = LookupMuted, fontSize = 8.sp)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
            rows.forEach { (label, leftValue, rightValue) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(formatLookupCount(leftValue).ifBlank { "—" }, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.weight(1.1f),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LookupGameFieldIcon(lookupComparisonIconKey(label), modifier = Modifier.size(15.dp))
                        Text(label, color = LookupMuted, fontSize = 9.sp, textAlign = TextAlign.Center)
                    }
                    Text(formatLookupCount(rightValue).ifBlank { "—" }, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End)
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
            }
            Text(
                "So sánh các trường dữ liệu có sẵn trong kết quả tìm kiếm. Dấu — nghĩa là API không trả về trường đó.",
                color = LookupMuted,
                fontSize = 8.sp,
                lineHeight = 12.sp
            )
        }
    }
}

@Composable
private fun LookupChoiceGroup(
    title: String,
    selected: String,
    options: List<Pair<String, String>>,
    onSelect: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, color = LookupMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            options.forEach { (value, label) ->
                FilterChip(
                    selected = selected == value,
                    onClick = { onSelect(value) },
                    label = { Text(label, fontSize = 8.sp) }
                )
            }
        }
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
    onCopy: () -> Unit,
    isFavorite: Boolean,
    onFavorite: () -> Unit,
    isCompared: Boolean,
    onCompare: () -> Unit
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
            LookupEntityAvatar(entry, 44.dp)
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    if (entry.isClan) "BANG HỘI" else "NGƯỜI CHƠI",
                    fontSize = 8.sp,
                    color = if (entry.isClan) LookupGold else LookupCyan,
                    fontWeight = FontWeight.Black
                )
                Text(entry.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "${if (entry.isClan) "TAG BANG HỘI" else "TAG NGƯỜI CHƠI"} · ${entry.tag.ifBlank { "Chưa có TAG" }}",
                    fontSize = 9.sp,
                    color = LookupCyan,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val subtitle = if (entry.isClan) {
                    listOfNotNull(
                        entry.rank.takeIf { it.isNotBlank() }?.let { "Hạng #${formatLookupCount(it)}" },
                        entry.level.takeIf { it.isNotBlank() }?.let { "Cấp ${formatLookupCount(it)}" },
                        entry.members.takeIf { it.isNotBlank() }?.let { "${formatLookupCount(it)} thành viên" },
                        entry.streak.takeIf { it.isNotBlank() }?.let { "Chuỗi thắng ${formatLookupCount(it)}" },
                        entry.metric.takeIf { it.isNotBlank() }?.let { "${entry.metricLabel}: ${formatLookupCount(it)}" }
                    ).joinToString(" • ")
                } else {
                    listOfNotNull(
                        entry.rank.takeIf { it.isNotBlank() }?.let { "Hạng #${formatLookupCount(it)}" },
                        entry.townHall.takeIf { it.isNotBlank() }?.let { "Nhà chính ${formatLookupCount(it)}" },
                        entry.league.takeIf { it.isNotBlank() },
                        entry.clanName.takeIf { it.isNotBlank() }
                    ).joinToString(" • ")
                }
                Text(subtitle.ifBlank { if (entry.isClan) "Bang hội" else "Người chơi" }, fontSize = 9.sp, color = LookupMuted, maxLines = 2)
                LookupEntryQuickStats(entry)
            }
            IconButton(onClick = onFavorite) {
                Icon(
                    if (isFavorite) Icons.Outlined.Star else Icons.Outlined.StarBorder,
                    contentDescription = if (isFavorite) "Bỏ ghim hồ sơ" else "Ghim hồ sơ",
                    tint = if (isFavorite) LookupGold else LookupMuted
                )
            }
            IconButton(onClick = onCompare) {
                Icon(
                    Icons.Outlined.CompareArrows,
                    contentDescription = if (isCompared) "Bỏ khỏi so sánh" else "Thêm vào so sánh",
                    tint = if (isCompared) LookupCyan else LookupMuted
                )
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

private fun lookupOrderedJsonKeys(payload: JSONObject): List<String> {
    val priority = listOf(
        "name", "tag", "playerTag", "clanTag", "description",
        "townHallLevel", "townhallLevel", "builderHallLevel", "expLevel", "experienceLevel", "playerLevel",
        "trophies", "builderBaseTrophies", "leagueTrophies", "league", "leagueTier", "clan",
        "clanLevel", "memberCount", "members", "rank", "globalRank", "localRank", "groupRank",
        "warWins", "warLosses", "warTies", "warWinStreak", "donated", "received",
        "capitalGoldTotal", "capitalLeague", "stars", "destructionPercentage",
        "updatedAt", "lastActive", "lastObservedAt", "joinedAt", "battleTime", "eventTime",
        "startTime", "endTime", "time", "type", "result"
    ).mapIndexed { index, key -> key.lowercase() to index }.toMap()
    return payload.keys().asSequence().toList().sortedWith(
        compareBy<String> { priority[it.lowercase()] ?: Int.MAX_VALUE }
            .thenBy { humanizeLookupKey(it).lowercase() }
    )
}

private fun JSONObject.lookupExplicitRank(): Long? {
    // "position" is often a war map position, not a leaderboard rank.
    for (key in listOf("rank", "rankPosition", "globalRank", "groupRank", "localRank")) {
        val parsed = lookupText(key).trim().removePrefix("#").toLongOrNull()
        if (parsed != null && parsed > 0L) return parsed
    }
    return null
}

private fun lookupJsonArrayDisplayOrder(key: String, values: JSONArray): List<Int> {
    val indices = (0 until values.length()).toList()
    if (indices.size < 2) return indices
    val normalizedKey = key.lowercase().replace("_", "").replace("-", "")
    val chronologicalKeys = listOf("history", "timeline", "series", "battle", "activity", "event", "change", "joinleave", "season", "daily", "day", "war")
    if (chronologicalKeys.any { marker -> normalizedKey.contains(marker) }) return indices

    val timeKeys = setOf(
        "date", "timestamp", "time", "eventTime", "battleTime", "updatedAt",
        "lastObservedAt", "joinedAt", "startTime", "endTime", "createdAt"
    )
    val objectRows = indices.mapNotNull(values::optJSONObject)
    // API arrays can be wrapped under generic keys such as "items". Preserve their original
    // chronology when every row contains a timestamp, even if the parent key is generic.
    if (objectRows.size == indices.size && objectRows.all { row -> timeKeys.any(row::has) }) return indices

    val ranked = indices.mapNotNull { index ->
        values.optJSONObject(index)?.lookupExplicitRank()?.let { index to it }
    }
    // Only rank-oriented lists with explicit rank fields are sorted. History, activity,
    // battle logs and time series retain the API's chronological order.
    return if (ranked.size == indices.size) {
        ranked.sortedWith(compareBy<Pair<Int, Long>> { it.second }.thenBy { it.first }).map { it.first }
    } else indices
}

private fun JSONObject.lookupListItemTitle(parentKey: String, index: Int): String {
    val name = listOf(
        "name", "playerName", "clanName", "unitName", "troopName", "heroName",
        "spellName", "petName", "equipmentName", "buildingName", "defenseName",
        "opponentName", "attackerName", "defenderName", "title"
    ).firstNotNullOfOrNull { key -> lookupText(key).takeIf { it.isNotBlank() } }
    if (!name.isNullOrBlank()) {
        val rank = lookupText("rank").ifBlank { lookupText("globalRank") }.ifBlank { lookupText("groupRank") }
        val level = lookupText("level").ifBlank { lookupText("townHallLevel") }.ifBlank { lookupText("builderHallLevel") }.ifBlank { lookupText("expLevel") }
        return when {
            rank.isNotBlank() -> "$name · Hạng #$rank"
            level.isNotBlank() -> "$name · Cấp $level"
            else -> name
        }
    }
    val tag = lookupText("tag").ifBlank { lookupText("playerTag") }.ifBlank { lookupText("clanTag") }
    if (tag.isNotBlank()) return "TAG $tag"
    val id = lookupText("id")
    if (id.isNotBlank()) return "ID $id"
    return "${humanizeLookupKey(parentKey)} #${index + 1}"
}

@Composable
private fun LookupDataPanel(
    title: String,
    payload: JSONObject,
    catalog: Map<Int, SupplementalCatalogEntry>
) {
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
            LookupJsonObject(payload, depth = 0, catalog = catalog)
        }
    }
}

@Composable
private fun LookupJsonObject(
    payload: JSONObject,
    depth: Int,
    catalog: Map<Int, SupplementalCatalogEntry>
) {
    if (depth > 8) {
        Text("Dữ liệu lồng quá sâu, không thể mở rộng thêm.", fontSize = 9.sp, color = LookupMuted)
        return
    }
    val keys = remember(payload) { lookupOrderedJsonKeys(payload) }
    keys.forEach { key ->
        val normalizedKey = key.lowercase()
        if (normalizedKey in setOf("iconurls", "badgeurls", "icons", "images", "imageurl", "image_url", "badge_url", "iconurl", "icon_url", "avatarurl", "avatar_url")) {
            return@forEach
        }
        val value = payload.opt(key)
        when (value) {
            is JSONObject -> {
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    val imageUrl = lookupImageUrl(value, catalog, key)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        LookupGameFieldIcon(key, imageUrl, modifier = Modifier.size(23.dp))
                        Text(
                            humanizeLookupKey(key),
                            color = if (depth == 0) LookupGold else LookupCyan,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.36f)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(Modifier.fillMaxWidth().padding(9.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            LookupJsonObject(value, depth + 1, catalog)
                        }
                    }
                }
            }
            is JSONArray -> LookupJsonArray(key, humanizeLookupKey(key), value, depth + 1, catalog)
            else -> {
                val display = lookupDisplayValue(key, value)
                if (display.isNotBlank()) LookupDetailRow(humanizeLookupKey(key), display, key)
            }
        }
    }
}

@Composable
private fun LookupJsonArray(
    key: String,
    label: String,
    values: JSONArray,
    depth: Int,
    catalog: Map<Int, SupplementalCatalogEntry>
) {
    var visibleCount by remember(values) { mutableIntStateOf(20) }
    val displayIndices = remember(values, key) { lookupJsonArrayDisplayOrder(key, values) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            LookupGameFieldIcon(key, modifier = Modifier.size(18.dp))
            Text(label, color = if (depth <= 1) LookupGold else LookupCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Text("${values.length()} mục", color = LookupMuted, fontSize = 9.sp)
        }
        if (values.length() == 0) {
            Text("Không có dữ liệu được ghi nhận.", color = LookupMuted, fontSize = 9.sp)
        } else {
            val end = minOf(displayIndices.size, visibleCount)
            for (position in 0 until end) {
                val index = displayIndices[position]
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
                                val imageUrl = lookupImageUrl(value, catalog, key)
                                val objectName = value.lookupListItemTitle(key, position)
                                val tag = value.lookupText("tag").ifBlank { value.lookupText("playerTag") }.ifBlank { value.lookupText("clanTag") }
                                val rank = value.lookupText("rank").ifBlank { value.lookupText("globalRank") }.ifBlank { value.lookupText("groupRank") }
                                val secondary = when {
                                    rank.isNotBlank() && !objectName.contains("Hạng #$rank", ignoreCase = true) -> "Hạng #$rank"
                                    tag.isNotBlank() && !objectName.contains(tag, ignoreCase = true) -> "TAG $tag"
                                    else -> "Vị trí ${position + 1}"
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                    if (imageUrl.isNotBlank()) {
                                        AsyncImage(model = imageUrl, contentDescription = objectName, modifier = Modifier.size(36.dp))
                                    } else {
                                        LookupGameFieldIcon(key, modifier = Modifier.size(22.dp))
                                    }
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(objectName, fontSize = 10.sp, color = LookupGold, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(secondary, fontSize = 8.sp, color = LookupMuted)
                                    }
                                }
                                LookupJsonObject(value, depth + 1, catalog)
                            }
                        }
                    }
                    is JSONArray -> LookupJsonArray(key, "#${index + 1}", value, depth + 1, catalog)
                    else -> LookupDetailRow("#${index + 1}", lookupDisplayValue(key, value), key)
                }
            }
            if (end < displayIndices.size) {
                Button(
                    onClick = { visibleCount = (visibleCount + 20).coerceAtMost(displayIndices.size) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = LookupCyan
                    )
                ) {
                    Text("HIỂN THỊ TIẾP ${minOf(20, displayIndices.size - end)} / ${displayIndices.size - end}", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun LookupDetailRow(label: String, value: String, key: String = "") {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Row(
            modifier = Modifier.weight(0.9f),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LookupGameFieldIcon(key, value = value, modifier = Modifier.size(16.dp))
            Text(label, color = LookupMuted, fontSize = 10.sp, lineHeight = 14.sp)
        }
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
