package com.auction.client.network;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import com.auction.client.util.JsonEntityMapper;
import com.auction.server.model.entity.Auction;

public class AuctionController {
  private final SocketConnection connection;
  private final JsonEntityMapper entityMapper = new JsonEntityMapper();

  public AuctionController() throws UnknownHostException, IOException {
    this.connection = SocketConnection.getInstance();
  }

  public List<Auction> getAllAuctions() {
    try {
      JSONObject req = new JSONObject();
      req.put("action", "GET_ALL_AUCTIONS");

      String json = connection.sendRequest(req.toString());
      JSONObject res = (JSONObject) new JSONParser().parse(json);

      if (!"OK".equals(res.get("status"))) {
        return new ArrayList<>();
      }

      List<Auction> result = new ArrayList<>();
      JSONArray arr = (JSONArray) res.get("auctions");
      for (Object obj : arr) {
        result.add(entityMapper.mapToAuction((JSONObject) obj));
      }
      return result;
    } catch (Exception e) {
      e.printStackTrace();
      return new ArrayList<>();
    }
  }

  public List<Auction> getAuctionsBySeller(Long sellerId) {
    try {
      JSONObject req = new JSONObject();
      req.put("action", "GET_AUCTIONS_BY_SELLER");
      req.put("sellerId", sellerId);

      String json = connection.sendRequest(req.toJSONString(req));
      JSONObject res = (JSONObject) new JSONParser().parse(json);

      if (!"OK".equals(res.get("status"))) {
        return new ArrayList<>();
      }

      List<Auction> result = new ArrayList<>();
      JSONArray arr = (JSONArray) res.get("auctions");
      for (Object obj : arr) {
        result.add(entityMapper.mapToAuction((JSONObject) obj));
      }
      return result;

    } catch (Exception e) {
      e.printStackTrace();
      return new ArrayList<>();
    }
  }
  /**
   * Tạo phiên đấu giá mới — gửi tên sản phẩm + loại sản phẩm lên server.
   * Server sẽ tự tạo Item trong DB rồi tạo Auction gắn với Item đó.
   * Không cần Auction object — truyền raw params để tránh lỗi itemId=0.
   *
   * @param sellerId ID của Seller
   * @param startingPrice giá khởi điểm
   * @param startTime thời gian bắt đầu
   * @param endTime thời gian kết thúc
   * @param itemName tên sản phẩm do Seller đặt
   * @param category loại sản phẩm (ELECTRONICS, ARTWORK, VEHICLE, OTHER)
   * @return ID của phiên đấu giá vừa tạo, -1 nếu thất bại
   */
  public Long createAuction(Long sellerId, long startingPrice,
      java.time.LocalDateTime startTime, java.time.LocalDateTime endTime,
      String itemName, String category) {
    try {
      JSONObject req = new JSONObject();
      req.put("action", "CREATE_AUCTION");
      // Gửi tên + loại sản phẩm (server tự tạo Item với ID auto-increment)
      req.put("itemName", itemName);
      req.put("category", category);
      req.put("sellerId", sellerId);
      req.put("startingPrice", startingPrice);
      req.put("startTime", startTime.toString());
      req.put("endTime", endTime.toString());

      String json = connection.sendRequest(req.toJSONString());
      JSONObject res = (JSONObject) new JSONParser().parse(json);

      if(!"OK".equals(res.get("status"))) return -1L;
      return (Long) res.get("auctionId");
    } catch (Exception e) {
      e.printStackTrace();
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
      e.printStackTrace();
      return false;
    }
  }

  public boolean updateAuctionAdmin(Long auctionId, long price, String status, String startTime, String endTime, String category) {
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
      e.printStackTrace();
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
      e.printStackTrace();
      return false;
    }
  }

  public boolean updateAuctionSeller(Long auctionId, Long sellerId, String itemName, String category, long startingPrice, java.time.LocalDateTime startTime, java.time.LocalDateTime endTime) {
    try {
      JSONObject req = new JSONObject();
      req.put("action", "SELLER_UPDATE_AUCTION");
      req.put("auctionId", auctionId);
      req.put("sellerId", sellerId);
      req.put("itemName", itemName);
      req.put("category", category);
      req.put("startingPrice", startingPrice);
      req.put("startTime", startTime.toString());
      req.put("endTime", endTime.toString());

      String json = connection.sendRequest(req.toJSONString());
      JSONObject res = (JSONObject) new JSONParser().parse(json);

      return "OK".equals(res.get("status"));
    } catch (Exception e) {
      e.printStackTrace();
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
      e.printStackTrace();
      return false;
    }
  }
}
