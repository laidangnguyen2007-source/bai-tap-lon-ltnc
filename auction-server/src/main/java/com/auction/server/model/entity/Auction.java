package com.auction.server.model.entity;

import com.auction.server.model.enums.AuctionStatus;
import java.time.LocalDateTime;
import java.util.Objects;

public class Auction extends BaseEntity {

  public static final int ANTI_SNIPE_WINDOW_SECONDS = 30;
  public static final int EXTENSION_SECONDS = 30;

  private Long itemId;
  private Long sellerId;
  private double currentPrice;
  private Long currentWinnerId;
  private AuctionStatus status;
  private LocalDateTime startTime;
  private LocalDateTime endTime;

  public Auction() {
    super();
  }

  public Auction(Long itemId, Long sellerId, LocalDateTime startTime, LocalDateTime endTime) {
    super();
    this.itemId = Objects.requireNonNull(itemId, "itemId must not be null");
    this.sellerId = Objects.requireNonNull(sellerId, "sellerId must not be null");
    this.currentPrice = 0.0;
    this.currentWinnerId = null;
    this.status = AuctionStatus.OPEN;
    this.startTime = Objects.requireNonNull(startTime, "startTime must not be null");
    this.endTime = Objects.requireNonNull(endTime, "endTime must not be null");
    if (!endTime.isAfter(startTime)) {
      throw new IllegalArgumentException("endTime must be after startTime");
    }
  }

  public Auction(Long id, LocalDateTime createdAt, Long itemId, Long sellerId, double currentPrice,
      Long currentWinnerId, AuctionStatus status, LocalDateTime startTime, LocalDateTime endTime) {
    super(id, createdAt);
    this.itemId = itemId;
    this.sellerId = sellerId;
    this.currentPrice = currentPrice;
    this.currentWinnerId = currentWinnerId;
    this.status = status;
    this.startTime = startTime;
    this.endTime = endTime;
  }

  public boolean isRunning() {
    return AuctionStatus.RUNNING.equals(status);
  }

  public boolean isFinished() {
    return AuctionStatus.FINISHED.equals(status);
  }

  public void applyBid(double newPrice, Long bidderId) {
    if (!isRunning()) {
      throw new IllegalStateException("Cannot place bid: auction is not RUNNING. Current status: " + status);
    }
    if (newPrice <= currentPrice) {
      throw new IllegalArgumentException(
          "New bid must be higher than current price. Current: " + currentPrice + ", offered: " + newPrice);
    }
    this.currentPrice = newPrice;
    this.currentWinnerId = Objects.requireNonNull(bidderId, "bidderId must not be null");
  }

  public void extendEndTime(int extraSeconds) {
    if (extraSeconds <= 0) {
      throw new IllegalArgumentException("extraSeconds must be positive: " + extraSeconds);
    }
    this.endTime = this.endTime.plusSeconds(extraSeconds);
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

  public double getCurrentPrice() {
    return currentPrice;
  }

  public void setCurrentPrice(double currentPrice) {
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

  @Override
  public String toString() {
    return "Auction{"
        + "id=" + getId()
        + ", itemId=" + itemId
        + ", sellerId=" + sellerId
        + ", currentPrice=" + currentPrice
        + ", currentWinnerId=" + currentWinnerId
        + ", status=" + status
        + ", startTime=" + startTime
        + ", endTime=" + endTime
        + ", createdAt=" + getCreatedAt()
        + "}";
  }
}