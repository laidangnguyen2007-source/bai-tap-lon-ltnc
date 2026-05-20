package server.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import server.dao.AuctionDao;
import server.model.entity.Auction;
import server.model.enums.AuctionStatus;

/**
 * Đồng bộ trạng thái phiên với <b>đồng hồ thực</b>.
 *
 * <p>Khi tạo phiên trước giờ mở, server ghi {@link AuctionStatus#OPEN}. Đến {@code startTime} không
 * có tiến trình nền tự chuyển sang {@link AuctionStatus#RUNNING} — client vẫn thấy OPEN / “Chưa bắt
 * đầu” dù đã quá giờ. Lớp này cập nhật DB + {@link AuctionManager} theo {@link LocalDateTime#now()}
 * (lazy, mỗi lần đọc danh sách hoặc đặt giá).
 */
public final class AuctionStatusSynchronizer {

  private AuctionStatusSynchronizer() {}

  /**
   * Cập nhật mọi phiên {@code OPEN}/{@code RUNNING} cho khớp thời điểm hiện tại.
   *
   * @param auctionDao DAO để đọc phiên “còn sống” và ghi {@code update}
   */
  public static void syncWithClock(AuctionDao auctionDao, WalletService walletService, server.net.ClientBroadcaster broadcaster) {
    LocalDateTime now = LocalDateTime.now();
    List<Auction> active = auctionDao.findActiveAuctions();
    for (Auction a : active) {
      AuctionStatus s = a.getStatus();
      if (s == AuctionStatus.OPEN) {
        if (!now.isBefore(a.getEndTime())) {
          a.setStatus(AuctionStatus.FINISHED);
          auctionDao.update(a);
          if (walletService != null) {
            walletService.settleAuction(a, AuctionManager.getInstance().getAutoBids(a.getId()));
            notifySettlement(a, broadcaster);
          }
        } else if (!now.isBefore(a.getStartTime())) {
          a.setStatus(AuctionStatus.RUNNING);
          auctionDao.update(a);
          AuctionManager.getInstance().restoreRunningAuction(a);
        }
      } else if (s == AuctionStatus.RUNNING) {
        if (!now.isBefore(a.getEndTime())) {
          Optional<Auction> fromRam = AuctionManager.getInstance().closeAuction(a.getId());
          if (fromRam.isPresent()) {
            auctionDao.update(fromRam.get());
            if (walletService != null) {
              walletService.settleAuction(fromRam.get(), AuctionManager.getInstance().getAutoBids(a.getId()));
              notifySettlement(fromRam.get(), broadcaster);
            }
          } else {
            a.setStatus(AuctionStatus.FINISHED);
            auctionDao.update(a);
            if (walletService != null) {
              walletService.settleAuction(a, AuctionManager.getInstance().getAutoBids(a.getId()));
              notifySettlement(a, broadcaster);
            }
          }
        } else {
          AuctionManager.getInstance().restoreRunningAuction(a);
        }
      }
    }
  }

  private static void notifySettlement(Auction a, server.net.ClientBroadcaster broadcaster) {
    if (broadcaster != null && a.getCurrentWinnerId() != null) {
      org.json.JSONObject winPush = new org.json.JSONObject();
      winPush.put("type", "AUCTION_WON");
      winPush.put("auctionId", a.getId());
      broadcaster.sendToUser(a.getCurrentWinnerId(), winPush.toString());

      org.json.JSONObject payoutPush = new org.json.JSONObject();
      payoutPush.put("type", "SELLER_PAYOUT");
      payoutPush.put("auctionId", a.getId());
      broadcaster.sendToUser(a.getSellerId(), payoutPush.toString());
    }
  }
}
