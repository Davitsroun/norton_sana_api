package com.leang.authservice.service;

import com.leang.authservice.model.dto.request.FavoriteBrandCreateRequest;
import com.leang.authservice.model.dto.response.ApiResponseWithPagination;
import com.leang.authservice.model.entity.FavoriteBrand;

import java.util.UUID;

public interface FavoriteBrandService {
    FavoriteBrand create(FavoriteBrandCreateRequest dto);

    FavoriteBrand getById(UUID id);

    ApiResponseWithPagination<FavoriteBrand> getAll(int page, int size);

    void delete(UUID id);
}

