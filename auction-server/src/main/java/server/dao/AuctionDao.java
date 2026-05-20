// Interface DAO cho Auction (phiên đấu giá)
// Hỗ trợ tìm kiếm theo trạng thái, item, seller để phục vụ các màn hình khác nhau
package server.dao;

import java.util.List;
import java.util.Optional;
import server.model.entity.Auction;
import server.model.enums.AuctionStatus;

public interface AuctionDao extends GenericDao<Auction> {

  // Lấy tất cả phiên đấu giá theo trạng thái (VD: lấy hết phiên đang RUNNING)
  List<Auction> findByStatus(AuctionStatus status);

  // Tìm phiên đấu giá theo sản phẩm
  Optional<Auction> findByItemId(Long itemId);

  // Lấy tất cả phiên đấu giá của 1 seller
  List<Auction> findBySellerId(Long sellerId);

  // Lấy tất cả phiên đang chạy (RUNNING) — dùng cho màn hình danh sách đấu giá
  List<Auction> findRunningAuctions();

  // Lấy tất cả phiên đang mở hoặc đang chạy (OPEN hoặc RUNNING)
  List<Auction> findActiveAuctions();
}
