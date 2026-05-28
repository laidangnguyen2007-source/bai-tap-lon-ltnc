package server.handler;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.json.JSONObject;
import server.net.JsonResponses;

/**
 * <b>Điều phối request</b> theo trường {@code action} trong JSON.
 *
 * <p><b>OCP:</b> Bản đồ {@code action → handler} được truyền vào constructor {@code
 * RequestRouter(Map)}. Để thêm API mới: tạo handler mới + {@code routes.put("NEW_ACTION", handler)}
 * — <b>không sửa</b> vòng lặp phân tuyến bên trong.
 *
 * <p><b>SRP:</b> Chỉ parse JSON tối thiểu, lookup map, gọi handler, bọc lỗi — không chứa nghiệp vụ
 * đăng nhập/đấu giá.
 */
public final class RequestRouter {

  private final Map<String, ActionHandler> routes;

  /**
   * @param routes bản đồ bất biến (nên dùng {@link Map#copyOf} hoặc {@link
   *     Collections#unmodifiableMap} sau khi build) để tránh sửa nhầm lúc chạy
   */
  public RequestRouter(Map<String, ActionHandler> routes) {
    this.routes = routes;
  }

  /**
   * Xử lý một dòng raw JSON từ client.
   *
   * @return chuỗi trả lời một dòng, hoặc {@code null} nếu handler quyết định không phản hồi trực
   *     tiếp
   */
  public String dispatch(String rawJson) {
    try {
      JSONObject req = new JSONObject(rawJson);
      String action = req.getString("action");
      System.out.println("ACTION: " + action);

      ActionHandler handler = routes.get(action);
      if (handler == null) {
        return JsonResponses.error("Invalid action: " + action);
      }
      return handler.handle(req);
    } catch (Exception e) {
      System.err.println("Request processing error: " + e.getMessage());
      return JsonResponses.error(e.getMessage());
    }
  }

  /**
   * Factory gom toàn bộ route của ứng dụng đấu giá — <b>một chỗ duy nhất</b> nhìn thấy danh sách
   * API (tiện cho đồ án / review).
   */
  public static RequestRouter createAuctionRouter(
      AuthHandlers auth,
      CatalogHandlers catalog,
      BiddingHandlers bidding,
      AuctionCommandHandlers commands,
      WalletHandlers wallet) {
    Map<String, ActionHandler> map = new HashMap<>();
    map.put("LOGIN", auth::login);
    map.put("REGISTER", auth::register);
    map.put("GET_ITEM", catalog::getItem);
    map.put("GET_BID_HISTORY", catalog::getBidHistory);
    map.put("GET_USER_BIDS", catalog::getUserBids);
    map.put("PLACE_BID", bidding::placeBid);
    map.put("CREATE_AUCTION", commands::createAuction);
    map.put("GET_ALL_AUCTIONS", catalog::getAllAuctions);
    map.put("GET_AUCTIONS_BY_SELLER", catalog::getAuctionsBySeller);
    map.put("DELETE_AUCTION", commands::deleteAuction);
    map.put("ADMIN_UPDATE_AUCTION", commands::adminUpdateAuction);
    map.put("RESET_AUCTION", commands::resetAuction);
    map.put("REGISTER_AUTOBID", bidding::registerAutoBid);
    map.put("SELLER_UPDATE_AUCTION", commands::sellerUpdateAuction);
    map.put("SELLER_DELETE_AUCTION", commands::sellerDeleteAuction);
    map.put("CANCEL_AUTOBID", bidding::cancelAutoBid);

    // Wallet routes
    map.put("GET_WALLET", wallet::getWallet);
    map.put("GET_WALLET_TRANSACTIONS", wallet::getWalletTransactions);
    map.put("USER_TOP_UP", wallet::userTopUp);
    map.put("ADMIN_ADJUST_BALANCE", wallet::adminAdjustBalance);
    map.put("ADMIN_GET_ALL_WALLETS", wallet::adminGetAllWallets);

    return new RequestRouter(Collections.unmodifiableMap(map));
  }
}
