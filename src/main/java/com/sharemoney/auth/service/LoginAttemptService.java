package com.sharemoney.auth.service;

import com.sharemoney.common.error.BusinessException;
import com.sharemoney.common.error.ErrorCode;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LoginAttemptService {

    private static final int MAX_ATTEMPTS = 5;
    private static final Duration LOCKOUT_DURATION = Duration.ofMinutes(15);
    private static final int CLEANUP_THRESHOLD = 10_000;

    private final Map<String, AttemptState> attemptsByUsername = new ConcurrentHashMap<>();

    public void assertNotLocked(String username) {
        AttemptState state = attemptsByUsername.get(normalize(username));
        if (state != null && state.isLocked()) {
            throw new BusinessException(ErrorCode.ACCOUNT_TEMPORARILY_LOCKED);
        }
    }

    public void recordFailure(String username) {
        String key = normalize(username);
        attemptsByUsername.compute(key, (ignoredKey, existing) -> {
            AttemptState state = existing != null && !existing.isStale() ? existing : new AttemptState();
            state.registerFailure();
            return state;
        });
        cleanupIfNeeded();
    }

    public void recordSuccess(String username) {
        attemptsByUsername.remove(normalize(username));
    }

    private void cleanupIfNeeded() {
        if (attemptsByUsername.size() > CLEANUP_THRESHOLD) {
            attemptsByUsername.values().removeIf(AttemptState::isStale);
        }
    }

    private String normalize(String username) {
        return username.toLowerCase();
    }

    private static final class AttemptState {

        private int failureCount;
        private Instant lastFailureAt;
        private Instant lockedUntil;

        void registerFailure() {
            failureCount++;
            lastFailureAt = Instant.now();
            if (failureCount >= MAX_ATTEMPTS) {
                lockedUntil = lastFailureAt.plus(LOCKOUT_DURATION);
            }
        }

        boolean isLocked() {
            return lockedUntil != null && Instant.now().isBefore(lockedUntil);
        }

        boolean isStale() {
            return !isLocked() && lastFailureAt.plus(LOCKOUT_DURATION).isBefore(Instant.now());
        }
    }
}
