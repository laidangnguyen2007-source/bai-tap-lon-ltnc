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
  private final ServerService serverService; // call notify

  public BidController(ServerService serverService) throws UnknownHostException, IOException {
    this.connection = SocketConnection.getInstance();
    this.serverService = serverService;
    startListening(); //Khởi động thread lắng nghe push từ server  
  }

  public boolean placeBid(Long auctionId, Long bidderId, long amount) {
    try {
      JSONObject req = new JSONObject();
      req.put("action", "PLACE_BID");
      req.put("auctionId", auctionId);
      req.put("bidderId", bidderId);
      req.put("amount", amount);

      String json = connection.sendRequest(req.toJSONString(req));
      JSONObject res = (JSONObject) new JSONParser().parse(json);

      if (!"OK".equals(res.get("status"))) {
        return false;
      }
      return true;
    } catch (Exception e) {
      e.printStackTrace();
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
    Long id = (Long) json.get("id");
    LocalDateTime createAt = LocalDateTime.parse((String) json.get("createAt"));
    Long auctionId = (Long) json.get("auctionId");
    Long bidderId = (Long) json.get("bidderId");
    long amount = ((Long) json.get("amount")).longValue();
    LocalDateTime timestamp = LocalDateTime.parse((String) json.get("timestamp"));

    return new BidTransaction(id, createAt, auctionId, bidderId, amount, timestamp);
  }

  private void startListening() {
    Thread listener = new Thread(() -> {
      try {
        String line;
        JSONParser parser = new JSONParser();
        while ((line = connection.getReader().readLine()) != null) {
          JSONObject push = (JSONObject) parser.parse(line);
          String type = (String) push.get("type");

          if ("BID_UPDATE".equals(type)) {
            BidTransaction bid = mapToBid((JSONObject) push.get("bid"));
            serverService.notifyBidUpdated(bid);

          } else if ("AUCTION_STATUS_CHANGED".equals(type)) {
            Long auctionId = (Long) push.get("auctionId");
            String status = (String) push.get("newStatus");
            serverService.notifyAuctionStatusChanged(auctionId, status);
          } 
        }
      } catch (Exception e) {
        System.err.println("NetworkListener mất kết nối: " + e.getMessage());
      }
    }, "NetworkListener");
    listener.setDaemon(true);
    listener.start();
  }

  
}
