package com.skillmatch.userservice.repository;

import com.skillmatch.userservice.model.Report;
import com.skillmatch.userservice.model.enums.ReportStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReportRepository extends JpaRepository<Report, UUID> {

    List<Report> findByReportedUserIdOrderByCreatedAtDesc(UUID reportedUserId);

    long countByReportedUserIdAndStatus(UUID reportedUserId, ReportStatus status);
}
