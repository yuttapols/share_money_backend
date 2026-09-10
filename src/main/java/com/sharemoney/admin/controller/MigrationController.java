package com.sharemoney.admin.controller;

import com.sharemoney.admin.service.LegacyDataImportService;
import com.sharemoney.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/migration")
@RequiredArgsConstructor
public class MigrationController {

    private final LegacyDataImportService legacyDataImportService;

    @PostMapping(value = "/legacy-import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> importLegacyExcel(@RequestPart MultipartFile file) {
        legacyDataImportService.importLegacyExcel(file);
        return ApiResponse.success(null, "Legacy data has been imported successfully.");
    }
}
