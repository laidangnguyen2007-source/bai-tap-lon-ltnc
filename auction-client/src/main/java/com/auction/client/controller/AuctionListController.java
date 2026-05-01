package com.auction.client.controller;

import com.auction.client.model.AuctionSessionState;
import com.auction.client.service.ServerService;
import com.auction.client.util.FxmlLoader;
import com.auction.server.model.entity.Auction;
import com.auction.server.model.enums.AuctionStatus;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;

import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

/**
 * Controller cho màn hình Danh Sách Đấu Giá (auction-list.fxml).
 *
 * <p>Hiển thị tất cả các phiên đấu giá dưới dạng bảng (TableView). Người dùng có thể: - Lọc
 * phiên theo trạng thái (OPEN, RUNNING, FINISHED, ...) - Click vào một hàng để xem chi tiết hoặc
 * tham gia đấu giá - Đăng xuất để quay lại màn hình Login
 */
public class AuctionListController {

  // -- @FXML inject từ auction-list.fxml --

  @FXML private TableView<Auction> auctionTable;

  @FXML private TableColumn<Auction, Long> idColumn;

  @FXML private TableColumn<Auction, String> itemNameColumn;

  @FXML private TableColumn<Auction, String> statusColumn;

  @FXML private TableColumn<Auction, String> currentPriceColumn;

  @FXML private TableColumn<Auction, String> startTimeColumn;

  @FXML private TableColumn<Auction, String> endTimeColumn;

  @FXML private ComboBox<String> statusFilterCombo;

  @FXML private Button refreshButton;

  @FXML private Button viewDetailButton;

  @FXML private Button logoutButton;

  @FXML private Label welcomeLabel;

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

    // Gắn dữ liệu vào bảng — mọi thay đổi của auctionData tự động cập nhật bảng
    auctionTable.setItems(auctionData);
    // Giãn đều các cột để không còn ô trống thừa ở cuối bảng
    auctionTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

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

  /** Load và lọc danh sách đấu giá từ server, sau đó hiển thị lên bảng. */
  private void loadAuctions() {
    List<Auction> allAuctions = serverService.getAllAuctions();
    String filter = statusFilterCombo.getValue();

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
   * Xử lý khi người dùng nhấn "Xem Chi Tiết". Lấy phiên được chọn trong bảng và chuyển màn hình.
   *
   * @param event ActionEvent từ viewDetailButton
   */
  @FXML
  private void handleViewDetail(ActionEvent event) {
    Auction selected = auctionTable.getSelectionModel().getSelectedItem();

    // Lập trình phòng thủ: không làm gì nếu chưa chọn hàng
    if (selected == null) {
      return;
    }

    // Lưu phiên đang chọn vào session để AuctionDetailController đọc
    session.setSelectedAuction(selected);

    try {
      Stage stage = (Stage) viewDetailButton.getScene().getWindow();
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
    session.clearSession(); // Xóa thông tin đăng nhập
    try {
      Stage stage = (Stage) logoutButton.getScene().getWindow();
      FxmlLoader.navigateTo(stage, "login.fxml", "Online Auction System — Đăng Nhập");
    } catch (IOException e) {
      e.printStackTrace();
    }
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
}
