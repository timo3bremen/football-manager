package com.example.manager.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.manager.model.MatchEvent;
import com.example.manager.model.Player;
import com.example.manager.repository.MatchEventRepository;
import com.example.manager.repository.PlayerRepository;

/**
 * Service zur Berechnung von Spielerbewertungen nach Matches
 * und note-basierter Progression
 */
@Service
public class PlayerRatingService {

	@Autowired
	private MatchEventRepository matchEventRepository;
	
	@Autowired
	private PlayerRepository playerRepository;
	
	private final Random random = new Random();
	
	/**
	 * Berechnet die Spielernote (1.0 - 6.0) basierend auf Performance im Match
	 * 
	 * @param playerId Spieler-ID
	 * @param matchId Match-ID
	 * @param teamId Team-ID des Spielers
	 * @param homeGoals Tore des Heimteams
	 * @param awayGoals Tore des Auswärtsteams
	 * @param isHomeTeam Ob der Spieler im Heimteam ist
	 * @return Note von 1.0 (excellent) bis 6.0 (sehr schwach)
	 */
	public double calculatePlayerRating(Long playerId, Long matchId, Long teamId, 
			int homeGoals, int awayGoals, boolean isHomeTeam) {
		
		Player player = playerRepository.findById(playerId).orElse(null);
		if (player == null) {
			return 4.0; // Default durchschnittliche Note
		}
		
		// Lade alle Events für diesen Spieler in diesem Match
		List<MatchEvent> allEvents = matchEventRepository.findByMatchId(matchId);
		
		// Zähle positive und negative Events für diesen Spieler
		int goals = 0;
		int assists = 0;
		int chancesCreated = 0;
		int yellowCards = 0;
		int redCards = 0;
		int errors = 0;
		
		for (MatchEvent event : allEvents) {
			if (event.getPlayerId() != null && event.getPlayerId().equals(playerId)) {
				switch (event.getType()) {
					case "goal":
						goals++;
						break;
					case "assist":
						assists++;
						break;
					case "chance_created":
						chancesCreated++;
						break;
					case "yellow_card":
						yellowCards++;
						break;
					case "red_card":
						redCards++;
						break;
					case "error":
						errors++;
						break;
				}
			}
			// NUR für Tor-Events: Prüfe ob Spieler eine Vorlage gegeben hat
			// Dies verhindert Doppelzählung da assist-Events separat gespeichert werden
			if ("goal".equals(event.getType()) && event.getAssistPlayerId() != null 
					&& event.getAssistPlayerId().equals(playerId)) {
				// Diese Vorlage wird NUR gezählt wenn es kein separates assist-Event gibt
				boolean hasAssistEvent = allEvents.stream()
						.anyMatch(e -> "assist".equals(e.getType()) 
								&& e.getPlayerId() != null 
								&& e.getPlayerId().equals(playerId)
								&& e.getMinute() == event.getMinute());
				if (!hasAssistEvent) {
					assists++;
				}
			}
		}
		
		// Berechne Gegentore (eigene Team kassierte Tore)
		int concededGoals = isHomeTeam ? awayGoals : homeGoals;
		
		// Spielergebnis (win=3, draw=1, loss=0)
		int resultPoints = 0;
		if (isHomeTeam) {
			if (homeGoals > awayGoals) resultPoints = 3;
			else if (homeGoals == awayGoals) resultPoints = 1;
		} else {
			if (awayGoals > homeGoals) resultPoints = 3;
			else if (awayGoals == homeGoals) resultPoints = 1;
		}
		
		// Basis-Note: 3.5 (durchschnittlich) wenn keine Events vorhanden
		// Wenn Spieler Events hat, starte bei 4.0
		double rating = (goals + assists + chancesCreated + yellowCards + redCards + errors) > 0 ? 4.0 : 3.5;
		
		// Positionsspezifische Bewertung (GROSSZÜGIGER)
		String position = player.getPosition();
		
		if ("FWD".equals(position)) {
			// STÜRMER
			// Tore: hoher Einfluss (-0.8 pro Tor)
			rating -= goals * 0.8;
			// Vorlagen: hoher Einfluss (-0.6 pro Vorlage)
			rating -= assists * 0.6;
			// Kreierte Chancen: (-0.3 pro Chance)
			rating -= chancesCreated * 0.3;
			// Ergebnis: (-0.5 bei Sieg, -0.17 bei Unentschieden)
			rating -= resultPoints * 0.17;
			// Gegentore: niedriger Einfluss (+0.05 pro Gegentor)
			rating += concededGoals * 0.05;
			
		} else if ("MID".equals(position)) {
			// MITTELFELD
			// Tore: sehr hoher Einfluss (-1.0 pro Tor)
			rating -= goals * 1.0;
			// Vorlagen: sehr hoher Einfluss (-0.7 pro Vorlage)
			rating -= assists * 0.7;
			// Kreierte Chancen: (-0.4 pro Chance)
			rating -= chancesCreated * 0.4;
			// Ergebnis: (-0.5 bei Sieg, -0.17 bei Unentschieden)
			rating -= resultPoints * 0.17;
			// Gegentore: mittlerer Einfluss (+0.12 pro Gegentor)
			rating += concededGoals * 0.12;
			
		} else if ("DEF".equals(position) || "GK".equals(position)) {
			// ABWEHR & TORWART
			// Tore: außergewöhnlich hoher Einfluss (-1.2 pro Tor für Verteidiger!)
			rating -= goals * 1.2;
			// Vorlagen: sehr hoher Einfluss (-0.8 pro Vorlage)
			rating -= assists * 0.8;
			// Kreierte Chancen: (-0.35 pro Chance)
			rating -= chancesCreated * 0.35;
			// Ergebnis: (-0.6 bei Sieg, -0.2 bei Unentschieden)
			rating -= resultPoints * 0.2;
			// Gegentore: hoher Einfluss (+0.25 pro Gegentor, reduziert)
			rating += concededGoals * 0.25;
			
			// CLEAN SHEET BONUS: Wenn kein Gegentor kassiert wurde
			if (concededGoals == 0) {
				rating -= 0.8; // Starker Bonus für Zu-Null-Spiel
				System.out.println("[Rating] Clean Sheet Bonus für " + player.getName() + ": -0.8");
			}
		}
		
		// Negative Faktoren (alle Positionen) - reduziert für großzügigere Bewertung
		// Gelbe Karten (+0.2)
		rating += yellowCards * 0.2;
		// Rote Karten (+1.0)
		rating += redCards * 1.0;
		// Fehler (+0.25 pro Fehler)
		rating += errors * 0.25;
		
		// Begrenze auf 1.0 - 6.0
		rating = Math.max(1.0, Math.min(6.0, rating));
		
		return Math.round(rating * 10.0) / 10.0; // Runde auf 1 Dezimalstelle
	}
	
	/**
	 * Trainiert einen Spieler basierend auf seiner Note
	 * 
	 * @param player Der zu trainierende Spieler
	 * @param rating Die Note des Spielers (1.0 - 6.0)
	 */
	public void trainPlayerByRating(Player player, double rating) {
		// Berechne Verbesserungs-Chance basierend auf Note
		double improvementChance = getImprovementChanceFromRating(rating);
		
		// Trainiere jede Fähigkeit mit der berechneten Chance
		if (player.getPace() < player.getPacePotential() && random.nextDouble() <= improvementChance) {
			player.setPace(Math.min(100, player.getPace() + 1));
		}
		
		if (player.getDribbling() < player.getDribblingPotential() && random.nextDouble() <= improvementChance) {
			player.setDribbling(Math.min(100, player.getDribbling() + 1));
		}
		
		if (player.getBallControl() < player.getBallControlPotential() && random.nextDouble() <= improvementChance) {
			player.setBallControl(Math.min(100, player.getBallControl() + 1));
		}
		
		if (player.getShooting() < player.getShootingPotential() && random.nextDouble() <= improvementChance) {
			player.setShooting(Math.min(100, player.getShooting() + 1));
		}
		
		if (player.getTackling() < player.getTacklingPotential() && random.nextDouble() <= improvementChance) {
			player.setTackling(Math.min(100, player.getTackling() + 1));
		}
		
		if (player.getSliding() < player.getSlidingPotential() && random.nextDouble() <= improvementChance) {
			player.setSliding(Math.min(100, player.getSliding() + 1));
		}
		
		if (player.getHeading() < player.getHeadingPotential() && random.nextDouble() <= improvementChance) {
			player.setHeading(Math.min(100, player.getHeading() + 1));
		}
		
		if (player.getCrossing() < player.getCrossingPotential() && random.nextDouble() <= improvementChance) {
			player.setCrossing(Math.min(100, player.getCrossing() + 1));
		}
		
		if (player.getPassing() < player.getPassingPotential() && random.nextDouble() <= improvementChance) {
			player.setPassing(Math.min(100, player.getPassing() + 1));
		}
		
		if (player.getAwareness() < player.getAwarenessPotential() && random.nextDouble() <= improvementChance) {
			player.setAwareness(Math.min(100, player.getAwareness() + 1));
		}
		
		if (player.getJumping() < player.getJumpingPotential() && random.nextDouble() <= improvementChance) {
			player.setJumping(Math.min(100, player.getJumping() + 1));
		}
		
		if (player.getStamina() < player.getStaminaPotential() && random.nextDouble() <= improvementChance) {
			player.setStamina(Math.min(100, player.getStamina() + 1));
		}
		
		if (player.getStrength() < player.getStrengthPotential() && random.nextDouble() <= improvementChance) {
			player.setStrength(Math.min(100, player.getStrength() + 1));
		}
		
		// Berechne neues Rating nach Training
		player.calculateRating();
	}
	
	/**
	 * Konvertiert Spielernote in Verbesserungs-Chance
	 * 
	 * @param rating Note von 1.0 - 6.0
	 * @return Verbesserungs-Chance von 0.0 - 0.15 (0% - 15%)
	 */
	private double getImprovementChanceFromRating(double rating) {
		if (rating <= 1.0) return 0.15;  // 15%
		if (rating <= 1.5) return 0.12;  // 12%
		if (rating <= 2.0) return 0.10;  // 10%
		if (rating <= 2.5) return 0.08;  // 8%
		if (rating <= 3.0) return 0.06;  // 6%
		if (rating <= 3.5) return 0.05;  // 5%
		if (rating <= 4.0) return 0.04;  // 4%
		if (rating <= 4.5) return 0.03;  // 3%
		if (rating <= 5.0) return 0.02;  // 2%
		if (rating <= 5.5) return 0.01;  // 1%
		return 0.0;  // 0% für Note 6.0
	}
}
