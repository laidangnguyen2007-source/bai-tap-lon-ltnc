# Cải thiện hiển thị chi tiết sản phẩm và quản lý phiên đấu giá cho Seller

Kế hoạch này sẽ giải quyết 3 yêu cầu của bạn:
1. Hiển thị ảnh sản phẩm trong giao diện Chi Tiết Phiên Đấu Giá (Auction Detail).
2. Thêm phần "Item Specifics" (Thông số kỹ thuật/Chi tiết nổi bật) cho sản phẩm khi Seller tạo mới.
3. Cho phép Seller sửa đổi Ảnh, Mô tả, và Thông số kỹ thuật ngay cả khi phiên đấu giá đang trong trạng thái RUNNING (Đang diễn ra), đồng thời khóa các thông tin nhạy cảm (Giá, Bước giá, Thời gian).

## User Review Required

> [!WARNING]
> Việc lưu "Item Specifics" (Thông số kỹ thuật) có thể thực hiện theo 2 cách:
> **Cách 1:** Thêm một ô nhập văn bản lớn (TextArea) để Seller tự gõ theo định dạng `Thuộc tính: Giá trị` (VD: `Thương hiệu: Apple` xuống dòng `Tình trạng: Mới`). Sau đó hệ thống sẽ tự động tách ra và hiển thị đẹp mắt dạng bảng ở phía người mua.
> **Cách 2:** Thêm giao diện động cho phép thêm từng cặp Key - Value (Cần ấn nút '+' để thêm dòng).
> 
> *Trong kế hoạch này, tôi đề xuất sử dụng **Cách 1** vì nó thân thiện, dễ gõ nhanh cho Seller và không làm phức tạp hóa giao diện FXML hiện tại. Bạn có đồng ý với Cách 1 không?*

## Proposed Changes

### Database Layer
- Thêm cột `item_specifics` (kiểu TEXT) vào bảng `items` trong MySQL.
- Viết script migration (tự động chạy) trong `schema.sql` để thêm cột này mà không làm mất dữ liệu cũ.

### Backend Entities & DAO
#### [MODIFY] `Item.java`
- Thêm trường `String itemSpecifics` cùng Getters/Setters.

#### [MODIFY] `JdbcItemDao.java`
- Cập nhật các câu lệnh SQL `INSERT` và `UPDATE` để lưu trường `item_specifics`.
- Cập nhật hàm `mapRow()` để đọc `item_specifics` từ DB.

#### [MODIFY] `Auction.java`
- Thêm trường `String itemSpecifics` để truyền dữ liệu đầy đủ xuống cho Client.

#### [MODIFY] `JdbcAuctionDao.java`
- Cập nhật câu lệnh `SELECT` (có JOIN với bảng `items`) và hàm `mapRow()` để gán `item_specifics` vào object `Auction`.

#### [MODIFY] `EntityJsonMapper.java` & `AuctionHandlers.java`
- Đóng gói trường `itemSpecifics` vào JSON truyền qua Socket.
- **Sửa logic Validate:** Trong hàm `updateAuctionSeller()`, nếu `status == RUNNING`, chỉ cho phép cập nhật `itemDescription`, `imageBase64`, và `itemSpecifics`. Ném ra lỗi nếu Seller cố tình đổi `startingPrice`, `minBidStep`, `startTime`, `endTime`, `itemName`.

### Frontend Client Layer
#### [MODIFY] `seller-dashboard.fxml` & `SellerDashboardController.java`
- Thêm một `TextArea` mới trên giao diện (bên dưới Mô tả) với tên `itemSpecificsArea` kèm hướng dẫn nhập `Thuộc tính: Giá trị`.
- Cập nhật logic `handleEdit()`: Khi ấn "Sửa" một phiên đấu giá đang `RUNNING`, sẽ `setDisable(true)` các trường: Giá khởi điểm, Thời gian, Tên sản phẩm, Bước giá. Chỉ cho phép chỉnh sửa Ảnh, Mô tả, và Item Specifics.
- Sửa điều kiện kích hoạt nút "Sửa" trên bảng: Cho phép ấn "Sửa" cả khi phiên đang `OPEN` hoặc `RUNNING` (trước đó chỉ cho phép `OPEN`).

#### [MODIFY] `auction-detail.fxml` & `AuctionDetailController.java`
- Thiết kế lại layout: Thêm `ImageView` lớn ở đầu để hiển thị ảnh sản phẩm.
- Thêm một vùng hiển thị `Item Specifics` (dạng lưới hoặc danh sách) ngay bên dưới ảnh để mô phỏng giống phong cách của eBay.

## Verification Plan

### Automated Tests
- Khởi động lại Server để tự động chạy Migration thêm cột `item_specifics`.

### Manual Verification
1. Đăng nhập với tài khoản Seller, tạo một phiên đấu giá mới kèm tải ảnh lên và nhập thông số vào ô Item Specifics.
2. Kiểm tra xem phiên đấu giá có xuất hiện đúng thông tin trên Server không.
3. Dùng tài khoản Bidder mở xem Chi tiết phiên đấu giá: Đảm bảo ảnh và bảng thông số được hiển thị đẹp mắt, đầy đủ.
4. Chờ phiên đấu giá chuyển sang trạng thái RUNNING. Seller ấn nút Sửa: Kiểm tra xem các trường nhạy cảm có bị làm xám (disabled) không. Cập nhật thử ảnh và mô tả mới.
5. Bidder mở lại giao diện chi tiết: Nhận thấy thông tin đã được cập nhật ngay lập tức.
