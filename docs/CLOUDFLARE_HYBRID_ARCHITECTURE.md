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

1. Repository riêng tư GitHub là nguồn dữ liệu chuẩn: remote-content/current.json và remote-content/catalog/*.json.
2. Phiên bản hiện tại (v3) đã được nhập vào Cloudflare KV theo từng khóa: source/v3/base.json và 17 shard. source/current là manifest trỏ tới bộ dữ liệu này.
3. Worker chỉ đọc KV; không cố truy cập repository private. Khi cần dựng phiên bản mới từ manifest, Worker kiểm tra cấu trúc, dataId trùng, giới hạn số mục, ghép JSON, tính SHA-256 và ghi bản bất biến content/vN.json trước khi đổi content/current.
4. Khi CLOUDFLARE_API_TOKEN được cấu hình trong GitHub Actions, workflow triển khai Worker và đẩy catalog mới từ GitHub vào KV. Workflow kiểm tra endpoint thực tế và so sánh toàn bộ JSON với nguồn GitHub. Nếu chưa có secret, workflow vẫn chạy unit test và kiểm tra dữ liệu đang phục vụ; nếu source tăng version nhưng chưa được xuất bản, kiểm tra sẽ báo lỗi thay vì âm thầm chấp nhận dữ liệu cũ.
5. Ứng dụng Android tải Cloudflare trước, xác minh SHA-256/version, lưu JSON hợp lệ trên máy và làm mới mỗi 5 phút khi ứng dụng hoạt động. ETag giảm tải khi nội dung không đổi; bản cache không bị ghi đè bởi phiên bản cũ.
6. Firestore listener được giữ làm đường dự phòng/thời gian thực và publisher riêng tiếp tục tạo bản mirror. Nếu Cloudflare không truy cập được, ứng dụng vẫn có cache/Firestore.

## Bảo mật và dữ liệu

- KV là kho công khai qua endpoint đọc. Chỉ chứa catalog, hướng dẫn, liên kết và mẫu nội dung chung.
- Tuyệt đối không đưa email, UID, Player Tag cá nhân, tin nhắn, hồ sơ, token, khóa bí mật, consent hoặc dữ liệu riêng tư vào KV/R2 public.
- Endpoint catalog chỉ hỗ trợ GET, HEAD và OPTIONS; không có API ghi công khai. Quyền ghi KV chỉ nằm ở tài khoản Cloudflare và GitHub Actions token giới hạn quyền.
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

## Trạng thái nhập dữ liệu và kiểm tra

- Phiên bản 3 đã được nạp vào KV: 134 mục, 17 shard. Endpoint thực tế trả HTTP 200; workflow xác nhận version, mọi ID, ETag và SHA-256 khớp với JSON nguồn.
- 11/11 unit test Worker đã qua, gồm bootstrap từ KV, không truy cập repository private, ETag/304, chặn phương thức ghi, dữ liệu thiếu, ID trùng, hash sai và fallback khi thiếu shard.
- Kiểm thử endpoint chạy từ GitHub Actions vì môi trường công cụ quản trị không gọi trực tiếp được workers.dev.
- Android đã có đường tải Cloudflare, cache cục bộ và Firebase fallback; cần đợi xác thực Android trên HEAD hiện tại trước khi build APK.

Không tự động build APK khi chỉ cập nhật Worker hoặc dữ liệu. Các thay đổi Kotlin trong ứng dụng cần được kiểm tra Android riêng và chỉ build bản cài đặt theo lệnh BUILD APK.
