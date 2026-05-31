import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import server.model.entity.Auction;
import server.model.exception.InvalidBidException;

public class TestAuctionException {

  @Test
  void testInvalidBidException() {
    // 1. Kiểm tra InvalidBidException
    // Kiểm tra xem ngoại lệ có lưu đúng thông điệp (message) được truyền vào hay không
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
    // 2. Kiểm tra logic ràng buộc thời gian trong lớp Auction
    // Thiết lập endTime trước startTime để kiểm tra hành vi ném ngoại lệ
    LocalDateTime now = LocalDateTime.now();
    LocalDateTime past = now.minusDays(1);

    assertThrows(
        IllegalArgumentException.class,
        () -> {
          new Auction(
              1L, 1L, now, past); // Constructor phải ném ra ngoại lệ IllegalArgumentException
        });

    System.out.println("✅ Test Auction Time Logic (End before Start): PASS");
  }
}
