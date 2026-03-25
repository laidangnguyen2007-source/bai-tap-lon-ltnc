package com.auction.server.model.entity;

import java.time.LocalDateTime;
import java.util.Objects;

public class BidTransaction extends BaseEntity {

  private final Long auctionId;
  private final Long bidderId;
  private final double amount;
  private final LocalDateTime timestamp;

  public BidTransaction(Long auctionId, Long bidderId, double amount) {
    super();
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