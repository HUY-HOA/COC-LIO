package com.huyhoa.coclio

import android.app.Activity
import com.huyhoa.coclio.data.CocAssets
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
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
        return preferences.getFloat("text_scale", 1f).coerceIn(0.8f, 1.25f)
    }

    fun themeMode(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString("theme_mode", "system") ?: "system"

    fun setThemeMode(context: Context, mode: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString("theme_mode", mode).apply()
    }

    fun setTextScale(context: Context, scale: Float) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putFloat("text_scale", scale.coerceIn(0.8f, 1.25f)).apply()
    }
}

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
            if (errorMessage.contains("[16] Account reauth failed", ignoreCase = true)) {
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
                        "Google vẫn từ chối xác thực. Trong cùng dự án Google Cloud, Android OAuth Client phải có package com.huyhoa.coclio và SHA-1 4E:25:C8:2C:96:FF:B7:9E:83:72:AA:83:45:95:01:98:6E:70:C7:C3. Secret GOOGLE_WEB_CLIENT_ID phải là Web application Client ID của chính dự án đó.",
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
        return GoogleAccountData(
            email = email.lowercase(),
            displayName = google.displayName?.trim()?.takeIf { it.isNotBlank() },
            givenName = google.givenName?.trim()?.takeIf { it.isNotBlank() },
            familyName = google.familyName?.trim()?.takeIf { it.isNotBlank() },
            profilePictureUri = google.profilePictureUri?.toString()
        )
    }

    suspend fun signOut(context: Context) {
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
        if (!enabled) return

        val nowSeconds = System.currentTimeMillis() / 1000L
        snapshots.distinctBy { it.tag }.forEach { snapshot ->
            (snapshot.upgrades + snapshot.gearUps).forEachIndexed { itemIndex, item ->
                val delay = item.remainingSecondsAt(snapshot.timestamp, nowSeconds)
                if (delay > 0L) {
                    val endAt = snapshot.timestamp + (item.timerSeconds ?: 0L)
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
        val textScale = AppPreferences.textScale(context)
        views.setTextViewTextSize(R.id.row_title, TypedValue.COMPLEX_UNIT_SP, 12f * textScale)
        views.setTextViewTextSize(R.id.row_timer, TypedValue.COMPLEX_UNIT_SP, 12f * textScale)
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
            else "$levelText • 100% • HOÀN THÀNH"
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
            views.setChronometer(
                R.id.row_timer,
                SystemClock.elapsedRealtime() + remaining * 1000L,
                "%s",
                true
            )
            views.setChronometerCountDown(R.id.row_timer, true)
        } else {
            views.setTextViewText(R.id.row_timer, "ĐÃ HẾT GIỜ")
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
        val textScale = AppPreferences.textScale(context)
        views.setTextViewTextSize(R.id.widget_village, TypedValue.COMPLEX_UNIT_SP, 10f * textScale)
        views.setTextViewTextSize(R.id.widget_status, TypedValue.COMPLEX_UNIT_SP, 9f * textScale)
        views.setTextViewTextSize(R.id.widget_empty, TypedValue.COMPLEX_UNIT_SP, 9f * textScale)

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
                activeCount > 0 && expiredCount > 0 -> "$activeCount đang nâng • $expiredCount đã hết giờ"
                activeCount > 0 -> "$activeCount mục đang nâng cấp"
                else -> "$expiredCount mục đã hết giờ • chờ JSON mới"
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

@Composable
fun SettingsScreen(
    accountEmail: String?,
    accountData: GoogleAccountData?,
    preferredName: String?,
    lastUsedLabel: String?,
    knownProfiles: List<LocalProfileSummary>,
    themeMode: String,
    onThemeModeChanged: (String) -> Unit,
    textScale: Float,
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
    onTextScaleChanged: (Float) -> Unit,
    onTextScaleChangeFinished: () -> Unit
) {
    val context = LocalContext.current
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
                    "Google cung cấp hồ sơ cơ bản như email, tên và ảnh đại diện nếu có. Lần đầu đăng nhập, bạn cần đặt tên hiển thị riêng.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
                Button(
                    onClick = onGoogleSignIn,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = Color(0xFF04101D))
                ) { Text("ĐĂNG NHẬP GOOGLE", fontWeight = FontWeight.Black) }
                if (knownProfiles.isNotEmpty()) {
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
                            preferredName?.takeIf { it.isNotBlank() } ?: accountData?.displayName ?: accountEmail,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                        Text(accountEmail, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                        Text("Đang dùng trên thiết bị này", color = MaterialTheme.colorScheme.primary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Text("THÔNG TIN GOOGLE TRẢ VỀ", color = MaterialTheme.colorScheme.primary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                ProfileDataRow("Tên Google", accountData?.displayName)
                ProfileDataRow("Tên", accountData?.givenName)
                ProfileDataRow("Họ", accountData?.familyName)
                ProfileDataRow("Email", accountData?.email ?: accountEmail)
                ProfileDataRow("Ảnh đại diện", accountData?.profilePictureUri)
                ProfileDataRow("Tên bạn đặt", preferredName)
                if (lastUsedLabel != null) ProfileDataRow("Mở app lần gần nhất", lastUsedLabel)
                Text(
                    "Thông tin hồ sơ chỉ lưu trên điện thoại này, không đồng bộ hoặc sao lưu lên đám mây. Ứng dụng không nhận mật khẩu Google và không lưu ID token.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp,
                    lineHeight = 15.sp
                )
                Text("CÁC TÀI KHOẢN TỪNG DÙNG TRÊN MÁY NÀY", color = MaterialTheme.colorScheme.primary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                if (knownProfiles.isEmpty()) {
                    Text("Chưa có hồ sơ nào khác.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                } else {
                    knownProfiles.forEach { profile ->
                        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    profile.preferredName?.takeIf { it.isNotBlank() } ?: profile.displayName ?: profile.email,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    if (profile.isCurrent) "ĐANG DÙNG" else "KHÔNG ĐANG DÙNG",
                                    color = if (profile.isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
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
                Text(
                    "Trạng thái trên đây chỉ tính các hồ sơ đã đăng nhập trên điện thoại này. Không thể biết người dùng ở điện thoại khác đang online nếu chưa có máy chủ.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp,
                    lineHeight = 15.sp
                )
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

        val currentFontLevel = (if (textScale <= 1f) {
            (((textScale.coerceIn(0.8f, 1f) - 0.8f) / 0.2f) * 40f + 10f).roundToInt()
        } else {
            (50f + ((textScale.coerceIn(1f, 1.25f) - 1f) / 0.25f) * 50f).roundToInt()
        }).coerceIn(10, 100)
        val selectedFontLevel = (((currentFontLevel + 5) / 10) * 10).coerceIn(10, 100)
        fun fontScaleForLevel(level: Int): Float {
            val safeLevel = level.coerceIn(10, 100)
            return if (safeLevel <= 50) {
                0.8f + (safeLevel - 10) * (0.2f / 40f)
            } else {
                1f + (safeLevel - 50) * (0.25f / 50f)
            }
        }
        FeaturePanel {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("CỠ CHỮ TOÀN ỨNG DỤNG", color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text("MỨC $selectedFontLevel/100", color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, fontWeight = FontWeight.Black)
            }
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                (10..100 step 10).toList().chunked(5).forEach { levels ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        levels.forEach { level ->
                            FilterChip(
                                selected = selectedFontLevel == level,
                                onClick = {
                                    onTextScaleChanged(fontScaleForLevel(level))
                                    onTextScaleChangeFinished()
                                },
                                modifier = Modifier.weight(1f),
                                label = { Text(level.toString(), fontSize = 10.sp) }
                            )
                        }
                    }
                }
            }
            Text("Chọn mức từ 10 đến 100. Mức 50 là cỡ chữ mặc định. Thay đổi được áp dụng và lưu ngay khi chọn.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
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
                valueRange = 0f..100f,
                steps = 99,
                thumb = {}
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
                valueRange = 1f..100f,
                steps = 98,
                thumb = {}
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
