package com.example.manager.controller;

import com.example.manager.dto.TransactionDTO;
import com.example.manager.model.Team;
import com.example.manager.model.Transaction;
import com.example.manager.repository.TeamRepository;
import com.example.manager.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for Transaction operations
 */
@RestController
@RequestMapping("/api/v2/transactions")
@CrossOrigin(origins = "*")
public class TransactionController {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private TeamRepository teamRepository;

    /**
     * Get all transactions for a team
     * GET /api/v2/transactions/team/{teamId}
     */
    @GetMapping("/team/{teamId}")
    public ResponseEntity<?> getTeamTransactions(@PathVariable Long teamId) {
        try {
            List<Transaction> transactions = transactionRepository.findByTeamIdOrderByCreatedAtDesc(teamId);
            List<TransactionDTO> dtos = new ArrayList<>();

            for (Transaction t : transactions) {
                TransactionDTO dto = new TransactionDTO();
                dto.setId(t.getId());
                dto.setTeamId(t.getTeamId());
                dto.setAmount(t.getAmount());
                dto.setType(t.getType());
                dto.setDescription(t.getDescription());
                dto.setCategory(t.getCategory());
                dto.setCreatedAt(t.getCreatedAt());
                dtos.add(dto);
            }

            return ResponseEntity.ok(dtos);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    /**
     * Add a new transaction
     * POST /api/v2/transactions
     * Body: { teamId, amount, type, description }
     */
    @PostMapping
    public ResponseEntity<?> addTransaction(@RequestBody Map<String, Object> request) {
        try {
            Long teamId = ((Number) request.get("teamId")).longValue();
            Long amount = ((Number) request.get("amount")).longValue();
            String type = (String) request.get("type");
            String description = (String) request.get("description");
            String category = (String) request.getOrDefault("category", "other");

            // Verify team exists
            if (!teamRepository.existsById(teamId)) {
                return ResponseEntity.status(404).body("Team not found");
            }

            // Create transaction
            Transaction transaction = new Transaction(teamId, amount, type, description, category);
            Transaction saved = transactionRepository.save(transaction);

            // Update team budget
            Team team = teamRepository.findById(teamId).orElse(null);
            if (team != null) {
                team.setBudget(team.getBudgetAsLong() + amount);
                teamRepository.save(team);
            }

            TransactionDTO dto = new TransactionDTO();
            dto.setId(saved.getId());
            dto.setTeamId(saved.getTeamId());
            dto.setAmount(saved.getAmount());
            dto.setType(saved.getType());
            dto.setDescription(saved.getDescription());
            dto.setCategory(saved.getCategory());
            dto.setCreatedAt(saved.getCreatedAt());

            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            return ResponseEntity.status(400).body("Error: " + e.getMessage());
        }
    }

    /**
     * Clear all transactions for a team
     * DELETE /api/v2/transactions/team/{teamId}
     */
    @DeleteMapping("/team/{teamId}")
    public ResponseEntity<?> clearTeamTransactions(@PathVariable Long teamId) {
        try {
            transactionRepository.deleteByTeamId(teamId);
            return ResponseEntity.ok(new HashMap<String, String>() {{
                put("message", "All transactions cleared for team " + teamId);
            }});
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }
}
