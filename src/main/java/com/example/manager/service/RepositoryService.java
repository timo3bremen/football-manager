package com.example.manager.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.manager.dto.CleanSheetDTO;
import com.example.manager.dto.LeagueInfoDTO;
import com.example.manager.dto.LeagueStandingsDTO;
import com.example.manager.dto.LeagueStatisticsDTO;
import com.example.manager.dto.MatchEventDTO;
import com.example.manager.dto.MatchReportDTO;
import com.example.manager.dto.MatchSimulationResultDTO;
import com.example.manager.dto.PlayerLineupDTO;
import com.example.manager.dto.PlayerStatisticsDTO;
import com.example.manager.dto.TeamDetailsDTO;
import com.example.manager.model.CupTournament;
import com.example.manager.model.GameState;
import com.example.manager.model.GameStateTracking;
import com.example.manager.model.League;
import com.example.manager.model.LeagueSlot;
import com.example.manager.model.LineupSlot;
import com.example.manager.model.Match;
import com.example.manager.model.MatchEvent;
import com.example.manager.model.Matchday;
import com.example.manager.model.Player;
import com.example.manager.model.Schedule;
import com.example.manager.model.Scout;
import com.example.manager.model.Sponsor;
import com.example.manager.model.StadiumBuild;
import com.example.manager.model.Team;
import com.example.manager.model.Transaction;
import com.example.manager.model.User;
import com.example.manager.model.YouthPlayer;
import com.example.manager.repository.CupTournamentRepository;
import com.example.manager.repository.GameStateRepository;
import com.example.manager.repository.GameStateTrackingRepository;
import com.example.manager.repository.LeagueRepository;
import com.example.manager.repository.LeagueSlotRepository;
import com.example.manager.repository.LineupRepository;
import com.example.manager.repository.MatchEventRepository;
import com.example.manager.repository.MatchRepository;
import com.example.manager.repository.MatchdayRepository;
import com.example.manager.repository.PlayerRepository;
import com.example.manager.repository.ScheduleRepository;
import com.example.manager.repository.ScoutRepository;
import com.example.manager.repository.SponsorRepository;
import com.example.manager.repository.StadiumBuildRepository;
import com.example.manager.repository.StadiumPartRepository;
import com.example.manager.repository.TeamRepository;
import com.example.manager.repository.TransactionRepository;
import com.example.manager.repository.UserRepository;
import com.example.manager.repository.YouthPlayerRepository;
import com.example.manager.util.PlayerNameGenerator;
import com.example.manager.util.TeamNameGenerator;
import com.example.manager.util.YouthPlayerGenerator;

/**
 * New repository-based service using Spring Data JPA.
 */
@Service
public class RepositoryService {

	@Autowired
	private PlayerRepository playerRepository;

	@Autowired
	private TeamRepository teamRepository;

	@Autowired
	private LineupRepository lineupRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private GameStateRepository gameStateRepository;

	@Autowired
	private LeagueRepository leagueRepository;

	@Autowired
	private LeagueSlotRepository leagueSlotRepository;

	@Autowired
	private ScheduleRepository scheduleRepository;

	@Autowired
	private MatchdayRepository matchdayRepository;

	@Autowired
	private MatchRepository matchRepository;

	@Autowired
	private MatchEventRepository matchEventRepository;

	@Autowired
	private GameStateTrackingRepository gameStateTrackingRepository;

	@Autowired
	private SponsorRepository sponsorRepository;

	@Autowired
	private TransactionRepository transactionRepository;

	@Autowired
	private StadiumBuildRepository stadiumBuildRepository;

	@Autowired
	private StadiumPartRepository stadiumPartRepository;

	@Autowired
	private CupService cupService;

	@Autowired
	private CupTournamentRepository cupTournamentRepository;

	private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
	private final Map<String, String> sessions = new HashMap<>(); // token -> username
	private final Random random = new Random();

	// User management
	@Transactional
	public String registerUser(String username, String password, Long teamId) {
		if (userRepository.findByUsername(username).isPresent()) {
			throw new IllegalArgumentException("user exists");
		}
		User user = new User();
		user.setUsername(username);
		user.setPasswordHash(passwordEncoder.encode(password));
		user.setTeamId(teamId);
		userRepository.save(user);

		String token = UUID.randomUUID().toString();
		sessions.put(token, username);
		return token;
	}

	/**
	 * Registriert einen User mit Ligawahl (neue Version) WICHTIG: Das neue Team
	 * übernimmt die Daten vom alten CPU-Team und die Liga-Daten bleiben erhalten!
	 */
	@Transactional
	public String registerUserWithLeague(String username, String password, String teamName, Long leagueId) {
		if (userRepository.findByUsername(username).isPresent()) {
			throw new IllegalArgumentException("user exists");
		}

		// Finde die Liga und wähle einen zufälligen CPU-Team-Slot
		League league = leagueRepository.findById(leagueId).orElse(null);
		if (league == null) {
			throw new IllegalArgumentException("league not found");
		}

		// Finde zufälligen gefüllten Slot (CPU-Team) der ersetzt werden soll
		List<LeagueSlot> filledSlots = new ArrayList<>();
		for (LeagueSlot slot : league.getSlots()) {
			if (slot.getTeamId() != null) {
				filledSlots.add(slot);
			}
		}

		// Wenn keine gefüllten Slots vorhanden, erstelle neues Team
		if (filledSlots.isEmpty()) {
			Team team = new Team(teamName, 1000000);
			team.setCPU(false); // Markiere als User-Team (kein CPU-Team)
			team = saveTeam(team);
			LeagueSlot emptySlot = league.addTeam(team);
			if (emptySlot != null) {
				leagueSlotRepository.save(emptySlot);
			}
			// Erstelle User mit Liga-Zuordnung
			User user = new User(username, passwordEncoder.encode(password), team.getId(), leagueId);
			userRepository.save(user);
			String token = UUID.randomUUID().toString();
			sessions.put(token, username);
			return token;
		}

		// Wähle zufälligen CPU-Team Slot aus
		LeagueSlot randomSlot = filledSlots.get(random.nextInt(filledSlots.size()));
		Long oldTeamId = randomSlot.getTeamId();

		// WICHTIG: Aktualisiere BESTEHENDES Team statt neues zu erstellen!
		// Das übernimmt alle Daten (Spieler, Stadion, Lineups) vom alten Team
		Team oldTeam = teamRepository.findById(oldTeamId).orElse(null);
		if (oldTeam == null) {
			throw new IllegalArgumentException("Old team not found");
		}

		// Update Team Name
		oldTeam.setName(teamName);
//		oldTeam.setBudget(100000); // Reset Budget
		oldTeam.setCPU(false); // Markiere als User-Team (kein CPU-Team mehr)
		teamRepository.save(oldTeam);

		// Verwende oldTeamId als Team-ID für den neuen User
		Long userTeamId = oldTeamId;

		// Erstelle User mit Liga-Zuordnung (mit dem bestehenden Team!)
		User user = new User(username, passwordEncoder.encode(password), userTeamId, leagueId);
		userRepository.save(user);

		System.out.println("[RepositoryService] Benutzer '" + username + "' registriert in Liga " + leagueId
				+ " mit bestehendem Team " + oldTeamId + " (" + oldTeam.getName() + ")");

		String token = UUID.randomUUID().toString();
		sessions.put(token, username);
		return token;
	}

	public String authenticateUser(String username, String password) {
		User user = userRepository.findByUsername(username)
				.orElseThrow(() -> new IllegalArgumentException("user not found"));
		if (!passwordEncoder.matches(password, user.getPasswordHash())) {
			throw new IllegalArgumentException("invalid credentials");
		}
		String token = UUID.randomUUID().toString();
		sessions.put(token, username);
		return token;
	}

	public User getUserByToken(String token) {
		String username = sessions.get(token);
		if (username == null)
			return null;
		return userRepository.findByUsername(username).orElse(null);
	}

	public String getUsernameForToken(String token) {
		return sessions.get(token);
	}

	public Long getTeamIdForToken(String token) {
		String username = sessions.get(token);
		if (username == null)
			return null;
		return userRepository.findByUsername(username).map(User::getTeamId).orElse(null);
	}

	@Transactional
	public void clearUsers() {
		// Delete all users and their teams
		List<User> allUsers = userRepository.findAll();
		for (User user : allUsers) {
			if (user.getTeamId() != null) {
				deleteTeamCascade(user.getTeamId());
			}
		}
		userRepository.deleteAll();
		sessions.clear();

		// Delete all remaining teams (including CPU teams)
		List<Team> allTeams = teamRepository.findAll();
		for (Team team : allTeams) {
			deleteTeamCascade(team.getId());
		}

		// Delete all match events
		matchEventRepository.deleteAll();

		// Delete all matches
		matchRepository.deleteAll();

		// Delete all matchdays
		matchdayRepository.deleteAll();

		// Delete all schedules
		scheduleRepository.deleteAll();

		// Delete all league slots
		leagueSlotRepository.deleteAll();

		// Delete all leagues
		leagueRepository.deleteAll();

		// Delete all game state tracking
		gameStateTrackingRepository.deleteAll();

		// Delete all game states
		gameStateRepository.deleteAll();

		System.out.println(
				"[RepositoryService] All data cleared: users, teams, players, schedules, leagues, match events");
	}

	/**
	 * Löscht ein Team und alle zugehörigen Daten (Spieler, Lineups, Stadionteile).
	 */
	@Transactional
	public void deleteTeamCascade(Long teamId) {
		try {
			// Delete all matches where this team is home or away team
			List<Match> matchesToDelete = new ArrayList<>();
			for (Match match : matchRepository.findAll()) {
				if ((match.getHomeTeamId() != null && match.getHomeTeamId().equals(teamId))
						|| (match.getAwayTeamId() != null && match.getAwayTeamId().equals(teamId))) {
					matchesToDelete.add(match);
				}
			}
			matchRepository.deleteAll(matchesToDelete);

			// Delete lineups
			lineupRepository.deleteByTeamId(teamId);

			// Delete transactions
			transactionRepository.deleteByTeamId(teamId);

			// Delete players
			List<Player> players = playerRepository.findByTeamId(teamId);
			playerRepository.deleteAll(players);

			// Delete team
			teamRepository.deleteById(teamId);

			System.out.println("[RepositoryService] Team " + teamId + " and all related data deleted (including "
					+ matchesToDelete.size() + " matches)");
		} catch (Exception e) {
			System.err.println("[RepositoryService] Error deleting team " + teamId + ": " + e.getMessage());
			e.printStackTrace();
		}
	}

	// Player management
	public List<Player> listPlayers() {
		return playerRepository.findAll();
	}

	public Player getPlayer(Long id) {
		return playerRepository.findById(id).orElse(null);
	}

	@Transactional
	public Player savePlayer(Player player) {
		return playerRepository.save(player);
	}

	public List<Player> getPlayersByTeam(Long teamId) {
		return playerRepository.findByTeamId(teamId);
	}

	// Team management
	public List<Team> listTeams() {
		return teamRepository.findAll();
	}

	public Team getTeam(Long id) {
		return teamRepository.findById(id).orElse(null);
	}

	@Transactional
	public Team saveTeam(Team team) {
		Team saved = teamRepository.save(team);

		// Initialize default players if squad is empty
		if (saved.getId() != null) {
			List<Player> existing = playerRepository.findByTeamId(saved.getId());
			if (existing.isEmpty()) {
				// Benutzer-Teams bekommen 1. Liga Spieler
				initializeTeamPlayers(saved, 1);

				// Initialize lineup slots for all formations with playerId = null
				String[] formationIds = { "4-4-2", "4-3-3", "3-5-2" };

				// Slot names for each formation
				Map<String, String[]> slotNames = new HashMap<>();
				slotNames.put("4-4-2",
						new String[] { "GK", "D1", "D2", "D3", "D4", "M1", "M2", "M3", "M4", "F1", "F2" });
				slotNames.put("4-3-3",
						new String[] { "GK", "D1", "D2", "D3", "D4", "M1", "M2", "M3", "F1", "F2", "F3" });
				slotNames.put("3-5-2",
						new String[] { "GK", "D1", "D2", "D3", "M1", "M2", "M3", "M4", "M5", "F1", "F2" });

				for (String formationId : formationIds) {
					String[] names = slotNames.get(formationId);
					List<Player> players = playerRepository.findByTeamId(saved.getId());

					// Auto-assign best players to default lineup
					Map<Integer, Long> assignment = new HashMap<>();
					if (formationId.equals("4-4-2")) {
						assignment = autoAssignPlayers(players, 1, 4, 4, 2);
					} else if (formationId.equals("4-3-3")) {
						assignment = autoAssignPlayers(players, 1, 4, 3, 3);
					} else if (formationId.equals("3-5-2")) {
						assignment = autoAssignPlayers(players, 1, 3, 5, 2);
					}

					for (int slotIndex = 1; slotIndex <= 11; slotIndex++) {
						LineupSlot slot = new LineupSlot();
						slot.setTeamId(saved.getId());
						slot.setFormationId(formationId);
						slot.setSlotIndex(slotIndex);
						slot.setPlayerId(assignment.getOrDefault(slotIndex, null)); // Use assigned player
						slot.setSlotName(names[slotIndex - 1]); // Use proper slot names
						lineupRepository.save(slot);
					}
				}

				System.out.println(
						"[RepositoryService] Initialized 18 players and lineup slots for team " + saved.getId());
			}
		}

		// Add team to league
		addTeamToLeague(saved);

		return saved;
	}

	/**
	 * Updates only the team name without triggering player generation or league
	 * logic. Use this for simple name updates.
	 */
	@Transactional
	public Team updateTeamName(Long teamId, String newName) {
		Team team = teamRepository.findById(teamId).orElse(null);
		if (team == null) {
			throw new IllegalArgumentException("Team not found");
		}
		team.setName(newName);
		return teamRepository.save(team);
	}

	/**
	 * Adds a team to the league. For backwards compatibility. Does nothing since
	 * new teams are not auto-added to leagues anymore.
	 */
	private void addTeamToLeague(Team team) {
		// Legacy: Neue Teams werden nicht mehr automatisch einer Liga hinzugefügt
		// Sie werden nur bei Registrierung mit Liga hinzugefügt
	}

	/**
	 * Creates a schedule with 22 matchdays for a new league (double round-robin for
	 * 12 teams). Matchdays are created empty; matches are added during
	 * updateSchedule().
	 */
	private void createSchedule(League league) {
		Schedule schedule = new Schedule(league.getId());
		scheduleRepository.save(schedule);

		// 12 teams = 11 rounds per half = 22 matchdays total
		// + 3 off-season days after day 22
		int dayNumber = 1;

		// All 22 matchdays
		for (int day = 1; day <= 22; day++) {
			Matchday matchday = new Matchday(schedule.getId(), dayNumber);
			matchday.setLeagueId(league.getId()); // Set the league ID
			matchdayRepository.save(matchday);
			schedule.getMatchdays().add(matchday);
			dayNumber++;
		}

		// Off-season: Days 23-25 (3 tage)
		for (int offSeason = 0; offSeason < 3; offSeason++) {
			Matchday matchday = new Matchday(schedule.getId(), dayNumber);
			matchday.setLeagueId(league.getId()); // Set the league ID
			matchday.setIsOffSeason(true);
			matchdayRepository.save(matchday);
			schedule.getMatchdays().add(matchday);
			dayNumber++;
		}

		System.out.println("[RepositoryService] Schedule created with 22 matchdays + 3 off-season days for league "
				+ league.getId());
	}

	/**
	 * Updates the schedule by assigning teams to matches based on current league
	 * standings. Uses a simple round-robin algorithm.
	 */
	private void updateSchedule(League league) {
		// Finde den Schedule für diese spezifische Liga
		List<Schedule> allSchedules = scheduleRepository.findAll();
		Schedule schedule = null;
		for (Schedule s : allSchedules) {
			if (s.getLeagueId().equals(league.getId())) {
				schedule = s;
				break;
			}
		}

		if (schedule == null) {
			System.err.println("[RepositoryService] No schedule found for league " + league.getId());
			return;
		}

		List<LeagueSlot> slots = league.getSlots();
		List<Long> teamIds = new ArrayList<>();

		// Collect all team IDs (including nulls for empty slots)
		for (LeagueSlot slot : slots) {
			teamIds.add(slot.getTeamId());
		}

		// Generate round-robin matchups
		generateRoundRobinMatchups(schedule, teamIds);

		System.out
				.println("[RepositoryService] Schedule updated with new team assignments for league " + league.getId());
	}

	/**
	 * Generates a round-robin tournament with 22 matchdays (double round-robin for
	 * 12 teams). Uses the Berger (circle) algorithm with proper home/away
	 * balancing.
	 */
	private void generateRoundRobinMatchups(Schedule schedule, List<Long> teamIds) {

		final int NUM_TEAMS = 12;

		// Filter null teams
		List<Long> teams = new ArrayList<>();
		for (Long teamId : teamIds) {
			if (teamId != null) {
				teams.add(teamId);
			}
		}

		// Fill empty slots
		while (teams.size() < NUM_TEAMS) {
			teams.add(null);
		}

		// Collect matchdays
		List<Matchday> regularMatchdays = new ArrayList<>();
		for (Matchday md : schedule.getMatchdays()) {
			if (!md.isOffSeason()) {
				regularMatchdays.add(md);
			}
		}

		if (regularMatchdays.size() < 22) {
			System.err.println("[RepositoryService] Not enough matchdays for round-robin!");
			return;
		}

		int matchdayIdx = 0;

		int rounds = NUM_TEAMS - 1;
		int matchesPerRound = NUM_TEAMS / 2;

		List<Long> rotation = new ArrayList<>(teams);

		// Store first half
		List<List<long[]>> firstHalf = new ArrayList<>();

		// ---------- FIRST HALF ----------
		for (int round = 0; round < rounds; round++) {

			Matchday matchday = regularMatchdays.get(matchdayIdx++);
			matchday.getMatches().clear();

			List<long[]> roundMatches = new ArrayList<>();

			for (int i = 0; i < matchesPerRound; i++) {

				Long home;
				Long away;

				if (i == 0) {
					home = rotation.get(0);
					away = rotation.get(NUM_TEAMS - 1);
				} else {
					home = rotation.get(i);
					away = rotation.get(NUM_TEAMS - 1 - i);
				}

				// Alternate home/away per round
				if (round % 2 == 1) {
					Long temp = home;
					home = away;
					away = temp;
				}

				if (home != null && away != null) {

					Match m = new Match(matchday.getId(), home, away);
					matchRepository.save(m);
					matchday.getMatches().add(m);

					roundMatches.add(new long[] { home, away });
				}
			}

			firstHalf.add(roundMatches);

			matchdayRepository.save(matchday);

			// Rotate (keep first team fixed)
			List<Long> newRotation = new ArrayList<>();
			newRotation.add(rotation.get(0));
			newRotation.add(rotation.get(NUM_TEAMS - 1));
			newRotation.addAll(rotation.subList(1, NUM_TEAMS - 1));

			rotation = newRotation;
		}

		// ---------- SECOND HALF (REVERSED) ----------
		for (int round = 0; round < rounds; round++) {

			Matchday matchday = regularMatchdays.get(matchdayIdx++);
			matchday.getMatches().clear();

			List<long[]> roundMatches = firstHalf.get(round);

			for (long[] pair : roundMatches) {

				Long home = pair[1];
				Long away = pair[0];

				Match m = new Match(matchday.getId(), home, away);
				matchRepository.save(m);
				matchday.getMatches().add(m);
			}

			matchdayRepository.save(matchday);
		}

		System.out.println("[RepositoryService] Generated " + matchdayIdx + " matchdays (double round-robin)");
	}

	/**
	 * Auto-assigns the best players to a lineup based on formation. Returns a map
	 * of slotIndex -> playerId.
	 */
	private Map<Integer, Long> autoAssignPlayers(List<Player> players, int numGK, int numDEF, int numMID, int numFWD) {
		Map<Integer, Long> assignment = new HashMap<>();

		// Separate players by position and sort by rating
		List<Player> gk = new ArrayList<>();
		List<Player> def = new ArrayList<>();
		List<Player> mid = new ArrayList<>();
		List<Player> fwd = new ArrayList<>();

		for (Player p : players) {
			if ("GK".equals(p.getPosition())) {
				gk.add(p);
			} else if ("DEF".equals(p.getPosition())) {
				def.add(p);
			} else if ("MID".equals(p.getPosition())) {
				mid.add(p);
			} else if ("FWD".equals(p.getPosition())) {
				fwd.add(p);
			}
		}

		// Sort by rating descending
		gk.sort((a, b) -> Integer.compare(b.getRating(), a.getRating()));
		def.sort((a, b) -> Integer.compare(b.getRating(), a.getRating()));
		mid.sort((a, b) -> Integer.compare(b.getRating(), a.getRating()));
		fwd.sort((a, b) -> Integer.compare(b.getRating(), a.getRating()));

		// Assign best players to slots (1-indexed)
		int slotIndex = 1;

		// GK slots
		for (int i = 0; i < numGK && i < gk.size(); i++) {
			assignment.put(slotIndex++, gk.get(i).getId());
		}

		// DEF slots
		for (int i = 0; i < numDEF && i < def.size(); i++) {
			assignment.put(slotIndex++, def.get(i).getId());
		}

		// MID slots
		for (int i = 0; i < numMID && i < mid.size(); i++) {
			assignment.put(slotIndex++, mid.get(i).getId());
		}

		// FWD slots
		for (int i = 0; i < numFWD && i < fwd.size(); i++) {
			assignment.put(slotIndex++, fwd.get(i).getId());
		}

		return assignment;
	}

	// Lineup management
	@Transactional
	public void saveLineup(Long teamId, String formationId, Map<Integer, Long> slots) {
		// Delete existing lineup for this formation
		lineupRepository.deleteByTeamIdAndFormationId(teamId, formationId);

		// Save new slots
		int index = 1;
		for (Map.Entry<Integer, Long> entry : slots.entrySet()) {
			LineupSlot slot = new LineupSlot();
			slot.setTeamId(teamId);
			slot.setFormationId(formationId);
			slot.setSlotIndex(entry.getKey());
			slot.setPlayerId(entry.getValue());
			lineupRepository.save(slot);
			index++;
		}
	}

	public List<LineupSlot> getLineup(Long teamId, String formationId) {
		return lineupRepository.findByTeamIdAndFormationId(teamId, formationId);
	}

	public List<LineupSlot> getAllLineups(Long teamId) {
		return lineupRepository.findByTeamId(teamId);
	}

	// GameState management
	public String getGameState(Long teamId) {
		return gameStateRepository.findById(teamId).map(GameState::getJson).orElse(null);
	}

	@Transactional
	public void saveGameState(Long teamId, String json) {
		GameState state = gameStateRepository.findById(teamId).orElse(new GameState(teamId, json));
		state.setJson(json);
		gameStateRepository.save(state);
	}

	/**
	 * Initialisiert die 7 Standard-Ligen für Deutschland und Spanien mit CPU-Teams:
	 * - 1 x 1. Liga (12 Teams) - 2 x 2. Liga (je 12 Teams) - 4 x 3. Liga (je 12
	 * Teams)
	 */
	@Transactional
	public void initializeLigues() {
		List<League> existing = leagueRepository.findAll();
		if (!existing.isEmpty()) {
			System.out.println("[RepositoryService] Ligen existieren bereits, Initialisierung übersprungen");
			return;
		}

		// DEUTSCHLAND - 7 Ligen
		System.out.println("[RepositoryService] Initialisiere Ligen für Deutschland...");
		createLeaguesForCountry("Deutschland");

		// SPANIEN - 7 Ligen
		System.out.println("[RepositoryService] Initialisiere Ligen für Spanien...");
		createLeaguesForCountry("Spanien");

		System.out
				.println("[RepositoryService] 14 Ligen (2 Länder x 7 Ligen) mit insgesamt 168 CPU-Teams initialisiert");
	}

	/**
	 * Erstellt 7 Ligen (1. Liga, 2. Liga A/B, 3. Liga A/B/C/D) für ein bestimmtes
	 * Land
	 */
	private void createLeaguesForCountry(String country) {
		// 1. Liga
		createLeagueWithCPUTeams(country, 1, "1. Liga", "1. Liga", 12);

		// 2. Ligen
		createLeagueWithCPUTeams(country, 2, "2. Liga A", "2. Liga A", 12);
		createLeagueWithCPUTeams(country, 2, "2. Liga B", "2. Liga B", 12);

		// 3. Ligen
		createLeagueWithCPUTeams(country, 3, "3. Liga A", "3. Liga A", 12);
		createLeagueWithCPUTeams(country, 3, "3. Liga B", "3. Liga B", 12);
		createLeagueWithCPUTeams(country, 3, "3. Liga C", "3. Liga C", 12);
		createLeagueWithCPUTeams(country, 3, "3. Liga D", "3. Liga D", 12);

		System.out.println("[RepositoryService] 7 Ligen für " + country + " mit 84 CPU-Teams erstellt");
	}

	/**
	 * Erstellt eine Liga mit den angegebenen CPU-Teams für ein bestimmtes Land
	 */
	private void createLeagueWithCPUTeams(String country, int division, String name, String divisionLabel,
			int numTeams) {
		League league = new League(name);
		league.setCountry(country);
		league.setDivision(division);
		league.setDivisionLabel(divisionLabel);
		leagueRepository.save(league);

		// Erstelle 12 Slots
		for (int i = 1; i <= 12; i++) {
			LeagueSlot slot = new LeagueSlot(league.getId(), i);
			leagueSlotRepository.save(slot);
			league.getSlots().add(slot);
		}

		// Erstelle CPU-Teams für diese Liga
		for (int i = 0; i < numTeams; i++) {
			String teamName = TeamNameGenerator.generateTeamName();
			Team cpuTeam = new Team(teamName, 1000000);
			cpuTeam = teamRepository.save(cpuTeam);

			// Initialisiere Spieler mit divisions-abhängigen Stärken und Stadionteile
			initializeTeamPlayers(cpuTeam, division);
			initializeTeamStadium(cpuTeam);
			initializeTeamLineups(cpuTeam);

			// Füge zum Slot hinzu
			LeagueSlot slot = league.getSlots().get(i);
			slot.setTeamId(cpuTeam.getId());
			leagueSlotRepository.save(slot);
		}

		// Erstelle Schedule
		createSchedule(league);

		// Generiere Matches für die Liga (RoundRobin)
		updateSchedule(league);

		System.out.println("[RepositoryService] Liga '" + divisionLabel + "' mit " + numTeams + " CPU-Teams erstellt");
	}

	/**
	 * Initialisiert die Spieler für ein Team mit divisions-abhängigen Stärken 1.
	 * Liga: 70-90 2. Liga: 60-80 3. Liga: 50-70
	 * 
	 * Generiert auch 2 zufällige Starspieler mit +5 Bonus zu allen Stats
	 */
	private void initializeTeamPlayers(Team team, int division) {
		String[] positions = { "GK", "GK", "DEF", "DEF", "DEF", "DEF", "DEF", "DEF", "MID", "MID", "MID", "MID", "MID",
				"MID", "MID", "FWD", "FWD", "FWD" };

		Random rand = new Random();
		List<Player> createdPlayers = new ArrayList<>();

		for (int i = 0; i < 18; i++) {
			String[] playerData = PlayerNameGenerator.generatePlayerNameAndCountry();

			Player p = new Player(playerData[0], 0, 0, (int) (Math.random() * 20) - 10, positions[i], playerData[1]);

			// Initialisiere alle Fähigkeiten nach Division
			p.initializeSkillsForDivision(division, rand);

			int age = 18 + rand.nextInt(17); // Age 18-34
			long baseSalary = (long) (Math.pow(p.getRating(), 2.5) * 1.2);
			long salary = (baseSalary * age / 10) / 30; // Pro-Spiel Gehalt
			int contractLength = 1 + rand.nextInt(3); // Contract 1-3 seasons (random)

			p.setAge(age);
			p.setSalary(salary);
			p.setContractLength(contractLength);
			p.setTeamId(team.getId());
			p.calculateMarketValue();
			playerRepository.save(p);
			createdPlayers.add(p);
		}

		// Modifiziere 2 Spieler als "bessere" (+7 Rating) und 2 als "schlechtere" (-7
		// Rating)
		if (createdPlayers.size() >= 4) {
			// 2 bessere Spieler (+7 Rating)
			int better1Index = rand.nextInt(createdPlayers.size());
			int better2Index;
			do {
				better2Index = rand.nextInt(createdPlayers.size());
			} while (better2Index == better1Index);

			Player better1 = createdPlayers.get(better1Index);
			Player better2 = createdPlayers.get(better2Index);

			better1.setRating(Math.min(100, better1.getRating() + 7));
			better2.setRating(Math.min(100, better2.getRating() + 7));
			better1.calculateMarketValue();
			better2.calculateMarketValue();
			playerRepository.save(better1);
			playerRepository.save(better2);

			System.out.println("[RepositoryService] Spieler " + better1.getName() + " und " + better2.getName()
					+ " erhalten +7 Rating Boost");

			// 2 schlechtere Spieler (-7 Rating)
			int worse1Index;
			int worse2Index;
			do {
				worse1Index = rand.nextInt(createdPlayers.size());
			} while (worse1Index == better1Index || worse1Index == better2Index);

			do {
				worse2Index = rand.nextInt(createdPlayers.size());
			} while (worse2Index == better1Index || worse2Index == better2Index || worse2Index == worse1Index);

			Player worse1 = createdPlayers.get(worse1Index);
			Player worse2 = createdPlayers.get(worse2Index);

			worse1.setRating(Math.max(1, worse1.getRating() - 7));
			worse2.setRating(Math.max(1, worse2.getRating() - 7));
			worse1.calculateMarketValue();
			worse2.calculateMarketValue();
			playerRepository.save(worse1);
			playerRepository.save(worse2);

			System.out.println("[RepositoryService] Spieler " + worse1.getName() + " und " + worse2.getName()
					+ " erhalten -7 Rating Malus");
		}
	}

	/**
	 * Initialisiert die Spieler für ein Team (Standard - für Benutzer-Teams)
	 */
	private void initializeTeamPlayers(Team team) {
		// Benutzer-Teams bekommen 1. Liga Spieler
		initializeTeamPlayers(team, 1);
	}

	/**
	 * Initialisiert die Stadionteile für ein Team
	 */
	private void initializeTeamStadium(Team team) {
		// Stadium initialization is now handled by StadiumBuild system
		// No longer needed for new teams
	}

	/**
	 * Initialisiert die Lineups für ein Team
	 */
	private void initializeTeamLineups(Team team) {
		String[] formationIds = { "4-4-2", "4-3-3", "3-5-2" };

		Map<String, String[]> slotNames = new HashMap<>();
		slotNames.put("4-4-2", new String[] { "GK", "D1", "D2", "D3", "D4", "M1", "M2", "M3", "M4", "F1", "F2" });
		slotNames.put("4-3-3", new String[] { "GK", "D1", "D2", "D3", "D4", "M1", "M2", "M3", "F1", "F2", "F3" });
		slotNames.put("3-5-2", new String[] { "GK", "D1", "D2", "D3", "M1", "M2", "M3", "M4", "M5", "F1", "F2" });

		for (String formationId : formationIds) {
			String[] names = slotNames.get(formationId);
			List<Player> players = playerRepository.findByTeamId(team.getId());

			Map<Integer, Long> assignment = new HashMap<>();
			if (formationId.equals("4-4-2")) {
				assignment = autoAssignPlayers(players, 1, 4, 4, 2);
			} else if (formationId.equals("4-3-3")) {
				assignment = autoAssignPlayers(players, 1, 4, 3, 3);
			} else if (formationId.equals("3-5-2")) {
				assignment = autoAssignPlayers(players, 1, 3, 5, 2);
			}

			for (int slotIndex = 1; slotIndex <= 11; slotIndex++) {
				LineupSlot slot = new LineupSlot();
				slot.setTeamId(team.getId());
				slot.setFormationId(formationId);
				slot.setSlotIndex(slotIndex);
				slot.setPlayerId(assignment.getOrDefault(slotIndex, null));
				slot.setSlotName(names[slotIndex - 1]);
				lineupRepository.save(slot);
			}
		}
	}

	/**
	 * Returns detailed team information including strength.
	 */
	public TeamDetailsDTO getTeamDetails(Long teamId) {
		Team team = teamRepository.findById(teamId).orElse(null);
		if (team == null) {
			return null;
		}

		List<Player> allPlayers = playerRepository.findByTeamId(teamId);
		List<LineupSlot> lineup = lineupRepository.findByTeamIdAndFormationId(teamId, "4-4-2"); // Default formation

		int teamStrength = calculateTeamStrength(teamId);
		int playersInLineup = (int) lineup.stream().filter(s -> s.getPlayerId() != null).count();

		TeamDetailsDTO dto = new TeamDetailsDTO(teamId, team.getName(), playersInLineup, allPlayers.size(),
				teamStrength);

		// Use stadium capacity from Team model (sum of all types)
		long totalCapacity = team.getStadiumCapacity();
		dto.setStadiumCapacity((int) totalCapacity);

		// Find league info for this team
		List<League> allLeagues = leagueRepository.findAll();
		for (League league : allLeagues) {
			for (LeagueSlot slot : league.getSlots()) {
				if (slot.getTeamId() != null && slot.getTeamId().equals(teamId)) {
					dto.setLeagueName(league.getName());
					dto.setCountry(league.getCountry());
					break;
				}
			}
		}

		// Add lineup players
		for (LineupSlot slot : lineup) {
			if (slot.getPlayerId() != null) {
				Player p = playerRepository.findById(slot.getPlayerId()).orElse(null);
				if (p != null) {
					PlayerLineupDTO playerDto = new PlayerLineupDTO(p.getId(), p.getName(), p.getPosition(),
							p.getRating(), slot.getSlotIndex());
					dto.getLineup().add(playerDto);
				}
			}
		}

		// Add all players
		for (Player p : allPlayers) {
			PlayerLineupDTO playerDto = new PlayerLineupDTO(p.getId(), p.getName(), p.getPosition(), p.getRating(),
					p.getAge(), p.getCountry());
			dto.getAllPlayers().add(playerDto);
		}

		return dto;
	}

	/**
	 * Gibt alle verfügbaren Ligen mit Informationen zurück (für
	 * Registrierungsformular)
	 */
	public List<LeagueInfoDTO> getAvailableLeagues() {
		List<League> leagues = leagueRepository.findAll();
		List<LeagueInfoDTO> result = new ArrayList<>();

		for (League league : leagues) {
			int filledSlots = league.getFilledSlots();
			int totalSlots = league.getSlots().size();

			LeagueInfoDTO dto = new LeagueInfoDTO(league.getId(), league.getName(), league.getCountry(),
					league.getDivision(), league.getDivisionLabel(), filledSlots, totalSlots);
			result.add(dto);
		}

		// Sortiere nach Land, dann Division, dann Name
		result.sort((a, b) -> {
			// Erst nach Land sortieren
			int countryCompare = a.getCountry().compareTo(b.getCountry());
			if (countryCompare != 0) {
				return countryCompare;
			}
			// Dann nach Division
			if (a.getDivision() != b.getDivision()) {
				return Integer.compare(a.getDivision(), b.getDivision());
			}
			// Dann nach Name
			return a.getDivisionLabel().compareTo(b.getDivisionLabel());
		});

		return result;
	}

	/**
	 * Gibt alle verfügbaren Länder zurück
	 */
	public List<String> getAvailableCountries() {
		List<League> leagues = leagueRepository.findAll();
		List<String> countries = new ArrayList<>();

		for (League league : leagues) {
			if (league.getCountry() != null && !countries.contains(league.getCountry())) {
				countries.add(league.getCountry());
			}
		}

		// Alphabetisch sortieren
		countries.sort(String::compareTo);
		return countries;
	}

	/**
	 * Gibt alle Ligen für ein bestimmtes Land zurück
	 */
	public List<LeagueInfoDTO> getLeaguesByCountry(String country) {
		List<League> leagues = leagueRepository.findAll();
		List<LeagueInfoDTO> result = new ArrayList<>();

		for (League league : leagues) {
			if (country.equals(league.getCountry())) {
				int filledSlots = league.getFilledSlots();
				int totalSlots = league.getSlots().size();

				LeagueInfoDTO dto = new LeagueInfoDTO(league.getId(), league.getName(), league.getCountry(),
						league.getDivision(), league.getDivisionLabel(), filledSlots, totalSlots);
				result.add(dto);
			}
		}

		// Sortiere nach Division, dann Name
		result.sort((a, b) -> {
			if (a.getDivision() != b.getDivision()) {
				return Integer.compare(a.getDivision(), b.getDivision());
			}
			return a.getDivisionLabel().compareTo(b.getDivisionLabel());
		});

		return result;
	}

	/**
	 * Gibt alle Ligen mit ihren Standings zurück
	 */
	public List<LeagueStandingsDTO> getLeagueStandingsByLeagueId(Long leagueId) {
		League league = leagueRepository.findById(leagueId).orElse(null);
		if (league == null) {
			return new ArrayList<>();
		}

		// Map to store team stats: teamId -> {played, won, drawn, lost, goalsFor,
		// goalsAgainst, points}
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
		Schedule schedule = null;
		List<Schedule> schedules = scheduleRepository.findAll();
		for (Schedule s : schedules) {
			if (s.getLeagueId().equals(leagueId)) {
				schedule = s;
				break;
			}
		}

		if (schedule != null) {
			for (Matchday matchday : schedule.getMatchdays()) {
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
		List<LeagueStandingsDTO> standings = new ArrayList<>();
		int position = 1;
		for (Map.Entry<Long, Map<String, Integer>> entry : sortedTeams) {
			Long teamId = entry.getKey();
			Map<String, Integer> stats = entry.getValue();
			Team team = teamRepository.findById(teamId).orElse(null);

			if (team != null) {
				int strength = calculateTeamStrength(team.getId());
				LeagueStandingsDTO dto = new LeagueStandingsDTO(teamId, team.getName(), position, stats.get("played"),
						stats.get("won"), stats.get("drawn"), stats.get("lost"), stats.get("goalsFor"),
						stats.get("goalsAgainst"), stats.get("points"), strength);
				standings.add(dto);
				position++;
			}
		}

		return standings;
	}

	/**
	 * Returns the current league standings/table with team strength (für die
	 * User-Liga). Calculates wins/draws/losses from match results.
	 */
	public List<LeagueStandingsDTO> getLeagueStandings() {
		// Diese Methode sollte nur die Liga des aktuellen Users zurückgeben
		// Für Abwärtskompatibilität: Nehme die erste Liga
		List<League> leagues = leagueRepository.findAll();
		if (leagues.isEmpty()) {
			return new ArrayList<>();
		}

		return getLeagueStandingsByLeagueId(leagues.get(0).getId());
	}

	/**
	 * Calculates team strength as sum of all lineup players' ratings.
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
	 * Gets or creates the game state tracking (singleton).
	 */
	private GameStateTracking getOrCreateGameStateTracking() {
		List<GameStateTracking> all = gameStateTrackingRepository.findAll();
		if (all.isEmpty()) {
			GameStateTracking tracking = new GameStateTracking();
			tracking.setCurrentMatchday(1);
			tracking.setLastSimulationTime(System.currentTimeMillis());
			return gameStateTrackingRepository.save(tracking);
		}
		return all.get(0);
	}

	/**
	 * Gets the current matchday.
	 */
	public int getCurrentMatchday() {
		GameStateTracking tracking = getOrCreateGameStateTracking();
		return tracking.getCurrentMatchday();
	}

	/**
	 * Simulates a single match based on team strength. Only allows simulation of
	 * matches in the current matchday.
	 */
	@Transactional
	public MatchSimulationResultDTO simulateMatch(Long matchId) {
		Match match = matchRepository.findById(matchId).orElse(null);
		if (match == null) {
			throw new IllegalArgumentException("Match not found");
		}

		// Check if match is in the current matchday
		int currentMatchday = getCurrentMatchday();
		Matchday matchday = matchdayRepository.findById(match.getMatchdayId()).orElse(null);
		if (matchday == null) {
			throw new IllegalArgumentException("Matchday not found");
		}

		if (matchday.getDayNumber() != currentMatchday) {
			throw new IllegalArgumentException(
					"Can only simulate matches of the current matchday (Matchday " + currentMatchday + ")");
		}

		Long homeTeamId = match.getHomeTeamId();
		Long awayTeamId = match.getAwayTeamId();

		// If either team is null, cannot simulate
		if (homeTeamId == null || awayTeamId == null) {
			throw new IllegalArgumentException("Cannot simulate: one or both teams are null");
		}

		int homeStrength = calculateTeamStrength(homeTeamId);
		int awayStrength = calculateTeamStrength(awayTeamId);

		// Generate goals based on strength
		int homeGoals = generateGoals(homeStrength, awayStrength);
		int awayGoals = generateGoals(awayStrength, homeStrength);

		// Determine result
		String result;
		if (homeGoals > awayGoals) {
			result = "home";
		} else if (awayGoals > homeGoals) {
			result = "away";
		} else {
			result = "draw";
		}

		// Save match result
		match.setHomeGoals(homeGoals);
		match.setAwayGoals(awayGoals);
		match.setStatus("played");
		matchRepository.save(match);

		// Generate goal events
		generateGoalEvents(matchId, homeTeamId, awayTeamId, homeGoals, awayGoals);

		// Process sponsor payouts for both teams
		processSponsorPayouts(homeTeamId, result.equals("home"));
		processSponsorPayouts(awayTeamId, result.equals("away"));

		// Deduct player salaries for both teams
		deductPlayerSalaries(homeTeamId);
		deductPlayerSalaries(awayTeamId);

		// Train players from both teams who were in the lineup
		Team homeTeamForTraining = teamRepository.findById(homeTeamId).orElse(null);
		Team awayTeamForTraining = teamRepository.findById(awayTeamId).orElse(null);
		String homeFormationForTraining = (homeTeamForTraining != null
				&& homeTeamForTraining.getActiveFormation() != null) ? homeTeamForTraining.getActiveFormation()
						: "4-4-2";
		String awayFormationForTraining = (awayTeamForTraining != null
				&& awayTeamForTraining.getActiveFormation() != null) ? awayTeamForTraining.getActiveFormation()
						: "4-4-2";
		trainPlayersAfterMatch(homeTeamId, homeFormationForTraining);
		trainPlayersAfterMatch(awayTeamId, awayFormationForTraining);

		// Trainiere Akademie-Spieler beider Teams nach dem Spieltag
		trainAcademyPlayers(homeTeamId);
		trainAcademyPlayers(awayTeamId);

		// Berechne Zuschauereinnahmen und aktualisiere Fanfreundschaft (nur für
		// HomeTeam)
		processAttendanceRevenue(match, homeTeamId, awayTeamId, homeGoals, awayGoals, homeStrength, awayStrength);

		// Get team names
		Team homeTeam = teamRepository.findById(homeTeamId).orElse(null);
		Team awayTeam = teamRepository.findById(awayTeamId).orElse(null);
		String homeTeamName = homeTeam != null ? homeTeam.getName() : "Unknown";
		String awayTeamName = awayTeam != null ? awayTeam.getName() : "Unknown";

		return new MatchSimulationResultDTO(matchId, homeTeamId, awayTeamId, homeTeamName, awayTeamName, homeGoals,
				awayGoals, result);
	}

	/**
	 * Trainiert alle Spieler eines Teams, die in der Aufstellung waren Jede
	 * Fähigkeit hat 10% Chance um 1 zu wachsen (wenn unter dem Potential)
	 */
	private void trainPlayersAfterMatch(Long teamId, String formationId) {
		try {
			// Lade die aktuelle Aufstellung des Teams mit der angegebenen Formation
			List<LineupSlot> lineup = lineupRepository.findByTeamIdAndFormationId(teamId, formationId);

			Random rand = new Random();
			int trainedCount = 0;
			int skillsImproved = 0;

			for (LineupSlot slot : lineup) {
				if (slot.getPlayerId() != null) {
					Player player = playerRepository.findById(slot.getPlayerId()).orElse(null);
					if (player != null) {
						// Zähle verbesserte Skills vor Training
						int skillsBefore = getSkillCount(player);

						// Trainiere den Spieler
						player.trainAfterMatch(rand);

						// Zähle verbesserte Skills nach Training
						int skillsAfter = getSkillCount(player);

						// Speichere den trainierten Spieler
						playerRepository.save(player);

						trainedCount++;
						skillsImproved += (skillsAfter - skillsBefore);
					}
				}
			}

			if (trainedCount > 0) {
				System.out.println("[Training] Team " + teamId + ": " + trainedCount + " Spieler trainiert, "
						+ skillsImproved + " Skills verbessert");
			}
		} catch (Exception e) {
			System.err.println("[Training] Fehler beim Training für Team " + teamId + ": " + e.getMessage());
		}
	}

	/**
	 * Zählt die Summe aller Skills eines Spielers
	 */
	private int getSkillCount(Player player) {
		return player.getPace() + player.getDribbling() + player.getBallControl() + player.getShooting()
				+ player.getTackling() + player.getSliding() + player.getHeading() + player.getCrossing()
				+ player.getPassing() + player.getAwareness() + player.getJumping() + player.getStamina()
				+ player.getStrength();
	}

	/**
	 * Verarbeitet Zuschauereinnahmen und Fanfreundschafts-Änderungen nach einem
	 * Spiel Nur für das Heimteam (HomeTeam)
	 */
	private void processAttendanceRevenue(Match match, Long homeTeamId, Long awayTeamId, int homeGoals, int awayGoals,
			int homeStrength, int awayStrength) {
		try {
			Team homeTeam = teamRepository.findById(homeTeamId).orElse(null);
			if (homeTeam == null)
				return;

			// 1. Ermittle Auslastung basierend auf Fanfreundschaft
			int fanSatisfaction = homeTeam.getFanSatisfaction();
			double occupancyMin = 0.0;
			double occupancyMax = 0.0;

			if (fanSatisfaction >= 95) {
				occupancyMin = 1.0;
				occupancyMax = 1.0; // 100%
			} else if (fanSatisfaction >= 85) {
				occupancyMin = 0.90;
				occupancyMax = 1.0; // 90-100%
			} else if (fanSatisfaction >= 70) {
				occupancyMin = 0.75;
				occupancyMax = 0.90; // 75-90%
			} else if (fanSatisfaction >= 50) {
				occupancyMin = 0.50;
				occupancyMax = 0.75; // 50-75%
			} else if (fanSatisfaction >= 30) {
				occupancyMin = 0.35;
				occupancyMax = 0.50; // 35-50%
			} else if (fanSatisfaction >= 10) {
				occupancyMin = 0.10;
				occupancyMax = 0.35; // 10-35%
			} else {
				occupancyMin = 0.0;
				occupancyMax = 0.10; // 0-10%
			}

		// Zufällige Auslastung im Bereich
		double occupancy = occupancyMin + (occupancyMax - occupancyMin) * random.nextDouble();

		// 2. Lade Stadion-Kapazitäten (3 getrennte Platztypen)
		long standingSeats = homeTeam.getStadiumCapacityStanding() != null ? homeTeam.getStadiumCapacityStanding() : 1000L;
		long seatedSeats = homeTeam.getStadiumCapacitySeated() != null ? homeTeam.getStadiumCapacitySeated() : 0L;
		long vipSeats = homeTeam.getStadiumCapacityVip() != null ? homeTeam.getStadiumCapacityVip() : 0L;
		
		// 2.5. Lade Ticketpreise
		int ticketPriceStanding = homeTeam.getTicketPriceStanding();
		int ticketPriceSeated = homeTeam.getTicketPriceSeated();
		int ticketPriceVip = homeTeam.getTicketPriceVip();

		// 2.6. NEUE LOGIK: Preis-basierte Auslastungs-Reduktion
		// "Normale" Preis-Bereiche: Steh 20-40€, Sitz 40-80€, VIP 80-200€
		// Bei zu hohen Preisen: Drastische Reduktion der Auslastung für diesen Platztyp
		
		double standingOccupancyModifier = calculatePriceOccupancyModifier(ticketPriceStanding, 20, 40, 60, 100);
		double seatedOccupancyModifier = calculatePriceOccupancyModifier(ticketPriceSeated, 40, 80, 120, 200);
		double vipOccupancyModifier = calculatePriceOccupancyModifier(ticketPriceVip, 80, 200, 300, 500);

		// 3. Berechne Zuschauerzahlen mit preis-basierter Reduktion
		// VIP-Plätze: Auslastung direkt anwenden × Preis-Modifier
		long vipAttendance = (long) (vipSeats * occupancy * vipOccupancyModifier);
		
		// Steh- und Sitzplätze: Werden entsprechend Auslastung gefüllt × Preis-Modifier
		long standingAttendance = (long) (standingSeats * occupancy * standingOccupancyModifier);
		long seatedAttendance = (long) (seatedSeats * occupancy * seatedOccupancyModifier);

		long totalAttendance = standingAttendance + seatedAttendance + vipAttendance;

		// Speichere Zuschauerzahl im Match
		match.setAttendance(totalAttendance);
		matchRepository.save(match);

		// 4. Berechne Einnahmen (bereits geladene Ticketpreise verwenden)
		long standingRevenue = standingAttendance * ticketPriceStanding;
		long seatedRevenue = seatedAttendance * ticketPriceSeated;
		long vipRevenue = vipAttendance * ticketPriceVip;
		long totalRevenue = standingRevenue + seatedRevenue + vipRevenue;

		// 5. Füge Einnahmen zum Budget hinzu
		homeTeam.setBudgetAsLong(homeTeam.getBudgetAsLong() + totalRevenue);
		
		// 6. Speichere als Transaktion
		Transaction transaction = new Transaction(homeTeamId, totalRevenue, "income",
				"Zuschauereinnahmen (" + totalAttendance + " Zuschauer)", "attendance");
		transactionRepository.save(transaction);

		System.out.println("[Attendance] Team " + homeTeam.getName() + ": " + totalAttendance 
				+ " Zuschauer (Steh: " + standingAttendance + ", Sitz: " + seatedAttendance + ", VIP: " + vipAttendance 
				+ ") → " + totalRevenue + "€ Einnahmen");
		System.out.println("[Attendance] Auslastung: " + Math.round(occupancy * 100) + "%, Preis-Modifier: Steh=" 
				+ Math.round(standingOccupancyModifier * 100) + "%, Sitz=" + Math.round(seatedOccupancyModifier * 100) 
				+ "%, VIP=" + Math.round(vipOccupancyModifier * 100) + "%");
		System.out.println("[Attendance] Ticketpreise: Steh=" + ticketPriceStanding + "€, Sitz=" + ticketPriceSeated 
				+ "€, VIP=" + ticketPriceVip + "€");

		// 7. Aktualisiere Fanfreundschaft basierend auf Spielergebnis und Ticketpreisen
			updateFanSatisfaction(homeTeam, homeGoals, awayGoals, homeStrength, awayStrength);

			teamRepository.save(homeTeam);

		} catch (Exception e) {
			System.err.println(
					"[Attendance] Fehler bei Zuschauereinnahmen für Team " + homeTeamId + ": " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * Berechnet einen Auslastungs-Modifier basierend auf Ticketpreis
	 * 
	 * @param price Aktueller Ticketpreis
	 * @param normalMin Untere Grenze des normalen Preisbereichs (z.B. 20€ für Stehplatz)
	 * @param normalMax Obere Grenze des normalen Preisbereichs (z.B. 40€ für Stehplatz)
	 * @param highMax Obere Grenze für "hohe" Preise (z.B. 60€)
	 * @param extremeMax Obere Grenze für "extreme" Preise (z.B. 100€)
	 * @return Modifier zwischen 0.0 und 1.0 (oder höher für günstige Preise)
	 */
	private double calculatePriceOccupancyModifier(int price, int normalMin, int normalMax, int highMax, int extremeMax) {
		// Sehr günstig (unter normalMin): Leichter Bonus (+5-10%)
		if (price < normalMin / 2) {
			return 1.10; // +10% Auslastung
		} else if (price < normalMin) {
			return 1.05; // +5% Auslastung
		}
		
		// Normaler Bereich (normalMin bis normalMax): Volle Auslastung
		if (price <= normalMax) {
			return 1.0; // 100% normale Auslastung
		}
		
		// Leicht erhöht (normalMax bis highMax): Leichte Reduktion
		if (price <= highMax) {
			// Linear von 1.0 bis 0.6 (40% Reduktion)
			double factor = (double) (price - normalMax) / (highMax - normalMax);
			return 1.0 - (factor * 0.4);
		}
		
		// Hoch (highMax bis extremeMax): Starke Reduktion
		if (price <= extremeMax) {
			// Linear von 0.6 bis 0.2 (80% Reduktion)
			double factor = (double) (price - highMax) / (extremeMax - highMax);
			return 0.6 - (factor * 0.4);
		}
		
		// Extrem hoch (über extremeMax): Drastische Reduktion
		if (price <= extremeMax * 2) {
			// Linear von 0.2 bis 0.05 (95% Reduktion)
			double factor = (double) (price - extremeMax) / extremeMax;
			return Math.max(0.05, 0.2 - (factor * 0.15));
		}
		
		// Völlig unrealistisch (über extremeMax * 2): Minimale Auslastung
		return 0.01; // Nur 1% kommen noch
	}

	/**
	 * Aktualisiert die Fanfreundschaft basierend auf Spielergebnis, Gegner-Stärke
	 * und Ticketpreisen
	 */
	private void updateFanSatisfaction(Team homeTeam, int homeGoals, int awayGoals, int homeStrength,
			int awayStrength) {
		int currentSatisfaction = homeTeam.getFanSatisfaction();
		int strengthDiff = homeStrength - awayStrength;

		// 1. Basis-Änderung durch Spielergebnis
		int satisfactionChange = 0;

		if (homeGoals > awayGoals) {
			// Sieg
			if (strengthDiff > 150) {
				// Sieg gegen deutlich schwächeren Gegner: +1 bis +3
				satisfactionChange = 1 + random.nextInt(3);
			} else if (strengthDiff > 75) {
				// Sieg gegen etwas schwächeren Gegner: +2 bis +4
				satisfactionChange = 2 + random.nextInt(3);
			} else if (strengthDiff > -75) {
				// Sieg gegen ebenbürtigen Gegner: +3 bis +5
				satisfactionChange = 3 + random.nextInt(3);
			} else if (strengthDiff > -150) {
				// Sieg gegen etwas stärkeren Gegner: +4 bis +6
				satisfactionChange = 4 + random.nextInt(3);
			} else {
				// Sieg gegen deutlich stärkeren Gegner: +5 bis +8
				satisfactionChange = 5 + random.nextInt(4);
			}
		} else if (homeGoals < awayGoals) {
			// Niederlage
			if (strengthDiff > 150) {
				// Niederlage gegen deutlich schwächeren Gegner: -5 bis -8
				satisfactionChange = -8 + random.nextInt(4);
			} else if (strengthDiff > 75) {
				// Niederlage gegen etwas schwächeren Gegner: -4 bis -6
				satisfactionChange = -6 + random.nextInt(3);
			} else if (strengthDiff > -75) {
				// Niederlage gegen ebenbürtigen Gegner: -3 bis -5
				satisfactionChange = -5 + random.nextInt(3);
			} else if (strengthDiff > -150) {
				// Niederlage gegen etwas stärkeren Gegner: -2 bis -4
				satisfactionChange = -4 + random.nextInt(3);
			} else {
				// Niederlage gegen deutlich stärkeren Gegner: -1 bis -3
				satisfactionChange = -3 + random.nextInt(3);
			}
		} else {
			// Unentschieden: -1 bis +1 (neutral)
			satisfactionChange = -1 + random.nextInt(3);
		}

		// Prüfe ob Gegner ein Top-Team ist (Platz 1-4 in der Liga)
		boolean isTopTeamOpponent = isTopTeamInLeague(homeTeam.getId(), awayStrength);

		// 2. Modifikation durch Ticketpreise (fließende Übergänge)
		int ticketPriceStanding = homeTeam.getTicketPriceStanding();
		int ticketPriceSeated = homeTeam.getTicketPriceSeated();
		int ticketPriceVip = homeTeam.getTicketPriceVip();

		// Top-Team Bonus: +20% Toleranz bei hohen Preisen
		double topTeamBonus = isTopTeamOpponent ? 1.2 : 1.0;

		// STEHPLÄTZE: Neutral 20-40€, Positiv <20€, Negativ >40€
		double standingImpact = calculatePriceFanImpact(ticketPriceStanding, 20, 40, 60, topTeamBonus);
		
		// SITZPLÄTZE: Neutral 40-60€, Positiv <40€, Negativ >60€
		double seatedImpact = calculatePriceFanImpact(ticketPriceSeated, 40, 60, 100, topTeamBonus);
		
		// VIP: Neutral 80-150€, Positiv <80€, Negativ >150€
		double vipImpact = calculatePriceFanImpact(ticketPriceVip, 80, 150, 300, topTeamBonus);

		// Summiere alle Preis-Impacts (gerundet)
		int priceImpact = (int) Math.round(standingImpact + seatedImpact + vipImpact);
		satisfactionChange += priceImpact;

		// 3. Wende Änderung an
		int newSatisfaction = currentSatisfaction + satisfactionChange;
		homeTeam.setFanSatisfaction(newSatisfaction); // Clamp automatisch auf 0-100

		String resultText = homeGoals > awayGoals ? "Sieg" : homeGoals < awayGoals ? "Niederlage" : "Unentschieden";
		String topTeamInfo = isTopTeamOpponent ? " [vs Top-Team +20% Toleranz]" : "";
		System.out.println("[FanSatisfaction] Team " + homeTeam.getName() + ": " + resultText + topTeamInfo
				+ " → Fanfreundschaft " + currentSatisfaction + "% → " + homeTeam.getFanSatisfaction() + "% ("
				+ (satisfactionChange > 0 ? "+" : "") + satisfactionChange + ")");
		System.out.println("[FanSatisfaction] Preis-Impact: Steh=" + String.format("%.1f", standingImpact) 
				+ ", Sitz=" + String.format("%.1f", seatedImpact) + ", VIP=" + String.format("%.1f", vipImpact));
	}

	/**
	 * Berechnet den Fanfreundschafts-Impact eines Ticketpreises (fließender Übergang)
	 * 
	 * @param price Aktueller Preis
	 * @param neutralMin Untere Grenze des neutralen Bereichs (z.B. 20€ für Stehplatz)
	 * @param neutralMax Obere Grenze des neutralen Bereichs (z.B. 40€ für Stehplatz)
	 * @param maxPrice Maximaler erlaubter Preis (z.B. 60€)
	 * @param topTeamBonus Multiplier für Top-Team Spiele (1.2 = +20% Toleranz)
	 * @return Impact auf Fanfreundschaft (-3.0 bis +3.0)
	 */
	private double calculatePriceFanImpact(int price, int neutralMin, int neutralMax, int maxPrice, double topTeamBonus) {
		// Wende Top-Team Bonus an: Erhöht neutralMax
		int adjustedNeutralMax = (int) (neutralMax * topTeamBonus);
		
		// Neutraler Bereich: Keine Änderung
		if (price >= neutralMin && price <= adjustedNeutralMax) {
			return 0.0;
		}
		
		// Positiver Bereich (unter neutralMin): Je günstiger, desto besser
		// Fließender Übergang von neutralMin bis 0€
		if (price < neutralMin) {
			// 0€ → +3.0, neutralMin → 0.0 (linear)
			double factor = (double) (neutralMin - price) / neutralMin;
			return Math.min(3.0, factor * 3.0);
		}
		
		// Negativer Bereich (über adjustedNeutralMax): Je teurer, desto schlechter
		// Fließender Übergang von adjustedNeutralMax bis maxPrice
		if (price > adjustedNeutralMax) {
			// adjustedNeutralMax → 0.0, maxPrice → -3.0 (linear)
			double factor = (double) (price - adjustedNeutralMax) / (maxPrice - adjustedNeutralMax);
			return Math.max(-3.0, -factor * 3.0);
		}
		
		return 0.0;
	}

	/**
	 * Prüft ob das Auswärtsteam ein Top-Team ist (Platz 1-4 in der Liga)
	 */
	private boolean isTopTeamInLeague(Long homeTeamId, int awayStrength) {
		try {
			// Finde Liga des Heimteams
			List<League> allLeagues = leagueRepository.findAll();
			Long homeLeagueId = null;
			
			for (League league : allLeagues) {
				for (LeagueSlot slot : league.getSlots()) {
					if (slot.getTeamId() != null && slot.getTeamId().equals(homeTeamId)) {
						homeLeagueId = league.getId();
						break;
					}
				}
				if (homeLeagueId != null) break;
			}
			
			if (homeLeagueId == null) return false;
			
			// Hole Tabelle und prüfe ob awayTeam in Top 4 ist
			List<LeagueStandingsDTO> standings = getLeagueStandingsByLeagueId(homeLeagueId);
			
			// Prüfe ob das Auswärtsteam in den Top 4 ist (basierend auf Stärke)
			for (LeagueStandingsDTO standing : standings) {
				if (standing.getPosition() <= 4) {
					// Berechne Stärke dieses Teams
					int teamStrength = calculateTeamStrength(standing.getTeamId());
					// Wenn awayStrength in der Nähe eines Top-4 Teams ist, ist es ein Top-Team
					if (Math.abs(teamStrength - awayStrength) < 50) {
						return true;
					}
				}
			}
			
			return false;
		} catch (Exception e) {
			return false; // Bei Fehler: Kein Top-Team Bonus
		}
	}

	/**
	 * Verarbeitet Sponsorenzahlungen nach einem Spiel - Antritt (appearance): Wird
	 * immer gezahlt - Sieg (win): Wird nur bei Sieg gezahlt
	 */
	private void processSponsorPayouts(Long teamId, boolean won) {
		try {
			Sponsor sponsor = sponsorRepository.findByTeamId(teamId).orElse(null);
			if (sponsor == null) {
				return; // Kein Sponsor für dieses Team
			}

			Team team = teamRepository.findById(teamId).orElse(null);
			if (team == null) {
				return;
			}

			// Antritt-Zahlung (immer)
			if (sponsor.getAppearancePayout() > 0) {
				team.setBudget(team.getBudgetAsLong() + sponsor.getAppearancePayout());

				// Save as transaction
				Transaction transaction = new Transaction(teamId, Long.valueOf(sponsor.getAppearancePayout()), "income",
						"Sponsor-Antritt (" + sponsor.getName() + ")", "sponsors");
				transactionRepository.save(transaction);

				System.out.println("[Sponsor] Team " + team.getName() + " erhält " + sponsor.getAppearancePayout()
						+ "€ Antritt von " + sponsor.getName());
			}

			// Sieg-Zahlung (nur bei Sieg)
			if (won && sponsor.getWinPayout() > 0) {
				team.setBudget(team.getBudgetAsLong() + sponsor.getWinPayout());

				// Save as transaction
				Transaction transaction = new Transaction(teamId, Long.valueOf(sponsor.getWinPayout()), "income",
						"Sponsor-Sieg (" + sponsor.getName() + ")", "sponsors");
				transactionRepository.save(transaction);

				System.out.println("[Sponsor] Team " + team.getName() + " erhält " + sponsor.getWinPayout()
						+ "€ Siegprämie von " + sponsor.getName());
			}

			teamRepository.save(team);
		} catch (Exception e) {
			System.err.println("[Sponsor] Fehler bei Sponsorenzahlung für Team " + teamId + ": " + e.getMessage());
		}
	}

	/**
	 * Zieht Spielergehälter (pro Spiel) für alle Kaderspieler ab
	 */
	private void deductPlayerSalaries(Long teamId) {
		try {
			Team team = teamRepository.findById(teamId).orElse(null);
			if (team == null) {
				return;
			}

			List<Player> squadPlayers = playerRepository.findByTeamId(teamId);
			long totalSalaries = 0;

			for (Player player : squadPlayers) {
				totalSalaries += player.getSalary();
			}

			if (totalSalaries > 0) {
				team.setBudget(team.getBudgetAsLong() - totalSalaries);
				teamRepository.save(team);

				// Save as transaction
				Transaction transaction = new Transaction(teamId, -totalSalaries, "expense",
						"Spielergehälter (" + squadPlayers.size() + " Spieler)", "salaries");
				transactionRepository.save(transaction);

				System.out.println("[Salaries] Team " + team.getName() + " zahlt " + totalSalaries
						+ "€ Spielergehälter für " + squadPlayers.size() + " Spieler");
			}
		} catch (Exception e) {
			System.err.println(
					"[Salaries] Fehler beim Abzug der Spielergehälter für Team " + teamId + ": " + e.getMessage());
		}
	}

	/**
	 * Generiert Goal Events für den Spielbericht
	 */
	private void generateGoalEvents(Long matchId, Long homeTeamId, Long awayTeamId, int homeGoals, int awayGoals) {
		// Lösche alte Events
		matchEventRepository.deleteByMatchId(matchId);

		// Lade die aktiven Formationen der Teams
		Team homeTeam = teamRepository.findById(homeTeamId).orElse(null);
		Team awayTeam = teamRepository.findById(awayTeamId).orElse(null);
		String homeFormation = (homeTeam != null && homeTeam.getActiveFormation() != null)
				? homeTeam.getActiveFormation()
				: "4-4-2";
		String awayFormation = (awayTeam != null && awayTeam.getActiveFormation() != null)
				? awayTeam.getActiveFormation()
				: "4-4-2";

		List<LineupSlot> homeLineup = lineupRepository.findByTeamIdAndFormationId(homeTeamId, homeFormation);
		List<LineupSlot> awayLineup = lineupRepository.findByTeamIdAndFormationId(awayTeamId, awayFormation);

		List<Player> homePlayers = new ArrayList<>();
		List<Player> awayPlayers = new ArrayList<>();

		for (LineupSlot slot : homeLineup) {
			if (slot.getPlayerId() != null) {
				Player p = playerRepository.findById(slot.getPlayerId()).orElse(null);
				if (p != null) {
					homePlayers.add(p);
				}
			}
		}

		for (LineupSlot slot : awayLineup) {
			if (slot.getPlayerId() != null) {
				Player p = playerRepository.findById(slot.getPlayerId()).orElse(null);
				if (p != null) {
					awayPlayers.add(p);
				}
			}
		}

		// Generiere Tore für Home-Team
		for (int i = 0; i < homeGoals; i++) {
			if (!homePlayers.isEmpty()) {
				Player scorer = selectGoalScorer(homePlayers);
				if (scorer != null) {
					int minute = 1 + random.nextInt(90);
					MatchEvent event = new MatchEvent(matchId, homeTeamId, scorer.getId(), scorer.getName(), "goal",
							minute);
					matchEventRepository.save(event);
				}
			}
		}

		// Generiere Tore für Away-Team
		for (int i = 0; i < awayGoals; i++) {
			if (!awayPlayers.isEmpty()) {
				Player scorer = selectGoalScorer(awayPlayers);
				if (scorer != null) {
					int minute = 1 + random.nextInt(90);
					MatchEvent event = new MatchEvent(matchId, awayTeamId, scorer.getId(), scorer.getName(), "goal",
							minute);
					matchEventRepository.save(event);
				}
			}
		}
	}

	/**
	 * Wählt einen Torschützen basierend auf Position FWD: 50%, MID: 35%, DEF: 15%
	 */
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
			// 50% Chance für FWD
			return forwards.get(random.nextInt(forwards.size()));
		} else if (rand < 0.85 && !midfielders.isEmpty()) {
			// 35% Chance für MID
			return midfielders.get(random.nextInt(midfielders.size()));
		} else if (!defenders.isEmpty()) {
			// 15% Chance für DEF
			return defenders.get(random.nextInt(defenders.size()));
		} else if (!forwards.isEmpty()) {
			return forwards.get(random.nextInt(forwards.size()));
		} else if (!midfielders.isEmpty()) {
			return midfielders.get(random.nextInt(midfielders.size()));
		}

		return players.isEmpty() ? null : players.get(random.nextInt(players.size()));
	}

	/**
	 * Gibt den Spielbericht eines Spiels zurück
	 */
	public MatchReportDTO getMatchReport(Long matchId) {
		Match match = matchRepository.findById(matchId).orElse(null);
		if (match == null) {
			throw new IllegalArgumentException("Match not found");
		}

		Long homeTeamId = match.getHomeTeamId();
		Long awayTeamId = match.getAwayTeamId();

		Team homeTeam = teamRepository.findById(homeTeamId).orElse(null);
		Team awayTeam = teamRepository.findById(awayTeamId).orElse(null);

		String homeTeamName = homeTeam != null ? homeTeam.getName() : "Unknown";
		String awayTeamName = awayTeam != null ? awayTeam.getName() : "Unknown";

		// Lade alle Events für dieses Spiel
		List<MatchEvent> events = matchEventRepository.findByMatchId(matchId);
		List<MatchEventDTO> eventDTOs = new ArrayList<>();

		for (MatchEvent event : events) {
			// Bestimme Team-Name basierend auf teamId
			String teamName = event.getTeamId().equals(homeTeamId) ? homeTeamName : awayTeamName;
			MatchEventDTO dto = new MatchEventDTO(event.getId(), event.getMatchId(), event.getTeamId(), teamName,
					event.getPlayerId(), event.getPlayerName(), event.getType(), event.getMinute());
			eventDTOs.add(dto);
		}

		// Sortiere Events nach Minute
		eventDTOs.sort((a, b) -> Integer.compare(a.getMinute(), b.getMinute()));

		MatchReportDTO report = new MatchReportDTO(matchId, homeTeamId, awayTeamId, homeTeamName, awayTeamName,
				match.getHomeGoals(), match.getAwayGoals(), match.getHomeGoals() > match.getAwayGoals() ? "home"
						: match.getAwayGoals() > match.getHomeGoals() ? "away" : "draw",
				eventDTOs);

		// Füge Zuschauerzahl hinzu
		report.setAttendance(match.getAttendance());

		return report;
	}

	/**
	 * Gibt erweiterte Spielerstatistiken für die aktuelle Saison zurück Nur
	 * Torschützen (Tore > 0)
	 */
	public LeagueStatisticsDTO getExtendedLeagueStatistics(Long leagueId) {
		// Torschützen (nur mit Toren > 0)
		List<PlayerStatisticsDTO> topScorers = getLeagueStatistics(leagueId);

		// Zu-Null-Spiele (Teams mit 0 Gegentor) - liga-abhängig
		List<CleanSheetDTO> cleanSheets = getCleanSheets(leagueId);

		// Nur Torschützen, keine Karten mehr
		return new LeagueStatisticsDTO(topScorers, cleanSheets, new ArrayList<>(), new ArrayList<>());
	}

	/**
	 * Gibt Zu-Null-Spiele (Teams, die kein Gegentor kassiert haben) zurück -
	 * liga-abhängig
	 */
	private List<CleanSheetDTO> getCleanSheets(Long leagueId) {
		Map<Long, Integer> cleanSheetsMap = new HashMap<>();
		Map<Long, String> teamNamesMap = new HashMap<>();

		// Initialisiere nur Teams dieser Liga mit 0 Clean Sheets
		League league = leagueRepository.findById(leagueId).orElse(null);
		if (league != null) {
			for (LeagueSlot slot : league.getSlots()) {
				if (slot.getTeamId() != null) {
					cleanSheetsMap.put(slot.getTeamId(), 0);
					Team team = teamRepository.findById(slot.getTeamId()).orElse(null);
					if (team != null) {
						teamNamesMap.put(slot.getTeamId(), team.getName());
					}
				}
			}
		}

		// Zähle Zu-Null-Spiele - nur für diese Liga
		Schedule schedule = null;
		List<Schedule> schedules = scheduleRepository.findAll();
		for (Schedule s : schedules) {
			if (s.getLeagueId().equals(leagueId)) {
				schedule = s;
				break;
			}
		}

		if (schedule != null) {
			for (Matchday matchday : schedule.getMatchdays()) {
				for (Match match : matchday.getMatches()) {
					if ("played".equals(match.getStatus()) && match.getAwayGoals() != null) {
						Long homeTeamId = match.getHomeTeamId();
						Long awayTeamId = match.getAwayTeamId();

						// Wenn Home-Team 0 Tore kassiert hat
						if (homeTeamId != null && match.getAwayGoals() == 0 && cleanSheetsMap.containsKey(homeTeamId)) {
							cleanSheetsMap.put(homeTeamId, cleanSheetsMap.get(homeTeamId) + 1);
						}

						// Wenn Away-Team 0 Tore kassiert hat
						if (awayTeamId != null && match.getHomeGoals() == 0 && cleanSheetsMap.containsKey(awayTeamId)) {
							cleanSheetsMap.put(awayTeamId, cleanSheetsMap.get(awayTeamId) + 1);
						}
					}
				}
			}
		}

		// Konvertiere zu Liste und sortiere
		List<CleanSheetDTO> result = new ArrayList<>();
		for (Map.Entry<Long, Integer> entry : cleanSheetsMap.entrySet()) {
			if (entry.getValue() > 0) { // Nur Teams mit mindestens 1 Clean Sheet
				result.add(new CleanSheetDTO(entry.getKey(), teamNamesMap.get(entry.getKey()), entry.getValue()));
			}
		}

		result.sort((a, b) -> Integer.compare(b.getCleanSheets(), a.getCleanSheets()));
		return result;
	}

	/**
	 * Gibt Spielerstatistiken für die aktuelle Saison zurück - nur Tore > 0,
	 * liga-abhängig
	 */
	public List<PlayerStatisticsDTO> getLeagueStatistics(Long leagueId) {
		Map<Long, PlayerStatisticsDTO> playerStatsMap = new HashMap<>();

		// Finde den Schedule für diese Liga
		Schedule schedule = null;
		List<Schedule> schedules = scheduleRepository.findAll();
		for (Schedule s : schedules) {
			if (s.getLeagueId().equals(leagueId)) {
				schedule = s;
				break;
			}
		}

		if (schedule == null) {
			return new ArrayList<>();
		}

		// Lade nur Events aus Matches dieser Liga
		for (Matchday matchday : schedule.getMatchdays()) {
			for (Match match : matchday.getMatches()) {
				if ("played".equals(match.getStatus())) {
					List<MatchEvent> events = matchEventRepository.findByMatchId(match.getId());
					for (MatchEvent event : events) {
						// Nur Tore berücksichtigen
						if ("goal".equals(event.getType())) {
							PlayerStatisticsDTO stats = playerStatsMap.getOrDefault(event.getPlayerId(),
									new PlayerStatisticsDTO());

							if (stats.getPlayerId() == null) {
								stats.setPlayerId(event.getPlayerId());
								stats.setPlayerName(event.getPlayerName());
								stats.setTeamId(event.getTeamId());

								// Lade Team-Info
								Team team = teamRepository.findById(event.getTeamId()).orElse(null);
								if (team != null) {
									stats.setTeamName(team.getName());
								}

								// Lade Player-Info für Position
								Player p = playerRepository.findById(event.getPlayerId()).orElse(null);
								if (p != null) {
									stats.setPosition(p.getPosition());
								}
							}

							stats.setGoals(stats.getGoals() + 1);
							playerStatsMap.put(event.getPlayerId(), stats);
						}
					}
				}
			}
		}

		// Konvertiere zu Liste und sortiere nach Toren
		// Filtere nur Spieler mit Toren > 0
		List<PlayerStatisticsDTO> result = new ArrayList<>();
		for (PlayerStatisticsDTO stats : playerStatsMap.values()) {
			if (stats.getGoals() > 0) {
				result.add(stats);
			}
		}
		result.sort((a, b) -> Integer.compare(b.getGoals(), a.getGoals()));

		return result;
	}

	/**
	 * Generates goals based on team strength difference. Die Teamstärke hat GROSSE
	 * Auswirkung auf das Ergebnis!
	 */
	private int generateGoals(int teamStrength, int opponentStrength) {
		int strengthDiff = teamStrength - opponentStrength;
		double rand = random.nextDouble();

		// Wenn Team schwächer ist (negative differenz), dann mit umgekehrter
		// Wahrscheinlichkeit generieren
		if (strengthDiff < 0) {
			return generateGoalsForWeakerTeam(teamStrength, opponentStrength);
		}

		// Je größer die Differenz, desto höher die Wahrscheinlichkeit zu gewinnen UND
		// mehr Tore zu schießen

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
			if (rand < 0.89)
				return 5;
			if (rand < 0.96)
				return 6;
			return 7;
		}

		// Mittlere Differenz: 70-100
		if (strengthDiff > 70) {
			if (rand < 0.10)
				return 0;
			if (rand < 0.27)
				return 1;
			if (rand < 0.48)
				return 2;
			if (rand < 0.66)
				return 3;
			if (rand < 0.80)
				return 4;
			if (rand < 0.90)
				return 5;
			if (rand < 0.96)
				return 6;
			return 7;
		}

		// Kleine Differenz: 40-70
		if (strengthDiff > 40) {
			if (rand < 0.15)
				return 0;
			if (rand < 0.35)
				return 1;
			if (rand < 0.53)
				return 2;
			if (rand < 0.68)
				return 3;
			if (rand < 0.81)
				return 4;
			if (rand < 0.91)
				return 5;
			if (rand < 0.97)
				return 6;
			return 7;
		}

		// Sehr kleine Differenz: 20-40
		if (strengthDiff > 20) {
			if (rand < 0.20)
				return 0;
			if (rand < 0.40)
				return 1;
			if (rand < 0.56)
				return 2;
			if (rand < 0.70)
				return 3;
			if (rand < 0.82)
				return 4;
			if (rand < 0.92)
				return 5;
			if (rand < 0.97)
				return 6;
			return 7;
		}

		// strengthDiff <= 20: Sehr ausgeglichen
		if (rand < 0.28)
			return 0;
		if (rand < 0.48)
			return 1;
		if (rand < 0.62)
			return 2;
		if (rand < 0.72)
			return 3;
		if (rand < 0.83)
			return 4;
		if (rand < 0.93)
			return 5;
		if (rand < 0.98)
			return 6;
		return 7;
	}

	/**
	 * Generiert Tore für schwächeres Team (negative Differenz)
	 */
	private int generateGoalsForWeakerTeam(int teamStrength, int opponentStrength) {
		int strengthDiff = opponentStrength - teamStrength;
		double rand = random.nextDouble();

		// Spiegelbild: Je größer der Unterschied, desto weniger Chancen

		if (strengthDiff > 200) {
			if (rand < 0.70)
				return 0;
			if (rand < 0.88)
				return 1;
			if (rand < 0.96)
				return 2;
			if (rand < 0.99)
				return 3;
			return 4;
		}

		if (strengthDiff > 150) {
			if (rand < 0.60)
				return 0;
			if (rand < 0.82)
				return 1;
			if (rand < 0.93)
				return 2;
			if (rand < 0.98)
				return 3;
			return 4;
		}

		if (strengthDiff > 100) {
			if (rand < 0.50)
				return 0;
			if (rand < 0.74)
				return 1;
			if (rand < 0.90)
				return 2;
			if (rand < 0.97)
				return 3;
			return 4;
		}

		if (strengthDiff > 70) {
			if (rand < 0.40)
				return 0;
			if (rand < 0.65)
				return 1;
			if (rand < 0.84)
				return 2;
			if (rand < 0.94)
				return 3;
			return 4;
		}

		if (strengthDiff > 40) {
			if (rand < 0.35)
				return 0;
			if (rand < 0.60)
				return 1;
			if (rand < 0.79)
				return 2;
			if (rand < 0.91)
				return 3;
			return 4;
		}

		if (strengthDiff > 20) {
			if (rand < 0.32)
				return 0;
			if (rand < 0.57)
				return 1;
			if (rand < 0.77)
				return 2;
			if (rand < 0.89)
				return 3;
			return 4;
		}

		// strengthDiff <= 20: Sehr ausgeglichen
		if (rand < 0.28)
			return 0;
		if (rand < 0.48)
			return 1;
		if (rand < 0.62)
			return 2;
		if (rand < 0.72)
			return 3;
		if (rand < 0.83)
			return 4;
		if (rand < 0.93)
			return 5;
		if (rand < 0.98)
			return 6;
		return 7;
	}

	/**
	 * Führt tägliche Aufgaben durch wenn "Nächster Tag" geklickt wird: 1. Löscht
	 * alle Transaktionen von heute (Tagesfinanzen zurückgesetzt) 2. Verkürzt alle
	 * Stadionausbauten um 24h oder schließt sie ab wenn < 24h übrig
	 */
	@Transactional
	private void processDailyTasks() {
		try {
			System.out.println("[RepositoryService] 📅 Verarbeite tägliche Tasks beim Tag-Wechsel...");

		// 1. Lösche nur HEUTIGE Tagesfinanzen (Sponsoren, Zinsen)
		// NICHT mehr: Zuschauereinnahmen (bleiben für die Saison)
		// NICHT: Infrastruktur, Transfers, Spielergehälter
		LocalDateTime now = LocalDateTime.now();
		LocalDateTime startOfDay = now.withHour(0).withMinute(0).withSecond(0).withNano(0);
		LocalDateTime endOfDay = now.withHour(23).withMinute(59).withSecond(59).withNano(999999999);

		List<Transaction> transactions = transactionRepository.findAll();
		List<Transaction> todaysTransactions = new ArrayList<>();

		for (Transaction t : transactions) {
			if (t.getCreatedAt() != null && t.getCreatedAt().isAfter(startOfDay)
					&& t.getCreatedAt().isBefore(endOfDay)) {
				// Lösche nur Sponsoren und Zinsen-Transaktionen (NICHT Zuschauereinnahmen!)
				String cat = t.getCategory() != null ? t.getCategory() : "";
				if (cat.equals("sponsors") || cat.equals("interest")) {
					todaysTransactions.add(t);
				}
			}
		}

		if (!todaysTransactions.isEmpty()) {
			System.out.println("[RepositoryService] 💳 Lösche " + todaysTransactions.size()
					+ " Tages-Transaktionen (Sponsoren, Zinsen) - Zuschauereinnahmen bleiben!");
			transactionRepository.deleteAll(todaysTransactions);
		}

		// 2. Verkürze Stadionausbauten um 24h oder schließe sie ab
		List<StadiumBuild> stadiumBuilds = stadiumBuildRepository.findAll();

		for (StadiumBuild build : stadiumBuilds) {
			if (build.getCompleted())
				continue; // Überspringe bereits abgeschlossene

			LocalDateTime endTime = build.getEndTime();
			// Berechne verbleibende Zeit
			if (endTime.isBefore(now)) {
				// Bereits abgelaufen - markiere als komplett
				System.out.println("[RepositoryService] 🏗️ Stadionausbau ID " + build.getId() + " ist fertig");
				build.setCompleted(true);
				// Stadium capacity is updated by completeBuild endpoint
			} else if (endTime.isBefore(now.plusDays(1))) {
				// Weniger als 1 Tag übrig - schließe jetzt ab
				System.out.println(
						"[RepositoryService] ⚡ Stadionausbau ID " + build.getId() + " mit < 24h abgeschlossen");
				build.setCompleted(true);
				// Stadium capacity is updated by completeBuild endpoint
			} else {
				// Mehr als 1 Tag übrig - verkürze um 24 Stunden
				LocalDateTime newEndTime = endTime.minusDays(1);
				System.out.println("[RepositoryService] ⏱️ Stadionausbau ID " + build.getId() + " um 24h verkürzt");
				build.setEndTime(newEndTime);
			}

			stadiumBuildRepository.save(build);
			}

			System.out.println("[RepositoryService] ✅ Tägliche Tasks abgeschlossen");

		} catch (Exception e) {
			System.err.println("[RepositoryService] ❌ Fehler bei Tägliche Tasks: " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * Simuliert alle ausstehenden Spiele des aktuellen Spieltags (für ALLE Ligen)
	 * und erhöht dann den aktuellen Spieltag
	 */
	@Transactional
	public Map<String, Object> advanceToNextMatchday() {
		// Verarbeite tägliche Tasks am Anfang
		processDailyTasks();

		int currentMatchday = getCurrentMatchday();

		// Finde ALLE Matchdays für den aktuellen Spieltag (alle Ligen!)
		List<Matchday> matchdaysForCurrentDay = new ArrayList<>();
		List<Matchday> allMatchdays = matchdayRepository.findAll();
		for (Matchday md : allMatchdays) {
			if (md.getDayNumber() == currentMatchday) {
				matchdaysForCurrentDay.add(md);
			}
		}

		Map<String, Object> result = new HashMap<>();
		result.put("previousMatchday", currentMatchday);
		result.put("simulatedMatches", 0);
		result.put("message", "");

		// Prüfe ob es nur Off-Season Matchdays gibt
		boolean allOffSeason = true;
		for (Matchday md : matchdaysForCurrentDay) {
			if (!md.isOffSeason()) {
				allOffSeason = false;
				break;
			}
		}

		// Wenn Off-Season, dann direkt zum nächsten Tag
		if (allOffSeason && !matchdaysForCurrentDay.isEmpty()) {
			// Erhöhe Spieltag
			GameStateTracking tracking = getOrCreateGameStateTracking();
			int nextMatchday = currentMatchday + 1;

			// Wenn Spieltag 25 erreicht: Saison Reset mit Auf- und Abstieg
			if (currentMatchday == 25) {
				resetSeasonWithPromotion();
				nextMatchday = 1; // Zurück auf Spieltag 1
			}

			tracking.setCurrentMatchday(nextMatchday);
			tracking.setLastSimulationTime(System.currentTimeMillis());
			gameStateTrackingRepository.save(tracking);

			result.put("newMatchday", tracking.getCurrentMatchday());
			result.put("isOffSeason", true);
			if (currentMatchday == 25) {
				result.put("message", "🏆 SAISON BEENDET! Auf- und Abstieg durchgeführt. Neue Saison startet!");
				result.put("seasonReset", true);
			} else {
				result.put("message", "Off-Season Tag - keine Spiele simuliert");
			}
			return result;
		}

		// Simuliere alle ausstehenden Spiele aller Matchdays für diesen Tag
		int totalSimulated = 0;
		for (Matchday matchday : matchdaysForCurrentDay) {
			if (matchday != null && matchday.getMatches() != null) {
				for (Match match : matchday.getMatches()) {
					if (!"played".equals(match.getStatus())) {
						try {
							simulateMatch(match.getId());
							totalSimulated++;
						} catch (Exception e) {
							// Ignoriere Fehler und fahre fort
							System.err.println(
									"Fehler beim Simulieren von Match " + match.getId() + ": " + e.getMessage());
						}
					}
				}
			}
		}
		result.put("simulatedMatches", totalSimulated);

		// Simuliere Cup-Spiele wenn es ein Cup-Spieltag ist (alle 3 Spieltage: 3, 6, 9,
		// 12, 15, 18, 21)
		if (currentMatchday % 3 == 0 && currentMatchday <= 21) {
			System.out.println("[RepositoryService] 🏆 Cup Spieltag " + currentMatchday + " - Simuliere Cup-Spiele");
			int cupRound = currentMatchday / 3; // Runde 1-7
			try {
				// Finde alle Cup-Turniere (für alle Länder) die aktuell aktiv sind
				List<CupTournament> activeCups = cupTournamentRepository.findByStatus("active");

				if (activeCups.isEmpty()) {
					System.out.println("[RepositoryService] ⚠️ Keine aktiven Cup-Turniere gefunden!");
				} else {
					System.out.println("[RepositoryService] 🏆 Gefundene Cup-Turniere: " + activeCups.size());
					for (CupTournament tournament : activeCups) {
						System.out.println("[RepositoryService] 🏆 - " + tournament.getCountry() + " (Runde: "
								+ tournament.getCurrentRound() + ", Status: " + tournament.getStatus() + ")");

						// Wenn Cup-Runde noch nicht generiert wurde, generiere sie jetzt
						if (tournament.getCurrentRound() < cupRound) {
							System.out.println("[RepositoryService] 🏆 Generiere " + tournament.getCountry()
									+ " Cup Runde " + cupRound);
							cupService.generateCupRound(tournament, cupRound);
						}

						// Simuliere die Runde wenn sie matchday-ready ist
						if (tournament.getCurrentRound() == cupRound) {
							System.out.println("[RepositoryService] 🏆 Simuliere " + tournament.getCountry()
									+ " Cup Runde " + cupRound);
							cupService.completeCupRound(tournament, cupRound);
						} else {
							System.out.println("[RepositoryService] ⚠️ Cup-Runde mismatch: " + tournament.getCountry()
									+ " ist in Runde " + tournament.getCurrentRound() + ", nicht " + cupRound);
						}
					}
				}
			} catch (Exception e) {
				System.err.println("[RepositoryService] Fehler bei Cup-Simulation: " + e.getMessage());
				e.printStackTrace();
			}
		}

		// Erhöhe Spieltag
		GameStateTracking tracking = getOrCreateGameStateTracking();
		int nextMatchday = Math.min(25, currentMatchday + 1);
		tracking.setCurrentMatchday(nextMatchday);
		tracking.setLastSimulationTime(System.currentTimeMillis());
		gameStateTrackingRepository.save(tracking);

		result.put("newMatchday", tracking.getCurrentMatchday());
		result.put("isOffSeason", false);
		result.put("message", "Spieltag " + currentMatchday + " abgeschlossen, " + result.get("simulatedMatches")
				+ " Spiele simuliert");

		return result;
	}

	/**
	 * Simuliert die ganze Saison schnell bis zum Ende (Tag 25)
	 */
	@Transactional
	public Map<String, Object> simulateEntireSeasonFast() {
		int currentMatchday = getCurrentMatchday();
		int totalMatches = 0;

		System.out.println(
				"[RepositoryService] ⚡ Starte schnelle Saison-Simulation vom Tag " + currentMatchday + " bis 25...");

		// Simuliere alle Spieltage von jetzt bis 25
		while (currentMatchday <= 25) {
			// Simuliere aktuellen Tag
			List<Matchday> allMatchdays = matchdayRepository.findAll();
			for (Matchday md : allMatchdays) {
				if (md.getDayNumber() == currentMatchday && !md.isOffSeason()) {
					for (Match match : md.getMatches()) {
						if (!"played".equals(match.getStatus())) {
							try {
								simulateMatch(match.getId());
								totalMatches++;
							} catch (Exception e) {
								System.err.println("Fehler: " + e.getMessage());
							}
						}
					}
				}
			}

			// Erhöhe auf nächsten Tag
			if (currentMatchday < 25) {
				GameStateTracking tracking = getOrCreateGameStateTracking();
				currentMatchday++;
				tracking.setCurrentMatchday(currentMatchday);
				tracking.setLastSimulationTime(System.currentTimeMillis());
				gameStateTrackingRepository.save(tracking);

				// Wenn Saison-Ende erreicht, führe Reset durch
				if (currentMatchday == 26) {
					resetSeasonWithPromotion();
					currentMatchday = 1;
				}
			} else {
				break;
			}
		}

		Map<String, Object> result = new HashMap<>();
		result.put("newMatchday", Math.min(25, currentMatchday));
		result.put("simulatedMatches", totalMatches);
		result.put("message", "⚡ Saison-Simulation abgeschlossen! " + totalMatches + " Spiele simuliert.");

		System.out.println("[RepositoryService] ✅ Saison-Simulation mit " + totalMatches + " Spielen abgeschlossen");

		return result;
	}

	/**
	 * Advances matchday if time has passed 18:00 UTC (6 PM).
	 */
	public void checkAndAdvanceMatchday() {
		GameStateTracking tracking = getOrCreateGameStateTracking();
		long now = System.currentTimeMillis();
		long dayInMs = 24 * 60 * 60 * 1000L;

		// Check if a new day has started (18:00 UTC)
		long timeSince = now - tracking.getLastSimulationTime();
		if (timeSince >= dayInMs) {
			int nextMatchday = Math.min(25, tracking.getCurrentMatchday() + 1);
			tracking.setCurrentMatchday(nextMatchday);
			tracking.setLastSimulationTime(now);
			gameStateTrackingRepository.save(tracking);
		}
	}

	/**
	 * Verarbeitet Saison-Ende Boni und entfernt Sponsoren - Klassenerhalt-Bonus für
	 * Platz 1-8 - Titel-Bonus für Platz 1-2 - Entfernt alle Sponsoren
	 */
	@Transactional
	private void processSeasonEndBonuses() {
		System.out.println("[RepositoryService] 💰 Verarbeite Saison-Ende Boni...");

		List<League> allLeagues = leagueRepository.findAll();

		for (League league : allLeagues) {
			List<LeagueStandingsDTO> standings = getLeagueStandingsByLeagueId(league.getId());

			for (LeagueStandingsDTO standing : standings) {
				Long teamId = standing.getTeamId();
				int position = standing.getPosition();

				Team team = teamRepository.findById(teamId).orElse(null);
				if (team == null) {
					continue;
				}

				// Prüfe ob Team einen Sponsor hat
				Sponsor sponsor = sponsorRepository.findByTeamId(teamId).orElse(null);

				// Klassenerhalt-Bonus (Platz 1-8)
				if (position >= 1 && position <= 8 && sponsor != null && sponsor.getSurvivePayout() > 0) {
					team.setBudget(team.getBudgetAsLong() + sponsor.getSurvivePayout());
					System.out.println("[Sponsor] Team " + team.getName() + " (Platz " + position + ") erhält "
							+ sponsor.getSurvivePayout() + "€ Klassenerhalt von " + sponsor.getName());
				}

				// Titel-Bonus (Platz 1-2)
				if (position >= 1 && position <= 2 && sponsor != null && sponsor.getTitlePayout() > 0) {
					team.setBudget(team.getBudgetAsLong() + sponsor.getTitlePayout());
					System.out.println("[Sponsor] Team " + team.getName() + " (Platz " + position + ") erhält "
							+ sponsor.getTitlePayout() + "€ Titelprämie von " + sponsor.getName());
				}

				teamRepository.save(team);

				// Entferne Sponsor
				if (sponsor != null) {
					sponsorRepository.delete(sponsor);
					System.out.println(
							"[Sponsor] Sponsor " + sponsor.getName() + " von Team " + team.getName() + " entfernt");
				}
			}
		}

		System.out.println("[RepositoryService] ✅ Saison-Ende Boni verarbeitet und Sponsoren entfernt");
	}

	/**
	 * Führt Saison-Reset mit Auf- und Abstieg durch! GARANTIERT: Kein Team geht
	 * verloren, alle Ligen haben danach 12 Teams! Jetzt mit Länder-Unterstützung:
	 * Auf-/Abstieg nur innerhalb eines Landes!
	 */
	@Transactional
	private void resetSeasonWithPromotion() {
		System.out.println("[RepositoryService] 🏆 Starte Saison-Reset mit Auf- und Abstieg...");

		// === SCHRITT 1: Altern aller Spieler und Karriereeenden ===
		// Rufe endSeason() für alle Teams auf
		endSeason();
		System.out.println("[RepositoryService] ✅ Alle Teams haben Saison-Ende verarbeitet");

		// === SPONSOR & BONUSZAHLUNGEN ===
		// 1. Entferne alle Sponsoren
		// 2. Zahle Saison-Ende Boni (Klassenerhalt und Titel)
		processSeasonEndBonuses();

		List<League> allLeagues = leagueRepository.findAll();

		// Gruppiere Ligen nach Land
		Map<String, List<League>> leaguesByCountry = new HashMap<>();
		for (League league : allLeagues) {
			String country = league.getCountry() != null ? league.getCountry() : "Unknown";
			leaguesByCountry.putIfAbsent(country, new ArrayList<>());
			leaguesByCountry.get(country).add(league);
		}

		// Führe Auf-/Abstieg für jedes Land separat durch
		for (Map.Entry<String, List<League>> entry : leaguesByCountry.entrySet()) {
			String country = entry.getKey();
			List<League> countryLeagues = entry.getValue();
			System.out.println("[RepositoryService] Führe Auf-/Abstieg für " + country + " durch ("
					+ countryLeagues.size() + " Ligen)...");
			resetSeasonForCountry(country, countryLeagues);
		}

		System.out.println("[RepositoryService] ✅ Saison-Reset abgeschlossen für alle Länder!");
	}

	/**
	 * Führt Auf-/Abstieg für ein spezifisches Land durch
	 */
	private void resetSeasonForCountry(String country, List<League> leagues) {
		// === SCHRITT 1: Sammle ALLE Teams sortiert nach Liga ===
		Map<String, List<Long>> teamsByLeague = new HashMap<>();

		for (League league : leagues) {
			List<LeagueStandingsDTO> standings = getLeagueStandingsByLeagueId(league.getId());
			List<Long> teamIds = new ArrayList<>();
			for (LeagueStandingsDTO team : standings) {
				teamIds.add(team.getTeamId());
			}
			teamsByLeague.put(league.getName(), teamIds);
			System.out.println(
					"[RepositoryService] " + country + " - " + league.getName() + " hat " + teamIds.size() + " Teams");
		}

		// === SCHRITT 2: Berechne neue Team-Verteilung für jede Liga ===
		Map<String, List<Long>> newTeamsByLeague = new HashMap<>();

		List<Long> liga1 = teamsByLeague.getOrDefault("1. Liga", new ArrayList<>());
		List<Long> liga2A = teamsByLeague.getOrDefault("2. Liga A", new ArrayList<>());
		List<Long> liga2B = teamsByLeague.getOrDefault("2. Liga B", new ArrayList<>());
		List<Long> liga3A = teamsByLeague.getOrDefault("3. Liga A", new ArrayList<>());
		List<Long> liga3B = teamsByLeague.getOrDefault("3. Liga B", new ArrayList<>());
		List<Long> liga3C = teamsByLeague.getOrDefault("3. Liga C", new ArrayList<>());
		List<Long> liga3D = teamsByLeague.getOrDefault("3. Liga D", new ArrayList<>());

		// NEUE LIGA 1: Top 8 von Liga 1 + Top 2 von Liga 2A + Top 2 von Liga 2B
		List<Long> newLiga1 = new ArrayList<>();
		for (int i = 0; i < Math.min(8, liga1.size()); i++)
			newLiga1.add(liga1.get(i));
		for (int i = 0; i < Math.min(2, liga2A.size()); i++)
			newLiga1.add(liga2A.get(i));
		for (int i = 0; i < Math.min(2, liga2B.size()); i++)
			newLiga1.add(liga2B.get(i));
		newTeamsByLeague.put("1. Liga", newLiga1);

		// NEUE LIGA 2A: Platz 3-8 von Liga 2A + Platz 9-10 von Liga 1 + Top 2 von Liga
		// 3A + Top 2 von Liga 3B
		List<Long> newLiga2A = new ArrayList<>();
		for (int i = 2; i < Math.min(8, liga2A.size()); i++)
			newLiga2A.add(liga2A.get(i));
		for (int i = 8; i < Math.min(10, liga1.size()); i++)
			newLiga2A.add(liga1.get(i));
		for (int i = 0; i < Math.min(2, liga3A.size()); i++)
			newLiga2A.add(liga3A.get(i));
		for (int i = 0; i < Math.min(2, liga3B.size()); i++)
			newLiga2A.add(liga3B.get(i));
		newTeamsByLeague.put("2. Liga A", newLiga2A);

		// NEUE LIGA 2B: Platz 3-8 von Liga 2B + Platz 11-12 von Liga 1 + Top 2 von Liga
		// 3C + Top 2 von Liga 3D
		List<Long> newLiga2B = new ArrayList<>();
		for (int i = 2; i < Math.min(8, liga2B.size()); i++)
			newLiga2B.add(liga2B.get(i));
		for (int i = 10; i < Math.min(12, liga1.size()); i++)
			newLiga2B.add(liga1.get(i));
		for (int i = 0; i < Math.min(2, liga3C.size()); i++)
			newLiga2B.add(liga3C.get(i));
		for (int i = 0; i < Math.min(2, liga3D.size()); i++)
			newLiga2B.add(liga3D.get(i));
		newTeamsByLeague.put("2. Liga B", newLiga2B);

		// NEUE LIGA 3A: Platz 3-12 von Liga 3A (alle bleiben) + Platz 9-10 von Liga 2A
		List<Long> newLiga3A = new ArrayList<>();
		for (int i = 2; i < liga3A.size(); i++)
			newLiga3A.add(liga3A.get(i));
		for (int i = 8; i < Math.min(10, liga2A.size()); i++)
			newLiga3A.add(liga2A.get(i));
		newTeamsByLeague.put("3. Liga A", newLiga3A);

		// NEUE LIGA 3B: Platz 3-12 von Liga 3B (alle bleiben) + Platz 11-12 von Liga 2A
		List<Long> newLiga3B = new ArrayList<>();
		for (int i = 2; i < liga3B.size(); i++)
			newLiga3B.add(liga3B.get(i));
		for (int i = 10; i < Math.min(12, liga2A.size()); i++)
			newLiga3B.add(liga2A.get(i));
		newTeamsByLeague.put("3. Liga B", newLiga3B);

		// NEUE LIGA 3C: Platz 3-12 von Liga 3C (alle bleiben) + Platz 9-10 von Liga 2B
		List<Long> newLiga3C = new ArrayList<>();
		for (int i = 2; i < liga3C.size(); i++)
			newLiga3C.add(liga3C.get(i));
		for (int i = 8; i < Math.min(10, liga2B.size()); i++)
			newLiga3C.add(liga2B.get(i));
		newTeamsByLeague.put("3. Liga C", newLiga3C);

		// NEUE LIGA 3D: Platz 3-12 von Liga 3D (alle bleiben) + Platz 11-12 von Liga 2B
		List<Long> newLiga3D = new ArrayList<>();
		for (int i = 2; i < liga3D.size(); i++)
			newLiga3D.add(liga3D.get(i));
		for (int i = 10; i < Math.min(12, liga2B.size()); i++)
			newLiga3D.add(liga2B.get(i));
		newTeamsByLeague.put("3. Liga D", newLiga3D);

		// Debug
		for (Map.Entry<String, List<Long>> entry : newTeamsByLeague.entrySet()) {
			System.out.println("[RepositoryService] " + country + " - " + entry.getKey() + " → "
					+ entry.getValue().size() + " Teams");
		}

		// === SCHRITT 3: Weise Teams zu Ligen zu ===
		for (League league : leagues) {
			List<Long> newTeams = newTeamsByLeague.get(league.getName());
			if (newTeams != null) {
				fillLeagueWithTeams(league, newTeams);
			}
		}

		// === SCHRITT 4: Regeneriere Schedules ===
		for (League league : leagues) {
			List<Schedule> schedules = scheduleRepository.findAll();
			for (Schedule s : schedules) {
				if (s.getLeagueId().equals(league.getId())) {
					List<Matchday> matchdays = matchdayRepository.findAll();
					List<Matchday> toDelete = matchdays.stream().filter(md -> md.getLeagueId().equals(league.getId()))
							.collect(java.util.stream.Collectors.toList());
					matchdayRepository.deleteAll(toDelete);
					scheduleRepository.delete(s);
				}
			}
			createSchedule(league);
			updateSchedule(league);
		}
	}

	/**
	 * Verschiebt ein Team zu einer neuen Liga
	 */
	private void moveTeamToLeague(Long teamId, League targetLeague) {
		removeTeamFromAllLeagues(teamId);
		assignTeamToLeague(teamId, targetLeague);
	}

	/**
	 * Füllt eine Liga mit Teams - garantiert dass alle Slots gefüllt sind
	 */
	private void fillLeagueWithTeams(League league, List<Long> teamIds) {
		// Leere alle Slots
		for (LeagueSlot slot : league.getSlots()) {
			slot.setTeamId(null);
			leagueSlotRepository.save(slot);
		}

		// Fülle Slots mit Teams
		List<LeagueSlot> slots = new ArrayList<>(league.getSlots());
		for (int i = 0; i < Math.min(slots.size(), teamIds.size()); i++) {
			slots.get(i).setTeamId(teamIds.get(i));
			leagueSlotRepository.save(slots.get(i));
		}

		System.out.println("[RepositoryService] Liga " + league.getName() + " gefüllt mit "
				+ Math.min(slots.size(), teamIds.size()) + " Teams");
	}

	/**
	 * Entfernt ein Team aus allen Ligen
	 */
	private void removeTeamFromAllLeagues(Long teamId) {
		List<League> allLeagues = leagueRepository.findAll();
		for (League league : allLeagues) {
			for (LeagueSlot slot : league.getSlots()) {
				if (slot.getTeamId() != null && slot.getTeamId().equals(teamId)) {
					slot.setTeamId(null);
					leagueSlotRepository.save(slot);
					return;
				}
			}
		}
	}

	/**
	 * Weist ein Team einem freien Slot in einer Liga zu
	 */
	private void assignTeamToLeague(Long teamId, League league) {
		for (LeagueSlot slot : league.getSlots()) {
			if (slot.getTeamId() == null) {
				slot.setTeamId(teamId);
				leagueSlotRepository.save(slot);
				System.out.println("[RepositoryService] Team " + teamId + " → Liga " + league.getName());
				return;
			}
		}
	}

	// ==================== SCOUTING SYSTEM ====================

	@Autowired
	private ScoutRepository scoutRepository;

	@Autowired
	private YouthPlayerRepository youthPlayerRepository;

	/**
	 * Startet einen Scout für ein Team in eine Region für X Tage Kostet 50.000€ pro
	 * Tag
	 */
	public void startScout(Long teamId, String region, int days) {
		Team team = teamRepository.findById(teamId).orElse(null);
		if (team == null)
			return;

		long cost = (long) days * 50000;
		if (team.getBudgetAsLong() < cost) {
			throw new IllegalArgumentException("Nicht genug Budget für Scout! Benötigt: " + cost + "€");
		}

		// Ziehe Kosten ab
		team.setBudgetAsLong(team.getBudgetAsLong() - cost);
		teamRepository.save(team);

		// Erstelle Scout
		Scout scout = new Scout(teamId, region, days);
		scoutRepository.save(scout);

		System.out.println("[Scouting] Team " + teamId + " startet Scout in " + region + " für " + days
				+ " Tage (Kosten: " + cost + "€)");
	}

	/**
	 * Liefert den aktiven Scout eines Teams (falls vorhanden)
	 */
	public Scout getActiveScout(Long teamId) {
		return scoutRepository.findByTeamIdAndIsActive(teamId, true).orElse(null);
	}

	/**
	 * Generiert einen Jugenspieler für einen aktiven Scout (täglich um 15 Uhr) mit
	 * Ländern aus der Region des Scouts
	 */
	public YouthPlayer generateScoutedPlayer(Long scoutId) {
		Scout scout = scoutRepository.findById(scoutId).orElse(null);
		if (scout == null || !scout.isActive())
			return null;

		// Generiere Spieler mit regionsbezogenem Land
		YouthPlayer player = YouthPlayerGenerator.generateYouthPlayerForRegion(scout.getTeamId(), scoutId,
				scout.getRegion());
		youthPlayerRepository.save(player);

		scout.setLastPlayerGeneratedAt(java.time.Instant.now());
		scoutRepository.save(scout);

		System.out.println("[Scouting] Neuer Jugenspieler gefunden: " + player.getName() + " ("
				+ player.getOverallPotential() + " Pot)");
		return player;
	}

	/**
	 * Verpflichtet einen Jugenspieler zum Kader
	 */
	public void recruitYouthPlayer(Long youthPlayerId) {
		YouthPlayer youth = youthPlayerRepository.findById(youthPlayerId).orElse(null);
		if (youth == null || youth.getAge() < 17) {
			throw new IllegalArgumentException("Nur 17-18 Jährige können zum Kader hinzugefügt werden!");
		}

		Team team = teamRepository.findById(youth.getTeamId()).orElse(null);
		if (team == null)
			return;

		long salary = 100000L / 30; // Pro-Spiel Gehalt für Jugenspieler
		if (team.getBudgetAsLong() < salary) {
			throw new IllegalArgumentException("Nicht genug Budget!");
		}

		// Erstelle echten Spieler aus Jugenspieler
		Player newPlayer = new Player(youth.getName(), youth.getRating(), youth.getOverallPotential(), 0,
				youth.getPosition(), youth.getCountry());
		newPlayer.setTeamId(team.getId());
		newPlayer.setAge(youth.getAge());
		newPlayer.setSalary(salary);
		newPlayer.setContractLength(3);

		// Übertrage alle Skill-Werte vom YouthPlayer
		newPlayer.setPace(youth.getPace());
		newPlayer.setDribbling(youth.getDribbling());
		newPlayer.setBallControl(youth.getBallControl());
		newPlayer.setShooting(youth.getShooting());
		newPlayer.setTackling(youth.getTackling());
		newPlayer.setSliding(youth.getSliding());
		newPlayer.setHeading(youth.getHeading());
		newPlayer.setCrossing(youth.getCrossing());
		newPlayer.setPassing(youth.getPassing());
		newPlayer.setAwareness(youth.getAwareness());
		newPlayer.setJumping(youth.getJumping());
		newPlayer.setStamina(youth.getStamina());
		newPlayer.setStrength(youth.getStrength());

		// Übertrage alle Potential-Werte vom YouthPlayer
		newPlayer.setPacePotential(youth.getPacePotential());
		newPlayer.setDribblingPotential(youth.getDribblingPotential());
		newPlayer.setBallControlPotential(youth.getBallControlPotential());
		newPlayer.setShootingPotential(youth.getShootingPotential());
		newPlayer.setTacklingPotential(youth.getTacklingPotential());
		newPlayer.setSlidingPotential(youth.getSlidingPotential());
		newPlayer.setHeadingPotential(youth.getHeadingPotential());
		newPlayer.setCrossingPotential(youth.getCrossingPotential());
		newPlayer.setPassingPotential(youth.getPassingPotential());
		newPlayer.setAwarenessPotential(youth.getAwarenessPotential());
		newPlayer.setJumpingPotential(youth.getJumpingPotential());
		newPlayer.setStaminaPotential(youth.getStaminaPotential());
		newPlayer.setStrengthPotential(youth.getStrengthPotential());

		// Berechne neues Rating und OverallPotential basierend auf den übertragenen
		// Werten
		newPlayer.calculateRating();
		newPlayer.calculateOverallPotential();

		newPlayer.calculateMarketValue();
		playerRepository.save(newPlayer);

		// Ziehe Gehalt ab (erste Saison)
		team.setBudgetAsLong(team.getBudgetAsLong() - salary);
		teamRepository.save(team);

		youth.setRecruited(true);
		youthPlayerRepository.save(youth);

		System.out.println("[Scouting] Jugenspieler " + youth.getName() + " zum Kader hinzugefügt!");
	}

	/**
	 * Verpflichtet einen Jugenspieler (15-16 Jahre) zur Akademie Kostenlos - der
	 * Spieler kommt in die Akademie
	 */
	public void recruitToAcademy(Long youthPlayerId) {
		YouthPlayer youth = youthPlayerRepository.findById(youthPlayerId).orElse(null);
		if (youth == null || youth.getAge() > 16) {
			throw new IllegalArgumentException("Nur 15-16 Jährige können zur Akademie hinzugefügt werden!");
		}

		// Füge zur Akademie hinzu - KOSTENLOS
		youth.setInAcademy(true);
		youth.setRecruited(true); // Markiert als verpflichtet
		youthPlayerRepository.save(youth);

		System.out.println("[Scouting] Jugenspieler " + youth.getName() + " zur Akademie hinzugefügt (kostenlos)!");
	}

	/**
	 * Lehnt einen Jugenspieler ab und löscht ihn
	 */
	public void rejectYouthPlayer(Long youthPlayerId) {
		YouthPlayer youth = youthPlayerRepository.findById(youthPlayerId).orElse(null);
		if (youth == null) {
			throw new IllegalArgumentException("Spieler nicht gefunden!");
		}

		String playerName = youth.getName();
		youthPlayerRepository.deleteById(youthPlayerId);

		System.out.println("[Scouting] Jugenspieler " + playerName + " abgelehnt und gelöscht!");
	}

	/**
	 * Gibt alle Jugenspieler in der Akademie für ein Team zurück
	 */
	public List<YouthPlayer> getYouthAcademy(Long teamId) {
		return youthPlayerRepository.findByTeamIdAndIsInAcademyTrue(teamId);
	}

	/**
	 * Gibt alle gescouteten Spieler für ein Team zurück (zum Anzeigen im
	 * Scout-Interface) Nur unverpflichtete Spieler werden angezeigt
	 */
	public List<YouthPlayer> getScoutedPlayers(Long teamId) {
		return youthPlayerRepository.findByTeamId(teamId).stream().filter(p -> !p.isRecruited()) // Nur unverpflichtete
				.toList();
	}

	/**
	 * Trainiert alle Akademie-Spieler eines Teams nach einem Spieltag Jeder Skill
	 * hat 15% Chance um 1 zu steigen
	 */
	public void trainAcademyPlayers(Long teamId) {
		try {
			List<YouthPlayer> academyPlayers = youthPlayerRepository.findByTeamIdAndIsInAcademyTrue(teamId);

			Random rand = new Random();
			int trainedCount = 0;
			int skillsImproved = 0;

			for (YouthPlayer player : academyPlayers) {
				int skillsBefore = getAcademySkillCount(player);

				// Trainiere den Spieler
				player.trainInAcademy(rand);

				int skillsAfter = getAcademySkillCount(player);

				youthPlayerRepository.save(player);

				trainedCount++;
				skillsImproved += (skillsAfter - skillsBefore);
			}

			if (trainedCount > 0) {
				System.out.println("[Academy Training] Team " + teamId + ": " + trainedCount
						+ " Akademie-Spieler trainiert, " + skillsImproved + " Skills verbessert");
			}
		} catch (Exception e) {
			System.err.println("[Academy Training] Fehler beim Training für Team " + teamId + ": " + e.getMessage());
		}
	}

	/**
	 * Zählt die Summe aller Skills eines Akademie-Spielers
	 */
	private int getAcademySkillCount(YouthPlayer player) {
		return player.getPace() + player.getDribbling() + player.getBallControl() + player.getShooting()
				+ player.getTackling() + player.getSliding() + player.getHeading() + player.getCrossing()
				+ player.getPassing() + player.getAwareness() + player.getJumping() + player.getStamina()
				+ player.getStrength();
	}

	/**
	 * Prozessiert das Ende einer Saison: - Alle Spieler altern um 1 Jahr - Prüfe
	 * Spieler >= 35 Jahren auf Karriereende - Akademie-Spieler, die 17 werden,
	 * kommen automatisch zum Kader
	 */
	public void endSeason() {
		try {

			Random rand = new Random();

			// SCHRITT 1: Alle Spieler um 1 Jahr altern
			List<Player> players = playerRepository.findAll();
			for (Player player : players) {
				player.setAge(player.getAge() + 1);
			}
			playerRepository.saveAll(players);
			System.out.println("[EndSeason] " + players.size() + " Spieler um 1 Jahr gealtert");

			// SCHRITT 2: Prüfe Spieler >= 35 Jahren auf Karriereende
			List<Player> playersToRemove = new ArrayList<>();
			for (Player player : players) {
				if (player.getAge() >= 35) {
					int retirementChance = 0;
					switch (player.getAge()) {
					case 35:
						retirementChance = 10;
						break;
					case 36:
						retirementChance = 20;
						break;
					case 37:
						retirementChance = 30;
						break;
					case 38:
						retirementChance = 50;
						break;
					case 39:
						retirementChance = 70;
						break;
					default:
						retirementChance = 100;
						break; // 40+
					}

					if (rand.nextInt(100) < retirementChance) {
						playersToRemove.add(player);
						System.out.println("[EndSeason] Spieler " + player.getName() + " (Alter " + player.getAge()
								+ ") beendet Karriere!");
					}
				}
			}

			// Entferne Spieler mit Karriereende
			if (!playersToRemove.isEmpty()) {
				playerRepository.deleteAll(playersToRemove);
				System.out.println("[EndSeason] " + playersToRemove.size() + " Spieler beendeten Karriere");
			}

			// SCHRITT 3: Verarbeite Jugenspieler
			List<YouthPlayer> youthPlayers = youthPlayerRepository.findAll();

			// Erst alle altern
			for (YouthPlayer youth : youthPlayers) {
				youth.setAge(youth.getAge() + 1);
			}
			youthPlayerRepository.saveAll(youthPlayers);
			System.out.println("[EndSeason] " + youthPlayers.size() + " Jugenspieler um 1 Jahr gealtert");

			// Dann Akademie-Spieler die 17 werden zum Kader befördern
			List<YouthPlayer> toPromote = youthPlayers.stream()
					.filter(y -> y.getAge() == 17 && y.isInAcademy() && !y.isRecruited()).toList();

			for (YouthPlayer youth : toPromote) {
				long salary = 100000L / 30; // Pro-Spiel Gehalt
				// Erstelle Spieler
				Player newPlayer = new Player(youth.getName(), youth.getRating(), youth.getOverallPotential(), 0,
						youth.getPosition(), youth.getCountry());
				newPlayer.setTeamId(youth.getTeamId());
				newPlayer.setAge(youth.getAge());
				newPlayer.setSalary(salary);
				newPlayer.setContractLength(3);

				// Übertrage Skills und Potentiale
				newPlayer.setPace(youth.getPace());
				newPlayer.setDribbling(youth.getDribbling());
				newPlayer.setBallControl(youth.getBallControl());
				newPlayer.setShooting(youth.getShooting());
				newPlayer.setTackling(youth.getTackling());
				newPlayer.setSliding(youth.getSliding());
				newPlayer.setHeading(youth.getHeading());
				newPlayer.setCrossing(youth.getCrossing());
				newPlayer.setPassing(youth.getPassing());
				newPlayer.setAwareness(youth.getAwareness());
				newPlayer.setJumping(youth.getJumping());
				newPlayer.setStamina(youth.getStamina());
				newPlayer.setStrength(youth.getStrength());

				newPlayer.setPacePotential(youth.getPacePotential());
				newPlayer.setDribblingPotential(youth.getDribblingPotential());
				newPlayer.setBallControlPotential(youth.getBallControlPotential());
				newPlayer.setShootingPotential(youth.getShootingPotential());
				newPlayer.setTacklingPotential(youth.getTacklingPotential());
				newPlayer.setSlidingPotential(youth.getSlidingPotential());
				newPlayer.setHeadingPotential(youth.getHeadingPotential());
				newPlayer.setCrossingPotential(youth.getCrossingPotential());
				newPlayer.setPassingPotential(youth.getPassingPotential());
				newPlayer.setAwarenessPotential(youth.getAwarenessPotential());
				newPlayer.setJumpingPotential(youth.getJumpingPotential());
				newPlayer.setStaminaPotential(youth.getStaminaPotential());
				newPlayer.setStrengthPotential(youth.getStrengthPotential());

				newPlayer.calculateRating();
				newPlayer.calculateOverallPotential();
				newPlayer.calculateMarketValue();
				playerRepository.save(newPlayer);

				// Markiere Youth-Spieler als verpflichtet
				youth.setRecruited(true);
				youth.setInAcademy(false);

				System.out.println("[EndSeason] Akademie-Spieler " + youth.getName()
						+ " (jetzt 17 Jahre) automatisch zum Kader verpflichtet!");
			}

			// Speichere Youth-Spieler
			youthPlayerRepository.saveAll(youthPlayers);

		} catch (Exception e) {
			System.err.println("[EndSeason] Fehler beim Verarbeiten des Saison-Endes: " + e.getMessage());
			e.printStackTrace();
		}
	}
}
