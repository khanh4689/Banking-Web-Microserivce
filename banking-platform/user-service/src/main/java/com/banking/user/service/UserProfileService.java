package com.banking.user.service;

import com.banking.user.dto.UpdateUserProfileRequest;
import com.banking.user.entity.UserProfile;
import com.banking.user.exception.DuplicateUserException;
import com.banking.user.exception.UserNotFoundException;
import com.banking.user.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserProfileService {

    private final UserProfileRepository userProfileRepository;

    public UserProfile getProfileByAuthId(String authId) {
        log.info("Get profile by authId={}", authId);
        UserProfile profile = userProfileRepository.findByAuthId(authId)
                .orElseThrow(() -> {
                    log.warn("User profile not found for authId={}", authId);
                    return new UserNotFoundException("User profile not found for authId: " + authId);
                });
        log.info("Profile found for authId={}", authId);
        return profile;
    }

    public UserProfile getProfileById(UUID id) {
        log.info("Get profile by id={}", id);
        UserProfile profile = userProfileRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("User profile not found for id={}", id);
                    return new UserNotFoundException("User profile not found for id: " + id);
                });
        log.info("Profile found for id={}", id);
        return profile;
    }

    @Transactional
    public UserProfile updateProfile(String authId, UpdateUserProfileRequest request) {
        log.info("Update profile request for authId={}", authId);

        UserProfile profile = getProfileByAuthId(authId);

        if (request.getFullName() != null) {
            profile.setFullName(request.getFullName());
        }
        if (request.getPhone() != null) {
            profile.setPhone(request.getPhone());
        }
        if (request.getAvatar() != null) {
            profile.setAvatar(request.getAvatar());
        }

        UserProfile saved = userProfileRepository.save(profile);
        log.info("Profile updated successfully for authId={}", authId);
        return saved;
    }

    /**
     * Creates a user profile from a Kafka UserCreatedEvent.
     * Throws DuplicateUserException if profile already exists — caller decides whether to skip or fail.
     */
    @Transactional
    public UserProfile createProfile(UUID id, String authId, String email, String username) {
        log.info("Creating user profile for authId={}, email={}", authId, email);

        if (userProfileRepository.existsByAuthId(authId)) {
            log.warn("Duplicate profile detected for authId={}", authId);
            throw new DuplicateUserException("User profile already exists for authId: " + authId);
        }

        UserProfile profile = UserProfile.builder()
                .id(id)
                .authId(authId)
                .email(email)
                .username(username)
                .kycStatus("PENDING")
                .build();

        UserProfile saved = userProfileRepository.save(profile);
        log.info("User profile created for authId={}", authId);
        return saved;
    }
}