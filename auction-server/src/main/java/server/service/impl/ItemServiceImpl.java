// Service xử lý nghiệp vụ liên quan đến Item (sản phẩm đấu giá)
// Chức năng: Thêm / Sửa / Xóa sản phẩm (CRUD), kiểm tra quyền ownership của Seller
// Seller chỉ được sửa/xóa sản phẩm của chính mình
package server.service.impl;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import server.dao.AuctionDao;
import server.dao.ItemDao;
import server.model.entity.item.Item;
import server.model.entity.item.ItemFactory;
import server.model.enums.ItemCategory;
import server.model.exception.AuctionException;
import server.service.ItemService;

public class ItemServiceImpl implements ItemService {

  private final ItemDao itemDao;
  private final AuctionDao auctionDao; // Cần kiểm tra xem item có phiên đấu giá chưa

  public ItemServiceImpl(ItemDao itemDao, AuctionDao auctionDao) {
    this.itemDao = Objects.requireNonNull(itemDao, "itemDao must not be null");
    this.auctionDao = Objects.requireNonNull(auctionDao, "auctionDao must not be null");
  }

  // ===== THÊM SẢN PHẨM =====

  /*
   * Factory Method Pattern: Tạo sản phẩm thông qua ItemFactory thay vì gọi new
   * Artwork/Electronics/Vehicle trực tiếp. Toàn bộ logic "đúc" đúng kiểu Item được giao cho
   * ItemFactory, Service chỉ lo validate và lưu DB.
   *
   * @param category Loại sản phẩm muốn tạo (ARTWORK, ELECTRONICS, VEHICLE)
   *
   * @param params Map chứa các thuộc tính cần thiết tuỳ theo loại
   *
   * @return Item đã được lưu vào database kèm ID được gán tự động
   */
  public Item createItem(ItemCategory category, Map<String, Object> params) {
    Objects.requireNonNull(category, "category must not be null");
    Objects.requireNonNull(params, "params must not be null");
    // Giao việc khởi tạo đúng kiểu Item cho Factory — đây là điểm cốt lõi của Factory Pattern
    Item item = ItemFactory.create(category, params);
    validateItem(item);
    return itemDao.save(item);
  }

  // Giữ lại phương thức cũ để tương thích với code đã dùng Item trực tiếp
  public Item createItem(Item item) {
    Objects.requireNonNull(item, "Item must not be null");
    validateItem(item);
    return itemDao.save(item);
  }

  // ===== SỬA SẢN PHẨM =====

  // Cập nhật thông tin sản phẩm (chỉ seller sở hữu mới có quyền)
  public Item updateItem(Item item, Long requestingSellerId) {
    Objects.requireNonNull(item, "Item must not be null");
    Objects.requireNonNull(requestingSellerId, "Requesting seller ID must not be null");

    // Kiểm tra sản phẩm có tồn tại không
    Item existingItem =
        itemDao
            .findById(item.getId())
            .orElseThrow(() -> new AuctionException("Item not found with ID: " + item.getId()));

    // Kiểm tra quyền: chỉ seller sở hữu sản phẩm mới được sửa
    if (!existingItem.getSellerId().equals(requestingSellerId)) {
      throw new AuctionException("You do not have permission to update this item");
    }

    // Kiểm tra nếu đang có phiên đấu giá thì không cho sửa
    if (auctionDao.findByItemId(item.getId()).isPresent()) {
      throw new AuctionException("Cannot update item while it has an active auction");
    }

    validateItem(item);
    return itemDao.update(item);
  }

  // ===== XÓA SẢN PHẨM =====

  // Xóa sản phẩm (chỉ seller sở hữu mới có quyền, và không có phiên đấu giá đang chạy)
  public boolean deleteItem(Long itemId, Long requestingSellerId) {
    Objects.requireNonNull(itemId, "Item ID must not be null");
    Objects.requireNonNull(requestingSellerId, "Requesting seller ID must not be null");

    Item existingItem =
        itemDao
            .findById(itemId)
            .orElseThrow(() -> new AuctionException("Item not found with ID: " + itemId));

    // Kiểm tra quyền ownership
    if (!existingItem.getSellerId().equals(requestingSellerId)) {
      throw new AuctionException("You do not have permission to delete this item");
    }

    // Kiểm tra phiên đấu giá đang hoạt động
    if (auctionDao.findByItemId(itemId).isPresent()) {
      throw new AuctionException("Cannot delete item while it has an active auction");
    }

    return itemDao.deleteById(itemId);
  }

  // ===== TRUY VẤN =====

  // Tìm sản phẩm theo ID
  public Optional<Item> findById(Long id) {
    return itemDao.findById(id);
  }

  // Lấy tất cả sản phẩm
  public List<Item> findAll() {
    return itemDao.findAll();
  }

  // Lấy sản phẩm của 1 seller (dùng cho màn hình quản lý sản phẩm)
  public List<Item> findBySellerId(Long sellerId) {
    return itemDao.findBySellerId(sellerId);
  }

  // Lấy sản phẩm theo danh mục
  public List<Item> findByCategory(ItemCategory category) {
    return itemDao.findByCategory(category);
  }

  // Tìm kiếm sản phẩm theo tên
  public List<Item> searchByName(String keyword) {
    return itemDao.findByNameContaining(keyword);
  }

  // ===== VALIDATION =====

  // Kiểm tra thông tin sản phẩm hợp lệ
  private void validateItem(Item item) {
    if (item.getName() == null || item.getName().isBlank()) {
      throw new AuctionException("Item name must not be blank");
    }
    if (item.getStartingPrice() <= 0) {
      throw new AuctionException("Starting price must be positive");
    }
    if (item.getSellerId() == null) {
      throw new AuctionException("Seller ID must not be null");
    }
  }
}
