package com.auction.server.model.entity.user;

import com.auction.server.model.enums.UserRole;
import java.time.LocalDateTime;

public class Admin extends User {

  private int accessLevel;

  // Giúp trung gian tạo đối tượng rỗng trước khi truyền dữ liệu từ database vào
  public Admin() {
    super();
  }

  // Tạo (đăng ký) tài khoản Admin mới có level thấp nhất
  public Admin(String username, String passwordHash, String email) {
    super(username, passwordHash, email);
    this.accessLevel = 1;
  }

  // Tạo được admin có cấp độ cao hơn 1
  public Admin(String username, String passwordHash, String email, int accessLevel) {
    super(username, passwordHash, email);
    this.accessLevel = validateAccessLevel(accessLevel);
  }

  // Đăng nhập tài khoản Admin đã có khi load từ liệu từ Database về để khôi phục
  // tài khoản
  public Admin(
      Long id,
      LocalDateTime createdAt,
      String username,
      String passwordHash,
      String email,
      int accessLevel) {
    super(id, createdAt, username, passwordHash, email);
    this.accessLevel = validateAccessLevel(accessLevel);
  }

  // Khai báo bản thân để chương trình biết
  @Override
  public UserRole getRole() {
    return UserRole.ADMIN;
  }

  // Yêu cầu level Admin để thực hiện hành vi cấp cao
  public boolean hasPermission(int requiredLevel) {
    return this.accessLevel >= requiredLevel;
  }

  public int getAccessLevel() {
    return accessLevel;
  }

  public void setAccessLevel(int accessLevel) {
    this.accessLevel = validateAccessLevel(accessLevel);
  }

  // Chỉ cho phép Admin có level từ 1 đến 3
  // hàm này khai báo static để toàn bộ Admin dùng chung 1 hàm này thay vì tạo ra
  // n hàm cho n Admin, nó chỉ cần lấy level ra và so sánh, không quan tâm gì khác
  private static int validateAccessLevel(int level) {
    if (level < 1 || level > 3) {
      throw new IllegalArgumentException("Access level must be between 1 and 3: " + level);
    }
    return level;
  }

  @Override
  public String toString() {
    return "Admin{"
        + "id="
        + getId()
        + ", username='"
        + getUsername()
        + '\''
        + ", email='"
        + getEmail()
        + '\''
        + ", accessLevel="
        + accessLevel
        + ", createdAt="
        + getCreatedAt()
        + "}";
  }
}
