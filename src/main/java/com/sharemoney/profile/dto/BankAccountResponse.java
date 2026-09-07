package com.sharemoney.profile.dto;

public record BankAccountResponse(
        String bankName,
        String accountNo,
        String accountName,
        String paymentNote
) {
}
