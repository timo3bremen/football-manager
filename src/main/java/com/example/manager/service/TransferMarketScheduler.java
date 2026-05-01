package com.example.manager.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Scheduler für Transfermarkt-Operationen
 * CPU-Teams geben alle 4 Stunden Angebote ab und reagieren auf Angebote
 */
@Service
public class TransferMarketScheduler {

	@Autowired
	private TransferMarketService transferMarketService;

	/**
	 * Generiert automatisch CPU-Angebote für Spieler auf der Transferliste
	 * Läuft alle 4 Stunden: 00:00, 04:00, 08:00, 12:00, 16:00, 20:00
	 */
	@Scheduled(cron = "0 0 0/4 * * ?") // Alle 4 Stunden
	@Transactional
	public void generateCPUOffersScheduled() {
		long startTime = System.currentTimeMillis();
		System.out.println("[TransferMarketScheduler] ⏰ Starting scheduled CPU offer generation (every 4h)");
		
		try {
			if (transferMarketService == null) {
				System.err.println("[TransferMarketScheduler] ❌ TransferMarketService is NULL - Autowiring failed!");
				return;
			}
			
			transferMarketService.generateCPUOffers();
			long duration = System.currentTimeMillis() - startTime;
			System.out.println("[TransferMarketScheduler] ✅ CPU offer generation completed successfully in " + duration + "ms");
		} catch (Exception e) {
			System.err.println("[TransferMarketScheduler] ❌ Error generating CPU offers: " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * Verarbeitet automatisch CPU-Team Antworten auf Angebote
	 * Läuft alle 4 Stunden (leicht versetzt): 00:30, 04:30, 08:30, 12:30, 16:30, 20:30
	 */
	@Scheduled(cron = "0 30 0/4 * * ?") // Alle 4 Stunden, 30 Min versetzt
	@Transactional
	public void processCPUOfferResponsesScheduled() {
		long startTime = System.currentTimeMillis();
		System.out.println("[TransferMarketScheduler] ⏰ Starting scheduled CPU offer response processing (every 4h)");
		
		try {
			if (transferMarketService == null) {
				System.err.println("[TransferMarketScheduler] ❌ TransferMarketService is NULL - Autowiring failed!");
				return;
			}
			
			transferMarketService.processCPUOfferResponses();
			long duration = System.currentTimeMillis() - startTime;
			System.out.println("[TransferMarketScheduler] ✅ CPU offer response processing completed successfully in " + duration + "ms");
		} catch (Exception e) {
			System.err.println("[TransferMarketScheduler] ❌ Error processing CPU offer responses: " + e.getMessage());
			e.printStackTrace();
		}
	}
}
