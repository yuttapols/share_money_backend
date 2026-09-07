package com.sharemoney.debt.dto;

import com.sharemoney.debt.entity.PaymentStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record OpenRecordUpdateRequest(

        @NotNull
        @DecimalMin("0")
        BigDecimal totalPaid,

        @NotNull
        PaymentStatus status
) {
}
