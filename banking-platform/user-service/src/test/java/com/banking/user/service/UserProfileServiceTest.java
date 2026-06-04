package com.banking.user.service;

import com.banking.user.dto.UpdateUserProfileRequest;
import com.banking.user.entity.UserProfile;
import com.banking.user.exception.DuplicateUserException;
import com.banking.user.exception.UserNotFoundException;
import com.banking.user.repository.UserProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {

    @Mock private UserProfileRepository userProfileRepository;

    @InjectMocks private UserProfileService userProfileService;

    private UserProfile profile;
    private final String AUTH_ID = "auth-001";
    private final UUID PROFILE_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        profile = UserProfile.builder()
                .id(PROFILE_ID)
                .authId(AUTH_ID)
                .email("khanh@example.com")
                .username("khanh")
                .fullName("Khanh Tran")
                .phone("0901234567")
                .avatar("https://avatar.url/khanh.png")
                .kycStatus("PENDING")
                .build();
    }

    // ─── getProfileByAuthId() ─────────────────────────────────────────────────

    @Test
    @DisplayName("getProfileByAuthId: success — returns profile")
    void getProfileByAuthId_success() {
        when(userProfileRepository.findByAuthId(AUTH_ID)).thenReturn(Optional.of(profile));

        UserProfile result = userProfileService.getProfileByAuthId(AUTH_ID);

        assertNotNull(result);
        assertEquals(AUTH_ID, result.getAuthId());
        assertEquals("khanh@example.com", result.getEmail());
        verify(userProfileRepository).findByAuthId(AUTH_ID);
    }

    @Test
    @DisplayName("getProfileByAuthId: throws UserNotFoundException when not found")
    void getProfileByAuthId_throwsWhenNotFound() {
        when(userProfileRepository.findByAuthId("unknown")).thenReturn(Optional.empty());

        UserNotFoundException ex = assertThrows(UserNotFoundException.class,
                () -> userProfileService.getProfileByAuthId("unknown"));

        assertTrue(ex.getMessage().contains("unknown"));
    }

    // ─── getProfileById() ────────────────────────────────────────────────────

    @Test
    @DisplayName("getProfileById: success — returns profile")
    void getProfileById_success() {
        when(userProfileRepository.findById(PROFILE_ID)).thenReturn(Optional.of(profile));

        UserProfile result = userProfileService.getProfileById(PROFILE_ID);

        assertNotNull(result);
        assertEquals(PROFILE_ID, result.getId());
    }

    @Test
    @DisplayName("getProfileById: throws UserNotFoundException when not found")
    void getProfileById_throwsWhenNotFound() {
        UUID unknownId = UUID.randomUUID();
        when(userProfileRepository.findById(unknownId)).thenReturn(Optional.empty());

        UserNotFoundException ex = assertThrows(UserNotFoundException.class,
                () -> userProfileService.getProfileById(unknownId));

        assertTrue(ex.getMessage().contains(unknownId.toString()));
    }

    // ─── createProfile() ─────────────────────────────────────────────────────

    @Test
    @DisplayName("createProfile: success — saves and returns new profile")
    void createProfile_success() {
        UUID newId = UUID.randomUUID();
        when(userProfileRepository.existsByAuthId("new-auth")).thenReturn(false);
        when(userProfileRepository.save(any(UserProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        UserProfile result = userProfileService.createProfile(newId, "new-auth", "new@example.com", "newuser");

        assertNotNull(result);
        assertEquals("new-auth", result.getAuthId());
        assertEquals("new@example.com", result.getEmail());
        assertEquals("newuser", result.getUsername());
        assertEquals("PENDING", result.getKycStatus());

        ArgumentCaptor<UserProfile> captor = ArgumentCaptor.forClass(UserProfile.class);
        verify(userProfileRepository).save(captor.capture());
        assertEquals(newId, captor.getValue().getId());
    }

    @Test
    @DisplayName("createProfile: throws DuplicateUserException when authId already exists")
    void createProfile_throwsWhenDuplicate() {
        when(userProfileRepository.existsByAuthId(AUTH_ID)).thenReturn(true);

        DuplicateUserException ex = assertThrows(DuplicateUserException.class,
                () -> userProfileService.createProfile(UUID.randomUUID(), AUTH_ID, "khanh@example.com", "khanh"));

        assertTrue(ex.getMessage().contains(AUTH_ID));
        verify(userProfileRepository, never()).save(any());
    }

    // ─── updateProfile() ─────────────────────────────────────────────────────

    @Test
    @DisplayName("updateProfile: success — all fields updated")
    void updateProfile_allFieldsUpdated() {
        UpdateUserProfileRequest request = new UpdateUserProfileRequest(
                "New Name", "0999999999", "https://new-avatar.url");

        when(userProfileRepository.findByAuthId(AUTH_ID)).thenReturn(Optional.of(profile));
        when(userProfileRepository.save(any(UserProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        UserProfile result = userProfileService.updateProfile(AUTH_ID, request);

        assertEquals("New Name", result.getFullName());
        assertEquals("0999999999", result.getPhone());
        assertEquals("https://new-avatar.url", result.getAvatar());
        verify(userProfileRepository).save(profile);
    }

    @Test
    @DisplayName("updateProfile: partial update — only non-null fields changed")
    void updateProfile_partialUpdate() {
        UpdateUserProfileRequest request = new UpdateUserProfileRequest("Updated Name", null, null);

        when(userProfileRepository.findByAuthId(AUTH_ID)).thenReturn(Optional.of(profile));
        when(userProfileRepository.save(any(UserProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        UserProfile result = userProfileService.updateProfile(AUTH_ID, request);

        assertEquals("Updated Name", result.getFullName());
        assertEquals("0901234567", result.getPhone());           // unchanged
        assertEquals("https://avatar.url/khanh.png", result.getAvatar()); // unchanged
    }

    @Test
    @DisplayName("updateProfile: throws UserNotFoundException when user not found")
    void updateProfile_throwsWhenUserNotFound() {
        UpdateUserProfileRequest request = new UpdateUserProfileRequest("Name", null, null);
        when(userProfileRepository.findByAuthId("ghost")).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class,
                () -> userProfileService.updateProfile("ghost", request));

        verify(userProfileRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateProfile: edge case — all null fields, nothing changes")
    void updateProfile_allNullFields_nothingChanges() {
        UpdateUserProfileRequest request = new UpdateUserProfileRequest(null, null, null);

        when(userProfileRepository.findByAuthId(AUTH_ID)).thenReturn(Optional.of(profile));
        when(userProfileRepository.save(any(UserProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        UserProfile result = userProfileService.updateProfile(AUTH_ID, request);

        assertEquals("Khanh Tran", result.getFullName());
        assertEquals("0901234567", result.getPhone());
        assertEquals("https://avatar.url/khanh.png", result.getAvatar());
    }
}
