package com.auction.server.handler;

import com.auction.server.dao.AuctionDao;
import com.auction.server.dao.BidTransactionDao;
import com.auction.server.dao.ItemDao;
import com.auction.server.model.entity.Auction;
import com.auction.server.model.entity.item.Item;
import com.auction.server.model.entity.item.ItemFactory;
import com.auction.server.model.enums.AuctionStatus;
import com.auction.server.model.enums.ItemCategory;
import com.auction.server.net.ClientBroadcaster;
import com.auction.server.net.JsonResponses;
import com.auction.server.service.AuctionManager;
import java.time.LocalDateTime;
import java.util.Optional;
import org.json.JSONObject;

/**
 * Các thao tác <b>thay đổi trạng thái</b> phiên đấu giá: tạo, xóa, admin chỉnh, reset lịch sử bid.
 *
 * <p>Khác {@link CatalogHandlers} (chỉ đọc), lớp này ghi DB + đồng bộ {@link AuctionManager} trong
 * RAM để hành vi giữa socket và persistence không lệch.
 */
public final class AuctionCommandHandlers {

  private final AuctionDao auctionDao;
  private final ItemDao itemDao;
  private final BidTransactionDao bidTransactionDao;
  private final ClientBroadcaster broadcaster;

  public AuctionCommandHandlers(
      AuctionDao auctionDao,
      ItemDao itemDao,
      BidTransactionDao bidTransactionDao,
      ClientBroadcaster broadcaster) {
    this.auctionDao = auctionDao;
    this.itemDao = itemDao;
    this.bidTransactionDao = bidTransactionDao;
    this.broadcaster = broadcaster;
  }

  /**
   * Tạo phiên đấu giá mới — đồng thời tự động tạo Item (sản phẩm) trong database.
   *
   * <p><b>Luồng xử lý:</b>
   *
   * <ol>
   *   <li>Nhận thông tin từ client: tên sản phẩm, loại sản phẩm, giá, thời gian
   *   <li>Tạo Item mới qua {@code ItemFactory.createSimpleItem()} và lưu vào DB
   *   <li>Lấy itemId tự động sinh bởi AUTO_INCREMENT (không bao giờ trùng)
   *   <li>Tạo Auction gắn với itemId vừa tạo
   *   <li>Xác định trạng thái dựa trên thời gian bắt đầu
   * </ol>
   */
  public String createAuction(JSONObject req) throws Exception {
    // --- Đọc dữ liệu từ request JSON ---
    String itemName = req.getString("itemName"); // Tên sản phẩm do Seller đặt
    String categoryStr =
        req.getString("category"); // Loại sản phẩm (ELECTRONICS, ARTWORK, VEHICLE, OTHER)
    Long sellerId = req.getLong("sellerId");
    Long startingPrice = req.getLong("startingPrice");
    LocalDateTime startTime = LocalDateTime.parse(req.getString("startTime"));
    LocalDateTime endTime = LocalDateTime.parse(req.getString("endTime"));
    LocalDateTime now = LocalDateTime.now();

    // --- Validate dữ liệu đầu vào ---
    if (itemName == null || itemName.trim().isEmpty()) {
      return JsonResponses.error("Tên sản phẩm không được để trống!");
    }

    // Chuyển đổi chuỗi category thành enum, báo lỗi nếu không hợp lệ
    ItemCategory category;
    try {
      category = ItemCategory.valueOf(categoryStr);
    } catch (IllegalArgumentException e) {
      return JsonResponses.error("Loại sản phẩm không hợp lệ: " + categoryStr);
    }

    // --- Bước 1: Tạo Item mới trong database ---
    // Sử dụng ItemFactory.createSimpleItem() để tạo Item với giá trị mặc định
    // cho các trường đặc thù (brand, warranty...). ID được AUTO_INCREMENT tự sinh.
    Item newItem = ItemFactory.createSimpleItem(category, itemName.trim(), startingPrice, sellerId);
    itemDao.save(newItem); // Sau lệnh này, newItem.getId() sẽ có giá trị từ DB

    Long itemId = newItem.getId();
    System.out.println(
        "CREATE_AUCTION: Auto-created item #"
            + itemId
            + " ["
            + category
            + "] \""
            + itemName
            + "\"");

    // --- Bước 2: Tạo Auction gắn với Item vừa tạo ---
    Auction auction = new Auction(itemId, sellerId, startTime, endTime);
    auction.setCurrentPrice(startingPrice);

    // --- Bước 3: Xác định trạng thái dựa trên thời gian ---
    // - startTime đã qua & endTime chưa qua → RUNNING (chạy ngay)
    // - startTime ở tương lai → OPEN (đợi đến giờ)
    // - endTime đã qua → FINISHED (đã hết hạn)
    if (now.isAfter(startTime) && now.isBefore(endTime)) {
      auction.setStatus(AuctionStatus.RUNNING);
    } else if (now.isAfter(endTime)) {
      auction.setStatus(AuctionStatus.FINISHED);
    } else {
      auction.setStatus(AuctionStatus.OPEN);
    }

    // Lưu Auction vào database
    auctionDao.save(auction);

    // Nếu phiên đang RUNNING thì đưa vào bộ nhớ đệm RAM để xử lý bid realtime
    if (auction.getStatus() == AuctionStatus.RUNNING) {
      AuctionManager.getInstance().restoreRunningAuction(auction);
    }

    System.out.println(
        "CREATE_AUCTION: auction #" + auction.getId() + " | status: " + auction.getStatus());

    JSONObject res = new JSONObject();
    res.put("status", "OK");
    res.put("auctionId", auction.getId());
    return res.toString();
  }

  public String deleteAuction(JSONObject req) throws Exception {
    Long auctionId = req.getLong("auctionId");
    AuctionManager.getInstance().closeAuction(auctionId);
    boolean deleted = auctionDao.deleteById(auctionId);
    if (deleted) {
      System.out.println("ADMIN ACTION: Deleted auction #" + auctionId);
      JSONObject res = new JSONObject();
      res.put("status", "OK");
      return res.toString();
    }
    return JsonResponses.error("Không tìm thấy phiên đấu giá #" + auctionId + " để xóa.");
  }

  public String adminUpdateAuction(JSONObject req) throws Exception {
    Long auctionId = req.getLong("auctionId");
    Long newPrice = req.getLong("currentPrice");
    AuctionStatus newStatus = AuctionStatus.valueOf(req.getString("status"));
    LocalDateTime newStartTime = LocalDateTime.parse(req.getString("startTime"));
    LocalDateTime newEndTime = LocalDateTime.parse(req.getString("endTime"));
    String newCategory = req.getString("category");

    Optional<Auction> auctionOpt = auctionDao.findById(auctionId);
    if (auctionOpt.isEmpty()) {
      return JsonResponses.error("Không tìm thấy đấu giá #" + auctionId);
    }
    Auction auction = auctionOpt.get();

    auction.setCurrentPrice(newPrice);
    auction.setStatus(newStatus);
    auction.setStartTime(newStartTime);
    auction.setEndTime(newEndTime);
    auctionDao.update(auction);

    Optional<Item> itemOpt = itemDao.findById(auction.getItemId());
    if (itemOpt.isPresent()) {
      Item item = itemOpt.get();
      item.setCategory(ItemCategory.valueOf(newCategory));
      itemDao.update(item);
    }

    if (newStatus == AuctionStatus.RUNNING) {
      AuctionManager.getInstance().restoreRunningAuction(auction);
    } else {
      AuctionManager.getInstance().closeAuction(auctionId);
    }

    System.out.println(
        "ADMIN ACTION: Updated auction #"
            + auctionId
            + " to price="
            + newPrice
            + ", status="
            + newStatus);

    JSONObject res = new JSONObject();
    res.put("status", "OK");
    return res.toString();
  }

  public String resetAuction(JSONObject req) throws Exception {
    Long auctionId = req.getLong("auctionId");
    Optional<Auction> auctionOpt = auctionDao.findById(auctionId);
    if (auctionOpt.isEmpty()) {
      return JsonResponses.error("Không tìm thấy đấu giá #" + auctionId);
    }
    Auction auction = auctionOpt.get();
    Optional<Item> itemOpt = itemDao.findById(auction.getItemId());

    if (itemOpt.isEmpty()) {
      return JsonResponses.error("Không tìm thấy sản phẩm liên quan để lấy giá khởi điểm.");
    }

    bidTransactionDao.deleteByAuctionId(auctionId);
    long startingPrice = itemOpt.get().getStartingPrice();
    auction.setCurrentPrice(startingPrice);
    auction.setCurrentWinnerId(null);
    auctionDao.update(auction);

    if (auction.getStatus() == AuctionStatus.RUNNING) {
      AuctionManager.getInstance().restoreRunningAuction(auction);
    }

    System.out.println("ADMIN ACTION: Reset auction #" + auctionId + " to price=" + startingPrice);

    JSONObject push = new JSONObject();
    push.put("type", "AUCTION_RESET");
    push.put("auctionId", auctionId);
    push.put("newPrice", startingPrice);
    broadcaster.broadcast(push.toString());

    JSONObject res = new JSONObject();
    res.put("status", "OK");
    return res.toString();
  }

  public String sellerUpdateAuction(JSONObject req) throws Exception {
    Long auctionId = req.getLong("auctionId");
    Long sellerId = req.getLong("sellerId");
    String itemName = req.getString("itemName");
    String categoryStr = req.getString("category");
    Long startingPrice = req.getLong("startingPrice");
    LocalDateTime startTime = LocalDateTime.parse(req.getString("startTime"));
    LocalDateTime endTime = LocalDateTime.parse(req.getString("endTime"));

    Optional<Auction> auctionOpt = auctionDao.findById(auctionId);
    if (auctionOpt.isEmpty()) {
      return JsonResponses.error("Không tìm thấy đấu giá #" + auctionId);
    }
    Auction auction = auctionOpt.get();

    if (!auction.getSellerId().equals(sellerId)) {
      return JsonResponses.error("Bạn không có quyền sửa phiên đấu giá này.");
    }

    if (auction.getStatus() != AuctionStatus.OPEN) {
      return JsonResponses.error("Chỉ có thể sửa phiên đấu giá chưa bắt đầu.");
    }

    if (itemName == null || itemName.trim().isEmpty()) {
      return JsonResponses.error("Tên sản phẩm không được để trống!");
    }

    ItemCategory category;
    try {
      category = ItemCategory.valueOf(categoryStr);
    } catch (IllegalArgumentException e) {
      return JsonResponses.error("Loại sản phẩm không hợp lệ: " + categoryStr);
    }

    // Update Auction
    auction.setCurrentPrice(startingPrice);
    auction.setStartTime(startTime);
    auction.setEndTime(endTime);

    // Update status based on new times
    LocalDateTime now = LocalDateTime.now();
    if (now.isAfter(startTime) && now.isBefore(endTime)) {
      auction.setStatus(AuctionStatus.RUNNING);
    } else if (now.isAfter(endTime)) {
      auction.setStatus(AuctionStatus.FINISHED);
    } else {
      auction.setStatus(AuctionStatus.OPEN);
    }

    auctionDao.update(auction);

    // Update Item
    Optional<Item> itemOpt = itemDao.findById(auction.getItemId());
    if (itemOpt.isPresent()) {
      Item item = itemOpt.get();
      item.setName(itemName);
      item.setCategory(category);
      item.setStartingPrice(startingPrice);
      itemDao.update(item);
    }

    if (auction.getStatus() == AuctionStatus.RUNNING) {
      AuctionManager.getInstance().restoreRunningAuction(auction);
    }

    JSONObject res = new JSONObject();
    res.put("status", "OK");
    return res.toString();
  }

  public String sellerDeleteAuction(JSONObject req) throws Exception {
    Long auctionId = req.getLong("auctionId");
    Long sellerId = req.getLong("sellerId");

    Optional<Auction> auctionOpt = auctionDao.findById(auctionId);
    if (auctionOpt.isEmpty()) {
      return JsonResponses.error("Không tìm thấy phiên đấu giá #" + auctionId);
    }
    Auction auction = auctionOpt.get();

    if (!auction.getSellerId().equals(sellerId)) {
      return JsonResponses.error("Bạn không có quyền xóa phiên đấu giá này.");
    }

    if (auction.getCurrentWinnerId() != null) {
      return JsonResponses.error("Không thể xóa phiên đấu giá đã có người đặt giá.");
    }

    AuctionManager.getInstance().closeAuction(auctionId);
    auctionDao.deleteById(auctionId);
    itemDao.deleteById(auction.getItemId());

    JSONObject res = new JSONObject();
    res.put("status", "OK");
    return res.toString();
  }
}
