package com.huyhoa.coclio

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

private enum class ClashLookupKind { PLAYER, CLAN }

private data class ClashLookupResponse(
    val kind: ClashLookupKind,
    val profile: JSONObject,
    val rankings: JSONObject? = null
)

private const val CLASHKING_API = "https://api.clashk.ing"

private fun normalizeClashTag(value: String): String {
    val tag = value.trim().uppercase().replace(" ", "").let {
        if (it.startsWith("#")) it else "#$it"
    }
    require(Regex("^#[PYLQGRJCUV0289]{3,15}$").matches(tag)) {
        "Mã không hợp lệ. Hãy nhập tag người chơi hoặc Clan dạng #ABC123."
    }
    return tag
}

private fun readClashKingJson(path: String): Any = run {
    val connection = URL("$CLASHKING_API$path").openConnection() as HttpURLConnection
    try {
        connection.requestMethod = "GET"
        connection.connectTimeout = 12000
        connection.readTimeout = 15000
        connection.setRequestProperty("Accept", "application/json")
        connection.setRequestProperty("User-Agent", "COC-LIO-Android")
        val status = connection.responseCode
        val body = (if (status in 200..299) connection.inputStream else connection.errorStream)
            ?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
        if (status !in 200..299) {
            when (status) {
                404 -> throw IllegalStateException("Không tìm thấy hồ sơ hoặc dữ liệu này chưa có trong kho ClashKing.")
                429 -> throw IllegalStateException("ClashKing đang giới hạn lượt tra cứu. Vui lòng thử lại sau.")
                in 500..599 -> throw IllegalStateException("Máy chủ ClashKing đang gặp sự cố. Vui lòng thử lại sau.")
                else -> throw IllegalStateException("ClashKing trả về lỗi HTTP $status.")
            }
        }
        JSONTokener(body).nextValue()
    } finally {
        connection.disconnect()
    }
}

private suspend fun lookupClashKing(kind: ClashLookupKind, rawTag: String): ClashLookupResponse =
    withContext(Dispatchers.IO) {
        val tag = normalizeClashTag(rawTag)
        val encodedTag = URLEncoder.encode(tag, "UTF-8")
        when (kind) {
            ClashLookupKind.PLAYER -> {
                val search = readClashKingJson(
                    "/v2/player/search?query=$encodedTag&limit=10"
                ) as? JSONObject ?: throw IllegalStateException("Dữ liệu người chơi không đúng định dạng.")
                val items = search.optJSONArray("items") ?: JSONArray()
                val player = (0 until items.length())
                    .mapNotNull { items.optJSONObject(it) }
                    .firstOrNull { it.optString("tag").equals(tag, ignoreCase = true) }
                    ?: throw IllegalStateException("Không tìm thấy người chơi $tag trong dữ liệu ClashKing.")
                val rankings = runCatching {
                    readClashKingJson("/v2/player/$encodedTag/rankings") as? JSONObject
                }.getOrNull()
                ClashLookupResponse(ClashLookupKind.PLAYER, player, rankings)
            }
            ClashLookupKind.CLAN -> {
                val clan = readClashKingJson("/v2/clan/$encodedTag/cached") as? JSONObject
                    ?: throw IllegalStateException("Chưa có hồ sơ Clan $tag trong kho dữ liệu ClashKing.")
                ClashLookupResponse(ClashLookupKind.CLAN, clan)
            }
        }
    }

@Composable
fun ClashLookupScreen() {
    var kind by remember { mutableStateOf(ClashLookupKind.PLAYER) }
    var tagInput by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<ClashLookupResponse?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var detailLoading by remember { mutableStateOf(false) }
    var detailTitle by remember { mutableStateOf<String?>(null) }
    var detailText by remember { mutableStateOf<String?>(null) }
    var detailError by remember { mutableStateOf<String?>(null) }
    var townHallInput by remember { mutableStateOf("") }
    var leagueTierInput by remember { mutableStateOf("") }
    var locationInput by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current

    fun loadAction(action: ClashLookupAction) {
        scope.launch {
            detailLoading = true
            detailTitle = action.title
            detailText = null
            detailError = null
            runCatching { loadClashKingDetail(action.path) }
                .onSuccess { detailText = it }
                .onFailure {
                    detailError = it.message?.takeIf(String::isNotBlank)
                        ?: "Không thể tải dữ liệu này."
                }
            detailLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "TRA CỨU TÀI KHOẢN & CLAN",
            color = MaterialTheme.colorScheme.primary,
            fontSize = 19.sp,
            fontWeight = FontWeight.Black
        )
        Text(
            "Nhập mã tag trong game để xem hồ sơ người chơi hoặc Clan.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = kind == ClashLookupKind.PLAYER,
                onClick = {
                    kind = ClashLookupKind.PLAYER
                    result = null
                    error = null
                },
                label = { Text("NGƯỜI CHƠI") }
            )
            FilterChip(
                selected = kind == ClashLookupKind.CLAN,
                onClick = {
                    kind = ClashLookupKind.CLAN
                    result = null
                    error = null
                },
                label = { Text("CLAN") }
            )
        }
        OutlinedTextField(
            value = tagInput,
            onValueChange = {
                tagInput = it
                error = null
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(if (kind == ClashLookupKind.PLAYER) "Mã người chơi" else "Mã Clan") },
            placeholder = { Text("#ABC123") },
            singleLine = true,
            enabled = !loading
        )
        Button(
            onClick = {
                scope.launch {
                    loading = true
                    error = null
                    result = null
                    runCatching { lookupClashKing(kind, tagInput) }
                        .onSuccess { result = it }
                        .onFailure {
                            error = it.message?.takeIf(String::isNotBlank)
                                ?: "Không thể tra cứu lúc này. Hãy thử lại."
                        }
                    loading = false
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !loading && tagInput.isNotBlank()
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
                Spacer(Modifier.width(8.dp))
                Text("ĐANG TRA CỨU...")
            } else {
                Text("TRA CỨU")
            }
        }
        Text(
            "Mã cần nhập là Player Tag hoặc Clan Tag có dấu #, không phải Supercell ID/email.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.sp
        )

        error?.let {
            LookupCard {
                Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
            }
        }

        result?.let { data ->
            if (data.kind == ClashLookupKind.PLAYER) {
                PlayerLookupCard(data)
            } else {
                ClanLookupCard(data)
            }

            Text(
                if (data.kind == ClashLookupKind.PLAYER) "THỐNG KÊ & LỊCH SỬ NGƯỜI CHƠI" else "THỐNG KÊ & LỊCH SỬ CLAN",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Chọn mục để tải thêm dữ liệu từ ClashKing.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp
            )
            val actions = clashLookupActions(data)
            actions.chunked(2).forEach { rowActions ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowActions.forEach { action ->
                        OutlinedButton(
                            onClick = { loadAction(action) },
                            modifier = Modifier.weight(1f),
                            enabled = !detailLoading
                        ) {
                            Text(action.title, fontSize = 9.sp, lineHeight = 12.sp)
                        }
                    }
                    if (rowActions.size == 1) Spacer(Modifier.weight(1f))
                }
            }
            if (detailLoading) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("ĐANG TẢI ${detailTitle.orEmpty()}...", fontSize = 10.sp)
                }
            }
            detailError?.let {
                LookupCard { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 11.sp) }
            }
            detailText?.let { json ->
                LookupCard {
                    Text(
                        detailTitle.orEmpty(),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Text(
                        json,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 9.sp,
                        lineHeight = 12.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                }
            }
        }

        Text(
            "TOÀN CẢNH CLASH OF CLANS",
            color = MaterialTheme.colorScheme.primary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Thống kê cộng đồng, lịch mùa giải và bảng xếp hạng công khai.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.sp
        )
        globalClashActions().chunked(2).forEach { rowActions ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowActions.forEach { action ->
                    OutlinedButton(
                        onClick = { loadAction(action) },
                        modifier = Modifier.weight(1f),
                        enabled = !detailLoading
                    ) {
                        Text(action.title, fontSize = 9.sp, lineHeight = 12.sp)
                    }
                }
                if (rowActions.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        LookupCard {
            Text(
                "BẢNG XẾP HẠNG THEO ĐIỀU KIỆN",
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
            OutlinedTextField(
                value = townHallInput,
                onValueChange = { townHallInput = it.filter { character -> character.isDigit() }.take(2) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Cấp Nhà chính (1–20)") },
                singleLine = true
            )
            val townHall = townHallInput.toIntOrNull()
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        townHall?.let {
                            loadAction(ClashLookupAction("Bảng xếp hạng Nhà chính $it", "/v2/leaderboard/townhalls/$it?limit=50"))
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !detailLoading && townHall != null && townHall in 1..20
                ) { Text("TOP NHÀ CHÍNH", fontSize = 9.sp) }
                OutlinedButton(
                    onClick = {
                        townHall?.let {
                            loadAction(ClashLookupAction("Chuỗi 3 sao Nhà chính $it", "/v2/leaderboard/war-triples/$it?limit=50"))
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !detailLoading && townHall != null && townHall in 1..20
                ) { Text("TOP 3 SAO", fontSize = 9.sp) }
            }
            OutlinedTextField(
                value = leagueTierInput,
                onValueChange = { leagueTierInput = it.filter(Char::isDigit).take(10) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Mã cấp giải đấu (League Tier ID)") },
                singleLine = true
            )
            val leagueTier = leagueTierInput.toIntOrNull()
            OutlinedButton(
                onClick = {
                    leagueTier?.let {
                        loadAction(ClashLookupAction("Bảng xếp hạng giải $it", "/v2/leaderboard/league/$it?limit=50"))
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !detailLoading && leagueTier != null && leagueTier > 0
            ) { Text("TOP GIẢI ĐẤU") }
            OutlinedTextField(
                value = locationInput,
                onValueChange = { locationInput = it.filter(Char::isDigit).take(10) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Mã khu vực / quốc gia (Location ID)") },
                singleLine = true
            )
            val location = locationInput.toIntOrNull()
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        location?.let {
                            loadAction(ClashLookupAction("Top quyên góp Clan", "/v2/leaderboard/$it/clan/donations?limit=50"))
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !detailLoading && location != null && location > 0
                ) { Text("TOP QUYÊN GÓP", fontSize = 9.sp) }
                OutlinedButton(
                    onClick = {
                        location?.let {
                            loadAction(ClashLookupAction("Top thắng chiến tranh Clan", "/v2/leaderboard/$it/clan/war-wins?limit=50"))
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !detailLoading && location != null && location > 0
                ) { Text("TOP CHIẾN THẮNG", fontSize = 9.sp) }
            }
            OutlinedButton(
                onClick = {
                    location?.let {
                        loadAction(ClashLookupAction("Top vàng Thủ đô Clan", "/v2/leaderboard/$it/clan/capital-gold?limit=50"))
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !detailLoading && location != null && location > 0
            ) { Text("TOP VÀNG THỦ ĐÔ") }
        }

        LookupCard {
            Text(
                "Nguồn dữ liệu: ClashKing",
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )
            Text(
                "Dữ liệu được ClashKing lưu và đồng bộ từ nguồn trò chơi; có thể chậm hoặc chưa có đầy đủ. COC-LIO không phải ứng dụng chính thức của Supercell.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp,
                lineHeight = 15.sp
            )
            Text(
                "MỞ CLASHKING",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable {
                    runCatching { uriHandler.openUri("https://clashk.ing/") }
                }
            )
        }
    }
}


private data class ClashLookupAction(val title: String, val path: String)

private fun globalClashActions(): List<ClashLookupAction> = listOf(
    ClashLookupAction("Số liệu toàn cầu", "/v2/counts"),
    ClashLookupAction("Số lượng Nhà chính", "/v2/counts/players/town-halls"),
    ClashLookupAction("Số lượng giải đấu", "/v2/counts/players/league-tiers"),
    ClashLookupAction("Số lượng Clan theo khu vực", "/v2/counts/clans/locations"),
    ClashLookupAction("Số lượng Clan theo CWL", "/v2/counts/clans/cwl-leagues"),
    ClashLookupAction("Số lượng Clan theo giải thủ đô", "/v2/counts/clans/capital-leagues"),
    ClashLookupAction("Phân bố quy mô Clan", "/v2/counts/clans/member-bins"),
    ClashLookupAction("Phân bố cúp Legend", "/v2/legends/trophy-buckets"),
    ClashLookupAction("Lịch mùa giải hiện tại", "/v2/dates/current"),
    ClashLookupAction("Lịch cuối tuần đột kích", "/v2/dates/raid-weekends"),
    ClashLookupAction("Lịch các mùa giải", "/v2/dates/seasons"),
    ClashLookupAction("Top chuỗi thắng Clan", "/v2/leaderboard/clan/win-streak?limit=50"),
    ClashLookupAction("Tỷ lệ sao chiến tranh", "/v2/stats/wars/hitrates?interval=month"),
    ClashLookupAction("Tổng kết chiến tranh", "/v2/stats/wars/summary?interval=month"),
    ClashLookupAction("Đội hình được dùng nhiều", "/v2/stats/army-setups?limit=20&sort=usage")
)

private fun clashLookupActions(data: ClashLookupResponse): List<ClashLookupAction> {
    val tag = URLEncoder.encode(data.profile.optString("tag"), "UTF-8")
    return if (data.kind == ClashLookupKind.PLAYER) {
        listOf(
            ClashLookupAction("Hoạt động 30 ngày", "/v2/player/$tag/activity-summary"),
            ClashLookupAction("Lịch sử trận đánh", "/v2/player/$tag/battlelog/history"),
            ClashLookupAction("Thống kê kiếm tài nguyên", "/v2/player/$tag/farming"),
            ClashLookupAction("Thống kê chiến tranh", "/v2/player/$tag/war/stats"),
            ClashLookupAction("Chi tiết lượt đánh", "/v2/player/$tag/war/attacks"),
            ClashLookupAction("Lịch sử CWL", "/v2/player/$tag/cwl/history?limit=15"),
            ClashLookupAction("Lịch sử vào/rời Clan", "/v2/player/$tag/join-leave?limit=40"),
            ClashLookupAction("Thống kê thời gian ở Clan", "/v2/player/$tag/join-leave/totals"),
            ClashLookupAction("Lịch sử nâng cấp Nhà chính", "/v2/player/$tag/history/changes?type=townhall_level&limit=30"),
            ClashLookupAction("Lịch sử quyên góp", "/v2/player/$tag/history/stats?type=donated&limit=30"),
            ClashLookupAction("Các mốc chiến tranh", "/v2/player/$tag/timers"),
            ClashLookupAction("Thành tích Legend", "/v2/player/$tag/legend-history"),
            ClashLookupAction("Mùa Legend hiện tại", "/v2/player/$tag/legend/season"),
            ClashLookupAction("Diễn biến cúp Legend", "/v2/player/$tag/legend/series"),
            ClashLookupAction("So sánh thành tích Legend", "/v2/player/$tag/legend/comparisons"),
            ClashLookupAction("Lịch sử Ranked", "/v2/player/$tag/ranked/history")
        )
    } else {
        listOf(
            ClashLookupAction("Tổng kết quyên góp", "/v2/clan/$tag/donations/summary"),
            ClashLookupAction("Lịch sử chiến tranh", "/v2/clan/$tag/wars?limit=20"),
            ClashLookupAction("Cuộc chiến hiện tại", "/v2/war/$tag/basic"),
            ClashLookupAction("Các mùa CWL", "/v2/cwl/$tag/seasons?limit=15"),
            ClashLookupAction("Nhóm CWL", "/v2/cwl/$tag/group"),
            ClashLookupAction("Thứ hạng CWL", "/v2/cwl/$tag/ranking-history"),
            ClashLookupAction("Lịch sử thành viên", "/v2/clan/$tag/join-leave?limit=50"),
            ClashLookupAction("Thay đổi hồ sơ Clan", "/v2/clan/$tag/history/changes?limit=30"),
            ClashLookupAction("Lịch sử bảng xếp hạng", "/v2/clan/$tag/history/leaderboards?limit=30"),
            ClashLookupAction("Lịch sử Legend của Clan", "/v2/clan/$tag/history/legends?limit=30"),
            ClashLookupAction("Top Legend của Clan", "/v2/clan/$tag/history/legends/summary?top=20"),
            ClashLookupAction("Kỷ lục Clan", "/v2/clan/$tag/records"),
            ClashLookupAction("Bảng xếp hạng Clan", "/v2/clan/$tag/rankings")
        )
    }
}

private suspend fun loadClashKingDetail(path: String): String = withContext(Dispatchers.IO) {
    val value = readClashKingJson(path)
    val formatted = when (value) {
        is JSONObject -> value.toString(2)
        is JSONArray -> value.toString(2)
        else -> value.toString()
    }
    if (formatted.length > 40000) {
        formatted.take(40000) + "\\n\\n... Đã rút gọn kết quả để tránh làm ứng dụng quá tải."
    } else {
        formatted
    }
}

@Composable
private fun PlayerLookupCard(data: ClashLookupResponse) {
    val player = data.profile
    val clan = player.optJSONObject("clan")
    val league = player.optJSONObject("leagueTier")
    val ranks = data.rankings
    LookupCard {
        Text(
            player.optString("name", "Người chơi"),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(player.optString("tag"), color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
        LookupValue("Nhà chính", player.optInt("townHallLevel").takeIf { it > 0 }?.toString() ?: "Chưa có dữ liệu")
        LookupValue("Giải đấu", league?.optString("name")?.takeIf { it.isNotBlank() } ?: "Chưa có dữ liệu")
        LookupValue("Clan", clan?.optString("name")?.takeIf { it.isNotBlank() } ?: "Không có / chưa có dữ liệu")
        clan?.optString("tag")?.takeIf { it.isNotBlank() }?.let { LookupValue("Mã Clan", it) }
        ranks?.optJSONObject("homeVillage")?.let {
            LookupValue("Cúp nhà chính", it.optInt("trophies").takeIf { value -> value > 0 }?.toString() ?: "—")
            LookupValue("Hạng toàn cầu", it.optInt("globalRank").takeIf { value -> value > 0 }?.toString() ?: "—")
            LookupValue("Hạng địa phương", it.optInt("localRank").takeIf { value -> value > 0 }?.toString() ?: "—")
        }
        ranks?.optJSONObject("builderBase")?.let {
            LookupValue("Cúp Nhà thợ", it.optInt("trophies").takeIf { value -> value > 0 }?.toString() ?: "—")
        }
    }
}

@Composable
private fun ClanLookupCard(data: ClashLookupResponse) {
    val clan = data.profile
    val badge = clan.optJSONObject("badgeUrls")?.optString("large").orEmpty()
    val location = clan.optJSONObject("location")?.optString("name").orEmpty()
    val warLeague = clan.optJSONObject("warLeague")?.optString("name").orEmpty()
    val capitalLeague = clan.optJSONObject("capitalLeague")?.optString("name").orEmpty()
    val members = clan.optJSONArray("members") ?: JSONArray()

    LookupCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (badge.isNotBlank()) {
                AsyncImage(
                    model = badge,
                    contentDescription = "Huy hiệu Clan",
                    modifier = Modifier.size(64.dp)
                )
                Spacer(Modifier.width(12.dp))
            }
            Column {
                Text(
                    clan.optString("name", "Clan"),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(clan.optString("tag"), color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
            }
        }
        if (clan.optString("description").isNotBlank()) {
            Text(
                clan.optString("description"),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                lineHeight = 16.sp
            )
        }
        LookupValue("Cấp Clan", clan.optInt("clanLevel").toString())
        LookupValue("Thành viên", clan.optInt("memberCount").toString())
        LookupValue("Điểm Clan", clan.optInt("clanPoints").toString())
        LookupValue("Vị trí", location.ifBlank { "Chưa có dữ liệu" })
        LookupValue("Hạng chiến tranh", warLeague.ifBlank { "Chưa có dữ liệu" })
        LookupValue("Giải thủ đô", capitalLeague.ifBlank { "Chưa có dữ liệu" })
        LookupValue("Số trận thắng", clan.optInt("warWins").toString())
        LookupValue("Chuỗi thắng", clan.optInt("warWinStreak").toString())
        LookupValue("Nhận lính", clan.optInt("troopsDonated").toString())
        LookupValue("Được nhận lính", clan.optInt("troopsReceived").toString())
        LookupValue("Vàng thủ đô", clan.optLong("capitalGoldTotal").toString())
        LookupValue("Nhật ký chiến tranh", if (clan.optBoolean("publicWarLog")) "Công khai" else "Riêng tư")

        if (members.length() > 0) {
            Spacer(Modifier.height(4.dp))
            Text("DANH SÁCH THÀNH VIÊN", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            (0 until minOf(members.length(), 50)).forEach { index ->
                val member = members.optJSONObject(index) ?: JSONObject()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        member.optString("name") + "  " + member.optString("tag"),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        "TH" + member.optInt("townHallLevel"),
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 10.sp
                    )
                }
            }
            if (members.length() > 50) {
                Text("Chỉ hiển thị 50 thành viên đầu tiên.", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun LookupCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
            content = content
        )
    }
}

@Composable
private fun LookupValue(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
        Spacer(Modifier.width(12.dp))
        Text(
            value,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
