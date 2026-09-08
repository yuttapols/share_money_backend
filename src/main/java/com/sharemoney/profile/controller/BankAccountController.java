package com.sharemoney.profile.controller;

import com.sharemoney.common.response.ApiResponse;
import com.sharemoney.profile.dto.BankAccountResponse;
import com.sharemoney.profile.dto.CreateBankAccountRequest;
import com.sharemoney.profile.dto.UpdateBankAccountRequest;
import com.sharemoney.profile.service.BankAccountService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/bank-accounts")
@RequiredArgsConstructor
public class BankAccountController {

    private final BankAccountService bankAccountService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','CREDITOR','DEBTOR')")
    public ApiResponse<List<BankAccountResponse>> list() {
        return ApiResponse.success(bankAccountService.list());
    }

    @PostMapping
    @PreAuthorize("hasRole('CREDITOR')")
    public ResponseEntity<ApiResponse<BankAccountResponse>> create(@Valid @RequestBody CreateBankAccountRequest request) {
        BankAccountResponse response = bankAccountService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Data has been saved successfully."));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('CREDITOR')")
    public ApiResponse<BankAccountResponse> update(@PathVariable Long id, @Valid @RequestBody UpdateBankAccountRequest request) {
        return ApiResponse.success(bankAccountService.update(id, request), "Data has been saved successfully.");
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('CREDITOR')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        bankAccountService.delete(id);
        return ApiResponse.success(null, "Data has been deleted successfully.");
    }
}
