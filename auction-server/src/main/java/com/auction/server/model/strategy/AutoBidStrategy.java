package com.auction.server.model.strategy;

import com.auction.server.model.entity.Auction;
import com.auction.server.model.entity.BidTransaction;
import com.auction.server.model.enums.AuctionStatus;
import com.auction.server.model.exception.AuctionClosedException;
import com.auction.server.model.exception.InvalidBidException;
import java.time.LocalDateTime;

public class AutoBidStrategy implements BidStrategy {
  public final Long userId;
  public final long maxBid;
  public final long increment;
  public final LocalDateTime registerAt;

  public AutoBidStrategy(Long userId, long maxBid, long increment, LocalDateTime registerAt) {
    if (maxBid < 0) throw new IllegalArgumentException("The maximum price must be greater than 0!");
    if (increment <= 0)
      throw new IllegalArgumentException("The price increment must be greater than 0!");
    this.userId = userId;
    this.maxBid = maxBid;
    this.increment = increment;
    this.registerAt = registerAt;
  }

  @Override
  public BidTransaction calculateBid(Auction auction, long bidderId, long requestAmount) {
    validateAuction(auction);

    long nextBid = Math.min(auction.getCurrentPrice() + increment, maxBid);

    validateAmount(auction, nextBid);
    return new BidTransaction(auction.getId(), bidderId, nextBid);
  }

  // Hàm kiểm tra xem Auction có tồn tại hoặc đang chạy hay không.
  private void validateAuction(Auction auction) {
    if (auction == null) throw new IllegalArgumentException("Auction must not be null!");
    if (auction.getStatus() != AuctionStatus.RUNNING) {
      throw new AuctionClosedException(
          "Auction #"
              + auction.getId()
              + " is not currently running (status ="
              + auction.getStatus()
              + ").");
    }
  }

  private void validateAmount(Auction auction, long nextBid) {

    if (auction.getCurrentPrice() >= maxBid) {
      throw new InvalidBidException("Auto-bid limit reached!");
    }
  }

  @Override
  public String getStrategyName() {
    return "AUTOBID";
  }

  public Long getUserId() {
    return userId;
  }

  public long getMaxBid() {
    return maxBid;
  }

  public long getIncrement() {
    return increment;
  }

  public LocalDateTime getRegisterAt() {
    return registerAt;
  }

  @Override
  public String toString() {
    return String.format("Autobid{user='%d', maxBid = %d , increment = %d, registeredAt = %s");
  }
}
