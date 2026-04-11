package com.example.manager.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Initializes a small dummy table with sample data on application startup.
 * This runs for development environments and is safe to keep — it uses
 * CREATE TABLE IF NOT EXISTS and only inserts rows when the table is empty.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private final JdbcTemplate jdbc;
    private final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    public DataInitializer(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("Running DataInitializer - creating dummy table if needed");

        // Create tables if not exists
        jdbc.execute("CREATE TABLE IF NOT EXISTS DUMMY_PLAYER (" +
                "ID BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                "NAME VARCHAR(200), RATING INT, POTENTIAL INT, FORM INT)");

        jdbc.execute("CREATE TABLE IF NOT EXISTS DUMMY_TEAM (" +
                "ID BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                "NAME VARCHAR(200), BUDGET BIGINT)");

        // Check if table empty
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM DUMMY_PLAYER", Integer.class);
        if (count == null || count == 0) {
            log.info("Inserting sample rows into DUMMY_PLAYER");
            jdbc.update("INSERT INTO DUMMY_PLAYER(NAME,RATING,POTENTIAL,FORM) VALUES (?,?,?,?)",
                    "Max Mustermann", 65, 75, 0);
            jdbc.update("INSERT INTO DUMMY_PLAYER(NAME,RATING,POTENTIAL,FORM) VALUES (?,?,?,?)",
                    "Lukas Klein", 55, 80, 2);
            jdbc.update("INSERT INTO DUMMY_PLAYER(NAME,RATING,POTENTIAL,FORM) VALUES (?,?,?,?)",
                    "Erik Gross", 72, 70, -1);
            // Insert sample teams
            log.info("Inserting sample rows into DUMMY_TEAM");
            jdbc.update("INSERT INTO DUMMY_TEAM(NAME,BUDGET) VALUES (?,?)", "FC Alpha", 1000000L);
            jdbc.update("INSERT INTO DUMMY_TEAM(NAME,BUDGET) VALUES (?,?)", "SV Beta", 800000L);
            jdbc.update("INSERT INTO DUMMY_TEAM(NAME,BUDGET) VALUES (?,?)", "United Gamma", 500000L);
        } else {
            log.info("DUMMY_PLAYER already contains {} rows - skipping inserts", count);
        }
    }
}
