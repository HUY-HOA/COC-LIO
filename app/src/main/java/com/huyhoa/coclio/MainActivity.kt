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
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
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
import com.huyhoa.coclio.data.DeletedVillageRecord
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
private val DefaultLedPalette = listOf(
    Color(0xFF00E5FF), Color(0xFF168BFF), Color(0xFF6548FF),
    Color(0xFFB33CFF), Color(0xFFFF35B8), Color(0xFFFF4A72),
    Color(0xFFFFA43A), Color(0xFFB7F34A), Color(0xFF35E6A2),
    Color(0xFF00E5FF)
)

data class LedThemeState(
    val palette: List<Color>,
    val enabled: Boolean,
    val speedMillis: Int,
    val brightness: Float
)

val LocalLedTheme = staticCompositionLocalOf {
    LedThemeState(DefaultLedPalette, true, 3000, 0.9f)
}

private fun paletteForLed(preset: String): List<Color> = when (preset) {
    "ice" -> listOf(Color(0xFFB9F8FF), Color(0xFF36F4FF), Color(0xFF168BFF), Color(0xFFB9F8FF))
    "violet" -> listOf(Color(0xFFFFA5F5), Color(0xFFB33CFF), Color(0xFF6548FF), Color(0xFFFF35B8))
    "fire" -> listOf(Color(0xFFFFE082), Color(0xFFFFA43A), Color(0xFFFF4A35), Color(0xFFFF35B8))
    "mint" -> listOf(Color(0xFFB7F34A), Color(0xFF35E6A2), Color(0xFF00CFA8), Color(0xFFB7F34A))
    else -> DefaultLedPalette
}

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
    innerPadding: androidx.compose.ui.unit.Dp = 14.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(18.dp)
    val led = LocalLedTheme.current
    val palette = led.palette.map { it.copy(alpha = led.brightness) }
    val transition = rememberInfiniteTransition(label = "led-rgb-border")
    val ledProgress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (led.enabled) led.speedMillis else 60000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "led-rgb-progress"
    )
    Column(
        modifier = modifier
            .shadow(5.dp, shape)
            .clip(shape)
            .background(Brush.linearGradient(listOf(Color(0xF2192945), Color(0xF20B142A), Color(0xF21A1230))))
            .drawWithCache {
                val stroke = 2.8.dp.toPx()
                val inset = stroke / 2f
                val path = Path().apply {
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
                            alpha = led.brightness
                        )
                    }
                    val brush = if (led.enabled) Brush.sweepGradient(movingColors) else SolidColor(palette.first())
                    drawPath(path, brush, style = Stroke(width = stroke))
                }
            }
            .padding(innerPadding),
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
    val led = LocalLedTheme.current
    val palette = led.palette.map { it.copy(alpha = led.brightness) }
    Box(
        modifier = modifier
            .shadow(5.dp, shape)
            .clip(shape)
            .background(Brush.horizontalGradient(palette.take(3)))
            .border(
                2.dp,
                if (led.enabled) Brush.sweepGradient(palette) else SolidColor(palette.first()),
                shape
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = Color(0xFF06101F), fontWeight = FontWeight.Black, fontSize = 11.sp, letterSpacing = 0.25.sp)
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
    var deletedVillages by remember(activeProfile) {
        mutableStateOf(VillageStore.loadDeleted(context, activeProfile))
    }
    var pinnedTags by remember(activeProfile) {
        mutableStateOf(VillageStore.pinnedTags(context, activeProfile))
    }
    val ledPrefs = remember {
        context.getSharedPreferences("coc_lio_led", android.content.Context.MODE_PRIVATE)
    }
    var ledEnabled by remember { mutableStateOf(ledPrefs.getBoolean("enabled", true)) }
    var ledPreset by remember { mutableStateOf(ledPrefs.getString("preset", "rainbow") ?: "rainbow") }
    var ledSpeed by remember { mutableStateOf(ledPrefs.getInt("speed", 2)) }
    var ledBrightness by remember { mutableStateOf(ledPrefs.getFloat("brightness", 0.9f)) }
    val ledTheme = LedThemeState(
        palette = paletteForLed(ledPreset),
        enabled = ledEnabled,
        speedMillis = when (ledSpeed.coerceIn(1, 5)) {
            1 -> 8000
            2 -> 6000
            3 -> 4500
            4 -> 3200
            else -> 2200
        },
        brightness = ledBrightness.coerceIn(0.25f, 1f)
    )

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

    val testNotificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val sent = UpgradeNotificationTester.show(context, activeProfile, notificationStyle)
            scope.launch {
                snackbar.showSnackbar(
                    if (sent) "Đã gửi thông báo thử. Hãy kéo thanh thông báo xuống để xem."
                    else "Không thể gửi thông báo thử. Hãy kiểm tra quyền thông báo của ứng dụng."
                )
            }
        } else {
            scope.launch { snackbar.showSnackbar("Bạn chưa cấp quyền thông báo cho COC-LIO.") }
        }
    }

    LaunchedEffect(activeProfile) {
        history = VillageStore.loadAll(context, activeProfile)
        current = history.firstOrNull()
        deletedVillages = VillageStore.loadDeleted(context, activeProfile)
        pinnedTags = VillageStore.pinnedTags(context, activeProfile)
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
            pinnedTags = VillageStore.pinnedTags(context, activeProfile)
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

    CompositionLocalProvider(LocalLedTheme provides ledTheme) {
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
                    Text("THEO DÕI TIẾN TRÌNH", color = CocMuted, fontSize = 9.sp, letterSpacing = 1.sp)
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
                    Triple("buildings", "Công trình", 1),
                    Triple("army", "Quân đội", 2),
                    Triple("history", "Kho JSON", 3),
                    Triple("settings", "Cài đặt", 4)
                )
                navItems.forEach { item ->
                    NavigationBarItem(
                        selected = selectedTab == item.third,
                        onClick = { selectedTab = item.third },
                        icon = {
                            Icon(
                                imageVector = when (item.first) {
                                    "home" -> Icons.Filled.Home
                                    "buildings" -> Icons.Filled.Business
                                    "army" -> Icons.Filled.Groups
                                    "history" -> Icons.Filled.History
                                    else -> Icons.Filled.Settings
                                },
                                contentDescription = item.second,
                                tint = if (selectedTab == item.third) CocCyan else CocMuted
                            )
                        },
                        label = {
                            Text(item.second, fontSize = 9.sp, maxLines = 1, color = if (selectedTab == item.third) CocCyan else CocMuted)
                        }
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
                        deletedVillages = deletedVillages,
                        pinnedTags = pinnedTags,
                        activeTag = current?.tag.orEmpty(),
                        onSelect = {
                            current = it
                            events = emptyList()
                            selectedTab = 0
                        },
                        onTogglePin = { tag ->
                            VillageStore.setPinned(context, tag, tag !in pinnedTags, activeProfile)
                            pinnedTags = VillageStore.pinnedTags(context, activeProfile)
                            history = VillageStore.loadAll(context, activeProfile)
                            AppWidgetUpdater.updateAll(context)
                        },
                        onDelete = { tag ->
                            if (VillageStore.deleteVillage(context, tag, activeProfile)) {
                                history = VillageStore.loadAll(context, activeProfile)
                                deletedVillages = VillageStore.loadDeleted(context, activeProfile)
                                pinnedTags = VillageStore.pinnedTags(context, activeProfile)
                                if (current?.tag == tag) current = history.firstOrNull()
                                events = emptyList()
                                UpgradeReminderScheduler.scheduleAll(context, history, activeProfile, notificationsEnabled)
                                AppWidgetUpdater.updateAll(context)
                                scope.launch { snackbar.showSnackbar("Đã chuyển $tag vào lịch sử xóa.") }
                            }
                        },
                        onRestore = { recordId ->
                            val restoredTag = deletedVillages.firstOrNull { it.id == recordId }?.tag
                            if (VillageStore.restoreDeleted(context, recordId, activeProfile)) {
                                history = VillageStore.loadAll(context, activeProfile)
                                deletedVillages = VillageStore.loadDeleted(context, activeProfile)
                                pinnedTags = VillageStore.pinnedTags(context, activeProfile)
                                current = history.firstOrNull { it.tag == restoredTag } ?: history.firstOrNull()
                                events = emptyList()
                                UpgradeReminderScheduler.scheduleAll(context, history, activeProfile, notificationsEnabled)
                                AppWidgetUpdater.updateAll(context)
                                scope.launch { snackbar.showSnackbar("Đã khôi phục dữ liệu làng.") }
                            }
                        }
                    )
                    4 -> SettingsScreen(
                        accountEmail = activeProfile.takeUnless { it == "local" },
                        notificationsEnabled = notificationsEnabled,
                        ledEnabled = ledEnabled,
                        ledPreset = ledPreset,
                        ledSpeed = ledSpeed,
                        ledBrightness = ledBrightness,
                        onLedEnabledChanged = { value ->
                            ledEnabled = value
                            ledPrefs.edit().putBoolean("enabled", value).apply()
                        },
                        onLedPresetChanged = { value ->
                            ledPreset = value
                            ledPrefs.edit().putString("preset", value).apply()
                        },
                        onLedSpeedChanged = { value ->
                            ledSpeed = value.coerceIn(1, 5)
                            ledPrefs.edit().putInt("speed", ledSpeed).apply()
                        },
                        onLedBrightnessChanged = { value ->
                            ledBrightness = value.coerceIn(0.25f, 1f)
                            ledPrefs.edit().putFloat("brightness", value).apply()
                        },
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
                        },
                        onTestNotification = {
                            if (Build.VERSION.SDK_INT >= 33 &&
                                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                            ) {
                                testNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                val sent = UpgradeNotificationTester.show(context, activeProfile, notificationStyle)
                                scope.launch {
                                    snackbar.showSnackbar(
                                        if (sent) "Đã gửi thông báo thử. Hãy kéo thanh thông báo xuống để xem."
                                        else "Không thể gửi thông báo thử. Hãy kiểm tra quyền thông báo của ứng dụng."
                                    )
                                }
                            }
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
        Text("Bắt đầu theo dõi", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            "Lấy JSON trực tiếp từ game để COC-LIO đọc cấp độ, công trình đang nâng và thời gian còn lại.",
            color = CocMuted, fontSize = 12.sp, lineHeight = 18.sp
        )
        Spacer(Modifier.height(18.dp))
        NeonPanel(modifier = Modifier.fillMaxWidth(), glow = CocCyan) {
            Text("CÁCH LẤY JSON LẦN ĐẦU", color = CocCyan, fontSize = 12.sp, fontWeight = FontWeight.Black)
            Text("1. Mở Clash of Clans và vào làng cần theo dõi.", color = Color.White, fontSize = 12.sp)
            Text("2. Nhấn bánh răng Cài đặt → Cài đặt khác.", color = Color.White, fontSize = 12.sp)
            Text("3. Kéo xuống Xuất dữ liệu → Xuất dữ liệu làng dưới dạng JSON → Sao chép.", color = Color.White, fontSize = 12.sp)
            Text("4. Quay lại COC-LIO, dán JSON rồi nhấn Phân tích & lưu.", color = Color.White, fontSize = 11.sp)
        }
        Spacer(Modifier.height(14.dp))
        NeonButton(text = "MỞ CLASH OF CLANS", onClick = onOpenGame, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(10.dp))
        NeonButton(text = "NHẬP JSON ĐẦU TIÊN", onClick = onImport, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        Text("Nút mở game chỉ đưa bạn vào trò chơi. Bạn cần tự thao tác xuất và sao chép JSON trong game.", color = CocMuted, fontSize = 10.sp, lineHeight = 15.sp)
    }
}

@Composable
private fun OverviewScreen(
    snapshot: VillageSnapshot,
    events: List<ChangeEvent>
) {
    var nowEpoch by remember(snapshot.tag, snapshot.timestamp) {
        mutableLongStateOf(System.currentTimeMillis() / 1000L)
    }
    LaunchedEffect(snapshot.tag, snapshot.timestamp) {
        while (true) {
            delay(1000L)
            nowEpoch = System.currentTimeMillis() / 1000L
        }
    }
    val homeRunning = (snapshot.upgrades + snapshot.gearUps).filter { it.base == "home" }
    val builderRunning = (snapshot.upgrades + snapshot.gearUps).filter { it.base == "builder" }
    val homeHall = snapshot.items.firstOrNull { it.sectionKey == "buildings" && it.dataId == 1000001 }
    val builderHall = snapshot.items.firstOrNull { it.sectionKey == "buildings2" && it.dataId == 1000034 }
    val builderTotal = snapshot.homeBuilderTotal
    val builderBusy = snapshot.homeBuildersBusyAt(nowEpoch)
    val builderFree = builderTotal?.let { (it - builderBusy).coerceAtLeast(0) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        item {
            NeonPanel(modifier = Modifier.fillMaxWidth(), glow = CocCyan) {
                Column(Modifier.padding(horizontal = 9.dp, vertical = 7.dp)) {
                    Text("ĐANG THEO DÕI", color = CocMuted, fontSize = 8.sp, letterSpacing = 1.sp)
                    Text(
                        snapshot.villageName ?: snapshot.tag,
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!snapshot.villageName.isNullOrBlank()) Text(snapshot.tag, color = CocMuted, fontSize = 8.sp)
                    Text("Cập nhật " + formatTimestamp(snapshot.timestamp), color = CocMuted, fontSize = 8.sp)
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                BaseSummaryCard(
                    title = "NHÀ CHÍNH",
                    levelLabel = "TH",
                    level = snapshot.townHallLevel,
                    itemCount = snapshot.homeItems.size,
                    upgradeCount = homeRunning.size,
                    hallItem = homeHall,
                    modifier = Modifier.weight(1f),
                    glow = CocCyan
                )
                BaseSummaryCard(
                    title = "LÀNG ĐÊM",
                    levelLabel = "BH",
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
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                StatTile("Công trình", snapshot.items.count { it.tab == "buildings" }.toString(), Modifier.weight(1f))
                StatTile("Đang nâng", (homeRunning.size + builderRunning.size).toString(), Modifier.weight(1f), CocGold)
                StatTile("Thợ rảnh", if (builderTotal == null) "—" else "$builderFree/$builderTotal", Modifier.weight(1f), CocGreen)
            }
        }

        item { SectionTitle("NHÀ CHÍNH • ĐANG NÂNG", homeRunning.size.toString()) }
        if (homeRunning.isEmpty()) {
            item { InfoCard("Không có tiến trình", "Các mục đang nâng của Nhà chính sẽ xuất hiện ở đây.") }
        } else {
            items(homeRunning, key = { "home_" + it.identity + "_" + it.timerSeconds }) { UpgradeCard(it, snapshot.timestamp) }
        }

        item { SectionTitle("LÀNG ĐÊM • ĐANG NÂNG", builderRunning.size.toString()) }
        if (builderRunning.isEmpty()) {
            item { InfoCard("Không có tiến trình", "Các mục đang nâng của Làng đêm sẽ xuất hiện ở đây.") }
        } else {
            items(builderRunning, key = { "builder_" + it.identity + "_" + it.timerSeconds }) { UpgradeCard(it, snapshot.timestamp) }
        }

        if (events.isNotEmpty()) {
            item { SectionTitle("THAY ĐỔI", events.size.toString()) }
            items(events, key = { it.title + it.detail }) { ChangeCard(it) }
        }

        val helpers = snapshot.items.filter { it.sectionKey == "helpers" }
        if (helpers.isNotEmpty()) {
            item { SectionTitle("TRỢ THỦ", helpers.size.toString()) }
            items(helpers, key = { it.identity }) { VillageItemCard(it) }
        }

        if (snapshot.extras.isNotEmpty()) {
            item { SectionTitle("DỮ LIỆU KHÁC", snapshot.extras.values.sum().toString()) }
            item {
                NeonPanel(modifier = Modifier.fillMaxWidth(), glow = CocViolet) {
                    Column(Modifier.padding(6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        snapshot.extras.forEach { (name, count) ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(name, color = Color.White, fontSize = 10.sp, modifier = Modifier.weight(1f))
                                Text(count.toString(), color = CocGold, fontWeight = FontWeight.Bold, fontSize = 10.sp)
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
    levelLabel: String,
    level: Int?,
    itemCount: Int,
    upgradeCount: Int,
    hallItem: VillageItem?,
    modifier: Modifier,
    glow: Color
) {
    NeonPanel(modifier = modifier, glow = glow) {
        Row(Modifier.padding(7.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = glow, fontSize = 8.sp, fontWeight = FontWeight.Black, maxLines = 1)
                Text("$levelLabel ${level ?: "—"}", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Black, maxLines = 1)
                Text("$itemCount mục • $upgradeCount nâng", color = CocMuted, fontSize = 7.sp, maxLines = 1)
            }
            hallItem?.let { CategoryImage(it, 32) }
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
        Text(title, color = CocCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp, modifier = Modifier.weight(1f))
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
    val levelText = when {
        item.gearUp -> item.level?.let { "CẤP $it • GEAR-UP" } ?: "GEAR-UP"
        item.level != null -> "CẤP ${item.level} → ${item.level + 1}"
        else -> "ĐANG NÂNG"
    }
    val totalSeconds = (item.timerSeconds ?: 1L).coerceAtLeast(1L)
    val remainingRatio = if (remaining > 0L) {
        (remaining.toFloat() / totalSeconds.toFloat()).coerceIn(0f, 1f)
    } else 0f
    val remainingPercent = (remainingRatio * 100f).toInt().coerceIn(0, 100)
    NeonPanel(
        modifier = Modifier.fillMaxWidth(),
        glow = if (remaining == 0L) CocGreen else if (item.gearUp) CocMagenta else CocCyan
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 9.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CategoryImage(item, 48)
            Spacer(Modifier.width(9.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        levelText,
                        color = if (remaining == 0L) CocGreen else Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                    if (item.count > 1) {
                        Text(
                            "×${item.count}",
                            color = CocGold,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier
                                .background(CocGold.copy(alpha = 0.13f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                }
                if (remaining > 0L) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(23.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .background(CocSurfaceAlt),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(remainingRatio)
                                .align(Alignment.CenterStart)
                                .background(
                                    Brush.horizontalGradient(
                                        if (item.gearUp) listOf(CocMagenta, CocViolet)
                                        else listOf(CocCyan, Color(0xFF168BFF))
                                    )
                                )
                        )
                        Text(
                            "${formatCountdown(remaining)}  •  CÒN ${remainingPercent}%",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            maxLines = 1
                        )
                    }
                } else {
                    Text(
                        "HOÀN THÀNH • CHỜ JSON XÁC NHẬN",
                        color = CocGreen,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black
                    )
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
    var selectedFilter by remember(snapshot.tag, tab) { mutableStateOf("all") }

    val resourceIds = setOf(
        1000002, 1000003, 1000004, 1000005, 1000023, 1000024,
        1000035, 1000036, 1000037, 1000038, 1000058
    )
    val defenseIds = setOf(
        1000008, 1000009, 1000010, 1000011, 1000012, 1000013, 1000019,
        1000021, 1000027, 1000028, 1000031, 1000032, 1000067, 1000077,
        1000079, 1000084, 1000085, 1000086, 1000089, 1000102,
        1000033, 1000041, 1000043, 1000044, 1000045, 1000048, 1000050,
        1000051, 1000052, 1000054, 1000055, 1000056, 1000057, 1000063,
        1000080, 1000081, 1000082
    )

    val baseItems = snapshot.items.filter { it.base == selectedBase && it.tab == tab }
    fun buildingCategory(item: VillageItem): String = when {
        item.sectionKey == "traps" || item.sectionKey == "traps2" -> "traps"
        item.dataId in resourceIds -> "resources"
        item.dataId in defenseIds -> "defense"
        else -> "other"
    }
    fun armyCategory(item: VillageItem): String = when (item.sectionKey) {
        "units", "units2", "siege_machines" -> "troops"
        "heroes", "heroes2" -> "heroes"
        "spells" -> "spells"
        "pets" -> "pets"
        "equipment" -> "equipment"
        else -> "other"
    }

    val filterChoices = if (tab == "buildings") {
        listOf(
            "all" to "Tất cả",
            "defense" to "Phòng thủ",
            "resources" to "Tài nguyên",
            "traps" to "Bẫy",
            "other" to "Khác"
        ).filter { (key, _) ->
            key == "all" || baseItems.any { buildingCategory(it) == key }
        }
    } else {
        listOf(
            "all" to "Tất cả",
            "troops" to "Quân",
            "heroes" to "Anh hùng",
            "spells" to "Phép",
            "pets" to "Thú cưng",
            "equipment" to "Trang bị",
            "other" to "Khác"
        ).filter { (key, _) ->
            key == "all" || baseItems.any { armyCategory(it) == key }
        }
    }

    val filtered = when {
        selectedFilter == "all" -> baseItems
        tab == "buildings" -> baseItems.filter { buildingCategory(it) == selectedFilter }
        else -> baseItems.filter { armyCategory(it) == selectedFilter }
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 68.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (tab == "buildings") "CÔNG TRÌNH" else "QUÂN ĐỘI",
                        color = CocGold,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.weight(1f)
                    )
                    Text("${filtered.sumOf { it.count }} mục", color = CocMuted, fontSize = 10.sp)
                }
                Text(
                    snapshot.villageName?.takeIf { it.isNotBlank() } ?: snapshot.tag,
                    color = Color.White,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                FilterChip(
                    selected = selectedBase == "home",
                    onClick = { selectedBase = "home"; selectedFilter = "all" },
                    label = { Text("Nhà chính", fontSize = 11.sp) }
                )
                FilterChip(
                    selected = selectedBase == "builder",
                    onClick = { selectedBase = "builder"; selectedFilter = "all" },
                    label = { Text("Làng đêm", fontSize = 11.sp) }
                )
            }
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                filterChoices.forEach { (key, label) ->
                    FilterChip(
                        selected = selectedFilter == key,
                        onClick = { selectedFilter = key },
                        label = { Text(label, fontSize = 10.sp) }
                    )
                }
            }
        }

        if (filtered.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                InfoCard("Chưa có dữ liệu", "JSON không có mục thuộc nhóm này.")
            }
        } else {
            val grouped = filtered.groupBy { it.sectionKey }
            grouped.forEach { (sectionKey, rows) ->
                item(key = "section_$sectionKey", span = { GridItemSpan(maxLineSpan) }) {
                    SectionTitle(rows.first().sectionLabel.uppercase(), rows.sumOf { it.count }.toString())
                }
                gridItems(
                    items = rows,
                    key = { it.identity + "_" + (it.timerSeconds ?: -1L) + "_" + it.count },
                    span = { item ->
                        if (item.isRunning) GridItemSpan(maxLineSpan) else GridItemSpan(1)
                    }
                ) { item ->
                    if (item.isRunning) {
                        UpgradeCard(item, snapshot.timestamp)
                    } else {
                        VillageGridItemCard(item)
                    }
                }
            }
        }
    }
}

@Composable
private fun VillageGridItemCard(item: VillageItem) {
    NeonPanel(
        modifier = Modifier.fillMaxWidth(),
        glow = if (item.tab == "army") CocMagenta else CocCyan,
        innerPadding = 3.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            CategoryImage(item, 44)
            Text(
                item.level?.let { "CẤP $it" } ?: "—",
                color = CocGold,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                if (item.count > 1) {
                    Text("×${item.count}", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                if (item.gearUp) Text("GEAR-UP", color = CocGreen, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                item.weaponLevel?.let {
                    Text("V$it", color = CocCyan, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun VillageItemCard(item: VillageItem) {
    NeonPanel(modifier = Modifier.fillMaxWidth(), glow = if (item.tab == "army") CocMagenta else CocCyan) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 7.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CategoryImage(item, 44)
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(item.level?.let { "CẤP $it" } ?: "—", color = CocGold, fontSize = 16.sp, fontWeight = FontWeight.Black, maxLines = 1)
                if (item.count > 1) Text("×${item.count}", color = CocMuted, fontSize = 9.sp)
            }
            if (item.gearUp) Text("⚙", color = CocGreen, fontSize = 18.sp)
            item.weaponLevel?.let { Text("V$it", color = CocCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
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
    deletedVillages: List<DeletedVillageRecord>,
    pinnedTags: List<String>,
    activeTag: String,
    onSelect: (VillageSnapshot) -> Unit,
    onTogglePin: (String) -> Unit,
    onDelete: (String) -> Unit,
    onRestore: (String) -> Unit
) {
    var selectedSection by remember { mutableStateOf("villages") }
    var pendingDelete by remember { mutableStateOf<VillageSnapshot?>(null) }
    val villages = history.distinctBy { it.tag }.sortedWith(
        compareBy<VillageSnapshot> { pinnedTags.indexOf(it.tag).let { index -> if (index < 0) Int.MAX_VALUE else index } }
            .thenByDescending { it.timestamp }
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text("KHO DỮ LIỆU JSON", color = CocCyan, fontSize = 13.sp, fontWeight = FontWeight.Black)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = selectedSection == "villages",
                        onClick = { selectedSection = "villages" },
                        modifier = Modifier.weight(1f),
                        label = {
                            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Text("ĐANG LƯU", fontSize = 10.sp, maxLines = 1)
                            }
                        }
                    )
                    FilterChip(
                        selected = selectedSection == "history",
                        onClick = { selectedSection = "history" },
                        modifier = Modifier.weight(1f),
                        label = {
                            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Text("LỊCH SỬ", fontSize = 10.sp, maxLines = 1)
                            }
                        }
                    )
                    FilterChip(
                        selected = selectedSection == "deleted",
                        onClick = { selectedSection = "deleted" },
                        modifier = Modifier.weight(1f),
                        label = {
                            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Text("ĐÃ XÓA", fontSize = 10.sp, maxLines = 1)
                            }
                        }
                    )
                }
            }
        }

        if (selectedSection == "villages") {
            if (villages.isEmpty()) {
                item { InfoCard("Chưa có làng nào", "Sao chép JSON trong game rồi dán vào ứng dụng để thêm làng.") }
            } else {
                items(villages, key = { "village_" + it.tag }) { snapshot ->
                    val pinned = snapshot.tag in pinnedTags
                    NeonPanel(
                        modifier = Modifier.fillMaxWidth().clickable { onSelect(snapshot) },
                        glow = if (pinned) CocGold else if (snapshot.tag == activeTag) CocCyan else CocViolet
                    ) {
                        Column(Modifier.padding(6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    snapshot.villageName ?: snapshot.tag,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    if (pinned) "★" else "☆",
                                    color = if (pinned) CocGold else CocMuted,
                                    fontSize = 19.sp,
                                    modifier = Modifier.clickable { onTogglePin(snapshot.tag) }.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                                Text(
                                    "×",
                                    color = CocRed,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.clickable { pendingDelete = snapshot }.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                            Text(
                                "${snapshot.tag}  •  TH ${snapshot.townHallLevel ?: "—"}  •  BH ${snapshot.builderHallLevel ?: "—"}  •  ${history.count { it.tag == snapshot.tag }} mốc  •  ${snapshot.upgrades.size + snapshot.gearUps.size} nâng",
                                color = if (snapshot.tag == activeTag) CocGreen else CocMuted,
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        } else if (selectedSection == "history") {
            item { Text("Tối đa 60 mốc nhập gần nhất trên thiết bị", color = CocMuted, fontSize = 9.sp) }
            if (history.isEmpty()) {
                item { InfoCard("Chưa có lịch sử nhập", "Các lần dán JSON sẽ được ghi nhận tại đây.") }
            } else {
                val chronologicalHistory = history.sortedByDescending { it.timestamp }
                items(chronologicalHistory, key = { it.tag + "_" + it.timestamp }) { snapshot ->
                    NeonPanel(
                        modifier = Modifier.fillMaxWidth().clickable { onSelect(snapshot) },
                        glow = if (snapshot.tag == activeTag) CocCyan else CocViolet
                    ) {
                        Column(Modifier.padding(6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(snapshot.villageName ?: snapshot.tag, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text("${snapshot.tag}  •  ${formatTimestamp(snapshot.timestamp)}  •  TH ${snapshot.townHallLevel ?: "—"}  •  BH ${snapshot.builderHallLevel ?: "—"}  •  ${snapshot.items.size} mục", color = CocMuted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        } else {
            if (deletedVillages.isEmpty()) {
                item { InfoCard("Chưa có dữ liệu bị xóa", "Khi xóa một làng, dữ liệu sẽ được lưu tại đây để khôi phục.") }
            } else {
                items(deletedVillages, key = { it.id }) { deleted ->
                    NeonPanel(modifier = Modifier.fillMaxWidth(), glow = CocRed) {
                        Column(Modifier.padding(9.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(deleted.villageName ?: deleted.tag, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("${deleted.tag} • ${deleted.snapshots.size} mốc • ${formatTimestamp(deleted.deletedAt)}", color = CocMuted, fontSize = 9.sp)
                            TextButton(onClick = { onRestore(deleted.id) }) {
                                Text("KHÔI PHỤC JSON", color = CocGreen, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    pendingDelete?.let { snapshot ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Xóa dữ liệu làng?") },
            text = {
                Text("Toàn bộ ${history.count { it.tag == snapshot.tag }} mốc JSON của ${snapshot.villageName ?: snapshot.tag} sẽ chuyển vào Lịch sử xóa. Bạn có thể khôi phục sau.")
            },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(snapshot.tag)
                    pendingDelete = null
                }) { Text("XÓA", color = CocRed) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("HỦY") }
            },
            containerColor = CocSurface,
            titleContentColor = Color.White,
            textContentColor = CocMuted
        )
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
