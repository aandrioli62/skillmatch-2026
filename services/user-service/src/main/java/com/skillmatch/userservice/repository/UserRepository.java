package com.skillmatch.userservice.repository;

import com.skillmatch.userservice.model.User;
import com.skillmatch.userservice.model.enums.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByKeycloakId(String keycloakId);

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    // DEACTIVATED accounts are a logical delete (the Keycloak identity is already
    // disabled) — excluded here so the admin oversight list doesn't fill up with
    // accounts nobody can act on anymore. Still reachable directly by id (e.g. from
    // a historical contract/payment/feedback record) via the inherited findById.
    Page<User> findByStatusNot(UserStatus status, Pageable pageable);
}
