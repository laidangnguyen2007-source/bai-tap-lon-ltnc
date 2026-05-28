package server.model.entity.user;

import java.time.LocalDateTime;
import server.model.enums.UserRole;

public class Bidder extends User {
  private long balance;

  public Bidder() {
    super();
  }

  public Bidder(String username, String passwordHash, String email, long balance) {
    super(username, passwordHash, email);
    this.balance = balance;
  }

  public Bidder(
      Long id,
      LocalDateTime createdAt,
      String username,
      String passwordHash,
      String email,
      long balance) {
    super(id, createdAt, username, passwordHash, email);
    this.balance = balance;
  }

  @Override
  public UserRole getRole() {
    return UserRole.BIDDER;
  }

  public long getBalance() {
    return balance;
  }

  public void setBalance(long balance) {
    if (balance < 0) {
      throw new IllegalArgumentException("Số dư không được là số âm: " + balance);
    }
    this.balance = balance;
  }

  public void deductBalance(long amount) {
    if (amount <= 0) {
      throw new IllegalArgumentException("Số tiền trừ phải lớn hơn 0: " + amount);
    }
    if (amount > balance) {
      throw new IllegalArgumentException(
          "Số dư không đủ. Yêu cầu: " + amount + ", hiện có: " + balance);
    }
    this.balance -= amount;
  }

  @Override
  public String toString() {
    return "Bidder{id="
        + getId()
        + ", username='"
        + getUsername()
        + '\''
        + ", email='"
        + getEmail()
        + '\''
        + ", balance="
        + balance
        + ", createdAt="
        + getCreatedAt()
        + "}";
  }
}
