package com.leang.authservice.controller;

import com.leang.authservice.model.dto.request.AdminUpdateOrderStatusRequest;
import com.leang.authservice.model.dto.request.ProductRequest;
import com.leang.authservice.model.dto.response.AdminOrderListItemResponse;
import com.leang.authservice.model.dto.response.AdminStatisticsResponse;
import com.leang.authservice.model.dto.response.AdminUserListItemResponse;
import com.leang.authservice.model.dto.response.ApiResponse;
import com.leang.authservice.model.dto.response.ApiResponseWithPagination;
import com.leang.authservice.model.dto.response.BaseResponse;
import com.leang.authservice.model.dto.response.OrderLineViewResponse;
import com.leang.authservice.model.dto.response.OrderViewResponse;
import com.leang.authservice.model.dto.response.ProductViewResponse;
import com.leang.authservice.service.AdminOrderMapper;
import com.leang.authservice.model.entity.Brand;
import com.leang.authservice.model.entity.Category;
import com.leang.authservice.model.entity.Order;
import com.leang.authservice.model.entity.Product;
import com.leang.authservice.repository.BrandRepository;
import com.leang.authservice.repository.CategoryRepository;
import com.leang.authservice.repository.OrderRepository;
import com.leang.authservice.repository.ProductRepository;
import com.leang.authservice.repository.UserProfileRepository;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Stream;

@RestController
@RequestMapping("/api/v1/admin")
@Tag(name = "Admin")
@SecurityRequirement(name = "bearerAuth")
public class AdminController extends BaseResponse {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserProfileRepository userProfileRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final AdminOrderMapper adminOrderMapper;

    public AdminController(
            OrderRepository orderRepository,
            ProductRepository productRepository,
            UserProfileRepository userProfileRepository,
            CategoryRepository categoryRepository,
            BrandRepository brandRepository,
            AdminOrderMapper adminOrderMapper
    ) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.userProfileRepository = userProfileRepository;
        this.categoryRepository = categoryRepository;
        this.brandRepository = brandRepository;
        this.adminOrderMapper = adminOrderMapper;
    }

    @GetMapping("/orders/recent")
    public ResponseEntity<ApiResponse<List<AdminOrderListItemResponse>>> getRecentOrders(
            @RequestParam(name = "limit", defaultValue = "10") int limit
    ) {
        int size = Math.min(Math.max(limit, 1), 50);
        List<AdminOrderListItemResponse> recent = orderRepository
                .findAll(PageRequest.of(0, size, Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(adminOrderMapper::toAdminListItem)
                .getContent();
        return responseEntity(true, "Recent orders retrieved successfully.", HttpStatus.OK, recent);
    }

    @GetMapping("/orders")
    public ResponseEntity<ApiResponseWithPagination<AdminOrderListItemResponse>> getAllOrders(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "status", required = false) String status
    ) {
        Page<AdminOrderListItemResponse> orders = orderRepository
                .findAdminOrders(status, PageRequest.of(page, size))
                .map(adminOrderMapper::toAdminListItem);
        ApiResponseWithPagination<AdminOrderListItemResponse> response = ApiResponseWithPagination.itemsAndPaginationResponse(
                orders.getContent(),
                page,
                size,
                (int) orders.getTotalElements()
        );
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PatchMapping("/orders/{id}/status")
    public ResponseEntity<ApiResponse<OrderViewResponse>> updateOrderStatus(
            @PathVariable UUID id,
            @Valid @RequestBody AdminUpdateOrderStatusRequest request
    ) {
        Order order = orderRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Order not found"));
        order.setStatus(request.status());
        if (request.trackingNumber() != null) {
            order.setTrackingNumber(request.trackingNumber());
        }
        return responseEntity(true, "Order status updated successfully.", HttpStatus.OK, toOrderView(orderRepository.save(order)));
    }

    @GetMapping("/users")
    public ResponseEntity<ApiResponseWithPagination<AdminUserListItemResponse>> getUsers(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size
    ) {
        Page<AdminUserListItemResponse> users = userProfileRepository.findAll(PageRequest.of(page, size))
                .map(user -> {
                    UUID keycloakUuid = UUID.fromString(user.getKeycloakId());
                    long orderCount = orderRepository.countByUserId(keycloakUuid);
                    String name = joinName(user.getFirstName(), user.getLastName());
                    if (name.isBlank()) {
                        name = user.getUsername();
                    }
                    return new AdminUserListItemResponse(
                            user.getKeycloakId(),
                            name,
                            user.getEmail(),
                            user.getCreatedAt(),
                            orderCount,
                            "ACTIVE",
                            "CUSTOMER",
                            user.getAvatarUrl()
                    );
                });
        ApiResponseWithPagination<AdminUserListItemResponse> response = ApiResponseWithPagination.itemsAndPaginationResponse(
                users.getContent(),
                page,
                size,
                (int) users.getTotalElements()
        );
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
    @GetMapping("/products")
    public ResponseEntity<ApiResponseWithPagination<ProductViewResponse>> getProducts(

            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "categoryId", required = false) UUID categoryId,
            @RequestParam(name = "minPrice", required = false) BigDecimal minPrice,
            @RequestParam(name = "maxPrice", required = false) BigDecimal maxPrice
    ) {
        Page<Product> products = productRepository.searchProducts(
                null,
                null,
                categoryId,
                minPrice,
                maxPrice,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        Page<ProductViewResponse> viewPage = products.map(this::toProductView);
        ApiResponseWithPagination<ProductViewResponse> response = ApiResponseWithPagination.itemsAndPaginationResponse(
                viewPage.getContent(),
                page,
                size,
                (int) viewPage.getTotalElements()
        );
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PostMapping("/products")
    public ResponseEntity<ApiResponse<ProductViewResponse>> createProduct(@Valid @RequestBody ProductRequest product) {
        Product newProduct = Product.builder()
                .name(product.name())
                .description(product.description())
                .price(product.price())
                .stockQuantity(product.stockQuantity())
                .imageUrl(product.imageUrl())
                .imageUrl2(product.imageUrl2())
                .imageUrl3(product.imageUrl3())
                .imageUrl4(product.imageUrl4())
                .category(resolveCategory(product.categoryId()))
                .brand(resolveBrand(product.brandId()))
                .createdAt(Instant.now())
                .build();
        return responseEntity(true, "Product created successfully.", HttpStatus.CREATED, toProductView(productRepository.save(newProduct)));
    }

    @PutMapping("/products/{id}")
    public ResponseEntity<ApiResponse<ProductViewResponse>> updateProduct(@PathVariable UUID id, @Valid @RequestBody ProductRequest payload) {
        Product existing = productRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Product not found"));
        existing.setName(payload.name());
        existing.setDescription(payload.description());
        existing.setPrice(payload.price());
        existing.setStockQuantity(payload.stockQuantity());
        existing.setImageUrl(payload.imageUrl());
        existing.setImageUrl2(payload.imageUrl2());
        existing.setImageUrl3(payload.imageUrl3());
        existing.setImageUrl4(payload.imageUrl4());
        existing.setCategory(resolveCategory(payload.categoryId()));
        existing.setBrand(resolveBrand(payload.brandId()));
        return responseEntity(true, "Product updated successfully.", HttpStatus.OK, toProductView(productRepository.save(existing)));
    }

    @GetMapping({"/statistics", "/statistics/overview"})
    public ResponseEntity<ApiResponse<AdminStatisticsResponse>> getStatistics() {
        AdminStatisticsResponse statistics = new AdminStatisticsResponse(
                userProfileRepository.count(),
                productRepository.count(),
                orderRepository.count()
        );
        return responseEntity(true, "Admin statistics retrieved successfully.", HttpStatus.OK, statistics);
    }

    @DeleteMapping("/products/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable UUID id) {
        Product existing = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));
        productRepository.delete(existing);
        return responseEntity(true, "Product deleted successfully.", HttpStatus.OK);
    }

    private static String joinName(String first, String last) {
        String f = first == null ? "" : first.trim();
        String l = last == null ? "" : last.trim();
        if (f.isEmpty()) {
            return l;
        }
        if (l.isEmpty()) {
            return f;
        }
        return f + " " + l;
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
                order.getFulfillment(),
                order.getCustomerName(),
                order.getContactNumber()
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
                product.getBrand() == null ? null : product.getBrand().getBrandId(),
                product.getName(),
                product.getPrice(),
                null,
                product.getImageUrl(),
                product.getImageUrl2(),
                product.getImageUrl3(),
                product.getImageUrl4(),
                // imageUrls(product),
                avgRating,
                reviewCount,
                product.getCategory() == null ? null : product.getCategory().getCategoryName(),
                product.getDescription(),
                null
        );
    }

    private List<String> imageUrls(Product product) {
        return Stream.of(product.getImageUrl(), product.getImageUrl2(), product.getImageUrl3(), product.getImageUrl4())
                .filter(url -> url != null && !url.isBlank())
                .toList();
    }

    private Category resolveCategory(UUID categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Category not found"));
    }

    private Brand resolveBrand(UUID brandId) {
        if (brandId == null) {
            return null;
        }
        return brandRepository.findById(brandId)
                .orElseThrow(() -> new IllegalArgumentException("Brand not found"));
    }
}
