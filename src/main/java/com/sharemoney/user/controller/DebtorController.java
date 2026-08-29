package com.sharemoney.user.controller;

import com.sharemoney.common.response.ApiResponse;
import com.sharemoney.user.dto.CreateDebtorRequest;
import com.sharemoney.user.dto.DebtorResponse;
import com.sharemoney.user.dto.DeleteDebtorResponse;
import com.sharemoney.user.dto.UpdateDebtorRequest;
import com.sharemoney.user.service.DebtorService;
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
@RequestMapping("/api/debtors")
@RequiredArgsConstructor
public class DebtorController {

    private final DebtorService debtorService;

    @PostMapping
    @PreAuthorize("hasRole('CREDITOR')")
    public ResponseEntity<ApiResponse<DebtorResponse>> create(@Valid @RequestBody CreateDebtorRequest request) {
        DebtorResponse response = debtorService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Debtor has been created successfully."));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','CREDITOR')")
    public ApiResponse<List<DebtorResponse>> list(@RequestParam(required = false) String search) {
        return ApiResponse.success(debtorService.list(search));
    }

    @PutMapping("/{username}")
    @PreAuthorize("hasAnyRole('ADMIN','CREDITOR')")
    public ApiResponse<DebtorResponse> update(@PathVariable String username, @Valid @RequestBody UpdateDebtorRequest request) {
        return ApiResponse.success(debtorService.update(username, request), "Data has been saved successfully.");
    }

    @DeleteMapping("/{username}")
    @PreAuthorize("hasAnyRole('ADMIN','CREDITOR')")
    public ApiResponse<DeleteDebtorResponse> delete(@PathVariable String username) {
        return ApiResponse.success(debtorService.delete(username), "Debtor has been deleted successfully.");
    }
}
