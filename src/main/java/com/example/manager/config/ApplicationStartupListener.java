package com.example.manager.config;

import com.example.manager.service.RepositoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Initialisiert die 7 Ligen beim Start der Anwendung
 */
@Component
public class ApplicationStartupListener {

    @Autowired
    private RepositoryService repositoryService;

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        System.out.println("[ApplicationStartupListener] Application started, initializing leagues...");
        try {
            repositoryService.initializeLigues();
            System.out.println("[ApplicationStartupListener] Leagues initialized successfully");
        } catch (Exception e) {
            System.err.println("[ApplicationStartupListener] Error initializing leagues: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
