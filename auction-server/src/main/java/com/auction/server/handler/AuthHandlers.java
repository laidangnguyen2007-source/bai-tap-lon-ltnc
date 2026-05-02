package com.auction.server.handler;

import com.auction.server.dao.UserDao;
import com.auction.server.model.entity.user.Bidder;
import com.auction.server.model.entity.user.Seller;
import com.auction.server.model.entity.user.User;
import com.auction.server.net.JsonResponses;
import com.auction.server.service.util.PasswordUtil;
import java.util.Optional;
import org.json.JSONObject;

/**
 * Các handler liên quan <b>xác thực và tài khoản</b> (đăng nhập / đăng ký).
 *
 * <p><b>SRP + DIP (gần):</b> Class chỉ lo luồng user; nhận {@link UserDao} qua constructor thay vì
 * singleton static — dễ test giả lập DAO sau này (đồ án nâng cao có thể dùng mock).
 */
public final class AuthHandlers {

  private final UserDao userDao;

  public AuthHandlers(UserDao userDao) {
    this.userDao = userDao;
  }

  public String login(JSONObject req) throws Exception {
    String username = req.getString("username");
    String password = req.getString("password");
    Optional<User> userOpt = userDao.findByUsername(username);
    if (userOpt.isEmpty()) {
      return JsonResponses.error("Incorrect username or password.");
    }

    User user = userOpt.get();
    if (!PasswordUtil.verifyPassword(password, user.getPasswordHash())) {
      return JsonResponses.error("Incorrect username or password.");
    }

    JSONObject res = new JSONObject();
    res.put("status", "OK");
    res.put("id", user.getId());
    res.put("username", user.getUsername());
    res.put("email", user.getEmail());
    res.put("role", user.getRole().name());
    if (user instanceof Seller s) {
      res.put("shopName", s.getShopName());
    }
    if (user instanceof Bidder b) {
      res.put("balance", b.getBalance());
    }

    System.out.println("LOGIN OK: " + username + " (" + user.getRole() + ")");
    return res.toString();
  }

  public String register(JSONObject req) throws Exception {
    String username = req.getString("username");
    String password = req.getString("password");
    String email = req.getString("email");
    String role = req.getString("role");
    String shopName = req.getString("shopName");

    if (userDao.existsByUsername(username)) {
      return JsonResponses.error("Username already exists!");
    }
    if (userDao.existsByEmail(email)) {
      return JsonResponses.error("Email has already been used!");
    }

    String passwordHash = PasswordUtil.hashPassword(password);
    User newUser =
        "SELLER".equals(role)
            ? new Seller(username, passwordHash, email, shopName)
            : new Bidder(username, passwordHash, email, 0L);
    userDao.save(newUser);
    System.out.println("REGISTER OK:" + username + " (" + role + ")");

    JSONObject res = new JSONObject();
    res.put("status", "OK");
    return res.toString();
  }
}
