import com.auction.server.model.entity.Auction;
import com.auction.server.model.entity.item.Item;
import com.auction.server.model.entity.item.ItemFactory;
import com.auction.server.model.enums.ItemCategory;
import java.time.LocalDateTime;
import java.util.Map;

public class TestAuction {
  public static void main(String[] args) {
    // 1. Tạo món đồ cụ thể — bắt buộc đi qua ItemFactory (Factory Pattern)
    Item item =
        ItemFactory.create(
            ItemCategory.ELECTRONICS,
            Map.of(
                "name",
                "Laptop Dell",
                "description",
                "Gaming laptop",
                "startingPrice",
                1000L,
                "sellerId",
                1L,
                "brand",
                "Dell",
                "warrantyMonths",
                12,
                "powerWatts",
                135.1));
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
