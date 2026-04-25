package com.example.manager.controller;

import com.example.manager.service.AuctionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for auction operations.
 * Endpoints for viewing and bidding on auction players.
 */
@RestController
@RequestMapping("/api/v2/auction")
@CrossOrigin(origins = "*")
public class AuctionController {

    @Autowired
    private AuctionService auctionService;

    /**
     * Gets all active auction players.
     * GET /api/v2/auction/active
     */
    @GetMapping("/active")
    public ResponseEntity<List<Map<String, Object>>> getActiveAuctions() {
        try {
            System.out.println("[AuctionController] GET /api/v2/auction/active called");
            List<Map<String, Object>> auctions = auctionService.getActiveAuctions();
            System.out.println("[AuctionController] Returning " + auctions.size() + " active auctions");
            return ResponseEntity.ok(auctions);
        } catch (Exception e) {
            System.err.println("[AuctionController] Error in getActiveAuctions: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.ok(new ArrayList<>());
        }
    }

    /**
     * Gets details of a specific auction including all bids (filtered).
     * Only shows own bids and the highest bid.
     * GET /api/v2/auction/{auctionPlayerId}/details?teamId=123
     */
    @GetMapping("/{auctionPlayerId}/details")
    public ResponseEntity<Map<String, Object>> getAuctionDetails(
            @PathVariable Long auctionPlayerId,
            @RequestParam(required = false) Long teamId) {
        try {
            Map<String, Object> details = auctionService.getAuctionDetails(auctionPlayerId, teamId);
            if (details == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(details);
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Gets all bids for a specific auction player (filtered).
     * Only shows own bids and the highest bid.
     * GET /api/v2/auction/{auctionPlayerId}/bids?teamId=123
     */
    @GetMapping("/{auctionPlayerId}/bids")
    public ResponseEntity<List<Map<String, Object>>> getBidsForAuction(
            @PathVariable Long auctionPlayerId,
            @RequestParam(required = false) Long teamId) {
        try {
            List<Map<String, Object>> bids = auctionService.getBidsForAuction(auctionPlayerId, teamId);
            return ResponseEntity.ok(bids);
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Places a bid on an auction player.
     * POST /api/v2/auction/{auctionPlayerId}/bid
     * Body: { teamId: Long, bidAmount: Long }
     */
    @PostMapping("/{auctionPlayerId}/bid")
    public ResponseEntity<Map<String, Object>> placeBid(
            @PathVariable Long auctionPlayerId,
            @RequestBody Map<String, Object> request) {
        try {
            Long teamId = ((Number) request.get("teamId")).longValue();
            Long bidAmount = ((Number) request.get("bidAmount")).longValue();

            Map<String, Object> result = auctionService.placeBid(auctionPlayerId, teamId, bidAmount);
            
            if ((Boolean) result.get("success")) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.badRequest().body(result);
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * Completes the contract negotiation after winning an auction.
     * POST /api/v2/auction/{auctionPlayerId}/complete-negotiation
     * Body: { teamId: Long, newSalary: Long, contractLength: Integer }
     */
    @PostMapping("/{auctionPlayerId}/complete-negotiation")
    public ResponseEntity<Map<String, Object>> completeAuctionNegotiation(
            @PathVariable Long auctionPlayerId,
            @RequestBody Map<String, Object> request) {
        try {
            Long teamId = ((Number) request.get("teamId")).longValue();
            Long newSalary = ((Number) request.get("newSalary")).longValue();
            Integer contractLength = ((Number) request.get("contractLength")).intValue();

            Map<String, Object> result = auctionService.completeAuctionNegotiation(
                    auctionPlayerId, teamId, newSalary, contractLength);

            if ((Boolean) result.get("success")) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.badRequest().body(result);
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * Manual endpoint to trigger daily auction creation (for testing).
     * POST /api/v2/auction/create-daily
     */
    @PostMapping("/create-daily")
    public ResponseEntity<Map<String, Object>> createDailyAuctionManual() {
        try {
            auctionService.createDailyAuction();
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Tägliche Auktion wurde erstellt"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Manual endpoint to trigger auction completion (for testing).
     * POST /api/v2/auction/complete-expired
     */
    @PostMapping("/complete-expired")
    public ResponseEntity<Map<String, Object>> completeExpiredAuctionsManual() {
        try {
            auctionService.completeExpiredAuctions();
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Abgelaufene Auktionen wurden abgeschlossen"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Manual endpoint to generate CPU bids (for testing).
     * POST /api/v2/auction/generate-cpu-bids
     */
    @PostMapping("/generate-cpu-bids")
    public ResponseEntity<Map<String, Object>> generateCPUBidsManual() {
        try {
            auctionService.generateCPUBids();
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "CPU-Gebote wurden generiert"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }
}
