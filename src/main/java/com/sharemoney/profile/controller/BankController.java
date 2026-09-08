package com.sharemoney.profile.controller;

import com.sharemoney.common.response.ApiResponse;
import com.sharemoney.profile.dto.BankResponse;
import com.sharemoney.profile.service.BankService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/banks")
@RequiredArgsConstructor
public class BankController {

    private final BankService bankService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','CREDITOR','DEBTOR')")
    public ApiResponse<List<BankResponse>> list() {
        return ApiResponse.success(bankService.list());
    }
}
