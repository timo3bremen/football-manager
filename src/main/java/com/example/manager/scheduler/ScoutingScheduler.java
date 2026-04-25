package com.example.manager.scheduler;

import java.time.Instant;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.example.manager.model.Scout;
import com.example.manager.model.YouthPlayer;
import com.example.manager.repository.ScoutRepository;
import com.example.manager.service.RepositoryService;

/**
 * Scheduler für Scouting-System Generiert täglich um 15 Uhr einen neuen
 * Jugenspieler pro aktivem Scout
 */
@Component
public class ScoutingScheduler {

	@Autowired
	private ScoutRepository scoutRepository;

	@Autowired
	private RepositoryService repositoryService;

	/**
	 * Läuft täglich um 15:00 Uhr Generiert für jeden aktiven Scout einen neuen
	 * Jugenspieler
	 */
//    @Scheduled(cron = "0 0 15 * * *") // täglich 15:00 Uhr
	@Scheduled(fixedDelay = 30000, initialDelay = 10000)
	public void generateScoutedPlayersDaily() {
		System.out.println("[ScoutingScheduler] Starte tägliche Jugenspieler-Generierung");

		try {
			// Lade alle aktiven Scouts
			List<Scout> activeScouts = scoutRepository.findAll().stream().filter(Scout::isActive).toList();

			System.out.println("[ScoutingScheduler] Gefundene aktive Scouts: " + activeScouts.size());

			for (Scout scout : activeScouts) {
				// Generiere neuen Jugenspieler - KEIN LIMIT pro Scout!
				YouthPlayer newPlayer = repositoryService.generateScoutedPlayer(scout.getId());

				if (newPlayer != null) {
					System.out.println("[ScoutingScheduler] ✓ Neuer Jugenspieler generiert: " + newPlayer.getName()
							+ " (Alter: " + newPlayer.getAge() + ", Rating: " + newPlayer.getRating() + ", Potential: "
							+ newPlayer.getOverallPotential() + ")");

					// Reduziere verbleibende Tage
					scout.setDaysRemaining(scout.getDaysRemaining() - 1);

					// Wenn keine Tage mehr übrig, deaktiviere Scout
					if (scout.getDaysRemaining() <= 0) {
						scout.setActive(false);
						System.out.println(
								"[ScoutingScheduler] Scout " + scout.getId() + " hat keine Tage mehr - deaktiviert");
					}

					scoutRepository.save(scout);
				}
			}

			System.out.println("[ScoutingScheduler] Tägliche Generierung abgeschlossen");

		} catch (Exception e) {
			System.err.println("[ScoutingScheduler] Fehler bei der Generierung: " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * Obsolete Methode - wird nicht mehr verwendet
	 * Es gibt kein Limit für gescoutete Spieler pro Tag
	 */
	@Deprecated
	private boolean hasAlreadyGeneratedTodayForScout(Scout scout) {
		// Immer false - kein Limit
		return false;
	}

	/**
	 * Optional: Tägliche Cleanup - entferne inaktive Scouts nach 7 Tagen
	 */
	@Scheduled(cron = "0 0 0 * * *") // täglich um Mitternacht
	public void cleanupExpiredScouts() {
		System.out.println("[ScoutingScheduler] Cleanup: Entferne abgelaufene Scouts");

		try {
			List<Scout> inactiveScouts = scoutRepository.findAll().stream()
					.filter(s -> !s.isActive() && s.getDaysRemaining() == 0).toList();

			System.out.println("[ScoutingScheduler] Gefundene abgelaufene Scouts: " + inactiveScouts.size());
			// Optional: Scouts löschen oder archivieren
			// scoutRepository.deleteAll(inactiveScouts);

		} catch (Exception e) {
			System.err.println("[ScoutingScheduler] Fehler beim Cleanup: " + e.getMessage());
		}
	}
}
