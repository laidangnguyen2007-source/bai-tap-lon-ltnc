package com.auction.server;

import com.auction.server.config.DatabaseConfig;
import com.auction.server.dao.AuctionDao;
import com.auction.server.dao.BidTransactionDao;
import com.auction.server.dao.ItemDao;
import com.auction.server.dao.UserDao;
import com.auction.server.dao.impl.JdbcAuctionDao;
import com.auction.server.dao.impl.JdbcBidTransactionDao;
import com.auction.server.dao.impl.JdbcItemDao;
import com.auction.server.dao.impl.JdbcUserDao;
import com.auction.server.handler.AuctionCommandHandlers;
import com.auction.server.handler.AuthHandlers;
import com.auction.server.handler.BiddingHandlers;
import com.auction.server.handler.CatalogHandlers;
import com.auction.server.handler.RequestRouter;
import com.auction.server.model.entity.Auction;
import com.auction.server.net.ClientBroadcaster;
import com.auction.server.net.ClientSession;
import com.auction.server.net.EntityJsonMapper;
import com.auction.server.service.AuctionManager;
import com.auction.server.service.AuctionStatusSynchronizer;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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

    try {
      userDao = new JdbcUserDao();
      auctionDao = new JdbcAuctionDao();
      itemDao = new JdbcItemDao();
      bidTransactionDao = new JdbcBidTransactionDao();
      System.out.println("MySQL connection successful — auction_db is ready.");

      AuctionStatusSynchronizer.syncWithClock(auctionDao);
      List<Auction> runningAuctions = auctionDao.findRunningAuctions();
      for (Auction a : runningAuctions) {
        AuctionManager.getInstance().restoreRunningAuction(a);
      }
      System.out.println("Loaded " + runningAuctions.size() + " running auctions into memory.");
    } catch (Exception e) {
      System.err.println("Unable to connect to MySql: " + e.getMessage());
      System.err.println("Please make sure XAMPP MySQL is running on port 3306.");
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

    AuthHandlers authHandlers = new AuthHandlers(userDao);
    CatalogHandlers catalogHandlers =
        new CatalogHandlers(itemDao, auctionDao, bidTransactionDao, jsonMapper);
    BiddingHandlers biddingHandlers =
        new BiddingHandlers(auctionDao, bidTransactionDao, jsonMapper, broadcaster);
    AuctionCommandHandlers commandHandlers =
        new AuctionCommandHandlers(auctionDao, itemDao, bidTransactionDao, broadcaster);

    RequestRouter router =
        RequestRouter.createAuctionRouter(
            authHandlers, catalogHandlers, biddingHandlers, commandHandlers);

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
