package com.huyhoa.coclio

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
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
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.core.content.ContextCompat
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF050817), Color(0xFF09132B), Color(0xFF070A1A))
                )
            )
    )
}

@Composable
private fun NeonPanel(
    modifier: Modifier = Modifier,
    glow: Color = CocCyan,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(18.dp)
    val palette = remember {
        listOf(
            Color(0xFF00E5FF),
            Color(0xFF168BFF),
            Color(0xFF6548FF),
            Color(0xFFB33CFF),
            Color(0xFFFF35B8),
            Color(0xFFFF4A72),
            Color(0xFFFFA43A),
            Color(0xFFB7F34A),
            Color(0xFF35E6A2),
            Color(0xFF00E5FF)
        )
    }
    val transition = rememberInfiniteTransition(label = "led-rgb-border")
    val ledProgress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "led-rgb-progress"
    )
    Column(
        modifier = modifier
            .shadow(5.dp, shape)
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(Color(0xF2192945), Color(0xF20B142A), Color(0xF21A1230))
                )
            )
            .drawWithCache {
                val stroke = 2.8.dp.toPx()
                val inset = stroke / 2f
                val borderPath = Path().apply {
                    addRoundRect(
                        RoundRect(
                            left = inset,
                            top = inset,
                            right = size.width - inset,
                            bottom = size.height - inset,
                            cornerRadius = CornerRadius(18.dp.toPx())
                        )
                    )
                }
                onDrawBehind {
                    val intervals = palette.size - 1
                    val movingColors = List(32) { index ->
                        val position = ((index / 32f + ledProgress) % 1f) * intervals
                        val startIndex = position.toInt().coerceIn(0, intervals - 1)
                        val fraction = position - startIndex
                        val first = palette[startIndex]
                        val second = palette[startIndex + 1]
                        Color(
                            red = first.red + (second.red - first.red) * fraction,
                            green = first.green + (second.green - first.green) * fraction,
                            blue = first.blue + (second.blue - first.blue) * fraction,
                            alpha = 1f
                        )
                    }
                    drawPath(
                        path = borderPath,
                        brush = Brush.sweepGradient(movingColors),
                        style = Stroke(width = stroke)
                    )
                }
            }
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
    val shape = RoundedCornerShape(14.dp)
    val ledColors = remember {
        listOf(
            Color(0xFF00E5FF),
            Color(0xFF168BFF),
            Color(0xFF6548FF),
            Color(0xFFB33CFF),
            Color(0xFFFF35B8),
            Color(0xFFFF4A72),
            Color(0xFFFFA43A),
            Color(0xFFB7F34A),
            Color(0xFF35E6A2),
            Color(0xFF00E5FF)
        )
    }
    Box(
        modifier = modifier
            .shadow(5.dp, shape)
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(CocCyan.copy(alpha = 0.95f), CocViolet.copy(alpha = 0.94f), CocMagenta.copy(alpha = 0.90f))
                )
            )
            .border(2.dp, Brush.sweepGradient(ledColors), shape)
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
    var activeProfile by remember { mutableStateOf(AccountProfileStore.active(context)) }
    val initialHistory = remember(activeProfile) { VillageStore.loadAll(context, activeProfile) }

    var history by remember { mutableStateOf(initialHistory) }
    var current by remember { mutableStateOf(initialHistory.firstOrNull()) }
    var events by remember { mutableStateOf(emptyList<ChangeEvent>()) }
    var selectedTab by remember { mutableStateOf(0) }
    var showImportDialog by remember { mutableStateOf(false) }
    var importError by remember { mutableStateOf<String?>(null) }
    var notificationsEnabled by remember(activeProfile) {
        mutableStateOf(AppPreferences.notificationsEnabled(context, activeProfile))
    }
    var notificationStyle by remember(activeProfile) {
        mutableStateOf(AppPreferences.notificationStyle(context, activeProfile))
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            AppPreferences.setNotificationsEnabled(context, activeProfile, true)
            notificationsEnabled = true
            UpgradeReminderScheduler.scheduleAll(context, history, activeProfile, true)
        } else {
            notificationsEnabled = false
            scope.launch { snackbar.showSnackbar("Bạn chưa cấp quyền thông báo cho COC-LIO.") }
        }
    }

    LaunchedEffect(activeProfile) {
        history = VillageStore.loadAll(context, activeProfile)
        current = history.firstOrNull()
        events = emptyList()
        notificationsEnabled = AppPreferences.notificationsEnabled(context, activeProfile)
        notificationStyle = AppPreferences.notificationStyle(context, activeProfile)
        UpgradeReminderScheduler.scheduleAll(context, history, activeProfile, notificationsEnabled)
    }

    LaunchedEffect(Unit) {
        AppWidgetUpdater.ensureRefresh(context)
    }

    fun importJson(raw: String, nameOverride: String? = null) {
        try {
            val parsedFromJson = VillageParser.parse(raw)
            val customName = nameOverride?.trim()?.takeIf { it.isNotBlank() }
            val parsed = if (customName == null) parsedFromJson
            else parsedFromJson.copy(villageName = customName)
            val previous = history.firstOrNull {
                it.tag == parsed.tag && it.timestamp < parsed.timestamp
            }
            VillageStore.save(context, parsed, activeProfile, customName)
            history = VillageStore.loadAll(context, activeProfile)
            UpgradeReminderScheduler.scheduleAll(context, history, activeProfile, notificationsEnabled)
            AppWidgetUpdater.updateAll(context)
            current = history.firstOrNull { it.tag == parsed.tag } ?: parsed
            events = VillageParser.compare(previous, parsed)
            importError = null
            showImportDialog = false
            scope.launch {
                snackbar.showSnackbar(
                    if (previous == null) "Đã lưu dữ liệu đầu tiên cho " + (parsed.villageName ?: parsed.tag)
                    else "Đã nhập JSON và so sánh với lần trước"
                )
            }
        } catch (error: Exception) {
            importError = error.message ?: "Không thể đọc JSON này."
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
                    .statusBarsPadding()
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
                    Triple("history", "Lịch sử", 3),
                    Triple("settings", "Cài đặt", 4)
                )
                navItems.forEach { item ->
                    NavigationBarItem(
                        selected = selectedTab == item.third,
                        onClick = { selectedTab = item.third },
                        icon = { Icon(imageVector = when (item.first) { "home" -> Icons.Filled.Home; "building" -> Icons.Filled.Business; "army" -> Icons.Filled.Groups; "history" -> Icons.Filled.History; else -> Icons.Filled.Settings }, contentDescription = item.second, tint = if (selectedTab == item.third) CocCyan else CocMuted) },
                        label = { Text(item.second, fontSize = 9.sp, maxLines = 1, color = if (selectedTab == item.third) CocCyan else CocMuted) }
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
            Crossfade(targetState = selectedTab, label = "navigation-transition") { page ->
                when (page) {
                    0 -> current?.let {
                        OverviewScreen(snapshot = it, events = events)
                    } ?: EmptyWelcome(
                        onImport = { importError = null; showImportDialog = true },
                        onOpenGame = { openClashOfClans(context) }
                    )
                    1 -> current?.let {
                        CategoryScreen(snapshot = it, tab = "buildings")
                    } ?: EmptyWelcome(
                        onImport = { importError = null; showImportDialog = true },
                        onOpenGame = { openClashOfClans(context) }
                    )
                    2 -> current?.let {
                        CategoryScreen(snapshot = it, tab = "army")
                    } ?: EmptyWelcome(
                        onImport = { importError = null; showImportDialog = true },
                        onOpenGame = { openClashOfClans(context) }
                    )
                    3 -> HistoryScreen(
                        history = history,
                        activeTag = current?.tag.orEmpty(),
                        onSelect = {
                            current = it
                            events = emptyList()
                            selectedTab = 0
                        }
                    )
                    else -> SettingsScreen(
                        accountEmail = activeProfile.takeUnless { it == "local" },
                        notificationsEnabled = notificationsEnabled,
                        notificationStyle = notificationStyle,
                        onGoogleSignIn = {
                            scope.launch {
                                try {
                                    val email = GoogleAccountAuth.signIn(context)
                                    AccountProfileStore.setActive(context, email)
                                    activeProfile = email
                                    selectedTab = 0
                                    snackbar.showSnackbar("Đã chuyển sang hồ sơ " + email)
                                } catch (error: Exception) {
                                    snackbar.showSnackbar(error.message ?: "Không thể đăng nhập Google.")
                                }
                            }
                        },
                        onGoogleSignOut = {
                            scope.launch {
                                GoogleAccountAuth.signOut(context)
                                AccountProfileStore.setActive(context, null)
                                activeProfile = "local"
                                selectedTab = 0
                                snackbar.showSnackbar("Đã chuyển về dữ liệu trên thiết bị.")
                            }
                        },
                        onOpenGame = { openClashOfClans(context) },
                        onNotificationsChanged = { enabled ->
                            if (enabled && Build.VERSION.SDK_INT >= 33 &&
                                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                            ) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                AppPreferences.setNotificationsEnabled(context, activeProfile, enabled)
                                notificationsEnabled = enabled
                                UpgradeReminderScheduler.scheduleAll(context, history, activeProfile, enabled)
                            }
                        },
                        onNotificationStyleChanged = { style ->
                            AppPreferences.setNotificationStyle(context, activeProfile, style)
                            notificationStyle = style
                            UpgradeReminderScheduler.scheduleAll(context, history, activeProfile, notificationsEnabled)
                        }
                    )
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
            onImport = { raw, name -> importJson(raw, name) }
        )
    }
}

@Composable
private fun EmptyWelcome(onImport: () -> Unit, onOpenGame: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("🏰", fontSize = 62.sp)
        Spacer(Modifier.height(14.dp))
        Text("Bắt đầu theo dõi ngôi làng", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            "Lấy JSON trực tiếp từ game để COC-LIO đọc cấp độ, công trình đang nâng và thời gian còn lại.",
            color = CocMuted, fontSize = 12.sp, lineHeight = 18.sp
        )
        Spacer(Modifier.height(18.dp))
        NeonPanel(modifier = Modifier.fillMaxWidth(), glow = CocCyan) {
            Text("CÁCH LẤY JSON LẦN ĐẦU", color = CocCyan, fontSize = 12.sp, fontWeight = FontWeight.Black)
            Text("1. Mở Clash of Clans và vào làng cần theo dõi.", color = Color.White, fontSize = 11.sp)
            Text("2. Nhấn bánh răng Cài đặt → Cài đặt khác.", color = Color.White, fontSize = 11.sp)
            Text("3. Kéo xuống Xuất dữ liệu → Xuất dữ liệu làng dưới dạng JSON → Sao chép.", color = Color.White, fontSize = 11.sp)
            Text("4. Quay lại COC-LIO, dán JSON hoặc chọn tệp .json rồi nhấn Phân tích & lưu.", color = Color.White, fontSize = 11.sp)
        }
        Spacer(Modifier.height(14.dp))
        NeonButton(text = "MỞ CLASH OF CLANS", onClick = onOpenGame, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(10.dp))
        NeonButton(text = "NHẬP JSON ĐẦU TIÊN", onClick = onImport, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        Text("Nút mở game chỉ đưa bạn vào trò chơi. Bạn cần tự thao tác xuất và sao chép JSON trong game.", color = CocMuted, fontSize = 9.sp, lineHeight = 14.sp)
    }
}

@Composable
private fun OverviewScreen(
    snapshot: VillageSnapshot,
    events: List<ChangeEvent>
) {
    val homeRunning = (snapshot.upgrades + snapshot.gearUps).filter { it.base == "home" }
    val builderRunning = (snapshot.upgrades + snapshot.gearUps).filter { it.base == "builder" }
    val homeHall = snapshot.items.firstOrNull {
        it.sectionKey == "buildings" && it.dataId == 1000001
    }
    val builderHall = snapshot.items.firstOrNull {
        it.sectionKey == "buildings2" && it.dataId == 1000034
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 9.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            NeonPanel(modifier = Modifier.fillMaxWidth(), glow = CocCyan) {
                Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                    Text("LÀNG ĐANG THEO DÕI", color = CocMuted, fontSize = 9.sp, letterSpacing = 1.sp)
                    Text(
                        snapshot.villageName ?: snapshot.tag,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!snapshot.villageName.isNullOrBlank()) {
                        Text(snapshot.tag, color = CocMuted, fontSize = 9.sp)
                    }
                    Text("Dữ liệu cập nhật: " + formatTimestamp(snapshot.timestamp), color = CocMuted, fontSize = 9.sp)
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BaseSummaryCard(
                    title = "LÀNG CHÍNH",
                    hallLabel = "Nhà chính",
                    level = snapshot.townHallLevel,
                    itemCount = snapshot.homeItems.size,
                    upgradeCount = homeRunning.size,
                    hallItem = homeHall,
                    modifier = Modifier.weight(1f),
                    glow = CocCyan
                )
                BaseSummaryCard(
                    title = "LÀNG ĐÊM",
                    hallLabel = "Nhà thợ",
                    level = snapshot.builderHallLevel,
                    itemCount = snapshot.builderItems.size,
                    upgradeCount = builderRunning.size,
                    hallItem = builderHall,
                    modifier = Modifier.weight(1f),
                    glow = CocMagenta
                )
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                StatTile("Công trình", snapshot.items.count { it.tab == "buildings" }.toString(), Modifier.weight(1f))
                StatTile("Quân đội", snapshot.items.count { it.tab == "army" }.toString(), Modifier.weight(1f))
                StatTile("Đang nâng", homeRunning.size.plus(builderRunning.size).toString(), Modifier.weight(1f), CocGold)
            }
        }

        item {
            SectionTitle("LÀNG CHÍNH • ĐANG NÂNG CẤP", homeRunning.size.toString())
        }
        if (homeRunning.isEmpty()) {
            item { InfoCard("Làng chính không có mục đang nâng", "Khi JSON có bộ đếm thời gian, tiến trình sẽ xuất hiện tại đây.") }
        } else {
            items(homeRunning, key = { "home_" + it.identity + "_" + it.timerSeconds }) { item ->
                UpgradeCard(item, snapshot.timestamp)
            }
        }

        item {
            SectionTitle("LÀNG ĐÊM • ĐANG NÂNG CẤP", builderRunning.size.toString())
        }
        if (builderRunning.isEmpty()) {
            item { InfoCard("Làng đêm không có mục đang nâng", "Các công trình và quân đội căn cứ thợ xây sẽ hiển thị riêng tại đây.") }
        } else {
            items(builderRunning, key = { "builder_" + it.identity + "_" + it.timerSeconds }) { item ->
                UpgradeCard(item, snapshot.timestamp)
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
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        snapshot.extras.forEach { (name, count) ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(name, color = Color.White, fontSize = 11.sp, modifier = Modifier.weight(1f))
                                Text(count.toString(), color = CocGold, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BaseSummaryCard(
    title: String,
    hallLabel: String,
    level: Int?,
    itemCount: Int,
    upgradeCount: Int,
    hallItem: VillageItem?,
    modifier: Modifier,
    glow: Color
) {
    NeonPanel(modifier = modifier, glow = glow) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = glow, fontSize = 9.sp, fontWeight = FontWeight.Black, maxLines = 1)
                Text(
                    level?.let { "Cấp $it" } ?: "Chưa có dữ liệu",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1
                )
                Text(hallLabel, color = CocMuted, fontSize = 8.sp, maxLines = 1)
                Text("$itemCount mục • $upgradeCount nâng", color = CocMuted, fontSize = 8.sp, maxLines = 1)
            }
            hallItem?.let { CategoryImage(it, 36) }
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
private fun UpgradeCard(item: VillageItem, snapshotTimestamp: Long) {
    var nowEpoch by remember(item.identity, snapshotTimestamp) {
        mutableLongStateOf(System.currentTimeMillis() / 1000L)
    }
    LaunchedEffect(item.identity, snapshotTimestamp) {
        while (true) {
            delay(1000L)
            nowEpoch = System.currentTimeMillis() / 1000L
        }
    }
    val endAt = snapshotTimestamp + (item.timerSeconds ?: 0L)
    val remaining = (endAt - nowEpoch).coerceAtLeast(0L)
    NeonPanel(modifier = Modifier.fillMaxWidth(), glow = if (item.gearUp) CocMagenta else CocCyan) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 9.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CategoryImage(item, 48)
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(item.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
                Text(
                    item.sectionLabel + (item.level?.let { " • cấp $it" } ?: "") +
                        if (item.count > 1) " • x" + item.count else "",
                    color = CocMuted,
                    fontSize = 9.sp,
                    maxLines = 1
                )
                if (item.gearUp) {
                    Text("Nâng cấp đặc biệt • Thợ xây cả", color = CocGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
                if (remaining > 0L) {
                    val totalSeconds = (item.timerSeconds ?: 1L).coerceAtLeast(1L)
                    val progress = (remaining.toFloat() / totalSeconds.toFloat()).coerceIn(0f, 1f)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                            .height(19.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(CocSurfaceAlt),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(progress)
                                .align(Alignment.CenterStart)
                                .background(
                                    Brush.horizontalGradient(
                                        if (item.gearUp) listOf(CocMagenta, CocViolet)
                                        else listOf(CocCyan, Color(0xFF168BFF))
                                    )
                                )
                        )
                        Text(
                            "Còn " + formatCountdown(remaining),
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                } else {
                    Text("Đã hết giờ • chờ JSON mới xác nhận", color = CocGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
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
private fun CategoryScreen(snapshot: VillageSnapshot, tab: String) {
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
            Text((snapshot.villageName?.takeIf { it.isNotBlank() }?.let { it + " • " + snapshot.tag } ?: snapshot.tag) + " • " + filtered.size + " nhóm dữ liệu", color = CocMuted, fontSize = 10.sp)
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
                    label = { Text("Căn cứ thợ xây") }
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
                        UpgradeCard(item, snapshot.timestamp)
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
            CategoryImage(item, 66)
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
                        append("Nâng cấp đặc biệt")
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
                Text("CẤP $it", color = CocCyan, fontWeight = FontWeight.Black, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun CategoryImage(item: VillageItem, size: Int) {
    val context = LocalContext.current
    val url = CocAssets.imageUrl(item)
    val imageRequest = remember(url) {
        url?.let {
            ImageRequest.Builder(context)
                .data(it)
                .crossfade(false)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .diskCachePolicy(CachePolicy.ENABLED)
                .build()
        }
    }
    if (imageRequest == null) {
        Spacer(Modifier.size(size.dp))
    } else {
        AsyncImage(
            model = imageRequest,
            contentDescription = item.name,
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(size.dp)
        )
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
                            Text(snapshot.villageName ?: snapshot.tag, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            if (!snapshot.villageName.isNullOrBlank()) Text(snapshot.tag, color = CocMuted, fontSize = 9.sp)
                            Text(formatTimestamp(snapshot.timestamp), color = CocMuted, fontSize = 10.sp)
                            Text(
                                "Nhà chính " + (snapshot.townHallLevel?.toString() ?: "—") +
                                    " • " + snapshot.items.size + " dòng dữ liệu" +
                                    " • " + (snapshot.upgrades.size + snapshot.gearUps.size) + " mục đang nâng",
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
    onImport: (String, String) -> Unit
) {
    var rawText by remember { mutableStateOf("") }
    var villageName by remember { mutableStateOf("") }
    Dialog(onDismissRequest = onDismiss) {
        NeonPanel(modifier = Modifier.fillMaxWidth(), glow = CocMagenta) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(11.dp)
            ) {
                Text("NHẬP DỮ LIỆU LÀNG", color = CocCyan, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text("Sao chép JSON trong Clash of Clans rồi dán vào ô bên dưới. Có thể đặt tên riêng cho làng. JSON là ảnh chụp tại thời điểm xuất; nếu dùng sách, potion, trợ thủ hoặc tăng tốc sau đó, hãy nhập JSON mới.", color = CocMuted, fontSize = 10.sp, lineHeight = 14.sp)
                OutlinedTextField(
                    value = villageName,
                    onValueChange = { villageName = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("Tên làng (không bắt buộc)") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CocCyan,
                        unfocusedBorderColor = CocViolet,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = CocCyan
                    )
                )
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
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CocCyan),
                        border = BorderStroke(1.dp, CocCyan.copy(alpha = 0.65f))
                    ) {
                        Text("DÁN JSON ĐÃ SAO CHÉP", color = CocCyan, fontSize = 10.sp)
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
                        onClick = { onImport(rawText, villageName) },
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

private fun formatCountdown(seconds: Long): String {
    val safe = seconds.coerceAtLeast(0L)
    val days = safe / 86400L
    val hours = (safe % 86400L) / 3600L
    val minutes = (safe % 3600L) / 60L
    val secs = safe % 60L
    val clock = "%02d:%02d:%02d".format(hours, minutes, secs)
    return if (days > 0L) "$days ngày $clock" else clock
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
