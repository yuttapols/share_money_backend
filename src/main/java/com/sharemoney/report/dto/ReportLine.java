package com.sharemoney.report.dto;

import java.math.BigDecimal;

public record ReportLine(
        String title,
        String what,
        BigDecimal due,
        boolean paid
) {
}
