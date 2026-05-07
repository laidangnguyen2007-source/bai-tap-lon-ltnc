# feat(seller-dashboard) replace item ID input with category selector and item name field

> **Ngày thay đổi:** 07/05/2026  
> **Người thực hiện:** (Ghi tên thành viên)  
> **Nhánh:** (Ghi tên branch nếu có)

---

## Commit command

```bash
git commit -m "feat(seller-dashboard) replace item ID input with category selector and item name field"
```

---

## Tổng quan thay đổi

### 🎯 Mục tiêu
Trước đây, Seller phải **nhập mã sản phẩm (Item ID)** thủ công khi tạo phiên đấu giá → dễ sai, phải nhớ ID.  
Giờ Seller chỉ cần **chọn loại sản phẩm** từ dropdown và **đặt tên** → hệ thống tự tạo Item với ID không trùng.

### ✅ Tính năng mới
1. **ComboBox chọn loại sản phẩm**: Điện tử / Nghệ thuật / Xe cộ / Khác
2. **TextField đặt tên sản phẩm**: Seller tự đặt tên tùy ý
3. **ID sản phẩm tự sinh**: MySQL `AUTO_INCREMENT`, không bao giờ trùng
4. **Cột "Loại SP" trên bảng**: Hiển thị loại sản phẩm bằng tiếng Việt
5. **Trạng thái tự động**: Thời gian bắt đầu ở quá khứ → RUNNING, ở tương lai → OPEN

### 🐛 Bug đã sửa
- **Tạo phiên thất bại với loại OTHER**: Do client tạo `Auction(0L, ...)` với `itemId=0` giả → gây lỗi. Đã sửa bằng cách truyền raw params trực tiếp, không tạo dummy Auction object.
- **Bỏ gọi `AuctionManager.openAuction()` trên client**: Logic này thuộc server, client không nên can thiệp.

---

## Chi tiết file thay đổi

### 📦 Server (7 file)

| File | Thay đổi |
|------|----------|
| `auction-server/.../model/enums/ItemCategory.java` | Thêm `OTHER` vào enum |
| `auction-server/.../resources/schema.sql` | Cập nhật CHECK constraint: thêm `'OTHER'` |
| `auction-server/.../model/entity/item/ItemFactory.java` | Thêm `OTHER` case + method `createSimpleItem()` public |
| `auction-server/.../handler/AuctionCommandHandlers.java` | `createAuction()` nhận `itemName` + `category`, tự tạo Item |
| `auction-server/.../dao/impl/JdbcItemDao.java` | `mapRow()` xử lý category `OTHER` |
| `auction-server/.../net/EntityJsonMapper.java` | Thêm `itemCategory` vào JSON response |
| `auction-server/.../model/entity/Auction.java` | Thêm field `itemCategory` (getter/setter) |

### 💻 Client (5 file)

| File | Thay đổi |
|------|----------|
| `auction-client/.../fxml/seller-dashboard.fxml` | Thay `itemIdField` → `categoryCombo` + `itemNameField`, thêm cột `categoryCol` |
| `auction-client/.../controller/SellerDashboardController.java` | Logic form mới, truyền raw params, cấu hình cột category |
| `auction-client/.../network/AuctionController.java` | `createAuction()` nhận raw params thay vì Auction object |
| `auction-client/.../service/ServerService.java` | Cập nhật signature `createAuction()` tương ứng |
| `auction-client/.../util/JsonEntityMapper.java` | Parse `itemCategory` + xử lý `OTHER` trong `mapToItem()` |

---

## ⚠️ Lưu ý quan trọng cho team

### Cập nhật Database (BẮT BUỘC nếu DB đã tồn tại)
Nếu database `auction_db` đã được tạo từ trước, cần chạy lệnh SQL sau để thêm `'OTHER'` vào CHECK constraint:

```sql
-- Chạy trên MySQL Workbench hoặc terminal
USE auction_db;
ALTER TABLE items DROP CHECK chk_category;
ALTER TABLE items ADD CONSTRAINT chk_category 
    CHECK (category IN ('ELECTRONICS', 'ARTWORK', 'VEHICLE', 'OTHER'));
```

> Nếu tạo DB mới từ `schema.sql` thì **không cần** chạy lệnh trên.

### API thay đổi (Breaking Change)
Request `CREATE_AUCTION` giờ **không còn gửi `itemId`**, thay bằng:
```json
{
  "action": "CREATE_AUCTION",
  "itemName": "Tên sản phẩm",
  "category": "ELECTRONICS | ARTWORK | VEHICLE | OTHER",
  "sellerId": 3,
  "startingPrice": 500000,
  "startTime": "2026-05-07T10:00:00",
  "endTime": "2026-05-07T22:00:00"
}
```

### Build & Test
```bash
# Build server
cd auction-server && mvn compile

# Build client
cd auction-client && mvn compile
```

---

## Liên hệ
Nếu có thắc mắc về các thay đổi này, liên hệ người thực hiện commit.
