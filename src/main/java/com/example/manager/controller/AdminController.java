package com.example.manager.controller;

import com.example.manager.model.Team;
import com.example.manager.service.RepositoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminController {

	@Autowired
	private RepositoryService repositoryService;

	/**
	 * Löscht ALLE Teams, Spieler und User aus dem System.
	 * ACHTUNG: Diese Operation ist nicht rückgängig zu machen!
	 * DELETE /api/admin/clear-all
	 */
	@DeleteMapping("/clear-all")
	public ResponseEntity<?> clearAll() {
		try {
			System.out.println("[AdminController] clearAll: Deleting all teams, players and users");

			// Clear all users (which also cascades to delete their teams)
			repositoryService.clearUsers();

			System.out.println("[AdminController] clearAll: All data deleted successfully");

			return ResponseEntity.ok(Map.of("success", true, "message",
					"All teams, players and users have been deleted"));
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.status(500).body(Map.of("success", false, "error", e.getMessage()));
		}
	}

	/**
	 * Initialisiert die 7 Standard-Ligen mit CPU-Teams.
	 * POST /api/admin/initialize-leagues
	 */
	@PostMapping("/initialize-leagues")
	public ResponseEntity<?> initializeLeagues() {
		try {
			repositoryService.initializeLigues();
			return ResponseEntity.ok(Map.of("success", true, "message", "7 Ligen mit 84 CPU-Teams initialisiert"));
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.status(500).body(Map.of("success", false, "error", e.getMessage()));
		}
	}
}
