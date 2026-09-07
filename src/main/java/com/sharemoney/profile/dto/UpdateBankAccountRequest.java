package com.sharemoney.profile.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateBankAccountRequest(

        @NotBlank
        @Size(max = 100)
        String bankName,

        @NotBlank
        @Size(max = 30)
        @Pattern(regexp = "^[0-9\\-\\s]+$")
        String accountNo,

        @NotBlank
        @Size(max = 150)
        String accountName,

        @Size(max = 255)
        String paymentNote
) {
}
