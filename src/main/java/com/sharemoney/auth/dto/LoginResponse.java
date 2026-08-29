package com.sharemoney.auth.dto;

import com.sharemoney.common.domain.UserRole;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        long expiresIn,
        UserRole role,
        String username,
        String name
) {
}
