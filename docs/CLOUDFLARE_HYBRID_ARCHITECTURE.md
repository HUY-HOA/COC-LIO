# Kiến trúc kết hợp Firebase và Cloudflare cho COC-LIO

## Quyết định phân chia dịch vụ

| Thành phần | Nhà cung cấp | Vai trò |
| --- | --- | --- |
| Google đăng nhập và phiên tài khoản | Firebase Authentication | Giữ SDK Android, luồng đăng nhập và tài khoản hiện tại |
| Hồ sơ, quyền, cài đặt riêng tư | Cloud Firestore | Giữ dữ liệu người dùng và quy tắc bảo mật đang hoạt động |
| Cộng đồng, tin nhắn, JSON người dùng | Cloud Firestore | Giữ đồng bộ thời gian thực và phân quyền hiện tại |
| Thông báo Android | Firebase Cloud Messaging | Giữ FCM làm kênh phân phối đến thiết bị |
| Theme, điều hướng, feature flags có điều kiện | Firebase Remote Config | Giữ fetch/activate và listener hiện tại |
| Crash và hiệu năng Android | Crashlytics + Firebase Performance | Giữ SDK theo dõi lỗi trên thiết bị |
| Nội dung catalog trò chơi công khai | Cloudflare Workers + KV | Phân phối JSON phiên bản lớn, ETag và hash |
| Tệp lớn/hình ảnh | Cloudflare R2 sau khi kích hoạt | Mục tiêu tiếp theo; chưa bật trong tài khoản |
| API, xác minh và phân phối nội dung | Cloudflare Workers | Không nhận dữ liệu riêng tư qua endpoint catalog công khai |

## Đường đi của nội dung

1. GitHub là nguồn dữ liệu chuẩn trong remote-content/current.json và remote-content/catalog/*.json.
2. Workflow ghép các shard, kiểm tra dataId trùng, schema, giới hạn số mục và kích thước.
3. Worker được triển khai với binding KV. Workflow ghi khóa content/vN.json trước, sau đó mới đổi content/current.
4. Worker chỉ đọc nội dung công khai, kiểm tra SHA-256 và version trong payload, trả ETag và cache headers.
5. Ứng dụng Android tải Cloudflare trước, lưu JSON hợp lệ vào bộ nhớ ứng dụng và không ghi đè cache bằng phiên bản cũ hơn.
6. Firestore listener còn lại làm dự phòng và cập nhật thời gian thực. Nếu Cloudflare không truy cập được, ứng dụng vẫn dùng cache/Firestore.

## Bảo mật và dữ liệu

- KV là kho công khai qua endpoint đọc. Chỉ chứa catalog, hướng dẫn, liên kết và mẫu nội dung chung.
- Tuyệt đối không đưa email, UID, Player Tag cá nhân, tin nhắn, hồ sơ, token, khóa bí mật, consent hoặc dữ liệu riêng tư vào KV/R2 public.
- Endpoint catalog không hỗ trợ POST, PUT, PATCH hoặc DELETE. Chỉ GitHub Actions với Cloudflare API token có quyền giới hạn mới được phát hành dữ liệu.
- Quyền chủ ứng dụng và quản trị viên tiếp tục được kiểm tra ở Firebase/backend; không dựa vào trạng thái giao diện APK.
- Ứng dụng không tải hay thực thi mã từ xa. Chỉ parse JSON theo parser đã đóng gói trong APK.
- Nếu Firebase không hoạt động, dữ liệu Cloudflare công khai và cache vẫn có thể đọc; các chức năng tài khoản/cộng đồng vẫn cần Firebase.

## Thiết lập tự động phát hành

Tạo Cloudflare API Token giới hạn tài khoản COC-LIO với:
- Account > Workers Scripts: Edit
- Account > Workers KV Storage: Edit

Lưu token trong GitHub Repository > Settings > Secrets and variables > Actions với tên CLOUDFLARE_API_TOKEN. Không đưa token vào source code hoặc APK. Workflow đã có Account ID và KV namespace ID của tài nguyên COC-LIO.

Cloudflare Worker endpoint hiện dùng:
https://coc-lio-remote-content.coc-lio-data.workers.dev

## R2 cần thao tác thủ công

Tài khoản hiện chưa bật R2. Cloudflare yêu cầu chủ tài khoản vào Dashboard > Storage & databases > R2 > Overview và hoàn tất bước kích hoạt/đăng ký. R2 có free monthly included usage nhưng phần vượt mức sẽ tính phí. Không bật hoặc thực hiện bước có thể liên kết thanh toán tự động.

Sau khi kích hoạt, tạo bucket riêng cho tài nguyên công khai. Tệp cộng đồng hoặc dữ liệu riêng tư phải đặt trong vùng không public và chỉ cấp quyền bằng backend sau xác thực. Không trộn tệp riêng tư với bucket public.

## Kiểm tra

Unit test Worker được chạy trong workflow bằng Node.js test runner. Kiểm tra bao gồm health, nội dung hợp lệ, ETag/304, chặn phương thức ghi, thiếu dữ liệu, hash sai, key không hợp lệ và đường asset trước khi R2 được gắn.

Không tự động build APK khi chỉ cập nhật Worker hoặc dữ liệu. Các thay đổi Kotlin trong ứng dụng cần được kiểm tra Android riêng và chỉ build bản cài đặt theo lệnh BUILD APK.
