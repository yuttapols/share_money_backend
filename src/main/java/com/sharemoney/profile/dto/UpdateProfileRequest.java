package com.sharemoney.profile.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(

        @NotBlank
        @Size(max = 150)
        String name,

        @Size(max = 20)
        @Pattern(regexp = "^[0-9+\\-\\s]{6,20}$")
        String phone,

        @NotBlank
        @Pattern(regexp = "^(th|en)$")
        String preferredLanguage
) {
}
