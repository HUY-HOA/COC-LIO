package com.huyhoa.coclio

import android.app.Service
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
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
        private const val DEFAULT_WIDTH_DP = 320
        private const val DEFAULT_HEIGHT_DP = 500
        private const val MIN_WIDTH_DP = 270
        private const val MIN_HEIGHT_DP = 350
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
    private var collapsed = false
    private var saving = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_CLOSE) {
            stopSelf()
            return START_NOT_STICKY
        }
        showOverlay()
        return START_STICKY
    }

    override fun onDestroy() {
        removeCurrentView()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun showOverlay() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !android.provider.Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }
        if (activeView != null) {
            refreshAccountDetails()
            return
        }

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val metrics = resources.displayMetrics
        val maxWidth = (metrics.widthPixels - dp(16)).coerceAtLeast(dp(240))
        val maxHeight = (metrics.heightPixels - dp(48)).coerceAtLeast(dp(300))
        val minWidth = MIN_WIDTH_DP.dpValue().coerceAtMost(maxWidth)
        val minHeight = MIN_HEIGHT_DP.dpValue().coerceAtMost(maxHeight)
        val preferences = getSharedPreferences(OVERLAY_PREFS, Context.MODE_PRIVATE)
        expandedWidth = preferences.getInt("width", DEFAULT_WIDTH_DP.dpValue()).coerceIn(minWidth, maxWidth)
        expandedHeight = preferences.getInt("height", DEFAULT_HEIGHT_DP.dpValue()).coerceIn(minHeight, maxHeight)
        val x = preferences.getInt("x", dp(8)).coerceIn(0, maxWidth - expandedWidth)
        val y = preferences.getInt("y", dp(92)).coerceIn(0, (maxHeight - expandedHeight).coerceAtLeast(0))

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
            singleLine = true
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
        statusText?.text = "Kéo tiêu đề để di chuyển • kéo góc ↘ để thu phóng."
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
                    val maxWidth = (resources.displayMetrics.widthPixels - dp(16)).coerceAtLeast(dp(MIN_WIDTH_DP))
                    val maxHeight = (resources.displayMetrics.heightPixels - dp(48)).coerceAtLeast(dp(MIN_HEIGHT_DP))
                    windowParams.width = (startWidth + (event.rawX - startX).toInt())
                        .coerceIn(dp(MIN_WIDTH_DP).coerceAtMost(maxWidth), maxWidth)
                    windowParams.height = (startHeight + (event.rawY - startY).toInt())
                        .coerceIn(dp(MIN_HEIGHT_DP).coerceAtMost(maxHeight), maxHeight)
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
            .putInt("x", windowParams.x)
            .putInt("y", windowParams.y)
            .apply()
    }

    private fun saveExpandedSize() {
        getSharedPreferences(OVERLAY_PREFS, Context.MODE_PRIVATE).edit()
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
            "Tài khoản COC-LIO: Chưa đăng nhập Google"
        } else {
            val name = AccountProfileStore.preferredName(this, profile)
                ?: AccountProfileStore.googleData(this, profile)?.displayName
            "Đang lưu vào: ${name?.takeIf { it.isNotBlank() } ?: profile} • ${profile}"
        }
        accountText?.text = account
        val count = VillageStore.storedSnapshotCount(this, profile)
        countText?.text = "JSON đã lưu: $count/50  •  Tính cả mục Đã xóa"
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
        if (profile == "local") {
            setStatus("Hãy đăng nhập Google trong COC-LIO trước khi lưu JSON.")
            return
        }
        val signedInEmail = FirebaseAuth.getInstance().currentUser?.email
        if (signedInEmail.isNullOrBlank() || !signedInEmail.equals(profile, ignoreCase = true)) {
            setStatus("Tài khoản đã thay đổi. Hãy mở COC-LIO và chọn đúng tài khoản trước khi lưu.")
            return
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
                VillageStore.save(this@FloatingJsonOverlayService, saved, profile, customName)
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

    private fun Int.dpValue(): Int = dp(this)
}
