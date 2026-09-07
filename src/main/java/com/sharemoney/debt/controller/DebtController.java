package com.sharemoney.debt.controller;

import com.sharemoney.common.response.ApiResponse;
import com.sharemoney.debt.dto.CreateDebtRequest;
import com.sharemoney.debt.dto.DebtDetailResponse;
import com.sharemoney.debt.dto.DebtSummaryResponse;
import com.sharemoney.debt.dto.FullPaidRequest;
import com.sharemoney.debt.dto.OpenRecordCreateRequest;
import com.sharemoney.debt.dto.OpenRecordUpdateRequest;
import com.sharemoney.debt.dto.PayInstallmentRequest;
import com.sharemoney.debt.dto.PayInterestRequest;
import com.sharemoney.debt.dto.ReorderRequest;
import com.sharemoney.debt.service.DebtService;
import com.sharemoney.debt.service.InstallmentPaymentService;
import com.sharemoney.debt.service.OpenLoanRecordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/debts")
@RequiredArgsConstructor
public class DebtController {

    private final DebtService debtService;
    private final InstallmentPaymentService installmentPaymentService;
    private final OpenLoanRecordService openLoanRecordService;

    @GetMapping
    @PreAuthorize("hasAnyRole('CREDITOR','DEBTOR')")
    public ApiResponse<List<DebtSummaryResponse>> list(@RequestParam(required = false) String debtorUsername,
                                                         @RequestParam(required = false) String search) {
        return ApiResponse.success(debtService.list(debtorUsername, search));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CREDITOR','DEBTOR')")
    public ApiResponse<DebtDetailResponse> detail(@PathVariable Long id) {
        return ApiResponse.success(debtService.getDetail(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('CREDITOR')")
    public ResponseEntity<ApiResponse<DebtDetailResponse>> create(@Valid @RequestBody CreateDebtRequest request) {
        DebtDetailResponse response = debtService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Debt has been created successfully."));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('CREDITOR')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        debtService.delete(id);
        return ApiResponse.success(null, "Debt has been deleted successfully.");
    }

    @PutMapping("/reorder")
    @PreAuthorize("hasRole('CREDITOR')")
    public ApiResponse<Void> reorder(@Valid @RequestBody ReorderRequest request) {
        debtService.reorder(request);
        return ApiResponse.success(null, "Data has been saved successfully.");
    }

    @PostMapping("/{id}/installments/{no}/pay")
    @PreAuthorize("hasRole('CREDITOR')")
    public ApiResponse<DebtDetailResponse> payInstallment(@PathVariable Long id, @PathVariable int no,
                                                            @Valid @RequestBody PayInstallmentRequest request) {
        return ApiResponse.success(installmentPaymentService.pay(id, no, request), "Data has been saved successfully.");
    }

    @PostMapping("/{id}/installments/{no}/pay-interest")
    @PreAuthorize("hasRole('CREDITOR')")
    public ApiResponse<DebtDetailResponse> payInterest(@PathVariable Long id, @PathVariable int no,
                                                         @Valid @RequestBody PayInterestRequest request) {
        return ApiResponse.success(installmentPaymentService.payInterest(id, no, request), "Data has been saved successfully.");
    }

    @PostMapping("/{id}/full-paid")
    @PreAuthorize("hasRole('CREDITOR')")
    public ApiResponse<DebtDetailResponse> fullPaid(@PathVariable Long id, @Valid @RequestBody FullPaidRequest request) {
        return ApiResponse.success(debtService.setFullPaid(id, request), "Data has been saved successfully.");
    }

    @PostMapping("/{id}/open-records")
    @PreAuthorize("hasRole('CREDITOR')")
    public ResponseEntity<ApiResponse<DebtDetailResponse>> addOpenRecord(@PathVariable Long id,
                                                                          @Valid @RequestBody OpenRecordCreateRequest request) {
        DebtDetailResponse response = openLoanRecordService.add(id, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Data has been saved successfully."));
    }

    @PutMapping("/{id}/open-records/{no}")
    @PreAuthorize("hasRole('CREDITOR')")
    public ApiResponse<DebtDetailResponse> updateOpenRecord(@PathVariable Long id, @PathVariable int no,
                                                             @Valid @RequestBody OpenRecordUpdateRequest request) {
        return ApiResponse.success(openLoanRecordService.update(id, no, request), "Data has been saved successfully.");
    }

    @DeleteMapping("/{id}/open-records/{no}")
    @PreAuthorize("hasRole('CREDITOR')")
    public ApiResponse<DebtDetailResponse> deleteOpenRecord(@PathVariable Long id, @PathVariable int no) {
        return ApiResponse.success(openLoanRecordService.delete(id, no), "Data has been deleted successfully.");
    }
}
