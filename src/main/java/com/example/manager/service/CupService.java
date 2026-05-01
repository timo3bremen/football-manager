package com.example.manager.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.manager.model.CupMatch;
import com.example.manager.model.CupTeam;
import com.example.manager.model.CupTournament;
import com.example.manager.model.League;
import com.example.manager.model.LeagueSlot;
import com.example.manager.model.LineupSlot;
import com.example.manager.model.Match;
import com.example.manager.model.Player;
import com.example.manager.model.Sponsor;
import com.example.manager.model.Team;
import com.example.manager.model.Transaction;
import com.example.manager.repository.CupMatchRepository;
import com.example.manager.repository.CupTeamRepository;
import com.example.manager.repository.CupTournamentRepository;
import com.example.manager.repository.LeagueRepository;
import com.example.manager.repository.LineupRepository;
import com.example.manager.repository.MatchRepository;
import com.example.manager.repository.PlayerRepository;
import com.example.manager.repository.ScheduleRepository;
import com.example.manager.repository.SponsorRepository;
import com.example.manager.repository.TeamRepository;
import com.example.manager.repository.TransactionRepository;

/**
 * Service for managing cup tournaments. 
 * - 64 teams per country per season 
 * - 6 rounds (KO-Modus)
 * 
 * CUP QUALIFICATION RULES:
 * - SAISON 1: All teams from 1st & 2nd league (36 teams) + randomly selected 28 teams from 3rd league
 *             → 20 teams from 3rd league are randomly excluded
 * - AB SAISON 2: All teams from 1st & 2nd league + teams from positions 1-7 in all four 3rd leagues
 *                → Teams on positions 8-12 in 3rd leagues are excluded (5 teams × 4 leagues = 20 teams)
 */
@Service
public class CupService {

	@Autowired
	private CupTournamentRepository cupTournamentRepository;

	@Autowired
	private CupTeamRepository cupTeamRepository;

	@Autowired
	private CupMatchRepository cupMatchRepository;

	@Autowired
	private TeamRepository teamRepository;

	@Autowired
	private LineupRepository lineupRepository;

	@Autowired
	private PlayerRepository playerRepository;

	@Autowired
	private LeagueRepository leagueRepository;

	@Autowired
	private TransactionRepository transactionRepository;

	@Autowired
	private SponsorRepository sponsorRepository;

	@Autowired
	private MatchRepository matchRepository;

	@Autowired
	private ScheduleRepository scheduleRepository;

	private final Random random = new Random();
	private static final int CUP_TEAMS = 64;
	private static final int CUP_ROUNDS = 6;
	private static final int EXCLUDED_DIV3_TEAMS = 20;

	/**
	 * Initialize cup tournaments for ALL countries in the system. Called at startup
	 * to ensure all cup tournaments exist. Verwendet nur die Länder, die in den Ligen
	 * existieren!
	 */
	@Transactional
	public void initializeAllCupTournaments(int season) {
		System.out.println("[CupService] Initializing cup tournaments for all countries, season " + season);

		// SCHRITT 1: Lösche ALLE alten Cup-Turniere für die alte Saison
		List<CupTournament> allOldTournaments = cupTournamentRepository.findAll().stream()
				.filter(t -> t.getSeason() != season).collect(Collectors.toList());
		if (!allOldTournaments.isEmpty()) {
			System.out.println("[CupService] Lösche " + allOldTournaments.size() + " alte Cup-Turniere");
			// Lösche alle Related Matches und Teams
			for (CupTournament tournament : allOldTournaments) {
				cupMatchRepository.deleteByTournamentId(tournament.getId());
				cupTeamRepository.deleteByTournamentId(tournament.getId());
				cupTournamentRepository.delete(tournament);
			}
		}

		// SCHRITT 2: Sammle alle eindeutigen Länder aus den Ligen
		List<League> allLeagues = leagueRepository.findAll();
		Set<String> countries = new java.util.HashSet<>();

		for (League league : allLeagues) {
			if (league.getCountry() != null && !league.getCountry().isEmpty()) {
				countries.add(league.getCountry());
			}
		}

		System.out.println("[CupService] Found " + countries.size() + " countries in leagues: " + countries);

		// SCHRITT 3: Initialisiere Cup-Turniere nur für die existierenden Länder
		for (String country : countries) {
			try {
				initializeCupTournament(country, season);
			} catch (Exception e) {
				System.err.println("[CupService] Error initializing cup for " + country + ": " + e.getMessage());
				e.printStackTrace();
			}
		}

		System.out.println("[CupService] ✅ Cup tournaments initialized for " + countries.size() + " countries");
	}

	/**
	 * Initialisiert Cup-Turnier für ein spezifisches Land. Sammelt alle Teams aus
	 * den Ligen dieses Landes und startet mit 64 Teams (oder weniger wenn nicht
	 * vorhanden).
	 * 
	 * SAISON 1: 20 zufällig ausgeschlossene Teams kommen alle aus der 3. Liga
	 * AB SAISON 2: Teams auf Plätzen 8-12 in allen 4 3. Ligen werden ausgeschlossen
	 */
	@Transactional
	public void initializeCupTournament(String country, int season) {
		System.out.println("[CupService] Initializing cup tournament for " + country + " season " + season);

		// Prüfe ob schon existiert
		Optional<CupTournament> existing = cupTournamentRepository.findByCountryAndSeason(country, season);
		if (existing.isPresent()) {
			System.out.println("[CupService] Cup tournament already exists for " + country + " season " + season);
			return;
		}

		// Erstelle neues Turnier
		CupTournament tournament = new CupTournament(country, season);
		cupTournamentRepository.save(tournament);

		// Sammle alle Ligen für dieses Land
		List<League> countryLeagues = leagueRepository.findAll().stream()
				.filter(l -> country.equals(l.getCountry())).collect(Collectors.toList());

		if (countryLeagues.isEmpty()) {
			System.out.println("[CupService] No leagues found for country " + country);
			return;
		}

		// Sammle alle Teams aus den Ligen 1 und 2
		List<Team> division1And2Teams = new ArrayList<>();
		// Sammle alle Teams aus Liga 3
		List<Team> division3Teams = new ArrayList<>();
		
		for (League league : countryLeagues) {
			for (LeagueSlot slot : league.getSlots()) {
				if (slot.getTeamId() != null) {
					Team team = teamRepository.findById(slot.getTeamId()).orElse(null);
					if (team != null) {
						if (league.getDivision() == 3) {
							division3Teams.add(team);
						} else if (league.getDivision() == 1 || league.getDivision() == 2) {
							division1And2Teams.add(team);
						}
					}
				}
			}
		}

		System.out.println("[CupService] Found " + division1And2Teams.size() + " teams in divisions 1-2 and " 
				+ division3Teams.size() + " teams in division 3 for country " + country);

		// Wähle Teams basierend auf Saison
		List<Team> cupTeams = new ArrayList<>();
		
		if (season == 1) {
			// SAISON 1: Alle Teams aus Liga 1 und 2, dann zufällig 44 aus Liga 3
			// (Total: 12+24+44 = 80 Teams, aber wir wollen 64, also 20 aus Liga 3 ausschließen)
			// 12 Teams Liga 1 + 24 Teams Liga 2 = 36 Teams garantiert
			// 48 Teams aus Liga 3 sollen teilnehmen (64 - 36 = 28? Nein, 48 - 20 = 28? Nein...)
			// Total Teams: 12 (Liga 1) + 24 (Liga 2) + 48 (Liga 3) = 84 Teams
			// Cup soll 64 Teams haben, also 20 ausschließen
			// 12 + 24 = 36 Teams aus Liga 1+2 (alle dabei)
			// 48 Teams aus Liga 3, davon 44 teilnehmen lassen → 4 ausschließen? Nein!
			// Korrektur: 64 - 36 = 28 Teams aus Liga 3 sollen dabei sein
			// Also 48 - 28 = 20 Teams aus Liga 3 ausschließen ✓
			
			cupTeams.addAll(division1And2Teams); // Alle Teams aus Liga 1+2
			
			// Shuffle Liga 3 Teams und nimm nur 28 (schließe 20 aus)
			Collections.shuffle(division3Teams);
			int division3Participants = Math.min(28, division3Teams.size());
			cupTeams.addAll(division3Teams.subList(0, division3Participants));
			
			System.out.println("[CupService] Season 1: Including all " + division1And2Teams.size() 
					+ " teams from divisions 1-2 and " + division3Participants + " random teams from division 3 (excluding " 
					+ (division3Teams.size() - division3Participants) + " teams from division 3)");
			
		} else {
			// AB SAISON 2: Schließe Teams auf Plätzen 8-12 in allen 4 3. Ligen aus
			// Teams aus Liga 1+2: Alle teilnehmen
			cupTeams.addAll(division1And2Teams);
			
			// Teams aus Liga 3: Nur Plätze 1-7 aus jeder Liga
			List<Long> excludedTeamIds = new ArrayList<>();
			
			for (League league : countryLeagues) {
				if (league.getDivision() == 3) {
					// Hole Tabelle dieser 3. Liga
					List<com.example.manager.dto.LeagueStandingsDTO> standings = getLeagueStandingsForCup(league.getId());
					
					// Schließe Teams auf Plätzen 8-12 aus (5 Teams pro Liga × 4 Ligen = 20 Teams)
					for (com.example.manager.dto.LeagueStandingsDTO standing : standings) {
						if (standing.getPosition() >= 8 && standing.getPosition() <= 12) {
							excludedTeamIds.add(standing.getTeamId());
						}
					}
				}
			}
			
			// Füge nur Teams aus Liga 3 hinzu, die nicht ausgeschlossen sind
			for (Team team : division3Teams) {
				if (!excludedTeamIds.contains(team.getId())) {
					cupTeams.add(team);
				}
			}
			
			System.out.println("[CupService] Season " + season + ": Including all " + division1And2Teams.size() 
					+ " teams from divisions 1-2 and " + (division3Teams.size() - excludedTeamIds.size()) 
					+ " teams from division 3 (excluding " + excludedTeamIds.size() + " teams from positions 8-12)");
		}

		// Limitiere auf maximal 64 Teams
		if (cupTeams.size() > CUP_TEAMS) {
			Collections.shuffle(cupTeams);
			cupTeams = cupTeams.stream().limit(CUP_TEAMS).collect(Collectors.toList());
		}

		// Erstelle CupTeam Einträge
		for (Team team : cupTeams) {
			CupTeam cupTeam = new CupTeam(tournament, team.getId(), team.getName());
			cupTeamRepository.save(cupTeam);
		}

		System.out.println(
				"[CupService] Cup tournament initialized with " + cupTeams.size() + " teams for " + country);

		// WICHTIG: Nicht sofort Runde 1 generieren!
		// Die Runde wird bei der Cup-Simulation generiert (bei Matchday % 3 == 0)
		// Das Tournament startet mit currentRound = 0
		// Dann wird bei Matchday 3 Runde 1 generiert und simuliert
	}
	
	/**
	 * Helper method to get league standings for cup qualification.
	 * Returns standings sorted by position.
	 */
	private List<com.example.manager.dto.LeagueStandingsDTO> getLeagueStandingsForCup(Long leagueId) {
		League league = leagueRepository.findById(leagueId).orElse(null);
		if (league == null) {
			return new ArrayList<>();
		}

		// Map to store team stats
		Map<Long, Map<String, Integer>> teamStats = new HashMap<>();

		// Initialize stats for all teams in league
		for (LeagueSlot slot : league.getSlots()) {
			if (slot.getTeamId() != null) {
				Map<String, Integer> stats = new HashMap<>();
				stats.put("played", 0);
				stats.put("won", 0);
				stats.put("drawn", 0);
				stats.put("lost", 0);
				stats.put("goalsFor", 0);
				stats.put("goalsAgainst", 0);
				stats.put("points", 0);
				teamStats.put(slot.getTeamId(), stats);
			}
		}

		// Get all matches from this league's schedule
		List<com.example.manager.model.Schedule> allSchedules = scheduleRepository.findAll();
		for (com.example.manager.model.Schedule s : allSchedules) {
			if (s.getLeagueId() != null && s.getLeagueId().equals(leagueId)) {
				for (com.example.manager.model.Matchday matchday : s.getMatchdays()) {
					for (Match match : matchday.getMatches()) {
						if ("played".equals(match.getStatus()) && match.getHomeGoals() != null
								&& match.getAwayGoals() != null) {
							Long homeTeamId = match.getHomeTeamId();
							Long awayTeamId = match.getAwayTeamId();

							if (homeTeamId != null && awayTeamId != null && teamStats.containsKey(homeTeamId)
									&& teamStats.containsKey(awayTeamId)) {

								int homeGoals = match.getHomeGoals();
								int awayGoals = match.getAwayGoals();

								Map<String, Integer> homeStats = teamStats.get(homeTeamId);
								Map<String, Integer> awayStats = teamStats.get(awayTeamId);

								homeStats.put("played", homeStats.get("played") + 1);
								awayStats.put("played", awayStats.get("played") + 1);

								homeStats.put("goalsFor", homeStats.get("goalsFor") + homeGoals);
								homeStats.put("goalsAgainst", homeStats.get("goalsAgainst") + awayGoals);
								awayStats.put("goalsFor", awayStats.get("goalsFor") + awayGoals);
								awayStats.put("goalsAgainst", awayStats.get("goalsAgainst") + homeGoals);

								if (homeGoals > awayGoals) {
									homeStats.put("won", homeStats.get("won") + 1);
									homeStats.put("points", homeStats.get("points") + 3);
									awayStats.put("lost", awayStats.get("lost") + 1);
								} else if (awayGoals > homeGoals) {
									awayStats.put("won", awayStats.get("won") + 1);
									awayStats.put("points", awayStats.get("points") + 3);
									homeStats.put("lost", homeStats.get("lost") + 1);
								} else {
									homeStats.put("drawn", homeStats.get("drawn") + 1);
									homeStats.put("points", homeStats.get("points") + 1);
									awayStats.put("drawn", awayStats.get("drawn") + 1);
									awayStats.put("points", awayStats.get("points") + 1);
								}
							}
						}
					}
				}
				break;
			}
		}

		// Create standings list and sort by points
		List<Map.Entry<Long, Map<String, Integer>>> sortedTeams = new ArrayList<>(teamStats.entrySet());
		sortedTeams.sort((a, b) -> {
			int pointsA = a.getValue().get("points");
			int pointsB = b.getValue().get("points");
			if (pointsA != pointsB) {
				return Integer.compare(pointsB, pointsA);
			}
			int diffA = a.getValue().get("goalsFor") - a.getValue().get("goalsAgainst");
			int diffB = b.getValue().get("goalsFor") - b.getValue().get("goalsAgainst");
			return Integer.compare(diffB, diffA);
		});

		// Build standings list
		List<com.example.manager.dto.LeagueStandingsDTO> standings = new ArrayList<>();
		int position = 1;
		for (Map.Entry<Long, Map<String, Integer>> entry : sortedTeams) {
			Long teamId = entry.getKey();
			Map<String, Integer> stats = entry.getValue();
			Team team = teamRepository.findById(teamId).orElse(null);

			if (team != null) {
				int strength = calculateTeamStrength(teamId);
				com.example.manager.dto.LeagueStandingsDTO dto = new com.example.manager.dto.LeagueStandingsDTO(
						teamId, team.getName(), position, stats.get("played"),
						stats.get("won"), stats.get("drawn"), stats.get("lost"), stats.get("goalsFor"),
						stats.get("goalsAgainst"), stats.get("points"), strength);
				standings.add(dto);
				position++;
			}
		}

		return standings;
	}

	/**
	 * Generates matches for a cup round.
	 */
	@Transactional
	public void generateCupRound(CupTournament tournament, int round) {
		System.out.println("[CupService] Generating cup round " + round + " for tournament " + tournament.getId());

		// Hole aktive Teams
		List<CupTeam> activeTeams = cupTeamRepository.findByTournamentIdAndIsActive(tournament.getId(), true);

		if (activeTeams.isEmpty()) {
			System.out.println("[CupService] No active teams for round " + round);
			return;
		}

		// Shuffle und paare auf
		Collections.shuffle(activeTeams);

		for (int i = 0; i < activeTeams.size(); i += 2) {
			if (i + 1 < activeTeams.size()) {
				CupTeam home = activeTeams.get(i);
				CupTeam away = activeTeams.get(i + 1);

				CupMatch match = new CupMatch(tournament, round, home.getTeamId(), home.getTeamName(), away.getTeamId(),
						away.getTeamName());
				cupMatchRepository.save(match);
			}
		}

		tournament.setCurrentRound(round);
		cupTournamentRepository.save(tournament);

		System.out.println("[CupService] Generated " + (activeTeams.size() / 2) + " matches for round " + round);
	}

	/**
	 * Complete a cup round - simulate matches and eliminate losers. Uses the same
	 * strength-based simulation logic as league matches. No draws allowed - extra
	 * time and penalties if needed.
	 */
	@Transactional
	public void completeCupRound(CupTournament tournament, int round) {
		System.out.println("[CupService] Completing cup round " + round);

		List<CupMatch> roundMatches = cupMatchRepository.findByTournamentIdAndRound(tournament.getId(), round);

		int completedMatches = 0;
		for (CupMatch match : roundMatches) {
			if ("completed".equals(match.getStatus())) {
				continue; // Bereits gespielt
			}

			// Berechne Teamstärke für beide Teams
			int homeStrength = calculateTeamStrength(match.getHomeTeamId());
			int awayStrength = calculateTeamStrength(match.getAwayTeamId());

			System.out.println("[CupService] Cup Match: " + match.getHomeTeamName() + " (" + homeStrength + ") vs "
					+ match.getAwayTeamName() + " (" + awayStrength + ")");

			// Simuliere Spiel mit Verlängerung und Elfmeterschießen wenn nötig
			int homeGoals = generateGoals(homeStrength, awayStrength);
			int awayGoals = generateGoals(awayStrength, homeStrength);

			String resultNote = ""; // Für n.V. oder n.E.

			// Falls Unentschieden - Verlängerung simulieren
			if (homeGoals == awayGoals) {
				System.out.println("[CupService] Draw after regular time (" + homeGoals + ":" + awayGoals
						+ "), simulating extra time...");

				int homeExtraGoals = generateGoals(homeStrength, awayStrength);
				int awayExtraGoals = generateGoals(awayStrength, homeStrength);

				homeGoals += homeExtraGoals;
				awayGoals += awayExtraGoals;

				// Falls immer noch Unentschieden - Elfmeterschießen
				if (homeGoals == awayGoals) {
					System.out.println("[CupService] Still draw after extra time (" + homeGoals + ":" + awayGoals
							+ "), going to penalties...");

					// Elfmeterschießen: 50/50 Chance
					if (random.nextBoolean()) {
						homeGoals++; // Home Team gewinnt im Elfmeterschießen
						resultNote = " n.E.";
						System.out.println("[CupService] Penalties: " + match.getHomeTeamName() + " wins");
					} else {
						awayGoals++; // Away Team gewinnt im Elfmeterschießen
						resultNote = " n.E.";
						System.out.println("[CupService] Penalties: " + match.getAwayTeamName() + " wins");
					}
				} else {
					resultNote = " n.V.";
					System.out.println("[CupService] Extra time winner determined");
				}
			}

			match.setHomeGoals(homeGoals);
			match.setAwayGoals(awayGoals);
			match.setResultNote(resultNote);
			match.setStatus("completed");

			Long winnerId;
			String winnerName;

			if (homeGoals > awayGoals) {
				winnerId = match.getHomeTeamId();
				winnerName = match.getHomeTeamName();
			} else {
				winnerId = match.getAwayTeamId();
				winnerName = match.getAwayTeamName();
			}

			match.setWinnerId(winnerId);
			match.setWinnerName(winnerName);
			cupMatchRepository.save(match);

			System.out.println("[CupService] Result: " + match.getHomeTeamName() + " " + homeGoals + ":" + awayGoals
					+ " " + match.getAwayTeamName() + resultNote + " → Winner: " + winnerName);

			// === FINANZIELLE TRANSAKTIONEN FÜR POKALSPIELE ===
			
			// 1. Sponsorprämien (beide Teams)
			boolean homeWonMatch = homeGoals > awayGoals;
			processCupSponsorPayouts(match.getHomeTeamId(), homeWonMatch, match);
			processCupSponsorPayouts(match.getAwayTeamId(), !homeWonMatch, match);
			
			// 2. Spielergehälter (beide Teams)
			deductCupPlayerSalaries(match.getHomeTeamId(), match);
			deductCupPlayerSalaries(match.getAwayTeamId(), match);
			
			// 3. Zuschauereinnahmen (nur Heimteam)
			processCupAttendanceRevenue(match, match.getHomeTeamId(), homeGoals, awayGoals, homeStrength, awayStrength);
			
			// 4. Pokalprämien (beide Teams)
			processCupCompetitionBonus(match.getHomeTeamId(), round);
			processCupCompetitionBonus(match.getAwayTeamId(), round);
			
			// 5. Extra-Prämie für Pokalsieger (nur im Finale, nur für Gewinner)
			if (round == CUP_ROUNDS) {
				processCupWinnerBonus(winnerId);
			}

			// Markiere Verlierer als eliminiert
			Long loserId = homeGoals > awayGoals ? match.getAwayTeamId() : match.getHomeTeamId();
			List<CupTeam> loserTeams = cupTeamRepository.findByTournamentId(tournament.getId()).stream()
					.filter(t -> t.getTeamId().equals(loserId)).collect(Collectors.toList());

			for (CupTeam loserTeam : loserTeams) {
				loserTeam.setActive(false);
				loserTeam.setEliminatedInRound(round);
				cupTeamRepository.save(loserTeam);
			}

			completedMatches++;
		}

		// Prüfe ob Finale (nur 1 Team übrig)
		List<CupTeam> activeTeams = cupTeamRepository.findByTournamentIdAndIsActive(tournament.getId(), true);

		if (activeTeams.size() == 1 && round == CUP_ROUNDS) {
			CupTeam winner = activeTeams.get(0);
			tournament.setWinnerTeamId(winner.getTeamId());
			tournament.setWinnerTeamName(winner.getTeamName());
			tournament.setStatus("completed");
			cupTournamentRepository.save(tournament);
			System.out.println("[CupService] ✅ Cup tournament completed! Winner: " + winner.getTeamName());
		} else if (activeTeams.size() > 1 && round < CUP_ROUNDS) {
			// Generiere nächste Runde
			generateCupRound(tournament, round + 1);
		}

		System.out.println("[CupService] Completed " + completedMatches + " matches in round " + round);
	}

	/**
	 * Calculate team strength based on lineup rating.
	 */
	private int calculateTeamStrength(Long teamId) {
		List<LineupSlot> lineup = lineupRepository.findByTeamIdAndFormationId(teamId, "4-4-2"); // Default formation
		int strength = 0;
		for (LineupSlot slot : lineup) {
			if (slot.getPlayerId() != null) {
				Player p = playerRepository.findById(slot.getPlayerId()).orElse(null);
				if (p != null) {
					strength += p.getRating();
				}
			}
		}
		return strength;
	}

	/**
	 * Generate goals based on team strength difference. Same logic as in
	 * RepositoryService.
	 */
	private int generateGoals(int teamStrength, int opponentStrength) {
		int strengthDiff = teamStrength - opponentStrength;
		double rand = random.nextDouble();

		// Wenn Team schwächer ist (negative differenz), dann mit umgekehrter
		// Wahrscheinlichkeit
		if (strengthDiff < 0) {
			return generateGoalsForWeakerTeam(teamStrength, opponentStrength);
		}

		// Je größer die Differenz, desto höher die Wahrscheinlichkeit zu gewinnen

		// Sehr große Differenz: 200+
		if (strengthDiff > 200) {
			if (rand < 0.01)
				return 0;
			if (rand < 0.05)
				return 1;
			if (rand < 0.20)
				return 2;
			if (rand < 0.45)
				return 3;
			if (rand < 0.70)
				return 4;
			if (rand < 0.88)
				return 5;
			if (rand < 0.96)
				return 6;
			return 7;
		}

		// Große Differenz: 150-200
		if (strengthDiff > 150) {
			if (rand < 0.03)
				return 0;
			if (rand < 0.10)
				return 1;
			if (rand < 0.30)
				return 2;
			if (rand < 0.55)
				return 3;
			if (rand < 0.75)
				return 4;
			if (rand < 0.88)
				return 5;
			if (rand < 0.95)
				return 6;
			return 7;
		}

		// Große Differenz: 100-150
		if (strengthDiff > 100) {
			if (rand < 0.05)
				return 0;
			if (rand < 0.18)
				return 1;
			if (rand < 0.40)
				return 2;
			if (rand < 0.62)
				return 3;
			if (rand < 0.78)
				return 4;
			if (rand < 0.90)
				return 5;
			if (rand < 0.97)
				return 6;
			return 7;
		}

		// Mittlere Differenz: 50-100
		if (strengthDiff > 50) {
			if (rand < 0.08)
				return 0;
			if (rand < 0.25)
				return 1;
			if (rand < 0.50)
				return 2;
			if (rand < 0.70)
				return 3;
			if (rand < 0.85)
				return 4;
			if (rand < 0.93)
				return 5;
			return 6;
		}

		// Kleine Differenz: 20-50
		if (strengthDiff > 20) {
			if (rand < 0.10)
				return 0;
			if (rand < 0.30)
				return 1;
			if (rand < 0.55)
				return 2;
			if (rand < 0.73)
				return 3;
			if (rand < 0.87)
				return 4;
			if (rand < 0.95)
				return 5;
			return 6;
		}

		// Sehr kleine Differenz: 0-20 (ausgeglichen)
		if (rand < 0.12)
			return 0;
		if (rand < 0.35)
			return 1;
		if (rand < 0.58)
			return 2;
		if (rand < 0.75)
			return 3;
		if (rand < 0.88)
			return 4;
		if (rand < 0.96)
			return 5;
		return 6;
	}

	/**
	 * Generate goals for weaker team.
	 */
	private int generateGoalsForWeakerTeam(int teamStrength, int opponentStrength) {
		int strengthDiff = opponentStrength - teamStrength;
		double rand = random.nextDouble();

		// Sehr große Differenz: 200+
		if (strengthDiff > 200) {
			if (rand < 0.70)
				return 0;
			if (rand < 0.85)
				return 1;
			if (rand < 0.95)
				return 2;
			return 3;
		}

		// Große Differenz: 150-200
		if (strengthDiff > 150) {
			if (rand < 0.60)
				return 0;
			if (rand < 0.80)
				return 1;
			if (rand < 0.93)
				return 2;
			return 3;
		}

		// Mittlere Differenz: 100-150
		if (strengthDiff > 100) {
			if (rand < 0.50)
				return 0;
			if (rand < 0.75)
				return 1;
			if (rand < 0.90)
				return 2;
			return 3;
		}

		// Kleinere Differenz: 50-100
		if (strengthDiff > 50) {
			if (rand < 0.40)
				return 0;
			if (rand < 0.70)
				return 1;
			if (rand < 0.87)
				return 2;
			if (rand < 0.96)
				return 3;
			return 4;
		}

		// Kleine Differenz: 20-50
		if (strengthDiff > 20) {
			if (rand < 0.35)
				return 0;
			if (rand < 0.65)
				return 1;
			if (rand < 0.85)
				return 2;
			if (rand < 0.95)
				return 3;
			return 4;
		}

		// Ausgeglichen: 0-20
		if (rand < 0.12)
			return 0;
		if (rand < 0.35)
			return 1;
		if (rand < 0.58)
			return 2;
		if (rand < 0.75)
			return 3;
		if (rand < 0.88)
			return 4;
		if (rand < 0.96)
			return 5;
		return 6;
	}

	/**
	 * Get all matches for a round.
	 */
	public List<CupMatch> getRoundMatches(Long tournamentId, int round) {
		return cupMatchRepository.findByTournamentIdAndRound(tournamentId, round);
	}

	/**
	 * Get tournament overview.
	 */
	public Map<String, Object> getTournamentOverview(Long tournamentId) {
		CupTournament tournament = cupTournamentRepository.findById(tournamentId).orElse(null);
		if (tournament == null) {
			return new HashMap<>();
		}

		// Wenn noch keine Runde generiert wurde, generiere Runde 1
		if (tournament.getCurrentRound() == 0) {
			System.out.println("[CupService] Generating round 1 for tournament " + tournamentId);
			generateCupRound(tournament, 1);
			// Neuladen
			tournament = cupTournamentRepository.findById(tournamentId).orElse(null);
		}

		Map<String, Object> overview = new HashMap<>();
		overview.put("id", tournament.getId());
		overview.put("country", tournament.getCountry());
		overview.put("season", tournament.getSeason());
		overview.put("currentRound", tournament.getCurrentRound());
		overview.put("status", tournament.getStatus());
		overview.put("winnerTeamId", tournament.getWinnerTeamId());
		overview.put("winnerTeamName", tournament.getWinnerTeamName());

		// Aktive Teams
		List<CupTeam> activeTeams = cupTeamRepository.findByTournamentIdAndIsActive(tournamentId, true);
		overview.put("remainingTeams", activeTeams.size());

		return overview;
	}

	/**
	 * Verarbeitet Sponsorprämien für Pokalspiele
	 */
	private void processCupSponsorPayouts(Long teamId, boolean won, CupMatch match) {
		try {
			Optional<Sponsor> sponsorOpt = sponsorRepository.findByTeamId(teamId);
			if (!sponsorOpt.isPresent()) return;
			
			Sponsor sponsor = sponsorOpt.get();
			Team team = teamRepository.findById(teamId).orElse(null);
			if (team == null) return;

			long totalPayout = 0;
			
			// Appearance Payout
			if (sponsor.getAppearancePayout() > 0) {
				team.setBudget(team.getBudgetAsLong() + sponsor.getAppearancePayout());
				totalPayout += sponsor.getAppearancePayout();
			}

			// Win Payout
			if (won && sponsor.getWinPayout() > 0) {
				team.setBudget(team.getBudgetAsLong() + sponsor.getWinPayout());
				totalPayout += sponsor.getWinPayout();
			}

			if (totalPayout > 0) {
				teamRepository.save(team);
				
				Transaction transaction = new Transaction(
					teamId,
					totalPayout,
					"income",
					"Pokal Sponsorprämie (" + (won ? "Sieg" : "Teilnahme") + ")",
					"sponsors"
				);
				transactionRepository.save(transaction);
				
				System.out.println("[CupService] Sponsor Payout für Team " + team.getName() + ": " + totalPayout + "€");
			}
		} catch (Exception e) {
			System.err.println("[CupService] Fehler bei Sponsorprämien: " + e.getMessage());
		}
	}

	/**
	 * Zieht Spielergehälter für Pokalspiele ab
	 */
	private void deductCupPlayerSalaries(Long teamId, CupMatch match) {
		try {
			Team team = teamRepository.findById(teamId).orElse(null);
			if (team == null) return;

			List<Player> allPlayers = playerRepository.findByTeamId(teamId);
			long totalSalaries = 0;

			for (Player player : allPlayers) {
				if (player.getSalary() > 0) {
					totalSalaries += player.getSalary();
				}
			}

			if (totalSalaries > 0) {
				team.setBudget(team.getBudgetAsLong() - totalSalaries);
				teamRepository.save(team);

				Transaction transaction = new Transaction(
					teamId,
					-totalSalaries,
					"expense",
					"Spielergehälter (Pokalspiel)",
					"salaries"
				);
				transactionRepository.save(transaction);

				System.out.println("[CupService] Gehälter für Team " + team.getName() + ": " + totalSalaries + "€ abgezogen");
			}
		} catch (Exception e) {
			System.err.println("[CupService] Fehler bei Gehältern: " + e.getMessage());
		}
	}

	/**
	 * Verarbeitet Zuschauereinnahmen für Pokalspiele (nur Heimteam)
	 */
	private void processCupAttendanceRevenue(CupMatch cupMatch, Long homeTeamId, int homeGoals, int awayGoals, 
			int homeStrength, int awayStrength) {
		try {
			Team homeTeam = teamRepository.findById(homeTeamId).orElse(null);
			if (homeTeam == null) return;

			// Basis-Auslastung basierend auf Fan-Zufriedenheit
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

			// Pokalspiel-Bonus: +20% Auslastung
			occupancy = Math.min(1.0, occupancy * 1.2);

			long standingSeats = homeTeam.getStadiumCapacityStanding() != null ? homeTeam.getStadiumCapacityStanding() : 1000L;
			long seatedSeats = homeTeam.getStadiumCapacitySeated() != null ? homeTeam.getStadiumCapacitySeated() : 0L;
			long vipSeats = homeTeam.getStadiumCapacityVip() != null ? homeTeam.getStadiumCapacityVip() : 0L;

			int ticketPriceStanding = homeTeam.getTicketPriceStanding();
			int ticketPriceSeated = homeTeam.getTicketPriceSeated();
			int ticketPriceVip = homeTeam.getTicketPriceVip();

			// Berechne Auslastung
			long standingAttendance = (long) (standingSeats * occupancy);
			long seatedAttendance = (long) (seatedSeats * occupancy);
			long vipAttendance = (long) (vipSeats * occupancy);
			long totalAttendance = standingAttendance + seatedAttendance + vipAttendance;

			// Berechne Einnahmen
			long standingRevenue = standingAttendance * ticketPriceStanding;
			long seatedRevenue = seatedAttendance * ticketPriceSeated;
			long vipRevenue = vipAttendance * ticketPriceVip;
			long totalRevenue = standingRevenue + seatedRevenue + vipRevenue;

			if (totalRevenue > 0) {
				homeTeam.setBudget(homeTeam.getBudgetAsLong() + totalRevenue);
				teamRepository.save(homeTeam);

				Transaction transaction = new Transaction(
					homeTeamId,
					totalRevenue,
					"income",
					"Pokal Zuschauereinnahmen (" + totalAttendance + " Zuschauer)",
					"attendance"
				);
				transactionRepository.save(transaction);

				System.out.println("[CupService] Zuschauereinnahmen für " + homeTeam.getName() + ": " 
					+ totalAttendance + " Zuschauer → " + totalRevenue + "€");
			}
		} catch (Exception e) {
			System.err.println("[CupService] Fehler bei Zuschauereinnahmen: " + e.getMessage());
		}
	}

	/**
	 * Zahlt Pokalprämie für die abgeschlossene Runde (beide Teams erhalten die Prämie)
	 */
	private void processCupCompetitionBonus(Long teamId, int round) {
		try {
			Team team = teamRepository.findById(teamId).orElse(null);
			if (team == null) return;

			// Prämien pro Runde
			long bonus = 0;
			switch (round) {
				case 1: bonus = 800_000; break;    // 1. Runde: 800k
				case 2: bonus = 1_200_000; break;  // 2. Runde: 1.2 Mio
				case 3: bonus = 1_800_000; break;  // 3. Runde: 1.8 Mio
				case 4: bonus = 2_500_000; break;  // 4. Runde: 2.5 Mio
				case 5: bonus = 3_500_000; break;  // 5. Runde: 3.5 Mio
				case 6: bonus = 5_000_000; break;  // Finale: 5 Mio
				default: return;
			}

			if (bonus > 0) {
				team.setBudget(team.getBudgetAsLong() + bonus);
				teamRepository.save(team);

				Transaction transaction = new Transaction(
					teamId,
					bonus,
					"income",
					"Pokalprämie Runde " + round,
					"competition"
				);
				transactionRepository.save(transaction);

				System.out.println("[CupService] Pokalprämie für Team " + team.getName() + " (Runde " + round + "): " + bonus + "€");
			}
		} catch (Exception e) {
			System.err.println("[CupService] Fehler bei Pokalprämie: " + e.getMessage());
		}
	}

	/**
	 * Zahlt Extra-Prämie für den Pokalsieger
	 */
	private void processCupWinnerBonus(Long teamId) {
		try {
			Team team = teamRepository.findById(teamId).orElse(null);
			if (team == null) return;

			long bonus = 2_000_000; // Extra 2 Mio für Gewinner

			team.setBudget(team.getBudgetAsLong() + bonus);
			teamRepository.save(team);

			Transaction transaction = new Transaction(
				teamId,
				bonus,
				"income",
				"Pokalsieger-Prämie",
				"competition"
			);
			transactionRepository.save(transaction);

			System.out.println("[CupService] Pokalsieger-Prämie für Team " + team.getName() + ": " + bonus + "€");
		} catch (Exception e) {
			System.err.println("[CupService] Fehler bei Pokalsieger-Prämie: " + e.getMessage());
		}
	}
}
