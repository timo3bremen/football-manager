package com.example.manager.config;

import com.example.manager.service.RepositoryService;
import com.example.manager.service.CupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Initialisiert die 7 Ligen und Cup-Turniere beim Start der Anwendung
 */
@Component
public class ApplicationStartupListener {

    @Autowired
    private RepositoryService repositoryService;

    @Autowired
    private CupService cupService;

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        System.out.println("[ApplicationStartupListener] 🚀 Application started, initializing leagues and cups...");
        try {
            // Initialisiere Ligen
            repositoryService.initializeLigues();
            System.out.println("[ApplicationStartupListener] ✅ Leagues initialized successfully");
            
            // Initialisiere Cup-Turniere für alle Länder
            cupService.initializeAllCupTournaments(1);
            System.out.println("[ApplicationStartupListener] ✅ All cup tournaments initialized");
        } catch (Exception e) {
            System.err.println("[ApplicationStartupListener] Error during initialization: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

