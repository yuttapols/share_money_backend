package com.sharemoney.profile.service;

import com.sharemoney.common.audit.AuditActions;
import com.sharemoney.common.audit.AuditLogService;
import com.sharemoney.common.error.BusinessException;
import com.sharemoney.common.error.ErrorCode;
import com.sharemoney.common.security.SecurityUtils;
import com.sharemoney.common.storage.FileStorageService;
import com.sharemoney.common.storage.FileValidator;
import com.sharemoney.common.storage.StoredFile;
import com.sharemoney.common.storage.TransactionalFileCleanup;
import com.sharemoney.profile.dto.AvatarResponse;
import com.sharemoney.profile.dto.ProfileResponse;
import com.sharemoney.profile.dto.UpdateProfileRequest;
import com.sharemoney.user.entity.User;
import com.sharemoney.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private static final long MAX_AVATAR_BYTES = 2L * 1024 * 1024;
    private static final String AVATAR_RESOURCE_TYPE = "image";
    private static final String FOLDER = "sharemoney/avatars";

    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final TransactionalFileCleanup fileCleanup;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public ProfileResponse getCurrentProfile() {
        return userRepository.findById(SecurityUtils.currentUserId())
                .map(this::toResponse)
                .orElse(null);
    }

    @Transactional
    public ProfileResponse updateProfile(UpdateProfileRequest request) {
        User user = currentUser();
        user.setFullName(request.name());
        user.setPhone(request.phone());
        user.setPreferredLanguage(request.preferredLanguage());
        return toResponse(user);
    }

    @Transactional
    public AvatarResponse uploadAvatar(MultipartFile file) {
        FileValidator.assertImage(file, MAX_AVATAR_BYTES);
        User user = currentUser();
        String previousPublicId = user.getAvatarPublicId();

        StoredFile stored = fileStorageService.upload(file, FOLDER, false);
        fileCleanup.deleteOnRollback(stored.publicId(), AVATAR_RESOURCE_TYPE, false);

        user.setAvatarPublicId(stored.publicId());
        user.setAvatarUrl(stored.secureUrl());

        if (StringUtils.hasText(previousPublicId)) {
            fileCleanup.deleteAfterCommit(previousPublicId, AVATAR_RESOURCE_TYPE, false);
        }

        auditLogService.record(user.getUsername(), user.getRole(), AuditActions.UPLOAD_AVATAR, null);

        return new AvatarResponse(user.getAvatarUrl());
    }

    @Transactional
    public void deleteAvatar() {
        User user = currentUser();
        String publicId = user.getAvatarPublicId();
        user.setAvatarPublicId(null);
        user.setAvatarUrl(null);

        if (StringUtils.hasText(publicId)) {
            fileCleanup.deleteAfterCommit(publicId, AVATAR_RESOURCE_TYPE, false);
            auditLogService.record(user.getUsername(), user.getRole(), AuditActions.DELETE_AVATAR, null);
        }
    }

    private User currentUser() {
        return userRepository.findById(SecurityUtils.currentUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    private ProfileResponse toResponse(User user) {
        return new ProfileResponse(user.getUsername(), user.getFullName(), user.getRole(),
                user.getPhone(), user.getAvatarUrl(), user.getPreferredLanguage());
    }
}
