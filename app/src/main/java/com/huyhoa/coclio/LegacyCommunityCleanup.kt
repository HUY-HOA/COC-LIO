package com.huyhoa.coclio

import android.app.NotificationManager
import android.content.Context
import android.content.pm.ShortcutManager
import android.os.Build
import androidx.work.WorkManager
import com.google.firebase.messaging.FirebaseMessaging

private const val LEGACY_COMMUNITY_NOTIFICATION_WORK = "coc_lio_background_notification_poll"
private const val LEGACY_COMMUNITY_CHANNEL = "coc_lio_community_messages"
private const val LEGACY_COMMUNITY_SHORTCUT = "coc_lio_community_conversation"

object LegacyCommunityCleanup {
    fun disable(context: Context) {
        val workManager = WorkManager.getInstance(context)
        workManager.cancelUniqueWork(LEGACY_COMMUNITY_NOTIFICATION_WORK)
        workManager.cancelUniqueWork(LEGACY_COMMUNITY_NOTIFICATION_WORK + "_initial")
        runCatching {
            FirebaseMessaging.getInstance().setAutoInitEnabled(false)
            FirebaseMessaging.getInstance().unsubscribeFromTopic("coc_lio_community")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            runCatching {
                context.getSystemService(NotificationManager::class.java)
                    .deleteNotificationChannel(LEGACY_COMMUNITY_CHANNEL)
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            runCatching {
                context.getSystemService(ShortcutManager::class.java)
                    .removeDynamicShortcuts(listOf(LEGACY_COMMUNITY_SHORTCUT))
            }
        }
    }
}
