package com.auction.server.model.exception;

// Được throw khi đặt giá cho một auction không ở trạng thái RUNNING
public class AuctionClosedException extends AuctionException {
  public AuctionClosedException(String message) {
    super(message);
  }
}
