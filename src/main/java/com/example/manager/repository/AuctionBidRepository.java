package com.example.manager.repository;

import com.example.manager.model.AuctionBid;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuctionBidRepository extends JpaRepository<AuctionBid, Long> {
    List<AuctionBid> findByAuctionPlayerId(Long auctionPlayerId);
    List<AuctionBid> findByBiddingTeamId(Long biddingTeamId);
    List<AuctionBid> findByAuctionPlayerIdOrderByBidAmountDesc(Long auctionPlayerId);
}
