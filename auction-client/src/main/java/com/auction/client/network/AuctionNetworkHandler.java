package com.auction.client.network;

import com.auction.client.util.JsonMapper;
import java.util.ArrayList;
import java.util.List;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import server.model.entity.Auction;

@SuppressWarnings("unchecked")
public class AuctionNetworkHandler {
  private final SocketConnection connection;

  public AuctionNetworkHandler(SocketConnection connection) {
    this.connection = connection;
  }

  public List<Auction> getAllAuctions() {
    try {
      JSONObject req = new JSONObject();
      req.put("action", "GET_ALL_AUCTIONS");
      String json = connection.sendRequest(req.toJSONString());
      JSONObject res = (JSONObject) new JSONParser().parse(json);
      if (!"OK".equals(res.get("status"))) return new ArrayList<>();
      List<Auction> result = new ArrayList<>();
      for (Object obj : (JSONArray) res.get("auctions")) {
        result.add(JsonMapper.mapToAuction((JSONObject) obj));
      }
      return result;
    } catch (Exception e) {
      return new ArrayList<>();
    }
  }

  public List<Auction> getAuctionsBySeller(Long sellerId) {
    try {
      JSONObject req = new JSONObject();
      req.put("action", "GET_AUCTIONS_BY_SELLER");
      req.put("sellerId", sellerId);
      String json = connection.sendRequest(req.toJSONString());
      JSONObject res = (JSONObject) new JSONParser().parse(json);
      if (!"OK".equals(res.get("status"))) return new ArrayList<>();
      List<Auction> result = new ArrayList<>();
      for (Object obj : (JSONArray) res.get("auctions")) {
        result.add(JsonMapper.mapToAuction((JSONObject) obj));
      }
      return result;
    } catch (Exception e) {
      return new ArrayList<>();
    }
  }

  public Long createAuction(Auction auction) {
    try {
      JSONObject req = new JSONObject();
      req.put("action", "CREATE_AUCTION");
      // Server tự tạo Item từ itemName + category — KHÔNG gửi itemId
      req.put("itemName", auction.getItemName());
      req.put("category", auction.getItemCategory());
      req.put("itemDescription", auction.getItemDescription());
      req.put("itemSpecifics", auction.getItemSpecifics());
      req.put("imageBase64", auction.getImageBase64());
      req.put("sellerId", auction.getSellerId());
      req.put("startingPrice", auction.getCurrentPrice());
      req.put("startTime", auction.getStartTime().toString());
      req.put("endTime", auction.getEndTime().toString());
      req.put("minBidStep", auction.getMinBidStep());
      String json = connection.sendRequest(req.toJSONString());
      JSONObject res = (JSONObject) new JSONParser().parse(json);
      if (!"OK".equals(res.get("status"))) return -1L;
      // Dùng Number thay vì cast thẳng (Long) để tránh ClassCastException
      return ((Number) res.get("auctionId")).longValue();
    } catch (Exception e) {
      System.err.println("[AuctionHandler] createAuction error: " + e.getMessage());
      return -1L;
    }
  }

  public boolean deleteAuction(Long auctionId) {
    try {
      JSONObject req = new JSONObject();
      req.put("action", "DELETE_AUCTION");
      req.put("auctionId", auctionId);
      String json = connection.sendRequest(req.toJSONString());
      JSONObject res = (JSONObject) new JSONParser().parse(json);
      return "OK".equals(res.get("status"));
    } catch (Exception e) {
      return false;
    }
  }

  public boolean resetAuction(Long auctionId) {
    try {
      JSONObject req = new JSONObject();
      req.put("action", "RESET_AUCTION");
      req.put("auctionId", auctionId);
      String json = connection.sendRequest(req.toJSONString());
      JSONObject res = (JSONObject) new JSONParser().parse(json);
      return "OK".equals(res.get("status"));
    } catch (Exception e) {
      return false;
    }
  }

  public boolean updateAuctionAdmin(
      Long auctionId,
      long price,
      String status,
      String startTime,
      String endTime,
      String category) {
    try {
      JSONObject req = new JSONObject();
      req.put("action", "ADMIN_UPDATE_AUCTION");
      req.put("auctionId", auctionId);
      req.put("currentPrice", price);
      req.put("status", status);
      req.put("startTime", startTime);
      req.put("endTime", endTime);
      req.put("category", category);
      String json = connection.sendRequest(req.toJSONString());
      JSONObject res = (JSONObject) new JSONParser().parse(json);
      return "OK".equals(res.get("status"));
    } catch (Exception e) {
      return false;
    }
  }

  public boolean updateAuctionSeller(
      Long auctionId,
      Long sellerId,
      String itemName,
      String category,
      long startingPrice,
      String startTime,
      String endTime,
      String itemDescription,
      String itemSpecifics,
      String imageBase64,
      long minBidStep) {
    try {
      JSONObject req = new JSONObject();
      req.put("action", "SELLER_UPDATE_AUCTION");
      req.put("auctionId", auctionId);
      req.put("sellerId", sellerId);
      req.put("itemName", itemName);
      req.put("category", category);
      req.put("itemDescription", itemDescription);
      req.put("itemSpecifics", itemSpecifics);
      req.put("imageBase64", imageBase64);
      req.put("startingPrice", startingPrice);
      req.put("startTime", startTime);
      req.put("endTime", endTime);
      req.put("minBidStep", minBidStep);
      String json = connection.sendRequest(req.toJSONString());
      JSONObject res = (JSONObject) new JSONParser().parse(json);
      return "OK".equals(res.get("status"));
    } catch (Exception e) {
      return false;
    }
  }

  public boolean deleteAuctionSeller(Long auctionId, Long sellerId) {
    try {
      JSONObject req = new JSONObject();
      req.put("action", "SELLER_DELETE_AUCTION");
      req.put("auctionId", auctionId);
      req.put("sellerId", sellerId);
      String json = connection.sendRequest(req.toJSONString());
      JSONObject res = (JSONObject) new JSONParser().parse(json);
      return "OK".equals(res.get("status"));
    } catch (Exception e) {
      return false;
    }
  }
}
