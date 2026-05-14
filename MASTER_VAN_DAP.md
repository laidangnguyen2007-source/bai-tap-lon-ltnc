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
> **SHA-256** — thuật toán hash một chiều thuộc họ SHA-2. Password được chuyển thành mảng byte (`getBytes(UTF_8)`), rồi `MessageDigest.getInstance("SHA-256")` tính ra 32 byte hash, cuối cùng `bytesToHex()` chuyển sang chuỗi hex 64 ký tự. Khi đăng nhập, server hash password nhập vào rồi so sánh với hash đã lưu trong DB — **không bao giờ lưu mật khẩu gốc**.

---

---

## 24. JSON MAPPING & DATA ENRICHMENT — `JsonMapper` & `EntityJsonMapper`

### Mục đích
Xử lý việc chuyển đổi dữ liệu giữa đối tượng Java (Entity) và định dạng truyền tin (JSON). 
- Phía Server: **Object -> JSON** (Để gửi đi).
- Phía Client: **JSON -> Object** (Để hiển thị lên GUI).

### Tại sao Server Mapper (`EntityJsonMapper`) cần `ItemDao`?
Đây là kỹ thuật **Data Enrichment (Làm giàu dữ liệu)**. 
- Đối tượng `Auction` trong Database chỉ có `itemId` (con số). 
- Client cần hiển thị **Tên sản phẩm** (chuỗi chữ) để người dùng dễ đọc.
- **Giải pháp:** Trong lúc chuyển đổi sang JSON, Mapper dùng `ItemDao` để "tra từ điển" lấy tên sản phẩm dựa trên ID và nhét vào JSON.

### ❓ Câu hỏi thầy hay hỏi

**Q: Tại sao không để logic chuyển đổi JSON ngay trong lớp Server hay Controller?**
> Để tuân thủ **SRP (Single Responsibility Principle)**. Lớp Mapper chỉ lo việc "biến hình" dữ liệu. Nếu Server lo cả việc này, code sẽ rất dài và khó kiểm soát. Khi cần đổi tên một trường JSON (ví dụ đổi `id` thành `auction_id`), ta chỉ cần sửa đúng 1 nơi trong Mapper.

**Q: Tại sao phải có 2 lớp Mapper riêng ở 2 phía Client và Server?**
> Vì hai đầu có nhiệm vụ khác nhau:
> - Server cần **Serialize** (đóng gói): Biến Object thành chuỗi JSON để đẩy vào luồng Socket.
> - Client cần **Deserialize** (giải nén): Đọc chuỗi JSON từ Socket và biến lại thành Object Java để đưa vào `TableView` hay `ListView`.

**Q: Nếu một hàm trong Mapper không dùng tới `ItemDao`, tại sao vẫn phải tiêm (inject) nó vào Constructor?**
> Đây là quy tắc của **Dependency Injection**. Một class được cấp sẵn các "công cụ" (DAO) ngay từ đầu để sẵn sàng phục vụ cho bất kỳ phương thức nào bên trong nó. Điều này giúp code gọn gàng và dễ Unit Test hơn (có thể dùng Mock DAO).

---

*📌 File này được cập nhật ngày 06/05/2026 — Nhóm 11 (Chốt kiến thức Mapping).*

---

## 12. TCP SOCKET & GIAO THỨC MẠNG — `SocketConnection.java` + `Server.java`

### Mục đích
Client và Server giao tiếp bằng **TCP Socket** qua cổng 8888. Mỗi message là 1 dòng JSON (text-based protocol). Hệ thống có 2 loại tin nhắn:
- **Request-Response** (đồng bộ): Client gửi → chờ Server trả lời (ví dụ: `LOGIN`, `GET_ALL_AUCTIONS`)
- **Push Notification** (bất đồng bộ): Server tự gửi xuống khi có sự kiện (ví dụ: `BID_UPDATE`)

### Cấu trúc

```java
// SocketConnection.java — Singleton, mỗi Client chỉ mở 1 kết nối duy nhất
private Socket socket;
private PrintWriter out;          // Gửi dữ liệu lên Server
private BufferedReader in;        // Nhận dữ liệu từ Server

// Hàng đợi chặn — chờ response đồng bộ từ Server
private final BlockingQueue<String> responseQueue = new LinkedBlockingQueue<>();

// Danh sách callback nhận push message (Observer Pattern ở tầng Network)
private final CopyOnWriteArrayList<Consumer<String>> pushListeners = new CopyOnWriteArrayList<>();
```

### Phân loại tin nhắn bằng nội dung

```java
// GlobalNetworkListener — 1 thread duy nhất đọc từ socket
if (msg.contains("\"type\":")) {
    // Push message → chuyển tới tất cả push listeners
    for (Consumer<String> listener : pushListeners) listener.accept(msg);
} else {
    // Response đồng bộ → đưa vào hàng đợi để sendRequest() lấy ra
    responseQueue.put(msg);
}
```

### Xử lý "dính dòng" bằng Regex Lookahead

```java
// Khi 2 JSON object bị dính sát: {"type":"BID"}{"status":"OK"}
String[] messages = line.split("(?<=\\})(?=\\{)");
// (?<=\}) = Lookbehind: đứng SAU dấu }
// (?=\{)  = Lookahead:  đứng TRƯỚC dấu {
// Kết quả: tách thành ["{"type":"BID"}", "{"status":"OK"}"]
```

### ❓ Câu hỏi thầy hay hỏi

**Q: Tại sao dùng `PrintWriter(out, true)` với tham số `true`?**
> Tham số `true` bật chế độ **auto-flush** — mỗi lần gọi `println()`, dữ liệu được gửi ngay lập tức qua mạng. Nếu không có auto-flush, dữ liệu có thể nằm trong buffer mà chưa gửi đi → Client đợi mãi không nhận được phản hồi.

**Q: Tại sao listener thread dùng `setDaemon(true)`?**
> **Daemon thread** = thread "phục vụ". Khi tất cả non-daemon thread (ví dụ: JavaFX Application Thread) kết thúc, JVM sẽ tự động tắt luôn daemon thread. Nếu không đặt daemon, ứng dụng JavaFX đóng xong mà listener thread vẫn chạy → chương trình không tắt hẳn được.

**Q: `responseQueue.poll(10, TimeUnit.SECONDS)` hoạt động thế nào?**
> `poll(timeout)` là phương thức của `BlockingQueue` — nó **chờ tối đa 10 giây** để lấy phần tử ra. Nếu Server không trả lời trong 10 giây → trả về `null` → Client biết là bị timeout. Khác với `take()` sẽ chờ vô hạn gây treo ứng dụng.

---

## 13. BROADCAST PATTERN — `Server.broadcast()`

### Mục đích
Khi Bidder A đặt giá thành công, **tất cả Client đang kết nối** (kể cả Bidder B, C, D ở các phiên khác) đều nhận được tin nhắn. Đây là cơ chế **Push-based Realtime**.

### Cấu trúc

```java
// Server.java
private static final List<PrintWriter> connectedClients = new CopyOnWriteArrayList<>();

// Khi client mới kết nối
connectedClients.add(out);

// Khi client ngắt kết nối (trong finally block)
connectedClients.remove(out);

// Phát tin tới TẤT CẢ client
private static void broadcast(String message) {
    for (PrintWriter client : connectedClients) {
        client.println(message);
        client.flush(); // Ép gửi ngay lập tức
    }
}
```

### ❓ Câu hỏi thầy hay hỏi

**Q: Tại sao `connectedClients` dùng `CopyOnWriteArrayList` thay vì `ArrayList`?**
> `ArrayList` không thread-safe. Khi 1 thread đang duyệt danh sách (`for` loop trong `broadcast()`), thread khác gọi `add()` hoặc `remove()` → `ConcurrentModificationException`. `CopyOnWriteArrayList` mỗi lần thêm/xóa sẽ **tạo bản sao** mới của mảng bên dưới → thread đang duyệt không bị ảnh hưởng. Phù hợp khi đọc rất nhiều mà ghi ít (broadcast liên tục, thêm/xóa client hiếm).

**Q: Tại sao xóa writer trong `finally` block?**
> `finally` **luôn chạy** dù client ngắt bình thường hay bị crash. Nếu không xóa, `broadcast()` cố gửi vào writer đã chết → lỗi, và writer tồn tại mãi trong danh sách → **memory leak**.

---

## 14. DISPATCHER PATTERN & SWITCH EXPRESSION — `Server.dispatch()`

### Mục đích
Server nhận JSON request, đọc trường `"action"` rồi chuyển tới handler phù hợp. Đây là **Command Dispatcher** — một điểm phân phối trung tâm.

### Cấu trúc (Java 14+ Switch Expression)

```java
return switch (action) {
    case "LOGIN"              -> handleLogin(req);
    case "REGISTER"           -> handleRegister(req);
    case "PLACE_BID"          -> handlePlaceBid(req);
    case "GET_ALL_AUCTIONS"   -> handleGetAllAuctions(req);
    case "ADMIN_UPDATE_AUCTION" -> handleAdminUpdateAuction(req);
    default -> errorResponse("Invalid action: " + action);
};
```

### ❓ Câu hỏi thầy hay hỏi

**Q: Switch Expression (`->`) khác gì Switch Statement (`case: break;`) truyền thống?**
> - Switch Expression **trả về giá trị** trực tiếp (dùng được trong `return`).
> - Không cần `break;` — mỗi nhánh tự động thoát, **không có fall-through** (lỗi kinh điển khi quên `break` trong switch cũ).
> - Compiler bắt buộc phải xử lý **tất cả trường hợp** (exhaustive) hoặc có `default`.

---

## 15. THREAD POOL & SHUTDOWN HOOK — `Server.java`

### ExecutorService — Quản lý luồng thông minh

```java
// Server.java — dùng CachedThreadPool thay vì tạo Thread thủ công
ExecutorService threadPool = Executors.newCachedThreadPool();
threadPool.submit(() -> handleClient(clientSocket));
```

### ❓ Q: Tại sao dùng `newCachedThreadPool()` thay vì `new Thread()` cho mỗi Client?

> - `new Thread()` mỗi client → có 1000 client = tạo 1000 thread → hệ thống kiệt sức (`OutOfMemoryError`).
> - `CachedThreadPool` **tái sử dụng** thread đã xong việc. Thread rảnh 60 giây sẽ bị thu hồi. Số thread co giãn tự động theo tải — không lãng phí tài nguyên.

### Shutdown Hook — Dọn dẹp khi tắt Server

```java
Runtime.getRuntime().addShutdownHook(new Thread(() -> {
    DatabaseConfig.getInstance().close(); // Đóng kết nối MySQL
}));
```

### ❓ Q: Shutdown Hook là gì?

> Là đoạn code **chạy tự động khi JVM tắt** (bấm Ctrl+C, đóng Terminal, hoặc `System.exit()`). Dùng để đóng kết nối DB, lưu trạng thái, giải phóng tài nguyên. Nếu không đóng Connection → MySQL vẫn giữ kết nối cũ → đến lúc đầy bảng `max_connections`.

---

## 16. SINGLETON PHÍA CLIENT — `AuctionSessionState.java`

### Mục đích
Lưu trạng thái phiên đăng nhập **chung cho toàn bộ ứng dụng Client**. Mọi Controller đều truy cập cùng một instance để biết "ai đang đăng nhập" và "đang xem phiên nào".

```java
public class AuctionSessionState {
    private static volatile AuctionSessionState instance;
    private User currentUser;       // Ai đang đăng nhập (null = chưa login)
    private Auction selectedAuction; // Đang xem phiên nào (null = chưa chọn)

    // LoginController ghi: setCurrentUser(user)
    // AuctionListController đọc: getCurrentUser().getUsername()
    // BiddingRoomController đọc: getSelectedAuction()
}
```

### ❓ Q: Tại sao không truyền User qua constructor của mỗi Controller?

> JavaFX tự tạo Controller khi load FXML (`FXMLLoader.load()`) — ta **không kiểm soát** được constructor. Singleton Session là cách duy nhất chia sẻ dữ liệu giữa các màn hình mà không phá vỡ quy trình FXML.

---

## 17. JAVAFX APPLICATION LIFECYCLE — `Launcher.java` + `MainApp.java`

### Vòng đời JavaFX

```
main() → launch() → init() → start(primaryStage) → [ứng dụng chạy] → stop()
```

### Tại sao cần `Launcher.java` riêng biệt?

```java
// Launcher.java — KHÔNG kế thừa Application
public class Launcher {
    public static void main(String[] args) {
        MainApp.main(args); // Gọi gián tiếp
    }
}
```

### ❓ Q: Tại sao không chạy `MainApp.main()` trực tiếp?

> Từ Java 11+, nếu class `main` kế thừa `Application` mà **không khai báo module JavaFX** đúng cách, JVM báo: *"JavaFX runtime components are missing"*. `Launcher` không kế thừa `Application` nên JVM load bình thường, sau đó mới kéo JavaFX runtime vào. Đây là **workaround chuẩn** được khuyến nghị bởi cộng đồng JavaFX.

---

## 18. FXML LOADER & DRY PRINCIPLE — `FxmlLoader.java`

### Mục đích
Đóng gói toàn bộ logic load FXML + CSS vào **một điểm duy nhất**. Tất cả Controller chỉ cần gọi 1 dòng.

```java
// Utility class — private constructor + UnsupportedOperationException
public final class FxmlLoader {
    private FxmlLoader() {
        throw new UnsupportedOperationException("Utility class — không khởi tạo");
    }

    public static void navigateTo(Stage stage, String fxmlFileName, String title) {
        FXMLLoader loader = new FXMLLoader(FxmlLoader.class.getResource(fullPath));
        Parent root = loader.load();
        Scene scene = new Scene(root);
        scene.getStylesheets().add(cssPath); // Inject CSS chung cho mọi màn hình
        stage.setScene(scene);
        stage.show();
    }
}
```

### ❓ Q: Utility Class là gì? Tại sao constructor `throw UnsupportedOperationException`?

> Utility class chỉ chứa **static method**, không bao giờ tạo instance. `private` constructor ngăn `new FxmlLoader()`, và `throw` bên trong ngăn luôn trường hợp dùng Reflection để bypass. Đây là best practice từ **Effective Java (Joshua Bloch)**.

### ❓ Q: `getResource()` load file FXML từ đâu?

> `getResource()` tìm file trong **classpath** (thư mục `resources/` sau khi Maven build). Không phải đường dẫn tuyệt đối trên ổ cứng. Nhờ đó, khi đóng gói JAR, file FXML vẫn được tìm thấy bình thường.

---

## 19. OBSERVABLE LIST & DATA BINDING — JavaFX TableView

### Mục đích
`ObservableList` là danh sách đặc biệt của JavaFX — khi dữ liệu thay đổi, **UI tự cập nhật** mà không cần gọi `refresh()` thủ công.

```java
// AuctionListController.java
private final ObservableList<Auction> auctionData = FXCollections.observableArrayList();

// Thiết lập binding 1 lần duy nhất
auctionTable.setItems(auctionData);

// Sau này: thêm/xóa phần tử → bảng tự vẽ lại
auctionData.setAll(newList); // Thay toàn bộ dữ liệu → bảng tự render
```

### CellValueFactory — Liên kết cột với thuộc tính

```java
// Cách 1: PropertyValueFactory (dùng Reflection, đơn giản)
idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));

// Cách 2: Lambda (linh hoạt hơn, có thể format)
currentPriceColumn.setCellValueFactory(cellData ->
    new SimpleStringProperty(String.format("%,d VNĐ", cellData.getValue().getCurrentPrice())));
```

### RowFactory — Double-Click để xem chi tiết

```java
auctionTable.setRowFactory(tv -> {
    TableRow<Auction> row = new TableRow<>();
    row.setOnMouseClicked(event -> {
        if (event.getClickCount() == 2 && (!row.isEmpty())) {
            navigateToDetail(row.getItem());
        }
    });
    return row;
});
```

### ❓ Q: `ObservableList` khác `ArrayList` thế nào?

> `ObservableList` kế thừa `List` nhưng thêm cơ chế **listener** (Observer Pattern ẩn bên trong). Khi bạn gọi `add()`, `remove()`, `setAll()`, nó tự phát sự kiện cho TableView biết → TableView vẽ lại. `ArrayList` không có cơ chế này → phải gọi `refresh()` thủ công.

---

## 20. STREAM API — `MyWinsController.java`

### Mục đích
Lọc danh sách đấu giá để tìm các phiên mà người dùng hiện tại đã **thắng cuộc**.

```java
List<Auction> won = all.stream()
    .filter(a -> currentUserId.equals(a.getCurrentWinnerId()))  // Mình là người thắng
    .filter(a -> a.getStatus() == AuctionStatus.FINISHED        // Phiên đã kết thúc
             || a.getStatus() == AuctionStatus.PAID)             // Hoặc đã thanh toán
    .collect(Collectors.toList());
```

### ❓ Q: Stream API khác vòng `for` truyền thống thế nào?

> - **Khai báo (declarative)**: nói "cái gì" thay vì "làm thế nào" → code ngắn gọn, dễ đọc.
> - **Lazy evaluation**: các operation trung gian (`filter`, `map`) chỉ chạy khi gặp terminal operation (`collect`) → tối ưu khi xử lý dữ liệu lớn.
> - **Có thể chuyển sang `.parallelStream()`** để xử lý song song trên multi-core CPU.

---

## 21. TOAST NOTIFICATION & ANIMATION — `NotificationUtils.java`

### Mục đích
Hiển thị thông báo nhỏ tự biến mất (Toast) giống Android/iOS — người dùng biết sự kiện mà không bị gián đoạn.

```java
// Chuỗi hiệu ứng: FadeIn → Chờ 3s → FadeOut → Đóng Popup
FadeTransition fadeIn = new FadeTransition(Duration.millis(300), container);
PauseTransition stay = new PauseTransition(Duration.seconds(3));
FadeTransition fadeOut = new FadeTransition(Duration.millis(500), container);
fadeOut.setOnFinished(e -> popup.hide());
new SequentialTransition(fadeIn, stay, fadeOut).play();
```

### ❓ Q: `SequentialTransition` là gì?

> Là lớp JavaFX chạy nhiều Animation **nối tiếp nhau** theo thứ tự. Ở đây: hiện lên mờ dần (300ms) → giữ yên (3s) → mờ dần biến mất (500ms). Tất cả chỉ cần 1 lệnh `.play()`. Nếu viết thủ công bằng `Thread.sleep()` trên UI thread → **giao diện đơ cứng**.

---

## 22. IMMUTABLE OBJECT — `BidTransaction`

### Mục đích
Lịch sử đặt giá là **bất biến** — một khi ghi vào DB thì không bao giờ được sửa (audit trail).

```java
// JdbcBidTransactionDao.java
@Override
public BidTransaction update(BidTransaction bid) {
    throw new UnsupportedOperationException("BidTransaction is immutable and cannot be updated.");
}
```

### `Collections.unmodifiableMap()` — Bảo vệ dữ liệu nội bộ

```java
// AuctionManager.java — trả về bản "chỉ đọc" của map nội bộ
public Map<Long, Auction> getAllActive() {
    return Collections.unmodifiableMap(activeAuctions);
}
// Gọi .put() trên map này → UnsupportedOperationException
```

### ❓ Q: Tại sao BidTransaction không cho update?

> Trong hệ thống tài chính/đấu giá, lịch sử giao dịch phải là **bất biến (Immutable)** để đảm bảo tính minh bạch. Nếu cho phép sửa → ai đó có thể thay đổi số tiền đã đặt → gian lận. Đây là nguyên tắc **Append-Only Log** trong thiết kế hệ thống.

---

## 23. DATABASE AUTO-INIT — `DatabaseConfig.initializeSchema()`

### Mục đích
Khi Server khởi động lần đầu, tự động tạo Database + tất cả bảng + dữ liệu mẫu mà **không cần thao tác thủ công**.

```java
private void initializeSchema() throws SQLException {
    // Đọc file schema.sql từ resources/
    InputStream is = getClass().getClassLoader().getResourceAsStream("schema.sql");
    String sql = new String(is.readAllBytes(), StandardCharsets.UTF_8);

    // Tách từng câu SQL bằng dấu ; và chạy tuần tự
    for (String statement : sql.split(";")) {
        String trimmed = statement.trim();
        if (!trimmed.isEmpty() && !trimmed.startsWith("--")) {
            stmt.execute(trimmed);
        }
    }
}
```

### ❓ Q: Tại sao dùng `CREATE TABLE IF NOT EXISTS` thay vì `CREATE TABLE`?

> Lần chạy đầu tiên: bảng chưa có → tạo mới. Lần chạy thứ hai trở đi: bảng đã tồn tại → bỏ qua. Nếu dùng `CREATE TABLE` (không có `IF NOT EXISTS`) → lần thứ hai sẽ báo lỗi *"Table already exists"* → Server crash.

### ❓ Q: `INSERT IGNORE` trong seed data dùng để làm gì?

> `INSERT IGNORE` sẽ **bỏ qua** nếu dữ liệu đã tồn tại (ví dụ: trùng `UNIQUE KEY` username). Nhờ đó, mỗi lần restart Server đều chạy lại schema.sql mà không bị lỗi duplicate. Dữ liệu mẫu chỉ được chèn đúng **1 lần duy nhất**.