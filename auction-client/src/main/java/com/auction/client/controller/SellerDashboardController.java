package com.auction.client.controller;

import com.auction.client.model.AuctionSessionState;
import com.auction.client.service.ServerService;
import com.auction.client.util.ComboBoxPopupWidthSync;
import com.auction.client.util.FxmlLoader;
import com.auction.client.util.NotificationUtils;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javax.imageio.ImageIO;
import server.model.entity.Auction;
import server.model.enums.AuctionStatus;

public class SellerDashboardController implements com.auction.client.observer.AuctionObserver {

  @FXML private Label sellerNameLabel;
  @FXML private Button walletButton;
  @FXML private Button logoutButton;
  @FXML private Button refreshButton;

  @FXML private FlowPane myAuctionsGrid;

  // Modal Form
  @FXML private StackPane modalOverlay;
  @FXML private Label modalTitleLabel;
  @FXML private ComboBox<String> categoryCombo;
  @FXML private TextField itemNameField;
  @FXML private TextField startingPriceField;
  @FXML private TextField minBidStepField;
  @FXML private DatePicker startDatePicker;
  @FXML private DatePicker endDatePicker;
  @FXML private ComboBox<Integer> startHourCombo;
  @FXML private ComboBox<Integer> startMinuteCombo;
  @FXML private ComboBox<Integer> endHourCombo;
  @FXML private ComboBox<Integer> endMinuteCombo;
  @FXML private TextArea itemDescriptionArea;
  @FXML private TextArea itemSpecificsArea;
  @FXML private ImageView itemImageView;
  @FXML private Label formResultLabel;
  @FXML private Button submitFormButton;

  private String currentImageBase64 = null;
  private Long editingAuctionId = null;

  private final ServerService serverService = new ServerService();
  private final AuctionSessionState session = AuctionSessionState.getInstance();

  private final List<Runnable> timeUpdaters = new ArrayList<>();
  private javafx.animation.Timeline countdownTimeline;

  private static final String[] CATEGORY_DISPLAY_NAMES = {"Điện tử", "Nghệ thuật", "Xe cộ", "Khác"};
  private static final String[] CATEGORY_ENUM_VALUES = {
    "ELECTRONICS", "ARTWORK", "VEHICLE", "OTHER"
  };
  private static final DateTimeFormatter DISPLAY_FORMAT =
      DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

  @FXML
  public void initialize() {
    if (session.isLoggedIn()) {
      sellerNameLabel.setText("Xin chào, " + session.getCurrentUser().getUsername());
    }

    categoryCombo.setItems(FXCollections.observableArrayList(CATEGORY_DISPLAY_NAMES));
    categoryCombo.setValue(CATEGORY_DISPLAY_NAMES[0]);
    ComboBoxPopupWidthSync.install(categoryCombo);

    ObservableList<Integer> hours = FXCollections.observableArrayList();
    for (int i = 0; i < 24; i++) hours.add(i);
    ObservableList<Integer> minutes = FXCollections.observableArrayList();
    for (int i = 0; i < 60; i++) minutes.add(i);

    startHourCombo.setItems(hours);
    startMinuteCombo.setItems(minutes);
    endHourCombo.setItems(hours);
    endMinuteCombo.setItems(minutes);

    startHourCombo.setValue(LocalDateTime.now().getHour());
    startMinuteCombo.setValue(LocalDateTime.now().getMinute());
    endHourCombo.setValue(23);
    endMinuteCombo.setValue(59);

    ComboBoxPopupWidthSync.install(startHourCombo);
    ComboBoxPopupWidthSync.install(startMinuteCombo);
    ComboBoxPopupWidthSync.install(endHourCombo);
    ComboBoxPopupWidthSync.install(endMinuteCombo);

    itemSpecificsArea.setPromptText("Ví dụ: \rThương hiệu: Apple \rTình trạng: Mới 100%");

    serverService.addObserver(this);
    startCountdownTimer();
    loadMyAuctions();
  }

  private void startCountdownTimer() {
    countdownTimeline =
        new javafx.animation.Timeline(
            new javafx.animation.KeyFrame(
                javafx.util.Duration.seconds(1),
                event -> {
                  for (Runnable r : timeUpdaters) {
                    r.run();
                  }
                }));
    countdownTimeline.setCycleCount(javafx.animation.Timeline.INDEFINITE);
    countdownTimeline.play();
  }

  private void loadMyAuctions() {
    Long sellerId = session.getCurrentUser().getId();
    List<Auction> auctions = serverService.getAuctionsBySeller(sellerId);

    auctions.sort(
        (a1, a2) -> {
          int rank1 = getStatusRank(a1.getStatus());
          int rank2 = getStatusRank(a2.getStatus());
          if (rank1 != rank2) return Integer.compare(rank1, rank2);
          if (rank1 == 1) {
            if (a1.getEndTime() == null && a2.getEndTime() == null) return 0;
            if (a1.getEndTime() == null) return 1;
            if (a2.getEndTime() == null) return -1;
            return a1.getEndTime().compareTo(a2.getEndTime());
          }
          return a2.getId().compareTo(a1.getId());
        });

    renderGrid(auctions);
  }

  private int getStatusRank(AuctionStatus status) {
    if (status == null) return 99;
    return switch (status) {
      case RUNNING -> 1;
      case OPEN -> 2;
      case FINISHED, PAID, CANCELED -> 3;
    };
  }

  private void renderGrid(List<Auction> auctions) {
    myAuctionsGrid.getChildren().clear();
    timeUpdaters.clear();

    if (auctions.isEmpty()) {
      Label emptyLabel = new Label("Bạn chưa có phiên đấu giá nào.");
      emptyLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #7f8c8d;");
      myAuctionsGrid.getChildren().add(emptyLabel);
      return;
    }

    for (Auction auction : auctions) {
      VBox card = new VBox(10);
      card.getStyleClass().add("info-card"); // Tái sử dụng class này cho đẹp
      card.setPrefWidth(280);
      card.setPadding(new Insets(15));
      card.setStyle(card.getStyle() + " -fx-cursor: hand;");

      ImageView imgView = new ImageView();
      imgView.setFitWidth(250);
      imgView.setFitHeight(180);
      imgView.setPreserveRatio(true);
      if (auction.getImageBase64() != null && !auction.getImageBase64().isEmpty()) {
        try {
          byte[] imgBytes = Base64.getDecoder().decode(auction.getImageBase64());
          imgView.setImage(new Image(new ByteArrayInputStream(imgBytes)));
        } catch (Exception ignored) {
        }
      }
      HBox imgContainer = new HBox(imgView);
      imgContainer.setAlignment(Pos.CENTER);
      imgContainer.setMinHeight(180);

      Label nameLbl =
          new Label(
              auction.getItemName() != null
                  ? auction.getItemName()
                  : "Sản phẩm #" + auction.getItemId());
      nameLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: #e2e8f0;");
      nameLbl.setWrapText(true);
      nameLbl.setMinHeight(45);
      nameLbl.setMaxHeight(45);
      nameLbl.setAlignment(Pos.TOP_LEFT);

      Label priceLbl = new Label(String.format("%,d VNĐ", auction.getCurrentPrice()));
      priceLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 18px; -fx-text-fill: #00ffff;");

      Label statusLbl =
          new Label(
              translateStatus(auction.getStatus())
                  + " | "
                  + translateCategoryToVi(auction.getItemCategory()));
      statusLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #94a3b8;");

      Label dateRangeLbl =
          new Label(
              "📅 "
                  + (auction.getStartTime() != null
                      ? auction.getStartTime().format(DISPLAY_FORMAT)
                      : "N/A")
                  + " - "
                  + (auction.getEndTime() != null
                      ? auction.getEndTime().format(DISPLAY_FORMAT)
                      : "N/A"));
      dateRangeLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");

      Label timeLbl = new Label();
      timeLbl.setStyle("-fx-font-size: 14px;");
      Runnable updateTime =
          () -> {
            String timeLeft = formatTimeLeft(auction);
            timeLbl.setText("⏳ " + timeLeft);
            if (timeLeft.startsWith("00:")) {
              timeLbl.setStyle(
                  "-fx-font-size: 14px; -fx-text-fill: #e74c3c; -fx-font-weight: bold;");
            } else if (timeLeft.contains("kết thúc")
                || timeLeft.contains("thanh toán")
                || timeLeft.contains("hủy")) {
              timeLbl.setStyle("-fx-font-size: 14px; -fx-text-fill: #94a3b8;");
            } else {
              timeLbl.setStyle(
                  "-fx-font-size: 14px; -fx-text-fill: #2ecc71; -fx-font-weight: bold;");
            }
          };
      updateTime.run();
      timeUpdaters.add(updateTime);

      card.getChildren().addAll(imgContainer, nameLbl, priceLbl, statusLbl, dateRangeLbl, timeLbl);

      // Nút hành động Sửa / Xóa cho Seller
      Button editBtn = new Button("Sửa");
      editBtn.getStyleClass().add("secondary-button");
      editBtn.setDisable(
          auction.getStatus() != AuctionStatus.OPEN
              && auction.getStatus() != AuctionStatus.RUNNING);
      editBtn.setOnAction(e -> showEditModal(auction));

      Button deleteBtn = new Button("Xóa");
      deleteBtn.getStyleClass().add("danger-button");
      deleteBtn.setDisable(auction.getCurrentWinnerId() != null); // Không xóa nếu đã có người đấu
      deleteBtn.setOnAction(e -> handleDelete(auction));

      Region spacer = new Region();
      VBox.setVgrow(spacer, Priority.ALWAYS);
      card.getChildren().add(spacer);

      HBox actionsBox = new HBox(10, editBtn, deleteBtn);
      actionsBox.setAlignment(Pos.CENTER);
      actionsBox.setPadding(new Insets(10, 0, 0, 0));
      card.getChildren().add(actionsBox);

      card.setOnMouseClicked(
          e -> {
            if (isInteractiveControlTarget(e.getTarget())) return;
            if (e.getButton() == javafx.scene.input.MouseButton.PRIMARY) {
              navigateToDetail(auction);
            }
          });

      myAuctionsGrid.getChildren().add(card);
    }
  }

  private boolean isInteractiveControlTarget(Object target) {
    if (!(target instanceof Node)) return false;
    for (Node n = (Node) target; n != null; n = n.getParent()) {
      if (n instanceof Button || n instanceof ComboBox<?>) return true;
    }
    return false;
  }

  private void navigateToDetail(Auction selected) {
    serverService.removeObserver(this);
    session.setSelectedAuction(selected);
    try {
      Stage stage = (Stage) refreshButton.getScene().getWindow();
      FxmlLoader.navigateTo(
          stage, "auction-detail.fxml", "Online Auction System — Chi Tiết Sản Phẩm");
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

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

  private String translateCategoryToVi(String categoryEn) {
    if (categoryEn == null) return "Khác";
    return switch (categoryEn.toUpperCase()) {
      case "ELECTRONICS" -> "Điện tử";
      case "ARTWORK" -> "Nghệ thuật";
      case "VEHICLE" -> "Xe cộ";
      case "OTHER" -> "Khác";
      default -> "Khác";
    };
  }

  private String categoryDisplayToEnum(String displayName) {
    for (int i = 0; i < CATEGORY_DISPLAY_NAMES.length; i++) {
      if (CATEGORY_DISPLAY_NAMES[i].equals(displayName)) return CATEGORY_ENUM_VALUES[i];
    }
    return "OTHER";
  }

  private String formatTimeLeft(Auction a) {
    if (a.getStatus() == AuctionStatus.FINISHED || a.isFinished()) return "Đã kết thúc";
    if (a.getStatus() == AuctionStatus.PAID) return "Đã thanh toán";
    if (a.getStatus() == AuctionStatus.CANCELED) return "Đã hủy";

    LocalDateTime now = LocalDateTime.now();
    LocalDateTime end = a.getEndTime();
    if (!now.isBefore(end)) return "Đã kết thúc";
    if (a.getStatus() == AuctionStatus.OPEN) return "Chưa bắt đầu";

    Duration d = Duration.between(now, end);
    long days = d.toDays();
    long hours = d.toHoursPart();
    long minutes = d.toMinutesPart();
    long seconds = d.toSecondsPart();

    if (days > 0) return String.format("%d ngày %02d:%02d", days, hours, minutes);
    return String.format("%02d:%02d:%02d", hours, minutes, seconds);
  }

  @FXML
  private void goToMarketplace(ActionEvent event) {
    serverService.removeObserver(this);
    try {
      Stage stage = (Stage) refreshButton.getScene().getWindow();
      FxmlLoader.navigateTo(stage, "auction-list.fxml", "Online Auction System — Chợ Đấu Giá");
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @FXML
  private void handleRefresh(ActionEvent event) {
    loadMyAuctions();
  }

  @FXML
  private void showCreateModal(ActionEvent event) {
    editingAuctionId = null;
    modalTitleLabel.setText("🚀 Tạo Phiên Đấu Giá Mới");
    submitFormButton.setText("Lưu Phiên Đấu Giá");
    formResultLabel.setVisible(false);
    clearForm();
    modalOverlay.setVisible(true);
  }

  private void showEditModal(Auction auction) {
    editingAuctionId = auction.getId();
    modalTitleLabel.setText("✏️ Chỉnh Sửa Phiên #" + auction.getId());
    submitFormButton.setText("Cập Nhật Phiên Đấu Giá");
    formResultLabel.setVisible(false);

    boolean isRunning = auction.getStatus() == AuctionStatus.RUNNING;
    itemNameField.setDisable(isRunning);
    categoryCombo.setDisable(isRunning);
    startingPriceField.setDisable(isRunning);
    minBidStepField.setDisable(isRunning);
    startDatePicker.setDisable(isRunning);
    startHourCombo.setDisable(isRunning);
    startMinuteCombo.setDisable(isRunning);
    endDatePicker.setDisable(isRunning);
    endHourCombo.setDisable(isRunning);
    endMinuteCombo.setDisable(isRunning);

    if (isRunning) {
      formResultLabel.setText("Phiên đang diễn ra. Bạn chỉ có thể sửa Mô tả, Thông số và Ảnh.");
      formResultLabel.setVisible(true);
    }

    itemNameField.setText(auction.getItemName());
    startingPriceField.setText(String.valueOf(auction.getCurrentPrice()));
    minBidStepField.setText(String.valueOf(auction.getMinBidStep()));

    String enumCat = auction.getItemCategory() != null ? auction.getItemCategory() : "OTHER";
    int idx = -1;
    for (int i = 0; i < CATEGORY_ENUM_VALUES.length; i++) {
      if (CATEGORY_ENUM_VALUES[i].equals(enumCat)) {
        idx = i;
        break;
      }
    }
    categoryCombo.setValue(idx != -1 ? CATEGORY_DISPLAY_NAMES[idx] : CATEGORY_DISPLAY_NAMES[0]);

    if (auction.getStartTime() != null) {
      startDatePicker.setValue(auction.getStartTime().toLocalDate());
      startHourCombo.setValue(auction.getStartTime().getHour());
      startMinuteCombo.setValue(auction.getStartTime().getMinute());
    }
    if (auction.getEndTime() != null) {
      endDatePicker.setValue(auction.getEndTime().toLocalDate());
      endHourCombo.setValue(auction.getEndTime().getHour());
      endMinuteCombo.setValue(auction.getEndTime().getMinute());
    }

    itemDescriptionArea.setText(
        auction.getItemDescription() != null ? auction.getItemDescription() : "");
    itemSpecificsArea.setText(auction.getItemSpecifics() != null ? auction.getItemSpecifics() : "");
    currentImageBase64 = auction.getImageBase64();
    if (currentImageBase64 != null && !currentImageBase64.isEmpty()) {
      try {
        byte[] imageBytes = Base64.getDecoder().decode(currentImageBase64);
        itemImageView.setImage(new Image(new ByteArrayInputStream(imageBytes)));
      } catch (Exception e) {
        itemImageView.setImage(null);
      }
    } else {
      itemImageView.setImage(null);
    }

    modalOverlay.setVisible(true);
  }

  @FXML
  private void hideCreateModal(ActionEvent event) {
    modalOverlay.setVisible(false);
  }

  @FXML
  private void handleCreateOrUpdateAuction(ActionEvent event) {
    String itemName = itemNameField.getText().trim();
    String priceText = startingPriceField.getText().trim();
    String minStepText = minBidStepField.getText().trim();

    if (categoryCombo.getValue() == null
        || itemName.isEmpty()
        || priceText.isEmpty()
        || startDatePicker.getValue() == null
        || endDatePicker.getValue() == null
        || startHourCombo.getValue() == null
        || startMinuteCombo.getValue() == null
        || endHourCombo.getValue() == null
        || endMinuteCombo.getValue() == null) {
      formResultLabel.setText("Vui lòng điền đầy đủ tất cả thông tin bắt buộc!");
      formResultLabel.setVisible(true);
      return;
    }

    long startingPrice;
    try {
      startingPrice = Long.parseLong(priceText);
    } catch (NumberFormatException e) {
      formResultLabel.setText("Giá khởi điểm phải là số hợp lệ.");
      formResultLabel.setVisible(true);
      return;
    }

    long minBidStep = 0;
    if (!minStepText.isEmpty()) {
      try {
        minBidStep = Long.parseLong(minStepText);
        if (minBidStep < 0) {
          formResultLabel.setText("Bước giá không được âm.");
          formResultLabel.setVisible(true);
          return;
        }
      } catch (NumberFormatException e) {
        formResultLabel.setText("Bước giá phải là số hợp lệ.");
        formResultLabel.setVisible(true);
        return;
      }
    }

    if (startingPrice <= 0) {
      formResultLabel.setText("Giá khởi điểm phải lớn hơn 0.");
      formResultLabel.setVisible(true);
      return;
    }

    String categoryEnum = categoryDisplayToEnum(categoryCombo.getValue());
    LocalDateTime startTime =
        startDatePicker.getValue().atTime(startHourCombo.getValue(), startMinuteCombo.getValue());
    LocalDateTime endTime =
        endDatePicker.getValue().atTime(endHourCombo.getValue(), endMinuteCombo.getValue());

    if (!endTime.isAfter(startTime)) {
      formResultLabel.setText("Lỗi: Thời gian kết thúc phải sau thời gian bắt đầu!");
      formResultLabel.setVisible(true);
      return;
    }

    Long sellerId = session.getCurrentUser().getId();

    String imageToSave = currentImageBase64;
    if (imageToSave == null || imageToSave.isEmpty()) {
      try (java.io.InputStream is = getClass().getResourceAsStream("/anh.jpg")) {
        if (is != null) {
          byte[] fileContent = is.readAllBytes();
          imageToSave = Base64.getEncoder().encodeToString(fileContent);
        } else {
          System.err.println("Không tìm thấy /anh.jpg trong resources.");
        }
      } catch (Exception e) {
        System.err.println("Không thể load ảnh mặc định anh.jpg: " + e.getMessage());
      }
    }

    if (editingAuctionId != null) {
      boolean success =
          serverService.updateAuctionSeller(
              editingAuctionId,
              sellerId,
              itemName,
              categoryEnum,
              startingPrice,
              startTime,
              endTime,
              itemDescriptionArea.getText(),
              itemSpecificsArea.getText(),
              imageToSave,
              minBidStep);
      if (success) {
        hideCreateModal(null);
        NotificationUtils.showSuccess(
            (Stage) refreshButton.getScene().getWindow(),
            "Cập nhật phiên #" + editingAuctionId + " thành công!");
        loadMyAuctions();
      } else {
        formResultLabel.setText("Cập nhật thất bại. Vui lòng kiểm tra lại trạng thái phiên.");
        formResultLabel.setVisible(true);
      }
    } else {
      Auction newAuction = new Auction();
      newAuction.setSellerId(sellerId);
      newAuction.setCurrentPrice(startingPrice);
      newAuction.setStartTime(startTime);
      newAuction.setEndTime(endTime);
      newAuction.setMinBidStep(minBidStep);
      newAuction.setItemName(itemName);
      newAuction.setItemCategory(categoryEnum);
      newAuction.setItemDescription(itemDescriptionArea.getText());
      newAuction.setItemSpecifics(itemSpecificsArea.getText());
      newAuction.setImageBase64(imageToSave);

      Long createdId = serverService.createAuction(newAuction);
      if (createdId != null && createdId > 0) {
        hideCreateModal(null);
        NotificationUtils.showSuccess(
            (Stage) refreshButton.getScene().getWindow(),
            "Tạo phiên đấu giá #" + createdId + " thành công!");
        loadMyAuctions();
      } else {
        formResultLabel.setText("Tạo phiên thất bại. Vui lòng kiểm tra lại thông tin.");
        formResultLabel.setVisible(true);
      }
    }
  }

  @FXML
  private void handleChooseImage(ActionEvent event) {
    FileChooser fileChooser = new FileChooser();
    fileChooser.setTitle("Chọn Hình Ảnh Sản Phẩm");
    fileChooser
        .getExtensionFilters()
        .addAll(new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg"));
    File selectedFile = fileChooser.showOpenDialog(itemNameField.getScene().getWindow());
    if (selectedFile != null) {
      try {
        try {
          BufferedImage img = ImageIO.read(selectedFile);
          if (img != null) {
            int targetSize = 800;
            if (img.getWidth() > targetSize || img.getHeight() > targetSize) {
              double scale =
                  Math.min(
                      (double) targetSize / img.getWidth(), (double) targetSize / img.getHeight());
              int w = (int) (img.getWidth() * scale);
              int h = (int) (img.getHeight() * scale);
              BufferedImage resized = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
              Graphics2D g = resized.createGraphics();
              g.drawImage(img, 0, 0, w, h, null);
              g.dispose();
              img = resized;
            } else {
              BufferedImage noAlpha =
                  new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_RGB);
              Graphics2D g = noAlpha.createGraphics();
              g.drawImage(img, 0, 0, null);
              g.dispose();
              img = noAlpha;
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(img, "jpg", baos);
            currentImageBase64 = Base64.getEncoder().encodeToString(baos.toByteArray());
          } else {
            byte[] fileContent = Files.readAllBytes(selectedFile.toPath());
            currentImageBase64 = Base64.getEncoder().encodeToString(fileContent);
          }
        } catch (Exception e) {
          byte[] fileContent = Files.readAllBytes(selectedFile.toPath());
          currentImageBase64 = Base64.getEncoder().encodeToString(fileContent);
        }
        itemImageView.setImage(new Image(selectedFile.toURI().toString()));
      } catch (Exception e) {
        formResultLabel.setText("Lỗi khi đọc file ảnh: " + e.getMessage());
        formResultLabel.setVisible(true);
      }
    }
  }

  private void handleDelete(Auction auction) {
    javafx.scene.control.Alert confirmAlert =
        new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.CONFIRMATION);
    NotificationUtils.styleAlert(confirmAlert);
    confirmAlert.setGraphic(null);
    confirmAlert.setTitle("Xác Nhận Xóa");
    confirmAlert.setHeaderText(
        "Bạn có chắc chắn muốn xóa phiên đấu giá #" + auction.getId() + " không?");
    confirmAlert.setContentText("Hành động này không thể hoàn tác!");

    java.util.Optional<javafx.scene.control.ButtonType> confirmResult = confirmAlert.showAndWait();
    if (confirmResult.isPresent() && confirmResult.get() == javafx.scene.control.ButtonType.OK) {
      boolean success =
          serverService.deleteAuctionSeller(auction.getId(), session.getCurrentUser().getId());
      if (success) {
        NotificationUtils.showSuccess(
            (Stage) refreshButton.getScene().getWindow(),
            "Đã xóa phiên đấu giá #" + auction.getId());
        loadMyAuctions();
      } else {
        NotificationUtils.showError(
            (Stage) refreshButton.getScene().getWindow(),
            "Xóa thất bại. Phiên có thể đã có người đặt giá.");
      }
    }
  }

  @FXML
  private void handleOpenWallet(ActionEvent event) {
    try {
      Stage stage = (Stage) walletButton.getScene().getWindow();
      FxmlLoader.navigateTo(stage, "wallet-view.fxml", "Online Auction System — Quản Lý Ví");
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @FXML
  private void handleLogout(ActionEvent event) {
    javafx.scene.control.Alert alert =
        new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.CONFIRMATION);
    NotificationUtils.styleAlert(alert);
    alert.setGraphic(null);
    alert.setTitle("Xác Nhận Đăng Xuất");
    alert.setHeaderText("Bạn có thực sự muốn đăng xuất không?");

    java.util.Optional<javafx.scene.control.ButtonType> result = alert.showAndWait();
    if (result.isPresent() && result.get() == javafx.scene.control.ButtonType.OK) {
      serverService.removeObserver(this);
      session.clearSession();
      try {
        Stage stage = (Stage) logoutButton.getScene().getWindow();
        FxmlLoader.navigateTo(stage, "login.fxml", "Online Auction System — Đăng Nhập");
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  private void clearForm() {
    itemNameField.clear();
    startingPriceField.clear();
    minBidStepField.clear();
    startDatePicker.setValue(null);
    endDatePicker.setValue(null);
    categoryCombo.setValue(CATEGORY_DISPLAY_NAMES[0]);
    startHourCombo.setValue(LocalDateTime.now().getHour());
    startMinuteCombo.setValue(LocalDateTime.now().getMinute());
    itemDescriptionArea.clear();
    itemSpecificsArea.clear();
    itemImageView.setImage(null);
    currentImageBase64 = null;

    itemNameField.setDisable(false);
    categoryCombo.setDisable(false);
    startingPriceField.setDisable(false);
    minBidStepField.setDisable(false);
    startDatePicker.setDisable(false);
    startHourCombo.setDisable(false);
    startMinuteCombo.setDisable(false);
    endDatePicker.setDisable(false);
    endHourCombo.setDisable(false);
    endMinuteCombo.setDisable(false);
  }

  @Override
  public void onBidUpdated(server.model.entity.BidTransaction bid) {
    Platform.runLater(
        () -> {
          NotificationUtils.showToast(
              (Stage) refreshButton.getScene().getWindow(),
              "📣 Giá mới cho #"
                  + bid.getAuctionId()
                  + ": "
                  + String.format("%,d", bid.getAmount())
                  + " VNĐ",
              false);
          loadMyAuctions();
        });
  }

  @Override
  public void onAuctionStatusChanged(Long auctionId, String newStatus) {
    Platform.runLater(
        () -> {
          NotificationUtils.showToast(
              (Stage) refreshButton.getScene().getWindow(),
              "🔔 Phiên #" + auctionId + " đã chuyển sang trạng thái: " + newStatus,
              false);
          loadMyAuctions();
        });
  }
}
