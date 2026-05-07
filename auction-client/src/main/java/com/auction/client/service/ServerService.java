package com.auction.client.service;

import com.auction.client.network.*;
import com.auction.client.observer.AuctionObserver;
import com.auction.client.util.JsonMapper;
import com.auction.server.model.entity.Auction;
import com.auction.server.model.entity.BidTransaction;
import com.auction.server.model.entity.item.Item;
import com.auction.server.model.entity.user.User;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

/**
 * Facade Service — Cổng kết nối duy nhất của ứng dụng Client.
 * Tuân thủ SRP: Lớp này chỉ điều phối (delegate) yêu cầu tới các Handler chuyên biệt.
 */
public class ServerService {
    private final UserNetworkHandler userHandler;
    private final AuctionNetworkHandler auctionHandler;
    private final BidNetworkHandler bidHandler;
    private final ItemNetworkHandler itemHandler;
    
    private final List<AuctionObserver> observers = new ArrayList<>();
    private final Consumer<String> pushHandler;

    public ServerService() {
        try {
            SocketConnection connection = SocketConnection.getInstance();
            this.userHandler = new UserNetworkHandler(connection);
            this.auctionHandler = new AuctionNetworkHandler(connection);
            this.bidHandler = new BidNetworkHandler(connection);
            this.itemHandler = new ItemNetworkHandler(connection);

            this.pushHandler = this::handlePushMessage;
            connection.addPushListener(this.pushHandler);
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khởi tạo ServerService: " + e.getMessage());
        }
    }

    // -- REALTIME OBSERVER --

    public void addObserver(AuctionObserver o) { if (o != null) observers.add(o); }
    public void removeObserver(AuctionObserver o) { observers.remove(o); }

    private void handlePushMessage(String line) {
        try {
            JSONObject push = (JSONObject) new JSONParser().parse(line);
            String type = (String) push.get("type");
            if ("BID_UPDATE".equals(type)) {
                BidTransaction bid = JsonMapper.mapToBid((JSONObject) push.get("bid"));
                observers.forEach(o -> o.onBidUpdated(bid));
            } else if ("AUCTION_STATUS_CHANGED".equals(type)) {
                Long id = ((Number) push.get("auctionId")).longValue();
                String status = (String) push.get("newStatus");
                observers.forEach(o -> o.onAuctionStatusChanged(id, status));
            }
        } catch (Exception ignored) {}
    }

    // -- DELEGATION METHODS (Giữ nguyên giao diện cho UI gọi) --

    public User login(String u, String p) { return userHandler.login(u, p); }
    public boolean register(String u, String p, String e, String r, String s) { return userHandler.register(u, p, e, r, s); }
    public User getUserById(Long id) { return userHandler.getUserById(id); }

    public List<Auction> getAllAuctions() { return auctionHandler.getAllAuctions(); }
    public List<Auction> getAuctionsBySeller(Long id) { return auctionHandler.getAuctionsBySeller(id); }
    public Long createAuction(Auction a) { return auctionHandler.createAuction(a); }
    public boolean deleteAuction(Long id) { return auctionHandler.deleteAuction(id); }
    public boolean resetAuction(Long id) { return auctionHandler.resetAuction(id); }
    public boolean updateAuctionAdmin(Long id, long p, String s, String st, String et, String c) { 
        return auctionHandler.updateAuctionAdmin(id, p, s, st, et, c); 
    }

<<<<<<< HEAD
    public Item getItemById(Long id) { return itemHandler.getItemById(id); }
    
    public boolean placeBid(Long aId, Long bId, long amt) { return bidHandler.placeBid(aId, bId, amt); }
    public List<BidTransaction> getBidHistory(Long id) { return bidHandler.getBidHistory(id); }
=======
  /**
   * TV3 gọi phương thức này khi nhận được thông báo thay đổi trạng thái phiên từ Server.
   *
   * @param auctionId ID phiên bị thay đổi
   * @param newStatus trạng thái mới (chuỗi tên của AuctionStatus enum)
   */
  public void notifyAuctionStatusChanged(Long auctionId, String newStatus) {
    for (AuctionObserver observer : observers) {
      observer.onAuctionStatusChanged(auctionId, newStatus);
    }
  }

  // -- Tầng nghiệp vụ (stub — TV3 sẽ thay bằng gọi socket thực) --

  /**
   * Đăng nhập vào hệ thống.
   *
   * @param username tên đăng nhập
   * @param password mật khẩu chưa hash (server sẽ hash phía sau)
   * @return đối tượng User (Bidder hoặc Seller) nếu thành công, null nếu sai thông tin
   */
  public User login(String username, String password) {
    // STUB: TV3 thay bằng gửi request LOGIN qua socket và nhận response
    return userController.login(username, password);
  }

  /**
   * Đăng ký tài khoản mới.
   *
   * @param username tên đăng nhập
   * @param password mật khẩu
   * @param email email
   * @param role "BIDDER" hoặc "SELLER"
   * @param shopName tên shop (chỉ dùng nếu role = SELLER, ngược lại truyền null)
   * @return true nếu đăng ký thành công
   */
  public boolean register(
      String username, String password, String email, String role, String shopName) {
    // STUB: TV3 thay bằng gửi request REGISTER qua socket
    return userController.register(username, password, email, role, shopName);
  }

  /**
   * Lấy danh sách tất cả các phiên đấu giá từ server.
   *
   * @return danh sách Auction (rỗng nếu chưa kết nối)
   */
  public List<Auction> getAllAuctions() {
    // STUB: TV3 thay bằng gửi request GET_AUCTIONS qua socket
    return auctionController.getAllAuctions();
  }

  /**
   * Lấy chi tiết item của một phiên đấu giá.
   *
   * @param itemId ID của sản phẩm
   * @return AuctionItem hoặc null nếu không tìm thấy
   */
  public Item getItemById(Long itemId) {
    // STUB: TV3 thay bằng gửi request GET_ITEM qua socket
    return itemController.getItemById(itemId);
  }

  /**
   * Lấy lịch sử đặt giá của một phiên đấu giá (dùng để vẽ biểu đồ ban đầu khi load màn hình).
   *
   * @param auctionId ID phiên đấu giá
   * @return danh sách BidTransaction đã sắp xếp theo thời gian tăng dần
   */
  public List<BidTransaction> getBidHistory(Long auctionId) {
    // STUB: TV3 thay bằng gửi request GET_BID_HISTORY qua socket
    return bidController.getBidHistory(auctionId);
  }

  /**
   * Gửi một lượt đặt giá lên server.
   *
   * @param auctionId ID phiên đấu giá
   * @param bidderId ID người đặt giá
   * @param amount số tiền muốn đặt (phải > giá hiện tại)
   * @return true nếu server chấp nhận, false nếu bị từ chối
   */
  public boolean placeBid(Long auctionId, Long bidderId, long amount) {
    // STUB: TV3 thay bằng gửi request PLACE_BID qua socket và chờ response
    return bidController.placeBid(auctionId, bidderId, amount);
  }

  /**
   * Tạo phiên đấu giá mới (chỉ dành cho Seller).
   * Server sẽ tự tạo Item mới từ tên + loại sản phẩm, rồi tạo Auction gắn với Item đó.
   * Không cần tạo Auction object trên client — truyền trực tiếp các tham số.
   *
   * @param sellerId ID của Seller tạo phiên
   * @param startingPrice giá khởi điểm
   * @param startTime thời gian bắt đầu
   * @param endTime thời gian kết thúc
   * @param itemName tên sản phẩm do Seller đặt
   * @param category loại sản phẩm (ELECTRONICS, ARTWORK, VEHICLE, OTHER)
   * @return ID của phiên vừa tạo, -1 nếu thất bại
   */
  public Long createAuction(Long sellerId, long startingPrice,
      java.time.LocalDateTime startTime, java.time.LocalDateTime endTime,
      String itemName, String category) {
    // Gửi request CREATE_AUCTION kèm itemName + category qua socket
    return auctionController.createAuction(sellerId, startingPrice, startTime, endTime,
        itemName, category);
  }

  /**
   * Lấy danh sách các phiên đấu giá do một Seller tạo ra.
   *
   * @param sellerId ID của Seller
   * @return danh sách Auction của seller đó
   */
  public List<Auction> getAuctionsBySeller(Long sellerId) {
    // STUB: TV3 thay bằng gửi request GET_SELLER_AUCTIONS qua socket
    return auctionController.getAuctionsBySeller(sellerId);
  }

  /**
   * Admin xóa phiên đấu giá vi phạm.
   *
   * @param auctionId ID của phiên cần xóa
   * @return true nếu xóa thành công
   */
  public boolean deleteAuction(Long auctionId) {
    return auctionController.deleteAuction(auctionId);
  }

  public boolean updateAuctionAdmin(Long auctionId, long price, String status, String startTime, String endTime, String category) {
    return auctionController.updateAuctionAdmin(auctionId, price, status, startTime, endTime, category);
  }

  public User getUserById(Long id) {
    return userController.getUserById(id);
  }

  /**
   * Reset phiên đấu giá: xóa lịch sử bid và đưa giá về khởi điểm (dùng để dọn rác).
   *
   * @param auctionId ID của phiên cần reset
   * @return true nếu reset thành công
   */
  public boolean resetAuction(Long auctionId) {
    return auctionController.resetAuction(auctionId);
  }
>>>>>>> 6c4a32b91f848ac1a36f56982ffa546c0398e45a
}
