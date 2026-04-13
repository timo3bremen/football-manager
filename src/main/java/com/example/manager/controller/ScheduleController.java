package com.example.manager.controller;

import com.example.manager.model.Schedule;
import com.example.manager.model.Matchday;
import com.example.manager.dto.LeagueInfoDTO;
import com.example.manager.dto.LeagueStandingsDTO;
import com.example.manager.dto.MatchSimulationResultDTO;
import com.example.manager.dto.MatchReportDTO;
import com.example.manager.dto.PlayerStatisticsDTO;
import com.example.manager.dto.LeagueStatisticsDTO;
import com.example.manager.repository.ScheduleRepository;
import com.example.manager.repository.MatchdayRepository;
import com.example.manager.service.RepositoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST Controller für Spielplan-Operationen.
 */
@RestController
@RequestMapping("/api/v2/schedule")
@CrossOrigin(origins = "*")
public class ScheduleController {

	@Autowired
	private ScheduleRepository scheduleRepository;

	@Autowired
	private MatchdayRepository matchdayRepository;

	@Autowired
	private RepositoryService repositoryService;

	/**
	 * Gibt den aktuellen Spielplan zurück.
	 * GET /api/v2/schedule
	 */
	@GetMapping
	public ResponseEntity<Schedule> getSchedule() {
		List<Schedule> schedules = scheduleRepository.findAll();
		if (schedules.isEmpty()) {
			return ResponseEntity.notFound().build();
		}
		Schedule schedule = schedules.get(0); // Return first schedule
		return ResponseEntity.ok(schedule);
	}

	/**
	 * Gibt einen bestimmten Spieltag zurück.
	 * GET /api/v2/schedule/matchday/{dayNumber}
	 */
	@GetMapping("/matchday/{dayNumber}")
	public ResponseEntity<Matchday> getMatchday(@PathVariable int dayNumber) {
		List<Matchday> matchdays = matchdayRepository.findAll();
		for (Matchday md : matchdays) {
			if (md.getDayNumber() == dayNumber) {
				return ResponseEntity.ok(md);
			}
		}
		return ResponseEntity.notFound().build();
	}

	/**
	 * Gibt einen bestimmten Spieltag für eine Liga zurück.
	 * GET /api/v2/schedule/matchday/league/{leagueId}/{dayNumber}
	 */
	@GetMapping("/matchday/league/{leagueId}/{dayNumber}")
	public ResponseEntity<Matchday> getMatchdayByLeague(@PathVariable Long leagueId, @PathVariable int dayNumber) {
		List<Matchday> matchdays = matchdayRepository.findAll();
		for (Matchday md : matchdays) {
			if (md.getDayNumber() == dayNumber && md.getLeagueId() != null && md.getLeagueId().equals(leagueId)) {
				return ResponseEntity.ok(md);
			}
		}
		return ResponseEntity.notFound().build();
	}

	/**
	 * Gibt alle Spieltage zurück.
	 * GET /api/v2/schedule/matchdays
	 */
	@GetMapping("/matchdays")
	public ResponseEntity<List<Matchday>> getAllMatchdays() {
		List<Matchday> matchdays = matchdayRepository.findAll();
		return ResponseEntity.ok(matchdays);
	}

	/**
	 * Gibt die aktuelle Ligatabelle zurück.
	 * GET /api/v2/schedule/standings
	 */
	@GetMapping("/standings")
	public ResponseEntity<List<LeagueStandingsDTO>> getStandings() {
		List<LeagueStandingsDTO> standings = repositoryService.getLeagueStandings();
		return ResponseEntity.ok(standings);
	}

	/**
	 * Gibt den aktuellen Spieltag zurück.
	 * GET /api/v2/schedule/current-matchday
	 */
	@GetMapping("/current-matchday")
	public ResponseEntity<Map<String, Integer>> getCurrentMatchday() {
		repositoryService.checkAndAdvanceMatchday();
		int matchday = repositoryService.getCurrentMatchday();
		Map<String, Integer> response = new HashMap<>();
		response.put("currentMatchday", matchday);
		return ResponseEntity.ok(response);
	}

	/**
	 * Simuliert ein einzelnes Spiel.
	 * POST /api/v2/schedule/simulate-match/{matchId}
	 */
	@PostMapping("/simulate-match/{matchId}")
	public ResponseEntity<?> simulateMatch(@PathVariable Long matchId) {
		try {
			MatchSimulationResultDTO result = repositoryService.simulateMatch(matchId);
			return ResponseEntity.ok(result);
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(e.getMessage());
		}
	}

	/**
	 * Gibt den Spielbericht eines Spiels zurück.
	 * GET /api/v2/schedule/match/{matchId}/report
	 */
	@GetMapping("/match/{matchId}/report")
	public ResponseEntity<?> getMatchReport(@PathVariable Long matchId) {
		try {
			MatchReportDTO report = repositoryService.getMatchReport(matchId);
			return ResponseEntity.ok(report);
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(e.getMessage());
		}
	}

	/**
	 * Gibt die Spielerstatistiken der Liga zurück - liga-abhängig.
	 * GET /api/v2/schedule/extended-statistics/league/{leagueId}
	 */
	@GetMapping("/extended-statistics/league/{leagueId}")
	public ResponseEntity<LeagueStatisticsDTO> getExtendedStatistics(@PathVariable Long leagueId) {
		LeagueStatisticsDTO stats = repositoryService.getExtendedLeagueStatistics(leagueId);
		return ResponseEntity.ok(stats);
	}

	/**
	 * Gibt alle verfügbaren Ligen zurück (für Ligawechsel).
	 * GET /api/v2/schedule/leagues
	 */
	@GetMapping("/leagues")
	public ResponseEntity<List<LeagueInfoDTO>> getAvailableLeagues() {
		List<LeagueInfoDTO> leagues = repositoryService.getAvailableLeagues();
		return ResponseEntity.ok(leagues);
	}

	/**
	 * Gibt die Tabelle einer spezifischen Liga zurück.
	 * GET /api/v2/schedule/standings/league/{leagueId}
	 */
	@GetMapping("/standings/league/{leagueId}")
	public ResponseEntity<List<LeagueStandingsDTO>> getLeagueStandings(@PathVariable Long leagueId) {
		List<LeagueStandingsDTO> standings = repositoryService.getLeagueStandingsByLeagueId(leagueId);
		return ResponseEntity.ok(standings);
	}

	/**
	 * Simuliert die ganze Saison (alle Spieltage) bis zum Ende.
	 * POST /api/v2/schedule/simulate-season
	 */
	@PostMapping("/simulate-season")
	public ResponseEntity<Map<String, Object>> simulateSeason() {
		Map<String, Object> result = repositoryService.simulateEntireSeasonFast();
		return ResponseEntity.ok(result);
	}

	/**
	 * Simuliert alle ausstehenden Spiele und erhöht den Spieltag.
	 * POST /api/v2/schedule/advance-matchday
	 */
	@PostMapping("/advance-matchday")
	public ResponseEntity<Map<String, Object>> advanceMatchday() {
		Map<String, Object> result = repositoryService.advanceToNextMatchday();
		return ResponseEntity.ok(result);
	}
}
