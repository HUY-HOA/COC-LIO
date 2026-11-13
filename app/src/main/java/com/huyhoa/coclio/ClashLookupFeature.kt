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
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current

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
