package com.sharemoney.profile.service;

import com.sharemoney.common.error.BusinessException;
import com.sharemoney.common.error.ErrorCode;
import com.sharemoney.common.security.SecurityUtils;
import com.sharemoney.common.storage.FileStorageService;
import com.sharemoney.common.storage.FileValidator;
import com.sharemoney.common.storage.StoredFile;
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

    @Transactional(readOnly = true)
    public ProfileResponse getCurrentProfile() {
        return toResponse(currentUser());
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

        if (StringUtils.hasText(user.getAvatarPublicId())) {
            fileStorageService.delete(user.getAvatarPublicId(), AVATAR_RESOURCE_TYPE);
        }

        StoredFile stored = fileStorageService.upload(file, FOLDER, false);
        user.setAvatarPublicId(stored.publicId());
        user.setAvatarUrl(stored.secureUrl());

        return new AvatarResponse(user.getAvatarUrl());
    }

    @Transactional
    public void deleteAvatar() {
        User user = currentUser();
        if (StringUtils.hasText(user.getAvatarPublicId())) {
            fileStorageService.delete(user.getAvatarPublicId(), AVATAR_RESOURCE_TYPE);
        }
        user.setAvatarPublicId(null);
        user.setAvatarUrl(null);
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
