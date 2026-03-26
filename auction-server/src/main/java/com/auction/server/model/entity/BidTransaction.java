// hàm này lưu trữ bản ghi cho mỗi lượt đặt giá trong hệ thống
// dữ liệu này có thể dùng để vẽ biểu đồ theo thời gian thực trong BiddingRoomController
package com.auction.server.model.entity;

import java.time.LocalDateTime;
import java.util.Objects;

public class BidTransaction extends BaseEntity {

  // dùng final để đảm bảo BidTransaction là bất biến sau khi khởi tạo
  private final Long auctionId; // id phiên đấu giá
  private final Long bidderId; // id người đấu giá
  private final double amount; // số tiền đặt giá
  private final LocalDateTime timestamp; // thời điểm đặt giá được ghi nhận trên sever

  public BidTransaction(Long auctionId, Long bidderId, double amount) {
    super(); // khởi tạo rỗng của lớp cha BaseEntity
    this.auctionId = Objects.requireNonNull(auctionId, "auctionId must not be null");
    this.bidderId = Objects.requireNonNull(bidderId, "bidderId must not be null");
    if (amount <= 0) {
      throw new IllegalArgumentException("amount must be positive: " + amount);
    }
    this.amount = amount;
    this.timestamp = LocalDateTime.now();
  }

  public BidTransaction(Long id, LocalDateTime createdAt, Long auctionId, Long bidderId, double amount,
      LocalDateTime timestamp) {
    super(id, createdAt);
    this.auctionId = auctionId;
    this.bidderId = bidderId;
    this.amount = amount;
    this.timestamp = timestamp;
  }

  public Long getAuctionId() {
    return auctionId;
  }

  public Long getBidderId() {
    return bidderId;
  }

  public double getAmount() {
    return amount;
  }
  // không có các hàm setter để đảm bảo tính bất biến

  public LocalDateTime getTimestamp() {
    return timestamp;
  }

  @Override
  public String toString() {
    return "BidTransaction{"
        + "id=" + getId()
        + ", auctionId=" + auctionId
        + ", bidderId=" + bidderId
        + ", amount=" + amount
        + ", timestamp=" + timestamp
        + "}";
  }
}