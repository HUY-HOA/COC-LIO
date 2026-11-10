package com.huyhoa.coclio

import android.app.Activity
import android.os.Bundle
import android.content.Context
import android.graphics.Typeface
import android.graphics.Color
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.concurrent.thread

class MainActivity : Activity() {
    private lateinit var apiKeyInput: EditText
    private lateinit var tagInput: EditText
    private lateinit var resultView: TextView
    private lateinit var searchButton: Button
    private val prefs by lazy { getSharedPreferences("coc_lio", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildScreen()
    }

    private fun buildScreen() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(28), dp(20), dp(20))
            setBackgroundColor(Color.rgb(15, 23, 42))
        }
        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        content.addView(TextView(this).apply {
            text = "COC LIO"
            textSize = 28f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
        })
        content.addView(TextView(this).apply {
            text = "TRA CỨU TÀI KHOẢN CLASH OF CLANS"
            textSize = 12f
            setTextColor(Color.rgb(148, 163, 184))
            setPadding(0, dp(4), 0, dp(24))
        })
        content.addView(label("Supercell Developer API Key"))
        apiKeyInput = EditText(this).apply {
            hint = "Dán API key của bạn"
            textSize = 14f
            setSingleLine(true)
            setTextColor(Color.WHITE)
            setHintTextColor(Color.rgb(148, 163, 184))
            setBackgroundColor(Color.rgb(30, 41, 59))
            setPadding(dp(12), 0, dp(12), 0)
            setText(prefs.getString("api_key", ""))
        }
        content.addView(apiKeyInput, fieldParams())
        content.addView(label("ID làng / Player Tag"))
        tagInput = EditText(this).apply {
            hint = "#ABC123XYZ"
            textSize = 16f
            setSingleLine(true)
            setTextColor(Color.WHITE)
            setHintTextColor(Color.rgb(148, 163, 184))
            setBackgroundColor(Color.rgb(30, 41, 59))
            setPadding(dp(12), 0, dp(12), 0)
        }
        content.addView(tagInput, fieldParams())
        searchButton = Button(this).apply {
            text = "TRA CỨU TÀI KHOẢN"
            isAllCaps = false
            textSize = 15f
            setOnClickListener { lookupPlayer() }
        }
        content.addView(searchButton, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(52)
        ).apply { topMargin = dp(16) })
        resultView = TextView(this).apply {
            text = "Thông tin tài khoản sẽ hiển thị tại đây."
            textSize = 14f
            setTextColor(Color.rgb(226, 232, 240))
            setPadding(dp(16), dp(16), dp(16), dp(16))
            setBackgroundColor(Color.rgb(30, 41, 59))
        }
        content.addView(resultView, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(20) })
        content.addView(TextView(this).apply {
            text = "Dữ liệu lấy trực tiếp từ API Supercell. API key được lưu trên thiết bị, không gửi đến máy chủ riêng."
            textSize = 12f
            setTextColor(Color.rgb(148, 163, 184))
            setPadding(0, dp(16), 0, 0)
        })
        scroll.addView(content)
        root.addView(scroll)
        setContentView(root)
    }

    private fun label(value: String) = TextView(this).apply {
        text = value
        textSize = 13f
        setTextColor(Color.rgb(203, 213, 225))
        setPadding(0, dp(12), 0, dp(7))
    }

    private fun fieldParams() = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT, dp(50)
    )

    private fun lookupPlayer() {
        val key = apiKeyInput.text.toString().trim()
        val tag = tagInput.text.toString().trim().removePrefix("#").replace(" ", "").uppercase()
        if (key.isBlank()) {
            resultView.text = "Vui lòng nhập Supercell Developer API Key."
            return
        }
        if (tag.isBlank()) {
            resultView.text = "Vui lòng nhập ID làng (Player Tag)."
            return
        }
        prefs.edit().putString("api_key", key).apply()
        (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
            .hideSoftInputFromWindow(tagInput.windowToken, 0)
        searchButton.isEnabled = false
        resultView.text = "Đang tra cứu..."
        thread {
            try {
                val encodedTag = URLEncoder.encode("#$tag", "UTF-8")
                val connection = (URL("https://api.clashofclans.com/v1/players/$encodedTag")
                    .openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 15000
                    readTimeout = 15000
                    setRequestProperty("Authorization", "Bearer $key")
                    setRequestProperty("Accept", "application/json")
                }
                val status = connection.responseCode
                val stream = if (status in 200..299) connection.inputStream else connection.errorStream
                val body = stream.bufferedReader().use { it.readText() }
                connection.disconnect()
                if (status !in 200..299) {
                    val message = try {
                        JSONObject(body).optString("message", "Lỗi API ($status)")
                    } catch (_: Exception) { "Lỗi API ($status)" }
                    throw Exception(message)
                }
                val display = formatPlayer(JSONObject(body))
                runOnUiThread { resultView.text = display }
            } catch (e: Exception) {
                runOnUiThread {
                    resultView.text = "Không thể tra cứu: ${e.message ?: "Lỗi kết nối"}"
                }
            } finally {
                runOnUiThread { searchButton.isEnabled = true }
            }
        }
    }

    private fun formatPlayer(p: JSONObject): String {
        val clan = p.optJSONObject("clan")
        val league = p.optJSONObject("league")
        return buildString {
            appendLine("HỒ SƠ NGƯỜI CHƠI")
            appendLine()
            appendLine("Tên: ${p.optString("name", "—")}")
            appendLine("ID làng: ${p.optString("tag", "—")}")
            appendLine("Nhà chính: ${p.optInt("townHallLevel", 0)}")
            appendLine("Cấp kinh nghiệm: ${p.optInt("expLevel", 0)}")
            appendLine("Cúp hiện tại: ${p.optInt("trophies", 0)}")
            appendLine("Cúp cao nhất: ${p.optInt("bestTrophies", 0)}")
            appendLine("Giải đấu: ${league?.optString("name") ?: "Chưa có"}")
            appendLine("Clan: ${clan?.optString("name") ?: "Không tham gia"}")
            if (clan != null) appendLine("ID Clan: ${clan.optString("tag", "—")}")
            appendLine()
            appendLine("THÀNH TÍCH")
            appendLine("Sao chiến tranh: ${p.optInt("warStars", 0)}")
            appendLine("Thắng tấn công: ${p.optInt("attackWins", 0)}")
            appendLine("Thắng phòng thủ: ${p.optInt("defenseWins", 0)}")
            appendLine("Lính đã quyên góp: ${p.optInt("donations", 0)}")
            appendLine("Lính đã nhận: ${p.optInt("donationsReceived", 0)}")
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
