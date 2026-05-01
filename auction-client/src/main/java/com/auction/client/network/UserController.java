package com.auction.client.network;

import java.io.IOException;
import java.net.UnknownHostException;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import com.auction.server.model.entity.user.Admin;
import com.auction.server.model.entity.user.Bidder;
import com.auction.server.model.entity.user.Seller;
import com.auction.server.model.entity.user.User;
public class UserController {
  private final SocketConnection connection;

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

      return mapToUser(res);
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

  //JSON -> User 
  private User mapToUser(JSONObject json) {
    String role = (String) json.get("role");
    Long id = (Long) json.get("id");
    String username = (String) json.get("username");
    String email = (String) json.get("email");
    String hash = "";
    return switch (role) {
      case "BIDDER" -> {
        Bidder bidder = new Bidder(username, hash, email, 0L);
        bidder.setId(id);
        yield bidder; // yield = return
      }
      
      case "SELLER" -> {
        String shopName = (String) json.get("shopName");
        Seller seller = new Seller(username, hash, email, shopName);
        seller.setId(id);
        yield seller;
      }

      case "ADMIN" -> {
        Admin admin = new Admin(username, hash, email, 1);
        admin.setId(id);
        yield admin;
      }

      default -> throw new IllegalArgumentException("Unknown role: " + role);
    };
  }
}
