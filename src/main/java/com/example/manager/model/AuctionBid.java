package com.example.manager.model;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Represents a bid placed on an auction player.
 */
@Entity
@Table(name = "auction_bids")
public class AuctionBid {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "auctionPlayerId", nullable = false)
    private Long auctionPlayerId;

    @Column(name = "biddingTeamId", nullable = false)
    private Long biddingTeamId;

    @Column(name = "biddingTeamName", nullable = false)
    private String biddingTeamName;

    @Column(name = "bidAmount", nullable = false)
    private Long bidAmount;

    @Column(name = "bidTime", nullable = false)
    private Instant bidTime;

    @Column(name = "isCPUBid", nullable = false)
    private boolean isCPUBid = false;

    public AuctionBid() {
    }

    public AuctionBid(Long auctionPlayerId, Long biddingTeamId, String biddingTeamName,
                      Long bidAmount, Instant bidTime, boolean isCPUBid) {
        this.auctionPlayerId = auctionPlayerId;
        this.biddingTeamId = biddingTeamId;
        this.biddingTeamName = biddingTeamName;
        this.bidAmount = bidAmount;
        this.bidTime = bidTime;
        this.isCPUBid = isCPUBid;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getAuctionPlayerId() {
        return auctionPlayerId;
    }

    public void setAuctionPlayerId(Long auctionPlayerId) {
        this.auctionPlayerId = auctionPlayerId;
    }

    public Long getBiddingTeamId() {
        return biddingTeamId;
    }

    public void setBiddingTeamId(Long biddingTeamId) {
        this.biddingTeamId = biddingTeamId;
    }

    public String getBiddingTeamName() {
        return biddingTeamName;
    }

    public void setBiddingTeamName(String biddingTeamName) {
        this.biddingTeamName = biddingTeamName;
    }

    public Long getBidAmount() {
        return bidAmount;
    }

    public void setBidAmount(Long bidAmount) {
        this.bidAmount = bidAmount;
    }

    public Instant getBidTime() {
        return bidTime;
    }

    public void setBidTime(Instant bidTime) {
        this.bidTime = bidTime;
    }

    public boolean isCPUBid() {
        return isCPUBid;
    }

    public void setCPUBid(boolean cpuBid) {
        isCPUBid = cpuBid;
    }
}
