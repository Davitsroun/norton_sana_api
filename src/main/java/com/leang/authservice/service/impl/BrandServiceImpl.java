package com.leang.authservice.service.impl;

import com.leang.authservice.exception.NotFoundException;
import com.leang.authservice.model.dto.response.ApiResponseWithPagination;
import com.leang.authservice.model.entity.Brand;
import com.leang.authservice.repository.BrandRepository;
import com.leang.authservice.service.BrandService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BrandServiceImpl implements BrandService {

    private final BrandRepository brandRepository;

    @Override
    public Brand create(Brand brand) {
        return brandRepository.save(brand);
    }

    @Override
    public Brand update(UUID id, Brand brand) {
        Brand existing = brandRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Brand not found"));
        existing.setBrandName(brand.getBrandName());
        existing.setCountry(brand.getCountry());
        return brandRepository.save(existing);
    }

    @Override
    public void delete(UUID id) {
        Brand existing = brandRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Brand not found"));
        brandRepository.delete(existing);
    }

    @Override
    public Brand getById(UUID id) {
        return brandRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Brand not found"));
    }

    @Override
    public ApiResponseWithPagination<Brand> getAll(int page, int size) {
        PageRequest pageable = PageRequest.of(page, size);
        Page<Brand> brandPage = brandRepository.findAll(pageable);
        return ApiResponseWithPagination.itemsAndPaginationResponse(
                brandPage.getContent(),
                page,
                size,
                (int) brandPage.getTotalElements()
        );
    }
}

