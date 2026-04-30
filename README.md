# 🛒 HỆ THỐNG ĐẤU GIÁ TRỰC TUYẾN (ONLINE AUCTION SYSTEM)

Bài Tập Lớn môn Lập trình Nâng Cao (LTNC) - Xây dựng hệ thống phòng đấu giá thời gian thực với kiến trúc Client-Server. Thực hiện bởi **Nhóm 11** (4 thành viên).

## 🌟 TÍNH NĂNG NỔI BẬT
- **Thời gian thực (Real-time):** Cập nhật giá thầu siêu tốc bằng công nghệ TCP Socket.
- **An toàn đồng thời (Concurrency):** Chống nứt gãy dữ liệu (Race Condition) bằng `Pessimistic Locking` (FOR UPDATE) của MySQL.
- **Giao diện trực quan:** Ứng dụng Desktop JavaFX theo chuẩn mô hình MVC.
- **Kiến trúc dữ liệu:** Sử dụng `Single Table Inheritance` tối ưu hóa lưu trữ đa hình (Bidder, Seller, Admin) và (Artwork, Electronics, Vehicle).

---

## ⚠️ YÊU CẦU MÔI TRƯỜNG BẮT BUỘC (CRITICAL)

Dự án này sử dụng CSDL **MySQL (Client-Server)** để demo chức năng **Row-level Locking** nâng cao (Khóa theo dòng), do đó hệ thống **KHÔNG PHẢI LÀ IN-MEMORY**. Thầy/Cô vui lòng bật MySQL Server trước khi chạy code.

1. **Java JDK:** Phiên bản 21 trở lên.
2. **Apache Maven:** Tích hợp sẵn trong IDE hoặc cài rời.
3. **XAMPP / MySQL Server:** Khởi chạy MySQL ở cổng mặc định `3306`, tài khoản `root` và mật khẩu để trống. 

---

## 🚀 HƯỚNG DẪN KHỞI CHẠY DÀNH CHO GIẢNG VIÊN

### Bước 1: Khởi động Động cơ CSDL (MySQL)
1. Mở phần mềm **XAMPP Control Panel**.
2. Nhấn nút **Start** ở dòng **MySQL** (Đảm bảo đang chạy ở Port 3306).
3. *(Lưu ý: Không cần tạo database bằng tay. Nhóm đã tự động hóa bước này).*

### Bước 2: Đóng gói và Khởi chạy Server Backend
Mở Terminal tại thư mục gốc của bài tập, di chuyển vào phần Server và đóng gói thư viện:
```bash
cd auction-server
mvn clean install -DskipTests
mvn exec:java
```
*(Ngay lần bật Server đầu tiên, mã nguồn Java sẽ tự động dò tìm cấu hình, tạo một Database tên là `auction_db` và xây dựng cấu trúc các Bảng).*

### Bước 3: Khởi chạy Giao diện Người dùng (Client)
Di chuyển sang thư mục Client và kích hoạt JavaFX:
```bash
cd ../auction-client
mvn javafx:run
```

Lúc này, cửa sổ ứng dụng Desktop sẽ hiện lên. Thầy/Cô có thể tiến hành Đăng ký tài khoản và trải nghiệm luồng Đấu giá.

---
*Bài Tập Lớn Lập trình Nâng Cao - Thực hiện bởi Nhóm 11.*
