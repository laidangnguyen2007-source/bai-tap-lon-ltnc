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
    Long id = (Long) json.get("id");
    LocalDateTime createdAt = LocalDateTime.parse((String) json.get("createdAt"));
    String name = (String) json.get("name");
    String desc = (String) json.get("description");
    long price  = (Long) json.get("startingPrice");
    Long seller = (Long) json.get("sellerId");

    switch (category) {
      case ELECTRONICS:
        return ItemFactory.reconstructElectronics(
        id, createdAt, name, desc, price, seller,
        (String) json.get("brand"),
        ((Long) json.get("warrantyMonths")).intValue(),
        (Double) json.get("powerWatts")
      );
      case ARTWORK:
        return ItemFactory.reconstructArtwork(
        id, createdAt, name, desc, price, seller,
        (String) json.get("artistName"),
        ((Long) json.get("yearCreated")).intValue(),
        (String) json.get("medium")
      );
      case VEHICLE:
        return ItemFactory.reconstructVehicle(
        id, createdAt, name, desc, price, seller,
        (String) json.get("manufacturer"),
        ((Long) json.get("yearManufactured")).intValue(),
        ((Long) json.get("mileageKm")).intValue(),
        (String) json.get("fuelType")
      );
      default:
        throw new IllegalArgumentException("Unknown item: " + category);
    }
  }
}

