package com.example.manager.controller;

import com.example.manager.model.Sponsor;
import com.example.manager.model.Team;
import com.example.manager.model.League;
import com.example.manager.model.LeagueSlot;
import com.example.manager.repository.SponsorRepository;
import com.example.manager.repository.TeamRepository;
import com.example.manager.repository.LeagueRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

    @Autowired
    private LeagueRepository leagueRepository;

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
     * GET /api/sponsors/options/{teamId}
     * Get sponsor options for a team based on their league division
     */
    @GetMapping("/options/{teamId}")
    public ResponseEntity<?> getSponsorOptions(@PathVariable Long teamId) {
        try {
            // Find team's league and division
            int division = 1; // Default to 1st division
            List<League> allLeagues = leagueRepository.findAll();
            
            for (League league : allLeagues) {
                for (LeagueSlot slot : league.getSlots()) {
                    if (slot.getTeamId() != null && slot.getTeamId().equals(teamId)) {
                        division = league.getDivision();
                        break;
                    }
                }
            }
            
            // Calculate multiplier based on division
            // 1. Liga: 100% (1.0)
            // 2. Liga: 60% (0.6) - 40% weniger
            // 3. Liga: 33% (0.33) - etwa 1/3
            double multiplier = division == 1 ? 1.0 : division == 2 ? 0.6 : 0.33;
            
            // Base sponsor options (1. Liga values)
            List<Map<String, Object>> options = new ArrayList<>();
            
            // SportCo
            Map<String, Object> sportCo = new HashMap<>();
            sportCo.put("key", "s1");
            sportCo.put("name", "SportCo");
            Map<String, Integer> sportCoPayouts = new HashMap<>();
            sportCoPayouts.put("appearance", (int) Math.round(40000 * multiplier));
            sportCoPayouts.put("win", (int) Math.round(100000 * multiplier));
            sportCoPayouts.put("survive", (int) Math.round(4000000 * multiplier));
            sportCoPayouts.put("title", (int) Math.round(15000000 * multiplier));
            sportCo.put("payouts", sportCoPayouts);
            options.add(sportCo);
            
            // MegaCorp
            Map<String, Object> megaCorp = new HashMap<>();
            megaCorp.put("key", "s2");
            megaCorp.put("name", "MegaCorp");
            Map<String, Integer> megaCorpPayouts = new HashMap<>();
            megaCorpPayouts.put("appearance", (int) Math.round(70000 * multiplier));
            megaCorpPayouts.put("win", (int) Math.round(20000 * multiplier));
            megaCorpPayouts.put("survive", (int) Math.round(7000000 * multiplier));
            megaCorpPayouts.put("title", (int) Math.round(12000000 * multiplier));
            megaCorp.put("payouts", megaCorpPayouts);
            options.add(megaCorp);
            
            // LocalBank
            Map<String, Object> localBank = new HashMap<>();
            localBank.put("key", "s3");
            localBank.put("name", "LocalBank");
            Map<String, Integer> localBankPayouts = new HashMap<>();
            localBankPayouts.put("appearance", (int) Math.round(120000 * multiplier));
            localBankPayouts.put("win", (int) Math.round(100000 * multiplier));
            localBankPayouts.put("survive", (int) Math.round(1000000 * multiplier));
            localBankPayouts.put("title", (int) Math.round(5000000 * multiplier));
            localBank.put("payouts", localBankPayouts);
            options.add(localBank);
            
            Map<String, Object> response = new HashMap<>();
            response.put("options", options);
            response.put("division", division);
            response.put("multiplier", multiplier);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("error", "Fehler beim Laden der Sponsor-Optionen: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
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
