package com.leang.authservice.controller;

import com.leang.authservice.model.dto.request.AdminUpdateOrderStatusRequest;
import com.leang.authservice.model.dto.response.AdminStatisticsResponse;
import com.leang.authservice.model.dto.response.MeResponse;
import com.leang.authservice.model.dto.response.OrderLineViewResponse;
import com.leang.authservice.model.dto.response.OrderViewResponse;
import com.leang.authservice.model.dto.response.ProductViewResponse;
import com.leang.authservice.model.entity.Order;
import com.leang.authservice.model.entity.Product;
import com.leang.authservice.repository.OrderRepository;
import com.leang.authservice.repository.ProductRepository;
import com.leang.authservice.repository.UserProfileRepository;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserProfileRepository userProfileRepository;

    public AdminController(
            OrderRepository orderRepository,
            ProductRepository productRepository,
            UserProfileRepository userProfileRepository
    ) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.userProfileRepository = userProfileRepository;
    }

    @GetMapping("/orders")
    public Page<OrderViewResponse> getAllOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return orderRepository.findAll(PageRequest.of(page, size)).map(this::toOrderView);
    }

    @PatchMapping("/orders/{id}/status")
    public OrderViewResponse updateOrderStatus(
            @PathVariable UUID id,
            @Valid @RequestBody AdminUpdateOrderStatusRequest request
    ) {
        Order order = orderRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Order not found"));
        order.setStatus(request.status());
        if (request.trackingNumber() != null) {
            order.setTrackingNumber(request.trackingNumber());
        }
        return toOrderView(orderRepository.save(order));
    }

    @GetMapping("/users")
    public Page<MeResponse> getUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return userProfileRepository.findAll(PageRequest.of(page, size))
                .map(user -> new MeResponse(
                        user.getKeycloakId(),
                        user.getEmail(),
                        user.getFirstName(),
                        user.getLastName(),
                        user.getUsername(),
                        user.getAvatarUrl(),
                        List.of("user"),
                        false
                ));
    }

    @GetMapping("/products")
    public Page<ProductViewResponse> getProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return productRepository.findAll(PageRequest.of(page, size))
                .map(this::toProductView);
    }

    @PostMapping("/products")
    public ProductViewResponse createProduct(@RequestBody Product product) {
        return toProductView(productRepository.save(product));
    }

    @PutMapping("/products/{id}")
    public ProductViewResponse updateProduct(@PathVariable UUID id, @RequestBody Product payload) {
        Product existing = productRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Product not found"));
        existing.setName(payload.getName());
        existing.setDescription(payload.getDescription());
        existing.setPrice(payload.getPrice());
        existing.setStockQuantity(payload.getStockQuantity());
        existing.setImageUrl(payload.getImageUrl());
        existing.setCategory(payload.getCategory());
        existing.setBrand(payload.getBrand());
        return toProductView(productRepository.save(existing));
    }

    @GetMapping("/statistics")
    public AdminStatisticsResponse getStatistics() {
        return new AdminStatisticsResponse(
                userProfileRepository.count(),
                productRepository.count(),
                orderRepository.count()
        );
    }

    private OrderViewResponse toOrderView(Order order) {
        List<OrderLineViewResponse> lines = order.getItems().stream()
                .map(item -> new OrderLineViewResponse(
                        item.getOrderItemId(),
                        item.getProduct().getName(),
                        item.getQuantity(),
                        item.getPrice(),
                        item.getProduct().getImageUrl()
                ))
                .toList();
        return new OrderViewResponse(
                order.getOrderId(),
                order.getCreatedAt(),
                lines,
                order.getTotalPrice(),
                order.getStatus() == null ? null : order.getStatus().toLowerCase(Locale.ROOT),
                order.getTrackingNumber(),
                order.getPaymentMethod(),
                order.getFulfillment()
        );
    }

    private ProductViewResponse toProductView(Product product) {
        double avgRating = product.getReviews().stream()
                .filter(r -> r.getRating() != null)
                .mapToInt(r -> r.getRating())
                .average()
                .orElse(0.0);
        int reviewCount = product.getReviews() == null ? 0 : product.getReviews().size();
        return new ProductViewResponse(
                product.getProductId(),
                product.getName(),
                product.getPrice(),
                null,
                product.getImageUrl(),
                avgRating,
                reviewCount,
                product.getCategory() == null ? null : product.getCategory().getCategoryName(),
                product.getDescription(),
                null
        );
    }
}
