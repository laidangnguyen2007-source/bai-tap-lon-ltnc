package com.auction.server.handler;

import com.auction.server.dao.AuctionDao;
import com.auction.server.dao.BidTransactionDao;
import com.auction.server.dao.ItemDao;
import com.auction.server.model.entity.Auction;
import com.auction.server.model.entity.BidTransaction;
import com.auction.server.model.entity.item.Item;
import com.auction.server.net.EntityJsonMapper;
import com.auction.server.net.JsonResponses;
import com.auction.server.service.AuctionStatusSynchronizer;
import java.util.List;
import java.util.Optional;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Handler <b>đọc</b> dữ liệu phục vụ UI: sản phẩm, lịch sử bid, danh sách phiên đấu giá.
 *
 * <p>Tách riêng khỏi handler <b>ghi</b> ({@link BiddingHandlers}, {@link AuctionCommandHandlers})
 * để đúng SRP và để nhóm dễ tìm code khi sửa màn hình danh sách / chi tiết.
 */
public final class CatalogHandlers {

  private final ItemDao itemDao;
  private final AuctionDao auctionDao;
  private final BidTransactionDao bidTransactionDao;
  private final EntityJsonMapper jsonMapper;

  public CatalogHandlers(
      ItemDao itemDao,
      AuctionDao auctionDao,
      BidTransactionDao bidTransactionDao,
      EntityJsonMapper jsonMapper) {
    this.itemDao = itemDao;
    this.auctionDao = auctionDao;
    this.bidTransactionDao = bidTransactionDao;
    this.jsonMapper = jsonMapper;
  }

  public String getItem(JSONObject req) throws Exception {
    Long itemId = req.getLong("itemId");
    Optional<Item> itemOpt = itemDao.findById(itemId);
    if (itemOpt.isEmpty()) {
      return JsonResponses.error("Product not found #" + itemId);
    }
    JSONObject res = jsonMapper.itemToJSON(itemOpt.get());
    res.put("status", "OK");
    return res.toString();
  }

  public String getBidHistory(JSONObject req) throws Exception {
    Long auctionId = req.getLong("auctionId");
    List<BidTransaction> bids = bidTransactionDao.findByAuctionId(auctionId);
    JSONArray arr = new JSONArray();
    for (BidTransaction bid : bids) {
      arr.put(jsonMapper.bidToJSON(bid));
    }
    JSONObject res = new JSONObject();
    res.put("status", "OK");
    res.put("bids", arr);
    return res.toString();
  }

  public String getAllAuctions(JSONObject req) throws Exception {
    AuctionStatusSynchronizer.syncWithClock(auctionDao);
    List<Auction> auctions = auctionDao.findAll();
    JSONArray arr = new JSONArray();
    for (Auction auction : auctions) {
      arr.put(jsonMapper.auctionToJSON(auction));
    }
    JSONObject res = new JSONObject();
    res.put("status", "OK");
    res.put("auctions", arr);
    return res.toString();
  }

  public String getAuctionsBySeller(JSONObject req) throws Exception {
    AuctionStatusSynchronizer.syncWithClock(auctionDao);
    Long sellerId = req.getLong("sellerId");
    List<Auction> auctions = auctionDao.findBySellerId(sellerId);
    JSONArray arr = new JSONArray();
    for (Auction auction : auctions) {
      arr.put(jsonMapper.auctionToJSON(auction));
    }
    JSONObject res = new JSONObject();
    res.put("status", "OK");
    res.put("auctions", arr);
    return res.toString();
  }
}
