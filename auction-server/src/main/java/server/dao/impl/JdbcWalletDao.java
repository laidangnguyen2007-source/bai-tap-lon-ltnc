package server.dao.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import server.config.DatabaseConfig;
import server.dao.WalletDao;
import server.model.entity.Wallet;
import server.model.exception.AuctionException;

public class JdbcWalletDao implements WalletDao {

  public JdbcWalletDao() {}
  ;

  // ─────────────────────────────────────────────
  // LOCKED READ (FOR UPDATE)
  // ─────────────────────────────────────────────
  @Override
  public Wallet findWalletWithLock(Long userId) {
    Connection connection = null;
    boolean originalAutoCommit = true;
    try {
      connection = DatabaseConfig.getInstance().getConnection();
      originalAutoCommit = connection.getAutoCommit();
      connection.setAutoCommit(false);

      String sql = "SELECT * FROM wallets where user_id = ? FOR UPDATE";
      try (PreparedStatement ps = connection.prepareStatement(sql)) {
        ps.setLong(1, userId);
        try (ResultSet rs = ps.executeQuery()) {
          if (rs.next()) return mapRow(rs);
        }
        return null;
      }
    } catch (SQLException e) {
      throw new AuctionException(
          "Error finding wallet with row lock for userId " + userId + ": " + e.getMessage(), e);
    } finally {
      if (connection != null) {
        try {
          connection.setAutoCommit(originalAutoCommit);
        } catch (SQLException ignored) {
        }
      }
    }
  }

  // ─────────────────────────────────────────────
  // READ ONLY (NO LOCK)
  // ─────────────────────────────────────────────
  @Override
  public Wallet findByUserIdNoLock(Long userId) {
    String sql = "SELECT * FROM wallets WHERE user_id = ?";

    try (PreparedStatement ps =
        DatabaseConfig.getInstance().getConnection().prepareStatement(sql)) {
      ps.setLong(1, userId);

      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) return mapRow(rs);
      }

      return null;
    } catch (SQLException e) {
      throw new AuctionException("Error finding wallet (no lock)", e);
    }
  }

  // ─────────────────────────────────────────────
  // ATOMIC UPDATE
  // ─────────────────────────────────────────────
  @Override
  public void updateBalance(Wallet wallet) {

    String sql =
        "UPDATE wallets "
            + "SET available_balance = ?, "
            + "    locked_balance = ?, "
            + "    total_balance = available_balance + locked_balance "
            + "WHERE user_id = ?";

    try (PreparedStatement ps =
        DatabaseConfig.getInstance().getConnection().prepareStatement(sql)) {

      ps.setLong(1, wallet.getAvailableBalance());
      ps.setLong(2, wallet.getLockedBalance());
      ps.setLong(3, wallet.getUserId());

      int affected = ps.executeUpdate();

      if (affected == 0) {
        throw new AuctionException("Wallet update failed: user not found");
      }

    } catch (SQLException e) {
      throw new AuctionException("Error updating wallet balance", e);
    }
  }

  // ─────────────────────────────────────────────
  // CREATE WALLET
  // ─────────────────────────────────────────────
  @Override
  public void createWallet(Long userId) {

    String sql =
        "INSERT INTO wallets (user_id, available_balance, locked_balance, total_balance) "
            + "VALUES (?, 0, 0, 0)";

    try (PreparedStatement ps =
        DatabaseConfig.getInstance().getConnection().prepareStatement(sql)) {

      ps.setLong(1, userId);
      ps.executeUpdate();

    } catch (SQLException e) {
      throw new AuctionException("Error creating wallet", e);
    }
  }

  // ─────────────────────────────────────────────
  // FIND ALL (ADMIN)
  // ─────────────────────────────────────────────
  @Override
  public List<Wallet> findAll() {

    List<Wallet> list = new ArrayList<>();

    String sql = "SELECT * FROM wallets";

    try (Statement st = DatabaseConfig.getInstance().getConnection().createStatement();
        ResultSet rs = st.executeQuery(sql)) {

      while (rs.next()) {
        list.add(mapRow(rs));
      }

    } catch (SQLException e) {
      throw new AuctionException("Error fetching all wallets", e);
    }

    return list;
  }

  // ─────────────────────────────────────────────
  // GENERIC DAO METHODS (optional if not used)
  // ─────────────────────────────────────────────

  @Override
  public Wallet save(Wallet entity) {
    throw new UnsupportedOperationException("Use createWallet instead");
  }

  @Override
  public Optional<Wallet> findById(Long id) {
    throw new UnsupportedOperationException("Use findWalletWithLock instead");
  }

  @Override
  public Wallet update(Wallet entity) {
    throw new UnsupportedOperationException("Use updateBalance instead");
  }

  @Override
  public boolean deleteById(Long id) {
    String sql = "DELETE FROM wallets WHERE user_id = ?";

    try (PreparedStatement ps =
        DatabaseConfig.getInstance().getConnection().prepareStatement(sql)) {

      ps.setLong(1, id);
      return ps.executeUpdate() > 0;

    } catch (SQLException e) {
      throw new AuctionException("Error deleting wallet", e);
    }
  }

  @Override
  public boolean existsById(Long id) {
    String sql = "SELECT COUNT(1) FROM wallets WHERE user_id = ?";

    try (PreparedStatement ps =
        DatabaseConfig.getInstance().getConnection().prepareStatement(sql)) {

      ps.setLong(1, id);

      try (ResultSet rs = ps.executeQuery()) {
        return rs.next() && rs.getLong(1) > 0;
      }

    } catch (SQLException e) {
      throw new AuctionException("Error checking wallet existence", e);
    }
  }

  @Override
  public long count() {
    String sql = "SELECT COUNT(1) FROM wallets";

    try (Statement st = DatabaseConfig.getInstance().getConnection().createStatement();
        ResultSet rs = st.executeQuery(sql)) {

      return rs.next() ? rs.getLong(1) : 0;

    } catch (SQLException e) {
      throw new AuctionException("Error counting wallets", e);
    }
  }

  // ─────────────────────────────────────────────
  // MAPPING
  // ─────────────────────────────────────────────
  private Wallet mapRow(ResultSet rs) throws SQLException {

    Wallet wallet = new Wallet();

    wallet.setId(rs.getLong("id"));
    wallet.setUserId(rs.getLong("user_id"));

    wallet.setAvailableBalance(rs.getLong("available_balance"));
    wallet.setLockedBalance(rs.getLong("locked_balance"));

    wallet.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
    wallet.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());

    return wallet;
  }
}
