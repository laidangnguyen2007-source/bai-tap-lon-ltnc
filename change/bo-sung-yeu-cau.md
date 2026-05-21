# 📝 Danh Sách Thay Đổi Đã Thực Hiện

> **Ngày thực hiện:** 2026-05-22  
> **Kết quả test:** ✅ 30/30 PASS (0 failures)

---

## Tổng Quan

Sau khi kiểm tra đối chiếu dự án với file `require.pdf`, đã phát hiện và sửa **3 vấn đề**:

| # | Vấn đề | Mức độ | Trạng thái |
|---|---|---|---|
| 1 | `TestAuction.java` dùng `main()` thay vì JUnit `@Test` | 🔴 Nghiêm trọng | ✅ Đã sửa |
| 2 | `TestBidTransaction.java` dùng `main()` thay vì JUnit `@Test` | 🔴 Nghiêm trọng | ✅ Đã sửa |
| 3 | Thiếu `printInfo()` polymorphism trong Item hierarchy | 🟡 Trung bình | ✅ Đã bổ sung |
| 4 | Thiếu test cho Strategy Pattern (logic quan trọng nhất) | 🟡 Trung bình | ✅ Đã tạo mới |

---

## Chi Tiết Từng Thay Đổi

### 1. ✅ Sửa `TestAuction.java` — Chuyển sang JUnit 5

**File:** `auction-server/src/test/java/TestAuction.java`

**Trước:** Dùng `public static void main()` → Maven Surefire **không chạy được** khi chạy `mvn test`.

**Sau:** Chuyển sang JUnit 5 với annotation `@Test` và `@DisplayName`. Thêm 9 test cases:
- `testFactoryCreateElectronics()` — Factory Pattern tạo Electronics
- `testFactoryCreateArtwork()` — Factory Pattern tạo Artwork  
- `testFactoryCreateVehicle()` — Factory Pattern tạo Vehicle
- `testAuctionInitStatus()` — Auction khởi tạo OPEN
- `testApplyBid()` — applyBid cập nhật giá
- `testApplyBidRejectLowerPrice()` — Từ chối giá thấp
- `testApplyBidRejectNotRunning()` — Từ chối khi không RUNNING
- `testExtendEndTime()` — Anti-sniping gia hạn thời gian
- `testAuctionRejectInvalidTime()` — Từ chối endTime trước startTime

---

### 2. ✅ Sửa `TestBidTransaction.java` — Chuyển sang JUnit 5

**File:** `auction-server/src/test/java/TestBidTransaction.java`

**Trước:** Dùng `public static void main()` → Maven Surefire **không chạy được**.

**Sau:** Chuyển sang JUnit 5 với 6 test cases:
- `testBidTransactionCreation()` — Tạo thành công
- `testBidTransactionRejectZeroAmount()` — Từ chối amount = 0
- `testBidTransactionRejectNegativeAmount()` — Từ chối amount âm
- `testBidTransactionRejectNullAuctionId()` — Từ chối null auctionId
- `testBidTransactionRejectNullBidderId()` — Từ chối null bidderId
- `testBidTransactionToString()` — Kiểm tra toString()

---

### 3. ✅ Tạo mới `TestBidStrategy.java` — Test Strategy Pattern

**File:** `auction-server/src/test/java/TestBidStrategy.java`

**Lý do:** Đề bài yêu cầu "Unit Test (JUnit) cho logic quan trọng". Strategy Pattern là logic cốt lõi nhất của hệ thống đấu giá nhưng chưa có test.

**Nội dung:** 13 test cases cho cả ManualBidStrategy và AutoBidStrategy:

**ManualBidStrategy (5 tests):**
- Chấp nhận bid hợp lệ
- Từ chối bid thấp hơn giá hiện tại
- Từ chối bid không đạt bước giá tối thiểu
- Từ chối khi phiên đã đóng
- Kiểm tra getStrategyName()

**AutoBidStrategy (8 tests):**
- Tính toán bid tiếp theo đúng (`currentPrice + increment`)
- Không vượt maxBid (`Math.min`)
- Không đặt giá khi đã dẫn đầu (return null)
- Từ chối khi giá hiện tại >= maxBid
- Priority: maxBid cao hơn thắng
- Priority: cùng maxBid → ưu tiên đăng ký trước
- Kiểm tra getStrategyName()
- Từ chối increment <= 0

---

### 4. ✅ Bổ sung `printInfo()` — OOP Polymorphism

**Files đã sửa:**
- `server/model/entity/item/Item.java` — Thêm `public abstract String printInfo()`
- `server/model/entity/item/Electronics.java` — Override `printInfo()`
- `server/model/entity/item/Artwork.java` — Override `printInfo()`
- `server/model/entity/item/Vehicle.java` — Override `printInfo()`

**Lý do:** Đề bài mục 3.3.2 yêu cầu: _"Polymorphism: override phương thức (ví dụ: printInfo())"_. 
Trước đó chỉ có `getRole()` trong User hierarchy thể hiện polymorphism. Giờ Item hierarchy cũng có `printInfo()` — mỗi subclass trả về chuỗi mô tả khác nhau theo đặc trưng riêng.

**Output ví dụ:**
```
[ELECTRONICS] Laptop Dell | Hãng: Dell | BH: 12 tháng | Công suất: 135.1W | Giá khởi điểm: 1,000 VNĐ
[ARTWORK] Mona Lisa | Họa sĩ: Leonardo da Vinci | Năm: 1503 | Chất liệu: Sơn dầu | Giá khởi điểm: 5,000 VNĐ
[VEHICLE] Toyota Camry | Hãng: Toyota | Năm: 2023 | Km: 15,000 | Nhiên liệu: Xăng | Giá khởi điểm: 800,000,000 VNĐ
```

---

## Kết Quả Chạy Test

```
[INFO] Running TestAuction
[INFO] Tests run: 9, Failures: 0, Errors: 0, Skipped: 0

[INFO] Running TestAuctionException
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0

[INFO] Running TestBidStrategy
[INFO] Tests run: 13, Failures: 0, Errors: 0, Skipped: 0

[INFO] Running TestBidTransaction
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0

[INFO] Tests run: 30, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```
