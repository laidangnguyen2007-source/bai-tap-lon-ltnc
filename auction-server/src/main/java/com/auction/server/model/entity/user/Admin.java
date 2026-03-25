package com.auction.server.model.entity.user;

import com.auction.server.model.enums.UserRole;
import java.time.LocalDateTime;

public class Admin extends User {

  private int accessLevel;

  public Admin() {
    super();
  }

  public Admin(String username, String passwordHash, String email) {
    super(username, passwordHash, email);
    this.accessLevel = 1;
  }

  public Admin(String username, String passwordHash, String email, int accessLevel) {
    super(username, passwordHash, email);
    this.accessLevel = validateAccessLevel(accessLevel);
  }

  public Admin(Long id, LocalDateTime createdAt, String username, String passwordHash, String email, int accessLevel) {
    super(id, createdAt, username, passwordHash, email);
    this.accessLevel = validateAccessLevel(accessLevel);
  }

  @Override
  public UserRole getRole() {
    return UserRole.ADMIN;
  }

  public boolean hasPermission(int requiredLevel) {
    return this.accessLevel >= requiredLevel;
  }

  public int getAccessLevel() {
    return accessLevel;
  }

  public void setAccessLevel(int accessLevel) {
    this.accessLevel = validateAccessLevel(accessLevel);
  }

  private static int validateAccessLevel(int level) {
    if (level < 1 || level > 3) {
      throw new IllegalArgumentException("Access level must be between 1 and 3: " + level);
    }
    return level;
  }

  @Override
  public String toString() {
    return "Admin{"
        + "id=" + getId()
        + ", username='" + getUsername() + '\''
        + ", email='" + getEmail() + '\''
        + ", accessLevel=" + accessLevel
        + ", createdAt=" + getCreatedAt()
        + "}";
  }
}