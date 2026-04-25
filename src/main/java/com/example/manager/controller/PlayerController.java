package com.example.manager.controller;

import com.example.manager.model.Player;
import com.example.manager.model.Team;
import com.example.manager.repository.PlayerRepository;
import com.example.manager.repository.TeamRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

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

    @Autowired
    private TeamRepository teamRepository;

    /**
     * Lädt alle Spieler, sortiert nach Marktwert absteigend.
     * GET /api/v2/players
     */
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAllPlayers() {
        List<Player> players = playerRepository.findAll();
        // Sortiere nach Marktwert absteigend
        players.sort((p1, p2) -> Long.compare(p2.getMarketValue(), p1.getMarketValue()));
        
        // Enriche mit Teamnamen
        List<Map<String, Object>> result = players.stream()
            .map(this::enrichPlayerWithTeamName)
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(result);
    }

    /**
     * Hilfsmethode: Enriche Spieler mit Teamnamen
     */
    private Map<String, Object> enrichPlayerWithTeamName(Player player) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", player.getId());
        map.put("name", player.getName());
        map.put("position", player.getPosition());
        map.put("country", player.getCountry());
        map.put("age", player.getAge());
        map.put("rating", player.getRating());
        map.put("potential", player.getOverallPotential());
        map.put("salary", player.getSalary());
        map.put("marketValue", player.getMarketValue());
        map.put("contractLength", player.getContractLength());
        map.put("teamId", player.getTeamId());
        map.put("onTransferList", player.isOnTransferList());
        map.put("fitness", player.getFitness());
        
        // Füge Teamnamen hinzu wenn verfügbar
        if (player.getTeamId() != null && player.getTeamId() > 0) {
            Team team = teamRepository.findById(player.getTeamId()).orElse(null);
            map.put("teamName", team != null ? team.getName() : null);
        }
        
        return map;
    }

    /**
     * Lädt alle Spieler eines Teams, sortiert nach Position (GK, DEF, MID, FWD) und Rating.
     * GET /api/v2/players/team/{teamId}
     */
    @GetMapping("/team/{teamId}")
    public ResponseEntity<List<Map<String, Object>>> getPlayersByTeam(@PathVariable Long teamId) {
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
        
        // Enriche mit Teamnamen
        List<Map<String, Object>> result = players.stream()
            .map(this::enrichPlayerWithTeamName)
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(result);
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

    /**
     * Gibt alle Spieler eines Teams mit Vertragsinformationen zurück.
     * GET /api/v2/players/team/{teamId}/contracts
     */
    @GetMapping("/team/{teamId}/contracts")
    public ResponseEntity<List<Map<String, Object>>> getPlayerContracts(@PathVariable Long teamId) {
        List<Player> players = playerRepository.findByTeamId(teamId);
        
        List<Map<String, Object>> result = players.stream()
            .map(this::enrichPlayerWithTeamName)
            .collect(Collectors.toList());
        
        // Sort by name
        result.sort((a, b) -> ((String)a.get("name")).compareTo((String)b.get("name")));
        
        return ResponseEntity.ok(result);
    }

    /**
     * Verlängert den Vertrag eines Spielers mit neuem Gehalt und Laufzeit.
     * POST /api/v2/players/{id}/extend-contract
     * Body: { newSalary: Long, newContractLength: Integer }
     */
    @PostMapping("/{id}/extend-contract")
    public ResponseEntity<Map<String, Object>> extendPlayerContract(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, Object> request) {
        Player player = playerRepository.findById(id).orElse(null);
        if (player == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Spieler nicht gefunden");
            return ResponseEntity.badRequest().body(error);
        }

        // Prüfe ob max. Vertrag
        if (player.getContractLength() >= 5) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Spieler hat bereits maximalen 5-Saisons-Vertrag");
            return ResponseEntity.badRequest().body(error);
        }

        // Wenn Request-Body vorhanden ist, nutze neue Werte
        if (request != null && request.containsKey("newSalary") && request.containsKey("newContractLength")) {
            Long newSalary = ((Number) request.get("newSalary")).longValue();
            Integer newContractLength = ((Number) request.get("newContractLength")).intValue();
            
            // Validierung
            if (newSalary < player.getSalary()) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "Spieler akzeptiert nicht - Gehalt zu niedrig");
                return ResponseEntity.badRequest().body(error);
            }
            
            if (newContractLength > 5) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "Vertrag kann max. 5 Saisons lang sein");
                return ResponseEntity.badRequest().body(error);
            }
            
            // Aktualisiere Spieler
            player.setSalary(newSalary);
            player.setContractLength(newContractLength);
        } else {
            // Fallback: Verlängere um eine Saison (alte Logik)
            int newContractLength = Math.min(5, player.getContractLength() + 1);
            player.setContractLength(newContractLength);
        }

        player.calculateMarketValue();
        playerRepository.save(player);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Vertrag verlängert");
        response.put("player", enrichPlayerWithTeamName(player));
        
        return ResponseEntity.ok(response);
    }
}