package com.banking.user.controller;

import com.banking.user.dto.ApiResponse;
import com.banking.user.dto.UpdateUserProfileRequest;
import com.banking.user.dto.UserProfileDto;
import com.banking.user.entity.UserProfile;
import com.banking.user.service.UserProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserProfileService userProfileService;

    @GetMapping("/health")
    public String health() {
        return "USER-SERVICE OK";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin")
    public String adminOnly() {
        return "USER-SERVICE ADMIN ONLY";
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileDto>> getCurrentUserProfile(@AuthenticationPrincipal Jwt jwt) {
        String authId = jwt.getSubject();
        UserProfile profile = userProfileService.getProfileByAuthId(authId);
        return ResponseEntity.ok(ApiResponse.success("Profile retrieved", mapToDto(profile)));
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileDto>> updateCurrentUserProfile(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdateUserProfileRequest request) {
        String authId = jwt.getSubject();
        UserProfile updatedProfile = userProfileService.updateProfile(authId, request);
        return ResponseEntity.ok(ApiResponse.success("Profile updated", mapToDto(updatedProfile)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserProfileDto>> getProfileById(@PathVariable UUID id) {
        UserProfile profile = userProfileService.getProfileById(id);
        return ResponseEntity.ok(ApiResponse.success("Profile retrieved", mapToDto(profile)));
    }

    private UserProfileDto mapToDto(UserProfile profile) {
        return UserProfileDto.builder()
                .id(profile.getId())
                .authId(profile.getAuthId())
                .email(profile.getEmail())
                .username(profile.getUsername())
                .fullName(profile.getFullName())
                .phone(profile.getPhone())
                .avatar(profile.getAvatar())
                .kycStatus(profile.getKycStatus())
                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt())
                .build();
    }
}
