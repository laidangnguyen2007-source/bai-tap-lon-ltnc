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
      return null;
    } catch (Exception e) {
      e.printStackTrace();
      System.out.println("BID REJECTED: " + e.getMessage());
      return JsonResponses.error(e.getMessage());
    }
  }
}
