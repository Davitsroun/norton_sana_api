package com.leang.authservice.service.impl;

import com.leang.authservice.exception.NotFoundException;
import com.leang.authservice.model.dto.response.ApiResponseWithPagination;
import com.leang.authservice.model.entity.Category;
import com.leang.authservice.repository.CategoryRepository;
import com.leang.authservice.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;

    @Override
    public Category create(Category category) {
        return categoryRepository.save(category);
    }

    @Override
    public Category update(UUID id, Category category) {
        Category existing = categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Category not found"));
        existing.setCategoryName(category.getCategoryName());
        return categoryRepository.save(existing);
    }

    @Override
    public void delete(UUID id) {
        Category existing = categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Category not found"));
        categoryRepository.delete(existing);
    }

    @Override
    public Category getById(UUID id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Category not found"));
    }

    @Override
    public ApiResponseWithPagination<Category> getAll(int page, int size) {
        PageRequest pageable = PageRequest.of(page, size);
        Page<Category> categoryPage = categoryRepository.findAll(pageable);
        return ApiResponseWithPagination.itemsAndPaginationResponse(
                categoryPage.getContent(),
                page,
                size,
                (int) categoryPage.getTotalElements()
        );
    }
}

