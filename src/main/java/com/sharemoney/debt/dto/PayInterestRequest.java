package com.sharemoney.debt.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PayInterestRequest(

        @NotNull
        LocalDate payDate,

        @DecimalMin("0.01")
        BigDecimal amount
) {
}
