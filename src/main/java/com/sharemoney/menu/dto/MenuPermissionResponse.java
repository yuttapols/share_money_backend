package com.sharemoney.menu.dto;

import com.sharemoney.common.domain.UserRole;

import java.util.List;

public record MenuPermissionResponse(
        Long menuItemId,
        List<UserRole> roles
) {
}
