package com.example.manager.repository;

import com.example.manager.model.AuctionPlayer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AuctionPlayerRepository extends JpaRepository<AuctionPlayer, Long> {
    List<AuctionPlayer> findByAuctionStatus(String status);
    List<AuctionPlayer> findByPlayerId(Long playerId);
    Optional<AuctionPlayer> findByPlayerIdAndAuctionStatus(Long playerId, String status);
}
