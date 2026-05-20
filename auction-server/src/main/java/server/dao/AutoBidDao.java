package server.dao;

import java.util.List;
import java.util.Optional;
import server.model.entity.AutoBid;

public interface AutoBidDao extends GenericDao<AutoBid> {

  Optional<AutoBid> findByAuctionAndBidder(Long auctionId, Long bidderId);

  List<AutoBid> findActiveByAuction(Long auctionId);

  List<AutoBid> findActiveByBidder(Long bidderId);

  void deactivate(Long id);

  void deactivateByAuction(Long auctionId);
}