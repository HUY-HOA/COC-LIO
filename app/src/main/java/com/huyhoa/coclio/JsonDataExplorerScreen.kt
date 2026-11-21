package com.huyhoa.coclio

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.huyhoa.coclio.data.CocAssets
import com.huyhoa.coclio.data.VillageExtraItem
import com.huyhoa.coclio.data.VillageItem
import com.huyhoa.coclio.data.VillageSnapshot
import kotlinx.coroutines.delay
import org.json.JSONObject

/**
 * Single-screen village dashboard. Important information is visible first;
 * the detailed catalogue is grouped into one-level accordions below it.
 */
@Composable
internal fun JsonDataExplorerScreen(snapshot: VillageSnapshot) {
    val context = LocalContext.current
    var selectedBase by rememberSaveable(snapshot.tag, snapshot.timestamp) {
        mutableStateOf("all")
    }
    var selectedStatus by rememberSaveable(snapshot.tag, snapshot.timestamp) {
        mutableStateOf("all")
    }
    var query by rememberSaveable(snapshot.tag, snapshot.timestamp) {
        mutableStateOf("")
    }
    var expandedSection by rememberSaveable(snapshot.tag, snapshot.timestamp) {
        mutableStateOf("")
    }
    var showRawJson by rememberSaveable(snapshot.tag, snapshot.timestamp) {
        mutableStateOf(false)
    }
    var showUnknownFields by rememberSaveable(snapshot.tag, snapshot.timestamp) {
        mutableStateOf(false)
    }
    var nowSeconds by remember(snapshot.tag, snapshot.timestamp) {
        mutableLongStateOf(System.currentTimeMillis() / 1000L)
    }

    LaunchedEffect(snapshot.tag, snapshot.timestamp) {
        while (true) {
            delay(5_000L)
            nowSeconds = System.currentTimeMillis() / 1000L
        }
    }

    val activeUpgrades = snapshot.activeUpgradesAt(nowSeconds)
        .sortedBy { it.remainingSecondsAt(snapshot.timestamp, nowSeconds) }
    val expiredUpgrades = snapshot.expiredUpgradesAt(nowSeconds)
    val filteredItems = filterJsonExplorerItems(
        items = snapshot.items,
        selectedBase = selectedBase,
        selectedStatus = selectedStatus,
        query = query,
        sortOrder = "name",
        snapshotTimestamp = snapshot.timestamp,
        nowSeconds = nowSeconds
    )
    val groupedItems = filteredItems.groupBy { it.sectionKey }
    val showExtras = selectedStatus == "all" || selectedStatus == "extras"
    val filteredExtras = if (showExtras) {
        snapshot.extraItems.filter { extra ->
            (selectedBase == "all" || extra.base == selectedBase) &&
                extra.matchesExplorerQuery(query)
        }
    } else emptyList()
    val groupedExtras = filteredExtras.groupBy { it.sectionKey }
    val normalizedQuery = normalizeJsonExplorerText(query)
    val filteredUnknownFields = if (showExtras) {
        snapshot.extraFields.filter { (key, value) ->
            normalizedQuery.isBlank() ||
                normalizeJsonExplorerText("$key $value").contains(normalizedQuery)
        }
    } else emptyMap()
    val sectionOrder = listOf(
        "buildings", "traps", "units", "siege_machines", "heroes", "spells",
        "pets", "guardians", "equipment", "squads", "helpers",
        "buildings2", "traps2", "units2", "heroes2"
    )
    val orderedItemGroups = groupedItems.entries.sortedBy { sectionOrder.indexOf(it.key).let { index ->
        if (index < 0) Int.MAX_VALUE else index
    } }
    val activeCount = activeUpgrades.size
    val totalItems = snapshot.items.size + snapshot.extraItems.size
    val prettyJson = remember(snapshot.rawJson) {
        runCatching { JSONObject(snapshot.rawJson).toString(2) }.getOrDefault(snapshot.rawJson)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        item(key = "dashboard_identity") {
            VillageIdentityCard(snapshot)
        }

        item(key = "dashboard_bases") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (snapshot.homeItems.isNotEmpty()) {
                    VillageBaseCard(
                        title = "NHÀ CHÍNH",
                        levelLabel = "TH",
                        level = snapshot.townHallLevel,
                        itemCount = snapshot.homeItems.size,
                        upgradeCount = activeUpgrades.count { it.base == "home" },
                        hall = snapshot.items.firstOrNull {
                            it.sectionKey == "buildings" && it.dataId == 1000001
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (snapshot.builderItems.isNotEmpty()) {
                    VillageBaseCard(
                        title = "LÀNG ĐÊM",
                        levelLabel = "BH",
                        level = snapshot.builderHallLevel,
                        itemCount = snapshot.builderItems.size,
                        upgradeCount = activeUpgrades.count { it.base == "builder" },
                        hall = snapshot.items.firstOrNull {
                            it.sectionKey == "buildings2" && it.dataId == 1000034
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        item(key = "dashboard_at_a_glance") {
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                DashboardMetric("ĐANG NÂNG", activeCount.toString(), Modifier.weight(1f), MaterialTheme.colorScheme.primary)
                DashboardMetric("CHỜ XÁC NHẬN", expiredUpgrades.size.toString(), Modifier.weight(1f), MaterialTheme.colorScheme.tertiary)
                DashboardMetric("TỔNG MỤC", totalItems.toString(), Modifier.weight(1f), MaterialTheme.colorScheme.onSurface)
            }
        }

        if (query.isBlank() && selectedStatus == "all") {
            item(key = "dashboard_upgrade_header") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            "TIẾN TRÌNH NÂNG CẤP",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.35.sp
                        )
                        Text(
                            if (activeCount == 0) "Không có mục nào đang chạy" else "$activeCount mục đang thực hiện",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 9.sp
                        )
                    }
                    if (activeCount > 4) {
                        TextButton(onClick = { selectedStatus = "running" }) {
                            Text("XEM TẤT CẢ", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
    
            if (activeUpgrades.isEmpty()) {
                item(key = "dashboard_no_upgrade") {
                    DashboardNotice(
                        title = "Làng đang không có tiến trình nâng cấp",
                        detail = "Khi JSON có timer đang chạy, tiến độ và thời gian còn lại sẽ hiển thị tại đây."
                    )
                }
            } else {
                itemsIndexed(
                    items = activeUpgrades.take(4),
                    key = { index, item -> "focus_upgrade_${item.identity}_${item.base}_$index" }
                ) { _, item ->
                    UpgradeFocusRow(item, snapshot.timestamp, nowSeconds)
                }
                if (expiredUpgrades.isNotEmpty()) {
                    item(key = "dashboard_expired_notice") {
                        DashboardNotice(
                            title = "${expiredUpgrades.size} mục đã hết thời gian theo JSON",
                            detail = "Cần nhập JSON mới để xác nhận cấp độ hoàn thành trong game."
                        )
                    }
                }
            }
    
    
        }

        item(key = "dashboard_catalog_title") {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    "DỮ LIỆU LÀNG",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.35.sp
                )
                Text(
                    "Các nhóm được xếp theo thứ tự trong game. Chạm vào nhóm để mở chi tiết.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 9.sp
                )
            }
        }

        item(key = "dashboard_search") {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(13.dp),
                label = { Text("Tìm trong toàn bộ dữ liệu") },
                placeholder = { Text("Tên công trình, quân đội, mã dữ liệu, thời gian...") }
            )
        }

        item(key = "dashboard_base_filter") {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    "all" to "CẢ HAI LÀNG",
                    "home" to "NHÀ CHÍNH",
                    "builder" to "LÀNG ĐÊM"
                ).forEach { (key, label) ->
                    FilterChip(
                        selected = selectedBase == key,
                        onClick = { selectedBase = key },
                        label = { Text(label, fontSize = 9.sp, maxLines = 1) }
                    )
                }
            }
        }

        item(key = "dashboard_status_filter") {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    "all" to "TẤT CẢ",
                    "running" to "ĐANG CHẠY",
                    "gear" to "GEAR UP",
                    "expired" to "HẾT GIỜ",
                    "idle" to "CHƯA NÂNG",
                    "extras" to "DỮ LIỆU PHỤ"
                ).forEach { (key, label) ->
                    FilterChip(
                        selected = selectedStatus == key,
                        onClick = {
                            selectedStatus = key
                            if (key != "all" && key != "extras") expandedSection = ""
                        },
                        label = { Text(label, fontSize = 9.sp, maxLines = 1) }
                    )
                }
            }
        }

        if (orderedItemGroups.isEmpty() && groupedExtras.isEmpty() && filteredUnknownFields.isEmpty()) {
            item(key = "dashboard_no_results") {
                DashboardNotice(
                    title = "Không tìm thấy dữ liệu phù hợp",
                    detail = "Thử từ khóa khác hoặc chọn lại bộ lọc."
                )
            }
        }

        orderedItemGroups.forEach { (sectionKey, rows) ->
            val first = rows.first()
            item(key = "catalog_section_$sectionKey") {
                JsonCategoryAccordion(
                    title = first.sectionLabel,
                    subtitle = rows.take(2).joinToString(" • ") { it.name },
                    count = rows.size,
                    expanded = expandedSection == sectionKey,
                    onClick = {
                        expandedSection = if (expandedSection == sectionKey) "" else sectionKey
                    }
                )
            }
            if (expandedSection == sectionKey) {
                itemsIndexed(
                    items = rows,
                    key = { index, item -> "catalog_item_${item.identity}_$index" }
                ) { _, item ->
                    JsonCatalogItemRow(item, snapshot, nowSeconds)
                }
            }
        }

        val orderedExtraGroups = groupedExtras.entries.sortedBy { sectionOrder.indexOf(it.key).let { index ->
            if (index < 0) Int.MAX_VALUE else index + 20
        } }
        orderedExtraGroups.forEach { (sectionKey, rows) ->
            val first = rows.first()
            val key = "extra_$sectionKey"
            item(key = "catalog_extra_section_$sectionKey") {
                JsonCategoryAccordion(
                    title = first.sectionLabel,
                    subtitle = "Đồ trang trí và vật phẩm phụ",
                    count = rows.sumOf { it.count },
                    expanded = expandedSection == key,
                    onClick = { expandedSection = if (expandedSection == key) "" else key }
                )
            }
            if (expandedSection == key) {
                itemsIndexed(
                    items = rows,
                    key = { index, extra -> "catalog_extra_${extra.sectionKey}_${extra.dataId}_$index" }
                ) { _, extra ->
                    JsonExtraRow(extra)
                }
            }
        }

        if (filteredUnknownFields.isNotEmpty()) {
            item(key = "catalog_unknown_fields") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .clickable { showUnknownFields = !showUnknownFields }
                                .padding(horizontal = 11.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Outlined.Code, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(17.dp))
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text("TRƯỜNG CHƯA PHÂN LOẠI", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                Text("Dữ liệu được giữ nguyên từ JSON", fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text("${filteredUnknownFields.size}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Icon(if (showUnknownFields) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                        if (showUnknownFields) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                filteredUnknownFields.forEach { (key, value) ->
                                    JsonUnknownFieldRow(key, value)
                                }
                            }
                        }
                    }
                }
            }
        }

        item(key = "dashboard_raw_json") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .clickable { showRawJson = !showRawJson }
                            .padding(horizontal = 11.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.Code, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("JSON GỐC", modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        TextButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                clipboard?.setPrimaryClip(ClipData.newPlainText("COC-LIO JSON", snapshot.rawJson))
                                Toast.makeText(context, "Đã sao chép JSON gốc.", Toast.LENGTH_SHORT).show()
                            },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                        ) {
                            Text("SAO CHÉP", fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }
                        Icon(if (showRawJson) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                    if (showRawJson) {
                        Text(
                            prettyJson,
                            modifier = Modifier.fillMaxWidth().padding(10.dp),
                            color = MaterialTheme.colorScheme.onSurface,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 8.sp,
                            lineHeight = 12.sp
                        )
                    }
                }
            }
        }

        item(key = "dashboard_footer") {
            Text(
                "Mọi thông tin được lấy từ bản JSON đã nhập. Timer hết giờ chỉ là ước tính; dữ liệu game mới là căn cứ xác nhận hoàn thành.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 8.sp,
                lineHeight = 12.sp
            )
        }
    }
}

@Composable
private fun VillageIdentityCard(snapshot: VillageSnapshot) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.58f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.28f))
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 13.dp, vertical = 11.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text("TỔNG HỢP LÀNG", color = MaterialTheme.colorScheme.primary, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 0.9.sp)
            Text(
                snapshot.villageName?.takeIf { it.isNotBlank() } ?: "Làng chưa đặt tên",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                Text(snapshot.tag, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
                Text("Cập nhật: ${formatTimestamp(snapshot.timestamp)}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
            }
        }
    }
}

@Composable
private fun VillageBaseCard(
    title: String,
    levelLabel: String,
    level: Int?,
    itemCount: Int,
    upgradeCount: Int,
    hall: VillageItem?,
    modifier: Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.46f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 9.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.sp, fontWeight = FontWeight.Black, maxLines = 1)
                Text("$levelLabel ${level ?: "—"}", color = MaterialTheme.colorScheme.primary, fontSize = 22.sp, fontWeight = FontWeight.Black, maxLines = 1)
                Text("$itemCount mục dữ liệu", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.sp)
                Text("$upgradeCount đang nâng", color = MaterialTheme.colorScheme.onSurface, fontSize = 8.sp, fontWeight = FontWeight.SemiBold)
            }
            if (hall != null) {
                val imageUrl = CocAssets.imageUrl(hall)
                if (imageUrl != null) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = title,
                        modifier = Modifier.size(52.dp),
                        contentScale = ContentScale.Fit
                    )
                }
            }
        }
    }
}

@Composable
private fun DashboardMetric(label: String, value: String, modifier: Modifier, accent: androidx.compose.ui.graphics.Color) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(11.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f))
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 7.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(value, color = accent, fontSize = 19.sp, fontWeight = FontWeight.Black, maxLines = 1)
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 7.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun UpgradeFocusRow(item: VillageItem, snapshotTimestamp: Long, nowSeconds: Long) {
    val remaining = item.remainingSecondsAt(snapshotTimestamp, nowSeconds)
    val progress = item.progressRatioAt(snapshotTimestamp, nowSeconds)
    val percent = item.progressPercentAt(snapshotTimestamp, nowSeconds)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 9.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val imageUrl = CocAssets.imageUrl(item)
            if (imageUrl != null) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = item.name,
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Fit
                )
            } else {
                Icon(Icons.Outlined.Category, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(38.dp))
            }
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(item.name, color = MaterialTheme.colorScheme.onSurface, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "${if (item.base == "builder") "Làng đêm" else "Nhà chính"} • ${if (item.gearUp) "Gear Up" else "Cấp ${item.level ?: "?"} → ${(item.level ?: 0) + 1}"}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 8.sp,
                    maxLines = 1
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("$percent%", color = MaterialTheme.colorScheme.primary, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(6.dp))
                    LinearProgressIndicator(
                        progress = progress,
                        modifier = Modifier.weight(1f).height(5.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surface
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(formatDuration(remaining), color = MaterialTheme.colorScheme.onSurface, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun JsonCategoryAccordion(
    title: String,
    subtitle: String,
    count: Int,
    expanded: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(11.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (expanded) 0.65f else 0.38f)),
        border = BorderStroke(1.dp, if (expanded) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f) else MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.Category, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(19.dp))
            Spacer(Modifier.width(9.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, color = MaterialTheme.colorScheme.onSurface, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(count.toString(), color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.width(5.dp))
            Icon(if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun JsonCatalogItemRow(item: VillageItem, snapshot: VillageSnapshot, nowSeconds: Long) {
    var expanded by remember(snapshot.tag, snapshot.timestamp, item.identity) { mutableStateOf(false) }
    val active = item.isTimerActive(snapshot.timestamp, nowSeconds)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f))
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(horizontal = 9.dp, vertical = 7.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val imageUrl = CocAssets.imageUrl(item)
                if (imageUrl != null) {
                    AsyncImage(model = imageUrl, contentDescription = item.name, modifier = Modifier.size(34.dp).clip(RoundedCornerShape(7.dp)), contentScale = ContentScale.Fit)
                } else {
                    Icon(Icons.Outlined.Category, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(30.dp))
                }
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(item.name, color = MaterialTheme.colorScheme.onSurface, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        "Cấp ${item.level ?: "—"} • ${item.base.jsonBaseLabel()} • x${item.count}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 8.sp
                    )
                }
                if (active) {
                    Text("ĐANG NÂNG", color = MaterialTheme.colorScheme.primary, fontSize = 7.sp, fontWeight = FontWeight.Black)
                } else if (item.timerSeconds != null) {
                    Text("HẾT GIỜ", color = MaterialTheme.colorScheme.tertiary, fontSize = 7.sp, fontWeight = FontWeight.Bold)
                }
                Icon(if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
            }
            if (expanded) {
                JsonDetailField("section", item.sectionKey)
                JsonDetailField("data", item.dataId.toString())
                JsonDetailField("lvl", item.level?.toString() ?: "Không có")
                JsonDetailField("cnt", if (item.hasExplicitCount) item.count.toString() else "Không có trong JSON")
                JsonDetailField(
                    "timer",
                    item.timerSeconds?.let { raw ->
                        "${formatDuration(raw)} • còn ${formatDuration(item.remainingSecondsAt(snapshot.timestamp, nowSeconds))}"
                    } ?: "Không có"
                )
                JsonDetailField("gear_up", if (item.gearUp) "1" else "0")
                item.helperCooldownSeconds?.let { JsonDetailField("helper_cooldown", formatDuration(it)) }
                item.weaponLevel?.let { JsonDetailField("weapon", it.toString()) }
                item.details.forEachIndexed { index, detail -> JsonDetailField("types[$index] / modules", detail) }
                item.properties.forEach { (key, value) -> JsonDetailField(key, value) }
                if (item.timerSeconds != null && !active) {
                    Text(
                        "Thời gian ước tính đã hết. Nhập JSON mới để xác nhận cấp độ.",
                        color = MaterialTheme.colorScheme.tertiary,
                        fontSize = 8.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun JsonExtraRow(item: VillageExtraItem) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Outlined.Category, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(17.dp))
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(item.name, color = MaterialTheme.colorScheme.onSurface, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
            Text("Mã ${item.dataId ?: "—"} • ${item.base.jsonBaseLabel()}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.sp)
        }
        Text("x${item.count}", color = MaterialTheme.colorScheme.primary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun JsonUnknownFieldRow(key: String, value: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 7.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(key, color = MaterialTheme.colorScheme.primary, fontSize = 8.sp, fontWeight = FontWeight.Bold)
        Text(value, color = MaterialTheme.colorScheme.onSurface, fontSize = 8.sp, lineHeight = 12.sp, fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun JsonDetailField(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(label, modifier = Modifier.width(100.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.sp)
        Text(value, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface, fontSize = 8.sp, lineHeight = 12.sp)
    }
}

@Composable
private fun DashboardNotice(title: String, detail: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(11.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f))
    ) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, color = MaterialTheme.colorScheme.onSurface, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Text(detail, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.sp, lineHeight = 12.sp)
        }
    }
}

private fun String.jsonBaseLabel(): String = when (this) {
    "home" -> "Nhà chính"
    "builder" -> "Làng đêm"
    else -> this
}
