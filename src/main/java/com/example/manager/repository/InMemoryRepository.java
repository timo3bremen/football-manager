package com.example.manager.repository;

import com.example.manager.model.Player;
import com.example.manager.model.Team;
import com.example.manager.model.TransferAuction;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;
import java.nio.file.Path;
import java.nio.file.Files;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Very small in-memory repository for initial development and testing.
 */
public class InMemoryRepository {

    private final Map<Long, Player> players = new ConcurrentHashMap<>();
    private final Map<Long, Team> teams = new ConcurrentHashMap<>();
    private final Map<Long, TransferAuction> auctions = new ConcurrentHashMap<>();
    private final Map<Long, String> gameStates = new ConcurrentHashMap<>();

    public InMemoryRepository() {
        // seed some teams and players for a quick start
        Team t1 = new Team("FC Alpha", 1000000);
        Team t2 = new Team("SV Beta", 800000);
        Team t3 = new Team("United Gamma", 500000);
        teams.put(t1.getId(), t1);
        teams.put(t2.getId(), t2);
        teams.put(t3.getId(), t3);

        Player p1 = new Player("Max Mustermann", 65, 75, 0);
        Player p2 = new Player("Lukas Klein", 55, 80, 2);
        Player p3 = new Player("Erik Gross", 72, 70, -1);
        players.put(p1.getId(), p1);
        players.put(p2.getId(), p2);
        players.put(p3.getId(), p3);

        t1.addPlayer(p1);
        t2.addPlayer(p2);
        t3.addPlayer(p3);
    }

    // --- simple persistent users and game state storage (file-based)
    private final Path storageDir = Path.of("data") ;
    private final Path usersFile = storageDir.resolve("users.json");
    private final Path statesFile = storageDir.resolve("gamestates.json");
    private final ObjectMapper mapper = new ObjectMapper();
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final Map<String, User> users = new ConcurrentHashMap<>(); // username -> User
    private final Map<String, String> sessions = new ConcurrentHashMap<>(); // token -> username

    private static class User {
        public String username;
        public String passwordHash;
        public long teamId;
    }

    // load persisted files if present
    {
        try{
            if (!Files.exists(storageDir)) Files.createDirectories(storageDir);
            if (Files.exists(usersFile)){
                User[] arr = mapper.readValue(usersFile.toFile(), User[].class);
                for (User u: arr) users.put(u.username, u);
            }
            if (Files.exists(statesFile)){
                // load game states into gameStates map
                Map<String,String> saved = mapper.readValue(statesFile.toFile(), Map.class);
                for (Map.Entry<String,String> e: saved.entrySet()){
                    try{ gameStates.put(Long.parseLong(e.getKey()), e.getValue()); }catch(Exception ex){}
                }
            }
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    private synchronized void persistUsers(){
        try{ mapper.writeValue(usersFile.toFile(), users.values()); }catch(Exception e){ e.printStackTrace(); }
    }

    private synchronized void persistGameStates(){
        try{
            Map<String,String> out = new ConcurrentHashMap<>();
            for (Map.Entry<Long,String> e: gameStates.entrySet()) out.put(String.valueOf(e.getKey()), e.getValue());
            mapper.writeValue(statesFile.toFile(), out);
        }catch(Exception e){ e.printStackTrace(); }
    }

    public String registerUser(String username, String password, long teamId){
        if (users.containsKey(username)) throw new IllegalArgumentException("user exists");
        User u = new User(); u.username = username; u.passwordHash = passwordEncoder.encode(password); u.teamId = teamId;
        users.put(username, u);
        persistUsers();
        // create session token
        String token = UUID.randomUUID().toString();
        sessions.put(token, username);
        return token;
    }

    public String authenticateUser(String username, String password){
        User u = users.get(username);
        if (u == null) throw new IllegalArgumentException("user not found");
        if (!passwordEncoder.matches(password, u.passwordHash)) throw new IllegalArgumentException("invalid credentials");
        String token = UUID.randomUUID().toString();
        sessions.put(token, username);
        return token;
    }

    public User getUserByToken(String token){
        String username = sessions.get(token);
        if (username == null) return null;
        return users.get(username);
    }

    public String getUsernameForToken(String token){
        return sessions.get(token);
    }

    public Long getTeamIdForToken(String token){
        String u = sessions.get(token);
        if (u == null) return null;
        User usr = users.get(u);
        return usr == null ? null : usr.teamId;
    }

    public List<Player> listPlayers() {
        return new ArrayList<>(players.values());
    }

    public Player getPlayer(long id) {
        return players.get(id);
    }

    public Player savePlayer(Player p) {
        players.put(p.getId(), p);
        return p;
    }

    public List<Team> listTeams() {
        return new ArrayList<>(teams.values());
    }

    public Team getTeam(long id) {
        return teams.get(id);
    }

    public Team saveTeam(Team t) {
        teams.put(t.getId(), t);
        return t;
    }

    public List<TransferAuction> listAuctions() {
        return new ArrayList<>(auctions.values());
    }

    public TransferAuction getAuction(long id) {
        return auctions.get(id);
    }

    public TransferAuction createAuction(Player player, long sellerTeamId, Instant expiresAt) {
        TransferAuction a = new TransferAuction(player, sellerTeamId, expiresAt);
        auctions.put(a.getId(), a);
        // remove player from seller's squad immediately
        Team seller = teams.get(sellerTeamId);
        if (seller != null) {
            seller.removePlayer(player);
        }
        return a;
    }

    public void removeAuction(long id) {
        auctions.remove(id);
    }

    public String getGameState(long teamId) {
        return gameStates.get(teamId);
    }

    public void saveGameState(long teamId, String json) {
        gameStates.put(teamId, json);
        persistGameStates();
    }

    public synchronized void clearUsers(){
        users.clear();
        sessions.clear();
        persistUsers();
    }

    public void clearExpiredAuctions() {
        Instant now = Instant.now();
        List<Long> toRemove = new ArrayList<>();
        for (Map.Entry<Long, TransferAuction> e : auctions.entrySet()) {
            if (e.getValue().getExpiresAt().isBefore(now) || e.getValue().getExpiresAt().equals(now)) {
                toRemove.add(e.getKey());
            }
        }
        toRemove.forEach(auctions::remove);
    }
}
