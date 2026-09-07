package com.sharemoney.slip.controller;

import com.sharemoney.common.response.ApiResponse;
import com.sharemoney.slip.dto.DebtorsWithSlipResponse;
import com.sharemoney.slip.dto.SlipResponse;
import com.sharemoney.slip.service.SlipService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/slips")
@RequiredArgsConstructor
public class SlipController {

    private final SlipService slipService;

    @PostMapping
    @PreAuthorize("hasRole('DEBTOR')")
    public ResponseEntity<ApiResponse<SlipResponse>> upload(@RequestPart String creditorUsername,
                                                              @RequestPart MultipartFile file) {
        SlipResponse response = slipService.upload(creditorUsername, file);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Slip has been uploaded successfully."));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','CREDITOR','DEBTOR')")
    public ApiResponse<List<SlipResponse>> list(@RequestParam String debtorUsername,
                                                 @RequestParam String creditorUsername) {
        return ApiResponse.success(slipService.list(debtorUsername, creditorUsername));
    }

    @GetMapping("/debtors")
    @PreAuthorize("hasAnyRole('ADMIN','CREDITOR')")
    public ApiResponse<DebtorsWithSlipResponse> debtorsWithSlip() {
        return ApiResponse.success(slipService.debtorsWithSlip());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','CREDITOR')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        slipService.delete(id);
        return ApiResponse.success(null, "Slip has been deleted successfully.");
    }
}
