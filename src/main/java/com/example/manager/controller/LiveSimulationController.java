package com.example.manager.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.manager.dto.LiveSimulationStatusDTO;
import com.example.manager.dto.SimulationMessageDTO;
import com.example.manager.service.LiveMatchSimulationService;
import com.example.manager.service.RepositoryService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST Controller für Live-Match-Simulation
 * Nur das Spiel des aktuellen Teams wird simuliert
 */
@RestController
@RequestMapping("/api/v2/live-simulation")
@CrossOrigin(origins = "*")
public class LiveSimulationController {

	@Autowired
	private LiveMatchSimulationService liveSimulationService;
	
	@Autowired
	private RepositoryService repositoryService;
	
	/**
	 * Startet die Live-Simulation nur für das Spiel des eingeloggten Teams
	 * POST /api/v2/live-simulation/start/{teamId}
	 */
	@PostMapping("/start/{teamId}")
	public ResponseEntity<?> startSimulation(@PathVariable Long teamId) {
		try {
			// Starte nur das Spiel des eingeloggten Teams
			liveSimulationService.startLiveSimulationForTeam(teamId);
			
			Map<String, Object> response = new HashMap<>();
			response.put("success", true);
			response.put("message", "Live-Simulation für dein Team gestartet!");
			response.put("duration", 270);
			
			return ResponseEntity.ok(response);
		} catch (IllegalStateException | IllegalArgumentException e) {
			Map<String, String> error = new HashMap<>();
			error.put("error", e.getMessage());
			return ResponseEntity.badRequest().body(error);
		}
	}
	
	/**
	 * Gibt den aktuellen Status der Simulation zurück
	 * GET /api/v2/live-simulation/status
	 */
	@GetMapping("/status")
	public ResponseEntity<LiveSimulationStatusDTO> getStatus() {
		LiveSimulationStatusDTO status = liveSimulationService.getSimulationStatus();
		return ResponseEntity.ok(status);
	}
	
	/**
	 * Wechselt einen Spieler während der Live-Simulation aus
	 * POST /api/v2/live-simulation/substitute
	 */
	@PostMapping("/substitute")
	public ResponseEntity<?> substitutePlayer(@RequestBody com.example.manager.dto.SubstitutionRequestDTO request) {
		try {
			liveSimulationService.substitutePlayer(
				request.getMatchId(),
				request.getTeamId(),
				request.getPlayerOutId(),
				request.getPlayerInId()
			);
			
			Map<String, Object> response = new HashMap<>();
			response.put("success", true);
			response.put("message", "Spieler erfolgreich ausgewechselt!");
			
			return ResponseEntity.ok(response);
		} catch (IllegalArgumentException | IllegalStateException e) {
			Map<String, String> error = new HashMap<>();
			error.put("error", e.getMessage());
			return ResponseEntity.badRequest().body(error);
		}
	}
	
	/**
	 * Lädt alle gespeicherten Nachrichten für ein Match
	 * GET /api/v2/live-simulation/messages/{matchId}
	 */
	@GetMapping("/messages/{matchId}")
	public ResponseEntity<List<SimulationMessageDTO>> getMessages(@PathVariable Long matchId) {
		try {
			List<SimulationMessageDTO> messages = liveSimulationService.getSimulationMessages(matchId);
			return ResponseEntity.ok(messages);
		} catch (Exception e) {
			return ResponseEntity.badRequest().build();
		}
	}
}
