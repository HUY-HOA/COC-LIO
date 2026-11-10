package com.huyhoa.coclio

import android.app.Activity
import android.os.Bundle
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
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

private class PlayerHistory(context: Context) : SQLiteOpenHelper(context, "coc_lio.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE player_history (tag TEXT PRIMARY KEY, name TEXT NOT NULL, town_hall INTEGER NOT NULL, trophies INTEGER NOT NULL, clan TEXT NOT NULL, checked_at INTEGER NOT NULL)")
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}
    fun save(tag: String, name: String, townHall: Int, trophies: Int, clan: String) {
        writableDatabase.execSQL(
            "INSERT OR REPLACE INTO player_history(tag,name,town_hall,trophies,clan,checked_at) VALUES(?,?,?,?,?,strftime('%s','now'))",
            arrayOf(tag, name, townHall, trophies, clan)
        )
    }
    fun latest(): List<String> {
        val items = mutableListOf<String>()
        readableDatabase.rawQuery("SELECT tag,name,town_hall,trophies,clan FROM player_history ORDER BY checked_at DESC LIMIT 20", null).use { c ->
            while (c.moveToNext()) {
                items.add("${c.getString(1)}  •  TH${c.getInt(2)}  •  ${c.getInt(3)} cúp\n${c.getString(0)}  •  ${c.getString(4)}")
            }
        }
        return items
    }
}

class MainActivity : Activity() {
    private lateinit var apiKeyInput: EditText
    private lateinit var tagInput: EditText
    private lateinit var resultView: TextView
    private lateinit var historyView: TextView
    private lateinit var searchButton: Button
    private val history by lazy { PlayerHistory(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildScreen()
        refreshHistory()
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
            hint = "Dán Developer API Key"
            textSize = 14f
            setSingleLine(true)
            setTextColor(Color.WHITE)
            setHintTextColor(Color.rgb(148, 163, 184))
            setBackgroundColor(Color.rgb(30, 41, 59))
            setPadding(dp(12), 0, dp(12), 0)
            inputType = 129
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
        content.addView(label("LỊCH SỬ TRA CỨU (20 TÀI KHOẢN GẦN NHẤT)"))
        historyView = TextView(this).apply {
            textSize = 13f
            setTextColor(Color.rgb(203, 213, 225))
            setPadding(dp(16), dp(14), dp(16), dp(14))
            setBackgroundColor(Color.rgb(30, 41, 59))
        }
        content.addView(historyView)
        content.addView(TextView(this).apply {
            text = "API key chỉ dùng trong phiên nhập hiện tại, không lưu vào lịch sử. Dữ liệu hồ sơ được lưu cục bộ trên thiết bị. Supercell yêu cầu Developer API Key và IP thiết bị nằm trong danh sách cho phép."
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
        setPadding(0, dp(16), 0, dp(7))
        typeface = Typeface.DEFAULT_BOLD
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
        if (!tag.matches(Regex("[0289PYLQGRJCUV]{3,15}"))) {
            resultView.text = "Player Tag không hợp lệ. Hãy kiểm tra lại ID làng."
            return
        }
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
                val body = stream?.bufferedReader()?.use { it.readText() } ?: ""
                connection.disconnect()
                if (status !in 200..299) {
                    val message = try {
                        JSONObject(body).optString("message", "Lỗi API ($status)")
                    } catch (_: Exception) { "Lỗi API ($status)" }
                    throw Exception(when (status) {
                        401 -> "API Key không hợp lệ hoặc hết hiệu lực."
                        403 -> "IP hiện tại chưa được cho phép trong Supercell Developer Portal."
                        404 -> "Không tìm thấy người chơi với ID này."
                        429 -> "Gọi API quá nhanh. Vui lòng thử lại sau."
                        else -> message
                    })
                }
                val player = JSONObject(body)
                val clanName = player.optJSONObject("clan")?.optString("name") ?: "Không tham gia"
                history.save(
                    player.optString("tag", "#$tag"),
                    player.optString("name", "Unknown"),
                    player.optInt("townHallLevel", 0),
                    player.optInt("trophies", 0),
                    clanName
                )
                val display = formatPlayer(player)
                runOnUiThread {
                    resultView.text = display
                    refreshHistory()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    resultView.text = "Không thể tra cứu: ${e.message ?: "Lỗi kết nối"}"
                }
            } finally {
                runOnUiThread { searchButton.isEnabled = true }
            }
        }
    }

    private fun refreshHistory() {
        val items = history.latest()
        historyView.text = if (items.isEmpty()) "Chưa có tài khoản nào được tra cứu." else items.joinToString("\n\n")
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
