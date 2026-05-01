package com.auction.client.network;

import java.io.IOException;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import com.auction.server.model.entity.Auction;
import com.auction.server.model.enums.AuctionStatus;

public class AuctionController {
  private final SocketConnection connection;

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
        result.add(mapToAuction((JSONObject) obj));
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
        result.add(mapToAuction((JSONObject) obj));
      }
      return result;

    } catch (Exception e) {
      e.printStackTrace();
      return new ArrayList<>();
    }
  }
  public Long createAuction(Auction auction) {
    try {
      JSONObject req = new JSONObject();
      req.put("action", "CREATE_AUCTION");
      req.put("itemId", auction.getItemId());
      req.put("sellerId", auction.getSellerId());
      req.put("startingPrice", auction.getCurrentPrice());
      req.put("startTime", auction.getStartTime().toString());
      req.put("endTime", auction.getEndTime().toString());

      String json = connection.sendRequest(req.toJSONString());
      JSONObject res = (JSONObject) new JSONParser().parse(json);

      if(!"OK".equals(res.get("status"))) return -1L;
      return (Long) res.get("auctionId");
    } catch (Exception e) {
      e.printStackTrace();
      return -1L;
    }
  }

  // JSON -> Auction
  private Auction mapToAuction(JSONObject json) {
    Long id = (Long) json.get("id");
    LocalDateTime createdAt = LocalDateTime.parse((String) json.get("createdAt"));
    Long itemId = (Long) json.get("itemId");
    Long sellerId = (Long) json.get("sellerId");
    Long currentPrice = ((Long) json.get("currentPrice")).longValue();
    Long currentWinnerId = (Long) json.get("currentWinnerId");
    AuctionStatus status = AuctionStatus.valueOf((String) json.get("status"));
    LocalDateTime startTime = LocalDateTime.parse((String) json.get("startTime"));
    LocalDateTime endTime = LocalDateTime.parse((String) json.get("endTime"));
    
    Auction auction = new Auction(id, createdAt, itemId, sellerId, currentPrice, currentWinnerId, status, startTime, endTime);
    Object nameObj = json.get("itemName");
    auction.setItemName(nameObj != null ? nameObj.toString() : "Sản phẩm #" + itemId);
    return auction;
  }
}
