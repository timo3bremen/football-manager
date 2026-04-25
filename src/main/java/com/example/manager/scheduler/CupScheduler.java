package com.example.manager.scheduler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.example.manager.service.CupService;

/**
 * Scheduler for cup tournaments.
 * - Generates cup round every 3 match days
 * - Completes cup matches
 */
@Component
public class CupScheduler {

    @Autowired
    private CupService cupService;

    /**
     * Check every hour if it's time to advance cup rounds (every 3 match days).
     * Runs every hour on the hour.
     */
    @Scheduled(cron = "0 0 * * * *", zone = "UTC")
    public void processCupRounds() {
        try {
            System.out.println("[CupScheduler] Cup scheduler running...");
            // Cup processing logic would go here
            // This would be triggered by game day events
        } catch (Exception e) {
            System.err.println("[CupScheduler] Error processing cup rounds: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
