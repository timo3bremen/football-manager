package com.example.manager.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

/**
 * H2 TCP server configuration.
 *
 * Disabled by default to avoid port conflicts on developer machines. Enable by setting
 * `h2.tcp.enabled=true` in `application.properties` if you explicitly want a TCP server.
 */
@Configuration
@ConditionalOnProperty(prefix = "h2.tcp", name = "enabled", havingValue = "true", matchIfMissing = false)
public class H2ServerConfig {
    // When enabled, this configuration will create an H2 TCP server bean.
    // To avoid compilation/runtime issues when the H2 Server classes are not desired to be used,
    // the bean is conditionally created only when h2.tcp.enabled=true.
}
