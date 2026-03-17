package com.leang.authservice.service;

import com.leang.authservice.model.dto.response.ApiResponseWithPagination;
import com.leang.authservice.model.entity.Review;

import java.util.UUID;

public interface ReviewService {

    Review create(Review review);

    Review update(UUID id, Review review);

    void delete(UUID id);

    Review getById(UUID id);

    ApiResponseWithPagination<Review> getAll(int page, int size);
}

