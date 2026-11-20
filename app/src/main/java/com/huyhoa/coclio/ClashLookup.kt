package com.huyhoa.coclio

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
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
    val badgeUrl: String = "",
    val raw: JSONObject
)

private object ClashKingLookupApi {
    suspend fun get(path: String): JSONObject = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            connection = (URL(CLASHKING_API + path).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10_000
                readTimeout = 15_000
                instanceFollowRedirects = true
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "COC-LIO-Android")
            }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (status !in 200..299) {
                throw IOException("Dịch vụ tra cứu trả về mã HTTP $status.")
            }
            if (body.isBlank()) throw IOException("Dịch vụ không trả về dữ liệu.")
            JSONObject(body)
        } finally {
            connection?.disconnect()
        }
    }

    fun searchPath(isClan: Boolean, query: String, cursor: String?): String {
        val encodedQuery = URLEncoder.encode(query.trim(), "UTF-8")
        val route = if (isClan) "/v2/clan/search" else "/v2/player/search"
        val cursorPart = cursor?.takeIf { it.isNotBlank() }
            ?.let { "&cursor=${URLEncoder.encode(it, "UTF-8")}" }.orEmpty()
        return "$route?query=$encodedQuery&limit=20$cursorPart"
    }

    fun detailPath(entry: ClashLookupEntry, section: String): String? {
        val tag = URLEncoder.encode(entry.tag.trim().uppercase(), "UTF-8")
        return if (entry.isClan) {
            when (section) {
                "TỔNG QUAN" -> "/v2/clan/$tag/cached"
                "HỘI CHIẾN" -> "/v2/clan/$tag/warlog"
                "QUYÊN GÓP" -> "/v2/clan/$tag/donations/summary"
                "KỶ LỤC" -> "/v2/clan/$tag/records"
                "LỊCH SỬ" -> "/v2/clan/$tag/history/changes"
                else -> null
            }
        } else {
            when (section) {
                "TỔNG QUAN" -> null
                "HOẠT ĐỘNG" -> "/v2/player/$tag/activity-summary"
                "HỘI CHIẾN" -> "/v2/player/$tag/war/stats"
                "XẾP HẠNG" -> "/v2/player/$tag/rankings"
                "LỊCH SỬ" -> "/v2/player/$tag/history/changes?limit=20"
                else -> null
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
        name = json.lookupText("name").ifBlank { if (isClan) "Bang hội chưa rõ tên" else "Người chơi chưa rõ tên" },
        tag = json.lookupText("tag").ifBlank { json.lookupText("playerTag") },
        isClan = isClan,
        townHall = number("townHallLevel", "townhallLevel"),
        clanName = clan?.lookupText("name").orEmpty(),
        league = league?.lookupText("name").orEmpty(),
        trophies = number("trophies", "leagueTrophies", "versusTrophies"),
        members = number("members", "memberCount"),
        level = number("clanLevel", "level"),
        badgeUrl = badge,
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
        "trophies" to "Cúp",
        "leagueTrophies" to "Cúp giải đấu",
        "clanLevel" to "Cấp bang hội",
        "members" to "Thành viên",
        "memberCount" to "Thành viên",
        "warWins" to "Thắng hội chiến",
        "warLosses" to "Thua hội chiến",
        "warTies" to "Hòa hội chiến",
        "warWinStreak" to "Chuỗi thắng",
        "donations" to "Đã quyên góp",
        "donationsReceived" to "Đã nhận",
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
        "delta" to "Thay đổi"
    )
    return known[key] ?: key
        .replace(Regex("([a-z])([A-Z])"), "$1 $2")
        .replace('_', ' ')
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
}

private fun lookupValue(value: Any?): String = when (value) {
    null, JSONObject.NULL -> ""
    is JSONObject -> ""
    is JSONArray -> "${value.length()} mục"
    is Boolean -> if (value) "Có" else "Không"
    else -> value.toString().take(110)
}

private fun flattenLookupJson(json: JSONObject): List<Pair<String, String>> {
    val output = mutableListOf<Pair<String, String>>()

    fun visit(value: JSONObject, prefix: String, depth: Int) {
        if (depth > 3 || output.size >= 24) return
        val keys = value.keys()
        while (keys.hasNext() && output.size < 24) {
            val key = keys.next()
            val child = value.opt(key)
            val label = if (prefix.isBlank()) humanizeLookupKey(key) else "$prefix • ${humanizeLookupKey(key)}"
            when (child) {
                is JSONObject -> visit(child, label, depth + 1)
                is JSONArray -> {
                    output += label to "${child.length()} bản ghi"
                    for (index in 0 until minOf(child.length(), 3)) {
                        val item = child.opt(index)
                        if (item is JSONObject) visit(item, "$label #${index + 1}", depth + 1)
                    }
                }
                else -> {
                    val text = lookupValue(child)
                    if (text.isNotBlank()) output += label to text
                }
            }
        }
    }

    visit(json, "", 0)
    return output
}

@Composable
fun ClashLookupScreen() {
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    var searchMode by rememberSaveable { mutableStateOf("player") }
    var query by rememberSaveable { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf<String?>(null) }
    var results by remember { mutableStateOf(emptyList<ClashLookupEntry>()) }
    var nextCursor by remember { mutableStateOf<String?>(null) }
    var selectedEntry by remember { mutableStateOf<ClashLookupEntry?>(null) }
    var selectedSection by rememberSaveable { mutableStateOf("TỔNG QUAN") }
    var detailPayload by remember { mutableStateOf<JSONObject?>(null) }
    var detailLoading by remember { mutableStateOf(false) }
    var detailError by remember { mutableStateOf<String?>(null) }

    fun runSearch(loadMore: Boolean = false) {
        val term = query.trim()
        if (term.isBlank()) {
            searchError = "Nhập tên người chơi, tên bang hội hoặc TAG/ID cần tìm."
            return
        }
        val oldCursor = if (loadMore) nextCursor else null
        scope.launch {
            isSearching = true
            searchError = null
            if (!loadMore) {
                results = emptyList()
                nextCursor = null
            }
            try {
                val response = ClashKingLookupApi.get(
                    ClashKingLookupApi.searchPath(searchMode == "clan", term, oldCursor)
                )
                val found = lookupItems(response, searchMode == "clan")
                results = if (loadMore) results + found else found
                nextCursor = response.lookupText("cursor").takeIf { it.isNotBlank() }
                if (results.isEmpty()) {
                    searchError = "Không tìm thấy kết quả. Hãy kiểm tra tên hoặc TAG/ID."
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
        val path = ClashKingLookupApi.detailPath(entry, selectedSection)
        if (path == null) {
            detailPayload = entry.raw
            detailError = null
            detailLoading = false
            return@LaunchedEffect
        }
        detailLoading = true
        detailError = null
        try {
            detailPayload = ClashKingLookupApi.get(path)
        } catch (error: Exception) {
            detailPayload = null
            detailError = error.message ?: "Chưa có dữ liệu cho mục này."
        } finally {
            detailLoading = false
        }
    }

    val entry = selectedEntry
    if (entry != null) {
        val sections = if (entry.isClan) {
            listOf("TỔNG QUAN", "HỘI CHIẾN", "QUYÊN GÓP", "KỶ LỤC", "LỊCH SỬ")
        } else {
            listOf("TỔNG QUAN", "HOẠT ĐỘNG", "HỘI CHIẾN", "XẾP HẠNG", "LỊCH SỬ")
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
                        detailPayload = null
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
                    if (entry.isClan && entry.badgeUrl.isNotBlank()) {
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

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                sections.forEach { section ->
                    FilterChip(
                        selected = selectedSection == section,
                        onClick = { selectedSection = section },
                        label = { Text(section, fontSize = 9.sp, maxLines = 1) }
                    )
                }
            }

            when {
                detailLoading -> Box(Modifier.fillMaxWidth().padding(28.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = LookupCyan)
                }
                detailError != null -> LookupMessageCard(detailError ?: "Không tải được dữ liệu", isError = true)
                detailPayload != null -> {
                    val fields = flattenLookupJson(detailPayload ?: JSONObject())
                    if (fields.isEmpty()) {
                        LookupMessageCard("API chưa có dữ liệu cho mục này.")
                    } else {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(9.dp)
                            ) {
                                Text(
                                    when (selectedSection) {
                                        "TỔNG QUAN" -> "THÔNG TIN CHÍNH"
                                        "HOẠT ĐỘNG" -> "THỐNG KÊ HOẠT ĐỘNG"
                                        "HỘI CHIẾN" -> "DỮ LIỆU HỘI CHIẾN"
                                        "XẾP HẠNG" -> "THỨ HẠNG"
                                        "QUYÊN GÓP" -> "THỐNG KÊ QUYÊN GÓP"
                                        "KỶ LỤC" -> "KỶ LỤC BANG HỘI"
                                        else -> "LỊCH SỬ GHI NHẬN"
                                    },
                                    fontSize = 11.sp,
                                    color = LookupCyan,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.7.sp
                                )
                                fields.forEachIndexed { index, field ->
                                    LookupDetailRow(field.first, field.second)
                                    if (index < fields.lastIndex) {
                                        androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Text(
                "Dữ liệu công khai từ ClashKing API. Lịch sử và thống kê phụ thuộc dữ liệu được ghi nhận, có thể không đầy đủ.",
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
            Text("Tìm người chơi và bang hội bằng tên hoặc TAG/ID", fontSize = 10.sp, color = LookupMuted)
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
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                }

                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(if (searchMode == "player") "Tên người chơi hoặc #TAG" else "Tên bang hội hoặc #TAG") },
                    placeholder = { Text(if (searchMode == "player") "Ví dụ: tên hoặc #ABC123" else "Ví dụ: tên clan hoặc #XYZ789") },
                    leadingIcon = { Icon(Icons.Outlined.Search, null, tint = LookupCyan) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LookupCyan,
                        focusedLabelColor = LookupCyan,
                        cursorColor = LookupCyan
                    )
                )

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
                        Text("TÌM KIẾM", fontWeight = FontWeight.Black, letterSpacing = 0.5.sp)
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
                        detailPayload = null
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
                if (searchMode == "player") "Có thể tìm bằng tên người chơi hoặc TAG/ID bắt đầu bằng #."
                else "Có thể tìm bằng tên bang hội hoặc TAG/ID bắt đầu bằng #."
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            Icon(Icons.Outlined.Security, contentDescription = null, tint = LookupMuted, modifier = Modifier.size(14.dp))
            Text(
                "Tra cứu dữ liệu game công khai • COC-LIO không yêu cầu mật khẩu tài khoản Clash of Clans",
                fontSize = 9.sp,
                lineHeight = 13.sp,
                color = LookupMuted
            )
        }
        Text(
            "Nguồn dữ liệu: ClashKing API. Đây là công cụ cộng đồng độc lập, không phải ứng dụng chính thức của Supercell.",
            fontSize = 9.sp,
            color = LookupMuted,
            lineHeight = 13.sp
        )
    }
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
            if (entry.isClan && entry.badgeUrl.isNotBlank()) {
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
                        entry.level.takeIf { it.isNotBlank() }?.let { "Cấp $it" },
                        entry.members.takeIf { it.isNotBlank() }?.let { "$it thành viên" }
                    ).joinToString(" • ")
                } else {
                    listOfNotNull(
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
