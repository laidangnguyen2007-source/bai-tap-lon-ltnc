package com.auction.client.network;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import com.auction.client.util.JsonEntityMapper;
import com.auction.server.model.entity.item.Item;

public class ItemController {
  private final SocketConnection connection;
  private final JsonEntityMapper entityMapper = new JsonEntityMapper();

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

      return entityMapper.mapToItem(res);

    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }


}

