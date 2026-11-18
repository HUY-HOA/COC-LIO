# AILIO · COC-LIO

AILIO là thương hiệu phát triển dự án Android COC-LIO, ứng dụng theo dõi tiến trình làng Clash of Clans từ JSON do chính game xuất ra.

## Nguyên tắc hoạt động

- Không đăng nhập Supercell ID, không yêu cầu API key.
- Phân tích JSON, lưu lịch sử nhập và theo dõi tiến trình cơ bản được thực hiện cục bộ trên điện thoại.
- Đăng nhập Google và hồ sơ tài khoản cần Internet; JSON làng và lịch sử nhập được xử lý, lưu cục bộ trên điện thoại. Phiên bản hiện tại không đồng bộ JSON lên đám mây.
- Không cần kết nối mạng để phân tích JSON đã có và xem dữ liệu đã lưu cục bộ; những nội dung cần tải từ Internet có thể chưa hiển thị khi ngoại tuyến.
- Mỗi lần nhập là một ảnh chụp dữ liệu tại thời điểm game xuất JSON.
- Lần nhập đầu tạo mốc ban đầu; lần nhập kế tiếp cùng Player Tag được so sánh để tìm thay đổi.
- Mỗi tài khoản COC-LIO lưu tối đa 50 bản JSON trên thiết bị. Khi lưu vượt giới hạn, bản JSON cũ nhất tự động bị xóa; các bản trong mục Đã xóa vẫn được tính vào giới hạn.
- Nút **Mở game** mở Clash of Clans đồng thời bật cửa sổ nhập JSON nổi. Có thể kéo cửa sổ, thu nhỏ thành nút nổi hoặc kéo góc để thay đổi kích thước; Android cần cấp quyền Hiển thị trên ứng dụng khác.

## Cách xuất JSON trong game

1. Mở Clash of Clans.
2. Mở Cài đặt.
3. Chọn Cài đặt khác.
4. Kéo xuống mục xuất dữ liệu làng (JSON).
5. Chọn Sao chép.
6. Trong COC-LIO, nhấn **Mở game** để mở Clash of Clans cùng cửa sổ nổi.
7. Dán JSON vào cửa sổ nổi, nhập tên tài khoản nếu muốn rồi nhấn **Phân tích và lưu JSON**. Bạn cũng có thể thu nhỏ cửa sổ thành nút nổi để không che game.

## Dữ liệu đang được đọc

Nhà chính: công trình, bẫy, quân đội, máy công thành, anh hùng, thần chú, thú cưng, trang bị và trợ thủ.

Builder Base: công trình, bẫy, quân đội và anh hùng.

Dữ liệu phụ: trang trí, chướng ngại, bộ phận nhà, trang phục, cảnh quan và thông tin tăng tốc được đếm riêng.

Trường timer được tính theo timestamp của bản xuất cộng số giây còn lại. Khi timer về 0, app chỉ báo hết giờ; cần nhập JSON mới để xác nhận nâng cấp đã hoàn thành.

## Build APK

GitHub Actions tạo APK Release có chữ ký khi quy trình build được kích hoạt theo yêu cầu. APK được lưu dưới dạng artifact của lần chạy trong 1 ngày. Dùng cùng khóa ký Release cho các bản cập nhật để có thể cài đè phiên bản cũ.

## Nguồn dữ liệu và giới hạn

- Quy tắc đọc JSON tham khảo hướng dẫn nhập/xuất làng của ClashClock và tài liệu theo dõi nâng cấp cộng đồng.
- Tên theo ID hiện dùng bảng đối chiếu cộng đồng cho JSON export. ID chưa biết vẫn được giữ lại và hiển thị dưới dạng mã số.
- Cấp độ, tên và dữ liệu có thể thay đổi khi Supercell cập nhật game; bảng đối chiếu cần được rà soát sau các bản cập nhật.
- Ảnh công trình theo từng cấp, bẫy, quân đội, anh hùng, thần chú, thú cưng, máy công thành và trang bị được tải từ kho dữ liệu công khai `clash-of-clans-data`. Ứng dụng kiểm tra URL ảnh và dùng bộ nhớ đệm Coil để tải mượt hơn; lần đầu cần Internet. Dữ liệu JSON và lịch sử vẫn được lưu trên điện thoại.
- Viền neon, ánh sáng nền và hiệu ứng chuyển ảnh chỉ áp dụng lên giao diện xung quanh; ảnh game gốc không bị chỉnh màu hay biến đổi.
- Việc sử dụng hình ảnh Supercell phải tuân thủ Fan Content Policy, bao gồm giới hạn phi thương mại và thông báo rõ đây là nội dung không chính thức.

COC-LIO là ứng dụng không chính thức, không liên kết hoặc được Supercell chứng thực. Clash of Clans và tài sản trong game thuộc về chủ sở hữu tương ứng.

## Cài đặt, nhắc nâng cấp và tiện ích

- Có tab **Cài đặt** để chọn hồ sơ Google, quyền thông báo và kiểu nhắc tin nhắn hoặc báo thức.
- Đăng nhập Google được xác thực bằng tài khoản Google. Hồ sơ tài khoản cần thiết (UID, email, tên/ảnh hồ sơ Google, trạng thái xác minh và metadata đăng nhập) được lưu trong Firestore để duy trì tài khoản. Người dùng cần chấp thuận thông tin dữ liệu trước khi sử dụng chức năng trực tuyến. Chỉ chủ sở hữu được liệt kê hồ sơ toàn bộ; mỗi tài khoản thường chỉ đọc hồ sơ của chính UID đó. JSON làng, Player Tag, tên làng và lịch sử nhập được lưu trên thiết bị; phiên bản hiện tại không cung cấp tùy chọn đồng bộ JSON lên máy chủ.
- Nhắc nâng cấp được lập lịch từ timer trong JSON. Android có thể trì hoãn tác vụ do chế độ tiết kiệm pin hoặc chính sách hệ thống.
- Có tiện ích màn hình chính theo từng hồ sơ và từng làng; bố cục dọc mặc định 2 × 4 ô, danh sách tất cả công trình đang nâng cấp có thể cuộn, mỗi mục có đồng hồ đếm ngược và thanh tiến trình riêng.
- Trong app, các thẻ công trình có dải LED neon chuyển động quanh viền, có tùy chọn **Ánh sáng lan tỏa**; ảnh công trình giữ nguyên, không phủ hiệu ứng lên artwork. Các thẻ đang nâng có thanh tiến trình cập nhật theo đồng hồ.
- Thông báo nâng cấp được đặt thời hạn tự đóng 25 giây; thời gian banner nổi và âm thanh thực tế vẫn do Android, kênh thông báo và thiết bị quyết định.

Để bật đăng nhập Google, xem [docs/GOOGLE_SIGN_IN_SETUP.md](docs/GOOGLE_SIGN_IN_SETUP.md) và cấu hình secret `GOOGLE_WEB_CLIENT_ID`.

## Cập nhật giao diện và nhập JSON

- Biểu tượng ứng dụng dùng ảnh minh họa do người dùng cung cấp.
- Tổng quan chia riêng Làng chính và Làng đêm (Căn cứ thợ xây).
- Khi nhấn Mở game, COC-LIO khởi chạy Clash of Clans và cửa sổ nhập JSON nổi. Người chơi có thể dán JSON ngay trên game, đặt tên riêng cho từng tài khoản game, di chuyển/thu phóng/thu nhỏ cửa sổ; dữ liệu được phân biệt bằng Player Tag.
- Thời gian được chuẩn hóa về giây khi JSON dùng timestamp mili-giây hoặc timer mili-giây rõ ràng.
- Thẻ nâng cấp và tiện ích màn hình chính được thu gọn; đồng hồ trong tiện ích không có nền nhãn riêng.

## Google và giấy phép

Google Sign-In được dùng để xác thực tài khoản. Hồ sơ Google cơ bản được lưu trong Firestore để duy trì tài khoản và quản lý quyền truy cập. Analytics, Crashlytics, Performance Monitoring, ghi nhận hoạt động/thiết bị và đồng bộ JSON đám mây không được bật trong phiên bản hiện tại. JSON và lịch sử làng chỉ lưu cục bộ. Chủ tài khoản có màn hình quản lý hồ sơ; người dùng thường chỉ xem hồ sơ của chính mình. FCM gửi thông báo từ máy chủ, cấp phép trực tuyến và xuất BigQuery chưa hoàn thiện. Xem [hướng dẫn cấu hình](docs/GOOGLE_SIGN_IN_SETUP.md).

## Dữ liệu tài khoản và quyền riêng tư

- Trước khi đăng nhập, người dùng xác nhận một lần rằng đã đọc và đồng ý Điều khoản sử dụng và Chính sách quyền riêng tư.
- Hồ sơ cần thiết gồm UID, email, tên/ảnh hồ sơ Google, trạng thái xác minh email, nhà cung cấp đăng nhập và metadata tài khoản (số lần đăng nhập, thời điểm tạo/cập nhật/đăng nhập gần nhất) được lưu trong Firestore.
- JSON làng, Player Tag, tên làng và tiến trình được xử lý, lưu cục bộ; phiên bản hiện tại không tải JSON lên Cloud Firestore.
- Analytics, Crashlytics, Performance Monitoring và ghi nhận hoạt động/thiết bị không được bật. Ứng dụng không đọc hoặc lưu IP, IMEI, số sê-ri, MAC, Android ID, vị trí GPS, SMS, danh bạ hoặc lịch sử cuộc gọi trong hồ sơ tài khoản.
- Chế độ Firebase Spark không cần thẻ thanh toán, nhưng Firestore có hạn mức miễn phí. Xem [docs/FIREBASE_SPARK_MODE.md](docs/FIREBASE_SPARK_MODE.md) để biết cách thu thập, lưu trữ và giới hạn.
