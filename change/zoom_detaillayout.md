# Danh Sách Các Thay Đổi Trong Phiên Làm Việc

Dưới đây là tổng hợp tất cả các thay đổi mà chúng ta đã thực hiện đối với source code trong phiên làm việc này, được nhóm theo từng tính năng.

---

## 1. Sửa lỗi Giao diện Seller Dashboard (Tạo phiên đấu giá)

**Mục tiêu:** 
- Ngăn việc form tạo phiên đấu giá quá dài đẩy cửa sổ ứng dụng xuống dưới thanh Taskbar của Windows.
- Ngăn thanh cuộn dọc (scrollbar) đè lên các nút bấm chọn giờ/ngày và ô nhập chữ.
- Sửa lỗi chữ hướng dẫn (`promptText`) trong ô nhập Thông số kỹ thuật không chịu xuống dòng.

**Các file đã sửa:**
- **`auction-client/src/main/resources/com/auction/client/fxml/seller-dashboard.fxml`**
  - Đã bọc toàn bộ form nhập liệu (khối `GridPane`) vào trong một thẻ `<ScrollPane>`.
  - Thiết lập `fitToWidth="true"` và `hbarPolicy="NEVER"` cho `ScrollPane` để không bao giờ hiện thanh cuộn ngang làm vỡ form.
  - Tăng nhẹ chiều rộng của khối cha `VBox` (`prefWidth` từ 460 lên 500, `minWidth` từ 420 lên 480).
  - Tăng khoảng đệm bên phải (right-padding) của `GridPane` từ 10 lên 15 pixel để dành chỗ cho thanh cuộn dọc xuất hiện mà không đè vào ComboBox/DatePicker.

- **`auction-client/src/main/java/com/auction/client/controller/SellerDashboardController.java`**
  - Tại phương thức `initialize()`, thay vì dùng FXML, tôi đã dùng code để thiết lập `promptText` cho biến `itemSpecificsArea`.
  - Sử dụng ký tự Carriage Return (`\r`) thay vì Newline (`\n`) để lách lỗi (bug) bộ lọc chuỗi tự động xóa dấu xuống dòng của JavaFX. 

---

## 2. Nâng cấp Giao diện Chi Tiết Sản Phẩm & Tính năng Xem Ảnh (Zoom)

**Mục tiêu:**
- Đưa hình ảnh sản phẩm và thông tin sản phẩm dàn sang 2 cột (trái - phải) để tối ưu không gian rộng của màn hình.
- Cho phép người dùng click vào ảnh để mở popup xem ảnh to với kích thước thật.
- Cho phép giữ Ctrl + Lăn chuột để phóng to/thu nhỏ ảnh, kéo thả để di chuyển.

**Các file đã sửa:**
- **`auction-client/src/main/resources/com/auction/client/fxml/auction-detail.fxml`**
  - Viết lại toàn bộ cấu trúc của cột `Panel trái: Thông tin sản phẩm`.
  - Thay thế cách xếp dọc (VBox) bằng kiểu xếp ngang (`HBox`).
  - **Cột con bên trái**: Chứa `ImageView` và một thẻ `Label` nhỏ hướng dẫn "🔍 Nhấn vào ảnh để phóng to". Gắn sự kiện `onMouseClicked="#handleImageClick"`.
  - **Cột con bên phải**: Chứa bảng `GridPane` hiển thị Tên, Danh mục, Mô tả và `VBox` hiển thị Thông số kỹ thuật. Thêm các `ColumnConstraints` để chữ tự động xuống dòng và lấp đầy không gian.

- **`auction-client/src/main/java/com/auction/client/controller/AuctionDetailController.java`**
  - Bổ sung dòng `itemImageView.setCursor(javafx.scene.Cursor.HAND);` vào đầu hàm `initialize()` để hiện con trỏ chuột bàn tay khi trỏ vào ảnh (lách lỗi biên dịch XML của JavaFX).
  - Thêm phương thức mới `@FXML private void handleImageClick(javafx.scene.input.MouseEvent event)`.
  - Bên trong `handleImageClick`:
    - Tạo một cửa sổ `Stage` mới (dạng Modal chặn cửa sổ dưới).
    - Tạo một `ImageView` thứ hai chứa ảnh gốc, đặt vào bên trong một đối tượng `Group` và sau đó là `StackPane`.
    - Bọc toàn bộ vào một `ScrollPane` có chức năng `setPannable(true)` để cho phép kéo thả chuột di chuyển góc nhìn.
    - Cài đặt bộ lắng nghe sự kiện `ScrollEvent` để thay đổi tỷ lệ ScaleX/ScaleY khi người dùng giữ phím Ctrl và lăn chuột (chức năng Zoom).

---

> [!TIP]
> **Lưu ý dành cho lập trình viên:** 
> - Lỗi không thể truy cập trang chi tiết sản phẩm ở lúc cuối xảy ra do thuộc tính `cursor="HAND"` viết trực tiếp trong file `.fxml` không được FXML Loader của JavaFX biên dịch được (vì nó không phải Enum). Khi viết app bằng JavaFX, bạn nên cấu hình `Cursor` trực tiếp trong code Java để an toàn nhất.
> - Bạn có thể xem file TestLoad.java vừa được tạo tạm trong mã nguồn; file này có thể xóa bỏ an toàn vì chỉ dùng để debug lỗi.
