# Cấu hình đăng nhập Google cho COC-LIO

COC-LIO dùng Android Credential Manager để hiển thị bộ chọn tài khoản Google. Ứng dụng dùng email Google làm khóa hồ sơ riêng trên điện thoại.

## Tạo Web Client ID

1. Mở Google Cloud Console và chọn hoặc tạo một dự án.
2. Mở Google Auth Platform, hoàn tất thông tin ứng dụng và màn hình đồng ý.
3. Tạo OAuth Client ID loại Web application.
4. Sao chép Client ID có đuôi `.apps.googleusercontent.com`.

Tài liệu chính thức: https://developer.android.com/identity/sign-in/credential-manager-siwg-implementation

## Thêm vào GitHub Actions

Trong repository `Huy-hoa/COC-LIO`:

1. Vào Settings → Secrets and variables → Actions.
2. Chọn New repository secret.
3. Tên secret: `GOOGLE_WEB_CLIENT_ID`.
4. Giá trị: Web Client ID vừa tạo.
5. Lưu secret và chạy lại workflow Build COC-LIO APK.

Không đưa Client ID vào mã nguồn hoặc gửi lên chat công khai.

## Phạm vi hiện tại

- Google được dùng để chọn hồ sơ dữ liệu cục bộ, tách lịch sử theo email.
- JSON và lịch sử vẫn lưu trên điện thoại.
- Đăng nhập Google hiện không đồng bộ dữ liệu sang Drive hoặc máy chủ.
- Ứng dụng chưa dùng Google ID token để cấp quyền cho API hoặc dữ liệu trên máy chủ. Không xem đăng nhập cục bộ này là cơ chế xác thực tài khoản cho hệ thống từ xa.
