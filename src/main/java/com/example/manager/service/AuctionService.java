package com.example.manager.service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.manager.model.AuctionBid;
import com.example.manager.model.AuctionPlayer;
import com.example.manager.model.Player;
import com.example.manager.model.Team;
import com.example.manager.model.TransferHistory;
import com.example.manager.repository.AuctionBidRepository;
import com.example.manager.repository.AuctionPlayerRepository;
import com.example.manager.repository.PlayerRepository;
import com.example.manager.repository.TeamRepository;
import com.example.manager.repository.TransferHistoryRepository;

/**
 * Service for managing daily auctions. - 7 new players appear every day at
 * 22:00 (server time) - Players are available for 24 hours - After 24 hours,
 * highest bidder wins and can negotiate contract
 */
@Service
public class AuctionService {

	@Autowired
	private AuctionPlayerRepository auctionPlayerRepository;

	@Autowired
	private AuctionBidRepository auctionBidRepository;

	@Autowired
	private PlayerRepository playerRepository;

	@Autowired
	private TeamRepository teamRepository;

	@Autowired
	private TransferHistoryRepository transferHistoryRepository;

	private static final int PLAYERS_PER_AUCTION = 7;
	private static final int AUCTION_DURATION_HOURS = 24;
	private static final int AUCTION_END_HOUR = 16; // 16:00 (4 PM) - Auktionen enden immer um 16 Uhr
	private final Random random = new Random();

	/**
	 * Creates 7 new auction players. Auctions start immediately and end at 20:00 next day.
	 * Generates new players without team for the auction.
	 */
	@Transactional
	public void createDailyAuction() {
		System.out.println("[AuctionService] Creating daily auction with 7 new players...");

		// Generiere 7 neue Spieler OHNE Team für die Auktion
		List<AuctionPlayer> auctionPlayers = new ArrayList<>();

		// Auktion startet SOFORT und endet um 16:00 Uhr am nächsten Tag
		Instant now = Instant.now();
		Instant auctionStart = now; // Sofortiger Start
		Instant auctionEnd = calculateNextAuctionEndTime(now); // Nächste 16:00 Uhr

		for (int i = 0; i < PLAYERS_PER_AUCTION; i++) {
			Player newPlayer = generateAuctionPlayer();
			playerRepository.save(newPlayer);

			// Erstelle AuctionPlayer
			AuctionPlayer auctionPlayer = new AuctionPlayer(newPlayer.getId(), newPlayer.getName(),
					newPlayer.getPosition(), newPlayer.getRating(), newPlayer.getAge(), newPlayer.getMarketValue(),
					newPlayer.getSalary(), newPlayer.getContractLength(), newPlayer.getCountry(), auctionStart,
					auctionEnd, newPlayer.getOverallPotential());
			auctionPlayerRepository.save(auctionPlayer);
			auctionPlayers.add(auctionPlayer);

			System.out.println("[AuctionService] Created new player " + newPlayer.getName() + " for auction (Rating: "
					+ newPlayer.getRating() + ", Market Value: " + newPlayer.getMarketValue() + ")");
		}

		System.out.println("[AuctionService] Daily auction created with " + auctionPlayers.size() + " new players (ends at " + auctionEnd + ")");
	}

	/**
	 * Generates a new player for the auction (without team). Random ratings between
	 * 40-90, random positions.
	 */
	private Player generateAuctionPlayer() {
		String[] positions = { "GK", "DEF", "MID", "FWD" };
		String position = positions[random.nextInt(positions.length)];

		// Generiere zufälliges Rating (40-90)
		int rating = 40 + random.nextInt(51); // 40-90

		String[] playerData = com.example.manager.util.PlayerNameGenerator.generatePlayerNameAndCountry();
		String name = playerData[0];
		String country = playerData[1];

		// Erstelle Spieler ohne Team (teamId = null)
		Player player = new Player(name, rating, rating + random.nextInt(10), random.nextInt(20) - 10, position,
				country);

		// Generiere Skills basierend auf Rating
		int baseSkill = Math.max(1, Math.min(100, rating + random.nextInt(20) - 10));
		player.setPace(Math.max(1, Math.min(100, baseSkill + random.nextInt(20) - 10)));
		player.setDribbling(Math.max(1, Math.min(100, baseSkill + random.nextInt(20) - 10)));
		player.setBallControl(Math.max(1, Math.min(100, baseSkill + random.nextInt(20) - 10)));
		player.setShooting(Math.max(1, Math.min(100, baseSkill + random.nextInt(20) - 10)));
		player.setTackling(Math.max(1, Math.min(100, baseSkill + random.nextInt(20) - 10)));
		player.setSliding(Math.max(1, Math.min(100, baseSkill + random.nextInt(20) - 10)));
		player.setHeading(Math.max(1, Math.min(100, baseSkill + random.nextInt(20) - 10)));
		player.setCrossing(Math.max(1, Math.min(100, baseSkill + random.nextInt(20) - 10)));
		player.setPassing(Math.max(1, Math.min(100, baseSkill + random.nextInt(20) - 10)));
		player.setAwareness(Math.max(1, Math.min(100, baseSkill + random.nextInt(20) - 10)));
		player.setJumping(Math.max(1, Math.min(100, baseSkill + random.nextInt(20) - 10)));
		player.setStamina(Math.max(1, Math.min(100, baseSkill + random.nextInt(20) - 10)));
		player.setStrength(Math.max(1, Math.min(100, baseSkill + random.nextInt(20) - 10)));

		int age = 20 + random.nextInt(16); // Age 20-35

		// Gehalt stark abhängig vom Rating (exponentiell)
		// Rating 40: ~30K - 50K
		// Rating 50: ~50K - 100K
		// Rating 60: ~100K - 200K
		// Rating 70: ~250K - 400K
		// Rating 80: ~500K - 800K
		// Rating 90: ~1M - 2M
		long baseSalary = (long) (Math.pow(rating, 2.5) * 1.2);
		long salary = baseSalary * age / 10;
		salary = Math.max(20000, salary); // Mindestens 20K

		int contractLength = 1 + random.nextInt(4); // Contract 1-4 seasons

		player.setAge(age);
		player.setSalary(salary);
		player.setContractLength(contractLength);
		player.setTeamId(null); // WICHTIG: Kein Team
		player.calculateMarketValue();

		return player;
	}

	/**
	 * Calculates the next auction start time (16:00). If current time is already
	 * past 16:00, returns next day's 16:00.
	 */
	private Instant calculateNextAuctionStartTime(Instant now) {
		ZonedDateTime zdt = now.atZone(ZoneId.systemDefault());
		ZonedDateTime nextAuctionTime = zdt.toLocalDate().atTime(AUCTION_END_HOUR, 0).atZone(ZoneId.systemDefault());

		// Wenn die Zeit bereits vorbei ist, nächster Tag
		if (zdt.isAfter(nextAuctionTime)) {
			nextAuctionTime = nextAuctionTime.plusDays(1);
		}

		return nextAuctionTime.toInstant();
	}

	/**
	 * Calculates the next auction end time (16:00). If current time is before 16:00,
	 * returns today's 16:00. Otherwise returns next day's 16:00.
	 */
	private Instant calculateNextAuctionEndTime(Instant now) {
		ZonedDateTime zdt = now.atZone(ZoneId.systemDefault());
		ZonedDateTime nextEndTime = zdt.toLocalDate().atTime(AUCTION_END_HOUR, 0).atZone(ZoneId.systemDefault());

		// Wenn die aktuelle Zeit bereits nach 16:00 ist, nächster Tag
		if (zdt.isAfter(nextEndTime) || zdt.equals(nextEndTime)) {
			nextEndTime = nextEndTime.plusDays(1);
		}

		return nextEndTime.toInstant();
	}

	/**
	 * Places a bid on an auction player. Bid amount must be at least the market
	 * value. Multiple teams can bid any amount >= market value.
	 */
	@Transactional
	public Map<String, Object> placeBid(Long auctionPlayerId, Long biddingTeamId, Long bidAmount) {
		Map<String, Object> result = new HashMap<>();

		// Finde AuctionPlayer
		AuctionPlayer auctionPlayer = auctionPlayerRepository.findById(auctionPlayerId).orElse(null);
		if (auctionPlayer == null) {
			result.put("success", false);
			result.put("message", "Auktion nicht gefunden");
			return result;
		}

		// Prüfe ob Auktion aktiv ist
		if (!"active".equals(auctionPlayer.getAuctionStatus())) {
			result.put("success", false);
			result.put("message", "Auktion ist nicht mehr aktiv");
			return result;
		}

		// Prüfe ob Auktion noch läuft
		Instant now = Instant.now();
		if (now.isAfter(auctionPlayer.getAuctionEndTime())) {
			result.put("success", false);
			result.put("message", "Auktion ist abgelaufen");
			return result;
		}

		// Prüfe Mindestgebot (Marktwert)
		if (bidAmount < auctionPlayer.getMarketValue()) {
			result.put("success", false);
			result.put("message", "Gebot unter Mindestpreis (" + auctionPlayer.getMarketValue() + ")");
			return result;
		}

		// KEINE Prüfung ob Gebot höher als aktuelles höchstes Gebot ist!
		// Jedes Team kann unabhängig bieten, höchstes gewinnt am Ende

		// Lade Bieter-Team
		Team biddingTeam = teamRepository.findById(biddingTeamId).orElse(null);
		if (biddingTeam == null) {
			result.put("success", false);
			result.put("message", "Team nicht gefunden");
			return result;
		}

		// Erstelle neues Gebot
		AuctionBid bid = new AuctionBid(auctionPlayerId, biddingTeamId, biddingTeam.getName(), bidAmount, Instant.now(),
				false);
		auctionBidRepository.save(bid);

		// Aktualisiere höchstes Gebot auf AuctionPlayer (falls dieses Gebot das höchste ist)
		if (auctionPlayer.getHighestBidAmount() == null || bidAmount > auctionPlayer.getHighestBidAmount()) {
			auctionPlayer.setHighestBidAmount(bidAmount);
			auctionPlayer.setHighestBidderTeamId(biddingTeamId);
			auctionPlayer.setHighestBidderTeamName(biddingTeam.getName());
			auctionPlayerRepository.save(auctionPlayer);
		}

		System.out.println("[AuctionService] Bid placed: " + biddingTeam.getName() + " bid " + bidAmount + " on "
				+ auctionPlayer.getPlayerName());

		result.put("success", true);
		result.put("message", "Gebot erfolgreich abgegeben");
		result.put("bidAmount", bidAmount);

		return result;
	}

	/**
	 * Gets all active auction players.
	 */
	public List<Map<String, Object>> getActiveAuctions() {
		try {
			List<AuctionPlayer> activeAuctions = new ArrayList<>();

			// Versuche aus Datenbank zu laden
			try {
				activeAuctions = auctionPlayerRepository.findByAuctionStatus("active");
			} catch (Exception e) {
				System.err.println("[AuctionService] Cannot load from DB: " + e.getMessage());
				activeAuctions = new ArrayList<>();
			}

			if (activeAuctions == null) {
				activeAuctions = new ArrayList<>();
			}

			Instant now = Instant.now();

			List<Map<String, Object>> result = activeAuctions.stream()
					.filter(a -> a != null && now.isBefore(a.getAuctionEndTime())).map(this::auctionPlayerToMap)
					.collect(Collectors.toList());

			System.out.println("[AuctionService] getActiveAuctions returning " + result.size() + " auctions");
			return result;
		} catch (Exception e) {
			System.err.println("[AuctionService] Error getting active auctions: " + e.getMessage());
			e.printStackTrace();
			return new ArrayList<>();
		}
	}

	/**
	 * Gets all active auction players with user's own bids included.
	 */
	public List<Map<String, Object>> getActiveAuctionsWithUserBids(Long teamId) {
		try {
			List<AuctionPlayer> activeAuctions = auctionPlayerRepository.findByAuctionStatus("active");
			if (activeAuctions == null) {
				activeAuctions = new ArrayList<>();
			}

			Instant now = Instant.now();
			List<Map<String, Object>> result = new ArrayList<>();

			for (AuctionPlayer auction : activeAuctions) {
				if (auction != null && now.isBefore(auction.getAuctionEndTime())) {
					Map<String, Object> auctionMap = auctionPlayerToMap(auction);
					
					// Füge eigenes Gebot hinzu wenn teamId vorhanden
					if (teamId != null) {
						List<AuctionBid> userBids = auctionBidRepository
							.findByAuctionPlayerIdOrderByBidAmountDesc(auction.getId())
							.stream()
							.filter(bid -> bid.getBiddingTeamId().equals(teamId))
							.collect(Collectors.toList());
						
						if (!userBids.isEmpty()) {
							// Nehme das höchste eigene Gebot
							AuctionBid highestUserBid = userBids.get(0);
							auctionMap.put("myBidAmount", highestUserBid.getBidAmount());
							auctionMap.put("myBidTime", highestUserBid.getBidTime());
						} else {
							auctionMap.put("myBidAmount", null);
						}
					}
					
					result.add(auctionMap);
				}
			}

			System.out.println("[AuctionService] getActiveAuctionsWithUserBids returning " + result.size() + " auctions");
			return result;
		} catch (Exception e) {
			System.err.println("[AuctionService] Error getting active auctions with user bids: " + e.getMessage());
			e.printStackTrace();
			return new ArrayList<>();
		}
	}

	/**
	 * Gets all bids for an auction player (filtered). Only shows own bids and the
	 * highest bid.
	 */
	public List<Map<String, Object>> getBidsForAuction(Long auctionPlayerId, Long requestingTeamId) {
		List<AuctionBid> bids = auctionBidRepository.findByAuctionPlayerIdOrderByBidAmountDesc(auctionPlayerId);

		if (bids == null || bids.isEmpty()) {
			return new ArrayList<>();
		}

		// Finde das höchste Gebot
		AuctionBid highestBid = bids.isEmpty() ? null : bids.get(0);

		return bids.stream().filter(bid -> {
			// Zeige: 1) Eigene Gebote 2) Das höchste Gebot
			if (requestingTeamId != null && bid.getBiddingTeamId().equals(requestingTeamId)) {
				return true; // Eigenes Gebot immer zeigen
			}
			if (highestBid != null && bid.getId().equals(highestBid.getId())) {
				return true; // Höchstes Gebot immer zeigen
			}
			return false;
		}).map(bid -> {
			Map<String, Object> bidMap = new HashMap<>();
			bidMap.put("id", bid.getId());
			bidMap.put("auctionPlayerId", bid.getAuctionPlayerId());
			bidMap.put("biddingTeamId", bid.getBiddingTeamId());

			// Verstecke Team-Namen bei fremden Geboten
			if (requestingTeamId != null && !bid.getBiddingTeamId().equals(requestingTeamId)) {
				bidMap.put("biddingTeamName", "Anderes Team");
			} else {
				bidMap.put("biddingTeamName", bid.getBiddingTeamName());
			}

			bidMap.put("bidAmount", bid.getBidAmount());
			bidMap.put("bidTime", bid.getBidTime());
			bidMap.put("isCPUBid", bid.isCPUBid());
			return bidMap;
		}).collect(Collectors.toList());
	}

	/**
	 * Gets all bids for an auction player (legacy method).
	 */
	public List<Map<String, Object>> getBidsForAuction(Long auctionPlayerId) {
		return getBidsForAuction(auctionPlayerId, null);
	}

	/**
	 * Completes expired auctions and determines winners. Called every hour or
	 * on-demand.
	 */
	@Transactional
	public void completeExpiredAuctions() {
		System.out.println("[AuctionService] Checking for expired auctions...");

		List<AuctionPlayer> activeAuctions = auctionPlayerRepository.findByAuctionStatus("active");
		Instant now = Instant.now();

		for (AuctionPlayer auction : activeAuctions) {
			if (now.isAfter(auction.getAuctionEndTime())) {
				completeAuction(auction);
			}
		}
	}

	/**
	 * Completes a single auction. If there are bids, winner gets the player and
	 * negotiates contract. If no bids, auction fails.
	 */
	@Transactional
	private void completeAuction(AuctionPlayer auction) {
		System.out.println("[AuctionService] Completing auction for " + auction.getPlayerName());

		List<AuctionBid> bids = auctionBidRepository.findByAuctionPlayerId(auction.getId());

		if (bids.isEmpty()) {
			// Keine Gebote - Auktion fehlgeschlagen
			auction.setAuctionStatus("failed");
			auctionPlayerRepository.save(auction);
			System.out.println("[AuctionService] ❌ Auction for " + auction.getPlayerName() + " failed - no bids");
			return;
		}

		// Finde höchstes Gebot
		AuctionBid winningBid = bids.stream().max(Comparator.comparingLong(AuctionBid::getBidAmount)).orElse(null);

		if (winningBid == null) {
			auction.setAuctionStatus("failed");
			auctionPlayerRepository.save(auction);
			return;
		}

		// Setze Gewinner
		auction.setWinnerTeamId(winningBid.getBiddingTeamId());
		auction.setWinnerTeamName(winningBid.getBiddingTeamName());
		auction.setAuctionStatus("completed");
		auctionPlayerRepository.save(auction);

		System.out.println("[AuctionService] ✅ Auction for " + auction.getPlayerName() + " completed");
		System.out.println("[AuctionService] Winner: " + winningBid.getBiddingTeamName() + " with bid "
				+ winningBid.getBidAmount());
	}

	/**
	 * Processes the contract negotiation after auction completion. Player is
	 * transferred to winning team with new salary and contract length.
	 */
	@Transactional
	public Map<String, Object> completeAuctionNegotiation(Long auctionPlayerId, Long winnerTeamId, Long newSalary,
			Integer contractLength) {
		Map<String, Object> result = new HashMap<>();

		// Finde Auktion
		AuctionPlayer auction = auctionPlayerRepository.findById(auctionPlayerId).orElse(null);
		if (auction == null) {
			result.put("success", false);
			result.put("message", "Auktion nicht gefunden");
			return result;
		}

		// Prüfe ob Auktion abgeschlossen ist
		if (!"completed".equals(auction.getAuctionStatus())) {
			result.put("success", false);
			result.put("message", "Auktion ist nicht abgeschlossen");
			return result;
		}

		// Prüfe ob winnerTeamId korrekt ist
		if (!auction.getWinnerTeamId().equals(winnerTeamId)) {
			result.put("success", false);
			result.put("message", "Nur das gewinnende Team kann die Verhandlung abschließen");
			return result;
		}

		// Lade den Original-Spieler
		Player player = playerRepository.findById(auction.getPlayerId()).orElse(null);
		if (player == null) {
			result.put("success", false);
			result.put("message", "Spieler nicht gefunden");
			return result;
		}

		// Lade Gewinner-Team
		Team winnerTeam = teamRepository.findById(winnerTeamId).orElse(null);
		if (winnerTeam == null) {
			result.put("success", false);
			result.put("message", "Gewinner-Team nicht gefunden");
			return result;
		}

		// Prüfe ob Spieler Gehalt akzeptiert
		if (newSalary < player.getSalary()) {
			result.put("success", false);
			result.put("message", "Spieler lehnt ab - Gehalt zu niedrig");
			return result;
		}

		// Führe Transfer durch
		Long currentTeamId = player.getTeamId();
		Long transferPrice = auction.getHighestBidAmount();

		player.setTeamId(winnerTeamId);
		player.setSalary(newSalary);
		player.setContractLength(contractLength);
		player.calculateMarketValue();
		playerRepository.save(player);

		// Aktualisiere Budgets
		if (currentTeamId != null) {
			Team currentTeam = teamRepository.findById(currentTeamId).orElse(null);
			if (currentTeam != null) {
				currentTeam.setBudgetAsLong(currentTeam.getBudgetAsLong() + transferPrice);
				teamRepository.save(currentTeam);
			}
		}

		winnerTeam.setBudgetAsLong(winnerTeam.getBudgetAsLong() - transferPrice);
		teamRepository.save(winnerTeam);

		System.out.println("[AuctionService] Transfer completed: " + player.getName() + " to " + winnerTeam.getName()
				+ " for " + transferPrice);

		result.put("success", true);
		result.put("message", "Verhandlung abgeschlossen - Spieler transferiert");
		result.put("player", player.getName());
		result.put("price", transferPrice);
		result.put("salary", newSalary);

		return result;
	}

	/**
	 * Gets auction details for a specific auction player.
	 */
	public Map<String, Object> getAuctionDetails(Long auctionPlayerId, Long requestingTeamId) {
		AuctionPlayer auction = auctionPlayerRepository.findById(auctionPlayerId).orElse(null);
		if (auction == null) {
			return null;
		}

		Map<String, Object> details = auctionPlayerToMap(auction);

		// Füge gefilterte Gebote hinzu (nur eigene + höchstes)
		List<Map<String, Object>> bids = getBidsForAuction(auctionPlayerId, requestingTeamId);
		details.put("bids", bids);

		return details;
	}

	/**
	 * Gets auction details (legacy method without team filtering).
	 */
	public Map<String, Object> getAuctionDetails(Long auctionPlayerId) {
		return getAuctionDetails(auctionPlayerId, null);
	}

	/**
	 * Converts AuctionPlayer to Map for API response.
	 */
	private Map<String, Object> auctionPlayerToMap(AuctionPlayer auction) {
		Map<String, Object> map = new HashMap<>();
		map.put("id", auction.getId());
		map.put("playerId", auction.getPlayerId());
		map.put("playerName", auction.getPlayerName());
		map.put("position", auction.getPosition());
		map.put("rating", auction.getRating());
		map.put("age", auction.getAge());
		map.put("marketValue", auction.getMarketValue());
		map.put("salary", auction.getSalary());
		map.put("contractLength", auction.getContractLength());
		map.put("country", auction.getCountry());
		map.put("auctionStartTime", auction.getAuctionStartTime());
		map.put("auctionEndTime", auction.getAuctionEndTime());
		map.put("highestBidAmount", auction.getHighestBidAmount());
		map.put("highestBidderTeamId", auction.getHighestBidderTeamId());
		map.put("highestBidderTeamName", auction.getHighestBidderTeamName());
		map.put("auctionStatus", auction.getAuctionStatus());
		map.put("winnerTeamId", auction.getWinnerTeamId());
		map.put("winnerTeamName", auction.getWinnerTeamName());
		map.put("potentialRating", auction.getPotentialRating());
		return map;
	}

	/**
	 * Generates CPU bids for active auctions. CPU teams bid randomly on available
	 * players.
	 */
	@Transactional
	public void generateCPUBids() {
		System.out.println("[AuctionService] 🤖 Generating CPU bids for active auctions...");

		List<AuctionPlayer> activeAuctions = getActiveAuctions().stream()
				.map(m -> auctionPlayerRepository.findById(((Number) m.get("id")).longValue()).orElse(null))
				.filter(Objects::nonNull).collect(Collectors.toList());

		if (activeAuctions.isEmpty()) {
			System.out.println("[AuctionService] No active auctions for CPU bids");
			return;
		}

		List<Team> cpuTeams = teamRepository.findAll().stream().filter(Team::isCPU).collect(Collectors.toList());

		if (cpuTeams.isEmpty()) {
			System.out.println("[AuctionService] No CPU teams available");
			return;
		}

		int bidsPlaced = 0;

		// Für jede aktive Auktion, lass einige CPU-Teams bieten
		for (AuctionPlayer auction : activeAuctions) {
			// 40-70% der CPU-Teams bieten auf diesen Spieler
			int teamsToGenerate = (int) (cpuTeams.size() * (0.4 + random.nextDouble() * 0.3));
			teamsToGenerate = Math.max(1, Math.min(teamsToGenerate, cpuTeams.size()));

			Collections.shuffle(cpuTeams);

			for (int i = 0; i < teamsToGenerate; i++) {
				Team cpuTeam = cpuTeams.get(i);

				// Generiere Gebotspreis: 100-150% des Marktwerts
				long marketValue = auction.getMarketValue();
				long bidAmount = marketValue + (long) (marketValue * (0 + random.nextDouble() * 0.5));

				// Prüfe ob Gebot höher als aktuell höchstes ist
				if (auction.getHighestBidAmount() != null && bidAmount <= auction.getHighestBidAmount()) {
					// Erhöhe Gebot um zufällige Menge
					bidAmount = auction.getHighestBidAmount() + 10000 + random.nextInt(20000);
				}

				// Erstelle CPU-Gebot
				AuctionBid bid = new AuctionBid(auction.getId(), cpuTeam.getId(), cpuTeam.getName(), bidAmount,
						Instant.now(), true // CPU bid
				);
				auctionBidRepository.save(bid);

				// Aktualisiere höchstes Gebot
				if (auction.getHighestBidAmount() == null || bidAmount > auction.getHighestBidAmount()) {
					auction.setHighestBidAmount(bidAmount);
					auction.setHighestBidderTeamId(cpuTeam.getId());
					auction.setHighestBidderTeamName(cpuTeam.getName());
					auctionPlayerRepository.save(auction);
				}

				bidsPlaced++;
			}
		}

		System.out.println("[AuctionService] 🤖 CPU placed " + bidsPlaced + " bids");
	}

	/**
	 * Closes all active auctions and automatically transfers players to highest
	 * bidder. Checks if expiresAt (unix timestamp) has been reached. Contract: 3
	 * seasons, Salary: auction player's salary
	 */
	@Transactional
	public void closeAndTransferAuctions(int currentMatchday, int currentSeason) {
		System.out.println("[AuctionService] 🔔 Checking auctions for expiration...");

		long currentTimeMs = System.currentTimeMillis();
		List<AuctionPlayer> activeAuctions = auctionPlayerRepository.findByAuctionStatus("active");
		int transferCount = 0;

		for (AuctionPlayer auction : activeAuctions) {
			// Prüfe ob Ablaufdatum erreicht ist
			if (currentTimeMs < auction.getExpiresAt()) {
				System.out.println("[AuctionService] ⏳ Auction for " + auction.getPlayerName() + " not yet expired");
				continue;
			}

			System.out.println("[AuctionService] ⏰ Auction for " + auction.getPlayerName() + " has expired");

			// Finde das höchste Gebot
			List<AuctionBid> bids = auctionBidRepository.findByAuctionPlayerId(auction.getId());

			if (bids.isEmpty()) {
				// Keine Gebote - Auktion fehlgeschlagen
				auction.setAuctionStatus("failed");
				auctionPlayerRepository.save(auction);
				System.out.println("[AuctionService] ❌ Auction for " + auction.getPlayerName() + " failed - no bids");
				continue;
			}

			// Finde höchstes Gebot
			AuctionBid winningBid = bids.stream().max(Comparator.comparingLong(AuctionBid::getBidAmount)).orElse(null);

			if (winningBid == null) {
				auction.setAuctionStatus("failed");
				auctionPlayerRepository.save(auction);
				continue;
			}

			// Lade den Original-Spieler
			Player player = playerRepository.findById(auction.getPlayerId()).orElse(null);
			if (player == null) {
				System.err.println("[AuctionService] Player not found: " + auction.getPlayerId());
				continue;
			}

			// Lade Gewinner-Team
			Team winnerTeam = teamRepository.findById(winningBid.getBiddingTeamId()).orElse(null);
			if (winnerTeam == null) {
				System.err.println("[AuctionService] Winner team not found: " + winningBid.getBiddingTeamId());
				continue;
			}

			// Führe Transfer durch: 3 Saisons, Gehalt vom Auktionsspieler
			Long currentTeamId = player.getTeamId();
			Long transferPrice = winningBid.getBidAmount();
			int contractLength = 3; // IMMER 3 Saisons
			long newSalary = auction.getSalary(); // Gehalt vom Auktionsspieler
			
			// Hole altes Team für Transfer-Historie
			String fromTeamName = null;
			if (currentTeamId != null) {
				Team fromTeam = teamRepository.findById(currentTeamId).orElse(null);
				if (fromTeam != null) {
					fromTeamName = fromTeam.getName();
				}
			}

			player.setTeamId(winningBid.getBiddingTeamId());
			player.setSalary(newSalary);
			player.setContractLength(contractLength);
			player.calculateMarketValue();
			playerRepository.save(player);

			// Aktualisiere Budgets
			if (currentTeamId != null) {
				Team currentTeam = teamRepository.findById(currentTeamId).orElse(null);
				if (currentTeam != null) {
					currentTeam.setBudgetAsLong(currentTeam.getBudgetAsLong() + transferPrice);
					teamRepository.save(currentTeam);
				}
			}

			winnerTeam.setBudgetAsLong(winnerTeam.getBudgetAsLong() - transferPrice);
			teamRepository.save(winnerTeam);

			// Markiere Auktion als abgeschlossen
			auction.setWinnerTeamId(winningBid.getBiddingTeamId());
			auction.setWinnerTeamName(winningBid.getBiddingTeamName());
			auction.setAuctionStatus("completed");
			auctionPlayerRepository.save(auction);
			
			// Speichere Transfer-Historie
			TransferHistory history = new TransferHistory(
				player.getId(),
				player.getName(),
				player.getPosition(),
				player.getRating(),
				player.getAge(),
				currentTeamId,
				fromTeamName != null ? fromTeamName : "Transfermarkt",
				winnerTeam.getId(),
				winnerTeam.getName(),
				transferPrice,
				currentMatchday,
				currentSeason,
				Instant.now()
			);
			transferHistoryRepository.save(history);

			System.out.println("[AuctionService] ✅ Transfer completed: " + player.getName() + " to "
					+ winnerTeam.getName() + " for " + transferPrice + " (3 seasons, salary: " + newSalary + ")");
			transferCount++;
		}

		System.out.println("[AuctionService] 🔔 Auction closing finished - " + transferCount + " players transferred");
	}

	/**
	 * Overload without parameters (uses default values)
	 */
	@Transactional
	public void closeAndTransferAuctions() {
		closeAndTransferAuctions(0, 1); // Default: Tag 0, Saison 1
	}

	/**
	 * Verkürzt die Auktionszeiten um 24 Stunden (wird bei advanceToNextMatchday aufgerufen)
	 */
	@Transactional
	public void reduceAuctionTimeBy24Hours() {
		System.out.println("[AuctionService] ⏰ Verkürze Auktionszeiten um 24 Stunden...");
		
		List<AuctionPlayer> activeAuctions = auctionPlayerRepository.findByAuctionStatus("active");
		int reducedCount = 0;
		
		for (AuctionPlayer auction : activeAuctions) {
			// Verkürze End-Zeit um 24 Stunden
			Instant currentEndTime = auction.getAuctionEndTime();
			Instant newEndTime = currentEndTime.minusSeconds(24 * 3600);
			auction.setAuctionEndTime(newEndTime);
			
			// Verkürze auch expiresAt (unix timestamp)
			long currentExpiresAt = auction.getExpiresAt();
			long newExpiresAt = currentExpiresAt - (24 * 3600 * 1000L); // 24h in Millisekunden
			auction.setExpiresAt(newExpiresAt);
			
			auctionPlayerRepository.save(auction);
			
			System.out.println("[AuctionService] ⏱️ Auktion für " + auction.getPlayerName() + " um 24h verkürzt: " 
					+ currentEndTime + " → " + newEndTime);
			reducedCount++;
		}
		
		System.out.println("[AuctionService] ✅ " + reducedCount + " Auktionszeiten um 24h verkürzt");
	}
}
