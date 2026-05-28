package server.handler;

import java.util.Optional;
import org.json.JSONObject;
import server.dao.UserDao;
import server.model.entity.Wallet;
import server.model.entity.user.Bidder;
import server.model.entity.user.Seller;
import server.model.entity.user.User;
import server.net.JsonResponses;
import server.service.WalletService;
import server.service.util.PasswordUtil;

/**
 * Các handler liên quan <b>xác thực và tài khoản</b> (đăng nhập / đăng ký).
 *
 * <p><b>SRP + DIP (gần):</b> Class chỉ lo luồng user; nhận {@link UserDao} qua constructor thay vì
 * singleton static — dễ test giả lập DAO sau này (đồ án nâng cao có thể dùng mock).
 */
public final class AuthHandlers {

  private final UserDao userDao;
  private final WalletService walletService;

  public AuthHandlers(UserDao userDao, WalletService walletService) {
    this.userDao = userDao;
    this.walletService = walletService;
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

    // Thêm thông tin ví vào response đăng nhập
    try {
      Wallet wallet = walletService.getWallet(user.getId());
      if (wallet != null) {
        res.put("availableBalance", wallet.getAvailableBalance());
        res.put("lockedBalance", wallet.getLockedBalance());
        res.put("totalBalance", wallet.getTotalBalance());
      }
    } catch (Exception e) {
      // Ví chưa tồn tại — trả về giá trị mặc định
      res.put("availableBalance", 0);
      res.put("lockedBalance", 0);
      res.put("totalBalance", 0);
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

    // Tự động tạo ví cho user mới
    try {
      walletService.createWallet(newUser.getId());
      System.out.println("WALLET CREATED for user: " + username + " (id=" + newUser.getId() + ")");
    } catch (Exception e) {
      System.err.println(
          "WARNING: Failed to create wallet for " + username + ": " + e.getMessage());
    }

    System.out.println("REGISTER OK:" + username + " (" + role + ")");

    JSONObject res = new JSONObject();
    res.put("status", "OK");
    return res.toString();
  }
}
