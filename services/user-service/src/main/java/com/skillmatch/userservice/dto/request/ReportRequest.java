package com.skillmatch.userservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class ReportRequest {

    @NotNull(message = "Reported user id is required")
    private UUID reportedUserId;

    @NotBlank(message = "Reason is required")
    @Size(max = 2000, message = "Reason must not exceed 2000 characters")
    private String reason;
}
