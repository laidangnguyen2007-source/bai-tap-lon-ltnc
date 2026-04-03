package com.auction.server.model.strategy;

import com.auction.server.model.entity.Auction;
import com.auction.server.model.entity.BidTransaction;
import com.auction.server.model.enums.AuctionStatus;
import com.auction.server.model.exception.*;
import com.auction.server.model.exception.InvalidBidException;

/**
 * Manual bidding strategy - chiến lược đấu giá thủ công Người đấu giá xác định rõ số tiền đấu giá
 * Quy tắc xác thực: Phiến đấu giá phải ở trạng thái đang chạy {@code RUNNING} Số tiền yêu cầu phải
 * lớn hơn số tiền đấu giá của phiên Số tiền yêu cầu phải lớn hơn hoặc bằng mức giá tối thiểu của
 * phiên
 */
public class ManualBidStrategy implements BidStrategy {
  // Mức tăng tối thiểu so với giá hiện tại để vượt qua
  private final double minimumIncrement; // Bước giá tối thiểu

  // Tạo ra ManualBidStrategy với mức tăng tùy ý
  // @param minimumIncrement Số tiền tối thiểu mà giá thầu phải cao hơn so với giá hiện tại.
  public ManualBidStrategy(double minimumIncrement) {
    if (minimumIncrement < 0) throw new IllegalArgumentException("minimumIncrement must be >=0!");
    this.minimumIncrement = minimumIncrement;
  }

  // Không yêu cầu mức tăng tối thiểu
  public ManualBidStrategy() {
    this(0.0);
  }

  @Override
  public BidTransaction calculateBid(Auction auction, long bidderId, double requestAmount) {
    validateAuction(auction);
    validateAmount(auction, requestAmount);
    return new BidTransaction(auction.getId(), bidderId, requestAmount);
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

  private void validateAmount(Auction auction, double requestAmount) {
    double currentPrice = auction.getCurrentPrice();
    double requiredMinimum = currentPrice + minimumIncrement;

    // Giá đặt bé hơn giá hiện tại
    if (requestAmount < currentPrice) {
      throw new InvalidBidException(
          String.format(
              "Bid amount %.2f must be greater than current price %.2f",
              requestAmount, currentPrice));
    }

    // Giá đặt bé hơn giá sàn
    if (requestAmount < requiredMinimum) {
      throw new InvalidBidException(
          String.format(
              "Bid amount %.2f doesn't meet minimum increment. Require >= %.2f.",
              requestAmount, requiredMinimum));
    }
  }

  @Override
  public String getStrategyName() {
    return "MANUAL";
  }

  public double getMinimumIncrement() {
    return minimumIncrement;
  }
}
