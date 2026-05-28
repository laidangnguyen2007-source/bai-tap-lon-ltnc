package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import server.config.DatabaseConfig;
import server.dao.AuctionDao;
import server.dao.AutoBidDao;
import server.dao.BidTransactionDao;
import server.dao.ItemDao;
import server.dao.UserDao;
import server.dao.WalletDao;
import server.dao.WalletTransactionDao;
import server.dao.impl.JdbcAuctionDao;
import server.dao.impl.JdbcAutoBidDao;
import server.dao.impl.JdbcBidTransactionDao;
import server.dao.impl.JdbcItemDao;
import server.dao.impl.JdbcUserDao;
import server.dao.impl.JdbcWalletDao;
import server.dao.impl.JdbcWalletTransactionDao;
import server.handler.AuctionCommandHandlers;
import server.handler.AuthHandlers;
import server.handler.BiddingHandlers;
import server.handler.CatalogHandlers;
import server.handler.RequestRouter;
import server.handler.WalletHandlers;
import server.model.entity.Auction;
import server.net.ClientBroadcaster;
import server.net.ClientSession;
import server.net.EntityJsonMapper;
import server.service.AuctionManager;
import server.service.AuctionStatusSynchronizer;
import server.service.WalletService;

/** Điểm vào (entry point) của backend đấu giá — <b>composition root</b> theo ngôn ngữ DI. */
public final class Server {

  private static final int PORT = 8888;

  private Server() {}

  public static void main(String[] args) {
    System.out.println("═══════════════════════════════════════");
    System.out.println("   AUCTION SERVER — Starting...");
    System.out.println("═══════════════════════════════════════");

    UserDao userDao;
    AuctionDao auctionDao;
    ItemDao itemDao;
    BidTransactionDao bidTransactionDao;
    AutoBidDao autoBidDao;
    WalletDao walletDao;
    WalletTransactionDao walletTransactionDao;
    WalletService walletService = null;

    try {
      userDao = new JdbcUserDao();
      auctionDao = new JdbcAuctionDao();
      itemDao = new JdbcItemDao();
      bidTransactionDao = new JdbcBidTransactionDao();
      autoBidDao = new JdbcAutoBidDao();
      walletDao = new JdbcWalletDao();
      walletTransactionDao = new JdbcWalletTransactionDao();
      walletService = new WalletService(walletDao, walletTransactionDao);
      System.out.println("MySQL connection successful — auction_db is ready.");

      List<server.model.entity.user.User> allUsers = userDao.findAll();
      for (server.model.entity.user.User u : allUsers) {
        if (walletDao.findByUserIdNoLock(u.getId()) == null) {
          walletService.createWallet(u.getId());
          System.out.println(
              " - Created wallet for user: " + u.getUsername() + " (ID: " + u.getId() + ")");
        }
      }

      AuctionStatusSynchronizer.syncWithClock(auctionDao, walletService, null);
      List<Auction> runningAuctions = auctionDao.findRunningAuctions();
      for (Auction a : runningAuctions) {
        AuctionManager.getInstance().restoreRunningAuction(a);
      }
      System.out.println("Loaded " + runningAuctions.size() + " running auctions into memory.");
    } catch (Exception e) {
      System.err.println("Unable to connect to MySql: " + e.getMessage());
      System.err.println("Please make sure XAMPP MySQL is running on port 3306.");
      e.printStackTrace();
      System.exit(1);
      return;
    }

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

    ClientBroadcaster broadcaster = new ClientBroadcaster();
    EntityJsonMapper jsonMapper = new EntityJsonMapper(itemDao);

    AuthHandlers authHandlers = new AuthHandlers(userDao, walletService);
    CatalogHandlers catalogHandlers =
        new CatalogHandlers(itemDao, auctionDao, bidTransactionDao, jsonMapper, walletService);
    BiddingHandlers biddingHandlers =
        new BiddingHandlers(
            auctionDao, bidTransactionDao, jsonMapper, broadcaster, walletService, autoBidDao);
    AuctionCommandHandlers commandHandlers =
        new AuctionCommandHandlers(auctionDao, itemDao, bidTransactionDao, broadcaster);
    WalletHandlers walletHandlers = new WalletHandlers(walletService, walletDao, broadcaster);

    RequestRouter router =
        RequestRouter.createAuctionRouter(
            authHandlers, catalogHandlers, biddingHandlers, commandHandlers, walletHandlers);

    ExecutorService threadPool = Executors.newCachedThreadPool();
    try (ServerSocket serverSocket = new ServerSocket(PORT)) {
      System.out.println("Server is listening on port " + PORT);
      System.out.println("═══════════════════════════════════════\n");

      while (!serverSocket.isClosed()) {
        Socket clientSocket = serverSocket.accept();
        System.out.println("New Client: " + clientSocket.getInetAddress());
        threadPool.submit(new ClientSession(clientSocket, router, broadcaster));
      }
    } catch (IOException e) {
      System.err.println("ServerSocket error: " + e.getMessage());
    } finally {
      threadPool.shutdown();
    }
  }
}
