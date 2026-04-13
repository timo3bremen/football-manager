package com.example.manager.controller;

import com.example.manager.model.Player;
import com.example.manager.service.TransferMarketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller für Transfermarkt-Operationen
 */
@RestController
@RequestMapping("/api/v2/transfer-market")
@CrossOrigin(origins = "*")
public class TransferMarketController {

    @Autowired
    private TransferMarketService transferMarketService;

    /**
     * Gibt alle verfügbaren Spieler auf dem Transfermarkt zurück
     * GET /api/v2/transfer-market/available
     */
    @GetMapping("/available")
    public ResponseEntity<List<Player>> getAvailablePlayers() {
        List<Player> players = transferMarketService.getAvailablePlayers();
        return ResponseEntity.ok(players);
    }

    /**
     * Listet einen Spieler eines Teams zum Verkauf an
     * POST /api/v2/transfer-market/list/{playerId}
     * Body: { teamId: Long }
     */
    @PostMapping("/list/{playerId}")
    public ResponseEntity<Player> listPlayerForSale(
            @PathVariable Long playerId,
            @RequestParam Long teamId) {
        Player player = transferMarketService.listPlayerForSale(playerId, teamId);
        if (player != null) {
            return ResponseEntity.ok(player);
        }
        return ResponseEntity.badRequest().build();
    }

    /**
     * Kauft einen Spieler für ein Team
     * POST /api/v2/transfer-market/buy/{playerId}
     * Body: { teamId: Long }
     */
    @PostMapping("/buy/{playerId}")
    public ResponseEntity<Player> buyPlayer(
            @PathVariable Long playerId,
            @RequestParam Long teamId) {
        Player player = transferMarketService.buyPlayer(playerId, teamId);
        if (player != null) {
            return ResponseEntity.ok(player);
        }
        return ResponseEntity.badRequest().build();
    }

    /**
     * Sucht nach Spielern basierend auf Kriterien
     * GET /api/v2/transfer-market/search
     * Optionale Parameter: position, minRating, maxRating
     */
    @GetMapping("/search")
    public ResponseEntity<List<Player>> searchPlayers(
            @RequestParam(required = false) String position,
            @RequestParam(required = false) Integer minRating,
            @RequestParam(required = false) Integer maxRating) {
        List<Player> players = transferMarketService.searchPlayers(position, minRating, maxRating);
        return ResponseEntity.ok(players);
    }
}
