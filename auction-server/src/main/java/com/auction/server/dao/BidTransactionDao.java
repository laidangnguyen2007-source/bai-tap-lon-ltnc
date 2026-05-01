// Interface DAO cho BidTransaction (lượt đặt giá)
// Hỗ trợ truy vấn lịch sử đặt giá theo phiên và theo người dùng
// Dữ liệu từ đây sẽ được dùng để vẽ biểu đồ giá realtime (Line Chart)
package com.auction.server.dao;

import com.auction.server.model.entity.BidTransaction;
import java.util.List;
import java.util.Optional;

public interface BidTransactionDao extends GenericDao<BidTransaction> {

  // Lấy tất cả lượt đặt giá của 1 phiên đấu giá (dùng cho biểu đồ Line Chart)
  List<BidTransaction> findByAuctionId(Long auctionId);

  // Lấy tất cả lượt đặt giá của 1 bidder (dùng cho lịch sử cá nhân)
  List<BidTransaction> findByBidderId(Long bidderId);

  // Lấy lượt đặt giá cao nhất (mới nhất) của 1 phiên đấu giá
  Optional<BidTransaction> findHighestBidByAuctionId(Long auctionId);

  // Đếm số lượt đặt giá trong 1 phiên
  long countByAuctionId(Long auctionId);

  // Xóa toàn bộ lịch sử đặt giá của 1 phiên (dùng để Reset phiên đấu giá)
  void deleteByAuctionId(Long auctionId);
}
