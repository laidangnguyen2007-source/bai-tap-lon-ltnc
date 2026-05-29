package com.auction.client.controller;

import com.auction.client.model.AuctionSessionState;
import com.auction.client.service.ServerService;
import com.auction.client.util.FxmlLoader;
import com.auction.client.util.NotificationUtils;
import java.io.IOException;
import java.util.List;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.json.simple.JSONObject;

/**
 * Controller cho màn hình Quản Lý Ví Admin (admin-wallet.fxml). Cho phép admin xem tất cả ví, điều
 * chỉnh số dư, và xem lịch sử giao dịch.
 */
public class AdminWalletController {

  // -- Form điều chỉnh --
  @FXML private TextField adjustUserIdField;
  @FXML private TextField adjustAmountField;
  @FXML private TextField adjustReasonField;
  @FXML private Label adjustResultLabel;

  // -- Bảng ví --
  @FXML private TableView<JSONObject> walletTable;
  @FXML private TableColumn<JSONObject, String> wUserIdCol;
  @FXML private TableColumn<JSONObject, String> wAvailableCol;
  @FXML private TableColumn<JSONObject, String> wLockedCol;
  @FXML private TableColumn<JSONObject, String> wTotalCol;

  // -- Bảng giao dịch --
  @FXML private TableView<JSONObject> txLogTable;
  @FXML private TableColumn<JSONObject, String> logIdCol;
  @FXML private TableColumn<JSONObject, String> logUserIdCol;
  @FXML private TableColumn<JSONObject, String> logTypeCol;
  @FXML private TableColumn<JSONObject, String> logAmountCol;
  @FXML private TableColumn<JSONObject, String> logDescCol;
  @FXML private TableColumn<JSONObject, String> logTimeCol;

  @FXML private Button refreshButton;
  @FXML private Button backButton;

  private final ServerService serverService = new ServerService();
  private final AuctionSessionState session = AuctionSessionState.getInstance();
  private final ObservableList<JSONObject> wallets = FXCollections.observableArrayList();
  private final ObservableList<JSONObject> txLogs = FXCollections.observableArrayList();

  @FXML
  public void initialize() {
    // Cấu hình bảng ví
    wUserIdCol.setCellValueFactory(
        cell -> {
          Object uid = cell.getValue().get("userId");
          return new SimpleStringProperty(uid != null ? uid.toString() : "-");
        });
    wAvailableCol.setCellValueFactory(
        cell -> {
          long val = getNumericValue(cell.getValue(), "availableBalance");
          return new SimpleStringProperty(String.format("%,d VNĐ", val));
        });
    wLockedCol.setCellValueFactory(
        cell -> {
          long val = getNumericValue(cell.getValue(), "lockedBalance");
          return new SimpleStringProperty(String.format("%,d VNĐ", val));
        });
    wTotalCol.setCellValueFactory(
        cell -> {
          long val = getNumericValue(cell.getValue(), "totalBalance");
          return new SimpleStringProperty(String.format("%,d VNĐ", val));
        });
    walletTable.setItems(wallets);

    // Click vào hàng wallet → load giao dịch của user đó
    walletTable
        .getSelectionModel()
        .selectedItemProperty()
        .addListener(
            (obs, oldVal, newVal) -> {
              if (newVal != null) {
                long userId = getNumericValue(newVal, "userId");
                adjustUserIdField.setText(String.valueOf(userId));
                loadTransactionsForUser(userId);
              }
            });

    // Cấu hình bảng giao dịch
    logIdCol.setCellValueFactory(
        cell -> {
          Object id = cell.getValue().get("id");
          return new SimpleStringProperty(id != null ? id.toString() : "-");
        });
    logUserIdCol.setCellValueFactory(
        cell -> {
          Object uid = cell.getValue().get("userId");
          return new SimpleStringProperty(uid != null ? uid.toString() : "-");
        });
    logTypeCol.setCellValueFactory(
        cell -> {
          String type = (String) cell.getValue().get("type");
          return new SimpleStringProperty(translateTxType(type));
        });
    logAmountCol.setCellValueFactory(
        cell -> {
          long amt = getNumericValue(cell.getValue(), "amount");
          return new SimpleStringProperty(String.format("%,d VNĐ", amt));
        });
    logDescCol.setCellValueFactory(
        cell -> {
          String desc = (String) cell.getValue().get("description");
          return new SimpleStringProperty(desc != null ? desc : "");
        });
    logTimeCol.setCellValueFactory(
        cell -> {
          String time = (String) cell.getValue().get("createdAt");
          if (time != null && time.contains("T")) {
            time = time.replace("T", " ");
            if (time.length() > 19) time = time.substring(0, 19);
          }
          return new SimpleStringProperty(time != null ? time : "-");
        });
    txLogTable.setItems(txLogs);

    // Load wallets
    loadAllWallets();
  }

  /** Load tất cả ví từ server (background thread). */
  private void loadAllWallets() {
    new Thread(
            () -> {
              List<JSONObject> list = serverService.adminGetAllWallets("ADMIN");
              Platform.runLater(() -> wallets.setAll(list));
            },
            "AdminWalletLoader")
        .start();
  }

  /** Load giao dịch cho một user cụ thể. */
  private void loadTransactionsForUser(long userId) {
    new Thread(
            () -> {
              List<JSONObject> list = serverService.getWalletTransactions(userId);
              Platform.runLater(() -> txLogs.setAll(list));
            },
            "AdminTxLoader")
        .start();
  }

  @FXML
  private void handleAdjust(ActionEvent event) {
    String userIdText = adjustUserIdField.getText().trim();
    String amountText = adjustAmountField.getText().trim();
    String reason = adjustReasonField.getText().trim();

    if (userIdText.isEmpty() || amountText.isEmpty()) {
      adjustResultLabel.setText("Vui lòng nhập User ID và số tiền.");
      return;
    }

    long userId;
    long amount;
    try {
      userId = Long.parseLong(userIdText);
      amount = Long.parseLong(amountText);
    } catch (NumberFormatException e) {
      adjustResultLabel.setText("User ID và số tiền phải là số hợp lệ.");
      return;
    }

    if (amount == 0) {
      adjustResultLabel.setText("Số tiền phải khác 0.");
      return;
    }

    if (reason.isEmpty()) {
      reason = "Admin adjustment";
    }

    Long adminId = session.getCurrentUser().getId();
    String desc = reason;

    new Thread(
            () -> {
              boolean success =
                  serverService.adminAdjustBalance(userId, amount, adminId, "ADMIN", desc);
              Platform.runLater(
                  () -> {
                    if (success) {
                      adjustResultLabel.setText("✅ Đã điều chỉnh thành công cho user #" + userId);
                      adjustAmountField.clear();
                      adjustReasonField.clear();
                      loadAllWallets();
                      loadTransactionsForUser(userId);
                      NotificationUtils.showSuccess(
                          (Stage) refreshButton.getScene().getWindow(),
                          "Điều chỉnh số dư cho user #" + userId + " thành công!");
                    } else {
                      adjustResultLabel.setText("❌ Điều chỉnh thất bại. Vui lòng kiểm tra lại.");
                      NotificationUtils.showError(
                          (Stage) refreshButton.getScene().getWindow(), "Điều chỉnh thất bại.");
                    }
                  });
            },
            "AdminAdjust")
        .start();
  }

  @FXML
  private void handleRefresh(ActionEvent event) {
    loadAllWallets();
    txLogs.clear();
    adjustResultLabel.setText("");
    NotificationUtils.showToast(
        (Stage) refreshButton.getScene().getWindow(), "✅ Đã làm mới", false);
  }

  @FXML
  private void handleBack(ActionEvent event) {
    try {
      Stage stage = (Stage) backButton.getScene().getWindow();
      FxmlLoader.navigateTo(
          stage, "auction-list.fxml", "Online Auction System — Danh Sách Đấu Giá");
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /** Lấy giá trị số từ JSONObject, an toàn với null. */
  private long getNumericValue(JSONObject json, String key) {
    Object val = json.get(key);
    if (val == null) return 0;
    return ((Number) val).longValue();
  }

  /** Dịch loại giao dịch sang tiếng Việt. */
  private String translateTxType(String type) {
    if (type == null) return "-";
    return switch (type) {
      case "BID_LOCK" -> "Khóa tiền đấu giá";
      case "BID_RELEASE" -> "Giải phóng (bị vượt)";
      case "AUTO_BID_LOCK" -> "Khóa Auto-Bid";
      case "AUTO_BID_RELEASE" -> "Giải phóng Auto-Bid";
      case "AUCTION_WIN" -> "Thắng đấu giá";
      case "SELLER_PAYOUT" -> "Nhận tiền bán";
      case "ADMIN_ADJUSTMENT" -> "Admin điều chỉnh";
      default -> type;
    };
  }
}
