package com.leang.authservice.service;

import com.leang.authservice.enums.BatchStatus;
import com.leang.authservice.model.dto.response.StockAlertItemResponse;
import com.leang.authservice.model.entity.Product;
import com.leang.authservice.model.entity.ProductBatch;
import com.leang.authservice.repository.ProductBatchRepository;
import com.leang.authservice.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StockAlertService {

    private final ProductBatchRepository productBatchRepository;
    private final ProductRepository productRepository;
    private final BatchInventoryService batchInventoryService;

    @Value("${catalog.low-stock-threshold:5}")
    private int lowStockThreshold;

    @Value("${catalog.expiring-soon-days:30}")
    private int expiringSoonDays;

    @Transactional
    public List<StockAlertItemResponse> listAlerts() {
        LocalDate today = batchInventoryService.today();
        List<StockAlertItemResponse> alerts = new ArrayList<>();

        for (ProductBatch batch : productBatchRepository.findExpiringSoon(today, today.plusDays(expiringSoonDays))) {
            Product product = batch.getProduct();
            alerts.add(new StockAlertItemResponse(
                    "EXPIRING_SOON",
                    product.getProductId(),
                    product.getName(),
                    product.getImageUrl(),
                    batch.getId(),
                    batch.getBatchCode(),
                    batch.getExpiryDate(),
                    batch.getQuantity() == null ? 0 : batch.getQuantity(),
                    batch.getStatus()
            ));
        }

        for (ProductBatch batch : productBatchRepository.findExpiredWithStock()) {
            Product product = batch.getProduct();
            alerts.add(new StockAlertItemResponse(
                    "EXPIRED_IN_WAREHOUSE",
                    product.getProductId(),
                    product.getName(),
                    product.getImageUrl(),
                    batch.getId(),
                    batch.getBatchCode(),
                    batch.getExpiryDate(),
                    batch.getQuantity() == null ? 0 : batch.getQuantity(),
                    batch.getStatus()
            ));
        }

        for (Product product : productRepository.findAll()) {
            batchInventoryService.refreshBatchStatuses(product.getProductId());
            int sellable = batchInventoryService.getSellableQuantity(product.getProductId());
            if (sellable > 0 && sellable <= lowStockThreshold) {
                alerts.add(new StockAlertItemResponse(
                        "LOW_STOCK",
                        product.getProductId(),
                        product.getName(),
                        product.getImageUrl(),
                        null,
                        null,
                        null,
                        sellable,
                        BatchStatus.ACTIVE
                ));
            }
        }

        return alerts;
    }
}
