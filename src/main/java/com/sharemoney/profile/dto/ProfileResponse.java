package com.sharemoney.profile.dto;

import com.sharemoney.common.domain.UserRole;

public record ProfileResponse(
        String username,
        String name,
        UserRole role,
        String phone,
        String avatarUrl,
        String preferredLanguage
) {
}
