package com.skillmatch.userservice.dto.response;

import com.skillmatch.userservice.model.enums.UserRole;
import com.skillmatch.userservice.model.enums.UserStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public class UserResponse {

    private UUID id;
    private String keycloakId;
    private String email;
    private UserRole role;
    private UserStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
