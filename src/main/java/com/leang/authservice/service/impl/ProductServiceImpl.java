package com.leang.authservice.service.impl;

import com.leang.authservice.exception.NotFoundException;
import com.leang.authservice.model.dto.response.ApiResponseWithPagination;
import com.leang.authservice.model.entity.Product;
import com.leang.authservice.repository.ProductRepository;
import com.leang.authservice.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;

    @Override
    public Product create(Product product) {
        product.setCreatedAt(Instant.now());
        return productRepository.save(product);
    }

    @Override
    public Product update(UUID id, Product product) {
        Product existing = productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Product not found"));
        existing.setName(product.getName());
        existing.setDescription(product.getDescription());
        existing.setPrice(product.getPrice());
        existing.setCostPrice(product.getCostPrice());
        existing.setStockQuantity(product.getStockQuantity());
        existing.setImageUrl(product.getImageUrl());
        existing.setImageUrl2(product.getImageUrl2());
        existing.setImageUrl3(product.getImageUrl3());
        existing.setImageUrl4(product.getImageUrl4());
        existing.setCategory(product.getCategory());
        existing.setBrand(product.getBrand());
        return productRepository.save(existing);
    }

    @Override
    public void delete(UUID id) {
        Product existing = productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Product not found"));
        productRepository.delete(existing);
    }

    @Override
    public Product getById(UUID id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Product not found"));
    }

    @Override
    public ApiResponseWithPagination<Product> getAll(
            int page,
            int size,
            String name,
            UUID brandId,
            UUID categoryId,
            BigDecimal minPrice,
            BigDecimal maxPrice
    ) {
        PageRequest pageable = PageRequest.of(page, size);
        String namePattern = null;
        if (name != null && !name.isBlank()) {
            namePattern = "%" + name.toLowerCase() + "%";
        }
        Page<Product> productPage = productRepository.searchProducts(
                namePattern,
                brandId,
                categoryId,
                minPrice,
                maxPrice,
                pageable
        );

        return ApiResponseWithPagination.itemsAndPaginationResponse(
                productPage.getContent(),
                page,
                size,
                (int) productPage.getTotalElements()
        );
    }
}

