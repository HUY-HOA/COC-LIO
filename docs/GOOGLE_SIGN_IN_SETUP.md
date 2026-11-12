# Cấu hình Google và chuẩn bị cấp phép COC-LIO

## Google Sign-In và cho phép mọi tài khoản

COC-LIO dùng Credential Manager để lấy Google ID token, sau đó dùng token đó đăng nhập Firebase Authentication bằng `GoogleAuthProvider`. Mã ứng dụng không giới hạn email theo danh sách cho phép; tài khoản Google khác có thể đăng nhập khi dự án OAuth được cấu hình cho người dùng bên ngoài.

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

### Xử lý lỗi [28444] Developer console is not set up correctly

Lỗi này được trả về từ Google Credential Manager trước khi Firebase Authentication nhận được Google ID Token. Cần kiểm tra cấu hình OAuth mà APK thực sự đang dùng, không chỉ nhìn SHA-1 đã nhập trong Firebase.

1. Cập nhật Secret `GOOGLE_WEB_CLIENT_ID` bằng đúng **Client ID của OAuth Client loại Web application** trong dự án `coc-lio-73de9`. Không dùng Client ID loại Android.
2. Mở Android OAuth Client trong cùng dự án. Package phải là `com.huyhoa.coclio`; SHA-1 phải trùng với SHA-1 của APK đang cài.
3. Sau khi build APK mới, nếu vẫn gặp [28444], hộp thoại lỗi trong ứng dụng sẽ hiển thị Client ID đang được nhúng, package và SHA-1 thực tế đọc từ chữ ký APK. Dùng các giá trị này để đối chiếu lại Google Cloud Console.
4. Nếu GitHub Actions dừng ở bước **Verify Firebase and Google OAuth configuration**, hãy đọc thông báo của bước đó: workflow kiểm tra Firebase Project ID, Project Number, package Android, định dạng Client ID, project number của Client ID và khoảng trắng thừa. Bước này không thể tự xác nhận loại OAuth Client; loại **Web application** vẫn phải được kiểm tra trong Google Cloud Console.

Lưu ý: GitHub không hiển thị lại nội dung Secret đã lưu. Nếu không chắc Secret hiện tại có đúng Client ID hay không, hãy cập nhật lại bằng cách sao chép trực tiếp Client ID từ OAuth Client loại Web application của dự án `coc-lio-73de9`. Client ID là mã định danh công khai của ứng dụng; không cung cấp Client Secret hoặc khóa riêng.

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

**Trạng thái mã nguồn:** Firebase Authentication đã được tích hợp. Sau khi đăng nhập, ứng dụng thử tạo hoặc cập nhật `users/{uid}` trong Firestore với UID, email, tên hiển thị và ảnh đại diện. Nếu Firestore chưa được tạo hoặc Security Rules chưa Publish, quá trình đăng nhập Firebase vẫn thành công nhưng thao tác ghi hồ sơ đám mây sẽ thất bại và được ghi log. Các quy tắc này hiện vẫn chỉ nằm trong repository, chưa được triển khai lên Firebase. Chưa có màn hình quản trị hoặc hệ thống cấp phép trực tuyến hoạt động. Không được coi việc nhận diện email trong giao diện là quyền admin an toàn.

### Việc cần có để bật quản lý trực tuyến

1. Dự án Firebase `coc-lio-73de9` đã có Android app với package `com.huyhoa.coclio`; tệp `app/google-services.json` đã nằm trong repository.
2. **Authentication → Google** đã được bật. Web OAuth Client ID được truyền qua GitHub Secret `GOOGLE_WEB_CLIENT_ID`.
3. Tạo cơ sở dữ liệu **Cloud Firestore** ở chế độ Native nếu chưa có. Trong tab **Rules**, dán nội dung `firestore.rules` từ repository rồi nhấn **Publish**.
4. Chạy thử đăng nhập Google trong APK. Sau khi đăng nhập, kiểm tra Authentication → Users để xác nhận tài khoản đã được tạo và Firestore → `users` để kiểm tra hồ sơ (chỉ xuất hiện khi Firestore đã tạo và Rules đã Publish).
5. Muốn đồng bộ lịch sử JSON giữa nhiều điện thoại, quản lý giấy phép hoặc có trang Admin, cần triển khai thêm cấu trúc dữ liệu, đồng bộ hai chiều và giao diện quản trị; các phần này chưa được bật trong APK hiện tại.

Tài liệu Firebase:
- https://firebase.google.com/docs/auth/android/google-signin
- https://firebase.google.com/docs/firestore/security/get-started

Không đưa khóa Admin SDK, service account private key hoặc bí mật quản trị vào APK hay GitHub repository.

## Các dịch vụ Firebase đã thêm vào mã nguồn

- **Authentication:** đăng nhập Google bằng Firebase Auth; đăng xuất cũng kết thúc phiên Firebase.
- **Firestore:** tạo/cập nhật hồ sơ cơ bản tại `users/{uid}` khi database và Rules đã sẵn sàng. Lịch sử làng, hồ sơ cục bộ và giấy phép chưa đồng bộ lên đám mây.
- **Analytics:** ghi các sự kiện `login`, `logout` và `village_json_import`; không gửi email, Player Tag hoặc nội dung JSON vào Analytics.
- **Crashlytics:** SDK và Gradle plugin đã được thêm để thu thập sự cố của bản phát hành.
- **Performance Monitoring:** SDK và Gradle plugin đã được thêm để thu thập hiệu năng tự động.

**Chưa triển khai:** Firebase Cloud Messaging cho thông báo từ máy chủ, đồng bộ lịch sử JSON nhiều thiết bị, hệ thống cấp phép/Admin và xuất BigQuery. BigQuery cần bật liên kết/xuất dữ liệu từ Google Analytics/Firebase Console; chỉ thêm SDK vào APK không tự bật tính năng này.

Tệp `firestore.rules` không tự triển khai khi build APK. Cần tạo Firestore database và Publish Rules trong Firebase Console trước khi hồ sơ người dùng có thể được ghi lên đám mây. Không đưa khóa Admin SDK, service account private key hoặc bí mật quản trị vào APK hay GitHub repository.
