package com.auction.server.model.entity.user;

import com.auction.server.model.entity.BaseEntity;
import com.auction.server.model.enums.UserRole;
import java.time.LocalDateTime;
import java.util.Objects;

public abstract class User extends BaseEntity {

  private String username;
  private String passwordHash;
  private String email;

  protected User() {
    super();
  }

  protected User(String username, String passwordHash, String email) {
    super();
    this.username = Objects.requireNonNull(username, "username must not be null");
    this.passwordHash = Objects.requireNonNull(passwordHash, "passwordHash must not be null");
    this.email = Objects.requireNonNull(email, "email must not be null");
  }

  protected User(
      Long id, LocalDateTime createdAt, String username, String passwordHash, String email) {
    super(id, createdAt);
    this.username = username;
    this.passwordHash = passwordHash;
    this.email = email;
  }

  public abstract UserRole getRole();

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = Objects.requireNonNull(username, "username must not be null");
  }

  public String getPasswordHash() {
    return passwordHash;
  }

  public void setPasswordHash(String passwordHash) {
    this.passwordHash = Objects.requireNonNull(passwordHash, "passwordHash must not be null");
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = Objects.requireNonNull(email, "email must not be null");
  }

  @Override
  public String toString() {
    return getClass().getSimpleName()
        + "{id="
        + getId()
        + ", username='"
        + username
        + '\''
        + ", email='"
        + email
        + '\''
        + ", role="
        + getRole()
        + ", createdAt="
        + getCreatedAt()
        + "}";
  }
}
