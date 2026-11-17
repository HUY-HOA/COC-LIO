package com.huyhoa.coclio

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import android.widget.Toast

object FloatingJsonOverlayController {
    private const val REQUEST_OVERLAY_PERMISSION = 7814
    private const val PREFS = "coc_lio_floating_json"
    private const val PENDING_GAME_OPEN = "pending_game_open"

    fun openGame(context: Context, gameIntent: Intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(PENDING_GAME_OPEN, true)
                .apply()

            val permissionIntent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            )
            Toast.makeText(
                context,
                "Hãy bật quyền Hiển thị trên ứng dụng khác để dùng cửa sổ nhập JSON khi chơi game.",
                Toast.LENGTH_LONG
            ).show()
            if (context is Activity) {
                context.startActivityForResult(permissionIntent, REQUEST_OVERLAY_PERMISSION)
            } else {
                permissionIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(permissionIntent)
            }
            return
        }
        startOverlayAndGame(context, gameIntent)
    }

    fun onHostResume(activity: Activity) {
        val preferences = activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!preferences.getBoolean(PENDING_GAME_OPEN, false)) return

        preferences.edit().remove(PENDING_GAME_OPEN).apply()
        val allowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
            Settings.canDrawOverlays(activity)
        if (!allowed) {
            Toast.makeText(
                activity,
                "Chưa cấp quyền cửa sổ nổi. Clash of Clans vẫn sẽ được mở.",
                Toast.LENGTH_LONG
            ).show()
        }

        val gameIntent = activity.packageManager
            .getLaunchIntentForPackage("com.supercell.clashofclans")
        if (gameIntent == null) {
            activity.startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=com.supercell.clashofclans")
                )
            )
            return
        }
        if (allowed) startOverlayService(activity)
        activity.startActivity(gameIntent)
    }

    private fun startOverlayAndGame(context: Context, gameIntent: Intent) {
        startOverlayService(context)
        val launch = Intent(gameIntent)
        if (context !is Activity) launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(launch)
    }

    private fun startOverlayService(context: Context) {
        runCatching {
            ContextCompat.startForegroundService(
                context,
                Intent(context, FloatingJsonOverlayService::class.java)
                    .setAction(FloatingJsonOverlayService.ACTION_SHOW)
            )
        }.onFailure {
            Toast.makeText(
                context,
                "Không thể mở cửa sổ nhập JSON: ${it.message.orEmpty()}",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}
