package com.example.manager.scheduler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.example.manager.service.AuctionService;

/**
 * Scheduler for daily auctions. - Creates 7 new auction players every day at
 * 22:00 - Closes auctions at 22:02 (2 minutes later) - Automatically transfers
 * players to highest bidder
 */
@Component
public class AuctionScheduler {

	@Autowired
	private AuctionService auctionService;

	/**
	 * Creates daily auction at 22:00 (10 PM). Cron: 0 22 * * * (22:00 every day)
	 */
	@Scheduled(cron = "0 0 22 * * *")
//	@Scheduled(fixedDelay = 30000, initialDelay = 10000)
	public void createDailyAuction() {
		System.out.println("[AuctionScheduler] ⏰ Running daily auction creation at 22:00");
		try {
			auctionService.createDailyAuction();
		} catch (Exception e) {
			System.err.println("[AuctionScheduler] Error creating daily auction: " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * Closes auctions and transfers players to highest bidder at 22:01 (1 minute
	 * after creation). Checks if expiration time (expiresAt) has been reached.
	 * Cron: 1 22 * * * (22:01 every day)
	 */
	@Scheduled(cron = "0 1 22 * * *")
//	@Scheduled(fixedDelay = 30000, initialDelay = 10000)
	public void closeAuctionsAndTransferPlayers() {
		System.out.println("[AuctionScheduler] 🔔 Checking for expired auctions at 22:01");
		try {
			auctionService.closeAndTransferAuctions();
		} catch (Exception e) {
			System.err.println("[AuctionScheduler] Error closing auctions: " + e.getMessage());
			e.printStackTrace();
		}
	}
}
