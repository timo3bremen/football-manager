package com.example.manager.controller;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.manager.dto.ContractNegotiationRequest;
import com.example.manager.dto.ContractNegotiationResponse;
import com.example.manager.dto.PlayerPerformanceDTO;
import com.example.manager.dto.PlayerStatsDTO;
import com.example.manager.model.ContractNegotiation;
import com.example.manager.model.FreeAgent;
import com.example.manager.model.FreeAgentOffer;
import com.example.manager.model.Match;
import com.example.manager.model.Matchday;
import com.example.manager.model.Player;
import com.example.manager.model.PlayerPerformance;
import com.example.manager.model.Team;
import com.example.manager.repository.MatchRepository;
import com.example.manager.repository.MatchdayRepository;
import com.example.manager.repository.PlayerPerformanceRepository;
import com.example.manager.repository.PlayerRepository;
import com.example.manager.repository.TeamRepository;

/**
 * REST Controller für erweiterte Player-Management Operations. Für
 * Basis-Endpoints siehe ManagerController.
 */
@RestController
@RequestMapping("/api/v2/players")
@CrossOrigin(origins = "*")
public class PlayerController {

	@Autowired
	private PlayerRepository playerRepository;

	@Autowired
	private TeamRepository teamRepository;

	@Autowired
	private PlayerPerformanceRepository playerPerformanceRepository;

	@Autowired
	private MatchRepository matchRepository;

	@Autowired
	private MatchdayRepository matchdayRepository;

	@Autowired
	private com.example.manager.repository.ContractNegotiationRepository contractNegotiationRepository;

	@Autowired
	private com.example.manager.repository.FreeAgentRepository freeAgentRepository;

	@Autowired
	private com.example.manager.repository.FreeAgentOfferRepository freeAgentOfferRepository;

	/**
	 * Lädt alle Spieler, sortiert nach Marktwert absteigend. GET /api/v2/players
	 */
	@GetMapping
	public ResponseEntity<List<Map<String, Object>>> getAllPlayers() {
		List<Player> players = playerRepository.findAll();
		// Sortiere nach Marktwert absteigend
		players.sort((p1, p2) -> Long.compare(p2.getMarketValue(), p1.getMarketValue()));

		// Enriche mit Teamnamen
		List<Map<String, Object>> result = players.stream().map(this::enrichPlayerWithTeamName)
				.collect(Collectors.toList());

		return ResponseEntity.ok(result);
	}

	/**
	 * Hilfsmethode: Enriche Spieler mit Teamnamen
	 */
	private Map<String, Object> enrichPlayerWithTeamName(Player player) {
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
		map.put("fitness", player.getFitness());
		map.put("isFreeAgent", player.isFreeAgent());

		// Füge Teamnamen hinzu wenn verfügbar
		if (player.getTeamId() != null && player.getTeamId() > 0) {
			Team team = teamRepository.findById(player.getTeamId()).orElse(null);
			map.put("teamName", team != null ? team.getName() : null);
		} else if (player.isFreeAgent()) {
			map.put("teamName", "Frei");
		}

		return map;
	}

	/**
	 * Lädt alle Spieler eines Teams, sortiert nach Position (GK, DEF, MID, FWD) und
	 * Rating. GET /api/v2/players/team/{teamId}
	 */
	@GetMapping("/team/{teamId}")
	public ResponseEntity<List<Map<String, Object>>> getPlayersByTeam(@PathVariable Long teamId) {
		List<Player> players = playerRepository.findByTeamId(teamId);

		// Sort by position order and then by rating (descending)
		players.sort(new Comparator<Player>() {
			private int getPositionOrder(String pos) {
				if ("GK".equals(pos))
					return 0;
				if ("DEF".equals(pos))
					return 1;
				if ("MID".equals(pos))
					return 2;
				if ("FWD".equals(pos))
					return 3;
				return 4;
			}

			@Override
			public int compare(Player p1, Player p2) {
				int posOrder1 = getPositionOrder(p1.getPosition());
				int posOrder2 = getPositionOrder(p2.getPosition());

				if (posOrder1 != posOrder2) {
					return Integer.compare(posOrder1, posOrder2);
				}
				// Same position, sort by rating descending
				return Integer.compare(p2.getRating(), p1.getRating());
			}
		});

		// Enriche mit Teamnamen
		List<Map<String, Object>> result = players.stream().map(this::enrichPlayerWithTeamName)
				.collect(Collectors.toList());

		return ResponseEntity.ok(result);
	}

	/**
	 * Lädt einen einzelnen Spieler. GET /api/v2/players/{id}
	 */
	@GetMapping("/{id}")
	public ResponseEntity<Player> getPlayer(@PathVariable Long id) {
		return playerRepository.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
	}

	/**
	 * Speichert/aktualisiert einen Spieler. POST /api/v2/players
	 */
	@PostMapping
	public ResponseEntity<Player> savePlayer(@RequestBody Player player) {
		Player saved = playerRepository.save(player);
		return ResponseEntity.ok(saved);
	}

	/**
	 * Löscht einen Spieler. DELETE /api/v2/players/{id}
	 */
	@DeleteMapping("/{id}")
	public ResponseEntity<?> deletePlayer(@PathVariable Long id) {
		playerRepository.deleteById(id);
		return ResponseEntity.ok("Player deleted");
	}

	/**
	 * Gibt alle Spieler eines Teams mit Vertragsinformationen zurück. GET
	 * /api/v2/players/team/{teamId}/contracts
	 */
	@GetMapping("/team/{teamId}/contracts")
	public ResponseEntity<List<Map<String, Object>>> getPlayerContracts(@PathVariable Long teamId) {
		List<Player> players = playerRepository.findByTeamId(teamId);

		List<Map<String, Object>> result = players.stream().map(this::enrichPlayerWithTeamName)
				.collect(Collectors.toList());

		// Sort by rating descending (highest first)
		result.sort((a, b) -> {
			Integer ratingA = (Integer) a.get("rating");
			Integer ratingB = (Integer) b.get("rating");
			return Integer.compare(ratingB != null ? ratingB : 0, ratingA != null ? ratingA : 0);
		});

		return ResponseEntity.ok(result);
	}

	/**
	 * Verlängert den Vertrag eines Spielers mit neuem Gehalt und Laufzeit. POST
	 * /api/v2/players/{id}/extend-contract Body: { newSalary: Long,
	 * newContractLength: Integer }
	 */
	@PostMapping("/{id}/extend-contract")
	public ResponseEntity<Map<String, Object>> extendPlayerContract(@PathVariable Long id,
			@RequestBody(required = false) Map<String, Object> request) {
		Player player = playerRepository.findById(id).orElse(null);
		if (player == null) {
			Map<String, Object> error = new HashMap<>();
			error.put("success", false);
			error.put("message", "Spieler nicht gefunden");
			return ResponseEntity.badRequest().body(error);
		}

		// Prüfe ob max. Vertrag
		if (player.getContractLength() >= 5) {
			Map<String, Object> error = new HashMap<>();
			error.put("success", false);
			error.put("message", "Spieler hat bereits maximalen 5-Saisons-Vertrag");
			return ResponseEntity.badRequest().body(error);
		}

		// Wenn Request-Body vorhanden ist, nutze neue Werte
		if (request != null && request.containsKey("newSalary") && request.containsKey("newContractLength")) {
			Long newSalary = ((Number) request.get("newSalary")).longValue();
			Integer newContractLength = ((Number) request.get("newContractLength")).intValue();

			// Validierung
			if (newSalary < player.getSalary()) {
				Map<String, Object> error = new HashMap<>();
				error.put("success", false);
				error.put("message", "Spieler akzeptiert nicht - Gehalt zu niedrig");
				return ResponseEntity.badRequest().body(error);
			}

			if (newContractLength > 5) {
				Map<String, Object> error = new HashMap<>();
				error.put("success", false);
				error.put("message", "Vertrag kann max. 5 Saisons lang sein");
				return ResponseEntity.badRequest().body(error);
			}

			// Aktualisiere Spieler
			player.setSalary(newSalary);
			player.setContractLength(newContractLength);
		} else {
			// Fallback: Verlängere um eine Saison (alte Logik)
			int newContractLength = Math.min(5, player.getContractLength() + 1);
			player.setContractLength(newContractLength);
		}

		player.calculateMarketValue();
		playerRepository.save(player);

		Map<String, Object> response = new HashMap<>();
		response.put("success", true);
		response.put("message", "Vertrag verlängert");
		response.put("player", enrichPlayerWithTeamName(player));

		return ResponseEntity.ok(response);
	}

	/**
	 * Verhandelt einen neuen Vertrag mit einem Spieler. POST
	 * /api/v2/players/{id}/negotiate-contract Body: { proposedSalary: Long,
	 * proposedContractLength: Integer }
	 * 
	 * Logik: - Spieler erwartet mindestens aktuelles Gehalt + 10-20% - Spieler
	 * erwartet mindestens 2 Jahre Vertragslaufzeit - Bei sehr niedrigem Angebot (<
	 * aktuelles Gehalt) wird Verhandlung abgebrochen - Feedback wird als Smileys
	 * gegeben: happy, neutral, unhappy - Max 3 Versuche pro Saison, dann wird
	 * Spieler zum freien Agenten
	 */
	@PostMapping("/{id}/negotiate-contract")
	@Transactional
	public ResponseEntity<ContractNegotiationResponse> negotiateContract(@PathVariable Long id,
			@RequestBody ContractNegotiationRequest request) {
		Player player = playerRepository.findById(id).orElse(null);
		if (player == null) {
			ContractNegotiationResponse error = new ContractNegotiationResponse();
			error.setAccepted(false);
			error.setNegotiationAborted(true);
			error.setMessage("Spieler nicht gefunden");
			return ResponseEntity.badRequest().body(error);
		}

		// Prüfe ob max. Vertrag
		if (player.getContractLength() >= 5) {
			ContractNegotiationResponse error = new ContractNegotiationResponse();
			error.setAccepted(false);
			error.setNegotiationAborted(true);
			error.setMessage("Spieler hat bereits maximalen 5-Saisons-Vertrag");
			return ResponseEntity.badRequest().body(error);
		}

		// Hole aktuelle Saison (hardcoded auf 1 für jetzt, später dynamisch)
		int currentSeason = 1; // TODO: Von GameState holen

		// Hole oder erstelle Verhandlungshistorie
		ContractNegotiation negotiation = contractNegotiationRepository.findByPlayerIdAndSeason(id, currentSeason)
				.orElse(new ContractNegotiation(id, player.getTeamId(), currentSeason));

		// Prüfe ob bereits 3 Versuche gemacht wurden
		if (negotiation.isFailed()) {
			ContractNegotiationResponse error = new ContractNegotiationResponse();
			error.setAccepted(false);
			error.setNegotiationAborted(true);
			error.setMessage("Verhandlungen bereits gescheitert. Spieler wird zum Saisonende zum freien Agenten.");
			error.setAttemptCount(negotiation.getAttemptCount());
			return ResponseEntity.ok(error);
		}

		// Berechne Spieler-Erwartungen basierend auf Rating und aktuellem Gehalt
		long currentSalary = player.getSalary();

		// Mindestgehalt: Aktuelles Gehalt + 10-20% (abhängig von Rating)
		double salaryIncreasePercent = 0.10 + (player.getRating() / 1000.0); // 10-19%
		long minimumSalary = (long) (currentSalary * (1 + salaryIncreasePercent));

		// Ideales Gehalt: Aktuelles Gehalt + 15-30%
		double idealSalaryIncreasePercent = 0.15 + (player.getRating() / 500.0); // 15-34%
		long idealSalary = (long) (currentSalary * (1 + idealSalaryIncreasePercent));

		// Mindestlaufzeit: Mindestens 2 Jahre, idealerweise 3-4
		int minimumContractLength = 2;
		int idealContractLength = 3;

		long proposedSalary = request.getProposedSalary();
		int proposedLength = request.getProposedContractLength();

		ContractNegotiationResponse response = new ContractNegotiationResponse();
		response.setMinimumSalary(minimumSalary);
		response.setMinimumContractLength(minimumContractLength);

		// Validierung
		if (proposedLength < 2 || proposedLength > 5) {
			response.setAccepted(false);
			response.setNegotiationAborted(false);
			response.setMessage("Vertragslaufzeit muss zwischen 2 und 5 Saisons liegen");
			response.setSalaryFeedback("neutral");
			response.setContractLengthFeedback("unhappy");
			response.setAttemptCount(negotiation.getAttemptCount());
			return ResponseEntity.ok(response);
		}

		// Prüfe auf Verhandlungsabbruch (beide Werte sehr schlecht)
		boolean salaryTooLow = proposedSalary < currentSalary * 0.95; // Weniger als aktuelles Gehalt
		boolean contractTooShort = proposedLength < minimumContractLength;

		if (salaryTooLow && contractTooShort) {
			// Spieler bricht Verhandlung ab
			negotiation.incrementAttempt();
			negotiation.setFailed(true);
			contractNegotiationRepository.save(negotiation);

			response.setAccepted(false);
			response.setNegotiationAborted(true);
			response.setMessage(player.getName() + " bricht die Verhandlungen ab! Das Angebot ist beleidigend.");
			response.setSalaryFeedback("unhappy");
			response.setContractLengthFeedback("unhappy");
			response.setAttemptCount(negotiation.getAttemptCount());

			// Mache Spieler zum freien Agenten wenn nur noch 1 Saison Vertrag
			if (player.getContractLength() <= 1) {
				makePlayerFreeAgent(player);
			}

			return ResponseEntity.ok(response);
		}

		// Berechne Feedback für Gehalt
		String salaryFeedback;
		if (proposedSalary >= idealSalary) {
			salaryFeedback = "happy"; // 😊
		} else if (proposedSalary >= minimumSalary) {
			salaryFeedback = "neutral"; // 😐
		} else {
			salaryFeedback = "unhappy"; // 😢
		}

		// Berechne Feedback für Vertragslaufzeit
		String contractFeedback;
		if (proposedLength >= idealContractLength) {
			contractFeedback = "happy"; // 😊
		} else if (proposedLength >= minimumContractLength) {
			contractFeedback = "neutral"; // 😐
		} else {
			contractFeedback = "unhappy"; // 😢
		}

		response.setSalaryFeedback(salaryFeedback);
		response.setContractLengthFeedback(contractFeedback);

		// Entscheidung: Spieler akzeptiert nur wenn beide mindestens "neutral" sind
		boolean salaryOk = !salaryFeedback.equals("unhappy");
		boolean contractOk = !contractFeedback.equals("unhappy");

		// Erhöhe Versuchszähler
		negotiation.incrementAttempt();

		if (salaryOk && contractOk) {
			// Angebot akzeptiert!
			player.setSalary(proposedSalary);
			player.setContractLength(proposedLength);
			player.calculateMarketValue();
			playerRepository.save(player);

			// Lösche Verhandlungshistorie (erfolgreich abgeschlossen)
			contractNegotiationRepository.deleteByPlayerId(id);

			response.setAccepted(true);
			response.setNegotiationAborted(false);
			response.setMessage(player.getName() + " akzeptiert das Angebot! ✅");
			response.setAttemptCount(negotiation.getAttemptCount());
			return ResponseEntity.ok(response);
		} else {
			// Angebot abgelehnt
			contractNegotiationRepository.save(negotiation);

			response.setAccepted(false);
			response.setNegotiationAborted(false);
			response.setAttemptCount(negotiation.getAttemptCount());

			// Generiere hilfreiche Nachricht
			StringBuilder msg = new StringBuilder(player.getName() + " lehnt ab. ");
			if (!salaryOk && !contractOk) {
				msg.append("Gehalt UND Vertragslaufzeit zu niedrig!");
			} else if (!salaryOk) {
				msg.append("Gehalt zu niedrig!");
			} else {
				msg.append("Vertragslaufzeit zu kurz!");
			}

			// Wenn alle 3 Versuche aufgebraucht
			if (negotiation.isFailed()) {
				msg.append(" Alle Versuche aufgebraucht!");
				if (player.getContractLength() <= 1) {
					msg.append(" Spieler wird zum freien Agenten.");
					makePlayerFreeAgent(player);
				} else {
					msg.append(" Nächste Saison neu verhandeln.");
				}
			}

			response.setMessage(msg.toString());

			return ResponseEntity.ok(response);
		}
	}

	/**
	 * Macht einen Spieler zum freien Agenten
	 */
	private void makePlayerFreeAgent(Player player) {
		player.setTeamId(null);
		player.setFreeAgent(true);
		player.setOnTransferList(false);
		playerRepository.save(player);

		// Erstelle FreeAgent Eintrag (TODO: currentMatchday dynamisch holen)
		int currentMatchday = 1; // TODO: Von GameState holen
		FreeAgent freeAgent = new FreeAgent(player.getId(), currentMatchday);
		freeAgentRepository.save(freeAgent);

		System.out.println("[Contract] " + player.getName() + " ist jetzt freier Agent!");
	}

	/**
	 * Lädt die Verhandlungshistorie eines Spielers für die aktuelle Saison GET
	 * /api/v2/players/{id}/negotiation-history
	 */
	@GetMapping("/{id}/negotiation-history")
	public ResponseEntity<Map<String, Object>> getNegotiationHistory(@PathVariable Long id) {
		int currentSeason = 1; // TODO: Von GameState holen

		Optional<ContractNegotiation> negotiation = contractNegotiationRepository.findByPlayerIdAndSeason(id,
				currentSeason);

		Map<String, Object> result = new HashMap<>();
		if (negotiation.isPresent()) {
			ContractNegotiation n = negotiation.get();
			result.put("attemptCount", n.getAttemptCount());
			result.put("failed", n.isFailed());
			result.put("season", n.getSeason());
		} else {
			result.put("attemptCount", 0);
			result.put("failed", false);
			result.put("season", currentSeason);
		}

		return ResponseEntity.ok(result);
	}

	/**
	 * Macht ein Angebot für einen freien Spieler
	 * POST /api/v2/players/{id}/offer-free-agent
	 * Body: { teamId: Long, salary: Long, contractLength: Integer }
	 */
	@PostMapping("/{id}/offer-free-agent")
	@Transactional
	public ResponseEntity<Map<String, Object>> offerFreeAgent(
			@PathVariable Long id,
			@RequestBody Map<String, Object> request) {
		Player player = playerRepository.findById(id).orElse(null);
		if (player == null) {
			return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Spieler nicht gefunden"));
		}

		if (!player.isFreeAgent()) {
			return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Spieler ist kein freier Agent"));
		}

		Long teamId = ((Number) request.get("teamId")).longValue();
		Long salary = ((Number) request.get("salary")).longValue();
		Integer contractLength = ((Number) request.get("contractLength")).intValue();

		Team team = teamRepository.findById(teamId).orElse(null);
		if (team == null) {
			return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Team nicht gefunden"));
		}

		// Hole oder erstelle FreeAgent-Eintrag
		FreeAgent freeAgent = freeAgentRepository.findByPlayerId(id)
				.orElseThrow(() -> new IllegalStateException("FreeAgent-Eintrag nicht gefunden"));

		// Prüfe ob Team bereits ein Angebot gemacht hat
		FreeAgentOffer existingOffer = freeAgentOfferRepository.findByPlayerIdAndTeamId(id, teamId).orElse(null);
		
		if (existingOffer != null) {
			// Aktualisiere bestehendes Angebot
			existingOffer.setSalary(salary);
			existingOffer.setContractLength(contractLength);
			existingOffer.setStatus("pending");
			existingOffer.setCreatedAt(LocalDateTime.now());
			freeAgentOfferRepository.save(existingOffer);
		} else {
			// Erstelle neues Angebot
			FreeAgentOffer newOffer = new FreeAgentOffer(id, teamId, salary, contractLength);
			freeAgentOfferRepository.save(newOffer);
		}

		// Prüfe ob Angebot besser ist als aktuelles bestes Angebot
		boolean isNewOffer = freeAgent.getBestOfferSalary() == null;
		boolean isBetterOffer = freeAgent.getBestOfferSalary() == null || salary > freeAgent.getBestOfferSalary();

		if (isBetterOffer) {
			// Markiere vorheriges bestes Angebot als "outbid"
			if (freeAgent.getBestOfferTeamId() != null) {
				List<FreeAgentOffer> oldOffers = freeAgentOfferRepository.findByPlayerIdAndTeamId(
						id, freeAgent.getBestOfferTeamId()).stream().toList();
				for (FreeAgentOffer oldOffer : oldOffers) {
					oldOffer.setStatus("outbid");
					freeAgentOfferRepository.save(oldOffer);
				}
			}
			
			freeAgent.setBestOfferTeamId(teamId);
			freeAgent.setBestOfferSalary(salary);
			freeAgent.setBestOfferContractLength(contractLength);
			freeAgent.setStatus("offers_pending");
			freeAgentRepository.save(freeAgent);

			Map<String, Object> response = new HashMap<>();
			response.put("success", true);
			response.put("message", isNewOffer ? 
					"Angebot abgegeben! Spieler entscheidet sich in 2 Spieltagen." :
					"Dein Angebot ist jetzt das beste! Spieler entscheidet sich in 2 Spieltagen.");
			response.put("playerName", player.getName());
			return ResponseEntity.ok(response);
		} else {
			Map<String, Object> response = new HashMap<>();
			response.put("success", false);
			response.put("message", "Dein Angebot ist nicht gut genug. Aktuell bestes Angebot: " + freeAgent.getBestOfferSalary());
			return ResponseEntity.ok(response);
		}
	}

	/**
	 * Lädt alle Angebote eines Teams für freie Spieler
	 * GET /api/v2/players/team/{teamId}/free-agent-offers
	 */
	@GetMapping("/team/{teamId}/free-agent-offers")
	public ResponseEntity<List<Map<String, Object>>> getTeamFreeAgentOffers(@PathVariable Long teamId) {
		List<FreeAgentOffer> offers = freeAgentOfferRepository.findByTeamId(teamId);
		
		List<Map<String, Object>> result = new java.util.ArrayList<>();
		for (FreeAgentOffer offer : offers) {
			Player player = playerRepository.findById(offer.getPlayerId()).orElse(null);
			if (player != null) {
				Map<String, Object> offerMap = new HashMap<>();
				offerMap.put("id", offer.getId());
				offerMap.put("playerId", player.getId());
				offerMap.put("playerName", player.getName());
				offerMap.put("position", player.getPosition());
				offerMap.put("rating", player.getRating());
				offerMap.put("age", player.getAge());
				offerMap.put("country", player.getCountry());
				offerMap.put("offerSalary", offer.getSalary());
				offerMap.put("contractLength", offer.getContractLength());
				offerMap.put("status", offer.getStatus());
				offerMap.put("createdAt", offer.getCreatedAt());
				offerMap.put("isFreeAgent", true);
				result.add(offerMap);
			}
		}
		
		return ResponseEntity.ok(result);
	}

	/**
	 * Lädt detaillierte Statistiken eines Spielers inkl. der letzten Spiele. GET
	 * /api/v2/players/{id}/stats
	 */
	@GetMapping("/{id}/stats")
	public ResponseEntity<PlayerStatsDTO> getPlayerStats(@PathVariable Long id) {
		Player player = playerRepository.findById(id).orElse(null);
		if (player == null) {
			return ResponseEntity.notFound().build();
		}

		PlayerStatsDTO stats = new PlayerStatsDTO();

		// Grundlegende Spielerinformationen
		stats.setPlayerId(player.getId());
		stats.setName(player.getName());
		stats.setPosition(player.getPosition());
		stats.setCountry(player.getCountry());
		stats.setAge(player.getAge());
		stats.setRating(player.getRating());
		stats.setOverallPotential(player.getOverallPotential());
		stats.setFitness(player.getFitness());

		// Vertragsinformationen
		stats.setContractLength(player.getContractLength());
		stats.setSalary(player.getSalary());
		stats.setMarketValue(player.getMarketValue());

		// Lade alle Performances des Spielers
		List<PlayerPerformance> performances = playerPerformanceRepository.findByPlayerIdOrderByMatchIdDesc(id);

		// Berechne Gesamt-Statistiken
		int totalGoals = 0;
		int totalAssists = 0;
		int totalYellowCards = 0;
		int totalRedCards = 0;
		double ratingSum = 0.0;
		int ratingCount = 0;

		for (PlayerPerformance perf : performances) {
			totalGoals += perf.getGoals();
			totalAssists += perf.getAssists();
			totalYellowCards += perf.getYellowCards();
			totalRedCards += perf.getRedCards();
			if (perf.getRating() != null) {
				ratingSum += perf.getRating();
				ratingCount++;
			}
		}

		stats.setMatchesPlayed(performances.size());
		stats.setTotalGoals(totalGoals);
		stats.setTotalAssists(totalAssists);
		stats.setTotalYellowCards(totalYellowCards);
		stats.setTotalRedCards(totalRedCards);
		stats.setAverageRating(ratingCount > 0 ? Math.round(ratingSum / ratingCount * 10.0) / 10.0 : null);

		// Lade die letzten 10 Spiele
		List<PlayerPerformanceDTO> recentPerformances = new ArrayList<>();
		int limit = Math.min(10, performances.size());

		for (int i = 0; i < limit; i++) {
			PlayerPerformance perf = performances.get(i);
			Match match = matchRepository.findById(perf.getMatchId()).orElse(null);
			if (match == null)
				continue;

			Matchday matchday = matchdayRepository.findById(match.getMatchdayId()).orElse(null);

			PlayerPerformanceDTO perfDTO = new PlayerPerformanceDTO();
			perfDTO.setMatchId(perf.getMatchId());
			perfDTO.setMatchday(matchday != null ? matchday.getDayNumber() : 0);
			perfDTO.setRating(perf.getRating());
			perfDTO.setGoals(perf.getGoals());
			perfDTO.setAssists(perf.getAssists());
			perfDTO.setYellowCards(perf.getYellowCards());
			perfDTO.setRedCards(perf.getRedCards());
			perfDTO.setMinutesPlayed(perf.getMinutesPlayed());

			// Match-Informationen
			perfDTO.setHomeGoals(match.getHomeGoals());
			perfDTO.setAwayGoals(match.getAwayGoals());

			// Bestimme ob Heimspiel und Gegner
			boolean isHomeMatch = match.getHomeTeamId().equals(player.getTeamId());
			perfDTO.setHomeMatch(isHomeMatch);

			Long opponentId = isHomeMatch ? match.getAwayTeamId() : match.getHomeTeamId();
			Team opponent = teamRepository.findById(opponentId).orElse(null);
			perfDTO.setOpponent(opponent != null ? opponent.getName() : "Unknown");

			// Berechne Ergebnis (W, D, L)
			if (match.getHomeGoals() != null && match.getAwayGoals() != null) {
				int playerGoals = isHomeMatch ? match.getHomeGoals() : match.getAwayGoals();
				int opponentGoals = isHomeMatch ? match.getAwayGoals() : match.getHomeGoals();

				if (playerGoals > opponentGoals) {
					perfDTO.setResult("W");
				} else if (playerGoals < opponentGoals) {
					perfDTO.setResult("L");
				} else {
					perfDTO.setResult("D");
				}
			}

			recentPerformances.add(perfDTO);
		}

		stats.setRecentPerformances(recentPerformances);

		return ResponseEntity.ok(stats);
	}
}