package com.sharemoney.debt.dto;

import com.sharemoney.debt.entity.InstallmentKind;
import com.sharemoney.debt.entity.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record InstallmentResponse(
        int no,
        BigDecimal amount,
        InstallmentKind kind,
        PaymentStatus status,
        LocalDate dueDate,
        LocalDate payDate,
        Instant paidAt
) {
}
