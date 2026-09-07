package com.sharemoney.profile.controller;

import com.sharemoney.common.response.ApiResponse;
import com.sharemoney.profile.dto.AvatarResponse;
import com.sharemoney.profile.dto.ProfileResponse;
import com.sharemoney.profile.dto.UpdateProfileRequest;
import com.sharemoney.profile.service.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/profile/me")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping
    public ApiResponse<ProfileResponse> me() {
        return ApiResponse.success(profileService.getCurrentProfile());
    }

    @PutMapping
    public ApiResponse<ProfileResponse> update(@Valid @RequestBody UpdateProfileRequest request) {
        return ApiResponse.success(profileService.updateProfile(request), "Data has been saved successfully.");
    }

    @PostMapping("/avatar")
    public ApiResponse<AvatarResponse> uploadAvatar(@RequestPart MultipartFile file) {
        return ApiResponse.success(profileService.uploadAvatar(file), "Avatar has been uploaded successfully.");
    }

    @DeleteMapping("/avatar")
    public ApiResponse<Void> deleteAvatar() {
        profileService.deleteAvatar();
        return ApiResponse.success(null, "Avatar has been deleted successfully.");
    }
}
