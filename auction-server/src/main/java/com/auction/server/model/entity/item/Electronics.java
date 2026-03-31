package com.auction.server.model.entity.item;

import com.auction.server.model.enums.ItemCategory;
import java.time.LocalDateTime;

public class Electronics extends Item {

  private String brand;
  private int warrantyMonths; // thời gian bảo hành
  private double powerWatts; // công suất tiêu thụ điện

  public Electronics() {
    super();
  }

  public Electronics(
      String name,
      String description,
      double startingPrice,
      Long sellerId,
      String brand,
      int warrantyMonths,
      double powerWatts) {
    super(name, description, startingPrice, sellerId);
    this.brand = brand;
    this.warrantyMonths = warrantyMonths;
    this.powerWatts = powerWatts;
  }

  public Electronics(
      Long id,
      LocalDateTime createdAt,
      String name,
      String description,
      double startingPrice,
      Long sellerId,
      String brand,
      int warrantyMonths,
      double powerWatts) {
    super(id, createdAt, name, description, startingPrice, sellerId);
    this.brand = brand;
    this.warrantyMonths = warrantyMonths;
    this.powerWatts = powerWatts;
  }

  @Override
  public ItemCategory getCategory() {
    return ItemCategory.ELECTRONICS;
  }

  public String getBrand() {
    return brand;
  }

  public void setBrand(String brand) {
    this.brand = brand;
  }

  public int getWarrantyMonths() {
    return warrantyMonths;
  }

  public void setWarrantyMonths(int warrantyMonths) {
    if (warrantyMonths < 0) {
      throw new IllegalArgumentException("warrantyMonths must not be negative: " + warrantyMonths);
    }
    this.warrantyMonths = warrantyMonths;
  }

  public double getPowerWatts() {
    return powerWatts;
  }

  public void setPowerWatts(double powerWatts) {
    if (powerWatts < 0) {
      throw new IllegalArgumentException("powerWatts must not be negative: " + powerWatts);
    }
    this.powerWatts = powerWatts;
  }

  @Override
  public String toString() {
    return "Electronics{"
        + "id="
        + getId()
        + ", name='"
        + getName()
        + '\''
        + ", brand='"
        + brand
        + '\''
        + ", warrantyMonths="
        + warrantyMonths
        + ", powerWatts="
        + powerWatts
        + ", startingPrice="
        + getStartingPrice()
        + ", sellerId="
        + getSellerId()
        + ", createdAt="
        + getCreatedAt()
        + "}";
  }
}
