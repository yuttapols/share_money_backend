package com.sharemoney.debt.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record OpenRecordCreateRequest(

        @NotNull
        LocalDate payDate,

        @NotNull
        @DecimalMin("0")
        BigDecimal interest,

        @NotNull
        @DecimalMin("0")
        BigDecimal remainingPrincipal,

        @NotNull
        Boolean paid
) {
}
