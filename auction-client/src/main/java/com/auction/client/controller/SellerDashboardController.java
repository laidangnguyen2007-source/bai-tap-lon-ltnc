package com.auction.client.controller;

import com.auction.client.model.AuctionSessionState;
import com.auction.client.service.ServerService;
import com.auction.client.util.FxmlLoader;
import com.auction.server.model.entity.Auction;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;

import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

/**
 * Controller cho màn hình Dashboard của Seller (seller-dashboard.fxml).
 *
 * <p>Cho phép người bán (Seller): 1. Xem danh sách các phiên đấu giá mình đã tạo 2. Tạo phiên đấu
 * giá mới bằng cách điền form và gửi lên server
 *
 * <p>Màn hình này chỉ dành cho user có role SELLER. LoginController đã đảm bảo điều này bằng cách
 * chỉ điều hướng Seller đến đây.
 */
public class SellerDashboardController {

  // -- @FXML inject từ seller-dashboard.fxml --

  // Form tạo phiên đấu giá mới
  @FXML private TextField itemIdField;

  @FXML private TextField startingPriceField;

  @FXML private DatePicker startDatePicker;

  @FXML private DatePicker endDatePicker;

  @FXML private Button createAuctionButton;

  @FXML private Label formResultLabel;

  // Bảng danh sách phiên đấu giá đã tạo
  @FXML private TableView<Auction> myAuctionsTable;

  @FXML private TableColumn<Auction, Long> idCol;

  @FXML private TableColumn<Auction, Double> priceCol;

  @FXML private TableColumn<Auction, String> statusCol;

  // Header
  @FXML private Label sellerNameLabel;

  @FXML private Button logoutButton;

  @FXML private Button refreshButton;

  // -- Dữ liệu & Dependency --

  private final ServerService serverService = new ServerService();
  private final AuctionSessionState session = AuctionSessionState.getInstance();

  private final ObservableList<Auction> myAuctions = FXCollections.observableArrayList();

  /**
   * Khởi tạo màn hình: hiển thị tên seller, cấu hình bảng và load danh sách phiên của seller này.
   */
  @FXML
  public void initialize() {
    if (session.isLoggedIn()) {
      sellerNameLabel.setText("Xin chào, " + session.getCurrentUser().getUsername());
    }

    // Cấu hình cột bảng
    idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
    priceCol.setCellValueFactory(new PropertyValueFactory<>("currentPrice"));
    statusCol.setCellValueFactory(
        cell ->
            new javafx.beans.property.SimpleStringProperty(
                cell.getValue().getStatus().name()));

    myAuctionsTable.setItems(myAuctions);

    // Load danh sách phiên của seller hiện tại
    loadMyAuctions();
  }

  /** Gọi server lấy danh sách phiên của seller và cập nhật bảng. */
  private void loadMyAuctions() {
    Long sellerId = session.getCurrentUser().getId();
    List<Auction> auctions = serverService.getAuctionsBySeller(sellerId);
    myAuctions.setAll(auctions);
  }

  /**
   * Xử lý khi Seller nhấn nút "Tạo Phiên Mới". Validate form và gửi yêu cầu tạo auction lên
   * server.
   *
   * @param event ActionEvent từ createAuctionButton
   */
  @FXML
  private void handleCreateAuction(ActionEvent event) {
    String itemIdText = itemIdField.getText().trim();
    String priceText = startingPriceField.getText().trim();

    // Validate: các trường bắt buộc không được rỗng
    if (itemIdText.isEmpty() || priceText.isEmpty()
        || startDatePicker.getValue() == null
        || endDatePicker.getValue() == null) {
      formResultLabel.setText("Vui lòng điền đầy đủ thông tin trước khi tạo phiên.");
      return;
    }

    Long itemId;
    double startingPrice;
    try {
      itemId = Long.parseLong(itemIdText);
      startingPrice = Double.parseDouble(priceText);
    } catch (NumberFormatException e) {
      formResultLabel.setText("Mã sản phẩm và giá khởi điểm phải là số hợp lệ.");
      return;
    }

    // Validate: giá khởi điểm phải dương
    if (startingPrice <= 0) {
      formResultLabel.setText("Giá khởi điểm phải lớn hơn 0.");
      return;
    }

    // Chuyển LocalDate (từ DatePicker) sang LocalDateTime (entity dùng)
    LocalDateTime startTime = startDatePicker.getValue().atStartOfDay();
    LocalDateTime endTime = endDatePicker.getValue().atTime(23, 59, 59);

    // Validate: thời gian kết thúc phải sau thời gian bắt đầu
    if (!endTime.isAfter(startTime)) {
      formResultLabel.setText("Thời gian kết thúc phải sau thời gian bắt đầu.");
      return;
    }

    Long sellerId = session.getCurrentUser().getId();

    // Tạo đối tượng Auction mới từ dữ liệu form
    Auction newAuction = new Auction(itemId, sellerId, startTime, endTime);
    newAuction.setCurrentPrice(startingPrice);

    Long createdId = serverService.createAuction(newAuction);

    if (createdId != null && createdId > 0) {
      formResultLabel.setText("Tạo phiên đấu giá thành công! Mã phiên: #" + createdId);
      clearForm();
      loadMyAuctions(); // Refresh bảng
    } else {
      formResultLabel.setText("Tạo phiên thất bại. Vui lòng kiểm tra lại thông tin.");
    }
  }

  /**
   * Xử lý nút Refresh — tải lại danh sách phiên đấu giá của seller.
   *
   * @param event ActionEvent từ refreshButton
   */
  @FXML
  private void handleRefresh(ActionEvent event) {
    loadMyAuctions();
  }

  /**
   * Đăng xuất: xóa session và quay về màn hình Login.
   *
   * @param event ActionEvent từ logoutButton
   */
  @FXML
  private void handleLogout(ActionEvent event) {
    session.clearSession();
    try {
      Stage stage = (Stage) logoutButton.getScene().getWindow();
      FxmlLoader.navigateTo(stage, "login.fxml", "Online Auction System — Đăng Nhập");
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /** Xóa trắng form tạo phiên sau khi tạo thành công. */
  private void clearForm() {
    itemIdField.clear();
    startingPriceField.clear();
    startDatePicker.setValue(null);
    endDatePicker.setValue(null);
  }
}
