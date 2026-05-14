package com.auction.client.network;

import com.auction.client.util.JsonMapper;
import com.auction.server.model.entity.user.User;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

@SuppressWarnings("unchecked")
public class UserNetworkHandler {
    private final SocketConnection connection;

    public UserNetworkHandler(SocketConnection connection) {
        this.connection = connection;
    }

    public User login(String username, String password) {
        try {
            JSONObject req = new JSONObject();
            req.put("action", "LOGIN");
            req.put("username", username);
            req.put("password", password);
            String response = connection.sendRequest(req.toJSONString());
            JSONObject res = (JSONObject) new JSONParser().parse(response);
            return "OK".equals(res.get("status")) ? JsonMapper.mapToUser(res) : null;
        } catch (Exception e) { return null; }
    }

    public boolean register(String username, String password, String email, String role, String shopName) {
        try {
            JSONObject req = new JSONObject();
            req.put("action", "REGISTER");
            req.put("username", username);
            req.put("password", password);
            req.put("email", email);
            req.put("role", role);
            req.put("shopName", shopName);
            String response = connection.sendRequest(req.toJSONString());
            JSONObject res = (JSONObject) new JSONParser().parse(response);
            return "OK".equals(res.get("status"));
        } catch (Exception e) { return false; }
    }

    public User getUserById(Long id) {
        try {
            JSONObject req = new JSONObject();
            req.put("action", "GET_USER");
            req.put("id", id);
            String response = connection.sendRequest(req.toJSONString());
            JSONObject res = (JSONObject) new JSONParser().parse(response);
            return "OK".equals(res.get("status")) ? JsonMapper.mapToUser(res) : null;
        } catch (Exception e) { return null; }
    }
}
