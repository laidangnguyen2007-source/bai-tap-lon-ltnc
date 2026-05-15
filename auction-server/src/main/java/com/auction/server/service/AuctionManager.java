package com.auction.server.service;

import com.auction.server.model.entity.Auction;
import com.auction.server.model.entity.BidTransaction;
import com.auction.server.model.enums.AuctionStatus;
import com.auction.server.model.exception.AuctionClosedException;
import com.auction.server.model.exception.AuctionException;
import com.auction.server.model.exception.InvalidBidException;
import com.auction.server.model.strategy.AutoBidStrategy;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/*
 * Singleton Pattern — Người điều hành toàn bộ phiên đấu giá đang hoạt động. Chỉ tồn tại duy nhất 1
 * instance trong suốt vòng đời Server. Nhiệm vụ: 1. Lưu trữ các phiên đấu giá đang chạy (in-memory
 * cache) . 2. Là điểm tập trung để đặt giá, mở phiên, đóng phiên. 3. Phối hợp với AuctionService
 * (tầng DAO) để ghi kết quả xuống Database.
 *
 * ConcurrentHashMap dùng khóa phân mảnh (Bucket-level locking) thay cho HashMap thông thường để đảm
 * bảo Thread-safe khi nhiều Client đồng thời truy vấn và cập nhật danh sách phiên đấu giá.
 */
public class AuctionManager {

  // Double-Checked Locking — chống Race Condition lúc khởi tạo
  private static volatile AuctionManager instance;

  // Bộ nhớ đệm (in-memory) chứa các phiên đang RUNNING
  private final Map<Long, Auction> activeAuctions = new ConcurrentHashMap<>();

  // Bộ nhớ đệm chứa các chiến lược AutoBid cho từng phiên
  private final Map<Long, List<AutoBidStrategy>> autoBids = new ConcurrentHashMap<>();

  // final đảm bảo bộ nhớ chỉ cấp 1 khung Map duy nhất, chống mất bộ nhớ gây sập server

  private AuctionManager() {}

  public static AuctionManager getInstance() {
    AuctionManager result = instance;
    if (result == null) {
      synchronized (AuctionManager.class) {
        result = instance;
        if (result == null) {
          instance = result = new AuctionManager();
        }
      }
    }
    return result;
  }

  /*
   * Khai mạc phiên đấu giá: chuyển sang RUNNING và đưa vào bộ nhớ đệm. Phương thức này sẽ được
   * AuctionService gọi sau khi đã lưu vào Database.
   */
  public synchronized Auction openAuction(Auction auction) {
    if (!AuctionStatus.OPEN.equals(auction.getStatus())) {
      throw new AuctionException("Chỉ phiên ở trạng thái OPEN mới có thể chuyển sang RUNNING");
    }
    auction.setStatus(AuctionStatus.RUNNING);
    activeAuctions.put(auction.getId(), auction); // thêm phiên vào ConcurrentHashMap trên RAM
    return auction;
  }

  /*
   * Khôi phục phiên đấu giá đang chạy từ Database vào bộ nhớ đệm (gọi khi Server khởi động).
   */
  public synchronized void restoreRunningAuction(Auction auction) {
    if (AuctionStatus.RUNNING.equals(auction.getStatus())) {
      activeAuctions.put(auction.getId(), auction);
    }
  }

  /*
   * Đặt giá vào phiên đấu giá. Bao gồm kiểm tra hợp lệ và logic Anti-sniping. Dùng synchronized
   * trên auction cụ thể để tránh Race Condition khi nhiều Bidder đặt giá cùng lúc vào cùng một
   * phiên.
   */
  public Auction placeBid(Long auctionId, Long bidderId, long bidPrice) {
    Auction auction = findActiveAuction(auctionId);

    synchronized (auction) {
      // Kiểm tra phiên còn đang chạy không (có thể đã kết thúc trong lúc đợi lock (TOCTOU))
      if (!auction.isRunning()) {
        throw new AuctionClosedException("Phiên đấu giá #" + auctionId + " đã đóng");
      }

      // Kiểm tra thời gian còn hạn không
      if (LocalDateTime.now().isAfter(auction.getEndTime())) {
        auction.setStatus(AuctionStatus.FINISHED);
        activeAuctions.remove(auctionId);
        throw new AuctionClosedException("Phiên đấu giá #" + auctionId + " đã hết thời gian");
      }

      // Kiểm tra giá đặt hợp lệ
      if (bidPrice <= auction.getCurrentPrice()) {
        throw new InvalidBidException(
            "Giá đặt " + bidPrice + " phải cao hơn giá hiện tại " + auction.getCurrentPrice());
      }

      // Áp dụng Anti-sniping: nếu đặt giá trong khoảng cuối thì gia hạn thêm
      long secondsLeft =
          java.time.Duration.between(LocalDateTime.now(), auction.getEndTime()).getSeconds();
      if (secondsLeft <= Auction.ANTI_SNIPE_WINDOW_SECONDS) {
        auction.extendEndTime(Auction.EXTENSION_SECONDS);
      }

      // Cập nhật giá và người dẫn đầu
      auction.applyBid(bidPrice, bidderId);
    }

    return auction;
  }

  public synchronized void registerAutoBid(Long auctionId, AutoBidStrategy strategy) {
    Auction auction = findActiveAuction(auctionId);
    autoBids.computeIfAbsent(auctionId, k -> new CopyOnWriteArrayList<>()).add(strategy);
  }

  public List<BidTransaction> resolveAutoBids(Long auctionId) {
    Auction auction = findActiveAuction(auctionId);
    List<BidTransaction> autoTransactions = new ArrayList<>();
    synchronized (auction) {
      boolean newBidPlaced;
      do {
        newBidPlaced = false;
        List<AutoBidStrategy> strategies =
            autoBids.getOrDefault(auctionId, Collections.emptyList());

        AutoBidStrategy bestStrategy = null;
        BidTransaction bestTx = null;

        for (AutoBidStrategy strategy : strategies) {
          try {
            BidTransaction tx = strategy.calculateBid(auction, strategy.getUserId(), 0);
            if (tx != null) {
              if (bestStrategy == null || strategy.hasPriorityOver(bestStrategy)) {
                bestStrategy = strategy;
                bestTx = tx;
              }
            }
          } catch (Exception e) {
            // ignore invalid bid
          }
        }

        if (bestTx != null) {
          auction.applyBid(bestTx.getAmount(), bestTx.getBidderId());
          autoTransactions.add(bestTx);
          newBidPlaced = true;
        }
      } while (newBidPlaced);
    }
    return autoTransactions;
  }

  // ─── ĐÓNG PHIÊN ─────────────────────────────────────────────────

  /*
   * Kết thúc phiên đấu giá, xác định người thắng. Phương thức này sẽ được Scheduler hoặc Server gọi
   * khi hết giờ.
   */
  public synchronized Optional<Auction> closeAuction(Long auctionId) {
    Auction auction = activeAuctions.remove(auctionId);
    if (auction == null) {
      return Optional.empty();
    }
    auction.setStatus(AuctionStatus.FINISHED);
    return Optional.of(auction);
  }

  // ─── TRUY VẤN ────────────────────────────────────────────────────

  public Optional<Auction> findById(Long auctionId) {
    return Optional.ofNullable(activeAuctions.get(auctionId));
    // hàm ofNullable(value) sẽ trả về Optional.of(value) nếu có giá trị, còn nếu là null
    // thì sẽ ném ra Optional.empty()
  }

  public Map<Long, Auction> getAllActive() {
    return Collections.unmodifiableMap(activeAuctions);
    // trả về bản "chỉ đọc" để tránh thay đổi từ bên ngoài
  }

  // ─── INTERNAL ────────────────────────────────────────────────────

  private Auction findActiveAuction(Long auctionId) {
    return Optional.ofNullable(activeAuctions.get(auctionId))
        .orElseThrow(
            () ->
                new AuctionException("Không tìm thấy phiên đấu giá đang hoạt động: #" + auctionId));
  }
}
