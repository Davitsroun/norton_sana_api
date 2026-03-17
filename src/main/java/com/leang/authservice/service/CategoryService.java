package com.leang.authservice.service;

import com.leang.authservice.model.dto.response.ApiResponseWithPagination;
import com.leang.authservice.model.entity.Category;

import java.util.UUID;

public interface CategoryService {

    Category create(Category category);

    Category update(UUID id, Category category);

    void delete(UUID id);

    Category getById(UUID id);

    ApiResponseWithPagination<Category> getAll(int page, int size);
}

