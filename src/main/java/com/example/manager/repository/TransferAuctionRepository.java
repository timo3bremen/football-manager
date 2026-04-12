package com.example.manager.repository;

import com.example.manager.model.TransferAuctionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface TransferAuctionRepository extends JpaRepository<TransferAuctionEntity, Long> {
    
    List<TransferAuctionEntity> findByExpiresAtBefore(Instant time);
    
    List<TransferAuctionEntity> findByExpiresAtAfter(Instant time);
}
