import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import server.model.entity.BidTransaction;

class TestBidTransaction {

  @Test
  @DisplayName("BidTransaction khởi tạo đúng các giá trị")
  void testBidTransactionCreation() {
    Long auctionId = 10L;
    Long bidderId = 99L;
    long amount = 1200;

    BidTransaction bid = new BidTransaction(auctionId, bidderId, amount);

    assertEquals(10L, bid.getAuctionId());
    assertEquals(99L, bid.getBidderId());
    assertEquals(1200L, bid.getAmount());
    assertNotNull(bid.getTimestamp(), "Timestamp phải được gán tự động");
  }

  @Test
  @DisplayName("BidTransaction từ chối amount <= 0")
  void testBidTransactionRejectZeroAmount() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new BidTransaction(10L, 99L, 0),
        "amount = 0 phải bị từ chối");
  }

  @Test
  @DisplayName("BidTransaction từ chối amount âm")
  void testBidTransactionRejectNegativeAmount() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new BidTransaction(10L, 99L, -500),
        "amount âm phải bị từ chối");
  }

  @Test
  @DisplayName("BidTransaction từ chối null auctionId")
  void testBidTransactionRejectNullAuctionId() {
    assertThrows(
        NullPointerException.class,
        () -> new BidTransaction(null, 99L, 1200),
        "auctionId null phải bị từ chối");
  }

  @Test
  @DisplayName("BidTransaction từ chối null bidderId")
  void testBidTransactionRejectNullBidderId() {
    assertThrows(
        NullPointerException.class,
        () -> new BidTransaction(10L, null, 1200),
        "bidderId null phải bị từ chối");
  }

  @Test
  @DisplayName("BidTransaction toString chứa thông tin đầy đủ")
  void testBidTransactionToString() {
    BidTransaction bid = new BidTransaction(10L, 99L, 1200L);
    String result = bid.toString();

    assertTrue(result.contains("auctionId=10"));
    assertTrue(result.contains("bidderId=99"));
    assertTrue(result.contains("amount=1200"));
  }
}
