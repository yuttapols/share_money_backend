package com.sharemoney.auth.service;

import com.sharemoney.auth.dto.ChangePasswordRequest;
import com.sharemoney.auth.dto.LoginRequest;
import com.sharemoney.auth.dto.LoginResponse;
import com.sharemoney.auth.dto.MeResponse;
import com.sharemoney.auth.dto.RefreshRequest;
import com.sharemoney.auth.dto.TokenResponse;
import com.sharemoney.auth.entity.RefreshToken;
import com.sharemoney.auth.repository.RefreshTokenRepository;
import com.sharemoney.auth.security.TokenHasher;
import com.sharemoney.common.audit.AuditActions;
import com.sharemoney.common.audit.AuditLogService;
import com.sharemoney.common.domain.UserRole;
import com.sharemoney.common.error.BusinessException;
import com.sharemoney.common.error.ErrorCode;
import com.sharemoney.user.entity.User;
import com.sharemoney.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuditLogService auditLogService;
    private final LoginAttemptService loginAttemptService;

    @Value("${app.jwt.refresh-token-ttl-seconds}")
    private long refreshTokenTtlSeconds;

    @Transactional
    public LoginResponse login(LoginRequest request, String ipAddress) {
        loginAttemptService.assertNotLocked(request.username());

        User user = userRepository.findByUsernameIgnoreCase(request.username()).orElse(null);
        if (user == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            loginAttemptService.recordFailure(request.username());
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }
        if (!user.isActive()) {
            throw new BusinessException(ErrorCode.ACCOUNT_DISABLED);
        }

        loginAttemptService.recordSuccess(request.username());

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = issueRefreshToken(user);

        auditLogService.record(user.getUsername(), user.getRole(), AuditActions.LOGIN, ipAddress);

        return new LoginResponse(accessToken, refreshToken, jwtService.getAccessTokenTtlSeconds(),
                user.getRole(), user.getUsername(), user.getFullName());
    }

    @Transactional
    public TokenResponse refresh(RefreshRequest request) {
        RefreshToken stored = refreshTokenRepository.findByTokenHash(TokenHasher.sha256(request.refreshToken()))
                .orElseThrow(() -> new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID));

        if (stored.isRevoked()) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID);
        }
        if (stored.getExpiresAt().isBefore(Instant.now())) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_EXPIRED);
        }

        stored.setRevoked(true);
        User user = stored.getUser();

        String accessToken = jwtService.generateAccessToken(user);
        String newRefreshToken = issueRefreshToken(user);

        return new TokenResponse(accessToken, newRefreshToken, jwtService.getAccessTokenTtlSeconds());
    }

    @Transactional
    public void logout(String refreshToken, String username, UserRole role, String ipAddress) {
        if (StringUtils.hasText(refreshToken)) {
            refreshTokenRepository.findByTokenHash(TokenHasher.sha256(refreshToken))
                    .ifPresent(token -> token.setRevoked(true));
        }
        auditLogService.record(username, role, AuditActions.LOGOUT, ipAddress);
    }

    @Transactional(readOnly = true)
    public MeResponse getCurrentUser(Long userId) {
        return userRepository.findById(userId)
                .map(user -> new MeResponse(user.getUsername(), user.getRole(), user.getFullName()))
                .orElse(null);
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(request.oldPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.OLD_PASSWORD_INCORRECT);
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
    }

    private String issueRefreshToken(User user) {
        String rawToken = UUID.randomUUID().toString();

        RefreshToken entity = new RefreshToken();
        entity.setUser(user);
        entity.setTokenHash(TokenHasher.sha256(rawToken));
        entity.setExpiresAt(Instant.now().plusSeconds(refreshTokenTtlSeconds));
        refreshTokenRepository.save(entity);

        return rawToken;
    }
}
