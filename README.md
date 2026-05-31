# Hệ thống Đấu giá Trực tuyến (Online Auction System)

**Bài Tập Lớn — Lập trình Nâng cao (LTNC)** · Nhóm 11

---

## 1. Mô tả bài toán và phạm vi hệ thống

**Phạm vi hệ thống:**

- Quản lý người dùng, sản phẩm đa loại (Điện tử, Tác phẩm nghệ thuật, Phương tiện) và phiên đấu giá.
- Đấu giá thủ công và **Auto-Bidding** (maxBid, increment, ưu tiên theo thời gian đăng ký).
- Cơ chế **Anti-sniping** (gia hạn phiên khi có bid trong cửa sổ cuối).
- Ví điện tử: khóa/giải phóng tiền khi đặt giá, thanh toán khi thắng cuộc.
- Cập nhật realtime qua Socket + Observer (giá, trạng thái phiên, biểu đồ lịch sử bid).
- Kiến trúc Client–Server, nhiều client kết nối đồng thời tới một server.

---

## 2. Công nghệ sử dụng, môi trường chạy, yêu cầu cài đặt

| Hạng mục             | Chi tiết                                                  |
| :------------------- | :-------------------------------------------------------- |
| **Ngôn ngữ**         | Java 21+                                                  |
| **Build tool**       | Apache Maven 3.9+                                         |
| **Giao diện Client** | JavaFX 21 + FXML (MVC)                                    |
| **Kiến trúc**        | Client–Server qua TCP Socket (cổng `8888`)                |
| **Cơ sở dữ liệu**    | MySQL 8 (JDBC, Pessimistic Locking `SELECT … FOR UPDATE`) |
| **Thư viện JSON**    | org.json (server), json-simple (client)                   |
| **Kiểm thử**         | JUnit 5                                                   |
| **CI/CD**            | GitHub Actions (`.github/workflows/ci.yml`)               |

**Yêu cầu cài đặt:**

1. **JDK 21** trở lên (`java -version`).
2. **Maven** đã cấu hình PATH (`mvn -version`).
3. **MySQL** (XAMPP hoặc MySQL Server), cổng mặc định `3306`.
   - Username: `root`
   - Password: `1234`
   - Database được tạo tự động khi server khởi động lần đầu (xem `auction-server/src/main/resources/schema.sql`).

---

## 3. Cấu trúc thư mục / module chính

```
bai-tap-lon-ltnc/
├── auction-server/          # Server: logic nghiệp vụ, DAO, Socket, đồng bộ đấu giá
│   ├── src/main/java/server/
│   │   ├── model/           # Entity, enum, exception, strategy
│   │   ├── dao/             # Interface + JDBC implementation
│   │   ├── service/         # AuctionManager, WalletService, …
│   │   ├── handler/         # Xử lý request từ client
│   │   └── net/             # ClientSession, ClientBroadcaster
│   ├── src/test/java/       # Unit test JUnit
│   └── pom.xml
├── auction-client/          # Client JavaFX
│   ├── src/main/java/com/auction/client/
│   │   ├── controller/      # FXML Controllers (MVC)
│   │   ├── network/         # Socket handlers
│   │   ├── observer/        # AuctionObserver
│   │   └── service/         # ServerService (facade)
│   ├── src/main/resources/com/auction/client/fxml/
│   └── pom.xml
├── release/
│   ├── server.jar
│   └── client.jar
├── build-jars.bat
└── README.md
```

---

## 4. Vị trí file `.jar`

Dự án dùng **maven-shade-plugin** tạo **fat JAR / uber JAR** chạy bằng `java -jar`.

| File       | Vị trí               | Mô tả                                       |
| :--------- | :------------------- | :------------------------------------------ |
| **Server** | `release/server.jar` | Fat JAR server — **dùng để chạy / nộp bài** |
| **Client** | `release/client.jar` | Fat JAR client — **dùng để chạy / nộp bài** |

```bash
build-jars.bat
```

> **Lưu ý OS:** Client JAR build kèm native JavaFX cho **Windows** (`javafx.platform=win` trong `auction-client/pom.xml`).

---

## 5. Hướng dẫn chạy Server → Client

**Thứ tự bắt buộc:** Bật MySQL → Chạy Server → Chạy Client.

**Yêu cầu khi chạy:** JDK 21 + MySQL (username `root`, password `1234`, cổng `3306`).

### Cách chạy (fat JAR — đúng yêu cầu nộp bài)

**Bước 1 — MySQL:** Start MySQL (XAMPP, cổng 3306).

**Bước 2 — Server (Terminal 1):**

```bash
java -jar release/server.jar
```

**Bước 3 — Client (Terminal 2):**

```bash
java -jar release/client.jar
```

**Chạy nhiều Client:** Mở thêm terminal, lặp lại:

```bash
java -jar release/client.jar
```

---

## 6. Danh sách chức năng đã hoàn thành

### Chức năng nghiệp vụ

| Chức năng              | Mô tả                                                              |
| :--------------------- | :----------------------------------------------------------------- |
| Đăng ký / Đăng nhập    | Bidder, Seller, Admin; mật khẩu hash                               |
| Quản lý sản phẩm       | Seller tạo/sửa/xóa item theo danh mục (Factory Pattern)            |
| Tạo & mở phiên đấu giá | Seller khởi tạo phiên, set giá khởi điểm, bước giá, thời gian      |
| Đặt giá thủ công       | Bidder đặt giá trong phòng đấu giá                                 |
| Auto-Bidding           | Đăng ký maxBid + increment; server tự đấu giá (Dùng PriorityQueue) |
| Anti-sniping           | Gia hạn phiên khi bid trong cửa sổ cuối                            |
| Ví điện tử             | Nạp tiền, khóa/giải phóng khi outbid, trừ tiền khi thắng           |
| Lịch sử bid            | ListView + biểu đồ LineChart realtime                              |
| Admin                  | Quản lý ví toàn hệ thống                                           |
| Realtime               | Push bid/status/wallet qua Socket + Observer                       |

### Đối chiếu barem điểm (rubric)

| Tiêu chí                         | Điểm |   Mức    | Trạng thái | Bằng chứng ngắn                                                                                                                      |
| :------------------------------- | :--: | :------: | :--------: | :----------------------------------------------------------------------------------------------------------------------------------- |
| Thiết kế lớp & cây kế thừa       | 0.5  | Bắt buộc |     ✅     | `User`→`Bidder`/`Seller`/`Admin`; `Item`→`Electronics`/`Artwork`/`Vehicle`; `Auction`, `BidTransaction`, `Wallet`                    |
| OOP (4 tính chất)                | 1.0  | Bắt buộc |     ✅     | Encapsulation (private fields); Inheritance; Polymorphism (`BidStrategy`); Abstraction (`User`, `Item`, DAO interfaces)              |
| Design Patterns                  | 1.0  | Bắt buộc |     ✅     | Singleton (`AuctionManager`, `DatabaseConfig`), Factory (`ItemFactory`), Strategy (`BidStrategy`), Observer (`AuctionObserver`), DAO |
| Quản lý người dùng & sản phẩm    | 1.0  | Bắt buộc |     ✅     | `AuthHandlers`, `CatalogHandlers`, `SellerDashboardController`                                                                       |
| Chức năng đấu giá                | 1.0  | Bắt buộc |     ✅     | `BiddingHandlers`, `AuctionManager.placeBid()`                                                                                       |
| Xử lý lỗi & ngoại lệ             | 1.0  | Bắt buộc |     ✅     | `AuctionException`, `InvalidBidException`, `AuthenticationException`; test `TestAuctionException`                                    |
| Concurrency (không mất cập nhật) | 1.0  | Bắt buộc |     ✅     | `synchronized(auction)`, `ConcurrentHashMap`, `SELECT … FOR UPDATE`                                                                  |
| Realtime (Observer/Socket)       | 0.5  | Bắt buộc |     ✅     | `ClientBroadcaster`, `AuctionObserver`, push JSON                                                                                    |
| Client–Server                    | 0.5  | Bắt buộc |     ✅     | TCP Socket cổng 8888, `Server.java`, `SocketConnection`                                                                              |
| MVC                              | 0.5  | Bắt buộc |     ✅     | FXML + Controller (client); Model + DAO (server)                                                                                     |
| Maven, coding convention         | 0.5  | Bắt buộc |     ✅     | Maven multi-module, Spotless Google Java Style                                                                                       |
| Unit Test JUnit                  | 0.5  | Bắt buộc |     ✅     | 4 file test, 30+ test case trong `auction-server/src/test/java/`                                                                     |
| CI/CD GitHub Actions             | 0.5  | Bắt buộc |     ✅     | `.github/workflows/ci.yml`                                                                                                           |
| **Auto-Bidding**                 | 0.5  | Tuỳ chọn |     ✅     | `AutoBidStrategy` + maxBid/increment hoạt động; **đã dùng `PriorityQueue`** (thay cho vòng lặp cũ)                                   |
| **Anti-sniping**                 | 0.5  | Tuỳ chọn |     ✅     | `Auction.ANTI_SNIPE_WINDOW_SECONDS`, `extendEndTime()`                                                                               |
| **Bid History Visualization**    | 0.5  | Tuỳ chọn |     ✅     | `LineChart` trong `bidding-room.fxml`, cập nhật qua Observer                                                                         |

---

## 7. Link báo cáo PDF và video demo

- **Báo cáo PDF:** [https://drive.google.com/file/d/105np4jyQ9K0evI9sWoFKNvOj5JN_7Sl4/view?usp=sharing](#)
- **Video demo:** [https://www.youtube.com/watch?v=JOw7UVB6rEY](#)

---

## Ghi chú cho giảng viên / người chấm

- Chạy bằng **`java -jar release/server.jar`** và **`java -jar release/client.jar`** — không cần Maven hay IDE.
- File fat JAR nằm trong thư mục **`release/`** (đã gói đủ thư viện).
- Mật khẩu MySQL mặc định `1234` — có thể sửa trong `DatabaseConfig.java` nếu môi trường khác.
