# Cấu hình Google và chuẩn bị cấp phép COC-LIO

## Việc cần làm ngay: tạo Web Client ID

1. Mở [Google Cloud Console](https://console.cloud.google.com/).
2. Chọn hoặc tạo một dự án.
3. Mở Google Auth Platform, hoàn tất màn hình đồng ý và thêm email quản trị.
4. Vào Clients → Create Client → chọn **Web application**.
5. Đặt tên, tạo OAuth Client ID và sao chép giá trị có đuôi `.apps.googleusercontent.com`.

Không sao chép Client Secret vào ứng dụng Android. Ứng dụng chỉ cần Web Client ID để yêu cầu Google ID token.

Tài liệu chính thức: https://developer.android.com/identity/sign-in/credential-manager-siwg-implementation

## Thêm Secret vào GitHub

Mở repository [Huy-hoa/COC-LIO](https://github.com/Huy-hoa/COC-LIO):

1. Chọn **Settings** → **Secrets and variables** → **Actions**.
2. Nhấn **New repository secret**.
3. Điền:

| Ô | Giá trị |
|---|---|
| Name | `GOOGLE_WEB_CLIENT_ID` |
| Secret | Dán Web Client ID có đuôi `.apps.googleusercontent.com` |

4. Nhấn **Add secret**.

Sau khi lưu, báo lại rằng đã thêm Secret. Không gửi Client Secret hoặc mật khẩu Google vào cuộc trò chuyện.

## Cấp phép người dùng (chưa được bật trong bản hiện tại)

Mục tiêu của COC-LIO là đăng nhập Google để nhận diện người dùng, sau đó máy chủ kiểm tra giấy phép:
- Email quản trị của chủ ứng dụng có quyền quản lý danh sách cấp phép.
- Người dùng khác chỉ sử dụng khi được cấp quyền và giấy phép còn hiệu lực.
- Có thể cấp, thu hồi và đặt hạn sử dụng từ màn hình quản trị.
- Không dùng Google để sao lưu JSON hoặc lịch sử làng.

Đăng nhập Google trong mã hiện tại mới chỉ đổi hồ sơ lưu cục bộ trên điện thoại; nó **chưa kiểm tra giấy phép**. Cần triển khai API và cơ sở dữ liệu riêng trước khi có thể bật quyền quản trị/cấp phép. Không được nhúng khóa quản trị hoặc khóa ký token vào APK.

Sau khi bạn thêm `GOOGLE_WEB_CLIENT_ID`, cần cung cấp thêm **địa chỉ email Google muốn đặt làm chủ ứng dụng/admin**. Email này sẽ được cấu hình ở phía máy chủ, không đưa vào APK.

## Các khóa máy chủ sẽ bổ sung sau

Các giá trị này không cần tạo trong GitHub Actions ở bước hiện tại:
- `COCLIO_LICENSE_API_URL`: địa chỉ API cấp phép; sẽ cung cấp sau khi máy chủ được triển khai.
- `COCLIO_ADMIN_EMAIL`: email Google của chủ ứng dụng; lưu ở môi trường máy chủ.
- `COCLIO_LICENSE_SIGNING_SECRET`: khóa ký phiên cấp phép; tạo ngẫu nhiên ở máy chủ, không gửi trong chat và không đóng gói vào APK.

Không tự đặt giá trị giả cho ba biến trên. Cần tạo backend, rồi mới có giá trị chính xác để cấu hình.
