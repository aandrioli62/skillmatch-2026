package com.skillmatch.userservice.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "company_profiles")
@Getter
@Setter
public class CompanyProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    private User user;

    @Column(name = "company_name", nullable = false)
    private String companyName;

    @Column(name = "vat_number", length = 50)
    private String vatNumber;

    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    @Column(name = "contact_person", length = 200)
    private String contactPerson;
}
