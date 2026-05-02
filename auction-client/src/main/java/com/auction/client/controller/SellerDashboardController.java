package com.auction.client.controller;

import com.auction.client.model.AuctionSessionState;
import com.auction.client.service.ServerService;
import com.auction.client.util.ComboBoxPopupWidthSync;
import com.auction.client.util.FxmlLoader;
import com.auction.client.util.NotificationUtils;
import com.auction.server.model.entity.Auction;
import com.auction.server.service.AuctionManager;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;

import javafx.scene.control.ComboBox;
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
  
  @FXML private ComboBox<Integer> startHourCombo;
  @FXML private ComboBox<Integer> startMinuteCombo;
  @FXML private ComboBox<Integer> endHourCombo;
  @FXML private ComboBox<Integer> endMinuteCombo;
  
  @FXML private Button createAuctionButton;

  @FXML private Label formResultLabel;

  // Bảng danh sách phiên đấu giá đã tạo
  @FXML private TableView<Auction> myAuctionsTable;

  @FXML private TableColumn<Auction, Long> idCol;
  @FXML private TableColumn<Auction, String> itemNameCol;
  @FXML private TableColumn<Auction, Long> priceCol;
  @FXML private TableColumn<Auction, String> statusCol;
  @FXML private TableColumn<Auction, String> startTimeCol;
  @FXML private TableColumn<Auction, String> endTimeCol;

  // Header
  @FXML private Label sellerNameLabel;
  @FXML private Button logoutButton;
  @FXML private Button refreshButton;

  // -- Dữ liệu & Dependency --
  private final java.time.format.DateTimeFormatter formatter = 
      java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

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
    itemNameCol.setCellValueFactory(new PropertyValueFactory<>("itemName"));
    
    priceCol.setCellFactory(column -> new javafx.scene.control.TableCell<>() {
      @Override
      protected void updateItem(Long price, boolean empty) {
        super.updateItem(price, empty);
        if (empty || price == null) {
          setText(null);
        } else {
          setText(String.format("%,d VNĐ", price));
        }
      }
    });
    priceCol.setCellValueFactory(new PropertyValueFactory<>("currentPrice"));

    statusCol.setCellValueFactory(
        cell -> {
          String s = cell.getValue().getStatus().name();
          String vietnameseStatus = switch (s) {
            case "OPEN" -> "Đang mở";
            case "RUNNING" -> "Đang diễn ra";
            case "FINISHED" -> "Đã kết thúc";
            case "PAID" -> "Đã thanh toán";
            case "CANCELED" -> "Đã hủy";
            default -> s;
          };
          return new javafx.beans.property.SimpleStringProperty(vietnameseStatus);
        });

    startTimeCol.setCellValueFactory(
        cell -> new javafx.beans.property.SimpleStringProperty(
            cell.getValue().getStartTime().format(formatter)));
            
    endTimeCol.setCellValueFactory(
        cell -> new javafx.beans.property.SimpleStringProperty(
            cell.getValue().getEndTime().format(formatter)));

    myAuctionsTable.setItems(myAuctions);

    // Khởi tạo ComboBox giờ và phút cho Form
    ObservableList<Integer> hours = FXCollections.observableArrayList();
    for (int i = 0; i < 24; i++) hours.add(i);
    ObservableList<Integer> minutes = FXCollections.observableArrayList();
    for (int i = 0; i < 60; i++) minutes.add(i);

    startHourCombo.setItems(hours);
    startMinuteCombo.setItems(minutes);
    endHourCombo.setItems(hours);
    endMinuteCombo.setItems(minutes);

    // Mặc định chọn giờ đẹp
    startHourCombo.setValue(LocalDateTime.now().getHour());
    startMinuteCombo.setValue(LocalDateTime.now().getMinute());
    endHourCombo.setValue(23);
    endMinuteCombo.setValue(59);

    ComboBoxPopupWidthSync.install(startHourCombo);
    ComboBoxPopupWidthSync.install(startMinuteCombo);
    ComboBoxPopupWidthSync.install(endHourCombo);
    ComboBoxPopupWidthSync.install(endMinuteCombo);

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
        || endDatePicker.getValue() == null
        || startHourCombo.getValue() == null
        || startMinuteCombo.getValue() == null
        || endHourCombo.getValue() == null
        || endMinuteCombo.getValue() == null) {
      formResultLabel.setText("Vui lòng điền đầy đủ ngày và giờ.");
      return;
    }

    Long itemId;
    long startingPrice;
    try {
      itemId = Long.parseLong(itemIdText);
      startingPrice = Long.parseLong(priceText);
    } catch (NumberFormatException e) {
      formResultLabel.setText("Mã sản phẩm và giá khởi điểm phải là số hợp lệ.");
      return;
    }

    // Validate: giá khởi điểm phải dương
    if (startingPrice <= 0) {
      formResultLabel.setText("Giá khởi điểm phải lớn hơn 0.");
      return;
    }

    // [Xử lý thời gian] Chuyển đổi dữ liệu từ DatePicker và ComboBox Giờ/Phút sang đối tượng LocalDateTime
    LocalDateTime now = LocalDateTime.now();
    LocalDateTime startTime = startDatePicker.getValue().atTime(
        startHourCombo.getValue(), startMinuteCombo.getValue());
    
    // [Validation] Chống tạo phiên đấu giá trong quá khứ. 
    // Chúng ta cho phép sai số 10 giây để bù đắp cho độ trễ khi thao tác form.
    if (startTime.isBefore(now.minusSeconds(10))) {
        formResultLabel.setText("Lỗi: Thời gian bắt đầu không được ở trong quá khứ!");
        return;
    }
    
    // Tương tự cho thời gian kết thúc
    LocalDateTime endTime = endDatePicker.getValue().atTime(
        endHourCombo.getValue(), endMinuteCombo.getValue());

    // Validate: thời gian kết thúc phải sau thời gian bắt đầu
    if (!endTime.isAfter(startTime)) {
      formResultLabel.setText("Thời gian kết thúc phải sau thời gian bắt đầu.");
      return;
    }

    Long sellerId = session.getCurrentUser().getId();

    // [Thêm bước xác nhận] Hỏi bạn trước khi tạo phiên đấu giá chính thức
    javafx.scene.control.Alert confirmAlert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.CONFIRMATION);
    NotificationUtils.styleAlert(confirmAlert);
    confirmAlert.setGraphic(null); // Bỏ dấu "?"
    confirmAlert.setTitle("Xác Nhận Tạo Phiên");
    confirmAlert.setHeaderText("Bạn có chắc chắn muốn tạo phiên đấu giá mới này không?");
    confirmAlert.setContentText("Vui lòng kiểm tra kỹ Item ID và thời gian trước khi nhấn OK.");

    java.util.Optional<javafx.scene.control.ButtonType> confirmResult = confirmAlert.showAndWait();
    if (confirmResult.isEmpty() || confirmResult.get() != javafx.scene.control.ButtonType.OK) {
        return; // Hủy tạo phiên
    }

    // Tạo đối tượng Auction mới từ dữ liệu form
    Auction newAuction = new Auction(itemId, sellerId, startTime, endTime);
    newAuction.setCurrentPrice(startingPrice);

    Long createdId = serverService.createAuction(newAuction);

    if (createdId != null && createdId > 0) {
      newAuction.setId(createdId);
      // Singleton Pattern: Khai mạc phiên đấu giá qua AuctionManager duy nhất của hệ thống
      // AuctionManager giúp quản lý tập trung tất cả phiên đang chạy, phục vụ Concurrency
      AuctionManager.getInstance().openAuction(newAuction);
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
    // [Thêm bước xác nhận] Hỏi bạn trước khi đăng xuất
    javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.CONFIRMATION);
    NotificationUtils.styleAlert(alert);
    alert.setGraphic(null); // Bỏ dấu "?"
    alert.setTitle("Xác Nhận Đăng Xuất");
    alert.setHeaderText("Bạn có thực sự muốn đăng xuất không?");
    
    java.util.Optional<javafx.scene.control.ButtonType> result = alert.showAndWait();
    if (result.isPresent() && result.get() == javafx.scene.control.ButtonType.OK) {
        session.clearSession();
        try {
          Stage stage = (Stage) logoutButton.getScene().getWindow();
          FxmlLoader.navigateTo(stage, "login.fxml", "Online Auction System — Đăng Nhập");
        } catch (IOException e) {
          e.printStackTrace();
        }
    }
  }

  /** Xóa trắng form tạo phiên sau khi tạo thành công. */
  private void clearForm() {
    itemIdField.clear();
    startingPriceField.clear();
    startDatePicker.setValue(null);
    endDatePicker.setValue(null);
    // Reset giờ về hiện tại
    startHourCombo.setValue(LocalDateTime.now().getHour());
    startMinuteCombo.setValue(LocalDateTime.now().getMinute());
  }
}
