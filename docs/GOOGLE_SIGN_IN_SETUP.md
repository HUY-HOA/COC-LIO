# Cấu hình Google và chuẩn bị cấp phép COC-LIO

## Google Sign-In và cho phép mọi tài khoản

COC-LIO dùng Credential Manager để đăng nhập bằng Google ID token. Mã ứng dụng hiện không giới hạn email theo danh sách cho phép; tài khoản Google khác có thể đăng nhập khi dự án OAuth được cấu hình cho người dùng bên ngoài.

1. Mở [Google Cloud Console](https://console.cloud.google.com/) và chọn đúng dự án đang chứa hai OAuth Client của COC-LIO.
2. Vào **Google Auth Platform → Audience**.
3. Chọn **External** nếu ứng dụng phục vụ người dùng ngoài tổ chức Google Workspace.
4. Để bất kỳ tài khoản Google nào cũng đăng nhập được, chuyển **Publishing status** sang **In production** bằng **Publish app**. Ở trạng thái **Testing**, quyền truy cập bị giới hạn theo chính sách người thử nghiệm của Google.
5. Hoàn thiện phần Branding và các thông tin liên hệ của ứng dụng.

Tài liệu Google: https://support.google.com/cloud/answer/15549945

## OAuth Client và chữ ký APK

COC-LIO dùng hai loại OAuth Client trong **cùng một dự án Google Cloud**:

- **Web application client**: dùng làm `GOOGLE_WEB_CLIENT_ID` trong GitHub Actions Secret. Không dùng Android Client ID ở biến này.
- **Android client**: ràng buộc với package `com.huyhoa.coclio` và SHA-1 của chứng thư ký APK cài trên điện thoại.

### Lấy SHA-1 chính xác của APK release

Workflow đã ký APK bằng keystore ổn định lưu trong GitHub Secrets. Sau khi tự chạy workflow **Build COC-LIO APK**, mở log bước **Print release signing certificate fingerprints**. Bước này in package name cùng SHA-1 và SHA-256 công khai của chứng thư release; không in khóa riêng hay mật khẩu.

Trong Google Cloud Console → Google Auth Platform → Clients:

1. Mở đúng dự án đã tạo Web application client.
2. Tạo hoặc mở OAuth Client loại **Android**.
3. Nhập package name chính xác: `com.huyhoa.coclio`.
4. Nhập SHA-1 lấy từ log release ở trên, không lấy SHA-1 của debug APK.
5. Lưu cấu hình. Giữ Web Client ID ở Secret `GOOGLE_WEB_CLIENT_ID` như phần dưới.

Nếu sau này phân phối qua Google Play với Play App Signing, cần thêm cả SHA-1 của **App signing key certificate** trong Play Console → App integrity; chứng thư này có thể khác upload key và khác khóa release ký trực tiếp trong GitHub Actions.

Mã lỗi xác thực 16 không đủ để kết luận Dịch vụ Google Play đã cũ. Hãy kiểm tra Web Client ID, dự án OAuth, package và SHA-1 của đúng chứng thư trước. Nếu cấu hình ngoài ứng dụng chưa khớp, chỉ sửa mã APK sẽ không giải quyết được lỗi.

## Thêm Secret vào GitHub

Mở repository [HUY-HOA-1/COC-LIO](https://github.com/HUY-HOA-1/COC-LIO):

1. Chọn **Settings** → **Secrets and variables** → **Actions**.
2. Nhấn **New repository secret**.
3. Điền:

| Ô | Giá trị |
|---|---|
| Name | `GOOGLE_WEB_CLIENT_ID` |
| Secret | Dán Web Client ID có đuôi `.apps.googleusercontent.com` |

4. Nhấn **Add secret**.

Không gửi Client Secret hoặc mật khẩu Google vào cuộc trò chuyện.

## Tài khoản chủ sở hữu và quyền quản trị

Email chủ sở hữu COC-LIO đã được xác định là:

`phunghuyhoahaikba@gmail.com`

Mô hình quyền dự kiến:

- Tất cả tài khoản Google đã xác thực được phép đăng nhập.
- Tài khoản chủ sở hữu có quyền xem và quản lý người dùng, trạng thái cấp phép và dữ liệu quản trị.
- Người dùng thường chỉ được xem hồ sơ và giấy phép của chính mình; không thể tự cấp quyền admin, tự bật giấy phép hoặc chỉnh sửa hồ sơ người khác.

### Quy tắc Firestore

Tệp `firestore.rules` ở thư mục gốc đã được chuẩn bị để thực thi nguyên tắc trên ở phía máy chủ:

- `users/{uid}`: người dùng chỉ được đọc hồ sơ của chính mình; chỉ chủ sở hữu được liệt kê hoặc xóa hồ sơ của người dùng.
- `licenses/{uid}`: người dùng chỉ được đọc giấy phép của chính mình; chỉ chủ sở hữu được tạo, sửa hoặc xóa giấy phép.
- `admin/{document}`: chỉ chủ sở hữu được đọc và ghi.
- Email chủ sở hữu được đối chiếu trong Security Rules, không dựa vào cờ admin lưu trong APK.

**Quan trọng:** các quy tắc này mới nằm trong repository, chưa được triển khai lên Firebase. Bản APK hiện tại cũng chưa tích hợp Firebase Authentication/Cloud Firestore; phần Google Sign-In vẫn chỉ lưu hồ sơ cục bộ trên điện thoại. Vì vậy, chưa có màn hình quản trị hoặc hệ thống cấp phép trực tuyến hoạt động. Không được coi việc nhận diện email trong giao diện là quyền admin an toàn.

### Việc cần có để bật quản lý trực tuyến

1. Tạo/chọn dự án Firebase và đăng ký Android app với package `com.huyhoa.coclio`.
2. Bật **Authentication → Google** và liên kết đúng dự án Google Cloud/OAuth đang dùng.
3. Tạo Cloud Firestore, triển khai nội dung tệp `firestore.rules` trong tab **Rules**, sau đó Publish.
4. Cung cấp tệp cấu hình Firebase Android `google-services.json` để tích hợp Firebase Authentication, Firestore và màn hình quản trị vào APK.

Tài liệu Firebase:
- https://firebase.google.com/docs/auth/android/google-signin
- https://firebase.google.com/docs/firestore/security/get-started

Không đưa khóa Admin SDK, service account private key hoặc bí mật quản trị vào APK hay GitHub repository.

## Giới hạn hiện tại

Đăng nhập Google trong mã hiện tại mới chỉ đổi hồ sơ lưu cục bộ trên điện thoại; nó chưa kiểm tra giấy phép. Tệp Firestore Rules là bước chuẩn bị bảo mật, không tự tạo máy chủ hoặc tự bật chức năng quản trị. Cần hoàn tất cấu hình Firebase và tích hợp Firebase vào ứng dụng trước khi quyền chủ sở hữu và quản lý giấy phép hoạt động thực tế.
