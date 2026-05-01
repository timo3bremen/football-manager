package com.example.manager.scheduler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.example.manager.service.AuctionService;

/**
 * Scheduler für tägliche Auktionen
 * - 16:00: Auktionen enden und Spieler werden übertragen
 * - 16:01: 7 neue Spieler werden zur Auktion angeboten
 */
@Component
public class AuctionScheduler {

	@Autowired
	private AuctionService auctionService;

	/**
	 * Schließt Auktionen und überträgt Spieler zum Höchstbietenden um 16:00 Uhr
	 */
	@Scheduled(cron = "0 0 16 * * ?") // 16:00 Uhr jeden Tag
	public void closeAuctionsAndTransferPlayers() {
		System.out.println("⏰ [16:00] AuctionScheduler - Schließe Auktionen und übertrage Spieler");
		try {
			auctionService.closeAndTransferAuctions();
			System.out.println("✅ Auktionen geschlossen und Spieler übertragen!");
		} catch (Exception e) {
			System.err.println("[AuctionScheduler] Error closing auctions: " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * Erstellt 7 neue Auktionsspieler um 16:01 Uhr (1 Minute nach Auktionsende)
	 */
	@Scheduled(cron = "0 1 16 * * ?") // 16:01 Uhr jeden Tag
	public void createDailyAuction() {
		System.out.println("⏰ [16:01] AuctionScheduler - Erstelle 7 neue Auktionsspieler");
		try {
			auctionService.createDailyAuction();
			System.out.println("✅ 7 neue Spieler zur Auktion hinzugefügt!");
		} catch (Exception e) {
			System.err.println("[AuctionScheduler] Error creating daily auction: " + e.getMessage());
			e.printStackTrace();
		}
	}
}
