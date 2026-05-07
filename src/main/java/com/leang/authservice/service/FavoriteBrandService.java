package com.leang.authservice.service;

import com.leang.authservice.model.dto.request.FavoriteBrandCreateRequest;
import com.leang.authservice.model.dto.response.ApiResponseWithPagination;
import com.leang.authservice.model.dto.response.FavoriteBrandResponse;

import java.util.UUID;

public interface FavoriteBrandService {
    FavoriteBrandResponse create(FavoriteBrandCreateRequest dto);

    FavoriteBrandResponse getById(UUID id);

    ApiResponseWithPagination<FavoriteBrandResponse> getAll(int page, int size);

    void delete(UUID id);
}

