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
- Ảnh công trình, bẫy, quân đội, anh hùng, phép thuật, thú cưng, máy công thành và trang bị được tải từ kho ảnh công khai `clash-of-clans-data`. Ảnh được tải lần đầu cần kết nối Internet; dữ liệu JSON và lịch sử vẫn được lưu trên điện thoại.
- Việc sử dụng hình ảnh Supercell phải tuân thủ Fan Content Policy, bao gồm giới hạn phi thương mại và thông báo rõ đây là nội dung không chính thức.

COC-LIO là dự án cộng đồng không chính thức, không liên kết hoặc được Supercell chứng thực. Clash of Clans và tài sản trong game thuộc về chủ sở hữu tương ứng.
