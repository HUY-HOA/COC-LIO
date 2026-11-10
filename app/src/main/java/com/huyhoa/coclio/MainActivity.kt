package com.huyhoa.coclio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import coil.compose.AsyncImage
import com.huyhoa.coclio.data.CocAssets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.huyhoa.coclio.data.ChangeEvent
import com.huyhoa.coclio.data.VillageItem
import com.huyhoa.coclio.data.VillageParser
import com.huyhoa.coclio.data.VillageSnapshot
import com.huyhoa.coclio.data.VillageStore
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val CocBackground = Color(0xFF050817)
private val CocSurface = Color(0xFF0D152B)
private val CocSurfaceAlt = Color(0xFF14203B)
private val CocGold = Color(0xFFFFD166)
private val CocGreen = Color(0xFF42F5C8)
private val CocRed = Color(0xFFFF5D91)
private val CocMuted = Color(0xFF9EAFCE)
private val CocCyan = Color(0xFF36F4FF)
private val CocMagenta = Color(0xFFFF4DDA)
private val CocViolet = Color(0xFF9470FF)

@Composable
private fun NeonBackdrop() {
    val transition = rememberInfiniteTransition(label = "background-glow")
    val pulse by transition.animateFloat(
        initialValue = 0.20f,
        targetValue = 0.42f,
        animationSpec = infiniteRepeatable(tween(3600), RepeatMode.Reverse),
        label = "glow-pulse"
    )
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF050817), Color(0xFF09132B), Color(0xFF070A1A))
                )
            )
    ) {
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .offset(x = 80.dp, y = (-70).dp)
                .size(250.dp)
                .blur(75.dp)
                .background(CocCyan.copy(alpha = pulse), CircleShape)
        )
        Box(
            Modifier
                .align(Alignment.CenterStart)
                .offset(x = (-145).dp, y = 60.dp)
                .size(230.dp)
                .blur(85.dp)
                .background(CocMagenta.copy(alpha = pulse * 0.58f), CircleShape)
        )
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 100.dp, y = 110.dp)
                .size(260.dp)
                .blur(90.dp)
                .background(CocViolet.copy(alpha = pulse * 0.38f), CircleShape)
        )
    }
}

@Composable
private fun NeonPanel(
    modifier: Modifier = Modifier,
    glow: Color = CocCyan,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier = modifier
            .shadow(15.dp, shape)
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(Color(0xF2192945), Color(0xF20B142A), Color(0xF21A1230))
                )
            )
            .border(
                BorderStroke(
                    1.dp,
                    Brush.linearGradient(
                        listOf(
                            glow.copy(alpha = 0.82f),
                            CocMagenta.copy(alpha = 0.48f),
                            CocViolet.copy(alpha = 0.30f)
                        )
                    )
                ),
                shape
            )
            .padding(14.dp),
        content = content
    )
}

@Composable
private fun NeonButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "button-glow")
    val pulse by transition.animateFloat(
        initialValue = 0.72f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1500), RepeatMode.Reverse),
        label = "button-pulse"
    )
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = modifier
            .shadow(13.dp, shape)
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(CocCyan.copy(alpha = 0.95f), CocViolet.copy(alpha = 0.94f), CocMagenta.copy(alpha = 0.90f))
                )
            )
            .border(
                1.dp,
                Brush.linearGradient(
                    listOf(Color.White.copy(alpha = pulse), CocCyan.copy(alpha = pulse * 0.8f))
                ),
                shape
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 17.dp, vertical = 11.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = Color(0xFF06101F), fontWeight = FontWeight.Black, fontSize = 12.sp, letterSpacing = 0.35.sp)
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = CocCyan,
                    onPrimary = Color(0xFF04101D),
                    secondary = CocMagenta,
                    background = CocBackground,
                    surface = CocSurface,
                    onBackground = Color.White,
                    onSurface = Color.White
                )
            ) {
                CocLioApp()
            }
        }
    }
}

@Composable
private fun CocLioApp() {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val initialHistory = remember { VillageStore.loadAll(context) }

    var history by remember { mutableStateOf(initialHistory) }
    var current by remember { mutableStateOf(initialHistory.firstOrNull()) }
    var events by remember { mutableStateOf(emptyList<ChangeEvent>()) }
    var selectedTab by remember { mutableStateOf(0) }
    var showImportDialog by remember { mutableStateOf(false) }
    var importError by remember { mutableStateOf<String?>(null) }
    var nowEpoch by remember { mutableLongStateOf(System.currentTimeMillis() / 1000L) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            nowEpoch = System.currentTimeMillis() / 1000L
        }
    }

    fun importJson(raw: String) {
        try {
            val parsed = VillageParser.parse(raw)
            val previous = history.firstOrNull {
                it.tag == parsed.tag && it.timestamp < parsed.timestamp
            }
            VillageStore.save(context, parsed)
            history = VillageStore.loadAll(context)
            current = parsed
            events = VillageParser.compare(previous, parsed)
            importError = null
            showImportDialog = false
            scope.launch {
                snackbar.showSnackbar(
                    if (previous == null) "Đã lưu mốc dữ liệu đầu tiên cho " + parsed.tag
                    else "Đã nhập JSON và so sánh với lần trước"
                )
            }
        } catch (error: Exception) {
            importError = error.message ?: "Không thể đọc JSON này."
        }
    }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                val raw = context.contentResolver.openInputStream(uri)
                    ?.bufferedReader()
                    ?.use { it.readText() }
                    ?: throw IllegalArgumentException("Không mở được tệp đã chọn.")
                importJson(raw)
            } catch (error: Exception) {
                importError = error.message ?: "Không thể đọc tệp JSON."
                showImportDialog = true
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        NeonBackdrop()
        Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("COC-LIO", color = CocCyan, fontSize = 23.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                    Text("THEO DÕI TIẾN TRÌNH NGÔI LÀNG", color = CocMuted, fontSize = 9.sp, letterSpacing = 1.sp)
                }
                NeonButton(
                    text = "＋ NHẬP JSON",
                    onClick = {
                        importError = null
                        showImportDialog = true
                    }
                )
            }
        },
        bottomBar = {
            NavigationBar(containerColor = Color(0xEE091226), contentColor = Color.White) {
                val navItems = listOf(
                    Triple("home", "Tổng quan", 0),
                    Triple("building", "Công trình", 1),
                    Triple("army", "Quân đội", 2),
                    Triple("history", "Lịch sử", 3)
                )
                navItems.forEach { item ->
                    NavigationBarItem(
                        selected = selectedTab == item.third,
                        onClick = { selectedTab = item.third },
                        icon = { Icon(imageVector = when (item.first) { "home" -> Icons.Filled.Home; "building" -> Icons.Filled.Business; "army" -> Icons.Filled.Groups; else -> Icons.Filled.History }, contentDescription = item.second, tint = if (selectedTab == item.third) CocCyan else CocMuted) },
                        label = { Text(item.second, fontSize = 10.sp, color = if (selectedTab == item.third) CocCyan else CocMuted) }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (current == null) {
                EmptyWelcome(
                    onImport = {
                        importError = null
                        showImportDialog = true
                    }
                )
            } else {
                Crossfade(targetState = selectedTab, label = "navigation-transition") { page ->
                when (page) {
                    0 -> OverviewScreen(
                        snapshot = current!!,
                        events = events,
                        nowEpoch = nowEpoch
                    )
                    1 -> CategoryScreen(
                        snapshot = current!!,
                        tab = "buildings",
                        nowEpoch = nowEpoch
                    )
                    2 -> CategoryScreen(
                        snapshot = current!!,
                        tab = "army",
                        nowEpoch = nowEpoch
                    )
                    else -> HistoryScreen(
                        history = history,
                        activeTag = current!!.tag,
                        onSelect = {
                            current = it
                            events = emptyList()
                            selectedTab = 0
                        }
                    )
                }
                }
            }
        }
    }
    }

    if (showImportDialog) {
        ImportDialog(
            error = importError,
            onDismiss = {
                showImportDialog = false
                importError = null
            },
            onPaste = {
                clipboard.getText()?.text?.let { it }
            },
            onChooseFile = {
                filePicker.launch(arrayOf("application/json", "text/plain", "*/*"))
            },
            onImport = { importJson(it) }
        )
    }
}

@Composable
private fun EmptyWelcome(onImport: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("🏰", fontSize = 64.sp)
        Spacer(Modifier.height(18.dp))
        Text("Bắt đầu theo dõi ngôi làng", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(9.dp))
        Text(
            "Nhập JSON được xuất trực tiếp trong Clash of Clans. Dữ liệu sẽ được lưu trên thiết bị này.",
            color = CocMuted,
            fontSize = 14.sp,
            lineHeight = 21.sp
        )
        Spacer(Modifier.height(24.dp))
        NeonButton(text = "NHẬP JSON ĐẦU TIÊN", onClick = onImport)
        Spacer(Modifier.height(12.dp))
        Text("Trong game: Cài đặt → Cài đặt khác → Xuất dữ liệu làng → Sao chép", color = CocMuted, fontSize = 11.sp)
    }
}

@Composable
private fun OverviewScreen(
    snapshot: VillageSnapshot,
    events: List<ChangeEvent>,
    nowEpoch: Long
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            NeonPanel(modifier = Modifier.fillMaxWidth(), glow = CocCyan) {
                Column(Modifier.padding(3.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("LÀNG CỦA BẠN", color = CocMuted, fontSize = 10.sp, letterSpacing = 1.2.sp)
                            Text(snapshot.tag, color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            snapshot.items.firstOrNull { it.sectionKey == "buildings" && it.dataId == 1000001 }?.let {
                                CategoryEmoji(it, 62)
                                Spacer(Modifier.width(8.dp))
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("NHÀ CHÍNH", color = CocCyan, fontSize = 9.sp, letterSpacing = 1.sp)
                                Text(snapshot.townHallLevel?.toString() ?: "—", color = CocGold, fontSize = 34.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Divider(color = CocSurfaceAlt)
                    Spacer(Modifier.height(12.dp))
                    Text("Lần xuất dữ liệu: " + formatTimestamp(snapshot.timestamp), color = CocMuted, fontSize = 11.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 12.dp)) {
                        StatTile("Công trình", snapshot.items.count { it.tab == "buildings" }.toString(), Modifier.weight(1f))
                        StatTile("Quân đội", snapshot.items.count { it.tab == "army" }.toString(), Modifier.weight(1f))
                        StatTile("Đang nâng", snapshot.upgrades.size.toString(), Modifier.weight(1f), CocGold)
                    }
                    if (snapshot.builderHallLevel != null) {
                        Text("Nhà Builder cấp " + snapshot.builderHallLevel, color = CocMuted, fontSize = 11.sp, modifier = Modifier.padding(top = 10.dp))
                    }
                }
            }
        }

        item {
            SectionTitle("ĐANG NÂNG CẤP", snapshot.upgrades.size.toString())
        }

        if (snapshot.upgrades.isEmpty() && snapshot.gearUps.isEmpty()) {
            item {
                InfoCard("Không có bộ đếm nâng cấp trong JSON này", "Khi một công trình, anh hùng hoặc đơn vị đang nâng cấp, game sẽ xuất trường timer.")
            }
        } else {
            items(snapshot.upgrades, key = { it.identity + "_" + it.timerSeconds }) { upgrade ->
                UpgradeCard(upgrade, snapshot.timestamp, nowEpoch)
            }
        }

        if (snapshot.gearUps.isNotEmpty()) {
            item { SectionTitle("GEAR-UP MASTER BUILDER", snapshot.gearUps.size.toString()) }
            items(snapshot.gearUps, key = { it.identity + "_gear_" + it.timerSeconds }) { item ->
                UpgradeCard(item, snapshot.timestamp, nowEpoch)
            }
        }

        if (events.isNotEmpty()) {
            item { SectionTitle("THAY ĐỔI SO VỚI LẦN TRƯỚC", events.size.toString()) }
            items(events, key = { it.title + it.detail }) { event ->
                ChangeCard(event)
            }
        }

        val helpers = snapshot.items.filter { it.sectionKey == "helpers" }
        if (helpers.isNotEmpty()) {
            item { SectionTitle("TRỢ THỦ", helpers.size.toString()) }
            items(helpers, key = { it.identity }) { item ->
                VillageItemCard(item)
            }
        }

        if (snapshot.extras.isNotEmpty()) {
            item { SectionTitle("DỮ LIỆU KHÁC", snapshot.extras.values.sum().toString()) }
            item {
                NeonPanel(modifier = Modifier.fillMaxWidth(), glow = CocViolet) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        snapshot.extras.forEach { (name, count) ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(name, color = Color.White, fontSize = 12.sp, modifier = Modifier.weight(1f))
                                Text(count.toString(), color = CocGold, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        item {
            NeonPanel(modifier = Modifier.fillMaxWidth(), glow = CocViolet) {
                Text(
                    "COC-LIO là nội dung cộng đồng không chính thức, không được Supercell chứng thực.",
                    color = CocMuted,
                    fontSize = 10.sp,
                    lineHeight = 15.sp
                )
                Text(
                    "This material is unofficial and is not endorsed by Supercell. For more information see www.supercell.com/fan-content-policy.",
                    color = CocCyan,
                    fontSize = 9.sp,
                    lineHeight = 13.sp
                )
            }
        }
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier = Modifier, valueColor: Color = Color.White) {
    NeonPanel(modifier = modifier, glow = valueColor.takeIf { it != Color.White } ?: CocMagenta) {
        Text(value, color = valueColor, fontSize = 24.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(3.dp))
        Text(label, color = CocMuted, fontSize = 10.sp, letterSpacing = 0.4.sp)
    }
}

@Composable
private fun SectionTitle(title: String, trailing: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 5.dp, bottom = 1.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, color = CocCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, modifier = Modifier.weight(1f))
        Text(trailing, color = CocMuted, fontSize = 11.sp)
    }
}

@Composable
private fun InfoCard(title: String, detail: String) {
    NeonPanel(modifier = Modifier.fillMaxWidth(), glow = CocViolet) {
        Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Spacer(Modifier.height(6.dp))
        Text(detail, color = CocMuted, fontSize = 11.sp, lineHeight = 17.sp)
    }
}

@Composable
private fun UpgradeCard(item: VillageItem, snapshotTimestamp: Long, nowEpoch: Long) {
    val endAt = snapshotTimestamp + (item.timerSeconds ?: 0L)
    val remaining = (endAt - nowEpoch).coerceAtLeast(0L)
    NeonPanel(modifier = Modifier.fillMaxWidth(), glow = if (item.gearUp) CocMagenta else CocCyan) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CategoryEmoji(item, 52)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(item.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(
                    (item.sectionLabel) + (item.level?.let { " • cấp $it" } ?: "") +
                        if (item.count > 1) " • x" + item.count else "",
                    color = CocMuted,
                    fontSize = 10.sp
                )
                Spacer(Modifier.height(5.dp))
                if (item.gearUp) {
                    Text("Gear-up • Master Builder", color = CocGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                if (remaining > 0L) {
                    Text("Còn lại " + formatDuration(remaining), color = CocGold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text("Dự kiến xong: " + formatTimestamp(endAt), color = CocMuted, fontSize = 10.sp)
                } else {
                    Text("Đã hết giờ • chờ JSON mới xác nhận", color = CocGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
            if (remaining > 0L) {
                Text("⏳", fontSize = 21.sp)
            } else {
                Text("✓", color = CocGreen, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ChangeCard(event: ChangeEvent) {
    NeonPanel(modifier = Modifier.fillMaxWidth(), glow = if (event.completed) CocGreen else CocMagenta) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(if (event.completed) "✦" else "↗", color = if (event.completed) CocGreen else CocMagenta, fontSize = 22.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(event.title, color = if (event.completed) CocGreen else CocCyan, fontWeight = FontWeight.Black, fontSize = 12.sp)
                Text(event.detail, color = CocMuted, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun CategoryScreen(snapshot: VillageSnapshot, tab: String, nowEpoch: Long) {
    var selectedBase by remember(snapshot.tag, tab) { mutableStateOf("home") }
    val filtered = snapshot.items.filter {
        it.tab == tab && it.base == selectedBase && it.sectionKey != "helpers"
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        item {
            Text(
                if (tab == "buildings") "CÔNG TRÌNH VÀ BẪY" else "QUÂN ĐỘI VÀ NGHIÊN CỨU",
                color = CocGold,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Text(snapshot.tag + " • " + filtered.size + " nhóm dữ liệu", color = CocMuted, fontSize = 11.sp)
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = selectedBase == "home",
                    onClick = { selectedBase = "home" },
                    label = { Text("Nhà chính") }
                )
                FilterChip(
                    selected = selectedBase == "builder",
                    onClick = { selectedBase = "builder" },
                    label = { Text("Builder Base") }
                )
            }
        }
        if (filtered.isEmpty()) {
            item {
                InfoCard(
                    "Chưa có dữ liệu trong mục này",
                    "JSON không chứa mục tương ứng hoặc làng này chưa mở khóa dữ liệu đó."
                )
            }
        } else {
            val grouped = filtered.groupBy { it.sectionKey }
            grouped.forEach { (sectionKey, rows) ->
                item(key = "section_$sectionKey") {
                    SectionTitle(rows.first().sectionLabel.uppercase(), rows.sumOf { it.count }.toString())
                }
                items(rows, key = { it.identity + "_" + (it.timerSeconds ?: -1L) + "_" + it.count }) { item ->
                    if (item.isRunning) {
                        UpgradeCard(item, snapshot.timestamp, nowEpoch)
                    } else {
                        VillageItemCard(item)
                    }
                }
            }
        }
    }
}

@Composable
private fun VillageItemCard(item: VillageItem) {
    NeonPanel(modifier = Modifier.fillMaxWidth(), glow = when (item.tab) {
        "army" -> CocMagenta
        else -> CocCyan
    }) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CategoryEmoji(item, 48)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(item.name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                val subtitle = buildString {
                    item.level?.let { append("Cấp $it") }
                    if (item.count > 1) {
                        if (isNotEmpty()) append(" • ")
                        append("Số lượng: " + item.count)
                    }
                    if (item.gearUp) {
                        if (isNotEmpty()) append(" • ")
                        append("Gear-up")
                    }
                    item.weaponLevel?.let {
                        if (isNotEmpty()) append(" • ")
                        append("Vũ khí cấp $it")
                    }
                    item.helperCooldownSeconds?.let {
                        if (isNotEmpty()) append(" • ")
                        append("Hồi chiêu " + formatDuration(it))
                    }
                }
                if (subtitle.isNotBlank()) {
                    Text(subtitle, color = CocMuted, fontSize = 10.sp)
                }
                item.details.forEach { detail ->
                    Text(detail, color = CocCyan, fontSize = 9.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
            item.level?.let {
                Text("LV.$it", color = CocCyan, fontWeight = FontWeight.Black, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun CategoryEmoji(item: VillageItem, size: Int) {
    val emoji = when (item.sectionKey) {
        "buildings", "buildings2" -> "🏰"
        "traps", "traps2" -> "💣"
        "units", "units2" -> "⚔️"
        "siege_machines" -> "🛠️"
        "heroes", "heroes2" -> "🦸"
        "spells" -> "✨"
        "pets" -> "🐾"
        "equipment" -> "🛡️"
        "helpers" -> "🧑‍🔧"
        else -> "🏷️"
    }
    val glow = when (item.tab) {
        "army" -> CocMagenta
        else -> CocCyan
    }
    val url = CocAssets.imageUrl(item)
    Box(
        modifier = Modifier
            .size(size.dp)
            .shadow(8.dp, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF101D36))
            .border(1.dp, glow.copy(alpha = 0.75f), RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(emoji, fontSize = (size * 0.52f).sp)
        if (url != null) {
            AsyncImage(
                model = url,
                contentDescription = item.name,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize().padding(3.dp)
            )
        }
    }
}

@Composable
private fun HistoryScreen(
    history: List<VillageSnapshot>,
    activeTag: String,
    onSelect: (VillageSnapshot) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text("LỊCH SỬ NHẬP JSON", color = CocCyan, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Text("Lưu trên thiết bị • tối đa 60 lần nhập gần nhất", color = CocMuted, fontSize = 10.sp)
        }
        if (history.isEmpty()) {
            item { InfoCard("Chưa có lịch sử", "Nhập JSON làng để tạo mốc dữ liệu đầu tiên.") }
        } else {
            items(history, key = { it.tag + "_" + it.timestamp }) { snapshot ->
                NeonPanel(
                    modifier = Modifier.fillMaxWidth().clickable { onSelect(snapshot) },
                    glow = if (snapshot.tag == activeTag) CocCyan else CocViolet
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(snapshot.tag, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text(formatTimestamp(snapshot.timestamp), color = CocMuted, fontSize = 10.sp)
                            Text(
                                "Nhà chính " + (snapshot.townHallLevel?.toString() ?: "—") +
                                    " • " + snapshot.items.size + " dòng dữ liệu" +
                                    " • " + (snapshot.upgrades.size + snapshot.gearUps.size) + " bộ đếm",
                                color = CocMuted,
                                fontSize = 10.sp
                            )
                        }
                        if (snapshot.tag == activeTag) {
                            Text("ĐANG XEM", color = CocGold, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        } else {
                            Text("MỞ", color = CocGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ImportDialog(
    error: String?,
    onDismiss: () -> Unit,
    onPaste: () -> String?,
    onChooseFile: () -> Unit,
    onImport: (String) -> Unit
) {
    var rawText by remember { mutableStateOf("") }
    Dialog(onDismissRequest = onDismiss) {
        NeonPanel(modifier = Modifier.fillMaxWidth(), glow = CocMagenta) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(11.dp)
            ) {
                Text("NHẬP DỮ LIỆU LÀNG", color = CocCyan, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text("Dán JSON vừa sao chép trong game hoặc chọn tệp .json.", color = CocMuted, fontSize = 11.sp)
                OutlinedTextField(
                    value = rawText,
                    onValueChange = { rawText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp),
                    placeholder = { Text("{ \"tag\": \"#...\", ... }", fontSize = 11.sp) },
                    minLines = 6,
                    maxLines = 10,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CocCyan,
                        unfocusedBorderColor = CocViolet,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = CocCyan
                    )
                )
                if (!error.isNullOrBlank()) {
                    Text(error, color = CocRed, fontSize = 11.sp)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { rawText = onPaste().orEmpty() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CocCyan),
                        border = BorderStroke(1.dp, CocCyan.copy(alpha = 0.65f))
                    ) {
                        Text("Dán clipboard", color = CocCyan, fontSize = 11.sp)
                    }
                    OutlinedButton(
                        onClick = onChooseFile,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CocCyan),
                        border = BorderStroke(1.dp, CocMagenta.copy(alpha = 0.65f))
                    ) {
                        Text("Chọn tệp", color = CocCyan, fontSize = 11.sp)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CocMuted),
                        border = BorderStroke(1.dp, CocViolet.copy(alpha = 0.65f))
                    ) {
                        Text("Đóng", color = CocCyan)
                    }
                    Button(
                        onClick = { onImport(rawText) },
                        enabled = rawText.isNotBlank(),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CocCyan, contentColor = Color(0xFF04101D))
                    ) {
                        Text("Phân tích & lưu", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    return try {
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
            .withZone(ZoneId.systemDefault())
            .format(Instant.ofEpochSecond(timestamp))
    } catch (_: Exception) {
        timestamp.toString()
    }
}

private fun formatDuration(seconds: Long): String {
    val days = seconds / 86400L
    val hours = (seconds % 86400L) / 3600L
    val minutes = (seconds % 3600L) / 60L
    val secs = seconds % 60L
    return buildString {
        if (days > 0) append(days).append(" ngày ")
        if (hours > 0 || days > 0) append(hours).append(" giờ ")
        if (minutes > 0 || hours > 0 || days > 0) append(minutes).append(" phút")
        else append(secs).append(" giây")
    }.trim()
}
