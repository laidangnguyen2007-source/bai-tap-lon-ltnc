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
import javafx.stage.Stage;
import org.json.simple.JSONObject;
import server.model.enums.UserRole;

/**
 * Controller cho màn hình Quản Lý Ví (wallet-view.fxml).
 * Hiển thị số dư ví và lịch sử giao dịch cho Bidder/Seller.
 */
public class WalletViewController implements com.auction.client.observer.AuctionObserver {

    @FXML private Label userInfoLabel;
    @FXML private Label totalBalanceLabel;
    @FXML private Label availableBalanceLabel;
    @FXML private Label lockedBalanceLabel;
    @FXML private Button refreshButton;
    @FXML private Button backButton;

    @FXML private TableView<JSONObject> transactionTable;
    @FXML private TableColumn<JSONObject, String> txIdCol;
    @FXML private TableColumn<JSONObject, String> txTypeCol;
    @FXML private TableColumn<JSONObject, String> txAmountCol;
    @FXML private TableColumn<JSONObject, String> txDescCol;
    @FXML private TableColumn<JSONObject, String> txTimeCol;

    private final ServerService serverService = new ServerService();
    private final AuctionSessionState session = AuctionSessionState.getInstance();
    private final ObservableList<JSONObject> transactions = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        if (session.isLoggedIn()) {
            userInfoLabel.setText("👤 " + session.getCurrentUser().getUsername()
                + " (" + translateRole(session.getCurrentUser().getRole()) + ")");
        }

        // Cấu hình cột bảng giao dịch
        txIdCol.setCellValueFactory(cell -> {
            Object id = cell.getValue().get("id");
            return new SimpleStringProperty(id != null ? id.toString() : "-");
        });
        txTypeCol.setCellValueFactory(cell -> {
            String type = (String) cell.getValue().get("type");
            return new SimpleStringProperty(translateTxType(type));
        });
        txAmountCol.setCellValueFactory(cell -> {
            Object amt = cell.getValue().get("amount");
            long amount = amt != null ? ((Number) amt).longValue() : 0;
            return new SimpleStringProperty(String.format("%,d VNĐ", amount));
        });
        txDescCol.setCellValueFactory(cell -> {
            String desc = (String) cell.getValue().get("description");
            return new SimpleStringProperty(desc != null ? desc : "");
        });
        txTimeCol.setCellValueFactory(cell -> {
            String time = (String) cell.getValue().get("createdAt");
            if (time != null && time.contains("T")) {
                time = time.replace("T", " ");
                if (time.length() > 19) time = time.substring(0, 19);
            }
            return new SimpleStringProperty(time != null ? time : "-");
        });

        transactionTable.setItems(transactions);

        // Đăng ký observer cho push notification
        serverService.addObserver(this);

        // Load dữ liệu
        loadWalletData();
    }

    /** Load thông tin ví và lịch sử giao dịch từ server (chạy trên background thread). */
    private void loadWalletData() {
        Long userId = session.getCurrentUser().getId();

        new Thread(() -> {
            JSONObject wallet = serverService.getWallet(userId);
            List<JSONObject> txList = serverService.getWalletTransactions(userId);

            Platform.runLater(() -> {
                if (wallet != null) {
                    long available = wallet.get("availableBalance") != null
                        ? ((Number) wallet.get("availableBalance")).longValue() : 0;
                    long locked = wallet.get("lockedBalance") != null
                        ? ((Number) wallet.get("lockedBalance")).longValue() : 0;
                    long total = wallet.get("totalBalance") != null
                        ? ((Number) wallet.get("totalBalance")).longValue() : 0;

                    availableBalanceLabel.setText(String.format("%,d VNĐ", available));
                    lockedBalanceLabel.setText(String.format("%,d VNĐ", locked));
                    totalBalanceLabel.setText(String.format("%,d VNĐ", total));
                } else {
                    availableBalanceLabel.setText("0 VNĐ");
                    lockedBalanceLabel.setText("0 VNĐ");
                    totalBalanceLabel.setText("0 VNĐ");
                }

                transactions.setAll(txList);
            });
        }, "WalletDataLoader").start();
    }

    @FXML
    private void handleRefresh(ActionEvent event) {
        loadWalletData();
        NotificationUtils.showToast((Stage) refreshButton.getScene().getWindow(), "✅ Đã làm mới dữ liệu ví", false);
    }

    @FXML
    private void handleBack(ActionEvent event) {
        serverService.removeObserver(this);
        try {
            Stage stage = (Stage) backButton.getScene().getWindow();
            if (session.isLoggedIn() && session.getCurrentUser().getRole() == UserRole.SELLER) {
                FxmlLoader.navigateTo(stage, "seller-dashboard.fxml", "Online Auction System — Quản Lý Bán Hàng");
            } else {
                FxmlLoader.navigateTo(stage, "auction-list.fxml", "Online Auction System — Danh Sách Đấu Giá");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // -- Observer callbacks --

    @Override
    public void onBidUpdated(server.model.entity.BidTransaction bid) {
        // Refresh khi có bid mới (có thể ảnh hưởng tới locked balance)
        Platform.runLater(this::loadWalletData);
    }

    @Override
    public void onAuctionStatusChanged(Long auctionId, String newStatus) {
        Platform.runLater(this::loadWalletData);
    }

    @Override
    public void onWalletEvent(String eventType, JSONObject payload) {
        Platform.runLater(() -> {
            String msg = switch (eventType) {
                case "FUNDS_LOCKED" -> "🔒 Đã khóa tiền cho phiên đấu giá";
                case "FUNDS_RELEASED" -> "🔓 Đã giải phóng tiền";
                case "OUTBID", "OUTBID_NOTIFICATION" -> "⚠ Bạn đã bị vượt giá!";
                case "AUCTION_WON" -> "🎉 Chúc mừng! Bạn đã thắng đấu giá!";
                case "SELLER_PAYOUT" -> "💵 Bạn đã nhận tiền từ đấu giá!";
                case "ADMIN_BALANCE_ADJUSTED" -> "🔧 Admin đã điều chỉnh số dư ví của bạn";
                case "AUTO_BID_CANCELLED" -> "❌ Đã hủy đấu giá tự động";
                default -> "💰 Cập nhật ví";
            };
            try {
                NotificationUtils.showToast((Stage) refreshButton.getScene().getWindow(), msg, false);
            } catch (Exception ignored) {}
            loadWalletData();
        });
    }

    /** Dịch role sang tiếng Việt. */
    private String translateRole(UserRole role) {
        if (role == null) return "User";
        return switch (role) {
            case BIDDER -> "Người đấu giá";
            case SELLER -> "Người bán";
            case ADMIN -> "Quản trị viên";
        };
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
