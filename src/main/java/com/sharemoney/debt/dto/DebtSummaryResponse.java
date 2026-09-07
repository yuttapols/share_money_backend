package com.sharemoney.debt.dto;

import com.sharemoney.debt.entity.DebtMethod;
import com.sharemoney.debt.entity.DebtStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record DebtSummaryResponse(
        Long id,
        String creditorUsername,
        String debtorUsername,
        String title,
        String description,
        DebtMethod method,
        BigDecimal amount,
        Integer installmentCount,
        Integer installmentPaidCount,
        BigDecimal paidAmount,
        LocalDate lastPayDate,
        DebtStatus status,
        BigDecimal dueAmount,
        String dueLabel,
        BigDecimal interest,
        Integer sortOrder,
        Instant createdAt
) {
}
