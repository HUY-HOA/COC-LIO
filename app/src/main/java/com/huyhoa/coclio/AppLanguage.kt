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
    "Tôi đồng ý chia sẻ dữ liệu tài khoản cần thiết để sử dụng COC-LIO." to "I agree to share the account data required to use COC-LIO.",
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
    "LÀNG CHÍNH • ĐANG NÂNG" to "HOME VILLAGE • UPGRADING",
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
    "Bản cập nhật COC-LIO" to "COC-LIO updates",
    "Thông báo mới trong cộng đồng." to "New community message.",
    "MỞ GAME" to "OPEN GAME",
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
    "Hãy mở COC-LIO, đăng nhập tài khoản cần dùng (nếu có), nhập JSON ít nhất một lần rồi thêm tiện ích." to "Open COC-LIO, sign in to the desired account (if needed), import JSON at least once, then add the widget.",
    "Cộng đồng COC-LIO" to "COC-LIO Community",
    "COC-LIO có bản cập nhật" to "COC-LIO update available",
    "Có tin nhắn mới trong cộng đồng." to "There is a new community message.",
    "Có phiên bản APK mới." to "A new APK version is available.",
    "Thông báo tin nhắn mới trong cộng đồng COC-LIO" to "Notifications for new community messages in COC-LIO",
    "Thông báo khi quản lý chọn gửi tin bản cập nhật" to "Notifications when an administrator posts an app update",
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
    "LƯU ẢNH" to "SAVE IMAGE",
    "TẢI TỆP" to "DOWNLOAD FILE",
    "Tệp gốc vượt quá giới hạn xử lý 25 MB." to "The original file exceeds the 25 MB processing limit.",
    "Không thể đọc tệp đã chọn." to "Unable to read the selected file.",
    "Tệp đang trống." to "The selected file is empty.",
    "Ảnh không hợp lệ." to "The image is invalid.",
    "Không thể đọc ảnh này." to "Unable to read this image.",
    "Định dạng tệp này chưa được hỗ trợ." to "This file format is not supported.",
    "Tệp quá lớn. Trên gói miễn phí, mỗi tệp tối đa 350 KB." to "The file is too large. Each file is limited to 350 KB on the free plan.",
    "Không thể nén tệp xuống giới hạn 350 KB. Hãy chọn tệp nhỏ hơn." to "The file cannot be compressed below 350 KB. Choose a smaller file.",
    "Ảnh quá lớn sau khi nén. Hãy chọn ảnh nhỏ hơn." to "The image is still too large after compression. Choose a smaller image.",
    "Bạn cần đăng nhập Google để gửi tệp." to "Sign in with Google to send an attachment.",
    "Chỉ tài khoản quản lý mới được gửi tệp." to "Only the administrator account can send files.",
    "Đã gửi ảnh." to "Image sent.",
    "Đã gửi tệp." to "File sent.",
    "Đã lưu tệp." to "File saved.",
    "Không thể mở vị trí lưu." to "Unable to open the save location.",
    "Không thể hiển thị ảnh này." to "Unable to display this image.",
    "Ảnh không còn dữ liệu xem trước." to "The image preview data is no longer available.",
    "Không thể lưu tệp." to "Unable to save the file.",
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
    "LÀNG CHÍNH" to "HOME VILLAGE",
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
    "Thông tin" to "Information",
    "Công trình Quân đội" to "Information",
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
    "Chọn làng cho tiện ích" to "Choose a village for the widget",
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
    "Không thể chia sẻ JSON." to "Unable to share JSON.",
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
    "Nhà máy Thần chú Hắc ám" to "Dark Spell Factory",
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
    "Thần chú Giận dữ" to "Fury Spell",
    "CĂN CỨ THỢ XÂY" to "BUILDER BASE",
    "Quân đội" to "Troops",
    "Linh thú" to "Pets",
    "Trang bị anh hùng" to "Hero Equipment",
    "Không thể kiểm tra xác nhận dữ liệu tài khoản." to "Unable to verify account data consent.",
    "Đã tạo mốc dữ liệu đầu tiên" to "Created the first data snapshot",
    "Đã lưu dữ liệu cho " to "Saved data for ",
    "Từ lần xuất JSON tiếp theo, COC-LIO sẽ đối chiếu cấp độ và tiến trình nâng cấp." to "Starting with the next JSON export, COC-LIO will compare levels and upgrade progress.",
    "Đã lưu JSON cũ vào kho" to "Older JSON saved to storage",
    "JSON này có thời điểm cũ hơn dữ liệu mới nhất đã lưu cho " to "This JSON is older than the latest saved data for ",
    "Bản mới nhất vẫn được giữ làm dữ liệu đang theo dõi." to "The latest snapshot remains the active tracked data.",
    "Đã cập nhật mốc JSON hiện có" to "Updated the existing JSON snapshot",
    "JSON có cùng timestamp với mốc đã lưu; ứng dụng đã thay nội dung tại mốc đó." to "The JSON has the same timestamp as the saved snapshot; its contents have been replaced.",
    "Đã phân tích và lưu JSON" to "JSON analyzed and saved",
    "Không phát hiện thay đổi về nâng cấp hoặc cấp độ trong các mục mà bộ so sánh hiện hỗ trợ." to "No upgrade or level changes were found in the sections currently supported by the comparison tool.",
    "Dữ liệu mới đã được lưu làm mốc hiện tại." to "The new data has been saved as the current snapshot.",
    "Không thể đọc JSON này." to "Unable to read this JSON.",
    "Không thể lưu xác nhận dữ liệu tài khoản. Hãy thử lại." to "Unable to save account data consent. Please try again.",
    "NHẬP JSON ĐẦU TIÊN" to "IMPORT YOUR FIRST JSON",
    "Mã người chơi" to "Player Tag",
    "Công trình" to "Buildings",
    "NHÀ CHÍNH • ĐANG NÂNG" to "HOME VILLAGE • UPGRADING",
    "Không có tiến trình" to "No active upgrades",
    "Các mục đang nâng của Nhà chính sẽ xuất hiện ở đây." to "Home Village upgrades will appear here.",
    "LÀNG ĐÊM • ĐANG NÂNG" to "BUILDER BASE • UPGRADING",
    "Các mục đang nâng của Làng đêm sẽ xuất hiện ở đây." to "Builder Base upgrades will appear here.",
    "TRỢ THỦ" to "HELPERS",
    "Tất cả" to "All",
    "Phòng thủ" to "Defenses",
    "Tài nguyên" to "Resources",
    "Bẫy" to "Traps",
    "Khác" to "Other",
    "Anh hùng" to "Heroes",
    "JSON không có mục thuộc nhóm này." to "This JSON contains no items in this category.",
    "Chưa có làng nào" to "No villages yet",
    "Sao chép JSON trong game rồi dán vào ứng dụng để thêm làng." to "Copy JSON in the game, then paste it into the app to add a village.",
    "Chưa có lịch sử nhập" to "No import history yet",
    "Các lần dán JSON sẽ được ghi nhận tại đây." to "Your JSON imports will be recorded here.",
    "Chưa có JSON nhận từ người khác" to "No JSON received from others",
    "JSON bạn nhận và nhập từ Cộng đồng sẽ được lưu riêng ở đây. JSON tự chia sẻ của chính bạn không được đưa vào danh sách này." to "JSON received and imported from the Community is stored here separately. JSON you share yourself is not included in this list.",
    "Chưa có JSON đã gỡ" to "No removed JSON",
    "JSON đã gỡ khỏi mục ĐÃ NHẬN sẽ được lưu riêng tại đây, không làm mất bản trong LỊCH SỬ." to "JSON removed from RECEIVED is stored here separately; the copy in HISTORY is kept.",
    "Chưa có dữ liệu bị xóa" to "No deleted data",
    "Khi xóa một làng khỏi ĐANG LƯU, dữ liệu sẽ được lưu tại đây để khôi phục." to "When you delete a village from SAVED, its data is kept here so you can restore it.",
    " ngày " to " days ",
    " giờ " to " hours ",
    " phút" to " minutes",
    " giây" to " seconds",
    "Đã chia sẻ JSON vào Cộng đồng." to "JSON shared with the Community.",
    "Vừa gửi" to "Just sent",
    "Đang tải từ COC-LIO" to "Downloading from COC-LIO",
    "Hiện tại, gửi ảnh đang tạm dừng trên gói miễn phí. Bạn vẫn có thể gửi tin nhắn văn bản và chia sẻ JSON." to "Image sharing is currently paused on the free plan. You can still send text messages and share JSON.",
    "Hiện tại, gửi tệp đính kèm đang tạm dừng trên gói miễn phí." to "File attachments are currently paused on the free plan.",
    "Không tìm thấy mã JSON được chia sẻ." to "Shared JSON code not found.",
    "JSON chia sẻ không còn dữ liệu." to "The shared JSON is no longer available.",
    "JSON chia sẻ vượt quá giới hạn cho phép." to "The shared JSON exceeds the allowed limit.",
    "Đã nhận JSON. Bản nhận được đã được lưu riêng trong Kho JSON; bản nhập vẫn có trong lịch sử." to "JSON received. The received copy was saved separately in JSON Storage; the imported copy remains in history.",
    "Không thể nhận JSON được chia sẻ." to "Unable to receive the shared JSON.",
    "Không thể tải JSON cộng đồng." to "Unable to load community JSON.",
    "Không thể tải danh sách cập nhật." to "Unable to load the update list.",
    "Không thể tải kho đã gỡ." to "Unable to load the archive.",
    "Không thể kiểm tra thời hạn lưu trữ." to "Unable to check the archive retention period.",
    "Đóng bảng emoji" to "Close emoji panel",
    "Mở bảng emoji" to "Open emoji panel",
    "Dữ liệu đã gỡ được giữ lại cho đến khi quản lý xóa vĩnh viễn." to "Removed data is kept until an administrator permanently deletes it.",
    "Áp dụng thời hạn cho các bản ghi trong kho. Tệp đính kèm cũ hiện chưa thể truy cập hoặc dọn dẹp trên gói miễn phí." to "Apply a retention period to archived records. Older attachments cannot currently be accessed or cleaned up on the free plan.",
    "Nhập thời hạn từ 1 đến 1200 ngày hoặc tháng." to "Enter a retention period from 1 to 1,200 days or months.",
    "Đã lưu chế độ lưu trữ." to "Archive settings saved.",
    "Không thể lưu thời hạn lưu trữ." to "Unable to save the archive retention period.",
    "Đã gỡ và lưu vào Kho đã gỡ." to "Removed and moved to the archive.",
    "Không thể chuyển tin nhắn vào kho." to "Unable to move the message to the archive.",
    "Đã gỡ và lưu APK cập nhật." to "Update APK removed and moved to the archive.",
    "Không thể chuyển APK vào kho." to "Unable to move the APK update to the archive.",
    "Đã xóa vĩnh viễn khỏi kho." to "Permanently deleted from the archive.",
    "Không thể xóa vĩnh viễn." to "Unable to permanently delete.",
    "Ảnh đại diện" to "Profile photo",
    "HÌNH ẢNH ĐÃ GỠ" to "REMOVED IMAGE",
    "TỆP ĐÃ GỠ" to "REMOVED FILE",
    "APK CẬP NHẬT ĐÃ GỠ" to "REMOVED APK UPDATE",
    "TIN NHẮN ĐÃ GỠ" to "REMOVED MESSAGE",
    "Không rõ dung lượng" to "Unknown size",
    "Không thể hoàn tất thao tác tài khoản. Vui lòng thử lại." to "Unable to complete the account operation. Please try again.",
    "Không nhận được email từ tài khoản Google." to "No email was received from the Google account.",
    "Tài khoản COC-LIO này đã bị quản lý vô hiệu hóa. Vui lòng liên hệ quản lý để được hỗ trợ." to "This COC-LIO account has been disabled by an administrator. Contact the administrator for help.",
    "Bạn cần đăng nhập Google để đồng bộ tên hiển thị." to "Sign in with Google to synchronize your display name.",
    "Email Google chưa được xác minh." to "Your Google email has not been verified.",
    "Tên hiển thị phải có từ 1 đến 40 ký tự." to "The display name must contain 1 to 40 characters.",
    "Tài khoản quản lý" to "Administrator account",
    "Thiếu mã tài khoản cần cập nhật." to "The account ID to update is missing.",
    "Bạn cần đăng nhập tài khoản quản lý." to "You must sign in to an administrator account.",
    "Chỉ chủ sở hữu COC-LIO mới được quản lý tài khoản." to "Only the COC-LIO owner can manage accounts.",
    "Không thể tự vô hiệu hóa tài khoản quản lý đang sử dụng." to "You cannot disable the administrator account currently in use.",
    "Bạn cần đăng nhập Google để xác nhận dữ liệu tài khoản." to "Sign in with Google to confirm account data processing.",
    "Player Tag trong JSON không hợp lệ." to "The Player Tag in the JSON is invalid.",
    "JSON lớn hơn giới hạn đồng bộ miễn phí 700 KB. Bản JSON vẫn được lưu trên điện thoại." to "The JSON exceeds the 700 KB free-sync limit. The JSON is still saved on your phone.",
    "Tin nhắn nâng cấp" to "Upgrade message",
    "Chưa rõ cấp độ" to "Level unknown",
    "Làng đêm" to "Builder Base",
    "Đến giờ hoàn thành" to "Upgrade completed",
    "ĐÃ VÔ HIỆU HÓA" to "DISABLED",
    "ĐANG HOẠT ĐỘNG" to "ACTIVE",
    "Loại tài khoản" to "Account type",
    "QUẢN LÝ / CHỦ SỞ HỮU" to "ADMIN / OWNER",
    "Người dùng" to "User",
    "Trạng thái truy cập" to "Access status",
    "Tên người dùng đặt" to "Custom display name",
    "Tên Google" to "Google name",
    "Họ" to "Last name",
    "Đồng bộ hồ sơ" to "Profile synchronization",
    "Tài khoản quản lý đang dùng" to "Administrator account in use",
    "Được đồng bộ khi đăng nhập Google" to "Synchronized when signing in with Google",
    "Quyền theo dõi hoạt động" to "Activity tracking permission",
    "Đã cho phép thiết bị và hoạt động" to "Device and activity tracking allowed",
    "Chưa cho phép thiết bị và hoạt động" to "Device and activity tracking not allowed",
    "Đồng ý phân tích mức sử dụng" to "Usage analytics consent",
    "Đã cho phép" to "Allowed",
    "Chưa cho phép" to "Not allowed",
    "Đồng ý báo lỗi và hiệu năng" to "Crash and performance reporting consent",
    "Thời điểm lưu lựa chọn dữ liệu" to "Data choice saved at",
    "Chưa lưu lựa chọn" to "No choice saved",
    "Email xác minh" to "Email verification",
    "Đã xác minh" to "Verified",
    "Chưa xác minh" to "Not verified",
    "Nhà cung cấp" to "Provider",
    "Số lần đăng nhập Google" to "Google sign-in count",
    "Chưa có số liệu thống kê" to "No statistics available",
    "Mở COC-LIO lần đầu" to "First COC-LIO launch",
    "Chưa ghi nhận lần mở" to "No launch recorded",
    "Chưa có dữ liệu lịch sử theo dõi" to "No activity history available",
    "Lần mở COC-LIO gần nhất" to "Last COC-LIO launch",
    "Hoạt động gần nhất" to "Most recent activity",
    "Lần chuyển ứng dụng xuống nền" to "Last time app moved to background",
    "Sự kiện hoạt động gần nhất" to "Most recent activity event",
    "Đang sử dụng ứng dụng" to "Using the app",
    "Chuyển ứng dụng xuống nền" to "App moved to background",
    "Thời điểm cập nhật sự kiện" to "Event updated at",
    "Bắt đầu phiên mở gần nhất" to "Most recent session start",
    "Số phiên mở COC-LIO" to "Number of COC-LIO sessions",
    "Chưa có dữ liệu do chưa bật theo dõi" to "No data because tracking is not enabled",
    "Thời lượng phiên gần nhất" to "Last session duration",
    "Tổng thời gian hoạt động" to "Total active time",
    "Thiết bị" to "Device",
    "Thương hiệu / dòng / mã thiết bị" to "Device brand / model / identifier",
    "Kiến trúc CPU" to "CPU architecture",
    "Màn hình" to "Display",
    "Ngôn ngữ / múi giờ" to "Language / time zone",
    "Loại kết nối" to "Connection type",
    "Di động" to "Mobile",
    "Ngoại tuyến" to "Offline",
    "Đồng bộ JSON Clash of Clans" to "Clash of Clans JSON synchronization",
    "Player Tag gần nhất" to "Most recent Player Tag",
    "Tên làng gần nhất" to "Most recent village name",
    "Số lần đồng bộ JSON" to "JSON sync count",
    "Thời điểm JSON mới nhất" to "Latest JSON timestamp",
    "Lần đồng bộ JSON gần nhất" to "Most recent JSON sync",
    "Dung lượng JSON gần nhất" to "Latest JSON size",
    "Phiên bản COC-LIO" to "COC-LIO version",
    "Mã phiên bản ứng dụng" to "App version code",
    "Tạo hồ sơ" to "Profile created",
    "Mã tài khoản" to "Account ID",
    "Tiếng Việt" to "Vietnamese",
    "KÍCH THƯỚC GIAO DIỆN" to "INTERFACE SIZE",
    "CỠ CHỮ TOÀN ỨNG DỤNG" to "APP-WIDE TEXT SIZE",
    "Không thể hoàn tất thao tác cộng đồng. Vui lòng thử lại." to "Unable to complete the community operation. Please try again.",
    "Bạn cần đăng nhập Google để chia sẻ JSON." to "Sign in with Google to share JSON.",
    "JSON trống hoặc quá lớn. Dung lượng tối đa là 700 KB." to "The JSON is empty or too large. The maximum size is 700 KB.",
    "Đã tự dọn " to "Automatically cleaned up ",
    "Đã lưu thời hạn và dọn " to "Retention period saved; cleaned up ",
    " mục hết hạn." to " expired items.",
    "Mã không hợp lệ. Hãy nhập tag người chơi hoặc Clan dạng #ABC123." to "Invalid tag. Enter a player or Clan tag in the format #ABC123.",
    "Không tìm thấy hồ sơ hoặc dữ liệu này chưa có trong kho ClashKing." to "Profile not found, or this data is not yet available in the ClashKing database.",
    "ClashKing đang giới hạn lượt tra cứu. Vui lòng thử lại sau." to "ClashKing is rate-limiting requests. Please try again later.",
    "Máy chủ ClashKing đang gặp sự cố. Vui lòng thử lại sau." to "The ClashKing server is experiencing issues. Please try again later.",
    "Hãy nhập ít nhất 2 ký tự, tên hoặc mã tag cần tìm." to "Enter at least 2 characters, a name or a tag to search.",
    "Dữ liệu tìm kiếm không đúng định dạng." to "The search data has an invalid format.",
    "Không thể tải dữ liệu này." to "Unable to load this data.",
    "Không tìm thấy kết quả phù hợp. Hãy kiểm tra tên hoặc mã tag." to "No matching results found. Check the name or tag.",
    "Không thể tìm kiếm lúc này. Hãy thử lại." to "Search is unavailable right now. Please try again.",
    "Không có" to "None",
    "So sánh người chơi" to "Compare players",
    "Mã người chơi thứ hai không hợp lệ." to "The second player tag is invalid.",
    "Thời gian ở Clan chung" to "Shared time in Clan",
    "Người chơi" to "Player",
    "Không có / chưa có dữ liệu" to "None / no data available",
    "Mã Clan" to "Clan tag",
    "Cúp nhà chính" to "Home Village trophies",
    "Cúp Nhà thợ" to "Builder Base trophies",
    "Điểm" to "Points",
    "Huy hiệu Clan" to "Clan badge",
    "Điểm Clan" to "Clan points",
    "Vị trí" to "Location",
    "Hạng chiến tranh" to "War ranking",
    "Giải thủ đô" to "Capital League",
    "Nhận lính" to "Troops received",
    "Được nhận lính" to "Troops received",
    "Vàng thủ đô" to "Capital Gold",
    "Nhật ký chiến tranh" to "War log",
    "Công khai" to "Public",
    "Riêng tư" to "Private",
    "Thủ đô Clan" to "Clan Capital",
    "Quyên góp" to "Donations",
    "Nhận quyên góp" to "Donations received",
    "Chiến thắng chiến tranh" to "War wins",
    "Đã tải kết quả. Chọn đúng hồ sơ để xem chi tiết." to "Results loaded. Select the correct profile to view details.",
    "Chưa có dữ liệu chi tiết." to "No detailed data available.",
    "Vua man di" to "Barbarian King",
    "Nữ hoàng cung thủ" to "Archer Queen",
    "Đại hộ vệ" to "Grand Warden",
    "Cỗ máy chiến đấu" to "Battle Machine",
    "Nhà vô địch Hoàng gia" to "Royal Champion",
    "Trực thăng chiến đấu" to "Battle Copter",
    "Hoàng tử tiểu quỷ" to "Minion Prince",
    "Công tước Rồng" to "Dragon Duke",
    "Trại quân" to "Army Camp",
    "Mỏ tinh chất" to "Elixir Collector",
    "Kho tinh chất" to "Elixir Storage",
    "Mỏ vàng" to "Gold Mine",
    "Kho vàng" to "Gold Storage",
    "Doanh trại" to "Barracks",
    "Phòng thí nghiệm" to "Laboratory",
    "Đại bác" to "Cannon",
    "Tháp cung" to "Archer Tower",
    "Tường" to "Wall",
    "Tháp pháp sư" to "Wizard Tower",
    "Phòng không" to "Air Defense",
    "Cối" to "Mortar",
    "Thành bang hội" to "Clan Castle",
    "Nhà thợ xây" to "Builder's Hut",
    "Tesla ẩn" to "Hidden Tesla",
    "Nỏ X" to "X-Bow",
    "Mỏ hắc tiên" to "Dark Elixir Drill",
    "Kho hắc tiên" to "Dark Elixir Storage",
    "Nhà lính hắc tiên" to "Dark Barracks",
    "Tháp địa ngục" to "Inferno Tower",
    "Quạt gió" to "Air Sweeper",
    "Pháo đại bàng" to "Eagle Artillery",
    "Tháp bom" to "Bomb Tower",
    "Xưởng công thành" to "Siege Workshop",
    "Nhà B.O.B" to "B.O.B's Hut",
    "Tháp pháo bắn chùm" to "Scattershot",
    "Nhà thú cưng" to "Pet House",
    "Lò rèn" to "Blacksmith",
    "Sảnh anh hùng" to "Hero Hall",
    "Đại bác Monolith" to "Monolith",
    "Tháp đa nòng" to "Multi-Gear Tower",
    "Tháp cung đa mục tiêu" to "Multi-Archer Tower",
    "Đại bác dội" to "Ricochet Cannon",
    "Tháp phun lửa" to "Firespitter",
    "Nhà trợ thủ" to "Helper Hut",
    "Trạm chế tạo" to "Crafting Station",
    "Tháp pháp sư siêu cấp" to "Super Wizard Tower",
    "Tường căn cứ thợ xây" to "Builder Base Wall",
    "Nhà chính căn cứ thợ xây" to "Builder Hall",
    "Mỏ tinh chất căn cứ thợ xây" to "Builder Base Elixir Collector",
    "Kho tinh chất căn cứ thợ xây" to "Builder Base Elixir Storage",
    "Mỏ vàng căn cứ thợ xây" to "Builder Base Gold Mine",
    "Kho vàng căn cứ thợ xây" to "Builder Base Gold Storage",
    "Tháp đồng hồ" to "Clock Tower",
    "Doanh trại căn cứ thợ xây" to "Builder Barracks",
    "Đại bác đôi" to "Double Cannon",
    "Trại quân căn cứ thợ xây" to "Builder Base Army Camp",
    "Tesla ẩn căn cứ thợ xây" to "Builder Base Hidden Tesla",
    "Đại bác căn cứ thợ xây" to "Builder Base Cannon",
    "Cối đa nòng" to "Multi Mortar",
    "Phòng thí nghiệm sao" to "Star Laboratory",
    "Nhà B.O.T.O" to "O.T.T.O's Hut",
    "Tháp cung căn cứ thợ xây" to "Builder Base Archer Tower",
    "Trại tiếp viện" to "Reinforcement Camp",
    "Pháo bông" to "Firecrackers",
    "Trạm canh gác" to "Guard Post",
    "Tesla khổng lồ" to "Mega Tesla",
    "Bệ máy chiến đấu" to "Battle Machine Altar",
    "Bom bay căn cứ thợ xây" to "Builder Base Air Bombs",
    "Máy nghiền" to "Crusher",
    "Lò nung" to "Roaster",
    "Đại bác khổng lồ" to "Giant Cannon",
    "Mỏ ngọc" to "Gem Mine",
    "Máy phóng dung nham" to "Lava Launcher",
    "Trung tâm điều khiển B.O.B" to "B.O.B Control",
    "Tiền đồn O.T.T.O" to "O.T.T.O's Outpost",
    "Bệ trực thăng chiến đấu" to "Battle Copter Platform",
    "Nỏ X căn cứ thợ xây" to "Builder Base X-Bow",
    "Nhà hồi máu" to "Healing Hut",
    "Bom" to "Bomb",
    "Bẫy lò xo" to "Spring Trap",
    "Bom khổng lồ" to "Giant Bomb",
    "Bom bay" to "Air Bomb",
    "Mìn tìm nhiệt" to "Seeking Air Mine",
    "Bẫy bộ xương" to "Skeleton Trap",
    "Bẫy lò xo căn cứ thợ xây" to "Builder Base Spring Trap",
    "Bẫy đẩy căn cứ thợ xây" to "Push Trap",
    "Mìn căn cứ thợ xây" to "Builder Base Mine",
    "Mìn khổng lồ căn cứ thợ xây" to "Mega Mine",
    "Bẫy lốc xoáy" to "Tornado Trap",
    "Bom Giga" to "Giga Bomb",
    "Chiến binh man di" to "Barbarian",
    "Cung thủ" to "Archer",
    "Yêu tinh" to "Goblin",
    "Khổng lồ" to "Giant",
    "Phá tường" to "Wall Breaker",
    "Bóng bay" to "Balloon",
    "Pháp sư" to "Wizard",
    "Nữ tu hồi máu" to "Healer",
    "Rồng" to "Dragon",
    "P.E.K.K.A" to "P.E.K.K.A",
    "Tiểu quỷ" to "Minion",
    "Kỵ sĩ heo" to "Hog Rider",
    "Nữ chiến binh" to "Valkyrie",
    "Người đá" to "Golem",
    "Phù thủy" to "Witch",
    "Chó dung nham" to "Lava Hound",
    "Kẻ ném đá" to "Bowler",
    "Rồng con" to "Baby Dragon",
    "Thợ mỏ" to "Miner",
    "Máy phá tường" to "Wall Wrecker",
    "Khinh khí cầu chiến đấu" to "Battle Blimp",
    "Người tuyết Yeti" to "Yeti",
    "Người đá băng" to "Ice Golem",
    "Rồng điện" to "Electro Dragon",
    "Máy nghiền đá" to "Stone Slammer",
    "Kỵ sĩ rồng" to "Dragon Rider",
    "Thợ săn anh hùng" to "Headhunter",
    "Máy phóng khúc gỗ" to "Log Launcher",
    "Máy phun lửa" to "Flame Flinger",
    "Máy khoan chiến đấu" to "Battle Drill",
    "Titan điện" to "Electro Titan",
    "Người canh giữ tập sự" to "Apprentice Warden",
    "Phù thủy tàn tích" to "Ruin Witch",
    "Kỵ sĩ rễ cây" to "Root Rider",
    "Tu sĩ rừng" to "Druid",
    "Kẻ ném" to "Thrower",
    "Bệ phóng quân" to "Troop Launcher",
    "Lò lửa" to "Furnace",
    "Khổng lồ thiên thạch" to "Meteor Golem",
    "Xe bay" to "Flying Fortress",
    "Chiến binh man di cuồng nộ" to "Raged Barbarian",
    "Cung thủ lén lút" to "Sneaky Archer",
    "Tiểu quỷ Beta" to "Beta Minion",
    "Khổng lồ đấm bốc" to "Boxer Giant",
    "Kẻ ném bom" to "Bomber",
    "P.E.K.K.A quyền năng" to "Power P.E.K.K.A",
    "Xe pháo" to "Cannon Cart",
    "Tàu thả bom" to "Drop Ship",
    "Phù thủy đêm" to "Night Witch",
    "Kỵ sĩ heo bay" to "Hog Glider",
    "Pháp sư điện lửa" to "Electrofire Wizard",
    "L.A.S.S.I" to "L.A.S.S.I",
    "Yak dũng mãnh" to "Mighty Yak",
    "Cú điện" to "Electro Owl",
    "Kỳ lân" to "Unicorn",
    "Phượng hoàng" to "Phoenix",
    "Thằn lằn độc" to "Poison Lizard",
    "Diggy" to "Diggy",
    "Frosty băng giá" to "Frosty",
    "Cáo linh" to "Spirit Fox",
    "Thạch dẻo giận dữ" to "Angry Jelly",
    "Hắt hơi" to "Sneezy",
    "Quạ tham ăn" to "Greedy Raven",
    "Búp bê man di" to "Barbarian Puppet",
    "Lọ cuồng nộ" to "Rage Vial",
    "Búp bê cung thủ" to "Archer Puppet",
    "Lọ tàng hình" to "Invisibility Vial",
    "Sách bất tử" to "Eternal Tome",
    "Ngọc sinh mệnh" to "Life Gem",
    "Khiên truy đuổi" to "Seeking Shield",
    "Ngọc Hoàng gia" to "Royal Gem",
    "Giày động đất" to "Earthquake Boots",
    "Búp bê Kỵ sĩ heo" to "Hog Rider Puppet",
    "Găng tay khổng lồ" to "Giant Gauntlet",
    "Râu ma cà rồng" to "Vampstache",
    "Lọ tăng tốc" to "Haste Vial",
    "Giáo tên lửa" to "Rocket Spear",
    "Bóng gai" to "Spiky Ball",
    "Mũi tên băng" to "Frozen Arrow",
    "Nanh điện" to "Electric Boots",
    "Mũi tên khổng lồ" to "Giant Arrow",
    "Ngọn đuốc anh hùng" to "Heroic Torch",
    "Búp bê nữ tu hồi máu" to "Healer Puppet",
    "Cầu lửa" to "Fireball",
    "Ngọc cuồng nộ" to "Rage Gem",
    "Vòng tay rắn" to "Snake Bracelet",
    "Sách hồi máu" to "Healing Tome",
    "Vương miện bóng tối" to "Dark Crown",
    "Gương phép" to "Magic Mirror",
    "Giày điện" to "Electro Boots",
    "Búp bê Lavaloon" to "Lavaloon Puppet",
    "Búp bê tay sai" to "Minion Puppet",
    "Quả cầu bóng tối" to "Dark Orb",
    "Quần kim loại" to "Metal Pants",
    "Sắt quý tộc" to "Noble Iron",
    "Hình nhân hành động" to "Action Figure",
    "Trượng thiên thạch" to "Meteor Staff",
    "Mảnh băng" to "Ice Shard",
    "Ngựa gậy" to "Stick Horse",
    "Trái tim lửa" to "Fire Heart",
    "Ba lô tên lửa" to "Rocket Backpack",
    "Súng gây choáng" to "Stun Gun",
    "Máy thổi lửa" to "Flame Blower",
    "Mũi tên Monolith" to "Monolith Arrow",
    "Bộ bài trả thù" to "Revenge Deck",
    "Mặt dây chuyền cổng không gian" to "Portal Pendant",
    "Thợ xây tập sự" to "Apprentice Builder",
    "Trợ lý phòng thí nghiệm" to "Laboratory Assistant",
    "Người đào quặng" to "Ore Miner",
    "Lò nướng" to "Oven",
    "Nến nóng" to "Hot Candle",
    "Máy bắn bánh" to "Cake Launcher",
    "Máu Nến nóng" to "Hot Candle Hitpoints",
    "Sát thương Nến nóng" to "Hot Candle Damage",
    "Tốc độ Nến nóng" to "Hot Candle Speed",
    "Máu Thợ săn anh hùng" to "Hero Hunter Hitpoints",
    "Sát thương Thợ săn anh hùng" to "Hero Hunter Damage",
    "Tầm bắn Thợ săn anh hùng" to "Hero Hunter Range",
    "Máu Máy bắn bánh" to "Cake Launcher Hitpoints",
    "Sát thương Máy bắn bánh" to "Cake Launcher Damage",
    "Hiệu ứng Máy bắn bánh" to "Cake Launcher Effect",
    "Đồ trang trí" to "Decorations",
    "Chướng ngại vật" to "Obstacles",
    "Bộ phận nhà" to "House Parts",
    "Trang phục tướng" to "Hero Skins",
    "Phong cảnh" to "Sceneries",
    "Trang phục" to "Skins",
    "Tăng tốc phòng thí nghiệm" to "Research Boost",
    "Tăng tốc thợ xây" to "Builder Boost",
    "Tăng tốc tháp đồng hồ" to "Clock Tower Boost",
    "Tăng tốc" to "Boost",
    "Bắt đầu nâng cấp" to "Upgrade started",
    "Đã hoàn thành nâng cấp" to "Upgrade completed",
    "Nâng cấp không còn trong bộ đếm" to "Upgrade no longer appears in the countdown",
    "Ghi nhận cấp độ mới" to "New level recorded",
    "Công trình & bẫy" to "Buildings & Traps",
    "Quân đội & nghiên cứu" to "Army & Research",
    "Thợ xây" to "Builder",
    "Toàn bộ" to "All",
    "Không xác định" to "Unknown",
    "JSON đang trống." to "The JSON is empty.",
    "Không đọc được JSON. Hãy kiểm tra nội dung đã sao chép đầy đủ." to "Unable to read the JSON. Make sure the copied content is complete.",
    "JSON không có Player Tag hợp lệ." to "The JSON does not contain a valid Player Tag.",
    "JSON không có timestamp hợp lệ." to "The JSON does not contain a valid timestamp.",
    "Nhắc nâng cấp kiểu báo thức" to "Upgrade alarm reminder",
    "Thông báo báo thức tự đóng sau " to "The alarm notification closes after ",
    "Nhắc khi bộ đếm nâng cấp trong JSON đã hết thời gian" to "Reminder when the JSON upgrade countdown finishes",
    "Đây là thông báo thử kiểu báo thức. Âm thanh và rung phụ thuộc cài đặt thông báo của Android." to "This is a test alarm notification. Sound and vibration depend on Android notification settings.",
    "Đây là thông báo thử kiểu tin nhắn. Thông báo nâng cấp thật sẽ xuất hiện khi bộ đếm kết thúc." to "This is a test message notification. A real upgrade notification appears when the countdown ends.",
    "Đang thử kiểu báo thức" to "Testing alarm notification",
    "Đang thử kiểu tin nhắn" to "Testing message notification",
    "COC-LIO • Thử thông báo" to "COC-LIO • Test notification",
    "Đã chia sẻ JSON" to "Shared JSON",
    "Đã lưu dữ liệu" to "Data saved",
    "Làng" to "Village",
    "Căn cứ" to "Base",
    "Nhóm" to "Section",
    "Dự kiến" to "Expected level",
    "Bộ đếm JSON đã hết giờ; hãy nhập JSON mới để xác nhận cấp độ trong game." to "The JSON countdown has ended. Import a new JSON to confirm the level in the game.",
    "Công trình căn cứ thợ xây" to "Builder Base Buildings",
    "Bẫy căn cứ thợ xây" to "Builder Base Traps",
    "Quân đội căn cứ thợ xây" to "Builder Base Troops",
    "Anh hùng căn cứ thợ xây" to "Builder Base Heroes",
    "Nhập JSON đầu tiên" to "Import your first JSON",
    "Không có dữ liệu phụ" to "No extra data",
    "Không tìm thấy dữ liệu phù hợp." to "No matching data found.",
    "Đã đồng bộ bản JSON mới nhất" to "Synced the latest JSON snapshot",
    "Máy công thành" to "Siege Machines",
    "Trợ thủ" to "Helpers",
    "Mục căn cứ thợ xây" to "Builder Base Item",
    "Mục" to "Item",
    "BẢN GHI CÒN LƯU TRONG FIREBASE" to "RECORDS STILL STORED IN FIREBASE",
    "Bản ghi và dữ liệu đính kèm/JSON được lưu bên trong bản ghi này sẽ bị xóa vĩnh viễn khỏi Firestore." to "This record and any attachment or JSON data stored inside it will be permanently deleted from Firestore.",
    "Bản lưu hiện chỉ có thông tin phiên bản, chưa có đường dẫn tải APK." to "This archive only contains version information; no APK download link is available.",
    "BẬT" to "ON",
    "TẮT" to "OFF",
    "Bộ đếm lấy tổng số tài liệu thật từ các mục Cộng đồng và Kho đã gỡ trên Firestore; không tính hồ sơ, cài đặt và dữ liệu khác của ứng dụng." to "The counter totals actual documents in the Community and Removed Archive collections in Firestore. It excludes profiles, settings and other app data.",
    "Danh sách lấy từ hồ sơ Firestore. Nếu bạn đã xóa tài khoản trong Firebase Authentication nhưng dòng này vẫn còn, hãy dùng XÓA HỒ SƠ sau khi kiểm tra đúng UID. Nút này chỉ xóa hồ sơ Firestore, không xóa tài khoản Authentication." to "This list comes from Firestore profiles. If you deleted an account from Firebase Authentication but it still appears here, verify the UID and use DELETE PROFILE. This button deletes only the Firestore profile, not the Authentication account.",
    "Đang kiểm tra dữ liệu trên máy chủ…" to "Checking data on the server…",
    "Độ sáng" to "Brightness",
    "HƯỚNG DẪN VÀ LIÊN KẾT" to "GUIDES & LINKS",
    "JSON gốc không có trong bản lưu cũ." to "The original JSON is not available in this older archive.",
    "Không thể tải JSON gốc từ bản lưu cũ vì tài liệu JSON riêng không còn trong kho." to "The original JSON cannot be downloaded from this older archive because its separate JSON document is no longer available.",
    "LÀM MỚI SỐ LIỆU" to "REFRESH STATISTICS",
    "LƯU THIẾT LẬP" to "SAVE SETTINGS",
    "Số ngày giữ nội dung" to "Content retention days",
    "TỰ ĐỘNG XÓA TOÀN BỘ CỘNG ĐỒNG" to "AUTOMATICALLY DELETE ALL COMMUNITY CONTENT",
    "XÓA HỒ SƠ FIREBASE" to "DELETE FIREBASE PROFILE",
    "Xóa hồ sơ Firebase cũ?" to "Delete old Firebase profile?",
    "XÓA TẤT CẢ" to "DELETE ALL",
    "XÓA VĨNH VIỄN TẤT CẢ" to "DELETE ALL PERMANENTLY",
    "Xóa vĩnh viễn toàn bộ Kho đã gỡ?" to "Permanently delete the entire Removed Archive?",
    "Tất cả tin nhắn, ảnh, tệp và APK trong Kho đã gỡ sẽ bị xóa khỏi Firestore. Nội dung đang còn trong Cộng đồng sẽ không bị ảnh hưởng. Thao tác này không thể hoàn tác." to "All messages, images, files and APKs in the Removed Archive will be deleted from Firestore. Content still in the Community will not be affected. This action cannot be undone.",
    "TRA CỨU NGƯỜI CHƠI & BANG HỘI" to "PLAYER & CLAN LOOKUP",
    "Tìm hồ sơ người chơi hoặc bang hội bằng tên hoặc mã trong game." to "Find player or Clan profiles by in-game name or tag.",
    "BANG HỘI" to "CLAN",
    "Tên hoặc mã người chơi" to "Name or Player Tag",
    "Tên hoặc mã bang hội" to "Name or Clan Tag",
    "Có thể tìm bằng tên người chơi, tên bang hội hoặc mã người chơi / mã bang hội. Không cần nhập Supercell ID hay email." to "Search by player name, Clan name, Player Tag or Clan Tag. No Supercell ID or email is required.",
    "Mã người chơi thứ hai" to "Second Player Tag",
    "SO SÁNH THỜI GIAN TRONG BANG HỘI" to "COMPARE CLAN TENURE",
    "Mã cấp bậc giải đấu" to "League Tier ID",
    "Mã khu vực / quốc gia" to "Region / country code (Location ID)",
    "Nguồn dữ liệu: ClashKing · Mã nhà sáng tạo: ClashKing" to "Data source: ClashKing · Creator Code: ClashKing",
    "Mã bang hội" to "Clan Tag",
    "Bang hội" to "Clan",
    "Cấp bang hội" to "Clan Level",
    "Điểm bang hội" to "Clan Points",
    "Thời gian trong bang hội chung" to "Shared time in Clan",
    "Huy hiệu bang hội" to "Clan badge",
    "XẾP HẠNG BANG HỘI" to "CLAN RANKING",
    "Thủ đô bang hội" to "Clan Capital",
    "Top quyên góp bang hội" to "Top Clan donations",
    "Top số trận thắng chiến tranh bang hội" to "Top Clan war wins",
    "Top vàng Thủ đô bang hội" to "Top Clan Capital Gold",
    "Số lượng bang hội theo khu vực" to "Clan counts by region",
    "Số lượng bang hội theo CWL" to "Clan counts by CWL",
    "Số lượng bang hội theo giải Thủ đô" to "Clan counts by Capital League",
    "Phân bố quy mô bang hội" to "Clan size distribution",
    "Top chuỗi thắng bang hội" to "Top Clan win streaks",
    "Lịch sử gia nhập/rời bang hội" to "Clan join/leave history",
    "Thống kê thời gian trong bang hội" to "Clan tenure statistics",
    "Lịch sử Trò chơi bang hội" to "Clan Games history",
    "Thay đổi hồ sơ bang hội" to "Clan profile changes",
    "Tổng kết xếp hạng bang hội Nhà chính" to "Clan Town Hall ranking summary",
    "Tổng kết xếp hạng bang hội Nhà thợ" to "Clan Builder Hall ranking summary",
    "Tổng kết xếp hạng bang hội Thủ đô" to "Clan Capital ranking summary",
    "Lịch sử Legend của bang hội" to "Clan Legend history",
    "Top Legend của bang hội" to "Top Clan Legend",
    "Kỷ lục bang hội" to "Clan records",
    "Bảng xếp hạng bang hội" to "Clan leaderboard",
    "Mã người chơi gần nhất" to "Most recent Player Tag",
    "Mã người chơi: " to "Player Tag: ",
    "Mã không hợp lệ. Hãy nhập mã người chơi hoặc bang hội dạng #ABC123." to "Invalid tag. Enter a player or Clan tag in the format #ABC123.",
    "Mã người chơi trong JSON không hợp lệ." to "The player tag in the JSON is invalid.",
    "JSON không có mã người chơi hợp lệ." to "The JSON does not contain a valid player tag.",
    "Lấy JSON trực tiếp từ trò chơi để COC-LIO đọc cấp độ, công trình đang nâng và thời gian còn lại." to "Import JSON directly from the game so COC-LIO can read levels, ongoing upgrades and remaining time.",
    "Nút mở trò chơi chỉ đưa bạn vào trò chơi. Bạn cần tự thao tác xuất và sao chép JSON trong trò chơi." to "The game button only opens the game. You must export and copy the JSON yourself inside the game.",
    "Sao chép JSON trong trò chơi rồi dán vào ứng dụng để thêm làng." to "Copy JSON in the game, then paste it into the app to add a village.",
    "Tìm hồ sơ người chơi hoặc bang hội bằng tên hoặc mã trong trò chơi." to "Find player or Clan profiles by in-game name or tag.",
    "Bộ đếm JSON đã hết giờ; hãy nhập JSON mới để xác nhận cấp độ trong trò chơi." to "The JSON countdown has ended. Import a new JSON to confirm the level in the game.",
    "MỞ TRÒ CHƠI" to "OPEN GAME",
    "QUẢN LÝ" to "ADMIN",
    "TẤN CÔNG NHANH" to "GEAR-UP",
    "Đăng nhập gần nhất" to "Last sign-in",
    "Cập nhật hồ sơ" to "Profile updated",
    "Trường dữ liệu" to "Data field",
    "Giải chiến tranh" to "War League",
    "Giải Thủ đô" to "Capital League",
    "Đang chuẩn bị" to "Preparing",
    "Đang chiến tranh" to "In war",
    "Đã kết thúc" to "Ended",
    "Không tham gia chiến tranh" to "Not in war",
    "Đang hoạt động" to "Active",
    "Không hoạt động" to "Inactive",
    "Đang chờ" to "Pending",
    "Đã hoàn tất" to "Completed",
    "Thắng" to "Win",
    "Thua" to "Loss",
    "Hòa" to "Tie",
    "Hạng Đồng" to "Bronze League",
    "Hạng Bạc" to "Silver League",
    "Hạng Vàng" to "Gold League",
    "Hạng Pha lê" to "Crystal League",
    "Hạng Cao thủ" to "Master League",
    "Hạng Vô địch" to "Champion League",
    "Hạng Titan" to "Titan League",
    "Hạng Huyền thoại" to "Legend League",
    "Chưa xếp hạng" to "Unranked",
    "Kết quả" to "Result",
    "TÀI KHOẢN GOOGLE ĐÃ CHỌN" to "SELECTED GOOGLE ACCOUNT",
    "ĐANG MỞ GOOGLE..." to "OPENING GOOGLE...",
    "KHÔNG ĐỒNG Ý / HỦY" to "DECLINE / CANCEL",
    "Dữ liệu hoạt động, thiết bị, phân tích, báo lỗi và đồng bộ JSON là các mục riêng; mặc định tắt và không được bật chỉ bằng sự đồng ý này." to "Activity, device, analytics, diagnostics and JSON synchronization are separate options. They are off by default and are not enabled by this consent.",
    "Bạn đã hủy chọn tài khoản Google." to "You cancelled Google account selection.",
    "Bạn đã hủy xác nhận và giữ nguyên tài khoản đang sử dụng." to "You cancelled consent and kept the current account.",
    "Bạn cần chọn tài khoản Google trước." to "Select a Google account first.",
    "Không thể hoàn tất xác nhận dữ liệu tài khoản. Hãy thử lại." to "Unable to complete account data consent. Please try again.",
    "CHIA SẺ" to "SHARING",
    "ĐÃ GỬI" to "SENT",
    "XÓA JSON ĐÃ GỬI" to "DELETE SENT JSON",
    "Bạn chưa chia sẻ JSON nào." to "You haven't shared any JSON yet.",
    "Bạn chưa nhận JSON nào từ người khác." to "You haven't received any JSON from others.",
    "Xóa JSON đã gửi?" to "Delete sent JSON?",
    "JSON này sẽ bị xóa khỏi danh sách chia sẻ và bài đăng JSON tương ứng trong Cộng đồng. Bản JSON trong Lịch sử và Đang lưu trên thiết bị vẫn được giữ." to "This JSON will be removed from your sharing list and its matching Community post. The copy in History and Saved on this device will remain.",
    "Bạn chỉ có thể xóa JSON do chính mình chia sẻ." to "You can only delete JSON that you shared yourself.",
    "Đã xóa JSON đã gửi khỏi Cộng đồng." to "Sent JSON was deleted from the Community.",
    "Đã xóa JSON nhận được khỏi danh sách chia sẻ. Bản trong lịch sử vẫn được giữ." to "Received JSON was removed from the sharing list. Its copy in History is still kept.",
    "NHẬP LẠI" to "IMPORT AGAIN",
    "XÓA JSON" to "DELETE JSON",
    "Không nhận được thông tin đăng nhập Google hợp lệ." to "No valid Google sign-in credential was received.",
    "Google không trả về địa chỉ email hợp lệ." to "Google did not return a valid email address.",
    "Không nhận được tài khoản Google sau khi xác thực." to "No Google account was received after authentication.",
    "Bạn cần đăng nhập Google trước." to "You must sign in with Google first.",
    "Tài khoản Google hiện tại không khớp với hồ sơ vừa chọn." to "The current Google account does not match the selected profile.",
    "Không thể mở hồ sơ này." to "Unable to open this profile.",
    "Không thể tải thêm kết quả." to "Unable to load more results.",
    "Tháp phản công" to "Revenge Tower",
    "Clan" to "Clan",
    "Phiên Google trên điện thoại cần xác thực lại. Hãy kiểm tra tài khoản Google trên thiết bị rồi thử lại. Nếu vẫn lỗi, kiểm tra OAuth Android của COC-LIO." to "The Google session on this device needs to be re-authenticated. Check the Google account on your device and try again. If the issue persists, check COC-LIO's Android OAuth configuration.",
    "Không tìm thấy tài khoản Google có thể đăng nhập. Hãy kiểm tra tài khoản Google trên điện thoại rồi thử lại." to "No Google account available for sign-in was found. Check the Google accounts on your phone and try again.",
)

private fun translateAppText(source: String, language: AppUiLanguage): String {
    val normalizedSource = source.removePrefix("# ID: ").removePrefix("ID: ")
    if (normalizedSource != source) return translateAppText(normalizedSource, language)
    RemoteAppConfigManager.config.value.translation(source, language)?.let { return it }
    if (language != AppUiLanguage.ENGLISH) return source
    englishTranslations[source]?.let { return it }
    return when {
        Regex("^(.+) • (\\d+) mốc • (.+)$").matchEntire(source) != null ->
            Regex("^(.+) • (\\d+) mốc • (.+)$").matchEntire(source)!!.let {
                "${it.groupValues[1]} • ${it.groupValues[2]} snapshots • ${it.groupValues[3]}"
            }
        Regex("^(.+) đã bị quản lý vô hiệu hóa$").matchEntire(source) != null ->
            Regex("^(.+) đã bị quản lý vô hiệu hóa$").matchEntire(source)!!.let {
                "${it.groupValues[1]} has been disabled by an administrator"
            }
        Regex("^Google từ chối cấu hình OAuth \\[(.+?)\\]\\. Web Client ID đang nhúng trong APK: (.+)\\. SHA-1 thực tế của APK: (.+)\\. Kiểm tra GOOGLE_WEB_CLIENT_ID phải là OAuth Client loại Web application; Android OAuth Client phải khớp package và SHA-1 thực tế; hai Client phải thuộc cùng dự án Google Cloud\\.$").matchEntire(source) != null ->
            Regex("^Google từ chối cấu hình OAuth \\[(.+?)\\]\\. Web Client ID đang nhúng trong APK: (.+)\\. SHA-1 thực tế của APK: (.+)\\. Kiểm tra GOOGLE_WEB_CLIENT_ID phải là OAuth Client loại Web application; Android OAuth Client phải khớp package và SHA-1 thực tế; hai Client phải thuộc cùng dự án Google Cloud\\.$").matchEntire(source)!!.let {
                "Google rejected OAuth configuration [${it.groupValues[1]}]. Web Client ID embedded in APK: ${it.groupValues[2]}. Actual APK SHA-1: ${it.groupValues[3]}. Verify that GOOGLE_WEB_CLIENT_ID is a Web application OAuth Client; the Android OAuth Client must match the actual package and SHA-1; both clients must belong to the same Google Cloud project."
            }
        Regex("^Google vẫn từ chối xác thực sau khi làm mới phiên\\. Web Client ID đang nhúng trong APK: (.+)\\. SHA-1 thực tế: (.+)\\. Kiểm tra Android OAuth Client theo đúng SHA-1 này và dùng Web application Client ID của cùng dự án\\.$").matchEntire(source) != null ->
            Regex("^Google vẫn từ chối xác thực sau khi làm mới phiên\\. Web Client ID đang nhúng trong APK: (.+)\\. SHA-1 thực tế: (.+)\\. Kiểm tra Android OAuth Client theo đúng SHA-1 này và dùng Web application Client ID của cùng dự án\\.$").matchEntire(source)!!.let {
                "Google still rejected authentication after refreshing the session. Web Client ID embedded in APK: ${it.groupValues[1]}. Actual SHA-1: ${it.groupValues[2]}. Check the Android OAuth Client against this SHA-1 and use the Web application Client ID from the same project."
            }
        Regex("^Thao tác này chỉ xóa hồ sơ users/(.+), dữ liệu JSON đồng bộ theo làng và giấy phép của hồ sơ khỏi Firestore\\. Nó KHÔNG xóa tài khoản đăng nhập trong Firebase Authentication\\. Chỉ dùng sau khi bạn đã xóa tài khoản đó ở Authentication\\. Bài đăng Cộng đồng không bị xóa\\.$").matchEntire(source) != null ->
            Regex("^Thao tác này chỉ xóa hồ sơ users/(.+), dữ liệu JSON đồng bộ theo làng và giấy phép của hồ sơ khỏi Firestore\\. Nó KHÔNG xóa tài khoản đăng nhập trong Firebase Authentication\\. Chỉ dùng sau khi bạn đã xóa tài khoản đó ở Authentication\\. Bài đăng Cộng đồng không bị xóa\\.$").matchEntire(source)!!.let {
                "This only deletes the users/${it.groupValues[1]} profile, the village-synced JSON data and the profile's entitlements from Firestore. It does NOT delete the sign-in account in Firebase Authentication. Use this only after deleting that account from Authentication. Community posts are not deleted."
            }
        Regex("^TỔNG CỘNG: (\\d+) BẢN GHI$").matchEntire(source) != null ->
            Regex("^TỔNG CỘNG: (\\d+) BẢN GHI$").matchEntire(source)!!.let { "TOTAL: ${it.groupValues[1]} RECORDS" }
        Regex("^Cộng đồng: (\\d+)  •  Kho đã gỡ: (\\d+)$").matchEntire(source) != null ->
            Regex("^Cộng đồng: (\\d+)  •  Kho đã gỡ: (\\d+)$").matchEntire(source)!!.let { "Community: ${it.groupValues[1]}  •  Removed archive: ${it.groupValues[2]}" }
        Regex("^Cộng đồng: (\\d+) mục tin nhắn/ảnh/tệp, (\\d+) JSON riêng, (\\d+) APK$").matchEntire(source) != null ->
            Regex("^Cộng đồng: (\\d+) mục tin nhắn/ảnh/tệp, (\\d+) JSON riêng, (\\d+) APK$").matchEntire(source)!!.let {
                "Community: ${it.groupValues[1]} message/image/file items, ${it.groupValues[2]} separate JSON records, ${it.groupValues[3]} APKs"
            }
        Regex("^Kho đã gỡ: (\\d+) tin nhắn, (\\d+) ảnh, (\\d+) tệp và APK$").matchEntire(source) != null ->
            Regex("^Kho đã gỡ: (\\d+) tin nhắn, (\\d+) ảnh, (\\d+) tệp và APK$").matchEntire(source)!!.let {
                "Removed archive: ${it.groupValues[1]} messages, ${it.groupValues[2]} images, ${it.groupValues[3]} files and APKs"
            }
        source.startsWith("không đọc được") ->
            "Unable to read" + source.removePrefix("không đọc được")
        Regex("^(.+) • cần nhập lại để xác nhận cấp độ mới$").matchEntire(source) != null ->
            Regex("^(.+) • cần nhập lại để xác nhận cấp độ mới$").matchEntire(source)!!.let {
                (englishTranslations[it.groupValues[1]] ?: it.groupValues[1]) + " • import again to confirm the new level"
            }
        Regex("^Mục (\\d+)$").matchEntire(source) != null ->
            Regex("^Mục (\\d+)$").matchEntire(source)!!.let { "Item ${it.groupValues[1]}" }
        Regex("^(.+) · (.+) · Tải tệp hiện tạm ngừng trên gói miễn phí\\.$").matchEntire(source) != null ->
            Regex("^(.+) · (.+) · Tải tệp hiện tạm ngừng trên gói miễn phí\\.$").matchEntire(source)!!.let {
                "${it.groupValues[1]} · ${it.groupValues[2]} · File downloads are currently paused on the free plan."
            }
        Regex("^(.+): (.+)$").matchEntire(source)?.takeIf { it.groupValues[1] in englishTranslations } != null ->
            Regex("^(.+): (.+)$").matchEntire(source)!!.let { match ->
                val detail = match.groupValues[2].split(", ").joinToString(", ") { module ->
                    val levelMatch = Regex("^(.+) cấp (\\d+)$").matchEntire(module)
                    if (levelMatch == null) translateAppText(module, AppUiLanguage.ENGLISH)
                    else (englishTranslations[levelMatch.groupValues[1]] ?: levelMatch.groupValues[1]) + " level " + levelMatch.groupValues[2]
                }
                (englishTranslations[match.groupValues[1]] ?: match.groupValues[1]) + ": " + detail
            }
        Regex("^(.+)  •  Nhà chính (.+)  •  Nhà thợ (.+)  •  (\\d+) mục$").matchEntire(source) != null ->
            Regex("^(.+)  •  Nhà chính (.+)  •  Nhà thợ (.+)  •  (\\d+) mục$").matchEntire(source)!!.let {
                "${it.groupValues[1]}  •  Town Hall ${it.groupValues[2]}  •  Builder Hall ${it.groupValues[3]}  •  ${it.groupValues[4]} items"
            }
        Regex("^(.+)  •  Nhà chính (.+)  •  Nhà thợ (.+)  •  (\\d+) mốc  •  (\\d+) đang nâng$").matchEntire(source) != null ->
            Regex("^(.+)  •  Nhà chính (.+)  •  Nhà thợ (.+)  •  (\\d+) mốc  •  (\\d+) đang nâng$").matchEntire(source)!!.let {
                "${it.groupValues[1]}  •  Town Hall ${it.groupValues[2]}  •  Builder Hall ${it.groupValues[3]}  •  ${it.groupValues[4]} snapshots  •  ${it.groupValues[5]} upgrades in progress"
            }
        Regex("^(.+) cấp (\\d+)$").matchEntire(source) != null ->
            Regex("^(.+) cấp (\\d+)$").matchEntire(source)!!.let { match ->
                (englishTranslations[match.groupValues[1]] ?: match.groupValues[1]) + " level " + match.groupValues[2]
            }
        Regex("^(.+) • cấp (.+)$").matchEntire(source) != null ->
            Regex("^(.+) • cấp (.+)$").matchEntire(source)!!.let { match ->
                (englishTranslations[match.groupValues[1]] ?: match.groupValues[1]) + " • level " + match.groupValues[2]
            }
        Regex("^(\\d+) mục • (\\d+) nâng$").matchEntire(source) != null ->
            Regex("^(\\d+) mục • (\\d+) nâng$").matchEntire(source)!!.let { "${it.groupValues[1]} items • ${it.groupValues[2]} upgrades" }
        Regex("^ID: (.+)  •  TH (.+)  •  BH (.+)  •  (\\d+) mốc  •  (\\d+) đang nâng$").matchEntire(source) != null ->
            Regex("^ID: (.+)  •  TH (.+)  •  BH (.+)  •  (\\d+) mốc  •  (\\d+) đang nâng$").matchEntire(source)!!.let {
                "ID: ${it.groupValues[1]}  •  TH ${it.groupValues[2]}  •  BH ${it.groupValues[3]}  •  ${it.groupValues[4]} snapshots  •  ${it.groupValues[5]} upgrading"
            }
        Regex("^(.+)  •  TH (.+)  •  BH (.+)  •  (\\d+) mục$").matchEntire(source) != null ->
            Regex("^(.+)  •  TH (.+)  •  BH (.+)  •  (\\d+) mục$").matchEntire(source)!!.let {
                "${it.groupValues[1]}  •  TH ${it.groupValues[2]}  •  BH ${it.groupValues[3]}  •  ${it.groupValues[4]} items"
            }
        Regex("^Lần đăng nhập gần nhất · (.+)$").matchEntire(source) != null ->
            "Last sign-in · " + source.removePrefix("Lần đăng nhập gần nhất · ")
        Regex("^Nhắc nâng cấp kiểu báo thức • (\\d+)s$").matchEntire(source) != null ->
            Regex("^Nhắc nâng cấp kiểu báo thức • (\\d+)s$").matchEntire(source)!!.let { "Upgrade alarm reminder • ${it.groupValues[1]}s" }
        Regex("^Thông báo báo thức tự đóng sau (\\d+) giây$").matchEntire(source) != null ->
            Regex("^Thông báo báo thức tự đóng sau (\\d+) giây$").matchEntire(source)!!.let { "Alarm notification closes after ${it.groupValues[1]} seconds" }
        Regex("^ClashKing trả về lỗi HTTP (.+)\\.$").matchEntire(source) != null ->
            Regex("^ClashKing trả về lỗi HTTP (.+)\\.$").matchEntire(source)!!.let { "ClashKing returned HTTP error ${it.groupValues[1]}." }
        Regex("^Chưa có hồ sơ bang hội (.+) trong kho dữ liệu ClashKing\\.$").matchEntire(source) != null ->
            Regex("^Chưa có hồ sơ bang hội (.+) trong kho dữ liệu ClashKing\\.$").matchEntire(source)!!.let { "No Clan profile for ${it.groupValues[1]} is available in the ClashKing database." }
        Regex("^Bản ghi (.+) sẽ được chuyển vào kho đã gỡ\\. Tệp APK cũ vẫn được giữ nhưng hiện không thể truy cập\\.$").matchEntire(source) != null ->
            Regex("^Bản ghi (.+) sẽ được chuyển vào kho đã gỡ\\. Tệp APK cũ vẫn được giữ nhưng hiện không thể truy cập\\.$").matchEntire(source)!!.let {
                "Update ${it.groupValues[1]} will be moved to the archive. The old APK file will be retained but will not be accessible."
            }
        source.startsWith("ĐANG TẢI ") ->
            "LOADING " + translateAppText(source.removePrefix("ĐANG TẢI ").removeSuffix("..."), AppUiLanguage.ENGLISH) + "..."
        source.startsWith("Tăng tốc ") ->
            "Boost " + source.removePrefix("Tăng tốc ")
        source.startsWith("Dữ liệu JSON: ") ->
            "JSON data: " + source.removePrefix("Dữ liệu JSON: ")
        source.startsWith("Máy công thành") ->
            "Siege Machines" + source.removePrefix("Máy công thành")
        Regex("^Đăng nhập (\\d+) lần · (.+)$").matchEntire(source)?.let {
            "Signed in ${it.groupValues[1]} times · ${it.groupValues[2]}"
        } != null -> Regex("^Đăng nhập (\\d+) lần · (.+)$").matchEntire(source)!!.let {
            "Signed in ${it.groupValues[1]} times · ${it.groupValues[2]}"
        }
        Regex("^Đã tải (\\d+) kết quả\\. Chọn đúng hồ sơ để xem chi tiết\\.$").matchEntire(source)?.let {
            "Loaded ${it.groupValues[1]} results. Select the correct profile to view details."
        } != null -> Regex("^Đã tải (\\d+) kết quả\\. Chọn đúng hồ sơ để xem chi tiết\\.$").matchEntire(source)!!.let {
            "Loaded ${it.groupValues[1]} results. Select the correct profile to view details."
        }
        Regex("^Phát hiện (\\d+) thay đổi$").matchEntire(source) != null ->
            Regex("^Phát hiện (\\d+) thay đổi$").matchEntire(source)!!.let { "Detected ${it.groupValues[1]} changes" }
        Regex("^Đã tự dọn (\\d+) mục lưu trữ hết hạn\\.$").matchEntire(source) != null ->
            Regex("^Đã tự dọn (\\d+) mục lưu trữ hết hạn\\.$").matchEntire(source)!!.let { "Automatically cleaned up ${it.groupValues[1]} expired archive items." }
        Regex("^Đã lưu thời hạn và dọn (\\d+) mục hết hạn\\.$").matchEntire(source) != null ->
            Regex("^Đã lưu thời hạn và dọn (\\d+) mục hết hạn\\.$").matchEntire(source)!!.let { "Retention period saved; cleaned up ${it.groupValues[1]} expired items." }
        Regex("^Thống kê Ranked TH(\\d+) – Giải (\\d+)$").matchEntire(source) != null ->
            Regex("^Thống kê Ranked TH(\\d+) – Giải (\\d+)$").matchEntire(source)!!.let { "Ranked statistics TH${it.groupValues[1]} – League ${it.groupValues[2]}" }
        Regex("^Hạng khu vực (.+)$").matchEntire(source) != null ->
            "Regional rank " + source.removePrefix("Hạng khu vực ")
        Regex("^Nhà chính (\\d+)  •  Bang hội (.+)$").matchEntire(source) != null ->
            Regex("^Nhà chính (\\d+)  •  Bang hội (.+)$").matchEntire(source)!!.let { "Town Hall ${it.groupValues[1]}  •  Clan ${it.groupValues[2]}" }
        Regex("^Cấp bang hội (\\d+)  •  (\\d+) thành viên$").matchEntire(source) != null ->
            Regex("^Cấp bang hội (\\d+)  •  (\\d+) thành viên$").matchEntire(source)!!.let { "Clan Level ${it.groupValues[1]}  •  ${it.groupValues[2]} members" }
        Regex("^(\\d+) thành viên$").matchEntire(source) != null ->
            Regex("^(\\d+) thành viên$").matchEntire(source)!!.let { "${it.groupValues[1]} members" }
        Regex("^(\\d+) mục đang nâng • Thợ rảnh (\\d+)/(\\d+)$").matchEntire(source) != null ->
            Regex("^(\\d+) mục đang nâng • Thợ rảnh (\\d+)/(\\d+)$").matchEntire(source)!!.let {
                "${it.groupValues[1]} upgrades in progress • Builders available: ${it.groupValues[2]}/${it.groupValues[3]}"
            }
        Regex("^Có (\\d+) mục dữ liệu\\.$").matchEntire(source) != null ->
            Regex("^Có (\\d+) mục dữ liệu\\.$").matchEntire(source)!!.let { "Contains ${it.groupValues[1]} data items." }
        Regex("^Đang hiển thị (\\d+) / (\\d+) mục\\.$").matchEntire(source) != null ->
            Regex("^Đang hiển thị (\\d+) / (\\d+) mục\\.$").matchEntire(source)!!.let {
                "Showing ${it.groupValues[1]} / ${it.groupValues[2]} items."
            }
        Regex("^CẤP (\\d+) → (\\d+) • CÒN (\\d+)%$").matchEntire(source) != null ->
            Regex("^CẤP (\\d+) → (\\d+) • CÒN (\\d+)%$").matchEntire(source)!!.let {
                "LEVEL ${it.groupValues[1]} → ${it.groupValues[2]} • ${it.groupValues[3]}% LEFT"
            }
        Regex("^(\\d+) mục$").matchEntire(source) != null ->
            Regex("^(\\d+) mục$").matchEntire(source)!!.let { "${it.groupValues[1]} items" }
        Regex("^(\\d+) nâng$").matchEntire(source) != null ->
            Regex("^(\\d+) nâng$").matchEntire(source)!!.let { "${it.groupValues[1]} upgrades" }
        Regex("^(\\d+) mốc$").matchEntire(source) != null ->
            Regex("^(\\d+) mốc$").matchEntire(source)!!.let { "${it.groupValues[1]} snapshots" }
        Regex("^cấp (\\d+)$").matchEntire(source) != null ->
            Regex("^cấp (\\d+)$").matchEntire(source)!!.let { "level ${it.groupValues[1]}" }
        Regex("^Cấp (\\d+)$").matchEntire(source) != null ->
            Regex("^Cấp (\\d+)$").matchEntire(source)!!.let { "Level ${it.groupValues[1]}" }
        Regex("^(\\d+) mục dữ liệu\\.$").matchEntire(source) != null ->
            Regex("^(\\d+) mục dữ liệu\\.$").matchEntire(source)!!.let { "${it.groupValues[1]} data items." }
        Regex("^(\\d+) mục\\.$").matchEntire(source) != null ->
            Regex("^(\\d+) mục\\.$").matchEntire(source)!!.let { "${it.groupValues[1]} items." }
        Regex("^(?:(\\d+) ngày )?(?:(\\d+) giờ )?(?:(\\d+) phút|(\\d+) giây)$").matchEntire(source) != null ->
            Regex("^(?:(\\d+) ngày )?(?:(\\d+) giờ )?(?:(\\d+) phút|(\\d+) giây)$").matchEntire(source)!!.let { match ->
                buildList {
                    match.groupValues[1].takeIf { it.isNotEmpty() }?.let { add("$it days") }
                    match.groupValues[2].takeIf { it.isNotEmpty() }?.let { add("$it hours") }
                    match.groupValues[3].takeIf { it.isNotEmpty() }?.let { add("$it minutes") }
                    match.groupValues[4].takeIf { it.isNotEmpty() }?.let { add("$it seconds") }
                }.joinToString(" ")
            }
        Regex("^(\\d+) ngày (.+)$").matchEntire(source) != null ->
            Regex("^(\\d+) ngày (.+)$").matchEntire(source)!!.let { "${it.groupValues[1]} days ${it.groupValues[2]}" }
        source.startsWith("Đang tải ") ->
            "Loading " + source.removePrefix("Đang tải ")
        source.startsWith("Đã lưu dữ liệu cho ") ->
            "Saved data for " + source.removePrefix("Đã lưu dữ liệu cho ")
        source.startsWith("JSON này có thời điểm cũ hơn dữ liệu mới nhất đã lưu cho ") ->
            "This JSON is older than the latest saved data for " + source.removePrefix("JSON này có thời điểm cũ hơn dữ liệu mới nhất đã lưu cho ")
        source.startsWith("Đã chia sẻ JSON ") ->
            "Shared JSON " + source.removePrefix("Đã chia sẻ JSON ")
        source.startsWith("JSON làng ") ->
            "Village JSON " + source.removePrefix("JSON làng ")
        source.startsWith("Làng #") ->
            "Village " + source.removePrefix("Làng ")
        source.endsWith(" • LÀNG ĐÊM") -> {
            val name = source.removeSuffix(" • LÀNG ĐÊM")
            (englishTranslations[name] ?: name) + " • BUILDER BASE"
        }
        source.endsWith(" • NHÀ CHÍNH") -> {
            val name = source.removeSuffix(" • NHÀ CHÍNH")
            (englishTranslations[name] ?: name) + " • HOME VILLAGE"
        }
        Regex("^(.+) ×(\\d+)$").matchEntire(source) != null -> {
            val match = Regex("^(.+) ×(\\d+)$").matchEntire(source)!!
            val itemName = match.groupValues[1]
            (englishTranslations[itemName] ?: itemName) + " ×" + match.groupValues[2]
        }
        source.startsWith("ĐẾM NGƯỢC") -> source
        source.startsWith("Tổng số tài khoản: ") -> "Total accounts: " + source.removePrefix("Tổng số tài khoản: ")
        Regex("^Tổng số tài khoản: (\\d+)$").matchEntire(source) != null ->
            Regex("^Tổng số tài khoản: (\\d+)$").matchEntire(source)!!.let { "Total accounts: ${it.groupValues[1]}" }
        Regex("^(.+) • (\\d+) mốc • (.+)$").matchEntire(source) != null ->
            Regex("^(.+) • (\\d+) mốc • (.+)$").matchEntire(source)!!.let {
                "${it.groupValues[1]} • ${it.groupValues[2]} entries • ${it.groupValues[3]}"
            }
        Regex("^Bản ghi (.+) sẽ được chuyển vào kho đã gỡ\\. Tệp APK cũ vẫn được giữ nhưng hiện không thể truy cập\\.$").matchEntire(source) != null ->
            Regex("^Bản ghi (.+) sẽ được chuyển vào kho đã gỡ\\. Tệp APK cũ vẫn được giữ nhưng hiện không thể truy cập\\.$").matchEntire(source)!!.let {
                "Release ${it.groupValues[1]} will be moved to the removed archive. The old APK file will be kept but will not be accessible for now."
            }
        Regex("^Dữ liệu JSON: (.+)$").matchEntire(source) != null ->
            Regex("^Dữ liệu JSON: (.+)$").matchEntire(source)!!.let { "JSON data: ${it.groupValues[1]}" }
        source.startsWith("Tải tệp hiện tạm ngừng trên gói miễn phí.") ->
            "File downloads are currently paused on the free plan."
        source.startsWith("Mở lần cuối: ") -> "Last opened: " + source.removePrefix("Mở lần cuối: ")
        source.startsWith("MỨC ") -> "LEVEL " + source.removePrefix("MỨC ")
        source.startsWith("Mở lần cuối: ") -> "Last opened: " + source.removePrefix("Mở lần cuối: ")
        source.startsWith("Đăng nhập ") -> "Signed in " + source.removePrefix("Đăng nhập ")
        Regex("^Cấp (\\d+) • Tấn công nhanh$").matchEntire(source) != null ->
            Regex("^Cấp (\\d+) • Tấn công nhanh$").matchEntire(source)!!.let { "Level ${it.groupValues[1]} • GEAR-UP" }
        Regex("^CẤP (\\d+) • TẤN CÔNG NHANH$").matchEntire(source) != null ->
            Regex("^CẤP (\\d+) • TẤN CÔNG NHANH$").matchEntire(source)!!.let { "LEVEL ${it.groupValues[1]} • GEAR-UP" }
        Regex("^Hạng (Đồng|Bạc|Vàng|Pha lê|Cao thủ|Vô địch|Titan) (I|II|III)$").matchEntire(source) != null ->
            Regex("^Hạng (Đồng|Bạc|Vàng|Pha lê|Cao thủ|Vô địch|Titan) (I|II|III)$").matchEntire(source)!!.let {
                val tier = when (it.groupValues[1]) {
                    "Đồng" -> "Bronze"
                    "Bạc" -> "Silver"
                    "Vàng" -> "Gold"
                    "Pha lê" -> "Crystal"
                    "Cao thủ" -> "Master"
                    "Vô địch" -> "Champion"
                    else -> "Titan"
                }
                "$tier League ${it.groupValues[2]}"
            }
        source.startsWith("CẤP ") -> "LEVEL " + source.removePrefix("CẤP ")
        source.startsWith("Bảng xếp hạng Nhà chính ") -> "Town Hall leaderboard " + source.removePrefix("Bảng xếp hạng Nhà chính ")
        source.startsWith("Chuỗi 3 sao Nhà chính ") -> "3-star streak by Town Hall " + source.removePrefix("Chuỗi 3 sao Nhà chính ")
        source.startsWith("Bảng xếp hạng giải ") -> "League leaderboard " + source.removePrefix("Bảng xếp hạng giải ")
        source.startsWith("Phân bố cúp giải đấu ") -> "League trophy distribution " + source.removePrefix("Phân bố cúp giải đấu ")
        source.startsWith("Hồi ") -> "Cooldown " + source.removePrefix("Hồi ")
        source.startsWith("Thông báo sẽ tự đóng sau ") -> {
            val match = Regex("^Thông báo sẽ tự đóng sau (\\d+) giây\\. Trong thời gian đó, bạn có thể nhấn MỞ TRÒ CHƠI để vào Clash of Clans\\.$").matchEntire(source)
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
        source.startsWith("Dữ liệu: ") -> "Data: " + source.removePrefix("Dữ liệu: ").replace(" • Nhận: ", " • Received: ")
        source.startsWith("Đã gỡ: ") -> "Removed: " + source.removePrefix("Đã gỡ: ")
        source.startsWith("Đã gửi: ") -> "Sent: " + source.removePrefix("Đã gửi: ").replace(" • Đã gỡ: ", " • Removed: ")
        source.startsWith("  •  Đã gỡ: ") -> "  •  Removed: " + source.removePrefix("  •  Đã gỡ: ")
        source.startsWith("Đã tải ") -> "Loaded " + source.removePrefix("Đã tải ")
        source.startsWith("Nhà chính ") -> "Town Hall " + source.removePrefix("Nhà chính ")
        source.startsWith("Nhà thợ ") -> "Builder Hall " + source.removePrefix("Nhà thợ ")
        source.startsWith("Cấp Nhà chính ") -> "Town Hall Level " + source.removePrefix("Cấp Nhà chính ")
        source.startsWith("Cấp bang hội ") -> "Clan Level " + source.removePrefix("Cấp bang hội ")
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
        source.startsWith("Đã gửi: ") -> "Sent: " + source.removePrefix("Đã gửi: ")
        source.startsWith("Chia sẻ bởi: ") -> "Shared by: " + source.removePrefix("Chia sẻ bởi: ")
        Regex("^Ngày dữ liệu: (.+) • Nhận: (.+)$").matchEntire(source) != null ->
            Regex("^Ngày dữ liệu: (.+) • Nhận: (.+)$").matchEntire(source)!!.let {
                "Data date: ${it.groupValues[1]} • Received: ${it.groupValues[2]}"
            }
        source.startsWith("Độ sáng: ") -> "Brightness: " + source.removePrefix("Độ sáng: ")
        source.startsWith("COC-LIO ") && source.endsWith(" đã có bản cập nhật mới.") -> source.removeSuffix(" đã có bản cập nhật mới.") + " has a new update."
        source.startsWith("ID: ") -> source
        else -> {
            val separator = when {
                source.contains(" • ") -> " • "
                source.contains(" · ") -> " · "
                else -> null
            }
            if (separator == null) source
            else source.split(separator).joinToString(separator) {
                translateAppText(it, AppUiLanguage.ENGLISH)
            }
        }
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
