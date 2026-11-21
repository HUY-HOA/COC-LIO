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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.huyhoa.coclio.data.VillageExtraItem
import com.huyhoa.coclio.data.VillageItem
import com.huyhoa.coclio.data.VillageSnapshot
import kotlinx.coroutines.delay
import org.json.JSONObject

/**
 * Full, local-only explorer for every value retained from the game JSON export.
 * It deliberately does not infer profile fields that are absent from the export.
 */
@Composable
internal fun JsonDataExplorerScreen(snapshot: VillageSnapshot) {
    val context = LocalContext.current
    var selectedBase by rememberSaveable(snapshot.tag, snapshot.timestamp) {
        mutableStateOf("all")
    }
    var query by rememberSaveable(snapshot.tag, snapshot.timestamp) {
        mutableStateOf("")
    }
    var showRawJson by rememberSaveable(snapshot.tag, snapshot.timestamp) {
        mutableStateOf(false)
    }
    var nowSeconds by remember(snapshot.tag, snapshot.timestamp) {
        mutableLongStateOf(System.currentTimeMillis() / 1000L)
    }

    LaunchedEffect(snapshot.tag, snapshot.timestamp) {
        while (true) {
            delay(30_000L)
            nowSeconds = System.currentTimeMillis() / 1000L
        }
    }

    val normalizedQuery = query.trim()
    val filteredItems = snapshot.items.filter { item ->
        (selectedBase == "all" || item.base == selectedBase) &&
            item.matchesExplorerQuery(normalizedQuery)
    }
    val groupedItems = filteredItems.groupBy { it.sectionKey }
    val filteredExtras = snapshot.extraItems.filter { extra ->
        (selectedBase == "all" || extra.base == selectedBase) &&
            extra.matchesExplorerQuery(normalizedQuery)
    }
    val groupedExtras = filteredExtras.groupBy { it.sectionKey }
    val filteredUnknownFields = snapshot.extraFields.filter { (key, value) ->
        normalizedQuery.isBlank() ||
            key.contains(normalizedQuery, ignoreCase = true) ||
            value.contains(normalizedQuery, ignoreCase = true)
    }
    val unknownItemFieldCount = snapshot.items.sumOf { it.properties.size } + snapshot.extraFields.size
    val activeUpgradeCount = snapshot.activeUpgradesAt(nowSeconds).size
    val prettyJson = remember(snapshot.rawJson) {
        runCatching { JSONObject(snapshot.rawJson).toString(2) }.getOrDefault(snapshot.rawJson)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        item(key = "json_explorer_header") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.42f))
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        Text(
                            "TOÀN BỘ DỮ LIỆU JSON",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.7.sp
                        )
                        Text(
                            snapshot.villageName?.takeIf { it.isNotBlank() } ?: "Làng chưa đặt tên",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            "${snapshot.tag}  •  ${formatTimestamp(snapshot.timestamp)}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            JsonExplorerMetric("MỤC", snapshot.items.size.toString(), Modifier.weight(1f))
                            JsonExplorerMetric("DỮ LIỆU PHỤ", snapshot.extraItems.size.toString(), Modifier.weight(1f))
                            JsonExplorerMetric("ĐANG NÂNG", activeUpgradeCount.toString(), Modifier.weight(1f))
                            JsonExplorerMetric("TRƯỜNG THÊM", unknownItemFieldCount.toString(), Modifier.weight(1f))
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "all" to "TẤT CẢ",
                        "home" to "LÀNG CHÍNH",
                        "builder" to "LÀNG ĐÊM"
                    ).forEach { (key, label) ->
                        FilterChip(
                            selected = selectedBase == key,
                            onClick = { selectedBase = key },
                            label = { Text(label, fontSize = 9.sp, maxLines = 1) }
                        )
                    }
                }

                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Tìm tên, mã dữ liệu, trường JSON") },
                    placeholder = { Text("Ví dụ: Nhà chính, 1000001, timer, gear_up") }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { showRawJson = !showRawJson },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            if (showRawJson) "ẨN JSON GỐC" else "XEM JSON GỐC",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    TextButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                            clipboard?.setPrimaryClip(ClipData.newPlainText("COC-LIO JSON", snapshot.rawJson))
                            Toast.makeText(context, "Đã sao chép JSON gốc.", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("SAO CHÉP JSON", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (groupedItems.isEmpty() && groupedExtras.isEmpty() && filteredUnknownFields.isEmpty()) {
            item(key = "json_explorer_no_results") {
                JsonExplorerInfoCard(
                    "Không tìm thấy dữ liệu",
                    "Thử từ khóa khác hoặc đổi bộ lọc khu vực làng."
                )
            }
        }

        groupedItems.forEach { (sectionKey, rows) ->
            val sectionName = rows.firstOrNull()?.sectionLabel ?: sectionKey
            item(key = "json_explorer_section_$sectionKey") {
                JsonExplorerSectionHeader(sectionName.uppercase(), rows.size)
            }
            items(
                items = rows,
                key = { item -> "json_item_${item.identity}" }
            ) { item ->
                JsonExplorerItemCard(
                    item = item,
                    snapshot = snapshot,
                    nowSeconds = nowSeconds
                )
            }
        }

        groupedExtras.forEach { (sectionKey, rows) ->
            val sectionName = rows.firstOrNull()?.sectionLabel ?: sectionKey
            item(key = "json_explorer_extra_section_$sectionKey") {
                JsonExplorerSectionHeader("${sectionName.uppercase()} • DỮ LIỆU PHỤ", rows.size)
            }
            items(
                items = rows,
                key = { extra -> "json_extra_${extra.sectionKey}_${extra.dataId}_${extra.level}_${extra.value}" }
            ) { extra ->
                JsonExplorerExtraCard(extra)
            }
        }

        if (filteredUnknownFields.isNotEmpty()) {
            item(key = "json_explorer_unknown_header") {
                JsonExplorerSectionHeader("TRƯỜNG JSON CHƯA PHÂN LOẠI", filteredUnknownFields.size)
            }
            filteredUnknownFields.forEach { (key, value) ->
                item(key = "json_unknown_$key") {
                    JsonExplorerUnknownField(key, value)
                }
            }
        }

        if (showRawJson) {
            item(key = "json_explorer_raw_header") {
                JsonExplorerSectionHeader("JSON GỐC • DỮ LIỆU NGUYÊN BẢN", prettyJson.length)
            }
            item(key = "json_explorer_raw_body") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Text(
                        prettyJson,
                        modifier = Modifier.fillMaxWidth().padding(10.dp),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        lineHeight = 13.sp
                    )
                }
            }
        }

        item(key = "json_explorer_footer") {
            Text(
                "Dữ liệu hiển thị được lấy từ bản JSON đã nhập. Trường không nhận diện vẫn được giữ nguyên; các thông tin không có trong JSON sẽ không được tự suy đoán.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 9.sp,
                lineHeight = 13.sp
            )
        }
    }
}

@Composable
private fun JsonExplorerMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 7.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                value,
                color = MaterialTheme.colorScheme.primary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1
            )
            Text(
                label,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 7.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun JsonExplorerSectionHeader(title: String, count: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 7.dp, bottom = 1.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.primary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.4.sp
        )
        Text(
            count.toString(),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun JsonExplorerItemCard(
    item: VillageItem,
    snapshot: VillageSnapshot,
    nowSeconds: Long
) {
    var expanded by remember(snapshot.tag, snapshot.timestamp, item.identity) {
        mutableStateOf(false)
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(9.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        item.name,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "ID ${item.dataId}  •  ${item.level?.let { "Cấp $it" } ?: "Chưa có cấp"}  •  ${item.base.jsonBaseLabel()}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 9.sp
                    )
                }
                Text(
                    if (expanded) "ẨN −" else "CHI TIẾT +",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                JsonExplorerTag("cnt: ${item.countLabel()}")
                if (item.isUpgradeRunning) JsonExplorerTag(if (item.gearUp) "ĐANG GEAR UP" else "ĐANG NÂNG")
                if (item.properties.isNotEmpty()) JsonExplorerTag("${item.properties.size} trường thêm")
            }
            if (expanded) {
                Spacer(Modifier.height(3.dp))
                JsonExplorerField("section", item.sectionKey)
                JsonExplorerField("data", item.dataId.toString())
                JsonExplorerField("lvl", item.level?.toString() ?: "Không có")
                JsonExplorerField("cnt", if (item.hasExplicitCount) item.count.toString() else "Không có trong JSON")
                JsonExplorerField(
                    "timer",
                    item.timerSeconds?.let { raw ->
                        val remaining = item.remainingSecondsAt(snapshot.timestamp, nowSeconds)
                        "${raw}s • còn khoảng ${formatDuration(remaining)}"
                    } ?: "Không có"
                )
                JsonExplorerField("gear_up", if (item.gearUp) "1 (có)" else "0 (không)")
                item.helperCooldownSeconds?.let {
                    JsonExplorerField("helper_cooldown", "${it}s")
                }
                item.weaponLevel?.let { JsonExplorerField("weapon", it.toString()) }
                item.details.forEachIndexed { index, detail ->
                    JsonExplorerField("types[$index] / modules", detail)
                }
                item.properties.forEach { (key, value) ->
                    JsonExplorerField(key, value)
                }
                if (item.timerSeconds != null && item.remainingSecondsAt(snapshot.timestamp, nowSeconds) == 0L) {
                    Text(
                        "Bộ đếm theo JSON đã hết. Cần nhập bản JSON mới để xác nhận cấp độ hoàn thành.",
                        color = MaterialTheme.colorScheme.tertiary,
                        fontSize = 9.sp,
                        lineHeight = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun JsonExplorerExtraCard(item: VillageExtraItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.44f)
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(9.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(item.name, color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            item.dataId?.let { JsonExplorerField("data", it.toString()) }
            item.level?.let { JsonExplorerField("lvl", it.toString()) }
            JsonExplorerField("cnt", item.count.toString())
            JsonExplorerField("khu vực", item.base.jsonBaseLabel())
            item.value?.let { JsonExplorerField("value", it) }
        }
    }
}

@Composable
private fun JsonExplorerUnknownField(key: String, value: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(9.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(key, color = MaterialTheme.colorScheme.primary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text(
                value,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 9.sp,
                lineHeight = 13.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
private fun JsonExplorerField(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            label,
            modifier = Modifier.width(106.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 9.sp
        )
        Text(
            value,
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 9.sp,
            lineHeight = 13.sp
        )
    }
}

@Composable
private fun JsonExplorerTag(label: String) {
    Card(
        shape = RoundedCornerShape(7.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        )
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            color = MaterialTheme.colorScheme.primary,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun JsonExplorerInfoCard(title: String, detail: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        )
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(title, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(detail, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
        }
    }
}

private fun VillageItem.matchesExplorerQuery(query: String): Boolean {
    if (query.isBlank()) return true
    val searchable = buildString {
        append(name).append(' ')
        append(sectionLabel).append(' ')
        append(sectionKey).append(' ')
        append(dataId).append(' ')
        append(level ?: "").append(' ')
        append(count).append(' ')
        append(timerSeconds ?: "").append(' ')
        append(helperCooldownSeconds ?: "").append(' ')
        append(properties.entries.joinToString(" ") { "${it.key} ${it.value}" }).append(' ')
        append(details.joinToString(" "))
    }
    return searchable.contains(query, ignoreCase = true)
}

private fun VillageExtraItem.matchesExplorerQuery(query: String): Boolean {
    if (query.isBlank()) return true
    val searchable = listOf(
        name,
        sectionLabel,
        sectionKey,
        dataId?.toString().orEmpty(),
        level?.toString().orEmpty(),
        count.toString(),
        value.orEmpty()
    ).joinToString(" ")
    return searchable.contains(query, ignoreCase = true)
}

private fun VillageItem.countLabel(): String =
    if (hasExplicitCount) count.toString() else "không có"

private fun String.jsonBaseLabel(): String = when (this) {
    "home" -> "Làng chính"
    "builder" -> "Làng đêm"
    else -> this
}
