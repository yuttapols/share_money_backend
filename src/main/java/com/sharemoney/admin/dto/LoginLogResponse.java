package com.sharemoney.admin.dto;

public record LoginLogResponse(
        String timestamp,
        String username,
        String role,
        String action
) {
}
