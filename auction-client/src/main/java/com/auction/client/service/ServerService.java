package com.auction.client.service;

import com.auction.client.network.AuctionController;
import com.auction.client.network.BidController;
import com.auction.client.network.ItemController;
import com.auction.client.network.UserController;
import com.auction.client.observer.AuctionObserver;
import com.auction.server.model.entity.Auction;
import com.auction.server.model.entity.BidTransaction;
import com.auction.server.model.entity.item.Item;
import com.auction.server.model.entity.user.User;
import java.util.ArrayList;
import java.util.List;

/**
 * Lớp Stub (giả lập) cho tầng kết nối với Server.
 *
 * <p><b>Mục đích:</b> Cho phép Thành viên 4 phát triển và kiểm thử GUI hoàn toàn độc lập, không
 * phụ thuộc vào tiến độ của Thành viên 3 (NetworkLayer thực). Khi Thành viên 3 hoàn thiện tầng
 * network, chỉ cần thay thế phần thân của từng phương thức bằng lời gọi socket thực sự — giao
 * diện (method signature) không đổi.
 *
 * <p><b>Quy ước tích hợp với TV3:</b> TV3 chỉ cần giữ nguyên tên lớp và package này, sau đó điền
 * logic socket/TCP vào bên trong từng phương thức.
 */
public class ServerService {
  private final UserController userController;
  private final AuctionController auctionController;
  private final BidController bidController;
  private final ItemController itemController;
  public ServerService() {
    try {
      this.userController = new UserController();
      this.auctionController = new AuctionController();
      this.bidController = new BidController(this);
      this.itemController = new ItemController();
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException("Unable to connect to the server: " + e.getMessage());
    }
  }
  // Danh sách các observer đang lắng nghe cập nhật realtime từ server.
  // Trong kiến trúc này, BiddingRoomController sẽ đăng ký vào danh sách này.
  private final List<AuctionObserver> observers = new ArrayList<>();

  // -- Quản lý Observer --

  /**
   * Đăng ký một observer để nhận thông báo cập nhật realtime.
   *
   * @param observer đối tượng cần nhận thông báo (thường là BiddingRoomController)
   */
  public void addObserver(AuctionObserver observer) {
    if (observer != null && !observers.contains(observer)) {
      observers.add(observer);
    }
  }

  /**
   * Hủy đăng ký observer (gọi khi người dùng rời phòng đấu giá).
   *
   * @param observer đối tượng cần hủy đăng ký
   */
  public void removeObserver(AuctionObserver observer) {
    observers.remove(observer);
  }

  /**
   * TV3 gọi phương thức này khi nhận được thông báo đặt giá mới từ Server socket. Phương thức này
   * sẽ notify toàn bộ observer đang đăng ký.
   *
   * @param bid giao dịch đặt giá vừa được xác nhận
   */
  public void notifyBidUpdated(BidTransaction bid) {
    for (AuctionObserver observer : observers) {
      observer.onBidUpdated(bid);
    }
  }

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
}
