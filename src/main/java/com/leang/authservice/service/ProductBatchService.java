package com.leang.authservice.service;

import com.leang.authservice.enums.BatchStatus;
import com.leang.authservice.exception.BadRequestException;
import com.leang.authservice.exception.NotFoundException;
import com.leang.authservice.model.dto.request.ProductBatchRequest;
import com.leang.authservice.model.dto.response.AdminBatchResponse;
import com.leang.authservice.model.dto.response.ApiResponseWithPagination;
import com.leang.authservice.model.dto.response.ProductBatchResponse;
import com.leang.authservice.model.entity.Product;
import com.leang.authservice.model.entity.ProductBatch;
import com.leang.authservice.repository.ProductBatchRepository;
import com.leang.authservice.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductBatchService {

    private final ProductBatchRepository productBatchRepository;
    private final ProductRepository productRepository;
    private final BatchInventoryService batchInventoryService;

    @Transactional
    public List<ProductBatchResponse> list(UUID productId) {
        requireProduct(productId);
        batchInventoryService.refreshBatchStatuses(productId);
        return productBatchRepository.findAllByProduct_ProductIdOrderByExpiryDateAscReceivedDateAsc(productId)
                .stream()
                .map(this::toProductScopedResponse)
                .toList();
    }

    @Transactional
    public ApiResponseWithPagination<AdminBatchResponse> searchAdmin(
            int page,
            int size,
            String search,
            UUID productId,
            String status,
            Integer expiringWithinDays,
            String sort
    ) {
        LocalDate today = batchInventoryService.today();
        BatchStatus statusFilter = parseStatus(status);
        String searchPattern = null;
        if (search != null && !search.isBlank()) {
            searchPattern = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
        }
        LocalDate expiringUntil = null;
        if (expiringWithinDays != null && expiringWithinDays > 0) {
            expiringUntil = today.plusDays(expiringWithinDays);
        }

        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), resolveSort(sort));
        Page<ProductBatch> result = productBatchRepository.searchAdminBatches(
                productId,
                statusFilter,
                searchPattern,
                today,
                expiringUntil,
                pageable
        );

        // Refresh statuses for products on this page (keeps ACTIVE/EXPIRED accurate)
        result.getContent().stream()
                .map(b -> b.getProduct().getProductId())
                .distinct()
                .forEach(batchInventoryService::refreshBatchStatuses);

        List<AdminBatchResponse> items = result.getContent().stream()
                .map(b -> productBatchRepository.findDetailById(b.getId()).orElse(b))
                .map(this::toAdminResponse)
                .toList();

        return ApiResponseWithPagination.itemsAndPaginationResponse(
                items,
                pageable.getPageNumber(),
                pageable.getPageSize(),
                (int) result.getTotalElements()
        );
    }

    @Transactional
    public AdminBatchResponse getAdminById(UUID batchId) {
        ProductBatch batch = productBatchRepository.findDetailById(batchId)
                .orElseThrow(() -> new NotFoundException("Batch not found"));
        batchInventoryService.refreshBatchStatuses(batch.getProduct().getProductId());
        return toAdminResponse(productBatchRepository.findDetailById(batchId).orElse(batch));
    }

    @Transactional
    public AdminBatchResponse createGlobal(ProductBatchRequest request) {
        if (request.productId() == null) {
            throw new BadRequestException("productId is required");
        }
        return toAdminResponse(createInternal(request.productId(), request));
    }

    @Transactional
    public ProductBatchResponse create(UUID productId, ProductBatchRequest request) {
        return toProductScopedResponse(createInternal(productId, request));
    }

    @Transactional
    public AdminBatchResponse updateGlobal(UUID batchId, ProductBatchRequest request) {
        ProductBatch batch = productBatchRepository.findDetailById(batchId)
                .orElseThrow(() -> new NotFoundException("Batch not found"));
        return toAdminResponse(updateInternal(batch.getProduct().getProductId(), batchId, request));
    }

    @Transactional
    public ProductBatchResponse update(UUID productId, UUID batchId, ProductBatchRequest request) {
        return toProductScopedResponse(updateInternal(productId, batchId, request));
    }

    @Transactional
    public void delete(UUID productId, UUID batchId) {
        ProductBatch batch = requireBatch(productId, batchId);
        if (batch.getQuantity() != null && batch.getQuantity() > 0) {
            throw new BadRequestException("Cannot delete batch with remaining quantity; write off first");
        }
        productBatchRepository.delete(batch);
        batchInventoryService.syncProductStockCache(productId);
    }

    @Transactional
    public AdminBatchResponse writeOffGlobal(UUID batchId, String reason) {
        ProductBatch batch = productBatchRepository.findDetailById(batchId)
                .orElseThrow(() -> new NotFoundException("Batch not found"));
        return toAdminResponse(writeOffInternal(batch.getProduct().getProductId(), batchId, reason));
    }

    @Transactional
    public ProductBatchResponse writeOff(UUID productId, UUID batchId, String reason) {
        return toProductScopedResponse(writeOffInternal(productId, batchId, reason));
    }

    private ProductBatch createInternal(UUID productId, ProductBatchRequest request) {
        Product product = requireProduct(productId);
        if (request.quantity() == null || request.quantity() <= 0) {
            throw new BadRequestException("quantity must be greater than 0");
        }
        if (request.expiryDate() == null) {
            throw new BadRequestException("expiryDate is required");
        }
        boolean allowPast = Boolean.TRUE.equals(request.allowPastExpiry());
        batchInventoryService.validateNewBatchDates(request.expiryDate(), request.receivedDate(), allowPast);

        String batchCode = request.batchCode();
        if (batchCode == null || batchCode.isBlank()) {
            batchCode = batchInventoryService.generateBatchCode();
        } else {
            batchCode = batchCode.trim();
            batchInventoryService.assertBatchCodeUnique(productId, batchCode);
        }

        LocalDate received = request.receivedDate() == null ? batchInventoryService.today() : request.receivedDate();
        LocalDate today = batchInventoryService.today();
        BatchStatus initialStatus = BatchStatus.ACTIVE;
        if (request.expiryDate().isBefore(today)) {
            initialStatus = BatchStatus.EXPIRED;
        }

        ProductBatch batch = ProductBatch.builder()
                .product(product)
                .batchCode(batchCode)
                .expiryDate(request.expiryDate())
                .receivedDate(received)
                .quantity(request.quantity())
                .initialQuantity(request.quantity())
                .costPrice(request.costPrice())
                .status(initialStatus)
                .build();
        ProductBatch saved = productBatchRepository.save(batch);
        batchInventoryService.syncProductStockCache(productId);
        return productBatchRepository.findDetailById(saved.getId()).orElse(saved);
    }

    private ProductBatch updateInternal(UUID productId, UUID batchId, ProductBatchRequest request) {
        ProductBatch batch = requireBatch(productId, batchId);
        if (batch.getStatus() == BatchStatus.WRITTEN_OFF) {
            throw new BadRequestException("Written-off batches cannot be updated");
        }

        if (request.batchCode() != null && !request.batchCode().isBlank()
                && !request.batchCode().equalsIgnoreCase(batch.getBatchCode())) {
            batchInventoryService.assertBatchCodeUnique(productId, request.batchCode());
            batch.setBatchCode(request.batchCode().trim());
        }
        if (request.expiryDate() != null) {
            batch.setExpiryDate(request.expiryDate());
        }
        if (request.receivedDate() != null) {
            batch.setReceivedDate(request.receivedDate());
        }
        if (request.quantity() != null) {
            if (request.quantity() < 0) {
                throw new BadRequestException("quantity must be >= 0");
            }
            batch.setQuantity(request.quantity());
        }
        if (request.costPrice() != null) {
            batch.setCostPrice(request.costPrice());
        }

        batchInventoryService.refreshBatchStatuses(productId);
        ProductBatch saved = productBatchRepository.save(batch);
        batchInventoryService.syncProductStockCache(productId);
        return productBatchRepository.findDetailById(saved.getId()).orElse(saved);
    }

    private ProductBatch writeOffInternal(UUID productId, UUID batchId, String reason) {
        ProductBatch batch = requireBatch(productId, batchId);
        batch.setQuantity(0);
        batch.setStatus(BatchStatus.WRITTEN_OFF);
        batch.setWriteOffReason(reason == null || reason.isBlank() ? null : reason.trim());
        ProductBatch saved = productBatchRepository.save(batch);
        batchInventoryService.syncProductStockCache(productId);
        return productBatchRepository.findDetailById(saved.getId()).orElse(saved);
    }

    private Product requireProduct(UUID productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("Product not found"));
    }

    private ProductBatch requireBatch(UUID productId, UUID batchId) {
        return productBatchRepository.findByIdAndProduct_ProductId(batchId, productId)
                .orElseThrow(() -> new NotFoundException("Batch not found"));
    }

    private BatchStatus parseStatus(String status) {
        if (status == null || status.isBlank() || "ALL".equalsIgnoreCase(status.trim())) {
            return null;
        }
        try {
            return BatchStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid status. Use ACTIVE, EXPIRED, DEPLETED, WRITTEN_OFF, or ALL");
        }
    }

    private Sort resolveSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return Sort.by(Sort.Direction.ASC, "expiryDate");
        }
        return switch (sort.trim()) {
            case "expiryDateDesc" -> Sort.by(Sort.Direction.DESC, "expiryDate");
            case "receivedDateDesc" -> Sort.by(Sort.Direction.DESC, "receivedDate");
            case "quantityAsc" -> Sort.by(Sort.Direction.ASC, "quantity");
            case "expiryDateAsc" -> Sort.by(Sort.Direction.ASC, "expiryDate");
            default -> Sort.by(Sort.Direction.ASC, "expiryDate");
        };
    }

    private int soldQuantity(ProductBatch batch) {
        int initial = batch.getInitialQuantity() == null ? 0 : batch.getInitialQuantity();
        int current = batch.getQuantity() == null ? 0 : batch.getQuantity();
        return Math.max(0, initial - current);
    }

    private ProductBatchResponse toProductScopedResponse(ProductBatch batch) {
        return new ProductBatchResponse(
                batch.getId(),
                batch.getProduct().getProductId(),
                batch.getBatchCode(),
                batch.getExpiryDate(),
                batch.getReceivedDate(),
                batch.getQuantity() == null ? 0 : batch.getQuantity(),
                batch.getInitialQuantity(),
                batch.getCostPrice(),
                batch.getStatus(),
                batch.getCreatedAt(),
                batch.getUpdatedAt()
        );
    }

    private AdminBatchResponse toAdminResponse(ProductBatch batch) {
        Product product = batch.getProduct();
        int initial = batch.getInitialQuantity() == null
                ? (batch.getQuantity() == null ? 0 : batch.getQuantity())
                : batch.getInitialQuantity();
        int current = batch.getQuantity() == null ? 0 : batch.getQuantity();
        return new AdminBatchResponse(
                batch.getId(),
                product.getProductId(),
                product.getName(),
                product.getImageUrl(),
                product.getBrand() == null ? null : product.getBrand().getBrandName(),
                product.getCategory() == null ? null : product.getCategory().getCategoryName(),
                batch.getBatchCode(),
                batch.getExpiryDate(),
                batch.getReceivedDate(),
                initial,
                current,
                soldQuantity(batch),
                batch.getCostPrice(),
                batch.getStatus(),
                batch.getWriteOffReason(),
                batch.getCreatedAt(),
                batch.getUpdatedAt()
        );
    }
}
