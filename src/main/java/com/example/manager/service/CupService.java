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
import com.example.manager.model.Player;
import com.example.manager.model.Team;
import com.example.manager.repository.CupMatchRepository;
import com.example.manager.repository.CupTeamRepository;
import com.example.manager.repository.CupTournamentRepository;
import com.example.manager.repository.LeagueRepository;
import com.example.manager.repository.LineupRepository;
import com.example.manager.repository.PlayerRepository;
import com.example.manager.repository.TeamRepository;

/**
 * Service for managing cup tournaments. - 64 teams per country per season - 6
 * rounds (KO-Modus) - 20 random teams from division 3 are excluded in first
 * season
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

		// Sammle alle Teams aus allen Ligen dieses Landes
		Set<Long> teamIds = new java.util.HashSet<>();
		for (League league : countryLeagues) {
			for (LeagueSlot slot : league.getSlots()) {
				if (slot.getTeamId() != null) {
					teamIds.add(slot.getTeamId());
				}
			}
		}

		// Lade die Team-Objekte
		List<Team> allTeamsForCountry = new ArrayList<>();
		for (Long teamId : teamIds) {
			Team team = teamRepository.findById(teamId).orElse(null);
			if (team != null) {
				allTeamsForCountry.add(team);
			}
		}

		System.out.println("[CupService] Found " + allTeamsForCountry.size() + " teams for country " + country);

		// Nimm bis zu 64 Teams (oder alle wenn weniger vorhanden)
		List<Team> cupTeams = new ArrayList<>(allTeamsForCountry);
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

			String resultNote = ""; // Für i.V. oder i.E.

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
						resultNote = " i.E.";
						System.out.println("[CupService] Penalties: " + match.getHomeTeamName() + " wins");
					} else {
						awayGoals++; // Away Team gewinnt im Elfmeterschießen
						resultNote = " i.E.";
						System.out.println("[CupService] Penalties: " + match.getAwayTeamName() + " wins");
					}
				} else {
					resultNote = " i.V.";
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
}
