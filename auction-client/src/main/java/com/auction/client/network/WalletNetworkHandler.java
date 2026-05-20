package com.auction.client.network;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unchecked")
public class WalletNetworkHandler {
    private final SocketConnection connection;

    public WalletNetworkHandler(SocketConnection connection) {
        this.connection = connection;
    }

    /** Lấy thông tin ví của user. Trả về JSONObject chứa wallet info hoặc null. */
    public JSONObject getWallet(Long userId) {
        try {
            JSONObject req = new JSONObject();
            req.put("action", "GET_WALLET");
            req.put("userId", userId);
            String json = connection.sendRequest(req.toJSONString());
            JSONObject res = (JSONObject) new JSONParser().parse(json);
            if ("OK".equals(res.get("status"))) {
                return (JSONObject) res.get("wallet");
            }
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /** Lấy lịch sử giao dịch ví. Trả về danh sách JSONObject. */
    public List<JSONObject> getTransactions(Long userId) {
        List<JSONObject> result = new ArrayList<>();
        try {
            JSONObject req = new JSONObject();
            req.put("action", "GET_WALLET_TRANSACTIONS");
            req.put("userId", userId);
            String json = connection.sendRequest(req.toJSONString());
            JSONObject res = (JSONObject) new JSONParser().parse(json);
            if ("OK".equals(res.get("status"))) {
                JSONArray arr = (JSONArray) res.get("transactions");
                if (arr != null) {
                    for (Object obj : arr) {
                        result.add((JSONObject) obj);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    /** Admin điều chỉnh số dư. Trả về true nếu thành công. */
    public boolean adminAdjustBalance(Long userId, long amount, Long adminId, String role, String description) {
        try {
            JSONObject req = new JSONObject();
            req.put("action", "ADMIN_ADJUST_BALANCE");
            req.put("userId", userId);
            req.put("amount", amount);
            req.put("adminId", adminId);
            req.put("role", role);
            req.put("description", description);
            String json = connection.sendRequest(req.toJSONString());
            JSONObject res = (JSONObject) new JSONParser().parse(json);
            return "OK".equals(res.get("status"));
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /** Admin xem tất cả ví. Trả về danh sách JSONObject. */
    public List<JSONObject> adminGetAllWallets(String role) {
        List<JSONObject> result = new ArrayList<>();
        try {
            JSONObject req = new JSONObject();
            req.put("action", "ADMIN_GET_ALL_WALLETS");
            req.put("role", role);
            String json = connection.sendRequest(req.toJSONString());
            JSONObject res = (JSONObject) new JSONParser().parse(json);
            if ("OK".equals(res.get("status"))) {
                JSONArray arr = (JSONArray) res.get("wallets");
                if (arr != null) {
                    for (Object obj : arr) {
                        result.add((JSONObject) obj);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    /** Hủy auto-bid. Trả về true nếu thành công. */
    public boolean cancelAutoBid(Long auctionId, Long bidderId) {
        try {
            JSONObject req = new JSONObject();
            req.put("action", "CANCEL_AUTOBID");
            req.put("auctionId", auctionId);
            req.put("bidderId", bidderId);
            String json = connection.sendRequest(req.toJSONString());
            JSONObject res = (JSONObject) new JSONParser().parse(json);
            return "OK".equals(res.get("status"));
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
