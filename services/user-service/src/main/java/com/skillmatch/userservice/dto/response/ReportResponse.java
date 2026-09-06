package com.skillmatch.userservice.dto.response;

import com.skillmatch.userservice.model.enums.ReportStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public class ReportResponse {

    private UUID id;
    private UUID reporterId;
    private String reporterEmail;
    private UUID reportedUserId;
    private String reason;
    private ReportStatus status;
    private LocalDateTime createdAt;
}
