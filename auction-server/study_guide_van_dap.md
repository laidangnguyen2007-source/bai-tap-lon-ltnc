# BÍ KÍP VẤN ĐÁP 10 ĐIỂM (Dành cho Thành Viên 1 - Core & Data)

Tài liệu này lưu lại các "bẫy" và câu hỏi đào sâu về bản chất mà các thầy hay hỏi để phân loại sinh viên khá và giỏi. Tuyệt đối không học vẹt, hãy hiểu lý do đằng sau mỗi dòng code.

---

## 1. Bản chất OOP và Kiến trúc Tách Lớp (Layered Architecture)

### Câu hỏi 1: "Em bảo em dùng Đa hình (Polymorphism), thế em dùng ở đâu? Chỉ vào code cho thầy đi!"
* **Cạm bẫy:** Hỏi để xem bạn có hiểu mình viết gì không, hay copy từ mạng.
* **Cách trả lời 10đ:** "Dạ thưa thầy, em dùng đa hình rất rõ ở `JdbcUserDao` (cụ thể là hàm `mapRow`).
  Bảng `users` trong MySQL của em gom trọn tất cả người dùng lại (Single Table Inheritance) và dùng 1 cột là `Role` để phân biệt. Khi lấy dữ liệu (ResultSet), hàm `mapRow` của em dùng lệnh `switch(role)` để sinh ra một thực thể con là `Bidder`, `Seller`, hoặc `Admin`. Cả 3 anh này đều kế thừa (extends) từ `User`. 
  Lên đến tầng `UserService`, em chỉ trả về đúng kiểu chung chung là `User`. Các hàm nghiệp vụ tự động hành xử chuẩn xác mà tịnh không cần phải viết `if(user instanceof Bidder)` rườm rà. Tính đa hình giúp xoá sạch logic if-else bùng nhùng ạ."

### Câu hỏi 2: "Tại sao em phải chia ra Interface (GenericDao, UserDao) rồi lại làm class implement (JdbcUserDao)? Tại sao không viết thẳng vào 1 class cho nhanh?"
* **Cạm bẫy:** Kiểm tra kiến thức về Dependency Inversion (chữ D trong SOLID) và nguyên lý thiết kế Interface.
* **Cách trả lời 10đ:** "Dạ em chia tách Interface vì **2 lý do cốt lõi**:
  * **Thứ nhất (Đặt Hợp Đồng):** `GenericDao` (Interface) nó đóng vai trò là một cái bản 'Hợp đồng'. Nó hô to: 'Ai muốn làm DAO quản lý dữ liệu thì phải tuân thủ việc cung cấp hàm `save`, `findById`!...'.
  * **Thứ hai (Chống Rò rỉ kiến trúc - Leaky Abstraction):** Nhờ có Interface, ở tầng `UserService`, em chỉ làm việc với Interface `UserDao`. Tầng Service CỦA EM HOÀN TOÀN KHÔNG BIẾT Ở DƯỚI ĐANG XÀI CÔNG NGHỆ GÌ (RAM hay MySQL hay MongoDB). Nếu thầy nhìn vào `UserService`, thầy sẽ không bao giờ thấy chữ `SQLException` nào lọt lên trên cả, em đã dùng `try-catch` bọc lại hết thành `AuctionException` ở tầng implementation rồi. Viết thẳng 1 class thì Service sẽ bị dính chặt vào công nghệ SQL, sau này đổi CSDL thì mệt mỏi đập toàn bộ code ạ."

---

## 2. Concurrency (Tính Đa luồng) - Sự tiến hóa từ RAM ảo lên thẳng MySQL

### Câu hỏi 3: "Ngày xưa hoặc các nhóm khác hay giả lập lưu RAM dùng `ConcurrentHashMap` để chống đập luồng. Sang Kiến trúc MySQL này, khả năng chịu tải đa luồng của hệ thống em nằm ở đâu?"
* **Cạm bẫy:** Rớt môn tại đây nếu không giải thích được vì sao dự án cần đa luồng và CSDL quản lý nó thế nào.
* **Cách trả lời 10đ:** "Dạ thưa thầy, đây chính là quyết định chiến lược của nhóm em khi chuyển sang MySQL. Khi 1000 người cùng bấm 'Đầu giá', nếu không khóa, dữ liệu sẽ bị giẫm đạp (Race Condition).
  Ban đầu trong hệ RAM, em phải tự khoá luồng (Lock) thủ công bằng `ConcurrentHashMap`. Nhưng sang kiến trúc này, em giao phó bài toán Locking ác mộng đó cho Engine InnoDB của MySQL.
  Trong `JdbcAuctionDao`, lúc truy vấn để đặt giá, em xài câu lệnh **`SELECT ... FOR UPDATE`**. Sức mạnh của nó là **Khóa bi quan (Pessimistic Locking)** cấp độ dòng (**Row-Level Locking**). Nghĩa là ông MySQL chặn đúng kỷ luật cái phiên đấu giá điện thoại iPhone đó lại một phần nghìn giây cho 1 user xử lý, 999 anh còn lại phải xếp hàng ngầm. Cùng lúc đó, các user mua cái bức tranh ở dòng khác vẫn mua bình thường nhờ khóa theo dòng chứ không khóa toàn bản. Độ ổn định và chịu tải là cấp doanh nghiệp ạ."

### Câu hỏi 4: "Cái ID của User hay Sản phẩm, xưa dùng `AtomicLong` (để id++ không bị đụng hàng), giờ dùng MySQL thì ai quản lý giùm em việc trùng ID khi 1000 người cùng lúc tạo tài khoản?"
* **Cách trả lời 10đ:** "Dạ thưa thầy, em sử dụng thẳng tính năng **`AUTO_INCREMENT`** của khóa chính bên MySQL. Thay vì phải tự bóp não kiểm soát `id++` bằng phần cứng (`AtomicLong`), tầng Database đã giang tay gánh vác trách nhiệm sinh ID duy nhất và an toàn luồng (Thread-safe) tuyệt đối.
  Lúc Insert (`save`), em dùng cờ `Statement.RETURN_GENERATED_KEYS` để bốc cái ID mới cứng cựa vừa ra lò bên Database đắp ngược vào RAM của Java lại luôn. Máy chủ backend có sập cúp điện khởi động lại thì bộ đếm ID vẫn tiến lên tiếp không bao giờ có đoạn 2 tài khoản trùng ID ạ."

---

## 3. Java 8 & Design

### Câu hỏi 5: "Lúc tìm theo ID `findById`, sao em không trả về luôn `User` (chẳng hạn User u = ...), mà lại bọc nó trong cái hộp lạ quắc `Optional<User>` làm cái gì?"
* **Cách trả lời 10đ:** "Dạ em dùng `Optional` của Java 8 để chặn đứng cái lỗi khét tiếng `NullPointerException`.
  Giả sử hồi xưa em trả về `User` (mà tìm không thấy thì trả về `null`). Trên phần Service, nếu đứa code cùng nhóm em nó não cá vàng quên không kiểm tra `if (u != null)`, nó lôi ra `u.getName()` một phát là văng Code 500 sập ứng dụng.
  Em trả về `Optional` là em chặn sẵn trước: 'Ê, mở cái hộp ra mà xem, bên trong có thể KHÔNG CÓ USER đâu'. Nhìn vào `UserService`, thầy sẽ thấy em bị ép phải xài hậu tố `.orElseThrow(...)` để quăng ra 1 cái lỗi trong tầm kiểm soát. Code từ DAO cho đến Service bao sạch, bao an toàn ạ."

---

## 4. Bảo mật & Triển khai

### Câu hỏi 6: "Mật khẩu em lưu thẳng thế kia vào Database à?"
* **Cách trả lời 10đ:** "Dạ không bao giờ. Mật khẩu nằm trong Database của em thực chất là cục băng đã bị băm nát bằng hàm Băm 1 chiều thuật toán `SHA-256` (qua util `PasswordUtil`).
  Lúc User đăng nhập, em lấy mật khẩu User vừa gõ, quăng nó vào máy xay `SHA-256`, ra 1 đoạn mã Hash mới, rồi đối chiếu xem 2 đoạn mã này có khớp nhau không (`verifyPassword`). Hacker có hack thẳng vào MySQL thì cũng chỉ nhìn thấy đống chữ loằng ngoằng, không dịch ngược ra được Password thật ạ. 
  Nếu lên mức thương mại em sẽ rắc thêm mắm muối (Salt) chống Rainbow Table, nhưng ở đồ án này thì SHA-256 là đã vượt tiêu chuẩn rồi ạ."

### Câu hỏi 7: "Làm thế nào mà thầy không cần bật trình quản lý MySQL lên bấm Create Database thủ công mà Java nó vẫn tự chạy được cơ sở dữ liệu của nhóm em?"
* **Cách trả lời 10đ:** "Dạ tụi em tinh tế để ý luôn cả trải nghiệm của thao tác chấm bài ạ! (cười)
  Trong `DatabaseConfig.java`, ở đường dẫn kết nối JDBC URL em đã chủ động nhúng cờ `createDatabaseIfNotExist=true`. Cứ thầy gõ phím khởi động là Java sẽ lao xuống hỏi cổng 3306, phát hiện thầy chưa có kho `auction_db` nó sẽ báo MySql cấu trúc tự động 100%. Thầy chỉ cần để tâm đến việc review Logic và Design thay vì phải mồ hôi hột config lại môi trường giống máy tụi em."
