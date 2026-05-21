# 🔍 Kiểm Tra Yêu Cầu Dự Án vs `require.pdf`

> **Ngày kiểm tra:** 2026-05-22  
> **Trạng thái tổng quát:** Dự án đã đạt phần lớn yêu cầu bắt buộc. Có **2 vấn đề cần sửa** và **1 mục cần bổ sung** để đạt điểm tối đa.

---

## 📊 Tổng Kết Nhanh

| Hạng mục | Điểm tối đa | Đánh giá | Ghi chú |
|---|---|---|---|
| Thiết kế lớp & cây kế thừa | 0.5 | ✅ ĐẠT | Đầy đủ BaseEntity → User/Item → subclass |
| Nguyên tắc OOP | 1.0 | ⚠️ CẦN BỔ SUNG | Thiếu `printInfo()` polymorphism minh họa |
| Design Pattern | 1.0 | ✅ ĐẠT | Singleton, Factory, Observer, Strategy đều có |
| Quản lý người dùng, sản phẩm | 1.0 | ✅ ĐẠT | CRUD đầy đủ |
| Chức năng đấu giá | 1.0 | ✅ ĐẠT | Đặt giá, kiểm tra hợp lệ, cập nhật winner |
| Xử lý lỗi & ngoại lệ | 1.0 | ✅ ĐẠT | 4 exception class + validate đầy đủ |
| Concurrent bidding | 1.0 | ✅ ĐẠT | synchronized + ConcurrentHashMap |
| Realtime update | 0.5 | ✅ ĐẠT | Observer + Socket broadcast |
| Client-Server architecture | 0.5 | ✅ ĐẠT | TCP Socket, JSON protocol |
| MVC (JavaFX + FXML) | 0.5 | ✅ ĐẠT | 9 FXML + 9 Controller |
| Maven, coding convention | 0.5 | ✅ ĐẠT | Maven + Spotless Google Java Style |
| Unit Test (JUnit) | 0.5 | ⚠️ CẦN SỬA | 2/3 file test dùng `main()` thay vì `@Test` annotation |
| CI/CD (GitHub Actions) | 0.5 | ✅ ĐẠT | ci.yml có build + test + commitlint |
| **Nâng cao:** Auto-Bidding | 0.5 | ✅ ĐẠT | AutoBidStrategy + PriorityQueue logic |
| **Nâng cao:** Anti-sniping | 0.5 | ✅ ĐẠT | ANTI_SNIPE_WINDOW + EXTENSION |
| **Nâng cao:** Bid History Chart | 0.5 | ✅ ĐẠT | LineChart realtime trong BiddingRoom |

---

## 📋 Chi Tiết Từng Mục

### 1. ✅ Thiết kế lớp và cây kế thừa (0.5đ) — ĐẠT

**Cây kế thừa hiện tại:**

```
BaseEntity (abstract)
├── User (abstract) → getRole() abstract
│   ├── Bidder
│   ├── Seller
│   └── Admin
├── Item (abstract)
│   ├── Electronics
│   ├── Artwork
│   └── Vehicle
├── Auction
├── BidTransaction
├── Wallet
├── WalletTransaction
└── AutoBid
```

**Nhận xét:** Đầy đủ các lớp yêu cầu: User, Bidder/Seller/Admin, Item, Auction, BidTransaction. Cây kế thừa rõ ràng.

---

### 2. ⚠️ Nguyên tắc OOP (1.0đ) — CẦN BỔ SUNG NHỎ

| Nguyên tắc | Trạng thái | Chi tiết |
|---|---|---|
| Encapsulation | ✅ | `private` fields + getter/setter, validate trong setter |
| Inheritance | ✅ | BaseEntity → User/Item → subclass |
| Polymorphism | ⚠️ | Có `getRole()` override nhưng **thiếu `printInfo()`** như yêu cầu gợi ý |
| Abstraction | ✅ | `User` abstract + `getRole()` abstract, `Item` abstract, `BaseEntity` abstract, `BidStrategy` interface |

**Cần bổ sung:**

Đề bài gợi ý override method `printInfo()` để thể hiện Polymorphism rõ ràng hơn. Cần thêm:

**File `Item.java` — thêm abstract method:**
```java
public abstract String printInfo();
```

**File `Electronics.java` — override:**
```java
@Override
public String printInfo() {
    return String.format("[ELECTRONICS] %s | Hãng: %s | BH: %d tháng | Công suất: %.1fW | Giá khởi điểm: %,d VNĐ",
        getName(), brand, warrantyMonths, powerWatts, getStartingPrice());
}
```

**File `Artwork.java` — override:**
```java
@Override
public String printInfo() {
    return String.format("[ARTWORK] %s | Họa sĩ: %s | Năm: %d | Chất liệu: %s | Giá khởi điểm: %,d VNĐ",
        getName(), artistName, yearCreated, medium, getStartingPrice());
}
```

**File `Vehicle.java` — override:**
```java
@Override
public String printInfo() {
    return String.format("[VEHICLE] %s | Hãng: %s | Năm: %d | Km: %,d | Nhiên liệu: %s | Giá khởi điểm: %,d VNĐ",
        getName(), manufacturer, yearManufactured, mileageKm, fuelType, getStartingPrice());
}
```

---

### 3. ✅ Design Pattern (1.0đ) — ĐẠT

| Pattern | Nơi áp dụng | File |
|---|---|---|
| **Singleton** | `AuctionManager`, `DatabaseConfig` | Double-checked locking đúng chuẩn |
| **Factory Method** | `ItemFactory` | `create()`, `createSimpleItem()`, `reconstruct*()` |
| **Observer** | `AuctionObserver` interface, `ClientBroadcaster`, `BiddingRoomController` | Push-based realtime |
| **Strategy** | `BidStrategy` → `ManualBidStrategy`, `AutoBidStrategy` | Xử lý bid khác nhau |

---

### 4. ✅ Chức năng chính (1.0đ) — ĐẠT

- ✅ Đăng ký / Đăng nhập: `AuthHandlers`
- ✅ 3 vai trò: BIDDER, SELLER, ADMIN
- ✅ Thêm/sửa/xóa sản phẩm: `SellerDashboardController`, `CatalogHandlers`
- ✅ Thông tin sản phẩm đầy đủ (tên, mô tả, giá, thời gian)

---

### 5. ✅ Chức năng đấu giá (1.0đ) — ĐẠT

- ✅ Đặt giá cao hơn giá hiện tại
- ✅ Kiểm tra tính hợp lệ (bước giá tối thiểu `minBidStep`)
- ✅ Cập nhật người dẫn đầu (`applyBid()`)

---

### 6. ✅ Kết thúc phiên đấu giá — ĐẠT

- ✅ Tự động đóng phiên: `AuctionStatusSynchronizer.syncWithClock()`
- ✅ Xác định người thắng: `closeAuction()` + `currentWinnerId`
- ✅ Trạng thái vòng đời: OPEN → RUNNING → FINISHED → PAID / CANCELED

---

### 7. ✅ Xử lý lỗi & ngoại lệ (1.0đ) — ĐẠT

| Exception | Trường hợp |
|---|---|
| `InvalidBidException` | Đặt giá thấp hơn giá hiện tại |
| `AuctionClosedException` | Đấu giá khi phiên đã đóng |
| `AuctionException` | Lỗi dữ liệu chung |
| `AuthenticationException` | Lỗi đăng nhập/đăng ký |
| `IllegalArgumentException` | Validate data trong entity |

---

### 8. ✅ Concurrent Bidding (1.0đ) — ĐẠT

- ✅ `synchronized` block trên từng Auction object trong `placeBid()`
- ✅ `ConcurrentHashMap` cho `activeAuctions` và `autoBids`
- ✅ `CopyOnWriteArrayList` cho `connectedClients` trong broadcaster
- ✅ `volatile` cho Singleton instances
- ✅ Kiểm tra TOCTOU: recheck `isRunning()` sau khi lấy lock

---

### 9. ✅ Realtime Update (0.5đ) — ĐẠT

- ✅ Observer Pattern: `AuctionObserver` interface
- ✅ Socket-based broadcast: `ClientBroadcaster.broadcast()`
- ✅ Push không polling: Server broadcast `BID_UPDATE` event
- ✅ Thread-safe notify: `Platform.runLater()` trong Controller

---

### 10. ✅ Kiến trúc Client-Server (0.5đ) — ĐẠT

- ✅ Client-Server qua TCP Socket (port 8888)
- ✅ Giao tiếp bằng JSON
- ✅ Chỉ Server truy cập database

---

### 11. ✅ MVC (0.5đ) — ĐẠT

**Client (JavaFX + FXML):**
- 9 file `.fxml` (View)
- 9 file `*Controller.java` (Controller)
- `AuctionSessionState` (Model)

**Server (Controller-Model-DAO):**
- Handler layer = Controller (`RequestRouter`, `BiddingHandlers`, ...)
- Model layer = Entity + Strategy + Exception
- DAO layer = Interface + JdbcImpl

---

### 12. ✅ Maven + Coding Convention (0.5đ) — ĐẠT

- ✅ Maven build tool (`pom.xml` cho cả server và client)
- ✅ Spotless plugin + Google Java Style format
- ✅ Tên biến/hàm có ý nghĩa, code clean

---

### 13. ⚠️ Unit Test — JUnit (0.5đ) — CẦN SỬA

**Hiện trạng:**

| File | Phương pháp | Vấn đề |
|---|---|---|
| `TestAuction.java` | `main()` | ❌ Không dùng JUnit annotation → `mvn test` **không chạy** được |
| `TestBidTransaction.java` | `main()` | ❌ Không dùng JUnit annotation → `mvn test` **không chạy** được |
| `TestAuctionException.java` | `@Test` | ✅ Đúng chuẩn JUnit 5, nhưng chỉ 2 test case |

**Vấn đề nghiêm trọng:** CI/CD (`mvn test`) sẽ **KHÔNG TÌM THẤY** 2 file dùng `main()`. Cần chuyển sang JUnit 5.

**Cần sửa — xem file bên dưới.**

---

### 14. ✅ CI/CD — GitHub Actions (0.5đ) — ĐẠT

File `.github/workflows/ci.yml` bao gồm:
- ✅ Conventional Commits check (commitlint)
- ✅ Java 21 setup + Maven cache
- ✅ Spotless auto-format + auto-commit
- ✅ Maven build (`mvn clean package`)
- ✅ JUnit test (`mvn test`)

---

### 15. ✅ Auto-Bidding — Nâng cao (0.5đ) — ĐẠT

- ✅ `maxBid` + `increment` trong `AutoBidStrategy`
- ✅ Tự động trả giá khi có bid mới: `resolveAutoBids()`
- ✅ Priority logic: `hasPriorityOver()` (so sánh maxBid, sau đó registerAt)
- ✅ Không vượt maxBid: `Math.min(currentPrice + increment, maxBid)`
- ✅ Xử lý xung đột đồng thời: synchronized trong `resolveAutoBids()`

---

### 16. ✅ Anti-sniping — Nâng cao (0.5đ) — ĐẠT

- ✅ `ANTI_SNIPE_WINDOW_SECONDS = 30` giây cuối
- ✅ `EXTENSION_SECONDS = 30` giây gia hạn
- ✅ Logic trong `AuctionManager.placeBid()`: kiểm tra `secondsLeft <= ANTI_SNIPE_WINDOW`

---

### 17. ✅ Bid History Visualization — Nâng cao (0.5đ) — ĐẠT

- ✅ `LineChart<Number, Number>` trong `BiddingRoomController`
- ✅ Trục X: thứ tự lượt bid
- ✅ Trục Y: giá đấu (VNĐ)
- ✅ Tự động cập nhật qua `onBidUpdated()` → `addBidToChart()`
- ✅ Load history khi vào phòng: `loadBidHistory()`

---

## 🔧 Tóm Tắt Thay Đổi Cần Thực Hiện

| # | Ưu tiên | Mô tả | File cần sửa |
|---|---|---|---|
| 1 | 🔴 CAO | Chuyển `TestAuction.java` sang JUnit 5 `@Test` | `auction-server/src/test/java/TestAuction.java` |
| 2 | 🔴 CAO | Chuyển `TestBidTransaction.java` sang JUnit 5 `@Test` | `auction-server/src/test/java/TestBidTransaction.java` |
| 3 | 🔴 CAO | Thêm `TestBidStrategy.java` (test Strategy Pattern) | `auction-server/src/test/java/TestBidStrategy.java` |
| 4 | 🟡 TRUNG BÌNH | Thêm `abstract printInfo()` trong `Item.java` + override 3 subclass | `server/model/entity/item/*.java` |

**Dự kiến điểm sau khi sửa: 9.5 — 10/10đ (bao gồm 1.5đ nâng cao)**
