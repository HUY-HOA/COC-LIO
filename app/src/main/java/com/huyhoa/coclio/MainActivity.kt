package com.huyhoa.coclio

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.Context
import android.content.IntentFilter
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
import androidx.compose.foundation.Image
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
import androidx.compose.ui.res.painterResource
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
import com.huyhoa.coclio.data.CocVillageMaxPlanCalculator
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MilitaryTech
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.withFrameNanos
import androidx.core.content.ContextCompat
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalConfiguration
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.huyhoa.coclio.data.DeletedVillageRecord
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

private data class AppNavigationItem(
    val id: String,
    val title: String,
    val page: Int
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
    strokeWidthDp: Float = 2.6f,
    brightnessIndependent: Boolean = false
): Modifier {
    val led = LocalLedTheme.current
    val palette = led.palette.map {
        it.copy(alpha = if (brightnessIndependent) 1f else led.brightness.coerceIn(0f, 1f))
    }
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
    enabled: Boolean = true,
    showLed: Boolean = true
) {
    val shape = RoundedCornerShape(14.dp)
    val buttonBackground = Color(0xFF168BFF).copy(alpha = if (enabled) 1f else 0.45f)
    Box(
        modifier = modifier
            .shadow(5.dp, shape)
            .ledRgbBorder(cornerRadiusDp = 14f, strokeWidthDp = if (showLed) 2f else 0f)
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
    override fun onResume() {
        super.onResume()
        FloatingJsonOverlayController.onHostResume(this)
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LegacyCommunityCleanup.disable(this)
        clearLegacyLanguagePreference(this)
        setContent {
            var themeMode by remember { mutableStateOf(AppPreferences.themeMode(this@MainActivity)) }
            val dark = when (themeMode) {
                "light" -> false
                "dark" -> true
                else -> isSystemInDarkTheme()
            }
            val colors = if (dark) {
                darkColorScheme(
                    primary = Color(0xFF36F4FF),
                    onPrimary = Color(0xFF04101D),
                    secondary = Color(0xFFFF4DDA),
                    tertiary = Color(0xFFFFD166),
                    background = Color(0xFF000000),
                    surface = Color(0xFF0D152B),
                    surfaceVariant = Color(0xFF14203B),
                    onBackground = Color.White,
                    onSurface = Color.White,
                    onSurfaceVariant = Color(0xFF9EAFCE)
                )
            } else {
                lightColorScheme(
                    primary = Color(0xFF006A78),
                    onPrimary = Color.White,
                    secondary = Color(0xFF8B3B85),
                    tertiary = Color(0xFF8A5A00),
                    background = Color(0xFFFFFFFF),
                    surface = Color(0xFFF1F5FB),
                    surfaceVariant = Color(0xFFE4ECF6),
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

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
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
    var accountProfileReadyForEmail by remember { mutableStateOf<String?>(null) }
    var accountProfileReady by remember { mutableStateOf(false) }
    var googleSignInSubmitting by remember { mutableStateOf(false) }
    var accountProfileError by remember { mutableStateOf<String?>(null) }
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
    var selectedTab by remember { mutableStateOf(2) }
    var jsonSection by rememberSaveable { mutableStateOf(0) }
    val navigationItems = listOf(
        AppNavigationItem("history", "JSON", 2),
        AppNavigationItem("lookup", "TRA CỨU", 3),
        AppNavigationItem("settings", "Cài đặt", 4)
    )
    LaunchedEffect(navigationItems, selectedTab) {
        if (navigationItems.none { it.page == selectedTab }) {
            selectedTab = navigationItems.first().page
        }
    }
    var showImportDialog by remember { mutableStateOf(false) }
    var importError by remember { mutableStateOf<String?>(null) }
    var googleAuthError by remember { mutableStateOf<String?>(null) }
    var loginTermsAccepted by rememberSaveable { mutableStateOf(false) }
    var importSummary by remember { mutableStateOf<ImportSummary?>(null) }
    var lastUsedAt by remember(activeProfile) {
        mutableLongStateOf(AccountProfileStore.lastUsedAt(context, activeProfile) ?: 0L)
    }
    var notificationsEnabled by remember(activeProfile) {
        mutableStateOf(AppPreferences.notificationsEnabled(context, activeProfile))
    }
    var bubbleNotificationsEnabled by remember(activeProfile) {
        mutableStateOf(AppPreferences.bubbleNotificationsEnabled(context, activeProfile))
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
    DisposableEffect(context, activeProfile) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(receiverContext: Context, intent: Intent) {
                if (intent.action != FloatingJsonOverlayService.ACTION_JSON_IMPORTED) return
                val importedProfile = intent.getStringExtra(FloatingJsonOverlayService.EXTRA_PROFILE).orEmpty()
                if (!importedProfile.equals(activeProfile, ignoreCase = true)) return
                val importedTag = intent.getStringExtra(FloatingJsonOverlayService.EXTRA_TAG).orEmpty()
                history = VillageStore.loadAll(context, activeProfile)
                deletedVillages = VillageStore.loadDeleted(context, activeProfile)
                pinnedTags = VillageStore.pinnedTags(context, activeProfile)
                current = history.firstOrNull { it.tag == importedTag } ?: history.firstOrNull()
                val importedName = intent.getStringExtra(FloatingJsonOverlayService.EXTRA_NAME).orEmpty()
                scope.launch {
                    snackbar.showAppSnackbar(
                        context,
                        "Đã lưu JSON ${importedName.ifBlank { importedTag }} từ cửa sổ nổi."
                    )
                }
            }
        }
        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(FloatingJsonOverlayService.ACTION_JSON_IMPORTED),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        onDispose { runCatching { context.unregisterReceiver(receiver) } }
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
    val ledSpeedMultiplier = 1f
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
        palette = paletteForLed(ledPreset),
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

    fun enableBubbleNotifications(profile: String) {
        AppPreferences.setBubbleNotificationsEnabled(context, profile, true)
        if (profile == activeProfile) bubbleNotificationsEnabled = true
        UpgradeReminderScheduler.scheduleAll(
            context,
            history,
            profile,
            AppPreferences.notificationsEnabled(context, profile)
        )
        val serviceIntent = Intent(context, NotificationBubbleService::class.java)
            .setAction(NotificationBubbleService.ACTION_ENABLE)
            .putExtra(NotificationBubbleService.EXTRA_PROFILE, profile)
        runCatching {
            ContextCompat.startForegroundService(context, serviceIntent)
        }.onFailure { error ->
            AppPreferences.setBubbleNotificationsEnabled(context, profile, false)
            if (profile == activeProfile) bubbleNotificationsEnabled = false
            UpgradeReminderScheduler.scheduleAll(
                context,
                history,
                profile,
                AppPreferences.notificationsEnabled(context, profile)
            )
            scope.launch {
                snackbar.showAppSnackbar(
                    context,
                    error.message?.takeIf { it.isNotBlank() }
                        ?: "Không thể khởi động bong bóng thông báo."
                )
            }
        }
    }

    fun disableBubbleNotifications(profile: String) {
        AppPreferences.setBubbleNotificationsEnabled(context, profile, false)
        if (profile == activeProfile) bubbleNotificationsEnabled = false
        UpgradeReminderScheduler.scheduleAll(
            context,
            history,
            profile,
            AppPreferences.notificationsEnabled(context, profile)
        )
        context.stopService(Intent(context, NotificationBubbleService::class.java))
    }

    fun bubbleOverlayPermissionIntent(): Intent =
        Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
            .setData(android.net.Uri.parse("package:${context.packageName}"))

    val bubbleOverlayPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
            android.provider.Settings.canDrawOverlays(context)
        ) {
            enableBubbleNotifications(activeProfile)
        } else {
            disableBubbleNotifications(activeProfile)
            scope.launch {
                snackbar.showAppSnackbar(
                    context,
                    "Bạn chưa cấp quyền Hiển thị trên ứng dụng khác. Bong bóng chưa được bật."
                )
            }
        }
    }

    val bubbleNotificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
                android.provider.Settings.canDrawOverlays(context)
            ) {
                enableBubbleNotifications(activeProfile)
            } else {
                bubbleOverlayPermissionLauncher.launch(bubbleOverlayPermissionIntent())
            }
        } else {
            disableBubbleNotifications(activeProfile)
            scope.launch {
                snackbar.showAppSnackbar(
                    context,
                    "Bạn chưa cấp quyền thông báo. Bong bóng thông báo chưa được bật."
                )
            }
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
        notificationsEnabled = AppPreferences.notificationsEnabled(context, activeProfile)
        notificationStyle = AppPreferences.notificationStyle(context, activeProfile)
        bubbleNotificationsEnabled = AppPreferences.bubbleNotificationsEnabled(context, activeProfile)
        if (bubbleNotificationsEnabled) {
            val overlayAllowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
                android.provider.Settings.canDrawOverlays(context)
            if (overlayAllowed) {
                runCatching {
                    ContextCompat.startForegroundService(
                        context,
                        Intent(context, NotificationBubbleService::class.java)
                            .setAction(NotificationBubbleService.ACTION_ENABLE)
                            .putExtra(NotificationBubbleService.EXTRA_PROFILE, activeProfile)
                    )
                }
            } else {
                AppPreferences.setBubbleNotificationsEnabled(context, activeProfile, false)
                bubbleNotificationsEnabled = false
                context.stopService(Intent(context, NotificationBubbleService::class.java))
            }
        } else {
            context.stopService(Intent(context, NotificationBubbleService::class.java))
        }
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
            AccountPrivacyConsent.hasRequiredAccountDataConsent()
        }
            .onSuccess { ready ->
                accountProfileReady = ready
                accountProfileReadyForEmail = email
                if (!ready) {
                    accountProfileError = "Bạn cần xác nhận điều khoản và chính sách trước khi đăng nhập Google."
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
        AccountPrivacyConsent.applyCollectionSettings(context, AccountPrivacyChoice())
        AccountActivityTracker.stopAndWait(context)
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
            val savedSuccessfully = VillageStore.save(context, parsed, activeProfile, customName)
            check(savedSuccessfully) {
                "Đã đạt giới hạn 50 JSON. Hãy xóa vĩnh viễn JSON cũ trước khi thêm bản mới."
            }
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
        check(loginTermsAccepted) { "Bạn cần đồng ý với điều khoản và chính sách trước khi đăng nhập." }
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
        GoogleAccountAuth.shareProfile(googleAccount, context)
        if (!accountProfileExists) {
            AccountPrivacyConsent.saveRequiredAccountDataConsent()
        }
        AccountPrivacyConsent.applyCollectionSettings(context, AccountPrivacyChoice())
        AccountActivityTracker.stopAndWait(context)
        FirebaseAccountAdministration.savePreferredName(profileName)
        AccountProfileStore.setActive(context, googleAccount.email)
        accountProfileReady = true
        accountProfileReadyForEmail = googleAccount.email
        googleSignInSubmitting = false
        accountProfileError = null
        activeProfile = googleAccount.email
        interactiveGoogleSignIn = false
        selectedTab = 2
        jsonSection = 0
        googleAuthError = null
        snackbar.showAppSnackbar(context, "Đăng nhập Google thành công.")
    }

    fun leaveGoogleAccount(message: String) {
        scope.launch {
            AccountActivityTracker.stopAndWait(context)
            AccountPrivacyConsent.applyCollectionSettings(context, AccountPrivacyChoice())
            GoogleAccountAuth.signOut(context)
            AccountProfileStore.setActive(context, null)
            interactiveGoogleSignIn = false
            activeProfile = "local"
            selectedTab = 2
            jsonSection = 0
            snackbar.showAppSnackbar(context, message)
        }
    }



    fun signInWithGoogle() {
        if (googleSignInSubmitting || interactiveGoogleSignIn) return
        if (!loginTermsAccepted) {
            scope.launch {
                snackbar.showAppSnackbar(context, "Bạn cần xác nhận Điều khoản sử dụng và Chính sách quyền riêng tư trước khi đăng nhập.")
            }
            return
        }
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
                        ?: "Không thể hoàn tất đăng nhập. Hãy kiểm tra quyền truy cập tài khoản và kết nối mạng."
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
                        text = "COC-LIO",
                        fontSize = 23.sp,
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
            CompositionLocalProvider(LocalRippleConfiguration provides null) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f),
                    contentColor = MaterialTheme.colorScheme.onSurface
                ) {
                val navItems = navigationItems
                navItems.forEach { item ->
                    val navigationIcon = when (item.id) {
                        "home" -> Icons.Outlined.Home
                        "buildings" -> Icons.Outlined.AccountBalanceWallet
                        "history" -> Icons.Outlined.Code
                        "lookup" -> Icons.Outlined.Search
                        else -> Icons.Outlined.Settings
                    }
                    NavigationBarItem(
                        selected = selectedTab == item.page,
                        onClick = { selectedTab = item.page },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            indicatorColor = Color.Transparent
                        ),
                        icon = {
                            Box(
                                modifier = Modifier
                                    .size(42.dp, 34.dp)
                                    .then(
                                        if (selectedTab == item.page) {
                                            Modifier.ledRgbBorder(cornerRadiusDp = 10f, strokeWidthDp = 1.8f, brightnessIndependent = true)
                                        } else {
                                            Modifier
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = navigationIcon,
                                    contentDescription = item.title,
                                    modifier = Modifier.size(22.dp)
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
                    isBusy = googleSignInSubmitting || interactiveGoogleSignIn,
                    readError = accountProfileError,
                    termsAccepted = loginTermsAccepted,
                    onTermsAcceptedChange = { loginTermsAccepted = it },
                    onGoogleSignIn = { signInWithGoogle() }
                )
            }
        } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
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
                    2 -> Column(Modifier.fillMaxSize()) {
                        JsonSectionTabs(selected = jsonSection, onSelected = { jsonSection = it })
                        Box(Modifier.weight(1f).fillMaxWidth()) {
                            when (jsonSection) {
                                0 -> current?.let { OverviewScreen(snapshot = it) } ?: EmptyWelcome(
                                    onImport = { requestImportJson() },
                                    onOpenGame = { openClashOfClans(context) }
                                )
                                1 -> current?.let {
                                    BuildingsAndArmyScreen(
                                        snapshot = it
                                    )
                                } ?: EmptyWelcome(
                                    onImport = { requestImportJson() },
                                    onOpenGame = { openClashOfClans(context) }
                                )
                                else -> HistoryScreen(
                        history = history,
                        deletedVillages = deletedVillages,
                        pinnedTags = pinnedTags,
                        activeTag = current?.tag.orEmpty(),
                        activeTimestamp = current?.timestamp,
                        onSelect = {
                            current = it
                            selectedTab = 2
                            jsonSection = 0
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
                                scope.launch { snackbar.showAppSnackbar(context, "Đã chuyển $tag vào mục ĐÃ XÓA.") }
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
                            }
                        }
                    }
                    3 -> ClashLookupScreen()
                    4 -> SettingsScreen(
                        accountEmail = activeProfile.takeUnless { it == "local" },
                        isAdmin = FirebaseAccountAdministration.isOwner(FirebaseAuth.getInstance().currentUser?.uid),
                        accountData = AccountProfileStore.googleData(context, activeProfile),
                        preferredName = AccountProfileStore.preferredName(context, activeProfile),
                        lastUsedLabel = lastUsedAt.takeIf { it > 0L }?.let { formatTimestamp(it / 1000L) },
                        knownProfiles = AccountProfileStore.knownProfiles(context, activeProfile),
                        themeMode = themeMode,
                        onThemeModeChanged = onThemeModeChanged,
                        textScale = appFontScale,
                        displaySizeLevel = displaySizeLevel,
                        onDisplaySizeLevelChanged = { level ->
                            displaySizeLevel = level.coerceIn(1, 10)
                            AppPreferences.setDisplaySizeLevel(context, displaySizeLevel)
                            AppWidgetUpdater.updateAll(context)
                        },
                        notificationsEnabled = notificationsEnabled,
                        bubbleNotificationsEnabled = bubbleNotificationsEnabled,
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
                        onBubbleNotificationsChanged = { enabled ->
                            if (!enabled) {
                                disableBubbleNotifications(activeProfile)
                            } else {
                                val notificationAllowed = Build.VERSION.SDK_INT < 33 ||
                                    ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.POST_NOTIFICATIONS
                                    ) == PackageManager.PERMISSION_GRANTED
                                if (!notificationAllowed) {
                                    bubbleNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else if (
                                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                                    !android.provider.Settings.canDrawOverlays(context)
                                ) {
                                    bubbleOverlayPermissionLauncher.launch(bubbleOverlayPermissionIntent())
                                } else {
                                    enableBubbleNotifications(activeProfile)
                                }
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
                        modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
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
    isBusy: Boolean,
    readError: String?,
    termsAccepted: Boolean,
    onTermsAcceptedChange: (Boolean) -> Unit,
    onGoogleSignIn: () -> Unit
) {
    var showDataDetails by rememberSaveable { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current

    Box(
        modifier = modifier.padding(10.dp),
        contentAlignment = Alignment.Center
    ) {
        NeonPanel(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            glow = CocCyan,
            innerPadding = 14.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                LedBrandTitle(
                    text = "COC-LIO",
                    fontSize = 26.sp,
                    letterSpacing = 2.sp,
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
                Divider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .ledRgbBorder(
                                cornerRadiusDp = 8f,
                                strokeWidthDp = if (termsAccepted) 0f else 2f
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Checkbox(
                            checked = termsAccepted,
                            onCheckedChange = onTermsAcceptedChange,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                    Text(
                        "Tôi đã đọc và đồng ý với Điều khoản sử dụng và Chính sách quyền riêng tư.",
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onTermsAcceptedChange(!termsAccepted) }
                            .padding(start = 7.dp),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = {
                        runCatching {
                            uriHandler.openUri("https://coc-lio-73de9.firebaseapp.com/terms")
                        }
                    }) {
                        Text("ĐIỀU KHOẢN", fontSize = 9.sp)
                    }
                    TextButton(onClick = {
                        runCatching {
                            uriHandler.openUri("https://coc-lio-73de9.firebaseapp.com/privacy")
                        }
                    }) {
                        Text("CHÍNH SÁCH RIÊNG TƯ", fontSize = 9.sp)
                    }
                }
                TextButton(
                    onClick = { showDataDetails = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("XEM CHI TIẾT DỮ LIỆU TÀI KHOẢN", fontSize = 10.sp)
                }
                Divider(color = MaterialTheme.colorScheme.outlineVariant)
                NeonButton(
                    text = if (isBusy) "ĐANG MỞ GOOGLE..." else "ĐĂNG NHẬP GOOGLE",
                    onClick = onGoogleSignIn,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isBusy && termsAccepted,
                    showLed = termsAccepted && !isBusy
                )
            }
        }
        if (showDataDetails) {
            AlertDialog(
                onDismissRequest = { showDataDetails = false },
                title = {
                    Text(
                        "DỮ LIỆU COC-LIO XỬ LÝ",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        "Khi bạn đăng nhập Google, COC-LIO dùng dịch vụ xác thực của Google. Hồ sơ trực tuyến gồm UID, email, tên và ảnh hồ sơ Google, trạng thái xác minh, tên hiển thị, số lần đăng nhập và các mốc tạo/cập nhật/đăng nhập.\n\n" +
                            "Để quản lý tài khoản, bảo mật phiên và hỗ trợ tương thích, ứng dụng cũng đồng bộ thông tin kỹ thuật cơ bản: hãng/model, thương hiệu, mã thiết bị do Android cung cấp, kiến trúc CPU, phiên bản Android và ứng dụng, ngôn ngữ, múi giờ, kích thước màn hình/mật độ điểm ảnh và loại/trạng thái kết nối mạng tại thời điểm đồng bộ. Các quyền INTERNET và ACCESS_NETWORK_STATE được Android tự cấp khi cài đặt, không hiện hộp thoại xin quyền.\n\n" +
                            "JSON Clash of Clans, Player Tag, tên làng, công trình và tiến trình được xử lý và lưu trên điện thoại; ứng dụng không tải JSON lên máy chủ. Firebase Analytics, Crashlytics và Performance Monitoring không được bật. Ứng dụng không thu thập IMEI, số sê-ri, MAC, Android ID, vị trí GPS, danh bạ, SMS hoặc lịch sử cuộc gọi.\n\n" +
                            "Thông tin kỹ thuật chỉ được dùng để duy trì tài khoản, bảo mật và hỗ trợ tương thích/kết nối; không theo dõi thao tác chi tiết trên từng màn hình. Những bản JSON từng được đồng bộ từ phiên bản cũ (nếu có) không tự động bị xóa khi cập nhật ứng dụng.",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 11.sp,
                        lineHeight = 17.sp
                    )
                },
                confirmButton = {
                    TextButton(onClick = { showDataDetails = false }) {
                        Text("ĐÃ HIỂU")
                    }
                }
            )
        }
    }
}

@Composable
private fun JsonSectionTabs(
    selected: Int,
    onSelected: (Int) -> Unit
) {
    val labels = listOf("TỔNG QUAN", "THÔNG TIN", "DỮ LIỆU JSON")
    val icons = listOf(
        Icons.Outlined.Home,
        Icons.Outlined.AccountBalanceWallet,
        Icons.Outlined.Code
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        labels.forEachIndexed { index, label ->
            FilterChip(
                selected = selected == index,
                onClick = { onSelected(index) },
                label = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = icons[index],
                            contentDescription = label,
                            tint = if (selected == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        MaterialText(
                            label,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }
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
        CocGameIcon(R.drawable.ic_coc_home, Modifier.size(62.dp), "Làng Clash of Clans")
        Spacer(Modifier.height(14.dp))
        Text("Bắt đầu theo dõi", color = MaterialTheme.colorScheme.onSurface, fontSize = 21.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            "Sau khi nhập JSON làng, COC-LIO sẽ tổng hợp tiến trình, cấp độ công trình, quân đội và thời gian nâng cấp.",
            color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, lineHeight = 18.sp
        )
        Spacer(Modifier.height(18.dp))
        NeonButton(text = "MỞ CLASH OF CLANS", onClick = onOpenGame, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(10.dp))
        NeonButton(text = "NHẬP JSON ĐẦU TIÊN", onClick = onImport, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        Text("Nút MỞ CLASH OF CLANS đồng thời bật cửa sổ nhập JSON nổi. Cấp quyền Hiển thị trên ứng dụng khác nếu được yêu cầu; kéo tiêu đề để di chuyển và kéo góc ↘ để thu phóng.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, lineHeight = 15.sp)
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
                        CocGameIcon(
                            R.drawable.ic_coc_tag,
                            Modifier.size(10.dp),
                            AppText.translate(context, "Mã người chơi")
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
private fun GameCategorySectionTitle(key: String, title: String, trailing: String) {
    val icon = when (key) {
        "defense" -> Icons.Outlined.Security
        "resources" -> Icons.Outlined.AccountBalanceWallet
        "army", "troops" -> Icons.Outlined.MilitaryTech
        "walls" -> Icons.Outlined.GridView
        "traps" -> Icons.Outlined.WarningAmber
        "guardians" -> Icons.Outlined.Shield
        "heroes" -> Icons.Outlined.Star
        "spells" -> Icons.Outlined.AutoAwesome
        "pets" -> Icons.Outlined.Pets
        "equipment" -> Icons.Outlined.Build
        else -> Icons.Outlined.Category
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 5.dp, bottom = 1.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = title, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(19.dp))
        Spacer(Modifier.width(7.dp))
        Text(title, color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        Text(trailing, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
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
    snapshot: VillageSnapshot
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
    val context = LocalContext.current
    var upgradeCatalog by remember(context) {
        mutableStateOf<Map<Int, CocUpgradeEntry>>(emptyMap())
    }
    var supplementalCatalog by remember(context) {
        mutableStateOf<Map<Int, SupplementalCatalogEntry>>(emptyMap())
    }
    LaunchedEffect(context) {
        val catalogs = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            CocUpgradeCatalog.all(context) to LocalSupplementalCatalog.load(context)
        }
        upgradeCatalog = catalogs.first
        supplementalCatalog = catalogs.second
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
    var showMaxPlan by remember(snapshot.tag, snapshot.timestamp, selectedBase) {
        mutableStateOf(false)
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
    val snapshotBaseItems = snapshot.items.filter { it.base == selectedBase && it.tab == tab }
    val catalogArmyItems = if (tab == "army") {
        upgradeCatalog.values.asSequence()
            .filter { it.base == selectedBase }
            .filter { it.category in setOf("troop", "siege-machine", "hero", "spell", "pet", "guardian", "hero-equipment", "squad") }
            .filterNot { entry -> snapshotBaseItems.any { it.dataId == entry.dataId && it.base == entry.base } }
            .map(::catalogVillageItem)
            .toList()
    } else {
        emptyList()
    }
    val baseItems = snapshotBaseItems + catalogArmyItems

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
            ?: supplementalCatalog[item.dataId]?.category
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
            "squad" -> "squads"
            else -> supplementalCatalog[item.dataId]?.category
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
            "squads" to "Đội hình Squad",
            "other" to "Khác"
        ).filter { (key, _) -> key == "all" || baseItems.any { armyCategory(it) == key } }
    }

    fun rarityFor(item: VillageItem): String? =
        upgradeCatalog[item.dataId]?.rarity?.takeIf { it.isNotBlank() }
            ?: supplementalCatalog[item.dataId]?.rarity?.takeIf { it.isNotBlank() }
            ?: if (item.sectionKey == "equipment") equipmentRarity(item.dataId) else null

    val categoryFiltered = when {
        selectedFilter == "all" -> baseItems
        tab == "buildings" -> baseItems.filter { buildingCategory(it) == selectedFilter }
        else -> baseItems.filter { armyCategory(it) == selectedFilter }
    }
    val rarityItems = categoryFiltered.filter { rarityFor(it) != null }
    val hasAnnouncedBlizzardAxe = tab == "army" && selectedBase == "home" &&
        (selectedFilter == "all" || selectedFilter == "equipment")
    val rarityChoices = listOf(
        "all" to "TẤT CẢ",
        "common" to "THƯỜNG",
        "rare" to "HIẾM",
        "epic" to "SỬ THI",
        "legendary" to "HUYỀN THOẠI"
    ).filter { (key, _) ->
        key == "all" || rarityItems.any { rarityFor(it) == key } ||
            (key == "legendary" && hasAnnouncedBlizzardAxe)
    }
    val filtered = if (tab == "army" && selectedRarity != "all") {
        categoryFiltered.filter { rarityFor(it) == selectedRarity }
    } else {
        categoryFiltered
    }
    val showAnnouncedBlizzardAxe = hasAnnouncedBlizzardAxe &&
        (selectedRarity == "all" || selectedRarity == "legendary")
    val showSuperTroops = tab == "army" && selectedBase == "home" &&
        (selectedFilter == "all" || selectedFilter == "troops") && selectedRarity == "all"

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
        "squads" -> "ĐỘI HÌNH SQUAD • TỔ HỢP QUÂN"
        else -> rows.firstOrNull()?.sectionLabel?.uppercase().orEmpty()
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
                        TextButton(
                            onClick = { showMaxPlan = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "TÍNH TỔNG TÀI NGUYÊN ĐỂ MAX LÀNG",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black
                            )
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
                                    val count = when {
                                        key == "all" -> rarityItems.size + if (hasAnnouncedBlizzardAxe) 1 else 0
                                        else -> rarityItems.count { rarityFor(it) == key } +
                                            if (key == "legendary" && hasAnnouncedBlizzardAxe) 1 else 0
                                    }
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

            if (baseItems.isEmpty() || (filtered.isEmpty() && !showAnnouncedBlizzardAxe)) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    InfoCard("Chưa có dữ liệu", "JSON không có mục thuộc nhóm này.")
                }
            } else {
                val grouped = filtered.groupBy(::groupKey).toMutableMap().apply {
                    if (showAnnouncedBlizzardAxe) putIfAbsent("equipment", emptyList())
                }
                grouped.forEach { (key, rows) ->
                    item(key = "led_group_$key", span = { GridItemSpan(maxLineSpan) }) {
                        val ordered = rows.filter { it.timerSeconds == null }.sortedWith(
                            compareBy<VillageItem> { it.count <= 0 }
                                .thenBy { it.level ?: Int.MAX_VALUE }
                                .thenBy { it.name.lowercase() }
                        ) + rows.filter { it.timerSeconds != null }
                            .sortedBy { it.timerSeconds ?: Long.MAX_VALUE }
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(7.dp)
                            ) {
                                val sectionCount = when {
                                    tab != "army" -> rows.sumOf { it.count }.toString()
                                    key == "squads" -> {
                                        val confirmedRows = rows.filter { it.properties["catalogOnly"] != "true" }
                                        val confirmedOwned = confirmedRows.count {
                                            it.count > 0 || (it.timerSeconds ?: 0L) > 0L
                                        }
                                        val confirmedMissing = confirmedRows.size - confirmedOwned
                                        val unknown = rows.size - confirmedRows.size
                                        "${rows.size} ĐỘI HÌNH • ĐÃ CÓ $confirmedOwned • CHƯA CÓ $confirmedMissing • CHƯA XÁC NHẬN $unknown"
                                    }
                                    else -> {
                                        val ownedCount = if (key == "equipment") {
                                            rows.count { it.hasConfirmedEquipmentOwnership }
                                        } else {
                                            rows.count {
                                                it.properties["catalogOnly"] != "true" &&
                                                    (it.count > 0 || (it.timerSeconds ?: 0L) > 0L)
                                            }
                                        }
                                        val unknownCount = if (key == "equipment") {
                                            rows.size - ownedCount
                                        } else {
                                            rows.count { it.properties["catalogOnly"] == "true" }
                                        }
                                        val countText = if (key == "equipment") {
                                            "$ownedCount/${rows.size} ĐÃ XÁC NHẬN • $unknownCount CHƯA XÁC NHẬN"
                                        } else if (unknownCount > 0) {
                                            "$ownedCount/${rows.size} • $unknownCount CHƯA XÁC NHẬN"
                                        } else {
                                            "$ownedCount/${rows.size}"
                                        }
                                        if (key == "equipment" && showAnnouncedBlizzardAxe) {
                                            "$countText • +1 SẮP RA MẮT"
                                        } else {
                                            countText
                                        }
                                    }
                                }
                                GameCategorySectionTitle(key, groupTitle(key, rows), sectionCount)
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
                                                supplemental = supplementalCatalog[item.dataId],
                                                upgrade = upgradeCatalog[item.dataId],
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                selectedItem = item
                                            }
                                        }
                                        if (rowItems.size == 1) Spacer(Modifier.weight(1f))
                                    }
                                }
                                if (key == "troops" && showSuperTroops) {
                                    SuperTroopsRoster(baseItems, upgradeCatalog)
                                }
                                if (key == "equipment") {
                                    if (showAnnouncedBlizzardAxe) {
                                        AnnouncedBlizzardAxeCard()
                                    }
                                    VillageItemDetailField("LƯU Ý VỀ CHI PHÍ EPIC", EQUIPMENT_COST_DATA_NOTICE)
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
                supplemental = supplementalCatalog[item.dataId],
                blacksmithLevel = blacksmithLevel,
                hallLevel = hallLevel,
                upgradeCatalog = upgradeCatalog,
                allVillageItems = snapshot.items
            ) {
                selectedItem = null
            }
        }
        if (showMaxPlan) {
            VillageMaxPlanDialog(
                snapshot = snapshot,
                base = selectedBase,
                upgradeCatalog = upgradeCatalog
            ) {
                showMaxPlan = false
            }
        }
    }
}

@Composable
private fun VillageMaxPlanDialog(
    snapshot: VillageSnapshot,
    base: String,
    upgradeCatalog: Map<Int, CocUpgradeEntry>,
    onDismiss: () -> Unit
) {
    val nowSeconds = remember(snapshot.timestamp, base) {
        System.currentTimeMillis() / 1000L
    }
    val plan = remember(snapshot, base, upgradeCatalog, nowSeconds) {
        CocVillageMaxPlanCalculator.calculate(
            snapshot = snapshot,
            base = base,
            catalog = upgradeCatalog,
            nowSeconds = nowSeconds
        )
    }
    val remainingCopies = plan.lines.sumOf { it.quantity }
    val currentHallLabel = if (base == "builder") {
        snapshot.builderHallLevel?.let { "Nhà thợ xây cấp $it" } ?: "Cấp Nhà thợ xây chưa rõ"
    } else {
        snapshot.townHallLevel?.let { "Nhà chính cấp $it" } ?: "Cấp Nhà chính chưa rõ"
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 660.dp)
                .ledRgbBorder(cornerRadiusDp = 18f, strokeWidthDp = 2.6f, brightnessIndependent = true),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "KẾ HOẠCH TÀI NGUYÊN LÊN MAX",
                    color = CocGold,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black
                )
                VillageItemDetailField(
                    "Phạm vi",
                    (if (base == "builder") "Toàn bộ Căn cứ thợ xây" else "Toàn bộ Làng chính") + " • " + currentHallLabel
                )
                VillageItemDetailField(
                    "Tiến độ",
                    "${plan.maxedCopies} đã MAX • ${remainingCopies} còn cần nâng • ${plan.ownedCopies} bản sao đã xác nhận"
                )
                Text(
                    "TỔNG TÀI NGUYÊN CÒN CẦN",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black
                )
                if (plan.resources.isEmpty()) {
                    VillageItemDetailField(
                        "Tài nguyên",
                        if (remainingCopies == 0) "Không còn nâng cấp thường cần thực hiện." else "Chưa có chi phí còn lại được xác nhận."
                    )
                } else {
                    plan.resources.forEach { (resource, amount) ->
                        UpgradeResourceChip(resource, amount)
                    }
                }
                if (plan.durationSeconds > 0L) {
                    VillageIconDetailField(
                        R.drawable.ic_coc_time,
                        "Thời gian cộng dồn",
                        formatDuration(plan.durationSeconds)
                    )
                }
                Text(
                    "CÁC MỤC CÒN CẦN NÂNG",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black
                )
                if (plan.lines.isEmpty()) {
                    VillageItemDetailField(
                        "Trạng thái",
                        "Không có mục đã xác nhận sở hữu cần nâng cấp theo JSON làng hiện tại."
                    )
                } else {
                    plan.lines.groupBy { it.groupLabel }.forEach { (group, rows) ->
                        Text(
                            group.uppercase(),
                            color = CocGold,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black
                        )
                        rows.forEach { line ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                        RoundedCornerShape(10.dp)
                                    )
                                    .border(
                                        1.dp,
                                        MaterialTheme.colorScheme.outlineVariant,
                                        RoundedCornerShape(10.dp)
                                    )
                                    .padding(9.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    "${line.name} • cấp ${line.currentLevel} → MAX ${line.targetLevel}" +
                                        if (line.quantity > 1) " • x${line.quantity}" else "",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                line.resources.forEach { (resource, amount) ->
                                    UpgradeResourceChip(resource, amount)
                                }
                                if (line.durationSeconds > 0L) {
                                    VillageIconDetailField(
                                        R.drawable.ic_coc_time,
                                        "Thời gian còn lại",
                                        formatDuration(line.durationSeconds)
                                    )
                                }
                                if (line.assumedCurrentLevel) {
                                    VillageItemDetailField(
                                        "Cấp hiện tại",
                                        "JSON không có cấp; tạm tính từ cấp mở khóa đầu tiên."
                                    )
                                }
                                if (line.requirements.isNotEmpty()) {
                                    VillageItemDetailField(
                                        "Điều kiện nâng cấp",
                                        line.requirements.joinToString(" • ") {
                                            "${it.label} cấp ${it.level}"
                                        }
                                    )
                                }
                                if (line.upgradeInProgress) {
                                    VillageItemDetailField(
                                        "Trạng thái",
                                        if (line.upgradeTimerExpired) {
                                            "Timer trong JSON đã hết; chi phí đã được trừ. Hãy nhập JSON mới để xác nhận cấp hiện tại."
                                        } else {
                                            "Đã trừ chi phí của một bản sao đang nâng; chỉ cộng thời gian còn lại của bản sao đó."
                                        }
                                    )
                                }
                                if (line.missingLevels.isNotEmpty()) {
                                    VillageItemDetailField(
                                        "Thiếu dữ liệu cấp",
                                        line.missingLevels.joinToString(", ") +
                                            ". Tổng tài nguyên hiện chỉ cộng các cấp có dữ liệu."
                                    )
                                }
                            }
                        }
                    }
                }
                if (plan.incompleteItems > 0) {
                    VillageItemDetailField(
                        "Cảnh báo dữ liệu",
                        "${plan.incompleteItems} mục còn thiếu dữ liệu cấp nâng cấp; tổng chi phí tương ứng chưa thể xem là đầy đủ."
                    )
                }
                if (plan.unconfirmedItems > 0) {
                    VillageItemDetailField(
                        "Chưa xác nhận sở hữu",
                        "${plan.unconfirmedItems} mục chưa được cộng vì JSON không xác nhận đã sở hữu hoặc đang nâng cấp."
                    )
                }
                if (plan.itemsWithoutCatalogData > 0) {
                    VillageItemDetailField(
                        "Thiếu danh mục",
                        "${plan.itemsWithoutCatalogData} mục đã có trong JSON nhưng chưa có dữ liệu nâng cấp để tính."
                    )
                }
                if (plan.gearUpItems > 0) {
                    VillageItemDetailField(
                        "Gear-up ngoài MAX thường",
                        "${plan.gearUpItems} công trình có timer gear-up; thời gian gear-up không được cộng vào kế hoạch nâng cấp cấp thường."
                    )
                }
                VillageItemDetailField(
                    "Phạm vi xác nhận",
                    "Chỉ tính các mục xuất hiện trong JSON và được xác nhận sở hữu; mục vắng mặt trong bản xuất không được tự suy đoán là đã sở hữu."
                )
                VillageItemDetailField(
                    "Cách tính",
                    "Công trình, bẫy và Tường tính theo số bản sao rảnh (cnt) cộng thêm một bản sao nếu đang nâng cấp (timer). Gear-up không cộng vào thời gian MAX cấp thường. Lính, Thần chú, Anh hùng và nghiên cứu chỉ tính một lần, không nhân theo số quân trong kho."
                )
                VillageItemDetailField(
                    "Số dư tài nguyên hiện tại",
                    "JSON xuất từ game không cung cấp số dư Gold, Elixir, Dark Elixir hoặc Quặng. Các số phía trên là tổng chi phí nâng cấp còn phải trả, chưa trừ tài nguyên đang có trong kho."
                )
                VillageItemDetailField(
                    "Độ chính xác danh mục",
                    "Dữ liệu nâng cấp dựa trên catalog cộng đồng và có thể chậm cập nhật một số chi phí cũ sau cân bằng game. Hãy đối chiếu trong game trước khi lập kế hoạch cuối cùng."
                )
                VillageItemDetailField(
                    "Lưu ý thời gian",
                    "Đây là tổng thời gian công việc cộng dồn, chưa trừ thời gian chạy song song của nhiều thợ xây/phòng thí nghiệm, Sách, Búa, trợ thủ hoặc tăng tốc."
                )
                VillageItemDetailField(
                    "Siêu cường hóa",
                    "Chi phí Siêu cường hóa được xem riêng trong chi tiết từng công trình và chưa cộng vào tổng MAX thường; JSON làng hiện không xác nhận bậc đã hoàn thành."
                )
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("ĐÓNG")
                }
            }
        }
    }
}

private data class SuperTroopVariant(val name: String, val baseDataId: Int)

private val superTroopVariants = listOf(
    SuperTroopVariant("Siêu Man di", 4000000),
    SuperTroopVariant("Siêu Cung thủ", 4000001),
    SuperTroopVariant("Siêu Khổng nhân", 4000003),
    SuperTroopVariant("Yêu tinh Tàng hình", 4000002),
    SuperTroopVariant("Siêu Công thành Binh", 4000004),
    SuperTroopVariant("Khí cầu Hỏa tiễn", 4000005),
    SuperTroopVariant("Siêu Pháp sư", 4000006),
    SuperTroopVariant("Rồng siêu cấp", 4000008),
    SuperTroopVariant("Hỏa Ngục Long", 4000023),
    SuperTroopVariant("Siêu Thợ mỏ", 4000024),
    SuperTroopVariant("Người tuyết Yeti siêu cấp", 4000053),
    SuperTroopVariant("Siêu Minion", 4000010),
    SuperTroopVariant("Siêu Trư Kỵ sĩ", 4000011),
    SuperTroopVariant("Siêu Valkyrie", 4000012),
    SuperTroopVariant("Siêu Phù thủy", 4000015),
    SuperTroopVariant("Băng khuyển", 4000017),
    SuperTroopVariant("Siêu Thạch thủ", 4000022)
)

@Composable
private fun SuperTroopsRoster(
    baseItems: List<VillageItem>,
    upgradeCatalog: Map<Int, CocUpgradeEntry>
) {
    val itemsById = baseItems.associateBy { it.dataId }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.72f), RoundedCornerShape(12.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
            .padding(9.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Text("SIÊU QUÂN • 17 BIẾN THỂ", color = MaterialTheme.colorScheme.primary, fontSize = 10.sp, fontWeight = FontWeight.Black)
        superTroopVariants.chunked(2).forEach { pair ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                pair.forEach { variant ->
                    val baseItem = itemsById[variant.baseDataId]
                    val baseEntry = upgradeCatalog[variant.baseDataId]
                    val cap = baseEntry?.let { CocUpgradeCatalog.currentMaxLevel(it) }
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.58f), RoundedCornerShape(9.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(9.dp))
                            .padding(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        if (baseItem != null) CategoryImage(baseItem, 32)
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(variant.name, color = MaterialTheme.colorScheme.onSurface, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            val levelText = when {
                                baseItem?.level != null && cap != null -> "Cấp quân gốc " + baseItem.level + " / " + cap
                                baseItem?.level != null -> "Cấp quân gốc " + baseItem.level
                                cap != null -> "Cấp quân gốc chưa nhập • MAX " + cap
                                else -> "Chưa có dữ liệu cấp quân gốc"
                            }
                            Text(levelText, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.sp)
                        }
                    }
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        VillageItemDetailField(
            "Cách tính cấp và sở hữu",
            "Siêu quân dùng cấp nghiên cứu của quân gốc, không có cấp nâng riêng. JSON làng không xác nhận siêu quân nào đang được kích hoạt; trạng thái kích hoạt phải kiểm tra trong game."
        )
    }
}

@Composable
private fun VillageGridItemCard(
    item: VillageItem,
    supplemental: SupplementalCatalogEntry?,
    upgrade: CocUpgradeEntry?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val itemName = item.name
    val cardMaxLevel = upgrade?.let { CocUpgradeCatalog.currentMaxLevel(it) }
        ?: supplemental?.maxLevel
    val isEquipment = upgrade?.category == "hero-equipment" || item.sectionKey == "equipment"
    val isSquad = upgrade?.category == "squad"
    val rarity = upgrade?.rarity?.takeIf { it.isNotBlank() }
        ?: supplemental?.rarity?.takeIf { it.isNotBlank() }
        ?: if (isEquipment) equipmentRarity(item.dataId) else null
    val heroId = if (isEquipment) {
        upgrade?.hero?.takeIf { it.isNotBlank() } ?: equipmentHeroId(item.dataId)
    } else null
    val heroName = heroId?.let(::equipmentHeroName)
    val cardShape = RoundedCornerShape(13.dp)
    val rarityColor = equipmentRarityColor(rarity)

    Column(
        modifier = modifier
            .heightIn(min = if (isEquipment) 148.dp else 112.dp)
            .ledRgbBorder(cornerRadiusDp = 13f, strokeWidthDp = 2.2f, brightnessIndependent = true)
            .clip(cardShape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f))
            .border(
                if (isEquipment && rarityColor != null) 1.7.dp else 1.dp,
                rarityColor ?: MaterialTheme.colorScheme.outlineVariant,
                cardShape
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(
                    if (isEquipment && rarityColor != null) 2.dp else 1.dp,
                    rarityColor ?: MaterialTheme.colorScheme.outlineVariant,
                    RoundedCornerShape(10.dp)
                )
                .padding(2.dp),
            contentAlignment = Alignment.Center
        ) {
            if (supplemental?.imageUrl?.isNotBlank() == true) {
                AsyncImage(
                    model = supplemental.imageUrl,
                    contentDescription = itemName,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            } else {
                CategoryImage(item, 46)
            }
        }
        Text(
            itemName,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            lineHeight = 14.sp
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                if (isSquad) {
                    val unlockTownHall = upgrade?.properties?.optInt("townHallRequired", 0)?.takeIf { it > 0 }
                    val catalogOnly = item.properties["catalogOnly"] == "true"
                    val owned = item.count > 0 || (item.timerSeconds ?: 0L) > 0L
                    val status = when {
                        owned -> "ĐÃ CÓ"
                        catalogOnly -> "CHƯA XÁC NHẬN"
                        else -> "CHƯA CÓ"
                    }
                    val level = item.level?.let { "CẤP $it/${cardMaxLevel ?: "?"}" }
                        ?: if (owned) "CẤP CHƯA RÕ • MAX ${cardMaxLevel ?: "?"}"
                        else "MAX ${cardMaxLevel ?: "?"}"
                    if (unlockTownHall != null) "$status • MỞ TỪ TH$unlockTownHall • $level"
                    else "$status • SQUAD • $level"
                } else if (isEquipment && !item.hasConfirmedEquipmentOwnership) {
                    if (item.properties["catalogOnly"] == "true") {
                        "CHƯA XÁC NHẬN • MAX ${cardMaxLevel ?: "?"}"
                    } else {
                        "CHƯA XÁC NHẬN • CẤP ${item.level ?: "?"} • MAX ${cardMaxLevel ?: "?"}"
                    }
                } else if (item.properties["catalogOnly"] == "true") {
                    "CHƯA XÁC NHẬN • MAX ${cardMaxLevel ?: "?"}"
                } else if (item.count <= 0 && (item.timerSeconds ?: 0L) <= 0L) {
                    "CHƯA SỞ HỮU • MAX ${cardMaxLevel ?: "?"}"
                } else {
                    item.level?.let { level ->
                        cardMaxLevel?.let { "Cấp $level / $it" } ?: "Cấp $level"
                    } ?: "Chưa rõ cấp"
                },
                color = when {
                    isEquipment && !item.hasConfirmedEquipmentOwnership -> CocGold
                    item.properties["catalogOnly"] == "true" -> CocGold
                    item.count <= 0 && !isSquad && !isEquipment -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.primary
                },
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            if (item.count > 1 && !isEquipment) {
                Text(
                    "  ×${item.count}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
            }
            if (item.gearUp) {
                Spacer(Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Outlined.Build,
                    contentDescription = "Đã ghép công trình",
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(13.dp)
                )
            }
        }
        if (isEquipment) {
            if (rarity != null) ItemRarityBadge(rarity)
            if (heroId != null && heroName != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    val heroImage = CocAssets.heroImageUrl(heroId)
                    if (heroImage != null) {
                        AsyncImage(
                            model = heroImage,
                            contentDescription = heroName,
                            modifier = Modifier.size(19.dp).clip(RoundedCornerShape(5.dp)),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        CocGameIcon(R.drawable.ic_coc_hero, Modifier.size(17.dp), heroName)
                    }
                    Spacer(Modifier.width(4.dp))
                    Text(
                        heroName,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        } else {
            Text(
                "Xem chi tiết",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 9.sp,
                maxLines = 1
            )
        }
    }
}

private fun equipmentHeroName(heroId: String): String =
    CocUpgradeCatalog.localizedHeroName(heroId)


internal val EquipmentDescriptionsVi = mapOf(
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

internal val CatalogDescriptionsVi = mapOf(
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

internal fun localizedCatalogField(label: String, value: String): Pair<String, String> {
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
        else -> localizedCatalogText(value)
    }
    return translatedLabel to translatedValue
}

private fun localizedCatalogText(value: String): String {
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

private const val EQUIPMENT_COST_DATA_NOTICE =
    "Chi phí Common/Epic hiện có dùng snapshot cộng đồng ngày 15/11/2026. Supercell ngày 17/11/2026 công bố cấu trúc chi phí mới cho Legendary và Epic phát hành trong tương lai; Epic cũ cũng sẽ được giảm chi phí dần theo thời gian nhưng chưa có lịch cụ thể. Vì vậy, số liệu trong ứng dụng là mốc tham khảo hiện tại, cần đối chiếu trong game trước khi lập kế hoạch quặng."

@Composable
private fun AnnouncedBlizzardAxeCard() {
    var expanded by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.72f), RoundedCornerShape(10.dp))
            .border(1.4.dp, CocGold.copy(alpha = 0.9f), RoundedCornerShape(10.dp))
            .padding(9.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("❄️", fontSize = 30.sp)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    "Rìu Bão Tuyết (Blizzard Axe)",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    ItemRarityBadge("legendary")
                    Text(
                        "ĐÃ CÔNG BỐ • CHƯA PHÁT HÀNH",
                        color = CocGold,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black
                    )
                }
                Text(
                    "Vua man di • MAX 33 • Mở khóa miễn phí từ Nhà chính cấp 8",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 9.sp
                )
            }
            Text(
                if (expanded) "ẨN" else "CHI PHÍ",
                color = CocGold,
                fontSize = 9.sp,
                fontWeight = FontWeight.Black
            )
        }
        if (expanded) {
            Divider(color = MaterialTheme.colorScheme.outlineVariant)
            Text(
                "TỔNG CHI PHÍ CẤP 1 → MAX 33",
                color = CocGold,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black
            )
            UpgradeResourceChip("Shiny Ore", 79760L)
            UpgradeResourceChip("Glowy Ore", 5100L)
            UpgradeResourceChip("Starry Ore", 705L)
            VillageItemDetailField(
                "Điều kiện cấp",
                "Cấp 28–30 cần Nhà chính cấp 16; cấp 31–33 cần Nhà chính cấp 18. Giới hạn còn phụ thuộc cấp Lò rèn."
            )
            VillageItemDetailField(
                "Trạng thái dữ liệu",
                "Supercell đã công bố trang bị và tổng quặng tối đa; kỹ năng và chi tiết từng cấp chưa được công bố. Mục này không tính vào số trang bị đã phát hành hoặc đã sở hữu."
            )
        }
    }
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

private fun equipmentHeroId(dataId: Int): String? = when (dataId) {
    90000000, 90000001, 90000008, 90000011, 90000010, 90000014, 90000032, 90000051 -> "barbarian-king"
    90000002, 90000003, 90000017, 90000020, 90000015, 90000039, 90000048, 90000059 -> "archer-queen"
    90000042, 90000043, 90000044, 90000047, 90000035, 90000049, 90000061 -> "minion-prince"
    90000004, 90000005, 90000024, 90000034, 90000022, 90000041, 90000019 -> "grand-warden"
    90000007, 90000006, 90000009, 90000012, 90000013, 90000040, 90000050 -> "royal-champion"
    90000052, 90000053, 90000056, 90000057, 90000016, 90000060 -> "dragon-duke"
    else -> null
}

private fun equipmentRarityColor(rarity: String?): Color? = when (rarity?.lowercase()) {
    "common" -> Color(0xFF8F9CAB)
    "rare" -> Color(0xFF55AFFF)
    "epic" -> Color(0xFFB58AFF)
    "legendary" -> Color(0xFFFFC857)
    else -> null
}

@Composable
private fun VillageEquipmentHeroField(heroId: String, heroName: String) {
    val imageUrl = CocAssets.heroImageUrl(heroId)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f), RoundedCornerShape(12.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (imageUrl != null) {
            AsyncImage(
                model = imageUrl,
                contentDescription = heroName,
                modifier = Modifier.size(42.dp).clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Fit
            )
        } else {
            CocGameIcon(R.drawable.ic_coc_hero, Modifier.size(38.dp), heroName)
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                "TRANG BỊ DÀNH CHO ANH HÙNG",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                heroName,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}


private fun catalogVillageItem(entry: CocUpgradeEntry): VillageItem {
    val label = when (entry.category) {
        "troop" -> "Quân lính"
        "siege-machine" -> "Máy công thành"
        "hero" -> "Anh hùng"
        "spell" -> "Thần chú"
        "pet" -> "Linh thú"
        "guardian" -> "Hộ vệ"
        "hero-equipment" -> "Trang bị anh hùng"
        "squad" -> "Đội hình Squad"
        else -> entry.category
    }
    return VillageItem(
        sectionKey = entry.sectionKey,
        sectionLabel = label,
        tab = "army",
        base = entry.base,
        dataId = entry.dataId,
        level = null,
        count = 0,
        timerSeconds = null,
        helperCooldownSeconds = null,
        gearUp = false,
        weaponLevel = null,
        details = emptyList(),
        properties = mapOf("catalogOnly" to "true")
    )
}

@Composable
private fun HeroEquipmentRoster(
    heroId: String,
    allVillageItems: List<VillageItem>,
    townHallLevel: Int?,
    blacksmithLevel: Int?,
    upgradeCatalog: Map<Int, CocUpgradeEntry>
) {
    val equipment = upgradeCatalog.values
        .filter { it.base == "home" && it.category == "hero-equipment" && it.hero == heroId }
    val villageEquipmentById = allVillageItems
        .filter { it.base == "home" && it.sectionKey == "equipment" }
        .groupBy { it.dataId }
    val ownedById = villageEquipmentById.mapNotNull { (dataId, rows) ->
        rows.filter { it.hasConfirmedEquipmentOwnership }
            .maxByOrNull { it.level ?: 0 }
            ?.let { dataId to it }
    }.toMap()
    val owned = equipment.filter { ownedById[it.dataId] != null }
        .sortedBy { it.localizedName.lowercase() }
    val unconfirmed = equipment.filter { it.dataId !in ownedById }
        .sortedBy { it.localizedName.lowercase() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f), RoundedCornerShape(12.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            "TRANG BỊ CỦA ANH HÙNG",
            color = MaterialTheme.colorScheme.primary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black
        )
        Text(
            "ĐÃ XÁC NHẬN SỞ HỮU ${owned.size}/${equipment.size}",
            color = Color(0xFF69D69A),
            fontSize = 10.sp,
            fontWeight = FontWeight.Black
        )
        owned.forEach { entry ->
            HeroEquipmentCatalogRow(
                entry = entry,
                villageItem = ownedById[entry.dataId],
                ownershipUnknown = false,
                townHallLevel = townHallLevel,
                blacksmithLevel = blacksmithLevel,
                upgradeCatalog = upgradeCatalog
            )
        }
        if (unconfirmed.isNotEmpty()) {
            Divider(color = MaterialTheme.colorScheme.outlineVariant)
            Text(
                "CHƯA XÁC NHẬN • ${unconfirmed.size}",
                color = CocGold,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black
            )
            unconfirmed.forEach { entry ->
                HeroEquipmentCatalogRow(
                    entry = entry,
                    villageItem = villageEquipmentById[entry.dataId]?.maxByOrNull { it.level ?: 0 },
                    ownershipUnknown = true,
                    townHallLevel = townHallLevel,
                    blacksmithLevel = blacksmithLevel,
                    upgradeCatalog = upgradeCatalog
                )
            }
        }
        VillageItemDetailField(
            "CÁCH XÁC ĐỊNH SỞ HỮU",
            "JSON Export của game có thể hiển thị cả trang bị chưa mở khóa ở cấp 1 giống trang bị đã sở hữu cấp 1. Ứng dụng chỉ xác nhận sở hữu khi cấp lớn hơn 1 hoặc có nâng cấp đang chạy; trường hợp còn lại được giữ là Chưa xác nhận."
        )
        if (heroId == "barbarian-king") {
            AnnouncedBlizzardAxeCard()
        }
        VillageItemDetailField("LƯU Ý VỀ CHI PHÍ EPIC", EQUIPMENT_COST_DATA_NOTICE)
        if (equipment.isEmpty()) {
            VillageItemDetailField("Trang bị", "Danh mục hiện chưa ghi nhận trang bị cho anh hùng này.")
        }
    }
}

@Composable
private fun HeroEquipmentCatalogRow(
    entry: CocUpgradeEntry,
    villageItem: VillageItem?,
    ownershipUnknown: Boolean,
    townHallLevel: Int?,
    blacksmithLevel: Int?,
    upgradeCatalog: Map<Int, CocUpgradeEntry>
) {
    var expanded by remember(entry.dataId) { mutableStateOf(false) }
    val item = villageItem ?: catalogVillageItem(entry)
    val owned = villageItem?.hasConfirmedEquipmentOwnership == true
    val maxLevel = CocUpgradeCatalog.currentMaxLevel(entry).coerceAtLeast(1)
    val currentLevel = (if (owned) villageItem?.level ?: 1 else 1).coerceIn(1, maxLevel)
    val townHallCap = CocUpgradeCatalog.maxLevelAtHall(entry, townHallLevel, upgradeCatalog)
    val blacksmithCap = CocUpgradeCatalog.maxEquipmentAtBlacksmithLevel(
        entry,
        blacksmithLevel,
        upgradeCatalog
    )
    val confirmedUsableCap = if (townHallCap != null && blacksmithCap != null) {
        minOf(townHallCap, blacksmithCap)
    } else {
        null
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.72f), RoundedCornerShape(10.dp))
            .border(1.dp, equipmentRarityColor(entry.rarity) ?: MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CategoryImage(item, 38)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    item.name,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    ItemRarityBadge(entry.rarity)
                    Text(
                        when {
                            owned -> "CẤP ${villageItem?.level ?: "?"} / $maxLevel"
                            ownershipUnknown -> "CHƯA XÁC NHẬN"
                            else -> "CHƯA SỞ HỮU"
                        },
                        color = when {
                            owned -> MaterialTheme.colorScheme.primary
                            ownershipUnknown -> CocGold
                            else -> MaterialTheme.colorScheme.error
                        },
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black
                    )
                }
                Text(
                    "Giới hạn TH: ${townHallCap ?: "?"} • Lò rèn: ${blacksmithCap ?: "?"} • Giới hạn áp dụng: ${confirmedUsableCap ?: "Chưa đủ dữ liệu"} / $maxLevel",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 9.sp
                )
            }
            Text(
                if (expanded) "ẨN" else "CHI PHÍ",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 9.sp,
                fontWeight = FontWeight.Black
            )
        }
        if (expanded) {
            Divider(color = MaterialTheme.colorScheme.outlineVariant)
            val fullTotals = CocUpgradeCatalog.upgradeTotals(entry, 1, maxLevel)
            Text(
                "CẤP 1 → MAX",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black
            )
            if (fullTotals.resources.isEmpty()) {
                VillageItemDetailField("Tài nguyên", "Chưa có dữ liệu chi phí.")
            } else {
                fullTotals.resources.forEach { (resource, amount) -> UpgradeResourceChip(resource, amount) }
            }
            if (fullTotals.durationSeconds > 0L) {
                VillageIconDetailField(
                    R.drawable.ic_coc_time,
                    "Thời gian cơ bản",
                    formatDuration(fullTotals.durationSeconds)
                )
            }
            val missingFullLevels = CocUpgradeCatalog.missingLevels(entry, 1, maxLevel)
            if (missingFullLevels.isNotEmpty()) {
                VillageItemDetailField(
                    "Thiếu dữ liệu chi phí cấp",
                    missingFullLevels.joinToString(", ") + ". Tổng trên chỉ cộng các cấp có dữ liệu, chưa phải tổng đầy đủ."
                )
            }
            if (owned) {
                val currentTotals = CocUpgradeCatalog.upgradeTotals(
                    entry,
                    currentLevel,
                    maxLevel,
                    skipFirstInProgress = villageItem?.timerSeconds != null && (villageItem.timerSeconds ?: 0L) > 0L
                )
                Text(
                    if (villageItem?.level != null) "CẤP ${villageItem.level} → MAX"
                    else "CẤP CHƯA RÕ → MAX • TẠM TÍNH TỪ CẤP 1",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black
                )
                if (currentTotals.resources.isEmpty()) {
                    VillageItemDetailField(
                        if (villageItem?.level != null) "Tài nguyên còn lại" else "Tài nguyên còn lại (tạm tính)",
                        if (currentLevel >= maxLevel) "Đã đạt cấp tối đa." else "Chưa có dữ liệu chi phí."
                    )
                } else {
                    currentTotals.resources.forEach { (resource, amount) -> UpgradeResourceChip(resource, amount) }
                }
                val currentDuration = currentTotals.durationSeconds +
                    (villageItem?.timerSeconds?.takeIf { it > 0L } ?: 0L)
                if (currentDuration > 0L) {
                    VillageIconDetailField(
                        R.drawable.ic_coc_time,
                        if (villageItem?.level != null) "Thời gian còn lại" else "Thời gian còn lại (tạm tính từ cấp 1)",
                        formatDuration(currentDuration)
                    )
                }
                val missingCurrentLevels = CocUpgradeCatalog.missingLevels(entry, currentLevel, maxLevel)
                if (missingCurrentLevels.isNotEmpty()) {
                    VillageItemDetailField(
                        "Thiếu dữ liệu chi phí còn lại",
                        missingCurrentLevels.joinToString(", ") + ". Không thể tính đủ chi phí tới MAX nếu chưa có dữ liệu các cấp này."
                    )
                }
            } else {
                VillageItemDetailField(
                    "Trạng thái",
                    "Export chưa xác nhận trang bị ở cấp 1 là đã sở hữu hay chưa mở khóa; chi phí còn lại đang tạm tính từ cấp 1."
                )
            }
        }
    }
}

@Composable
private fun ItemRarityBadge(rarity: String) {
    val normalized = rarity.lowercase()
    val tint = equipmentRarityColor(normalized) ?: MaterialTheme.colorScheme.onSurfaceVariant
    val label = when (normalized) {
        "common" -> "THƯỜNG"
        "rare" -> "HIẾM"
        "epic" -> "SỬ THI"
        "legendary" -> "HUYỀN THOẠI"
        else -> rarity.uppercase()
    }
    Text(
        label,
        color = tint,
        fontSize = 9.sp,
        fontWeight = FontWeight.Black,
        modifier = Modifier
            .background(tint.copy(alpha = 0.20f), RoundedCornerShape(7.dp))
            .border(1.4.dp, tint.copy(alpha = 0.92f), RoundedCornerShape(7.dp))
            .padding(horizontal = 9.dp, vertical = 4.dp)
    )
}

@Composable
private fun VillageItemDetailDialog(
    item: VillageItem,
    supplemental: SupplementalCatalogEntry?,
    blacksmithLevel: Int?,
    hallLevel: Int?,
    upgradeCatalog: Map<Int, CocUpgradeEntry>,
    allVillageItems: List<VillageItem>,
    onDismiss: () -> Unit
) {
    val upgradeEntry = upgradeCatalog[item.dataId]
    val rarity = upgradeEntry?.rarity?.takeIf { it.isNotBlank() }
        ?: supplemental?.rarity?.takeIf { it.isNotBlank() }
        ?: if (item.sectionKey == "equipment") equipmentRarity(item.dataId) else null
    val maxLevel = upgradeEntry?.let { CocUpgradeCatalog.currentMaxLevel(it) }
        ?: supplemental?.maxLevel
    val hallMax = upgradeEntry?.let { CocUpgradeCatalog.maxLevelAtHall(it, hallLevel, upgradeCatalog) }
    val blacksmithMax = upgradeEntry?.let {
        CocUpgradeCatalog.maxEquipmentAtBlacksmithLevel(it, blacksmithLevel, upgradeCatalog)
    }
    val isEquipment = upgradeEntry?.category == "hero-equipment" || item.sectionKey == "equipment"
    val ownerHeroId = if (isEquipment) {
        upgradeEntry?.hero?.takeIf { it.isNotBlank() } ?: equipmentHeroId(item.dataId)
    } else null
    val ownerHero = ownerHeroId?.let(::equipmentHeroName)
    val usableCap = if (isEquipment && blacksmithMax != null) {
        listOfNotNull(hallMax, blacksmithMax).minOrNull()
    } else {
        hallMax
    } ?: maxLevel
    val currentLevel = if (upgradeEntry?.category == "squad") (item.level ?: 1) else (item.level ?: 0)
    val hasConfirmedOwnership = if (isEquipment) {
        item.hasConfirmedEquipmentOwnership
    } else {
        item.count > 0 || (item.timerSeconds ?: 0L) > 0L
    }
    val itemName = item.name
    val description = EquipmentDescriptionsVi[item.dataId]
        ?: CatalogDescriptionsVi[item.dataId]
        ?: supplemental?.description?.takeIf { it.isNotBlank() }
    val levelDetails = supplemental?.levelDetails.orEmpty()
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
                .heightIn(max = 620.dp)
                .ledRgbBorder(cornerRadiusDp = 18f, strokeWidthDp = 2.6f, brightnessIndependent = true),
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
                    if (supplemental?.imageUrl?.isNotBlank() == true) {
                        AsyncImage(
                            model = supplemental.imageUrl,
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
                description?.takeIf { it.isNotBlank() }?.let {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.78f), RoundedCornerShape(12.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f), RoundedCornerShape(12.dp))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            "MÔ TẢ",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            it,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 21.sp
                        )
                    }
                }
                VillageItemDetailField(
                    "Nguồn dữ liệu",
                    "Dữ liệu cấp nâng cấp là snapshot tĩnh cộng đồng ngày 17/11/2026; danh mục bổ sung được cập nhật ngày 18/11/2026. Đây không phải API chính thức của Supercell. Giá và thời gian có thể thay đổi theo bản cập nhật; các cấp thiếu dữ liệu được liệt kê riêng. Hãy đối chiếu trong game trước khi dùng làm kế hoạch chính xác."
                )
                if (isEquipment) {
                    VillageItemDetailField("LƯU Ý VỀ CHI PHÍ EPIC", EQUIPMENT_COST_DATA_NOTICE)
                }
                VillageItemDetailField(
                    "Cách tính thời gian",
                    "Tổng thời gian dùng thời lượng cơ bản của từng cấp; chưa trừ thời gian từ Sách, Búa, trợ thủ, vật phẩm tăng tốc hoặc ưu đãi sự kiện."
                )
                VillageItemDetailField("Căn cứ", if (item.base == "builder") "Làng đêm" else "Làng chính")
                if (upgradeEntry?.category == "squad") {
                    val squadInfo = upgradeEntry.properties
                    val unlockHall = squadInfo.optInt("townHallRequired", 0)
                    val barracksLevel = squadInfo.optInt("squadBarracksLevelRequired", 0)
                    val composition = squadInfo.optString("composition")
                    val housing = squadInfo.optInt("housingSpace", 0)
                    VillageItemDetailField("Mở khóa", "Nhà chính cấp $unlockHall • Squad Barracks cấp $barracksLevel")
                    if (composition.isNotBlank()) VillageItemDetailField("Thành phần", composition)
                    if (housing > 0) VillageItemDetailField("Sức chứa", "$housing ô nhà")
                    val barracksCost = squadInfo.optLong("barracksUpgradeCost", 0L)
                    val barracksResource = squadInfo.optString("barracksUpgradeCostResource", "Elixir")
                    if (barracksCost > 0L) UpgradeResourceChip(barracksResource, barracksCost)
                    val barracksTime = squadInfo.optJSONObject("barracksUpgradeTime")
                    val barracksSeconds = barracksTime?.let {
                        it.optLong("days") * 86400L + it.optLong("hours") * 3600L +
                            it.optLong("minutes") * 60L + it.optLong("seconds")
                    } ?: 0L
                    if (barracksSeconds > 0L) {
                        VillageIconDetailField(R.drawable.ic_coc_time, "Thời gian nâng Squad Barracks", formatDuration(barracksSeconds))
                    }
                    val level2Ability = squadInfo.optString("abilityLevel2")
                    val level3Ability = squadInfo.optString("abilityLevel3")
                    if (level2Ability.isNotBlank()) VillageItemDetailField("Kỹ năng cấp 2", level2Ability)
                    if (level3Ability.isNotBlank()) VillageItemDetailField("Kỹ năng cấp 3", level3Ability)
                    val squadStatus = when {
                        item.properties["catalogOnly"] == "true" ->
                            "JSON làng không có bản ghi cho đội hình này; chưa xác nhận sở hữu hoặc cấp hiện tại."
                        item.count > 0 || (item.timerSeconds ?: 0L) > 0L ->
                            "JSON làng có ghi nhận đội hình này${item.level?.let { " ở cấp $it" }.orEmpty()}."
                        else ->
                            "JSON làng có bản ghi nhưng số lượng bằng 0; đang xem là chưa sở hữu trong dữ liệu đã nhập."
                    }
                    VillageItemDetailField("Trạng thái trong JSON", squadStatus)
                }
                if (ownerHeroId != null && ownerHero != null) {
                    VillageEquipmentHeroField(ownerHeroId, ownerHero)
                }
                if (upgradeEntry?.let { it.category == "hero" && it.base == "home" } == true) {
                    HeroEquipmentRoster(
                        heroId = upgradeEntry.id,
                        allVillageItems = allVillageItems,
                        townHallLevel = hallLevel,
                        blacksmithLevel = blacksmithLevel,
                        upgradeCatalog = upgradeCatalog
                    )
                }
                val levelText = when {
                    upgradeEntry?.category == "squad" && item.properties["catalogOnly"] == "true" ->
                        "JSON chưa có bản ghi • chưa xác nhận sở hữu/cấp • MAX ${maxLevel ?: upgradeEntry.maxLevel}"
                    upgradeEntry?.category == "squad" && item.count <= 0 && (item.timerSeconds ?: 0L) <= 0L ->
                        "JSON ghi số lượng 0 • chưa sở hữu • MAX ${maxLevel ?: upgradeEntry.maxLevel}"
                    upgradeEntry?.category == "squad" && item.level != null ->
                        usableCap?.let { "${item.level} / $it" } ?: "${item.level} / ${maxLevel ?: upgradeEntry.maxLevel}"
                    upgradeEntry?.category == "squad" ->
                        "Đã có trong JSON • cấp chưa rõ • MAX ${maxLevel ?: upgradeEntry.maxLevel}"
                    item.properties["catalogOnly"] == "true" ->
                        "Không có bản ghi trong JSON • chưa xác nhận sở hữu/cấp • MAX ${maxLevel ?: upgradeEntry?.maxLevel ?: "?"}"
                    !hasConfirmedOwnership && isEquipment ->
                        "JSON ghi cấp ${item.level ?: "?"} • chưa xác nhận sở hữu • MAX ${maxLevel ?: upgradeEntry?.maxLevel ?: "?"}"
                    !hasConfirmedOwnership -> "Chưa sở hữu • dữ liệu nâng cấp tính từ cấp đầu tiên"
                    else -> item.level?.let { level ->
                        usableCap?.let { "$level / $it" } ?: level.toString()
                    } ?: "Chưa rõ cấp"
                }
                VillageItemDetailField("Cấp hiện tại / giới hạn đang mở", levelText)
                hallMax?.let {
                    VillageItemDetailField(if (item.base == "builder") "Giới hạn theo Nhà thợ xây" else "Giới hạn theo Nhà chính", it.toString())
                }
                if (isEquipment && blacksmithMax != null) {
                    VillageItemDetailField("Giới hạn theo Lò rèn hiện tại", blacksmithMax.toString())
                }
                maxLevel?.let { VillageItemDetailField("Cấp tối đa toàn trò chơi", it.toString()) }
                VillageItemDetailField(if (item.base == "builder") "Cấp Nhà thợ xây hiện tại" else "Cấp Nhà chính hiện tại", hallLevel?.toString() ?: "Chưa có dữ liệu")
                if (item.count > 0) VillageItemDetailField("Số lượng", item.count.toString())
                if (item.properties["catalogOnly"] == "true") {
                    VillageItemDetailField(
                        "Trạng thái dữ liệu",
                        "Danh mục có mục này nhưng JSON làng đã nhập không có bản ghi; chưa thể xác nhận đã sở hữu hay chưa."
                    )
                } else if (!hasConfirmedOwnership) {
                    VillageItemDetailField(
                        "Trạng thái sở hữu",
                        if (isEquipment) {
                            "Export không thể phân biệt trang bị đã sở hữu ở cấp 1 với trang bị chưa mở khóa. Cấp 1 chỉ là dữ liệu chưa xác nhận."
                        } else {
                            "JSON làng không ghi nhận số lượng sở hữu hoặc nâng cấp đang chạy cho mục này."
                        }
                    )
                }
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
                                VillageIconDetailField(R.drawable.ic_coc_time, "Thời gian nâng cấp tiếp theo", formatDuration(nextStep.durationSeconds()))
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
                        if (totalSeconds > 0L) VillageIconDetailField(R.drawable.ic_coc_time, "Thời gian ước tính còn lại", formatDuration(totalSeconds))
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
                            VillageIconDetailField(R.drawable.ic_coc_time, "Thời gian bổ sung", formatDuration(totals.durationSeconds))
                        }
                    }
                    if (entry.superchargeLevels.isNotEmpty()) {
                        Text(
                            "SIÊU CƯỜNG HÓA • TIẾN TRÌNH RIÊNG",
                            color = CocGold,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black
                        )
                        val superchargeCap = CocUpgradeCatalog.superchargeMaxAtHall(entry, hallLevel)
                        if ((item.level ?: 0) < entry.maxLevel) {
                            VillageItemDetailField(
                                "Điều kiện",
                                "Cần đạt cấp thường tối đa ${entry.maxLevel} trước khi tính các bậc Siêu cường hóa."
                            )
                        } else if (superchargeCap == null) {
                            VillageItemDetailField(
                                "Giới hạn Siêu cường hóa",
                                "Chưa đủ dữ liệu cấp Nhà chính/Nhà thợ xây để xác định bậc được mở."
                            )
                        } else {
                            VillageItemDetailField("Bậc Siêu cường hóa tối đa được mở", superchargeCap.toString())
                            val totals = CocUpgradeCatalog.superchargeTotals(entry, 0, superchargeCap)
                            entry.superchargeLevels.filter { it.level <= superchargeCap }.forEach { tier ->
                                VillageItemDetailField("BẬC SIÊU CƯỜNG HÓA ${tier.level}", "")
                                if (tier.cost() > 0L) UpgradeResourceChip(tier.resource(), tier.cost())
                                tier.oreCosts().forEach { (resource, amount) -> UpgradeResourceChip(resource, amount) }
                                if (tier.durationSeconds() > 0L) {
                                    VillageIconDetailField(R.drawable.ic_coc_time, "Thời gian bậc ${tier.level}", formatDuration(tier.durationSeconds()))
                                }
                            }
                            Text("TỔNG CHI PHÍ SIÊU CƯỜNG HÓA ĐANG MỞ", color = CocGold, fontSize = 9.sp, fontWeight = FontWeight.Black)
                            totals.resources.forEach { (resource, amount) -> UpgradeResourceChip(resource, amount) }
                            if (totals.durationSeconds > 0L) {
                                VillageIconDetailField(R.drawable.ic_coc_time, "Tổng thời gian siêu cường hóa", formatDuration(totals.durationSeconds))
                            }
                            VillageItemDetailField(
                                "Lưu ý",
                                "JSON làng hiện không xác nhận bậc Siêu cường hóa đã hoàn thành. Các bậc trên là dữ liệu có thể nâng cấp, không phải trạng thái thực tế."
                            )
                        }
                    }
                    if (hallMax == null && targetAtMax > currentLevel) {
                        VillageItemDetailField(
                            "Giới hạn theo Nhà chính",
                            "Chưa đủ dữ liệu yêu cầu cấp Nhà chính/công trình để xác định chính xác. Không tự ước đoán cấp này."
                        )
                    }
                }

                upgradeEntry?.let { entry ->
                    val globalMax = (maxLevel ?: entry.maxLevel).coerceAtLeast(1)
                    if (entry.category == "squad") {
                        entry.properties.optString("composition").takeIf { it.isNotBlank() }?.let {
                            VillageItemDetailField("THÀNH PHẦN ĐỘI HÌNH", it)
                        }
                        entry.properties.optInt("housingSpace", 0).takeIf { it > 0 }?.let {
                            VillageItemDetailField("SỨC CHỨA", "$it ô nhà")
                        }
                        entry.properties.optInt("squadBarracksLevelRequired", 0).takeIf { it > 0 }?.let {
                            VillageItemDetailField("DOANH TRẠI SQUAD", "Cần cấp $it")
                        }
                        entry.properties.optString("abilityName").takeIf { it.isNotBlank() }?.let {
                            VillageItemDetailField("KỸ NĂNG ĐẶC BIỆT", it)
                        }
                        listOf(2, 3).forEach { level ->
                            entry.properties.optString("abilityLevel$level").takeIf { it.isNotBlank() }?.let {
                                VillageItemDetailField("KỸ NĂNG CẤP $level", it)
                            }
                        }
                        entry.properties.optLong("barracksUpgradeCost", 0L).takeIf { it > 0L }?.let { cost ->
                            val resource = entry.properties.optString("barracksUpgradeCostResource", "Elixir")
                            VillageItemDetailField("CHI PHÍ NÂNG DOANH TRẠI", "$cost $resource")
                        }
                        entry.properties.optJSONObject("barracksUpgradeTime")?.let { time ->
                            val seconds = time.optLong("days") * 86400L +
                                time.optLong("hours") * 3600L +
                                time.optLong("minutes") * 60L +
                                time.optLong("seconds")
                            if (seconds > 0L) {
                                VillageItemDetailField("THỜI GIAN NÂNG DOANH TRẠI", formatDuration(seconds))
                            }
                        }
                    }
                    if (globalMax > 1 && entry.levels.isNotEmpty()) {
                        val firstAvailableLevel = entry.firstAvailableLevel
                        val fromLevelOne = CocUpgradeCatalog.upgradeTotals(entry, firstAvailableLevel, globalMax)
                        Text(
                            "SO SÁNH CHI PHÍ NÂNG CẤP",
                            color = CocGold,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            "TỪ CẤP $firstAvailableLevel ĐẾN MAX ($globalMax)",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black
                        )
                        if (fromLevelOne.resources.isEmpty()) {
                            VillageItemDetailField("Tài nguyên", "Không có dữ liệu chi phí cho các cấp này.")
                        } else {
                            fromLevelOne.resources.forEach { (resource, amount) ->
                                UpgradeResourceChip(resource, amount)
                            }
                        }
                        if (fromLevelOne.durationSeconds > 0L) {
                            VillageIconDetailField(
                                R.drawable.ic_coc_time,
                                "Tổng thời gian cơ bản",
                                formatDuration(fromLevelOne.durationSeconds)
                            )
                        }
                        val missingFullLevels = CocUpgradeCatalog.missingLevels(entry, firstAvailableLevel, globalMax)
                        if (missingFullLevels.isNotEmpty()) {
                            VillageItemDetailField(
                                "Thiếu dữ liệu chi phí cấp",
                                missingFullLevels.joinToString(", ") + ". Tổng ở trên chỉ cộng những cấp có dữ liệu, chưa phải tổng đầy đủ."
                            )
                        }
                        val currentStart = (if (hasConfirmedOwnership) item.level ?: firstAvailableLevel else firstAvailableLevel)
                            .coerceIn(firstAvailableLevel, globalMax)
                        val fromCurrent = CocUpgradeCatalog.upgradeTotals(
                            entry,
                            currentStart,
                            globalMax,
                            skipFirstInProgress = item.timerSeconds != null && item.timerSeconds > 0L
                        )
                        Text(
                            if (hasConfirmedOwnership && item.level != null) {
                                "TỪ CẤP $currentStart ĐẾN MAX ($globalMax)"
                            } else {
                                "TẠM TÍNH TỪ CẤP $currentStart ĐẾN MAX ($globalMax)"
                            },
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black
                        )
                        if (!hasConfirmedOwnership || item.level == null) {
                            val assumption = when {
                                item.properties["catalogOnly"] == "true" ->
                                    "JSON làng không có bản ghi cho mục này; chi phí tạm tính từ cấp mở khóa đầu tiên ($firstAvailableLevel)."
                                isEquipment && !hasConfirmedOwnership ->
                                    "Export có thể ghi trang bị chưa mở khóa ở cấp 1 giống trang bị đã sở hữu cấp 1; chi phí còn lại tạm tính từ cấp 1."
                                !hasConfirmedOwnership ->
                                    "JSON làng không ghi nhận sở hữu; chi phí tạm tính từ cấp mở khóa đầu tiên ($firstAvailableLevel)."
                                else ->
                                    "JSON làng có ghi nhận mục này nhưng chưa có cấp hiện tại; chi phí tạm tính từ cấp mở khóa đầu tiên ($firstAvailableLevel)."
                            }
                            VillageItemDetailField("Giả định", assumption)
                        }
                        if (fromCurrent.resources.isEmpty()) {
                            VillageItemDetailField(
                                "Tài nguyên còn lại",
                                if (currentStart >= globalMax) "Đã đạt cấp tối đa." else "Không có dữ liệu chi phí cho các cấp còn lại."
                            )
                        } else {
                            fromCurrent.resources.forEach { (resource, amount) ->
                                UpgradeResourceChip(resource, amount)
                            }
                        }
                        val currentDuration = fromCurrent.durationSeconds +
                            (item.timerSeconds?.takeIf { it > 0L } ?: 0L)
                        if (currentDuration > 0L) {
                            VillageIconDetailField(
                                R.drawable.ic_coc_time,
                                "Thời gian ước tính còn lại",
                                formatDuration(currentDuration)
                            )
                        }
                        val missingRemainingLevels = CocUpgradeCatalog.missingLevels(entry, currentStart, globalMax)
                        if (missingRemainingLevels.isNotEmpty()) {
                            VillageItemDetailField(
                                "Thiếu dữ liệu chi phí còn lại",
                                missingRemainingLevels.joinToString(", ") + ". Cần cập nhật dữ liệu trò chơi để tính chính xác."
                            )
                        }
                    }
                }

                supplemental?.aliases?.takeIf { it.isNotEmpty() }?.let {
                    VillageItemDetailField("Tên gọi khác", it.joinToString(", "))
                }
                supplemental?.requirements?.takeIf { it.isNotEmpty() }?.let { requirements ->
                    VillageItemDetailField(
                        "Yêu cầu mở khóa",
                        requirements.joinToString("\n") { localizedCatalogText(it) }
                    )
                }
                supplemental?.abilityType?.takeIf { it.isNotBlank() }?.let {
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
                            val rawLabel = if (separator > 0) rawValue.substring(0, separator).trim()
                                else "Chỉ số ${index + 1}"
                            val value = if (separator > 0) rawValue.substring(separator + 1).trim() else rawValue
                            val (label, displayValue) = localizedCatalogField(rawLabel, value)
                            VillageItemStatField(label, displayValue)
                        }
                    } else {
                        catalogStats.forEach { (rawLabel, rawValue) ->
                            val normalizedLabel = rawLabel.removePrefix("stats_").removePrefix("normal_")
                            val (label, value) = localizedCatalogField(normalizedLabel, rawValue)
                            VillageItemStatField(label, value)
                        }
                    }
                }
                supplemental?.upgradeCost?.takeIf { it.isNotBlank() }?.let {
                    VillageItemDetailField("Chi phí nâng cấp", it)
                }
                supplemental?.upgradeDurationSeconds?.let {
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
                if (supplemental == null && description == null && item.details.isEmpty() && item.properties.isEmpty() &&
                    item.timerSeconds == null && item.helperCooldownSeconds == null
                ) {
                    Text(
                        "JSON hiện không cung cấp thêm chỉ số cho mục này.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
                if (supplemental?.details?.isNotEmpty() == true) {
                    Text("THÔNG TIN BỔ SUNG", color = MaterialTheme.colorScheme.primary, fontSize = 10.sp, fontWeight = FontWeight.Black)
                    supplemental.details.forEachIndexed { index, value ->
                        val separator = value.indexOf(':')
                        val rawLabel = if (separator > 0) value.substring(0, separator).trim() else "Chi tiết ${index + 1}"
                        val rawValue = if (separator > 0) value.substring(separator + 1).trim() else value
                        val (label, displayValue) = localizedCatalogField(rawLabel, rawValue)
                        VillageItemDetailField(label, displayValue)
                    }
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
            Text(label, color = oreColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(value, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, lineHeight = 20.sp)
        }
    }
}

@Composable
private fun UpgradeResourceChip(resource: String, amount: Long) {
    val normalized = resource.trim().lowercase()
    val (label, tint) = when (normalized) {
        "gold" -> "Vàng" to Color(0xFFFFD166)
        "elixir" -> "Tiên dược" to Color(0xFFFF6EC7)
        "dark elixir" -> "Dầu đen" to Color(0xFFB77AFF)
        "builder gold" -> "Vàng thợ xây" to Color(0xFFFFD166)
        "builder elixir" -> "Tiên dược thợ xây" to Color(0xFFFF6EC7)
        "gold or elixir" -> "Vàng hoặc Tiên dược" to Color(0xFFFFD166)
        "builder gold or builder elixir" -> "Vàng thợ xây hoặc Tiên dược thợ xây" to Color(0xFFFFD166)
        "capital gold" -> "Vàng thủ đô" to Color(0xFFFFD166)
        "quặng sáng", "shiny ore" -> "Quặng sáng" to Color(0xFF55B9FF)
        "quặng phát sáng", "glowy ore" -> "Quặng phát sáng" to Color(0xFFB77AFF)
        "quặng sao", "starry ore" -> "Quặng sao" to Color(0xFFFFC857)
        else -> resource to MaterialTheme.colorScheme.primary
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
        if (normalized == "gold or elixir" || normalized == "builder gold or builder elixir") {
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                CocResourceIcon("gold", Modifier.size(18.dp))
                CocResourceIcon("elixir", Modifier.size(18.dp))
            }
        } else {
            CocResourceIcon(resource, Modifier.size(18.dp))
        }
        Text(label, color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp, modifier = Modifier.weight(1f))
        Text(java.text.NumberFormat.getIntegerInstance().format(amount), color = tint, fontSize = 12.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun VillageIconDetailField(iconRes: Int, label: String, value: String) {
    val icon = when (iconRes) {
        R.drawable.ic_coc_time -> Icons.Outlined.AccessTime
        R.drawable.ic_coc_hero -> Icons.Outlined.Person
        else -> Icons.Outlined.Info
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(17.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
            Text(value, color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, lineHeight = 17.sp)
        }
    }
}

@Composable
private fun VillageItemDetailField(label: String, value: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        Text(value, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, lineHeight = 20.sp)
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
            if (item.gearUp) CocGameIcon(R.drawable.ic_coc_gear, Modifier.size(18.dp), "Đã ghép")
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
    onDeletePermanently: (String) -> Unit
) {
    val context = LocalContext.current
    var selectedSection by remember { mutableStateOf("villages") }
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
                                Text("ĐÃ XÓA", fontSize = 9.sp, maxLines = 1)
                                Text("(${deletedVillages.size})", fontSize = 9.sp, maxLines = 1)
                            }
                        }
                    )
                }
            }
        }

        if (selectedSection == "villages") {
            if (villages.isEmpty()) {
                item { InfoCard("Chưa có làng nào", "Nhấn MỞ CLASH OF CLANS để mở game cùng cửa sổ nhập JSON nổi. Sao chép JSON trong game rồi dán vào cửa sổ đó để lưu làng.") }
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
                                CocGameIcon(
                                    R.drawable.ic_coc_star,
                                    Modifier.size(20.dp)
                                        .clickable { onTogglePin(snapshot.tag) }
                                        .padding(horizontal = 5.dp, vertical = 1.dp),
                                    if (pinned) "Bỏ ghim làng" else "Ghim làng"
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
                                CocGameIcon(R.drawable.ic_coc_tag, Modifier.size(11.dp), AppText.translate(context, "Mã người chơi"))
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
            item { Text("Tối đa 50 bản JSON trên mỗi tài khoản. Khi đủ 50, hãy xóa vĩnh viễn bản cũ trước khi lưu bản mới; mục Đã xóa vẫn chiếm chỗ.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp) }
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
                item { InfoCard("Chưa có JSON nào bị xóa", "Những làng bạn xóa khỏi mục ĐANG LƯU sẽ xuất hiện tại đây để xem, lưu hoặc khôi phục.") }
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
                                "Mã người chơi: ${record.tag}  •  Đã xóa: ${formatTimestamp(record.deletedAt)}",
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
