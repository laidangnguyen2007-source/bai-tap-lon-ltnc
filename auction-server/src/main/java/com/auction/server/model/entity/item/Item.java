package com.auction.server.model.entity.item;

import com.auction.server.model.entity.BaseEntity;
import com.auction.server.model.enums.ItemCategory;
import java.time.LocalDateTime;
import java.util.Objects;

public abstract class Item extends BaseEntity {

  private String name;
  private String description;
  private double startingPrice;
  private Long sellerId;

  protected Item() {
    super();
  }

  protected Item(String name, String description, double startingPrice, Long sellerId) {
    super();
    this.name = Objects.requireNonNull(name, "name must not be null");
    this.description = Objects.requireNonNull(description, "description must not be null");
    this.startingPrice = validatePositive(startingPrice, "startingPrice");
    this.sellerId = Objects.requireNonNull(sellerId, "sellerId must not be null");
  }

  protected Item(
      Long id,
      LocalDateTime createdAt,
      String name,
      String description,
      double startingPrice,
      Long sellerId) {
    super(id, createdAt);
    this.name = name;
    this.description = description;
    this.startingPrice = startingPrice;
    this.sellerId = sellerId;
  }

  // giống như user phải getRole(), item cũng phải khai báo bản thân để chương
  // trình dễ quản lý
  public abstract ItemCategory getCategory();

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = Objects.requireNonNull(name, "name must not be null");
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public double getStartingPrice() {
    return startingPrice;
  }

  public void setStartingPrice(double startingPrice) {
    this.startingPrice = validatePositive(startingPrice, "startingPrice");
  }

  public Long getSellerId() {
    return sellerId;
  }

  public void setSellerId(Long sellerId) {
    this.sellerId = Objects.requireNonNull(sellerId, "sellerId must not be null");
  }

  // giá khởi điểm luôn phải lớn hơn 0
  // fieldName ở đây là startingPrice
  private static double validatePositive(double value, String fieldName) {
    if (value <= 0) {
      throw new IllegalArgumentException(fieldName + " must be positive: " + value);
    }
    return value;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName()
        + "{id="
        + getId()
        + ", name='"
        + name
        + '\''
        + ", category="
        + getCategory()
        + ", startingPrice="
        + startingPrice
        + ", sellerId="
        + sellerId
        + ", createdAt="
        + getCreatedAt()
        + "}";
  }
}
