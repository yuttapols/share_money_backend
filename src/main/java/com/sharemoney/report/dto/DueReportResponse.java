package com.sharemoney.report.dto;

import java.math.BigDecimal;
import java.util.List;

public record DueReportResponse(
        String issuedAt,
        String issuedBy,
        List<ReportLine> lines,
        int dueCount,
        BigDecimal total
) {
}
