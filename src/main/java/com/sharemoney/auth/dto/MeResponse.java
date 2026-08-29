package com.sharemoney.auth.dto;

import com.sharemoney.common.domain.UserRole;

public record MeResponse(
        String username,
        UserRole role,
        String name
) {
}
