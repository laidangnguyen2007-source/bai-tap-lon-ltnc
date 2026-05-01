package com.auction.client.controller;

import com.auction.client.model.AuctionSessionState;
import com.auction.client.service.ServerService;
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

import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.util.Pair;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.DatePicker;

/**
 * Controller cho màn hình Danh Sách Đấu Giá (auction-list.fxml).
 *
 * <p>Hiển thị tất cả các phiên đấu giá dưới dạng bảng (TableView). Người dùng có thể: - Lọc
 * phiên theo trạng thái (OPEN, RUNNING, FINISHED, ...) - Click vào một hàng để xem chi tiết hoặc
 * tham gia đấu giá - Đăng xuất để quay lại màn hình Login
 */
public class AuctionListController implements com.auction.client.observer.AuctionObserver {

  // -- @FXML inject từ auction-list.fxml --

  @FXML private TableView<Auction> auctionTable;

  @FXML private TableColumn<Auction, Long> idColumn;

  @FXML private TableColumn<Auction, String> itemNameColumn;

  @FXML private TableColumn<Auction, String> statusColumn;

  @FXML private TableColumn<Auction, String> currentPriceColumn;

  @FXML private TableColumn<Auction, String> startTimeColumn;

  @FXML private TableColumn<Auction, String> endTimeColumn;

  @FXML private TableColumn<Auction, Void> actionsColumn;

  @FXML private TableColumn<Auction, String> timeLeftColumn;

  @FXML private ComboBox<String> statusFilterCombo;

  @FXML private Button refreshButton;

  @FXML private Button logoutButton;

  @FXML private Button myWinsButton;

  @FXML private Label welcomeLabel;

  // -- [Tính năng 2] Thống kê dành cho Admin --
  @FXML private javafx.scene.layout.HBox adminStatsPane;
  @FXML private Label totalAuctionsLabel;
  @FXML private Label runningAuctionsLabel;
  @FXML private Label totalValueLabel;

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

    // Thiết lập từng cột của TableView — liên kết với thuộc tính của Auction object
    idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
    
    // Cột tên sản phẩm: dùng lambda để ép hiển thị, tránh lỗi reflection
    itemNameColumn.setCellValueFactory(cellData -> {
        String name = cellData.getValue().getItemName();
        return new SimpleStringProperty(name != null ? name : "Sản phẩm #" + cellData.getValue().getItemId());
    });

    // Cột trạng thái: hiển thị tên tiếng Việt thay vì tên enum tiếng Anh
    statusColumn.setCellValueFactory(
        cellData ->
            new SimpleStringProperty(translateStatus(cellData.getValue().getStatus())));

    // Cột giá: định dạng số có dấu phân cách hàng nghìn và thêm đơn vị VNĐ
    currentPriceColumn.setCellValueFactory(
        cellData -> {
          long price = cellData.getValue().getCurrentPrice();
          return new SimpleStringProperty(String.format("%,d VNĐ", price));
        });

    // Cột thời gian: format LocalDateTime thành chuỗi có thể đọc được
    startTimeColumn.setCellValueFactory(
        cellData -> {
          LocalDateTime t = cellData.getValue().getStartTime();
          return new SimpleStringProperty(t != null ? t.format(DISPLAY_FORMAT) : "—");
        });

    endTimeColumn.setCellValueFactory(
        cellData -> {
          LocalDateTime t = cellData.getValue().getEndTime();
          return new SimpleStringProperty(t != null ? t.format(DISPLAY_FORMAT) : "—");
        });

    // [Tính năng mới] Click đúp vào hàng để xem chi tiết luôn
    auctionTable.setRowFactory(tv -> {
        javafx.scene.control.TableRow<Auction> row = new javafx.scene.control.TableRow<>();
        row.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && (!row.isEmpty())) {
                Auction auction = row.getItem();
                navigateToDetail(auction);
            }
        });
        return row;
    });

    // Cấu hình cột Thao tác cho Admin
    if (session.isLoggedIn() && session.getCurrentUser().getRole() == com.auction.server.model.enums.UserRole.ADMIN) {
        actionsColumn.setPrefWidth(280);
        actionsColumn.setCellFactory(param -> new javafx.scene.control.TableCell<>() {
            private final Button deleteBtn = new Button("🗑 Xóa");
            private final Button editBtn = new Button("⚙ Sửa");
            private final Button resetBtn = new Button("🔄 Reset");
            private final HBox container = new HBox(8, editBtn, resetBtn, deleteBtn);
            {
                container.setAlignment(Pos.CENTER);
                deleteBtn.getStyleClass().add("danger-button");
                editBtn.getStyleClass().add("secondary-button");
                resetBtn.getStyleClass().add("primary-button");
                
                deleteBtn.setOnAction(event -> {
                    Auction auction = getTableView().getItems().get(getIndex());
                    
                    // [Thêm bước xác nhận] Hỏi bạn trước khi xóa phiên đấu giá
                    javafx.scene.control.Alert confirmAlert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.CONFIRMATION);
                    NotificationUtils.styleAlert(confirmAlert);
                    confirmAlert.setGraphic(null); // Bỏ dấu "?"
                    confirmAlert.setTitle("Xác Nhận Xóa Phiên");
                    confirmAlert.setHeaderText("Bạn có chắc chắn muốn xóa phiên đấu giá #" + auction.getId() + " không?");
                    confirmAlert.setContentText("Hành động này sẽ gỡ bỏ hoàn toàn phiên đấu giá khỏi hệ thống.");

                    java.util.Optional<javafx.scene.control.ButtonType> confirmResult = confirmAlert.showAndWait();
                    if (confirmResult.isPresent() && confirmResult.get() == javafx.scene.control.ButtonType.OK) {
                        boolean success = serverService.deleteAuction(auction.getId());
                        if (success) {
                            NotificationUtils.showSuccess((Stage) auctionTable.getScene().getWindow(), "Đã xóa phiên #" + auction.getId());
                            loadAuctions();
                        }
                    }
                });

                editBtn.setOnAction(event -> {
                    Auction auction = getTableView().getItems().get(getIndex());
                    showEditDialog(auction);
                });

                resetBtn.setOnAction(event -> {
                    Auction auction = getTableView().getItems().get(getIndex());
                    
                    // [Thêm bước xác nhận] Hỏi bạn trước khi Reset (dọn rác)
                    javafx.scene.control.Alert confirmAlert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.CONFIRMATION);
                    NotificationUtils.styleAlert(confirmAlert);
                    confirmAlert.setGraphic(null); // Bỏ dấu "?"
                    confirmAlert.setTitle("Xác Nhận Reset Phiên");
                    confirmAlert.setHeaderText("Bạn có muốn dọn sạch lịch sử và đưa giá phiên #" + auction.getId() + " về ban đầu?");
                    confirmAlert.setContentText("Hành động này sẽ xóa vĩnh viễn toàn bộ lịch sử đặt giá rác.");

                    java.util.Optional<javafx.scene.control.ButtonType> confirmResult = confirmAlert.showAndWait();
                    if (confirmResult.isPresent() && confirmResult.get() == javafx.scene.control.ButtonType.OK) {
                        boolean success = serverService.resetAuction(auction.getId());
                        if (success) {
                            NotificationUtils.showSuccess((Stage) auctionTable.getScene().getWindow(), "Đã Reset và dọn rác cho phiên #" + auction.getId());
                            loadAuctions();
                        }
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(container);
                }
            }
        });
    } else {
        // Nếu không phải Admin, ẩn cột Thao tác đi cho đẹp
        actionsColumn.setVisible(false);
    }

    // Gắn dữ liệu vào bảng — mọi thay đổi của auctionData tự động cập nhật bảng
    auctionTable.setItems(auctionData);

    // [Tính năng 1] Khởi tạo cột Thời gian còn lại
    setupTimeLeftColumn();

    // [Tính năng 1] Bắt đầu bộ đếm thời gian thực (1 giây cập nhật 1 lần)
    startCountdownTimer();

    // [Tính năng 2] Hiển thị bảng thống kê nếu là Admin
    if (session.isLoggedIn() && session.getCurrentUser().getRole() == com.auction.server.model.enums.UserRole.ADMIN) {
        adminStatsPane.setVisible(true);
        adminStatsPane.setManaged(true);
    } else {
        // [Tính năng 3] Hiển thị nút Lịch sử thắng cho Bidder/Seller
        myWinsButton.setVisible(true);
        myWinsButton.setManaged(true);
    }

    // Giãn đều các cột để không còn ô trống thừa ở cuối bảng
    auctionTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

    // [Tính năng 4] Đăng ký nhận thông báo Real-time từ Server
    serverService.addObserver(this);

    // Cấu hình ComboBox lọc trạng thái
    statusFilterCombo.setItems(
        FXCollections.observableArrayList(
            "Tất cả", "Đang mở (OPEN)", "Đang chạy (RUNNING)", "Đã kết thúc (FINISHED)"));
    statusFilterCombo.setValue("Tất cả");

    // Load dữ liệu lần đầu ngay khi màn hình khởi tạo
    loadAuctions();
  }

  /**
   * Gọi ServerService để lấy danh sách đấu giá và áp dụng bộ lọc nếu có. Phương thức public để
   * nút Refresh cũng có thể gọi.
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
    List<Auction> filtered =
        allAuctions.stream()
            .filter(
                auction -> {
                  if ("Tất cả".equals(filter)) return true;
                  if ("Đang mở (OPEN)".equals(filter))
                    return auction.getStatus() == AuctionStatus.OPEN;
                  if ("Đang chạy (RUNNING)".equals(filter)) return auction.isRunning();
                  if ("Đã kết thúc (FINISHED)".equals(filter)) return auction.isFinished();
                  return true;
                })
            .collect(Collectors.toList());

    // Cập nhật ObservableList — TableView tự động refresh giao diện
    auctionData.setAll(filtered);
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
   * Phương thức chung để chuyển sang màn hình chi tiết phiên đấu giá.
   */
  private void navigateToDetail(Auction selected) {
    serverService.removeObserver(this); // Tạm dừng observer khi rời màn hình
    
    // Lưu phiên đang chọn vào session để AuctionDetailController đọc
    session.setSelectedAuction(selected);

    try {
      Stage stage = (Stage) auctionTable.getScene().getWindow();
      FxmlLoader.navigateTo(
          stage, "auction-detail.fxml", "Online Auction System — Chi Tiết Sản Phẩm");
    } catch (IOException e) {
      e.printStackTrace();
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
    javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.CONFIRMATION);
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
   * Hiển thị hộp thoại chỉnh sửa phiên đấu giá dành cho Admin.
   * Cải tiến: Cho phép sửa cả Ngày bắt đầu và Ngày kết thúc.
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
      ComboBox<String> statusCombo = new ComboBox<>(FXCollections.observableArrayList("OPEN", "RUNNING", "FINISHED", "PAID", "CANCELED"));
      statusCombo.setValue(auction.getStatus().name());
      statusCombo.setMaxWidth(Double.MAX_VALUE);
      
      // 3. THỜI GIAN BẮT ĐẦU (MỚI THÊM)
      LocalDateTime currentStart = auction.getStartTime();
      DatePicker startDatePicker = new DatePicker(currentStart.toLocalDate());
      startDatePicker.setMaxWidth(Double.MAX_VALUE);
      
      ComboBox<Integer> startHourCombo = new ComboBox<>();
      for (int i = 0; i < 24; i++) startHourCombo.getItems().add(i);
      startHourCombo.setValue(currentStart.getHour());
      
      ComboBox<Integer> startMinuteCombo = new ComboBox<>();
      for (int i = 0; i < 60; i++) startMinuteCombo.getItems().add(i);
      startMinuteCombo.setValue(currentStart.getMinute());

      HBox startTimePickerBox = new HBox(5, startHourCombo, new Label("giờ"), startMinuteCombo, new Label("phút"));
      startTimePickerBox.setAlignment(Pos.CENTER_LEFT);
      startTimePickerBox.getStyleClass().add("time-picker-unit");

      // 4. THỜI GIAN KẾT THÚC
      LocalDateTime currentEnd = auction.getEndTime();
      DatePicker endDatePicker = new DatePicker(currentEnd.toLocalDate());
      endDatePicker.setMaxWidth(Double.MAX_VALUE);
      
      ComboBox<Integer> endHourCombo = new ComboBox<>();
      for (int i = 0; i < 24; i++) endHourCombo.getItems().add(i);
      endHourCombo.setValue(currentEnd.getHour());
      
      ComboBox<Integer> endMinuteCombo = new ComboBox<>();
      for (int i = 0; i < 60; i++) endMinuteCombo.getItems().add(i);
      endMinuteCombo.setValue(currentEnd.getMinute());

      HBox endTimePickerBox = new HBox(5, endHourCombo, new Label("giờ"), endMinuteCombo, new Label("phút"));
      endTimePickerBox.setAlignment(Pos.CENTER_LEFT);
      endTimePickerBox.getStyleClass().add("time-picker-unit");
      
      // 5. Loại sản phẩm
      ComboBox<String> categoryCombo = new ComboBox<>(FXCollections.observableArrayList("ELECTRONICS", "ARTWORK", "VEHICLE"));
      categoryCombo.setValue("ELECTRONICS");
      categoryCombo.setMaxWidth(Double.MAX_VALUE);

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
                  String category = categoryCombo.getValue();
                  
                  // Ghép Ngày bắt đầu
                  LocalDateTime newStart = LocalDateTime.of(
                      startDatePicker.getValue(),
                      java.time.LocalTime.of(startHourCombo.getValue(), startMinuteCombo.getValue())
                  );
                  String startTimeStr = newStart.toString();

                  // Ghép Ngày kết thúc
                  LocalDateTime newEnd = LocalDateTime.of(
                      endDatePicker.getValue(),
                      java.time.LocalTime.of(endHourCombo.getValue(), endMinuteCombo.getValue())
                  );
                  String endTimeStr = newEnd.toString();
                  
                  boolean success = serverService.updateAuctionAdmin(auction.getId(), price, status, startTimeStr, endTimeStr, category);
                  if (success) {
                      NotificationUtils.showSuccess((Stage) auctionTable.getScene().getWindow(), "Cập nhật phiên #" + auction.getId() + " thành công!");
                      loadAuctions();
                  } else {
                      NotificationUtils.showError((Stage) auctionTable.getScene().getWindow(), "Cập nhật thất bại. Vui lòng thử lại.");
                  }
              } catch (Exception e) {
                  NotificationUtils.showError((Stage) auctionTable.getScene().getWindow(), "Dữ liệu nhập vào không hợp lệ!");
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
    if (status == null) return "Không rõ";
    return switch (status) {
      case OPEN -> "Đang mở";
      case RUNNING -> "Đang diễn ra";
      case FINISHED -> "Đã kết thúc";
      case PAID -> "Đã thanh toán";
      case CANCELED -> "Đã hủy";
    };
  }

  /**
   * [Tính năng 1] Thiết lập cột "Còn lại" để hiển thị đếm ngược thời gian thực.
   */
  private void setupTimeLeftColumn() {
      timeLeftColumn.setCellValueFactory(cellData -> {
          Auction a = cellData.getValue();
          return new SimpleStringProperty(formatTimeLeft(a));
      });

      // Tạo style riêng cho cột này: nếu sắp hết giờ thì hiện chữ đỏ để tạo áp lực cho người mua
      timeLeftColumn.setCellFactory(column -> new javafx.scene.control.TableCell<>() {
          @Override
          protected void updateItem(String item, boolean empty) {
              super.updateItem(item, empty);
              if (empty || item == null) {
                  setText(null);
                  setStyle("");
              } else {
                  setText(item);
                  // LOGIC MÀU SẮC:
                  // Nếu còn dưới 1 giờ (định dạng 00:mm:ss), tô màu đỏ rực và in đậm
                  if (item.startsWith("00:")) {
                      setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                  } else if (item.equals("Đã kết thúc") || item.contains("kết thúc")) {
                      setStyle("-fx-text-fill: #95a5a6;"); // Màu xám cho phiên đã xong
                  } else {
                      setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;"); // Màu xanh lá cho phiên còn nhiều thời gian
                  }
              }
          }
      });
  }

  /**
   * [Tính năng 1] Khởi chạy bộ đếm 1 giây một lần để cập nhật UI.
   */
  private void startCountdownTimer() {
      // Timeline là cơ chế đếm nhịp của JavaFX, chạy trên UI Thread nên an toàn để cập nhật giao diện
      countdownTimeline = new javafx.animation.Timeline(
          new javafx.animation.KeyFrame(javafx.util.Duration.seconds(1), event -> {
              // Gọi refresh() để TableView vẽ lại toàn bộ các dòng, từ đó cập nhật số giây mới
              auctionTable.refresh();
          })
      );
      countdownTimeline.setCycleCount(javafx.animation.Timeline.INDEFINITE);
      countdownTimeline.play();
  }

  /**
   * [Tính năng 1] Hàm helper tính toán chuỗi hiển thị thời gian còn lại (Countdown logic).
   */
  private String formatTimeLeft(Auction a) {
      if (a.getStatus() == AuctionStatus.FINISHED || a.isFinished()) return "Đã kết thúc";
      if (a.getStatus() == AuctionStatus.OPEN) return "Chưa bắt đầu";
      
      // Tính khoảng cách giữa thời điểm hiện tại và lúc kết thúc
      java.time.Duration d = java.time.Duration.between(LocalDateTime.now(), a.getEndTime());
      if (d.isNegative() || d.isZero()) return "Đang kết thúc...";

      long days = d.toDays();
      long hours = d.toHoursPart();
      long minutes = d.toMinutesPart();
      long seconds = d.toSecondsPart();

      // Nếu còn trên 1 ngày: Hiện "X ngày HH:mm"
      if (days > 0) return String.format("%d ngày %02d:%02d", days, hours, minutes);
      
      // Nếu dưới 1 ngày: Hiện định dạng đồng hồ "HH:mm:ss"
      return String.format("%02d:%02d:%02d", hours, minutes, seconds);
  }

  /**
   * [Tính năng 4] Nhận cập nhật khi có người đặt giá mới.
   */
  @Override
  public void onBidUpdated(com.auction.server.model.entity.BidTransaction bid) {
      Platform.runLater(() -> {
          // Hiện thông báo nhỏ góc màn hình
          NotificationUtils.showToast((Stage) auctionTable.getScene().getWindow(), 
              "📣 Giá mới cho #" + bid.getAuctionId() + ": " + String.format("%,d", bid.getAmount()) + " VNĐ", false);
          
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
          NotificationUtils.showToast((Stage) auctionTable.getScene().getWindow(), 
              "🔔 Phiên #" + auctionId + " đổi trạng thái: " + newStatus, newStatus.equals("CANCELED"));
          
          // Refresh lại bảng
          loadAuctions();
      });
  }
}
