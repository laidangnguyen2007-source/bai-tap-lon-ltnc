package com.auction.server.model.entity.item;

import com.auction.server.model.enums.ItemCategory;
import java.time.LocalDateTime;

public class Vehicle extends Item {

  private String manufacturer; // hãng sản xuất
  private int yearManufactured; // năm sản xuất
  private int mileageKm; // số kilometre đã đi
  private String fuelType; // loại nhiên liệu sử dụng

  public Vehicle() {
    super();
  }

  // Package-private: chỉ ItemFactory (cùng package) mới được dùng
  Vehicle(
      String name,
      String description,
      long startingPrice,
      Long sellerId,
      String manufacturer,
      int yearManufactured,
      int mileageKm,
      String fuelType) {
    super(name, description, startingPrice, sellerId);
    this.manufacturer = manufacturer;
    this.yearManufactured = yearManufactured;
    this.mileageKm = mileageKm;
    this.fuelType = fuelType;
  }

  // Package-private: chỉ ItemFactory dùng để phục dựng từ DB
  Vehicle(
      Long id,
      LocalDateTime createdAt,
      String name,
      String description,
      long startingPrice,
      Long sellerId,
      String manufacturer,
      int yearManufactured,
      int mileageKm,
      String fuelType) {
    super(id, createdAt, name, description, startingPrice, sellerId);
    this.manufacturer = manufacturer;
    this.yearManufactured = yearManufactured;
    this.mileageKm = mileageKm;
    this.fuelType = fuelType;
  }

  @Override
  public ItemCategory getCategory() {
    return ItemCategory.VEHICLE;
  }

  public String getManufacturer() {
    return manufacturer;
  }

  public void setManufacturer(String manufacturer) {
    this.manufacturer = manufacturer;
  }

  public int getYearManufactured() {
    return yearManufactured;
  }

  public void setYearManufactured(int yearManufactured) {
    if (yearManufactured <= 0) {
      throw new IllegalArgumentException("yearManufactured must be positive: " + yearManufactured);
    }
    this.yearManufactured = yearManufactured;
  }

  public int getMileageKm() {
    return mileageKm;
  }

  public void setMileageKm(int mileageKm) {
    if (mileageKm < 0) {
      throw new IllegalArgumentException("mileageKm must not be negative: " + mileageKm);
    }
    this.mileageKm = mileageKm;
  }

  public String getFuelType() {
    return fuelType;
  }

  public void setFuelType(String fuelType) {
    this.fuelType = fuelType;
  }

  @Override
  public String toString() {
    return "Vehicle{"
        + "id="
        + getId()
        + ", name='"
        + getName()
        + '\''
        + ", manufacturer='"
        + manufacturer
        + '\''
        + ", yearManufactured="
        + yearManufactured
        + ", mileageKm="
        + mileageKm
        + ", fuelType='"
        + fuelType
        + '\''
        + ", startingPrice="
        + getStartingPrice()
        + ", sellerId="
        + getSellerId()
        + ", createdAt="
        + getCreatedAt()
        + "}";
  }
}
