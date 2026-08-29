package com.sharemoney.common.audit;

import com.sharemoney.common.domain.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final LoginLogRepository loginLogRepository;

    @Transactional
    public void record(String username, UserRole role, String action, String ipAddress) {
        LoginLog log = new LoginLog();
        log.setUsername(username);
        log.setRole(role.name());
        log.setAction(action);
        log.setIpAddress(ipAddress);
        loginLogRepository.save(log);
    }
}
