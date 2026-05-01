package com.example.manager.controller;

import com.example.manager.model.Team;
import com.example.manager.model.League;
import com.example.manager.model.LeagueSlot;
import com.example.manager.repository.TeamRepository;
import com.example.manager.repository.LeagueRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST Controller für Team-Operationen.
 */
@RestController
@RequestMapping("/api/v2/teams")
@CrossOrigin(origins = "*")
public class TeamController {

	@Autowired
	private TeamRepository teamRepository;

	@Autowired
	private LeagueRepository leagueRepository;

	/**
	 * Gibt Team-Details zurück.
	 * GET /api/v2/teams/{teamId}
	 */
	@GetMapping("/{teamId}")
	public ResponseEntity<?> getTeam(@PathVariable Long teamId) {
		try {
			Team team = teamRepository.findById(teamId).orElse(null);
			if (team == null) {
				return ResponseEntity.notFound().build();
			}

			// Erstelle Response mit allen relevanten Daten
			Map<String, Object> response = new HashMap<>();
			response.put("id", team.getId());
			response.put("name", team.getName());
			response.put("budget", team.getBudgetAsLong());
			response.put("activeFormation", team.getActiveFormation());
			response.put("isCPU", team.isCPU());
			response.put("stadiumCapacityStanding", team.getStadiumCapacityStanding());
			response.put("stadiumCapacitySeated", team.getStadiumCapacitySeated());
			response.put("stadiumCapacityVip", team.getStadiumCapacityVip());
			response.put("stadiumCapacity", team.getStadiumCapacity()); // Total
			response.put("fanSatisfaction", team.getFanSatisfaction());
			response.put("ticketPriceStanding", team.getTicketPriceStanding());
			response.put("ticketPriceSeated", team.getTicketPriceSeated());
			response.put("ticketPriceVip", team.getTicketPriceVip());

			return ResponseEntity.ok(response);
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
		}
	}

	/**
	 * Gibt Liga und Land für ein Team zurück.
	 * GET /api/v2/teams/{teamId}/league-info
	 */
	@GetMapping("/{teamId}/league-info")
	public ResponseEntity<?> getTeamLeagueInfo(@PathVariable Long teamId) {
		try {
			// Finde das Team
			Team team = teamRepository.findById(teamId).orElse(null);
			if (team == null) {
				return ResponseEntity.notFound().build();
			}

			// Finde die Liga, in der dieses Team ist
			List<League> allLeagues = leagueRepository.findAll();
			League userLeague = null;
			
			for (League league : allLeagues) {
				for (LeagueSlot slot : league.getSlots()) {
					if (slot.getTeamId() != null && slot.getTeamId().equals(teamId)) {
						userLeague = league;
						break;
					}
				}
				if (userLeague != null) break;
			}

			if (userLeague == null) {
				// Team ist in keiner Liga
				return ResponseEntity.ok(Map.of(
					"teamId", teamId,
					"teamName", team.getName(),
					"leagueId", null,
					"leagueName", null,
					"country", null
				));
			}

			// Gebe Liga und Land zurück
			return ResponseEntity.ok(Map.of(
				"teamId", teamId,
				"teamName", team.getName(),
				"leagueId", userLeague.getId(),
				"leagueName", userLeague.getName(),
				"divisionLabel", userLeague.getDivisionLabel(),
				"country", userLeague.getCountry()
			));

		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
		}
	}

	/**
	 * Aktualisiert ein Team (z.B. aktive Formation).
	 * PUT /api/v2/teams/{teamId}
	 */
	@PutMapping("/{teamId}")
	public ResponseEntity<?> updateTeam(@PathVariable Long teamId, @RequestBody Team updatedTeam) {
		try {
			Team team = teamRepository.findById(teamId).orElse(null);
			if (team == null) {
				return ResponseEntity.notFound().build();
			}

			// Aktualisiere nur bestimmte Felder
			if (updatedTeam.getActiveFormation() != null) {
				team.setActiveFormation(updatedTeam.getActiveFormation());
			}
			if (updatedTeam.getName() != null) {
				team.setName(updatedTeam.getName());
			}

			Team saved = teamRepository.save(team);
			return ResponseEntity.ok(saved);

		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
		}
	}

	/**
	 * Prüft wie viele CPU-Teams in einer Liga verfügbar sind
	 * GET /api/v2/teams/league/{leagueId}/available-cpu-teams
	 */
	@GetMapping("/league/{leagueId}/available-cpu-teams")
	public ResponseEntity<?> getAvailableCpuTeams(@PathVariable Long leagueId) {
		try {
			League league = leagueRepository.findById(leagueId).orElse(null);
			if (league == null) {
				return ResponseEntity.status(404).body(Map.of("error", "Liga nicht gefunden"));
			}

			int cpuTeamCount = 0;
			int totalTeams = 0;

			for (LeagueSlot slot : league.getSlots()) {
				if (slot.getTeamId() != null) {
					totalTeams++;
					Team team = teamRepository.findById(slot.getTeamId()).orElse(null);
					if (team != null && team.isCPU()) {
						cpuTeamCount++;
					}
				}
			}

			Map<String, Object> response = new HashMap<>();
			response.put("leagueId", leagueId);
			response.put("leagueName", league.getName());
			response.put("cpuTeamsAvailable", cpuTeamCount);
			response.put("totalTeams", totalTeams);
			response.put("canJoin", cpuTeamCount > 0);

			return ResponseEntity.ok(response);
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
		}
	}
}
