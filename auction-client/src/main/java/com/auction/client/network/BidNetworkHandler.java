package com.auction.client.network;

import com.auction.client.util.JsonMapper;
import com.auction.server.model.entity.BidTransaction;
import java.util.ArrayList;
import java.util.List;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

@SuppressWarnings("unchecked")
public class BidNetworkHandler {
    private final SocketConnection connection;

    public BidNetworkHandler(SocketConnection connection) {
        this.connection = connection;
    }

    public boolean placeBid(Long auctionId, Long bidderId, long amount) {
        try {
            JSONObject req = new JSONObject();
            req.put("action", "PLACE_BID");
            req.put("auctionId", auctionId);
            req.put("bidderId", bidderId);
            req.put("amount", amount);
            connection.sendOneWay(req.toJSONString());
            return true;
        } catch (Exception e) { return false; }
    }

    public List<BidTransaction> getBidHistory(Long auctionId) {
        try {
            JSONObject req = new JSONObject();
            req.put("action", "GET_BID_HISTORY");
            req.put("auctionId", auctionId);
            String json = connection.sendRequest(req.toJSONString());
            JSONObject res = (JSONObject) new JSONParser().parse(json);
            if (!"OK".equals(res.get("status"))) return new ArrayList<>();
            List<BidTransaction> result = new ArrayList<>();
            for (Object obj : (JSONArray) res.get("bids")) {
                result.add(JsonMapper.mapToBid((JSONObject) obj));
            }
            return result;
        } catch (Exception e) { return new ArrayList<>(); }
    }

    public boolean registerAutoBid(Long auctionId, Long bidderId, long maxBid, long increment) {
        try {
            JSONObject req = new JSONObject();
            req.put("action", "REGISTER_AUTOBID");
            req.put("auctionId", auctionId);
            req.put("bidderId", bidderId);
            req.put("maxBid", maxBid);
            req.put("increment", increment);
            String json = connection.sendRequest(req.toJSONString());
            JSONObject res = (JSONObject) new JSONParser().parse(json);
            return "OK".equals(res.get("status"));
        } catch (Exception e) { return false; }
    }
}
