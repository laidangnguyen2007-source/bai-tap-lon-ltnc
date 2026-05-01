package com.auction.client.network;

import java.io.IOException;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import com.auction.client.service.ServerService;
import com.auction.server.model.entity.BidTransaction;

public class BidController {
  private final SocketConnection connection;
  private final ServerService serverService;
  // Lưu callback để có thể hủy đăng ký khi cần
  private final java.util.function.Consumer<String> pushHandler;

  public BidController(ServerService serverService) throws UnknownHostException, IOException {
    this.connection = SocketConnection.getInstance();
    this.serverService = serverService;
    // Tạo handler và đăng ký vào SocketConnection toàn cục
    this.pushHandler = this::handlePushMessage;
    connection.addPushListener(this.pushHandler);
  }

  /** Hủy đăng ký listener (gọi khi ServerService không còn dùng nữa). */
  public void dispose() {
    connection.removePushListener(this.pushHandler);
  }

  public boolean placeBid(Long auctionId, Long bidderId, long amount) {
    try {
      JSONObject req = new JSONObject();
      req.put("action", "PLACE_BID");
      req.put("auctionId", auctionId);
      req.put("bidderId", bidderId);
      req.put("amount", amount);

      // Gửi lệnh đến server — không cần chờ response đồng bộ.
      // Kết quả (thành công/thất bại) sẽ được xác nhận qua BID_UPDATE push notification.
      connection.sendOneWay(req.toJSONString(req));
      return true; // Luôn trả về true — UI sẽ reset nếu không nhận được BID_UPDATE
    } catch (Exception e) {
      System.err.println("Lỗi gửi PLACE_BID: " + e.getMessage());
      return false;
    }
  }

  public List<BidTransaction> getBidHistory(Long auctionId) {
    try {
      JSONObject req = new JSONObject();
      req.put("action", "GET_BID_HISTORY");
      req.put("auctionId", auctionId);

      String json = connection.sendRequest(req.toJSONString());
      JSONObject res = (JSONObject) new JSONParser().parse(json);

      if (!"OK".equals(res.get("status"))) {
        return new ArrayList<>();
      }
      List<BidTransaction> result = new ArrayList<>();
      for (Object obj : (JSONArray) res.get("bids")) {
        result.add(mapToBid((JSONObject) obj));
      }
      return result;

    } catch (Exception e) {
      e.printStackTrace();
      return new ArrayList<>(); 
    }
    
  }
  private BidTransaction mapToBid(JSONObject json) {
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


  /** Xử lý mọi push message đến từ server (được gọi bởi GlobalNetworkListener). */
  private void handlePushMessage(String line) {
    try {
      JSONObject push = (JSONObject) new JSONParser().parse(line);
      String type = (String) push.get("type");

      if ("BID_UPDATE".equals(type)) {
        BidTransaction bid = mapToBid((JSONObject) push.get("bid"));
        serverService.notifyBidUpdated(bid);
      } else if ("AUCTION_STATUS_CHANGED".equals(type)) {
        Long auctionId = ((Number) push.get("auctionId")).longValue();
        String status = (String) push.get("newStatus");
        serverService.notifyAuctionStatusChanged(auctionId, status);
      }
    } catch (Exception e) {
      System.err.println("[BidController] Lỗi xử lý push: " + e.getMessage());
    }
  }
}
