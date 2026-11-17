# COC-LIO – nội dung cập nhật từ xa

Ứng dụng ưu tiên tải nội dung công khai từ Cloudflare Workers KV qua Worker chỉ đọc. Firestore vẫn là kênh dự phòng và đồng bộ thời gian thực trong ứng dụng. Hồ sơ, quyền tài khoản, tin nhắn cộng đồng và dữ liệu riêng tư không được đưa vào kho KV công khai.

## Thiết lập một lần

1. Mở Firebase project `coc-lio-73de9`, vào Firestore Database.
2. Tạo collection `app_remote_content`, sau đó tạo document có ID `current`.
3. Dùng `current.json` làm mẫu cấu trúc và tạo từng field trong document bằng Firestore Console. Không dán toàn bộ JSON vào một field String; giữ đúng kiểu Boolean, Number, String, Map và Array cho từng trường.
4. Deploy nội dung `firestore.rules` của repository. Người dùng đã đăng nhập chỉ được đọc tài liệu này; chỉ tài khoản chủ COC-LIO được tạo, sửa hoặc xóa.
5. Tăng trường `version` mỗi lần phát hành nội dung mới.

## Các nhóm dữ liệu

- `maintenance`: bật/tắt thông báo bảo trì và nội dung đi kèm. Đây là banner thông tin, không khóa app hay vô hiệu hóa đăng nhập.
- `announcement`: thông báo nổi bật, mức `info`, `success`, `warning`, `error`, ảnh HTTPS và nút hành động.
- `catalog`: tối đa 500 mục, khóa theo `dataId`; `catalogShards` liệt kê các tệp phân mảnh `part-NN.json` được ghép khi phát hành; có `name`, `category`, `description`, `maxLevel`, `imageUrl` và các trường tùy chọn `rarity` (`common`, `rare`, `epic`, `legendary`), `abilityType` (`active`, `passive`), `levelDetails` (bảng chỉ số theo từng cấp), `aliases`, `requirements`, `details`, `upgradeCost`, `upgradeDurationSeconds`. Thẻ quân lính hiển thị tên, cấp hiện tại/tối đa và số lượng; hồ sơ chi tiết đọc chỉ số theo đúng cấp hiện có trong JSON làng. Trang bị anh hùng có nhãn độ hiếm Thường màu xanh và Sử thi màu tím; cấp tối đa hiện theo cấp Lò rèn. Nhãn Huyền thoại màu vàng chỉ xuất hiện khi dữ liệu thực sự có độ hiếm này. Category hợp lệ: `defense`, `resources`, `traps`, `other`, `troops`, `heroes`, `spells`, `pets`, `equipment`. Ứng dụng có thể cập nhật tên, nhóm, cấp tối đa, ảnh, tên gọi khác, yêu cầu, chi phí và thời gian nâng cấp hiển thị mà không cần APK mới. Các dữ liệu được giới hạn số lượng và độ dài; parser JSON, cách tính tiến trình thực tế và logic xác nhận hoàn thành vẫn thuộc APK.
- `guides`: tối đa 20 hướng dẫn, mỗi mục có `id`, `title`, `body`, `imageUrl`, `enabled`.
- `links`: tối đa 20 liên kết, chỉ mở địa chỉ HTTPS.
- `notificationTemplates`: mẫu tiếng Việt dùng trực tiếp cho tiêu đề và nội dung nhắc nâng cấp, gồm `title`, `text`, `body` ở cùng một cấp dữ liệu. Biến được hỗ trợ: `{item}`, `{village}`, `{tag}`, `{id}`, `{level}`, `{base}`, `{section}`.

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
3. Push thay đổi `remote-content/current.json` hoặc `remote-content/catalog/`; xem kết quả tại GitHub Actions → **Publish COC-LIO Remote Content**.

Nếu thiếu Secret hoặc quyền Firestore, workflow sẽ báo lỗi và tài liệu đang chạy không bị báo là đã cập nhật. Workflow này chỉ đồng bộ nội dung, không build APK.


## Nguồn dữ liệu quân lính và trang bị

Danh mục các nhóm quân lính, Anh hùng, Thần chú, Linh thú, Máy công thành và 43 trang bị Anh hùng được lưu theo các tệp phân mảnh trong `remote-content/catalog/`. Dữ liệu chỉ số theo cấp được chuẩn hóa từ dự án dữ liệu cộng đồng [clash-of-clans-data](https://github.com/chiefpansancolt/clash-of-clans-data), dự án này ghi nguồn dữ liệu trò chơi từ Clash of Clans Wiki. Đây không phải API chính thức của Supercell; chỉ số có thể thay đổi sau bản cập nhật game, vì vậy cần đối chiếu trong game khi có thay đổi.


## Phân phối Cloudflare và dự phòng Firebase

- Cloudflare Worker: https://coc-lio-remote-content.coc-lio-data.workers.dev
- Endpoint nội dung: /v1/content
- KV namespace: COC-LIO Remote Content. Phiên bản 3 hiện tại đã nhập 134 mục từ 17 shard vào KV. Worker chỉ đọc KV và không gọi trực tiếp GitHub vì repository COC-LIO là private.
- Nội dung được phát hành theo khóa bất biến `content/vN.json`, sau đó cập nhật con trỏ `content/current`. Worker kiểm tra cấu trúc, dataId, phiên bản và SHA-256 trước khi phục vụ.
- Endpoint chỉ cho phép GET, HEAD và OPTIONS. Mỗi phản hồi có ETag, version và SHA-256 để Android kiểm tra dữ liệu.
- Android tải Cloudflare trước, giữ bản cache cục bộ, chỉ chấp nhận phiên bản mới hơn và tiếp tục dùng cache khi ngoại tuyến. Firestore listener được giữ làm dự phòng/thời gian thực.
- KV chỉ chứa catalog trò chơi, hướng dẫn, liên kết và mẫu thông báo chung. Không lưu email, UID, Player Tag cá nhân, tin nhắn, token hoặc dữ liệu riêng tư.
- Để tự động phát hành nội dung mới: tạo GitHub Actions secret `CLOUDFLARE_API_TOKEN` bằng token giới hạn quyền Account Workers Scripts: Edit và Account Workers KV Storage: Edit, đúng tài khoản COC-LIO. Không dán token vào mã nguồn hoặc chat.
- Khi thiếu secret, workflow vẫn chạy kiểm thử và so sánh dữ liệu đang phục vụ với repository. Nếu repository tăng version mà chưa phát hành Cloudflare, workflow sẽ báo lỗi rõ ràng; Firestore vẫn có bản mirror làm dự phòng.

## Kế hoạch R2 cho hình ảnh và tệp lớn

R2 phù hợp hơn KV cho hình ảnh, ảnh cộng đồng, tệp đính kèm và các object lớn vì đây là kho object storage, có lưu lượng egress Internet miễn phí. Tài khoản Cloudflare hiện chưa kích hoạt R2; API trả về yêu cầu bật R2 và hoàn thành bước đăng ký trong Dashboard. Không tự động bật vì bước này có thể liên kết thanh toán và sử dụng vượt mức miễn phí có thể phát sinh phí.

Sau khi chủ tài khoản bật R2 và chấp nhận hạn mức/chi phí, tạo bucket COC-LIO, gắn binding chỉ đọc cho tài nguyên công khai, rồi mới chuyển ảnh/tệp theo từng loại. Tệp riêng tư phải được cấp URL tạm thời qua Worker sau khi xác minh người dùng; không đặt bucket riêng tư ở chế độ public. Trong lúc chưa bật R2, ứng dụng tiếp tục dùng đường ảnh hiện tại và Firestore cho các nội dung nhỏ như trước.


## Đồng bộ Cloudflare và Firebase

- GitHub Actions `Publish COC-LIO Content to Cloudflare` kiểm tra Worker, dựng catalog từ `current.json` và các shard, xác minh ID trùng, sau đó phát hành phiên bản bất biến vào KV nếu có secret `CLOUDFLARE_API_TOKEN`.
- Sau khi phát hành, workflow gọi endpoint workers.dev thật và so sánh JSON đã phục vụ với nguồn trong repository, bao gồm version, catalog, ID, ETag và SHA-256.
- GitHub Actions `Publish COC-LIO Remote Content` tiếp tục đồng bộ mirror sang Firestore. Không xóa Firestore vì app vẫn dùng listener này làm dự phòng.
- Ứng dụng kiểm tra Cloudflare mỗi 5 phút khi đang hoạt động; Firestore listener vẫn nhận thay đổi thời gian thực. Nếu Cloudflare lỗi, bản cache và Firestore không bị mất.
- Không build APK chỉ vì nội dung JSON thay đổi; chỉ cần build lại khi sửa mã Android.
