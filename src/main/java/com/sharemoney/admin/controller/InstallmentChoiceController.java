package com.sharemoney.admin.controller;

import com.sharemoney.admin.dto.AddInstallmentChoiceRequest;
import com.sharemoney.admin.dto.InstallmentChoicesResponse;
import com.sharemoney.admin.service.InstallmentChoiceService;
import com.sharemoney.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/installment-choices")
@RequiredArgsConstructor
public class InstallmentChoiceController {

    private final InstallmentChoiceService installmentChoiceService;

    @GetMapping
    public ApiResponse<InstallmentChoicesResponse> list() {
        return ApiResponse.success(new InstallmentChoicesResponse(installmentChoiceService.getChoices()));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<InstallmentChoicesResponse> add(@Valid @RequestBody AddInstallmentChoiceRequest request) {
        InstallmentChoicesResponse response = new InstallmentChoicesResponse(installmentChoiceService.addChoice(request.count()));
        return ApiResponse.success(response, "Data has been saved successfully.");
    }

    @DeleteMapping("/{count}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<InstallmentChoicesResponse> remove(@PathVariable int count) {
        InstallmentChoicesResponse response = new InstallmentChoicesResponse(installmentChoiceService.removeChoice(count));
        return ApiResponse.success(response, "Data has been deleted successfully.");
    }
}
