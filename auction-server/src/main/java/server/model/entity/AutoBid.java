package server.model.entity;

import java.time.LocalDateTime;
import server.model.strategy.AutoBidStrategy;

public class AutoBid extends BaseEntity {
  private Long auctionId;
  private Long bidderId;
  private Long maxBid;
  private Long increment;
  private boolean isActive;
  
  public AutoBid() {};
  
  public AutoBid(Long auctionId, Long bidderId, Long maxBid, Long increment) {
    if (maxBid < 0) throw new IllegalArgumentException("maxBid must be > 0");
    if (increment < 0) throw new IllegalArgumentException("increment must be > 0");

    this.auctionId = auctionId;
    this.bidderId = bidderId;
    this.maxBid = maxBid;
    this.increment = increment;
    this.isActive = true;
    this.setCreatedAt(LocalDateTime.now());
  }
  //Strategy -> Entity
  public static AutoBid fromStrategy(Long auctionId, AutoBidStrategy strategy) {
    Long bidderId = strategy.getUserId();
    Long maxBid = strategy.getMaxBid();
    Long increment = strategy.getIncrement();
    return new AutoBid(auctionId, bidderId, maxBid, increment);
  }

  // Entity -> Strategy
  public AutoBidStrategy toStrategy() {
    return new AutoBidStrategy(bidderId, maxBid, increment, getCreatedAt());
  }

  public void deactivate() {
    this.isActive = false;
  }

  public Long getAuctionId() {
    return auctionId;
  }

  public void setAuctionId(Long auctionId) {
    this.auctionId = auctionId;
  }

  public Long getBidderId() {
    return bidderId;
  }

  public void setBidderId(Long bidderId) {
    this.bidderId = bidderId;
  }

  public Long getMaxBid() {
    return maxBid;
  }

  public void setMaxBid(Long maxBid) {
    this.maxBid = maxBid;
  }

  public Long getIncrement() {
    return increment;
  }

  public void setIncrement(Long increment) {
    this.increment = increment;
  }

  public boolean isActive() {
    return isActive;
  }

  public void setActive(boolean isActive) {
    this.isActive = isActive;
  }

  
}
