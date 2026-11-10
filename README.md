# COC-LIO

Ứng dụng Android theo dõi tiến trình làng Clash of Clans từ JSON do chính game xuất ra.

## Nguyên tắc hoạt động

- Không đăng nhập Supercell ID, không yêu cầu API key.
- Không cần máy chủ, database đám mây hoặc kết nối mạng để phân tích.
- JSON được phân tích và lưu cục bộ trên điện thoại.
- Mỗi lần nhập là một ảnh chụp dữ liệu tại thời điểm game xuất JSON.
- Lần nhập đầu tạo mốc ban đầu; lần nhập kế tiếp cùng Player Tag được so sánh để tìm thay đổi.

## Cách xuất JSON trong game

1. Mở Clash of Clans.
2. Mở Cài đặt.
3. Chọn Cài đặt khác.
4. Kéo xuống mục xuất dữ liệu làng (JSON).
5. Chọn Sao chép.
6. Mở COC-LIO, chọn Nhập JSON rồi Dán clipboard hoặc chọn tệp.

## Dữ liệu đang được đọc

Nhà chính: công trình, bẫy, quân đội, máy công thành, anh hùng, phép thuật, thú cưng, trang bị và trợ thủ.

Builder Base: công trình, bẫy, quân đội và anh hùng.

Dữ liệu phụ: trang trí, chướng ngại, bộ phận nhà, trang phục, cảnh quan và thông tin tăng tốc được đếm riêng.

Trường timer được tính theo timestamp của bản xuất cộng số giây còn lại. Khi timer về 0, app chỉ báo hết giờ; cần nhập JSON mới để xác nhận nâng cấp đã hoàn thành.

## Build APK

Mỗi lần push lên nhánh main hoặc chạy workflow thủ công, GitHub Actions build APK debug. Mã phiên bản tăng theo số lần chạy workflow. APK được lưu dưới dạng artifact của lần chạy trong 14 ngày.

## Nguồn dữ liệu và giới hạn

- Quy tắc đọc JSON tham khảo hướng dẫn nhập/xuất làng của ClashClock và tài liệu theo dõi nâng cấp cộng đồng.
- Tên theo ID hiện dùng bảng đối chiếu cộng đồng cho JSON export. ID chưa biết vẫn được giữ lại và hiển thị dưới dạng mã số.
- Cấp độ, tên và dữ liệu có thể thay đổi khi Supercell cập nhật game; bảng đối chiếu cần được rà soát sau các bản cập nhật.
- Ảnh công trình theo từng cấp, bẫy, quân đội, anh hùng, phép thuật, thú cưng, máy công thành và trang bị được tải từ kho dữ liệu công khai `clash-of-clans-data`. Ứng dụng kiểm tra URL ảnh và dùng bộ nhớ đệm Coil để tải mượt hơn; lần đầu cần Internet. Dữ liệu JSON và lịch sử vẫn được lưu trên điện thoại.
- Viền neon, ánh sáng nền và hiệu ứng chuyển ảnh chỉ áp dụng lên giao diện xung quanh; ảnh game gốc không bị chỉnh màu hay biến đổi.
- Việc sử dụng hình ảnh Supercell phải tuân thủ Fan Content Policy, bao gồm giới hạn phi thương mại và thông báo rõ đây là nội dung không chính thức.

COC-LIO là dự án cộng đồng không chính thức, không liên kết hoặc được Supercell chứng thực. Clash of Clans và tài sản trong game thuộc về chủ sở hữu tương ứng.


## Cài đặt, nhắc nâng cấp và tiện ích

- Có tab **Cài đặt** thứ năm để chọn hồ sơ Google, quyền thông báo và kiểu nhắc tin nhắn hoặc báo thức.
- Hồ sơ Google tách lịch sử JSON trên cùng thiết bị; dữ liệu chưa tự đồng bộ lên đám mây.
- Nhắc nâng cấp được lập lịch từ timer trong JSON. Android có thể trì hoãn tác vụ do chế độ tiết kiệm pin hoặc chính sách hệ thống.
- Có tiện ích màn hình chính theo từng hồ sơ và từng làng; bố cục dọc mặc định 2 × 6 ô, danh sách tất cả công trình đang nâng cấp có thể cuộn, mỗi mục có đồng hồ đếm ngược và thanh tiến trình riêng.
- Trong app, các thẻ công trình có dải LED neon chuyển động quanh viền; ảnh công trình giữ nguyên, không phủ hiệu ứng lên artwork. Các thẻ đang nâng có thanh tiến trình cập nhật theo đồng hồ.

Để bật đăng nhập Google, xem [docs/GOOGLE_SIGN_IN_SETUP.md](docs/GOOGLE_SIGN_IN_SETUP.md) và cấu hình secret `GOOGLE_WEB_CLIENT_ID`.

## Cập nhật giao diện và nhập JSON

- Biểu tượng ứng dụng dùng ảnh minh họa do người dùng cung cấp.
- Tổng quan chia riêng Làng chính và Làng đêm (Căn cứ thợ xây).
- Nhập dữ liệu bằng cách sao chép JSON trong trò chơi rồi dán vào ứng dụng; không còn chọn tệp.
- Thời gian được chuẩn hóa về giây khi JSON dùng timestamp mili-giây hoặc timer mili-giây rõ ràng.
- Thẻ nâng cấp và tiện ích màn hình chính được thu gọn; đồng hồ trong tiện ích không có nền nhãn riêng.

## Google và giấy phép

Google trong bản hiện tại chỉ chọn hồ sơ cục bộ. Cấp phép sử dụng và quyền admin cần máy chủ xác minh Google ID token. Xem [hướng dẫn cấu hình](docs/GOOGLE_SIGN_IN_SETUP.md).
