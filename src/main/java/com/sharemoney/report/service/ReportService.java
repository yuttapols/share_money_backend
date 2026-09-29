package com.sharemoney.report.service;

import com.sharemoney.common.error.BusinessException;
import com.sharemoney.common.error.ErrorCode;
import com.sharemoney.common.security.SecurityUtils;
import com.sharemoney.debt.dto.DebtSummaryResponse;
import com.sharemoney.debt.entity.DebtMethod;
import com.sharemoney.debt.entity.DebtStatus;
import com.sharemoney.debt.entity.OpenLoanRecord;
import com.sharemoney.debt.entity.PaymentStatus;
import com.sharemoney.debt.repository.OpenLoanRecordRepository;
import com.sharemoney.debt.service.DebtService;
import com.sharemoney.report.dto.DueReportResponse;
import com.sharemoney.report.dto.ReportLine;
import com.sharemoney.user.entity.User;
import com.sharemoney.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Bangkok");
    private static final DateTimeFormatter ISSUED_AT_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String ALL_DEBTORS = "__all";
    private static final int GRACE_PERIOD_DAY = 5;

    private final DebtService debtService;
    private final UserRepository userRepository;
    private final OpenLoanRecordRepository openLoanRecordRepository;

    @Transactional(readOnly = true)
    public DueReportResponse buildDueReport(String debtorUsername) {
        String filter = ALL_DEBTORS.equalsIgnoreCase(debtorUsername) ? null : debtorUsername;
        List<DebtSummaryResponse> debts = debtService.list(filter, null);

        Map<Long, OpenLoanRecord> currentDueRecordByDebtId = findCurrentDueRecordsForOpenDebts(debts);

        List<ReportLine> lines = debts.stream()
                .map(debt -> toReportLine(debt, currentDueRecordByDebtId.get(debt.id())))
                .toList();

        long dueCount = lines.stream().filter(line -> !line.paid()).count();
        BigDecimal total = lines.stream()
                .filter(line -> !line.paid())
                .map(ReportLine::due)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        User currentUser = userRepository.findById(SecurityUtils.currentUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        String issuedAt = ISSUED_AT_FORMATTER.format(LocalDate.now(ZONE));

        return new DueReportResponse(issuedAt, currentUser.getFullName(), lines, (int) dueCount, total);
    }

    private ReportLine toReportLine(DebtSummaryResponse debt, OpenLoanRecord currentDueRecord) {
        if (debt.method() == DebtMethod.OPEN && currentDueRecord != null) {
            boolean paid = currentDueRecord.getStatus() == PaymentStatus.PAID;
            return new ReportLine(debt.title(), debt.dueLabel(), debt.dueAmount(), paid);
        }
        // OPEN debts with no record matching "current due" (e.g. paid ahead of schedule, or this
        // month's record hasn't been added yet) fall back to the existing latest-record-based amount
        // instead of hiding the row or showing 0 — a due report should never silently drop a debt that
        // might still need follow-up.
        return new ReportLine(debt.title(), debt.dueLabel(), debt.dueAmount(), debt.status() == DebtStatus.PAID);
    }

    private Map<Long, OpenLoanRecord> findCurrentDueRecordsForOpenDebts(List<DebtSummaryResponse> debts) {
        List<Long> openDebtIds = debts.stream()
                .filter(debt -> debt.method() == DebtMethod.OPEN)
                .map(DebtSummaryResponse::id)
                .toList();

        if (openDebtIds.isEmpty()) {
            return Map.of();
        }

        LocalDate today = LocalDate.now(ZONE);

        return openLoanRecordRepository.findByDebt_IdIn(openDebtIds).stream()
                .collect(Collectors.groupingBy(record -> record.getDebt().getId()))
                .entrySet().stream()
                .map(entry -> Map.entry(entry.getKey(), findCurrentDueRecord(entry.getValue(), today)))
                .filter(entry -> entry.getValue() != null)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private OpenLoanRecord findCurrentDueRecord(List<OpenLoanRecord> records, LocalDate today) {
        return records.stream()
                .sorted(Comparator.comparingInt(OpenLoanRecord::getNo))
                .filter(record -> isCurrentDueRecord(record.getPayDate(), today))
                .findFirst()
                .orElse(null);
    }

    private boolean isCurrentDueRecord(LocalDate payDate, LocalDate today) {
        int monthsAhead = (payDate.getYear() - today.getYear()) * 12 + (payDate.getMonthValue() - today.getMonthValue());
        if (monthsAhead == 0) {
            return true;
        }
        return monthsAhead == 1 && payDate.getDayOfMonth() <= GRACE_PERIOD_DAY;
    }
}
