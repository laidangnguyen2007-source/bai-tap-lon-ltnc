import static org.junit.jupiter.api.Assertions.*;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import server.model.entity.Auction;
import server.model.exception.InvalidBidException;

public class TestAuctionException {

  @Test
  void testInvalidBidException() {
    // 1. Test thử cái Exception
    // Xem nó có lưu đúng message mình truyền vào không
    InvalidBidException ex =
        assertThrows(
            InvalidBidException.class,
            () -> {
              throw new InvalidBidException("Giá đặt thấp quá!");
            });

    assertEquals("Giá đặt thấp quá!", ex.getMessage());
    System.out.println("✅ Test InvalidBidException: PASS");
  }

  @Test
  void testAuctionTimeLogic() {
    // 2. Test cái bẫy thời gian trong lớp Auction
    // Cố tình để endTime TRƯỚC startTime
    LocalDateTime now = LocalDateTime.now();
    LocalDateTime past = now.minusDays(1);

    assertThrows(
        IllegalArgumentException.class,
        () -> {
          new Auction(1L, 1L, now, past); // Cái này phải văng lỗi!
        });

    System.out.println("✅ Test Auction Time Logic (End before Start): PASS");
  }
}
