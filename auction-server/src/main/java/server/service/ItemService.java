package server.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import server.model.entity.item.Item;
import server.model.enums.ItemCategory;

public interface ItemService {
  Item createItem(ItemCategory category, Map<String, Object> params);

  Item createItem(Item item);

  Item updateItem(Item item, Long requestingSellerId);

  boolean deleteItem(Long itemId, Long requestingSellerId);

  Optional<Item> findById(Long id);

  List<Item> findAll();

  List<Item> findBySellerId(Long sellerId);

  List<Item> findByCategory(ItemCategory category);

  List<Item> searchByName(String keyword);
}
