# Cấu hình Google và chuẩn bị cấp phép COC-LIO

## Google Sign-In và cho phép mọi tài khoản

COC-LIO dùng Credential Manager để lấy Google ID token, sau đó dùng token đó đăng nhập Firebase Authentication bằng `GoogleAuthProvider`. Mã ứng dụng không giới hạn email theo danh sách cho phép; tài khoản Google khác có thể đăng nhập khi dự án OAuth được cấu hình cho người dùng bên ngoài.

1. Mở [Google Cloud Console](https://console.cloud.google.com/) và chọn đúng dự án đang chứa hai OAuth Client của COC-LIO.
2. Vào **Google Auth Platform → Audience**.
3. Chọn **External** nếu ứng dụng phục vụ người dùng ngoài tổ chức Google Workspace.
4. Để bất kỳ tài khoản Google nào cũng đăng nhập được, chuyển **Publishing status** sang **In production** bằng **Publish app**. Ở trạng thái **Testing**, quyền truy cập bị giới hạn theo chính sách người thử nghiệm của Google.
5. Hoàn thiện phần Branding và các thông tin liên hệ của ứng dụng.

Tài liệu Google: https://support.google.com/cloud/answer/15549945

## Khi chỉ tài khoản chủ sở hữu đăng nhập được

Mã Android không khóa đăng nhập theo email chủ sở hữu. Chủ sở hữu chỉ có quyền quản trị bổ sung; người dùng thường được tạo và đọc hồ sơ theo UID của chính họ.

Kiểm tra theo thứ tự sau:

1. **Google OAuth Audience:** chọn đúng dự án `coc-lio-73de9`, vào Google Auth Platform → Audience. Ứng dụng phải là **External** nếu người dùng dùng Gmail bên ngoài tổ chức và **Publishing status = In production** nếu cần cho mọi tài khoản Google đăng nhập. Khi để **Testing**, chỉ tài khoản trong danh sách Test users được phép tiếp tục.
2. **Nếu Google đã trả về ứng dụng nhưng COC-LIO báo lỗi lưu hồ sơ hoặc `PERMISSION_DENIED`:** cần deploy Firestore Rules mới nhất. GitHub Actions có workflow **Deploy COC-LIO Firestore Rules** tại [đây](https://github.com/HUY-HOA-1/COC-LIO/actions/workflows/deploy-firestore-rules.yml). Workflow chỉ deploy `firestore.rules`, không build APK; nó cần GitHub Actions Secret `FIREBASE_SERVICE_ACCOUNT` hợp lệ cho dự án `coc-lio-73de9`.
3. **Kiểm tra tài khoản bị vô hiệu hóa:** nếu tài liệu `users/{uid}` có `accountDisabled = true`, ứng dụng sẽ từ chối tài khoản đó theo chủ ý quản lý.
4. **Phân biệt lỗi:** lỗi xuất hiện trước khi chọn/hoàn tất tài khoản Google thường thuộc Google OAuth Audience hoặc OAuth Client; lỗi sau khi Google xác thực nhưng khi tạo hồ sơ thường thuộc Firestore Rules hoặc hồ sơ bị vô hiệu hóa.

Không nới lỏng Firestore Rules thành quyền ghi chung cho mọi người. Quy tắc hiện tại cho phép mỗi tài khoản tạo hồ sơ mang UID của chính mình, chỉ đọc hồ sơ của mình; chỉ chủ sở hữu mới được liệt kê hồ sơ tất cả người dùng.

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

Chủ sở hữu được nhận diện bằng Firebase UID trong Firestore Rules, Storage Rules, Cloud Functions và ứng dụng Android. UID chỉ là mã định danh; mọi quyền quản trị vẫn yêu cầu phiên đăng nhập Firebase Authentication hợp lệ.

Mô hình quyền:

- Tất cả tài khoản Google đã xác thực được phép đăng nhập.
- Tài khoản chủ sở hữu có quyền xem và quản lý người dùng, trạng thái cấp phép và dữ liệu quản trị.
- Người dùng thường chỉ được xem hồ sơ và giấy phép của chính mình; không thể tự cấp quyền admin, tự bật giấy phép hoặc chỉnh sửa hồ sơ người khác.

### Quy tắc Firestore

Tệp `firestore.rules` ở thư mục gốc đã được chuẩn bị để thực thi nguyên tắc trên ở phía máy chủ:

- `users/{uid}`: người dùng chỉ được đọc hồ sơ của chính mình; chỉ chủ sở hữu được liệt kê hoặc xóa hồ sơ của người dùng.
- `licenses/{uid}`: người dùng chỉ được đọc giấy phép của chính mình; chỉ chủ sở hữu được tạo, sửa hoặc xóa giấy phép.
- `admin/{document}`: chỉ chủ sở hữu được đọc và ghi.
- Email chủ sở hữu được đối chiếu trong Security Rules, không dựa vào cờ admin lưu trong APK.

**Trạng thái mã nguồn:** Firebase Authentication và màn hình quản lý tài khoản Google đã được tích hợp. Sau khi đăng nhập, ứng dụng tạo hoặc cập nhật `users/{uid}` trong Firestore với UID, Gmail, tên hiển thị, tên riêng, họ, ảnh đại diện, trạng thái xác minh email, nhà cung cấp đăng nhập, số lần đăng nhập, thời điểm tạo hồ sơ và lần đăng nhập gần nhất. Tài khoản chủ sở hữu mới có quyền liệt kê toàn bộ hồ sơ; tài khoản thường chỉ được đọc hồ sơ của chính UID đó. Nếu Firestore Rules chưa được Publish, đăng nhập vẫn có thể thành công nhưng dữ liệu sẽ không đồng bộ. Không được coi việc nhận diện email trong giao diện là quyền admin an toàn.

### Việc cần có để bật quản lý trực tuyến

1. Dự án Firebase `coc-lio-73de9` đã có Android app với package `com.huyhoa.coclio`; tệp `app/google-services.json` đã nằm trong repository.
2. **Authentication → Google** đã được bật. Web OAuth Client ID được truyền qua GitHub Secret `GOOGLE_WEB_CLIENT_ID`.
3. Tạo cơ sở dữ liệu **Cloud Firestore** ở chế độ Native nếu chưa có. Trong tab **Rules**, dán nội dung `firestore.rules` từ repository rồi nhấn **Publish**.
4. Chạy thử đăng nhập Google trong APK. Sau khi đăng nhập, kiểm tra Authentication → Users và Firestore → `users` để xác nhận hồ sơ được đồng bộ.
5. Đăng nhập bằng tài khoản chủ sở hữu đã cấu hình trong Rules, mở **Cài đặt → Quản lý tài khoản Google** để xem danh sách và thông tin tài khoản. Người dùng thường không thấy khu vực này.
6. Đồng bộ lịch sử JSON giữa nhiều điện thoại và hệ thống cấp phép trực tuyến vẫn là các phần riêng, chưa được bật trong APK hiện tại.

Tài liệu Firebase:
- https://firebase.google.com/docs/auth/android/google-signin
- https://firebase.google.com/docs/firestore/security/get-started

Không đưa khóa Admin SDK, service account private key hoặc bí mật quản trị vào APK hay GitHub repository.

## Các dịch vụ Firebase đã thêm vào mã nguồn

- **Authentication:** đăng nhập Google bằng Firebase Auth; đăng xuất cũng kết thúc phiên Firebase.
- **Firestore:** tạo/cập nhật hồ sơ Google tại `users/{uid}`, gồm thông tin tên/email/ảnh và metadata đăng nhập. Chỉ chủ sở hữu được liệt kê hồ sơ toàn bộ người dùng; mỗi người dùng chỉ đọc hồ sơ của chính mình. Lịch sử JSON, hồ sơ tên hiển thị cục bộ và giấy phép chưa đồng bộ lên đám mây.
- **Analytics, Crashlytics và Performance Monitoring:** không được bật trong phiên bản hiện tại; ứng dụng đặt trạng thái thu thập là tắt và không phát sinh sự kiện Analytics từ các luồng đăng nhập, đăng xuất hoặc nhập JSON.
- **Thông tin kỹ thuật:** khi đồng bộ hồ sơ, ứng dụng lưu nhóm metadata cơ bản gồm hãng/model, brand/product/device, ABI, phiên bản Android/API, phiên bản ứng dụng, ngôn ngữ, múi giờ, kích thước màn hình/mật độ điểm ảnh và trạng thái/loại kết nối mạng. Không bật heartbeat, theo dõi thao tác chi tiết, Analytics, Crashlytics hoặc Performance Monitoring.
- **JSON:** dữ liệu làng và lịch sử nhập được lưu cục bộ; không có luồng tải JSON lên Cloud Firestore trong phiên bản hiện tại.

**Chưa triển khai:** Firebase Cloud Messaging cho thông báo từ máy chủ, đồng bộ lịch sử JSON nhiều thiết bị, hệ thống cấp phép/Admin và xuất BigQuery. BigQuery cần bật liên kết/xuất dữ liệu từ Google Analytics/Firebase Console; chỉ thêm SDK vào APK không tự bật tính năng này.

Tệp `firestore.rules` không tự triển khai khi build APK. Cần tạo Firestore database và Publish Rules trong Firebase Console trước khi hồ sơ người dùng có thể được ghi lên đám mây. Không đưa khóa Admin SDK, service account private key hoặc bí mật quản trị vào APK hay GitHub repository.
