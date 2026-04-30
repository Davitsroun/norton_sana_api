package com.leang.authservice.repository;

import com.leang.authservice.model.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ReviewRepository extends JpaRepository<Review, UUID> {

    Page<Review> findByProduct_ProductIdOrderByCreatedAtDesc(UUID productId, Pageable pageable);
}
