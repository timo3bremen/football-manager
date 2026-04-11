package com.example.manager.service;

import com.example.manager.dto.BidRequest;
import com.example.manager.dto.CreateAuctionRequest;
import com.example.manager.model.Player;
import com.example.manager.model.Team;
import com.example.manager.model.TransferAuction;
import com.example.manager.repository.InMemoryRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Random;

@Service
public class ManagerService {

    private final InMemoryRepository repo = new InMemoryRepository();
    private final Random rnd = new Random();

    public List<Player> listPlayers() {
        return repo.listPlayers();
    }

    public Player getPlayer(long id) {
        return repo.getPlayer(id);
    }

    public List<Team> listTeams() {
        return repo.listTeams();
    }

    public List<TransferAuction> listAuctions() {
        return repo.listAuctions();
    }

    public String registerUser(String username, String password, String teamName) {
        // create team
        Team t = new Team(teamName, 100000);
        repo.saveTeam(t);
        return repo.registerUser(username, password, t.getId());
    }

    public String loginUser(String username, String password){
        return repo.authenticateUser(username, password);
    }

    public Long getTeamIdForToken(String token){
        return repo.getTeamIdForToken(token);
    }

    public Team getTeamById(long id){
        return repo.getTeam(id);
    }

    // simple game state persistence (stores JSON blob per team id)
    public void saveGameState(long teamId, String json) {
        repo.saveGameState(teamId, json);
    }

    public String getGameState(long teamId) {
        return repo.getGameState(teamId);
    }

    public void clearUsers(){
        repo.clearUsers();
    }

    public TransferAuction createAuction(CreateAuctionRequest req) {
        Player p = repo.getPlayer(req.getPlayerId());
        if (p == null) throw new IllegalArgumentException("player not found");
        Instant expires = Instant.now().plusSeconds(req.getDurationSeconds());
        return repo.createAuction(p, req.getSellerTeamId(), expires);
    }

    public TransferAuction placeBid(long auctionId, BidRequest req) {
        TransferAuction a = repo.getAuction(auctionId);
        if (a == null) throw new IllegalArgumentException("auction not found");
        Team bidder = repo.getTeam(req.getTeamId());
        if (bidder == null) throw new IllegalArgumentException("team not found");
        // simple budget check
        if (bidder.getBudget() < req.getAmount()) throw new IllegalArgumentException("insufficient budget");
        a.addBid(new TransferAuction.Bid(req.getTeamId(), req.getAmount(), Instant.now()));
        return a;
    }

    // Scheduled tick that simulates CPU bidding and processes expiries
    @Scheduled(fixedRateString = "PT5S")
    public void scheduledTick() {
        try {
            // cpu bidding
            cpuBidding();
            // process expired auctions
            processExpiredAuctions();
        } catch (Exception e) {
            // swallow for scheduled job
            e.printStackTrace();
        }
    }

    private void cpuBidding() {
        List<TransferAuction> auctions = repo.listAuctions();
        for (TransferAuction a : auctions) {
            // skip auctions that are about to expire (less than 2s)
            if (a.getExpiresAt().isBefore(Instant.now().plusSeconds(2))) continue;
            // pick a random CPU team (not seller)
            List<Team> teams = repo.listTeams();
            if (teams.isEmpty()) continue;
            Team cpu = teams.get(rnd.nextInt(teams.size()));
            if (cpu.getId() == a.getSellerTeamId()) continue;
            // decide a max willingness based on player rating+potential
            int willingness = Math.min(cpu.getBudget(), a.getPlayer().getRating() + a.getPlayer().getPotential() / 2 + rnd.nextInt(50));
            TransferAuction.Bid highest = a.getHighestBid();
            int current = highest == null ? 0 : highest.amount;
            int increment = 1000 + rnd.nextInt(10000);
            int bidAmount = Math.min(willingness, current + increment);
            if (bidAmount > current && bidAmount <= cpu.getBudget()) {
                a.addBid(new TransferAuction.Bid(cpu.getId(), bidAmount, Instant.now()));
            }
        }
    }

    public void processExpiredAuctions() {
        List<TransferAuction> auctions = repo.listAuctions();
        Instant now = Instant.now();
        for (TransferAuction a : auctions) {
            if (a.getExpiresAt().isBefore(now) || a.getExpiresAt().equals(now)) {
                TransferAuction.Bid winner = a.getHighestBid();
                if (winner != null) {
                    Team buyer = repo.getTeam(winner.bidderTeamId);
                    Team seller = repo.getTeam(a.getSellerTeamId());
                    if (buyer != null && buyer.getBudget() >= winner.amount) {
                        // transfer player
                        buyer.addPlayer(a.getPlayer());
                        buyer.setBudget(buyer.getBudget() - winner.amount);
                        if (seller != null) {
                            seller.setBudget(seller.getBudget() + winner.amount);
                        }
                    } else {
                        // if buyer invalid, return player to seller
                        if (seller != null) seller.addPlayer(a.getPlayer());
                    }
                } else {
                    // no bids: return player to seller
                    Team seller = repo.getTeam(a.getSellerTeamId());
                    if (seller != null) seller.addPlayer(a.getPlayer());
                }
                repo.removeAuction(a.getId());
            }
        }
    }

    // simple growth simulation: apply small increases based on potential
    public void simulateTrainingTick() {
        for (Player p : repo.listPlayers()) {
            int growthChance = rnd.nextInt(100);
            if (growthChance < Math.max(1, p.getPotential() / 10)) {
                int delta = 1 + rnd.nextInt(3);
                p.setRating(Math.min(100, p.getRating() + delta));
            }
            // random form fluctuations
            int formDelta = rnd.nextInt(3) - 1; // -1,0,1
            p.setForm(Math.max(-10, Math.min(10, p.getForm() + formDelta)));
        }
    }
}
