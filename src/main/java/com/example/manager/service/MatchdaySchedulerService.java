package com.example.manager.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Service für zeitabhängige Spieltag-Simulation. - 18:00 Uhr: Startet
 * Live-Simulation für Liga-Spiele - 20:00 Uhr: Startet Live-Simulation für
 * Pokal-Spiele (an Spieltagen 3, 6, 9, 12, 15, 18) - 00:00 Uhr: Wechselt zum
 * nächsten Spieltag (inkl. Saison-Reset bei Tag 25)
 */
@Service
@EnableScheduling
public class MatchdaySchedulerService {

	@Autowired
	private RepositoryService repositoryService;

	@Autowired
	private LiveMatchSimulationService liveSimulationService;

	@Autowired
	private CpuLineupOptimizerService cpuLineupOptimizerService;

	@Autowired
	private CupService cupService;

	/**
	 * Scheduled Task: Läuft jeden Tag um 18:00 Uhr Startet die Live-Simulation für
	 * Liga-Spiele User können live Events mitverfolgen
	 */
	@Scheduled(cron = "0 0 18 * * ?") // 18:00 Uhr jeden Tag
	public void startLiveSimulationDaily() {
		try {
			System.out.println("⏰ [18:00] Automatische Live-Simulation für Liga-Spiele startet...");

			// Starte Live-Simulation für Liga
			liveSimulationService.startLiveSimulation();

			System.out.println("✅ Liga-Simulation gestartet!");
			System.out.println("   Dauer: 270 Sekunden (4,5 Minuten)");
			System.out.println("   Endet um ca. 18:05 Uhr");
		} catch (Exception e) {
			System.err.println("❌ Fehler bei Live-Simulation:");
			e.printStackTrace();
		}
	}

	/**
	 * Scheduled Task: Läuft jeden Tag um 20:00 Uhr Startet die Live-Simulation für
	 * Pokal-Spiele (an Spieltagen 3, 6, 9, 12, 15, 18)
	 */
	@Scheduled(cron = "0 0 20 * * ?") // 20:00 Uhr jeden Tag
	public void startLiveSimulationCupDaily() {
		try {
			int currentMatchday = repositoryService.getCurrentMatchday();

			// Prüfe ob aktueller Spieltag ein Pokalspiel-Tag ist (3, 6, 9, 12, 15, 18)
			if (isCupMatchday(currentMatchday)) {
				System.out.println("⏰ [20:00] Automatische Live-Simulation für Pokal-Spiele startet (Spieltag "
						+ currentMatchday + ")...");

				// Starte Live-Simulation für Pokal
				liveSimulationService.startLiveCupSimulation();

				System.out.println("✅ Pokal-Simulation gestartet!");
				System.out.println("   Dauer: 270 Sekunden (4,5 Minuten)");
				System.out.println("   Endet um ca. 20:05 Uhr");
			} else {
				System.out.println("⏰ [20:00] Spieltag " + currentMatchday + " hat keine Pokalspiele");
			}
		} catch (Exception e) {
			System.err.println("❌ Fehler bei Pokal-Simulation:");
			e.printStackTrace();
		}
	}

	/**
	 * Prüft ob ein Spieltag ein Pokalspiel-Tag ist Pokalspiele finden an den
	 * Spieltagen 3, 6, 9, 12, 15, 18 statt
	 */
	private boolean isCupMatchday(int matchday) {
		return matchday == 3 || matchday == 6 || matchday == 9 || matchday == 12 || matchday == 15 || matchday == 18;
	}

	/**
	 * Scheduled Task: Läuft jeden Tag um 00:00 Uhr (Mitternacht) Wechselt zum
	 * nächsten Spieltag (inkl. Saison-Reset bei Tag 25)
	 */
	@Scheduled(cron = "0 0 0 * * ?") // 00:00 Uhr jeden Tag
	public void advanceMatchdayAtMidnight() {
		try {
			System.out.println("⏰ [00:00] Automatischer Spieltagwechsel startet...");

			int currentMatchday = repositoryService.getCurrentMatchday();

			// Wechsle zum nächsten Spieltag (mit automatischem Saison-Reset bei Tag 25)
			repositoryService.advanceToNextMatchday();

			System.out.println("✅ Spieltagwechsel abgeschlossen!");

			// Wenn Spieltag 25 war, wurde Saison zurückgesetzt
			if (currentMatchday >= 25) {
				System.out.println("🏆 Neue Saison gestartet!");
			}
		} catch (Exception e) {
			System.err.println("❌ Fehler beim Spieltagwechsel:");
			e.printStackTrace();
		}
	}
}
