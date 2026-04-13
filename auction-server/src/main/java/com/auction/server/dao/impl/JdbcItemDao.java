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

  private final Connection conn;

  public JdbcItemDao() {
    try {
      this.conn = DatabaseConfig.getInstance().getConnection();
    } catch (SQLException e) {
      throw new AuctionException("Failed to connect to database for JdbcItemDao", e);
    }
  }

  // ─────────────────────────────────────────────
  // GenericDao methods
  // ─────────────────────────────────────────────

  @Override
  public Item save(Item item) {
    String sql =
        "INSERT INTO items (created_at, name, description, starting_price, seller_id, category,"
            + " brand, warranty_months, power_watts, artist, art_year, medium,"
            + " make, model, vehicle_year, mileage)"
            + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
      ps.setTimestamp(1, Timestamp.valueOf(item.getCreatedAt()));
      ps.setString(2, item.getName());
      ps.setString(3, item.getDescription());
      ps.setLong(4, item.getStartingPrice());
      ps.setLong(5, item.getSellerId());
      ps.setString(6, item.getCategory().name());
      bindItemTypeFields(ps, item); // gán các cột đặc thù bắt đầu từ index 7
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
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setLong(1, id);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) return Optional.of(mapRow(rs));
      }
    } catch (SQLException e) {
      throw new AuctionException("Database error finding item by id", e);
    }
    return Optional.empty();
  }

  @Override
  public List<Item> findAll() {
    List<Item> list = new ArrayList<>();
    String sql = "SELECT * FROM items";
    try (Statement st = conn.createStatement();
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
        "UPDATE items SET name=?, description=?, starting_price=?,"
            + " brand=?, warranty_months=?, power_watts=?,"
            + " artist=?, art_year=?, medium=?,"
            + " make=?, model=?, vehicle_year=?, mileage=?"
            + " WHERE id=?";
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, item.getName());
      ps.setString(2, item.getDescription());
      ps.setLong(3, item.getStartingPrice());
      bindItemTypeFieldsForUpdate(ps, item, 4);
      ps.setLong(14, item.getId());
      ps.executeUpdate();
      return item;
    } catch (SQLException e) {
      throw new AuctionException("Database error updating item", e);
    }
  }

  @Override
  public boolean deleteById(Long id) {
    String sql = "DELETE FROM items WHERE id = ?";
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setLong(1, id);
      return ps.executeUpdate() > 0;
    } catch (SQLException e) {
      throw new AuctionException("Database error deleting item by id", e);
    }
  }

  @Override
  public boolean existsById(Long id) {
    String sql = "SELECT COUNT(1) FROM items WHERE id = ?";
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
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
    try (Statement st = conn.createStatement();
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
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
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
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
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
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
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

    // Factory Pattern: gọi ItemFactory.reconstruct*() thay vì new trực tiếp
    // Giữ cho DAO không bị phụ thuộc vào cụ thể của từng loại Item
    return switch (category) {
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
    };
  }

  private void bindItemTypeFields(PreparedStatement ps, Item item) throws SQLException {
    if (item instanceof Electronics e) {
      ps.setString(7, e.getBrand());
      ps.setInt(8, e.getWarrantyMonths());
      ps.setDouble(9, e.getPowerWatts());
      ps.setNull(10, Types.VARCHAR);
      ps.setNull(11, Types.INTEGER);
      ps.setNull(12, Types.VARCHAR);
      ps.setNull(13, Types.VARCHAR);
      ps.setNull(14, Types.VARCHAR);
      ps.setNull(15, Types.INTEGER);
      ps.setNull(16, Types.DOUBLE);
    } else if (item instanceof Artwork a) {
      ps.setNull(7, Types.VARCHAR);
      ps.setNull(8, Types.INTEGER);
      ps.setNull(9, Types.DOUBLE);
      ps.setString(10, a.getArtistName());
      ps.setInt(11, a.getYearCreated());
      ps.setString(12, a.getMedium());
      ps.setNull(13, Types.VARCHAR);
      ps.setNull(14, Types.VARCHAR);
      ps.setNull(15, Types.INTEGER);
      ps.setNull(16, Types.DOUBLE);
    } else if (item instanceof Vehicle v) {
      ps.setNull(7, Types.VARCHAR);
      ps.setNull(8, Types.INTEGER);
      ps.setNull(9, Types.DOUBLE);
      ps.setNull(10, Types.VARCHAR);
      ps.setNull(11, Types.INTEGER);
      ps.setNull(12, Types.VARCHAR);
      ps.setString(13, v.getManufacturer());
      ps.setNull(14, Types.VARCHAR); // model chưa có getter riêng
      ps.setInt(15, v.getYearManufactured());
      ps.setDouble(16, v.getMileageKm());
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
