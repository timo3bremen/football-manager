package com.example.manager.service;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Service für zeitabhängige Spieltag-Simulation. Startet jeden Tag um 18:51 Uhr
 * eine Live-Simulation für 270 Sekunden (4,5 Minuten). User können während der
 * Simulation einsteigen und live Events mitverfolgen.
 */
@Service
@EnableScheduling
public class MatchdaySchedulerService {

	@Autowired
	private RepositoryService repositoryService;

	@Autowired
	private LiveMatchSimulationService liveSimulationService;

	/**
	 * Scheduled Task: Läuft jeden Tag um 18:51 Uhr Startet die Live-Simulation für
	 * 270 Sekunden (4,5 Minuten) User können auch später beitreten und die laufende
	 * Simulation sehen
	 */
	@Scheduled(cron = "0 51 18 * * ?") // 18:51 Uhr jeden Tag
	public void advanceMatchdayDaily() {
		try {
			LocalDateTime now = LocalDateTime.now();
			System.out.println("⏰ Automatischer Spieltag-Scheduler aktiviert um: " + now);
			System.out.println("🎮 Starte Live-Simulation (270 Sekunden)...");

			// Starte Live-Simulation statt sofortiger Berechnung
			liveSimulationService.startLiveSimulation();

			System.out.println("✅ Live-Simulation gestartet!");
			System.out.println("   Dauer: 270 Sekunden (4,5 Minuten)");
			System.out.println("   User können jederzeit beitreten und live mitverfolgen!");

		} catch (Exception e) {
			System.err.println("❌ Fehler bei Live-Simulation:");
			e.printStackTrace();
		}
	}
}
