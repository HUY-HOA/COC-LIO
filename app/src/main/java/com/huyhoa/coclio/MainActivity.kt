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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.draw.drawWithCache
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
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
import androidx.compose.material3.Divider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text as MaterialText
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.collectAsState
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
import com.huyhoa.coclio.data.ReceivedSharedJsonStore
import com.huyhoa.coclio.data.VillageItem
import com.huyhoa.coclio.data.VillageExtraItem
import com.huyhoa.coclio.data.VillageParser
import com.huyhoa.coclio.data.VillageSnapshot
import com.huyhoa.coclio.data.VillageStore
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

private val CocBackground = Color(0xFF000000)
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
    val glowEnabled: Boolean,
    val progress: State<Float>
)

private data class ImportSummary(
    val title: String,
    val messages: List<String>
)

val LocalLedTheme = staticCompositionLocalOf {
    LedThemeState(DefaultLedPalette, true, 3000, 1f, false, mutableStateOf(0f))
}

private fun paletteForLed(preset: String): List<Color> = when (preset) {
    "ice" -> listOf(Color(0xFFB9F8FF), Color(0xFF36F4FF), Color(0xFF168BFF), Color(0xFFB9F8FF))
    "violet" -> listOf(Color(0xFFFFA5F5), Color(0xFFB33CFF), Color(0xFF6548FF), Color(0xFFFF35B8), Color(0xFFFFA5F5))
    "fire" -> listOf(Color(0xFFFFE082), Color(0xFFFFA43A), Color(0xFFFF4A35), Color(0xFFFF35B8), Color(0xFFFFE082))
    "mint" -> listOf(Color(0xFFB7F34A), Color(0xFF35E6A2), Color(0xFF00CFA8), Color(0xFFB7F34A))
    else -> DefaultLedPalette
}

internal fun ledCycleDurationMillis(
    speedPercent: Int,
    speedMultiplier: Float
): Int {
    val speed = speedPercent.coerceIn(0, 100)
    if (speed == 0) return 0

    val multiplier = speedMultiplier.coerceIn(0.5f, 3f).toDouble()
    val maxCyclesPerSecond = 1_000.0 / 375.0
    val cyclesPerSecond = maxCyclesPerSecond * (speed / 100.0) * multiplier

    return (1_000.0 / cyclesPerSecond).roundToInt().coerceAtLeast(1)
}

private fun interpolateLedColor(first: Color, second: Color, fraction: Float): Color {
    val firstHsv = FloatArray(3)
    val secondHsv = FloatArray(3)
    android.graphics.Color.colorToHSV(first.toArgb(), firstHsv)
    android.graphics.Color.colorToHSV(second.toArgb(), secondHsv)

    val hueDelta = ((secondHsv[0] - firstHsv[0] + 540f) % 360f) - 180f
    val hsv = floatArrayOf(
        (firstHsv[0] + hueDelta * fraction + 360f) % 360f,
        firstHsv[1] + (secondHsv[1] - firstHsv[1]) * fraction,
        firstHsv[2] + (secondHsv[2] - firstHsv[2]) * fraction
    )
    val alpha = (first.alpha + (second.alpha - first.alpha) * fraction) * 255f

    return Color(
        android.graphics.Color.HSVToColor(alpha.roundToInt().coerceIn(0, 255), hsv)
    )
}

internal fun movingLedColors(
    palette: List<Color>,
    phase: Float,
    sampleCount: Int = 256
): List<Color> {
    if (palette.size < 2) return palette
    val intervals = palette.size - 1
    val normalizedPhase = phase.rem(1f)
    return List(sampleCount.coerceAtLeast(2)) { index ->
        val position = ((index / sampleCount.toFloat() - normalizedPhase + 1f) % 1f) * intervals
        val startIndex = position.toInt().coerceIn(0, intervals - 1)
        val fraction = (position - startIndex).coerceIn(0f, 1f)
        interpolateLedColor(palette[startIndex], palette[startIndex + 1], fraction)
    }
}

internal fun androidx.compose.ui.graphics.drawscope.DrawScope.drawLedStroke(
    path: Path,
    brush: Brush,
    strokeWidth: Float,
    glowEnabled: Boolean
) {
    if (glowEnabled) {
        drawPath(path, brush, alpha = 0.035f, style = Stroke(width = strokeWidth * 9f))
        drawPath(path, brush, alpha = 0.065f, style = Stroke(width = strokeWidth * 6f))
        drawPath(path, brush, alpha = 0.12f, style = Stroke(width = strokeWidth * 4f))
        drawPath(path, brush, alpha = 0.22f, style = Stroke(width = strokeWidth * 2.2f))
    }
    drawPath(path, brush, style = Stroke(width = strokeWidth))
    if (glowEnabled) {
        drawPath(path, brush, alpha = 0.72f, style = Stroke(width = strokeWidth * 0.48f))
    }
}

@Composable
internal fun LedBrandTitle(
    text: String,
    fontSize: androidx.compose.ui.unit.TextUnit,
    letterSpacing: androidx.compose.ui.unit.TextUnit,
    modifier: Modifier = Modifier,
    textAlign: androidx.compose.ui.text.style.TextAlign = androidx.compose.ui.text.style.TextAlign.Start,
    outlineWidthDp: Float = 1.35f
) {
    val led = LocalLedTheme.current
    if (!led.enabled) {
        Text(
            text,
            modifier = modifier,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = fontSize,
            fontWeight = FontWeight.Black,
            letterSpacing = letterSpacing,
            textAlign = textAlign,
            maxLines = 1
        )
        return
    }

    val phase = led.progress.value.rem(1f)
    val fullPalette = led.palette.map { it.copy(alpha = 1f) }
    val haloPalette = led.palette.map { it.copy(alpha = 0.68f) }
    val coreBrush = Brush.linearGradient(movingLedColors(fullPalette, phase))
    val haloBrush = Brush.linearGradient(movingLedColors(haloPalette, phase))
    val outlineWidth = with(LocalDensity.current) { outlineWidthDp.coerceAtLeast(0.1f).dp.toPx() }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            style = TextStyle(
                brush = haloBrush,
                drawStyle = Stroke(width = outlineWidth * 3f),
                shadow = androidx.compose.ui.graphics.Shadow(
                    color = CocCyan.copy(alpha = 0.96f),
                    blurRadius = outlineWidth * 11f
                )
            ),
            fontSize = fontSize,
            fontWeight = FontWeight.Black,
            letterSpacing = letterSpacing,
            textAlign = textAlign,
            maxLines = 1
        )
        Text(
            text,
            style = TextStyle(
                brush = haloBrush,
                drawStyle = Stroke(width = outlineWidth),
                shadow = androidx.compose.ui.graphics.Shadow(
                    color = CocMagenta.copy(alpha = 0.82f),
                    blurRadius = outlineWidth * 4f
                )
            ),
            fontSize = fontSize,
            fontWeight = FontWeight.Black,
            letterSpacing = letterSpacing,
            textAlign = textAlign,
            maxLines = 1
        )
        Text(
            text,
            style = TextStyle(
                brush = coreBrush,
                shadow = androidx.compose.ui.graphics.Shadow(
                    color = CocCyan.copy(alpha = 0.72f),
                    blurRadius = outlineWidth * 3f
                )
            ),
            fontSize = fontSize,
            fontWeight = FontWeight.Black,
            letterSpacing = letterSpacing,
            textAlign = textAlign,
            maxLines = 1
        )
    }
}

@Composable
internal fun LedAdminAvatar(size: androidx.compose.ui.unit.Dp = 30.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        LedBrandTitle(
            text = "ADMIN",
            fontSize = (size.value * 0.19f).sp,
            letterSpacing = 0.05.sp,
            outlineWidthDp = (size.value * 0.018f).coerceIn(0.35f, 0.7f)
        )
    }
}

@Composable
private fun LedFrameBox(
    modifier: Modifier = Modifier,
    content: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit
) {
    val led = LocalLedTheme.current
    val palette = led.palette.map { it.copy(alpha = led.brightness) }
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
                if (led.enabled) {
                    val phase = led.progress.value.rem(1f)
                    drawLedStroke(
                        path,
                        Brush.sweepGradient(movingLedColors(palette, phase)),
                        stroke,
                        led.glowEnabled
                    )
                }
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
    val palette = led.palette.map { it.copy(alpha = led.brightness.coerceIn(0f, 1f)) }
    val active = led.enabled && palette.size >= 2 && strokeWidthDp > 0f
    val glowEnabled = led.glowEnabled
    return this.drawWithCache {
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
            if (active) {
                val phase = led.progress.value.rem(1f)
                drawLedStroke(
                    path,
                    Brush.sweepGradient(movingLedColors(palette, phase)),
                    stroke,
                    glowEnabled
                )
            }
        }
    }
}

@Composable
private fun NeonBackdrop() {
    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
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
                onDrawWithContent {
                    drawContent()
                    if (led.enabled) {
                        val ledProgress = led.progress.value.rem(1f)
                        drawLedStroke(
                            path,
                            Brush.sweepGradient(movingLedColors(palette, ledProgress)),
                            stroke,
                            led.glowEnabled
                        )
                    }
                }
            }
            .clip(shape)
            .background(Brush.linearGradient(listOf(
                MaterialTheme.colorScheme.surface,
                MaterialTheme.colorScheme.surfaceVariant,
                MaterialTheme.colorScheme.surface
            )))
            .padding(innerPadding),
        content = content
    )
}

@Composable
private fun NeonButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val shape = RoundedCornerShape(14.dp)
    val buttonBackground = Color(0xFF168BFF).copy(alpha = if (enabled) 1f else 0.45f)
    Box(
        modifier = modifier
            .shadow(5.dp, shape)
            .ledRgbBorder(cornerRadiusDp = 14f, strokeWidthDp = 2f)
            .clip(shape)
            .background(buttonBackground)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            color = if (enabled) Color(0xFF06101F) else MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Black,
            fontSize = 11.sp,
            letterSpacing = 0.25.sp
        )
    }
}

class MainActivity : ComponentActivity() {
    override fun onStart() {
        super.onStart()
        RemoteAppConfigManager.startRealtimeUpdates()
        RemoteContentManager.onHostStart(this)
    }

    override fun onStop() {
        RemoteAppConfigManager.stopRealtimeUpdates()
        RemoteContentManager.stopRealtimeUpdates()
        super.onStop()
    }

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
        RemoteAppConfigManager.initialize()
        setContent {
            val remoteConfig by RemoteAppConfigManager.config.collectAsState()
            var themeMode by remember { mutableStateOf(AppPreferences.themeMode(this@MainActivity)) }
            var languagePreference by remember { mutableStateOf(AppLanguageSettings.load(this@MainActivity)) }
            val deviceLanguage = LocalConfiguration.current.locales[0].language
            val dark = when (themeMode) {
                "light" -> false
                "dark" -> true
                else -> isSystemInDarkTheme()
            }
            val colors = if (dark) {
                darkColorScheme(
                    primary = Color(remoteConfig.darkPrimary),
                    onPrimary = Color(0xFF04101D),
                    secondary = Color(remoteConfig.darkSecondary),
                    tertiary = Color(remoteConfig.darkTertiary),
                    background = Color(remoteConfig.darkBackground),
                    surface = Color(remoteConfig.darkSurface),
                    surfaceVariant = Color(remoteConfig.darkSurfaceVariant),
                    onBackground = Color.White,
                    onSurface = Color.White,
                    onSurfaceVariant = Color(remoteConfig.darkOnSurfaceVariant)
                )
            } else {
                lightColorScheme(
                    primary = Color(remoteConfig.lightPrimary),
                    onPrimary = Color.White,
                    secondary = Color(remoteConfig.lightSecondary),
                    tertiary = Color(remoteConfig.lightTertiary),
                    background = Color(remoteConfig.lightBackground),
                    surface = Color(remoteConfig.lightSurface),
                    surfaceVariant = Color(remoteConfig.lightSurfaceVariant),
                    onBackground = Color(remoteConfig.lightOnBackground),
                    onSurface = Color(remoteConfig.lightOnSurface),
                    onSurfaceVariant = Color(remoteConfig.lightOnSurfaceVariant)
                )
            }
            CompositionLocalProvider(
                LocalAppLanguage provides AppLanguageSettings.resolve(languagePreference, deviceLanguage)
            ) {
                MaterialTheme(colorScheme = colors) {
                    CocLioApp(
                        languagePreference = languagePreference,
                        onLanguagePreferenceChanged = { preference ->
                            AppLanguageSettings.save(this@MainActivity, preference)
                            languagePreference = preference
                            AppWidgetUpdater.updateAll(this@MainActivity)
                        },
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
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun CocLioApp(
    languagePreference: String,
    onLanguagePreferenceChanged: (String) -> Unit,
    themeMode: String,
    onThemeModeChanged: (String) -> Unit
) {
    val context = LocalContext.current
    val remoteAppConfig by RemoteAppConfigManager.config.collectAsState()
    val remoteContent by RemoteContentManager.content.collectAsState()
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
    var accountProfileReadyForEmail by remember { mutableStateOf<String?>(null) }
    var accountProfileReady by remember { mutableStateOf(false) }
    var googleSignInSubmitting by remember { mutableStateOf(false) }
    var accountProfileError by remember { mutableStateOf<String?>(null) }
    var villageDataSyncConsent by remember { mutableStateOf(false) }
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
        accountProfileReadyForEmail?.equals(firebaseGoogleEmail, ignoreCase = true) == true &&
        accountProfileReady
    val isPreparingGoogleAccount = firebaseGoogleEmail != null &&
        accountProfileReadyForEmail == null &&
        !googleSignInSubmitting
    LaunchedEffect(isGoogleSignedIn) {
        RemoteContentManager.setAccessAllowed(context, isGoogleSignedIn)
    }
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
    val remoteNavigationItems = remoteAppConfig.navigationItems
        .filter { it.enabled && remoteAppConfig.featureEnabled(it.id) }
        .take(6)
        .ifEmpty { remoteAppConfig.navigationItems.firstOrNull()?.let { listOf(it.copy(enabled = true)) }.orEmpty() }
    LaunchedEffect(remoteNavigationItems, selectedTab) {
        if (remoteNavigationItems.none { it.page == selectedTab }) {
            selectedTab = remoteNavigationItems.firstOrNull()?.page ?: 0
        }
    }
    var communityInitialSection by remember { mutableStateOf(0) }
    var showImportDialog by remember { mutableStateOf(false) }
    var importError by remember { mutableStateOf<String?>(null) }
    var googleAuthError by remember { mutableStateOf<String?>(null) }
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
    var communityNotificationsEnabled by remember(activeProfile) {
        mutableStateOf(AppPreferences.communityNotificationsEnabled(context, activeProfile))
    }
    var updateNotificationsEnabled by remember(activeProfile) {
        mutableStateOf(AppPreferences.updateNotificationsEnabled(context, activeProfile))
    }
    LaunchedEffect(activeProfile, isGoogleSignedIn, communityNotificationsEnabled, updateNotificationsEnabled) {
        val notificationsAllowed = isGoogleSignedIn
        CommunityNotificationScheduler.update(
            context,
            activeProfile,
            notificationsAllowed,
            communityNotificationsEnabled,
            updateNotificationsEnabled
        )
        FirebaseMessagingTopicSubscriptions.sync(context, activeProfile, notificationsAllowed)
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


    val ledPrefs = remember {
        context.getSharedPreferences("coc_lio_led", android.content.Context.MODE_PRIVATE).also { prefs ->
            if (prefs.getInt("settings_version", 0) < 1) {
                prefs.edit()
                    .putInt("speed", 50)
                    .putInt("settings_version", 1)
                    .apply()
            }
            if (prefs.getInt("settings_version", 0) < 2) {
                val editor = prefs.edit()
                if (!prefs.contains("brightness")) editor.putFloat("brightness", 1f)
                editor.putInt("settings_version", 2).apply()
            }
            if (prefs.getInt("settings_version", 0) < 3) {
                val editor = prefs.edit()
                if (prefs.getFloat("brightness", 1f) == 0.5f) {
                    editor.putFloat("brightness", 1f)
                }
                editor.putInt("settings_version", 3).apply()
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
    var ledBrightness by remember { mutableStateOf(ledPrefs.getFloat("brightness", 1f).coerceIn(0f, 1f)) }
    var ledGlowEnabled by remember { mutableStateOf(ledPrefs.getBoolean("glow_enabled", true)) }
    val ledSpeedMultiplier = remoteAppConfig.ledSpeedMultiplier.coerceIn(0.5f, 3f)
    val ledSpeedMillis = if (ledEnabled) {
        ledCycleDurationMillis(ledSpeed, ledSpeedMultiplier)
    } else {
        0
    }
    val ledProgress = remember { mutableStateOf(0f) }
    val currentLedSpeedMillis by rememberUpdatedState(ledSpeedMillis)
    LaunchedEffect(ledSpeedMillis > 0) {
        if (ledSpeedMillis > 0) {
            var lastFrameNanos = 0L
            while (true) {
                withFrameNanos { frameTimeNanos ->
                    val speedForFrame = currentLedSpeedMillis
                    if (lastFrameNanos != 0L && speedForFrame > 0) {
                        val elapsedNanos = (frameTimeNanos - lastFrameNanos).coerceAtLeast(0L)
                        val progressDelta = elapsedNanos.toDouble() /
                            (speedForFrame.toDouble() * 1_000_000.0)
                        ledProgress.value = (ledProgress.value.toDouble() + progressDelta)
                            .rem(1.0)
                            .toFloat()
                    }
                    lastFrameNanos = frameTimeNanos
                }
            }
        }
    }
    val ledTheme = LedThemeState(
        palette = if (ledPreset == "rainbow") {
            remoteAppConfig.ledPalette.takeIf { it.size >= 2 }?.map { Color(it) }
                ?: paletteForLed(ledPreset)
        } else {
            paletteForLed(ledPreset)
        },
        enabled = ledEnabled,
        speedMillis = ledSpeedMillis,
        brightness = ledBrightness.coerceIn(0f, 1f),
        glowEnabled = ledGlowEnabled,
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
            scope.launch { snackbar.showAppSnackbar(context, "Bạn chưa cấp quyền thông báo cho COC-LIO.") }
        }
    }

    val firstLaunchNotificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        AppPreferences.setNotificationsEnabled(context, activeProfile, granted)
        AppPreferences.setCommunityNotificationsEnabled(context, activeProfile, granted)
        AppPreferences.setUpdateNotificationsEnabled(context, activeProfile, granted)
        notificationsEnabled = granted
        communityNotificationsEnabled = granted
        updateNotificationsEnabled = granted
        UpgradeReminderScheduler.scheduleAll(context, history, activeProfile, granted)
        if (!granted) {
            scope.launch { snackbar.showAppSnackbar(context, "Bạn chưa cấp quyền thông báo cho COC-LIO.") }
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
            scope.launch { snackbar.showAppSnackbar(context, "Bạn chưa cấp quyền thông báo cho COC-LIO.") }
        }
    }

    val testNotificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val sent = UpgradeNotificationTester.show(context, activeProfile, notificationStyle)
            scope.launch {
                snackbar.showAppSnackbar(context, 
                    if (sent) "Đã gửi thông báo thử. Hãy kéo thanh thông báo xuống để xem."
                    else "Không thể gửi thông báo thử. Hãy kiểm tra quyền thông báo của ứng dụng."
                )
            }
        } else {
            scope.launch { snackbar.showAppSnackbar(context, "Bạn chưa cấp quyền thông báo cho COC-LIO.") }
        }
    }

    LaunchedEffect(Unit) {
        val permissionPrefs = context.getSharedPreferences("coc_lio_permission_state", android.content.Context.MODE_PRIVATE)
        if (!permissionPrefs.getBoolean("notification_permission_requested_v1", false)) {
            permissionPrefs.edit().putBoolean("notification_permission_requested_v1", true).apply()
            if (Build.VERSION.SDK_INT >= 33 &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) {
                delay(800)
                firstLaunchNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                AppPreferences.setNotificationsEnabled(context, activeProfile, true)
                AppPreferences.setCommunityNotificationsEnabled(context, activeProfile, true)
                AppPreferences.setUpdateNotificationsEnabled(context, activeProfile, true)
                notificationsEnabled = true
                communityNotificationsEnabled = true
                updateNotificationsEnabled = true
                UpgradeReminderScheduler.scheduleAll(context, history, activeProfile, true)
            }
        }
    }

    LaunchedEffect(activeProfile) {
        val notificationPermissionGranted = Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        AppPreferences.ensureNotificationPreferences(context, activeProfile, notificationPermissionGranted)
        notificationsEnabled = AppPreferences.notificationsEnabled(context, activeProfile)
        communityNotificationsEnabled = AppPreferences.communityNotificationsEnabled(context, activeProfile)
        updateNotificationsEnabled = AppPreferences.updateNotificationsEnabled(context, activeProfile)
        history = VillageStore.loadAll(context, activeProfile)
        current = history.firstOrNull()
        deletedVillages = VillageStore.loadDeleted(context, activeProfile)
        pinnedTags = VillageStore.pinnedTags(context, activeProfile)
        receivedSharedJsons = ReceivedSharedJsonStore.loadReceived(context, activeProfile)
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

    LaunchedEffect(firebaseGoogleEmail, googleSignInSubmitting) {
        val email = firebaseGoogleEmail
        if (email.isNullOrBlank()) {
            accountProfileReady = false
            accountProfileReadyForEmail = ""
            accountProfileError = null
            return@LaunchedEffect
        }
        if (googleSignInSubmitting) return@LaunchedEffect
        if (
            accountProfileReady &&
            accountProfileReadyForEmail?.equals(email, ignoreCase = true) == true
        ) return@LaunchedEffect
        accountProfileReady = false
        accountProfileReadyForEmail = null
        accountProfileError = null
        runCatching {
            if (!AccountPrivacyConsent.hasRequiredAccountDataConsent()) {
                val user = FirebaseAuth.getInstance().currentUser
                    ?: throw IllegalStateException("Bạn cần đăng nhập Google trước.")
                val googleAccount = GoogleAccountData(
                    email = user.email?.trim()?.lowercase()
                        ?: throw IllegalStateException("Google không trả về địa chỉ email hợp lệ."),
                    displayName = user.displayName?.trim()?.takeIf { it.isNotBlank() },
                    givenName = null,
                    familyName = null,
                    profilePictureUri = user.photoUrl?.toString()
                )
                val profileExists = AccountPrivacyConsent.hasAccountProfile()
                if (profileExists) {
                    AccountPrivacyConsent.saveRequiredAccountDataConsent()
                }
                GoogleAccountAuth.shareProfile(googleAccount, countAsSignIn = false)
                if (!profileExists) {
                    AccountPrivacyConsent.saveRequiredAccountDataConsent()
                }
            }
            AccountPrivacyConsent.hasRequiredAccountDataConsent()
        }
            .onSuccess { ready ->
                accountProfileReady = ready
                accountProfileReadyForEmail = email
                if (!ready) {
                    accountProfileError = "Không thể hoàn tất thiết lập hồ sơ tài khoản."
                }
            }
            .onFailure { error ->
                accountProfileReady = false
                accountProfileReadyForEmail = email
                accountProfileError = error.message
                    ?.takeIf { it.isNotBlank() }
                    ?: "Không thể thiết lập hồ sơ tài khoản."
            }
    }

    LaunchedEffect(isGoogleSignedIn, activeProfile) {
        if (!isGoogleSignedIn) {
            villageDataSyncConsent = false
            AccountPrivacyConsent.applyCollectionSettings(context, AccountPrivacyChoice())
            AccountActivityTracker.stopAndWait(context)
        } else {
            runCatching { AccountPrivacyConsent.load() }
                .onSuccess { choice ->
                    villageDataSyncConsent = choice.villageDataSync
                    AccountPrivacyConsent.applyCollectionSettings(context, choice)
                    if (choice.securityTelemetry) {
                        AccountActivityTracker.start(context)
                    } else {
                        AccountActivityTracker.stopAndWait(context)
                    }
                }
                .onFailure {
                    villageDataSyncConsent = false
                    AccountPrivacyConsent.applyCollectionSettings(context, AccountPrivacyChoice())
                    AccountActivityTracker.stopAndWait(context)
                    snackbar.showAppSnackbar(context, "Không tải được lựa chọn dữ liệu tài khoản. Các mục tùy chọn vẫn đang tắt.")
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
                snackbar.showAppSnackbar(context, "Bạn cần đăng nhập Google trước khi nhập JSON và xem dữ liệu làng.")
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
                            snackbar.showAppSnackbar(context, "Đã đồng bộ bản JSON mới nhất của ${parsed.tag} lên tài khoản COC-LIO.")
                        }
                    } catch (error: Exception) {
                        snackbar.showAppSnackbar(context, 
                            error.message?.takeIf { it.isNotBlank() }
                                ?: "JSON vẫn được lưu trên điện thoại nhưng chưa thể đồng bộ lên tài khoản của bạn."
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

    suspend fun finishGoogleSignIn(googleAccount: GoogleAccountData) {
        val savedName = AccountProfileStore.preferredName(context, googleAccount.email)
        val profileName = (
            savedName?.takeIf { it.isNotBlank() }
                ?: googleAccount.displayName?.takeIf { it.isNotBlank() }
                ?: googleAccount.email.substringBefore("@")
            ).take(40)
        if (savedName.isNullOrBlank()) {
            AccountProfileStore.setPreferredName(context, googleAccount.email, profileName)
        }
        AccountProfileStore.saveGoogleData(context, googleAccount)
        val accountProfileExists = AccountPrivacyConsent.hasAccountProfile()
        if (accountProfileExists) {
            AccountPrivacyConsent.saveRequiredAccountDataConsent()
        }
        GoogleAccountAuth.shareProfile(googleAccount)
        if (!accountProfileExists) {
            AccountPrivacyConsent.saveRequiredAccountDataConsent()
        }
        FirebaseAccountAdministration.savePreferredName(profileName)
        AccountProfileStore.setActive(context, googleAccount.email)
        accountProfileReady = true
        accountProfileReadyForEmail = googleAccount.email
        googleSignInSubmitting = false
        accountProfileError = null
        activeProfile = googleAccount.email
        interactiveGoogleSignIn = false
        selectedTab = 0
        googleAuthError = null
        snackbar.showAppSnackbar(context, "Đăng nhập Google thành công.")
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
            snackbar.showAppSnackbar(context, message)
        }
    }



    fun signInWithGoogle() {
        if (googleSignInSubmitting || interactiveGoogleSignIn) return
        scope.launch {
            interactiveGoogleSignIn = true
            googleSignInSubmitting = true
            googleAuthError = null
            accountProfileError = null
            try {
                val selection = GoogleAccountAuth.selectAccount(context)
                val googleAccount = GoogleAccountAuth.authenticateSelectedAccount(selection)
                finishGoogleSignIn(googleAccount)
            } catch (error: Exception) {
                googleSignInSubmitting = false
                interactiveGoogleSignIn = false
                if (error is GetCredentialCancellationException) {
                    googleAuthError = null
                    snackbar.showAppSnackbar(context, "Bạn đã hủy đăng nhập Google.")
                } else {
                    googleAuthError = error.message
                        ?.takeIf { it.isNotBlank() }
                        ?: "Không thể hoàn tất đăng nhập. Hãy kiểm tra quyền truy cập tài khoản và cấu hình Firebase."
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
                    LedBrandTitle(
                        text = remoteAppConfig.appTitle,
                        fontSize = 23.sp,
                        letterSpacing = 2.sp
                    )
                    Text(
                        remoteAppConfig.appSubtitle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 8.sp,
                        letterSpacing = 0.3.sp,
                        maxLines = 1
                    )
                }
                if (remoteAppConfig.featureEnabled("importButton", remoteAppConfig.showImportButton)) {
                    NeonButton(
                        text = if (isGoogleSignedIn) "＋ NHẬP JSON" else "ĐĂNG NHẬP",
                        onClick = { requestImportJson() }
                    )
                }
            }
            }
        },
        bottomBar = {
            if (isGoogleSignedIn) {
            CompositionLocalProvider(LocalRippleConfiguration provides null) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f),
                    contentColor = MaterialTheme.colorScheme.onSurface
                ) {
                val navItems = remoteNavigationItems
                navItems.forEach { item ->
                    NavigationBarItem(
                        selected = selectedTab == item.page,
                        onClick = { selectedTab = item.page },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = Color.Transparent
                        ),
                        icon = {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .then(
                                        if (selectedTab == item.page) {
                                            Modifier.ledRgbBorder(cornerRadiusDp = 10f, strokeWidthDp = 1.8f)
                                        } else {
                                            Modifier
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = when (item.id) {
                                        "home" -> Icons.Filled.Home
                                        "buildings" -> Icons.Filled.Business
                                        "history" -> Icons.Filled.History
                                        "community" -> Icons.Filled.Forum
                                        "lookup" -> Icons.Filled.Tag
                                        else -> Icons.Filled.Settings
                                    },
                                    contentDescription = item.title,
                                    modifier = Modifier.size(20.dp),
                                    tint = if (selectedTab == item.page) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        label = {
                            Text(
                                item.title,
                                fontSize = 9.sp,
                                lineHeight = 10.sp,
                                maxLines = 2,
                                softWrap = true,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                overflow = TextOverflow.Clip,
                                color = if (selectedTab == item.page) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                }
                }
            }
            }
        }
    ) { innerPadding ->
        if (!isGoogleSignedIn) {
            if (isPreparingGoogleAccount && !googleSignInSubmitting) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "ĐANG ĐỒNG BỘ HỒ SƠ TÀI KHOẢN...",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
            } else {
                GoogleLoginStartScreen(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    appTitle = remoteAppConfig.appTitle,
                    appSubtitle = remoteAppConfig.appSubtitle,
                    isBusy = googleSignInSubmitting || interactiveGoogleSignIn,
                    readError = accountProfileError,
                    onGoogleSignIn = { signInWithGoogle() }
                )
            }
        } else {
        val visibleRemoteBlocks = remoteAppConfig.dashboardBlocks.filter {
            (it.page == selectedTab || it.page == -1) &&
                remoteAppConfig.featureEnabled("dashboardBlocks")
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (visibleRemoteBlocks.isNotEmpty()) {
                RemoteDashboardBlocks(
                    blocks = visibleRemoteBlocks,
                    onAction = { action ->
                        when (action) {
                            "import_json" -> requestImportJson()
                            "open_game" -> openClashOfClans(context)
                            "open_settings" -> selectedTab = 4
                            "open_community" -> selectedTab = 3
                            "open_history" -> selectedTab = 2
                            "open_lookup" -> selectedTab = 5
                        }
                    }
                )
            }
            RemoteServiceNotices(
                content = remoteContent,
                onAction = { action ->
                    when (action) {
                        "import_json" -> requestImportJson()
                        "open_game" -> openClashOfClans(context)
                        "open_settings" -> selectedTab = 4
                        "open_community" -> selectedTab = 3
                        "open_history" -> selectedTab = 2
                        "open_lookup" -> selectedTab = 5
                    }
                }
            )
            if (selectedTab == 0 && (remoteContent.guides.isNotEmpty() || remoteContent.links.isNotEmpty())) {
                RemoteGuidesSection(
                    guides = remoteContent.guides,
                    links = remoteContent.links
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
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
                        BuildingsAndArmyScreen(
                            snapshot = it,
                            remoteCatalog = remoteContent.catalog
                        )
                    } ?: EmptyWelcome(
                        onImport = { requestImportJson() },
                        onOpenGame = { openClashOfClans(context) }
                    )
                    2 -> HistoryScreen(
                        history = history,
                        deletedVillages = deletedVillages,
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
                                scope.launch { snackbar.showAppSnackbar(context, "Đã chuyển $tag vào mục ĐÃ GỠ.") }
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
                                scope.launch { snackbar.showAppSnackbar(context, "Đã khôi phục dữ liệu làng.") }
                            }
                        },
                        onDeletePermanently = { recordId ->
                            if (VillageStore.permanentlyDelete(context, recordId, activeProfile)) {
                                deletedVillages = VillageStore.loadDeleted(context, activeProfile)
                                scope.launch { snackbar.showAppSnackbar(context, "Đã xóa vĩnh viễn dữ liệu JSON.") }
                            }
                        },
                        onShare = { snapshot ->
                            scope.launch {
                                runCatching { shareVillageJsonToCommunity(snapshot) }
                                    .onSuccess { snackbar.showAppSnackbar(context, it) }
                                    .onFailure { error ->
                                        snackbar.showAppSnackbar(context, error.message ?: "Không thể chia sẻ JSON.")
                                    }
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
                        receivedJsons = receivedSharedJsons,
                        onDeleteReceivedJson = { jsonId ->
                            if (ReceivedSharedJsonStore.deleteReceived(context, jsonId, activeProfile)) {
                                receivedSharedJsons = ReceivedSharedJsonStore.loadReceived(context, activeProfile)
                                scope.launch { snackbar.showAppSnackbar(context, "Đã xóa JSON nhận được khỏi danh sách chia sẻ. Bản trong lịch sử vẫn được giữ.") }
                            }
                        },
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
                    5 -> ClashLookupScreen()
                    4 -> SettingsScreen(
                        accountEmail = activeProfile.takeUnless { it == "local" },
                        isAdmin = FirebaseAccountAdministration.isOwner(activeProfile),
                        accountData = AccountProfileStore.googleData(context, activeProfile),
                        preferredName = AccountProfileStore.preferredName(context, activeProfile),
                        lastUsedLabel = lastUsedAt.takeIf { it > 0L }?.let { formatTimestamp(it / 1000L) },
                        knownProfiles = AccountProfileStore.knownProfiles(context, activeProfile),
                        themeMode = themeMode,
                        onThemeModeChanged = onThemeModeChanged,
                        languagePreference = languagePreference,
                        onLanguagePreferenceChanged = onLanguagePreferenceChanged,
                        textScale = appFontScale,
                        displaySizeLevel = displaySizeLevel,
                        onDisplaySizeLevelChanged = { level ->
                            displaySizeLevel = level.coerceIn(1, 10)
                            AppPreferences.setDisplaySizeLevel(context, displaySizeLevel)
                            AppWidgetUpdater.updateAll(context)
                        },
                        notificationsEnabled = notificationsEnabled,
                        ledEnabled = ledEnabled,
                        ledPreset = ledPreset,
                        ledSpeed = ledSpeed,
                        ledBrightness = ledBrightness,
                        ledGlowEnabled = ledGlowEnabled,
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
                        onLedGlowEnabledChanged = { value ->
                            ledGlowEnabled = value
                            ledPrefs.edit().putBoolean("glow_enabled", value).apply()
                        },
                        notificationStyle = notificationStyle,
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
                        onTestNotification = {
                            if (Build.VERSION.SDK_INT >= 33 &&
                                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                            ) {
                                testNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                val sent = UpgradeNotificationTester.show(context, activeProfile, notificationStyle)
                                scope.launch {
                                    snackbar.showAppSnackbar(context, 
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
    appTitle: String,
    appSubtitle: String,
    isBusy: Boolean,
    readError: String?,
    onGoogleSignIn: () -> Unit
) {
    val context = LocalContext.current
    Box(
        modifier = modifier.padding(10.dp),
        contentAlignment = Alignment.Center
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
                LedBrandTitle(
                    text = appTitle,
                    fontSize = 26.sp,
                    letterSpacing = 2.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Text(
                    appSubtitle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 9.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Text(
                    "CHÀO MỪNG ĐẾN VỚI CỘNG ĐỒNG COC-LIO",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Text(
                    "Google sẽ chia sẻ email, tên và ảnh hồ sơ của bạn với COC-LIO khi bạn chấp thuận trên màn hình Google. COC-LIO sử dụng các thông tin này để xác thực, tạo và duy trì tài khoản, quản lý tài khoản và hiển thị danh tính thành viên. Mã tài khoản được tạo trong Firebase; hồ sơ được lưu trong Firestore.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                TextButton(
                    onClick = {
                        runCatching {
                            context.startActivity(
                                android.content.Intent(
                                    android.content.Intent.ACTION_VIEW,
                                    android.net.Uri.parse("https://coc-lio-73de9.firebaseapp.com/privacy")
                                )
                            )
                        }
                    }
                ) {
                    Text("CHÍNH SÁCH QUYỀN RIÊNG TƯ", fontSize = 10.sp)
                }
                if (!readError.isNullOrBlank()) {
                    Text(
                        readError,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 10.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
                NeonButton(
                    text = if (isBusy) "ĐANG MỞ GOOGLE..." else "ĐĂNG NHẬP GOOGLE",
                    onClick = onGoogleSignIn,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isBusy
                )
                Text(
                    "Đăng nhập Google không tự bật dữ liệu hoạt động, thiết bị, phân tích, báo lỗi hoặc đồng bộ JSON. Các mục này là lựa chọn riêng và mặc định tắt.",
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
            "Lấy JSON trực tiếp từ trò chơi để COC-LIO đọc cấp độ, công trình đang nâng và thời gian còn lại.",
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
        Text("Nút mở trò chơi chỉ đưa bạn vào trò chơi. Bạn cần tự thao tác xuất và sao chép JSON trong trò chơi.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, lineHeight = 15.sp)
    }
}

@Composable
private fun OverviewScreen(
    snapshot: VillageSnapshot,
) {
    val context = LocalContext.current
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
                            contentDescription = AppLanguageSettings.translate(context, "Mã người chơi"),
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(10.dp)
                        )
                        Text(snapshot.tag.removePrefix("#"), color = MaterialTheme.colorScheme.onSurface, fontSize = 9.sp)
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
                                title = "LÀNG CHÍNH",
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
                        title = "LÀNG CHÍNH",
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
            item { SectionTitle("LÀNG CHÍNH • ĐANG NÂNG", homeRunning.size.toString()) }
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
                SectionTitle("LÀNG CHÍNH", homeExtras.sumOf { it.count }.toString())
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
                Text("${if (levelLabel == "TH") "Nhà chính" else "Nhà thợ"} ${level ?: "—"}", color = MaterialTheme.colorScheme.onSurface, fontSize = 19.sp, fontWeight = FontWeight.Black, maxLines = 1)
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
        item.gearUp -> item.level?.let { "CẤP $it • TẤN CÔNG NHANH" } ?: "TẤN CÔNG NHANH"
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
                        if (item.base == "builder") " • LÀNG ĐÊM" else " • LÀNG CHÍNH"
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
                        color = if (remaining == 0L) CocGold else MaterialTheme.colorScheme.onSurface,
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
private fun BuildingsAndArmyScreen(
    snapshot: VillageSnapshot,
    remoteCatalog: Map<Int, RemoteCatalogEntry>
) {
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
                                Text(if (base == "home") "LÀNG CHÍNH" else "CĂN CỨ THỢ XÂY", fontSize = 10.sp, maxLines = 1)
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
            remoteCatalog = remoteCatalog,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun CategoryScreen(
    snapshot: VillageSnapshot,
    tab: String,
    selectedBase: String,
    remoteCatalog: Map<Int, RemoteCatalogEntry>,
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
    fun buildingCategory(item: VillageItem): String =
        remoteCatalog[item.dataId]?.category
            ?.takeIf { it in setOf("defense", "resources", "traps", "other") }
            ?: when {
                item.sectionKey == "traps" || item.sectionKey == "traps2" -> "traps"
                item.dataId in resourceIds -> "resources"
                item.dataId in defenseIds -> "defense"
                else -> "other"
            }
    fun armyCategory(item: VillageItem): String =
        remoteCatalog[item.dataId]?.category
            ?.takeIf { it in setOf("troops", "heroes", "spells", "pets", "equipment", "other") }
            ?: when (item.sectionKey) {
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
            "troops" to "Quân đội",
            "heroes" to "Anh hùng",
            "spells" to "Thần chú",
            "pets" to "Linh thú",
            "equipment" to "Trang bị anh hùng",
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
        columns = GridCells.Adaptive(minSize = 82.dp),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
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
                val keyedRows = orderedRows.mapIndexed { index, row -> index to row }
                gridItems(
                    items = keyedRows,
                    key = { entry ->
                        entry.second.identity + "_" + (entry.second.timerSeconds ?: -1L) + "_" + entry.second.count + "_" + entry.first
                    },
                    span = { entry ->
                        if (entry.second.timerSeconds != null) GridItemSpan(maxLineSpan) else GridItemSpan(1)
                    }
                ) { entry ->
                    val item = entry.second
                    if (item.timerSeconds != null) {
                        UpgradeCard(item, snapshot.timestamp)
                    } else {
                        VillageGridItemCard(item, remoteCatalog[item.dataId])
                    }
                }
            }
        }
    }
}

@Composable
private fun VillageGridItemCard(
    item: VillageItem,
    remote: RemoteCatalogEntry? = null
) {
    NeonPanel(
        modifier = Modifier.fillMaxWidth(),
        glow = if (item.tab == "army") CocMagenta else CocCyan,
        innerPadding = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(122.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            if (remote?.imageUrl?.isNotBlank() == true) {
                AsyncImage(
                    model = remote.imageUrl,
                    contentDescription = remote.name,
                    modifier = Modifier.size(58.dp),
                    contentScale = ContentScale.Fit
                )
            } else {
                CategoryImage(item, 58)
            }
            Text(
                item.level?.let { level ->
                    remote?.maxLevel?.let { maxLevel -> "CẤP $level/$maxLevel" } ?: "CẤP $level"
                } ?: "—",
                color = MaterialTheme.colorScheme.tertiary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.14f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth().height(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (item.count > 1) {
                    Text(
                        "×" + item.count,
                        color = MaterialTheme.colorScheme.tertiary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.16f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 7.dp, vertical = 1.dp)
                    )
                }
                if (item.gearUp) {
                    Spacer(Modifier.width(5.dp))
                    Text("⚙", color = CocGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                item.weaponLevel?.let {
                    Spacer(Modifier.width(5.dp))
                    Text("V$it", color = MaterialTheme.colorScheme.primary, fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                }
            }
            val remoteDetails = buildList {
                remote?.description?.takeIf { it.isNotBlank() }?.let(::add)
                remote?.requirements?.takeIf { it.isNotEmpty() }?.let { add("Yêu cầu: " + it.joinToString(", ")) }
                remote?.upgradeCost?.takeIf { it.isNotBlank() }?.let { add("Chi phí: $it") }
                remote?.upgradeDurationSeconds?.let { add("Thời gian: " + formatDuration(it)) }
                remote?.details?.forEach(::add)
            }
            val detailText = (item.details + item.properties.map { (key, value) -> "$key: $value" } + remoteDetails).joinToString(" • ")
            if (detailText.isNotBlank()) {
                Text(
                    detailText,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 7.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
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
            CategoryImage(item, 44)
            
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
    pinnedTags: List<String>,
    activeTag: String,
    activeTimestamp: Long?,
    onSelect: (VillageSnapshot) -> Unit,
    onTogglePin: (String) -> Unit,
    onDelete: (String) -> Unit,
    onRestore: (String) -> Unit,
    onDeletePermanently: (String) -> Unit,
    onShare: (VillageSnapshot) -> Unit,
    onOpenSharedJsonFeed: () -> Unit
) {
    val context = LocalContext.current
    var selectedSection by remember { mutableStateOf("villages") }
    var pendingDelete by remember { mutableStateOf<VillageSnapshot?>(null) }
    var pendingPermanentDelete by remember { mutableStateOf<DeletedVillageRecord?>(null) }
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
                        label = { Text("CHIA SẺ", fontSize = 9.sp, maxLines = 1) }
                    )
                }
            }
        }

        if (selectedSection == "villages") {
            if (villages.isEmpty()) {
                item { InfoCard("Chưa có làng nào", "Sao chép JSON trong trò chơi rồi dán vào ứng dụng để thêm làng.") }
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
                                    contentDescription = AppLanguageSettings.translate(context, "Mã người chơi"),
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(11.dp)
                                )
                                Text(
                                    "${snapshot.tag.removePrefix("#")}  •  Nhà chính ${snapshot.townHallLevel ?: "—"}  •  Nhà thợ ${snapshot.builderHallLevel ?: "—"}  •  ${history.count { it.tag == snapshot.tag }} mốc  •  ${snapshot.activeUpgradesAt(nowSeconds).size} đang nâng",
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
                            Text("${snapshot.tag}  •  ${formatTimestamp(snapshot.timestamp)}  •  Nhà chính ${snapshot.townHallLevel ?: "—"}  •  Nhà thợ ${snapshot.builderHallLevel ?: "—"}  •  ${snapshot.items.size} mục", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
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

internal fun formatTimestamp(timestamp: Long): String {
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
