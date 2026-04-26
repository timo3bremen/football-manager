package com.example.manager.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.manager.model.Team;
import com.example.manager.repository.TeamRepository;

/**
 * REST Controller for Ticket Price and Fan Satisfaction management
 */
@RestController
@RequestMapping("/api/v2/tickets")
@CrossOrigin(origins = "*")
public class TicketPriceController {

	@Autowired
	private TeamRepository teamRepository;

	/**
	 * Get ticket prices and fan satisfaction for a team
	 * GET /api/v2/tickets/team/{teamId}
	 */
	@GetMapping("/team/{teamId}")
	public ResponseEntity<?> getTicketInfo(@PathVariable Long teamId) {
		try {
			Team team = teamRepository.findById(teamId).orElse(null);
			
			if (team == null) {
				return ResponseEntity.status(404).body("Team not found");
			}

		Map<String, Object> result = new HashMap<>();
		result.put("teamId", team.getId());
		result.put("teamName", team.getName());
		result.put("ticketPriceStanding", team.getTicketPriceStanding());
		result.put("ticketPriceSeated", team.getTicketPriceSeated());
		result.put("ticketPriceVip", team.getTicketPriceVip());
		result.put("fanSatisfaction", team.getFanSatisfaction());
		result.put("stadiumCapacityStanding", team.getStadiumCapacityStanding());
		result.put("stadiumCapacitySeated", team.getStadiumCapacitySeated());
		result.put("stadiumCapacityVip", team.getStadiumCapacityVip());
		result.put("stadiumCapacityTotal", team.getStadiumCapacity());

		return ResponseEntity.ok(result);
		} catch (Exception e) {
			return ResponseEntity.status(500).body("Error: " + e.getMessage());
		}
	}

	/**
	 * Update ticket prices for a team
	 * PUT /api/v2/tickets/team/{teamId}
	 * Body: { ticketPriceStanding, ticketPriceSeated, ticketPriceVip }
	 */
	@PutMapping("/team/{teamId}")
	public ResponseEntity<?> updateTicketPrices(@PathVariable Long teamId, 
			@RequestBody Map<String, Object> request) {
		try {
			Team team = teamRepository.findById(teamId).orElse(null);
			
			if (team == null) {
				return ResponseEntity.status(404).body("Team not found");
			}

			// Update prices if provided
			if (request.containsKey("ticketPriceStanding")) {
				int price = ((Number) request.get("ticketPriceStanding")).intValue();
				team.setTicketPriceStanding(price);
			}

			if (request.containsKey("ticketPriceSeated")) {
				int price = ((Number) request.get("ticketPriceSeated")).intValue();
				team.setTicketPriceSeated(price);
			}

			if (request.containsKey("ticketPriceVip")) {
				int price = ((Number) request.get("ticketPriceVip")).intValue();
				team.setTicketPriceVip(price);
			}

			teamRepository.save(team);

			Map<String, Object> result = new HashMap<>();
			result.put("teamId", team.getId());
			result.put("teamName", team.getName());
			result.put("ticketPriceStanding", team.getTicketPriceStanding());
			result.put("ticketPriceSeated", team.getTicketPriceSeated());
			result.put("ticketPriceVip", team.getTicketPriceVip());
			result.put("fanSatisfaction", team.getFanSatisfaction());
			result.put("stadiumCapacityStanding", team.getStadiumCapacityStanding());
			result.put("stadiumCapacitySeated", team.getStadiumCapacitySeated());
			result.put("stadiumCapacityVip", team.getStadiumCapacityVip());
			result.put("message", "Ticket prices updated successfully");

			return ResponseEntity.ok(result);
		} catch (Exception e) {
			return ResponseEntity.status(400).body("Error: " + e.getMessage());
		}
	}
}
