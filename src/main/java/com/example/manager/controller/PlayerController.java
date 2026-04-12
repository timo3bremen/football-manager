package com.example.manager.controller;

import com.example.manager.model.Player;
import com.example.manager.repository.PlayerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Comparator;

/**
 * REST Controller für erweiterte Player-Management Operations.
 * Für Basis-Endpoints siehe ManagerController.
 */
@RestController
@RequestMapping("/api/v2/players")
@CrossOrigin(origins = "*")
public class PlayerController {

    @Autowired
    private PlayerRepository playerRepository;

    /**
     * Lädt alle Spieler eines Teams, sortiert nach Position (GK, DEF, MID, FWD) und Rating.
     * GET /api/v2/players/team/{teamId}
     */
    @GetMapping("/team/{teamId}")
    public ResponseEntity<List<Player>> getPlayersByTeam(@PathVariable Long teamId) {
        List<Player> players = playerRepository.findByTeamId(teamId);
        
        // Sort by position order and then by rating (descending)
        players.sort(new Comparator<Player>() {
            private int getPositionOrder(String pos) {
                if ("GK".equals(pos)) return 0;
                if ("DEF".equals(pos)) return 1;
                if ("MID".equals(pos)) return 2;
                if ("FWD".equals(pos)) return 3;
                return 4;
            }
            
            @Override
            public int compare(Player p1, Player p2) {
                int posOrder1 = getPositionOrder(p1.getPosition());
                int posOrder2 = getPositionOrder(p2.getPosition());
                
                if (posOrder1 != posOrder2) {
                    return Integer.compare(posOrder1, posOrder2);
                }
                // Same position, sort by rating descending
                return Integer.compare(p2.getRating(), p1.getRating());
            }
        });
        
        return ResponseEntity.ok(players);
    }

    /**
     * Lädt einen einzelnen Spieler.
     * GET /api/v2/players/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<Player> getPlayer(@PathVariable Long id) {
        return playerRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Speichert/aktualisiert einen Spieler.
     * POST /api/v2/players
     */
    @PostMapping
    public ResponseEntity<Player> savePlayer(@RequestBody Player player) {
        Player saved = playerRepository.save(player);
        return ResponseEntity.ok(saved);
    }

    /**
     * Löscht einen Spieler.
     * DELETE /api/v2/players/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletePlayer(@PathVariable Long id) {
        playerRepository.deleteById(id);
        return ResponseEntity.ok("Player deleted");
    }
}
