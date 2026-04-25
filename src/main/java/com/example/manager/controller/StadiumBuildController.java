package com.example.manager.controller;

import com.example.manager.dto.StadiumBuildDTO;
import com.example.manager.model.StadiumBuild;
import com.example.manager.model.Team;
import com.example.manager.repository.StadiumBuildRepository;
import com.example.manager.repository.TeamRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * REST Controller for Stadium Build operations
 */
@RestController
@RequestMapping("/api/v2/stadium-build")
@CrossOrigin(origins = "*")
public class StadiumBuildController {

    @Autowired
    private StadiumBuildRepository stadiumBuildRepository;

    @Autowired
    private TeamRepository teamRepository;

    /**
     * Get current active build for a team
     * GET /api/v2/stadium-build/team/{teamId}
     */
    @GetMapping("/team/{teamId}")
    public ResponseEntity<?> getActiveBuild(@PathVariable Long teamId) {
        try {
            Optional<StadiumBuild> build = stadiumBuildRepository.findFirstByTeamIdAndCompletedFalseOrderByStartTimeDesc(teamId);
            
            if (build.isPresent()) {
                StadiumBuildDTO dto = new StadiumBuildDTO(
                    build.get().getId(),
                    build.get().getTeamId(),
                    build.get().getTotalSeats(),
                    build.get().getSeatType(),
                    build.get().getCost(),
                    build.get().getStartTime(),
                    build.get().getEndTime(),
                    build.get().getCompleted()
                );
                return ResponseEntity.ok(dto);
            }
            
            return ResponseEntity.ok(new HashMap<>());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    /**
     * Start a new stadium build
     * POST /api/v2/stadium-build/start
     * Body: { teamId, totalSeats, seatType, cost, durationSeconds }
     */
    @PostMapping("/start")
    public ResponseEntity<?> startBuild(@RequestBody Map<String, Object> request) {
        try {
            Long teamId = ((Number) request.get("teamId")).longValue();
            Integer totalSeats = ((Number) request.get("totalSeats")).intValue();
            String seatType = (String) request.get("seatType");
            Long cost = ((Number) request.get("cost")).longValue();
            Long durationSeconds = ((Number) request.get("durationSeconds")).longValue();

            // Verify team exists
            if (!teamRepository.existsById(teamId)) {
                return ResponseEntity.status(404).body("Team not found");
            }

            // Create new build
            LocalDateTime startTime = LocalDateTime.now();
            LocalDateTime endTime = startTime.plusSeconds(durationSeconds);

            StadiumBuild build = new StadiumBuild(teamId, totalSeats, seatType, cost, startTime, endTime);
            StadiumBuild savedBuild = stadiumBuildRepository.save(build);

            StadiumBuildDTO dto = new StadiumBuildDTO(
                savedBuild.getId(),
                savedBuild.getTeamId(),
                savedBuild.getTotalSeats(),
                savedBuild.getSeatType(),
                savedBuild.getCost(),
                savedBuild.getStartTime(),
                savedBuild.getEndTime(),
                savedBuild.getCompleted()
            );

            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            return ResponseEntity.status(400).body("Error: " + e.getMessage());
        }
    }

    /**
     * Complete a build
     * PUT /api/v2/stadium-build/{buildId}/complete
     */
    @PutMapping("/{buildId}/complete")
    public ResponseEntity<?> completeBuild(@PathVariable Long buildId) {
        try {
            Optional<StadiumBuild> build = stadiumBuildRepository.findById(buildId);
            
            if (!build.isPresent()) {
                return ResponseEntity.status(404).body("Build not found");
            }

            StadiumBuild sb = build.get();
            sb.setCompleted(true);
            StadiumBuild updated = stadiumBuildRepository.save(sb);

            StadiumBuildDTO dto = new StadiumBuildDTO(
                updated.getId(),
                updated.getTeamId(),
                updated.getTotalSeats(),
                updated.getSeatType(),
                updated.getCost(),
                updated.getStartTime(),
                updated.getEndTime(),
                updated.getCompleted()
            );

            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }
}
