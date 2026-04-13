package com.example.manager.service;

import com.example.manager.dto.BidRequest;
import com.example.manager.dto.CreateAuctionRequest;
import com.example.manager.dto.LeagueInfoDTO;
import com.example.manager.dto.TeamDetailsDTO;
import com.example.manager.model.Player;
import com.example.manager.model.Team;
import com.example.manager.model.TransferAuction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ManagerService {

    @Autowired
    private RepositoryService repositoryService;
    
    // Temporary in-memory auction storage (TODO: migrate to DB via TransferAuctionRepository)
    private final Map<Long, TransferAuction> auctions = new ConcurrentHashMap<>();
    private final Random rnd = new Random();

    public List<Player> listPlayers() {
        return repositoryService.listPlayers();
    }

    public Player getPlayer(long id) {
        return repositoryService.getPlayer(id);
    }

    public List<Team> listTeams() {
        return repositoryService.listTeams();
    }

    public List<TransferAuction> listAuctions() {
        return new ArrayList<>(auctions.values());
    }

    public String registerUser(String username, String password, String teamName) {
        Team t = new Team(teamName, 100000);
        t = repositoryService.saveTeam(t);
        return repositoryService.registerUser(username, password, t.getId());
    }

    public String registerUserWithLeague(String username, String password, String teamName, Long leagueId) {
        return repositoryService.registerUserWithLeague(username, password, teamName, leagueId);
    }

    public List<LeagueInfoDTO> getAvailableLeagues() {
        return repositoryService.getAvailableLeagues();
    }

    public void initializeLigues() {
        repositoryService.initializeLigues();
    }

    public String loginUser(String username, String password){
        return repositoryService.authenticateUser(username, password);
    }

    public Long getTeamIdForToken(String token){
        return repositoryService.getTeamIdForToken(token);
    }

    public Team getTeamById(long id){
        return repositoryService.getTeam(id);
    }

    public Team saveTeam(Team team){
        return repositoryService.saveTeam(team);
    }

    public Team updateTeamName(Long teamId, String newName){
        return repositoryService.updateTeamName(teamId, newName);
    }

    public TeamDetailsDTO getTeamDetails(Long teamId){
        return repositoryService.getTeamDetails(teamId);
    }

    public void saveGameState(long teamId, String json) {
        repositoryService.saveGameState(teamId, json);
    }

    public String getGameState(long teamId) {
        return repositoryService.getGameState(teamId);
    }

    public void clearUsers(){
        repositoryService.clearUsers();
    }

    public TransferAuction createAuction(CreateAuctionRequest req) {
        Player p = repositoryService.getPlayer(req.getPlayerId());
        if (p == null) throw new IllegalArgumentException("player not found");
        Instant expires = Instant.now().plusSeconds(req.getDurationSeconds());
        
        TransferAuction auction = new TransferAuction(p, req.getSellerTeamId(), expires);
        auctions.put(auction.getId(), auction);
        return auction;
    }

    public TransferAuction placeBid(long auctionId, BidRequest req) {
        TransferAuction a = auctions.get(auctionId);
        if (a == null) throw new IllegalArgumentException("auction not found");
        Team bidder = repositoryService.getTeam(req.getTeamId());
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
        List<TransferAuction> auctionList = new ArrayList<>(auctions.values());
        for (TransferAuction a : auctionList) {
            // skip auctions that are about to expire (less than 2s)
            if (a.getExpiresAt().isBefore(Instant.now().plusSeconds(2))) continue;
            // pick a random CPU team (not seller)
            List<Team> teams = repositoryService.listTeams();
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
        List<TransferAuction> auctionList = new ArrayList<>(auctions.values());
        Instant now = Instant.now();
        for (TransferAuction a : auctionList) {
            if (a.getExpiresAt().isBefore(now) || a.getExpiresAt().equals(now)) {
                TransferAuction.Bid winner = a.getHighestBid();
                if (winner != null) {
                    Team buyer = repositoryService.getTeam(winner.bidderTeamId);
                    Team seller = repositoryService.getTeam(a.getSellerTeamId());
                    if (buyer != null && buyer.getBudget() >= winner.amount) {
                        // transfer player
                        buyer.addPlayer(a.getPlayer());
                        buyer.setBudget(buyer.getBudget() - winner.amount);
                        repositoryService.saveTeam(buyer);
                        if (seller != null) {
                            seller.setBudget(seller.getBudget() + winner.amount);
                            repositoryService.saveTeam(seller);
                        }
                    } else {
                        // if buyer invalid, return player to seller
                        if (seller != null) {
                            seller.addPlayer(a.getPlayer());
                            repositoryService.saveTeam(seller);
                        }
                    }
                } else {
                    // no bids: return player to seller
                    Team seller = repositoryService.getTeam(a.getSellerTeamId());
                    if (seller != null) {
                        seller.addPlayer(a.getPlayer());
                        repositoryService.saveTeam(seller);
                    }
                }
                auctions.remove(a.getId());
            }
        }
    }

    // simple growth simulation: apply small increases based on potential
    public void simulateTrainingTick() {
        for (Player p : repositoryService.listPlayers()) {
            int growthChance = rnd.nextInt(100);
            if (growthChance < Math.max(1, p.getPotential() / 10)) {
                int delta = 1 + rnd.nextInt(3);
                p.setRating(Math.min(100, p.getRating() + delta));
            }
            // random form fluctuations
            int formDelta = rnd.nextInt(3) - 1; // -1,0,1
            p.setForm(Math.max(-10, Math.min(10, p.getForm() + formDelta)));
            repositoryService.savePlayer(p);
        }
    }
}