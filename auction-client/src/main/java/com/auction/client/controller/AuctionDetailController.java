package com.auction.client.controller;

import com.auction.client.model.AuctionSessionState;
import com.auction.client.service.ServerService;
import com.auction.client.util.FxmlLoader;
import com.auction.server.model.entity.Auction;
import com.auction.server.model.entity.item.Item;
import com.auction.server.model.entity.user.User;
import com.auction.server.model.entity.user.Seller;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;

/**
 * Controller cho màn hình Chi Tiết Sản Phẩm / Phiên Đấu Giá (auction-detail.fxml).
 *
 * <p>Nhận dữ liệu từ AuctionSessionState (phiên đang được chọn) và hiển thị: - Thông tin chi tiết
 * sản phẩm (tên, mô tả, danh mục) - Thông tin phiên đấu giá (giá hiện tại, thời gian, trạng thái)
 * - Nút "Tham Gia Đấu Giá" → chuyển vào BiddingRoom
 */
public class AuctionDetailController {

  // -- @FXML inject từ auction-detail.fxml --

  // Thông tin sản phẩm
  @FXML private Label itemNameLabel;

  @FXML private Label itemCategoryLabel;

  @FXML private Label itemDescriptionLabel;

  // Thông tin phiên đấu giá
  @FXML private Label auctionIdLabel;

  @FXML private Label currentPriceLabel;

  @FXML private Label startTimeLabel;

  @FXML private Label endTimeLabel;

  @FXML private Label timeLeftLabel;

  @FXML private Label statusLabel;

  @FXML private Label sellerInfoLabel;

  // -- Bộ đếm thời gian thực --
  private javafx.animation.Timeline countdownTimeline;

  // Nút điều hướng
  @FXML private Button joinBiddingButton;

  @FXML private Button backButton;

  // -- Dependency --

  private final ServerService serverService = new ServerService();
  private final AuctionSessionState session = AuctionSessionState.getInstance();

  private static final DateTimeFormatter DISPLAY_FORMAT =
      DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

  /**
   * Được JavaFX gọi tự động sau khi load FXML. Đọc phiên đã được chọn từ session và điền thông tin
   * lên màn hình.
   */
  @FXML
  public void initialize() {
    Auction auction = session.getSelectedAuction();

    // Lập trình phòng thủ: nếu không có dữ liệu thì không làm gì để tránh NullPointerException
    if (auction == null) {
      itemNameLabel.setText("Không có thông tin sản phẩm");
      return;
    }

    // Hiển thị thông tin phiên đấu giá
    auctionIdLabel.setText("Mã phiên: #" + auction.getId());
    currentPriceLabel.setText(String.format("%,d VNĐ", auction.getCurrentPrice()));
    statusLabel.setText(auction.getStatus().name());

    if (auction.getStartTime() != null) {
      startTimeLabel.setText("Bắt đầu: " + auction.getStartTime().format(DISPLAY_FORMAT));
    }
    if (auction.getEndTime() != null) {
      endTimeLabel.setText("Kết thúc: " + auction.getEndTime().format(DISPLAY_FORMAT));
    }

    // Lấy thông tin sản phẩm từ Server (gọi riêng vì AuctionItem không được nhúng trong Auction)
    Item item = serverService.getItemById(auction.getItemId());
    if (item != null) {
      itemNameLabel.setText(item.getName());
      itemCategoryLabel.setText("Danh mục: " + item.getCategory().name());
      itemDescriptionLabel.setText(item.getDescription());
    } else {
      // Trường hợp stub chưa có dữ liệu: hiển thị placeholder
      itemNameLabel.setText("Sản phẩm #" + auction.getItemId());
      itemCategoryLabel.setText("Danh mục: Đang tải...");
      itemDescriptionLabel.setText("Mô tả sản phẩm sẽ hiển thị sau khi kết nối server.");
    }

    // Lấy thông tin người bán
    User seller = serverService.getUserById(auction.getSellerId());
    if (seller != null) {
        String displayName = seller.getUsername();
        if (seller instanceof Seller) {
            displayName = ((Seller) seller).getShopName() + " (" + seller.getUsername() + ")";
        }
        sellerInfoLabel.setText("Người bán: " + displayName);
    } else {
        sellerInfoLabel.setText("Người bán: ID #" + auction.getSellerId());
    }

    // Chỉ hiển thị nút đấu giá nếu phiên đang RUNNING
    joinBiddingButton.setDisable(!auction.isRunning());
    if (!auction.isRunning()) {
      joinBiddingButton.setText("Phiên không còn nhận giá");
    }

    // [Tính năng 1] Bắt đầu đồng hồ đếm ngược
    startCountdown(auction);
  }

  /**
   * Xử lý khi người dùng nhấn "Tham Gia Đấu Giá" — chuyển sang màn hình phòng đấu giá thời gian
   * thực.
   *
   * @param event ActionEvent từ joinBiddingButton
   */
  @FXML
  private void handleJoinBidding(ActionEvent event) {
    stopTimer();
    try {
      Stage stage = (Stage) joinBiddingButton.getScene().getWindow();
      // Auction đã được lưu trong session, BiddingRoomController sẽ tự đọc
      FxmlLoader.navigateTo(
          stage, "bidding-room.fxml", "Online Auction System — Phòng Đấu Giá Trực Tiếp");
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * Quay lại danh sách đấu giá.
   *
   * @param event ActionEvent từ backButton
   */
  @FXML
  private void handleBack(ActionEvent event) {
    stopTimer();
    try {
      Stage stage = (Stage) backButton.getScene().getWindow();
      FxmlLoader.navigateTo(
          stage, "auction-list.fxml", "Online Auction System — Danh Sách Đấu Giá");
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * [Tính năng 1] Khởi tạo và chạy bộ đếm ngược thời gian thực.
   */
  private void startCountdown(Auction auction) {
      if (auction.getStatus() == com.auction.server.model.enums.AuctionStatus.FINISHED) {
          timeLeftLabel.setText("Thời gian còn lại: Đã kết thúc");
          return;
      }

      countdownTimeline = new javafx.animation.Timeline(
          new javafx.animation.KeyFrame(javafx.util.Duration.seconds(1), event -> {
              String timeLeft = formatTimeLeft(auction);
              timeLeftLabel.setText("Thời gian còn lại: " + timeLeft);

              // Nếu hết giờ, vô hiệu hóa nút đấu giá ngay lập tức
              if (timeLeft.equals("Đã kết thúc")) {
                  joinBiddingButton.setDisable(true);
                  joinBiddingButton.setText("Phiên đã kết thúc");
              }
          })
      );
      countdownTimeline.setCycleCount(javafx.animation.Timeline.INDEFINITE);
      countdownTimeline.play();
  }

  /**
   * [Tính năng 1] Helper tính toán chuỗi hiển thị thời gian còn lại.
   */
  private String formatTimeLeft(Auction a) {
      java.time.Duration d = java.time.Duration.between(java.time.LocalDateTime.now(), a.getEndTime());
      if (d.isNegative() || d.isZero()) return "Đã kết thúc";

      long hours = d.toHours();
      long minutes = d.toMinutesPart();
      long seconds = d.toSecondsPart();

      return String.format("%02d:%02d:%02d", hours, minutes, seconds);
  }

  /**
   * [Tính năng 1] Dừng bộ đếm thời gian để giải phóng tài nguyên.
   */
  private void stopTimer() {
      if (countdownTimeline != null) {
          countdownTimeline.stop();
      }
  }
}
