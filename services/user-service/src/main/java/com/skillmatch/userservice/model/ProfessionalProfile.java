package com.skillmatch.userservice.model;

import com.skillmatch.userservice.model.enums.ReputationLevel;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "professional_profiles")
@Getter
@Setter
public class ProfessionalProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    private User user;

    @Column(name = "first_name", length = 100)
    private String firstName;

    @Column(name = "last_name", length = 100)
    private String lastName;

    @Column(name = "bio", columnDefinition = "TEXT")
    private String bio;

    @Column(name = "payment_account")
    private String paymentAccount;

    @Enumerated(EnumType.STRING)
    @Column(name = "reputation_level", length = 20)
    private ReputationLevel reputationLevel = ReputationLevel.JUNIOR;

    @Column(name = "avg_rating", precision = 3, scale = 2)
    private BigDecimal avgRating = BigDecimal.ZERO;

    @Column(name = "total_reviews")
    private Integer totalReviews = 0;
}
