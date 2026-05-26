package com.auction.client.controller;

import com.auction.client.model.AuctionSessionState;
import com.auction.client.service.ServerService;
import com.auction.client.util.FxmlLoader;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
import server.model.entity.BidTransaction;
import server.model.entity.item.Item;

public class MyBidsController {

    @FXML private TableView<BidRow> bidsTable;
    @FXML private TableColumn<BidRow, Long> auctionIdColumn;
    @FXML private TableColumn<BidRow, String> itemNameColumn;
    @FXML private TableColumn<BidRow, String> bidAmountColumn;
    @FXML private TableColumn<BidRow, String> endTimeColumn;
    @FXML private TableColumn<BidRow, String> bidTimeColumn;

    private final ServerService serverService = new ServerService();
    private final AuctionSessionState session = AuctionSessionState.getInstance();
    private final ObservableList<BidRow> bidsData = FXCollections.observableArrayList();
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    public static class BidRow {
        private final Long auctionId;
        private final String itemName;
        private final long bidAmount;
        private final LocalDateTime endTime;
        private final LocalDateTime bidTime;

        public BidRow(Long auctionId, String itemName, long bidAmount, LocalDateTime endTime, LocalDateTime bidTime) {
            this.auctionId = auctionId;
            this.itemName = itemName;
            this.bidAmount = bidAmount;
            this.endTime = endTime;
            this.bidTime = bidTime;
        }

        public Long getAuctionId() { return auctionId; }
        public String getItemName() { return itemName; }
        public long getBidAmount() { return bidAmount; }
        public LocalDateTime getEndTime() { return endTime; }
        public LocalDateTime getBidTime() { return bidTime; }
    }

    @FXML
    public void initialize() {
        auctionIdColumn.setCellValueFactory(new PropertyValueFactory<>("auctionId"));
        itemNameColumn.setCellValueFactory(new PropertyValueFactory<>("itemName"));
        
        bidAmountColumn.setCellValueFactory(cellData -> 
            new SimpleStringProperty(String.format("%,d", cellData.getValue().getBidAmount())));

        endTimeColumn.setCellValueFactory(cellData -> {
            LocalDateTime et = cellData.getValue().getEndTime();
            return new SimpleStringProperty(et != null ? et.format(DATE_FORMAT) : "N/A");
        });

        bidTimeColumn.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getBidTime().format(DATE_FORMAT)));

        bidsTable.setItems(bidsData);
        loadBids();
    }

    private void loadBids() {
        if (!session.isLoggedIn()) return;
        
        Long currentUserId = session.getCurrentUser().getId();
        
        // Fetch user's bids
        List<BidTransaction> myBids = serverService.getUserBids(currentUserId);
        
        // Fetch all auctions to get item mapping and end times
        List<Auction> allAuctions = serverService.getAllAuctions();
        Map<Long, Auction> auctionMap = allAuctions.stream().collect(Collectors.toMap(Auction::getId, a -> a));
        
        List<BidRow> rows = new ArrayList<>();
        for (BidTransaction bid : myBids) {
            Auction auction = auctionMap.get(bid.getAuctionId());
            String itemName = "Phiên đấu giá #" + bid.getAuctionId();
            LocalDateTime endTime = null;
            
            if (auction != null) {
                endTime = auction.getEndTime();
                Item item = serverService.getItemById(auction.getItemId());
                if (item != null) {
                    itemName = item.getName();
                }
            }
            
            rows.add(new BidRow(bid.getAuctionId(), itemName, bid.getAmount(), endTime, bid.getTimestamp()));
        }
        
        // Sắp xếp theo thời gian đặt giá mới nhất
        rows.sort((r1, r2) -> r2.getBidTime().compareTo(r1.getBidTime()));
        
        bidsData.setAll(rows);
    }

    @FXML
    private void handleBack(ActionEvent event) {
        try {
            Stage stage = (Stage) bidsTable.getScene().getWindow();
            FxmlLoader.navigateTo(stage, "auction-list.fxml", "Online Auction System — Danh Sách Đấu Giá");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
