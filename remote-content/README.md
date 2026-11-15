# COC-LIO – nội dung cập nhật từ xa

Ứng dụng đọc tài liệu Firestore sau khi người dùng đăng nhập Google. Tài liệu không chứa mã thực thi; ứng dụng chỉ diễn giải các trường dữ liệu đã được giới hạn.

## Thiết lập một lần

1. Mở Firebase project `coc-lio-73de9`, vào Firestore Database.
2. Tạo collection `app_remote_content`, sau đó tạo document có ID `current`.
3. Dùng `current.json` làm mẫu cấu trúc và tạo từng field trong document bằng Firestore Console. Không dán toàn bộ JSON vào một field String; giữ đúng kiểu Boolean, Number, String, Map và Array cho từng trường.
4. Deploy nội dung `firestore.rules` của repository. Người dùng đã đăng nhập chỉ được đọc tài liệu này; chỉ tài khoản chủ COC-LIO được tạo, sửa hoặc xóa.
5. Tăng trường `version` mỗi lần phát hành nội dung mới.

## Các nhóm dữ liệu

- `maintenance`: bật/tắt thông báo bảo trì và nội dung đi kèm. Đây là banner thông tin, không khóa app hay vô hiệu hóa đăng nhập.
- `announcement`: thông báo nổi bật, mức `info`, `success`, `warning`, `error`, ảnh HTTPS và nút hành động.
- `catalog`: tối đa 500 mục, khóa theo `dataId`; có `name`, `category`, `description`, `maxLevel`, `imageUrl` và các trường tùy chọn `rarity` (`common`, `rare`, `epic`, `legendary`), `aliases`, `requirements`, `details`, `upgradeCost`, `upgradeDurationSeconds`. Màn danh mục hiển thị nhãn độ hiếm theo màu và cho phép mở hồ sơ chi tiết từng mục. Với trang bị anh hùng, ứng dụng có sẵn phân loại Thường/Sử thi; dữ liệu catalog từ xa có thể bổ sung hoặc ghi đè độ hiếm. Category hợp lệ: `defense`, `resources`, `traps`, `other`, `troops`, `heroes`, `spells`, `pets`, `equipment`. Ứng dụng có thể cập nhật tên, nhóm, cấp tối đa, ảnh, tên gọi khác, yêu cầu, chi phí và thời gian nâng cấp hiển thị mà không cần APK mới. Các dữ liệu được giới hạn số lượng và độ dài; parser JSON, cách tính tiến trình thực tế và logic xác nhận hoàn thành vẫn thuộc APK.
- `guides`: tối đa 20 hướng dẫn, mỗi mục có `id`, `title`, `body`, `imageUrl`, `enabled`.
- `links`: tối đa 20 liên kết, chỉ mở địa chỉ HTTPS.
- `notificationTemplates`: mẫu tiêu đề/nội dung thông báo nhắc nâng cấp, tách riêng hai nhánh `vi` và `en` để giữ đúng ngôn ngữ ứng dụng. Mỗi nhánh có `title`, `text`, `body`. Biến được hỗ trợ: `{item}`, `{village}`, `{tag}`, `{id}`, `{level}`, `{base}`, `{section}`. Khi người dùng chọn ngôn ngữ hệ thống khác tiếng Việt/tiếng Anh, ứng dụng dùng tiếng Anh.

## Hình ảnh và quyền truy cập

Ứng dụng chỉ nhận URL bắt đầu bằng `https://`. Máy chủ ảnh phải cho phép thiết bị người dùng tải ảnh công khai qua HTTPS. Không lưu dữ liệu cá nhân, thông tin đăng nhập, token, khóa API hoặc nội dung riêng tư vào tài liệu này. Trên Firebase Spark, dùng URL ảnh HTTPS từ nguồn công khai hiện có; không yêu cầu bật Firebase Storage/Blaze.

## Remote Config và dữ liệu Firestore

Các màu giao diện, LED, tên ứng dụng, điều hướng, bật/tắt trang, bản dịch và khối bố cục vẫn được quản lý qua Remote Config, tham số String `coclio_app_config_json` (xem `remote-config/app.json` và `remote-config/README.md`). Hướng dẫn Firestore trong file này là kênh riêng cho nội dung dài và danh mục.

## Giới hạn cần build APK

Đổi dữ liệu, thông báo, hướng dẫn, URL ảnh, tên/nhóm/cấp tối đa, mẫu nhắc và bố cục/điều hướng đã hỗ trợ có thể cập nhật mà không build lại. Thay đổi parser JSON, cách tính thời gian, thuật toán phân loại mới, quyền Android, dịch vụ nền, SDK, hành vi native hoặc loại thành phần UI mới vẫn cần APK mới. Không tải hay chạy Kotlin/Java, JavaScript, DEX hoặc bất kỳ mã nào từ Firestore/Remote Config.


## Tự động đồng bộ từ GitHub

Khi commit và push `remote-content/current.json` lên nhánh `main`, GitHub Actions chạy workflow **Publish COC-LIO Remote Content** để ghi toàn bộ nội dung vào `app_remote_content/current`.

Quy trình kiểm tra JSON, giới hạn số mục, ghi tài liệu bằng Firebase Admin SDK, đọc lại tài liệu và so sánh với file nguồn. File trong repository là nguồn dữ liệu chính thức; nội dung được sửa trực tiếp trong Firestore có thể bị thay thế ở lần đồng bộ kế tiếp.

Thiết lập một lần:

1. GitHub Actions cần Secret `FIREBASE_SERVICE_ACCOUNT` đang được dùng bởi các quy trình Firebase khác.
2. Tài khoản dịch vụ phải có quyền đọc và ghi tài liệu Firestore trong dự án `coc-lio-73de9`.
3. Push thay đổi `remote-content/current.json`; xem kết quả tại GitHub Actions → **Publish COC-LIO Remote Content**.

Nếu thiếu Secret hoặc quyền Firestore, workflow sẽ báo lỗi và tài liệu đang chạy không bị báo là đã cập nhật. Workflow này chỉ đồng bộ nội dung, không build APK.
