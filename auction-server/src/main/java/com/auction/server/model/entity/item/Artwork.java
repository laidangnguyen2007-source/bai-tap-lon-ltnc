package com.auction.server.model.entity.item;

import com.auction.server.model.enums.ItemCategory;
import java.time.LocalDateTime;

public class Artwork extends Item {

  private String artistName;
  private int yearCreated; // năm sáng tác
  private String medium; // chất liệu được sử dụng

  public Artwork() {
    super();
  }

  // Package-private: chỉ ItemFactory (cùng package) mới được dùng để đúc Artwork mới
  Artwork(
      String name,
      String description,
      long startingPrice,
      Long sellerId,
      String artistName,
      int yearCreated,
      String medium) {
    super(name, description, startingPrice, sellerId);
    this.artistName = artistName;
    this.yearCreated = yearCreated;
    this.medium = medium;
  }

  // Package-private: chỉ ItemFactory dùng để phục dựng Artwork từ DB
  Artwork(
      Long id,
      LocalDateTime createdAt,
      String name,
      String description,
      long startingPrice,
      Long sellerId,
      String artistName,
      int yearCreated,
      String medium) {
    super(id, createdAt, name, description, startingPrice, sellerId);
    this.artistName = artistName;
    this.yearCreated = yearCreated;
    this.medium = medium;
  }

  @Override
  public ItemCategory getCategory() {
    return ItemCategory.ARTWORK;
  }

  public String getArtistName() {
    return artistName;
  }

  public void setArtistName(String artistName) {
    this.artistName = artistName;
  }

  public int getYearCreated() {
    return yearCreated;
  }

  public void setYearCreated(int yearCreated) {
    if (yearCreated <= 0) {
      throw new IllegalArgumentException("yearCreated must be positive: " + yearCreated);
    }
    this.yearCreated = yearCreated;
  }

  public String getMedium() {
    return medium;
  }

  public void setMedium(String medium) {
    this.medium = medium;
  }

  @Override
  public String toString() {
    return "Artwork{"
        + "id="
        + getId()
        + ", name='"
        + getName()
        + '\''
        + ", artistName='"
        + artistName
        + '\''
        + ", yearCreated="
        + yearCreated
        + ", medium='"
        + medium
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
