package com.sharemoney.slip.dto;

import java.time.Instant;

public record SlipResponse(
        Long id,
        String debtorUsername,
        String creditorUsername,
        String filename,
        String url,
        String thumbnailUrl,
        long sizeBytes,
        Instant uploadedAt
) {
}
