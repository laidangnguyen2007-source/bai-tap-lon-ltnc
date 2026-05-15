package com.auction.server.handler;

import com.auction.server.dao.AuctionDao;
import com.auction.server.dao.BidTransactionDao;
import com.auction.server.model.entity.Auction;
import com.auction.server.model.entity.BidTransaction;
import com.auction.server.net.ClientBroadcaster;
import com.auction.server.net.EntityJsonMapper;
import com.auction.server.net.JsonResponses;
import com.auction.server.service.AuctionManager;
import com.auction.server.service.AuctionStatusSynchronizer;
import org.json.JSONObject;
import java.time.LocalDateTime;
import java.util.List;
import com.auction.server.model.strategy.AutoBidStrategy;

/**
 * Luồng <b>đặt giá</b>: phối hợp bộ nhớ đệm {@link AuctionManager}, DAO, và broadcast realtime.
 *
 * <p><b>Vì sao tách class riêng:</b> Đây là use-case nhạy cảm (đồng thời, anti-snipe, ghi DB, push
 * socket). Gom vào một lớp giúp đọc và debug dễ hơn so với file server ngàn dòng.
 */
public final class BiddingHandlers {

  private final AuctionDao auctionDao;
  private final BidTransactionDao bidTransactionDao;
  private final EntityJsonMapper jsonMapper;
  private final ClientBroadcaster broadcaster;

  public BiddingHandlers(
      AuctionDao auctionDao,
      BidTransactionDao bidTransactionDao,
      EntityJsonMapper jsonMapper,
      ClientBroadcaster broadcaster) {
    this.auctionDao = auctionDao;
    this.bidTransactionDao = bidTransactionDao;
    this.jsonMapper = jsonMapper;
    this.broadcaster = broadcaster;
  }

  /**
   * Thành công: trả {@code null} (client chờ {@code BID_UPDATE}). Thất bại: trả JSON lỗi một dòng.
   */
  public String placeBid(JSONObject req) throws Exception {
    Long auctionId = req.getLong("auctionId");
    Long bidderId = req.getLong("bidderId");
    long amount = req.getLong("amount");

    try {
      AuctionStatusSynchronizer.syncWithClock(auctionDao);
      Auction auction = AuctionManager.getInstance().placeBid(auctionId, bidderId, amount);
      BidTransaction bid = new BidTransaction(auctionId, bidderId, amount);
      bidTransactionDao.save(bid);
      auctionDao.update(auction);

      System.out.println(
          "BID: auction #" + auctionId + " | bidder #" + bidderId + " | " + amount + " VND");

      JSONObject push = new JSONObject();
      push.put("type", "BID_UPDATE");
      push.put("bid", jsonMapper.bidToJSON(bid));
      broadcaster.broadcast(push.toString());

      // Kích hoạt các AutoBid (nếu có)
      List<BidTransaction> autoBids = AuctionManager.getInstance().resolveAutoBids(auctionId);
      for (BidTransaction autoBid : autoBids) {
          bidTransactionDao.save(autoBid);
          auctionDao.update(auction);
          
          System.out.println("AUTO-BID: auction #" + auctionId + " | bidder #" + autoBid.getBidderId() + " | " + autoBid.getAmount() + " VND");
          JSONObject autoPush = new JSONObject();
          autoPush.put("type", "BID_UPDATE");
          autoPush.put("bid", jsonMapper.bidToJSON(autoBid));
          broadcaster.broadcast(autoPush.toString());
      }

      return null;
    } catch (Exception e) {
      e.printStackTrace();
      System.out.println("BID REJECTED: " + e.getMessage());
      return JsonResponses.error(e.getMessage());
    }
  }

  /**
   * Đăng ký một chiến lược AutoBid cho người dùng.
   */
  public String registerAutoBid(JSONObject req) throws Exception {
    try {
      Long auctionId = req.getLong("auctionId");
      Long bidderId = req.getLong("bidderId");
      long maxBid = req.getLong("maxBid");
      long increment = req.getLong("increment");

      AutoBidStrategy strategy = new AutoBidStrategy(bidderId, maxBid, increment, LocalDateTime.now());
      AuctionManager.getInstance().registerAutoBid(auctionId, strategy);

      System.out.println("REGISTER AUTOBID: auction #" + auctionId + " | bidder #" + bidderId + " | maxBid=" + maxBid);

      // Kích hoạt AutoBid ngay lập tức phòng trường hợp có thể đấu giá luôn
      List<BidTransaction> autoBids = AuctionManager.getInstance().resolveAutoBids(auctionId);
      if (!autoBids.isEmpty()) {
          Auction auction = AuctionManager.getInstance().findById(auctionId).orElse(null);
          if (auction != null) {
              for (BidTransaction autoBid : autoBids) {
                  bidTransactionDao.save(autoBid);
                  auctionDao.update(auction);
                  
                  System.out.println("AUTO-BID (Immediate): auction #" + auctionId + " | bidder #" + autoBid.getBidderId() + " | " + autoBid.getAmount() + " VND");
                  JSONObject autoPush = new JSONObject();
                  autoPush.put("type", "BID_UPDATE");
                  autoPush.put("bid", jsonMapper.bidToJSON(autoBid));
                  broadcaster.broadcast(autoPush.toString());
              }
          }
      }

      JSONObject res = new JSONObject();
      res.put("status", "OK");
      return res.toString();
    } catch (Exception e) {
      e.printStackTrace();
      System.out.println("REGISTER AUTOBID REJECTED: " + e.getMessage());
      return JsonResponses.error(e.getMessage());
    }
  }
}
