package com.example.manager.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.manager.model.FreeAgentOffer;
import com.example.manager.model.Player;
import com.example.manager.model.Team;
import com.example.manager.repository.TeamRepository;
import com.example.manager.service.TransferMarketService;

/**
 * REST Controller für Transfermarkt-Operationen
 */
@RestController
@RequestMapping("/api/v2/transfer-market")
@CrossOrigin(origins = "*")
public class TransferMarketController {

	@Autowired
	private TransferMarketService transferMarketService;

	@Autowired
	private TeamRepository teamRepository;

	@Autowired
	private com.example.manager.repository.FreeAgentOfferRepository freeAgentOfferRepository;

	/**
	 * Gibt alle Angebote ab, die ein Team abgegeben hat GET
	 * /api/v2/transfer-market/my-offers/outgoing
	 */
	@GetMapping("/my-offers/outgoing")
	public ResponseEntity<List<Map<String, Object>>> getMyOutgoingOffers(@RequestParam Long teamId) {
		List<Map<String, Object>> offers = transferMarketService.getOutgoingOffers(teamId);
		return ResponseEntity.ok(offers);
	}

	/**
	 * Gibt alle Angebote ein, die für Spieler eines Teams eingegangen sind GET
	 * /api/v2/transfer-market/my-offers/incoming
	 */
	@GetMapping("/my-offers/incoming")
	public ResponseEntity<List<Map<String, Object>>> getMyIncomingOffers(@RequestParam Long teamId) {
		List<Map<String, Object>> offers = transferMarketService.getIncomingOffers(teamId);
		return ResponseEntity.ok(offers);
	}

	/**
	 * Macht ein Angebot für einen Spieler POST /api/v2/transfer-market/offer Body:
	 * { playerId: Long, buyingTeamId: Long, offerPrice: Long }
	 */
	@PostMapping("/offer")
	public ResponseEntity<Map<String, Object>> makeOffer(@RequestBody Map<String, Object> request) {
		try {
			Long playerId = ((Number) request.get("playerId")).longValue();
			Long buyingTeamId = ((Number) request.get("buyingTeamId")).longValue();
			Long offerPrice = ((Number) request.get("offerPrice")).longValue();

			Map<String, Object> result = transferMarketService.makeOffer(playerId, buyingTeamId, offerPrice);
			return ResponseEntity.ok(result);
		} catch (Exception e) {
			return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
		}
	}

	/**
	 * Nimmt ein Angebot an und verarbeitet den Transfer POST
	 * /api/v2/transfer-market/accept-offer Body: { offerId: Long, teamId: Long }
	 */
	@PostMapping("/accept-offer")
	public ResponseEntity<Map<String, Object>> acceptOffer(@RequestBody Map<String, Object> request) {
		try {
			Long offerId = ((Number) request.get("offerId")).longValue();
			Long teamId = ((Number) request.get("teamId")).longValue();

			Map<String, Object> result = transferMarketService.acceptOffer(offerId, teamId);
			if ((Boolean) result.get("success")) {
				return ResponseEntity.ok(result);
			} else {
				return ResponseEntity.badRequest().body(result);
			}
		} catch (Exception e) {
			return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
		}
	}

	/**
	 * Gibt alle verfügbaren Spieler auf dem Transfermarkt zurück (alle Spieler
	 * außer des eigenen Teams) GET
	 * /api/v2/transfer-market/available?teamId=123&onTransferList=true&position=GK&minRating=75&maxRating=90&freeAgentsOnly=true
	 * Optional: onTransferList Parameter um nur Transferlisten-Spieler zu filtern
	 * Optional: freeAgentsOnly Parameter um nur freie Spieler anzuzeigen
	 * Optional: position, minRating, maxRating für erweiterte Suche
	 */
	@GetMapping("/available")
	public ResponseEntity<List<Map<String, Object>>> getAvailablePlayers(@RequestParam(required = false) Long teamId,
			@RequestParam(required = false) Boolean onTransferList,
			@RequestParam(required = false) Boolean freeAgentsOnly,
			@RequestParam(required = false) String position,
			@RequestParam(required = false) Integer minRating, @RequestParam(required = false) Integer maxRating) {
		List<Player> players = transferMarketService.getAvailablePlayers().stream()
				// Filtere eigene Spieler NUR aus wenn nicht freeAgentsOnly
				.filter(p -> {
					if (freeAgentsOnly != null && freeAgentsOnly) {
						// Bei freeAgentsOnly: Zeige alle freien Spieler, egal welches Team
						return p.isFreeAgent();
					} else {
						// Normal: Filtere nur Spieler des eigenen Teams aus
						return teamId == null || (p.getTeamId() != null && !p.getTeamId().equals(teamId));
					}
				})
				// Filtere optional nach onTransferList Status
				.filter(p -> onTransferList == null || p.isOnTransferList() == onTransferList)
				// Filtere optional nach Position
				.filter(p -> position == null || position.isEmpty() || position.equals(p.getPosition()))
				// Filtere optional nach minRating
				.filter(p -> minRating == null || p.getRating() >= minRating)
				// Filtere optional nach maxRating
				.filter(p -> maxRating == null || p.getRating() <= maxRating).collect(Collectors.toList());

		// Enriche mit Teamnamen und Angebots-Status
		final Long currentTeamId = teamId; // Final für Lambda
		List<Map<String, Object>> result = players.stream()
				.map(p -> enrichPlayerWithTeamNameAndOffer(p, currentTeamId))
				.collect(Collectors.toList());

		return ResponseEntity.ok(result);
	}

	/**
	 * Listet einen Spieler eines Teams zum Verkauf an POST
	 * /api/v2/transfer-market/list/{playerId} Body: { teamId: Long }
	 */
	@PostMapping("/list/{playerId}")
	public ResponseEntity<Player> listPlayerForSale(@PathVariable Long playerId, @RequestParam Long teamId) {
		Player player = transferMarketService.listPlayerForSale(playerId, teamId);
		if (player != null) {
			return ResponseEntity.ok(player);
		}
		return ResponseEntity.badRequest().build();
	}

	/**
	 * Entfernt einen Spieler von der Transferliste DELETE
	 * /api/v2/transfer-market/list/{playerId} Body: { teamId: Long }
	 */
	@DeleteMapping("/list/{playerId}")
	public ResponseEntity<Player> removePlayerFromSale(@PathVariable Long playerId, @RequestParam Long teamId) {
		Player player = transferMarketService.removePlayerFromSale(playerId, teamId);
		if (player != null) {
			return ResponseEntity.ok(player);
		}
		return ResponseEntity.badRequest().build();
	}

	/**
	 * Kauft einen Spieler für ein Team POST /api/v2/transfer-market/buy/{playerId}
	 * Body: { teamId: Long }
	 */
	@PostMapping("/buy/{playerId}")
	public ResponseEntity<Player> buyPlayer(@PathVariable Long playerId, @RequestParam Long teamId) {
		Player player = transferMarketService.buyPlayer(playerId, teamId);
		if (player != null) {
			return ResponseEntity.ok(player);
		}
		return ResponseEntity.badRequest().build();
	}

	/**
	 * Sucht nach Spielern basierend auf Kriterien GET
	 * /api/v2/transfer-market/search Optionale Parameter: position, minRating,
	 * maxRating
	 */
	@GetMapping("/search")
	public ResponseEntity<List<Map<String, Object>>> searchPlayers(@RequestParam(required = false) String position,
			@RequestParam(required = false) Integer minRating, @RequestParam(required = false) Integer maxRating) {
		List<Player> players = transferMarketService.searchPlayers(position, minRating, maxRating);

		// Enriche mit Teamnamen
		List<Map<String, Object>> result = players.stream().map(this::enrichPlayerWithTeamName)
				.collect(Collectors.toList());

		return ResponseEntity.ok(result);
	}

	/**
	 * Hilfsmethode: Enriche Spieler mit Teamnamen
	 */
	private Map<String, Object> enrichPlayerWithTeamName(Player player) {
		return enrichPlayerWithTeamNameAndOffer(player, null);
	}

	/**
	 * Hilfsmethode: Enriche Spieler mit Teamnamen und Angebots-Status
	 */
	private Map<String, Object> enrichPlayerWithTeamNameAndOffer(Player player, Long checkingTeamId) {
		Map<String, Object> map = new HashMap<>();
		map.put("id", player.getId());
		map.put("name", player.getName());
		map.put("position", player.getPosition());
		map.put("country", player.getCountry());
		map.put("age", player.getAge());
		map.put("rating", player.getRating());
		map.put("potential", player.getOverallPotential());
		map.put("salary", player.getSalary());
		map.put("marketValue", player.getMarketValue());
		map.put("contractLength", player.getContractLength());
		map.put("teamId", player.getTeamId());
		map.put("onTransferList", player.isOnTransferList());
		map.put("isFreeAgent", player.isFreeAgent());

		// Füge Teamnamen hinzu wenn verfügbar
		if (player.getTeamId() != null && player.getTeamId() > 0) {
			Team team = teamRepository.findById(player.getTeamId()).orElse(null);
			map.put("teamName", team != null ? team.getName() : null);
		} else if (player.isFreeAgent()) {
			map.put("teamName", "Frei");
		}

		// Prüfe ob User bereits ein Angebot für diesen freien Spieler gemacht hat
		if (player.isFreeAgent() && checkingTeamId != null) {
			FreeAgentOffer existingOffer = freeAgentOfferRepository
					.findByPlayerIdAndTeamId(player.getId(), checkingTeamId)
					.orElse(null);
			map.put("hasOffer", existingOffer != null);
			if (existingOffer != null) {
				map.put("offerStatus", existingOffer.getStatus());
			}
		} else {
			map.put("hasOffer", false);
		}

		return map;
	}

	/**
	 * Manueller Endpoint um CPU-Angebote zu generieren (für Tests/Debugging) POST
	 * /api/v2/transfer-market/generate-cpu-offers
	 */
	@PostMapping("/generate-cpu-offers")
	public ResponseEntity<Map<String, Object>> generateCPUOffersManual() {
		try {
			transferMarketService.generateCPUOffers();
			return ResponseEntity.ok(Map.of("success", true, "message", "CPU Angebote wurden generiert"));
		} catch (Exception e) {
			return ResponseEntity.status(500).body(Map.of("success", false, "error", e.getMessage()));
		}
	}

	/**
	 * Manueller Endpoint um CPU-Team Angebote zu verarbeiten (für Tests/Debugging)
	 * POST /api/v2/transfer-market/process-cpu-offers
	 */
	@PostMapping("/process-cpu-offers")
	public ResponseEntity<Map<String, Object>> processCPUOffersManual() {
		try {
			transferMarketService.processCPUOfferResponses();
			return ResponseEntity.ok(Map.of("success", true, "message", "CPU-Angebote wurden verarbeitet"));
		} catch (Exception e) {
			return ResponseEntity.status(500).body(Map.of("success", false, "error", e.getMessage()));
		}
	}

	/**
	 * Schließt eine Gehaltsverhandlung ab und führt den Transfer durch POST
	 * /api/v2/transfer-market/complete-negotiation Body: { offerId: Long, teamId:
	 * Long, newSalary: Long, contractLength: Integer }
	 */
	@PostMapping("/complete-negotiation")
	public ResponseEntity<Map<String, Object>> completeNegotiation(@RequestBody Map<String, Object> request) {
		try {
			System.out.println("[TransferMarketController] Complete negotiation request: " + request);

			if (!request.containsKey("offerId") || !request.containsKey("teamId") || !request.containsKey("newSalary")
					|| !request.containsKey("contractLength")) {
				return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Fehlende Parameter"));
			}

			Long offerId = null;
			Long teamId = null;
			Long newSalary = null;
			Integer contractLength = null;

			try {
				offerId = ((Number) request.get("offerId")).longValue();
				teamId = ((Number) request.get("teamId")).longValue();
				newSalary = ((Number) request.get("newSalary")).longValue();
				contractLength = ((Number) request.get("contractLength")).intValue();
			} catch (ClassCastException | NumberFormatException e) {
				System.err.println("[TransferMarketController] Parameter conversion error: " + e.getMessage());
				return ResponseEntity.badRequest()
						.body(Map.of("success", false, "message", "Ungültige Parameter: " + e.getMessage()));
			}

			System.out.println("[TransferMarketController] Parameters: offerId=" + offerId + ", teamId=" + teamId
					+ ", newSalary=" + newSalary + ", contractLength=" + contractLength);

			Map<String, Object> result = transferMarketService.completeNegotiation(offerId, teamId, newSalary,
					contractLength);

			if ((Boolean) result.get("success")) {
				return ResponseEntity.ok(result);
			} else {
				return ResponseEntity.badRequest().body(result);
			}
		} catch (Exception e) {
			System.err.println("[TransferMarketController] Error in completeNegotiation: " + e.getMessage());
			e.printStackTrace();
			return ResponseEntity.status(500).body(Map.of("success", false, "error", e.getMessage()));
		}
	}

	/**
	 * Lehnt ein Angebot ab (entfernt es) DELETE
	 * /api/v2/transfer-market/reject-offer/{offerId}
	 */
	@DeleteMapping("/reject-offer/{offerId}")
	public ResponseEntity<Map<String, Object>> rejectOffer(@PathVariable Long offerId) {
		try {
			Map<String, Object> result = transferMarketService.rejectOffer(offerId);

			if ((Boolean) result.get("success")) {
				return ResponseEntity.ok(result);
			} else {
				return ResponseEntity.badRequest().body(result);
			}
		} catch (Exception e) {
			return ResponseEntity.status(500).body(Map.of("success", false, "error", e.getMessage()));
		}
	}
}