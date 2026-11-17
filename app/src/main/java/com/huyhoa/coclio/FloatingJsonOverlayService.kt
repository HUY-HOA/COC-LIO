package com.huyhoa.coclio

import android.app.Service
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.ServiceInfo
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.google.firebase.auth.FirebaseAuth
import com.huyhoa.coclio.data.VillageParser
import com.huyhoa.coclio.data.VillageSnapshot
import com.huyhoa.coclio.data.VillageStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FloatingJsonOverlayService : Service() {
    companion object {
        const val ACTION_SHOW = "com.huyhoa.coclio.FLOATING_JSON_SHOW"
        const val ACTION_CLOSE = "com.huyhoa.coclio.FLOATING_JSON_CLOSE"
        const val ACTION_JSON_IMPORTED = "com.huyhoa.coclio.FLOATING_JSON_IMPORTED"
        const val EXTRA_PROFILE = "profile"
        const val EXTRA_TAG = "tag"
        const val EXTRA_NAME = "village_name"
        private const val OVERLAY_PREFS = "coc_lio_floating_json"
        private const val FOREGROUND_CHANNEL_ID = "coc_lio_floating_json"
        private const val FOREGROUND_NOTIFICATION_ID = 7815
        private const val MIN_WIDTH_DP = 270
        private const val MIN_HEIGHT_DP = 350
        private const val MIN_LANDSCAPE_WIDTH_DP = 320
        private const val MIN_LANDSCAPE_HEIGHT_DP = 240
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var windowManager: WindowManager
    private lateinit var windowParams: WindowManager.LayoutParams
    private var panelView: View? = null
    private var bubbleView: View? = null
    private var activeView: View? = null
    private var jsonInput: EditText? = null
    private var villageNameInput: EditText? = null
    private var statusText: TextView? = null
    private var countText: TextView? = null
    private var accountText: TextView? = null
    private var saveButton: TextView? = null
    private var expandedWidth = 0
    private var expandedHeight = 0
    private var currentOrientationKey = "portrait"
    private var collapsed = false
    private var saving = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_CLOSE) {
            stopSelf()
            return START_NOT_STICKY
        }
        promoteToForeground()
        if (activeView != null && collapsed) {
            expandOverlay()
        } else {
            showOverlay()
        }
        return START_STICKY
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (!::windowManager.isInitialized || activeView == null) {
            currentOrientationKey = orientationKey(newConfig.orientation)
            return
        }

        val nextOrientationKey = orientationKey(newConfig.orientation)
        val orientationChanged = nextOrientationKey != currentOrientationKey
        if (orientationChanged) {
            if (!collapsed) {
                expandedWidth = windowParams.width
                expandedHeight = windowParams.height
                saveExpandedSize()
            }
            savePosition()
            currentOrientationKey = nextOrientationKey
        }

        val limits = overlayLimits(currentOrientationKey)
        if (orientationChanged) {
            val size = loadExpandedSize(currentOrientationKey, limits)
            expandedWidth = size.first
            expandedHeight = size.second
            val position = loadPosition(currentOrientationKey, expandedWidth)
            windowParams.x = position.first
            windowParams.y = position.second
        } else if (!collapsed) {
            expandedWidth = windowParams.width
            expandedHeight = windowParams.height
        }

        expandedWidth = expandedWidth.coerceIn(limits.minWidth, limits.maxWidth)
        expandedHeight = expandedHeight.coerceIn(limits.minHeight, limits.maxHeight)

        if (collapsed) {
            windowParams.width = dp(100)
            windowParams.height = dp(50)
        } else {
            windowParams.width = expandedWidth
            windowParams.height = expandedHeight
        }

        clampWindowPosition()
        updateWindow()
        if (!collapsed) saveExpandedSize()
        savePosition()
    }

    override fun onDestroy() {
        removeCurrentView()
        serviceScope.cancel()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    private fun promoteToForeground() {
        val notificationManager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(
                NotificationChannel(
                    FOREGROUND_CHANNEL_ID,
                    "Cửa sổ nhập JSON",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Giữ cửa sổ nhập JSON nổi hoạt động khi Clash of Clans đang mở."
                    setShowBadge(false)
                }
            )
        }

        val notification = NotificationCompat.Builder(this, FOREGROUND_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_edit)
            .setContentTitle("COC-LIO • Cửa sổ nhập JSON")
            .setContentText("Cửa sổ nổi đang sẵn sàng khi bạn chơi Clash of Clans.")
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
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
            ServiceCompat.startForeground(
                this,
                FOREGROUND_NOTIFICATION_ID,
                notification,
                0
            )
        }
    }

    private fun showOverlay() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !android.provider.Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }
        if (activeView != null) {
            if (collapsed) expandOverlay()
            refreshAccountDetails()
            return
        }

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val metrics = resources.displayMetrics
        currentOrientationKey = orientationKey()
        val limits = overlayLimits(currentOrientationKey)
        val size = loadExpandedSize(currentOrientationKey, limits)
        expandedWidth = size.first
        expandedHeight = size.second
        val position = loadPosition(currentOrientationKey, expandedWidth)
        val x = position.first.coerceIn(0, (metrics.widthPixels - expandedWidth).coerceAtLeast(0))
        val y = position.second.coerceIn(0, (metrics.heightPixels - dp(48) - expandedHeight).coerceAtLeast(0))

        val windowType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }
        windowParams = WindowManager.LayoutParams(
            expandedWidth,
            expandedHeight,
            windowType,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            this.x = x
            this.y = y
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        }

        panelView = buildPanel()
        attachPanel(panelView!!)
        refreshAccountDetails()
    }

    private fun buildPanel(): View {
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(10), dp(9), dp(10), dp(7))
            background = rounded(Color.rgb(13, 21, 43), Color.rgb(54, 244, 255), dp(2), dp(17))
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val title = TextView(this).apply {
            text = "COC-LIO  •  NHẬP JSON"
            setTextColor(Color.rgb(54, 244, 255))
            textSize = 13f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setPadding(dp(4), dp(7), dp(4), dp(7))
        }
        header.addView(title, LinearLayout.LayoutParams(0, dp(38), 1f))
        header.addView(actionText("−", Color.rgb(35, 49, 77)) { minimizeOverlay() }, LinearLayout.LayoutParams(dp(38), dp(34)))
        header.addView(actionText("×", Color.rgb(112, 38, 67)) { stopSelf() }, LinearLayout.LayoutParams(dp(38), dp(34)))
        panel.addView(header)

        val account = TextView(this).apply {
            textSize = 9f
            setTextColor(Color.rgb(174, 192, 221))
            setPadding(dp(4), dp(2), dp(4), dp(8))
            maxLines = 2
        }
        accountText = account
        panel.addView(account)

        villageNameInput = EditText(this).apply {
            hint = "Tên làng / tài khoản game (không bắt buộc)"
            setTextColor(Color.WHITE)
            setHintTextColor(Color.rgb(135, 151, 179))
            textSize = 12f
            setSingleLine(true)
            imeOptions = EditorInfo.IME_ACTION_NEXT
            setPadding(dp(10), dp(6), dp(10), dp(6))
            background = rounded(Color.rgb(24, 37, 64), Color.rgb(101, 72, 255), dp(1), dp(9))
        }
        panel.addView(villageNameInput, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            dp(43)
        ).apply { bottomMargin = dp(7) })

        val tools = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        tools.addView(actionText("DÁN JSON", Color.rgb(0, 105, 120)) { pasteClipboard() },
            LinearLayout.LayoutParams(0, dp(35), 1f).apply { rightMargin = dp(5) })
        tools.addView(actionText("XÓA Ô", Color.rgb(52, 62, 84)) {
            jsonInput?.setText("")
            setStatus("Đã xóa nội dung ô nhập.")
        }, LinearLayout.LayoutParams(0, dp(35), 1f))
        panel.addView(tools, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            dp(35)
        ).apply { bottomMargin = dp(7) })

        jsonInput = EditText(this).apply {
            hint = "Dán JSON làng vừa xuất từ Clash of Clans vào đây…"
            setTextColor(Color.WHITE)
            setHintTextColor(Color.rgb(135, 151, 179))
            textSize = 11f
            gravity = Gravity.TOP or Gravity.START
            setPadding(dp(10), dp(9), dp(10), dp(9))
            inputType = android.text.InputType.TYPE_CLASS_TEXT or
                android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE or
                android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            imeOptions = EditorInfo.IME_ACTION_NONE
            background = rounded(Color.rgb(7, 14, 30), Color.rgb(101, 72, 255), dp(1), dp(9))
            isVerticalScrollBarEnabled = true
        }
        panel.addView(jsonInput, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            0,
            1f
        ).apply { bottomMargin = dp(7) })

        countText = TextView(this).apply {
            setTextColor(Color.rgb(174, 192, 221))
            textSize = 9f
            setPadding(dp(4), dp(2), dp(4), dp(5))
        }
        panel.addView(countText)

        val footer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        saveButton = actionText("PHÂN TÍCH VÀ LƯU JSON", Color.rgb(0, 105, 120)) { saveJson() }.apply {
            textSize = 10f
        }
        footer.addView(saveButton, LinearLayout.LayoutParams(0, dp(42), 1f).apply { rightMargin = dp(6) })
        val resize = TextView(this).apply {
            text = "↘"
            textSize = 20f
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(54, 244, 255))
            background = rounded(Color.rgb(35, 49, 77), Color.rgb(54, 244, 255), dp(1), dp(8))
        }
        attachResize(resize)
        footer.addView(resize, LinearLayout.LayoutParams(dp(42), dp(42)))
        panel.addView(footer)

        statusText = TextView(this).apply {
            textSize = 9f
            setTextColor(Color.rgb(174, 192, 221))
            setPadding(dp(4), dp(5), dp(4), 0)
            maxLines = 2
        }
        statusText?.text = "Xoay màn hình để tự điều chỉnh • kéo góc ↘ để đổi kích thước."
        panel.addView(statusText)
        attachDrag(title)
        attachDrag(account)
        return panel
    }

    private fun actionText(label: String, backgroundColor: Int, action: () -> Unit): TextView =
        TextView(this).apply {
            text = label
            gravity = Gravity.CENTER
            textSize = 10f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            background = rounded(backgroundColor, Color.TRANSPARENT, 0, dp(8))
            isClickable = true
            isFocusable = true
            setOnClickListener { action() }
        }

    private fun rounded(fill: Int, stroke: Int, strokeWidth: Int, radius: Int) =
        GradientDrawable().apply {
            setColor(fill)
            cornerRadius = radius.toFloat()
            if (strokeWidth > 0) setStroke(strokeWidth, stroke)
        }

    private fun attachPanel(view: View) {
        try {
            activeView = view
            windowManager.addView(view, windowParams)
        } catch (error: Exception) {
            activeView = null
            Toast.makeText(this, "Không thể mở cửa sổ nổi: ${error.message.orEmpty()}", Toast.LENGTH_LONG).show()
            stopSelf()
        }
    }

    private fun minimizeOverlay() {
        val current = activeView ?: return
        if (collapsed) return
        saveExpandedSize()
        runCatching { windowManager.removeView(current) }
        collapsed = true
        val bubble = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            background = rounded(Color.rgb(13, 21, 43), Color.rgb(54, 244, 255), dp(2), dp(24))
        }
        val open = TextView(this).apply {
            text = "JSON"
            textSize = 12f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setTextColor(Color.rgb(54, 244, 255))
            gravity = Gravity.CENTER
            setPadding(dp(12), 0, dp(10), 0)
        }
        val close = actionText("×", Color.rgb(112, 38, 67)) { stopSelf() }
        bubble.addView(open, LinearLayout.LayoutParams(0, dp(48), 1f))
        bubble.addView(close, LinearLayout.LayoutParams(dp(38), dp(48)))
        attachDrag(open) { expandOverlay() }
        attachDrag(close) { stopSelf() }
        bubbleView = bubble
        windowParams.width = dp(100)
        windowParams.height = dp(50)
        activeView = bubble
        runCatching { windowManager.addView(bubble, windowParams) }
            .onFailure {
                activeView = null
                stopSelf()
            }
        savePosition()
    }

    private fun expandOverlay() {
        val current = activeView ?: return
        if (!collapsed) return
        runCatching { windowManager.removeView(current) }
        collapsed = false
        windowParams.width = expandedWidth
        windowParams.height = expandedHeight
        clampWindowPosition()
        val panel = panelView ?: buildPanel().also { panelView = it }
        activeView = panel
        runCatching { windowManager.addView(panel, windowParams) }
            .onFailure {
                activeView = null
                stopSelf()
            }
        refreshAccountDetails()
    }

    private fun attachDrag(view: View, onTap: (() -> Unit)? = null) {
        var startX = 0
        var startY = 0
        var startTouchX = 0f
        var startTouchY = 0f
        var moved = false
        view.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    startX = windowParams.x
                    startY = windowParams.y
                    startTouchX = event.rawX
                    startTouchY = event.rawY
                    moved = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - startTouchX
                    val dy = event.rawY - startTouchY
                    if (kotlin.math.abs(dx) > dp(4) || kotlin.math.abs(dy) > dp(4)) moved = true
                    if (moved) {
                        windowParams.x = (startX + dx.toInt()).coerceAtLeast(0)
                        windowParams.y = (startY + dy.toInt()).coerceAtLeast(0)
                        clampWindowPosition()
                        updateWindow()
                    }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (!moved) onTap?.invoke()
                    savePosition()
                    true
                }
                else -> true
            }
        }
    }

    private fun attachResize(view: View) {
        var startWidth = 0
        var startHeight = 0
        var startX = 0f
        var startY = 0f
        view.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    startWidth = windowParams.width
                    startHeight = windowParams.height
                    startX = event.rawX
                    startY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val limits = overlayLimits(currentOrientationKey)
                    windowParams.width = (startWidth + (event.rawX - startX).toInt())
                        .coerceIn(limits.minWidth, limits.maxWidth)
                    windowParams.height = (startHeight + (event.rawY - startY).toInt())
                        .coerceIn(limits.minHeight, limits.maxHeight)
                    expandedWidth = windowParams.width
                    expandedHeight = windowParams.height
                    clampWindowPosition()
                    updateWindow()
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    saveExpandedSize()
                    savePosition()
                    true
                }
                else -> true
            }
        }
    }

    private data class OverlayLimits(
        val maxWidth: Int,
        val maxHeight: Int,
        val minWidth: Int,
        val minHeight: Int
    )

    private fun orientationKey(orientation: Int = resources.configuration.orientation): String =
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) "landscape" else "portrait"

    private fun overlayLimits(orientation: String = currentOrientationKey): OverlayLimits {
        val metrics = resources.displayMetrics
        val isLandscape = orientation == "landscape"
        val maxWidth = (metrics.widthPixels - dp(16)).coerceAtLeast(dp(240))
        val maxHeight = (metrics.heightPixels - dp(48)).coerceAtLeast(dp(if (isLandscape) 220 else 300))
        val minWidth = dp(if (isLandscape) MIN_LANDSCAPE_WIDTH_DP else MIN_WIDTH_DP)
            .coerceAtMost(maxWidth)
        val minHeight = dp(if (isLandscape) MIN_LANDSCAPE_HEIGHT_DP else MIN_HEIGHT_DP)
            .coerceAtMost(maxHeight)
        return OverlayLimits(maxWidth, maxHeight, minWidth, minHeight)
    }

    private fun defaultExpandedSize(
        orientation: String,
        limits: OverlayLimits
    ): Pair<Int, Int> {
        val isLandscape = orientation == "landscape"
        val widthRatio = if (isLandscape) 0.88f else 0.94f
        val heightRatio = if (isLandscape) 0.98f else 0.72f
        val widthCap = dp(if (isLandscape) 900 else 460)
        val heightCap = dp(if (isLandscape) 420 else 680)
        val width = (limits.maxWidth * widthRatio).toInt()
            .coerceAtMost(widthCap)
            .coerceIn(limits.minWidth, limits.maxWidth)
        val height = (limits.maxHeight * heightRatio).toInt()
            .coerceAtMost(heightCap)
            .coerceIn(limits.minHeight, limits.maxHeight)
        return width to height
    }

    private fun loadExpandedSize(
        orientation: String,
        limits: OverlayLimits
    ): Pair<Int, Int> {
        val preferences = getSharedPreferences(OVERLAY_PREFS, Context.MODE_PRIVATE)
        val defaults = defaultExpandedSize(orientation, limits)
        val widthKey = "width_$orientation"
        val heightKey = "height_$orientation"
        val useLegacyPortraitSize = orientation == "portrait"
        val storedWidth = when {
            preferences.contains(widthKey) -> preferences.getInt(widthKey, defaults.first)
            useLegacyPortraitSize && preferences.contains("width") ->
                preferences.getInt("width", defaults.first)
            else -> defaults.first
        }
        val storedHeight = when {
            preferences.contains(heightKey) -> preferences.getInt(heightKey, defaults.second)
            useLegacyPortraitSize && preferences.contains("height") ->
                preferences.getInt("height", defaults.second)
            else -> defaults.second
        }
        return storedWidth.coerceIn(limits.minWidth, limits.maxWidth) to
            storedHeight.coerceIn(limits.minHeight, limits.maxHeight)
    }

    private fun loadPosition(
        orientation: String,
        panelWidth: Int
    ): Pair<Int, Int> {
        val preferences = getSharedPreferences(OVERLAY_PREFS, Context.MODE_PRIVATE)
        val xKey = "x_$orientation"
        val yKey = "y_$orientation"
        val useLegacyPortraitPosition = orientation == "portrait"
        val defaultX = ((resources.displayMetrics.widthPixels - panelWidth) / 2).coerceAtLeast(0)
        val defaultY = dp(if (orientation == "landscape") 16 else 72)
        val x = when {
            preferences.contains(xKey) -> preferences.getInt(xKey, defaultX)
            useLegacyPortraitPosition && preferences.contains("x") ->
                preferences.getInt("x", defaultX)
            else -> defaultX
        }
        val y = when {
            preferences.contains(yKey) -> preferences.getInt(yKey, defaultY)
            useLegacyPortraitPosition && preferences.contains("y") ->
                preferences.getInt("y", defaultY)
            else -> defaultY
        }
        return x to y
    }

    private fun clampWindowPosition() {
        val width = resources.displayMetrics.widthPixels
        val height = resources.displayMetrics.heightPixels
        windowParams.x = windowParams.x.coerceIn(0, (width - windowParams.width).coerceAtLeast(0))
        windowParams.y = windowParams.y.coerceIn(0, (height - dp(48) - windowParams.height).coerceAtLeast(0))
    }

    private fun updateWindow() {
        val view = activeView ?: return
        runCatching { windowManager.updateViewLayout(view, windowParams) }
    }

    private fun savePosition() {
        getSharedPreferences(OVERLAY_PREFS, Context.MODE_PRIVATE).edit()
            .putInt("x_$currentOrientationKey", windowParams.x)
            .putInt("y_$currentOrientationKey", windowParams.y)
            .putInt("x", windowParams.x)
            .putInt("y", windowParams.y)
            .apply()
    }

    private fun saveExpandedSize() {
        getSharedPreferences(OVERLAY_PREFS, Context.MODE_PRIVATE).edit()
            .putInt("width_$currentOrientationKey", expandedWidth)
            .putInt("height_$currentOrientationKey", expandedHeight)
            .putInt("width", expandedWidth)
            .putInt("height", expandedHeight)
            .apply()
    }

    private fun pasteClipboard() {
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as? ClipboardManager
        val text = runCatching {
            clipboard?.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(this)?.toString()
        }.getOrNull().orEmpty()
        if (text.isBlank()) {
            setStatus("Bộ nhớ tạm chưa có JSON.")
        } else {
            jsonInput?.setText(text)
            jsonInput?.setSelection(text.length)
            setStatus("Đã dán nội dung từ bộ nhớ tạm.")
        }
    }

    private fun refreshAccountDetails() {
        val profile = AccountProfileStore.active(this)
        val account = if (profile == "local") {
            "Đang lưu JSON trên thiết bị • Chưa đăng nhập Google"
        } else {
            val name = AccountProfileStore.preferredName(this, profile)
                ?: AccountProfileStore.googleData(this, profile)?.displayName
            "Đang lưu vào: ${name?.takeIf { it.isNotBlank() } ?: profile} • ${profile}"
        }
        accountText?.text = account
        val count = VillageStore.storedSnapshotCount(this, profile)
        countText?.text = "JSON đã lưu: $count/50  •  Mục Đã xóa vẫn chiếm chỗ"
    }

    private fun saveJson() {
        if (saving) return
        refreshAccountDetails()
        val raw = jsonInput?.text?.toString().orEmpty().trim()
        val customName = villageNameInput?.text?.toString()?.trim().orEmpty().take(40)
        if (raw.isBlank()) {
            setStatus("Hãy dán JSON trước khi lưu.")
            return
        }

        val profile = AccountProfileStore.active(this)
        if (profile != "local") {
            val signedInEmail = FirebaseAuth.getInstance().currentUser?.email
            if (signedInEmail.isNullOrBlank() || !signedInEmail.equals(profile, ignoreCase = true)) {
                setStatus("Tài khoản đã thay đổi. Hãy mở COC-LIO và chọn đúng tài khoản trước khi lưu.")
                return
            }
        }

        saving = true
        saveButton?.isEnabled = false
        saveButton?.alpha = 0.55f
        setStatus("Đang phân tích và lưu JSON…")

        serviceScope.launch {
            val result = runCatching {
                val parsed = VillageParser.parse(raw)
                val previousExists = VillageStore.loadAll(this@FloatingJsonOverlayService, profile)
                    .any { it.tag == parsed.tag }
                val saved = if (customName.isBlank()) parsed else parsed.copy(villageName = customName)
                val savedSuccessfully = VillageStore.save(
                    this@FloatingJsonOverlayService,
                    saved,
                    profile,
                    customName
                )
                check(savedSuccessfully) {
                    "Đã đạt giới hạn 50 JSON. Hãy xóa vĩnh viễn JSON cũ trước khi thêm bản mới."
                }
                val history = VillageStore.loadAll(this@FloatingJsonOverlayService, profile)
                UpgradeReminderScheduler.scheduleAll(
                    this@FloatingJsonOverlayService,
                    history,
                    profile,
                    AppPreferences.notificationsEnabled(this@FloatingJsonOverlayService, profile)
                )
                AppWidgetUpdater.updateAll(this@FloatingJsonOverlayService)
                FirebaseTelemetry.logVillageImport(this@FloatingJsonOverlayService, previousExists)
                Triple(saved, previousExists, VillageStore.storedSnapshotCount(this@FloatingJsonOverlayService, profile))
            }

            if (result.isSuccess) {
                val (snapshot, previousExists, count) = result.getOrThrow()
                val choice = try {
                    AccountPrivacyConsent.load()
                } catch (_: Exception) {
                    null
                }
                if (choice?.villageDataSync == true) {
                    try {
                        FirebaseVillageDataSync.saveLatest(snapshot)
                    } catch (_: Exception) {
                    }
                }
                sendBroadcast(
                    Intent(ACTION_JSON_IMPORTED)
                        .setPackage(packageName)
                        .putExtra(EXTRA_PROFILE, profile)
                        .putExtra(EXTRA_TAG, snapshot.tag)
                        .putExtra(EXTRA_NAME, snapshot.villageName.orEmpty())
                )
                withContext(Dispatchers.Main) {
                    saving = false
                    saveButton?.isEnabled = true
                    saveButton?.alpha = 1f
                    jsonInput?.setText("")
                    villageNameInput?.setText("")
                    refreshAccountDetails()
                    setStatus("Đã lưu ${snapshot.tag} • $count/50 JSON. ${if (previousExists) "Đã cập nhật làng này." else "Đã thêm làng mới."}")
                }
            } else {
                val error = result.exceptionOrNull()
                withContext(Dispatchers.Main) {
                    saving = false
                    saveButton?.isEnabled = true
                    saveButton?.alpha = 1f
                    setStatus(error?.message?.takeIf { it.isNotBlank() } ?: "Không thể phân tích JSON này.")
                }
            }
        }
    }

    private fun setStatus(message: String) {
        statusText?.text = message
    }

    private fun removeCurrentView() {
        activeView?.let { view ->
            runCatching { windowManager.removeViewImmediate(view) }
        }
        activeView = null
        panelView = null
        bubbleView = null
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt().coerceAtLeast(1)

}
