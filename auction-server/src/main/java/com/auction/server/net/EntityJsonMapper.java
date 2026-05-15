package com.auction.server.net;

import com.auction.server.dao.ItemDao;
import com.auction.server.model.entity.Auction;
import com.auction.server.model.entity.BidTransaction;
import com.auction.server.model.entity.item.Artwork;
import com.auction.server.model.entity.item.Electronics;
import com.auction.server.model.entity.item.Item;
import com.auction.server.model.entity.item.Vehicle;
import org.json.JSONObject;

/**
 * Chuyển <b>entity domain</b> sang {@link JSONObject} đúng contract mà client đang kỳ vọng.
 *
 * <p><b>SRP:</b> Tách hẳn khỏi lớp mở socket và khỏi handler nghiệp vụ — khi đổi field JSON chỉ sửa
 * một nơi.
 *
 * <p><b>Phụ thuộc {@link ItemDao}:</b> {@link #auctionToJSON(Auction)} cần tên sản phẩm để hiển thị
 * list phiên; nếu không tìm thấy item thì fallback chuỗi an toàn (giữ hành vi cũ của {@code
 * Server}).
 */
public final class EntityJsonMapper {

  private final ItemDao itemDao;

  public EntityJsonMapper(ItemDao itemDao) {
    this.itemDao = itemDao;
  }

  public JSONObject bidToJSON(BidTransaction bid) {
    JSONObject json = new JSONObject();
    json.put("id", bid.getId() != null ? bid.getId() : -1L);
    json.put("createdAt", bid.getCreatedAt().toString());
    json.put("auctionId", bid.getAuctionId());
    json.put("bidderId", bid.getBidderId());
    json.put("amount", bid.getAmount());
    json.put("timestamp", bid.getTimestamp().toString());
    return json;
  }

  public JSONObject auctionToJSON(Auction auction) {
    JSONObject json = new JSONObject();
    json.put("id", auction.getId());
    json.put("createdAt", auction.getCreatedAt().toString());
    json.put("itemId", auction.getItemId());

    // Tra cứu Item từ DB để lấy tên + loại sản phẩm hiển thị trên danh sách
    var itemOpt = itemDao.findById(auction.getItemId());
    String name = itemOpt.map(Item::getName).orElse("Sản phẩm #" + auction.getItemId());
    String category = itemOpt.map(i -> i.getCategory().name()).orElse("OTHER");

    // Lấy giá khởi điểm từ item vào cho vào json
    long startingPrice = itemOpt.map(Item::getStartingPrice).orElse(0L);
    json.put("startingPrice", startingPrice);

    json.put("itemName", name);
    json.put("itemCategory", category); // Loại sản phẩm để hiển thị ở bảng Seller Dashboard
    json.put("itemDescription", itemOpt.map(Item::getDescription).orElse(""));
    json.put("imageBase64", itemOpt.map(Item::getImageBase64).orElse(null));
    json.put("sellerId", auction.getSellerId());
    json.put("currentPrice", auction.getCurrentPrice());
    json.put("currentWinnerId", auction.getCurrentWinnerId());
    json.put("status", auction.getStatus());
    json.put("startTime", auction.getStartTime().toString());
    json.put("endTime", auction.getEndTime().toString());
    json.put("minBidStep", auction.getMinBidStep());
    return json;
  }

  /** Giữ nguyên cấu trúc field theo từng loại {@link Item} (đa hình STI). */
  public JSONObject itemToJSON(Item item) {
    JSONObject json = new JSONObject();
    json.put("id", item.getId());
    json.put("createdAt", item.getCreatedAt().toString());
    json.put("name", item.getName());
    json.put("description", item.getDescription());
    json.put("imageBase64", item.getImageBase64());
    json.put("startingPrice", item.getStartingPrice());
    json.put("sellerId", item.getSellerId());
    json.put("category", item.getCategory().name());

    if (item instanceof Electronics e) {
      json.put("brand", e.getBrand());
      json.put("warrantyMonths", e.getWarrantyMonths());
      json.put("powerWatts", e.getPowerWatts());
    } else if (item instanceof Artwork a) {
      json.put("artistName", a.getArtistName());
      json.put("yearCreated", a.getYearCreated());
      json.put("medium", a.getMedium());
    } else if (item instanceof Vehicle v) {
      json.put("manufacturer", v.getManufacturer());
      json.put("yearManufactured", v.getYearManufactured());
      json.put("mileageKm", v.getMileageKm());
      json.put("fuelType", v.getFuelType());
    }
    return json;
  }
}
