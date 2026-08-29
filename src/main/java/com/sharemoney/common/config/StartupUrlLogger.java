package com.sharemoney.common.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StartupUrlLogger {

    private final Environment environment;

    @EventListener(ApplicationReadyEvent.class)
    public void logStartupUrls() {
        String port = environment.getProperty("server.port", "8080");
        String contextPath = environment.getProperty("server.servlet.context-path", "");
        String baseUrl = "http://localhost:" + port + contextPath;

        log.info("Application is running at: {}", baseUrl);

        boolean swaggerEnabled = environment.getProperty("springdoc.swagger-ui.enabled", Boolean.class, true);
        if (swaggerEnabled) {
            String swaggerPath = environment.getProperty("springdoc.swagger-ui.path", "/swagger-ui.html");
            log.info("Swagger UI is available at: {}{}", baseUrl, swaggerPath);
        }
    }
}
