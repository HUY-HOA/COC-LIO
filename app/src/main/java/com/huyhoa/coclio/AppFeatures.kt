package com.huyhoa.coclio

import android.app.Activity
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.suspendCancellableCoroutine
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
            .getBoolean(key(profile, "notifications"), true)

    fun setNotificationsEnabled(context: Context, profile: String, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(key(profile, "notifications"), enabled).apply()
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
        1 -> 0.82f
        2 -> 0.89f
        3 -> 0.96f
        4 -> 1.03f
        else -> 1.10f
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
                    task.exception ?: IllegalStateException("Firebase không trả về kết quả.")
                )
            }
        }
    }

private suspend fun syncFirebaseUserProfile(
    user: FirebaseUser,
    google: GoogleAccountData,
    countAsSignIn: Boolean = true
) {
    val email = user.email?.trim()?.takeIf { it.isNotBlank() }
        ?: throw IllegalStateException("Firebase không trả về email tài khoản.")
    val reference = FirebaseFirestore.getInstance()
        .collection("users")
        .document(user.uid)
    val existing = reference.get().awaitFirebaseResult()
    val signInCount = (existing.getLong("signInCount") ?: 0L) + 1L
    val sharesAdditionalData = existing.getBoolean("dataSharingConsent") == true
    val profile = hashMapOf<String, Any>(
        "uid" to user.uid,
        "email" to email,
        "displayName" to (google.displayName ?: user.displayName.orEmpty()),
        "givenName" to if (sharesAdditionalData) google.givenName.orEmpty() else "",
        "familyName" to if (sharesAdditionalData) google.familyName.orEmpty() else "",
        "photoUrl" to (google.profilePictureUri ?: user.photoUrl?.toString().orEmpty()),
        "emailVerified" to user.isEmailVerified,
        "providerId" to "google.com",
        "updatedAt" to FieldValue.serverTimestamp()
    )
    if (!existing.exists()) {
        profile["createdAt"] = FieldValue.serverTimestamp()
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
    val dataSharingConsent: Boolean?
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

    suspend fun getDataSharingConsent(expectedEmail: String): Boolean? {
        val user = FirebaseAuth.getInstance().currentUser ?: return null
        if (!user.email.equals(expectedEmail, ignoreCase = true)) return null
        val document = FirebaseFirestore.getInstance()
            .collection("users")
            .document(user.uid)
            .get()
            .awaitFirebaseResult()
        return document.getBoolean("dataSharingConsent")
    }

    suspend fun saveDataSharingConsent(google: GoogleAccountData, consented: Boolean) {
        val user = FirebaseAuth.getInstance().currentUser
            ?: throw IllegalStateException("Bạn cần đăng nhập Google để lưu lựa chọn chia sẻ dữ liệu.")
        require(user.email.equals(google.email, ignoreCase = true)) {
            "Tài khoản Google hiện tại không khớp."
        }
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(user.uid)
            .update(
                mapOf(
                    "dataSharingConsent" to consented,
                    "givenName" to if (consented) google.givenName.orEmpty() else "",
                    "familyName" to if (consented) google.familyName.orEmpty() else "",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            .awaitFirebaseResult()
    }

    suspend fun loadAccounts(): List<AdminGoogleAccount> {
        val result = FirebaseFirestore.getInstance()
            .collection("users")
            .get()
            .awaitFirebaseResult()
        return result.documents.map { document ->
            AdminGoogleAccount(
                uid = document.getString("uid") ?: document.id,
                email = document.getString("email").orEmpty(),
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
                dataSharingConsent = document.getBoolean("dataSharingConsent")
            )
        }.sortedByDescending { it.lastSignInAt }
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
            "Chưa cấu hình mã ứng dụng đăng nhập Google. Cần thêm mã GOOGLE_WEB_CLIENT_ID trong phần bí mật của GitHub để bật tính năng này."
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
            ?: throw IllegalStateException("Firebase Authentication không trả về người dùng.")

        val accountData = GoogleAccountData(
            email = email.lowercase(),
            displayName = google.displayName?.trim()?.takeIf { it.isNotBlank() },
            givenName = google.givenName?.trim()?.takeIf { it.isNotBlank() },
            familyName = google.familyName?.trim()?.takeIf { it.isNotBlank() },
            profilePictureUri = google.profilePictureUri?.toString()
        )

        runCatching { syncFirebaseUserProfile(firebaseUser, accountData) }
            .onFailure {
                android.util.Log.w(
                    "COC-LIO",
                    "Không thể đồng bộ hồ sơ Google lên Firestore. Cần kiểm tra Firestore và Security Rules.",
                    it
                )
            }

        return accountData
    }

    suspend fun syncCurrentProfile(context: Context, expectedEmail: String) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val email = user.email?.trim().orEmpty()
        if (email.isBlank() || !email.equals(expectedEmail, ignoreCase = true)) return
        val stored = AccountProfileStore.googleData(context, expectedEmail)
        val google = stored ?: GoogleAccountData(
            email = email,
            displayName = user.displayName,
            givenName = null,
            familyName = null,
            profilePictureUri = user.photoUrl?.toString()
        )
        syncFirebaseUserProfile(user, google, countAsSignIn = false)
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

private fun ensureUpgradeNotificationChannel(
    context: Context,
    channelId: String,
    alarmStyle: Boolean
) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val manager = context.getSystemService(NotificationManager::class.java)
    if (manager.getNotificationChannel(channelId) != null) return
    val channel = NotificationChannel(
        channelId,
        if (alarmStyle) "Nhắc nâng cấp kiểu báo thức" else "Tin nhắn nâng cấp",
        if (alarmStyle) NotificationManager.IMPORTANCE_HIGH else NotificationManager.IMPORTANCE_DEFAULT
    )
    channel.description = "Nhắc khi bộ đếm nâng cấp trong JSON đã hết thời gian"
    channel.enableVibration(true)
    if (alarmStyle) {
        channel.vibrationPattern = LongArray(21) { index ->
            when {
                index == 0 -> 0L
                index % 2 == 1 -> 650L
                else -> 350L
            }
        }
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
            if (alarmStyle) LongArray(21) { index ->
                when {
                    index == 0 -> 0L
                    index % 2 == 1 -> 650L
                    else -> 350L
                }
            } else longArrayOf(0, 250)
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
        val profileCode = profile.hashCode().toUInt().toString(16)
        val channelId = "upgrade_" + profileCode + if (alarmStyle) "_alarm_10s" else "_message"
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
            .setAutoCancel(true)
            .setOnlyAlertOnce(false)
            .setPriority(if (alarmStyle) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
        if (alarmStyle) notificationBuilder.setTimeoutAfter(10_000L)
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
        applyLegacyNotificationSettings(notificationBuilder, alarmStyle)
        val notification = notificationBuilder.build()
        runCatching {
            NotificationManagerCompat.from(applicationContext)
                .notify(notificationId, notification)
        }
        Result.success()
    }
}


object UpgradeNotificationTester {
    fun show(context: Context, profile: String, style: String): Boolean = runCatching {
        val alarmStyle = style == "alarm"
        val profileCode = profile.hashCode().toUInt().toString(16)
        val channelId = "upgrade_" + profileCode + if (alarmStyle) "_alarm_10s" else "_message"
        ensureUpgradeNotificationChannel(context, channelId, alarmStyle)

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
            .setAutoCancel(true)
            .setPriority(if (alarmStyle) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
        applyLegacyNotificationSettings(notificationBuilder, alarmStyle)
        if (alarmStyle) notificationBuilder.setTimeoutAfter(10_000L)
        val notification = notificationBuilder.build()
        NotificationManagerCompat.from(context)
            .notify(("coc_lio_test_" + profile).hashCode(), notification)
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
            (snapshot.upgrades + snapshot.gearUps)
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
                .sortedWith(
                    compareBy<WidgetUpgradeEntry> { if (it.endAt > nowSeconds) 0 else 1 }
                        .thenBy { if (it.endAt > nowSeconds) it.endAt else -it.endAt }
                )
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
            if (remaining > 0L) "$levelText • $progressPercent%"
            else "$levelText • HOÀN THÀNH"
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
            views.setTextViewText(R.id.row_timer, "HOÀN THÀNH")
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
            setTextViewText(R.id.row_title, "Đang tải danh sách nâng cấp…")
            setTextViewText(R.id.row_subtitle, "COC-LIO")
            setTextViewText(R.id.row_timer, "ĐANG CẬP NHẬT")
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
        val widgetPadding = (5f * AppPreferences.displaySizeScale(context) * context.resources.displayMetrics.density).roundToInt()
        views.setViewPadding(R.id.widget_root, widgetPadding, widgetPadding, widgetPadding, widgetPadding)

        val builderTotal = snapshot?.homeBuilderTotal
        val buildersFree = if (snapshot == null || builderTotal == null) null else
            (builderTotal - snapshot.homeBuildersBusyAt(nowSeconds)).coerceAtLeast(0)
        views.setTextViewText(
            R.id.widget_village,
            if (snapshot == null) "COC-LIO • Chưa có dữ liệu"
            else (snapshot.villageName ?: snapshot.tag) +
                (if (snapshot.villageName.isNullOrBlank()) "" else " • " + snapshot.tag) +
                " • TH" + (snapshot.townHallLevel ?: "—") +
                (if (builderTotal == null) "" else " • Thợ rảnh $buildersFree/$builderTotal")
        )
        views.setTextViewText(
            R.id.widget_status,
            when {
                snapshot == null -> "Mở ứng dụng và nhập dữ liệu JSON"
                activeCount == 0 && expiredCount == 0 -> "Không có mục nào đang nâng cấp"
                activeCount > 0 && expiredCount > 0 -> "$activeCount đang nâng • $expiredCount hoàn thành"
                activeCount > 0 -> "$activeCount mục đang nâng cấp"
                else -> "$expiredCount hoàn thành • chờ JSON mới"
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
                .setTitle("Chưa có làng để theo dõi")
                .setMessage("Hãy mở COC-LIO, đăng nhập tài khoản cần dùng (nếu có), nhập JSON ít nhất một lần rồi thêm tiện ích.")
                .setPositiveButton("Đóng") { _, _ -> finish() }
                .show()
            return
        }

        val labels = villages.map {
            (it.villageName?.takeIf { name -> name.isNotBlank() } ?: it.tag) +
                (if (it.villageName.isNullOrBlank()) "" else " • " + it.tag) +
                " • Nhà chính " + (it.townHallLevel ?: "—")
        }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Chọn làng cho tiện ích")
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
    val scope = rememberCoroutineScope()
    var accounts by remember { mutableStateOf<List<AdminGoogleAccount>>(emptyList()) }
    var expandedAccounts by remember { mutableStateOf<Set<String>>(emptySet()) }
    var loading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun refreshAccounts() {
        scope.launch {
            loading = true
            errorMessage = null
            try {
                accounts = FirebaseAccountAdministration.loadAccounts()
            } catch (error: Exception) {
                errorMessage = error.message ?: "Không thể tải danh sách tài khoản."
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshAccounts()
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
            "Chỉ tài khoản quản lý mới xem được. Thông tin chi tiết chỉ mở khi bạn chọn từng tài khoản.",
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
                modifier = Modifier.fillMaxWidth(),
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
                                "Đăng nhập ${account.signInCount} lần · ${formatAdminAccountTime(account.lastSignInAt)}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 8.sp
                            )
                            Text(
                                when (account.dataSharingConsent) {
                                    true -> "Đã đồng ý chia sẻ dữ liệu bổ sung"
                                    false -> "Không đồng ý chia sẻ dữ liệu bổ sung"
                                    null -> "Chưa lựa chọn chia sẻ dữ liệu"
                                },
                                color = when (account.dataSharingConsent) {
                                    true -> Color(0xFF35D6A3)
                                    false -> Color(0xFFFFB74D)
                                    null -> MaterialTheme.colorScheme.onSurfaceVariant
                                },
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
                    TextButton(
                        onClick = {
                            expandedAccounts = if (expanded) expandedAccounts - account.uid
                            else expandedAccounts + account.uid
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(if (expanded) "ẨN THÔNG TIN" else "XEM THÔNG TIN", fontSize = 9.sp)
                    }
                    if (expanded) {
                        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            ProfileDataRow("Tên người dùng đặt", account.preferredName)
                            ProfileDataRow("Tên Google", account.displayName)
                            ProfileDataRow("Tên", account.givenName)
                            ProfileDataRow("Họ", account.familyName)
                            ProfileDataRow(
                                "Chia sẻ dữ liệu bổ sung",
                                when (account.dataSharingConsent) {
                                    true -> "Đã đồng ý"
                                    false -> "Không đồng ý"
                                    null -> "Chưa lựa chọn"
                                }
                            )
                            ProfileDataRow("Ảnh đại diện", account.photoUrl)
                            ProfileDataRow("Email xác minh", if (account.emailVerified) "Đã xác minh" else "Chưa xác minh")
                            ProfileDataRow("Nhà cung cấp", account.providerId)
                            ProfileDataRow("Số lần đăng nhập", account.signInCount.toString())
                            ProfileDataRow("Đăng nhập gần nhất", formatAdminAccountTime(account.lastSignInAt))
                            ProfileDataRow("Tạo hồ sơ", formatAdminAccountTime(account.createdAt))
                            ProfileDataRow("Cập nhật hồ sơ", formatAdminAccountTime(account.updatedAt))
                            ProfileDataRow("Firebase UID", account.uid)
                        }
                    }
                }
            }
        }
        if (!loading && errorMessage == null && accounts.isEmpty()) {
            Text("Chưa có tài khoản nào được đồng bộ lên Firestore.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
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
    ledEnabled: Boolean,
    ledPreset: String,
    ledSpeed: Int,
    ledBrightness: Float,
    onLedEnabledChanged: (Boolean) -> Unit,
    onLedPresetChanged: (String) -> Unit,
    onLedSpeedChanged: (Int) -> Unit,
    onLedBrightnessChanged: (Float) -> Unit,
    notificationStyle: String,
    onGoogleSignIn: () -> Unit,
    onGoogleSignOut: () -> Unit,
    onOpenGame: () -> Unit,
    onNotificationsChanged: (Boolean) -> Unit,
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
                    "Google cung cấp email, tên và ảnh đại diện nếu có. Tên hiển thị bạn tự đặt cũng được lưu trong hồ sơ tài khoản để quản lý nhận biết tài khoản.",
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
                    Text("THÔNG TIN GOOGLE QUẢN LÝ", color = MaterialTheme.colorScheme.primary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    ProfileDataRow("Tên Google", accountData?.displayName)
                    ProfileDataRow("Tên", accountData?.givenName)
                    ProfileDataRow("Họ", accountData?.familyName)
                    ProfileDataRow("Email", accountData?.email ?: accountEmail)
                    ProfileDataRow("Ảnh đại diện", accountData?.profilePictureUri)
                    ProfileDataRow("Tên bạn đặt", preferredName)
                    if (lastUsedLabel != null) ProfileDataRow("Mở app lần gần nhất trên máy này", lastUsedLabel)
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
                        onClick = onGoogleSignIn,
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
                listOf("system" to "Theo thiết bị", "light" to "Sáng", "dark" to "Tối").forEach { (mode, label) ->
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
                    "Điều chỉnh kích thước tổng thể của giao diện, nút và khoảng cách. Mức 3 (96%) là mặc định."
                else
                    "Điều chỉnh cỡ chữ trong toàn ứng dụng. Mức 3 (100%) là mặc định.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp,
                lineHeight = 14.sp
            )
        }
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
                valueRange = 0f..100f
            )
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("ĐỨNG YÊN", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
                Spacer(Modifier.weight(1f))
                Text("CỰC NHANH", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
            }
            Text("Độ sáng: ${(ledBrightness * 100f).roundToInt().coerceIn(1, 100)}%", color = MaterialTheme.colorScheme.onSurface, fontSize = 10.sp)
            Slider(
                value = (ledBrightness * 100f).roundToInt().coerceIn(1, 100).toFloat(),
                onValueChange = { onLedBrightnessChanged(it.roundToInt().coerceIn(1, 100) / 100f) },
                valueRange = 1f..100f
            )
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Mờ", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
                Spacer(Modifier.weight(1f))
                Text("Sáng", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
            }
        }

        FeaturePanel {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("THÔNG BÁO NÂNG CẤP", color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("Nhắc khi bộ đếm trong JSON đã đếm hết.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                }
                Switch(checked = notificationsEnabled, onCheckedChange = onNotificationsChanged)
            }
            Text("Kiểu thông báo", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
                Text("THỬ THÔNG BÁO NGAY", fontWeight = FontWeight.Black)
            }
            Text(
                "Gửi một thông báo mẫu theo kiểu đang chọn. Thử nghiệm không tự bật lịch nhắc nâng cấp.",
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
            Text("Quét mã QR để mở liên hệ Zalo của nhà phát triển COC-LIO.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(232.dp)
                        .background(Color.White, RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    val qrMatrix = androidx.compose.runtime.remember {
                        com.google.zxing.qrcode.QRCodeWriter().encode(
                            ZALO_CONTACT_URL,
                            com.google.zxing.BarcodeFormat.QR_CODE,
                            1,
                            1
                        )
                    }
                    Canvas(Modifier.fillMaxSize()) {
                        val cellWidth = size.width / qrMatrix.width
                        val cellHeight = size.height / qrMatrix.height
                        for (y in 0 until qrMatrix.height) {
                            for (x in 0 until qrMatrix.width) {
                                if (qrMatrix[x, y]) {
                                    drawRect(
                                        color = Color.Black,
                                        topLeft = androidx.compose.ui.geometry.Offset(
                                            x * cellWidth,
                                            y * cellHeight
                                        ),
                                        size = androidx.compose.ui.geometry.Size(cellWidth, cellHeight)
                                    )
                                }
                            }
                        }
                    }
                }
            }
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
                            val position = ((index / 32f + progress) % 1f) * intervals
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
