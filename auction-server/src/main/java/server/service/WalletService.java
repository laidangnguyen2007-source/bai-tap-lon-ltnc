package server.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import server.config.DatabaseConfig;
import server.dao.WalletDao;
import server.dao.WalletTransactionDao;
import server.model.entity.Wallet;
import server.model.entity.WalletTransaction;
import server.model.enums.TransactionActor;
import server.model.enums.WalletReferenceType;
import server.model.enums.WalletTransactionType;
import server.model.exception.AuctionException;
import server.model.strategy.AutoBidStrategy;
import server.model.entity.Auction;

public class WalletService {

  private final Connection connection;
  private final WalletDao walletDao;
  private final WalletTransactionDao walletTransactionDao;

  public WalletService(WalletDao walletDao,
                       WalletTransactionDao walletTransactionDao) throws SQLException {
    this.connection = DatabaseConfig.getInstance().getConnection();
    this.walletDao = walletDao;
    this.walletTransactionDao = walletTransactionDao;
  }

  // ─── HELPER ───
  private Wallet getOrCreateWalletWithLock(Long userId) {
    Wallet wallet = walletDao.findWalletWithLock(userId);
    if (wallet == null) {
      walletDao.createWallet(userId);
      wallet = walletDao.findWalletWithLock(userId);
      if (wallet == null) {
        throw new AuctionException("Failed to auto-create wallet for user " + userId);
      }
    }
    return wallet;
  }

  // ─── BID LOCK ───
  public void lockForBid(Long userId, Long auctionId, long amount) {
    try {
      connection.setAutoCommit(false);

      Wallet wallet = getOrCreateWalletWithLock(userId);

      if (wallet.getAvailableBalance() < amount) {
        throw new AuctionException("Insufficient balance");
      }

      long before = wallet.getAvailableBalance();

      wallet.lockFunds(amount);
      walletDao.updateBalance(wallet);

      WalletTransaction tx = new WalletTransaction(
          userId,
          WalletTransactionType.BID_LOCK,
          amount,
          before,
          wallet.getAvailableBalance(),
          auctionId,
          WalletReferenceType.AUCTION,
          "Lock funds for auction #" + auctionId,
          TransactionActor.USER
      );

      walletTransactionDao.save(tx);

      connection.commit();

    } catch (Exception e) {
      try { connection.rollback(); } catch (Exception ignored) {}
      throw new AuctionException(e.getMessage(), e);
    } finally {
      try { connection.setAutoCommit(true); } catch (Exception ignored) {}
    }
  }

  // ─── BID RELEASE ───
  public void releaseForOutbid(Long userId, Long auctionId, long amount) {
    try {
      connection.setAutoCommit(false);

      Wallet wallet = getOrCreateWalletWithLock(userId);

      long before = wallet.getAvailableBalance();

      wallet.releaseFunds(amount);
      walletDao.updateBalance(wallet);

      WalletTransaction tx = new WalletTransaction(
          userId,
          WalletTransactionType.BID_RELEASE,
          amount,
          before,
          wallet.getAvailableBalance(),
          auctionId,
          WalletReferenceType.AUCTION,
          "Release funds (outbid) auction #" + auctionId,
          TransactionActor.SYSTEM
      );

      walletTransactionDao.save(tx);

      connection.commit();

    } catch (Exception e) {
      try { connection.rollback(); } catch (Exception ignored) {}
      throw new AuctionException(e.getMessage(), e);
    } finally {
      try { connection.setAutoCommit(true); } catch (Exception ignored) {}
    }
  }

  // ─── AUTO BID LOCK ───
  public void lockForAutoBid(Long userId, Long auctionId, long maxBidAmount) {
    try {
      connection.setAutoCommit(false);

      Wallet wallet = getOrCreateWalletWithLock(userId);

      if (wallet.getAvailableBalance() < maxBidAmount) {
        throw new AuctionException("Insufficient balance for auto-bid");
      }

      long before = wallet.getAvailableBalance();

      wallet.lockFunds(maxBidAmount);
      walletDao.updateBalance(wallet);

      WalletTransaction tx = new WalletTransaction(
          userId,
          WalletTransactionType.AUTO_BID_LOCK,
          maxBidAmount,
          before,
          wallet.getAvailableBalance(),
          auctionId,
          WalletReferenceType.AUTO_BID,
          "Lock max auto-bid funds",
          TransactionActor.USER
      );

      walletTransactionDao.save(tx);

      connection.commit();

    } catch (Exception e) {
      try { connection.rollback(); } catch (Exception ignored) {}
      throw new AuctionException(e.getMessage(), e);
    } finally {
      try { connection.setAutoCommit(true); } catch (Exception ignored) {}
    }
  }

  // ─── AUTO BID RELEASE ───
  public void releaseAutoBid(Long userId, Long auctionId, long releaseAmount) {
    try {
      connection.setAutoCommit(false);

      Wallet wallet = getOrCreateWalletWithLock(userId);

      long before = wallet.getAvailableBalance();

      wallet.releaseFunds(releaseAmount);
      walletDao.updateBalance(wallet);

      WalletTransaction tx = new WalletTransaction(
          userId,
          WalletTransactionType.AUTO_BID_RELEASE,
          releaseAmount,
          before,
          wallet.getAvailableBalance(),
          auctionId,
          WalletReferenceType.AUTO_BID,
          "Release unused auto-bid funds",
          TransactionActor.SYSTEM
      );

      walletTransactionDao.save(tx);

      connection.commit();

    } catch (Exception e) {
      try { connection.rollback(); } catch (Exception ignored) {}
      throw new AuctionException(e.getMessage(), e);
    } finally {
      try { connection.setAutoCommit(true); } catch (Exception ignored) {}
    }
  }

  // ─── SETTLE AUCTION ───
  public void settleAuction(Auction auction, List<AutoBidStrategy> autoBids) {
    try {
      connection.setAutoCommit(false);
      Long winnerId = auction.getCurrentWinnerId();
      long price = auction.getCurrentPrice();

      if (winnerId != null && price > 0) {
        // 1. Deduct from winner
        Wallet winnerWallet = getOrCreateWalletWithLock(winnerId);
        long wBefore = winnerWallet.getAvailableBalance();
        winnerWallet.deductLocked(price);
        walletDao.updateBalance(winnerWallet);
        WalletTransaction wTx = new WalletTransaction(
            winnerId, WalletTransactionType.AUCTION_WIN, price, wBefore, winnerWallet.getAvailableBalance(),
            auction.getId(), WalletReferenceType.AUCTION, "Auction Win Deduction", TransactionActor.SYSTEM
        );
        walletTransactionDao.save(wTx);

        // 2. Add to seller
        Wallet sellerWallet = getOrCreateWalletWithLock(auction.getSellerId());
        long sBefore = sellerWallet.getAvailableBalance();
        sellerWallet.addAvailable(price);
        walletDao.updateBalance(sellerWallet);
        WalletTransaction sTx = new WalletTransaction(
            auction.getSellerId(), WalletTransactionType.SELLER_PAYOUT, price, sBefore, sellerWallet.getAvailableBalance(),
            auction.getId(), WalletReferenceType.AUCTION, "Auction Payout", TransactionActor.SYSTEM
        );
        walletTransactionDao.save(sTx);
      }

      // 3. Release AutoBids
      for (AutoBidStrategy strategy : autoBids) {
        Long autoBidderId = strategy.getUserId();
        long releaseAmount = strategy.getMaxBid();
        
        if (winnerId != null && winnerId.equals(autoBidderId)) {
            releaseAmount -= price; 
        }

        if (releaseAmount > 0) {
            Wallet autoWallet = getOrCreateWalletWithLock(autoBidderId);
            long aBefore = autoWallet.getAvailableBalance();
            autoWallet.releaseFunds(releaseAmount);
            walletDao.updateBalance(autoWallet);
            WalletTransaction aTx = new WalletTransaction(
                autoBidderId, WalletTransactionType.AUTO_BID_RELEASE, releaseAmount, aBefore, autoWallet.getAvailableBalance(),
                auction.getId(), WalletReferenceType.AUTO_BID, "Release unused auto-bid funds", TransactionActor.SYSTEM
            );
            walletTransactionDao.save(aTx);
        }
      }

      connection.commit();
    } catch (Exception e) {
      try { connection.rollback(); } catch (Exception ignored) {}
      throw new AuctionException("Error settling auction: " + e.getMessage(), e);
    } finally {
      try { connection.setAutoCommit(true); } catch (Exception ignored) {}
    }
  }

  // ─── ADMIN ADJUST ───
  public void adminAdjustBalance(Long userId,
                                 long amount,
                                 Long adminId,
                                 String description) {
    try {
      connection.setAutoCommit(false);

      Wallet wallet = getOrCreateWalletWithLock(userId);

      long before = wallet.getAvailableBalance();

      wallet.addAvailable(amount);
      walletDao.updateBalance(wallet);

      WalletTransaction tx = new WalletTransaction(
          userId,
          WalletTransactionType.ADMIN_ADJUSTMENT,
          amount,
          before,
          wallet.getAvailableBalance(),
          userId,
          WalletReferenceType.ADMIN,
          description,
          TransactionActor.ADMIN
      );

      walletTransactionDao.save(tx);

      connection.commit();

    } catch (Exception e) {
      try { connection.rollback(); } catch (Exception ignored) {}
      throw new AuctionException(e.getMessage(), e);
    } finally {
      try { connection.setAutoCommit(true); } catch (Exception ignored) {}
    }
  }

  // ─── GET WALLET ───
  public Wallet getWallet(Long userId) {
    return walletDao.findByUserIdNoLock(userId);
  }

  // ─── GET TRANSACTIONS ───
  public List<WalletTransaction> getTransactions(Long userId) {
    return walletTransactionDao.findByUserId(userId);
  }

  // ─── CREATE WALLET ───
  public void createWallet(Long userId) {
    walletDao.createWallet(userId);
  }
}