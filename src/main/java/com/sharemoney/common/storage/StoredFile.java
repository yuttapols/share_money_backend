package com.sharemoney.common.storage;

public record StoredFile(
        String publicId,
        String secureUrl,
        String originalFilename,
        String format,
        String resourceType,
        long bytes
) {
}
