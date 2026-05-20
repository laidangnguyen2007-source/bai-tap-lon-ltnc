package com.auction.client.controller;
 
import com.auction.client.model.AuctionSessionState;
import com.auction.client.service.ServerService;
import com.auction.client.util.FxmlLoader;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import server.model.entity.Auction;
import server.model.enums.AuctionStatus;
 
/**
 * Controller cho màn hình Lịch Sử Thắng Cuộc (my-wins.fxml).
 */
public class MyWinsController {
 
    @FXML private TableView<Auction> winsTable;
    @FXML private TableColumn<Auction, Long> idColumn;
    @FXML private TableColumn<Auction, String> itemNameColumn;
    @FXML private TableColumn<Auction, String> finalPriceColumn;
    @FXML private TableColumn<Auction, String> endTimeColumn;
    @FXML private TableColumn<Auction, String> statusColumn;
 
    private final ServerService serverService = new ServerService();
    private final AuctionSessionState session = AuctionSessionState.getInstance();
    private final ObservableList<Auction> winsData = FXCollections.observableArrayList();
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
 
    @FXML
    public void initialize() {
        // Cấu hình các cột cho bảng
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        
        // Tên sản phẩm cần gọi server lấy thông tin (hoặc lấy từ cache nếu có)
        itemNameColumn.setCellValueFactory(cellData -> {
            Long itemId = cellData.getValue().getItemId();
            var item = serverService.getItemById(itemId);
            return new SimpleStringProperty(item != null ? item.getName() : "Sản phẩm #" + itemId);
        });
 
        finalPriceColumn.setCellValueFactory(cellData -> 
            new SimpleStringProperty(String.format("%,d", cellData.getValue().getCurrentPrice())));
 
        endTimeColumn.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getEndTime().format(DATE_FORMAT)));
 
        statusColumn.setCellValueFactory(cellData -> 
            new SimpleStringProperty(translateStatus(cellData.getValue().getStatus())));
 
        winsTable.setItems(winsData);
        loadWins();
    }
 
    /**
     * Tải danh sách các phiên thắng từ Server và lọc theo WinnerId của người dùng hiện tại.
     */
    private void loadWins() {
        if (!session.isLoggedIn()) return;
        
        // Lấy ID người dùng hiện tại từ phiên đăng nhập (Session)
        Long currentUserId = session.getCurrentUser().getId();
        
        // Gọi ServerService để lấy toàn bộ danh sách phiên đấu giá hiện có trên hệ thống
        List<Auction> all = serverService.getAllAuctions();
        
        // Sử dụng Stream API để lọc ra các phiên thỏa mãn đồng thời 2 điều kiện:
        // 1. currentWinnerId khớp với ID của mình (Nghĩa là mình đang là người trả giá cao nhất)
        // 2. Trạng thái phiên là FINISHED hoặc PAID (Nghĩa là phiên đã đóng, xác nhận mình là người thắng cuộc)
        List<Auction> won = all.stream()
                .filter(a -> currentUserId.equals(a.getCurrentWinnerId()))
                .filter(a -> a.getStatus() == AuctionStatus.FINISHED || a.getStatus() == AuctionStatus.PAID)
                .collect(Collectors.toList());
        
        // Đưa dữ liệu đã lọc vào ObservableList để hiển thị lên TableView
        winsData.setAll(won);
    }
 
    @FXML
    private void handleBack(ActionEvent event) {
        try {
            Stage stage = (Stage) winsTable.getScene().getWindow();
            FxmlLoader.navigateTo(stage, "auction-list.fxml", "Online Auction System — Danh Sách Đấu Giá");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
 
    private String translateStatus(AuctionStatus status) {
        if (status == null) return "Không rõ";
        return switch (status) {
            case OPEN -> "Đang mở";
            case RUNNING -> "Đang diễn ra";
            case FINISHED -> "Đã thắng (Chờ)";
            case PAID -> "Đã thanh toán";
            case CANCELED -> "Đã hủy";
        };
    }
}
