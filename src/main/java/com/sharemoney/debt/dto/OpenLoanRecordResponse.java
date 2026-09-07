package com.sharemoney.debt.dto;

import com.sharemoney.debt.entity.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record OpenLoanRecordResponse(
        int no,
        LocalDate payDate,
        BigDecimal totalPaid,
        BigDecimal interest,
        BigDecimal remainingPrincipal,
        PaymentStatus status,
        Instant paidAt
) {
}
