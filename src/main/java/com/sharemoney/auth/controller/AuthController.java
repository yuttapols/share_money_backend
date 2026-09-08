package com.sharemoney.auth.controller;

import com.sharemoney.auth.dto.ChangePasswordRequest;
import com.sharemoney.auth.dto.LoginRequest;
import com.sharemoney.auth.dto.LoginResponse;
import com.sharemoney.auth.dto.MeResponse;
import com.sharemoney.auth.dto.RefreshRequest;
import com.sharemoney.auth.dto.TokenResponse;
import com.sharemoney.auth.service.AuthService;
import com.sharemoney.common.response.ApiResponse;
import com.sharemoney.common.security.SecurityUtils;
import com.sharemoney.common.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        LoginResponse response = authService.login(request, httpRequest.getRemoteAddr());
        return ApiResponse.success(response, "Login successful.");
    }

    @PostMapping("/refresh")
    public ApiResponse<TokenResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ApiResponse.success(authService.refresh(request));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestBody(required = false) RefreshRequest request, HttpServletRequest httpRequest) {
        UserPrincipal principal = SecurityUtils.currentUser();
        String refreshToken = request == null ? null : request.refreshToken();
        authService.logout(refreshToken, principal.getUsername(), principal.getRole(), httpRequest.getRemoteAddr());
        return ApiResponse.success(null, "Logout successful.");
    }

    @GetMapping("/me")
    public ApiResponse<MeResponse> me() {
        return ApiResponse.success(authService.getCurrentUser(SecurityUtils.currentUserId()));
    }

    @PostMapping("/change-password")
    public ApiResponse<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(SecurityUtils.currentUserId(), request);
        return ApiResponse.success(null, "Password changed successfully.");
    }
}
