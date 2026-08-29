package com.sharemoney.menu.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record MenuUpdateRequest(

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

        @NotNull
        Boolean active
) {
}
