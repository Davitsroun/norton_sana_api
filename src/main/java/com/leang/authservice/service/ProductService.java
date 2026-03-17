package com.leang.authservice.service;

import com.leang.authservice.model.dto.response.ApiResponseWithPagination;
import com.leang.authservice.model.entity.Product;

import java.math.BigDecimal;
import java.util.UUID;

public interface ProductService {

    Product create(Product product);

    Product update(UUID id, Product product);

    void delete(UUID id);

    Product getById(UUID id);

    ApiResponseWithPagination<Product> getAll(
            int page,
            int size,
            String name,
            UUID brandId,
            UUID categoryId,
            BigDecimal minPrice,
            BigDecimal maxPrice
    );
}

