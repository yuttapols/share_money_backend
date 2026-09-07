package com.sharemoney.report.service;

import com.sharemoney.common.error.BusinessException;
import com.sharemoney.common.error.ErrorCode;
import com.sharemoney.common.security.SecurityUtils;
import com.sharemoney.debt.dto.DebtSummaryResponse;
import com.sharemoney.debt.entity.DebtStatus;
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
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Bangkok");
    private static final DateTimeFormatter ISSUED_AT_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String ALL_DEBTORS = "__all";

    private final DebtService debtService;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public DueReportResponse buildDueReport(String debtorUsername) {
        String filter = ALL_DEBTORS.equalsIgnoreCase(debtorUsername) ? null : debtorUsername;
        List<DebtSummaryResponse> debts = debtService.list(filter, null);

        List<ReportLine> lines = debts.stream()
                .map(debt -> new ReportLine(debt.title(), debt.dueLabel(), debt.dueAmount(), debt.status() == DebtStatus.PAID))
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
}
