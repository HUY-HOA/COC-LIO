package com.huyhoa.coclio

import android.app.NotificationManager
import android.content.Context
import android.content.pm.ShortcutManager
import android.os.Build
import androidx.work.CoroutineWorker
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.firebase.messaging.FirebaseMessaging

private const val COC_LIO_NOTIFICATION_WORK = "coc_lio_background_notification_poll"
private const val COC_LIO_COMMUNITY_CHANNEL = "coc_lio_community_messages"
private const val COC_LIO_COMMUNITY_SHORTCUT = "coc_lio_community_conversation"

object CommunityNotificationCleanup {
    fun disable(context: Context) {
        val workManager = WorkManager.getInstance(context)
        workManager.cancelUniqueWork(COC_LIO_NOTIFICATION_WORK)
        workManager.cancelUniqueWork(COC_LIO_NOTIFICATION_WORK + "_initial")
        runCatching {
            FirebaseMessaging.getInstance().setAutoInitEnabled(false)
            FirebaseMessaging.getInstance().unsubscribeFromTopic("coc_lio_community")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            runCatching {
                context.getSystemService(NotificationManager::class.java)
                    .deleteNotificationChannel(COC_LIO_COMMUNITY_CHANNEL)
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            runCatching {
                context.getSystemService(ShortcutManager::class.java)
                    .removeDynamicShortcuts(listOf(COC_LIO_COMMUNITY_SHORTCUT))
            }
        }
    }
}

class CommunityNotificationWorker(
    context: Context,
    parameters: WorkerParameters
) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result = Result.success()
}
