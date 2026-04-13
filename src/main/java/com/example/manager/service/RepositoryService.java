package com.example.manager.service;

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
import com.example.manager.model.StadiumPart;
import com.example.manager.model.Team;
import com.example.manager.model.User;
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
import com.example.manager.repository.StadiumPartRepository;
import com.example.manager.repository.TeamRepository;
import com.example.manager.repository.UserRepository;
import com.example.manager.util.PlayerNameGenerator;
import com.example.manager.util.TeamNameGenerator;

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
	private StadiumPartRepository stadiumPartRepository;

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
	 * Registriert einen User mit Ligawahl (neue Version)
	 */
	@Transactional
	public String registerUserWithLeague(String username, String password, String teamName, Long leagueId) {
		if (userRepository.findByUsername(username).isPresent()) {
			throw new IllegalArgumentException("user exists");
		}

		// Erstelle Team
		Team team = new Team(teamName, 100000);
		team = saveTeam(team);

		// Finde die Liga und tausche einen zufälligen CPU-Team-Slot
		League league = leagueRepository.findById(leagueId).orElse(null);
		if (league == null) {
			throw new IllegalArgumentException("league not found");
		}

		// Finde zufälligen gefüllten Slot (CPU-Team) und ersetze ihn mit dem neuen
		// User-Team
		List<LeagueSlot> filledSlots = new ArrayList<>();
		for (LeagueSlot slot : league.getSlots()) {
			if (slot.getTeamId() != null) {
				filledSlots.add(slot);
			}
		}

		// Wenn keine gefüllten Slots vorhanden, füge zum ersten leeren hinzu
		if (!filledSlots.isEmpty()) {
			LeagueSlot randomSlot = filledSlots.get(random.nextInt(filledSlots.size()));
			Long oldTeamId = randomSlot.getTeamId();
			randomSlot.setTeamId(team.getId());
			leagueSlotRepository.save(randomSlot);
			// Alte Team löschen (war CPU-Team)
			if (oldTeamId != null) {
				deleteTeamCascade(oldTeamId);
			}
			// WICHTIG: Regeneriere den Spielplan nach dem Austausch des Teams!
			// Das stellt sicher, dass alle Matches die neuen Team IDs haben
			updateSchedule(league);
		} else {
			// Leeren Slot hinzufügen
			LeagueSlot emptySlot = league.addTeam(team);
			if (emptySlot != null) {
				leagueSlotRepository.save(emptySlot);
			}
		}

		// Erstelle User mit Liga-Zuordnung
		User user = new User(username, passwordEncoder.encode(password), team.getId(), leagueId);
		userRepository.save(user);

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

			// Delete stadium parts
			stadiumPartRepository.deleteByTeamId(teamId);

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
				String[] positions = { "GK", "GK", "DEF", "DEF", "DEF", "DEF", "DEF", "DEF", "MID", "MID", "MID", "MID",
						"MID", "MID", "MID", "FWD", "FWD", "FWD" };

				// Generate 18 random fictional players with random names and countries
				Random rand = new Random();
				for (int i = 0; i < 18; i++) {
					String[] playerData = PlayerNameGenerator.generatePlayerNameAndCountry();

					// Generate realistic rating (50-90)
					int rating = 50 + rand.nextInt(41);

					int age = 18 + rand.nextInt(17); // Age 18-34
					int potential = generatePotential(rating, age);
					// Generate potential: Rating -5 to +20, max 99

					Player p = new Player(playerData[0], // Random name
							rating, potential, (int) (Math.random() * 20) - 10, // Form -10 to +10
							positions[i], playerData[1] // Random country
					);

					// Set realistic player attributes
					long salary = 50000L + (long) rating * rating * 100; // Salary based on rating
					long contractYears = 1 + rand.nextInt(5); // Contract 1-5 years
					long contractEndDate = System.currentTimeMillis() + (contractYears * 30L * 24L * 60L * 60L * 1000L);

					p.setAge(age);
					p.setSalary(salary);
					p.setContractEndDate(contractEndDate);
					p.setTeamId(saved.getId());

					// Calculate market value based on age, rating, and contract
					p.calculateMarketValue();
					playerRepository.save(p);
				}

				// Initialize stadium parts
				for (int i = 0; i < 30; i++) {
					StadiumPart part = new StadiumPart(saved.getId(), i, false, null);
					stadiumPartRepository.save(part);
				}

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

	private int generatePotential(int rating, int age) {
		Random rand = new Random();
		int potential = 0;

		// Ensure the bound is always positive for rand.nextInt()
		if (rating < 70) {
			// Higher potential for lower-rated players
			int bound = Math.max(1, 45 - age);
			potential = Math.min(99, rating + rand.nextInt(bound));
		} else if (rating < 80) {
			// Medium potential for mid-rated players
			int bound = Math.max(1, 40 - age);
			potential = Math.min(99, rating - 3 + rand.nextInt(bound));
		} else {
			// Lower potential for high-rated players
			int bound = Math.max(1, 34 - age);
			potential = Math.min(99, rating - 5 + rand.nextInt(bound));
		}

		// Ensure potential is at least as high as rating
		potential = Math.max(potential, rating);
		return potential;
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

	// Stadium management
	public List<StadiumPart> getStadiumParts(Long teamId) {
		return stadiumPartRepository.findByTeamId(teamId);
	}

	@Transactional
	public StadiumPart saveStadiumPart(StadiumPart part) {
		return stadiumPartRepository.save(part);
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
	 * Initialisiert die 7 Standard-Ligen mit CPU-Teams: - 1 x 1. Liga (12 Teams) -
	 * 2 x 2. Liga (je 12 Teams) - 4 x 3. Liga (je 12 Teams)
	 */
	@Transactional
	public void initializeLigues() {
		List<League> existing = leagueRepository.findAll();
		if (!existing.isEmpty()) {
			System.out.println("[RepositoryService] Ligen existieren bereits, Initialisierung übersprungen");
			return;
		}

		// 1. Liga
		createLeagueWithCPUTeams(1, "1. Liga", "1. Liga", 12);

		// 2. Ligen
		createLeagueWithCPUTeams(2, "2. Liga A", "2. Liga A", 12);
		createLeagueWithCPUTeams(2, "2. Liga B", "2. Liga B", 12);

		// 3. Ligen
		createLeagueWithCPUTeams(3, "3. Liga A", "3. Liga A", 12);
		createLeagueWithCPUTeams(3, "3. Liga B", "3. Liga B", 12);
		createLeagueWithCPUTeams(3, "3. Liga C", "3. Liga C", 12);
		createLeagueWithCPUTeams(3, "3. Liga D", "3. Liga D", 12);

		System.out.println("[RepositoryService] 7 Ligen mit insgesamt 84 CPU-Teams initialisiert");
	}

	/**
	 * Erstellt eine Liga mit den angegebenen CPU-Teams
	 */
	private void createLeagueWithCPUTeams(int division, String name, String divisionLabel, int numTeams) {
		League league = new League(name);
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
	 * Liga: 60-95 2. Liga: 55-80 3. Liga: 50-73
	 */
	private void initializeTeamPlayers(Team team, int division) {
		String[] positions = { "GK", "GK", "DEF", "DEF", "DEF", "DEF", "DEF", "DEF", "MID", "MID", "MID", "MID", "MID",
				"MID", "MID", "FWD", "FWD", "FWD" };

		// Bestimme Stärke-Range basierend auf Division
		int minRating, maxRating;
		switch (division) {
		case 1:
			minRating = 60;
			maxRating = 95;
			break;
		case 2:
			minRating = 55;
			maxRating = 80;
			break;
		case 3:
			minRating = 50;
			maxRating = 73;
			break;
		default:
			minRating = 50;
			maxRating = 73;
			break;
		}

		Random rand = new Random();
		for (int i = 0; i < 18; i++) {
			String[] playerData = PlayerNameGenerator.generatePlayerNameAndCountry();
			int rating = minRating + rand.nextInt(maxRating - minRating + 1);
			int age = 18 + rand.nextInt(17);
			int potential = generatePotential(rating, age);

			Player p = new Player(playerData[0], rating, potential, (int) (Math.random() * 20) - 10, positions[i],
					playerData[1]);

			long salary = 50000L + (long) rating * rating * 100;
			long contractYears = 1 + rand.nextInt(5);
			long contractEndDate = System.currentTimeMillis() + (contractYears * 30L * 24L * 60L * 60L * 1000L);

			p.setAge(age);
			p.setSalary(salary);
			p.setContractEndDate(contractEndDate);
			p.setTeamId(team.getId());
			p.calculateMarketValue();
			playerRepository.save(p);
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
		for (int i = 0; i < 30; i++) {
			StadiumPart part = new StadiumPart(team.getId(), i, false, null);
			stadiumPartRepository.save(part);
		}
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

			LeagueInfoDTO dto = new LeagueInfoDTO(league.getId(), league.getName(), league.getDivision(),
					league.getDivisionLabel(), filledSlots, totalSlots);
			result.add(dto);
		}

		// Sortiere nach Division, dann nach Name
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

		// Get team names
		Team homeTeam = teamRepository.findById(homeTeamId).orElse(null);
		Team awayTeam = teamRepository.findById(awayTeamId).orElse(null);
		String homeTeamName = homeTeam != null ? homeTeam.getName() : "Unknown";
		String awayTeamName = awayTeam != null ? awayTeam.getName() : "Unknown";

		return new MatchSimulationResultDTO(matchId, homeTeamId, awayTeamId, homeTeamName, awayTeamName, homeGoals,
				awayGoals, result);
	}

	/**
	 * Generiert Goal Events für den Spielbericht
	 */
	private void generateGoalEvents(Long matchId, Long homeTeamId, Long awayTeamId, int homeGoals, int awayGoals) {
		// Lösche alte Events
		matchEventRepository.deleteByMatchId(matchId);

		List<LineupSlot> homeLineup = lineupRepository.findByTeamIdAndFormationId(homeTeamId, "4-4-2");
		List<LineupSlot> awayLineup = lineupRepository.findByTeamIdAndFormationId(awayTeamId, "4-4-2");

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

		return new MatchReportDTO(matchId, homeTeamId, awayTeamId, homeTeamName, awayTeamName, match.getHomeGoals(),
				match.getAwayGoals(), match.getHomeGoals() > match.getAwayGoals() ? "home"
						: match.getAwayGoals() > match.getHomeGoals() ? "away" : "draw",
				eventDTOs);
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
	 * Simuliert alle ausstehenden Spiele des aktuellen Spieltags (für ALLE Ligen)
	 * und erhöht dann den aktuellen Spieltag
	 */
	@Transactional
	public Map<String, Object> advanceToNextMatchday() {
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
	 * Führt Saison-Reset mit Auf- und Abstieg durch!
	 * GARANTIERT: Kein Team geht verloren, alle Ligen haben danach 12 Teams!
	 */
	@Transactional
	private void resetSeasonWithPromotion() {
		System.out.println("[RepositoryService] 🏆 Starte Saison-Reset mit Auf- und Abstieg...");
		
		List<League> allLeagues = leagueRepository.findAll();
		
		// === SCHRITT 1: Sammle ALLE Teams sortiert nach Liga ===
		Map<String, List<Long>> teamsByLeague = new HashMap<>();
		
		for (League league : allLeagues) {
			List<LeagueStandingsDTO> standings = getLeagueStandingsByLeagueId(league.getId());
			List<Long> teamIds = new ArrayList<>();
			for (LeagueStandingsDTO team : standings) {
				teamIds.add(team.getTeamId());
			}
			teamsByLeague.put(league.getName(), teamIds);
			System.out.println("[RepositoryService] " + league.getName() + " hat " + teamIds.size() + " Teams");
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
		for (int i = 0; i < Math.min(8, liga1.size()); i++) newLiga1.add(liga1.get(i));
		for (int i = 0; i < Math.min(2, liga2A.size()); i++) newLiga1.add(liga2A.get(i));
		for (int i = 0; i < Math.min(2, liga2B.size()); i++) newLiga1.add(liga2B.get(i));
		newTeamsByLeague.put("1. Liga", newLiga1);
		
		// NEUE LIGA 2A: Platz 3-8 von Liga 2A + Platz 9-10 von Liga 1 + Top 2 von Liga 3A + Top 2 von Liga 3B
		List<Long> newLiga2A = new ArrayList<>();
		for (int i = 2; i < Math.min(8, liga2A.size()); i++) newLiga2A.add(liga2A.get(i));
		for (int i = 8; i < Math.min(10, liga1.size()); i++) newLiga2A.add(liga1.get(i));
		for (int i = 0; i < Math.min(2, liga3A.size()); i++) newLiga2A.add(liga3A.get(i));
		for (int i = 0; i < Math.min(2, liga3B.size()); i++) newLiga2A.add(liga3B.get(i));
		newTeamsByLeague.put("2. Liga A", newLiga2A);
		
		// NEUE LIGA 2B: Platz 3-8 von Liga 2B + Platz 11-12 von Liga 1 + Top 2 von Liga 3C + Top 2 von Liga 3D
		List<Long> newLiga2B = new ArrayList<>();
		for (int i = 2; i < Math.min(8, liga2B.size()); i++) newLiga2B.add(liga2B.get(i));
		for (int i = 10; i < Math.min(12, liga1.size()); i++) newLiga2B.add(liga1.get(i));
		for (int i = 0; i < Math.min(2, liga3C.size()); i++) newLiga2B.add(liga3C.get(i));
		for (int i = 0; i < Math.min(2, liga3D.size()); i++) newLiga2B.add(liga3D.get(i));
		newTeamsByLeague.put("2. Liga B", newLiga2B);
		
		// NEUE LIGA 3A: Platz 3-12 von Liga 3A (alle bleiben) + Platz 9-10 von Liga 2A
		List<Long> newLiga3A = new ArrayList<>();
		for (int i = 2; i < liga3A.size(); i++) newLiga3A.add(liga3A.get(i));
		for (int i = 8; i < Math.min(10, liga2A.size()); i++) newLiga3A.add(liga2A.get(i));
		newTeamsByLeague.put("3. Liga A", newLiga3A);
		
		// NEUE LIGA 3B: Platz 3-12 von Liga 3B (alle bleiben) + Platz 11-12 von Liga 2A
		List<Long> newLiga3B = new ArrayList<>();
		for (int i = 2; i < liga3B.size(); i++) newLiga3B.add(liga3B.get(i));
		for (int i = 10; i < Math.min(12, liga2A.size()); i++) newLiga3B.add(liga2A.get(i));
		newTeamsByLeague.put("3. Liga B", newLiga3B);
		
		// NEUE LIGA 3C: Platz 3-12 von Liga 3C (alle bleiben) + Platz 9-10 von Liga 2B
		List<Long> newLiga3C = new ArrayList<>();
		for (int i = 2; i < liga3C.size(); i++) newLiga3C.add(liga3C.get(i));
		for (int i = 8; i < Math.min(10, liga2B.size()); i++) newLiga3C.add(liga2B.get(i));
		newTeamsByLeague.put("3. Liga C", newLiga3C);
		
		// NEUE LIGA 3D: Platz 3-12 von Liga 3D (alle bleiben) + Platz 11-12 von Liga 2B
		List<Long> newLiga3D = new ArrayList<>();
		for (int i = 2; i < liga3D.size(); i++) newLiga3D.add(liga3D.get(i));
		for (int i = 10; i < Math.min(12, liga2B.size()); i++) newLiga3D.add(liga2B.get(i));
		newTeamsByLeague.put("3. Liga D", newLiga3D);
		
		// Debug
		for (Map.Entry<String, List<Long>> entry : newTeamsByLeague.entrySet()) {
			System.out.println("[RepositoryService] " + entry.getKey() + " → " + entry.getValue().size() + " Teams");
		}
		
		// === SCHRITT 3: Weise Teams zu Ligen zu ===
		for (League league : allLeagues) {
			List<Long> newTeams = newTeamsByLeague.get(league.getName());
			if (newTeams != null) {
				fillLeagueWithTeams(league, newTeams);
			}
		}
		
		// === SCHRITT 4: Regeneriere Schedules ===
		for (League league : allLeagues) {
			List<Schedule> schedules = scheduleRepository.findAll();
			for (Schedule s : schedules) {
				if (s.getLeagueId().equals(league.getId())) {
					List<Matchday> matchdays = matchdayRepository.findAll();
					List<Matchday> toDelete = matchdays.stream()
						.filter(md -> md.getLeagueId().equals(league.getId()))
						.collect(java.util.stream.Collectors.toList());
					matchdayRepository.deleteAll(toDelete);
					scheduleRepository.delete(s);
				}
			}
			createSchedule(league);
			updateSchedule(league);
		}
		System.out.println("[RepositoryService] ✅ Saison-Reset abgeschlossen!");
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
}
