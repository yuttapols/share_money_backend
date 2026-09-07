package com.sharemoney.common.storage;

import com.sharemoney.common.error.BusinessException;
import com.sharemoney.common.error.ErrorCode;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

public final class FileValidator {

    private static final Set<String> DOCUMENT_CONTENT_TYPES = Set.of(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );

    private FileValidator() {
    }

    public static void assertImage(MultipartFile file, long maxBytes) {
        assertNotEmpty(file);
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BusinessException(ErrorCode.INVALID_FILE_TYPE);
        }
        assertMaxSize(file, maxBytes);
    }

    public static void assertDocument(MultipartFile file, long maxBytes) {
        assertNotEmpty(file);
        String contentType = file.getContentType();
        if (contentType == null || !DOCUMENT_CONTENT_TYPES.contains(contentType)) {
            throw new BusinessException(ErrorCode.INVALID_FILE_TYPE);
        }
        assertMaxSize(file, maxBytes);
    }

    private static void assertNotEmpty(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.EMPTY_FILE);
        }
    }

    private static void assertMaxSize(MultipartFile file, long maxBytes) {
        if (file.getSize() > maxBytes) {
            throw new BusinessException(ErrorCode.FILE_TOO_LARGE);
        }
    }
}
