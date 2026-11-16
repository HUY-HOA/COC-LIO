package com.huyhoa.coclio

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material3.Icon
import androidx.compose.ui.window.Dialog
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
import androidx.compose.material3.Slider
import androidx.compose.material3.Text as MaterialText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.huyhoa.coclio.data.CocAssets
import com.huyhoa.coclio.data.CocUpgradeCatalog
import com.huyhoa.coclio.data.CocUpgradeEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale

private enum class ClashLookupKind { PLAYER, CLAN }

private data class ClashLookupResponse(
    val kind: ClashLookupKind,
    val profile: JSONObject,
    val rankings: JSONObject? = null
)

private data class ClashLookupCandidate(
    val kind: ClashLookupKind,
    val profile: JSONObject
)

private data class ClashLookupSearchPage(
    val candidates: List<ClashLookupCandidate>,
    val nextCursor: String?,
    val nextQuery: String? = null
)

private const val CLASHKING_API = "https://api.clashk.ing"

private fun normalizeClashTag(value: String): String {
    val compact = value.uppercase(Locale.ROOT).filter {
        it in 'A'..'Z' || it in '0'..'9'
    }
    val tag = "#$compact"
    require(Regex("^#[PYLQGRJCUV0289]{3,15}$").matches(tag)) {
        "Mã không hợp lệ. Hãy nhập mã người chơi hoặc Clan Tag dạng #2PYLQRC."
    }
    return tag
}

private fun clashProfileIdentityValues(profile: JSONObject): List<String> = listOf(
    profile.optString("tag"),
    profile.optString("playerTag"),
    profile.optString("player_tag"),
    profile.optString("clanTag"),
    profile.optString("clan_tag"),
    profile.optString("id"),
    profile.optString("clanId"),
    profile.optString("clan_id"),
    profile.optString("playerId"),
    profile.optString("player_id"),
    profile.optJSONObject("clan")?.optString("tag").orEmpty()
).filter(String::isNotBlank)

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

private suspend fun searchClashKing(
    kind: ClashLookupKind,
    query: String,
    cursor: String? = null,
    townHallFilter: Int? = null,
    minMembersFilter: Int? = null,
    cursorQuery: String? = null
): ClashLookupSearchPage = withContext(Dispatchers.IO) {
    val term = query.trim()
    require(term.length in 2..40) {
        "Hãy nhập ít nhất 2 ký tự, tên hoặc mã tag cần tìm."
    }

    val exactTag = runCatching { normalizeClashTag(term) }.getOrNull()
    if (kind == ClashLookupKind.CLAN && exactTag != null && cursor == null) {
        val encodedTag = URLEncoder.encode(exactTag, "UTF-8")
        val directProfile = runCatching {
            readClashKingJson("/v2/clan/$encodedTag/cached") as? JSONObject
        }.getOrNull()
        if (directProfile != null && directProfile.optString("tag").isNotBlank()) {
            return@withContext ClashLookupSearchPage(
                listOf(ClashLookupCandidate(ClashLookupKind.CLAN, directProfile)),
                null
            )
        }
    }

    val variants = if (exactTag != null) {
        listOf(exactTag, exactTag.removePrefix("#"), term).distinct()
    } else {
        clashSearchTerms(term)
    }
    val requestTerms = if (cursor == null) {
        variants.take(8)
    } else {
        listOf(cursorQuery?.takeIf(String::isNotBlank) ?: variants.firstOrNull() ?: term)
    }
    val responses = requestTerms.map { searchTerm ->
        async {
            runCatching {
                val encodedQuery = URLEncoder.encode(searchTerm, "UTF-8")
                val encodedCursor = cursor?.let {
                    "&cursor=" + URLEncoder.encode(it, "UTF-8")
                }.orEmpty()
                val path = when (kind) {
                    ClashLookupKind.PLAYER -> {
                        val filter = townHallFilter?.let { "&townhallLevels=$it" }.orEmpty()
                        "/v2/player/search?query=$encodedQuery&limit=20$filter$encodedCursor"
                    }
                    ClashLookupKind.CLAN -> {
                        val filter = minMembersFilter?.let {
                            "&" + URLEncoder.encode("members[min]", "UTF-8") + "=$it"
                        }.orEmpty()
                        "/v2/clan/search?query=$encodedQuery&limit=20$filter$encodedCursor"
                    }
                }
                val search = readClashKingJson(path) as? JSONObject
                    ?: throw IllegalStateException("Dữ liệu tìm kiếm không đúng định dạng.")
                val items = search.optJSONArray("items") ?: JSONArray()
                val candidates = (0 until items.length()).mapNotNull { index ->
                    items.optJSONObject(index)?.let { ClashLookupCandidate(kind, it) }
                }
                val nextCursor = search.optJSONObject("pagination")
                    ?.opt("nextCursor") as? String
                Triple(candidates, nextCursor?.takeIf { it.isNotBlank() }, searchTerm)
            }
        }
    }.awaitAll()

    val successful = responses.mapNotNull { it.getOrNull() }
    if (successful.isEmpty()) {
        throw responses.firstNotNullOfOrNull { it.exceptionOrNull() }
            ?: IllegalStateException("Không thể tìm kiếm lúc này. Hãy thử lại.")
    }

    val uniqueCandidates = LinkedHashMap<String, ClashLookupCandidate>()
    successful.flatMap { it.first }.forEach { candidate ->
        val profileTag = candidate.profile.optString("tag").uppercase(Locale.ROOT)
        val key = if (profileTag.isNotBlank()) profileTag
        else normalizeClashSearchText(candidate.profile.optString("name"))
        if (key.isNotBlank()) uniqueCandidates.putIfAbsent(key, candidate)
    }

    val allCandidates = uniqueCandidates.values.toList()
    val normalizedQuery = normalizeClashSearchText(term)
    val matchedCandidates = allCandidates.filter { candidate ->
        val profile = candidate.profile
        val identityValues = clashProfileIdentityValues(profile)
        val identityMatch = identityValues.any { value ->
            val normalizedIdentity = normalizeClashSearchText(value)
            normalizedIdentity == normalizedQuery ||
                (normalizedQuery.length >= 3 && normalizedIdentity.contains(normalizedQuery)) ||
                (exactTag != null && runCatching { normalizeClashTag(value) }.getOrNull() == exactTag)
        }
        val nameMatch = clashNameMatchesSearch(profile.optString("name"), term)
        if (exactTag != null) identityMatch else nameMatch || identityMatch
    }

    val matchedTags = matchedCandidates.map { it.profile.optString("tag").uppercase(Locale.ROOT) }.toSet()
    val primaryResponse = successful.firstOrNull { response ->
        response.first.any { it.profile.optString("tag").uppercase(Locale.ROOT) in matchedTags }
    } ?: successful.firstOrNull { it.third == requestTerms.first() }
        ?: successful.first()
    ClashLookupSearchPage(
        candidates = matchedCandidates,
        nextCursor = primaryResponse.second,
        nextQuery = primaryResponse.third.takeIf { primaryResponse.second != null }
    )
}

private suspend fun loadClashKingProfile(candidate: ClashLookupCandidate): ClashLookupResponse =
    withContext(Dispatchers.IO) {
        val tag = normalizeClashTag(candidate.profile.optString("tag"))
        val encodedTag = URLEncoder.encode(tag, "UTF-8")
        when (candidate.kind) {
            ClashLookupKind.PLAYER -> {
                val rankings = runCatching {
                    readClashKingJson("/v2/player/$encodedTag/rankings") as? JSONObject
                }.getOrNull()
                ClashLookupResponse(ClashLookupKind.PLAYER, candidate.profile, rankings)
            }
            ClashLookupKind.CLAN -> {
                val clan = readClashKingJson("/v2/clan/$encodedTag/cached") as? JSONObject
                    ?: throw IllegalStateException("Chưa có hồ sơ bang hội $tag trong kho dữ liệu ClashKing.")
                val rankings = runCatching {
                    readClashKingJson("/v2/clan/$encodedTag/rankings") as? JSONObject
                }.getOrNull()
                ClashLookupResponse(ClashLookupKind.CLAN, clan, rankings)
            }
        }
    }

@Composable
fun ClashLookupScreen() {
    val english = LocalAppLanguage.current == AppUiLanguage.ENGLISH
    var mode by remember { mutableStateOf("player") }
    Column(modifier = Modifier.fillMaxSize()) {
        LookupCard {
            Text(
                if (english) "ID LOOKUP" else "TRUNG TÂM TRA CỨU ID",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                if (english) {
                    "Choose what you want to find. Search by player or clan name, in-game tag, or game data ID."
                } else {
                    "Chọn đúng loại dữ liệu cần tìm: người chơi, bang hội hoặc mã vật phẩm trong trò chơi."
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp,
                lineHeight = 15.sp
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = mode == "player",
                    onClick = { mode = "player" },
                    leadingIcon = {
                        Icon(Icons.Outlined.Person, contentDescription = null, modifier = Modifier.size(16.dp))
                    },
                    label = { Text(if (english) "Player" else "Người chơi", fontSize = 9.sp) }
                )
                FilterChip(
                    selected = mode == "clan",
                    onClick = { mode = "clan" },
                    leadingIcon = {
                        Icon(Icons.Outlined.Groups, contentDescription = null, modifier = Modifier.size(16.dp))
                    },
                    label = { Text(if (english) "Clan" else "Bang hội", fontSize = 9.sp) }
                )
                FilterChip(
                    selected = mode == "game-data",
                    onClick = { mode = "game-data" },
                    leadingIcon = {
                        Icon(Icons.Outlined.Tag, contentDescription = null, modifier = Modifier.size(16.dp))
                    },
                    label = { Text(if (english) "Game ID" else "ID trò chơi", fontSize = 9.sp) }
                )
            }
        }
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (mode) {
                "player" -> ClashPlayerClanLookupScreen(ClashLookupKind.PLAYER)
                "clan" -> ClashPlayerClanLookupScreen(ClashLookupKind.CLAN)
                else -> ClashGameDataLookupScreen()
            }
        }
    }
}

@Composable
private fun ClashGameDataLookupScreen() {
    val context = LocalContext.current
    var catalog by remember { mutableStateOf<List<CocUpgradeEntry>>(emptyList()) }
    var catalogWarnings by remember { mutableStateOf<List<String>>(emptyList()) }
    var query by remember { mutableStateOf("") }
    var baseFilter by remember { mutableStateOf("all") }
    var categoryFilter by remember { mutableStateOf("all") }
    var rarityFilter by remember { mutableStateOf("all") }
    var heroFilter by remember { mutableStateOf("all") }
    var advancedFiltersExpanded by remember { mutableStateOf(false) }
    var selectedEntry by remember { mutableStateOf<CocUpgradeEntry?>(null) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(context) {
        val loaded = withContext(Dispatchers.IO) {
            CocUpgradeCatalog.all(context).values.toList() to CocUpgradeCatalog.warnings(context)
        }
        catalog = loaded.first
        catalogWarnings = loaded.second
        loading = false
    }

    val results = remember(catalog, query, baseFilter, categoryFilter, rarityFilter, heroFilter) {
        if (query.isBlank()) emptyList()
        else CocUpgradeCatalog.search(catalog, query, baseFilter, categoryFilter, 40, rarityFilter, heroFilter)
    }
    val categoryOptions = remember(catalog) {
        listOf("all") + catalog.map { it.category }.filter { it.isNotBlank() }.distinct().sorted()
    }
    val rarityOptions = remember(catalog) { catalog.map { it.rarity }.filter { it.isNotBlank() }.distinct().sorted() }
    val heroOptions = remember(catalog) { catalog.map { it.hero }.filter { it.isNotBlank() }.distinct().sorted() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            "TRA CỨU ID VẬT PHẨM VÀ ĐƠN VỊ",
            color = MaterialTheme.colorScheme.primary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Black
        )
        Text(
            "Tìm theo mã số, tên tiếng Việt có dấu/không dấu, tên tiếng Anh hoặc tên gọi khác. Chọn một kết quả để xem cấp, chỉ số và chi phí nâng cấp.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.sp,
            lineHeight = 15.sp
        )
        OutlinedTextField(
            value = query,
            onValueChange = {
                query = it
                selectedEntry = null
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("ID hoặc tên trong game") },
            placeholder = { Text("Ví dụ: 4000008, 90000061, Rồng, Portal Pendant") },
            singleLine = true
        )
        Text("CĂN CỨ", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("all" to "TẤT CẢ", "home" to "LÀNG CHÍNH", "builder" to "THỢ XÂY").forEach { (key, label) ->
                FilterChip(
                    selected = baseFilter == key,
                    onClick = {
                        baseFilter = key
                        selectedEntry = null
                    },
                    label = { Text(label, fontSize = 9.sp) }
                )
            }
        }
        Text("NHÓM DỮ LIỆU", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            categoryOptions.forEach { key ->
                FilterChip(
                    selected = categoryFilter == key,
                    onClick = {
                        categoryFilter = key
                        selectedEntry = null
                    },
                    label = { Text(if (key == "all") "TẤT CẢ" else gameDataCategoryLabel(key), fontSize = 9.sp) }
                )
            }
        }
        OutlinedButton(
            onClick = { advancedFiltersExpanded = !advancedFiltersExpanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (advancedFiltersExpanded) "ẨN BỘ LỌC NÂNG CAO" else "BỘ LỌC NÂNG CAO")
        }
        if (advancedFiltersExpanded && rarityOptions.isNotEmpty() && categoryFilter in setOf("all", "hero-equipment")) {
            Text("ĐỘ HIẾM", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(selected = rarityFilter == "all", onClick = { rarityFilter = "all" }, label = { Text("Tất cả", fontSize = 9.sp) })
                rarityOptions.forEach { key ->
                    FilterChip(selected = rarityFilter == key, onClick = { rarityFilter = key }, label = { Text(gameDataRarityLabel(key), fontSize = 9.sp) })
                }
            }
        }
        if (advancedFiltersExpanded && heroOptions.isNotEmpty() && categoryFilter in setOf("all", "hero-equipment")) {
            Text("ANH HÙNG SỬ DỤNG", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(selected = heroFilter == "all", onClick = { heroFilter = "all" }, label = { Text("Tất cả", fontSize = 9.sp) })
                heroOptions.forEach { key ->
                    FilterChip(selected = heroFilter == key, onClick = { heroFilter = key }, label = { Text(gameDataHeroLabel(key), fontSize = 9.sp) })
                }
            }
        }
        if (loading) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
                Text("ĐANG TẢI DANH MỤC ID...", fontSize = 10.sp)
            }
        } else {
            LookupValue("Số ID trong danh mục", catalog.size.toString())
            Text("Nhập ID để tra cứu chính xác hoặc nhập tên để tìm gần đúng.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
        }
        catalogWarnings.forEach { warning ->
            LookupCard {
                Text("CẢNH BÁO DỮ LIỆU", color = MaterialTheme.colorScheme.error, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text(warning, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
            }
        }
        if (query.isBlank() && !loading) {
            LookupCard {
                Text("Bắt đầu tra cứu", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text(
                    "Nhập mã ID hoặc tên. Ví dụ: 4000008 (Rồng), 90000061 (trang bị Anh hùng), hoặc tên công trình/đơn vị.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp,
                    lineHeight = 15.sp
                )
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("4000008", "90000061", "Rồng").forEach { sample ->
                        FilterChip(
                            selected = false,
                            onClick = { query = sample; selectedEntry = null },
                            label = { Text(sample, fontSize = 9.sp) }
                        )
                    }
                }
            }
        }
        if (query.isNotBlank() && results.isEmpty() && !loading) {
            LookupCard {
                Text("Không tìm thấy ID phù hợp.", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                Text("Kiểm tra lại mã hoặc tên. ID chưa có trong danh mục sẽ vẫn được giữ nguyên trong dữ liệu JSON gốc.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
            }
        }
        if (results.isNotEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("KẾT QUẢ", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text("• ${results.size}", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
            }
            results.forEach { entry ->
                val isEnglish = LocalAppLanguage.current == AppUiLanguage.ENGLISH
                val itemName = if (isEnglish) entry.name else entry.localizedName
                LookupCard {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        val imageUrl = CocAssets.imageUrl(entry)
                        if (!imageUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = imageUrl,
                                contentDescription = itemName,
                                modifier = Modifier.size(62.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(itemName, color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("ID: ${entry.dataId}", color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, fontWeight = FontWeight.Black)
                            Text("${if (entry.base == "builder") "Căn cứ thợ xây" else "Làng chính"} • ${gameDataCategoryLabel(entry.category)}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
                            LookupValue("Cấp tối đa", entry.maxLevel.toString())
                        }
                    }
                    Button(onClick = { selectedEntry = entry }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (selectedEntry?.dataId == entry.dataId) "ĐANG CHỌN" else "XEM CHI TIẾT")
                    }
                }
            }
            if (results.size == 40) {
                Text("Đang giới hạn 40 kết quả đầu tiên. Hãy nhập thêm ký tự để thu hẹp tìm kiếm.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
            }
        }
        selectedEntry?.let { entry ->
            GameDataLookupDetail(entry, catalog.associateBy { it.dataId }) {
                selectedEntry = null
            }
        }
        Text(
            "Nguồn: bộ dữ liệu cộng đồng MIT, đối chiếu theo các thông báo cập nhật của Supercell. Dữ liệu ID, cấp và chỉ số có thể thay đổi sau bản cập nhật game.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 9.sp
        )
    }
}

@Composable
private fun GameDataLookupDetail(
    entry: CocUpgradeEntry,
    catalog: Map<Int, CocUpgradeEntry>,
    onDismiss: () -> Unit
) {
    val levels = entry.levels.map { it.level }.distinct().sorted()
    var selectedIndex by remember(entry.dataId, levels) { mutableStateOf((levels.lastIndex).coerceAtLeast(0)) }
    var planStartIndex by remember(entry.dataId, levels) { mutableStateOf(0) }
    var planTargetIndex by remember(entry.dataId, levels) { mutableStateOf((levels.lastIndex).coerceAtLeast(0)) }
    val selectedLevel = levels.getOrNull(selectedIndex) ?: entry.levels.firstOrNull()?.level ?: 1
    val levelData = entry.level(selectedLevel)
    val english = LocalAppLanguage.current == AppUiLanguage.ENGLISH
    val title = if (english) entry.name else entry.localizedName
    val nextLevelData = entry.level(selectedLevel + 1)
    val requirements = nextLevelData?.let { CocUpgradeCatalog.nextRequirements(entry, selectedLevel + 1, catalog) }.orEmpty()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().heightIn(max = 660.dp),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            val imageUrl = CocAssets.imageUrl(entry)
            if (!imageUrl.isNullOrBlank()) {
                AsyncImage(model = imageUrl, contentDescription = title, modifier = Modifier.size(72.dp))
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Black)
                Text("ID dữ liệu: ${entry.dataId}", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.Black)
                Text(entry.id, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
            }
        }
        LookupValue("Căn cứ", if (entry.base == "builder") "Căn cứ thợ xây" else "Làng chính")
        LookupValue("Phân loại", gameDataCategoryLabel(entry.category))
        if (entry.rarity.isNotBlank()) LookupValue("Độ hiếm", gameDataRarityLabel(entry.rarity))
        entry.hero.takeIf { it.isNotBlank() }?.let { LookupValue("Anh hùng sử dụng", gameDataHeroLabel(it)) }
        LookupValue("Cấp tối đa trong dữ liệu", entry.maxLevel.toString())
        val recordedRegularMax = entry.levels.maxOfOrNull { it.level } ?: 0
        if (recordedRegularMax > 0 && recordedRegularMax < entry.maxLevel) {
            LookupValue("Dữ liệu chỉ số", "$recordedRegularMax/${entry.maxLevel}")
            Text("Các cấp còn thiếu chưa được tự ước đoán.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
        }
        val localizedDescription = if (english) entry.description else {
            EquipmentDescriptionsVi[entry.dataId] ?: CatalogDescriptionsVi[entry.dataId] ?: entry.description
        }
        if (localizedDescription.isNotBlank()) {
            Text(localizedDescription, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, lineHeight = 15.sp)
        }
        if (levels.isNotEmpty()) {
            Text("CHỈ SỐ THEO CẤP", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 10.sp)
            LookupValue("Cấp đang xem", "$selectedLevel (${selectedIndex + 1}/${levels.size})")
            if (levels.size > 1) {
                Slider(
                    value = selectedIndex.toFloat(),
                    onValueChange = { selectedIndex = it.toInt().coerceIn(levels.indices) },
                    valueRange = 0f..levels.lastIndex.toFloat(),
                    steps = (levels.size - 2).coerceAtLeast(0)
                )
            }
            levelData?.displayStats()?.forEach { (rawLabel, rawValue) ->
                val normalizedLabel = rawLabel.removePrefix("stats_").removePrefix("normal_")
                val (label, value) = localizedCatalogField(normalizedLabel, rawValue, english)
                LookupValue(label, value)
            }
            levelData?.let { level ->
                if (level.cost() > 0L) {
                    LookupResourceValue(level.resource(), "Cấp $selectedLevel: ${lookupNumber(level.cost())}")
                }
                level.oreCosts().forEach { (name, amount) ->
                    LookupResourceValue(name, lookupNumber(amount))
                }
                if (level.durationSeconds() > 0L) {
                    LookupIconValue(R.drawable.ic_coc_time, "Thời gian nâng cấp", lookupDuration(level.durationSeconds()))
                }
            }
            if (requirements.isNotEmpty()) {
                LookupValue("Yêu cầu mở cấp tiếp theo", requirements.joinToString(" • ") { "${it.label} cấp ${it.level}" })
            }
            if (levels.size > 1) {
                Text("TÍNH TỔNG NÂNG CẤP THEO KHOẢNG CẤP", color = MaterialTheme.colorScheme.primary, fontSize = 10.sp, fontWeight = FontWeight.Black)
                LookupValue("Từ cấp", levels[planStartIndex].toString())
                Slider(
                    value = planStartIndex.toFloat(),
                    onValueChange = { planStartIndex = it.toInt().coerceIn(0, planTargetIndex) },
                    valueRange = 0f..levels.lastIndex.toFloat(),
                    steps = (levels.size - 2).coerceAtLeast(0)
                )
                LookupValue("Đến cấp", levels[planTargetIndex].toString())
                Slider(
                    value = planTargetIndex.toFloat(),
                    onValueChange = { planTargetIndex = it.toInt().coerceIn(planStartIndex, levels.lastIndex) },
                    valueRange = 0f..levels.lastIndex.toFloat(),
                    steps = (levels.size - 2).coerceAtLeast(0)
                )
                if (planTargetIndex > planStartIndex) {
                    val planTotals = CocUpgradeCatalog.upgradeTotals(
                        entry,
                        levels[planStartIndex],
                        levels[planTargetIndex]
                    )
                    planTotals.resources.forEach { (resource, amount) ->
                        LookupResourceValue(resource, lookupNumber(amount))
                    }
                    if (planTotals.durationSeconds > 0L) {
                        LookupValue("Tổng thời gian nâng cấp", lookupDuration(planTotals.durationSeconds))
                    }
                    if (planTotals.resources.isEmpty() && planTotals.durationSeconds == 0L) {
                        LookupValue("Kết quả tính toán", "Nguồn hiện chưa có chi phí hoặc thời gian cho khoảng cấp này.")
                    }
                } else {
                    LookupValue("Khoảng cấp", "Chọn cấp đích lớn hơn cấp bắt đầu để tính tổng.")
                }
            }
            if (nextLevelData == null && selectedLevel >= entry.maxLevel) {
                LookupValue("Trạng thái", "Đã ở cấp tối đa theo dữ liệu hiện có")
            }
        }

        if (entry.superchargeLevels.isNotEmpty()) {
            val highestSuperchargeTier = entry.superchargeLevels.maxOf { it.level }
            val superchargeTotals = CocUpgradeCatalog.superchargeTotals(entry, 0, highestSuperchargeTier)
            Text("SIÊU CƯỜNG HÓA", color = Color(0xFFFFC857), fontSize = 10.sp, fontWeight = FontWeight.Black)
            LookupValue("Số bậc dữ liệu", highestSuperchargeTier.toString())
            entry.superchargeLevels.forEach { tier ->
                Text("BẬC SIÊU CƯỜNG HÓA", color = Color(0xFFFFC857), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text("CẤP ${tier.level}", color = Color(0xFFFFC857), fontSize = 9.sp)
                tier.displayStats().forEach { (rawLabel, rawValue) ->
                    val normalizedLabel = rawLabel.removePrefix("stats_").removePrefix("normal_")
                    val (label, value) = localizedCatalogField(normalizedLabel, rawValue, english)
                    LookupValue(label, value)
                }
                if (tier.cost() > 0L) {
                    LookupValue("Chi phí bậc", "Cấp ${tier.level}: ${lookupNumber(tier.cost())} ${gameDataResourceLabel(tier.resource())}")
                }
                tier.oreCosts().forEach { (name, amount) -> LookupResourceValue(name, lookupNumber(amount)) }
                if (tier.durationSeconds() > 0L) {
                    LookupValue("Thời gian bậc", "Cấp ${tier.level}: ${lookupDuration(tier.durationSeconds())}")
                }
                tier.int("townHallRequired", "builderHallLevelRequired", "builderHallRequired")?.let {
                    LookupValue("Yêu cầu Nhà chính/Nhà thợ xây", "Cấp $it")
                }
            }
            superchargeTotals.resources.forEach { (resource, amount) ->
                LookupValue("Tổng chi phí siêu cường hóa", "$resource: ${lookupNumber(amount)}")
            }
            if (superchargeTotals.durationSeconds > 0L) {
                LookupValue("Tổng thời gian siêu cường hóa", lookupDuration(superchargeTotals.durationSeconds))
            }
            Text(
                "Các bậc siêu cường hóa được tách riêng khỏi cấp thường; đây là dữ liệu tham khảo, không phải trạng thái siêu cường hóa hiện tại của làng.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 9.sp
            )
        }
                OutlinedButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("ĐÓNG CHI TIẾT")
                }
            }
        }
    }
}

private fun gameDataCategoryLabel(category: String): String = when (category) {
    "troop" -> "Quân lính"
    "siege-machine" -> "Máy công thành"
    "hero" -> "Anh hùng"
    "hero-equipment" -> "Trang bị anh hùng"
    "spell" -> "Thần chú"
    "pet" -> "Linh thú"
    "guardian" -> "Hộ vệ"
    "defense" -> "Phòng thủ"
    "resource" -> "Tài nguyên"
    "army" -> "Công trình quân đội"
    "research" -> "Nghiên cứu"
    "trap" -> "Bẫy"
    "wall" -> "Tường"
    "other" -> "Khác"
    "town-hall" -> "Nhà chính"
    "builder-hall" -> "Nhà thợ xây"
    else -> category.replace('-', ' ').replaceFirstChar { it.uppercase() }
}

private fun gameDataRarityLabel(rarity: String): String = when (rarity.lowercase()) {
    "common" -> "Thường"
    "rare" -> "Hiếm"
    "epic" -> "Sử thi"
    "legendary" -> "Huyền thoại"
    else -> rarity
}

private fun gameDataHeroLabel(hero: String): String = when (hero.lowercase()) {
    "barbarian-king" -> "Vua man di"
    "archer-queen" -> "Nữ hoàng cung thủ"
    "grand-warden" -> "Đại hộ vệ"
    "royal-champion" -> "Nhà vô địch Hoàng gia"
    "minion-prince" -> "Hoàng tử tiểu quỷ"
    "dragon-duke" -> "Công tước Rồng"
    else -> hero.replace('-', ' ').replaceFirstChar { it.uppercase() }
}

private fun gameDataResourceLabel(resource: String): String = when (resource.lowercase()) {
    "gold" -> "Vàng"
    "elixir" -> "Tiên dược"
    "dark elixir" -> "Dầu đen"
    "builder gold" -> "Vàng thợ xây"
    "builder elixir" -> "Tiên dược thợ xây"
    "capital gold" -> "Vàng Thủ đô"
    "shiny ore", "quặng sáng" -> "Quặng sáng"
    "glowy ore", "quặng phát sáng" -> "Quặng phát sáng"
    "starry ore", "quặng sao" -> "Quặng sao"
    else -> resource
}

private fun lookupNumber(value: Long): String =
    java.text.NumberFormat.getIntegerInstance().format(value)

private fun lookupDuration(totalSeconds: Long): String {
    val days = totalSeconds / 86400L
    val hours = (totalSeconds % 86400L) / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    return buildList {
        if (days > 0L) add("$days ngày")
        if (hours > 0L) add("$hours giờ")
        if (minutes > 0L) add("$minutes phút")
        if (isEmpty()) add("Dưới 1 phút")
    }.joinToString(" ")
}

@Composable
private fun ClashPlayerClanLookupScreen(initialKind: ClashLookupKind) {
    var kind by remember(initialKind) { mutableStateOf(initialKind) }
    var tagInput by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<ClashLookupResponse?>(null) }
    var searchResults by remember { mutableStateOf<List<ClashLookupCandidate>>(emptyList()) }
    var searchNextCursor by remember { mutableStateOf<String?>(null) }
    var searchNextQuery by remember { mutableStateOf<String?>(null) }
    var searchFiltersExpanded by remember { mutableStateOf(false) }
    var searchTownHallFilter by remember { mutableStateOf("") }
    var searchMinMembersFilter by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var detailLoading by remember { mutableStateOf(false) }
    var detailTitle by remember { mutableStateOf<String?>(null) }
    var detailText by remember { mutableStateOf<Any?>(null) }
    var detailError by remember { mutableStateOf<String?>(null) }
    var showProfileTools by remember { mutableStateOf(false) }
    var selectedActionCategory by remember { mutableStateOf("Tất cả") }
    var showGlobalTools by remember { mutableStateOf(false) }
    var townHallInput by remember { mutableStateOf("") }
    var leagueTierInput by remember { mutableStateOf("") }
    var locationInput by remember { mutableStateOf("") }
    var comparePlayerTagInput by remember { mutableStateOf("") }
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
            if (kind == ClashLookupKind.PLAYER) "TRA CỨU NGƯỜI CHƠI" else "TRA CỨU BANG HỘI",
            color = MaterialTheme.colorScheme.primary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Black
        )
        Text(
            if (kind == ClashLookupKind.PLAYER) {
                "Tìm bằng tên trong game hoặc mã người chơi (Player Tag). Có thể lọc theo cấp Nhà chính."
            } else {
                "Tìm bằng tên bang hội hoặc Clan Tag. Có thể lọc theo số thành viên tối thiểu."
            },
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.sp,
            lineHeight = 15.sp
        )
        OutlinedTextField(
            value = tagInput,
            onValueChange = {
                tagInput = it
                result = null
                searchResults = emptyList()
                searchNextCursor = null
                searchNextQuery = null
                error = null
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(if (kind == ClashLookupKind.PLAYER) "Tên hoặc mã người chơi" else "Tên hoặc mã bang hội (Clan Tag)") },
            placeholder = { Text(if (kind == ClashLookupKind.PLAYER) "HUY⭐⭐⭐HÒA hoặc #2PYLQRC" else "LIO⭐⭐⭐ONE hoặc #2PYLQRC") },
            singleLine = true,
            enabled = !loading
        )
        OutlinedButton(
            onClick = { searchFiltersExpanded = !searchFiltersExpanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (searchFiltersExpanded) "ẨN LỌC NÂNG CAO" else "LỌC NÂNG CAO")
        }
        if (searchFiltersExpanded) {
            if (kind == ClashLookupKind.PLAYER) {
                OutlinedTextField(
                    value = searchTownHallFilter,
                    onValueChange = {
                        searchTownHallFilter = it.filter(Char::isDigit).take(2)
                        result = null
                        searchResults = emptyList()
                        searchNextCursor = null
                        searchNextQuery = null
                        error = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Lọc theo cấp Nhà chính (1–20)") },
                    singleLine = true,
                    enabled = !loading
                )
            } else {
                OutlinedTextField(
                    value = searchMinMembersFilter,
                    onValueChange = {
                        searchMinMembersFilter = it.filter(Char::isDigit).take(2)
                        result = null
                        searchResults = emptyList()
                        searchNextCursor = null
                        searchNextQuery = null
                        error = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Số thành viên tối thiểu (1–50)") },
                    singleLine = true,
                    enabled = !loading
                )
            }
        }
        Button(
            onClick = {
                scope.launch {
                    val townHallFilter = searchTownHallFilter.toIntOrNull()?.takeIf { it in 1..20 }
                    val minMembersFilter = searchMinMembersFilter.toIntOrNull()?.takeIf { it in 1..50 }
                    if (kind == ClashLookupKind.PLAYER && searchTownHallFilter.isNotBlank() && townHallFilter == null) {
                        error = "Cấp Nhà chính phải từ 1 đến 20."
                        return@launch
                    }
                    if (kind == ClashLookupKind.CLAN && searchMinMembersFilter.isNotBlank() && minMembersFilter == null) {
                        error = "Số thành viên tối thiểu phải từ 1 đến 50."
                        return@launch
                    }
                    loading = true
                    error = null
                    result = null
                    searchResults = emptyList()
                    searchNextCursor = null
                    searchNextQuery = null
                    runCatching {
                        searchClashKing(kind, tagInput, null, townHallFilter, minMembersFilter)
                    }
                        .onSuccess { page ->
                            if (page.candidates.isEmpty()) {
                                error = "Không tìm thấy kết quả phù hợp. Hãy kiểm tra tên hoặc mã tag."
                            } else {
                                searchResults = page.candidates
                                searchNextCursor = page.nextCursor
                                searchNextQuery = page.nextQuery
                            }
                        }
                        .onFailure {
                            error = it.message?.takeIf(String::isNotBlank)
                                ?: "Không thể tìm kiếm lúc này. Hãy thử lại."
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
            if (kind == ClashLookupKind.PLAYER) {
                "Có thể nhập tên có dấu, tên có ký hiệu hoặc mã người chơi dạng #ABC123. Đây là Player Tag trong game, không phải email hay Supercell ID."
            } else {
                "Có thể nhập tên có dấu, tên có ký hiệu hoặc Clan Tag dạng #2PYLQRC. Clan Tag khác với tên bang hội và Supercell ID."
            },
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.sp
        )

        error?.let {
            LookupCard {
                Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
            }
        }

        if (searchResults.isNotEmpty()) {
            Text(
                "CHỌN KẾT QUẢ TÌM ĐƯỢC",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Đã tải " + searchResults.size + " kết quả. Chọn đúng hồ sơ để xem chi tiết.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp
            )
            searchResults.forEach { candidate ->
                LookupCard {
                    Text(
                        candidate.profile.optString("name", "Không rõ tên"),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Text(
                        candidate.profile.optString("tag"),
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 11.sp
                    )
                    if (candidate.kind == ClashLookupKind.PLAYER) {
                        Text(
                            "Nhà chính " + candidate.profile.optInt("townHallLevel") +
                                "  •  Bang hội " + candidate.profile.optJSONObject("clan")?.optString("name").orEmpty().ifBlank { "Không có" },
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp
                        )
                    } else {
                        Text(
                            "Cấp bang hội " + candidate.profile.optInt("clanLevel") +
                                "  •  " + candidate.profile.optInt("members") + " thành viên",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp
                        )
                    }
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                loading = true
                                error = null
                                runCatching { loadClashKingProfile(candidate) }
                                    .onSuccess {
                                        result = it
                                        tagInput = candidate.profile.optString("tag")
                                        searchResults = emptyList()
                                        searchNextCursor = null
                                        searchNextQuery = null
                                    }
                                    .onFailure {
                                        error = it.message?.takeIf(String::isNotBlank)
                                            ?: "Không thể mở hồ sơ này."
                                    }
                                loading = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !loading
                    ) {
                        Text("MỞ HỒ SƠ")
                    }
                }
            }
            searchNextCursor?.let { cursor ->
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            loading = true
                            error = null
                            val townHallFilter = searchTownHallFilter.toIntOrNull()?.takeIf { it in 1..20 }
                            val minMembersFilter = searchMinMembersFilter.toIntOrNull()?.takeIf { it in 1..50 }
                            runCatching {
                                searchClashKing(kind, tagInput, cursor, townHallFilter, minMembersFilter, searchNextQuery)
                            }
                                .onSuccess { page ->
                                    searchResults = (searchResults + page.candidates).distinctBy { candidate ->
                                        candidate.profile.optString("tag").uppercase(Locale.ROOT)
                                            .ifBlank { normalizeClashSearchText(candidate.profile.optString("name")) }
                                    }
                                    searchNextCursor = page.nextCursor
                                    searchNextQuery = page.nextQuery
                                }
                                .onFailure {
                                    error = it.message?.takeIf(String::isNotBlank)
                                        ?: "Không thể tải thêm kết quả."
                                }
                            loading = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !loading
                ) {
                    Text(if (loading) "ĐANG TẢI..." else "TẢI THÊM KẾT QUẢ")
                }
            }
        }

        result?.let { data ->
            if (data.kind == ClashLookupKind.PLAYER) {
                PlayerLookupCard(data)
            } else {
                ClanLookupCard(data)
            }

            if (data.kind == ClashLookupKind.PLAYER) {
                LookupCard {
                    Text(
                        "SO SÁNH THỜI GIAN TRONG BANG HỘI",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    OutlinedTextField(
                        value = comparePlayerTagInput,
                        onValueChange = { comparePlayerTagInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Mã người chơi thứ hai") },
                        placeholder = { Text("#ABC123") },
                        singleLine = true
                    )
                    OutlinedButton(
                        onClick = {
                            val otherTag = runCatching { normalizeClashTag(comparePlayerTagInput) }.getOrNull()
                            if (otherTag == null) {
                                detailTitle = "So sánh người chơi"
                                detailText = null
                                detailError = "Mã người chơi thứ hai không hợp lệ."
                            } else {
                                val encodedOther = URLEncoder.encode(otherTag, "UTF-8")
                                val encodedCurrent = URLEncoder.encode(data.profile.optString("tag"), "UTF-8")
                                loadAction(
                                    ClashLookupAction(
                                        "Thời gian trong bang hội chung",
                                        "/v2/player/$encodedCurrent/join-leave/shared?tag=$encodedOther"
                                    )
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !detailLoading && comparePlayerTagInput.isNotBlank()
                    ) { Text("SO SÁNH") }
                }
            }

            val actions = clashLookupActions(data)
            val actionGroups = actions.groupBy(::clashLookupActionCategory)
            OutlinedButton(
                onClick = { showProfileTools = !showProfileTools },
                modifier = Modifier.fillMaxWidth(),
                enabled = !detailLoading
            ) {
                Text(
                    if (showProfileTools) "ẨN THỐNG KÊ VÀ LỊCH SỬ"
                    else "MỞ THỐNG KÊ VÀ LỊCH SỬ (${actions.size} mục)"
                )
            }
            if (showProfileTools) {
                Text(
                    "Chọn nhóm dữ liệu cần xem. Mỗi mục sẽ tải thông tin riêng từ ClashKing.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp
                )
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    (listOf("Tất cả") + actionGroups.keys).forEach { category ->
                        FilterChip(
                            selected = selectedActionCategory == category,
                            onClick = { selectedActionCategory = category },
                            label = { Text(category, fontSize = 9.sp) }
                        )
                    }
                }
                val visibleActions = if (selectedActionCategory == "Tất cả") actions
                    else actionGroups[selectedActionCategory].orEmpty()
                visibleActions.chunked(2).forEach { rowActions ->
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
        detailText?.let { dataValue ->
            LookupCard {
                Text(
                    detailTitle.orEmpty(),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                LookupJsonViewer(dataValue)
            }
        }

        OutlinedButton(
            onClick = { showGlobalTools = !showGlobalTools },
            modifier = Modifier.fillMaxWidth(),
            enabled = !detailLoading
        ) {
            Text(if (showGlobalTools) "ẨN CÔNG CỤ TOÀN GAME" else "CÔNG CỤ THỐNG KÊ TOÀN GAME")
        }
        if (showGlobalTools) {
        Text(
            "Thống kê cộng đồng, lịch mùa giải, dữ liệu chiến tranh và bảng xếp hạng công khai.",
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
                label = { Text("Mã cấp bậc giải đấu") },
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
            OutlinedButton(
                onClick = {
                    if (townHall != null && leagueTier != null) {
                        loadAction(
                            ClashLookupAction(
                                "Thống kê Ranked TH$townHall – Giải $leagueTier",
                                "/v2/stats/ranked?townHallLevel=$townHall&leagueTierId=$leagueTier"
                            )
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !detailLoading && townHall != null && townHall in 1..20 && leagueTier != null && leagueTier > 0
            ) { Text("THỐNG KÊ RANKED THEO NHÀ CHÍNH & GIẢI") }
            OutlinedButton(
                onClick = {
                    leagueTier?.let {
                        loadAction(ClashLookupAction("Phân bố cúp giải đấu $it", "/v2/leaderboard/$it/trophy-buckets"))
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !detailLoading && leagueTier != null && leagueTier > 0
            ) { Text("PHÂN BỐ CÚP GIẢI ĐẤU") }
            OutlinedTextField(
                value = locationInput,
                onValueChange = { locationInput = it.filter(Char::isDigit).take(10) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Mã khu vực / quốc gia") },
                singleLine = true
            )
            val location = locationInput.toIntOrNull()
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        location?.let {
                            loadAction(ClashLookupAction("Top quyên góp bang hội", "/v2/leaderboard/$it/clan/donations?limit=50"))
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !detailLoading && location != null && location > 0
                ) { Text("TOP QUYÊN GÓP", fontSize = 9.sp) }
                OutlinedButton(
                    onClick = {
                        location?.let {
                            loadAction(ClashLookupAction("Top số trận thắng chiến tranh bang hội", "/v2/leaderboard/$it/clan/war-wins?limit=50"))
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !detailLoading && location != null && location > 0
                ) { Text("TOP CHIẾN THẮNG", fontSize = 9.sp) }
            }
            OutlinedButton(
                onClick = {
                    location?.let {
                        loadAction(ClashLookupAction("Top vàng Thủ đô bang hội", "/v2/leaderboard/$it/clan/capital-gold?limit=50"))
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !detailLoading && location != null && location > 0
            ) { Text("TOP VÀNG THỦ ĐÔ") }
        }
        }

        LookupCard {
            Text(
                "Nguồn dữ liệu: ClashKing · Mã nhà sáng tạo: ClashKing",
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )
            Text(
                "Dữ liệu do ClashKing thu thập bằng cách đồng bộ nguồn trò chơi; có thể chậm hoặc chưa đầy đủ. COC-LIO không liên kết, được bảo trợ hoặc xác nhận bởi Supercell.",
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

private fun clashLookupActionCategory(action: ClashLookupAction): String {
    val title = action.title.lowercase(Locale.ROOT)
    return when {
        listOf("chiến tranh", "war", "cwl", "trận đánh", "lượt đánh").any(title::contains) -> "Chiến tranh"
        listOf("quyên góp", "nhận lính", "tài nguyên", "farming", "vàng thủ đô").any(title::contains) -> "Quyên góp & tài nguyên"
        listOf("xếp hạng", "ranked", "cúp", "top ", "legend", "kỷ lục", "chuỗi thắng").any(title::contains) -> "Xếp hạng"
        listOf("lịch sử", "thay đổi", "gia nhập", "rời bang", "mùa ").any(title::contains) -> "Lịch sử"
        else -> "Hoạt động & thống kê"
    }
}

private fun globalClashActions(): List<ClashLookupAction> = listOf(
    ClashLookupAction("Số liệu toàn cầu", "/v2/counts"),
    ClashLookupAction("Số lượng Nhà chính", "/v2/counts/players/town-halls"),
    ClashLookupAction("Số lượng giải đấu", "/v2/counts/players/league-tiers"),
    ClashLookupAction("Số lượng bang hội theo khu vực", "/v2/counts/clans/locations"),
    ClashLookupAction("Số lượng bang hội theo CWL", "/v2/counts/clans/cwl-leagues"),
    ClashLookupAction("Số lượng bang hội theo giải Thủ đô", "/v2/counts/clans/capital-leagues"),
    ClashLookupAction("Phân bố quy mô bang hội", "/v2/counts/clans/member-bins"),
    ClashLookupAction("Lịch mùa giải hiện tại", "/v2/dates/current"),
    ClashLookupAction("Lịch cuối tuần đột kích", "/v2/dates/raid-weekends"),
    ClashLookupAction("Lịch các mùa giải", "/v2/dates/seasons"),
    ClashLookupAction("Top chuỗi thắng bang hội", "/v2/leaderboard/clan/win-streak?limit=50"),
    ClashLookupAction("Tỷ lệ sao chiến tranh", "/v2/stats/wars/hitrates?interval=month"),
    ClashLookupAction("Tổng kết chiến tranh", "/v2/stats/wars/summary?interval=month"),
    ClashLookupAction("Hiệu suất chiến tranh", "/v2/stats/war"),
    ClashLookupAction("Thống kê CWL", "/v2/stats/cwl"),
    ClashLookupAction("Tỷ lệ đánh theo giải", "/v2/stats/league/hit-rates"),
    ClashLookupAction("Hiệu suất Legend", "/v2/stats/legend/performance?cohort=observed"),
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
            ClashLookupAction("Chi tiết từng lượt đánh chiến tranh", "/v2/player/$tag/war/attacks"),
            ClashLookupAction("Lịch sử CWL", "/v2/player/$tag/cwl/history?limit=15"),
            ClashLookupAction("Lịch sử gia nhập/rời bang hội", "/v2/player/$tag/join-leave?limit=40"),
            ClashLookupAction("Thống kê thời gian trong bang hội", "/v2/player/$tag/join-leave/totals"),
            ClashLookupAction("Lịch sử thay đổi hồ sơ", "/v2/player/$tag/history/changes"),
            ClashLookupAction("Lịch sử quyên góp", "/v2/player/$tag/history/stats?type=donated&limit=30"),
            ClashLookupAction("Lịch sử nhận lính", "/v2/player/$tag/history/stats?type=received&limit=30"),
            ClashLookupAction("Lịch sử Trò chơi bang hội", "/v2/player/$tag/history/stats?type=clan_games&limit=30"),
            ClashLookupAction("Lịch sử vàng Thủ đô", "/v2/player/$tag/history/stats?type=capital_gold_donated&limit=30"),
            ClashLookupAction("Lịch sử cúp Nhà chính", "/v2/player/$tag/leaderboard-history/player_home_trophies"),
            ClashLookupAction("Lịch sử cúp Nhà thợ", "/v2/player/$tag/leaderboard-history/player_builder_base_trophies"),
            ClashLookupAction("Các mốc chiến tranh", "/v2/player/$tag/timers"),
            ClashLookupAction("Thành tích Legend", "/v2/player/$tag/legend-history"),
            ClashLookupAction("Mùa Legend hiện tại", "/v2/player/$tag/legend/season"),
            ClashLookupAction("Diễn biến cúp Legend", "/v2/player/$tag/legend/series"),
            ClashLookupAction("So sánh thành tích Legend", "/v2/player/$tag/legend/comparisons"),
            ClashLookupAction("Lịch sử Ranked", "/v2/player/$tag/ranked/history"),
            ClashLookupAction("Lịch sử giải đấu Ranked và Legend", "/v2/player/$tag/league/history")
        )
    } else {
        listOf(
            ClashLookupAction("Tổng kết quyên góp", "/v2/clan/$tag/donations/summary"),
            ClashLookupAction("Lịch sử chiến tranh", "/v2/clan/$tag/wars?limit=20"),
            ClashLookupAction("Nhật ký chiến tranh công khai", "/v2/clan/$tag/warlog"),
            ClashLookupAction("Cuộc chiến hiện tại", "/v2/war/$tag/basic"),
            ClashLookupAction("Các mùa CWL", "/v2/cwl/$tag/seasons?limit=15"),
            ClashLookupAction("Nhóm CWL", "/v2/cwl/$tag/group"),
            ClashLookupAction("Thứ hạng CWL", "/v2/cwl/$tag/ranking-history"),
            ClashLookupAction("Lịch sử thành viên", "/v2/clan/$tag/join-leave?limit=50"),
            ClashLookupAction("Thay đổi hồ sơ bang hội", "/v2/clan/$tag/history/changes?limit=30"),
            ClashLookupAction("Lịch sử bảng xếp hạng", "/v2/clan/$tag/history/leaderboards"),
            ClashLookupAction("Tổng kết xếp hạng bang hội Nhà chính", "/v2/clan/$tag/history/leaderboards/summary?type=clan_home_points"),
            ClashLookupAction("Tổng kết xếp hạng bang hội Nhà thợ", "/v2/clan/$tag/history/leaderboards/summary?type=clan_builder_base_points"),
            ClashLookupAction("Tổng kết xếp hạng bang hội Thủ đô", "/v2/clan/$tag/history/leaderboards/summary?type=clan_capital_points"),
            ClashLookupAction("Lịch sử Legend của bang hội", "/v2/clan/$tag/history/legends?limit=30"),
            ClashLookupAction("Top Legend của bang hội", "/v2/clan/$tag/history/legends/summary?top=20"),
            ClashLookupAction("Kỷ lục bang hội", "/v2/clan/$tag/records"),
            ClashLookupAction("Bảng xếp hạng bang hội", "/v2/clan/$tag/rankings")
        )
    }
}

private suspend fun loadClashKingDetail(path: String): Any = withContext(Dispatchers.IO) {
    readClashKingJson(path)
}

private fun lookupFieldLabel(key: String): String = when (key) {
    "items" -> "Danh sách"
    "tag" -> "Mã"
    "name" -> "Tên"
    "townHallLevel", "townhall_level", "townhallLevel" -> "Nhà chính"
    "builderHallLevel" -> "Nhà thợ"
    "clanLevel" -> "Cấp bang hội"
    "members", "memberCount" -> "Số thành viên"
    "trophies" -> "Cúp"
    "stars", "totalStars" -> "Số sao"
    "destructionPercentage" -> "Phần trăm phá hủy"
    "attackWins" -> "Thắng tấn công"
    "defenseWins" -> "Thắng phòng thủ"
    "donated", "troopsDonated" -> "Lính đã cho"
    "received", "troopsReceived" -> "Lính đã nhận"
    "warWins" -> "Số trận thắng"
    "warWinStreak" -> "Chuỗi thắng"
    "capitalGoldTotal" -> "Vàng Thủ đô"
    "season" -> "Mùa giải"
    "rank" -> "Thứ hạng"
    "globalRank" -> "Hạng toàn cầu"
    "localRank" -> "Hạng địa phương"
    "time", "battleTime", "endTime", "startTime", "preparationStartTime" -> "Thời gian"
    "state", "status" -> "Trạng thái"
    "result" -> "Kết quả"
    "opponent" -> "Đối thủ"
    "clan" -> "Bang hội"
    "clanCapital" -> "Thủ đô bang hội"
    "league", "leagueTier" -> "Giải đấu"
    "warLeague" -> "Giải chiến tranh"
    "capitalLeague" -> "Giải Thủ đô"
    "attacks" -> "Lượt tấn công"
    "defenses" -> "Lượt phòng thủ"
    "averageStars" -> "Số sao trung bình"
    "averageDestruction" -> "Phá hủy trung bình"
    "averageDuration" -> "Thời lượng trung bình"
    "missedAttacks" -> "Lượt đánh bị bỏ lỡ"
    "teamSize" -> "Quy mô đội"
    "visits" -> "Số lần tham gia"
    "minutes" -> "Số phút"
    "pagination" -> "Phân trang"
    "hasMore" -> "Còn dữ liệu"
    "count" -> "Tổng số"
    else -> "Trường dữ liệu"
}

private fun localizeLookupLeagueName(value: String): String {
    val match = Regex(
        "^(Bronze|Silver|Gold|Crystal|Master|Champion|Titan) League(?: (I|II|III))?$",
        RegexOption.IGNORE_CASE
    ).matchEntire(value.trim())
    if (match != null) {
        val tier = when (match.groupValues[1].lowercase()) {
            "bronze" -> "Hạng Đồng"
            "silver" -> "Hạng Bạc"
            "gold" -> "Hạng Vàng"
            "crystal" -> "Hạng Pha lê"
            "master" -> "Hạng Cao thủ"
            "champion" -> "Hạng Vô địch"
            else -> "Hạng Titan"
        }
        return tier + match.groupValues[2].takeIf { it.isNotBlank() }?.let { " $it" }.orEmpty()
    }
    return when (value.trim().lowercase()) {
        "legend", "legend league" -> "Hạng Huyền thoại"
        "unranked" -> "Chưa xếp hạng"
        else -> value
    }
}

private fun lookupScalarText(value: Any?, key: String, parentKey: String = ""): String = when (value) {
    null, JSONObject.NULL -> "Chưa có dữ liệu"
    is Boolean -> if (value) "Có" else "Không"
    is Number -> if (key.contains("destructionPercentage", true)) {
        "${value.toDouble()}%"
    } else {
        value.toString()
    }
    is String -> when (key.lowercase()) {
        "state" -> when (value.lowercase()) {
            "preparation" -> "Đang chuẩn bị"
            "inwar" -> "Đang chiến tranh"
            "warended" -> "Đã kết thúc"
            "notinwar" -> "Không tham gia chiến tranh"
            else -> value
        }
        "status" -> when (value.lowercase()) {
            "active" -> "Đang hoạt động"
            "inactive" -> "Không hoạt động"
            "pending" -> "Đang chờ"
            "completed" -> "Đã hoàn tất"
            else -> value
        }
        "result" -> when (value.lowercase()) {
            "win", "won" -> "Thắng"
            "lose", "lost", "loss" -> "Thua"
            "tie", "draw" -> "Hòa"
            else -> value
        }
        "name" -> if (parentKey.lowercase() in setOf("league", "leaguetier", "warleague", "capitalleague")) {
            localizeLookupLeagueName(value)
        } else value
        else -> value
    }
    else -> value.toString()
}

@Composable
private fun LookupJsonViewer(value: Any, depth: Int = 0, parentKey: String = "") {
    when (value) {
        is JSONObject -> {
            var visibleFields by remember(value) { mutableStateOf(40) }
            val keys = value.keys().asSequence().toList()
            if (keys.isEmpty()) {
                Text("Không có dữ liệu chi tiết.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    keys.take(visibleFields).forEach { key ->
                        val child = value.opt(key)
                        when (child) {
                            is JSONObject -> {
                                Text(lookupFieldLabel(key), color = MaterialTheme.colorScheme.primary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                if (depth < 3) {
                                    LookupCard { LookupJsonViewer(child, depth + 1, key) }
                                } else {
                                    Text("Có dữ liệu chi tiết.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                                }
                            }
                            is JSONArray -> {
                                Text(lookupFieldLabel(key), color = MaterialTheme.colorScheme.primary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                if (depth < 3) {
                                    LookupJsonViewer(child, depth + 1, key)
                                } else {
                                    Text("Có " + child.length() + " mục dữ liệu.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                                }
                            }
                            else -> LookupValue(lookupFieldLabel(key), lookupScalarText(child, key, parentKey))
                        }
                    }
                    if (keys.size > visibleFields) {
                        OutlinedButton(
                            onClick = { visibleFields = (visibleFields + 40).coerceAtMost(keys.size) },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("XEM THÊM TRƯỜNG") }
                    }
                }
            }
        }
        is JSONArray -> {
            var visibleItems by remember(value) { mutableStateOf(15) }
            val count = minOf(value.length(), visibleItems)
            if (count == 0) {
                Text("Chưa có dữ liệu trong mục này.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (index in 0 until count) {
                        val item = value.opt(index)
                        when (item) {
                            is JSONObject -> LookupCard {
                                Text("MỤC " + (index + 1), color = MaterialTheme.colorScheme.primary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                if (depth < 3) {
                                    LookupJsonViewer(item, depth + 1, parentKey)
                                } else {
                                    Text("Có dữ liệu chi tiết.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                                }
                            }
                            is JSONArray -> {
                                Text("Nhóm " + (index + 1), color = MaterialTheme.colorScheme.primary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                if (depth < 3) LookupJsonViewer(item, depth + 1, parentKey)
                            }
                            else -> LookupValue("Mục " + (index + 1), lookupScalarText(item, "", parentKey))
                        }
                    }
                    if (value.length() > count) {
                        Text(
                            "Đang hiển thị " + count + " / " + value.length() + " mục.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 9.sp
                        )
                        OutlinedButton(
                            onClick = { visibleItems = (visibleItems + 15).coerceAtMost(value.length()) },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("XEM THÊM DỮ LIỆU") }
                    }
                }
            }
        }
        else -> Text(lookupScalarText(value, ""), color = MaterialTheme.colorScheme.onSurface, fontSize = 10.sp)
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
        LookupValue("Giải đấu", league?.optString("name")?.takeIf { it.isNotBlank() }?.let(::localizeLookupLeagueName) ?: "Chưa có dữ liệu")
        LookupValue("Bang hội", clan?.optString("name")?.takeIf { it.isNotBlank() } ?: "Không có / chưa có dữ liệu")
        clan?.optString("tag")?.takeIf { it.isNotBlank() }?.let { LookupValue("Mã bang hội", it) }
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
private fun ClanRankingCategory(label: String, category: JSONObject?) {
    if (category == null) return
    Text(label, color = MaterialTheme.colorScheme.primary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    LookupValue("Điểm", category.optInt("points").toString())
    val placements = category.optJSONArray("placements") ?: JSONArray()
    (0 until minOf(placements.length(), 6)).forEach { index ->
        val placement = placements.optJSONObject(index) ?: JSONObject()
        val locationId = placement.optString("locationId").ifBlank { "—" }
        LookupValue("Hạng khu vực $locationId", placement.optInt("rank").toString())
    }
}

@Composable
private fun ClanLookupCard(data: ClashLookupResponse) {
    val clan = data.profile
    val rankings = data.rankings
    val badge = clan.optJSONObject("badgeUrls")?.optString("large").orEmpty()
    val location = clan.optJSONObject("location")?.optString("name").orEmpty()
    val warLeague = clan.optJSONObject("warLeague")?.optString("name").orEmpty().let(::localizeLookupLeagueName)
    val capitalLeague = clan.optJSONObject("capitalLeague")?.optString("name").orEmpty().let(::localizeLookupLeagueName)
    val members = clan.optJSONArray("members") ?: JSONArray()

    LookupCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (badge.isNotBlank()) {
                AsyncImage(
                    model = badge,
                    contentDescription = AppLanguageSettings.translate(LocalContext.current, "Huy hiệu bang hội"),
                    modifier = Modifier.size(64.dp)
                )
                Spacer(Modifier.width(12.dp))
            }
            Column {
                Text(
                    clan.optString("name", "Bang hội"),
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
        LookupValue("Cấp bang hội", clan.optInt("clanLevel").toString())
        LookupValue("Thành viên", clan.optInt("memberCount").toString())
        LookupValue("Điểm bang hội", clan.optInt("clanPoints").toString())
        LookupValue("Vị trí", location.ifBlank { "Chưa có dữ liệu" })
        LookupValue("Giải chiến tranh", warLeague.ifBlank { "Chưa có dữ liệu" })
        LookupValue("Giải Thủ đô", capitalLeague.ifBlank { "Chưa có dữ liệu" })
        LookupValue("Số trận thắng", clan.optInt("warWins").toString())
        LookupValue("Chuỗi thắng", clan.optInt("warWinStreak").toString())
        LookupValue("Nhận lính", clan.optInt("troopsDonated").toString())
        LookupValue("Được nhận lính", clan.optInt("troopsReceived").toString())
        LookupValue("Vàng thủ đô", clan.optLong("capitalGoldTotal").toString())
        LookupValue("Nhật ký chiến tranh", if (clan.optBoolean("publicWarLog")) "Công khai" else "Riêng tư")
        if (rankings != null) {
            Spacer(Modifier.height(4.dp))
            Text("XẾP HẠNG BANG HỘI", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            ClanRankingCategory("Nhà chính", rankings.optJSONObject("homeVillage"))
            ClanRankingCategory("Nhà thợ", rankings.optJSONObject("builderBase"))
            ClanRankingCategory("Thủ đô bang hội", rankings.optJSONObject("clanCapital"))
            ClanRankingCategory("Quyên góp", rankings.optJSONObject("donations"))
            ClanRankingCategory("Nhận quyên góp", rankings.optJSONObject("donationsReceived"))
            ClanRankingCategory("Chiến thắng chiến tranh", rankings.optJSONObject("warWins"))
            ClanRankingCategory("Chuỗi thắng", rankings.optJSONObject("warWinStreak"))
            ClanRankingCategory("Vàng Thủ đô", rankings.optJSONObject("capitalGold"))
        }

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
                        "Nhà chính " + member.optInt("townHallLevel"),
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
private fun LookupResourceValue(resource: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CocResourceIcon(resource, Modifier.size(16.dp))
        Text(gameDataResourceLabel(resource), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, modifier = Modifier.weight(1f))
        Text(value, color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun LookupIconValue(iconRes: Int, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CocGameIcon(iconRes, Modifier.size(16.dp), label)
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, modifier = Modifier.weight(1f))
        Text(value, color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
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
