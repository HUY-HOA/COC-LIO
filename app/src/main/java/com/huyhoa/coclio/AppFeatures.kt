package com.huyhoa.coclio

import android.Manifest

import android.app.Activity
import android.graphics.drawable.Icon
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.huyhoa.coclio.data.CocAssets
import java.security.MessageDigest
import java.net.URL
import java.net.HttpURLConnection
import java.io.FileOutputStream
import java.io.File
import android.view.View
import android.util.TypedValue
import android.graphics.BitmapFactory
import android.graphics.Bitmap
import android.graphics.Typeface
import android.graphics.Paint
import android.graphics.Canvas
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.content.Intent
import android.net.Uri
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.os.Build
import android.os.SystemClock
import android.app.NotificationChannel
import android.app.NotificationManager
import android.media.AudioAttributes
import android.provider.Settings
import android.app.AlertDialog
import android.widget.RemoteViews
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Slider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.shape.CircleShape
import coil.compose.AsyncImage
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Icon as ComposeIcon
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Switch
import androidx.compose.material3.Text as MaterialText
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.work.CoroutineWorker
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.gms.tasks.Task
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.perf.FirebasePerformance
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.huyhoa.coclio.data.VillageSnapshot
import com.huyhoa.coclio.data.VillageStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

private val FeatureCyan = Color(0xFF36F4FF)
private val FeatureMuted = Color(0xFF9EAFCE)
private const val ZALO_CONTACT_URL = "https://zaloapp.com/qr/p/1gl8y41uqztdt?src=qr"

data class GoogleAccountData(
    val email: String,
    val displayName: String?,
    val givenName: String?,
    val familyName: String?,
    val profilePictureUri: String?
)

class GoogleAccountSelection(
    val account: GoogleAccountData,
    val idToken: String
)

data class LocalProfileSummary(
    val email: String,
    val displayName: String?,
    val preferredName: String?,
    val lastUsedAt: Long,
    val isCurrent: Boolean
)

object AccountProfileStore {
    private const val PREFS = "coc_lio_profiles"
    private const val ACTIVE = "active_profile"
    private const val KNOWN = "known_profiles"

    private fun key(email: String, field: String) =
        field + "_" + email.trim().lowercase().hashCode().toUInt().toString(16)

    fun active(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(ACTIVE, "local") ?: "local"

    private fun addKnown(context: Context, email: String) {
        if (email == "local") return
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val emails = prefs.getStringSet(KNOWN, emptySet())?.toMutableSet() ?: mutableSetOf()
        emails.add(email.trim().lowercase())
        prefs.edit().putStringSet(KNOWN, emails).apply()
    }

    fun setActive(context: Context, email: String?) {
        val normalized = email?.trim()?.lowercase()?.takeIf { it.isNotBlank() } ?: "local"
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(ACTIVE, normalized).apply()
        addKnown(context, normalized)
    }

    fun saveGoogleData(context: Context, data: GoogleAccountData) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(key(data.email, "google_email"), data.email)
            .putString(key(data.email, "google_display_name"), data.displayName)
            .putString(key(data.email, "google_given_name"), data.givenName)
            .putString(key(data.email, "google_family_name"), data.familyName)
            .putString(key(data.email, "google_picture_uri"), data.profilePictureUri)
            .apply()
        addKnown(context, data.email)
    }

    fun googleData(context: Context, email: String): GoogleAccountData? {
        if (email == "local") return null
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return GoogleAccountData(
            email = prefs.getString(key(email, "google_email"), null) ?: email,
            displayName = prefs.getString(key(email, "google_display_name"), null),
            givenName = prefs.getString(key(email, "google_given_name"), null),
            familyName = prefs.getString(key(email, "google_family_name"), null),
            profilePictureUri = prefs.getString(key(email, "google_picture_uri"), null)
        )
    }

    fun preferredName(context: Context, email: String): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(key(email, "preferred_name"), null)

    fun setPreferredName(context: Context, email: String, name: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(key(email, "preferred_name"), name.trim()).apply()
    }

    fun markLastUsed(context: Context, email: String, timestamp: Long = System.currentTimeMillis()) {
        if (email == "local") return
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putLong(key(email, "last_used"), timestamp).apply()
        addKnown(context, email)
    }

    fun lastUsedAt(context: Context, email: String): Long? {
        if (email == "local") return null
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val field = key(email, "last_used")
        return if (prefs.contains(field)) prefs.getLong(field, 0L).takeIf { it > 0L } else null
    }

    fun knownProfiles(context: Context, currentEmail: String): List<LocalProfileSummary> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val emails = prefs.getStringSet(KNOWN, emptySet()).orEmpty()
        return emails.map { email ->
            val data = googleData(context, email)
            LocalProfileSummary(
                email = email,
                displayName = data?.displayName,
                preferredName = preferredName(context, email),
                lastUsedAt = lastUsedAt(context, email) ?: 0L,
                isCurrent = email.equals(currentEmail, ignoreCase = true)
            )
        }.sortedWith(compareByDescending<LocalProfileSummary> { it.isCurrent }.thenByDescending { it.lastUsedAt })
    }
}

object AppPreferences {
    private const val PREFS = "coc_lio_settings"

    private fun key(profile: String, name: String): String =
        name + "_" + profile.hashCode().toUInt().toString(16)

    fun notificationsEnabled(context: Context, profile: String): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(key(profile, "notifications"), false)

    fun setNotificationsEnabled(context: Context, profile: String, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(key(profile, "notifications"), enabled).apply()
    }

    fun bubbleNotificationsEnabled(context: Context, profile: String): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(key(profile, "bubble_notifications"), false)

    fun setBubbleNotificationsEnabled(context: Context, profile: String, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(key(profile, "bubble_notifications"), enabled).apply()
    }

    fun ensureNotificationPreferences(context: Context, profile: String, defaultEnabled: Boolean) {
        val preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val editor = preferences.edit()
        val notificationKey = key(profile, "notifications")
        if (!preferences.contains(notificationKey)) editor.putBoolean(notificationKey, defaultEnabled)
        editor.apply()
    }

    fun notificationStyle(context: Context, profile: String): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(key(profile, "notification_style"), "alarm") ?: "alarm"

    fun setNotificationStyle(context: Context, profile: String, style: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(key(profile, "notification_style"), style).apply()
    }

    fun textScale(context: Context): Float {
        val preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (preferences.getInt("text_scale_version", 0) < 1) {
            preferences.edit()
                .putFloat("text_scale", 1f)
                .putInt("text_scale_version", 1)
                .apply()
        }
        val savedScale = preferences.getFloat("text_scale", 1f).coerceIn(0.8f, 1.25f)
        val normalizedScale = textScaleForLevel(textScaleLevel(savedScale))
        if (savedScale != normalizedScale) {
            preferences.edit().putFloat("text_scale", normalizedScale).apply()
        }
        return normalizedScale
    }

    fun textScaleLevel(scale: Float): Int =
        (((scale.coerceIn(0.8f, 1.25f) - 0.8f) / 0.05f).roundToInt() + 1).coerceIn(1, 10)

    fun textScaleForLevel(level: Int): Float =
        0.8f + (level.coerceIn(1, 10) - 1) * 0.05f

    fun themeMode(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString("theme_mode", "system") ?: "system"

    fun setThemeMode(context: Context, mode: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString("theme_mode", mode).apply()
    }

    fun displaySizeLevel(context: Context): Int {
        val preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (preferences.getInt("display_size_level_version", 0) < 1) {
            val initialLevel = if (preferences.contains("display_size_level")) {
                (preferences.getInt("display_size_level", 3).coerceIn(1, 5) - 1) * 2 + 1
            } else {
                5
            }
            preferences.edit()
                .putInt("display_size_level", initialLevel)
                .putInt("display_size_level_version", 1)
                .apply()
        }
        return preferences.getInt("display_size_level", 5).coerceIn(1, 10)
    }

    fun setDisplaySizeLevel(context: Context, level: Int) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putInt("display_size_level", level.coerceIn(1, 10))
            .putInt("display_size_level_version", 1)
            .apply()
    }

    fun displaySizeScale(context: Context): Float = displaySizeScale(displaySizeLevel(context))

    fun displaySizeScale(level: Int): Float =
        0.8f + (level.coerceIn(1, 10) - 1) * 0.05f

    fun setTextScale(context: Context, scale: Float) {
        val normalizedScale = textScaleForLevel(textScaleLevel(scale))
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putFloat("text_scale", normalizedScale).apply()
    }
}

private suspend fun <T> Task<T>.awaitFirebaseResult(): T =
    suspendCancellableCoroutine { continuation ->
        addOnCompleteListener { task ->
            if (!continuation.isActive) return@addOnCompleteListener
            if (task.isSuccessful) {
                continuation.resume(task.result)
            } else {
                continuation.resumeWithException(
                    task.exception ?: IllegalStateException("Không thể hoàn tất thao tác tài khoản. Vui lòng thử lại.")
                )
            }
        }
    }

private suspend fun syncFirebaseUserProfile(
    user: FirebaseUser,
    google: GoogleAccountData,
    countAsSignIn: Boolean = true
) {
    val email = user.email?.trim()?.lowercase()?.takeIf { it.isNotBlank() }
        ?: throw IllegalStateException("Không nhận được email từ tài khoản Google.")
    val reference = FirebaseFirestore.getInstance()
        .collection("users")
        .document(user.uid)
    val existing = reference.get().awaitFirebaseResult()
    if (existing.getBoolean("accountDisabled") == true) {
        throw IllegalStateException("Tài khoản COC-LIO này đã bị quản lý vô hiệu hóa. Vui lòng liên hệ quản lý để được hỗ trợ.")
    }
    val signInCount = (existing.getLong("signInCount") ?: 0L) + 1L
    val profile = hashMapOf<String, Any>(
        "uid" to user.uid,
        "email" to email,
        "displayName" to (google.displayName ?: user.displayName.orEmpty()),
        "givenName" to google.givenName.orEmpty(),
        "familyName" to google.familyName.orEmpty(),
        "photoUrl" to (google.profilePictureUri ?: user.photoUrl?.toString().orEmpty()),
        "emailVerified" to user.isEmailVerified,
        "providerId" to "google.com",
        "updatedAt" to FieldValue.serverTimestamp()
    )
    if (!existing.exists()) {
        profile["createdAt"] = FieldValue.serverTimestamp()
        profile["villageDataConsent"] = false
        profile["signInCount"] = 1L
        profile["lastSignInAt"] = FieldValue.serverTimestamp()
    } else if (countAsSignIn) {
        profile["signInCount"] = signInCount
        profile["lastSignInAt"] = FieldValue.serverTimestamp()
    }
    reference.set(profile, SetOptions.merge()).awaitFirebaseResult()
}

data class AdminGoogleAccount(
    val uid: String,
    val email: String,
    val preferredName: String,
    val displayName: String,
    val givenName: String,
    val familyName: String,
    val photoUrl: String,
    val emailVerified: Boolean,
    val providerId: String,
    val signInCount: Long,
    val createdAt: Long,
    val lastSignInAt: Long,
    val updatedAt: Long,
    val isAdmin: Boolean = false,
    val isOnline: Boolean = false,
    val lastActiveAt: Long = 0L,
    val firstOpenedAt: Long = 0L,
    val lastOpenedAt: Long = 0L,
    val lastBackgroundAt: Long = 0L,
    val sessionStartedAt: Long = 0L,
    val activityUpdatedAt: Long = 0L,
    val lastActivityEvent: String = "",
    val appOpenCount: Long = 0L,
    val lastSessionDurationSeconds: Long = 0L,
    val totalForegroundSeconds: Long = 0L,
    val deviceManufacturer: String = "",
    val deviceModel: String = "",
    val deviceBrand: String = "",
    val deviceProduct: String = "",
    val deviceCodeName: String = "",
    val supportedAbis: String = "",
    val localeTag: String = "",
    val timeZoneId: String = "",
    val screenWidthPx: Long = 0L,
    val screenHeightPx: Long = 0L,
    val densityDpi: Long = 0L,
    val networkType: String = "",
    val androidVersion: String = "",
    val sdkInt: Long = 0L,
    val appVersionName: String = "",
    val appVersionCode: Long = 0L,
    val activityDataConsentVersion: Long = 0L,
    val dataSharingConsent: Boolean = false,
    val analyticsConsent: Boolean = false,
    val diagnosticsConsent: Boolean = false,
    val villageDataConsent: Boolean = false,
    val lastVillageTag: String = "",
    val lastVillageName: String = "",
    val lastVillageTimestamp: Long = 0L,
    val lastVillageTownHallLevel: Long = 0L,
    val lastVillageBuilderHallLevel: Long = 0L,
    val villageImportCount: Long = 0L,
    val lastVillageSyncedAt: Long = 0L,
    val lastVillageRawJsonBytes: Long = 0L,
    val activityConsentAt: Long = 0L,
    val accountDisabled: Boolean = false
)

object FirebaseAccountAdministration {
    private const val OWNER_EMAIL = "phunghuyhoahaikba@gmail.com"

    fun isOwner(email: String?): Boolean =
        email?.trim()?.equals(OWNER_EMAIL, ignoreCase = true) == true

    suspend fun savePreferredName(name: String) {
        val user = FirebaseAuth.getInstance().currentUser
            ?: throw IllegalStateException("Bạn cần đăng nhập Google để đồng bộ tên hiển thị.")
        require(user.isEmailVerified) { "Email Google chưa được xác minh." }
        require(name.trim().isNotEmpty() && name.trim().length <= 40) {
            "Tên hiển thị phải có từ 1 đến 40 ký tự."
        }
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(user.uid)
            .update(
                mapOf(
                    "preferredName" to name.trim(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            .awaitFirebaseResult()
    }

    suspend fun loadAccounts(context: Context): List<AdminGoogleAccount> {
        val result = FirebaseFirestore.getInstance()
            .collection("users")
            .get()
            .awaitFirebaseResult()
        val accounts = result.documents.map { document ->
            val email = document.getString("email").orEmpty()
            val hasActivityConsent =
                document.getLong("activityDataConsentVersion") == AccountPrivacyConsent.CURRENT_VERSION &&
                    document.getBoolean("dataSharingConsent") == true
            val lastActiveAt = if (hasActivityConsent) {
                document.getTimestamp("lastActiveAt")?.toDate()?.time ?: 0L
            } else 0L
            val lastBackgroundAt = if (hasActivityConsent) {
                document.getTimestamp("lastBackgroundAt")?.toDate()?.time ?: 0L
            } else 0L
            AdminGoogleAccount(
                uid = document.getString("uid") ?: document.id,
                email = email,
                preferredName = document.getString("preferredName").orEmpty(),
                displayName = document.getString("displayName").orEmpty(),
                givenName = document.getString("givenName").orEmpty(),
                familyName = document.getString("familyName").orEmpty(),
                photoUrl = document.getString("photoUrl").orEmpty(),
                emailVerified = document.getBoolean("emailVerified") ?: false,
                providerId = document.getString("providerId").orEmpty(),
                signInCount = document.getLong("signInCount") ?: 0L,
                createdAt = document.getTimestamp("createdAt")?.toDate()?.time ?: 0L,
                lastSignInAt = document.getTimestamp("lastSignInAt")?.toDate()?.time ?: 0L,
                updatedAt = document.getTimestamp("updatedAt")?.toDate()?.time ?: 0L,
                isAdmin = isOwner(email),
                isOnline = hasActivityConsent && lastActiveAt > lastBackgroundAt &&
                    lastActiveAt > 0L &&
                    System.currentTimeMillis() - lastActiveAt <= 660_000L,
                lastActiveAt = lastActiveAt,
                firstOpenedAt = if (hasActivityConsent) document.getTimestamp("firstOpenedAt")?.toDate()?.time ?: 0L else 0L,
                lastOpenedAt = if (hasActivityConsent) document.getTimestamp("lastOpenedAt")?.toDate()?.time ?: 0L else 0L,
                lastBackgroundAt = lastBackgroundAt,
                sessionStartedAt = if (hasActivityConsent) document.getTimestamp("sessionStartedAt")?.toDate()?.time ?: 0L else 0L,
                activityUpdatedAt = if (hasActivityConsent) document.getTimestamp("activityUpdatedAt")?.toDate()?.time ?: 0L else 0L,
                lastActivityEvent = if (hasActivityConsent) document.getString("lastActivityEvent").orEmpty() else "",
                appOpenCount = if (hasActivityConsent) document.getLong("appOpenCount") ?: 0L else 0L,
                lastSessionDurationSeconds = if (hasActivityConsent) document.getLong("lastSessionDurationSeconds") ?: 0L else 0L,
                totalForegroundSeconds = if (hasActivityConsent) document.getLong("totalForegroundSeconds") ?: 0L else 0L,
                deviceManufacturer = if (hasActivityConsent) document.getString("deviceManufacturer").orEmpty() else "",
                deviceModel = if (hasActivityConsent) document.getString("deviceModel").orEmpty() else "",
                deviceBrand = if (hasActivityConsent) document.getString("deviceBrand").orEmpty() else "",
                deviceProduct = if (hasActivityConsent) document.getString("deviceProduct").orEmpty() else "",
                deviceCodeName = if (hasActivityConsent) document.getString("deviceCodeName").orEmpty() else "",
                supportedAbis = if (hasActivityConsent) document.getString("supportedAbis").orEmpty() else "",
                localeTag = if (hasActivityConsent) document.getString("localeTag").orEmpty() else "",
                timeZoneId = if (hasActivityConsent) document.getString("timeZoneId").orEmpty() else "",
                screenWidthPx = if (hasActivityConsent) document.getLong("screenWidthPx") ?: 0L else 0L,
                screenHeightPx = if (hasActivityConsent) document.getLong("screenHeightPx") ?: 0L else 0L,
                densityDpi = if (hasActivityConsent) document.getLong("densityDpi") ?: 0L else 0L,
                networkType = if (hasActivityConsent) document.getString("networkType").orEmpty() else "",
                androidVersion = if (hasActivityConsent) document.getString("androidVersion").orEmpty() else "",
                sdkInt = if (hasActivityConsent) document.getLong("sdkInt") ?: 0L else 0L,
                appVersionName = if (hasActivityConsent) document.getString("appVersionName").orEmpty() else "",
                appVersionCode = if (hasActivityConsent) document.getLong("appVersionCode") ?: 0L else 0L,
                activityDataConsentVersion = document.getLong("activityDataConsentVersion") ?: 0L,
                dataSharingConsent = document.getBoolean("dataSharingConsent") ?: false,
                analyticsConsent = document.getBoolean("analyticsConsent") ?: false,
                diagnosticsConsent = document.getBoolean("diagnosticsConsent") ?: false,
                villageDataConsent = document.getBoolean("villageDataConsent") ?: false,
                lastVillageTag = document.getString("lastVillageTag").orEmpty(),
                lastVillageName = document.getString("lastVillageName").orEmpty(),
                lastVillageTimestamp = document.getLong("lastVillageTimestamp") ?: 0L,
                lastVillageTownHallLevel = document.getLong("lastVillageTownHallLevel") ?: 0L,
                lastVillageBuilderHallLevel = document.getLong("lastVillageBuilderHallLevel") ?: 0L,
                villageImportCount = document.getLong("villageImportCount") ?: 0L,
                lastVillageSyncedAt = document.getTimestamp("lastVillageSyncedAt")?.toDate()?.time ?: 0L,
                lastVillageRawJsonBytes = document.getLong("lastVillageRawJsonBytes") ?: 0L,
                activityConsentAt = document.getTimestamp("activityConsentAt")?.toDate()?.time ?: 0L,
                accountDisabled = document.getBoolean("accountDisabled") ?: false
            )
        }.toMutableList()

        val owner = FirebaseAuth.getInstance().currentUser
            ?.takeIf { isOwner(it.email) }
        if (owner != null) {
            val legacyIpDocuments = result.documents.filter { document ->
                document.contains("lastIp") || document.contains("lastIpCapturedAt")
            }
            for (chunk in legacyIpDocuments.chunked(400)) {
                val batch = FirebaseFirestore.getInstance().batch()
                for (document in chunk) {
                    val legacyFields = mutableMapOf<String, Any>()
                    if (document.contains("lastIp")) legacyFields["lastIp"] = FieldValue.delete()
                    if (document.contains("lastIpCapturedAt")) legacyFields["lastIpCapturedAt"] = FieldValue.delete()
                    if (legacyFields.isNotEmpty()) batch.update(document.reference, legacyFields)
                }
                if (chunk.isNotEmpty()) batch.commit().awaitFirebaseResult()
            }
        }
        return accounts.sortedWith(
            compareByDescending<AdminGoogleAccount> { it.isAdmin }
                .thenByDescending { it.lastSignInAt }
        )
    }

    suspend fun setAccountDisabled(uid: String, disabled: Boolean) {
        require(uid.isNotBlank()) { "Thiếu mã tài khoản cần cập nhật." }
        val owner = FirebaseAuth.getInstance().currentUser
            ?: throw IllegalStateException("Bạn cần đăng nhập tài khoản quản lý.")
        require(isOwner(owner.email)) { "Chỉ chủ sở hữu COC-LIO mới được quản lý tài khoản." }
        require(uid != owner.uid) { "Không thể tự vô hiệu hóa tài khoản quản lý đang sử dụng." }
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .update(
                mapOf(
                    "accountDisabled" to disabled,
                    "accountDisabledAt" to FieldValue.serverTimestamp(),
                    "accountDisabledBy" to owner.uid
                )
            )
            .awaitFirebaseResult()
    }

    suspend fun deleteStoredAccountProfile(uid: String) {
        require(uid.isNotBlank()) { "Thiếu mã tài khoản cần xóa." }
        val owner = FirebaseAuth.getInstance().currentUser
            ?: throw IllegalStateException("Bạn cần đăng nhập tài khoản quản lý.")
        require(isOwner(owner.email)) { "Chỉ chủ sở hữu COC-LIO mới được xóa hồ sơ tài khoản." }
        require(uid != owner.uid) { "Không thể xóa hồ sơ tài khoản quản lý đang sử dụng." }

        val firestore = FirebaseFirestore.getInstance()
        val userReference = firestore.collection("users").document(uid)
        val snapshots = userReference.collection("village_snapshots").get().awaitFirebaseResult()
        for (chunk in snapshots.documents.chunked(400)) {
            val batch = firestore.batch()
            chunk.forEach { batch.delete(it.reference) }
            batch.commit().awaitFirebaseResult()
        }
        firestore.collection("licenses").document(uid).delete().awaitFirebaseResult()
        userReference.delete().awaitFirebaseResult()
    }
}

data class AccountPrivacyChoice(
    val securityTelemetry: Boolean = false,
    val analytics: Boolean = false,
    val diagnostics: Boolean = false,
    val villageDataSync: Boolean = false,
    val version: Long = 0L
)

object AccountPrivacyConsent {
    const val CURRENT_VERSION = 8L
    const val REQUIRED_ACCOUNT_DATA_VERSION = 2L

    suspend fun hasRequiredAccountDataConsent(): Boolean {
        val user = FirebaseAuth.getInstance().currentUser ?: return false
        val profile = FirebaseFirestore.getInstance()
            .collection("users")
            .document(user.uid)
            .get()
            .awaitFirebaseResult()
        return profile.getLong("requiredAccountDataConsentVersion") == REQUIRED_ACCOUNT_DATA_VERSION
    }

    suspend fun hasAccountProfile(): Boolean {
        val user = FirebaseAuth.getInstance().currentUser ?: return false
        return FirebaseFirestore.getInstance()
            .collection("users")
            .document(user.uid)
            .get()
            .awaitFirebaseResult()
            .exists()
    }

    suspend fun saveRequiredAccountDataConsent() {
        val user = FirebaseAuth.getInstance().currentUser
            ?: throw IllegalStateException("Bạn cần đăng nhập Google để xác nhận dữ liệu tài khoản.")
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(user.uid)
            .update(
                mapOf(
                    "requiredAccountDataConsentVersion" to REQUIRED_ACCOUNT_DATA_VERSION,
                    "requiredAccountDataConsentAt" to FieldValue.serverTimestamp()
                )
            )
            .awaitFirebaseResult()
    }

    suspend fun save(choice: AccountPrivacyChoice) {
        val user = FirebaseAuth.getInstance().currentUser
            ?: throw IllegalStateException("Bạn cần đăng nhập Google để lưu lựa chọn dữ liệu.")
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(user.uid)
            .update(
                mapOf(
                    "dataSharingConsent" to choice.securityTelemetry,
                    "analyticsConsent" to choice.analytics,
                    "diagnosticsConsent" to choice.diagnostics,
                    "villageDataConsent" to choice.villageDataSync,
                    "activityDataConsentVersion" to CURRENT_VERSION,
                    "activityConsentAt" to FieldValue.serverTimestamp()
                )
            )
            .awaitFirebaseResult()
    }

    suspend fun load(): AccountPrivacyChoice {
        val user = FirebaseAuth.getInstance().currentUser ?: return AccountPrivacyChoice()
        val profile = FirebaseFirestore.getInstance()
            .collection("users")
            .document(user.uid)
            .get()
            .awaitFirebaseResult()
        val version = profile.getLong("activityDataConsentVersion") ?: 0L
        if (version != CURRENT_VERSION) return AccountPrivacyChoice(version = version)
        return AccountPrivacyChoice(
            securityTelemetry = profile.getBoolean("dataSharingConsent") == true,
            analytics = profile.getBoolean("analyticsConsent") == true,
            diagnostics = profile.getBoolean("diagnosticsConsent") == true,
            villageDataSync = profile.getBoolean("villageDataConsent") == true,
            version = version
        )
    }

    fun applyCollectionSettings(context: Context, choice: AccountPrivacyChoice) {
        FirebaseAnalytics.getInstance(context).setAnalyticsCollectionEnabled(choice.analytics)
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(choice.diagnostics)
        FirebasePerformance.getInstance().setPerformanceCollectionEnabled(choice.diagnostics)
        if (!choice.diagnostics) {
            FirebaseCrashlytics.getInstance().deleteUnsentReports()
        }
    }
}

object AccountActivityTracker {
    @Volatile var lastSuccessfulWriteAt: Long = 0L
        private set
    @Volatile var lastWriteError: String? = null
        private set
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val lifecycleLock = Any()
    private var heartbeatJob: Job? = null
    private var pendingStopJob: Job? = null
    private var lifecycleGeneration = 0L
    @Volatile private var sessionStartedAt = 0L

    fun start(context: Context) {
        synchronized(lifecycleLock) {
            if (heartbeatJob?.isActive == true) return
            val previousStop = pendingStopJob
            val generation = ++lifecycleGeneration
            val job = scope.launch(start = CoroutineStart.LAZY) {
                previousStop?.join()
                val user = FirebaseAuth.getInstance().currentUser ?: return@launch
                if (!hasConsent(user.uid)) return@launch
                val started = synchronized(lifecycleLock) {
                    if (generation != lifecycleGeneration) {
                        false
                    } else {
                        sessionStartedAt = System.currentTimeMillis()
                        true
                    }
                }
                if (!started) return@launch
                record(context.applicationContext, "open", 0L)
                while (isActive) {
                    delay(600_000L)
                    val isCurrentSession = synchronized(lifecycleLock) {
                        generation == lifecycleGeneration
                    }
                    if (!isCurrentSession) return@launch
                    if (!hasConsent(user.uid)) break
                    record(context.applicationContext, "heartbeat", 0L)
                }
                synchronized(lifecycleLock) {
                    if (generation == lifecycleGeneration) {
                        heartbeatJob = null
                        sessionStartedAt = 0L
                    }
                }
            }
            heartbeatJob = job
            job.start()
        }
    }

    fun stop(context: Context) {
        requestStop(context)
    }

    suspend fun stopAndWait(context: Context) {
        requestStop(context).join()
    }

    private fun requestStop(context: Context): Job {
        synchronized(lifecycleLock) {
            val activeJob = heartbeatJob
            heartbeatJob = null
            val startedAt = sessionStartedAt
            sessionStartedAt = 0L
            val duration = if (startedAt > 0L) {
                ((System.currentTimeMillis() - startedAt) / 1000L).coerceAtLeast(0L)
            } else {
                0L
            }
            val uidAtStop = FirebaseAuth.getInstance().currentUser?.uid
            val previousStop = pendingStopJob
            ++lifecycleGeneration
            activeJob?.cancel()
            val stopJob = scope.launch(start = CoroutineStart.LAZY) {
                previousStop?.join()
                activeJob?.join()
                if (
                    uidAtStop != null &&
                    FirebaseAuth.getInstance().currentUser?.uid == uidAtStop &&
                    hasConsent(uidAtStop)
                ) {
                    record(context.applicationContext, "background", duration)
                }
            }
            pendingStopJob = stopJob
            stopJob.invokeOnCompletion {
                synchronized(lifecycleLock) {
                    if (pendingStopJob === stopJob) {
                        pendingStopJob = null
                    }
                }
            }
            stopJob.start()
            return stopJob
        }
    }

    private suspend fun hasConsent(uid: String): Boolean {
        return runCatching {
            val profile = FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .get()
                .awaitFirebaseResult()
            profile.exists() &&
                profile.getLong("requiredAccountDataConsentVersion") == AccountPrivacyConsent.REQUIRED_ACCOUNT_DATA_VERSION &&
                profile.getBoolean("accountDisabled") != true &&
                profile.getBoolean("dataSharingConsent") == true &&
                profile.getLong("activityDataConsentVersion") == AccountPrivacyConsent.CURRENT_VERSION
        }.getOrDefault(false)
    }

    private suspend fun record(context: Context, event: String, durationSeconds: Long) {
        runCatching {
            val user = FirebaseAuth.getInstance().currentUser ?: return@runCatching
            val profileRef = FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.uid)
            val profile = profileRef.get().awaitFirebaseResult()
            if (
                !profile.exists() ||
                profile.getLong("requiredAccountDataConsentVersion") != AccountPrivacyConsent.REQUIRED_ACCOUNT_DATA_VERSION ||
                profile.getBoolean("accountDisabled") == true ||
                profile.getBoolean("dataSharingConsent") != true ||
                profile.getLong("activityDataConsentVersion") != AccountPrivacyConsent.CURRENT_VERSION
            ) {
                return@runCatching
            }
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val versionCode = if (Build.VERSION.SDK_INT >= 28) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }
            val displayMetrics = context.resources.displayMetrics
            val localeTag = runCatching {
                context.resources.configuration.locales.get(0).toLanguageTag()
            }.getOrDefault(java.util.Locale.getDefault().toLanguageTag()).take(35)
            val timeZoneId = java.util.TimeZone.getDefault().id.take(80)
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val activeNetwork = connectivityManager?.activeNetwork
            val networkCapabilities = if (activeNetwork != null) {
                connectivityManager?.getNetworkCapabilities(activeNetwork)
            } else {
                null
            }
            val networkType = when {
                networkCapabilities == null -> "offline"
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "wifi"
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "cellular"
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "ethernet"
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> "bluetooth"
                else -> "other"
            }
            val payload = hashMapOf<String, Any>(
                "lastActivityEvent" to event,
                "activityUpdatedAt" to FieldValue.serverTimestamp(),
                "deviceManufacturer" to Build.MANUFACTURER.orEmpty().take(60),
                "deviceModel" to Build.MODEL.orEmpty().take(100),
                "deviceBrand" to Build.BRAND.orEmpty().take(60),
                "deviceProduct" to Build.PRODUCT.orEmpty().take(80),
                "deviceCodeName" to Build.DEVICE.orEmpty().take(80),
                "supportedAbis" to Build.SUPPORTED_ABIS.take(4).joinToString(",").take(100),
                "localeTag" to localeTag,
                "timeZoneId" to timeZoneId,
                "screenWidthPx" to displayMetrics.widthPixels.coerceAtLeast(0).toLong(),
                "screenHeightPx" to displayMetrics.heightPixels.coerceAtLeast(0).toLong(),
                "densityDpi" to displayMetrics.densityDpi.coerceAtLeast(0).toLong(),
                "networkType" to networkType,
                "androidVersion" to Build.VERSION.RELEASE.orEmpty().take(40),
                "sdkInt" to Build.VERSION.SDK_INT.toLong(),
                "appVersionName" to packageInfo.versionName.orEmpty().take(40),
                "appVersionCode" to versionCode.coerceAtLeast(0L)
            )
            when (event) {
                "open" -> {
                    if (!profile.contains("firstOpenedAt")) {
                        payload["firstOpenedAt"] = FieldValue.serverTimestamp()
                    }
                    payload["lastOpenedAt"] = FieldValue.serverTimestamp()
                    payload["lastActiveAt"] = FieldValue.serverTimestamp()
                    payload["sessionStartedAt"] = FieldValue.serverTimestamp()
                    payload["appOpenCount"] = FieldValue.increment(1)
                }
                "heartbeat" -> {
                    payload["lastActiveAt"] = FieldValue.serverTimestamp()
                }
                "background" -> {
                    val duration = durationSeconds.coerceIn(0L, 86_400L)
                    payload["lastBackgroundAt"] = FieldValue.serverTimestamp()
                    payload["lastSessionDurationSeconds"] = duration
                    payload["totalForegroundSeconds"] = FieldValue.increment(duration)
                }
                else -> return@runCatching
            }
            profileRef.set(payload, SetOptions.merge()).awaitFirebaseResult()
        }.onSuccess {
            lastSuccessfulWriteAt = System.currentTimeMillis()
            lastWriteError = null
        }.onFailure { error ->
            lastWriteError = error.message?.takeIf { it.isNotBlank() }
                ?: error.javaClass.simpleName
            android.util.Log.e("COC-LIO", "Không thể ghi dữ liệu hoạt động/thiết bị ($event).", error)
        }
    }
}
object FirebaseVillageDataSync {
    suspend fun saveLatest(snapshot: VillageSnapshot): Boolean {
        val user = FirebaseAuth.getInstance().currentUser ?: return false
        val firestore = FirebaseFirestore.getInstance()
        val userRef = firestore.collection("users").document(user.uid)
        val profile = userRef.get().awaitFirebaseResult()
        if (
            !profile.exists() ||
            profile.getBoolean("villageDataConsent") != true ||
            profile.getLong("activityDataConsentVersion") != AccountPrivacyConsent.CURRENT_VERSION
        ) {
            return false
        }

        val playerTag = snapshot.tag.trim()
        require(playerTag.matches(Regex("^#[A-Za-z0-9]+$"))) {
            "Mã người chơi trong JSON không hợp lệ."
        }
        val rawBytes = snapshot.rawJson.toByteArray(Charsets.UTF_8).size
        require(rawBytes in 1..700_000) {
            "JSON lớn hơn giới hạn đồng bộ miễn phí 700 KB. Bản JSON vẫn được lưu trên điện thoại."
        }

        val snapshotData = hashMapOf<String, Any>(
            "uid" to user.uid,
            "playerTag" to playerTag,
            "villageName" to (snapshot.villageName.orEmpty().take(80)),
            "snapshotTimestamp" to snapshot.timestamp,
            "rawJson" to snapshot.rawJson,
            "rawJsonBytes" to rawBytes.toLong(),
            "itemCount" to snapshot.items.size.toLong(),
            "extraItemCount" to snapshot.extraItems.size.toLong(),
            "activeUpgradeCount" to (snapshot.upgrades.size + snapshot.gearUps.size).toLong(),
            "createdAt" to FieldValue.serverTimestamp()
        )
        snapshot.townHallLevel?.let { snapshotData["townHallLevel"] = it.toLong() }
        snapshot.builderHallLevel?.let { snapshotData["builderHallLevel"] = it.toLong() }

        val snapshotId = playerTag.removePrefix("#")
        userRef.collection("village_snapshots").document(snapshotId)
            .set(snapshotData).awaitFirebaseResult()

        val summary = hashMapOf<String, Any>(
            "lastVillageTag" to playerTag,
            "lastVillageName" to snapshot.villageName.orEmpty().take(80),
            "lastVillageTimestamp" to snapshot.timestamp,
            "lastVillageTownHallLevel" to (snapshot.townHallLevel ?: 0).toLong(),
            "lastVillageBuilderHallLevel" to (snapshot.builderHallLevel ?: 0).toLong(),
            "villageImportCount" to FieldValue.increment(1),
            "lastVillageRawJsonBytes" to rawBytes.toLong(),
            "lastVillageSyncedAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        userRef.set(summary, SetOptions.merge()).awaitFirebaseResult()
        return true
    }
}

object FirebaseTelemetry {
    fun logGoogleSignIn(context: Context) {
        runCatching {
            FirebaseAnalytics.getInstance(context).logEvent(
                FirebaseAnalytics.Event.LOGIN,
                Bundle().apply {
                    putString(FirebaseAnalytics.Param.METHOD, "google")
                }
            )
        }
    }

    fun logSignOut(context: Context) {
        runCatching {
            FirebaseAnalytics.getInstance(context).logEvent("logout", Bundle())
        }
    }

    fun logVillageImport(context: Context, hasPreviousSnapshot: Boolean) {
        runCatching {
            FirebaseAnalytics.getInstance(context).logEvent(
                "village_json_import",
                Bundle().apply {
                    putBoolean("has_previous_snapshot", hasPreviousSnapshot)
                }
            )
        }
    }
}

@Suppress("DEPRECATION")
private fun installedSigningSha1(context: Context): String = runCatching {
    val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        context.packageManager.getPackageInfo(
            context.packageName,
            PackageManager.GET_SIGNING_CERTIFICATES
        )
    } else {
        context.packageManager.getPackageInfo(
            context.packageName,
            PackageManager.GET_SIGNATURES
        )
    }
    val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        packageInfo.signingInfo?.apkContentsSigners
    } else {
        packageInfo.signatures
    }
    signatures.orEmpty().map { signature ->
        MessageDigest.getInstance("SHA-1")
            .digest(signature.toByteArray())
            .joinToString(":") { byte ->
                (byte.toInt() and 0xFF).toString(16).uppercase().padStart(2, '0')
            }
    }.distinct().joinToString(", ").ifBlank { "không đọc được" }
}.getOrElse { "không đọc được (" + it.javaClass.simpleName + ")" }

object GoogleAccountAuth {
    suspend fun selectAccount(context: Context): GoogleAccountSelection {
        val clientId = BuildConfig.GOOGLE_WEB_CLIENT_ID
        require(clientId.isNotBlank() && clientId.contains(".apps.googleusercontent.com")) {
            "Đăng nhập Google chưa được cấu hình hoàn chỉnh. Vui lòng liên hệ quản lý ứng dụng."
        }

        val credentialManager = CredentialManager.create(context)
        val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId)
            .build()
        val signInRequest = GetCredentialRequest.Builder()
            .addCredentialOption(signInOption)
            .build()
        val result = try {
            credentialManager.getCredential(context, signInRequest)
        } catch (error: GetCredentialException) {
            val errorMessage = error.message.orEmpty()
            if (errorMessage.contains("28444", ignoreCase = true)) {
                val signingSha1 = installedSigningSha1(context)
                val diagnostic = "packageName=" + context.packageName + "; APK SHA-1=" + signingSha1 + "; GOOGLE_WEB_CLIENT_ID=" + clientId
                android.util.Log.e("COC-LIO_AUTH", diagnostic, error)
                throw IllegalStateException(
                    "Google từ chối cấu hình OAuth [28444]. Web Client ID đang nhúng trong APK: " + clientId + ". Package APK: " + context.packageName + ". SHA-1 thực tế của APK: " + signingSha1 + ". Kiểm tra GOOGLE_WEB_CLIENT_ID phải là OAuth Client loại Web application; Android OAuth Client phải khớp package và SHA-1 thực tế; hai Client phải thuộc cùng dự án Google Cloud.",
                    error
                )
            } else if (errorMessage.contains("[16] Account reauth failed", ignoreCase = true)) {
                runCatching {
                    credentialManager.clearCredentialState(ClearCredentialStateRequest())
                }
                try {
                    credentialManager.getCredential(context, signInRequest)
                } catch (retryError: NoCredentialException) {
                    throw IllegalStateException(
                        "Phiên Google trên điện thoại cần xác thực lại. Hãy kiểm tra tài khoản Google trên thiết bị rồi thử lại. Nếu vẫn lỗi, kiểm tra OAuth Android của COC-LIO.",
                        retryError
                    )
                } catch (retryError: GetCredentialException) {
                    throw IllegalStateException(
                        "Google vẫn từ chối xác thực sau khi làm mới phiên. Web Client ID đang nhúng trong APK: " + clientId + ". Package APK: " + context.packageName + ". SHA-1 thực tế: " + installedSigningSha1(context) + ". Kiểm tra Android OAuth Client theo đúng SHA-1 này và dùng Web application Client ID của cùng dự án.",
                        retryError
                    )
                }
            } else if (error is NoCredentialException) {
                throw IllegalStateException(
                    "Không tìm thấy tài khoản Google có thể đăng nhập. Hãy kiểm tra tài khoản Google trên điện thoại rồi thử lại.",
                    error
                )
            } else {
                throw error
            }
        }

        val credential = result.credential
        require(
            credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) { "Không nhận được thông tin đăng nhập Google hợp lệ." }
        val google = GoogleIdTokenCredential.createFrom(credential.data)
        val email = google.email?.trim()?.takeIf { it.contains("@") }
            ?: throw IllegalStateException("Google không trả về địa chỉ email hợp lệ.")

        return GoogleAccountSelection(
            account = GoogleAccountData(
                email = email.lowercase(),
                displayName = google.displayName?.trim()?.takeIf { it.isNotBlank() },
                givenName = google.givenName?.trim()?.takeIf { it.isNotBlank() },
                familyName = google.familyName?.trim()?.takeIf { it.isNotBlank() },
                profilePictureUri = google.profilePictureUri?.toString()
            ),
            idToken = google.idToken
        )
    }

    suspend fun authenticateSelectedAccount(selection: GoogleAccountSelection): GoogleAccountData {
        val firebaseCredential = GoogleAuthProvider.getCredential(selection.idToken, null)
        val authResult = FirebaseAuth.getInstance()
            .signInWithCredential(firebaseCredential)
            .awaitFirebaseResult()
        val firebaseUser = authResult.user
            ?: throw IllegalStateException("Không nhận được tài khoản Google sau khi xác thực.")
        firebaseUser.reload().awaitFirebaseResult()
        val authenticatedEmail = firebaseUser.email?.trim()
        if (!authenticatedEmail.equals(selection.account.email, ignoreCase = true)) {
            FirebaseAuth.getInstance().signOut()
            throw IllegalStateException("Tài khoản Google hiện tại không khớp với hồ sơ vừa chọn.")
        }
        firebaseUser.getIdToken(true).awaitFirebaseResult()
        return selection.account
    }

    suspend fun shareProfile(
        google: GoogleAccountData,
        countAsSignIn: Boolean = true
    ) {
        val user = FirebaseAuth.getInstance().currentUser
            ?: throw IllegalStateException("Bạn cần đăng nhập Google trước.")
        if (!user.email.equals(google.email, ignoreCase = true)) {
            throw IllegalStateException("Tài khoản Google hiện tại không khớp với hồ sơ vừa chọn.")
        }
        try {
            syncFirebaseUserProfile(user, google, countAsSignIn)
        } catch (error: Exception) {
            if (error.message?.contains("đã bị quản lý vô hiệu hóa", ignoreCase = true) == true) {
                FirebaseAuth.getInstance().signOut()
            }
            throw error
        }
    }

    suspend fun removeSharedProfile() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(user.uid)
            .delete()
            .awaitFirebaseResult()
    }

    suspend fun signOut(context: Context) {
        FirebaseAuth.getInstance().signOut()
        runCatching {
            CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
        }
    }
}

fun openClashOfClans(context: Context) {
    val launch = context.packageManager.getLaunchIntentForPackage("com.supercell.clashofclans")
    if (launch != null) {
        FloatingJsonOverlayController.openGame(context, launch)
    } else {
        val store = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://play.google.com/store/apps/details?id=com.supercell.clashofclans")
        )
        context.startActivity(store)
    }
}

private const val UPGRADE_NOTIFICATION_TIMEOUT_MS = 25_000L

private fun alarmVibrationPattern(): LongArray =
    longArrayOf(0L, 2_000L, 500L, 2_000L, 500L, 2_000L, 500L, 2_000L, 500L)

private fun ensureUpgradeNotificationChannel(
    context: Context,
    channelId: String,
    alarmStyle: Boolean
) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val manager = context.getSystemService(NotificationManager::class.java)
    val channelName = AppText.translate(
        context,
        if (alarmStyle) "Nhắc nâng cấp kiểu báo thức" else "Tin nhắn nâng cấp"
    )
    val channel = NotificationChannel(
        channelId,
        channelName,
        if (alarmStyle) NotificationManager.IMPORTANCE_HIGH else NotificationManager.IMPORTANCE_DEFAULT
    )
    channel.description = AppText.translate(
        context,
        if (alarmStyle) {
            "Thông báo nhắc nâng cấp bằng âm thanh và rung theo cài đặt Android"
        } else {
            "Nhắc khi bộ đếm nâng cấp trong JSON đã hết thời gian"
        }
    )
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) channel.setAllowBubbles(true)
    channel.enableVibration(true)
    if (alarmStyle) {
        channel.vibrationPattern = alarmVibrationPattern()
    }
    channel.setSound(
        if (alarmStyle) Settings.System.DEFAULT_ALARM_ALERT_URI
        else Settings.System.DEFAULT_NOTIFICATION_URI,
        AudioAttributes.Builder()
            .setUsage(if (alarmStyle) AudioAttributes.USAGE_ALARM else AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
    )
    manager.createNotificationChannel(channel)
}

private fun applyLegacyNotificationSettings(
    builder: NotificationCompat.Builder,
    alarmStyle: Boolean
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) return
    builder
        .setSound(
            if (alarmStyle) Settings.System.DEFAULT_ALARM_ALERT_URI
            else Settings.System.DEFAULT_NOTIFICATION_URI
        )
        .setVibrate(
            if (alarmStyle) alarmVibrationPattern()
            else longArrayOf(0, 250)
        )
}

internal fun notificationDisplayItemName(itemName: String): String {
    val cleanName = itemName.replace(Regex("""\s*#\s*\d+\b"""), "").trim()
    return if (
        cleanName.isBlank() ||
        cleanName.equals("Mục", ignoreCase = true) ||
        cleanName.equals("Item", ignoreCase = true)
    ) {
        "Mục chưa được nhận diện"
    } else {
        cleanName
    }
}

object UpgradeReminderScheduler {
    private fun profileTag(profile: String) =
        "coc_lio_upgrade_" + profile.hashCode().toUInt().toString(16)

    fun scheduleAll(context: Context, snapshots: List<VillageSnapshot>, profile: String, enabled: Boolean) {
        val manager = WorkManager.getInstance(context)
        val tag = profileTag(profile)
        manager.cancelAllWorkByTag(tag)

        val nowSeconds = System.currentTimeMillis() / 1000L
        val remindersEnabled = enabled || AppPreferences.bubbleNotificationsEnabled(context, profile)
        snapshots.distinctBy { it.tag }.forEach { snapshot ->
            (snapshot.upgrades + snapshot.gearUps).forEachIndexed { itemIndex, item ->
                val delay = item.remainingSecondsAt(snapshot.timestamp, nowSeconds)
                if (delay <= 0L) return@forEachIndexed

                val endAt = snapshot.timestamp + (item.timerSeconds ?: 0L)
                val refreshName = "coc_lio_widget_finish_" + profile.hashCode() + "_" +
                    snapshot.tag + "_" + item.identity.hashCode() + "_" + endAt + "_" + itemIndex
                val refreshRequest = OneTimeWorkRequestBuilder<WidgetRefreshWorker>()
                    .setInitialDelay(delay, TimeUnit.SECONDS)
                    .addTag(tag)
                    .build()
                manager.enqueueUniqueWork(refreshName, ExistingWorkPolicy.REPLACE, refreshRequest)

                if (!remindersEnabled) return@forEachIndexed

                val uniqueName = "coc_lio_" + profile.hashCode() + "_" + snapshot.tag +
                    "_" + item.identity.hashCode() + "_" + endAt + "_" + itemIndex
                val data = Data.Builder()
                    .putString("profile", profile)
                    .putString("tag", snapshot.tag)
                    .putString("village_name", snapshot.villageName ?: snapshot.tag)
                    .putString("item", item.name)
                    .putString("section", item.sectionLabel)
                    .putString("base", item.base)
                    .putInt("data_id", item.dataId)
                    .putInt("level", item.level ?: -1)
                    .putBoolean("gear_up", item.gearUp)
                    .putString("icon_url", CocAssets.imageUrl(item))
                    .putLong("end_at", endAt)
                    .build()
                val request = OneTimeWorkRequestBuilder<UpgradeReminderWorker>()
                    .setInitialDelay(delay, TimeUnit.SECONDS)
                    .setInputData(data)
                    .addTag(tag)
                    .build()
                manager.enqueueUniqueWork(uniqueName, ExistingWorkPolicy.REPLACE, request)
            }
        }
    }
}
class UpgradeReminderWorker(
    context: Context,
    parameters: WorkerParameters
) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val profile = inputData.getString("profile") ?: "local"
        val normalNotificationsEnabled = AppPreferences.notificationsEnabled(applicationContext, profile)
        val bubbleNotificationsEnabled = AppPreferences.bubbleNotificationsEnabled(applicationContext, profile)
        if (!normalNotificationsEnabled && !bubbleNotificationsEnabled) {
            return@withContext Result.success()
        }

        val item = inputData.getString("item") ?: "Công trình"
        val tag = inputData.getString("tag") ?: ""
        val villageName = inputData.getString("village_name") ?: tag
        val section = inputData.getString("section") ?: "Công trình"
        val base = inputData.getString("base") ?: "home"
        val dataId = inputData.getInt("data_id", -1)
        val level = inputData.getInt("level", -1)
        val gearUp = inputData.getBoolean("gear_up", false)
        val iconUrl = inputData.getString("icon_url")
        val endAt = inputData.getLong("end_at", 0L)
        val alarmStyle = AppPreferences.notificationStyle(applicationContext, profile) == "alarm"
        val profileCode = profile.hashCode().toUInt().toString(16)
        val channelId = "upgrade_" + profileCode +
            if (alarmStyle) "_alarm_v6" else "_message"
        ensureUpgradeNotificationChannel(applicationContext, channelId, alarmStyle)

        val notificationKey = "$profile|$tag|$dataId|$endAt"
        val notificationId = notificationKey.hashCode()
        val openApp = Intent(applicationContext, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            notificationId,
            openApp,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val levelText = AppText.translate(
            applicationContext,
            if (level >= 0) {
                if (gearUp) "Cấp $level • Tấn công nhanh" else "Cấp $level → ${level + 1}"
            } else {
                if (gearUp) "TẤN CÔNG NHANH" else "Chưa rõ cấp độ"
            }
        )
        val baseText = AppText.translate(
            applicationContext,
            if (base == "builder") "Làng đêm" else "Làng chính"
        )
        val localizedItem = notificationDisplayItemName(AppText.translate(applicationContext, item))
        val localizedSection = AppText.translate(applicationContext, section)
        val notificationBody = listOf(
            "${AppText.translate(applicationContext, "Làng")}: $villageName ($tag)",
            "${AppText.translate(applicationContext, "Căn cứ")}: $baseText",
            "${AppText.translate(applicationContext, "Nhóm")}: $localizedSection",
            "${AppText.translate(applicationContext, "Công trình")}: $localizedItem",
            "${AppText.translate(applicationContext, "Dự kiến")}: $levelText",
            AppText.translate(
                applicationContext,
                "Bộ đếm JSON đã hết giờ; hãy nhập JSON mới để xác nhận cấp độ trong game."
            )
        ).joinToString("\n")

        val remoteTemplates = applicationContext.getSharedPreferences(
            "coc_lio_remote_content",
            Context.MODE_PRIVATE
        )
        fun remoteNotificationText(key: String, fallback: String): String {
            val template = remoteTemplates.getString("notification_$key", null)
                ?.takeIf { it.isNotBlank() && it.length <= 1800 }
                ?: fallback
            return template
                .replace(Regex("(?i)\\b(?:ID|Mã)\\s*[:#]?\\s*\\{id\\}"), "")
                .replace("{item}", localizedItem)
                .replace("{village}", villageName)
                .replace("{tag}", tag)
                .replace("{id}", "")
                .replace("{level}", levelText)
                .replace("{base}", baseText)
                .replace("{section}", localizedSection)
                .take(1800)
        }
        val gameIntent = applicationContext.packageManager
            .getLaunchIntentForPackage("com.supercell.clashofclans")
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val iconBitmap = iconUrl?.let { url ->
            runCatching {
                downloadWidgetIcon(applicationContext, url)
                widgetIconFile(applicationContext, url)
                    .takeIf { it.exists() && it.length() > 0L }
                    ?.let { BitmapFactory.decodeFile(it.absolutePath) }
            }.getOrNull()
        }
        val notificationTitle = remoteNotificationText(
            "title",
            AppText.translate(applicationContext, "Đến giờ hoàn thành") + " • $localizedItem"
        )
        val notificationText = remoteNotificationText("text", "$villageName • $baseText • $levelText")
        val notificationExpandedText = remoteNotificationText("body", notificationBody)
        val notificationBuilder = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setCategory(if (alarmStyle) NotificationCompat.CATEGORY_ALARM else NotificationCompat.CATEGORY_MESSAGE)
            .setContentTitle(notificationTitle)
            .setContentText(notificationText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(notificationExpandedText))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setOngoing(false)
            .setOnlyAlertOnce(false)
            .setTimeoutAfter(UPGRADE_NOTIFICATION_TIMEOUT_MS)
            .setPriority(if (alarmStyle) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
        if (gameIntent != null) {
            val gamePendingIntent = PendingIntent.getActivity(
                applicationContext,
                notificationId xor 0x4C494F,
                gameIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            notificationBuilder.addAction(android.R.drawable.ic_media_play, AppText.translate(applicationContext, "MỞ TRÒ CHƠI"), gamePendingIntent)
        }
        if (iconBitmap != null) notificationBuilder.setLargeIcon(iconBitmap)
        applyLegacyNotificationSettings(notificationBuilder, alarmStyle)
        val notification = notificationBuilder.build()
        val notificationPermissionGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        if (normalNotificationsEnabled && notificationPermissionGranted) {
            runCatching {
                NotificationManagerCompat.from(applicationContext)
                    .notify(notificationId, notification)
            }
        }
        if (
            bubbleNotificationsEnabled &&
            (Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
                Settings.canDrawOverlays(applicationContext))
        ) {
            applicationContext.sendBroadcast(
                Intent(NotificationBubbleService.ACTION_SHOW_ALERT)
                    .setPackage(applicationContext.packageName)
                    .putExtra(NotificationBubbleService.EXTRA_TITLE, notificationTitle)
                    .putExtra(
                        NotificationBubbleService.EXTRA_BODY,
                        (notificationText + "\n" + notificationExpandedText).take(1800)
                    )
            )
        }
        Result.success()
    }
}


object UpgradeNotificationTester {
    fun show(
        context: Context,
        profile: String,
        style: String
    ): Boolean = runCatching {
        val alarmStyle = style == "alarm"
        val profileCode = profile.hashCode().toUInt().toString(16)
        val channelId = "upgrade_" + profileCode +
            if (alarmStyle) "_alarm_v6" else "_message"
        ensureUpgradeNotificationChannel(context, channelId, alarmStyle)

        val openApp = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(
            context,
            ("coc_lio_test_" + profile).hashCode(),
            openApp,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val title = AppText.translate(context, "COC-LIO • Thử thông báo")
        val message = AppText.translate(
            context,
            if (alarmStyle) {
                "Đây là thông báo thử kiểu báo thức. Âm thanh và rung phụ thuộc cài đặt thông báo của Android."
            } else {
                "Đây là thông báo thử kiểu tin nhắn. Thông báo nâng cấp thật sẽ xuất hiện khi bộ đếm kết thúc."
            }
        )
        val notificationBuilder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setCategory(if (alarmStyle) NotificationCompat.CATEGORY_ALARM else NotificationCompat.CATEGORY_MESSAGE)
            .setContentTitle(title)
            .setContentText(AppText.translate(context, if (alarmStyle) "Đang thử kiểu báo thức" else "Đang thử kiểu tin nhắn"))
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setOngoing(false)
            .setTimeoutAfter(UPGRADE_NOTIFICATION_TIMEOUT_MS)
            .setPriority(if (alarmStyle) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
        val testNotificationId = ("coc_lio_test_" + profile).hashCode()
        val gameIntent = context.packageManager
            .getLaunchIntentForPackage("com.supercell.clashofclans")
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        if (gameIntent != null) {
            val gamePendingIntent = PendingIntent.getActivity(
                context,
                testNotificationId xor 0x4C494F,
                gameIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            notificationBuilder.addAction(android.R.drawable.ic_media_play, AppText.translate(context, "MỞ TRÒ CHƠI"), gamePendingIntent)
        }
        applyLegacyNotificationSettings(notificationBuilder, alarmStyle)
        val notification = notificationBuilder.build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return@runCatching false
        }
        NotificationManagerCompat.from(context).cancel(testNotificationId)
        NotificationManagerCompat.from(context).notify(testNotificationId, notification)
        true
    }.getOrDefault(false)
}

class WidgetRefreshWorker(
    context: Context,
    parameters: WorkerParameters
) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        AppWidgetUpdater.updateAll(applicationContext)
        return Result.success()
    }
}

private fun widgetIconFile(context: Context, url: String): File {
    val directory = File(context.cacheDir, "coc-widget-icons")
    return File(directory, url.hashCode().toUInt().toString(16) + ".png")
}

private fun downloadWidgetIcon(context: Context, url: String) {
    val target = widgetIconFile(context, url)
    if (target.exists() && target.length() > 0L) return

    var connection: HttpURLConnection? = null
    try {
        connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 4000
            readTimeout = 5000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "COC-LIO")
        }
        if (connection.responseCode !in 200..299) return

        val bytes = connection.inputStream.use { it.readBytes() }
        if (bytes.isEmpty()) return
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        var sampleSize = 1
        val largestSide = maxOf(bounds.outWidth, bounds.outHeight)
        while (largestSide / sampleSize > 128) sampleSize *= 2
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options) ?: return
        target.parentFile?.mkdirs()
        FileOutputStream(target).use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        bitmap.recycle()
    } catch (_: Exception) {
        target.delete()
    } finally {
        connection?.disconnect()
    }
}

class WidgetIconPreloadWorker(
    context: Context,
    parameters: WorkerParameters
) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val widgetId = inputData.getInt("widget_id", AppWidgetManager.INVALID_APPWIDGET_ID)
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            return@withContext Result.success()
        }
        val preferences = applicationContext.getSharedPreferences("coc_lio_widgets", Context.MODE_PRIVATE)
        val profile = preferences.getString("profile_$widgetId", AccountProfileStore.active(applicationContext)) ?: "local"
        val tag = preferences.getString("tag_$widgetId", null)
        val snapshot = VillageStore.loadAll(applicationContext, profile).firstOrNull {
            tag == null || it.tag == tag
        }
        val urls = snapshot?.let { village ->
            (village.upgrades + village.gearUps).mapNotNull { CocAssets.imageUrl(it) }.distinct()
        }.orEmpty()

        urls.forEach { url -> downloadWidgetIcon(applicationContext, url) }
        AppWidgetManager.getInstance(applicationContext)
            .notifyAppWidgetViewDataChanged(widgetId, R.id.widget_list)
        Result.success()
    }
}

class UpgradeWidgetListService : android.widget.RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return UpgradeWidgetListFactory(applicationContext, intent)
    }
}

private data class WidgetUpgradeEntry(
    val name: String,
    val level: Int?,
    val section: String,
    val imageUrl: String?,
    val base: String,
    val gearUp: Boolean,
    val endAt: Long,
    val totalSeconds: Long
)

private class UpgradeWidgetListFactory(
    private val context: Context,
    intent: Intent
) : android.widget.RemoteViewsService.RemoteViewsFactory {
    private val widgetId = intent.getIntExtra(
        AppWidgetManager.EXTRA_APPWIDGET_ID,
        AppWidgetManager.INVALID_APPWIDGET_ID
    )
    private var entries: List<WidgetUpgradeEntry> = emptyList()
    private val decodedIcons = mutableMapOf<String, Bitmap>()

    override fun onCreate() = Unit

    override fun onDataSetChanged() {
        val prefs = context.getSharedPreferences("coc_lio_widgets", Context.MODE_PRIVATE)
        val profile = prefs.getString("profile_" + widgetId, AccountProfileStore.active(context)) ?: "local"
        val tag = prefs.getString("tag_" + widgetId, null)
        val snapshot = VillageStore.loadAll(context, profile).firstOrNull {
            tag == null || it.tag == tag
        }

        val nowSeconds = System.currentTimeMillis() / 1000L
        entries = if (snapshot == null) {
            emptyList()
        } else {
            val expired = snapshot.expiredUpgradesAt(nowSeconds)
            val active = snapshot.activeUpgradesAt(nowSeconds)
            val completedOrdered = expired.sortedBy { it.timerSeconds ?: Long.MAX_VALUE }
            val homeRunning = active.filter { it.base == "home" }
                .sortedBy { it.timerSeconds ?: Long.MAX_VALUE }
            val builderRunning = active.filter { it.base == "builder" }
                .sortedBy { it.timerSeconds ?: Long.MAX_VALUE }
            (completedOrdered + homeRunning + builderRunning)
                .mapNotNull { item ->
                    val seconds = item.timerSeconds ?: return@mapNotNull null
                    if (seconds <= 0L) return@mapNotNull null
                    WidgetUpgradeEntry(
                        name = item.name,
                        level = item.level,
                        section = item.sectionLabel,
                        imageUrl = CocAssets.imageUrl(item),
                        base = item.base,
                        gearUp = item.gearUp,
                        endAt = snapshot.timestamp + seconds,
                        totalSeconds = seconds
                    )
                }
        }
    }

    override fun getViewAt(position: Int): RemoteViews? {
        val entry = entries.getOrNull(position) ?: return null
        val views = RemoteViews(context.packageName, R.layout.widget_upgrade_row)
        val displayScale = AppPreferences.displaySizeScale(context)
        val combinedScale = AppPreferences.textScale(context) * displayScale
        views.setTextViewTextSize(R.id.row_title, TypedValue.COMPLEX_UNIT_SP, 12f * combinedScale)
        views.setTextViewTextSize(R.id.row_subtitle, TypedValue.COMPLEX_UNIT_SP, 7f * combinedScale)
        views.setTextViewTextSize(R.id.row_timer, TypedValue.COMPLEX_UNIT_SP, 13f * combinedScale)
        val rowPadding = (4f * displayScale * context.resources.displayMetrics.density).roundToInt()
        views.setViewPadding(R.id.row_root, rowPadding, rowPadding, rowPadding, rowPadding)
        if (Build.VERSION.SDK_INT >= 31) {
            views.setViewLayoutWidth(R.id.row_icon, 40f * displayScale, TypedValue.COMPLEX_UNIT_DIP)
            views.setViewLayoutHeight(R.id.row_icon, 40f * displayScale, TypedValue.COMPLEX_UNIT_DIP)
            views.setViewLayoutWidth(R.id.row_timer, 84f * displayScale, TypedValue.COMPLEX_UNIT_DIP)
            views.setViewLayoutHeight(R.id.row_timer, 24f * displayScale, TypedValue.COMPLEX_UNIT_DIP)
            views.setViewLayoutHeight(R.id.row_progress_container, 30f * displayScale, TypedValue.COMPLEX_UNIT_DIP)
            views.setViewLayoutHeight(R.id.row_progress, 17f * displayScale, TypedValue.COMPLEX_UNIT_DIP)
            val timerPadding = (2f * displayScale * context.resources.displayMetrics.density).roundToInt()
            views.setViewPadding(R.id.row_timer, timerPadding, 0, timerPadding, 0)
        }
        val now = System.currentTimeMillis() / 1000L
        val remaining = (entry.endAt - now).coerceIn(0L, entry.totalSeconds.coerceAtLeast(1L))
        val totalSeconds = entry.totalSeconds.coerceAtLeast(1L)
        val startAt = entry.endAt - totalSeconds
        val elapsedSeconds = (now - startAt).coerceIn(0L, totalSeconds)
        val progressPercent = (elapsedSeconds.toDouble() * 100.0 / totalSeconds).toInt().coerceIn(0, 100)

        val iconBitmap = entry.imageUrl?.let { url ->
            decodedIcons[url] ?: widgetIconFile(context, url)
                .takeIf { it.exists() }
                ?.let { BitmapFactory.decodeFile(it.absolutePath) }
                ?.also { decodedIcons[url] = it }
        }
        if (iconBitmap != null) {
            views.setImageViewBitmap(R.id.row_icon, iconBitmap)
            views.setViewVisibility(R.id.row_icon, View.VISIBLE)
        } else {
            views.setViewVisibility(R.id.row_icon, View.GONE)
        }
        val levelText = when {
            entry.gearUp -> entry.level?.let { "CẤP $it • TẤN CÔNG NHANH" } ?: "TẤN CÔNG NHANH"
            entry.level != null -> "CẤP ${entry.level} → ${entry.level + 1}"
            else -> "ĐANG NÂNG"
        }
        views.setTextViewText(
            R.id.row_title,
            AppText.translate(
                context,
                if (remaining > 0L) "$levelText • $progressPercent%" else levelText
            )
        )
        views.setTextViewTextSize(
            R.id.row_timer,
            TypedValue.COMPLEX_UNIT_SP,
            (if (remaining > 0L) 13f else 10f) * combinedScale
        )
        views.setViewVisibility(R.id.row_subtitle, View.GONE)
        views.setProgressBar(R.id.row_progress, 100, progressPercent, false)
        val progressFraction = progressPercent / 100f
        val progressColor = if (progressFraction < 0.5f) {
            val fraction = progressFraction * 2f
            android.graphics.Color.rgb(
                255,
                (59f + (213f - 59f) * fraction).toInt(),
                (48f + (79f - 48f) * fraction).toInt()
            )
        } else {
            val fraction = (progressFraction - 0.5f) * 2f
            android.graphics.Color.rgb(
                (255f + (37f - 255f) * fraction).toInt(),
                (213f + (211f - 213f) * fraction).toInt(),
                (79f + (102f - 79f) * fraction).toInt()
            )
        }
        if (Build.VERSION.SDK_INT >= 31) {
            views.setColorStateList(
                R.id.row_progress,
                "setProgressTintList",
                android.content.res.ColorStateList.valueOf(progressColor)
            )
        }
        views.setTextColor(
            R.id.row_timer,
            if (remaining > 0L) android.graphics.Color.WHITE else android.graphics.Color.rgb(255, 213, 79)
        )

        if (remaining > 0L) {
            views.setChronometerCountDown(R.id.row_timer, true)
            views.setChronometer(
                R.id.row_timer,
                SystemClock.elapsedRealtime() + remaining * 1000L,
                "%s",
                true
            )
        } else {
            views.setChronometerCountDown(R.id.row_timer, false)
            views.setChronometer(R.id.row_timer, SystemClock.elapsedRealtime(), "%s", false)
            views.setTextViewText(R.id.row_timer, AppText.translate(context, "HOÀN THÀNH"))
        }
        return views
    }

    override fun getCount(): Int = entries.size
    override fun getItemId(position: Int): Long = entries.getOrNull(position)?.let {
        (it.name + it.endAt).hashCode().toLong()
    } ?: position.toLong()
    override fun getViewTypeCount(): Int = 1
    override fun hasStableIds(): Boolean = true
    override fun getLoadingView(): RemoteViews? {
        return RemoteViews(context.packageName, R.layout.widget_upgrade_row).apply {
            setViewVisibility(R.id.row_icon, View.GONE)
            setTextViewText(R.id.row_title, AppText.translate(context, "Đang tải danh sách nâng cấp…"))
            setTextViewText(R.id.row_subtitle, "COC-LIO")
            setTextViewText(R.id.row_timer, AppText.translate(context, "ĐANG CẬP NHẬT"))
            setProgressBar(R.id.row_progress, 100, 0, false)
        }
    }

    override fun onDestroy() {
        entries = emptyList()
        decodedIcons.clear()
    }
}

object AppWidgetUpdater {
    private fun updateLocalizedPreview(context: Context) {
        if (Build.VERSION.SDK_INT < 35) return
        val status = AppText.translate(context, "2 mục đang nâng • Thợ rảnh 4/6")
        val firstTitle = AppText.translate(context, "CẤP 14 → 15 • CÒN 56%")
        val secondTitle = AppText.translate(context, "CẤP 8 → 9 • CÒN 32%")
        val signature = listOf(status, firstTitle, secondTitle).joinToString("|")
        val previewPrefs = context.getSharedPreferences("coc_lio_widgets", Context.MODE_PRIVATE)
        if (previewPrefs.getString("generated_preview_signature", null) == signature) return
        runCatching {
            val preview = RemoteViews(context.packageName, R.layout.widget_upgrade_preview).apply {
                setTextViewText(R.id.widget_preview_status, status)
                setTextViewText(R.id.widget_preview_row_title_1, firstTitle)
                setTextViewText(R.id.widget_preview_row_title_2, secondTitle)
            }
            val previewSet = AppWidgetManager.getInstance(context).setWidgetPreview(
                ComponentName(context, UpgradeWidgetProvider::class.java),
                AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN,
                preview
            )
            if (previewSet) {
                previewPrefs.edit().putString("generated_preview_signature", signature).apply()
            }
        }
    }

    fun ensureRefresh(context: Context) {
        updateLocalizedPreview(context)
        val request = PeriodicWorkRequestBuilder<WidgetRefreshWorker>(15, TimeUnit.MINUTES).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "coc_lio_widget_refresh",
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun updateAll(context: Context) {
        updateLocalizedPreview(context)
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(ComponentName(context, UpgradeWidgetProvider::class.java))
        ids.forEach { updateWidget(context, it) }
    }

    fun updateWidget(context: Context, appWidgetId: Int) {
        val prefs = context.getSharedPreferences("coc_lio_widgets", Context.MODE_PRIVATE)
        val profile = prefs.getString("profile_" + appWidgetId, AccountProfileStore.active(context)) ?: "local"
        val tag = prefs.getString("tag_" + appWidgetId, null)
        val snapshot = VillageStore.loadAll(context, profile).firstOrNull { tag == null || it.tag == tag }
        val nowSeconds = System.currentTimeMillis() / 1000L
        val activeCount = snapshot?.activeUpgradesAt(nowSeconds)?.size ?: 0
        val expiredCount = snapshot?.expiredUpgradesAt(nowSeconds)?.size ?: 0
        val views = RemoteViews(context.packageName, R.layout.widget_upgrade)
        val combinedScale = AppPreferences.textScale(context) * AppPreferences.displaySizeScale(context)
        views.setTextViewTextSize(R.id.widget_village, TypedValue.COMPLEX_UNIT_SP, 10f * combinedScale)
        views.setTextViewTextSize(R.id.widget_status, TypedValue.COMPLEX_UNIT_SP, 9f * combinedScale)
        views.setTextViewTextSize(R.id.widget_empty, TypedValue.COMPLEX_UNIT_SP, 9f * combinedScale)
        views.setTextViewText(R.id.widget_empty, AppText.translate(context, "Chưa có mục nào đang nâng cấp"))
        val widgetPadding = (5f * AppPreferences.displaySizeScale(context) * context.resources.displayMetrics.density).roundToInt()
        views.setViewPadding(R.id.widget_root, widgetPadding, widgetPadding, widgetPadding, widgetPadding)

        val builderTotal = snapshot?.homeBuilderTotal
        val buildersFree = if (snapshot == null || builderTotal == null) null else
            (builderTotal - snapshot.homeBuildersBusyAt(nowSeconds)).coerceAtLeast(0)
        views.setTextViewText(
            R.id.widget_village,
            if (snapshot == null) "COC-LIO • " + AppText.translate(context, "Chưa có dữ liệu")
            else (snapshot.villageName ?: snapshot.tag) +
                (if (snapshot.villageName.isNullOrBlank()) "" else " • " + snapshot.tag) +
                " • " + AppText.translate(context, "Nhà chính") + " " + (snapshot.townHallLevel ?: "—") +
                (if (builderTotal == null) "" else " • " + AppText.translate(context, "Thợ rảnh") + " $buildersFree/$builderTotal")
        )
        views.setTextViewText(
            R.id.widget_status,
            when {
                snapshot == null -> AppText.translate(context, "Mở ứng dụng và nhập dữ liệu JSON")
                activeCount == 0 && expiredCount == 0 -> AppText.translate(context, "Không có mục nào đang nâng cấp")
                activeCount > 0 && expiredCount > 0 -> "$activeCount " + AppText.translate(context, "đang nâng") + " • $expiredCount " + AppText.translate(context, "hoàn thành")
                activeCount > 0 -> "$activeCount " + AppText.translate(context, "mục đang nâng cấp")
                else -> "$expiredCount " + AppText.translate(context, "hoàn thành • chờ JSON mới")
            }
        )

        val adapterIntent = Intent(context, UpgradeWidgetListService::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            data = Uri.parse("coclio://widget/$appWidgetId")
        }
        views.setRemoteAdapter(R.id.widget_list, adapterIntent)
        views.setEmptyView(R.id.widget_list, R.id.widget_empty)

        val open = PendingIntent.getActivity(
            context,
            appWidgetId,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_root, open)

        val manager = AppWidgetManager.getInstance(context)
        manager.updateAppWidget(appWidgetId, views)
        manager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_list)

        val preload = OneTimeWorkRequestBuilder<WidgetIconPreloadWorker>()
            .setInputData(Data.Builder().putInt("widget_id", appWidgetId).build())
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "coc_lio_widget_icons_" + appWidgetId,
            ExistingWorkPolicy.KEEP,
            preload
        )
    }
}

class UpgradeWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { AppWidgetUpdater.updateWidget(context, it) }
        AppWidgetUpdater.ensureRefresh(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == Intent.ACTION_LOCALE_CHANGED) {
            AppWidgetUpdater.updateAll(context)
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        manager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        AppWidgetUpdater.updateWidget(context, appWidgetId)
    }
}

class UpgradeWidgetConfigActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val appWidgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        )
        setResult(RESULT_CANCELED)
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        val profile = AccountProfileStore.active(this)
        val villages = VillageStore.loadAll(this, profile).distinctBy { it.tag }
        if (villages.isEmpty()) {
            AlertDialog.Builder(this)
                .setTitle(AppText.translate(this, "Chưa có làng để theo dõi"))
                .setMessage(AppText.translate(this, "Hãy mở COC-LIO, đăng nhập tài khoản cần dùng (nếu có), nhập JSON ít nhất một lần rồi thêm tiện ích."))
                .setPositiveButton(AppText.translate(this, "Đóng")) { _, _ -> finish() }
                .show()
            return
        }

        val labels = villages.map {
            (it.villageName?.takeIf { name -> name.isNotBlank() } ?: it.tag) +
                (if (it.villageName.isNullOrBlank()) "" else " • " + it.tag) +
                " • " + AppText.translate(this, "Nhà chính") + " " + (it.townHallLevel ?: "—")
        }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle(AppText.translate(this, "Chọn làng cho tiện ích"))
            .setItems(labels) { _, which ->
                getSharedPreferences("coc_lio_widgets", Context.MODE_PRIVATE)
                    .edit()
                    .putString("profile_" + appWidgetId, profile)
                    .putString("tag_" + appWidgetId, villages[which].tag)
                    .apply()
                AppWidgetUpdater.updateWidget(this, appWidgetId)
                setResult(
                    RESULT_OK,
                    Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                )
                finish()
            }
            .setOnCancelListener { finish() }
            .show()
    }
}

@Composable
private fun ProfileDataRow(label: String, value: String?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, modifier = Modifier.weight(0.9f))
        Text(
            value?.takeIf { it.isNotBlank() } ?: "Google không cung cấp",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 10.sp,
            modifier = Modifier.weight(1.1f)
        )
    }
}

private fun formatAdminAccountTime(timestamp: Long): String =
    if (timestamp <= 0L) {
        "Chưa ghi nhận"
    } else {
        java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
            .withZone(java.time.ZoneId.systemDefault())
            .format(java.time.Instant.ofEpochMilli(timestamp))
    }

@Composable
private fun AdminAccountsPanel(onClose: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var accounts by remember { mutableStateOf<List<AdminGoogleAccount>>(emptyList()) }
    var expandedAccounts by remember { mutableStateOf<Set<String>>(emptySet()) }
    var loading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var pendingDelete by remember { mutableStateOf<AdminGoogleAccount?>(null) }
    var pendingProfileRemoval by remember { mutableStateOf<AdminGoogleAccount?>(null) }
    var deleting by remember { mutableStateOf(false) }

    fun refreshAccounts() {
        scope.launch {
            loading = true
            errorMessage = null
            try {
                accounts = FirebaseAccountAdministration.loadAccounts(context)
            } catch (error: Exception) {
                errorMessage = error.message ?: "Không thể tải danh sách tài khoản."
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshAccounts()
        while (true) {
            delay(300_000L)
            scope.launch {
                loading = false
                errorMessage = null
                try {
                    accounts = FirebaseAccountAdministration.loadAccounts(context)
                } catch (error: Exception) {
                    errorMessage = error.message ?: "Không thể cập nhật trạng thái tài khoản."
                }
            }
        }
    }

    pendingDelete?.let { account ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { if (!deleting) pendingDelete = null },
            title = { Text(if (account.accountDisabled) "Kích hoạt lại tài khoản COC-LIO?" else "Vô hiệu hóa tài khoản COC-LIO?") },
            text = {
                Text(
                    if (account.accountDisabled) "Tài khoản Google vẫn tồn tại. Kích hoạt lại sẽ cho phép tài khoản này truy cập COC-LIO và dữ liệu của mình theo quyền bình thường." else "Tài khoản Google vẫn tồn tại. COC-LIO sẽ đánh dấu hồ sơ này bị vô hiệu hóa để chặn truy cập dữ liệu tài khoản. Dữ liệu cũ không bị xóa.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            },
            confirmButton = {
                TextButton(
                    enabled = !deleting,
                    onClick = {
                        scope.launch {
                            deleting = true
                            errorMessage = null
                            try {
                                FirebaseAccountAdministration.setAccountDisabled(account.uid, !account.accountDisabled)
                                pendingDelete = null
                                expandedAccounts = expandedAccounts - account.uid
                                accounts = FirebaseAccountAdministration.loadAccounts(context)
                            } catch (error: Exception) {
                                errorMessage = error.message ?: "Không thể vô hiệu hóa tài khoản COC-LIO."
                            } finally {
                                deleting = false
                            }
                        }
                    }
                ) {
                    Text(if (deleting) "ĐANG XỬ LÝ…" else if (account.accountDisabled) "KÍCH HOẠT LẠI" else "VÔ HIỆU HÓA")
                }
            },
            dismissButton = {
                TextButton(enabled = !deleting, onClick = { pendingDelete = null }) { Text("HỦY") }
            }
        )
    }

    pendingProfileRemoval?.let { account ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { if (!deleting) pendingProfileRemoval = null },
            title = { Text("Xóa hồ sơ Firebase cũ?") },
            text = {
                Text(
                    "Thao tác này chỉ xóa hồ sơ users/${account.uid}, dữ liệu JSON đồng bộ theo làng và giấy phép của hồ sơ khỏi Firestore. Nó KHÔNG xóa tài khoản đăng nhập trong Firebase Authentication. Chỉ dùng sau khi bạn đã xóa tài khoản đó ở Authentication.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            },
            confirmButton = {
                TextButton(
                    enabled = !deleting,
                    onClick = {
                        scope.launch {
                            deleting = true
                            errorMessage = null
                            try {
                                FirebaseAccountAdministration.deleteStoredAccountProfile(account.uid)
                                pendingProfileRemoval = null
                                expandedAccounts = expandedAccounts - account.uid
                                accounts = FirebaseAccountAdministration.loadAccounts(context)
                            } catch (error: Exception) {
                                errorMessage = error.message ?: "Không thể xóa hồ sơ Firebase cũ."
                            } finally {
                                deleting = false
                            }
                        }
                    }
                ) {
                    Text(if (deleting) "ĐANG XÓA…" else "XÓA HỒ SƠ")
                }
            },
            dismissButton = {
                TextButton(enabled = !deleting, onClick = { pendingProfileRemoval = null }) {
                    Text("HỦY")
                }
            }
        )
    }

    FeaturePanel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "DANH SÁCH QUẢN LÝ NGƯỜI DÙNG",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onClose) {
                Text("ĐÓNG", fontSize = 9.sp)
            }
            TextButton(onClick = { refreshAccounts() }, enabled = !loading) {
                Text("LÀM MỚI", fontSize = 9.sp)
            }
        }
        Text(
            "Tổng số tài khoản: ${accounts.size}",
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp
        )
        Text(
            "Danh sách lấy từ hồ sơ Firestore. Nếu bạn đã xóa tài khoản trong Firebase Authentication nhưng dòng này vẫn còn, hãy dùng XÓA HỒ SƠ sau khi kiểm tra đúng UID. Nút này chỉ xóa hồ sơ Firestore, không xóa tài khoản Authentication.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.sp,
            lineHeight = 15.sp
        )
        if (loading) {
            Text("Đang tải dữ liệu tài khoản…", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
        }
        errorMessage?.let {
            Text(it, color = Color(0xFFFF5D91), fontSize = 10.sp)
        }
        accounts.forEach { account ->
            val expanded = account.uid in expandedAccounts
            Spacer(Modifier.height(7.dp))
            Card(
                modifier = Modifier.fillMaxWidth().ledRgbBorder(cornerRadiusDp = 13f, strokeWidthDp = 2.2f),
                shape = RoundedCornerShape(13.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (account.photoUrl.isNotBlank()) {
                            AsyncImage(
                                model = account.photoUrl,
                                contentDescription = AppText.translate(context, "Ảnh đại diện Google"),
                                modifier = Modifier.size(42.dp).clip(CircleShape)
                            )
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                account.preferredName.ifBlank { account.displayName.ifBlank { account.email } },
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                            Text(account.email, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
                            Text(
                                when { account.accountDisabled -> "● ĐÃ VÔ HIỆU HÓA"; account.isOnline -> "● ĐANG ONLINE"; else -> "● OFFLINE" },
                                color = if (account.isOnline) Color(0xFF35D6A3) else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                if (account.signInCount > 0L) "Đăng nhập ${account.signInCount} lần · ${formatAdminAccountTime(account.lastSignInAt)}"
                                else "Lần đăng nhập gần nhất · ${formatAdminAccountTime(account.lastSignInAt)}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 8.sp
                            )
                            Text(
                                when {
                                    account.activityDataConsentVersion >= AccountPrivacyConsent.CURRENT_VERSION && account.dataSharingConsent -> "Đã cho phép ghi nhận hoạt động/thiết bị (không ghi IP trên Spark)"
                                    account.isAdmin -> "Tài khoản quản lý · thiết bị hiện tại"
                                    else -> "Hồ sơ Google được đồng bộ"
                                },
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 8.sp
                            )
                        }
                        Text(
                            if (account.emailVerified) "✓" else "!",
                            color = if (account.emailVerified) Color(0xFF35D6A3) else Color(0xFFFFB74D),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                    Row(
                        modifier = Modifier.align(Alignment.End),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        TextButton(
                            onClick = {
                                expandedAccounts = if (expanded) expandedAccounts - account.uid
                                else expandedAccounts + account.uid
                            }
                        ) {
                            Text(if (expanded) "ẨN THÔNG TIN" else "XEM THÔNG TIN", fontSize = 9.sp)
                        }
                        if (!account.isAdmin) {
                            TextButton(
                                onClick = { pendingDelete = account },
                                enabled = !deleting
                            ) {
                                Text(if (account.accountDisabled) "KÍCH HOẠT LẠI" else "VÔ HIỆU HÓA", color = if (account.accountDisabled) MaterialTheme.colorScheme.primary else Color(0xFFFF5D91), fontSize = 9.sp)
                            }
                        }
                    }
                    if (!account.isAdmin) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = { pendingProfileRemoval = account },
                                enabled = !deleting
                            ) {
                                Text("XÓA HỒ SƠ FIREBASE", color = Color(0xFFFF5D91), fontSize = 9.sp)
                            }
                        }
                    }
                    if (expanded) {
                        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            ProfileDataRow("Loại tài khoản", if (account.isAdmin) "QUẢN LÝ / CHỦ SỞ HỮU" else "Người dùng")
                            ProfileDataRow("Trạng thái truy cập", if (account.accountDisabled) "ĐÃ VÔ HIỆU HÓA" else "ĐANG HOẠT ĐỘNG")
                            ProfileDataRow("Tên người dùng đặt", account.preferredName)
                            ProfileDataRow("Tên Google", account.displayName)
                            ProfileDataRow("Tên", account.givenName)
                            ProfileDataRow("Họ", account.familyName)
                            ProfileDataRow(
                                "Đồng bộ hồ sơ",
                                if (account.isAdmin && account.activityDataConsentVersion < AccountPrivacyConsent.CURRENT_VERSION) "Tài khoản quản lý đang dùng"
                                else "Được đồng bộ khi đăng nhập Google"
                            )
                            ProfileDataRow(
                                "Quyền theo dõi hoạt động",
                                if (account.activityDataConsentVersion >= AccountPrivacyConsent.CURRENT_VERSION && account.dataSharingConsent) "Đã cho phép thiết bị và hoạt động" else "Chưa cho phép thiết bị và hoạt động"
                            )
                            ProfileDataRow(
                                "Đồng ý phân tích mức sử dụng",
                                if (account.activityDataConsentVersion >= AccountPrivacyConsent.CURRENT_VERSION && account.analyticsConsent) "Đã cho phép" else "Chưa cho phép"
                            )
                            ProfileDataRow(
                                "Đồng ý báo lỗi và hiệu năng",
                                if (account.activityDataConsentVersion >= AccountPrivacyConsent.CURRENT_VERSION && account.diagnosticsConsent) "Đã cho phép" else "Chưa cho phép"
                            )
                            ProfileDataRow(
                                "Thời điểm lưu lựa chọn dữ liệu",
                                if (account.activityConsentAt > 0L) formatAdminAccountTime(account.activityConsentAt) else "Chưa lưu lựa chọn"
                            )
                            ProfileDataRow("Ảnh đại diện", account.photoUrl)
                            ProfileDataRow("Email xác minh", if (account.emailVerified) "Đã xác minh" else "Chưa xác minh")
                            ProfileDataRow("Nhà cung cấp", account.providerId)
                            ProfileDataRow(
                                "Số lần đăng nhập Google",
                                if (account.signInCount > 0L) account.signInCount.toString() else "Chưa có số liệu thống kê"
                            )
                            ProfileDataRow("Đăng nhập gần nhất", formatAdminAccountTime(account.lastSignInAt))
                            ProfileDataRow(
                                "Mở COC-LIO lần đầu",
                                if (account.firstOpenedAt > 0L) formatAdminAccountTime(account.firstOpenedAt)
                                else if (account.activityDataConsentVersion >= AccountPrivacyConsent.CURRENT_VERSION && account.dataSharingConsent) "Chưa ghi nhận lần mở"
                                else "Chưa có dữ liệu lịch sử theo dõi"
                            )
                            ProfileDataRow(
                                "Lần mở COC-LIO gần nhất",
                                if (account.lastOpenedAt > 0L) formatAdminAccountTime(account.lastOpenedAt)
                                else if (account.activityDataConsentVersion >= AccountPrivacyConsent.CURRENT_VERSION && account.dataSharingConsent) "Chưa ghi nhận lần mở"
                                else "Chưa có dữ liệu lịch sử theo dõi"
                            )
                            ProfileDataRow(
                                "Hoạt động gần nhất",
                                if (account.lastActiveAt > 0L) formatAdminAccountTime(account.lastActiveAt)
                                else "Chưa có dữ liệu lịch sử theo dõi"
                            )
                            ProfileDataRow(
                                "Lần chuyển ứng dụng xuống nền",
                                if (account.lastBackgroundAt > 0L) formatAdminAccountTime(account.lastBackgroundAt)
                                else "Chưa ghi nhận"
                            )
                            ProfileDataRow(
                                "Sự kiện hoạt động gần nhất",
                                when (account.lastActivityEvent) {
                                    "open" -> "Mở ứng dụng"
                                    "heartbeat" -> "Đang sử dụng ứng dụng"
                                    "background" -> "Chuyển ứng dụng xuống nền"
                                    else -> "Chưa ghi nhận"
                                }
                            )
                            ProfileDataRow(
                                "Thời điểm cập nhật sự kiện",
                                formatAdminAccountTime(account.activityUpdatedAt)
                            )
                            ProfileDataRow(
                                "Bắt đầu phiên mở gần nhất",
                                formatAdminAccountTime(account.sessionStartedAt)
                            )
                            ProfileDataRow(
                                "Số phiên mở COC-LIO",
                                if (account.activityDataConsentVersion >= AccountPrivacyConsent.CURRENT_VERSION && account.dataSharingConsent) account.appOpenCount.toString()
                                else "Chưa có dữ liệu do chưa bật theo dõi"
                            )
                            ProfileDataRow(
                                "Thời lượng phiên gần nhất",
                                if (account.lastSessionDurationSeconds > 0L) formatDuration(account.lastSessionDurationSeconds) else "Chưa ghi nhận"
                            )
                            ProfileDataRow(
                                "Tổng thời gian hoạt động",
                                if (account.totalForegroundSeconds > 0L) formatDuration(account.totalForegroundSeconds) else "Chưa ghi nhận"
                            )
                            ProfileDataRow(
                                "Thiết bị",
                                listOf(account.deviceManufacturer, account.deviceModel).filter { it.isNotBlank() }.joinToString(" ").ifBlank { "Chưa ghi nhận" }
                            )
                            ProfileDataRow("Thương hiệu / dòng / mã thiết bị", listOf(account.deviceBrand, account.deviceProduct, account.deviceCodeName).filter { it.isNotBlank() }.joinToString(" / ").ifBlank { "Chưa ghi nhận" })
                            ProfileDataRow("Kiến trúc CPU", account.supportedAbis.ifBlank { "Chưa ghi nhận" })
                            ProfileDataRow("Màn hình", if (account.screenWidthPx > 0L && account.screenHeightPx > 0L) "${account.screenWidthPx} × ${account.screenHeightPx} px · ${account.densityDpi} dpi" else "Chưa ghi nhận")
                            ProfileDataRow("Ngôn ngữ / múi giờ", listOf(account.localeTag, account.timeZoneId).filter { it.isNotBlank() }.joinToString(" / ").ifBlank { "Chưa ghi nhận" })
                            ProfileDataRow("Loại kết nối", when (account.networkType) {
                                "wifi" -> "Wi-Fi"
                                "cellular" -> "Di động"
                                "ethernet" -> "Ethernet"
                                "bluetooth" -> "Bluetooth"
                                "offline" -> "Ngoại tuyến"
                                "other" -> "Khác"
                                else -> "Chưa ghi nhận"
                            })
                            ProfileDataRow("Đồng bộ JSON Clash of Clans", if (account.activityDataConsentVersion >= AccountPrivacyConsent.CURRENT_VERSION && account.villageDataConsent) "Đã cho phép" else "Chưa cho phép")
                            ProfileDataRow("Mã người chơi gần nhất", account.lastVillageTag.ifBlank { "Chưa ghi nhận" })
                            ProfileDataRow("Tên làng gần nhất", account.lastVillageName.ifBlank { "Chưa ghi nhận" })
                            ProfileDataRow("Nhà chính", if (account.lastVillageTownHallLevel > 0L) account.lastVillageTownHallLevel.toString() else "Chưa ghi nhận")
                            ProfileDataRow("Nhà thợ", if (account.lastVillageBuilderHallLevel > 0L) account.lastVillageBuilderHallLevel.toString() else "Chưa ghi nhận")
                            ProfileDataRow("Số lần đồng bộ JSON", if (account.villageImportCount > 0L) account.villageImportCount.toString() else "Chưa ghi nhận")
                            ProfileDataRow("Thời điểm JSON mới nhất", if (account.lastVillageTimestamp > 0L) formatAdminAccountTime(account.lastVillageTimestamp * 1000L) else "Chưa ghi nhận")
                            ProfileDataRow("Lần đồng bộ JSON gần nhất", if (account.lastVillageSyncedAt > 0L) formatAdminAccountTime(account.lastVillageSyncedAt) else "Chưa ghi nhận")
                            ProfileDataRow("Dung lượng JSON gần nhất", if (account.lastVillageRawJsonBytes > 0L) "${account.lastVillageRawJsonBytes} byte" else "Chưa ghi nhận")
                            ProfileDataRow("Android", account.androidVersion.ifBlank { "Chưa ghi nhận" })
                            ProfileDataRow("API Android", if (account.sdkInt > 0L) account.sdkInt.toString() else "Chưa ghi nhận")
                            ProfileDataRow("Phiên bản COC-LIO", account.appVersionName.ifBlank { "Chưa ghi nhận" })
                            ProfileDataRow("Mã phiên bản ứng dụng", if (account.appVersionCode > 0L) account.appVersionCode.toString() else "Chưa ghi nhận")
                            ProfileDataRow("Tạo hồ sơ", formatAdminAccountTime(account.createdAt))
                            ProfileDataRow("Cập nhật hồ sơ", formatAdminAccountTime(account.updatedAt))
                            ProfileDataRow("Mã tài khoản", account.uid)
                        }
                    }
                }
            }
        }
        if (!loading && errorMessage == null && accounts.isEmpty()) {
            Text("Chưa có tài khoản Google nào đăng nhập COC-LIO.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    accountEmail: String?,
    isAdmin: Boolean,
    accountData: GoogleAccountData?,
    preferredName: String?,
    lastUsedLabel: String?,
    knownProfiles: List<LocalProfileSummary>,
    themeMode: String,
    onThemeModeChanged: (String) -> Unit,
    textScale: Float,
    displaySizeLevel: Int,
    onDisplaySizeLevelChanged: (Int) -> Unit,
    notificationsEnabled: Boolean,
    bubbleNotificationsEnabled: Boolean,
    ledEnabled: Boolean,
    ledPreset: String,
    ledSpeed: Int,
    ledBrightness: Float,
    ledGlowEnabled: Boolean,
    onLedEnabledChanged: (Boolean) -> Unit,
    onLedPresetChanged: (String) -> Unit,
    onLedSpeedChanged: (Int) -> Unit,
    onLedBrightnessChanged: (Float) -> Unit,
    onLedGlowEnabledChanged: (Boolean) -> Unit,
    notificationStyle: String,
    onGoogleSignIn: () -> Unit,
    onGoogleSwitchAccount: () -> Unit,
    onGoogleSignOut: () -> Unit,
    onOpenGame: () -> Unit,
    onNotificationsChanged: (Boolean) -> Unit,
    onBubbleNotificationsChanged: (Boolean) -> Unit,
    onNotificationStyleChanged: (String) -> Unit,
    onTestNotification: () -> Unit,
    onTextScaleChanged: (Float) -> Unit
) {
    val context = LocalContext.current
    var showAdminAccounts by remember(accountEmail, isAdmin) { mutableStateOf(false) }
    var selectedSizeSetting by remember { mutableStateOf(0) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text("CÀI ĐẶT", color = MaterialTheme.colorScheme.primary, fontSize = 15.sp, fontWeight = FontWeight.Black)

        FeaturePanel {
            FeaturePanelTitle(Icons.Filled.AccountCircle, "TÀI KHOẢN GOOGLE")
            Spacer(Modifier.height(8.dp))
            if (accountEmail.isNullOrBlank()) {
                Text("Chưa đăng nhập", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                Text(
                    "Khi đăng nhập Google, hồ sơ cơ bản gồm mã tài khoản, email, tên và ảnh đại diện sẽ được đồng bộ để quản lý tài khoản. IP, thiết bị và lịch sử hoạt động không được tự động thu thập chỉ vì bạn đăng nhập Google.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
                Button(
                    onClick = onGoogleSignIn,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = Color(0xFF04101D))
                ) { Text("ĐĂNG NHẬP GOOGLE", fontWeight = FontWeight.Black) }
                if (isAdmin && knownProfiles.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text("CÁC HỒ SƠ ĐÃ ĐĂNG NHẬP TRÊN MÁY NÀY", color = MaterialTheme.colorScheme.primary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    knownProfiles.forEach { profile ->
                        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                            Text(profile.preferredName?.takeIf { it.isNotBlank() } ?: profile.displayName ?: profile.email,
                                color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(profile.email, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
                            Text(
                                if (profile.lastUsedAt > 0L) "Mở lần cuối: " + java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(java.time.ZoneId.systemDefault()).format(java.time.Instant.ofEpochMilli(profile.lastUsedAt))
                                else "Chưa ghi nhận lần mở gần nhất",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 9.sp
                            )
                        }
                    }
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (!accountData?.profilePictureUri.isNullOrBlank()) {
                        AsyncImage(
                            model = accountData?.profilePictureUri,
                            contentDescription = AppText.translate(context, "Ảnh hồ sơ Google"),
                            modifier = Modifier.size(52.dp).clip(CircleShape)
                        )
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            if (isAdmin) preferredName?.takeIf { it.isNotBlank() } ?: accountData?.displayName ?: accountEmail
                            else accountData?.displayName ?: accountEmail,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                        Text(accountEmail, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                        if (isAdmin) {
                            Text("CHỦ SỞ HỮU / QUẢN LÝ", color = MaterialTheme.colorScheme.primary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                if (isAdmin) {
                    Button(
                        onClick = { showAdminAccounts = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color(0xFF04101D)
                        )
                    ) {
                        Text("MỞ DANH SÁCH QUẢN LÝ NGƯỜI DÙNG", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Button(
                        onClick = onGoogleSwitchAccount,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = Color(0xFF04101D))
                    ) { Text("ĐỔI TÀI KHOẢN", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                    Button(
                        onClick = onGoogleSignOut,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurface)
                    ) { Text("ĐĂNG XUẤT", fontSize = 10.sp) }
                }
            }
        }

        if (isAdmin && showAdminAccounts) {
            AdminAccountsPanel(onClose = { showAdminAccounts = false })
        }

        FeaturePanel {
            FeaturePanelTitle(Icons.Filled.Palette, "GIAO DIỆN ỨNG DỤNG")
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(
                    "system" to "Theo thiết bị",
                    "light" to "Sáng",
                    "dark" to "Tối"
                ).forEach { (mode, label) ->
                    FilterChip(
                        selected = themeMode == mode,
                        onClick = { onThemeModeChanged(mode) },
                        label = { Text(label, fontSize = 10.sp) }
                    )
                }
            }
        }

        val currentFontLevel = AppPreferences.textScaleLevel(textScale)
        val activeSizeLevel = if (selectedSizeSetting == 0) displaySizeLevel else currentFontLevel
        val activeSizePercent = if (selectedSizeSetting == 0) {
            (AppPreferences.displaySizeScale(displaySizeLevel) * 100f).roundToInt()
        } else {
            (AppPreferences.textScaleForLevel(currentFontLevel) * 100f).roundToInt()
        }
        FeaturePanel {
            FeaturePanelTitle(Icons.Filled.TextFields, "KÍCH THƯỚC GIAO DIỆN & CỠ CHỮ")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedSizeSetting == 0,
                    onClick = { selectedSizeSetting = 0 },
                    modifier = Modifier.weight(1f).heightIn(min = 42.dp),
                    label = {
                        Text(
                            "Kích thước giao diện",
                            fontSize = 9.sp,
                            lineHeight = 11.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            maxLines = 2
                        )
                    }
                )
                FilterChip(
                    selected = selectedSizeSetting == 1,
                    onClick = { selectedSizeSetting = 1 },
                    modifier = Modifier.weight(1f).heightIn(min = 42.dp),
                    label = {
                        Text(
                            "Cỡ chữ",
                            fontSize = 9.sp,
                            lineHeight = 11.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            maxLines = 2
                        )
                    }
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (selectedSizeSetting == 0) "KÍCH THƯỚC GIAO DIỆN" else "CỠ CHỮ TOÀN ỨNG DỤNG",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "MỨC $activeSizeLevel/10 · $activeSizePercent%",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                (1..10).forEach { level ->
                    val selected = activeSizeLevel == level
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(
                                if (selected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .clickable {
                                if (selectedSizeSetting == 0) {
                                    onDisplaySizeLevelChanged(level)
                                } else {
                                    onTextScaleChanged(AppPreferences.textScaleForLevel(level))
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            level.toString(),
                            modifier = Modifier.fillMaxWidth(),
                            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (selectedSizeSetting == 0) "RẤT GỌN" else "NHỎ",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 9.sp
                )
                Spacer(Modifier.weight(1f))
                Text(
                    if (selectedSizeSetting == 0) "LỚN" else "TO",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 9.sp
                )
            }
            Text(
                if (selectedSizeSetting == 0)
                    "Điều chỉnh kích thước tổng thể của giao diện, nút và khoảng cách. Mức 5 (100%) là mặc định."
                else
                    "Điều chỉnh cỡ chữ trong toàn ứng dụng. Mức 5 (100%) là mặc định.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp,
                lineHeight = 14.sp
            )
        }
        val ledSliderColors = androidx.compose.material3.SliderDefaults.colors(
            activeTrackColor = Color(0xFF36F4FF),
            inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
            thumbColor = Color.Transparent,
            activeTickColor = Color.Transparent,
            inactiveTickColor = Color.Transparent
        )
        FeaturePanel {
            FeaturePanelTitle(Icons.Filled.Lightbulb, "LED RGB TÙY CHỈNH")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Bật hiệu ứng chuyển động", color = MaterialTheme.colorScheme.onSurface, fontSize = 10.sp, modifier = Modifier.weight(1f))
                Switch(checked = ledEnabled, onCheckedChange = onLedEnabledChanged)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(AppText.translate(context, "Ánh sáng lan tỏa"), color = MaterialTheme.colorScheme.onSurface, fontSize = 10.sp)
                    Text(AppText.translate(context, "Tạo vùng sáng mờ quanh viền LED"), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.sp)
                }
                Switch(checked = ledGlowEnabled, onCheckedChange = onLedGlowEnabledChanged)
            }
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    "rainbow" to "RGB",
                    "ice" to "Băng",
                    "violet" to "Tím",
                    "fire" to "Lửa",
                    "mint" to "Ngọc"
                ).forEach { (key, label) ->
                    FilterChip(
                        selected = ledPreset == key,
                        onClick = { onLedPresetChanged(key) },
                        label = { Text(label, fontSize = 10.sp) }
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Tốc độ chuyển màu", color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp, modifier = Modifier.weight(1f))
                Text("${ledSpeed.coerceIn(0, 100)}%", color = MaterialTheme.colorScheme.primary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            Slider(
                value = ledSpeed.coerceIn(0, 100).toFloat(),
                onValueChange = { onLedSpeedChanged(it.roundToInt().coerceIn(0, 100)) },
                valueRange = 0f..100f,
                colors = ledSliderColors,
                thumb = {
                    Box(
                        Modifier
                            .width(3.dp)
                            .height(20.dp)
                            .background(Color.White, RoundedCornerShape(2.dp))
                    )
                },
                track = { sliderState ->
                    androidx.compose.material3.SliderDefaults.Track(
                        sliderState = sliderState,
                        colors = ledSliderColors,
                        modifier = Modifier.height(12.dp),
                        drawStopIndicator = null,
                        thumbTrackGapSize = 0.dp
                    )
                }
            )
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("ĐỨNG YÊN", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
                Spacer(Modifier.weight(1f))
                Text("CỰC NHANH", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Độ sáng", color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp, modifier = Modifier.weight(1f))
                Text("${(ledBrightness * 100f).roundToInt().coerceIn(0, 100)}%", color = MaterialTheme.colorScheme.primary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            Slider(
                value = (ledBrightness * 100f).roundToInt().coerceIn(0, 100).toFloat(),
                onValueChange = { onLedBrightnessChanged(it.roundToInt().coerceIn(0, 100) / 100f) },
                valueRange = 0f..100f,
                colors = ledSliderColors,
                thumb = {
                    Box(
                        Modifier
                            .width(3.dp)
                            .height(20.dp)
                            .background(Color.White, RoundedCornerShape(2.dp))
                    )
                },
                track = { sliderState ->
                    androidx.compose.material3.SliderDefaults.Track(
                        sliderState = sliderState,
                        colors = ledSliderColors,
                        modifier = Modifier.height(12.dp),
                        drawStopIndicator = null,
                        thumbTrackGapSize = 0.dp
                    )
                }
            )
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Mờ", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
                Spacer(Modifier.weight(1f))
                Text("Sáng", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
            }
        }

        FeaturePanel {
            FeaturePanelTitle(Icons.Filled.Notifications, "THEO DÕI TIẾN TRÌNH NÂNG CẤP VÀ THÔNG BÁO")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("THÔNG BÁO NÂNG CẤP", color = MaterialTheme.colorScheme.onSurface, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text("Nhắc khi bộ đếm trong JSON đã đếm hết.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                }
                Switch(checked = notificationsEnabled, onCheckedChange = onNotificationsChanged)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("BONG BÓNG THÔNG BÁO", color = MaterialTheme.colorScheme.onSurface, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text("Hiện lời nhắc nâng cấp thành cửa sổ nổi, kể cả khi tắt thông báo thường.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, lineHeight = 14.sp)
                }
                Switch(
                    checked = bubbleNotificationsEnabled,
                    onCheckedChange = onBubbleNotificationsChanged
                )
            }
            Text(
                "Khi bật, COC-LIO cần quyền Hiển thị trên ứng dụng khác. Android 13 trở lên cần thêm quyền thông báo.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp,
                lineHeight = 14.sp
            )
            Text("Kiểu nhắc nâng cấp", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                FilterChip(
                    selected = notificationStyle == "message",
                    onClick = { onNotificationStyleChanged("message") },
                    label = { Text("Tin nhắn") }
                )
                FilterChip(
                    selected = notificationStyle == "alarm",
                    onClick = { onNotificationStyleChanged("alarm") },
                    label = { Text("Báo thức") }
                )
            }
            Button(
                onClick = onTestNotification,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = FeatureCyan,
                    contentColor = Color(0xFF04101D)
                )
            ) {
                Text("THỬ NHẮC NÂNG CẤP", fontWeight = FontWeight.Black)
            }
            Text(
                "Gửi một thông báo thử cho chức năng nhắc nâng cấp. Không tự bật lịch nhắc.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp,
                lineHeight = 14.sp
            )
            Text(
                "Thông báo nhắc tự ẩn sau 25 giây. Thời gian banner nổi và âm thanh thực tế còn phụ thuộc Android, kênh thông báo và thiết bị.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                lineHeight = 16.sp
            )
        }

        FeaturePanel {
            FeaturePanelTitle(Icons.Filled.Widgets, "TIỆN ÍCH MÀN HÌNH CHÍNH")
            Text(
                "Nhấn giữ màn hình chính → chọn Tiện ích → COC-LIO → Theo dõi nâng cấp. Mỗi tiện ích gắn với tài khoản đang đăng nhập và một làng đã chọn. Muốn thêm tiện ích cho tài khoản khác, hãy đổi tài khoản trong COC-LIO rồi thêm một tiện ích mới.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                lineHeight = 18.sp
            )
            Text(
                "Tiện ích dạng dọc 2 × 4 ô gọn hơn, có viền RGB chuyển sắc liền mạch và danh sách nâng cấp có thể cuộn. Mỗi mục hiển thị ảnh công trình, đồng hồ đếm ngược ở giữa thanh và phần trăm thời gian còn lại.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp,
                lineHeight = 15.sp
            )
        }

        FeaturePanel {
            FeaturePanelTitle(Icons.Filled.Code, "CÁCH LẤY JSON")
            Text(
                "1. Nhấn MỞ CLASH OF CLANS trong COC-LIO. Lần đầu, bật quyền Hiển thị trên ứng dụng khác để dùng cửa sổ nổi.\n2. Khi game mở, vào làng/tài khoản cần lưu. Nhấn Cài đặt → Cài đặt khác → Xuất dữ liệu làng dưới dạng JSON → Sao chép.\n3. Cửa sổ COC-LIO vẫn nằm trên game. Dán JSON vào đó, nhập tên làng hoặc tên tài khoản để dễ phân biệt rồi nhấn PHÂN TÍCH VÀ LƯU JSON.\n4. Lặp lại với các tài khoản game khác. COC-LIO nhận diện từng làng bằng Player Tag; tối đa 50 bản JSON cho mỗi tài khoản COC-LIO.\nKéo thanh tiêu đề để di chuyển, kéo góc ↘ để thu phóng, nhấn − để thu nhỏ thành nút nổi hoặc × để đóng.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                lineHeight = 18.sp
            )
            Button(
                onClick = onOpenGame,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = FeatureCyan, contentColor = Color(0xFF04101D))
            ) { Text("MỞ CLASH OF CLANS", fontWeight = FontWeight.Black) }
            Button(
                onClick = {
                    runCatching {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://supercell.com/en/games/clashofclans/")))
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            ) { Text("TRANG CHÍNH THỨC CLASH OF CLANS", fontWeight = FontWeight.Bold) }
        }

        FeaturePanel {
            FeaturePanelTitle(Icons.Filled.Phone, "LIÊN HỆ NHÀ PHÁT TRIỂN")
            Text("Mở Zalo để liên hệ nhà phát triển COC-LIO.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
            Button(
                onClick = {
                    runCatching {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(ZALO_CONTACT_URL)))
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = FeatureCyan, contentColor = Color(0xFF04101D))
            ) { Text("MỞ ZALO LIÊN HỆ", fontWeight = FontWeight.Black) }
        }

        FeaturePanel {
            FeaturePanelTitle(Icons.Filled.Info, "THÔNG TIN ỨNG DỤNG")
            Text(
                "COC-LIO là ứng dụng cộng đồng không chính thức, không được Supercell chứng thực. Hình ảnh game thuộc quyền sở hữu của Supercell.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp,
                lineHeight = 15.sp
            )
            Text(
                "Đây là nội dung không chính thức và không được Supercell bảo trợ. Xem chính sách nội dung người hâm mộ tại www.supercell.com/fan-content-policy.",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 9.sp,
                lineHeight = 14.sp
            )
        }
    }
}

@Composable
private fun FeaturePanelTitle(icon: ImageVector, title: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        ComposeIcon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(17.dp)
        )
        Text(title, color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun FeaturePanel(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    val led = LocalLedTheme.current
    val palette = led.palette.map { it.copy(alpha = led.brightness) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .drawWithCache {
                val stroke = 2.6.dp.toPx()
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
                    if (led.enabled && palette.size >= 2 && led.brightness > 0f) {
                        val progress = led.progress.value.rem(1f)
                        val colors = movingLedColors(palette, progress)
                        drawLedStroke(path, Brush.sweepGradient(colors), stroke, led.glowEnabled)
                    }
                }
            }
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        content = content
    )
}

