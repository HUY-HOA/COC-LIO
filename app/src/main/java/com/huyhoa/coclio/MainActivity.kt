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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.Icon
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.google.firebase.auth.FirebaseAuth
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.huyhoa.coclio.data.CocAssets
import com.huyhoa.coclio.data.CocAssetCache
import com.huyhoa.coclio.data.CocAssetPrefetchWorker
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Checkbox
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
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Surface
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalConfiguration
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.huyhoa.coclio.data.DeletedVillageRecord
import com.huyhoa.coclio.data.ReceivedSharedJsonRecord
import com.huyhoa.coclio.data.DeletedReceivedSharedJsonRecord
import com.huyhoa.coclio.data.ReceivedSharedJsonStore
import com.huyhoa.coclio.data.VillageItem
import com.huyhoa.coclio.data.VillageExtraItem
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
private val CocOrange = Color(0xFFFFA43A)
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
    val brightness: Float,
    val progress: State<Float>
)

private data class ImportSummary(
    val title: String,
    val messages: List<String>
)

val LocalLedTheme = staticCompositionLocalOf {
    LedThemeState(DefaultLedPalette, true, 3000, 0.9f, mutableStateOf(0f))
}

private fun paletteForLed(preset: String): List<Color> = when (preset) {
    "ice" -> listOf(Color(0xFFB9F8FF), Color(0xFF36F4FF), Color(0xFF168BFF), Color(0xFFB9F8FF))
    "violet" -> listOf(Color(0xFFFFA5F5), Color(0xFFB33CFF), Color(0xFF6548FF), Color(0xFFFF35B8))
    "fire" -> listOf(Color(0xFFFFE082), Color(0xFFFFA43A), Color(0xFFFF4A35), Color(0xFFFF35B8))
    "mint" -> listOf(Color(0xFFB7F34A), Color(0xFF35E6A2), Color(0xFF00CFA8), Color(0xFFB7F34A))
    else -> DefaultLedPalette
}

@Composable
private fun animatedLedTextBrush(): Brush {
    val led = LocalLedTheme.current
    val palette = led.palette.map { it.copy(alpha = led.brightness) }
    val phase = if (led.enabled && led.speedMillis > 0) {
        (led.progress.value * (20000f / led.speedMillis)).rem(1f)
    } else 0f
    val movingColors = List(32) { index ->
        val position = ((index / 32f + phase) % 1f) * (palette.size - 1)
        val startIndex = position.toInt().coerceIn(0, palette.size - 2)
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
    return Brush.linearGradient(movingColors)
}

@Composable
private fun LedFrameBox(
    modifier: Modifier = Modifier,
    content: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit
) {
    val led = LocalLedTheme.current
    val palette = led.palette.map { it.copy(alpha = led.brightness) }
    val phase = if (led.enabled && led.speedMillis > 0) {
        (led.progress.value * (20000f / led.speedMillis)).rem(1f)
    } else 0f
    val movingColors = List(48) { index ->
        val position = ((index / 48f + phase) % 1f) * (palette.size - 1)
        val startIndex = position.toInt().coerceIn(0, palette.size - 2)
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
    val brush = if (led.enabled && led.speedMillis > 0) {
        Brush.sweepGradient(movingColors)
    } else {
        Brush.sweepGradient(palette)
    }
    Box(
        modifier = modifier.drawWithCache {
            val stroke = 3.dp.toPx()
            val inset = stroke / 2f
            val path = Path().apply {
                addRoundRect(
                    RoundRect(
                        left = inset,
                        top = inset,
                        right = size.width - inset,
                        bottom = size.height - inset,
                        cornerRadius = CornerRadius(22.dp.toPx())
                    )
                )
            }
            onDrawBehind {
                drawPath(path, brush, style = Stroke(width = stroke))
            }
        },
        contentAlignment = Alignment.Center,
        content = content
    )
}

@Composable
fun Modifier.ledRgbBorder(
    cornerRadiusDp: Float = 18f,
    strokeWidthDp: Float = 2.6f
): Modifier {
    val led = LocalLedTheme.current
    return this.drawWithCache {
        val palette = led.palette.map { it.copy(alpha = led.brightness.coerceIn(0f, 1f)) }
        val active = led.enabled && palette.size >= 2 && strokeWidthDp > 0f
        val phase = if (active && led.speedMillis > 0) {
            (led.progress.value * (20000f / led.speedMillis)).rem(1f)
        } else {
            0f
        }
        val intervals = (palette.size - 1).coerceAtLeast(1)
        val colors = if (active && led.speedMillis > 0) {
            List(32) { index ->
                val position = ((index / 32f + phase) % 1f) * intervals
                val startIndex = position.toInt().coerceIn(0, intervals - 1)
                val fraction = position - startIndex
                val first = palette[startIndex]
                val second = palette[startIndex + 1]
                Color(
                    red = first.red + (second.red - first.red) * fraction,
                    green = first.green + (second.green - first.green) * fraction,
                    blue = first.blue + (second.blue - first.blue) * fraction,
                    alpha = led.brightness.coerceIn(0f, 1f)
                )
            }
        } else {
            palette
        }
        val brush = Brush.sweepGradient(colors)
        val stroke = strokeWidthDp.dp.toPx()
        val inset = stroke / 2f
        val path = Path().apply {
            addRoundRect(
                RoundRect(
                    left = inset,
                    top = inset,
                    right = size.width - inset,
                    bottom = size.height - inset,
                    cornerRadius = CornerRadius(cornerRadiusDp.dp.toPx())
                )
            )
        }
        onDrawWithContent {
            drawContent()
            if (active) drawPath(path, brush, style = Stroke(width = stroke))
        }
    }
}

@Composable
private fun NeonBackdrop() {
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(MaterialTheme.colorScheme.background, MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.background)
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
    Column(
        modifier = modifier
            .shadow(5.dp, shape)
            .clip(shape)
            .background(Brush.linearGradient(listOf(
                MaterialTheme.colorScheme.surface,
                MaterialTheme.colorScheme.surfaceVariant,
                MaterialTheme.colorScheme.surface
            )))
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
                    val brush = if (led.enabled && led.speedMillis > 0) {
                        val ledProgress = (led.progress.value * (20000f / led.speedMillis)).rem(1f)
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
                        Brush.sweepGradient(movingColors)
                    } else if (led.enabled) {
                        Brush.sweepGradient(palette)
                    } else {
                        SolidColor(glow.copy(alpha = led.brightness.coerceIn(0f, 1f)))
                    }
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
    val buttonFillPalette = led.palette.map { it.copy(alpha = 1f) }
    Box(
        modifier = modifier
            .shadow(5.dp, shape)
            .clip(shape)
            .background(Brush.horizontalGradient(buttonFillPalette.take(3)))
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
    override fun onResume() {
        super.onResume()
        AccountActivityTracker.start(this)
    }

    override fun onPause() {
        AccountActivityTracker.stop(this)
        super.onPause()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var themeMode by remember { mutableStateOf(AppPreferences.themeMode(this@MainActivity)) }
            val dark = when (themeMode) {
                "light" -> false
                "dark" -> true
                else -> isSystemInDarkTheme()
            }
            val colors = if (dark) {
                darkColorScheme(
                    primary = CocCyan,
                    onPrimary = Color(0xFF04101D),
                    secondary = CocMagenta,
                    tertiary = CocGold,
                    background = CocBackground,
                    surface = CocSurface,
                    surfaceVariant = CocSurfaceAlt,
                    onBackground = Color.White,
                    onSurface = Color.White,
                    onSurfaceVariant = CocMuted
                )
            } else {
                lightColorScheme(
                    primary = Color(0xFF006A78),
                    onPrimary = Color.White,
                    secondary = Color(0xFF8B3B85),
                    tertiary = Color(0xFF8A5A00),
                    background = Color(0xFFF3F6FB),
                    surface = Color.White,
                    surfaceVariant = Color(0xFFE8EEF6),
                    onBackground = Color(0xFF172235),
                    onSurface = Color(0xFF172235),
                    onSurfaceVariant = Color(0xFF526176)
                )
            }
            MaterialTheme(colorScheme = colors) {
                CocLioApp(
                    themeMode = themeMode,
                    onThemeModeChanged = {
                        themeMode = it
                        AppPreferences.setThemeMode(this@MainActivity, it)
                    }
                )
            }
        }
    }
}

@Composable
private fun CocLioApp(
    themeMode: String,
    onThemeModeChanged: (String) -> Unit
) {
    val context = LocalContext.current
    val baseDensity = LocalDensity.current
    var appFontScale by remember { mutableStateOf(AppPreferences.textScale(context)) }
    var displaySizeLevel by remember { mutableStateOf(AppPreferences.displaySizeLevel(context)) }
    val appDisplayScale = AppPreferences.displaySizeScale(displaySizeLevel)
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val initialFirebaseGoogleEmail = remember {
        FirebaseAuth.getInstance().currentUser
            ?.takeIf { user -> user.providerData.any { it.providerId == "google.com" } }
            ?.email
    }
    val savedActiveProfile = remember { AccountProfileStore.active(context) }
    var activeProfile by remember {
        mutableStateOf(
            savedActiveProfile.takeIf { saved ->
                saved != "local" &&
                    initialFirebaseGoogleEmail?.equals(saved, ignoreCase = true) == true
            } ?: "local"
        )
    }
    var firebaseGoogleEmail by remember { mutableStateOf(initialFirebaseGoogleEmail) }
    var initialAuthStateHandled by remember { mutableStateOf(false) }
    var interactiveGoogleSignIn by remember { mutableStateOf(false) }
    var requiredAccountConsentLoadedForEmail by remember { mutableStateOf<String?>(null) }
    var requiredAccountConsentAccepted by remember { mutableStateOf(false) }
    var requiredAccountConsentChecked by remember { mutableStateOf(false) }
    var requiredAccountConsentSubmitting by remember { mutableStateOf(false) }
    var requiredAccountConsentReadError by remember { mutableStateOf<String?>(null) }
    var showRequiredPrivacyDetails by remember { mutableStateOf(false) }
    DisposableEffect(Unit) {
        val auth = FirebaseAuth.getInstance()
        val listener = FirebaseAuth.AuthStateListener { state ->
            val googleUser = state.currentUser
                ?.takeIf { user -> user.providerData.any { it.providerId == "google.com" } }
            val googleEmail = googleUser?.email?.takeIf { it.isNotBlank() }
            firebaseGoogleEmail = googleEmail

            if (!initialAuthStateHandled) {
                initialAuthStateHandled = true
                if (!interactiveGoogleSignIn && googleEmail != null) {
                    val savedProfile = AccountProfileStore.active(context)
                    if (savedProfile != "local" &&
                        savedProfile.equals(googleEmail, ignoreCase = true)
                    ) {
                        activeProfile = savedProfile
                    } else {
                        activeProfile = "local"
                    }
                } else if (!interactiveGoogleSignIn) {
                    activeProfile = "local"
                }
            } else if (googleEmail == null && !interactiveGoogleSignIn) {
                activeProfile = "local"
            }
        }
        auth.addAuthStateListener(listener)
        onDispose { auth.removeAuthStateListener(listener) }
    }
    val isGoogleSignedIn = activeProfile != "local" &&
        firebaseGoogleEmail?.equals(activeProfile, ignoreCase = true) == true &&
        requiredAccountConsentLoadedForEmail?.equals(firebaseGoogleEmail, ignoreCase = true) == true &&
        requiredAccountConsentAccepted
    val initialHistory = remember(activeProfile) { VillageStore.loadAll(context, activeProfile) }

    var history by remember { mutableStateOf(initialHistory) }
    val assetUrls = remember(activeProfile, history) {
        history.asSequence()
            .flatMap { it.items.asSequence() }
            .mapNotNull(CocAssets::imageUrl)
            .distinct()
            .toList()
    }
    var current by remember { mutableStateOf(initialHistory.firstOrNull()) }
    var selectedTab by remember { mutableStateOf(0) }
    var communityInitialSection by remember { mutableStateOf(0) }
    var showImportDialog by remember { mutableStateOf(false) }
    var importError by remember { mutableStateOf<String?>(null) }
    var googleAuthError by remember { mutableStateOf<String?>(null) }
    var securityTelemetryConsent by remember { mutableStateOf(false) }
    var analyticsConsent by remember { mutableStateOf(false) }
    var diagnosticsConsent by remember { mutableStateOf(false) }
    var villageDataSyncConsent by remember { mutableStateOf(false) }
    var privacyChoiceVersion by remember { mutableLongStateOf(0L) }
    var showPrivacyConsentDialog by remember { mutableStateOf(false) }
    var privacyDialogSecurity by remember { mutableStateOf(false) }
    var privacyDialogAnalytics by remember { mutableStateOf(false) }
    var privacyDialogDiagnostics by remember { mutableStateOf(false) }
    var privacyDialogVillageData by remember { mutableStateOf(false) }
    var privacySaving by remember { mutableStateOf(false) }
    var importSummary by remember { mutableStateOf<ImportSummary?>(null) }
    var lastUsedAt by remember(activeProfile) {
        mutableLongStateOf(AccountProfileStore.lastUsedAt(context, activeProfile) ?: 0L)
    }
    var notificationsEnabled by remember(activeProfile) {
        mutableStateOf(AppPreferences.notificationsEnabled(context, activeProfile))
    }
    var pendingPushPermission by remember { mutableStateOf<String?>(null) }
    var notificationStyle by remember(activeProfile) {
        mutableStateOf(AppPreferences.notificationStyle(context, activeProfile))
    }
    var alarmDurationSeconds by remember(activeProfile) {
        mutableStateOf(AppPreferences.alarmDurationSeconds(context, activeProfile))
    }
    var communityNotificationsEnabled by remember(activeProfile) {
        mutableStateOf(AppPreferences.communityNotificationsEnabled(context, activeProfile))
    }
    var updateNotificationsEnabled by remember(activeProfile) {
        mutableStateOf(AppPreferences.updateNotificationsEnabled(context, activeProfile))
    }
    var deletedVillages by remember(activeProfile) {
        mutableStateOf(VillageStore.loadDeleted(context, activeProfile))
    }
    var pinnedTags by remember(activeProfile) {
        mutableStateOf(VillageStore.pinnedTags(context, activeProfile))
    }
    var receivedSharedJsons by remember(activeProfile) {
        mutableStateOf(ReceivedSharedJsonStore.loadReceived(context, activeProfile))
    }
    var deletedReceivedSharedJsons by remember(activeProfile) {
        mutableStateOf(ReceivedSharedJsonStore.loadDeleted(context, activeProfile))
    }
    fun openPrivacyDialog() {
        privacyDialogSecurity = securityTelemetryConsent
        privacyDialogAnalytics = analyticsConsent
        privacyDialogDiagnostics = diagnosticsConsent
        privacyDialogVillageData = villageDataSyncConsent
        showPrivacyConsentDialog = true
    }

    val ledPrefs = remember {
        context.getSharedPreferences("coc_lio_led", android.content.Context.MODE_PRIVATE).also { prefs ->
            if (prefs.getInt("settings_version", 0) < 1) {
                prefs.edit()
                    .putInt("speed", 50)
                    .putInt("settings_version", 1)
                    .apply()
            }
        }
    }
    var ledEnabled by remember { mutableStateOf(ledPrefs.getBoolean("enabled", true)) }
    var ledPreset by remember { mutableStateOf(ledPrefs.getString("preset", "rainbow") ?: "rainbow") }
    val savedLedSpeed = ledPrefs.getInt("speed", 50)
    var ledSpeed by remember {
        mutableStateOf(
            savedLedSpeed.coerceIn(0, 100)
        )
    }
    var ledBrightness by remember { mutableStateOf(ledPrefs.getFloat("brightness", 0.9f).coerceIn(0f, 1f)) }
    val ledProgress = if (ledEnabled && ledSpeed > 0) {
        val ledTransition = rememberInfiniteTransition(label = "global-led-rgb")
        ledTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(20000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "global-led-progress"
        )
    } else {
        remember { mutableStateOf(0f) }
    }
    val ledTheme = LedThemeState(
        palette = paletteForLed(ledPreset),
        enabled = ledEnabled,
        speedMillis = if (ledSpeed <= 0 || !ledEnabled) 0 else (
            40000.0 / ledSpeed.coerceIn(1, 100)
        ).toInt().coerceIn(400, 40000),
        brightness = ledBrightness.coerceIn(0f, 1f),
        progress = ledProgress
    )

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            AppPreferences.setNotificationsEnabled(context, activeProfile, true)
            notificationsEnabled = true
            UpgradeReminderScheduler.scheduleAll(context, history, activeProfile, true)
        } else {
            AppPreferences.setNotificationsEnabled(context, activeProfile, false)
            notificationsEnabled = false
            UpgradeReminderScheduler.scheduleAll(context, history, activeProfile, false)
            scope.launch { snackbar.showSnackbar("Bạn chưa cấp quyền thông báo cho COC-LIO.") }
        }
    }

    val pushNotificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val target = pendingPushPermission
        pendingPushPermission = null
        if (granted) {
            when (target) {
                "community" -> {
                    AppPreferences.setCommunityNotificationsEnabled(context, activeProfile, true)
                    communityNotificationsEnabled = true
                }
                "updates" -> {
                    AppPreferences.setUpdateNotificationsEnabled(context, activeProfile, true)
                    updateNotificationsEnabled = true
                }
            }
        } else {
            when (target) {
                "community" -> {
                    AppPreferences.setCommunityNotificationsEnabled(context, activeProfile, false)
                    communityNotificationsEnabled = false
                }
                "updates" -> {
                    AppPreferences.setUpdateNotificationsEnabled(context, activeProfile, false)
                    updateNotificationsEnabled = false
                }
            }
            scope.launch { snackbar.showSnackbar("Bạn chưa cấp quyền thông báo cho COC-LIO.") }
        }
    }

    val testNotificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val sent = UpgradeNotificationTester.show(context, activeProfile, notificationStyle, alarmDurationSeconds)
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
        receivedSharedJsons = ReceivedSharedJsonStore.loadReceived(context, activeProfile)
        deletedReceivedSharedJsons = ReceivedSharedJsonStore.loadDeleted(context, activeProfile)
        notificationsEnabled = AppPreferences.notificationsEnabled(context, activeProfile)
        notificationStyle = AppPreferences.notificationStyle(context, activeProfile)
        AccountProfileStore.markLastUsed(context, activeProfile)
        lastUsedAt = AccountProfileStore.lastUsedAt(context, activeProfile) ?: 0L
        UpgradeReminderScheduler.scheduleAll(context, history, activeProfile, notificationsEnabled)
    }

    LaunchedEffect(activeProfile, assetUrls) {
        if (assetUrls.isNotEmpty()) {
            CocAssetPrefetchWorker.enqueue(context, activeProfile, assetUrls.hashCode())
        }
    }

    LaunchedEffect(isGoogleSignedIn) {
        AppWidgetUpdater.ensureRefresh(context)
    }

    LaunchedEffect(firebaseGoogleEmail) {
        val email = firebaseGoogleEmail
        requiredAccountConsentAccepted = false
        requiredAccountConsentLoadedForEmail = null
        requiredAccountConsentReadError = null
        if (email.isNullOrBlank()) {
            requiredAccountConsentLoadedForEmail = ""
            requiredAccountConsentChecked = false
        } else if (!requiredAccountConsentSubmitting) {
            runCatching { AccountPrivacyConsent.hasRequiredAccountDataConsent() }
                .onSuccess { accepted ->
                    requiredAccountConsentAccepted = accepted
                    requiredAccountConsentLoadedForEmail = email
                    if (accepted) requiredAccountConsentChecked = false
                }
                .onFailure { error ->
                    requiredAccountConsentAccepted = false
                    requiredAccountConsentLoadedForEmail = email
                    requiredAccountConsentChecked = false
                    requiredAccountConsentReadError = error.message
                        ?.takeIf { it.isNotBlank() }
                        ?: "Không thể kiểm tra xác nhận dữ liệu tài khoản."
                }
        }
    }

    LaunchedEffect(isGoogleSignedIn, activeProfile) {
        if (!isGoogleSignedIn) {
            securityTelemetryConsent = false
            analyticsConsent = false
            diagnosticsConsent = false
            villageDataSyncConsent = false
            privacyChoiceVersion = 0L
            AccountPrivacyConsent.applyCollectionSettings(context, AccountPrivacyChoice())
        } else {
            runCatching { AccountPrivacyConsent.load() }
                .onSuccess { choice ->
                    securityTelemetryConsent = choice.securityTelemetry
                    analyticsConsent = choice.analytics
                    diagnosticsConsent = choice.diagnostics
                    villageDataSyncConsent = choice.villageDataSync
                    privacyChoiceVersion = choice.version
                    AccountPrivacyConsent.applyCollectionSettings(context, choice)
                    if (choice.version < AccountPrivacyConsent.CURRENT_VERSION) {
                        AccountPrivacyConsent.applyCollectionSettings(context, AccountPrivacyChoice())
                        AccountActivityTracker.stopAndWait(context)
                        securityTelemetryConsent = false
                        analyticsConsent = false
                        diagnosticsConsent = false
                        villageDataSyncConsent = false
                    } else if (choice.securityTelemetry) {
                        AccountActivityTracker.start(context)
                    } else {
                        AccountActivityTracker.stopAndWait(context)
                    }
                }
                .onFailure {
                    securityTelemetryConsent = false
                    analyticsConsent = false
                    diagnosticsConsent = false
                    villageDataSyncConsent = false
                    privacyChoiceVersion = 0L
                    AccountPrivacyConsent.applyCollectionSettings(context, AccountPrivacyChoice())
                    AccountActivityTracker.stopAndWait(context)
                    snackbar.showSnackbar("Không tải được lựa chọn quyền riêng tư. Dữ liệu tùy chọn vẫn đang tắt.")
                }
        }
    }

    LaunchedEffect(isGoogleSignedIn) {
        if (!isGoogleSignedIn) {
            showImportDialog = false
            importError = null
        }
    }

    fun requestImportJson() {
        if (isGoogleSignedIn) {
            importError = null
            showImportDialog = true
        } else {
            showImportDialog = false
            selectedTab = 4
            scope.launch {
                snackbar.showSnackbar("Bạn cần đăng nhập Google trước khi nhập JSON và xem dữ liệu làng.")
            }
        }
    }

    fun importJson(raw: String, nameOverride: String? = null) {
        if (!isGoogleSignedIn) {
            requestImportJson()
            return
        }
        try {
            val parsedFromJson = VillageParser.parse(raw)
            val customName = nameOverride?.trim()?.takeIf { it.isNotBlank() }
            val parsed = if (customName == null) parsedFromJson
            else parsedFromJson.copy(villageName = customName)
            val latestPrevious = history
                .filter { it.tag == parsed.tag }
                .maxByOrNull { it.timestamp }
            val isOlderThanLatest = latestPrevious != null && parsed.timestamp < latestPrevious.timestamp
            val comparisonEvents = latestPrevious
                ?.takeIf { parsed.timestamp > it.timestamp }
                ?.let { VillageParser.compare(it, parsed) }
                .orEmpty()
            val summary = when {
                latestPrevious == null -> ImportSummary(
                    "Đã tạo mốc dữ liệu đầu tiên",
                    listOf(
                        "Đã lưu dữ liệu cho " + (parsed.villageName ?: parsed.tag) + ".",
                        "Từ lần xuất JSON tiếp theo, COC-LIO sẽ đối chiếu cấp độ và tiến trình nâng cấp."
                    )
                )
                isOlderThanLatest -> ImportSummary(
                    "Đã lưu JSON cũ vào kho",
                    listOf(
                        "JSON này có thời điểm cũ hơn dữ liệu mới nhất đã lưu cho " + parsed.tag + ".",
                        "Bản mới nhất vẫn được giữ làm dữ liệu đang theo dõi."
                    )
                )
                parsed.timestamp == latestPrevious?.timestamp -> ImportSummary(
                    "Đã cập nhật mốc JSON hiện có",
                    listOf("JSON có cùng timestamp với mốc đã lưu; ứng dụng đã thay nội dung tại mốc đó.")
                )
                comparisonEvents.isEmpty() -> ImportSummary(
                    "Đã phân tích và lưu JSON",
                    listOf(
                        "Không phát hiện thay đổi về nâng cấp hoặc cấp độ trong các mục mà bộ so sánh hiện hỗ trợ.",
                        "Dữ liệu mới đã được lưu làm mốc hiện tại."
                    )
                )
                else -> ImportSummary(
                    "Phát hiện " + comparisonEvents.size + " thay đổi",
                    comparisonEvents.map { it.title + ": " + it.detail }
                )
            }
            VillageStore.save(context, parsed, activeProfile, customName)
            if (isGoogleSignedIn && villageDataSyncConsent) {
                scope.launch {
                    try {
                        if (FirebaseVillageDataSync.saveLatest(parsed)) {
                            snackbar.showSnackbar("Đã đồng bộ bản JSON mới nhất của ${parsed.tag} lên tài khoản COC-LIO.")
                        }
                    } catch (error: Exception) {
                        snackbar.showSnackbar(
                            error.message?.takeIf { it.isNotBlank() }
                                ?: "JSON vẫn được lưu trên điện thoại nhưng chưa đồng bộ được lên Firestore."
                        )
                    }
                }
            }
            FirebaseTelemetry.logVillageImport(context, latestPrevious != null)
            history = VillageStore.loadAll(context, activeProfile)
            pinnedTags = VillageStore.pinnedTags(context, activeProfile)
            UpgradeReminderScheduler.scheduleAll(context, history, activeProfile, notificationsEnabled)
            AppWidgetUpdater.updateAll(context)
            current = history.firstOrNull { it.tag == parsed.tag } ?: parsed
            importError = null
            showImportDialog = false
            importSummary = summary
        } catch (error: Exception) {
            importError = error.message ?: "Không thể đọc JSON này."
        }
    }

    fun finishGoogleSignIn(googleAccount: GoogleAccountData) {
        scope.launch {
            try {
                val savedName = AccountProfileStore.preferredName(context, googleAccount.email)
                val profileName = (
                    savedName?.takeIf { it.isNotBlank() }
                        ?: googleAccount.displayName?.takeIf { it.isNotBlank() }
                        ?: googleAccount.email.substringBefore("@")
                    ).take(40)
                if (savedName.isNullOrBlank()) {
                    AccountProfileStore.setPreferredName(context, googleAccount.email, profileName)
                }
                GoogleAccountAuth.shareProfile(googleAccount)
                AccountPrivacyConsent.saveRequiredAccountDataConsent()
                FirebaseAccountAdministration.savePreferredName(profileName)
                AccountProfileStore.setActive(context, googleAccount.email)
                requiredAccountConsentAccepted = true
                requiredAccountConsentLoadedForEmail = googleAccount.email
                requiredAccountConsentChecked = false
                requiredAccountConsentSubmitting = false
                requiredAccountConsentReadError = null
                activeProfile = googleAccount.email
                interactiveGoogleSignIn = false
                selectedTab = 0
                googleAuthError = null
                snackbar.showSnackbar("Đã xác nhận dữ liệu cần thiết và đăng nhập Google thành công.")
            } catch (error: Exception) {
                requiredAccountConsentSubmitting = false
                interactiveGoogleSignIn = false
                snackbar.showSnackbar(
                    error.message?.takeIf { it.isNotBlank() }
                        ?: "Không thể lưu xác nhận dữ liệu tài khoản. Hãy thử lại."
                )
            }
        }
    }

    fun leaveGoogleAccount(message: String) {
        scope.launch {
            AccountActivityTracker.stopAndWait(context)
            FirebaseTelemetry.logSignOut(context)
            AccountPrivacyConsent.applyCollectionSettings(context, AccountPrivacyChoice())
            GoogleAccountAuth.signOut(context)
            AccountProfileStore.setActive(context, null)
            interactiveGoogleSignIn = false
            activeProfile = "local"
            selectedTab = 0
            snackbar.showSnackbar(message)
        }
    }

    fun savePrivacyChoice(choice: AccountPrivacyChoice) {
        if (privacySaving) return
        privacySaving = true
        scope.launch {
            try {
                AccountPrivacyConsent.save(choice)
                securityTelemetryConsent = choice.securityTelemetry
                analyticsConsent = choice.analytics
                diagnosticsConsent = choice.diagnostics
                villageDataSyncConsent = choice.villageDataSync
                privacyChoiceVersion = AccountPrivacyConsent.CURRENT_VERSION
                AccountPrivacyConsent.applyCollectionSettings(context, choice)
                if (choice.securityTelemetry) {
                    AccountActivityTracker.start(context)
                } else {
                    AccountActivityTracker.stopAndWait(context)
                }
                showPrivacyConsentDialog = false
                snackbar.showSnackbar("Đã lưu lựa chọn quyền riêng tư của bạn.")
            } catch (error: Exception) {
                snackbar.showSnackbar(
                    error.message?.takeIf { it.isNotBlank() }
                        ?: "Không thể lưu lựa chọn quyền riêng tư. Hãy thử lại."
                )
            } finally {
                privacySaving = false
            }
        }
    }

    fun signInWithGoogle() {
        if (!requiredAccountConsentChecked || requiredAccountConsentSubmitting) return
        requiredAccountConsentSubmitting = true
        scope.launch {
            interactiveGoogleSignIn = true
            try {
                AccountActivityTracker.stopAndWait(context)
                val googleAccount = GoogleAccountAuth.signIn(context)
                AccountProfileStore.saveGoogleData(context, googleAccount)
                finishGoogleSignIn(googleAccount)
                googleAuthError = null
            } catch (error: Exception) {
                interactiveGoogleSignIn = false
                requiredAccountConsentSubmitting = false
                if (error is GetCredentialCancellationException) {
                    googleAuthError = null
                    snackbar.showSnackbar("Bạn đã hủy đăng nhập Google.")
                } else {
                    googleAuthError = error.message
                        ?.takeIf { it.isNotBlank() }
                        ?: "Không thể đăng nhập Google. Hãy kiểm tra kết nối mạng và cấu hình OAuth."
                }
            }
        }
    }

    CompositionLocalProvider(
        LocalLedTheme provides ledTheme,
        LocalDensity provides Density(baseDensity.density * appDisplayScale, baseDensity.fontScale * appFontScale)
    ) {
    Box(Modifier.fillMaxSize()) {
        NeonBackdrop()
        Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            if (isGoogleSignedIn) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "COC-LIO",
                        style = TextStyle(brush = animatedLedTextBrush()),
                        fontSize = 23.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    )
                    Text(
                        "THEO DÕI TIẾN TRÌNH NÂNG CẤP VÀ THÔNG BÁO",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 8.sp,
                        letterSpacing = 0.3.sp,
                        maxLines = 1
                    )
                }
                NeonButton(
                    text = if (isGoogleSignedIn) "＋ NHẬP JSON" else "ĐĂNG NHẬP",
                    onClick = { requestImportJson() }
                )
            }
            }
        },
        bottomBar = {
            if (isGoogleSignedIn) {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f),
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                val navItems = listOf(
                    Triple("home", "Tổng quan", 0),
                    Triple("buildings", "Công trình Quân đội", 1),
                    Triple("history", "Kho JSON", 2),
                    Triple("community", "Cộng đồng", 3),
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
                                    "history" -> Icons.Filled.History
                                    "community" -> Icons.Filled.Forum
                                    else -> Icons.Filled.Settings
                                },
                                contentDescription = item.second,
                                tint = if (selectedTab == item.third) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        label = {
                            Text(
                                item.second,
                                fontSize = 9.sp,
                                lineHeight = 10.sp,
                                maxLines = 2,
                                softWrap = true,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                overflow = TextOverflow.Clip,
                                color = if (selectedTab == item.third) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                }
            }
            }
        }
    ) { innerPadding ->
        if (!isGoogleSignedIn) {
            GoogleLoginStartScreen(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                consentChecked = requiredAccountConsentChecked,
                onConsentChecked = { requiredAccountConsentChecked = it },
                isBusy = requiredAccountConsentSubmitting || interactiveGoogleSignIn,
                hasAuthenticatedGoogleAccount = firebaseGoogleEmail != null,
                readError = requiredAccountConsentReadError,
                onOpenDetails = { showRequiredPrivacyDetails = true },
                onReject = { leaveGoogleAccount("Bạn đã từ chối xử lý dữ liệu tài khoản cần thiết và đã đăng xuất.") },
                onGoogleSignIn = { signInWithGoogle() }
            )
        } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Box(
                modifier = Modifier
                    .widthIn(max = 1100.dp)
                    .fillMaxSize()
            ) {
            Crossfade(targetState = selectedTab, label = "navigation-transition") { page ->
                when (page) {
                    0 -> current?.let {
                        OverviewScreen(snapshot = it)
                    } ?: EmptyWelcome(
                        onImport = { requestImportJson() },
                        onOpenGame = { openClashOfClans(context) }
                    )
                    1 -> current?.let {
                        BuildingsAndArmyScreen(snapshot = it)
                    } ?: EmptyWelcome(
                        onImport = { requestImportJson() },
                        onOpenGame = { openClashOfClans(context) }
                    )
                    2 -> HistoryScreen(
                        history = history,
                        deletedVillages = deletedVillages,
                        receivedSharedJsons = receivedSharedJsons,
                        deletedReceivedSharedJsons = deletedReceivedSharedJsons,
                        pinnedTags = pinnedTags,
                        activeTag = current?.tag.orEmpty(),
                        activeTimestamp = current?.timestamp,
                        onSelect = {
                            current = it
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
                                UpgradeReminderScheduler.scheduleAll(context, history, activeProfile, notificationsEnabled)
                                AppWidgetUpdater.updateAll(context)
                                scope.launch { snackbar.showSnackbar("Đã chuyển $tag vào mục ĐÃ GỠ.") }
                            }
                        },
                        onRestore = { recordId ->
                            val restoredTag = deletedVillages.firstOrNull { it.id == recordId }?.tag
                            if (VillageStore.restoreDeleted(context, recordId, activeProfile)) {
                                history = VillageStore.loadAll(context, activeProfile)
                                deletedVillages = VillageStore.loadDeleted(context, activeProfile)
                                pinnedTags = VillageStore.pinnedTags(context, activeProfile)
                                current = history.firstOrNull { it.tag == restoredTag } ?: history.firstOrNull()
                                UpgradeReminderScheduler.scheduleAll(context, history, activeProfile, notificationsEnabled)
                                AppWidgetUpdater.updateAll(context)
                                scope.launch { snackbar.showSnackbar("Đã khôi phục dữ liệu làng.") }
                            }
                        },
                        onDeletePermanently = { recordId ->
                            if (VillageStore.permanentlyDelete(context, recordId, activeProfile)) {
                                deletedVillages = VillageStore.loadDeleted(context, activeProfile)
                                scope.launch { snackbar.showSnackbar("Đã xóa vĩnh viễn dữ liệu JSON.") }
                            }
                        },
                        onShare = { snapshot ->
                            scope.launch {
                                runCatching { shareVillageJsonToCommunity(snapshot) }
                                    .onSuccess { snackbar.showSnackbar(it) }
                                    .onFailure { error ->
                                        snackbar.showSnackbar(error.message ?: "Không thể chia sẻ JSON.")
                                    }
                            }
                        },
                        onUseReceivedJson = { record -> importJson(record.rawJson) },
                        onDeleteReceivedJson = { jsonId ->
                            if (ReceivedSharedJsonStore.moveToDeleted(context, jsonId, activeProfile)) {
                                receivedSharedJsons = ReceivedSharedJsonStore.loadReceived(context, activeProfile)
                                deletedReceivedSharedJsons = ReceivedSharedJsonStore.loadDeleted(context, activeProfile)
                                scope.launch { snackbar.showSnackbar("Đã chuyển JSON nhận được vào mục ĐÃ GỠ. Bản trong lịch sử vẫn được giữ.") }
                            }
                        },
                        onRestoreReceivedJson = { jsonId ->
                            if (ReceivedSharedJsonStore.restore(context, jsonId, activeProfile)) {
                                receivedSharedJsons = ReceivedSharedJsonStore.loadReceived(context, activeProfile)
                                deletedReceivedSharedJsons = ReceivedSharedJsonStore.loadDeleted(context, activeProfile)
                                scope.launch { snackbar.showSnackbar("Đã khôi phục JSON đã nhận.") }
                            }
                        },
                        onDeleteReceivedJsonPermanently = { jsonId ->
                            if (ReceivedSharedJsonStore.permanentlyDelete(context, jsonId, activeProfile)) {
                                deletedReceivedSharedJsons = ReceivedSharedJsonStore.loadDeleted(context, activeProfile)
                                scope.launch { snackbar.showSnackbar("Đã xóa vĩnh viễn JSON nhận được khỏi thiết bị.") }
                            }
                        },
                        onOpenSharedJsonFeed = {
                            communityInitialSection = 3
                            selectedTab = 3
                        }
                    )
                    3 -> CommunityScreen(
                        isGoogleSignedIn = isGoogleSignedIn,
                        availableJsons = history,
                        initialSection = communityInitialSection,
                        onInitialSectionConsumed = { communityInitialSection = 0 },
                        onImportJson = { rawJson, senderUid, senderName, tag, villageName, timestamp, jsonId ->
                            val parsed = runCatching { VillageParser.parse(rawJson) }.getOrNull()
                            if (parsed != null) {
                                ReceivedSharedJsonStore.saveReceived(
                                    context = context,
                                    receiverUid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty(),
                                    senderUid = senderUid,
                                    senderName = senderName,
                                    tag = tag.ifBlank { parsed.tag },
                                    villageName = villageName.ifBlank { parsed.villageName.orEmpty() },
                                    timestamp = timestamp.takeIf { it > 0L } ?: parsed.timestamp,
                                    jsonId = jsonId,
                                    rawJson = rawJson,
                                    profile = activeProfile
                                )
                            }
                            importJson(rawJson)
                            receivedSharedJsons = ReceivedSharedJsonStore.loadReceived(context, activeProfile)
                        },
                        onOpenSettings = { selectedTab = 4 }
                    )
                    4 -> SettingsScreen(
                        accountEmail = activeProfile.takeUnless { it == "local" },
                        isAdmin = FirebaseAccountAdministration.isOwner(activeProfile),
                        accountData = AccountProfileStore.googleData(context, activeProfile),
                        preferredName = AccountProfileStore.preferredName(context, activeProfile),
                        lastUsedLabel = lastUsedAt.takeIf { it > 0L }?.let { formatTimestamp(it / 1000L) },
                        securityTelemetryConsent = securityTelemetryConsent,
                        analyticsConsent = analyticsConsent,
                        diagnosticsConsent = diagnosticsConsent,
                        privacyChoiceConfigured = privacyChoiceVersion >= AccountPrivacyConsent.CURRENT_VERSION,
                        onManagePrivacy = { openPrivacyDialog() },
                        knownProfiles = AccountProfileStore.knownProfiles(context, activeProfile),
                        themeMode = themeMode,
                        onThemeModeChanged = onThemeModeChanged,
                        textScale = appFontScale,
                        displaySizeLevel = displaySizeLevel,
                        onDisplaySizeLevelChanged = { level ->
                            displaySizeLevel = level.coerceIn(1, 5)
                            AppPreferences.setDisplaySizeLevel(context, displaySizeLevel)
                            AppWidgetUpdater.updateAll(context)
                        },
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
                            ledSpeed = value.coerceIn(0, 100)
                            ledPrefs.edit().putInt("speed", ledSpeed).apply()
                        },
                        onLedBrightnessChanged = { value ->
                            ledBrightness = value.coerceIn(0f, 1f)
                            ledPrefs.edit().putFloat("brightness", ledBrightness).apply()
                        },
                        notificationStyle = notificationStyle,
                        alarmDurationSeconds = alarmDurationSeconds,
                        communityNotificationsEnabled = communityNotificationsEnabled,
                        updateNotificationsEnabled = updateNotificationsEnabled,
                        onCommunityNotificationsChanged = { enabled ->
                            if (enabled && Build.VERSION.SDK_INT >= 33 &&
                                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                            ) {
                                pendingPushPermission = "community"
                                pushNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                AppPreferences.setCommunityNotificationsEnabled(context, activeProfile, enabled)
                                communityNotificationsEnabled = enabled
                            }
                        },
                        onUpdateNotificationsChanged = { enabled ->
                            if (enabled && Build.VERSION.SDK_INT >= 33 &&
                                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                            ) {
                                pendingPushPermission = "updates"
                                pushNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                AppPreferences.setUpdateNotificationsEnabled(context, activeProfile, enabled)
                                updateNotificationsEnabled = enabled
                            }
                        },
                        onGoogleSignIn = { signInWithGoogle() },
                        onGoogleSwitchAccount = {
                            leaveGoogleAccount("Đã đăng xuất tài khoản hiện tại. Hãy chọn tài khoản Google khác trên màn hình đăng nhập.")
                        },
                        onGoogleSignOut = {
                            leaveGoogleAccount("Đã đăng xuất Google. Dữ liệu cục bộ vẫn được giữ trên thiết bị.")
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
                        onAlarmDurationChanged = { seconds ->
                            AppPreferences.setAlarmDurationSeconds(context, activeProfile, seconds)
                            alarmDurationSeconds = AppPreferences.alarmDurationSeconds(context, activeProfile)
                        },
                        onTestNotification = {
                            if (Build.VERSION.SDK_INT >= 33 &&
                                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                            ) {
                                testNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                val sent = UpgradeNotificationTester.show(context, activeProfile, notificationStyle, alarmDurationSeconds)
                                scope.launch {
                                    snackbar.showSnackbar(
                                        if (sent) "Đã gửi thông báo thử. Hãy kéo thanh thông báo xuống để xem."
                                        else "Không thể gửi thông báo thử. Hãy kiểm tra quyền thông báo của ứng dụng."
                                    )
                                }
                            }
                        },
                        onTextScaleChanged = { value ->
                            val selectedScale = AppPreferences.textScaleForLevel(AppPreferences.textScaleLevel(value))
                            appFontScale = selectedScale
                            AppPreferences.setTextScale(context, selectedScale)
                            AppWidgetUpdater.updateAll(context)
                        }
                    )
                    }

            }
            }
        }
        }
    }

    importSummary?.let { summary ->
        AlertDialog(
            onDismissRequest = { importSummary = null },
            title = { Text(summary.title) },
            text = {
                Column(
                    modifier = Modifier
                        .heightIn(max = 320.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    summary.messages.forEach { message ->
                        Text(
                            "• " + message,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            lineHeight = 17.sp
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { importSummary = null }) { Text("ĐÓNG") }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurface
        )
    }

    googleAuthError?.let { details ->
        AlertDialog(
            onDismissRequest = { googleAuthError = null },
            title = { Text("Không thể đăng nhập Google") },
            text = {
                Column(
                    modifier = Modifier
                        .heightIn(max = 320.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Chi tiết bên dưới giúp xác định chính xác cấu hình OAuth nào chưa khớp. Bạn có thể sao chép để gửi kiểm tra.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                    Text(
                        details,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 10.sp,
                        lineHeight = 14.sp
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        clipboard.setText(androidx.compose.ui.text.AnnotatedString(details))
                        googleAuthError = null
                    }
                ) { Text("SAO CHÉP CHẨN ĐOÁN") }
            },
            dismissButton = {
                TextButton(onClick = { googleAuthError = null }) { Text("ĐÓNG") }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurface
        )
    }

    if (showRequiredPrivacyDetails) {
        RequiredAccountDataDetailsDialog(
            onDismiss = { showRequiredPrivacyDetails = false }
        )
    }

    if (showPrivacyConsentDialog && isGoogleSignedIn) {
        AlertDialog(
            onDismissRequest = { showPrivacyConsentDialog = false },
            title = {
                Text("QUYỀN RIÊNG TƯ & DỮ LIỆU COC-LIO", fontWeight = FontWeight.Black)
            },
            text = {
                Column(
                    modifier = Modifier
                        .heightIn(max = 440.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "THÔNG TIN TÀI KHOẢN CẦN THIẾT",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                    Text(
                        "Khi bạn đăng nhập Google và dùng tính năng tài khoản, COC-LIO lưu Firebase UID, email, tên và ảnh hồ sơ Google, trạng thái xác minh email, tên hiển thị bạn đặt, nhà cung cấp đăng nhập, số lần và thời điểm đăng nhập. Mục đích là xác thực, duy trì tài khoản, đồng bộ hồ sơ và hiển thị danh tính trong cộng đồng. Hồ sơ được lưu trong Firebase Authentication và Cloud Firestore.",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 10.sp,
                        lineHeight = 15.sp
                    )
                    Text(
                        "CÁC MỤC THU THẬP BỔ SUNG — TỰ CHỌN",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                    Text(
                        "Các mục dưới đây mặc định tắt. Bạn vẫn có thể dùng ứng dụng và các chức năng chính khi không bật.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = privacyDialogSecurity,
                            onCheckedChange = { privacyDialogSecurity = it },
                            enabled = !privacySaving
                        )
                        Column(Modifier.weight(1f)) {
                            Text("Bảo mật tài khoản và hoạt động", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                            Text(
                                "Cho phép ghi nhận hãng, mẫu, dòng thiết bị và kiến trúc CPU; phiên bản Android/API và COC-LIO; kích thước/độ phân giải màn hình; ngôn ngữ, múi giờ, loại kết nối Wi-Fi/di động/khác; thời điểm mở, hoạt động, chuyển nền; số phiên và thời lượng sử dụng. Không ghi IP hoặc mã định danh phần cứng. Mục đích: hỗ trợ tương thích, bảo mật tài khoản và kỹ thuật.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 9.sp,
                                lineHeight = 13.sp
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = privacyDialogVillageData,
                            onCheckedChange = { privacyDialogVillageData = it },
                            enabled = !privacySaving
                        )
                        Column(Modifier.weight(1f)) {
                            Text("Đồng bộ dữ liệu Clash of Clans lên tài khoản", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                            Text(
                                "Khi bạn tự nhập JSON từ game, COC-LIO có thể gửi bản JSON đó lên Firestore để chủ tài khoản và chủ sở hữu ứng dụng xem dữ liệu làng. Chỉ lưu bản mới nhất cho mỗi Player Tag; giới hạn 700 KB/bản. Không tự đăng nhập game, không tự đọc dữ liệu game khi bạn chưa nhập JSON. Tắt mục này sẽ dừng đồng bộ mới; dữ liệu đã lưu không tự xóa.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 9.sp,
                                lineHeight = 13.sp
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = privacyDialogAnalytics,
                            onCheckedChange = { privacyDialogAnalytics = it },
                            enabled = !privacySaving
                        )
                        Column(Modifier.weight(1f)) {
                            Text("Phân tích mức sử dụng", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                            Text(
                                "Cho phép Firebase Analytics ghi nhận sự kiện như đăng nhập, đăng xuất, nhập JSON và một số thông tin sử dụng/thiết bị tự động của SDK. Mục đích: hiểu tính năng được dùng ra sao để cải thiện ứng dụng.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 9.sp,
                                lineHeight = 13.sp
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = privacyDialogDiagnostics,
                            onCheckedChange = { privacyDialogDiagnostics = it },
                            enabled = !privacySaving
                        )
                        Column(Modifier.weight(1f)) {
                            Text("Báo cáo lỗi và hiệu năng", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                            Text(
                                "Cho phép Firebase Crashlytics gửi thông tin sự cố, dấu vết lỗi và trạng thái thiết bị/ứng dụng; Firebase Performance ghi nhận số liệu hiệu năng và thời gian mạng. Mục đích: tìm lỗi, cải thiện độ ổn định và tốc độ.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 9.sp,
                                lineHeight = 13.sp
                            )
                        }
                    }
                    Text(
                        "DỮ LIỆU LƯU TRÊN THIẾT BỊ",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                    Text(
                        "Lịch sử làng, JSON đã nhập, tên làng, mục ghim, lịch sử đã xóa, JSON nhận từ cộng đồng và các tùy chỉnh giao diện/thông báo/nhắc nhở có thể được lưu trong bộ nhớ riêng của ứng dụng trên điện thoại để bạn xem lại. JSON chỉ được đồng bộ lên Firestore nếu bạn bật riêng lựa chọn đồng bộ dữ liệu Clash of Clans; nếu tắt, JSON chỉ lưu trên thiết bị. Chia sẻ JSON vào cộng đồng là thao tác riêng và nội dung sẽ hiển thị cho thành viên theo quy tắc cộng đồng. Dữ liệu cục bộ không tự mất khi xóa hồ sơ Firebase; bạn cần xóa dữ liệu ứng dụng hoặc gỡ ứng dụng để xóa bản lưu trên máy.",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 9.sp,
                        lineHeight = 13.sp
                    )
                    Text(
                        "KHI BẠN TỰ GỬI NỘI DUNG VÀ DÙNG THÔNG BÁO",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                    Text(
                        "Tin nhắn chữ, hồ sơ và JSON bạn chủ động chia sẻ được lưu trong Cloud Firestore; thành viên đã đăng nhập có thể xem nội dung cộng đồng. Trên gói Firebase Spark, gửi ảnh, tệp và APK qua Cloud Storage đang tạm dừng; tệp cũ trong Storage vẫn còn nhưng không thể đọc/ghi khi chưa liên kết lại gói Blaze. Thông báo đẩy tự động cho tin nhắn và cập nhật cũng tạm dừng vì cần Cloud Functions. Nếu bật tùy chọn thông báo, ứng dụng có thể quản lý đăng ký FCM nhưng hiện không có chức năng máy chủ gửi các thông báo này.",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 9.sp,
                        lineHeight = 13.sp
                    )
                    Text(
                        "AI CÓ THỂ XEM DỮ LIỆU",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                    Text(
                        "Trong giao diện COC-LIO, chỉ tài khoản chủ sở hữu COC-LIO mới được xem hồ sơ và dữ liệu hoạt động trong danh sách quản lý. Bản JSON làng đồng bộ theo lựa chọn chỉ người dùng sở hữu và chủ sở hữu ứng dụng được đọc qua quy tắc Firestore; người có quyền quản trị dự án Firebase vẫn có thể có quyền truy cập kỹ thuật theo phân quyền dự án. Nội dung bạn gửi vào cộng đồng được hiển thị cho các thành viên đã đăng nhập. Analytics, Crashlytics và Performance được xử lý trong bảng điều khiển Firebase tương ứng.",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 9.sp,
                        lineHeight = 13.sp
                    )
                    Text(
                        "THỜI GIAN LƯU TRỮ VÀ XÓA DỮ LIỆU",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                    Text(
                        "Hồ sơ và dữ liệu hoạt động được ghi trong Firestore được giữ cho đến khi được quản lý xóa; hiện chưa đặt thời hạn tự động xóa cố định. Bài đăng cộng đồng được giữ cho đến khi bị gỡ. Trên Spark, việc dọn kho chỉ xóa bản ghi Firestore; tệp đính kèm cũ trong Cloud Storage không thể truy cập hoặc xóa từ ứng dụng. Dữ liệu Analytics, Crashlytics và Performance đã gửi trước đó tuân theo thời hạn lưu trữ của từng dịch vụ. Chức năng Vô hiệu hóa tài khoản trong COC-LIO chỉ đánh dấu hồ sơ Firestore và chặn quyền truy cập COC-LIO; chức năng này không xóa tài khoản Firebase Authentication hoặc tự xóa toàn bộ dữ liệu liên quan. Dữ liệu lưu cục bộ trên điện thoại không bị xóa.",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 9.sp,
                        lineHeight = 13.sp
                    )
                    Text(
                        "Bạn có thể đổi hoặc rút lại từng lựa chọn thu thập tùy chọn trong Cài đặt. Khi rút lại, việc thu thập mới cho mục đó sẽ dừng; dữ liệu đã ghi trước đó không tự động bị xóa, trừ khi bạn gửi yêu cầu xóa và yêu cầu được xử lý. Bạn có thể yêu cầu xem, sửa, hạn chế xử lý hoặc xóa dữ liệu qua email phunghuyhoahaikba@gmail.com. Dịch vụ xử lý/lưu trữ kỹ thuật: Google Firebase. Đầu mối vận hành và tiếp nhận yêu cầu: COC-LIO. Việc rút lại không làm mất hiệu lực xử lý đã thực hiện hợp pháp trước thời điểm rút lại.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 9.sp,
                        lineHeight = 13.sp
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !privacySaving,
                    onClick = {
                        savePrivacyChoice(
                            AccountPrivacyChoice(
                                securityTelemetry = privacyDialogSecurity,
                                analytics = privacyDialogAnalytics,
                                diagnostics = privacyDialogDiagnostics,
                                villageDataSync = privacyDialogVillageData
                            )
                        )
                    }
                ) {
                    Text(if (privacySaving) "ĐANG LƯU..." else "LƯU LỰA CHỌN")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    TextButton(
                        enabled = !privacySaving,
                        onClick = {
                            privacyDialogSecurity = false
                            privacyDialogAnalytics = false
                            privacyDialogDiagnostics = false
                            savePrivacyChoice(AccountPrivacyChoice())
                        }
                    ) {
                        Text("TỪ CHỐI TẤT CẢ", color = MaterialTheme.colorScheme.error)
                    }
                    TextButton(
                        enabled = !privacySaving,
                        onClick = { showPrivacyConsentDialog = false }
                    ) {
                        Text("ĐỂ SAU")
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurface
        )
    }

    if (showImportDialog && isGoogleSignedIn) {
        CompositionLocalProvider(
            LocalDensity provides Density(baseDensity.density * appDisplayScale, baseDensity.fontScale * appFontScale)
        ) {
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
}

}

@Composable
private fun GoogleLoginStartScreen(
    modifier: Modifier = Modifier,
    consentChecked: Boolean,
    onConsentChecked: (Boolean) -> Unit,
    isBusy: Boolean,
    hasAuthenticatedGoogleAccount: Boolean,
    readError: String?,
    onOpenDetails: () -> Unit,
    onReject: () -> Unit,
    onGoogleSignIn: () -> Unit
) {
    LedFrameBox(
        modifier = modifier.padding(10.dp)
    ) {
        NeonPanel(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            glow = CocCyan,
            innerPadding = 20.dp
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "COC-LIO",
                    style = TextStyle(brush = animatedLedTextBrush()),
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                )
                Text(
                    "CHÀO MỪNG ĐẾN VỚI CỘNG ĐỒNG COC-LIO",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Text(
                    "Để tạo và duy trì tài khoản, COC-LIO cần xử lý UID Firebase, email, tên và ảnh hồ sơ Google, trạng thái xác minh cùng thông tin đăng nhập. Dữ liệu này được dùng để xác thực, quản lý tài khoản và hiển thị danh tính thành viên.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = consentChecked,
                        onCheckedChange = onConsentChecked,
                        enabled = !isBusy
                    )
                    Text(
                        "Tôi đã đọc và đồng ý với việc xử lý dữ liệu tài khoản cần thiết để sử dụng COC-LIO.",
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }
                TextButton(
                    onClick = onOpenDetails,
                    enabled = !isBusy
                ) {
                    Text("XEM CHI TIẾT DỮ LIỆU", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                if (!readError.isNullOrBlank()) {
                    Text(
                        readError,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 10.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
                Button(
                    onClick = onGoogleSignIn,
                    enabled = consentChecked && !isBusy,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        when {
                            isBusy -> "ĐANG XÁC NHẬN..."
                            hasAuthenticatedGoogleAccount -> "ĐỒNG Ý & TIẾP TỤC"
                            else -> "ĐỒNG Ý & ĐĂNG NHẬP GOOGLE"
                        }
                    )
                }
                if (hasAuthenticatedGoogleAccount) {
                    TextButton(
                        onClick = onReject,
                        enabled = !isBusy
                    ) {
                        Text("KHÔNG ĐỒNG Ý / ĐĂNG XUẤT", color = MaterialTheme.colorScheme.error, fontSize = 10.sp)
                    }
                }
                Text(
                    "Dữ liệu hoạt động, thiết bị, phân tích, báo lỗi và đồng bộ JSON là các mục riêng; mặc định tắt và không được bật chỉ bằng ô xác nhận này.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 9.sp,
                    lineHeight = 13.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun RequiredAccountDataDetailsDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("CHI TIẾT DỮ LIỆU TÀI KHOẢN", fontWeight = FontWeight.Black) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("1. DỮ LIỆU CẦN THIẾT KHI ĐĂNG NHẬP", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                Text(
                    "COC-LIO xử lý Firebase UID, email Google, tên hiển thị, tên riêng, họ, ảnh đại diện, trạng thái xác minh email, nhà cung cấp đăng nhập, số lần và thời điểm đăng nhập. Mục đích là xác thực, tạo và duy trì tài khoản, quản lý hồ sơ và hiển thị danh tính người gửi trong cộng đồng.",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 10.sp,
                    lineHeight = 15.sp
                )
                Text("2. NƠI LƯU VÀ NGƯỜI CÓ THỂ XEM", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                Text(
                    "Thông tin xác thực được xử lý bằng Firebase Authentication; hồ sơ tài khoản được lưu trong Cloud Firestore. Bạn có thể xem hồ sơ của mình. Chủ sở hữu COC-LIO có quyền quản lý hồ sơ người dùng trong ứng dụng. Tên và ảnh đại diện có thể xuất hiện cạnh nội dung bạn gửi trong cộng đồng.",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 10.sp,
                    lineHeight = 15.sp
                )
                Text("3. DỮ LIỆU KHÔNG BẮT BUỘC", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                Text(
                    "Thông tin thiết bị và hoạt động, phân tích mức sử dụng, báo cáo lỗi/hiệu năng và đồng bộ JSON Clash of Clans lên tài khoản là các mục riêng. Các mục này mặc định tắt; bạn có thể quản lý riêng trong Cài đặt sau khi đăng nhập. Ô xác nhận trên màn hình này không bật các mục đó.",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 10.sp,
                    lineHeight = 15.sp
                )
                Text("4. JSON VÀ NỘI DUNG CỘNG ĐỒNG", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                Text(
                    "JSON bạn nhập được lưu trên điện thoại. Chỉ khi bật đồng bộ riêng thì bản JSON mới được gửi lên Firestore. Tin nhắn và JSON bạn chủ động chia sẻ trong cộng đồng có thể được thành viên đã đăng nhập xem theo quy tắc cộng đồng.",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 10.sp,
                    lineHeight = 15.sp
                )
                Text("5. THỜI HẠN, QUYỀN VÀ LIÊN HỆ", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                Text(
                    "Hồ sơ Firestore được giữ cho đến khi được xóa theo quy trình quản lý hoặc yêu cầu hợp lệ; dữ liệu trên điện thoại vẫn còn cho đến khi bạn xóa dữ liệu ứng dụng hoặc gỡ ứng dụng. Bạn có quyền yêu cầu xem, chỉnh sửa, hạn chế xử lý hoặc xóa dữ liệu và rút lại các lựa chọn tùy chọn trong Cài đặt. Việc rút lại không làm mất hiệu lực xử lý đã thực hiện hợp pháp trước đó. Đơn vị vận hành: COC-LIO. Dịch vụ xử lý và lưu trữ kỹ thuật: Google Firebase. Liên hệ: phunghuyhoahaikba@gmail.com.",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 10.sp,
                    lineHeight = 15.sp
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("ĐÓNG") }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurface
    )
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
        Text("Bắt đầu theo dõi", color = MaterialTheme.colorScheme.onSurface, fontSize = 21.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            "Lấy JSON trực tiếp từ game để COC-LIO đọc cấp độ, công trình đang nâng và thời gian còn lại.",
            color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, lineHeight = 18.sp
        )
        Spacer(Modifier.height(18.dp))
        NeonPanel(modifier = Modifier.fillMaxWidth(), glow = CocCyan) {
            Text("CÁCH LẤY JSON LẦN ĐẦU", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.Black)
            Text("1. Mở Clash of Clans và vào làng cần theo dõi.", color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp)
            Text("2. Nhấn bánh răng Cài đặt → Cài đặt khác.", color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp)
            Text("3. Kéo xuống Xuất dữ liệu → Xuất dữ liệu làng dưới dạng JSON → Sao chép.", color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp)
            Text("4. Quay lại COC-LIO, dán JSON rồi nhấn Phân tích.", color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp)
        }
        Spacer(Modifier.height(14.dp))
        NeonButton(text = "MỞ CLASH OF CLANS", onClick = onOpenGame, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(10.dp))
        NeonButton(text = "NHẬP JSON ĐẦU TIÊN", onClick = onImport, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        Text("Nút mở game chỉ đưa bạn vào trò chơi. Bạn cần tự thao tác xuất và sao chép JSON trong game.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, lineHeight = 15.sp)
    }
}

@Composable
private fun OverviewScreen(
    snapshot: VillageSnapshot,
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
    val activeUpgrades = snapshot.activeUpgradesAt(nowEpoch)
    val expiredUpgrades = snapshot.expiredUpgradesAt(nowEpoch)
    val homeRunning = activeUpgrades.filter { it.base == "home" }.sortedBy { it.timerSeconds ?: Long.MAX_VALUE }
    val builderRunning = activeUpgrades.filter { it.base == "builder" }.sortedBy { it.timerSeconds ?: Long.MAX_VALUE }
    val homeCompleted = expiredUpgrades.filter { it.base == "home" }.sortedBy { it.timerSeconds ?: Long.MAX_VALUE }
    val builderCompleted = expiredUpgrades.filter { it.base == "builder" }.sortedBy { it.timerSeconds ?: Long.MAX_VALUE }
    val completedUpgrades = (homeCompleted + builderCompleted).sortedBy { it.timerSeconds ?: Long.MAX_VALUE }
    val hasHomeBase = snapshot.homeItems.isNotEmpty()
    val hasBuilderBase = snapshot.builderItems.isNotEmpty()
    val homeHall = snapshot.items.firstOrNull { it.sectionKey == "buildings" && it.dataId == 1000001 }
    val builderHall = snapshot.items.firstOrNull { it.sectionKey == "buildings2" && it.dataId == 1000034 }
    val builderTotal = snapshot.homeBuilderTotal
    val builderBusy = snapshot.homeBuildersBusyAt(nowEpoch)
    val builderFree = builderTotal?.let { (it - builderBusy).coerceAtLeast(0) }
    val homeExtras = snapshot.extraItems.filter { it.base == "home" }
    val builderExtras = snapshot.extraItems.filter { it.base == "builder" }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        item {
            NeonPanel(modifier = Modifier.fillMaxWidth(), glow = CocCyan) {
                Column(Modifier.padding(horizontal = 9.dp, vertical = 7.dp)) {
                    Text("ĐANG THEO DÕI", color = MaterialTheme.colorScheme.onSurface, fontSize = 9.sp, letterSpacing = 1.sp)
                    Text(
                        snapshot.villageName?.takeIf { it.isNotBlank() } ?: "Làng chưa đặt tên",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Tag,
                            contentDescription = "Mã người chơi",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(10.dp)
                        )
                        Text("ID: ${snapshot.tag}", color = MaterialTheme.colorScheme.onSurface, fontSize = 9.sp)
                    }
                    Text("Cập nhật " + formatTimestamp(snapshot.timestamp), color = MaterialTheme.colorScheme.onSurface, fontSize = 9.sp)
                }
            }
        }

        if (hasHomeBase || hasBuilderBase) {
            item {
                when {
                    hasHomeBase && hasBuilderBase -> {
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
                    hasHomeBase -> BaseSummaryCard(
                        title = "NHÀ CHÍNH",
                        levelLabel = "TH",
                        level = snapshot.townHallLevel,
                        itemCount = snapshot.homeItems.size,
                        upgradeCount = homeRunning.size,
                        hallItem = homeHall,
                        modifier = Modifier.fillMaxWidth(),
                        glow = CocCyan
                    )
                    else -> BaseSummaryCard(
                        title = "LÀNG ĐÊM",
                        levelLabel = "BH",
                        level = snapshot.builderHallLevel,
                        itemCount = snapshot.builderItems.size,
                        upgradeCount = builderRunning.size,
                        hallItem = builderHall,
                        modifier = Modifier.fillMaxWidth(),
                        glow = CocMagenta
                    )
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                StatTile("Công trình", snapshot.items.count { it.tab == "buildings" }.toString(), Modifier.weight(1f))
                StatTile("Đang nâng", (homeRunning.size + builderRunning.size).toString(), Modifier.weight(1f), MaterialTheme.colorScheme.tertiary)
                StatTile("Thợ rảnh", if (builderTotal == null) "—" else "$builderFree/$builderTotal", Modifier.weight(1f), CocGreen)
            }
        }

        if (completedUpgrades.isNotEmpty()) {
            item { SectionTitle("HOÀN THÀNH • CHỜ JSON MỚI", completedUpgrades.size.toString()) }
            items(completedUpgrades, key = { "completed_" + it.base + "_" + it.identity + "_" + it.timerSeconds }) {
                UpgradeCard(it, snapshot.timestamp, showBase = true)
            }
        }

        if (hasHomeBase) {
            item { SectionTitle("NHÀ CHÍNH • ĐANG NÂNG", homeRunning.size.toString()) }
            if (homeRunning.isEmpty()) {
                item { InfoCard("Không có tiến trình", "Các mục đang nâng của Nhà chính sẽ xuất hiện ở đây.") }
            } else {
                items(homeRunning, key = { "home_" + it.identity + "_" + it.timerSeconds }) { UpgradeCard(it, snapshot.timestamp) }
            }

        }

        if (hasBuilderBase) {
            item { SectionTitle("LÀNG ĐÊM • ĐANG NÂNG", builderRunning.size.toString()) }
            if (builderRunning.isEmpty()) {
                item { InfoCard("Không có tiến trình", "Các mục đang nâng của Làng đêm sẽ xuất hiện ở đây.") }
            } else {
                items(builderRunning, key = { "builder_" + it.identity + "_" + it.timerSeconds }) { UpgradeCard(it, snapshot.timestamp) }
            }

        }

        val helpers = snapshot.items.filter { it.sectionKey == "helpers" }
        if (helpers.isNotEmpty()) {
            item { SectionTitle("TRỢ THỦ", helpers.size.toString()) }
            items(helpers.chunked(3), key = { row -> row.joinToString("_") { it.identity } }) { helperRow ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    helperRow.forEach { helper ->
                        HelperCompactCard(helper, Modifier.weight(1f))
                    }
                    repeat(3 - helperRow.size) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }

        if (hasHomeBase || homeExtras.isNotEmpty()) {
            item {
                SectionTitle("NHÀ CHÍNH", homeExtras.sumOf { it.count }.toString())
            }
            item { ExtraEntriesCard(homeExtras) }
        }

        if (hasBuilderBase || builderExtras.isNotEmpty()) {
            item {
                SectionTitle("LÀNG ĐÊM", builderExtras.sumOf { it.count }.toString())
            }
            item { ExtraEntriesCard(builderExtras) }
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
                Text("$levelLabel ${level ?: "—"}", color = MaterialTheme.colorScheme.onSurface, fontSize = 19.sp, fontWeight = FontWeight.Black, maxLines = 1)
                Text("$itemCount mục • $upgradeCount nâng", color = MaterialTheme.colorScheme.onSurface, fontSize = 9.sp, maxLines = 1)
            }
            hallItem?.let { CategoryImage(it, if (levelLabel == "TH") 60 else 52) }
        }
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier = Modifier, valueColor: Color = Color.White) {
    NeonPanel(modifier = modifier, glow = valueColor.takeIf { it != Color.White } ?: CocMagenta) {
        Text(value, color = valueColor, fontSize = 24.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(3.dp))
        Text(label, color = MaterialTheme.colorScheme.onSurface, fontSize = 10.sp, letterSpacing = 0.4.sp)
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
        Text(title, color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp, modifier = Modifier.weight(1f))
        Text(trailing, color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp)
    }
}

@Composable
private fun InfoCard(title: String, detail: String) {
    NeonPanel(modifier = Modifier.fillMaxWidth(), glow = CocViolet) {
        Text(title, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Spacer(Modifier.height(6.dp))
        Text(detail, color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp, lineHeight = 17.sp)
    }
}

@Composable
private fun UpgradeCard(item: VillageItem, snapshotTimestamp: Long, showBase: Boolean = false) {
    var nowEpoch by remember(item.identity, snapshotTimestamp) {
        mutableLongStateOf(System.currentTimeMillis() / 1000L)
    }
    LaunchedEffect(item.identity, snapshotTimestamp) {
        while (true) {
            delay(1000L)
            nowEpoch = System.currentTimeMillis() / 1000L
        }
    }
    val remaining = item.remainingSecondsAt(snapshotTimestamp, nowEpoch)
    val levelText = when {
        item.gearUp -> item.level?.let { "CẤP $it • GEAR-UP" } ?: "GEAR-UP"
        item.level != null -> "CẤP ${item.level} → ${item.level + 1}"
        else -> "ĐANG NÂNG"
    }
    val elapsedRatio = item.progressRatioAt(snapshotTimestamp, nowEpoch)
    val progressPercent = item.progressPercentAt(snapshotTimestamp, nowEpoch)
    val targetProgressColor = if (elapsedRatio < 0.5f) {
        androidx.compose.ui.graphics.lerp(Color(0xFFFF3B30), Color(0xFFFFD54F), elapsedRatio * 2f)
    } else {
        androidx.compose.ui.graphics.lerp(Color(0xFFFFD54F), Color(0xFF25D366), (elapsedRatio - 0.5f) * 2f)
    }
    val progressColor by androidx.compose.animation.animateColorAsState(
        targetValue = targetProgressColor,
        animationSpec = tween(900),
        label = "upgrade-progress-color"
    )
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
                Text(
                    item.name + if (showBase) {
                        if (item.base == "builder") " • LÀNG ĐÊM" else " • NHÀ CHÍNH"
                    } else "",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        levelText,
                        color = if (remaining == 0L) CocGreen else MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                    if (item.count > 1) {
                        Text(
                            "×${item.count}",
                            color = MaterialTheme.colorScheme.tertiary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.13f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(23.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(elapsedRatio)
                            .align(Alignment.CenterStart)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(progressColor.copy(alpha = 0.88f), progressColor)
                                )
                            )
                    )
                    Text(
                        if (remaining > 0L) "${formatCountdown(remaining)} • $progressPercent%"
                        else "HOÀN THÀNH • CHỜ JSON MỚI",
                        color = if (remaining == 0L) CocGreen else MaterialTheme.colorScheme.onSurface,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun BuildingsAndArmyScreen(snapshot: VillageSnapshot) {
    var selectedGroup by remember(snapshot.tag, snapshot.timestamp) {
        mutableStateOf("buildings")
    }
    val availableBases = listOf("home", "builder").filter { base ->
        snapshot.items.any { it.base == base }
    }
    var selectedBase by remember(snapshot.tag, snapshot.timestamp) {
        mutableStateOf(availableBases.firstOrNull() ?: "home")
    }
    Column(modifier = Modifier.fillMaxSize()) {
        if (availableBases.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                availableBases.forEach { base ->
                    FilterChip(
                        selected = selectedBase == base,
                        onClick = { selectedBase = base },
                        modifier = Modifier.weight(1f),
                        label = {
                            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Text(if (base == "home") "NHÀ CHÍNH" else "LÀNG ĐÊM", fontSize = 10.sp, maxLines = 1)
                            }
                        }
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedGroup == "buildings",
                onClick = { selectedGroup = "buildings" },
                modifier = Modifier.weight(1f),
                label = {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("CÔNG TRÌNH", fontSize = 10.sp, maxLines = 1)
                    }
                }
            )
            FilterChip(
                selected = selectedGroup == "army",
                onClick = { selectedGroup = "army" },
                modifier = Modifier.weight(1f),
                label = {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("QUÂN ĐỘI", fontSize = 10.sp, maxLines = 1)
                    }
                }
            )
        }
        CategoryScreen(
            snapshot = snapshot,
            tab = selectedGroup,
            selectedBase = selectedBase,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun CategoryScreen(
    snapshot: VillageSnapshot,
    tab: String,
    selectedBase: String,
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember(snapshot.tag, snapshot.timestamp, tab, selectedBase) {
        mutableStateOf("all")
    }

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
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        if (baseItems.isNotEmpty()) {
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
        }

        if (baseItems.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                InfoCard("Chưa có dữ liệu", "JSON không có mục thuộc nhóm này.")
            }
        } else if (filtered.isEmpty()) {

            item(span = { GridItemSpan(maxLineSpan) }) {
                InfoCard("Chưa có dữ liệu", "JSON không có mục thuộc nhóm này.")
            }
        } else {
            val grouped = filtered.groupBy { it.sectionKey }
            grouped.forEach { (sectionKey, rows) ->
                item(key = "section_$sectionKey", span = { GridItemSpan(maxLineSpan) }) {
                    SectionTitle(rows.first().sectionLabel.uppercase(), rows.sumOf { it.count }.toString())
                }
                val orderedRows = rows.filter { it.timerSeconds == null }.sortedWith(
                    compareBy<VillageItem> { it.count > 1 }
                        .thenBy { it.level ?: Int.MIN_VALUE }
                ) + rows.filter { it.timerSeconds != null }.sortedBy { it.timerSeconds ?: Long.MAX_VALUE }
                gridItems(
                    items = orderedRows,
                    key = { it.identity + "_" + (it.timerSeconds ?: -1L) + "_" + it.count },
                    span = { item ->
                        if (item.timerSeconds != null) GridItemSpan(maxLineSpan) else GridItemSpan(1)
                    }
                ) { item ->
                    if (item.timerSeconds != null) {
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
            modifier = Modifier
                .fillMaxWidth()
                .height(116.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            CategoryImage(item, 48)
            Text(
                item.level?.let { "CẤP $it" } ?: "—",
                color = MaterialTheme.colorScheme.tertiary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1
            )
            Row(
                modifier = Modifier.fillMaxWidth().height(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (item.count > 1) {
                    Text(
                        "×${item.count}",
                        color = MaterialTheme.colorScheme.tertiary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.16f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 1.dp)
                    )
                }
                if (item.gearUp) {
                    Spacer(Modifier.width(4.dp))
                    Text("GEAR-UP", color = CocGreen, fontSize = 8.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                }
                item.weaponLevel?.let {
                    Spacer(Modifier.width(4.dp))
                    Text("V$it", color = MaterialTheme.colorScheme.primary, fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                }
            }
            val detailText = (item.details + item.properties.map { (key, value) -> "$key: $value" }).joinToString(" • ")
            if (detailText.isNotBlank()) {
                Text(detailText, color = MaterialTheme.colorScheme.onSurface, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun HelperCompactCard(item: VillageItem, modifier: Modifier = Modifier) {
    NeonPanel(modifier = modifier.fillMaxWidth(), glow = CocMagenta, innerPadding = 5.dp) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            CategoryImage(item, 38)
            Text(item.name, color = MaterialTheme.colorScheme.onSurface, fontSize = 8.sp, fontWeight = FontWeight.Bold, maxLines = 2, textAlign = androidx.compose.ui.text.style.TextAlign.Center, overflow = TextOverflow.Ellipsis)
            Text("Cấp ${item.level ?: "—"}", color = MaterialTheme.colorScheme.tertiary, fontSize = 9.sp, fontWeight = FontWeight.Black)
            Text(
                item.helperCooldownSeconds?.let { "Hồi ${formatDuration(it)}" } ?: "Sẵn sàng",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 9.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun ExtraEntriesCard(entries: List<VillageExtraItem>) {
    val summary = entries
        .groupBy { it.sectionKey }
        .values
        .map { group -> group.first().sectionLabel to group.sumOf { it.count } }
        .joinToString(" • ") { (label, count) -> "$label ×$count" }

    NeonPanel(modifier = Modifier.fillMaxWidth(), glow = CocViolet, innerPadding = 7.dp) {
        Text(
            summary.ifBlank { "Chưa có dữ liệu phụ" },
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 10.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
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
                Text(item.level?.let { "CẤP $it" } ?: "—", color = MaterialTheme.colorScheme.tertiary, fontSize = 16.sp, fontWeight = FontWeight.Black, maxLines = 1)
                if (item.count > 1) Text("×${item.count}", color = MaterialTheme.colorScheme.onSurface, fontSize = 9.sp)
            }
            if (item.gearUp) Text("⚙", color = CocGreen, fontSize = 18.sp)
            item.weaponLevel?.let { Text("V$it", color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun CategoryImage(item: VillageItem, size: Int) {
    val context = LocalContext.current
    val url = CocAssets.imageUrl(item)
    val cachedFile = url?.let { CocAssetCache.cachedFile(context, it) }
    val imageRequest = remember(url, cachedFile) {
        url?.let {
            ImageRequest.Builder(context)
                .data(cachedFile ?: it)
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
    receivedSharedJsons: List<ReceivedSharedJsonRecord>,
    deletedReceivedSharedJsons: List<DeletedReceivedSharedJsonRecord>,
    pinnedTags: List<String>,
    activeTag: String,
    activeTimestamp: Long?,
    onSelect: (VillageSnapshot) -> Unit,
    onTogglePin: (String) -> Unit,
    onDelete: (String) -> Unit,
    onRestore: (String) -> Unit,
    onDeletePermanently: (String) -> Unit,
    onShare: (VillageSnapshot) -> Unit,
    onUseReceivedJson: (ReceivedSharedJsonRecord) -> Unit,
    onDeleteReceivedJson: (String) -> Unit,
    onRestoreReceivedJson: (String) -> Unit,
    onDeleteReceivedJsonPermanently: (String) -> Unit,
    onOpenSharedJsonFeed: () -> Unit
) {
    var selectedSection by remember { mutableStateOf("villages") }
    var pendingDelete by remember { mutableStateOf<VillageSnapshot?>(null) }
    var pendingPermanentDelete by remember { mutableStateOf<DeletedVillageRecord?>(null) }
    var pendingPermanentReceivedDelete by remember { mutableStateOf<DeletedReceivedSharedJsonRecord?>(null) }
    val nowSeconds = System.currentTimeMillis() / 1000L
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
                Text("KHO DỮ LIỆU JSON", color = MaterialTheme.colorScheme.primary, fontSize = 13.sp, fontWeight = FontWeight.Black)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = selectedSection == "villages",
                        onClick = { selectedSection = "villages" },
                        label = { Text("ĐANG LƯU", fontSize = 9.sp, maxLines = 1) }
                    )
                    FilterChip(
                        selected = selectedSection == "history",
                        onClick = { selectedSection = "history" },
                        label = { Text("LỊCH SỬ", fontSize = 9.sp, maxLines = 1) }
                    )
                    FilterChip(
                        selected = false,
                        onClick = onOpenSharedJsonFeed,
                        label = { Text("JSON CHIA SẺ", fontSize = 9.sp, maxLines = 1) }
                    )
                    FilterChip(
                        selected = selectedSection == "received",
                        onClick = { selectedSection = "received" },
                        label = { Text("ĐÃ NHẬN", fontSize = 9.sp, maxLines = 1) }
                    )
                    FilterChip(
                        selected = selectedSection == "deleted",
                        onClick = { selectedSection = "deleted" },
                        label = { Text("ĐÃ XÓA", fontSize = 9.sp, maxLines = 1) }
                    )
                    FilterChip(
                        selected = selectedSection == "deletedReceived",
                        onClick = { selectedSection = "deletedReceived" },
                        label = { Text("JSON ĐÃ GỠ", fontSize = 9.sp, maxLines = 1) }
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
                        glow = if (pinned) MaterialTheme.colorScheme.tertiary else if (snapshot.tag == activeTag) CocCyan else CocViolet
                    ) {
                        Column(Modifier.padding(6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    snapshot.villageName ?: snapshot.tag,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    if (pinned) "★" else "☆",
                                    color = if (pinned) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Tag,
                                    contentDescription = "Mã người chơi",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(11.dp)
                                )
                                Text(
                                    "ID: ${snapshot.tag}  •  TH ${snapshot.townHallLevel ?: "—"}  •  BH ${snapshot.builderHallLevel ?: "—"}  •  ${history.count { it.tag == snapshot.tag }} mốc  •  ${snapshot.activeUpgradesAt(nowSeconds).size} đang nâng",
                                    color = if (snapshot.tag == activeTag) CocGreen else MaterialTheme.colorScheme.onSurface,
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(onClick = { onShare(snapshot) }) {
                                    Text("CHIA SẺ JSON", color = MaterialTheme.colorScheme.primary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        } else if (selectedSection == "history") {
            item { Text("Tối đa 60 mốc nhập gần nhất trên thiết bị", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp) }
            if (history.isEmpty() && deletedVillages.none { it.snapshots.isNotEmpty() }) {
                item { InfoCard("Chưa có lịch sử nhập", "Các lần dán JSON sẽ được ghi nhận tại đây.") }
            } else {
                val deletedSnapshotKeys = deletedVillages.flatMap { record ->
                    record.snapshots.map { it.tag to it.timestamp }
                }.toSet()
                val chronologicalHistory = (history + deletedVillages.flatMap { it.snapshots })
                    .distinctBy { it.tag to it.timestamp }
                    .sortedByDescending { it.timestamp }
                items(chronologicalHistory, key = { it.tag + "_" + it.timestamp }) { snapshot ->
                    NeonPanel(
                        modifier = Modifier.fillMaxWidth().clickable { onSelect(snapshot) },
                        glow = if (snapshot.tag == activeTag) CocCyan else CocViolet
                    ) {
                        Column(Modifier.padding(6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(snapshot.villageName ?: snapshot.tag, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                if ((snapshot.tag to snapshot.timestamp) in deletedSnapshotKeys) {
                                    Text("ĐÃ XÓA", color = CocRed, fontSize = 9.sp, fontWeight = FontWeight.Black)
                                } else if (snapshot.tag == activeTag && snapshot.timestamp == activeTimestamp) {
                                    Text("ĐANG XEM", color = MaterialTheme.colorScheme.primary, fontSize = 9.sp, fontWeight = FontWeight.Black)
                                }
                            }
                            Text("${snapshot.tag}  •  ${formatTimestamp(snapshot.timestamp)}  •  TH ${snapshot.townHallLevel ?: "—"}  •  BH ${snapshot.builderHallLevel ?: "—"}  •  ${snapshot.items.size} mục", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        } else if (selectedSection == "received") {
            if (receivedSharedJsons.isEmpty()) {
                item { InfoCard("Chưa có JSON nhận từ người khác", "JSON bạn nhận và nhập từ Cộng đồng sẽ được lưu riêng ở đây. JSON tự chia sẻ của chính bạn không được đưa vào danh sách này.") }
            } else {
                items(receivedSharedJsons, key = { it.id }) { received ->
                    NeonPanel(modifier = Modifier.fillMaxWidth(), glow = CocCyan) {
                        Column(Modifier.padding(9.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(received.villageName.ifBlank { "JSON làng ${received.tag}" }, color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("Player Tag: ${received.tag}", color = MaterialTheme.colorScheme.onSurface, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text("Chia sẻ bởi: ${received.senderName}", color = MaterialTheme.colorScheme.onSurface, fontSize = 10.sp)
                            Text("Dữ liệu: ${formatTimestamp(received.timestamp)} • Nhận: ${formatTimestamp(received.receivedAt)}", color = MaterialTheme.colorScheme.onSurface, fontSize = 9.sp)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(onClick = { onUseReceivedJson(received) }) {
                                    Text("NHẬP / SỬ DỤNG", color = MaterialTheme.colorScheme.primary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                                TextButton(onClick = { onDeleteReceivedJson(received.id) }) {
                                    Text("GỠ", color = CocRed, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        } else if (selectedSection == "deletedReceived") {
            if (deletedReceivedSharedJsons.isEmpty()) {
                item { InfoCard("Chưa có JSON đã gỡ", "JSON đã gỡ khỏi mục ĐÃ NHẬN sẽ được lưu riêng tại đây, không làm mất bản trong LỊCH SỬ.") }
            } else {
                items(deletedReceivedSharedJsons, key = { it.record.id }) { deleted ->
                    val received = deleted.record
                    NeonPanel(modifier = Modifier.fillMaxWidth(), glow = CocRed) {
                        Column(Modifier.padding(9.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(received.villageName.ifBlank { "JSON làng ${received.tag}" }, color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("Player Tag: ${received.tag} • ${received.senderName}", color = MaterialTheme.colorScheme.onSurface, fontSize = 10.sp)
                            Text("Đã gỡ: ${formatTimestamp(deleted.deletedAt)}", color = MaterialTheme.colorScheme.onSurface, fontSize = 9.sp)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(onClick = { onRestoreReceivedJson(received.id) }) {
                                    Text("KHÔI PHỤC", color = CocGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                                TextButton(onClick = { pendingPermanentReceivedDelete = deleted }) {
                                    Text("XÓA VĨNH VIỄN", color = CocRed, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        } else {
            if (deletedVillages.isEmpty()) {
                item { InfoCard("Chưa có dữ liệu bị xóa", "Khi xóa một làng khỏi ĐANG LƯU, dữ liệu sẽ được lưu tại đây để khôi phục.") }
            } else {
                items(deletedVillages, key = { it.id }) { deleted ->
                    NeonPanel(modifier = Modifier.fillMaxWidth(), glow = CocRed) {
                        Column(Modifier.padding(9.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(deleted.villageName ?: deleted.tag, color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("${deleted.tag} • ${deleted.snapshots.size} mốc • ${formatTimestamp(deleted.deletedAt)}", color = MaterialTheme.colorScheme.onSurface, fontSize = 9.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(onClick = { onRestore(deleted.id) }) {
                                    Text("KHÔI PHỤC JSON", color = CocGreen, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                }
                                TextButton(onClick = { pendingPermanentDelete = deleted }) {
                                    Text("XÓA VĨNH VIỄN", color = CocRed, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    pendingPermanentDelete?.let { record ->
        AlertDialog(
            onDismissRequest = { pendingPermanentDelete = null },
            title = { Text("Xóa vĩnh viễn JSON?") },
            text = { Text("Toàn bộ ${record.snapshots.size} mốc JSON của ${record.villageName ?: record.tag} sẽ bị xóa khỏi thiết bị và không thể khôi phục.") },
            confirmButton = {
                TextButton(onClick = {
                    onDeletePermanently(record.id)
                    pendingPermanentDelete = null
                }) { Text("XÓA VĨNH VIỄN", color = CocRed) }
            },
            dismissButton = {
                TextButton(onClick = { pendingPermanentDelete = null }) { Text("HỦY") }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    pendingPermanentReceivedDelete?.let { deleted ->
        AlertDialog(
            onDismissRequest = { pendingPermanentReceivedDelete = null },
            title = { Text("Xóa vĩnh viễn JSON đã nhận?") },
            text = { Text("JSON từ ${deleted.record.senderName} sẽ bị xóa khỏi Kho JSON và không thể khôi phục. Bản nhập trong LỊCH SỬ vẫn được giữ.") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteReceivedJsonPermanently(deleted.record.id)
                    pendingPermanentReceivedDelete = null
                }) { Text("XÓA VĨNH VIỄN", color = CocRed) }
            },
            dismissButton = {
                TextButton(onClick = { pendingPermanentReceivedDelete = null }) { Text("HỦY") }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurface
        )
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
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = Color.White,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant
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
    val maxDialogHeight = (LocalConfiguration.current.screenHeightDp.dp - 32.dp).coerceAtLeast(240.dp)
    Dialog(onDismissRequest = onDismiss) {
        NeonPanel(modifier = Modifier.fillMaxWidth(), glow = CocMagenta) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = maxDialogHeight)
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(11.dp)
            ) {
                Text("NHẬP DỮ LIỆU LÀNG", color = MaterialTheme.colorScheme.primary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text("Sao chép JSON trong Clash of Clans rồi dán vào ô bên dưới. Có thể đặt tên riêng cho làng. JSON là ảnh chụp tại thời điểm xuất; nếu dùng sách, potion, trợ thủ hoặc tăng tốc sau đó, hãy nhập JSON mới.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, lineHeight = 14.sp)
                OutlinedTextField(
                    value = villageName,
                    onValueChange = { villageName = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("Tên làng (không bắt buộc)") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CocCyan,
                        unfocusedBorderColor = CocViolet,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        cursorColor = MaterialTheme.colorScheme.primary
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
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        cursorColor = MaterialTheme.colorScheme.primary
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
                        Text("DÁN JSON ĐÃ SAO CHÉP", color = MaterialTheme.colorScheme.primary, fontSize = 10.sp)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
                        border = BorderStroke(1.dp, CocViolet.copy(alpha = 0.65f))
                    ) {
                        Text("Đóng", color = MaterialTheme.colorScheme.primary)
                    }
                    Button(
                        onClick = { onImport(rawText, villageName) },
                        enabled = rawText.isNotBlank(),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CocCyan, contentColor = Color(0xFF04101D))
                    ) {
                        Text(
                            "Phân tích",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            maxLines = 1,
                            softWrap = false
                        )
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

fun formatDuration(seconds: Long): String {
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
