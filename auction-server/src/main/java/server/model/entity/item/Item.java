package server.model.entity.item;

import java.time.LocalDateTime;
import java.util.Objects;
import server.model.entity.BaseEntity;
import server.model.enums.ItemCategory;

public abstract class Item extends BaseEntity {

  private String name;
  private String description;
  private String imageBase64;
  private long startingPrice;
  private Long sellerId;
  private ItemCategory category;
  private String itemSpecifics;

  protected Item() {
    super();
  }

  protected Item(String name, String description, long startingPrice, Long sellerId) {
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
      long startingPrice,
      Long sellerId) {
    super(id, createdAt);
    this.name = name;
    this.description = description;
    this.startingPrice = startingPrice;
    this.sellerId = sellerId;
  }

  // giống như user phải getRole(), item cũng phải khai báo bản thân để chương
  // trình dễ quản lý
  public ItemCategory getCategory() {
    return category;
  }

  public void setCategory(ItemCategory category) {
    this.category = category;
  }

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

  public String getImageBase64() {
    return imageBase64;
  }

  public void setImageBase64(String imageBase64) {
    this.imageBase64 = imageBase64;
  }

  public String getItemSpecifics() {
    return itemSpecifics;
  }

  public void setItemSpecifics(String itemSpecifics) {
    this.itemSpecifics = itemSpecifics;
  }

  public long getStartingPrice() {
    return startingPrice;
  }

  public void setStartingPrice(long startingPrice) {
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
  private static long validatePositive(long value, String fieldName) {
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
