package server.dao.impl;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import server.config.DatabaseConfig;
import server.dao.WalletTransactionDao;
import server.model.entity.WalletTransaction;
import server.model.enums.WalletReferenceType;
import server.model.enums.WalletTransactionType;
import server.model.exception.AuctionException;

public class JdbcWalletTransactionDao implements WalletTransactionDao {

  public JdbcWalletTransactionDao() {}

  // ─────────────────────────────────────────────
  // SAVE
  // ─────────────────────────────────────────────
  @Override
  public WalletTransaction save(WalletTransaction tx) {

    String sql =
        "INSERT INTO wallet_transactions "
            + "(user_id, type, amount, balance_before, balance_after, "
            + "reference_id, reference_type, description, created_by, created_at) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    try (PreparedStatement ps =
        DatabaseConfig.getInstance()
            .getConnection()
            .prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

      ps.setLong(1, tx.getUserId());
      ps.setString(2, tx.getType().name());
      ps.setLong(3, tx.getAmount());
      ps.setLong(4, tx.getBalanceBefore());
      ps.setLong(5, tx.getBalanceAfter());
      ps.setObject(6, tx.getReferenceId());
      ps.setString(7, tx.getReferenceType().name());
      ps.setString(8, tx.getDescription());
      ps.setString(9, tx.getCreatedBy().name());
      ps.setTimestamp(10, Timestamp.valueOf(tx.getCreatedAt()));

      ps.executeUpdate();

      try (ResultSet rs = ps.getGeneratedKeys()) {
        if (rs.next()) {
          tx.setId(rs.getLong(1));
        }
      }

      return tx;

    } catch (SQLException e) {
      throw new AuctionException("Error saving WalletTransaction", e);
    }
  }

  // ─────────────────────────────────────────────
  // FIND BY USER
  // ─────────────────────────────────────────────
  @Override
  public List<WalletTransaction> findByUserId(Long userId) {

    List<WalletTransaction> list = new ArrayList<>();

    String sql =
        "SELECT * FROM wallet_transactions " + "WHERE user_id = ? ORDER BY created_at DESC";

    try (PreparedStatement ps =
        DatabaseConfig.getInstance().getConnection().prepareStatement(sql)) {

      ps.setLong(1, userId);

      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          list.add(mapRow(rs));
        }
      }

    } catch (SQLException e) {
      throw new AuctionException("Error finding WalletTransaction by userId", e);
    }

    return list;
  }

  // ─────────────────────────────────────────────
  // FIND BY REFERENCE
  // ─────────────────────────────────────────────
  @Override
  public List<WalletTransaction> findByReference(Long refId, WalletReferenceType refType) {

    List<WalletTransaction> list = new ArrayList<>();

    String sql =
        "SELECT * FROM wallet_transactions "
            + "WHERE reference_id = ? AND reference_type = ? "
            + "ORDER BY created_at DESC";

    try (PreparedStatement ps =
        DatabaseConfig.getInstance().getConnection().prepareStatement(sql)) {

      ps.setLong(1, refId);
      ps.setString(2, refType.name());

      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          list.add(mapRow(rs));
        }
      }

    } catch (SQLException e) {
      throw new AuctionException("Error finding WalletTransaction by reference", e);
    }

    return list;
  }

  // ─────────────────────────────────────────────
  // FIND BY TYPE
  // ─────────────────────────────────────────────
  @Override
  public List<WalletTransaction> findByType(WalletTransactionType type) {

    List<WalletTransaction> list = new ArrayList<>();

    String sql = "SELECT * FROM wallet_transactions " + "WHERE type = ? ORDER BY created_at DESC";

    try (PreparedStatement ps =
        DatabaseConfig.getInstance().getConnection().prepareStatement(sql)) {

      ps.setString(1, type.name());

      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          list.add(mapRow(rs));
        }
      }

    } catch (SQLException e) {
      throw new AuctionException("Error finding WalletTransaction by type", e);
    }

    return list;
  }

  // ─────────────────────────────────────────────
  // FIND ALL (ADMIN)
  // ─────────────────────────────────────────────
  @Override
  public List<WalletTransaction> findAll() {

    List<WalletTransaction> list = new ArrayList<>();

    String sql = "SELECT * FROM wallet_transactions ORDER BY created_at DESC";

    try (Statement st = DatabaseConfig.getInstance().getConnection().createStatement();
        ResultSet rs = st.executeQuery(sql)) {

      while (rs.next()) {
        list.add(mapRow(rs));
      }

    } catch (SQLException e) {
      throw new AuctionException("Error fetching all WalletTransactions", e);
    }

    return list;
  }

  // ─────────────────────────────────────────────
  // MAPPING
  // ─────────────────────────────────────────────
  private WalletTransaction mapRow(ResultSet rs) throws SQLException {
    Timestamp ts = rs.getTimestamp("created_at");
    LocalDateTime createdAt = ts != null ? ts.toLocalDateTime() : null;

    long refIdVal = rs.getLong("reference_id");
    Long referenceId = rs.wasNull() ? null : refIdVal;

    String refTypeStr = rs.getString("reference_type");
    WalletReferenceType referenceType =
        refTypeStr != null ? WalletReferenceType.valueOf(refTypeStr) : null;

    String createdByStr = rs.getString("created_by");
    server.model.enums.TransactionActor createdBy =
        createdByStr != null ? server.model.enums.TransactionActor.valueOf(createdByStr) : null;

    return new WalletTransaction(
        rs.getLong("id"),
        createdAt,
        rs.getLong("user_id"),
        WalletTransactionType.valueOf(rs.getString("type")),
        rs.getLong("amount"),
        rs.getLong("balance_before"),
        rs.getLong("balance_after"),
        referenceId,
        referenceType,
        rs.getString("description"),
        createdBy);
  }

  // ─────────────────────────────────────────────
  // GenericDao methods (optional override)
  // ─────────────────────────────────────────────
  @Override
  public Optional<WalletTransaction> findById(Long id) {
    throw new UnsupportedOperationException("Use custom queries");
  }

  @Override
  public WalletTransaction update(WalletTransaction entity) {
    throw new UnsupportedOperationException("WalletTransaction is immutable");
  }

  @Override
  public boolean deleteById(Long id) {

    String sql = "DELETE FROM wallet_transactions WHERE id = ?";

    try (PreparedStatement ps =
        DatabaseConfig.getInstance().getConnection().prepareStatement(sql)) {

      ps.setLong(1, id);
      return ps.executeUpdate() > 0;

    } catch (SQLException e) {
      throw new AuctionException("Error deleting WalletTransaction", e);
    }
  }

  @Override
  public boolean existsById(Long id) {

    String sql = "SELECT COUNT(1) FROM wallet_transactions WHERE id = ?";

    try (PreparedStatement ps =
        DatabaseConfig.getInstance().getConnection().prepareStatement(sql)) {

      ps.setLong(1, id);

      try (ResultSet rs = ps.executeQuery()) {
        return rs.next() && rs.getLong(1) > 0;
      }

    } catch (SQLException e) {
      throw new AuctionException("Error checking WalletTransaction existence", e);
    }
  }

  @Override
  public long count() {

    String sql = "SELECT COUNT(1) FROM wallet_transactions";

    try (Statement st = DatabaseConfig.getInstance().getConnection().createStatement();
        ResultSet rs = st.executeQuery(sql)) {

      return rs.next() ? rs.getLong(1) : 0;

    } catch (SQLException e) {
      throw new AuctionException("Error counting WalletTransactions", e);
    }
  }
}
