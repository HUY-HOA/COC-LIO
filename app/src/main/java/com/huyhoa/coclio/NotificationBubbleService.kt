package com.huyhoa.coclio

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import kotlin.math.abs

class NotificationBubbleService : Service() {
    companion object {
        const val ACTION_ENABLE = "com.huyhoa.coclio.NOTIFICATION_BUBBLE_ENABLE"
        const val ACTION_SHOW_ALERT = "com.huyhoa.coclio.NOTIFICATION_BUBBLE_SHOW_ALERT"
        const val EXTRA_PROFILE = "profile"
        const val EXTRA_TITLE = "title"
        const val EXTRA_BODY = "body"
        private const val CHANNEL_ID = "coc_lio_notification_bubble"
        private const val FOREGROUND_NOTIFICATION_ID = 7816
        private const val PREFS = "coc_lio_notification_bubble"
    }

    private lateinit var windowManager: WindowManager
    private lateinit var windowParams: WindowManager.LayoutParams
    private val handler = Handler(Looper.getMainLooper())
    private var bubbleView: TextView? = null
    private var panelView: LinearLayout? = null
    private var activeView: View? = null
    private var expanded = false
    private var savedBubbleX = 0
    private var savedBubbleY = 0
    private var currentTitle = "Bong bóng thông báo đang bật"
    private var currentBody = "COC-LIO sẽ hiển thị nhắc nâng cấp tại đây."
    private var receiverRegistered = false

    private val alertReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != ACTION_SHOW_ALERT) return
            val title = intent.getStringExtra(EXTRA_TITLE).orEmpty()
            val body = intent.getStringExtra(EXTRA_BODY).orEmpty()
            showAlert(
                title.ifBlank { "Đến giờ hoàn thành" },
                body.ifBlank { "Một bộ đếm nâng cấp đã kết thúc." }
            )
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val savedAlert = getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        currentTitle = savedAlert.getString("last_title", currentTitle) ?: currentTitle
        currentBody = savedAlert.getString("last_body", currentBody) ?: currentBody
        ContextCompat.registerReceiver(
            this,
            alertReceiver,
            IntentFilter(ACTION_SHOW_ALERT),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        receiverRegistered = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !android.provider.Settings.canDrawOverlays(this)) {
            stopSelf()
            return START_NOT_STICKY
        }
        promoteToForeground()
        if (intent?.action == ACTION_SHOW_ALERT) {
            showAlert(
                intent.getStringExtra(EXTRA_TITLE).orEmpty(),
                intent.getStringExtra(EXTRA_BODY).orEmpty()
            )
        }
        return START_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        if (receiverRegistered) {
            runCatching { unregisterReceiver(alertReceiver) }
            receiverRegistered = false
        }
        activeView?.let { runCatching { windowManager.removeViewImmediate(it) } }
        activeView = null
        bubbleView = null
        panelView = null
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    private fun promoteToForeground() {
        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "Bong bóng thông báo COC-LIO",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Duy trì bong bóng nhắc nâng cấp do người dùng chủ động bật."
                    setShowBadge(false)
                }
            )
        }
        val openApp = PendingIntent.getActivity(
            this,
            FOREGROUND_NOTIFICATION_ID,
            Intent(this, MainActivity::class.java).addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            ),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("COC-LIO • Bong bóng thông báo")
            .setContentText("Bong bóng sẽ chỉ xuất hiện khi nâng cấp hoàn thành.")
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(openApp)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
        if (Build.VERSION.SDK_INT >= 34) {
            ServiceCompat.startForeground(
                this,
                FOREGROUND_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            ServiceCompat.startForeground(this, FOREGROUND_NOTIFICATION_ID, notification, 0)
        }
    }

    private fun ensureBubble() {
        if (activeView != null) return
        val preferences = getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val width = resources.displayMetrics.widthPixels
        val height = resources.displayMetrics.heightPixels
        val x = preferences.getInt("x", dp(12)).coerceIn(0, (width - dp(58)).coerceAtLeast(0))
        val y = preferences.getInt("y", dp(190)).coerceIn(0, (height - dp(90)).coerceAtLeast(0))
        windowParams = WindowManager.LayoutParams(
            dp(58),
            dp(58),
            overlayWindowType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            this.x = x
            this.y = y
        }
        val bubble = TextView(this).apply {
            text = "COC"
            textSize = 13f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setTextColor(Color.rgb(54, 244, 255))
            gravity = Gravity.CENTER
            background = rounded(Color.rgb(13, 21, 43), Color.rgb(54, 244, 255), dp(2), dp(29))
            isClickable = true
            isFocusable = false
            setOnClickListener { togglePanel() }
        }
        bubbleView = bubble
        activeView = bubble
        attachDrag(bubble)
        runCatching { windowManager.addView(bubble, windowParams) }
            .onFailure {
                activeView = null
                bubbleView = null
                stopSelf()
            }
    }

    private fun showAlert(title: String, body: String) {
        currentTitle = title.take(180)
        currentBody = body.take(1800)
        getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString("last_title", currentTitle)
            .putString("last_body", currentBody)
            .apply()
        ensureBubble()
        if (expanded) updatePanelText()
    }

    private fun togglePanel() {
        if (expanded) collapsePanel() else showPanel()
    }

    private fun showPanel() {
        if (activeView == null) ensureBubble()
        if (expanded) {
            updatePanelText()
            return
        }
        val bubble = activeView ?: return
        savedBubbleX = windowParams.x
        savedBubbleY = windowParams.y
        runCatching { windowManager.removeView(bubble) }
        expanded = true

        val width = minOf(dp(292), (resources.displayMetrics.widthPixels - dp(16)).coerceAtLeast(dp(220)))
        windowParams.width = width
        windowParams.height = dp(190)
        windowParams.x = savedBubbleX.coerceIn(0, (resources.displayMetrics.widthPixels - width).coerceAtLeast(0))
        windowParams.y = savedBubbleY.coerceIn(0, (resources.displayMetrics.heightPixels - dp(210)).coerceAtLeast(0))
        val panel = buildPanel()
        panelView = panel
        activeView = panel
        runCatching { windowManager.addView(panel, windowParams) }
            .onFailure {
                expanded = false
                panelView = null
                activeView = null
                stopSelf()
            }
    }

    private fun buildPanel(): LinearLayout {
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(10), dp(12), dp(10))
            background = rounded(Color.rgb(13, 21, 43), Color.rgb(54, 244, 255), dp(2), dp(16))
        }
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val heading = TextView(this).apply {
            text = "COC-LIO • NHẮC NÂNG CẤP"
            textSize = 11f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setTextColor(Color.rgb(54, 244, 255))
            setPadding(dp(3), dp(5), dp(5), dp(7))
        }
        header.addView(heading, LinearLayout.LayoutParams(0, dp(34), 1f))
        val close = TextView(this).apply {
            text = "×"
            textSize = 21f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            background = rounded(Color.rgb(112, 38, 67), Color.TRANSPARENT, 0, dp(8))
            isClickable = true
            setOnClickListener { dismissBubble() }
        }
        header.addView(close, LinearLayout.LayoutParams(dp(34), dp(32)))
        attachDrag(heading)
        panel.addView(header)

        val title = TextView(this).apply {
            textSize = 13f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            maxLines = 2
            setPadding(dp(3), dp(5), dp(3), dp(4))
        }
        title.tag = "bubble_title"
        panel.addView(title)

        val body = TextView(this).apply {
            textSize = 11f
            setTextColor(Color.rgb(190, 205, 226))
            maxLines = 5
            setPadding(dp(3), dp(2), dp(3), dp(5))
        }
        body.tag = "bubble_body"
        panel.addView(body, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            0,
            1f
        ))

        val open = TextView(this).apply {
            text = "MỞ COC-LIO"
            textSize = 10f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(4, 16, 29))
            background = rounded(Color.rgb(54, 244, 255), Color.TRANSPARENT, 0, dp(9))
            isClickable = true
            setOnClickListener {
                dismissBubble()
                startActivity(
                    Intent(this@NotificationBubbleService, MainActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                )
            }
        }
        panel.addView(open, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            dp(34)
        ))
        updatePanelText(panel)
        return panel
    }

    private fun updatePanelText(target: LinearLayout? = panelView) {
        target?.findViewWithTag<TextView>("bubble_title")?.text = currentTitle
        target?.findViewWithTag<TextView>("bubble_body")?.text = currentBody
    }

    private fun dismissBubble() {
        handler.removeCallbacksAndMessages(null)
        savePosition()
        activeView?.let { runCatching { windowManager.removeViewImmediate(it) } }
        activeView = null
        bubbleView = null
        panelView = null
        expanded = false
    }

    private fun collapsePanel() {
        if (!expanded) return
        val current = activeView ?: return
        runCatching { windowManager.removeView(current) }
        expanded = false
        panelView = null
        windowParams.width = dp(58)
        windowParams.height = dp(58)
        windowParams.x = savedBubbleX
        windowParams.y = savedBubbleY
        val bubble = bubbleView ?: TextView(this).apply {
            text = "COC"
            textSize = 13f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setTextColor(Color.rgb(54, 244, 255))
            gravity = Gravity.CENTER
            background = rounded(Color.rgb(13, 21, 43), Color.rgb(54, 244, 255), dp(2), dp(29))
            setOnClickListener { togglePanel() }
        }.also { bubbleView = it }
        activeView = bubble
        runCatching { windowManager.addView(bubble, windowParams) }
            .onFailure {
                activeView = null
                stopSelf()
            }
        savePosition()
    }

    private fun attachDrag(view: View) {
        var startX = 0
        var startY = 0
        var startRawX = 0f
        var startRawY = 0f
        var moved = false
        view.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    startX = windowParams.x
                    startY = windowParams.y
                    startRawX = event.rawX
                    startRawY = event.rawY
                    moved = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - startRawX
                    val dy = event.rawY - startRawY
                    if (abs(dx) > dp(4) || abs(dy) > dp(4)) moved = true
                    if (moved) {
                        windowParams.x = (startX + dx.toInt()).coerceIn(
                            0,
                            (resources.displayMetrics.widthPixels - windowParams.width).coerceAtLeast(0)
                        )
                        windowParams.y = (startY + dy.toInt()).coerceIn(
                            0,
                            (resources.displayMetrics.heightPixels - windowParams.height).coerceAtLeast(0)
                        )
                        activeView?.let { view ->
                            runCatching { windowManager.updateViewLayout(view, windowParams) }
                        }
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!moved) view.performClick()
                    savePosition()
                    true
                }
                MotionEvent.ACTION_CANCEL -> {
                    savePosition()
                    true
                }
                else -> true
            }
        }
    }

    private fun savePosition() {
        getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putInt("x", windowParams.x)
            .putInt("y", windowParams.y)
            .apply()
    }

    private fun overlayWindowType(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }

    private fun rounded(fill: Int, stroke: Int, strokeWidth: Int, radius: Int) =
        GradientDrawable().apply {
            setColor(fill)
            cornerRadius = radius.toFloat()
            if (strokeWidth > 0) setStroke(strokeWidth, stroke)
        }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt().coerceAtLeast(1)
}
