package com.leang.authservice.repository;

import com.leang.authservice.model.entity.GuestSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface GuestSessionRepository extends JpaRepository<GuestSession, UUID> {
}
