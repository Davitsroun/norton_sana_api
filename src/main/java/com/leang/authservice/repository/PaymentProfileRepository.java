package com.leang.authservice.repository;

import com.leang.authservice.model.entity.PaymentProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PaymentProfileRepository extends JpaRepository<PaymentProfile, UUID> {
    Page<PaymentProfile> findByUserId(UUID userId, Pageable pageable);
    Optional<PaymentProfile> findByPaymentProfileIdAndUserId(UUID paymentProfileId, UUID userId);
}
