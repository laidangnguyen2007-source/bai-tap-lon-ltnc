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


}
