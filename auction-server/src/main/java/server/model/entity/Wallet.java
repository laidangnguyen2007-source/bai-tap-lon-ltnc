package server.model.entity;

import java.beans.Transient;
import java.time.LocalDateTime;

public class Wallet extends BaseEntity {
  private Long userId;
  private Long totalBalance;
  private Long availableBalance;
  private Long lockedBalance;
  private LocalDateTime updatedAt;

  public Wallet() {};
  public Wallet(Long id, Long userId, Long totalBalance,
      Long availableBalance, Long lockedBalanace, LocalDateTime createdAt, LocalDateTime updatedAt) {
    super(id, createdAt);
    this.userId = userId;
    this.totalBalance = totalBalance;
    this.availableBalance = availableBalance;
    this.lockedBalance = lockedBalanace;
    this.updatedAt = updatedAt;
  }

  public void lockFunds(Long amount) {
    validatePositiveAmount(amount);

    if (availableBalance + amount < 0) throw new IllegalArgumentException("Insufficient available balance");

    availableBalance -= amount;
    lockedBalance += amount;
  }

  public void releaseFunds(Long amount) {
    validatePositiveAmount(amount);

    if (lockedBalance < 0) throw new IllegalArgumentException("Insufficient locked balance!");

    lockedBalance -= amount;
    availableBalance += amount;
  }

  public void deductLocked(Long amount) {
    validatePositiveAmount(amount);

    if (lockedBalance < amount) throw new IllegalArgumentException("Insufficient locked balance!");

    lockedBalance -= amount;
  }

  public void addAvailable(Long amount) {
    validatePositiveAmount(amount);

    availableBalance += amount;
  }

  @Transient
  public Long getTotalBalance() {
      return availableBalance + lockedBalance;
  }

  public void validatePositiveAmount(Long amount) {
    if (amount == null) throw new IllegalArgumentException("Amount cannot be null!");
    if (amount <= 0) throw new IllegalArgumentException("Amount must be greater than zero!");
  }

  public boolean canAfford(Long amount) {
    validatePositiveAmount(amount);

    return availableBalance >= amount;
  }

  public boolean hasLockedFunds(Long amount) {
    validatePositiveAmount(amount);

    return lockedBalance >= amount;
  }

  public Long getUserId() {
    return userId;
  }

  public void setUserId(Long userId) {
    this.userId = userId;
  }

  public void setTotalBalance(Long totalBalance) {
    this.totalBalance = totalBalance;
  }

  public Long getAvailableBalance() {
    return availableBalance;
  }

  public void setAvailableBalance(Long availableBalance) {
    this.availableBalance = availableBalance;
  }

  public Long getLockedBalance() {
    return lockedBalance;
  }

  public void setLockedBalance(Long lockedBalance) {
    this.lockedBalance = lockedBalance;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(LocalDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }

  
}
