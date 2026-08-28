package com.leang.authservice.controller;

import com.leang.authservice.model.CartOwner;
import com.leang.authservice.model.dto.request.CashierPosCheckoutRequest;
import com.leang.authservice.model.dto.response.AdminOrderListItemResponse;
import com.leang.authservice.model.dto.response.ApiResponse;
import com.leang.authservice.model.dto.response.ApiResponseWithPagination;
import com.leang.authservice.model.dto.response.BaseResponse;
import com.leang.authservice.model.dto.response.CashierStockItemResponse;
import com.leang.authservice.model.dto.response.OrderViewResponse;
import com.leang.authservice.model.entity.Order;
import com.leang.authservice.model.entity.Product;
import com.leang.authservice.repository.OrderRepository;
import com.leang.authservice.repository.ProductRepository;
import com.leang.authservice.service.AdminOrderMapper;
import com.leang.authservice.service.CartOwnerResolver;
import com.leang.authservice.service.CurrentUserService;
import com.leang.authservice.service.OrderService;
import com.leang.authservice.util.OrderStatuses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cashier")
@RequiredArgsConstructor
@Tag(name = "Cashier")
@SecurityRequirement(name = "bearerAuth")
public class CashierController extends BaseResponse {

    private static final int LOW_STOCK_THRESHOLD = 5;

    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final OrderService orderService;
    private final CartOwnerResolver cartOwnerResolver;
    private final CurrentUserService currentUserService;
    private final AdminOrderMapper adminOrderMapper;

    @GetMapping("/stock")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponseWithPagination<CashierStockItemResponse>> listStock(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @RequestParam(name = "name", required = false) String name
    ) {
        String namePattern = null;
        if (name != null && !name.isBlank()) {
            namePattern = "%" + name.trim().toLowerCase(Locale.ROOT) + "%";
        }
        Page<Product> products = productRepository.searchProducts(
                namePattern,
                null,
                null,
                null,
                null,
                PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "name"))
        );
        Page<CashierStockItemResponse> view = products.map(this::toStockItem);
        ApiResponseWithPagination<CashierStockItemResponse> response = ApiResponseWithPagination.itemsAndPaginationResponse(
                view.getContent(),
                page,
                size,
                (int) view.getTotalElements()
        );
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PostMapping("/checkout")
    @Transactional
    public ResponseEntity<ApiResponse<OrderViewResponse>> posCheckout(
            @Valid @RequestBody CashierPosCheckoutRequest body,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        if (!currentUserService.isStaff()) {
            throw new IllegalArgumentException("Cashier checkout requires staff role");
        }
        CartOwner owner = CartOwner.user(UUID.fromString(currentUserService.keycloakSub()));
        Order order = orderService.findOrCreatePendingCart(owner);
        if (order.getItems() == null || order.getItems().isEmpty()) {
            throw new IllegalArgumentException("Cart is empty");
        }
        order.setCustomerName(body.customerName());
        order.setContactNumber(body.contactNumber());
        if (body.guestEmail() != null && !body.guestEmail().isBlank()) {
            order.setGuestEmail(body.guestEmail().trim().toLowerCase(Locale.ROOT));
        }
        order.setFulfillment(body.fulfillment());
        order.setPaymentMethod(body.paymentMethod());
        order.setStatus(OrderStatuses.PROCESSING);
        orderRepository.save(order);
        return responseEntity(true, "POS checkout details saved.", HttpStatus.OK, toOrderView(order));
    }

    @GetMapping("/orders/today")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<List<AdminOrderListItemResponse>>> todayOrders() {
        Instant startOfDay = LocalDate.now(ZoneOffset.UTC).atStartOfDay(ZoneOffset.UTC).toInstant();
        List<AdminOrderListItemResponse> orders = orderRepository.findOrdersCreatedSince(startOfDay)
                .stream()
                .map(adminOrderMapper::toAdminListItem)
                .toList();
        return responseEntity(true, "Today's orders retrieved successfully.", HttpStatus.OK, orders);
    }

    private CashierStockItemResponse toStockItem(Product product) {
        int qty = product.getStockQuantity() == null ? 0 : product.getStockQuantity();
        return new CashierStockItemResponse(
                product.getProductId(),
                product.getName(),
                qty,
                product.getPrice(),
                product.getImageUrl(),
                product.getCategory() == null ? null : product.getCategory().getCategoryName(),
                qty <= LOW_STOCK_THRESHOLD
        );
    }

    private OrderViewResponse toOrderView(Order order) {
        return new OrderViewResponse(
                order.getOrderId(),
                order.getCreatedAt(),
                adminOrderMapper.toLineItems(order),
                order.getTotalPrice(),
                order.getStatus() == null ? null : order.getStatus().toLowerCase(Locale.ROOT),
                order.getTrackingNumber(),
                order.getPaymentMethod(),
                order.getFulfillment(),
                order.getCustomerName(),
                order.getContactNumber()
        );
    }
}
