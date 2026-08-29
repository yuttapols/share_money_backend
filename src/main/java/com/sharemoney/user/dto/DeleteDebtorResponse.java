package com.sharemoney.user.dto;

public record DeleteDebtorResponse(
        boolean ok,
        long deletedDebtCount
) {
}
