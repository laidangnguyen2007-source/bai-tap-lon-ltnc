package com.auction.client.controller;

import com.auction.client.model.AuctionSessionState;
import com.auction.client.service.ServerService;
import com.auction.client.util.ComboBoxPopupWidthSync;
import com.auction.client.util.FxmlLoader;
import com.auction.client.util.NotificationUtils;
import com.auction.server.model.entity.Auction;
import com.auction.server.model.enums.AuctionStatus;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;

import javafx.scene.control.TextField;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ButtonBar;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.DatePicker;
import javafx.scene.Node;

/**
 * Controller cho màn hình Danh Sách Đấu Giá (auction-list.fxml).
 *
 * <p>
 * Hiển thị tất cả các phiên đấu giá dưới dạng bảng (TableView). Người dùng có thể: - Lọc phiên theo
 * trạng thái (OPEN, RUNNING, FINISHED, ...) - Click vào một hàng để xem chi tiết hoặc tham gia đấu
 * giá - Đăng xuất để quay lại màn hình Login
 */
public class AuctionListController implements com.auction.client.observer.AuctionObserver {

  // -- @FXML inject từ auction-list.fxml --

  @FXML
  private javafx.scene.layout.FlowPane auctionGridPane;

  // Danh sách các hàm cập nhật thời gian đếm ngược cho từng thẻ
  private final java.util.List<Runnable> timeUpdaters = new java.util.ArrayList<>();

  @FXML
  private ComboBox<String> statusFilterCombo;

  @FXML
  private Button refreshButton;

  @FXML
  private TextField searchField;

  @FXML
  private Button logoutButton;

  @FXML
  private Button myWinsButton;

  @FXML
  private Label welcomeLabel;

  // -- [Tính năng 2] Thống kê dành cho Admin --
  @FXML
  private javafx.scene.layout.HBox adminStatsPane;
  @FXML
  private Label totalAuctionsLabel;
  @FXML
  private Label runningAuctionsLabel;
  @FXML
  private Label totalValueLabel;

  // -- Bộ đếm thời gian thực --
  private javafx.animation.Timeline countdownTimeline;

  // -- Dữ liệu & Dependency --

  private final ServerService serverService = new ServerService();
  private final AuctionSessionState session = AuctionSessionState.getInstance();

  // ObservableList: khi list này thay đổi, TableView tự cập nhật giao diện (binding)
  private final ObservableList<Auction> auctionData = FXCollections.observableArrayList();

  // Định dạng hiển thị ngày giờ trên bảng
  private static final DateTimeFormatter DISPLAY_FORMAT =
      DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

  /**
   * Được JavaFX gọi tự động sau khi load FXML. Thiết lập cấu hình bảng và load dữ liệu ban đầu.
   */
  @FXML
  public void initialize() {
    // Hiển thị tên người dùng đang đăng nhập
    if (session.isLoggedIn()) {
      welcomeLabel.setText("Chào mừng, " + session.getCurrentUser().getUsername() + "!");
    }

    // Xóa các khởi tạo TableView cũ

    // [Tính năng 1] Bắt đầu bộ đếm thời gian thực (1 giây cập nhật 1 lần)
    startCountdownTimer();

    // [Tính năng 2] Hiển thị bảng thống kê nếu là Admin
    if (session.isLoggedIn()
        && session.getCurrentUser().getRole() == com.auction.server.model.enums.UserRole.ADMIN) {
      adminStatsPane.setVisible(true);
      adminStatsPane.setManaged(true);
    } else {
      // [Tính năng 3] Hiển thị nút Lịch sử thắng cho Bidder/Seller
      myWinsButton.setVisible(true);
      myWinsButton.setManaged(true);
    }

    // Bỏ tự động resize của TableView

    // [Tính năng 4] Đăng ký nhận thông báo Real-time từ Server
    serverService.addObserver(this);

    // Cấu hình ComboBox lọc trạng thái
    statusFilterCombo.setItems(FXCollections.observableArrayList("Tất cả", "Đang mở (OPEN)",
        "Đang chạy (RUNNING)", "Đã kết thúc (FINISHED)"));
    statusFilterCombo.setValue("Tất cả");
    ComboBoxPopupWidthSync.install(statusFilterCombo);

    // [Tính năng 5] Tìm kiếm theo tên sản phẩm — lọc tức thì khi gõ (không cần nhấn Enter)
    searchField.textProperty().addListener((obs, oldText, newText) -> loadAuctions());

    // Load dữ liệu lần đầu ngay khi màn hình khởi tạo
    loadAuctions();
  }

  /**
   * Gọi ServerService để lấy danh sách đấu giá và áp dụng bộ lọc nếu có. Phương thức public để nút
   * Refresh cũng có thể gọi.
   */
  @FXML
  private void handleRefresh(ActionEvent event) {
    loadAuctions();
  }

  /**
   * [Tính năng 3] Chuyển sang màn hình Lịch sử thắng cuộc.
   */
  @FXML
  private void handleViewMyWins(ActionEvent event) {
    try {
      Stage stage = (Stage) myWinsButton.getScene().getWindow();
      FxmlLoader.navigateTo(stage, "my-wins.fxml", "Online Auction System — Lịch Sử Thắng Cuộc");
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /** Load và lọc danh sách đấu giá từ server, sau đó hiển thị lên bảng. */
  private void loadAuctions() {
    List<Auction> allAuctions = serverService.getAllAuctions();
    String filter = statusFilterCombo.getValue();

    // [Tính năng 2] Cập nhật số liệu thống kê cho Admin
    if (adminStatsPane.isVisible()) {
      updateAdminStats(allAuctions);
    }

    // Lọc theo bộ lọc đang chọn — dùng Stream API để code gọn và có tính biểu đạt cao
    List<Auction> filtered = allAuctions.stream().filter(auction -> {
      if ("Tất cả".equals(filter))
        return true;
      if ("Đang mở (OPEN)".equals(filter))
        return auction.getStatus() == AuctionStatus.OPEN;
      if ("Đang chạy (RUNNING)".equals(filter))
        return auction.isRunning();
      if ("Đã kết thúc (FINISHED)".equals(filter))
        return auction.isFinished();
      return true;
    }).filter(auction -> {
      // [Tính năng 5] Lọc theo từ khóa tìm kiếm (không phân biệt hoa thường)
      String keyword = searchField.getText();
      if (keyword == null || keyword.isBlank())
        return true;
      String lowerKeyword = keyword.toLowerCase();
      String itemName = auction.getItemName();
      return itemName != null && itemName.toLowerCase().contains(lowerKeyword);
    }).collect(Collectors.toList());

    // Render thẻ giao diện thay vì TableView
    renderGrid(filtered);
  }

  /**
   * [Tính năng 2] Tính toán và hiển thị số liệu thống kê cho Admin.
   */
  private void updateAdminStats(List<Auction> auctions) {
    long total = auctions.size();
    long running = auctions.stream().filter(Auction::isRunning).count();
    long totalValue = auctions.stream().mapToLong(Auction::getCurrentPrice).sum();

    totalAuctionsLabel.setText(String.valueOf(total));
    runningAuctionsLabel.setText(String.valueOf(running));
    totalValueLabel.setText(String.format("%,d", totalValue));
  }


  /**
   * True nếu điểm bấm nằm trên control “tương tác” (nút, combobox, …) — tránh coi double-click trên
   * Sửa/Reset/Xóa là lệnh mở màn chi tiết.
   */
  private static boolean isInteractiveControlTarget(Object target) {
    if (!(target instanceof Node)) {
      return false;
    }
    for (Node n = (Node) target; n != null; n = n.getParent()) {
      if (n instanceof Button || n instanceof ComboBox<?>) {
        return true;
      }
    }
    return false;
  }

  /**
   * Phương thức chung để chuyển sang màn hình chi tiết phiên đấu giá.
   */
  private void navigateToDetail(Auction selected) {
    serverService.removeObserver(this); // Tạm dừng observer khi rời màn hình

    // Lưu phiên đang chọn vào session để AuctionDetailController đọc
    session.setSelectedAuction(selected);

    try {
      Stage stage = (Stage) searchField.getScene().getWindow(); // Thay vì lấy từ bảng
      FxmlLoader.navigateTo(stage, "auction-detail.fxml",
          "Online Auction System — Chi Tiết Sản Phẩm");
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * Tạo lưới thẻ (Card Grid) cho các phiên đấu giá
   */
  private void renderGrid(List<Auction> auctions) {
    auctionGridPane.getChildren().clear();
    timeUpdaters.clear();

    if (auctions.isEmpty()) {
      Label emptyLabel = new Label("Không có phiên đấu giá nào.");
      emptyLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #7f8c8d;");
      auctionGridPane.getChildren().add(emptyLabel);
      return;
    }

    for (Auction auction : auctions) {
      javafx.scene.layout.VBox card = new javafx.scene.layout.VBox(10);
      card.getStyleClass().add("auction-card");
      card.setPrefWidth(260);
      // Không đặt style inline ở đây để tránh xung đột với CSS

      // Hình ảnh
      javafx.scene.image.ImageView imgView = new javafx.scene.image.ImageView();
      imgView.setFitWidth(230);
      imgView.setFitHeight(180);
      imgView.setPreserveRatio(true);
      if (auction.getImageBase64() != null && !auction.getImageBase64().isEmpty()) {
        try {
          byte[] imgBytes = java.util.Base64.getDecoder().decode(auction.getImageBase64());
          imgView
              .setImage(new javafx.scene.image.Image(new java.io.ByteArrayInputStream(imgBytes)));
        } catch (Exception e) {
          // placeholder
        }
      } else {
        // Gắn placeholder nếu cần, tạm thời để trống hoặc màu nền
      }
      HBox imgContainer = new HBox(imgView);
      imgContainer.setAlignment(Pos.CENTER);
      imgContainer.setMinHeight(180);

      // Tên sản phẩm
      Label nameLbl = new Label(auction.getItemName() != null ? auction.getItemName()
          : "Sản phẩm #" + auction.getItemId());
      nameLbl.getStyleClass().add("card-title");
      nameLbl.setWrapText(true);
      nameLbl.setMinHeight(50);
      nameLbl.setMaxHeight(50);
      nameLbl.setAlignment(Pos.TOP_LEFT);

      // Giá hiện tại
      Label priceLbl = new Label(String.format("%,d VNĐ", auction.getCurrentPrice()));
      priceLbl.getStyleClass().add("card-price");

      // Mô tả ngắn (giới hạn độ dài để khung thẻ đồng đều)
      String desc = auction.getItemDescription();
      if (desc == null || desc.isEmpty()) {
        desc = "Không có mô tả.";
      } else if (desc.length() > 80) {
        desc = desc.substring(0, 77) + "...";
      }
      Label descLbl = new Label(desc);
      descLbl.getStyleClass().add("card-desc");
      descLbl.setWrapText(true);
      descLbl.setMinHeight(40);
      descLbl.setMaxHeight(40);

      // Trạng thái + Loại (Việt hóa Loại sản phẩm)
      Label statusLbl = new Label(translateStatus(auction.getStatus()) + " | "
          + translateCategoryToVi(auction.getItemCategory()));
      statusLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #bdc3c7; -fx-font-style: italic;");

      // Thời gian còn lại
      Label timeLbl = new Label();
      timeLbl.setStyle("-fx-font-size: 14px;");
      Runnable updateTime = () -> {
        String timeLeft = formatTimeLeft(auction);
        timeLbl.setText("⏳ " + timeLeft);
        if (timeLeft.startsWith("00:")) {
          timeLbl.setStyle("-fx-font-size: 14px; -fx-text-fill: #e74c3c; -fx-font-weight: bold;");
        } else if (timeLeft.contains("kết thúc") || timeLeft.contains("thanh toán")
            || timeLeft.contains("hủy")) {
          timeLbl.setStyle("-fx-font-size: 14px; -fx-text-fill: #95a5a6;");
        } else {
          timeLbl.setStyle("-fx-font-size: 14px; -fx-text-fill: #27ae60; -fx-font-weight: bold;");
        }
      };
      updateTime.run();
      timeUpdaters.add(updateTime);

      card.getChildren().addAll(imgContainer, nameLbl, priceLbl, descLbl, statusLbl, timeLbl);

      // Admin Action Buttons
      if (session.isLoggedIn()
          && session.getCurrentUser().getRole() == com.auction.server.model.enums.UserRole.ADMIN) {
        Button editBtn = new Button("Sửa");
        editBtn.getStyleClass().add("secondary-button");
        editBtn.setOnAction(e -> showEditDialog(auction));

        Button deleteBtn = new Button("Xóa");
        deleteBtn.getStyleClass().add("danger-button");
        deleteBtn.setOnAction(e -> {
          javafx.scene.control.Alert confirmAlert =
              new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.CONFIRMATION);
          NotificationUtils.styleAlert(confirmAlert);
          confirmAlert.setGraphic(null);
          confirmAlert.setTitle("Xác Nhận Xóa Phiên");
          confirmAlert.setHeaderText(
              "Bạn có chắc chắn muốn xóa phiên đấu giá #" + auction.getId() + " không?");
          java.util.Optional<javafx.scene.control.ButtonType> confirmResult =
              confirmAlert.showAndWait();
          if (confirmResult.isPresent()
              && confirmResult.get() == javafx.scene.control.ButtonType.OK) {
            if (serverService.deleteAuction(auction.getId())) {
              NotificationUtils.showSuccess((Stage) searchField.getScene().getWindow(),
                  "Đã xóa phiên #" + auction.getId());
              loadAuctions();
            }
          }
        });

        // Thêm vùng đệm để đẩy nút xuống dưới cùng
        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        javafx.scene.layout.VBox.setVgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        card.getChildren().add(spacer);

        HBox adminActions = new HBox(10, editBtn, deleteBtn);
        adminActions.setAlignment(Pos.CENTER);
        adminActions.setPadding(new Insets(10, 0, 0, 0));
        card.getChildren().add(adminActions);
      } else {
        // Đối với User, cũng thêm spacer để các thành phần bên trên thẳng hàng
        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        javafx.scene.layout.VBox.setVgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        card.getChildren().add(spacer);
      }

      // Click vào thẻ để xem chi tiết (áp dụng cho cả User và Admin)
      card.setOnMouseClicked(e -> {
        // Nếu bấm vào các nút điều khiển (Sửa/Xóa) thì không chuyển trang chi tiết
        if (isInteractiveControlTarget(e.getTarget()))
          return;

        if (e.getButton() == javafx.scene.input.MouseButton.PRIMARY) {
          navigateToDetail(auction);
        }
      });
      card.setStyle(card.getStyle() + " -fx-cursor: hand;");

      auctionGridPane.getChildren().add(card);
    }
  }

  /**
   * Xử lý đăng xuất: xóa session và quay về màn hình Login.
   *
   * @param event ActionEvent từ logoutButton
   */
  @FXML
  private void handleLogout(ActionEvent event) {
    // [Thêm bước xác nhận] Hỏi bạn trước khi đăng xuất
    javafx.scene.control.Alert alert =
        new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.CONFIRMATION);
    NotificationUtils.styleAlert(alert);
    alert.setGraphic(null); // Bỏ dấu "?"
    alert.setTitle("Xác Nhận Đăng Xuất");
    alert.setHeaderText("Bạn có thực sự muốn đăng xuất không?");

    java.util.Optional<javafx.scene.control.ButtonType> result = alert.showAndWait();
    if (result.isPresent() && result.get() == javafx.scene.control.ButtonType.OK) {
      serverService.removeObserver(this); // Hủy observer khi đăng xuất
      session.clearSession(); // Xóa thông tin đăng nhập
      try {
        Stage stage = (Stage) logoutButton.getScene().getWindow();
        FxmlLoader.navigateTo(stage, "login.fxml", "Online Auction System — Đăng Nhập");
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  /**
   * Hiển thị hộp thoại chỉnh sửa phiên đấu giá dành cho Admin. Cải tiến: Cho phép sửa cả Ngày bắt
   * đầu và Ngày kết thúc.
   */
  private void showEditDialog(Auction auction) {
    Dialog<ButtonType> dialog = new Dialog<>();
    dialog.setTitle("⚙ Quản Lý Phiên Đấu Giá #" + auction.getId());
    dialog.setHeaderText("Chỉnh sửa thông số phiên đấu giá vi phạm hoặc cần điều chỉnh.");

    // Áp dụng style dark theme cho Dialog
    NotificationUtils.styleDialog(dialog);

    ButtonType saveButtonType = new ButtonType("Lưu thay đổi", ButtonBar.ButtonData.OK_DONE);
    dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

    GridPane grid = new GridPane();
    grid.setHgap(15);
    grid.setVgap(15);
    grid.setPadding(new Insets(20, 30, 20, 30));

    // 1. Giá hiện tại
    TextField priceField = new TextField(String.valueOf(auction.getCurrentPrice()));
    priceField.setPromptText("Nhập số tiền...");

    // 2. Trạng thái
    ComboBox<String> statusCombo = new ComboBox<>(
        FXCollections.observableArrayList("OPEN", "RUNNING", "FINISHED", "PAID", "CANCELED"));
    statusCombo.setValue(auction.getStatus().name());
    statusCombo.setMaxWidth(Double.MAX_VALUE);

    // 3. THỜI GIAN BẮT ĐẦU (MỚI THÊM)
    LocalDateTime currentStart = auction.getStartTime();
    DatePicker startDatePicker = new DatePicker(currentStart.toLocalDate());
    startDatePicker.setMaxWidth(Double.MAX_VALUE);

    ComboBox<Integer> startHourCombo = new ComboBox<>();
    for (int i = 0; i < 24; i++)
      startHourCombo.getItems().add(i);
    startHourCombo.setValue(currentStart.getHour());

    ComboBox<Integer> startMinuteCombo = new ComboBox<>();
    for (int i = 0; i < 60; i++)
      startMinuteCombo.getItems().add(i);
    startMinuteCombo.setValue(currentStart.getMinute());

    HBox startTimePickerBox =
        new HBox(5, startHourCombo, new Label("giờ"), startMinuteCombo, new Label("phút"));
    startTimePickerBox.setAlignment(Pos.CENTER_LEFT);
    startTimePickerBox.getStyleClass().add("time-picker-unit");

    // 4. THỜI GIAN KẾT THÚC
    LocalDateTime currentEnd = auction.getEndTime();
    DatePicker endDatePicker = new DatePicker(currentEnd.toLocalDate());
    endDatePicker.setMaxWidth(Double.MAX_VALUE);

    ComboBox<Integer> endHourCombo = new ComboBox<>();
    for (int i = 0; i < 24; i++)
      endHourCombo.getItems().add(i);
    endHourCombo.setValue(currentEnd.getHour());

    ComboBox<Integer> endMinuteCombo = new ComboBox<>();
    for (int i = 0; i < 60; i++)
      endMinuteCombo.getItems().add(i);
    endMinuteCombo.setValue(currentEnd.getMinute());

    HBox endTimePickerBox =
        new HBox(5, endHourCombo, new Label("giờ"), endMinuteCombo, new Label("phút"));
    endTimePickerBox.setAlignment(Pos.CENTER_LEFT);
    endTimePickerBox.getStyleClass().add("time-picker-unit");

    // 5. Loại sản phẩm (Việt hóa và bổ sung mục KHÁC)
    ComboBox<String> categoryCombo =
        new ComboBox<>(FXCollections.observableArrayList("Điện tử", "Nghệ thuật", "Xe cộ", "Khác"));

    // Tự động chọn giá trị hiện tại của phiên
    String currentCatEn = auction.getItemCategory();
    categoryCombo.setValue(translateCategoryToVi(currentCatEn));
    categoryCombo.setMaxWidth(Double.MAX_VALUE);

    ComboBoxPopupWidthSync.install(statusCombo);
    ComboBoxPopupWidthSync.install(startHourCombo);
    ComboBoxPopupWidthSync.install(startMinuteCombo);
    ComboBoxPopupWidthSync.install(endHourCombo);
    ComboBoxPopupWidthSync.install(endMinuteCombo);
    ComboBoxPopupWidthSync.install(categoryCombo);

    // Thêm các control vào grid với Label trắng sáng
    grid.add(new Label("Giá hiện tại:"), 0, 0);
    grid.add(priceField, 1, 0);

    grid.add(new Label("Trạng thái:"), 0, 1);
    grid.add(statusCombo, 1, 1);

    grid.add(new Label("Ngày bắt đầu:"), 0, 2);
    grid.add(startDatePicker, 1, 2);
    grid.add(new Label("Giờ bắt đầu:"), 0, 3);
    grid.add(startTimePickerBox, 1, 3);

    grid.add(new Label("Ngày kết thúc:"), 0, 4);
    grid.add(endDatePicker, 1, 4);
    grid.add(new Label("Giờ kết thúc:"), 0, 5);
    grid.add(endTimePickerBox, 1, 5);

    grid.add(new Label("Loại sản phẩm:"), 0, 6);
    grid.add(categoryCombo, 1, 6);

    dialog.getDialogPane().setContent(grid);

    dialog.setResultConverter(dialogButton -> {
      if (dialogButton == saveButtonType) {
        try {
          long price = Long.parseLong(priceField.getText().trim());
          String status = statusCombo.getValue();
          // Dịch từ Tiếng Việt sang Enum Tiếng Anh trước khi gửi lên Server
          String category = translateCategoryToEn(categoryCombo.getValue());

          // Ghép Ngày bắt đầu
          LocalDateTime newStart = LocalDateTime.of(startDatePicker.getValue(),
              java.time.LocalTime.of(startHourCombo.getValue(), startMinuteCombo.getValue()));
          String startTimeStr = newStart.toString();

          // Ghép Ngày kết thúc
          LocalDateTime newEnd = LocalDateTime.of(endDatePicker.getValue(),
              java.time.LocalTime.of(endHourCombo.getValue(), endMinuteCombo.getValue()));
          String endTimeStr = newEnd.toString();

          boolean success = serverService.updateAuctionAdmin(auction.getId(), price, status,
              startTimeStr, endTimeStr, category);
          if (success) {
            NotificationUtils.showSuccess((Stage) searchField.getScene().getWindow(),
                "Cập nhật phiên #" + auction.getId() + " thành công!");
            loadAuctions();
          } else {
            NotificationUtils.showError((Stage) searchField.getScene().getWindow(),
                "Cập nhật thất bại. Vui lòng thử lại.");
          }
        } catch (Exception e) {
          NotificationUtils.showError((Stage) searchField.getScene().getWindow(),
              "Dữ liệu nhập vào không hợp lệ!");
          e.printStackTrace();
        }
      }
      return dialogButton;
    });

    dialog.showAndWait();
  }

  /**
   * Chuyển đổi tên enum sang tiếng Việt để hiển thị thân thiện hơn với người dùng.
   *
   * @param status trạng thái phiên đấu giá
   * @return chuỗi tiếng Việt tương ứng
   */
  private String translateStatus(AuctionStatus status) {
    if (status == null)
      return "Không rõ";
    return switch (status) {
      case OPEN -> "Đang mở";
      case RUNNING -> "Đang diễn ra";
      case FINISHED -> "Đã kết thúc";
      case PAID -> "Đã thanh toán";
      case CANCELED -> "Đã hủy";
    };
  }

  /**
   * Chuyển đổi Enum Category sang Tiếng Việt để hiển thị trên UI.
   */
  private String translateCategoryToVi(String categoryEn) {
    if (categoryEn == null)
      return "Khác";
    return switch (categoryEn.toUpperCase()) {
      case "ELECTRONICS" -> "Điện tử";
      case "ARTWORK" -> "Nghệ thuật";
      case "VEHICLE" -> "Xe cộ";
      case "OTHER" -> "Khác";
      default -> "Khác";
    };
  }

  /**
   * Chuyển đổi từ Tiếng Việt trên UI về Enum Tiếng Anh để gửi lên Server.
   */
  private String translateCategoryToEn(String categoryVi) {
    if (categoryVi == null)
      return "OTHER";
    return switch (categoryVi) {
      case "Điện tử" -> "ELECTRONICS";
      case "Nghệ thuật" -> "ARTWORK";
      case "Xe cộ" -> "VEHICLE";
      case "Khác" -> "OTHER";
      default -> "OTHER";
    };
  }

  /**
   * [Tính năng 1] Xóa bỏ column time setup
   */

  /**
   * [Tính năng 1] Khởi chạy bộ đếm 1 giây một lần để cập nhật UI.
   */
  private void startCountdownTimer() {
    // Timeline là cơ chế đếm nhịp của JavaFX, chạy trên UI Thread nên an toàn để cập nhật giao diện
    countdownTimeline = new javafx.animation.Timeline(
        new javafx.animation.KeyFrame(javafx.util.Duration.seconds(1), event -> {
          for (Runnable r : timeUpdaters) {
            r.run();
          }
        }));
    countdownTimeline.setCycleCount(javafx.animation.Timeline.INDEFINITE);
    countdownTimeline.play();
  }

  /**
   * [Tính năng 1] Chuỗi hiển thị cho cột "Còn lại" (đếm ngược theo {@code endTime}).
   *
   * <p>
   * <b>Vì sao trước đây vừa "Đã kết thúc" vừa "Đang kết thúc..."?</b> Trạng thái {@code
   * FINISHED} trong DB chỉ được gán khi server cập nhật (đặt giá sau giờ, admin sửa, v.v.). Nhiều
   * phiên vẫn là {@code RUNNING} trên DB dù đồng hồ đã quá {@code endTime} — code cũ coi duration
   * âm là "Đang kết thúc..." và treo mãi. <b>Theo thời gian thực</b>, hết {@code endTime} là hết
   * phiên đấu: hiển thị "Đã kết thúc" cho đồng nhất với cột trạng thái mà người dùng kỳ vọng.
   */
  private String formatTimeLeft(Auction a) {
    if (a.getStatus() == AuctionStatus.FINISHED || a.isFinished()) {
      return "Đã kết thúc";
    }
    if (a.getStatus() == AuctionStatus.PAID) {
      return "Đã thanh toán";
    }
    if (a.getStatus() == AuctionStatus.CANCELED) {
      return "Đã hủy";
    }

    LocalDateTime now = LocalDateTime.now();
    LocalDateTime end = a.getEndTime();

    // Đã quá mốc kết thúc theo lịch — không phụ thuộc DB đã kịp FINISHED hay chưa
    if (!now.isBefore(end)) {
      return "Đã kết thúc";
    }

    if (a.getStatus() == AuctionStatus.OPEN) {
      return "Chưa bắt đầu";
    }

    java.time.Duration d = java.time.Duration.between(now, end);
    long days = d.toDays();
    long hours = d.toHoursPart();
    long minutes = d.toMinutesPart();
    long seconds = d.toSecondsPart();

    if (days > 0) {
      return String.format("%d ngày %02d:%02d", days, hours, minutes);
    }
    return String.format("%02d:%02d:%02d", hours, minutes, seconds);
  }

  /**
   * [Tính năng 4] Nhận cập nhật khi có người đặt giá mới.
   */
  @Override
  public void onBidUpdated(com.auction.server.model.entity.BidTransaction bid) {
    Platform.runLater(() -> {
      // Hiện thông báo nhỏ góc màn hình
      NotificationUtils.showToast((Stage) searchField.getScene().getWindow(), "📣 Giá mới cho #"
          + bid.getAuctionId() + ": " + String.format("%,d", bid.getAmount()) + " VNĐ", false);

      // Refresh lại bảng để số liệu luôn mới nhất
      loadAuctions();
    });
  }

  /**
   * [Tính năng 4] Nhận cập nhật khi trạng thái phiên thay đổi (Admin xóa/sửa).
   */
  @Override
  public void onAuctionStatusChanged(Long auctionId, String newStatus) {
    Platform.runLater(() -> {
      NotificationUtils.showToast((Stage) searchField.getScene().getWindow(),
          "🔔 Phiên #" + auctionId + " đổi trạng thái: " + newStatus, newStatus.equals("CANCELED"));

      // Refresh lại bảng
      loadAuctions();
    });
  }
}
