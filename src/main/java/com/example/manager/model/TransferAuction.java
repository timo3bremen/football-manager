package com.example.manager.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Represents an auction for a player. Bids are simple entries with bidderId and amount.
 */
public class TransferAuction {

    private static final AtomicLong ID_GEN = new AtomicLong(1);

    public static class Bid {
        public final long bidderTeamId;
        public final int amount;
        public final Instant time;

        public Bid(long bidderTeamId, int amount, Instant time) {
            this.bidderTeamId = bidderTeamId;
            this.amount = amount;
            this.time = time;
        }
    }

    private final long id;
    private final Player player;
    private final long sellerTeamId;
    private final Instant expiresAt;
    private final List<Bid> bids = new ArrayList<>();

    public TransferAuction(Player player, long sellerTeamId, Instant expiresAt) {
        this.id = ID_GEN.getAndIncrement();
        this.player = player;
        this.sellerTeamId = sellerTeamId;
        this.expiresAt = expiresAt;
    }

    public long getId() {
        return id;
    }

    public Player getPlayer() {
        return player;
    }

    public long getSellerTeamId() {
        return sellerTeamId;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public List<Bid> getBids() {
        return bids;
    }

    public void addBid(Bid bid) {
        bids.add(bid);
    }

    public Bid getHighestBid() {
        return bids.stream().max((a, b) -> Integer.compare(a.amount, b.amount)).orElse(null);
    }
}
