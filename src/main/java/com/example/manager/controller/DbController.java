package com.example.manager.controller;

import com.example.manager.service.LineupService;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/db")
public class DbController {

    private final JdbcTemplate jdbc;
    private final LineupService lineupService;

    public DbController(JdbcTemplate jdbc, LineupService lineupService) {
        this.jdbc = jdbc;
        this.lineupService = lineupService;
    }

    @GetMapping("/dummy")
    public ResponseEntity<List<Map<String, Object>>> listDummy() {
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT * FROM DUMMY_PLAYER");
        return ResponseEntity.ok(rows);
    }

    @GetMapping("/dummy/{id}")
    public ResponseEntity<Map<String, Object>> getDummy(@PathVariable("id") long id) {
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT * FROM DUMMY_PLAYER WHERE ID = ?", id);
        if (rows.isEmpty()) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(rows.get(0));
    }

    @GetMapping("/teams")
    public ResponseEntity<List<Map<String, Object>>> listTeams() {
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT * FROM DUMMY_TEAM");
        return ResponseEntity.ok(rows);
    }

    @GetMapping("/teams/{id}")
    public ResponseEntity<Map<String, Object>> getTeam(@PathVariable("id") long id) {
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT * FROM DUMMY_TEAM WHERE ID = ?", id);
        if (rows.isEmpty()) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(rows.get(0));
    }

    /**
     * Cleanup-Endpoint: Entfernt alle alten/gelöschten Spieler-IDs aus den Lineups eines Teams.
     * GET /api/db/cleanup/team/{teamId}
     */
    @GetMapping("/cleanup/team/{teamId}")
    public ResponseEntity<?> cleanupTeamLineups(@PathVariable Long teamId) {
        System.out.println("[DbController] cleanup called for teamId=" + teamId);
        int deletedCount = lineupService.cleanupInvalidPlayerIds(teamId);
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Cleanup completed",
            "teamId", teamId,
            "deletedInvalidPlayerIds", deletedCount
        ));
    }
}
