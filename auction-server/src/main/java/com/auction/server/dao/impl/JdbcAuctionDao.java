package com.auction.server.dao.impl;

import com.auction.server.config.DatabaseConfig;
import com.auction.server.dao.AuctionDao;
import com.auction.server.model.entity.Auction;
import com.auction.server.model.enums.AuctionStatus;
import com.auction.server.model.exception.AuctionException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Triển khai AuctionDao sử dụng JDBC + MySQL.
 *
 * Pessimistic Locking: Phương thức findById dùng SELECT ... FOR UPDATE để khóa dòng dữ liệu khi
 * AuctionService đang xử lý lệnh đặt giá (bid). Nếu 2 luồng cùng cố gắng đặt giá trên cùng 1
 * Auction, luồng thứ 2 bị Database giữ lại cho đến khi luồng thứ 1 COMMIT xong. Đây là cơ chế chống
 * Lost Update ở tầng Database, đảm bảo tính Concurrent Bidding của hệ thống.
 */
public class JdbcAuctionDao implements AuctionDao { // DIP

  public JdbcAuctionDao() {}

  @Override // GenericDao
  public Auction save(Auction auction) {
    String sql = "INSERT INTO auctions (created_at, item_id, seller_id, current_price,"
        + " current_winner_id, status, start_time, end_time)" + " VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

    try (PreparedStatement ps = DatabaseConfig.getInstance().getConnection().prepareStatement(sql,
        Statement.RETURN_GENERATED_KEYS)) {
      // prepareStatement: Biến câu lệnh SQL có dấu ? lúc nãy thành một đối tượng có thể điều khiển.
      // RETURN_GENERATED_KEYS: Một cái "giỏ" đi kèm để hứng cái ID tự động mà MySQL sắp tạo
      // ra.
      ps.setTimestamp(1, Timestamp.valueOf(auction.getCreatedAt()));
      ps.setLong(2, auction.getItemId());
      ps.setLong(3, auction.getSellerId());
      ps.setLong(4, auction.getCurrentPrice());
      setNullableLong(ps, 5, auction.getCurrentWinnerId()); // null nếu chưa có ai đặt giá
      ps.setString(6, auction.getStatus().name());
      ps.setTimestamp(7, Timestamp.valueOf(auction.getStartTime()));
      ps.setTimestamp(8, Timestamp.valueOf(auction.getEndTime()));
      ps.executeUpdate();

      try (ResultSet keys = ps.getGeneratedKeys()) {
        if (keys.next())
          auction.setId(keys.getLong(1)); // 1 là cột đầu tiên trong ResultSet (tương ứng với ID)
      }
      return auction;
    } catch (SQLException e) {
      throw new AuctionException("Database error saving Auction", e);
    }
  }

  /**
   * Tìm Auction theo ID với khóa Pessimistic (FOR UPDATE).
   *
   * Khi AuctionService cần đặt giá, nó gọi hàm này trong 1 Transaction. MySQL sẽ khóa dòng
   * auction đó lại. Các luồng khác gọi findById cùng ID sẽ bị block cho đến khi Transaction kết
   * thúc (COMMIT/ROLLBACK). Điều này đảm bảo chỉ 1 luồng cập nhật giá tại 1 thời điểm.
   */
  @Override
  public Optional<Auction> findById(Long id) {
    // FOR UPDATE: Khóa dòng này lại cho đến khi transaction kết thúc
    String sql = "SELECT * FROM auctions WHERE id = ? FOR UPDATE";
    try (
        PreparedStatement ps = DatabaseConfig.getInstance().getConnection().prepareStatement(sql)) {
      ps.setLong(1, id);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next())
          return Optional.of(mapRow(rs));
      }
    } catch (SQLException e) {
      throw new AuctionException("Database error finding Auction by id", e);
    }
    return Optional.empty();
  }

  @Override
  public List<Auction> findAll() {
    List<Auction> list = new ArrayList<>();
    String sql = "SELECT * FROM auctions";
    try (Statement st = DatabaseConfig.getInstance().getConnection().createStatement();
        ResultSet rs = st.executeQuery(sql)) {
      while (rs.next())
        list.add(mapRow(rs));
    } catch (SQLException e) {
      throw new AuctionException("Database error fetching all Auctions", e);
    }
    return list;
  }

  @Override
  public Auction update(Auction auction) {
    String sql =
        "UPDATE auctions SET current_price=?, current_winner_id=?, status=?, start_time=?, end_time=?"
            + " WHERE id=?";
    try (
        PreparedStatement ps = DatabaseConfig.getInstance().getConnection().prepareStatement(sql)) {
      ps.setLong(1, auction.getCurrentPrice());
      setNullableLong(ps, 2, auction.getCurrentWinnerId());
      ps.setString(3, auction.getStatus().name());
      ps.setTimestamp(4, Timestamp.valueOf(auction.getStartTime()));
      ps.setTimestamp(5, Timestamp.valueOf(auction.getEndTime()));
      ps.setLong(6, auction.getId());
      ps.executeUpdate();
      return auction;
    } catch (SQLException e) {
      throw new AuctionException("Database error updating Auction", e);
    }
  }

  @Override
  public boolean deleteById(Long id) {
    String sql = "DELETE FROM auctions WHERE id = ?";
    try (
        PreparedStatement ps = DatabaseConfig.getInstance().getConnection().prepareStatement(sql)) {
      ps.setLong(1, id);
      return ps.executeUpdate() > 0;
    } catch (SQLException e) {
      throw new AuctionException("Database error deleting Auction", e);
    }
  }

  @Override
  public boolean existsById(Long id) {
    String sql = "SELECT COUNT(1) FROM auctions WHERE id = ?";
    try (
        PreparedStatement ps = DatabaseConfig.getInstance().getConnection().prepareStatement(sql)) {
      ps.setLong(1, id);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next() && rs.getInt(1) > 0;
      }
    } catch (SQLException e) {
      throw new AuctionException("Database error checking Auction existence", e);
    }
  }

  @Override
  public long count() {
    String sql = "SELECT COUNT(1) FROM auctions";
    try (Statement st = DatabaseConfig.getInstance().getConnection().createStatement();
        ResultSet rs = st.executeQuery(sql)) {
      return rs.next() ? rs.getLong(1) : 0L;
    } catch (SQLException e) {
      throw new AuctionException("Database error counting Auctions", e);
    }
  }

  // ─────────────────────────────────────────────
  // AuctionDao specific methods
  // ─────────────────────────────────────────────

  @Override
  public List<Auction> findByStatus(AuctionStatus status) {
    List<Auction> list = new ArrayList<>();
    String sql = "SELECT * FROM auctions WHERE status = ?";
    try (
        PreparedStatement ps = DatabaseConfig.getInstance().getConnection().prepareStatement(sql)) {
      ps.setString(1, status.name());
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next())
          list.add(mapRow(rs));
      }
    } catch (SQLException e) {
      throw new AuctionException("Database error finding Auctions by status", e);
    }
    return list;
  }

  @Override
  public Optional<Auction> findByItemId(Long itemId) {
    String sql = "SELECT * FROM auctions WHERE item_id = ?";
    try (
        PreparedStatement ps = DatabaseConfig.getInstance().getConnection().prepareStatement(sql)) {
      ps.setLong(1, itemId);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next())
          return Optional.of(mapRow(rs));
      }
    } catch (SQLException e) {
      throw new AuctionException("Database error finding Auction by item ID", e);
    }
    return Optional.empty();
  }

  @Override
  public List<Auction> findBySellerId(Long sellerId) {
    List<Auction> list = new ArrayList<>();
    String sql = "SELECT * FROM auctions WHERE seller_id = ?";
    try (
        PreparedStatement ps = DatabaseConfig.getInstance().getConnection().prepareStatement(sql)) {
      ps.setLong(1, sellerId);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next())
          list.add(mapRow(rs));
      }
    } catch (SQLException e) {
      throw new AuctionException("Database error finding Auctions by seller ID", e);
    }
    return list;
  }

  @Override
  public List<Auction> findRunningAuctions() {
    return findByStatus(AuctionStatus.RUNNING);
  }

  @Override
  public List<Auction> findActiveAuctions() {
    List<Auction> list = new ArrayList<>();
    String sql = "SELECT * FROM auctions WHERE status IN ('OPEN', 'RUNNING')";
    try (Statement st = DatabaseConfig.getInstance().getConnection().createStatement();
        ResultSet rs = st.executeQuery(sql)) {
      while (rs.next())
        list.add(mapRow(rs));
    } catch (SQLException e) {
      throw new AuctionException("Database error finding active Auctions", e);
    }
    return list;
  }

  // ─────────────────────────────────────────────
  // Helper methods
  // ─────────────────────────────────────────────

  private Auction mapRow(ResultSet rs) throws SQLException {
    Long currentWinnerId = rs.getLong("current_winner_id");
    if (rs.wasNull()) {
      currentWinnerId = null;
    }

    return new Auction(rs.getLong("id"), rs.getTimestamp("created_at").toLocalDateTime(),
        rs.getLong("item_id"), rs.getLong("seller_id"), rs.getLong("current_price"),
        currentWinnerId, AuctionStatus.valueOf(rs.getString("status")),
        rs.getTimestamp("start_time").toLocalDateTime(),
        rs.getTimestamp("end_time").toLocalDateTime());
  }

  private void setNullableLong(PreparedStatement ps, int index, Long value) throws SQLException {
    if (value == null) {
      ps.setNull(index, Types.BIGINT);
    } else {
      ps.setLong(index, value);
    }
  }
}
