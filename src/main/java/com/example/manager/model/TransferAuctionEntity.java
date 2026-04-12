package com.example.manager.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * JPA Entity for TransferAuction - stores auctions in DB.
 */
@Entity
@Table(name = "transfer_auctions")
public class TransferAuctionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "playerId")
    private Long playerId;

    @Column(name = "sellerTeamId")
    private Long sellerTeamId;

    @Column(name = "expiresAt")
    private Instant expiresAt;

    @Column(name = "highestBid")
    private Integer highestBid;

    @Column(name = "highestBidderId")
    private Long highestBidderId;

    public TransferAuctionEntity() {
    }

    public TransferAuctionEntity(Long playerId, Long sellerTeamId, Instant expiresAt) {
        this.playerId = playerId;
        this.sellerTeamId = sellerTeamId;
        this.expiresAt = expiresAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPlayerId() {
        return playerId;
    }

    public void setPlayerId(Long playerId) {
        this.playerId = playerId;
    }

    public Long getSellerTeamId() {
        return sellerTeamId;
    }

    public void setSellerTeamId(Long sellerTeamId) {
        this.sellerTeamId = sellerTeamId;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Integer getHighestBid() {
        return highestBid;
    }

    public void setHighestBid(Integer highestBid) {
        this.highestBid = highestBid;
    }

    public Long getHighestBidderId() {
        return highestBidderId;
    }

    public void setHighestBidderId(Long highestBidderId) {
        this.highestBidderId = highestBidderId;
    }
}
