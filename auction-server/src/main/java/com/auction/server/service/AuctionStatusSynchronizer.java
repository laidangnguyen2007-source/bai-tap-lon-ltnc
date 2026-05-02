package com.auction.server.service;

import com.auction.server.dao.AuctionDao;
import com.auction.server.model.entity.Auction;
import com.auction.server.model.enums.AuctionStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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
  public static void syncWithClock(AuctionDao auctionDao) {
    LocalDateTime now = LocalDateTime.now();
    List<Auction> active = auctionDao.findActiveAuctions();
    for (Auction a : active) {
      AuctionStatus s = a.getStatus();
      if (s == AuctionStatus.OPEN) {
        if (!now.isBefore(a.getEndTime())) {
          a.setStatus(AuctionStatus.FINISHED);
          auctionDao.update(a);
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
          } else {
            a.setStatus(AuctionStatus.FINISHED);
            auctionDao.update(a);
          }
        } else {
          AuctionManager.getInstance().restoreRunningAuction(a);
        }
      }
    }
  }
}
