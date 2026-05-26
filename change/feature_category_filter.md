# Tính năng: Lọc phiên đấu giá theo danh mục

**Ngày hoàn thành:** 26/05/2026
**Mục tiêu:** Cho phép người dùng lọc danh sách phiên đấu giá theo từng loại sản phẩm (Điện tử, Nghệ thuật, Xe cộ, Khác).

## 1. Giao diện (`auction-list.fxml`)
- Đã bổ sung một `ComboBox` với ID `categoryFilterCombo` nằm kế bên ô lọc trạng thái.
- Điều chỉnh kích thước các ô lọc cho vừa vặn với giao diện (giảm chiều dài combobox trạng thái từ 220 xuống 180, thêm combobox danh mục độ rộng 150).

## 2. Controller (`AuctionListController.java`)
- **Kết nối FXML:** Inject biến `@FXML private ComboBox<String> categoryFilterCombo;`.
- **Khởi tạo dữ liệu:** Trong hàm `initialize()`, thiết lập danh sách các tuỳ chọn cho `categoryFilterCombo` gồm `["Tất cả danh mục", "Điện tử", "Nghệ thuật", "Xe cộ", "Khác"]`. Giá trị mặc định được đặt là `"Tất cả danh mục"`.
- **Sự kiện thay đổi (Event Listener):** Gắn `setOnAction` cho cả `statusFilterCombo` và `categoryFilterCombo` để tự động gọi hàm `loadAuctions()` mỗi khi người dùng thay đổi lựa chọn (không cần bấm nút Làm mới).
- **Logic lọc dữ liệu (`loadAuctions`):** 
  - Bổ sung một luồng `filter` mới trong `Stream API` để đối chiếu danh mục của phiên đấu giá với lựa chọn trong ComboBox.
  - Sử dụng hàm `translateCategoryToVi` có sẵn để chuyển từ Enum tiếng Anh (`ELECTRONICS`, `VEHICLE`,...) sang tiếng Việt trước khi so sánh.
  - Kết quả cuối cùng là sự kết hợp của 3 điều kiện: Trạng thái + Danh mục + Từ khoá tìm kiếm.

## 3. Trạng thái Build
- Đã chạy `mvn clean compile` thành công và không ghi nhận lỗi.
