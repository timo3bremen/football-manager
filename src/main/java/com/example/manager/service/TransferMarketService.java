package com.example.manager.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.manager.model.Player;
import com.example.manager.model.Team;
import com.example.manager.model.TransferHistory;
import com.example.manager.repository.PlayerRepository;
import com.example.manager.repository.TeamRepository;
import com.example.manager.repository.LineupRepository;
import com.example.manager.repository.TransferHistoryRepository;
import com.example.manager.repository.GameStateTrackingRepository;

/**
 * Service für Transfermarkt-Operationen
 */
@Service
public class TransferMarketService {

	@Autowired
	private PlayerRepository playerRepository;

	@Autowired
	private TeamRepository teamRepository;

	@Autowired
	private LineupRepository lineupRepository;

	@Autowired
	private TransferHistoryRepository transferHistoryRepository;

	@Autowired
	private GameStateTrackingRepository gameStateTrackingRepository;

	// Speichert alle Angebote: Liste von Maps mit Angebotsinformationen
	private List<Map<String, Object>> transferOffers = new ArrayList<>();

	/**
	 * Gibt alle Spieler zurück (sortiert nach Marktwert)
	 */
	public List<Player> getAvailablePlayers() {
		return playerRepository.findAll();
	}

	/**
	 * Listet Spieler eines Teams zum Verkauf an Der Spieler bleibt im Team, bekommt
	 * aber den Status "auf Transferliste"
	 */
	public Player listPlayerForSale(Long playerId, Long teamId) {
		Player player = playerRepository.findById(playerId).orElse(null);
		if (player != null && player.getTeamId() != null && player.getTeamId().equals(teamId)) {
			// Spieler bleibt im Team, bekommt aber den Status "auf Transferliste"
			player.setOnTransferList(true);
			return playerRepository.save(player);
		}
		return null;
	}

	/**
	 * Kauft einen Spieler für ein Team
	 */
	public Player buyPlayer(Long playerId, Long teamId) {
		Player player = playerRepository.findById(playerId).orElse(null);
		if (player != null && (player.getTeamId() == null || player.getTeamId() == 0)) {
			player.setTeamId(teamId);
			return playerRepository.save(player);
		}
		return null;
	}

	/**
	 * Sucht nach Spielern basierend auf Kriterien
	 */
	public List<Player> searchPlayers(String position, Integer minRating, Integer maxRating) {
		return playerRepository.findAll().stream()
				.filter(p -> position == null || position.isEmpty() || position.equals(p.getPosition()))
				.filter(p -> minRating == null || p.getRating() >= minRating)
				.filter(p -> maxRating == null || p.getRating() <= maxRating).collect(Collectors.toList());
	}

	/**
	 * Gibt alle abgegebenen Angebote eines Teams zurück
	 */
	public List<Map<String, Object>> getOutgoingOffers(Long buyingTeamId) {
		return transferOffers.stream().filter(offer -> offer.get("buyingTeamId").equals(buyingTeamId))
				.collect(Collectors.toList());
	}

	/**
	 * Gibt alle eingehenden Angebote für Spieler eines Teams zurück (nur Angebote
	 * für Spieler, die aktuell noch in meinem Team sind)
	 */
	public List<Map<String, Object>> getIncomingOffers(Long sellingTeamId) {
		return transferOffers.stream()
				.filter(offer -> offer.get("sellingTeamId") != null && offer.get("sellingTeamId").equals(sellingTeamId))
				.collect(Collectors.toList());
	}

	/**
	 * Macht ein Angebot für einen Spieler
	 */
	public Map<String, Object> makeOffer(Long playerId, Long buyingTeamId, Long offerPrice) {
		Player player = playerRepository.findById(playerId).orElse(null);
		if (player == null) {
			throw new IllegalArgumentException("Spieler nicht gefunden");
		}

		Map<String, Object> offer = new HashMap<>();
		offer.put("id", System.nanoTime()); // Eindeutige ID
		offer.put("playerId", playerId);
		offer.put("buyingTeamId", buyingTeamId);
		offer.put("sellingTeamId", player.getTeamId());
		offer.put("offerPrice", offerPrice);
		offer.put("status", "pending");
		offer.put("createdAt", System.currentTimeMillis());

		// Füge Player-Informationen hinzu
		offer.put("name", player.getName());
		offer.put("position", player.getPosition());
		offer.put("rating", player.getRating());
		offer.put("marketValue", player.getMarketValue());
		offer.put("salary", player.getSalary());
		offer.put("age", player.getAge());
		offer.put("country", player.getCountry());
		offer.put("potential", player.getOverallPotential());
		offer.put("contractLength", player.getContractLength());

		transferOffers.add(offer);

		Map<String, Object> result = new HashMap<>();
		result.put("success", true);
		result.put("message", "Angebot erfolgreich abgegeben");
		result.put("offerId", offer.get("id"));

		return result;
	}

	/**
	 * Entfernt einen Spieler von der Transferliste (gibt ihn zurück an das Team)
	 */
	public Player removePlayerFromSale(Long playerId, Long teamId) {
		Player player = playerRepository.findById(playerId).orElse(null);
		if (player != null && player.getTeamId() != null && player.getTeamId().equals(teamId)
				&& player.isOnTransferList()) {
			// Spieler wird vom Verkauf genommen - Status wird entfernt
			player.setOnTransferList(false);
			Player saved = playerRepository.save(player);

			// Entferne alle Angebote für diesen Spieler
			transferOffers.removeIf(offer -> offer.get("playerId").equals(playerId));

			return saved;
		}
		return null;
	}

	/**
	 * Verkauft einen Spieler von einem Team zu einem anderen Team. Der Spieler
	 * wechselt das Team und der Status "auf Transferliste" wird entfernt. Die
	 * Teams-Budgets sollten vom Aufrufer aktualisiert werden.
	 */
	public Player sellPlayer(Long playerId, Long fromTeamId, Long toTeamId, Long salePrice) {
		Player player = playerRepository.findById(playerId).orElse(null);
		if (player != null && player.getTeamId() != null && player.getTeamId().equals(fromTeamId)
				&& player.isOnTransferList()) {
			// Spieler wechselt Team und verlässt die Transferliste
			player.setTeamId(toTeamId);
			player.setOnTransferList(false);
			Player saved = playerRepository.save(player);

			// Entferne alle Angebote für diesen Spieler
			transferOffers.removeIf(offer -> offer.get("playerId").equals(playerId));

			return saved;
		}
		return null;
	}

	/**
	 * Generiert automatisch Angebote von CPU-Teams für Spieler auf der
	 * Transferliste Diese Methode wird alle 4 Stunden vom Scheduler aufgerufen
	 */
	public void generateCPUOffers() {
		System.out.println("[TransferMarketService] Generating CPU offers for players on transfer list...");

		// Lade alle Spieler auf der Transferliste
		List<Player> transferListPlayers = playerRepository.findAll().stream().filter(Player::isOnTransferList)
				.collect(Collectors.toList());

		if (transferListPlayers.isEmpty()) {
			System.out.println("[TransferMarketService] No players on transfer list");
			return;
		}

		// Lade alle Teams
		List<Team> allTeams = teamRepository.findAll();
		List<Team> cpuTeams = allTeams.stream().filter(Team::isCPU) // Filtere nur CPU-Teams
				.collect(Collectors.toList());

		if (cpuTeams.isEmpty()) {
			System.out.println("[TransferMarketService] No CPU teams available");
			return;
		}

		Random random = new Random();

		// Für jeden Spieler auf der Transferliste: erzeuge ein Angebot von einem
		// zufälligen CPU-Team
		for (Player player : transferListPlayers) {
			// Wähle ein zufälliges CPU-Team (aber nicht das Team des Spielers)
			List<Team> availableCpuTeams = cpuTeams.stream().filter(t -> !t.getId().equals(player.getTeamId()))
					.collect(Collectors.toList());

			if (availableCpuTeams.isEmpty()) {
				continue;
			}

			Team cpuTeam = availableCpuTeams.get(random.nextInt(availableCpuTeams.size()));

			// Generiere einen Angebotspreis basierend auf dem Marktwert (80-120% des
			// Marktwerts)
			long basePrice = player.getMarketValue();
			long offerPrice = (long) (basePrice * (0.8 + random.nextDouble() * 0.4));

			Team sellingTeam = teamRepository.findById(player.getTeamId()).orElse(null);

			// Erstelle das Angebot
			Map<String, Object> offer = new HashMap<>();
			offer.put("id", System.nanoTime());
			offer.put("playerId", player.getId());
			offer.put("buyingTeamId", cpuTeam.getId());
			offer.put("buyingTeamName", cpuTeam.getName());
			offer.put("sellingTeamId", player.getTeamId());
			offer.put("sellingTeamName", sellingTeam != null ? sellingTeam.getName() : "Unknown");
			offer.put("offerPrice", offerPrice);
			offer.put("status", "pending");
			offer.put("createdAt", System.currentTimeMillis());
			offer.put("isCPUOffer", true); // Markiere als CPU-Angebot

			// Füge Player-Informationen hinzu
			offer.put("name", player.getName());
			offer.put("position", player.getPosition());
			offer.put("rating", player.getRating());
			offer.put("marketValue", player.getMarketValue());
			offer.put("salary", player.getSalary());
			offer.put("age", player.getAge());
			offer.put("country", player.getCountry());
			offer.put("potential", player.getOverallPotential());
			offer.put("contractLength", player.getContractLength());
			offer.put("teamId", player.getTeamId());

			transferOffers.add(offer);
			System.out.println("[TransferMarketService] CPU Offer created: " + cpuTeam.getName() + " -> "
					+ player.getName() + " for " + offerPrice);
		}
	}

	/**
	 * Nimmt ein Angebot an und verarbeitet den Spieler-Transfer Aktualisiert
	 * Budgets beider Teams
	 */
	public Map<String, Object> acceptOffer(Long offerId, Long acceptingTeamId) {
		// Finde das Angebot - robust gegen unterschiedliche Number-Typen
		Map<String, Object> offer = null;
		for (Map<String, Object> o : transferOffers) {
			Object id = o.get("id");
			if (id instanceof Number) {
				if (((Number) id).longValue() == offerId) {
					offer = o;
					break;
				}
			}
		}

		if (offer == null) {
			return Map.of("success", false, "message", "Angebot nicht gefunden");
		}

		Long playerId = ((Number) offer.get("playerId")).longValue();
		Long sellingTeamId = ((Number) offer.get("sellingTeamId")).longValue();
		Long buyingTeamId = ((Number) offer.get("buyingTeamId")).longValue();
		Long offerPrice = ((Number) offer.get("offerPrice")).longValue();

		// Prüfe ob das akzeptierende Team das verkaufende Team ist
		if (!sellingTeamId.equals(acceptingTeamId)) {
			return Map.of("success", false, "message", "Nur das verkaufende Team kann das Angebot annehmen");
		}

		// Lade Spieler und Teams
		Player player = playerRepository.findById(playerId).orElse(null);
		Team sellingTeam = teamRepository.findById(sellingTeamId).orElse(null);
		Team buyingTeam = teamRepository.findById(buyingTeamId).orElse(null);

		if (player == null || sellingTeam == null || buyingTeam == null) {
			return Map.of("success", false, "message", "Spieler oder Team nicht gefunden");
		}

		// WICHTIG: Entferne Spieler zuerst aus ALLEN Aufstellungen des verkaufenden Teams
		List<com.example.manager.model.LineupSlot> lineupSlots = lineupRepository.findByTeamId(sellingTeamId);
		for (com.example.manager.model.LineupSlot slot : lineupSlots) {
			if (slot.getPlayerId() != null && slot.getPlayerId().equals(playerId)) {
				slot.setPlayerId(null);
				lineupRepository.save(slot);
				System.out.println("[TransferMarketService] Removed player " + player.getName() + " from lineup slot");
			}
		}

		// Führe den Transfer durch
		player.setTeamId(buyingTeamId);
		player.setOnTransferList(false);
		playerRepository.save(player);

		// Aktualisiere Budgets
		sellingTeam.setBudgetAsLong(sellingTeam.getBudgetAsLong() + offerPrice);
		buyingTeam.setBudgetAsLong(buyingTeam.getBudgetAsLong() - offerPrice);
		teamRepository.save(sellingTeam);
		teamRepository.save(buyingTeam);

		// Entferne ALLE Angebote für diesen Spieler (da er jetzt verkauft ist)
		transferOffers.removeIf(o -> {
			Object id = o.get("playerId");
			if (id instanceof Number) {
				return ((Number) id).longValue() == playerId;
			}
			return false;
		});
		
		// Speichere Transfer-Historie
		try {
			com.example.manager.model.GameStateTracking tracking = gameStateTrackingRepository.findAll().stream().findFirst().orElse(null);
			int currentMatchday = tracking != null ? tracking.getCurrentMatchday() : 0;
			int currentSeason = tracking != null ? tracking.getCurrentSeason() : 1;
			
			TransferHistory history = new TransferHistory(
				player.getId(),
				player.getName(),
				player.getPosition(),
				player.getRating(),
				player.getAge(),
				sellingTeamId,
				sellingTeam.getName(),
				buyingTeamId,
				buyingTeam.getName(),
				offerPrice,
				currentMatchday,
				currentSeason,
				Instant.now()
			);
			transferHistoryRepository.save(history);
			System.out.println("[TransferMarketService] 📜 Transfer-Historie gespeichert: " + player.getName());
		} catch (Exception e) {
			System.err.println("[TransferMarketService] Fehler beim Speichern der Transfer-Historie: " + e.getMessage());
		}

		System.out.println("[TransferMarketService] Transfer completed: " + player.getName() + " from "
				+ sellingTeam.getName() + " to " + buyingTeam.getName() + " for " + offerPrice);

		return Map.of("success", true, "message", "Angebot akzeptiert", "player", player.getName(), "price",
				offerPrice);
	}

	/**
	 * CPU-Teams reagieren automatisch auf Angebote basierend auf Marktwert
	 * Logik:
	 * - Unter Marktwert: 0% akzeptiert (100% ablehnen)
	 * - 0-5% über Marktwert: 10% akzeptiert
	 * - 5-10% über Marktwert: 20% akzeptiert
	 * - 10-20% über Marktwert: 50% akzeptiert
	 * - 20-30% über Marktwert: 75% akzeptiert
	 * - 30%+ über Marktwert: 90% akzeptiert
	 */
	public void processCPUOfferResponses() {
		System.out.println("[TransferMarketService] 🤖 Processing CPU offer responses...");

		// Kopiere die Liste, da wir sie während der Iteration modifizieren werden
		List<Map<String, Object>> offersToProcess = new ArrayList<>(transferOffers);
		Random random = new Random();
		int acceptedCount = 0;
		int rejectedCount = 0;

		for (Map<String, Object> offer : offersToProcess) {
			try {
				// Überspringe wenn Angebot bereits verarbeitet wurde
				if (offer.get("cpuResponse") != null) {
					continue;
				}

				Long sellingTeamId = ((Number) offer.get("sellingTeamId")).longValue();
				Long playerId = ((Number) offer.get("playerId")).longValue();
				Long offerPrice = ((Number) offer.get("offerPrice")).longValue();

				// Prüfe ob das Team ein CPU-Team ist
				Team sellingTeam = teamRepository.findById(sellingTeamId).orElse(null);
				if (sellingTeam == null || !sellingTeam.isCPU()) {
					continue; // Ignoriere User-Teams
				}

				Player player = playerRepository.findById(playerId).orElse(null);
				if (player == null) {
					continue;
				}

				// Berechne den prozentualen Unterschied zum Marktwert
				long marketValue = player.getMarketValue();
				double pricePercentage = ((double) (offerPrice - marketValue) / marketValue) * 100;

				// Bestimme die Akzeptanz-Wahrscheinlichkeit basierend auf dem Prozentsatz
				double acceptanceProbability = 0.0;

				if (pricePercentage < 0) {
					// Unter Marktwert: 0% akzeptiert
					acceptanceProbability = 0.0;
				} else if (pricePercentage <= 5) {
					// 0-5% über Marktwert: 10% akzeptiert
					acceptanceProbability = 0.10;
				} else if (pricePercentage <= 10) {
					// 5-10% über Marktwert: 20% akzeptiert
					acceptanceProbability = 0.20;
				} else if (pricePercentage <= 20) {
					// 10-20% über Marktwert: 50% akzeptiert
					acceptanceProbability = 0.50;
				} else if (pricePercentage <= 30) {
					// 20-30% über Marktwert: 75% akzeptiert
					acceptanceProbability = 0.75;
				} else {
					// 30%+ über Marktwert: 90% akzeptiert
					acceptanceProbability = 0.90;
				}

				// Entscheide basierend auf der Wahrscheinlichkeit
				boolean shouldAccept = random.nextDouble() < acceptanceProbability;

				// Markiere Angebot als verarbeitet und setze CPU-Antwort
				offer.put("cpuResponse", shouldAccept ? "accepted" : "rejected");
				offer.put("cpuResponseTime", System.currentTimeMillis());

				if (shouldAccept) {
					offer.put("status", "accepted");
					acceptedCount++;
					System.out.println("[TransferMarketService] ✅ CPU Team accepted offer for " + player.getName()
							+ " at " + String.format("%.1f", pricePercentage) + "% over market value");
				} else {
					offer.put("status", "rejected");
					rejectedCount++;
					System.out.println("[TransferMarketService] ❌ CPU Team rejected offer for " + player.getName()
							+ " at " + String.format("%.1f", pricePercentage) + "% over market value");
				}

			} catch (Exception e) {
				System.err.println("[TransferMarketService] Error processing CPU offer response: " + e.getMessage());
				e.printStackTrace();
			}
		}

		System.out.println("[TransferMarketService] 🤖 CPU offer responses: " + acceptedCount + " accepted, "
				+ rejectedCount + " rejected");
	}

	/**
	 * Verarbeitet die Gehaltsverhandlung und führt den Transfer durch
	 * Spieler wird transferiert und Budgets werden aktualisiert
	 */
	public Map<String, Object> completeNegotiation(Long offerId, Long acceptingTeamId, Long newSalary, Integer contractLength) {
		System.out.println("[TransferMarketService] CompleteNegotiation called with offerId=" + offerId + 
			", acceptingTeamId=" + acceptingTeamId + ", newSalary=" + newSalary + ", contractLength=" + contractLength);
		
		// Finde das Angebot - robust gegen unterschiedliche Number-Typen
		Map<String, Object> offer = null;
		for (Map<String, Object> o : transferOffers) {
			Object id = o.get("id");
			if (id != null && offerId != null) {
				if (id instanceof Number) {
					if (((Number) id).longValue() == offerId) {
						offer = o;
						System.out.println("[TransferMarketService] Found offer with matching id");
						break;
					}
				}
			}
		}

		if (offer == null) {
			System.err.println("[TransferMarketService] Angebot nicht gefunden mit ID " + offerId);
			System.err.println("[TransferMarketService] Verfügbare Angebote: " + transferOffers.stream()
				.map(o -> o.get("id")).collect(Collectors.toList()));
			return Map.of("success", false, "message", "Angebot nicht gefunden");
		}

		// ...existing code...
		Long playerId = ((Number) offer.get("playerId")).longValue();
		Long sellingTeamId = ((Number) offer.get("sellingTeamId")).longValue();
		Long buyingTeamId = ((Number) offer.get("buyingTeamId")).longValue();
		Long offerPrice = ((Number) offer.get("offerPrice")).longValue();

		// Prüfe ob das akzeptierende Team das KAUFENDE Team ist
		// (Das Team, das das Angebot gemacht hat und die Verhandlung führt)
		if (!buyingTeamId.equals(acceptingTeamId)) {
			System.err.println("[TransferMarketService] Invalid team: acceptingTeamId=" + acceptingTeamId + 
				", sellingTeamId=" + sellingTeamId + ", buyingTeamId=" + buyingTeamId);
			return Map.of("success", false, "message", "Nur das Team, das das Angebot gemacht hat, kann die Gehaltsverhandlung führen");
		}

		// Lade Spieler und Teams
		Player player = playerRepository.findById(playerId).orElse(null);
		Team sellingTeam = teamRepository.findById(sellingTeamId).orElse(null);
		Team buyingTeam = teamRepository.findById(buyingTeamId).orElse(null);

		if (player == null || sellingTeam == null || buyingTeam == null) {
			return Map.of("success", false, "message", "Spieler oder Team nicht gefunden");
		}

		// Prüfe ob Spieler akzeptiert (Gehalt >= aktuelles Gehalt)
		long currentSalary = player.getSalary();
		if (newSalary < currentSalary) {
			return Map.of("success", false, "message", "Spieler lehnt ab - Gehalt zu niedrig");
		}

		// WICHTIG: Entferne Spieler zuerst aus ALLEN Aufstellungen des verkaufenden Teams
		List<com.example.manager.model.LineupSlot> lineupSlots = lineupRepository.findByTeamId(sellingTeamId);
		for (com.example.manager.model.LineupSlot slot : lineupSlots) {
			if (slot.getPlayerId() != null && slot.getPlayerId().equals(playerId)) {
				slot.setPlayerId(null);
				lineupRepository.save(slot);
				System.out.println("[TransferMarketService] Removed player " + player.getName() + " from lineup slot");
			}
		}

		// Aktualisiere Spieler: neues Team, neues Gehalt, neue Vertragslänge
		player.setTeamId(buyingTeamId);
		player.setOnTransferList(false);
		player.setSalary(newSalary);
		player.setContractLength(contractLength);
		player.calculateMarketValue();
		playerRepository.save(player);

		// Aktualisiere Budgets
		sellingTeam.setBudgetAsLong(sellingTeam.getBudgetAsLong() + offerPrice);
		buyingTeam.setBudgetAsLong(buyingTeam.getBudgetAsLong() - offerPrice);
		teamRepository.save(sellingTeam);
		teamRepository.save(buyingTeam);

		// Entferne ALLE Angebote für diesen Spieler (da er jetzt verkauft ist)
		transferOffers.removeIf(o -> {
			Object id = o.get("playerId");
			if (id instanceof Number) {
				return ((Number) id).longValue() == playerId;
			}
			return false;
		});
		
		// Speichere Transfer-Historie
		try {
			com.example.manager.model.GameStateTracking tracking = gameStateTrackingRepository.findAll().stream().findFirst().orElse(null);
			int currentMatchday = tracking != null ? tracking.getCurrentMatchday() : 0;
			int currentSeason = tracking != null ? tracking.getCurrentSeason() : 1;
			
			TransferHistory history = new TransferHistory(
				player.getId(),
				player.getName(),
				player.getPosition(),
				player.getRating(),
				player.getAge(),
				sellingTeamId,
				sellingTeam.getName(),
				buyingTeamId,
				buyingTeam.getName(),
				offerPrice,
				currentMatchday,
				currentSeason,
				Instant.now()
			);
			transferHistoryRepository.save(history);
			System.out.println("[TransferMarketService] 📜 Transfer-Historie gespeichert: " + player.getName());
		} catch (Exception e) {
			System.err.println("[TransferMarketService] Fehler beim Speichern der Transfer-Historie: " + e.getMessage());
		}

		System.out.println("[TransferMarketService] Transfer completed via negotiation: " + player.getName() + " from "
				+ sellingTeam.getName() + " to " + buyingTeam.getName() + " for " + offerPrice);

		return Map.of("success", true, "message", "Gehaltsverhandlung abgeschlossen", "player", player.getName(), "price",
				offerPrice, "salary", newSalary);
	}

	/**
	 * Lehnt ein Angebot ab (entfernt es aus der Liste)
	 */
	public Map<String, Object> rejectOffer(Long offerId) {
		boolean removed = transferOffers.removeIf(o -> o.get("id").equals(offerId));
		
		if (removed) {
			System.out.println("[TransferMarketService] Offer " + offerId + " rejected and removed");
			return Map.of("success", true, "message", "Angebot abgelehnt");
		} else {
			return Map.of("success", false, "message", "Angebot nicht gefunden");
		}
	}
}
