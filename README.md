# COC LIO

Ứng dụng Android tra cứu hồ sơ người chơi Clash of Clans bằng Player Tag.

## Chức năng hiện tại
- Nhập Supercell Developer API Key trong phiên sử dụng.
- Nhập Player Tag để tra cứu hồ sơ người chơi.
- Hiển thị tên, Player Tag, Town Hall, cấp kinh nghiệm, cúp, giải đấu, Clan và một số thành tích.
- Lưu tối đa 20 hồ sơ tra cứu gần nhất trong SQLite trên thiết bị.
- Không lưu Developer API Key vào lịch sử hoặc SharedPreferences.

## Điều kiện sử dụng
- Cần kết nối Internet.
- Cần Supercell Developer API Key được cấp trong Supercell Developer Portal.
- Địa chỉ IP công khai dùng để gọi API phải nằm trong danh sách IP được cho phép của API key.
- API token xác minh trong game không thay thế Developer API Key.

## Giới hạn API
Ứng dụng chỉ hiển thị những trường mà Clash of Clans API công khai trả về. API này không cung cấp trạng thái online/offline trực tiếp hoặc tiến trình và thời gian nâng cấp từng công trình.

## Build APK
GitHub Actions tự chạy workflow **Build Android APK** khi có commit lên nhánh main hoặc có thể chạy thủ công trong tab Actions. APK debug được lưu dưới dạng artifact `COC-LIO-debug`.

Bản debug không cần GitHub Secrets. Bộ Secret ký APK phát hành sẽ được cấu hình sau khi hoàn thiện ứng dụng.
