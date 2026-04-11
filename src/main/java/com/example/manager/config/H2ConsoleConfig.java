package com.example.manager.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

/**
 * Placeholder configuration for H2 Console. The explicit servlet registration
 * that referenced H2's WebServlet caused compilation issues in some setups
 * (servlet package mismatches). Rely on Spring Boot's built-in H2 console
 * auto-configuration instead. If the console still doesn't appear, use the
 * file-based DB and connect with DBeaver (recommended for development).
 */
@Configuration
public class H2ConsoleConfig {
    private static final Logger log = LoggerFactory.getLogger(H2ConsoleConfig.class);

    public H2ConsoleConfig() {
        log.debug("H2ConsoleConfig loaded - relying on Spring Boot auto-config for H2 console");
    }
}
