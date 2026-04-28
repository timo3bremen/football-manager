package com.example.manager.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.manager.dto.LiveMatchEventDTO;
import com.example.manager.dto.LiveSimulationStatusDTO;
import com.example.manager.dto.SimulationMessageDTO;
import com.example.manager.model.LineupSlot;
import com.example.manager.model.Match;
import com.example.manager.model.MatchEvent;
import com.example.manager.model.Matchday;
import com.example.manager.model.Player;
import com.example.manager.model.SimulationMessage;
import com.example.manager.model.Team;
import com.example.manager.repository.LineupRepository;
import com.example.manager.repository.MatchEventRepository;
import com.example.manager.repository.MatchRepository;
import com.example.manager.repository.MatchdayRepository;
import com.example.manager.repository.PlayerRepository;
import com.example.manager.repository.SimulationMessageRepository;
import com.example.manager.repository.TeamRepository;
import com.example.manager.repository.TransactionRepository;

/**
 * Service für Live-Spielsimulationen mit textueller Ausgabe in Echtzeit 270
 * Sekunden = 90 Minuten Spielzeit (20 Minuten Ingame = 1 Minute Real)
 */
@Service
public class LiveMatchSimulationService {

	@Autowired
	private SimpMessagingTemplate messagingTemplate;

	@Autowired
	private MatchRepository matchRepository;

	@Autowired
	private TeamRepository teamRepository;

	@Autowired
	private PlayerRepository playerRepository;

	@Autowired
	private LineupRepository lineupRepository;

	@Autowired
	private MatchEventRepository matchEventRepository;

	@Autowired
	private RepositoryService repositoryService;

	@Autowired
	private SimulationMessageRepository simulationMessageRepository;

	@Autowired
	private TransactionRepository transactionRepository;

	private final Random random = new Random();
	private boolean simulationRunning = false;
	private LocalDateTime simulationStartTime;
	private ScheduledExecutorService executor;

	private static final int SIMULATION_DURATION_SECONDS = 270; // 4.5 Minuten
	private static final int GAME_MINUTES = 90;

	/**
	 * Startet die Live-Simulation nur für das Spiel des eingeloggten Teams
	 */
	@Transactional
	public synchronized void startLiveSimulationForTeam(Long teamId) {
		if (simulationRunning) {
			throw new IllegalStateException("Simulation läuft bereits!");
		}

		if (teamId == null) {
			throw new IllegalArgumentException("Team ID erforderlich!");
		}

		int currentMatchday = repositoryService.getCurrentMatchday();

		// Finde das Spiel des Teams für den aktuellen Spieltag
		Match userMatch = null;
		List<Matchday> allMatchdays = matchdayRepository.findAll();

		for (Matchday md : allMatchdays) {
			if (md.getDayNumber() == currentMatchday && !md.isOffSeason()) {
				for (Match match : md.getMatches()) {
					if (!"played".equals(match.getStatus())) {
						// Prüfe ob dieses Match das Team enthält
						if ((match.getHomeTeamId() != null && match.getHomeTeamId().equals(teamId))
								|| (match.getAwayTeamId() != null && match.getAwayTeamId().equals(teamId))) {
							userMatch = match;
							break;
						}
					}
				}
				if (userMatch != null)
					break;
			}
		}

		if (userMatch == null) {
			throw new IllegalStateException("Keine ausstehenden Spiele für dein Team heute!");
		}

		simulationRunning = true;
		simulationStartTime = LocalDateTime.now();

		System.out.println("[LiveSimulation] ⚽ Starte Live-Simulation für Team " + teamId);

		// Starte Simulation in separatem Thread
		executor = Executors.newSingleThreadScheduledExecutor();

		// Sende Start-Event für das Match
		sendMatchStartEvent(userMatch);

		// Simuliere Spiel-Events über 270 Sekunden (nur für dieses Match)
		List<Match> singleMatch = new ArrayList<>();
		singleMatch.add(userMatch);
		simulateMatchEvents(singleMatch);
	}

	/**
	 * Startet die Live-Simulation für alle Matches des aktuellen Spieltags (legacy)
	 */
	@Transactional
	public synchronized void startLiveSimulation() {
		if (simulationRunning) {
			throw new IllegalStateException("Simulation läuft bereits!");
		}

		int currentMatchday = repositoryService.getCurrentMatchday();

		// Finde alle Matchdays für den aktuellen Spieltag
		List<Matchday> matchdaysForCurrentDay = new ArrayList<>();
		List<Matchday> allMatchdays = matchdayRepository.findAll();
		for (Matchday md : allMatchdays) {
			if (md.getDayNumber() == currentMatchday && !md.isOffSeason()) {
				matchdaysForCurrentDay.add(md);
			}
		}

		if (matchdaysForCurrentDay.isEmpty()) {
			throw new IllegalStateException("Keine Spieltage für aktuelle Runde gefunden!");
		}

		// Sammle alle Matches
		List<Match> allMatches = new ArrayList<>();
		for (Matchday md : matchdaysForCurrentDay) {
			for (Match match : md.getMatches()) {
				if (!"played".equals(match.getStatus())) {
					allMatches.add(match);
				}
			}
		}

		if (allMatches.isEmpty()) {
			throw new IllegalStateException("Keine ausstehenden Spiele für diesen Spieltag!");
		}

		simulationRunning = true;
		simulationStartTime = LocalDateTime.now();

		System.out.println("[LiveSimulation] ⚽ Starte Live-Simulation für " + allMatches.size() + " Spiele");

		// Starte Simulation in separatem Thread
		executor = Executors.newSingleThreadScheduledExecutor();

		// Sende Start-Events für alle Matches
		for (Match match : allMatches) {
			sendMatchStartEvent(match);
		}

		// Simuliere Spiel-Events über 270 Sekunden
		simulateMatchEvents(allMatches);
	}

	/**
	 * Simuliert Events für alle Matches über 270 Sekunden
	 */
	private void simulateMatchEvents(List<Match> matches) {
		// Erstelle Event-Timeline für jedes Match
		Map<Match, List<SimulationEvent>> eventTimelines = new HashMap<>();

		for (Match match : matches) {
			List<SimulationEvent> events = generateEventTimeline(match);
			eventTimelines.put(match, events);
		}

		// Sende Events basierend auf Echtzeit (3 Sekunden = 1 Spielminute)
		for (int minute = 1; minute <= GAME_MINUTES; minute++) {
			final int currentMinute = minute;
			long delayMillis = (minute - 1) * 3000L; // 3 Sekunden pro Spielminute

			executor.schedule(() -> {
				for (Map.Entry<Match, List<SimulationEvent>> entry : eventTimelines.entrySet()) {
					Match match = entry.getKey();
					List<SimulationEvent> events = entry.getValue();

					// Sende alle Events für diese Minute
					for (SimulationEvent event : events) {
						if (event.minute == currentMinute) {
							sendLiveEvent(match, event);
						}
					}
				}

				// Reduziere Stamina JEDE Minute um 1 (nicht alle 2 Minuten)
				// Nach 90 Minuten = 90 Stamina verloren, aber max 45 da Start bei 100
				if (currentMinute % 1 == 0) { // Jede Minute
					for (Match match : matches) {
						reducePlayerStamina(match);
					}
				}

				// Halbzeit-Nachricht
				if (currentMinute == 45) {
					for (Match match : matches) {
						sendHalftimeEvent(match);
					}
				}
			}, delayMillis, TimeUnit.MILLISECONDS);
		}

		// Nach 270 Sekunden: Simulation beenden und Ergebnisse speichern
		executor.schedule(() -> {
			finishSimulation(matches, eventTimelines);
		}, SIMULATION_DURATION_SECONDS * 1000L, TimeUnit.MILLISECONDS);
	}

	/**
	 * Generiert Event-Timeline für ein Match
	 */
	private List<SimulationEvent> generateEventTimeline(Match match) {
		List<SimulationEvent> events = new ArrayList<>();

		Long homeTeamId = match.getHomeTeamId();
		Long awayTeamId = match.getAwayTeamId();

		Team homeTeam = teamRepository.findById(homeTeamId).orElse(null);
		Team awayTeam = teamRepository.findById(awayTeamId).orElse(null);

		if (homeTeam == null || awayTeam == null) {
			return events;
		}

		int homeStrength = calculateTeamStrength(homeTeamId);
		int awayStrength = calculateTeamStrength(awayTeamId);

		// Generiere Tor-Events
		int homeGoals = generateGoals(homeStrength, awayStrength);
		int awayGoals = generateGoals(awayStrength, homeStrength);

		List<Player> homePlayers = getLineupPlayers(homeTeamId,
				homeTeam.getActiveFormation() != null ? homeTeam.getActiveFormation() : "4-4-2");
		List<Player> awayPlayers = getLineupPlayers(awayTeamId,
				awayTeam.getActiveFormation() != null ? awayTeam.getActiveFormation() : "4-4-2");

		// Generiere Home-Team Tore
		for (int i = 0; i < homeGoals; i++) {
			int minute = 1 + random.nextInt(89);
			Player scorer = selectGoalScorer(homePlayers);
			if (scorer != null) {
				events.add(new SimulationEvent("goal", minute, homeTeam.getName(), scorer.getName(),
						scorer.getName() + " erzielt ein Tor für " + homeTeam.getName() + "! ⚽", true));
			}
		}

		// Generiere Away-Team Tore
		for (int i = 0; i < awayGoals; i++) {
			int minute = 1 + random.nextInt(89);
			Player scorer = selectGoalScorer(awayPlayers);
			if (scorer != null) {
				events.add(new SimulationEvent("goal", minute, awayTeam.getName(), scorer.getName(),
						scorer.getName() + " erzielt ein Tor für " + awayTeam.getName() + "! ⚽", false));
			}
		}

		// Generiere Chancen (5-15 pro Team)
		int homeChances = 5 + random.nextInt(11);
		int awayChances = 5 + random.nextInt(11);

		for (int i = 0; i < homeChances; i++) {
			int minute = 1 + random.nextInt(89);
			Player player = homePlayers.isEmpty() ? null : homePlayers.get(random.nextInt(homePlayers.size()));
			if (player != null) {
				String[] chanceTexts = { player.getName() + " hat eine gute Chance, aber der Ball geht knapp vorbei!",
						player.getName() + " zieht ab - der Torwart hält brillant!",
						"Tolle Kombination von " + homeTeam.getName() + ", aber " + player.getName() + " vergibt!",
						player.getName() + " mit einem Schuss aus der Distanz - weit daneben!" };
				events.add(new SimulationEvent("chance", minute, homeTeam.getName(), player.getName(),
						chanceTexts[random.nextInt(chanceTexts.length)], true));
			}
		}

		for (int i = 0; i < awayChances; i++) {
			int minute = 1 + random.nextInt(89);
			Player player = awayPlayers.isEmpty() ? null : awayPlayers.get(random.nextInt(awayPlayers.size()));
			if (player != null) {
				String[] chanceTexts = { player.getName() + " hat eine gute Chance, aber der Ball geht knapp vorbei!",
						player.getName() + " zieht ab - der Torwart hält brillant!",
						"Tolle Kombination von " + awayTeam.getName() + ", aber " + player.getName() + " vergibt!",
						player.getName() + " mit einem Schuss aus der Distanz - weit daneben!" };
				events.add(new SimulationEvent("chance", minute, awayTeam.getName(), player.getName(),
						chanceTexts[random.nextInt(chanceTexts.length)], false));
			}
		}

		// Gelbe Karten (0-3 pro Match)
		int yellowCards = random.nextInt(4);
		for (int i = 0; i < yellowCards; i++) {
			int minute = 1 + random.nextInt(89);
			boolean isHome = random.nextBoolean();
			List<Player> players = isHome ? homePlayers : awayPlayers;
			String teamName = isHome ? homeTeam.getName() : awayTeam.getName();

			if (!players.isEmpty()) {
				Player player = players.get(random.nextInt(players.size()));
				events.add(new SimulationEvent("yellow_card", minute, teamName, player.getName(),
						"Gelbe Karte für " + player.getName() + " (" + teamName + ")! 🟨", isHome));
			}
		}

		// Rote Karten (0-1 pro Match, selten)
		if (random.nextDouble() < 0.1) {
			int minute = 30 + random.nextInt(60);
			boolean isHome = random.nextBoolean();
			List<Player> players = isHome ? homePlayers : awayPlayers;
			String teamName = isHome ? homeTeam.getName() : awayTeam.getName();

			if (!players.isEmpty()) {
				Player player = players.get(random.nextInt(players.size()));
				events.add(new SimulationEvent("red_card", minute, teamName, player.getName(),
						"Rote Karte für " + player.getName() + " (" + teamName + ")! 🟥", isHome));
			}
		}

		// Verletzungen (0-2 pro Match, selten)
		int injuries = random.nextDouble() < 0.3 ? (random.nextBoolean() ? 1 : 2) : 0;
		for (int i = 0; i < injuries; i++) {
			int minute = 10 + random.nextInt(80);
			boolean isHome = random.nextBoolean();
			List<Player> players = isHome ? homePlayers : awayPlayers;
			String teamName = isHome ? homeTeam.getName() : awayTeam.getName();

			if (!players.isEmpty()) {
				Player player = players.get(random.nextInt(players.size()));
				events.add(new SimulationEvent("injury", minute, teamName, player.getName(),
						player.getName() + " (" + teamName + ") verletzt sich und muss ausgewechselt werden! 🚑",
						isHome));
			}
		}

		// Sortiere Events nach Minute
		events.sort(Comparator.comparingInt(e -> e.minute));

		return events;
	}

	/**
	 * Sendet Match-Start Event
	 */
	private void sendMatchStartEvent(Match match) {
		Team homeTeam = teamRepository.findById(match.getHomeTeamId()).orElse(null);
		Team awayTeam = teamRepository.findById(match.getAwayTeamId()).orElse(null);

		if (homeTeam != null && awayTeam != null) {
			// Berechne erwartete Zuschauerzahl (gleiche Logik wie in RepositoryService)
			int fanSatisfaction = homeTeam.getFanSatisfaction();
			double occupancyMin = 0.0;
			double occupancyMax = 0.0;

			if (fanSatisfaction >= 95) {
				occupancyMin = 1.0;
				occupancyMax = 1.0;
			} else if (fanSatisfaction >= 85) {
				occupancyMin = 0.90;
				occupancyMax = 1.0;
			} else if (fanSatisfaction >= 70) {
				occupancyMin = 0.75;
				occupancyMax = 0.90;
			} else if (fanSatisfaction >= 50) {
				occupancyMin = 0.50;
				occupancyMax = 0.75;
			} else if (fanSatisfaction >= 30) {
				occupancyMin = 0.35;
				occupancyMax = 0.50;
			} else if (fanSatisfaction >= 10) {
				occupancyMin = 0.10;
				occupancyMax = 0.35;
			} else {
				occupancyMin = 0.0;
				occupancyMax = 0.10;
			}

			double occupancy = occupancyMin + (occupancyMax - occupancyMin) * random.nextDouble();

			long standingSeats = homeTeam.getStadiumCapacityStanding() != null ? homeTeam.getStadiumCapacityStanding()
					: 1000L;
			long seatedSeats = homeTeam.getStadiumCapacitySeated() != null ? homeTeam.getStadiumCapacitySeated() : 0L;
			long vipSeats = homeTeam.getStadiumCapacityVip() != null ? homeTeam.getStadiumCapacityVip() : 0L;

			int ticketPriceStanding = homeTeam.getTicketPriceStanding();
			int ticketPriceSeated = homeTeam.getTicketPriceSeated();
			int ticketPriceVip = homeTeam.getTicketPriceVip();

			double standingOccupancyModifier = repositoryService.calculatePriceOccupancyModifier(ticketPriceStanding,
					20, 40, 60, 100);
			double seatedOccupancyModifier = repositoryService.calculatePriceOccupancyModifier(ticketPriceSeated, 40,
					80, 120, 200);
			double vipOccupancyModifier = repositoryService.calculatePriceOccupancyModifier(ticketPriceVip, 80, 200,
					300, 500);

			long standingAttendance = (long) (standingSeats * occupancy * standingOccupancyModifier);
			long seatedAttendance = (long) (seatedSeats * occupancy * seatedOccupancyModifier);
			long vipAttendance = (long) (vipSeats * occupancy * vipOccupancyModifier);
			long totalSpectators = standingAttendance + seatedAttendance + vipAttendance;

			String startMessage = "⚽ Anpfiff: " + homeTeam.getName() + " vs " + awayTeam.getName() + " ("
					+ totalSpectators + " Zuschauer im Stadion)";

			LiveMatchEventDTO event = new LiveMatchEventDTO(match.getId(), "match_start", 0, "", "", startMessage, 0,
					0);

			// Speichere auch in DB
			SimulationMessage message = new SimulationMessage(match.getId(), null, "match_start", 0, "", "",
					startMessage, 0, 0, true);
			simulationMessageRepository.save(message);

			messagingTemplate.convertAndSend("/topic/live-match/" + match.getId(), event);
			messagingTemplate.convertAndSend("/topic/live-match/all", event);
		}
	}

	/**
	 * Sendet Halbzeit Event
	 */
	private void sendHalftimeEvent(Match match) {
		LiveMatchEventDTO event = new LiveMatchEventDTO(match.getId(), "halftime", 45, "", "", "⏸ Halbzeit", null,
				null);

		// Speichere auch in DB
		SimulationMessage message = new SimulationMessage(match.getId(), null, "halftime", 45, "", "", "⏸ Halbzeit",
				null, null, true);
		simulationMessageRepository.save(message);

		messagingTemplate.convertAndSend("/topic/live-match/" + match.getId(), event);
		messagingTemplate.convertAndSend("/topic/live-match/all", event);
	}

	/**
	 * Sendet Live-Event per WebSocket
	 */
	private void sendLiveEvent(Match match, SimulationEvent event) {
		// Zähle ALLE aktuellen Tore im Match (für alle Events)
		List<MatchEvent> savedEvents = matchEventRepository.findByMatchId(match.getId());
		Integer currentHomeGoals = 0;
		Integer currentAwayGoals = 0;

		// Zähle bereits gespeicherte Tore
		for (MatchEvent me : savedEvents) {
			if ("goal".equals(me.getType())) {
				if (me.getTeamId().equals(match.getHomeTeamId())) {
					currentHomeGoals++;
				} else {
					currentAwayGoals++;
				}
			}
		}

		// Wenn aktuelles Event ein Tor ist, erhöhe den Zähler und speichere es
		if ("goal".equals(event.type)) {
			if (event.isHomeTeam) {
				currentHomeGoals++;
			} else {
				currentAwayGoals++;
			}

			// Speichere Tor-Event in Datenbank
			Player player = findPlayerByName(event.playerName,
					event.isHomeTeam ? match.getHomeTeamId() : match.getAwayTeamId());
			if (player != null) {
				MatchEvent matchEvent = new MatchEvent(match.getId(),
						event.isHomeTeam ? match.getHomeTeamId() : match.getAwayTeamId(), player.getId(),
						player.getName(), "goal", event.minute);
				matchEventRepository.save(matchEvent);
			}
		}

		// Sende Event mit aktuellem Spielstand
		LiveMatchEventDTO liveEvent = new LiveMatchEventDTO(match.getId(), event.type, event.minute, event.teamName,
				event.playerName, event.description, currentHomeGoals, currentAwayGoals);

		// Speichere Event in Datenbank für später Abfrage
		SimulationMessage message = new SimulationMessage(match.getId(), null, // teamId wird nicht gespeichert, da es
																				// ein globales Event ist
				event.type, event.minute, event.teamName, event.playerName, event.description, currentHomeGoals,
				currentAwayGoals, event.isHomeTeam);
		simulationMessageRepository.save(message);

		messagingTemplate.convertAndSend("/topic/live-match/" + match.getId(), liveEvent);
		messagingTemplate.convertAndSend("/topic/live-match/all", liveEvent);
	}

	/**
	 * Beendet die Simulation und speichert Ergebnisse
	 */
	@Transactional
	private void finishSimulation(List<Match> matches, Map<Match, List<SimulationEvent>> eventTimelines) {
		System.out.println("[LiveSimulation] ✅ Simulation beendet, speichere Ergebnisse...");

		for (Match match : matches) {
			// Zähle Tore aus gespeicherten Events
			List<MatchEvent> events = matchEventRepository.findByMatchId(match.getId());
			int homeGoals = 0;
			int awayGoals = 0;

			for (MatchEvent event : events) {
				if ("goal".equals(event.getType())) {
					if (event.getTeamId().equals(match.getHomeTeamId())) {
						homeGoals++;
					} else {
						awayGoals++;
					}
				}
			}

			// Speichere Endergebnis
			match.setHomeGoals(homeGoals);
			match.setAwayGoals(awayGoals);
			match.setStatus("played");
			matchRepository.save(match);

			// Sende Match-End Event
			Team homeTeam = teamRepository.findById(match.getHomeTeamId()).orElse(null);
			Team awayTeam = teamRepository.findById(match.getAwayTeamId()).orElse(null);

			if (homeTeam != null && awayTeam != null) {
				String endMessage = "🏁 Abpfiff: " + homeTeam.getName() + " " + homeGoals + " : " + awayGoals + " "
						+ awayTeam.getName();
				LiveMatchEventDTO endEvent = new LiveMatchEventDTO(match.getId(), "match_end", 90, "", "", endMessage,
						homeGoals, awayGoals);

				// Speichere Abpfiff-Event in DB
				SimulationMessage endSimMessage = new SimulationMessage(match.getId(), null, "match_end", 90, "", "",
						endMessage, homeGoals, awayGoals, true);
				simulationMessageRepository.save(endSimMessage);

				messagingTemplate.convertAndSend("/topic/live-match/" + match.getId(), endEvent);
				messagingTemplate.convertAndSend("/topic/live-match/all", endEvent);
			}

			// Determine result
			String result;
			if (homeGoals > awayGoals) {
				result = "home";
			} else if (awayGoals > homeGoals) {
				result = "away";
			} else {
				result = "draw";
			}

			// Berechne Team-Stärken für Zuschauereinnahmen
			int homeStrength = calculateTeamStrength(match.getHomeTeamId());
			int awayStrength = calculateTeamStrength(match.getAwayTeamId());

			// Nutze RepositoryService-Methoden für alle finanziellen Transaktionen
			// Process sponsor payouts for both teams
			repositoryService.processSponsorPayouts(match.getHomeTeamId(), result.equals("home"));
			repositoryService.processSponsorPayouts(match.getAwayTeamId(), result.equals("away"));

			// Deduct player salaries for both teams
			repositoryService.deductPlayerSalaries(match.getHomeTeamId());
			repositoryService.deductPlayerSalaries(match.getAwayTeamId());

			// Process attendance revenue (only for home team)
			repositoryService.processAttendanceRevenue(match, match.getHomeTeamId(), match.getAwayTeamId(), homeGoals,
					awayGoals, homeStrength, awayStrength);

			// Trainiere Spieler
			trainPlayersAfterMatch(match.getHomeTeamId(),
					homeTeam.getActiveFormation() != null ? homeTeam.getActiveFormation() : "4-4-2");
			trainPlayersAfterMatch(match.getAwayTeamId(),
					awayTeam.getActiveFormation() != null ? awayTeam.getActiveFormation() : "4-4-2");

			// Trainiere Akademie-Spieler beider Teams
			repositoryService.trainAcademyPlayers(match.getHomeTeamId());
			repositoryService.trainAcademyPlayers(match.getAwayTeamId());

			// Sende "simulation-complete" Event für dieses Match
			// Dies signalisiert dem Frontend, dass es die Daten neu laden soll
			LiveMatchEventDTO completeEvent = new LiveMatchEventDTO(match.getId(), "simulation_complete", 90, "", "",
					"Simulation abgeschlossen. Daten werden aktualisiert...", homeGoals, awayGoals);

			messagingTemplate.convertAndSend("/topic/live-match/" + match.getId(), completeEvent);
			messagingTemplate.convertAndSend("/topic/live-match/all", completeEvent);
		}

		simulationRunning = false;

		if (executor != null) {
			executor.shutdown();
		}

		System.out.println("[LiveSimulation] 🎉 Alle Spiele abgeschlossen!");
	}

	// Helper Methods (aus RepositoryService übernommen)

	private int calculateTeamStrength(Long teamId) {
		List<LineupSlot> lineup = lineupRepository.findByTeamIdAndFormationId(teamId, "4-4-2");
		int strength = 0;
		for (LineupSlot slot : lineup) {
			if (slot.getPlayerId() != null) {
				Player p = playerRepository.findById(slot.getPlayerId()).orElse(null);
				if (p != null) {
					// Berechne Rating mit Fitness-Penalty
					int adjustedRating = applyFitnessPenalty(p.getRating(), p.getFitness());
					strength += adjustedRating;
				}
			}
		}
		return strength;
	}

	/**
	 * Berechnet Penalty basierend auf Fitness-Level Unter 80: -10%, Unter 70: -15%,
	 * Unter 60: -20%, Unter 50: -25% Unter 40: -30%, Unter 30: -35%, Unter 20:
	 * -40%, Unter 10: -50%
	 */
	private int applyFitnessPenalty(int rating, int fitness) {
		if (fitness < 10)
			return (int) (rating * 0.5); // -50%
		if (fitness < 20)
			return (int) (rating * 0.6); // -40%
		if (fitness < 30)
			return (int) (rating * 0.65); // -35%
		if (fitness < 40)
			return (int) (rating * 0.7); // -30%
		if (fitness < 50)
			return (int) (rating * 0.75); // -25%
		if (fitness < 60)
			return (int) (rating * 0.8); // -20%
		if (fitness < 70)
			return (int) (rating * 0.85); // -15%
		if (fitness < 80)
			return (int) (rating * 0.9); // -10%
		return rating; // Voll Rating wenn Fitness >= 80
	}

	private int generateGoals(int teamStrength, int opponentStrength) {
		int strengthDiff = teamStrength - opponentStrength;
		double rand = random.nextDouble();

		if (strengthDiff < 0) {
			return generateGoalsForWeakerTeam(teamStrength, opponentStrength);
		}

		// Vereinfachte Logik
		if (strengthDiff > 100) {
			if (rand < 0.1)
				return 0;
			if (rand < 0.3)
				return 1;
			if (rand < 0.5)
				return 2;
			if (rand < 0.7)
				return 3;
			if (rand < 0.85)
				return 4;
			return 5;
		} else if (strengthDiff > 50) {
			if (rand < 0.2)
				return 0;
			if (rand < 0.4)
				return 1;
			if (rand < 0.6)
				return 2;
			if (rand < 0.8)
				return 3;
			return 4;
		} else {
			if (rand < 0.3)
				return 0;
			if (rand < 0.5)
				return 1;
			if (rand < 0.7)
				return 2;
			if (rand < 0.9)
				return 3;
			return 4;
		}
	}

	private int generateGoalsForWeakerTeam(int teamStrength, int opponentStrength) {
		int strengthDiff = opponentStrength - teamStrength;
		double rand = random.nextDouble();

		if (strengthDiff > 100) {
			if (rand < 0.6)
				return 0;
			if (rand < 0.85)
				return 1;
			return 2;
		} else if (strengthDiff > 50) {
			if (rand < 0.4)
				return 0;
			if (rand < 0.7)
				return 1;
			if (rand < 0.9)
				return 2;
			return 3;
		} else {
			if (rand < 0.3)
				return 0;
			if (rand < 0.5)
				return 1;
			if (rand < 0.7)
				return 2;
			if (rand < 0.9)
				return 3;
			return 4;
		}
	}

	private List<Player> getLineupPlayers(Long teamId, String formation) {
		List<LineupSlot> lineup = lineupRepository.findByTeamIdAndFormationId(teamId, formation);
		List<Player> players = new ArrayList<>();

		for (LineupSlot slot : lineup) {
			if (slot.getPlayerId() != null) {
				Player p = playerRepository.findById(slot.getPlayerId()).orElse(null);
				if (p != null) {
					players.add(p);
				}
			}
		}

		return players;
	}

	private Player selectGoalScorer(List<Player> players) {
		List<Player> forwards = new ArrayList<>();
		List<Player> midfielders = new ArrayList<>();
		List<Player> defenders = new ArrayList<>();

		for (Player p : players) {
			if ("FWD".equals(p.getPosition())) {
				forwards.add(p);
			} else if ("MID".equals(p.getPosition())) {
				midfielders.add(p);
			} else if ("DEF".equals(p.getPosition())) {
				defenders.add(p);
			}
		}

		double rand = random.nextDouble();

		if (rand < 0.5 && !forwards.isEmpty()) {
			return forwards.get(random.nextInt(forwards.size()));
		} else if (rand < 0.85 && !midfielders.isEmpty()) {
			return midfielders.get(random.nextInt(midfielders.size()));
		} else if (!defenders.isEmpty()) {
			return defenders.get(random.nextInt(defenders.size()));
		}

		return players.isEmpty() ? null : players.get(random.nextInt(players.size()));
	}

	private Player findPlayerByName(String name, Long teamId) {
		List<Player> players = playerRepository.findByTeamId(teamId);
		for (Player p : players) {
			if (p.getName().equals(name)) {
				return p;
			}
		}
		return null;
	}

	private void trainPlayersAfterMatch(Long teamId, String formationId) {
		try {
			List<LineupSlot> lineup = lineupRepository.findByTeamIdAndFormationId(teamId, formationId);
			Random rand = new Random();

			for (LineupSlot slot : lineup) {
				if (slot.getPlayerId() != null) {
					Player player = playerRepository.findById(slot.getPlayerId()).orElse(null);
					if (player != null) {
						player.trainAfterMatch(rand);
						playerRepository.save(player);
					}
				}
			}
		} catch (Exception e) {
			System.err.println("[Training] Fehler: " + e.getMessage());
		}
	}

	@Autowired
	private MatchdayRepository matchdayRepository;

	@Autowired
	private com.example.manager.repository.SponsorRepository sponsorRepository;

	/**
	 * Gibt den aktuellen Status der Simulation zurück
	 */
	public LiveSimulationStatusDTO getSimulationStatus() {
		if (!simulationRunning) {
			return new LiveSimulationStatusDTO(false, null, null, 0, SIMULATION_DURATION_SECONDS, 0);
		}

		LocalDateTime now = LocalDateTime.now();
		long secondsElapsed = java.time.Duration.between(simulationStartTime, now).getSeconds();
		long secondsRemaining = Math.max(0, SIMULATION_DURATION_SECONDS - secondsElapsed);
		int currentMinute = (int) ((secondsElapsed / 3) % 91); // 3 Sekunden pro Spielminute

		LocalDateTime expectedEnd = simulationStartTime.plusSeconds(SIMULATION_DURATION_SECONDS);

		return new LiveSimulationStatusDTO(true, simulationStartTime, expectedEnd, currentMinute,
				SIMULATION_DURATION_SECONDS, secondsRemaining);
	}

	public boolean isSimulationRunning() {
		return simulationRunning;
	}

	/**
	 * Erlaubt User Substitution während der Live-Simulation
	 */
	@Transactional
	public void substitutePlayer(Long matchId, Long teamId, Long playerOutId, Long playerInId) {
		if (!simulationRunning) {
			throw new IllegalStateException("Keine aktive Simulation!");
		}

		Match match = matchRepository.findById(matchId).orElse(null);
		if (match == null) {
			throw new IllegalArgumentException("Match nicht gefunden!");
		}

		// Prüfe ob Team am Match beteiligt ist
		if (!match.getHomeTeamId().equals(teamId) && !match.getAwayTeamId().equals(teamId)) {
			throw new IllegalArgumentException("Team ist nicht an diesem Match beteiligt!");
		}

		Team team = teamRepository.findById(teamId).orElse(null);
		if (team == null) {
			throw new IllegalArgumentException("Team nicht gefunden!");
		}

		Player playerOut = playerRepository.findById(playerOutId).orElse(null);
		Player playerIn = playerRepository.findById(playerInId).orElse(null);

		if (playerOut == null || playerIn == null) {
			throw new IllegalArgumentException("Spieler nicht gefunden!");
		}

		// Prüfe ob beide Spieler zum selben Team gehören
		if (!playerOut.getTeamId().equals(teamId) || !playerIn.getTeamId().equals(teamId)) {
			throw new IllegalArgumentException("Spieler gehören nicht zum Team!");
		}

		String formation = team.getActiveFormation() != null ? team.getActiveFormation() : "4-4-2";

		// Finde Lineup-Slot des auszuwechselnden Spielers
		List<LineupSlot> lineup = lineupRepository.findByTeamIdAndFormationId(teamId, formation);
		LineupSlot slotOut = null;

		for (LineupSlot slot : lineup) {
			if (slot.getPlayerId() != null && slot.getPlayerId().equals(playerOutId)) {
				slotOut = slot;
				break;
			}
		}

		if (slotOut == null) {
			throw new IllegalArgumentException("Spieler ist nicht in der Aufstellung!");
		}

		// Wechsle Spieler aus
		slotOut.setPlayerId(playerInId);
		lineupRepository.save(slotOut);

		// Berechne aktuelle Spielminute
		LocalDateTime now = LocalDateTime.now();
		long secondsElapsed = java.time.Duration.between(simulationStartTime, now).getSeconds();
		int currentMinute = (int) ((secondsElapsed / 3) % 91);

		// Erstelle Substitution Event
		LiveMatchEventDTO subEvent = new LiveMatchEventDTO(matchId, "substitution", currentMinute, team.getName(),
				playerOut.getName() + " ➡️ " + playerIn.getName(), "Auswechslung bei " + team.getName() + ": "
						+ playerOut.getName() + " raus, " + playerIn.getName() + " rein! 🔄",
				null, null);

		// Sende Event
		messagingTemplate.convertAndSend("/topic/live-match/" + matchId, subEvent);
		messagingTemplate.convertAndSend("/topic/live-match/all", subEvent);

		System.out.println("[LiveSimulation] 🔄 Substitution: " + team.getName() + " - " + playerOut.getName() + " ➡️ "
				+ playerIn.getName());
	}

	/**
	 * Lädt alle gespeicherten Nachrichten für ein Match
	 */
	public List<SimulationMessageDTO> getSimulationMessages(Long matchId) {
		List<SimulationMessage> messages = simulationMessageRepository.findByMatchIdOrderByCreatedAtAsc(matchId);
		List<SimulationMessageDTO> dtos = new ArrayList<>();

		for (SimulationMessage msg : messages) {
			SimulationMessageDTO dto = new SimulationMessageDTO(msg.getMatchId(), msg.getTeamId(), msg.getType(),
					msg.getMinute(), msg.getTeamName(), msg.getPlayerName(), msg.getDescription(), msg.getHomeGoals(),
					msg.getAwayGoals(), msg.isHomeTeam());
			dto.setId(msg.getId());
			dtos.add(dto);
		}

		return dtos;
	}

	/**
	 * Löscht alle Nachrichten für ein Match (wird nach Spielende aufgerufen)
	 */
	public void clearSimulationMessages(Long matchId) {
		simulationMessageRepository.deleteByMatchId(matchId);
		System.out.println("[LiveSimulation] 🗑️ Nachrichten für Match " + matchId + " gelöscht");
	}

	/**
	 * Internes Event-Objekt für die Simulation
	 */
	private static class SimulationEvent {
		String type;
		int minute;
		String teamName;
		String playerName;
		String description;
		boolean isHomeTeam;

		SimulationEvent(String type, int minute, String teamName, String playerName, String description,
				boolean isHomeTeam) {
			this.type = type;
			this.minute = minute;
			this.teamName = teamName;
			this.playerName = playerName;
			this.description = description;
			this.isHomeTeam = isHomeTeam;
		}
	}

	/**
	 * Reduziert die Fitness aller spielenden Spieler um 1 Wird jede Spielminute
	 * aufgerufen (alle 3 Sekunden in Echtzeit)
	 */
	private void reducePlayerStamina(Match match) {
		try {
			Team homeTeam = teamRepository.findById(match.getHomeTeamId()).orElse(null);
			Team awayTeam = teamRepository.findById(match.getAwayTeamId()).orElse(null);

			if (homeTeam == null || awayTeam == null)
				return;

			String homeFormation = homeTeam.getActiveFormation() != null ? homeTeam.getActiveFormation() : "4-4-2";
			String awayFormation = awayTeam.getActiveFormation() != null ? awayTeam.getActiveFormation() : "4-4-2";

			// Reduziere Fitness für Home-Team Spieler
			List<LineupSlot> homeLineup = lineupRepository.findByTeamIdAndFormationId(match.getHomeTeamId(),
					homeFormation);
			for (LineupSlot slot : homeLineup) {
				if (slot.getPlayerId() != null) {
					Player player = playerRepository.findById(slot.getPlayerId()).orElse(null);
					if (player != null) {
						// Reduziere Fitness um 1, mindestens 0
						int newFitness = Math.max(0, player.getFitness() - 1);
						player.setFitness(newFitness);
						playerRepository.save(player);
					}
				}
			}

			// Reduziere Fitness für Away-Team Spieler
			List<LineupSlot> awayLineup = lineupRepository.findByTeamIdAndFormationId(match.getAwayTeamId(),
					awayFormation);
			for (LineupSlot slot : awayLineup) {
				if (slot.getPlayerId() != null) {
					Player player = playerRepository.findById(slot.getPlayerId()).orElse(null);
					if (player != null) {
						// Reduziere Fitness um 1, mindestens 0
						int newFitness = Math.max(0, player.getFitness() - 1);
						player.setFitness(newFitness);
						playerRepository.save(player);
					}
				}
			}
		} catch (Exception e) {
			System.err.println("[Stamina] Fehler beim Reduzieren der Fitness: " + e.getMessage());
		}
	}
}
