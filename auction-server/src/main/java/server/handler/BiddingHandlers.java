package server.handler;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.json.JSONObject;
import server.dao.AuctionDao;
import server.dao.AutoBidDao;
import server.dao.BidTransactionDao;
import server.model.entity.Auction;
import server.model.entity.AutoBid;
import server.model.entity.BidTransaction;
import server.model.strategy.AutoBidStrategy;
import server.net.ClientBroadcaster;
import server.net.EntityJsonMapper;
import server.net.JsonResponses;
import server.service.AuctionManager;
import server.service.AuctionManager.BidInfo;
import server.service.AuctionStatusSynchronizer;
import server.service.WalletService;

public final class BiddingHandlers {

  private final AuctionDao auctionDao;
  private final BidTransactionDao bidTransactionDao;
  private final EntityJsonMapper jsonMapper;
  private final ClientBroadcaster broadcaster;
  private final WalletService walletService;
  private final AutoBidDao autoBidDao;

  public BiddingHandlers(
      AuctionDao auctionDao,
      BidTransactionDao bidTransactionDao,
      EntityJsonMapper jsonMapper,
      ClientBroadcaster broadcaster,
      WalletService walletService,
      AutoBidDao autoBidDao) {
    this.auctionDao = auctionDao;
    this.bidTransactionDao = bidTransactionDao;
    this.jsonMapper = jsonMapper;
    this.broadcaster = broadcaster;
    this.walletService = walletService;
    this.autoBidDao = autoBidDao;
  }

  public String placeBid(JSONObject req) throws Exception {
    Long auctionId = req.getLong("auctionId");
    Long bidderId = req.getLong("bidderId");
    long amount = req.getLong("amount");

    try {
      AuctionStatusSynchronizer.syncWithClock(auctionDao, walletService, broadcaster);

      BidInfo prevWinner = AuctionManager.getInstance().getPreviousWinner(auctionId);

      long additionalAmountToLock = amount;
      if (prevWinner != null && prevWinner.bidderId.equals(bidderId)) {
        additionalAmountToLock = amount - prevWinner.amount;
      }

      if (additionalAmountToLock > 0) {
        walletService.lockForBid(bidderId, auctionId, additionalAmountToLock);
      }

      LocalDateTime oldEndTime = AuctionManager.getInstance().findById(auctionId).map(Auction::getEndTime).orElse(null);
      Auction auction;
      try {
        auction = AuctionManager.getInstance().placeBid(auctionId, bidderId, amount);
      } catch (Exception e) {
        if (additionalAmountToLock > 0) {
          walletService.releaseForOutbid(bidderId, auctionId, additionalAmountToLock);
        }
        throw e;
      }

      if (prevWinner != null && !prevWinner.isAutoBid && !prevWinner.bidderId.equals(bidderId)) {
        walletService.releaseForOutbid(prevWinner.bidderId, auctionId, prevWinner.amount);
        JSONObject outbidPush = new JSONObject();
        outbidPush.put("type", "OUTBID");
        outbidPush.put("auctionId", auctionId);
        outbidPush.put("userId", prevWinner.bidderId);
        broadcaster.sendToUser(prevWinner.bidderId, outbidPush.toString());
      }

      JSONObject lockedPush = new JSONObject();
      lockedPush.put("type", "FUNDS_LOCKED");
      lockedPush.put("auctionId", auctionId);
      lockedPush.put("userId", bidderId);
      lockedPush.put("amount", amount);
      broadcaster.sendToUser(bidderId, lockedPush.toString());

      BidTransaction bid = new BidTransaction(auctionId, bidderId, amount);
      bidTransactionDao.save(bid);
      auctionDao.update(auction);

      System.out.println(
          "BID: auction #" + auctionId + " | bidder #" + bidderId + " | " + amount + " VND");

      JSONObject push = new JSONObject();
      push.put("type", "BID_UPDATE");
      push.put("bid", jsonMapper.bidToJSON(bid));
      broadcaster.broadcast(push.toString());

      if (oldEndTime != null && auction.getEndTime().isAfter(oldEndTime)) {
        JSONObject extPush = new JSONObject();
        extPush.put("type", "AUCTION_TIME_EXTENDED");
        extPush.put("auctionId", auctionId);
        extPush.put("newEndTime", auction.getEndTime().toString());
        broadcaster.broadcast(extPush.toString());
      }

      BidInfo winnerBeforeAuto = AuctionManager.getInstance().getPreviousWinner(auctionId);
      List<BidTransaction> autoBids = AuctionManager.getInstance().resolveAutoBids(auctionId);

      if (!autoBids.isEmpty()) {
        BidInfo winnerAfterAuto = AuctionManager.getInstance().getPreviousWinner(auctionId);

        if (winnerBeforeAuto != null && !winnerBeforeAuto.isAutoBid) {
          if (winnerAfterAuto == null
              || !winnerAfterAuto.bidderId.equals(winnerBeforeAuto.bidderId)) {
            walletService.releaseForOutbid(
                winnerBeforeAuto.bidderId, auctionId, winnerBeforeAuto.amount);

            JSONObject outbidPush = new JSONObject();
            outbidPush.put("type", "OUTBID");
            outbidPush.put("auctionId", auctionId);
            outbidPush.put("userId", winnerBeforeAuto.bidderId);
            broadcaster.sendToUser(winnerBeforeAuto.bidderId, outbidPush.toString());
          }
        }

        for (BidTransaction autoBid : autoBids) {
          bidTransactionDao.save(autoBid);
          auctionDao.update(auction);

          System.out.println(
              "AUTO-BID: auction #"
                  + auctionId
                  + " | bidder #"
                  + autoBid.getBidderId()
                  + " | "
                  + autoBid.getAmount()
                  + " VND");
          JSONObject autoPush = new JSONObject();
          autoPush.put("type", "BID_UPDATE");
          autoPush.put("bid", jsonMapper.bidToJSON(autoBid));
          broadcaster.broadcast(autoPush.toString());
        }
      }

      JSONObject res = new JSONObject();
      res.put("status", "OK");
      return res.toString();
    } catch (Exception e) {
      e.printStackTrace();
      System.out.println("BID REJECTED: " + e.getMessage());
      return JsonResponses.error(e.getMessage());
    }
  }

  public String registerAutoBid(JSONObject req) throws Exception {
    try {
      Long auctionId = req.getLong("auctionId");
      Long bidderId = req.getLong("bidderId");
      long maxBid = req.getLong("maxBid");
      long increment = req.getLong("increment");

      walletService.lockForAutoBid(bidderId, auctionId, maxBid);

      AutoBid autoBidEntity = new AutoBid();
      autoBidEntity.setAuctionId(auctionId);
      autoBidEntity.setBidderId(bidderId);
      autoBidEntity.setMaxBid(maxBid);
      autoBidEntity.setIncrement(increment);
      autoBidEntity.setActive(true);
      autoBidEntity.setCreatedAt(LocalDateTime.now());
      autoBidDao.save(autoBidEntity);

      AutoBidStrategy strategy =
          new AutoBidStrategy(bidderId, maxBid, increment, LocalDateTime.now());
      AuctionManager.getInstance().registerAutoBid(auctionId, strategy);

      JSONObject activatedPush = new JSONObject();
      activatedPush.put("type", "AUTO_BID_ACTIVATED");
      activatedPush.put("auctionId", auctionId);
      activatedPush.put("userId", bidderId);
      broadcaster.sendToUser(bidderId, activatedPush.toString());

      System.out.println(
          "REGISTER AUTOBID: auction #"
              + auctionId
              + " | bidder #"
              + bidderId
              + " | maxBid="
              + maxBid);

      BidInfo winnerBeforeAuto = AuctionManager.getInstance().getPreviousWinner(auctionId);
      List<BidTransaction> autoBids = AuctionManager.getInstance().resolveAutoBids(auctionId);

      if (!autoBids.isEmpty()) {
        BidInfo winnerAfterAuto = AuctionManager.getInstance().getPreviousWinner(auctionId);
        if (winnerBeforeAuto != null && !winnerBeforeAuto.isAutoBid) {
          if (winnerAfterAuto == null
              || !winnerAfterAuto.bidderId.equals(winnerBeforeAuto.bidderId)) {
            walletService.releaseForOutbid(
                winnerBeforeAuto.bidderId, auctionId, winnerBeforeAuto.amount);

            JSONObject outbidPush = new JSONObject();
            outbidPush.put("type", "OUTBID");
            outbidPush.put("auctionId", auctionId);
            outbidPush.put("userId", winnerBeforeAuto.bidderId);
            broadcaster.sendToUser(winnerBeforeAuto.bidderId, outbidPush.toString());
          }
        }

        Auction auction = AuctionManager.getInstance().findById(auctionId).orElse(null);
        if (auction != null) {
          for (BidTransaction autoBid : autoBids) {
            bidTransactionDao.save(autoBid);
            auctionDao.update(auction);

            System.out.println(
                "AUTO-BID (Immediate): auction #"
                    + auctionId
                    + " | bidder #"
                    + autoBid.getBidderId()
                    + " | "
                    + autoBid.getAmount()
                    + " VND");
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

  public String cancelAutoBid(JSONObject req) throws Exception {
    try {
      Long auctionId = req.getLong("auctionId");
      Long bidderId = req.getLong("bidderId");

      Optional<AutoBid> opt = autoBidDao.findByAuctionAndBidder(auctionId, bidderId);
      if (opt.isPresent()) {
        autoBidDao.deactivate(opt.get().getId());
      }

      List<AutoBidStrategy> strategies = AuctionManager.getInstance().getAutoBids(auctionId);
      AutoBidStrategy userStrategy = null;
      for (AutoBidStrategy s : strategies) {
        if (s.getUserId().equals(bidderId)) {
          userStrategy = s;
          break;
        }
      }

      if (userStrategy != null) {
        long lockedAmount = userStrategy.getMaxBid();

        BidInfo currentWinner = AuctionManager.getInstance().getPreviousWinner(auctionId);
        if (currentWinner != null && currentWinner.bidderId.equals(bidderId)) {
          lockedAmount -= currentWinner.amount;
        }

        AuctionManager.getInstance().removeAutoBid(auctionId, bidderId);
        walletService.releaseAutoBid(bidderId, auctionId, lockedAmount);

        JSONObject cancelledPush = new JSONObject();
        cancelledPush.put("type", "AUTO_BID_CANCELLED");
        cancelledPush.put("auctionId", auctionId);
        cancelledPush.put("userId", bidderId);
        broadcaster.sendToUser(bidderId, cancelledPush.toString());
      }

      JSONObject res = new JSONObject();
      res.put("status", "OK");
      return res.toString();
    } catch (Exception e) {
      e.printStackTrace();
      System.out.println("CANCEL AUTOBID REJECTED: " + e.getMessage());
      return JsonResponses.error(e.getMessage());
    }
  }
}
