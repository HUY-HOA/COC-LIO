package com.huyhoa.coclio

import android.Manifest

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.android.gms.tasks.Task
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private const val COC_LIO_NOTIFICATION_WORK = "coc_lio_background_notification_poll"
private const val COC_LIO_NOTIFICATION_CHANNEL_UPDATES = "coc_lio_app_updates"
private const val MAX_NOTIFICATION_DELIVERIES_PER_SCAN = 5
private val notificationWorkerMutex = Mutex()

private fun isNotificationChannelBlocked(context: Context, channelId: String): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
    val manager = context.getSystemService(NotificationManager::class.java)
    return manager.getNotificationChannel(channelId)?.importance == NotificationManager.IMPORTANCE_NONE
}

object CommunityNotificationScheduler {
    fun update(
        context: Context,
        profile: String,
        signedIn: Boolean,
        communityEnabled: Boolean,
        updatesEnabled: Boolean
    ) {
        val manager = WorkManager.getInstance(context)
        if (!signedIn || profile == "local" || (!communityEnabled && !updatesEnabled)) {
            manager.cancelUniqueWork(COC_LIO_NOTIFICATION_WORK)
            manager.cancelUniqueWork(COC_LIO_NOTIFICATION_WORK + "_initial")
            return
        }

        val input = Data.Builder()
            .putString("profile", profile)
            .build()
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val periodic = PeriodicWorkRequestBuilder<CommunityNotificationWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .setInputData(input)
            .build()
        manager.enqueueUniquePeriodicWork(
            COC_LIO_NOTIFICATION_WORK,
            ExistingPeriodicWorkPolicy.UPDATE,
            periodic
        )
        val initial = OneTimeWorkRequestBuilder<CommunityNotificationWorker>()
            .setConstraints(constraints)
            .setInputData(input)
            .build()
        manager.enqueueUniqueWork(
            COC_LIO_NOTIFICATION_WORK + "_initial",
            ExistingWorkPolicy.KEEP,
            initial
        )
    }
}

class CommunityNotificationWorker(
    context: Context,
    parameters: WorkerParameters
) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        notificationWorkerMutex.withLock {
            val profile = inputData.getString("profile").orEmpty()
            val user = FirebaseAuth.getInstance().currentUser
                ?: return@withLock Result.success()
            if (profile.isBlank() || user.email?.equals(profile, ignoreCase = true) != true) {
                return@withLock Result.success()
            }

            val communityEnabled = AppPreferences.communityNotificationsEnabled(applicationContext, profile)
            val updatesEnabled = AppPreferences.updateNotificationsEnabled(applicationContext, profile)
            if (!communityEnabled && !updatesEnabled) return@withLock Result.success()

            val notificationsAllowed =
                (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                    ContextCompat.checkSelfPermission(
                        applicationContext,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED) &&
                    NotificationManagerCompat.from(applicationContext).areNotificationsEnabled()
            val communityChannelBlocked = communityEnabled &&
                isNotificationChannelBlocked(applicationContext, "coc_lio_community_messages")
            val updatesChannelBlocked = updatesEnabled &&
                isNotificationChannelBlocked(applicationContext, COC_LIO_NOTIFICATION_CHANNEL_UPDATES)
            if (!notificationsAllowed || communityChannelBlocked || updatesChannelBlocked) {
                return@withLock Result.success()
            }

            try {
                val preferences = applicationContext.getSharedPreferences(
                    "coc_lio_notification_watermarks",
                    Context.MODE_PRIVATE
                )
                val firestore = FirebaseFirestore.getInstance()
                val account = firestore.collection("users")
                    .document(user.uid)
                    .get()
                    .awaitNotificationResult()
                if (
                    !account.exists() ||
                    account.getLong("requiredAccountDataConsentVersion") != AccountPrivacyConsent.REQUIRED_ACCOUNT_DATA_VERSION ||
                    account.getBoolean("accountDisabled") == true
                ) {
                    return@withLock Result.success()
                }
                var hasMoreNotifications = false
                if (communityEnabled) {
                    hasMoreNotifications = checkCommunityMessages(
                        applicationContext,
                        firestore,
                        preferences,
                        profile,
                        user.uid
                    ) || hasMoreNotifications
                }
                if (updatesEnabled) {
                    hasMoreNotifications = checkAppUpdates(
                        applicationContext,
                        firestore,
                        preferences,
                        profile
                    ) || hasMoreNotifications
                }
                if (hasMoreNotifications) {
                    val drainRequest = OneTimeWorkRequestBuilder<CommunityNotificationWorker>()
                        .setConstraints(
                            Constraints.Builder()
                                .setRequiredNetworkType(NetworkType.CONNECTED)
                                .build()
                        )
                        .setInputData(Data.Builder().putString("profile", profile).build())
                        .setInitialDelay(1, TimeUnit.MINUTES)
                        .build()
                    WorkManager.getInstance(applicationContext).enqueueUniqueWork(
                        COC_LIO_NOTIFICATION_WORK + "_drain_" + System.currentTimeMillis(),
                        ExistingWorkPolicy.KEEP,
                        drainRequest
                    )
                }
                Result.success()
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                Result.retry()
            }
        }
    }
}

private data class NotificationDocument(
    val document: DocumentSnapshot,
    val timestamp: Timestamp
)

private fun notificationTimestamp(document: DocumentSnapshot): Timestamp? =
    document.getTimestamp("createdAt")

private fun compareNotificationTimestamps(first: Timestamp, second: Timestamp): Int {
    val seconds = first.seconds.compareTo(second.seconds)
    return if (seconds != 0) seconds else first.nanoseconds.compareTo(second.nanoseconds)
}

private data class NotificationScan(
    val firstRun: Boolean,
    val documents: List<NotificationDocument>,
    val hasMore: Boolean = false,
    val secondsKey: String = "",
    val nanosKey: String = "",
    val idsKey: String = "",
    val previousTimestamp: Timestamp? = null,
    val previousIds: Set<String> = emptySet()
)

private suspend fun readNotificationChanges(
    firestore: FirebaseFirestore,
    collectionName: String,
    preferences: android.content.SharedPreferences,
    profile: String,
    watermarkType: String,
    limit: Long
): NotificationScan {
    val collection = firestore.collection(collectionName)
    val prefix = profile.hashCode().toUInt().toString(16) + "_" + watermarkType
    val secondsKey = prefix + "_seconds"
    val nanosKey = prefix + "_nanos"
    val idsKey = prefix + "_ids"

    if (!preferences.contains(secondsKey)) {
        val latest = collection.orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .awaitNotificationResult()
            .documents
            .firstOrNull()
        val latestTimestamp = latest?.let(::notificationTimestamp)
        if (latest == null || latestTimestamp == null) {
            preferences.edit()
                .putLong(secondsKey, Long.MIN_VALUE)
                .putInt(nanosKey, 0)
                .putStringSet(idsKey, mutableSetOf())
                .apply()
            return NotificationScan(true, emptyList())
        }
        val existingAtLatestTime = collection.whereEqualTo("createdAt", latestTimestamp)
            .get()
            .awaitNotificationResult()
            .documents
            .mapNotNull { document ->
                notificationTimestamp(document)?.let { NotificationDocument(document, it) }
            }
        saveNotificationWatermark(
            preferences,
            secondsKey,
            nanosKey,
            idsKey,
            existingAtLatestTime.ifEmpty { listOf(NotificationDocument(latest, latestTimestamp)) }
        )
        return NotificationScan(true, listOf(NotificationDocument(latest, latestTimestamp)))
    }

    val lastSeconds = preferences.getLong(secondsKey, Long.MIN_VALUE)
    val lastNanos = preferences.getInt(nanosKey, 0)
    val lastTimestamp = if (lastSeconds == Long.MIN_VALUE) null else Timestamp(lastSeconds, lastNanos)
    val lastIds = preferences.getStringSet(idsKey, mutableSetOf()).orEmpty()
    val newerDocuments = if (lastTimestamp == null) {
        collection.orderBy("createdAt", Query.Direction.ASCENDING)
            .limit(limit)
            .get()
            .awaitNotificationResult()
            .documents
    } else {
        collection.whereGreaterThan("createdAt", lastTimestamp)
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .limit(limit)
            .get()
            .awaitNotificationResult()
            .documents
    }
    val sameTimestampDocuments = if (lastTimestamp == null) {
        emptyList()
    } else {
        collection.whereEqualTo("createdAt", lastTimestamp)
            .get()
            .awaitNotificationResult()
            .documents
            .filter { it.id !in lastIds }
    }
    val changes = (newerDocuments + sameTimestampDocuments)
        .distinctBy { it.id }
        .mapNotNull { document ->
            notificationTimestamp(document)?.let { NotificationDocument(document, it) }
        }
        .sortedWith(
            compareBy<NotificationDocument> { it.timestamp.seconds }
                .thenBy { it.timestamp.nanoseconds }
                .thenBy { it.document.id }
        )

    val pendingChanges = changes.take(MAX_NOTIFICATION_DELIVERIES_PER_SCAN)
    if (pendingChanges.isEmpty() && lastTimestamp == null) {
        preferences.edit()
            .putLong(secondsKey, Long.MIN_VALUE)
            .putInt(nanosKey, 0)
            .putStringSet(idsKey, mutableSetOf())
            .apply()
    }
    return NotificationScan(
        firstRun = false,
        documents = pendingChanges,
        hasMore = changes.size > pendingChanges.size,
        secondsKey = secondsKey,
        nanosKey = nanosKey,
        idsKey = idsKey,
        previousTimestamp = lastTimestamp,
        previousIds = lastIds
    )
}

private suspend fun checkCommunityMessages(
    context: Context,
    firestore: FirebaseFirestore,
    preferences: android.content.SharedPreferences,
    profile: String,
    currentUid: String
): Boolean {
    val scan = readNotificationChanges(
        firestore,
        "community_messages",
        preferences,
        profile,
        "community",
        50L
    )
    if (scan.firstRun) return false

    scan.documents.forEach { item ->
        val document = item.document
        val senderUid = document.getString("uid").orEmpty()
        if (senderUid == currentUid) return@forEach
        val senderIsAdmin = document.getBoolean("senderIsAdmin") == true
        val sender = if (senderIsAdmin) "AILIO"
        else document.getString("displayName").orEmpty().ifBlank { AppLanguageSettings.translate(context, "Thành viên") }
        val senderPhotoUrl = if (senderIsAdmin) "" else document.getString("photoUrl").orEmpty()
        val type = document.getString("type").orEmpty()
        val messageText = when (type) {
            "text" -> document.getString("text").orEmpty().trim()
            "json" -> AppLanguageSettings.translate(context, "Đã chia sẻ dữ liệu JSON.")
            "image" -> AppLanguageSettings.translate(context, "Đã gửi một hình ảnh.")
            "file" -> AppLanguageSettings.translate(context, "Đã gửi một tệp.")
            else -> ""
        }
        if (messageText.isNotBlank()) {
            postCommunityChatBubbleNotification(
                context = context,
                notificationId = ("community_" + document.id).hashCode(),
                senderUid = senderUid,
                senderName = sender,
                messageText = messageText,
                senderIsAdmin = senderIsAdmin,
                senderPhotoUrl = senderPhotoUrl
            )
        }
    }
    commitNotificationScan(preferences, scan)
    return scan.hasMore
}

private suspend fun checkAppUpdates(
    context: Context,
    firestore: FirebaseFirestore,
    preferences: android.content.SharedPreferences,
    profile: String
): Boolean {
    val scan = readNotificationChanges(
        firestore,
        "app_updates",
        preferences,
        profile,
        "updates",
        20L
    )
    if (scan.firstRun) return false

    val installedVersion = runCatching {
        applicationVersionName(context)
    }.getOrDefault("")

    scan.documents.forEach { item ->
        val document = item.document
        if (document.getBoolean("notifyUsers") == false) return@forEach
        val versionName = document.getString("versionName").orEmpty().trim()
        if (versionName.isBlank() || !isNewerAppVersion(versionName, installedVersion)) return@forEach
        val notes = document.getString("notes").orEmpty().trim()
        val body = (if (notes.isBlank()) {
            AppLanguageSettings.translate(context, "COC-LIO có bản cập nhật mới")
        } else {
            "COC-LIO $versionName: $notes"
        }).take(420)
        postUpdateNotification(
            context,
            ("update_" + document.id).hashCode(),
            AppLanguageSettings.translate(context, "COC-LIO có bản cập nhật mới"),
            body
        )
    }
    commitNotificationScan(preferences, scan)
    return scan.hasMore
}

private fun commitNotificationScan(
    preferences: android.content.SharedPreferences,
    scan: NotificationScan
) {
    if (scan.documents.isEmpty()) return
    saveNotificationWatermark(
        preferences,
        scan.secondsKey,
        scan.nanosKey,
        scan.idsKey,
        scan.documents,
        scan.previousTimestamp,
        scan.previousIds
    )
}

private fun saveNotificationWatermark(
    preferences: android.content.SharedPreferences,
    secondsKey: String,
    nanosKey: String,
    idsKey: String,
    documents: List<NotificationDocument>,
    previousTimestamp: Timestamp? = null,
    previousIds: Set<String> = emptySet()
) {
    val newest = documents.maxWithOrNull(
        compareBy<NotificationDocument> { it.timestamp.seconds }
            .thenBy { it.timestamp.nanoseconds }
    ) ?: return
    val comparison = previousTimestamp?.let { compareNotificationTimestamps(newest.timestamp, it) } ?: 1
    val latestIds = if (comparison == 0) {
        previousIds + documents
            .filter { compareNotificationTimestamps(it.timestamp, newest.timestamp) == 0 }
            .map { it.document.id }
    } else {
        documents
            .filter { compareNotificationTimestamps(it.timestamp, newest.timestamp) == 0 }
            .map { it.document.id }
            .toSet()
    }
    preferences.edit()
        .putLong(secondsKey, newest.timestamp.seconds)
        .putInt(nanosKey, newest.timestamp.nanoseconds)
        .putStringSet(idsKey, latestIds)
        .apply()
}

private fun applicationVersionName(context: Context): String =
    context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()

private fun isNewerAppVersion(candidate: String, installed: String): Boolean {
    if (candidate.equals(installed, ignoreCase = true)) return false
    val numberPattern = Regex("^\\d+(?:\\.\\d+)*$")
    if (!numberPattern.matches(candidate) || !numberPattern.matches(installed)) return true
    val candidateParts = candidate.split('.').mapNotNull(String::toLongOrNull)
    val installedParts = installed.split('.').mapNotNull(String::toLongOrNull)
    val length = maxOf(candidateParts.size, installedParts.size)
    for (index in 0 until length) {
        val comparison = (candidateParts.getOrElse(index) { 0L })
            .compareTo(installedParts.getOrElse(index) { 0L })
        if (comparison != 0) return comparison > 0
    }
    return false
}

private fun postUpdateNotification(
    context: Context,
    notificationId: Int,
    title: String,
    body: String
) {
    postLocalCommunityNotification(
        context,
        COC_LIO_NOTIFICATION_CHANNEL_UPDATES,
        AppLanguageSettings.translate(context, "Bản cập nhật COC-LIO"),
        notificationId,
        title,
        body
    )
}

private fun postLocalCommunityNotification(
    context: Context,
    channelId: String,
    channelName: String,
    notificationId: Int,
    title: String,
    body: String
) {
    val notificationManager = NotificationManagerCompat.from(context)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
    ) {
        return
    }
    if (!notificationManager.areNotificationsEnabled()) return

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val systemManager = context.getSystemService(NotificationManager::class.java)
        systemManager.createNotificationChannel(
            NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH)
        )
    }

    val openIntent = Intent(context, MainActivity::class.java)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
    val contentIntent = PendingIntent.getActivity(
        context,
        notificationId,
        openIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    val notification = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentTitle(title)
        .setContentText(body)
        .setStyle(NotificationCompat.BigTextStyle().bigText(body))
        .setContentIntent(contentIntent)
        .setAutoCancel(true)
        .setCategory(NotificationCompat.CATEGORY_MESSAGE)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .build()
    runCatching { notificationManager.notify(notificationId, notification) }
}

private suspend fun <T> Task<T>.awaitNotificationResult(): T =
    suspendCancellableCoroutine { continuation ->
        addOnSuccessListener { value ->
            if (continuation.isActive) continuation.resume(value)
        }
        addOnFailureListener { error ->
            if (continuation.isActive) continuation.resumeWithException(error)
        }
        addOnCanceledListener { continuation.cancel() }
    }
