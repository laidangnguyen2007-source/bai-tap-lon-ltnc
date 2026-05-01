# 🎓 MASTER VẤN ĐÁP — HỆ THỐNG ĐẤU GIÁ TRỰC TUYẾN (NHÓM 11)

> **Mục tiêu:** Đọc file này 1 lần, trả lời được MỌI câu hỏi thầy hỏi.
> Mỗi mục đều có: **Câu hỏi thầy hay hỏi → Câu trả lời chuẩn + Chỉ đúng dòng code.**

---

## 1. SINGLETON PATTERN — `DatabaseConfig.java`

### Mục đích
Toàn hệ thống Server chỉ được mở **đúng 1 kết nối** tới MySQL. Nếu mở nhiều → "Too many connections" → crash.

### Cấu trúc Singleton (4 thành phần bắt buộc)

```java
// 1. Instance duy nhất — private static volatile (QUAN TRỌNG: volatile bắt buộc)
private static volatile DatabaseConfig instance;

// 2. Constructor private — ngăn ai bên ngoài gọi new DatabaseConfig()
private DatabaseConfig() throws SQLException { ... }

// 3. Phương thức truy cập — Double-Checked Locking
public static DatabaseConfig getInstance() throws SQLException {
    DatabaseConfig result = instance;
    // Lệnh isClosed() đóng vai trò cơ chế Tự phục hồi (Self-Healing)
    if (result == null || result.connection.isClosed()) {
        synchronized (DatabaseConfig.class) {
            result = instance;
            if (result == null || result.connection.isClosed()) {
                instance = result = new DatabaseConfig(); // 4. Khởi tạo
            }
        }
    }
    return result;
}
```

### ❓ Câu hỏi thầy hay hỏi

**Q: Tại sao phải dùng `volatile`?**
> Không có `volatile`, CPU có thể **sắp xếp lại thứ tự lệnh** (instruction reordering). Một luồng có thể thấy `instance != null` nhưng object bên trong chưa được khởi tạo xong → NullPointerException. `volatile` đảm bảo **tất cả luồng đều thấy giá trị mới nhất** từ RAM thay vì cache CPU.

**Q: Tại sao check 2 lần (Double-Checked Locking)?**
> - **Check lần 1** (ngoài `synchronized`): Nếu đã có instance rồi thì KHÔNG vào `synchronized` — tránh bottleneck vì `synchronized` rất chậm (mỗi luồng phải xếp hàng).
> - **Check lần 2** (trong `synchronized`): Phòng trường hợp 2 luồng cùng qua Check 1, rồi 1 luồng tạo xong, luồng kia vào `synchronized` lại tạo thêm 1 cái nữa.

**Q: `isClosed()` trả về gì và dùng để làm gì?**
> `isClosed()` là hàm của `java.sql.Connection`, trả về `boolean`:
> - `true` → Sợi dây kết nối với MySQL đã **bị đứt** (timeout, mất mạng, server MySQL tắt).
> - `false` → Kết nối vẫn **đang sống**.
>
> Tác dụng: **Self-Healing** — khi kết nối chết, lần gọi `getInstance()` tiếp theo sẽ tự động tạo kết nối mới. Server tự phục hồi mà không cần restart.

**Q: Tại sao không dùng `synchronized` trên cả method `getInstance()`?**
> Nếu dùng `public static synchronized DatabaseConfig getInstance()`, mỗi lần gọi đều phải khóa — kể cả khi instance đã tồn tại. Với hàng nghìn request đồng thời, tất cả đều phải xếp hàng → **bottleneck**. Double-Checked Locking chỉ khóa đúng lần đầu tiên khởi tạo.

---

## 2. FACTORY METHOD PATTERN — `ItemFactory.java`

### Mục đích
Thay vì gọi `new Artwork(...)`, `new Electronics(...)` rải rác khắp nơi, gom tất cả về **một điểm duy nhất**. Thêm loại Item mới → chỉ sửa `ItemFactory`, không đụng code khác.

### Cấu trúc

```
model/entity/item/
├── Item.java          (abstract — lớp cha chung)
├── Artwork.java       (constructor package-private ← chỉ ItemFactory gọi được)
├── Electronics.java   (constructor package-private ← chỉ ItemFactory gọi được)
├── Vehicle.java       (constructor package-private ← chỉ ItemFactory gọi được)
└── ItemFactory.java   (cùng package → gọi được constructor package-private)
```

### Hai nhiệm vụ của ItemFactory

```java
// Nhiệm vụ 1: Tạo Item MỚI từ dữ liệu người dùng
Item item = ItemFactory.create(ItemCategory.ELECTRONICS, Map.of(
    "name", "Laptop Dell", "startingPrice", 1000.0, ...
));

// Nhiệm vụ 2: Phục dựng Item từ Database (dùng trong JdbcItemDao.mapRow())
Electronics e = ItemFactory.reconstructElectronics(id, createdAt, name, ...);
```

### ❓ Câu hỏi thầy hay hỏi

**Q: Tại sao `ItemFactory` phải nằm CÙNG package với các Entity?**
> Vì constructor của `Artwork/Electronics/Vehicle` là **package-private** (không có chữ `public`). Java cho phép code cùng package gọi constructor package-private. Nhờ đó, bên ngoài package `model.entity.item` KHÔNG AI có thể `new Artwork(...)` trực tiếp → bắt buộc phải đi qua `ItemFactory` → Factory Pattern được áp dụng **triệt để**.

**Q: Nếu thêm loại Item mới, ví dụ `RealEstate`, thì phải sửa những gì?**
> 1. Tạo class `RealEstate extends Item`
> 2. Thêm `REAL_ESTATE` vào enum `ItemCategory`
> 3. Thêm `case REAL_ESTATE ->` vào `ItemFactory.create()`
> 4. Thêm `reconstructRealEstate()` vào `ItemFactory`
>
> Đây chính là lợi ích của Factory — thay đổi tập trung, không có "magic new" nằm rải rác.

**Q: `JdbcItemDao.mapRow()` không thể `new Artwork()` nữa thì làm thế nào?**
> `JdbcItemDao` gọi `ItemFactory.reconstructArtwork(id, createdAt, ...)`. Phương thức `reconstruct*()` là `public static` nên ai cũng gọi được, còn bên trong nó gọi constructor package-private (được phép vì cùng package). 

---

## 3. SINGLETON PATTERN — `AuctionManager.java`

### Mục đích
Là **bộ não trung tâm** quản lý tất cả phiên đấu giá đang RUNNING trong RAM. Đảm bảo mọi luồng đều nhìn vào **cùng một danh sách phiên**.

### Điểm nổi bật (Khác biệt so với Map thông thường)

```java
// Nếu xài HashMap thông thường: Phải dùng synchronized bọc lại cực kỳ chậm
// AuctionManager: dùng ConcurrentHashMap — thread-safe với khóa phân mảnh!
private final Map<Long, Auction> activeAuctions = new ConcurrentHashMap<>();
```

### Logic đặt giá — Đây là nơi thể hiện Concurrency đỉnh nhất

```java
public Auction placeBid(Long auctionId, Long bidderId, double bidPrice) {
    Auction auction = findActiveAuction(auctionId);
    
    synchronized (auction) {  // Khóa trên TỪNG phiên riêng lẻ, không khóa cả Manager
        if (!auction.isRunning()) throw new AuctionClosedException(...);
        if (bidPrice <= auction.getCurrentPrice()) throw new InvalidBidException(...);
        
        // Anti-sniping: bid trong 30 giây cuối → gia hạn thêm 30 giây
        long secondsLeft = Duration.between(now, auction.getEndTime()).getSeconds();
        if (secondsLeft <= Auction.ANTI_SNIPE_WINDOW_SECONDS) {
            auction.extendEndTime(Auction.EXTENSION_SECONDS);
        }
        
        auction.applyBid(bidPrice, bidderId);
    }
    return auction;
}
```

### ❓ Câu hỏi thầy hay hỏi

**Q: Tại sao dùng `ConcurrentHashMap` thay vì `HashMap`?**
> `HashMap` không an toàn khi nhiều luồng đọc/ghi đồng thời → có thể bị **data corruption**. `ConcurrentHashMap` khóa ở cấp độ từng bucket (segment), nhiều luồng đọc song song hoàn toàn không block nhau → hiệu năng cao hơn `Collections.synchronizedMap()` rất nhiều.

**Q: `synchronized (auction)` khác gì `synchronized (AuctionManager.class)`?**
> - `synchronized (AuctionManager.class)` → Khóa TOÀN BỘ Manager. Bidder A đặt giá phiên 1 sẽ chặn Bidder B đặt giá phiên 2 — vô lý và chậm.
> - `synchronized (auction)` → Chỉ khóa **đúng phiên đấu giá đó**. Bidder A và B có thể đặt giá 2 phiên khác nhau **đồng thời** mà không block nhau.

**Q: Anti-sniping là gì? Tại sao cần?**
> Anti-sniping là cơ chế **gia hạn thời gian** khi có người bid vào phút cuối. Trên thực tế, nhiều người cố tình chờ 1-2 giây trước khi hết giờ mới đặt giá — những người khác không còn thời gian phản ứng. Anti-sniping ngăn điều này bằng cách tự động thêm 30 giây, đảm bảo công bằng.

---

## 4. OBSERVER PATTERN — `BiddingRoomController.java`

### Mục đích
Khi Bidder A đặt giá thành công, **tất cả Client đang xem phiên đó** phải tự động cập nhật giá mới — không cần ai nhấn F5.

### Cấu trúc Observer

```
Subject (Publisher):  ServerService
Observer (Subscriber): BiddingRoomController implements AuctionObserver
```

```java
// Đăng ký làm Observer — gọi khi MỞ màn hình
serverService.addObserver(this);

// Hủy đăng ký — gọi khi RỜI màn hình (tránh memory leak!)
serverService.removeObserver(this);

// Method nhận thông báo từ Server — được gọi tự động khi có bid mới
@Override
public void onBidUpdated(BidTransaction bid) {
    // QUAN TRỌNG: phải dùng Platform.runLater() vì method này chạy trên
    // background thread, mà JavaFX chỉ cho sửa UI trên Application Thread
    Platform.runLater(() -> {
        addBidToChart(bid);
        currentPriceLabel.setText(String.format("%,.0f VNĐ", bid.getAmount()));
        addBidToHistoryList(bid);
    });
}
```

### ❓ Câu hỏi thầy hay hỏi

**Q: Tại sao phải `removeObserver(this)` trước khi rời màn hình?**
> Nếu không remove, `ServerService` vẫn giữ tham chiếu tới Controller cũ → **Memory Leak**. Object Controller không được Garbage Collector thu hồi dù đã không dùng nữa. Tệ hơn, mỗi lần có bid mới, code vẫn cố cập nhật UI đã bị đóng → crash.

**Q: Tại sao phải dùng `Platform.runLater()`?**
> JavaFX có quy tắc bất di bất dịch: chỉ **JavaFX Application Thread** được phép chạm vào UI component. `onBidUpdated()` được gọi từ **background network thread**. Vi phạm → `IllegalStateException`. `Platform.runLater()` đưa code vào hàng đợi của JavaFX Thread để thực thi an toàn.

---

## 5. EXCEPTION HIERARCHY

```
AuctionException (lớp cha — RuntimeException)
├── InvalidBidException    → Đặt giá thấp hơn giá hiện tại
├── AuctionClosedException → Đặt giá khi phiên đã đóng
└── AuthenticationException → Sai username/mật khẩu khi đăng nhập
```

### ❓ Câu hỏi thầy hay hỏi

**Q: Tại sao cần tạo Exception riêng thay vì dùng IOException hay RuntimeException?**
> Exception riêng biệt giúp **catch có chọn lọc**. Controller có thể xử lý khác nhau:
> - Bắt `AuthenticationException` → hiển thị "Sai mật khẩu"
> - Bắt `InvalidBidException` → hiển thị "Giá phải cao hơn"
> - Bắt `AuctionException` chung → hiển thị lỗi generic
>
> Đây là nguyên tắc **SRP**: mỗi loại lỗi có class riêng, xử lý riêng.

**Q: Tại sao `AuctionException extends RuntimeException` mà không phải `Exception`?**
> `RuntimeException` là **unchecked** — không bắt buộc phải try-catch hoặc khai báo `throws`. Điều này giúp code Service/DAO sạch hơn, không bị ô nhiễm bởi `throws` ở mọi method. Các lỗi nghiệp vụ (sai giá, sai mật khẩu) thường không cần recover ở mọi tầng.

---

## 6. NGUYÊN TẮC SOLID — Áp dụng vào đâu?

| Nguyên tắc | Nghĩa ngắn gọn | Ví dụ trong dự án |
| :--- | :--- | :--- |
| **S** — Single Responsibility | Mỗi class 1 nhiệm vụ | `UserService` chỉ lo User, `ItemService` chỉ lo Item |
| **O** — Open/Closed | Mở cho mở rộng, đóng với sửa | Thêm `Admin` chỉ cần tạo class mới, không sửa `User` |
| **L** — Liskov Substitution | Class con thay thế được cha | `Bidder`, `Seller` dùng được ở mọi nơi nhận `User` |
| **I** — Interface Segregation | Interface nhỏ, đúng việc | `UserDao`, `ItemDao`, `AuctionDao` tách riêng theo domain |
| **D** — Dependency Inversion | Phụ thuộc vào Interface | `ItemService(ItemDao itemDao)` — nhận Interface, không phải `JdbcItemDao` |

### ❓ Q: Tại sao `JdbcItemDao.bindItemTypeFields()` dùng `instanceof` mà vẫn là SOLID?

> Đây là **trade-off tất yếu của Single Table Inheritance (STI)**. Nếu đưa logic SQL binding vào `Item.java` để tránh instanceof, class `Item` (Domain Model) sẽ phụ thuộc vào `java.sql.PreparedStatement` (Persistence Layer) — vi phạm **nguyên tắc phân tầng nghiêm trọng hơn**. Trade-off này được chấp nhận rộng rãi trong thiết kế STI.

---

## 7. CONCURRENCY — Cơ chế chống Race Condition

### Tầng Java (In-memory)

```java
// AuctionManager.placeBid() — synchronized trên từng Auction object
synchronized (auction) {
    // Chỉ 1 luồng được vào đặt giá cho phiên này tại một thời điểm
    auction.applyBid(bidPrice, bidderId);
}
```

### Tầng Database (MySQL — Pessimistic Locking)

```sql
-- JdbcAuctionDao — khóa dòng DB trước khi đọc, không ai đọc/ghi được trong transaction
SELECT * FROM auctions WHERE id = ? FOR UPDATE
```

### ❓ Q: `synchronized` và `FOR UPDATE` khác nhau thế nào?

> - `synchronized` → Chống tranh chấp trong **bộ nhớ Java** (giữa các luồng cùng Server).
> - `FOR UPDATE` → Chống tranh chấp tại **tầng Database** (giữa các request khác nhau, kể cả từ nhiều Server).
>
> Hai lớp bảo vệ kết hợp → **bất khả xâm phạm** dù có bao nhiêu Client đấu giá đồng thời.

---

## 8. OOP — Kế thừa & Đa hình

### Cây kế thừa

```
BaseEntity (id, createdAt)
└── User (abstract: username, passwordHash, email)
    ├── Bidder  (balance)          → getRole() = BIDDER
    ├── Seller  (shopName)         → getRole() = SELLER
    └── Admin   (permissions)      → getRole() = ADMIN

BaseEntity
└── Item (abstract: name, description, startingPrice, sellerId)
    ├── Electronics (brand, warrantyMonths, powerWatts) → getCategory() = ELECTRONICS
    ├── Artwork     (artistName, yearCreated, medium)   → getCategory() = ARTWORK
    └── Vehicle     (manufacturer, year, mileage, fuel) → getCategory() = VEHICLE
```

### ❓ Q: `abstract` method `getRole()` và `getCategory()` dùng để làm gì?

> **Đa hình (Polymorphism)**: Code có thể làm việc với `User` mà không cần biết đó là `Bidder` hay `Seller`. Gọi `user.getRole()` → mỗi class con tự trả về giá trị đúng của nó. Không cần `if (user instanceof Seller)` rải rác khắp code.

---

## 9. ĐẶC ĐIỂM KỸ THUẬT DATABASE

### DB_URL — Giải thích từng tham số

```
jdbc:mysql://localhost:3306/auction_db?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC
│    │       │         │   │           │                             │            │
│    │       │         │   Database   Tự tạo DB nếu chưa có         Bỏ SSL       Múi giờ UTC 
│    │       IP       Port
│    Giao thức kết nối MySQL
```

### Single Table Inheritance
Tất cả `Electronics`, `Artwork`, `Vehicle` đều lưu vào **1 bảng `items`** duy nhất. Cột `category` phân biệt loại. Lý do:
- Đơn giản hơn JOIN nhiều bảng
- Query nhanh hơn
- `JdbcItemDao.mapRow()` dùng `switch (category)` để phục dựng đúng class con

---

## 10. KIẾN TRÚC TỔNG THỂ

```
[Client - JavaFX]          [Server - Java]           [Database - MySQL]
LoginController      →     UserService          →     JdbcUserDao  →  users table
SellerDashboard      →     ItemService          →     JdbcItemDao  →  items table
                           AuctionManager (RAM) →     JdbcAuctionDao → auctions table
BiddingRoom          ←[Observer]←  ServerService ←─── JdbcBidDao   →  bid_transactions
```

### ❓ Q: Tại sao tách Client và Server thành 2 module riêng?
> - **Client** (JavaFX) chạy trên máy của từng người dùng — hiển thị UI, gửi lệnh qua mạng.
> - **Server** (Java thuần) chạy trên 1 máy chủ — xử lý logic, truy cập Database.
> - Tách biệt → nhiều Client đồng thời → **khả năng mở rộng (scalability)**.
> - Nếu gộp chung → mỗi người dùng cần cài MySQL → vô lý.

---

## 11. CÂU HỎI THẦY CÓ THỂ HỎi THÊM

**Q: Singleton khác `static class` thế nào?**
> `static class` không kiểm soát được thời điểm khởi tạo và không thể implement interface. Singleton là object thật — có thể inject vào các class khác, có thể mock trong test, có thể implement interface. Singleton linh hoạt hơn nhiều.

**Q: `private static final` khác `private static volatile` thế nào?**
> - `final` → Giá trị **không bao giờ thay đổi** sau khi gán (hằng số).
> - `volatile` → Giá trị **có thể thay đổi**, và mọi luồng luôn đọc từ RAM thật (không dùng cache CPU). Dùng cho biến mà nhiều luồng có thể ghi vào.

**Q: Tại sao `Map.of()` trong TestAuction không thể thêm phần tử sau khi tạo?**
> `Map.of()` trả về **immutable map** (bất biến). Cố gắng `.put()` thêm sau sẽ ném `UnsupportedOperationException`. Đây là cố ý — params của Factory không được phép bị sửa bên ngoài.

**Q: `PasswordUtil.hashPassword()` dùng thuật toán gì?**
> BCrypt — thuật toán hash một chiều có **salt ngẫu nhiên**. Không thể reverse lại password gốc từ hash. Mỗi lần hash cùng 1 password cho kết quả khác nhau (do salt) — chống Rainbow Table attack.

---

*📌 File này được tạo ngày 11/04/2026 — bám sát 100% code thực tế của Nhóm 11.*
