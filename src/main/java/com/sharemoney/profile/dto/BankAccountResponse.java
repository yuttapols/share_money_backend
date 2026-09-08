package com.sharemoney.profile.dto;

public record BankAccountResponse(
        Long id,
        String bankName,
        String accountNo,
        String accountName,
        String paymentNote
) {
}
