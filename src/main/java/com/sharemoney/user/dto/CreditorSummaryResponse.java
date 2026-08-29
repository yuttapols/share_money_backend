package com.sharemoney.user.dto;

public record CreditorSummaryResponse(
        Long id,
        String username,
        String name,
        boolean active,
        long debtorCount
) {
}
