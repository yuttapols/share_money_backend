package com.sharemoney.admin.controller;

import com.sharemoney.admin.dto.LoginLogResponse;
import com.sharemoney.admin.service.LoginLogQueryService;
import com.sharemoney.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/login-logs")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class LoginLogController {

    private static final int MAX_LIMIT = 1000;

    private final LoginLogQueryService loginLogQueryService;

    @GetMapping
    public ApiResponse<List<LoginLogResponse>> list(@RequestParam(defaultValue = "200") int limit) {
        return ApiResponse.success(loginLogQueryService.list(Math.min(limit, MAX_LIMIT)));
    }
}
