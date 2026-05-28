package server.model.entity;

import java.time.LocalDateTime;
import java.util.Objects;
import server.model.enums.TransactionActor;
import server.model.enums.WalletReferenceType;
import server.model.enums.WalletTransactionType;

public class WalletTransaction extends BaseEntity {
  private final Long userId;
  private final WalletTransactionType type;
  private final Long amount;
  private final Long balanceBefore;
  private final Long balanceAfter;
  private final Long referenceId;
  private final WalletReferenceType referenceType;
  private final String description;
  private final TransactionActor createdBy;

  public WalletTransaction(
      Long userId,
      WalletTransactionType type,
      Long amount,
      Long balanceBefore,
      Long balanceAfter,
      Long referenceId,
      WalletReferenceType referenceType,
      String description,
      TransactionActor createBy) {
    super();
    this.userId = Objects.requireNonNull(userId, "userId must not be null");
    this.type = Objects.requireNonNull(type, "type must not be null");
    if (amount < 0) throw new IllegalArgumentException("amount must be positive: " + amount);
    this.amount = amount;
    this.balanceBefore = balanceBefore;
    this.balanceAfter = balanceAfter;
    this.referenceId = referenceId;
    this.referenceType = referenceType;
    this.description = description;
    this.createdBy = createBy;
  }

  public WalletTransaction(
      Long id,
      LocalDateTime createdAt,
      Long userId,
      WalletTransactionType type,
      Long amount,
      Long balanceBefore,
      Long balanceAfter,
      Long referenceId,
      WalletReferenceType referenceType,
      String description,
      TransactionActor createBy) {
    super(id, createdAt);
    this.userId = userId;
    this.type = type;
    this.amount = amount;
    this.balanceBefore = balanceBefore;
    this.balanceAfter = balanceAfter;
    this.referenceId = referenceId;
    this.referenceType = referenceType;
    this.description = description;
    this.createdBy = createBy;
  }

  @Override
  public String toString() {
    return "WalletTransaction{"
        + "id="
        + getId()
        + ", userId="
        + userId
        + ", type='"
        + type
        + '\''
        + ", amount="
        + amount
        + ", balanceBefore="
        + balanceBefore
        + ", balanceAfter="
        + balanceAfter
        + ", referenceId="
        + referenceId
        + ", referenceType='"
        + referenceType
        + '\''
        + ", description='"
        + description
        + '\''
        + ", createdBy='"
        + createdBy
        + '\''
        + ", createdAt="
        + getCreatedAt().toString()
        + '}';
  }

  public Long getUserId() {
    return userId;
  }

  public WalletTransactionType getType() {
    return type;
  }

  public Long getAmount() {
    return amount;
  }

  public Long getBalanceBefore() {
    return balanceBefore;
  }

  public Long getBalanceAfter() {
    return balanceAfter;
  }

  public Long getReferenceId() {
    return referenceId;
  }

  public WalletReferenceType getReferenceType() {
    return referenceType;
  }

  public String getDescription() {
    return description;
  }

  public TransactionActor getCreatedBy() {
    return createdBy;
  }
}
