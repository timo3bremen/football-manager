package com.example.manager.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Scheduler für Transfermarkt-Operationen
 */
@Service
public class TransferMarketScheduler {

	@Autowired
	private TransferMarketService transferMarketService;

	/**
	 * Generiert automatisch CPU-Angebote für Spieler auf der Transferliste
	 * initialDelay: 10 Sekunden nach Start (10000 ms) - schnell für Tests
	 * fixedDelay: 30 Sekunden = 30000 ms (sehr schnell für Tests, später erhöhen!)
	 * 
	 * PRODUKTIV: initialDelay = 60000, fixedDelay = 14400000 (4 Stunden)
	 */
	@Scheduled(fixedDelay = 30000, initialDelay = 10000)
	@Transactional
	public void generateCPUOffersScheduled() {
		long startTime = System.currentTimeMillis();
		System.out.println("[TransferMarketScheduler] ⏰ Starting scheduled CPU offer generation at " + startTime);
		System.out.println("[TransferMarketScheduler] Service instance: " + (transferMarketService != null ? "✅ Autowired" : "❌ NULL"));
		
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
	 * Läuft alle 45 Sekunden (nach Angebotserstellung)
	 * initialDelay: 15 Sekunden (nach Angebotserstellung)
	 * 
	 * PRODUKTIV: initialDelay = 120000, fixedDelay = 21600000 (6 Stunden)
	 */
	@Scheduled(fixedDelay = 45000, initialDelay = 15000)
	@Transactional
	public void processCPUOfferResponsesScheduled() {
		long startTime = System.currentTimeMillis();
		System.out.println("[TransferMarketScheduler] ⏰ Starting scheduled CPU offer response processing at " + startTime);
		
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
