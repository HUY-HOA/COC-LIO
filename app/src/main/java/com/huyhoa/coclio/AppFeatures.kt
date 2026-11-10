package com.huyhoa.coclio

import android.app.Activity
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
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
            "Chưa cấu hình Google OAuth Web Client ID. Cần thêm Secret GOOGLE_WEB_CLIENT_ID trong GitHub để bật đăng nhập Google."
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
        val views = RemoteViews(context.packageName, R.layout.widget_upgrade)

        if (snapshot == null) {
            views.setTextViewText(R.id.widget_village, "COC-LIO • Chưa có dữ liệu")
            views.setTextViewText(R.id.widget_item, "Mở ứng dụng và nhập JSON")
            views.setTextViewText(R.id.widget_timer, "Chọn làng khi thêm tiện ích")
            views.setProgressBar(R.id.widget_progress, 100, 0, false)
        } else {
            val upgrade = (snapshot.upgrades + snapshot.gearUps).firstOrNull()
            views.setTextViewText(
                R.id.widget_village,
                "LÀNG " + snapshot.tag + " • NHÀ CHÍNH " + (snapshot.townHallLevel ?: "—")
            )
            if (upgrade == null) {
                views.setTextViewText(R.id.widget_item, "Không có nâng cấp đang chạy")
                views.setTextViewText(R.id.widget_timer, "Nhập JSON mới để cập nhật")
                views.setProgressBar(R.id.widget_progress, 100, 0, false)
            } else {
                val total = (upgrade.timerSeconds ?: 0L).coerceAtLeast(1L)
                val endAt = snapshot.timestamp + total
                val remaining = (endAt - System.currentTimeMillis() / 1000L).coerceAtLeast(0L)
                val elapsedPercent = (100.0 - remaining.toDouble() * 100.0 / total.toDouble()).toInt().coerceIn(0, 100)
                views.setTextViewText(
                    R.id.widget_item,
                    upgrade.name + (upgrade.level?.let { " • cấp " + it } ?: "")
                )
                views.setProgressBar(R.id.widget_progress, 100, elapsedPercent, false)
                if (remaining > 0L) {
                    views.setChronometer(
                        R.id.widget_timer,
                        SystemClock.elapsedRealtime() + remaining * 1000L,
                        null,
                        true
                    )
                } else {
                    views.setTextViewText(R.id.widget_timer, "Đã hết giờ • nhập JSON để xác nhận")
                }
            }
        }

        val open = PendingIntent.getActivity(
            context,
            appWidgetId,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_root, open)
        managerUpdate(context, appWidgetId, views)
    }

    private fun managerUpdate(context: Context, id: Int, views: RemoteViews) {
        AppWidgetManager.getInstance(context).updateAppWidget(id, views)
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
        val villages = VillageStore.loadAll(this, profile).distinctBy { it.tag }.sortedBy { it.tag }
        if (villages.isEmpty()) {
            AlertDialog.Builder(this)
                .setTitle("Chưa có làng để theo dõi")
                .setMessage("Hãy mở COC-LIO, đăng nhập tài khoản cần dùng (nếu có), nhập JSON ít nhất một lần rồi thêm tiện ích.")
                .setPositiveButton("Đóng") { _, _ -> finish() }
                .show()
            return
        }

        val labels = villages.map {
            it.tag + " • Nhà chính " + (it.townHallLevel ?: "—")
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
    notificationStyle: String,
    onGoogleSignIn: () -> Unit,
    onGoogleSignOut: () -> Unit,
    onOpenGame: () -> Unit,
    onNotificationsChanged: (Boolean) -> Unit,
    onNotificationStyleChanged: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        Text("CÀI ĐẶT", color = FeatureCyan, fontSize = 16.sp, fontWeight = FontWeight.Black)
        FeaturePanel {
            Text("TÀI KHOẢN GOOGLE", color = FeatureCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            if (accountEmail.isNullOrBlank()) {
                Text("Chưa đăng nhập", color = Color.White, fontWeight = FontWeight.Bold)
                Text(
                    "Đăng nhập để tách dữ liệu làng theo từng tài khoản Google trên thiết bị này.",
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
                    "Dữ liệu hiện lưu cục bộ trên điện thoại; đăng nhập chưa tự đồng bộ hay sao lưu lên đám mây.",
                    color = FeatureMuted,
                    fontSize = 10.sp,
                    lineHeight = 15.sp
                )
            } else {
                Text(accountEmail, color = Color.White, fontWeight = FontWeight.Bold)
                Text("Dữ liệu nhập mới được lưu riêng trong hồ sơ này.", color = FeatureMuted, fontSize = 11.sp)
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
                "Thời điểm nhắc lấy từ timer của JSON đã nhập. Android vẫn có thể trì hoãn nếu tắt quyền thông báo, hạn chế pin hoặc đặt chế độ Không làm phiền.",
                color = FeatureMuted,
                fontSize = 10.sp,
                lineHeight = 15.sp
            )
        }

        FeaturePanel {
            Text("TIỆN ÍCH MÀN HÌNH CHÍNH", color = FeatureCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(
                "Nhấn giữ màn hình chính → Tiện ích (Widgets) → COC-LIO → Theo dõi nâng cấp. Mỗi tiện ích được gắn với tài khoản đang đăng nhập và một làng đã chọn. Muốn thêm tiện ích cho tài khoản khác, đổi sang tài khoản đó rồi thêm một tiện ích mới.",
                color = FeatureMuted,
                fontSize = 11.sp,
                lineHeight = 17.sp
            )
            Text(
                "Tiện ích hiển thị công trình đang nâng, đồng hồ còn lại và thanh tiến trình thời gian. Đồng hồ chạy liên tục; thanh tiến trình được làm mới định kỳ bởi Android.",
                color = FeatureMuted,
                fontSize = 10.sp,
                lineHeight = 15.sp
            )
        }

        FeaturePanel {
            Text("CÁCH LẤY JSON", color = FeatureCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(
                "Mở Clash of Clans → Cài đặt (bánh răng) → Cài đặt khác (More Settings) → kéo xuống Data Export → Export village data in JSON format → Copy. Trở lại COC-LIO và dán JSON.",
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
            Text("THÔNG TIN ỨNG DỤNG", color = FeatureCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(
                "COC-LIO là ứng dụng cộng đồng không chính thức, không được Supercell chứng thực. Hình ảnh game thuộc quyền sở hữu của Supercell.",
                color = FeatureMuted,
                fontSize = 10.sp,
                lineHeight = 15.sp
            )
            Text(
                "This material is unofficial and is not endorsed by Supercell. For more information see www.supercell.com/fan-content-policy.",
                color = FeatureCyan,
                fontSize = 9.sp,
                lineHeight = 14.sp
            )
        }
    }
}

@Composable
private fun FeaturePanel(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xF20D182F))
    ) {
        Column(
            modifier = Modifier.padding(15.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
            content = content
        )
    }
}
