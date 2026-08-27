package com.skillmatch.feedbackservice.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GenerationType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Marks a project as eligible for mutual feedback, created when this service consumes
 * the payment.completed event. Caches the project's two parties (company, professional)
 * so a feedback submission can be authorized and routed to the correct reviewee without
 * a synchronous call back to Project/Contract Service.
 */
@Entity
@Table(name = "feedback_eligibility")
@Getter
@Setter
public class FeedbackEligibility {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "project_id", nullable = false, unique = true)
    private UUID projectId;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "professional_id", nullable = false)
    private UUID professionalId;

    @CreationTimestamp
    @Column(name = "enabled_at", nullable = false, updatable = false)
    private LocalDateTime enabledAt;
}
