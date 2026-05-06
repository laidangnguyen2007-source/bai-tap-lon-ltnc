package com.auction.client.util;

import java.time.LocalDateTime;
import org.json.simple.JSONObject;
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

public class JsonEntityMapper {
   
  public JsonEntityMapper() {};

  //JSON -> User 
  public User mapToUser(JSONObject json) {
    String role = (String) json.get("role");
    Long id = (Long) json.get("id");
    String username = (String) json.get("username");
    String email = (String) json.get("email");
    String hash = "";
    return switch (role) {
      case "BIDDER" -> {
        Bidder bidder = new Bidder(username, hash, email, 0L);
        bidder.setId(id);
        yield bidder; // yield = return
      }
      
      case "SELLER" -> {
        String shopName = (String) json.get("shopName");
        Seller seller = new Seller(username, hash, email, shopName);
        seller.setId(id);
        yield seller;
      }

      case "ADMIN" -> {
        Admin admin = new Admin(username, hash, email, 1);
        admin.setId(id);
        yield admin;
      }

      default -> throw new IllegalArgumentException("Unknown role: " + role);
    };
  }

  // JSON -> Item
  public Item mapToItem(JSONObject json) {
    ItemCategory category = ItemCategory.valueOf((String) json.get("category"));
    Long id = ((Number) json.get("id")).longValue();
    LocalDateTime createdAt = LocalDateTime.parse((String) json.get("createdAt"));
    String name = (String) json.get("name");
    String desc = (String) json.get("description");
    // Handle potential nulls for description
    if (desc == null) desc = "Đang cập nhật...";
    long price  = ((Number) json.get("startingPrice")).longValue();
    Long seller = ((Number) json.get("sellerId")).longValue();

    switch (category) {
      case ELECTRONICS:
        return ItemFactory.reconstructElectronics(
        id, createdAt, name, desc, price, seller,
        (String) json.get("brand"),
        json.get("warrantyMonths") != null ? ((Number) json.get("warrantyMonths")).intValue() : 0,
        json.get("powerWatts") != null ? ((Number) json.get("powerWatts")).doubleValue() : 0.0
      );
      case ARTWORK:
        return ItemFactory.reconstructArtwork(
        id, createdAt, name, desc, price, seller,
        (String) json.get("artistName"),
        json.get("yearCreated") != null ? ((Number) json.get("yearCreated")).intValue() : 0,
        (String) json.get("medium")
      );
      case VEHICLE:
        return ItemFactory.reconstructVehicle(
        id, createdAt, name, desc, price, seller,
        (String) json.get("manufacturer"),
        json.get("yearManufactured") != null ? ((Number) json.get("yearManufactured")).intValue() : 0,
        json.get("mileageKm") != null ? ((Number) json.get("mileageKm")).intValue() : 0,
        (String) json.get("fuelType")
      );
      default:
        throw new IllegalArgumentException("Unknown item: " + category);
    }
  }
  
  // JSON -> BidTransaction
  public BidTransaction mapToBid(JSONObject json) {
    Long id = json.get("id") != null ? ((Number) json.get("id")).longValue() : -1L;
    
    LocalDateTime createdAt;
    try {
        createdAt = LocalDateTime.parse((String) json.get("createdAt"));
    } catch (Exception e) {
        createdAt = LocalDateTime.now(); // Fallback nếu format có vấn đề
    }

    Long auctionId = json.get("auctionId") != null ? ((Number) json.get("auctionId")).longValue() : -1L;
    Long bidderId = json.get("bidderId") != null ? ((Number) json.get("bidderId")).longValue() : -1L;
    long amount = json.get("amount") != null ? ((Number) json.get("amount")).longValue() : 0L;
    
    LocalDateTime timestamp;
    try {
        timestamp = LocalDateTime.parse((String) json.get("timestamp"));
    } catch (Exception e) {
        timestamp = LocalDateTime.now(); // Fallback
    }

    return new BidTransaction(id, createdAt, auctionId, bidderId, amount, timestamp);
  }

  // JSON -> Auction
  public Auction mapToAuction(JSONObject json) {
    Long id = json.get("id") != null ? Long.parseLong(json.get("id").toString()) : null;
    LocalDateTime createdAt = LocalDateTime.parse(json.get("createdAt").toString());
    Long itemId = json.get("itemId") != null ? Long.parseLong(json.get("itemId").toString()) : null;
    Long sellerId = json.get("sellerId") != null ? Long.parseLong(json.get("sellerId").toString()) : null;
    long currentPrice = json.get("currentPrice") != null ? Long.parseLong(json.get("currentPrice").toString()) : 0L;
    Long currentWinnerId = json.get("currentWinnerId") != null ? Long.parseLong(json.get("currentWinnerId").toString()) : null;
    AuctionStatus status = AuctionStatus.valueOf(json.get("status").toString());
    LocalDateTime startTime = LocalDateTime.parse(json.get("startTime").toString());
    LocalDateTime endTime = LocalDateTime.parse(json.get("endTime").toString());
    
    Auction auction = new Auction(id, createdAt, itemId, sellerId, currentPrice, currentWinnerId, status, startTime, endTime);
    Object nameObj = json.get("itemName");
    auction.setItemName(nameObj != null ? nameObj.toString() : "Sản phẩm #" + itemId);
    return auction;
  }
}
