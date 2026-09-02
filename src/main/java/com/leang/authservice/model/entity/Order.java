package com.leang.authservice.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
        name = "orders",
        indexes = {
                @Index(name = "idx_orders_session_id", columnList = "sessionId"),
                @Index(name = "idx_orders_guest_email", columnList = "guestEmail")
        }
)
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID orderId;

    /** Keycloak user ID; null for guest carts/orders. */
    private UUID userId;

    /** Guest browser session; null after merge or for pure user carts. */
    private UUID sessionId;

    /** Set at guest checkout; links order without an account. */
    private String guestEmail;

    private BigDecimal totalPrice;

    private String status;
    private String currency;
    private String paymentMethod;
    private String fulfillment;
    private String trackingNumber;
    private String deliveryAddress;
    private String customerName;
    private String contactNumber;

    private Double latitude;
    private Double longitude;
    private String province;
    private String district;
    private String commune;
    private String placeId;
    private String formattedAddress;
    private String deliveryInstructions;
    private String pickupNotes;

    private Instant createdAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();

    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL)
    @JsonIgnore
    private Payment payment;
}

