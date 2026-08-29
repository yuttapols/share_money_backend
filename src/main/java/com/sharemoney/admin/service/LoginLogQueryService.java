package com.sharemoney.admin.service;

import com.sharemoney.admin.dto.LoginLogResponse;
import com.sharemoney.common.audit.LoginLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LoginLogQueryService {

    private static final DateTimeFormatter TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.of("Asia/Bangkok"));

    private final LoginLogRepository loginLogRepository;

    @Transactional(readOnly = true)
    public List<LoginLogResponse> list(int limit) {
        return loginLogRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, limit)).stream()
                .map(log -> new LoginLogResponse(
                        TIMESTAMP_FORMATTER.format(log.getCreatedAt()),
                        log.getUsername(),
                        log.getRole(),
                        log.getAction()))
                .toList();
    }
}
