package com.sharemoney.debt.service;

import com.sharemoney.common.period.BillingPeriod;
import com.sharemoney.debt.dto.DebtDetailResponse;
import com.sharemoney.debt.dto.DebtSummaryResponse;
import com.sharemoney.debt.dto.InstallmentResponse;
import com.sharemoney.debt.dto.OpenLoanRecordResponse;
import com.sharemoney.debt.entity.Debt;
import com.sharemoney.debt.entity.DebtMethod;
import com.sharemoney.debt.entity.DebtStatus;
import com.sharemoney.debt.entity.Installment;
import com.sharemoney.debt.entity.InstallmentInterestPayment;
import com.sharemoney.debt.entity.InstallmentStatus;
import com.sharemoney.debt.entity.OpenLoanRecord;
import com.sharemoney.debt.entity.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Stream;

public final class DebtCalculator {

    private static final ZoneId ZONE = ZoneId.of("Asia/Bangkok");

    private DebtCalculator() {
    }

    public static void recalculateInstallmentDebt(Debt debt) {
        List<Installment> installments = debt.getInstallments();

        BigDecimal paidAmount = installments.stream()
                .filter(installment -> installment.getStatus() == InstallmentStatus.PAID)
                .map(Installment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long paidCount = installments.stream().filter(installment -> installment.getStatus() == InstallmentStatus.PAID).count();
        boolean anyInterestPaid = installments.stream().anyMatch(installment -> !installment.getInterestPayments().isEmpty());

        DebtStatus status;
        if (!installments.isEmpty() && paidCount == installments.size()) {
            status = DebtStatus.PAID;
        } else if (paidCount > 0 || anyInterestPaid) {
            status = DebtStatus.PARTIAL;
        } else {
            status = DebtStatus.PENDING;
        }

        debt.setPaidAmount(paidAmount);
        debt.setStatus(status);
        debt.setPaidAt(status == DebtStatus.PAID ? Instant.now() : null);
    }

    public static void recalculateOpenDebt(Debt debt) {
        OpenLoanRecord latest = latestOpenRecord(debt);
        BigDecimal remaining = latest.getRemainingPrincipal();
        BigDecimal paidAmount = debt.getPrincipalAmount().subtract(remaining);

        DebtStatus status;
        if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
            status = DebtStatus.PAID;
        } else if (paidAmount.compareTo(BigDecimal.ZERO) > 0) {
            status = DebtStatus.PARTIAL;
        } else {
            status = DebtStatus.PENDING;
        }

        debt.setPaidAmount(paidAmount);
        debt.setStatus(status);
        debt.setPaidAt(status == DebtStatus.PAID ? Instant.now() : null);
    }

    public static DebtSummaryResponse toSummary(Debt debt, List<Installment> installments, List<OpenLoanRecord> openRecords) {
        return switch (debt.getMethod()) {
            case INSTALLMENT -> summarizeInstallment(debt, installments);
            case OPEN -> summarizeOpen(debt, openRecords);
            case FULL -> summarizeFull(debt);
        };
    }

    public static DebtDetailResponse toDetail(Debt debt) {
        List<InstallmentResponse> installments = debt.getMethod() == DebtMethod.INSTALLMENT
                ? debt.getInstallments().stream().map(DebtCalculator::toInstallmentResponse).toList()
                : null;

        List<OpenLoanRecordResponse> openRecords = debt.getMethod() == DebtMethod.OPEN
                ? debt.getOpenLoanRecords().stream().map(DebtCalculator::toOpenRecordResponse).toList()
                : null;

        BigDecimal interest = debt.getMethod() == DebtMethod.OPEN
                ? latestOpenRecord(debt).getInterest()
                : null;

        BigDecimal interestPaidTotal = debt.getMethod() == DebtMethod.INSTALLMENT
                ? debt.getInstallments().stream()
                        .flatMap(installment -> installment.getInterestPayments().stream())
                        .map(InstallmentInterestPayment::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                : null;

        return new DebtDetailResponse(
                debt.getId(), debt.getCreditor().getUsername(), debt.getDebtor().getUsername(),
                debt.getTitle(), debt.getDescription(), debt.getMethod(),
                debt.getPrincipalAmount(), debt.getPaidAmount(), debt.getStatus(),
                debt.getStartDate(), debt.getInstallmentAmount(), debt.getInstallmentCount(),
                interest, interestPaidTotal, installments, openRecords);
    }

    public static Installment nextDueInstallment(List<Installment> installments) {
        return installments.stream()
                .filter(installment -> installment.getStatus() != InstallmentStatus.PAID)
                .min(Comparator.comparingInt(Installment::getNo))
                .orElse(null);
    }

    private static DebtSummaryResponse summarizeInstallment(Debt debt, List<Installment> installments) {
        long paidCount = installments.stream().filter(i -> i.getStatus() == InstallmentStatus.PAID).count();

        LocalDate lastPrincipalPayDate = installments.stream()
                .filter(i -> i.getStatus() == InstallmentStatus.PAID && i.getPayDate() != null)
                .map(Installment::getPayDate)
                .max(Comparator.naturalOrder())
                .orElse(null);
        LocalDate lastInterestPayDate = installments.stream()
                .flatMap(i -> i.getInterestPayments().stream())
                .map(InstallmentInterestPayment::getPayDate)
                .max(Comparator.naturalOrder())
                .orElse(null);
        LocalDate lastPayDate = Stream.of(lastPrincipalPayDate, lastInterestPayDate)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(null);

        Installment nextDue = nextDueInstallment(installments);
        boolean interestPaidThisPeriod = nextDue != null && nextDue.hasInterestPaidIn(BillingPeriod.current());

        BigDecimal dueAmount = nextDue != null ? nextDue.getAmount() : BigDecimal.ZERO;
        String dueLabel;
        if (nextDue == null) {
            dueLabel = "จ่ายครบแล้ว";
        } else if (interestPaidThisPeriod) {
            dueLabel = "ตัดดอกเดือนนี้แล้ว (ค้างงวดที่ " + nextDue.getNo() + ")";
        } else {
            dueLabel = "งวดที่ " + nextDue.getNo();
        }

        return new DebtSummaryResponse(
                debt.getId(), debt.getCreditor().getUsername(), debt.getDebtor().getUsername(),
                debt.getTitle(), debt.getDescription(), debt.getMethod(),
                debt.getPrincipalAmount(), debt.getInstallmentCount(), (int) paidCount,
                debt.getPaidAmount(), lastPayDate, debt.getStatus(),
                dueAmount, dueLabel, debt.getStatus() == DebtStatus.PAID || interestPaidThisPeriod, null,
                debt.getSortOrder(), debt.getCreatedAt());
    }

    private static BigDecimal openDueAmount(Debt debt, OpenLoanRecord record) {
        BigDecimal remaining = record.getRemainingPrincipal();
        if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        if (record.getInterest().compareTo(BigDecimal.ZERO) > 0) {
            return record.getInterest();
        }
        return debt.getInstallmentAmount() != null ? debt.getInstallmentAmount() : remaining;
    }

    private static DebtSummaryResponse summarizeOpen(Debt debt, List<OpenLoanRecord> openRecords) {
        OpenLoanRecord latest = openRecords.stream()
                .max(Comparator.comparingInt(OpenLoanRecord::getNo))
                .orElseThrow();

        BigDecimal remaining = latest.getRemainingPrincipal();
        BigDecimal dueAmount = openDueAmount(debt, latest);
        boolean currentPeriodPaid = remaining.compareTo(BigDecimal.ZERO) <= 0
                || openRecords.stream()
                        .sorted(Comparator.comparingInt(OpenLoanRecord::getNo))
                        .filter(record -> BillingPeriod.isCurrent(record.getPayDate()))
                        .findFirst()
                        .map(record -> record.getStatus() == PaymentStatus.PAID)
                        .orElse(false);
        String dueLabel;

        if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
            dueLabel = "จ่ายครบแล้ว";
        } else if (currentPeriodPaid) {
            dueLabel = "ชำระงวดเดือนนี้แล้ว (เงินต้นคงเหลือ " + formatAmount(remaining) + ")";
        } else if (latest.getInterest().compareTo(BigDecimal.ZERO) > 0) {
            dueLabel = "ดอกเบี้ยรายเดือน (เงินต้นคงเหลือ " + formatAmount(remaining) + ")";
        } else if (debt.getInstallmentAmount() != null) {
            dueLabel = "ยอดที่ต้องชำระ (เงินต้นคงเหลือ " + formatAmount(remaining) + ")";
        } else {
            dueLabel = "ชำระเงินต้นคงเหลือทั้งหมด";
        }

        LocalDate lastPayDate = openRecords.stream()
                .filter(r -> r.getStatus() == PaymentStatus.PAID)
                .map(OpenLoanRecord::getPayDate)
                .max(Comparator.naturalOrder())
                .orElse(null);

        return new DebtSummaryResponse(
                debt.getId(), debt.getCreditor().getUsername(), debt.getDebtor().getUsername(),
                debt.getTitle(), debt.getDescription(), debt.getMethod(),
                debt.getPrincipalAmount(), null, null,
                debt.getPaidAmount(), lastPayDate, debt.getStatus(),
                dueAmount, dueLabel, currentPeriodPaid, latest.getInterest(),
                debt.getSortOrder(), debt.getCreatedAt());
    }

    private static DebtSummaryResponse summarizeFull(Debt debt) {
        boolean paid = debt.getStatus() == DebtStatus.PAID;
        BigDecimal dueAmount = paid ? BigDecimal.ZERO : debt.getPrincipalAmount();
        String dueLabel = paid ? "จ่ายครบแล้ว" : "ชำระเต็มจำนวน";
        LocalDate lastPayDate = paid && debt.getPaidAt() != null
                ? debt.getPaidAt().atZone(ZONE).toLocalDate()
                : null;

        return new DebtSummaryResponse(
                debt.getId(), debt.getCreditor().getUsername(), debt.getDebtor().getUsername(),
                debt.getTitle(), debt.getDescription(), debt.getMethod(),
                debt.getPrincipalAmount(), null, null,
                debt.getPaidAmount(), lastPayDate, debt.getStatus(),
                dueAmount, dueLabel, paid, null, debt.getSortOrder(), debt.getCreatedAt());
    }

    private static InstallmentResponse toInstallmentResponse(Installment installment) {
        List<InstallmentInterestPayment> payments = installment.getInterestPayments();
        BigDecimal interestPaidTotal = payments.stream()
                .map(InstallmentInterestPayment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        LocalDate lastInterestPayDate = payments.stream()
                .map(InstallmentInterestPayment::getPayDate)
                .max(Comparator.naturalOrder())
                .orElse(null);

        return new InstallmentResponse(installment.getNo(), installment.getAmount(), installment.getKind(),
                installment.getStatus(), installment.getDueDate(), installment.getPayDate(), installment.getPaidAt(),
                payments.size(), interestPaidTotal, lastInterestPayDate);
    }

    private static OpenLoanRecordResponse toOpenRecordResponse(OpenLoanRecord record) {
        return new OpenLoanRecordResponse(record.getNo(), record.getPayDate(), record.getTotalPaid(),
                record.getInterest(), record.getRemainingPrincipal(), record.getStatus(), record.getPaidAt());
    }

    private static OpenLoanRecord latestOpenRecord(Debt debt) {
        return debt.getOpenLoanRecords().stream()
                .max(Comparator.comparingInt(OpenLoanRecord::getNo))
                .orElseThrow();
    }

    private static String formatAmount(BigDecimal amount) {
        return String.format(Locale.US, "%,.2f", amount);
    }
}
