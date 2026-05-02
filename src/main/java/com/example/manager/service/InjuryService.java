package com.example.manager.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.manager.model.LineupSlot;
import com.example.manager.model.Player;
import com.example.manager.repository.LineupRepository;
import com.example.manager.repository.PlayerRepository;

/**
 * Service für Spielerverletzungen - Zufällige Verletzungen während
 * Match-Simulation - Automatisches Entfernen aus Aufstellung - Heilung über
 * Spieltage
 */
@Service
public class InjuryService {

	@Autowired
	private PlayerRepository playerRepository;

	@Autowired
	private LineupRepository lineupRepository;

	@Autowired
	private NotificationService notificationService;

	private final Random random = new Random();

	// Verletzungskatalog: [name, duration_in_matchdays, severity]
	// Severity: LIGHT (1-3 Tage), MODERATE (4-7 Tage), SEVERE (8-15 Tage), CRITICAL
	// (16+ Tage)
	private static final String[][] INJURIES = {
			// === LEICHTE VERLETZUNGEN (1-3 Tage) ===
			{ "Oberschenkelprellung", "2", "LIGHT" }, { "Zerrung in der Wade", "1", "LIGHT" },
			{ "Prellungen", "2", "LIGHT" }, { "Platzwunde", "1", "LIGHT" }, { "Wadenkrampf", "1", "LIGHT" },
			{ "Kopfwunde", "2", "LIGHT" }, { "Verstauchung", "3", "LIGHT" },

			// === MODERATE VERLETZUNGEN (4-7 Tage) ===
			{ "Bänderriss leicht", "4", "MODERATE" }, { "Muskelfaserriss", "5", "MODERATE" },
			{ "Gehirnerschütterung", "4", "MODERATE" }, { "Rückenprellungen", "4", "MODERATE" },
			{ "Sehnenentzündung", "7", "MODERATE" }, { "Zehenbruch", "6", "MODERATE" },

			// === SCHWERE VERLETZUNGEN (8-15 Tage) ===
			{ "Innenbänder gerissen", "8", "SEVERE" }, { "Außenbänder gerissen", "8", "SEVERE" },
			{ "Fraktur (Mittelfuß)", "10", "SEVERE" }, { "Meniskus-Verletzung (leicht)", "10", "SEVERE" },
			{ "Oberschenkelzerrung (schwer)", "8", "SEVERE" }, { "Sprunggelenksfraktur", "12", "SEVERE" },

			// === KRITISCHE VERLETZUNGEN (16+ Tage) ===
			{ "Fraktur (Knöchel)", "14", "SEVERE" }, { "Fraktur (Rippe)", "10", "SEVERE" },
			{ "Meniskus-Verletzung (schwer)", "15", "CRITICAL" }, { "Kreuzbandriss", "20", "CRITICAL" },
			{ "Vorderes Kreuzband & Innenband", "25", "CRITICAL" }, { "Mehrfachband-Verletzung", "22", "CRITICAL" },
			{ "Oberschenkelbruch (Femur)", "28", "CRITICAL" }, { "Beckenbruch", "30", "CRITICAL" } };

	/**
	 * Berechnet Verletzungswahrscheinlichkeit für einen Spieler - Basierend auf
	 * Position (Abwehr höheres Risiko) - Basierend auf Fitness
	 */
	public double getInjuryChance(Player player) {
		double baseChance = 0.02; // 2% Basis-Chance pro Spieler pro Match

		// Defensiv-Spieler haben höheres Risiko
		if ("DEF".equals(player.getPosition())) {
			baseChance = 0.025; // 2.5%
		}

		// Schlechtere Fitness = höheres Risiko
		baseChance *= (100.0 / player.getFitness());

		// Höheres Alter = höheres Risiko
		if (player.getAge() >= 32) {
			baseChance *= 1.5;
		}

		return Math.min(0.15, baseChance); // Max 15%
	}

	/**
	 * Versucht einen Spieler zu verletzen Gibt true zurück wenn Spieler verletzt
	 * wurde KEINE Benachrichtigung - wird später beim Spieltagwechsel versendet
	 */
	@Transactional
	public void tryInjurePlayer(Player player, int currentMatchday, Long teamId) {
		if (player.isInjured()) {
			return;
		}

		// Wähle zufällige Verletzung
		String[] injury = INJURIES[random.nextInt(INJURIES.length)];
		String injuryName = injury[0];
		int matchdaysOut = Integer.parseInt(injury[1]);

		// Verletzung anwenden
		player.injure(injuryName, matchdaysOut, currentMatchday);
		playerRepository.save(player);

		// Entferne aus Aufstellung
//		removePlayerFromLineup(player.getId(), teamId);

		// KEINE Benachrichtigung hier - wird beim Spieltagwechsel versendet!

		System.out.println(
				"[Injury] 🤕 " + player.getName() + " verletzt: " + injuryName + " (" + matchdaysOut + " Spieltage)");
	}

	/**
	 * Entfernt einen Spieler aus der Aufstellung
	 */
	@Transactional
	private void removePlayerFromLineup(Long playerId, Long teamId) {
		List<LineupSlot> lineupSlots = lineupRepository.findByTeamId(teamId);
		for (LineupSlot slot : lineupSlots) {
			if (slot.getPlayerId() != null && slot.getPlayerId().equals(playerId)) {
				slot.setPlayerId(null);
				lineupRepository.save(slot);
				System.out.println("[Injury] Spieler " + playerId + " aus Aufstellung entfernt");
			}
		}
	}

	/**
	 * Reduziert alle Verletzungen aller Spieler um 1 Spieltag Wird bei
	 * Spieltagwechsel aufgerufen
	 */
	@Transactional
	public void healAllInjuries() {
		System.out.println("[Injury] 🏥 Heile alle Verletzungen um 1 Spieltag...");

		List<Player> injuredPlayers = playerRepository.findAll().stream().filter(Player::isInjured).toList();

		int totalHealed = 0;
		for (Player player : injuredPlayers) {
			player.decreaseInjury();
			playerRepository.save(player);

			if (!player.isInjured()) {
				totalHealed++;
				System.out.println("[Injury] ✅ " + player.getName() + " genesen!");
			}
		}

		System.out.println("[Injury] 📊 " + totalHealed + " Spieler sind genesen");
	}

	/**
	 * Gibt alle verletzten Spieler eines Teams zurück
	 */
	public List<Player> getInjuredPlayers(Long teamId) {
		List<Player> teamPlayers = playerRepository.findByTeamId(teamId);
		List<Player> injuredPlayers = new ArrayList<>();

		for (Player player : teamPlayers) {
			if (player.isInjured()) {
				injuredPlayers.add(player);
			}
		}

		return injuredPlayers;
	}

	/**
	 * Gibt Anzahl verletzter Spieler eines Teams zurück
	 */
	public int getInjuredPlayerCount(Long teamId) {
		return (int) playerRepository.findByTeamId(teamId).stream().filter(Player::isInjured).count();
	}

	/**
	 * Verarbeitet alle Verletzungen beim Spieltagwechsel für einen einzelnen
	 * Spieler: 1. Versendet Benachrichtigung mit gespeicherter Diagnose und
	 * Ausfallszeit 2. Reduziert die Ausfallzeit um 1 Tag 3. Markiert Spieler als
	 * rot/nicht verfügbar Wird beim Spieltagwechsel aufgerufen
	 */
	@Transactional
	public void processInjuriesOnMatchdayChange(Player player) {
		if (player.isInjured()) {
			// Die Verletzung wurde bereits bei tryInjurePlayer() ermittelt
			// Hier nur die bereits gespeicherten Daten verarbeiten
			String diagnosis = player.getInjuryName() != null ? player.getInjuryName() : "Unbekannte Verletzung";
			int daysRemaining = player.getInjuryMatchdaysRemaining();

			String title = "🤕 " + player.getName() + " ist verletzt";
			String content = String.format(
					"Spieler: %s\n" + "Diagnose: %s\n" + "Ausfallszeit: %d Spieltag(e)\n\n"
							+ "Der Spieler kann nicht zur Aufstellung hinzugefügt werden, bis er sich geheilt hat.",
					player.getName(), diagnosis, daysRemaining);

			notificationService.notifyInjury(player.getTeamId(), player.getName(), daysRemaining - 1, diagnosis);
			System.out.println("[Injury] 📬 Benachrichtigung versendet: " + player.getName() + " - Diagnose: "
					+ diagnosis + " (" + daysRemaining + " Spieltage Ausfallzeit)");

			// 2. Reduziere Ausfallzeit um 1 Tag
			player.decreaseInjury();
			playerRepository.save(player);

			// 3. Entferne aus Aufstellung (bleibt rot markiert solange verletzt)
			removePlayerFromLineup(player.getId(), player.getTeamId());

			// Status-Log
			if (!player.isInjured()) {
				System.out.println("[Injury] ✅ " + player.getName() + " ist genesen!");
			} else {
				System.out.println("[Injury] 🔴 " + player.getName() + " ist noch "
						+ player.getInjuryMatchdaysRemaining() + " Spieltag(e) verletzt");
			}
		}

		System.out.println("[Injury] 🏥 Verletzungsverarbeitung für Spieler " + player.getName() + " abgeschlossen");
	}

	@Transactional
	public void processSuspensionsOnMatchdayChange(Player player) {
		if (player.isSuspended()) {

			notificationService.notifySuspension(player.getTeamId(), player.getName(),
					player.getSuspensionMatchesRemaining() - 1);
			// 2. Reduziere Ausfallzeit um 1 Tag
			player.decreaseSuspension();
			playerRepository.save(player);

			// 3. Entferne aus Aufstellung (bleibt rot markiert solange verletzt)
			removePlayerFromLineup(player.getId(), player.getTeamId());

			// Status-Log
			if (!player.isSuspended()) {
				System.out.println("[Injury] ✅ " + player.getName() + " ist genesen!");
			} else {
				System.out.println("[Injury] 🔴 " + player.getName() + " ist noch "
						+ player.getInjuryMatchdaysRemaining() + " Spieltag(e) verletzt");
			}
		}

		System.out.println("[Injury] 🏥 Verletzungsverarbeitung für Spieler " + player.getName() + " abgeschlossen");
	}

	/**
	 * Verarbeitet alle Verletzungen beim Spieltagwechsel für ALLE Teams Wird beim
	 * globalen Spieltagwechsel aufgerufen
	 */
	@Transactional
	public void processAllTeamInjuriesOnMatchdayChange() {
		System.out.println("[Injury] 🏥 Verarbeite Verletzungen beim Spieltagswechsel für ALLE Teams...");

		// Hole tatsächlich alle Teams über einen anderen Weg
		for (Player player : playerRepository.findByIsInjured(true)) {
			processInjuriesOnMatchdayChange(player);
		}

		System.out.println("[Injury] 🏥 Verletzungsverarbeitung abgeschlossen");
	}

	@Transactional
	public void processAllTeamSuspensionsOnMatchdayChange() {
		System.out.println("[Suspension] 🏥 Verarbeite Suspension beim Spieltagswechsel für ALLE Teams...");

		// Hole tatsächlich alle Teams über einen anderen Weg
		for (Player player : playerRepository.findByIsSuspended(true)) {
			processSuspensionsOnMatchdayChange(player);
		}

		System.out.println("[Suspension] 🏥 Suspensionsverarbeitung abgeschlossen");
	}

	/**
	 * Gibt alle gesperrten Spieler eines Teams zurück
	 */
	public List<Player> getSuspendedPlayers(Long teamId) {
		List<Player> teamPlayers = playerRepository.findByTeamId(teamId);
		List<Player> suspendedPlayers = new ArrayList<>();

		for (Player player : teamPlayers) {
			if (player.isSuspended()) {
				suspendedPlayers.add(player);
			}
		}

		return suspendedPlayers;
	}

	/**
	 * Gibt Anzahl gesperrter Spieler eines Teams zurück
	 */
	public int getSuspendedPlayerCount(Long teamId) {
		return (int) playerRepository.findByTeamId(teamId).stream().filter(Player::isSuspended).count();
	}

	/**
	 * Gibt eine Vorschau auf zukünftige verfügbare Spieler
	 */
	public String getInjuryReport(Long teamId) {
		List<Player> injuredPlayers = getInjuredPlayers(teamId);

		if (injuredPlayers.isEmpty()) {
			return "Keine verletzten Spieler! ✅";
		}

		StringBuilder report = new StringBuilder();
		report.append("Verletzte Spieler (").append(injuredPlayers.size()).append("):\n\n");

		for (Player player : injuredPlayers) {
			report.append("🤕 ").append(player.getName()).append("\n");
			report.append("   Diagnose: ").append(player.getInjuryName()).append("\n");
			report.append("   Verfügbar in: ").append(player.getInjuryMatchdaysRemaining()).append(" Spieltag(en)\n\n");
		}

		return report.toString();
	}

	@Transactional
	public void trySuspendPlayer(Player player, int currentMatchday, Long teamId) {
		if (player.isSuspended()) {
			return;
		}

		// Verletzung anwenden
		player.suspend(random.nextInt(3) + 1, "Rote Karte");
		playerRepository.save(player);

		// Entferne aus Aufstellung
//		removePlayerFromLineup(player.getId(), teamId);

		// KEINE Benachrichtigung hier - wird beim Spieltagwechsel versendet!

		System.out.println("[Suspended] 🤕 " + player.getName() + " suspendiert.");
	}
}
