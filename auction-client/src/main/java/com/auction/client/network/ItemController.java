package com.auction.client.network;

import java.time.LocalDateTime;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import com.auction.server.model.entity.item.Item;
import com.auction.server.model.entity.item.ItemFactory;
import com.auction.server.model.enums.ItemCategory;

public class ItemController {
  private final SocketConnection connection;

  public ItemController() throws Exception {
    this.connection = SocketConnection.getInstance();
  }

  public Item getItemById(Long itemId) {
    try {
      JSONObject req = new JSONObject();
      req.put("action", "GET_ITEM");
      req.put("itemId", itemId);

      String json = connection.sendRequest(req.toJSONString());
      JSONObject res = (JSONObject) new JSONParser().parse(json);

      if (!"OK".equals(res.get("status"))) return null;

      return mapToItem(res);

    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  private Item mapToItem(JSONObject json) {
    ItemCategory category = ItemCategory.valueOf((String) json.get("category"));
    Long id = ((Number) json.get("id")).longValue();
    LocalDateTime createdAt = LocalDateTime.parse((String) json.get("createdAt"));
    String name = (String) json.get("name");
    String desc = (String) json.get("description");
    // Handle potential nulls for description
    if (desc == null) desc = "Đang cập nhật...";
    long price  = ((Number) json.get("startingPrice")).longValue();
    Long seller = ((Number) json.get("sellerId")).longValue();

    switch (category) {
      case ELECTRONICS:
        return ItemFactory.reconstructElectronics(
        id, createdAt, name, desc, price, seller,
        (String) json.get("brand"),
        json.get("warrantyMonths") != null ? ((Number) json.get("warrantyMonths")).intValue() : 0,
        json.get("powerWatts") != null ? ((Number) json.get("powerWatts")).doubleValue() : 0.0
      );
      case ARTWORK:
        return ItemFactory.reconstructArtwork(
        id, createdAt, name, desc, price, seller,
        (String) json.get("artistName"),
        json.get("yearCreated") != null ? ((Number) json.get("yearCreated")).intValue() : 0,
        (String) json.get("medium")
      );
      case VEHICLE:
        return ItemFactory.reconstructVehicle(
        id, createdAt, name, desc, price, seller,
        (String) json.get("manufacturer"),
        json.get("yearManufactured") != null ? ((Number) json.get("yearManufactured")).intValue() : 0,
        json.get("mileageKm") != null ? ((Number) json.get("mileageKm")).intValue() : 0,
        (String) json.get("fuelType")
      );
      default:
        throw new IllegalArgumentException("Unknown item: " + category);
    }
  }
}

