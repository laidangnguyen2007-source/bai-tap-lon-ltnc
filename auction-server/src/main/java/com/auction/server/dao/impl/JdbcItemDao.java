package com.auction.server.dao.impl;

import com.auction.server.config.DatabaseConfig;
import com.auction.server.dao.ItemDao;
import com.auction.server.model.entity.item.Artwork;
import com.auction.server.model.entity.item.Electronics;
import com.auction.server.model.entity.item.Item;
import com.auction.server.model.entity.item.ItemFactory;
import com.auction.server.model.entity.item.Vehicle;
import com.auction.server.model.enums.ItemCategory;
import com.auction.server.model.exception.AuctionException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Triển khai ItemDao sử dụng JDBC + MySQL.
 *
 * <p>Single Table Inheritance: Electronics, Artwork, Vehicle đều lưu vào bảng "items". Cột CATEGORY
 * xác định loại item. Phương thức mapRow() tạo đúng class con tương ứng.
 */
public class JdbcItemDao implements ItemDao {

  public JdbcItemDao() {
    // Constructor trống, connection sẽ được lấy trực tiếp trong mỗi method
  }

  // ─────────────────────────────────────────────
  // GenericDao methods
  // ─────────────────────────────────────────────

  @Override
  public Item save(Item item) {
    String sql =
        "INSERT INTO items (created_at, name, description, starting_price, seller_id, category, image_base64,"
            + " brand, warranty_months, power_watts, artist, art_year, medium,"
            + " make, model, vehicle_year, mileage)"
            + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    try (PreparedStatement ps =
        DatabaseConfig.getInstance()
            .getConnection()
            .prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
      ps.setTimestamp(1, Timestamp.valueOf(item.getCreatedAt()));
      ps.setString(2, item.getName());
      ps.setString(3, item.getDescription());
      ps.setLong(4, item.getStartingPrice());
      ps.setLong(5, item.getSellerId());
      ps.setString(6, item.getCategory().name());
      ps.setString(7, item.getImageBase64());
      bindItemTypeFields(ps, item, 8); // gán các cột đặc thù bắt đầu từ index 8
      ps.executeUpdate();

      try (ResultSet keys = ps.getGeneratedKeys()) {
        if (keys.next()) item.setId(keys.getLong(1));
      }
      return item;
    } catch (SQLException e) {
      throw new AuctionException("Database error saving item", e);
    }
  }

  @Override
  public Optional<Item> findById(Long id) {
    String sql = "SELECT * FROM items WHERE id = ?";
    try {
      Connection conn = DatabaseConfig.getInstance().getConnection();
      try (PreparedStatement ps = conn.prepareStatement(sql)) {
        ps.setLong(1, id);
        try (ResultSet rs = ps.executeQuery()) {
          if (rs.next()) {
            return Optional.of(mapRow(rs));
          }
        }
      }
    } catch (SQLException e) {
      System.err.println("Database error finding item by id " + id + ": " + e.getMessage());
      throw new AuctionException("Database error finding item by id", e);
    }
    return Optional.empty();
  }

  @Override
  public List<Item> findAll() {
    List<Item> list = new ArrayList<>();
    String sql = "SELECT * FROM items";
    try (Statement st = DatabaseConfig.getInstance().getConnection().createStatement();
        ResultSet rs = st.executeQuery(sql)) {
      while (rs.next()) list.add(mapRow(rs));
    } catch (SQLException e) {
      throw new AuctionException("Database error fetching all items", e);
    }
    return list;
  }

  @Override
  public Item update(Item item) {
    String sql =
        "UPDATE items SET name=?, description=?, starting_price=?, category=?, image_base64=?,"
            + " brand=?, warranty_months=?, power_watts=?,"
            + " artist=?, art_year=?, medium=?,"
            + " make=?, model=?, vehicle_year=?, mileage=?"
            + " WHERE id=?";
    try (PreparedStatement ps =
        DatabaseConfig.getInstance().getConnection().prepareStatement(sql)) {
      ps.setString(1, item.getName());
      ps.setString(2, item.getDescription());
      ps.setLong(3, item.getStartingPrice());
      ps.setString(4, item.getCategory().name());
      ps.setString(5, item.getImageBase64());
      bindItemTypeFieldsForUpdate(ps, item, 6);
      ps.setLong(16, item.getId());
      ps.executeUpdate();
      return item;
    } catch (SQLException e) {
      throw new AuctionException("Database error updating item", e);
    }
  }

  @Override
  public boolean deleteById(Long id) {
    String sql = "DELETE FROM items WHERE id = ?";
    try (PreparedStatement ps =
        DatabaseConfig.getInstance().getConnection().prepareStatement(sql)) {
      ps.setLong(1, id);
      return ps.executeUpdate() > 0;
    } catch (SQLException e) {
      throw new AuctionException("Database error deleting item by id", e);
    }
  }

  @Override
  public boolean existsById(Long id) {
    String sql = "SELECT COUNT(1) FROM items WHERE id = ?";
    try (PreparedStatement ps =
        DatabaseConfig.getInstance().getConnection().prepareStatement(sql)) {
      ps.setLong(1, id);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next() && rs.getInt(1) > 0;
      }
    } catch (SQLException e) {
      throw new AuctionException("Database error checking item existence", e);
    }
  }

  @Override
  public long count() {
    String sql = "SELECT COUNT(1) FROM items";
    try (Statement st = DatabaseConfig.getInstance().getConnection().createStatement();
        ResultSet rs = st.executeQuery(sql)) {
      return rs.next() ? rs.getLong(1) : 0L;
    } catch (SQLException e) {
      throw new AuctionException("Database error counting items", e);
    }
  }

  // ─────────────────────────────────────────────
  // ItemDao specific methods
  // ─────────────────────────────────────────────

  @Override
  public List<Item> findBySellerId(Long sellerId) {
    List<Item> list = new ArrayList<>();
    String sql = "SELECT * FROM items WHERE seller_id = ?";
    try (PreparedStatement ps =
        DatabaseConfig.getInstance().getConnection().prepareStatement(sql)) {
      ps.setLong(1, sellerId);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) list.add(mapRow(rs));
      }
    } catch (SQLException e) {
      throw new AuctionException("Database error finding items by seller id", e);
    }
    return list;
  }

  @Override
  public List<Item> findByCategory(ItemCategory category) {
    List<Item> list = new ArrayList<>();
    String sql = "SELECT * FROM items WHERE category = ?";
    try (PreparedStatement ps =
        DatabaseConfig.getInstance().getConnection().prepareStatement(sql)) {
      ps.setString(1, category.name());
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) list.add(mapRow(rs));
      }
    } catch (SQLException e) {
      throw new AuctionException("Database error finding items by category", e);
    }
    return list;
  }

  @Override
  public List<Item> findByNameContaining(String keyword) {
    List<Item> list = new ArrayList<>();
    String sql = "SELECT * FROM items WHERE LOWER(name) LIKE LOWER(?)";
    try (PreparedStatement ps =
        DatabaseConfig.getInstance().getConnection().prepareStatement(sql)) {
      ps.setString(1, "%" + keyword + "%");
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) list.add(mapRow(rs));
      }
    } catch (SQLException e) {
      throw new AuctionException("Database error finding items by name containing", e);
    }
    return list;
  }

  // ─────────────────────────────────────────────
  // Helper methods
  // ─────────────────────────────────────────────

  private Item mapRow(ResultSet rs) throws SQLException {
    Long id = rs.getLong("id");
    var createdAt = rs.getTimestamp("created_at").toLocalDateTime();
    String name = rs.getString("name");
    String description = rs.getString("description");
    long startingPrice = rs.getLong("starting_price");
    Long sellerId = rs.getLong("seller_id");
    ItemCategory category = ItemCategory.valueOf(rs.getString("category"));
    String imageBase64 = rs.getString("image_base64");

    // Factory Pattern: gọi ItemFactory.reconstruct*() thay vì new trực tiếp
    // Giữ cho DAO không bị phụ thuộc vào cụ thể của từng loại Item
    Item result =
        switch (category) {
          case ELECTRONICS ->
              ItemFactory.reconstructElectronics(
                  id,
                  createdAt,
                  name,
                  description,
                  startingPrice,
                  sellerId,
                  rs.getString("brand"),
                  rs.getInt("warranty_months"),
                  rs.getDouble("power_watts"));
          case ARTWORK ->
              ItemFactory.reconstructArtwork(
                  id,
                  createdAt,
                  name,
                  description,
                  startingPrice,
                  sellerId,
                  rs.getString("artist"),
                  rs.getInt("art_year"),
                  rs.getString("medium"));
          case VEHICLE ->
              ItemFactory.reconstructVehicle(
                  id,
                  createdAt,
                  name,
                  description,
                  startingPrice,
                  sellerId,
                  rs.getString("make"),
                  rs.getInt("vehicle_year"),
                  rs.getInt("mileage"),
                  ""); // fuelType chưa có cột, để trống

          // OTHER: Sản phẩm chung — dùng Electronics làm class cụ thể với giá trị mặc định
          // vì Item là abstract, cần 1 class con cụ thể để khởi tạo đối tượng
          case OTHER -> {
            Item otherItem =
                ItemFactory.reconstructElectronics(
                    id,
                    createdAt,
                    name,
                    description,
                    startingPrice,
                    sellerId,
                    rs.getString("brand") != null ? rs.getString("brand") : "N/A",
                    rs.getInt("warranty_months"),
                    rs.getDouble("power_watts"));
            otherItem.setCategory(ItemCategory.OTHER); // Ghi đè category đúng
            yield otherItem;
          }
        };
    result.setImageBase64(imageBase64);
    return result;
  }

  private void bindItemTypeFields(PreparedStatement ps, Item item, int start) throws SQLException {
    if (item instanceof Electronics e) {
      ps.setString(start, e.getBrand());
      ps.setInt(start + 1, e.getWarrantyMonths());
      ps.setDouble(start + 2, e.getPowerWatts());
      ps.setNull(start + 3, Types.VARCHAR);
      ps.setNull(start + 4, Types.INTEGER);
      ps.setNull(start + 5, Types.VARCHAR);
      ps.setNull(start + 6, Types.VARCHAR);
      ps.setNull(start + 7, Types.VARCHAR);
      ps.setNull(start + 8, Types.INTEGER);
      ps.setNull(start + 9, Types.DOUBLE);
    } else if (item instanceof Artwork a) {
      ps.setNull(start, Types.VARCHAR);
      ps.setNull(start + 1, Types.INTEGER);
      ps.setNull(start + 2, Types.DOUBLE);
      ps.setString(start + 3, a.getArtistName());
      ps.setInt(start + 4, a.getYearCreated());
      ps.setString(start + 5, a.getMedium());
      ps.setNull(start + 6, Types.VARCHAR);
      ps.setNull(start + 7, Types.VARCHAR);
      ps.setNull(start + 8, Types.INTEGER);
      ps.setNull(start + 9, Types.DOUBLE);
    } else if (item instanceof Vehicle v) {
      ps.setNull(start, Types.VARCHAR);
      ps.setNull(start + 1, Types.INTEGER);
      ps.setNull(start + 2, Types.DOUBLE);
      ps.setNull(start + 3, Types.VARCHAR);
      ps.setNull(start + 4, Types.INTEGER);
      ps.setNull(start + 5, Types.VARCHAR);
      ps.setString(start + 6, v.getManufacturer());
      ps.setNull(start + 7, Types.VARCHAR); // model chưa có getter riêng
      ps.setInt(start + 8, v.getYearManufactured());
      ps.setDouble(start + 9, v.getMileageKm());
    }
  }

  private void bindItemTypeFieldsForUpdate(PreparedStatement ps, Item item, int start)
      throws SQLException {
    if (item instanceof Electronics e) {
      ps.setString(start, e.getBrand());
      ps.setInt(start + 1, e.getWarrantyMonths());
      ps.setDouble(start + 2, e.getPowerWatts());
      ps.setNull(start + 3, Types.VARCHAR);
      ps.setNull(start + 4, Types.INTEGER);
      ps.setNull(start + 5, Types.VARCHAR);
      ps.setNull(start + 6, Types.VARCHAR);
      ps.setNull(start + 7, Types.VARCHAR);
      ps.setNull(start + 8, Types.INTEGER);
      ps.setNull(start + 9, Types.DOUBLE);
    } else if (item instanceof Artwork a) {
      ps.setNull(start, Types.VARCHAR);
      ps.setNull(start + 1, Types.INTEGER);
      ps.setNull(start + 2, Types.DOUBLE);
      ps.setString(start + 3, a.getArtistName());
      ps.setInt(start + 4, a.getYearCreated());
      ps.setString(start + 5, a.getMedium());
      ps.setNull(start + 6, Types.VARCHAR);
      ps.setNull(start + 7, Types.VARCHAR);
      ps.setNull(start + 8, Types.INTEGER);
      ps.setNull(start + 9, Types.DOUBLE);
    } else if (item instanceof Vehicle v) {
      ps.setNull(start, Types.VARCHAR);
      ps.setNull(start + 1, Types.INTEGER);
      ps.setNull(start + 2, Types.DOUBLE);
      ps.setNull(start + 3, Types.VARCHAR);
      ps.setNull(start + 4, Types.INTEGER);
      ps.setNull(start + 5, Types.VARCHAR);
      ps.setString(start + 6, v.getManufacturer());
      ps.setNull(start + 7, Types.VARCHAR);
      ps.setInt(start + 8, v.getYearManufactured());
      ps.setDouble(start + 9, v.getMileageKm());
    }
  }
}
