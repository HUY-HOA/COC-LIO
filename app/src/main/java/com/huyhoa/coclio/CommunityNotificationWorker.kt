package com.huyhoa.coclio

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
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
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private const val COC_LIO_NOTIFICATION_WORK = "coc_lio_background_notification_poll"
private const val COC_LIO_NOTIFICATION_CHANNEL_COMMUNITY = "coc_lio_community_messages"
private const val COC_LIO_NOTIFICATION_CHANNEL_UPDATES = "coc_lio_app_updates"

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
        val profile = inputData.getString("profile").orEmpty()
        val user = FirebaseAuth.getInstance().currentUser
        if (profile.isBlank() || user?.email?.equals(profile, ignoreCase = true) != true) {
            return@withContext Result.success()
        }

        val communityEnabled = AppPreferences.communityNotificationsEnabled(applicationContext, profile)
        val updatesEnabled = AppPreferences.updateNotificationsEnabled(applicationContext, profile)
        if (!communityEnabled && !updatesEnabled) return@withContext Result.success()

        try {
            val preferences = applicationContext.getSharedPreferences(
                "coc_lio_notification_watermarks",
                Context.MODE_PRIVATE
            )
            val firestore = FirebaseFirestore.getInstance()
            if (communityEnabled) {
                checkCommunityMessages(firestore, preferences, profile, user.uid)
            }
            if (updatesEnabled) {
                checkAppUpdates(firestore, preferences, profile)
            }
            Result.success()
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            Result.retry()
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

private fun loadNotificationDocuments(
    firestore: FirebaseFirestore,
    collection: String
): List<NotificationDocument> {
    return emptyList()
}

private suspend fun checkCommunityMessages(
    firestore: FirebaseFirestore,
    preferences: android.content.SharedPreferences,
    profile: String,
    currentUid: String
) {
    val documents = firestore.collection("community_messages")
        .orderBy("createdAt", Query.Direction.DESCENDING)
        .orderBy(FieldPath.documentId(), Query.Direction.DESCENDING)
        .limit(50)
        .get()
        .awaitNotificationResult()
        .documents
        .mapNotNull { document ->
            notificationTimestamp(document)?.let { NotificationDocument(document, it) }
        }
    if (documents.isEmpty()) return

    val prefix = profile.hashCode().toUInt().toString(16) + "_community"
    val secondsKey = prefix + "_seconds"
    val nanosKey = prefix + "_nanos"
    val idsKey = prefix + "_ids"
    val hasCursor = preferences.contains(secondsKey)
    val lastSeconds = preferences.getLong(secondsKey, Long.MIN_VALUE)
    val lastNanos = preferences.getInt(nanosKey, 0)
    val lastIds = preferences.getStringSet(idsKey, emptySet()).orEmpty()

    if (hasCursor) {
        val fresh = documents.filter { item ->
            val timestampComparison = compareNotificationTimestamps(
                item.timestamp,
                Timestamp(lastSeconds, lastNanos)
            )
            timestampComparison > 0 ||
                (timestampComparison == 0 && item.document.id !in lastIds)
        }.sortedWith(
            compareBy<NotificationDocument> { it.timestamp.seconds }
                .thenBy { it.timestamp.nanoseconds }
                .thenBy { it.document.id }
        )

        fresh.takeLast(5).forEach { item ->
            val document = item.document
            val senderUid = document.getString("uid").orEmpty()
            if (senderUid == currentUid) return@forEach
            val sender = document.getString("displayName").orEmpty().ifBlank { "Thành viên" }
            val type = document.getString("type").orEmpty()
            val messageText = when (type) {
                "text" -> document.getString("text").orEmpty().trim()
                "json" -> "Đã chia sẻ dữ liệu JSON."
                else -> ""
            }
            if (messageText.isNotBlank()) {
                postCommunityNotification(
                    applicationContext = preferencesContext(firestore),
                    notificationId = ("community_" + document.id).hashCode(),
                    title = "Tin nhắn cộng đồng",
                    body = (sender + ": " + messageText).take(420)
                )
            }
        }
    }

    saveNotificationWatermark(preferences, secondsKey, nanosKey, idsKey, documents)
}

private suspend fun checkAppUpdates(
    firestore: FirebaseFirestore,
    preferences: android.content.SharedPreferences,
    profile: String
) {
    val documents = firestore.collection("app_updates")
        .orderBy("createdAt", Query.Direction.DESCENDING)
        .orderBy(FieldPath.documentId(), Query.Direction.DESCENDING)
        .limit(20)
        .get()
        .awaitNotificationResult()
        .documents
        .mapNotNull { document ->
            notificationTimestamp(document)?.let { NotificationDocument(document, it) }
        }
    if (documents.isEmpty()) return

    val prefix = profile.hashCode().toUInt().toString(16) + "_updates"
    val secondsKey = prefix + "_seconds"
    val nanosKey = prefix + "_nanos"
    val idsKey = prefix + "_ids"
    val hasCursor = preferences.contains(secondsKey)
    val lastSeconds = preferences.getLong(secondsKey, Long.MIN_VALUE)
    val lastNanos = preferences.getInt(nanosKey, 0)
    val lastIds = preferences.getStringSet(idsKey, emptySet()).orEmpty()

    val fresh = if (hasCursor) {
        documents.filter { item ->
            val timestampComparison = compareNotificationTimestamps(
                item.timestamp,
                Timestamp(lastSeconds, lastNanos)
            )
            timestampComparison > 0 ||
                (timestampComparison == 0 && item.document.id !in lastIds)
        }.sortedWith(
            compareBy<NotificationDocument> { it.timestamp.seconds }
                .thenBy { it.timestamp.nanoseconds }
                .thenBy { it.document.id }
        )
    } else {
        documents.take(1)
    }

    val installedVersion = runCatching {
        applicationVersionName(applicationContext)
    }.getOrDefault("")

    fresh.takeLast(5).forEach { item ->
        val document = item.document
        if (document.getBoolean("notifyUsers") == false) return@forEach
        val versionName = document.getString("versionName").orEmpty().trim()
        if (versionName.isBlank() || !isNewerAppVersion(versionName, installedVersion)) return@forEach
        val notes = document.getString("notes").orEmpty().trim()
        val body = ("COC-LIO " + versionName + if (notes.isBlank()) {
            " đã có bản cập nhật mới."
        } else {
            ": " + notes
        }).take(420)
        postUpdateNotification(
            applicationContext,
            ("update_" + document.id).hashCode(),
            "COC-LIO có bản cập nhật mới",
            body
        )
    }

    saveNotificationWatermark(preferences, secondsKey, nanosKey, idsKey, documents)
}

private fun saveNotificationWatermark(
    preferences: android.content.SharedPreferences,
    secondsKey: String,
    nanosKey: String,
    idsKey: String,
    documents: List<NotificationDocument>
) {
    val newest = documents.maxWithOrNull(
        compareBy<NotificationDocument> { it.timestamp.seconds }
            .thenBy { it.timestamp.nanoseconds }
    ) ?: return
    val latestIds = documents
        .filter { compareNotificationTimestamps(it.timestamp, newest.timestamp) == 0 }
        .map { it.document.id }
        .toSet()
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

private fun postCommunityNotification(
    applicationContext: Context,
    notificationId: Int,
    title: String,
    body: String
) {
    postLocalCommunityNotification(
        applicationContext,
        COC_LIO_NOTIFICATION_CHANNEL_COMMUNITY,
        "Tin nhắn cộng đồng",
        notificationId,
        title,
        body
    )
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
        "Bản cập nhật COC-LIO",
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
    if (!notificationManager.areNotificationsEnabled()) return

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val systemManager = context.getSystemService(NotificationManager::class.java)
        if (systemManager.getNotificationChannel(channelId) == null) {
            systemManager.createNotificationChannel(
                NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH)
            )
        }
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
