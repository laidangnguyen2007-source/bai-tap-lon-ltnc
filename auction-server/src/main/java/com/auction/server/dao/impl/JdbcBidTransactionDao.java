package com.auction.server.dao.impl;

import com.auction.server.config.DatabaseConfig;
import com.auction.server.dao.BidTransactionDao;
import com.auction.server.model.entity.BidTransaction;
import com.auction.server.model.exception.AuctionException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Triển khai BidTransactionDao sử dụng JDBC + MySQL.
 *
 * <p>ORDER BY timestamp: Dữ liệu được sắp xếp ngay tại tầng Database thay vì dùng Java Stream API
 * .sorted(). Hiệu quả hơn vì Database có index tối ưu.
 *
 * <p>Ngoại lệ SQLException đã được bọc vào trong AuctionException để không làm lộ implementation
 * chi tiết.
 */
public class JdbcBidTransactionDao implements BidTransactionDao {

  private final Connection conn;

  public JdbcBidTransactionDao() {
    try {
      this.conn = DatabaseConfig.getInstance().getConnection();
    } catch (SQLException e) {
      throw new AuctionException("Failed to connect to database for JdbcBidTransactionDao", e);
    }
  }

  // ─────────────────────────────────────────────
  // GenericDao methods
  // ─────────────────────────────────────────────

  @Override
  public BidTransaction save(BidTransaction bid) {
    String sql =
        "INSERT INTO bid_transactions (created_at, auction_id, bidder_id, amount, timestamp)"
            + " VALUES (?, ?, ?, ?, ?)";
    try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
      ps.setTimestamp(1, Timestamp.valueOf(bid.getCreatedAt()));
      ps.setLong(2, bid.getAuctionId());
      ps.setLong(3, bid.getBidderId());
      ps.setLong(4, bid.getAmount());
      ps.setTimestamp(5, Timestamp.valueOf(bid.getTimestamp()));
      ps.executeUpdate();

      try (ResultSet keys = ps.getGeneratedKeys()) {
        if (keys.next()) bid.setId(keys.getLong(1));
      }
      return bid;
    } catch (SQLException e) {
      throw new AuctionException("Database error saving BidTransaction", e);
    }
  }

  @Override
  public Optional<BidTransaction> findById(Long id) {
    String sql = "SELECT * FROM bid_transactions WHERE id = ?";
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setLong(1, id);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) return Optional.of(mapRow(rs));
      }
    } catch (SQLException e) {
      throw new AuctionException("Database error finding BidTransaction by id", e);
    }
    return Optional.empty();
  }

  @Override
  public List<BidTransaction> findAll() {
    List<BidTransaction> list = new ArrayList<>();
    String sql = "SELECT * FROM bid_transactions ORDER BY timestamp ASC";
    try (Statement st = conn.createStatement();
        ResultSet rs = st.executeQuery(sql)) {
      while (rs.next()) list.add(mapRow(rs));
    } catch (SQLException e) {
      throw new AuctionException("Database error fetching all BidTransactions", e);
    }
    return list;
  }

  @Override
  public BidTransaction update(BidTransaction bid) {
    throw new UnsupportedOperationException("BidTransaction is immutable and cannot be updated.");
  }

  @Override
  public boolean deleteById(Long id) {
    String sql = "DELETE FROM bid_transactions WHERE id = ?";
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setLong(1, id);
      return ps.executeUpdate() > 0;
    } catch (SQLException e) {
      throw new AuctionException("Database error deleting BidTransaction", e);
    }
  }

  @Override
  public boolean existsById(Long id) {
    String sql = "SELECT COUNT(1) FROM bid_transactions WHERE id = ?";
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setLong(1, id);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next() && rs.getInt(1) > 0;
      }
    } catch (SQLException e) {
      throw new AuctionException("Database error checking BidTransaction existence", e);
    }
  }

  @Override
  public long count() {
    String sql = "SELECT COUNT(1) FROM bid_transactions";
    try (Statement st = conn.createStatement();
        ResultSet rs = st.executeQuery(sql)) {
      return rs.next() ? rs.getLong(1) : 0L;
    } catch (SQLException e) {
      throw new AuctionException("Database error counting BidTransactions", e);
    }
  }

  // ─────────────────────────────────────────────
  // BidTransactionDao specific methods
  // ─────────────────────────────────────────────

  @Override
  public List<BidTransaction> findByAuctionId(Long auctionId) {
    List<BidTransaction> list = new ArrayList<>();
    String sql = "SELECT * FROM bid_transactions WHERE auction_id = ? ORDER BY timestamp ASC";
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setLong(1, auctionId);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) list.add(mapRow(rs));
      }
    } catch (SQLException e) {
      throw new AuctionException("Database error finding BidTransactions by auction ID", e);
    }
    return list;
  }

  @Override
  public List<BidTransaction> findByBidderId(Long bidderId) {
    List<BidTransaction> list = new ArrayList<>();
    String sql = "SELECT * FROM bid_transactions WHERE bidder_id = ? ORDER BY timestamp DESC";
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setLong(1, bidderId);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) list.add(mapRow(rs));
      }
    } catch (SQLException e) {
      throw new AuctionException("Database error finding BidTransactions by bidder ID", e);
    }
    return list;
  }

  @Override
  public Optional<BidTransaction> findHighestBidByAuctionId(Long auctionId) {
    String sql = "SELECT * FROM bid_transactions WHERE auction_id = ? ORDER BY amount DESC LIMIT 1";
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setLong(1, auctionId);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) return Optional.of(mapRow(rs));
      }
    } catch (SQLException e) {
      throw new AuctionException("Database error finding highest bid for auction", e);
    }
    return Optional.empty();
  }

  @Override
  public long countByAuctionId(Long auctionId) {
    String sql = "SELECT COUNT(1) FROM bid_transactions WHERE auction_id = ?";
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setLong(1, auctionId);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next() ? rs.getLong(1) : 0L;
      }
    } catch (SQLException e) {
      throw new AuctionException("Database error counting bids for auction", e);
    }
  }

  // ─────────────────────────────────────────────
  // Helper methods
  // ─────────────────────────────────────────────

  private BidTransaction mapRow(ResultSet rs) throws SQLException {
    return new BidTransaction(
        rs.getLong("id"),
        rs.getTimestamp("created_at").toLocalDateTime(),
        rs.getLong("auction_id"),
        rs.getLong("bidder_id"),
        rs.getLong("amount"),
        rs.getTimestamp("timestamp").toLocalDateTime());
  }
}
