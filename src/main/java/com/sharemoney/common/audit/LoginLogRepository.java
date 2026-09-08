package com.sharemoney.common.audit;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface LoginLogRepository extends JpaRepository<LoginLog, Long> {

    List<LoginLog> findAllByOrderByCreatedAtDesc(Pageable pageable);

    long deleteByCreatedAtBefore(Instant cutoff);
}
