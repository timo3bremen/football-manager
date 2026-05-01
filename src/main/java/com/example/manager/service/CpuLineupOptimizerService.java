package com.example.manager.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.manager.model.LineupSlot;
import com.example.manager.model.Player;
import com.example.manager.model.Team;
import com.example.manager.model.YouthPlayer;
import com.example.manager.repository.LineupRepository;
import com.example.manager.repository.PlayerRepository;
import com.example.manager.repository.TeamRepository;
import com.example.manager.repository.YouthPlayerRepository;

/**
 * Service zur Optimierung der Aufstellungen von CPU-Teams
 * und automatischen Verpflichtung von Jugendakademie-Spielern
 */
@Service
public class CpuLineupOptimizerService {

	@Autowired
	private TeamRepository teamRepository;

	@Autowired
	private PlayerRepository playerRepository;

	@Autowired
	private LineupRepository lineupRepository;

	@Autowired
	private YouthPlayerRepository youthPlayerRepository;

	/**
	 * Optimiert die Aufstellungen aller CPU-Teams
	 * Wird täglich um 8:00 Uhr aufgerufen
	 */
	@Transactional
	public void optimizeAllCpuTeamLineups() {
		System.out.println("[CpuLineupOptimizer] 🤖 Starte Aufstellungs-Optimierung für alle CPU-Teams...");
		
		List<Team> allTeams = teamRepository.findAll();
		int optimizedCount = 0;
		int signedPlayers = 0;
		
		for (Team team : allTeams) {
			if (team.isCPU()) {
				// 1. Verpflichte Jugendakademie-Spieler wenn nötig
				int signed = signAcademyPlayersIfReady(team.getId());
				signedPlayers += signed;
				
				// 2. Optimiere Aufstellung
				optimizeTeamLineup(team.getId());
				optimizedCount++;
			}
		}
		
		System.out.println("[CpuLineupOptimizer] ✅ " + optimizedCount + " CPU-Teams optimiert, " + 
				signedPlayers + " Akademie-Spieler verpflichtet");
	}
	
	/**
	 * Optimiert die Aufstellung eines einzelnen Teams
	 * Stellt automatisch die beste verfügbare Aufstellung zusammen
	 */
	@Transactional
	public void optimizeTeamLineup(Long teamId) {
		Team team = teamRepository.findById(teamId).orElse(null);
		if (team == null) {
			return;
		}
		
		// Hole alle Spieler des Teams (nur Profi-Spieler, keine Jugendakademie)
		List<Player> proPlayers = playerRepository.findByTeamId(teamId);
		
		if (proPlayers.isEmpty()) {
			System.out.println("[CpuLineupOptimizer] ⚠️ Team " + team.getName() + " hat keine Spieler!");
			return;
		}
		
		// Optimiere alle 3 Formationen
		String[] formations = {"4-4-2", "4-3-3", "3-5-2"};
		Map<String, String[]> slotNames = new HashMap<>();
		slotNames.put("4-4-2", new String[] { "GK", "D1", "D2", "D3", "D4", "M1", "M2", "M3", "M4", "F1", "F2" });
		slotNames.put("4-3-3", new String[] { "GK", "D1", "D2", "D3", "D4", "M1", "M2", "M3", "F1", "F2", "F3" });
		slotNames.put("3-5-2", new String[] { "GK", "D1", "D2", "D3", "M1", "M2", "M3", "M4", "M5", "F1", "F2" });
		
		for (String formation : formations) {
			Map<Integer, Long> assignment;
			
			if ("4-4-2".equals(formation)) {
				assignment = autoAssignPlayers(proPlayers, 1, 4, 4, 2);
			} else if ("4-3-3".equals(formation)) {
				assignment = autoAssignPlayers(proPlayers, 1, 4, 3, 3);
			} else {
				assignment = autoAssignPlayers(proPlayers, 1, 3, 5, 2);
			}
			
			// Lösche alte Aufstellung
			lineupRepository.deleteByTeamIdAndFormationId(teamId, formation);
			
			// Speichere neue Aufstellung
			String[] names = slotNames.get(formation);
			for (int slotIndex = 1; slotIndex <= 11; slotIndex++) {
				LineupSlot slot = new LineupSlot();
				slot.setTeamId(teamId);
				slot.setFormationId(formation);
				slot.setSlotIndex(slotIndex);
				slot.setPlayerId(assignment.getOrDefault(slotIndex, null));
				slot.setSlotName(names[slotIndex - 1]);
				lineupRepository.save(slot);
			}
		}
		
		System.out.println("[CpuLineupOptimizer] ✅ Aufstellung für " + team.getName() + " optimiert");
	}
	
	/**
	 * Verpflichtet Jugendakademie-Spieler wenn sie bereit sind
	 * Bereit = Rating >= 65 ODER Alter >= 18
	 */
	@Transactional
	public int signAcademyPlayersIfReady(Long teamId) {
		List<YouthPlayer> academyPlayers = youthPlayerRepository.findByTeamIdAndIsInAcademyTrue(teamId);
		int signedCount = 0;
		
		for (YouthPlayer youthPlayer : academyPlayers) {
			// Prüfe ob bereit für Profi-Vertrag
			if (youthPlayer.getRating() >= 65 || youthPlayer.getAge() >= 18) {
				System.out.println("[CpuLineupOptimizer] 🌟 " + youthPlayer.getName() + " (Rating: " + 
						youthPlayer.getRating() + ", Alter: " + youthPlayer.getAge() + ") wird zum Profi!");
				
				// Erstelle neuen Profi-Spieler aus Jugend-Spieler
				Player proPlayer = new Player();
				proPlayer.setName(youthPlayer.getName());
				proPlayer.setAge(youthPlayer.getAge());
				proPlayer.setPosition(youthPlayer.getPosition());
				proPlayer.setTeamId(teamId);
				proPlayer.setCountry(youthPlayer.getCountry());
				proPlayer.setForm(0);
				proPlayer.setFitness(100);
				
				// Kopiere alle Stats
				proPlayer.setPace(youthPlayer.getPace());
				proPlayer.setDribbling(youthPlayer.getDribbling());
				proPlayer.setBallControl(youthPlayer.getBallControl());
				proPlayer.setShooting(youthPlayer.getShooting());
				proPlayer.setTackling(youthPlayer.getTackling());
				proPlayer.setSliding(youthPlayer.getSliding());
				proPlayer.setHeading(youthPlayer.getHeading());
				proPlayer.setCrossing(youthPlayer.getCrossing());
				proPlayer.setPassing(youthPlayer.getPassing());
				proPlayer.setAwareness(youthPlayer.getAwareness());
				proPlayer.setJumping(youthPlayer.getJumping());
				proPlayer.setStamina(youthPlayer.getStamina());
				proPlayer.setStrength(youthPlayer.getStrength());
				
				// Kopiere Potentiale
				proPlayer.setPacePotential(youthPlayer.getPacePotential());
				proPlayer.setDribblingPotential(youthPlayer.getDribblingPotential());
				proPlayer.setBallControlPotential(youthPlayer.getBallControlPotential());
				proPlayer.setShootingPotential(youthPlayer.getShootingPotential());
				proPlayer.setTacklingPotential(youthPlayer.getTacklingPotential());
				proPlayer.setSlidingPotential(youthPlayer.getSlidingPotential());
				proPlayer.setHeadingPotential(youthPlayer.getHeadingPotential());
				proPlayer.setCrossingPotential(youthPlayer.getCrossingPotential());
				proPlayer.setPassingPotential(youthPlayer.getPassingPotential());
				proPlayer.setAwarenessPotential(youthPlayer.getAwarenessPotential());
				proPlayer.setJumpingPotential(youthPlayer.getJumpingPotential());
				proPlayer.setStaminaPotential(youthPlayer.getStaminaPotential());
				proPlayer.setStrengthPotential(youthPlayer.getStrengthPotential());
				
				// Setze Gehalt basierend auf Rating (500€ pro Rating-Punkt)
				proPlayer.setSalary(youthPlayer.getRating() * 500);
				
				// Setze Fitness auf 100
				proPlayer.setFitness(100);
				
				// Berechne Rating
				proPlayer.calculateRating();
				
				// Speichere neuen Profi-Spieler
				playerRepository.save(proPlayer);
				
				// Entferne aus Akademie (setze isInAcademy auf false)
				youthPlayer.setInAcademy(false);
				youthPlayerRepository.save(youthPlayer);
				
				signedCount++;
			}
		}
		
		return signedCount;
	}
	
	/**
	 * Auto-Assignment Logik
	 * Weist Spieler basierend auf Position und Rating zu
	 * Nutzt andere Positionen als Ersatz wenn nicht genug Spieler vorhanden
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

		// GK slots (immer 1 Slot)
		for (int i = 0; i < numGK && i < gk.size(); i++) {
			assignment.put(slotIndex++, gk.get(i).getId());
		}
		// Falls kein GK: Slot bleibt leer aber Index muss weitergehen
		if (gk.size() < numGK) {
			slotIndex += (numGK - gk.size());
		}

		// DEF slots
		int assignedDef = 0;
		for (int i = 0; i < numDEF && i < def.size(); i++) {
			assignment.put(slotIndex++, def.get(i).getId());
			assignedDef++;
		}
		// Falls nicht genug DEF: Nutze MID als Ersatz
		if (assignedDef < numDEF) {
			int needed = numDEF - assignedDef;
			for (int i = 0; i < needed && i < mid.size(); i++) {
				assignment.put(slotIndex++, mid.get(i).getId());
				mid.remove(i);
				i--;
				assignedDef++;
			}
		}

		// MID slots
		int assignedMid = 0;
		for (int i = 0; i < numMID && i < mid.size(); i++) {
			assignment.put(slotIndex++, mid.get(i).getId());
			assignedMid++;
		}
		// Falls nicht genug MID: Nutze FWD als Ersatz
		if (assignedMid < numMID) {
			int needed = numMID - assignedMid;
			for (int i = 0; i < needed && i < fwd.size(); i++) {
				assignment.put(slotIndex++, fwd.get(i).getId());
				fwd.remove(i);
				i--;
				assignedMid++;
			}
		}

		// FWD slots
		int assignedFwd = 0;
		for (int i = 0; i < numFWD && i < fwd.size(); i++) {
			assignment.put(slotIndex++, fwd.get(i).getId());
			assignedFwd++;
		}
		// Falls nicht genug FWD: Nutze übrige DEF als Ersatz
		if (assignedFwd < numFWD && def.size() > assignedDef) {
			int needed = numFWD - assignedFwd;
			for (int i = assignedDef; i < def.size() && (i - assignedDef) < needed; i++) {
				assignment.put(slotIndex++, def.get(i).getId());
				assignedFwd++;
			}
		}

		System.out.println("[AutoAssign] Assigned " + assignment.size() + "/11 players (GK:" + 
			Math.min(numGK, gk.size()) + "/" + numGK + ", DEF:" + assignedDef + "/" + numDEF + 
			", MID:" + assignedMid + "/" + numMID + ", FWD:" + assignedFwd + "/" + numFWD + ")");

		return assignment;
	}
}
