package com.sharemoney.document.dto;

public record DocumentResponse(
        Long id,
        String title,
        String scope,
        String debtorUsername,
        String url
) {
}
