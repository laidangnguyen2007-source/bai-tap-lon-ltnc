package com.auction.client.observer;

import org.json.simple.JSONObject;
import server.model.entity.BidTransaction;

/**
 * Interface Observer dành cho phía Client.
 *
 * <p>Đây là "hợp đồng" giữa Thành viên 3 (NetworkLayer) và Thành viên 4 (GUI). Thành viên 3 sẽ gọi
 * phương thức {@link #onBidUpdated} mỗi khi nhận được thông báo đặt giá mới từ Server. Thành viên 4
 * implement interface này trong BiddingRoomController để cập nhật UI tự động.
 *
 * <p>Lưu ý quan trọng về thread safety: phương thức này có thể được gọi từ background network
 * thread, do đó mọi thao tác cập nhật UI PHẢI được bọc trong Platform.runLater().
 */
public interface AuctionObserver {

  /**
   * Được gọi khi có một lượt đặt giá mới được xác nhận từ Server.
   *
   * @param bid thông tin giao dịch đặt giá vừa được server xác nhận (không null)
   */
  void onBidUpdated(BidTransaction bid);

  /**
   * Được gọi khi trạng thái phiên đấu giá thay đổi (ví dụ: RUNNING → FINISHED).
   *
   * @param auctionId ID của phiên bị thay đổi
   * @param newStatus chuỗi trạng thái mới (ví dụ: "FINISHED", "CANCELED")
   */
  void onAuctionStatusChanged(Long auctionId, String newStatus);

  /**
   * Được gọi khi có sự kiện liên quan đến ví (FUNDS_LOCKED, OUTBID, AUCTION_WON, v.v.). Default
   * no-op để backward compatible với các observer cũ.
   *
   * @param eventType loại sự kiện (ví dụ: "FUNDS_LOCKED", "OUTBID_NOTIFICATION")
   * @param payload dữ liệu JSON đi kèm sự kiện
   */
  default void onWalletEvent(String eventType, JSONObject payload) {
    // Default no-op — override trong controller cần xử lý
  }

  /**
   * Được gọi khi thời gian kết thúc phiên đấu giá được gia hạn (ví dụ: do Anti-sniping).
   *
   * @param auctionId ID của phiên bị thay đổi
   * @param newEndTime thời gian kết thúc mới (định dạng ISO 8601 chuỗi)
   */
  default void onAuctionTimeExtended(Long auctionId, String newEndTime) {
    // Default no-op
  }
}
