package com.sharemoney.menu.dto;

import com.sharemoney.common.domain.UserRole;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record MenuCreateRequest(

        Long parentId,

        @NotBlank
        @Size(max = 100)
        @Pattern(regexp = "^[a-z0-9_.]+$")
        String menuKey,

        @NotBlank
        @Size(max = 50)
        String icon,

        @Size(max = 255)
        @Pattern(regexp = "^/.*", message = "route must start with /")
        String route,

        @NotNull
        @Min(0)
        @Max(9999)
        Integer sortOrder,

        @NotEmpty
        List<UserRole> roles
) {
}
