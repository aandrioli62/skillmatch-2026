package com.skillmatch.userservice.repository;

import com.skillmatch.userservice.model.PortfolioItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PortfolioItemRepository extends JpaRepository<PortfolioItem, UUID> {

    List<PortfolioItem> findByUserId(UUID userId);
}
