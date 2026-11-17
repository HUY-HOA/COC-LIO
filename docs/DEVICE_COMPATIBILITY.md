# Khả năng tương thích thiết bị Android

## Phạm vi nền tảng
- Android 7.0 (API 24) trở lên.
- Ứng dụng dùng Java 17; Core Library Desugaring được bật để hỗ trợ các API thư viện Java như `java.time` trên Android cũ.
- Mục tiêu SDK hiện giữ ở API 35; thay đổi target SDK cần được rà soát riêng vì có thể làm thay đổi hành vi hệ thống.

## Kích thước và tư thế màn hình
- Giao diện nội dung được giới hạn chiều rộng tối đa trên màn hình lớn và vẫn lấp đầy chiều rộng trên điện thoại.
- Hộp nhập JSON có thể cuộn theo chiều dọc để tránh bị cắt trên màn hình thấp hoặc chế độ ngang.
- Activity cho phép thay đổi kích thước và nhận các thay đổi cấu hình về hướng, kích thước và bố cục màn hình.

## Thông báo
- Android 8.0 trở lên dùng Notification Channels; người dùng có thể chỉnh âm thanh, rung và mức ưu tiên trong cài đặt hệ thống.
- Android 7.0–7.1 dùng cấu hình âm thanh và rung trực tiếp trên thông báo.
- Android 13 trở lên cần quyền thông báo runtime.
- Bong bóng nổi nhắc nâng cấp có công tắc riêng trong Cài đặt; khi bật cần quyền Hiển thị trên ứng dụng khác và quyền thông báo trên Android 13 trở lên. Bong bóng có thể kéo, chạm để xem nội dung nhắc và tự thu gọn sau 25 giây; dịch vụ nền sẽ duy trì đến khi tắt công tắc.
- Nhà sản xuất hoặc chế độ tiết kiệm pin có thể trì hoãn tác vụ nền; không thể bảo đảm thời điểm thông báo tuyệt đối trên mọi thiết bị.

## Thành phần phụ thuộc
- Đăng nhập Google cần dịch vụ Google Play tương thích và cấu hình OAuth hợp lệ. Thiết bị không có Google Play Services có thể không dùng được chức năng đăng nhập Google; các chức năng lưu dữ liệu cục bộ vẫn hoạt động.
- Widget cần launcher Android hỗ trợ App Widgets. Một số launcher của nhà sản xuất có thể hiển thị widget khác nhau.
- Mở Clash of Clans yêu cầu game đã được cài đặt; nếu chưa có, ứng dụng mở trang game.

## Phạm vi kiểm chứng
Cấu hình nền tảng và giao diện được chuẩn bị cho dải thiết bị rộng. Chưa thể tuyên bố tương thích tuyệt đối với mọi mẫu máy khi chưa chạy kiểm thử trên thiết bị/emulator đại diện cho từng phiên bản Android, kích thước màn hình và chính sách nền của nhà sản xuất.
