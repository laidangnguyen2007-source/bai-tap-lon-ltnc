// Interface DAO cho Item (sản phẩm đấu giá)
// Hỗ trợ tìm kiếm theo seller và danh mục sản phẩm
package server.dao;

import java.util.List;
import server.model.entity.item.Item;
import server.model.enums.ItemCategory;

public interface ItemDao extends GenericDao<Item> {

  // Lấy tất cả sản phẩm của 1 seller (dùng trong màn hình quản lý sản phẩm)
  List<Item> findBySellerId(Long sellerId);

  // Lấy tất cả sản phẩm theo danh mục (Electronics, Artwork, Vehicle)
  List<Item> findByCategory(ItemCategory category);

  // Tìm kiếm sản phẩm theo tên (hỗ trợ tìm kiếm gần đúng, không phân biệt hoa thường)
  List<Item> findByNameContaining(String keyword);
}
