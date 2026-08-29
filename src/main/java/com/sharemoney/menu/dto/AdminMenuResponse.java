package com.sharemoney.menu.dto;

import com.sharemoney.common.domain.UserRole;

import java.util.List;

public record AdminMenuResponse(
        Long id,
        Long parentId,
        String menuKey,
        String icon,
        String route,
        int sortOrder,
        boolean active,
        List<UserRole> roles
) {
}
