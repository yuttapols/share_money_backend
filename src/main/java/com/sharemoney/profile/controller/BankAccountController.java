package com.sharemoney.profile.controller;

import com.sharemoney.common.response.ApiResponse;
import com.sharemoney.profile.dto.BankAccountResponse;
import com.sharemoney.profile.dto.UpdateBankAccountRequest;
import com.sharemoney.profile.service.BankAccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bank-accounts/me")
@RequiredArgsConstructor
public class BankAccountController {

    private final BankAccountService bankAccountService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','CREDITOR','DEBTOR')")
    public ApiResponse<BankAccountResponse> get() {
        return ApiResponse.success(bankAccountService.getMine());
    }

    @PutMapping
    @PreAuthorize("hasRole('CREDITOR')")
    public ApiResponse<BankAccountResponse> update(@Valid @RequestBody UpdateBankAccountRequest request) {
        return ApiResponse.success(bankAccountService.update(request), "Data has been saved successfully.");
    }
}
