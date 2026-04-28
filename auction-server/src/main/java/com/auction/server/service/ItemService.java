package com.auction.server.service;

import com.auction.server.model.entity.item.Item;
import com.auction.server.model.enums.ItemCategory;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
