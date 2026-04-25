package com.example.manager.model;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Represents a player available in the daily auction.
 * One player is available for 24 hours starting from the daily auction refresh time (22:00).
 */
@Entity
@Table(name = "auction_players")
public class AuctionPlayer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "playerId", nullable = false)
    private Long playerId;

    @Column(name = "playerName", nullable = false)
    private String playerName;

    @Column(name = "position", nullable = false)
    private String position;

    @Column(name = "rating", nullable = false)
    private int rating;

    @Column(name = "age", nullable = false)
    private int age;

    @Column(name = "marketValue", nullable = false)
    private long marketValue;

    @Column(name = "salary", nullable = false)
    private long salary;

    @Column(name = "contractLength", nullable = false)
    private int contractLength;

    @Column(name = "country")
    private String country;

    @Column(name = "auctionStartTime", nullable = false)
    private Instant auctionStartTime;

    @Column(name = "auctionEndTime", nullable = false)
    private Instant auctionEndTime;

    @Column(name = "highestBidAmount")
    private Long highestBidAmount;

    @Column(name = "highestBidderTeamId")
    private Long highestBidderTeamId;

    @Column(name = "highestBidderTeamName")
    private String highestBidderTeamName;

    @Column(name = "auctionStatus", nullable = false)
    private String auctionStatus = "active"; // active, completed, failed

    @Column(name = "winnerTeamId")
    private Long winnerTeamId;

    @Column(name = "winnerTeamName")
    private String winnerTeamName;

    @Column(name = "potentialRating")
    private int potentialRating;

    @Column(name = "expiresAt", nullable = false)
    private long expiresAt; // Unix timestamp (milliseconds) - 24h nach Erstellung

    public AuctionPlayer() {
    }

    public AuctionPlayer(Long playerId, String playerName, String position, int rating, int age,
                         long marketValue, long salary, int contractLength, String country,
                         Instant auctionStartTime, Instant auctionEndTime, int potentialRating) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.position = position;
        this.rating = rating;
        this.age = age;
        this.marketValue = marketValue;
        this.salary = salary;
        this.contractLength = contractLength;
        this.country = country;
        this.auctionStartTime = auctionStartTime;
        this.auctionEndTime = auctionEndTime;
        this.auctionStatus = "active";
        this.potentialRating = potentialRating;
        // Ablaufdatum: 24 Stunden nach jetzt (in Millisekunden)
        this.expiresAt = System.currentTimeMillis() + (24 * 60 * 60 * 1000);
    }

    // Getters and Setters
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

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public long getMarketValue() {
        return marketValue;
    }

    public void setMarketValue(long marketValue) {
        this.marketValue = marketValue;
    }

    public long getSalary() {
        return salary;
    }

    public void setSalary(long salary) {
        this.salary = salary;
    }

    public int getContractLength() {
        return contractLength;
    }

    public void setContractLength(int contractLength) {
        this.contractLength = contractLength;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public Instant getAuctionStartTime() {
        return auctionStartTime;
    }

    public void setAuctionStartTime(Instant auctionStartTime) {
        this.auctionStartTime = auctionStartTime;
    }

    public Instant getAuctionEndTime() {
        return auctionEndTime;
    }

    public void setAuctionEndTime(Instant auctionEndTime) {
        this.auctionEndTime = auctionEndTime;
    }

    public Long getHighestBidAmount() {
        return highestBidAmount;
    }

    public void setHighestBidAmount(Long highestBidAmount) {
        this.highestBidAmount = highestBidAmount;
    }

    public Long getHighestBidderTeamId() {
        return highestBidderTeamId;
    }

    public void setHighestBidderTeamId(Long highestBidderTeamId) {
        this.highestBidderTeamId = highestBidderTeamId;
    }

    public String getHighestBidderTeamName() {
        return highestBidderTeamName;
    }

    public void setHighestBidderTeamName(String highestBidderTeamName) {
        this.highestBidderTeamName = highestBidderTeamName;
    }

    public String getAuctionStatus() {
        return auctionStatus;
    }

    public void setAuctionStatus(String auctionStatus) {
        this.auctionStatus = auctionStatus;
    }

    public Long getWinnerTeamId() {
        return winnerTeamId;
    }

    public void setWinnerTeamId(Long winnerTeamId) {
        this.winnerTeamId = winnerTeamId;
    }

    public String getWinnerTeamName() {
        return winnerTeamName;
    }

    public void setWinnerTeamName(String winnerTeamName) {
        this.winnerTeamName = winnerTeamName;
    }

    public int getPotentialRating() {
        return potentialRating;
    }

    public void setPotentialRating(int potentialRating) {
        this.potentialRating = potentialRating;
    }

    public long getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(long expiresAt) {
        this.expiresAt = expiresAt;
    }
}
