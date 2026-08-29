package com.sharemoney.common.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JwtSecretGuard {

    private static final String INSECURE_DEFAULT_SECRET =
            "change-this-secret-change-this-secret-change-this-secret";

    private final Environment environment;

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @PostConstruct
    public void verify() {
        if (!environment.matchesProfiles("dev") && INSECURE_DEFAULT_SECRET.equals(jwtSecret)) {
            throw new IllegalStateException(
                    "JWT_SECRET is still the insecure default value. Set the JWT_SECRET environment variable before running outside the dev profile.");
        }
    }
}
