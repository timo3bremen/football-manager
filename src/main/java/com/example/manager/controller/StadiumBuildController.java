package com.example.manager.controller;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.manager.dto.StadiumBuildDTO;
import com.example.manager.model.StadiumBuild;
import com.example.manager.model.Team;
import com.example.manager.repository.StadiumBuildRepository;
import com.example.manager.repository.TeamRepository;

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
	 * Get current active build for a team GET /api/v2/stadium-build/team/{teamId}
	 */
	@GetMapping("/team/{teamId}")
	public ResponseEntity<?> getActiveBuild(@PathVariable Long teamId) {
		try {
			Optional<StadiumBuild> build = stadiumBuildRepository
					.findFirstByTeamIdAndCompletedFalseOrderByStartTimeDesc(teamId);

			if (build.isPresent()) {
				StadiumBuildDTO dto = new StadiumBuildDTO(build.get().getId(), build.get().getTeamId(),
						build.get().getTotalSeats(), build.get().getSeatType(), build.get().getCost(),
						build.get().getStartTime(), build.get().getEndTime(), build.get().getCompleted());
				return ResponseEntity.ok(dto);
			}

			return ResponseEntity.ok(new HashMap<>());
		} catch (Exception e) {
			return ResponseEntity.status(500).body("Error: " + e.getMessage());
		}
	}

	/**
	 * Start a new stadium build POST /api/v2/stadium-build/start Body: { teamId,
	 * totalSeats, seatType, cost, durationSeconds }
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

			StadiumBuildDTO dto = new StadiumBuildDTO(savedBuild.getId(), savedBuild.getTeamId(),
					savedBuild.getTotalSeats(), savedBuild.getSeatType(), savedBuild.getCost(),
					savedBuild.getStartTime(), savedBuild.getEndTime(), savedBuild.getCompleted());

			return ResponseEntity.ok(dto);
		} catch (Exception e) {
			return ResponseEntity.status(400).body("Error: " + e.getMessage());
		}
	}

	/**
	 * Complete a build PUT /api/v2/stadium-build/{buildId}/complete
	 * Also updates the team's stadium capacity
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

		// Update the team's stadium capacity based on seat type
		Team team = teamRepository.findById(sb.getTeamId()).orElse(null);
		if (team != null) {
			String seatType = sb.getSeatType();
			int totalSeats = sb.getTotalSeats();
			
			if ("standing".equals(seatType)) {
				Long current = team.getStadiumCapacityStanding() != null ? team.getStadiumCapacityStanding() : 1000L;
				team.setStadiumCapacityStanding(current + totalSeats);
				System.out.println("[StadiumBuildController] 🏟️ Stehplätze: " + current + " -> " + (current + totalSeats));
			} else if ("seated".equals(seatType)) {
				Long current = team.getStadiumCapacitySeated() != null ? team.getStadiumCapacitySeated() : 0L;
				team.setStadiumCapacitySeated(current + totalSeats);
				System.out.println("[StadiumBuildController] 🏟️ Sitzplätze: " + current + " -> " + (current + totalSeats));
			} else if ("vip".equals(seatType)) {
				Long current = team.getStadiumCapacityVip() != null ? team.getStadiumCapacityVip() : 0L;
				team.setStadiumCapacityVip(current + totalSeats);
				System.out.println("[StadiumBuildController] 🏟️ VIP-Plätze: " + current + " -> " + (current + totalSeats));
			}
			
			teamRepository.save(team);
			System.out.println("[StadiumBuildController] ✅ Stadium capacity updated for Team " + team.getName());
		} else {
			System.err.println("[StadiumBuildController] ❌ Team not found: " + sb.getTeamId());
		}

			StadiumBuildDTO dto = new StadiumBuildDTO(updated.getId(), updated.getTeamId(), updated.getTotalSeats(),
					updated.getSeatType(), updated.getCost(), updated.getStartTime(), updated.getEndTime(),
					updated.getCompleted());

			return ResponseEntity.ok(dto);
		} catch (Exception e) {
			return ResponseEntity.status(500).body("Error: " + e.getMessage());
		}
	}
}
