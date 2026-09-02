package com.leang.authservice.service.impl;

import com.leang.authservice.exception.BadRequestException;
import com.leang.authservice.exception.ForbiddenException;
import com.leang.authservice.exception.NotFoundException;
import com.leang.authservice.model.CartOwner;
import com.leang.authservice.model.dto.request.OrderItemCreateRequest;
import com.leang.authservice.model.dto.response.ApiResponseWithPagination;
import com.leang.authservice.model.entity.Order;
import com.leang.authservice.model.entity.OrderItem;
import com.leang.authservice.model.entity.Product;
import com.leang.authservice.repository.OrderItemRepository;
import com.leang.authservice.repository.OrderRepository;
import com.leang.authservice.service.BatchInventoryService;
import com.leang.authservice.service.CartLineMerger;
import com.leang.authservice.service.CartMergeService;
import com.leang.authservice.service.CartOwnerResolver;
import com.leang.authservice.service.OrderItemService;
import com.leang.authservice.service.OrderService;
import com.leang.authservice.service.ProductService;
import com.leang.authservice.util.ProductCostHelper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderItemServiceImpl implements OrderItemService {

    private final OrderItemRepository orderItemRepository;
    private final OrderService orderService;
    private final OrderRepository orderRepository;
    private final ProductService productService;
    private final CartOwnerResolver cartOwnerResolver;
    private final CartMergeService cartMergeService;
    private final CartLineMerger cartLineMerger;
    private final BatchInventoryService batchInventoryService;

    @Override
    public OrderItem create(OrderItemCreateRequest dto) {
        if (dto.getProductId() == null) {
            throw new BadRequestException("productId is required");
        }
        if (dto.getQuantity() == null || dto.getQuantity() < 1) {
            throw new BadRequestException("quantity must be at least 1");
        }

        CartOwner owner = requireOwner();
        Order order = orderService.findOrCreatePendingCart(owner);
        cartLineMerger.consolidateDuplicateProductsInOrder(order.getOrderId());

        Product product = productService.getById(dto.getProductId());
        int addQty = dto.getQuantity();
        assertSellable(product.getProductId(), addQty);

        List<OrderItem> existingLines = orderItemRepository
                .findAllByOrder_OrderIdAndProduct_ProductIdOrderByOrderItemIdAsc(order.getOrderId(), dto.getProductId());

        if (!existingLines.isEmpty()) {
            OrderItem existing = existingLines.get(0);
            int newQty = existing.getQuantity() + addQty;
            existing.setQuantity(newQty);
            existing.setPrice(product.getPrice().multiply(BigDecimal.valueOf(newQty)));
            OrderItem saved = orderItemRepository.save(existing);
            batchInventoryService.deductFefo(product.getProductId(), addQty, saved);
            order.setTotalPrice(orderItemRepository.getTotalPriceByOrderId(order.getOrderId()));
            orderRepository.save(order);
            return saved;
        }

        OrderItem orderItem = OrderItem.builder()
                .order(order)
                .product(product)
                .quantity(addQty)
                .price(product.getPrice().multiply(BigDecimal.valueOf(addQty)))
                .unitCost(ProductCostHelper.unitCost(product))
                .build();
        OrderItem item = orderItemRepository.save(orderItem);
        batchInventoryService.deductFefo(product.getProductId(), addQty, item);
        order.setTotalPrice(orderItemRepository.getTotalPriceByOrderId(order.getOrderId()));
        orderRepository.save(order);
        return item;
    }

    @Override
    public OrderItem update(UUID id, OrderItem orderItem) {
        OrderItem existing = requireOwnedOrderItem(id);
        Product product = productService.getById(existing.getProduct().getProductId());

        int oldQty = existing.getQuantity();
        int newQty = orderItem.getQuantity();
        int diff = newQty - oldQty;

        if (diff > 0) {
            assertSellable(product.getProductId(), diff);
            batchInventoryService.deductFefo(product.getProductId(), diff, existing);
        } else if (diff < 0) {
            batchInventoryService.restoreForOrderItem(id, -diff);
        }

        existing.setQuantity(newQty);
        existing.setPrice(product.getPrice().multiply(BigDecimal.valueOf(newQty)));
        OrderItem saved = orderItemRepository.save(existing);

        Order order = existing.getOrder();
        order.setTotalPrice(orderItemRepository.getTotalPriceByOrderId(order.getOrderId()));
        orderRepository.save(order);
        return saved;
    }

    @Override
    public void delete(UUID id) {
        OrderItem existing = requireOwnedOrderItem(id);
        batchInventoryService.restoreAllForOrderItem(id);

        UUID orderId = existing.getOrder().getOrderId();
        orderItemRepository.delete(existing);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found"));
        BigDecimal newTotal = orderItemRepository.getTotalPriceByOrderId(orderId);
        order.setTotalPrice(newTotal != null ? newTotal : BigDecimal.ZERO);
        orderRepository.save(order);
    }

    @Override
    public OrderItem getById(UUID id) {
        return requireOwnedOrderItem(id);
    }

    @Override
    public ApiResponseWithPagination<OrderItem> getAll(int page, int size) {
        PageRequest pageable = PageRequest.of(page, size);
        Page<OrderItem> orderItemPage = orderItemRepository.findAll(pageable);
        return ApiResponseWithPagination.itemsAndPaginationResponse(
                orderItemPage.getContent(),
                page,
                size,
                (int) orderItemPage.getTotalElements()
        );
    }

    private void assertSellable(UUID productId, int quantity) {
        if (batchInventoryService.getSellableQuantity(productId) < quantity) {
            throw new BadRequestException("Insufficient stock available");
        }
    }

    private CartOwner requireOwner() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            throw new BadRequestException("No HTTP request in context");
        }
        HttpServletRequest request = attrs.getRequest();
        HttpServletResponse response = attrs.getResponse();
        if (response == null) {
            throw new BadRequestException("No HTTP response in context");
        }
        cartOwnerResolver.currentUserId().ifPresent(userId ->
                cartMergeService.mergeGuestCartIfPresent(userId, request, response));
        return cartOwnerResolver.resolve(request, response);
    }

    private OrderItem requireOwnedOrderItem(UUID orderItemId) {
        CartOwner owner = requireOwner();
        if (owner.isRegistered()) {
            return orderItemRepository.findByOrderItemIdAndOrder_UserId(orderItemId, owner.userId())
                    .orElseThrow(() -> new ForbiddenException("You can only access order items from your own cart."));
        }
        return orderItemRepository
                .findByOrderItemIdAndOrder_SessionIdAndOrder_UserIdIsNull(orderItemId, owner.sessionId())
                .orElseThrow(() -> new ForbiddenException("You can only access order items from your own cart."));
    }
}
