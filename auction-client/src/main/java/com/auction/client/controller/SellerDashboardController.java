package com.auction.client.controller;

import com.auction.client.model.AuctionSessionState;
import com.auction.client.service.ServerService;
import com.auction.client.util.ComboBoxPopupWidthSync;
import com.auction.client.util.FxmlLoader;
import com.auction.client.util.NotificationUtils;
import com.auction.server.model.entity.Auction;

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
 * giá mới bằng cách chọn loại sản phẩm, đặt tên, và thiết lập thời gian
 *
 * <p>Màn hình này chỉ dành cho user có role SELLER. LoginController đã đảm bảo điều này bằng cách
 * chỉ điều hướng Seller đến đây.
 */
public class SellerDashboardController {

  // -- @FXML inject từ seller-dashboard.fxml --

  // Form tạo phiên đấu giá mới

  // ComboBox chọn loại sản phẩm (thay thế TextField itemIdField cũ)
  // Seller chọn 1 trong 4 loại: Điện tử, Nghệ thuật, Xe cộ, Khác
  @FXML private ComboBox<String> categoryCombo;

  // TextField nhập tên sản phẩm (mới thêm)
  // Seller tự đặt tên cho sản phẩm thay vì phải nhớ mã ID
  @FXML private TextField itemNameField;

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
  // Cột loại sản phẩm (mới) — hiển thị ELECTRONICS/ARTWORK/VEHICLE/OTHER bằng tiếng Việt
  @FXML private TableColumn<Auction, String> categoryCol;
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

  // Bảng ánh xạ: Tên tiếng Việt hiển thị trên ComboBox → giá trị enum gửi lên server
  // Giúp Seller dễ hiểu hơn so với tên enum tiếng Anh (ELECTRONICS, ARTWORK, VEHICLE, OTHER)
  private static final String[] CATEGORY_DISPLAY_NAMES = {
      "Điện tử",      // → ELECTRONICS
      "Nghệ thuật",   // → ARTWORK
      "Xe cộ",         // → VEHICLE
      "Khác"           // → OTHER
  };
  private static final String[] CATEGORY_ENUM_VALUES = {
      "ELECTRONICS", "ARTWORK", "VEHICLE", "OTHER"
  };

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
    
    // Cột loại sản phẩm — chuyển enum tiếng Anh sang tiếng Việt để dễ đọc
    categoryCol.setCellValueFactory(
        cell -> {
          String cat = cell.getValue().getItemCategory();
          if (cat == null) cat = "OTHER";
          String vietnameseCategory = switch (cat) {
            case "ELECTRONICS" -> "Điện tử";
            case "ARTWORK" -> "Nghệ thuật";
            case "VEHICLE" -> "Xe cộ";
            case "OTHER" -> "Khác";
            default -> cat;
          };
          return new javafx.beans.property.SimpleStringProperty(vietnameseCategory);
        });
    
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

    // Khởi tạo ComboBox loại sản phẩm (mới) — hiển thị tên tiếng Việt cho dễ hiểu
    categoryCombo.setItems(FXCollections.observableArrayList(CATEGORY_DISPLAY_NAMES));
    categoryCombo.setValue(CATEGORY_DISPLAY_NAMES[0]); // Mặc định: Điện tử
    ComboBoxPopupWidthSync.install(categoryCombo);

    // Khởi tạo ComboBox giờ và phút cho Form
    ObservableList<Integer> hours = FXCollections.observableArrayList();
    for (int i = 0; i < 24; i++) hours.add(i);
    ObservableList<Integer> minutes = FXCollections.observableArrayList();
    for (int i = 0; i < 60; i++) minutes.add(i);

    startHourCombo.setItems(hours);
    startMinuteCombo.setItems(minutes);
    endHourCombo.setItems(hours);
    endMinuteCombo.setItems(minutes);

    // Mặc định chọn giờ hiện tại
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
   * Chuyển đổi tên hiển thị tiếng Việt trong ComboBox sang giá trị enum để gửi lên server.
   * Ví dụ: "Điện tử" → "ELECTRONICS", "Nghệ thuật" → "ARTWORK"
   *
   * @param displayName tên hiển thị tiếng Việt từ categoryCombo
   * @return tên enum tiếng Anh tương ứng, hoặc "OTHER" nếu không tìm thấy
   */
  private String categoryDisplayToEnum(String displayName) {
    for (int i = 0; i < CATEGORY_DISPLAY_NAMES.length; i++) {
      if (CATEGORY_DISPLAY_NAMES[i].equals(displayName)) {
        return CATEGORY_ENUM_VALUES[i];
      }
    }
    return "OTHER"; // Fallback an toàn
  }

  /**
   * Xử lý khi Seller nhấn nút "Tạo Phiên Mới".
   * Validate form, tạo Item + Auction trên server.
   *
   * <p>Luồng xử lý:
   * 1. Validate: kiểm tra các trường bắt buộc
   * 2. Chuyển đổi category từ tiếng Việt sang enum
   * 3. Gửi request lên server (server sẽ tự tạo Item + Auction)
   * 4. Xác định trạng thái dựa trên thời gian bắt đầu
   *
   * @param event ActionEvent từ createAuctionButton
   */
  @FXML
  private void handleCreateAuction(ActionEvent event) {
    String itemName = itemNameField.getText().trim();
    String priceText = startingPriceField.getText().trim();

    // Validate: các trường bắt buộc không được rỗng
    if (categoryCombo.getValue() == null
        || itemName.isEmpty()
        || priceText.isEmpty()
        || startDatePicker.getValue() == null
        || endDatePicker.getValue() == null
        || startHourCombo.getValue() == null
        || startMinuteCombo.getValue() == null
        || endHourCombo.getValue() == null
        || endMinuteCombo.getValue() == null) {
      formResultLabel.setText("Vui lòng điền đầy đủ tất cả thông tin!");
      return;
    }

    long startingPrice;
    try {
      startingPrice = Long.parseLong(priceText);
    } catch (NumberFormatException e) {
      formResultLabel.setText("Giá khởi điểm phải là số hợp lệ.");
      return;
    }

    // Validate: giá khởi điểm phải dương
    if (startingPrice <= 0) {
      formResultLabel.setText("Giá khởi điểm phải lớn hơn 0.");
      return;
    }

    // Chuyển đổi tên hiển thị tiếng Việt → enum tiếng Anh để gửi lên server
    String categoryEnum = categoryDisplayToEnum(categoryCombo.getValue());

    // [Xử lý thời gian] Chuyển đổi dữ liệu từ DatePicker và ComboBox Giờ/Phút sang LocalDateTime
    LocalDateTime now = LocalDateTime.now();
    LocalDateTime startTime = startDatePicker.getValue().atTime(
        startHourCombo.getValue(), startMinuteCombo.getValue());
    
    // [Validation] Chống tạo phiên đấu giá trong quá khứ. 
    // Chúng ta cho phép sai số 10 giây để bù đắp cho độ trễ khi thao tác form.
    // if (startTime.isBefore(now.minusSeconds(10))) {
    //     formResultLabel.setText("Lỗi: Thời gian bắt đầu không được ở trong quá khứ!");
    //     return;
    // }
    
    // Tương tự cho thời gian kết thúc
    LocalDateTime endTime = endDatePicker.getValue().atTime(
        endHourCombo.getValue(), endMinuteCombo.getValue());

    // [Validation] Thời gian kết thúc phải sau thời gian bắt đầu
    if (!endTime.isAfter(startTime)) {
      formResultLabel.setText("Lỗi: Thời gian kết thúc phải sau thời gian bắt đầu!");
      return;
    }

    Long sellerId = session.getCurrentUser().getId();

    // [Thêm bước xác nhận] Hỏi Seller trước khi tạo phiên đấu giá chính thức
    javafx.scene.control.Alert confirmAlert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.CONFIRMATION);
    NotificationUtils.styleAlert(confirmAlert);
    confirmAlert.setGraphic(null); // Bỏ dấu "?"
    confirmAlert.setTitle("Xác Nhận Tạo Phiên");
    confirmAlert.setHeaderText("Bạn có chắc chắn muốn tạo phiên đấu giá mới này không?");
    confirmAlert.setContentText(
        "Sản phẩm: " + itemName + "\n"
        + "Loại: " + categoryCombo.getValue() + "\n"
        + "Giá khởi điểm: " + String.format("%,d", startingPrice) + " VNĐ");

    java.util.Optional<javafx.scene.control.ButtonType> confirmResult = confirmAlert.showAndWait();
    if (confirmResult.isEmpty() || confirmResult.get() != javafx.scene.control.ButtonType.OK) {
        return; // Hủy tạo phiên
    }

    // Gửi request lên server
    // Tạo đối tượng Auction tạm thời để đóng gói dữ liệu
    Auction newAuction = new Auction();
    newAuction.setSellerId(sellerId);
    newAuction.setCurrentPrice(startingPrice);
    newAuction.setStartTime(startTime);
    newAuction.setEndTime(endTime);
    // Lưu tạm itemName và category vào object để Handler lấy ra gửi JSON
    newAuction.setItemName(itemName);
    newAuction.setItemCategory(categoryEnum); 

    Long createdId = serverService.createAuction(newAuction);

    if (createdId != null && createdId > 0) {
      // [Thông báo trạng thái dựa trên thời gian]
      // Server đã tự xử lý trạng thái (OPEN/RUNNING) — client chỉ cần hiển thị
      if (!startTime.isAfter(now)) {
        formResultLabel.setText("Tạo phiên đấu giá thành công! Mã phiên: #" + createdId
            + " — Trạng thái: ĐANG DIỄN RA (thời gian bắt đầu đã qua)");
      } else {
        formResultLabel.setText("Tạo phiên đấu giá thành công! Mã phiên: #" + createdId
            + " — Trạng thái: ĐANG MỞ (sẽ tự chạy khi đến giờ bắt đầu)");
      }

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
    itemNameField.clear();
    startingPriceField.clear();
    startDatePicker.setValue(null);
    endDatePicker.setValue(null);
    categoryCombo.setValue(CATEGORY_DISPLAY_NAMES[0]); // Reset về "Điện tử"
    // Reset giờ về hiện tại
    startHourCombo.setValue(LocalDateTime.now().getHour());
    startMinuteCombo.setValue(LocalDateTime.now().getMinute());
  }
}
