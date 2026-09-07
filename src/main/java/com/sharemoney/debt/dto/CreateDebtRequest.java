package com.sharemoney.debt.dto;

import com.sharemoney.debt.entity.DebtMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateDebtRequest(

        @NotBlank
        String debtorUsername,

        @NotBlank
        @Size(max = 200)
        String title,

        @Size(max = 1000)
        String description,

        @NotNull
        DebtMethod method,

        @NotNull
        LocalDate startDate,

        Integer installmentCount,

        @DecimalMin(value = "0.01", message = "must be greater than 0")
        BigDecimal installmentAmount,

        @DecimalMin(value = "0.01", message = "must be greater than 0")
        BigDecimal principal
) {
}
