package com.leang.authservice.service;

import com.leang.authservice.model.dto.response.ApiResponseWithPagination;
import com.leang.authservice.model.entity.Brand;

import java.util.UUID;

public interface BrandService {

    Brand create(Brand brand);

    Brand update(UUID id, Brand brand);

    void delete(UUID id);

    Brand getById(UUID id);

    ApiResponseWithPagination<Brand> getAll(int page, int size);
}

