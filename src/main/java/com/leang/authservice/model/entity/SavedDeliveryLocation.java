package com.leang.authservice.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "saved_delivery_location",
        indexes = {
                @Index(name = "idx_saved_delivery_location_user_id", columnList = "userId")
        }
)
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SavedDeliveryLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Keycloak user subject (JWT sub). */
    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 40)
    private String label;

    private String deliveryAddress;
    private String formattedAddress;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    private String province;
    private String district;
    private String commune;
    private String placeId;
    private String deliveryInstructions;

    @Column(name = "is_default", nullable = false)
    @Builder.Default
    private boolean defaultLocation = false;

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;
}
