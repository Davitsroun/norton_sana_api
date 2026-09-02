package com.leang.authservice.service;

import com.leang.authservice.enums.BatchStatus;
import com.leang.authservice.exception.BadRequestException;
import com.leang.authservice.model.entity.OrderItem;
import com.leang.authservice.model.entity.Product;
import com.leang.authservice.model.entity.ProductBatch;
import com.leang.authservice.repository.OrderItemBatchAllocationRepository;
import com.leang.authservice.repository.ProductBatchRepository;
import com.leang.authservice.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BatchInventoryServiceTest {

    @Mock
    private ProductBatchRepository productBatchRepository;
    @Mock
    private OrderItemBatchAllocationRepository allocationRepository;
    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private BatchInventoryService batchInventoryService;

    private UUID productId;
    private Product product;
    private OrderItem orderItem;

    @BeforeEach
    void setUp() {
        productId = UUID.randomUUID();
        product = Product.builder().productId(productId).name("Serum").stockQuantity(0).build();
        orderItem = OrderItem.builder().orderItemId(UUID.randomUUID()).build();
    }

    @Test
    void deductFefo_usesEarliestExpiryFirst() {
        LocalDate today = batchInventoryService.today();
        ProductBatch soon = batch("A", today.plusDays(10), 10);
        ProductBatch later = batch("B", today.plusDays(365), 50);

        when(productBatchRepository.findAllByProduct_ProductIdOrderByExpiryDateAscReceivedDateAsc(productId))
                .thenReturn(List.of(soon, later));
        when(productBatchRepository.findSellableForUpdate(eq(productId), eq(today)))
                .thenReturn(List.of(soon, later));
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        batchInventoryService.deductFefo(productId, 15, orderItem);

        assertEquals(0, soon.getQuantity());
        assertEquals(BatchStatus.DEPLETED, soon.getStatus());
        assertEquals(45, later.getQuantity());
        verify(allocationRepository, org.mockito.Mockito.atLeastOnce()).save(any());
        verify(productRepository).save(product);
    }

    @Test
    void deductFefo_throwsWhenInsufficientStock() {
        LocalDate today = batchInventoryService.today();
        ProductBatch batch = batch("A", today.plusDays(30), 5);

        when(productBatchRepository.findAllByProduct_ProductIdOrderByExpiryDateAscReceivedDateAsc(productId))
                .thenReturn(List.of(batch));
        when(productBatchRepository.findSellableForUpdate(eq(productId), eq(today)))
                .thenReturn(List.of(batch));

        assertThrows(BadRequestException.class, () ->
                batchInventoryService.deductFefo(productId, 10, orderItem));
    }

    @Test
    void syncProductStockCache_setsProductQuantityFromSellableSum() {
        LocalDate today = batchInventoryService.today();
        when(productBatchRepository.findAllByProduct_ProductIdOrderByExpiryDateAscReceivedDateAsc(productId))
                .thenReturn(List.of());
        when(productBatchRepository.sumSellableQuantity(productId, today)).thenReturn(80);
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        batchInventoryService.syncProductStockCache(productId);

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(captor.capture());
        assertEquals(80, captor.getValue().getStockQuantity());
    }

    private ProductBatch batch(String code, LocalDate expiry, int qty) {
        return ProductBatch.builder()
                .id(UUID.randomUUID())
                .product(product)
                .batchCode(code)
                .expiryDate(expiry)
                .receivedDate(batchInventoryService.today())
                .quantity(qty)
                .initialQuantity(qty)
                .costPrice(BigDecimal.TEN)
                .status(BatchStatus.ACTIVE)
                .build();
    }
}
