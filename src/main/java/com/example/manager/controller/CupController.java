package com.example.manager.controller;

import com.example.manager.model.CupTournament;
import com.example.manager.service.CupService;
import com.example.manager.repository.CupTournamentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for cup tournament operations.
 */
@RestController
@RequestMapping("/api/v2/cup")
@CrossOrigin(origins = "*")
public class CupController {

    @Autowired
    private CupService cupService;

    @Autowired
    private CupTournamentRepository cupTournamentRepository;

    /**
     * Get tournament by country and season.
     * GET /api/v2/cup/tournament/{country}
     * Auto-initializes if not exists
     */
    @GetMapping("/tournament/{country}")
    public ResponseEntity<Map<String, Object>> getTournamentByCountry(@PathVariable String country) {
        try {
            // Hole aktuelles Turnier (Saison 1 für jetzt)
            CupTournament tournament = cupTournamentRepository.findByCountryAndSeason(country, 1)
                    .orElse(null);
            
            // Wenn nicht existiert, erstelle automatisch
            if (tournament == null) {
                System.out.println("[CupController] Cup tournament for " + country + " not found, initializing...");
                cupService.initializeCupTournament(country, 1);
                tournament = cupTournamentRepository.findByCountryAndSeason(country, 1).orElse(null);
            }
            
            if (tournament == null) {
                return ResponseEntity.ok(new HashMap<>()); // Leeres Turnier wenn Fehler
            }

            return ResponseEntity.ok(cupService.getTournamentOverview(tournament.getId()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Initialize all cup tournaments for all countries.
     * POST /api/v2/cup/initialize-all
     */
    @PostMapping("/initialize-all")
    public ResponseEntity<Map<String, Object>> initializeAllCups(@RequestParam(defaultValue = "1") int season) {
        try {
            cupService.initializeAllCupTournaments(season);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "All cup tournaments initialized for season " + season
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * ...existing code...
     */
    public ResponseEntity<Map<String, Object>> getTournamentOverview(@PathVariable Long tournamentId) {
        try {
            Map<String, Object> overview = cupService.getTournamentOverview(tournamentId);
            return ResponseEntity.ok(overview);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get matches for a specific round.
     * GET /api/v2/cup/{tournamentId}/round/{round}
     */
    @GetMapping("/{tournamentId}/round/{round}")
    public ResponseEntity<List<Map<String, Object>>> getRoundMatches(
            @PathVariable Long tournamentId,
            @PathVariable int round) {
        try {
            List<Map<String, Object>> matches = cupService.getRoundMatches(tournamentId, round).stream()
                    .map(m -> {
                        Map<String, Object> match = new HashMap<>();
                        match.put("id", m.getId());
                        match.put("round", m.getRound());
                        match.put("homeTeamId", m.getHomeTeamId());
                        match.put("homeTeamName", m.getHomeTeamName());
                        match.put("awayTeamId", m.getAwayTeamId());
                        match.put("awayTeamName", m.getAwayTeamName());
                        match.put("homeGoals", m.getHomeGoals());
                        match.put("awayGoals", m.getAwayGoals());
                        match.put("winnerId", m.getWinnerId());
                        match.put("winnerName", m.getWinnerName());
                        match.put("status", m.getStatus());
                        return match;
                    })
                    .toList();

            return ResponseEntity.ok(matches);
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Manual endpoint to initialize cup tournament (for testing).
     * POST /api/v2/cup/initialize
     */
    @PostMapping("/initialize")
    public ResponseEntity<Map<String, Object>> initializeCup(
            @RequestParam String country,
            @RequestParam int season) {
        try {
            cupService.initializeCupTournament(country, season);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Cup tournament initialized for " + country + " season " + season
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Manual endpoint to complete a cup round (for testing).
     * POST /api/v2/cup/{tournamentId}/complete-round
     */
    @PostMapping("/{tournamentId}/complete-round")
    public ResponseEntity<Map<String, Object>> completeRound(
            @PathVariable Long tournamentId,
            @RequestParam int round) {
        try {
            CupTournament tournament = cupTournamentRepository.findById(tournamentId).orElse(null);
            
            if (tournament == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Tournament not found"));
            }

            cupService.completeCupRound(tournament, round);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Cup round " + round + " completed"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }
}
