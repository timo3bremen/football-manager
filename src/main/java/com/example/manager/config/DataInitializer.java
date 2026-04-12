package com.example.manager.config;

import com.example.manager.model.Player;
import com.example.manager.model.Team;
import com.example.manager.repository.PlayerRepository;
import com.example.manager.repository.TeamRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Initializes the database with seed data on startup using JPA repositories.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private PlayerRepository playerRepository;

    @Override
    public void run(String... args) throws Exception {
        // Check if data already exists
        if (teamRepository.count() > 0) {
            log.info("Database already initialized, skipping seed data");
            return;
        }

        log.info("Initializing database with seed data...");

        // Create seed teams
        Team t1 = new Team("FC Alpha", 1000000);
        Team t2 = new Team("SV Beta", 800000);
        Team t3 = new Team("United Gamma", 500000);
        
        t1 = teamRepository.save(t1);
        t2 = teamRepository.save(t2);
        t3 = teamRepository.save(t3);

        // Create seed players
        Player p1 = new Player("Max Mustermann", 65, 75, 0);
        p1.setTeamId(t1.getId());
        playerRepository.save(p1);

        Player p2 = new Player("Lukas Klein", 55, 80, 2);
        p2.setTeamId(t2.getId());
        playerRepository.save(p2);

        Player p3 = new Player("Erik Gross", 72, 70, -1);
        p3.setTeamId(t3.getId());
        playerRepository.save(p3);

        log.info("Seed data created: 3 teams and 3 players");
    }
}
