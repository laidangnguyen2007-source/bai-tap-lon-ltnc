package com.auction.client.controller;

import com.auction.client.model.AuctionSessionState;
import com.auction.client.observer.AuctionObserver;
import com.auction.client.service.ServerService;
import com.auction.client.util.FxmlLoader;
import com.auction.server.model.entity.Auction;
import com.auction.server.model.entity.BidTransaction;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

/**
 * Controller cho màn hình Phòng Đấu Giá Trực Tiếp (bidding-room.fxml).
 *
 * <p><b>Đây là Controller phức tạp và quan trọng nhất của Thành viên 4.</b>
 *
 * <p><b>Vai trò trong mô hình Observer:</b> Lớp này implement {@link AuctionObserver} để nhận
 * thông báo realtime từ TV3 (NetworkLayer). Mỗi khi có lượt đặt giá mới từ BẤT KỲ người dùng nào
 * trong phòng, TV3 sẽ gọi {@link #onBidUpdated(BidTransaction)}, và controller này sẽ tự động cập
 * nhật biểu đồ và nhãn giá — không cần người dùng nhấn refresh.
 *
 * <p><b>Nguyên tắc thread safety bắt buộc:</b> {@link #onBidUpdated} được gọi từ background network
 * thread của TV3. Mọi thao tác cập nhật UI đều phải được bọc trong {@link Platform#runLater} để
 * chuyển về JavaFX Application Thread — vi phạm điều này sẽ gây IllegalStateException.
 */
public class BiddingRoomController implements AuctionObserver {

  // -- @FXML inject từ bidding-room.fxml --

  // Biểu đồ đường giá theo thời gian thực (mục 3.2.5)
  @FXML private LineChart<Number, Number> bidChart;

  @FXML private NumberAxis xAxis; // Trục X: thứ tự lượt bid (tự động cập nhật)

  @FXML private NumberAxis yAxis; // Trục Y: giá tiền (VNĐ)

  // Thông tin phiên đấu giá
  @FXML private Label auctionTitleLabel;

  @FXML private Label currentPriceLabel;

  @FXML private Label timeRemainingLabel;

  @FXML private Label statusLabel;

  @FXML private Label infoLabel; // Thông báo kết quả bid (thành công/thất bại)

  // Khu vực đặt giá
  @FXML private TextField bidAmountField;

  @FXML private Button placeBidButton;

  // Lịch sử bid gần đây dạng danh sách
  @FXML private ListView<String> bidHistoryList;

  @FXML private Button backButton;

  // -- Dữ liệu & Dependency --

  private final ServerService serverService = new ServerService();
  private final AuctionSessionState session = AuctionSessionState.getInstance();

  // Series dữ liệu của biểu đồ — thêm điểm vào đây thì chart tự cập nhật
  private final XYChart.Series<Number, Number> bidSeries = new XYChart.Series<>();

  // Danh sách text lịch sử bid hiển thị trong ListView
  private final ObservableList<String> bidHistoryItems = FXCollections.observableArrayList();

  // Đếm số lượt bid để làm trục X (thứ tự lần đặt giá: 1, 2, 3, ...)
  private int bidCount = 0;

  private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

  /**
   * Được JavaFX gọi tự động sau khi load FXML. Khởi tạo biểu đồ, đăng ký Observer, và load lịch sử
   * bid ban đầu.
   */
  @FXML
  public void initialize() {
    Auction auction = session.getSelectedAuction();

    if (auction == null) {
      infoLabel.setText("Lỗi: Không tìm được thông tin phiên đấu giá.");
      return;
    }

    // Thiết lập thông tin tiêu đề phiên
    auctionTitleLabel.setText("Phiên Đấu Giá #" + auction.getId());
    currentPriceLabel.setText(String.format("%,d VNĐ", auction.getCurrentPrice()));
    statusLabel.setText(auction.getStatus().name());

    // Thiết lập biểu đồ đường
    setupBidChart();

    // Load lịch sử bid đã có từ trước (khi mới vào phòng, client lấy toàn bộ history từ server)
    loadBidHistory(auction.getId());

    // Đăng ký lớp này làm Observer — TV3 sẽ notify mỗi khi có bid mới
    serverService.addObserver(this);

    // Gắn dữ liệu ListView
    bidHistoryList.setItems(bidHistoryItems);

    // Disable nút đặt giá nếu phiên không ở trạng thái RUNNING
    if (!auction.isRunning()) {
      placeBidButton.setDisable(true);
      bidAmountField.setDisable(true);
      infoLabel.setText("Phiên đấu giá này không còn nhận giá mới.");
    }
  }

  // ===== MỤC 3.2.5: BID HISTORY VISUALIZATION =====

  /**
   * Cấu hình chi tiết biểu đồ đường (LineChart). Thiết lập tên trục, nhãn series, và style đường
   * biểu đồ.
   */
  private void setupBidChart() {
    bidSeries.setName("Đường giá đấu");

    // Thiết lập label cho 2 trục
    xAxis.setLabel("Lượt đặt giá (thứ tự)");
    yAxis.setLabel("Giá (VNĐ)");

    // Không tự động giới hạn khoảng trục Y — để chart tự mở rộng theo dữ liệu
    yAxis.setAutoRanging(true);
    xAxis.setAutoRanging(true);

    // Tắt animation mỗi khi thêm điểm mới (animation chậm sẽ gây lag khi cập nhật liên tục)
    bidChart.setAnimated(false);

    // Tắt hiển thị symbols (chấm tròn) trên mỗi điểm để chart gọn hơn khi có nhiều điểm
    bidChart.setCreateSymbols(true);

    bidChart.setTitle("Biểu Đồ Giá Đấu Theo Thời Gian");

    // Thêm series vào chart — chart sẽ vẽ đường nối qua tất cả điểm trong series này
    bidChart.getData().add(bidSeries);
  }

  /**
   * Load toàn bộ lịch sử bid từ server khi mới vào phòng để vẽ đường cơ sở trên biểu đồ. Cần gọi
   * một lần duy nhất trong initialize().
   *
   * @param auctionId ID phiên đấu giá cần lấy lịch sử
   */
  private void loadBidHistory(Long auctionId) {
    List<BidTransaction> history = serverService.getBidHistory(auctionId);

    for (BidTransaction bid : history) {
      addBidToChart(bid);
      addBidToHistoryList(bid);
    }
  }

  /**
   * Thêm một điểm dữ liệu mới vào biểu đồ. Mỗi điểm = (thứ tự lượt bid, số tiền). Phương thức
   * này phải được gọi trên JavaFX Application Thread.
   *
   * @param bid giao dịch đặt giá cần vẽ
   */
  private void addBidToChart(BidTransaction bid) {
    bidCount++;
    // Tạo điểm mới (x = thứ tự bid, y = số tiền) và thêm vào series
    XYChart.Data<Number, Number> dataPoint = new XYChart.Data<>(bidCount, bid.getAmount());
    bidSeries.getData().add(dataPoint);
  }

  /**
   * Thêm một dòng mô tả text vào ListView lịch sử bid. Phương thức này phải được gọi trên JavaFX
   * Application Thread.
   *
   * @param bid giao dịch đặt giá cần hiển thị
   */
  private void addBidToHistoryList(BidTransaction bid) {
    String timeStr = bid.getTimestamp() != null ? bid.getTimestamp().format(TIME_FORMAT) : "--:--";
    String entry =
        String.format(
            "[%s] Bidder #%d đặt giá: %,d VNĐ", timeStr, bid.getBidderId(), bid.getAmount());

    // Thêm vào đầu danh sách để lần đặt giá mới nhất luôn hiển thị ở trên cùng
    bidHistoryItems.add(0, entry);
  }

  // ===== OBSERVER PATTERN — NHẬN CẬP NHẬT REALTIME TỪ TV3 =====

  /**
   * <b>Điểm then chốt của toàn bộ hệ thống realtime.</b>
   *
   * <p>TV3 gọi phương thức này từ background network thread mỗi khi nhận được thông báo bid mới từ
   * Server. Bắt buộc phải dùng Platform.runLater() để chuyển mọi thao tác UI về JavaFX Application
   * Thread — đây là quy tắc căn bản của lập trình đa luồng với JavaFX.
   *
   * @param bid giao dịch đặt giá vừa được server xác nhận
   */
  @Override
  public void onBidUpdated(BidTransaction bid) {
    // Lập trình phòng thủ: bỏ qua nếu bid không thuộc phiên đang xem
    Auction currentAuction = session.getSelectedAuction();
    if (currentAuction == null || !currentAuction.getId().equals(bid.getAuctionId())) {
      return;
    }

    // QUAN TRỌNG: chuyển về JavaFX Application Thread trước khi chạm vào bất kỳ UI component nào
    Platform.runLater(
        () -> {
          // Cập nhật biểu đồ — thêm điểm mới trên đường giá
          addBidToChart(bid);

          // Cập nhật nhãn giá hiện tại
          currentPriceLabel.setText(String.format("%,d VNĐ", bid.getAmount()));

          // Thêm vào danh sách lịch sử
          addBidToHistoryList(bid);

          // Cập nhật giá hiện tại trong session model
          currentAuction.setCurrentPrice(bid.getAmount());
          currentAuction.setCurrentWinnerId(bid.getBidderId());
        });
  }

  /**
   * Được gọi khi trạng thái phiên đấu giá thay đổi (ví dụ: RUNNING → FINISHED). Cập nhật UI để
   * phản ánh trạng thái mới — vô hiệu hóa nút đấu giá nếu phiên đã kết thúc.
   *
   * @param auctionId ID của phiên bị thay đổi trạng thái
   * @param newStatus chuỗi tên trạng thái mới
   */
  @Override
  public void onAuctionStatusChanged(Long auctionId, String newStatus) {
    Auction currentAuction = session.getSelectedAuction();
    if (currentAuction == null || !currentAuction.getId().equals(auctionId)) {
      return;
    }

    Platform.runLater(
        () -> {
          statusLabel.setText(newStatus);

          // Nếu phiên kết thúc, vô hiệu hóa các control đặt giá
          if ("FINISHED".equals(newStatus)
              || "CANCELED".equals(newStatus)
              || "PAID".equals(newStatus)) {
            placeBidButton.setDisable(true);
            bidAmountField.setDisable(true);
            infoLabel.setText("Phiên đấu giá đã kết thúc. Không thể đặt thêm giá.");
          }
        });
  }

  // ===== XỬ LÝ SỰ KIỆN NGƯỜI DÙNG =====

  /**
   * Xử lý khi người dùng nhấn nút "Đặt Giá". Validate số tiền nhập vào, gửi lên server, và chờ
   * phản hồi Observer (không tự cập nhật UI ở đây — sẽ cập nhật qua onBidUpdated khi server xác
   * nhận).
   *
   * @param event ActionEvent từ placeBidButton
   */
  @FXML
  private void handlePlaceBid(ActionEvent event) {
    String amountText = bidAmountField.getText().trim();

    // Validate: trường không được rỗng
    if (amountText.isEmpty()) {
      infoLabel.setText("Vui lòng nhập số tiền muốn đặt giá.");
      return;
    }

    long amount;
    try {
      // Validate: phải là số hợp lệ
      amount = Long.parseLong(amountText.replace(",", ""));
    } catch (NumberFormatException e) {
      infoLabel.setText("Số tiền không hợp lệ. Chỉ nhập số.");
      return;
    }

    // Validate: phải dương và lớn hơn giá hiện tại
    Auction auction = session.getSelectedAuction();
    if (amount <= 0) {
      infoLabel.setText("Số tiền phải lớn hơn 0.");
      return;
    }
    if (amount <= auction.getCurrentPrice()) {
      infoLabel.setText(
          String.format(
              "Giá đặt phải cao hơn giá hiện tại (%,d VNĐ).", auction.getCurrentPrice()));
      return;
    }

    Long bidderId = session.getCurrentUser().getId();
    boolean accepted = serverService.placeBid(auction.getId(), bidderId, amount);

    if (accepted) {
      // Server chấp nhận: UI sẽ tự cập nhật qua onBidUpdated() — không cần làm gì thêm ở đây
      infoLabel.setText("Đặt giá thành công! Chờ cập nhật...");
      bidAmountField.clear();
    } else {
      infoLabel.setText("Đặt giá thất bại. Giá có thể đã bị vượt qua hoặc phiên đã đóng.");
    }
  }

  /**
   * Quay lại trang chi tiết sản phẩm. Hủy đăng ký Observer trước khi rời màn hình để tránh memory
   * leak và nhận thông báo thừa.
   *
   * @param event ActionEvent từ backButton
   */
  @FXML
  private void handleBack(ActionEvent event) {
    // Quan trọng: hủy đăng ký Observer khi rời màn hình để tránh memory leak
    serverService.removeObserver(this);

    try {
      Stage stage = (Stage) backButton.getScene().getWindow();
      FxmlLoader.navigateTo(
          stage, "auction-detail.fxml", "Online Auction System — Chi Tiết Sản Phẩm");
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
