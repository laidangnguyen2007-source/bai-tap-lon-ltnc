package com.auction.server.model.entity.item;

import com.auction.server.model.enums.ItemCategory;
import com.auction.server.model.exception.AuctionException;
import java.time.LocalDateTime;
import java.util.Map;

/*
 * Factory Method Pattern — Nhà máy đúc sản phẩm đấu giá.
 *
 * File này nằm CÙNG PACKAGE với Artwork / Electronics / Vehicle để có thể gọi các Constructor
 * package-private của chúng. Nhờ đó KHÔNG AI bên ngoài package này có thể "new Artwork(...)" trực
 * tiếp — bắt buộc phải đi qua Factory này.
 *
 * Hai nhiệm vụ: 1. create() — Đúc Item MỚI từ dữ liệu người dùng nhập vào. 2. reconstruct() — Phục
 * dựng lại Item từ dòng dữ liệu đọc từ Database.
 */
public class ItemFactory {

  // Ngăn khởi tạo object ItemFactory — toàn bộ method đều là static
  private ItemFactory() {}

  /*
   * Hàm create() dùng cho lúc đăng bán sản phẩm, dữ liệu được lấy từ client gửi lên (nhập từ bán
   * phím lúc điền thông tin đăng bán), nhưng chỉ lưu trên RAM, khi tắt sever sẽ bị mất, nên sau này
   * cần phục dựng lại. Đúc một Item mới từ loại (là cái enum ItemCategory) và các thuộc tính
   * (Map<String, Object> params). Caller (ItemService) chỉ cần truyền vào đúng category và map đủ
   * trường, Factory sẽ quyết định gọi constructor của class con nào.
   *
   * @param category Loại sản phẩm (ARTWORK, ELECTRONICS, VEHICLE)
   *
   * @param params Bản đồ chứa các thuộc tính bắt buộc tuỳ theo loại
   *
   * @return Item đã khởi tạo, chưa có ID (sẽ được DAO gán sau khi INSERT)
   */
  public static Item create(ItemCategory category, Map<String, Object> params) {
    String name = require(params, "name", String.class);
    String description = require(params, "description", String.class);
    long startingPrice = require(params, "startingPrice", Long.class);
    Long sellerId = require(params, "sellerId", Long.class);
    // Dùng require() thay vì get() để bắt lỗi nếu thiếu tham số thì hàm get sẽ trả về null
    // còn require() thì ném ra exception, và hàm require() còn kiểm tra cả kiểu dữ liệu tham số,
    // nếu sai kiểu cũng sẽ ném ra exception

    return switch (category) {
      case ARTWORK ->
          new Artwork(
              name,
              description,
              startingPrice,
              sellerId,
              require(params, "artistName", String.class),
              require(params, "yearCreated", Integer.class),
              require(params, "medium", String.class));

      case ELECTRONICS ->
          new Electronics(
              name,
              description,
              startingPrice,
              sellerId,
              require(params, "brand", String.class),
              require(params, "warrantyMonths", Integer.class),
              require(params, "powerWatts", Double.class));

      case VEHICLE ->
          new Vehicle(
              name,
              description,
              startingPrice,
              sellerId,
              require(params, "manufacturer", String.class),
              require(params, "yearManufactured", Integer.class),
              require(params, "mileageKm", Integer.class),
              require(params, "fuelType", String.class));
    };
  }

  // ─── PHỤC DỰNG ITEM TỪ DATABASE ──────────────────────────────────

  /*
   * Phục dựng lại Item từ dữ liệu lấy từ Database (dành cho JdbcItemDao.mapRow()) thường dùng để
   * hiển thị dữ liệu đang có (các sản phẩm đang đấu giá, các cuộc đấu giá đang diễn ra, ...). Cần
   * truyền đầy đủ id + createdAt vì đây là Item ĐÃ TỒN TẠI trong DB.
   */
  public static Artwork reconstructArtwork(
      Long id,
      LocalDateTime createdAt,
      String name,
      String description,
      long startingPrice,
      Long sellerId,
      String artistName,
      int yearCreated,
      String medium) {
    return new Artwork(
        id, createdAt, name, description, startingPrice, sellerId, artistName, yearCreated, medium);
  }

  public static Electronics reconstructElectronics(
      Long id,
      LocalDateTime createdAt,
      String name,
      String description,
      long startingPrice,
      Long sellerId,
      String brand,
      int warrantyMonths,
      double powerWatts) {
    return new Electronics(
        id,
        createdAt,
        name,
        description,
        startingPrice,
        sellerId,
        brand,
        warrantyMonths,
        powerWatts);
  }

  public static Vehicle reconstructVehicle(
      Long id,
      LocalDateTime createdAt,
      String name,
      String description,
      long startingPrice,
      Long sellerId,
      String manufacturer,
      int yearManufactured,
      int mileageKm,
      String fuelType) {
    return new Vehicle(
        id,
        createdAt,
        name,
        description,
        startingPrice,
        sellerId,
        manufacturer,
        yearManufactured,
        mileageKm,
        fuelType);
  }

  // ─── INTERNAL ────────────────────────────────────────────────────

  @SuppressWarnings("unchecked")
  private static <T> T require(Map<String, Object> params, String key, Class<T> type) {
    Object value = params.get(key);
    if (value == null) {
      throw new AuctionException("ItemFactory: thiếu tham số bắt buộc → '" + key + "'");
    }
    if (!type.isInstance(value)) {
      // isInstance() lợi hơn instanceof() ở chỗ nó không yêu cầu xác định kiểu dữ liệu tại thời
      // điểm Compile nên có thể linh hoạt kiểm tra tùy theo biến Class người dùng đẩy vào Map
      throw new AuctionException(
          "ItemFactory: tham số '"
              + key
              + "' phải là kiểu "
              + type.getSimpleName()
              + " nhưng nhận được "
              + value.getClass().getSimpleName());
    }
    return (T) value;
  }
}
