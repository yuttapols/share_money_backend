package com.sharemoney.debt.dto;

import com.sharemoney.debt.entity.DebtMethod;
import com.sharemoney.debt.entity.DebtStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record DebtDetailResponse(
        Long id,
        String creditorUsername,
        String debtorUsername,
        String title,
        String description,
        DebtMethod method,
        BigDecimal amount,
        BigDecimal paidAmount,
        DebtStatus status,
        LocalDate startDate,
        BigDecimal installmentAmount,
        Integer installmentCount,
        BigDecimal interest,
        List<InstallmentResponse> installments,
        List<OpenLoanRecordResponse> openRecords
) {
}
