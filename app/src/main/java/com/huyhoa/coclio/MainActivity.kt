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
import com.huyhoa.coclio.data.CocUpgradeCatalog
import com.huyhoa.coclio.data.CocUpgradeEntry
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
import androidx.compose.material3.Slider
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

private data class LedOklab(
    val lightness: Double,
    val a: Double,
    val b: Double,
    val alpha: Double
)

private fun srgbToLinear(value: Double): Double =
    if (value <= 0.04045) value / 12.92
    else Math.pow((value + 0.055) / 1.055, 2.4)

private fun linearToSrgb(value: Double): Float {
    val clamped = value.coerceIn(0.0, 1.0)
    val encoded = if (clamped <= 0.0031308) {
        clamped * 12.92
    } else {
        1.055 * Math.pow(clamped, 1.0 / 2.4) - 0.055
    }
    return encoded.toFloat().coerceIn(0f, 1f)
}

private fun Color.toLedOklab(): LedOklab {
    val argb = toArgb()
    val red = srgbToLinear(((argb shr 16) and 0xFF) / 255.0)
    val green = srgbToLinear(((argb shr 8) and 0xFF) / 255.0)
    val blue = srgbToLinear((argb and 0xFF) / 255.0)

    val l = Math.cbrt(0.4122214708 * red + 0.5363325363 * green + 0.0514459929 * blue)
    val m = Math.cbrt(0.2119034982 * red + 0.6806995451 * green + 0.1073969566 * blue)
    val s = Math.cbrt(0.0883024619 * red + 0.2817188376 * green + 0.6299787005 * blue)

    return LedOklab(
        lightness = 0.2104542553 * l + 0.7936177850 * m - 0.0040720468 * s,
        a = 1.9779984951 * l - 2.4285922050 * m + 0.4505937099 * s,
        b = 0.0259040371 * l + 0.7827717662 * m - 0.8086757660 * s,
        alpha = alpha.toDouble()
    )
}

private fun interpolateLedColor(start: LedOklab, end: LedOklab, fraction: Float): Color {
    val amount = fraction.coerceIn(0f, 1f).toDouble()
    val lightness = start.lightness + (end.lightness - start.lightness) * amount
    val a = start.a + (end.a - start.a) * amount
    val b = start.b + (end.b - start.b) * amount

    val l = Math.pow(lightness + 0.3963377774 * a + 0.2158037573 * b, 3.0)
    val m = Math.pow(lightness - 0.1055613458 * a - 0.0638541728 * b, 3.0)
    val s = Math.pow(lightness - 0.0894841775 * a - 1.2914855480 * b, 3.0)

    val red = 4.0767416621 * l - 3.3077115913 * m + 0.2309699292 * s
    val green = -1.2684380046 * l + 2.6097574011 * m - 0.3413193965 * s
    val blue = -0.0041960863 * l - 0.7034186147 * m + 1.7076147010 * s
    val alpha = (start.alpha + (end.alpha - start.alpha) * amount).toFloat()

    return Color(
        red = linearToSrgb(red),
        green = linearToSrgb(green),
        blue = linearToSrgb(blue),
        alpha = alpha.coerceIn(0f, 1f)
    )
}

internal fun movingLedColors(
    palette: List<Color>,
    phase: Float,
    sampleCount: Int = 256
): List<Color> {
    if (palette.size < 2) return palette

    val cyclePalette = if (palette.first().toArgb() == palette.last().toArgb()) {
        palette
    } else {
        palette + palette.first()
    }
    val intervals = cyclePalette.lastIndex
    val convertedPalette = cyclePalette.map { it.toLedOklab() }
    val normalizedPhase = ((phase % 1f) + 1f) % 1f
    val samples = sampleCount.coerceIn(2, 512)

    return List(samples) { index ->
        val cyclePosition = ((index / samples.toFloat() - normalizedPhase + 1f) % 1f)
        val position = cyclePosition * intervals
        val startIndex = position.toInt().coerceIn(0, intervals - 1)
        val fraction = (position - startIndex).coerceIn(0f, 1f)
        interpolateLedColor(convertedPalette[startIndex], convertedPalette[startIndex + 1], fraction)
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

    val palette = led.palette.ifEmpty { listOf(CocCyan) }.map { it.copy(alpha = 1f) }
    val animatedColors = movingLedColors(palette, led.progress.value.rem(1f))
    val coreBrush = Brush.horizontalGradient(animatedColors)
    val glowColor = animatedColors.first().copy(alpha = 0.24f)
    val glowBlur = with(LocalDensity.current) {
        outlineWidthDp.coerceIn(0.1f, 2f).dp.toPx() * 1.8f
    }

    Text(
        text,
        modifier = modifier,
        style = TextStyle(
            brush = coreBrush,
            shadow = if (led.glowEnabled) {
                androidx.compose.ui.graphics.Shadow(
                    color = glowColor,
                    blurRadius = glowBlur
                )
            } else {
                null
            }
        ),
        fontSize = fontSize,
        fontWeight = FontWeight.Black,
        letterSpacing = letterSpacing,
        textAlign = textAlign,
        maxLines = 1
    )
}

@Composable
internal fun LedAdminAvatar(size: androidx.compose.ui.unit.Dp = 30.dp) {
    val led = LocalLedTheme.current
    val phase = led.progress.value.rem(1f)
    val palette = led.palette.ifEmpty { listOf(CocCyan) }.map { it.copy(alpha = 1f) }
    val animatedColors = movingLedColors(palette, phase)
    val ringBrush: Brush = if (led.enabled && palette.size >= 2) {
        Brush.sweepGradient(animatedColors)
    } else {
        SolidColor(MaterialTheme.colorScheme.outlineVariant)
    }
    val textBrush: Brush = if (led.enabled) {
        Brush.horizontalGradient(animatedColors)
    } else {
        SolidColor(MaterialTheme.colorScheme.onSurface)
    }

    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(Color.Black)
            .border(1.dp, ringBrush, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "AD",
            style = TextStyle(
                brush = textBrush,
                shadow = if (led.enabled && led.glowEnabled) {
                    androidx.compose.ui.graphics.Shadow(
                        color = animatedColors.first().copy(alpha = 0.24f),
                        blurRadius = with(LocalDensity.current) { 1.dp.toPx() }
                    )
                } else {
                    null
                }
            ),
            fontSize = (size.value * 0.34f).sp,
            fontWeight = FontWeight.Black,
            letterSpacing = (-0.4).sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            maxLines = 1
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
    val palette = led.palette.map { it.copy(alpha = 1f) }
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
        CommunityNotificationCleanup.disable(this)
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
    var notificationStyle by remember(activeProfile) {
        mutableStateOf(AppPreferences.notificationStyle(context, activeProfile))
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
        notificationsEnabled = granted
        UpgradeReminderScheduler.scheduleAll(context, history, activeProfile, granted)
        if (!granted) {
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
                notificationsEnabled = true
                UpgradeReminderScheduler.scheduleAll(context, history, activeProfile, true)
            }
        }
    }

    LaunchedEffect(activeProfile) {
        val notificationPermissionGranted = Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        AppPreferences.ensureNotificationPreferences(context, activeProfile, notificationPermissionGranted)
        notificationsEnabled = AppPreferences.notificationsEnabled(context, activeProfile)
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
                        isGoogleSignedIn = isGoogleSignedIn,
                        history = history,
                        receivedJsons = receivedSharedJsons,
                        onDeleteReceivedJson = { jsonId ->
                            if (ReceivedSharedJsonStore.deleteReceived(context, jsonId, activeProfile)) {
                                receivedSharedJsons = ReceivedSharedJsonStore.loadReceived(context, activeProfile)
                                scope.launch { snackbar.showAppSnackbar(context, "Đã xóa JSON nhận được khỏi danh sách chia sẻ. Bản trong lịch sử vẫn được giữ.") }
                            }
                        },
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
                    )
                    3 -> CommunityScreen(
                        isGoogleSignedIn = isGoogleSignedIn,
                        jsonOptions = history,
                        onShareJson = { snapshot -> shareVillageJsonToCommunity(snapshot) },
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
    val context = LocalContext.current
    var upgradeCatalog by remember(context) {
        mutableStateOf<Map<Int, CocUpgradeEntry>>(emptyMap())
    }
    LaunchedEffect(context) {
        upgradeCatalog = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            CocUpgradeCatalog.all(context)
        }
    }
    var selectedFilter by remember(snapshot.tag, snapshot.timestamp, tab, selectedBase) {
        mutableStateOf("all")
    }
    var selectedRarity by remember(snapshot.tag, snapshot.timestamp, tab, selectedBase) {
        mutableStateOf("all")
    }
    var selectedItem by remember(snapshot.tag, snapshot.timestamp, tab, selectedBase) {
        mutableStateOf<VillageItem?>(null)
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
    val blacksmithLevel = snapshot.items.firstOrNull {
        it.sectionKey == "buildings" && it.dataId == 1000070
    }?.level
    val hallLevel = if (selectedBase == "builder") snapshot.builderHallLevel else snapshot.townHallLevel
    val baseItems = snapshot.items.filter { it.base == selectedBase && it.tab == tab }

    fun buildingCategory(item: VillageItem): String =
        upgradeCatalog[item.dataId]?.category?.let {
            when (it) {
                "resource" -> "resources"
                "defense" -> "defense"
                "trap" -> "traps"
                "army", "research" -> "army"
                "wall" -> "walls"
                else -> null
            }
        }?.takeIf { it in setOf("defense", "resources", "traps", "army", "walls", "other") }
            ?: remoteCatalog[item.dataId]?.category
                ?.takeIf { it in setOf("defense", "resources", "traps", "army", "walls", "other") }
            ?: when {
                item.sectionKey == "traps" || item.sectionKey == "traps2" -> "traps"
                item.dataId in resourceIds -> "resources"
                item.dataId in defenseIds -> "defense"
                else -> "other"
            }

    fun armyCategory(item: VillageItem): String =
        when (upgradeCatalog[item.dataId]?.category) {
            "troop", "siege-machine" -> "troops"
            "hero" -> "heroes"
            "spell" -> "spells"
            "pet" -> "pets"
            "guardian" -> "guardians"
            "hero-equipment" -> "equipment"
            else -> remoteCatalog[item.dataId]?.category
                ?.takeIf { it in setOf("troops", "heroes", "spells", "pets", "guardians", "equipment", "other") }
                ?: when (item.sectionKey) {
                    "units", "units2", "siege_machines" -> "troops"
                    "heroes", "heroes2" -> "heroes"
                    "spells" -> "spells"
                    "pets" -> "pets"
                    "guardians" -> "guardians"
                    "equipment" -> "equipment"
                    else -> "other"
                }
        }

    val filterChoices = if (tab == "buildings") {
        listOf(
            "all" to "Tất cả",
            "defense" to "Phòng thủ",
            "resources" to "Tài nguyên",
            "army" to "Công trình quân đội",
            "walls" to "Tường",
            "traps" to "Bẫy",
            "other" to "Khác"
        ).filter { (key, _) -> key == "all" || baseItems.any { buildingCategory(it) == key } }
    } else {
        listOf(
            "all" to "Tất cả",
            "troops" to "Quân lính & máy công thành",
            "guardians" to "Hộ vệ",
            "heroes" to "Anh hùng",
            "spells" to "Thần chú",
            "pets" to "Linh thú",
            "equipment" to "Trang bị anh hùng",
            "other" to "Khác"
        ).filter { (key, _) -> key == "all" || baseItems.any { armyCategory(it) == key } }
    }

    fun rarityFor(item: VillageItem): String? =
        upgradeCatalog[item.dataId]?.rarity?.takeIf { it.isNotBlank() }
            ?: remoteCatalog[item.dataId]?.rarity?.takeIf { it.isNotBlank() }
            ?: if (item.sectionKey == "equipment") equipmentRarity(item.dataId) else null

    val categoryFiltered = when {
        selectedFilter == "all" -> baseItems
        tab == "buildings" -> baseItems.filter { buildingCategory(it) == selectedFilter }
        else -> baseItems.filter { armyCategory(it) == selectedFilter }
    }
    val rarityItems = categoryFiltered.filter { rarityFor(it) != null }
    val rarityChoices = listOf(
        "all" to "TẤT CẢ",
        "common" to "THƯỜNG",
        "rare" to "HIẾM",
        "epic" to "SỬ THI",
        "legendary" to "HUYỀN THOẠI"
    ).filter { (key, _) -> key == "all" || rarityItems.any { rarityFor(it) == key } }
    val filtered = if (tab == "army" && selectedRarity != "all") {
        categoryFiltered.filter { rarityFor(it) == selectedRarity }
    } else {
        categoryFiltered
    }

    fun groupKey(item: VillageItem): String {
        val category = if (tab == "buildings") buildingCategory(item) else armyCategory(item)
        if (category == "equipment") return "equipment"
        return category
    }

    fun groupTitle(key: String, rows: List<VillageItem>): String = when (key) {
        "defense" -> "CÔNG TRÌNH PHÒNG THỦ"
        "resources" -> "CÔNG TRÌNH TÀI NGUYÊN"
        "army" -> "CÔNG TRÌNH QUÂN ĐỘI & NGHIÊN CỨU"
        "walls" -> "TƯỜNG"
        "traps" -> "BẪY"
        "other" -> if (tab == "buildings") "CÔNG TRÌNH KHÁC" else "MỤC KHÁC"
        "troops" -> "QUÂN LÍNH & MÁY CÔNG THÀNH"
        "guardians" -> "HỘ VỆ"
        "heroes" -> "ANH HÙNG"
        "spells" -> "THẦN CHÚ"
        "pets" -> "LINH THÚ"
        "equipment" -> "TRANG BỊ ANH HÙNG • TẤT CẢ TƯỚNG"
        else -> rows.firstOrNull()?.sectionLabel?.uppercase().orEmpty()
    }

    fun groupGlow(key: String): Color = when {
        key == "defense" -> Color(0xFFFF626B)
        key == "resources" -> CocGold
        key == "traps" -> CocViolet
        key == "troops" -> CocMagenta
        key == "guardians" -> Color(0xFFFFB45C)
        key == "heroes" -> Color(0xFFFFB45C)
        key == "spells" -> Color(0xFF64B5F6)
        key == "pets" -> Color(0xFF6FE0B4)
        key == "equipment" -> CocViolet
        else -> CocCyan
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = if (tab == "army") 140.dp else 100.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (baseItems.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            filterChoices.forEach { (key, label) ->
                                FilterChip(
                                    selected = selectedFilter == key,
                                    onClick = {
                                        selectedFilter = key
                                        selectedRarity = "all"
                                    },
                                    label = { Text(label, fontSize = 10.sp) }
                                )
                            }
                        }
                        if (tab == "army" && rarityChoices.size > 1) {
                            Text(
                                "LỌC ĐỘ HIẾM TRANG BỊ",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                rarityChoices.forEach { (key, label) ->
                                    val count = if (key == "all") rarityItems.size
                                    else rarityItems.count { rarityFor(it) == key }
                                    FilterChip(
                                        selected = selectedRarity == key,
                                        onClick = { selectedRarity = key },
                                        label = { Text("$label ($count)", fontSize = 10.sp) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (baseItems.isEmpty() || filtered.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    InfoCard("Chưa có dữ liệu", "JSON không có mục thuộc nhóm này.")
                }
            } else {
                val grouped = filtered.groupBy(::groupKey)
                grouped.forEach { (key, rows) ->
                    item(key = "led_group_$key", span = { GridItemSpan(maxLineSpan) }) {
                        val ordered = rows.filter { it.timerSeconds == null }.sortedWith(
                            compareBy<VillageItem> { it.count > 1 }.thenBy { it.level ?: Int.MIN_VALUE }
                        ) + rows.filter { it.timerSeconds != null }
                            .sortedBy { it.timerSeconds ?: Long.MAX_VALUE }
                        NeonPanel(
                            modifier = Modifier.fillMaxWidth(),
                            glow = groupGlow(key),
                            innerPadding = 8.dp
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                                SectionTitle(groupTitle(key, rows), rows.sumOf { it.count }.toString())
                                ordered.filter { it.timerSeconds != null }.forEach { item ->
                                    UpgradeCard(item, snapshot.timestamp)
                                }
                                ordered.filter { it.timerSeconds == null }.chunked(2).forEach { rowItems ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        rowItems.forEach { item ->
                                            VillageGridItemCard(
                                                item = item,
                                                remote = remoteCatalog[item.dataId],
                                                upgrade = upgradeCatalog[item.dataId],
                                                catalog = upgradeCatalog,
                                                hallLevel = hallLevel,
                                                blacksmithLevel = blacksmithLevel,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                selectedItem = item
                                            }
                                        }
                                        if (rowItems.size == 1) Spacer(Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        selectedItem?.let { item ->
            VillageItemDetailDialog(
                item = item,
                remote = remoteCatalog[item.dataId],
                blacksmithLevel = blacksmithLevel,
                hallLevel = hallLevel,
                upgradeCatalog = upgradeCatalog
            ) {
                selectedItem = null
            }
        }
    }
}

@Composable
private fun VillageGridItemCard(
    item: VillageItem,
    remote: RemoteCatalogEntry?,
    upgrade: CocUpgradeEntry?,
    catalog: Map<Int, CocUpgradeEntry>,
    hallLevel: Int?,
    blacksmithLevel: Int?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val rarity = upgrade?.rarity?.takeIf { it.isNotBlank() }
        ?: remote?.rarity?.takeIf { it.isNotBlank() }
        ?: if (item.sectionKey == "equipment") equipmentRarity(item.dataId) else null
    val absoluteMax = upgrade?.maxLevel?.takeIf { it > 0 } ?: remote?.maxLevel
    val hallMax = upgrade?.let { CocUpgradeCatalog.maxLevelAtHall(it, hallLevel, catalog) }
    val blacksmithMax = upgrade?.let {
        CocUpgradeCatalog.maxEquipmentAtBlacksmithLevel(it, blacksmithLevel, catalog)
    }
    val currentLimit = when {
        item.sectionKey == "equipment" -> listOfNotNull(hallMax, blacksmithMax).minOrNull() ?: blacksmithMax
        else -> hallMax
    }
    val effectiveMax = currentLimit ?: absoluteMax
    val itemName = if (LocalAppLanguage.current == AppUiLanguage.ENGLISH) {
        remote?.name?.takeIf { it.isNotBlank() } ?: upgrade?.name?.takeIf { it.isNotBlank() } ?: item.name
    } else {
        item.name
    }
    val owner = upgrade?.hero?.takeIf { it.isNotBlank() }?.let(::equipmentHeroName)
        ?: if (item.sectionKey == "equipment") equipmentHero(item.dataId) else null
    val rarityTint = rarityColor(rarity) ?: MaterialTheme.colorScheme.outlineVariant

    Column(
        modifier = modifier
            .heightIn(min = 166.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 3.dp, vertical = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(66.dp)
                .clip(RoundedCornerShape(15.dp))
                .background(rarityTint.copy(alpha = 0.12f))
                .border(2.dp, rarityTint, RoundedCornerShape(15.dp))
                .padding(5.dp),
            contentAlignment = Alignment.Center
        ) {
            if (remote?.imageUrl?.isNotBlank() == true) {
                AsyncImage(
                    model = remote.imageUrl,
                    contentDescription = itemName,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            } else {
                CategoryImage(item, 54)
            }
        }
        Text(
            itemName,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        owner?.let {
            Text(
                "👑 $it",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 8.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (rarity != null) ItemRarityBadge(rarity)
        Text(
            item.level?.let { level ->
                "CẤP $level" + (effectiveMax?.let { " / $it" } ?: "")
            } ?: "CHƯA CÓ CẤP",
            color = MaterialTheme.colorScheme.tertiary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            maxLines = 1
        )
        Text(
            "${if (item.base == "builder") "NHÀ THỢ XÂY" else "NHÀ CHÍNH"}: ${hallLevel?.let { if (item.base == "builder") "BH$it" else "TH$it" } ?: "—"}",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
        if (currentLimit != null || absoluteMax != null) {
            Text(
                "GIỚI HẠN ĐANG MỞ: ${currentLimit ?: "Chưa rõ"}",
                color = if (currentLimit != null && absoluteMax != null && currentLimit < absoluteMax) CocGold else MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Text(
                "MAX TOÀN GAME: ${absoluteMax ?: "Chưa có dữ liệu"}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
        if (item.sectionKey == "equipment") {
            Text(
                "LÒ RÈN: ${blacksmithLevel?.let { "CẤP $it" } ?: "CHƯA CÓ DỮ LIỆU"}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (item.count > 1) Text("×${item.count}", color = MaterialTheme.colorScheme.tertiary, fontSize = 9.sp)
            if (item.gearUp) Text("⚙ ĐÃ GHÉP", color = CocGreen, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            item.weaponLevel?.let { Text("VŨ KHÍ $it", color = MaterialTheme.colorScheme.primary, fontSize = 8.sp) }
        }
        Text(
            "CHẠM ĐỂ XEM CHI TIẾT",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 7.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

private fun equipmentHeroName(heroId: String): String = when (heroId.lowercase()) {
    "barbarian-king" -> "Vua man di"
    "archer-queen" -> "Nữ hoàng cung thủ"
    "minion-prince" -> "Hoàng tử tiểu quỷ"
    "grand-warden" -> "Đại hộ vệ"
    "royal-champion" -> "Nhà vô địch Hoàng gia"
    "dragon-duke" -> "Công tước Rồng"
    else -> heroId.replace('-', ' ').replaceFirstChar { it.uppercase() }
}

private fun rarityColor(rarity: String?): Color? = when (rarity?.lowercase()) {
    "common" -> Color(0xFF55B9FF)
    "rare" -> Color(0xFF39D8D0)
    "epic" -> Color(0xFFB77AFF)
    "legendary" -> Color(0xFFFFC857)
    else -> null
}


private val EquipmentDescriptionsVi = mapOf(
    90000048 to "Triệu hồi một Đại khổng lồ chiến đấu bên cạnh Nữ hoàng Cung thủ.",
    90000002 to "Triệu hồi một nhóm Cung thủ tàng hình.",
    90000000 to "Triệu hồi một nhóm Man di được tăng sức mạnh cuồng nộ.",
    90000035 to "Tăng máu và sát thương tổng cộng 3 lần khi đồng minh bị hạ gục.",
    90000043 to "Gây sát thương và làm chậm mọi mục tiêu mà nó chạm vào.",
    90000008 to "Tạo ra động đất mạnh, phá hủy Tường và gây sát thương lên Công trình.",
    90000040 to "Định kỳ gây sát thương lên mọi mục tiêu xung quanh Nhà vô địch Hoàng gia.",
    90000016 to "Tạo chuỗi sát thương điện qua các công trình xung quanh mục tiêu của Công tước Rồng; sát thương giảm 20% sau mỗi lần lan.",
    90000004 to "Đại hộ vệ và tất cả quân đồng minh ở gần trở nên miễn nhiễm sát thương.",
    90000052 to "Từ từ hồi máu và bùng cháy gây sát thương khi Công tước Rồng bị hạ gục.",
    90000022 to "Ném một quả cầu lửa phát nổ lớn vào Phòng thủ gần nhất.",
    90000057 to "Phun lửa cực nóng gây sát thương lên kẻ địch và Công trình.",
    90000050 to "Bắn những mảnh băng về phía Phòng thủ gần nhất để đóng băng chúng.",
    90000015 to "Mũi tên băng làm chậm mục tiêu trúng đòn.",
    90000017 to "Bắn một mũi tên khổng lồ xuyên qua toàn bộ ngôi làng.",
    90000010 to "Vua Man di trở nên khổng lồ, gây sát thương diện rộng và giảm sát thương phải nhận.",
    90000012 to "Ban hiệu ứng Tăng tốc cho Nhà vô địch Hoàng gia.",
    90000020 to "Triệu hồi một nhóm Healer hỗ trợ hồi máu.",
    90000034 to "Hồi máu cho Đại hộ vệ và tất cả quân đồng minh ở gần.",
    90000042 to "Triệu hồi các tay sai bay hỗ trợ Hoàng tử Tiểu quỷ.",
    90000019 to "Giúp quân đồng minh ở gần vượt Tường, di chuyển nhanh hơn và nhận ít sát thương hơn.",
    90000009 to "Triệu hồi một nhóm Kỵ sĩ Heo.",
    90000003 to "Khiến Nữ hoàng Cung thủ tàng hình và tăng sức tấn công của cô ấy.",
    90000041 to "Triệu hồi các Khinh khí cầu Dung nham hỗ trợ tấn công.",
    90000005 to "Quân đồng minh ở gần được tăng thêm máu.",
    90000039 to "Triệu hồi các bản sao của Nữ hoàng Cung thủ.",
    90000044 to "Tạo lá chắn bảo vệ, tạm thời chặn gần như toàn bộ sát thương.",
    90000049 to "Triệu hồi các thiên thạch liên tục tấn công Phòng thủ gần nhất.",
    90000059 to "Sát thương cộng thêm của Mũi tên Monolith giảm khi triển khai quân vượt các mốc 180 và 250 đơn vị nhà ở.",
    90000047 to "Tăng tốc độ, tầm đánh và sức mạnh của một số đòn tấn công đầu tiên của Hoàng tử Tiểu quỷ.",
    90000061 to "Thu hồi Hoàng tử Tiểu quỷ và quân đồng minh ở gần về thanh triển khai để gọi ra vị trí khác, kèm thêm máu và Tăng tốc.",
    90000024 to "Quân đồng minh ở gần được tăng sát thương.",
    90000001 to "Ban hiệu ứng Cuồng nộ cho Vua Man di.",
    90000060 to "Kẻ địch đánh trúng Công tước Rồng sẽ phải nhận đòn phản công.",
    90000053 to "Lướt qua trung tâm ngôi làng, phun lửa và né tránh các đòn tấn công.",
    90000013 to "Một vài lần phóng lao tiếp theo sẽ mạnh hơn và có tầm bắn xa hơn.",
    90000007 to "Hồi một lượng máu lớn bổ sung cho Nhà vô địch Hoàng gia.",
    90000006 to "Ném khiên bật qua các công trình Phòng thủ và gây sát thương.",
    90000032 to "Triệu hồi rắn chiến đấu bên cạnh khi Anh hùng bị tấn công.",
    90000014 to "Bắn một quả cầu gai nảy giữa các Công trình.",
    90000051 to "Vượt qua Tường và tăng tốc độ di chuyển, tốc độ đánh sau khi được triển khai.",
    90000056 to "Phát ra sóng xung kích làm choáng và gây sát thương lên kẻ địch cùng Công trình xung quanh.",
    90000011 to "Mỗi lần Vua Man di tấn công, ông sẽ tự hồi máu."
)

private val CatalogDescriptionsVi = mapOf(
    4000041 to "Rồng con này hiếu động, thích phun lửa và gây hỗn loạn. Nhỏ bé nhưng có sức tàn phá đáng gờm.",
    4000033 to "Tấn công mục tiêu từ xa, phát sáng trong bóng tối và luôn sẵn sàng khuấy động chiến trường.",
    4000035 to "Ném bom gây sát thương mạnh lên mục tiêu và gây thêm sát thương lên Tường.",
    4000034 to "Người khổng lồ với cú đấm cực mạnh, có sức chịu đựng cao và chuyên áp sát đối thủ.",
    4000037 to "Khẩu đại bác cơ động trên bánh xe, có thể chuyển sang chế độ súng cối để tấn công linh hoạt.",
    4000038 to "Khinh khí cầu chở đầy Bộ xương; chúng nhảy xuống ngay cạnh quân địch để tấn công.",
    4000106 to "Pháp sư điện hỏa có thể thiêu đốt từng mục tiêu hoặc phóng điện đánh trúng nhiều kẻ địch.",
    4000070 to "Kỵ sĩ lướt trên không, vượt qua Tường và lao vào mục tiêu nhưng dễ gặp nguy hiểm khi va chạm.",
    4000042 to "Phù thủy bóng đêm triệu hồi đàn sinh vật bay để hỗ trợ tấn công.",
    4000036 to "Bộ giáp P.E.K.K.A mạnh mẽ tích tụ nguồn năng lượng bất ổn và giải phóng sức mạnh trong chiến đấu.",
    4000031 to "Man di giận dữ di chuyển nhanh và tấn công dữ dội ngay khi được triển khai.",
    4000032 to "Cung thủ lén lút ẩn mình, có thể tấn công mục tiêu trước khi bị phát hiện.",
    4000052 to "Khinh khí cầu chiến đấu bay vượt chướng ngại vật, thả bom trên đường và đưa quân trong Lâu đài Bang hội thẳng vào trung tâm căn cứ. Nó miễn nhiễm với hiệu ứng Thần chú nhưng có lớp vỏ dễ bị bắn hạ.",
    4000092 to "Máy khoan chiến đấu đào ngầm đến Công trình phòng thủ gần nhất, sau đó trồi lên làm choáng và phá hủy mục tiêu.",
    4000091 to "Máy phóng lửa bắn các Linh hỏa gây sát thương và để lại vùng cháy. Tầm bắn xa giúp nó tránh nhiều đòn phản công; máy miễn nhiễm với hiệu ứng Thần chú.",
    4000087 to "Máy phóng khúc gỗ lăn thẳng về phía trước, phá hủy công trình và mở đường đến Nhà chính. Máy miễn nhiễm với hiệu ứng Thần chú.",
    4000075 to "Doanh trại công thành được thả xuống để lần lượt thả P.E.K.K.A, Pháp sư và quân tiếp viện từ Lâu đài Bang hội.",
    4000188 to "Phương tiện bay thả quân từ trên cao, vượt qua Tường và gây sát thương từ xa. Thân gỗ không quá bền nhưng có khả năng di chuyển trên không và miễn nhiễm với Thần chú.",
    4000062 to "Khối đá bay khổng lồ nhắm vào Công trình phòng thủ, nghiền nát mục tiêu bằng đá tảng và tạo chấn động. Miễn nhiễm với hiệu ứng Thần chú.",
    4000135 to "Máy phóng quân đứng yên, bắn các thùng chứa quân thường và quân từ Lâu đài Bang hội để hỗ trợ đồng minh.",
    4000051 to "Xe phá tường lao thẳng qua chướng ngại vật, phá Tường và đưa quân trong Lâu đài Bang hội đến trung tâm căn cứ địch.",
    4000097 to "Hộ vệ tập sự hỗ trợ quân bằng Hào quang Sinh lực và tấn công từ xa bằng ná cao su.",
    4000001 to "Cung thủ tấn công từ xa, giữ khoảng cách an toàn và tập trung hạ gục từng mục tiêu.",
    4000023 to "Rồng con phun lửa tấn công mục tiêu trên mặt đất và trên không. Khi không có đơn vị bay thân thiện ở gần, nó nổi giận và được tăng sát thương cùng tốc độ đánh.",
    4000005 to "Khinh khí cầu do Bộ xương điều khiển, bỏ qua Tường và tập trung phá hủy Công trình phòng thủ.",
    4000000 to "Man di là chiến binh dũng mãnh, dùng sức mạnh cận chiến để tấn công và áp đảo căn cứ đối phương.",
    4000022 to "Người ném đá ném tảng đá lớn có thể nảy qua mục tiêu, gây sát thương nhiều lần.",
    4000065 to "Kỵ sĩ Rồng bay chuyên phá hủy Công trình phòng thủ bằng các đòn tấn công mạnh.",
    4000008 to "Rồng phun lửa từ trên không, gây sát thương diện rộng lên quân và công trình đối phương.",
    4000123 to "Tu sĩ Druid hồi máu cho quân đồng minh; khi biến hình thành Gấu, nó chuyển sang tấn công bằng sức mạnh cận chiến.",
    4000059 to "Rồng điện phóng tia sét có thể lan qua nhiều mục tiêu. Khi bị hạ, nó còn giáng sét xuống mặt đất.",
    4000095 to "Titan Điện dùng roi điện tấn công mục tiêu và giải phóng năng lượng gây sát thương lên các đơn vị xung quanh.",
    4000150 to "Lò lửa đứng yên, định kỳ sinh ra Bọ lửa nhắm vào Công trình và để lại vệt lửa trên đường di chuyển.",
    4000003 to "Người khổng lồ có lượng máu cao, chuyên hấp thụ sát thương và ưu tiên tấn công Công trình phòng thủ.",
    4000002 to "Goblin cực nhanh và ưu tiên nhắm vào tài nguyên, giúp cướp tài nguyên hiệu quả.",
    4000013 to "Golem có sức chịu đựng lớn. Khi bị phá hủy, nó phát nổ và tách thành các Golem con yếu hơn.",
    4000082 to "Thợ săn Anh hùng chuyên nhắm vào Anh hùng đối phương. Cô có thể nhảy qua Tường và dùng vũ khí độc làm chậm mục tiêu.",
    4000007 to "Nữ tu bay hỗ trợ hồi máu cho quân đồng minh ở gần. Healer không trực tiếp tấn công quân hoặc công trình đối phương.",
    4000011 to "Kỵ sĩ Heo có thể nhảy qua Tường và nhanh chóng áp sát các mục tiêu phía sau hệ thống phòng thủ.",
    4000058 to "Golem Băng làm chậm và đóng băng mục tiêu khi tiếp xúc; khi bị hạ, nó tạo hiệu ứng đóng băng khu vực xung quanh.",
    4000017 to "Chó săn Dung nham là đơn vị bay có sức chịu đựng cao, ưu tiên tấn công Phòng không và tách thành các Chó con Dung nham khi bị hạ.",
    4000177 to "Người khổng lồ Thiên thạch ném đá từ xa. Khi tách ra, các Meteormite tấn công mục tiêu gần đó hoặc hợp lại thành Người khổng lồ Thiên thạch.",
    4000024 to "Thợ mỏ đào ngầm, đi xuyên qua Tường và tấn công các Công trình mục tiêu.",
    4000010 to "Minion là đơn vị bay nhanh, tấn công từ trên không và không bị Mìn tìm kiếm trên không phát hiện.",
    4000009 to "P.E.K.K.A mặc bộ giáp cực kỳ bền chắc, gây sát thương cận chiến rất lớn.",
    4000110 to "Kỵ sĩ Rễ cây cưỡi rễ cây khổng lồ, phá Tường và nghiền nát Công trình phòng thủ.",
    4000109 to "Phù thủy Phế tích dùng cỗ máy Debrisbuster 5000 hút đống đổ nát rồi tạo ra các Kỵ sĩ Phế tích để chiến đấu.",
    4000132 to "Chiến binh Ném lao có tầm đánh xa, phóng lao mạnh vào quân địch và Công trình phòng thủ.",
    4000012 to "Nữ chiến binh dùng rìu hai lưỡi, xoay người tấn công nhiều quân hoặc công trình ở gần.",
    4000004 to "Pháo thủ phá Tường lao đến Tường và phát nổ để mở đường cho quân bộ binh.",
    4000015 to "Phù thủy liên tục triệu hồi Bộ xương từ những chiến binh đã ngã xuống để hỗ trợ tấn công.",
    4000006 to "Pháp sư phóng những luồng phép thuật mạnh, gây sát thương diện rộng lên mục tiêu trên mặt đất và trên không.",
    4000053 to "Yeti có sức tấn công mạnh và mang theo các Yetimite. Khi bị thương, các Yetimite sẽ trở nên hung hãn.",
    28000005 to "Trực thăng chiến đấu giúp Chủ xây dựng bay qua Tường, tấn công từ xa bằng đại bác hoặc lao đến gần đối thủ bằng kỹ năng Bão bom.",
    28000003 to "Cỗ máy chiến đấu do Chủ xây dựng chế tạo, dùng nắm đấm sắt và Búa điện để đập tan công trình đối phương.",
    28000001 to "Nữ hoàng Cung thủ là Anh hùng tầm xa, dùng nỏ mạnh để tiêu diệt mục tiêu và bảo vệ căn cứ khi phòng thủ.",
    28000000 to "Vua Man di là Anh hùng cận chiến có lượng máu lớn, dùng kiếm khổng lồ nghiền nát kẻ địch và bảo vệ lãnh thổ.",
    28000007 to "Công tước Rồng là Anh hùng bay có sức chịu đựng cao. Khi tách khỏi các đơn vị bay đồng minh, ông trở nên cuồng nộ, tăng sức mạnh tấn công và giảm sát thương từ bẫy.",
    28000002 to "Đại hộ vệ hỗ trợ quân bằng phép thuật và cây trượng, có thể đi bộ hoặc bay qua Tường; khi phòng thủ, ông bảo vệ khu vực quanh biểu ngữ Anh hùng.",
    28000006 to "Hoàng tử Tiểu quỷ bay trên chiến trường và dùng ma thuật hắc ám tấn công. Khi phòng thủ, anh chỉ bị tấn công bởi các đơn vị tầm xa và đơn vị bay.",
    28000004 to "Nhà vô địch Hoàng gia chuyên áp sát và tiêu diệt Công trình phòng thủ bằng cây lao và chiếc khiên.",
    73000011 to "Khối thạch nhầy giận dữ hỗ trợ Anh hùng nhắm vào Công trình phòng thủ, sau đó tách ra để tiếp tục chiến đấu độc lập.",
    73000008 to "Diggy đào xuống lòng đất để ẩn nấp, sau đó trồi lên tấn công công trình và hỗ trợ Anh hùng. Khi Anh hùng bị hạ, Diggy tìm mục tiêu mới.",
    73000002 to "Cú điện tấn công Công trình phòng thủ từ xa và có thể phóng sét lan qua các mục tiêu.",
    73000009 to "Frosty phun hơi lạnh làm chậm kẻ địch và triệu hồi các Bọ băng để hỗ trợ Anh hùng.",
    73000017 to "Quạ tham lam nhắm vào Công trình tài nguyên và gây thêm sát thương lên các mục tiêu này.",
    73000000 to "L.A.S.S.I là thú cưng trung thành, nhanh chóng lao đến tấn công kẻ địch ở gần Anh hùng; không thể tấn công mục tiêu bay.",
    73000001 to "Yak hùng mạnh dùng đầu húc phá Tường và nổi giận khi Anh hùng đồng hành bị hạ.",
    73000004 to "Phượng hoàng bắt đầu trận đấu dưới dạng trứng. Khi Anh hùng sắp bị hạ, nó nở ra, tạm thời bảo vệ Anh hùng và cùng chiến đấu.",
    73000007 to "Thằn lằn Độc phun độc làm chậm kẻ địch và hỗ trợ Anh hùng đối phó với quân cùng Anh hùng đối phương.",
    73000016 to "Sneezy đứng phía sau Anh hùng, hắt ra các khối chất nhầy bay nhắm vào Công trình; khi Anh hùng bị hạ, nó tham gia tấn công.",
    73000010 to "Cáo Linh hồn là sinh vật nhút nhát nhưng chiến đấu quyết liệt bên cạnh Anh hùng và giúp Anh hùng tàng hình.",
    73000003 to "Kỳ lân hồi máu cho Anh hùng đồng hành, giúp duy trì sức chiến đấu trong suốt trận chiến.",
    26000123 to "Thần chú Giận dữ khiến quân trong phạm vi ảnh hưởng trở nên hung hãn và tạm thời ưu tiên tấn công Công trình phòng thủ.",
    26000028 to "Triệu hồi một đàn Dơi tại vị trí bất kỳ trên chiến trường. Dơi không kích hoạt Bẫy.",
    26000016 to "Tạo ra một vùng triệu hồi các bản sao quân lính đi vào khu vực. Bản sao chỉ tồn tại trong thời gian giới hạn và có tổng sức chứa tối đa.",
    26000010 to "Gây động đất làm suy yếu Tường và Công trình. Sát thương lặp lại giảm dần trên công trình nhưng tăng dần trên Tường.",
    26000005 to "Tạo luồng băng giá đóng băng quân địch và vô hiệu hóa Công trình phòng thủ trong phạm vi ảnh hưởng.",
    26000011 to "Tăng mạnh tốc độ di chuyển cho quân, đặc biệt hữu ích với đơn vị chậm. Thần chú Tăng tốc không tăng sát thương như Thần chú Cuồng nộ.",
    26000001 to "Tạo vòng hồi máu giúp quân đồng minh được hồi phục khi đứng bên trong phạm vi ảnh hưởng.",
    26000109 to "Bao bọc quân mặt đất và trên không trong lớp băng bảo vệ, chặn phần lớn sát thương nhưng khiến chúng không thể di chuyển hoặc tấn công.",
    26000035 to "Khiến hầu hết mục tiêu trong phạm vi trở nên vô hình và không thể bị nhắm chọn, ngoại trừ Tường và Máy công thành.",
    26000003 to "Tạo lối đi vượt qua Tường đối phương, giúp quân đồng minh nhảy qua các đoạn Tường bị ảnh hưởng.",
    26000000 to "Giáng sét xuống khu vực chỉ định, gây sát thương và làm choáng các đơn vị cùng Công trình trong phạm vi nhỏ.",
    26000070 to "Dùng rễ cây khổng lồ trói các Công trình gần đó, ngăn Phòng thủ tấn công; quân đồng minh sẽ bỏ qua các Công trình bị trói.",
    26000009 to "Tạo đám mây độc gây sát thương tăng dần, đồng thời làm chậm tốc độ di chuyển và tốc độ tấn công của quân phòng thủ ở trong phạm vi.",
    26000002 to "Tạo vòng Cuồng nộ giúp quân đồng minh tăng tốc độ và sức mạnh tấn công khi đứng bên trong.",
    26000053 to "Thu hồi quân lính, Anh hùng và Linh thú trong phạm vi về thanh triển khai để có thể gọi chúng ra lại ở vị trí khác.",
    26000098 to "Hồi sinh một Anh hùng đã bị hạ gục trong phạm vi và đưa Anh hùng trở lại trận chiến với một phần máu.",
    26000017 to "Triệu hồi Bộ xương tại vị trí bất kỳ trên chiến trường. Bộ xương không kích hoạt Bẫy.",
    26000120 to "Đặt Totem để đánh lạc hướng Phòng thủ mặt đất và trên không. Kẻ địch gần đó sẽ tập trung tấn công Totem."
)

private val EquipmentStatLabelsVi = mapOf(
    "build cost" to "Chi phí xây dựng",
    "build time" to "Thời gian xây dựng",
    "production rate" to "Tốc độ sản xuất",
    "storage capacity" to "Sức chứa kho",
    "troop capacity" to "Sức chứa quân đội",
    "spell capacity" to "Sức chứa Thần chú",
    "max buildings" to "Số công trình tối đa",
    "max traps" to "Số bẫy tối đa",
    "hero slots" to "Số vị trí Anh hùng",
    "unlocked hero" to "Anh hùng được mở khóa",
    "unlocked pet" to "Linh thú được mở khóa",
    "unlocked siege machine" to "Máy công thành được mở khóa",
    "unlocked unit" to "Đơn vị được mở khóa",
    "equipment unlocked" to "Trang bị được mở khóa",
    "ore capacity shiny ore" to "Sức chứa Quặng sáng",
    "ore capacity glowy ore" to "Sức chứa Quặng phát sáng",
    "ore capacity starry ore" to "Sức chứa Quặng sao",
    "hero level caps barbarian king" to "Giới hạn cấp Vua man di",
    "hero level caps archer queen" to "Giới hạn cấp Nữ hoàng cung thủ",
    "hero level caps minion prince" to "Giới hạn cấp Hoàng tử tiểu quỷ",
    "hero level caps grand warden" to "Giới hạn cấp Đại hộ vệ",
    "hero level caps royal champion" to "Giới hạn cấp Nhà vô địch Hoàng gia",
    "hero level caps dragon duke" to "Giới hạn cấp Công tước Rồng",
    "hitpoint increase" to "Tăng máu",
    "hp recovery increase" to "Tăng hồi phục HP",
    "summoned barbarians" to "Số Man di được triệu hồi",
    "barbarian damage increase" to "Tăng sát thương Man di",
    "barbarian speed increase" to "Tăng tốc độ Man di",
    "invisibility duration" to "Thời gian tàng hình",
    "summoned archers" to "Số Cung thủ được triệu hồi",
    "archer invisibility duration" to "Thời gian tàng hình của Cung thủ",
    "summoned healers" to "Số Healer được triệu hồi",
    "healer level" to "Cấp Healer",
    "summoned hog riders" to "Số Kỵ sĩ Heo được triệu hồi",
    "hog rider level" to "Cấp Kỵ sĩ Heo",
    "henchmen level" to "Cấp tay sai",
    "henchmen count" to "Số tay sai",
    "lavaloon level" to "Cấp Khinh khí cầu Dung nham",
    "summoned lavaloons" to "Số Khinh khí cầu Dung nham được triệu hồi",
    "giant giant level" to "Cấp Đại khổng lồ",
    "max health increase" to "Tăng máu tối đa",
    "max damage increase" to "Tăng sát thương tối đa",
    "aura dps" to "Sát thương mỗi giây của hào quang",
    "aura damage per hit" to "Sát thương hào quang mỗi đòn",
    "aura attack speed" to "Tốc độ đánh của hào quang",
    "aura radius" to "Bán kính hào quang",
    "damage radius" to "Bán kính sát thương",
    "projectile damage" to "Sát thương đạn",
    "projectile damage per target" to "Sát thương đạn trên mỗi mục tiêu",
    "damage per shot increase" to "Tăng sát thương mỗi phát bắn",
    "number of targets" to "Số mục tiêu",
    "number of attacks" to "Số đòn đánh",
    "slow down duration" to "Thời gian làm chậm",
    "slow down percent" to "Tỷ lệ làm chậm",
    "slow down" to "Mức làm chậm",
    "damage reduction increase" to "Tăng giảm sát thương",
    "damage increase percent" to "Tỷ lệ tăng sát thương",
    "damage increase" to "Tăng sát thương",
    "damage on defeat" to "Sát thương khi bị hạ gục",
    "ability total damage" to "Tổng sát thương kỹ năng",
    "ability attack speed increase" to "Tăng tốc độ đánh của kỹ năng",
    "ability radius" to "Bán kính kỹ năng",
    "ability type" to "Loại kỹ năng",
    "speed increase" to "Tăng tốc độ",
    "attack range" to "Tầm tấn công",
    "damage area" to "Phạm vi sát thương",
    "damage per second increase" to "Tăng sát thương mỗi giây",
    "projectile damage increase" to "Tăng sát thương đạn",
    "clone duration" to "Thời gian tồn tại bản sao",
    "clones" to "Số bản sao",
    "clone dps" to "Sát thương mỗi giây của bản sao",
    "clone health" to "Máu bản sao",
    "cooldown time" to "Thời gian hồi chiêu",
    "extra damage below180" to "Sát thương cộng thêm khi dưới 180 đơn vị nhà ở",
    "extra damage181 to250" to "Sát thương cộng thêm từ 181 đến 250 đơn vị nhà ở",
    "extra damage251 plus" to "Sát thương cộng thêm khi trên 250 đơn vị nhà ở",
    "portal tile radius" to "Bán kính cổng (ô)",
    "healing on redeploy" to "Lượng hồi máu khi triển khai lại",
    "haste on redeploy" to "Tốc độ khi triển khai lại",
    "recall capacity" to "Sức chứa khi thu hồi",
    "heal per counter" to "Lượng hồi máu mỗi lần phản công",
    "counter damage" to "Sát thương phản công",
    "max summoned snakes" to "Số rắn tối đa được triệu hồi",
    "snake level" to "Cấp rắn",
    "building damage percent" to "Tỷ lệ sát thương lên Công trình",
    "troop damage percent" to "Tỷ lệ sát thương lên quân",
    "max hp increase" to "Tăng HP tối đa",
    "hp increase percent" to "Tỷ lệ tăng HP",
    "stun duration" to "Thời gian choáng",
    "heal per hit" to "Lượng hồi máu mỗi đòn đánh",
    "invisibility" to "Thời gian tàng hình",
    "chain damage decay" to "Mức giảm sát thương dây chuyền",
    "chain distance" to "Khoảng cách dây chuyền",
    "max counter range" to "Tầm phản công tối đa",
    "counter cooldown" to "Thời gian hồi phản công",
    "barbarian rage duration" to "Thời gian cuồng nộ của Man di",
    "damage per second" to "Sát thương mỗi giây",
    "damage per hit" to "Sát thương mỗi đòn",
    "damage per attack" to "Sát thương mỗi lần tấn công",
    "damage per shot" to "Sát thương mỗi phát bắn",
    "hitpoints" to "Máu",
    "hit points" to "Máu",
    "health" to "Máu",
    "housing space" to "Không gian nhà",
    "movement speed" to "Tốc độ di chuyển",
    "attack speed" to "Tốc độ tấn công",
    "attack rate" to "Tốc độ tấn công",
    "range" to "Tầm đánh",
    "attack range" to "Tầm tấn công",
    "preferred target" to "Mục tiêu ưu tiên",
    "target type" to "Loại mục tiêu",
    "damage type" to "Loại sát thương",
    "attack type" to "Kiểu tấn công",
    "training time" to "Thời gian huấn luyện",
    "training cost" to "Chi phí huấn luyện",
    "research cost" to "Chi phí nghiên cứu",
    "research time" to "Thời gian nghiên cứu",
    "upgrade cost" to "Chi phí nâng cấp",
    "upgrade time" to "Thời gian nâng cấp",
    "units per camp" to "Số đơn vị mỗi trại",
    "troop housing space" to "Không gian nhà của quân",
    "healing per second" to "Hồi máu mỗi giây",
    "healing per hit" to "Hồi máu mỗi đòn",
    "splash radius" to "Bán kính sát thương diện rộng",
    "attack speed increase" to "Tăng tốc độ tấn công",
    "movement speed increase" to "Tăng tốc độ di chuyển",
    "ability level" to "Cấp kỹ năng",
    "special ability" to "Kỹ năng đặc biệt",
    "builder hall level required" to "Yêu cầu Nhà thợ xây cấp",
    "town hall level required" to "Yêu cầu Nhà chính cấp",
    "star lab required" to "Yêu cầu Phòng thí nghiệm sao cấp",
    "laboratory level required" to "Yêu cầu Phòng thí nghiệm cấp",
    "spell duration" to "Thời gian hiệu lực thần chú",
    "spell radius" to "Bán kính thần chú",
    "spell housing space" to "Không gian nhà thần chú",
    "summoned units" to "Số đơn vị được triệu hồi",
    "summoned unit level" to "Cấp đơn vị được triệu hồi",
    "number summoned" to "Số lượng triệu hồi",
    "attack speed bonus" to "Tốc độ tấn công cộng thêm",
    "damage bonus" to "Sát thương cộng thêm",
    "health bonus" to "Máu cộng thêm",
    "duration" to "Thời gian hiệu lực",
    "radius" to "Bán kính",
    "speed" to "Tốc độ",
    "dps" to "Sát thương mỗi giây",
    "hp" to "Máu",
    "death damage" to "Sát thương khi bị tiêu diệt",
    "damage vs walls" to "Sát thương lên Tường",
    "rage duration seconds" to "Thời gian Cuồng nộ (giây)",
    "cloak duration seconds" to "Thời gian tàng hình (giây)",
    "poison max dps" to "Sát thương độc tối đa mỗi giây",
    "power shot attacks" to "Số lần bắn cường lực",
    "power shot damage per hit" to "Sát thương mỗi phát bắn cường lực",
    "boxer block duration seconds" to "Thời gian đỡ đòn (giây)",
    "power punch damage" to "Sát thương cú đấm mạnh",
    "spell type" to "Loại Thần chú",
    "healing per pulse" to "Lượng hồi máu mỗi nhịp",
    "healing per second on heroes" to "Lượng hồi máu mỗi giây lên Anh hùng",
    "healing per pulse on heroes" to "Lượng hồi máu mỗi nhịp lên Anh hùng",
    "tantrum damage bonus" to "Sát thương tăng thêm khi nổi giận",
    "fiery sneeze damage min" to "Sát thương hắt lửa tối thiểu",
    "fiery sneeze damage max" to "Sát thương hắt lửa tối đa",
    "bouncing bomb damage" to "Sát thương bom nảy",
    "bouncing bomb damage vs walls" to "Sát thương bom nảy lên Tường",
    "frostmites per summon" to "Số Bọ băng mỗi lần triệu hồi",
    "max frostmites" to "Số Bọ băng tối đa",
    "poison speed decrease percent" to "Phần trăm giảm tốc độ do độc",
    "poison attack rate decrease percent" to "Phần trăm giảm tốc độ tấn công do độc",
    "lifetime" to "Thời gian tồn tại",
    "golemites spawned" to "Số Golem con được tạo ra",
    "mortar dps" to "Sát thương mỗi giây của súng cối",
    "mortar damage per shot" to "Sát thương mỗi phát bắn của súng cối",
    "bat summon cooldown" to "Thời gian hồi triệu hồi Dơi",
    "bats per summon" to "Số Dơi mỗi lần triệu hồi",
    "bats max" to "Số Dơi tối đa",
    "bat swarm count" to "Số đàn Dơi",
    "total healing" to "Tổng lượng hồi máu",
    "total healing on heroes" to "Tổng lượng hồi máu lên Anh hùng",
    "max damage per second" to "Sát thương tối đa mỗi giây",
    "speed decrease" to "Mức giảm tốc độ",
    "attack rate decrease" to "Mức giảm tốc độ tấn công",
    "skeleton bomb damage" to "Sát thương bom Bộ xương",
    "skeleton bomb skeletons" to "Số Bộ xương từ bom",
    "brainwash duration" to "Thời gian điều khiển",
    "dps on resource buildings" to "Sát thương mỗi giây lên Công trình tài nguyên",
    "revive duration" to "Thời gian hồi sinh",
    "cloned capacity" to "Sức chứa bản sao",
    "death freeze time on offense" to "Thời gian đóng băng khi bị hạ (tấn công)",
    "death freeze time on defense" to "Thời gian đóng băng khi bị hạ (phòng thủ)",
    "overcharge damage" to "Sát thương quá tải",
    "barrel count" to "Số thùng",
    "minions" to "Số Tay sai",
    "balloons" to "Số Khinh khí cầu",
    "baby dragons" to "Số Rồng con",
    "bats generated" to "Số Dơi được tạo ra",
    "skeletons generated" to "Số Bộ xương được tạo ra",
    "pups on offense" to "Số Chó con khi tấn công",
    "pups on defense" to "Số Chó con khi phòng thủ",
    "skeletons per summon" to "Số Bộ xương mỗi lần triệu hồi",
    "max skeletons" to "Số Bộ xương tối đa",
    "skeleton level" to "Cấp Bộ xương",
    "recalled capacity" to "Sức chứa thu hồi",
    "damage when destroyed" to "Sát thương khi bị phá hủy",
    "point blank damage" to "Sát thương cận chiến",
    "pekkas spawned" to "Số P.E.K.K.A được triệu hồi",
    "wizards spawned" to "Số Pháp sư được triệu hồi",
    "damage when destroyed hitbox1" to "Sát thương khi bị phá hủy (vùng 1)",
    "damage when destroyed hitbox2" to "Sát thương khi bị phá hủy (vùng 2)",
    "flame max dps" to "Sát thương lửa tối đa mỗi giây",
    "hero heal percent" to "Tỷ lệ hồi máu cho Anh hùng",
    "inferno initial dps" to "Sát thương ban đầu mỗi giây của Hỏa ngục",
    "inferno dps after1_5s" to "Sát thương Hỏa ngục mỗi giây sau 1,5 giây",
    "inferno dps after3_0s" to "Sát thương Hỏa ngục mỗi giây sau 3 giây",
    "electro dps" to "Sát thương điện mỗi giây",
    "electro damage per shot" to "Sát thương điện mỗi phát",
    "troop level" to "Cấp quân",
    "barbarians" to "Số Man di",
    "archers" to "Số Cung thủ",
    "giants" to "Số Người khổng lồ",
    "wall breakers" to "Số Pháo thủ phá Tường",
    "anger duration" to "Thời gian giận dữ",
    "totem hitpoints" to "Máu của Totem",
    "aura hp increase" to "Tăng máu từ hào quang",
    "firemites spawned" to "Số Bọ lửa được sinh ra",
    "poison speed decrease" to "Mức giảm tốc độ do độc",
    "poison attack rate decrease" to "Mức giảm tốc độ tấn công do độc",
    "dps on heroes" to "Sát thương mỗi giây lên Anh hùng",
    "ruin knight level" to "Cấp Kỵ sĩ Phế tích",
    "maximum ruin knights summoned" to "Số Kỵ sĩ Phế tích tối đa được triệu hồi",
    "redeploy speed increase" to "Tăng tốc độ khi triển khai lại",
    "troop type" to "Loại quân",
    "targets" to "Mục tiêu"
)

private fun localizedCatalogField(label: String, value: String, isEnglish: Boolean): Pair<String, String> {
    if (isEnglish) return label to value
    val normalizedLabel = label.trim()
        .replace(Regex("([a-z0-9])([A-Z])")) { "${it.groupValues[1]} ${it.groupValues[2]}" }
        .lowercase()
        .replace('_', ' ')
    val translatedLabel = EquipmentStatLabelsVi[normalizedLabel]
        ?: normalizedLabel.replaceFirstChar { it.uppercase() }
    val normalizedValue = value.trim().lowercase()
    val translatedValue = when (normalizedValue) {
        "ground" -> "Mặt đất"
        "air" -> "Trên không"
        "ground & air", "ground and air" -> "Mặt đất và trên không"
        "ground only" -> "Chỉ mặt đất"
        "air only" -> "Chỉ trên không"
        "active" -> "Chủ động"
        "passive" -> "Bị động"
        "single target" -> "Đơn mục tiêu"
        "single target (ground only)" -> "Đơn mục tiêu (mặt đất)"
        "splash" -> "Sát thương diện rộng"
        "chain" -> "Sát thương dây chuyền"
        "chain lightning" -> "Sét dây chuyền"
        "area splash" -> "Sát thương diện rộng"
        "area splash (ground only)" -> "Sát thương diện rộng (mặt đất)"
        "area splash 1 and 3 tile radius (ground only)" -> "Sát thương diện rộng bán kính 1 và 3 ô (mặt đất)"
        "defenses" -> "Công trình phòng thủ"
        "defense" -> "Công trình phòng thủ"
        "walls" -> "Tường"
        "wall" -> "Tường"
        "walls (damage x4)" -> "Tường (sát thương ×4)"
        "resources" -> "Tài nguyên"
        "any" -> "Bất kỳ mục tiêu nào"
        "none" -> "Không có"
        "yes" -> "Có"
        "no" -> "Không"
        "hero's target" -> "Mục tiêu của Anh hùng"
        "within 2.5 tiles of hero" -> "Trong phạm vi 2,5 ô quanh Anh hùng"
        "within 4.5 tiles of hero" -> "Trong phạm vi 4,5 ô quanh Anh hùng"
        "within 7 tiles of hero" -> "Trong phạm vi 7 ô quanh Anh hùng"
        "heroes and troops" -> "Anh hùng và quân lính"
        "heroes" -> "Anh hùng"
        "hero" -> "Anh hùng"
        "ranged (ground & air)" -> "Tầm xa (mặt đất và trên không)"
        "melee (ground only)" -> "Cận chiến (mặt đất)"
        "ranged single target (any target)" -> "Tầm xa, đơn mục tiêu (mọi mục tiêu)"
        "melee (with nearby air units)/ranged (otherwise); (ground & air)" -> "Cận chiến khi có đơn vị bay gần, nếu không thì tầm xa (mặt đất và trên không)"
        "dark" -> "Hắc ám"
        "regular" -> "Thông thường"
        else -> localizedCatalogText(value, isEnglish)
    }
    return translatedLabel to translatedValue
}

private fun localizedCatalogText(value: String, isEnglish: Boolean): String {
    if (isEnglish) return value
    Regex("(?i)^Blacksmith level (\\d+)$").matchEntire(value.trim())?.let {
        return "Lò rèn cấp ${it.groupValues[1]}"
    }
    Regex("(?i)^Buy in (.+?) event for ([\\d,]+) (.+)$").matchEntire(value.trim())?.let {
        val medals = it.groupValues[3].replace("Medals", "huy hiệu", ignoreCase = true)
        return "Nhận trong sự kiện ${it.groupValues[1]} với ${it.groupValues[2]} $medals"
    }
    Regex("(?i)^Purchasable from the Trader for ([\\d,]+) Gems$").matchEntire(value.trim())?.let {
        return "Có thể mua tại Thương nhân với ${it.groupValues[1]} Ngọc"
    }
    return when (value.trim()) {
        "Available by default" -> "Có sẵn mặc định"
        "Unlocked by default" -> "Được mở khóa mặc định"
        "Available in the Shop" -> "Có trong Cửa hàng"
        "Available from the Trader" -> "Có thể nhận từ Thương nhân"
        else -> value
            .replace("Dark Elixir", "Dầu đen", ignoreCase = true)
            .replace("Elixir", "Dầu tiên", ignoreCase = true)
            .replace("Gold", "Vàng", ignoreCase = true)
            .replace("Gems", "Ngọc", ignoreCase = true)
            .replace("Shiny Ore", "Quặng sáng", ignoreCase = true)
            .replace("Glowy Ore", "Quặng phát sáng", ignoreCase = true)
            .replace("Starry Ore", "Quặng sao", ignoreCase = true)
    }
}

private val EpicEquipmentIds = setOf(
    90000010, 90000013, 90000014, 90000015, 90000019, 90000022,
    90000032, 90000035, 90000039, 90000040, 90000041, 90000048,
    90000049, 90000050, 90000051, 90000053, 90000059, 90000060,
    90000061
)

private val CommonEquipmentIds = setOf(
    90000000, 90000001, 90000002, 90000003, 90000004, 90000005,
    90000006, 90000007, 90000008, 90000009, 90000011, 90000012,
    90000016, 90000017, 90000020, 90000024, 90000034, 90000042,
    90000043, 90000044, 90000047, 90000052, 90000056, 90000057
)

private fun equipmentRarity(dataId: Int): String? = when (dataId) {
    in EpicEquipmentIds -> "epic"
    in CommonEquipmentIds -> "common"
    else -> null
}

private fun equipmentMaxLevel(rarity: String?, blacksmithLevel: Int?): Int? {
    val normalized = rarity?.lowercase() ?: return null
    val maximum = when (normalized) {
        "common" -> 18
        "epic" -> 27
        else -> return null
    }
    val smith = blacksmithLevel ?: return maximum
    return when (normalized) {
        "common" -> when {
            smith < 3 -> 9
            smith < 5 -> 12
            smith < 7 -> 15
            else -> 18
        }
        "epic" -> when {
            smith < 3 -> 12
            smith < 5 -> 15
            smith < 7 -> 18
            smith < 8 -> 21
            smith < 9 -> 24
            else -> 27
        }
        else -> maximum
    }
}

private fun equipmentHero(dataId: Int): String? = when (dataId) {
    90000000, 90000001, 90000008, 90000011, 90000010, 90000014, 90000032, 90000051 -> "Vua man di"
    90000002, 90000003, 90000017, 90000020, 90000015, 90000039, 90000048, 90000059 -> "Nữ hoàng cung thủ"
    90000042, 90000043, 90000044, 90000047, 90000035, 90000049, 90000061 -> "Hoàng tử tiểu quỷ"
    90000004, 90000005, 90000024, 90000034, 90000022, 90000041, 90000019 -> "Đại hộ vệ"
    90000007, 90000006, 90000009, 90000012, 90000013, 90000040, 90000050 -> "Nhà vô địch Hoàng gia"
    90000052, 90000053, 90000056, 90000057, 90000016, 90000060 -> "Công tước Rồng"
    else -> null
}

@Composable
private fun ItemRarityBadge(rarity: String) {
    val normalized = rarity.lowercase()
    val (label, tint) = when (normalized) {
        "common" -> "THƯỜNG" to Color(0xFF55B9FF)
        "rare" -> "HIẾM" to Color(0xFF39D8D0)
        "epic" -> "SỬ THI" to Color(0xFFB77AFF)
        "legendary" -> "HUYỀN THOẠI" to Color(0xFFFFC857)
        else -> rarity.uppercase() to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Text(
        label,
        color = tint,
        fontSize = 9.sp,
        fontWeight = FontWeight.Black,
        modifier = Modifier
            .background(tint.copy(alpha = 0.17f), RoundedCornerShape(7.dp))
            .border(1.dp, tint.copy(alpha = 0.55f), RoundedCornerShape(7.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    )
}

@Composable
private fun VillageItemDetailDialog(
    item: VillageItem,
    remote: RemoteCatalogEntry?,
    blacksmithLevel: Int?,
    hallLevel: Int?,
    upgradeCatalog: Map<Int, CocUpgradeEntry>,
    onDismiss: () -> Unit
) {
    val upgradeEntry = upgradeCatalog[item.dataId]
    val rarity = upgradeEntry?.rarity?.takeIf { it.isNotBlank() }
        ?: remote?.rarity?.takeIf { it.isNotBlank() }
        ?: if (item.sectionKey == "equipment") equipmentRarity(item.dataId) else null
    val maxLevel = upgradeEntry?.maxLevel?.takeIf { it > 0 } ?: remote?.maxLevel
    val hallMax = upgradeEntry?.let { CocUpgradeCatalog.maxLevelAtHall(it, hallLevel, upgradeCatalog) }
    val blacksmithMax = upgradeEntry?.let {
        CocUpgradeCatalog.maxEquipmentAtBlacksmithLevel(it, blacksmithLevel, upgradeCatalog)
    }
    val isEquipment = upgradeEntry?.category == "hero-equipment" || item.sectionKey == "equipment"
    val usableCap = if (isEquipment && blacksmithMax != null) {
        listOfNotNull(hallMax, blacksmithMax).minOrNull()
    } else {
        hallMax
    } ?: maxLevel
    val currentLevel = item.level ?: 0
    val isEnglish = LocalAppLanguage.current == AppUiLanguage.ENGLISH
    val itemName = if (isEnglish) {
        remote?.name?.takeIf { it.isNotBlank() } ?: item.name
    } else {
        item.name
    }
    val description = if (isEnglish) {
        remote?.description?.takeIf { it.isNotBlank() }
    } else {
        EquipmentDescriptionsVi[item.dataId]
            ?: CatalogDescriptionsVi[item.dataId]
            ?: remote?.description?.takeIf { it.isNotBlank() }
    }
    val levelDetails = remote?.levelDetails.orEmpty()
    val availableStatsLevels = (levelDetails.keys + upgradeEntry?.levels.orEmpty().map { it.level })
        .distinct()
        .sorted()
    val initialStatsTarget = item.level ?: availableStatsLevels.firstOrNull() ?: 1
    val initialStatsLevel = availableStatsLevels.minByOrNull {
        kotlin.math.abs(it - initialStatsTarget)
    } ?: initialStatsTarget
    var selectedStatsLevel by remember(item.identity, availableStatsLevels) {
        mutableStateOf(initialStatsLevel)
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 620.dp),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (remote?.imageUrl?.isNotBlank() == true) {
                        AsyncImage(
                            model = remote.imageUrl,
                            contentDescription = itemName,
                            modifier = Modifier.size(70.dp),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        CategoryImage(item, 70)
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            itemName,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            item.sectionLabel,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (rarity != null) ItemRarityBadge(rarity)
                    }
                }

                Divider(color = MaterialTheme.colorScheme.outlineVariant)
                VillageItemDetailField(
                    "Nguồn dữ liệu nâng cấp",
                    "Dữ liệu cộng đồng được chốt ngày 15/11/2026; không phải API chính thức của Supercell. Kiểm tra trong trò chơi sau mỗi bản cập nhật."
                )
                VillageItemDetailField(
                    "Cách tính thời gian",
                    "Cộng thời gian cơ bản theo từng cấp; chưa trừ Sách, Búa, trợ thủ, tăng tốc hoặc ưu đãi sự kiện."
                )
                VillageItemDetailField("Căn cứ", if (item.base == "builder") "Làng đêm" else "Làng chính")
                val ownerHero = upgradeEntry?.hero?.takeIf { it.isNotBlank() }?.let(::equipmentHeroName)
                    ?: equipmentHero(item.dataId)?.takeIf { isEquipment }
                ownerHero?.let { VillageItemDetailField("👑 Anh hùng sử dụng", it) }
                val levelText = item.level?.let { level ->
                    usableCap?.let { "$level / $it" } ?: level.toString()
                } ?: "Chưa có dữ liệu"
                VillageItemDetailField("Cấp hiện tại / giới hạn đang mở", levelText)
                hallMax?.let {
                    VillageItemDetailField(if (item.base == "builder") "Giới hạn theo Nhà thợ xây" else "Giới hạn theo Nhà chính", it.toString())
                }
                if (isEquipment && blacksmithMax != null) {
                    VillageItemDetailField("Giới hạn theo Lò rèn hiện tại", blacksmithMax.toString())
                }
                maxLevel?.let { VillageItemDetailField("Cấp tối đa toàn trò chơi", it.toString()) }
                VillageItemDetailField(if (item.base == "builder") "Cấp Nhà thợ xây hiện tại" else "Cấp Nhà chính hiện tại", hallLevel?.toString() ?: "Chưa có dữ liệu")
                VillageItemDetailField("Số lượng", item.count.toString())
                if (item.gearUp) VillageItemDetailField("Trạng thái", "Đã ghép công trình")
                item.weaponLevel?.let { VillageItemDetailField("Cấp vũ khí", it.toString()) }
                item.timerSeconds?.let {
                    VillageItemDetailField(
                        "Thời gian còn lại",
                        if (it <= 0L) "Đã hết thời gian" else formatDuration(it)
                    )
                }
                item.helperCooldownSeconds?.let {
                    VillageItemDetailField("Thời gian hồi trợ thủ", formatDuration(it))
                }

                upgradeEntry?.let { entry ->
                    val nextLevel = currentLevel + 1
                    val nextStep = entry.level(nextLevel)
                    if (nextStep != null) {
                        Text(
                            if (item.timerSeconds != null && item.timerSeconds > 0L) "ĐANG NÂNG CẤP" else "NÂNG CẤP TIẾP THEO",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black
                        )
                        if (item.timerSeconds != null && item.timerSeconds > 0L) {
                            VillageItemDetailField("Mục tiêu hiện tại", "Cấp $nextLevel")
                        } else {
                            val requirements = CocUpgradeCatalog.nextRequirements(entry, nextLevel, upgradeCatalog)
                            if (requirements.isNotEmpty()) {
                                VillageItemDetailField(
                                    "Yêu cầu mở cấp tiếp theo",
                                    requirements.joinToString(" • ") { "${it.label} cấp ${it.level}" }
                                )
                            }
                            if (nextStep.cost() > 0L) {
                                UpgradeResourceChip(nextStep.resource(), nextStep.cost())
                            }
                            nextStep.oreCosts().forEach { (resource, amount) ->
                                UpgradeResourceChip(resource, amount)
                            }
                            if (nextStep.durationSeconds() > 0L) {
                                VillageItemDetailField("⏱️ Thời gian nâng cấp tiếp theo", formatDuration(nextStep.durationSeconds()))
                            }
                        }
                    }
                    val targetAtHall = hallMax
                    val targetAtMax = maxLevel ?: entry.maxLevel
                    if (targetAtHall != null && targetAtHall > currentLevel) {
                        val totals = CocUpgradeCatalog.upgradeTotals(
                            entry,
                            currentLevel,
                            targetAtHall,
                            skipFirstInProgress = item.timerSeconds != null && item.timerSeconds > 0L
                        )
                        Text(
                            "TỔNG CÒN ĐẾN GIỚI HẠN THEO NHÀ",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black
                        )
                        totals.resources.forEach { (resource, amount) -> UpgradeResourceChip(resource, amount) }
                        val totalSeconds = totals.durationSeconds + (item.timerSeconds?.coerceAtLeast(0L) ?: 0L)
                        if (totalSeconds > 0L) VillageItemDetailField("🕒 Thời gian ước tính còn lại", formatDuration(totalSeconds))
                        if (totals.resources.isEmpty() && totalSeconds == 0L) {
                            VillageItemDetailField("Tài nguyên và thời gian", "Không cần thêm theo dữ liệu cấp hiện có")
                        }
                    }
                    val globalMaxStart = maxOf(currentLevel, targetAtHall ?: currentLevel)
                    if (targetAtMax > globalMaxStart) {
                        val totals = CocUpgradeCatalog.upgradeTotals(
                            entry,
                            globalMaxStart,
                            targetAtMax
                        )
                        Text(
                            if (targetAtHall == null) "TỔNG CÒN ĐẾN CẤP MAX TOÀN TRÒ CHƠI"
                            else "TỪ GIỚI HẠN THEO NHÀ ĐẾN CẤP MAX TOÀN TRÒ CHƠI",
                            color = CocGold,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black
                        )
                        totals.resources.forEach { (resource, amount) -> UpgradeResourceChip(resource, amount) }
                        if (totals.durationSeconds > 0L) {
                            VillageItemDetailField("🕒 Thời gian bổ sung", formatDuration(totals.durationSeconds))
                        }
                    }
                    if (hallMax == null && targetAtMax > currentLevel) {
                        VillageItemDetailField(
                            "Giới hạn theo Nhà chính",
                            "Chưa đủ dữ liệu yêu cầu cấp Nhà chính/công trình để xác định chính xác. Không tự ước đoán cấp này."
                        )
                    }
                }

                description?.let {
                    VillageItemDetailField("Mô tả", it)
                }
                remote?.aliases?.takeIf { it.isNotEmpty() }?.let {
                    VillageItemDetailField("Tên gọi khác", it.joinToString(", "))
                }
                remote?.requirements?.takeIf { it.isNotEmpty() }?.let { requirements ->
                    VillageItemDetailField(
                        "Yêu cầu mở khóa",
                        requirements.joinToString("\n") { localizedCatalogText(it, isEnglish) }
                    )
                }
                remote?.abilityType?.takeIf { it.isNotBlank() }?.let {
                    VillageItemDetailField(
                        "Loại kỹ năng",
                        if (it == "active") "Chủ động" else "Bị động"
                    )
                }
                if (availableStatsLevels.isNotEmpty()) {
                    Text(
                        "THÔNG SỐ THEO CẤP",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black
                    )
                    val selectedStatsIndex = availableStatsLevels.indexOf(selectedStatsLevel).coerceAtLeast(0)
                    VillageItemDetailField(
                        "Cấp dữ liệu đang xem",
                        "$selectedStatsLevel (${selectedStatsIndex + 1}/${availableStatsLevels.size})"
                    )
                    if (availableStatsLevels.size > 1) {
                        Slider(
                            value = selectedStatsIndex.toFloat(),
                            onValueChange = {
                                val index = it.roundToInt().coerceIn(availableStatsLevels.indices)
                                selectedStatsLevel = availableStatsLevels[index]
                            },
                            valueRange = 0f..(availableStatsLevels.lastIndex).toFloat(),
                            steps = (availableStatsLevels.size - 2).coerceAtLeast(0)
                        )
                    }
                    val stats = levelDetails[selectedStatsLevel].orEmpty()
                    val catalogStats = upgradeEntry?.level(selectedStatsLevel)?.displayStats().orEmpty()
                    if (stats.isEmpty() && catalogStats.isEmpty()) {
                        Text(
                            "Chưa có chỉ số cho cấp này.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    } else if (stats.isNotEmpty()) {
                        stats.forEachIndexed { index, rawValue ->
                            val separator = rawValue.indexOf(':')
                            val rawLabel = if (separator > 0) {
                                rawValue.substring(0, separator).trim()
                            } else {
                                "Chỉ số ${index + 1}"
                            }
                            val value = if (separator > 0) {
                                rawValue.substring(separator + 1).trim()
                            } else {
                                rawValue
                            }
                            val (label, displayValue) = localizedCatalogField(rawLabel, value, isEnglish)
                            VillageItemStatField(label, displayValue)
                        }
                    } else {
                        catalogStats.forEach { (rawLabel, rawValue) ->
                            val normalizedLabel = rawLabel.removePrefix("stats_").removePrefix("normal_")
                            val (label, value) = localizedCatalogField(normalizedLabel, rawValue, isEnglish)
                            VillageItemStatField(label, value)
                        }
                    }
                }
                remote?.upgradeCost?.takeIf { it.isNotBlank() }?.let {
                    VillageItemDetailField("Chi phí nâng cấp", it)
                }
                remote?.upgradeDurationSeconds?.let {
                    VillageItemDetailField("Thời gian nâng cấp", formatDuration(it))
                }

                if (item.details.isNotEmpty()) {
                    Text("THÔNG TIN TỪ DỮ LIỆU LÀNG", color = MaterialTheme.colorScheme.primary, fontSize = 10.sp, fontWeight = FontWeight.Black)
                    item.details.forEachIndexed { index, value ->
                        VillageItemDetailField("Thông tin ${index + 1}", value)
                    }
                }
                if (item.properties.isNotEmpty()) {
                    Text("CHỈ SỐ VÀ THUỘC TÍNH", color = MaterialTheme.colorScheme.primary, fontSize = 10.sp, fontWeight = FontWeight.Black)
                    item.properties.forEach { (key, value) ->
                        VillageItemDetailField(
                            key.replace('_', ' ').replaceFirstChar { it.uppercase() },
                            value
                        )
                    }
                }
                if (remote?.details?.isNotEmpty() == true) {
                    Text("THÔNG TIN BỔ SUNG", color = MaterialTheme.colorScheme.primary, fontSize = 10.sp, fontWeight = FontWeight.Black)
                    remote.details.forEachIndexed { index, value ->
                        val separator = value.indexOf(':')
                        val rawLabel = if (separator > 0) value.substring(0, separator).trim() else "Chi tiết ${index + 1}"
                        val rawValue = if (separator > 0) value.substring(separator + 1).trim() else value
                        val (label, displayValue) = localizedCatalogField(rawLabel, rawValue, isEnglish)
                        VillageItemDetailField(label, displayValue)
                    }
                }
                if (remote == null && description == null && item.details.isEmpty() && item.properties.isEmpty() &&
                    item.timerSeconds == null && item.helperCooldownSeconds == null
                ) {
                    Text(
                        "JSON hiện không cung cấp thêm chỉ số cho mục này. Có thể bổ sung mô tả, chỉ số, yêu cầu và ảnh trong danh mục dữ liệu cập nhật từ xa.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("ĐÓNG")
                }
            }
        }
    }
}

@Composable
private fun VillageItemStatField(label: String, value: String) {
    val normalized = label.trim().lowercase()
    val oreColor = when {
        normalized.contains("quặng sao") || normalized.contains("starry ore") -> Color(0xFFFFC857)
        normalized.contains("quặng phát sáng") || normalized.contains("glowy ore") -> Color(0xFFB77AFF)
        normalized.contains("quặng sáng") || normalized.contains("shiny ore") -> Color(0xFF55B9FF)
        else -> null
    }
    if (oreColor == null) {
        VillageItemDetailField(label, value)
    } else {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(oreColor.copy(alpha = 0.10f), RoundedCornerShape(10.dp))
                .border(1.dp, oreColor.copy(alpha = 0.42f), RoundedCornerShape(10.dp))
                .padding(9.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(label, color = oreColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text(value, color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, lineHeight = 17.sp)
        }
    }
}

@Composable
private fun UpgradeResourceChip(resource: String, amount: Long) {
    val (icon, label, tint) = when (resource.trim().lowercase()) {
        "gold" -> Triple("🪙", "Vàng", Color(0xFFFFD166))
        "elixir" -> Triple("🧪", "Tiên dược", Color(0xFFFF6EC7))
        "dark elixir" -> Triple("🖤", "Dầu đen", Color(0xFFB77AFF))
        "builder gold" -> Triple("🪙", "Vàng thợ xây", Color(0xFFFFD166))
        "builder elixir" -> Triple("🧪", "Tiên dược thợ xây", Color(0xFFFF6EC7))
        "gold or elixir" -> Triple("🪙🧪", "Vàng hoặc Tiên dược", Color(0xFFFFD166))
        "builder gold or builder elixir" -> Triple("🪙🧪", "Vàng thợ xây hoặc Tiên dược thợ xây", Color(0xFFFFD166))
        "capital gold" -> Triple("🏅", "Vàng thủ đô", Color(0xFFFFD166))
        "quặng sáng", "shiny ore" -> Triple("💠", "Quặng sáng", Color(0xFF55B9FF))
        "quặng phát sáng", "glowy ore" -> Triple("🔮", "Quặng phát sáng", Color(0xFFB77AFF))
        "quặng sao", "starry ore" -> Triple("⭐", "Quặng sao", Color(0xFFFFC857))
        else -> Triple("📦", resource, MaterialTheme.colorScheme.primary)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(tint.copy(alpha = 0.10f), RoundedCornerShape(10.dp))
            .border(1.dp, tint.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(icon, fontSize = 17.sp)
        Text(label, color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp, modifier = Modifier.weight(1f))
        Text(java.text.NumberFormat.getIntegerInstance().format(amount), color = tint, fontSize = 12.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun VillageItemDetailField(label: String, value: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
        Text(value, color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, lineHeight = 17.sp)
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
    isGoogleSignedIn: Boolean,
    history: List<VillageSnapshot>,
    receivedJsons: List<ReceivedSharedJsonRecord>,
    onDeleteReceivedJson: (String) -> Unit,
    onImportJson: (String, String, String, String, String, Long, String) -> Unit,
    deletedVillages: List<DeletedVillageRecord>,
    pinnedTags: List<String>,
    activeTag: String,
    activeTimestamp: Long?,
    onSelect: (VillageSnapshot) -> Unit,
    onTogglePin: (String) -> Unit,
    onDelete: (String) -> Unit,
    onRestore: (String) -> Unit,
    onDeletePermanently: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val signedInUser = FirebaseAuth.getInstance().currentUser?.takeIf { isGoogleSignedIn }
    var selectedSection by remember { mutableStateOf("villages") }
    var sharedJsonTab by remember { mutableStateOf("sent") }
    var sharedJsons by remember { mutableStateOf<List<CommunitySharedJson>>(emptyList()) }
    var loadingSharedJsons by remember { mutableStateOf(true) }
    var sharedJsonStatus by remember { mutableStateOf<String?>(null) }
    var pendingSharedJsonDelete by remember { mutableStateOf<CommunitySharedJson?>(null) }
    var pendingReceivedJsonDelete by remember { mutableStateOf<ReceivedSharedJsonRecord?>(null) }
    var pendingDelete by remember { mutableStateOf<VillageSnapshot?>(null) }
    var pendingPermanentDelete by remember { mutableStateOf<DeletedVillageRecord?>(null) }
    var expandedDeletedRecordId by remember { mutableStateOf<String?>(null) }
    var pendingDeletedJsonSave by remember { mutableStateOf<VillageSnapshot?>(null) }
    var deletedSaveStatus by remember { mutableStateOf<String?>(null) }
    val saveDeletedJsonLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { destination ->
        val snapshot = pendingDeletedJsonSave
        pendingDeletedJsonSave = null
        if (destination != null && snapshot != null) {
            val result = runCatching {
                val output = context.contentResolver.openOutputStream(destination)
                    ?: throw IllegalStateException("Không thể mở vị trí lưu JSON.")
                output.use { it.write(snapshot.rawJson.toByteArray(Charsets.UTF_8)) }
            }
            deletedSaveStatus = if (result.isSuccess) {
                "Đã lưu JSON của ${snapshot.villageName ?: snapshot.tag}."
            } else {
                result.exceptionOrNull()?.message ?: "Không thể lưu JSON."
            }
        }
    }
    DisposableEffect(signedInUser?.uid, selectedSection) {
        if (signedInUser == null || selectedSection != "shared") {
            onDispose { }
        } else {
            loadingSharedJsons = true
            val registration = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("community_shared_jsons")
                .whereEqualTo("uid", signedInUser.uid)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        sharedJsonStatus = error.message ?: "Không thể tải JSON đã chia sẻ."
                        loadingSharedJsons = false
                    } else {
                        sharedJsons = snapshot?.documents.orEmpty().mapNotNull { document ->
                            val rawJson = document.getString("rawJson") ?: return@mapNotNull null
                            CommunitySharedJson(
                                id = document.id,
                                uid = document.getString("uid").orEmpty(),
                                displayName = document.getString("displayName").orEmpty().ifBlank { "Thành viên" },
                                playerTag = document.getString("playerTag").orEmpty(),
                                villageName = document.getString("villageName").orEmpty(),
                                timestamp = document.getLong("timestamp") ?: 0L,
                                rawJson = rawJson,
                                createdAt = document.getTimestamp("createdAt")?.toDate()?.time ?: 0L
                            )
                        }.sortedByDescending { it.createdAt }
                        sharedJsonStatus = null
                        loadingSharedJsons = false
                    }
                }
            onDispose { registration.remove() }
        }
    }
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
                Text("JSON", color = MaterialTheme.colorScheme.primary, fontSize = 13.sp, fontWeight = FontWeight.Black)
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
                        selected = selectedSection == "deleted",
                        onClick = {
                            selectedSection = "deleted"
                            deletedSaveStatus = null
                        },
                        label = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text("ĐÃ GỠ", fontSize = 9.sp, maxLines = 1)
                                Text("(${deletedVillages.size})", fontSize = 9.sp, maxLines = 1)
                            }
                        }
                    )
                    FilterChip(
                        selected = selectedSection == "shared",
                        onClick = { selectedSection = "shared" },
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
        } else if (selectedSection == "deleted") {
            deletedSaveStatus?.let {
                item { Text(it, color = MaterialTheme.colorScheme.primary, fontSize = 10.sp) }
            }
            if (deletedVillages.isEmpty()) {
                item { InfoCard("Chưa có JSON nào bị gỡ", "Những làng bạn xóa khỏi mục ĐANG LƯU sẽ xuất hiện tại đây để xem, lưu hoặc khôi phục.") }
            } else {
                items(deletedVillages, key = { "deleted_village_" + it.id }) { record ->
                    val expanded = expandedDeletedRecordId == record.id
                    NeonPanel(
                        modifier = Modifier.fillMaxWidth(),
                        glow = CocRed
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    record.villageName ?: record.tag,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    "${record.snapshots.size} mốc",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                "Mã người chơi: ${record.tag}  •  Đã gỡ: ${formatTimestamp(record.deletedAt)}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 9.sp
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    onClick = {
                                        expandedDeletedRecordId = if (expanded) null else record.id
                                    },
                                    enabled = record.snapshots.isNotEmpty()
                                ) {
                                    Text(if (expanded) "ẨN JSON" else "XEM JSON", fontSize = 9.sp)
                                }
                                TextButton(onClick = { onRestore(record.id) }) {
                                    Text("KHÔI PHỤC", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                                TextButton(onClick = { pendingPermanentDelete = record }) {
                                    Text("XÓA VĨNH VIỄN", color = CocRed, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            if (expanded) {
                                record.snapshots.forEach { snapshot ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(7.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Column(
                                                modifier = Modifier.weight(1f),
                                                verticalArrangement = Arrangement.spacedBy(2.dp)
                                            ) {
                                                Text(
                                                    formatTimestamp(snapshot.timestamp),
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    "Nhà chính ${snapshot.townHallLevel ?: "—"}  •  Nhà thợ ${snapshot.builderHallLevel ?: "—"}  •  ${snapshot.items.size} mục",
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    fontSize = 8.sp
                                                )
                                            }
                                            TextButton(
                                                onClick = {
                                                    pendingDeletedJsonSave = snapshot
                                                    val safeName = (snapshot.villageName ?: snapshot.tag)
                                                        .replace(Regex("[^A-Za-z0-9._-]"), "_")
                                                    saveDeletedJsonLauncher.launch(
                                                        "${safeName}_${snapshot.timestamp}.json"
                                                    )
                                                }
                                            ) {
                                                Text("LƯU JSON", fontSize = 9.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else if (selectedSection == "shared") {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text("CHIA SẺ JSON", color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, fontWeight = FontWeight.Black)
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        FilterChip(
                            selected = sharedJsonTab == "sent",
                            onClick = { sharedJsonTab = "sent" },
                            label = { Text("ĐÃ GỬI", fontSize = 10.sp) }
                        )
                        FilterChip(
                            selected = sharedJsonTab == "received",
                            onClick = { sharedJsonTab = "received" },
                            label = { Text("ĐÃ NHẬN", fontSize = 10.sp) }
                        )
                    }
                    sharedJsonStatus?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, fontSize = 10.sp)
                    }
                }
            }
            if (signedInUser == null) {
                item { InfoCard("Đăng nhập Google", "Đăng nhập để xem JSON đã chia sẻ và JSON nhận được.") }
            } else if (sharedJsonTab == "sent") {
                if (loadingSharedJsons) {
                    item { Box(Modifier.fillMaxWidth().padding(18.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
                } else if (sharedJsons.isEmpty()) {
                    item { InfoCard("Chưa chia sẻ JSON nào", "Mở Cộng đồng và dùng nút GỬI JSON để đăng một làng. Danh sách đã gửi sẽ xuất hiện tại đây.") }
                } else {
                    items(sharedJsons, key = { "shared_" + it.id }) { sharedJson ->
                        CommunitySharedJsonCard(
                            item = sharedJson,
                            enabled = true,
                            onDelete = { pendingSharedJsonDelete = sharedJson }
                        )
                    }
                }
            } else {
                if (receivedJsons.isEmpty()) {
                    item { InfoCard("Chưa nhận JSON nào", "JSON được người khác gửi trong Cộng đồng sẽ xuất hiện tại đây sau khi bạn nhận.") }
                } else {
                    items(receivedJsons, key = { "received_" + it.id }) { received ->
                        CommunityReceivedJsonCard(
                            item = received,
                            enabled = true,
                            onUse = {
                                onImportJson(
                                    received.rawJson,
                                    received.senderUid,
                                    received.senderName,
                                    received.tag,
                                    received.villageName,
                                    received.timestamp,
                                    received.id
                                )
                            },
                            onDelete = { pendingReceivedJsonDelete = received }
                        )
                    }
                }
            }
        }
    }

    pendingSharedJsonDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { pendingSharedJsonDelete = null },
            title = { Text("Xóa JSON đã gửi?") },
            text = {
                Text("JSON này sẽ bị xóa khỏi danh sách chia sẻ và bài đăng JSON tương ứng trong Cộng đồng. Bản JSON trong Lịch sử và Đang lưu trên thiết bị vẫn được giữ.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingSharedJsonDelete = null
                        scope.launch {
                            try {
                                deleteSharedJsonFromCommunity(item.id)
                                sharedJsonStatus = "Đã xóa JSON đã gửi."
                            } catch (error: Exception) {
                                sharedJsonStatus = error.message ?: "Không thể xóa JSON đã gửi."
                            }
                        }
                    }
                ) { Text("XÓA JSON", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { pendingSharedJsonDelete = null }) { Text("HỦY") }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    pendingReceivedJsonDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { pendingReceivedJsonDelete = null },
            title = { Text("Xóa JSON đã nhận?") },
            text = {
                Text("Chỉ xóa bản JSON trong danh sách ĐÃ NHẬN trên thiết bị này. Bản trong Lịch sử và JSON gốc của người chia sẻ vẫn được giữ.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingReceivedJsonDelete = null
                        onDeleteReceivedJson(item.id)
                    }
                ) { Text("XÓA", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { pendingReceivedJsonDelete = null }) { Text("HỦY") }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
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
