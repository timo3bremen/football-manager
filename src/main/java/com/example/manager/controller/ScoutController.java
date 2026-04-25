package com.example.manager.controller;

import com.example.manager.model.Scout;
import com.example.manager.model.YouthPlayer;
import com.example.manager.service.RepositoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v2/scouts")
@CrossOrigin(origins = "*")
public class ScoutController {

    @Autowired
    private RepositoryService repositoryService;

    /**
     * Startet einen neuen Scout
     * POST /api/v2/scouts/start
     */
    @PostMapping("/start")
    public ResponseEntity<?> startScout(@RequestBody Map<String, Object> req) {
        try {
            Long teamId = ((Number) req.get("teamId")).longValue();
            String region = (String) req.get("region");
            int days = ((Number) req.get("days")).intValue();

            if (days < 1 || days > 7) {
                return ResponseEntity.badRequest().body(Map.of("error", "Tage müssen zwischen 1 und 7 liegen"));
            }

            repositoryService.startScout(teamId, region, days);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Scout gestartet in " + region + " für " + days + " Tage");
            response.put("cost", (long) days * 50000);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Gibt den aktiven Scout für ein Team zurück
     * GET /api/v2/scouts/team/{teamId}
     */
    @GetMapping("/team/{teamId}")
    public ResponseEntity<?> getActiveScout(@PathVariable Long teamId) {
        Scout scout = repositoryService.getActiveScout(teamId);
        if (scout == null) {
            return ResponseEntity.ok(Map.of("hasActiveScout", false));
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("id", scout.getId());
        result.put("region", scout.getRegion());
        result.put("daysRemaining", scout.getDaysRemaining());
        result.put("startedAt", scout.getStartedAt());
        return ResponseEntity.ok(result);
    }

    /**
     * Gibt alle gescouteten Spieler für ein Team zurück
     * GET /api/v2/scouts/players/team/{teamId}
     */
    @GetMapping("/players/team/{teamId}")
    public ResponseEntity<?> getScoutedPlayers(@PathVariable Long teamId) {
        List<YouthPlayer> players = repositoryService.getScoutedPlayers(teamId);
        return ResponseEntity.ok(players);
    }

    /**
     * Verpflichtet einen Jugenspieler zum Kader
     * POST /api/v2/scouts/recruit/{youthPlayerId}
     */
    @PostMapping("/recruit/{youthPlayerId}")
    public ResponseEntity<?> recruitPlayer(@PathVariable Long youthPlayerId) {
        try {
            repositoryService.recruitYouthPlayer(youthPlayerId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Spieler erfolgreich zum Kader hinzugefügt!");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Verpflichtet einen Jugenspieler zur Akademie (15-16 Jahre, kostenlos)
     * POST /api/v2/scouts/recruit-academy/{youthPlayerId}
     */
    @PostMapping("/recruit-academy/{youthPlayerId}")
    public ResponseEntity<?> recruitToAcademy(@PathVariable Long youthPlayerId) {
        try {
            repositoryService.recruitToAcademy(youthPlayerId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Spieler erfolgreich zur Akademie hinzugefügt!");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Lehnt einen Jugenspieler ab
     * DELETE /api/v2/scouts/{youthPlayerId}
     */
    @DeleteMapping("/{youthPlayerId}")
    public ResponseEntity<?> rejectPlayer(@PathVariable Long youthPlayerId) {
        try {
            repositoryService.rejectYouthPlayer(youthPlayerId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Spieler abgelehnt!");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Gibt die Jugenakademie für ein Team zurück
     * GET /api/v2/scouts/academy/team/{teamId}
     */
    @GetMapping("/academy/team/{teamId}")
    public ResponseEntity<?> getYouthAcademy(@PathVariable Long teamId) {
        List<YouthPlayer> academy = repositoryService.getYouthAcademy(teamId);
        return ResponseEntity.ok(academy);
    }
}
