package com.skillmatch.paymentservice.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GenerationType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Each row is a commission-rate change, effective from its {@code effectiveFrom}
 * timestamp onward. The rate applied to a payment is the most recent row whose
 * effectiveFrom is not in the future — this preserves a full history of changes
 * (CLAUDE.md: "L'admin puo' cambiare [la commissione]") rather than mutating a
 * single row in place.
 */
@Entity
@Table(name = "commission_config")
@Getter
@Setter
public class CommissionConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "rate_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal ratePercentage;

    @CreationTimestamp
    @Column(name = "effective_from", nullable = false, updatable = false)
    private LocalDateTime effectiveFrom;

    @Column(name = "set_by_admin_id")
    private UUID setByAdminId;
}
