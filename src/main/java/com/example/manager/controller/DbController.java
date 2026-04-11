package com.example.manager.controller;

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

    public DbController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
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
}
