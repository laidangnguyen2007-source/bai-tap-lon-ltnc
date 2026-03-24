package com.auction.server.model.entity.user;

import com.auction.server.model.enums.UserRole;
import java.time.LocalDateTime;

public class Bidder extends User {

  private double balance;

  public Bidder() {
    super();
  }

  public Bidder(String username, String passwordHash, String email, double balance) {
    super(username, passwordHash, email);
    this.balance = balance;
  }

  public Bidder(Long id, LocalDateTime createdAt, String username, String passwordHash, String email, double balance) {
    super(id, createdAt, username, passwordHash, email);
    this.balance = balance;
  }

  @Override
  public UserRole getRole() {
    return UserRole.BIDDER;
  }

  public double getBalance() {
    return balance;
  }

  public void setBalance(double balance) {
    if (balance < 0) {
      throw new IllegalArgumentException("Balance must not be negative: " + balance);
    }
    this.balance = balance;
  }

  public void deductBalance(double amount) {
    if (amount <= 0) {
      throw new IllegalArgumentException("Deduction amount must be positive: " + amount);
    }
    if (amount > balance) {
      throw new IllegalArgumentException("Insufficient balance. Required: " + amount + ", available: " + balance);
    }
    this.balance -= amount;
  }

  @Override
  public String toString() {
    return "Bidder{id=" + getId() 
        + ", username='" + getUsername() + '\'' 
        + ", email='" + getEmail() + '\'' 
        + ", balance=" + balance 
        + ", createdAt=" + getCreatedAt() 
        + "}";
  }
}