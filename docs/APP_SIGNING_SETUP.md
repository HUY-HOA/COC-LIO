# Ký APK COC-LIO bằng khóa riêng

Khóa ký là định danh lâu dài của ứng dụng Android. Giữ an toàn file keystore và các mật khẩu. Không tải keystore lên GitHub, không commit vào kho mã và không gửi khóa hoặc mật khẩu qua cuộc trò chuyện.

## 1. Tạo keystore một lần

Cần Java JDK 17 trở lên. Trên Windows mở PowerShell tại thư mục muốn lưu khóa; trên Android có thể dùng Termux và cài OpenJDK.

Chạy lệnh:

```bash
keytool -genkeypair -v -keystore coclio-release.jks -alias coclio -keyalg RSA -keysize 4096 -validity 10000
```

Đặt mật khẩu keystore và mật khẩu khóa. Có thể dùng cùng một mật khẩu cho cả hai. Hãy cất file `coclio-release.jks` và mật khẩu vào nơi an toàn, có bản sao dự phòng ngoại tuyến. Không tạo khóa mới cho mỗi phiên bản.

## 2. Lấy SHA-1 để cấu hình Google

Chạy:

```bash
keytool -list -v -keystore coclio-release.jks -alias coclio
```

Ghi lại dòng SHA1. Trong Google Cloud Console → Google Auth Platform → Clients, tạo OAuth Client loại Android với:
- Package name: `com.huyhoa.coclio`
- SHA-1 certificate fingerprint: SHA-1 vừa lấy.

Web Client ID vẫn cần được tạo riêng và giữ trong Secret `GOOGLE_WEB_CLIENT_ID`. Không đưa Google Client Secret vào APK.

## 3. Mã hóa keystore thành Base64

**Windows PowerShell:**

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes(".\coclio-release.jks")) | Set-Content -NoNewline ".\coclio-release.base64"
```

**Linux / macOS / Termux:**

```bash
base64 -w 0 coclio-release.jks > coclio-release.base64
```

Mở file `coclio-release.base64`, sao chép toàn bộ một dòng. File này vẫn là dữ liệu bí mật vì có thể khôi phục keystore từ đó.

## 4. Thêm GitHub Actions Secrets

Mở [Secrets của kho COC-LIO](https://github.com/Huy-hoa/COC-LIO/settings/secrets/actions) → **New repository secret**. Tạo đúng bốn Secret ký APK:

| Name | Giá trị |
|---|---|
| `ANDROID_KEYSTORE_BASE64` | Toàn bộ nội dung file `coclio-release.base64` |
| `ANDROID_KEYSTORE_PASSWORD` | Mật khẩu keystore |
| `ANDROID_KEY_ALIAS` | `coclio` |
| `ANDROID_KEY_PASSWORD` | Mật khẩu khóa đã nhập khi tạo keystore |

Đồng thời cần Secret OAuth:

| Name | Giá trị |
|---|---|
| `GOOGLE_WEB_CLIENT_ID` | Web Client ID kết thúc bằng `.apps.googleusercontent.com` |

Không tạo Secret tên `ANDROID_KEYSTORE_FILE`; workflow tự tạo đường dẫn file tạm khi chạy build. Không thêm file `.jks` vào repository.

## 5. Build và cài đặt

Sau khi đủ bốn Secret ký APK, workflow **Build COC-LIO APK** sẽ:
- Chạy `lintDebug`, `testDebugUnitTest` và tạo APK release.
- Ký APK release bằng keystore ổn định.
- Chỉ tải lên một Artifact chứa **Signed Release APK**; thời hạn lưu hiện được đặt ở mức tối thiểu là **1 ngày**. GitHub tự xóa Artifact khi hết hạn.

Workflow hiện tại không tải APK debug lên Artifact. Các log và lịch sử chạy workflow là dữ liệu riêng, không bị xóa ngay khi Artifact APK hết hạn.

Dùng **Signed Release APK** để cài và cập nhật về sau. Không làm mất file keystore: nếu mất khóa, bạn có thể không cập nhật đè được các bản release đã ký bằng khóa đó.

Lưu ý: bản APK debug được ký bằng khóa debug khác khóa release. Android không cho cài đè APK debug bằng APK release có chữ ký khác. Trước lần cài release đầu tiên, hãy bảo đảm đã giữ JSON gốc của từng làng ở nơi an toàn; nếu máy báo xung đột chữ ký, phải gỡ bản debug trước rồi mới cài release, và dữ liệu cục bộ trong app có thể bị xóa.

## 6. Địa chỉ chủ ứng dụng

Email chủ ứng dụng được cung cấp cho COC-LIO: `phunghuyhoahaikba@gmail.com`.

Email này sẽ được đặt làm quản trị viên trong **máy chủ cấp phép** khi backend được triển khai. Không nên đưa email này vào APK hoặc dùng một chuỗi email trong app làm cơ chế bảo mật. Hiện tại đăng nhập Google mới chỉ đổi hồ sơ cục bộ; quyền Admin cấp/thu hồi giấy phép chưa hoạt động vì backend xác minh ID token và cơ sở dữ liệu cấp phép chưa được triển khai.
