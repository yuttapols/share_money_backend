package com.sharemoney.menu.dto;

import com.sharemoney.common.domain.UserRole;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record MenuPermissionRequest(

        @NotEmpty
        List<UserRole> roles
) {
}
