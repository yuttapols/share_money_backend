package com.sharemoney.slip.dto;

import java.util.List;

public record DebtorsWithSlipResponse(
        List<String> debtorUsernames
) {
}
