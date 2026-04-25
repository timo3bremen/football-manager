package com.example.manager.controller;

import com.example.manager.model.Sponsor;
import com.example.manager.model.Team;
import com.example.manager.repository.SponsorRepository;
import com.example.manager.repository.TeamRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/sponsors")
@CrossOrigin(origins = "*")
public class SponsorController {

    @Autowired
    private SponsorRepository sponsorRepository;

    @Autowired
    private TeamRepository teamRepository;

    /**
     * GET /api/sponsors/{teamId}
     * Get sponsor for a team
     */
    @GetMapping("/{teamId}")
    public ResponseEntity<?> getSponsor(@PathVariable Long teamId) {
        Optional<Sponsor> sponsor = sponsorRepository.findByTeamId(teamId);
        if (sponsor.isPresent()) {
            return ResponseEntity.ok(sponsor.get());
        }
        return ResponseEntity.ok(new HashMap<>());
    }

    /**
     * POST /api/sponsors
     * Add sponsor to team
     */
    @PostMapping
    public ResponseEntity<?> addSponsor(@RequestBody Map<String, Object> payload) {
        try {
            Long teamId = Long.parseLong(payload.get("teamId").toString());
            String name = payload.get("name").toString();
            int appearancePayout = Integer.parseInt(payload.get("appearancePayout").toString());
            int winPayout = Integer.parseInt(payload.get("winPayout").toString());
            int survivePayout = Integer.parseInt(payload.get("survivePayout").toString());
            int titlePayout = Integer.parseInt(payload.get("titlePayout").toString());

            // Check if team already has a sponsor
            Optional<Sponsor> existingSponsor = sponsorRepository.findByTeamId(teamId);
            if (existingSponsor.isPresent()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Team hat bereits einen Sponsor");
                return ResponseEntity.badRequest().body(response);
            }

            // Create new sponsor
            Sponsor sponsor = new Sponsor(teamId, name, appearancePayout, winPayout, survivePayout, titlePayout);
            sponsorRepository.save(sponsor);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("sponsor", sponsor);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Fehler beim Hinzufügen des Sponsors: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * DELETE /api/sponsors/{teamId}
     * Remove sponsor from team
     */
    @DeleteMapping("/{teamId}")
    public ResponseEntity<?> removeSponsor(@PathVariable Long teamId) {
        sponsorRepository.deleteByTeamId(teamId);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        return ResponseEntity.ok(response);
    }
}
