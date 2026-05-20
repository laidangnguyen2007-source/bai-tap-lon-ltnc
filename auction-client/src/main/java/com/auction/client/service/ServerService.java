package com.auction.client.service;

import com.auction.client.network.*;
import com.auction.client.observer.AuctionObserver;
import com.auction.client.util.JsonMapper;
import server.model.entity.Auction;
import server.model.entity.BidTransaction;
import server.model.entity.item.Item;
import server.model.entity.user.User;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

/**
 * Facade Service — Cổng kết nối duy nhất của ứng dụng Client. Tuân thủ SRP: Lớp này chỉ điều phối
 * (delegate) yêu cầu tới các Handler chuyên biệt.
 */
public class ServerService {
    private final UserNetworkHandler userHandler;
    private final AuctionNetworkHandler auctionHandler;
    private final BidNetworkHandler bidHandler;
    private final ItemNetworkHandler itemHandler;
    private final WalletNetworkHandler walletHandler;

    private final List<AuctionObserver> observers = new ArrayList<>();
    private final Consumer<String> pushHandler;

    public ServerService() {
        try {
            SocketConnection connection = SocketConnection.getInstance();
            this.userHandler = new UserNetworkHandler(connection);
            this.auctionHandler = new AuctionNetworkHandler(connection);
            this.bidHandler = new BidNetworkHandler(connection);
            this.itemHandler = new ItemNetworkHandler(connection);
            this.walletHandler = new WalletNetworkHandler(connection);

            this.pushHandler = this::handlePushMessage;
            connection.addPushListener(this.pushHandler);
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khởi tạo ServerService: " + e.getMessage());
        }
    }

    // -- REALTIME OBSERVER --

    public void addObserver(AuctionObserver o) {
        if (o != null)
            observers.add(o);
    }

    public void removeObserver(AuctionObserver o) {
        observers.remove(o);
    }

    private void handlePushMessage(String line) {
        try {
            JSONObject push = (JSONObject) new JSONParser().parse(line);
            String type = (String) push.get("type");
            if ("BID_UPDATE".equals(type)) {
                BidTransaction bid = JsonMapper.mapToBid((JSONObject) push.get("bid"));
                observers.forEach(o -> o.onBidUpdated(bid));
            } else if ("AUCTION_STATUS_CHANGED".equals(type)) {
                Long id = ((Number) push.get("auctionId")).longValue();
                String status = (String) push.get("newStatus");
                observers.forEach(o -> o.onAuctionStatusChanged(id, status));
            } else if ("FUNDS_LOCKED".equals(type) || "FUNDS_RELEASED".equals(type)
                    || "OUTBID".equals(type) || "OUTBID_NOTIFICATION".equals(type)
                    || "AUCTION_WON".equals(type) || "AUCTION_LOST".equals(type)
                    || "SELLER_PAYOUT".equals(type) || "ADMIN_BALANCE_ADJUSTED".equals(type)
                    || "AUTO_BID_CANCELLED".equals(type)) {
                observers.forEach(o -> o.onWalletEvent(type, push));
            }
        } catch (Exception ignored) {
        }
    }

    // -- DELEGATION METHODS --

    public User login(String u, String p) {
        return userHandler.login(u, p);
    }

    public boolean register(String u, String p, String e, String r, String s) {
        return userHandler.register(u, p, e, r, s);
    }

    public User getUserById(Long id) {
        return userHandler.getUserById(id);
    }

    public List<Auction> getAllAuctions() {
        return auctionHandler.getAllAuctions();
    }

    public List<Auction> getAuctionsBySeller(Long id) {
        return auctionHandler.getAuctionsBySeller(id);
    }

    public Long createAuction(Auction a) {
        return auctionHandler.createAuction(a);
    }

    public boolean deleteAuction(Long id) {
        return auctionHandler.deleteAuction(id);
    }

    public boolean resetAuction(Long id) {
        return auctionHandler.resetAuction(id);
    }

    public boolean updateAuctionAdmin(Long id, long p, String s, String st, String et, String c) {
        return auctionHandler.updateAuctionAdmin(id, p, s, st, et, c);
    }

    public Item getItemById(Long id) {
        return itemHandler.getItemById(id);
    }

    public boolean placeBid(Long aId, Long bId, long amt) {
        return bidHandler.placeBid(aId, bId, amt);
    }

    public boolean registerAutoBid(Long aId, Long bId, long maxBid, long increment) {
        return bidHandler.registerAutoBid(aId, bId, maxBid, increment);
    }

    public List<BidTransaction> getBidHistory(Long id) {
        return bidHandler.getBidHistory(id);
    }

    public boolean updateAuctionSeller(Long auctionId, Long sellerId, String itemName,
            String category, long startingPrice, java.time.LocalDateTime startTime,
            java.time.LocalDateTime endTime, String itemDescription, String imageBase64,
            long minBidStep) {
        return auctionHandler.updateAuctionSeller(auctionId, sellerId, itemName, category,
                startingPrice, startTime.toString(), endTime.toString(), itemDescription,
                imageBase64, minBidStep);
    }

    public boolean deleteAuctionSeller(Long auctionId, Long sellerId) {
        return auctionHandler.deleteAuctionSeller(auctionId, sellerId);
    }

    // -- WALLET DELEGATION --

    public JSONObject getWallet(Long userId) {
        return walletHandler.getWallet(userId);
    }

    public List<JSONObject> getWalletTransactions(Long userId) {
        return walletHandler.getTransactions(userId);
    }

    public boolean adminAdjustBalance(Long userId, long amount, Long adminId, String role, String desc) {
        return walletHandler.adminAdjustBalance(userId, amount, adminId, role, desc);
    }

    public List<JSONObject> adminGetAllWallets(String role) {
        return walletHandler.adminGetAllWallets(role);
    }

    public boolean cancelAutoBid(Long auctionId, Long bidderId) {
        return walletHandler.cancelAutoBid(auctionId, bidderId);
    }
}

