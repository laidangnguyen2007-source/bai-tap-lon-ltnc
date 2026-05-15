// đây là entity tổng hợp cốt lõi đại diện cho 1 phiên đấu giá trên hệ thống
package com.auction.server.model.entity;

import com.auction.server.model.enums.AuctionStatus;
import java.time.LocalDateTime;
import java.util.Objects;

public class Auction extends BaseEntity {

  // Anti-sniping logic (logic chống đặt giá giây cuối): nếu một lượt đặt giá được
  // thực hiện trong vòng ANTI_SNIPE_WINDOW_SECONDS giây trước khi kết thúc, thời
  // gian kết thúc sẽ tự động được gia hạn thêm EXTENSION_SECONDS giây. Logic này
  // được xử lý ở tầng AuctionServiceImpl.

  // số giây trước khi kết thúc đấu giá để kích hoạt anti-snipping
  public static final int ANTI_SNIPE_WINDOW_SECONDS = 30;

  // số giây gia hạn thêm khi anti-snipping kích hoạt
  public static final int EXTENSION_SECONDS = 30;

  private long startingPrice;
  private Long itemId;
  private Long sellerId;
  private long currentPrice; // giá cao nhất được đặt hiện tại
  private Long currentWinnerId; // id của Bidder dẫn đầu, gán null nếu chưa có ai
  private AuctionStatus status; // trạng thái vòng đời của phiên đấu giá
  private LocalDateTime startTime;
  private LocalDateTime endTime;
  private String itemName; // Tên sản phẩm (để hiển thị ở danh sách)
  private String itemCategory; // Loại sản phẩm (để hiển thị ở bảng Seller Dashboard)
  private String imageBase64; // Hình ảnh (hiển thị UI mới)
  private String itemDescription; // Mô tả sản phẩm (hiển thị UI mới)
  private long minBidStep; // Bước giá tối thiểu

  public Auction() {
    super();
  }

  public Auction(Long itemId, Long sellerId, LocalDateTime startTime, LocalDateTime endTime) {
    super();
    this.itemId = Objects.requireNonNull(itemId, "itemId must not be null");
    this.sellerId = Objects.requireNonNull(sellerId, "sellerId must not be null");
    this.currentPrice = 0L;
    this.currentWinnerId = null;
    this.status = AuctionStatus.OPEN;
    this.startTime = Objects.requireNonNull(startTime, "startTime must not be null");
    this.endTime = Objects.requireNonNull(endTime, "endTime must not be null");

    // endTime phải ở sau startTime
    if (!endTime.isAfter(startTime)) {
      throw new IllegalArgumentException("endTime must be after startTime");
    }
  }

  public Auction(
      Long id,
      LocalDateTime createdAt,
      Long itemId,
      Long sellerId,
      long currentPrice,
      Long currentWinnerId,
      AuctionStatus status,
      LocalDateTime startTime,
      LocalDateTime endTime,
      long minBidStep) {
    super(id, createdAt);
    this.itemId = itemId;
    this.sellerId = sellerId;
    this.currentPrice = currentPrice;
    this.currentWinnerId = currentWinnerId;
    this.status = status;
    this.startTime = startTime;
    this.endTime = endTime;
    this.minBidStep = minBidStep;
  }

  // kiểm tra xem phiên đấu giá có đang ở trạng thái Running không
  public boolean isRunning() {
    return AuctionStatus.RUNNING.equals(status);
  }

  // kiểm tra xem phiên đấu giá có đang ở trạng thái Kết thúc không
  public boolean isFinished() {
    return AuctionStatus.FINISHED.equals(status);
  }

  // kiểm tra xem phiên đấu giá đã được thanh toán chưa
  public boolean isPaid() {
    return AuctionStatus.PAID.equals(status);
  }

  // kiểm tra xem phiên đấu giá có bị hủy không
  public boolean isCanceled() {
    return AuctionStatus.CANCELED.equals(status);
  }

  // cập nhật giá hiện tại và id người dẫn đầu sau 1 lượt đặt giá hợp lệ
  public void applyBid(long newPrice, Long bidderId) {
    if (!isRunning()) {
      throw new IllegalStateException(
          "Cannot place bid: auction is not RUNNING. Current status: " + status);
    }
    if (newPrice <= currentPrice) {
      throw new IllegalArgumentException(
          "New bid must be higher than current price. Current: "
              + currentPrice
              + ", offered: "
              + newPrice);
    }
    this.currentPrice = newPrice;
    this.currentWinnerId = Objects.requireNonNull(bidderId, "bidderId must not be null");
  }

  public void extendEndTime(int extraSeconds) {
    if (extraSeconds <= 0) {
      throw new IllegalArgumentException("extraSeconds must be positive: " + extraSeconds);
    }
    this.endTime = this.endTime.plusSeconds(extraSeconds); // plusSeconds() là phương thức built-in
    // của LocalDateTime
  }

  public Long getItemId() {
    return itemId;
  }

  public void setItemId(Long itemId) {
    this.itemId = itemId;
  }

  public Long getSellerId() {
    return sellerId;
  }

  public void setSellerId(Long sellerId) {
    this.sellerId = sellerId;
  }

  public long getCurrentPrice() {
    return currentPrice;
  }

  public long getStartingPrice() {
    return startingPrice;
  }

  public void setStartingPrice(long startingPrice) {
    this.startingPrice = startingPrice;
  }

  public void setCurrentPrice(long currentPrice) {
    this.currentPrice = currentPrice;
  }

  public Long getCurrentWinnerId() {
    return currentWinnerId;
  }

  public void setCurrentWinnerId(Long currentWinnerId) {
    this.currentWinnerId = currentWinnerId;
  }

  public AuctionStatus getStatus() {
    return status;
  }

  public void setStatus(AuctionStatus status) {
    this.status = Objects.requireNonNull(status, "status must not be null");
  }

  public LocalDateTime getStartTime() {
    return startTime;
  }

  public void setStartTime(LocalDateTime startTime) {
    this.startTime = startTime;
  }

  public LocalDateTime getEndTime() {
    return endTime;
  }

  public void setEndTime(LocalDateTime endTime) {
    this.endTime = endTime;
  }

  public String getItemName() {
    return itemName;
  }

  public void setItemName(String itemName) {
    this.itemName = itemName;
  }

  // Loại sản phẩm (transient — chỉ dùng để hiển thị, không lưu trong bảng auctions)
  public String getItemCategory() {
    return itemCategory;
  }

  public void setItemCategory(String itemCategory) {
    this.itemCategory = itemCategory;
  }

  public String getImageBase64() {
    return imageBase64;
  }

  public void setImageBase64(String imageBase64) {
    this.imageBase64 = imageBase64;
  }

  public String getItemDescription() {
    return itemDescription;
  }

  public void setItemDescription(String itemDescription) {
    this.itemDescription = itemDescription;
  }

  public long getMinBidStep() {
    return minBidStep;
  }

  public void setMinBidStep(long minBidStep) {
    this.minBidStep = minBidStep;
  }

  @Override
  public String toString() {
    return "Auction{"
        + "id="
        + getId()
        + ", itemId="
        + itemId
        + ", sellerId="
        + sellerId
        + ", currentPrice="
        + currentPrice
        + ", currentWinnerId="
        + currentWinnerId
        + ", status="
        + status
        + ", startTime="
        + startTime
        + ", endTime="
        + endTime
        + ", createdAt="
        + getCreatedAt()
        + "}";
  }
}
