package com.example.manager.service;

import com.example.manager.model.*;
import com.example.manager.repository.*;
import com.example.manager.util.PlayerNameGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * New repository-based service using Spring Data JPA.
 */
@Service
public class RepositoryService {

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private LineupRepository lineupRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StadiumPartRepository stadiumPartRepository;

    @Autowired
    private GameStateRepository gameStateRepository;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final Map<String, String> sessions = new HashMap<>(); // token -> username

    // User management
    @Transactional
    public String registerUser(String username, String password, Long teamId) {
        if (userRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("user exists");
        }
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setTeamId(teamId);
        userRepository.save(user);
        
        String token = UUID.randomUUID().toString();
        sessions.put(token, username);
        return token;
    }

    public String authenticateUser(String username, String password) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("user not found"));
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("invalid credentials");
        }
        String token = UUID.randomUUID().toString();
        sessions.put(token, username);
        return token;
    }

    public User getUserByToken(String token) {
        String username = sessions.get(token);
        if (username == null) return null;
        return userRepository.findByUsername(username).orElse(null);
    }

    public String getUsernameForToken(String token) {
        return sessions.get(token);
    }

    public Long getTeamIdForToken(String token) {
        String username = sessions.get(token);
        if (username == null) return null;
        return userRepository.findByUsername(username)
                .map(User::getTeamId)
                .orElse(null);
    }

    @Transactional
    public void clearUsers() {
        // When deleting users, also delete their teams
        List<User> allUsers = userRepository.findAll();
        for (User user : allUsers) {
            if (user.getTeamId() != null) {
                deleteTeamCascade(user.getTeamId());
            }
        }
        userRepository.deleteAll();
        sessions.clear();
    }
    
    /**
     * Löscht ein Team und alle zugehörigen Daten (Spieler, Lineups, Stadionteile).
     */
    @Transactional
    public void deleteTeamCascade(Long teamId) {
        try {
            // Delete lineups
            lineupRepository.deleteByTeamId(teamId);
            
            // Delete stadium parts
            stadiumPartRepository.deleteByTeamId(teamId);
            
            // Delete players
            List<Player> players = playerRepository.findByTeamId(teamId);
            playerRepository.deleteAll(players);
            
            // Delete team
            teamRepository.deleteById(teamId);
            
            System.out.println("[RepositoryService] Team " + teamId + " and all related data deleted");
        } catch (Exception e) {
            System.err.println("[RepositoryService] Error deleting team " + teamId + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Player management
    public List<Player> listPlayers() {
        return playerRepository.findAll();
    }

    public Player getPlayer(Long id) {
        return playerRepository.findById(id).orElse(null);
    }

    @Transactional
    public Player savePlayer(Player player) {
        return playerRepository.save(player);
    }

    public List<Player> getPlayersByTeam(Long teamId) {
        return playerRepository.findByTeamId(teamId);
    }

    // Team management
    public List<Team> listTeams() {
        return teamRepository.findAll();
    }

    public Team getTeam(Long id) {
        return teamRepository.findById(id).orElse(null);
    }

    @Transactional
    public Team saveTeam(Team team) {
        Team saved = teamRepository.save(team);
        
        // Initialize default players if squad is empty
        if (saved.getId() != null) {
            List<Player> existing = playerRepository.findByTeamId(saved.getId());
            if (existing.isEmpty()) {
                // Create 18 players with proper position distribution
                // 1 GK, 4 DEF, 7 MID, 6 FWD
                String[] positions = {"GK", "DEF", "DEF", "DEF", "DEF", "MID", "MID", "MID", "MID", "MID", "MID", "MID", "FWD", "FWD", "FWD", "FWD", "FWD", "FWD"};
                
                // Generate 18 random fictional players with random names and countries
                for (int i = 0; i < 18; i++) {
                    String[] playerData = PlayerNameGenerator.generatePlayerNameAndCountry();
                    
                    Player p = new Player(
                        playerData[0],  // Random name
                        50 + (int) (Math.random() * 40),  // Rating 50-90
                        50 + (int) (Math.random() * 50),  // Potential 50-100
                        (int) (Math.random() * 20) - 10,  // Form -10 to +10
                        positions[i],
                        playerData[1]   // Random country
                    );
                    p.setTeamId(saved.getId());
                    playerRepository.save(p);
                }
                
                // Initialize stadium parts
                for (int i = 0; i < 30; i++) {
                    StadiumPart part = new StadiumPart(saved.getId(), i, false, null);
                    stadiumPartRepository.save(part);
                }
                
                // Initialize lineup slots for all formations with playerId = null
                String[] formationIds = {"4-4-2", "4-3-3", "3-5-2"};
                
                // Slot names for each formation
                Map<String, String[]> slotNames = new HashMap<>();
                slotNames.put("4-4-2", new String[]{"GK", "D1", "D2", "D3", "D4", "M1", "M2", "M3", "M4", "F1", "F2"});
                slotNames.put("4-3-3", new String[]{"GK", "D1", "D2", "D3", "D4", "M1", "M2", "M3", "F1", "F2", "F3"});
                slotNames.put("3-5-2", new String[]{"GK", "D1", "D2", "D3", "M1", "M2", "M3", "M4", "M5", "F1", "F2"});
                
                for (String formationId : formationIds) {
                    String[] names = slotNames.get(formationId);
                    for (int slotIndex = 1; slotIndex <= 11; slotIndex++) {
                        LineupSlot slot = new LineupSlot();
                        slot.setTeamId(saved.getId());
                        slot.setFormationId(formationId);
                        slot.setSlotIndex(slotIndex);
                        slot.setPlayerId(null);  // Initially empty
                        slot.setSlotName(names[slotIndex - 1]);  // Use proper slot names
                        lineupRepository.save(slot);
                    }
                }
                
                System.out.println("[RepositoryService] Initialized 18 players and lineup slots for team " + saved.getId());
            }
        }
        
        return saved;
    }

    // Lineup management
    @Transactional
    public void saveLineup(Long teamId, String formationId, Map<Integer, Long> slots) {
        // Delete existing lineup for this formation
        lineupRepository.deleteByTeamIdAndFormationId(teamId, formationId);
        
        // Save new slots
        int index = 1;
        for (Map.Entry<Integer, Long> entry : slots.entrySet()) {
            LineupSlot slot = new LineupSlot();
            slot.setTeamId(teamId);
            slot.setFormationId(formationId);
            slot.setSlotIndex(entry.getKey());
            slot.setPlayerId(entry.getValue());
            lineupRepository.save(slot);
            index++;
        }
    }

    public List<LineupSlot> getLineup(Long teamId, String formationId) {
        return lineupRepository.findByTeamIdAndFormationId(teamId, formationId);
    }

    public List<LineupSlot> getAllLineups(Long teamId) {
        return lineupRepository.findByTeamId(teamId);
    }

    // Stadium management
    public List<StadiumPart> getStadiumParts(Long teamId) {
        return stadiumPartRepository.findByTeamId(teamId);
    }

    @Transactional
    public StadiumPart saveStadiumPart(StadiumPart part) {
        return stadiumPartRepository.save(part);
    }

    // GameState management
    public String getGameState(Long teamId) {
        return gameStateRepository.findById(teamId)
                .map(GameState::getJson)
                .orElse(null);
    }

    @Transactional
    public void saveGameState(Long teamId, String json) {
        GameState state = gameStateRepository.findById(teamId)
                .orElse(new GameState(teamId, json));
        state.setJson(json);
        gameStateRepository.save(state);
    }
}
