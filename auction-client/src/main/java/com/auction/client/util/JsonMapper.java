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
 * Utility class chuyên trách việc chuyển đổi JSON sang Object (Mapping).
 * Giúp tuân thủ SRP: Các lớp Network chỉ lo gửi/nhận, lớp này lo parse dữ liệu.
 */
public final class JsonMapper {
    private JsonMapper() { throw new UnsupportedOperationException(); }

    public static User mapToUser(JSONObject json) {
        String role = (String) json.get("role");
        Long id = ((Number) json.get("id")).longValue();
        String username = (String) json.get("username");
        String email = (String) json.get("email");
        return switch (role) {
            case "BIDDER" -> {
                long balance = json.get("balance") != null ? ((Number) json.get("balance")).longValue() : 0L;
                Bidder b = new Bidder(username, "", email, balance);
                b.setId(id); yield b;
            }
            case "SELLER" -> {
                Seller s = new Seller(username, "", email, (String) json.get("shopName"));
                s.setId(id); yield s;
            }
            case "ADMIN" -> {
                Admin a = new Admin(username, "", email, 1);
                a.setId(id); yield a;
            }
            default -> throw new IllegalArgumentException("Unknown role: " + role);
        };
    }

    public static Auction mapToAuction(JSONObject json) {
        Long id = ((Number) json.get("id")).longValue();
        LocalDateTime createdAt = LocalDateTime.parse((String) json.get("createdAt"));
        Long itemId = ((Number) json.get("itemId")).longValue();
        Long sellerId = ((Number) json.get("sellerId")).longValue();
        long currentPrice = ((Number) json.get("currentPrice")).longValue();
        Long currentWinnerId = json.get("currentWinnerId") != null ? ((Number) json.get("currentWinnerId")).longValue() : null;
        AuctionStatus status = AuctionStatus.valueOf((String) json.get("status"));
        LocalDateTime startTime = LocalDateTime.parse((String) json.get("startTime"));
        LocalDateTime endTime = LocalDateTime.parse((String) json.get("endTime"));
        Auction a = new Auction(id, createdAt, itemId, sellerId, currentPrice, currentWinnerId, status, startTime, endTime);
        a.setItemName(json.get("itemName") != null ? (String) json.get("itemName") : "Sản phẩm #" + itemId);
        return a;
    }

    public static BidTransaction mapToBid(JSONObject json) {
        Long id = json.get("id") != null ? ((Number) json.get("id")).longValue() : -1L;
        LocalDateTime createdAt = LocalDateTime.parse((String) json.get("createdAt"));
        Long auctionId = ((Number) json.get("auctionId")).longValue();
        Long bidderId = ((Number) json.get("bidderId")).longValue();
        long amount = ((Number) json.get("amount")).longValue();
        LocalDateTime timestamp = LocalDateTime.parse((String) json.get("timestamp"));
        return new BidTransaction(id, createdAt, auctionId, bidderId, amount, timestamp);
    }

    public static Item mapToItem(JSONObject json) {
        ItemCategory cat = ItemCategory.valueOf((String) json.get("category"));
        Long id = ((Number) json.get("id")).longValue();
        LocalDateTime createdAt = LocalDateTime.parse((String) json.get("createdAt"));
        String name = (String) json.get("name");
        String desc = json.get("description") != null ? (String) json.get("description") : "Đang cập nhật...";
        long price = ((Number) json.get("startingPrice")).longValue();
        Long seller = ((Number) json.get("sellerId")).longValue();

        return switch (cat) {
            case ELECTRONICS -> ItemFactory.reconstructElectronics(id, createdAt, name, desc, price, seller, (String) json.get("brand"), json.get("warrantyMonths") != null ? ((Number) json.get("warrantyMonths")).intValue() : 0, json.get("powerWatts") != null ? ((Number) json.get("powerWatts")).doubleValue() : 0.0);
            case ARTWORK -> ItemFactory.reconstructArtwork(id, createdAt, name, desc, price, seller, (String) json.get("artistName"), json.get("yearCreated") != null ? ((Number) json.get("yearCreated")).intValue() : 0, (String) json.get("medium"));
            case VEHICLE -> ItemFactory.reconstructVehicle(id, createdAt, name, desc, price, seller, (String) json.get("manufacturer"), json.get("yearManufactured") != null ? ((Number) json.get("yearManufactured")).intValue() : 0, json.get("mileageKm") != null ? ((Number) json.get("mileageKm")).intValue() : 0, (String) json.get("fuelType"));
        };
    }
}
