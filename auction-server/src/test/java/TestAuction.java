import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import server.model.entity.Auction;
import server.model.entity.item.Item;
import server.model.entity.item.ItemFactory;
import server.model.enums.AuctionStatus;
import server.model.enums.ItemCategory;

class TestAuction {

  @Test
  @DisplayName("Factory tạo Electronics thành công qua ItemFactory.create()")
  void testFactoryCreateElectronics() {
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

    assertNotNull(item, "Item không được null");
    assertEquals("Laptop Dell", item.getName());
    assertEquals(ItemCategory.ELECTRONICS, item.getCategory());
    assertEquals(1000L, item.getStartingPrice());
  }

  @Test
  @DisplayName("Factory tạo Artwork thành công")
  void testFactoryCreateArtwork() {
    Item item =
        ItemFactory.create(
            ItemCategory.ARTWORK,
            Map.of(
                "name", "Bức tranh Mona Lisa",
                "description", "Tranh phục chế",
                "startingPrice", 5000L,
                "sellerId", 2L,
                "artistName", "Leonardo da Vinci",
                "yearCreated", 1503,
                "medium", "Sơn dầu"));

    assertNotNull(item);
    assertEquals("Bức tranh Mona Lisa", item.getName());
    assertEquals(ItemCategory.ARTWORK, item.getCategory());
  }

  @Test
  @DisplayName("Factory tạo Vehicle thành công")
  void testFactoryCreateVehicle() {
    Item item =
        ItemFactory.create(
            ItemCategory.VEHICLE,
            Map.of(
                "name", "Toyota Camry 2023",
                "description", "Xe ô tô cũ",
                "startingPrice", 800000000L,
                "sellerId", 3L,
                "manufacturer", "Toyota",
                "yearManufactured", 2023,
                "mileageKm", 15000,
                "fuelType", "Xăng"));

    assertNotNull(item);
    assertEquals("Toyota Camry 2023", item.getName());
    assertEquals(ItemCategory.VEHICLE, item.getCategory());
  }

  @Test
  @DisplayName("Auction khởi tạo đúng trạng thái OPEN")
  void testAuctionInitStatus() {
    Auction auction = new Auction(1L, 1L, LocalDateTime.now(), LocalDateTime.now().plusDays(1));

    assertEquals(AuctionStatus.OPEN, auction.getStatus());
    assertEquals(0L, auction.getCurrentPrice());
    assertNull(auction.getCurrentWinnerId());
  }

  @Test
  @DisplayName("applyBid cập nhật giá và người dẫn đầu")
  void testApplyBid() {
    Auction auction = new Auction(1L, 1L, LocalDateTime.now(), LocalDateTime.now().plusDays(1));
    auction.setStatus(AuctionStatus.RUNNING);

    auction.applyBid(5000L, 99L);

    assertEquals(5000L, auction.getCurrentPrice());
    assertEquals(99L, auction.getCurrentWinnerId());
  }

  @Test
  @DisplayName("applyBid từ chối giá thấp hơn giá hiện tại")
  void testApplyBidRejectLowerPrice() {
    Auction auction = new Auction(1L, 1L, LocalDateTime.now(), LocalDateTime.now().plusDays(1));
    auction.setStatus(AuctionStatus.RUNNING);
    auction.applyBid(5000L, 99L);

    assertThrows(IllegalArgumentException.class, () -> auction.applyBid(3000L, 100L));
  }

  @Test
  @DisplayName("applyBid từ chối khi phiên không RUNNING")
  void testApplyBidRejectNotRunning() {
    Auction auction = new Auction(1L, 1L, LocalDateTime.now(), LocalDateTime.now().plusDays(1));
    // Status mặc định là OPEN, không phải RUNNING

    assertThrows(IllegalStateException.class, () -> auction.applyBid(5000L, 99L));
  }

  @Test
  @DisplayName("Anti-sniping: extendEndTime gia hạn thời gian đúng")
  void testExtendEndTime() {
    LocalDateTime start = LocalDateTime.now();
    LocalDateTime end = start.plusHours(1);
    Auction auction = new Auction(1L, 1L, start, end);

    auction.extendEndTime(30);

    assertEquals(end.plusSeconds(30), auction.getEndTime());
  }

  @Test
  @DisplayName("Auction từ chối endTime trước startTime")
  void testAuctionRejectInvalidTime() {
    LocalDateTime now = LocalDateTime.now();
    LocalDateTime past = now.minusDays(1);

    assertThrows(IllegalArgumentException.class, () -> new Auction(1L, 1L, now, past));
  }
}
