package com.example.manager.controller;

import com.example.manager.model.LineupSlot;
import com.example.manager.model.Player;
import com.example.manager.service.LineupService;
import com.example.manager.service.RepositoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST Controller für Lineup-Management.
 * Direkte Persistierung in lineups-Tabelle statt GameState.
 */
@RestController
@RequestMapping("/api/lineups")
@CrossOrigin(origins = "*")
public class LineupController {

    @Autowired
    private LineupService lineupService;

    @Autowired
    private RepositoryService repositoryService;

    /**
     * Speichert eine Aufstellung aus dem Frontend format.
     * POST /api/lineups/{teamId}/save
     * Body: { "formationId": "4-4-2", "lineup": { "GK": "123", "D1": "456", ... }, "formationRows": [...] }
     */
    @PostMapping("/{teamId}/save")
    public ResponseEntity<?> saveLineupFromFrontend(
            @PathVariable Long teamId,
            @RequestBody Map<String, Object> request) {
        
        try {
            System.out.println("[LineupController] saveLineupFromFrontend called: teamId=" + teamId);
            System.out.println("[LineupController] Request body: " + request);
            
            String formationId = (String) request.getOrDefault("currentFormation", "4-4-2");
            Object lineupObj = request.get("lineup");
            List<List<String>> formationRows = (List<List<String>>) request.get("formationRows");
            
            System.out.println("[LineupController] formationId=" + formationId);
            System.out.println("[LineupController] lineupObj type=" + (lineupObj != null ? lineupObj.getClass().getName() : "null"));
            System.out.println("[LineupController] lineupObj=" + lineupObj);
            System.out.println("[LineupController] formationRows=" + formationRows);
            
            // Handle lineup as Map (could be LinkedHashMap or other Map implementation)
            Map<String, Object> lineupMap = new HashMap<>();
            if (lineupObj instanceof Map) {
                lineupMap = (Map<String, Object>) lineupObj;
            } else if (lineupObj instanceof java.util.ArrayList) {
                // Frontend sometimes sends empty array instead of empty map
                System.out.println("[LineupController] Frontend sent ArrayList instead of Map, treating as empty");
                lineupMap = new HashMap<>();
            } else if (lineupObj != null) {
                // If it's not a map but is an object, try to convert
                System.out.println("[LineupController] Warning: lineup is not a Map, type=" + lineupObj.getClass().getName());
                return ResponseEntity.badRequest().body("lineup must be a map object");
            }
            
            if (lineupMap == null) {
                lineupMap = new HashMap<>();
            }
            
            System.out.println("[LineupController] Final lineupMap: " + lineupMap);
            
            // Build map for saving: nur die playerId updaten für die Namen die im lineupMap sind
            // Die slotIndex sollte aus den bestehenden Slots kommen, nicht neu berechnet werden
            Map<Integer, Long> slots = new HashMap<>();
            
            // Lade alle existierenden Slots für diese Formation
            List<LineupSlot> existingSlots = repositoryService.getLineup(teamId, formationId);
            System.out.println("[LineupController] Loaded " + existingSlots.size() + " existing slots");
            
            // Für jeden existierenden Slot: Update playerId wenn in lineupMap vorhanden
            for (LineupSlot existing : existingSlots) {
                String slotName = existing.getSlotName();
                Object playerIdObj = lineupMap.get(slotName);
                
                Long playerId = null;
                if (playerIdObj != null && !playerIdObj.toString().equals("null")) {
                    try {
                        playerId = Long.parseLong(playerIdObj.toString());
                        System.out.println("[LineupController] Slot " + existing.getSlotIndex() + " (" + slotName + ") = Player " + playerId);
                    } catch (NumberFormatException e) {
                        System.out.println("[LineupController] Invalid player ID for " + slotName + ": " + playerIdObj);
                    }
                }
                
                // Speichere die playerId (kann auch null sein)
                slots.put(existing.getSlotIndex(), playerId);
            }
            
            System.out.println("[LineupController] Final slots: " + slots);
            
            // Save lineup
            lineupService.saveLineup(teamId, formationId, slots);
            
            System.out.println("[LineupController] Lineup saved successfully");
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Lineup saved",
                "teamId", teamId,
                "formationId", formationId,
                "slots", slots.size()
            ));
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }

    /**
     * Lädt die Aufstellung eines Teams für eine Formation.
     * GET /api/lineups/{teamId}/{formationId}
     */
    @GetMapping("/{teamId}/{formationId}")
    public ResponseEntity<?> getLineup(@PathVariable Long teamId, @PathVariable String formationId) {
        List<LineupSlot> lineup = repositoryService.getLineup(teamId, formationId);
        return ResponseEntity.ok(lineup);
    }

    /**
     * Lädt die Aufstellung als Map (slotName -> playerId oder null)
     * GET /api/lineups/{teamId}/{formationId}/map
     * 
     * Gibt die Lineup in dem Format zurück, das das Frontend erwartet:
     * { "GK": 1, "D1": 2, "D2": null, ... }
     * WICHTIG: Auch leere Slots (playerId=null) werden zurückgegeben!
     */
    @GetMapping("/{teamId}/{formationId}/map")
    public ResponseEntity<?> getLineupAsMap(@PathVariable Long teamId, @PathVariable String formationId) {
        System.out.println("[LineupController] getLineupAsMap called: teamId=" + teamId + ", formationId=" + formationId);
        
        // Lade direkt die Slots aus der DB
        List<LineupSlot> slots = repositoryService.getLineup(teamId, formationId);
        
        System.out.println("[LineupController] Found " + slots.size() + " lineup slots");
        for (LineupSlot slot : slots) {
            System.out.println("[LineupController]   Slot " + slot.getSlotIndex() + " ('" + slot.getSlotName() + "') -> Player " + slot.getPlayerId());
        }
        
        // Konvertiere zu Map: slotName -> playerId (das Format, das das Frontend braucht)
        // WICHTIG: Auch null-Werte einschließen, damit das Frontend weiß, welche Positionen leer sind
        Map<String, Long> result = new HashMap<>();
        for (LineupSlot slot : slots) {
            String slotName = slot.getSlotName() != null ? slot.getSlotName() : ("Slot" + slot.getSlotIndex());
            result.put(slotName, slot.getPlayerId());  // playerId kann null sein!
        }
        
        System.out.println("[LineupController] Returning lineup map: " + result);
        return ResponseEntity.ok(result);
    }

    /**
     * Speichert eine neue Aufstellung.
     * POST /api/lineups/{teamId}/{formationId}
     * Body: { "1": 123, "2": 456, ... } (slotIndex -> playerId)
     */
    @PostMapping("/{teamId}/{formationId}")
    public ResponseEntity<?> saveLineup(
            @PathVariable Long teamId,
            @PathVariable String formationId,
            @RequestBody Map<Integer, Long> slots) {
        
        lineupService.saveLineup(teamId, formationId, slots);
        return ResponseEntity.ok("Lineup saved");
    }

    /**
     * Wechselt einen einzelnen Spieler in der Aufstellung.
     * PUT /api/lineups/{teamId}/{formationId}/slot/{slotIndex}
     * Body: { "playerId": 123 }
     */
    @PutMapping("/{teamId}/{formationId}/slot/{slotIndex}")
    public ResponseEntity<?> swapPlayer(
            @PathVariable Long teamId,
            @PathVariable String formationId,
            @PathVariable Integer slotIndex,
            @RequestBody Map<String, Long> body) {
        
        Long playerId = body.get("playerId");
        lineupService.swapPlayer(teamId, formationId, slotIndex, playerId);
        return ResponseEntity.ok("Player swapped");
    }

    /**
     * Entfernt einen Spieler aus einer Position.
     * DELETE /api/lineups/{teamId}/{formationId}/slot/{slotIndex}
     */
    @DeleteMapping("/{teamId}/{formationId}/slot/{slotIndex}")
    public ResponseEntity<?> removePlayer(
            @PathVariable Long teamId,
            @PathVariable String formationId,
            @PathVariable Integer slotIndex) {
        
        lineupService.removePlayerFromSlot(teamId, formationId, slotIndex);
        return ResponseEntity.ok("Player removed");
    }

    /**
     * Erstellt eine Standard-Aufstellung mit den besten Spielern.
     * POST /api/lineups/{teamId}/{formationId}/default
     */
    @PostMapping("/{teamId}/{formationId}/default")
    public ResponseEntity<?> createDefaultLineup(
            @PathVariable Long teamId,
            @PathVariable String formationId) {
        
        lineupService.createDefaultLineup(teamId, formationId);
        return ResponseEntity.ok("Default lineup created");
    }

    /**
     * Lädt alle Spieler die in einer Aufstellung verwendet werden.
     * GET /api/lineups/{teamId}/{formationId}/players
     */
    @GetMapping("/{teamId}/{formationId}/players")
    public ResponseEntity<?> getPlayersInLineup(
            @PathVariable Long teamId,
            @PathVariable String formationId) {
        
        List<Player> players = lineupService.getPlayersInLineup(teamId, formationId);
        return ResponseEntity.ok(players);
    }

    /**
     * Validiert eine Aufstellung.
     * GET /api/lineups/{teamId}/{formationId}/validate
     */
    @GetMapping("/{teamId}/{formationId}/validate")
    public ResponseEntity<?> validateLineup(
            @PathVariable Long teamId,
            @PathVariable String formationId) {
        
        boolean isValid = lineupService.validateLineup(teamId, formationId);
        return ResponseEntity.ok(Map.of("valid", isValid));
    }

    /**
     * Debug: Zeigt alle Lineups für ein Team
     * GET /api/lineups/debug/{teamId}
     */
    @GetMapping("/debug/{teamId}")
    public ResponseEntity<?> debugGetAllLineups(@PathVariable Long teamId) {
        System.out.println("[LineupController] DEBUG: Getting all lineups for teamId=" + teamId);
        List<LineupSlot> lineups = repositoryService.getAllLineups(teamId);
        System.out.println("[LineupController] DEBUG: Found " + lineups.size() + " lineups");
        for (LineupSlot slot : lineups) {
            System.out.println("[LineupController] DEBUG: " + slot.getFormationId() + " Slot " + slot.getSlotIndex() + " = Player " + slot.getPlayerId());
        }
        return ResponseEntity.ok(Map.of(
            "teamId", teamId,
            "count", lineups.size(),
            "lineups", lineups
        ));
    }
}