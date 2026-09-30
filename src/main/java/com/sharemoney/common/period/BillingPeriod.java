package com.sharemoney.common.period;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;

public final class BillingPeriod {

    private static final ZoneId ZONE = ZoneId.of("Asia/Bangkok");
    private static final int CUTOFF_DAY = 5;

    private BillingPeriod() {
    }

    // A billing period for month M runs from the 6th of M to the 5th of M+1,
    // so the 1st-5th of a month still belongs to the previous month's period.
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
