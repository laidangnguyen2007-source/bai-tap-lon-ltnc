package server.handler;

import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;
import server.dao.WalletDao;
import server.model.entity.Wallet;
import server.model.entity.WalletTransaction;
import server.net.ClientBroadcaster;
import server.net.JsonResponses;
import server.service.WalletService;

/** Handler quản lý ví tiền — xem số dư, lịch sử giao dịch, admin điều chỉnh. */
public final class WalletHandlers {

  private final WalletService walletService;
  private final WalletDao walletDao;
  private final ClientBroadcaster broadcaster;

  public WalletHandlers(
      WalletService walletService, WalletDao walletDao, ClientBroadcaster broadcaster) {
    this.walletService = walletService;
    this.walletDao = walletDao;
    this.broadcaster = broadcaster;
  }

  // ─── GET_WALLET ───
  public String getWallet(JSONObject req) throws Exception {
    try {
      Long userId = req.getLong("userId");

      Wallet wallet = walletService.getWallet(userId);
      if (wallet == null) {
        return JsonResponses.error("Không tìm thấy ví cho user #" + userId);
      }

      JSONObject walletJson = new JSONObject();
      walletJson.put("availableBalance", wallet.getAvailableBalance());
      walletJson.put("lockedBalance", wallet.getLockedBalance());
      walletJson.put("totalBalance", wallet.getTotalBalance());

      JSONObject res = new JSONObject();
      res.put("status", "OK");
      res.put("wallet", walletJson);
      return res.toString();

    } catch (Exception e) {
      e.printStackTrace();
      return JsonResponses.error(e.getMessage());
    }
  }

  // ─── GET_WALLET_TRANSACTIONS ───
  public String getWalletTransactions(JSONObject req) throws Exception {
    try {
      Long userId = req.getLong("userId");

      List<WalletTransaction> transactions = walletService.getTransactions(userId);

      JSONArray arr = new JSONArray();
      for (WalletTransaction tx : transactions) {
        JSONObject txJson = new JSONObject();
        txJson.put("id", tx.getId());
        txJson.put("userId", tx.getUserId());
        txJson.put("type", tx.getType().name());
        txJson.put("amount", tx.getAmount());
        txJson.put("balanceBefore", tx.getBalanceBefore());
        txJson.put("balanceAfter", tx.getBalanceAfter());
        txJson.put("referenceId", tx.getReferenceId());
        txJson.put(
            "referenceType", tx.getReferenceType() != null ? tx.getReferenceType().name() : null);
        txJson.put("description", tx.getDescription());
        txJson.put("createdBy", tx.getCreatedBy() != null ? tx.getCreatedBy().name() : null);
        txJson.put("createdAt", tx.getCreatedAt() != null ? tx.getCreatedAt().toString() : null);
        arr.put(txJson);
      }

      JSONObject res = new JSONObject();
      res.put("status", "OK");
      res.put("transactions", arr);
      return res.toString();

    } catch (Exception e) {
      e.printStackTrace();
      return JsonResponses.error(e.getMessage());
    }
  }

  // ─── ADMIN_ADJUST_BALANCE ───
  public String adminAdjustBalance(JSONObject req) throws Exception {
    try {
      String role = req.optString("role", "");
      if (!"ADMIN".equals(role)) {
        return JsonResponses.error("Chỉ Admin mới được phép điều chỉnh số dư!");
      }

      Long userId = req.getLong("userId");
      long amount = req.getLong("amount");
      Long adminId = req.getLong("adminId");
      String description = req.optString("description", "Admin adjustment");

      walletService.adminAdjustBalance(userId, amount, adminId, description);

      JSONObject notifyPush = new JSONObject();
      notifyPush.put("type", "ADMIN_BALANCE_ADJUSTED");
      broadcaster.sendToUser(userId, notifyPush.toString());

      // Lấy wallet mới sau khi điều chỉnh
      Wallet updated = walletService.getWallet(userId);

      JSONObject walletJson = new JSONObject();
      walletJson.put("availableBalance", updated.getAvailableBalance());
      walletJson.put("lockedBalance", updated.getLockedBalance());
      walletJson.put("totalBalance", updated.getTotalBalance());

      JSONObject res = new JSONObject();
      res.put("status", "OK");
      res.put("wallet", walletJson);

      System.out.println(
          "ADMIN ADJUST: user #" + userId + " | amount=" + amount + " | by admin #" + adminId);
      return res.toString();

    } catch (Exception e) {
      e.printStackTrace();
      return JsonResponses.error(e.getMessage());
    }
  }

  // ─── ADMIN_GET_ALL_WALLETS ───
  public String adminGetAllWallets(JSONObject req) throws Exception {
    try {
      String role = req.optString("role", "");
      if (!"ADMIN".equals(role)) {
        return JsonResponses.error("Chỉ Admin mới được phép xem tất cả ví!");
      }

      List<Wallet> wallets = walletDao.findAll();

      JSONArray arr = new JSONArray();
      for (Wallet w : wallets) {
        JSONObject wJson = new JSONObject();
        wJson.put("userId", w.getUserId());
        wJson.put("availableBalance", w.getAvailableBalance());
        wJson.put("lockedBalance", w.getLockedBalance());
        wJson.put("totalBalance", w.getTotalBalance());
        arr.put(wJson);
      }

      JSONObject res = new JSONObject();
      res.put("status", "OK");
      res.put("wallets", arr);
      return res.toString();

    } catch (Exception e) {
      e.printStackTrace();
      return JsonResponses.error(e.getMessage());
    }
  }

  // ─── USER_TOP_UP ───
  public String userTopUp(JSONObject req) throws Exception {
    try {
      Long userId = req.getLong("userId");
      long amount = req.getLong("amount");

      walletService.topUpWallet(userId, amount);

      JSONObject notifyPush = new JSONObject();
      notifyPush.put("type", "USER_TOP_UP");
      broadcaster.sendToUser(userId, notifyPush.toString());

      Wallet updated = walletService.getWallet(userId);

      JSONObject walletJson = new JSONObject();
      walletJson.put("availableBalance", updated.getAvailableBalance());
      walletJson.put("lockedBalance", updated.getLockedBalance());
      walletJson.put("totalBalance", updated.getTotalBalance());

      JSONObject res = new JSONObject();
      res.put("status", "OK");
      res.put("wallet", walletJson);

      return res.toString();
    } catch (Exception e) {
      e.printStackTrace();
      return JsonResponses.error(e.getMessage());
    }
  }
}
