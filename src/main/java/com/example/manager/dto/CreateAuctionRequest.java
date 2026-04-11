package com.example.manager.dto;

public class CreateAuctionRequest {
    private long playerId;
    private long sellerTeamId;
    // duration seconds
    private long durationSeconds = 60;

    public long getPlayerId() {
        return playerId;
    }

    public void setPlayerId(long playerId) {
        this.playerId = playerId;
    }

    public long getSellerTeamId() {
        return sellerTeamId;
    }

    public void setSellerTeamId(long sellerTeamId) {
        this.sellerTeamId = sellerTeamId;
    }

    public long getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(long durationSeconds) {
        this.durationSeconds = durationSeconds;
    }
}
