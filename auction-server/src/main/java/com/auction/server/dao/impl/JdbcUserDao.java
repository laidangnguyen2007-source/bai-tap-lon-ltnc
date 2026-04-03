package com.auction.server.dao.impl;

import com.auction.server.config.DatabaseConfig;
import com.auction.server.dao.UserDao;
import com.auction.server.model.entity.user.Admin;
import com.auction.server.model.entity.user.Bidder;
import com.auction.server.model.entity.user.Seller;
import com.auction.server.model.entity.user.User;
import com.auction.server.model.enums.UserRole;
import com.auction.server.model.exception.AuctionException;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Triển khai UserDao sử dụng JDBC + MySQL.
 *
 * <p>PreparedStatement: Dùng dấu ? thay vì nhúng trực tiếp giá trị vào chuỗi SQL. Lợi ích kép: (1)
 * Chống SQL Injection (2) Database cache query plan → nhanh hơn.
 *
 * <p>Single Table Inheritance: Toàn bộ Bidder/Seller/Admin đều lưu chung vào 1 bảng "users". Cột
 * ROLE phân biệt loại. Phương thức mapRow() đọc cột ROLE và tạo đúng class con tương ứng.
 */
public class JdbcUserDao implements UserDao {

  private final Connection conn;

  public JdbcUserDao() {
    try {
      this.conn = DatabaseConfig.getInstance().getConnection();
    } catch (SQLException e) {
      throw new AuctionException("Failed to connect to database for JdbcUserDao", e);
    }
  }

  // ─────────────────────────────────────────────
  // GenericDao methods
  // ─────────────────────────────────────────────

  @Override
  public User save(User user) {
    String sql =
        "INSERT INTO users (created_at, username, password_hash, email, role,"
            + " balance, shop_name, rating, access_level) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

    try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
      ps.setTimestamp(1, Timestamp.valueOf(user.getCreatedAt()));
      ps.setString(2, user.getUsername());
      ps.setString(3, user.getPasswordHash());
      ps.setString(4, user.getEmail());
      ps.setString(5, user.getRole().name());
      bindUserTypeFields(ps, user); // gán các cột riêng của từng loại User
      ps.executeUpdate();

      // Lấy ID vừa được Database tự sinh ra (AUTO_INCREMENT)
      try (ResultSet keys = ps.getGeneratedKeys()) {
        if (keys.next()) {
          user.setId(keys.getLong(1));
        }
      }
      return user;
    } catch (SQLException e) {
      throw new AuctionException("Database error saving User", e);
    }
  }

  @Override
  public Optional<User> findById(Long id) {
    String sql = "SELECT * FROM users WHERE id = ?";
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setLong(1, id);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return Optional.of(mapRow(rs));
        }
      }
    } catch (SQLException e) {
      throw new AuctionException("Database error finding User by id", e);
    }
    return Optional.empty();
  }

  @Override
  public List<User> findAll() {
    List<User> users = new ArrayList<>();
    String sql = "SELECT * FROM users";
    try (Statement st = conn.createStatement();
        ResultSet rs = st.executeQuery(sql)) {
      while (rs.next()) {
        users.add(mapRow(rs));
      }
    } catch (SQLException e) {
      throw new AuctionException("Database error fetching all Users", e);
    }
    return users;
  }

  @Override
  public User update(User user) {
    String sql =
        "UPDATE users SET username=?, password_hash=?, email=?,"
            + " balance=?, shop_name=?, rating=?, access_level=? WHERE id=?";
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, user.getUsername());
      ps.setString(2, user.getPasswordHash());
      ps.setString(3, user.getEmail());
      bindUserTypeFields(ps, user, 4); // các cột riêng bắt đầu từ index 4
      ps.setLong(8, user.getId());
      ps.executeUpdate();
      return user;
    } catch (SQLException e) {
      throw new AuctionException("Database error updating User", e);
    }
  }

  @Override
  public boolean deleteById(Long id) {
    String sql = "DELETE FROM users WHERE id = ?";
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setLong(1, id);
      return ps.executeUpdate() > 0;
    } catch (SQLException e) {
      throw new AuctionException("Database error deleting User", e);
    }
  }

  @Override
  public boolean existsById(Long id) {
    String sql = "SELECT COUNT(1) FROM users WHERE id = ?";
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setLong(1, id);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next() && rs.getInt(1) > 0;
      }
    } catch (SQLException e) {
      throw new AuctionException("Database error checking User existence", e);
    }
  }

  @Override
  public long count() {
    String sql = "SELECT COUNT(1) FROM users";
    try (Statement st = conn.createStatement();
        ResultSet rs = st.executeQuery(sql)) {
      return rs.next() ? rs.getLong(1) : 0L;
    } catch (SQLException e) {
      throw new AuctionException("Database error counting Users", e);
    }
  }

  // ─────────────────────────────────────────────
  // UserDao specific methods
  // ─────────────────────────────────────────────

  @Override
  public Optional<User> findByUsername(String username) {
    String sql = "SELECT * FROM users WHERE username = ?";
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, username);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) return Optional.of(mapRow(rs));
      }
    } catch (SQLException e) {
      throw new AuctionException("Database error finding User by username", e);
    }
    return Optional.empty();
  }

  @Override
  public Optional<User> findByEmail(String email) {
    String sql = "SELECT * FROM users WHERE email = ?";
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, email);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) return Optional.of(mapRow(rs));
      }
    } catch (SQLException e) {
      throw new AuctionException("Database error finding User by email", e);
    }
    return Optional.empty();
  }

  @Override
  public List<User> findByRole(UserRole role) {
    List<User> users = new ArrayList<>();
    String sql = "SELECT * FROM users WHERE role = ?";
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, role.name());
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) users.add(mapRow(rs));
      }
    } catch (SQLException e) {
      throw new AuctionException("Database error finding Users by role", e);
    }
    return users;
  }

  @Override
  public boolean existsByUsername(String username) {
    String sql = "SELECT COUNT(1) FROM users WHERE username = ?";
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, username);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next() && rs.getInt(1) > 0;
      }
    } catch (SQLException e) {
      throw new AuctionException("Database error checking username existence", e);
    }
  }

  @Override
  public boolean existsByEmail(String email) {
    String sql = "SELECT COUNT(1) FROM users WHERE email = ?";
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, email);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next() && rs.getInt(1) > 0;
      }
    } catch (SQLException e) {
      throw new AuctionException("Database error checking email existence", e);
    }
  }

  // ─────────────────────────────────────────────
  // Helper methods
  // ─────────────────────────────────────────────

  /**
   * Đọc 1 dòng từ ResultSet và tạo ra đúng class con (Bidder/Seller/Admin) dựa vào giá trị của cột
   * ROLE. Đây là điểm áp dụng Đa hình (Polymorphism): cùng 1 hàm mapRow trả về nhiều kiểu User khác
   * nhau tuỳ vào dữ liệu.
   */
  private User mapRow(ResultSet rs) throws SQLException {
    Long id = rs.getLong("id");
    LocalDateTime createdAt = rs.getTimestamp("created_at").toLocalDateTime();
    String username = rs.getString("username");
    String passwordHash = rs.getString("password_hash");
    String email = rs.getString("email");
    UserRole role = UserRole.valueOf(rs.getString("role"));

    return switch (role) {
      case BIDDER ->
          new Bidder(id, createdAt, username, passwordHash, email, rs.getDouble("balance"));
      case SELLER ->
          new Seller(
              id,
              createdAt,
              username,
              passwordHash,
              email,
              rs.getString("shop_name"),
              rs.getDouble("rating"));
      case ADMIN ->
          new Admin(id, createdAt, username, passwordHash, email, rs.getInt("access_level"));
    };
  }

  /** Gán giá trị các cột đặc thù của từng loại User khi INSERT (bắt đầu từ index 6). */
  private void bindUserTypeFields(PreparedStatement ps, User user) throws SQLException {
    bindUserTypeFields(ps, user, 6);
  }

  /** Gán giá trị các cột đặc thù bắt đầu từ startIndex tùy chỉnh. */
  private void bindUserTypeFields(PreparedStatement ps, User user, int startIndex)
      throws SQLException {
    if (user instanceof Bidder b) {
      ps.setDouble(startIndex, b.getBalance());
      ps.setNull(startIndex + 1, Types.VARCHAR); // shop_name
      ps.setNull(startIndex + 2, Types.DOUBLE); // rating
      ps.setNull(startIndex + 3, Types.INTEGER); // access_level
    } else if (user instanceof Seller s) {
      ps.setNull(startIndex, Types.DOUBLE); // balance
      ps.setString(startIndex + 1, s.getShopName());
      ps.setDouble(startIndex + 2, s.getRating());
      ps.setNull(startIndex + 3, Types.INTEGER); // access_level
    } else if (user instanceof Admin a) {
      ps.setNull(startIndex, Types.DOUBLE); // balance
      ps.setNull(startIndex + 1, Types.VARCHAR); // shop_name
      ps.setNull(startIndex + 2, Types.DOUBLE); // rating
      ps.setInt(startIndex + 3, a.getAccessLevel());
    }
  }
}
