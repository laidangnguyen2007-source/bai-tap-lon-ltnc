package com.auction.server.model.strategy;

import com.auction.server.model.entity.Auction;
import com.auction.server.model.entity.BidTransaction;

/**
 * Giao diện dành cho các phương pháp đấu giá khác nhau triển khai giúp xác định cách thức xác định
 * số tiền đấu giá
 */
public interface BidStrategy {
  /**
   * Mục đích: Tính toán và trả về mức tiền đấu giá tiếp theo phiên đấu giá tiếp theo
   *
   * @param auction Bối cảnh hiện tại
   * @param bidderId ID của người đặt giá
   * @param requestAmount Số tiền yêu cầu rõ ràng (được sử dụng bởi ManualBidStrategy và có thể bị
   *     bỏ qua bởi AutoBidStrategy)
   * @return một BidTranscation để lưu trữ (vào database), hoặc null nếu không nên đặt giá đầu thầu
   *     nào (xảy ra khi không ai bật autobid, đạt maxbid, đặt bid cao nhất)
   */
  BidTransaction calculateBid(Auction auction, long bidderId, double requestAmount);

  /**
   * @return tên hiển thị (dành cho logging/UI)
   */
  String getStrategyName();
}
