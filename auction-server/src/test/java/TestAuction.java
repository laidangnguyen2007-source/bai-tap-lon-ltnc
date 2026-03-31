import com.auction.server.model.entity.Auction;
import com.auction.server.model.entity.item.Electronics;
import com.auction.server.model.entity.item.Item;
import java.time.LocalDateTime;

public class TestAuction {
  public static void main(String[] args) {
    // 1. Tạo món đồ cụ thể
    Item item = new Electronics("Laptop Dell", "Gaming laptop", 1000.0, 1L, "Dell", 12, 135.1);
    Long itemId = 1L;
    Long sellerId = 1L;
    LocalDateTime now = LocalDateTime.now();
    LocalDateTime end = now.plusDays(1);

    // 2. Tạo cuộc đấu giá cho món đồ đó
    Auction auction = new Auction(itemId, sellerId, now, end);

    // 3. In kết quả
    System.out.println("Item: " + item.getName());
    System.out.println("Current price: " + auction.getCurrentPrice());
  }
}
