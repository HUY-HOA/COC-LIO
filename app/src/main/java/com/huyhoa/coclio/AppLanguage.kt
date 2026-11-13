package com.huyhoa.coclio

import android.content.Context
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text as MaterialText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit

enum class AppUiLanguage {
    VIETNAMESE,
    ENGLISH
}

val LocalAppLanguage = staticCompositionLocalOf { AppUiLanguage.VIETNAMESE }

object AppLanguageSettings {
    private const val PREFERENCES = "coc_lio_language"
    private const val KEY_LANGUAGE = "preference"

    fun load(context: Context): String =
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
            .getString(KEY_LANGUAGE, "system")
            ?.takeIf { it in setOf("system", "vi", "en") }
            ?: "system"

    fun save(context: Context, preference: String) {
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LANGUAGE, preference.takeIf { it in setOf("system", "vi", "en") } ?: "system")
            .apply()
    }

    fun translate(context: Context, source: String): String {
        val systemLanguage = context.resources.configuration.locales[0].language
        return translateAppText(source, resolve(load(context), systemLanguage))
    }

    fun resolve(preference: String, systemLanguage: String): AppUiLanguage =
        when (preference) {
            "en" -> AppUiLanguage.ENGLISH
            "vi" -> AppUiLanguage.VIETNAMESE
            else -> if (systemLanguage.lowercase().startsWith("vi")) AppUiLanguage.VIETNAMESE else AppUiLanguage.ENGLISH
        }
}

private val englishTranslations = mapOf(
    "THEO DÕI TIẾN TRÌNH NÂNG CẤP VÀ THÔNG BÁO" to "UPGRADE PROGRESS & NOTIFICATIONS",
    "ĐANG KIỂM TRA XÁC NHẬN DỮ LIỆU TÀI KHOẢN..." to "CHECKING ACCOUNT DATA CONSENT...",
    "ĐÓNG" to "CLOSE",
    "Không thể đăng nhập Google" to "Unable to sign in with Google",
    "Chi tiết bên dưới giúp xác định chính xác cấu hình OAuth nào chưa khớp. Bạn có thể sao chép để gửi kiểm tra." to "The details below help identify which OAuth setting does not match. You can copy them for troubleshooting.",
    "SAO CHÉP CHẨN ĐOÁN" to "COPY DIAGNOSTICS",
    "CHÀO MỪNG ĐẾN VỚI CỘNG ĐỒNG COC-LIO" to "WELCOME TO THE COC-LIO COMMUNITY",
    "Để tạo và duy trì tài khoản, COC-LIO cần xử lý mã tài khoản, email, tên và ảnh hồ sơ Google, trạng thái xác minh cùng thông tin đăng nhập. Dữ liệu này được dùng để xác thực, quản lý tài khoản và hiển thị danh tính thành viên." to "To create and maintain your account, COC-LIO processes your account ID, email, name, Google profile photo, verification status and sign-in information. This data is used for authentication, account management and displaying your identity to other members.",
    "Tôi đã đọc và đồng ý với việc xử lý dữ liệu tài khoản cần thiết để sử dụng COC-LIO." to "I have read and agree to the processing of the account data required to use COC-LIO.",
    "XEM CHI TIẾT DỮ LIỆU" to "VIEW DATA DETAILS",
    "KHÔNG ĐỒNG Ý / ĐĂNG XUẤT" to "DISAGREE / SIGN OUT",
    "Dữ liệu hoạt động, thiết bị, phân tích, báo lỗi và đồng bộ JSON là các mục riêng; mặc định tắt và không được bật chỉ bằng ô xác nhận này." to "Activity, device, analytics, crash-reporting and JSON-sync data are separate options. They are off by default and are not enabled by this consent checkbox.",
    "CHI TIẾT DỮ LIỆU TÀI KHOẢN" to "ACCOUNT DATA DETAILS",
    "1. LOẠI DỮ LIỆU ĐƯỢC XỬ LÝ" to "1. TYPES OF DATA PROCESSED",
    "Mã tài khoản Google (UID); email; tên hiển thị, tên, họ và tên hiển thị do bạn đặt (nếu có); ảnh đại diện; trạng thái xác minh email; nhà cung cấp đăng nhập; số lần, thời điểm đăng nhập; thời điểm tạo, cập nhật hồ sơ; phiên bản và thời điểm xác nhận đồng ý." to "Google account ID (UID); email; display name, first and last name, and your chosen display name (if any); profile photo; email verification status; sign-in provider; sign-in count and times; profile creation and update times; and the version and time of consent.",
    "2. MỤC ĐÍCH XỬ LÝ" to "2. PURPOSES OF PROCESSING",
    "Xác thực đăng nhập Google; tạo và duy trì tài khoản COC-LIO; quản lý trạng thái, bảo mật tài khoản; hiển thị tên và ảnh đại diện khi bạn đăng nội dung trong cộng đồng." to "To authenticate Google sign-in; create and maintain your COC-LIO account; manage account status and security; and display your name and profile photo when you post in the community.",
    "3. BÊN KIỂM SOÁT VÀ DỊCH VỤ XỬ LÝ" to "3. DATA CONTROLLER AND PROCESSING SERVICES",
    "Bên kiểm soát và xử lý dữ liệu: cá nhân vận hành ứng dụng COC-LIO. Dịch vụ tham gia xử lý kỹ thuật: Google Sign-In, Firebase Authentication và Cloud Firestore của Google. Liên hệ về dữ liệu cá nhân: phunghuyhoahaikba@gmail.com." to "Data controller and processor: the individual operating the COC-LIO app. Technical processing services: Google Sign-In, Firebase Authentication and Google Cloud Firestore. Contact for personal data matters: phunghuyhoahaikba@gmail.com.",
    "4. QUYỀN VÀ NGHĨA VỤ CỦA BẠN" to "4. YOUR RIGHTS AND OBLIGATIONS",
    "Quyền: được biết; đồng ý hoặc không đồng ý, yêu cầu rút lại đồng ý; xem, chỉnh sửa hoặc yêu cầu chỉnh sửa; yêu cầu cung cấp, xóa, hạn chế hoặc phản đối xử lý; khiếu nại, tố cáo, khởi kiện, yêu cầu bồi thường và yêu cầu áp dụng biện pháp bảo vệ dữ liệu. Nghĩa vụ: cung cấp thông tin chính xác; tự bảo vệ dữ liệu của mình; tôn trọng, bảo vệ dữ liệu của người khác; tuân thủ pháp luật và tham gia phòng, chống hành vi xâm phạm dữ liệu cá nhân." to "Rights: to be informed; to consent or refuse and request withdrawal of consent; to view, edit or request correction; to request access, deletion, restriction or object to processing; to complain, report, sue, request compensation and request data-protection measures. Obligations: provide accurate information; protect your own data; respect and protect other people's data; comply with the law and help prevent violations of personal data.",
    "Bắt đầu theo dõi" to "Start tracking",
    "Lấy JSON trực tiếp từ game để COC-LIO đọc cấp độ, công trình đang nâng và thời gian còn lại." to "Import JSON from the game so COC-LIO can read levels, ongoing upgrades and remaining time.",
    "CÁCH LẤY JSON LẦN ĐẦU" to "HOW TO GET JSON FOR THE FIRST TIME",
    "1. Mở Clash of Clans và vào làng cần theo dõi." to "1. Open Clash of Clans and go to the village you want to track.",
    "2. Nhấn bánh răng Cài đặt → Cài đặt khác." to "2. Tap the Settings gear → More Settings.",
    "3. Kéo xuống Xuất dữ liệu → Xuất dữ liệu làng dưới dạng JSON → Sao chép." to "3. Scroll to Export Data → Export village data as JSON → Copy.",
    "4. Quay lại COC-LIO, dán JSON rồi nhấn Phân tích." to "4. Return to COC-LIO, paste the JSON and tap Analyze.",
    "Nút mở game chỉ đưa bạn vào trò chơi. Bạn cần tự thao tác xuất và sao chép JSON trong game." to "The game button only opens the game. You must export and copy the JSON yourself inside the game.",
    "ĐANG THEO DÕI" to "TRACKING",
    "CÔNG TRÌNH" to "BUILDINGS",
    "QUÂN ĐỘI" to "TROOPS",
    "KHO DỮ LIỆU JSON" to "JSON DATA STORAGE",
    "ĐANG LƯU" to "SAVING",
    "LỊCH SỬ" to "HISTORY",
    "JSON CHIA SẺ" to "SHARED JSON",
    "ĐÃ NHẬN" to "RECEIVED",
    "ĐÃ XÓA" to "DELETED",
    "JSON ĐÃ GỠ" to "REMOVED JSON",
    "CHIA SẺ JSON" to "SHARE JSON",
    "Tối đa 60 mốc nhập gần nhất trên thiết bị" to "Up to the 60 most recent imports on this device",
    "ĐANG XEM" to "VIEWING",
    "NHẬP / SỬ DỤNG" to "IMPORT / USE",
    "GỠ" to "REMOVE",
    "KHÔI PHỤC" to "RESTORE",
    "KHÔI PHỤC JSON" to "RESTORE JSON",
    "XÓA VĨNH VIỄN" to "DELETE PERMANENTLY",
    "Xóa vĩnh viễn JSON?" to "Permanently delete JSON?",
    "Xóa vĩnh viễn JSON đã nhận?" to "Permanently delete received JSON?",
    "Xóa dữ liệu làng?" to "Delete village data?",
    "NHẬP DỮ LIỆU LÀNG" to "IMPORT VILLAGE DATA",
    "Sao chép JSON trong Clash of Clans rồi dán vào ô bên dưới. Có thể đặt tên riêng cho làng. JSON là ảnh chụp tại thời điểm xuất; nếu dùng sách, potion, trợ thủ hoặc tăng tốc sau đó, hãy nhập JSON mới." to "Copy JSON from Clash of Clans and paste it below. You can give the village a custom name. JSON is a snapshot from the time it was exported; import new JSON after using books, potions, helpers or boosts.",
    "Tên làng (không bắt buộc)" to "Village name (optional)",
    "DÁN JSON ĐÃ SAO CHÉP" to "PASTE COPIED JSON",
    "Đóng" to "Close",
    "Phân tích" to "Analyze",
    "HỦY" to "CANCEL",
    "DANH SÁCH QUẢN LÝ NGƯỜI DÙNG" to "USER MANAGEMENT LIST",
    "LÀM MỚI" to "REFRESH",
    "Chỉ tài khoản quản lý mới xem được. Danh sách lấy từ hồ sơ tài khoản đã đăng nhập; ứng dụng không tự đối chiếu trạng thái toàn bộ tài khoản Google." to "Only administrators can view this list. It is based on account profiles that have signed in; the app does not check the status of every Google account.",
    "Đang tải dữ liệu tài khoản…" to "Loading account data…",
    "Chưa có tài khoản Google nào đăng nhập COC-LIO." to "No Google accounts have signed in to COC-LIO.",
    "CÀI ĐẶT" to "SETTINGS",
    "TÀI KHOẢN GOOGLE" to "GOOGLE ACCOUNT",
    "Chưa đăng nhập" to "Not signed in",
    "Khi đăng nhập Google, hồ sơ cơ bản gồm mã tài khoản, email, tên và ảnh đại diện sẽ được đồng bộ để quản lý tài khoản. IP, thiết bị và lịch sử hoạt động không được tự động thu thập chỉ vì bạn đăng nhập Google." to "When you sign in with Google, basic profile details including account ID, email, name and profile photo are synchronized for account management. Your IP, device details and activity history are not automatically collected just because you sign in with Google.",
    "ĐĂNG NHẬP GOOGLE" to "SIGN IN WITH GOOGLE",
    "CÁC HỒ SƠ ĐÃ ĐĂNG NHẬP TRÊN MÁY NÀY" to "PROFILES SIGNED IN ON THIS DEVICE",
    "CHỦ SỞ HỮU / QUẢN LÝ" to "OWNER / ADMIN",
    "MỞ DANH SÁCH QUẢN LÝ NGƯỜI DÙNG" to "OPEN USER MANAGEMENT LIST",
    "ĐỔI TÀI KHOẢN" to "SWITCH ACCOUNT",
    "ĐĂNG XUẤT" to "SIGN OUT",
    "GIAO DIỆN ỨNG DỤNG" to "APP APPEARANCE",
    "Tối" to "Dark",
    "KÍCH THƯỚC GIAO DIỆN & CỠ CHỮ" to "INTERFACE SIZE & TEXT SIZE",
    "Kích thước giao diện" to "Interface size",
    "Cỡ chữ" to "Text size",
    "RẤT GỌN" to "VERY COMPACT",
    "NHỎ" to "SMALL",
    "LỚN" to "LARGE",
    "TO" to "LARGE",
    "Điều chỉnh kích thước tổng thể của giao diện, nút và khoảng cách. Mức 3 (100%) là mặc định." to "Adjust the overall interface, button and spacing size. Level 3 (100%) is the default.",
    "Điều chỉnh cỡ chữ trong toàn ứng dụng. Mức 3 (100%) là mặc định." to "Adjust text size throughout the app. Level 3 (100%) is the default.",
    "LED RGB TÙY CHỈNH" to "CUSTOM RGB LED",
    "Bật hiệu ứng chuyển động" to "Enable animation",
    "Tốc độ chuyển màu" to "Color transition speed",
    "ĐỨNG YÊN" to "STILL",
    "CỰC NHANH" to "FASTEST",
    "Độ sáng: " to "Brightness: ",
    "Mờ" to "Dim",
    "Sáng" to "Bright",
    "THÔNG BÁO NÂNG CẤP" to "UPGRADE NOTIFICATIONS",
    "Nhắc khi bộ đếm trong JSON đã đếm hết." to "Remind me when the JSON countdown finishes.",
    "TIN NHẮN CỘNG ĐỒNG" to "COMMUNITY MESSAGES",
    "Cần đăng nhập Google; kiểm tra tin mới khi có mạng, khoảng 15 phút/lần." to "Google sign-in required; checks for new messages when online, about every 15 minutes.",
    "THÔNG BÁO BẢN CẬP NHẬT APK" to "APK UPDATE NOTIFICATIONS",
    "Cần đăng nhập Google; báo khi có phiên bản mới trong mục Cộng đồng." to "Google sign-in required; notifies you when a new version is posted in Community.",
    "Thông báo được kiểm tra định kỳ bằng kết nối mạng. Android có thể trì hoãn lâu hơn 15 phút khi tiết kiệm pin hoặc hạn chế chạy nền." to "Notifications are checked periodically over the network. Android may delay checks beyond 15 minutes when battery saver or background restrictions are active.",
    "Kiểu nhắc nâng cấp" to "Upgrade reminder type",
    "Tin nhắn" to "Message",
    "Báo thức" to "Alarm",
    "Thời gian hiển thị báo thức" to "Alarm display duration",
    "THỬ NHẮC NÂNG CẤP" to "TEST UPGRADE REMINDER",
    "Gửi một thông báo thử cho chức năng nhắc nâng cấp. Không tự bật lịch nhắc." to "Send a test notification for upgrade reminders. This does not enable reminder schedules.",
    "Thời điểm nhắc lấy từ bộ đếm thời gian trong JSON đã nhập. Android vẫn có thể trì hoãn nếu tắt quyền thông báo, hạn chế pin hoặc đặt chế độ Không làm phiền." to "Reminder times are based on the countdown in the imported JSON. Android may still delay them if notifications are disabled, battery use is restricted or Do Not Disturb is enabled.",
    "TIỆN ÍCH MÀN HÌNH CHÍNH" to "HOME SCREEN WIDGETS",
    "Nhấn giữ màn hình chính → chọn Tiện ích → COC-LIO → Theo dõi nâng cấp. Mỗi tiện ích gắn với tài khoản đang đăng nhập và một làng đã chọn. Muốn thêm tiện ích cho tài khoản khác, hãy đổi tài khoản trong COC-LIO rồi thêm một tiện ích mới." to "Long-press the home screen → select Widgets → COC-LIO → Upgrade Tracker. Each widget is linked to the signed-in account and a selected village. To add a widget for another account, switch accounts in COC-LIO and add a new widget.",
    "Tiện ích dạng dọc 2 × 4 ô gọn hơn, có viền RGB chuyển sắc liền mạch và danh sách nâng cấp có thể cuộn. Mỗi mục hiển thị ảnh công trình, đồng hồ đếm ngược ở giữa thanh và phần trăm thời gian còn lại." to "The compact vertical 2 × 4 widget has a continuous RGB gradient border and a scrollable upgrade list. Each item shows the building image, a countdown centered on the progress bar and the percentage of time remaining.",
    "CÁCH LẤY JSON" to "HOW TO GET JSON",
    "Mở Clash of Clans → Cài đặt (bánh răng) → Cài đặt khác → kéo xuống mục Xuất dữ liệu → chọn Xuất dữ liệu làng dưới dạng JSON → Sao chép. Trở lại COC-LIO và dán nội dung JSON." to "Open Clash of Clans → Settings (gear) → More Settings → scroll to Export Data → select Export village data as JSON → Copy. Return to COC-LIO and paste the JSON.",
    "MỞ CLASH OF CLANS" to "OPEN CLASH OF CLANS",
    "LIÊN HỆ NHÀ PHÁT TRIỂN" to "CONTACT THE DEVELOPER",
    "Mở Zalo để liên hệ nhà phát triển COC-LIO." to "Open Zalo to contact the COC-LIO developer.",
    "MỞ ZALO LIÊN HỆ" to "CONTACT VIA ZALO",
    "THÔNG TIN ỨNG DỤNG" to "ABOUT THE APP",
    "COC-LIO là ứng dụng cộng đồng không chính thức, không được Supercell chứng thực. Hình ảnh game thuộc quyền sở hữu của Supercell." to "COC-LIO is an unofficial community app and is not endorsed by Supercell. Game images are owned by Supercell.",
    "Đây là nội dung không chính thức và không được Supercell bảo trợ. Xem chính sách nội dung người hâm mộ tại www.supercell.com/fan-content-policy." to "This is unofficial content and is not sponsored by Supercell. See the Fan Content Policy at www.supercell.com/fan-content-policy.",
    "CỘNG ĐỒNG COC-LIO" to "COC-LIO COMMUNITY",
    "Trò chuyện, chia sẻ hình ảnh và cập nhật COC-LIO cùng mọi thành viên." to "Chat, share images and follow COC-LIO updates with other members.",
    "TRÒ CHUYỆN" to "CHAT",
    "CẬP NHẬT APK" to "APK UPDATES",
    "KHO ĐÃ GỠ" to "ARCHIVE",
    "ĐĂNG NHẬP ĐỂ THAM GIA" to "SIGN IN TO JOIN",
    "Đăng nhập Google để trò chuyện và chia sẻ cùng cộng đồng." to "Sign in with Google to chat and share with the community.",
    "ĐĂNG NHẬP" to "SIGN IN",
    "Chưa có tin nhắn. Hãy gửi lời chào đầu tiên!" to "No messages yet. Send the first greeting!",
    "🖼 ẢNH" to "🖼 IMAGE",
    "📦 GỬI JSON" to "📦 SEND JSON",
    "📎 TỆP" to "📎 FILE",
    "Viết tin nhắn…" to "Write a message…",
    "GỬI" to "SEND",
    "Đang gửi…" to "Sending…",
    "THỜI HẠN LƯU KHO" to "ARCHIVE RETENTION",
    "GIỮ MÃI" to "KEEP FOREVER",
    "TỰ ĐỘNG XÓA" to "DELETE AUTOMATICALLY",
    "Số lượng" to "Quantity",
    "NGÀY" to "DAYS",
    "THÁNG" to "MONTHS",
    "LƯU THIẾT LẬP LƯU TRỮ" to "SAVE ARCHIVE SETTINGS",
    "JSON DO CỘNG ĐỒNG CHIA SẺ" to "JSON SHARED BY THE COMMUNITY",
    "Tất cả thành viên đã đăng nhập đều có thể nhận JSON và nhập vào tài khoản của mình." to "All signed-in members can receive JSON and import it into their own account.",
    "Chưa có JSON nào được chia sẻ." to "No JSON has been shared yet.",
    "ĐĂNG BẢN CẬP NHẬT APK" to "POST APK UPDATE",
    "Tạm dừng trên gói miễn phí hiện tại. Tải lên và phân phối APK chưa được hỗ trợ." to "Paused on the current free plan. APK uploads and distribution are not supported.",
    "Chưa có bản cập nhật nào được đăng." to "No updates have been posted yet.",
    "CHỌN JSON ĐỂ GỬI" to "SELECT JSON TO SEND",
    "Gỡ tin nhắn vào kho?" to "Move message to archive?",
    "Bản ghi tin nhắn/JSON sẽ được chuyển vào Kho đã gỡ. Tệp đính kèm cũ hiện có thể chưa truy cập được." to "The message/JSON record will be moved to the archive. Older attachments may currently be inaccessible.",
    "GỠ VÀ LƯU" to "REMOVE AND ARCHIVE",
    "Gỡ bản cập nhật vào kho?" to "Move update to archive?",
    "Xóa vĩnh viễn khỏi kho?" to "Permanently delete from archive?",
    "Bản ghi dữ liệu này sẽ bị xóa vĩnh viễn. Tệp đính kèm cũ không bị xóa và hiện vẫn không thể truy cập." to "This data record will be permanently deleted. The old attachment will not be deleted and remains inaccessible.",
    "ADMIN" to "ADMIN",
    "Ảnh đính kèm hiện chưa thể xem trên gói miễn phí." to "Attachments cannot currently be viewed on the free plan.",
    "NHẬN JSON" to "RECEIVE JSON",
    "GỠ TIN" to "REMOVE MESSAGE",
    "Ảnh lưu trữ hiện chưa thể xem trên gói miễn phí." to "Archived images cannot currently be viewed on the free plan.",
    "Tệp đính kèm hiện chưa thể tải xuống" to "Attachments cannot currently be downloaded",
    "Tải APK hiện đang tạm dừng trên gói miễn phí." to "APK downloads are currently paused on the free plan.",
    "GỠ VÀO KHO" to "MOVE TO ARCHIVE",
    "TRA CỨU TÀI KHOẢN & CLAN" to "PLAYER & CLAN LOOKUP",
    "Tìm hồ sơ người chơi hoặc Clan bằng tên hoặc mã tag trong game." to "Find player or Clan profiles by in-game name or tag.",
    "NGƯỜI CHƠI" to "PLAYER",
    "CLAN" to "CLAN",
    "Nhập tên hoặc #ABC123" to "Enter a name or #ABC123",
    "Lọc theo cấp Nhà chính (1–20)" to "Filter by Town Hall level (1–20)",
    "Số thành viên tối thiểu (1–50)" to "Minimum member count (1–50)",
    "ĐANG TRA CỨU..." to "SEARCHING...",
    "TRA CỨU" to "SEARCH",
    "Có thể tìm bằng tên người chơi, tên Clan hoặc Player Tag / Clan Tag. Không cần nhập Supercell ID hay email." to "Search by player name, Clan name, Player Tag or Clan Tag. No Supercell ID or email is required.",
    "CHỌN KẾT QUẢ TÌM ĐƯỢC" to "SELECT A SEARCH RESULT",
    "MỞ HỒ SƠ" to "OPEN PROFILE",
    "SO SÁNH THỜI GIAN Ở CLAN" to "COMPARE CLAN TENURE",
    "Player Tag thứ hai" to "Second Player Tag",
    "SO SÁNH" to "COMPARE",
    "Chọn mục để tải thêm dữ liệu từ ClashKing." to "Select a section to load more data from ClashKing.",
    "TOÀN CẢNH CLASH OF CLANS" to "CLASH OF CLANS OVERVIEW",
    "Thống kê cộng đồng, lịch mùa giải và bảng xếp hạng công khai." to "Community statistics, season schedule and public leaderboards.",
    "BẢNG XẾP HẠNG THEO ĐIỀU KIỆN" to "FILTERED LEADERBOARDS",
    "Cấp Nhà chính (1–20)" to "Town Hall level (1–20)",
    "TOP NHÀ CHÍNH" to "TOP TOWN HALL",
    "TOP 3 SAO" to "TOP 3 STARS",
    "Mã cấp giải đấu (League Tier ID)" to "League Tier ID",
    "TOP GIẢI ĐẤU" to "TOP LEAGUES",
    "THỐNG KÊ RANKED THEO NHÀ CHÍNH & GIẢI" to "RANKED STATS BY TOWN HALL & LEAGUE",
    "PHÂN BỐ CÚP GIẢI ĐẤU" to "LEAGUE TROPHY DISTRIBUTION",
    "Mã khu vực / quốc gia (Location ID)" to "Region / country code (Location ID)",
    "TOP QUYÊN GÓP" to "TOP DONATIONS",
    "TOP CHIẾN THẮNG" to "TOP WINS",
    "TOP VÀNG THỦ ĐÔ" to "TOP CAPITAL GOLD",
    "Nguồn dữ liệu: ClashKing · Creator Code: ClashKing" to "Data source: ClashKing · Creator Code: ClashKing",
    "Dữ liệu do ClashKing thu thập bằng cách đồng bộ nguồn trò chơi; có thể chậm hoặc chưa đầy đủ. COC-LIO không liên kết, được bảo trợ hoặc xác nhận bởi Supercell." to "Data is collected by ClashKing by synchronizing game sources and may be delayed or incomplete. COC-LIO is not affiliated with, sponsored or endorsed by Supercell.",
    "MỞ CLASHKING" to "OPEN CLASHKING",
    "Không có dữ liệu chi tiết." to "No detailed data available.",
    "Có dữ liệu chi tiết." to "Detailed data available.",
    "XEM THÊM TRƯỜNG" to "SHOW MORE FIELDS",
    "Chưa có dữ liệu trong mục này." to "No data in this section.",
    "XEM THÊM DỮ LIỆU" to "LOAD MORE DATA",
    "XẾP HẠNG CLAN" to "CLAN RANKING",
    "DANH SÁCH THÀNH VIÊN" to "MEMBER LIST",
    "Chỉ hiển thị 50 thành viên đầu tiên." to "Only the first 50 members are shown.",
    "ẨN LỌC NÂNG CAO" to "HIDE ADVANCED FILTERS",
    "LỌC NÂNG CAO" to "ADVANCED FILTERS",
    "TẢI THÊM KẾT QUẢ" to "LOAD MORE RESULTS",
    "ĐANG TẢI..." to "LOADING...",
    "Tên hoặc Player Tag" to "Name or Player Tag",
    "Tên hoặc Clan Tag" to "Name or Clan Tag",
    "THỐNG KÊ & LỊCH SỬ NGƯỜI CHƠI" to "PLAYER STATS & HISTORY",
    "THỐNG KÊ & LỊCH SỬ CLAN" to "CLAN STATS & HISTORY",
    "LÀNG ĐÊM" to "BUILDER BASE",
    "HOÀN THÀNH • CHỜ JSON MỚI" to "COMPLETE • WAITING FOR NEW JSON",
    "Sẵn sàng" to "Ready",
    "Chưa có dữ liệu phụ" to "No additional data",
    "Ảnh hồ sơ Google" to "Google profile photo",
    "Ảnh hồ sơ" to "Profile photo",
    "Theo hệ thống" to "System default",
    "Tự động theo thiết bị" to "Automatic (device language)",
    "NGÔN NGỮ" to "LANGUAGE",
    "Ngôn ngữ ứng dụng" to "App language",
    "Tự động dùng ngôn ngữ thiết bị. Có thể chọn Tiếng Việt hoặc English bên dưới." to "Automatically use your device language. You can also select Vietnamese or English below.",
    "Đang dùng ngôn ngữ hệ thống: Tiếng Việt" to "Using system language: Vietnamese",
    "Đang dùng ngôn ngữ hệ thống: English" to "Using system language: English",
    "Ngôn ngữ đang dùng: Tiếng Việt" to "Current language: Vietnamese",
    "Ngôn ngữ đang dùng: English" to "Current language: English",
    "Lựa chọn ngôn ngữ được lưu trên thiết bị này." to "Your language preference is saved on this device.",
    "Tin nhắn cộng đồng" to "Community messages",
    "Bản cập nhật COC-LIO" to "COC-LIO updates",,
    "Thông báo mới trong cộng đồng." to "New community message.",
    "MỞ GAME" to "OPEN GAME",,
    "Thợ rảnh" to "Free builders",
    "Mở ứng dụng và nhập dữ liệu JSON" to "Open the app and import JSON",
    "Không có mục nào đang nâng cấp" to "No active upgrades",
    "đang nâng" to "upgrading",
    "hoàn thành" to "completed",
    "mục đang nâng cấp" to "upgrades in progress",
    "hoàn thành • chờ JSON mới" to "completed • waiting for new JSON",
    "Đang tải danh sách nâng cấp…" to "Loading upgrades…",
    "ĐANG CẬP NHẬT" to "UPDATING",
    "HOÀN THÀNH" to "COMPLETE",
    "Chưa có làng để theo dõi" to "No village to track",
    "Hãy mở COC-LIO, đăng nhập tài khoản cần dùng (nếu có), nhập JSON ít nhất một lần rồi thêm tiện ích." to "Open COC-LIO, sign in to the desired account (if needed), import JSON at least once, then add the widget."
    "Cộng đồng COC-LIO" to "COC-LIO Community",
    "COC-LIO có bản cập nhật" to "COC-LIO update available",
    "Có tin nhắn mới trong cộng đồng." to "There is a new community message.",
    "Có phiên bản APK mới." to "A new APK version is available.",
    "Thông báo tin nhắn mới trong cộng đồng COC-LIO" to "Notifications for new community messages in COC-LIO",
    "Thông báo khi quản lý chọn gửi tin bản cập nhật" to "Notifications when an administrator posts an app update"
    "Tin nhắn cộng đồng mới" to "New community message",
    "COC-LIO có bản cập nhật mới" to "COC-LIO update available",
    "Đã chia sẻ dữ liệu JSON." to "Shared JSON data.",
    "MỞ COC-LIO" to "OPEN COC-LIO",
    "NHẬN JSON VÀ SỬ DỤNG" to "RECEIVE AND USE JSON",
    "XÓA" to "DELETE",
    "Băng" to "Ice",
    "Tím" to "Violet",
    "Lửa" to "Fire",
    "Ngọc" to "Mint",
    "TIN NHẮN" to "MESSAGES",
    "HÌNH ẢNH" to "IMAGES",
    "TỆP" to "FILES",
    "APK CẬP NHẬT" to "APK UPDATES",
    "ĐANG XÁC NHẬN..." to "VERIFYING...",
    "ĐỒNG Ý & TIẾP TỤC" to "AGREE & CONTINUE",
    "ĐỒNG Ý & ĐĂNG NHẬP GOOGLE" to "AGREE & SIGN IN WITH GOOGLE",
    "Kho JSON chưa có dữ liệu. Hãy nhập JSON trước khi chia sẻ." to "JSON Storage is empty. Import JSON before sharing.",
    "Cấp Nhà chính phải từ 1 đến 20." to "Town Hall level must be between 1 and 20.",
    "Số thành viên tối thiểu phải từ 1 đến 50." to "Minimum member count must be between 1 and 50.",
    "Đang tải" to "Loading",
    "Đang tải…" to "Loading…",
    "Không thể tải dữ liệu." to "Unable to load data.",
    "Không thể kết nối máy chủ." to "Unable to connect to the server.",
    "Đang tải dữ liệu..." to "Loading data...",
    "Đang tải dữ liệu…" to "Loading data…",
    "Thành viên" to "Member",
    "Đang nâng" to "Upgrading",
    "Hoàn thành" to "Complete",
    "NHÀ CHÍNH" to "TOWN HALL",
    "ĐANG NÂNG" to "UPGRADING",
    "Nâng cấp" to "Upgrade",
    "Thời gian còn lại" to "Time remaining",
    "Lọc nâng cao" to "Advanced filters",
    "Tìm kiếm" to "Search",
    "Không rõ tên" to "Unknown name",
    "Làng chưa đặt tên" to "Unnamed village",
    "Chưa có tên" to "No name",
    "Bạn" to "You",
    "Tổng quan" to "Overview",
    "Công trình Quân đội" to "Buildings & Troops",
    "Kho JSON" to "JSON Storage",
    "Cộng đồng" to "Community",
    "Tra cứu ID" to "ID Lookup",
    "Cài đặt" to "Settings",
    "Theo thiết bị" to "Follow device",
    "＋ NHẬP JSON" to "＋ IMPORT JSON",
    "Google không cung cấp" to "Not provided by Google",
    "Chưa ghi nhận" to "Not recorded",
    "Chưa ghi nhận lần mở gần nhất" to "No recent app launch recorded",
    "Ảnh đại diện Google" to "Google profile photo",
    "Kích hoạt lại tài khoản COC-LIO?" to "Reactivate this COC-LIO account?",
    "Vô hiệu hóa tài khoản COC-LIO?" to "Disable this COC-LIO account?",
    "Tài khoản Google vẫn tồn tại. Kích hoạt lại sẽ cho phép tài khoản này truy cập COC-LIO và dữ liệu của mình theo quyền bình thường." to "The Google account still exists. Reactivating this profile will restore its normal access to COC-LIO and its data.",
    "Tài khoản Google vẫn tồn tại. COC-LIO sẽ đánh dấu hồ sơ này bị vô hiệu hóa để chặn truy cập dữ liệu tài khoản. Dữ liệu cũ không bị xóa." to "The Google account still exists. COC-LIO will mark this profile as disabled to block access to account data. Existing data will not be deleted.",
    "ĐANG XỬ LÝ…" to "PROCESSING…",
    "KÍCH HOẠT LẠI" to "REACTIVATE",
    "VÔ HIỆU HÓA" to "DISABLE",
    "● ĐÃ VÔ HIỆU HÓA" to "● DISABLED",
    "● ĐANG ONLINE" to "● ONLINE",
    "● OFFLINE" to "● OFFLINE",
    "Lần đăng nhập gần nhất · " to "Last sign-in · ",
    "Đã cho phép ghi nhận hoạt động/thiết bị (không ghi IP trên Spark)" to "Activity/device logging allowed (IP is not logged on Spark)",
    "Tài khoản quản lý · thiết bị hiện tại" to "Admin account · current device",
    "Hồ sơ Google được đồng bộ" to "Google profile synchronized",
    "ẨN THÔNG TIN" to "HIDE DETAILS",
    "XEM THÔNG TIN" to "VIEW DETAILS",
    "Chọn làng cho tiện ích" to "Choose a village for the widget",,
    "Chưa có mục nào đang nâng cấp" to "No upgrades in progress",
    "Bạn chưa cấp quyền thông báo cho COC-LIO." to "You have not granted notification permission to COC-LIO.",
    "Đã gửi thông báo thử. Hãy kéo thanh thông báo xuống để xem." to "Test notification sent. Pull down the notification shade to view it.",
    "Không thể gửi thông báo thử. Hãy kiểm tra quyền thông báo của ứng dụng." to "Unable to send the test notification. Check the app's notification permission.",
    "Không tải được lựa chọn dữ liệu tài khoản. Các mục tùy chọn vẫn đang tắt." to "Unable to load account data choices. Optional data collection remains off.",
    "Bạn cần đăng nhập Google trước khi nhập JSON và xem dữ liệu làng." to "Sign in with Google before importing JSON and viewing village data.",
    "JSON vẫn được lưu trên điện thoại nhưng chưa thể đồng bộ lên tài khoản của bạn." to "The JSON is saved on your phone but could not be synchronized to your account.",
    "Đã xác nhận dữ liệu cần thiết và đăng nhập Google thành công." to "Required data consent confirmed and Google sign-in completed.",
    "Đã khôi phục dữ liệu làng." to "Village data restored.",
    "Đã xóa vĩnh viễn dữ liệu JSON." to "JSON data permanently deleted.",
    "Đã chuyển JSON nhận được vào mục ĐÃ GỠ. Bản trong lịch sử vẫn được giữ." to "Received JSON moved to REMOVED. The copy in HISTORY is kept.",
    "Đã khôi phục JSON đã nhận." to "Received JSON restored.",
    "Đã xóa vĩnh viễn JSON nhận được khỏi thiết bị." to "Received JSON permanently deleted from this device.",
    "Không thể chia sẻ JSON." to "Unable to share JSON."
    "Bạn đã hủy đăng nhập Google." to "You cancelled Google sign-in.",
    "Không thể đăng nhập Google. Hãy kiểm tra kết nối mạng và cấu hình OAuth." to "Unable to sign in with Google. Check your network connection and OAuth configuration.",
    "Bạn đã từ chối xử lý dữ liệu tài khoản cần thiết và đã đăng xuất." to "You declined the required account data processing and have been signed out.",
    "Đã đăng xuất tài khoản hiện tại. Hãy chọn tài khoản Google khác trên màn hình đăng nhập." to "You have signed out of the current account. Choose another Google account on the sign-in screen.",
    "Đã đăng xuất Google. Dữ liệu cục bộ vẫn được giữ trên thiết bị." to "You have signed out of Google. Local data remains on this device.",
    "Không thể tải danh sách tài khoản." to "Unable to load the account list.",
    "Không thể cập nhật trạng thái tài khoản." to "Unable to refresh account status.",
    "Không thể vô hiệu hóa tài khoản COC-LIO." to "Unable to disable the COC-LIO account.",
    "Lần đăng nhập gần nhất" to "Last sign-in",
    "Chưa có dữ liệu" to "No data",
    "Có" to "Yes",
    "Không" to "No",
    "Danh sách" to "List",
    "Mã" to "Tag",
    "Tên" to "Name",
    "Nhà chính" to "Town Hall",
    "Nhà thợ" to "Builder Hall",
    "Cấp Clan" to "Clan Level",
    "Số thành viên" to "Members",
    "Cúp" to "Trophies",
    "Số sao" to "Stars",
    "Phần trăm phá hủy" to "Destruction percentage",
    "Thắng tấn công" to "Attack wins",
    "Thắng phòng thủ" to "Defense wins",
    "Lính đã cho" to "Troops donated",
    "Lính đã nhận" to "Troops received",
    "Số trận thắng" to "War wins",
    "Chuỗi thắng" to "Win streak",
    "Vàng Thủ đô" to "Capital Gold",
    "Mùa giải" to "Season",
    "Thứ hạng" to "Rank",
    "Hạng toàn cầu" to "Global rank",
    "Hạng địa phương" to "Local rank",
    "Thời gian" to "Time",
    "Trạng thái" to "Status",
    "Đối thủ" to "Opponent",
    "Giải đấu" to "League",
    "Lượt tấn công" to "Attacks",
    "Lượt phòng thủ" to "Defenses",
    "Số sao trung bình" to "Average stars",
    "Phá hủy trung bình" to "Average destruction",
    "Thời lượng trung bình" to "Average duration",
    "Lượt đánh bị bỏ lỡ" to "Missed attacks",
    "Quy mô đội" to "Team size",
    "Số lần tham gia" to "Visits",
    "Số phút" to "Minutes",
    "Phân trang" to "Pagination",
    "Còn dữ liệu" to "More data available",
    "Tổng số" to "Total",
    "Bảng xếp hạng Nhà chính" to "Town Hall leaderboard",
    "Chuỗi 3 sao Nhà chính" to "3-star streak by Town Hall",
    "Bảng xếp hạng giải" to "League leaderboard",
    "Phân bố cúp giải đấu" to "League trophy distribution",
    "Top quyên góp Clan" to "Top Clan donations",
    "Top thắng chiến tranh Clan" to "Top Clan war wins",
    "Top vàng Thủ đô Clan" to "Top Clan Capital Gold",
    "Số liệu toàn cầu" to "Global statistics",
    "Số lượng Nhà chính" to "Town Hall counts",
    "Số lượng giải đấu" to "League counts",
    "Số lượng Clan theo khu vực" to "Clan counts by region",
    "Số lượng Clan theo CWL" to "Clan counts by CWL",
    "Số lượng Clan theo giải thủ đô" to "Clan counts by Capital League",
    "Phân bố quy mô Clan" to "Clan size distribution",
    "Lịch mùa giải hiện tại" to "Current season schedule",
    "Lịch cuối tuần đột kích" to "Raid weekend schedule",
    "Lịch các mùa giải" to "Season schedules",
    "Top chuỗi thắng Clan" to "Top Clan win streaks",
    "Tỷ lệ sao chiến tranh" to "War star rate",
    "Tổng kết chiến tranh" to "War summary",
    "Hiệu suất chiến tranh" to "War performance",
    "Thống kê CWL" to "CWL statistics",
    "Tỷ lệ đánh theo giải" to "Attack rate by league",
    "Hiệu suất Legend" to "Legend performance",
    "Đội hình được dùng nhiều" to "Most-used army compositions",
    "Hoạt động 30 ngày" to "30-day activity",
    "Lịch sử trận đánh" to "Battle history",
    "Thống kê kiếm tài nguyên" to "Loot statistics",
    "Thống kê chiến tranh" to "War statistics",
    "Chi tiết từng lượt đánh chiến tranh" to "War attack details",
    "Lịch sử CWL" to "CWL history",
    "Lịch sử vào/rời Clan" to "Clan join/leave history",
    "Thống kê thời gian ở Clan" to "Clan tenure statistics",
    "Lịch sử thay đổi hồ sơ" to "Profile change history",
    "Lịch sử quyên góp" to "Donation history",
    "Lịch sử nhận lính" to "Troop received history",
    "Lịch sử Clan Games" to "Clan Games history",
    "Lịch sử vàng Thủ đô" to "Capital Gold history",
    "Lịch sử cúp Nhà chính" to "Town Hall trophy history",
    "Lịch sử cúp Nhà thợ" to "Builder Hall trophy history",
    "Các mốc chiến tranh" to "War milestones",
    "Thành tích Legend" to "Legend achievements",
    "Mùa Legend hiện tại" to "Current Legend season",
    "Diễn biến cúp Legend" to "Legend trophy progress",
    "So sánh thành tích Legend" to "Compare Legend achievements",
    "Lịch sử Ranked" to "Ranked history",
    "Lịch sử giải đấu Ranked và Legend" to "Ranked and Legend league history",
    "Tổng kết quyên góp" to "Donation summary",
    "Lịch sử chiến tranh" to "War history",
    "Nhật ký chiến tranh công khai" to "Public war log",
    "Cuộc chiến hiện tại" to "Current war",
    "Các mùa CWL" to "CWL seasons",
    "Nhóm CWL" to "CWL group",
    "Thứ hạng CWL" to "CWL ranking",
    "Lịch sử thành viên" to "Member history",
    "Thay đổi hồ sơ Clan" to "Clan profile changes",
    "Lịch sử bảng xếp hạng" to "Leaderboard history",
    "Tổng kết hạng Clan nhà chính" to "Clan Town Hall ranking summary",
    "Tổng kết hạng Clan Nhà thợ" to "Clan Builder Hall ranking summary",
    "Tổng kết hạng Thủ đô Clan" to "Clan Capital ranking summary",
    "Lịch sử Legend của Clan" to "Clan Legend history",
    "Top Legend của Clan" to "Top Clan Legend",
    "Kỷ lục Clan" to "Clan records",
    "Bảng xếp hạng Clan" to "Clan leaderboard",
    "Trò chuyện cộng đồng" to "Community chat",
    "Mở ứng dụng" to "Open app",
    "Đăng nhập Google để trò chuyện và gửi tin nhắn." to "Sign in with Google to chat and send messages.",
    "Đang tải tin nhắn…" to "Loading messages…",
    "Không thể tải tin nhắn." to "Unable to load messages.",
    "Không gửi được tin nhắn." to "Unable to send the message.",
    "Bong bóng chat cộng đồng" to "Community chat bubbles",
    "Để hiện bong bóng, hãy bật Bong bóng trong cài đặt thông báo Android hoặc nhấn giữ thông báo tin nhắn cộng đồng rồi chọn hiển thị dạng bong bóng." to "To show chat bubbles, enable Bubbles in Android notification settings or long-press a community message notification and choose to show it as a bubble.",
    "Thần chú" to "SPELLS",
    "Nhà máy Thần chú" to "Spell Factory",
    "Nhà máy Hắc Thần chú" to "Dark Spell Factory",
    "Tháp Thần chú" to "Spell Tower",
    "Thần chú Sấm sét" to "Lightning Spell",
    "Thần chú Hồi máu" to "Healing Spell",
    "Thần chú Thịnh nộ" to "Rage Spell",
    "Thần chú Bật nhảy" to "Jump Spell",
    "Thần chú Đóng băng" to "Freeze Spell",
    "Thần chú Độc dược" to "Poison Spell",
    "Thần chú Động đất" to "Earthquake Spell",
    "Thần chú Tăng tốc" to "Haste Spell",
    "Thần chú Nhân bản" to "Clone Spell",
    "Thần chú Bộ xương" to "Skeleton Spell",
    "Thần chú Dơi" to "Bat Spell",
    "Thần chú Tàng hình" to "Invisibility Spell",
    "Thần chú Thu quân" to "Recall Spell",
    "Thần chú Siêu tăng trưởng" to "Overgrowth Spell",
    "Thần chú Hồi sinh" to "Revive Spell",
    "Thần chú Khiên chắn băng" to "Ice Block Spell",
    "Thần chú Vật tổ" to "Totem Spell",
    "Thần chú Giận dữ" to "Angry Spell",
    "Bản dịch tự động" to "Automatic translation",
    "Bản gốc" to "Original",
    "Đang dịch tự động…" to "Translating automatically…",
    "CĂN CỨ THỢ XÂY" to "BUILDER BASE",
    "Quân đội" to "Troops",
    "Linh thú" to "Pets",
    "Trang bị anh hùng" to "Hero Equipment",
    "Tin nhắn và ghi chú cập nhật được tự động dịch theo ngôn ngữ ứng dụng khi có thể; bản gốc vẫn được giữ." to "Messages and update notes are automatically translated into the app language when available; the original text is preserved.",
)

private fun translateAppText(source: String, language: AppUiLanguage): String {
    if (language != AppUiLanguage.ENGLISH) return source
    englishTranslations[source]?.let { return it }
    return when {
        source.startsWith("Tổng số tài khoản: ") -> "Total accounts: " + source.removePrefix("Tổng số tài khoản: ")
        source.startsWith("MỨC ") -> "LEVEL " + source.removePrefix("MỨC ")
        source.startsWith("Mở lần cuối: ") -> "Last opened: " + source.removePrefix("Mở lần cuối: ")
        source.startsWith("Đăng nhập ") -> "Signed in " + source.removePrefix("Đăng nhập ")
        source.startsWith("CẤP ") -> "LEVEL " + source.removePrefix("CẤP ")
        source.startsWith("Bảng xếp hạng Nhà chính ") -> "Town Hall leaderboard " + source.removePrefix("Bảng xếp hạng Nhà chính ")
        source.startsWith("Chuỗi 3 sao Nhà chính ") -> "3-star streak by Town Hall " + source.removePrefix("Chuỗi 3 sao Nhà chính ")
        source.startsWith("Bảng xếp hạng giải ") -> "League leaderboard " + source.removePrefix("Bảng xếp hạng giải ")
        source.startsWith("Phân bố cúp giải đấu ") -> "League trophy distribution " + source.removePrefix("Phân bố cúp giải đấu ")
        source.startsWith("Hồi ") -> "Cooldown " + source.removePrefix("Hồi ")
        source.startsWith("Thông báo sẽ tự đóng sau ") -> {
            val match = Regex("^Thông báo sẽ tự đóng sau (\\d+) giây\\. Trong thời gian đó, bạn có thể nhấn MỞ GAME để vào Clash of Clans\\.$").matchEntire(source)
            match?.let { "The notification will close automatically after ${it.groupValues[1]} seconds. During that time, tap OPEN GAME to launch Clash of Clans." } ?: source
        }
        source.startsWith("Toàn bộ ") -> source
            .replace(" mốc JSON của ", " JSON entries for ")
            .replace(" sẽ bị xóa khỏi thiết bị và không thể khôi phục.", " will be deleted from this device and cannot be recovered.")
            .replaceFirst("Toàn bộ ", "All ")
        source.startsWith("JSON từ ") -> source
            .replaceFirst("JSON từ ", "JSON from ")
            .replace(" sẽ bị xóa khỏi Kho JSON và không thể khôi phục.", " will be deleted from JSON Storage and cannot be recovered.")
            .replace(" Bản nhập trong LỊCH SỬ vẫn được giữ.", " Imports in HISTORY will be kept.")
        source.startsWith("Chia sẻ bởi: ") -> "Shared by: " + source.removePrefix("Chia sẻ bởi: ")
        source.startsWith("Dữ liệu: ") -> "Data: " + source.removePrefix("Dữ liệu: ")
        source.startsWith("Đã gỡ: ") -> "Removed: " + source.removePrefix("Đã gỡ: ")
        source.startsWith("Đã gửi: ") -> "Sent: " + source.removePrefix("Đã gửi: ")
        source.startsWith("Đã tải ") -> "Loaded " + source.removePrefix("Đã tải ")
        source.startsWith("Nhà chính ") -> "Town Hall " + source.removePrefix("Nhà chính ")
        source.startsWith("Cấp Nhà chính ") -> "Town Hall Level " + source.removePrefix("Cấp Nhà chính ")
        source.startsWith("Cấp Clan ") -> "Clan Level " + source.removePrefix("Cấp Clan ")
        source.startsWith("Cấp ") -> "Level " + source.removePrefix("Cấp ")
        source.startsWith("Có ") -> "Has " + source.removePrefix("Có ")
        source.startsWith("MỤC ") -> "SECTION " + source.removePrefix("MỤC ")
        source.startsWith("Nhóm ") -> "Group " + source.removePrefix("Nhóm ")
        source.startsWith("Đang hiển thị ") -> "Showing " + source.removePrefix("Đang hiển thị ")
        source.startsWith("Cập nhật ") -> "Updated " + source.removePrefix("Cập nhật ")
        source.startsWith("Đã đồng bộ bản JSON mới nhất của ") -> {
            val suffix = " lên tài khoản COC-LIO."
            if (source.endsWith(suffix)) {
                "Synced the latest JSON snapshot for " + source.removePrefix("Đã đồng bộ bản JSON mới nhất của ").removeSuffix(suffix) + " to your COC-LIO account."
            } else source
        }
        source.startsWith("Đã chuyển ") && source.endsWith(" vào mục ĐÃ GỠ.") -> {
            val tag = source.removePrefix("Đã chuyển ").removeSuffix(" vào mục ĐÃ GỠ.")
            "Moved " + tag + " to REMOVED."
        }
        source.startsWith("Độ sáng: ") -> "Brightness: " + source.removePrefix("Độ sáng: ")
        source.startsWith("Player Tag: ") -> source
        source.startsWith("COC-LIO ") && source.endsWith(" đã có bản cập nhật mới.") -> source.removeSuffix(" đã có bản cập nhật mới.") + " has a new update."
        source.startsWith("ID: ") -> source
        else -> source
    }
}

@Composable
fun Text(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    style: TextStyle = LocalTextStyle.current
) {
    val language = LocalAppLanguage.current
    MaterialText(
        text = translateAppText(text, language),
        modifier = modifier,
        color = color,
        fontSize = fontSize,
        fontStyle = fontStyle,
        fontWeight = fontWeight,
        fontFamily = fontFamily,
        letterSpacing = letterSpacing,
        textDecoration = textDecoration,
        textAlign = textAlign,
        lineHeight = lineHeight,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        minLines = minLines,
        onTextLayout = onTextLayout,
        style = style
    )
}

suspend fun SnackbarHostState.showAppSnackbar(
    context: Context,
    message: String
): SnackbarResult {
    val language = AppLanguageSettings.resolve(
        AppLanguageSettings.load(context),
        context.resources.configuration.locales[0].language
    )
    return showSnackbar(translateAppText(message, language))
}

@Composable
fun Text(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    style: TextStyle = LocalTextStyle.current
) {
    MaterialText(
        text = text,
        modifier = modifier,
        color = color,
        fontSize = fontSize,
        fontStyle = fontStyle,
        fontWeight = fontWeight,
        fontFamily = fontFamily,
        letterSpacing = letterSpacing,
        textDecoration = textDecoration,
        textAlign = textAlign,
        lineHeight = lineHeight,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        minLines = minLines,
        onTextLayout = onTextLayout,
        style = style
    )
}
