package server.dao.impl;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import server.config.DatabaseConfig;
import server.dao.AutoBidDao;
import server.model.entity.AutoBid;
import server.model.exception.AuctionException;

public class JdbcAutoBidDao implements AutoBidDao {

  public JdbcAutoBidDao() {}

  // ─────────────────────────────────────────────
  // SAVE
  // ─────────────────────────────────────────────
  @Override
  public AutoBid save(AutoBid autoBid) {

    String sql =
        "INSERT INTO auto_bids "
            + "(auction_id, bidder_id, max_bid, increment, is_active, created_at) "
            + "VALUES (?, ?, ?, ?, ?, ?) "
            + "ON DUPLICATE KEY UPDATE max_bid = VALUES(max_bid), increment = VALUES(increment), is_active = VALUES(is_active), created_at = VALUES(created_at)";

    try (PreparedStatement ps =
        DatabaseConfig.getInstance()
            .getConnection()
            .prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

      ps.setLong(1, autoBid.getAuctionId());
      ps.setLong(2, autoBid.getBidderId());
      ps.setLong(3, autoBid.getMaxBid());
      ps.setLong(4, autoBid.getIncrement());
      ps.setBoolean(5, autoBid.isActive());
      ps.setTimestamp(6, Timestamp.valueOf(autoBid.getCreatedAt()));

      ps.executeUpdate();

      try (ResultSet rs = ps.getGeneratedKeys()) {
        if (rs.next()) {
          autoBid.setId(rs.getLong(1));
        }
      }

      return autoBid;

    } catch (SQLException e) {
      throw new AuctionException("Error saving AutoBid", e);
    }
  }

  // ─────────────────────────────────────────────
  // FIND BY AUCTION + BIDDER
  // ─────────────────────────────────────────────
  @Override
  public Optional<AutoBid> findByAuctionAndBidder(Long auctionId, Long bidderId) {

    String sql = "SELECT * FROM auto_bids WHERE auction_id = ? AND bidder_id = ?";

    try (PreparedStatement ps =
        DatabaseConfig.getInstance().getConnection().prepareStatement(sql)) {

      ps.setLong(1, auctionId);
      ps.setLong(2, bidderId);

      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return Optional.of(mapRow(rs));
        }
      }

    } catch (SQLException e) {
      throw new AuctionException("Error finding AutoBid by auction and bidder", e);
    }

    return Optional.empty();
  }

  // ─────────────────────────────────────────────
  // ACTIVE BY AUCTION
  // ─────────────────────────────────────────────
  @Override
  public List<AutoBid> findActiveByAuction(Long auctionId) {

    List<AutoBid> list = new ArrayList<>();

    String sql = "SELECT * FROM auto_bids WHERE auction_id = ? AND is_active = true";

    try (PreparedStatement ps =
        DatabaseConfig.getInstance().getConnection().prepareStatement(sql)) {

      ps.setLong(1, auctionId);

      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          list.add(mapRow(rs));
        }
      }

    } catch (SQLException e) {
      throw new AuctionException("Error finding active AutoBids by auction", e);
    }

    return list;
  }

  // ─────────────────────────────────────────────
  // ACTIVE BY BIDDER
  // ─────────────────────────────────────────────
  @Override
  public List<AutoBid> findActiveByBidder(Long bidderId) {

    List<AutoBid> list = new ArrayList<>();

    String sql = "SELECT * FROM auto_bids WHERE bidder_id = ? AND is_active = true";

    try (PreparedStatement ps =
        DatabaseConfig.getInstance().getConnection().prepareStatement(sql)) {

      ps.setLong(1, bidderId);

      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          list.add(mapRow(rs));
        }
      }

    } catch (SQLException e) {
      throw new AuctionException("Error finding active AutoBids by bidder", e);
    }

    return list;
  }

  // ─────────────────────────────────────────────
  // DEACTIVATE BY ID
  // ─────────────────────────────────────────────
  @Override
  public void deactivate(Long id) {

    String sql = "UPDATE auto_bids SET is_active = false WHERE id = ?";

    try (PreparedStatement ps =
        DatabaseConfig.getInstance().getConnection().prepareStatement(sql)) {

      ps.setLong(1, id);
      ps.executeUpdate();

    } catch (SQLException e) {
      throw new AuctionException("Error deactivating AutoBid", e);
    }
  }

  // ─────────────────────────────────────────────
  // DEACTIVATE BY AUCTION
  // ─────────────────────────────────────────────
  @Override
  public void deactivateByAuction(Long auctionId) {

    String sql = "UPDATE auto_bids SET is_active = false WHERE auction_id = ?";

    try (PreparedStatement ps =
        DatabaseConfig.getInstance().getConnection().prepareStatement(sql)) {

      ps.setLong(1, auctionId);
      ps.executeUpdate();

    } catch (SQLException e) {
      throw new AuctionException("Error deactivating AutoBids by auction", e);
    }
  }

  // ─────────────────────────────────────────────
  // MAPPER
  // ─────────────────────────────────────────────
  private AutoBid mapRow(ResultSet rs) throws SQLException {

    AutoBid autoBid = new AutoBid();

    autoBid.setId(rs.getLong("id"));
    autoBid.setAuctionId(rs.getLong("auction_id"));
    autoBid.setBidderId(rs.getLong("bidder_id"));
    autoBid.setMaxBid(rs.getLong("max_bid"));
    autoBid.setIncrement(rs.getLong("increment"));
    autoBid.setActive(rs.getBoolean("is_active"));
    autoBid.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());

    return autoBid;
  }

  @Override
  public Optional<AutoBid> findById(Long id) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'findById'");
  }

  @Override
  public List<AutoBid> findAll() {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'findAll'");
  }

  @Override
  public AutoBid update(AutoBid entity) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'update'");
  }

  @Override
  public boolean deleteById(Long id) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'deleteById'");
  }

  @Override
  public boolean existsById(Long id) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'existsById'");
  }

  @Override
  public long count() {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'count'");
  }
}
