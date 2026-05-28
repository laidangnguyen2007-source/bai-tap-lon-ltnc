package com.auction.client.network;

import com.auction.client.util.JsonMapper;
import server.model.entity.item.Item;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

@SuppressWarnings("unchecked")
public class ItemNetworkHandler {
    private final SocketConnection connection;

    public ItemNetworkHandler(SocketConnection connection) {
        this.connection = connection;
    }

    public Item getItemById(Long itemId) {
        try {
            JSONObject req = new JSONObject();
            req.put("action", "GET_ITEM");
            req.put("itemId", itemId);
            String json = connection.sendRequest(req.toJSONString());
            JSONObject res = (JSONObject) new JSONParser().parse(json);
            return "OK".equals(res.get("status")) ? JsonMapper.mapToItem(res) : null;
        } catch (Exception e) { return null; }
    }
}
