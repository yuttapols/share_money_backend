package com.sharemoney.common.period;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;

public final class BillingPeriod {

    private static final ZoneId ZONE = ZoneId.of("Asia/Bangkok");
    private static final int CUTOFF_DAY = 10;

    private BillingPeriod() {
    }

    // A billing period for month M runs from the 11th of M to the 10th of M+1,
    // so the 1st-10th of a month still belongs to the previous month's period.
    public static YearMonth of(LocalDate date) {
        YearMonth month = YearMonth.from(date);
        return date.getDayOfMonth() <= CUTOFF_DAY ? month.minusMonths(1) : month;
    }

    public static YearMonth current() {
        return of(LocalDate.now(ZONE));
    }

    public static boolean isCurrent(LocalDate date) {
        return date != null && of(date).equals(current());
    }
}
