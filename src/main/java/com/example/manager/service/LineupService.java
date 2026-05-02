package com.example.manager.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.manager.model.LineupSlot;
import com.example.manager.model.LineupSlotId;
import com.example.manager.model.Player;
import com.example.manager.repository.LineupRepository;
import com.example.manager.repository.PlayerRepository;
import com.example.manager.repository.TeamRepository;

/**
 * Beispiel-Service der zeigt, wie die Repositories verwendet werden.
 */
@Service
public class LineupService {

	@Autowired
	private LineupRepository lineupRepository;

	@Autowired
	private PlayerRepository playerRepository;

	@Autowired
	private TeamRepository teamRepository;

	/**
	 * Speichert eine komplette Aufstellung für ein Team. Erstellt neue Slots wenn
	 * nicht vorhanden, updated bestehende.
	 * 
	 * @param teamId          Die Team-ID
	 * @param formationId     Die Formation (z.B. "4-4-2")
	 * @param slotAssignments Map von Slot-Index zu Spieler-ID (playerId kann auch null sein!)
	 */
	@Transactional
	public void saveLineup(Long teamId, String formationId, Map<Integer, Long> slotAssignments) {
		System.out.println("[LineupService] saveLineup called: teamId=" + teamId + ", formationId=" + formationId);
		System.out.println("[LineupService] slotAssignments: " + slotAssignments);

		// Für jeden Slot: erstelle oder update
		for (Map.Entry<Integer, Long> entry : slotAssignments.entrySet()) {
			Integer slotIndex = entry.getKey();
			Long playerId = entry.getValue();  // kann null sein!

			// Validiere: Wenn playerId nicht null ist, muss der Spieler existieren und zum Team gehören
			if (playerId != null) {
				Player player = playerRepository.findById(playerId).orElse(null);
				if (player == null) {
					System.out.println("[LineupService] WARNING: Player " + playerId + " does not exist! Skipping this slot.");
					continue;  // Überspringe diesen Slot, da der Spieler nicht existiert
				}
				if (player.getTeamId() == null || !player.getTeamId().equals(teamId)) {
					System.out.println("[LineupService] WARNING: Player " + playerId + " does not belong to team " + teamId + "! Skipping this slot.");
					continue;  // Überspringe, wenn der Spieler nicht zum Team gehört
				}
				
				// === VERLETZUNGS-CHECK ===
				// Verhindere das Hinzufügen von verletzten oder gesperrten Spielern zur Aufstellung
				if (player.isInjured()) {
					System.out.println("[LineupService] ❌ FEHLER: Spieler " + player.getName() + " ist verletzt (" + 
							player.getInjuryName() + ") und kann nicht zur Aufstellung hinzugefügt werden! " +
							"Noch " + player.getInjuryMatchdaysRemaining() + " Spieltag(e) Ausfallzeit.");
					continue;  // Überspringe diesen Spieler
				}
				
				if (player.isSuspended()) {
					System.out.println("[LineupService] ❌ FEHLER: Spieler " + player.getName() + " ist gesperrt (" + 
							player.getSuspensionReason() + ") und kann nicht zur Aufstellung hinzugefügt werden! " +
							"Noch " + player.getSuspensionMatchesRemaining() + " Spiel(e) Sperrung.");
					continue;  // Überspringe diesen Spieler
				}
			}

			LineupSlotId id = new LineupSlotId(teamId, formationId, slotIndex);

			// Versuche zu laden
			LineupSlot slot = lineupRepository.findById(id).orElse(null);

			if (slot == null) {
				// Neuer Slot - erstellen (sollte aber eigentlich schon existieren)
				slot = new LineupSlot();
				slot.setTeamId(teamId);
				slot.setFormationId(formationId);
				slot.setSlotIndex(slotIndex);
				slot.setPlayerId(playerId);
				slot.setSlotName("Slot" + slotIndex);

				System.out.println("[LineupService] Creating new slot: " + slotIndex + " -> Player " + playerId);
				lineupRepository.save(slot);
			} else {
				// Existierender Slot - update über ORM (auch wenn playerId null ist!)
				System.out.println("[LineupService] Updating slot: teamId=" + teamId + ", formationId=" + formationId + ", slotIndex=" + slotIndex + " -> Player " + playerId);
				slot.setPlayerId(playerId);  // kann null sein!
				lineupRepository.save(slot);
			}
		}

		// Kein flush() nötig! Die @Transactional Annotation kümmert sich um das committen
		System.out.println("[LineupService] All slots saved/updated");
	}

	/**
	 * Speichert eine Aufstellung mit expliziten Slot-Namen. Erstellt neue Slots
	 * wenn nicht vorhanden, updated bestehende.
	 * 
	 * @param teamId          Die Team-ID
	 * @param formationId     Die Formation (z.B. "4-4-2")
	 * @param slotAssignments Map von Slot-Name (z.B. "GK") zu Spieler-ID
	 */
	@Transactional
	public void saveLineupWithNames(Long teamId, String formationId, Map<String, Long> slotAssignments) {
		int slotIndex = 1;
		for (Map.Entry<String, Long> entry : slotAssignments.entrySet()) {
			if (entry.getValue() != null) {
				Long playerId = entry.getValue();
				
				// === VERLETZUNGS- UND SPERRUNGSPRÜFUNG ===
				Player player = playerRepository.findById(playerId).orElse(null);
				if (player != null) {
					// Prüfe ob Spieler verletzt ist
					if (player.isInjured()) {
						System.out.println("[LineupService] ❌ FEHLER: Spieler " + player.getName() + 
								" ist verletzt (" + player.getInjuryName() + ") und kann nicht zur Aufstellung hinzugefügt werden! " +
								"Noch " + player.getInjuryMatchdaysRemaining() + " Spieltag(e) Ausfallzeit.");
						continue; // Überspringe diesen Spieler
					}
					
					// Prüfe ob Spieler gesperrt ist
					if (player.isSuspended()) {
						System.out.println("[LineupService] ❌ FEHLER: Spieler " + player.getName() + 
								" ist gesperrt (" + player.getSuspensionReason() + ") und kann nicht zur Aufstellung hinzugefügt werden! " +
								"Noch " + player.getSuspensionMatchesRemaining() + " Spiel(e) Sperrung.");
						continue; // Überspringe diesen Spieler
					}
				}
				
				LineupSlotId id = new LineupSlotId(teamId, formationId, slotIndex);
				LineupSlot slot = lineupRepository.findById(id).orElse(null);

				if (slot == null) {
					slot = new LineupSlot();
					slot.setTeamId(teamId);
					slot.setFormationId(formationId);
					slot.setSlotIndex(slotIndex);
					slot.setSlotName(entry.getKey());
					slot.setPlayerId(playerId);
				} else {
					slot.setSlotName(entry.getKey());
					slot.setPlayerId(playerId);
				}

				lineupRepository.save(slot);
				slotIndex++;
			}
		}
	}

	/**
	 * Lädt eine Aufstellung und gibt sie als Map zurück.
	 */
	public Map<Integer, Long> getLineupAsMap(Long teamId, String formationId) {
		List<LineupSlot> slots = lineupRepository.findByTeamIdAndFormationId(teamId, formationId);

		Map<Integer, Long> result = new HashMap<>();
		for (LineupSlot slot : slots) {
			if (slot.getPlayerId() != null) {
				result.put(slot.getSlotIndex(), slot.getPlayerId());
			}
		}
		return result;
	}

	/**
	 * Wechselt einen Spieler in der Aufstellung.
	 */
	@Transactional
	public void swapPlayer(Long teamId, String formationId, int slotIndex, Long newPlayerId) {
		// === VERLETZUNGS- UND SPERRUNGSPRÜFUNG ===
		if (newPlayerId != null) {
			Player player = playerRepository.findById(newPlayerId).orElse(null);
			if (player != null) {
				// Prüfe ob Spieler verletzt ist
				if (player.isInjured()) {
					System.out.println("[LineupService] ❌ FEHLER: Spieler " + player.getName() + 
							" ist verletzt (" + player.getInjuryName() + ") und kann nicht zur Aufstellung hinzugefügt werden! " +
							"Noch " + player.getInjuryMatchdaysRemaining() + " Spieltag(e) Ausfallzeit.");
					return; // Abbrechen
				}
				
				// Prüfe ob Spieler gesperrt ist
				if (player.isSuspended()) {
					System.out.println("[LineupService] ❌ FEHLER: Spieler " + player.getName() + 
							" ist gesperrt (" + player.getSuspensionReason() + ") und kann nicht zur Aufstellung hinzugefügt werden! " +
							"Noch " + player.getSuspensionMatchesRemaining() + " Spiel(e) Sperrung.");
					return; // Abbrechen
				}
			}
		}
		
		LineupSlotId id = new LineupSlotId(teamId, formationId, slotIndex);

		LineupSlot slot = lineupRepository.findById(id).orElse(null);
		if (slot == null) {
			// Slot existiert noch nicht, neu erstellen
			slot = new LineupSlot(teamId, formationId, slotIndex, "Slot" + slotIndex, newPlayerId);
		} else {
			// Slot existiert, Spieler ändern
			slot.setPlayerId(newPlayerId);
		}

		lineupRepository.save(slot); // Speichern (INSERT oder UPDATE) - kein flush!
	}

	/**
	 * Entfernt einen Spieler aus einer Position.
	 */
	@Transactional
	public void removePlayerFromSlot(Long teamId, String formationId, int slotIndex) {
		LineupSlotId id = new LineupSlotId(teamId, formationId, slotIndex);
		lineupRepository.deleteById(id);
	}

	/**
	 * Gibt alle Spieler zurück, die in einer Aufstellung verwendet werden.
	 */
	public List<Player> getPlayersInLineup(Long teamId, String formationId) {
		List<LineupSlot> slots = lineupRepository.findByTeamIdAndFormationId(teamId, formationId);

		return slots.stream().map(LineupSlot::getPlayerId).filter(id -> id != null)
				.map(id -> playerRepository.findById(id).orElse(null)).filter(p -> p != null).toList();
	}

	/**
	 * Erstellt eine Standard-Aufstellung mit den besten Spielern des Teams.
	 * Erstellt neue Slots wenn nicht vorhanden, updated bestehende.
	 */
	@Transactional
	public void createDefaultLineup(Long teamId, String formationId) {
		List<Player> players = playerRepository.findByTeamId(teamId);

		// Sortiere nach Rating absteigend
		players.sort((p1, p2) -> Integer.compare(p2.getRating(), p1.getRating()));

		// Nehme die besten 11 Spieler
		int slotIndex = 1;
		for (int i = 0; i < Math.min(11, players.size()); i++) {
			LineupSlotId id = new LineupSlotId(teamId, formationId, slotIndex);
			LineupSlot slot = lineupRepository.findById(id).orElse(null);

			if (slot == null) {
				slot = new LineupSlot();
				slot.setTeamId(teamId);
				slot.setFormationId(formationId);
				slot.setSlotIndex(slotIndex);
				slot.setPlayerId(players.get(i).getId());
				slot.setSlotName("Slot" + slotIndex);
			} else {
				slot.setPlayerId(players.get(i).getId());
			}

			lineupRepository.save(slot);
			slotIndex++;
		}
	}

	/**
	 * Validiert eine Aufstellung.
	 */
	public boolean validateLineup(Long teamId, String formationId) {
		List<LineupSlot> slots = lineupRepository.findByTeamIdAndFormationId(teamId, formationId);
		List<Player> teamPlayers = playerRepository.findByTeamId(teamId);

		for (LineupSlot slot : slots) {
			if (slot.getPlayerId() != null) {
				boolean found = teamPlayers.stream().anyMatch(p -> p.getId().equals(slot.getPlayerId()));
				if (!found) {
					return false; // Spieler gehört nicht zum Team
				}
			}
		}
		return true;
	}

	/**
	 * Bereinigt alte/gelöschte Spieler-IDs aus allen Lineups eines Teams.
	 * Entfernt alle Lineup-Slots, die Player-IDs referenzieren, die nicht mehr existieren.
	 * 
	 * @param teamId Die Team-ID
	 * @return Anzahl der gelöschten Slot-Einträge
	 */
	@Transactional
	public int cleanupInvalidPlayerIds(Long teamId) {
		List<LineupSlot> allSlots = lineupRepository.findByTeamId(teamId);
		int deletedCount = 0;

		System.out.println("[LineupService] Cleanup: Found " + allSlots.size() + " lineup slots for team " + teamId);

		for (LineupSlot slot : allSlots) {
			if (slot.getPlayerId() != null) {
				// Prüfe ob der Spieler noch existiert
				boolean exists = playerRepository.existsById(slot.getPlayerId());
				if (!exists) {
					System.out.println("[LineupService] Cleanup: Removing invalid player " + slot.getPlayerId() 
						+ " from lineup slot " + slot.getTeamId() + "/" + slot.getFormationId() + "/" + slot.getSlotIndex());
					lineupRepository.deleteById(new LineupSlotId(slot.getTeamId(), slot.getFormationId(), slot.getSlotIndex()));
					deletedCount++;
				}
			}
		}

		System.out.println("[LineupService] Cleanup: Deleted " + deletedCount + " invalid lineup entries");
		return deletedCount;
	}
}
