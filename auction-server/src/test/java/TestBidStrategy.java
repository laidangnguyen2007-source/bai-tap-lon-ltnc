import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import server.model.entity.Auction;
import server.model.entity.BidTransaction;
import server.model.enums.AuctionStatus;
import server.model.exception.AuctionClosedException;
import server.model.exception.InvalidBidException;
import server.model.strategy.AutoBidStrategy;
import server.model.strategy.ManualBidStrategy;

class TestBidStrategy {

  /** Tạo một Auction đang RUNNING với giá hiện tại cho trước. */
  private Auction createRunningAuction(long currentPrice) {
    Auction auction = new Auction(1L, 1L, LocalDateTime.now(), LocalDateTime.now().plusHours(1));
    auction.setId(1L);
    auction.setStatus(AuctionStatus.RUNNING);
    if (currentPrice > 0) {
      auction.applyBid(currentPrice, 99L);
    }
    return auction;
  }

  // ===== MANUAL BID STRATEGY =====

  @Test
  @DisplayName("ManualBidStrategy: chấp nhận bid hợp lệ")
  void testManualBidValid() {
    ManualBidStrategy strategy = new ManualBidStrategy(100);
    Auction auction = createRunningAuction(1000);

    BidTransaction tx = strategy.calculateBid(auction, 2L, 1200L);

    assertNotNull(tx, "Bid hợp lệ phải trả về BidTransaction");
    assertEquals(1200L, tx.getAmount());
    assertEquals(2L, tx.getBidderId());
  }

  @Test
  @DisplayName("ManualBidStrategy: từ chối bid thấp hơn giá hiện tại")
  void testManualBidRejectLow() {
    ManualBidStrategy strategy = new ManualBidStrategy(100);
    Auction auction = createRunningAuction(1000);

    assertThrows(
        InvalidBidException.class,
        () -> strategy.calculateBid(auction, 2L, 500L),
        "Bid thấp hơn giá hiện tại phải bị từ chối");
  }

  @Test
  @DisplayName("ManualBidStrategy: từ chối bid không đạt bước giá tối thiểu")
  void testManualBidRejectBelowIncrement() {
    ManualBidStrategy strategy = new ManualBidStrategy(500); // bước giá tối thiểu 500
    Auction auction = createRunningAuction(1000);

    // 1200 < 1000 + 500 = 1500, nên phải bị từ chối
    assertThrows(
        InvalidBidException.class,
        () -> strategy.calculateBid(auction, 2L, 1200L),
        "Bid không đạt minimum increment phải bị từ chối");
  }

  @Test
  @DisplayName("ManualBidStrategy: từ chối khi phiên đã đóng")
  void testManualBidRejectClosed() {
    ManualBidStrategy strategy = new ManualBidStrategy();
    Auction auction = createRunningAuction(1000);
    auction.setStatus(AuctionStatus.FINISHED);

    assertThrows(
        AuctionClosedException.class,
        () -> strategy.calculateBid(auction, 2L, 2000L),
        "Bid trên phiên đã FINISHED phải bị từ chối");
  }

  @Test
  @DisplayName("ManualBidStrategy: getStrategyName() trả về MANUAL")
  void testManualBidStrategyName() {
    ManualBidStrategy strategy = new ManualBidStrategy();
    assertEquals("MANUAL", strategy.getStrategyName());
  }

  // ===== AUTO BID STRATEGY =====

  @Test
  @DisplayName("AutoBidStrategy: tính toán bid tiếp theo đúng")
  void testAutoBidCalculation() {
    AutoBidStrategy strategy = new AutoBidStrategy(2L, 5000L, 500L, LocalDateTime.now());
    Auction auction = createRunningAuction(1000);

    BidTransaction tx = strategy.calculateBid(auction, 2L, 0);

    assertNotNull(tx, "AutoBid hợp lệ phải trả về BidTransaction");
    assertEquals(1500L, tx.getAmount(), "nextBid = min(1000 + 500, 5000) = 1500");
  }

  @Test
  @DisplayName("AutoBidStrategy: không vượt quá maxBid")
  void testAutoBidMaxBidCap() {
    AutoBidStrategy strategy = new AutoBidStrategy(2L, 1200L, 500L, LocalDateTime.now());
    Auction auction = createRunningAuction(1000);

    BidTransaction tx = strategy.calculateBid(auction, 2L, 0);

    assertNotNull(tx);
    assertEquals(1200L, tx.getAmount(), "nextBid = min(1000 + 500, 1200) = 1200");
  }

  @Test
  @DisplayName("AutoBidStrategy: không đặt giá khi đã dẫn đầu")
  void testAutoBidSkipWhenLeading() {
    AutoBidStrategy strategy = new AutoBidStrategy(99L, 5000L, 500L, LocalDateTime.now());
    Auction auction = createRunningAuction(1000); // currentWinnerId = 99L

    BidTransaction tx = strategy.calculateBid(auction, 99L, 0);

    assertNull(tx, "Không nên đặt giá khi đã đang dẫn đầu");
  }

  @Test
  @DisplayName("AutoBidStrategy: từ chối khi giá hiện tại >= maxBid")
  void testAutoBidRejectWhenMaxReached() {
    AutoBidStrategy strategy = new AutoBidStrategy(2L, 1000L, 500L, LocalDateTime.now());
    Auction auction = createRunningAuction(1000);

    assertThrows(
        InvalidBidException.class,
        () -> strategy.calculateBid(auction, 2L, 0),
        "Khi giá hiện tại >= maxBid, auto-bid phải từ chối");
  }

  @Test
  @DisplayName("AutoBidStrategy: priority — maxBid cao hơn thắng")
  void testAutoBidPriorityByMaxBid() {
    AutoBidStrategy s1 = new AutoBidStrategy(1L, 5000L, 100L, LocalDateTime.now());
    AutoBidStrategy s2 = new AutoBidStrategy(2L, 3000L, 100L, LocalDateTime.now());

    assertTrue(s1.compareTo(s2) < 0, "Người có maxBid cao hơn phải được ưu tiên");
    assertFalse(s2.compareTo(s1) < 0);
  }

  @Test
  @DisplayName("AutoBidStrategy: priority — cùng maxBid thì ưu tiên đăng ký trước")
  void testAutoBidPriorityByTime() {
    LocalDateTime earlier = LocalDateTime.of(2026, 5, 1, 10, 0);
    LocalDateTime later = LocalDateTime.of(2026, 5, 1, 11, 0);

    AutoBidStrategy s1 = new AutoBidStrategy(1L, 5000L, 100L, earlier);
    AutoBidStrategy s2 = new AutoBidStrategy(2L, 5000L, 100L, later);

    assertTrue(s1.compareTo(s2) < 0, "Cùng maxBid → người đăng ký trước được ưu tiên");
    assertFalse(s2.compareTo(s1) < 0);
  }

  @Test
  @DisplayName("AutoBidStrategy: getStrategyName() trả về AUTOBID")
  void testAutoBidStrategyName() {
    AutoBidStrategy strategy = new AutoBidStrategy(1L, 5000L, 100L, LocalDateTime.now());
    assertEquals("AUTOBID", strategy.getStrategyName());
  }

  @Test
  @DisplayName("AutoBidStrategy: từ chối increment <= 0")
  void testAutoBidRejectInvalidIncrement() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new AutoBidStrategy(1L, 5000L, 0L, LocalDateTime.now()),
        "increment = 0 phải bị từ chối");
  }
}
