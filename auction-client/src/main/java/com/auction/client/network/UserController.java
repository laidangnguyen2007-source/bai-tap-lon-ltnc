package com.auction.client.network;

import java.io.IOException;
import java.net.UnknownHostException;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import com.auction.client.util.JsonEntityMapper;
import com.auction.server.model.entity.user.User;
public class UserController {
  private final SocketConnection connection;
  private final JsonEntityMapper entityMapper = new JsonEntityMapper();

  public UserController() throws UnknownHostException, IOException {
    this.connection = SocketConnection.getInstance();
  }

  public User login(String username, String password) {
    try {
      JSONObject req = new JSONObject();
      req.put("action", "LOGIN");
      req.put("username", username);
      req.put("password", password);

      String response = connection.sendRequest(req.toJSONString());

      JSONObject res = (JSONObject) new JSONParser().parse(response);

      if (!"OK".equals(res.get("status"))) {
        return null;
      }

      return entityMapper.mapToUser(res);
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  public boolean register(String username, String password, String email, String role, String shopName) {
    try {
      JSONObject req = new JSONObject();
      req.put("action","REGISTER");
      req.put("username", username);
      req.put("password", password);
      req.put("email", email);
      req.put("role", role);
      req.put("shopName", shopName);

      String response = connection.sendRequest(req.toJSONString());

      JSONObject res = (JSONObject) new JSONParser().parse(response);
      
      if (!"OK".equals(res.get("status"))) {
        return false;
      }
      return true;
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    }
  }

  public User getUserById(Long id) {
    try {
      JSONObject req = new JSONObject();
      req.put("action", "GET_USER");
      req.put("id", id);

      String response = connection.sendRequest(req.toJSONString());
      JSONObject res = (JSONObject) new JSONParser().parse(response);

      if (!"OK".equals(res.get("status"))) {
        return null;
      }

      return entityMapper.mapToUser(res);
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }
}
