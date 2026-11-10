package com.huyhoa.coclio

import android.app.Activity
import com.huyhoa.coclio.data.CocAssets
import java.net.URL
import java.net.HttpURLConnection
import java.io.FileOutputStream
import java.io.File
import android.view.View
import android.graphics.BitmapFactory
import android.graphics.Bitmap
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.huyhoa.coclio.data.VillageSnapshot
import com.huyhoa.coclio.data.VillageStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

private val FeatureCyan = Color(0xFF36F4FF)
private val FeatureMuted = Color(0xFF9EAFCE)
private const val ZALO_CONTACT_URL = "https://zaloapp.com/qr/p/1gl8y41uqztdt?src=qr"
private val ZALO_QR_ROWS = """
000000000000000000000000000000000
000000000000000000000000000000000
001111111001000000111100111111100
001000001011011011010000100000100
001011101001110001100100101110100
001011101011010111110010101110100
001011101000101010100110101110100
001000001010100011001010100000100
001111111010101010101010111111100
000000000000001000001000000000000
001111101111101100111011010101000
000011110101000110111111111000100
001111101111011111110011010000000
000010010011110011000110010101000
001110111011010111111100000110000
001111000101101010100100111000100
001111011010100101101011001110000
001000000011101011100011010001000
000101001110101111110100000110000
001011110101000010101110111010100
001011001100111101110011011010000
001010000010110001000111001001000
001110111001011111110111111011100
000000000010101000011010001111100
001111111011001101000110101110000
001000001001111011101110001000000
001011101010100101111011111010100
001011101010110110010111000111100
001011101010000011101001111111000
001000001010101011001010100101000
001111111010000110110101100110000
000000000000000000000000000000000
000000000000000000000000000000000
""".trimIndent().lines()

object AccountProfileStore {
    private const val PREFS = "coc_lio_profiles"
    private const val ACTIVE = "active_profile"

    fun active(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(ACTIVE, "local") ?: "local"

    fun setActive(context: Context, email: String?) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(ACTIVE, email?.trim()?.lowercase()?.takeIf { it.isNotBlank() } ?: "local")
            .apply()
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

    fun notificationStyle(context: Context, profile: String): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(key(profile, "notification_style"), "message") ?: "message"

    fun setNotificationStyle(context: Context, profile: String, style: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(key(profile, "notification_style"), style).apply()
    }
}

object GoogleAccountAuth {
    suspend fun signIn(context: Context): String {
        val clientId = BuildConfig.GOOGLE_WEB_CLIENT_ID
        require(clientId.isNotBlank() && clientId.contains(".apps.googleusercontent.com")) {
            "Chưa cấu hình mã ứng dụng đăng nhập Google. Cần thêm mã GOOGLE_WEB_CLIENT_ID trong phần bí mật của GitHub để bật tính năng này."
        }

        val option = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(clientId)
            .setAutoSelectEnabled(false)
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(option)
            .build()
        val result = CredentialManager.create(context).getCredential(context, request)
        val credential = result.credential
        require(
            credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) { "Không nhận được thông tin đăng nhập Google hợp lệ." }
        val google = GoogleIdTokenCredential.createFrom(credential.data)
        require(google.id.isNotBlank()) { "Google không trả về địa chỉ email." }
        return google.id.lowercase()
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

object UpgradeReminderScheduler {
    private fun profileTag(profile: String) =
        "coc_lio_upgrade_" + profile.hashCode().toUInt().toString(16)

    fun scheduleAll(context: Context, snapshots: List<VillageSnapshot>, profile: String, enabled: Boolean) {
        val manager = WorkManager.getInstance(context)
        val tag = profileTag(profile)
        manager.cancelAllWorkByTag(tag)
        if (!enabled) return

        snapshots.distinctBy { it.tag }.forEach { snapshot ->
            (snapshot.upgrades + snapshot.gearUps).forEach { item ->
                val timer = item.timerSeconds
                if (timer != null && timer > 0L) {
                    val endAt = snapshot.timestamp + timer
                    val delay = (endAt - System.currentTimeMillis() / 1000L).coerceAtLeast(1L)
                    val uniqueName = "coc_lio_" + profile.hashCode() + "_" + snapshot.tag + "_" + item.identity
                    val data = Data.Builder()
                        .putString("profile", profile)
                        .putString("tag", snapshot.tag)
                        .putString("item", item.name)
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
        val alarmStyle = AppPreferences.notificationStyle(applicationContext, profile) == "alarm"
        val profileCode = profile.hashCode().toUInt().toString(16)
        val channelId = "upgrade_" + profileCode + if (alarmStyle) "_alarm" else "_message"
        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(channelId) == null) {
            val channel = NotificationChannel(
                channelId,
                if (alarmStyle) "Nhắc nâng cấp kiểu báo thức" else "Tin nhắn nâng cấp",
                if (alarmStyle) NotificationManager.IMPORTANCE_HIGH else NotificationManager.IMPORTANCE_DEFAULT
            )
            channel.description = "Nhắc khi bộ đếm nâng cấp trong JSON đã hết thời gian"
            channel.enableVibration(true)
            if (alarmStyle) channel.vibrationPattern = longArrayOf(0, 450, 250, 450)
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

        val openApp = Intent(applicationContext, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            (item + tag + profile).hashCode(),
            openApp,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setCategory(if (alarmStyle) NotificationCompat.CATEGORY_ALARM else NotificationCompat.CATEGORY_MESSAGE)
            .setContentTitle("Bộ đếm nâng cấp đã hết")
            .setContentText(item + " • " + tag)
            .setStyle(NotificationCompat.BigTextStyle().bigText(item + " của làng " + tag + " đã đến thời gian dự kiến hoàn thành. Mở COC-LIO và nhập JSON mới để xác nhận cấp độ."))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(if (alarmStyle) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .build()
        runCatching {
            NotificationManagerCompat.from(applicationContext)
                .notify((item + tag + profile).hashCode(), notification)
        }
        Result.success()
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
                .sortedBy { it.endAt }
        }
    }

    override fun getViewAt(position: Int): RemoteViews? {
        val entry = entries.getOrNull(position) ?: return null
        val views = RemoteViews(context.packageName, R.layout.widget_upgrade_row)
        val now = System.currentTimeMillis() / 1000L
        val remaining = (entry.endAt - now).coerceAtLeast(0L)
        val remainingPercent = (
            remaining.toDouble() * 100.0 / entry.totalSeconds.coerceAtLeast(1L)
        ).toInt().coerceIn(0, 100)

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
        views.setTextViewText(R.id.row_title, "$levelText • $remainingPercent%")
        views.setViewVisibility(R.id.row_subtitle, View.GONE)
        views.setProgressBar(R.id.row_progress, 100, remainingPercent, false)

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
        val count = snapshot?.let { it.upgrades.size + it.gearUps.size } ?: 0
        val views = RemoteViews(context.packageName, R.layout.widget_upgrade)

        val nowSeconds = System.currentTimeMillis() / 1000L
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
                count == 0 -> "Không có mục nào đang nâng cấp"
                else -> "$count mục đang nâng cấp"
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
fun SettingsScreen(
    accountEmail: String?,
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
    onNotificationStyleChanged: (String) -> Unit
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Text("CÀI ĐẶT", color = FeatureCyan, fontSize = 15.sp, fontWeight = FontWeight.Black)
        FeaturePanel {
            Text("TÀI KHOẢN GOOGLE", color = FeatureCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            if (accountEmail.isNullOrBlank()) {
                Text("Chưa đăng nhập", color = Color.White, fontWeight = FontWeight.Bold)
                Text(
                    "Đăng nhập hiện tại chỉ xác định hồ sơ trên điện thoại; chưa xác minh giấy phép sử dụng. Chức năng cấp phép của chủ ứng dụng sẽ được bật sau khi cấu hình máy chủ.",
                    color = FeatureMuted,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
                Button(
                    onClick = onGoogleSignIn,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = FeatureCyan, contentColor = Color(0xFF04101D))
                ) { Text("ĐĂNG NHẬP GOOGLE", fontWeight = FontWeight.Black) }
                Text(
                    "Tài khoản Google không dùng để sao lưu. Hiện đăng nhập chỉ đổi hồ sơ cục bộ; hệ thống cấp phép máy chủ chưa được kích hoạt.",
                    color = FeatureMuted,
                    fontSize = 10.sp,
                    lineHeight = 15.sp
                )
            } else {
                Text(accountEmail, color = Color.White, fontWeight = FontWeight.Bold)
                Text("Hồ sơ cục bộ: dữ liệu làng lưu trên điện thoại. Trạng thái giấy phép chưa được xác minh qua máy chủ.", color = FeatureMuted, fontSize = 11.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Button(
                        onClick = onGoogleSignIn,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = FeatureCyan, contentColor = Color(0xFF04101D))
                    ) { Text("ĐỔI TÀI KHOẢN", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                    Button(
                        onClick = onGoogleSignOut,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF263653), contentColor = Color.White)
                    ) { Text("ĐĂNG XUẤT", fontSize = 10.sp) }
                }
            }
        }

        FeaturePanel {
            Text("LED RGB TÙY CHỈNH", color = FeatureCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Bật hiệu ứng chuyển động", color = Color.White, fontSize = 10.sp, modifier = Modifier.weight(1f))
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
            Text("Tốc độ: $ledSpeed/4", color = Color.White, fontSize = 10.sp)
            Slider(
                value = ledSpeed.toFloat(),
                onValueChange = { onLedSpeedChanged(it.toInt().coerceIn(1, 4)) },
                valueRange = 1f..4f,
                steps = 2
            )
            Text("Độ sáng: ${(ledBrightness * 100).toInt()}%", color = Color.White, fontSize = 10.sp)
            Slider(
                value = ledBrightness,
                onValueChange = onLedBrightnessChanged,
                valueRange = 0.25f..1f
            )
        }

        FeaturePanel {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("THÔNG BÁO NÂNG CẤP", color = FeatureCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("Nhắc khi bộ đếm trong JSON đã đếm hết.", color = FeatureMuted, fontSize = 10.sp)
                }
                Switch(checked = notificationsEnabled, onCheckedChange = onNotificationsChanged)
            }
            Text("Kiểu thông báo", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
            Text(
                "Thời điểm nhắc lấy từ bộ đếm thời gian trong JSON đã nhập. Android vẫn có thể trì hoãn nếu tắt quyền thông báo, hạn chế pin hoặc đặt chế độ Không làm phiền.",
                color = FeatureMuted,
                fontSize = 10.sp,
                lineHeight = 15.sp
            )
        }

        FeaturePanel {
            Text("TIỆN ÍCH MÀN HÌNH CHÍNH", color = FeatureCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(
                "Nhấn giữ màn hình chính → chọn Tiện ích → COC-LIO → Theo dõi nâng cấp. Mỗi tiện ích gắn với tài khoản đang đăng nhập và một làng đã chọn. Muốn thêm tiện ích cho tài khoản khác, hãy đổi tài khoản trong COC-LIO rồi thêm một tiện ích mới.",
                color = FeatureMuted,
                fontSize = 11.sp,
                lineHeight = 17.sp
            )
            Text(
                "Tiện ích dạng dọc 2 × 4 ô gọn hơn, có viền RGB chuyển sắc liền mạch và danh sách nâng cấp có thể cuộn. Mỗi mục hiển thị ảnh công trình, đồng hồ đếm ngược ở giữa thanh và phần trăm thời gian còn lại.",
                color = FeatureMuted,
                fontSize = 10.sp,
                lineHeight = 15.sp
            )
        }

        FeaturePanel {
            Text("CÁCH LẤY JSON", color = FeatureCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(
                "Mở Clash of Clans → Cài đặt (bánh răng) → Cài đặt khác → kéo xuống mục Xuất dữ liệu → chọn Xuất dữ liệu làng dưới dạng JSON → Sao chép. Trở lại COC-LIO và dán nội dung JSON.",
                color = FeatureMuted,
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
            Text("LIÊN HỆ NHÀ PHÁT TRIỂN", color = FeatureCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text("Quét mã QR để mở liên hệ Zalo của nhà phát triển COC-LIO.", color = FeatureMuted, fontSize = 10.sp)
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
                    Canvas(Modifier.fillMaxSize()) {
                        val rows = ZALO_QR_ROWS
                        val modules = rows.first().length
                        val cell = size.width / modules
                        rows.forEachIndexed { y, row ->
                            row.forEachIndexed { x, value ->
                                if (value == '1') {
                                    drawRect(
                                        color = Color.Black,
                                        topLeft = Offset(x * cell, y * cell),
                                        size = Size(cell, cell)
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
            Text("THÔNG TIN ỨNG DỤNG", color = FeatureCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(
                "COC-LIO là ứng dụng cộng đồng không chính thức, không được Supercell chứng thực. Hình ảnh game thuộc quyền sở hữu của Supercell.",
                color = FeatureMuted,
                fontSize = 10.sp,
                lineHeight = 15.sp
            )
            Text(
                "Đây là nội dung không chính thức và không được Supercell bảo trợ. Xem chính sách nội dung người hâm mộ tại www.supercell.com/fan-content-policy.",
                color = FeatureCyan,
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
    val transition = rememberInfiniteTransition(label = "settings-led-border")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (led.enabled) led.speedMillis else 60000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "settings-led-progress"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color(0xF20D182F))
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
                    val brush = if (led.enabled) Brush.sweepGradient(colors) else SolidColor(palette.first())
                    drawPath(path, brush, style = Stroke(width = stroke))
                }
            }
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
        content = content
    )
}
