package com.sharemoney.user.dto;

public record DebtorResponse(
        Long id,
        String username,
        String name,
        String creditorUsername,
        boolean active
) {
}
