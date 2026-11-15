package com.huyhoa.coclio

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.firebase.messaging.FirebaseMessaging

private const val COC_LIO_NOTIFICATION_WORK = "coc_lio_background_notification_poll"

object CommunityNotificationCleanup {
    fun disable(context: Context) {
        val workManager = WorkManager.getInstance(context)
        workManager.cancelUniqueWork(COC_LIO_NOTIFICATION_WORK)
        workManager.cancelUniqueWork(COC_LIO_NOTIFICATION_WORK + "_initial")
        runCatching {
            FirebaseMessaging.getInstance().setAutoInitEnabled(false)
            FirebaseMessaging.getInstance().unsubscribeFromTopic("coc_lio_community")
        }
    }
}

class CommunityNotificationWorker(
    context: Context,
    parameters: WorkerParameters
) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result = Result.success()
}
