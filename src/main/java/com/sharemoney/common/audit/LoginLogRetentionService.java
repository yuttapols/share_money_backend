package com.sharemoney.common.audit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class LoginLogRetentionService {

    private final LoginLogRepository loginLogRepository;

    @Value("${app.login-log.retention-days}")
    private int retentionDays;

    @Scheduled(cron = "0 30 2 * * *")
    @Transactional
    public void purgeExpiredLogs() {
        Instant cutoff = Instant.now().minus(retentionDays, ChronoUnit.DAYS);
        long deleted = loginLogRepository.deleteByCreatedAtBefore(cutoff);
        if (deleted > 0) {
            log.info("Purged {} login_logs entries older than {} days", deleted, retentionDays);
        }
    }
}
