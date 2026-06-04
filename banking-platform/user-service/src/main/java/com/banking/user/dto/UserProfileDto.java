package com.banking.user.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserProfileDto {
    private UUID id;
    private String authId;
    private String email;
    private String username;
    private String fullName;
    private String phone;
    private String avatar;
    private String kycStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
