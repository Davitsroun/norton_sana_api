package com.leang.authservice.repository;

import com.leang.authservice.model.entity.FavoriteBrand;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface FavoriteBrandRepository extends JpaRepository<FavoriteBrand, UUID> {

    Page<FavoriteBrand> findByUserId(UUID userId, Pageable pageable);

    boolean existsByUserIdAndBrand_BrandId(UUID userId, UUID brandId);

    Optional<FavoriteBrand> findByUserIdAndBrand_BrandId(UUID userId, UUID brandId);
}

