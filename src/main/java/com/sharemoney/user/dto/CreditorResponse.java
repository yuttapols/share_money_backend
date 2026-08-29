package com.sharemoney.user.dto;

import com.sharemoney.common.domain.UserRole;

import java.time.Instant;

public record CreditorResponse(
        Long id,
        String username,
        String name,
        UserRole role,
        boolean active,
        Instant createdAt
) {
}
