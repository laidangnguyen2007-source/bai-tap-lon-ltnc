package com.auction.client.util;

import com.auction.server.model.entity.Auction;
import com.auction.server.model.entity.BidTransaction;
import com.auction.server.model.entity.item.Item;
import com.auction.server.model.entity.item.ItemFactory;
import com.auction.server.model.entity.user.Admin;
import com.auction.server.model.entity.user.Bidder;
import com.auction.server.model.entity.user.Seller;
import com.auction.server.model.entity.user.User;
import com.auction.server.model.enums.AuctionStatus;
import com.auction.server.model.enums.ItemCategory;
import java.time.LocalDateTime;
import org.json.simple.JSONObject;

/**
 * Utility class chuyên trách việc chuyển đổi JSON sang Java Object (Deserialize).
 * Tuân thủ SRP: Các lớp Network chỉ lo gửi/nhận, lớp này lo parse dữ liệu.
 *
 * <p>Tất cả phương thức là static — không cần khởi tạo đối tượng.
 * Null-safety: tất cả trường số đều ép qua (Number) thay vì (Long) để tránh
 * ClassCastException khi server trả về Integer hay BigDecimal.
 */
public final class JsonMapper {
    private JsonMapper() { throw new UnsupportedOperationException(); }

    // ── JSON → User ──────────────────────────────────────────────────

    public static User mapToUser(JSONObject json) {
        String role = (String) json.get("role");
        Long id = ((Number) json.get("id")).longValue();
        String username = (String) json.get("username");
        String email = (String) json.get("email");
        return switch (role) {
            case "BIDDER" -> {
                long balance = json.get("balance") != null
                    ? ((Number) json.get("balance")).longValue() : 0L;
                Bidder b = new Bidder(username, "", email, balance);
                b.setId(id);
                yield b;
            }
            case "SELLER" -> {
                Seller s = new Seller(username, "", email, (String) json.get("shopName"));
                s.setId(id);
                yield s;
            }
            case "ADMIN" -> {
                Admin a = new Admin(username, "", email, 1);
                a.setId(id);
                yield a;
            }
            default -> throw new IllegalArgumentException("Unknown role: " + role);
        };
    }

    // ── JSON → Auction ───────────────────────────────────────────────

    public static Auction mapToAuction(JSONObject json) {
        // Dùng toString() + parse để xử lý cả Integer lẫn Long từ server
        Long id = json.get("id") != null
            ? Long.parseLong(json.get("id").toString()) : null;
        LocalDateTime createdAt = LocalDateTime.parse(json.get("createdAt").toString());
        Long itemId = json.get("itemId") != null
            ? Long.parseLong(json.get("itemId").toString()) : null;
        Long sellerId = json.get("sellerId") != null
            ? Long.parseLong(json.get("sellerId").toString()) : null;
        long currentPrice = json.get("currentPrice") != null
            ? Long.parseLong(json.get("currentPrice").toString()) : 0L;
        Long currentWinnerId = json.get("currentWinnerId") != null
            ? Long.parseLong(json.get("currentWinnerId").toString()) : null;
        AuctionStatus status = AuctionStatus.valueOf(json.get("status").toString());
        LocalDateTime startTime = LocalDateTime.parse(json.get("startTime").toString());
        LocalDateTime endTime   = LocalDateTime.parse(json.get("endTime").toString());
        long minBidStep = json.get("minBidStep") != null
            ? Long.parseLong(json.get("minBidStep").toString()) : 0L;

        Auction a = new Auction(id, createdAt, itemId, sellerId,
            currentPrice, currentWinnerId, status, startTime, endTime, minBidStep);

        // Tên + loại sản phẩm do server enrichment đính kèm
        a.setItemName(json.get("itemName") != null
            ? json.get("itemName").toString() : "Sản phẩm #" + itemId);
        a.setItemCategory(json.get("itemCategory") != null
            ? json.get("itemCategory").toString() : "OTHER");
        a.setItemDescription(json.get("itemDescription") != null
            ? json.get("itemDescription").toString() : "");
        a.setImageBase64(json.get("imageBase64") != null
            ? json.get("imageBase64").toString() : null);

        // Nhận giá khởi điểm và gán cho auction hiện tại để trả về
        long startingPrice = json.get("startingPrice") != null
                ? Long.parseLong(json.get("startingPrice").toString()) : 0L;
        a.setStartingPrice(startingPrice);

        return a;
    }

    // ── JSON → BidTransaction ────────────────────────────────────────

    public static BidTransaction mapToBid(JSONObject json) {
        Long id = json.get("id") != null
            ? ((Number) json.get("id")).longValue() : -1L;

        LocalDateTime createdAt;
        try {
            createdAt = LocalDateTime.parse((String) json.get("createdAt"));
        } catch (Exception e) {
            createdAt = LocalDateTime.now(); // Fallback nếu format có vấn đề
        }

        Long auctionId = json.get("auctionId") != null
            ? ((Number) json.get("auctionId")).longValue() : -1L;
        Long bidderId = json.get("bidderId") != null
            ? ((Number) json.get("bidderId")).longValue() : -1L;
        long amount = json.get("amount") != null
            ? ((Number) json.get("amount")).longValue() : 0L;

        LocalDateTime timestamp;
        try {
            timestamp = LocalDateTime.parse((String) json.get("timestamp"));
        } catch (Exception e) {
            timestamp = LocalDateTime.now(); // Fallback
        }

        return new BidTransaction(id, createdAt, auctionId, bidderId, amount, timestamp);
    }

    // ── JSON → Item ──────────────────────────────────────────────────

    public static Item mapToItem(JSONObject json) {
        ItemCategory cat = ItemCategory.valueOf((String) json.get("category"));
        Long id = ((Number) json.get("id")).longValue();
        LocalDateTime createdAt = LocalDateTime.parse((String) json.get("createdAt"));
        String name = (String) json.get("name");
        String desc = json.get("description") != null
            ? (String) json.get("description") : "Đang cập nhật...";
        long price  = ((Number) json.get("startingPrice")).longValue();
        Long seller = ((Number) json.get("sellerId")).longValue();

        Item result = switch (cat) {
            case ELECTRONICS -> ItemFactory.reconstructElectronics(
                id, createdAt, name, desc, price, seller,
                (String) json.get("brand"),
                json.get("warrantyMonths") != null ? ((Number) json.get("warrantyMonths")).intValue() : 0,
                json.get("powerWatts")    != null ? ((Number) json.get("powerWatts")).doubleValue()    : 0.0);
            case ARTWORK -> ItemFactory.reconstructArtwork(
                id, createdAt, name, desc, price, seller,
                (String) json.get("artistName"),
                json.get("yearCreated") != null ? ((Number) json.get("yearCreated")).intValue() : 0,
                (String) json.get("medium"));
            case VEHICLE -> ItemFactory.reconstructVehicle(
                id, createdAt, name, desc, price, seller,
                (String) json.get("manufacturer"),
                json.get("yearManufactured") != null ? ((Number) json.get("yearManufactured")).intValue() : 0,
                json.get("mileageKm")        != null ? ((Number) json.get("mileageKm")).intValue()        : 0,
                (String) json.get("fuelType"));
            case OTHER -> {
                // OTHER dùng Electronics làm class nền (Item là abstract), ghi đè category
                Item other = ItemFactory.createSimpleItem(ItemCategory.OTHER, name, price, seller);
                other.setId(id);
                yield other;
            }
            default -> throw new IllegalArgumentException("Unknown category: " + cat);
        };
        
        if (json.get("imageBase64") != null) {
            result.setImageBase64((String) json.get("imageBase64"));
        }
        
        return result;
    }
}
