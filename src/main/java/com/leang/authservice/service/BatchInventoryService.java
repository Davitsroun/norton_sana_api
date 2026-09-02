package com.leang.authservice.service;

import com.leang.authservice.enums.BatchStatus;
import com.leang.authservice.exception.BadRequestException;
import com.leang.authservice.exception.ConflictException;
import com.leang.authservice.model.dto.response.ProductStockSummary;
import com.leang.authservice.model.entity.OrderItem;
import com.leang.authservice.model.entity.OrderItemBatchAllocation;
import com.leang.authservice.model.entity.Product;
import com.leang.authservice.model.entity.ProductBatch;
import com.leang.authservice.repository.OrderItemBatchAllocationRepository;
import com.leang.authservice.repository.ProductBatchRepository;
import com.leang.authservice.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * FEFO batch inventory: sellable stock, deduct, restore, sync cached product.stockQuantity.
 */
@Service
@RequiredArgsConstructor
public class BatchInventoryService {

    public static final ZoneId STOCK_ZONE = ZoneId.of("Asia/Phnom_Penh");

    private final ProductBatchRepository productBatchRepository;
    private final OrderItemBatchAllocationRepository allocationRepository;
    private final ProductRepository productRepository;

    @Value("${catalog.expiring-soon-days:30}")
    private int expiringSoonDays;

    public LocalDate today() {
        return LocalDate.now(STOCK_ZONE);
    }

    @Transactional
    public void refreshBatchStatuses(UUID productId) {
        LocalDate today = today();
        for (ProductBatch batch : productBatchRepository.findAllByProduct_ProductIdOrderByExpiryDateAscReceivedDateAsc(productId)) {
            if (batch.getStatus() == BatchStatus.WRITTEN_OFF) {
                continue;
            }
            int qty = batch.getQuantity() == null ? 0 : batch.getQuantity();
            if (batch.getExpiryDate() != null && batch.getExpiryDate().isBefore(today)) {
                batch.setStatus(BatchStatus.EXPIRED);
            } else if (qty <= 0) {
                batch.setStatus(BatchStatus.DEPLETED);
            } else {
                batch.setStatus(BatchStatus.ACTIVE);
            }
            productBatchRepository.save(batch);
        }
    }

    @Transactional
    public ProductStockSummary getStockSummary(UUID productId) {
        refreshBatchStatuses(productId);
        LocalDate today = today();
        LocalDate expiringUntil = today.plusDays(expiringSoonDays);
        int sellable = productBatchRepository.sumSellableQuantity(productId, today);
        long expiredCount = productBatchRepository.countByProduct_ProductIdAndStatus(productId, BatchStatus.EXPIRED);
        return new ProductStockSummary(
                sellable,
                (int) productBatchRepository.countByProduct_ProductId(productId),
                productBatchRepository.findNearestExpiryDate(productId, today),
                (int) expiredCount,
                productBatchRepository.sumExpiringSoonQuantity(productId, today, expiringUntil)
        );
    }

    @Transactional
    public void syncProductStockCache(UUID productId) {
        refreshBatchStatuses(productId);
        int sellable = productBatchRepository.sumSellableQuantity(productId, today());
        productRepository.findById(productId).ifPresent(product -> {
            product.setStockQuantity(sellable);
            productRepository.save(product);
        });
    }

    @Transactional
    public int getSellableQuantity(UUID productId) {
        refreshBatchStatuses(productId);
        return productBatchRepository.sumSellableQuantity(productId, today());
    }

    @Transactional
    public void deductFefo(UUID productId, int quantity, OrderItem orderItem) {
        if (quantity <= 0) {
            return;
        }
        refreshBatchStatuses(productId);
        List<ProductBatch> batches = productBatchRepository.findSellableForUpdate(productId, today());
        int remaining = quantity;
        for (ProductBatch batch : batches) {
            if (remaining <= 0) {
                break;
            }
            int available = batch.getQuantity() == null ? 0 : batch.getQuantity();
            if (available <= 0) {
                continue;
            }
            int take = Math.min(available, remaining);
            batch.setQuantity(available - take);
            if (batch.getQuantity() <= 0) {
                batch.setStatus(BatchStatus.DEPLETED);
            }
            productBatchRepository.save(batch);
            allocationRepository.save(OrderItemBatchAllocation.builder()
                    .orderItem(orderItem)
                    .batch(batch)
                    .quantity(take)
                    .build());
            remaining -= take;
        }
        if (remaining > 0) {
            throw new BadRequestException("Insufficient stock available");
        }
        syncProductStockCache(productId);
    }

    @Transactional
    public void restoreForOrderItem(UUID orderItemId, int quantityToRestore) {
        if (quantityToRestore <= 0) {
            return;
        }
        List<OrderItemBatchAllocation> allocations = allocationRepository
                .findAllByOrderItem_OrderItemIdOrderByCreatedAtDesc(orderItemId);
        int remaining = quantityToRestore;
        UUID productId = null;
        for (OrderItemBatchAllocation allocation : allocations) {
            if (remaining <= 0) {
                break;
            }
            int allocated = allocation.getQuantity() == null ? 0 : allocation.getQuantity();
            if (allocated <= 0) {
                continue;
            }
            int restore = Math.min(allocated, remaining);
            ProductBatch batch = allocation.getBatch();
            if (batch != null && batch.getProduct() != null) {
                productId = batch.getProduct().getProductId();
            }
            if (batch != null && batch.getStatus() != BatchStatus.WRITTEN_OFF) {
                batch.setQuantity((batch.getQuantity() == null ? 0 : batch.getQuantity()) + restore);
                if (batch.getExpiryDate() != null && !batch.getExpiryDate().isBefore(today())) {
                    batch.setStatus(BatchStatus.ACTIVE);
                }
                productBatchRepository.save(batch);
            }
            if (restore == allocated) {
                allocationRepository.delete(allocation);
            } else {
                allocation.setQuantity(allocated - restore);
                allocationRepository.save(allocation);
            }
            remaining -= restore;
        }
        if (productId != null) {
            syncProductStockCache(productId);
        }
    }

    @Transactional
    public void restoreAllForOrderItem(UUID orderItemId) {
        List<OrderItemBatchAllocation> allocations = allocationRepository
                .findAllByOrderItem_OrderItemIdOrderByCreatedAtDesc(orderItemId);
        int total = allocations.stream().mapToInt(a -> a.getQuantity() == null ? 0 : a.getQuantity()).sum();
        restoreForOrderItem(orderItemId, total);
    }

    public void assertBatchCodeUnique(UUID productId, String batchCode) {
        if (batchCode == null || batchCode.isBlank()) {
            return;
        }
        if (productBatchRepository.existsByProduct_ProductIdAndBatchCodeIgnoreCase(productId, batchCode.trim())) {
            throw new ConflictException("Batch code already exists for this product", Map.of("batchCode", batchCode));
        }
    }

    public String generateBatchCode() {
        LocalDate today = today();
        return "LOT-" + today.getYear() + "-" + String.format("%02d", today.getMonthValue())
                + "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
    }

    public void validateNewBatchDates(LocalDate expiryDate, LocalDate receivedDate, boolean allowPastExpiry) {
        if (expiryDate == null) {
            throw new BadRequestException("expiryDate is required");
        }
        LocalDate received = receivedDate == null ? today() : receivedDate;
        if (expiryDate.isBefore(received)) {
            throw new BadRequestException("expiryDate must be on or after receivedDate");
        }
        if (!allowPastExpiry && expiryDate.isBefore(today())) {
            throw new BadRequestException("expiryDate is in the past");
        }
    }

    @Transactional
    public ProductBatch createLegacyBatch(Product product, int quantity) {
        if (quantity <= 0) {
            return null;
        }
        LocalDate today = today();
        ProductBatch batch = ProductBatch.builder()
                .product(product)
                .batchCode("LEGACY")
                .expiryDate(today.plusYears(2))
                .receivedDate(today)
                .quantity(quantity)
                .initialQuantity(quantity)
                .costPrice(product.getCostPrice())
                .status(BatchStatus.ACTIVE)
                .build();
        ProductBatch saved = productBatchRepository.save(batch);
        syncProductStockCache(product.getProductId());
        return saved;
    }
}
