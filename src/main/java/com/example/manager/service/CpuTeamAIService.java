package com.example.manager.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.manager.model.ContractNegotiation;
import com.example.manager.model.FreeAgent;
import com.example.manager.model.League;
import com.example.manager.model.LeagueSlot;
import com.example.manager.model.Player;
import com.example.manager.model.Sponsor;
import com.example.manager.model.StadiumBuild;
import com.example.manager.model.Team;
import com.example.manager.model.YouthPlayer;
import com.example.manager.repository.LeagueRepository;
import com.example.manager.repository.PlayerRepository;
import com.example.manager.repository.SponsorRepository;
import com.example.manager.repository.StadiumBuildRepository;
import com.example.manager.repository.TeamRepository;
import com.example.manager.repository.YouthPlayerRepository;
import com.example.manager.util.YouthPlayerGenerator;

/**
 * KI-Service für CPU-Teams - lässt CPU-Teams automatisch Entscheidungen treffen
 */
@Service
public class CpuTeamAIService {

	@Autowired
	private TeamRepository teamRepository;

	@Autowired
	private LeagueRepository leagueRepository;

	@Autowired
	private SponsorRepository sponsorRepository;

	@Autowired
	private StadiumBuildRepository stadiumBuildRepository;

	@Autowired
	private YouthPlayerRepository youthPlayerRepository;

	@Autowired
	private PlayerRepository playerRepository;

	@Autowired
	private com.example.manager.repository.ContractNegotiationRepository contractNegotiationRepository;

	@Autowired
	private com.example.manager.repository.FreeAgentRepository freeAgentRepository;

	@Autowired
	private com.example.manager.repository.LineupRepository lineupRepository;

	private final Random random = new Random();

	/**
	 * Lässt alle CPU-Teams KI-Entscheidungen treffen Wird beim Saison-Start
	 * aufgerufen
	 */
	@Transactional
	public void processCpuTeamDecisions() {
		System.out.println("[CPU-AI] 🤖 Starte KI-Entscheidungen für CPU-Teams...");

		List<Team> allTeams = teamRepository.findAll();
		int cpuTeamsProcessed = 0;

		for (Team team : allTeams) {
			if (team.isCPU()) {
				processSingleCpuTeam(team);
				cpuTeamsProcessed++;
			}
		}

		System.out.println("[CPU-AI] ✅ KI-Entscheidungen für " + cpuTeamsProcessed + " CPU-Teams abgeschlossen");
	}

	/**
	 * Lässt CPU-Teams periodische Entscheidungen treffen (z.B. alle 5 Spieltage)
	 * Für laufende Saison-Management
	 */
	@Transactional
	public void processPeriodicCpuDecisions() {
		System.out.println("[CPU-AI] 🤖 Periodische KI-Entscheidungen...");

		List<Team> allTeams = teamRepository.findAll();

		for (Team team : allTeams) {
			if (team.isCPU()) {
				// Erwäge Stadionausbau oder Jugend-Rekrutierung
				considerStadiumExpansion(team);
				recruitYouthPlayers(team);
			}
		}
	}

	/**
	 * Verarbeitet KI-Entscheidungen für ein einzelnes CPU-Team
	 */
	@Transactional
	private void processSingleCpuTeam(Team team) {
		try {
			// 1. Sponsor auswählen (wenn noch keiner vorhanden)
			chooseSponsorIfNeeded(team);

			// 2. Stadionausbau planen (mit Wahrscheinlichkeit)
			considerStadiumExpansion(team);

			// 3. Jugendakademie-Spieler rekrutieren
			recruitYouthPlayers(team);

		} catch (Exception e) {
			System.err.println("[CPU-AI] Fehler bei Team " + team.getName() + ": " + e.getMessage());
		}
	}

	/**
	 * Wählt einen Sponsor für das CPU-Team aus (wenn noch keiner vorhanden)
	 */
	private void chooseSponsorIfNeeded(Team team) {
		// Prüfe ob Team bereits einen Sponsor hat
		if (sponsorRepository.findByTeamId(team.getId()).isPresent()) {
			return; // Bereits Sponsor vorhanden
		}

		// 85% Chance dass CPU-Team einen Sponsor wählt (sehr wahrscheinlich)
		if (random.nextDouble() > 0.85) {
			return;
		}

		// Finde Liga-Division des Teams
		int division = getTeamDivision(team.getId());
		double multiplier = division == 1 ? 1.0 : division == 2 ? 0.6 : 0.33;

		// Wähle Sponsor basierend auf Team-Budget (intelligenter)
		long budget = team.getBudgetAsLong();
		int sponsorChoice;

		if (budget > 5_000_000) {
			// Reiche Teams bevorzugen MegaCorp (ausgewogen)
			sponsorChoice = random.nextDouble() < 0.6 ? 1 : (random.nextDouble() < 0.5 ? 0 : 2);
		} else if (budget > 2_000_000) {
			// Mittelklasse bevorzugt SportCo (hohe Titelprämie)
			sponsorChoice = random.nextDouble() < 0.5 ? 0 : (random.nextDouble() < 0.5 ? 1 : 2);
		} else {
			// Ärmere Teams bevorzugen LocalBank (hohe laufende Einnahmen)
			sponsorChoice = random.nextDouble() < 0.6 ? 2 : (random.nextDouble() < 0.5 ? 0 : 1);
		}

		String sponsorName;
		int appearance, win, survive, title;

		switch (sponsorChoice) {
		case 0: // SportCo
			sponsorName = "SportCo";
			appearance = (int) Math.round(40000 * multiplier);
			win = (int) Math.round(100000 * multiplier);
			survive = (int) Math.round(4000000 * multiplier);
			title = (int) Math.round(15000000 * multiplier);
			break;
		case 1: // MegaCorp
			sponsorName = "MegaCorp";
			appearance = (int) Math.round(70000 * multiplier);
			win = (int) Math.round(20000 * multiplier);
			survive = (int) Math.round(7000000 * multiplier);
			title = (int) Math.round(12000000 * multiplier);
			break;
		default: // LocalBank
			sponsorName = "LocalBank";
			appearance = (int) Math.round(120000 * multiplier);
			win = (int) Math.round(100000 * multiplier);
			survive = (int) Math.round(1000000 * multiplier);
			title = (int) Math.round(5000000 * multiplier);
			break;
		}

		Sponsor sponsor = new Sponsor(team.getId(), sponsorName, appearance, win, survive, title);
		sponsorRepository.save(sponsor);

		System.out.println("[CPU-AI] 🤝 Team " + team.getName() + " hat Sponsor " + sponsorName + " gewählt (Division: "
				+ division + ")");
	}

	/**
	 * Erwägt Stadionausbau für das CPU-Team
	 */
	private void considerStadiumExpansion(Team team) {
		// Prüfe ob bereits ein aktiver Ausbau läuft
		List<StadiumBuild> activeBuilds = stadiumBuildRepository.findByTeamIdAndCompletedFalse(team.getId());
		if (!activeBuilds.isEmpty()) {
			return; // Bereits Ausbau im Gange
		}

		// Chance für Stadionausbau basierend auf Budget und Division
		long budget = team.getBudgetAsLong();
		int division = getTeamDivision(team.getId());
		double expansionChance = 0.0;

		// Je mehr Budget und höher die Division, desto höher die Chance
		if (division == 1) {
			if (budget > 15_000_000) {
				expansionChance = 0.50; // 50% Chance in 1. Liga mit viel Geld
			} else if (budget > 10_000_000) {
				expansionChance = 0.35;
			} else if (budget > 5_000_000) {
				expansionChance = 0.25;
			} else if (budget > 2_000_000) {
				expansionChance = 0.15;
			}
		} else if (division == 2) {
			if (budget > 8_000_000) {
				expansionChance = 0.50;
			} else if (budget > 5_000_000) {
				expansionChance = 0.35;
			} else if (budget > 2_000_000) {
				expansionChance = 0.22;
			}
		} else { // Division 3
			if (budget > 5_000_000) {
				expansionChance = 0.50;
			} else if (budget > 2_000_000) {
				expansionChance = 0.35;
			} else if (budget > 1_000_000) {
				expansionChance = 0.228;
			}
		}

		if (random.nextDouble() > expansionChance) {
			return; // Kein Ausbau diesmal
		}

		// Wähle zufälligen Ausbau-Typ basierend auf aktueller Kapazität
		long standing = team.getStadiumCapacityStanding() != null ? team.getStadiumCapacityStanding() : 1000L;
		long seated = team.getStadiumCapacitySeated() != null ? team.getStadiumCapacitySeated() : 0L;
		long vip = team.getStadiumCapacityVip() != null ? team.getStadiumCapacityVip() : 0L;

		String seatType;
		int seats;
		long cost;

		// Intelligente Auswahl basierend auf aktuellem Stadium und Division
		double rand = random.nextDouble();

		// 1. Liga bevorzugt VIP und Sitzplätze
		if (division == 1) {
			if (vip < 2000 && rand < 0.4) {
				seatType = "vip";
				seats = 500;
				cost = 2_000_000 + random.nextInt(1_000_000);
			} else if (seated < 5000 && rand < 0.7) {
				seatType = "seated";
				seats = 1000;
				cost = 1_500_000 + random.nextInt(500_000);
			} else {
				seatType = "standing";
				seats = 1000;
				cost = 800_000 + random.nextInt(400_000);
			}
		}
		// 2. Liga: ausgeglichen
		else if (division == 2) {
			if (seated < 3000 && rand < 0.5) {
				seatType = "seated";
				seats = 1000;
				cost = 1_500_000 + random.nextInt(500_000);
			} else if (vip < 1000 && rand < 0.7) {
				seatType = "vip";
				seats = 500;
				cost = 2_000_000 + random.nextInt(1_000_000);
			} else {
				seatType = "standing";
				seats = 1000;
				cost = 800_000 + random.nextInt(400_000);
			}
		}
		// 3. Liga: hauptsächlich Stehplätze
		else {
			if (standing < 8000 && rand < 0.7) {
				seatType = "standing";
				seats = 1000;
				cost = 800_000 + random.nextInt(400_000);
			} else if (seated < 2000 && rand < 0.85) {
				seatType = "seated";
				seats = 1000;
				cost = 1_500_000 + random.nextInt(500_000);
			} else {
				seatType = "vip";
				seats = 500;
				cost = 2_000_000 + random.nextInt(1_000_000);
			}
		}

		// Prüfe ob Team sich den Ausbau leisten kann (mind. 50% Budget übrig lassen)
		if (budget < cost * 2) {
			return; // Zu teuer, nicht genug Budget-Puffer
		}

		// Starte Stadionausbau
		LocalDateTime startTime = LocalDateTime.now();
		// CPU-Teams bauen in 5-10 Tagen
		int durationDays = 5 + random.nextInt(6);
		LocalDateTime endTime = startTime.plusDays(durationDays);

		StadiumBuild build = new StadiumBuild(team.getId(), seats, seatType, cost, startTime, endTime);
		stadiumBuildRepository.save(build);

		// Ziehe Kosten ab
		team.setBudgetAsLong(budget - cost);
		teamRepository.save(team);

		System.out
				.println("[CPU-AI] 🏗️ Team " + team.getName() + " (Division " + division + ") startet Stadionausbau: "
						+ seats + " " + seatType + " für " + cost + "€ (Dauer: " + durationDays + " Tage)");
	}

	/**
	 * Rekrutiert Jugendakademie-Spieler für das CPU-Team
	 */
	private void recruitYouthPlayers(Team team) {
		// Prüfe wie viele Akademie-Spieler das Team bereits hat
		List<YouthPlayer> existingYouth = youthPlayerRepository.findByTeamId(team.getId());

		// CPU-Teams haben maximal 2-4 Akademie-Spieler (weniger als User-Teams)
		int maxYouth = 2 + random.nextInt(3); // 2-4
		if (existingYouth.size() >= maxYouth) {
			return; // Genug Akademie-Spieler
		}

		// 60% Chance dass ein neuer Spieler rekrutiert wird
		if (random.nextDouble() > 0.6) {
			return;
		}

		long budget = team.getBudgetAsLong();
		long recruitCost = 50_000 + random.nextInt(150_000); // 50k-200k

		if (budget < recruitCost * 3) {
			return; // Nicht genug Budget (3x Puffer)
		}

		// Division beeinflusst Scout-Region (1. Liga = bessere Regionen)
		int division = getTeamDivision(team.getId());
		String[] regions = division == 1 ? new String[] { "WestEuropa", "Südeuropa", "SüdAmerika" }
				: division == 2 ? new String[] { "WestEuropa", "Südeuropa", "England", "Skandinavien" }
						: new String[] { "WestEuropa", "Osteuropa", "Afrika" };

		String region = regions[random.nextInt(regions.length)];

		// Generiere Jugend-Spieler mit YouthPlayerGenerator
		YouthPlayer youthPlayer = YouthPlayerGenerator.generateYouthPlayerForRegion(team.getId(), null, // Kein Scout
																										// für CPU-Teams
																										// (automatisch
																										// rekrutiert)
				region);

		// Markiere als in Akademie
		youthPlayer.setInAcademy(true);

		youthPlayerRepository.save(youthPlayer);

		// Ziehe Kosten ab
		team.setBudgetAsLong(budget - recruitCost);
		teamRepository.save(team);

		System.out.println("[CPU-AI] 🏫 Team " + team.getName() + " (Division " + division
				+ ") rekrutiert Jugend-Spieler: " + youthPlayer.getName() + " (" + youthPlayer.getPosition()
				+ ", Rating: " + youthPlayer.getRating() + ", Potential: " + youthPlayer.getOverallPotential()
				+ ") aus " + region + " für " + recruitCost + "€");
	}

	/**
	 * Ermittelt die Liga-Division eines Teams
	 */
	private int getTeamDivision(Long teamId) {
		List<League> allLeagues = leagueRepository.findAll();

		for (League league : allLeagues) {
			for (LeagueSlot slot : league.getSlots()) {
				if (slot.getTeamId() != null && slot.getTeamId().equals(teamId)) {
					return league.getDivision();
				}
			}
		}

		return 1; // Default: 1. Liga
	}

	/**
	 * CPU-Teams verhandeln mit ihren Spielern deren Verträge bald auslaufen
	 * Wird beim Spieltagwechsel aufgerufen
	 * 8% Chance pro Spieltag dass ein Team mit einem Spieler verhandelt (1-2 Saisons Restlaufzeit)
	 */
	@Transactional
	public void processCpuContractNegotiations(int currentSeason) {
		try {
			System.out.println("[CPU-AI] 🤝 CPU-Teams erwägen Vertragsverhandlungen...");
			
			List<Team> allTeams = teamRepository.findAll();
			int totalAttempts = 0;
			int successfulNegotiations = 0;
			int failedNegotiations = 0;
			
			for (Team team : allTeams) {
				if (!team.isCPU()) continue;
				
				// Hole alle Spieler des Teams mit auslaufenden Verträgen (1-2 Saisons)
				List<Player> players = playerRepository.findByTeamId(team.getId());
				
			for (Player player : players) {
				// Nur Spieler mit 1-2 Saisons Restlaufzeit (nicht 0 und nicht >2)
				if (player.getContractLength() >= 1 && player.getContractLength() <= 2 && player.getContractLength() < 5) {
					
					// NEUE LOGIK: Höhere Chance für Spieler mit nur 1 Saison
					double negotiationChance = player.getContractLength() == 1 ? 0.80 : 0.25; // 80% bei 1 Saison, 25% bei 2 Saisons
					
					if (random.nextDouble() >= negotiationChance) {
						continue; // Skip
					}
					
					// Prüfe ob bereits verhandelt wurde diese Saison
					ContractNegotiation existingNegotiation = contractNegotiationRepository
							.findByPlayerIdAndSeason(player.getId(), currentSeason)
							.orElse(null);
					
					// Wenn bereits 3 Versuche oder gescheitert: überspringen
					if (existingNegotiation != null && existingNegotiation.isFailed()) {
						continue;
					}
					
					totalAttempts++;
					
					// CPU macht "intelligentes" Angebot
					// Berechne faires Gehalt (aktuell + 15-25%)
					double increase = 0.15 + (random.nextDouble() * 0.10);
					long proposedSalary = (long)(player.getSalary() * (1 + increase));
					
					// Vertragslaufzeit: 3-4 Saisons (CPU ist großzügig)
					int proposedLength = 3 + random.nextInt(2);
					
					// Simuliere Verhandlung (85% Erfolgsrate für CPU - sie bieten faire Angebote)
					boolean accepted = random.nextDouble() < 0.85;
						
						if (accepted) {
							// Vertrag verlängert
							player.setSalary(proposedSalary);
							player.setContractLength(proposedLength);
							player.calculateMarketValue();
							playerRepository.save(player);
							
							// Lösche Verhandlungshistorie
							if (existingNegotiation != null) {
								contractNegotiationRepository.delete(existingNegotiation);
							}
							
							successfulNegotiations++;
							System.out.println("[CPU-AI] ✅ " + team.getName() + " verlängert mit " + player.getName() + 
									" (" + proposedLength + " Saisons, " + proposedSalary + " Gehalt)");
						} else {
							// Verhandlung fehlgeschlagen
							ContractNegotiation negotiation = existingNegotiation != null ? 
									existingNegotiation : 
									new ContractNegotiation(player.getId(), team.getId(), currentSeason);
							
							negotiation.incrementAttempt();
							contractNegotiationRepository.save(negotiation);
							
					// Wenn alle 3 Versuche aufgebraucht: Spieler wird frei
					if (negotiation.isFailed() && player.getContractLength() <= 1) {
						Long oldTeamId = player.getTeamId();
						player.setTeamId(null);
						player.setFreeAgent(true);
						player.setOnTransferList(false);
						playerRepository.save(player);
						
						// Entferne aus Aufstellung
						if (oldTeamId != null) {
							List<com.example.manager.model.LineupSlot> lineupSlots = 
									lineupRepository.findByTeamId(oldTeamId);
							for (com.example.manager.model.LineupSlot slot : lineupSlots) {
								if (slot.getPlayerId() != null && slot.getPlayerId().equals(player.getId())) {
									slot.setPlayerId(null);
									lineupRepository.save(slot);
								}
							}
						}
						
						// Erstelle FreeAgent Eintrag
						FreeAgent freeAgent = new FreeAgent(player.getId(), currentSeason);
						freeAgentRepository.save(freeAgent);
						
						failedNegotiations++;
						System.out.println("[CPU-AI] ❌ " + team.getName() + " scheitert mit " + player.getName() + 
								" - Spieler wird FREIER AGENT!");
					} else {
						System.out.println("[CPU-AI] 🔄 " + team.getName() + " Verhandlung mit " + player.getName() + 
								" gescheitert (Versuch " + negotiation.getAttemptCount() + "/3)");
					}
						}
					}
			}
		}
		
		if (totalAttempts > 0) {
			System.out.println("[CPU-AI] 📊 Verhandlungen: " + totalAttempts + " Versuche, " + 
					successfulNegotiations + " erfolgreich, " + failedNegotiations + " gescheitert (neue freie Spieler)");
		}
		
	} catch (Exception e) {
		System.err.println("[CPU-AI] Fehler bei Vertragsverhandlungen: " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * CPU-Teams machen Angebote für freie Spieler
	 * Wird beim Spieltagwechsel aufgerufen
	 */
	@Transactional
	public void processCpuFreeAgentOffers() {
		try {
			System.out.println("[CPU-AI] ⭐ CPU-Teams machen Angebote für freie Spieler...");
			
			List<FreeAgent> freeAgents = freeAgentRepository.findByStatus("available");
			if (freeAgents.isEmpty()) {
				System.out.println("[CPU-AI] Keine freien Spieler verfügbar");
				return;
			}
			
			List<Team> cpuTeams = teamRepository.findAll().stream()
					.filter(Team::isCPU)
					.collect(java.util.stream.Collectors.toList());
			
			for (FreeAgent freeAgent : freeAgents) {
				Player player = playerRepository.findById(freeAgent.getPlayerId()).orElse(null);
				if (player == null) continue;
				
				// 30% Chance dass ein CPU-Team interessiert ist
				if (random.nextDouble() < 0.30) {
					// Wähle zufälliges CPU-Team
					Team interestedTeam = cpuTeams.get(random.nextInt(cpuTeams.size()));
					
					// Berechne Angebot (etwas niedriger als aktuelles Gehalt oder Standard)
					long currentSalary = player.getSalary() > 0 ? player.getSalary() : 50000;
					long offeredSalary = (long)(currentSalary * (0.8 + random.nextDouble() * 0.4)); // 80-120%
					int contractLength = 2 + random.nextInt(2); // 2-3 Jahre
					
					// Prüfe ob Angebot besser ist
					boolean isBetterOffer = freeAgent.getBestOfferSalary() == null || 
							offeredSalary > freeAgent.getBestOfferSalary();
					
					if (isBetterOffer) {
						freeAgent.setBestOfferTeamId(interestedTeam.getId());
						freeAgent.setBestOfferSalary(offeredSalary);
						freeAgent.setBestOfferContractLength(contractLength);
						freeAgent.setStatus("offers_pending");
						freeAgentRepository.save(freeAgent);
						
						System.out.println("[CPU-AI] 💼 " + interestedTeam.getName() + " bietet " + player.getName() + 
								" " + offeredSalary + " Gehalt für " + contractLength + " Saisons");
					}
				}
			}
			
		} catch (Exception e) {
			System.err.println("[CPU-AI] Fehler bei Free-Agent-Angeboten: " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * Automatisch verlängert Verträge aller CPU-Team-Spieler mit nur noch 1 Saison Laufzeit
	 * Wird VOR der neuen Saison aufgerufen (im Saison-Reset)
	 * - Spieler mit 1 Saison Restlaufzeit → automatisch um 3-4 Saisons verlängert
	 * - Gehalt wird um 15-25% erhöht
	 */
	@Transactional
	public void autoRenewExpiringContracts() {
		try {
			System.out.println("[CPU-AI] 🔄 Auto-Verlängerung: Verlängere Verträge mit 1 Saison Restlaufzeit...");
			
			List<Team> allTeams = teamRepository.findAll();
			int totalRenewed = 0;
			
			for (Team team : allTeams) {
				if (!team.isCPU()) continue;
				
				// Hole alle Spieler des Teams
				List<Player> players = playerRepository.findByTeamId(team.getId());
				
				for (Player player : players) {
					// Nur Spieler mit EXAKT 1 Saison Restlaufzeit verlängern
					if (player.getContractLength() == 1) {
						// Erhöhe Gehalt um 15-25%
						double increase = 0.15 + (random.nextDouble() * 0.10);
						long newSalary = (long)(player.getSalary() * (1 + increase));
						
						// Verlängere um 3-4 Saisons
						int newLength = 3 + random.nextInt(2);
						
						player.setSalary(newSalary);
						player.setContractLength(newLength);
						player.calculateMarketValue();
						playerRepository.save(player);
						
						totalRenewed++;
						System.out.println("[CPU-AI] ✅ " + team.getName() + " verlängert mit " + player.getName() + 
								" (" + newLength + " Saisons, Neues Gehalt: " + newSalary + ")");
					}
				}
			}
			
			System.out.println("[CPU-AI] 📊 Auto-Verlängerung abgeschlossen: " + totalRenewed + " Spieler verlängert");
			
		} catch (Exception e) {
			System.err.println("[CPU-AI] Fehler bei Auto-Verlängerung: " + e.getMessage());
			e.printStackTrace();
		}
	}
}
