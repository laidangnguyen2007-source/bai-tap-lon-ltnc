package server.model.entity.user;

import java.time.LocalDateTime;
import server.model.enums.UserRole;

public class Seller extends User {

  private String shopName;
  private double rating; // đánh giá khách hàng về người bán

  public Seller() {
    super();
  }

  public Seller(String username, String passwordHash, String email, String shopName) {
    super(username, passwordHash, email);
    this.shopName = shopName;
    this.rating = 0.0;
  }

  public Seller(
      Long id,
      LocalDateTime createdAt,
      String username,
      String passwordHash,
      String email,
      String shopName,
      double rating) {
    super(id, createdAt, username, passwordHash, email);
    this.shopName = shopName;
    this.rating = rating;
  }

  @Override
  public UserRole getRole() {
    return UserRole.SELLER;
  }

  public String getShopName() {
    return shopName;
  }

  public void setShopName(String shopName) {
    this.shopName = shopName;
  }

  public double getRating() {
    return rating;
  }

  public void setRating(double rating) {
    if (rating < 0.0 || rating > 5.0) {
      throw new IllegalArgumentException("Rating must be between 0.0 and 5.0: " + rating);
    }
    this.rating = rating;
  }

  @Override
  public String toString() {
    return "Seller{"
        + "id="
        + getId()
        + ", username='"
        + getUsername()
        + '\''
        + ", shopName='"
        + shopName
        + '\''
        + ", rating="
        + rating
        + ", createdAt="
        + getCreatedAt()
        + "}";
  }
}
