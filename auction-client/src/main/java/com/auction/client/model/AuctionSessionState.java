package com.auction.client.model;

import server.model.entity.Auction;
import server.model.entity.user.User;

/**
 * Model lưu trữ trạng thái phiên làm việc hiện tại của Client.
 *
 * <p>Đây là lớp "Model" trong mô hình MVC phía Client. Sử dụng Singleton Pattern để đảm bảo chỉ
 * tồn tại một phiên làm việc duy nhất trong suốt vòng đời ứng dụng. Tất cả Controller đều truy cập
 * vào cùng một trạng thái thông qua getInstance().
 *
 * <p>Ví dụ: Khi người dùng đăng nhập thành công ở LoginController, thông tin User được lưu vào đây
 * để AuctionListController có thể đọc mà không cần truyền parameter.
 */
public class AuctionSessionState {

  // Instance duy nhất của Singleton - volatile để đảm bảo thread safety
  private static volatile AuctionSessionState instance;

  // Người dùng đang đăng nhập (null khi chưa đăng nhập)
  private User currentUser;

  // Phiên đấu giá đang được xem/tham gia (null khi không ở trong phòng đấu giá)
  private Auction selectedAuction;

  // Constructor private để ngăn tạo instance từ bên ngoài (Singleton pattern)
  private AuctionSessionState() {}

  /**
   * Lấy instance duy nhất của AuctionSessionState. Dùng Double-Checked Locking để thread-safe mà
   * không làm chậm mỗi lần truy cập.
   *
   * @return instance Singleton duy nhất
   */
  public static AuctionSessionState getInstance() {
    if (instance == null) {
      synchronized (AuctionSessionState.class) {
        if (instance == null) {
          instance = new AuctionSessionState();
        }
      }
    }
    return instance;
  }

  /**
   * Xóa toàn bộ trạng thái khi người dùng đăng xuất. Lấy lại instance sau khi gọi hàm này vẫn
   * hoạt động bình thường nhưng trạng thái đã được reset.
   */
  public void clearSession() {
    this.currentUser = null;
    this.selectedAuction = null;
  }

  public User getCurrentUser() {
    return currentUser;
  }

  public void setCurrentUser(User currentUser) {
    this.currentUser = currentUser;
  }

  public Auction getSelectedAuction() {
    return selectedAuction;
  }

  public void setSelectedAuction(Auction selectedAuction) {
    this.selectedAuction = selectedAuction;
  }

  /**
   * Kiểm tra nhanh xem người dùng đã đăng nhập chưa.
   *
   * @return true nếu đã đăng nhập
   */
  public boolean isLoggedIn() {
    return currentUser != null;
  }
}
