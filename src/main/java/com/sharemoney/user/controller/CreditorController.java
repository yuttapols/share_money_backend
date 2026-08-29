package com.sharemoney.user.controller;

import com.sharemoney.common.response.ApiResponse;
import com.sharemoney.user.dto.CreateCreditorRequest;
import com.sharemoney.user.dto.CreditorResponse;
import com.sharemoney.user.dto.CreditorSummaryResponse;
import com.sharemoney.user.service.CreditorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/creditors")
@RequiredArgsConstructor
public class CreditorController {

    private final CreditorService creditorService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CreditorResponse>> create(@Valid @RequestBody CreateCreditorRequest request) {
        CreditorResponse response = creditorService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Creditor has been created successfully."));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<CreditorSummaryResponse>> list() {
        return ApiResponse.success(creditorService.list());
    }
}
