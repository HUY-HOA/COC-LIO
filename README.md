# COC LIO

Ứng dụng Android tra cứu hồ sơ người chơi Clash of Clans bằng Player Tag.

## Chức năng hiện tại
- Nhập Supercell Developer API Key trong phiên sử dụng.
- Nhập Player Tag để tra cứu hồ sơ người chơi.
- Hiển thị các trường mà Clash of Clans API trả về.
- Lưu tối đa 20 hồ sơ tra cứu gần nhất trong SQLite trên thiết bị.
- Không lưu Developer API Key vào lịch sử hoặc SharedPreferences.

## Điều kiện sử dụng
- Cần kết nối Internet.
- Cần Supercell Developer API Key được cấp trong Supercell Developer Portal.
- Địa chỉ IP công khai dùng để gọi API phải nằm trong danh sách IP được cho phép của API key.
- API token xác minh trong game không thay thế Developer API Key.

## Giới hạn API
Ứng dụng chỉ hiển thị những trường mà Clash of Clans API công khai trả về. API không cung cấp trạng thái online/offline trực tiếp, bản đồ làng, cấp từng công trình hoặc tiến trình và thời gian nâng cấp.

## Build APK bằng GitHub Actions
Workflow: **Build Android APK** trong tab Actions.

- Tự chạy khi có thay đổi trong mã nguồn app, Gradle hoặc workflow trên nhánh `main`.
- Có thể chạy thủ công bằng nút **Run workflow**.
- Dùng Java 17 và Gradle 8.9.
- Bật Gradle build cache và build song song.
- Hủy lần build cũ nếu có lần build mới trên cùng nhánh.
- Chỉ xuất APK khi build và kiểm tra file thành công.
- Artifact được lưu 30 ngày.

### Đánh số phiên bản
Mỗi lần chạy workflow:
- `versionCode = GITHUB_RUN_NUMBER`
- `versionName = 1.0.GITHUB_RUN_NUMBER`

Ví dụ lần chạy 12 tạo phiên bản `1.0.12`, versionCode `12`. Số lần chạy có thể nhảy nếu một workflow bị hủy hoặc thất bại; không tái sử dụng số cũ.

### Tải APK
Mở lần chạy thành công trong **Actions**, kéo xuống **Artifacts**, tải file `COC-LIO-vX.Y.Z-debug`, rồi giải nén để lấy `app-debug.apk` và `apk-sha256.txt`.

Bản debug có thể cài trực tiếp để kiểm tra. Bản phát hành release có chữ ký riêng sẽ được cấu hình sau; không cần GitHub Secrets cho bản debug.
