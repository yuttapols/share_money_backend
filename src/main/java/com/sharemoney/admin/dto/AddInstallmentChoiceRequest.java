package com.sharemoney.admin.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record AddInstallmentChoiceRequest(

        @NotNull
        @Min(1)
        @Max(360)
        Integer count
) {
}
