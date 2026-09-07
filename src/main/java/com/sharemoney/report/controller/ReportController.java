package com.sharemoney.report.controller;

import com.sharemoney.common.response.ApiResponse;
import com.sharemoney.report.dto.DueReportResponse;
import com.sharemoney.report.service.DueReportPdfGenerator;
import com.sharemoney.report.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/reports")
@PreAuthorize("hasAnyRole('CREDITOR','DEBTOR')")
@RequiredArgsConstructor
public class ReportController {

    private static final ZoneId ZONE = ZoneId.of("Asia/Bangkok");
    private static final DateTimeFormatter FILENAME_DATE_FORMATTER = DateTimeFormatter.ofPattern("ddMMyyyy");

    private final ReportService reportService;
    private final DueReportPdfGenerator pdfGenerator;

    @GetMapping("/due")
    public ApiResponse<DueReportResponse> due(@RequestParam(defaultValue = "__all") String debtorUsername) {
        return ApiResponse.success(reportService.buildDueReport(debtorUsername));
    }

    @GetMapping("/due/pdf")
    public ResponseEntity<byte[]> duePdf(@RequestParam(defaultValue = "__all") String debtorUsername) {
        DueReportResponse report = reportService.buildDueReport(debtorUsername);
        byte[] pdf = pdfGenerator.generate(report);

        String filename = "Share_money_" + FILENAME_DATE_FORMATTER.format(LocalDate.now(ZONE)) + ".pdf";

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(pdf);
    }
}
