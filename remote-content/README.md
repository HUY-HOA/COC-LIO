# COC-LIO – nội dung cập nhật từ xa

Ứng dụng đọc tài liệu Firestore sau khi người dùng đăng nhập Google. Tài liệu không chứa mã thực thi; ứng dụng chỉ diễn giải các trường dữ liệu đã được giới hạn.

## Thiết lập một lần

1. Mở Firebase project `coc-lio-73de9`, vào Firestore Database.
2. Trong collection `app_remote_content`, tạo document có ID `current`.
3. Dán nội dung mẫu trong `current.json` vào document. Dùng kiểu dữ liệu đúng như mẫu: Boolean, Number, String và Array/Map.
4. Deploy nội dung `firestore.rules` của repository. Người dùng đã đăng nhập chỉ được đọc tài liệu này; chỉ tài khoản chủ COC-LIO được tạo, sửa hoặc xóa.
5. Tăng trường `version` mỗi lần phát hành nội dung mới.

## Các nhóm dữ liệu

- `maintenance`: bật/tắt thông báo bảo trì và nội dung đi kèm. Đây là banner thông tin, không khóa app hay vô hiệu hóa đăng nhập.
- `announcement`: thông báo nổi bật, mức `info`, `success`, `warning`, `error`, ảnh HTTPS và nút hành động.
- `catalog`: tối đa 500 mục, khóa theo `dataId`; có `name`, `category`, `description`, `maxLevel`, `imageUrl`. Category hợp lệ: `defense`, `resources`, `traps`, `other`, `troops`, `heroes`, `spells`, `pets`, `equipment`. Chỉ tên/nhóm/cấp tối đa/ảnh thẻ được ghi đè; parser JSON và logic tiến trình nâng cấp vẫn là mã cài trong APK.
- `guides`: tối đa 20 hướng dẫn, mỗi mục có `id`, `title`, `body`, `imageUrl`, `enabled`.
- `links`: tối đa 20 liên kết, chỉ mở địa chỉ HTTPS.
- `notificationTemplates`: mẫu tiêu đề/nội dung thông báo nhắc nâng cấp, tách riêng hai nhánh `vi` và `en` để giữ đúng ngôn ngữ ứng dụng. Mỗi nhánh có `title`, `text`, `body`. Biến được hỗ trợ: `{item}`, `{village}`, `{tag}`, `{id}`, `{level}`, `{base}`, `{section}`. Khi người dùng chọn ngôn ngữ hệ thống khác tiếng Việt/tiếng Anh, ứng dụng dùng tiếng Anh.

## Hình ảnh và quyền truy cập

Ứng dụng chỉ nhận URL bắt đầu bằng `https://`. Máy chủ ảnh phải cho phép thiết bị người dùng tải ảnh công khai qua HTTPS. Không lưu dữ liệu cá nhân, thông tin đăng nhập, token, khóa API hoặc nội dung riêng tư vào tài liệu này. Trên Firebase Spark, dùng URL ảnh HTTPS từ nguồn công khai hiện có; không yêu cầu bật Firebase Storage/Blaze.

## Remote Config và dữ liệu Firestore

Các màu giao diện, LED, tên ứng dụng, điều hướng, bật/tắt trang, bản dịch và khối bố cục vẫn được quản lý qua Remote Config, tham số String `coclio_app_config_json` (xem `remote-config/app.json` và `remote-config/README.md`). Hướng dẫn Firestore trong file này là kênh riêng cho nội dung dài và danh mục.

## Giới hạn cần build APK

Đổi dữ liệu, thông báo, hướng dẫn, URL ảnh, tên/nhóm/cấp tối đa, mẫu nhắc và bố cục/điều hướng đã hỗ trợ có thể cập nhật mà không build lại. Thay đổi parser JSON, cách tính thời gian, thuật toán phân loại mới, quyền Android, dịch vụ nền, SDK, hành vi native hoặc loại thành phần UI mới vẫn cần APK mới. Không tải hay chạy Kotlin/Java, JavaScript, DEX hoặc bất kỳ mã nào từ Firestore/Remote Config.
