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

	@Autowired
	private PlayerRatingService playerRatingService;

	@Autowired
	private com.example.manager.repository.PlayerPerformanceRepository playerPerformanceRepository;

	@Autowired
	private com.example.manager.repository.CupTournamentRepository cupTournamentRepository;
	
	@Autowired
	private com.example.manager.repository.CupMatchRepository cupMatchRepository;
	
	@Autowired
	private CupService cupService;

	private final Random random = new Random();
	private boolean simulationRunning = false;
	private LocalDateTime simulationStartTime;
	private ScheduledExecutorService executor;

	private static final int SIMULATION_DURATION_SECONDS = 27; // 27 Sekunden (10x schneller)
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

		// Lösche alte Nachrichten für dieses Match, um frisch zu starten
		clearSimulationMessages(userMatch.getId());

		// Lösche auch alte MatchEvents
		List<MatchEvent> oldEvents = matchEventRepository.findByMatchId(userMatch.getId());
		if (!oldEvents.isEmpty()) {
			matchEventRepository.deleteAll(oldEvents);
			System.out.println("[LiveSimulation] 🗑️ " + oldEvents.size() + " alte MatchEvents gelöscht");
		}

		// Starte Simulation in separatem Thread
		executor = Executors.newSingleThreadScheduledExecutor();

		// Sende Start-Event für das Match mit kleiner Verzögerung (100ms)
		// damit WebSocket-Verbindung garantiert steht
		final Match matchToStart = userMatch; // Final für Lambda
		executor.schedule(() -> {
			sendMatchStartEvent(matchToStart);
		}, 100, TimeUnit.MILLISECONDS);

		// Simuliere Spiel-Events über 27 Sekunden (nur für dieses Match)
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

		// Lösche alte Nachrichten für alle Matches
		for (Match match : allMatches) {
			clearSimulationMessages(match.getId());
			List<MatchEvent> oldEvents = matchEventRepository.findByMatchId(match.getId());
			if (!oldEvents.isEmpty()) {
				matchEventRepository.deleteAll(oldEvents);
			}
		}

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

		// Sende Events basierend auf Echtzeit (0.3 Sekunden = 1 Spielminute, 10x
		// schneller)
		for (int minute = 1; minute <= GAME_MINUTES; minute++) {
			final int currentMinute = minute;
			long delayMillis = 100 + (minute - 1) * 300L; // 100ms Offset + 0.3 Sekunden pro Spielminute

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
				if (currentMinute % 2 == 0) { // Jede Minute
					for (Match match : matches) {
						reducePlayerFitness(match);
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

		// Nach 27 Sekunden + 100ms Offset: Simulation beenden und Ergebnisse speichern
		executor.schedule(() -> {
			finishSimulation(matches, eventTimelines);
		}, 100 + SIMULATION_DURATION_SECONDS * 1000L, TimeUnit.MILLISECONDS);
	}

	/**
	 * Generiert Event-Timeline für ein Match
	 * Prüft auch ob Teams genug Spieler haben (min. 7)
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
		
		// Prüfe ob Teams genug Spieler in der Aufstellung haben (min. 7)
		String homeFormation = homeTeam.getActiveFormation() != null ? homeTeam.getActiveFormation() : "4-4-2";
		String awayFormation = awayTeam.getActiveFormation() != null ? awayTeam.getActiveFormation() : "4-4-2";
		
		List<LineupSlot> homeLineup = lineupRepository.findByTeamIdAndFormationId(homeTeamId, homeFormation);
		List<LineupSlot> awayLineup = lineupRepository.findByTeamIdAndFormationId(awayTeamId, awayFormation);
		
		int homePlayerCount = (int) homeLineup.stream().filter(s -> s.getPlayerId() != null).count();
		int awayPlayerCount = (int) awayLineup.stream().filter(s -> s.getPlayerId() != null).count();
		
		System.out.println("[Match] " + homeTeam.getName() + " hat " + homePlayerCount + " Spieler, " + 
				awayTeam.getName() + " hat " + awayPlayerCount + " Spieler");
		
		// Wenn ein Team weniger als 7 Spieler hat: Automatische 0:3 Niederlage
		if (homePlayerCount < 7) {
			System.out.println("[Match] ⚠️ " + homeTeam.getName() + " hat zu wenig Spieler (<7)! Automatische 0:3 Niederlage!");
			// Setze direkt Ergebnis ohne Events zu generieren
			match.setHomeGoals(0);
			match.setAwayGoals(3);
			return events; // Leere Event-Liste
		}
		if (awayPlayerCount < 7) {
			System.out.println("[Match] ⚠️ " + awayTeam.getName() + " hat zu wenig Spieler (<7)! Automatische 0:3 Niederlage!");
			match.setHomeGoals(3);
			match.setAwayGoals(0);
			return events; // Leere Event-Liste
		}

		int homeStrength = calculateTeamStrength(homeTeamId);
		int awayStrength = calculateTeamStrength(awayTeamId);

		// Berechne Gesamtfitness beider Teams
		int homeTotalFitness = calculateTotalFitness(homeTeamId);
		int awayTotalFitness = calculateTotalFitness(awayTeamId);

		// Berechne Fitness-Unterschied
		int fitnessDiff = homeTotalFitness - awayTotalFitness;

		// Wende Fitness-Modifier an
		if (fitnessDiff > 280) {
			// Home Team hat >160 mehr Fitness -> Away Team -20%
			awayStrength = (int) (awayStrength * 0.65);
			System.out.println("[Fitness] Fitness-Vorteil Home: " + fitnessDiff + " -> Away Strength -20%");
		} else if (fitnessDiff > 240) {
			// Home Team hat >160 mehr Fitness -> Away Team -20%
			awayStrength = (int) (awayStrength * 0.70);
			System.out.println("[Fitness] Fitness-Vorteil Home: " + fitnessDiff + " -> Away Strength -20%");
		} else if (fitnessDiff > 200) {
			// Home Team hat >160 mehr Fitness -> Away Team -20%
			awayStrength = (int) (awayStrength * 0.75);
			System.out.println("[Fitness] Fitness-Vorteil Home: " + fitnessDiff + " -> Away Strength -20%");
		} else if (fitnessDiff > 160) {
			// Home Team hat >160 mehr Fitness -> Away Team -20%
			awayStrength = (int) (awayStrength * 0.80);
			System.out.println("[Fitness] Fitness-Vorteil Home: " + fitnessDiff + " -> Away Strength -20%");
		} else if (fitnessDiff > 120) {
			// Home Team hat >120 mehr Fitness -> Away Team -15%
			awayStrength = (int) (awayStrength * 0.85);
			System.out.println("[Fitness] Fitness-Vorteil Home: " + fitnessDiff + " -> Away Strength -15%");
		} else if (fitnessDiff > 80) {
			// Home Team hat >80 mehr Fitness -> Away Team -10%
			awayStrength = (int) (awayStrength * 0.90);
			System.out.println("[Fitness] Fitness-Vorteil Home: " + fitnessDiff + " -> Away Strength -10%");
		} else if (fitnessDiff > 40) {
			// Home Team hat >40 mehr Fitness -> Away Team -5%
			awayStrength = (int) (awayStrength * 0.95);
			System.out.println("[Fitness] Fitness-Vorteil Home: " + fitnessDiff + " -> Away Strength -5%");
		} else if (fitnessDiff < -280) {
			// Away Team hat >160 mehr Fitness -> Home Team -20%
			homeStrength = (int) (homeStrength * 0.65);
			System.out.println("[Fitness] Fitness-Vorteil Away: " + (-fitnessDiff) + " -> Home Strength -20%");
		} else if (fitnessDiff < -240) {
			// Away Team hat >160 mehr Fitness -> Home Team -20%
			homeStrength = (int) (homeStrength * 0.70);
			System.out.println("[Fitness] Fitness-Vorteil Away: " + (-fitnessDiff) + " -> Home Strength -20%");
		} else if (fitnessDiff < -200) {
			// Away Team hat >160 mehr Fitness -> Home Team -20%
			homeStrength = (int) (homeStrength * 0.75);
			System.out.println("[Fitness] Fitness-Vorteil Away: " + (-fitnessDiff) + " -> Home Strength -20%");
		} else if (fitnessDiff < -160) {
			// Away Team hat >160 mehr Fitness -> Home Team -20%
			homeStrength = (int) (homeStrength * 0.80);
			System.out.println("[Fitness] Fitness-Vorteil Away: " + (-fitnessDiff) + " -> Home Strength -20%");
		} else if (fitnessDiff < -120) {
			// Away Team hat >120 mehr Fitness -> Home Team -15%
			homeStrength = (int) (homeStrength * 0.85);
			System.out.println("[Fitness] Fitness-Vorteil Away: " + (-fitnessDiff) + " -> Home Strength -15%");
		} else if (fitnessDiff < -80) {
			// Away Team hat >80 mehr Fitness -> Home Team -10%
			homeStrength = (int) (homeStrength * 0.90);
			System.out.println("[Fitness] Fitness-Vorteil Away: " + (-fitnessDiff) + " -> Home Strength -10%");
		} else if (fitnessDiff < -40) {
			// Away Team hat >40 mehr Fitness -> Home Team -5%
			homeStrength = (int) (homeStrength * 0.95);
			System.out.println("[Fitness] Fitness-Vorteil Away: " + (-fitnessDiff) + " -> Home Strength -5%");
		}

		System.out.println(
				"[Fitness] Home Total Fitness: " + homeTotalFitness + ", Away Total Fitness: " + awayTotalFitness);
		System.out.println("[Fitness] Adjusted Strengths - Home: " + homeStrength + ", Away: " + awayStrength);

		// Generiere Tor-Events mit angepassten Stärken
		int homeGoals = generateGoals(homeStrength, awayStrength);
		int awayGoals = generateGoals(awayStrength, homeStrength);

		List<Player> homePlayers = getLineupPlayers(homeTeamId,
				homeTeam.getActiveFormation() != null ? homeTeam.getActiveFormation() : "4-4-2");
		List<Player> awayPlayers = getLineupPlayers(awayTeamId,
				awayTeam.getActiveFormation() != null ? awayTeam.getActiveFormation() : "4-4-2");

		// Generiere Home-Team Tore MIT VORLAGEN
		for (int i = 0; i < homeGoals; i++) {
			int minute = 1 + random.nextInt(89);
			Player scorer = selectGoalScorer(homePlayers);
			if (scorer != null) {
				// 60% Chance auf Vorlage (aber keine Torhüter als Vorlagengeber)
				Player assist = null;
				if (random.nextDouble() < 0.6) {
					// Wähle zufälligen Mitspieler als Vorlagengeber (KEINE TORHÜTER)
					List<Player> possibleAssisters = new ArrayList<>();
					for (Player p : homePlayers) {
						if (!p.getId().equals(scorer.getId()) && !"GK".equals(p.getPosition())) {
							possibleAssisters.add(p);
						}
					}
					if (!possibleAssisters.isEmpty()) {
						assist = possibleAssisters.get(random.nextInt(possibleAssisters.size()));
					}
				}
				
				String goalText;
				if (assist != null) {
					goalText = "⚽ TOR! " + scorer.getName() + " trifft für " + homeTeam.getName() + "! Vorlage von " + assist.getName() + "!";
				} else {
					goalText = "⚽ TOR! " + scorer.getName() + " erzielt ein Tor für " + homeTeam.getName() + "!";
				}
				
				SimulationEvent goalEvent = new SimulationEvent("goal", minute, homeTeam.getName(), scorer.getName(), goalText, true);
				if (assist != null) {
					goalEvent.assistPlayerName = assist.getName();
				}
				events.add(goalEvent);
			}
		}

		// Generiere Away-Team Tore MIT VORLAGEN
		for (int i = 0; i < awayGoals; i++) {
			int minute = 1 + random.nextInt(89);
			Player scorer = selectGoalScorer(awayPlayers);
			if (scorer != null) {
				// 60% Chance auf Vorlage (aber keine Torhüter als Vorlagengeber)
				Player assist = null;
				if (random.nextDouble() < 0.6) {
					// Wähle zufälligen Mitspieler als Vorlagengeber (KEINE TORHÜTER)
					List<Player> possibleAssisters = new ArrayList<>();
					for (Player p : awayPlayers) {
						if (!p.getId().equals(scorer.getId()) && !"GK".equals(p.getPosition())) {
							possibleAssisters.add(p);
						}
					}
					if (!possibleAssisters.isEmpty()) {
						assist = possibleAssisters.get(random.nextInt(possibleAssisters.size()));
					}
				}
				
				String goalText;
				if (assist != null) {
					goalText = "⚽ TOR! " + scorer.getName() + " trifft für " + awayTeam.getName() + "! Vorlage von " + assist.getName() + "!";
				} else {
					goalText = "⚽ TOR! " + scorer.getName() + " erzielt ein Tor für " + awayTeam.getName() + "!";
				}
				
				SimulationEvent goalEvent = new SimulationEvent("goal", minute, awayTeam.getName(), scorer.getName(), goalText, false);
				if (assist != null) {
					goalEvent.assistPlayerName = assist.getName();
				}
				events.add(goalEvent);
			}
		}

		// Generiere MEHR DETAILLIERTE Chancen (10-20 pro Team, alle 2 Minuten mindestens eine)
		int homeChances = 10 + random.nextInt(11); // 10-20
		int awayChances = 10 + random.nextInt(11);

		for (int i = 0; i < homeChances; i++) {
			int minute = 1 + random.nextInt(89);
			Player player = homePlayers.isEmpty() ? null : homePlayers.get(random.nextInt(homePlayers.size()));
			if (player != null) {
				String[] chanceTexts = { 
					player.getName() + " hat eine gute Chance, aber der Ball geht knapp am Tor vorbei!",
					player.getName() + " zieht von der Strafraumgrenze ab - der Torwart pariert stark!",
					"Tolle Kombination von " + homeTeam.getName() + "! " + player.getName() + " schießt, aber knapp daneben!",
					player.getName() + " mit einem Fernschuss - der Ball rauscht nur wenige Zentimeter am Pfosten vorbei!",
					"Kopfballchance für " + player.getName() + " nach einer Ecke - zu ungenau!",
					player.getName() + " wird von der Abwehr in letzter Sekunde gestoppt!",
					"Konter von " + homeTeam.getName() + "! " + player.getName() + " läuft alleine auf das Tor zu, aber der Torwart rettet!",
					player.getName() + " nimmt den Ball an und schießt sofort - der Keeper hält spektakulär!"
				};
				events.add(new SimulationEvent("chance", minute, homeTeam.getName(), player.getName(),
						chanceTexts[random.nextInt(chanceTexts.length)], true));
			}
		}

		for (int i = 0; i < awayChances; i++) {
			int minute = 1 + random.nextInt(89);
			Player player = awayPlayers.isEmpty() ? null : awayPlayers.get(random.nextInt(awayPlayers.size()));
			if (player != null) {
				String[] chanceTexts = { 
					player.getName() + " hat eine gute Chance, aber der Ball geht knapp am Tor vorbei!",
					player.getName() + " zieht von der Strafraumgrenze ab - der Torwart pariert stark!",
					"Tolle Kombination von " + awayTeam.getName() + "! " + player.getName() + " schießt, aber knapp daneben!",
					player.getName() + " mit einem Fernschuss - der Ball rauscht nur wenige Zentimeter am Pfosten vorbei!",
					"Kopfballchance für " + player.getName() + " nach einer Ecke - zu ungenau!",
					player.getName() + " wird von der Abwehr in letzter Sekunde gestoppt!",
					"Konter von " + awayTeam.getName() + "! " + player.getName() + " läuft alleine auf das Tor zu, aber der Torwart rettet!",
					player.getName() + " nimmt den Ball an und schießt sofort - der Keeper hält spektakulär!"
				};
				events.add(new SimulationEvent("chance", minute, awayTeam.getName(), player.getName(),
						chanceTexts[random.nextInt(chanceTexts.length)], false));
			}
		}
		
		// Generiere ZUSÄTZLICHE allgemeine Spielaktionen (15-25 pro Team)
		int homeActions = 15 + random.nextInt(11);
		int awayActions = 15 + random.nextInt(11);
		
		for (int i = 0; i < homeActions; i++) {
			int minute = 1 + random.nextInt(89);
			Player player = homePlayers.isEmpty() ? null : homePlayers.get(random.nextInt(homePlayers.size()));
			if (player != null) {
				String[] actionTexts = {
					player.getName() + " setzt sich im Mittelfeld durch und spielt einen guten Pass!",
					player.getName() + " gewinnt den Zweikampf und leitet einen Angriff ein!",
					"Starke Grätsche von " + player.getName() + " - der Ball ist erobert!",
					player.getName() + " mit einer präzisen Flanke in den Strafraum!",
					player.getName() + " dribbelt sich durch, aber verliert dann den Ball.",
					"Guter Laufweg von " + player.getName() + ", aber der Pass kommt nicht an.",
					player.getName() + " blockt einen gegnerischen Schuss!",
					player.getName() + " mit einer wichtigen Balleroberung im eigenen Drittel!"
				};
				events.add(new SimulationEvent("action", minute, homeTeam.getName(), player.getName(),
						actionTexts[random.nextInt(actionTexts.length)], true));
			}
		}
		
		for (int i = 0; i < awayActions; i++) {
			int minute = 1 + random.nextInt(89);
			Player player = awayPlayers.isEmpty() ? null : awayPlayers.get(random.nextInt(awayPlayers.size()));
			if (player != null) {
				String[] actionTexts = {
					player.getName() + " setzt sich im Mittelfeld durch und spielt einen guten Pass!",
					player.getName() + " gewinnt den Zweikampf und leitet einen Angriff ein!",
					"Starke Grätsche von " + player.getName() + " - der Ball ist erobert!",
					player.getName() + " mit einer präzisen Flanke in den Strafraum!",
					player.getName() + " dribbelt sich durch, aber verliert dann den Ball.",
					"Guter Laufweg von " + player.getName() + ", aber der Pass kommt nicht an.",
					player.getName() + " blockt einen gegnerischen Schuss!",
					player.getName() + " mit einer wichtigen Balleroberung im eigenen Drittel!"
				};
				events.add(new SimulationEvent("action", minute, awayTeam.getName(), player.getName(),
						actionTexts[random.nextInt(actionTexts.length)], false));
			}
		}
		
		// Generiere FEHLER die zu Chancen führen (2-5 pro Team)
		// Nach jedem Fehler folgt DIREKT eine Chance oder Tor für das andere Team
		int homeErrors = 2 + random.nextInt(4);
		int awayErrors = 2 + random.nextInt(4);
		
		for (int i = 0; i < homeErrors; i++) {
			int minute = 1 + random.nextInt(88); // Max 88 damit danach noch Event kommt
			Player player = homePlayers.isEmpty() ? null : homePlayers.get(random.nextInt(homePlayers.size()));
			if (player != null) {
				String[] errorTexts = {
					"⚠️ Fehlpass von " + player.getName() + " - " + awayTeam.getName() + " kommt gefährlich vor das Tor!",
					"⚠️ " + player.getName() + " verliert den Ball im Mittelfeld - gefährliche Situation!",
					"⚠️ Ballverlust von " + player.getName() + " - " + awayTeam.getName() + " kontert sofort!",
					"⚠️ " + player.getName() + " leistet sich einen Fehlpass - " + awayTeam.getName() + " schaltet schnell um!"
				};
				events.add(new SimulationEvent("error", minute, homeTeam.getName(), player.getName(),
						errorTexts[random.nextInt(errorTexts.length)], true));
				
				// Direkt danach: Chance oder Tor für Gegner (80% Chance, 20% Tor)
				Player counterPlayer = awayPlayers.isEmpty() ? null : awayPlayers.get(random.nextInt(awayPlayers.size()));
				if (counterPlayer != null) {
					if (random.nextDouble() < 0.8) {
						// Chance
						String[] counterChanceTexts = {
							counterPlayer.getName() + " nutzt den Fehler, aber der Schuss geht vorbei!",
							"Konter! " + counterPlayer.getName() + " läuft alleine auf das Tor, aber der Keeper rettet!",
							counterPlayer.getName() + " mit der Riesenchance nach Ballverlust - knapp daneben!",
							"Gefährlich! " + counterPlayer.getName() + " zieht ab, aber der Ball verfehlt das Ziel!"
						};
						events.add(new SimulationEvent("chance", minute + 1, awayTeam.getName(), counterPlayer.getName(),
								counterChanceTexts[random.nextInt(counterChanceTexts.length)], false));
					}
					// 20% Tor wird durch normale Tor-Generierung abgedeckt
				}
			}
		}
		
		for (int i = 0; i < awayErrors; i++) {
			int minute = 1 + random.nextInt(88);
			Player player = awayPlayers.isEmpty() ? null : awayPlayers.get(random.nextInt(awayPlayers.size()));
			if (player != null) {
				String[] errorTexts = {
					"⚠️ Fehlpass von " + player.getName() + " - " + homeTeam.getName() + " kommt gefährlich vor das Tor!",
					"⚠️ " + player.getName() + " verliert den Ball im Mittelfeld - gefährliche Situation!",
					"⚠️ Ballverlust von " + player.getName() + " - " + homeTeam.getName() + " kontert sofort!",
					"⚠️ " + player.getName() + " leistet sich einen Fehlpass - " + homeTeam.getName() + " schaltet schnell um!"
				};
				events.add(new SimulationEvent("error", minute, awayTeam.getName(), player.getName(),
						errorTexts[random.nextInt(errorTexts.length)], false));
				
				// Direkt danach: Chance für Gegner
				Player counterPlayer = homePlayers.isEmpty() ? null : homePlayers.get(random.nextInt(homePlayers.size()));
				if (counterPlayer != null) {
					if (random.nextDouble() < 0.8) {
						// Chance
						String[] counterChanceTexts = {
							counterPlayer.getName() + " nutzt den Fehler, aber der Schuss geht vorbei!",
							"Konter! " + counterPlayer.getName() + " läuft alleine auf das Tor, aber der Keeper rettet!",
							counterPlayer.getName() + " mit der Riesenchance nach Ballverlust - knapp daneben!",
							"Gefährlich! " + counterPlayer.getName() + " zieht ab, aber der Ball verfehlt das Ziel!"
						};
						events.add(new SimulationEvent("chance", minute + 1, homeTeam.getName(), counterPlayer.getName(),
								counterChanceTexts[random.nextInt(counterChanceTexts.length)], true));
					}
				}
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

		// Verletzungen (0-2 pro Match, selten) MIT automatischer Auswechslung
		int injuries = random.nextDouble() < 0.3 ? (random.nextBoolean() ? 1 : 2) : 0;
		for (int i = 0; i < injuries; i++) {
			int minute = 10 + random.nextInt(80);
			boolean isHome = random.nextBoolean();
			List<Player> players = isHome ? homePlayers : awayPlayers;
			String teamName = isHome ? homeTeam.getName() : awayTeam.getName();
			Long teamId = isHome ? homeTeamId : awayTeamId;

			if (!players.isEmpty()) {
				Player injuredPlayer = players.get(random.nextInt(players.size()));
				
				// Verletzungs-Event
				events.add(new SimulationEvent("injury", minute, teamName, injuredPlayer.getName(),
						injuredPlayer.getName() + " (" + teamName + ") verletzt sich! 🚑",
						isHome));
				
				// Finde Ersatzspieler (gleiche Position bevorzugt)
				Player substitute = findSubstitute(teamId, injuredPlayer, players);
				if (substitute != null) {
					// Auswechslungs-Event direkt danach
					SimulationEvent subEvent = new SimulationEvent("substitution", minute + 1, teamName, substitute.getName(),
							"🔄 Auswechslung: " + injuredPlayer.getName() + " raus, " + substitute.getName() + " (" + substitute.getPosition() + ") rein",
							isHome);
					subEvent.injuredPlayerName = injuredPlayer.getName(); // Merke verletzten Spieler
					events.add(subEvent);
				}
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
				
				// Speichere auch Vorlagengeber wenn vorhanden
				if (event.assistPlayerName != null) {
					Player assistPlayer = findPlayerByName(event.assistPlayerName,
							event.isHomeTeam ? match.getHomeTeamId() : match.getAwayTeamId());
					if (assistPlayer != null) {
						matchEvent.setAssistPlayerId(assistPlayer.getId());
						matchEvent.setAssistPlayerName(assistPlayer.getName());
						
						// Erstelle auch ein separates "assist" Event für den Vorlagengeber
						MatchEvent assistEvent = new MatchEvent(match.getId(),
								event.isHomeTeam ? match.getHomeTeamId() : match.getAwayTeamId(),
								assistPlayer.getId(), assistPlayer.getName(), "assist", event.minute);
						matchEventRepository.save(assistEvent);
					}
				}
				
				matchEventRepository.save(matchEvent);
			}
		}
		
		// Speichere auch andere Event-Typen (chance_created, error, etc.)
		if ("chance".equals(event.type)) {
			// Speichere als "chance_created" Event
			Player player = findPlayerByName(event.playerName,
					event.isHomeTeam ? match.getHomeTeamId() : match.getAwayTeamId());
			if (player != null) {
				MatchEvent matchEvent = new MatchEvent(match.getId(),
						event.isHomeTeam ? match.getHomeTeamId() : match.getAwayTeamId(),
						player.getId(), player.getName(), "chance_created", event.minute);
				matchEventRepository.save(matchEvent);
			}
		}
		
		if ("error".equals(event.type)) {
			// Speichere Fehler-Event
			Player player = findPlayerByName(event.playerName,
					event.isHomeTeam ? match.getHomeTeamId() : match.getAwayTeamId());
			if (player != null) {
				MatchEvent matchEvent = new MatchEvent(match.getId(),
						event.isHomeTeam ? match.getHomeTeamId() : match.getAwayTeamId(),
						player.getId(), player.getName(), "error", event.minute);
				matchEventRepository.save(matchEvent);
			}
		}
		
		if ("yellow_card".equals(event.type)) {
			// Speichere Gelbe Karte
			Player player = findPlayerByName(event.playerName,
					event.isHomeTeam ? match.getHomeTeamId() : match.getAwayTeamId());
			if (player != null) {
				MatchEvent matchEvent = new MatchEvent(match.getId(),
						event.isHomeTeam ? match.getHomeTeamId() : match.getAwayTeamId(),
						player.getId(), player.getName(), "yellow_card", event.minute);
				matchEventRepository.save(matchEvent);
			}
		}
		
		if ("red_card".equals(event.type)) {
			// Speichere Rote Karte
			Player player = findPlayerByName(event.playerName,
					event.isHomeTeam ? match.getHomeTeamId() : match.getAwayTeamId());
			if (player != null) {
				MatchEvent matchEvent = new MatchEvent(match.getId(),
						event.isHomeTeam ? match.getHomeTeamId() : match.getAwayTeamId(),
						player.getId(), player.getName(), "red_card", event.minute);
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

			// Erstelle PlayerPerformance-Einträge für beide Teams
			createPlayerPerformances(match, match.getHomeTeamId(), 
					homeTeam.getActiveFormation() != null ? homeTeam.getActiveFormation() : "4-4-2", 
					homeGoals, awayGoals, true);
			createPlayerPerformances(match, match.getAwayTeamId(), 
					awayTeam.getActiveFormation() != null ? awayTeam.getActiveFormation() : "4-4-2", 
					homeGoals, awayGoals, false);

			// Trainiere Spieler mit Match-Kontext
			trainPlayersAfterMatch(match, match.getHomeTeamId(),
					homeTeam.getActiveFormation() != null ? homeTeam.getActiveFormation() : "4-4-2");
			trainPlayersAfterMatch(match, match.getAwayTeamId(),
					awayTeam.getActiveFormation() != null ? awayTeam.getActiveFormation() : "4-4-2");

			// Regeneriere Fitness für beide Teams nach dem Spiel
			regeneratePlayerFitness(match.getHomeTeamId());
			regeneratePlayerFitness(match.getAwayTeamId());

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
	 * Berechnet die Gesamtfitness aller Spieler in der Aufstellung
	 */
	private int calculateTotalFitness(Long teamId) {
		Team team = teamRepository.findById(teamId).orElse(null);
		if (team == null)
			return 0;

		String formation = team.getActiveFormation() != null ? team.getActiveFormation() : "4-4-2";
		List<LineupSlot> lineup = lineupRepository.findByTeamIdAndFormationId(teamId, formation);
		int totalFitness = 0;

		for (LineupSlot slot : lineup) {
			if (slot.getPlayerId() != null) {
				Player p = playerRepository.findById(slot.getPlayerId()).orElse(null);
				if (p != null) {
					totalFitness += p.getFitness();
				}
			}
		}

		return totalFitness;
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

	/**
	 * Erstellt PlayerPerformance-Einträge für alle Spieler der Aufstellung
	 */
	private void createPlayerPerformances(Match match, Long teamId, String formationId, 
			int homeGoals, int awayGoals, boolean isHomeTeam) {
		try {
			List<LineupSlot> lineup = lineupRepository.findByTeamIdAndFormationId(teamId, formationId);
			List<MatchEvent> allEvents = matchEventRepository.findByMatchId(match.getId());
			
			for (LineupSlot slot : lineup) {
				if (slot.getPlayerId() != null) {
					Player player = playerRepository.findById(slot.getPlayerId()).orElse(null);
					if (player != null) {
						// Erstelle Performance-Eintrag
						com.example.manager.model.PlayerPerformance performance = 
								new com.example.manager.model.PlayerPerformance(match.getId(), player.getId(), teamId);
						
						// Zähle Statistiken aus MatchEvents
						int goals = 0;
						int assists = 0;
						int yellowCards = 0;
						int redCards = 0;
						
						for (MatchEvent event : allEvents) {
							if (event.getPlayerId() != null && event.getPlayerId().equals(player.getId())) {
								switch (event.getType()) {
									case "goal":
										goals++;
										break;
									case "assist":
										assists++;
										break;
									case "yellow_card":
										yellowCards++;
										break;
									case "red_card":
										redCards++;
										break;
								}
							}
						}
						
						performance.setGoals(goals);
						performance.setAssists(assists);
						performance.setYellowCards(yellowCards);
						performance.setRedCards(redCards);
						performance.setMinutesPlayed(90); // Aktuell spielt jeder 90 Minuten
						
						// Berechne Spielernote
						double rating = playerRatingService.calculatePlayerRating(
								player.getId(), 
								match.getId(), 
								teamId,
								homeGoals,
								awayGoals,
								isHomeTeam
						);
						performance.setRating(rating);
						
						// Speichere Performance
						playerPerformanceRepository.save(performance);
						
						System.out.println("[Performance] " + player.getName() + ": Note " + 
								String.format("%.1f", rating) + ", " + goals + " Tore, " + 
								assists + " Vorlagen");
					}
				}
			}
		} catch (Exception e) {
			System.err.println("[Performance] Fehler beim Erstellen: " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * Trainiert Spieler nach Match basierend auf ihrer Performance-Note
	 */
	private void trainPlayersAfterMatch(Match match, Long teamId, String formationId) {
		try {
			List<LineupSlot> lineup = lineupRepository.findByTeamIdAndFormationId(teamId, formationId);
			
			// Ermittle ob Team Home oder Away ist
			boolean isHomeTeam = match.getHomeTeamId().equals(teamId);
			int homeGoals = match.getHomeGoals() != null ? match.getHomeGoals() : 0;
			int awayGoals = match.getAwayGoals() != null ? match.getAwayGoals() : 0;
			
			int totalSkillsImproved = 0;
			int playersRated = 0;

			for (LineupSlot slot : lineup) {
				if (slot.getPlayerId() != null) {
					Player player = playerRepository.findById(slot.getPlayerId()).orElse(null);
					if (player != null) {
						// Berechne Spielernote basierend auf Performance
						double rating = playerRatingService.calculatePlayerRating(
							player.getId(), 
							match.getId(), 
							teamId,
							homeGoals,
							awayGoals,
							isHomeTeam
						);
						
						// Speichere alte Skills zum Vergleich
						int skillsBefore = player.getPace() + player.getDribbling() + player.getBallControl() +
							player.getShooting() + player.getTackling() + player.getSliding() + 
							player.getHeading() + player.getCrossing() + player.getPassing() + 
							player.getAwareness() + player.getJumping() + player.getStamina() + 
							player.getStrength();
						
						// Trainiere basierend auf Note
						playerRatingService.trainPlayerByRating(player, rating);
						playerRepository.save(player);
						
						// Zähle verbesserte Skills
						int skillsAfter = player.getPace() + player.getDribbling() + player.getBallControl() +
							player.getShooting() + player.getTackling() + player.getSliding() + 
							player.getHeading() + player.getCrossing() + player.getPassing() + 
							player.getAwareness() + player.getJumping() + player.getStamina() + 
							player.getStrength();
						
						int improved = skillsAfter - skillsBefore;
						if (improved > 0) {
							totalSkillsImproved += improved;
							System.out.println("[Training] " + player.getName() + " (Note: " + 
								String.format("%.1f", rating) + ") verbesserte " + improved + " Skills");
						}
						
						playersRated++;
					}
				}
			}
			
			if (playersRated > 0) {
				System.out.println("[Training] Team " + teamId + ": " + playersRated + 
					" Spieler bewertet, " + totalSkillsImproved + " Skills insgesamt verbessert");
			}
		} catch (Exception e) {
			System.err.println("[Training] Fehler: " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	/**
	 * Findet einen Ersatzspieler für einen verletzten Spieler
	 * Bevorzugt gleiche Position, sonst andere Feldspieler (kein GK)
	 */
	private Player findSubstitute(Long teamId, Player injuredPlayer, List<Player> lineupPlayers) {
		// Hole alle Spieler des Teams
		List<Player> allPlayers = playerRepository.findByTeamId(teamId);
		
		// Entferne Spieler die bereits in der Aufstellung sind
		List<Player> benchPlayers = new ArrayList<>();
		for (Player p : allPlayers) {
			boolean inLineup = lineupPlayers.stream().anyMatch(lp -> lp.getId().equals(p.getId()));
			if (!inLineup && !"GK".equals(p.getPosition())) { // Keine GKs als Ersatz (außer für GK)
				benchPlayers.add(p);
			}
		}
		
		if (benchPlayers.isEmpty()) {
			return null; // Keine Ersatzspieler verfügbar
		}
		
		// Versuche gleiche Position zu finden
		String targetPosition = injuredPlayer.getPosition();
		List<Player> samePositionPlayers = benchPlayers.stream()
				.filter(p -> targetPosition.equals(p.getPosition()))
				.collect(java.util.stream.Collectors.toList());
		
		if (!samePositionPlayers.isEmpty()) {
			// Wähle besten Spieler der gleichen Position (höchstes Rating)
			return samePositionPlayers.stream()
					.max((a, b) -> Integer.compare(a.getRating(), b.getRating()))
					.orElse(null);
		}
		
		// Wenn keine gleiche Position: Wähle besten Feldspieler
		return benchPlayers.stream()
				.max((a, b) -> Integer.compare(a.getRating(), b.getRating()))
				.orElse(null);
	}

	/**
	 * Regeneriert Fitness aller Spieler im Team basierend auf ihrer Ausdauer
	 * >95 Ausdauer -> +50 Fitness
	 * >90 Ausdauer -> +47 Fitness
	 * >85 Ausdauer -> +44 Fitness
	 * >75 Ausdauer -> +41 Fitness
	 * >70 Ausdauer -> +38 Fitness
	 * >65 Ausdauer -> +35 Fitness
	 * >60 Ausdauer -> +32 Fitness
	 * <=60 Ausdauer -> +29 Fitness
	 */
	private void regeneratePlayerFitness(Long teamId) {
		try {
			List<Player> players = playerRepository.findByTeamId(teamId);
			
			for (Player player : players) {
				int stamina = player.getStamina();
				int currentFitness = player.getFitness();
				int regeneration = 0;
				
				if (stamina > 95) {
					regeneration = 50;
				} else if (stamina > 90) {
					regeneration = 47;
				} else if (stamina > 85) {
					regeneration = 44;
				} else if (stamina > 75) {
					regeneration = 41;
				} else if (stamina > 70) {
					regeneration = 38;
				} else if (stamina > 65) {
					regeneration = 35;
				} else if (stamina > 60) {
					regeneration = 32;
				} else {
					regeneration = 29;
				}
				
				// Addiere Regeneration, maximal 100
				int newFitness = Math.min(100, currentFitness + regeneration);
				player.setFitness(newFitness);
				playerRepository.save(player);
				
				System.out.println("[Regeneration] " + player.getName() + ": " + currentFitness + " -> " + newFitness 
					+ " (+" + regeneration + " durch " + stamina + " Stamina)");
			}
		} catch (Exception e) {
			System.err.println("[Regeneration] Fehler: " + e.getMessage());
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
		int currentMinute = (int) ((secondsElapsed / 0.3) % 91); // 0.3 Sekunden pro Spielminute (10x schneller)

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
		int currentMinute = (int) ((secondsElapsed / 0.3) % 91); // 0.3 Sekunden pro Spielminute (10x schneller)

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
		String assistPlayerName; // Für Vorlagen
		String injuredPlayerName; // Für Auswechslungen nach Verletzung

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
	private void reducePlayerFitness(Match match) {
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

	/**
	 * Startet die Live-Simulation für Pokalspiele des aktuellen Spieltags
	 * Wird um 20:00 Uhr aufgerufen für Spieltage 3, 6, 9, 12, 15, 18
	 */
	@Transactional
	public synchronized void startLiveCupSimulation() {
		if (simulationRunning) {
			throw new IllegalStateException("Simulation läuft bereits!");
		}

		int currentMatchday = repositoryService.getCurrentMatchday();

		// Prüfe ob aktueller Spieltag ein Pokalspiel-Tag ist
		if (!isCupMatchday(currentMatchday)) {
			throw new IllegalStateException("Spieltag " + currentMatchday + " hat keine Pokalspiele!");
		}

		// Finde alle Cup-Turniere für die aktuelle Saison
		int currentSeason = repositoryService.getCurrentSeason();
		List<com.example.manager.model.CupTournament> tournaments = 
			cupTournamentRepository.findAll().stream()
				.filter(t -> t.getSeason() == currentSeason)
				.collect(java.util.stream.Collectors.toList());

		if (tournaments.isEmpty()) {
			throw new IllegalStateException("Keine Cup-Turniere für diese Saison gefunden!");
		}

		// Sammle alle ausstehenden Cup-Matches
		List<com.example.manager.model.CupMatch> allCupMatches = new ArrayList<>();
		for (com.example.manager.model.CupTournament tournament : tournaments) {
			// Prüfe ob eine neue Runde generiert werden muss
			if (tournament.getCurrentRound() == 0) {
				// Generiere erste Runde
				cupService.generateCupRound(tournament, 1);
				tournament = cupTournamentRepository.findById(tournament.getId()).orElse(tournament);
			}
			
			// Sammle alle ausstehenden Matches für diese Runde
			int currentRound = tournament.getCurrentRound();
			List<com.example.manager.model.CupMatch> roundMatches = 
				cupMatchRepository.findByTournamentIdAndRound(tournament.getId(), currentRound);
			
			for (com.example.manager.model.CupMatch match : roundMatches) {
				if (!"completed".equals(match.getStatus())) {
					allCupMatches.add(match);
				}
			}
		}

		if (allCupMatches.isEmpty()) {
			throw new IllegalStateException("Keine ausstehenden Pokalspiele für diesen Spieltag!");
		}

		simulationRunning = true;
		simulationStartTime = LocalDateTime.now();

		System.out.println("[LiveCupSimulation] 🏆 Starte Live-Simulation für " + allCupMatches.size() + " Pokalspiele");

		// Starte Simulation in separatem Thread
		executor = Executors.newSingleThreadScheduledExecutor();

		// Sende Start-Events für alle Cup-Matches
		for (com.example.manager.model.CupMatch match : allCupMatches) {
			sendCupMatchStartEvent(match);
		}

		// Simuliere Spiel-Events über 270 Sekunden
		simulateCupMatchEvents(allCupMatches);
	}

	/**
	 * Prüft ob ein Spieltag ein Pokalspiel-Tag ist (3, 6, 9, 12, 15, 18)
	 */
	private boolean isCupMatchday(int matchday) {
		return matchday == 3 || matchday == 6 || matchday == 9 || 
		       matchday == 12 || matchday == 15 || matchday == 18;
	}

	/**
	 * Simuliert Events für alle Cup-Matches
	 */
	private void simulateCupMatchEvents(List<com.example.manager.model.CupMatch> cupMatches) {
		// Erstelle Event-Timeline für jedes Match
		Map<com.example.manager.model.CupMatch, List<SimulationEvent>> eventTimelines = new HashMap<>();

		for (com.example.manager.model.CupMatch cupMatch : cupMatches) {
			List<SimulationEvent> events = generateCupEventTimeline(cupMatch);
			eventTimelines.put(cupMatch, events);
		}

		// Sende Events basierend auf Echtzeit (0.3 Sekunden = 1 Spielminute)
		for (int minute = 1; minute <= GAME_MINUTES; minute++) {
			final int currentMinute = minute;
			long delayMillis = 100 + (minute - 1) * 300L;

			executor.schedule(() -> {
				for (Map.Entry<com.example.manager.model.CupMatch, List<SimulationEvent>> entry : eventTimelines.entrySet()) {
					com.example.manager.model.CupMatch cupMatch = entry.getKey();
					List<SimulationEvent> events = entry.getValue();

					// Sende alle Events für diese Minute
					for (SimulationEvent event : events) {
						if (event.minute == currentMinute) {
							sendLiveCupEvent(cupMatch, event);
						}
					}
				}

				// Halbzeit-Nachricht
				if (currentMinute == 45) {
					for (com.example.manager.model.CupMatch cupMatch : cupMatches) {
						sendCupHalftimeEvent(cupMatch);
					}
				}
			}, delayMillis, TimeUnit.MILLISECONDS);
		}

		// Nach 27 Sekunden: Simulation beenden und Ergebnisse speichern
		executor.schedule(() -> {
			finishCupSimulation(cupMatches, eventTimelines);
		}, 100 + SIMULATION_DURATION_SECONDS * 1000L, TimeUnit.MILLISECONDS);
	}

	/**
	 * Generiert Event-Timeline für ein Cup-Match
	 */
	private List<SimulationEvent> generateCupEventTimeline(com.example.manager.model.CupMatch cupMatch) {
		List<SimulationEvent> events = new ArrayList<>();

		Long homeTeamId = cupMatch.getHomeTeamId();
		Long awayTeamId = cupMatch.getAwayTeamId();

		Team homeTeam = teamRepository.findById(homeTeamId).orElse(null);
		Team awayTeam = teamRepository.findById(awayTeamId).orElse(null);

		if (homeTeam == null || awayTeam == null) {
			return events;
		}

		// Berechne Team-Stärken
		int homeStrength = calculateTeamStrength(homeTeamId);
		int awayStrength = calculateTeamStrength(awayTeamId);

		// Generiere Tore
		int homeGoals = generateGoals(homeStrength, awayStrength);
		int awayGoals = generateGoals(awayStrength, homeStrength);

		// Bei Unentschieden: Verlängerung simulieren (wie in CupService)
		if (homeGoals == awayGoals) {
			int homeExtraGoals = generateGoals(homeStrength, awayStrength);
			int awayExtraGoals = generateGoals(awayStrength, homeStrength);
			homeGoals += homeExtraGoals;
			awayGoals += awayExtraGoals;

			// Falls immer noch Unentschieden: Elfmeterschießen (50/50)
			if (homeGoals == awayGoals) {
				if (random.nextBoolean()) {
					homeGoals++;
				} else {
					awayGoals++;
				}
			}
		}

		List<Player> homePlayers = getLineupPlayers(homeTeamId, "4-4-2");
		List<Player> awayPlayers = getLineupPlayers(awayTeamId, "4-4-2");

		// Generiere Tor-Events
		for (int i = 0; i < homeGoals; i++) {
			int minute = 1 + random.nextInt(89);
			Player scorer = selectGoalScorer(homePlayers);
			if (scorer != null) {
				Player assist = null;
				if (random.nextDouble() < 0.6) {
					List<Player> possibleAssisters = new ArrayList<>();
					for (Player p : homePlayers) {
						if (!p.getId().equals(scorer.getId()) && !"GK".equals(p.getPosition())) {
							possibleAssisters.add(p);
						}
					}
					if (!possibleAssisters.isEmpty()) {
						assist = possibleAssisters.get(random.nextInt(possibleAssisters.size()));
					}
				}

				String goalText;
				if (assist != null) {
					goalText = "⚽ TOR! " + scorer.getName() + " trifft für " + homeTeam.getName() + "! Vorlage von " + assist.getName() + "!";
				} else {
					goalText = "⚽ TOR! " + scorer.getName() + " erzielt ein Tor für " + homeTeam.getName() + "!";
				}

				SimulationEvent goalEvent = new SimulationEvent("goal", minute, homeTeam.getName(), scorer.getName(), goalText, true);
				if (assist != null) {
					goalEvent.assistPlayerName = assist.getName();
				}
				events.add(goalEvent);
			}
		}

		for (int i = 0; i < awayGoals; i++) {
			int minute = 1 + random.nextInt(89);
			Player scorer = selectGoalScorer(awayPlayers);
			if (scorer != null) {
				Player assist = null;
				if (random.nextDouble() < 0.6) {
					List<Player> possibleAssisters = new ArrayList<>();
					for (Player p : awayPlayers) {
						if (!p.getId().equals(scorer.getId()) && !"GK".equals(p.getPosition())) {
							possibleAssisters.add(p);
						}
					}
					if (!possibleAssisters.isEmpty()) {
						assist = possibleAssisters.get(random.nextInt(possibleAssisters.size()));
					}
				}

				String goalText;
				if (assist != null) {
					goalText = "⚽ TOR! " + scorer.getName() + " trifft für " + awayTeam.getName() + "! Vorlage von " + assist.getName() + "!";
				} else {
					goalText = "⚽ TOR! " + scorer.getName() + " erzielt ein Tor für " + awayTeam.getName() + "!";
				}

				SimulationEvent goalEvent = new SimulationEvent("goal", minute, awayTeam.getName(), scorer.getName(), goalText, false);
				if (assist != null) {
					goalEvent.assistPlayerName = assist.getName();
				}
				events.add(goalEvent);
			}
		}

		// Generiere Chancen und weitere Events (ähnlich wie Liga)
		int homeChances = 8 + random.nextInt(8);
		int awayChances = 8 + random.nextInt(8);

		for (int i = 0; i < homeChances; i++) {
			int minute = 1 + random.nextInt(89);
			Player player = homePlayers.isEmpty() ? null : homePlayers.get(random.nextInt(homePlayers.size()));
			if (player != null) {
				String[] chanceTexts = {
					player.getName() + " hat eine Chance, aber knapp daneben!",
					player.getName() + " schießt, der Keeper pariert!",
					"Tolle Kombination! " + player.getName() + " schießt, knapp vorbei!",
					player.getName() + " mit einem Fernschuss!",
					"Kopfballchance für " + player.getName() + "!"
				};
				events.add(new SimulationEvent("chance", minute, homeTeam.getName(), player.getName(),
						chanceTexts[random.nextInt(chanceTexts.length)], true));
			}
		}

		for (int i = 0; i < awayChances; i++) {
			int minute = 1 + random.nextInt(89);
			Player player = awayPlayers.isEmpty() ? null : awayPlayers.get(random.nextInt(awayPlayers.size()));
			if (player != null) {
				String[] chanceTexts = {
					player.getName() + " hat eine Chance, aber knapp daneben!",
					player.getName() + " schießt, der Keeper pariert!",
					"Tolle Kombination! " + player.getName() + " schießt, knapp vorbei!",
					player.getName() + " mit einem Fernschuss!",
					"Kopfballchance für " + player.getName() + "!"
				};
				events.add(new SimulationEvent("chance", minute, awayTeam.getName(), player.getName(),
						chanceTexts[random.nextInt(chanceTexts.length)], false));
			}
		}

		// Gelbe/Rote Karten
		int yellowCards = random.nextInt(3);
		for (int i = 0; i < yellowCards; i++) {
			int minute = 1 + random.nextInt(89);
			boolean isHome = random.nextBoolean();
			List<Player> players = isHome ? homePlayers : awayPlayers;
			String teamName = isHome ? homeTeam.getName() : awayTeam.getName();

			if (!players.isEmpty()) {
				Player player = players.get(random.nextInt(players.size()));
				events.add(new SimulationEvent("yellow_card", minute, teamName, player.getName(),
						"Gelbe Karte für " + player.getName() + "! 🟨", isHome));
			}
		}

		events.sort(Comparator.comparingInt(e -> e.minute));
		return events;
	}

	/**
	 * Sendet Cup Match-Start Event
	 */
	private void sendCupMatchStartEvent(com.example.manager.model.CupMatch cupMatch) {
		Team homeTeam = teamRepository.findById(cupMatch.getHomeTeamId()).orElse(null);
		Team awayTeam = teamRepository.findById(cupMatch.getAwayTeamId()).orElse(null);

		if (homeTeam != null && awayTeam != null) {
			String startMessage = "⚽ Pokalspiel Anpfiff: " + homeTeam.getName() + " vs " + awayTeam.getName();

			LiveMatchEventDTO event = new LiveMatchEventDTO(null, "match_start", 0, "", "", startMessage, 0, 0);
			messagingTemplate.convertAndSend("/topic/live-cup-match/" + cupMatch.getId(), event);
			messagingTemplate.convertAndSend("/topic/live-cup-match/all", event);

			System.out.println("[LiveCupSimulation] ⚽ " + startMessage);
		}
	}

	/**
	 * Sendet Cup Halbzeit Event
	 */
	private void sendCupHalftimeEvent(com.example.manager.model.CupMatch cupMatch) {
		LiveMatchEventDTO event = new LiveMatchEventDTO(null, "halftime", 45, "", "", "⏸ Halbzeit", null, null);
		messagingTemplate.convertAndSend("/topic/live-cup-match/" + cupMatch.getId(), event);
		messagingTemplate.convertAndSend("/topic/live-cup-match/all", event);
	}

	/**
	 * Sendet Live Cup-Event
	 */
	private void sendLiveCupEvent(com.example.manager.model.CupMatch cupMatch, SimulationEvent event) {
		LiveMatchEventDTO liveEvent = new LiveMatchEventDTO(null, event.type, event.minute, event.teamName,
				event.playerName, event.description, null, null);

		messagingTemplate.convertAndSend("/topic/live-cup-match/" + cupMatch.getId(), liveEvent);
		messagingTemplate.convertAndSend("/topic/live-cup-match/all", liveEvent);
	}

	/**
	 * Beendet die Cup-Simulation und speichert Ergebnisse
	 */
	@Transactional
	private void finishCupSimulation(List<com.example.manager.model.CupMatch> cupMatches, 
			Map<com.example.manager.model.CupMatch, List<SimulationEvent>> eventTimelines) {
		System.out.println("[LiveCupSimulation] ✅ Cup-Simulation beendet, speichere Ergebnisse...");

		for (com.example.manager.model.CupMatch cupMatch : cupMatches) {
			// Zähle Tore aus Timeline
			List<SimulationEvent> events = eventTimelines.get(cupMatch);
			int homeGoals = 0;
			int awayGoals = 0;

			for (SimulationEvent event : events) {
				if ("goal".equals(event.type)) {
					if (event.isHomeTeam) {
						homeGoals++;
					} else {
						awayGoals++;
					}
				}
			}

			// Speichere Ergebnis
			cupMatch.setHomeGoals(homeGoals);
			cupMatch.setAwayGoals(awayGoals);
			cupMatch.setStatus("completed");
			cupMatchRepository.save(cupMatch);

			// Sende Match-End Event
			Team homeTeam = teamRepository.findById(cupMatch.getHomeTeamId()).orElse(null);
			Team awayTeam = teamRepository.findById(cupMatch.getAwayTeamId()).orElse(null);

			if (homeTeam != null && awayTeam != null) {
				String endMessage = "🏆 Pokalspiel Abpfiff: " + homeTeam.getName() + " " + homeGoals + " : " 
					+ awayGoals + " " + awayTeam.getName();
				LiveMatchEventDTO endEvent = new LiveMatchEventDTO(null, "match_end", 90, "", "", endMessage,
						homeGoals, awayGoals);

				messagingTemplate.convertAndSend("/topic/live-cup-match/" + cupMatch.getId(), endEvent);
				messagingTemplate.convertAndSend("/topic/live-cup-match/all", endEvent);

				System.out.println("[LiveCupSimulation] " + endMessage);

				// Regeneriere Fitness
				regeneratePlayerFitness(cupMatch.getHomeTeamId());
				regeneratePlayerFitness(cupMatch.getAwayTeamId());
			}
		}

		// Nach allen Matches: Vervollständige Cup-Runde
		try {
			int currentSeason = repositoryService.getCurrentSeason();
			List<com.example.manager.model.CupTournament> tournaments = 
				cupTournamentRepository.findAll().stream()
					.filter(t -> t.getSeason() == currentSeason)
					.collect(java.util.stream.Collectors.toList());

			for (com.example.manager.model.CupTournament tournament : tournaments) {
				cupService.completeCupRound(tournament, tournament.getCurrentRound());
			}
		} catch (Exception e) {
			System.err.println("[LiveCupSimulation] Fehler beim Vervollständigen der Cup-Runde: " + e.getMessage());
		}

		simulationRunning = false;

		if (executor != null) {
			executor.shutdown();
		}

		System.out.println("[LiveCupSimulation] 🏆 Pokalspiele abgeschlossen!");
	}
}
