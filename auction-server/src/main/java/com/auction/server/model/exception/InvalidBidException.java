package com.auction.server.model.exception;
//Được sử dụng khi bid không hợp lệ (quá thấp, dưới mức giá sau khi tăng, ...)
public class InvalidBidException extends AuctionException {
    public InvalidBidException(String message) {
        super(message);
    }
}
