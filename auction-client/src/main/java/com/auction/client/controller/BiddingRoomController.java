package com.auction.client.controller;

import com.auction.client.model.AuctionSessionState;
import com.auction.client.observer.AuctionObserver;
import com.auction.client.service.ServerService;
import com.auction.client.util.FxmlLoader;
import com.auction.client.util.NotificationUtils;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Cursor;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.stage.Stage;
import server.model.entity.Auction;
import server.model.entity.BidTransaction;

/**
 * Controller cho màn hình Phòng Đấu Giá Trực Tiếp (bidding-room.fxml).
 *
 * <p><b>Đây là Controller phức tạp và quan trọng nhất của Thành viên 4.</b>
 *
 * <p><b>Vai trò trong mô hình Observer:</b> Lớp này implement {@link AuctionObserver} để nhận thông
 * báo realtime từ TV3 (NetworkLayer). Mỗi khi có lượt đặt giá mới từ BẤT KỲ người dùng nào trong
 * phòng, TV3 sẽ gọi {@link #onBidUpdated(BidTransaction)}, và controller này sẽ tự động cập nhật
 * biểu đồ và nhãn giá — không cần người dùng nhấn refresh.
 *
 * <p><b>Nguyên tắc thread safety bắt buộc:</b> {@link #onBidUpdated} được gọi từ background network
 * thread của TV3. Mọi thao tác cập nhật UI đều phải được bọc trong {@link Platform#runLater} để
 * chuyển về JavaFX Application Thread — vi phạm điều này sẽ gây IllegalStateException.
 */
public class BiddingRoomController implements AuctionObserver {

  // -- @FXML inject từ bidding-room.fxml --

  // Biểu đồ đường giá theo thời gian thực (mục 3.2.5)
  @FXML private LineChart<String, Number> bidChart;

  @FXML private CategoryAxis xAxis; // Trục X: nhãn thời gian đặt giá (cách đều)

  @FXML private NumberAxis yAxis; // Trục Y: giá tiền (VNĐ)

  // Thông tin phiên đấu giá
  @FXML private Label auctionTitleLabel;

  @FXML private Label currentPriceLabel;

  @FXML private Label timeRemainingLabel;

  @FXML private Label statusLabel;
  @FXML private Label minBidStepLabel; // Bước giá tối thiểu

  @FXML private Label infoLabel; // Thông báo kết quả bid (thành công/thất bại)

  // Khu vực đặt giá
  @FXML private TextField bidAmountField;

  @FXML private Button placeBidButton;

  // AutoBid
  @FXML private Label autoBidStatusLabel;
  @FXML private TextField maxBidField;
  @FXML private TextField incrementField;
  @FXML private Button autoBidButton;
  @FXML private Button cancelAutoBidButton;

  // Lịch sử bid gần đây dạng danh sách
  @FXML private ListView<String> bidHistoryList;

  @FXML private Button backButton;

  // -- Dữ liệu & Dependency --

  private final ServerService serverService = new ServerService();
  private final AuctionSessionState session = AuctionSessionState.getInstance();

  // Series dữ liệu của biểu đồ — thêm điểm vào đây thì chart tự cập nhật
  private final XYChart.Series<String, Number> bidSeries = new XYChart.Series<>();

  // Danh sách text lịch sử bid hiển thị trong ListView
  private final ObservableList<String> bidHistoryItems = FXCollections.observableArrayList();

  // Đếm số lượt bid để làm trục X (thứ tự lần đặt giá: 1, 2, 3, ...)
  private int bidCount = 0;

  private Timer countdownTimer;
  private volatile long pendingBidAmount = -1;

  private static final DateTimeFormatter LIST_TIME_FORMAT =
      DateTimeFormatter.ofPattern("dd/MM HH:mm:ss");
  private static final DateTimeFormatter CHART_TIME_FORMAT =
      DateTimeFormatter.ofPattern("dd/MM/yyyy");

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

    // Thêm giá khởi điểm vào Chart
    if (bidSeries.getData().isEmpty()) {
      bidCount++;
      String timeLabel;
      String fullTimeStr;
      if (auction.getStartTime() != null) {
        timeLabel = auction.getStartTime().format(CHART_TIME_FORMAT);
        fullTimeStr = auction.getStartTime().format(LIST_TIME_FORMAT);
      } else {
        timeLabel = LocalDateTime.now().format(CHART_TIME_FORMAT);
        fullTimeStr = LocalDateTime.now().format(LIST_TIME_FORMAT);
      }
      // Thêm số thứ tự để đảm bảo nhãn duy nhất trên CategoryAxis
      String label = "#" + bidCount + " " + timeLabel;
      XYChart.Data<String, Number> dataPoint =
          new XYChart.Data<>(label, auction.getStartingPrice());

      dataPoint
          .nodeProperty()
          .addListener(
              (obs, oldNode, newNode) -> {
                if (newNode != null) {
                  String tooltipText =
                      String.format(
                          "Thời gian: %s\nGiá khởi điểm: %,d VNĐ",
                          fullTimeStr, auction.getStartingPrice());
                  Tooltip tooltip = new Tooltip(tooltipText);
                  tooltip.setShowDelay(javafx.util.Duration.ZERO);
                  Tooltip.install(newNode, tooltip);
                  newNode.setCursor(Cursor.HAND);
                }
              });

      bidSeries.getData().add(dataPoint);
    }

    currentPriceLabel.setText(String.format("%,d VNĐ", auction.getCurrentPrice()));
    statusLabel.setText(auction.getStatus().name());
    minBidStepLabel.setText(String.format("Bước giá tối thiểu: %,d VNĐ", auction.getMinBidStep()));

    // Thiết lập biểu đồ đường
    setupBidChart();

    // Load lịch sử bid đã có từ trước (khi mới vào phòng, client lấy toàn bộ history từ server)
    loadBidHistory(auction.getId());

    // Đăng ký lớp này làm Observer — TV3 sẽ notify mỗi khi có bid mới
    serverService.addObserver(this);

    // Gắn dữ liệu ListView
    bidHistoryList.setItems(bidHistoryItems);

    // Disable nút đặt giá nếu phiên không RUNNING — phân biệt OPEN (chưa mở) với đã đóng
    if (!auction.isRunning()) {
      placeBidButton.setDisable(true);
      bidAmountField.setDisable(true);
      autoBidButton.setDisable(true);
      maxBidField.setDisable(true);
      incrementField.setDisable(true);
      infoLabel.setText(
          switch (auction.getStatus()) {
            case OPEN -> "Phiên chưa bắt đầu — chưa thể đặt giá.";
            case FINISHED -> "Phiên đã kết thúc — không còn nhận giá mới.";
            case PAID -> "Phiên đã thanh toán.";
            case CANCELED -> "Phiên đã hủy.";
            default -> "Phiên đấu giá này không còn nhận giá mới.";
          });
    }

    // Bắt đầu đếm ngược thời gian còn lại
    startCountdownTimer(auction);

    // Lấy trạng thái Auto-Bid hiện tại từ server
    Long bidderId = session.getCurrentUser().getId();
    new Thread(
            () -> {
              boolean isActive = serverService.checkAutoBidStatus(auction.getId(), bidderId);
              Platform.runLater(() -> setAutoBidActive(isActive));
            })
        .start();
  }

  /** Khởi động timer đếm ngược đến endTime của phiên. */
  private void startCountdownTimer(Auction auction) {
    if (auction.getEndTime() == null) return;
    countdownTimer = new Timer(true);
    countdownTimer.scheduleAtFixedRate(
        new TimerTask() {
          @Override
          public void run() {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime end = auction.getEndTime();
            if (now.isAfter(end)) {
              Platform.runLater(() -> timeRemainingLabel.setText("Đã kết thúc"));
              countdownTimer.cancel();
              return;
            }
            Duration remaining = Duration.between(now, end);
            long hours = remaining.toHours();
            long minutes = remaining.toMinutesPart();
            long seconds = remaining.toSecondsPart();
            String text =
                hours > 0
                    ? String.format("%02d:%02d:%02d", hours, minutes, seconds)
                    : String.format("%02d:%02d", minutes, seconds);
            Platform.runLater(() -> timeRemainingLabel.setText(text));
          }
        },
        0,
        1000);
  }

  // ===== MỤC 3.2.5: BID HISTORY VISUALIZATION =====

  /**
   * Cấu hình chi tiết biểu đồ đường (LineChart). Thiết lập tên trục, nhãn series, và style đường
   * biểu đồ.
   */
  private void setupBidChart() {
    bidSeries.setName("Đường giá đấu");

    // Thiết lập label cho 2 trục
    xAxis.setLabel("Thời gian");
    yAxis.setLabel("Giá (VNĐ)");

    // Không tự động giới hạn khoảng trục Y — để chart tự mở rộng theo dữ liệu
    yAxis.setAutoRanging(true);
    yAxis.setForceZeroInRange(false); // Giúp giá bám sát hơn

    // CategoryAxis tự cách đều các điểm — không cần setForceZeroInRange hay TickLabelFormatter
    // Xoay nhẹ nhãn trục X để tránh chồng chéo khi có nhiều bid
    xAxis.setTickLabelRotation(-45);
    xAxis.setTickLabelGap(5);

    // Tắt animation mỗi khi thêm điểm mới (animation chậm sẽ gây lag khi cập nhật liên tục)
    bidChart.setAnimated(false);
    xAxis.setAnimated(false);

    // Hiển thị symbols (chấm tròn) trên mỗi điểm dữ liệu
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
   * Thêm một điểm dữ liệu mới vào biểu đồ. Mỗi điểm = (thứ tự lượt bid, số tiền). Phương thức này
   * phải được gọi trên JavaFX Application Thread.
   *
   * @param bid giao dịch đặt giá cần vẽ
   */
  private void addBidToChart(BidTransaction bid) {
    bidCount++;
    String timeLabel;
    String fullTimeStr;
    if (bid.getTimestamp() != null) {
      timeLabel = bid.getTimestamp().format(CHART_TIME_FORMAT);
      fullTimeStr = bid.getTimestamp().format(LIST_TIME_FORMAT);
    } else {
      timeLabel = LocalDateTime.now().format(CHART_TIME_FORMAT);
      fullTimeStr = LocalDateTime.now().format(LIST_TIME_FORMAT);
    }

    // Thêm số thứ tự (#N) để mỗi nhãn luôn duy nhất — tránh Auto-Bid bị gộp vào 1 điểm
    String label = "#" + bidCount + " " + timeLabel;
    XYChart.Data<String, Number> dataPoint = new XYChart.Data<>(label, bid.getAmount());

    dataPoint
        .nodeProperty()
        .addListener(
            (obs, oldNode, newNode) -> {
              if (newNode != null) {
                String tooltipText =
                    String.format(
                        "Thời gian: %s\nNgười đặt: Bidder #%d\nGiá đặt: %,d VNĐ",
                        fullTimeStr, bid.getBidderId(), bid.getAmount());
                Tooltip tooltip = new Tooltip(tooltipText);
                tooltip.setShowDelay(javafx.util.Duration.ZERO);
                Tooltip.install(newNode, tooltip);
                newNode.setCursor(Cursor.HAND);
              }
            });

    bidSeries.getData().add(dataPoint);
  }

  /**
   * Thêm một dòng mô tả text vào ListView lịch sử bid. Phương thức này phải được gọi trên JavaFX
   * Application Thread.
   *
   * @param bid giao dịch đặt giá cần hiển thị
   */
  private void addBidToHistoryList(BidTransaction bid) {
    String timeStr =
        bid.getTimestamp() != null ? bid.getTimestamp().format(LIST_TIME_FORMAT) : "--:--";
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

          // Nếu bid này là của chính mình, hiện thông báo thành công và MỞ KHÓA nút ngay
          if (pendingBidAmount > 0 && bid.getAmount() == pendingBidAmount) {
            infoLabel.setText("");
            NotificationUtils.showSuccess(
                (Stage) bidChart.getScene().getWindow(),
                "Bạn đã dẫn đầu với giá " + String.format("%,d", bid.getAmount()) + " VNĐ");
            pendingBidAmount = -1;
            placeBidButton.setDisable(false); // Mở khóa nút để đặt giá tiếp
          } else {
            // [Tính năng 4] Thông báo khi có người khác đặt giá
            NotificationUtils.showToast(
                (Stage) bidChart.getScene().getWindow(),
                "📣 Có người vừa đặt giá mới: " + String.format("%,d", bid.getAmount()) + " VNĐ",
                false);
          }
        });
  }

  /**
   * Được gọi khi trạng thái phiên đấu giá thay đổi (ví dụ: RUNNING → FINISHED). Cập nhật UI để phản
   * ánh trạng thái mới — vô hiệu hóa nút đấu giá nếu phiên đã kết thúc.
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
          // [Tính năng 4] Hiện thông báo trạng thái
          NotificationUtils.showToast(
              (Stage) bidChart.getScene().getWindow(),
              "🔔 Phiên #" + auctionId + ": " + newStatus,
              newStatus.equals("CANCELED"));

          // Nếu phiên kết thúc, vô hiệu hóa các control đặt giá
          if ("FINISHED".equals(newStatus)
              || "CANCELED".equals(newStatus)
              || "PAID".equals(newStatus)) {
            placeBidButton.setDisable(true);
            bidAmountField.setDisable(true);
            autoBidButton.setDisable(true);
            maxBidField.setDisable(true);
            incrementField.setDisable(true);
            infoLabel.setText("Phiên đấu giá đã kết thúc. Không thể đặt thêm giá.");
          }
        });
  }

  @Override
  public void onAuctionTimeExtended(Long auctionId, String newEndTimeStr) {
    Auction currentAuction = session.getSelectedAuction();
    if (currentAuction == null || !currentAuction.getId().equals(auctionId)) {
      return;
    }

    Platform.runLater(
        () -> {
          try {
            LocalDateTime newEndTime = LocalDateTime.parse(newEndTimeStr);
            currentAuction.setEndTime(newEndTime);
            // Cập nhật lại UI thông báo thời gian đã được gia hạn
            NotificationUtils.showToast(
                (Stage) bidChart.getScene().getWindow(),
                "⏰ Phiên đấu giá được gia hạn do có người đặt giá ở phút cuối!",
                false);
          } catch (Exception e) {
            e.printStackTrace();
          }
        });
  }

  @Override
  public void onWalletEvent(String eventType, org.json.simple.JSONObject payload) {
    Auction currentAuction = session.getSelectedAuction();
    if (currentAuction == null) return;

    Long auctionId;
    if (payload.get("auctionId") instanceof Number) {
      auctionId = ((Number) payload.get("auctionId")).longValue();
    } else {
      return;
    }

    if (!currentAuction.getId().equals(auctionId)) return;

    Platform.runLater(
        () -> {
          if ("AUTO_BID_CANCELLED".equals(eventType)) {
            infoLabel.setText("");
            NotificationUtils.showToast(
                (Stage) bidChart.getScene().getWindow(), "Đã tắt chế độ Auto-Bid", false);
            setAutoBidActive(false);
          } else if ("AUTO_BID_ACTIVATED".equals(eventType)) {
            setAutoBidActive(true);
          } else if ("AUTO_BID_EXHAUSTED".equals(eventType)) {
            infoLabel.setText("");
            NotificationUtils.showError(
                (Stage) bidChart.getScene().getWindow(), "Auto-Bid đã dừng do giá vượt mức tối đa!");
            setAutoBidActive(false);
          }
        });
  }

  // ===== XỬ LÝ SỰ KIỆN NGƯỜI DÙNG =====

  /**
   * Xử lý khi người dùng nhấn nút "Đặt Giá". Validate số tiền nhập vào, gửi lên server, và chờ phản
   * hồi Observer (không tự cập nhật UI ở đây — sẽ cập nhật qua onBidUpdated khi server xác nhận).
   *
   * @param event ActionEvent từ placeBidButton
   */
  @FXML
  private void handlePlaceBid(ActionEvent event) {
    String amountText = bidAmountField.getText().trim();

    // Validate: trường không được rỗng
    if (amountText.isEmpty()) {
      infoLabel.setText("");
      NotificationUtils.showError(
          (Stage) bidChart.getScene().getWindow(), "Vui lòng nhập số tiền muốn đặt giá.");
      return;
    }

    long amount;
    try {
      // Validate: phải là số hợp lệ
      amount = Long.parseLong(amountText.replace(",", ""));
    } catch (NumberFormatException e) {
      infoLabel.setText("");
      NotificationUtils.showError(
          (Stage) bidChart.getScene().getWindow(), "Số tiền không hợp lệ. Chỉ nhập số.");
      return;
    }

    // Validate: kiểm tra bước giá tối thiểu (áp dụng cho mọi lượt bid)
    Auction auction = session.getSelectedAuction();
    long minRequired = auction.getCurrentPrice() + auction.getMinBidStep();

    if (amount < minRequired) {
      infoLabel.setText("");
      NotificationUtils.showError(
          (Stage) bidChart.getScene().getWindow(),
          String.format(
              "Giá đặt chưa đạt mức tối thiểu. Bạn cần đặt ít nhất %,d VNĐ.", minRequired));
      return;
    }

    Long bidderId = session.getCurrentUser().getId();
    final long finalAmount = amount;

    // [Thêm bước xác nhận] Hỏi bạn trước khi đặt giá (tránh ấn nhầm)
    javafx.scene.control.Alert confirmAlert =
        new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.CONFIRMATION);
    NotificationUtils.styleAlert(confirmAlert);
    confirmAlert.setGraphic(null); // Bỏ dấu "?" phèn phèn
    confirmAlert.setTitle("Xác Nhận Đặt Giá");
    confirmAlert.setHeaderText("Bạn có chắc chắn muốn đặt giá cho phiên này?");
    confirmAlert.setContentText(
        "Số tiền đặt: "
            + String.format("%,d", finalAmount)
            + " VNĐ\nLưu ý: Hành động này không thể hoàn tác!");

    java.util.Optional<javafx.scene.control.ButtonType> result = confirmAlert.showAndWait();
    if (result.isEmpty() || result.get() != javafx.scene.control.ButtonType.OK) {
      infoLabel.setText("Đã hủy lệnh đặt giá.");
      return;
    }

    // Lưu lại để nhận diện khi BID_UPDATE về
    pendingBidAmount = finalAmount;
    infoLabel.setText("Đang gửi giá " + String.format("%,d", finalAmount) + " VNĐ...");
    placeBidButton.setDisable(true);
    bidAmountField.clear();

    // Gửi đi, kết quả phản hồi ngay lập tức qua sendRequest
    String errorMsg = serverService.placeBid(auction.getId(), bidderId, finalAmount);
    if (errorMsg != null) {
      // Có lỗi từ server (ví dụ: số dư không đủ)
      infoLabel.setText("");
      NotificationUtils.showError(
          (Stage) bidChart.getScene().getWindow(), "Đặt giá thất bại: " + errorMsg);
      placeBidButton.setDisable(false);
      pendingBidAmount = -1;
      return;
    }

    // Nếu errorMsg == null (thành công), UI sẽ được cập nhật tự động và mở khóa nút
    // qua cơ chế Push (onBidUpdated) từ server.
  }

  @FXML
  private void handleAutoBid(ActionEvent event) {
    String maxBidText = maxBidField.getText().trim();
    String incText = incrementField.getText().trim();

    if (maxBidText.isEmpty() || incText.isEmpty()) {
      infoLabel.setText("");
      NotificationUtils.showError(
          (Stage) bidChart.getScene().getWindow(), "Vui lòng nhập giá tối đa và bước giá.");
      return;
    }

    long maxBid, inc;
    try {
      maxBid = Long.parseLong(maxBidText.replace(",", ""));
      inc = Long.parseLong(incText.replace(",", ""));
    } catch (NumberFormatException e) {
      infoLabel.setText("");
      NotificationUtils.showError((Stage) bidChart.getScene().getWindow(), "Số tiền không hợp lệ.");
      return;
    }

    Auction auction = session.getSelectedAuction();
    if (maxBid <= auction.getCurrentPrice()) {
      infoLabel.setText("");
      NotificationUtils.showError(
          (Stage) bidChart.getScene().getWindow(), "Giá tối đa phải lớn hơn giá hiện tại.");
      return;
    }
    if (inc < auction.getMinBidStep()) {
      infoLabel.setText("");
      NotificationUtils.showError(
          (Stage) bidChart.getScene().getWindow(),
          "Bước giá Auto-Bid phải lớn hơn hoặc bằng bước giá tối thiểu ("
              + String.format("%,d", auction.getMinBidStep())
              + " VNĐ).");
      return;
    }

    Long bidderId = session.getCurrentUser().getId();

    javafx.scene.control.Alert confirmAlert =
        new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.CONFIRMATION);
    NotificationUtils.styleAlert(confirmAlert);
    confirmAlert.setGraphic(null);
    confirmAlert.setTitle("Xác Nhận Đăng Ký Auto-Bid");
    confirmAlert.setHeaderText("Bạn có chắc chắn muốn bật Auto-Bid?");
    confirmAlert.setContentText(
        "Giá tối đa: "
            + String.format("%,d", maxBid)
            + " VNĐ\nBước giá: "
            + String.format("%,d", inc)
            + " VNĐ");

    java.util.Optional<javafx.scene.control.ButtonType> result = confirmAlert.showAndWait();
    if (result.isEmpty() || result.get() != javafx.scene.control.ButtonType.OK) {
      return;
    }

    infoLabel.setText("Đang đăng ký Auto-Bid...");
    autoBidButton.setDisable(true);

    boolean sent = serverService.registerAutoBid(auction.getId(), bidderId, maxBid, inc);
    if (!sent) {
      infoLabel.setText("");
      NotificationUtils.showError(
          (Stage) bidChart.getScene().getWindow(), "Lỗi kết nối, không thể đăng ký Auto-Bid.");
      autoBidButton.setDisable(false);
      return;
    }

    infoLabel.setText("");
    NotificationUtils.showSuccess(
        (Stage) bidChart.getScene().getWindow(), "Auto-Bid đã được kích hoạt.");
    maxBidField.clear();
    incrementField.clear();
  }

  @FXML
  private void handleCancelAutoBid(ActionEvent event) {
    Auction auction = session.getSelectedAuction();
    if (auction == null) return;

    javafx.scene.control.Alert confirmAlert =
        new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.CONFIRMATION);
    NotificationUtils.styleAlert(confirmAlert);
    confirmAlert.setGraphic(null);
    confirmAlert.setTitle("Xác Nhận Tắt Auto-Bid");
    confirmAlert.setHeaderText("Bạn có chắc chắn muốn tắt Auto-Bid?");
    confirmAlert.setContentText("Bạn sẽ không tự động đặt giá nữa.");

    java.util.Optional<javafx.scene.control.ButtonType> result = confirmAlert.showAndWait();
    if (result.isEmpty() || result.get() != javafx.scene.control.ButtonType.OK) {
      return;
    }

    Long bidderId = session.getCurrentUser().getId();
    boolean sent = serverService.cancelAutoBid(auction.getId(), bidderId);
    if (!sent) {
      NotificationUtils.showError(
          (Stage) bidChart.getScene().getWindow(), "Lỗi kết nối, không thể hủy Auto-Bid.");
    } else {
      infoLabel.setText("Đang tắt Auto-Bid...");
      cancelAutoBidButton.setDisable(true);
    }
  }

  private void setAutoBidActive(boolean active) {
    if (active) {
      autoBidStatusLabel.setText("Trạng thái: Đang hoạt động");
      autoBidStatusLabel.setStyle("-fx-text-fill: #4CAF50; -fx-font-weight: bold;"); // Green
      autoBidButton.setVisible(false);
      autoBidButton.setManaged(false);
      cancelAutoBidButton.setVisible(true);
      cancelAutoBidButton.setManaged(true);
      cancelAutoBidButton.setDisable(false);
      maxBidField.setDisable(true);
      incrementField.setDisable(true);

      // Khóa nút đặt giá thủ công
      placeBidButton.setDisable(true);
      bidAmountField.setDisable(true);
      infoLabel.setText("Đang dùng Auto-Bid. Vui lòng tắt Auto-Bid để đặt giá thủ công.");
    } else {
      autoBidStatusLabel.setText("Trạng thái: Chưa kích hoạt");
      autoBidStatusLabel.setStyle("-fx-text-fill: #9e9e9e; -fx-font-weight: normal;"); // Gray
      autoBidButton.setVisible(true);
      autoBidButton.setManaged(true);
      cancelAutoBidButton.setVisible(false);
      cancelAutoBidButton.setManaged(false);
      maxBidField.setDisable(false);
      incrementField.setDisable(false);

      // Mở lại nút đặt giá thủ công nếu phiên đấu giá đang chạy
      Auction auction = session.getSelectedAuction();
      if (auction != null && auction.isRunning()) {
        placeBidButton.setDisable(false);
        bidAmountField.setDisable(false);
        infoLabel.setText("");
      }
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
    if (countdownTimer != null) countdownTimer.cancel();

    try {
      Stage stage = (Stage) backButton.getScene().getWindow();
      FxmlLoader.navigateTo(
          stage, "auction-detail.fxml", "Online Auction System — Chi Tiết Sản Phẩm");
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
