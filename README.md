# 🛒 HỆ THỐNG ĐẤU GIÁ TRỰC TUYẾN (ONLINE AUCTION SYSTEM)

Bài Tập Lớn môn Lập trình Nâng Cao (LTNC) - Thực hiện bởi **Nhóm 11**.

## 1. MÔ TẢ BÀI TOÁN VÀ PHẠM VI HỆ THỐNG
**Bài toán:** Xây dựng một nền tảng đấu giá trực tuyến thời gian thực, nơi người dùng có thể đóng vai trò là Người bán (Seller) để đăng bán sản phẩm hoặc Người mua (Bidder) để tham gia đấu giá.
**Phạm vi hệ thống:**
- Hệ thống hỗ trợ đa dạng loại sản phẩm (Tác phẩm nghệ thuật, Đồ điện tử, Phương tiện...).
- Xử lý đấu giá thời gian thực (Real-time) và đảm bảo tính toàn vẹn dữ liệu khi có nhiều người cùng trả giá đồng thời (Concurrency).
- Có tính năng Auto-Bidding (Đấu giá tự động) và các cơ chế chống Snipe (Anti-sniping).

## 2. CÔNG NGHỆ SỬ DỤNG VÀ YÊU CẦU CÀI ĐẶT
**Công nghệ sử dụng:**
- **Ngôn ngữ:** Java (JDK 21+)
- **Kiến trúc:** Client - Server qua TCP Socket.
- **Giao diện (Client):** JavaFX (MVC Pattern với FXML).
- **Cơ sở dữ liệu:** MySQL (sử dụng Row-level Locking với `Pessimistic Locking`).
- **Quản lý dự án:** Apache Maven.

**Yêu cầu cài đặt & Môi trường chạy:**
1. **Java JDK:** Phiên bản 21 trở lên.
2. **Apache Maven:** Đã được cài đặt và thêm vào biến môi trường (hoặc dùng Maven tích hợp trong IDE).
3. **Cơ sở dữ liệu MySQL:**
   - Sử dụng XAMPP hoặc cài MySQL Server độc lập.
   - Chạy ở cổng mặc định `3306`.
   - Cấu hình tài khoản: username = `root`, password = `1234`.
   *(Lưu ý: Không cần tạo database trước, hệ thống sẽ tự động khởi tạo khi Server chạy lần đầu).*

## 3. CẤU TRÚC THƯ MỤC VÀ MODULE CHÍNH
Dự án được chia thành các module chính:
- `auction-server`: Chứa mã nguồn phía Server (xử lý logic nghiệp vụ chính, kết nối CSDL, quản lý các kết nối Socket của Client, đồng bộ hóa đấu giá).
- `auction-client`: Chứa mã nguồn phía Client (giao diện người dùng JavaFX, nhận và gửi request tới Server, hiển thị dữ liệu real-time).
- `demo`: Thư mục chứa mã nguồn demo.
- `.github`: Cấu hình CI/CD GitHub Actions.

## 4. VỊ TRÍ CÁC FILE `.jar`
Sau khi tiến hành đóng gói (Build) bằng Maven, các file `.jar` sẽ được sinh ra ở các vị trí sau:
- **Server:** `auction-server/target/auction-server-1.0-SNAPSHOT.jar`
- **Client:** `auction-client/target/auction-client-1.0-SNAPSHOT.jar`
- *(Lưu ý: Bạn có thể tìm thấy một bản build mẫu trong thư mục `demo/target/demo-1.0-SNAPSHOT.jar`)*

## 5. HƯỚNG DẪN CHẠY HỆ THỐNG
*Thứ tự khởi chạy bắt buộc: Bật MySQL -> Chạy Server -> Chạy Client.*

**Cách 1: Sử dụng Script tự động (Windows)**
- Click đúp vào file `run.bat` ở thư mục gốc. Script sẽ tự động build, khởi chạy Server, đợi 10 giây rồi khởi chạy Client.

**Cách 2: Chạy thủ công qua Terminal**
**Bước 1: Khởi động CSDL MySQL**
- Mở XAMPP và Start dịch vụ MySQL (cổng 3306).

**Bước 2: Chạy Server**
Mở Terminal tại thư mục gốc của dự án:
```bash
cd auction-server
mvn clean install -DskipTests
mvn exec:java
```

**Bước 3: Chạy Client**
Mở thêm một Terminal mới tại thư mục gốc:
```bash
cd auction-client
mvn javafx:run
```

## 6. DANH SÁCH CHỨC NĂNG ĐÃ HOÀN THÀNH

Dưới đây là bảng tổng hợp các chức năng đã hoàn thành theo yêu cầu đồ án:

| Nội dung | Điểm | Mức độ | Trạng thái |
| :--- | :---: | :---: | :---: |
| Thiết kế lớp và cây kế thừa | 0.5 | Bắt buộc | ✅ Hoàn thành |
| Áp dụng OOP (Encapsulation, Inheritance, Polymorphism, Abstraction) | 1.0 | Bắt buộc | ✅ Hoàn thành |
| Design Patterns phù hợp | 1.0 | Bắt buộc | ✅ Hoàn thành |
| Quản lý người dùng, sản phẩm | 1.0 | Bắt buộc | ✅ Hoàn thành |
| Chức năng đấu giá | 1.0 | Bắt buộc | ✅ Hoàn thành |
| Xử lý lỗi & ngoại lệ | 1.0 | Bắt buộc | ✅ Hoàn thành |
| Xử lý đấu giá đồng thời (concurrency) | 1.0 | Bắt buộc | ✅ Hoàn thành |
| Realtime update (Observer/Socket) | 0.5 | Bắt buộc | ✅ Hoàn thành |
| Kiến trúc Client-Server | 0.5 | Bắt buộc | ✅ Hoàn thành |
| MVC (JavaFX + FXML, Controller-Model-DAO) | 0.5 | Bắt buộc | ✅ Hoàn thành |
| Maven/Gradle, coding convention | 0.5 | Bắt buộc | ✅ Hoàn thành |
| Unit Test (JUnit) | 0.5 | Bắt buộc | ✅ Hoàn thành |
| CI/CD (GitHub Actions) | 0.5 | Bắt buộc | ✅ Hoàn thành |
| Auto-Bidding | 0.5 | Tuỳ chọn | ✅ Hoàn thành |
| Anti-sniping | 0.5 | Tuỳ chọn | ✅ Hoàn thành |
| Bid History Visualization | 0.5 | Tuỳ chọn | ✅ Hoàn thành |

## 7. LIÊN KẾT TÀI LIỆU (BÁO CÁO & VIDEO DEMO)
- **Báo cáo PDF:** [Chèn Link Báo Cáo PDF Vào Đây](#)
- **Video Demo:** [Chèn Link Video Demo Vào Đây](#)
