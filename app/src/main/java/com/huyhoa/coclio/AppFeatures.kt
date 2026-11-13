package com.huyhoa.coclio

import android.app.Activity
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.graphics.drawable.IconCompat
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
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
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.CoroutineScope
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

    fun communityNotificationsEnabled(context: Context, profile: String): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(key(profile, "community_notifications"), false)

    fun setCommunityNotificationsEnabled(context: Context, profile: String, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(key(profile, "community_notifications"), enabled).apply()
    }

    fun updateNotificationsEnabled(context: Context, profile: String): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(key(profile, "update_notifications"), false)

    fun setUpdateNotificationsEnabled(context: Context, profile: String, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(key(profile, "update_notifications"), enabled).apply()
    }

    fun ensureNotificationPreferences(context: Context, profile: String, defaultEnabled: Boolean) {
        val preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val editor = preferences.edit()
        val notificationKey = key(profile, "notifications")
        val communityKey = key(profile, "community_notifications")
        val updateKey = key(profile, "update_notifications")
        if (!preferences.contains(notificationKey)) editor.putBoolean(notificationKey, defaultEnabled)
        if (!preferences.contains(communityKey)) editor.putBoolean(communityKey, defaultEnabled)
        if (!preferences.contains(updateKey)) editor.putBoolean(updateKey, defaultEnabled)
        editor.apply()
    }

    fun notificationStyle(context: Context, profile: String): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(key(profile, "notification_style"), "alarm") ?: "alarm"

    fun setNotificationStyle(context: Context, profile: String, style: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(key(profile, "notification_style"), style).apply()
    }

    fun alarmDurationSeconds(context: Context, profile: String): Int {
        val saved = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getInt(key(profile, "alarm_duration_seconds"), 10)
        return if (saved in listOf(5, 10, 15, 20, 25)) saved else 10
    }

    fun setAlarmDurationSeconds(context: Context, profile: String, seconds: Int) {
        val selected = if (seconds in listOf(5, 10, 15, 20, 25)) seconds else 10
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putInt(key(profile, "alarm_duration_seconds"), selected).apply()
    }

    fun textScale(context: Context): Float {
        val preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (preferences.getInt("text_scale_version", 0) < 1) {
            preferences.edit()
                .putFloat("text_scale", 1f)
                .putInt("text_scale_version", 1)
                .apply()
        }
        val savedScale = preferences.getFloat("text_scale", 1f).coerceIn(0.8f, 1.2f)
        val normalizedScale = textScaleForLevel(textScaleLevel(savedScale))
        if (savedScale != normalizedScale) {
            preferences.edit().putFloat("text_scale", normalizedScale).apply()
        }
        return normalizedScale
    }

    fun textScaleLevel(scale: Float): Int = when {
        scale < 0.85f -> 1
        scale < 0.95f -> 2
        scale < 1.05f -> 3
        scale < 1.15f -> 4
        else -> 5
    }

    fun textScaleForLevel(level: Int): Float = when (level.coerceIn(1, 5)) {
        1 -> 0.8f
        2 -> 0.9f
        3 -> 1f
        4 -> 1.1f
        else -> 1.2f
    }

    fun themeMode(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString("theme_mode", "system") ?: "system"

    fun setThemeMode(context: Context, mode: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString("theme_mode", mode).apply()
    }

    fun displaySizeLevel(context: Context): Int =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getInt("display_size_level", 3).coerceIn(1, 5)

    fun setDisplaySizeLevel(context: Context, level: Int) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putInt("display_size_level", level.coerceIn(1, 5)).apply()
    }

    fun displaySizeScale(context: Context): Float = displaySizeScale(displaySizeLevel(context))

    fun displaySizeScale(level: Int): Float = when (level.coerceIn(1, 5)) {
        1 -> 0.80f
        2 -> 0.90f
        3 -> 1.00f
        4 -> 1.10f
        else -> 1.20f
    }

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
            val lastActiveAt = document.getTimestamp("lastActiveAt")?.toDate()?.time ?: 0L
            val lastBackgroundAt = document.getTimestamp("lastBackgroundAt")?.toDate()?.time ?: 0L
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
                isOnline = lastActiveAt > lastBackgroundAt &&
                    lastActiveAt > 0L &&
                    System.currentTimeMillis() - lastActiveAt <= 660_000L,
                lastActiveAt = lastActiveAt,
                firstOpenedAt = document.getTimestamp("firstOpenedAt")?.toDate()?.time ?: 0L,
                lastOpenedAt = document.getTimestamp("lastOpenedAt")?.toDate()?.time ?: 0L,
                lastBackgroundAt = lastBackgroundAt,
                sessionStartedAt = document.getTimestamp("sessionStartedAt")?.toDate()?.time ?: 0L,
                activityUpdatedAt = document.getTimestamp("activityUpdatedAt")?.toDate()?.time ?: 0L,
                lastActivityEvent = document.getString("lastActivityEvent").orEmpty(),
                appOpenCount = document.getLong("appOpenCount") ?: 0L,
                lastSessionDurationSeconds = document.getLong("lastSessionDurationSeconds") ?: 0L,
                totalForegroundSeconds = document.getLong("totalForegroundSeconds") ?: 0L,
                deviceManufacturer = document.getString("deviceManufacturer").orEmpty(),
                deviceModel = document.getString("deviceModel").orEmpty(),
                deviceBrand = document.getString("deviceBrand").orEmpty(),
                deviceProduct = document.getString("deviceProduct").orEmpty(),
                deviceCodeName = document.getString("deviceCodeName").orEmpty(),
                supportedAbis = document.getString("supportedAbis").orEmpty(),
                localeTag = document.getString("localeTag").orEmpty(),
                timeZoneId = document.getString("timeZoneId").orEmpty(),
                screenWidthPx = document.getLong("screenWidthPx") ?: 0L,
                screenHeightPx = document.getLong("screenHeightPx") ?: 0L,
                densityDpi = document.getLong("densityDpi") ?: 0L,
                networkType = document.getString("networkType").orEmpty(),
                androidVersion = document.getString("androidVersion").orEmpty(),
                sdkInt = document.getLong("sdkInt") ?: 0L,
                appVersionName = document.getString("appVersionName").orEmpty(),
                appVersionCode = document.getLong("appVersionCode") ?: 0L,
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
            val packageInfo = runCatching {
                context.packageManager.getPackageInfo(context.packageName, 0)
            }.getOrNull()
            val appVersionCode = if (Build.VERSION.SDK_INT >= 28) {
                packageInfo?.longVersionCode ?: 0L
            } else {
                @Suppress("DEPRECATION")
                packageInfo?.versionCode?.toLong() ?: 0L
            }
            val now = System.currentTimeMillis()
            val ownerIndex = accounts.indexOfFirst { it.uid == owner.uid }
            if (ownerIndex >= 0 && accounts[ownerIndex].activityDataConsentVersion < AccountPrivacyConsent.CURRENT_VERSION) {
                accounts[ownerIndex] = accounts[ownerIndex].copy(
                    isOnline = true,
                    lastActiveAt = now,
                    deviceManufacturer = Build.MANUFACTURER.orEmpty(),
                    deviceModel = Build.MODEL.orEmpty(),
                    androidVersion = Build.VERSION.RELEASE.orEmpty(),
                    sdkInt = Build.VERSION.SDK_INT.toLong(),
                    appVersionName = packageInfo?.versionName.orEmpty(),
                    appVersionCode = appVersionCode
                )
            } else if (ownerIndex < 0) {
                accounts.add(
                    AdminGoogleAccount(
                        uid = owner.uid,
                        email = owner.email.orEmpty(),
                        preferredName = "Tài khoản quản lý",
                        displayName = owner.displayName.orEmpty(),
                        givenName = "",
                        familyName = "",
                        photoUrl = owner.photoUrl?.toString().orEmpty(),
                        emailVerified = owner.isEmailVerified,
                        providerId = owner.providerData.firstOrNull { it.providerId == "google.com" }?.providerId.orEmpty(),
                        signInCount = 0L,
                        createdAt = owner.metadata?.creationTimestamp ?: 0L,
                        lastSignInAt = owner.metadata?.lastSignInTimestamp ?: 0L,
                        updatedAt = owner.metadata?.lastSignInTimestamp ?: 0L,
                        isAdmin = true,
                        isOnline = true,
                        lastActiveAt = now,
                        lastOpenedAt = now,
                        deviceManufacturer = Build.MANUFACTURER.orEmpty(),
                        deviceModel = Build.MODEL.orEmpty(),
                        androidVersion = Build.VERSION.RELEASE.orEmpty(),
                        sdkInt = Build.VERSION.SDK_INT.toLong(),
                        appVersionName = packageInfo?.versionName.orEmpty(),
                        appVersionCode = appVersionCode
                    )
                )
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
}

data class AccountPrivacyChoice(
    val securityTelemetry: Boolean = false,
    val analytics: Boolean = false,
    val diagnostics: Boolean = false,
    val villageDataSync: Boolean = false,
    val version: Long = 0L
)

object AccountPrivacyConsent {
    const val CURRENT_VERSION = 6L
    const val REQUIRED_ACCOUNT_DATA_VERSION = 1L

    suspend fun hasRequiredAccountDataConsent(): Boolean {
        val user = FirebaseAuth.getInstance().currentUser ?: return false
        val profile = FirebaseFirestore.getInstance()
            .collection("users")
            .document(user.uid)
            .get()
            .awaitFirebaseResult()
        return profile.getLong("requiredAccountDataConsentVersion") == REQUIRED_ACCOUNT_DATA_VERSION
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
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var heartbeatJob: Job? = null
    @Volatile private var sessionStartedAt = 0L

    fun start(context: Context) {
        if (heartbeatJob?.isActive == true) return
        heartbeatJob = scope.launch {
            val user = FirebaseAuth.getInstance().currentUser ?: return@launch
            if (!hasConsent(user.uid)) return@launch
            sessionStartedAt = System.currentTimeMillis()
            record(context.applicationContext, "open", 0L)
            while (isActive) {
                delay(600_000L)
                if (!hasConsent(user.uid)) break
                record(context.applicationContext, "heartbeat", 0L)
            }
        }
    }

    fun stop(context: Context) {
        scope.launch { stopAndWait(context) }
    }

    suspend fun stopAndWait(context: Context) {
        heartbeatJob?.cancel()
        heartbeatJob = null
        val user = FirebaseAuth.getInstance().currentUser
        val duration = if (sessionStartedAt > 0L) {
            ((System.currentTimeMillis() - sessionStartedAt) / 1000L).coerceAtLeast(0L)
        } else {
            0L
        }
        sessionStartedAt = 0L
        val uid = user?.uid ?: return
        if (hasConsent(uid)) {
            record(context.applicationContext, "background", duration)
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
            "Player Tag trong JSON không hợp lệ."
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
    suspend fun signIn(context: Context): GoogleAccountData {
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
        val firebaseCredential = GoogleAuthProvider.getCredential(google.idToken, null)
        val authResult = FirebaseAuth.getInstance()
            .signInWithCredential(firebaseCredential)
            .awaitFirebaseResult()
        val firebaseUser = authResult.user
            ?: throw IllegalStateException("Không nhận được tài khoản Google sau khi xác thực.")
        firebaseUser.reload().awaitFirebaseResult()
        firebaseUser.getIdToken(true).awaitFirebaseResult()

        val accountData = GoogleAccountData(
            email = email.lowercase(),
            displayName = google.displayName?.trim()?.takeIf { it.isNotBlank() },
            givenName = google.givenName?.trim()?.takeIf { it.isNotBlank() },
            familyName = google.familyName?.trim()?.takeIf { it.isNotBlank() },
            profilePictureUri = google.profilePictureUri?.toString()
        )

        return accountData
    }

    suspend fun shareProfile(google: GoogleAccountData) {
        val user = FirebaseAuth.getInstance().currentUser
            ?: throw IllegalStateException("Bạn cần đăng nhập Google trước.")
        if (!user.email.equals(google.email, ignoreCase = true)) {
            throw IllegalStateException("Tài khoản Google hiện tại không khớp với hồ sơ vừa chọn.")
        }
        try {
            syncFirebaseUserProfile(user, google)
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
        context.startActivity(launch)
    } else {
        val store = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://play.google.com/store/apps/details?id=com.supercell.clashofclans")
        )
        context.startActivity(store)
    }
}

private fun alarmVibrationPattern(durationSeconds: Int): LongArray {
    val seconds = if (durationSeconds in listOf(5, 10, 15, 20, 25)) durationSeconds else 10
    return LongArray(seconds * 2 + 1) { index ->
        when {
            index == 0 -> 0L
            index % 2 == 1 -> 650L
            else -> 350L
        }
    }
}

private fun ensureUpgradeNotificationChannel(
    context: Context,
    channelId: String,
    alarmStyle: Boolean,
    alarmDurationSeconds: Int
) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val manager = context.getSystemService(NotificationManager::class.java)
    if (manager.getNotificationChannel(channelId) != null) return
    val channel = NotificationChannel(
        channelId,
        if (alarmStyle) "Nhắc nâng cấp kiểu báo thức • ${alarmDurationSeconds}s" else "Tin nhắn nâng cấp",
        if (alarmStyle) NotificationManager.IMPORTANCE_HIGH else NotificationManager.IMPORTANCE_DEFAULT
    )
    channel.description = if (alarmStyle) {
        "Thông báo báo thức tự đóng sau ${alarmDurationSeconds} giây"
    } else {
        "Nhắc khi bộ đếm nâng cấp trong JSON đã hết thời gian"
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) channel.setAllowBubbles(true)
    channel.enableVibration(true)
    if (alarmStyle) {
        channel.vibrationPattern = alarmVibrationPattern(alarmDurationSeconds)
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
    alarmStyle: Boolean,
    alarmDurationSeconds: Int
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) return
    builder
        .setSound(
            if (alarmStyle) Settings.System.DEFAULT_ALARM_ALERT_URI
            else Settings.System.DEFAULT_NOTIFICATION_URI
        )
        .setVibrate(
            if (alarmStyle) alarmVibrationPattern(alarmDurationSeconds)
            else longArrayOf(0, 250)
        )
}

object UpgradeReminderScheduler {
    private fun profileTag(profile: String) =
        "coc_lio_upgrade_" + profile.hashCode().toUInt().toString(16)

    fun scheduleAll(context: Context, snapshots: List<VillageSnapshot>, profile: String, enabled: Boolean) {
        val manager = WorkManager.getInstance(context)
        val tag = profileTag(profile)
        manager.cancelAllWorkByTag(tag)

        val nowSeconds = System.currentTimeMillis() / 1000L
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

                if (!enabled) return@forEachIndexed

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
        if (!AppPreferences.notificationsEnabled(applicationContext, profile)) {
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
        val alarmDurationSeconds = AppPreferences.alarmDurationSeconds(applicationContext, profile)
        val profileCode = profile.hashCode().toUInt().toString(16)
        val channelId = "upgrade_" + profileCode +
            if (alarmStyle) "_alarm_${alarmDurationSeconds}s_v2" else "_message"
        ensureUpgradeNotificationChannel(applicationContext, channelId, alarmStyle, alarmDurationSeconds)

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
        val levelText = if (level >= 0) {
            if (gearUp) "Cấp $level • GEAR-UP" else "Cấp $level → ${level + 1}"
        } else {
            if (gearUp) "GEAR-UP" else "Chưa rõ cấp độ"
        }
        val baseText = if (base == "builder") "Làng đêm" else "Nhà chính"
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
        val notificationBuilder = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setCategory(if (alarmStyle) NotificationCompat.CATEGORY_ALARM else NotificationCompat.CATEGORY_MESSAGE)
            .setContentTitle("Đến giờ hoàn thành • $item")
            .setContentText("$villageName • ID $dataId • $levelText")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "Làng: $villageName ($tag)\nCăn cứ: $baseText\nNhóm: $section\nCông trình: $item\nID: $dataId\nDự kiến: $levelText\nBộ đếm JSON đã hết giờ; hãy nhập JSON mới để xác nhận cấp độ trong game."
                )
            )
            .setContentIntent(pendingIntent)
            .setAutoCancel(!alarmStyle)
            .setOngoing(alarmStyle)
            .setOnlyAlertOnce(false)
            .setPriority(if (alarmStyle) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
        if (alarmStyle) notificationBuilder.setTimeoutAfter(alarmDurationSeconds * 1_000L)
        if (gameIntent != null) {
            val gamePendingIntent = PendingIntent.getActivity(
                applicationContext,
                notificationId xor 0x4C494F,
                gameIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            notificationBuilder.addAction(android.R.drawable.ic_media_play, "MỞ GAME", gamePendingIntent)
        }
        if (iconBitmap != null) notificationBuilder.setLargeIcon(iconBitmap)
        applyLegacyNotificationSettings(notificationBuilder, alarmStyle, alarmDurationSeconds)
        val notification = notificationBuilder.build()
        val posted = runCatching {
            NotificationManagerCompat.from(applicationContext)
                .notify(notificationId, notification)
        }.isSuccess
        if (posted && alarmStyle && Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            scheduleUpgradeNotificationDismissal(applicationContext, notificationId, alarmDurationSeconds)
        }
        Result.success()
    }
}


object UpgradeNotificationTester {
    fun show(
        context: Context,
        profile: String,
        style: String,
        alarmDurationSeconds: Int = AppPreferences.alarmDurationSeconds(context, profile)
    ): Boolean = runCatching {
        val alarmStyle = style == "alarm"
        val profileCode = profile.hashCode().toUInt().toString(16)
        val channelId = "upgrade_" + profileCode +
            if (alarmStyle) "_alarm_${alarmDurationSeconds}s_v2" else "_message"
        ensureUpgradeNotificationChannel(context, channelId, alarmStyle, alarmDurationSeconds)

        val openApp = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(
            context,
            ("coc_lio_test_" + profile).hashCode(),
            openApp,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val title = "COC-LIO • Thử thông báo"
        val message = if (alarmStyle) {
            "Đây là thông báo thử kiểu báo thức. Âm thanh và rung phụ thuộc cài đặt thông báo của Android."
        } else {
            "Đây là thông báo thử kiểu tin nhắn. Thông báo nâng cấp thật sẽ xuất hiện khi bộ đếm kết thúc."
        }
        val notificationBuilder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setCategory(if (alarmStyle) NotificationCompat.CATEGORY_ALARM else NotificationCompat.CATEGORY_MESSAGE)
            .setContentTitle(title)
            .setContentText(if (alarmStyle) "Đang thử kiểu báo thức" else "Đang thử kiểu tin nhắn")
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setContentIntent(pendingIntent)
            .setAutoCancel(!alarmStyle)
            .setOngoing(alarmStyle)
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
            notificationBuilder.addAction(android.R.drawable.ic_media_play, "MỞ GAME", gamePendingIntent)
        }
        applyLegacyNotificationSettings(notificationBuilder, alarmStyle, alarmDurationSeconds)
        if (alarmStyle) notificationBuilder.setTimeoutAfter(alarmDurationSeconds * 1_000L)
        val notification = notificationBuilder.build()
        NotificationManagerCompat.from(context).notify(testNotificationId, notification)
        if (alarmStyle && Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            scheduleUpgradeNotificationDismissal(context, testNotificationId, alarmDurationSeconds)
        }
        true
    }.getOrDefault(false)
}

private fun scheduleUpgradeNotificationDismissal(context: Context, notificationId: Int, durationSeconds: Int) {
    val data = Data.Builder()
        .putInt("notification_id", notificationId)
        .build()
    val request = OneTimeWorkRequestBuilder<UpgradeNotificationDismissWorker>()
        .setInitialDelay(durationSeconds.toLong(), TimeUnit.SECONDS)
        .setInputData(data)
        .build()
    WorkManager.getInstance(context).enqueueUniqueWork(
        "coc_lio_notification_dismiss_$notificationId",
        ExistingWorkPolicy.REPLACE,
        request
    )
}

class UpgradeNotificationDismissWorker(
    context: Context,
    parameters: WorkerParameters
) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        val notificationId = inputData.getInt("notification_id", Int.MIN_VALUE)
        if (notificationId != Int.MIN_VALUE) {
            NotificationManagerCompat.from(applicationContext).cancel(notificationId)
        }
        return Result.success()
    }
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
            entry.gearUp -> entry.level?.let { "CẤP $it • GEAR-UP" } ?: "GEAR-UP"
            entry.level != null -> "CẤP ${entry.level} → ${entry.level + 1}"
            else -> "ĐANG NÂNG"
        }
        views.setTextViewText(
            R.id.row_title,
            AppLanguageSettings.translate(
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
            if (remaining > 0L) android.graphics.Color.WHITE else android.graphics.Color.rgb(37, 211, 102)
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
            views.setTextViewText(R.id.row_timer, AppLanguageSettings.translate(context, "HOÀN THÀNH"))
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
            setTextViewText(R.id.row_title, AppLanguageSettings.translate(context, "Đang tải danh sách nâng cấp…"))
            setTextViewText(R.id.row_subtitle, "COC-LIO")
            setTextViewText(R.id.row_timer, AppLanguageSettings.translate(context, "ĐANG CẬP NHẬT"))
            setProgressBar(R.id.row_progress, 100, 0, false)
        }
    }

    override fun onDestroy() {
        entries = emptyList()
        decodedIcons.clear()
    }
}

object AppWidgetUpdater {
    fun ensureRefresh(context: Context) {
        if (Build.VERSION.SDK_INT >= 35) {
            val previewPrefs = context.getSharedPreferences("coc_lio_widgets", Context.MODE_PRIVATE)
            if (!previewPrefs.getBoolean("generated_preview_set", false)) {
                runCatching {
                    val previewSet = AppWidgetManager.getInstance(context).setWidgetPreview(
                        ComponentName(context, UpgradeWidgetProvider::class.java),
                        AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN,
                        RemoteViews(context.packageName, R.layout.widget_upgrade_preview)
                    )
                    if (previewSet) {
                        previewPrefs.edit().putBoolean("generated_preview_set", true).apply()
                    }
                }
            }
        }
        val request = PeriodicWorkRequestBuilder<WidgetRefreshWorker>(15, TimeUnit.MINUTES).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "coc_lio_widget_refresh",
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun updateAll(context: Context) {
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
        views.setTextViewText(R.id.widget_empty, AppLanguageSettings.translate(context, "Chưa có mục nào đang nâng cấp"))
        val widgetPadding = (5f * AppPreferences.displaySizeScale(context) * context.resources.displayMetrics.density).roundToInt()
        views.setViewPadding(R.id.widget_root, widgetPadding, widgetPadding, widgetPadding, widgetPadding)

        val builderTotal = snapshot?.homeBuilderTotal
        val buildersFree = if (snapshot == null || builderTotal == null) null else
            (builderTotal - snapshot.homeBuildersBusyAt(nowSeconds)).coerceAtLeast(0)
        views.setTextViewText(
            R.id.widget_village,
            if (snapshot == null) "COC-LIO • " + AppLanguageSettings.translate(context, "Chưa có dữ liệu")
            else (snapshot.villageName ?: snapshot.tag) +
                (if (snapshot.villageName.isNullOrBlank()) "" else " • " + snapshot.tag) +
                " • TH" + (snapshot.townHallLevel ?: "—") +
                (if (builderTotal == null) "" else " • " + AppLanguageSettings.translate(context, "Thợ rảnh") + " $buildersFree/$builderTotal")
        )
        views.setTextViewText(
            R.id.widget_status,
            when {
                snapshot == null -> AppLanguageSettings.translate(context, "Mở ứng dụng và nhập dữ liệu JSON")
                activeCount == 0 && expiredCount == 0 -> AppLanguageSettings.translate(context, "Không có mục nào đang nâng cấp")
                activeCount > 0 && expiredCount > 0 -> "$activeCount " + AppLanguageSettings.translate(context, "đang nâng") + " • $expiredCount " + AppLanguageSettings.translate(context, "hoàn thành")
                activeCount > 0 -> "$activeCount " + AppLanguageSettings.translate(context, "mục đang nâng cấp")
                else -> "$expiredCount " + AppLanguageSettings.translate(context, "hoàn thành • chờ JSON mới")
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
                .setTitle(AppLanguageSettings.translate(this, "Chưa có làng để theo dõi"))
                .setMessage(AppLanguageSettings.translate(this, "Hãy mở COC-LIO, đăng nhập tài khoản cần dùng (nếu có), nhập JSON ít nhất một lần rồi thêm tiện ích."))
                .setPositiveButton(AppLanguageSettings.translate(this, "Đóng")) { _, _ -> finish() }
                .show()
            return
        }

        val labels = villages.map {
            (it.villageName?.takeIf { name -> name.isNotBlank() } ?: it.tag) +
                (if (it.villageName.isNullOrBlank()) "" else " • " + it.tag) +
                " • " + AppLanguageSettings.translate(this, "Nhà chính") + " " + (it.townHallLevel ?: "—")
        }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle(AppLanguageSettings.translate(this, "Chọn làng cho tiện ích"))
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
            "Chỉ tài khoản quản lý mới xem được. Danh sách lấy từ hồ sơ tài khoản đã đăng nhập; ứng dụng không tự đối chiếu trạng thái toàn bộ tài khoản Google.",
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
                                contentDescription = "Ảnh đại diện Google",
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
                            ProfileDataRow("Player Tag gần nhất", account.lastVillageTag.ifBlank { "Chưa ghi nhận" })
                            ProfileDataRow("Tên làng gần nhất", account.lastVillageName.ifBlank { "Chưa ghi nhận" })
                            ProfileDataRow("Town Hall", if (account.lastVillageTownHallLevel > 0L) account.lastVillageTownHallLevel.toString() else "Chưa ghi nhận")
                            ProfileDataRow("Builder Hall", if (account.lastVillageBuilderHallLevel > 0L) account.lastVillageBuilderHallLevel.toString() else "Chưa ghi nhận")
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
    languagePreference: String,
    onLanguagePreferenceChanged: (String) -> Unit,
    textScale: Float,
    displaySizeLevel: Int,
    onDisplaySizeLevelChanged: (Int) -> Unit,
    notificationsEnabled: Boolean,
    ledEnabled: Boolean,
    ledPreset: String,
    ledSpeed: Int,
    ledBrightness: Float,
    onLedEnabledChanged: (Boolean) -> Unit,
    onLedPresetChanged: (String) -> Unit,
    onLedSpeedChanged: (Int) -> Unit,
    onLedBrightnessChanged: (Float) -> Unit,
    notificationStyle: String,
    alarmDurationSeconds: Int,
    communityNotificationsEnabled: Boolean,
    updateNotificationsEnabled: Boolean,
    onCommunityNotificationsChanged: (Boolean) -> Unit,
    onUpdateNotificationsChanged: (Boolean) -> Unit,
    onGoogleSignIn: () -> Unit,
    onGoogleSwitchAccount: () -> Unit,
    onGoogleSignOut: () -> Unit,
    onOpenGame: () -> Unit,
    onNotificationsChanged: (Boolean) -> Unit,
    onNotificationStyleChanged: (String) -> Unit,
    onAlarmDurationChanged: (Int) -> Unit,
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
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Text("CÀI ĐẶT", color = MaterialTheme.colorScheme.primary, fontSize = 15.sp, fontWeight = FontWeight.Black)

        FeaturePanel {
            Text("TÀI KHOẢN GOOGLE", color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
                            contentDescription = "Ảnh hồ sơ Google",
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
            Text("GIAO DIỆN ỨNG DỤNG", color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(
                    Triple("system", "Theo thiết bị", "System"),
                    Triple("light", "Sáng", "Light"),
                    Triple("dark", "Tối", "Dark")
                ).forEach { (mode, labelVi, labelEn) ->
                    FilterChip(
                        selected = themeMode == mode,
                        onClick = { onThemeModeChanged(mode) },
                        label = {
                            Text(
                                if (LocalAppLanguage.current == AppUiLanguage.ENGLISH) labelEn else labelVi,
                                fontSize = 10.sp
                            )
                        }
                    )
                }
            }
        }

        FeaturePanel {
            Text("NGÔN NGỮ", color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(
                "Tự động dùng ngôn ngữ thiết bị. Có thể chọn Tiếng Việt hoặc English bên dưới.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp,
                lineHeight = 15.sp
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    "system" to "Theo hệ thống",
                    "vi" to "Tiếng Việt",
                    "en" to "English"
                ).forEach { (preference, label) ->
                    FilterChip(
                        selected = languagePreference == preference,
                        onClick = { onLanguagePreferenceChanged(preference) },
                        label = { Text(label, fontSize = 10.sp) }
                    )
                }
            }
            Text(
                when (languagePreference) {
                    "system" -> if (LocalAppLanguage.current == AppUiLanguage.ENGLISH) {
                        "Đang dùng ngôn ngữ hệ thống: English"
                    } else {
                        "Đang dùng ngôn ngữ hệ thống: Tiếng Việt"
                    }
                    "en" -> "Ngôn ngữ đang dùng: English"
                    else -> "Ngôn ngữ đang dùng: Tiếng Việt"
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp
            )
            Text(
                "Lựa chọn ngôn ngữ được lưu trên thiết bị này.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 9.sp
            )
        }

        val currentFontLevel = AppPreferences.textScaleLevel(textScale)
        val activeSizeLevel = if (selectedSizeSetting == 0) displaySizeLevel else currentFontLevel
        val activeSizePercent = if (selectedSizeSetting == 0) {
            (AppPreferences.displaySizeScale(displaySizeLevel) * 100f).roundToInt()
        } else {
            (AppPreferences.textScaleForLevel(currentFontLevel) * 100f).roundToInt()
        }
        FeaturePanel {
            Text("KÍCH THƯỚC GIAO DIỆN & CỠ CHỮ", color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
                    "MỨC $activeSizeLevel/5 · $activeSizePercent%",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                (1..5).forEach { level ->
                    FilterChip(
                        selected = activeSizeLevel == level,
                        onClick = {
                            if (selectedSizeSetting == 0) {
                                onDisplaySizeLevelChanged(level)
                            } else {
                                onTextScaleChanged(AppPreferences.textScaleForLevel(level))
                            }
                        },
                        modifier = Modifier.weight(1f).heightIn(min = 38.dp),
                        label = { Text(level.toString(), fontSize = 10.sp) }
                    )
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
                    "Điều chỉnh kích thước tổng thể của giao diện, nút và khoảng cách. Mức 3 (100%) là mặc định."
                else
                    "Điều chỉnh cỡ chữ trong toàn ứng dụng. Mức 3 (100%) là mặc định.",
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
            Text("LED RGB TÙY CHỈNH", color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Bật hiệu ứng chuyển động", color = MaterialTheme.colorScheme.onSurface, fontSize = 10.sp, modifier = Modifier.weight(1f))
                Switch(checked = ledEnabled, onCheckedChange = onLedEnabledChanged)
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
            Text("Độ sáng: ${(ledBrightness * 100f).roundToInt().coerceIn(0, 100)}%", color = MaterialTheme.colorScheme.onSurface, fontSize = 10.sp)
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
            Text(
                "THEO DÕI TIẾN TRÌNH NÂNG CẤP VÀ THÔNG BÁO",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("THÔNG BÁO NÂNG CẤP", color = MaterialTheme.colorScheme.onSurface, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text("Nhắc khi bộ đếm trong JSON đã đếm hết.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                }
                Switch(checked = notificationsEnabled, onCheckedChange = onNotificationsChanged)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("TIN NHẮN CỘNG ĐỒNG", color = MaterialTheme.colorScheme.onSurface, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text("Cần đăng nhập Google; kiểm tra tin mới khi có mạng, khoảng 15 phút/lần.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                }
                Switch(checked = communityNotificationsEnabled, onCheckedChange = onCommunityNotificationsChanged)
            }
            Text(
                "Để hiện bong bóng, hãy bật Bong bóng trong cài đặt thông báo Android hoặc nhấn giữ thông báo tin nhắn cộng đồng rồi chọn hiển thị dạng bong bóng.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 9.sp,
                lineHeight = 13.sp
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("THÔNG BÁO BẢN CẬP NHẬT APK", color = MaterialTheme.colorScheme.onSurface, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text("Cần đăng nhập Google; báo khi có phiên bản mới trong mục Cộng đồng.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                }
                Switch(checked = updateNotificationsEnabled, onCheckedChange = onUpdateNotificationsChanged)
            }
            Text(
                "Thông báo được kiểm tra định kỳ bằng kết nối mạng. Android có thể trì hoãn lâu hơn 15 phút khi tiết kiệm pin hoặc hạn chế chạy nền.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 9.sp,
                lineHeight = 13.sp
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
            if (notificationStyle == "alarm") {
                Text(
                    "Thời gian hiển thị báo thức",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(5, 10, 15, 20, 25).forEach { seconds ->
                        FilterChip(
                            selected = alarmDurationSeconds == seconds,
                            onClick = { onAlarmDurationChanged(seconds) },
                            label = { Text("${seconds}s", fontSize = 10.sp) }
                        )
                    }
                }
                Text(
                    "Thông báo sẽ tự đóng sau ${alarmDurationSeconds} giây. Trong thời gian đó, bạn có thể nhấn MỞ GAME để vào Clash of Clans.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp,
                    lineHeight = 14.sp
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
                "Thời điểm nhắc lấy từ bộ đếm thời gian trong JSON đã nhập. Android vẫn có thể trì hoãn nếu tắt quyền thông báo, hạn chế pin hoặc đặt chế độ Không làm phiền.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                lineHeight = 16.sp
            )
        }

        FeaturePanel {
            Text("TIỆN ÍCH MÀN HÌNH CHÍNH", color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
            Text("CÁCH LẤY JSON", color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(
                "Mở Clash of Clans → Cài đặt (bánh răng) → Cài đặt khác → kéo xuống mục Xuất dữ liệu → chọn Xuất dữ liệu làng dưới dạng JSON → Sao chép. Trở lại COC-LIO và dán nội dung JSON.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                lineHeight = 17.sp
            )
            Button(
                onClick = onOpenGame,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = FeatureCyan, contentColor = Color(0xFF04101D))
            ) { Text("MỞ CLASH OF CLANS", fontWeight = FontWeight.Black) }
        }

        FeaturePanel {
            Text("LIÊN HỆ NHÀ PHÁT TRIỂN", color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
            Text("THÔNG TIN ỨNG DỤNG", color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
                    val brush = if (led.enabled && led.speedMillis > 0) {
                        val progress = (led.progress.value * (20000f / led.speedMillis)).rem(1f)
                        val intervals = palette.size - 1
                        val colors = List(32) { index ->
                            val position = ((index / 32f - progress + 1f) % 1f) * intervals
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
                        Brush.sweepGradient(colors)
                    } else {
                        Brush.sweepGradient(palette)
                    }
                    drawPath(path, brush, style = Stroke(width = stroke))
                }
            }
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
        content = content
    )
}

private const val COC_LIO_COMMUNITY_CHANNEL = "coc_lio_community_messages"
private const val COC_LIO_UPDATE_CHANNEL = "coc_lio_app_updates"
private const val COC_LIO_COMMUNITY_SHORTCUT = "coc_lio_community_conversation"

private fun ensureCommunityBubbleShortcut(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return
    runCatching {
        val shortcutManager = context.getSystemService(ShortcutManager::class.java)
        val bubbleIntent = Intent(context, CocLioBubbleActivity::class.java).setAction(Intent.ACTION_VIEW)
        val shortcut = ShortcutInfo.Builder(context, COC_LIO_COMMUNITY_SHORTCUT)
            .setShortLabel(AppLanguageSettings.translate(context, "Cộng đồng"))
            .setLongLabel(AppLanguageSettings.translate(context, "Cộng đồng COC-LIO"))
            .setIcon(Icon.createWithResource(context, android.R.drawable.sym_action_chat))
            .setIntent(bubbleIntent)
            .setLongLived(true)
            .setCategories(setOf("android.shortcut.conversation"))
            .setPerson(
                android.app.Person.Builder()
                    .setName(AppLanguageSettings.translate(context, "Cộng đồng COC-LIO"))
                    .setKey(COC_LIO_COMMUNITY_SHORTCUT)
                    .setImportant(true)
                    .build()
            )
            .build()
        shortcutManager.pushDynamicShortcut(shortcut)
    }
}

class CocLioMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        val type = data["type"] ?: return
        val isCommunity = type == "community_message"
        if (isCommunity && FirebaseAuth.getInstance().currentUser?.uid == data["senderUid"]) return
        val channelId = if (isCommunity) COC_LIO_COMMUNITY_CHANNEL else COC_LIO_UPDATE_CHANNEL
        val title = data["title"] ?: AppLanguageSettings.translate(this, if (isCommunity) "Cộng đồng COC-LIO" else "COC-LIO có bản cập nhật")
        val body = data["body"] ?: AppLanguageSettings.translate(this, if (isCommunity) "Có tin nhắn mới trong cộng đồng." else "Có phiên bản APK mới.")
        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                AppLanguageSettings.translate(this, if (isCommunity) "Tin nhắn cộng đồng" else "Bản cập nhật COC-LIO"),
                NotificationManager.IMPORTANCE_HIGH
            )
            channel.description = AppLanguageSettings.translate(this, if (isCommunity) "Thông báo tin nhắn mới trong cộng đồng COC-LIO" else "Thông báo khi quản lý chọn gửi tin bản cập nhật")
            if (isCommunity && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) channel.setAllowBubbles(true)
            manager.createNotificationChannel(channel)
        }
        val notificationId = (type + data["messageId"].orEmpty() + data["version"].orEmpty()).hashCode()
        val openIntent = Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val contentIntent = PendingIntent.getActivity(
            this,
            notificationId,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val senderPerson = androidx.core.app.Person.Builder()
            .setName(data["senderName"] ?: AppLanguageSettings.translate(this, "Thành viên"))
            .setKey(data["senderUid"] ?: "community")
            .setImportant(true)
            .build()
        val currentPerson = androidx.core.app.Person.Builder()
            .setName(AppLanguageSettings.translate(this, "Bạn"))
            .setKey("coc-lio-current-user")
            .build()
        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(
                if (isCommunity) {
                    NotificationCompat.MessagingStyle(currentPerson)
                        .addMessage(body, System.currentTimeMillis(), senderPerson)
                } else {
                    NotificationCompat.BigTextStyle().bigText(body)
                }
            )
            .setContentIntent(contentIntent)
            .setAutoCancel(!(isCommunity && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R))
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
        if (isCommunity) builder.addPerson(senderPerson)

        if (isCommunity && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            ensureCommunityBubbleShortcut(this)
            val bubbleIntent = Intent(this, CocLioBubbleActivity::class.java)
                .setAction(Intent.ACTION_VIEW)
                .putExtra("bubble_title", title)
                .putExtra("bubble_body", body)
            val bubblePendingIntent = PendingIntent.getActivity(
                this,
                notificationId xor 0x434F43,
                bubbleIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
            val bubble = NotificationCompat.BubbleMetadata.Builder(
                bubblePendingIntent,
                IconCompat.createWithResource(this, android.R.drawable.sym_action_chat)
            ).setDesiredHeight(480).build()
            builder.setShortcutId(COC_LIO_COMMUNITY_SHORTCUT).setBubbleMetadata(bubble)
        }

        runCatching {
            NotificationManagerCompat.from(this).notify(notificationId, builder.build())
        }
    }
}

internal fun postCommunityChatBubbleNotification(
    context: Context,
    notificationId: Int,
    senderUid: String,
    senderName: String,
    messageText: String
) {
    val notificationManager = NotificationManagerCompat.from(context)
    if (!notificationManager.areNotificationsEnabled()) return
    val channelId = COC_LIO_COMMUNITY_CHANNEL
    val title = AppLanguageSettings.translate(context, "Tin nhắn cộng đồng")
    val channelName = AppLanguageSettings.translate(context, "Tin nhắn cộng đồng")
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH)
        channel.description = AppLanguageSettings.translate(context, "Thông báo tin nhắn mới trong cộng đồng COC-LIO")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) channel.setAllowBubbles(true)
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
    val openIntent = Intent(context, MainActivity::class.java)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
    val contentIntent = PendingIntent.getActivity(
        context,
        notificationId,
        openIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    val sender = androidx.core.app.Person.Builder()
        .setName(senderName.ifBlank { AppLanguageSettings.translate(context, "Thành viên") })
        .setKey(senderUid.ifBlank { "community" })
        .setImportant(true)
        .build()
    val currentUser = FirebaseAuth.getInstance().currentUser
    val currentPerson = androidx.core.app.Person.Builder()
        .setName(AppLanguageSettings.translate(context, "Bạn"))
        .setKey(currentUser?.uid ?: "coc-lio-current-user")
        .build()
    val builder = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentTitle(title)
        .setContentText((senderName + ": " + messageText).take(420))
        .setStyle(
            NotificationCompat.MessagingStyle(currentPerson)
                .setConversationTitle(AppLanguageSettings.translate(context, "Cộng đồng COC-LIO"))
                .addMessage(messageText.take(2000), System.currentTimeMillis(), sender)
        )
        .setContentIntent(contentIntent)
        .setAutoCancel(false)
        .setCategory(NotificationCompat.CATEGORY_MESSAGE)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .addPerson(sender)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        ensureCommunityBubbleShortcut(context)
        val bubbleIntent = Intent(context, CocLioBubbleActivity::class.java)
            .setAction(Intent.ACTION_VIEW)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .putExtra("bubble_title", AppLanguageSettings.translate(context, "Cộng đồng COC-LIO"))
        val mutableFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
        val bubblePendingIntent = PendingIntent.getActivity(
            context,
            notificationId xor 0x434F43,
            bubbleIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or mutableFlag
        )
        val bubble = NotificationCompat.BubbleMetadata.Builder(
            bubblePendingIntent,
            IconCompat.createWithResource(context, android.R.drawable.sym_action_chat)
        ).setDesiredHeight(560).build()
        builder.setShortcutId(COC_LIO_COMMUNITY_SHORTCUT).setBubbleMetadata(bubble)
    }
    runCatching { notificationManager.notify(notificationId, builder.build()) }
}

private data class CommunityBubbleMessage(
    val id: String,
    val uid: String,
    val displayName: String,
    val text: String,
    val type: String,
    val senderIsAdmin: Boolean
)

class CocLioBubbleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        val language = AppLanguageSettings.resolve(
            AppLanguageSettings.load(this),
            resources.configuration.locales[0].language
        )
        val title = intent.getStringExtra("bubble_title")
            ?: AppLanguageSettings.translate(this, "Cộng đồng COC-LIO")
        setContent {
            CompositionLocalProvider(LocalAppLanguage provides language) {
                MaterialTheme {
                    CommunityBubbleChatContent(
                        title = title,
                        onOpenApp = {
                            startActivity(
                                Intent(this@CocLioBubbleActivity, MainActivity::class.java)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun CommunityBubbleChatContent(
    title: String,
    onOpenApp: () -> Unit
) {
    val context = LocalContext.current
    val user = remember { FirebaseAuth.getInstance().currentUser }
    var messages by remember { mutableStateOf<List<CommunityBubbleMessage>>(emptyList()) }
    var draft by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(user != null) }
    var sending by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scrollState = rememberScrollState()

    DisposableEffect(user?.uid) {
        if (user == null) {
            messages = emptyList()
            loading = false
            onDispose { }
        } else {
            val registration = FirebaseFirestore.getInstance()
                .collection("community_messages")
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .limitToLast(60)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        errorMessage = error.message
                            ?: AppLanguageSettings.translate(context, "Không thể tải tin nhắn.")
                        loading = false
                    } else {
                        messages = snapshot?.documents.orEmpty().mapNotNull { document ->
                            val uid = document.getString("uid") ?: return@mapNotNull null
                            val type = document.getString("type").orEmpty()
                            val text = when (type) {
                                "text", "json" -> document.getString("text").orEmpty()
                                "image", "file" -> document.getString("fileName").orEmpty()
                                else -> ""
                            }
                            if (text.isBlank()) return@mapNotNull null
                            CommunityBubbleMessage(
                                id = document.id,
                                uid = uid,
                                displayName = document.getString("displayName").orEmpty(),
                                text = text,
                                type = type,
                                senderIsAdmin = document.getBoolean("senderIsAdmin") ?: false
                            )
                        }
                        errorMessage = null
                        loading = false
                    }
                }
            onDispose { registration.remove() }
        }
    }

    LaunchedEffect(messages.size) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    androidx.compose.material3.Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(Modifier.weight(1f)) {
                    Text(title, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black)
                    Text(
                        AppLanguageSettings.translate(context, "Trò chuyện cộng đồng"),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 9.sp
                    )
                }
                TextButton(onClick = onOpenApp) {
                    Text(AppLanguageSettings.translate(context, "Mở ứng dụng"), fontSize = 10.sp)
                }
            }
            if (user == null) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        AppLanguageSettings.translate(context, "Đăng nhập Google để trò chuyện và gửi tin nhắn."),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                    Button(onClick = onOpenApp) {
                        Text(AppLanguageSettings.translate(context, "ĐĂNG NHẬP"))
                    }
                }
            } else {
                Column(
                    modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (loading) {
                        Text(
                            AppLanguageSettings.translate(context, "Đang tải tin nhắn…"),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp
                        )
                    }
                    if (!loading && messages.isEmpty()) {
                        Text(
                            AppLanguageSettings.translate(context, "Chưa có tin nhắn. Hãy gửi lời chào đầu tiên!"),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                    messages.forEach { message ->
                        val mine = message.uid == user.uid
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start
                        ) {
                            Card(
                                modifier = Modifier.widthIn(max = 300.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (mine) MaterialTheme.colorScheme.primaryContainer
                                    else if (message.senderIsAdmin) Color(0xFF3A2A12)
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                    verticalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    if (!mine) {
                                        Text(
                                            message.displayName.ifBlank {
                                                AppLanguageSettings.translate(context, "Thành viên")
                                            },
                                            color = MaterialTheme.colorScheme.primary,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    if (message.type == "text") {
                                        CommunityAutoTranslatedText(
                                            sourceText = message.text,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontSize = 11.sp
                                        )
                                    } else {
                                        Text(
                                            message.text,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                errorMessage?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, fontSize = 9.sp)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    androidx.compose.material3.OutlinedTextField(
                        value = draft,
                        onValueChange = { if (it.length <= 2000) draft = it },
                        modifier = Modifier.weight(1f),
                        placeholder = {
                            Text(
                                AppLanguageSettings.translate(context, "Viết tin nhắn…"),
                                fontSize = 11.sp
                            )
                        },
                        maxLines = 3,
                        textStyle = MaterialTheme.typography.bodySmall
                    )
                    Button(
                        enabled = !sending && draft.trim().isNotEmpty(),
                        onClick = {
                            val text = draft.trim()
                            val signedInUser = FirebaseAuth.getInstance().currentUser
                            if (signedInUser != null && text.isNotEmpty()) {
                                sending = true
                                errorMessage = null
                                FirebaseFirestore.getInstance()
                                    .collection("community_messages")
                                    .add(
                                        mapOf(
                                            "uid" to signedInUser.uid,
                                            "displayName" to (signedInUser.displayName ?: signedInUser.email ?: "Thành viên").take(80),
                                            "senderIsAdmin" to FirebaseAccountAdministration.isOwner(signedInUser.email),
                                            "photoUrl" to signedInUser.photoUrl?.toString().orEmpty().take(1000),
                                            "type" to "text",
                                            "text" to text.take(2000),
                                            "fileName" to "",
                                            "storagePath" to "",
                                            "downloadUrl" to "",
                                            "contentType" to "",
                                            "fileSize" to 0L,
                                            "sharedJsonId" to "",
                                            "createdAt" to FieldValue.serverTimestamp()
                                        )
                                    )
                                    .addOnSuccessListener {
                                        draft = ""
                                        sending = false
                                    }
                                    .addOnFailureListener { failure ->
                                        errorMessage = failure.message
                                            ?: AppLanguageSettings.translate(context, "Không gửi được tin nhắn.")
                                        sending = false
                                    }
                            }
                        },
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Text(
                            if (sending) "…" else AppLanguageSettings.translate(context, "GỬI"),
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}
