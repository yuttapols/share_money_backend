package com.sharemoney.admin.service;

import com.sharemoney.common.audit.AuditActions;
import com.sharemoney.common.audit.AuditLogService;
import com.sharemoney.common.error.BusinessException;
import com.sharemoney.common.error.ErrorCode;
import com.sharemoney.common.security.SecurityUtils;
import com.sharemoney.migration.LegacyDataMigrationTool;
import lombok.RequiredArgsConstructor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.sql.DataSource;
import java.io.InputStream;
import java.sql.Connection;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class LegacyDataImportService {

    private static final Set<String> EXCEL_CONTENT_TYPES = Set.of(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    );
    private static final long MAX_EXCEL_BYTES = 10L * 1024 * 1024;

    private final DataSource dataSource;
    private final AuditLogService auditLogService;

    public void importLegacyExcel(MultipartFile file) {
        assertExcelFile(file);

        try (InputStream is = file.getInputStream();
             XSSFWorkbook workbook = new XSSFWorkbook(is);
             Connection connection = dataSource.getConnection()) {

            connection.setAutoCommit(false);
            LegacyDataMigrationTool.of(connection, workbook).run();
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.LEGACY_IMPORT_FAILED, e.getMessage());
        }

        auditLogService.record(SecurityUtils.currentUsername(), SecurityUtils.currentRole(),
                AuditActions.IMPORT_LEGACY_DATA, null);
    }

    private void assertExcelFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.EMPTY_FILE);
        }
        String contentType = file.getContentType();
        if (contentType == null || !EXCEL_CONTENT_TYPES.contains(contentType)) {
            throw new BusinessException(ErrorCode.INVALID_FILE_TYPE);
        }
        if (file.getSize() > MAX_EXCEL_BYTES) {
            throw new BusinessException(ErrorCode.FILE_TOO_LARGE);
        }
    }
}
