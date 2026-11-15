# AILIO · COC-LIO

AILIO là thương hiệu phát triển dự án Android COC-LIO, ứng dụng theo dõi tiến trình làng Clash of Clans từ JSON do chính game xuất ra.

## Nguyên tắc hoạt động

- Không đăng nhập Supercell ID, không yêu cầu API key.
- Phân tích JSON, lưu lịch sử nhập và theo dõi tiến trình cơ bản được thực hiện cục bộ trên điện thoại.
- Các tính năng trực tuyến như đăng nhập Google, hồ sơ tài khoản, Cộng đồng, tra cứu dữ liệu, tải ảnh tài sản và đồng bộ JSON cần kết nối Internet; một số tính năng sử dụng Firebase hoặc dịch vụ dữ liệu bên ngoài.
- Không cần kết nối mạng để phân tích JSON đã có và xem dữ liệu đã lưu cục bộ; những nội dung cần tải từ Internet có thể chưa hiển thị khi ngoại tuyến.
- Mỗi lần nhập là một ảnh chụp dữ liệu tại thời điểm game xuất JSON.
- Lần nhập đầu tạo mốc ban đầu; lần nhập kế tiếp cùng Player Tag được so sánh để tìm thay đổi.

## Cách xuất JSON trong game

1. Mở Clash of Clans.
2. Mở Cài đặt.
3. Chọn Cài đặt khác.
4. Kéo xuống mục xuất dữ liệu làng (JSON).
5. Chọn Sao chép.
6. Mở COC-LIO, chọn Nhập JSON, dán JSON đã sao chép rồi nhấn Phân tích.

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

COC-LIO là dự án cộng đồng không chính thức, không liên kết hoặc được Supercell chứng thực. Clash of Clans và tài sản trong game thuộc về chủ sở hữu tương ứng.

## Cài đặt, nhắc nâng cấp và tiện ích

- Có tab **Cài đặt** để chọn hồ sơ Google, quyền thông báo và kiểu nhắc tin nhắn hoặc báo thức.
- Đăng nhập Google được xác thực bằng tài khoản Google. Hồ sơ cơ bản gồm email, tên và ảnh đại diện được đồng bộ để duy trì tài khoản; người dùng cần xác nhận dữ liệu tài khoản trước khi sử dụng tính năng trực tuyến. Chỉ tài khoản quản lý được đọc danh sách hồ sơ; mỗi tài khoản thường chỉ đọc hồ sơ của chính mình. Lịch sử nhập JSON đầy đủ vẫn lưu cục bộ; chỉ bản JSON mới nhất theo từng Player Tag được đồng bộ lên tài khoản khi người dùng bật lựa chọn riêng.
- Nhắc nâng cấp được lập lịch từ timer trong JSON. Android có thể trì hoãn tác vụ do chế độ tiết kiệm pin hoặc chính sách hệ thống.
- Có tiện ích màn hình chính theo từng hồ sơ và từng làng; bố cục dọc mặc định 2 × 4 ô, danh sách tất cả công trình đang nâng cấp có thể cuộn, mỗi mục có đồng hồ đếm ngược và thanh tiến trình riêng.
- Trong app, các thẻ công trình có dải LED neon chuyển động quanh viền, có tùy chọn **Ánh sáng lan tỏa**; ảnh công trình giữ nguyên, không phủ hiệu ứng lên artwork. Các thẻ đang nâng có thanh tiến trình cập nhật theo đồng hồ.
- Trong Cộng đồng, tài khoản quản lý hiển thị tên AILIO và avatar ADMIN; thành viên khác giữ tên và ảnh Google. Thông báo nâng cấp được đặt thời hạn tự đóng 25 giây; thời gian banner nổi và âm thanh thực tế vẫn do Android, kênh thông báo và thiết bị quyết định.
- Cộng đồng lưu tin nhắn và JSON trong Firestore. Ảnh được nén và lưu trực tiếp dưới dạng dữ liệu Firestore, tối đa 350 KiB/ảnh; thành viên đủ điều kiện có thể gửi ảnh. Tệp tài liệu và tệp APK đính kèm chỉ tài khoản quản lý được gửi, tối đa 350 KiB/tệp. Tệp lớn hơn và chức năng đăng/tải APK phát hành qua Firebase Storage hiện chưa khả dụng trên gói Spark.
- Thông báo Cộng đồng hiện chưa có máy chủ gửi FCM được triển khai trên Firebase Spark. APK có phần đăng ký chủ đề FCM nhưng đăng ký này không tự gửi thông báo. WorkManager kiểm tra dự phòng khoảng 15 phút/lần; Android có thể trì hoãn tác vụ nền.

Để bật đăng nhập Google, xem [docs/GOOGLE_SIGN_IN_SETUP.md](docs/GOOGLE_SIGN_IN_SETUP.md) và cấu hình secret `GOOGLE_WEB_CLIENT_ID`.

## Cập nhật giao diện và nhập JSON

- Biểu tượng ứng dụng dùng ảnh minh họa do người dùng cung cấp.
- Tổng quan chia riêng Làng chính và Làng đêm (Căn cứ thợ xây).
- Nhập dữ liệu bằng cách sao chép JSON trong trò chơi rồi dán vào ứng dụng; không còn chọn tệp.
- Thời gian được chuẩn hóa về giây khi JSON dùng timestamp mili-giây hoặc timer mili-giây rõ ràng.
- Thẻ nâng cấp và tiện ích màn hình chính được thu gọn; đồng hồ trong tiện ích không có nền nhãn riêng.

## Google và giấy phép

Google Sign-In được dùng để xác thực tài khoản. Analytics, Crashlytics và Performance chỉ thu thập khi người dùng bật lựa chọn tương ứng. Chủ tài khoản có màn hình quản lý hồ sơ; người dùng thường chỉ xem hồ sơ của chính mình. Đồng bộ bản JSON mới nhất theo Player Tag có thể bật riêng. FCM hiện mới có đăng ký chủ đề ở APK, chưa có máy chủ gửi tin; cấp phép trực tuyến và xuất BigQuery chưa hoàn thiện. Xem [hướng dẫn cấu hình](docs/GOOGLE_SIGN_IN_SETUP.md).

## Thu thập dữ liệu tùy chọn và chế độ miễn phí

- Hồ sơ Google cần thiết cho đăng nhập được đồng bộ vào Firestore.
- Khi người dùng đồng ý riêng, ứng dụng ghi nhận thông tin thiết bị/phiên hoạt động và bật Analytics hoặc Crashlytics/Performance theo đúng lựa chọn.
- Người dùng có thể chọn đồng bộ JSON Clash of Clans do chính họ nhập lên Firestore. Chỉ bản mới nhất theo từng Player Tag được giữ; dữ liệu không tự công khai lên cộng đồng.
- COC-LIO không đọc IMEI, số sê-ri, MAC, Android ID, vị trí chính xác, SMS, danh bạ hoặc lịch sử cuộc gọi.
- Chế độ Firebase Spark không cần thẻ thanh toán, nhưng Firestore có hạn mức miễn phí. Xem [docs/FIREBASE_SPARK_MODE.md](docs/FIREBASE_SPARK_MODE.md) để biết cách thu thập, lưu trữ và giới hạn.
