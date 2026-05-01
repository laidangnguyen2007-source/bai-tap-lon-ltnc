package com.auction.server;

import com.auction.server.config.DatabaseConfig;
import com.auction.server.dao.*;
import com.auction.server.dao.impl.*;
import com.auction.server.model.entity.Auction;
import com.auction.server.model.entity.BidTransaction;
import com.auction.server.model.entity.item.*;
import com.auction.server.model.entity.user.*;
import com.auction.server.model.enums.AuctionStatus;
import com.auction.server.service.AuctionManager;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.JSONArray;
import org.json.JSONObject;

public class Server {
  private static final int PORT = 8888;

  // Danh sách writer của tất cả client đang online — dùng để broadcast push message
  // CopyOnWriteArrayList đảm bảo thread-safe khi thêm/xóa client đồng thời
  private static final List<PrintWriter> connectedClients = new CopyOnWriteArrayList<>();

  // DAO tầng database - khởi tạo 1 lần khi server boot
  private static UserDao userDao;
  private static AuctionDao auctionDao;
  private static ItemDao itemDao;
  private static BidTransactionDao bidTransactionDao;

  public static void main(String[] args) {
    System.out.println("═══════════════════════════════════════");
    System.out.println("   AUCTION SERVER — Starting...");
    System.out.println("═══════════════════════════════════════");
    // Khởi tạo DAO (kết nối MySQL, tạo schema nếu chưa có)
    try {
      userDao = new JdbcUserDao();
      auctionDao = new JdbcAuctionDao();
      itemDao = new JdbcItemDao();
      bidTransactionDao = new JdbcBidTransactionDao();
      System.out.println("MySQL connection successful — auction_db is ready.");
      
      // Nạp các phiên đấu giá đang chạy vào bộ nhớ đệm
      List<Auction> runningAuctions = auctionDao.findRunningAuctions();
      for (Auction a : runningAuctions) {
        AuctionManager.getInstance().restoreRunningAuction(a);
      }
      System.out.println("Loaded " + runningAuctions.size() + " running auctions into memory.");


    } catch (Exception e) {
      System.err.println("Unable to connect to MySql: " + e.getMessage());
      System.err.println("Please make sure XAMPP MySQL is running on port 3306.");
      System.exit(1);
    }

    // Đăng ký shutdown hook: đóng DB connection khi server tắt
    Runtime.getRuntime()
        .addShutdownHook(
            new Thread(
                () -> {
                  System.out.println("\nServer is shutting down - closing DB connection...");
                  try {
                    DatabaseConfig.getInstance().close();
                  } catch (Exception ignored) {
                    ignored.printStackTrace();
                  }
                }));

    // Mở ServerSocket và vào vòng lập chấp nhận Client
    ExecutorService threadPool = Executors.newCachedThreadPool();
    try (ServerSocket serverSocket = new ServerSocket(PORT)) {
      System.out.println("Server is listening on port " + PORT);
      System.out.println("═══════════════════════════════════════\n");

      while (!serverSocket.isClosed()) {
        Socket clientSocket = serverSocket.accept();
        System.out.println("New Client: " + clientSocket.getInetAddress());
        threadPool.submit(() -> handleClient(clientSocket));
      }
    } catch (IOException e) {
      System.err.println("ServerSocket error: " + e.getMessage());
    } finally {
      threadPool.shutdown();
    }
  }

  // ─── XỬ LÝ TỪNG CLIENT ──────────────────────────────────────────

  /**
   * Vòng lặp đọc/ghi cho 1 client. Chạy trên thread riêng do threadPool cấp. Khi client ngắt kết
   * nối, writer được xóa khỏi connectedClients để tránh memory leak.
   */
  private static void handleClient(Socket socket) {
    PrintWriter out = null;
    try {
      BufferedReader in =
          new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
      out = new PrintWriter(socket.getOutputStream(), true);
      connectedClients.add(out); // Đăng ký nhận broadcast

      String line;
      while ((line = in.readLine()) != null) {
        String response = dispatch(line);
        if (response != null) out.println(response); // null = no response needed (e.g. PLACE_BID)
      }
    } catch (IOException e) {
      System.out.println("Client disconnected: " + e.getMessage());
    } finally {
      if (out != null) connectedClients.remove(out);
      try {
        socket.close();
      } catch (IOException ignored) {
      }
    }
  }

  // ─── DISPATCHER ─────────────────────────────────────────────────

  /** Phân phối request tới handler tương ứng theo trường "action". */
  private static String dispatch(String rawJson) {
    try {
      JSONObject req = new JSONObject(rawJson);
      String action = req.getString("action");
      System.out.println("ACTION: " + action);

      return switch (action) {
        case "LOGIN" -> handleLogin(req);
        case "REGISTER" -> handleRegister(req);
        case "GET_ITEM" -> handleGetItem(req);
        case "GET_BID_HISTORY" -> handleGetBidHistory(req);
        case "PLACE_BID" -> handlePlaceBid(req);
        case "CREATE_AUCTION" -> handleCreateAuction(req);
        case "GET_ALL_AUCTIONS" -> handleGetAllAuctions(req);
        case "GET_AUCTIONS_BY_SELLER" -> handleGetAuctionBySeller(req);
        case "DELETE_AUCTION" -> handleDeleteAuction(req);
        case "ADMIN_UPDATE_AUCTION" -> handleAdminUpdateAuction(req);
        default -> errorResponse("Invalid action: " + action);
      };
    } catch (Exception e) {
      System.err.println("Request processing error: " + e.getMessage());
      return errorResponse(e.getMessage());
    }
  }

  // HANDLERS
  private static String handleLogin(JSONObject req) {
    String username = req.getString("username");
    String password = req.getString("password");
    Optional<User> userOpt = userDao.findByUsername(username);
    if (userOpt.isEmpty()) return errorResponse("Incorrect username or password.");

    User user = userOpt.get();
    // Sử dụng PasswordUtil để kiểm tra mật khẩu đã được hash
    if (!com.auction.server.service.util.PasswordUtil.verifyPassword(password, user.getPasswordHash())) {
      return errorResponse("Incorrect username or password.");
    }

    JSONObject res = new JSONObject();
    res.put("status", "OK");
    res.put("id", user.getId());
    res.put("username", user.getUsername());
    res.put("email", user.getEmail());
    res.put("role", user.getRole().name()); // BIDDER | SELLER | ADMIN
    if (user instanceof Seller s) res.put("shopName", s.getShopName());
    if (user instanceof Bidder b) res.put("balance", b.getBalance());

    System.out.println("LOGIN OK: " + username + " (" + user.getRole() + ")");
    return res.toString();
  }

  private static String handleRegister(JSONObject req) {
    String username = req.getString("username");
    String password = req.getString("password");
    String email = req.getString("email");
    String role = req.getString("role");
    String shopName = req.getString("shopName");

    if (userDao.existsByUsername(username)) return errorResponse("Username already exists!");
    if (userDao.existsByEmail(email)) return errorResponse("Email has already been used!");

    String passwordHash = com.auction.server.service.util.PasswordUtil.hashPassword(password);
    User newUser =
        "SELLER".equals(role)
            ? new Seller(username, passwordHash, email, shopName)
            : new Bidder(username, passwordHash, email, 0L);
    userDao.save(newUser);
    System.out.println("REGISTER OK:" + username + " (" + role + ")");

    JSONObject res = new JSONObject();
    res.put("status", "OK");

    return res.toString();
  }

  private static String handleGetItem(JSONObject req) {
    Long itemId = req.getLong("itemId");
    Optional<Item> itemOpt = itemDao.findById(itemId);
    if (itemOpt.isEmpty()) return errorResponse("Product not found #" + itemId);

    JSONObject res = itemToJSON(itemOpt.get());
    res.put("status", "OK");
    return res.toString();
  }

  private static String handleGetBidHistory(JSONObject req) {
    Long auctionId = req.getLong("auctionId");
    List<BidTransaction> bids = bidTransactionDao.findByAuctionId(auctionId);

    JSONArray arr = new JSONArray();
    for (BidTransaction bid : bids) {
      arr.put(bidToJSON(bid));
    }

    JSONObject res = new JSONObject();
    res.put("status", "OK");
    res.put("bids", arr);
    return res.toString();
  }

  private static String handlePlaceBid(JSONObject req) {
    Long auctionId = req.getLong("auctionId");
    Long bidderId = req.getLong("bidderId");
    long amount = req.getLong("amount");

    try {
      // AuctionManager xử lí concurrency + anti-snipe và trả về auction đã cập nhập
      Auction auction = AuctionManager.getInstance().placeBid(auctionId, bidderId, amount);

      // Lưu BidTransaction vào Database
      BidTransaction bid = new BidTransaction(auctionId, bidderId, amount);
      bidTransactionDao.save(bid);

      // Đồng bộ auction xuống database
      auctionDao.update(auction);

      System.out.println(
          "BID: auction #" + auctionId + " | bidder #" + bidderId + " | " + amount + " VND");

      // Broadcast BID_UPDATE tới tất cả client
      JSONObject push = new JSONObject();
      push.put("type", "BID_UPDATE");
      push.put("bid", bidToJSON(bid));
      broadcast(push.toString());

      // Không trả về response — client dùng BID_UPDATE làm xác nhận
      return null;
    } catch (Exception e) {
      e.printStackTrace();
      System.out.println("BID REJECTED: " + e.getMessage());
      return errorResponse(e.getMessage());
    }
  }

  private static String handleCreateAuction(JSONObject req) {
    Long itemId = req.getLong("itemId");
    Long sellerId = req.getLong("sellerId");
    Long startingPrice = req.getLong("startingPrice");
    LocalDateTime startTime = LocalDateTime.parse(req.getString("startTime"));
    LocalDateTime endTime = LocalDateTime.parse(req.getString("endTime"));
    LocalDateTime now = LocalDateTime.now();

    // KIỂM TRA: Sản phẩm phải tồn tại mới cho tạo đấu giá
    if (itemDao.findById(itemId).isEmpty()) {
      return errorResponse("Sản phẩm #" + itemId + " không tồn tại!");
    }

    // KIỂM TRA: Thời gian bắt đầu không được ở quá khứ (Cho phép lệch 1 phút để tránh sai số đồng hồ)
    if (startTime.plusMinutes(1).isBefore(now)) {
      return errorResponse("Thời gian bắt đầu không được ở trong quá khứ!");
    }

    Auction auction = new Auction(itemId, sellerId, startTime, endTime);
    auction.setCurrentPrice(startingPrice);

    // TỰ ĐỘNG CHUYỂN TRẠNG THÁI: Nếu giờ tạo nằm trong khoảng bắt đầu-kết thúc
    if (now.isAfter(startTime) && now.isBefore(endTime)) {
      auction.setStatus(AuctionStatus.RUNNING);
    } else if (now.isAfter(endTime)) {
      auction.setStatus(AuctionStatus.FINISHED);
    } else {
      auction.setStatus(AuctionStatus.OPEN);
    }

    auctionDao.save(auction);

    // ĐƯA VÀO QUẢN LÝ: Nếu đang chạy thì đưa vào AuctionManager để cho phép đặt giá ngay
    if (auction.getStatus() == AuctionStatus.RUNNING) {
      AuctionManager.getInstance().restoreRunningAuction(auction);
    }

    System.out.println("CREATE_AUCTION: auction #" + auction.getId() + " | status: " + auction.getStatus());

    JSONObject res = new JSONObject();
    res.put("status", "OK");
    res.put("auctionId", auction.getId());
    return res.toString();
  }

  private static String handleDeleteAuction(JSONObject req) {
    Long auctionId = req.getLong("auctionId");
    
    // 1. Xóa trong RAM (nếu đang chạy)
    AuctionManager.getInstance().closeAuction(auctionId);
    
    // 2. Xóa trong Database
    boolean deleted = auctionDao.deleteById(auctionId);
    
    if (deleted) {
      System.out.println("ADMIN ACTION: Deleted auction #" + auctionId);
      JSONObject res = new JSONObject();
      res.put("status", "OK");
      return res.toString();
    } else {
      return errorResponse("Không tìm thấy phiên đấu giá #" + auctionId + " để xóa.");
    }
  }

  private static String handleAdminUpdateAuction(JSONObject req) {
    Long auctionId = req.getLong("auctionId");
    Long newPrice = req.getLong("currentPrice");
    AuctionStatus newStatus = AuctionStatus.valueOf(req.getString("status"));
    LocalDateTime newEndTime = LocalDateTime.parse(req.getString("endTime"));
    String newCategory = req.getString("category");

    Optional<Auction> auctionOpt = auctionDao.findById(auctionId);
    if (auctionOpt.isEmpty()) return errorResponse("Không tìm thấy đấu giá #" + auctionId);

    Auction auction = auctionOpt.get();
    
    // 1. Cập nhật Auction
    auction.setCurrentPrice(newPrice);
    auction.setStatus(newStatus);
    auction.setEndTime(newEndTime);
    auctionDao.update(auction);

    // 2. Cập nhật Item (Category)
    Optional<Item> itemOpt = itemDao.findById(auction.getItemId());
    if (itemOpt.isPresent()) {
      Item item = itemOpt.get();
      item.setCategory(com.auction.server.model.enums.ItemCategory.valueOf(newCategory));
      itemDao.update(item);
    }

    // 3. Đồng bộ RAM (AuctionManager)
    if (newStatus == AuctionStatus.RUNNING) {
      AuctionManager.getInstance().restoreRunningAuction(auction);
    } else {
      AuctionManager.getInstance().closeAuction(auctionId);
    }

    System.out.println("ADMIN ACTION: Updated auction #" + auctionId + " to price=" + newPrice + ", status=" + newStatus);
    
    JSONObject res = new JSONObject();
    res.put("status", "OK");
    return res.toString();
  }

  private static String handleGetAllAuctions(JSONObject req) {
    List<Auction> auctions = auctionDao.findAll();

    JSONArray arr = new JSONArray();
    for (Auction auction : auctions) {
      arr.put(auctionToJSON(auction));
    }

    JSONObject res = new JSONObject();
    res.put("status", "OK");
    res.put("auctions", arr);
    return res.toString();
  }

  private static String handleGetAuctionBySeller(JSONObject req) {
    Long sellerId = req.getLong("sellerId");
    List<Auction> auctions = auctionDao.findBySellerId(sellerId);

    JSONArray arr = new JSONArray();
    for (Auction auction : auctions) {
      arr.put(auctionToJSON(auction));
    }

    JSONObject res = new JSONObject();
    res.put("status", "OK");
    res.put("auctions", arr);
    return res.toString();
  }

  private static String errorResponse(String message) {
    JSONObject res = new JSONObject();
    res.put("status", "ERROR");
    res.put("message", message != null ? message : "Unknown error!");
    return res.toString();
  }

  // JSON Serializers
  // BidTransaction -> JSON
  private static JSONObject bidToJSON(BidTransaction bid) {
    JSONObject json = new JSONObject();
    json.put("id", bid.getId() != null ? bid.getId() : -1L);
    json.put("createdAt", bid.getCreatedAt().toString());
    json.put("auctionId", bid.getAuctionId());
    json.put("bidderId", bid.getBidderId());
    json.put("amount", bid.getAmount());
    json.put("timestamp", bid.getTimestamp().toString());
    return json;
  }

  // Auction -> JSON
  private static JSONObject auctionToJSON(Auction auction) {
    JSONObject json = new JSONObject();
    json.put("id", auction.getId());
    json.put("createdAt", auction.getCreatedAt().toString());
    json.put("itemId", auction.getItemId());
    
    // Thêm tên sản phẩm (có fallback nếu không tìm thấy)
    String name = itemDao.findById(auction.getItemId())
                        .map(item -> item.getName())
                        .orElse("Sản phẩm #" + auction.getItemId());
    json.put("itemName", name);
    json.put("sellerId", auction.getSellerId());
    json.put("currentPrice", auction.getCurrentPrice());
    json.put("currentWinnerId", auction.getCurrentWinnerId());
    json.put("status", auction.getStatus());
    json.put("startTime", auction.getStartTime().toString());
    json.put("endTime", auction.getEndTime().toString());
    return json;
  }

  public static JSONObject itemToJSON(Item item) {
    JSONObject json = new JSONObject();
    json.put("id", item.getId());
    json.put("createdAt", item.getCreatedAt().toString());
    json.put("name", item.getName());
    json.put("description", item.getDescription());
    json.put("startingPrice", item.getStartingPrice());
    json.put("sellerId", item.getSellerId());
    json.put("category", item.getCategory().name());

    if (item instanceof Electronics e) {
      json.put("brand", e.getBrand());
      json.put("warrantyMonths", e.getWarrantyMonths());
      json.put("powerWatts", e.getPowerWatts());
    } else if (item instanceof Artwork a) {
      json.put("artistName", a.getArtistName());
      json.put("yearCreated", a.getYearCreated());
      json.put("medium", a.getMedium());
    } else if (item instanceof Vehicle v) {
      json.put("manufacturer", v.getManufacturer());
      json.put("yearManufactured", v.getYearManufactured());
      json.put("mileageKm", v.getMileageKm());
      json.put("fuelType", v.getFuelType());
    }
    return json;
  }

  // BroadCast
  // ─── PHÁT TIN (BROADCAST) ───────────────────────────────────────

  /** Gửi tin nhắn tới toàn bộ client đang kết nối (Realtime update). */
  private static void broadcast(String message) {
    for (PrintWriter client : connectedClients) {
      try {
        client.println(message);
        client.flush(); // Ép gửi ngay lập tức
      } catch (Exception e) {
        System.err.println("Lỗi broadcast: " + e.getMessage());
      }
    }
  }
}
